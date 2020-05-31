package se.gustavkarlsson.snappier.app

import se.gustavkarlsson.snappier.common.domain.FileRef
import se.gustavkarlsson.snappier.receiver.connection.ReceiverConnectionModule
import se.gustavkarlsson.snappier.sender.connection.SenderConnection
import se.gustavkarlsson.snappier.sender.connection.SenderConnectionModule
import java.io.File
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.exitProcess
import se.gustavkarlsson.snappier.receiver.statemachine.State as ReceiverState
import se.gustavkarlsson.snappier.sender.statemachine.State as SenderState

internal fun main() {
    val appComponent = DaggerAppComponent.create()

    val senderConnection: SenderConnection = appComponent.createDefaultSenderConnectionSubcomponentBuilder()
        .build()
        .senderConnection()

    val senderStateMachine = appComponent.createKnotSenderStateMachineSubcomponentBuilder()
        .senderConnectionModule(SenderConnectionModule(senderConnection))
        .build()
        .senderStateMachine()

    val receiverConnection = appComponent.createDefaultReceiverConnectionSubcomponentBuilder()
        .build()
        .receiverConnection()

    val receiverStateMachine = appComponent.createKnotReceiverStateMachineSubcomponentBuilder()
        .receiverConnectionModule(ReceiverConnectionModule(receiverConnection))
        .build()
        .receiverStateMachine()

    val completed = AtomicInteger(-2)

    receiverStateMachine.state
        .ofType(ReceiverState.Completed::class.java)
        .subscribe {
            if (completed.addAndGet(1) == 0) {
                exitProcess(0)
            }
        }
    senderStateMachine.state
        .ofType(SenderState.Completed::class.java)
        .subscribe {
            if (completed.addAndGet(1) == 0) {
                exitProcess(0)
            }
        }

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
