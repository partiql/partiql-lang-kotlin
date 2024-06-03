package org.partiql.eval.internal

import org.partiql.eval.value.PQLValue

internal data class Record(val values: Array<PQLValue>) {

    companion object {
        val empty = Record(emptyArray())
        fun of(vararg values: PQLValue) = Record(arrayOf(*(values)))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Record
        return values.contentEquals(other.values)
    }

    override fun hashCode(): Int {
        return values.contentHashCode()
    }

    public operator fun plus(rhs: Record): Record {
        return Record(this.values + rhs.values)
    }

    public fun copy(): Record {
        return Record(this.values.copyOf())
    }

    public operator fun get(index: Int): PQLValue {
        return this.values[index]
    }

    override fun toString(): String {
        return buildString {
            append("< ")
            values.forEachIndexed { index, value ->
                append("$index: $value")
                if (index != values.lastIndex) {
                    append(", ")
                }
            }
            append(" >")
        }
    }
}
