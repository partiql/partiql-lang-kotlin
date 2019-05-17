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

// TODO consider making this API stateless and purely functional, AVG makes this ugly

/**
 * Defines an aggregate function in the evaluator in terms of a stateful accumulator.
 * An aggregate function is always unary, and effectively operates over a collection
 * (e.g. `BAG`/`LIST`) of values.  This API defines the accumulator function over elements of the
 * operand.  The evaluator's responsibility is to effectively compile this definition
 * into a form of [ExprFunction] that operates over the collection as an [ExprValue].
 */
interface ExprAggregator {
    /** Accumulates the next value into this [ExprAggregator]. */
    fun next(value: ExprValue)

    /** Digests the result of the accumulated values. */
    fun compute(): ExprValue
}
