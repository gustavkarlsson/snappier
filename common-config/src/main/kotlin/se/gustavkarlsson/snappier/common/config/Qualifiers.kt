package se.gustavkarlsson.snappier.common.config

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class FileBufferSize

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ProtocolVersion

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class NetworkBufferSize

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class IoScheduler
