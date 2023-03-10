package org.partiql.ionschema.parser

import com.amazon.ionelement.api.IonElementLoaderOptions
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionSymbol
import com.amazon.ionelement.api.ionTimestamp
import com.amazon.ionelement.api.loadAllElements
import com.amazon.ionelement.api.loadSingleElement
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.ionschema.model.IonSchemaModel
import org.partiql.ionschema.model.toIsl
import org.partiql.ionschema.util.ArgumentsProviderBase
import kotlin.test.assertEquals

/*
open question:

- where exactly is open content allowed in ISL schemas?  The only tests I can find for this have it at the top
level of a document, which is *not* what I expected.  I am currently proceeding under the assumption that
open content is not allowed except at the top level.
answer: at the top level, header, footer and type definitions which is intended for custom constraints.

- do inline types allow a name to be specified?  should we force a name to *not* be specified? conversely,
do top-level types *require* a name?
answer:  at the moment, the parser assumes names are optional:
TODO: based on a comment from Therapon, the names should actually be *required* in the short term.

 TODO:

- constraints:
    - annotations
    - timestamp_precision
    - timestamp_offset
 - inline imports
 */

class ParseTypeTestCase(
    val input: String,
    val expectedObjectModelFactory: IonSchemaModel.Builder.() -> IonSchemaModel.IonSchemaModelNode
) {
    fun run() {
        // Load the specified schema into an IonElement, which is required by `parseTypeDefinition`
        val structElem = loadSingleElement(input, IonElementLoaderOptions(includeLocationMeta = true)).asStruct()

        // Parse the IonElement
        val typeModel = parseTypeDefinition(structElem, isInline = false)

        // Invoke the expected object model factory
        val expectedObjectModel = IonSchemaModel.build { expectedObjectModelFactory() }

        // Ensure the parsed object model matches the expected object model
        assertEquals(expectedObjectModel, typeModel, "Parsed object model must match the expected")

        val roundTripped = typeModel.toIsl(isInline = false)
        assertEquals(structElem, roundTripped, "roundTripped ion should be equivalent the original ion")
    }

    override fun toString(): String = input
}

class ConstraintTestCase(
    val constraintInput: String,
    val expectedObjectModelFactory: IonSchemaModel.Builder.() -> IonSchemaModel.Constraint
) {
    fun run() {
        // Load the specified schema into an IonElement, which is required by `parseTypeDefinition`
        val structElem = loadSingleElement(
            "type::{ $constraintInput }",
            IonElementLoaderOptions(includeLocationMeta = true)
        ).asStruct()

        // Parse the IonElement
        val typeModel = parseTypeDefinition(structElem, isInline = false)

        // Invoke the expected object model factory
        val expectedObjectModel = IonSchemaModel.build {
            typeDefinition(constraints = constraintList(expectedObjectModelFactory()))
        }

        // Ensure the parsed object model matches the expected object model
        assertEquals(expectedObjectModel, typeModel, "Parsed object model must match the expected")

        val roundTripped = typeModel.toIsl(isInline = false)
        assertEquals(structElem, roundTripped, "roundTripped ion should be equivalent the original ion")
    }

    override fun toString(): String = constraintInput
}

class ParseSchemaTestCase(
    val input: String,
    val expectedObjectModelFactory: IonSchemaModel.Builder.() -> IonSchemaModel.Schema
) {
    fun run() {
        // Load the specified schema into an IonElement, which is required by `parseTypeDefinition`
        val schemaElements = loadAllElements(input).toList()

        // Parse the IonElement
        val typeModel = parseSchema(schemaElements)

        // Invoke the expected object model factory
        val expectedObjectModel = IonSchemaModel.build { expectedObjectModelFactory() }

        // Ensure the parsed object model matches the expected object model
        assertEquals(expectedObjectModel, typeModel, "Parsed object model must match the expected")

        val roundTripped = typeModel.toIsl()
        assertEquals(schemaElements, roundTripped, "roundTripped ion should be equivalent the original ion")
    }

    override fun toString(): String = input
}

class IonSchemaParserTests {

    @ParameterizedTest
    @ArgumentsSource(RangedConstraintTests::class)
    fun rangedConstraintsTest(tc: ConstraintTestCase) = tc.run()

    class RangedConstraintTests : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // content
            ConstraintTestCase("content: closed") {
                closedContent()
            },
            // codepoint_length - exact int
            ConstraintTestCase("codepoint_length: 5") {
                codepointLength(equalsNumber(ionInt(5L)))
            },
            // codepoint_length - range
            ConstraintTestCase("codepoint_length: range::[min, 5]") {
                codepointLength(equalsRange(numberRange(min(), inclusive(ionInt(5L)))))
            },
            // precision - exact int
            ConstraintTestCase("precision: 42") {
                precision(equalsNumber(ionInt(42)))
            },
            // precision - range
            ConstraintTestCase("precision: range::[1, 42]") {
                precision(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(42)))))
            },
            // scale - exact int
            ConstraintTestCase("scale: 42") {
                scale(equalsNumber(ionInt(42)))
            },
            // scale - range
            ConstraintTestCase("scale: range::[1, 42]") {
                scale(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(42)))))
            },
            // byte_length - exact int
            ConstraintTestCase("byte_length: 42") {
                byteLength(equalsNumber(ionInt(42)))
            },
            // byte_length - range
            ConstraintTestCase("byte_length: range::[1, 42]") {
                byteLength(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(42)))))
            },
            // container_length - exact int
            ConstraintTestCase("container_length: 42") {
                containerLength(equalsNumber(ionInt(42)))
            },
            // container_length - range
            ConstraintTestCase("container_length: range::[1, 42]") {
                containerLength(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(42)))))
            },
            // occurs - range - exact int
            ConstraintTestCase("occurs: 42") {
                occurs(occursRule(equalsNumber(ionInt(42))))
            },
            // occurs - range - between
            ConstraintTestCase("occurs: range::[1, 42]") {
                occurs(occursRule(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(42))))))
            },
            // occurs - optional
            ConstraintTestCase("occurs: optional") {
                occurs(occursOptional())
            },
            // occurs - required
            ConstraintTestCase("occurs: required") {
                occurs(occursRequired())
            },
            // valid_values - specific values
            ConstraintTestCase("valid_values: [one, \"two\", 3]") {
                validValues(oneOfValidValues(ionSymbol("one").asAnyElement(), ionString("two").asAnyElement(), ionInt(3).asAnyElement()))
            },
            // valid_values - number range
            ConstraintTestCase("valid_values: range::[10, 20]") {
                validValues(rangeOfValidValues(numRange(numberRange(inclusive(ionInt(10)), inclusive(ionInt(20))))))
            },
            // valid_values - timestamp range
            ConstraintTestCase("valid_values: range::[2000-10-07T00:00:00Z, 2020-10-07T00:00:00Z]") {
                validValues(
                    rangeOfValidValues(
                        timestampRange(
                            tsValueRange(
                                inclusiveTsValue(ionTimestamp("2000-10-07T00:00:00Z")),
                                inclusiveTsValue(ionTimestamp("2020-10-07T00:00:00Z"))
                            )
                        )
                    )
                )
            },
            // regex
            ConstraintTestCase("regex: \"doesntmatter\"") {
                regex("doesntmatter", caseInsensitive = ionBool(false), multiline = ionBool(false))
            },
            // regex - case insensitive
            ConstraintTestCase("regex: i::\"doesntmatter\"") {
                regex("doesntmatter", caseInsensitive = ionBool(true), multiline = ionBool(false))
            },
            // regex - multiline
            ConstraintTestCase("regex: m::\"doesntmatter\"") {
                regex("doesntmatter", caseInsensitive = ionBool(false), multiline = ionBool(true))
            },
            // regex - case insensitive and multiline
            ConstraintTestCase("regex: i::m::\"doesntmatter\"") {
                regex("doesntmatter", caseInsensitive = ionBool(true), multiline = ionBool(true))
            },
            // not - named type reference
            ConstraintTestCase("not: int") {
                not(namedType("int", nullable = ionBool(false)))
            },
            // not - inline type reference
            ConstraintTestCase("not: { type: int }") {
                not(
                    inlineType(
                        typeDefinition(
                            constraints = constraintList(
                                typeConstraint(namedType("int", nullable = ionBool(false)))
                            )
                        ),
                        nullable = ionBool(false)
                    )
                )
            },
            // all_of - single named type reference
            ConstraintTestCase("all_of: [int]") {
                allOf(namedType("int", nullable = ionBool(false)))
            },
            // all_of - single nullable named type reference
            ConstraintTestCase("all_of: [nullable::int]") {
                allOf(namedType("int", nullable = ionBool(true)))
            },
            // all_of - multiple named type reference
            ConstraintTestCase("all_of: [foo, bar, bat]") {
                allOf(
                    namedType("foo", nullable = ionBool(false)),
                    namedType("bar", nullable = ionBool(false)),
                    namedType("bat", nullable = ionBool(false))
                )
            },
            // all_of - multiple named type reference with nullables
            ConstraintTestCase("all_of: [nullable::foo, bar, nullable::bat]") {
                allOf(
                    namedType("foo", nullable = ionBool(true)),
                    namedType("bar", nullable = ionBool(false)),
                    namedType("bat", nullable = ionBool(true))
                )
            },
            // all_of - imported type reference
            ConstraintTestCase("all_of: [{ id: \"useless_schema\", type: test_me }]") {
                allOf(importedType(id = "useless_schema", type = "test_me", nullable = ionBool(false)))
            },
            // all_of - imported type reference, nullable
            ConstraintTestCase("all_of: [nullable::{ id: \"useless_schema\", type: test_me }]") {
                allOf(importedType(id = "useless_schema", type = "test_me", nullable = ionBool(true)))
            },
            // all_of - imported type reference, nullable, with alias
            ConstraintTestCase("all_of: [nullable::{ id: \"useless_schema\", type: test_me, as: foo }]") {
                allOf(importedType(id = "useless_schema", type = "test_me", nullable = ionBool(true), alias = "foo"))
            },
            // note parsing logic for any_of, one_of & ordered_elements is re-used, which allows fewer test cases
            // any_of - single named type reference
            ConstraintTestCase("one_of: [int]") {
                oneOf(namedType("int", nullable = ionBool(false)))
            },
            // one_of - single named type reference
            ConstraintTestCase("one_of: [int]") {
                oneOf(namedType("int", nullable = ionBool(false)))
            },
            // ordered_elements - single named type reference
            ConstraintTestCase("ordered_elements: [int]") {
                orderedElements(namedType("int", nullable = ionBool(false)))
            },
            // contains - single value
            ConstraintTestCase("contains: [1]") {
                contains(listOf(ionInt(1)))
            },
            // contains - multiple values
            ConstraintTestCase("contains: [1, 2, 3]") {
                contains(listOf(ionInt(1), ionInt(2), ionInt(3)))
            },
            // annotations - single annotation - not ordered
            ConstraintTestCase("annotations: [foo]") {
                annotations(
                    isOrdered = ionBool(false).asAnyElement(),
                    annos = annotationList(listOf(annotation("foo")))
                )
            },
            // annotations - multiple annotations - not ordered
            ConstraintTestCase("annotations: [foo, bar, bat]") {
                annotations(
                    isOrdered = ionBool(false).asAnyElement(),
                    annos = annotationList(listOf(annotation("foo"), annotation("bar"), annotation("bat")))
                )
            },
            // annotations - multiple annotations - optional, not ordered
            ConstraintTestCase("annotations: optional::[foo, bar, bat]") {
                annotations(
                    isOrdered = ionBool(false).asAnyElement(),
                    annos = annotationList(listOf(annotation("foo"), annotation("bar"), annotation("bat"))),
                    defaultOptionality = optional()
                )
            },
            // annotations - multiple annotations - required, not ordered
            ConstraintTestCase("annotations: required::[foo, bar, bat]") {
                annotations(
                    isOrdered = ionBool(false).asAnyElement(),
                    annos = annotationList(listOf(annotation("foo"), annotation("bar"), annotation("bat"))),
                    defaultOptionality = required()
                )
            },
            // annotations - multiple annotations - ordered
            ConstraintTestCase("annotations: ordered::[foo, bar, bat]") {
                annotations(
                    isOrdered = ionBool(true).asAnyElement(),
                    annos = annotationList(listOf(annotation("foo"), annotation("bar"), annotation("bat")))
                )
            },
            // annotations - multiple annotations - optional, ordered
            ConstraintTestCase("annotations: optional::ordered::[foo, bar, bat]") {
                annotations(
                    isOrdered = ionBool(true).asAnyElement(),
                    annos = annotationList(listOf(annotation("foo"), annotation("bar"), annotation("bat"))),
                    defaultOptionality = optional()
                )
            },
            // annotations - multiple annotations - required, ordered
            ConstraintTestCase("annotations: required::ordered::[foo, bar, bat]") {
                annotations(
                    isOrdered = ionBool(true).asAnyElement(),
                    annos = annotationList(listOf(annotation("foo"), annotation("bar"), annotation("bat"))),
                    defaultOptionality = required()
                )
            },
            // annotations - multiple annotations - mixed
            ConstraintTestCase("annotations: [foo, required::bar, bat]") {
                annotations(
                    isOrdered = ionBool(false).asAnyElement(),
                    annos = annotationList(listOf(annotation("foo"), annotation("bar", required()), annotation("bat")))
                )
            },
            // annotations - multiple annotations - mixed
            ConstraintTestCase("annotations: required::[foo, optional::bar, bat]") {
                annotations(
                    isOrdered = ionBool(false).asAnyElement(),
                    annos = annotationList(listOf(annotation("foo"), annotation("bar", optional()), annotation("bat"))),
                    defaultOptionality = required()
                )
            },
            // annotations - multiple annotations - mixed
            ConstraintTestCase("annotations: required::ordered::[foo, bar, optional::bat]") {
                annotations(
                    isOrdered = ionBool(true).asAnyElement(),
                    annos = annotationList(listOf(annotation("foo"), annotation("bar"), annotation("bat", optional()))),
                    defaultOptionality = required()
                )
            },
            // timestamp_precision - year
            ConstraintTestCase("timestamp_precision: year") {
                timestampPrecision(equalsTsPrecisionValue(year()))
            },
            // timestamp_precision - month
            ConstraintTestCase("timestamp_precision: month") {
                timestampPrecision(equalsTsPrecisionValue(month()))
            },
            // timestamp_precision - day
            ConstraintTestCase("timestamp_precision: day") {
                timestampPrecision(equalsTsPrecisionValue(day()))
            },
            // timestamp_precision - minute
            ConstraintTestCase("timestamp_precision: minute") {
                timestampPrecision(equalsTsPrecisionValue(minute()))
            },
            // timestamp_precision - second
            ConstraintTestCase("timestamp_precision: second") {
                timestampPrecision(equalsTsPrecisionValue(second()))
            },
            // timestamp_precision - millisecond
            ConstraintTestCase("timestamp_precision: millisecond") {
                timestampPrecision(equalsTsPrecisionValue(millisecond()))
            },
            // timestamp_precision - microsecond
            ConstraintTestCase("timestamp_precision: microsecond") {
                timestampPrecision(equalsTsPrecisionValue(microsecond()))
            },
            // timestamp_precision - nanosecond
            ConstraintTestCase("timestamp_precision: nanosecond") {
                timestampPrecision(equalsTsPrecisionValue(nanosecond()))
            },
            // timestamp_precision - range with annotations
            ConstraintTestCase("timestamp_precision: range::[exclusive::year, day]") {
                timestampPrecision(equalsTsPrecisionRange(IonSchemaModel.TsPrecisionRange(min = exclusiveTsp(year()), max = inclusiveTsp(day()))))
            },
            // timestamp_precision - range min to max
            ConstraintTestCase("timestamp_precision: range::[min, max]") {
                timestampPrecision(equalsTsPrecisionRange(IonSchemaModel.TsPrecisionRange(min = minTsp(), max = maxTsp())))
            },
            // timestamp_offset - single value
            ConstraintTestCase("timestamp_offset: [\"+23:59\"]") {
                timestampOffset(listOf("+23:59"))
            },
            // timestamp_offset - two values
            ConstraintTestCase("timestamp_offset: [\"+23:59\", \"-00:00\"]") {
                timestampOffset(listOf("+23:59", "-00:00"))
            },
            // timestamp_offset - multiple values
            ConstraintTestCase("timestamp_offset: [\"+23:59\", \"-00:00\", \"-04:36\", \"+00:00\", \"-23:59\", \"+01:24\", \"+20:00\"]") {
                timestampOffset(listOf("+23:59", "-00:00", "-04:36", "+00:00", "-23:59", "+01:24", "+20:00"))
            },
            // utf8_byte_length - exact int
            ConstraintTestCase("utf8_byte_length: 42") {
                utf8ByteLength(equalsNumber(ionInt(42)))
            },
            // utf8_byte_length - range
            ConstraintTestCase("utf8_byte_length: range::[1, 42]") {
                utf8ByteLength(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(42)))))
            }
        )
    }

    @ParameterizedTest
    @ArgumentsSource(InlineTypesTests::class)
    fun inlineTypesTest(tc: ParseTypeTestCase) = tc.run()

    class InlineTypesTests : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // type constraint
            ParseTypeTestCase("type::{ type: int }") {
                typeDefinition(constraints = constraintList(typeConstraint(namedType("int", nullable = ionBool(false)))))
            },
            // type constraint - nullable
            ParseTypeTestCase("type::{ type: nullable::int }") {
                typeDefinition(constraints = constraintList(typeConstraint(namedType("int", nullable = ionBool(true)))))
            },
            // type constraint - inline
            ParseTypeTestCase("type::{ type: { name: foo } }") {
                typeDefinition(
                    constraints = constraintList(
                        typeConstraint(inlineType(typeDefinition("foo", constraintList()), nullable = ionBool(false)))
                    )
                )
            },
            // element constraint - named type
            ParseTypeTestCase("type::{ element: int }") {
                typeDefinition(constraints = constraintList(element(namedType("int", nullable = ionBool(false)))))
            },
            // element constraint - inline type
            ParseTypeTestCase("type::{ element: { name: foo } }") {
                typeDefinition(
                    constraints = constraintList(
                        element(inlineType(typeDefinition("foo", constraintList()), nullable = ionBool(false)))
                    )
                )
            }
        )
    }

    @ParameterizedTest
    @ArgumentsSource(FieldsTests::class)
    fun fieldsTest(tc: ParseTypeTestCase) = tc.run()

    class FieldsTests : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // single field - named type
            ParseTypeTestCase("type::{ fields: { foo: int } }") {
                typeDefinition(null, constraintList(fields(field("foo", namedType("int", nullable = ionBool(false))))))
            },
            // single field - named type nullable.
            ParseTypeTestCase("type::{ fields: { foo: nullable::int } }") {
                typeDefinition(null, constraintList(fields(field("foo", namedType("int", nullable = ionBool(true))))))
            },
            // single field - inline type (note that we are indicating nullability twice... which do we obey then?)
            ParseTypeTestCase("type::{ fields: { foo: { type: int } } }") {
                typeDefinition(
                    null,
                    constraintList(
                        fields(
                            field(
                                "foo",
                                inlineType(
                                    typeDefinition(null, constraintList(typeConstraint(namedType("int", nullable = ionBool(false))))),
                                    nullable = ionBool(false)
                                )
                            )
                        )
                    )
                )
            },
            // single field - inline type nullable.
            ParseTypeTestCase("type::{ fields: { foo: nullable::int } }") {
                typeDefinition(null, constraintList(fields(field("foo", namedType("int", nullable = ionBool(true))))))
            },

            // multiple fields - named types
            ParseTypeTestCase("type::{ fields: { foo: int, bar: nullable::string, bat: timestamp } }") {
                typeDefinition(
                    null,
                    constraintList(
                        fields(
                            field("foo", namedType("int", nullable = ionBool(false))),
                            field("bar", namedType("string", nullable = ionBool(true))),
                            field("bat", namedType("timestamp", nullable = ionBool(false)))
                        )
                    )
                )
            }
            // TODO: type with open content
            // TODO: inline imports
        )
    }

    @ParameterizedTest
    @ArgumentsSource(MiscTypesTests::class)
    fun miscTypesTest(tc: ParseTypeTestCase) = tc.run()

    class MiscTypesTests : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // type with a name but no constraints.
            ParseTypeTestCase("type::{ name: foo }") {
                typeDefinition("foo", constraintList())
            }
        )
    }

    @ParameterizedTest
    @ArgumentsSource(ParseIntRangeTests::class)
    fun parseIntRangeTest(tc: ParseTypeTestCase) {
        // TODO:  extract & move to ParserTestCase
        val elem = assertDoesNotThrow("loading the test input into an IonElement") {
            loadSingleElement(tc.input)
        }
        val range = parseNumberRule(elem)
        val omf = tc.expectedObjectModelFactory
        val expected = IonSchemaModel.build { omf() }

        assertEquals(expected, range)
    }

    class ParseIntRangeTests : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            ParseTypeTestCase("5") {
                equalsNumber(ionInt(5))
            },
            ParseTypeTestCase("range::[min, 6]") {
                equalsRange(numberRange(min(), inclusive(ionInt(6))))
            },
            ParseTypeTestCase("range::[min, exclusive::6]") {
                equalsRange(numberRange(min(), exclusive(ionInt(6))))
            },
            ParseTypeTestCase("range::[7, max]") {
                equalsRange(numberRange(inclusive(ionInt(7)), max()))
            },
            ParseTypeTestCase("range::[exclusive::7, max]") {
                equalsRange(numberRange(exclusive(ionInt(7)), max()))
            }
        )
    }

    @ParameterizedTest
    @ArgumentsSource(WholeSchemaTests::class)
    fun WholeSchemaTests(tc: ParseSchemaTestCase) = tc.run()

    class WholeSchemaTests : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // single type
            ParseSchemaTestCase("type::{ name: foo }") {
                schema(typeStatement(typeDefinition("foo", constraintList())))
            },
            // multiple types
            ParseSchemaTestCase("type::{ name: foo } type::{ name: bar }") {
                schema(
                    typeStatement(typeDefinition("foo", constraintList())),
                    typeStatement(typeDefinition("bar", constraintList()))
                )
            },
            // with open content only
            ParseSchemaTestCase("a b c 1 2 3") {
                schema(
                    contentStatement(ionSymbol("a")),
                    contentStatement(ionSymbol("b")),
                    contentStatement(ionSymbol("c")),
                    contentStatement(ionInt(1)),
                    contentStatement(ionInt(2)),
                    contentStatement(ionInt(3))
                )
            },
            // mixed open content and types
            ParseSchemaTestCase("a type::{ name: foo } b type::{ name: bar } c") {
                schema(
                    contentStatement(ionSymbol("a")),
                    typeStatement(typeDefinition("foo", constraintList())),
                    contentStatement(ionSymbol("b")),
                    typeStatement(typeDefinition("bar", constraintList())),
                    contentStatement(ionSymbol("c"))
                )
            },
            // with header/footer and no fields
            ParseSchemaTestCase("schema_header::{  } schema_footer::{ }") {
                schema(
                    headerStatement(openFieldList()),
                    footerStatement(openFieldList())
                )
            },
            // with header/footer with open content.
            ParseSchemaTestCase("schema_header::{ blee: foo } schema_footer::{ blar: bat }") {
                schema(
                    headerStatement(openFieldList(openField("blee", ionSymbol("foo")))),
                    footerStatement(openFieldList(openField("blar", ionSymbol("bat"))))
                )
            },
            // with header with empty imports
            ParseSchemaTestCase("schema_header::{ imports: [] } schema_footer::{ }") {
                schema(
                    headerStatement(openFieldList(), importList()),
                    footerStatement(openFieldList())
                )
            },
            // with header and footer with imports and single import (id only)
            ParseSchemaTestCase("schema_header::{ imports: [{id: foo}] } schema_footer::{ }") {
                schema(
                    headerStatement(openFieldList(), importList(import("foo"))),
                    footerStatement(openFieldList())
                )
            },
            // with header and footer with imports and single import (id and type)
            ParseSchemaTestCase("schema_header::{ imports: [{id: foo, type: bar}] } schema_footer::{ }") {
                schema(
                    headerStatement(openFieldList(), importList(import("foo", "bar"))),
                    footerStatement(openFieldList())
                )
            },
            // with header and footer with imports and single import (id, type and as alias)
            ParseSchemaTestCase("schema_header::{ imports: [{id: foo, type: bar, as: bat}] } schema_footer::{ }") {
                schema(
                    headerStatement(openFieldList(), importList(import("foo", "bar", "bat"))),
                    footerStatement(openFieldList())
                )
            },
            // with header and footer multiple imports
            ParseSchemaTestCase("schema_header::{ imports: [{id: foo}, {id: bar}, {id: bat}] } schema_footer::{ }") {
                schema(
                    headerStatement(openFieldList(), importList(import("foo"), import("bar"), import("bat"))),
                    footerStatement(openFieldList())
                )
            }
        )
    }
}
