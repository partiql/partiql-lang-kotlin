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
    private val nullValue = ion.newNull().seal().exprValue()

    private fun IonValue.determinePathWildcard(): PathWildcardKind = when (this) {
        normalPathWildcard -> PathWildcardKind.NORMAL
        unpivotPathWildcard -> PathWildcardKind.UNPIVOT
        else -> PathWildcardKind.NONE
    }

    private fun IonValue.extractAsName(id: Int) = when (this[0].text) {
        "as", "id" -> this[1].text
        "path" -> {
            var name = syntheticColumnName(id)
            val lastExpr = this[lastIndex]
            when (lastExpr[0].text) {
                "lit" -> {
                    val literal = lastExpr[1]
                    when {
                         literal.isNonNullText -> {
                             name = literal.text
                         }
                    }
                }
            }
            name
        }
        else -> syntheticColumnName(id)
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
            else -> err("Bad alias: $expr")
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
            else -> err("Arity incorrect for 'is'/'is_not': $expr")
        }

    }

    /** Dispatch table for AST "op-codes."  */
    private val syntax: Map<String, (Environment, IonSexp) -> ExprValue> = mapOf(
        "lit" to { _, expr ->
            expr[1].exprValue()
        },
        "id" to { env, expr ->
            val name = expr[1].text
            env.current[name] ?: err("No such binding: $name")
        },
        "missing" to { _, _ -> missingValue },
        "call" to { env, expr ->
            expr.evalCall(env, startIndex = 1)
        },
        "list" to bindOp(minArity = 0, maxArity = Integer.MAX_VALUE) { _, args ->
            ion.newEmptyList().apply {
                for (value in args) {
                    add(value.ionValue.clone())
                }
            }.seal().exprValue()
        },
        "struct" to bindOp(minArity = 0, maxArity = Integer.MAX_VALUE) { _, args ->
            if (args.size % 2 != 0) {
                err("struct requires even number of parameters")
            }
            val names = ArrayList<String>(args.size / 2)
            ion.newEmptyStruct().apply {
                (0 until args.size).step(2)
                    .asSequence()
                    .map { args[it].ionValue to args[it + 1].ionValue.clone() }
                    .forEach { (nameVal, child) ->
                        val name = nameVal.text
                        names.add(name)
                        add(name, child)
                    }
            }.seal().exprValue().orderedNamesValue(names)
        },
        "bag" to bindOp(minArity = 0, maxArity = Integer.MAX_VALUE) { _, args ->
            SequenceExprValue(
                ion,
                args.asSequence().map {
                    // make sure we don't expose any underlying value name/ordinal
                    it.unnamedValue()
                }
            )
        },
        "||" to bindOp { _, args ->
            (args[0].stringValue() + args[1].stringValue()).exprValue()
        },
        "+" to bindOp(minArity = 1, maxArity = 2) { _, args ->
            when (args.size) {
                1 -> {
                    // force interpretation as a number, and do nothing
                    args[0].numberValue()
                    args[0]
                }
                else -> (args[0].numberValue() + args[1].numberValue()).exprValue()
            }
        },
        "-" to bindOp(minArity = 1, maxArity = 2) { _, args ->
            when (args.size) {
                1 -> {
                    -args[0].numberValue()
                }
                else -> args[0].numberValue() - args[1].numberValue()
            }.exprValue()
        },
        "*" to bindOp { _, args ->
            (args[0].numberValue() * args[1].numberValue()).exprValue()
        },
        "/" to bindOp { _, args ->
            (args[0].numberValue() / args[1].numberValue()).exprValue()
        },
        "%" to bindOp { _, args ->
            (args[0].numberValue() % args[1].numberValue()).exprValue()
        },
        "<" to bindOp { _, args ->
            (args[0] < args[1]).exprValue()
        },
        "<=" to bindOp { _, args ->
            (args[0] <= args[1]).exprValue()
        },
        ">" to bindOp { _, args ->
            (args[0] > args[1]).exprValue()
        },
        ">=" to bindOp { _, args ->
            (args[0] >= args[1]).exprValue()
        },
        "=" to bindOp { _, args ->
            args[0].exprEquals(args[1]).exprValue()
        },
        "<>" to bindOp { _, args ->
            (!args[0].exprEquals(args[1])).exprValue()
        },
        "between" to bindOp(minArity = 3, maxArity = 3) { _, args ->
            (args[0] >= args[1] && args[0] <= args[2]).exprValue()
        },
        "not_between" to bindOp(minArity = 3, maxArity = 3) { _, args ->
            (!(args[0] >= args[1] && args[0] <= args[2])).exprValue()
        },
        "in" to bindOp { _, args ->
            val needle = args[0]
            args[1].asSequence().any { needle.exprEquals(it) }.exprValue()
        },
        "not_in" to bindOp { _, args ->
            val needle = args[0]
            (!args[1].asSequence().any { needle.exprEquals(it) }).exprValue()
        },
        "not" to bindOp(minArity = 1, maxArity = 1) { _, args ->
            (!args[0].booleanValue()).exprValue()
        },
        "or" to { env, expr ->
            when (expr.size) {
                3 -> expr[1].eval(env).booleanValue() || expr[2].eval(env).booleanValue()
                else -> err("Arity incorrect for 'or': $expr")
            }.exprValue()
        },
        "and" to { env, expr ->
            when (expr.size) {
                3 -> expr[1].eval(env).booleanValue() && expr[2].eval(env).booleanValue()
                else -> err("Arity incorrect for 'and': $expr")
            }.exprValue()
        },
        "is" to isHandler,
        "is_not" to { env, expr ->
            (!isHandler(env, expr).booleanValue()).exprValue()
        },
        "simple_case" to { env, expr ->
            if (expr.size < 3) {
                err("Arity incorrect for simple case: $expr")
            }

            val target = expr[1].eval(env)
            val match = expr.asSequence().drop(2).map {
                when (it[0].text) {
                    "when" -> when (it.size) {
                        3 -> when (target.exprEquals(it[1].eval(env))) {
                            true -> it[2].eval(env)
                            else -> null
                        }
                        else -> err("Arity incorrect for 'when': $it")
                    }
                    "else" -> when (it.size) {
                        2 -> it[1].eval(env)
                        else -> err("Arity incorrect for 'else': $it")
                    }
                    else -> err("Unexpected syntax in simple case: ${it[0]}")
                }
            }.find { it != null }

            match ?: nullValue
        },
        "searched_case" to { env, expr ->
            if (expr.size < 2) {
                err("Arity incorrect for searched case: $expr")
            }

            // go through the cases and else and evaluate them
            val match = expr.asSequence().drop(1).map {
                when (it[0].text) {
                    "when" -> when (it.size) {
                        3 -> when (it[1].eval(env).booleanValue()) {
                            true -> it[2].eval(env)
                            else -> null
                        }
                        else -> err("Arity incorrect for 'when': $it")
                    }
                    "else" -> when (it.size) {
                        2 -> it[1].eval(env)
                        else -> err("Arity incorrect for 'else': $it")
                    }
                    else -> err("Unexpected syntax in search case: ${it[0]}")
                }
            }.find { it != null }

            match ?: nullValue
        },
        "@" to { env, expr ->
            expr[1].eval(env.flipToLocals())
        },
        "path" to { env, expr ->
            if (expr.size < 3) {
                err("Path arity to low: $expr")
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
                        exprVal.unpivot(ion).asSequence()
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
                else -> {
                    if (firstWildcardKind == PathWildcardKind.UNPIVOT) {
                        curr = curr.unpivot(ion)
                    }
                    var seq = sequenceOf(curr)
                    for (component in components) {
                        seq = seq.flatMap(component)
                    }

                    SequenceExprValue(ion, seq)
                }
            }
        },
        "as" to aliasHandler,
        "at" to aliasHandler,
        "unpivot" to { env, expr ->
            if (expr.size != 2) {
                err("UNPIVOT form must have one expression")
            }
            expr[1].eval(env).unpivot(ion)
        },
        "select" to { env, expr ->
            if (expr.size < 3) {
                err("Bad arity on SELECT form $expr: ${expr.size}")
            }

            val projectExprs = expr[1]
            if (projectExprs !is IonSequence || projectExprs.isEmpty) {
                err("SELECT projection node must be non-empty sequence: $projectExprs")
            }
            if (projectExprs[0].text != "project") {
                err("SELECT projection is not supported ${projectExprs[0].text}")
            }
            val selectExprs = projectExprs[1]
            if (selectExprs !is IonSequence || selectExprs.isEmpty) {
                err("SELECT projection must be non-empty sequence: $selectExprs")
            }

            val selectFunc: (List<ExprValue>, Environment) -> ExprValue = when (selectExprs[0].text) {
                "*" -> {
                    if (selectExprs.size != 1) {
                        err("SELECT * must be a singleton list")
                    }
                    // FIXME select * doesn't project ordered tuples
                    // TODO this should work for very specific cases...
                    { joinedValues, _ -> applyToNewStruct { projectAllInto(joinedValues) } }
                }
                "list" -> {
                    if (selectExprs.size < 2) {
                        err("SELECT ... must have at least one expression")
                    }
                    val selectNames =
                        aliasExtractor(selectExprs.asSequence().drop(1)).map { it.asName };

                    { _, locals ->
                        applyToNewStruct {
                            projectSelectList(locals, selectExprs.asSequence().drop(1), selectNames)
                        }.orderedNamesValue(selectNames)
                    }
                }
                "value" -> {
                    if (selectExprs.size != 2) {
                        err("SELECT VALUE must have a single expression")
                    }
                    { _, locals ->
                        selectExprs[1].eval(locals)
                    }
                }
                else -> err("Invalid node in SELECT: $selectExprs")
            }

            SequenceExprValue(
                ion,
                evalQueryWithoutProjection(env, expr).map { (joinedValues, locals) ->
                    selectFunc(joinedValues, locals)
                }.map {
                    // TODO make this expose the ordinal for ordered sequences
                    // make sure we don't expose the underlying value's name out of a SELECT
                    it.unnamedValue()
                }
            )
        },
        "pivot" to { env, expr ->
            if (expr.size < 3) {
                err("Bad arity on PIVOT form $expr: ${expr.size}")
            }

            val memberExpr = expr[1]
            if (memberExpr !is IonSequence
                    || memberExpr.size != 3
                    || memberExpr[0].text != "member") {
                err("PIVOT member node must be of the form (member <name> <value>): $memberExpr")
            }
            val nameExpr = memberExpr[1]
            val valueExpr = memberExpr[2]

            // TODO support ordered names (when underlying sequence is ordered)
            // TODO support doing this as a lazy value
            ion.newEmptyStruct().apply {
                evalQueryWithoutProjection(env, expr).map { (_, locals) ->
                    Pair(nameExpr.eval(locals), valueExpr.eval(locals))
                }.filter { (nameVal, _) ->
                    nameVal.type.isText
                }.forEach { (nameVal, valueVal) ->
                    val name = nameVal.stringValue()
                    val value = valueVal.ionValue.clone()
                    add(name, value)
                }
            }.seal().exprValue()
        }
    )

    /** Evaluates the clauses of the SELECT or PIVOT without generating the final projection. */
    private fun evalQueryWithoutProjection(env: Environment,
                                           expr: IonSexp): Sequence<Pair<List<ExprValue>, Environment>> {
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
                else -> err("Unknown clause in SELECT: $clause")
            }
        }

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

        if (limitExpr != null) {
            // TODO determine if this needs to be scoped over projection
            seq = seq.take(limitExpr.eval(env).ionValue.intValue())
        }

        return seq
    }

    private fun applyToNewStruct(applicator: IonStruct.() -> Unit): ExprValue =
        ion.newEmptyStruct().apply(applicator).seal().exprValue()

    /** Dispatch table for built-in functions. */
    private val builtins: Map<String, (Environment, List<ExprValue>) -> ExprValue> = mapOf(
        "exists" to { _, args ->
            when (args.size) {
                1 -> {
                    args[0].asSequence().any().exprValue()
                }
                else -> err("Expected a single argument for exists: ${args.size}")
            }
        },
        // TODO make this a proper aggregate
        "count" to { _, args ->
            when (args.size) {
                1 -> {
                    args[0].asSequence().count().exprValue()
                }
                else -> err("Expected a single argument for count: ${args.size}")
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
                    add(syntheticColumnName(col), ionVal.clone())
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
                        else -> value[name]
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
                else -> err("$name is ambigious: ${found.map { it?.ionValue }}")
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
        else -> err("Cannot convert number to expression value: $this")
    }.seal().exprValue()

    private fun String.exprValue(): ExprValue = ion.newString(this).seal().exprValue()

    private operator fun ExprValue.get(index: ExprValue): ExprValue {
        val indexVal = index.ionValue
        return when (indexVal) {
            is IonInt -> ionValue[indexVal.intValue()].exprValue()
            is IonText -> {
                val name = indexVal.stringValue()
                // delegate to bindings logic as the scope of lookup by name
                return bindings[name] ?: missingValue
            }
            else -> err("Cannot convert index to int/string: $indexVal")
        }
    }

    private val ExprValue.name get() = asFacet(Named::class.java)?.name ?: missingValue

    private val IonValue.text get() = stringValue() ?: err("Expected non-null string: $this")

    private fun IonSexp.evalCall(env: Environment, startIndex: Int): ExprValue {
        val name = this[startIndex].text
        val func = functions[name] ?: err("No such function: $name")
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
            err("AST node is not s-expression: $this")
        }

        val name = this[0].stringValue() ?:
            err("AST node does not start with non-null string: $this")
        val handler = syntax[name] ?:
            err("No such syntax handler for $name")
        return handler(env, this)
    }

    private fun bindOp(minArity: Int = 2,
                       maxArity: Int = 2,
                       op: (Environment, List<ExprValue>) -> ExprValue): (Environment, IonSexp) -> ExprValue {
        return { env, expr ->
            val arity = expr.size - 1
            when {
                arity < minArity -> err("Not enough arguments: $expr")
                arity > maxArity -> err("Too many arguments: $expr")
            }
            expr.evalFunc(env, 1, op)
        }
    }

    // TODO support meta-nodes properly for error reporting

    /** Evaluates an unbound syntax tree against a global set of bindings. */
    fun eval(ast: IonSexp, globals: Bindings): ExprValue {
        val env = Environment(globals = globals, locals = globals, current = globals)
        try {
            return ast.filterMetaNodes().seal().eval(env)
        } catch (e: EvaluationException) {
            throw e
        } catch (e: Exception) {
            val message = e.message ?: "<NO MESSAGE>"
            throw EvaluationException("Internal error, $message", e)
        }
    }

    /** Compiles the given source expression into a bound [Expression]. */
    override fun compile(source: String): Expression {
        val ast = parser.parse(source)

        return object : Expression {
            override fun eval(globals: Bindings): ExprValue = eval(ast, globals)
        }
    }
}
