package se.gustavkarlsson.snappier.receiver.files

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import se.gustavkarlsson.snappier.common.config.FileBufferSize
import java.io.BufferedOutputStream
import java.io.File
import javax.inject.Inject

internal class DefaultFileWriter @Inject constructor(
    @FileBufferSize private val writeBufferSize: Int,
    @DefaultFileWriterScheduler private val scheduler: Scheduler
) : FileWriter {
    private var currentStream: BufferedOutputStream? = null

    override fun create(path: String): Single<FileWriter.Result> =
        actionWithErrorHandling {
            check(currentStream == null) { "Stream is not null: $currentStream" }
            val file = File(path)
            file.parentFile.mkdirs()
            val created = file.createNewFile()
            check(created) { "File already exists: $file" }
            currentStream = file.outputStream().buffered(writeBufferSize)
        }

    override fun write(data: ByteArray): Single<FileWriter.Result> =
        actionWithErrorHandling {
            val stream = checkNotNull(currentStream) { "Stream is null" }
            stream.write(data)
        }

    override fun close(): Single<FileWriter.Result> =
        actionWithErrorHandling {
            val stream = checkNotNull(currentStream) { "Stream is null" }
            stream.flush()
            stream.close()
            currentStream = null
        }

    private fun actionWithErrorHandling(block: () -> Unit) =
        Completable.fromAction(block)
            .toSingleDefault<FileWriter.Result>(FileWriter.Result.Success)
            .onErrorReturn { FileWriter.Result.Error(it) }
            .subscribeOn(scheduler)
}
