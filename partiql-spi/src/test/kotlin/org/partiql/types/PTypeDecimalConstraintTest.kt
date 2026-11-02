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

package org.partiql.types

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import java.math.BigDecimal

/**
 * Tests the DECIMAL/NUMERIC type-system invariant: 0 <= scale <= precision.
 */
class PTypeDecimalConstraintTest {

    @Test
    fun `decimal rejects scale greater than precision`() {
        assertThrows(IllegalArgumentException::class.java) { PType.decimal(1, 3) }
    }

    @Test
    fun `numeric rejects scale greater than precision`() {
        assertThrows(IllegalArgumentException::class.java) { PType.numeric(1, 3) }
    }

    @Test
    fun `decimal rejects negative scale`() {
        assertThrows(IllegalArgumentException::class.java) { PType.decimal(5, -1) }
    }

    @Test
    fun `decimal allows scale equal to precision`() {
        val type = PType.decimal(3, 3)
        assertEquals(3, type.precision)
        assertEquals(3, type.scale)
    }

    @Test
    fun `decimal allows scale less than precision`() {
        val type = PType.decimal(5, 2)
        assertEquals(5, type.precision)
        assertEquals(2, type.scale)
    }

    /**
     * The three-argument Datum.decimal factory trusts its caller: it rejects a scale greater than precision instead
     * of silently coercing to a different type.
     */
    @Test
    fun `datum decimal rejects scale greater than precision`() {
        assertThrows(IllegalArgumentException::class.java) { Datum.decimal(BigDecimal("0.001"), 1, 3) }
    }

    /**
     * The three-argument Datum.decimal factory rejects a negative scale.
     */
    @Test
    fun `datum decimal rejects negative scale`() {
        assertThrows(IllegalArgumentException::class.java) { Datum.decimal(BigDecimal("1000"), 1, -3) }
    }

    /**
     * java.math.BigDecimal reports precision 1 and scale 3 for 0.001, which violates precision >= scale. The
     * value-only Datum.decimal factory derives a valid DECIMAL (here, decimal(3, 3)) from the value itself.
     */
    @Test
    fun `datum decimal derives a valid type for a sub-one value whose precision is less than scale`() {
        val datum = Datum.decimal(BigDecimal("0.001"))
        assertEquals(3, datum.type.precision)
        assertEquals(3, datum.type.scale)
        assertEquals(0, BigDecimal("0.001").compareTo(datum.bigDecimal))
    }

    /**
     * java.math.BigDecimal permits a negative scale (e.g. 1E+3 -> scale -3). The value-only Datum.decimal factory
     * clamps the scale to a non-negative value.
     */
    @Test
    fun `datum decimal derives a non-negative scale`() {
        val datum = Datum.decimal(BigDecimal("1E+3"))
        assertEquals(0, datum.type.scale)
        assertEquals(0, BigDecimal("1000").compareTo(datum.bigDecimal))
    }
}
