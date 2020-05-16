package se.gustavkarlsson.snappier.sender.connection

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import se.gustavkarlsson.snappier.common.message.File

interface SenderConnection {
    val incoming: Observable<Event>

    sealed class Event {
        data class Handshake(val protocolVersion: Int) : Event()
        data class AcceptedFiles(val files: Set<File>) : Event()
    }

    fun sendHandshake(): Completable

    fun sendIntendedFiles(files: Set<File>): Completable

    fun sendFileStart(file: File): Completable

    fun sendFileData(data: ByteArray): Completable

    fun sendFileEnd(): Completable
}
