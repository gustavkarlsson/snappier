package receiver

import common.File
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable

interface ReceiverConnection {
    val incoming: Observable<Event>

    sealed class Event {
        data class Handshake(val protocolVersion: Int) : Event()
        data class IntendedFiles(val files: Set<File>) : Event()
        data class NewFile(val file: File) : Event()
        data class FileDataReceived(val received: Long) : Event()
        object FileCompleted : Event()
    }

    fun sendHandshake(): Completable

    fun sendAcceptedFiles(files: Set<File>): Completable
}
