package se.gustavkarlsson.snappier.receiver.files.default

import io.reactivex.rxjava3.core.Completable
import se.gustavkarlsson.snappier.receiver.files.FileWriter
import java.io.BufferedOutputStream
import java.io.File

class DefaultFileWriter(private val writeBufferSize: Int = DEFAULT_BUFFER_SIZE) : FileWriter {

    private var currentStream: BufferedOutputStream? = null

    override fun create(path: String): Completable =
        Completable.fromAction {
            synchronized(this) {
                check(currentStream == null) { "Stream is not null: $currentStream" }
                File("received").mkdir() // FIXME don't hardcode this directory
                val file = File("received/$path") // FIXME don't hardcode this directory
                val created = file.createNewFile()
                check(created) { "File already exists: $file" }
                currentStream = file.outputStream().buffered(writeBufferSize)
            }
        }

    override fun write(byte: ByteArray): Completable =
        Completable.fromAction {
            synchronized(this) {
                val stream = checkNotNull(currentStream) { "File is null" }
                stream.write(byte)
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
