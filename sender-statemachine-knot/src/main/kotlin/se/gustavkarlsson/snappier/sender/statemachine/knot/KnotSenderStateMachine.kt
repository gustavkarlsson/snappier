package se.gustavkarlsson.snappier.sender.statemachine.knot

import de.halfbit.knot3.Effect
import de.halfbit.knot3.knot
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import mu.KotlinLogging
import se.gustavkarlsson.snappier.common.domain.FileRef
import se.gustavkarlsson.snappier.sender.connection.SenderConnection
import se.gustavkarlsson.snappier.sender.files.FileReader
import se.gustavkarlsson.snappier.sender.statemachine.SenderStateMachine
import se.gustavkarlsson.snappier.sender.statemachine.State

private val logger = KotlinLogging.logger {}

class KnotSenderStateMachine(
    protocolVersion: Int,
    connection: SenderConnection,
    fileReader: FileReader
) : SenderStateMachine {

    private val knot = createSenderKnot(protocolVersion, connection, fileReader)

    override val state: Observable<State> get() = knot.state

    override fun sendHandshake() = knot.change.accept(Change.SendHandshake)

    override fun sendIntendedFiles(files: Collection<FileRef>) =
        knot.change.accept(Change.SendIntendedFiles(files))
}

private sealed class Change {
    object SendHandshake : Change()
    data class HandshakeReceived(val protocolVersion: Int) : Change()
    data class SendIntendedFiles(val files: Collection<FileRef>) : Change()
    data class AcceptedPathsReceived(val transferPaths: Collection<String>) : Change()
    data class FileDataSent(val sentBytes: Long) : Change()
    object FileCompleted : Change()
    data class ConnectionError(val cause: Throwable) : Change()
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
                        is SenderConnection.Event.HandshakeReceived -> Change.HandshakeReceived(event.protocolVersion)
                        is SenderConnection.Event.AcceptedPathsReceived ->
                            Change.AcceptedPathsReceived(event.transferPaths)
                        is SenderConnection.Event.Error -> Change.ConnectionError(event.cause)
                    }
                }
        }
    }

    changes {
        watchAll { logger.info { "Change: $it" } }
        reduce { change ->
            val state = this
            if (change is Change.ConnectionError &&
                state !is State.Completed &&
                state !is State.Failed
            ) return@reduce connectionError(change)
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
                    Change.FileCompleted -> fileCompleted(state)
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
            @Suppress("RemoveExplicitTypeArguments")
            concatMap { connection.sendHandshake().toObservable<Change>() }
        }
        perform<Action.SendIntendedFiles> {
            concatMap { action ->
                @Suppress("RemoveExplicitTypeArguments")
                connection.sendIntendedFiles(action.files).toObservable<Change>()
            }
        }
        perform<Action.SendFile> {
            concatMap { action ->
                val sendFileStart = connection.sendFileStart(action.file.transferPath).toFlowable<Change>()
                val sendData = fileReader.readFile(action.file.fileSystemPath)
                    .concatMap { data ->
                        connection.sendFileData(data).andThen(Flowable.just(Change.FileDataSent(data.size.toLong())))
                    }
                val sendFileCompleted = connection.sendFileEnd().andThen(Flowable.just(Change.FileCompleted))

                sendFileStart
                    .concatWith(sendData)
                    .concatWith(sendFileCompleted)
                    .toObservable()
            }
        }
    }
}

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
    return Effect.WithAction(newState, newAction)
}

private fun fileDataSent(
    state: State.SendingFile,
    change: Change.FileDataSent
): Effect<State, Action> {
    val newCurrentSent = state.currentSentBytes + change.sentBytes
    return Effect.WithAction(state.copy(currentSentBytes = newCurrentSent))
}

private fun fileCompleted(state: State.SendingFile): Effect<State, Action> {
    val nextFile = state.remainingFiles.firstOrNull()
    return if (nextFile != null) {
        effect(State.SendingFile(nextFile, 0, state.remainingFiles - nextFile), Action.SendFile(nextFile))
    } else {
        effect(State.Completed)
    }
}

private fun connectionError(change: Change.ConnectionError): Effect<State, Action> =
    effect(State.Failed(change.cause.message?: change.cause.toString()))

private fun effect(state: State, vararg actions: Action): Effect<State, Action> =
    when (actions.size) {
        0 -> Effect.WithAction(state)
        1 -> Effect.WithAction(state, actions.first())
        else -> Effect.WithActions(state, actions.asList())
    }
