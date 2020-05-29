package se.gustavkarlsson.snappier.common.domain

data class Bytes(val array: ByteArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Bytes

        if (!array.contentEquals(other.array)) return false

        return true
    }

    override fun hashCode(): Int {
        return array.contentHashCode()
    }

    override fun toString(): String = "Bytes(size=${array.size})"

    val size: Int get() = array.size
}
