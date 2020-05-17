package se.gustavkarlsson.snappier.common.message

sealed class ReceiverMessage {
    data class Handshake(val protocolVersion: Int) : ReceiverMessage()
    data class AcceptedPaths(val transferPaths: Collection<String>) : ReceiverMessage()
}
