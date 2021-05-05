package org.partiql.lang.thread

import com.amazon.ion.IonSexp
import com.amazon.ion.system.IonSystemBuilder
import org.junit.Test
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.CompilerPipelineImpl
import org.partiql.lang.StepContext
import org.partiql.lang.ast.AstDeserializerImpl
import org.partiql.lang.ast.AstSerializer
import org.partiql.lang.ast.AstVersion
import org.partiql.lang.ast.CaseSensitivity
import org.partiql.lang.ast.Literal
import org.partiql.lang.ast.NAry
import org.partiql.lang.ast.NAryOp
import org.partiql.lang.ast.ScopeQualifier
import org.partiql.lang.ast.VariableReference
import org.partiql.lang.ast.metaContainerOf
import org.partiql.lang.ast.passes.AstRewriterBase
import org.partiql.lang.ast.passes.AstVisitor
import org.partiql.lang.ast.passes.AstWalker
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.syntax.SqlParser
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.test.assertTrue


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
class ThreadInterruptedTests {
    private val ion = IonSystemBuilder.standard().build()
    private val reallyBigNAry by lazy { makeBigExprNode(20000000) }
    private val bigNAry by lazy { makeBigExprNode(10000000) }

    private val bigSexpAst by lazy {
        @Suppress("DEPRECATION")
        val sexp = AstSerializer.serialize(bigNAry, ion)
        // format of sexp is:
        // (ast
        //    (version x)
        //    (root y))
        // Extract y:
        (sexp[2] as IonSexp)[1] as IonSexp
    }

    private fun makeBigExprNode(n: Int): NAry {
        val emptyMetas = metaContainerOf()
        val variableA = VariableReference("a", CaseSensitivity.INSENSITIVE, ScopeQualifier.UNQUALIFIED, emptyMetas)
        return NAry(
            NAryOp.ADD,
            (0..n).map { variableA },
            emptyMetas
        )
    }


    private fun testThreadInterrupt(block: () -> Unit) {
        val wasInterrupted = AtomicBoolean(false)
        val t = thread {
            try {
                block()
            } catch(_: InterruptedException) {
                wasInterrupted.set(true)
            }
        }

        Thread.sleep(INTERRUPT_AFTER_MS)

        t.interrupt()
        t.join(WAIT_FOR_THREAD_TERMINATION_MS)
        assertTrue(wasInterrupted.get(), "Thread should have been interrupted.")
    }

    @Test
    fun parser() {
        testThreadInterrupt {
            val sqlParser = SqlParser(ion)
            val endlessTokenList = EndlessTokenList(ion)
            sqlParser.run {
                endlessTokenList.parseExpression()
            }
        }
    }

    @Test
    fun astChildIterator() {
        // force lazy load
        reallyBigNAry
        testThreadInterrupt {
            @Suppress("DEPRECATION")
            reallyBigNAry.iterator()
        }
    }

    @Test
    fun astWalker() {
        // force lazy load
        reallyBigNAry
        val walker = AstWalker(object : AstVisitor { })
        testThreadInterrupt {
            walker.walk(reallyBigNAry)
        }
    }

    @Test
    fun serialize() {
        // force lazy load
        bigNAry
        testThreadInterrupt {
            @Suppress("DEPRECATION")
            AstSerializer.serialize(bigNAry, ion)
        }
    }

    @Test
    fun deserialize_validate() {
        // force lazy load
        bigSexpAst

        val deserializer = AstDeserializerImpl(ion, emptyMap())
        deserializer.astVersion = AstVersion.V1
        testThreadInterrupt {
            deserializer.validate(bigSexpAst)
        }
    }

    @Test
    fun deserialize_deserializeExprNode() {
        // force lazy load
        bigSexpAst

        val deserializer = AstDeserializerImpl(ion, emptyMap())
        deserializer.astVersion = AstVersion.V1

        testThreadInterrupt {
            deserializer.deserializeExprNode(bigSexpAst)
        }
    }

    @Test
    fun astRewriterBase() {
        // force lazy load
        reallyBigNAry

        @Suppress("DEPRECATION")
        val identityRewriter = AstRewriterBase()
        testThreadInterrupt {
            identityRewriter.rewriteExprNode(reallyBigNAry)
        }
    }

    @Test
    fun compilerPipeline() {
        val numSteps = 10000000
        val pipeline = CompilerPipeline.build(ion) {
            repeat(numSteps) {
                addPreprocessingStep { expr, _ ->
                    // burns about 200ms on first run
                    assert(factorial(Int.MAX_VALUE / 8) >= 0)
                    expr
                }
            }
        } as CompilerPipelineImpl

        val expr = Literal(ion.newInt(42), metaContainerOf())
        val context = StepContext(pipeline.valueFactory, CompileOptions.standard(), emptyMap())

        testThreadInterrupt {
            pipeline.executePreProcessingSteps(expr, context)
        }
    }
}

private fun factorial(num: Int): Long {
    var result = 1L
    for (i in 2..num) result *= i
    return result
}

fun <T> time(message: String = "", block: () -> T): T {
    val startTime = System.currentTimeMillis()

    return block().also {
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        val of = when(message.length) { 0 -> "" else -> " of $message"}
        println("duration$of: $duration")
    }
}
