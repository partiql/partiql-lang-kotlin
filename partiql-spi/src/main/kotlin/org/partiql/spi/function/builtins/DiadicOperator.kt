package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.TypePrecedence.TYPE_PRECEDENCE
import org.partiql.spi.function.utils.FunctionUtils
import org.partiql.spi.internal.SqlTypeFamily
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum

/**
 * This represents an operator backed by a function provider. Note that the name of the operator is hidden
 * using [FunctionUtils.hide].
 *
 * This carries along with it a static table containing a mapping between the input types and the implementation.
 *
 * Implementations of this should invoke [fillTable] in the constructor of the function.
 */
internal abstract class DiadicOperator(
    name: String,
    private val lhs: Parameter,
    private val rhs: Parameter
) : Function {

    private val name = FunctionUtils.hide(name)

    companion object {
        private val DEC_TINY_INT = PType.decimal(3, 0)
        private val DEC_SMALL_INT = PType.decimal(5, 0)
        private val DEC_INT = PType.decimal(10, 0)
        private val DEC_BIG_INT = PType.decimal(19, 0)
    }

    override fun getName(): String {
        return name
    }

    override fun getParameters(): Array<Parameter> {
        return arrayOf(lhs, rhs)
    }

    override fun getInstance(args: Array<PType>): Function.Instance? {
        val lhs = args[0]
        val rhs = args[1]
        val lhsPrecedence = TYPE_PRECEDENCE[lhs.code()] ?: return null
        val rhsPrecedence = TYPE_PRECEDENCE[rhs.code()] ?: return null
        val (newLhs, newRhs) = when (lhsPrecedence.compareTo(rhsPrecedence)) {
            -1 -> (rhs to rhs)
            0 -> (lhs to rhs)
            else -> (lhs to lhs)
        }
        val instance = instances[lhs.code()][rhs.code()]
        return instance(newLhs, newRhs)
    }

    /**
     * @param booleanLhs TODO
     * @param booleanRhs TODO
     * @return TODO
     */
    open fun getBooleanInstance(booleanLhs: PType, booleanRhs: PType): Function.Instance? {
        return null
    }

    /**
     * @param stringLhs TODO
     * @param stringRhs TODO
     * @return TODO
     */
    open fun getStringInstance(stringLhs: PType, stringRhs: PType): Function.Instance? {
        return null
    }

    /**
     * @param charLhs TODO
     * @param charRhs TODO
     * @return TODO
     */
    open fun getCharInstance(charLhs: PType, charRhs: PType): Function.Instance? {
        return null
    }

    /**
     * @param varcharLhs TODO
     * @param varcharRhs TODO
     * @return TODO
     */
    open fun getVarcharInstance(varcharLhs: PType, varcharRhs: PType): Function.Instance? {
        return null
    }

    /**
     * @param blobLhs TODO
     * @param blobRhs TODO
     * @return TODO
     */
    open fun getBlobInstance(blobLhs: PType, blobRhs: PType): Function.Instance? {
        return null
    }

    /**
     * @param clobLhs TODO
     * @param clobRhs TODO
     * @return TODO
     */
    open fun getClobInstance(clobLhs: PType, clobRhs: PType): Function.Instance? {
        return null
    }

    /**
     * @param dateLhs TODO
     * @param dateRhs TODO
     * @return TODO
     */
    open fun getDateInstance(dateLhs: PType, dateRhs: PType): Function.Instance? {
        return null
    }

    /**
     * @param timeLhs TODO
     * @param timeRhs TODO
     * @return TODO
     */
    open fun getTimeInstance(timeLhs: PType, timeRhs: PType): Function.Instance? {
        return null
    }

    /**
     * @param timestampLhs TODO
     * @param timestampRhs TODO
     * @return TODO
     */
    open fun getTimestampInstance(timestampLhs: PType, timestampRhs: PType): Function.Instance? {
        return null
    }

    /**
     * @param integerLhs TODO
     * @param integerRhs TODO
     * @return TODO
     */
    open fun getIntegerInstance(integerLhs: PType, integerRhs: PType): Function.Instance? {
        return null
    }

    /**
     * @param tinyIntLhs TODO
     * @param tinyIntRhs TODO
     * @return TODO
     */
    open fun getTinyIntInstance(tinyIntLhs: PType, tinyIntRhs: PType): Function.Instance? {
        return null
    }

    /**
     * @param smallIntLhs TODO
     * @param smallIntRhs TODO
     * @return TODO
     */
    open fun getSmallIntInstance(smallIntLhs: PType, smallIntRhs: PType): Function.Instance? {
        return null
    }

    /**
     * @param bigIntLhs TODO
     * @param bigIntRhs TODO
     * @return TODO
     */
    open fun getBigIntInstance(bigIntLhs: PType, bigIntRhs: PType): Function.Instance? {
        return null
    }

    /**
     * TODO: This will soon be removed.
     * @param numericLhs TODO
     * @param numericRhs TODO
     * @return TODO
     */
    open fun getNumericInstance(numericLhs: PType, numericRhs: PType): Function.Instance? {
        return null
    }

    /**
     * @param decimalLhs TODO
     * @param decimalRhs TODO
     * @return TODO
     */
    open fun getDecimalInstance(decimalLhs: PType, decimalRhs: PType): Function.Instance? {
        return null
    }

    /**
     * @param realLhs TODO
     * @param realRhs TODO
     * @return TODO
     */
    open fun getRealInstance(realLhs: PType, realRhs: PType): Function.Instance? {
        return null
    }

    /**
     * @param doubleLhs TODO
     * @param doubleRhs TODO
     * @return TODO
     */
    open fun getDoubleInstance(doubleLhs: PType, doubleRhs: PType): Function.Instance? {
        return null
    }

    override fun getReturnType(args: Array<PType>): PType {
        return getInstance(args)?.returns ?: PType.dynamic() // TODO: Do we need this method?
    }

    /**
     * This is a lookup table for finding the appropriate instance for the given types. The table is
     * initialized on construction using the get*Instance methods.
     */
    protected val instances: Array<Array<(PType, PType) -> Function.Instance?>> = Array(PType.codes().size) {
        Array(PType.codes().size) {
            { _, _ -> null }
        }
    }

    protected fun fillTable(lhs: Int, rhs: Int, instance: (PType, PType) -> Function.Instance?) {
        instances[lhs][rhs] = instance
    }

    protected fun fillNumberTable(highPrecedence: Int, instance: (PType, PType) -> Function.Instance?) {
        return fillPrioritizedTable(highPrecedence, SqlTypeFamily.NUMBER, instance)
    }

    private fun fillCharacterStringTable(highPrecedence: Int, instance: (PType, PType) -> Function.Instance?) {
        return fillPrioritizedTable(highPrecedence, SqlTypeFamily.TEXT, instance)
    }

    protected fun fillPrioritizedTable(highPrecedence: Int, family: SqlTypeFamily, instance: (PType, PType) -> Function.Instance?) {
        val members = family.members + setOf(PType.UNKNOWN)
        members.filter {
            (TYPE_PRECEDENCE[highPrecedence]!! > TYPE_PRECEDENCE[it]!!)
        }.forEach {
            fillTable(highPrecedence, it) { lhs, _ -> instance(lhs, lhs) }
            fillTable(it, highPrecedence) { _, rhs -> instance(rhs, rhs) }
        }
        fillTable(highPrecedence, highPrecedence) { lhs, rhs -> instance(lhs, rhs) }
    }

    private fun fillBooleanTable(instance: (PType, PType) -> Function.Instance?) {
        fillTable(PType.BOOL, PType.BOOL) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.BOOL, PType.UNKNOWN) { lhs, _ -> instance(lhs, lhs) }
        fillTable(PType.UNKNOWN, PType.BOOL) { _, rhs -> instance(rhs, rhs) }
    }

    private fun fillTimestampTable(instance: (PType, PType) -> Function.Instance?) {
        fillTable(PType.TIMESTAMPZ, PType.TIMESTAMPZ) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.TIMESTAMP, PType.TIMESTAMP) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.TIMESTAMPZ, PType.TIMESTAMP) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.TIMESTAMP, PType.TIMESTAMPZ) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.TIMESTAMP, PType.UNKNOWN) { lhs, _ -> instance(lhs, lhs) }
        fillTable(PType.TIMESTAMPZ, PType.UNKNOWN) { lhs, _ -> instance(lhs, lhs) }
        fillTable(PType.UNKNOWN, PType.TIMESTAMP) { _, rhs -> instance(rhs, rhs) }
        fillTable(PType.UNKNOWN, PType.TIMESTAMPZ) { _, rhs -> instance(rhs, rhs) }
    }

    private fun fillTimeTable(instance: (PType, PType) -> Function.Instance?) {
        fillTable(PType.TIMEZ, PType.TIMEZ) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.TIME, PType.TIME) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.TIMEZ, PType.TIME) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.TIME, PType.TIMEZ) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.TIMEZ, PType.UNKNOWN) { lhs, _ -> instance(lhs, lhs) }
        fillTable(PType.TIME, PType.UNKNOWN) { lhs, _ -> instance(lhs, lhs) }
        fillTable(PType.UNKNOWN, PType.TIME) { _, rhs -> instance(rhs, rhs) }
        fillTable(PType.UNKNOWN, PType.TIMEZ) { _, rhs -> instance(rhs, rhs) }
    }

    private fun fillDateTable(instance: (PType, PType) -> Function.Instance?) {
        fillTable(PType.DATE, PType.DATE) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.DATE, PType.UNKNOWN) { lhs, _ -> instance(lhs, lhs) }
        fillTable(PType.UNKNOWN, PType.DATE) { _, rhs -> instance(rhs, rhs) }
    }

    private fun fillBlobTable(instance: (PType, PType) -> Function.Instance?) {
        fillTable(PType.BLOB, PType.BLOB) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.BLOB, PType.UNKNOWN) { lhs, _ -> instance(lhs, lhs) }
        fillTable(PType.UNKNOWN, PType.BLOB) { _, rhs -> instance(rhs, rhs) }
    }

    private fun fillDecimalTable() {
        // Tiny Int
        fillTable(PType.TINYINT, PType.DECIMAL) { lhs, rhs -> getDecimalInstance(DEC_TINY_INT, rhs) }
        fillTable(PType.DECIMAL, PType.TINYINT) { lhs, rhs -> getDecimalInstance(lhs, DEC_TINY_INT) }

        // Small Int
        fillTable(PType.SMALLINT, PType.DECIMAL) { lhs, rhs -> getDecimalInstance(DEC_SMALL_INT, rhs) }
        fillTable(PType.DECIMAL, PType.SMALLINT) { lhs, rhs -> getDecimalInstance(lhs, DEC_SMALL_INT) }

        // Integer
        fillTable(PType.INTEGER, PType.DECIMAL) { lhs, rhs -> getDecimalInstance(DEC_INT, rhs) }
        fillTable(PType.DECIMAL, PType.INTEGER) { lhs, rhs -> getDecimalInstance(lhs, DEC_INT) }

        // Big Int
        fillTable(PType.BIGINT, PType.DECIMAL) { lhs, rhs -> getDecimalInstance(DEC_BIG_INT, rhs) }
        fillTable(PType.DECIMAL, PType.BIGINT) { lhs, rhs -> getDecimalInstance(lhs, DEC_BIG_INT) }

        // Numeric
        fillTable(PType.NUMERIC, PType.DECIMAL) { lhs, rhs -> getDecimalInstance(PType.decimal(38, 19), rhs) } // TODO: Convert numeric to decimal once numeric is not modeled as BigInteger
        fillTable(PType.DECIMAL, PType.NUMERIC) { lhs, rhs -> getDecimalInstance(lhs, PType.decimal(38, 19)) } // TODO: Convert numeric to decimal once numeric is not modeled as BigInteger

        // Decimal
        fillTable(PType.DECIMAL, PType.DECIMAL) { lhs, rhs -> getDecimalInstance(lhs, rhs) }
        fillTable(PType.UNKNOWN, PType.DECIMAL) { lhs, rhs -> getDecimalInstance(rhs, rhs) }
        fillTable(PType.DECIMAL, PType.UNKNOWN) { lhs, rhs -> getDecimalInstance(lhs, lhs) }
    }

    protected fun fillTable() {
        fillBooleanTable(::getBooleanInstance)
        fillNumberTable(PType.TINYINT, ::getTinyIntInstance)
        fillNumberTable(PType.SMALLINT, ::getSmallIntInstance)
        fillNumberTable(PType.INTEGER, ::getIntegerInstance)
        fillNumberTable(PType.BIGINT, ::getBigIntInstance)
        fillDecimalTable()
        fillNumberTable(PType.NUMERIC, ::getNumericInstance)
        fillNumberTable(PType.REAL, ::getRealInstance)
        fillNumberTable(PType.DOUBLE, ::getDoubleInstance)
        fillTimeTable(::getTimeInstance)
        fillDateTable(::getDateInstance)
        fillBlobTable(::getBlobInstance)
        fillTimestampTable(::getTimestampInstance)
        fillCharacterStringTable(PType.STRING, ::getStringInstance)
        fillCharacterStringTable(PType.CHAR, ::getCharInstance)
        fillCharacterStringTable(PType.VARCHAR, ::getVarcharInstance)
        fillCharacterStringTable(PType.CLOB, ::getClobInstance)
    }

    protected fun basic(returns: PType, lhs: PType, rhs: PType, invocation: (Array<Datum>) -> Datum): Function.Instance {
        return Function.instance(
            name = getName(),
            returns = returns,
            parameters = arrayOf(
                Parameter("lhs", lhs),
                Parameter("rhs", rhs),
            ),
            invoke = invocation
        )
    }

    protected fun basic(returns: PType, args: PType, invocation: (Array<Datum>) -> Datum): Function.Instance {
        return basic(returns, args, args, invocation)
    }

    protected fun basic(arg: PType, invocation: (Array<Datum>) -> Datum): Function.Instance {
        return basic(arg, arg, arg, invocation)
    }
}
