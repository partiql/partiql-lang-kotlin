package org.partiql.ionschema.parser

import com.amazon.ion.Decimal
import com.amazon.ionelement.api.IonElementLoaderOptions
import com.amazon.ionelement.api.IonTextLocation
import com.amazon.ionelement.api.ionDecimal
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionSymbol
import com.amazon.ionelement.api.loadAllElements
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.ionschema.util.ArgumentsProviderBase
import kotlin.test.assertEquals

class ErrorTestCase(
    private val isl: String,
    private val expectedLine: Int,
    private val expectedCol: Int,
    private val expectedError: Error
) {
    override fun toString(): String = isl
    fun run() {
        val elements = assertDoesNotThrow("example schema must be valid Ion") {
            loadAllElements(isl, IonElementLoaderOptions(includeLocationMeta = true)).toList()
        }

        val ex = assertThrows<IonSchemaParseException>("parsing the isl should throw") {
            parseSchema(elements)
        }

        assertEquals(expectedError, ex.error)
        val location = ex.location as IonTextLocation
        assertEquals(IonTextLocation(expectedLine.toLong(), expectedCol.toLong()), location)
    }
}

class IonSchemaParserErrorsTest {
    // The following are column guides which help to quickly identify the column of a particular character in the
    // string on the following line:
    //        1         2         3         4         5
    // 2345778901234567890123456789012345678901234567890123456789

    @ParameterizedTest
    @ArgumentsSource(ErrorsTests::class)
    fun errorsTest(tc: ErrorTestCase) = tc.run()

    class ErrorsTests : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(

            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "schema_header::{ imports: [{ }] }", 1, 28,
                Error.RequiredFieldMissing("id")
            ),

            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "schema_header::{ imports: [{ id: foo, as: bar }] }", 1, 28,
                Error.ImportMissingTypeFieldWhenAsSpecified
            ),
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ content: unexpected_symbol }", 1, 9,
                Error.ValueOfClosedFieldNotContentSymbol(ionSymbol("unexpected_symbol"))
            ),
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::extra::annotations::{ content: unexpected_symbol }", 1, 1,
                Error.UnexpectedAnnotationCount(1..1, 3)
            ),
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ type: invalid_anno::int }", 1, 9,
                Error.AnnotationNotAllowedHere("invalid_anno")
            ),
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ type: 1.0 }", 1, 9,
                Error.TypeReferenceMustBeSymbolOrStruct
            ),
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ codepoint_length: illegal::1 }", 1, 9,
                Error.UnexpectedAnnotationCount(0..0, 1)
            ),
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ codepoint_length: bad::[1, 2] }", 1, 9,
                Error.UnexpectedAnnotation("bad")
            ),
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ codepoint_length: range::[1, 2, 3] }", 1, 9,
                Error.UnexpectedListSize(2..2, 3)
            ),
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ codepoint_length: range::[bad, 1] }", 1, 35,
                Error.InvalidNumericExtent
            ),
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ codepoint_length: range::[1, bad] }", 1, 36,
                Error.InvalidNumericExtent
            ),
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ codepoint_length: range::[bad, bad] }", 1, 35,
                Error.InvalidNumericExtent
            ),
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ codepoint_length: range::[illegal::1, 2] }", 1, 35,
                Error.AnnotationNotAllowedHere("illegal")
            ),
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ codepoint_length: range::[1, illegal::2] }", 1, 36,
                // TODO:  column 36 is the `,`.  why isn't it 38?  I believe ion-java is reporting the column wrong.
                Error.AnnotationNotAllowedHere("illegal")
            ),

            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ occurs: invalid_symbol }", 1, 9,
                Error.InvalidOccursSpec(ionSymbol("invalid_symbol"))
            ),
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ occurs: \"invalid data type\" }", 1, 9,
                Error.InvalidOccursSpec(ionString("invalid data type"))
            ),

            // trailing type::{} after footer
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "schema_header::{} schema_footer::{} type::{ type: int }", 1, 37,
                Error.TypeNotAllowedAfterFooter
            ),
            // two schema_header::{}
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "schema_header::{} schema_header::{}", 1, 19,
                Error.MoreThanOneHeaderFound
            ),
            // two schema_footer::{}
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "schema_header::{} schema_footer::{} schema_footer::{}", 1, 37,
                Error.MoreThanOneFooterFound
            ),
            // schema_header without schema_footer
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "schema_header::{}", 1, 1,
                Error.HeaderPresentButNoFooter
            ),
            // schema_footer appears before header (variant 1)
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "schema_footer::{}", 1, 1,
                Error.FooterMustAppearAfterHeader
            ),
            // schema_footer appears before header (variant 2)
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ type: int } schema_footer::{}", 1, 21,
                Error.FooterMustAppearAfterHeader
            ),
            // valid_values is numeric or timestamp data type
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ valid_values: 0. }", 1, 9,
                Error.InvalidValidValuesSpec(ionDecimal(Decimal.ZERO))
            ),
            // valid_values is numeric or timestamp data type
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ valid_values: range::[2, foo] }", 1, 32,
                // TODO:  column 32 is the `,`.  why isn't it 34?  I believe ion-java is reporting the column wrong.
                Error.InvalidNumericExtent
            ),
            // valid_values is numeric or timestamp data type
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ valid_values: range::[min, foo] }", 1, 9,
                Error.InvalidValidValuesRangeExtent
            ),
            // valid_values is numeric or timestamp data type
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ valid_values: range::[foo, 2020-10-07T00:00:01Z] }", 1, 31,
                Error.InvalidTimestampExtent
            ),
            // regex - too many annotations
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ regex: i::m::one_too_many::\"foo\" }", 1, 9,
                Error.UnexpectedAnnotationCount(0..2, 3)
            ),
            // regex - unexpected annotation
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ regex: not_a_regex_property::\"foo\" }", 1, 9,
                Error.UnexpectedAnnotation("not_a_regex_property")
            ),
            // regex - i and m reversed
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ regex: m::i::\"foo\" }", 1, 9,
                Error.IncorrectRegexPropertyOrder
            ),
            // not - invalid type reference
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ not: 123 }", 1, 9,
                Error.TypeReferenceMustBeSymbolOrStruct
            ),
            // all_of - invalid type reference
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ all_of: [123] }", 1, 18,
                Error.TypeReferenceMustBeSymbolOrStruct
            ),
            // one_of - invalid type reference
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ one_of: [123] }", 1, 18,
                Error.TypeReferenceMustBeSymbolOrStruct
            ),
            // any_of - invalid type reference
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ any_of: [123] }", 1, 18,
                Error.TypeReferenceMustBeSymbolOrStruct
            ),
            // ordered_elements - invalid type reference
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ ordered_elements: [123] }", 1, 28,
                Error.TypeReferenceMustBeSymbolOrStruct
            ),
            // contains - not a list
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ contains: oops_not_a_list }", 1, 9,
                Error.IonElementConstraintException(
                    "1:9: Expected an element of type LIST but found an element of type SYMBOL"
                )
            ),
            // annotations - Annotation count not in 0..2
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ annotations: required::ordered::third_one::[a,b,c,d,e] }", 1, 9,
                Error.UnexpectedAnnotationCount(
                    0..2, 3
                )
            ),
            // annotations - invalid annotation
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ annotations: required::invalid_anno::[a,b,c,d,e] }", 1, 9,
                Error.InvalidAnnotationsForAnnotationsConstraint("invalid_anno")
            ),
            // annotations - invalid annotations
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ annotations: invalid_anno_1::invalid_anno_2::[a,b,c,d,e] }", 1, 9,
                Error.InvalidAnnotationsForAnnotationsConstraint("invalid_anno_1")
            ),
            // annotations - invalid annotation for mixed type
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ annotations: required::ordered::[invalid_anno::a,b,required::c,d,e] }", 1, 42,
                Error.AnnotationNotAllowedHere("invalid_anno")
            ),
            // annotations - duplicate annotations
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ annotations: required::required::[a,b,c,d,e] }", 1, 9,
                Error.DuplicateAnnotationsNotAllowed("required")
            ),
            // annotations - duplicate annotations
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ annotations: ordered::ordered::[a,b,c,d,e] }", 1, 9,
                Error.DuplicateAnnotationsNotAllowed("ordered")
            ),
            // annotations - cannot contain required and optional annotations at the same time
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ annotations: required::optional::[a,b,c,d,e] }", 1, 9,
                Error.CannotIncludeRequiredAndOptional
            ),
            // timestamp_precision - not a valid timestamp precision symbol
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ timestamp_precision: week }", 1, 9,
                Error.InvalidTimeStampPrecision(
                    "week"
                )
            ),
            // timestamp_precision - not a valid timestamp precision range size
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ timestamp_precision: range::[second,day,year] }", 1, 9,
                Error.UnexpectedListSize(
                    2..2, 3
                )
            ),
            // timestamp_precision - not a valid timestamp precision annotation
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ timestamp_precision: invalid_anno::[year, second] }", 1, 9,
                Error.AnnotationNotAllowedHere(
                    "invalid_anno"
                )
            ),
            // timestamp_precision - not a valid timestamp precision annotation
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ timestamp_precision: range::[exclusive::year, invalid_anno::second] }", 1, 53,
                Error.AnnotationNotAllowedHere(
                    "invalid_anno"
                )
            ),
            // timestamp_offset - invalid pattern [+-] missing
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ timestamp_offset: [\"23:59\"] }", 1, 28,
                Error.InvalidTimeStampOffsetPattern(
                    "23:59"
                )
            ),
            // timestamp_offset - invalid pattern non-numeric character found
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ timestamp_offset: [\"+23:S9\"] }", 1, 28,
                Error.InvalidTimeStampOffsetPattern(
                    "+23:S9"
                )
            ),
            // timestamp_offset - invalid pattern non-numeric character found
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ timestamp_offset: [\"+Z3:59\"] }", 1, 28,
                Error.InvalidTimeStampOffsetPattern(
                    "+Z3:59"
                )
            ),
            // timestamp_offset - invalid HH range
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ timestamp_offset: [\"+24:45\"] }", 1, 28,
                Error.InvalidTimeStampOffsetValueForHH(
                    "+24:45"
                )
            ),
            // timestamp_offset - invalid MM range
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ timestamp_offset: [\"+23:60\"] }", 1, 28,
                Error.InvalidTimeStampOffsetValueForMM(
                    "+23:60"
                )
            ),
            // timestamp_offset - empty list not allowed
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ timestamp_offset: [] }", 1, 9,
                Error.EmptyListNotAllowedHere
            ),
            // inline import or imported type TypeReference - unexpected number of fields
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ type: { id: super_fictitious_schema } }", 1, 9,
                Error.UnexpectedNumberOfFields(2..3, 1)
            ),
            // inline import or imported type TypeReference - "as" field missing
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ type: { id: super_fictitious_schema, type: foo, yet_another_field: bar } }", 1, 9,
                Error.InvalidFieldsForInlineImport(listOf("yet_another_field"))
            ),
            // inline import or imported type TypeReference - invalid fields
            ErrorTestCase(
                //        1         2         3         4         5
                // 2345778901234567890123456789012345678901234567890123456789
                "type::{ type: { id: super_fictitious_schema, not_type: foo, yet_another_field: bar } }", 1, 9,
                Error.InvalidFieldsForInlineImport(listOf("not_type", "yet_another_field"))
            )
        )
    }
}
