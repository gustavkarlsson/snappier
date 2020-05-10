package sender

import com.google.protobuf.ByteString
import common.File
import common.SenderMessage
import receiver.SenderMessageDeserializer
import se.gustavkarlsson.snappier.protobuf.ProtoCommon
import se.gustavkarlsson.snappier.protobuf.ProtoSender
import java.lang.Exception

object ProtobufSenderMessageSerializer : SenderMessageSerializer, SenderMessageDeserializer {
    override fun serialize(message: SenderMessage): ByteArray =
        when (message) {
            is SenderMessage.Handshake -> message.proto().toByteArray()
            is SenderMessage.IntendedFiles -> message.proto().toByteArray()
            is SenderMessage.FileStart -> message.proto().toByteArray()
            is SenderMessage.FileData -> message.proto().toByteArray()
            is SenderMessage.FileEnd -> message.proto().toByteArray()
        }

    // TODO Conversion to domain objects doesn't fail reliably. Needs validation?
    override fun deserialize(data: ByteArray): SenderMessage {
        val message = ProtoSender.Body.parseFrom(data)
        return when (message.messageCase!!) {
            ProtoSender.Body.MessageCase.HANDSHAKE -> message.handshake.domain()
            ProtoSender.Body.MessageCase.INTENDEDFILES -> message.intendedFiles.domain()
            ProtoSender.Body.MessageCase.FILESTART -> message.fileStart.domain()
            ProtoSender.Body.MessageCase.FILEDATA -> message.fileData.domain()
            ProtoSender.Body.MessageCase.FILEEND -> message.fileEnd.domain()
            ProtoSender.Body.MessageCase.MESSAGE_NOT_SET -> error("Message not set")
        }
    }
}

private fun SenderMessage.Handshake.proto(): ProtoSender.Body =
    ProtoSender.Body.newBuilder()
        .setHandshake(
            ProtoSender.Handshake.newBuilder()
                .setProtocolVersion(protocolVersion)
                .build()
        )
        .build()

private fun SenderMessage.IntendedFiles.proto(): ProtoSender.Body =
    ProtoSender.Body.newBuilder()
        .setIntendedFiles(
            ProtoSender.IntendedFiles.newBuilder()
                .addAllFile(files.map(File::proto))
                .build()
        )
        .build()

private fun SenderMessage.FileStart.proto(): ProtoSender.Body =
    ProtoSender.Body.newBuilder()
        .setFileStart(
            ProtoSender.FileStart.newBuilder()
                .setFile(file.proto())
                .build()
        )
        .build()

private fun SenderMessage.FileData.proto(): ProtoSender.Body =
    ProtoSender.Body.newBuilder()
        .setFileData(
            ProtoSender.FileData.newBuilder()
                .setData(ByteString.copyFrom(data))
                .build()
        )
        .build()

@Suppress("unused")
private fun SenderMessage.FileEnd.proto(): ProtoSender.Body =
    ProtoSender.Body.newBuilder()
        .setFileEnd(
            ProtoSender.FileEnd.newBuilder()
                .build()
        )
        .build()

// TODO this is a duplicate
private fun File.proto(): ProtoCommon.File =
    ProtoCommon.File.newBuilder()
        .setPath(path)
        .setSize(size)
        .build()

private fun ProtoSender.Handshake.domain(): SenderMessage.Handshake =
    SenderMessage.Handshake(protocolVersion)

private fun ProtoSender.IntendedFiles.domain(): SenderMessage.IntendedFiles =
    SenderMessage.IntendedFiles(fileList.map(ProtoCommon.File::domain).toSet())

private fun ProtoSender.FileStart.domain(): SenderMessage.FileStart =
    SenderMessage.FileStart(file.domain())

private fun ProtoSender.FileData.domain(): SenderMessage.FileData =
    SenderMessage.FileData(data.toByteArray())

@Suppress("unused")
private fun ProtoSender.FileEnd.domain(): SenderMessage.FileEnd =
    SenderMessage.FileEnd

// TODO this is a duplicate
private fun ProtoCommon.File.domain(): File =
    File(path, size)
