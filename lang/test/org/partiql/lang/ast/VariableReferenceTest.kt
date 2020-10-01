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

import org.junit.*
import org.junit.Test
import kotlin.test.*

class VariableReferenceTest {

    /**
     * Asserts that [vr1] and [vr2] are equal according to .equals and that they have the same hash code.
     */
    private fun assertEquals(vr1: VariableReference, vr2: VariableReference) {
        Assert.assertTrue("vr1 and vr2 must be equal", vr1.equals(vr2))
        Assert.assertEquals(vr1.hashCode(), vr2.hashCode())
    }

    /**
     * Asserts that [vr1] and [vr2] are not equal.
     */
    private fun assertNotEquals(vr1: VariableReference, vr2: VariableReference) {
        Assert.assertTrue(!vr1.equals(vr2))
    }

    val sensitiveFoo = VariableReference("foo", CaseSensitivity.SENSITIVE, ScopeQualifier.UNQUALIFIED, emptyMetaContainer)
    val insensitiveFoo = sensitiveFoo.copy(case = CaseSensitivity.INSENSITIVE)

    @Test
    fun caseSensitiveEquals() {
        assertEquals(
            sensitiveFoo,
            sensitiveFoo.copy())
    }

    @Test
    fun caseSensitiveNotEquals() {
        assertNotEquals(sensitiveFoo, sensitiveFoo.copy(id = "fop"))
        assertNotEquals(sensitiveFoo, insensitiveFoo)
        assertNotEquals(sensitiveFoo, sensitiveFoo.copy(scopeQualifier = ScopeQualifier.LEXICAL))
        assertNotEquals(sensitiveFoo, sensitiveFoo.copy(metas = metaContainerOf(SourceLocationMeta(1, 1))))
    }

    @Test
    fun caseInsensitiveEquals() {
        assertEquals(insensitiveFoo, insensitiveFoo.copy())

        assertEquals(insensitiveFoo, insensitiveFoo.copy(id = "foO"))
        assertEquals(insensitiveFoo, insensitiveFoo.copy(id = "fOo"))
        assertEquals(insensitiveFoo, insensitiveFoo.copy(id = "Foo"))
        assertEquals(insensitiveFoo, insensitiveFoo.copy(id = "FOO"))
    }

    @Test
    fun caseInsensitiveNotEquals() {
        assertNotEquals(insensitiveFoo, sensitiveFoo)
        assertNotEquals(insensitiveFoo, insensitiveFoo.copy(scopeQualifier = ScopeQualifier.LEXICAL))
        assertNotEquals(insensitiveFoo, insensitiveFoo.copy(metas = metaContainerOf(SourceLocationMeta(1, 1))))
    }
}