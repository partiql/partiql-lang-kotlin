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
import org.partiql.spi.fn.FnExperimental

/**
 * Aids in retrieving relevant Catalog metadata for the purpose of planning and execution.
 */
public interface ConnectorMetadata {

    /**
     * Given a [BindingPath], returns a [ConnectorHandle] that corresponds to the longest-available requested path.
     * For example, given an object named "Object" located within Catalog "AWS" and Namespace "a".b"."c", a user could
     * call [getObject] with the [path] of "a"."b"."c"."Object". The returned [ConnectorHandle] will contain
     * the object representation and the matching path: "a"."b"."c"."Object"
     *
     * As another example, consider an object within a Namespace that may be a Struct with nested attributes. A user could
     * call [getObject] with the [path] of "a"."b"."c"."Object"."x". In the Namespace, only object "Object" exists.
     * Therefore, this method will return a [ConnectorHandle] with the "Object" representation and the matching
     * path: "a"."b"."c"."Object". The returned [ConnectorHandle.path] must be correct for correct
     * evaluation.
     *
     * If the [path] does not correspond to an existing [ConnectorObject], implementers should return null.
     */
    public fun getObject(path: BindingPath): ConnectorHandle.Obj?

    /**
     * Returns all function signatures matching the given path.
     *
     * @param path
     * @return
     */
    @FnExperimental
    public fun getFunction(path: BindingPath): ConnectorHandle.Fn?

    /**
     * Returns all aggregation function signatures matching the given name.
     *
     * @param path
     * @return
     */
    @FnExperimental
    public fun getAggregation(path: BindingPath): ConnectorHandle.Agg?
}
