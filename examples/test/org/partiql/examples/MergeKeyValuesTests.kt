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
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Test
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.StructOrdering
import org.partiql.lang.eval.bagExprValue
import org.partiql.lang.eval.intExprValue
import org.partiql.lang.eval.listExprValue
import org.partiql.lang.eval.namedValue
import org.partiql.lang.eval.sexpExprValue
import org.partiql.lang.eval.stringExprValue
import org.partiql.lang.eval.structExprValue
import org.partiql.lang.eval.symbolExprValue
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
            stringExprValue("certificate").namedValue(symbolExprValue("Name")),
            ionValue1.toExprValue().namedValue(symbolExprValue("Values")),
        )
        val list2 = listOf(
            stringExprValue("certificate").namedValue(symbolExprValue("Name")),
            ionValue2.toExprValue().namedValue(symbolExprValue("Values")),
        )
        val list3 = listOf(
            stringExprValue("test").namedValue(symbolExprValue("Name")),
            ionValue3.toExprValue().namedValue(symbolExprValue("Values")),
        )
        val res1 = fn.callWithRequired(
            session,
            listOf(
                bagExprValue(
                    listOf(
                        structExprValue(list1.asSequence(), StructOrdering.UNORDERED),
                        structExprValue(list2.asSequence(), StructOrdering.UNORDERED),
                        structExprValue(list3.asSequence(), StructOrdering.UNORDERED)
                    )
                ),
                stringExprValue("Name"),
                stringExprValue("Values")
            )
        )

        val res2 = fn.callWithRequired(
            session,
            listOf(
                sexpExprValue(
                    listOf(
                        structExprValue(list1.asSequence(), StructOrdering.UNORDERED),
                        structExprValue(list2.asSequence(), StructOrdering.UNORDERED),
                        structExprValue(list3.asSequence(), StructOrdering.UNORDERED)
                    )
                ),
                stringExprValue("Name"),
                stringExprValue("Values")
            )
        )

        val res3 = fn.callWithRequired(
            session,
            listOf(
                listExprValue(
                    listOf(
                        structExprValue(list1.asSequence(), StructOrdering.UNORDERED),
                        structExprValue(list2.asSequence(), StructOrdering.UNORDERED),
                        structExprValue(list3.asSequence(), StructOrdering.UNORDERED)
                    )
                ),
                stringExprValue("Name"),
                stringExprValue("Values")
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
                    listExprValue(
                        listOf(
                            intExprValue(10),
                            structExprValue(list2.asSequence(), StructOrdering.UNORDERED),
                        )
                    ),
                    stringExprValue("Name"),
                    stringExprValue("Values")
                )
            )
        }

        assertEquals(
            "All elements on input collection must be of type struct. Erroneous value: 10",
            ex.message
        )
    }
}
