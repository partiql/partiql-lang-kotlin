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
 * Represents the execution data of a PartiQL Statement as it relates to Code Coverage. This structure specifically
 * represents the aggregation of data of a particular statement. For example, a PartiQL Statement containing a single
 * boolean expression would be represented with a [CoverageStructure] containing two branches. However, upon execution
 * of the [Expression], this structure ([CoverageData]) will be populated with information related to which branches
 * were taken and their frequency. If the execution of a compiler query results in only a single branch being taken, this
 * class shall reflect that.
 *
 * NOTE: It's important to note that many implementations of [ExprValue] are **lazy**. Therefore, in order to retrieve
 * accurate [CoverageData], one must access each individual [ExprValue] of the [PartiQLResult] before accessing the
 * [CoverageData].
 *
 * @param branchCount represents the branch name (String) and the corresponding number of times it was executed.
 * @param branchConditionCount represents the branch-condition name (String) and the corresponding number of times it was executed.
 *
 * @see CoverageStructure
 * @see ExecutionCount
 */
public data class CoverageData(
    val branchConditionCount: ExecutionCount = ExecutionCount(emptyMap()),
    val branchCount: ExecutionCount = ExecutionCount(emptyMap())
) {
    /**
     * Holds the number of times each branch or branch-condition of a [CoverageStructure] has been executed.
     * @see CoverageData
     * @see CoverageStructure
     */
    public class ExecutionCount(private val _map: Map<String, Long>) : Map<String, Long> {
        override val entries: Set<Map.Entry<String, Long>> = this._map.entries

        override val keys: Set<String> = this._map.keys

        override val size: Int = this._map.size

        override val values: Collection<Long> = this._map.values

        override fun containsKey(key: String): Boolean = this._map.containsKey(key)

        override fun containsValue(value: Long): Boolean = this._map.containsValue(value)

        override fun get(key: String): Long? = this._map[key]

        override fun isEmpty(): Boolean = this._map.isEmpty()
    }
}
