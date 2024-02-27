package org.partiql.eval.internal.operator.rel

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.value.MissingValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StructValue
import org.partiql.value.stringValue
import org.partiql.value.structValue

/**
 * The unpivot operator produces a bag of records from a struct.
 *
 *  Input:   { k_0: v_0, ..., k_i: v_i }
 *  Output:  [ k_0, v_0 ] ... [ k_i, v_i ]
 */
@OptIn(PartiQLValueExperimental::class)
internal sealed class RelUnpivot : Operator.Relation {

    /**
     * Iterator of the struct fields.
     */
    private lateinit var _iterator: Iterator<Pair<String, PartiQLValue>>

    internal lateinit var env: Environment

    /**
     * Each mode overrides.
     */
    abstract fun struct(): StructValue<*>

    /**
     * Initialize the _iterator from the concrete implementation's struct()
     */
    override fun open(env: Environment) {
        _iterator = struct().entries.iterator()
    }

    override fun hasNext(): Boolean {
        return _iterator.hasNext()
    }

    override fun next(): Record {
        val f = _iterator.next()
        val k = stringValue(f.first)
        val v = f.second
        return Record.of(k, v)
    }

    override fun close() {}

    /**
     * In strict mode, the UNPIVOT operator raises an error on mistyped input.
     *
     * @property expr
     */
    class Strict(private val expr: Operator.Expr) : RelUnpivot() {

        override fun struct(): StructValue<*> {
            val v = expr.eval(env.nest(Record.empty))
            if (v !is StructValue<*>) {
                throw TypeCheckException()
            }
            return v
        }
    }

    /**
     * In permissive mode, the UNPIVOT operator coerces the input (v) to a struct.
     *
     *  1. If v is a struct, return it.
     *  2. If v is MISSING, return { }.
     *  3. Else, return { '_1': v }.
     *
     * @property expr
     */
    class Permissive(private val expr: Operator.Expr) : RelUnpivot() {

        override fun struct(): StructValue<*> = when (val v = expr.eval(env.nest(Record.empty))) {
            is StructValue<*> -> v
            is MissingValue -> structValue<PartiQLValue>()
            else -> structValue("_1" to v)
        }
    }
}
