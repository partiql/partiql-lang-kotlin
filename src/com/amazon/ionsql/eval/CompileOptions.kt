package com.amazon.ionsql.eval


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
 * Specifies options that effect the behavior of the IonSQL++ compiler.
 */
class CompileOptions private constructor(val undefinedVariable: UndefinedVariableBehavior) {

    companion object {

        /**
         * Creates a java style builder that will choose the default values for any unspecified options.
         */
        @JvmStatic
        fun builder() = Builder()

        /**
         * Kotlin style builder that will choose the default values for any unspecified options.
         */
        fun build(block: Builder.() -> Unit) = Builder().apply(block).build()

        /**
         * Creates a [CompileOptions] instance with default values.
         */
        @JvmStatic
        fun default() = Builder().build()
    }

    /**
     * Builds a [CompileOptions] instance.
     */
    class Builder {
        private var undefinedVariable: UndefinedVariableBehavior = UndefinedVariableBehavior.ERROR

        fun undefinedVariable(value: UndefinedVariableBehavior): Builder {
            undefinedVariable = value
            return this
        }

        fun build(): CompileOptions = CompileOptions(
            undefinedVariable)
    }
}
