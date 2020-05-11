package sender

import common.File
import de.halfbit.knot3.Knot
import de.halfbit.knot3.knot
import io.reactivex.rxjava3.core.Observable
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

sealed class State {
    object Initial : State()
    object AwaitingHandshake : State()
    object AwaitingIntendedFiles : State()
    object AwaitingAcceptedFiles : State()
    data class SendingFile(val currentFile: File, val currentSent: Long, val remainingFiles: Set<File>) : State()
    object Completed : State()
    // TODO Add abort and pause/resume
}

sealed class Change {
    object SendHandshake : Change()
    object ReceivedHandshake : Change()
    data class SendIntendedFiles(val files: Set<File>) : Change()
    data class ReceivedAcceptedFiles(val files: Set<File>) : Change()
    data class FileDataSent(val sent: Long) : Change()
    object FileCompleted : Change()
}

private sealed class Action {
    object SendHandshake : Action()
    data class SendIntendedFiles(val files: Set<File>) : Action()
    data class SendFile(val file: File) : Action()
}

fun createSenderKnot(connection: SenderConnection): Knot<State, Change> = knot<State, Change, Action> {

    state {
        watchAll { logger.info { "State: $it" } }
        initial = State.Initial
    }

    events {
        source {
            connection.incoming
                .map { event ->
                    when (event) {
                        is SenderConnection.Event.Handshake -> Change.ReceivedHandshake
                        is SenderConnection.Event.AcceptedFiles -> Change.ReceivedAcceptedFiles(
                            event.files
                        )
                    }
                }
                .doOnError { logger.error(it) { "Event source failed" } }
        }
    }

    changes {
        watchAll { logger.info { "Change: $it" } }
        reduce { change ->
            when (val state = this) {
                State.Initial -> when (change) {
                    Change.SendHandshake -> State.AwaitingHandshake + Action.SendHandshake
                    else -> unexpected(change)
                }
                State.AwaitingHandshake -> when (change) {
                    Change.ReceivedHandshake -> State.AwaitingIntendedFiles.only // TODO Verify protocol version
                    else -> unexpected(change)
                }
                State.AwaitingIntendedFiles -> when (change) {
                    is Change.SendIntendedFiles -> State.AwaitingAcceptedFiles + Action.SendIntendedFiles(
                        change.files
                    )
                    else -> unexpected(change)
                }
                State.AwaitingAcceptedFiles -> when (change) {
                    is Change.ReceivedAcceptedFiles -> {
                        val firstFile = change.files.first()
                        State.SendingFile(firstFile, 0, change.files - firstFile) +
                            Action.SendFile(firstFile)
                    }
                    else -> unexpected(change)
                }
                is State.SendingFile -> when (change) {
                    is Change.FileDataSent -> {
                        val newCurrentSent = state.currentSent + change.sent
                        state.copy(currentSent = newCurrentSent).only
                    }
                    Change.FileCompleted -> {
                        val nextFile = state.remainingFiles.firstOrNull()
                        if (nextFile != null) {
                            State.SendingFile(nextFile, 0, state.remainingFiles - nextFile) +
                                Action.SendFile(nextFile)
                        } else State.Completed.only
                    }
                    else -> unexpected(change)
                }
                State.Completed -> state.only
            }
        }
    }

    actions {
        watchAll { logger.info { "Action: $it" } }
        perform<Action.SendHandshake> {
            concatMap { connection.sendHandshake().toObservable<Change>() }
                .doOnError { logger.error(it) { "Action source failed" } }
        }
        perform<Action.SendIntendedFiles> {
            concatMap { action -> connection.sendIntendedFiles(action.files).toObservable<Change>() }
                .doOnError { logger.error(it) { "Action source failed" } }
        }
        perform<Action.SendFile> {
            concatMap { action ->
                connection.sendFile(action.file)
                    .map<Change> { sent -> Change.FileDataSent(sent) }
                    .concatWith(Observable.just(Change.FileCompleted))
                    .doOnError { logger.error(it) { "Action source failed" } }
            }
        }
    }
}
