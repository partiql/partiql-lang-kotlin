/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ion.*
import com.amazon.ionsql.util.*

enum class BindingCase {
    SENSITIVE, INSENSITIVE;

    companion object {
        fun fromIonValue(sym: IonValue): BindingCase =
            when(sym.stringValue()) {
                "case_sensitive" -> SENSITIVE
                "case_insensitive" -> INSENSITIVE
                else -> errNoContext("Unable to convert ion value '${sym.stringValue()}' to a BindingCase instance",
                                     internal = true)
            }
        }

    fun toSymbol(ions: IonSystem) =
        ions.newSymbol(
            when(this) {
                SENSITIVE -> "case_sensitive"
                INSENSITIVE -> "case_insensitive"
            })
}

/**
 * Encapsulates the data necessary to perform a binding lookup.
 */
data class BindingName(val name: String, val bindingCase: BindingCase) {
    /**
     * Compares [name] to [otherName] using the rules specified by [bindingCase].
     */
    fun isEquivalentTo(otherName: String?) = otherName != null && name.isBindingNameEquivalent(otherName, bindingCase)
}

/** A simple mapping of name to [ExprValue]. */
interface Bindings {
    companion object {
        private val EMPTY = over { _ -> null }

        /** The empty bindings. */
        fun empty(): Bindings = EMPTY

        /**
         * A SAM conversion for [Bindings] from a function object.
         *
         * This is necessary as Kotlin currently doesn't support SAM conversions to
         * Kotlin defined interfaces (only Java defined interfaces).
         */
        fun over(func: (BindingName) -> ExprValue?): Bindings = object : Bindings {
            override fun get(bindingName: BindingName): ExprValue? = func(bindingName)
        }
    }

    /**
     * Looks up a name within the environment.
     *
     * @param name The binding to look up.
     *
     * @return The value mapped to the binding, or `null` if no such binding exists.
     * @throws [EvaluationException] If multiple bindings matching the specified [BindingName] are found.
     * Clients should use [BindingHelper.throwAmbiguousBindingEvaluationException] to throw this exception.
     */
    @Throws(EvaluationException::class)
    operator fun get(bindingName: BindingName): ExprValue?

}

