package org.partiql.planner.test

import java.io.PrintStream
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

/**
 * Simple enough; raise an interface later.
 *
 * Consider formatting/color, DRY-ing this out, using an actual lib...
 */
public class PlannerTestLog(val out: PrintStream) {

    fun debug(message: String) {
        val time = OffsetDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)
        out.appendLine("DEBUG $time: $message")
    }

    fun info(message: String) {
        val time = OffsetDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)
        out.appendLine("INFO $time: $message")
    }

    fun error(message: String) {
        val time = OffsetDateTime.now().format(DateTimeFormatter.ISO_LOCAL_TIME)
        out.appendLine("ERROR $time: $message")
    }
}
