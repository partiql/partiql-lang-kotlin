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

/**
 * A [ConnectorFunctions] implementation is responsible for linking a handle to a function implementation for execution.
 */
@ConnectorFunctionExperimental
public interface ConnectorFunctions {

    public fun getScalarFunction(handle: ConnectorHandle<FunctionSignature.Scalar>): ConnectorFunction.Scalar?

    public fun getAggregationFunction(handle: ConnectorHandle<FunctionSignature.Aggregation>): ConnectorFunction.Aggregation?
}
