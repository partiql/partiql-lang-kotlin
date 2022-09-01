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

package org.partiql.lang.ots_work.plugins.standard.functions.timestamp

import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.util.codePointSequence
import org.partiql.lang.util.propertyValueMapOf

private const val NON_ESCAPED_TEXT = " /-,:."
private const val SINGLE_QUOTE_CP = '\''.toInt()
private const val PATTERN = "yMdahHmsSXxn"

// max code point range
// i.e. result of (NON_ESCAPED_TEXT + "'" + PATTERN).codePoints().max() + 1
private const val TABLE_SIZE = 122

/**
 * Timestamp format pattern token types.
 */
internal enum class TokenType { PATTERN, TEXT }

/**
 * Timestamp format pattern tokens.
 */
internal data class Token(val tokenType: TokenType, val value: String)

/**
 * State machine state types
 */
private enum class StateType(val beginsToken: Boolean, val endsToken: Boolean) {
    /** lexer initial state */
    INITIAL(beginsToken = false, endsToken = false),

    /** an error state */
    ERROR(beginsToken = false, endsToken = false),

    /** start of a new token */
    START(beginsToken = true, endsToken = false),

    /** possible termination of a token */
    TERMINAL(beginsToken = false, endsToken = true),

    /** state that's both start and terminal */
    START_AND_TERMINAL(beginsToken = true, endsToken = true),

    /** middle of a token */
    INCOMPLETE(beginsToken = false, endsToken = false)
}

/**
 * A lexer state machine node
 */
private interface State {
    val tokenType: TokenType?
    val stateType: StateType

    /**
     * Next state node for [cp]
     *
     * @throws IllegalStateException if no transition exists.
     */
    fun nextFor(cp: Int): State
}

/**
 * Table backed [State]. This class is mutable through [transitionTo] so needs to be setup statically to be thread safe
 */
private class TableState(
    override val tokenType: TokenType?,
    override val stateType: StateType,
    val delegate: State? = null
) : State {
    private val transitionTable = object {
        val backingArray = Array<State?>(TABLE_SIZE) { null }

        operator fun get(codePoint: Int): State? = when {
            codePoint < TABLE_SIZE -> backingArray[codePoint]
            else -> null
        }

        operator fun set(codePoint: Int, next: State) {
            backingArray[codePoint] = next
        }
    }

    /**
     * Registers a transition for all code points in [characters] to the [next] state
     */
    fun transitionTo(characters: String, next: State) {
        characters.forEach {
            val cp = it.toInt()
            transitionTo(cp, next)
        }
    }

    /**
     * Registers a transition for the code point to the [next] state
     */
    fun transitionTo(codePoint: Int, next: State) {
        transitionTable[codePoint] = next
    }

    override fun nextFor(cp: Int): State = transitionTable[cp] ?: delegate?.nextFor(cp) ?: throw IllegalStateException("Unknown transition")
}

private abstract class PatternState(val codePoint: Int, override val stateType: StateType) : State {
    override val tokenType = TokenType.PATTERN
}

private abstract class TextState(override val stateType: StateType) : State {
    override val tokenType = TokenType.TEXT
}

internal class TimestampFormatPatternLexer {
    companion object {
        private val ERROR_STATE: State = object : State {
            override val tokenType = null
            override val stateType: StateType = StateType.ERROR

            override fun nextFor(cp: Int): State = this
        }

        private val INITIAL_STATE = TableState(tokenType = null, stateType = StateType.INITIAL, delegate = ERROR_STATE)

        // setups the lexer state machine
        init {
            val startEscapedText = TableState(TokenType.TEXT, StateType.START_AND_TERMINAL, INITIAL_STATE)
            val inNonEscapedText = TableState(TokenType.TEXT, StateType.TERMINAL, INITIAL_STATE)
            startEscapedText.transitionTo(NON_ESCAPED_TEXT, inNonEscapedText)
            inNonEscapedText.transitionTo(NON_ESCAPED_TEXT, inNonEscapedText)

            val startQuotedText = object : TextState(StateType.START) {
                val startQuotedText = this

                val endQuotedState = object : TextState(StateType.TERMINAL) {
                    override fun nextFor(cp: Int): State = when (cp) {
                        SINGLE_QUOTE_CP -> startQuotedText
                        else -> INITIAL_STATE.nextFor(cp)
                    }
                }

                val inQuotedState = object : TextState(StateType.INCOMPLETE) {
                    override fun nextFor(cp: Int): State = when (cp) {
                        SINGLE_QUOTE_CP -> endQuotedState
                        else -> this
                    }
                }

                override fun nextFor(cp: Int): State = when (cp) {
                    SINGLE_QUOTE_CP -> endQuotedState
                    else -> inQuotedState
                }
            }

            INITIAL_STATE.transitionTo(NON_ESCAPED_TEXT, startEscapedText)
            INITIAL_STATE.transitionTo(SINGLE_QUOTE_CP, startQuotedText)
            PATTERN.codePoints().forEach { cp ->
                INITIAL_STATE.transitionTo(
                    cp,
                    object : PatternState(cp, StateType.START_AND_TERMINAL) {
                        val repeatingState = object : PatternState(cp, StateType.TERMINAL) {
                            override fun nextFor(cp: Int): State = when (cp) {
                                codePoint -> this
                                else -> INITIAL_STATE.nextFor(cp)
                            }
                        }

                        override fun nextFor(cp: Int): State = when (cp) {
                            codePoint -> repeatingState
                            else -> INITIAL_STATE.nextFor(cp)
                        }
                    }
                )
            }
        }
    }

    private fun StringBuilder.reset() = this.setLength(0)

    private fun tokenEnd(current: State, next: State) = when {
        current.stateType == StateType.INITIAL -> false
        current.tokenType == next.tokenType && next.stateType.beginsToken -> true
        current.tokenType != next.tokenType -> true
        else -> false
    }

    fun tokenize(source: String): List<Token> {
        val tokens = mutableListOf<Token>()
        val buffer = StringBuilder()

        if (source.isEmpty()) {
            return listOf()
        }

        fun flushToken(tokenType: TokenType) {
            tokens.add(Token(tokenType, buffer.toString()))
            buffer.reset()
        }

        var current: State = INITIAL_STATE

        val codePoints = source.codePointSequence()

        codePoints.forEach { cp ->
            val next = current.nextFor(cp)

            if (next.stateType == StateType.ERROR) {
                throw EvaluationException(
                    message = "Invalid token in timestamp format pattern",
                    errorCode = ErrorCode.EVALUATOR_INVALID_TIMESTAMP_FORMAT_PATTERN_TOKEN,
                    errorContext = propertyValueMapOf(Property.TIMESTAMP_FORMAT_PATTERN to source),
                    internal = false
                )
            }

            if (tokenEnd(current, next)) {
                flushToken(current.tokenType!!)
            }

            current = next
            buffer.appendCodePoint(cp)
        }

        if (!current.stateType.endsToken) {
            throw EvaluationException(
                message = "Unterminated token in timestamp format pattern",
                errorCode = ErrorCode.EVALUATOR_UNTERMINATED_TIMESTAMP_FORMAT_PATTERN_TOKEN,
                errorContext = propertyValueMapOf(Property.TIMESTAMP_FORMAT_PATTERN to source),
                internal = false
            )
        }

        flushToken(current.tokenType!!)

        return tokens
    }
}
