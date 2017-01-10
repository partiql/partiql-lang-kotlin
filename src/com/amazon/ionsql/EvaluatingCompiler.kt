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
 *
 * @param ion The ion system to use for synthesizing Ion values.
 * @param userFuncs Functions to provide access to in addition to the built-ins.
 */
class EvaluatingCompiler(private val ion: IonSystem,
                         userFuncs: Map<String, (Bindings, List<ExprValue>) -> ExprValue> = emptyMap()) : Compiler {
    private val tokenizer = IonSqlLexer(ion)
    private val parser = IonSqlParser(ion)

    private val wildcardPath = ion.newSexp().apply { add().newSymbol("*") }.seal()

    private val instrinsicCall: (Bindings, IonSexp) -> ExprValue = { env, expr ->
        expr.evalCall(env, startIndex = 0)
    }

    private fun aliasExtractor(seq: Sequence<IonValue>): List<String> =
        seq.mapIndexed { col, value ->
            when (value) {
                is IonSexp -> {
                    when (value[0].text) {
                        "as", "id" -> value[1].text
                        else -> "$col"
                    }
                }
                else -> throw IllegalArgumentException("Cannot extract alias out of: $value")
            }
        }.toList()

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
        "=" to bindOp { env, args ->
            args[0].exprEquals(args[1]).exprValue()
        },
        "<>" to bindOp { env, args ->
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
        "path" to { env, expr ->
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

                root = root[raw.eval(env)]
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
                    // NO-OP for evaluation--handled separately by syntax handlers
                    expr[2].eval(env)
                }
                else -> throw IllegalArgumentException("Bad alias: $expr")
            }
        },
        "select" to { env, expr ->
            if (expr.size < 3 || expr.size > 4) {
                throw IllegalArgumentException("Bad arity on SELECT form $expr: ${expr.size}")
            }

            // FIXME - don't use arity, use the tag name

            val selectExprs = expr[1]
            if (selectExprs !is IonSequence || selectExprs.isEmpty()) {
                throw IllegalArgumentException(
                    "SELECT projection must be non-empty sequence: $selectExprs"
                )
            }

            val selectFunc: (List<ExprValue?>, Bindings) -> ExprValue = when (selectExprs[0].text) {
                "*" -> {
                    if (selectExprs.size != 1) {
                        throw IllegalArgumentException("SELECT * must be a singleton list")
                    }
                    // FIXME select * doesn't project ordered tuples
                    // TODO this should work for very specific cases...
                    { joinedValues, locals -> applyToNewStruct { projectAllInto(joinedValues) } }
                }
                "list" -> {
                    if (selectExprs.size < 2) {
                        throw IllegalArgumentException(
                            "SELECT ... must have at least one expression"
                        )
                    }
                    val selectNames = aliasExtractor(selectExprs.asSequence().drop(1));

                    { joinedValues, locals ->
                        applyToNewStruct {
                            projectSelectList(locals, selectExprs.asSequence().drop(1), selectNames)
                        }
                    }
                }
                "values" -> {
                    if (selectExprs.size != 2) {
                        throw IllegalArgumentException("SELECT VALUES must have a single expression")
                    }
                    { joinedValues, locals ->
                        selectExprs[1].eval(locals)
                    }
                }
                else -> throw IllegalArgumentException("Invalid node in SELECT: $selectExprs")
            }

            val fromValues = expr[2].asSequence()
                .drop(1)
                .map { it.eval(env) }
                .toList()
            val fromNames = aliasExtractor(expr[2].asSequence().drop(1))

            val whereExpr = when {
                expr.size > 3 -> expr[3][1]
                else -> null
            }

            SequenceExprValue(ion) {
                // compute the join over the data sources
                fromValues.product().asSequence()
                    .map { joinedValues ->
                        // bind the joined value to the bindings for the filter/project
                        Pair(joinedValues, joinedValues.bind(env, fromNames))
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
                        selectFunc(joinedValues, locals)
                    }
            }
        }
    )

    private fun applyToNewStruct(applicator: IonStruct.() -> Unit): ExprValue =
        ion.newEmptyStruct().apply(applicator).seal().exprValue()

    /** Dispatch table for built-in functions. */
    private val builtins: Map<String, (Bindings, List<ExprValue>) -> ExprValue> = mapOf(
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
            val names = ArrayList<String>(args.size)
            ion.newEmptyStruct().apply {
                for (arg in args) {
                    val value = arg.ionValue
                    when (value) {
                        is IonSequence -> when (value.size) {
                            2 -> {
                                val name = value[0].text
                                val child = value[1].clone()
                                names.add(name)
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
            }.seal().exprValue().orderedNamesValue(names)
        },
        "exists" to { env, args ->
            when (args.size) {
                1 -> {
                    args[0].asSequence().any().exprValue()
                }
                else -> throw IllegalArgumentException(
                    "Expected a single argument for exists: ${args.size}"
                )
            }
        },
        // TODO make this a proper aggregate
        "count" to { env, args ->
            when (args.size) {
                1 -> {
                    args[0].asSequence().count().exprValue()
                }
                else -> throw IllegalArgumentException(
                    "Expected a single argument for count: ${args.size}"
                )
            }
        },
        "__limit" to { env, args ->
            when (args.size) {
                2 -> {
                    val limit = args[1].numberValue().toInt()
                    SequenceExprValue(ion) {
                        args[0].asSequence().take(limit)
                    }
                }
                else -> throw IllegalArgumentException(
                    "Expected a single argument for limit: ${args.size}"
                )
            }
        }
        // TODO finish implementing "standard" functions
    )

    private val functions = builtins + userFuncs

    private fun IonStruct.projectAllInto(joinedValues: List<ExprValue?>) {
        joinedValues.forEachIndexed { col, joinValue ->
            val ionVal = joinValue?.ionValue!!
            when (ionVal) {
                is IonStruct -> {
                    for (child in ionVal) {
                        val name = child.fieldName
                        add(name, child.clone())
                    }
                }
                else -> {
                    add(SYS_VALUE, ionVal.clone())
                }
            }
        }
    }

    private fun IonStruct.projectSelectList(locals: Bindings,
                                            exprs: Sequence<IonValue>,
                                            aliases: List<String>) {
        exprs.forEachIndexed { col, raw ->
            var name = aliases[col]
            val value = raw.eval(locals)
            add(name, value.ionValue.clone())
        }
    }

    private fun List<ExprValue?>.bind(parent: Bindings, aliases: List<String>): Bindings {
        val locals = map { it?.bind(Bindings.empty()) }

        return Bindings.over { name ->
            val found = locals.asSequence()
                .mapIndexed { col, value ->
                    when {
                        // the alias binds to the value itself
                        aliases[col] == name -> this[col]!!
                        // otherwise scope look up within the value
                        else -> value?.get(name)
                    }
                }
                .filter { it != null }
                .toList()
            when (found.size) {
                // nothing found at our scope, go to parent
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
        is Int -> ion.newInt(this)
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
                // TODO determine if we should fail here when the member doesn't exist
                return bind(Bindings.empty())[name] ?:
                    ion.newNull().seal().exprValue()
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

    /** Parses the given source into an s-expression syntax tree. */
    fun parse(source: String): IonSexp {
        val tokens = tokenizer.tokenize(source)
        return parser.parse(tokens)
    }

    /** Evaluates an unbound syntax tree against a set of bindings. */
    fun eval(ast: IonSexp, env: Bindings) = ast.eval(env)

    /** Compiles the given source expression into a bound [Expression]. */
    override fun compile(source: String): Expression {
        val ast = parse(source)

        return object : Expression {
            override fun eval(env: Bindings): ExprValue = ast.eval(env)
        }
    }
}
