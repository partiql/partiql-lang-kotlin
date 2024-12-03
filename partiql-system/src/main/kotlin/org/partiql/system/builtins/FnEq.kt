// ktlint-disable filename
@file:Suppress("ClassName")

package org.partiql.system.builtins

import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

/**
 * According to SQL:1999:
 * > If either XV or YV is the null value, then `X <comp op> Y` is unknown
 *
 * According to the PartiQL Specification:
 * > Equality never fails in the type-checking mode and never returns MISSING in the permissive mode. Instead, it can
 * compare values of any two types, according to the rules of the PartiQL type system. For example, 5 = 'a' is false.
 *
 * For the existing conformance tests, whenever an operand is NULL or MISSING, the output is NULL. This implementation
 * follows this.
 *
 * TODO: The PartiQL Specification needs to clearly define the semantics of MISSING. That being said, this implementation
 *  follows the existing conformance tests and SQL:1999.
 */
internal object FnEq : Function {

    // Memoize shared variables
    private val comparator = Datum.comparator()
    private val boolType = PType.bool()
    private val nullValue = Datum.nullValue(boolType)

    override fun getName(): String {
        return "eq"
    }

    override fun getParameters(): Array<Parameter> {
        return arrayOf(Parameter.dynamic("lhs"), Parameter.dynamic("rhs"))
    }

    override fun getInstance(args: Array<PType>): Function.Instance {
        return object : Function.Instance(
            "eq",
            args,
            boolType,
            isNullCall = true,
            isMissingCall = false
        ) {
            override fun invoke(args: Array<Datum>): Datum {
                val lhs = args[0]
                val rhs = args[1]
                if (lhs.isMissing || rhs.isMissing) {
                    return nullValue
                }
                return Datum.bool(comparator.compare(lhs, rhs) == 0)
            }
        }
    }

    override fun getReturnType(args: Array<PType>): PType {
        return getInstance(args).returns
    }
}
