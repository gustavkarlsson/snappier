package se.gustavkarlsson.snappier.sender.files

import io.reactivex.rxjava3.core.Flowable
import se.gustavkarlsson.snappier.common.message.File

interface FileReader {
    fun readFile(file: File): Flowable<ByteArray>
}
