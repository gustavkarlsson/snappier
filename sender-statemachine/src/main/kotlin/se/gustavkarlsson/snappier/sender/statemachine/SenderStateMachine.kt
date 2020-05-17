package se.gustavkarlsson.snappier.sender.statemachine

import io.reactivex.rxjava3.core.Observable
import se.gustavkarlsson.snappier.common.domain.TransferFile

interface SenderStateMachine {
    val state: Observable<State>

    fun sendHandshake()

    fun sendIntendedFiles(files: Collection<TransferFile>)
}
