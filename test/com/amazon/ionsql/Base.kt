/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonValue
import com.amazon.ion.system.IonSystemBuilder
import org.junit.Assert
import java.util.*

open class Base : Assert() {
    val ion = IonSystemBuilder.standard().build()

    fun literal(text: String): IonValue = ion.singleValue(text)

    inner class AssertExprValue(val exprValue: ExprValue,
                                val bindingsTransform: Bindings.() -> Bindings = { this }) {
        fun assertBindings(predicate: Bindings.() -> Boolean) =
            assertTrue(
                exprValue.bind(Bindings.empty()).bindingsTransform().predicate()
            )

        fun assertBinding(name: String, predicate: ExprValue.() -> Boolean) = assertBindings {
            get(name)?.predicate() ?: false
        }

        fun assertNoBinding(name: String) = assertBindings { get(name) == null }

        fun assertIonValue(expected: IonValue) {
            assertEquals(expected, exprValue.ionValue)
        }

        fun assertLiteral(expected: String) = assertIonValue(literal(expected))

        fun assertIterator(expected: Collection<IonValue>) {
            val actual = ArrayList<IonValue>()
            exprValue.asSequence().map { it.ionValue }.toCollection(actual)
            assertEquals(expected, actual)
        }
    }
}