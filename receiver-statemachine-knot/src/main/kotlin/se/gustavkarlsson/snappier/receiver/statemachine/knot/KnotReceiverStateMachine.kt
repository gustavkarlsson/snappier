package se.gustavkarlsson.snappier.receiver.statemachine.knot

import de.halfbit.knot3.knot
import io.reactivex.rxjava3.core.Observable
import mu.KotlinLogging
import se.gustavkarlsson.snappier.common.domain.FileRef
import se.gustavkarlsson.snappier.common.message.SenderMessage
import se.gustavkarlsson.snappier.common.message.TransferFile
import se.gustavkarlsson.snappier.receiver.connection.ReceiverConnection
import se.gustavkarlsson.snappier.receiver.files.FileWriter
import se.gustavkarlsson.snappier.receiver.statemachine.ReceiverStateMachine
import se.gustavkarlsson.snappier.receiver.statemachine.State

private val logger = KotlinLogging.logger {}

class KnotReceiverStateMachine(
    connection: ReceiverConnection,
    fileWriter: FileWriter
) : ReceiverStateMachine {

    private val knot = createReceiverKnot(connection, fileWriter)

    override val state: Observable<State> get() = knot.state

    override fun setAcceptedPaths(receivePath: String, acceptedPaths: Collection<String>) =
        knot.change.accept(Change.SendAcceptedPaths(receivePath, acceptedPaths))
}

private sealed class Change {
    object HandshakeReceived : Change()
    data class IntendedFilesReceived(val files: Collection<TransferFile>) : Change()
    data class SendAcceptedPaths(val receivePath: String, val acceptedPaths: Collection<String>) : Change()
    data class FileStartReceived(val transferPath: String) : Change()
    data class FileDataReceived(val data: ByteArray) : Change() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SenderMessage.FileData

            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            return data.contentHashCode()
        }

        override fun toString(): String = "FileDataReceived(size=${data.size})"
    }

    data class FileDataWritten(val writtenBytes: Long) : Change()

    object FileEndReceived : Change()
}

private sealed class Action {
    object SendHandshake : Action()
    data class SendAcceptedFiles(val transferPaths: Collection<String>) : Action()
    data class CreateFile(val path: String) : Action()
    data class WriteFileData(val data: ByteArray) : Action() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SenderMessage.FileData

            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            return data.contentHashCode()
        }

        override fun toString(): String = "WriteFileData(size=${data.size})"
    }

    object CloseFile : Action()
}

// TODO error handling in knot????
private fun createReceiverKnot(
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
                        is ReceiverConnection.Event.HandshakeReceived -> Change.HandshakeReceived
                        is ReceiverConnection.Event.IntendedFilesReceived -> Change.IntendedFilesReceived(event.files)
                        is ReceiverConnection.Event.FileStartReceived -> Change.FileStartReceived(event.path)
                        is ReceiverConnection.Event.FileDataReceived -> Change.FileDataReceived(event.data)
                        ReceiverConnection.Event.FileEndReceived -> Change.FileEndReceived
                    }
                }
                .doOnError { logger.error(it) { "Event source failed" } }
        }
    }

    changes {
        watchAll { logger.info { "Change: $it" } }
        reduce { change ->
            when (val state = this) {
                State.AwaitingHandshake -> when (change) {
                    // TODO Verify protocol version
                    Change.HandshakeReceived -> State.AwaitingIntendedFiles.only + Action.SendHandshake
                    else -> unexpected(change)
                }
                State.AwaitingIntendedFiles -> when (change) {
                    is Change.IntendedFilesReceived -> State.AwaitingAcceptedPaths(change.files).only
                    else -> unexpected(change)
                }
                is State.AwaitingAcceptedPaths -> when (change) {
                    is Change.SendAcceptedPaths -> {
                        val remainingFiles = state.intendedFiles
                            .filter { change.acceptedPaths.contains(it.path) }
                            .map { FileRef(change.receivePath + '/' + it.path, it.path, it.size) }
                        State.AwaitingFile(remainingFiles) +
                            Action.SendAcceptedFiles(change.acceptedPaths)
                    }
                    else -> unexpected(change)
                }
                is State.AwaitingFile -> when (change) {
                    is Change.FileStartReceived -> {
                        val newFile = state.remainingFiles.first { it.transferPath == change.transferPath }
                        State.ReceivingFile(newFile, 0, state.remainingFiles - newFile) +
                            Action.CreateFile(newFile.fileSystemPath)
                    }
                    else -> unexpected(change)
                }
                is State.ReceivingFile -> when (change) {
                    is Change.FileDataReceived -> state + Action.WriteFileData(change.data)
                    is Change.FileDataWritten -> {
                        val newCurrentReceivedBytes = state.currentReceivedBytes + change.writtenBytes
                        state.copy(currentReceivedBytes = newCurrentReceivedBytes).only
                    }
                    Change.FileEndReceived -> if (state.remainingFiles.isEmpty()) {
                        State.Completed + Action.CloseFile
                    } else {
                        State.AwaitingFile(state.remainingFiles) + Action.CloseFile
                    }
                    else -> unexpected(change)
                }
                State.Completed -> state.only
            }
        }
    }

    actions {
        watchAll {
            watchAll { logger.info { "Action: $it" } }
        }
        perform<Action.SendHandshake> {
            concatMap { connection.sendHandshake().toObservable<Change>() }
                .doOnError { logger.error(it) { "Action failed" } }
        }
        perform<Action.SendAcceptedFiles> {
            concatMap { action -> connection.sendAcceptedPaths(action.transferPaths).toObservable<Change>() }
                .doOnError { logger.error(it) { "Action failed" } }
        }
        perform<Action.CreateFile> {
            concatMap { action -> fileWriter.create(action.path).toObservable<Change>() }
                .doOnError { logger.error(it) { "Action failed" } }
        }
        perform<Action.WriteFileData> {
            concatMap { action ->
                fileWriter.write(action.data)
                    .andThen(Observable.just<Change>(Change.FileDataWritten(action.data.size.toLong())))
            }.doOnError { logger.error(it) { "Action failed" } }
        }
        perform<Action.CloseFile> {
            concatMap { fileWriter.close().toObservable<Change>() }
                .doOnError { logger.error(it) { "Action failed" } }
        }
    }
}
