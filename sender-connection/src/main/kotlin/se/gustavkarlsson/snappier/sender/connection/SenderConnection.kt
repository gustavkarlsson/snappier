package se.gustavkarlsson.snappier.sender.connection

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import se.gustavkarlsson.snappier.common.domain.FileRef

interface SenderConnection : AutoCloseable {
    sealed class ReceivedEvent {
        data class Handshake(val protocolVersion: Int) : ReceivedEvent()
        data class AcceptedPaths(val transferPaths: Collection<String>) : ReceivedEvent()
        data class Error(val cause: Throwable) : ReceivedEvent()
    }

    val incoming: Observable<ReceivedEvent>

    sealed class SendResult {
        object Success : SendResult()
        data class Error(val cause: Throwable) : SendResult()
    }

    fun sendHandshake(): Single<SendResult>

    fun sendIntendedFiles(files: Collection<FileRef>): Single<SendResult>

    fun sendFileStart(path: String): Single<SendResult>

    fun sendFileData(data: ByteArray): Single<SendResult>

    fun sendFileEnd(): Single<SendResult>
}
