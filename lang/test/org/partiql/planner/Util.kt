package org.partiql.planner

import com.amazon.ionelement.api.ionSymbol
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemDetails

/**
 * Creates a fake implementation of [GlobalBindings] with the specified [globalVariableNames].
 *
 * The fake unique identifier of bound variables is computed to be `fake_uid_for_${globalVariableName}`.
 */
fun createFakeGlobalBindings(vararg globalVariableNames: String) =
    GlobalBindings { bindingName ->
        val matches = globalVariableNames.filter { bindingName.isEquivalentTo(it) }
        when(matches.size) {
            0 -> ResolutionResult.Undefined
            else -> ResolutionResult.GlobalVariable(ionSymbol("fake_uid_for_" + matches.first()))
        }
    }

fun problem(line: Int, charOffset: Int, detail: ProblemDetails): Problem =
    Problem(SourceLocationMeta(line.toLong(), charOffset.toLong()), detail)
