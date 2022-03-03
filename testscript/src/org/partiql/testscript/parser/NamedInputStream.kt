package org.partiql.testscript.parser

import java.io.InputStream

/**
 * A named [InputStream]
 */
class NamedInputStream(val name: String, private val inputStream: InputStream) : InputStream() {
    override fun read(): Int = inputStream.read()
}
