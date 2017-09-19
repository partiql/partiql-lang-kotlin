package com.amazon.ionsql.eval.binding

import com.amazon.ionsql.eval.Bindings
import com.amazon.ionsql.eval.ExprValue
import com.amazon.ionsql.eval.err
import com.amazon.ionsql.eval.name

/**
 * Creates a list of bindings from a list of locals.
 * The various implementations (such as List<Alias>.[localsBinder]) will assign names to the locals.
 * Think of this as a factory which precomputes the name-bindings map for a list of locals.
 */
abstract class LocalsBinder {
    fun bindLocals(locals: List<ExprValue>) : Bindings {
        return Bindings.over { name -> binderForName(name)(locals) }
    }

    /** This method is the backbone of [bindLocals] and should be used when optimizing lookups. */
    abstract fun binderForName(name: String): (List<ExprValue>) -> ExprValue?
}


/** Sources can be aliased to names with 'AS' and 'AT' */
data class Alias(val asName: String, val atName: String?)

/**
 * Returns a [LocalsBinder] for the bindings specified in the [Alias] ('AS' and optionally 'AT').
 * Each local [ExprValue] will be bound to the [Alias] with the same ordinal.
 *
 * For example, `[(as: 'x'), (as: 'y', at: 'z')].localsBinder.bindLocals(a, b)` will return the [Bindings]
 * `x => a, y => b, z => b.name`.
 *
 * A name can resolve to 0,1,.. bindings. For these cases:
 *  * 0 could be optimized (see [dynamicLocalsBinder]).
 *  * 1 is optimized and is fast. Further optimization most likely requires caching lookups by name (see [LocalsBinder.binderForName]).
 *  * 2+ throws an error.
 */
fun List<Alias>.localsBinder(missingValue: ExprValue): LocalsBinder {

    // For each 'as' and 'at' alias, create a locals accessor => { name: binding_accessor }
    data class Binder(val name: String, val func: (List<ExprValue>) -> ExprValue)
    val localBindings = this.mapIndexed { index, alias -> sequenceOf(
                // the alias binds to the value itself
                Binder(alias.asName) { it[index] },
                // the alias binds to the name of the value
                if (alias.atName == null) null
                else Binder(alias.atName) { it[index].name ?: missingValue }
            )}
            .asSequence()
            .flatten()
            .filterNotNull()
            // There may be multiple accessors per name.
            // Squash the accessor list to either the sole element or an error function
            .groupBy { it.name }
            .mapValues { (name, binders) ->
                when (binders.size) {
                    // The 0 case is fulfilled by withDefault
                    1 -> binders[0].func
                    else -> { locals -> err("$name is ambiguous: ${binders.map { it.func(locals).ionValue }}") }
                }
            }
            .withDefault(::dynamicLocalsBinder)

    return object: LocalsBinder() {
        override fun binderForName(name: String): (List<ExprValue>) -> ExprValue? = localBindings.getValue(name)
    }
}

/**
 * Nothing found at our scope, attempt to look at the attributes in our variables
 * TODO fix dynamic scoping to be in line with SQL++ rules
 */
private fun dynamicLocalsBinder(name: String): (List<ExprValue>) -> ExprValue? {
    return { locals ->
        locals.asSequence()
                .map { it.bindings[name] }
                .filterNotNull()
                .firstOrNull()
    }
}