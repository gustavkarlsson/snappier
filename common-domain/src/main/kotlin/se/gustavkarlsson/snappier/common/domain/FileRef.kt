package se.gustavkarlsson.snappier.common.domain

data class FileRef(val fileSystemPath: String, val transferPath: String, val size: Long)
