package org.partiql.eval.internal.operator.rel

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Row
import org.partiql.eval.internal.operator.Operator
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import org.partiql.types.PType

/**
 * The unpivot operator produces a bag of records from a struct.
 *
 *  Input:   { k_0: v_0, ..., k_i: v_i }
 *  Output:  [ k_0, v_0 ] ... [ k_i, v_i ]
 */
internal sealed class RelOpUnpivot : Operator.Relation {

    /**
     * Iterator of the struct fields.
     */
    private lateinit var _iterator: Iterator<Field>

    /**
     * Each mode overrides.
     */
    abstract fun struct(): Datum

    /**
     * Initialize the _iterator from the concrete implementation's struct()
     */
    override fun open() {
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
    class Strict(private val expr: Operator.Expr) : RelOpUnpivot() {

        override fun struct(): Datum {
            val v = expr.eval()
            if (v.type.kind != PType.Kind.STRUCT && v.type.kind != PType.Kind.ROW) {
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
    class Permissive(private val expr: Operator.Expr) : RelOpUnpivot() {

        override fun struct(): Datum {
            val v = expr.eval()
            if (v.isMissing) {
                return Datum.struct(emptyList())
            }
            return when (v.type.kind) {
                PType.Kind.STRUCT, PType.Kind.ROW -> v
                else -> Datum.struct(listOf(Field.of("_1", v)))
            }
        }
    }
}
