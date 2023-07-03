package org.partiql.value.io

import com.amazon.ion.IonValue
import com.amazon.ion.system.IonBinaryWriterBuilder
import com.amazon.ion.system.IonReaderBuilder
import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ion.system.IonTextWriterBuilder
import com.amazon.ionelement.api.toIonElement
import com.amazon.ionelement.api.toIonValue
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
import org.partiql.value.impl.PartiQLValueIonReaderBuilder
import org.partiql.value.int16Value
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.int8Value
import org.partiql.value.intValue
import org.partiql.value.listValue
import org.partiql.value.nullableBagValue
import org.partiql.value.sexpValue
import org.partiql.value.stringValue
import org.partiql.value.structValue
import org.partiql.value.symbolValue
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertEquals

@OptIn(PartiQLValueExperimental::class)
class PartiQLValueIonSerdeTest {

    @ParameterizedTest
    @MethodSource("nulls")
    @Execution(ExecutionMode.CONCURRENT)
    fun testNulls(case: Case) = assert(case)

    @ParameterizedTest
    @MethodSource("scalars")
    @Execution(ExecutionMode.CONCURRENT)
    fun testScalars(case: Case) = assert(case)

    @ParameterizedTest
    @MethodSource("collections")
    @Execution(ExecutionMode.CONCURRENT)
    fun testCollections(case: Case) = assert(case)

    @ParameterizedTest
    @MethodSource("annotations")
    @Execution(ExecutionMode.CONCURRENT)
    fun testAnnotations(case: Case) = assert(case)

    companion object {
        private val ION = IonSystemBuilder.standard().build()

        private val annotations: Annotations = listOf("x", "y")

        private fun oneWayTrip(partiQLValueBefore: PartiQLValue, ionValue: IonValue, partiQLValueAfter: PartiQLValue) = Case.OneWayTrip(partiQLValueBefore, ionValue, partiQLValueAfter)

        private fun roundTrip(partiQLValue: PartiQLValue, ionValue: IonValue) = Case.RoundTrip(partiQLValue, ionValue)

        @JvmStatic
        fun nulls() = listOf(
            // absent value
//            roundTrip(
//                nullValue(),
//                ION.newNull()
//            ),
//            roundTrip(
//                missingValue(),
//                ION.newNull().apply { addTypeAnnotation("\$missing") }
//            ),
            // when(reader.type){
            //                IonType.NULL -> TODO()
            //                IonType.BOOL -> TODO()
            //                IonType.INT -> TODO()
            //                IonType.FLOAT -> TODO()
            //                IonType.DECIMAL -> TODO()
            //                IonType.TIMESTAMP -> TODO()
            //                IonType.SYMBOL -> TODO()
            //                IonType.STRING -> TODO()
            //                IonType.CLOB -> TODO()
            //                IonType.BLOB -> TODO()
            //                IonType.LIST -> TODO()
            //                IonType.SEXP -> TODO()
            //                IonType.STRUCT -> TODO()
            //                IonType.DATAGRAM -> TODO()
            //            }
            // bool.null
//            roundTrip(
//                nullableBoolValue(),
//                ION.newBool(null)
//            ),
//            roundTrip(
//                nullableIntValue(),
//                ION.newInt(null)
//            ),
            roundTrip(
                nullableBagValue<PartiQLValue>(),
                ION.newNullList()
            )
        )
        @JvmStatic
        fun scalars() = listOf(
            roundTrip(
                boolValue(true),
                ION.newBool(true)
            ),
            roundTrip(
                boolValue(false),
                ION.newBool(false)
            ),
            oneWayTrip(
                int8Value(1),
                ION.newInt(1),
                intValue(BigInteger.ONE)
            ),
            oneWayTrip(
                int16Value(1),
                ION.newInt(1),
                intValue(BigInteger.ONE)
            ),
            oneWayTrip(
                int32Value(1),
                ION.newInt(1),
                intValue(BigInteger.ONE)
            ),
            oneWayTrip(
                int64Value(1),
                ION.newInt(1),
                intValue(BigInteger.ONE)
            ),
            roundTrip(
                intValue(BigInteger.valueOf(1)),
                ION.newInt(1),
            ),
            roundTrip(
                decimalValue(BigDecimal("123.456")),
                ION.newDecimal(BigDecimal("123.456")),
            ),
            roundTrip(
                decimalValue(BigDecimal("0.456")),
                ION.newDecimal(BigDecimal("0.456")),
            ),
            oneWayTrip(
                float32Value(123.0f),
                ION.newFloat(123.0f.toString().toDouble()),
                float64Value(123.0)
            ),
            oneWayTrip(
                float32Value(123f),
                ION.newFloat(123f.toString().toDouble()),
                float64Value(123.0)
            ),
            roundTrip(
                float64Value(123.0),
                ION.newFloat(123.0.toString().toDouble())
            ),
            roundTrip(
                float64Value(123.toDouble()),
                ION.newFloat(123),
            ),
            oneWayTrip(
                charValue('C'),
                ION.newString("C"),
                stringValue("C")
            ),
            roundTrip(
                stringValue("word"),
                ION.newString("word")
            ),
            roundTrip(
                stringValue("word\nword"),
                ION.newString("word\nword")
            ),
            roundTrip(
                symbolValue("x"),
                ION.newSymbol("x"),
            ),
            roundTrip(
                symbolValue("f.x"),
                ION.newSymbol("f.x"),
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
            roundTrip(
                bagValue(emptyList()),
                ION.newEmptyList().apply { addTypeAnnotation("\$bag") },
            ),
            roundTrip(
                listValue(emptyList()),
                ION.newEmptyList()
            ),
            roundTrip(
                sexpValue(emptyList()),
                ION.newEmptySexp()
            ),
            oneWayTrip(
                bagValue(
                    listOf(
                        int32Value(1),
                        int32Value(2),
                        int32Value(3),
                    )
                ),
                ION.newList(
                    ION.newInt(1),
                    ION.newInt(2),
                    ION.newInt(3)
                ).apply { addTypeAnnotation("\$bag") },
                bagValue(
                    listOf(
                        intValue(BigInteger.ONE),
                        intValue(BigInteger.valueOf(2L)),
                        intValue(BigInteger.valueOf(3L)),
                    )
                )
            ),
            roundTrip(
                listValue(
                    listOf(
                        stringValue("a"),
                        stringValue("b"),
                        stringValue("c"),
                    )
                ),
                ION.newList(
                    ION.newString("a"),
                    ION.newString("b"),
                    ION.newString("c")
                )
            ),
            oneWayTrip(
                sexpValue(
                    listOf(
                        int32Value(1),
                        int32Value(2),
                        int32Value(3),
                    )
                ),
                ION.newSexp(
                    ION.newInt(1),
                    ION.newInt(2),
                    ION.newInt(3)
                ),
                sexpValue(
                    listOf(
                        intValue(BigInteger.ONE),
                        intValue(BigInteger.valueOf(2L)),
                        intValue(BigInteger.valueOf(3L)),
                    )
                )
            ),
            oneWayTrip(
                structValue(
                    listOf(
                        "a" to int32Value(1),
                        "b" to stringValue("x"),
                    )
                ),
                ION.newEmptyStruct()
                    .apply {
                        add("a", ION.newInt(1))
                        add("b", ION.newString("x"))
                    },
                structValue(
                    listOf(
                        "a" to intValue(BigInteger.ONE),
                        "b" to stringValue("x"),
                    )
                ),
            )
        )

        @JvmStatic
        fun nestedCollections(): List<Case> = TODO()

        @JvmStatic
        fun annotations(): List<Case> = scalars().map { toAnnotated(it) } + collections().map { toAnnotated(it) }

        private fun toAnnotated(case: Case): Case =
            when (case) {
                is Case.OneWayTrip -> {
                    val partiQLValueBeforeAnnotation = case.partiQLValueBefore.annotations
                    val ionValueAnnotation = case.ionValueForPartiQL.typeAnnotations.toList()
                    val partiQLValueAfterAnnotation = case.partiQLValueAfter.annotations
                    Case.OneWayTrip(
                        case.partiQLValueBefore.withoutAnnotations().withAnnotations(annotations + partiQLValueBeforeAnnotation),
                        case.ionValueForPartiQL.toIonElement()
                            .withoutAnnotations()
                            .withAnnotations(annotations + ionValueAnnotation)
                            .toIonValue(ION),
                        case.partiQLValueAfter
                            .withoutAnnotations()
                            .withAnnotations(annotations + partiQLValueAfterAnnotation)
                    )
                }
                is Case.RoundTrip -> {
                    val partiQLValueBeforeAnnotation = case.partiQLValueBefore.annotations
                    val ionValueAnnotation = case.ionValue.typeAnnotations.toList()
                    Case.RoundTrip(
                        case.partiQLValueBefore.withoutAnnotations().withAnnotations(annotations + partiQLValueBeforeAnnotation),
                        case.ionValue.toIonElement()
                            .withoutAnnotations()
                            .withAnnotations(annotations + ionValueAnnotation)
                            .toIonValue(ION),
                    )
                }
            }
    }

    // This class test use [IonForPartiQL] as source format
    sealed class Case() {
        data class OneWayTrip(
            val partiQLValueBefore: PartiQLValue,
            val ionValueForPartiQL: IonValue,
            val partiQLValueAfter: PartiQLValue,
        ) : Case()
        data class RoundTrip(
            val partiQLValueBefore: PartiQLValue,
            val ionValue: IonValue,
        ) : Case()
    }

    // If the test is round trip enabled, then
    // 1. test partiql value before to ion binary/ ion text
    // 2. test ion to partiql value before
    private fun assert(tc: Case) {
        when (tc) {
            // if the test shall be one way trip
            // then we test on:
            // 1. PartiQL value before to Ion text/binary
            // 2. Ion to PartiQL Value after
            // 3. PartiQL Value after to ion
            //    -- No further divergent should appear.
            is Case.OneWayTrip -> {
                assertToIon(tc.partiQLValueBefore, tc.ionValueForPartiQL)
                assertRoundTrip(tc.partiQLValueAfter!!, tc.ionValueForPartiQL)
            }
            is Case.RoundTrip -> {
                assertRoundTrip(tc.partiQLValueBefore, tc.ionValue)
            }
        }
    }

    private fun assertRoundTrip(pValue: PartiQLValue, iValue: IonValue) {
        try {
            assertToIon(pValue, iValue)
        } catch (e: AssertionError) {
            val message = """
                        From PartiQL To Ion Failed: 
                        original PartiQL Value: $pValue,
                        ${e.message}
            """.trimIndent()
            throw AssertionError(message)
        }
        try {
            assertToPartiQL(iValue, pValue)
        } catch (e: AssertionError) {
            val message = """
                        From Ion To PartiQL Failed: 
                        original ion Value: $pValue,
                        ${e.message}
            """.trimIndent()
            throw AssertionError(message)
        }
    }

    private fun assertToPartiQL(iValue: IonValue, pValue: PartiQLValue) {
        val out = ByteArrayOutputStream()
        val reader = IonReaderBuilder.standard().build(iValue)
        val writer = IonTextWriterBuilder.standard().build(out)
        writer.writeValues(reader)
        val input = ByteArrayInputStream(out.toByteArray())
        val ionForPartiQL =
            PartiQLValueIonReaderBuilder
                .standard().build(input)
        val actual = ionForPartiQL.read()
        assertEquals(
            pValue, actual,
            """PartiQLValue Assertion Failed
                    | Expected : $pValue
                    | Actual: $actual
                """.trimMargin()
        )
    }

    private fun assertToIon(pValue: PartiQLValue, iValue: IonValue) {
        assertToIonBinary(pValue, iValue)
        assertToIonText(pValue, iValue)
    }

    private fun assertToIonText(pValue: PartiQLValue, iValue: IonValue) {
        val bufferForPartiQL = ByteArrayOutputStream()
        val textWriter = PartiQLValueIonWriterBuilder.standardIonTextBuilder().build(bufferForPartiQL)
        textWriter.append(pValue)
        val actual = bufferForPartiQL.toString()
        val bufferForIon = ByteArrayOutputStream()
        val ionTextWriter = IonTextWriterBuilder.standard().build(bufferForIon)
        ionTextWriter.writeValues(IonReaderBuilder.standard().build(iValue))
        PrintStream(bufferForPartiQL)
        val expected = bufferForIon.toString()
        assertEquals(
            expected, actual,
            """Ion Text assertion Failed:
                   | Expected : $expected,
                   | Actual : $actual
                """.trimMargin()
        )
    }

    private fun assertToIonBinary(pValue: PartiQLValue, iValue: IonValue) {
        val bufferForPartiQL = ByteArrayOutputStream()
        val binaryWriter = PartiQLValueIonWriterBuilder.standardIonBinaryBuilder().build(bufferForPartiQL)
        binaryWriter.append(pValue)
        val actual = bufferForPartiQL.toString()
        val bufferForIon = ByteArrayOutputStream()
        val ionBinaryWriter = IonBinaryWriterBuilder.standard().build(bufferForIon)
        ionBinaryWriter.writeValues(IonReaderBuilder.standard().build(iValue))
        val expected = bufferForIon.toString()
        assertEquals(
            expected, actual,
            """Ion Binary assertion Failed:
                    | Expected : $expected,
                    | Actual : $actual
                """.trimMargin()
        )
    }
}
