package org.partiql.eval.internal.operator.rex

import org.partiql.eval.Environment
import org.partiql.eval.ExprValue
import org.partiql.eval.internal.helpers.DatumUtils.lowerSafe
import org.partiql.eval.internal.helpers.PErrors
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

internal class ExprPathIndexCollection(
    @JvmField val root: ExprValue,
    @JvmField val key: ExprValue,
) : ExprValue {

    override fun eval(env: Environment): Datum {
        return evalWithInput(root.eval(env), env)
    }

    fun evalWithInput(input: Datum, env: Environment): Datum {
        // A VARIANT holding an Ion list or S-expression lowers to an ARRAY, so it is indexed the same way.
        val collection = input.lowerSafe()
        if (collection.isMissing) {
            throw PErrors.pathIndexFailureException()
        }
        when (collection.type.code()) {
            PType.BAG,
            PType.ARRAY -> Unit
            else -> throw PErrors.pathIndexFailureException()
        }
        // Evaluate the index before the null check, so an error in it is raised even when the array is null,
        // as ExprPathKeyStruct does for its key.
        val k = key.eval(env).lowerSafe()
        // A null array has no element to return, so the result is null (SQL:2023 section 6.25, general rule 1).
        if (collection.isNull) {
            return Datum.nullValue()
        }
        val index = index(k)
        val iterator = collection.iterator()
        var i = 0L
        while (iterator.hasNext()) {
            val v = iterator.next()
            if (i == index) {
                return v
            }
            i++
        }
        throw PErrors.pathIndexFailureException()
    }

    /**
     * Returns the index as a non-negative [Long]. The index must have an integer type. It is never narrowed, so a
     * BIGINT that does not fit in an INT cannot wrap around to a valid position.
     */
    private fun index(k: Datum): Long {
        // A null/missing index cannot address an element; treat it as a path index failure.
        if (k.isNull || k.isMissing) {
            throw PErrors.pathIndexFailureException()
        }
        val index = when (k.type.code()) {
            PType.TINYINT -> k.byte.toLong()
            PType.SMALLINT -> k.short.toLong()
            PType.INTEGER -> k.int.toLong()
            PType.BIGINT -> k.long
            else -> throw PErrors.pathIndexFailureException()
        }
        if (index < 0) {
            throw PErrors.pathIndexFailureException()
        }
        return index
    }
}
