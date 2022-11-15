package org.partiql.lang.planner.transforms.optimizations

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.ProblemHandler
import org.partiql.lang.planner.PartiqlPhysicalPass

// TODO: Remove from experimental once https://github.com/partiql/partiql-docs/issues/31 is resolved and a RFC is approved
internal fun createConcatWindowFunctionPass(): PartiqlPhysicalPass =
    ConcatWindowFunction()

/**
 * Creates an instance of [PartiqlPhysicalPass] that concatenate window functions if they are
 * 1) in the same query level (we don't want to concatenate sub-query's window function)
 * 2) operate the same window partition
 *
 * window(
 *      window(
 *          scan(...),
 *          over([some window definition A]),
 *          windowExpression(varDecl("\$__partiql_window_function_0"), "lag", listOf(id("a")))
 *      ),
 *      over([some window definition A])
 *      windowExpression(varDecl("\$__partiql_window_function_1"), "lead", listOf(id("a")))
 * )
 * Will become
 *  window(
 *      scan(...),
 *      over([some window definition A]),
 *      windowExpression(varDecl("\$__partiql_window_function_0"), "lag", listOf(id("a")))
 *      windowExpression(varDecl("\$__partiql_window_function_1"), "lead", listOf(id("a")))
 * )
 */
private class ConcatWindowFunction : PartiqlPhysicalPass {
    override val passName: String = "concat_window_function"

    override fun rewrite(inputPlan: PartiqlPhysical.Plan, problemHandler: ProblemHandler): PartiqlPhysical.Plan {
        return object : PartiqlPhysical.VisitorTransform() {
            override fun transformBexprWindow(node: PartiqlPhysical.Bexpr.Window): PartiqlPhysical.Bexpr {
                val rewritten = super.transformBexprWindow(node) as PartiqlPhysical.Bexpr.Window
                return rewritten.rewriteWindowExpression()
            }
        }.transformPlan(inputPlan)
    }
}

private fun PartiqlPhysical.Bexpr.Window.rewriteWindowExpression(): PartiqlPhysical.Bexpr {
    val modifiedWindowExpressionList = object : PartiqlPhysical.VisitorTransform() {

        // Only allow to recursive into the window node
        override fun transformBexpr(node: PartiqlPhysical.Bexpr) =
            when (node) {
                is PartiqlPhysical.Bexpr.Window -> super.transformBexpr(node)
                else -> node
            }

        override fun transformBexprWindow(node: PartiqlPhysical.Bexpr.Window): PartiqlPhysical.Bexpr {
            val rewritten = super.transformBexprWindow(node) as PartiqlPhysical.Bexpr.Window
            return handleIdenticalOver(rewritten, rewritten.source, rewritten)
        }

        private fun handleIdenticalOver(
            toCompareNode: PartiqlPhysical.Bexpr.Window,
            previousNode: PartiqlPhysical.Bexpr,
            currentNode: PartiqlPhysical.Bexpr.Window
        ): PartiqlPhysical.Bexpr {
            if (previousNode !is PartiqlPhysical.Bexpr.Window) {
                return currentNode
            }
            // check the next level
            else if (toCompareNode.windowSpecification != previousNode.windowSpecification) {
                return PartiqlPhysical.Bexpr.Window(
                    i = currentNode.i,
                    source = handleIdenticalOver(toCompareNode, previousNode.source, previousNode),
                    windowSpecification = currentNode.windowSpecification,
                    windowExpressionList = currentNode.windowExpressionList
                )
            } else {
                // No need to further recurisve
                return PartiqlPhysical.Bexpr.Window(
                    i = previousNode.i,
                    source = previousNode.source,
                    windowSpecification = previousNode.windowSpecification,
                    windowExpressionList = previousNode.windowExpressionList.toMutableList().plus(toCompareNode.windowExpressionList).toList()
                )
            }
        }
    }.transformBexpr(this)

    return modifiedWindowExpressionList
}
