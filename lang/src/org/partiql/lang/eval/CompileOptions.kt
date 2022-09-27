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

import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.ProjectionIterationBehavior.FILTER_MISSING
import org.partiql.lang.eval.ProjectionIterationBehavior.UNFILTERED
import org.partiql.lang.eval.ThunkReturnTypeAssertions.ENABLED
import org.partiql.lang.eval.VisitorTransformMode.DEFAULT
import org.partiql.lang.eval.VisitorTransformMode.NONE
import org.partiql.lang.eval.visitors.IDENTITY_VISITOR_TRANSFORM
import org.partiql.lang.eval.visitors.basicVisitorTransforms
import java.time.ZoneOffset

/**
 * Defines the behavior when a non-existent variable is referenced.
 *
 * When `ERROR` any reference to a non-existent variable results in an [EvaluationException].
 *
 * When 'MISSING`, any reference to a non-existent variable results in an Ion `MISSING` value.]
 */
enum class UndefinedVariableBehavior {
    ERROR, MISSING
}

/**
 * Controls the behavior of [ExprValue.iterator] in the projection result.
 * For the query `Select a,b,c From <<{a:null, c:3}>>`;
 * * [FILTER_MISSING] will iterate over `[null,3]`
 * * [UNFILTERED] will iterate over `[null, missing, 3]`
 */
enum class ProjectionIterationBehavior {
    FILTER_MISSING, UNFILTERED
}

/**
 * Indicates how the evaluator is to handle type checking errors and how `MISSING` values are propagated
 * when encountered while evaluating binary operators and function calls.
 */
enum class TypingMode {
    /**
     * Affects the following evaluation-time behavior:
     *
     * - Most evaluation-time errors due to type mismatches are surfaced in the form of an [EvaluationException]
     * immediately.
     * - `IN` operator returns `FALSE` when the right-hand side is not a `BAG`, `LIST` or `SEXP`.
     * - For binary operators, if any operand is `MISSING` the result is `NULL`.
     * - For functions other than `COALESCE`, if any argument is `MISSING`, the result is `NULL`.
     */
    LEGACY,

    /**
     * Affects the following evaluation-time behavior:
     *
     * - Most exceptions that would stop query execution under [LEGACY] mode result in a `MISSING` value
     * instead.  Mostly, this relates to data type mismatch errors.
     * - `IN` operator returns `MISSING` when the right-hand side is not a `BAG`, `LIST` or `SEXP`.
     * - For binary operators, if any operand is `MISSING` the result is `MISSING`.
     * - For functions other than `COALESCE`, if any argument is `MISSING`, the result is `MISSING`.
     */
    PERMISSIVE

    // TODO: STRICT
}

/**
 * Indicates how CAST should behave.
 */
enum class TypedOpBehavior {
    @Deprecated(message = "TypedOpBehavior.LEGACY is an old compile option that will be removed in an upcoming release", replaceWith = ReplaceWith("TypedOpBehavior.HONOR_PARAMETERS"))
    /** The old behavior that ignores type arguments in CAST and IS. */
    LEGACY,

    /**
     * CAST and IS operators respect type parameters.
     *
     * The following behavior is added to `CAST`:
     *
     * - When casting a `DECIMAL(precision, scale)` with a greater scale to a `DECIMAL(precision, scale)` type with a
     * lower scale, rounds [half to even](https://en.wikipedia.org/wiki/Rounding#Round_half_to_even) as needed.
     * - When casting to `CHAR(n)` and `VARCHAR(n)`, if after conversion to unicode string, the value has more unicode
     * codepoints than `n`, truncation is performed.  Trailing spaces (`U+0020`) are fully preserved when casting to
     * `VARCHAR(n)`, but trimmed when casting to `CHAR(n).
     *
     * The following behavior is added to `IS`:
     *
     * - For string type `VARCHAR(n)`, the left-hand side of `IS` must evaluate be a string (not a symbol)
     * where the number of unicode code points is less than or equal `n`.
     * - When casting a `DECIMAL(precision, scale)` with a greater scale to a `DECIMAL(precision, scale)` type with a
     * lower scale, rounds [half to even](https://en.wikipedia.org/wiki/Rounding#Round_half_to_even) as needed.
     * - When casting to `CHAR(n)` and `VARCHAR(n)`, if after conversion to unicode string, the value has more unicode
     * codepoints than `n`, truncation is performed.  Trailing spaces (`U+0020`) are fully preserved when casting to
     * `VARCHAR(n)`, but trimmed when casting to `CHAR(n).
     **/
    HONOR_PARAMETERS
}

/**
 * Controls the behavior of intrinsic AST visitor transforms with [EvaluatingCompiler.compile].
 *
 * Most users will want [DEFAULT], which does the built-in visitor transforms for them, while
 * users wanting full control of the visitor transform process should use [NONE].
 */
enum class VisitorTransformMode {
    DEFAULT {
        override fun createVisitorTransform() = basicVisitorTransforms()
    },
    NONE {
        override fun createVisitorTransform() = IDENTITY_VISITOR_TRANSFORM
    };

    internal abstract fun createVisitorTransform(): PartiqlAst.VisitorTransform
}

/**
 * When [ENABLED], the compiler adds additional evaluation-time checks to every thunk that verify that the
 * [ExprValue] instance returned conforms to the expected [org.partiql.lang.types.StaticType].
 *
 * This is intended only for testing and diagnostic purposes as it likely comes with a significant performance penalty.
 * Production use may not be desirable.
 */
enum class ThunkReturnTypeAssertions {
    DISABLED,
    ENABLED
}

/**
 * Specifies options that effect the behavior of the PartiQL compiler.
 *
 * @param defaultTimezoneOffset Default timezone offset to be used when TIME WITH TIME ZONE does not explicitly
 * specify the time zone. Defaults to [ZoneOffset.UTC]
 */
@Suppress("DataClassPrivateConstructor")
data class CompileOptions private constructor (
    val undefinedVariable: UndefinedVariableBehavior,
    val projectionIteration: ProjectionIterationBehavior = ProjectionIterationBehavior.FILTER_MISSING,
    val visitorTransformMode: VisitorTransformMode = VisitorTransformMode.DEFAULT,
    val thunkOptions: ThunkOptions = ThunkOptions.standard(),
    val typingMode: TypingMode = TypingMode.LEGACY,
    val typedOpBehavior: TypedOpBehavior = TypedOpBehavior.LEGACY,
    val defaultTimezoneOffset: ZoneOffset = ZoneOffset.UTC
) {

    companion object {

        /**
         * Creates a java style builder that will choose the default values for any unspecified options.
         */
        @JvmStatic
        fun builder() = Builder()

        /**
         * Creates a java style builder that will clone the [CompileOptions] passed to the constructor.
         */
        @JvmStatic
        fun builder(options: CompileOptions) = Builder(options)

        /**
         * Kotlin style builder that will choose the default values for any unspecified options.
         */
        fun build(block: Builder.() -> Unit) = Builder().apply(block).build()

        /**
         * Kotlin style builder that will clone the [CompileOptions] passed to the constructor.
         */
        fun build(options: CompileOptions, block: Builder.() -> Unit) = Builder(options).apply(block).build()

        /**
         * Creates a [CompileOptions] instance with the standard values for use by the legacy AST compiler.
         */
        @JvmStatic
        fun standard() = Builder().build()
    }

    /**
     * Builds a [CompileOptions] instance.
     */
    class Builder(private var options: CompileOptions = CompileOptions(UndefinedVariableBehavior.ERROR)) {

        fun undefinedVariable(value: UndefinedVariableBehavior) = set { copy(undefinedVariable = value) }
        fun projectionIteration(value: ProjectionIterationBehavior) = set { copy(projectionIteration = value) }
        fun visitorTransformMode(value: VisitorTransformMode) = set { copy(visitorTransformMode = value) }
        fun typingMode(value: TypingMode) = set { copy(typingMode = value) }
        fun typedOpBehavior(value: TypedOpBehavior) = set { copy(typedOpBehavior = value) }
        fun thunkOptions(value: ThunkOptions) = set { copy(thunkOptions = value) }
        fun thunkOptions(build: ThunkOptions.Builder.() -> Unit) = set { copy(thunkOptions = ThunkOptions.build(build)) }
        fun defaultTimezoneOffset(value: ZoneOffset) = set { copy(defaultTimezoneOffset = value) }

        private inline fun set(block: CompileOptions.() -> CompileOptions): Builder {
            options = block(options)
            return this
        }

        fun build() = options
    }
}
