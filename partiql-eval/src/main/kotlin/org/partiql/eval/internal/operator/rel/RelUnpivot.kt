package org.partiql.eval.internal.operator.rel

import org.partiql.errors.TypeCheckException
import org.partiql.eval.internal.Environment
import org.partiql.eval.internal.Record
import org.partiql.eval.internal.operator.Operator
import org.partiql.eval.value.Datum
import org.partiql.eval.value.Field
import org.partiql.types.PType
import org.partiql.value.PartiQLValueExperimental

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

    override fun next(): Record {
        val f = _iterator.next()
        val k = Datum.stringValue(f.name)
        val v = f.value
        return Record.of(k, v)
    }

    override fun close() {}

    /**
     * In strict mode, the UNPIVOT operator raises an error on mistyped input.
     *
     * @property expr
     */
    class Strict(private val expr: Operator.Expr) : RelUnpivot() {

        override fun struct(): Datum {
            val v = expr.eval(env.push(Record.empty))
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
    class Permissive(private val expr: Operator.Expr) : RelUnpivot() {

        override fun struct(): Datum {
            val v = expr.eval(env.push(Record.empty))
            if (v.isMissing) {
                return Datum.structValue(emptyList())
            }
            return when (v.type.kind) {
                PType.Kind.STRUCT, PType.Kind.ROW -> v
                else -> Datum.structValue(listOf(Field.of("_1", v)))
            }
        }
    }
}
