package org.partiql.parser.internal

import org.partiql.parser.PartiQLParser

/**
 * This test case simply cares about whether the [input] can be parsed or not.
 */
class ParserTestCaseSimple(
    private val name: String,
    private val input: String,
    private val isValid: Boolean = true
) : PTestDef {

    override fun name(): String = name

    private val parser: PartiQLParser = PartiQLParser.standard()

    override fun assert() {
        when (isValid) {
            true -> parser.parse(input)
            false -> {
                try {
                    parser.parse(input)
                    throw AssertionError("Expected parse failure for input: $input")
                } catch (e: Exception) {
                    // Expected exception
                }
            }
        }
    }
}
