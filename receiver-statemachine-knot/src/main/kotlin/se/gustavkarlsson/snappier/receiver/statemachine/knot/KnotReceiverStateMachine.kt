package se.gustavkarlsson.snappier.receiver.statemachine.knot

import de.halfbit.knot3.Effect
import de.halfbit.knot3.knot
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import mu.KotlinLogging
import se.gustavkarlsson.snappier.common.domain.Bytes
import se.gustavkarlsson.snappier.common.domain.FileRef
import se.gustavkarlsson.snappier.common.message.TransferFile
import se.gustavkarlsson.snappier.receiver.connection.ReceiverConnection
import se.gustavkarlsson.snappier.receiver.files.FileWriter
import se.gustavkarlsson.snappier.receiver.statemachine.ReceiverStateMachine
import se.gustavkarlsson.snappier.receiver.statemachine.State

private val logger = KotlinLogging.logger {}

class KnotReceiverStateMachine(
    protocolVersion: Int,
    connection: ReceiverConnection,
    fileWriter: FileWriter
) : ReceiverStateMachine {

    private val knot = createReceiverKnot(protocolVersion, connection, fileWriter)

    override val state: Observable<State> get() = knot.state

    override fun setAcceptedPaths(receivePath: String, acceptedPaths: Collection<String>) =
        knot.change.accept(Change.SendAcceptedPaths(receivePath, acceptedPaths))
}

private sealed class Change {
    data class HandshakeReceived(val protocolVersion: Int) : Change()
    data class IntendedFilesReceived(val files: Collection<TransferFile>) : Change()
    data class SendAcceptedPaths(val receivePath: String, val acceptedPaths: Collection<String>) : Change()
    data class FileStartReceived(val transferPath: String) : Change()
    data class FileDataReceived(val data: Bytes) : Change()
    data class FileDataWritten(val writtenBytes: Long) : Change()
    data class Error(val cause: Throwable) : Change()
    object FileEndReceived : Change()
}

private sealed class Action {
    object SendHandshake : Action()
    data class SendAcceptedFiles(val transferPaths: Collection<String>) : Action()
    data class CreateFile(val path: String) : Action()
    data class WriteFileData(val data: Bytes) : Action()
    object CloseFile : Action()
}

private fun createReceiverKnot(
    protocolVersion: Int,
    connection: ReceiverConnection,
    fileWriter: FileWriter
) = knot<State, Change, Action> {

    state {
        watchAll { logger.info { "State: $it" } }
        initial = State.AwaitingHandshake
    }

    events {
        source {
            connection.incoming
                .map { event ->
                    when (event) {
                        is ReceiverConnection.ReceivedEvent.Handshake -> Change.HandshakeReceived(event.protocolVersion)
                        is ReceiverConnection.ReceivedEvent.IntendedFiles -> Change.IntendedFilesReceived(event.files)
                        is ReceiverConnection.ReceivedEvent.FileStart -> Change.FileStartReceived(event.path)
                        is ReceiverConnection.ReceivedEvent.FileData -> Change.FileDataReceived(event.data)
                        ReceiverConnection.ReceivedEvent.FileEnd -> Change.FileEndReceived
                        is ReceiverConnection.ReceivedEvent.Error -> Change.Error(event.cause)
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
                State.AwaitingHandshake -> when (change) {
                    is Change.HandshakeReceived -> handshakeReceived(change, protocolVersion)
                    else -> unexpected(change)
                }
                State.AwaitingIntendedFiles -> when (change) {
                    is Change.IntendedFilesReceived -> intendedFilesReceived(change)
                    else -> unexpected(change)
                }
                is State.AwaitingAcceptedPaths -> when (change) {
                    is Change.SendAcceptedPaths -> sendAcceptedPaths(state, change)
                    else -> unexpected(change)
                }
                is State.AwaitingFile -> when (change) {
                    is Change.FileStartReceived -> fileStartReceived(state, change)
                    else -> unexpected(change)
                }
                is State.ReceivingFile -> when (change) {
                    is Change.FileDataReceived -> fileDataReceived(state, change)
                    is Change.FileDataWritten -> fileDataWritten(state, change)
                    Change.FileEndReceived -> fileEndReceived(state)
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
                            ReceiverConnection.SendResult.Success -> Maybe.empty()
                            is ReceiverConnection.SendResult.Error -> Maybe.just(Change.Error(result.cause))
                        }
                    }
            }
        }
        perform<Action.SendAcceptedFiles> {
            concatMapMaybe { action ->
                connection.sendAcceptedPaths(action.transferPaths)
                    .flatMapMaybe { result ->
                        when (result) {
                            ReceiverConnection.SendResult.Success -> Maybe.empty()
                            is ReceiverConnection.SendResult.Error -> Maybe.just(Change.Error(result.cause))
                        }
                    }
            }
        }
        perform<Action.CreateFile> {
            concatMapMaybe { action ->
                fileWriter.create(action.path)
                    .flatMapMaybe { result ->
                        when (result) {
                            FileWriter.Result.Success -> Maybe.empty()
                            is FileWriter.Result.Error -> Maybe.just(Change.Error(result.cause))
                        }
                    }
            }
        }
        perform<Action.WriteFileData> {
            concatMapSingle { action ->
                fileWriter.write(action.data.array)
                    .map { result ->
                        when (result) {
                            FileWriter.Result.Success -> Change.FileDataWritten(action.data.size.toLong())
                            is FileWriter.Result.Error -> Change.Error(result.cause)
                        }
                    }
            }
        }
        perform<Action.CloseFile> {
            concatMapMaybe {
                fileWriter.close()
                    .flatMapMaybe { result ->
                        when (result) {
                            FileWriter.Result.Success -> Maybe.empty()
                            is FileWriter.Result.Error -> Maybe.just(Change.Error(result.cause))
                        }
                    }
            }
        }
    }
}

private fun error(change: Change.Error): Effect<State, Action> =
    effect(State.Failed(change.cause.message ?: change.cause.toString()))

private fun handshakeReceived(change: Change.HandshakeReceived, protocolVersion: Int): Effect<State, Action> =
    if (change.protocolVersion != protocolVersion) {
        val message = "Wrong protocol version received. Got ${change.protocolVersion} but expected $protocolVersion"
        effect(State.Failed(message))
    } else {
        effect(State.AwaitingIntendedFiles, Action.SendHandshake)
    }

private fun intendedFilesReceived(change: Change.IntendedFilesReceived): Effect<State, Action> =
    effect(State.AwaitingAcceptedPaths(change.files))

private fun sendAcceptedPaths(
    state: State.AwaitingAcceptedPaths,
    change: Change.SendAcceptedPaths
): Effect<State, Action> {
    val remainingFiles = state.intendedFiles
        .filter { change.acceptedPaths.contains(it.path) }
        .map { FileRef(change.receivePath + '/' + it.path, it.path, it.size) }
    return effect(State.AwaitingFile(remainingFiles), Action.SendAcceptedFiles(change.acceptedPaths))
}

private fun fileStartReceived(state: State.AwaitingFile, change: Change.FileStartReceived): Effect<State, Action> {
    val newFile = state.remainingFiles.first { it.transferPath == change.transferPath }
    val newState = State.ReceivingFile(newFile, 0, state.remainingFiles - newFile)
    val action = Action.CreateFile(newFile.fileSystemPath)
    return effect(newState, action)
}

private fun fileDataReceived(state: State.ReceivingFile, change: Change.FileDataReceived): Effect<State, Action> =
    effect(state, Action.WriteFileData(change.data))

private fun fileDataWritten(state: State.ReceivingFile, change: Change.FileDataWritten): Effect<State, Action> {
    val newCurrentReceivedBytes = state.currentReceivedBytes + change.writtenBytes
    return effect(state.copy(currentReceivedBytes = newCurrentReceivedBytes))
}

private fun fileEndReceived(state: State.ReceivingFile): Effect<State, Action> =
    if (state.remainingFiles.isEmpty()) {
        effect(State.Completed, Action.CloseFile)
    } else {
        effect(State.AwaitingFile(state.remainingFiles), Action.CloseFile)
    }

private fun effect(state: State, vararg actions: Action): Effect<State, Action> =
    when (actions.size) {
        0 -> Effect.WithAction(state)
        1 -> Effect.WithAction(state, actions.first())
        else -> Effect.WithActions(state, actions.asList())
    }
