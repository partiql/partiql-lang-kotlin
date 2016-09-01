/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonList
import com.amazon.ion.IonSexp
import com.amazon.ion.IonSymbol
import com.amazon.ion.IonType.*
import com.amazon.ion.IonValue
import com.amazon.ionsql.Token.Type
import com.amazon.ionsql.Token.Type.*
import java.util.*

/**
 * Provides generates a list of tokens from an expression.
 */
object Tokenizer {
    fun tokenize(source: IonValue): List<Token> {
        // make sure we have an expression
        val expr = when(source.type) {
            SEXP -> source
            else -> source.system.newSexp(source.clone())
        }

        val tokens = ArrayList<Token>()
        tokens.tokenize(expr)
        return tokens
    }

    private fun MutableList<Token>.tokenizeContainer(left: Type, right: Type, value: IonValue) {
        add(Token(left))
        this.tokenize(value)
        add(Token(right))
    }

    private fun MutableList<Token>.tokenize(source: IonValue) {
        for (value in source) {
            when (value) {
                is IonList -> tokenizeContainer(LEFT_BRACKET, RIGHT_BRACKET, value)
                is IonSexp -> tokenizeContainer(LEFT_PAREN, RIGHT_PAREN, value)
                is IonSymbol -> addAll(value.tokenize())
                else -> add(Token(LITERAL, value))
            }
        }
    }

    private val BREAK_OUT_OPERATORS = setOf("*", ".")
    private val IDENTIFIER_PATTERN = Regex("""[a-z_$][a-z0-9_$]*""")

    private fun IonSymbol.tokenize(): List<Token> {
        val tokens = ArrayList<Token>()

        // names are not case sensitive
        var text = stringValue().toLowerCase()

        // we need to deal with the case that certain operator characters may get glommed together
        // and we need to be able to distinguish those as distinct tokens
        while (text.length > 1) {
            val head = text.substring(0, 1)
            if (head in BREAK_OUT_OPERATORS) {
                tokens.add(Token(type(head), system.newSymbol(head)))
            } else {
                break
            }
            text = text.substring(1)
        }

        // add in remainder as the appropriate token
        tokens.add(Token(type(text), system.newSymbol(text)))

        return tokens
    }

    private fun type(text: String): Type = when (text) {
        in Token.KEYWORDS -> KEYWORD
        in Token.ALL_OPERATORS -> OPERATOR
        "," -> COMMA
        "*" -> STAR
        "." -> DOT
        else -> {
            // TODO we should probably be less strict here
            if (text.matches(IDENTIFIER_PATTERN)) {
                IDENTIFIER
            } else {
                throw IllegalArgumentException("Illegal identifier $text")
            }
        }
    }
}