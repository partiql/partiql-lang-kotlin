package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.TypePrecedence.TYPE_PRECEDENCE
import org.partiql.spi.internal.SqlTypeFamily
import org.partiql.spi.value.Datum
import org.partiql.types.PType

/**
 * This carries along with it a static table containing a mapping between the input types and the implementation.
 */
internal abstract class ArithmeticDiadicOperator : Function {

    companion object {
        val allowed = SqlTypeFamily.NUMBER.members + setOf(PType.Kind.UNKNOWN)
    }

    override fun getInstance(args: Array<PType>): Function.Instance? {
        if (!allowed.contains(args[0].kind) || !allowed.contains(args[1].kind)) {
            return null
        }
        val lhs = args[0]
        val rhs = args[1]
        val lhsPrecedence = TYPE_PRECEDENCE[lhs.kind] ?: throw IllegalArgumentException("Type not supported -- LHS = $lhs")
        val rhsPrecedence = TYPE_PRECEDENCE[rhs.kind] ?: throw IllegalArgumentException("Type not supported -- RHS = $rhs")
        val (newLhs, newRhs) = when (lhsPrecedence.compareTo(rhsPrecedence)) {
            -1 -> (rhs to rhs)
            0 -> (lhs to rhs)
            else -> (lhs to lhs)
        }
        val instance = instances[lhs.kind.ordinal][rhs.kind.ordinal]
        return instance(newLhs, newRhs)
    }

    /**
     * @param integerLhs TODO
     * @param integerRhs TODO
     * @return TODO
     */
    abstract fun getIntegerInstance(integerLhs: PType, integerRhs: PType): Function.Instance

    /**
     * @param tinyIntLhs TODO
     * @param tinyIntRhs TODO
     * @return TODO
     */
    abstract fun getTinyIntInstance(tinyIntLhs: PType, tinyIntRhs: PType): Function.Instance

    /**
     * @param smallIntLhs TODO
     * @param smallIntRhs TODO
     * @return TODO
     */
    abstract fun getSmallIntInstance(smallIntLhs: PType, smallIntRhs: PType): Function.Instance

    /**
     * @param bigIntLhs TODO
     * @param bigIntRhs TODO
     * @return TODO
     */
    abstract fun getBigIntInstance(bigIntLhs: PType, bigIntRhs: PType): Function.Instance

    /**
     * TODO: This will soon be removed.
     * @param numericLhs TODO
     * @param numericRhs TODO
     * @return TODO
     */
    abstract fun getNumericInstance(numericLhs: PType, numericRhs: PType): Function.Instance

    /**
     * @param decimalLhs TODO
     * @param decimalRhs TODO
     * @return TODO
     */
    abstract fun getDecimalInstance(decimalLhs: PType, decimalRhs: PType): Function.Instance

    /**
     * @param realLhs TODO
     * @param realRhs TODO
     * @return TODO
     */
    abstract fun getRealInstance(realLhs: PType, realRhs: PType): Function.Instance

    /**
     * @param doubleLhs TODO
     * @param doubleRhs TODO
     * @return TODO
     */
    abstract fun getDoubleInstance(doubleLhs: PType, doubleRhs: PType): Function.Instance

    override fun getParameters(): Array<Parameter> {
        return arrayOf(
            Parameter.number("lhs"),
            Parameter.number("rhs"),
        )
    }

    override fun getReturnType(args: Array<PType>): PType {
        return getInstance(args)?.returns ?: PType.dynamic() // TODO: Do we need this method?
    }

    /**
     * This is a lookup table for finding the appropriate instance for the given types. The table is
     * initialized on construction using the get*Instance methods.
     */
    private val instances: Array<Array<(PType, PType) -> Function.Instance?>> = Array(PType.Kind.entries.size) {
        Array(PType.Kind.entries.size) {
            { _, _ -> null }
        }
    }

    private fun fillTable(lhs: PType.Kind, rhs: PType.Kind, instance: (PType, PType) -> Function.Instance) {
        instances[lhs.ordinal][rhs.ordinal] = instance
    }

    private fun fillTable(highPrecedence: PType.Kind, instance: (PType, PType) -> Function.Instance) {
        val numbers = SqlTypeFamily.NUMBER.members + setOf(PType.Kind.UNKNOWN)
        numbers.filter {
            (TYPE_PRECEDENCE[highPrecedence]!! > TYPE_PRECEDENCE[it]!!)
        }.forEach {
            fillTable(highPrecedence, it) { lhs, _ -> instance(lhs, lhs) }
            fillTable(it, highPrecedence) { _, rhs -> instance(rhs, rhs) }
        }
        fillTable(highPrecedence, highPrecedence) { lhs, rhs -> instance(lhs, rhs) }
    }

    init {
        fillTable(PType.Kind.TINYINT) { lhs, rhs -> getTinyIntInstance(lhs, rhs) }
        fillTable(PType.Kind.SMALLINT) { lhs, rhs -> getSmallIntInstance(lhs, rhs) }
        fillTable(PType.Kind.INTEGER) { lhs, rhs -> getIntegerInstance(lhs, rhs) }
        fillTable(PType.Kind.BIGINT) { lhs, rhs -> getBigIntInstance(lhs, rhs) }
        fillTable(PType.Kind.DECIMAL) { lhs, rhs -> getDecimalInstance(lhs, rhs) }
        fillTable(PType.Kind.NUMERIC) { lhs, rhs -> getNumericInstance(lhs, rhs) } // TODO: Remove this
        fillTable(PType.Kind.REAL) { lhs, rhs -> getRealInstance(lhs, rhs) }
        fillTable(PType.Kind.DOUBLE) { lhs, rhs -> getDoubleInstance(lhs, rhs) }
    }

    protected fun basic(arg: PType, invocation: (Array<Datum>) -> Datum): Function.Instance {
        return Function.instance(
            name = getName(),
            returns = arg,
            parameters = arrayOf(
                Parameter("lhs", arg),
                Parameter("rhs", arg),
            ),
            invoke = invocation
        )
    }
}
