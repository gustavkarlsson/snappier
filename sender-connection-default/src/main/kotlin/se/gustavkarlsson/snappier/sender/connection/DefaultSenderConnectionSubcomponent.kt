package se.gustavkarlsson.snappier.sender.connection

import dagger.Subcomponent

@Subcomponent(
    modules = [
        DefaultSenderConnection.Binding::class,
        SenderMessageStreamsModule::class
    ]
)
interface DefaultSenderConnectionSubcomponent {
    @Subcomponent.Builder
    interface Builder {
        fun messageStreamsModule(senderMessageStreamsModule: SenderMessageStreamsModule): Builder
        fun build(): DefaultSenderConnectionSubcomponent
    }

    fun senderConnection(): SenderConnection
}
