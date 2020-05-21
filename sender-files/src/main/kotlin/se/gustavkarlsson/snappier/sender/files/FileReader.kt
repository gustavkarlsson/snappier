package se.gustavkarlsson.snappier.sender.files

import io.reactivex.rxjava3.core.Flowable

interface FileReader {
    fun readFile(path: String): Flowable<ByteArray>
}
