package se.gustavkarlsson.snappier.app

import dagger.Module
import dagger.Provides
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import se.gustavkarlsson.snappier.common.config.FileBufferSize
import se.gustavkarlsson.snappier.common.config.IoScheduler
import se.gustavkarlsson.snappier.common.config.NetworkBufferSize
import se.gustavkarlsson.snappier.common.config.ProtocolVersion
import javax.inject.Singleton

@Module
class ConfigModule {

    @Provides
    @Singleton
    @ProtocolVersion
    internal fun provideProtocolVersion(): Int = 1

    @Provides
    @Singleton
    @FileBufferSize
    internal fun provideFileBufferSize(): Int = 8192

    @Provides
    @Singleton
    @NetworkBufferSize
    internal fun provideNetworkBufferSize(): Int = 32768

    @Provides
    @Singleton
    @IoScheduler
    internal fun provideIoScheduler(): Scheduler = Schedulers.io()
}
