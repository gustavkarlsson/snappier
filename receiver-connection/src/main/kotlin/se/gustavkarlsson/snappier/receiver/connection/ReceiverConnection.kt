package se.gustavkarlsson.snappier.receiver.connection

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import se.gustavkarlsson.snappier.common.message.TransferFile
import se.gustavkarlsson.snappier.common.message.SenderMessage

interface ReceiverConnection {
    val incoming: Observable<Event>

    sealed class Event {
        data class Handshake(val protocolVersion: Int) : Event()
        data class IntendedFiles(val files: Collection<TransferFile>) : Event()
        data class NewFile(val path: String) : Event()
        data class FileDataReceived(val data: ByteArray) : Event() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false

                other as SenderMessage.FileData

                if (!data.contentEquals(other.data)) return false

                return true
            }

            override fun hashCode(): Int {
                return data.contentHashCode()
            }

            override fun toString(): String = "FileDataReceived(size=${data.size})"
        }

        object FileCompleted : Event()
    }

    fun sendHandshake(): Completable

    fun sendAcceptedPaths(transferPaths: Collection<String>): Completable
}
