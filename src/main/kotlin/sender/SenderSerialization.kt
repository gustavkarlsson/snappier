package sender

import common.ReceiverMessage
import common.SenderMessage

interface SenderMessageSerializer {
    fun serialize(message: SenderMessage): ByteArray
}

interface ReceiverMessageDeserializer {
    fun deserialize(data: ByteArray): ReceiverMessage
}
