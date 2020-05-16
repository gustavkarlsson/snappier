package se.gustavkarlsson.snappier.serialization.protobuf

import se.gustavkarlsson.snappier.message.SenderMessage
import se.gustavkarlsson.snappier.protobuf.ProtoSender
import se.gustavkarlsson.snappier.serialization.SenderMessageDeserializer
import se.gustavkarlsson.snappier.serialization.SenderMessageSerializer

object ProtobufSenderMessageSerializer : SenderMessageSerializer, SenderMessageDeserializer {
    override fun serialize(message: SenderMessage): ByteArray = message.toProto().toByteArray()

    override fun deserialize(data: ByteArray): SenderMessage = ProtoSender.Body.parseFrom(data).toMessage()
}
