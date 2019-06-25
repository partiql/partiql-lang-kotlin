package org.partiql.testscript

import org.partiql.testscript.parser.ScriptLocation

private val ptsErrorComparator = Comparator.comparing<PtsError, String> { it.scriptLocation.inputName }
        .thenBy { it.scriptLocation.lineNum }
        .thenBy { it.message }

data class PtsError(val scriptLocation: ScriptLocation, val message: String) : Comparable<PtsError> {
    override fun compareTo(other: PtsError): Int = ptsErrorComparator.compare(this, other)

    override fun toString(): String = "$scriptLocation - $message"
}

abstract class PtsException(val errors: List<PtsError>) : RuntimeException() {
    private val formattedErrors = errors.sorted().joinToString("\n") { "    $it" }

    override val message: String = "Errors found when parsing test scripts:\n$formattedErrors"
}


