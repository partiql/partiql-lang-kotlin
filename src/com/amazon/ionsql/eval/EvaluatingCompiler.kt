/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ion.*
import com.amazon.ionsql.syntax.IonSqlParser
import com.amazon.ionsql.syntax.Parser
import com.amazon.ionsql.syntax.Token
import com.amazon.ionsql.util.*
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
                         userFuncs: @JvmSuppressWildcards
                                    Map<String, (Environment, List<ExprValue>) -> ExprValue>) : Compiler {
    constructor(ion: IonSystem) : this(ion, IonSqlParser(ion), emptyMap())
    constructor(ion: IonSystem,
                userFuncs: @JvmSuppressWildcards
                           Map<String, (Environment, List<ExprValue>) -> ExprValue>)
        : this(ion, IonSqlParser(ion), userFuncs)

    private data class Alias(val asName: String, val atName: String?)
    private data class FromSource(val alias: Alias, val expr: IonValue)

    private enum class PathWildcardKind(val isWildcard: Boolean) {
        NONE(isWildcard = false),
        NORMAL(isWildcard = true),
        UNPIVOT(isWildcard = true)
    }

    private val normalPathWildcard = ion.singleValue("(*)").seal()
    private val unpivotPathWildcard = ion.singleValue("(* unpivot)").seal()

    private val missingValue = object : ExprValue by ion.newNull().seal().exprValue() {
        override val type = ExprValueType.MISSING
    }

    private fun IonValue.determinePathWildcard(): PathWildcardKind = when (this) {
        normalPathWildcard -> PathWildcardKind.NORMAL
        unpivotPathWildcard -> PathWildcardKind.UNPIVOT
        else -> PathWildcardKind.NONE
    }

    private fun valueName(col: Int): String = "_$col"

    private fun IonValue.extractAsName(id: Int) = when (this[0].text) {
        "as", "id" -> this[1].text
        else -> valueName(id)
    }

    private fun aliasExtractor(seq: Sequence<IonValue>): List<Alias> =
        seq.mapIndexed { idx, value ->
            var asName: String = value.extractAsName(idx)
            val atName: String? = when (value[0].text) {
                "at" -> {
                    asName = value[2].extractAsName(idx)
                    value[1].text
                }
                else -> null
            }
            Alias(asName, atName)
        }.toList()

    private val aliasHandler = { env: Environment, expr: IonSexp ->
        when (expr.size) {
            3 -> {
                // NO-OP for evaluation--handled separately by syntax handlers
                expr[2].eval(env)
            }
            else -> throw IllegalArgumentException("Bad alias: $expr")
        }
    }

    private val isHandler = { env: Environment, expr: IonSexp ->
        when (expr.size) {
            3 -> {
                val instance = expr[1].eval(env)
                // TODO consider the type parameters
                val targetType = ExprValueType.fromTypeName(expr[2][1].text)

                when {
                    // MISSING is NULL
                    instance.type == ExprValueType.MISSING
                        && targetType == ExprValueType.NULL -> true
                    else -> instance.type == targetType
                }.exprValue()
            }
            else -> throw IllegalArgumentException("Arity incorrect for 'is'/'is_not': $expr")
        }

    }

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
        "missing" to { env, expr -> missingValue },
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
        "and" to { env, expr ->
            when (expr.size) {
                3 -> expr[1].eval(env).booleanValue() && expr[2].eval(env).booleanValue()
                else -> throw IllegalArgumentException("Arity incorrect for 'and': $expr")
            }.exprValue()
        },
        "is" to isHandler,
        "is_not" to { env, expr ->
            (!isHandler(env, expr).booleanValue()).exprValue()
        },
        "@" to { env, expr ->
            expr[1].eval(env.flipToLocals())
        },
        "path" to { env, expr ->
            if (expr.size < 3) {
                throw IllegalArgumentException("Path arity to low: $expr")
            }

            var curr = expr[1].eval(env)
            var firstWildcardKind = PathWildcardKind.NONE

            // extract all the non-wildcard paths
            var idx = 2
            while (idx < expr.size) {
                val raw = expr[idx]
                firstWildcardKind = raw.determinePathWildcard()
                if (firstWildcardKind.isWildcard) {
                    // need special processing for the rest of the path
                    break
                }

                curr = curr[raw.eval(env)]
                idx++
            }

            // we are either done or we have wild-card paths and beyond
            val components = ArrayList<(ExprValue) -> Sequence<ExprValue>>()
            while (idx < expr.size) {
                val raw = expr[idx]
                val wildcardKind = raw.determinePathWildcard()
                components.add(when (wildcardKind) {
                    // treat the entire value as a sequence
                    PathWildcardKind.NORMAL -> { exprVal ->
                        exprVal.asSequence()
                    }
                    // treat the entire value as a sequence
                    PathWildcardKind.UNPIVOT -> { exprVal ->
                        exprVal.unpivot().asSequence()
                    }
                    // "index" into the value lazily
                    PathWildcardKind.NONE -> { exprVal ->
                        sequenceOf(exprVal[raw.eval(env)])
                    }
                })
                idx++
            }

            when (firstWildcardKind) {
                PathWildcardKind.NONE -> curr
                else -> SequenceExprValue(ion) {
                    if (firstWildcardKind == PathWildcardKind.UNPIVOT) {
                        curr = curr.unpivot()
                    }
                    var seq = sequenceOf(curr)
                    for (component in components) {
                        seq = seq.flatMap(component)
                    }
                    seq
                }
            }
        },
        "as" to aliasHandler,
        "at" to aliasHandler,
        "unpivot" to { env, expr ->
            if (expr.size != 2) {
                throw IllegalArgumentException("UNPIVOT form must have one expression")
            }
            expr[1].eval(env).unpivot()
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
                    val selectNames =
                        aliasExtractor(selectExprs.asSequence().drop(1)).map { it.asName };

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
                                // add the correlated binding(s)
                                val alias = source.alias
                                val childEnv = env.nest(
                                    Bindings.over {
                                        when (it) {
                                            alias.asName -> value
                                            alias.atName -> value.name
                                            else -> null
                                        }
                                    },
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

    private fun List<ExprValue>.bind(parent: Environment, aliases: List<Alias>): Environment {
        val locals = map { it.bindings }

        val bindings = Bindings.over { name ->
            val found = locals.asSequence()
                .mapIndexed { col, value ->
                    when (name) {
                        // the alias binds to the value itself
                        aliases[col].asName -> this[col]
                        // the alias binds to the name of the value
                        aliases[col].atName -> this[col].name
                        // otherwise scope look up within the value
                        else -> value.get(name)
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
                return bindings[name] ?: missingValue
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
            // null is never equal to anything
            first.isNullValue || second.isNullValue -> false
            // arithmetic equality
            first.isNumeric && second.isNumeric ->
                first.numberValue().compareTo(second.numberValue()) == 0
            // text equality for symbols/strings
            first is IonText && second is IonText ->
                first.stringValue() == second.stringValue()
            // defer to strict Ion equality
            else -> first == second
        }
    }

    private val ExprValue.name: ExprValue
        get() = asFacet(Named::class.java)?.name ?: missingValue

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
