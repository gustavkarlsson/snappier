package se.gustavkarlsson.snappier.serialization

import se.gustavkarlsson.snappier.message.ReceiverMessage

interface ReceiverMessageDeserializer {
    fun deserialize(data: ByteArray): ReceiverMessage
}
