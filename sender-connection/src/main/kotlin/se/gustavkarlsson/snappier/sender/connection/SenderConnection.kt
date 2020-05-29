package se.gustavkarlsson.snappier.sender.connection

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import se.gustavkarlsson.snappier.common.domain.FileRef

interface SenderConnection {
    sealed class Event {
        data class HandshakeReceived(val protocolVersion: Int) : Event()
        data class AcceptedPathsReceived(val transferPaths: Collection<String>) : Event()
        data class Error(val cause: Throwable) : Event()
    }

    val incoming: Observable<Event>

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
