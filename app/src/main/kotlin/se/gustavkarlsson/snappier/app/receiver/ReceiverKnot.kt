package se.gustavkarlsson.snappier.app.receiver

import de.halfbit.knot3.Knot
import de.halfbit.knot3.knot
import mu.KotlinLogging
import se.gustavkarlsson.snappier.common.message.File

private val logger = KotlinLogging.logger {}

sealed class State {
    object AwaitingHandshake : State()
    object AwaitingIntendedFiles : State()
    data class AwaitingAcceptedFiles(val intendedFiles: Set<File>) : State()
    data class AwaitingFile(val remainingFiles: Set<File>) : State()
    data class ReceivingFile(val currentFile: File, val currentReceived: Long, val remainingFiles: Set<File>) : State()

    object Completed : State()
    // TODO Add abort and pause/resume
}

sealed class Change {
    object ReceivedHandshake : Change()
    data class ReceivedIntendedFiles(val files: Set<File>) : Change()
    data class SendAcceptedFiles(val files: Set<File>) : Change()
    data class NewFile(val file: File) : Change()
    data class FileDataReceived(val received: Long) : Change()
    object FileCompleted : Change()
}

private sealed class Action {
    object SendHandshake : Action()
    data class SendAcceptedFiles(val files: Set<File>) : Action()
}

fun createReceiverKnot(connection: ReceiverConnection): Knot<State, Change> = knot<State, Change, Action> {

    state {
        watchAll { logger.info { "State: $it" } }
        initial = State.AwaitingHandshake
    }

    events {
        source {
            connection.incoming
                .map { event ->
                    when (event) {
                        is ReceiverConnection.Event.Handshake -> Change.ReceivedHandshake
                        is ReceiverConnection.Event.IntendedFiles -> Change.ReceivedIntendedFiles(event.files)
                        is ReceiverConnection.Event.NewFile -> Change.NewFile(event.file)
                        is ReceiverConnection.Event.FileDataReceived -> Change.FileDataReceived(event.received)
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
                    is Change.ReceivedIntendedFiles -> State.AwaitingAcceptedFiles(change.files).only
                    else -> unexpected(change)
                }
                is State.AwaitingAcceptedFiles -> when (change) {
                    is Change.SendAcceptedFiles -> State.AwaitingFile(change.files) +
                        Action.SendAcceptedFiles(change.files)
                    else -> unexpected(change)
                }
                is State.AwaitingFile -> when (change) {
                    is Change.NewFile -> {
                        val newFile = change.file
                        State.ReceivingFile(newFile, 0, state.remainingFiles - newFile).only
                    }
                    else -> unexpected(change)
                }
                is State.ReceivingFile -> when (change) {
                    is Change.FileDataReceived -> {
                        val newCurrentReceived = state.currentReceived + change.received
                        state.copy(currentReceived = newCurrentReceived).only
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
            concatMap { action -> connection.sendAcceptedFiles(action.files).toObservable<Change>() }
                .doOnError { logger.error(it) { "Action source failed" } }
        }
    }
}
