package se.gustavkarlsson.snappier.receiver.files.default

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import se.gustavkarlsson.snappier.receiver.files.FileWriter
import java.io.BufferedOutputStream
import java.io.File
import java.util.concurrent.Executors

class DefaultFileWriter(
    private val writeBufferSize: Int = DEFAULT_BUFFER_SIZE,
    private val scheduler: Scheduler = Schedulers.from(Executors.newSingleThreadExecutor())
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
