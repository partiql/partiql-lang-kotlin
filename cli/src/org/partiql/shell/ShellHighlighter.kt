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
package org.partiql.shell

import com.amazon.ion.IonString
import com.amazon.ion.system.IonSystemBuilder
import org.jline.reader.Highlighter
import org.jline.reader.LineReader
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle
import org.partiql.lang.errors.Property
import org.partiql.lang.syntax.ParserException
import org.partiql.lang.syntax.SqlLexer
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.syntax.Token
import org.partiql.lang.syntax.TokenType
import java.io.PrintStream
import java.util.regex.Pattern

private val SUCCESS: AttributedStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN)
private val ERROR: AttributedStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)
private val INFO: AttributedStyle = AttributedStyle.DEFAULT
private val WARN: AttributedStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW)

private val ALLOWED_SUFFIXES = setOf("!!")

internal class ShellHighlighter() : Highlighter {

    private val ion = IonSystemBuilder.standard().build()
    private val lexer = SqlLexer(ion)
    private val parser = SqlParser(ion)

    /**
     * Returns a highlighted string by passing the [input] string through the [lexer] and [parser] to identify and
     * highlight tokens
     */
    override fun highlight(reader: LineReader, input: String): AttributedString {
        if (input.isEmpty()) return AttributedString(input)

        // Map Between Line Number and Index
        val lineIndexesMap = mutableMapOf<Int, Int>()
        var currentLine = 0
        lineIndexesMap[currentLine] = -1
        input.forEachIndexed { index, char ->
            if (char == '\n') lineIndexesMap[++currentLine] = index
        }

        // Check for Non-PartiQL Suffixes (REPL Only)
        val lastNewlineIndex = lineIndexesMap[currentLine]!!
        val suffixString = input.substring(lastNewlineIndex + 1, input.lastIndex + 1)
        val lastValidQueryIndex = when (currentLine > 0 && ALLOWED_SUFFIXES.contains(suffixString)) {
            true -> lastNewlineIndex
            false -> input.length
        }

        // Get Tokens
        val tokens: List<Token>
        try {
            tokens = lexer.tokenize(input.substring(0, lastValidQueryIndex))
        } catch (e: Exception) {
            return AttributedString(input, AttributedStyle().foreground(AttributedStyle.RED))
        }

        // Build Token Colors (Last Token is EOF)
        var builder = AttributedStringBuilder()
        for (tokenIndex in 0..(tokens.size - 2)) {
            if (tokenIndex == tokens.lastIndex) break
            val currentToken = tokens[tokenIndex]
            val preIndex = when (tokenIndex) {
                0 -> 0
                else -> {
                    val prevToken = tokens[tokenIndex - 1]
                    (getTokenIndex(prevToken, lineIndexesMap) ?: 0) + prevToken.span.length.toInt()
                }
            }
            val postIndex = when (tokenIndex) {
                tokens.lastIndex - 1 -> input.lastIndex + 1
                else -> (getTokenIndex(currentToken, lineIndexesMap) ?: input.lastIndex) + currentToken.span.length.toInt()
            }
            addToAttributeStringBuilder(currentToken, lineIndexesMap, builder, input, preIndex, postIndex)
        }

        // Parse and Replace Token Style if Failures
        try {
            parser.parseAstStatement(input.substring(0, lastValidQueryIndex))
        } catch (e: ParserException) {
            val column =
                e.errorContext[Property.COLUMN_NUMBER]?.longValue()?.toInt() ?: return builder.toAttributedString()
            val lineNumber =
                e.errorContext[Property.LINE_NUMBER]?.longValue()?.toInt() ?: return builder.toAttributedString()
            val token = tokens.find {
                it.span.column.toInt() == column && it.span.line.toInt() == lineNumber
            } ?: return builder.toAttributedString()
            builder = createAttributeStringBuilder(token, lineIndexesMap, builder)
        }
        return builder.toAttributedString()
    }

    override fun setErrorPattern(errorPattern: Pattern?) {}

    override fun setErrorIndex(errorIndex: Int) {}

    /**
     * Based on the [token] type and location, this function will return a replica of the [stringBuilder] with the
     * token's location having an updated color-scheme (of color RED). This is used if the [parser] throws a
     * [ParserException].
     */
    private fun createAttributeStringBuilder(
        token: Token,
        lineIndexes: Map<Int, Int>,
        stringBuilder: AttributedStringBuilder
    ): AttributedStringBuilder {
        val a = AttributedStringBuilder()
        val style = AttributedStyle().foreground(AttributedStyle.RED)
        val length = token.span.length.toInt()
        val index = getTokenIndex(token, lineIndexes) ?: return a
        a.append(stringBuilder.subSequence(0, index))
        a.append(stringBuilder.subSequence(index, index + length), style)
        a.append(stringBuilder.subSequence(index + length, stringBuilder.length))
        return a
    }

    /**
     * Based on the [token] type and location, this function will modify the [stringBuilder] in place by adding a
     * [token] and its associated [AttributedStyle]. This is used by the [lexer] to create the highlighted string.
     */
    private fun addToAttributeStringBuilder(
        token: Token,
        lineIndexes: Map<Int, Int>,
        stringBuilder: AttributedStringBuilder,
        input: String,
        preIndex: Int,
        postIndex: Int
    ) {
        val style = getStyle(token)
        val length = token.span.length.toInt()
        val index = getTokenIndex(token, lineIndexes) ?: return
        stringBuilder.append(input.subSequence(preIndex, index))
        stringBuilder.append(input.subSequence(index, index + length), style)
        stringBuilder.append(input.subSequence(index + length, postIndex))
    }

    /**
     * Gets the index of a specific token
     */
    private fun getTokenIndex(token: Token, lineIndexes: Map<Int, Int>): Int? {
        val column = token.span.column.toInt()
        val lineNumber = token.span.line.toInt() - 1
        return lineIndexes[lineNumber]?.plus(column)
    }

    /**
     * Sets the color and thickness of the string based on the [token] type
     */
    private fun getStyle(token: Token): AttributedStyle {
        var style = AttributedStyle()
        val attrCode = when (token.type) {
            TokenType.KEYWORD -> if (token.isDataType) AttributedStyle.GREEN else AttributedStyle.CYAN
            TokenType.AS, TokenType.FOR, TokenType.ASC, TokenType.LAST, TokenType.DESC,
            TokenType.BY, TokenType.FIRST -> AttributedStyle.CYAN
            TokenType.LITERAL -> if (token.value is IonString) AttributedStyle.YELLOW else AttributedStyle.BLUE
            TokenType.ION_LITERAL -> AttributedStyle.YELLOW
            TokenType.OPERATOR -> AttributedStyle.WHITE
            TokenType.QUOTED_IDENTIFIER -> AttributedStyle.BRIGHT
            TokenType.IDENTIFIER -> AttributedStyle.BRIGHT
            TokenType.MISSING -> AttributedStyle.BLUE
            TokenType.NULL -> AttributedStyle.BLUE
            else -> AttributedStyle.WHITE
        }
        style = style.foreground(attrCode)

        return when (token.type) {
            TokenType.IDENTIFIER, TokenType.OPERATOR -> style.bold()
            else -> style
        }
    }
}

private fun ansi(string: String, style: AttributedStyle) = AttributedString(string, style).toAnsi()

fun PrintStream.success(string: String) {
    this.println(ansi(string, SUCCESS))
}

fun PrintStream.error(string: String) {
    this.println(ansi(string, ERROR))
}

fun PrintStream.info(string: String) {
    this.println(ansi(string, INFO))
}

fun PrintStream.warn(string: String) {
    this.println(ansi(string, WARN))
}
