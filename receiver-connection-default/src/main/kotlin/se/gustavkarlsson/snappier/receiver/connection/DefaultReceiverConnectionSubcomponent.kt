package se.gustavkarlsson.snappier.receiver.connection

import dagger.Subcomponent

@Subcomponent(
    modules = [
        DefaultReceiverConnection.Binding::class
    ]
)
interface DefaultReceiverConnectionSubcomponent {
    @Subcomponent.Builder
    interface Builder {
        fun build(): DefaultReceiverConnectionSubcomponent
    }

    fun receiverConnection(): ReceiverConnection
}
