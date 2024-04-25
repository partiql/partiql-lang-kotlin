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
import org.partiql.spi.PartiQLException
import org.partiql.spi.fn.FnExperimental
import org.partiql.types.StaticType

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
     * Lists all scopes and objects at the given path.
     */
    public fun ls(path: BindingPath): List<ConnectorHandle<*>> = emptyList()

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

    /**
     * Write Operation: Attempts to create a table into the remote binding source.
     *
     * For now: validations like `assertion on if table already exists` or `schema does not exist`
     * should be done at connector level.
     *
     * Implementation should throw a [PartiQLException] with appropriate error message when validation failed.
     *
     * @param path [BindingPath] represents the qualified table name.
     * @param shape The shape of the table to be created, represented in PartiQL Type system.
     * @param checkExpression Expression used in check constraints.
     * Note that PartiQL Planner does not have an concept of attribute level constraint,
     * all attribute constraints will be normalized to tuple level constraints.
     *  TODO: modify those constraints modeling in connector, one aspect that is missing is constraint name.
     * @param unique a list of column that has been tagged with Unique constraint.
     * @param primaryKey a list of column that has been tag with primary key.
     *
     */
    public fun createTable(
        path: BindingPath,
        shape: StaticType,
        checkExpression: List<String>,
        unique: List<String>,
        primaryKey: List<String>
    ): ConnectorHandle.Obj =
        throw PartiQLException("Create Table Operation is not Implemented for this connector")
}
