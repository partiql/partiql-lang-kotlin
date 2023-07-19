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
 * @param conditionCount represents the branch name (String) and the corresponding number of times it was executed.
 * @param lineCount represents how many times (value) a particular line number (key) was executed.
 */
public data class CoverageData(
    val conditionCount: Map<String, Int> = emptyMap(),
    val lineCount: Map<Int, Int> = emptyMap(),
    val branchCount: Map<String, Int> = emptyMap()
)
