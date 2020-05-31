package se.gustavkarlsson.snappier.sender.connection

import dagger.Binds
import dagger.Module
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.core.Single
import mu.KotlinLogging
import se.gustavkarlsson.snappier.common.config.ProtocolVersion
import se.gustavkarlsson.snappier.common.domain.Bytes
import se.gustavkarlsson.snappier.common.domain.FileRef
import se.gustavkarlsson.snappier.common.message.ReceiverMessage
import se.gustavkarlsson.snappier.common.message.SenderMessage
import se.gustavkarlsson.snappier.common.message.TransferFile
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

internal class DefaultSenderConnection @Inject constructor(
    incoming: Observable<ReceiverMessage>,
    private val outgoing: Observer<SenderMessage>,
    @ProtocolVersion private val protocolVersion: Int
) : SenderConnection {

    private val open = AtomicBoolean(true)

    private fun checkOpen() = check(open.get()) { "Connection is closed" }

    override fun close() = open.set(false)

    override val incoming: Observable<SenderConnection.ReceivedEvent> =
        incoming
            .doOnNext { checkOpen() }
            .doOnNext { logger.info { "Incoming message: $it" } }
            .map { message ->
                when (message) {
                    is ReceiverMessage.Handshake -> SenderConnection.ReceivedEvent.Handshake(message.protocolVersion)
                    is ReceiverMessage.AcceptedPaths -> SenderConnection.ReceivedEvent.AcceptedPaths(message.transferPaths)
                }
            }
            .onErrorReturn { SenderConnection.ReceivedEvent.Error(it) }

    override fun sendHandshake(): Single<SenderConnection.SendResult> =
        send(SenderMessage.Handshake(protocolVersion))

    override fun sendIntendedFiles(files: Collection<FileRef>): Single<SenderConnection.SendResult> =
        send(SenderMessage.IntendedFiles(files.map(FileRef::toTransferFile)))

    override fun sendFileStart(path: String): Single<SenderConnection.SendResult> =
        send(SenderMessage.FileStart(path))

    override fun sendFileData(data: ByteArray): Single<SenderConnection.SendResult> =
        send(SenderMessage.FileData(Bytes(data)))

    override fun sendFileEnd(): Single<SenderConnection.SendResult> =
        send(SenderMessage.FileEnd)

    private fun send(message: SenderMessage) =
        Completable.fromAction(::checkOpen)
            .andThen(Completable.fromAction { outgoing.onNext(message) })
            .toSingleDefault<SenderConnection.SendResult>(SenderConnection.SendResult.Success)
            .onErrorReturn { SenderConnection.SendResult.Error(it) }

    @Module
    abstract class Binding {
        @Binds
        abstract fun bind(implementation: DefaultSenderConnection): SenderConnection
    }
}

private fun FileRef.toTransferFile() = TransferFile(transferPath, size)
