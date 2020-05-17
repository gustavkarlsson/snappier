package se.gustavkarlsson.snappier.sender.serialization.protobuf

import se.gustavkarlsson.snappier.common.serialization.protobuf.toProto
import se.gustavkarlsson.snappier.common.message.File
import se.gustavkarlsson.snappier.common.message.SenderMessage
import se.gustavkarlsson.snappier.protobuf.ProtoSender
import se.gustavkarlsson.snappier.sender.serialization.SenderMessageSerializer

object ProtobufSenderMessageSerializer : SenderMessageSerializer {
    override fun serialize(message: SenderMessage): ByteArray = message.toProto().toByteArray()
}

private fun SenderMessage.toProto(): ProtoSender.Body =
    when (this) {
        is SenderMessage.Handshake -> toProto()
        is SenderMessage.IntendedFiles -> toProto()
        is SenderMessage.FileStart -> toProto()
        is SenderMessage.FileData -> toProto()
        is SenderMessage.FileEnd -> toProto()
    }

private fun SenderMessage.Handshake.toProto(): ProtoSender.Body =
    ProtoSender.Body.newBuilder()
        .setHandshake(
            ProtoSender.Handshake.newBuilder()
                .setProtocolVersion(protocolVersion)
                .build()
        )
        .build()

private fun SenderMessage.IntendedFiles.toProto(): ProtoSender.Body =
    ProtoSender.Body.newBuilder()
        .setIntendedFiles(
            ProtoSender.IntendedFiles.newBuilder()
                .addAllFile(files.map(File::toProto))
                .build()
        )
        .build()

private fun SenderMessage.FileStart.toProto(): ProtoSender.Body =
    ProtoSender.Body.newBuilder()
        .setFileStart(
            ProtoSender.FileStart.newBuilder()
                .setFile(file.toProto())
                .build()
        )
        .build()

private fun SenderMessage.FileData.toProto(): ProtoSender.Body =
    ProtoSender.Body.newBuilder()
        .setFileData(
            ProtoSender.FileData.newBuilder()
                .setData(com.google.protobuf.ByteString.copyFrom(data))
                .build()
        )
        .build()

@Suppress("unused")
private fun SenderMessage.FileEnd.toProto(): ProtoSender.Body =
    ProtoSender.Body.newBuilder()
        .setFileEnd(
            ProtoSender.FileEnd.newBuilder()
                .build()
        )
        .build()
