package se.gustavkarlsson.snappier.sender.statemachine.knot

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
    connection: SenderConnection,
    fileReader: FileReader
) : SenderStateMachine {

    private val knot = createSenderKnot(connection, fileReader)

    override val state: Observable<State> get() = knot.state

    override fun sendHandshake() = knot.change.accept(Change.SendHandshake)

    override fun sendIntendedFiles(files: Collection<FileRef>) =
        knot.change.accept(Change.SendIntendedFiles(files))
}

private sealed class Change {
    object SendHandshake : Change()
    object HandshakeReceived : Change()
    data class SendIntendedFiles(val files: Collection<FileRef>) : Change()
    data class AcceptedPathsReceived(val transferPaths: Collection<String>) : Change()
    data class FileDataSent(val sentBytes: Long) : Change()
    object FileCompleted : Change()
    object FileSendingFailed : Change()
}

private sealed class Action {
    object SendHandshake : Action()
    data class SendIntendedFiles(val files: Collection<FileRef>) : Action()
    data class SendFile(val file: FileRef) : Action()
}

// TODO error handling in knot????
private fun createSenderKnot(connection: SenderConnection, fileReader: FileReader) = knot<State, Change, Action> {

    state {
        watchAll { logger.info { "State: $it" } }
        initial = State.Initial
    }

    events {
        source {
            connection.incoming
                .map { event ->
                    when (event) {
                        is SenderConnection.Event.HandshakeReceived -> Change.HandshakeReceived
                        is SenderConnection.Event.AcceptedPathsReceived -> Change.AcceptedPathsReceived(event.transferPaths)
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
                    // TODO Verify protocol version
                    Change.HandshakeReceived -> State.AwaitingIntendedFiles.only
                    else -> unexpected(change)
                }
                State.AwaitingIntendedFiles -> when (change) {
                    is Change.SendIntendedFiles -> State.AwaitingAcceptedFiles(change.files) +
                        Action.SendIntendedFiles(change.files)
                    else -> unexpected(change)
                }
                is State.AwaitingAcceptedFiles -> when (change) {
                    is Change.AcceptedPathsReceived -> {
                        val firstPath = change.transferPaths.first()
                        val firstFile = state.intendedFiles.first { it.transferPath == firstPath }
                        val remainingFiles = state.intendedFiles.filter { change.transferPaths.contains(it.transferPath) } - firstFile
                        State.SendingFile(firstFile, 0, remainingFiles) + Action.SendFile(firstFile)
                    }
                    else -> unexpected(change)
                }
                is State.SendingFile -> when (change) {
                    is Change.FileDataSent -> {
                        val newCurrentSent = state.currentSentBytes + change.sentBytes
                        state.copy(currentSentBytes = newCurrentSent).only
                    }
                    Change.FileCompleted -> {
                        val nextFile = state.remainingFiles.firstOrNull()
                        if (nextFile != null) {
                            State.SendingFile(nextFile, 0, state.remainingFiles - nextFile) +
                                Action.SendFile(nextFile)
                        } else State.Completed.only
                    }
                    Change.FileSendingFailed -> State.TransferFailed.only // TODO show error?
                    else -> unexpected(change)
                }
                State.Completed -> state.only
                State.TransferFailed -> state.only
            }
        }
    }

    actions {
        watchAll { logger.info { "Action: $it" } }
        perform<Action.SendHandshake> {
            concatMap { connection.sendHandshake().toObservable<Change>() }
                .doOnError { logger.error(it) { "Action failed" } }
        }
        perform<Action.SendIntendedFiles> {
            concatMap { action -> connection.sendIntendedFiles(action.files).toObservable<Change>() }
                .doOnError { logger.error(it) { "Action failed" } }
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
