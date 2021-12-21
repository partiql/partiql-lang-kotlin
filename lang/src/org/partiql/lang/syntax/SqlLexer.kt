/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.syntax

import com.amazon.ion.IonException
import com.amazon.ion.IonSystem
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.util.bigDecimalOf
import org.partiql.lang.util.codePointSequence
import org.partiql.lang.util.seal
import java.math.BigInteger

/**
 * Simple tokenizer for PartiQL.
 */
class SqlLexer(private val ion: IonSystem) : Lexer {
    /** Transition types. */
    internal enum class StateType(val beginsToken: Boolean = false,
                                  val endsToken: Boolean = false) {
        /** Indicates the initial state for recognition. */
        INITIAL(),
        /** Indicates an error state. */
        ERROR(),
        /** Indicates the end of the stream */
        END(beginsToken = true),
        /** Indicates the middle of a token. */
        INCOMPLETE(),
        /** Indicates the begining of a new token. */
        START(beginsToken = true),
        /** A state that is both a start and terminal. */
        START_AND_TERMINAL(beginsToken = true, endsToken = true),
        /** Indicates a possible termination point for a token. */
        TERMINAL(endsToken = true),
    }

    /** Lexer types. */
    internal enum class LexType {
        /** No particular lexer type. */
        NONE,
        /* Integral number. */
        INTEGER,
        /* Decimal number. */
        DECIMAL,
        /** Single quoted string. */
        SQ_STRING,
        /** Double quoted string. */
        DQ_STRING,
        /** Ion literals. */
        ION_LITERAL,
        /** Whitespace. */
        WHITESPACE,
    }

    /** Representation of a Lexer state machine node. */
    internal interface State {
        val stateType: StateType
        val tokenType: TokenType?
            get() = null
        val lexType: LexType
            get() = LexType.NONE
        val replacement: Int
            get() = REPLACE_SAME

        /** Retrieves the next state from this one with the transition code point. */
        operator fun get(next: Int): State
    }

    /** Simple repeating state. */
    internal class RepeatingState(override val stateType: StateType) : State {
        override fun get(next: Int): State = this
    }

    /** State node and corresponding state table. */
    internal class TableState(override val stateType: StateType,
                              override val tokenType: TokenType? = null,
                              override val lexType: LexType = LexType.NONE,
                              override val replacement: Int = REPLACE_SAME,
                              var delegate: State = ERROR_STATE,
                              setup: TableState.() -> Unit = { }) : State {
        /** Default table with null states. */
        val table = Array<State?>(TABLE_SIZE) { null }

        init {
            setup()
        }

        operator fun Array<State?>.set(chars: String, new: State) {
            chars.forEach {
                val cp = it.toInt()
                val old = this[cp]
                this[cp] = when (old) {
                    null -> new
                    else -> throw IllegalStateException(
                        "Cannot replace existing state $old with $new")
                }
            }
        }

        private fun getFromTable(next: Int): State? = when {
            next < TABLE_SIZE -> table[next]
            else -> null
        }

        override fun get(next: Int): State = getFromTable(next) ?: delegate[next]

        fun selfRepeatingDelegate(stateType: StateType,
                                  tokenType: TokenType? = null,
                                  lexType: LexType = LexType.NONE) {
            delegate = object : State {
                override val stateType = stateType
                override val tokenType = tokenType
                override val lexType = lexType
                override fun get(next: Int): State = getFromTable(next) ?: this
            }
        }

        fun delta(chars: String,
                  stateType: StateType,
                  tokenType: TokenType? = null,
                  lexType: LexType = LexType.NONE,
                  replacement: Int = REPLACE_SAME,
                  delegate: State = this,
                  setup: TableState.(String) -> Unit = { }): TableState {
            val child = TableState(stateType, tokenType, lexType, replacement, delegate) {
                setup(chars)
            }
            table[chars] = child
            return child
        }
    }

    /** Simple line/column tracker. */
    internal class PositionTracker {
        var line = 1L
        var col = 0L
        var sawCR = false

        fun newline() {
            line++
            col = 0L
        }

        fun advance(next: Int) {
            when (next) {
                CR -> when {
                    sawCR -> newline()
                    else -> sawCR = true
                }
                LF -> {
                    newline()
                    sawCR = false
                }
                else -> {
                    if (sawCR) {
                        newline()
                        sawCR = false
                    }
                    col++
                }
            }
        }

        val position: SourcePosition
            get() = SourcePosition(line, col)

        override fun toString(): String = position.toString()
    }

    companion object {
        private val CR = '\r'.toInt()
        private val LF = '\n'.toInt()

        /** The synthetic EOF code point. */
        private val EOF = -1

        /** Code point range that table-driven lexing will operate on. */
        private val TABLE_SIZE = 127

        /** Do not replace character. */
        private val REPLACE_SAME = -1

        /** Replace with nothing. */
        private val REPLACE_NOTHING = -2

        /** Synthetic state for EOF to trigger a flush of the last token. */
        private val EOF_STATE = RepeatingState(StateType.END)

        /** Error state. */
        private val ERROR_STATE = RepeatingState(StateType.ERROR)

        /** Initial state. */
        private val INITIAL_STATE = TableState(StateType.INITIAL) {
            val initialState = this

            delta("(", StateType.START_AND_TERMINAL, TokenType.LEFT_PAREN)
            delta(")", StateType.START_AND_TERMINAL, TokenType.RIGHT_PAREN)
            delta("[", StateType.START_AND_TERMINAL, TokenType.LEFT_BRACKET)
            delta("]", StateType.START_AND_TERMINAL, TokenType.RIGHT_BRACKET)
            delta("{", StateType.START_AND_TERMINAL, TokenType.LEFT_CURLY)
            delta("}", StateType.START_AND_TERMINAL, TokenType.RIGHT_CURLY)
            delta(":", StateType.START_AND_TERMINAL, TokenType.COLON)
            delta(",", StateType.START_AND_TERMINAL, TokenType.COMMA)
            delta("*", StateType.START_AND_TERMINAL, TokenType.STAR)
            delta(";", StateType.START_AND_TERMINAL, TokenType.SEMICOLON)
            delta("?", StateType.START_AND_TERMINAL, TokenType.QUESTION_MARK)

            delta(NON_OVERLOADED_OPERATOR_CHARS, StateType.START_AND_TERMINAL, TokenType.OPERATOR)

            delta("|", StateType.START) {
                delta("|", StateType.TERMINAL, TokenType.OPERATOR, delegate = initialState)
            }
            delta("!", StateType.START) {
                delta("=", StateType.TERMINAL, TokenType.OPERATOR, delegate = initialState)
            }
            delta("<", StateType.START_AND_TERMINAL, TokenType.OPERATOR) {
                delta("=", StateType.TERMINAL, TokenType.OPERATOR, delegate = initialState)
                delta(">", StateType.TERMINAL, TokenType.OPERATOR, delegate = initialState)
                delta("<", StateType.TERMINAL, TokenType.LEFT_DOUBLE_ANGLE_BRACKET, delegate = initialState)
            }
            delta(">", StateType.START_AND_TERMINAL, TokenType.OPERATOR) {
                delta("=", StateType.TERMINAL, TokenType.OPERATOR, delegate = initialState)
                delta(">", StateType.TERMINAL, TokenType.RIGHT_DOUBLE_ANGLE_BRACKET, delegate = initialState)
            }

            delta(IDENT_START_CHARS, StateType.START_AND_TERMINAL, TokenType.IDENTIFIER) {
                delta(IDENT_CONTINUE_CHARS, StateType.TERMINAL, TokenType.IDENTIFIER)
            }

            fun TableState.deltaDecimalInteger(stateType: StateType, lexType: LexType, setup: TableState.(String) -> Unit = { }): Unit {
                delta(DIGIT_CHARS, stateType, TokenType.LITERAL, lexType, delegate = initialState) {
                    delta(DIGIT_CHARS, StateType.TERMINAL, TokenType.LITERAL, lexType)
                    setup(it)
                }
            }

            fun TableState.deltaDecimalFraction(setup: TableState.(String) -> Unit = { }): Unit {
                delta(".", StateType.TERMINAL, TokenType.LITERAL, LexType.DECIMAL) {
                    deltaDecimalInteger(StateType.TERMINAL, LexType.DECIMAL, setup)
                }
            }

            fun TableState.deltaExponent(setup: TableState.(String) -> Unit = { }): Unit {
                delta(E_NOTATION_CHARS, StateType.INCOMPLETE, delegate = ERROR_STATE) {
                    delta(SIGN_CHARS, StateType.INCOMPLETE, delegate = ERROR_STATE) {
                        deltaDecimalInteger(StateType.TERMINAL, LexType.DECIMAL, setup)
                    }
                    deltaDecimalInteger(StateType.TERMINAL, LexType.DECIMAL, setup)
                }
            }

            fun TableState.deltaNumber(stateType: StateType) {
                deltaDecimalInteger(stateType, LexType.INTEGER) {
                    deltaDecimalFraction {
                        deltaExponent { }
                    }
                    deltaExponent { }
                }
                when (stateType) {
                    StateType.START_AND_TERMINAL -> {
                        // at the top-level we need to support dot as a special
                        delta(".", StateType.START_AND_TERMINAL, TokenType.DOT) {
                            deltaDecimalInteger(StateType.TERMINAL, LexType.DECIMAL) {
                                deltaExponent { }
                            }
                        }
                    }
                    else -> {
                        deltaDecimalFraction {
                            deltaExponent { }
                        }
                    }
                }
            }

            deltaNumber(StateType.START_AND_TERMINAL)

            fun TableState.deltaQuote(quoteChar: String, tokenType: TokenType, lexType: LexType): Unit {
                delta(quoteChar, StateType.START, replacement = REPLACE_NOTHING) {
                    selfRepeatingDelegate(StateType.INCOMPLETE)
                    val quoteState = this
                    delta(quoteChar, StateType.TERMINAL, tokenType, lexType = lexType, replacement = REPLACE_NOTHING, delegate = initialState) {
                        delta(quoteChar, StateType.INCOMPLETE, delegate = quoteState)
                    }
                }
            }

            deltaQuote(SINGLE_QUOTE_CHARS, TokenType.LITERAL, LexType.SQ_STRING)
            deltaQuote(DOUBLE_QUOTE_CHARS, TokenType.QUOTED_IDENTIFIER, LexType.DQ_STRING)

            // Ion literals - very partial lexing of Ion to support nested back-tick
            // in Ion strings/symbols/comments
            delta(BACKTICK_CHARS, StateType.START, replacement = REPLACE_NOTHING) {
                selfRepeatingDelegate(StateType.INCOMPLETE)
                val quoteState = this

                delta("/", StateType.INCOMPLETE) {
                    delta("/", StateType.INCOMPLETE) {
                        val ionCommentState = this
                        selfRepeatingDelegate(StateType.INCOMPLETE)
                        delta(BACKTICK_CHARS, StateType.INCOMPLETE, delegate = ionCommentState)
                        delta(NL_WHITESPACE_CHARS, StateType.INCOMPLETE, delegate = quoteState)
                    }
                    delta("*",  StateType.INCOMPLETE) {
                        val ionCommentState = this
                        selfRepeatingDelegate(StateType.INCOMPLETE)
                        delta(BACKTICK_CHARS, StateType.INCOMPLETE, delegate = ionCommentState)
                        delta("*", StateType.INCOMPLETE) {
                            delta("/", StateType.INCOMPLETE, delegate = quoteState)
                        }
                    }
                }
                delta(DOUBLE_QUOTE_CHARS, StateType.INCOMPLETE) {
                    val ionStringState = this
                    selfRepeatingDelegate(StateType.INCOMPLETE)

                    delta("\\", StateType.INCOMPLETE) {
                        delta(DOUBLE_QUOTE_CHARS, StateType.INCOMPLETE, delegate = ionStringState)
                    }
                    delta(BACKTICK_CHARS, StateType.INCOMPLETE, delegate = ionStringState)
                    delta(DOUBLE_QUOTE_CHARS, StateType.INCOMPLETE, delegate = quoteState)
                }
                delta(SINGLE_QUOTE_CHARS, StateType.INCOMPLETE) {
                    val ionStringState = this
                    selfRepeatingDelegate(StateType.INCOMPLETE)

                    delta("\\", StateType.INCOMPLETE) {
                        delta(SINGLE_QUOTE_CHARS, StateType.INCOMPLETE, delegate = ionStringState)
                    }
                    delta(BACKTICK_CHARS, StateType.INCOMPLETE, delegate = ionStringState)
                    delta(SINGLE_QUOTE_CHARS, StateType.INCOMPLETE, delegate = quoteState) {
                        delta(SINGLE_QUOTE_CHARS, StateType.INCOMPLETE, delegate = ionStringState) {
                            val ionLongStringState = this
                            selfRepeatingDelegate(StateType.INCOMPLETE)

                            delta("\\", StateType.INCOMPLETE) {
                                delta(SINGLE_QUOTE_CHARS, StateType.INCOMPLETE, delegate = ionLongStringState)
                            }
                            delta(BACKTICK_CHARS, StateType.INCOMPLETE, delegate = ionLongStringState)
                            delta(SINGLE_QUOTE_CHARS, StateType.INCOMPLETE, delegate = ionLongStringState) {
                                delta(SINGLE_QUOTE_CHARS, StateType.INCOMPLETE, delegate = ionLongStringState) {
                                    delta(SINGLE_QUOTE_CHARS, StateType.INCOMPLETE, delegate = quoteState)
                                }
                            }
                        }
                    }
                }

                delta("{", StateType.INCOMPLETE) {
                    delta("{", StateType.INCOMPLETE) {
                        selfRepeatingDelegate(StateType.INCOMPLETE)
                        delta("}", StateType.INCOMPLETE) {
                            delta("}", StateType.INCOMPLETE, delegate = quoteState)
                        }
                    }
                }

                delta(BACKTICK_CHARS, StateType.TERMINAL, TokenType.ION_LITERAL, LexType.ION_LITERAL, replacement = REPLACE_NOTHING, delegate = initialState)
            }

            delta(ALL_WHITESPACE_CHARS, StateType.START_AND_TERMINAL, null, LexType.WHITESPACE)

            // block comment and divide operator
            delta("/", StateType.START_AND_TERMINAL, TokenType.OPERATOR) {
                delta("*", StateType.INCOMPLETE) {
                    selfRepeatingDelegate(StateType.INCOMPLETE)
                    delta("*", StateType.INCOMPLETE) {
                        delta("/", StateType.TERMINAL, null, LexType.WHITESPACE, delegate = initialState)
                    }
                }
            }
            // line comment, subtraction operator, and signed positive integer
            delta("-", StateType.START_AND_TERMINAL, TokenType.OPERATOR) {
                // inline comments don't need a special terminator before EOF
                delta("-", StateType.TERMINAL, null, LexType.WHITESPACE) {
                    selfRepeatingDelegate(StateType.TERMINAL, null, LexType.WHITESPACE)
                    delta(NL_WHITESPACE_CHARS, StateType.TERMINAL, null, LexType.WHITESPACE, delegate = initialState)
                }
            }

            // TODO datetime/hex/bin literals (not required for SQL-92 Entry compliance)
        }
    }

    private fun repr(codePoint: Int): String = when {
        codePoint == EOF -> "<EOF>"
        codePoint < EOF -> "<$codePoint>"
        else -> "'${String(Character.toChars(codePoint))}' [U+${Integer.toHexString(codePoint)}]"
    }

    /**
     * Given a token as a [String] and a [tracker] creates and populates a [PropertyValueMap] with line and column number as
     * well as the token string.
     */
    private fun makePropertyBag(tokenString: String, tracker: PositionTracker): PropertyValueMap {
        val pvmap = PropertyValueMap()
        pvmap[Property.LINE_NUMBER] =  tracker.line
        pvmap[Property.COLUMN_NUMBER] =  tracker.col
        pvmap[Property.TOKEN_STRING] =  tokenString
        return pvmap
    }


    override fun tokenize(source: String): List<Token> {


        val codePoints = source.codePointSequence() + EOF

        val tokens = ArrayList<Token>()
        val tracker = PositionTracker()
        var parameterCt = 0
        var currPos = tracker.position
        var tokenCodePointCount = 0L
        var curr: State = INITIAL_STATE
        val buffer = StringBuilder()


        for (cp in codePoints) {
            tokenCodePointCount++;

            fun errInvalidChar(): Nothing =
                throw LexerException(errorCode = ErrorCode.LEXER_INVALID_CHAR, errorContext = makePropertyBag(repr(cp), tracker))

            fun errInvalidOperator(operator: String): Nothing =
                throw LexerException(errorCode = ErrorCode.LEXER_INVALID_OPERATOR, errorContext = makePropertyBag(operator, tracker))

            fun errInvalidLiteral(literal: String): Nothing =
                throw LexerException(errorCode = ErrorCode.LEXER_INVALID_LITERAL, errorContext = makePropertyBag(literal, tracker))

            fun errInvalidIonLiteral(literal: String, cause: IonException): Nothing =
                throw LexerException(errorCode = ErrorCode.LEXER_INVALID_ION_LITERAL,
                                     errorContext = makePropertyBag(literal, tracker),
                                     cause = cause)

            tracker.advance(cp)

            // retrieve the next state
            val next = when (cp) {
                EOF  -> EOF_STATE
                else -> curr[cp]
            }

            val currType = curr.stateType
            val nextType = next.stateType
            when {

                nextType == StateType.ERROR -> errInvalidChar()
                nextType.beginsToken -> {
                    // we can only start a token if we've properly ended another one.
                    if (currType != StateType.INITIAL && !currType.endsToken) {
                        errInvalidChar()
                    }
                    if (currType.endsToken && curr.lexType != LexType.WHITESPACE) {
                        // flush out the previous token
                        val text = buffer.toString()

                        var tokenType = curr.tokenType!!
                        val ionValue = when (tokenType) {
                            TokenType.OPERATOR -> {
                                val unaliased = OPERATOR_ALIASES[text] ?: text
                                when (unaliased) {
                                    in ALL_OPERATORS -> ion.newSymbol(unaliased)
                                    else -> errInvalidOperator(unaliased)
                                }
                            }
                            TokenType.IDENTIFIER -> {
                                val lower = text.toLowerCase()
                                when {
                                    curr.lexType == LexType.DQ_STRING -> ion.newSymbol(text)
                                    lower in ALL_SINGLE_LEXEME_OPERATORS -> {
                                        // an operator that looks like a keyword
                                        tokenType = TokenType.OPERATOR
                                        ion.newSymbol(lower)
                                    }
                                    lower == "as" -> {
                                        // AS token
                                        tokenType = TokenType.AS
                                        ion.newSymbol(lower)
                                    }
                                    lower == "at" -> {
                                        // AS token
                                        tokenType = TokenType.AT
                                        ion.newSymbol(lower)
                                    }
                                    lower == "by" -> {
                                        // BY token
                                        tokenType = TokenType.BY
                                        ion.newSymbol(lower)
                                    }
                                    lower == "null" -> {
                                        // literal null
                                        tokenType = TokenType.NULL
                                        ion.newNull()
                                    }
                                    lower == "missing" -> {
                                        // special literal for MISSING
                                        tokenType = TokenType.MISSING
                                        ion.newNull()
                                    }
                                    lower == "for" -> {
                                        // used as an argument delimiter for substring
                                        tokenType = TokenType.FOR
                                        ion.newSymbol(lower)
                                    }
                                    lower == "asc" -> {
                                        tokenType = TokenType.ASC
                                        ion.newSymbol(lower)
                                    }
                                    lower == "desc" -> {
                                        tokenType = TokenType.DESC
                                        ion.newSymbol(lower)
                                    }
                                    lower in BOOLEAN_KEYWORDS -> {
                                        // literal boolean
                                        tokenType = TokenType.LITERAL
                                        ion.newBool(lower == "true")
                                    }
                                    lower in KEYWORDS -> {
                                        // unquoted identifier that is a keyword
                                        tokenType = TokenType.KEYWORD
                                        ion.newSymbol(KEYWORD_ALIASES[lower] ?: lower)
                                    }
                                    else -> ion.newSymbol(text)
                                }
                            }
                            TokenType.LITERAL -> when (curr.lexType) {
                                LexType.SQ_STRING   -> ion.newString(text)
                                LexType.INTEGER     -> ion.newInt(BigInteger(text, 10))
                                LexType.DECIMAL     -> try {
                                    ion.newDecimal(bigDecimalOf(text))
                                }
                                catch (e: NumberFormatException) {
                                    errInvalidLiteral(text)
                                }

                                else        -> errInvalidLiteral(text)
                            }
                            TokenType.ION_LITERAL -> {
                                try {
                                    // anything wrapped by `` is considered as an ion literal, including invalid
                                    // ion so we need to handle the exception here for proper error reporting
                                    ion.singleValue(text)
                                }
                                catch (e: IonException) {
                                    errInvalidIonLiteral(text, e)
                                }
                            }
                            TokenType.QUESTION_MARK -> {
                                ion.newInt(++parameterCt)
                            }
                            else -> ion.newSymbol(text)
                        }.seal()

                        tokens.addOrMerge(
                            Token(
                                type = tokenType,
                                value = ionValue,
                                span = SourceSpan(currPos.line, currPos.column, tokenCodePointCount)))
                    }

                    // get ready for next token
                    buffer.setLength(0)
                    currPos = tracker.position
                    tokenCodePointCount = 0
                }
            }
            val replacement = next.replacement
            if (cp != EOF && replacement != REPLACE_NOTHING) {
                buffer.appendCodePoint(when (replacement) {
                    REPLACE_SAME -> cp
                    else -> replacement
                })
            }

            // if next state is the EOF marker add it to `tokens`.
            if (next.stateType == StateType.END) tokens.add(
                Token(
                    type = TokenType.EOF,
                    value = ion.newSymbol("EOF"),
                    span = SourceSpan(currPos.line, currPos.column, 0)))

            curr = next
        }

        return tokens
    }

    private fun MutableList<Token>.addOrMerge(token: Token) {
        var newToken = token

        // try to merge with previous tokens (have to go from greatest to lowest)
        for (i in MULTI_LEXEME_MAX_LENGTH downTo MULTI_LEXEME_MIN_LENGTH) {
            val prefixLength = i - 1
            if (prefixLength > size) {
                // go to the next size down
                continue
            }

            // composite candidate
            val keywords = subList(size - prefixLength, size)
                .asSequence()
                .plus(newToken)
                .map { it.keywordText }
                .toList()
            val lexemeMapping = MULTI_LEXEME_TOKEN_MAP[keywords] ?: continue

            // at this point we found the candidate so we need to replace the suffix
            var newPos = newToken.span
            for (count in 1..prefixLength) {
                newPos = removeAt(size - 1).span
                // TODO: calculate length of multi-lexeme tokens correctly. We use the length of the first token for now
            }

            // create our new token
            val (keyword, type) = lexemeMapping
            newToken = Token(type, ion.newSymbol(keyword), newPos)
        }

        add(newToken)
    }
}
