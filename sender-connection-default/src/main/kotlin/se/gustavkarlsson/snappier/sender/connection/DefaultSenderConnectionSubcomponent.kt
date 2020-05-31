package se.gustavkarlsson.snappier.sender.connection

import dagger.Subcomponent

@Subcomponent(
    modules = [
        DefaultSenderConnection.Binding::class
    ]
)
interface DefaultSenderConnectionSubcomponent {
    @Subcomponent.Builder
    interface Builder {
        fun build(): DefaultSenderConnectionSubcomponent
    }

    fun senderConnection(): SenderConnection
}
