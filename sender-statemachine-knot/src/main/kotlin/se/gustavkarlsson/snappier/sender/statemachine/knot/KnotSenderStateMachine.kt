package se.gustavkarlsson.snappier.sender.statemachine.knot

import de.halfbit.knot3.knot
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import mu.KotlinLogging
import se.gustavkarlsson.snappier.common.domain.TransferFile
import se.gustavkarlsson.snappier.common.message.File
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

    override fun sendIntendedFiles(files: Collection<TransferFile>) =
        knot.change.accept(Change.SendIntendedFiles(files))
}

private sealed class Change {
    object SendHandshake : Change()
    object ReceivedHandshake : Change()
    data class SendIntendedFiles(val files: Collection<TransferFile>) : Change()
    data class ReceivedAcceptedFiles(val transferPaths: Collection<String>) : Change()
    data class FileDataSent(val sent: Long) : Change()
    object FileCompleted : Change()
    object FileSendingFailed : Change()
}

private sealed class Action {
    object SendHandshake : Action()
    data class SendIntendedFiles(val files: Collection<TransferFile>) : Action()
    data class SendFile(val file: TransferFile) : Action()
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
                        is SenderConnection.Event.Handshake -> Change.ReceivedHandshake
                        is SenderConnection.Event.AcceptedPaths -> Change.ReceivedAcceptedFiles(event.transferPaths)
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
                    is Change.SendIntendedFiles -> State.AwaitingAcceptedFiles(change.files) +
                        Action.SendIntendedFiles(change.files)
                    else -> unexpected(change)
                }
                is State.AwaitingAcceptedFiles -> when (change) {
                    is Change.ReceivedAcceptedFiles -> {
                        val firstPath = change.transferPaths.first()
                        val firstFile = state.intendedFiles.first { it.transferPath == firstPath }
                        val remainingFiles = state.intendedFiles.filter { change.transferPaths.contains(it.transferPath) } - firstFile
                        State.SendingFile(firstFile, 0, remainingFiles) + Action.SendFile(firstFile)
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
                .doOnError { logger.error(it) { "Action source failed" } }
        }
        perform<Action.SendIntendedFiles> {
            concatMap { action -> connection.sendIntendedFiles(action.files).toObservable<Change>() }
                .doOnError { logger.error(it) { "Action source failed" } }
        }
        perform<Action.SendFile> {
            concatMap { action ->
                val sendFileStart = connection.sendFileStart(action.file).toFlowable<Change>()
                val sendData = fileReader.readFile(File(action.file.fileSystemPath, action.file.size))
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
