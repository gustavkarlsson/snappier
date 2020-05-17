package se.gustavkarlsson.snappier.sender.serialization.protobuf

import se.gustavkarlsson.snappier.common.message.ReceiverMessage
import se.gustavkarlsson.snappier.protobuf.ProtoReceiver
import se.gustavkarlsson.snappier.sender.serialization.ReceiverMessageDeserializer

object ProtobufReceiverMessageDeserializer : ReceiverMessageDeserializer {
    override fun deserialize(data: ByteArray): ReceiverMessage = ProtoReceiver.Body.parseFrom(data).toMessage()
}

// TODO Conversion to message objects doesn't fail reliably. Needs validation?
private fun ProtoReceiver.Body.toMessage(): ReceiverMessage =
    when (messageCase!!) {
        ProtoReceiver.Body.MessageCase.HANDSHAKE -> handshake.toMessage()
        ProtoReceiver.Body.MessageCase.ACCEPTEDPATHS -> acceptedPaths.toMessage()
        ProtoReceiver.Body.MessageCase.MESSAGE_NOT_SET -> error("Message not set")
    }

private fun ProtoReceiver.Handshake.toMessage(): ReceiverMessage.Handshake =
    ReceiverMessage.Handshake(protocolVersion)

private fun ProtoReceiver.AcceptedPaths.toMessage(): ReceiverMessage.AcceptedPaths =
    ReceiverMessage.AcceptedPaths(pathList)
