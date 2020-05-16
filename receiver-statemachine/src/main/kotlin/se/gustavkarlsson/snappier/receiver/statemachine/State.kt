package se.gustavkarlsson.snappier.receiver.statemachine

import se.gustavkarlsson.snappier.common.message.File

sealed class State {
    object AwaitingHandshake : State()
    object AwaitingIntendedFiles : State()
    data class AwaitingAcceptedFiles(val intendedFiles: Set<File>) : State()
    data class AwaitingFile(val remainingFiles: Set<File>) : State()
    data class ReceivingFile(val currentFile: File, val currentReceived: Long, val remainingFiles: Set<File>) : State()

    object Completed : State()
    // TODO Add abort and pause/resume
}
