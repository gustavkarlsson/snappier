package se.gustavkarlsson.snappier.sender.files.buffered

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers
import se.gustavkarlsson.snappier.sender.files.FileReader
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.InputStream
import kotlin.collections.Iterator as KotlinIterator

class BufferedFileReader(
    private val readBufferSize: Int = DEFAULT_BUFFER_SIZE,
    private val chunkBufferSize: Int = DEFAULT_BUFFER_SIZE,
    private val scheduler: Scheduler = Schedulers.io()
) : FileReader {
    override fun readFile(path: String): Flowable<ByteArray> =
        Single
            .fromCallable { FileInputStream(path).iterableBuffered(readBufferSize, chunkBufferSize) }
            .flatMapPublisher { stream ->
                Flowable.using(
                    { stream },
                    { Flowable.fromIterable(it).subscribeOn(scheduler) },
                    InputStream::close
                ).filter { it.isNotEmpty() }
            }
}

private fun InputStream.iterableBuffered(
    readBufferSize: Int,
    iteratorBufferSize: Int
): IterableBufferedInputStream =
    IterableBufferedInputStream(
        this,
        readBufferSize,
        iteratorBufferSize
    )

private class IterableBufferedInputStream(
    inputStream: InputStream,
    readBufferSize: Int,
    private val iteratorBufferSize: Int
) : BufferedInputStream(inputStream, readBufferSize), Iterable<ByteArray> {

    override fun iterator(): KotlinIterator<ByteArray> = BufferedIterator()

    private inner class BufferedIterator : KotlinIterator<ByteArray> {
        private var hasMore = true

        override fun hasNext(): Boolean = hasMore

        override fun next(): ByteArray {
            val buffer = ByteArray(iteratorBufferSize)
            val read = read(buffer)
            hasMore = read > -1
            return when {
                read == -1 -> EMPTY_BYTE_ARRAY
                read < buffer.size -> buffer.copyOf(read)
                else -> buffer
            }
        }

    }
}

private val EMPTY_BYTE_ARRAY = byteArrayOf()
