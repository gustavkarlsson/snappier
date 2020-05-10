import common.File
import common.ReceiverMessage
import common.SenderMessage
import io.reactivex.rxjava3.subjects.PublishSubject
import receiver.DummyReceiverConnection
import receiver.createReceiverKnot
import sender.DummySenderConnection
import sender.ProtobufSenderMessageSerializer
import sender.createSenderKnot
import receiver.Change as ReceiverChange
import sender.Change as SenderChange

fun main() {
    val senderToReceiverMessages = PublishSubject.create<SenderMessage>()
    val receiverToSenderMessages = PublishSubject.create<ReceiverMessage>()

    val senderConnection = DummySenderConnection(receiverToSenderMessages, senderToReceiverMessages)
    val receiverConnection = DummyReceiverConnection(senderToReceiverMessages, receiverToSenderMessages)

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

    val data = ProtobufSenderMessageSerializer.serialize(SenderMessage.FileStart(File("some/file.txt", 522)))

    val value = ProtobufSenderMessageSerializer.deserialize(data)

    println(data)
    println(value)

    Thread.sleep(1_000_000_000)
}
