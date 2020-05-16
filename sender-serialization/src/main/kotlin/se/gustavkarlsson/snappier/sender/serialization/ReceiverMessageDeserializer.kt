package se.gustavkarlsson.snappier.sender.serialization

import se.gustavkarlsson.snappier.common.message.ReceiverMessage

interface ReceiverMessageDeserializer {
    fun deserialize(data: ByteArray): ReceiverMessage
}
