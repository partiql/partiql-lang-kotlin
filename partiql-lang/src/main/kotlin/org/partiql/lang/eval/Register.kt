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

/**
 * Variant type over elements of a [RegisterBank].
 *
 * Currently this supports registers that are [ExprAggregator], but may be expanded in the future
 * to support other things like [ExprValue] for things like optimized local variable access.
 */
internal abstract class Register {
    companion object {
        /** The empty register. */
        val EMPTY = object : Register() {
            override val aggregator: ExprAggregator
                get() = throw UnsupportedOperationException("Register is not an aggregator")
        }
    }

    /** The [ExprAggregator] value stored in this register. */
    abstract val aggregator: ExprAggregator
}
