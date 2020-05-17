package se.gustavkarlsson.snappier.receiver.statemachine

import io.reactivex.rxjava3.core.Observable

interface ReceiverStateMachine {
    val state: Observable<State>

    fun setAcceptedPaths(receivePath: String, acceptedPaths: Collection<String>)
}
