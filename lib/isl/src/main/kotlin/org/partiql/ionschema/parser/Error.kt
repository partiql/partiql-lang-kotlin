package org.partiql.ionschema.parser

import com.amazon.ionelement.api.ElementType
import com.amazon.ionelement.api.IonElement

sealed class Error(private val messageFormatter: () -> String) {
    val message = messageFormatter()

    data class IonElementConstraintException(val msg: String) :
        Error({ "Parse error: $msg" })

    data class OpenContentOnTypeTemporarilyBlocked(val fieldName: String) :
        Error({ "Unrecognized type field '$fieldName' (this is temporary until the parser supports all constraints)" })

    object ImportMissingTypeFieldWhenAsSpecified :
        Error({ "The import's `type` field is required if the `as` field is specified." })

    data class ValueOfClosedFieldNotContentSymbol(val foundValue: IonElement) :
        Error({ "Expected `content`, found `$foundValue`" })

    data class UnexpectedAnnotation(val annotation: String) :
        Error({ "Unexpected annotation: '$annotation'" })

    data class UnexpectedAnnotationCount(val expectedCount: IntRange, val actualCount: Int) :
        Error({ "Expected $expectedCount annotation but $actualCount was/were found" })

    data class AnnotationNotAllowedHere(val annotation: String) :
        Error({ "Annotation '$annotation' is not allowed here" })

    data class UnexpectedListSize(val expectedCount: IntRange, val actualCount: Int) :
        Error({ "Expected $expectedCount list elements but $actualCount was/were found" })

    object EmptyListNotAllowedHere :
        Error({ "Expected nonempty list but empty list was found" })

    object InvalidNumericExtent :
        Error({ "Invalid numeric range; expected 'min', 'max' or [exclusive::]<number>" })

    object InvalidTimestampExtent :
        Error({ "Invalid timestamp range; expected 'min', 'max' or [exclusive::]<timestamp>" })

    object InvalidValidValuesRangeExtent :
        Error({ "Invalid range; expected one of the ends of range to be valid number or timestamp" })

    object InvalidRange :
        Error({ "Invalid range specification" })

    data class DuplicateField(val fieldName: String) :
        Error({ "Duplicate struct field: '$fieldName'" })

    data class RequiredFieldMissing(val fieldName: String) :
        Error({ "Required field '$fieldName' missing" })

    data class UnexpectedField(val fieldName: String) :
        Error({ "Unexpected field '$fieldName'" })

    object TypeReferenceMustBeSymbolOrStruct :
        Error({ "Type references must be a symbol or a struct" })

    object HeaderMustAppearBeforeTypes :
        Error({ "schema_header::{} must appear before any type::{}" })

    object TypeNotAllowedAfterFooter :
        Error({ "type::{} is not allowed after schema_footer::{}" })

    object MoreThanOneHeaderFound :
        Error({ "More than one schema_header::{} is not allowed" })

    object MoreThanOneFooterFound :
        Error({ "More than one schema_footer::{} is not allowed" })

    object HeaderPresentButNoFooter :
        Error({ "A schema_header::{} was included but a schema_footer::{} was not" })

    object FooterMustAppearAfterHeader :
        Error({ "A schema_footer::{} must appear after the schema_header::{}" })

    object IncorrectRegexPropertyOrder :
        Error({ "Incorrect regex property order (expected 'i::m::<regex>')" })

    data class UnexpectedType(val type: ElementType, val expectedTypes: List<ElementType>) :
        Error({ "Expected a value of type(s): [${expectedTypes.joinToString()}]; instead found a value of type $type" })

    data class InvalidOccursSpec(val found: IonElement) :
        Error({ "Expected 'optional', 'required' or int range instead of '$found'" })

    data class InvalidValidValuesSpec(val invalidSpec: IonElement) :
        Error({ "Invalid valid_values specification: '$invalidSpec'" })

    data class InvalidAnnotationsForAnnotationsConstraint(val annotation: String) :
        Error({ "Invalid annotations for 'annotations' constraint. Expected 'required', 'optional' or 'ordered' but found $annotation" })

    data class DuplicateAnnotationsNotAllowed(val annotation: String) :
        Error({ "Expected unique annotations but found duplicated annotations $annotation" })

    object CannotIncludeRequiredAndOptional :
        Error({ "Cannot include both 'required' and 'optional' annotations at the same time. Expected no annotations or only either of 'required' or 'optional'." })

    data class InvalidTimeStampPrecision(val found: String) :
        Error({ "Expected 'year', 'month', 'day', 'minute', 'second', 'millisecond', 'microsecond' or 'nanosecond' but found '$found'" })

    data class InvalidTimeStampOffsetPattern(val found: String) :
        Error({ "Pattern must be of the form '[+|-]HH:MM' but found '$found'" })

    data class InvalidTimeStampOffsetValueForHH(val found: String) :
        Error({ "'HH' offset in the offset pattern '[+|-]HH:MM' expected in the range [0,23] but found '$found'" })

    data class InvalidTimeStampOffsetValueForMM(val found: String) :
        Error({ "'MM' offset in the offset pattern '[+|-]HH:MM' expected in the range [0,59] but found '$found'" })

    data class UnexpectedNumberOfFields(val expectedCount: IntRange, val actualCount: Int) :
        Error({ "Expected $expectedCount fields but $actualCount was/were found" })

    data class InvalidFieldsForInlineImport(val found: List<String>) :
        Error({ "Expected fields to be 'id', 'type' or 'as' but found $found" })
}
