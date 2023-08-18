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

package org.partiql.planner

/**
 * Indicates the result of an attempt to resolve a global variable to its supplied unique identifier supplied by
 * the application embedding PartiQL.
 */
public sealed class GlobalResolutionResult {
    /**
     * A success case, indicates the [uniqueId] of the match to the [BindingName] in the global scope.
     * Typically, this is defined by the storage layer.
     *
     * In the future, this will likely contain much more than just a unique id.  It might include detailed schema
     * information about global variables.
     */
    public data class GlobalVariable(val uniqueId: String) : GlobalResolutionResult()

    /** A failure case, indicates that resolution did not match any variable. */
    public object Undefined : GlobalResolutionResult()
}
