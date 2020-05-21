package se.gustavkarlsson.snappier.common.message

import se.gustavkarlsson.snappier.common.domain.Bytes

sealed class SenderMessage {
    data class Handshake(val protocolVersion: Int) : SenderMessage()
    data class IntendedFiles(val files: Collection<TransferFile>) : SenderMessage()
    data class FileStart(val path: String) : SenderMessage()
    data class FileData(val data: Bytes) : SenderMessage()
    object FileEnd : SenderMessage()
}
