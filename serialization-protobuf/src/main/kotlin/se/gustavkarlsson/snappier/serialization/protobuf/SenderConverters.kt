package se.gustavkarlsson.snappier.serialization.protobuf

import se.gustavkarlsson.snappier.message.File
import se.gustavkarlsson.snappier.message.SenderMessage
import se.gustavkarlsson.snappier.protobuf.ProtoCommon
import se.gustavkarlsson.snappier.protobuf.ProtoSender

internal fun SenderMessage.toProto(): ProtoSender.Body =
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
    SenderMessage.IntendedFiles(fileList.map(ProtoCommon.File::toMessage).toSet())

private fun ProtoSender.FileStart.toMessage(): SenderMessage.FileStart =
    SenderMessage.FileStart(file.toMessage())

private fun ProtoSender.FileData.toMessage(): SenderMessage.FileData =
    SenderMessage.FileData(data.toByteArray())

@Suppress("unused")
private fun ProtoSender.FileEnd.toMessage(): SenderMessage.FileEnd =
    SenderMessage.FileEnd
