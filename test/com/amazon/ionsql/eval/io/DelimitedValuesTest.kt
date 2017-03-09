/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval.io

import com.amazon.ionsql.Base
import com.amazon.ionsql.eval.ExprValue
import com.amazon.ionsql.eval.ExprValueType
import com.amazon.ionsql.eval.SequenceExprValue
import com.amazon.ionsql.eval.io.DelimitedValues.ConversionMode
import com.amazon.ionsql.eval.io.DelimitedValues.ConversionMode.*
import com.amazon.ionsql.eval.orderedNamesValue
import com.amazon.ionsql.util.exprValue
import org.junit.Test
import java.io.StringReader
import java.io.StringWriter

class DelimitedValuesTest : Base() {
    private fun assertValues(expectedIon: String, value: ExprValue) {
        val expectedValues = literal(expectedIon)

        assertSame(ExprValueType.BAG, value.type)
        assertEquals(expectedValues, value.ionValue)
        try {
            value.iterator()
            fail("Expected single pass sequence")
        } catch (e: IllegalStateException) {}
    }

    private fun read(text: String,
                     delimiter: String,
                     hasHeader: Boolean,
                     conversionMode: ConversionMode): ExprValue =
        DelimitedValues.exprValue(ion, StringReader(text), delimiter, hasHeader, conversionMode)

    private fun voidRead(text: String,
                         delimiter: String,
                         hasHeader: Boolean,
                         conversionMode: ConversionMode): Unit {
        read(text, delimiter, hasHeader, conversionMode)
    }

    private fun assertWrite(expectedText: String,
                            valueText: String,
                            names: List<String>,
                            writeHeader: Boolean,
                            delimiter: String = ",",
                            newline: String = "\n") {
        val actualText = StringWriter().use {
            val exprValue = SequenceExprValue(
                ion,
                literal(valueText).exprValue()
                    .asSequence()
                    // apply the "schema"
                    .map { it.orderedNamesValue(names) }
            )
            DelimitedValues.writeTo(ion, it, exprValue, delimiter, newline, writeHeader)

            it.toString()
        }
        assertEquals(expectedText, actualText)
    }

    private fun voidWrite(exprValue: ExprValue,
                          writeHeader: Boolean,
                          delimiter: String = ",",
                          newline: String = "\n") {
        DelimitedValues.writeTo(ion, StringWriter(), exprValue, delimiter, newline, writeHeader)
    }

    @Test
    fun emptyExprValueCommaNoAutoNoHeader() = assertValues(
        """[]""",
        read(
            "",
            delimiter = ",",
            hasHeader = false,
            conversionMode = NONE
        )
    )

    @Test
    fun emptyExprValueTabAutoNoHeader() = assertValues(
        """[]""",
        read(
            "",
            delimiter = ",\t",
            hasHeader = false,
            conversionMode = AUTO
        )
    )

    @Test(expected = IllegalArgumentException::class)
    fun emptyExprValueTabAutoHeader() = voidRead(
        "",
        delimiter = ",\t",
        hasHeader = true,
        conversionMode = AUTO
    )

    @Test
    fun singleExprValueCommaNoAutoNoHeader() = assertValues(
        """[{_0: "1", _1: "2", _2: "3"}]""",
        read(
            """1,2,3""",
            delimiter = ",",
            hasHeader = false,
            conversionMode = NONE
        )
    )

    @Test
    fun multiExprValueCommaAutoNoHeader() = assertValues(
        """[
          {_0: 1, _1: 2, _2: 3},
          {_0: 1.0, _1: 2e0, _2: 2007-10-10T12:00:00Z},
          {_0: "hello", _1: "{", _2: "}"},
        ]""",
        read(
            """
            |1,2,3
            |1.0,2e0,2007-10-10T12:00:00Z
            |hello,{,}
            """.trimMargin(),
            delimiter = ",",
            hasHeader = false,
            conversionMode = AUTO
        )
    )

    @Test
    fun multiExprValueCommaAutoHeader() = assertValues(
        """[
          {a: 1, b: 2, _2: 3},
          {a: 1.0, b: 2e0, _2: 2007-10-10T12:00:00Z},
          {a: "hello", b: "{", _2: "}"},
        ]""",
        read(
            """
            |a,b
            |1,2,3
            |1.0,2e0,2007-10-10T12:00:00Z
            |hello,{,}
            """.trimMargin(),
            delimiter = ",",
            hasHeader = true,
            conversionMode = AUTO
        )
    )

    @Test
    fun writeEmpty() = assertWrite(
        "",
        "[]",
        emptyList(),
        writeHeader = false
    )

    @Test
    fun writeNoHeader() = assertWrite(
        """
        |1,2,3
        |null,2e0,3.0
        |moo,cow,2012-10-10
        |4,true,null
        |null,null,null
        """.trimMargin() + "\n",
        """[
            {a: 1, b: 2, c: 3},
            {b: 2e0, c: 3.0},
            {c: 2012-10-10, a: moo, b: "cow"},
            {c: null, a: 4, b: true},
            {},
        ]""",
        listOf("a", "b", "c"),
        writeHeader = false
    )

    @Test
    fun writeHeader() = assertWrite(
        """
        |a,b,c
        |1,2,3
        |null,2e0,3.0
        """.trimMargin() + "\n",
        """[
            {a: 1, b: 2, c: 3},
            {b: 2e0, c: 3.0},
        ]""",
        listOf("a", "b", "c"),
        writeHeader = true
    )

    @Test(expected = IllegalArgumentException::class)
    fun mismatchSchema() = voidWrite(
        SequenceExprValue(
            ion,
            literal("[{a:1}, {b:2}]").exprValue()
                .asSequence()
                .mapIndexed { index, exprValue ->
                    when (index) {
                        0 -> exprValue.orderedNamesValue(listOf("a"))
                        else -> exprValue.orderedNamesValue(listOf("b"))
                    }
                }
        ),
        writeHeader = false
    )

    @Test(expected = IllegalArgumentException::class)
    fun noSchema() = voidWrite(
        literal("[{a:1}, {b:2}]").exprValue(),
        writeHeader = false
    )
}
