package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.Row
import org.partiql.eval.internal.helpers.DatumUtils.lowerSafe
import org.partiql.eval.internal.helpers.PErrors
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field

/**
 * The unpivot operator produces a bag of records from a struct or map.
 *
 *  Struct Input:  { k_0: v_0, ..., k_i: v_i }
 *  MAP Input:     MAP { k_0: v_0, ..., k_i: v_i }
 *  Output:        [ k_0, v_0 ] ... [ k_i, v_i ]
 */
internal sealed class RelOpUnpivot : ExprRelation {

    private lateinit var _next: () -> Row?
    private var _row: Row? = null

    internal lateinit var env: Environment

    abstract fun input(): Datum

    override fun open(env: Environment) {
        this.env = env
        val v = input()
        _next = when (v.type.code()) {
            PType.MAP -> {
                val iter = v.entries
                ({ if (iter.hasNext()) { val e = iter.next(); Row.of(e.key, e.value) } else null })
            }
            else -> {
                val iter = v.fields
                ({ if (iter.hasNext()) { val f = iter.next(); Row.of(Datum.string(f.name), f.value) } else null })
            }
        }
        _row = _next()
    }

    override fun hasNext(): Boolean = _row != null

    override fun next(): Row {
        val row = _row ?: throw NoSuchElementException()
        _row = _next()
        return row
    }

    override fun close() {}

    /**
     * In strict mode, the UNPIVOT operator raises an error on mistyped input.
     */
    class Strict(private val expr: ExprValue) : RelOpUnpivot() {

        override fun input(): Datum {
            val v = expr.eval(env.push(Row())).lowerSafe()
            val type = v.type
            if (type.code() != PType.STRUCT && type.code() != PType.ROW && type.code() != PType.MAP) {
                throw PErrors.structureExpectedException(type)
            }
            return v
        }
    }

    /**
     * In permissive mode, the UNPIVOT operator coerces the input (v) to a struct.
     *
     *  1. If v is a struct or map, return it.
     *  2. If v is MISSING, return { }.
     *  3. Else, return { '_1': v }.
     */
    class Permissive(private val expr: ExprValue) : RelOpUnpivot() {

        override fun input(): Datum {
            val v = expr.eval(env.push(Row())).lowerSafe()
            if (v.isMissing) {
                return Datum.struct(emptyList())
            }
            if (v.isNull) {
                return Datum.struct(listOf(Field.of("_1", v)))
            }
            return when (v.type.code()) {
                PType.STRUCT, PType.ROW, PType.MAP -> v
                else -> Datum.struct(listOf(Field.of("_1", v)))
            }
        }
    }
}
