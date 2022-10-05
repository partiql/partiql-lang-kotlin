package org.partiql.testscript.compiler

import org.partiql.testscript.PtsError
import org.partiql.testscript.PtsException

class CompilerException(errors: List<PtsError>) : PtsException(errors) {
    override val message: String = "Errors found when compiling test scripts:\n$formattedErrors"
}
