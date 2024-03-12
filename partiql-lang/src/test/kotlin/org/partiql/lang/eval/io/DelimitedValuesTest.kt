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

package org.partiql.lang.eval.io

import com.amazon.ion.IonType
import com.amazon.ion.IonValue
import org.apache.commons.csv.CSVFormat
import org.junit.Test
import org.partiql.eval.TestBase
import org.partiql.eval.util.newFromIonText
import org.partiql.lang.eval.BAG_ANNOTATION
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.MISSING_ANNOTATION
import org.partiql.lang.eval.io.DelimitedValues.ConversionMode.AUTO
import org.partiql.lang.eval.io.DelimitedValues.ConversionMode.NONE
import org.partiql.lang.eval.orderedNamesValue
import org.partiql.lang.eval.toIonValue
import org.partiql.lang.util.asSequence
import java.io.StringReader
import java.io.StringWriter

class DelimitedValuesTest : TestBase() {
    private fun assertValues(expectedIon: String, value: ExprValue) {
        val expectedValues = ion.singleValue(expectedIon)

        assertSame(ExprValueType.BAG, value.type)
        assertEquals(expectedValues, value.toIonValue(ion).cloneAndRemoveBagAndMissingAnnotations())
    }

    private fun read(
        text: String,
        csvFormat: CSVFormat,
        conversionMode: DelimitedValues.ConversionMode
    ): ExprValue =
        DelimitedValues.exprValue(StringReader(text), csvFormat, conversionMode)

    private fun assertWrite(
        expectedText: String,
        valueText: String,
        names: List<String>,
        writeHeader: Boolean,
        delimiter: Char = ',',
        newline: String = "\n"
    ) {
        val actualText = StringWriter().use {

            val rowExprValue = newFromIonText(valueText)
            val exprValue = ExprValue.newBag(
                rowExprValue.asSequence().map {
                    // apply the "schema"
                    it.orderedNamesValue(names)
                }
            )

//            val exprValue = SequenceExprValue(
//                ion,
//                valueFactory.newFromIonValue(literal(valueText))
//                    .asSequence()
//                    // apply the "schema"
//                    .map { it.orderedNamesValue(names) }

            DelimitedValues.writeTo(ion, it, exprValue, delimiter, newline, writeHeader)

            it.toString()
        }
        assertEquals(expectedText, actualText)
    }

    private fun voidWrite(
        exprValue: ExprValue,
        writeHeader: Boolean,
        delimiter: Char = ',',
        newline: String = "\n"
    ) {
        DelimitedValues.writeTo(ion, StringWriter(), exprValue, delimiter, newline, writeHeader)
    }

    @Test
    fun emptyExprValueCommaNoAutoNoHeader() = assertValues(
        """[]""",
        read(
            "",
            CSVFormat.DEFAULT,
            conversionMode = NONE
        )
    )

    @Test
    fun emptyExprValueTabAutoNoHeader() = assertValues(
        """[]""",
        read(
            "",
            CSVFormat.DEFAULT,
            conversionMode = AUTO
        )
    )

    @Test
    fun singleExprValueCommaNoAutoNoHeader() = assertValues(
        """[{_1: "1", _2: "2", _3: "3"}]""",
        read(
            """1,2,3""",
            CSVFormat.DEFAULT,
            conversionMode = NONE
        )
    )

    @Test
    fun multiExprValueCommaAutoNoHeader() = assertValues(
        """[
          {_1: 1, _2: 2, _3: 3},
          {_1: 1.0, _2: 2e0, _3: 2007-10-10T12:00:00Z},
          {_1: "hello", _2: "{", _3: "}"},
        ]""",
        read(
            """
            |1,2,3
            |1.0,2e0,2007-10-10T12:00:00Z
            |hello,{,}
            """.trimMargin(),
            CSVFormat.DEFAULT,
            conversionMode = AUTO
        )
    )

    @Test
    fun multiExprValueCommaAutoHeader() = assertValues(
        """[
          {a: 1, b: 2, _3: 3},
          {a: 1.0, b: 2e0, _3: 2007-10-10T12:00:00Z},
          {a: "hello", b: "{", _3: "}"},
        ]""",
        read(
            """
            |a,b
            |1,2,3
            |1.0,2e0,2007-10-10T12:00:00Z
            |hello,{,}
            """.trimMargin(),
            CSVFormat.DEFAULT.withFirstRecordAsHeader(),
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
        ExprValue.newBag(
            newFromIonText("[{a:1}, {b:2}]")
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
        newFromIonText("[{a:1}, {b:2}]"),
        writeHeader = false
    )
}

internal fun IonValue.removeBagAndMissingAnnotations() {
    when (this.type) {
        // Remove $missing annotation from NULL for assertions
        IonType.NULL -> this.removeTypeAnnotation(MISSING_ANNOTATION)
        // Recurse into all container types.
        IonType.DATAGRAM, IonType.SEXP, IonType.STRUCT, IonType.LIST -> {
            // Remove $bag annotation from LIST for assertions
            if (this.type == IonType.LIST) {
                this.removeTypeAnnotation(BAG_ANNOTATION)
            }
            // Recursively remove annotations
            this.asSequence().forEach {
                it.removeBagAndMissingAnnotations()
            }
        }
        else -> { /* ok to do nothing. */ }
    }
}

/**
 * Clones and removes $bag and $missing annotations from the clone and any child values.
 *
 * There are many tests which were created before these annotations were present and thus do not include them
 * in their expected values.  This function provides an alternative to having to go and update all of them.
 * This is tech debt of the unhappy variety:  all of those test cases should really be updated and this function
 * should be deleted.
 *
 * NOTE: this function does not remove $date annotations ever!  There are tests that depend on this too.
 * $date however, was added AFTER this function was created, and so no test cases needed to remove that
 * annotation.
 */
internal fun IonValue.cloneAndRemoveBagAndMissingAnnotations() = this.clone().apply {
    removeBagAndMissingAnnotations()
    makeReadOnly()
}
