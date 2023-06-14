package org.partiql.plan.debug

import com.amazon.ionelement.api.ionInt
import org.junit.jupiter.api.Test
import org.partiql.plan.Rex
import org.partiql.plan.builder.plan

class PlanPrinterTest {

    @Test
    fun prettyPrintPlan() {
        val root = plan {
            // (1 + 2) - 3
            rexBinary {
                op = Rex.Binary.Op.MINUS
                lhs = rexBinary {
                    op = Rex.Binary.Op.PLUS
                    lhs = rexLit(ionInt(1))
                    rhs = rexLit(ionInt(2))
                }
                rhs = rexLit(ionInt(3))
            }
        }
        PlanPrinter.append(System.out, root)
    }
}
