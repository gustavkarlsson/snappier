package se.gustavkarlsson.snappier.app.sender

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import se.gustavkarlsson.snappier.message.File

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
