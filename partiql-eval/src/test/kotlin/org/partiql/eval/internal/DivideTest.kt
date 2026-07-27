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

package org.partiql.eval.internal

import org.junit.jupiter.api.Test
import org.partiql.eval.Mode
import org.partiql.spi.types.PType
import org.partiql.spi.types.PTypeField
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import java.math.BigDecimal

class DivideTest {

    private val ratio = Datum.decimal(
        BigDecimal("0.32258064516"),
        38,
        11,
    )

    private val unitRatio = Datum.decimal(
        BigDecimal("1.00000000000"),
        38,
        11,
    )

    private val salaries = Global(
        name = "salaries",
        value = Datum.bag(
            listOf(
                salary(id = 1, dept = "engineering", amount = "100000.00"),
                salary(id = 2, dept = "engineering", amount = "210000.00"),
                salary(id = 3, dept = "hr", amount = "50000.00"),
            ),
        ),
        type = PType.bag(
            PType.row(
                PTypeField.of("id", PType.integer()),
                PTypeField.of("dept", PType.varchar(16)),
                PTypeField.of("salary", PType.decimal(10, 2)),
            ),
        ),
    )

    @Test
    fun decimalDivisionReturnsCappedPrecisionAndScale() {
        SuccessTestCase(
            input = """
                CAST(100000.00 AS DECIMAL(10, 2)) /
                    CAST(310000.00 AS DECIMAL(38, 19))
            """.trimIndent(),
            mode = Mode.STRICT(),
            expected = ratio,
            jvmEquality = true,
        ).run()
    }

    @Test
    fun permissiveAggregateRatioProjectionRetainsDivisionResult() {
        SuccessTestCase(
            input = """
                SELECT
                    id,
                    dept,
                    salary,
                    salary / (
                        SELECT SUM(salary)
                        FROM salaries AS t2
                        WHERE t2.dept = t1.dept
                    ) AS ratio
                FROM salaries AS t1
                ORDER BY id
            """.trimIndent(),
            mode = Mode.PERMISSIVE(),
            expected = Datum.array(
                listOf(
                    salary(id = 1, dept = "engineering", amount = "100000.00", ratio = ratio),
                    salary(
                        id = 2,
                        dept = "engineering",
                        amount = "210000.00",
                        ratio = Datum.decimal(BigDecimal("0.67741935484"), 38, 11),
                    ),
                    salary(id = 3, dept = "hr", amount = "50000.00", ratio = unitRatio),
                ),
            ),
            globals = listOf(salaries),
        ).run()
    }

    private fun salary(id: Int, dept: String, amount: String, ratio: Datum? = null): Datum {
        val fields = mutableListOf(
            Field.of("id", Datum.integer(id)),
            Field.of("dept", Datum.varchar(dept, 16)),
            Field.of("salary", Datum.decimal(BigDecimal(amount), 10, 2)),
        )
        ratio?.let { fields.add(Field.of("ratio", it)) }
        return Datum.struct(fields)
    }
}
