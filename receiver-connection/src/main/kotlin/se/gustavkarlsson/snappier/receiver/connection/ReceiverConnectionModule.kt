package se.gustavkarlsson.snappier.receiver.connection

import dagger.Module
import dagger.Provides

@Module
class ReceiverConnectionModule(private val receiverConnection: ReceiverConnection) {

    @Provides
    internal fun provideReceiverConnection(): ReceiverConnection = receiverConnection
}
