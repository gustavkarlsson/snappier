package se.gustavkarlsson.snappier.receiver.files.default

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Scheduler
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

    override fun create(path: String): Completable =
        completable {
            check(currentStream == null) { "Stream is not null: $currentStream" }
            val file = File(path)
            file.parentFile.mkdirs()
            val created = file.createNewFile()
            check(created) { "File already exists: $file" }
            currentStream = file.outputStream().buffered(writeBufferSize)
        }

    override fun write(data: ByteArray): Completable =
        completable {
            val stream = checkNotNull(currentStream) { "Stream is null" }
            stream.write(data)
        }

    override fun close(): Completable =
        completable {
            val stream = checkNotNull(currentStream) { "Stream is null" }
            stream.flush()
            stream.close()
            currentStream = null
        }

    private fun completable(block: () -> Unit) =
        Completable.fromAction(block).subscribeOn(scheduler)
}
