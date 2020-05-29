package se.gustavkarlsson.snappier.sender.files

import io.reactivex.rxjava3.core.Flowable
import se.gustavkarlsson.snappier.common.domain.Bytes

interface FileReader {
    sealed class Result {
        data class Success(val data: Bytes) : Result()
        data class Error(val cause: Throwable) : Result()
    }

    fun readFile(path: String): Flowable<Result>
}
