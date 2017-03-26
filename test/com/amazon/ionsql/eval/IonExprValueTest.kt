/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ion.IonContainer
import com.amazon.ion.IonValue
import com.amazon.ionsql.Base
import com.amazon.ionsql.util.get
import com.amazon.ionsql.util.intValue
import com.amazon.ionsql.util.stringValue
import org.junit.Test
import java.math.BigDecimal

class IonExprValueTest : Base() {
    fun over(expectedIonValue: IonValue,
             block: AssertExprValue.() -> Unit = { }) =
        AssertExprValue(IonExprValue(expectedIonValue))
            .apply {
                assertIonValue(expectedIonValue)

                when (expectedIonValue) {
                    is IonContainer -> expectedIonValue.map { it }
                    else -> assertIterator(emptyList())
                }
            }
            .run(block)

    fun over(text: String,
             transform: IonValue.() -> IonValue = { this },
             block: AssertExprValue.() -> Unit = { }) = over(
        literal(text).transform(),
        block
    )

    @Test
    fun scalarInt() = over("5") {
        assertNull(exprValue.asFacet(Named::class.java))
    }

    @Test
    fun scalarString() = over("\"hello\"") {
        assertNull(exprValue.asFacet(Named::class.java))
    }

    @Test
    fun list() = over("[1, 2, 3]") {
        assertNull(exprValue.asFacet(Named::class.java))
    }

    @Test
    fun struct() = over("{a: 1, b: 3.14, c: \"hello\"}") {
        assertBinding("a") { ion.newInt(1) == ionValue }
        assertBinding("b") { ion.newDecimal(BigDecimal("3.14")) == ionValue }
        assertBinding("c") { ion.newString("hello") == ionValue }
        assertNoBinding("d")
    }

    @Test
    fun listChild() = over("[1, 2, 3]", { this[0] }) {
        val named = exprValue.asFacet(Named::class.java)!!
        assertEquals(0, named.name.ionValue.intValue())
    }

    @Test
    fun structChild() = over("{a: 1, b: 2, c: 3}", { this["a"]!! }) {
        val named = exprValue.asFacet(Named::class.java)!!
        assertEquals("a", named.name.ionValue.stringValue())
    }
}
