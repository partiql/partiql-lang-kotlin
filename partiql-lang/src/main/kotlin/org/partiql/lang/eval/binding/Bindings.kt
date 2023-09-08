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
import com.amazon.ion.IonStruct
import org.partiql.lang.Ident
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.util.errAmbiguousBinding

/**
 * A mapping of name to [ExprValue].
 *
 * Due to the need to throw a consistent [EvaluationException] in the event of an ambiguous
 * case-insensitive binding lookup, customers should avoid implementing this interface directly
 * and should instead use one of the static factory methods or the lazy bindings builder to obtain
 * an instance of [Bindings] that correctly complies with the exception contract.  See
 * [org.partiql.lang.examples.Evaluation] for examples.
 */
interface Bindings<T> {

    /**
     * Looks up a name within the environment.
     *
     * @param bindingName The binding to look up.
     *
     * @return The value mapped to the binding, or `null` if no such binding exists.
     * @throws [EvaluationException] If multiple bindings matching the specified [BindingName] are found.
     * Clients should use [BindingHelper.throwAmbiguousBindingEvaluationException] to throw this exception.
     */
    @Throws(EvaluationException::class)
    operator fun get(bindingName: Ident): T?

    companion object {
        private val EMPTY = over { _ -> null }

        @Suppress("UNCHECKED_CAST")
        fun <T> empty(): Bindings<T> = EMPTY as Bindings<T>

        /**
         * A SAM conversion for [Bindings] from a function object.
         *
         * This is necessary as Kotlin currently doesn't support SAM conversions to
         * Kotlin defined interfaces (only Java defined interfaces).
         */
        fun <T> over(func: (Ident) -> T?): Bindings<T> = object : Bindings<T> {
            override fun get(bindingName: Ident): T? = func(bindingName)
        }

        /**
         * Returns an instance of [LazyBindingsBuilder].  If calling from Kotlin, prefer to use [buildLazyBindings]
         * instead.
         */
        @JvmStatic
        fun <T> lazyBindingsBuilder(): LazyBindingBuilder<T> = LazyBindingBuilder()

        /**
         * Invokes [block], passing an instance of [LazyBindingBuilder]. If calling from Java, prefer to use
         * [lazyBindingsBuilder] instead.
         */
        fun <T> buildLazyBindings(block: LazyBindingBuilder<T>.() -> Unit): Bindings<T> = LazyBindingBuilder<T>().apply(block).build()

        // TODO This API needs to be fleshed out with respect to mutable maps (the Map interface doesn't guaranteed immutability)
        /**
         * Returns an instance of [Bindings] that is backed by a `Map<String, ExprValue?>`.
         *
         * A current limitation of this factory method is that [backingMap] must not change.  In other words,
         * any [Map] that is passed that can be mutated, is not guaranteed to work correctly with this API.
         */
        @JvmStatic
        fun <T> ofMap(backingMap: Map<Ident, T>): Bindings<T> = MapBindings(backingMap)

        /**
         * Returns an instance of [Bindings<T>] that is backed by an [IonStruct].
         */
        @JvmStatic
        fun ofIonStruct(struct: IonStruct): Bindings<ExprValue> = IonStructBindings(struct)
    }

    /** An implementation of the builder pattern for instances of [Bindings<T>]. */
    class LazyBindingBuilder<T> {
        private val bindings = HashMap<Ident, Lazy<T>> ()

        fun addBinding(name: Ident, getter: () -> T): LazyBindingBuilder<T> =
            this.apply { bindings[name] = lazy(getter) }

        fun build(): Bindings<T> =
            LazyBindings<T>(bindings)
    }
}

/**
 * A [Bindings] implementation that is backed by a [Map<String, ExprValue?>].
 *
 * [originalCaseMap] is the backing [Map<String, ExprValue?>].  Important note:
 * *this must be immutable!  Changes to this map will not be reflected in [loweredCaseMap]
 * after it has been instantiated.
 *
 * [loweredCaseMap] is based on [originalCaseMap] and is lazily calculated the first time
 * a case-insensitive lookup is performed.
 *
 * If an ambiguous binding match is found during a case-insensitive lookup, [errAmbiguousBinding]
 * is invoked to report the error to the customer.
 */
class MapBindings<T>(val originalCaseMap: Map<Ident, T>) : Bindings<T> {
    override fun get(bindingName: Ident): T? =
        originalCaseMap[bindingName]
}

/** A [Bindings] implementation that lazily materializes the values of the bindings contained within. */
private class LazyBindings<T>(originalCaseMap: Map<Ident, Lazy<T>>) : Bindings<T> {
    private val delegate: Bindings<Lazy<T>> = MapBindings(originalCaseMap)

    override fun get(bindingName: Ident): T? = delegate[bindingName]?.value
}
