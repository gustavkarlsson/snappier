package se.gustavkarlsson.snappier.sender.connection

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import se.gustavkarlsson.snappier.common.domain.FileRef

interface SenderConnection {
    val incoming: Observable<Event>

    sealed class Event {
        data class Handshake(val protocolVersion: Int) : Event()
        data class AcceptedPaths(val transferPaths: Collection<String>) : Event()
    }

    fun sendHandshake(): Completable

    fun sendIntendedFiles(files: Collection<FileRef>): Completable

    fun sendFileStart(path: String): Completable

    fun sendFileData(data: ByteArray): Completable

    fun sendFileEnd(): Completable
}
