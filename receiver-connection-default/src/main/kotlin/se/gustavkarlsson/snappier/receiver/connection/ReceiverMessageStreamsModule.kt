package se.gustavkarlsson.snappier.receiver.connection

import dagger.Module
import dagger.Provides
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import se.gustavkarlsson.snappier.common.message.ReceiverMessage
import se.gustavkarlsson.snappier.common.message.SenderMessage

@Module
class ReceiverMessageStreamsModule(
    private val incomingReceiverMessages: Observable<SenderMessage>,
    private val outgoingReceiverMessages: Observer<ReceiverMessage>
) {

    @Provides
    internal fun provideIncomingReceiverMessages(): Observable<SenderMessage> = incomingReceiverMessages

    @Provides
    internal fun provideOutgoingReceiverMessages(): Observer<ReceiverMessage> = outgoingReceiverMessages
}
