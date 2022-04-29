package org.partiql.lang.planner

import org.partiql.lang.eval.ProjectionIterationBehavior
import org.partiql.lang.eval.ThunkOptions
import org.partiql.lang.eval.TypedOpBehavior
import org.partiql.lang.eval.TypingMode
import java.time.ZoneOffset

/*

Differences between CompilerOptions and PlannerOptions:

- There is no EvaluatorOptions equivalent for CompileOptions.visitorTransformMode since the planner always runs some basic
 normalization and variable resolution passes *before* the customer can inject their own transforms.
- There is no EvaluatorOptions equivalent for CompileOptions.thunkReturnTypeAssertions since PlannerPipeline does not
support the static type inferencer (yet).
- PlannerOptions.allowUndefinedVariables is new.
- PlannerOptions has no equivalent for CompileOptions.undefinedVariableBehavior -- this was added for backward
compatibility on behalf of a customer we don't have anymore.  Internal bug number is IONSQL-134.
 */

/**
 * Specifies options that effect the behavior of the PartiQL physical plan evaluator.
 *
 * @param defaultTimezoneOffset Default timezone offset to be used when TIME WITH TIME ZONE does not explicitly
 * specify the time zone. Defaults to [ZoneOffset.UTC].
 */
@Suppress("DataClassPrivateConstructor")
data class EvaluatorOptions private constructor (
    val projectionIteration: ProjectionIterationBehavior = ProjectionIterationBehavior.FILTER_MISSING,
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
         * Creates a java style builder that will clone the [EvaluatorOptions] passed to the constructor.
         */
        @JvmStatic
        fun builder(options: EvaluatorOptions) = Builder(options)

        /**
         * Kotlin style builder that will choose the default values for any unspecified options.
         */
        fun build(block: Builder.() -> Unit) = Builder().apply(block).build()

        /**
         * Kotlin style builder that will clone the [EvaluatorOptions] passed to the constructor.
         */
        fun build(options: EvaluatorOptions, block: Builder.() -> Unit) = Builder(options).apply(block).build()

        /**
         * Creates a [EvaluatorOptions] instance with the standard values for use by the legacy AST compiler.
         */
        @JvmStatic
        fun standard() = Builder().build()
    }

    /**
     * Builds a [EvaluatorOptions] instance.
     */
    class Builder(private var options: EvaluatorOptions = EvaluatorOptions()) {

        fun projectionIteration(value: ProjectionIterationBehavior) = set { copy(projectionIteration = value) }
        fun typingMode(value: TypingMode) = set { copy(typingMode = value) }
        fun typedOpBehavior(value: TypedOpBehavior) = set { copy(typedOpBehavior = value) }
        fun thunkOptions(value: ThunkOptions) = set { copy(thunkOptions = value) }
        fun defaultTimezoneOffset(value: ZoneOffset) = set { copy(defaultTimezoneOffset = value) }

        private inline fun set(block: EvaluatorOptions.() -> EvaluatorOptions): Builder {
            options = block(options)
            return this
        }

        fun build() = options
    }
}
