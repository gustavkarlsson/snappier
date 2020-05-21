package se.gustavkarlsson.snappier.common.serialization.protobuf

import se.gustavkarlsson.snappier.common.message.TransferFile
import se.gustavkarlsson.snappier.protobuf.ProtoCommon

fun TransferFile.toProto(): ProtoCommon.File =
    ProtoCommon.File.newBuilder()
        .setPath(path)
        .setSize(size)
        .build()

fun ProtoCommon.File.toMessage(): TransferFile =
    TransferFile(path, size)
