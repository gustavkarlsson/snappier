package se.gustavkarlsson.snappier.sender.statemachine

import se.gustavkarlsson.snappier.common.message.File

sealed class State {
    object Initial : State()
    object AwaitingHandshake : State()
    object AwaitingIntendedFiles : State()
    object AwaitingAcceptedFiles : State()
    data class SendingFile(val currentFile: File, val currentSent: Long, val remainingFiles: Set<File>) : State()
    object Completed : State()
    object TransferFailed : State()
    // TODO Add abort and pause/resume
}
