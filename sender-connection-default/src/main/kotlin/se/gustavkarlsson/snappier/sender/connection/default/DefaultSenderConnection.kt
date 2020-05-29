package se.gustavkarlsson.snappier.sender.connection.default

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.core.Single
import mu.KotlinLogging
import se.gustavkarlsson.snappier.common.domain.Bytes
import se.gustavkarlsson.snappier.common.domain.FileRef
import se.gustavkarlsson.snappier.common.message.ReceiverMessage
import se.gustavkarlsson.snappier.common.message.SenderMessage
import se.gustavkarlsson.snappier.common.message.TransferFile
import se.gustavkarlsson.snappier.sender.connection.SenderConnection

private val logger = KotlinLogging.logger {}

class DefaultSenderConnection(
    incoming: Observable<ReceiverMessage>,
    private val outgoing: Observer<SenderMessage>,
    private val protocolVersion: Int
) : SenderConnection {
    override val incoming: Observable<SenderConnection.Event> =
        incoming
            .doOnNext { logger.info { "Incoming message: $it" } }
            .map { message ->
                when (message) {
                    is ReceiverMessage.Handshake -> SenderConnection.Event.HandshakeReceived(message.protocolVersion)
                    is ReceiverMessage.AcceptedPaths -> SenderConnection.Event.AcceptedPathsReceived(message.transferPaths)
                }
            }
            .onErrorReturn { SenderConnection.Event.Error(it) }

    override fun sendHandshake(): Single<SenderConnection.SendResult> =
        actionWithErrorHandling { outgoing.onNext(SenderMessage.Handshake(protocolVersion)) }

    override fun sendIntendedFiles(files: Collection<FileRef>): Single<SenderConnection.SendResult> =
        actionWithErrorHandling {
            val transferFiles = files.map(FileRef::toTransferFile)
            outgoing.onNext(SenderMessage.IntendedFiles(transferFiles))
        }

    override fun sendFileStart(path: String): Single<SenderConnection.SendResult> =
        actionWithErrorHandling { outgoing.onNext(SenderMessage.FileStart(path)) }

    override fun sendFileData(data: ByteArray): Single<SenderConnection.SendResult> =
        actionWithErrorHandling { outgoing.onNext(SenderMessage.FileData(Bytes(data))) }

    override fun sendFileEnd(): Single<SenderConnection.SendResult> =
        actionWithErrorHandling { outgoing.onNext(SenderMessage.FileEnd) }
}

private fun actionWithErrorHandling(block: () -> Unit): Single<SenderConnection.SendResult> =
    Completable.fromAction(block)
        .toSingleDefault<SenderConnection.SendResult>(SenderConnection.SendResult.Success)
        .onErrorReturn { SenderConnection.SendResult.Error(it) }

private fun FileRef.toTransferFile(): TransferFile = TransferFile(transferPath, size)
