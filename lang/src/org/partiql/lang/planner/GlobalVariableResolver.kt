package org.partiql.lang.planner

import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName

/** Indicates the result of an attempt to resolve a global variable to its customer supplied unique identifier. */
sealed class GlboalResolutionResult {
    /**
     * A success case, indicates the [uniqueId] of the match to the [BindingName] in the global scope.
     * Typically, this is defined by the storage layer.
     *
     * In the future, this will likely contain much more than just a unique id.  It might include detailed schema
     * information about global variables.
     */
    data class GlobalVariable(val uniqueId: String) : GlboalResolutionResult()

    /** A failure case, indicates that resolution did not match any variable. */
    object Undefined : GlboalResolutionResult()
}

/**
 * Resolves global variables (usually tables) of the current database.
 *
 * Global variables are not limited to tables, but may be any PartiQL value assigned by the application embedding
 * PartiQL.  Most databases associate a UUID or similar unique identifier to a table.  The actual type used for the
 * unique identifier doesn't matter as long as it can be converted to and from a [String] and is unique within the
 * current database.
 *
 * The term "resolution" in means to look up a global variable's unique identifier, or to verify that it is not
 * defined in the current database.
 *
 * This interface is meant to be implemented by the application embedding PartiQL and added to the [PlannerPipeline]
 * via [PlannerPipeline.Builder.globalVariableResolver].
 */
interface GlobalVariableResolver {
    /**
     * Implementations try to resolve a variable which is typically a database table to a schema
     * using [bindingName].  [bindingName] includes both the name as specified by the query author and a [BindingCase]
     * which indicates if query author included double quotes (") which mean the lookup should be case-sensitive.
     *
     * Implementations of this function must return:
     *
     * - [GlboalResolutionResult.GlobalVariable] if [bindingName] matches a global variable (typically a database table).
     * - [GlboalResolutionResult.Undefined] if no identifier matches [bindingName].
     *
     * When determining if a variable name matches a global variable, it is important to consider if the comparison
     * should be case-sensitive or case-insensitive.  @see [BindingName.bindingCase].  In the event that more than one
     * variable matches a case-insensitive [BindingName], the implementation must still select one of them
     * without providing an error. (This is consistent with Postres's behavior in this scenario.)
     *
     * Note that while [GlboalResolutionResult.LocalVariable] exists, it is intentionally marked `internal` and cannot
     * be used outside this project.
     */
    fun resolveGlobal(bindingName: BindingName): GlboalResolutionResult
}

private val EMPTY: GlobalVariableResolver = object : GlobalVariableResolver {
    override fun resolveGlobal(bindingName: BindingName): GlboalResolutionResult = GlboalResolutionResult.Undefined
}

/** Convenience function for obtaining an instance of [GlobalVariableResolver] with no defined global variables. */
fun emptyGlobalsResolver(): GlobalVariableResolver = EMPTY
