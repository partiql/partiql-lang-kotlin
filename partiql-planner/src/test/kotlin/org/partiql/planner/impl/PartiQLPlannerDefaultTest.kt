package org.partiql.planner.impl

import com.amazon.ionelement.api.IonElement
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.builder.ast
import org.partiql.plan.PartiQLVersion
import org.partiql.plan.ion.PartiQLPlanIonWriter
import org.partiql.planner.PartiQLPlannerDefault
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.float64Value
import org.partiql.value.int64Value

@OptIn(PartiQLValueExperimental::class)
class PartiQLPlannerDefaultTest {

    private val planner = PartiQLPlannerDefault()
    private val writer = PartiQLPlanIonWriter.get(PartiQLVersion.VERSION_0_1)

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

        // Debug dump for sanity check
        val representation = writer.toIon(result.plan)

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
        println(representation.toString())
    }

    @ParameterizedTest
    @MethodSource("pathCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testPaths(case: Case) = case.assert()

    companion object {

        // TODO need equals/hashcode on IRs
        @JvmStatic
        fun pathCases() = listOf<Case>()
    }

    class Case(
        val input: AstNode,
        val expected: IonElement,
    ) {
        fun assert() {}
    }
}
