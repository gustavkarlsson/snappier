package se.gustavkarlsson.snappier.message

sealed class ReceiverMessage {
    data class Handshake(val protocolVersion: Int) : ReceiverMessage()
    data class AcceptedFiles(val files: Set<File>) : ReceiverMessage()
}
