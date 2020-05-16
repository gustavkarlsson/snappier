package se.gustavkarlsson.snappier.serialization

import se.gustavkarlsson.snappier.message.ReceiverMessage

interface ReceiverMessageSerializer {
    fun serialize(message: ReceiverMessage): ByteArray
}
