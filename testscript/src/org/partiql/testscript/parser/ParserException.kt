package org.partiql.testscript.parser

import com.amazon.ion.IonException
import org.partiql.testscript.PtsError
import org.partiql.testscript.PtsException

class ParserException(errors: List<PtsError>) : PtsException(errors) {
    override val message: String = "Errors found when parsing test scripts:\n$formattedErrors"
}

class ParserIonException(filePath: String, e: IonException) : PtsException(exception = e) {
    override val message: String = "IonException on file $filePath: ${e.message}"
}