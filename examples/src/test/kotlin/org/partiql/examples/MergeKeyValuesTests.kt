/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.partiql.examples

import com.amazon.ion.system.IonSystemBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.StructOrdering
import org.partiql.lang.eval.namedValue

class MergeKeyValuesTests {
    private val ion = IonSystemBuilder.standard().build()
    private val session = EvaluationSession.standard()

    @Test
    fun testFunction() {
        val fn = MergeKeyValues()

        val ionValue1 = ion.newList(ion.newString("abc"), ion.newString("cde"))
        val ionValue2 = ion.newList(ion.newString("ghj"), ion.newString("klu"))
        val ionValue3 = ion.newList(ion.newString("ghj"), ion.newString("klu"))

        val list1 = listOf(
            ExprValue.newString("certificate").namedValue(ExprValue.newSymbol("Name")),
            ExprValue.of(ionValue1).namedValue(ExprValue.newSymbol("Values")),
        )
        val list2 = listOf(
            ExprValue.newString("certificate").namedValue(ExprValue.newSymbol("Name")),
            ExprValue.of(ionValue2).namedValue(ExprValue.newSymbol("Values")),
        )
        val list3 = listOf(
            ExprValue.newString("test").namedValue(ExprValue.newSymbol("Name")),
            ExprValue.of(ionValue3).namedValue(ExprValue.newSymbol("Values")),
        )
        val res1 = fn.callWithRequired(
            session,
            listOf(
                ExprValue.newBag(
                    listOf(
                        ExprValue.newStruct(list1.asSequence(), StructOrdering.UNORDERED),
                        ExprValue.newStruct(list2.asSequence(), StructOrdering.UNORDERED),
                        ExprValue.newStruct(list3.asSequence(), StructOrdering.UNORDERED)
                    )
                ),
                ExprValue.newString("Name"),
                ExprValue.newString("Values")
            )
        )

        val res2 = fn.callWithRequired(
            session,
            listOf(
                ExprValue.newSexp(
                    listOf(
                        ExprValue.newStruct(list1.asSequence(), StructOrdering.UNORDERED),
                        ExprValue.newStruct(list2.asSequence(), StructOrdering.UNORDERED),
                        ExprValue.newStruct(list3.asSequence(), StructOrdering.UNORDERED)
                    )
                ),
                ExprValue.newString("Name"),
                ExprValue.newString("Values")
            )
        )

        val res3 = fn.callWithRequired(
            session,
            listOf(
                ExprValue.newList(
                    listOf(
                        ExprValue.newStruct(list1.asSequence(), StructOrdering.UNORDERED),
                        ExprValue.newStruct(list2.asSequence(), StructOrdering.UNORDERED),
                        ExprValue.newStruct(list3.asSequence(), StructOrdering.UNORDERED)
                    )
                ),
                ExprValue.newString("Name"),
                ExprValue.newString("Values")
            )
        )

        setOf(res1, res2, res3).forEach {
            assertNotNull(it)
            assertEquals(
                "[{'test': ['ghj', 'klu']}, {'certificate': ['abc', 'cde', 'ghj', 'klu']}]",
                it.toString()
            )
        }

        val ex = assertThrows(Exception::class.java) {
            fn.callWithRequired(
                session,
                listOf(
                    ExprValue.newList(
                        listOf(
                            ExprValue.newInt(10),
                            ExprValue.newStruct(list2.asSequence(), StructOrdering.UNORDERED),
                        )
                    ),
                    ExprValue.newString("Name"),
                    ExprValue.newString("Values")
                )
            )
        }

        assertEquals(
            "All elements on input collection must be of type struct. Erroneous value: 10",
            ex.message
        )
    }
}
