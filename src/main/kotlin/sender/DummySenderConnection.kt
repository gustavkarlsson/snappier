package sender

import common.File
import common.ReceiverMessage
import common.SenderMessage
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class DummySenderConnection(
    incoming: Observable<ReceiverMessage>,
    private val outgoing: Observer<SenderMessage>
) : SenderConnection {
    override val incoming: Observable<SenderConnection.Event> =
        incoming
            .doOnNext { logger.info { "Incoming message: $it" } }
            .map { message ->
                when (message) {
                    is ReceiverMessage.Handshake -> SenderConnection.Event.Handshake(message.protocolVersion)
                    is ReceiverMessage.AcceptedFiles -> SenderConnection.Event.AcceptedFiles(message.files)
                }
            }

    override fun sendHandshake(): Completable =
        Completable.fromAction { outgoing.onNext(SenderMessage.Handshake(1)) }

    override fun sendIntendedFiles(files: Set<File>): Completable =
        Completable.fromAction { outgoing.onNext(SenderMessage.IntendedFiles(files)) }

    override fun sendFile(file: File): Observable<Long> =
        Observable.fromIterable(LongRange(1, file.size))
            .buffer(32)
            .concatMap { sendBytesAndUpdateProgress(it) }
            .doOnSubscribe { outgoing.onNext(SenderMessage.FileStart(file)) }
            .doOnComplete { outgoing.onNext(SenderMessage.FileEnd) }

    private fun sendBytesAndUpdateProgress(values: List<Long>): Observable<Long> =
        Observable.fromCallable {
            val bytes = values.map { it.toByte() }.toByteArray()
            outgoing.onNext(SenderMessage.FileData(bytes))
            bytes.size.toLong()
        }
}
