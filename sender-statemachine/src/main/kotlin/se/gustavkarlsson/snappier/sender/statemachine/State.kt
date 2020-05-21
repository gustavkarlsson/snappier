package se.gustavkarlsson.snappier.sender.statemachine

import se.gustavkarlsson.snappier.common.domain.FileRef

sealed class State {
    object Initial : State()
    object AwaitingHandshake : State()
    object AwaitingIntendedFiles : State()
    data class AwaitingAcceptedFiles(val intendedFiles: Collection<FileRef>) : State()
    data class SendingFile(
        val currentFile: FileRef,
        val currentSentBytes: Long,
        val remainingFiles: Collection<FileRef>
    ) : State()

    object Completed : State()
    object TransferFailed : State()
    // TODO Add abort and pause/resume
}
