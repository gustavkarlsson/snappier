package se.gustavkarlsson.snappier.serialization.protobuf

import se.gustavkarlsson.snappier.message.SenderMessage
import se.gustavkarlsson.snappier.protobuf.ProtoSender
import se.gustavkarlsson.snappier.serialization.SenderMessageDeserializer

object ProtobufSenderMessageDeserializer : SenderMessageDeserializer {
    override fun deserialize(data: ByteArray): SenderMessage = ProtoSender.Body.parseFrom(data).toMessage()
}
