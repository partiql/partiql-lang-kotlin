@file:OptIn(PartiQLValueExperimental::class)

package org.partiql.value.io

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.value.Annotations
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.bagValue
import org.partiql.value.boolValue
import org.partiql.value.charValue
import org.partiql.value.decimalValue
import org.partiql.value.float32Value
import org.partiql.value.float64Value
import org.partiql.value.int16Value
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.int8Value
import org.partiql.value.intValue
import org.partiql.value.listValue
import org.partiql.value.missingValue
import org.partiql.value.nullValue
import org.partiql.value.sexpValue
import org.partiql.value.stringValue
import org.partiql.value.structValue
import org.partiql.value.symbolValue
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Basic text writing test.
 *
 * TODOs
 *  - Dates and times
 *  - String/Symbol escapes
 */
class PartiQLValueTextWriterTest {

    @ParameterizedTest
    @MethodSource("scalars")
    @Execution(ExecutionMode.CONCURRENT)
    fun testScalars(case: Case) = case.assert()

    @ParameterizedTest
    @MethodSource("nulls")
    @Execution(ExecutionMode.CONCURRENT)
    fun testNulls(case: Case) = case.assert()

    @ParameterizedTest
    @MethodSource("collections")
    @Execution(ExecutionMode.CONCURRENT)
    fun testCollections(case: Case) = case.assert()

    @ParameterizedTest
    @MethodSource("struct")
    @Execution(ExecutionMode.CONCURRENT)
    fun testStruct(case: Case) = case.assert()

    @ParameterizedTest
    @MethodSource("collectionsFormatted")
    @Execution(ExecutionMode.CONCURRENT)
    fun testCollectionsFormatted(case: Case) = case.assert()

    @ParameterizedTest
    @MethodSource("structFormatted")
    @Execution(ExecutionMode.CONCURRENT)
    fun testStructFormatted(case: Case) = case.assert()

    @ParameterizedTest
    @MethodSource("nestedCollectionsFormatted")
    @Execution(ExecutionMode.CONCURRENT)
    fun testNestedCollectionsFormatted(case: Case) = case.assert()

    @ParameterizedTest
    @MethodSource("annotations")
    @Execution(ExecutionMode.CONCURRENT)
    fun testAnnotations(case: Case) = case.assert()

    @OptIn(PartiQLValueExperimental::class)
    companion object {

        private val annotations: Annotations = listOf("x", "y")

        private fun case(value: PartiQLValue, expected: String) = Case(value, expected, false)

        private fun formatted(value: PartiQLValue, expected: String) = Case(value, expected, true)

        @JvmStatic
        fun scalars() = listOf(
            case(
                value = nullValue(),
                expected = "null",
            ),
            case(
                value = missingValue(),
                expected = "missing",
            ),
            case(
                value = boolValue(true),
                expected = "true",
            ),
            case(
                value = boolValue(false),
                expected = "false",
            ),
            case(
                value = int8Value(1),
                expected = "1",
            ),
            case(
                value = int16Value(1),
                expected = "1",
            ),
            case(
                value = int32Value(1),
                expected = "1",
            ),
            case(
                value = int64Value(1),
                expected = "1",
            ),
            case(
                value = intValue(BigInteger.valueOf(1)),
                expected = "1",
            ),
            case(
                value = decimalValue(BigDecimal("123.456")),
                expected = "123.456",
            ),
            case(
                value = decimalValue(BigDecimal("0.456")),
                expected = "0.456",
            ),
            case(
                value = float32Value(123.0f),
                expected = "123.0",
            ),
            case(
                value = float32Value(123f),
                expected = "123.0",
            ),
            case(
                value = float64Value(123.0),
                expected = "123.0",
            ),
            case(
                value = float64Value(123.toDouble()),
                expected = "123.0",
            ),
            case(
                value = charValue('C'),
                expected = "'C'",
            ),
            case(
                value = stringValue("word"),
                expected = "'word'",
            ),
            case(
                value = stringValue("word\nword"),
                expected = "'word\nword'",
            ),
            case(
                value = symbolValue("x"),
                expected = "x",
            ),
            case(
                value = symbolValue("f.x"),
                expected = "f.x",
            ),
            // TODO CLOB
            // TODO BINARY
            // TODO BYTE
            // TODO BLOB
            // TODO DATE
            // TODO TIME
            // TODO TIMESTAMP
            // TODO INTERVAL
        )

        @JvmStatic
        fun nulls() = listOf(
            case(
                value = nullValue(),
                expected = "null",
            ),
            case(
                value = missingValue(),
                expected = "missing",
            ),
            case(
                value = boolValue(true),
                expected = "true",
            ),
            case(
                value = boolValue(false),
                expected = "false",
            ),
            case(
                value = boolValue(null),
                expected = "null",
            ),
            case(
                value = int8Value(1),
                expected = "1",
            ),
            case(
                value = int8Value(null),
                expected = "null",
            ),
            case(
                value = int16Value(1),
                expected = "1",
            ),
            case(
                value = int16Value(null),
                expected = "null",
            ),
            case(
                value = int32Value(1),
                expected = "1",
            ),
            case(
                value = int32Value(null),
                expected = "null",
            ),
            case(
                value = int64Value(1),
                expected = "1",
            ),
            case(
                value = int64Value(null),
                expected = "null",
            ),
            case(
                value = intValue(BigInteger.valueOf(1)),
                expected = "1",
            ),
            case(
                value = intValue(null),
                expected = "null",
            ),
            case(
                value = decimalValue(BigDecimal("123.456")),
                expected = "123.456",
            ),
            case(
                value = decimalValue(null),
                expected = "null",
            ),
            case(
                value = float32Value(123.0f),
                expected = "123.0",
            ),
            case(
                value = float32Value(null),
                expected = "null",
            ),
            case(
                value = float64Value(123.0),
                expected = "123.0",
            ),
            case(
                value = float64Value(null),
                expected = "null",
            ),
            case(
                value = charValue('C'),
                expected = "'C'",
            ),
            case(
                value = charValue(null),
                expected = "null",
            ),
            case(
                value = stringValue("word"),
                expected = "'word'",
            ),
            case(
                value = stringValue(null),
                expected = "null",
            ),
            case(
                value = stringValue("word\nword"),
                expected = "'word\nword'",
            ),
            case(
                value = stringValue(null),
                expected = "null",
            ),
            case(
                value = symbolValue("x"),
                expected = "x",
            ),
            case(
                value = symbolValue(null),
                expected = "null",
            ),
            case(
                value = symbolValue("f.x"),
                expected = "f.x",
            ),
            case(
                value = symbolValue(null),
                expected = "null",
            ),
            // TODO CLOB
            // TODO BINARY
            // TODO BYTE
            // TODO BLOB
            // TODO DATE
            // TODO TIME
            // TODO TIMESTAMP
            // TODO INTERVAL
        )

        @JvmStatic
        fun collections() = listOf(
            case(
                value = bagValue(emptySequence()),
                expected = "<<>>",
            ),
            case(
                value = listValue(emptySequence()),
                expected = "[]",
            ),
            case(
                value = sexpValue(emptySequence()),
                expected = "()",
            ),
            case(
                value = bagValue(
                    sequenceOf(
                        int32Value(1),
                        int32Value(2),
                        int32Value(3),
                    )
                ),
                expected = "<<1,2,3>>",
            ),
            case(
                value = listValue(
                    sequenceOf(
                        stringValue("a"),
                        stringValue("b"),
                        stringValue("c"),
                    )
                ),
                expected = "['a','b','c']",
            ),
            case(
                value = sexpValue(
                    sequenceOf(
                        int32Value(1),
                        int32Value(2),
                        int32Value(3),
                    )
                ),
                expected = "(1 2 3)",
            ),
            //  collections
            case(
                value = bagValue<PartiQLValue>(null),
                expected = "null",
            ),
            case(
                value = listValue<PartiQLValue>(null),
                expected = "null",
            ),
            case(
                value = sexpValue<PartiQLValue>(null),
                expected = "null",
            ),
            case(
                value = structValue<PartiQLValue>(null),
                expected = "null",
            ),
        )

        @JvmStatic
        fun struct() = listOf(
            case(
                value = structValue<PartiQLValue>(emptySequence()),
                expected = "{}",
            ),
            case(
                value = structValue(
                    sequenceOf(
                        "a" to int32Value(1),
                        "b" to stringValue("x"),
                    )
                ),
                expected = "{a:1,b:'x'}",
            ),
        )

        @JvmStatic
        fun collectionsFormatted() = listOf(
            formatted(
                value = bagValue(
                    sequenceOf(
                        int32Value(1),
                        int32Value(2),
                        int32Value(3),
                    )
                ),
                expected = """
                    |<<
                    |  1,
                    |  2,
                    |  3
                    |>>
                """.trimMargin("|"),
            ),
            formatted(
                value = listValue(
                    sequenceOf(
                        stringValue("a"),
                        stringValue("b"),
                        stringValue("c"),
                    )
                ),
                expected = """
                    |[
                    |  'a',
                    |  'b',
                    |  'c'
                    |]
                """.trimMargin("|"),
            ),
            formatted(
                value = sexpValue(
                    sequenceOf(
                        int32Value(1),
                        int32Value(2),
                        int32Value(3),
                    )
                ),
                expected = """
                    |(
                    |  1,
                    |  2,
                    |  3
                    |)
                """.trimMargin("|"),
            ),
        )

        @JvmStatic
        fun structFormatted() = listOf(
            formatted(
                value = structValue<PartiQLValue>(emptySequence()),
                expected = "{}",
            ),
            formatted(
                value = structValue(
                    sequenceOf(
                        "a" to int32Value(1),
                        "b" to stringValue("x"),
                    )
                ),
                expected = """
                    |{
                    |  a: 1,
                    |  b: 'x'
                    |}
                """.trimMargin("|"),
            ),
        )

        @JvmStatic
        fun nestedCollectionsFormatted() = listOf(
            formatted(
                value = structValue(
                    sequenceOf(
                        "bag" to bagValue(
                            sequenceOf(
                                int32Value(1),
                                int32Value(2),
                                int32Value(3),
                            )
                        ),
                        "list" to listValue(
                            sequenceOf(
                                stringValue("a"),
                                stringValue("b"),
                                stringValue("c"),
                            )
                        ),
                        "sexp" to sexpValue(
                            sequenceOf(
                                int32Value(1),
                                int32Value(2),
                                int32Value(3),
                            )
                        ),
                    )
                ),
                expected = """
                    |{
                    |  bag: <<
                    |    1,
                    |    2,
                    |    3
                    |  >>,
                    |  list: [
                    |    'a',
                    |    'b',
                    |    'c'
                    |  ],
                    |  sexp: (
                    |    1,
                    |    2,
                    |    3
                    |  )
                    |}
                """.trimMargin("|"),
            ),
            formatted(
                value = bagValue(
                    sequenceOf(
                        listValue(
                            sequenceOf(
                                stringValue("a"),
                                stringValue("b"),
                                stringValue("c"),
                            )
                        ),
                        sexpValue(
                            sequenceOf(
                                int32Value(1),
                                int32Value(2),
                                int32Value(3),
                            )
                        ),
                        structValue(
                            sequenceOf(
                                "a" to int32Value(1),
                                "b" to stringValue("x"),
                            )
                        ),
                    )
                ),
                expected = """
                    |<<
                    |  [
                    |    'a',
                    |    'b',
                    |    'c'
                    |  ],
                    |  (
                    |    1,
                    |    2,
                    |    3
                    |  ),
                    |  {
                    |    a: 1,
                    |    b: 'x'
                    |  }
                    |>>
                """.trimMargin("|"),
            ),
            formatted(
                value = structValue(
                    sequenceOf(
                        "bag" to bagValue<PartiQLValue>(emptySequence()),
                        "list" to listValue<PartiQLValue>(emptySequence()),
                        "sexp" to sexpValue<PartiQLValue>(emptySequence()),
                    )
                ),
                expected = """
                    |{
                    |  bag: <<>>,
                    |  list: [],
                    |  sexp: ()
                    |}
                """.trimMargin("|"),
            ),
            formatted(
                value = bagValue(
                    sequenceOf(
                        listValue<PartiQLValue>(emptySequence()),
                        sexpValue<PartiQLValue>(emptySequence()),
                        structValue<PartiQLValue>(emptySequence()),
                    )
                ),
                expected = """
                    |<<
                    |  [],
                    |  (),
                    |  {}
                    |>>
                """.trimMargin("|"),
            ),
        )

        @JvmStatic
        fun annotations() = listOf(
            case(
                value = nullValue(annotations),
                expected = "x::y::null",
            ),
            case(
                value = missingValue(annotations),
                expected = "x::y::missing",
            ),
            case(
                value = boolValue(true, annotations),
                expected = "x::y::true",
            ),
            case(
                value = boolValue(false, annotations),
                expected = "x::y::false",
            ),
            case(
                value = int8Value(1, annotations),
                expected = "x::y::1",
            ),
            case(
                value = int16Value(1, annotations),
                expected = "x::y::1",
            ),
            case(
                value = int32Value(1, annotations),
                expected = "x::y::1",
            ),
            case(
                value = int64Value(1, annotations),
                expected = "x::y::1",
            ),
            case(
                value = intValue(BigInteger.valueOf(1), annotations),
                expected = "x::y::1",
            ),
            case(
                value = decimalValue(BigDecimal("123.456"), annotations),
                expected = "x::y::123.456",
            ),
            case(
                value = decimalValue(BigDecimal("0.456"), annotations),
                expected = "x::y::0.456",
            ),
            case(
                value = float32Value(123.0f, annotations),
                expected = "x::y::123.0",
            ),
            case(
                value = float32Value(123f, annotations),
                expected = "x::y::123.0",
            ),
            case(
                value = float64Value(123.0, annotations),
                expected = "x::y::123.0",
            ),
            case(
                value = float64Value(123.toDouble(), annotations),
                expected = "x::y::123.0",
            ),
            case(
                value = charValue('C', annotations),
                expected = "x::y::'C'",
            ),
            case(
                value = stringValue("word", annotations),
                expected = "x::y::'word'",
            ),
            case(
                value = stringValue("word\nword", annotations),
                expected = "x::y::'word\nword'",
            ),
            case(
                value = symbolValue("x", annotations),
                expected = "x::y::x",
            ),
            case(
                value = symbolValue("f.x", annotations),
                expected = "x::y::f.x",
            ),
            // TODO CLOB
            // TODO BINARY
            // TODO BYTE
            // TODO BLOB
            // TODO DATE
            // TODO TIME
            // TODO TIMESTAMP
            // TODO INTERVAL
            case(
                value = bagValue(emptySequence(), annotations),
                expected = "x::y::<<>>",
            ),
            case(
                value = listValue(emptySequence(), annotations),
                expected = "x::y::[]",
            ),
            case(
                value = sexpValue(emptySequence(), annotations),
                expected = "x::y::()",
            ),
            formatted(
                value = bagValue(
                    sequenceOf(
                        listValue(
                            sequenceOf(
                                stringValue("a", listOf("x")),
                            ),
                            listOf("list")
                        ),
                        sexpValue(
                            sequenceOf(
                                int32Value(1, listOf("y")),
                            ),
                            listOf("sexp")
                        ),
                        structValue(
                            sequenceOf(
                                "a" to int32Value(1, listOf("z")),
                            ),
                            listOf("struct")
                        ),
                    )
                ),
                expected = """
                    |<<
                    |  list::[
                    |    x::'a'
                    |  ],
                    |  sexp::(
                    |    y::1
                    |  ),
                    |  struct::{
                    |    a: z::1
                    |  }
                    |>>
                """.trimMargin("|"),
            ),
        )
    }

    @OptIn(PartiQLValueExperimental::class)
    class Case(
        private val value: PartiQLValue,
        private val expected: String,
        private val formatted: Boolean,
    ) {

        fun assert() {
            val buffer = ByteArrayOutputStream()
            val out = PrintStream(buffer)
            val writer = PartiQLValueTextWriter(out, formatted)
            writer.append(value)
            val actual = buffer.toString()
            // string equality
            Assertions.assertEquals(expected, actual)
        }
    }
}
