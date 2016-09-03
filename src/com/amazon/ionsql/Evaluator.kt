/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.IonSequence
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

    private val instrinsicCall: (Bindings, IonSexp) -> ExprValue = { env, expr ->
        expr.evalCall(env, startIndex = 0)
    }

    private val syntax: Map<String, (Bindings, IonSexp) -> ExprValue> = mapOf(
        "lit" to { env, expr ->
            expr[1].exprValue()
        },
        "id" to { env, expr ->
            val name = expr[1].text
            env[name] ?:
                throw IllegalArgumentException("No such binding: $name")
        },
        "call" to { env, expr ->
            expr.evalCall(env, startIndex = 1)
        },
        "list" to instrinsicCall,
        "struct" to instrinsicCall,
        "+" to bindOp(minArity = 1, maxArity = 2) { env, args ->
            when (args.size) {
                1 -> throw UnsupportedOperationException("TODO")
                else -> throw UnsupportedOperationException("TODO")
            }
        }
        // TODO implement all of the syntax constructs
    )

    private val functions: Map<String, (Bindings, List<ExprValue>) -> ExprValue> = mapOf(
        "list" to { env, args ->
            ion.newEmptyList().apply {
                for (value in args) {
                    add(value.ionValue.clone())
                }
            }.exprValue()
        },
        "struct" to { env, args ->
            ion.newEmptyStruct().apply {
                for (arg in args) {
                    val value = arg.ionValue
                    when (value) {
                        is IonSequence -> when (value.size) {
                            2 -> {
                                val name = value[0].text
                                val child = value[1].clone()
                                add(name, child)
                            }
                            else -> throw IllegalArgumentException(
                                "Expected pair for struct argument: $value"
                            )
                        }
                        else -> throw IllegalArgumentException(
                            "Expected pair for struct argument: $value"
                        )
                    }
                }
            }.exprValue()
        }
    )

    private fun bindOp(minArity: Int,
                       maxArity: Int,
                       op: (Bindings, List<ExprValue>) -> ExprValue): (Bindings, IonSexp) -> ExprValue {
        return { env, expr ->
            val arity = expr.size - 1
            when {
                arity < minArity -> throw IllegalArgumentException("Not enough arguments: $expr")
                arity > maxArity -> throw IllegalArgumentException("Too many arguments: $expr")
            }
            expr.evalFunc(env, 1, op)
        }
    }

    private val IonValue.text: String
        get() = stringValue() ?:
            throw IllegalArgumentException("Expected non-null string: $this")

    private fun IonSexp.evalCall(env: Bindings, startIndex: Int): ExprValue {
        val name = this[startIndex].text
        val func = functions[name] ?:
            throw IllegalArgumentException("No such function: $name")
        val argIndex = startIndex + 1
        return evalFunc(env, argIndex, func)
    }

    private fun IonSexp.evalArgs(env: Bindings, startIndex: Int): List<ExprValue> {
        val args = ArrayList<ExprValue>()
        for (idx in startIndex until size) {
            val raw = this[idx]
            args.add(raw.eval(env))
        }
        return args
    }

    private fun IonSexp.evalFunc(env: Bindings,
                                 argIndex: Int,
                                 func: (Bindings, List<ExprValue>) -> ExprValue): ExprValue {
        val args = evalArgs(env, argIndex)
        return func(env, args)
    }

    private fun IonValue.eval(env: Bindings): ExprValue {
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
            override fun eval(env: Bindings): ExprValue = ast.eval(env)
        }
    }
}