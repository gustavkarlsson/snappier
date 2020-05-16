package se.gustavkarlsson.snappier.receiver.serialization

import se.gustavkarlsson.snappier.common.message.ReceiverMessage

interface ReceiverMessageSerializer {
    fun serialize(message: ReceiverMessage): ByteArray
}
