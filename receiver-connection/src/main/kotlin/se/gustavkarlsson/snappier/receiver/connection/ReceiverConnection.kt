package se.gustavkarlsson.snappier.receiver.connection

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import se.gustavkarlsson.snappier.common.domain.Bytes
import se.gustavkarlsson.snappier.common.message.TransferFile

interface ReceiverConnection {
    val incoming: Observable<Event>

    sealed class Event {
        data class HandshakeReceived(val protocolVersion: Int) : Event()
        data class IntendedFilesReceived(val files: Collection<TransferFile>) : Event()
        data class FileStartReceived(val path: String) : Event()
        data class FileDataReceived(val data: Bytes) : Event()
        object FileEndReceived : Event()
        data class Error(val cause: Throwable) : Event()
    }

    fun sendHandshake(): Completable

    fun sendAcceptedPaths(transferPaths: Collection<String>): Completable
}
