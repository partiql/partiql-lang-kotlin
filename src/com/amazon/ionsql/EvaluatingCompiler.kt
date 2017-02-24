/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql

import com.amazon.ion.*
import java.math.BigDecimal
import java.util.*

/**
 * A basic implementation of the [Compiler] that parses Ion SQL [Token] instances
 * into an [Expression].
 *
 * This implementation produces a very simple AST-walking evaluator as its "compiled" form.
 *
 * @param ion The ion system to use for synthesizing Ion values.
 * @param userFuncs Functions to provide access to in addition to the built-ins.
 */
class EvaluatingCompiler(private val ion: IonSystem,
                         private val parser: Parser,
                         userFuncs: @JvmSuppressWildcards Map<String, (Environment, List<ExprValue>) -> ExprValue>) : Compiler {
    constructor(ion: IonSystem)
        : this(ion, IonSqlParser(ion), emptyMap())
    constructor(ion: IonSystem,
                userFuncs: @JvmSuppressWildcards Map<String, (Environment, List<ExprValue>) -> ExprValue>)
        : this(ion, IonSqlParser(ion), userFuncs)

    private data class FromSource(val name: String, val expr: IonValue)

    private val wildcardPath = ion.newSexp().apply { add().newSymbol("*") }.seal()

    private fun valueName(col: Int): String = "_$col"

    private fun aliasExtractor(seq: Sequence<IonValue>): List<String> =
        seq.mapIndexed { col, value ->
            when (value) {
                is IonSexp -> {
                    when (value[0].text) {
                        "as", "id" -> value[1].text
                        else -> valueName(col)
                    }
                }
                else -> throw IllegalArgumentException("Cannot extract alias out of: $value")
            }
        }.toList()

    /** Dispatch table for AST "op-codes."  */
    private val syntax: Map<String, (Environment, IonSexp) -> ExprValue> = mapOf(
        "lit" to { env, expr ->
            expr[1].exprValue()
        },
        "id" to { env, expr ->
            val name = expr[1].text
            env.current[name] ?:
                throw IllegalArgumentException("No such binding: $name")
        },
        "call" to { env, expr ->
            expr.evalCall(env, startIndex = 1)
        },
        "list" to bindOp(minArity = 0, maxArity = Integer.MAX_VALUE) { env, args ->
            ion.newEmptyList().apply {
                for (value in args) {
                    add(value.ionValue.clone())
                }
            }.seal().exprValue()
        },
        "struct" to bindOp(minArity = 0, maxArity = Integer.MAX_VALUE) { env, args ->
            if (args.size % 2 != 0) {
                throw IllegalArgumentException("struct requires even number of parameters")
            }
            val names = ArrayList<String>(args.size / 2)
            ion.newEmptyStruct().apply {
                (0 until args.size).step(2)
                    .asSequence()
                    .map { args[it].ionValue to args[it + 1].ionValue.clone() }
                    .forEach {
                        val (nameVal, child) = it
                        val name = nameVal.text
                        names.add(name)
                        add(name, child)
                    }
            }.seal().exprValue().orderedNamesValue(names)
        },
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
        "@" to { env, expr ->
            expr[1].eval(env.flipToLocals())
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
            if (expr.size < 3) {
                throw IllegalArgumentException("Bad arity on SELECT form $expr: ${expr.size}")
            }

            val projectExprs = expr[1]
            if (projectExprs !is IonSequence || projectExprs.isEmpty) {
                throw IllegalArgumentException(
                    "SELECT projection node must be non-empty sequence: $projectExprs"
                )
            }
            if (projectExprs[0].text != "project") {
                throw IllegalArgumentException(
                    "SELECT projection is not supported ${projectExprs[0].text}"
                )
            }
            val selectExprs = projectExprs[1]
            if (selectExprs !is IonSequence || selectExprs.isEmpty) {
                throw IllegalArgumentException(
                    "SELECT projection must be non-empty sequence: $selectExprs"
                )
            }

            val selectFunc: (List<ExprValue?>, Environment) -> ExprValue = when (selectExprs[0].text) {
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
                "value" -> {
                    if (selectExprs.size != 2) {
                        throw IllegalArgumentException("SELECT VALUE must have a single expression")
                    }
                    { joinedValues, locals ->
                        selectExprs[1].eval(locals)
                    }
                }
                else -> throw IllegalArgumentException("Invalid node in SELECT: $selectExprs")
            }

            val fromNames = aliasExtractor(expr[2].asSequence().drop(1))
            val fromSources = expr[2].asSequence()
                .drop(1)
                .mapIndexed { idx, expr -> FromSource(fromNames[idx], expr) }
                .toList()
            val fromEnv = env.flipToGlobals()

            var whereExpr: IonValue? = null
            var limitExpr: IonValue? = null
            for (clause in expr.drop(3)) {
                when (clause[0].text) {
                    "where" -> whereExpr = clause[1]
                    "limit" -> limitExpr = clause[1]
                    else -> throw IllegalArgumentException("Unknown clause in SELECT: $clause")
                }
            }

            SequenceExprValue(ion) {
                // compute the join over the data sources
                var seq = fromSources
                    .foldLeftProduct(fromEnv) { env, source ->
                        source.expr.eval(env)
                            .asSequence()
                            .map { value ->
                                // add the correlated binding
                                val childEnv = env.nest(
                                    Bindings.singleton(source.name, value),
                                    useAsCurrent = false
                                )
                                Pair(childEnv, value)
                            }
                            .iterator()
                    }
                    .asSequence()
                    .map { joinedValues ->
                        // bind the joined value to the bindings for the filter/project
                        Pair(joinedValues, joinedValues.bind(env, fromNames))
                    }
                    .filter {
                        val locals = it.second
                        when (whereExpr) {
                            null -> true
                            else -> whereExpr!!.eval(locals).booleanValue()
                        }
                    }
                    .map {
                        val (joinedValues, locals) = it
                        selectFunc(joinedValues, locals)
                    }

                if (limitExpr != null) {
                    seq = seq.take(limitExpr!!.eval(env).ionValue.intValue())
                }

                seq
            }
        }
    )

    private fun applyToNewStruct(applicator: IonStruct.() -> Unit): ExprValue =
        ion.newEmptyStruct().apply(applicator).seal().exprValue()

    /** Dispatch table for built-in functions. */
    private val builtins: Map<String, (Environment, List<ExprValue>) -> ExprValue> = mapOf(
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
                    // construct an artificial tuple for SELECT *
                    add(valueName(col), ionVal.clone())
                }
            }
        }
    }

    private fun IonStruct.projectSelectList(env: Environment,
                                            exprs: Sequence<IonValue>,
                                            aliases: List<String>) {
        exprs.forEachIndexed { col, raw ->
            val name = aliases[col]
            val value = raw.eval(env)
            add(name, value.ionValue.clone())
        }
    }

    private fun List<ExprValue?>.bind(parent: Environment, aliases: List<String>): Environment {
        val locals = map { it?.bindings }

        val bindings = Bindings.over { name ->
            val found = locals.asSequence()
                .mapIndexed { col, value ->
                    when (name) {
                        // the alias binds to the value itself
                        aliases[col] -> this[col]!!
                        // otherwise scope look up within the value
                        else -> value?.get(name)
                    }
                }
                .filter { it != null }
                .toList()
            when (found.size) {
                // nothing found at our scope, return nothing
                0 -> null
                // found exactly one thing, success
                1 -> found.head!!
                // multiple things with the same name is a conflict
                else -> throw IllegalArgumentException(
                    "$name is ambigious: ${found.map { it?.ionValue }}")
            }
        }

        return parent.nest(bindings)
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
                return bindings[name] ?:
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

    private fun IonSexp.evalCall(env: Environment, startIndex: Int): ExprValue {
        val name = this[startIndex].text
        val func = functions[name] ?:
            throw IllegalArgumentException("No such function: $name")
        val argIndex = startIndex + 1
        return evalFunc(env, argIndex, func)
    }

    private fun IonSexp.evalArgs(env: Environment, startIndex: Int): List<ExprValue> =
        (startIndex until size)
            .asSequence()
            .map { this[it] }
            .map { it.eval(env) }
            .toList()

    private fun IonSexp.evalFunc(env: Environment,
                                 argIndex: Int,
                                 func: (Environment, List<ExprValue>) -> ExprValue): ExprValue {
        val args = evalArgs(env, argIndex)
        return func(env, args)
    }

    private fun IonValue.eval(env: Environment): ExprValue {
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
                       op: (Environment, List<ExprValue>) -> ExprValue): (Environment, IonSexp) -> ExprValue {
        return { env, expr ->
            val arity = expr.size - 1
            when {
                arity < minArity -> throw IllegalArgumentException("Not enough arguments: $expr")
                arity > maxArity -> throw IllegalArgumentException("Too many arguments: $expr")
            }
            expr.evalFunc(env, 1, op)
        }
    }

    // TODO support meta-nodes properly for error reporting

    /** Evaluates an unbound syntax tree against a global set of bindings. */
    fun eval(ast: IonSexp, globals: Bindings): ExprValue {
        val env = Environment(globals = globals, locals = globals, current = globals)
        return ast.filterMetaNodes().seal().eval(env)
    }

    /** Compiles the given source expression into a bound [Expression]. */
    override fun compile(source: String): Expression {
        val ast = parser.parse(source)

        return object : Expression {
            override fun eval(globals: Bindings): ExprValue = eval(ast, globals)
        }
    }
}
