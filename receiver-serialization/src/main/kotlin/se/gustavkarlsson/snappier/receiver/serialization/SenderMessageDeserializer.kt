package se.gustavkarlsson.snappier.receiver.serialization

import se.gustavkarlsson.snappier.common.message.SenderMessage

interface SenderMessageDeserializer {
    fun deserialize(data: ByteArray): SenderMessage
}
