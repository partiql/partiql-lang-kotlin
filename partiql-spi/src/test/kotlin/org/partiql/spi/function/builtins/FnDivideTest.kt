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

package org.partiql.spi.function.builtins

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import java.math.BigDecimal

class FnDivideTest {

    @ParameterizedTest
    @MethodSource("divisionCases")
    fun divisionReturnsValidPrecisionAndScale(case: DivisionCase) {
        val fn = requireNotNull(FnDivide.getInstance(arrayOf(case.lhs.type, case.rhs.type)))

        assertEquals(case.expectedType, fn.signature.returns)

        val result = fn.invoke(arrayOf(case.lhs, case.rhs))
        assertEquals(case.expectedType, result.type)
        assertEquals(case.expectedValue, result.bigDecimal)
        assertTrue(result.type.scale <= result.type.precision)
    }

    data class DivisionCase(
        val name: String,
        val lhs: Datum,
        val rhs: Datum,
        val expectedType: PType,
        val expectedValue: BigDecimal,
    ) {
        override fun toString(): String = name
    }

    companion object {

        @JvmStatic
        fun divisionCases(): List<DivisionCase> = listOf(
            DivisionCase(
                name = "decimal scale is clamped to returned precision",
                lhs = Datum.decimal(BigDecimal("100000.00"), 10, 2),
                rhs = Datum.decimal(BigDecimal("310000.00"), 38, 19),
                expectedType = PType.decimal(38, 38),
                expectedValue = BigDecimal("0.32258064516129032258064516129032258065"),
            ),
            DivisionCase(
                name = "numeric scale is clamped to returned precision",
                lhs = Datum.numeric(BigDecimal("100000.00"), 10, 2),
                rhs = Datum.numeric(BigDecimal("310000.00"), 38, 19),
                expectedType = PType.numeric(38, 38),
                expectedValue = BigDecimal("0.32258064516129032258064516129032258065"),
            ),
            DivisionCase(
                name = "uncapped precision and scale are unchanged",
                lhs = Datum.decimal(BigDecimal("10.00"), 10, 2),
                rhs = Datum.decimal(BigDecimal("4.00"), 10, 2),
                expectedType = PType.decimal(23, 13),
                expectedValue = BigDecimal("2.5000000000000"),
            ),
            DivisionCase(
                name = "valid capped precision and scale are unchanged",
                lhs = Datum.decimal(BigDecimal("10"), 29, 0),
                rhs = Datum.decimal(BigDecimal("2"), 9, 0),
                expectedType = PType.decimal(38, 10),
                expectedValue = BigDecimal("5.0000000000"),
            ),
            DivisionCase(
                name = "large integral capacity does not force scale reduction",
                lhs = Datum.decimal(BigDecimal("10"), 38, 0),
                rhs = Datum.decimal(BigDecimal("2.00"), 10, 2),
                expectedType = PType.decimal(38, 11),
                expectedValue = BigDecimal("5.00000000000"),
            ),
            DivisionCase(
                name = "scale and precision are both capped at maximum precision",
                lhs = Datum.decimal(BigDecimal("0.5"), 38, 38),
                rhs = Datum.decimal(BigDecimal.ONE, 1, 0),
                expectedType = PType.decimal(38, 38),
                expectedValue = BigDecimal("0.50000000000000000000000000000000000000"),
            ),
            DivisionCase(
                name = "scale is clamped for malformed input below maximum precision",
                lhs = Datum.decimal(BigDecimal.ZERO, 1, 2),
                rhs = Datum.decimal(BigDecimal.ONE, 1, 0),
                expectedType = PType.decimal(5, 5),
                expectedValue = BigDecimal("0.00000"),
            ),
            DivisionCase(
                name = "scale is clamped for malformed input above maximum precision",
                lhs = Datum.decimal(BigDecimal.ZERO, 1, 2),
                rhs = Datum.decimal(BigDecimal.ONE, 38, 0),
                expectedType = PType.decimal(38, 38),
                expectedValue = BigDecimal("0.00000000000000000000000000000000000000"),
            ),
        )
    }
}
