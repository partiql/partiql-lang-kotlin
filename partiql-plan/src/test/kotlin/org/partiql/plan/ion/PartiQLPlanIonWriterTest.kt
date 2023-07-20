package org.partiql.plan.ion

import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.ionNull
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
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.symbolValue
import kotlin.test.assertEquals

/**
 * This class specifically tests converting a Kotlin PartiQLPLan to its V_0_1 Ion representation.
 *
 * We explicitly do NOT test type or function resolution, as we do not want to conflate our unit tests.
 */
@OptIn(PartiQLValueExperimental::class)
class PartiQLPlanIonWriterTest {

    @ParameterizedTest
    @MethodSource("pathCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testPaths(case: Case) = case.assert()

    companion object {

        // Use this as ($type) as it looks like a type ref with no ordinal
        private const val type = "\$type 0"

        // Use `any` anytime there is a type as we don't want to break serde on resolution changes
        private val any = Plan.typeRef("any", 0)

        @JvmStatic
        fun pathCases(): List<Case> {
            // Let `a` be resolved to (var ($type x) 0).
            val a = Plan.rex(any, Plan.rexOpVarResolved(0))
            // Example cases
            return listOf(
                // `a.b.c`
                expect(
                    """
                    (path ($type)
                        (var ($type) 0) (
                            (step (lit ($type) b))
                            (step (lit ($type) c))
                    ))
                    """.trimIndent()
                ) {
                    rex(
                        any,
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
                    (path ($type)
                        (var ($type) 0) (
                            (step (lit ($type) b))
                            (step wildcard)
                    ))
                    """.trimIndent()
                ) {
                    rex(
                        any,
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
                    (path ($type)
                        (var ($type) 0) (
                            (step unpivot)
                    ))
                    """.trimIndent()
                ) {
                    rex(
                        any,
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
            return rex(any, rexOpLit(symbolValue(symbol)))
        }
    }

    class Case(
        val input: PlanNode,
        val expected: IonElement,
        val ctx: IonElement = ionNull(),
    ) {
        fun assert() {
            val actual = PartiQLPlanIonWriter_VERSION_0_1.toIonDebug(input, ctx)
            assertEquals(expected, actual)
        }
    }
}
