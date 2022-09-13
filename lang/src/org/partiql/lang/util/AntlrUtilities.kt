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

package org.partiql.lang.util

import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue
import com.amazon.ion.system.IonSystemBuilder
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.Lexer
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.TerminalNode
import org.partiql.grammar.parser.generated.PartiQLParser
import org.partiql.grammar.parser.generated.PartiQLTokens
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.syntax.ALL_OPERATORS
import org.partiql.lang.syntax.ALL_SINGLE_LEXEME_OPERATORS
import org.partiql.lang.syntax.KEYWORDS
import org.partiql.lang.syntax.LexerException
import org.partiql.lang.syntax.MULTI_LEXEME_TOKEN_MAP
import org.partiql.lang.syntax.ParserException
import org.partiql.lang.syntax.SourceSpan
import org.partiql.lang.syntax.TYPE_ALIASES
import org.partiql.lang.syntax.TokenType
import java.math.BigInteger
import java.nio.charset.StandardCharsets

internal fun TerminalNode?.error(
    message: String,
    errorCode: ErrorCode,
    errorContext: PropertyValueMap = PropertyValueMap(),
    cause: Throwable? = null,
    ion: IonSystem = IonSystemBuilder.standard().build()
) = when (this) {
    null -> ParserException(errorCode = errorCode, errorContext = errorContext, cause = cause)
    else -> this.symbol.error(message, errorCode, errorContext, cause, ion)
}

internal fun Token?.error(
    message: String,
    errorCode: ErrorCode,
    errorContext: PropertyValueMap = PropertyValueMap(),
    cause: Throwable? = null,
    ion: IonSystem = IonSystemBuilder.standard().build()
) = when (this) {
    null -> ParserException(errorCode = errorCode, errorContext = errorContext, cause = cause)
    else -> {
        errorContext[Property.LINE_NUMBER] = this.line.toLong()
        errorContext[Property.COLUMN_NUMBER] = this.charPositionInLine.toLong() + 1
        errorContext[Property.TOKEN_TYPE] = getPartiQLTokenType(this)
        errorContext[Property.TOKEN_VALUE] = getIonValue(ion, this)
        ParserException(message, errorCode, errorContext, cause)
    }
}

internal fun getLexer(source: String, ion: IonSystem): Lexer {
    val inputStream = CharStreams.fromStream(source.byteInputStream(StandardCharsets.UTF_8), StandardCharsets.UTF_8)
    val lexer = PartiQLTokens(inputStream)
    val handler = TokenizeErrorListener()
    lexer.removeErrorListeners()
    lexer.addErrorListener(handler)
    return lexer
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

internal fun Token.toPartiQLToken(ion: IonSystem): org.partiql.lang.syntax.Token {
    val text = when (this.type) {
        PartiQLParser.EOF -> ""
        else -> this.text
    }
    val span = SourceSpan(
        line = this.line.toLong(),
        column = this.charPositionInLine.toLong() + 1L,
        length = text.length.toLong()
    )
    return org.partiql.lang.syntax.Token(
        type = getPartiQLTokenType(this),
        value = getIonValue(ion, this),
        sourceText = text,
        span = span
    )
}

/**
 * Returns the corresponding PartiQL Token Type for the ANTLR Token
 */
internal fun getPartiQLTokenType(token: Token): TokenType {
    val type = token.type
    val text = token.text
    return when {
        type == PartiQLParser.PAREN_LEFT -> TokenType.LEFT_PAREN
        type == PartiQLParser.PAREN_RIGHT -> TokenType.RIGHT_PAREN
        type == PartiQLParser.ASTERISK -> TokenType.STAR
        type == PartiQLParser.BRACKET_LEFT -> TokenType.LEFT_BRACKET
        type == PartiQLParser.BRACKET_RIGHT -> TokenType.RIGHT_BRACKET
        type == PartiQLParser.ANGLE_DOUBLE_LEFT -> TokenType.LEFT_DOUBLE_ANGLE_BRACKET
        type == PartiQLParser.ANGLE_DOUBLE_RIGHT -> TokenType.RIGHT_DOUBLE_ANGLE_BRACKET
        type == PartiQLParser.BRACE_LEFT -> TokenType.LEFT_CURLY
        type == PartiQLParser.BRACE_RIGHT -> TokenType.RIGHT_CURLY
        type == PartiQLParser.COLON -> TokenType.COLON
        type == PartiQLParser.COLON_SEMI -> TokenType.SEMICOLON
        type == PartiQLParser.LAST -> TokenType.LAST
        type == PartiQLParser.FIRST -> TokenType.FIRST
        type == PartiQLParser.AS -> TokenType.AS
        type == PartiQLParser.AT -> TokenType.AT
        type == PartiQLParser.ASC -> TokenType.ASC
        type == PartiQLParser.DESC -> TokenType.DESC
        type == PartiQLParser.NULL -> TokenType.NULL
        type == PartiQLParser.NULLS -> TokenType.NULLS
        type == PartiQLParser.MISSING -> TokenType.MISSING
        type == PartiQLParser.COMMA -> TokenType.COMMA
        type == PartiQLParser.PERIOD -> TokenType.DOT
        type == PartiQLParser.QUESTION_MARK -> TokenType.QUESTION_MARK
        type == PartiQLParser.EOF -> TokenType.EOF
        type == PartiQLParser.FOR -> TokenType.FOR
        type == PartiQLParser.BY -> TokenType.BY
        type == PartiQLParser.ION_CLOSURE -> TokenType.ION_LITERAL
        type == PartiQLParser.LITERAL_STRING -> TokenType.LITERAL
        type == PartiQLParser.LITERAL_INTEGER -> TokenType.LITERAL
        type == PartiQLParser.LITERAL_DECIMAL -> TokenType.LITERAL
        type == PartiQLParser.IDENTIFIER_QUOTED -> TokenType.QUOTED_IDENTIFIER
        type == PartiQLParser.TRUE -> TokenType.LITERAL
        type == PartiQLParser.FALSE -> TokenType.LITERAL
        ALL_SINGLE_LEXEME_OPERATORS.contains(text.toLowerCase()) -> TokenType.OPERATOR
        type == PartiQLParser.IDENTIFIER -> TokenType.IDENTIFIER
        type == PartiQLParser.IDENTIFIER_QUOTED -> TokenType.QUOTED_IDENTIFIER
        ALL_OPERATORS.contains(text.toLowerCase()) -> TokenType.OPERATOR
        MULTI_LEXEME_TOKEN_MAP.containsKey(text.toLowerCase().split("\\s+".toRegex())) -> {
            val pair = MULTI_LEXEME_TOKEN_MAP[text.toLowerCase().split("\\s+".toRegex())]!!
            pair.second
        }
        KEYWORDS.contains(text.toLowerCase()) -> TokenType.KEYWORD
        else -> TokenType.IDENTIFIER
    }
}

/**
 * Returns the corresponding [IonValue] for a particular ANTLR Token
 */
internal fun getIonValue(ion: IonSystem, token: Token): IonValue {
    val type = token.type
    val text = token.text
    return when {
        type == PartiQLParser.EOF -> ion.newSymbol("EOF")
        ALL_OPERATORS.contains(text.toLowerCase()) -> ion.newSymbol(text.toLowerCase())
        type == PartiQLParser.ION_CLOSURE -> ion.singleValue(text.trimStart('`').trimEnd('`'))
        type == PartiQLParser.TRUE -> ion.newBool(true)
        type == PartiQLParser.FALSE -> ion.newBool(false)
        type == PartiQLParser.NULL -> ion.newNull()
        type == PartiQLParser.NULLS -> ion.newSymbol("nulls")
        type == PartiQLParser.MISSING -> ion.newNull()
        type == PartiQLParser.LITERAL_STRING -> ion.newString(text.trim('\'').replace("''", "'"))
        type == PartiQLParser.LITERAL_INTEGER -> ion.newInt(BigInteger(text, 10))
        type == PartiQLParser.LITERAL_DECIMAL -> try {
            ion.newDecimal(bigDecimalOf(text))
        } catch (e: NumberFormatException) {
            throw token.error(e.localizedMessage, ErrorCode.PARSE_EXPECTED_NUMBER, cause = e, ion = ion)
        }
        type == PartiQLParser.IDENTIFIER_QUOTED -> ion.newSymbol(text.trim('\"').replace("\"\"", "\""))
        MULTI_LEXEME_TOKEN_MAP.containsKey(text.toLowerCase().split("\\s+".toRegex())) -> {
            val pair = MULTI_LEXEME_TOKEN_MAP[text.toLowerCase().split("\\s+".toRegex())]!!
            ion.newSymbol(pair.first)
        }
        KEYWORDS.contains(text.toLowerCase()) -> ion.newSymbol(TYPE_ALIASES[text.toLowerCase()] ?: text.toLowerCase())
        else -> ion.newSymbol(text)
    }
}
