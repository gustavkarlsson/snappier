package se.gustavkarlsson.snappier.receiver.serialization.protobuf

import se.gustavkarlsson.snappier.common.message.ReceiverMessage
import se.gustavkarlsson.snappier.protobuf.ProtoReceiver
import se.gustavkarlsson.snappier.receiver.serialization.ReceiverMessageSerializer

object ProtobufReceiverMessageSerializer : ReceiverMessageSerializer {
    override fun serialize(message: ReceiverMessage): ByteArray = message.toProto().toByteArray()
}

private fun ReceiverMessage.toProto(): ProtoReceiver.Body =
    when (this) {
        is ReceiverMessage.Handshake -> toProto()
        is ReceiverMessage.AcceptedPaths -> toProto()
    }

private fun ReceiverMessage.Handshake.toProto(): ProtoReceiver.Body =
    ProtoReceiver.Body.newBuilder()
        .setHandshake(
            ProtoReceiver.Handshake.newBuilder()
                .setProtocolVersion(protocolVersion)
                .build()
        ).build()

private fun ReceiverMessage.AcceptedPaths.toProto(): ProtoReceiver.Body =
    ProtoReceiver.Body.newBuilder()
        .setAcceptedPaths(
            ProtoReceiver.AcceptedPaths.newBuilder()
                .addAllPath(transferPaths)
                .build()
        ).build()
