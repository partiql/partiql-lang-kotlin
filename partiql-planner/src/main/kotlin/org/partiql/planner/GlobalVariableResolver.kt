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

import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName

/**
 * Resolves global variables (usually tables) of the current database.
 *
 * Global variables are not limited to tables, but may be any PartiQL value assigned by the application embedding
 * PartiQL.  Most databases associate a UUID or similar unique identifier to a table.  The actual type used for the
 * unique identifier doesn't matter as long as it can be converted to and from a [String]. The values must be unique
 * within the current database.
 *
 * The term "resolution" in means to look up a global variable's unique identifier, or to indicate that it is not
 * defined in the current database.
 *
 * This interface is meant to be implemented by the application embedding PartiQL and added to the [PlannerPipeline]
 * via [PlannerPipeline.Builder.globalVariableResolver].
 */
public fun interface GlobalVariableResolver {
    /**
     * Implementations try to resolve a variable which is typically a database table to a schema
     * using [bindingName].  [bindingName] includes both the name as specified by the query author and a [BindingCase]
     * which indicates if query author included double quotes (") which mean the lookup should be case-sensitive.
     *
     * Implementations of this function must return:
     *
     * - [GlobalResolutionResult.GlobalVariable] if [bindingName] matches a global variable (typically a database table).
     * - [GlobalResolutionResult.Undefined] if no identifier matches [bindingName].
     *
     * When determining if a variable name matches a global variable, it is important to consider if the comparison
     * should be case-sensitive or case-insensitive.  @see [BindingName.bindingCase].  In the event that more than one
     * variable matches a case-insensitive [BindingName], the implementation must still select one of them
     * without providing an error. (This is consistent with Postres's behavior in this scenario.)
     *
     * Note that while [GlobalResolutionResult.LocalVariable] exists, it is intentionally marked `internal` and cannot
     * be used outside this project.
     */
    public fun resolveGlobal(bindingName: BindingName): GlobalResolutionResult

    public companion object {

        public val EMPTY: GlobalVariableResolver = GlobalVariableResolver { GlobalResolutionResult.Undefined }
    }
}
