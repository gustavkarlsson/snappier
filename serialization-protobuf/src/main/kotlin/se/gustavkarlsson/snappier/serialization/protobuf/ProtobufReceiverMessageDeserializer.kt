package se.gustavkarlsson.snappier.serialization.protobuf

import se.gustavkarlsson.snappier.message.ReceiverMessage
import se.gustavkarlsson.snappier.protobuf.ProtoReceiver
import se.gustavkarlsson.snappier.serialization.ReceiverMessageDeserializer

object ProtobufReceiverMessageDeserializer : ReceiverMessageDeserializer {
    override fun deserialize(data: ByteArray): ReceiverMessage = ProtoReceiver.Body.parseFrom(data).toMessage()
}
