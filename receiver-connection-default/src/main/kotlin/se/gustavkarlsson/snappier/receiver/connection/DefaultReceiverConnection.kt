package se.gustavkarlsson.snappier.receiver.connection

import dagger.Binds
import dagger.Module
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.core.Single
import mu.KotlinLogging
import se.gustavkarlsson.snappier.common.config.ProtocolVersion
import se.gustavkarlsson.snappier.common.message.ReceiverMessage
import se.gustavkarlsson.snappier.common.message.SenderMessage
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

private val logger = KotlinLogging.logger {}

internal class DefaultReceiverConnection @Inject constructor(
    incoming: Observable<SenderMessage>,
    private val outgoing: Observer<ReceiverMessage>,
    @ProtocolVersion private val protocolVersion: Int
) : ReceiverConnection {

    private val open = AtomicBoolean(true)

    private fun checkOpen() = check(open.get()) { "Connection is closed" }

    override fun close() = open.set(false)

    override val incoming: Observable<ReceiverConnection.ReceivedEvent> =
        incoming
            .doOnNext { checkOpen() }
            .doOnNext { logger.info { "Incoming message: $it" } }
            .map { message ->
                when (message) {
                    is SenderMessage.Handshake -> ReceiverConnection.ReceivedEvent.Handshake(
                        message.protocolVersion
                    )
                    is SenderMessage.IntendedFiles -> ReceiverConnection.ReceivedEvent.IntendedFiles(
                        message.files
                    )
                    is SenderMessage.FileStart -> ReceiverConnection.ReceivedEvent.FileStart(
                        message.path
                    )
                    is SenderMessage.FileData -> ReceiverConnection.ReceivedEvent.FileData(
                        message.data
                    )
                    SenderMessage.FileEnd -> ReceiverConnection.ReceivedEvent.FileEnd
                }
            }
            .onErrorReturn {
                ReceiverConnection.ReceivedEvent.Error(
                    it
                )
            }

    override fun sendHandshake(): Single<ReceiverConnection.SendResult> =
        send(ReceiverMessage.Handshake(protocolVersion))

    override fun sendAcceptedPaths(transferPaths: Collection<String>): Single<ReceiverConnection.SendResult> =
        send(ReceiverMessage.AcceptedPaths(transferPaths))

    private fun send(message: ReceiverMessage) =
        Completable.fromAction(::checkOpen)
            .andThen(Completable.fromAction { outgoing.onNext(message) })
            .toSingleDefault<ReceiverConnection.SendResult>(
                ReceiverConnection.SendResult.Success
            )
            .onErrorReturn {
                ReceiverConnection.SendResult.Error(
                    it
                )
            }

    @Module
    abstract class Binding {
        @Binds
        abstract fun bind(implementation: DefaultReceiverConnection): ReceiverConnection
    }
}
