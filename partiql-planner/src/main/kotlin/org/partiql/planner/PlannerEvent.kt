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

import java.time.Duration

/** Information about a planner event. */
public data class PlannerEvent(
    /** The name of the event. */
    val eventName: String,
    /** The input to the pass, e.g. the SQL query text or instance of the AST or query plan.*/
    val input: Any,
    /** The output of the pass, e.g., the AST or rewritten query plan. */
    val output: Any,
    /** The duration of the pass. */
    val duration: Duration
) {
    override fun toString(): String =
        StringBuilder().let { sb ->
            println("event:    $eventName")
            // NOTE: please do not be surprised by the durations shown here if they are from the first
            // run in an instance of the JVM.  Those durations are 50-100x longer than subsequent runs
            // and should improve vastly once the JIT warms up.
            sb.appendLine("duration: $duration")
            sb.appendLine("input:\n$input")
            sb.append("output:\n$output")
            sb.toString()
        }
}
