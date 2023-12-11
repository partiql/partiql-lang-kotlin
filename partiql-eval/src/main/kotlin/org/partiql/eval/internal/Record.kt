package org.partiql.eval.internal

import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

@OptIn(PartiQLValueExperimental::class)
internal class Record(val values: Array<PartiQLValue>) {

    companion object {
        val empty = Record(emptyArray())
        fun of(vararg values: PartiQLValue) = Record(arrayOf(*(values)))
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
}
