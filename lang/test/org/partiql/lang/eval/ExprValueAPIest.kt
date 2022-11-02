/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval

import com.amazon.ion.IonTimestamp
import com.amazon.ion.IonValue
import com.amazon.ion.Timestamp
import com.amazon.ion.system.IonSystemBuilder
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.partiql.lang.ION
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.time.Time
import org.partiql.lang.util.isBag
import org.partiql.lang.util.isMissing
import org.partiql.lang.util.seal
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

@RunWith(JUnitParamsRunner::class)
class ExprValueAPIest {
    companion object {
        /**
         * We need to store the IonSystem and ExprValueFactory in the companion object to ensure everything uses the
         * same IonSystem because JUnitParams creates and instance of the test fixture just to invoke
         * [parametersForExprValueFactoryTest] and another instance to actually run the tests.  Keeping this in an
         * instance field would result in multiple instances of IonSystem being used.
         */
        private val ion = IonSystemBuilder.standard().build()
        private val someTestBytes = "some test bytes".toByteArray(Charsets.UTF_8)
    }

    data class TestCase(val expectedType: ExprValueType, val expectedValue: Any?, val expectedIonValue: IonValue, val value: ExprValue)

    fun parametersForExprValueFactoryTest(): List<TestCase> {
        val localTime = LocalTime.of(17, 40, 1, 123456789)
        val localDate = LocalDate.of(2022, 1, 1)
        val time = Time.of(localTime, 9, ZoneOffset.ofHoursMinutes(1, 5))
        return listOf(
            TestCase(ExprValueType.BOOL, true, ion.newBool(true), boolExprValue(true)),
            TestCase(ExprValueType.BOOL, false, ion.newBool(false), boolExprValue(false)),
            TestCase(ExprValueType.INT, 100L, ion.newInt(100), intExprValue(100)), // <--Int converted to Long
            TestCase(ExprValueType.INT, 101L, ion.newInt(101), intExprValue(101L)),
            TestCase(ExprValueType.FLOAT, 103.0, ion.newFloat(103.0), floatExprValue(103.0)),
            TestCase(ExprValueType.DECIMAL, BigDecimal(104), ion.newDecimal(BigDecimal(104)), decimalExprValue(104)),
            TestCase(ExprValueType.DECIMAL, BigDecimal(105), ion.newDecimal(BigDecimal(105)), decimalExprValue(105L)),
            TestCase(ExprValueType.DECIMAL, BigDecimal(106), ion.newDecimal(BigDecimal(106)), decimalExprValue(BigDecimal(106))),
            TestCase(ExprValueType.STRING, "107", ion.newString("107"), stringExprValue("107")),
            TestCase(ExprValueType.STRING, "", ion.newString(""), stringExprValue("")),
            TestCase(ExprValueType.SYMBOL, "108", ion.newSymbol("108"), symbolExprValue("108")),
            TestCase(ExprValueType.SYMBOL, "", ion.newSymbol(""), symbolExprValue("")),
            TestCase(ExprValueType.CLOB, someTestBytes, ion.newClob(someTestBytes), clobExprValue(someTestBytes)),
            TestCase(ExprValueType.BLOB, someTestBytes, ion.newBlob(someTestBytes), blobExprValue(someTestBytes)),
            TestCase(ExprValueType.DATE, localDate, ion.singleValue("\$partiql_date::2022-01-01"), dateExprValue(localDate)),
            TestCase(ExprValueType.TIME, time, ion.singleValue("\$partiql_time::{hour:17,minute:40,second:1.123456789,timezone_hour:1,timezone_minute:5}"), timeExprValue(time))
        )
    }

    @Test
    @Parameters
    fun exprValueFactoryTest(tc: TestCase) {
        val expectedValue = tc.expectedValue

        // None of these values should be named
        assertNull(tc.value.name)

        // None of these values should have any child values.
        assertTrue(tc.value.none())

        // The IonValue should match the expected value
        assertEquals(tc.expectedIonValue, tc.value.toIonValue(ION))

        // An ExprValue created from tc.expectedIonValue must be equivalent to tc.value
        val exprValueFromExpectedIonValue = tc.expectedIonValue.toExprValue()
        assertEquals(0, DEFAULT_COMPARATOR.compare(exprValueFromExpectedIonValue, tc.value))

        // Converting to Ion and back again should yield an equivalent value
        assertEquivalentAfterConversionToIon(tc.value)

        when (tc.expectedType) {
            ExprValueType.MISSING,
            ExprValueType.NULL,
            ExprValueType.STRUCT,
            ExprValueType.SEXP,
            ExprValueType.LIST,
            ExprValueType.BAG -> {
                assertScalarEmpty(tc.value)
            }
            ExprValueType.BOOL -> {
                assertEquals(expectedValue, tc.value.scalar.booleanValue())
                assertNull(tc.value.scalar.numberValue())
                assertNull(tc.value.scalar.stringValue())
                assertNull(tc.value.scalar.bytesValue())
                assertNull(tc.value.scalar.timestampValue())
            }
            ExprValueType.INT -> {
                assertNull(tc.value.scalar.booleanValue())
                assertEquals(expectedValue, tc.value.scalar.numberValue())
                assertNull(tc.value.scalar.stringValue())
                assertNull(tc.value.scalar.bytesValue())
                assertNull(tc.value.scalar.timestampValue())
            }
            ExprValueType.FLOAT -> {
                assertNull(tc.value.scalar.booleanValue())
                assertEquals(expectedValue, tc.value.scalar.numberValue())
                assertNull(tc.value.scalar.stringValue())
                assertNull(tc.value.scalar.bytesValue())
                assertNull(tc.value.scalar.timestampValue())
            }
            ExprValueType.DECIMAL -> {
                assertNull(tc.value.scalar.booleanValue())
                assertEquals(expectedValue, tc.value.scalar.numberValue())
                assertNull(tc.value.scalar.stringValue())
                assertNull(tc.value.scalar.bytesValue())
                assertNull(tc.value.scalar.timestampValue())
            }
            ExprValueType.TIMESTAMP -> {
                assertNull(tc.value.scalar.booleanValue())
                assertNull(tc.value.scalar.numberValue())
                assertNull(tc.value.scalar.stringValue())
                assertNull(tc.value.scalar.bytesValue())
                assertEquals(expectedValue, tc.value.scalar.timestampValue())
            }
            ExprValueType.SYMBOL -> {
                assertNull(tc.value.scalar.booleanValue())
                assertNull(tc.value.scalar.numberValue())
                assertEquals(expectedValue, tc.value.scalar.stringValue())
                assertNull(tc.value.scalar.bytesValue())
                assertNull(tc.value.scalar.timestampValue())
            }
            ExprValueType.STRING -> {
                assertNull(tc.value.scalar.booleanValue())
                assertNull(tc.value.scalar.numberValue())
                assertEquals(expectedValue, tc.value.scalar.stringValue())
                assertNull(tc.value.scalar.bytesValue())
                assertNull(tc.value.scalar.timestampValue())
            }
            ExprValueType.CLOB -> {
                assertNull(tc.value.scalar.booleanValue())
                assertNull(tc.value.scalar.numberValue())
                assertNull(tc.value.scalar.stringValue())
                assertEquals(expectedValue, tc.value.scalar.bytesValue())
                assertNull(tc.value.scalar.timestampValue())
            }
            ExprValueType.BLOB -> {
                assertNull(tc.value.scalar.booleanValue())
                assertNull(tc.value.scalar.numberValue())
                assertNull(tc.value.scalar.stringValue())
                assertEquals(expectedValue, tc.value.scalar.bytesValue())
                assertNull(tc.value.scalar.timestampValue())
            }
            ExprValueType.DATE -> {
                assertNull(tc.value.scalar.booleanValue())
                assertNull(tc.value.scalar.numberValue())
                assertNull(tc.value.scalar.stringValue())
                assertNull(tc.value.scalar.bytesValue())
                assertNull(tc.value.scalar.timestampValue())
                assertEquals(expectedValue, tc.value.dateValue())
            }
            ExprValueType.TIME -> {
                assertNull(tc.value.scalar.booleanValue())
                assertNull(tc.value.scalar.numberValue())
                assertNull(tc.value.scalar.stringValue())
                assertNull(tc.value.scalar.bytesValue())
                assertNull(tc.value.scalar.timestampValue())
                assertEquals(expectedValue, tc.value.timeValue())
            }
            else -> fail("Unexpected ExprValueType: ${tc.expectedType}")
        }
    }

    private fun assertEquivalentAfterConversionToIon(value: ExprValue) {
        val reconstitutedValue = value.toIonValue(ION).toExprValue()
        assertEquals(0, DEFAULT_COMPARATOR.compare(value, reconstitutedValue))
    }

    private fun assertScalarEmpty(ev: ExprValue) {
        assertNull(ev.scalar.booleanValue())
        assertNull(ev.scalar.numberValue())
        assertNull(ev.scalar.stringValue())
        assertNull(ev.scalar.bytesValue())
        assertNull(ev.scalar.timestampValue())
    }

    @Test
    fun emptyBag() {
        assertEquals(ExprValueType.BAG, emptyBagExprValue().type)
        assertTrue(emptyBagExprValue().none())
        assertEquals(ion.singleValue("$BAG_ANNOTATION::[]"), emptyBagExprValue().toIonValue(ION))
    }

    @Test
    fun emptyList() {
        assertEquals(ExprValueType.LIST, emptyListExprValue().type)
        assertTrue(emptyListExprValue().none())
        assertEquals(ion.singleValue("[]"), emptyListExprValue().toIonValue(ION))
        assertEquivalentAfterConversionToIon(emptyListExprValue())
    }

    @Test
    fun emptySexp() {
        assertEquals(ExprValueType.SEXP, emptySexpExprValue().type)
        assertTrue(emptySexpExprValue().none())
        assertEquivalentAfterConversionToIon(emptySexpExprValue())
    }

    @Test
    fun emptyStruct() {
        assertEquals(ExprValueType.STRUCT, emptyStructExprValue().type)
        assertTrue(emptyBagExprValue().none())
        assertEquivalentAfterConversionToIon(emptyStructExprValue())
    }

    private val testList = listOf(1L, 2L, 3L)
    private val testListExprValues = testList.map { intExprValue(it) }
    private val ionList = ion.singleValue("[1,2,3]")
    private val ionSexp = ion.singleValue("(1 2 3)")
    private val testBag = "$BAG_ANNOTATION::[1,2,3]"
    private val bagFromSequence = bagExprValue(testListExprValues.asSequence())
    private val bagFromList = bagExprValue(testListExprValues)

    fun parametersForNonEmptyContainers() = listOf(
        TestCase(ExprValueType.LIST, null, ionList, listExprValue(testListExprValues.asSequence())),
        TestCase(ExprValueType.LIST, null, ionList, listExprValue(testListExprValues)),
        TestCase(ExprValueType.SEXP, null, ionSexp, sexpExprValue(testListExprValues.asSequence())),
        TestCase(ExprValueType.SEXP, null, ionSexp, sexpExprValue(testListExprValues)),
        TestCase(ExprValueType.BAG, null, ion.singleValue(testBag), bagFromSequence),
        TestCase(ExprValueType.BAG, null, ion.singleValue(testBag), bagFromList)
    )

    @Test
    @Parameters
    fun nonEmptyContainers(tc: TestCase) {
        when (tc.expectedType) {
            ExprValueType.BAG -> {
                assertEquals(ExprValueType.BAG, tc.value.type)
                assertBagValues(tc.value)
                assertTrue(tc.value.toIonValue(ION).isBag)
                assertEquals(tc.expectedIonValue, tc.value.toIonValue(ION))

                val fromIonValue = tc.value.toIonValue(ION).toExprValue()
                assertEquals(ExprValueType.BAG, fromIonValue.type) // Ion has no bag type--[bag.ionVaule] converts to a list with annotation $partiql_bag
                assertBagValues(fromIonValue)
                assertEquals(fromIonValue.toIonValue(ION), tc.value.toIonValue(ION))

                assertTrue(fromIonValue.toIonValue(ION).isBag, "The ion value should be ionList with annotation $BAG_ANNOTATION")
                assertEquals(1, fromIonValue.toIonValue(ION).typeAnnotations.count { it == BAG_ANNOTATION })
            }
            ExprValueType.LIST -> {
                assertEquals(ExprValueType.LIST, tc.value.type)
                assertOrderedContainer(tc.value)

                assertEquals(tc.expectedIonValue, tc.value.toIonValue(ION))
                assertEquivalentAfterConversionToIon(tc.value)
            }
            ExprValueType.SEXP -> {
                assertEquals(ExprValueType.SEXP, tc.value.type)
                assertOrderedContainer(tc.value)

                assertEquals(tc.expectedIonValue, tc.value.toIonValue(ION))
                assertEquivalentAfterConversionToIon(tc.value)
            }
            else -> fail("Unexpected ExprValueType: ${tc.expectedType}")
        }
    }

    private fun assertBagValues(bagExprValue: ExprValue) {
        assertScalarEmpty(bagExprValue)
        val contents = bagExprValue.toList()
        assertEquals(3, contents.size)

        assertTrue(contents.any { it.scalar.numberValue() == 1L })
        assertTrue(contents.any { it.scalar.numberValue() == 2L })
        assertTrue(contents.any { it.scalar.numberValue() == 3L })
    }

    private fun assertOrderedContainer(container: ExprValue) {
        assertScalarEmpty(container)
        val contents = container.toList()
        assertEquals(3, contents.size)

        testList.forEachIndexed { i, v ->
            assertEquals(v, contents[i].numberValue())
        }
    }

    fun nonEmptyUnorderedStructs(): Array<ExprValue> {
        val list = listOf(
            intExprValue(1).namedValue(symbolExprValue("foo")),
            intExprValue(2).namedValue(symbolExprValue("bar")),
            intExprValue(3).namedValue(symbolExprValue("bat"))
        )

        return arrayOf(
            structExprValue(list.asSequence(), StructOrdering.UNORDERED),
            structExprValue(list, StructOrdering.UNORDERED)
        )
    }

    @Test
    @Parameters(method = "nonEmptyUnorderedStructs")
    fun nonEmptyUnorderedStruct(struct: ExprValue) {
        assertUnorderderedStructValues(struct)
        assertUnorderderedStructValues(struct.toIonValue(ION).toExprValue())
        assertEquivalentAfterConversionToIon(struct)
    }

    private fun assertUnorderderedStructValues(struct: ExprValue) {
        val contents = struct.toList()

        assertEquals(3, contents.size)

        assertEquals(1L, contents.single { it.name!!.stringValue() == "foo" }.numberValue())
        assertEquals(2L, contents.single { it.name!!.stringValue() == "bar" }.numberValue())
        assertEquals(3L, contents.single { it.name!!.stringValue() == "bat" }.numberValue())
    }

    fun nonEmptyOrderedStructs(): Array<ExprValue> {
        val list = listOf(
            intExprValue(1).namedValue(symbolExprValue("foo")),
            intExprValue(2).namedValue(symbolExprValue("bar")),
            intExprValue(3).namedValue(symbolExprValue("bat"))
        )

        return arrayOf(
            structExprValue(list.asSequence(), StructOrdering.ORDERED),
            structExprValue(list, StructOrdering.ORDERED)
        )
    }

    @Test
    @Parameters(method = "nonEmptyOrderedStructs")
    fun nonEmptyOrderedStruct(struct: ExprValue) {
        assertOrderedStructValues(struct)
        assertOrderedStructValues(struct.toIonValue(ION).toExprValue())
        assertEquivalentAfterConversionToIon(struct)
    }

    private fun assertOrderedStructValues(struct: ExprValue) {
        val contents = struct.toList()

        assertEquals(3, contents.size)
        assertEquals("foo", contents[0].name!!.stringValue())
        assertEquals("bar", contents[1].name!!.stringValue())
        assertEquals("bat", contents[2].name!!.stringValue())

        assertEquals(1L, contents[0].scalar.numberValue())
        assertEquals(2L, contents[1].scalar.numberValue())
        assertEquals(3L, contents[2].scalar.numberValue())
    }

    @Test
    fun serializeDeserializeMissing() {
        // Deserialize - IonValue to ExprValue using newFromIonValue
        val ionValue = ion.newNull().also { it.addTypeAnnotation(MISSING_ANNOTATION) }
        val exprValue = ionValue.toExprValue()
        assertEquals(ExprValueType.MISSING, exprValue.type)
        assertEquals(exprValue.toIonValue(ION), ionValue)

        // Deserialize - IonValue to ExprValue using factory's missing value
        val exprValueFromFactory = missingExprValue()
        assertEquals(ExprValueType.MISSING, exprValueFromFactory.type)

        // Serialize - ExprValue to IonValue using ionValue by lazy
        val missingIonValue = missingExprValue().toIonValue(ION)
        assertTrue(missingIonValue.isMissing, "The ion value should be ionNull with annotation $MISSING_ANNOTATION")

        // Ensure round trip doesn't add the annotation if it already has $partiql_missing annotation
        val roundTrippedMissingExprValue = exprValueFromFactory.toIonValue(ION).toExprValue()
        assertTrue(roundTrippedMissingExprValue.toIonValue(ION).isMissing, "The ion value should be ionNull with annotation $MISSING_ANNOTATION")
        assertEquals(1, roundTrippedMissingExprValue.toIonValue(ION).typeAnnotations.size)
        assertEquals(MISSING_ANNOTATION, roundTrippedMissingExprValue.toIonValue(ION).typeAnnotations[0])
    }

    @Test
    fun serializeDeserializeBag() {
        // Deserialize - IonValue to ExprValue using newFromIonValue
        val ionValue = ion.newList(ion.newInt(1), ion.newInt(2), ion.newInt(3)).also { it.addTypeAnnotation(BAG_ANNOTATION) }
        val exprValue = ionValue.toExprValue()
        assertEquals(ExprValueType.BAG, exprValue.type)
        assertEquals(exprValue.toIonValue(ION), ionValue)

        // Deserialize - IonValue to ExprValue using newBag, newBag adds $partiql_bag annotation to the list
        val exprValueFromFactory = bagExprValue(listOf(intExprValue(1), intExprValue(2), intExprValue(3)).asSequence())
        assertEquals(ExprValueType.BAG, exprValueFromFactory.type)

        // Serialize - ExprValue to IonValue using ionValue by lazy
        assertTrue(exprValueFromFactory.toIonValue(ION).isBag)

        // Ensure round trip doesn't add the annotation if it already has $partiql_bag annotation
        val roundTrippedBagExprValue = exprValueFromFactory.toIonValue(ION).toExprValue()
        assertTrue(roundTrippedBagExprValue.toIonValue(ION).isBag, "The ion value should be ionList with annotation $BAG_ANNOTATION")
        assertEquals(1, roundTrippedBagExprValue.toIonValue(ION).typeAnnotations.size)
        assertEquals(BAG_ANNOTATION, roundTrippedBagExprValue.toIonValue(ION).typeAnnotations[0])
    }

    @Test
    fun dateExprValueTest() {
        val date = LocalDate.of(2020, 2, 29)

        val ionDate =
            ion.newTimestamp(Timestamp.forDay(date.year, date.monthValue, date.dayOfMonth)).apply {
                addTypeAnnotation("\$partiql_date")
            }.seal()

        val dateExprValue = dateExprValue(date)
        val dateIonValue = dateExprValue.toIonValue(ION)
        assertEquals(ionDate, dateIonValue, "Expected ionValues to be equal.")
        dateIonValue as IonTimestamp
        val timestamp = dateIonValue.timestampValue()
        Assert.assertEquals("Expected year to be 2020", 2020, timestamp.year)
        Assert.assertEquals("Expected month to be 02", 2, timestamp.month)
        Assert.assertEquals("Expected day to be 29", 29, timestamp.day)
    }

    @Test
    fun genericTimeExprValueTest() {
        val timeExprValue = timeExprValue(Time.of(23, 2, 29, 23, 2))
        assertEquals(
            expected = LocalTime.of(23, 2, 29),
            actual = timeExprValue.scalar.timeValue()!!.localTime,
            message = "Expected values to be equal."
        )
    }

    @Test
    fun genericTimeExprValueTest2() {
        val timeExprValue = timeExprValue(Time.of(23, 2, 29, 23, 2, -720))
        assertEquals(
            expected = OffsetTime.of(23, 2, 29, 0, ZoneOffset.ofTotalSeconds(-720 * 60)),
            actual = timeExprValue.scalar.timeValue()!!.offsetTime,
            message = "Expected values to be equal."
        )
    }

    @Test
    fun negativePrecisionForTime() {
        try {
            Time.of(23, 12, 34, 344423, -1, 300)
            Assert.fail("Expected evaluation error")
        } catch (e: EvaluationException) {
            Assert.assertEquals(ErrorCode.EVALUATOR_INVALID_PRECISION_FOR_TIME, e.errorCode)
        }
    }

    @Test
    fun outOfRangePrecisionForTime() {
        try {
            Time.of(23, 12, 34, 344423, 10, 300)
            Assert.fail("Expected evaluation error")
        } catch (e: EvaluationException) {
            Assert.assertEquals(ErrorCode.EVALUATOR_INVALID_PRECISION_FOR_TIME, e.errorCode)
        }
    }

    @Test
    fun testIonDate() {
        // Arrange
        val ionValueString = "\$partiql_date::2022-01-01"
        val ionValue = ion.singleValue(ionValueString)
        val expected = LocalDate.of(2022, 1, 1)

        // Act
        val exprValue = ionValue.toExprValue()

        // Assert
        assertEquals(expected, exprValue.dateValue())
    }

    @Test
    fun testIonTime() {
        // Arrange
        val ionValueString = "\$partiql_time::{hour:0,minute:40,second:1.123456789,timezone_hour:1,timezone_minute:5}"
        val ionValue = ion.singleValue(ionValueString)
        val expected = Time.of(0, 40, 1, 123456789, 9, 65)

        // Act
        val exprValue = ionValue.toExprValue()

        // Assert
        assertEquals(expected, exprValue.timeValue())
    }
}
