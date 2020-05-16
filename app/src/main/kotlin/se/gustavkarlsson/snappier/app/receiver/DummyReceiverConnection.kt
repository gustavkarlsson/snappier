package se.gustavkarlsson.snappier.app.receiver

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.core.Single
import mu.KotlinLogging
import se.gustavkarlsson.snappier.common.message.File
import se.gustavkarlsson.snappier.common.message.ReceiverMessage
import se.gustavkarlsson.snappier.common.message.SenderMessage
import se.gustavkarlsson.snappier.receiver.files.FileWriter

private val logger = KotlinLogging.logger {}

class DummyReceiverConnection(
    incoming: Observable<SenderMessage>,
    private val outgoing: Observer<ReceiverMessage>,
    private val fileWriter: FileWriter,
    private val protocolVersion: Int
) : ReceiverConnection {

    override val incoming: Observable<ReceiverConnection.Event> =
        incoming
            .doOnNext { logger.info { "Incoming message: $it" } }
            .flatMapSingle { message ->
                when (message) {
                    is SenderMessage.Handshake -> ReceiverConnection.Event.Handshake(message.protocolVersion).toSingle()
                    is SenderMessage.IntendedFiles -> ReceiverConnection.Event.IntendedFiles(message.files).toSingle()
                    is SenderMessage.FileStart -> createNewFile(message.file)
                    is SenderMessage.FileData -> writeToFile(message.data)
                    SenderMessage.FileEnd -> closeFile()
                }
            }

    private fun createNewFile(file: File): Single<ReceiverConnection.Event.NewFile> =
        fileWriter.create(file.path)
            .andThen(ReceiverConnection.Event.NewFile(file).toSingle())

    private fun writeToFile(bytes: ByteArray): Single<ReceiverConnection.Event.FileDataReceived> =
        fileWriter.write(bytes)
            .andThen(ReceiverConnection.Event.FileDataReceived(bytes.size.toLong()).toSingle())

    private fun closeFile(): Single<ReceiverConnection.Event.FileCompleted> =
        fileWriter.close()
            .andThen(ReceiverConnection.Event.FileCompleted.toSingle())

    override fun sendHandshake(): Completable =
        Completable.fromAction { outgoing.onNext(ReceiverMessage.Handshake(protocolVersion)) }

    override fun sendAcceptedFiles(files: Set<File>): Completable =
        Completable.fromAction { outgoing.onNext(ReceiverMessage.AcceptedFiles(files)) }
}

private fun <T> T.toSingle(): Single<T> = Single.just(this)
