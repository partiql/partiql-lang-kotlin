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
import org.partiql.lang.eval.StructOrdering
import org.partiql.lang.eval.exprBag
import org.partiql.lang.eval.exprInt
import org.partiql.lang.eval.exprList
import org.partiql.lang.eval.exprSexp
import org.partiql.lang.eval.exprString
import org.partiql.lang.eval.exprStruct
import org.partiql.lang.eval.exprSymbol
import org.partiql.lang.eval.namedValue
import org.partiql.lang.eval.toExprValue

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
            exprString("certificate").namedValue(exprSymbol("Name")),
            ionValue1.toExprValue().namedValue(exprSymbol("Values")),
        )
        val list2 = listOf(
            exprString("certificate").namedValue(exprSymbol("Name")),
            ionValue2.toExprValue().namedValue(exprSymbol("Values")),
        )
        val list3 = listOf(
            exprString("test").namedValue(exprSymbol("Name")),
            ionValue3.toExprValue().namedValue(exprSymbol("Values")),
        )
        val res1 = fn.callWithRequired(
            session,
            listOf(
                exprBag(
                    listOf(
                        exprStruct(list1.asSequence(), StructOrdering.UNORDERED),
                        exprStruct(list2.asSequence(), StructOrdering.UNORDERED),
                        exprStruct(list3.asSequence(), StructOrdering.UNORDERED)
                    )
                ),
                exprString("Name"),
                exprString("Values")
            )
        )

        val res2 = fn.callWithRequired(
            session,
            listOf(
                exprSexp(
                    listOf(
                        exprStruct(list1.asSequence(), StructOrdering.UNORDERED),
                        exprStruct(list2.asSequence(), StructOrdering.UNORDERED),
                        exprStruct(list3.asSequence(), StructOrdering.UNORDERED)
                    )
                ),
                exprString("Name"),
                exprString("Values")
            )
        )

        val res3 = fn.callWithRequired(
            session,
            listOf(
                exprList(
                    listOf(
                        exprStruct(list1.asSequence(), StructOrdering.UNORDERED),
                        exprStruct(list2.asSequence(), StructOrdering.UNORDERED),
                        exprStruct(list3.asSequence(), StructOrdering.UNORDERED)
                    )
                ),
                exprString("Name"),
                exprString("Values")
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
                    exprList(
                        listOf(
                            exprInt(10),
                            exprStruct(list2.asSequence(), StructOrdering.UNORDERED),
                        )
                    ),
                    exprString("Name"),
                    exprString("Values")
                )
            )
        }

        assertEquals(
            "All elements on input collection must be of type struct. Erroneous value: 10",
            ex.message
        )
    }
}
