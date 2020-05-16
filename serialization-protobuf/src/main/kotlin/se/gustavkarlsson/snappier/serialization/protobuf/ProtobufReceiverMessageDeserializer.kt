package se.gustavkarlsson.snappier.serialization.protobuf

import se.gustavkarlsson.snappier.message.ReceiverMessage
import se.gustavkarlsson.snappier.protobuf.ProtoCommon
import se.gustavkarlsson.snappier.protobuf.ProtoReceiver
import se.gustavkarlsson.snappier.serialization.ReceiverMessageDeserializer

object ProtobufReceiverMessageDeserializer : ReceiverMessageDeserializer {
    override fun deserialize(data: ByteArray): ReceiverMessage = ProtoReceiver.Body.parseFrom(data).toMessage()
}

// TODO Conversion to message objects doesn't fail reliably. Needs validation?
private fun ProtoReceiver.Body.toMessage(): ReceiverMessage =
    when (messageCase!!) {
        ProtoReceiver.Body.MessageCase.HANDSHAKE -> handshake.toMessage()
        ProtoReceiver.Body.MessageCase.ACCEPTEDFILES -> acceptedFiles.toMessage()
        ProtoReceiver.Body.MessageCase.MESSAGE_NOT_SET -> error("Message not set")
    }

private fun ProtoReceiver.Handshake.toMessage(): ReceiverMessage.Handshake =
    ReceiverMessage.Handshake(protocolVersion)

private fun ProtoReceiver.AcceptedFiles.toMessage(): ReceiverMessage.AcceptedFiles =
    ReceiverMessage.AcceptedFiles(fileList.map(ProtoCommon.File::toMessage).toSet())
