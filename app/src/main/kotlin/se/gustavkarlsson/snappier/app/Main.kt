package se.gustavkarlsson.snappier.app

import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.subjects.PublishSubject
import se.gustavkarlsson.snappier.app.receiver.DummyReceiverConnection
import se.gustavkarlsson.snappier.app.receiver.createReceiverKnot
import se.gustavkarlsson.snappier.common.message.File
import se.gustavkarlsson.snappier.common.message.ReceiverMessage
import se.gustavkarlsson.snappier.common.message.SenderMessage
import se.gustavkarlsson.snappier.receiver.serialization.protobuf.ProtobufReceiverMessageSerializer
import se.gustavkarlsson.snappier.sender.serialization.protobuf.ProtobufSenderMessageSerializer
import se.gustavkarlsson.snappier.app.sender.DummySenderConnection
import se.gustavkarlsson.snappier.app.sender.createSenderKnot
import se.gustavkarlsson.snappier.sender.serialization.protobuf.ProtobufReceiverMessageDeserializer
import se.gustavkarlsson.snappier.receiver.serialization.protobuf.ProtobufSenderMessageDeserializer
import se.gustavkarlsson.snappier.app.receiver.Change as ReceiverChange
import se.gustavkarlsson.snappier.app.sender.Change as SenderChange

const val PROTOCOL_VERSION = 1
const val BUFFER_SIZE = 1024

fun main() {
    val senderToReceiverMessages = PublishSubject.create<SenderMessage>()
    val receiverToSenderMessages = PublishSubject.create<ReceiverMessage>()

    val incomingSenderMessages = receiverToSenderMessages
        .map { ProtobufReceiverMessageSerializer.serialize(it) }
        .map { ProtobufReceiverMessageDeserializer.deserialize(it) }

    val outgoingSenderMessages: Observer<SenderMessage> = senderToReceiverMessages

    val incomingReceiverMessages = senderToReceiverMessages
        .map { ProtobufSenderMessageSerializer.serialize(it) }
        .map { ProtobufSenderMessageDeserializer.deserialize(it) }

    val outgoingReceiverMessages: Observer<ReceiverMessage> = receiverToSenderMessages

    val senderConnection = DummySenderConnection(incomingSenderMessages, outgoingSenderMessages)
    val receiverConnection = DummyReceiverConnection(incomingReceiverMessages, outgoingReceiverMessages)

    val senderKnot = createSenderKnot(senderConnection)
    val receiverKnot = createReceiverKnot(receiverConnection)

    senderKnot.change.accept(SenderChange.SendHandshake)
    senderKnot.change.accept(
        SenderChange.SendIntendedFiles(
            setOf(
                File("some.file.txt", 155),
                File("some.other.file.txt", 179),
                File("some.stupid.file.txt", 1560)
            )
        )
    )
    receiverKnot.change.accept(
        ReceiverChange.SendAcceptedFiles(
            setOf(
                File("some.file.txt", 155),
                File("some.other.file.txt", 179)
            )
        )
    )

    Thread.sleep(1_000_000_000)
}
