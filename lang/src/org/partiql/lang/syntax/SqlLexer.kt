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

import com.amazon.ion.*
import org.partiql.lang.errors.*
import org.partiql.lang.errors.ErrorCode.*
import org.partiql.lang.errors.Property.*
import org.partiql.lang.syntax.SqlLexer.LexType.*
import org.partiql.lang.syntax.SqlLexer.StateType.*
import org.partiql.lang.syntax.TokenType.*
import org.partiql.lang.syntax.TokenType.KEYWORD
import org.partiql.lang.util.*
import java.math.*


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
            get() = NONE
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
                              override val lexType: LexType = NONE,
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
                                  lexType: LexType = NONE) {
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
                  lexType: LexType = NONE,
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
        private val EOF_STATE = RepeatingState(END)

        /** Error state. */
        private val ERROR_STATE = RepeatingState(ERROR)

        /** Initial state. */
        private val INITIAL_STATE = TableState(INITIAL) {
            val initialState = this

            delta("(", START_AND_TERMINAL, LEFT_PAREN)
            delta(")", START_AND_TERMINAL, RIGHT_PAREN)
            delta("[", START_AND_TERMINAL, LEFT_BRACKET)
            delta("]", START_AND_TERMINAL, RIGHT_BRACKET)
            delta("{", START_AND_TERMINAL, LEFT_CURLY)
            delta("}", START_AND_TERMINAL, RIGHT_CURLY)
            delta(":", START_AND_TERMINAL, COLON)
            delta(",", START_AND_TERMINAL, COMMA)
            delta("*", START_AND_TERMINAL, STAR)
            delta(";", START_AND_TERMINAL, SEMICOLON)
            delta("?", START_AND_TERMINAL, QUESTION_MARK)

            delta(NON_OVERLOADED_OPERATOR_CHARS, START_AND_TERMINAL, OPERATOR)

            delta("|", START) {
                delta("|", TERMINAL, OPERATOR, delegate = initialState)
            }
            delta("!", START) {
                delta("=", TERMINAL, OPERATOR, delegate = initialState)
            }
            delta("<", START_AND_TERMINAL, OPERATOR) {
                delta("=", TERMINAL, OPERATOR, delegate = initialState)
                delta(">", TERMINAL, OPERATOR, delegate = initialState)
                delta("<", TERMINAL, LEFT_DOUBLE_ANGLE_BRACKET, delegate = initialState)
            }
            delta(">", START_AND_TERMINAL, OPERATOR) {
                delta("=", TERMINAL, OPERATOR, delegate = initialState)
                delta(">", TERMINAL, RIGHT_DOUBLE_ANGLE_BRACKET, delegate = initialState)
            }

            delta(IDENT_START_CHARS, START_AND_TERMINAL, IDENTIFIER) {
                delta(IDENT_CONTINUE_CHARS, TERMINAL, IDENTIFIER)
            }

            fun TableState.deltaDecimalInteger(stateType: StateType, lexType: LexType, setup: TableState.(String) -> Unit = { }): Unit {
                delta(DIGIT_CHARS, stateType, LITERAL, lexType, delegate = initialState) {
                    delta(DIGIT_CHARS, TERMINAL, LITERAL, lexType)
                    setup(it)
                }
            }

            fun TableState.deltaDecimalFraction(setup: TableState.(String) -> Unit = { }): Unit {
                delta(".", TERMINAL, LITERAL, DECIMAL) {
                    deltaDecimalInteger(TERMINAL, DECIMAL, setup)
                }
            }

            fun TableState.deltaExponent(setup: TableState.(String) -> Unit = { }): Unit {
                delta(E_NOTATION_CHARS, INCOMPLETE, delegate = ERROR_STATE) {
                    delta(SIGN_CHARS, INCOMPLETE, delegate = ERROR_STATE) {
                        deltaDecimalInteger(TERMINAL, DECIMAL, setup)
                    }
                    deltaDecimalInteger(TERMINAL, DECIMAL, setup)
                }
            }

            fun TableState.deltaNumber(stateType: StateType) {
                deltaDecimalInteger(stateType, INTEGER) {
                    deltaDecimalFraction {
                        deltaExponent { }
                    }
                    deltaExponent { }
                }
                when (stateType) {
                    START_AND_TERMINAL -> {
                        // at the top-level we need to support dot as a special
                        delta(".", START_AND_TERMINAL, DOT) {
                            deltaDecimalInteger(TERMINAL, DECIMAL) {
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

            deltaNumber(START_AND_TERMINAL)

            fun TableState.deltaQuote(quoteChar: String, tokenType: TokenType, lexType: LexType): Unit {
                delta(quoteChar, START, replacement = REPLACE_NOTHING) {
                    selfRepeatingDelegate(INCOMPLETE)
                    val quoteState = this
                    delta(quoteChar, TERMINAL, tokenType, lexType = lexType, replacement = REPLACE_NOTHING, delegate = initialState) {
                        delta(quoteChar, INCOMPLETE, delegate = quoteState)
                    }
                }
            }

            deltaQuote(SINGLE_QUOTE_CHARS, LITERAL, SQ_STRING)
            deltaQuote(DOUBLE_QUOTE_CHARS, QUOTED_IDENTIFIER, DQ_STRING)

            // Ion literals - very partial lexing of Ion to support nested back-tick
            // in Ion strings/symbols/comments
            delta(BACKTICK_CHARS, START, replacement = REPLACE_NOTHING) {
                selfRepeatingDelegate(INCOMPLETE)
                val quoteState = this

                delta("/", INCOMPLETE) {
                    delta("/", INCOMPLETE) {
                        val ionCommentState = this
                        selfRepeatingDelegate(INCOMPLETE)
                        delta(BACKTICK_CHARS, INCOMPLETE, delegate = ionCommentState)
                        delta(NL_WHITESPACE_CHARS, INCOMPLETE, delegate = quoteState)
                    }
                    delta("*", INCOMPLETE) {
                        val ionCommentState = this
                        selfRepeatingDelegate(INCOMPLETE)
                        delta(BACKTICK_CHARS, INCOMPLETE, delegate = ionCommentState)
                        delta("*", INCOMPLETE) {
                            delta("/", INCOMPLETE, delegate = quoteState)
                        }
                    }
                }
                delta(DOUBLE_QUOTE_CHARS, INCOMPLETE) {
                    val ionStringState = this
                    selfRepeatingDelegate(INCOMPLETE)

                    delta("\\", INCOMPLETE) {
                        delta(DOUBLE_QUOTE_CHARS, INCOMPLETE, delegate = ionStringState)
                    }
                    delta(BACKTICK_CHARS, INCOMPLETE, delegate = ionStringState)
                    delta(DOUBLE_QUOTE_CHARS, INCOMPLETE, delegate = quoteState)
                }
                delta(SINGLE_QUOTE_CHARS, INCOMPLETE) {
                    val ionStringState = this
                    selfRepeatingDelegate(INCOMPLETE)

                    delta("\\", INCOMPLETE) {
                        delta(SINGLE_QUOTE_CHARS, INCOMPLETE, delegate = ionStringState)
                    }
                    delta(BACKTICK_CHARS, INCOMPLETE, delegate = ionStringState)
                    delta(SINGLE_QUOTE_CHARS, INCOMPLETE, delegate = quoteState) {
                        delta(SINGLE_QUOTE_CHARS, INCOMPLETE, delegate = ionStringState) {
                            val ionLongStringState = this
                            selfRepeatingDelegate(INCOMPLETE)

                            delta("\\", INCOMPLETE) {
                                delta(SINGLE_QUOTE_CHARS, INCOMPLETE, delegate = ionLongStringState)
                            }
                            delta(BACKTICK_CHARS, INCOMPLETE, delegate = ionLongStringState)
                            delta(SINGLE_QUOTE_CHARS, INCOMPLETE, delegate = ionLongStringState) {
                                delta(SINGLE_QUOTE_CHARS, INCOMPLETE, delegate = ionLongStringState) {
                                    delta(SINGLE_QUOTE_CHARS, INCOMPLETE, delegate = quoteState)
                                }
                            }
                        }
                    }
                }

                delta("{", INCOMPLETE) {
                    delta("{", INCOMPLETE) {
                        selfRepeatingDelegate(INCOMPLETE)
                        delta("}", INCOMPLETE) {
                            delta("}", INCOMPLETE, delegate = quoteState)
                        }
                    }
                }

                delta(BACKTICK_CHARS, TERMINAL, TokenType.ION_LITERAL, LexType.ION_LITERAL, replacement = REPLACE_NOTHING, delegate = initialState)
            }

            delta(ALL_WHITESPACE_CHARS, START_AND_TERMINAL, null, WHITESPACE)

            // block comment and divide operator
            delta("/", START_AND_TERMINAL, OPERATOR) {
                delta("*", INCOMPLETE) {
                    selfRepeatingDelegate(INCOMPLETE)
                    delta("*", INCOMPLETE) {
                        delta("/", TERMINAL, null, WHITESPACE, delegate = initialState)
                    }
                }
            }
            // line comment, subtraction operator, and signed positive integer
            delta("-", START_AND_TERMINAL, OPERATOR) {
                // inline comments don't need a special terminator before EOF
                delta("-", TERMINAL, null, WHITESPACE) {
                    selfRepeatingDelegate(TERMINAL, null, WHITESPACE)
                    delta(NL_WHITESPACE_CHARS, TERMINAL, null, WHITESPACE, delegate = initialState)
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
        pvmap[LINE_NUMBER] =  tracker.line
        pvmap[COLUMN_NUMBER] =  tracker.col
        pvmap[TOKEN_STRING] =  tokenString
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
                throw LexerException(errorCode = LEXER_INVALID_CHAR, errorContext = makePropertyBag(repr(cp), tracker))

            fun errInvalidOperator(operator: String): Nothing =
                throw LexerException(errorCode = LEXER_INVALID_OPERATOR, errorContext = makePropertyBag(operator, tracker))

            fun errInvalidLiteral(literal: String): Nothing =
                throw LexerException(errorCode = LEXER_INVALID_LITERAL, errorContext = makePropertyBag(literal, tracker))

            fun errInvalidIonLiteral(literal: String, cause: IonException): Nothing =
                throw LexerException(errorCode = LEXER_INVALID_ION_LITERAL,
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

                nextType == ERROR -> errInvalidChar()
                nextType.beginsToken -> {
                    // we can only start a token if we've properly ended another one.
                    if (currType != INITIAL && !currType.endsToken) {
                        errInvalidChar()
                    }
                    if (currType.endsToken && curr.lexType != WHITESPACE) {
                        // flush out the previous token
                        val text = buffer.toString()

                        var tokenType = curr.tokenType!!
                        val ionValue = when (tokenType) {
                            OPERATOR -> {
                                val unaliased = OPERATOR_ALIASES[text] ?: text
                                when (unaliased) {
                                    in ALL_OPERATORS -> ion.newSymbol(unaliased)
                                    else -> errInvalidOperator(unaliased)
                                }
                            }
                            IDENTIFIER -> {
                                val lower = text.toLowerCase()
                                when {
                                    curr.lexType == DQ_STRING -> ion.newSymbol(text)
                                    lower in ALL_SINGLE_LEXEME_OPERATORS -> {
                                        // an operator that looks like a keyword
                                        tokenType = OPERATOR
                                        ion.newSymbol(lower)
                                    }
                                    lower == "as" -> {
                                        // AS token
                                        tokenType = AS
                                        ion.newSymbol(lower)
                                    }
                                    lower == "at" -> {
                                        // AS token
                                        tokenType = AT
                                        ion.newSymbol(lower)
                                    }
                                    lower == "by" -> {
                                        // BY token
                                        tokenType = BY
                                        ion.newSymbol(lower)
                                    }
                                    lower == "null" -> {
                                        // literal null
                                        tokenType = NULL
                                        ion.newNull()
                                    }
                                    lower == "missing" -> {
                                        // special literal for MISSING
                                        tokenType = MISSING
                                        ion.newNull()
                                    }
                                    lower == "for" -> {
                                        // used as an argument delimiter for substring
                                        tokenType = FOR
                                        ion.newSymbol(lower)
                                    }
                                    lower == "asc" -> {
                                        tokenType = ASC
                                        ion.newSymbol(lower)
                                    }
                                    lower == "desc" -> {
                                        tokenType = DESC
                                        ion.newSymbol(lower)
                                    }
                                    lower in BOOLEAN_KEYWORDS -> {
                                        // literal boolean
                                        tokenType = LITERAL
                                        ion.newBool(lower == "true")
                                    }
                                    lower in KEYWORDS -> {
                                        // unquoted identifier that is a keyword
                                        tokenType = KEYWORD
                                        ion.newSymbol(KEYWORD_ALIASES[lower] ?: lower)
                                    }
                                    else -> ion.newSymbol(text)
                                }
                            }
                            LITERAL -> when (curr.lexType) {
                                SQ_STRING   -> ion.newString(text)
                                INTEGER     -> ion.newInt(BigInteger(text, 10))
                                DECIMAL     -> try {
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
                            QUESTION_MARK -> {
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
            if (next.stateType == END) tokens.add(
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
