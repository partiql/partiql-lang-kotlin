package org.partiql.planner.impl

import org.junit.jupiter.api.Test
import org.partiql.ast.Expr
import org.partiql.ast.builder.ast
import org.partiql.planner.Env
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.float64Value
import org.partiql.value.int64Value

@OptIn(PartiQLValueExperimental::class)
class PartiQLPlannerDefaultTest {

    private val env = Env()
    private val planner = PartiQLPlannerDefault(env)
    // private val writer = PartiQLPlanWriter.ion(PartiQLVersion.VERSION_0_1)

    @Test
    fun sanity() {
        //
        // -- QUERY
        // > 1 + 1.0
        //
        val source = ast {
            statementQuery {
                expr = exprBinary {
                    op = Expr.Binary.Op.PLUS
                    lhs = exprLit(int64Value(1))
                    rhs = exprLit(float64Value(1.0))
                }
            }
        }

        // Default planner, no passes
        val result = planner.plan(source)
        println(result)

        // Debug dump for sanity check
        // val representation = writer.write(result.plan)

        // Expecting to see something similar to,
        //
        //  (
        //      partiql :: plan
        //      version :: (0 1)
        //
        //      (include
        //          (partiql '**' '*')
        //      )
        //
        //      (statement
        //          (query
        //              (call
        //                  (fn (partiql plus) (int float64)
        //      )))
        //  )
        //
        // println(representation.toString())
    }
}
