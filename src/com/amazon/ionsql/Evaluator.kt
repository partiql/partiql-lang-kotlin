/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonSexp
import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue
import java.util.*

/**
 * A basic implementation of the [Compiler] that parses Ion SQL into an [Expression].
 *
 * In particular, this implementation parses a SQL-like syntax over Ion s-expressions
 * by treating the s-expressions as an effective token stream.
 *
 * Note that this implementation relies on a variant of the Ion parser that
 * supports `,` in s-expressions directly to make it convenient, which is non-standard Ion.
 * It will technically work without this variant but separators become awkward.
 *
 * This implementation produces a very simple AST-walking evaluator as its "compiled" form.
 */
class Evaluator(private val ion: IonSystem) : Compiler {
    private val tokenizer = Tokenizer(ion)
    private val parser = Parser(ion)

    private val syntax: Map<String, (Bindings, IonSexp) -> ExpressionValue> = mapOf(
        "lit" to { env, expr ->
            IonExpressionValue(expr[1])
        },
        "id" to { env, expr ->
            val name = expr[1].text
            env[name] ?:
                throw IllegalArgumentException("No such binding: $name")
        },
        "call" to { env, expr ->
            val name = expr[1].text
            val func = functions[name] ?:
                throw IllegalArgumentException("No such function: $name")
            func(env, expr.evalCallArgs(env))
        }
        // TODO implement all of the syntax
    )

    private val functions: Map<String, (Bindings, List<ExpressionValue>) -> ExpressionValue> =
        mapOf(
            // TODO implement the supported functions
        )

    private val IonValue.text: String
        get() = stringValue() ?:
            throw IllegalArgumentException("Expected non-null string: $this")

    private fun IonSexp.evalCallArgs(env: Bindings, startIndex: Int = 2): List<ExpressionValue> {
        val args = ArrayList<ExpressionValue>()
        for (idx in startIndex until size) {
            val raw = this[idx]
            args.add(raw.eval(env))
        }
        return args
    }

    private fun IonValue.eval(env: Bindings): ExpressionValue {
        if (this !is IonSexp) {
            throw IllegalArgumentException("AST node is not s-expression: $this")
        }

        val name = this[0].stringValue() ?:
            throw IllegalArgumentException("AST node does not start with non-null string: $this")
        val handler = syntax[name] ?:
            throw IllegalArgumentException("No such syntax handler for $name")
        return handler(env, this)
    }

    override fun compile(source: String): Expression {
        // We have to wrap the source in an s-expression to get the right parsing behavior
        val expression = ion.singleValue("($source)")
        val tokens = tokenizer.tokenize(expression)
        val ast = parser.parse(tokens)

        return object : Expression {
            override fun eval(env: Bindings): ExpressionValue = ast.eval(env)
        }
    }
}