package se.gustavkarlsson.snappier.receiver.connection

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import se.gustavkarlsson.snappier.common.domain.Bytes
import se.gustavkarlsson.snappier.common.message.TransferFile

interface ReceiverConnection : AutoCloseable {
    sealed class ReceivedEvent {
        data class Handshake(val protocolVersion: Int) : ReceivedEvent()
        data class IntendedFiles(val files: Collection<TransferFile>) : ReceivedEvent()
        data class FileStart(val path: String) : ReceivedEvent()
        data class FileData(val data: Bytes) : ReceivedEvent()
        object FileEnd : ReceivedEvent()
        data class Error(val cause: Throwable) : ReceivedEvent()
    }

    val incoming: Observable<ReceivedEvent>

    sealed class SendResult {
        object Success : SendResult()
        data class Error(val cause: Throwable) : SendResult()
    }

    fun sendHandshake(): Single<SendResult>

    fun sendAcceptedPaths(transferPaths: Collection<String>): Single<SendResult>
}
