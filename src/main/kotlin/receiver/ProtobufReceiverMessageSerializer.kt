package receiver

import common.File
import common.ReceiverMessage
import se.gustavkarlsson.snappier.protobuf.ProtoCommon
import se.gustavkarlsson.snappier.protobuf.ProtoReceiver
import sender.ReceiverMessageDeserializer

object ProtobufReceiverMessageSerializer : ReceiverMessageSerializer, ReceiverMessageDeserializer {
    override fun serialize(message: ReceiverMessage): ByteArray =
        when (message) {
            is ReceiverMessage.Handshake -> message.proto().toByteArray()
            is ReceiverMessage.AcceptedFiles -> message.proto().toByteArray()
        }

    override fun deserialize(data: ByteArray): ReceiverMessage {
        val message = ProtoReceiver.Body.parseFrom(data)
        return when (message.messageCase!!) {
            ProtoReceiver.Body.MessageCase.HANDSHAKE -> message.handshake.domain()
            ProtoReceiver.Body.MessageCase.ACCEPTEDFILES -> message.acceptedFiles.domain()
            ProtoReceiver.Body.MessageCase.MESSAGE_NOT_SET -> error("Message not set")
        }
    }
}

private fun ReceiverMessage.Handshake.proto(): ProtoReceiver.Body =
    ProtoReceiver.Body.newBuilder()
        .setHandshake(
            ProtoReceiver.Handshake.newBuilder()
                .setProtocolVersion(protocolVersion)
                .build()
        ).build()

private fun ReceiverMessage.AcceptedFiles.proto(): ProtoReceiver.Body =
    ProtoReceiver.Body.newBuilder()
        .setAcceptedFiles(
            ProtoReceiver.AcceptedFiles.newBuilder()
                .addAllFile(files.map(File::proto))
                .build()
        ).build()

private fun File.proto(): ProtoCommon.File =
    ProtoCommon.File.newBuilder()
        .setPath(path)
        .setSize(size)
        .build()

private fun ProtoReceiver.Handshake.domain(): ReceiverMessage.Handshake =
    ReceiverMessage.Handshake(protocolVersion)

private fun ProtoReceiver.AcceptedFiles.domain(): ReceiverMessage.AcceptedFiles =
    ReceiverMessage.AcceptedFiles(fileList.map(ProtoCommon.File::domain).toSet())

private fun ProtoCommon.File.domain(): File =
    File(path, size)
