package se.gustavkarlsson.snappier.common.serialization.protobuf

import se.gustavkarlsson.snappier.common.message.File
import se.gustavkarlsson.snappier.protobuf.ProtoCommon

fun File.toProto(): ProtoCommon.File =
    ProtoCommon.File.newBuilder()
        .setPath(path)
        .setSize(size)
        .build()

fun ProtoCommon.File.toMessage(): File =
    File(path, size)
