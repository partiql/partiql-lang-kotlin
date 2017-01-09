/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.*
import com.amazon.ion.IonType.SEXP
import com.amazon.ionsql.TokenType.*
import java.util.*

/**
 * Lexically generates tokens from an expression leveraging the Ion parser as the
 * sort of tokenizer.
 */
class IonSqlHackLexer(private val ion: IonSystem) : Lexer {
    companion object {
        // note that this has to be length == 1 strings
        private val BREAK_OUT_OPERATORS = setOf("*", ".", "+", "-")
        private val IDENTIFIER_PATTERN = Regex("""[a-zA-Z_$][a-zA-Z0-9_$]*""")
    }

    override fun tokenize(source: String): List<Token> =
        // We have to wrap the source in an s-expression to get the right tokenizing behavior
        tokenize(ion.singleValue("($source)").seal())

    // TODO eliminate this (REPL has a dependency on it)
    internal fun tokenize(source: IonValue): List<Token> {
        // make sure we have an expression
        val expr = when(source.type) {
            SEXP -> source
            else -> source.system.newSexp(source.clone())
        }

        val tokens = ArrayList<Token>()
        tokens.tokenize(expr)
        return tokens
    }

    private fun MutableList<Token>.tokenizeContainer(left: TokenType, right: TokenType, value: IonValue) {
        add(Token(left))
        tokenize(value)
        add(Token(right))
    }

    private fun MutableList<Token>.tokenizeStruct(struct: IonValue) {
        add(Token(LEFT_CURLY))
        tokenize(struct, isInStruct = true)
        add(Token(RIGHT_CURLY))
    }

    private fun MutableList<Token>.tokenize(source: IonValue, isInStruct: Boolean = false) {
        var first = true
        for (child in source) {
            if (!first) {
                when (source) {
                    // we "put back in" the commas in the list to normalize parsing
                    is IonList, is IonStruct -> add(Token(COMMA))
                }
            }
            if (isInStruct) {
                add(Token(LITERAL, ion.newString(child.fieldName)))
                // we "put back in" the colon to normalize the parsing
                add(Token(COLON))
            }
            when (child) {
                is IonList -> tokenizeContainer(LEFT_BRACKET, RIGHT_BRACKET, child)
                is IonSexp -> tokenizeContainer(LEFT_PAREN, RIGHT_PAREN, child)
                is IonStruct -> tokenizeStruct(child)
                is IonSymbol -> addAll(child.tokenize())
                else -> add(Token(LITERAL, child))
            }
            first = false
        }
    }

    private fun IonSymbol.tokenize(): List<Token> {
        val tokens = ArrayList<Token>()

        // names are not case sensitive
        var text = stringValue()

        // we need to deal with the case that certain operator characters may get glommed together
        // and we need to be able to distinguish those as distinct tokens
        while (text.length > 1) {
            val head = text.substring(0, 1)
            if (head in BREAK_OUT_OPERATORS) {
                tokens.add(token(head))
            } else {
                break
            }
            text = text.substring(1)
        }

        // add in remainder as the appropriate token
        tokens.add(token(text))

        return tokens
    }
    private fun token(text: String): Token {
        val alias = text.toLowerCase()
        val unaliased = OPERATOR_ALIASES[alias] ?: alias

        val type = when (unaliased) {
            "," -> COMMA
            "*" -> STAR
            "." -> DOT
            in ALL_OPERATORS -> OPERATOR
            in KEYWORDS -> KEYWORD
            else -> {
                // TODO we should probably be less strict here
                if (text.matches(IDENTIFIER_PATTERN)) {
                    IDENTIFIER
                } else {
                    throw IllegalArgumentException("Illegal identifier $text")
                }
            }
        }

        val actualText = when(type) {
            KEYWORD, OPERATOR -> unaliased
            else -> text
        }

        return Token(type, ion.newSymbol(actualText))
    }
}
