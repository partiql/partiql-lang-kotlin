package org.partiql.ionschema.parser

import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.IonElement
import org.partiql.ionschema.model.IonSchemaModel

private object IonSchemaModelValidator : IonSchemaModel.Visitor() {

    /** Constrains `byte_length` values to integers. */
    override fun visitConstraintByteLength(node: IonSchemaModel.Constraint.ByteLength) = validateIntRule(node.rule)

    /** Constrains `codepoint_length` values to integers. */
    override fun visitConstraintCodepointLength(node: IonSchemaModel.Constraint.CodepointLength) =
        validateIntRule(node.rule)

    /** Constrains `container_length` values to integers. */
    override fun visitConstraintContainerLength(node: IonSchemaModel.Constraint.ContainerLength) =
        validateIntRule(node.rule)

    /** Constrains `precision` values to integers. */
    override fun visitConstraintPrecision(node: IonSchemaModel.Constraint.Precision) = validateIntRule(node.rule)

    /** Constrains `scale` values to integers. */
    override fun visitConstraintScale(node: IonSchemaModel.Constraint.Scale) = validateIntRule(node.rule)

    /** Constrains `occurs` values to integers. */
    override fun visitOccursSpecOccursRule(node: IonSchemaModel.OccursSpec.OccursRule) = validateIntRule(node.rule)

    /** Constrains `imported_type`'s `nullable` annotation to booleans. */
    override fun visitTypeReferenceImportedType(node: IonSchemaModel.TypeReference.ImportedType) =
        requireBooleanType(node.nullable, "ImportedType.nullable")

    /** Constrains `inline_type`'s `nullable` annotation to booleans. */
    override fun visitTypeReferenceInlineType(node: IonSchemaModel.TypeReference.InlineType) =
        requireBooleanType(node.nullable, "InlineType.nullable")

    /** Constrains `named_type`'s `nullable` annotation to booleans. */
    override fun visitTypeReferenceNamedType(node: IonSchemaModel.TypeReference.NamedType) =
        requireBooleanType(node.nullable, "NamedType.nullable")

    /** Constrains `utf8_byte_length` to integers. */
    override fun visitConstraintUtf8ByteLength(node: IonSchemaModel.Constraint.Utf8ByteLength) {
        validateIntRule(node.rule)
    }

    /**
     * Constrains `valid_values: range::...` values to *numbers*.
     * The reason we can't just override [visitNumberExtent] and [visitNumberRule] directly is because
     * the validation must be applied *differently* to this particular number range.  For `valid_values`,
     * any number is allowed.
     */
    override fun visitValuesRangeNumRange(node: IonSchemaModel.ValuesRange.NumRange) =
        validateNumberRange(node.range)

    /** Constrains `regex`'s `case_insensitive` and `multiline` annotations to booleans. */
    override fun visitConstraintRegex(node: IonSchemaModel.Constraint.Regex) {
        requireBooleanType(node.caseInsensitive, "Regex.caseInsensitive")
        requireBooleanType(node.multiline, "Regex.multiline")
    }
}

private fun requireBooleanType(elem: IonElement, component: String) {
    if (elem.type != ElementType.BOOL) {
        modelValidationError(component, elem.type, listOf(ElementType.BOOL))
    }
}

/**
 * Validates the given schema.
 *
 * Constrains the types of Ion values that are allowed in the various elements of type
 * `number_range` within the `ion_schema_model` PIG domain.
 *
 * This is necessary because the `number_range` type is currently forced to allow any
 * Ion value due to a number of as-yet unimplemented features that PIG would help
 * make modeling this simpler:
 *
 * https://github.com/partiql/partiql-ir-generator/issues/43
 * https://github.com/partiql/partiql-ir-generator/issues/46
 * https://github.com/partiql/partiql-ir-generator/issues/47
 */
fun validateSchemaModel(schema: IonSchemaModel.Schema) =
    IonSchemaModelValidator.walkSchema(schema)
