package se.gustavkarlsson.snappier.serialization

import se.gustavkarlsson.snappier.message.SenderMessage

interface SenderMessageDeserializer {
    fun deserialize(data: ByteArray): SenderMessage
}
