
package org.partiql.lang.thread

import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionelement.api.ionInt
import io.mockk.every
import io.mockk.spyk
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonToken
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.TokenSource
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
import org.partiql.lang.syntax.PartiQLParser
import org.partiql.lang.syntax.antlr.PartiQLTokens
import java.io.InputStream
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

    private fun testThreadInterrupt(interruptAfter: Long = INTERRUPT_AFTER_MS, interruptWait: Long = WAIT_FOR_THREAD_TERMINATION_MS, block: () -> Unit) {
        val wasInterrupted = AtomicBoolean(false)
        val t = thread {
            try {
                block()
            } catch (_: InterruptedException) {
                wasInterrupted.set(true)
            }
        }

        Thread.sleep(interruptAfter)
        t.interrupt()
        t.join(interruptWait)
        assertTrue(wasInterrupted.get(), "Thread should have been interrupted.")
    }

    @Test
    fun parserPartiQL() {
        val parser = spyk<PartiQLParser>()
        val query = "hello world"
        every {
            parser.createTokenStream(any())
        } returns EndlessTokenStream(PartiQLTokens(CharStreams.fromStream(InputStream.nullInputStream())))
        testThreadInterrupt(5) { parser.run { parseAstStatement(query) } }
    }

    @Test
    fun parserPartiQLUsingSLL() {
        val parser = PartiQLParser()
        val tokenStream = EndlessTokenStream(PartiQLTokens(CharStreams.fromStream(InputStream.nullInputStream())))
        val sllParser = parser.createParserSLL(tokenStream)
        testThreadInterrupt(5) { sllParser.run { statement() } }
    }

    @Test
    fun parserPartiQLUsingLL() {
        val parser = PartiQLParser()
        val tokenStream = EndlessTokenStream(PartiQLTokens(CharStreams.fromStream(InputStream.nullInputStream())))
        val llParser = parser.createParserLL(tokenStream)
        testThreadInterrupt(5) { llParser.run { statement() } }
    }

    @Test
    fun parserPartiQLTokenStream() {
        val parser = PartiQLParser()
        val endlessStream = EndlessInputStream()
        testThreadInterrupt { parser.run { createTokenStream(endlessStream) } }
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

    private class EndlessInputStream : InputStream() {
        override fun read(): Int {
            return 1
        }
    }

    private class EndlessTokenStream(source: TokenSource) : PartiQLParser.CountingTokenStream(source) {
        override fun size(): Int = Int.MAX_VALUE
        override fun LT(k: Int): Token {
            return CommonToken(PartiQLTokens.PLUS)
        }
    }
}

private tailrec fun fibonacci(n: Long, a: Long = 0, b: Long = 1): Long =
    when (n) {
        0L -> a
        1L -> b
        else -> fibonacci(n - 1L, b, a + b)
    }
