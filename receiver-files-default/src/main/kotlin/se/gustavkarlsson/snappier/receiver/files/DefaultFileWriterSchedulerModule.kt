package se.gustavkarlsson.snappier.receiver.files

import dagger.Module
import dagger.Provides
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import java.util.concurrent.Executors

@Module
internal class DefaultFileWriterSchedulerModule {

    @Provides
    @DefaultFileWriterScheduler
    fun provideFileWriterScheduler(): Scheduler =
        Schedulers.from(Executors.newSingleThreadExecutor()) // TODO Different thread per file
}
