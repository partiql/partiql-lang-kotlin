/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.spi.connector

import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental

/**
 * The [ConnectorFunction] interface is used to implement user-defined-functions (UDFs).
 * UDFs can be registered to a plugin for use in the query planner and evaluator.
 */
@ConnectorFunctionExperimental
public sealed interface ConnectorFunction {

    /**
     * Defines the function's parameters and argument handling.
     */
    public val signature: FunctionSignature

    /**
     * Represents an SQL row-value expression call.
     */
    public interface Scalar : ConnectorFunction {

        /**
         * Scalar function signature.
         */
        override val signature: FunctionSignature.Scalar

        /**
         * Invoke the routine with the given arguments.
         *
         * @param args
         * @return
         */
        @OptIn(PartiQLValueExperimental::class)
        public fun invoke(args: Array<PartiQLValue>): PartiQLValue
    }

    /**
     * Represents an SQL table-value expression call.
     */
    public interface Aggregation : ConnectorFunction {

        /**
         * Aggregation function signature.
         */
        override val signature: FunctionSignature.Aggregation

        /**
         * Instantiates an accumulator for this aggregation function.
         *
         * @return
         */
        public fun accumulator(): Accumulator
    }

    public interface Accumulator {

        /**
         * Apply args to the accumulator.
         *
         * @param args
         * @return
         */
        @OptIn(PartiQLValueExperimental::class)
        public fun next(args: Array<PartiQLValue>): PartiQLValue

        /**
         * Return the accumulator value.
         *
         * @return
         */
        @OptIn(PartiQLValueExperimental::class)
        public fun value(): PartiQLValue
    }
}
