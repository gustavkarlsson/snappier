package sender

import common.File
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable

interface SenderConnection {
    val incoming: Observable<Event>

    sealed class Event {
        data class Handshake(val protocolVersion: Int) : Event()
        data class AcceptedFiles(val files: Set<File>) : Event()
    }

    fun sendHandshake(): Completable

    fun sendIntendedFiles(files: Set<File>): Completable

    fun sendFile(file: File): Observable<Long>
}
