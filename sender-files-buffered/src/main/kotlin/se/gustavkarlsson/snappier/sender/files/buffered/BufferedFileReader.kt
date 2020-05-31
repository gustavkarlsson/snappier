package se.gustavkarlsson.snappier.sender.files.buffered

import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.core.Single
import se.gustavkarlsson.snappier.common.config.FileBufferSize
import se.gustavkarlsson.snappier.common.config.IoScheduler
import se.gustavkarlsson.snappier.common.config.NetworkBufferSize
import se.gustavkarlsson.snappier.common.domain.Bytes
import se.gustavkarlsson.snappier.sender.files.FileReader
import java.io.BufferedInputStream
import java.io.FileInputStream
import java.io.InputStream
import javax.inject.Inject
import kotlin.collections.Iterator as KotlinIterator

internal class BufferedFileReader @Inject constructor(
    @FileBufferSize private val readBufferSize: Int,
    @NetworkBufferSize val chunkBufferSize: Int,
    @IoScheduler val scheduler: Scheduler
) : FileReader {
    override fun readFile(path: String): Flowable<FileReader.Result> =
        Single
            .fromCallable { FileInputStream(path).iterableBuffered(readBufferSize, chunkBufferSize) }
            .flatMapPublisher { stream ->
                Flowable.using(
                    { stream },
                    { Flowable.fromIterable(it) },
                    InputStream::close
                ).filter { it.isNotEmpty() }
            }
            .map<FileReader.Result> { FileReader.Result.Success(Bytes(it)) }
            .onErrorReturn { FileReader.Result.Error(it) }
            .subscribeOn(scheduler)
}

private fun InputStream.iterableBuffered(readBufferSize: Int, iteratorBufferSize: Int): IterableBufferedInputStream =
    IterableBufferedInputStream(this, readBufferSize, iteratorBufferSize)

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
            return when {
                read == -1 -> EMPTY_BYTE_ARRAY.also { hasMore = false }
                read < buffer.size -> buffer.copyOf(read)
                else -> buffer
            }
        }
    }
}

private val EMPTY_BYTE_ARRAY = byteArrayOf()
