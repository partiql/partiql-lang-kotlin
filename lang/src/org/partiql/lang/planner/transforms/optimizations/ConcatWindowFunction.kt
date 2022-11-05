package org.partiql.lang.planner.transforms.optimizations

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.ProblemHandler
import org.partiql.lang.planner.PartiqlPhysicalPass

fun createConcatWindowFunctionPass(): PartiqlPhysicalPass =
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

        /**
         * Only allow to recursive into the window node
         */
        override fun transformBexpr(node: PartiqlPhysical.Bexpr) =
            when (node) {
                is PartiqlPhysical.Bexpr.Window -> super.transformBexpr(node)
                else -> node
            }

        override fun transformBexprWindow(node: PartiqlPhysical.Bexpr.Window): PartiqlPhysical.Bexpr {
            val rewritten = super.transformBexprWindow(node) as PartiqlPhysical.Bexpr.Window
            return handleIdenticalOver(rewritten, rewritten.source, rewritten)
        }

        /**
         * Consider a chain of window operator w1 -> w2 -> w3 -> scan
         * start with w3, since the w3's source is a scan, we want to return a exact copy of w3
         * now w2, we want to check if w2 and w3 has an identical over, if so, we want to concatenate the window functions
         * otherwise we return w2. Denoted w2', which may or may not contains a window node in source
         * now w1, we want to check if w1 has a idential over with w2' if so, we want to concatenate the window function and done
         * if not, we want to check if the source of w2' is window, if so, we check again
         * otherwise we are done.
         */

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

    // what should we return here?
    return modifiedWindowExpressionList
}
