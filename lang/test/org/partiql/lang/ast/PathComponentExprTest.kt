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

package org.partiql.lang.ast

import com.amazon.ion.*
import com.amazon.ion.system.*
import junitparams.*
import org.junit.Test
import org.junit.runner.*
import kotlin.test.*

@RunWith(JUnitParamsRunner::class)
class PathComponentExprTest {
    private val ion: IonSystem = IonSystemBuilder.standard().build()
    private fun litString(str: String) = Literal(ion.newString(str), emptyMetaContainer)
    private fun litInt(value: Int) = Literal(ion.newInt(value), emptyMetaContainer)

    private val oneSensitive = PathComponentExpr(litInt(1), CaseSensitivity.SENSITIVE)
    private val oneInsensitive = oneSensitive.copy(case = CaseSensitivity.INSENSITIVE)

    private val fooInsensitive = PathComponentExpr(litString("foo"), CaseSensitivity.INSENSITIVE)
    private val fooSensitive = fooInsensitive.copy(case = CaseSensitivity.SENSITIVE)

    data class TestCase(val a: PathComponentExpr, val b: PathComponentExpr, val shouldBeEquivalent: Boolean)

    /** Provides at least a modicum of sanity checks for [PathComponentExpr.hashCode]. */
    @Test
    fun hashCodeTest() {
        // If these have the same hash code there's probably a problem somewhere
        val distinctHashCodes = listOf(
            oneSensitive.hashCode(),
            oneInsensitive.hashCode(),
            fooSensitive.hashCode(),
            fooInsensitive.hashCode()
        ).distinct()

        // The odds two or more of the hash codes being the same also seems rather low but hashCode() uniqueness is not
        // a requirement so we'll just make sure there's more than one distinct hash code.
        assertEquals(4, distinctHashCodes.size, "There must be 4 distinct hash codes, most likely!.")

        // Same for any of the hash codes being zero
        assertFalse(distinctHashCodes.any { it == 0 }, "None of the hash codes should equal zero, most likely.")
    }

    /** Tests [PathComponentExpr.equals]. */
    @Test
    @Parameters
    fun equivalenceTest(tc: TestCase) {
        when(tc.shouldBeEquivalent) {
            true -> {
                assertTrue(tc.a.equals(tc.b), "a must equal b")
                assertTrue(tc.b.equals(tc.a), "b must equal a")
                assertEquals(tc.a.hashCode(), tc.b.hashCode(), "a.hashCode() must equal b.hashCode(), most definitively.")
            }
            false -> {
                assertFalse(tc.a.equals(tc.b), "a must not equal b")
                assertFalse(tc.b.equals(tc.a), "b must not equal a")

                // .hashCode() uniqueness is not guaranteed so this assertion might fail problem someday.
                // however, the odds of this should be extremely low if `.hashCode()` is implemented effectively.
                assertNotEquals(tc.a.hashCode(), tc.b.hashCode(),
                                "a.hashCode() must not equal b.hashCode(), most likely.")
            }
        }
    }

    fun parametersForEquivalenceTest() =
        listOf(
            // is equivalent to itself
            TestCase(fooSensitive, fooSensitive, true),
            TestCase(fooInsensitive, fooInsensitive, true),
            TestCase(oneSensitive, oneSensitive, true),
            TestCase(oneInsensitive, oneInsensitive, true),

            // is equivalent to a copy of itself
            TestCase(fooSensitive, fooSensitive.copy(), true),
            TestCase(fooInsensitive, fooInsensitive.copy(), true),
            TestCase(oneSensitive, oneSensitive.copy(), true),
            TestCase(oneInsensitive, oneInsensitive.copy(), true),

            // sensitive PathComponentExpr with a literal string is not equivalent to varying cases
            TestCase(fooSensitive, fooSensitive.copy(expr = litString("FOO")), false),
            TestCase(fooSensitive, fooSensitive.copy(expr = litString("FoO")), false),
            TestCase(fooSensitive, fooSensitive.copy(expr = litString("fOo")), false),

            // insensitive PathComponentExpr with a literal string is equivalent to varying cases
            TestCase(fooInsensitive, fooInsensitive.copy(expr = litString("FOO")), true),
            TestCase(fooInsensitive, fooInsensitive.copy(expr = litString("FoO")), true),
            TestCase(fooInsensitive, fooInsensitive.copy(expr = litString("fOo")), true),

            // isn't equivalent because PathComponentExpr.expr doesn't match (but [PathComponentExpr.case] does.)
            TestCase(fooSensitive, oneSensitive, false),
            TestCase(fooInsensitive, oneInsensitive, false),
            TestCase(fooSensitive, fooSensitive.copy(expr = litString("bat")), false),
            TestCase(fooInsensitive, fooInsensitive.copy(expr = litString("bat")), false),
            TestCase(oneSensitive, oneSensitive.copy(expr = litInt(2)), false),
            TestCase(oneInsensitive, oneInsensitive.copy(expr = litInt(2)), false),

            // isn't equivalent because PathComponentExpr.case doesn't match (but [PathComponentExpr.expr] does.)
            TestCase(fooSensitive, fooInsensitive, false),
            TestCase(fooSensitive, oneInsensitive, false),
            TestCase(oneSensitive, oneInsensitive, false)
        )

}
