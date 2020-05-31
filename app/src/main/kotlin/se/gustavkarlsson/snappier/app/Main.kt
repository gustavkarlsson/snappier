package se.gustavkarlsson.snappier.app

import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.subjects.PublishSubject
import se.gustavkarlsson.snappier.common.domain.FileRef
import se.gustavkarlsson.snappier.common.message.ReceiverMessage
import se.gustavkarlsson.snappier.common.message.SenderMessage
import se.gustavkarlsson.snappier.receiver.connection.ReceiverConnectionModule
import se.gustavkarlsson.snappier.receiver.connection.ReceiverMessageStreamsModule
import se.gustavkarlsson.snappier.receiver.serialization.protobuf.ProtobufReceiverMessageSerializer
import se.gustavkarlsson.snappier.receiver.serialization.protobuf.ProtobufSenderMessageDeserializer
import se.gustavkarlsson.snappier.sender.connection.SenderConnection
import se.gustavkarlsson.snappier.sender.connection.SenderConnectionModule
import se.gustavkarlsson.snappier.sender.connection.SenderMessageStreamsModule
import se.gustavkarlsson.snappier.sender.serialization.protobuf.ProtobufReceiverMessageDeserializer
import se.gustavkarlsson.snappier.sender.serialization.protobuf.ProtobufSenderMessageSerializer
import java.io.File

internal fun main() {
    val appComponent = DaggerAppComponent.create()

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

    val senderConnection: SenderConnection = appComponent.createDefaultSenderConnectionSubcomponentBuilder()
        .messageStreamsModule(SenderMessageStreamsModule(incomingSenderMessages, outgoingSenderMessages))
        .build()
        .senderConnection()

    val receiverConnection = appComponent.createDefaultReceiverConnectionSubcomponentBuilder()
        .messageStreamsModule(ReceiverMessageStreamsModule(incomingReceiverMessages, outgoingReceiverMessages))
        .build()
        .receiverConnection()

    val senderStateMachine = appComponent.createKnotSenderStateMachineSubcomponentBuilder()
        .senderConnectionModule(SenderConnectionModule(senderConnection))
        .build()
        .senderStateMachine()
        .apply { state.subscribe() }

    val receiverStateMachine = appComponent.createKnotReceiverStateMachineSubcomponentBuilder()
        .receiverConnectionModule(ReceiverConnectionModule(receiverConnection))
        .build()
        .receiverStateMachine()
        .apply { state.subscribe() }

    senderStateMachine.sendHandshake()
    senderStateMachine.sendIntendedFiles(
        listOf(
            File("settings.gradle.kts"),
            File("build.gradle.kts"),
            File(".editorconfig")
        ).map(File::toFileRef)
    )
    receiverStateMachine.setAcceptedPaths(
        "received",
        listOf(
            "transfer/settings.gradle.kts",
            "transfer/build.gradle.kts"
        )
    )

    Thread.sleep(1_000_000_000)
}

private fun File.toFileRef(): FileRef =
    FileRef(path, "transfer/$name", length())
