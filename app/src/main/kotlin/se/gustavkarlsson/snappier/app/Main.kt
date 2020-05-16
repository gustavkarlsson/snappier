package se.gustavkarlsson.snappier.app

import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.subjects.PublishSubject
import se.gustavkarlsson.snappier.common.message.File
import se.gustavkarlsson.snappier.common.message.ReceiverMessage
import se.gustavkarlsson.snappier.common.message.SenderMessage
import se.gustavkarlsson.snappier.receiver.connection.default.DefaultReceiverConnection
import se.gustavkarlsson.snappier.receiver.serialization.protobuf.ProtobufReceiverMessageSerializer
import se.gustavkarlsson.snappier.receiver.serialization.protobuf.ProtobufSenderMessageDeserializer
import se.gustavkarlsson.snappier.receiver.statemachine.knot.KnotReceiverStateMachine
import se.gustavkarlsson.snappier.sender.connection.default.DefaultSenderConnection
import se.gustavkarlsson.snappier.sender.serialization.protobuf.ProtobufReceiverMessageDeserializer
import se.gustavkarlsson.snappier.sender.serialization.protobuf.ProtobufSenderMessageSerializer
import se.gustavkarlsson.snappier.sender.statemachine.knot.KnotSenderStateMachine
import java.io.File as JavaFile

const val PROTOCOL_VERSION = 1
const val BUFFER_SIZE = 20480

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

    val senderConnection =
        DefaultSenderConnection(
            incomingSenderMessages,
            outgoingSenderMessages,
            PROTOCOL_VERSION
        )

    val receiverConnection = DefaultReceiverConnection(
        incomingReceiverMessages,
        outgoingReceiverMessages,
        PROTOCOL_VERSION
    )

    val senderStateMachine = KnotSenderStateMachine(senderConnection)
    val receiverStateMachine = KnotReceiverStateMachine(receiverConnection)

    senderStateMachine.sendHandshake()
    senderStateMachine.sendIntendedFiles(
        listOf(
            JavaFile("settings.gradle.kts"),
            JavaFile("build.gradle.kts"),
            JavaFile(".editorconfig")
        )
            .map { it.toFile() }
            .toSet()
    )
    receiverStateMachine.setAcceptedFiles(
        listOf(
            JavaFile("settings.gradle.kts"),
            JavaFile("build.gradle.kts")
        )
            .map { it.toFile() }
            .toSet()
    )

    Thread.sleep(1_000_000_000)
}

private fun JavaFile.toFile(): File = File(path, length())
