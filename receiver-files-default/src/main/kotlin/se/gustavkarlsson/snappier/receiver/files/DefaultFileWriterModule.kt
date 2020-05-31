package se.gustavkarlsson.snappier.receiver.files

import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module(
    includes = [
        DefaultFileWriterSchedulerModule::class
    ]
)
abstract class DefaultFileWriterModule {

    @Binds
    @Singleton
    internal abstract fun bind(implementation: DefaultFileWriter): FileWriter
}
