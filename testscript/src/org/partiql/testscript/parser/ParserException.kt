package org.partiql.testscript.parser

import org.partiql.testscript.PtsError
import org.partiql.testscript.PtsException

class ParserException(errors: List<PtsError>) : PtsException(errors)
