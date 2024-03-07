package org.partiql.lang.compiler.async

import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.partiql.annotations.ExperimentalPartiQLCompilerPipeline
import org.partiql.lang.compiler.PartiQLCompilerPipelineAsync
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.PartiQLResult
import org.partiql.lang.eval.PartiQLStatementAsync
import org.partiql.lang.eval.booleanValue
import org.partiql.lang.eval.isNotUnknown
import org.partiql.lang.eval.physical.operators.FilterRelationalOperatorFactoryAsync
import org.partiql.lang.eval.physical.operators.RelationExpressionAsync
import org.partiql.lang.eval.physical.operators.ValueExpressionAsync
import org.partiql.lang.eval.relation.RelationType
import org.partiql.lang.eval.relation.relation
import org.partiql.lang.planner.litTrue
import org.partiql.lang.planner.transforms.DEFAULT_IMPL
import org.partiql.lang.planner.transforms.PLAN_VERSION_NUMBER

private const val FAKE_IMPL_NAME = "test_async_fake"
private val FAKE_IMPL_NODE = PartiqlPhysical.build { impl(FAKE_IMPL_NAME) }

/**
 * Test is included to demonstrate the previous behavior for a relational operator expression that calls an async
 * functions. Previously, in the synchronous evaluator, making an async function call would require wrapping the call
 * in [runBlocking], which blocks the current thread of execution. This results in the 10 evaluation calls to be
 * executed one after the other, waiting for the previous call to finish.
 *
 * Since the [PartiQLStatementAsync] evaluation is now async, the [runBlocking] around the async function is no longer
 * required. Thus, the result is the 10 evaluation calls can be executed without waiting for the previous call to
 * finish.
 */
@OptIn(ExperimentalPartiQLCompilerPipeline::class)
class AsyncOperatorTests {
    private val fakeOperatorFactories = listOf(
        object : FilterRelationalOperatorFactoryAsync(FAKE_IMPL_NAME) {
            override fun create(
                impl: PartiqlPhysical.Impl,
                predicate: ValueExpressionAsync,
                sourceBexpr: RelationExpressionAsync
            ): RelationExpressionAsync = RelationExpressionAsync { state ->
                // If `RelationExpressionAsync`'s `evaluate` was NOT a `suspend fun`, then `runBlocking` would be
                // required
//                runBlocking {
                println("Calling")
                someAsyncOp()
//                }
                val input = sourceBexpr.evaluate(state)

                relation(RelationType.BAG) {
                    while (true) {
                        if (!input.nextRow()) {
                            break
                        } else {
                            val matches = predicate.invoke(state)
                            if (matches.isNotUnknown() && matches.booleanValue()) {
                                yield()
                            }
                        }
                    }
                }
            }
        }
    )

    private suspend fun someAsyncOp() {
        println("sleeping")
        delay(2000L)
        println("done sleeping")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun compilePlan() = runTest {
        val pipeline = PartiQLCompilerPipelineAsync.build {
            compiler
                .customOperatorFactories(
                    fakeOperatorFactories.map { it }
                )
        }
        val plan = PartiqlPhysical.build {
            plan(
                stmt = query(
                    bindingsToValues(
                        exp = lit(ionInt(42)),
                        query = filter(
                            i = FAKE_IMPL_NODE,
                            predicate = litTrue(),
                            source = scan(
                                i = DEFAULT_IMPL,
                                expr = bag(struct(listOf(structField(fieldName = lit(ionString("a")), value = lit(ionInt(1)))))),
                                asDecl = varDecl(0)
                            )
                        )
                    )
                ),
                version = PLAN_VERSION_NUMBER,
                locals = listOf(localVariable("_1", 0))
            )
        }
        val statement = pipeline.compile(plan)
        // asynchronously evaluate 10 statements and print out the results
        repeat(10) { index ->
            launch {
                print("\nCompiling $index. ")
                val result = statement.eval(EvaluationSession.standard()) as PartiQLResult.Value
                println("About to print value; $index")
                println(result.value)
            }
        }
    }
}
