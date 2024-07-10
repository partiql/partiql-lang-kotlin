package org.partiql.eval.internal.operator.rel

import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.BoolValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StructValue
import org.partiql.value.nullValue
import org.partiql.value.structValue

internal abstract class RelJoinNestedLoop : RelPeeking() {

    abstract val lhs: Operator.Relation
    abstract val rhs: Operator.Relation
    abstract val condition: Operator.Expr

    private var lhsRecord: Record? = null
    private lateinit var env: Environment

    override fun openPeeking(env: Environment) {
        this.env = env
        lhs.open(env)
        if (lhs.hasNext().not()) {
            return
        }
        lhsRecord = lhs.next()
        rhs.open(env.push(lhsRecord!!))
    }

    abstract fun join(condition: Boolean, lhs: Record, rhs: Record): Record?

    @OptIn(PartiQLValueExperimental::class)
    override fun peek(): Record? {
        if (lhsRecord == null) {
            return null
        }
        var rhsRecord = when (rhs.hasNext()) {
            true -> rhs.next()
            false -> null
        }
        var toReturn: Record? = null
        do {
            // Acquire LHS and RHS Records
            if (rhsRecord == null) {
                rhs.close()
                if (lhs.hasNext().not()) {
                    return null
                }
                lhsRecord = lhs.next()
                rhs.open(env.push(lhsRecord!!))
                rhsRecord = when (rhs.hasNext()) {
                    true -> rhs.next()
                    false -> null
                }
            }
            // Return Joined Record
            if (rhsRecord != null && lhsRecord != null) {
                val input = lhsRecord!! + rhsRecord
                val result = condition.eval(env.push(input))
                toReturn = join(result.isTrue(), lhsRecord!!, rhsRecord)
            }
            // Move the pointer to the next row for the RHS
            if (toReturn == null) rhsRecord = if (rhs.hasNext()) rhs.next() else null
        }
        while (toReturn == null)
        return toReturn
    }

    override fun closePeeking() {
        lhs.close()
        rhs.close()
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun PartiQLValue.isTrue(): Boolean {
        return this is BoolValue && this.value == true
    }

    @OptIn(PartiQLValueExperimental::class)
    internal fun Record.padNull() {
        this.values.indices.forEach { index ->
            this.values[index] = values[index].padNull()
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun PartiQLValue.padNull(): PartiQLValue {
        return when (this) {
            is StructValue<*> -> {
                val newFields = this.entries.map { it.first to nullValue() }
                structValue(newFields)
            }
            else -> nullValue()
        }
    }
}
