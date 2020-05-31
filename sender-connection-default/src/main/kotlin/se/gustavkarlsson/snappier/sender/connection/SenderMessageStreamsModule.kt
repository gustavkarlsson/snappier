package se.gustavkarlsson.snappier.sender.connection

import dagger.Module
import dagger.Provides
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import se.gustavkarlsson.snappier.common.message.ReceiverMessage
import se.gustavkarlsson.snappier.common.message.SenderMessage

@Module
class SenderMessageStreamsModule(
    private val incomingSenderMessages: Observable<ReceiverMessage>,
    private val outgoingSenderMessages: Observer<SenderMessage>
) {

    @Provides
    internal fun provideIncomingSenderMessages(): Observable<ReceiverMessage> = incomingSenderMessages

    @Provides
    internal fun provideOutgoingSenderMessages(): Observer<SenderMessage> = outgoingSenderMessages
}
