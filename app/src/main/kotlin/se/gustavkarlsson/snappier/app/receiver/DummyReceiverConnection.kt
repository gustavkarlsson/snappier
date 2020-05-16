package se.gustavkarlsson.snappier.app.receiver

import se.gustavkarlsson.snappier.app.PROTOCOL_VERSION
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import mu.KotlinLogging
import se.gustavkarlsson.snappier.message.File
import se.gustavkarlsson.snappier.message.ReceiverMessage
import se.gustavkarlsson.snappier.message.SenderMessage

private val logger = KotlinLogging.logger {}

class DummyReceiverConnection(
    incoming: Observable<SenderMessage>,
    private val outgoing: Observer<ReceiverMessage>,
    private val protocolVersion: Int = PROTOCOL_VERSION
) : ReceiverConnection {

    override val incoming: Observable<ReceiverConnection.Event> =
        incoming
            .doOnNext { logger.info { "Incoming message: $it" } }
            .map { message ->
                when (message) {
                    is SenderMessage.Handshake -> ReceiverConnection.Event.Handshake(message.protocolVersion)
                    is SenderMessage.IntendedFiles -> ReceiverConnection.Event.IntendedFiles(message.files)
                    is SenderMessage.FileStart -> ReceiverConnection.Event.NewFile(message.file)
                    is SenderMessage.FileData -> ReceiverConnection.Event.FileDataReceived(message.data.size.toLong())
                    SenderMessage.FileEnd -> ReceiverConnection.Event.FileCompleted
                }
            }

    override fun sendHandshake(): Completable =
        Completable.fromAction { outgoing.onNext(ReceiverMessage.Handshake(protocolVersion)) }

    override fun sendAcceptedFiles(files: Set<File>): Completable =
        Completable.fromAction { outgoing.onNext(ReceiverMessage.AcceptedFiles(files)) }
}
