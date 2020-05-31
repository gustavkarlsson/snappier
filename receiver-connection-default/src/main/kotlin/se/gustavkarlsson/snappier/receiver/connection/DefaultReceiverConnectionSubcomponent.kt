package se.gustavkarlsson.snappier.receiver.connection

import dagger.Subcomponent

@Subcomponent(
    modules = [
        DefaultReceiverConnection.Binding::class,
        ReceiverMessageStreamsModule::class
    ]
)
interface DefaultReceiverConnectionSubcomponent {
    @Subcomponent.Builder
    interface Builder {
        fun messageStreamsModule(receiverMessageStreamsModule: ReceiverMessageStreamsModule): Builder
        fun build(): DefaultReceiverConnectionSubcomponent
    }

    fun receiverConnection(): ReceiverConnection
}
