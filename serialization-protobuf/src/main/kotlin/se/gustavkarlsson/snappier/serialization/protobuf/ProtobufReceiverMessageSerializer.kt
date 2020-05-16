package se.gustavkarlsson.snappier.serialization.protobuf

import se.gustavkarlsson.snappier.message.File
import se.gustavkarlsson.snappier.message.ReceiverMessage
import se.gustavkarlsson.snappier.protobuf.ProtoReceiver
import se.gustavkarlsson.snappier.serialization.ReceiverMessageSerializer

object ProtobufReceiverMessageSerializer : ReceiverMessageSerializer {
    override fun serialize(message: ReceiverMessage): ByteArray = message.toProto().toByteArray()
}

private fun ReceiverMessage.toProto(): ProtoReceiver.Body =
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
