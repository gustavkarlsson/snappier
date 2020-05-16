package se.gustavkarlsson.snappier.serialization.protobuf

import se.gustavkarlsson.snappier.message.File
import se.gustavkarlsson.snappier.protobuf.ProtoCommon

fun ProtoCommon.File.toMessage(): File =
    File(path, size)

fun File.toProto(): ProtoCommon.File =
    ProtoCommon.File.newBuilder()
        .setPath(path)
        .setSize(size)
        .build()
