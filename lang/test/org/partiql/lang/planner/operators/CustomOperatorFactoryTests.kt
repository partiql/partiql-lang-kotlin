package org.partiql.lang.planner.operators

import com.amazon.ionelement.api.ionBool
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.ION
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.physical.SetVariableFunc
import org.partiql.lang.eval.physical.operators.BindingsExpr
import org.partiql.lang.eval.physical.operators.FilterPhysicalOperatorFactory
import org.partiql.lang.eval.physical.operators.JoinPhysicalOperatorFactory
import org.partiql.lang.eval.physical.operators.LetPhysicalOperatorFactory
import org.partiql.lang.eval.physical.operators.LimitPhysicalOperatorFactory
import org.partiql.lang.eval.physical.operators.OffsetPhysicalOperatorFactory
import org.partiql.lang.eval.physical.operators.PhysicalOperatorKind
import org.partiql.lang.eval.physical.operators.ProjectPhysicalOperatorFactory
import org.partiql.lang.eval.physical.operators.ScanPhysicalOperatorFactory
import org.partiql.lang.eval.physical.operators.ValueExpr
import org.partiql.lang.eval.physical.operators.VariableBinding
import org.partiql.lang.planner.PlannerPipeline
import org.partiql.lang.planner.transforms.DEFAULT_IMPL
import org.partiql.lang.planner.transforms.PLAN_VERSION_NUMBER
import org.partiql.lang.util.ArgumentsProviderBase

private const val FAKE_IMPL_NAME = "fake_impl"
private val FAKE_IMPL_NODE = PartiqlPhysical.build { impl(FAKE_IMPL_NAME) }
class CreateFunctionWasCalledException(val thrownFromOperator: PhysicalOperatorKind) :
    Exception("The create function was called!")

class CustomOperatorFactoryTests {
    // it's too expensive to create reasonable facsimiles of these operator factories, so we cheat by making them
    // all just throw CreateFunctionWasCalledException and asserting that this exception is thrown.
    val fakeOperatorFactories = listOf(
        object : ProjectPhysicalOperatorFactory(FAKE_IMPL_NAME) {
            override fun create(
                impl: PartiqlPhysical.Impl,
                setVar: SetVariableFunc,
                args: List<ValueExpr>
            ): BindingsExpr =
                throw CreateFunctionWasCalledException(PhysicalOperatorKind.PROJECT)
        },
        object : ScanPhysicalOperatorFactory(FAKE_IMPL_NAME) {
            override fun create(
                impl: PartiqlPhysical.Impl,
                expr: ValueExpr,
                setAsVar: SetVariableFunc,
                setAtVar: SetVariableFunc?,
                setByVar: SetVariableFunc?
            ): BindingsExpr =
                throw CreateFunctionWasCalledException(PhysicalOperatorKind.SCAN)
        },
        object : FilterPhysicalOperatorFactory(FAKE_IMPL_NAME) {
            override fun create(impl: PartiqlPhysical.Impl, predicate: ValueExpr, sourceBexpr: BindingsExpr) =
                throw CreateFunctionWasCalledException(PhysicalOperatorKind.FILTER)
        },
        object : JoinPhysicalOperatorFactory(FAKE_IMPL_NAME) {
            override fun create(
                impl: PartiqlPhysical.Impl,
                joinType: PartiqlPhysical.JoinType,
                leftBexpr: BindingsExpr,
                rightBexpr: BindingsExpr,
                predicateExpr: ValueExpr?,
                setLeftSideVariablesToNull: (EvaluatorState) -> Unit,
                setRightSideVariablesToNull: (EvaluatorState) -> Unit
            ): BindingsExpr =
                throw CreateFunctionWasCalledException(PhysicalOperatorKind.JOIN)
        },
        object : OffsetPhysicalOperatorFactory(FAKE_IMPL_NAME) {
            override fun create(
                impl: PartiqlPhysical.Impl,
                rowCountExpr: ValueExpr,
                sourceBexpr: BindingsExpr
            ): BindingsExpr =
                throw CreateFunctionWasCalledException(PhysicalOperatorKind.OFFSET)
        },
        object : LimitPhysicalOperatorFactory(FAKE_IMPL_NAME) {
            override fun create(
                impl: PartiqlPhysical.Impl,
                rowCountExpr: ValueExpr,
                sourceBexpr: BindingsExpr
            ): BindingsExpr =
                throw CreateFunctionWasCalledException(PhysicalOperatorKind.LIMIT)
        },
        object : LetPhysicalOperatorFactory(FAKE_IMPL_NAME) {
            override fun create(
                impl: PartiqlPhysical.Impl,
                sourceBexpr: BindingsExpr,
                bindings: List<VariableBinding>
            ) =
                throw CreateFunctionWasCalledException(PhysicalOperatorKind.LET)
        }
    )

    @ParameterizedTest
    @ArgumentsSource(CustomOperatorCases::class)
    fun `make sure custom operator implementations are called`(tc: CustomOperatorCases.TestCase) {
        @Suppress("DEPRECATION")
        val pipeline = PlannerPipeline.build(ION) {
            fakeOperatorFactories.forEach { addPhysicalOperatorFactory(it) }
        }
        val ex = assertThrows<CreateFunctionWasCalledException> {
            pipeline.compile(tc.plan)
        }
        assertEquals(tc.expectedThrownFromOperator, ex.thrownFromOperator)
    }

    class CustomOperatorCases : ArgumentsProviderBase() {
        class TestCase(val expectedThrownFromOperator: PhysicalOperatorKind, val plan: PartiqlPhysical.Plan)
        override fun getParameters() = listOf(
            // The key parts of the cases below are the setting of FAKE_IMPL_NODE which causes the custom operator
            // factories to be called.  The rest is the minimum gibberish needed to make complete PartiqlPhsyical.Bexpr
            // nodes.  There must only be one FAKE_IMPL_NODE per plan otherwise the CreateFunctionWasCalledException
            // might be called for an operator other than the one intended.
            createTestCase(PhysicalOperatorKind.PROJECT) { project(FAKE_IMPL_NODE, varDecl(0)) },
            createTestCase(PhysicalOperatorKind.SCAN) { scan(FAKE_IMPL_NODE, litTrue(), varDecl(0)) },
            createTestCase(PhysicalOperatorKind.FILTER) { filter(FAKE_IMPL_NODE, litTrue(), defaultScan()) },
            createTestCase(PhysicalOperatorKind.JOIN) { join(FAKE_IMPL_NODE, inner(), defaultScan(), defaultScan(), litTrue()) },
            createTestCase(PhysicalOperatorKind.OFFSET) { offset(FAKE_IMPL_NODE, litTrue(), defaultScan()) },
            createTestCase(PhysicalOperatorKind.LIMIT) { limit(FAKE_IMPL_NODE, litTrue(), defaultScan()) },
            createTestCase(PhysicalOperatorKind.LET) { let(FAKE_IMPL_NODE, defaultScan(), listOf()) },
        )

        private fun PartiqlPhysical.Builder.litTrue() = lit(ionBool(true))

        private fun PartiqlPhysical.Builder.defaultScan() = scan(
            DEFAULT_IMPL,
            globalId("whatever", caseInsensitive()), varDecl(0)
        )

        private fun <T : PartiqlPhysical.Bexpr> createTestCase(
            operatorKind: PhysicalOperatorKind,
            block: PartiqlPhysical.Builder.() -> T
        ) = TestCase(
            operatorKind,
            PartiqlPhysical.build {
                plan(
                    stmt = query(
                        bindingsToValues(
                            globalId("whatever", caseInsensitive()),
                            this.block()
                        )
                    ),
                    version = PLAN_VERSION_NUMBER
                )
            }
        )
    }
}
