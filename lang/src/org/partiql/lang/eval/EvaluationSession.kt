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
import java.time.ZoneOffset

/**
 * Evaluation Session. Holds user defined constants used during evaluation. Each value has a default value that can
 * be overridden by the client
 *
 * @property globals The global bindings. Defaults to [Bindings.empty]
 * @property parameters List of parameters to be substituted for positional placeholders
 * @property now Timestamp to consider as the current time, used by functions like `utcnow()` and `now()`. Defaults to [Timestamp.nowZ]
 * @property defaultTimezoneOffset Default timezone offset to be used when TIME WITH TIME ZONE does not explicitily specify the time zone. Defaults to [ZoneOffset.UTC]
 */
class EvaluationSession private constructor(val globals: Bindings<ExprValue>,
                                            val parameters: List<ExprValue>,
                                            val now: Timestamp,
                                            val defaultTimezoneOffset: ZoneOffset) {
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

        private var globals: Bindings<ExprValue> = Bindings.empty()
        fun globals(value: Bindings<ExprValue>): Builder {
            globals = value
            return this
        }

        private var parameters: List<ExprValue> = listOf()
        fun parameters(value: List<ExprValue>): Builder {
            parameters = value
            return this
        }

        private var defaultTimezoneOffset: ZoneOffset = ZoneOffset.UTC
        fun defaultTimezoneOffset(value: ZoneOffset): Builder {
            defaultTimezoneOffset = value
            return this
        }

        fun build(): EvaluationSession = EvaluationSession(now = now ?: Timestamp.nowZ(),
                                                           parameters = parameters,
                                                           globals = globals,
                                                           defaultTimezoneOffset = defaultTimezoneOffset)
    }
}
