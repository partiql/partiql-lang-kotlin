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
import org.junit.Test
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.StructOrdering
import org.partiql.lang.eval.namedValue

class MergeKeyValuesTests {
    private val ion = IonSystemBuilder.standard().build()
    private val factory = ExprValueFactory.standard(ion)
    private val session = EvaluationSession.standard()

    @Test
    fun testFunction() {
        val fn = MergeKeyValues(factory)

        val ionValue1 = ion.newList(ion.newString("abc"), ion.newString("cde"))
        val ionValue2 = ion.newList(ion.newString("ghj"), ion.newString("klu"))
        val ionValue3 = ion.newList(ion.newString("ghj"), ion.newString("klu"))

        val list1 = listOf(
            factory.newString("certificate").namedValue(factory.newSymbol("Name")),
            factory.newFromIonValue(ionValue1).namedValue(factory.newSymbol("Values")),
        )
        val list2 = listOf(
            factory.newString("certificate").namedValue(factory.newSymbol("Name")),
            factory.newFromIonValue(ionValue2).namedValue(factory.newSymbol("Values")),
        )
        val list3 = listOf(
            factory.newString("test").namedValue(factory.newSymbol("Name")),
            factory.newFromIonValue(ionValue3).namedValue(factory.newSymbol("Values")),
        )
        val res = fn.callWithRequired(
            session,
            listOf(
                factory.newBag(
                    listOf(
                        factory.newStruct(list1.asSequence(), StructOrdering.UNORDERED),
                        factory.newStruct(list2.asSequence(), StructOrdering.UNORDERED),
                        factory.newStruct(list3.asSequence(), StructOrdering.UNORDERED)
                    )
                ),
                factory.newString("Name"),
                factory.newString("Values")
            )
        )

        assertNotNull(res)
        assertEquals(
            "[{'test': ['ghj', 'klu']}, {'certificate': ['abc', 'cde', 'ghj', 'klu']}]",
            res.toString()
        )
    }
}
