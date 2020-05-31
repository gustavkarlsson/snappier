package se.gustavkarlsson.snappier.sender.statemachine.knot

import dagger.Binds
import dagger.Module
import de.halfbit.knot3.Effect
import de.halfbit.knot3.knot
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import mu.KotlinLogging
import se.gustavkarlsson.snappier.common.config.ProtocolVersion
import se.gustavkarlsson.snappier.common.domain.FileRef
import se.gustavkarlsson.snappier.sender.connection.SenderConnection
import se.gustavkarlsson.snappier.sender.files.FileReader
import se.gustavkarlsson.snappier.sender.statemachine.SenderStateMachine
import se.gustavkarlsson.snappier.sender.statemachine.State
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

internal class KnotSenderStateMachine @Inject constructor(
    @ProtocolVersion protocolVersion: Int,
    connection: SenderConnection,
    fileReader: FileReader
) : SenderStateMachine {

    private val knot = createSenderKnot(protocolVersion, connection, fileReader)

    override val state: Observable<State> get() = knot.state

    override fun sendHandshake() = knot.change.accept(Change.SendHandshake)

    override fun sendIntendedFiles(files: Collection<FileRef>) =
        knot.change.accept(Change.SendIntendedFiles(files))

    @Module
    abstract class Binding {
        @Binds
        abstract fun bind(implementation: KnotSenderStateMachine): SenderStateMachine
    }
}

private sealed class Change {
    object SendHandshake : Change()
    data class HandshakeReceived(val protocolVersion: Int) : Change()
    data class SendIntendedFiles(val files: Collection<FileRef>) : Change()
    data class AcceptedPathsReceived(val transferPaths: Collection<String>) : Change()
    data class FileDataSent(val sentBytes: Long) : Change()
    object FileEnd : Change()
    data class Error(val cause: Throwable) : Change()
}

private sealed class Action {
    object SendHandshake : Action()
    data class SendIntendedFiles(val files: Collection<FileRef>) : Action()
    data class SendFile(val file: FileRef) : Action()
}

private fun createSenderKnot(
    protocolVersion: Int,
    connection: SenderConnection,
    fileReader: FileReader
) = knot<State, Change, Action> {

    state {
        watchAll { logger.info { "State: $it" } }
        initial = State.Initial
    }

    events {
        source {
            connection.incoming
                .map { event ->
                    when (event) {
                        is SenderConnection.ReceivedEvent.Handshake -> Change.HandshakeReceived(event.protocolVersion)
                        is SenderConnection.ReceivedEvent.AcceptedPaths ->
                            Change.AcceptedPathsReceived(event.transferPaths)
                        is SenderConnection.ReceivedEvent.Error -> Change.Error(event.cause)
                    }
                }
        }
    }

    changes {
        watchAll { logger.info { "Change: $it" } }
        reduce { change ->
            val state = this
            if (change is Change.Error &&
                state !is State.Completed &&
                state !is State.Failed
            ) return@reduce error(change)
            when (state) {
                State.Initial -> when (change) {
                    Change.SendHandshake -> sendHandshake()
                    else -> unexpected(change)
                }
                State.AwaitingHandshake -> when (change) {
                    is Change.HandshakeReceived -> handshakeReceived(change, protocolVersion)
                    else -> unexpected(change)
                }
                State.AwaitingIntendedFiles -> when (change) {
                    is Change.SendIntendedFiles -> sendIntendedFiles(change)
                    else -> unexpected(change)
                }
                is State.AwaitingAcceptedFiles -> when (change) {
                    is Change.AcceptedPathsReceived -> acceptedPathsReceived(state, change)
                    else -> unexpected(change)
                }
                is State.SendingFile -> when (change) {
                    is Change.FileDataSent -> fileDataSent(state, change)
                    Change.FileEnd -> fileCompleted(state)
                    else -> unexpected(change)
                }
                State.Completed -> state.only
                is State.Failed -> state.only
            }
        }
    }

    actions {
        watchAll { logger.info { "Action: $it" } }
        perform<Action.SendHandshake> {
            concatMapMaybe {
                connection.sendHandshake()
                    .flatMapMaybe { result ->
                        when (result) {
                            SenderConnection.SendResult.Success -> Maybe.empty()
                            is SenderConnection.SendResult.Error -> Maybe.just(Change.Error(result.cause))
                        }
                    }
            }
        }
        perform<Action.SendIntendedFiles> {
            concatMapMaybe { action ->
                connection.sendIntendedFiles(action.files)
                    .flatMapMaybe { result ->
                        when (result) {
                            SenderConnection.SendResult.Success -> Maybe.empty()
                            is SenderConnection.SendResult.Error -> Maybe.just(Change.Error(result.cause))
                        }
                    }
            }
        }
        perform<Action.SendFile> {
            concatMap { action ->
                val sendFileStart = connection.sendFileStart(action.file.transferPath)
                    .flatMapMaybe { result ->
                        when (result) {
                            SenderConnection.SendResult.Success -> Maybe.empty<Change>()
                            is SenderConnection.SendResult.Error -> Maybe.just(Change.Error(result.cause))
                        }
                    }

                val sendData = fileReader.readFile(action.file.fileSystemPath)
                    .flatMapSingle { fileResult ->
                        when (fileResult) {
                            is FileReader.Result.Success -> {
                                connection.sendFileData(fileResult.data.array)
                                    .map { sendResult ->
                                        when (sendResult) {
                                            SenderConnection.SendResult.Success ->
                                                Change.FileDataSent(fileResult.data.size.toLong())
                                            is SenderConnection.SendResult.Error -> Change.Error(sendResult.cause)
                                        }
                                    }
                            }
                            is FileReader.Result.Error -> Single.just(Change.Error(fileResult.cause))
                        }
                    }

                val sendFileEnd = connection.sendFileEnd()
                    .map { result ->
                        when (result) {
                            SenderConnection.SendResult.Success -> Change.FileEnd
                            is SenderConnection.SendResult.Error -> Change.Error(result.cause)
                        }
                    }

                sendFileStart.toObservable()
                    .concatWith(sendData.toObservable())
                    .concatWith(sendFileEnd)
                    .takeUntil { it is Change.Error }
            }
        }
    }
}

private fun error(change: Change.Error): Effect<State, Action> =
    effect(State.Failed(change.cause.message ?: change.cause.toString()))

private fun sendHandshake(): Effect<State, Action> =
    effect(State.AwaitingHandshake, Action.SendHandshake)

private fun handshakeReceived(change: Change.HandshakeReceived, protocolVersion: Int): Effect<State, Action> =
    if (change.protocolVersion != protocolVersion) {
        val message = "Wrong protocol version received. Got ${change.protocolVersion} but expected $protocolVersion"
        effect(State.Failed(message))
    } else {
        effect(State.AwaitingIntendedFiles)
    }

private fun sendIntendedFiles(change: Change.SendIntendedFiles): Effect<State, Action> =
    effect(State.AwaitingAcceptedFiles(change.files), Action.SendIntendedFiles(change.files))

private fun acceptedPathsReceived(
    state: State.AwaitingAcceptedFiles,
    change: Change.AcceptedPathsReceived
): Effect<State, Action> {
    val firstPath = change.transferPaths.first()
    val firstFile = state.intendedFiles.first { it.transferPath == firstPath }
    val remainingFiles = state.intendedFiles.filter { change.transferPaths.contains(it.transferPath) } - firstFile
    val newState = State.SendingFile(firstFile, 0, remainingFiles)
    val newAction = Action.SendFile(firstFile)
    return effect(newState, newAction)
}

private fun fileDataSent(
    state: State.SendingFile,
    change: Change.FileDataSent
): Effect<State, Action> {
    val newCurrentSent = state.currentSentBytes + change.sentBytes
    return effect(state.copy(currentSentBytes = newCurrentSent))
}

private fun fileCompleted(state: State.SendingFile): Effect<State, Action> {
    val nextFile = state.remainingFiles.firstOrNull()
    return if (nextFile != null) {
        val nextState = State.SendingFile(nextFile, 0, state.remainingFiles - nextFile)
        val action = Action.SendFile(nextFile)
        effect(nextState, action)
    } else {
        effect(State.Completed)
    }
}

private fun effect(state: State, vararg actions: Action): Effect<State, Action> =
    when (actions.size) {
        0 -> Effect.WithAction(state)
        1 -> Effect.WithAction(state, actions.first())
        else -> Effect.WithActions(state, actions.asList())
    }
