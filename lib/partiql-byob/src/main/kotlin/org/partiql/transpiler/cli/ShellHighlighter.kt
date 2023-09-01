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
package org.partiql.transpiler.cli

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.jline.reader.Highlighter
import org.jline.reader.LineReader
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle
import org.partiql.parser.antlr.PartiQLParser
import org.partiql.parser.antlr.PartiQLTokens
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

/**
 * COPY-PASTED FROM partiql-cli, credits to johnedquinn@, only slightly modified
 */
internal class ShellHighlighter : Highlighter {

    companion object {
        private val ALLOWED_SUFFIXES = setOf<String>()

        private val STYLE_COMMAND = AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN)
        private val STYLE_KEYWORD = AttributedStyle.BOLD.foreground(AttributedStyle.CYAN).bold()
        private val STYLE_DATATYPE = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN)
        private val STYLE_IDENTIFIER = AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT)
        private val STYLE_STRING = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW)
        private val STYLE_NUMBER = AttributedStyle.DEFAULT.foreground(AttributedStyle.BLUE)
        private val STYLE_COMMENT = AttributedStyle.DEFAULT.foreground(AttributedStyle.BRIGHT).italic()
        private val STYLE_ERROR = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)
    }

    override fun highlight(reader: LineReader, line: String): AttributedString {
        val input = line
        if (input.isBlank() || input.startsWith("\\")) {
            // short-circuit for command
            return AttributedString(line)
        }

        // Temporarily Remove Allowed Suffix from Input
        val lastNewlineIndex = input.indexOfLast { c -> c == '\n' }
        val suffixString = input.substring(lastNewlineIndex + 1)
        val lastValidQueryIndex = when (lastNewlineIndex > 0 && ALLOWED_SUFFIXES.contains(suffixString)) {
            true -> lastNewlineIndex + 1
            false -> input.length
        }
        val usableInput = input.substring(0, lastValidQueryIndex)

        // Build Token Colors (Last Token is EOF)
        val tokenIter = getTokenStream(usableInput).also { it.fill() }.tokens.iterator()
        var builder = AttributedStringBuilder()
        while (tokenIter.hasNext()) {
            val token = tokenIter.next()
            val (type, text) = token.type to token.text
            when {
                isUnrecognized(type) -> builder.styled(STYLE_ERROR, text)
                isDatatype(type) -> builder.styled(STYLE_DATATYPE, text)
                isIdentifier(type) -> builder.styled(STYLE_IDENTIFIER, text)
                isString(type) -> builder.styled(STYLE_STRING, text)
                isLiteral(type) -> builder.styled(STYLE_NUMBER, text)
                isComment(type) -> builder.styled(STYLE_COMMENT, text)
                isIonMode(type, text) -> builder.append(text)
                isKeyword(type, text) -> builder.styled(STYLE_KEYWORD, text)
                isEOF(type) -> builder.styled(STYLE_ERROR, text.removeSuffix("<EOF>"))
                else -> builder.append(text)
            }
        }

        // Re-add Suffix
        if (usableInput.length < input.length) {
            builder.append(input.substring(lastValidQueryIndex))
        }

        // Create Parser
        val parser = PartiQLParser(getTokenStream(usableInput))
        parser.removeErrorListeners()
        parser.addErrorListener(RethrowErrorListener())

        // Parse and Replace Token Style if Failures
        try {
            parser.root()
        } catch (e: RethrowErrorListener.OffendingSymbolException) {
            val offending = e.offendingSymbol
            val prefix = builder.substring(0, offending.startIndex)
            val insertedStyle = AttributedString(offending.text, STYLE_ERROR)
            val suffix = builder.substring(offending.stopIndex + 1, builder.length)
            val replacementBuilder = AttributedStringBuilder()
            replacementBuilder.append(prefix)
            replacementBuilder.append(insertedStyle)
            replacementBuilder.append(suffix)
            builder = replacementBuilder
        }

        return builder.toAttributedString()
    }

    override fun setErrorPattern(errorPattern: Pattern?) {}

    override fun setErrorIndex(errorIndex: Int) {}

    /**
     * A means by which we can return the offending token during parse
     */
    private class RethrowErrorListener : BaseErrorListener() {
        @Throws(OffendingSymbolException::class)
        override fun syntaxError(
            recognizer: Recognizer<*, *>?,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            msg: String?,
            e: RecognitionException?
        ) {
            if (offendingSymbol != null && offendingSymbol is org.antlr.v4.runtime.Token && offendingSymbol.type != PartiQLParser.EOF) {
                throw OffendingSymbolException(offendingSymbol)
            }
        }

        class OffendingSymbolException(val offendingSymbol: org.antlr.v4.runtime.Token) : Exception()
    }

    private fun getTokenStream(input: String): CommonTokenStream {
        val inputStream = CharStreams.fromStream(input.byteInputStream(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
        val tokenizer = PartiQLTokens(inputStream)
        tokenizer.removeErrorListeners()
        return CommonTokenStream(tokenizer)
    }

    private fun isKeyword(type: Int, text: String): Boolean = PartiQLTokens.VOCABULARY.getSymbolicName(type).equals(text, ignoreCase = true)

    private fun isDatatype(type: Int) = when (type) {
        PartiQLTokens.SMALLINT, PartiQLTokens.INT, PartiQLTokens.INT2, PartiQLTokens.INTEGER, PartiQLTokens.INTEGER2,
        PartiQLTokens.INT4, PartiQLTokens.INTEGER4, PartiQLTokens.INT8, PartiQLTokens.INTEGER8, PartiQLTokens.BIGINT,
        PartiQLTokens.REAL, PartiQLTokens.TIMESTAMP, PartiQLTokens.DATE, PartiQLTokens.SYMBOL, PartiQLTokens.STRING,
        PartiQLTokens.BLOB, PartiQLTokens.CLOB, PartiQLTokens.STRUCT, PartiQLTokens.TUPLE, PartiQLTokens.BAG,
        PartiQLTokens.LIST, PartiQLTokens.SEXP, PartiQLTokens.DECIMAL, PartiQLTokens.FLOAT, PartiQLTokens.CHAR,
        PartiQLTokens.CHARACTER, PartiQLTokens.VARYING, PartiQLTokens.VARCHAR, PartiQLTokens.NULL, PartiQLTokens.MISSING,
        PartiQLTokens.BOOL, PartiQLTokens.BOOLEAN, PartiQLTokens.ANY -> true
        else -> false
    }

    /**
     * ANTLR treats the Ion island mode a bit oddly -- essentially, the accumulation of tokens in Ion mode, for some reason,
     * is associated with the EOF token.
     */
    private fun isIonMode(type: Int, text: String) = type == PartiQLTokens.EOF && text.contains("<EOF>").not()

    private fun isEOF(type: Int) = type == PartiQLTokens.EOF

    private fun isIdentifier(type: Int) = when (type) {
        PartiQLTokens.IDENTIFIER,
        PartiQLTokens.IDENTIFIER_QUOTED -> true
        else -> false
    }

    private fun isString(type: Int) = when (type) {
        PartiQLTokens.LITERAL_STRING -> true
        else -> false
    }

    private fun isLiteral(type: Int) = when (type) {
        PartiQLTokens.ION_CLOSURE,
        PartiQLTokens.LITERAL_DECIMAL,
        PartiQLTokens.LITERAL_INTEGER -> true
        else -> false
    }

    private fun isComment(type: Int) = when (type) {
        PartiQLTokens.COMMENT_SINGLE,
        PartiQLTokens.COMMENT_BLOCK -> true
        else -> false
    }

    private fun isUnrecognized(type: Int) = type == PartiQLTokens.UNRECOGNIZED
}
