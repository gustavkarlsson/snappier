package se.gustavkarlsson.snappier.sender.connection

import dagger.Module
import dagger.Provides

@Module
class SenderConnectionModule(private val senderConnection: SenderConnection) {

    @Provides
    internal fun provideSenderConnection(): SenderConnection = senderConnection
}
