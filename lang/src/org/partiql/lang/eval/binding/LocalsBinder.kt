/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval.binding

import org.partiql.lang.eval.*
import org.partiql.lang.util.*

/**
 * Creates a list of bindings from a list of locals.
 * The various implementations (such as List<Alias>.[localsBinder]) will assign names to the locals.
 * Think of this as a factory which precomputes the name-bindings map for a list of locals.
 */
abstract class LocalsBinder {
    fun bindLocals(locals: List<ExprValue>) : Bindings {
        return object : Bindings {
            override fun get(bindingName: BindingName): ExprValue? = binderForName(bindingName)(locals)
        }
    }

    /** This method is the backbone of [bindLocals] and should be used when optimizing lookups. */
    abstract fun binderForName(bindingName: BindingName): (List<ExprValue>) -> ExprValue?
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
    fun compileBindings(keyMangler: (String) -> String = { it }): Map<String, (List<ExprValue>) -> ExprValue?> {
        data class Binder(val name: String, val func: (List<ExprValue>) -> ExprValue)
        return this.mapIndexed { index, alias -> sequenceOf(
            // the alias binds to the value itself
            Binder(alias.asName) { it[index] },
            // the alias binds to the name of the value
            if (alias.atName == null) null
            else Binder(alias.atName) { it[index].name ?: missingValue })}
            .asSequence()
            .flatten()
            .filterNotNull()
            // There may be multiple accessors per name.
            // Squash the accessor list to either the sole element or an error function
            .groupBy { keyMangler(it.name)  }
            .mapValues { (name, binders) ->
                when (binders.size) {
                    1 -> binders[0].func
                    else -> { _ ->
                        errAmbiguousBinding(name, binders.map { it.name })
                    }
                }
            }
    }

    /**
     * Nothing found at our scope, attempt to look at the attributes in our variables
     * TODO fix dynamic scoping to be in line with PartiQL rules
     */
    val dynamicLocalsBinder: (BindingName) -> (List<ExprValue>) -> ExprValue? = when (this.count()) {
        0 -> { _ -> { _ -> null } }
        1 -> { name -> { locals -> locals.first().bindings[name] } }
        else -> { name -> { locals -> locals.asSequence()
            .map { it.bindings[name] }
            .filterNotNull()
            .firstOrNull()
        }}
    }

    // Compile case-[in]sensitive bindings and return the accessor
    return object: LocalsBinder() {
        val caseSensitiveBindings = compileBindings()
        val caseInsensitiveBindings = compileBindings { it.toLowerCase() }
        override fun binderForName(bindingName: BindingName): (List<ExprValue>) -> ExprValue? {
            return when (bindingName.bindingCase) {
                       BindingCase.INSENSITIVE -> caseInsensitiveBindings[bindingName.name.toLowerCase()]
                       BindingCase.SENSITIVE -> caseSensitiveBindings[bindingName.name]
                   } ?: dynamicLocalsBinder(bindingName)
        }
    }
}