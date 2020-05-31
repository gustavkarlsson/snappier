package se.gustavkarlsson.snappier.app

import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.subjects.PublishSubject
import se.gustavkarlsson.snappier.common.domain.FileRef
import se.gustavkarlsson.snappier.common.message.ReceiverMessage
import se.gustavkarlsson.snappier.common.message.SenderMessage
import se.gustavkarlsson.snappier.receiver.connection.default.DefaultReceiverConnection
import se.gustavkarlsson.snappier.receiver.files.default.DefaultFileWriter
import se.gustavkarlsson.snappier.receiver.serialization.protobuf.ProtobufReceiverMessageSerializer
import se.gustavkarlsson.snappier.receiver.serialization.protobuf.ProtobufSenderMessageDeserializer
import se.gustavkarlsson.snappier.receiver.statemachine.knot.KnotReceiverStateMachine
import se.gustavkarlsson.snappier.sender.connection.SenderConnection
import se.gustavkarlsson.snappier.sender.connection.SenderConnectionModule
import se.gustavkarlsson.snappier.sender.connection.SenderMessageStreamsModule
import se.gustavkarlsson.snappier.sender.serialization.protobuf.ProtobufReceiverMessageDeserializer
import se.gustavkarlsson.snappier.sender.serialization.protobuf.ProtobufSenderMessageSerializer
import java.io.File

private const val PROTOCOL_VERSION = 1
private const val FILE_BUFFER_SIZE = 8192

internal fun main() {
    val appComponent = DaggerAppComponent
        .create()

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

    val receiverConnection = DefaultReceiverConnection(
        incomingReceiverMessages,
        outgoingReceiverMessages,
        PROTOCOL_VERSION
    )

    val fileWriter = DefaultFileWriter(FILE_BUFFER_SIZE)

    val senderConnection: SenderConnection = appComponent.createDefaultSenderConnectionSubcomponentBuilder()
        .messageStreamsModule(
            SenderMessageStreamsModule(
                incomingSenderMessages,
                outgoingSenderMessages
            )
        )
        .build()
        .senderConnection()

    val senderStateMachine = appComponent.createKnotSenderStateMachineSubcomponentBuilder()
        .senderConnectionModule(SenderConnectionModule(senderConnection))
        .build()
        .senderStateMachine()
        .apply { state.subscribe() }

    val receiverStateMachine = KnotReceiverStateMachine(PROTOCOL_VERSION, receiverConnection, fileWriter)
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
