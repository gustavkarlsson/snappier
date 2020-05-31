package se.gustavkarlsson.snappier.app

import dagger.Module
import dagger.Provides
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Observer
import io.reactivex.rxjava3.subjects.PublishSubject
import se.gustavkarlsson.snappier.common.message.ReceiverMessage
import se.gustavkarlsson.snappier.common.message.SenderMessage
import se.gustavkarlsson.snappier.receiver.serialization.protobuf.ProtobufReceiverMessageSerializer
import se.gustavkarlsson.snappier.receiver.serialization.protobuf.ProtobufSenderMessageDeserializer
import se.gustavkarlsson.snappier.sender.serialization.protobuf.ProtobufReceiverMessageDeserializer
import se.gustavkarlsson.snappier.sender.serialization.protobuf.ProtobufSenderMessageSerializer

@Module
internal class MessageStreamsModule {

    private val incomingSenderMessages: Observable<ReceiverMessage>

    private val outgoingSenderMessages: Observer<SenderMessage>

    private val incomingReceiverMessages: Observable<SenderMessage>

    private val outgoingReceiverMessages: Observer<ReceiverMessage>

    init {
        val senderToReceiverMessages = PublishSubject.create<SenderMessage>()
        val receiverToSenderMessages = PublishSubject.create<ReceiverMessage>()

        incomingSenderMessages = receiverToSenderMessages
            .map { ProtobufReceiverMessageSerializer.serialize(it) }
            .map { ProtobufReceiverMessageDeserializer.deserialize(it) }

        outgoingSenderMessages = senderToReceiverMessages

        incomingReceiverMessages = senderToReceiverMessages
            .map { ProtobufSenderMessageSerializer.serialize(it) }
            .map { ProtobufSenderMessageDeserializer.deserialize(it) }

        outgoingReceiverMessages = receiverToSenderMessages
    }

    @Provides
    fun provideIncomingSenderMessages(): Observable<ReceiverMessage> = incomingSenderMessages

    @Provides
    fun provideOutgoingSenderMessages(): Observer<SenderMessage> = outgoingSenderMessages

    @Provides
    fun provideIncomingReceiverMessages(): Observable<SenderMessage> = incomingReceiverMessages

    @Provides
    fun provideOutgoingReceiverMessages(): Observer<ReceiverMessage> = outgoingReceiverMessages
}
