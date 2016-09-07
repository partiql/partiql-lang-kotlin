/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.*
import java.math.BigDecimal
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

    private val wildcardPath = ion.newSexp().apply { add().newSymbol("*") }.seal()
    private val parentPath = ion.newSexp().apply { add().newSymbol("..") }.seal()

    private val instrinsicCall: (Bindings, IonSexp) -> ExprValue = { env, expr ->
        expr.evalCall(env, startIndex = 0)
    }

    /** Dispatch table for AST "op-codes."  */
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
                1 -> {
                    // force interpretation as a number, and do nothing
                    args[0].numberValue()
                    args[0]
                }
                else -> (args[0].numberValue() + args[1].numberValue()).exprValue()
            }
        },
        "-" to bindOp(minArity = 1, maxArity = 2) { env, args ->
            when (args.size) {
                1 -> {
                    -args[0].numberValue()
                }
                else -> args[0].numberValue() - args[1].numberValue()
            }.exprValue()
        },
        "*" to bindOp { env, args ->
            (args[0].numberValue() * args[1].numberValue()).exprValue()
        },
        "/" to bindOp { env, args ->
            (args[0].numberValue() / args[1].numberValue()).exprValue()
        },
        "%" to bindOp { env, args ->
            (args[0].numberValue() % args[1].numberValue()).exprValue()
        },
        "<" to bindOp { env, args ->
            (args[0] < args[1]).exprValue()
        },
        "<=" to bindOp { env, args ->
            (args[0] <= args[1]).exprValue()
        },
        ">" to bindOp { env, args ->
            (args[0] > args[1]).exprValue()
        },
        ">=" to bindOp { env, args ->
            (args[0] >= args[1]).exprValue()
        },
        "==" to bindOp { env, args ->
            args[0].exprEquals(args[1]).exprValue()
        },
        "!=" to bindOp { env, args ->
            (!args[0].exprEquals(args[1])).exprValue()
        },
        "not" to bindOp(minArity = 1, maxArity = 1) { env, args ->
            (!args[0].booleanValue()).exprValue()
        },
        "or" to { env, expr ->
            when (expr.size) {
                3 -> expr[1].eval(env).booleanValue() || expr[2].eval(env).booleanValue()
                else -> throw IllegalArgumentException("Arity incorrect for 'or': $expr")
            }.exprValue()
        },
        "and" to { env, expr ->
            when (expr.size) {
                3 -> expr[1].eval(env).booleanValue() && expr[2].eval(env).booleanValue()
                else -> throw IllegalArgumentException("Arity incorrect for 'and': $expr")
            }.exprValue()
        },
        "." to { env, expr ->
            if (expr.size < 3) {
                throw IllegalArgumentException("Path arity to low: $expr")
            }

            var root = expr[1].eval(env)

            // extract all the non-wildcard paths
            var idx = 2
            while (idx < expr.size) {
                val raw = expr[idx]
                if (raw == wildcardPath) {
                    // need special processing for the rest of the path
                    break
                }

                root = when (raw) {
                    parentPath -> root.ionValue.container?.exprValue() ?:
                        throw IllegalArgumentException("Cannot .. out of top-level: $root")
                    else -> {
                        root[raw.eval(env)]
                    }
                }
                idx++
            }

            // we are either done or we have wild-card paths and beyond
            val components = ArrayList<(ExprValue) -> Sequence<ExprValue>>()
            while (idx < expr.size) {
                val raw = expr[idx]
                components.add(when (raw) {
                    // treat the entire value as a sequence
                    wildcardPath -> { exprVal ->
                        exprVal.asSequence()
                    }
                    parentPath -> { exprVal ->
                        sequenceOf(
                            exprVal.ionValue.container?.exprValue() ?:
                                throw IllegalArgumentException(
                                    "Cannot .. out of top-level: ${exprVal.ionValue}"
                                )
                        )
                    }
                    // "index" into the value lazily
                    else -> { exprVal ->
                        sequenceOf(exprVal[raw.eval(env)])
                    }
                })
                idx++
            }

            when (components.size) {
                0 -> root
                else -> SequenceExprValue(ion) {
                    var seq = sequenceOf(root)
                    for (component in components) {
                        seq = seq.flatMap(component)
                    }
                    seq
                }
            }
        },
        "as" to { env, expr ->
            when (expr.size) {
                3 -> {
                    expr[2].eval(env).alias(expr[1].text)
                }
                else -> throw IllegalArgumentException("Bad alias: $expr")
            }
        },
        "select" to { env, expr ->
            if (expr.size < 3 || expr.size > 4) {
                throw IllegalArgumentException("Bad arity on SELECT form $expr: ${expr.size}")
            }

            val selectExprs = expr[1]
            if (selectExprs !is IonSequence) {
                throw IllegalArgumentException("SELECT list must be sequence: $selectExprs")
            }

            val fromValues = expr[2].asSequence()
                .drop(1)
                .map { it.eval(env) }
                .toList()

            val whereExpr = when {
                expr.size > 3 -> expr[3][0]
                else -> null
            }

            SequenceExprValue(ion) {
                // compute the join over the data sources
                fromValues.product().asSequence()
                    .map { joinedValues ->
                        // bind the joined value to the bindings for the filter/project
                        Pair(joinedValues, joinedValues.bind(env))
                    }
                    .filter {
                        val locals = it.second
                        when (whereExpr) {
                            null -> true
                            else -> whereExpr.eval(locals).booleanValue()
                        }
                    }
                    .map {
                        val (joinedValues, locals) = it
                        ion.newEmptyStruct().apply {
                            when (selectExprs.size) {
                                0 -> {
                                    // select * case
                                    projectAllInto(joinedValues);
                                }
                                else -> {
                                    // select a, b as c, ... case
                                    projectSelectList(locals, selectExprs)
                                }
                            }
                        }.seal().exprValue()
                    }
            }
        }
    )

    /** Dispatch table for built-in functions. */
    private val functions: Map<String, (Bindings, List<ExprValue>) -> ExprValue> = mapOf(
        "list" to { env, args ->
            ion.newEmptyList().apply {
                for (value in args) {
                    add(value.ionValue.clone())
                }
            }.seal().exprValue()
        },
        "sexp" to { env, args ->
            ion.newEmptySexp().apply {
                for (value in args) {
                    add(value.ionValue.clone())
                }
            }.seal().exprValue()
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
            }.seal().exprValue()
        }
        // TODO finish implementing "standard" functions
    )

    private fun IonStruct.projectAllInto(joinedValues: List<ExprValue?>) {
        val names = HashSet<String>()
        joinedValues.forEachIndexed { col, joinValue ->
            val ionVal = joinValue?.ionValue!!
            when (ionVal) {
                is IonStruct -> {
                    for (child in ionVal) {
                        val childName = child.fieldName
                        val name = when {
                            childName in names -> "${col}_$childName"
                            else -> childName
                        }
                        names.add(name)
                        add(name, child.clone())
                    }
                }
                else -> {
                    val name = when {
                        SYS_VALUE in names -> "${col}_$SYS_VALUE"
                        else -> SYS_VALUE
                    }
                    names.add(name)
                    add(name, ionVal.clone())
                }
            }
        }
    }

    private fun IonStruct.projectSelectList(locals: Bindings, exprs: IonSequence) {
        val names = HashSet<String>()
        exprs.forEachIndexed { col, raw ->
            var desiredName = "$col"
            val value = when (raw[0].text) {
                "as" -> {
                    // extract alias
                    desiredName = raw[1].text
                    raw[2]
                }
                else -> raw
            }.eval(locals)
            val name = when {
                desiredName in names -> "${col}_$desiredName"
                else -> desiredName
            }
            names.add(name)
            add(name, value.ionValue.clone())
        }
    }

    private fun List<ExprValue?>.bind(parent: Bindings): Bindings {
        val locals = map { it?.bind(Bindings.empty()) }

        return Bindings.over { name ->
            val found = locals.asSequence()
                .map { it?.get(name) }
                .filter { it != null }
                .toList()
            when (found.size) {
                // nothing found at our scope, got to parent
                0 -> parent[name]
                // found exactly one thing, success
                1 -> found.head!!
                // multiple things with the same name is a conflict
                else -> throw IllegalArgumentException(
                    "$name is ambigious: ${found.map { it?.ionValue }}")
            }
        }
    }

    private fun Boolean.exprValue(): ExprValue = ion.newBool(this).seal().exprValue()

    private fun Number.exprValue(): ExprValue = when (this) {
        is Long -> ion.newInt(this)
        is Double -> ion.newFloat(this)
        is BigDecimal -> ion.newDecimal(this)
        else -> throw IllegalArgumentException("Cannot convert number to expression value: $this")
    }.seal().exprValue()

    private fun ExprValue.numberValue(): Number = ionValue.numberValue()

    private fun ExprValue.booleanValue(): Boolean =
        ionValue.booleanValue() ?:
            throw IllegalArgumentException("Expected non-null boolean: $ionValue")

    private operator fun ExprValue.get(index: ExprValue): ExprValue {
        val indexVal = index.ionValue
        return when (indexVal) {
            is IonInt -> ionValue[indexVal.intValue()].exprValue()
            is IonText -> {
                val name = indexVal.stringValue()
                // delegate to bindings logic as the scope of lookup by name
                bind(Bindings.empty())[name] ?:
                    throw IllegalArgumentException("Could not find member $name in $ionValue")
            }
            else -> throw IllegalArgumentException("Cannot convert index to int/string: $indexVal")
        }
    }

    private operator fun ExprValue.compareTo(other: ExprValue): Int {
        val first = this.ionValue
        val second = other.ionValue

        return when {
            // nulls can't compare
            first.isNullValue || second.isNullValue ->
                throw IllegalArgumentException("Null value cannot be compared: $first, $second")
            // compare the number types
            first.isNumeric && second.isNumeric ->
                first.numberValue().compareTo(second.numberValue())
            // timestamps compare against timestamps
            first is IonTimestamp && second is IonTimestamp ->
                first.timestampValue().compareTo(second.timestampValue())
            // string/symbol compare against themselves
            first is IonText && second is IonText ->
                first.stringValue().compareTo(second.stringValue())
            // TODO should bool/LOBs/aggregates compare?
            else -> throw IllegalArgumentException("Cannot compare values: $first, $second")
        }
    }

    // TODO define the various forms of equality properly
    private fun ExprValue.exprEquals(other: ExprValue): Boolean {
        val first = this.ionValue
        val second = other.ionValue

        return when {
            // any nulls involved need strict equality
            first.isNullValue || second.isNullValue -> first == second
            // arithmetic equality
            first.isNumeric && second.isNumeric ->
                first.numberValue().compareTo(second.numberValue()) == 0
            // text equality for symbols/strings
            first is IonText && second is IonText ->
                first.stringValue() == second.stringValue()
            // defer to strict equality
            else -> first == second
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

    private fun bindOp(minArity: Int = 2,
                       maxArity: Int = 2,
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

    override fun compile(source: String): Expression {
        // We have to wrap the source in an s-expression to get the right parsing behavior
        val expression = ion.singleValue("($source)").seal()
        val tokens = tokenizer.tokenize(expression)
        val ast = parser.parse(tokens)

        return object : Expression {
            override fun eval(env: Bindings): ExprValue = ast.eval(env)
        }
    }
}