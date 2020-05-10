package receiver

import common.ReceiverMessage
import common.SenderMessage

interface ReceiverMessageSerializer {
    fun serialize(message: ReceiverMessage): ByteArray
}

interface SenderMessageDeserializer {
    fun deserialize(data: ByteArray): SenderMessage
}
