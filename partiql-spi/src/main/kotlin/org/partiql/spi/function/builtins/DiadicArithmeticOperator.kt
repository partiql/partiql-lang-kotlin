package org.partiql.spi.function.builtins

import org.partiql.spi.function.Fn
import org.partiql.spi.types.PType

/**
 * This carries along with it a static table containing a mapping between the input types and the implementation.
 * @param hidesName dictates whether the [name] should be hidden; true by default.
 */
internal abstract class DiadicArithmeticOperator(name: String, hidesName: Boolean = true) : DiadicOperator(
    name,
    PType.doublePrecision(),
    PType.doublePrecision(),
    hidesName = hidesName
) {
    override fun getUnknownInstance(): Fn? {
        return getDoubleInstance(PType.doublePrecision(), PType.doublePrecision())
    }

    override fun getIntervalInstance(lhs: PType, rhs: PType): Fn? {
        if (lhs.code() == PType.UNKNOWN || rhs.code() == PType.UNKNOWN) {
            return basic(PType.unknown(), lhs, rhs) { args -> throw NotImplementedError() }
        }
        return super.getIntervalInstance(lhs, rhs)
    }

    override fun getDateInstance(dateLhs: PType, dateRhs: PType): Fn? {
        if (dateLhs.code() == PType.UNKNOWN || dateRhs.code() == PType.UNKNOWN) {
            return basic(PType.unknown(), dateLhs, dateRhs) { args -> throw NotImplementedError() }
        }
        return super.getDateInstance(dateLhs, dateRhs)
    }

    override fun getTimeInstance(timeLhs: PType, timeRhs: PType): Fn? {
        if (timeLhs.code() == PType.UNKNOWN || timeRhs.code() == PType.UNKNOWN) {
            return basic(PType.unknown(), timeLhs, timeRhs) { args -> throw NotImplementedError() }
        }
        return super.getTimeInstance(timeLhs, timeRhs)
    }

    override fun getTimestampInstance(timestampLhs: PType, timestampRhs: PType): Fn? {
        if (timestampLhs.code() == PType.UNKNOWN || timestampRhs.code() == PType.UNKNOWN) {
            return basic(PType.unknown(), timestampLhs, timestampRhs) { args -> throw NotImplementedError() }
        }
        return super.getTimestampInstance(timestampLhs, timestampRhs)
    }
}
