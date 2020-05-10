package sender

import common.File
import common.ReceiverMessage
import common.SenderMessage
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.subjects.Subject
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class DummySenderConnection(
    incomingObservable: Observable<ReceiverMessage>,
    private val outgoingSubject: Subject<SenderMessage>
) : SenderConnection {
    override val incoming: Observable<SenderConnection.Event> =
        incomingObservable
            .doOnNext { logger.info { "Incoming message: $it" } }
            .map { message ->
                when (message) {
                    is ReceiverMessage.Handshake -> SenderConnection.Event.Handshake(message.protocolVersion)
                    is ReceiverMessage.AcceptedFiles -> SenderConnection.Event.AcceptedFiles(message.files)
                }
            }

    override fun sendHandshake(): Completable =
        Completable.fromAction { outgoingSubject.onNext(SenderMessage.Handshake(1)) }

    override fun sendIntendedFiles(files: Set<File>): Completable =
        Completable.fromAction { outgoingSubject.onNext(SenderMessage.IntendedFiles(files)) }

    override fun sendFile(file: File): Observable<Long> =
        Observable.fromIterable(LongRange(1, file.size))
            .buffer(32)
            .concatMap { sendBytesAndUpdateProgress(it) }
            .doOnSubscribe { outgoingSubject.onNext(SenderMessage.FileStart(file)) }
            .doOnComplete { outgoingSubject.onNext(SenderMessage.FileEnd) }

    private fun sendBytesAndUpdateProgress(values: List<Long>): Observable<Long> =
        Observable.fromCallable {
            val bytes = values.map { it.toByte() }.toByteArray()
            outgoingSubject.onNext(SenderMessage.FileData(bytes))
            bytes.size.toLong()
        }
}
