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
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.Token
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
import org.partiql.lang.visitors.PartiQLVisitor
import java.nio.charset.StandardCharsets
import org.partiql.grammar.parser.generated.PartiQLParser as GeneratedParser
import org.partiql.grammar.parser.generated.PartiQLTokens as GeneratedLexer

/**
 * Extends [Parser] to provide a mechanism to parse an input query string. It internally uses ANTLR's generated parser,
 * [GeneratedParser] to create an ANTLR [ParseTree] from the input query. Then, it uses the configured [PartiQLVisitor]
 * to convert the [ParseTree] into a [PartiqlAst.Statement].
 */
internal class PartiQLParser(
    private val ion: IonSystem,
    val customTypes: List<CustomType> = listOf()
) : Parser {

    override fun parseAstStatement(source: String): PartiqlAst.Statement {
        // TODO: Research use-case of parameters and implementation -- see https://github.com/partiql/partiql-docs/issues/23
        val parameterIndexes = calculateTokenToParameterOrdinals(source)
        val lexer = getLexer(source)
        val tree = try {
            parseQuery(lexer)
        } catch (e: StackOverflowError) {
            val msg = "Input query too large. This error typically occurs when there are several nested " +
                "expressions/predicates and can usually be fixed by simplifying expressions."
            throw ParserException(msg, ErrorCode.PARSE_FAILED_STACK_OVERFLOW, cause = e)
        }
        val visitor = PartiQLVisitor(ion, customTypes, parameterIndexes)
        return visitor.visit(tree) as PartiqlAst.Statement
    }

    private fun parseQuery(lexer: Lexer): ParseTree {
        val parser = getParser(lexer)
        return parser.statement()
    }

    private fun getLexer(source: String): Lexer {
        val inputStream = CharStreams.fromStream(source.byteInputStream(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
        val lexer = GeneratedLexer(inputStream)
        val handler = TokenizeErrorListener(ion)
        lexer.removeErrorListeners()
        lexer.addErrorListener(handler)
        return lexer
    }

    private fun getParser(lexer: Lexer): GeneratedParser {
        val tokens = CommonTokenStream(lexer)
        val parser = GeneratedParser(tokens)
        val handler = ParseErrorListener(ion)
        parser.removeErrorListeners()
        parser.addErrorListener(handler)
        return parser
    }

    /**
     * Create a map where the key is the index of a '?' token relative to all tokens, and the value is the index of a
     * '?' token relative to all other '?' tokens (starting at index 1). This is used for visiting.
     * NOTE: This needs to create its own lexer. Cannot share with others due to consumption of token stream.
     */
    private fun calculateTokenToParameterOrdinals(query: String): Map<Int, Int> {
        val lexer = getLexer(query)
        val tokenIndexToParameterIndex = mutableMapOf<Int, Int>()
        var parametersFound = 0
        val tokens = CommonTokenStream(lexer)
        for (i in 0 until tokens.numberOfOnChannelTokens) {
            if (tokens[i].type == GeneratedParser.QUESTION_MARK) {
                tokenIndexToParameterIndex[tokens[i].tokenIndex] = ++parametersFound
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
