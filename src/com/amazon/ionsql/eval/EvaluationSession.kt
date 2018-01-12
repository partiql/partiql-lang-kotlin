package com.amazon.ionsql.eval

import com.amazon.ion.*

/**
 * Evaluation Session. Holds user defined constants used during evaluation. Each value has a default value that can
 * be overridden by the client
 *
 * @param globals The global bindings. Defaults to [Bindings.empty]
 * @param now Timestamp to consider as the current time, used by functions like `utcnow()` and `now()`. Defaults to [Timestamp.nowZ]
 */
class EvaluationSession private constructor(val globals: Bindings,
                                            val now: Timestamp) {
    companion object {
        /**
         * Java style builder to construct a new [EvaluationSession]. Uses the default value for any non specified field
         */
        @JvmStatic
        fun builder() = Builder()

        /**
         * Kotlin style builder for an [EvaluationSession]. Uses the default value for any non specified field
         */
        fun build(block: Builder.() -> Unit) = Builder().apply(block).build()

        /**
         * Builds a [EvaluationSession] using standard values for all fields
         */
        @JvmStatic
        fun standard() = builder().build()
    }

    class Builder {
        private fun Timestamp.toUtc() = this.withLocalOffset(0)!!

        // using null to postpone defaulting to when the session is created
        private var now: Timestamp? = null
        fun now(value: Timestamp): Builder {
            now = value.toUtc()
            return this
        }

        private var globals: Bindings = Bindings.empty()
        fun globals(value: Bindings): Builder {
            globals = value
            return this
        }

        fun build(): EvaluationSession = EvaluationSession(now = now ?: Timestamp.nowZ(),
                                                           globals = globals)
    }
}
