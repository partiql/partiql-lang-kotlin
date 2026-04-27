package org.partiql.parser.internal

import org.antlr.v4.runtime.BailErrorStrategy
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.junit.jupiter.api.fail
import org.partiql.parser.internal.antlr.PartiQLParser as GeneratedParser
import org.partiql.parser.internal.antlr.PartiQLTokens as GeneratedLexer

/**
 * This test case cares about whether the [input] can be parsed through SLL mode.
 */
class ParserTestCaseSLL(
    private val name: String,
    private val input: String,
    private val sllValid: Boolean
) : PTestDef {
    override fun name(): String = name

    override fun assert() {
        val lexer = GeneratedLexer(CharStreams.fromString(input))
        val tokens = CommonTokenStream(lexer)
        val parser = GeneratedParser(tokens)
        parser.removeErrorListeners()
        parser.interpreter.predictionMode = PredictionMode.SLL
        parser.errorHandler = BailErrorStrategy()
        if (sllValid) {
            try {
                parser.statements()
            } catch (_: ParseCancellationException) {
                fail { "Expect success, but parsing the statement under SLL mode failed." }
            }
        } else {
            try {
                parser.statements()
                fail { "Expect failure, but parsing the statement under SLL mode succeeded." }
            } catch (_: ParseCancellationException) { }
        }
    }

    override fun toString(): String {
        return name
    }
}
