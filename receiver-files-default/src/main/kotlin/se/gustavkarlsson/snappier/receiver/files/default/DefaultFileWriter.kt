package se.gustavkarlsson.snappier.receiver.files.default

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import se.gustavkarlsson.snappier.receiver.files.FileWriter
import java.io.BufferedOutputStream
import java.io.File

class DefaultFileWriter(
    private val writeBufferSize: Int = DEFAULT_BUFFER_SIZE,
    private val scheduler: Scheduler = Schedulers.io() // TODO How to use schedulers intelligently here?
) : FileWriter {
    private var currentStream: BufferedOutputStream? = null

    override fun create(path: String): Completable =
        Completable.fromAction {
            synchronized(this) {
                check(currentStream == null) { "Stream is not null: $currentStream" }
                val file = File(path)
                file.parentFile.mkdirs()
                val created = file.createNewFile()
                check(created) { "File already exists: $file" }
                currentStream = file.outputStream().buffered(writeBufferSize)
            }
        }

    override fun write(data: ByteArray): Completable =
        Completable.fromAction {
            synchronized(this) {
                val stream = checkNotNull(currentStream) { "File is null" }
                stream.write(data)
            }
        }

    override fun close(): Completable =
        Completable.fromAction {
            synchronized(this) {
                val stream = checkNotNull(currentStream) { "File is null" }
                stream.flush()
                stream.close()
                currentStream = null
            }
        }
}
