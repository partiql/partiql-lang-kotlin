package org.partiql.lang.planner.operators

import com.amazon.ionelement.api.ionBool
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.physical.EvaluatorState
import org.partiql.lang.eval.physical.SetVariableFunc
import org.partiql.lang.eval.physical.operators.FilterRelationalOperatorFactory
import org.partiql.lang.eval.physical.operators.JoinRelationalOperatorFactory
import org.partiql.lang.eval.physical.operators.LetRelationalOperatorFactory
import org.partiql.lang.eval.physical.operators.LimitRelationalOperatorFactory
import org.partiql.lang.eval.physical.operators.OffsetRelationalOperatorFactory
import org.partiql.lang.eval.physical.operators.ProjectRelationalOperatorFactory
import org.partiql.lang.eval.physical.operators.RelationExpression
import org.partiql.lang.eval.physical.operators.RelationalOperatorKind
import org.partiql.lang.eval.physical.operators.ScanRelationalOperatorFactory
import org.partiql.lang.eval.physical.operators.ValueExpression
import org.partiql.lang.eval.physical.operators.VariableBinding
import org.partiql.lang.planner.PlannerPipeline
import org.partiql.lang.planner.transforms.DEFAULT_IMPL
import org.partiql.lang.planner.transforms.PLAN_VERSION_NUMBER
import org.partiql.lang.util.ArgumentsProviderBase

private const val FAKE_IMPL_NAME = "fake_impl"
private val FAKE_IMPL_NODE = PartiqlPhysical.build { impl(FAKE_IMPL_NAME) }
class CreateFunctionWasCalledException(val thrownFromOperator: RelationalOperatorKind) :
    Exception("The create function was called!")

class CustomOperatorFactoryTests {
    // it's too expensive to create reasonable facsimiles of these operator factories, so we cheat by making them
    // all just throw CreateFunctionWasCalledException and asserting that this exception is thrown.
    val fakeOperatorFactories = listOf(
        object : ProjectRelationalOperatorFactory(FAKE_IMPL_NAME) {
            override fun create(
                impl: PartiqlPhysical.Impl,
                setVar: SetVariableFunc,
                args: List<ValueExpression>
            ): RelationExpression =
                throw CreateFunctionWasCalledException(RelationalOperatorKind.PROJECT)
        },
        object : ScanRelationalOperatorFactory(FAKE_IMPL_NAME) {
            override fun create(
                impl: PartiqlPhysical.Impl,
                expr: ValueExpression,
                setAsVar: SetVariableFunc,
                setAtVar: SetVariableFunc?,
                setByVar: SetVariableFunc?
            ): RelationExpression =
                throw CreateFunctionWasCalledException(RelationalOperatorKind.SCAN)
        },
        object : FilterRelationalOperatorFactory(FAKE_IMPL_NAME) {
            override fun create(impl: PartiqlPhysical.Impl, predicate: ValueExpression, sourceBexpr: RelationExpression) =
                throw CreateFunctionWasCalledException(RelationalOperatorKind.FILTER)
        },
        object : JoinRelationalOperatorFactory(FAKE_IMPL_NAME) {
            override fun create(
                impl: PartiqlPhysical.Impl,
                joinType: PartiqlPhysical.JoinType,
                leftBexpr: RelationExpression,
                rightBexpr: RelationExpression,
                predicateExpr: ValueExpression?,
                setLeftSideVariablesToNull: (EvaluatorState) -> Unit,
                setRightSideVariablesToNull: (EvaluatorState) -> Unit
            ): RelationExpression =
                throw CreateFunctionWasCalledException(RelationalOperatorKind.JOIN)
        },
        object : OffsetRelationalOperatorFactory(FAKE_IMPL_NAME) {
            override fun create(
                impl: PartiqlPhysical.Impl,
                rowCountExpr: ValueExpression,
                sourceBexpr: RelationExpression
            ): RelationExpression =
                throw CreateFunctionWasCalledException(RelationalOperatorKind.OFFSET)
        },
        object : LimitRelationalOperatorFactory(FAKE_IMPL_NAME) {
            override fun create(
                impl: PartiqlPhysical.Impl,
                rowCountExpr: ValueExpression,
                sourceBexpr: RelationExpression
            ): RelationExpression =
                throw CreateFunctionWasCalledException(RelationalOperatorKind.LIMIT)
        },
        object : LetRelationalOperatorFactory(FAKE_IMPL_NAME) {
            override fun create(
                impl: PartiqlPhysical.Impl,
                sourceBexpr: RelationExpression,
                bindings: List<VariableBinding>
            ) =
                throw CreateFunctionWasCalledException(RelationalOperatorKind.LET)
        }
    )

    @ParameterizedTest
    @ArgumentsSource(CustomOperatorCases::class)
    fun `make sure custom operator implementations are called`(tc: CustomOperatorCases.TestCase) {
        @Suppress("DEPRECATION")
        val pipeline = PlannerPipeline.build() {
            fakeOperatorFactories.forEach { addRelationalOperatorFactory(it) }
        }
        val ex = assertThrows<CreateFunctionWasCalledException> {
            pipeline.compile(tc.plan)
        }
        assertEquals(tc.expectedThrownFromOperator, ex.thrownFromOperator)
    }

    class CustomOperatorCases : ArgumentsProviderBase() {
        class TestCase(val expectedThrownFromOperator: RelationalOperatorKind, val plan: PartiqlPhysical.Plan)
        override fun getParameters() = listOf(
            // The key parts of the cases below are the setting of FAKE_IMPL_NODE which causes the custom operator
            // factories to be called.  The rest is the minimum gibberish needed to make complete PartiqlPhsyical.Bexpr
            // nodes.  There must only be one FAKE_IMPL_NODE per plan otherwise the CreateFunctionWasCalledException
            // might be called for an operator other than the one intended.
            createTestCase(RelationalOperatorKind.PROJECT) { project(FAKE_IMPL_NODE, varDecl(0)) },
            createTestCase(RelationalOperatorKind.SCAN) { scan(FAKE_IMPL_NODE, litTrue(), varDecl(0)) },
            createTestCase(RelationalOperatorKind.FILTER) { filter(FAKE_IMPL_NODE, litTrue(), defaultScan()) },
            createTestCase(RelationalOperatorKind.JOIN) { join(FAKE_IMPL_NODE, inner(), defaultScan(), defaultScan(), litTrue()) },
            createTestCase(RelationalOperatorKind.OFFSET) { offset(FAKE_IMPL_NODE, litTrue(), defaultScan()) },
            createTestCase(RelationalOperatorKind.LIMIT) { limit(FAKE_IMPL_NODE, litTrue(), defaultScan()) },
            createTestCase(RelationalOperatorKind.LET) { let(FAKE_IMPL_NODE, defaultScan(), listOf()) },
        )

        private fun PartiqlPhysical.Builder.litTrue() = lit(ionBool(true))

        private fun PartiqlPhysical.Builder.defaultScan() = scan(
            DEFAULT_IMPL,
            globalId("whatever"), varDecl(0)
        )

        private fun <T : PartiqlPhysical.Bexpr> createTestCase(
            operatorKind: RelationalOperatorKind,
            block: PartiqlPhysical.Builder.() -> T
        ) = TestCase(
            operatorKind,
            PartiqlPhysical.build {
                plan(
                    stmt = query(
                        bindingsToValues(
                            globalId("whatever"),
                            this.block()
                        )
                    ),
                    version = PLAN_VERSION_NUMBER
                )
            }
        )
    }
}
