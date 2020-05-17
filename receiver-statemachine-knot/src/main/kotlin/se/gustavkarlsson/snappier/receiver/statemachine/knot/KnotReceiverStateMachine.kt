package se.gustavkarlsson.snappier.receiver.statemachine.knot

import de.halfbit.knot3.knot
import io.reactivex.rxjava3.core.Observable
import mu.KotlinLogging
import se.gustavkarlsson.snappier.common.domain.TransferFile
import se.gustavkarlsson.snappier.common.message.File
import se.gustavkarlsson.snappier.common.message.SenderMessage
import se.gustavkarlsson.snappier.receiver.connection.ReceiverConnection
import se.gustavkarlsson.snappier.receiver.files.FileWriter
import se.gustavkarlsson.snappier.receiver.statemachine.ReceiverStateMachine
import se.gustavkarlsson.snappier.receiver.statemachine.State

private val logger = KotlinLogging.logger {}

class KnotReceiverStateMachine(
    connection: ReceiverConnection,
    fileWriter: FileWriter
) : ReceiverStateMachine {

    private val knot = createReceiverKnot(connection)

    override val state: Observable<State> get() = knot.state

    override fun setAcceptedPaths(receivePath: String, acceptedPaths: Collection<String>) =
        knot.change.accept(Change.SendAcceptedFiles(receivePath, acceptedPaths))
}

private sealed class Change {
    object ReceivedHandshake : Change()
    data class ReceivedIntendedFiles(val files: Collection<File>) : Change()
    data class SendAcceptedFiles(val receivePath: String, val acceptedPaths: Collection<String>) : Change()
    data class NewFile(val transferPath: String) : Change()
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

    object FileCompleted : Change()
}

private sealed class Action {
    object SendHandshake : Action()
    data class SendAcceptedFiles(val transferPaths: Collection<String>) : Action()
}

// TODO error handling in knot????
private fun createReceiverKnot(connection: ReceiverConnection) = knot<State, Change, Action> {

    state {
        watchAll { logger.info { "State: $it" } }
        initial = State.AwaitingHandshake
    }

    // TODO write file before mapping to change
    events {
        source {
            connection.incoming
                .map { event ->
                    when (event) {
                        is ReceiverConnection.Event.Handshake -> Change.ReceivedHandshake
                        is ReceiverConnection.Event.IntendedFiles -> Change.ReceivedIntendedFiles(event.files)
                        is ReceiverConnection.Event.NewFile -> Change.NewFile(event.file.path)
                        is ReceiverConnection.Event.FileDataReceived -> Change.FileDataReceived(event.data)
                        ReceiverConnection.Event.FileCompleted -> Change.FileCompleted
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
                    Change.ReceivedHandshake -> State.AwaitingIntendedFiles.only + Action.SendHandshake // TODO Verify protocol version
                    else -> unexpected(change)
                }
                State.AwaitingIntendedFiles -> when (change) {
                    is Change.ReceivedIntendedFiles -> State.AwaitingAcceptedPaths(change.files).only
                    else -> unexpected(change)
                }
                is State.AwaitingAcceptedPaths -> when (change) {
                    is Change.SendAcceptedFiles -> {
                        val remainingFiles = state.intendedFiles
                            .filter { change.acceptedPaths.contains(it.path) }
                            .map { TransferFile(change.receivePath + '/' + it.path, it.path, it.size) }
                        State.AwaitingFile(remainingFiles) +
                            Action.SendAcceptedFiles(change.acceptedPaths)
                    }
                    else -> unexpected(change)
                }
                is State.AwaitingFile -> when (change) {
                    is Change.NewFile -> {
                        val newFile = state.remainingFiles.first { it.transferPath == change.transferPath }
                        State.ReceivingFile(newFile, 0, state.remainingFiles - newFile).only
                    }
                    else -> unexpected(change)
                }
                is State.ReceivingFile -> when (change) {
                    is Change.FileDataReceived -> {
                        val newCurrentReceivedBytes = state.currentReceivedBytes + change.data.size
                        state.copy(currentReceivedBytes = newCurrentReceivedBytes).only
                    }
                    Change.FileCompleted -> if (state.remainingFiles.isEmpty()) {
                        State.Completed.only
                    } else {
                        State.AwaitingFile(state.remainingFiles).only
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
                .doOnError { logger.error(it) { "Action source failed" } }
        }
        perform<Action.SendAcceptedFiles> {
            concatMap { action -> connection.sendAcceptedPaths(action.transferPaths).toObservable<Change>() }
                .doOnError { logger.error(it) { "Action source failed" } }
        }
    }
}
