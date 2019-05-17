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
 * Simple functional interface to create [ExprAggregator] instances.
 *
 * This is the entry point for aggregate function definitions in the evaluator.
 */
interface ExprAggregatorFactory {
    companion object {
        fun over(func: () -> ExprAggregator): ExprAggregatorFactory =
            object : ExprAggregatorFactory {
                override fun create() = func()
            }
    }

    /** Generates a new instance of an aggregator. */
    fun create(): ExprAggregator
}
