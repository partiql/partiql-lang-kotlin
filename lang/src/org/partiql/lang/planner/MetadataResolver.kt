package org.partiql.lang.planner

import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName

/** Indicates the result of an attempt to resolve a global variable to its customer supplied unique identifier. */
sealed class ResolutionResult {
    /**
     * A success case, indicates the [uniqueId] of the match to the [BindingName] in the global scope.
     * Typically, this is defined by the storage layer.
     *
     * In the future, this will likely contain much more than just a unique id.  It might include detailed schema
     * information about global variables.
     */
    data class GlobalVariable(val uniqueId: String) : ResolutionResult()

    /**
     * A success case, indicates the [index] of the only possible match to the [BindingName] in a local lexical scope.
     * This is `internal` because [index] is an implementation detail that shouldn't be accessible outside of this
     * library.
     */
    internal data class LocalVariable(val index: Int) : ResolutionResult()

    /** A failure case, indicates that resolution did not match any variable. */
    object Undefined : ResolutionResult()
}

/**
 * Supplies the query planner with metadata about the current database.  Meant to be implemented by the application
 * embedding PartiQL.
 *
 * Metadata is associated with global variables.  Global variables can be tables or (less commonly) any other
 * application specific global variable.
 *
 * In the future, new methods could be added which expose information about other types of database metadata such as
 * available indexes and table statistics.
 */
interface MetadataResolver {
    /**
     * Implementations try to resolve a variable which is typically a database table to a schema
     * using [bindingName].  [bindingName] includes both the name as specified by the query author and a [BindingCase]
     * which indicates if query author included double quotes (") which mean the lookup should be case-sensitive.
     *
     * Implementations of this function must return:
     *
     * - [ResolutionResult.GlobalVariable] if [bindingName] matches a global variable (typically a database table).
     * - [ResolutionResult.Undefined] if no identifier matches [bindingName].
     *
     * When determining if a variable name matches a global variable, it is important to consider if the comparison
     * should be case-sensitive or case-insensitive.  @see [BindingName.bindingCase].  In the event that more than one
     * variable matches a case-insensitive [BindingName], the implementation must still select one of them
     * without providing an error. (This is consistent with Postres's behavior in this scenario.)
     *
     * Note that while [ResolutionResult.LocalVariable] exists, it is intentionally marked `internal` and cannot
     * be used by outside this project.
     */
    fun resolveVariable(bindingName: BindingName): ResolutionResult
}

private val EMPTY: MetadataResolver = object : MetadataResolver {
    override fun resolveVariable(bindingName: BindingName): ResolutionResult = ResolutionResult.Undefined
}

/** Convenience function for obtaining an instance of [MetadataResolver] with no defined variables. */
fun emptySchemaResolver(): MetadataResolver = EMPTY
