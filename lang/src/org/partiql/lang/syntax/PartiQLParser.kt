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

import com.amazon.ion.IonSexp
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
import org.partiql.lang.ast.ExprNode
import org.partiql.lang.ast.toExprNode
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.types.CustomType
import org.partiql.lang.util.getIonValue
import org.partiql.lang.util.getPartiQLTokenType
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
     * As a performance optimization, first attempt to parse using ANTLR's fast, but less powerful [PredictionMode.SLL],
     * falling back to the slower [PredictionMode.LL] that is capable of parsing all valid ANTLR grammars.
     */
    private fun parseQuery(input: String): ParseTree = try {
        parseUsingSLL(input)
    } catch (ex: ParseCancellationException) {
        parseUsingLL(input)
    }

    internal fun parseUsingSLL(input: String): org.partiql.lang.syntax.antlr.PartiQLParser.StatementContext {
        val parser = createParserSLL(input)
        return parser.statement()
    }

    internal fun parseUsingLL(input: String): org.partiql.lang.syntax.antlr.PartiQLParser.StatementContext {
        val parser = createParserLL(input)
        return parser.statement()
    }

    private fun createTokenStream(query: String): CommonTokenStream {
        val inputStream = CharStreams.fromStream(query.byteInputStream(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
        val handler = TokenizeErrorListener(ion)
        val lexer = GeneratedLexer(inputStream)
        lexer.removeErrorListeners()
        lexer.addErrorListener(handler)
        return CommonTokenStream(lexer)
    }

    /**
     * Creates a [GeneratedParser] that uses [PredictionMode.SLL] and the [BailErrorStrategy]. The [GeneratedParser],
     * upon seeing a syntax error, will throw a [ParseCancellationException] due to the [GeneratedParser.getErrorHandler]
     * being a [BailErrorStrategy]. The purpose of this is to throw syntax errors as quickly as possible once encountered.
     * As noted by the [PredictionMode.SLL] documentation, to guarantee results, it is useful to follow-up a failed parse
     * by parsing with [PredictionMode.LL] -- see [createParserLL] for more information.
     * See the JavaDocs for [PredictionMode.SLL] and [BailErrorStrategy] for more information.
     */
    private fun createParserSLL(query: String): GeneratedParser {
        val stream = createTokenStream(query)
        val parser = GeneratedParser(stream)
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
    private fun createParserLL(query: String): GeneratedParser {
        val stream = createTokenStream(query)
        val parser = GeneratedParser(stream)
        parser.reset()
        parser.interpreter.predictionMode = PredictionMode.LL
        parser.removeErrorListeners()
        parser.addErrorListener(ParseErrorListener(ion))
        return parser
    }

    /**
     * Create a map where the key is the index of a '?' token relative to all tokens, and the value is the index of a
     * '?' token relative to all other '?' tokens (starting at index 1). This is used for visiting.
     * NOTE: This needs to create its own lexer. Cannot share with others due to consumption of token stream.
     */
    private fun calculateTokenToParameterOrdinals(query: String): Map<Int, Int> {
        val stream = createTokenStream(query)
        val tokenIndexToParameterIndex = mutableMapOf<Int, Int>()
        var parametersFound = 0
        val tokenIter = stream.also { it.fill() }.tokens.iterator()
        while (tokenIter.hasNext()) {
            val token = tokenIter.next()
            if (token.type == GeneratedParser.QUESTION_MARK) {
                tokenIndexToParameterIndex[token.tokenIndex] = ++parametersFound
            }
        }
        return tokenIndexToParameterIndex
    }

    @Deprecated("Please use parseAstStatement() instead--ExprNode is deprecated.")
    override fun parseExprNode(source: String): @Suppress("DEPRECATION") ExprNode {
        return parseAstStatement(source).toExprNode(ion)
    }

    @Deprecated("Please use parseAstStatement() instead--the return value can be deserialized to backward-compatible IonSexp.")
    override fun parse(source: String): IonSexp =
        @Suppress("DEPRECATION")
        org.partiql.lang.ast.AstSerializer.serialize(
            parseExprNode(source),
            org.partiql.lang.ast.AstVersion.V0, ion
        )

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
