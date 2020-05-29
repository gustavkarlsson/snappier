package se.gustavkarlsson.snappier.receiver.files

import io.reactivex.rxjava3.core.Single

interface FileWriter {
    sealed class Result {
        object Success : Result()
        data class Error(val cause: Throwable) : Result()
    }

    fun create(path: String): Single<Result>
    fun write(data: ByteArray): Single<Result>
    fun close(): Single<Result>
}
