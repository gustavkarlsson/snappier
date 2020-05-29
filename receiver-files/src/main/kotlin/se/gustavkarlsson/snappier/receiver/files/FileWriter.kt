package se.gustavkarlsson.snappier.receiver.files

import io.reactivex.rxjava3.core.Completable

// TODO Use result class
interface FileWriter {
    fun create(path: String): Completable
    fun write(data: ByteArray): Completable
    fun close(): Completable
}
