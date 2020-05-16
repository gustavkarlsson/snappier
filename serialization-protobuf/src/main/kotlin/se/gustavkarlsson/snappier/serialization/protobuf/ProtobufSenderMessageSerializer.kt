package se.gustavkarlsson.snappier.serialization.protobuf

import se.gustavkarlsson.snappier.message.SenderMessage
import se.gustavkarlsson.snappier.serialization.SenderMessageSerializer

object ProtobufSenderMessageSerializer : SenderMessageSerializer {
    override fun serialize(message: SenderMessage): ByteArray = message.toProto().toByteArray()
}
