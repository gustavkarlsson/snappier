package se.gustavkarlsson.snappier.app

import dagger.Component
import se.gustavkarlsson.snappier.sender.connection.DefaultSenderConnectionSubcomponent
import se.gustavkarlsson.snappier.sender.files.buffered.BufferedFileReaderModule
import se.gustavkarlsson.snappier.sender.statemachine.knot.KnotSenderStateMachineSubcomponent
import javax.inject.Singleton

@Singleton
@Component(
    modules = [
        ConfigModule::class,
        BufferedFileReaderModule::class
    ]
)
internal interface AppComponent {

    fun createKnotSenderStateMachineSubcomponentBuilder(): KnotSenderStateMachineSubcomponent.Builder

    fun createDefaultSenderConnectionSubcomponentBuilder(): DefaultSenderConnectionSubcomponent.Builder
}
