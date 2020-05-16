package se.gustavkarlsson.snappier.serialization.protobuf

import se.gustavkarlsson.snappier.message.File
import se.gustavkarlsson.snappier.message.ReceiverMessage
import se.gustavkarlsson.snappier.protobuf.ProtoCommon
import se.gustavkarlsson.snappier.protobuf.ProtoReceiver

internal fun ReceiverMessage.toProto(): ProtoReceiver.Body =
    when (this) {
        is ReceiverMessage.Handshake -> toProto()
        is ReceiverMessage.AcceptedFiles -> toProto()
    }

private fun ReceiverMessage.Handshake.toProto(): ProtoReceiver.Body =
    ProtoReceiver.Body.newBuilder()
        .setHandshake(
            ProtoReceiver.Handshake.newBuilder()
                .setProtocolVersion(protocolVersion)
                .build()
        ).build()

private fun ReceiverMessage.AcceptedFiles.toProto(): ProtoReceiver.Body =
    ProtoReceiver.Body.newBuilder()
        .setAcceptedFiles(
            ProtoReceiver.AcceptedFiles.newBuilder()
                .addAllFile(files.map(File::toProto))
                .build()
        ).build()

// TODO Conversion to message objects doesn't fail reliably. Needs validation?
internal fun ProtoReceiver.Body.toMessage(): ReceiverMessage =
    when (messageCase!!) {
        ProtoReceiver.Body.MessageCase.HANDSHAKE -> handshake.toMessage()
        ProtoReceiver.Body.MessageCase.ACCEPTEDFILES -> acceptedFiles.toMessage()
        ProtoReceiver.Body.MessageCase.MESSAGE_NOT_SET -> error("Message not set")
    }

private fun ProtoReceiver.Handshake.toMessage(): ReceiverMessage.Handshake =
    ReceiverMessage.Handshake(protocolVersion)

private fun ProtoReceiver.AcceptedFiles.toMessage(): ReceiverMessage.AcceptedFiles =
    ReceiverMessage.AcceptedFiles(fileList.map(ProtoCommon.File::toMessage).toSet())
