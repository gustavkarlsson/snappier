package se.gustavkarlsson.snappier.receiver.connection.default

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import mu.KotlinLogging
import se.gustavkarlsson.snappier.common.message.ReceiverMessage
import se.gustavkarlsson.snappier.common.message.SenderMessage
import se.gustavkarlsson.snappier.receiver.connection.ReceiverConnection

private val logger = KotlinLogging.logger {}

class DefaultReceiverConnection(
    incoming: Observable<SenderMessage>,
    private val outgoing: Observer<ReceiverMessage>,
    private val protocolVersion: Int
) : ReceiverConnection {

    override val incoming: Observable<ReceiverConnection.Event> =
        incoming
            .doOnNext { logger.info { "Incoming message: $it" } }
            .map { message ->
                when (message) {
                    is SenderMessage.Handshake -> ReceiverConnection.Event.Handshake(message.protocolVersion)
                    is SenderMessage.IntendedFiles -> ReceiverConnection.Event.IntendedFiles(message.files)
                    is SenderMessage.FileStart -> ReceiverConnection.Event.NewFile(message.path)
                    is SenderMessage.FileData -> ReceiverConnection.Event.FileDataReceived(message.data)
                    SenderMessage.FileEnd -> ReceiverConnection.Event.FileCompleted
                }
            }

    override fun sendHandshake(): Completable =
        Completable.fromAction { outgoing.onNext(ReceiverMessage.Handshake(protocolVersion)) }

    override fun sendAcceptedPaths(transferPaths: Collection<String>): Completable =
        Completable.fromAction { outgoing.onNext(ReceiverMessage.AcceptedPaths(transferPaths)) }
}
