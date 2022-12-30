package org.partiql.ionschema.parser

import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.IonElement
import org.partiql.ionschema.model.IonSchemaModel

private class RangeValidator(
    val allowedTypes: Set<ElementType>
) : IonSchemaModel.Visitor() {

    private fun assertIsNumber(elem: IonElement, component: String) {
        if (!allowedTypes.contains(elem.type)) {
            modelValidationError(component, elem.type, allowedTypes.toList())
        }
    }

    override fun visitNumberExtentInclusive(node: IonSchemaModel.NumberExtent.Inclusive) =
        assertIsNumber(node.value, "Inclusive.value")

    override fun visitNumberExtentExclusive(node: IonSchemaModel.NumberExtent.Exclusive) =
        assertIsNumber(node.value, "Exclusive.value")

    override fun visitNumberRuleEqualsNumber(node: IonSchemaModel.NumberRule.EqualsNumber) =
        assertIsNumber(node.value, "EqualsNumber.value")
}

internal val NUMBER_TYPES = setOf(ElementType.INT, ElementType.FLOAT, ElementType.DECIMAL)
private val NUMBER_RANGE_VALIDATOR = RangeValidator(NUMBER_TYPES)
private val INT_RANGE_VALIDATOR = RangeValidator(setOf(ElementType.INT))

/**
 * Validates that the specified [IonSchemaModel.NumberRange] contains only Ion numbers.
 *
 * Throws an exception of the range contains a value that is not an `int`, `float`, or `decimal`.
 *
 * This is necessary because at this time PIG does not support generics.
 */
internal fun validateNumberRange(range: IonSchemaModel.NumberRange) =
    NUMBER_RANGE_VALIDATOR.walkNumberRange(range)

/**
 * Validates that the specified [IonSchemaModel.NumberRange] contains only Ion integers.
 *
 * Throws an exception of the range contains a value that is not an `int`.
 *
 * This is necessary because at this time PIG does not support a `number` type, only `int`.
 */
internal fun validateIntRule(rule: IonSchemaModel.NumberRule) =
    INT_RANGE_VALIDATOR.walkNumberRule(rule)
