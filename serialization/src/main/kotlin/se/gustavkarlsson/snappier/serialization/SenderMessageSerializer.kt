package se.gustavkarlsson.snappier.serialization

import se.gustavkarlsson.snappier.message.SenderMessage

interface SenderMessageSerializer {
    fun serialize(message: SenderMessage): ByteArray
}
