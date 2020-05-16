package se.gustavkarlsson.snappier.receiver.statemachine

import io.reactivex.rxjava3.core.Observable
import se.gustavkarlsson.snappier.common.message.File

interface ReceiverStateMachine {
    val state: Observable<State>

    fun setAcceptedFiles(files: Set<File>)
}
