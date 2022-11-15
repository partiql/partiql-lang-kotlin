
package org.partiql.lang.thread

import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionelement.api.ionInt
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.CompilerPipelineImpl
import org.partiql.lang.StepContext
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.visitors.VisitorTransformBase
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

/** How long (in miilis) to wait after starting a thread to set the interrupted flag. */
const val INTERRUPT_AFTER_MS: Long = 100

/** How long (in millis) to wait for a thread to terminate after setting the interrupted flag. */
const val WAIT_FOR_THREAD_TERMINATION_MS: Long = 1000

/**
 * At various locations in this codebase we check the state of [Thread.interrupted] and throw an
 * [InterruptedException] if it is set.  This class contains one test for each of those locations.
 * Each test spins up a background thread and tries to interrupt it.
 *
 * In order to ensure the these tests are deterministic, we use fairly low values for [INTERRUPT_AFTER_MS],
 * a large value for [WAIT_FOR_THREAD_TERMINATION_MS] and pathologically large mock data.
 */
// Enforce execution of tests in same thread as we need the execution to be deterministic for interruption behavior.
@Execution(ExecutionMode.SAME_THREAD)
class ThreadInterruptedTests {
    private val ion = IonSystemBuilder.standard().build()
    private val bigPartiqlAst = makeBigPartiqlAstExpr(10000000)

    /**
     * A "fake" list that contains [size] elements of [item].
     *
     * Constructing this is very cheap.  Adding the same element [size] times to real list is expensive.
     */
    class FakeList<T>(override val size: Int, private val item: T) : AbstractList<T>() {
        override fun get(index: Int): T = item
    }

    private fun makeBigPartiqlAstExpr(n: Int): PartiqlAst.Expr =
        PartiqlAst.build {
            val variableA = id("a", caseInsensitive(), unqualified())
            plus(FakeList(n, variableA))
        }

    private fun testThreadInterrupt(block: () -> Unit) {
        val wasInterrupted = AtomicBoolean(false)
        val t = thread {
            try {
                block()
            } catch (_: InterruptedException) {
                wasInterrupted.set(true)
            }
        }

        Thread.sleep(INTERRUPT_AFTER_MS)
        t.interrupt()
        t.join(WAIT_FOR_THREAD_TERMINATION_MS)
        assertTrue(wasInterrupted.get(), "Thread should have been interrupted.")
    }

    @Test
    fun visitorTransformBase() {
        val identityTransform = object : VisitorTransformBase() {}
        testThreadInterrupt {
            identityTransform.transformExpr(bigPartiqlAst)
        }
    }

    @Test
    fun compilerPipeline() {
        val numSteps = 10000000
        var accumulator = 0L

        val pipeline = CompilerPipeline.build(ion) {
            repeat(numSteps) {
                addPreprocessingStep { expr, _ ->
                    // Burn some CPU so we don't get thru all the pipeline steps before the interrupt.
                    // Adding the return value to accumulator guarantees this won't be elided by the JIT.
                    accumulator += fibonacci(131071)
                    expr
                }
            }
        } as CompilerPipelineImpl

        val expr = PartiqlAst.build { query(lit((ionInt(42)))) }
        val context = StepContext(pipeline.valueFactory, CompileOptions.standard(), emptyMap(), emptyMap())

        testThreadInterrupt {
            pipeline.executePreProcessingSteps(expr, context)
        }

        // At this point, there's a remote possibility that accumulator has overflowed to zero and the assertion
        // below might fail.  This guarantees that it will always pass.
        if (accumulator == 0L) {
            accumulator = 1L
        }

        assertTrue(accumulator != 0L)
    }
}

private tailrec fun fibonacci(n: Long, a: Long = 0, b: Long = 1): Long =
    when (n) {
        0L -> a
        1L -> b
        else -> fibonacci(n - 1L, b, a + b)
    }
