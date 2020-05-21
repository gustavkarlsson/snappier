package se.gustavkarlsson.snappier.sender.connection.default

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import mu.KotlinLogging
import se.gustavkarlsson.snappier.common.domain.Bytes
import se.gustavkarlsson.snappier.common.message.TransferFile
import se.gustavkarlsson.snappier.common.message.ReceiverMessage
import se.gustavkarlsson.snappier.common.message.SenderMessage
import se.gustavkarlsson.snappier.sender.connection.SenderConnection
import se.gustavkarlsson.snappier.common.domain.FileRef

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

    override fun sendHandshake(): Completable =
        Completable.fromAction { outgoing.onNext(SenderMessage.Handshake(protocolVersion)) }

    override fun sendIntendedFiles(files: Collection<FileRef>): Completable =
        Completable.fromAction {
            val transferFiles = files.map(FileRef::toTransferFile)
            outgoing.onNext(SenderMessage.IntendedFiles(transferFiles))
        }

    override fun sendFileStart(path: String): Completable =
        Completable.fromAction { outgoing.onNext(SenderMessage.FileStart(path)) }

    override fun sendFileData(data: ByteArray): Completable =
        Completable.fromAction { outgoing.onNext(SenderMessage.FileData(Bytes(data))) }

    override fun sendFileEnd(): Completable =
        Completable.fromAction { outgoing.onNext(SenderMessage.FileEnd) }
}

private fun FileRef.toTransferFile(): TransferFile = TransferFile(transferPath, size)
