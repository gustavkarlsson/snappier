package se.gustavkarlsson.snappier.app

import dagger.Component
import se.gustavkarlsson.snappier.receiver.connection.DefaultReceiverConnectionSubcomponent
import se.gustavkarlsson.snappier.receiver.files.DefaultFileWriterModule
import se.gustavkarlsson.snappier.receiver.statemachine.knot.KnotReceiverStateMachineSubcomponent
import se.gustavkarlsson.snappier.sender.connection.DefaultSenderConnectionSubcomponent
import se.gustavkarlsson.snappier.sender.files.buffered.BufferedFileReaderModule
import se.gustavkarlsson.snappier.sender.statemachine.knot.KnotSenderStateMachineSubcomponent
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        ConfigModule::class,
        BufferedFileReaderModule::class,
        DefaultFileWriterModule::class,
        MessageStreamsModule::class
    ]
)
internal interface AppComponent {

    fun createKnotReceiverStateMachineSubcomponentBuilder(): KnotReceiverStateMachineSubcomponent.Builder

    fun createKnotSenderStateMachineSubcomponentBuilder(): KnotSenderStateMachineSubcomponent.Builder

    fun createDefaultSenderConnectionSubcomponentBuilder(): DefaultSenderConnectionSubcomponent.Builder

    fun createDefaultReceiverConnectionSubcomponentBuilder(): DefaultReceiverConnectionSubcomponent.Builder
}
