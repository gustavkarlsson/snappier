package se.gustavkarlsson.snappier.receiver.serialization.protobuf

import se.gustavkarlsson.snappier.common.domain.Bytes
import se.gustavkarlsson.snappier.common.serialization.protobuf.toMessage
import se.gustavkarlsson.snappier.common.message.SenderMessage
import se.gustavkarlsson.snappier.protobuf.ProtoCommon
import se.gustavkarlsson.snappier.protobuf.ProtoSender
import se.gustavkarlsson.snappier.receiver.serialization.SenderMessageDeserializer

object ProtobufSenderMessageDeserializer : SenderMessageDeserializer {
    override fun deserialize(data: ByteArray): SenderMessage = ProtoSender.Body.parseFrom(data).toMessage()
}

// TODO Conversion to message objects doesn't fail reliably. Needs validation?
internal fun ProtoSender.Body.toMessage(): SenderMessage =
    when (messageCase!!) {
        ProtoSender.Body.MessageCase.HANDSHAKE -> handshake.toMessage()
        ProtoSender.Body.MessageCase.INTENDEDFILES -> intendedFiles.toMessage()
        ProtoSender.Body.MessageCase.FILESTART -> fileStart.toMessage()
        ProtoSender.Body.MessageCase.FILEDATA -> fileData.toMessage()
        ProtoSender.Body.MessageCase.FILEEND -> fileEnd.toMessage()
        ProtoSender.Body.MessageCase.MESSAGE_NOT_SET -> error("Message not set")
    }

private fun ProtoSender.Handshake.toMessage(): SenderMessage.Handshake =
    SenderMessage.Handshake(protocolVersion)

private fun ProtoSender.IntendedFiles.toMessage(): SenderMessage.IntendedFiles =
    SenderMessage.IntendedFiles(fileList.map(ProtoCommon.File::toMessage))

private fun ProtoSender.FileStart.toMessage(): SenderMessage.FileStart =
    SenderMessage.FileStart(path)

private fun ProtoSender.FileData.toMessage(): SenderMessage.FileData =
    SenderMessage.FileData(Bytes(data.toByteArray()))

@Suppress("unused")
private fun ProtoSender.FileEnd.toMessage(): SenderMessage.FileEnd =
    SenderMessage.FileEnd
