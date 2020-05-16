package se.gustavkarlsson.snappier.app.sender

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import mu.KotlinLogging
import se.gustavkarlsson.snappier.common.message.File
import se.gustavkarlsson.snappier.common.message.ReceiverMessage
import se.gustavkarlsson.snappier.common.message.SenderMessage
import se.gustavkarlsson.snappier.sender.files.FileReader

private val logger = KotlinLogging.logger {}

class DummySenderConnection(
    incoming: Observable<ReceiverMessage>,
    private val outgoing: Observer<SenderMessage>,
    private val fileReader: FileReader,
    private val protocolVersion: Int
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

    override fun sendFile(file: File): Flowable<Long> =
        fileReader.readFile(file)
            .doOnNext { outgoing.onNext(SenderMessage.FileData(it)) }
            .map { it.size.toLong() }
            .doOnSubscribe { outgoing.onNext(SenderMessage.FileStart(file)) }
            .doOnComplete { outgoing.onNext(SenderMessage.FileEnd) }
}
