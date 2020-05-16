package se.gustavkarlsson.snappier.serialization.protobuf

import se.gustavkarlsson.snappier.message.File
import se.gustavkarlsson.snappier.protobuf.ProtoCommon

internal fun ProtoCommon.File.toMessage(): File =
    File(path, size)

internal fun File.toProto(): ProtoCommon.File =
    ProtoCommon.File.newBuilder()
        .setPath(path)
        .setSize(size)
        .build()
