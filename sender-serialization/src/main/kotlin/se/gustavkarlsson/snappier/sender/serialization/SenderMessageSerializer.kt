package se.gustavkarlsson.snappier.sender.serialization

import se.gustavkarlsson.snappier.common.message.SenderMessage

interface SenderMessageSerializer {
    fun serialize(message: SenderMessage): ByteArray
}
