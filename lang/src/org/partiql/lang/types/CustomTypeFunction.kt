package org.partiql.lang.types

import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.syntax.ParserException


/**
 * Custom type function interface to create a custom foreign type.
 * The implementations of this interface must implement the [constructStaticType] method:
 *
 *  @property arity Range of number of arguments for the custom type. The default arity range is 0..0.
 *  @property validateExprValue The optional validator that validates the [ExprValue] at run time and returns [Boolean].
 *            Default value is `null` indicating that the `ExprValue` is always valid.
 */
interface CustomTypeFunction {
    val arity: IntRange
        get() = 0..0

    val validateExprValue: ((ExprValue) -> Boolean)?
        get() = null

    /**
     * Returns a constructed [StaticType] provided by the implementation based on the [TypeParameter]s passed.
     * The static type is used by typed operators such as CAST, IS, CAN_CAST, etc.
     * Note that for now, the [StaticType] generated must be one of the existing PartiQL's primitive [StaticType]s.
     */
    fun constructStaticType(args: List<TypeParameter> = listOf()): StaticType
}

/**
 * A generic [TypeParameter] for the custom parameterized types.
 * For now, it only has [IntParameter] as one subclass, but it can easily be extended in the future to incorporate other types.
 */
sealed class TypeParameter(private val sourceLocationMeta: SourceLocationMeta? = null) {

    internal class IntParameter(val value: Int, sourceLocationMeta: SourceLocationMeta? = null) : TypeParameter(sourceLocationMeta) {
        override fun toString() = "$value"
    }

    /**
     * Returns [Int] if the [TypeParameter] is [IntParameter] and throws [ParserException] otherwise.
     */
    fun intValue(): Int = when (this) {
        is IntParameter -> this.value
        // The else part here is not reachable as of now but will be useful once we extend the TypeParameter.
        else -> throw ParserException(
            "Expected integer but found $this at $sourceLocationMeta.",
            ErrorCode.PARSE_INVALID_TYPE_PARAM,
            PropertyValueMap()
        )
    }
}

/**
 * Convenience helper function to build a [CustomTypeFunction] based on [StaticType] and [validationThunk].
 * Note that this method is not recommended for the parameterized types
 * as it does not consider parameters to generate the [StaticType].
 */
fun buildTypeFunction(
    staticType: StaticType,
    arity: IntRange = 0..0,
    validationThunk: ((ExprValue) -> Boolean)? = null
): CustomTypeFunction = when (validationThunk) {
    null -> object: CustomTypeFunction {
        override fun constructStaticType(args: List<TypeParameter>): StaticType = staticType
        override val arity: IntRange = arity
    }
    else -> object: CustomTypeFunction {
        override fun constructStaticType(args: List<TypeParameter>): StaticType = staticType

        override val validateExprValue = validationThunk

        override val arity: IntRange = arity
    }
}
