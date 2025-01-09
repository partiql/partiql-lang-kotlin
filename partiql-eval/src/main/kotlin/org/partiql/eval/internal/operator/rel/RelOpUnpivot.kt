package org.partiql.eval.internal.operator.rel

import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.ExprValue
import org.partiql.eval.Row
import org.partiql.eval.internal.helpers.DatumUtils.lowerSafe
import org.partiql.spi.errors.TypeCheckException
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import org.partiql.types.PType

/**
 * The unpivot operator produces a bag of records from a struct.
 *
 *  Input:   { k_0: v_0, ..., k_i: v_i }
 *  Output:  [ k_0, v_0 ] ... [ k_i, v_i ]
 */
internal sealed class RelOpUnpivot : ExprRelation {

    /**
     * Iterator of the struct fields.
     */
    private lateinit var _iterator: Iterator<Field>

    internal lateinit var env: Environment

    /**
     * Each mode overrides.
     */
    abstract fun struct(): Datum

    /**
     * Initialize the _iterator from the concrete implementation's struct()
     */
    override fun open(env: Environment) {
        this.env = env
        _iterator = struct().fields
    }

    override fun hasNext(): Boolean {
        return _iterator.hasNext()
    }

    override fun next(): Row {
        val f = _iterator.next()
        val k = Datum.string(f.name)
        val v = f.value
        return Row.of(k, v)
    }

    override fun close() {}

    /**
     * In strict mode, the UNPIVOT operator raises an error on mistyped input.
     *
     * @property expr
     */
    class Strict(private val expr: ExprValue) : RelOpUnpivot() {

        override fun struct(): Datum {
            val v = expr.eval(env.push(Row())).lowerSafe()
            if (v.type.code() != PType.STRUCT && v.type.code() != PType.ROW) {
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
    class Permissive(private val expr: ExprValue) : RelOpUnpivot() {

        override fun struct(): Datum {
            val v = expr.eval(env.push(Row())).lowerSafe()
            if (v.isMissing) {
                return Datum.struct(emptyList())
            }
            return when (v.type.code()) {
                PType.STRUCT, PType.ROW -> v
                else -> Datum.struct(listOf(Field.of("_1", v)))
            }
        }
    }
}
