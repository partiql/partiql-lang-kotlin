package org.partiql.lang.schemadiscovery

import com.amazon.ion.system.IonReaderBuilder
import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionelement.api.loadAllElements
import com.amazon.ionschema.IonSchemaSystemBuilder
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.ionschema.model.toIsl
import org.partiql.ionschema.parser.parseSchema
import org.partiql.lang.TestBase
import org.partiql.lang.partiqlisl.ResourceAuthority
import org.partiql.lang.util.ArgumentsProviderBase
import java.lang.AssertionError
import java.math.BigInteger

private const val typeName = "SchemaInferencerFromExample"
private const val schemaId = "partiql.isl"
private val islHeader = """
    schema_header::{
        imports: [
            { id: "$schemaId" },
        ],
    }
""".trimIndent()

private val islFooter = """
    schema_footer::{ }
""".trimIndent()

private val ion = IonSystemBuilder.standard().build()
private val resourceAuthority = ResourceAuthority("org/partiql/schemas", ClassLoader.getSystemClassLoader(), ion)
private val iss = IonSchemaSystemBuilder.standard().addAuthority(resourceAuthority).build()

private val inferencer: SchemaInferencerFromExample = SchemaInferencerFromExampleImpl(typeName, iss, listOf(schemaId))

private val NEG_BIG_INT = BigInteger.valueOf(MIN_INT8).minus(BigInteger.ONE)
private val POS_BIG_INT = BigInteger.valueOf(MAX_INT8).plus(BigInteger.ONE)

private const val INT2_VALID_VALUES = "valid_values: range::[$MIN_INT2, $MAX_INT2]"
private const val INT4_VALID_VALUES = "valid_values: range::[$MIN_INT4, $MAX_INT4]"
private const val INT8_VALID_VALUES = "valid_values: range::[$MIN_INT8, $MAX_INT8]"

data class ExampleInferenceTestCase(
    val examples: String,
    val islAsString: String,
    val maxExampleCount: Int = Int.MAX_VALUE
) {

    override fun toString(): String {
        return examples.trimIndent() + " -> " + islAsString.trimIndent()
    }
}

data class InferenceAndDefiniteUnifyTestCase(
    val name: String,
    val examples: String,
    val definiteIslAsString: String,
    val islAsString: String,
    val maxExampleCount: Int = Int.MAX_VALUE
) {

    override fun toString(): String = name
}

/**
 * Checks that [examples]' inferred schema is the same as [islAsString] given [maxExampleCount] examples.
 * If a non-null [definiteIslAsString] is provided, the inferred schema will also be unified with the definite schema.
 */
private fun assertCorrectISL(
    examples: String,
    islAsString: String,
    definiteIslAsString: String? = null,
    maxExampleCount: Int = Int.MAX_VALUE
) {
    val reader = IonReaderBuilder.standard().build(examples)
    val expectedIonSchemaModel = parseSchema(loadAllElements(islHeader + islAsString + islFooter).toList())
    val definiteIsl = definiteIslAsString?.let {
        parseSchema(loadAllElements(islHeader + definiteIslAsString + islFooter).toList())
    }
    val generatedIonSchemaModel = inferencer.inferFromExamples(reader, maxExampleCount, definiteIsl)

    val expectedISL = expectedIonSchemaModel.toIsl()
    val generatedISL = generatedIonSchemaModel.toIsl()

    if (expectedISL != generatedISL) {
        throw AssertionError(
            """
            Expected ISL and discovered ISL differ,
            Expected: $expectedISL
            Actual:   $generatedISL
            """.trimIndent()
        )
    }
}

class SchemaInferencerFromExampleTests : TestBase() {

    class SchemaDiscoveryTests : ArgumentsProviderBase() {
        private val structOneTwoIon = """{ one: 1, two: 2 }"""
        private val structOneTwoThreeIon = """{ one: 1, two: 2, three: 3 }"""
        private val structOneTwoFloatIon = """{ one: 1, two: 2e0 }"""
        private val examplesOneTwoThreeIon = """{ one: 1 }{ two: 2 }{ three: 3 }"""
        private val timeIon = """
                ${'$'}partiql_time::{
                    hour: 23,
                    min: 59,
                    sec: 59,
                    sec_fraction: 9999
                }
                """
        override fun getParameters(): List<Any> = listOf(
            // empty sequence
            ExampleInferenceTestCase("", "type::{ name: $typeName }"),
            // boolean
            ExampleInferenceTestCase("false", "type::{ name: $typeName, type: bool }"),
            // int
            ExampleInferenceTestCase("1", "type::{ name: $typeName, type: int, $INT2_VALID_VALUES }"),
            // float
            ExampleInferenceTestCase("3e0", "type::{ name: $typeName, type: float }"),
            // decimal
            ExampleInferenceTestCase("3d0", "type::{ name: $typeName, type: decimal, scale: 0, precision: 1 }"),
            // timestamp
            ExampleInferenceTestCase("2000-01-01T00:00:00Z", "type::{ name: $typeName, type: timestamp }"),
            // symbol
            ExampleInferenceTestCase("foo", "type::{ name: $typeName, type: symbol }"),
            // string
            ExampleInferenceTestCase("\"foo\"", "type::{ name: $typeName, type: string, codepoint_length: 3 }"),
            // clob
            ExampleInferenceTestCase("{{ \"+AB/\" }}", "type::{ name: $typeName, type: clob }"),
            // blob
            ExampleInferenceTestCase("{{ +AB/ }}", "type::{ name: $typeName, type: blob }"),
            // empty bag
            ExampleInferenceTestCase("\$partiql_bag::[]", "type::{ name: $typeName, type: bag }"),
            // empty list
            ExampleInferenceTestCase("[]", "type::{ name: $typeName, type: list }"),
            // empty sexp
            ExampleInferenceTestCase("()", "type::{ name: $typeName, type: sexp }"),
            // empty struct
            ExampleInferenceTestCase("{ }", "type::{ name: $typeName, type: struct, content: closed }"),
            // missing
            ExampleInferenceTestCase("\$partiql_missing::null", "type::{ name: $typeName, type: missing }"),
            // null and missing
            ExampleInferenceTestCase(
                "{ val: \$partiql_missing::null }" + "{ val: null }",
                """
                type::{name: $typeName, type: struct,
                    content: closed,
                    fields: {
                        val: { type: nullable::missing }
                    }
                }
                """
            ),
            // non-empty list of int
            ExampleInferenceTestCase(
                "[1]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: int, $INT2_VALID_VALUES }
                }
                """
            ),
            // non-empty list of int and float
            ExampleInferenceTestCase(
                "[1, 1e0]",
                """
                type::{ name: $typeName, type: list,
                    element: {
                        any_of: [
                            { type: int, $INT2_VALID_VALUES },
                            float
                        ]
                    }
                }
                """
            ),
            // non-empty list of decimal and float
            ExampleInferenceTestCase(
                "[1d0, 1e0]",
                """
                type::{ name: $typeName, type: list,
                    element: {
                        any_of: [ 
                            { type: decimal, scale: 0, precision: 1 },
                            float
                        ]
                    }
                }
                """
            ),
            // non-empty list of int and decimal
            ExampleInferenceTestCase(
                "[1, 1d0]",
                """
                type::{ name: $typeName, type: list,
                    element: {
                        any_of: [ 
                            { type: int, $INT2_VALID_VALUES },
                            { type: decimal, scale: 0, precision: 1 }
                        ]
                    }
                }
                """
            ),
            // non-empty list of int, decimal, and float
            ExampleInferenceTestCase(
                "[1, 1d0, 1e0]",
                """
                type::{ name: $typeName, type: list,
                    element: {
                        any_of: [ 
                            { type: int, $INT2_VALID_VALUES },
                            { type: decimal, scale: 0, precision: 1 },
                            float
                        ]
                    }
                }
                """
            ),
            // non-empty list of int and string
            ExampleInferenceTestCase(
                "[1, \"1\"]",
                """
                type::{ name: $typeName, type: list,
                    element: {
                        any_of: [ 
                            { type: int, $INT2_VALID_VALUES },
                            { type: string, codepoint_length: 1 }
                        ]
                    }
                }
                """
            ),
            // non-empty list of string and symbol
            ExampleInferenceTestCase(
                "[foo, \"foo\"]",
                """
                type::{ name: $typeName, type: list,
                    element: {
                        any_of: [ 
                            symbol,
                            { type: string, codepoint_length: 3 }
                        ]
                    }
                }
                """
            ),
            // struct of one elem
            ExampleInferenceTestCase(
                "{ one: 1 }",
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: { 
                        one: { type: int, $INT2_VALID_VALUES }
                    }
                }
                """
            ),
            // struct of two elems
            ExampleInferenceTestCase(
                """{ id: "foo", one: 1 }""",
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: {
                        id: { type: string, codepoint_length: 3 },
                        one: { type: int, $INT2_VALID_VALUES }
                    }
                }
                """
            ),
            // list of list
            ExampleInferenceTestCase(
                "[[1]]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: list,
                        element: { type: int, $INT2_VALID_VALUES }
                    }
                }
                """
            ),
            // list of list of list
            ExampleInferenceTestCase(
                "[[[1]]]",
                """type::{ name: $typeName, type: list,
                    element: { type: list,
                        element: { type: list,
                            element: { type: int, $INT2_VALID_VALUES }
                        }
                    }
                }
                """
            ),
            // bag of bag
            ExampleInferenceTestCase(
                "\$partiql_bag::[\$partiql_bag::[1]]",
                """
                type::{name: $typeName, type: bag,
                    element: { type: bag,
                        element: { type: int, $INT2_VALID_VALUES }
                    }
                }
                """
            ),
            // bag of different types
            ExampleInferenceTestCase(
                "\$partiql_bag::[3, 3e0]",
                """
                type::{name: $typeName, type: bag,
                    element: {
                        any_of: [
                            { type: int, $INT2_VALID_VALUES },
                            float
                        ]
                    }
                }
                """
            ),
            // sexp of sexp
            ExampleInferenceTestCase(
                "( ( 1 ) )",
                """
                type::{ name: $typeName, type: sexp,
                    element: { type: sexp,
                        element: { type: int, $INT2_VALID_VALUES }
                    }
                }
                """
            ),
            // list of list of int
            ExampleInferenceTestCase(
                "[ [3 , 4] ]",
                """
                type:: { name: $typeName, type: list,
                    element: { type: list,
                        element: { type: int, $INT2_VALID_VALUES }
                    }
                }
                """
            ),
            // list of int list and int
            ExampleInferenceTestCase(
                "[ [3], 4 ]",
                """
                type:: { name: $typeName, type: list,
                    element: {
                        any_of: [
                            { type: list, element: { type: int, $INT2_VALID_VALUES } },
                            { type: int, $INT2_VALID_VALUES }
                        ]
                    }
                }
                """
            ),
            // list of int list and empty list
            ExampleInferenceTestCase(
                "[ [3], [ ] ]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: list,
                        element: { type: int, $INT2_VALID_VALUES }
                    }
                }
                """
            ),
            // list of empty list and int list
            ExampleInferenceTestCase(
                "[ [ ], [3] ]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: list,
                        element: { type: int, $INT2_VALID_VALUES }
                    }
                }
                """
            ),
            // list of empty lists
            ExampleInferenceTestCase(
                "[ [ ], [ ] ]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: list }
                }
                """
            ),
            // list of empty lists and int list
            ExampleInferenceTestCase(
                "[ [ ], [ ], [ ], [3] ]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: list,
                        element: { type: int, $INT2_VALID_VALUES }
                    }
                }
                """
            ),
            // list of empty lists and conflicting lists
            ExampleInferenceTestCase(
                "[ [ ], [ ], [ ], [3], [3e0] ]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: list,
                        element: {
                            any_of: [
                                { type: int, $INT2_VALID_VALUES },
                                float
                            ]
                        }
                    }
                }
                """
            ),
            // list of list of two different types
            ExampleInferenceTestCase(
                "[ [3, 4.0] ]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: list,
                        element: {
                            any_of: [
                                { type: int, $INT2_VALID_VALUES },
                                { type: decimal, scale: 1, precision: 2 }
                            ]
                        }
                    }
                }
                """
            ),
            // list of lists with a different type
            ExampleInferenceTestCase(
                "[ [3], [4.0] ]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: list,
                        element: {
                            any_of: [
                                { type: int, $INT2_VALID_VALUES },
                                { type: decimal, scale: 1, precision: 2 }
                            ]
                        }
                    }
                }
                """
            ),
            // list of lists with multiple different types
            ExampleInferenceTestCase(
                "[ [1], [2, abc] ]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: list,
                        element: {
                            any_of: [
                                { type: int, $INT2_VALID_VALUES },
                                symbol
                            ]
                        }
                    }
                }
                """
            ),
            // list of lists with different types and different INT valid_values
            ExampleInferenceTestCase(
                "[ [1], [$MAX_INT8, abc] ]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: list,
                        element: {
                            any_of: [
                                { type: int, $INT8_VALID_VALUES },
                                symbol
                            ]
                        }
                    }
                }
                """
            ),
            // list of lists with multiple different types
            ExampleInferenceTestCase(
                "[ [1, \"abc\"], [1.0, abc] ]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: list,
                        element: {
                            any_of: [
                                { type: int, $INT2_VALID_VALUES },
                                { type: string, codepoint_length: 3 },
                                { type: decimal, scale: 1, precision: 2 },
                                symbol
                            ]
                        }
                    }
                }
                """
            ),
            // list of lists with empty and int lists
            ExampleInferenceTestCase(
                "[ [1, []], [1, [1]] ]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: list,
                        element: {
                            any_of: [
                                { type: int, $INT2_VALID_VALUES },
                                { type: list, element: { type: int, $INT2_VALID_VALUES } }
                            ]
                        }
                    }
                }
                """
            ),
            // list of sexp and bag of int
            ExampleInferenceTestCase(
                "[ ( 1 ), \$partiql_bag::[ 1 ] ] ",
                """
                type::{ name: $typeName, type: list,
                    element: {
                        any_of: [
                            { type: sexp, element: { type: int, $INT2_VALID_VALUES } },
                            { type: bag, element: { type: int, $INT2_VALID_VALUES } }
                        ]
                    }
                }
                """
            ),
            // sexp and bag
            ExampleInferenceTestCase(
                "{ one: ( 1 ) }" + "{ one: \$partiql_bag::[ 1 ] }",
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: {
                        one: {
                            any_of: [
                                { type: sexp, element: { type: int, $INT2_VALID_VALUES } },
                                { type: bag, element: { type: int, $INT2_VALID_VALUES } }
                            ]
                        }
                    }
                }
                """
            ),
            // list and bag
            ExampleInferenceTestCase(
                "{ one: [ 1 ] }" + "{ one: \$partiql_bag::[ 1 ] }",
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: {
                        one: {
                            any_of: [
                                { type: list, element: { type: int, $INT2_VALID_VALUES } },
                                { type: bag, element: { type: int, $INT2_VALID_VALUES } }
                            ]
                        }
                    }
                }
                """
            ),
            // list of struct
            ExampleInferenceTestCase(
                "[ $structOneTwoIon ] ",
                """
                type::{ name: $typeName, type: list,
                    element: { type: struct,
                        content: closed,
                        fields: {
                            one: { type: int, $INT2_VALID_VALUES },
                            two: { type: int, $INT2_VALID_VALUES }
                        }
                    }
                }
                """
            ),
            // struct and list
            ExampleInferenceTestCase(
                structOneTwoIon + "[]",
                """
                type:: { name: $typeName,
                    any_of: [
                        { type: struct, content: closed, fields: { one: { type: int, $INT2_VALID_VALUES }, two: { type: int, $INT2_VALID_VALUES } } },
                        list
                    ]
                }
                """
            ),
            // struct and empty struct
            ExampleInferenceTestCase(
                structOneTwoIon + "{}",
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: {
                        one: { type: int, $INT2_VALID_VALUES },
                        two: { type: int, $INT2_VALID_VALUES } 
                    }
                }
                """
            ),
            // empty struct and struct
            ExampleInferenceTestCase(
                "{}" + structOneTwoIon,
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: {
                        one: { type: int, $INT2_VALID_VALUES },
                        two: { type: int, $INT2_VALID_VALUES } 
                    }
                }
                """
            ),
            // empty structs and struct
            ExampleInferenceTestCase(
                "{}" + "{}" + structOneTwoIon,
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: {
                        one: { type: int, $INT2_VALID_VALUES },
                        two: { type: int, $INT2_VALID_VALUES } 
                    }
                }
                """
            ),
            // list of conflicting struct
            ExampleInferenceTestCase(
                "[ $structOneTwoIon, $structOneTwoFloatIon ]",
                """
                type::{ name: $typeName, type: list,
                    element: {
                        type: struct,
                        content: closed,
                        fields: {
                            one: { type: int, $INT2_VALID_VALUES },
                            two: {
                                any_of: [
                                    { type: int, $INT2_VALID_VALUES },
                                    float
                                ]
                            }
                        }
                    }
                }
                """
            ),
            // list of conflicting struct - additional element
            ExampleInferenceTestCase(
                "[ $structOneTwoIon, $structOneTwoThreeIon ]",
                """
                type::{ name: $typeName, type: list, 
                    element: {
                        type: struct,
                        content: closed,
                        fields: {
                            one: { type: int, $INT2_VALID_VALUES },
                            two: { type: int, $INT2_VALID_VALUES },
                            three: { type: int, $INT2_VALID_VALUES }
                        }
                    }
                }"""
            ),
            // struct of struct
            ExampleInferenceTestCase(
                "{ onetwo: $structOneTwoIon }",
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: {
                        onetwo: { type: struct,
                            content: closed,
                            fields: { 
                                one: { type: int, $INT2_VALID_VALUES },
                                two: { type: int, $INT2_VALID_VALUES }
                            }
                        }
                    }
                }
                """
            ),
            // struct of two elems
            ExampleInferenceTestCase(
                structOneTwoIon,
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: {
                        one: { type: int, $INT2_VALID_VALUES },
                        two: { type: int, $INT2_VALID_VALUES }
                    }
                }
                """
            ),
            // struct of same schema
            ExampleInferenceTestCase(
                structOneTwoIon + structOneTwoIon,
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: {
                        one: { type: int, $INT2_VALID_VALUES },
                        two: { type: int, $INT2_VALID_VALUES }
                    }
                }"""
            ),
            // struct of schema with additional element
            ExampleInferenceTestCase(
                structOneTwoIon + structOneTwoThreeIon,
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: {
                        one: { type: int, $INT2_VALID_VALUES },
                        two: { type: int, $INT2_VALID_VALUES },
                        three: { type: int, $INT2_VALID_VALUES }
                    }
                }
                """
            ),
            // struct of schema with conflicting type
            ExampleInferenceTestCase(
                structOneTwoIon + structOneTwoFloatIon,
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: {
                        one: { type: int, $INT2_VALID_VALUES },
                        two: {
                            any_of: [
                                { type: int, $INT2_VALID_VALUES },
                                float
                            ]
                        } 
                    } 
                }
                """
            ),
            // list of lists with empty and non-empty struct and other elements
            ExampleInferenceTestCase(
                "[ [ 1, abc, { } ], [ 1 , abc, { one: 1 } ] ]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: list,
                        element: {
                            any_of: [
                                { type: int, $INT2_VALID_VALUES },
                                symbol,
                                { type: struct, content: closed, fields: { one: { type: int, $INT2_VALID_VALUES } } }
                            ]
                        }
                    }
                }
                """
            ),
            // nested unions with conflicting discovered constraints in lists
            ExampleInferenceTestCase(
                "[ [ 1, abc, [ 1e0 ] ], [ 1 , abc, [ 1 ] ] ]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: list,
                        element: {
                            any_of: [
                                { type: int, $INT2_VALID_VALUES },
                                symbol,
                                { type: list, element: { 
                                    any_of: [
                                        float,
                                        { type: int, $INT2_VALID_VALUES }
                                    ]
                                }}
                            ]
                        }
                    }
                }
                """
            ),
            // nested unions with conflicting discovered constraints in struct
            ExampleInferenceTestCase(
                "[ [ 1, abc, { one: 1e0 } ], [ 1 , abc, { one: 1 } ] ]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: list,
                        element: {
                            any_of: [
                                { type: int, $INT2_VALID_VALUES },
                                symbol,
                                {
                                    type: struct, 
                                    content: closed,
                                    fields: {
                                        one: {
                                            any_of: [
                                                float,
                                                { type: int, $INT2_VALID_VALUES }
                                            ]
                                        }
                                    }
                                }
                            ]
                        }
                    }
                }
                """
            ),
            // struct of multiple fields
            ExampleInferenceTestCase(
                """
                {
                    id: "book",
                    price: 6.,
                    categories: ["media", "books"]
                }
                {
                    id: "car",
                    price: 61234.22,
                    categories: ["automotive"]
                }
                """,
                """
                type::{name:$typeName, type: struct,
                    content: closed,
                    fields: { 
                        id: { type: string, codepoint_length: range::[3, 4] },
                        price: { type: decimal, scale: range::[0, 2], precision: range::[1, 7] },
                        categories: { 
                            type: list, 
                            element: { type: string, codepoint_length: range::[5, 10] }
                        }
                    }
                }
                """
            ),
            // struct of multiple fields mismatch case
            ExampleInferenceTestCase(
                """
                {
                    id: "book",
                    price: 6.,
                    categories: ["media", "books"]
                }
                {
                    id: "car",
                    price: 61234.22,
                    categories: [12346]
                }
                """,
                """
                type::{name:$typeName, type: struct,
                    content: closed,
                    fields: { 
                        id: { type: string, codepoint_length: range::[3, 4] },
                        price: { type: decimal, scale: range::[0, 2], precision: range::[1, 7] },
                        categories: { 
                            type: list, 
                            element: {
                                any_of:[
                                    { type: string,codepoint_length: 5 },
                                    { type: int, $INT2_VALID_VALUES }
                                ]
                            }
                        }
                    }
                }
                """
            ),
            // bag of lists + structs
            ExampleInferenceTestCase(
                """
                ${'$'}partiql_bag::[ { a: 1,   b: [1, 2, 3],       c: { x: 1,   y: 2} },
                                     { a: 10,  b: [10, 20, 30],    c: { x: 10,  y: 20} },
                                     { a: 100, b: [100, 200, 300], c: { x: 100, y: 200} } ]
                """,
                """
                type::{ name: $typeName, type: bag,
                    element: { type: struct,
                        content: closed,
                        fields: {
                            a: { type: int, $INT2_VALID_VALUES },
                            b: { type: list, 
                                element: { type: int, $INT2_VALID_VALUES } },
                            c: { type: struct,
                                content: closed,
                                fields: {
                                    x: { type: int, $INT2_VALID_VALUES },
                                    y: { type: int, $INT2_VALID_VALUES }
                                }
                            }
                        }
                    }
                }
                """
            ),
            // maxExampleCount = 0
            ExampleInferenceTestCase(examplesOneTwoThreeIon, "type::{ name: $typeName }", maxExampleCount = 0),
            // maxExampleCount = 1
            ExampleInferenceTestCase(
                examplesOneTwoThreeIon,
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: { 
                        one: { type: int, $INT2_VALID_VALUES }
                    }
                }
                """,
                maxExampleCount = 1
            ),
            // maxExampleCount = 2
            ExampleInferenceTestCase(
                examplesOneTwoThreeIon,
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: { 
                        one: { type: int, $INT2_VALID_VALUES },
                        two: { type: int, $INT2_VALID_VALUES }
                    }
                }
                """,
                maxExampleCount = 2
            ),
            // maxExampleCount = 3 (equal to number of examples)
            ExampleInferenceTestCase(
                examplesOneTwoThreeIon,
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: { 
                        one: { type: int, $INT2_VALID_VALUES },
                        two: { type: int, $INT2_VALID_VALUES },
                        three: { type: int, $INT2_VALID_VALUES }
                    }
                }
                """,
                maxExampleCount = 3
            ),
            // maxExampleCount = 4 (greater than number of examples)
            ExampleInferenceTestCase(
                examplesOneTwoThreeIon,
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: { 
                        one: { type: int, $INT2_VALID_VALUES },
                        two: { type: int, $INT2_VALID_VALUES },
                        three: { type: int, $INT2_VALID_VALUES }
                    }
                }
                """,
                maxExampleCount = 4
            ),
            // $partiql_date
            ExampleInferenceTestCase("\$partiql_date::2020-01-01", "type::{ name:$typeName, type: date}"),
            // $partiql_date in struct
            ExampleInferenceTestCase(
                "{ today: \$partiql_date::2020-01-01 }",
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: {
                        today: { type: date }
                    }
                }
                """
            ),
            // $partiql_date and timestamp
            ExampleInferenceTestCase(
                "{ today: \$partiql_date::2020-01-01 }" + "{ today: \"2000-01-01T00:00:00Z\" }",
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: {
                        today: {
                            any_of: [
                                date,
                                { type: string, codepoint_length: 20 }
                            ]
                        }
                    }
                }
                """
            ),
            // $partiql_date and empty struct
            ExampleInferenceTestCase(
                "{ today: \$partiql_date::2020-01-01 }" + "{ }",
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: {
                        today: { type: date }
                    }
                }
                """
            ),
            // $partiql_time
            ExampleInferenceTestCase(timeIon, "type::{ name:$typeName, type: time }"),
            // $partiql_time in struct
            ExampleInferenceTestCase(
                "{ curTime: $timeIon }",
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: {
                        curTime: { type: time }
                    }
                }
                """
            ),
            // $partiql_time and conflicting struct
            ExampleInferenceTestCase(
                "{ curTime: $timeIon }" + "{ curTime: 1 }",
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: {
                        curTime: {
                            any_of: [
                                time,
                                { type: int, $INT2_VALID_VALUES }
                            ]
                        }
                    }
                }
                """
            ),
            // $partiql_time and empty struct
            ExampleInferenceTestCase(
                "{ curTime: $timeIon }" + "{ }",
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: {
                        curTime: { type: time }
                    }
                }
                """
            )
        )

        @ParameterizedTest
        @ArgumentsSource(SchemaDiscoveryTests::class)
        fun schemaDiscoveryTests(tc: ExampleInferenceTestCase) =
            assertCorrectISL(
                examples = tc.examples,
                islAsString = tc.islAsString,
                maxExampleCount = tc.maxExampleCount
            )
    }

    class ConstraintDiscoveryTests : ArgumentsProviderBase() {
        private fun createMultiExampleConstraintTestCase(
            examples: List<String>,
            constraints: List<String>
        ): ExampleInferenceTestCase {
            var examplesCombined = ""
            examples.map {
                examplesCombined += "{ foo: $it }"
            }

            val fooType = ion.singleValue(examples.first()).type.name.toLowerCase()
            var additionalConstraints = ""
            if (constraints.isNotEmpty()) {
                constraints.map {
                    additionalConstraints += ", $it"
                }
            }

            return ExampleInferenceTestCase(
                examples = examplesCombined,
                islAsString =
                """
                    type::{ name: $typeName, type: struct,
                        content: closed,
                        fields: {
                            foo: { type: $fooType$additionalConstraints }
                        }
                    }
                    """
            )
        }

        override fun getParameters(): List<Any> = listOf(
            // single example: int
            // single example: int (0)
            createMultiExampleConstraintTestCase(
                examples = listOf("0"),
                constraints = listOf(INT2_VALID_VALUES)
            ),
            // single example: int2
            createMultiExampleConstraintTestCase(
                examples = listOf("12345"),
                constraints = listOf(INT2_VALID_VALUES)
            ),
            // single example: int2 min
            createMultiExampleConstraintTestCase(
                examples = listOf("$MIN_INT2"),
                constraints = listOf(INT2_VALID_VALUES)
            ),
            // single example: int2 max
            createMultiExampleConstraintTestCase(
                examples = listOf("$MAX_INT2"),
                constraints = listOf(INT2_VALID_VALUES)
            ),
            // single example: int4 (minInt2 - 1)
            createMultiExampleConstraintTestCase(
                examples = listOf("${MIN_INT2 - 1}"),
                constraints = listOf(INT4_VALID_VALUES)
            ),
            // single example: int4 (maxInt2 + 1)
            createMultiExampleConstraintTestCase(
                examples = listOf("${MAX_INT2 + 1}"),
                constraints = listOf(INT4_VALID_VALUES)
            ),
            // single example: int4 min
            createMultiExampleConstraintTestCase(
                examples = listOf("$MIN_INT4"),
                constraints = listOf(INT4_VALID_VALUES)
            ),
            // single example: int4 max
            createMultiExampleConstraintTestCase(
                examples = listOf("$MAX_INT4"),
                constraints = listOf(INT4_VALID_VALUES)
            ),
            // single example: int8 (minInt4 - 1)
            createMultiExampleConstraintTestCase(
                examples = listOf("${MIN_INT4 - 1}"),
                constraints = listOf(INT8_VALID_VALUES)
            ),
            // single example: int8 (maxInt4 + 1)
            createMultiExampleConstraintTestCase(
                examples = listOf("${MAX_INT4 + 1}"),
                constraints = listOf(INT8_VALID_VALUES)
            ),
            // single example: int8 min
            createMultiExampleConstraintTestCase(
                examples = listOf("$MIN_INT8"),
                constraints = listOf(INT8_VALID_VALUES)
            ),
            // single example: int8 max
            createMultiExampleConstraintTestCase(
                examples = listOf("$MAX_INT8"),
                constraints = listOf(INT8_VALID_VALUES)
            ),
            // single example: unconstrained negative int
            createMultiExampleConstraintTestCase(
                examples = listOf("$NEG_BIG_INT"),
                constraints = emptyList()
            ),
            // single example: unconstrained positive int
            createMultiExampleConstraintTestCase(
                examples = listOf("$POS_BIG_INT"),
                constraints = emptyList()
            ),
            // single example: decimal
            // single example: decimal zero
            createMultiExampleConstraintTestCase(
                examples = listOf("0d0"),
                constraints = listOf("scale: 0", "precision: 1")
            ),
            // single example: decimal negative zero
            createMultiExampleConstraintTestCase(
                examples = listOf("-0d0"),
                constraints = listOf("scale: 0", "precision: 1")
            ),
            // single example: decimal w/ precision != 1, scale = 0
            createMultiExampleConstraintTestCase(
                examples = listOf("12345d0"),
                constraints = listOf("scale: 0", "precision: 5")
            ),
            // single example: decimal w/ precision = 1, scale != 0
            createMultiExampleConstraintTestCase(
                examples = listOf("1d-5"),
                constraints = listOf("scale: 5", "precision: 1")
            ),
            // single example: decimal w/ precision != 1, scale != 0
            createMultiExampleConstraintTestCase(
                examples = listOf("12345.123"),
                constraints = listOf("scale: 3", "precision: 8")
            ),
            // single example: string
            // single example: empty string
            createMultiExampleConstraintTestCase(
                examples = listOf("\"\""),
                constraints = listOf("codepoint_length: 0")
            ),
            // single example: non-empty string
            createMultiExampleConstraintTestCase(
                examples = listOf("\"abc\""),
                constraints = listOf("codepoint_length: 3")
            ),
            // multiple examples: int
            // int2 with int2 -> int2
            createMultiExampleConstraintTestCase(
                examples = listOf("12345", "-12345"),
                constraints = listOf(INT2_VALID_VALUES)
            ),
            // int2 with int4 -> int4
            createMultiExampleConstraintTestCase(
                examples = listOf("12345", "$MAX_INT4"),
                constraints = listOf(INT4_VALID_VALUES)
            ),
            // int2 with int8 -> int8
            createMultiExampleConstraintTestCase(
                examples = listOf("12345", "$MAX_INT8"),
                constraints = listOf(INT8_VALID_VALUES)
            ),
            // int4 with int8 -> int8
            createMultiExampleConstraintTestCase(
                examples = listOf("$MAX_INT4", "$MIN_INT8"),
                constraints = listOf(INT8_VALID_VALUES)
            ),
            // int2 and int4 with int8 -> int8
            createMultiExampleConstraintTestCase(
                examples = listOf("12345", "$MAX_INT4", "$MAX_INT8"),
                constraints = listOf(INT8_VALID_VALUES)
            ),
            // int2 with unconstrained int
            createMultiExampleConstraintTestCase(
                examples = listOf("$MAX_INT2", "$POS_BIG_INT"),
                constraints = emptyList()
            ),
            // int4 with unconstrained int
            createMultiExampleConstraintTestCase(
                examples = listOf("$MAX_INT4", "$POS_BIG_INT"),
                constraints = emptyList()
            ),
            // int8 with unconstrained int
            createMultiExampleConstraintTestCase(
                examples = listOf("$MAX_INT8", "$POS_BIG_INT"),
                constraints = emptyList()
            ),
            // unconstrained int with unconstrained int
            createMultiExampleConstraintTestCase(
                examples = listOf("$NEG_BIG_INT", "$POS_BIG_INT"),
                constraints = emptyList()
            ),
            // int2, int4, int8 with unconstrained int
            createMultiExampleConstraintTestCase(
                examples = listOf("$MIN_INT2", "$MAX_INT4", "$MIN_INT8", "$POS_BIG_INT"),
                constraints = emptyList()
            ),
            // multiple examples: decimal
            // decimals of the same scale and precision
            createMultiExampleConstraintTestCase(
                examples = listOf("12345.123", "54321.321"),
                constraints = listOf("scale: 3", "precision: 8")
            ),
            // decimals of the same scale and different precision
            createMultiExampleConstraintTestCase(
                examples = listOf("12345.123", "1234.123"),
                constraints = listOf("scale: 3", "precision: range::[7, 8]")
            ),
            // decimals of the different scale and same precision
            createMultiExampleConstraintTestCase(
                examples = listOf("12345.123", "123456.12"),
                constraints = listOf("scale: range::[2, 3]", "precision: 8")
            ),
            // decimals of the different scale and precision
            createMultiExampleConstraintTestCase(
                examples = listOf("12345.123", "123456.1234"),
                constraints = listOf("scale: range::[3, 4]", "precision: range::[8, 10]")
            ),
            // multiple decimals of the different scale and precision
            createMultiExampleConstraintTestCase(
                examples = listOf("1.1", "123.123", "12345.12345"),
                constraints = listOf("scale: range::[1, 5]", "precision: range::[2, 10]")
            ),
            // multiple examples: string
            // string with string of same lengths
            createMultiExampleConstraintTestCase(
                examples = listOf("\"123\"", "\"456\""),
                constraints = listOf("codepoint_length: 3")
            ),
            // string with string of differing lengths
            createMultiExampleConstraintTestCase(
                examples = listOf("\"abc\"", "\"abcdefgh\""),
                constraints = listOf("codepoint_length: range::[3, 8]")
            ),
            // collections + structs
            // list of int2s
            ExampleInferenceTestCase(
                "[1, 2, $MIN_INT2, $MAX_INT2]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: int, $INT2_VALID_VALUES }
                }
                """
            ),
            // list of int2 and int4
            ExampleInferenceTestCase(
                "[1, $MAX_INT4]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: int, $INT4_VALID_VALUES }
                }
                """
            ),
            // list of int2, int4, and unconstrained int
            ExampleInferenceTestCase(
                "[1, $MAX_INT4, $POS_BIG_INT]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: int }
                }
                """
            ),
            // list of differing decimal lengths
            ExampleInferenceTestCase(
                "[1., 1.1, 1.123]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: decimal, scale: range::[0, 3], precision: range::[1, 4] }
                }
                """
            ),
            // list of float32
            ExampleInferenceTestCase(
                "[1e0, 2e0, 4e0]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: float }
                }
                """
            ),
            // list of float32 and float64
            ExampleInferenceTestCase(
                "[1e0, 2e0, 4e0, ${Float.MAX_VALUE.toDouble() * 2}]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: float }
                }
                """
            ),
            // list of strings of different lengths
            ExampleInferenceTestCase(
                "[\"abc\", \"abcdef\", \"abcdefgh\"]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: string, codepoint_length: range::[3, 8] }
                }
                """
            ),
            // list of lists of strings
            ExampleInferenceTestCase(
                "[[\"abc\"], [\"abcdef\"], [\"abcdefgh\"]]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: list,
                        element: { type: string, codepoint_length: range::[3, 8] }
                    }
                }
                """
            ),
            // list of lists of strings and empty list
            ExampleInferenceTestCase(
                "[[\"abc\"], [\"abcdef\"], [\"abcdefgh\"], []]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: list,
                        element: { type: string, codepoint_length: range::[3, 8] }
                    }
                }
                """
            ),
            // list of lists of strings and empty list example
            ExampleInferenceTestCase(
                "[[\"abc\"], [\"abcdef\"], [\"abcdefgh\"]]" + "[[]]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: list,
                        element: { type: string, codepoint_length: range::[3, 8] }
                    }
                }
                """
            ),
            // list of lists of strings of different lengths
            ExampleInferenceTestCase(
                "[[\"\", \"a\", \"ab\"], [\"abc\", \"abcd\", \"abcde\"], [\"abcdef\", \"abcdefg\", \"abcdefgh\"]]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: list,
                        element: { type: string, codepoint_length: range::[0, 8] }
                    }
                }
                """
            ),
            // bag of different int2 and int4
            ExampleInferenceTestCase(
                "\$partiql_bag::[12345, $MAX_INT4]",
                """
                type::{name: $typeName, type: bag,
                    element: { type: int, $INT4_VALID_VALUES }
                }
                """
            ),
            // struct of int2s
            ExampleInferenceTestCase(
                "{ one: 1, two: 2 }",
                """
                type::{name: $typeName,type: struct,
                    content: closed,
                    fields: {
                        one: { type:int, $INT2_VALID_VALUES },
                        two: { type:int, $INT2_VALID_VALUES }
                    }
                }
                """
            ),
            // struct of int2s with struct of int4s
            ExampleInferenceTestCase(
                "{ one: 1, two: 2 }" + "{ one: $MIN_INT4, two: $MAX_INT4 }",
                """
                type::{name: $typeName,type: struct,
                    content: closed,
                    fields: {
                        one: { type:int, $INT4_VALID_VALUES },
                        two: { type:int, $INT4_VALID_VALUES }
                    }
                }
                """
            ),
            // struct of structs (conflicting field constraint, empty field, missing struct)
            ExampleInferenceTestCase(
                """
                {
                    structOne: { one: 1 },
                    structTwo: { },
                    structThree: { three: 3 }
                }
                {
                    structOne: { one: $MIN_INT4 },
                    structTwo: { two: 2 }
                }
                """,
                """
                type::{name: $typeName, type: struct,
                    content: closed,
                    fields: {
                        structOne: { type: struct, content: closed, fields: { one: { type:int, $INT4_VALID_VALUES } } },
                        structTwo: { type: struct, content: closed, fields: { two: { type:int, $INT2_VALID_VALUES } } },
                        structThree: { type: struct, content: closed, fields: { three: { type:int, $INT2_VALID_VALUES } } }
                    }
                }
                """
            ),
            // struct of multiple fields (string, decimal, string list)
            ExampleInferenceTestCase(
                """
                {
                    id: "book",
                    price: 6.,
                    categories: ["media", "books"]
                }
                {
                    id: "car",
                    price: 61234.22,
                    categories: ["automotive"]
                }
                """,
                """
                type::{name:$typeName, type: struct,
                    content: closed,
                    fields: {
                        id: { type: string, codepoint_length: range::[3,4] },
                        price: { type: decimal, scale: range::[0,2], precision: range::[1,7] },
                        categories: {
                            type: list,
                            element: { type: string, codepoint_length: range::[5,10] }
                        }
                    }
                }
                """
            ),
            // bag of lists + structs
            ExampleInferenceTestCase(
                """
                ${'$'}partiql_bag::[ { a: 1,          b: [1, 2, 3],       c: { x: 1,   y: 2 } },
                                     { a: 10,         b: [10, 20, 30],    c: { x: 10,  y: $POS_BIG_INT } },
                                     { a: $MAX_INT4, b: [100, 200, 300], c: { x: 100, y: 200 } } ]
                """,
                """
                type::{ name: $typeName, type: bag,
                    element: { type: struct,
                        content: closed,
                        fields: {
                            a: { type: int, $INT4_VALID_VALUES },
                            b: { type: list,
                                element: { type: int, $INT2_VALID_VALUES } },
                            c: { type: struct,
                                content: closed,
                                fields: {
                                    x: { type: int, $INT2_VALID_VALUES },
                                    y: { type: int }
                                }
                            }
                        }
                    }
                }
                """
            )
        )

        @ParameterizedTest
        @ArgumentsSource(ConstraintDiscoveryTests::class)
        fun constraintDiscoveryTests(tc: ExampleInferenceTestCase) =
            assertCorrectISL(
                examples = tc.examples,
                islAsString = tc.islAsString,
                maxExampleCount = tc.maxExampleCount
            )
    }

    class NullTypeTests : ArgumentsProviderBase() {
        // All typed nulls will collapse down to untyped null
        private fun createTypedNullTests(): List<ExampleInferenceTestCase> {
            val coreTypedNulls = listOf(
                "null.null", "null.int", "null.float", "null.decimal", "null.string",
                "null.symbol", "null.timestamp", "null.blob", "null.clob", "null.list", "null.sexp", "null.struct"
            )
            return coreTypedNulls.map { typedNull ->
                ExampleInferenceTestCase(typedNull, "type::{ name: $typeName, type: nullable::\$null }")
            }
        }

        override fun getParameters(): List<Any> = createTypedNullTests() + listOf(
            // null
            ExampleInferenceTestCase("null", "type::{ name: $typeName, type: nullable::\$null }"),
            // list of null.int
            ExampleInferenceTestCase("[null.int]", "type::{ name: $typeName, type: list, element: { type: nullable::\$null } }"),
            // struct with null.int field value
            ExampleInferenceTestCase(
                "{ foo: null.int }",
                """
                type::{ name: $typeName, type: struct,
                    content:closed,
                    fields: {
                        foo: { type: nullable::${'$'}null }
                    }
                }
                """
            ),
            // list of struct with null.int field value
            ExampleInferenceTestCase(
                "[ { foo: null.int } ]",
                """
                type::{ name: $typeName, type: list,
                    element: {
                        type: struct,
                        content:closed,
                        fields: {
                            foo: { type: nullable::${'$'}null }
                        }
                    }
                }
                """
            ),
            // struct of list with null.int field value
            ExampleInferenceTestCase(
                "{ foo: [ null.int ] }",
                """
                type::{ name: $typeName, type: struct,
                    content:closed,
                    fields: {
                        foo: { type: list, element: { type: nullable::${'$'}null } }
                    }
                }
                """
            ),
            // unification of null types tests
            // list of int and null
            ExampleInferenceTestCase(
                "[1, null]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: nullable::int, $INT2_VALID_VALUES }
                }
                """
            ),
            // list of int and null.int
            ExampleInferenceTestCase(
                "[1, null.int]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: nullable::int, $INT2_VALID_VALUES }
                }
                """
            ),
            // list of int, null, and null.int
            ExampleInferenceTestCase(
                "[1, null, null.int]",
                """
                type::{ name: $typeName, type: list,
                    element: { type: nullable::int, $INT2_VALID_VALUES }
                }
                """
            ),
            // empty list and null
            ExampleInferenceTestCase("[] null", "type::{ name: $typeName, type: nullable::list }"),
            // empty list and null.list
            ExampleInferenceTestCase("[] null.list", "type::{ name: $typeName, type: nullable::list }"),
            // empty list, null, and null.list
            ExampleInferenceTestCase("[] null null.list", "type::{ name: $typeName, type: nullable::list }"),
            // empty struct and null
            ExampleInferenceTestCase("{} null", "type::{ name: $typeName, type: nullable::struct, content:closed }"),
            // empty struct and null.struct
            ExampleInferenceTestCase("{} null.struct", "type::{ name: $typeName, type: nullable::struct, content:closed }"),
            // empty struct, null, and null.struct
            ExampleInferenceTestCase("{} null null.struct", "type::{ name: $typeName, type: nullable::struct, content:closed }"),
            // lists with conflicting types
            // list of int, decimal, and null
            ExampleInferenceTestCase(
                "[null, 1, 1d0]",
                """
                type::{ name: $typeName, type: list,
                    element: {
                        any_of: [
                            { type: nullable::int, $INT2_VALID_VALUES },
                            { type: nullable::decimal, scale: 0, precision: 1 }
                        ]
                    }
                }
                """
            ),
            // list of int, decimal, and null (separate lists)
            ExampleInferenceTestCase(
                "[1, 1d0] [null]",
                """
                type::{ name: $typeName, type: list,
                    element: {
                        any_of: [
                            { type: nullable::int, $INT2_VALID_VALUES },
                            { type: nullable::decimal, scale: 0, precision: 1 }
                        ]
                    }
                }
                """
            ),
            // list of int and decimal with int and null (separate lists)
            ExampleInferenceTestCase(
                "[1, 1d0] [1, null]",
                """
                type::{ name: $typeName, type: list,
                    element: {
                        any_of: [
                            { type: nullable::int, $INT2_VALID_VALUES },
                            { type: nullable::decimal, scale: 0, precision: 1 }
                        ]
                    }
                }
                """
            ),
            // structs with nullable type
            ExampleInferenceTestCase(
                "{ one: 1 } { one: null }",
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: {
                        one: { type: nullable::int, $INT2_VALID_VALUES }
                    }
                }
                """
            ),
            // structs with nullable type and conflict
            ExampleInferenceTestCase(
                "{ one: 1 } { one: 1d0 } { one: null }",
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: {
                        one: {
                            any_of: [
                                { type: nullable::int, $INT2_VALID_VALUES },
                                { type: nullable::decimal, scale: 0, precision: 1 }
                            ]
                        }
                    }
                }
                """
            ),
            // structs with nullable container type
            ExampleInferenceTestCase(
                "{ one: [1] } { one: null }",
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: {
                        one: {
                            type: nullable::list,
                            element: { type: int, $INT2_VALID_VALUES }
                        }
                    }
                }
                """
            ),
            // structs with nullable container type and nullable element
            ExampleInferenceTestCase(
                "{ one: [1] } { one: null } { one: [null] }",
                """
                type::{ name: $typeName, type: struct,
                    content: closed,
                    fields: {
                        one: {
                            type: nullable::list,
                            element: { type: nullable::int, $INT2_VALID_VALUES }
                        }
                    }
                }
                """
            ),
            // bag of structs with nullable field
            ExampleInferenceTestCase(
                """
                ${'$'}partiql_bag::[ { a: null },
                                     { a: 10 },
                                     { a: 100 } ]
                """,
                """
                type::{ name: $typeName, type: bag,
                    element: { type: struct,
                        content: closed,
                        fields: {
                            a: { type: nullable::int, $INT2_VALID_VALUES }
                        }
                    }
                }
                """
            ),
            // bag of nullable structs with nullable field
            ExampleInferenceTestCase(
                """
                ${'$'}partiql_bag::[ { a: null },
                                     { a: 10 },
                                     { a: 100 },
                                     null ]
                """,
                """
                type::{ name: $typeName, type: bag,
                    element: { type: nullable::struct,
                        content: closed,
                        fields: {
                            a: { type: nullable::int, $INT2_VALID_VALUES }
                        }
                    }
                }
                """
            ),
            // bag of structs with nullable lists
            ExampleInferenceTestCase(
                """
                ${'$'}partiql_bag::[ { b: [1, 2, 3] },
                                     { b: null },
                                     { b: [100, 200, 300] }]
                """,
                """
                type::{ name: $typeName, type: bag,
                    element: { type: struct,
                        content: closed,
                        fields: {
                            b: { type: nullable::list,
                                 element: { type: int, $INT2_VALID_VALUES } },
                        }
                    }
                }
                """
            ),
            // bag of structs with nullable structs and nullable field
            ExampleInferenceTestCase(
                """
                ${'$'}partiql_bag::[ { c: { x: null, y: 2} },
                                     { c: { x: 10,   y: 20} },
                                     { c: null } ]
                """,
                """
                type::{ name: $typeName, type: bag,
                    element: { type: struct,
                        content: closed,
                        fields: {
                            c: { type: nullable::struct,
                                content: closed,
                                fields: {
                                    x: { type: nullable::int, $INT2_VALID_VALUES },
                                    y: { type: int, $INT2_VALID_VALUES }
                                }
                            }
                        }
                    }
                }
                """
            ),
            // (combined) bag of structs with nullable fields
            ExampleInferenceTestCase(
                """
                ${'$'}partiql_bag::[ { a: null,   b: [1, 2, 3],       c: { x: null,   y: 2} },
                                     { a: 10,     b: null,            c: { x: 10,  y: 20} },
                                     { a: 100,    b: [100, 200, 300], c: null } ]
                """,
                """
                type::{ name: $typeName, type: bag,
                    element: { type: struct,
                        content: closed,
                        fields: {
                            a: { type: nullable::int, $INT2_VALID_VALUES },
                            b: { type: nullable::list,
                                element: { type: int, $INT2_VALID_VALUES } },
                            c: { type: nullable::struct,
                                content: closed,
                                fields: {
                                    x: { type: nullable::int, $INT2_VALID_VALUES },
                                    y: { type: int, $INT2_VALID_VALUES }
                                }
                            }
                        }
                    }
                }
                """
            )
        )

        @ParameterizedTest
        @ArgumentsSource(NullTypeTests::class)
        fun nullTypeTests(tc: ExampleInferenceTestCase) =
            assertCorrectISL(
                examples = tc.examples,
                islAsString = tc.islAsString,
                maxExampleCount = tc.maxExampleCount
            )
    }

    class DefiniteSchemaUnifyingTests : ArgumentsProviderBase() {
        private val decimalValidValuesRange = "valid_values: range::[-9.9d125, 9.9d125]"
        private val stringUTF8ByteLengthRange = "utf8_byte_length: range::[0, 409600]"
        private val blobByteLengthRange = "byte_length: range::[1, 1024]"

        override fun getParameters(): List<Any> = listOf(
            InferenceAndDefiniteUnifyTestCase(
                // decimal has precision and scale discovered
                name = "decimal unified with definite schema with non-discovered constraint (valid_values)",
                examples = "1d0",
                definiteIslAsString =
                """
                    type::{ name: $typeName, type: decimal,
                        $decimalValidValuesRange
                    }
                    """,
                islAsString =
                """
                    type::{ name: $typeName, type: decimal,
                        scale: 0,
                        precision: 1,
                        $decimalValidValuesRange
                    }
                    """
            ),
            InferenceAndDefiniteUnifyTestCase(
                // if discovered and definite schemas have a same constraint, take the discovered constraint
                name = "decimal unified with definite schema with discovered (precision) and non-discovered constraint (valid_values)",
                examples = "1d0",
                definiteIslAsString =
                """
                    type::{ name: $typeName, type: decimal,
                        precision: range::[1, 38],
                        $decimalValidValuesRange
                    }
                    """,
                islAsString =
                """
                    type::{ name: $typeName, type: decimal,
                        scale: 0,
                        precision: 1,
                        $decimalValidValuesRange
                    }
                    """
            ),
            InferenceAndDefiniteUnifyTestCase(
                // string has just codepoint_length discovered
                name = "string unified with definite schema with non-discovered constraint (utf8_byte_length)",
                examples = "\"abc\"",
                definiteIslAsString =
                """
                    type::{ name: $typeName, type: string,
                        $stringUTF8ByteLengthRange
                    }
                    """,
                islAsString =
                """
                    type::{ name: $typeName, type: string,
                        codepoint_length: 3,
                        $stringUTF8ByteLengthRange
                    }
                    """
            ),
            InferenceAndDefiniteUnifyTestCase(
                // blob has no constraints discovered
                name = "blob unified with definite schema with non-discovered constraint (byte_length)",
                examples = "{{ +AB/ }}",
                definiteIslAsString =
                """
                    type::{ name: $typeName, type: blob,
                        $blobByteLengthRange
                    }
                    """,
                islAsString =
                """
                    type::{ name: $typeName, type: blob,
                        $blobByteLengthRange
                    }
                    """
            ),
            InferenceAndDefiniteUnifyTestCase(
                name = "union(int, decimal) with definite schema of decimal with non-discovered constraint (valid_values)",
                examples = "1 1d0",
                definiteIslAsString =
                """
                    type::{ name: $typeName, type: decimal,
                        $decimalValidValuesRange
                    }
                    """,
                islAsString =
                """
                    type::{ name: $typeName, any_of:[
                        { type: int, $INT2_VALID_VALUES },
                        { type: decimal, scale: 0, precision: 1, $decimalValidValuesRange }
                    ]}
                    """
            ),
            InferenceAndDefiniteUnifyTestCase(
                name = "decimal with definite schema of union(blob, decimal) with non-discovered constraints",
                examples = "1d0",
                definiteIslAsString =
                """
                    type::{ name: $typeName, any_of:[
                        { type: blob, $blobByteLengthRange },
                        { type: decimal, $decimalValidValuesRange }
                    ]}
                    """,
                islAsString =
                """
                    type::{ name: $typeName, any_of:[
                        { type: blob, $blobByteLengthRange },
                        { type: decimal, scale: 0, precision: 1, $decimalValidValuesRange }
                    ]}
                    """
            ),
            InferenceAndDefiniteUnifyTestCase(
                name = "empty struct with definite schema struct",
                examples = "{ }",
                definiteIslAsString =
                """
                    type::{ name: $typeName, type: struct,
                        fields: {
                            a: { type: decimal, $decimalValidValuesRange },
                        }
                    }
                    """,
                islAsString =
                """
                    type::{ name: $typeName, type: struct, content: closed,
                        fields: {
                            a: { type: decimal, $decimalValidValuesRange },
                        }
                    }
                    """
            ),
            InferenceAndDefiniteUnifyTestCase(
                name = "struct with definite schema struct with additional field",
                examples = "{ a: 1d0 }",
                definiteIslAsString =
                """
                    type::{ name: $typeName, type: struct,
                        fields: {
                            a: { type: decimal, $decimalValidValuesRange },
                            b: { type: blob, $blobByteLengthRange }
                        }
                    }
                    """,
                islAsString =
                """
                    type::{ name: $typeName, type: struct, content: closed,
                        fields: {
                            a: { type: decimal, scale: 0, precision: 1, $decimalValidValuesRange },
                            b: { type: blob, $blobByteLengthRange }
                        }
                    }
                    """
            ),
            InferenceAndDefiniteUnifyTestCase(
                name = "struct with additional fields with definite schema struct",
                examples = "{ a: 1d0, b: {{ +AB/ }}, c: \"abc\" }",
                definiteIslAsString =
                """
                    type::{ name: $typeName, type: struct,
                        fields: {
                            a: { type: decimal, $decimalValidValuesRange },
                        }
                    }
                    """,
                islAsString =
                """
                    type::{ name: $typeName, type: struct, content: closed,
                        fields: {
                            a: { type: decimal, scale: 0, precision: 1, $decimalValidValuesRange },
                            b: { type: blob },
                            c: { type: string, codepoint_length: 3 }
                        }
                    }
                    """
            ),
            InferenceAndDefiniteUnifyTestCase(
                name = "bag of struct unified with additional decimal constraint",
                examples = "\$partiql_bag::[ { a: 1d0 } ]",
                definiteIslAsString =
                """
                    type::{ name: $typeName, type: bag,
                        element: {
                            type: struct,
                            fields: {
                                a: { type: decimal, $decimalValidValuesRange },
                            }
                        }
                    }
                    """,
                islAsString =
                """
                    type::{ name: $typeName, type: bag,
                        element: {
                            type: struct, content: closed,
                            fields: {
                                a: { type: decimal, scale: 0, precision: 1, $decimalValidValuesRange },
                            }
                        }
                    }
                    """
            ),
            InferenceAndDefiniteUnifyTestCase(
                name = "bag of struct unified with additional decimal constraint",
                examples = "\$partiql_bag::[ { a: 1d0 } ]",
                definiteIslAsString =
                """
                    type::{ name: $typeName, type: bag,
                        element: {
                            type: struct,
                            fields: {
                                a: { type: decimal, $decimalValidValuesRange },
                            }
                        }
                    }
                    """,
                islAsString =
                """
                    type::{ name: $typeName, type: bag,
                        element: {
                            type: struct, content: closed,
                            fields: {
                                a: { type: decimal, scale: 0, precision: 1, $decimalValidValuesRange },
                            }
                        }
                    }
                    """
            ),
            InferenceAndDefiniteUnifyTestCase(
                name = "bag of structs unified with additional constraints",
                examples =
                """
                    ${'$'}partiql_bag::[ { a: 1,   b: ["a", "b", "c"],       c: { x: 1.,   y: {{ +AA/ }} } },
                                         { a: 10,  b: ["aa", "bb", "cc"],    c: { x: 10.,  y: {{ +BB/ }} } },
                                         { a: 100, b: ["aaa", "bbb", "ccc"], c: { x: 100., y: {{ +CC/ }} } } ]
                    """,
                definiteIslAsString =
                """
                    type::{ name: $typeName, type: bag,
                        element: {
                            type: struct,
                            fields: {
                                b: { type: list, element: { type: string, $stringUTF8ByteLengthRange } },
                                c: { type: struct,
                                    fields: {
                                        x: { type: decimal, $decimalValidValuesRange },
                                        y: { type: blob, $blobByteLengthRange }
                                    }
                                }
                            }
                        }
                    }
                    """,
                islAsString =
                """
                    type::{ name: $typeName, type: bag,
                        element: {
                            type: struct, content: closed,
                            fields: {
                                a: { type: int, $INT2_VALID_VALUES },
                                b: { type: list, element: { type: string, codepoint_length:range::[1, 3], $stringUTF8ByteLengthRange } },
                                c: { type: struct, content: closed,
                                    fields: {
                                        x: { type: decimal, scale: 0, precision:range::[1,3], $decimalValidValuesRange },
                                        y: { type: blob, $blobByteLengthRange }
                                    }
                                }
                            }
                        }
                    }
                    """
            )
        )

        @ParameterizedTest
        @ArgumentsSource(DefiniteSchemaUnifyingTests::class)
        fun definiteSchemaUnifyingTests(tc: InferenceAndDefiniteUnifyTestCase) =
            assertCorrectISL(
                examples = tc.examples,
                definiteIslAsString = tc.definiteIslAsString,
                islAsString = tc.islAsString,
                maxExampleCount = tc.maxExampleCount
            )
    }
}
