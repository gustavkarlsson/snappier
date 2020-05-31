package se.gustavkarlsson.snappier.receiver.statemachine.knot

import dagger.Subcomponent
import se.gustavkarlsson.snappier.receiver.connection.ReceiverConnectionModule
import se.gustavkarlsson.snappier.receiver.statemachine.ReceiverStateMachine

@Subcomponent(
    modules = [
        KnotReceiverStateMachine.Binding::class,
        ReceiverConnectionModule::class
    ]
)
interface KnotReceiverStateMachineSubcomponent {
    @Subcomponent.Builder
    interface Builder {
        fun receiverConnectionModule(receiverConnectionModule: ReceiverConnectionModule): Builder
        fun build(): KnotReceiverStateMachineSubcomponent
    }

    fun receiverStateMachine(): ReceiverStateMachine
}
