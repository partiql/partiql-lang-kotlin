/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.spi.catalog

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RoutineBindingTest {

    @Test
    fun copiesCanonicalNameAndOverloadCollection() {
        val canonicalName = Name.of("math", "pow")
        val overload = Any()
        val overloads = mutableListOf(overload)
        val binding = RoutineBinding(canonicalName, overloads)

        overloads += Any()
        canonicalName.getNamespace().getLevels()[0] = "changed"

        assertEquals(Name.of("math", "pow"), binding.canonicalName)
        assertNotSame(canonicalName, binding.canonicalName)
        assertEquals(1, binding.overloads.size)
        assertSame(overload, binding.overloads.single())
    }

    @Test
    fun doesNotExposeCanonicalNameState() {
        val binding = RoutineBinding(Name.of("math", "pow"), listOf("overload"))
        val exposed = binding.canonicalName

        exposed.getNamespace().getLevels()[0] = "changed"

        assertEquals(Name.of("math", "pow"), binding.canonicalName)
        assertNotSame(exposed, binding.canonicalName)
    }

    @Test
    fun exposesJavaUnmodifiableOverloads() {
        val binding = RoutineBinding(Name.of("math", "pow"), listOf("first"))

        assertThrows<UnsupportedOperationException> {
            (binding.overloads as MutableList<String>).add("second")
        }
    }

    @Test
    fun rejectsEmptyOverloads() {
        val error = assertThrows<IllegalArgumentException> {
            RoutineBinding(Name.of("math", "pow"), emptyList<String>())
        }

        assertEquals("Routine binding overloads cannot be empty", error.message)
    }
}
