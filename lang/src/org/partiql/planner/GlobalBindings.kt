package org.partiql.planner

import com.amazon.ionelement.api.IonElement
import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName

/** Indicates the result of an attempt to resolve a global binding. */
sealed class ResolutionResult {
    /**
     * A success case, indicates the [uniqueId] of the match to the [BindingName] in the global scope.
     * Typically, this is defined by the storage layer.
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

fun interface GlobalBindings {
    /**
     * Tries to resolve a global variable, typically a database table, which is identified by its [bindingName],
     * which includes both the name and a [BindingCase] which indicates if the variable lookup should be
     * case-sensitive.
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
     * be used by PartiQL services.
     */
    fun resolve(bindingName: BindingName): ResolutionResult
}