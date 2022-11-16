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
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.antlr.v4.runtime.tree.ParseTree
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.types.CustomType
import org.partiql.lang.util.getIonValue
import org.partiql.lang.util.getPartiQLTokenType
import java.io.InputStream
import java.nio.charset.StandardCharsets
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

    private val charStream = CharStreams.fromStream(InputStream.nullInputStream())
    private val lexer = GeneratedLexer(charStream)
    private val tokens = CommonTokenStream(lexer)
    private val parserSLL = GeneratedParser(tokens)
    private val parserLL = GeneratedParser(tokens)

    override fun parseAstStatement(source: String): PartiqlAst.Statement {
        // TODO: Research use-case of parameters and implementation -- see https://github.com/partiql/partiql-docs/issues/23
        val parameterIndexes = calculateTokenToParameterOrdinals(source)
        val tree = try {
            parseQuery(source)
        } catch (e: StackOverflowError) {
            val msg = "Input query too large. This error typically occurs when there are several nested " +
                "expressions/predicates and can usually be fixed by simplifying expressions."
            throw ParserException(msg, ErrorCode.PARSE_FAILED_STACK_OVERFLOW, cause = e)
        }
        val visitor = PartiQLVisitor(ion, customTypes, parameterIndexes)
        return visitor.visit(tree) as PartiqlAst.Statement
    }

    /**
     * To reduce latency costs, the [PartiQLParser] attempts to use [PredictionMode.SLL] and falls back to
     * [PredictionMode.LL] if a [ParseCancellationException] is thrown by the [BailErrorStrategy].
     */
    private fun parseQuery(input: String): ParseTree = try {
        parseUsingSLL(input)
    } catch (ex: ParseCancellationException) {
        parseUsingLL(input)
    }

    internal fun parseUsingSLL(input: String): org.partiql.lang.syntax.antlr.PartiQLParser.StatementContext {
        resetParserSLL(input)
        return this.parserSLL.statement()
    }

    internal fun parseUsingLL(input: String): org.partiql.lang.syntax.antlr.PartiQLParser.StatementContext {
        resetParserLL(input)
        return this.parserLL.statement()
    }

    private fun resetLexer(query: String) {
        val inputStream = CharStreams.fromStream(query.byteInputStream(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
        val handler = TokenizeErrorListener(ion)
        this.lexer.removeErrorListeners()
        this.lexer.addErrorListener(handler)
        this.lexer.inputStream = inputStream
        this.lexer.reset()
        this.tokens.tokenSource = this.lexer
        this.tokens.seek(0)
    }

    private fun resetParserSLL(query: String) {
        resetLexer(query)
        this.parserSLL.reset()
        this.parserSLL.interpreter.predictionMode = PredictionMode.SLL
        this.parserSLL.removeErrorListeners()
        this.parserSLL.errorHandler = BailErrorStrategy()
    }

    private fun resetParserLL(query: String) {
        resetLexer(query)
        this.parserLL.reset()
        this.parserLL.interpreter.predictionMode = PredictionMode.LL
        this.parserLL.removeErrorListeners()
        this.parserLL.addErrorListener(ParseErrorListener(ion))
    }

    /**
     * Create a map where the key is the index of a '?' token relative to all tokens, and the value is the index of a
     * '?' token relative to all other '?' tokens (starting at index 1). This is used for visiting.
     * NOTE: This needs to create its own lexer. Cannot share with others due to consumption of token stream.
     */
    private fun calculateTokenToParameterOrdinals(query: String): Map<Int, Int> {
        resetLexer(query)
        val tokenIndexToParameterIndex = mutableMapOf<Int, Int>()
        var parametersFound = 0
        val tokenIter = this.tokens.also { it.fill() }.tokens.iterator()
        while (tokenIter.hasNext()) {
            val token = tokenIter.next()
            if (token.type == GeneratedParser.QUESTION_MARK) {
                tokenIndexToParameterIndex[token.tokenIndex] = ++parametersFound
            }
        }
        return tokenIndexToParameterIndex
    }

    /**
     * Catches Lexical errors (unidentified tokens) and throws a [LexerException]
     */
    private class TokenizeErrorListener(val ion: IonSystem) : BaseErrorListener() {
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
}
