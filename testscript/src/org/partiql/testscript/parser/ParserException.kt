package org.partiql.testscript.parser

import org.partiql.testscript.PtsError
import org.partiql.testscript.PtsException

class ParserException(errors: List<PtsError>) : PtsException(errors) {
    override val message: String = "Errors found when parsing test scripts:\n$formattedErrors"
}
