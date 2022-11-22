/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.lang.syntax

import com.amazon.ion.IonSystem
import com.amazon.ion.system.IonSystemBuilder
import org.antlr.v4.runtime.BailErrorStrategy
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.TokenSource
import org.antlr.v4.runtime.TokenStream
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.antlr.v4.runtime.tree.ParseTree
import org.partiql.lang.SqlException
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.types.CustomType
import org.partiql.lang.util.checkThreadInterrupted
import org.partiql.lang.util.getIonValue
import org.partiql.lang.util.getPartiQLTokenType
import java.io.InputStream
import java.nio.channels.ClosedByInterruptException
import java.nio.charset.StandardCharsets
import kotlin.jvm.Throws
import org.partiql.lang.syntax.antlr.PartiQLParser as GeneratedParser
import org.partiql.lang.syntax.antlr.PartiQLTokens as GeneratedLexer

/**
 * Extends [Parser] to provide a mechanism to parse an input query string. It internally uses ANTLR's generated parser,
 * [GeneratedParser] to create an ANTLR [ParseTree] from the input query. Then, it uses the configured [PartiQLVisitor]
 * to convert the [ParseTree] into a [PartiqlAst.Statement].
 */
internal class PartiQLParser(
    private val ion: IonSystem = IonSystemBuilder.standard().build(),
    val customTypes: List<CustomType> = listOf()
) : Parser {

    @Throws(ParserException::class, InterruptedException::class)
    override fun parseAstStatement(source: String): PartiqlAst.Statement {
        try {
            return parseQuery(source)
        } catch (throwable: Throwable) {
            when (throwable) {
                is ParserException -> throw throwable
                is SqlException -> throw ParserException(
                    "Intercepted PartiQL exception.",
                    throwable.errorCode,
                    cause = throwable,
                    errorContext = throwable.errorContext
                )
                is StackOverflowError -> {
                    val msg = "Input query too large. This error typically occurs when there are several nested " +
                        "expressions/predicates and can usually be fixed by simplifying expressions."
                    throw ParserException(msg, ErrorCode.PARSE_FAILED_STACK_OVERFLOW, cause = throwable)
                }
                is InterruptedException -> throw throwable
                else -> throw ParserException("Unhandled exception.", ErrorCode.INTERNAL_ERROR, cause = throwable)
            }
        }
    }

    /**
     * To reduce latency costs, the [PartiQLParser] attempts to use [PredictionMode.SLL] and falls back to
     * [PredictionMode.LL] if a [ParseCancellationException] is thrown by the [BailErrorStrategy]. See [createParserSLL]
     * and [createParserLL] for more information.
     */
    private fun parseQuery(input: String) = try {
        parseQuery(input) { createParserSLL(it) }
    } catch (ex: ParseCancellationException) {
        parseQuery(input) { createParserLL(it) }
    }

    /**
     * Parses an input string [input] using whichever parser [parserInit] creates.
     */
    internal fun parseQuery(input: String, parserInit: (TokenStream) -> InterruptibleParser): PartiqlAst.Statement {
        val queryStream = createInputStream(input)
        val tokenStream = createTokenStream(queryStream)
        val parser = parserInit(tokenStream)
        val tree = parser.statement()
        val visitor = PartiQLVisitor(ion, customTypes, tokenStream.parameterIndexes)
        return visitor.visit(tree) as PartiqlAst.Statement
    }

    private fun createInputStream(input: String) = input.byteInputStream(StandardCharsets.UTF_8)

    internal fun createTokenStream(queryStream: InputStream): CountingTokenStream {
        val inputStream = try {
            CharStreams.fromStream(queryStream)
        } catch (ex: ClosedByInterruptException) {
            throw InterruptedException()
        }
        val handler = TokenizeErrorListener()
        val lexer = GeneratedLexer(inputStream)
        lexer.removeErrorListeners()
        lexer.addErrorListener(handler)
        return CountingTokenStream(lexer)
    }

    /**
     * Creates a [GeneratedParser] that uses [PredictionMode.SLL] and the [BailErrorStrategy]. The [GeneratedParser],
     * upon seeing a syntax error, will throw a [ParseCancellationException] due to the [GeneratedParser.getErrorHandler]
     * being a [BailErrorStrategy]. The purpose of this is to throw syntax errors as quickly as possible once encountered.
     * As noted by the [PredictionMode.SLL] documentation, to guarantee results, it is useful to follow-up a failed parse
     * by parsing with [PredictionMode.LL] -- see [createParserLL] for more information.
     * See the JavaDocs for [PredictionMode.SLL] and [BailErrorStrategy] for more information.
     */
    internal fun createParserSLL(stream: TokenStream): InterruptibleParser {
        val parser = InterruptibleParser(stream)
        parser.reset()
        parser.interpreter.predictionMode = PredictionMode.SLL
        parser.removeErrorListeners()
        parser.errorHandler = BailErrorStrategy()
        return parser
    }

    /**
     * Creates a [GeneratedParser] that uses [PredictionMode.LL]. This method is capable of parsing all valid inputs
     * for a grammar, but is slower than [PredictionMode.SLL]. Upon seeing a syntax error, this parser throws a
     * [ParserException].
     */
    internal fun createParserLL(stream: TokenStream): InterruptibleParser {
        val parser = InterruptibleParser(stream)
        parser.reset()
        parser.interpreter.predictionMode = PredictionMode.LL
        parser.removeErrorListeners()
        parser.addErrorListener(ParseErrorListener(ion))
        return parser
    }

    /**
     * Catches Lexical errors (unidentified tokens) and throws a [LexerException]
     */
    private class TokenizeErrorListener : BaseErrorListener() {
        @Throws(LexerException::class)
        override fun syntaxError(
            recognizer: Recognizer<*, *>?,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            msg: String,
            e: RecognitionException?
        ) {
            val propertyValues = PropertyValueMap()
            propertyValues[Property.LINE_NUMBER] = line.toLong()
            propertyValues[Property.COLUMN_NUMBER] = charPositionInLine.toLong() + 1
            propertyValues[Property.TOKEN_STRING] = msg
            throw LexerException(message = msg, errorCode = ErrorCode.LEXER_INVALID_TOKEN, errorContext = propertyValues, cause = e)
        }
    }

    /**
     * Catches Parser errors (malformed syntax) and throws a [ParserException]
     */
    private class ParseErrorListener(val ion: IonSystem) : BaseErrorListener() {
        @Throws(ParserException::class)
        override fun syntaxError(
            recognizer: Recognizer<*, *>?,
            offendingSymbol: Any,
            line: Int,
            charPositionInLine: Int,
            msg: String,
            e: RecognitionException?
        ) {
            val propertyValues = PropertyValueMap()
            propertyValues[Property.LINE_NUMBER] = line.toLong()
            propertyValues[Property.COLUMN_NUMBER] = charPositionInLine.toLong() + 1
            propertyValues[Property.TOKEN_TYPE] = getPartiQLTokenType(offendingSymbol as Token)
            propertyValues[Property.TOKEN_VALUE] = getIonValue(ion, offendingSymbol)
            throw ParserException(message = msg, errorCode = ErrorCode.PARSE_UNEXPECTED_TOKEN, errorContext = propertyValues, cause = e)
        }
    }

    /**
     * A wrapped [GeneratedParser] to allow thread interruption during parse.
     */
    internal class InterruptibleParser(input: TokenStream) : GeneratedParser(input) {
        override fun enterRule(localctx: ParserRuleContext?, state: Int, ruleIndex: Int) {
            checkThreadInterrupted()
            super.enterRule(localctx, state, ruleIndex)
        }
    }

    /**
     * This token stream creates [parameterIndexes], which is a map with key-value pairs, where the keys represent the
     * indexes of all [GeneratedLexer.QUESTION_MARK]'s and the values represent their relative index amongst all other
     * [GeneratedLexer.QUESTION_MARK]'s.
     */
    internal open class CountingTokenStream(tokenSource: TokenSource) : CommonTokenStream(tokenSource) {
        val parameterIndexes = mutableMapOf<Int, Int>()
        private var parametersFound = 0
        override fun LT(k: Int): Token? {
            val token = super.LT(k)
            token?.let {
                if (it.type == GeneratedLexer.QUESTION_MARK && parameterIndexes.containsKey(token.tokenIndex).not()) {
                    parameterIndexes[token.tokenIndex] = ++parametersFound
                }
            }
            return token
        }
    }
}
