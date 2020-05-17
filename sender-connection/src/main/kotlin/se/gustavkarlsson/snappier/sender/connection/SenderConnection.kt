package se.gustavkarlsson.snappier.sender.connection

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import se.gustavkarlsson.snappier.common.domain.TransferFile

interface SenderConnection {
    val incoming: Observable<Event>

    sealed class Event {
        data class Handshake(val protocolVersion: Int) : Event()
        data class AcceptedPaths(val transferPaths: Collection<String>) : Event()
    }

    fun sendHandshake(): Completable

    fun sendIntendedFiles(files: Collection<TransferFile>): Completable

    fun sendFileStart(file: TransferFile): Completable

    fun sendFileData(data: ByteArray): Completable

    fun sendFileEnd(): Completable
}
