/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.*
import org.junit.Test

class IonExprValueTest : Base() {
    fun over(expectedIonValue: IonValue,
             name: Any?,
             block: AssertExprValue.() -> Unit = { }) =
        AssertExprValue(IonExprValue(expectedIonValue))
            .apply {
                assertIonValue(expectedIonValue)
                when (name) {
                    null -> assertNoBinding(SYS_NAME)
                    else -> assertBinding(SYS_NAME) {
                        val nameIonVal = when (name) {
                            is String -> ion.newString(name)
                            is Int -> ion.newInt(name)
                            else -> throw IllegalArgumentException("Unsupported name value: $name")
                        }.seal()

                        nameIonVal == ionValue
                    }
                }
                assertBinding(SYS_VALUE) { expectedIonValue == ionValue }
                assertBinding(SYS_VALUE) { exprValue.ionValue === ionValue }

                when (expectedIonValue) {
                    is IonContainer -> expectedIonValue.map { it }
                    else -> assertIterator(listOf(expectedIonValue))
                }
            }
            .run(block)

    fun over(text: String, block: AssertExprValue.() -> Unit = { }) = over(
        literal(text),
        null,
        block
    )

    @Test
    fun scalarInt() = over("5")

    @Test
    fun scalarString() = over("\"hello\"")

    @Test
    fun list() = over("[1, 2, 3]")

}