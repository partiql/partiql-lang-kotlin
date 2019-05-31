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

package org.partiql.lang.eval
import com.amazon.ion.*
import org.partiql.lang.util.*

/** Indicates if the lookup of a particular binding should be case-sensitive or not. */
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
    val loweredName: String by lazy(LazyThreadSafetyMode.NONE) { name.toLowerCase() }
    /**
     * Compares [name] to [otherName] using the rules specified by [bindingCase].
     */
    fun isEquivalentTo(otherName: String?) = otherName != null && name.isBindingNameEquivalent(otherName, bindingCase)
}

/**
 * A mapping of name to [ExprValue].
 *
 * Due to the need to throw a consistent [EvaluationException] in the event of an ambiguous
 * case-insensitive binding lookup, customers should avoid implementing this interface directly
 * and should instead use one of the static factory methods or the lazy bindings builder to obtain
 * an instance of [Bindings] that correctly complies with the exception contract.  See
 * [org.partiql.lang.examples.Evaluation] for examples.
 */
interface Bindings {

    /**
     * Looks up a name within the environment.
     *
     * Implementations should respect the value of [BindingName.bindingCase].
     *
     * @param bindingName The binding to look up.
     *
     * @return The value mapped to the binding, or `null` if no such binding exists.
     * @throws [EvaluationException] If multiple bindings matching the specified [BindingName] are found.
     * Clients should use [BindingHelper.throwAmbiguousBindingEvaluationException] to throw this exception.
     */
    @Throws(EvaluationException::class)
    operator fun get(bindingName: BindingName): ExprValue?

    companion object {

        /** An "empty" set of bindings. */
        @JvmField
        val EMPTY = object : Bindings {
            override fun get(bindingName: BindingName): ExprValue? = null
        }

        /**
         * Returns an instance of [LazyBindingsBuilder].  If calling from Kotlin, prefer to use [buildLazyBindings]
         * instead.
         */
        @JvmStatic
        fun lazyBindingsBuilder(): LazyBindingBuilder = LazyBindingBuilder()


        /**
         * Invokes [block], passing an instance of [LazyBindingBuilder]. If calling from Java, prefer to use
         * [lazyBindingsBuilder] instead.
         */
        fun buildLazyBindings(block: LazyBindingBuilder.() -> Unit): Bindings = LazyBindingBuilder().apply(block).build()

        /**
         * Returns an instance of [Bindings] that is backed by a `Map<String, ExprValue?>`.
         */
        @JvmStatic
        fun ofMap(backingMap: Map<String, ExprValue>): Bindings = MapBindings(backingMap)

        /**
        * Returns an instance of [Bindings] that is backed by an [IonStruct].
        */
        @JvmStatic
        fun ofIonStruct(struct: IonStruct, valueFactory: ExprValueFactory): Bindings = IonStructBindings(valueFactory, struct)
    }

    /** An implementation of the builder pattern for instances of [Bindings]. */
    class LazyBindingBuilder {
        private val bindings = HashMap<String, Lazy<ExprValue>> ()

        fun addBinding(name: String, getter: () -> ExprValue): LazyBindingBuilder =
            this.apply { bindings[name] = lazy(getter)}

        fun build(): Bindings =
            LazyBindings(bindings)
    }
}

/**
 * [BindingMap<T>] stores two [Map<String, T>] fields, [originalCaseMap] and [loweredCaseMap].
 *
 * [loweredCaseMap] is based on [originalCaseMap] and is lazily calculated the first time
 * a case-insensitive lookup is performed.
 *
 * If an ambiguous binding match is found during a case-insensitive lookup, [errAmbiguousBinding]
 * is invoked to report the error to the customer.
 */
public class BindingMap<T>(val originalCaseMap: Map<String, T>) {
    val loweredCaseMap: Map<String, List<Map.Entry<String, T>>> by lazy {
        originalCaseMap.entries.groupBy { it.key.toLowerCase() }
    }

    fun lookup(bindingName: BindingName): T? =
        when (bindingName.bindingCase) {
            BindingCase.SENSITIVE   -> originalCaseMap[bindingName.name]
            BindingCase.INSENSITIVE -> {
                val foundBindings = loweredCaseMap[bindingName.loweredName]
                when {
                    foundBindings == null   -> null
                    foundBindings.size == 1 -> foundBindings.first().value
                    else                    ->
                        errAmbiguousBinding(bindingName.name, foundBindings.map { it.key })
                }
            }
        }
}

/** A [Bindings] implementation that is backed by a [Map<String, ExprValue?>]. */
public class MapBindings(backingMap: Map<String, ExprValue>) : Bindings {
    val bindingMap = BindingMap(backingMap)

    override fun get(bindingName: BindingName): ExprValue? = bindingMap.lookup(bindingName)
}


/** A [Bindings] implementation that lazily materializes the values of the bindings contained within. */
private class LazyBindings(backingMap: Map<String, Lazy<ExprValue>>) : Bindings {
    val bindingMap = BindingMap(backingMap)

    override fun get(bindingName: BindingName): ExprValue? = bindingMap.lookup(bindingName)?.value
}
