package se.gustavkarlsson.snappier.receiver.statemachine

import se.gustavkarlsson.snappier.common.domain.TransferFile
import se.gustavkarlsson.snappier.common.message.File

sealed class State {
    object AwaitingHandshake : State()
    object AwaitingIntendedFiles : State()
    data class AwaitingAcceptedPaths(val intendedFiles: Collection<File>) : State()
    data class AwaitingFile(val remainingFiles: Collection<TransferFile>) : State()
    data class ReceivingFile(val currentFile: TransferFile, val currentReceivedBytes: Long, val remainingFiles: Collection<TransferFile>) : State()

    object Completed : State()
    // TODO Add abort and pause/resume
}
