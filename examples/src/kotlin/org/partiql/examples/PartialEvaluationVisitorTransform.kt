package org.partiql.examples

import com.amazon.ion.IonSystem
import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionelement.api.toIonElement
import org.partiql.examples.util.Example
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.ast.toAstStatement
import org.partiql.lang.ast.toExprNode
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.syntax.SqlParser
import java.io.PrintStream



/**
 * A simple AST visitor transform that performs partial evaluation--i.e.: evaluates all sub-expressions containing only
 * literal operands and replaces them with the result. For example, the query `1 + 2 * 3` would be transformed to
 * simply `7`. (That is, `(+ 1 (* (lit 2) (lit 3)))` would be transformed to `(lit 7)`) (Note: s-expression AST is
 * shown without wrapping `term` or `meta`.)
 *
 * The query `foo + 2 * 3` would be transformed to `foo + 6` (`(+ (id foo case_insensitive) (* 2 3)))`
 * becomes `(+ (id foo case_insensitive) (lit 6))`
 *
 * This example just shows the partial evaluation for addition. Once these operations are better modeled in the
 * PIG domain (https://github.com/partiql/partiql-lang-kotlin/issues/241), this can be expanded to all NAry operations
 * easily.
 *
 * This isn't suitable for production use primarily because it needs significantly more testing and
 * it doesn't go as far as it could:
 * - function calls with only literal arguments could also be transformed, assuming the function is deterministic
 * - cast operations against literal values, e.g.: `CAST(1 AS string)` are not considered literals, even though they
 * could.
 * - the transform will only be applied if all of the arguments of an NAry AST node are literals, however
 * a production quality version of this should be able to transform this:
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
private class PartialEvaluationVisitorTransform(val ion: IonSystem, val compileOptions: CompileOptions) : PartiqlAst.VisitorTransform() {
    private val pipeline = CompilerPipeline.build(ion) { compileOptions(compileOptions) }
    private val session = EvaluationSession.standard()

    override fun transformExprPlus(node: PartiqlAst.Expr.Plus): PartiqlAst.Expr {
        val ops = node.operands
        val metas = node.metas

        val transformedOps = ops.map { transformExpr(it) }
        val transformedNAry = PartiqlAst.build { plus(transformedOps, metas) }

        return when {
            transformedOps.all { it is PartiqlAst.Expr.Lit } -> {
                val e = pipeline.compile(PartiqlAst.build { query(transformedNAry) }.toExprNode(ion) )
                val partiallyEvaluatedResult = e.eval(session)
                PartiqlAst.build { lit(partiallyEvaluatedResult.ionValue.toIonElement(), metas) }
            }
            else -> {
                transformedNAry
            }
        }
    }
}

/** Demonstrates how to use [PartialEvaluationVisitorTransform] as part of a [CompilerPipeline]. */
class PartialEvaluationVisitorTransformExample(out: PrintStream) : Example(out) {
    private val ion = IonSystemBuilder.standard().build()

    override fun run() {
        val pipeline = CompilerPipeline.build(ion) {
            addPreprocessingStep { node, stepContext ->
                val visitorTransformer = PartialEvaluationVisitorTransform(ion, stepContext.compileOptions)

                val originalAst = node.toAstStatement()
                print("Original AST:", originalAst.toString())

                val query = node.toAstStatement() as PartiqlAst.Statement.Query

                val transformedNode = PartiqlAst.build { query(visitorTransformer.transformExpr(query.expr)) }
                print("Transformed AST:", transformedNode.toString())

                transformedNode.toExprNode(ion)
            }
        }

        pipeline.compile("1 + 1")
    }
}
