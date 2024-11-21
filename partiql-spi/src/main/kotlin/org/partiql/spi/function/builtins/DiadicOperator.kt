package org.partiql.spi.function.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.function.builtins.TypePrecedence.TYPE_PRECEDENCE
import org.partiql.spi.internal.SqlTypeFamily
import org.partiql.spi.value.Datum
import org.partiql.types.PType

/**
 * This carries along with it a static table containing a mapping between the input types and the implementation.
 *
 * Implementations of this should invoke [fillTable] in the constructor of the function.
 */
internal abstract class DiadicOperator(
    private val name: String,
    private val lhs: Parameter,
    private val rhs: Parameter
) : Function {

    override fun getName(): String {
        return name
    }

    override fun getParameters(): Array<Parameter> {
        return arrayOf(lhs, rhs)
    }

    override fun getInstance(args: Array<PType>): Function.Instance? {
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
    protected val instances: Array<Array<(PType, PType) -> Function.Instance?>> = Array(PType.Kind.entries.size) {
        Array(PType.Kind.entries.size) {
            { _, _ -> null }
        }
    }

    protected fun fillTable(lhs: PType.Kind, rhs: PType.Kind, instance: (PType, PType) -> Function.Instance?) {
        instances[lhs.ordinal][rhs.ordinal] = instance
    }

    protected fun fillNumberTable(highPrecedence: PType.Kind, instance: (PType, PType) -> Function.Instance?) {
        return fillPrioritizedTable(highPrecedence, SqlTypeFamily.NUMBER, instance)
    }

    private fun fillCharacterStringTable(highPrecedence: PType.Kind, instance: (PType, PType) -> Function.Instance?) {
        return fillPrioritizedTable(highPrecedence, SqlTypeFamily.TEXT, instance)
    }

    protected fun fillPrioritizedTable(highPrecedence: PType.Kind, family: SqlTypeFamily, instance: (PType, PType) -> Function.Instance?) {
        val members = family.members + setOf(PType.Kind.UNKNOWN)
        members.filter {
            (TYPE_PRECEDENCE[highPrecedence]!! > TYPE_PRECEDENCE[it]!!)
        }.forEach {
            fillTable(highPrecedence, it) { lhs, _ -> instance(lhs, lhs) }
            fillTable(it, highPrecedence) { _, rhs -> instance(rhs, rhs) }
        }
        fillTable(highPrecedence, highPrecedence) { lhs, rhs -> instance(lhs, rhs) }
    }

    private fun fillBooleanTable(instance: (PType, PType) -> Function.Instance?) {
        fillTable(PType.Kind.BOOL, PType.Kind.BOOL) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.Kind.BOOL, PType.Kind.UNKNOWN) { lhs, _ -> instance(lhs, lhs) }
        fillTable(PType.Kind.UNKNOWN, PType.Kind.BOOL) { _, rhs -> instance(rhs, rhs) }
    }

    private fun fillTimestampTable(instance: (PType, PType) -> Function.Instance?) {
        fillTable(PType.Kind.TIMESTAMPZ, PType.Kind.TIMESTAMPZ) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.Kind.TIMESTAMP, PType.Kind.TIMESTAMP) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.Kind.TIMESTAMPZ, PType.Kind.TIMESTAMP) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.Kind.TIMESTAMP, PType.Kind.TIMESTAMPZ) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.Kind.TIMESTAMP, PType.Kind.UNKNOWN) { lhs, _ -> instance(lhs, lhs) }
        fillTable(PType.Kind.TIMESTAMPZ, PType.Kind.UNKNOWN) { lhs, _ -> instance(lhs, lhs) }
        fillTable(PType.Kind.UNKNOWN, PType.Kind.TIMESTAMP) { _, rhs -> instance(rhs, rhs) }
        fillTable(PType.Kind.UNKNOWN, PType.Kind.TIMESTAMPZ) { _, rhs -> instance(rhs, rhs) }
    }

    private fun fillTimeTable(instance: (PType, PType) -> Function.Instance?) {
        fillTable(PType.Kind.TIMEZ, PType.Kind.TIMEZ) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.Kind.TIME, PType.Kind.TIME) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.Kind.TIMEZ, PType.Kind.TIME) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.Kind.TIME, PType.Kind.TIMEZ) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.Kind.TIMEZ, PType.Kind.UNKNOWN) { lhs, _ -> instance(lhs, lhs) }
        fillTable(PType.Kind.TIME, PType.Kind.UNKNOWN) { lhs, _ -> instance(lhs, lhs) }
        fillTable(PType.Kind.UNKNOWN, PType.Kind.TIME) { _, rhs -> instance(rhs, rhs) }
        fillTable(PType.Kind.UNKNOWN, PType.Kind.TIMEZ) { _, rhs -> instance(rhs, rhs) }
    }

    private fun fillDateTable(instance: (PType, PType) -> Function.Instance?) {
        fillTable(PType.Kind.DATE, PType.Kind.DATE) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.Kind.DATE, PType.Kind.UNKNOWN) { lhs, _ -> instance(lhs, lhs) }
        fillTable(PType.Kind.UNKNOWN, PType.Kind.DATE) { _, rhs -> instance(rhs, rhs) }
    }

    private fun fillBlobTable(instance: (PType, PType) -> Function.Instance?) {
        fillTable(PType.Kind.BLOB, PType.Kind.BLOB) { lhs, rhs -> instance(lhs, rhs) }
        fillTable(PType.Kind.BLOB, PType.Kind.UNKNOWN) { lhs, _ -> instance(lhs, lhs) }
        fillTable(PType.Kind.UNKNOWN, PType.Kind.BLOB) { _, rhs -> instance(rhs, rhs) }
    }

    open fun fillDecimalTable() {
        fillNumberTable(PType.Kind.DECIMAL, ::getDecimalInstance)
    }

    open fun fillTable() {
        fillBooleanTable(::getBooleanInstance)
        fillNumberTable(PType.Kind.TINYINT, ::getTinyIntInstance)
        fillNumberTable(PType.Kind.SMALLINT, ::getSmallIntInstance)
        fillNumberTable(PType.Kind.INTEGER, ::getIntegerInstance)
        fillNumberTable(PType.Kind.BIGINT, ::getBigIntInstance)
        fillDecimalTable()
        fillNumberTable(PType.Kind.NUMERIC, ::getNumericInstance)
        fillNumberTable(PType.Kind.REAL, ::getRealInstance)
        fillNumberTable(PType.Kind.DOUBLE, ::getDoubleInstance)
        fillTimeTable(::getTimeInstance)
        fillDateTable(::getDateInstance)
        fillBlobTable(::getBlobInstance)
        fillTimestampTable(::getTimestampInstance)
        fillCharacterStringTable(PType.Kind.STRING, ::getStringInstance)
        fillCharacterStringTable(PType.Kind.CHAR, ::getCharInstance)
        fillCharacterStringTable(PType.Kind.VARCHAR, ::getVarcharInstance)
        fillCharacterStringTable(PType.Kind.CLOB, ::getClobInstance)
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
