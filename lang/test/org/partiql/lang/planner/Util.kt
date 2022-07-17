package org.partiql.lang.planner

import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemDetails
import org.partiql.lang.eval.BindingName

/**
 * Creates a fake implementation of [GlobalVariableResolver] with the specified [globalVariableNames].
 *
 * The fake unique identifier of bound variables is computed to be `fake_uid_for_${globalVariableName}`.
 */
fun createFakeGlobalsResolver(vararg globalVariableNames: Pair<String, String>) =
    object : GlobalVariableResolver {
        override fun resolveGlobal(bindingName: BindingName): GlobalResolutionResult {
            val matches = globalVariableNames.filter { bindingName.isEquivalentTo(it.first) }
            return when (matches.size) {
                0 -> GlobalResolutionResult.Undefined
                else -> GlobalResolutionResult.GlobalVariable(matches.first().second)
            }
        }
    }

fun problem(line: Int, charOffset: Int, detail: ProblemDetails): Problem =
    Problem(SourceLocationMeta(line.toLong(), charOffset.toLong()), detail)
