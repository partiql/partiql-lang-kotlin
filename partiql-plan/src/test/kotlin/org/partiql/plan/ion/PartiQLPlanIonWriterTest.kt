package org.partiql.plan.ion

import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.SexpElement
import com.amazon.ionelement.api.loadSingleElement
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.plan.Plan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rex
import org.partiql.plan.builder.PlanBuilder
import org.partiql.plan.builder.PlanFactory
import org.partiql.plan.builder.plan
import org.partiql.plan.ion.impl.PartiQLPlanIonWriter_VERSION_0_1
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.symbolValue
import kotlin.test.assertEquals

@OptIn(PartiQLValueExperimental::class)
class PartiQLPlanIonWriterTest {

    @ParameterizedTest
    @MethodSource("pathCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testPaths(case: Case) = case.assert()

    companion object {

        private const val type = "\$type"

        @JvmStatic
        fun pathCases(): List<Case> {
            // Let `a` be resolved to (var ($type x) 0).
            val a = Plan.rex(StaticType.ANY, Plan.rexOpVarResolved(0))
            // Example cases
            return listOf(
                // `a.b.c`
                expect(
                    """
                    (path ($type 0)
                        (var ($type 0) 0) (
                            (step (lit ($type 7) b))
                            (step (lit ($type 7) c))
                    ))
                    """.trimIndent()
                ) {
                    rex(
                        StaticType.ANY,
                        rexOpPath {
                            root = a
                            steps += rexOpPathStepIndex(symbol("b"))
                            steps += rexOpPathStepIndex(symbol("c"))
                        }
                    )
                },
                // `a.b[*]`
                expect(
                    """
                    (path ($type 0)
                        (var ($type 0) 0) (
                            (step (lit ($type 7) b))
                            (step wildcard)
                    ))
                    """.trimIndent()
                ) {
                    rex(
                        StaticType.ANY,
                        rexOpPath {
                            root = a
                            steps += rexOpPathStepIndex(symbol("b"))
                            steps += rexOpPathStepWildcard()
                        }
                    )
                },
                // `a.*`
                expect(
                    """
                    (path ($type 0)
                        (var ($type 0) 0) (
                            (step unpivot)
                    ))
                    """.trimIndent()
                ) {
                    rex(
                        StaticType.ANY,
                        rexOpPath {
                            root = a
                            steps += rexOpPathStepUnpivot()
                        }
                    )
                }
            )
        }

        @JvmStatic
        private fun expect(
            expected: String,
            block: PlanBuilder.() -> PlanNode,
        ): Case {
            val i = plan(PlanFactory.DEFAULT, block)
            val e = loadSingleElement(expected)
            return Case(i, e)
        }

        private fun PlanBuilder.symbol(symbol: String): Rex {
            return rex(StaticType.SYMBOL, rexOpLit(symbolValue(symbol)))
        }
    }

    class Case(
        val input: PlanNode,
        val expected: IonElement,
        val type: SexpElement? = null,
    ) {
        fun assert() {
            val actual = PartiQLPlanIonWriter_VERSION_0_1.toIon(input, type)
            assertEquals(expected, actual)
        }
    }
}
