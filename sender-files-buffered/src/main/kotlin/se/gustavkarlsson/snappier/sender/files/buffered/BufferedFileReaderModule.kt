package se.gustavkarlsson.snappier.sender.files.buffered

import dagger.Binds
import dagger.Module
import se.gustavkarlsson.snappier.sender.files.FileReader
import javax.inject.Singleton

@Module
abstract class BufferedFileReaderModule {

    @Binds
    @Singleton
    internal abstract fun bind(implementation: BufferedFileReader): FileReader
}
