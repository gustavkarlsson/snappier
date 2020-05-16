package se.gustavkarlsson.snappier.receiver.files.default

import io.reactivex.rxjava3.core.Completable
import se.gustavkarlsson.snappier.receiver.files.FileWriter
import java.io.BufferedOutputStream
import java.io.File

class DefaultFileWriter(private val writeBufferSize: Int = DEFAULT_BUFFER_SIZE) : FileWriter {

    private var currentStream: BufferedOutputStream? = null

    @Synchronized
    override fun create(path: String): Completable =
        Completable.fromAction {
            check(currentStream == null) { "Stream is not null: $currentStream" }
            val file = File(path)
            val created = file.createNewFile()
            check(created) { "File already exists: $file" }
            currentStream = file.outputStream().buffered(writeBufferSize)
        }

    @Synchronized
    override fun write(byte: ByteArray): Completable =
        Completable.fromAction {
            val stream = checkNotNull(currentStream) { "File is null" }
            stream.write(byte)
        }

    @Synchronized
    override fun close(): Completable =
        Completable.fromAction {
            val stream = checkNotNull(currentStream) { "File is null" }
            stream.flush()
            stream.close()
        }
}
