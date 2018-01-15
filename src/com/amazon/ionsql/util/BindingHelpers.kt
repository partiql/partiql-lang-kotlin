package com.amazon.ionsql.util

import com.amazon.ion.*
import com.amazon.ionsql.errors.*
import com.amazon.ionsql.eval.*

internal fun errAmbiguousBinding(bindingName: String, matchingNames: List<String>): Nothing {
    err("Case insensitive binding name matched more than one identifier",
        ErrorCode.EVALUATOR_AMBIGUOUS_BINDING,
        propertyValueMapOf(Property.BINDING_NAME to bindingName,
                           Property.BINDING_NAME_MATCHES to matchingNames.joinToString(", ")),
        internal = false)
}

/**
 * Compares this string to [other] using the rules specified by [case].
 */
fun String.isBindingNameEquivalent(other: String, case: BindingCase): Boolean =
    when(case) {
        BindingCase.SENSITIVE   -> this.equals(other)
        BindingCase.INSENSITIVE -> this.caseInsensitiveEquivalent(other)
    }

/**
 * One case insensitive equality check to rule them all.
 */
fun String.caseInsensitiveEquivalent(name: String) =
    equals(name, ignoreCase = true)


/**
 * Provides a common interface for performing case-sensitive or case-insensitive lookups against a delegated
 * collection.
 */
internal interface BindingExtractor<V> {
    /**
     * Return all case-insensitive matches for the specified case-insensitive [name] or an empty list of Pair<String, V>
     * if no matches are found.  The `first` property of each pair is the matched name, and the `second` property is
     * the value.
     */
    fun getAllCaseInsensitive(name: String): List<Pair<String, V>>
    /**
     * Returns a single match for the specified case-sensitive [name] or `null` if no binding is found.
     * */
    fun getCaseSensitive(name: String): V?
}

private fun <T> getBoundValue(bindingName: BindingName, extractor: BindingExtractor<T>): T? = when (bindingName.bindingCase) {
    BindingCase.SENSITIVE   -> extractor.getCaseSensitive(bindingName.name)
    BindingCase.INSENSITIVE -> {
        val matching = extractor.getAllCaseInsensitive(bindingName.name)
        when(matching.size) {
            0 -> null
            1 -> matching.first().second
            else -> errAmbiguousBinding(bindingName.name, matching.map { it.first })
        }
    }
}


/**
 * Looks up a binding from an [IonStruct], respecting the [BindingName]'s case sensitivity.
 * @throws [EvaluationException] if the binding's name was ambiguous, which can only happen if the lookup
 * is case insensitive.
 * @returns `null` if the binding was not found.
 */
operator fun IonStruct.get(bindingName: BindingName): IonValue? =
    getBoundValue(
        bindingName,
        object: BindingExtractor<IonValue> {
            override fun getAllCaseInsensitive(name: String) =
                this@get.filter { it.fieldName.caseInsensitiveEquivalent(name) }
                    .map { Pair(it.fieldName, it) }
            override fun getCaseSensitive(name: String): IonValue? = this@get[name]
        })

/**
 * Looks up a binding from a [Map<String, V>], respecting the [BindingName]'s case sensitivity.
 * @throws [EvaluationException] if the binding's name was ambiguous, which can only happen if the lookup
 * is case insensitive.
 * @returns `null` if the binding was not found.
 */
operator fun <V> Map<String, V>.get(bindingName: BindingName): V? =
    getBoundValue(
        bindingName,
        object: BindingExtractor<V> {
            override fun getAllCaseInsensitive(name: String) =
                this@get.entries.filter { it.key.caseInsensitiveEquivalent(name) }.map { Pair(it.key, it.value) }
            override fun getCaseSensitive(name: String): V? = this@get[name]
        })

/**
 * Containing static methods intended to be invoked by Java clients, this class provides a standard way to compare
 * identifiers and includes some helper methods for looking up values from [IonStruct] and [Map] instances according to
 * the specified [BindingCase].
 */
abstract class BindingHelper private constructor() {
    companion object {

        /**
         * Use this method to determine if the given identifiers match according to IonSQL++ rules
         * for identifier equality.
         */
        @JvmStatic
        fun bindingNameEquals(id1: String, id2: String, case: BindingCase): Boolean =
            when(case) {
                BindingCase.SENSITIVE   -> id1.equals(id2)
                BindingCase.INSENSITIVE -> id1.caseInsensitiveEquivalent(id2)
            }

        /**
         * Use this overload to look up a field from the specified [IonStruct] using the specified [BindingCase].
         * @throws [EvaluationException] if the binding's name was ambiguous, which can only happen if the lookup
         * is case insensitive.
         * @returns `null` if the binding was not found.
         */
        @JvmStatic
        fun lookupBinding(container: IonStruct, bindingName: BindingName): IonValue? =
            container.get(bindingName)

        /**
         * Use this overload to lookup a field from the specified [Map] using the specified binding case.
         * @throws [EvaluationException] if the binding's name was ambiguous, which can only happen if the lookup
         * is case insensitive.
         * @returns `null` if the binding was not found.
         */
        @JvmStatic
        fun <V> lookupBinding(container: Map<String, V>, bindingName: BindingName): V? =
            container.get(bindingName)
        /**
         * Should be used by clients when a case insensitive lookup results in matching more than one identifier.
         */
        @JvmStatic
        fun throwAmbiguousBindingEvaluationException(bindingName: String, matchingNames: List<String>): Nothing = errAmbiguousBinding(bindingName, matchingNames)
    }
}