package se.gustavkarlsson.snappier.serialization.protobuf

import se.gustavkarlsson.snappier.message.ReceiverMessage
import se.gustavkarlsson.snappier.protobuf.ProtoReceiver
import se.gustavkarlsson.snappier.serialization.ReceiverMessageDeserializer
import se.gustavkarlsson.snappier.serialization.ReceiverMessageSerializer

object ProtobufReceiverMessageSerializer : ReceiverMessageSerializer, ReceiverMessageDeserializer {
    override fun serialize(message: ReceiverMessage): ByteArray = message.toProto().toByteArray()

    override fun deserialize(data: ByteArray): ReceiverMessage = ProtoReceiver.Body.parseFrom(data).toMessage()
}
