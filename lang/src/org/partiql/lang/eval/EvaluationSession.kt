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

        private var globals: Bindings = Bindings.EMPTY
        fun globals(value: Bindings): Builder {
            globals = value
            return this
        }

        fun build(): EvaluationSession = EvaluationSession(now = now ?: Timestamp.nowZ(),
                                                           globals = globals)
    }
}
