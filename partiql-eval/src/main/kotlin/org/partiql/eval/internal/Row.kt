package org.partiql.eval.internal

import org.partiql.spi.value.Datum
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.toIon

/**
 * Thin layer over
 */
internal class Row(val values: Array<Datum>) {

    companion object {
        val empty = Row(emptyArray())
        fun of(vararg values: Datum) = Row(arrayOf(*(values)))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Row
        return values.contentEquals(other.values)
    }

    override fun hashCode(): Int {
        return values.contentHashCode()
    }

    public operator fun plus(rhs: Row): Row {
        return Row(this.values + rhs.values)
    }

    public fun copy(): Row {
        return Row(this.values.copyOf())
    }

    public operator fun get(index: Int): Datum {
        return this.values[index]
    }

    @OptIn(PartiQLValueExperimental::class)
    override fun toString(): String {
        return buildString {
            append("< ")
            values.forEachIndexed { index, value ->
                append("$index: ${value.toPartiQLValue().toIon()}")
                if (index != values.lastIndex) {
                    append(", ")
                }
            }
            append(" >")
        }
    }
}
