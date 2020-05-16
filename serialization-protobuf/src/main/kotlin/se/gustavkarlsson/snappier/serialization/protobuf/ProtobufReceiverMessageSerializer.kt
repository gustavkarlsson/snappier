package se.gustavkarlsson.snappier.serialization.protobuf

import se.gustavkarlsson.snappier.message.ReceiverMessage
import se.gustavkarlsson.snappier.serialization.ReceiverMessageSerializer

object ProtobufReceiverMessageSerializer : ReceiverMessageSerializer {
    override fun serialize(message: ReceiverMessage): ByteArray = message.toProto().toByteArray()
}
