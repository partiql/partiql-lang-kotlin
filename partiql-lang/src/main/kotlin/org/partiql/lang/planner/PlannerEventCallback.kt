package org.partiql.lang.planner

import java.time.Duration
import java.time.Instant

/** Called by [PartiQLPlanner] after a phase of query planning has completed. */
typealias PlannerEventCallback = (PlannerEvent) -> Unit

/** Information about a planner event. */
data class PlannerEvent(
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

/** Convenience function for optionally invoking [PlannerEventCallback] functions. */
internal inline fun <T : Any> PlannerEventCallback?.doEvent(eventName: String, input: Any, crossinline block: () -> T): T {
    if (this == null) return block()
    val startTime = Instant.now()
    return block().also { output ->
        val endTime = Instant.now()
        this(PlannerEvent(eventName, input, output, Duration.between(startTime, endTime)))
    }
}
