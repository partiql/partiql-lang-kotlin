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

import org.partiql.spi.BindingPath

/**
 * Aids in retrieving relevant Catalog metadata for the purpose of planning and execution.
 */
public interface ConnectorMetadata {

    /**
     * Given a [BindingPath], returns a [ConnectorObjectHandle] that corresponds to the longest-available requested path.
     * For example, given an object named "Object" located within Catalog "AWS" and Namespace "a".b"."c", a user could
     * call [getObject] with the [path] of "a"."b"."c"."Object". The returned [ConnectorObjectHandle] will contain
     * the object representation and the matching path: "a"."b"."c"."Object"
     *
     * As another example, consider an object within a Namespace that may be a Struct with nested attributes. A user could
     * call [getObject] with the [path] of "a"."b"."c"."Object"."x". In the Namespace, only object "Object" exists.
     * Therefore, this method will return a [ConnectorObjectHandle] with the "Object" representation and the matching
     * path: "a"."b"."c"."Object". The returned [ConnectorObjectHandle.path] must be correct for correct
     * evaluation.
     *
     * If the [path] does not correspond to an existing [ConnectorObjectType], implementers should return null.
     */
    public fun getObject(path: BindingPath): ConnectorObjectHandle?

    /**
     * Returns all matching scalar functions (potentially overloaded) at the given path.
     * For example, if the [path] is `catalog.schema.foo`, and `foo` has two implementations, this should return both implementations in any order. If it only has a single implementation, this should return a list containing the single implementation.
     * If there is not a matching function, return an empty list.
     *
     * @param path : the [BindingPath] that ends with a function's name. Example: `catalog.schema.foo` where `foo` is the function name.
     * @return
     */
    public fun getScalarFunctions(path: BindingPath): List<ConnectorFunctionHandle.Scalar>

    /**
     * Returns a list of scalar operators at the given path.
     *
     * @param path
     * @return
     */
    public fun getScalarOperators(path: BindingPath): List<ConnectorFunctionHandle.Scalar>

    /**
     * Returns a list of aggregation functions at the given path.
     *
     * @param path
     * @return
     */
    public fun getAggregationFunctions(path: BindingPath): List<ConnectorFunctionHandle.Aggregation>

    /**
     * A base implementation of ConnectorMetadata for use in Java as the generated interface DefaultImpls is final.
     */
    public abstract class Base : ConnectorMetadata {

        override fun getScalarFunctions(path: BindingPath): List<ConnectorFunctionHandle.Scalar> =
            emptyList()

        override fun getScalarOperators(path: BindingPath): List<ConnectorFunctionHandle.Scalar> =
            emptyList()

        override fun getAggregationFunctions(path: BindingPath): List<ConnectorFunctionHandle.Aggregation> =
            emptyList()
    }
}
