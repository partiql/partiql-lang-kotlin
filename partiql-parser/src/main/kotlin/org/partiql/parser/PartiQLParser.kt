package org.partiql.parser

import org.partiql.ast.Statement

/**
 * PartiQL Parser interface.
 *
 */
interface PartiQLParser {

    @Throws(PartiQLParseException::class, InterruptedException::class)
    fun parse(source: String): Statement
}
