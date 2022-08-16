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
import org.antlr.v4.runtime.*
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.antlr.v4.runtime.misc.Utils
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.Tree
import org.antlr.v4.runtime.tree.Trees
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
import java.io.PrintStream
import java.nio.charset.StandardCharsets
import org.partiql.lang.generated.PartiQLParser as GeneratedParser
import org.partiql.lang.generated.PartiQLTokens as GeneratedLexer

/**
 * Extends [Parser] to provide a mechanism to parse an input query string. It internally uses ANTLR's generated parser,
 * [GeneratedParser] to create an ANTLR [ParseTree] from the input query. Then, it uses the configured [PartiQLVisitor]
 * to convert the [ParseTree] into a [PartiqlAst.Statement].
 */
class PartiQLParser(
        private val ion: IonSystem,
        val customTypes: List<CustomType> = listOf()
) : Parser {

    override fun parseAstStatement(source: String): PartiqlAst.Statement {
        val parameterIndexes = getNumberOfParameters(source)
        val lexer = getLexer(source)
        val tree = parseQuery(lexer)
        val visitor = PartiQLVisitor(ion, customTypes, parameterIndexes)
        return visitor.visit(tree) as PartiqlAst.Statement
    }

    fun parseQuery(lexer: Lexer): ParseTree {
        val parser = getParser(lexer)
        return parser.topQuery()
    }

    fun describe(source: String, out: PrintStream) {
        val parameterIndexes = getNumberOfParameters(source)

        val lexer = getLexer(source)
        val tokens = CommonTokenStream(lexer)
        val parser = GeneratedParser(tokens)
        parser.addErrorListener(ParseErrorListener(ion))
        val tree = parser.topQuery()

        out.println("==Tokens")
        tokens.tokens.forEach {
            val type = org.partiql.lang.generated.PartiQLLexer.VOCABULARY.getSymbolicName(it.type)
            out.println("$type: ${it.text}")
        }

        out.println("\n==Tree")
        out.println(toPrettyTree(tree, parser))

        // TODO
//        val visitor = PartiQLVisitor(ion, customTypes, parameterIndexes)
//        val ast = visitor.visit(tree) as PartiqlAst.Statement
//        out.println("==AST")
    }

    internal fun getLexer(source: String): Lexer {
        val inputStream = CharStreams.fromStream(source.byteInputStream(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
        val lexer = GeneratedLexer(inputStream)
        lexer.removeErrorListeners()
        lexer.addErrorListener(PartiQLLexer.TokenizeErrorListener.INSTANCE)
        return lexer
    }

    /**
     * Create a map where the key is the index of a '?' token relative to all tokens, and the value is the index of a
     * '?' token relative to all other '?' tokens (starting at index 1). This is used for visiting.
     * NOTE: This needs to create its own lexer. Cannot share with others due to consumption of token stream.
     */
    private fun getNumberOfParameters(query: String): Map<Int, Int> {
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

    fun getParser(lexer: Lexer): GeneratedParser {
        val tokens = CommonTokenStream(lexer)
        val parser = GeneratedParser(tokens)
        val handler = ParseErrorListener(ion)
        parser.removeErrorListeners()
        parser.addErrorListener(handler)
        return parser
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

    class ParseErrorListener(val ion: IonSystem) : BaseErrorListener() {
        @Throws(ParseCancellationException::class)
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

    // COW HACK
    companion object {

        private val EOL = System.lineSeparator()
        private const val INDENTS = "   "
        private const val INDENT_ROOT = "⚬"
        private const val INDENT_PIPE = "──"
        private const val INDENT_T = "├" + INDENT_PIPE
        private const val INDENT_I = "│  "
        private const val INDENT_ELBOW = "└" + INDENT_PIPE
        fun toPrettyTree(tree: Tree, parser: org.antlr.v4.runtime.Parser): String {
            return processLined(tree, parser, 0, HashSet(), true)
        }

        private fun processLined(tree: Tree, parser: org.antlr.v4.runtime.Parser, level: Int, levels: Set<Int>, isLast: Boolean): String {
            val nodeText: String = Utils.escapeWhitespace(Trees.getNodeText(tree, parser), false)
            val childCount: Int = tree.childCount
            val sb = StringBuilder()
            sb.append(getLead(level, levels, isLast)).append(' ').append(nodeText).append(EOL)
            if (childCount > 0) {
                val levelsForChildren: MutableSet<Int> = HashSet(levels)
                if (!isLast) levelsForChildren.add(level - 1)
                for (c in 0 until childCount) {
                    sb.append(processLined(tree.getChild(c), parser, level + 1, levelsForChildren, c == childCount - 1))
                }
            }
            return sb.toString()
        }

        /**
         * Constructs the leading text in a pretty tree print
         *
         * - Use "  " for empty space
         * - Use "| " when we are "carrying" a level for easy reading
         * - Use a sideways "T" for mid-level elements with no children
         * - Use an elbow "L" for end elements or those with children
         */
        private fun getLead(level: Int, levels: Set<Int>, useElbow: Boolean): String {
            if (level == 0) return INDENT_ROOT
            val sb = StringBuilder()
            for (l in 0 until level - 1) {
                sb.append(if (levels.contains(l)) INDENT_I else INDENTS)
            }
            sb.append(if (useElbow) INDENT_ELBOW else INDENT_T)
            return sb.toString()
        }

    }
}
