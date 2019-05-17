package org.partiql.examples

import org.junit.*
import org.junit.Assert.*
import com.amazon.ion.*
import com.amazon.ion.system.*
import org.partiql.lang.*
import org.partiql.lang.ast.*
import org.partiql.lang.ast.passes.*
import org.partiql.lang.eval.*
import org.partiql.lang.syntax.*

/**
 * A simple AST rewriter that performs partial evaluation--i.e.: evaluates all sub-expressions containing only
 * literal operands and replaces them with the result. For example, the query `1 + 2 * 3` would be rewritten to
 * simply `7`. (That is, `(+ 1 (* (lit 2) (lit 3)))` would be rewritten to `(lit 7)`) (Note: s-expression AST is
 * shown without wrapping `term` or `meta`.)
 *
 * The query `foo + 2 * 3` would be rewritten to `foo + 6` (`(+ (id foo case_insensitive) (* 2 3)))`
 * becomes `(+ (id foo case_insensitive) (lit 6))`
 *
 * This isn't suitable for production use primarily because it needs significantly more testing and
 * it doesn't go as far as it could:
 * - function calls with only literal arguments could also be rewritten, assuming the function is deterministic
 * - cast operations against literal values, e.g.: `CAST(1 AS string)` are not considered literals, even though they
 * could.
 * - the rewrite will only be applied if all of the arguments of an NAry AST node are literals, however
 * a production quality version of this should be able to rewrite this:
 *
 * ```
 * (+
 *     (lit 1)
 *     (lit 2)
 *     (id foo case_insensitive)
 *     (lit 3)
 *     (lit 4))
 * ```
 *
 * Into:
 *
 * ```
 * (+
 *   (lit 3)
 *   (id foo case_insensitive)
 *   (lit 7))
 * ```
 *
 * @param ion should be the same instance of [IonSystem] that was passed to [SqlParser].
 * @param compileOptions should be the same instance of [CompileOptions] that was/will be passed to
 * [EvaluatingCompiler].
 */
private class PartialEvaluationRewriter(ion: IonSystem, val compileOptions: CompileOptions) : AstRewriterBase() {
    private val pipeline = CompilerPipeline.build(ion) { compileOptions(compileOptions) }
    private val session = EvaluationSession.standard()

    override fun rewriteNAry(node: NAry): ExprNode {
        val (op, args, metas) = node
        val rewrittenArgs = args.map { rewriteExprNode(it) }
        val rewrittenNary = NAry(op, rewrittenArgs, metas)

        return when {
            rewrittenArgs.all { it is Literal } -> {
                val e = pipeline.compile(rewrittenNary)
                val partiallyEvaluatedResult = e.eval(session)
                Literal(partiallyEvaluatedResult.ionValue, metas)
            }
            else -> {
                rewrittenNary
            }
        }
    }
}

/**
 * Demonstrates the use of a simple AST rewriter.
 */
class RewriterExampleTest {
    private val ion = IonSystemBuilder.standard().build()

    /** Demonstrates how to use [PartialEvaluationRewriter] as part of a [CompilerPipeline]. */
    @Test
    fun rewriterPipelineTest() {

        val pipeline = CompilerPipeline.build(ion) {
            addPreprocessingStep { exprNode, stepContext ->
                val rewriter = PartialEvaluationRewriter(stepContext.valueFactory.ion, stepContext.compileOptions)
                rewriter.rewriteExprNode(exprNode)
            }
        }

        val compiledQuery = pipeline.compile("1 + 1")

        assertEquals(2, compiledQuery.eval(EvaluationSession.standard()).scalar.numberValue()!!.toInt())
    }

    /** Demonstrates a how [PartialEvaluationRewriter] changes ASTs. */
    @Test
    fun rewriterSexpTest() {
        /** An instance of the example rewriter. */
        val rewriter = PartialEvaluationRewriter(ion, CompileOptions.standard())

        fun getAstFromSexp(sexp: String) =
            AstDeserializerBuilder(ion).build().deserialize(ion.singleValue("(ast (version 1) (root $sexp))") as IonSexp)

        /**
         * We pass the ASTs here as s-expressions in strings because this is far less code than constructing a
         * tree with the constructors of the types deriving from [ExprNode].
         *
         * @param expectedSexpAst The AST as it should appear after the rewrite has been applied.
         * @param originalSexpAst The AST in its original form.
         */
        fun runSexpTest(expectedSexpAst: String, originalSexpAst: String) {
            // Deserialize the original and expected ASTs into [ExprNode] instances.
            val originalAst = getAstFromSexp(originalSexpAst)
            val expectedAst = getAstFromSexp(expectedSexpAst)

            // Apply the rewrite
            val rewrittenAst = rewriter.rewriteExprNode(originalAst)

            // Verify the correct result.
            assertEquals("The rewritten AST must match the expected AST", expectedAst, rewrittenAst)
        }

        runSexpTest(
            // 7
            expectedSexpAst = "(lit 7)",
            // 1 + 2 * 3
            originalSexpAst = "(+ (lit 1) (* (lit 2) (lit 3)))"
            )

        runSexpTest(
            // foo + 6
            expectedSexpAst = "(+ (id foo case_insensitive) (lit 6))",
            // foo + 2 * 3
            originalSexpAst = "(+ (id foo case_insensitive) (* (lit 2) (lit 3)))")

        runSexpTest(
            // 20
            expectedSexpAst = "(lit 20)",
            // (2 + 3) * 4
            originalSexpAst = "(* (+ (lit 2) (lit 3)) (lit 4))")

        runSexpTest(
            // 20
            expectedSexpAst = "(lit 20)",
            // (2 + 3) * 4
            originalSexpAst = "(* (+ (lit 2) (lit 3)) (lit 4))")

        runSexpTest(
            // "foobar"
            expectedSexpAst = """(lit "foobar")""",
            // "foo" || "bar"
            originalSexpAst = """(|| (lit "foo") (lit "bar"))""")

        runSexpTest(
            // Expression does not change since no sub-expression contained only literals.
            expectedSexpAst = "(* (+ (id foo case_insensitive) (lit 2)) (lit 3))",
            // (foo + 2) * 3
            originalSexpAst = "(* (+ (id foo case_insensitive) (lit 2)) (lit 3))")
    }


}
