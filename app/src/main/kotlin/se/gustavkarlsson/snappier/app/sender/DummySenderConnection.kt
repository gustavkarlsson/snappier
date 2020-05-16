package se.gustavkarlsson.snappier.app.sender

import se.gustavkarlsson.snappier.app.PROTOCOL_VERSION
import se.gustavkarlsson.snappier.app.BUFFER_SIZE
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import mu.KotlinLogging
import se.gustavkarlsson.snappier.message.File
import se.gustavkarlsson.snappier.message.ReceiverMessage
import se.gustavkarlsson.snappier.message.SenderMessage

private val logger = KotlinLogging.logger {}

class DummySenderConnection(
    incoming: Observable<ReceiverMessage>,
    private val outgoing: Observer<SenderMessage>,
    private val protocolVersion: Int = PROTOCOL_VERSION,
    private val bufferSize: Int = BUFFER_SIZE
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
        Completable.fromAction { outgoing.onNext(SenderMessage.Handshake(protocolVersion)) }

    override fun sendIntendedFiles(files: Set<File>): Completable =
        Completable.fromAction { outgoing.onNext(SenderMessage.IntendedFiles(files)) }

    override fun sendFile(file: File): Observable<Long> =
        Observable.fromIterable(LongRange(1, file.size))
            .buffer(bufferSize)
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
