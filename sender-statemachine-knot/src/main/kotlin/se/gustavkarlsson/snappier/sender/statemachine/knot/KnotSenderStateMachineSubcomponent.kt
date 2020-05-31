package se.gustavkarlsson.snappier.sender.statemachine.knot

import dagger.Subcomponent
import se.gustavkarlsson.snappier.sender.connection.SenderConnectionModule
import se.gustavkarlsson.snappier.sender.statemachine.SenderStateMachine

@Subcomponent(
    modules = [
        KnotSenderStateMachine.Binding::class,
        SenderConnectionModule::class
    ]
)
interface KnotSenderStateMachineSubcomponent {
    @Subcomponent.Builder
    interface Builder {
        fun senderConnectionModule(senderConnectionModule: SenderConnectionModule): Builder
        fun build(): KnotSenderStateMachineSubcomponent
    }

    fun senderStateMachine(): SenderStateMachine
}
