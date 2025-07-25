package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.RoutineOverloadSignature
import org.partiql.spi.function.builtins.TypePrecedence.TYPE_PRECEDENCE
import org.partiql.spi.internal.SqlTypeFamily
import org.partiql.spi.types.PType
import org.partiql.spi.utils.FunctionUtils
import org.partiql.spi.value.Datum

/**
 * This represents an operator backed by a function overload. Note that the name of the operator is hidden
 * using [FunctionUtils.hide].
 *
 * This carries along with it a static table containing a mapping between the input types and the implementation.
 *
 * Implementations of this should invoke [fillTable] in the constructor of the function.
 * @param hidesName dictates whether the [name] should be hidden; true by default.
 */
internal abstract class DiadicOperator(
    name: String,
    private val lhs: PType,
    private val rhs: PType,
    hidesName: Boolean = true
) : FnOverload() {

    private val name = when (hidesName) {
        true -> FunctionUtils.hide(name)
        false -> name
    }

    companion object {
        private val DEC_TINY_INT = PType.decimal(3, 0)
        private val NUM_TINY_INT = PType.numeric(3, 0)
        private val DEC_SMALL_INT = PType.decimal(5, 0)
        private val NUM_SMALL_INT = PType.numeric(5, 0)
        private val DEC_INT = PType.decimal(10, 0)
        private val NUM_INT = PType.numeric(10, 0)
        private val DEC_BIG_INT = PType.decimal(19, 0)
        private val NUM_BIG_INT = PType.numeric(19, 0)
    }

    override fun getSignature(): RoutineOverloadSignature {
        return RoutineOverloadSignature(name, listOf(lhs, rhs))
    }

    override fun getInstance(args: Array<PType>): Fn? {
        val lhs = args[0]
        val rhs = args[1]
        val (newLhs, newRhs) = getOperands(lhs, rhs) ?: return null
        val instance = instances[lhs.code()][rhs.code()]
        return instance(newLhs, newRhs)
    }

    private fun getOperands(lhs: PType, rhs: PType): Pair<PType, PType>? {
        // Check if not in same type family. There is no "top" type between them.
        if (!SqlTypeFamily.of(lhs).contains(rhs)) {
            return lhs to rhs
        }

        val lhsPrecedence = TYPE_PRECEDENCE[lhs.code()] ?: return null
        val rhsPrecedence = TYPE_PRECEDENCE[rhs.code()] ?: return null
        val (newLhs, newRhs) = when (lhsPrecedence.compareTo(rhsPrecedence)) {
            -1 -> (rhs to rhs)
            0 -> (lhs to rhs)
            else -> (lhs to lhs)
        }
        return newLhs to newRhs
    }

    /**
     * @param booleanLhs TODO
     * @param booleanRhs TODO
     * @return TODO
     */
    open fun getBooleanInstance(booleanLhs: PType, booleanRhs: PType): Fn? {
        return null
    }

    /**
     * @param stringLhs TODO
     * @param stringRhs TODO
     * @return TODO
     */
    open fun getStringInstance(stringLhs: PType, stringRhs: PType): Fn? {
        return null
    }

    /**
     * @param charLhs TODO
     * @param charRhs TODO
     * @return TODO
     */
    open fun getCharInstance(charLhs: PType, charRhs: PType): Fn? {
        return null
    }

    /**
     * @param varcharLhs TODO
     * @param varcharRhs TODO
     * @return TODO
     */
    open fun getVarcharInstance(varcharLhs: PType, varcharRhs: PType): Fn? {
        return null
    }

    /**
     * @param blobLhs TODO
     * @param blobRhs TODO
     * @return TODO
     */
    open fun getBlobInstance(blobLhs: PType, blobRhs: PType): Fn? {
        return null
    }

    /**
     * @param clobLhs TODO
     * @param clobRhs TODO
     * @return TODO
     */
    open fun getClobInstance(clobLhs: PType, clobRhs: PType): Fn? {
        return null
    }

    /**
     * @param dateLhs TODO
     * @param dateRhs TODO
     * @return TODO
     */
    open fun getDateInstance(dateLhs: PType, dateRhs: PType): Fn? {
        return null
    }

    /**
     * @param timeLhs TODO
     * @param timeRhs TODO
     * @return TODO
     */
    open fun getTimeInstance(timeLhs: PType, timeRhs: PType): Fn? {
        return null
    }

    /**
     * @param timestampLhs TODO
     * @param timestampRhs TODO
     * @return TODO
     */
    open fun getTimestampInstance(timestampLhs: PType, timestampRhs: PType): Fn? {
        return null
    }

    /**
     * @param integerLhs TODO
     * @param integerRhs TODO
     * @return TODO
     */
    open fun getIntegerInstance(integerLhs: PType, integerRhs: PType): Fn? {
        return null
    }

    /**
     * @param tinyIntLhs TODO
     * @param tinyIntRhs TODO
     * @return TODO
     */
    open fun getTinyIntInstance(tinyIntLhs: PType, tinyIntRhs: PType): Fn? {
        return null
    }

    /**
     * @param smallIntLhs TODO
     * @param smallIntRhs TODO
     * @return TODO
     */
    open fun getSmallIntInstance(smallIntLhs: PType, smallIntRhs: PType): Fn? {
        return null
    }

    /**
     * @param bigIntLhs TODO
     * @param bigIntRhs TODO
     * @return TODO
     */
    open fun getBigIntInstance(bigIntLhs: PType, bigIntRhs: PType): Fn? {
        return null
    }

    /**
     * @param numericLhs TODO
     * @param numericRhs TODO
     * @return TODO
     */
    open fun getNumericInstance(numericLhs: PType, numericRhs: PType): Fn? {
        return null
    }

    /**
     * @param decimalLhs TODO
     * @param decimalRhs TODO
     * @return TODO
     */
    open fun getDecimalInstance(decimalLhs: PType, decimalRhs: PType): Fn? {
        return null
    }

    /**
     * @param realLhs TODO
     * @param realRhs TODO
     * @return TODO
     */
    open fun getRealInstance(realLhs: PType, realRhs: PType): Fn? {
        return null
    }

    /**
     * @param doubleLhs TODO
     * @param doubleRhs TODO
     * @return TODO
     */
    open fun getDoubleInstance(doubleLhs: PType, doubleRhs: PType): Fn? {
        return null
    }

    /**
     * @param lhs a type of interval
     * @param rhs a type of interval
     * @return an instance of a function
     */
    open fun getIntervalInstance(lhs: PType, rhs: PType): Fn? {
        return null
    }

    /**
     * @param lhs a type of date
     * @param rhs a type of interval
     * @return an instance of a function
     */
    open fun getDateIntervalInstance(lhs: PType, rhs: PType): Fn? {
        return null
    }

    /**
     * @param lhs a type of time
     * @param rhs a type of interval
     * @return an instance of a function
     */
    open fun getTimeIntervalInstance(lhs: PType, rhs: PType): Fn? {
        return null
    }

    /**
     * @param lhs a type of timestamp
     * @param rhs a type of interval
     * @return an instance of a function
     */
    open fun getTimestampIntervalInstance(lhs: PType, rhs: PType): Fn? {
        return null
    }

    /**
     * @param lhs a type of interval
     * @param rhs a type of date
     * @return an instance of a function
     */
    open fun getIntervalDateInstance(lhs: PType, rhs: PType): Fn? {
        return null
    }

    /**
     * @param lhs a type of interval
     * @param rhs a type of time
     * @return an instance of a function
     */
    open fun getIntervalTimeInstance(lhs: PType, rhs: PType): Fn? {
        return null
    }

    /**
     * @param lhs a type of interval
     * @param rhs a type of timestamp
     * @return an instance of a function
     */
    open fun getIntervalTimestampInstance(lhs: PType, rhs: PType): Fn? {
        return null
    }

    /**
     * @param lhs a type of number
     * @param rhs a type of interval
     * @return an instance of a function
     */
    open fun getNumberIntervalInstance(lhs: PType, rhs: PType): Fn? {
        return null
    }

    /**
     * @param lhs a type of interval
     * @param rhs a type of number
     * @return an instance of a function
     */
    open fun getIntervalNumberInstance(lhs: PType, rhs: PType): Fn? {
        return null
    }

    /**
     * This is used when all operands are NULL/MISSING.
     * @return an instance of a function
     */
    open fun getUnknownInstance(): Fn? {
        return null
    }

    /**
     * This is a lookup table for finding the appropriate instance for the given types. The table is
     * initialized on construction using the get*Instance methods.
     */
    protected val instances: Array<Array<(PType, PType) -> Fn?>> = Array(PType.codes().size) {
        Array(PType.codes().size) {
            { _, _ -> null }
        }
    }

    protected fun fillTable(lhs: Int, rhs: Int, instance: (PType, PType) -> Fn?) {
        instances[lhs][rhs] = instance
    }

    protected fun fillNumberTable(highPrecedence: Int, instance: (PType, PType) -> Fn?) {
        return fillPrioritizedTable(highPrecedence, SqlTypeFamily.NUMBER, instance)
    }

    private fun fillCharacterStringTable(highPrecedence: Int, instance: (PType, PType) -> Fn?) {
        return fillPrioritizedTable(highPrecedence, SqlTypeFamily.TEXT, instance)
    }

    protected fun fillPrioritizedTable(highPrecedence: Int, family: SqlTypeFamily, instance: (PType, PType) -> Fn?) {
        val members = family.members + setOf(PType.UNKNOWN)
        members.filter {
            (TYPE_PRECEDENCE[highPrecedence]!! > TYPE_PRECEDENCE[it]!!)
        }.forEach {
            fillTable(highPrecedence, it) { lhs, _ -> instance(lhs, lhs) }
            fillTable(it, highPrecedence) { _, rhs -> instance(rhs, rhs) }
        }
        fillTable(highPrecedence, highPrecedence) { lhs, rhs -> instance(lhs, rhs) }
    }

    private fun fillBooleanTable(instance: (PType, PType) -> Fn?) {
        fillTable(PType.BOOL, PType.BOOL) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.BOOL, PType.UNKNOWN) { lhs, _ -> instance(lhs, lhs) }
        fillTable(PType.UNKNOWN, PType.BOOL) { _, rhs -> instance(rhs, rhs) }
    }

    private fun fillIntervalTable() {
        fillTable(PType.INTERVAL_YM, PType.INTERVAL_YM, ::getIntervalInstance)
        fillTable(PType.INTERVAL_YM, PType.INTERVAL_DT, ::getIntervalInstance)
        fillTable(PType.INTERVAL_YM, PType.UNKNOWN, ::getIntervalInstance)
        fillTable(PType.INTERVAL_DT, PType.INTERVAL_DT, ::getIntervalInstance)
        fillTable(PType.INTERVAL_DT, PType.INTERVAL_YM, ::getIntervalInstance)
        fillTable(PType.INTERVAL_DT, PType.UNKNOWN, ::getIntervalInstance)
        fillTable(PType.UNKNOWN, PType.INTERVAL_YM, ::getIntervalInstance)
        fillTable(PType.UNKNOWN, PType.INTERVAL_DT, ::getIntervalInstance)

        fillTable(PType.DATE, PType.INTERVAL_YM, ::getDateIntervalInstance)
        fillTable(PType.DATE, PType.INTERVAL_DT, ::getDateIntervalInstance)
        fillTable(PType.TIME, PType.INTERVAL_YM, ::getTimeIntervalInstance)
        fillTable(PType.TIME, PType.INTERVAL_DT, ::getTimeIntervalInstance)
        fillTable(PType.TIMEZ, PType.INTERVAL_DT, ::getTimeIntervalInstance) // TODO: Create a specific timezone impl
        fillTable(PType.TIMESTAMP, PType.INTERVAL_YM, ::getTimestampIntervalInstance)
        fillTable(PType.TIMESTAMP, PType.INTERVAL_DT, ::getTimestampIntervalInstance)
        fillTable(PType.TIMESTAMPZ, PType.INTERVAL_YM, ::getTimestampIntervalInstance) // TODO: Create a specific timezone impl
        fillTable(PType.TIMESTAMPZ, PType.INTERVAL_DT, ::getTimestampIntervalInstance) // TODO: Create a specific timezone impl
        fillTable(PType.INTERVAL_YM, PType.DATE, ::getIntervalDateInstance)
        fillTable(PType.INTERVAL_DT, PType.DATE, ::getIntervalDateInstance)
        fillTable(PType.INTERVAL_DT, PType.TIME, ::getIntervalTimeInstance)
        fillTable(PType.INTERVAL_DT, PType.TIMEZ, ::getIntervalTimeInstance) // TODO: Create a specific timezone impl
        fillTable(PType.INTERVAL_YM, PType.TIMESTAMP, ::getIntervalTimestampInstance)
        fillTable(PType.INTERVAL_DT, PType.TIMESTAMP, ::getIntervalTimestampInstance)
        fillTable(PType.INTERVAL_YM, PType.TIMESTAMPZ, ::getIntervalTimestampInstance) // TODO: Create a specific timezone impl

        // TinyInt
        fillTable(PType.INTERVAL_YM, PType.TINYINT, ::getIntervalNumberInstance)
        fillTable(PType.INTERVAL_DT, PType.TINYINT, ::getIntervalNumberInstance)
        fillTable(PType.TINYINT, PType.INTERVAL_YM, ::getNumberIntervalInstance)
        fillTable(PType.TINYINT, PType.INTERVAL_DT, ::getNumberIntervalInstance)

        // SmallInt
        fillTable(PType.INTERVAL_YM, PType.SMALLINT, ::getIntervalNumberInstance)
        fillTable(PType.INTERVAL_DT, PType.SMALLINT, ::getIntervalNumberInstance)
        fillTable(PType.SMALLINT, PType.INTERVAL_YM, ::getNumberIntervalInstance)
        fillTable(PType.SMALLINT, PType.INTERVAL_DT, ::getNumberIntervalInstance)

        // Integer
        fillTable(PType.INTERVAL_YM, PType.INTEGER, ::getIntervalNumberInstance)
        fillTable(PType.INTERVAL_DT, PType.INTEGER, ::getIntervalNumberInstance)
        fillTable(PType.INTEGER, PType.INTERVAL_YM, ::getNumberIntervalInstance)
        fillTable(PType.INTEGER, PType.INTERVAL_DT, ::getNumberIntervalInstance)

        // BigInt
        fillTable(PType.INTERVAL_YM, PType.BIGINT, ::getIntervalNumberInstance)
        fillTable(PType.INTERVAL_DT, PType.BIGINT, ::getIntervalNumberInstance)
        fillTable(PType.BIGINT, PType.INTERVAL_YM, ::getNumberIntervalInstance)
        fillTable(PType.BIGINT, PType.INTERVAL_DT, ::getNumberIntervalInstance)

        // Numeric
        fillTable(PType.INTERVAL_YM, PType.NUMERIC, ::getIntervalNumberInstance)
        fillTable(PType.INTERVAL_DT, PType.NUMERIC, ::getIntervalNumberInstance)
        fillTable(PType.NUMERIC, PType.INTERVAL_YM, ::getNumberIntervalInstance)
        fillTable(PType.NUMERIC, PType.INTERVAL_DT, ::getNumberIntervalInstance)

        // Decimal
        fillTable(PType.INTERVAL_YM, PType.DECIMAL, ::getIntervalNumberInstance)
        fillTable(PType.INTERVAL_DT, PType.DECIMAL, ::getIntervalNumberInstance)
        fillTable(PType.DECIMAL, PType.INTERVAL_YM, ::getNumberIntervalInstance)
        fillTable(PType.DECIMAL, PType.INTERVAL_DT, ::getNumberIntervalInstance)

        // REAL
        fillTable(PType.INTERVAL_YM, PType.REAL, ::getIntervalNumberInstance)
        fillTable(PType.INTERVAL_DT, PType.REAL, ::getIntervalNumberInstance)
        fillTable(PType.REAL, PType.INTERVAL_YM, ::getNumberIntervalInstance)
        fillTable(PType.NUMERIC, PType.INTERVAL_DT, ::getNumberIntervalInstance)

        // DOUBLE
        fillTable(PType.INTERVAL_YM, PType.DOUBLE, ::getIntervalNumberInstance)
        fillTable(PType.INTERVAL_DT, PType.DOUBLE, ::getIntervalNumberInstance)
        fillTable(PType.DOUBLE, PType.INTERVAL_YM, ::getNumberIntervalInstance)
        fillTable(PType.DOUBLE, PType.INTERVAL_DT, ::getNumberIntervalInstance)
    }

    private fun fillTimestampTable(instance: (PType, PType) -> Fn?) {
        fillTable(PType.TIMESTAMPZ, PType.TIMESTAMPZ) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.TIMESTAMP, PType.TIMESTAMP) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.TIMESTAMPZ, PType.TIMESTAMP) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.TIMESTAMP, PType.TIMESTAMPZ) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.TIMESTAMP, PType.UNKNOWN) { lhs, _ -> instance(lhs, lhs) }
        fillTable(PType.TIMESTAMPZ, PType.UNKNOWN) { lhs, _ -> instance(lhs, lhs) }
        fillTable(PType.UNKNOWN, PType.TIMESTAMP) { _, rhs -> instance(rhs, rhs) }
        fillTable(PType.UNKNOWN, PType.TIMESTAMPZ) { _, rhs -> instance(rhs, rhs) }
    }

    private fun fillTimeTable(instance: (PType, PType) -> Fn?) {
        fillTable(PType.TIMEZ, PType.TIMEZ) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.TIME, PType.TIME) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.TIMEZ, PType.TIME) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.TIME, PType.TIMEZ) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.TIMEZ, PType.UNKNOWN) { lhs, _ -> instance(lhs, lhs) }
        fillTable(PType.TIME, PType.UNKNOWN) { lhs, _ -> instance(lhs, lhs) }
        fillTable(PType.UNKNOWN, PType.TIME) { _, rhs -> instance(rhs, rhs) }
        fillTable(PType.UNKNOWN, PType.TIMEZ) { _, rhs -> instance(rhs, rhs) }
    }

    private fun fillDateTable(instance: (PType, PType) -> Fn?) {
        fillTable(PType.DATE, PType.DATE) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.DATE, PType.UNKNOWN) { lhs, _ -> instance(lhs, lhs) }
        fillTable(PType.UNKNOWN, PType.DATE) { _, rhs -> instance(rhs, rhs) }
    }

    private fun fillDatetimeIntervalTable(instance: (PType, PType) -> Fn?) {
        fillTable(PType.DATE, PType.INTERVAL_YM) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.DATE, PType.INTERVAL_DT) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.TIME, PType.INTERVAL_YM) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.TIME, PType.INTERVAL_DT) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.TIMEZ, PType.INTERVAL_YM) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.TIMEZ, PType.INTERVAL_DT) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.TIMESTAMP, PType.INTERVAL_YM) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.TIMESTAMP, PType.INTERVAL_DT) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.TIMESTAMPZ, PType.INTERVAL_YM) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.TIMESTAMPZ, PType.INTERVAL_DT) { lhs, rhs -> instance(lhs, rhs) }
    }

    private fun fillBlobTable(instance: (PType, PType) -> Fn?) {
        fillTable(PType.BLOB, PType.BLOB) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.BLOB, PType.UNKNOWN) { lhs, _ -> instance(lhs, lhs) }
        fillTable(PType.UNKNOWN, PType.BLOB) { _, rhs -> instance(rhs, rhs) }
    }

    private fun fillNumericTable() {
        // Tiny Int
        fillTable(PType.TINYINT, PType.NUMERIC) { lhs, rhs -> getNumericInstance(NUM_TINY_INT, rhs) }
        fillTable(PType.NUMERIC, PType.TINYINT) { lhs, rhs -> getNumericInstance(lhs, NUM_TINY_INT) }

        // Small Int
        fillTable(PType.SMALLINT, PType.NUMERIC) { lhs, rhs -> getNumericInstance(NUM_SMALL_INT, rhs) }
        fillTable(PType.NUMERIC, PType.SMALLINT) { lhs, rhs -> getNumericInstance(lhs, NUM_SMALL_INT) }

        // Integer
        fillTable(PType.INTEGER, PType.NUMERIC) { lhs, rhs -> getNumericInstance(NUM_INT, rhs) }
        fillTable(PType.NUMERIC, PType.INTEGER) { lhs, rhs -> getNumericInstance(lhs, NUM_INT) }

        // Big Int
        fillTable(PType.BIGINT, PType.NUMERIC) { lhs, rhs -> getNumericInstance(NUM_BIG_INT, rhs) }
        fillTable(PType.NUMERIC, PType.BIGINT) { lhs, rhs -> getNumericInstance(lhs, NUM_BIG_INT) }

        // Numeric
        fillTable(PType.NUMERIC, PType.NUMERIC) { lhs, rhs -> getNumericInstance(lhs, rhs) }
        fillTable(PType.UNKNOWN, PType.NUMERIC) { lhs, rhs -> getNumericInstance(rhs, rhs) }
        fillTable(PType.NUMERIC, PType.UNKNOWN) { lhs, rhs -> getNumericInstance(lhs, lhs) }
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
        fillTable(PType.NUMERIC, PType.DECIMAL) { lhs, rhs -> getDecimalInstance(PType.decimal(lhs.precision, lhs.scale), rhs) }
        fillTable(PType.DECIMAL, PType.NUMERIC) { lhs, rhs -> getDecimalInstance(lhs, PType.decimal(rhs.precision, rhs.scale)) }

        // Decimal
        fillTable(PType.DECIMAL, PType.DECIMAL) { lhs, rhs -> getDecimalInstance(lhs, rhs) }
        fillTable(PType.UNKNOWN, PType.DECIMAL) { lhs, rhs -> getDecimalInstance(rhs, rhs) }
        fillTable(PType.DECIMAL, PType.UNKNOWN) { lhs, rhs -> getDecimalInstance(lhs, lhs) }
    }

    private fun fillUnknownTable() {
        fillTable(PType.UNKNOWN, PType.UNKNOWN) { _, _ -> getUnknownInstance() }
    }

    protected fun fillTable() {
        fillBooleanTable(::getBooleanInstance)
        fillNumberTable(PType.TINYINT, ::getTinyIntInstance)
        fillNumberTable(PType.SMALLINT, ::getSmallIntInstance)
        fillNumberTable(PType.INTEGER, ::getIntegerInstance)
        fillNumberTable(PType.BIGINT, ::getBigIntInstance)
        fillDecimalTable()
        fillNumericTable()
        fillNumberTable(PType.REAL, ::getRealInstance)
        fillNumberTable(PType.DOUBLE, ::getDoubleInstance)
        fillTimeTable(::getTimeInstance)
        fillDateTable(::getDateInstance)
        fillIntervalTable()
        fillBlobTable(::getBlobInstance)
        fillTimestampTable(::getTimestampInstance)
        fillCharacterStringTable(PType.STRING, ::getStringInstance)
        fillCharacterStringTable(PType.CHAR, ::getCharInstance)
        fillCharacterStringTable(PType.VARCHAR, ::getVarcharInstance)
        fillCharacterStringTable(PType.CLOB, ::getClobInstance)
        fillUnknownTable()
    }

    protected fun basic(returns: PType, lhs: PType, rhs: PType, invocation: (Array<Datum>) -> Datum): Fn {
        return Function.instance(
            name = name,
            returns = returns,
            parameters = arrayOf(
                Parameter("lhs", lhs),
                Parameter("rhs", rhs),
            ),
            invoke = invocation
        )
    }

    protected fun basic(returns: PType, args: PType, invocation: (Array<Datum>) -> Datum): Fn {
        return basic(returns, args, args, invocation)
    }

    protected fun basic(arg: PType, invocation: (Array<Datum>) -> Datum): Fn {
        return basic(arg, arg, arg, invocation)
    }
}
