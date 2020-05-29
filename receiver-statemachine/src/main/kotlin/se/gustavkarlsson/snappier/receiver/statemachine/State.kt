package se.gustavkarlsson.snappier.receiver.statemachine

import se.gustavkarlsson.snappier.common.domain.FileRef
import se.gustavkarlsson.snappier.common.message.TransferFile

sealed class State {
    object AwaitingHandshake : State()
    object AwaitingIntendedFiles : State()
    data class AwaitingAcceptedPaths(val intendedFiles: Collection<TransferFile>) : State()
    data class AwaitingFile(val remainingFiles: Collection<FileRef>) : State()
    data class ReceivingFile(
        val currentFile: FileRef,
        val currentReceivedBytes: Long,
        val remainingFiles: Collection<FileRef>
    ) : State()

    object Completed : State()
    data class Failed(val message: String) : State()
    // TODO Add abort and pause/resume
}
