/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
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
 * Represents the static structure of a compiled [Expression]. For example, a PartiQL query that contains a single
 * boolean expression will contain a [branchCount] of 2. This structure is distinct from the execution data found
 * within [CoverageData].
 *
 * @param branchCount the total number of branches found in the PartiQL Statement
 * @param branchLocations represents the line numbers (value) for each branch name (key)
 */
public data class CoverageStructure(
    val branchCount: Int = 0,
    val branchLocations: Map<String, Int> = emptyMap(),
    val conditionCount: Int = 0,
    val conditionLocations: Map<String, Int> = emptyMap()
)
