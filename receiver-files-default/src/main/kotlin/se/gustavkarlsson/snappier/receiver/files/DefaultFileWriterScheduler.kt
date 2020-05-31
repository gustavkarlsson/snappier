package se.gustavkarlsson.snappier.receiver.files

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.RUNTIME)
internal annotation class DefaultFileWriterScheduler
