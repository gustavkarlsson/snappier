package se.gustavkarlsson.snappier.common.message

sealed class SenderMessage {
    data class Handshake(val protocolVersion: Int) : SenderMessage()
    data class IntendedFiles(val files: Collection<TransferFile>) : SenderMessage()
    data class FileStart(val path: String) : SenderMessage()
    data class FileData(val data: ByteArray) : SenderMessage() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as FileData

            if (!data.contentEquals(other.data)) return false

            return true
        }

        override fun hashCode(): Int {
            return data.contentHashCode()
        }

        override fun toString(): String = "FileData(size=${data.size})"
    }

    object FileEnd : SenderMessage()
}
