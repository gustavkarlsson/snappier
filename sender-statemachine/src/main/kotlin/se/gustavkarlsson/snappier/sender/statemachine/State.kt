package se.gustavkarlsson.snappier.sender.statemachine

import se.gustavkarlsson.snappier.common.domain.TransferFile

sealed class State {
    object Initial : State()
    object AwaitingHandshake : State()
    object AwaitingIntendedFiles : State()
    data class AwaitingAcceptedFiles(val intendedFiles: Collection<TransferFile>) : State()
    data class SendingFile(
        val currentFile: TransferFile,
        val currentSent: Long,
        val remainingFiles: Collection<TransferFile>
    ) : State()

    object Completed : State()
    object TransferFailed : State()
    // TODO Add abort and pause/resume
}
