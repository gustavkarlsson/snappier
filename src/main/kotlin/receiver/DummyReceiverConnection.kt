package receiver

import common.File
import common.ReceiverMessage
import common.SenderMessage
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.Subject
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class DummyReceiverConnection(
    incomingObservable: Observable<SenderMessage>,
    private val outgoingSubject: Subject<ReceiverMessage>
) : ReceiverConnection {

    override val incoming: Observable<ReceiverConnection.Event> =
        incomingObservable
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
        Completable.fromAction { outgoingSubject.onNext(ReceiverMessage.Handshake(1)) }

    override fun sendAcceptedFiles(files: Set<File>): Completable =
        Completable.fromAction { outgoingSubject.onNext(ReceiverMessage.AcceptedFiles(files)) }
}
