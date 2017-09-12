/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ion.IonSequence
import com.amazon.ion.IonSexp
import com.amazon.ion.IonSystem
import com.amazon.ion.IonValue
import com.amazon.ionsql.syntax.IonSqlParser
import com.amazon.ionsql.syntax.Parser
import com.amazon.ionsql.syntax.Token
import com.amazon.ionsql.util.*
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * A basic implementation of the [Compiler] that parses Ion SQL [Token] instances
 * into an [Expression].
 *
 * This implementation produces a "compiled" form consisting of context-threaded
 * code in the form of a tree of thunks.  An overview of this technique can be found
 * [here][1]
 *
 * **Note:** *threaded* in this context is used in how the code gets *threaded* together for
 * interpretation and **not** the concurrency primitive.
 *
 * [1]: https://www.complang.tuwien.ac.at/anton/lvas/sem06w/fest.pdf
 *
 * @param ion The ion system to use for synthesizing Ion values.
 * @param userFunctions Functions to provide access to in addition to the built-ins.
 */
class EvaluatingCompiler(private val ion: IonSystem,
                         private val parser: Parser,
                         private val userFunctions: @JvmSuppressWildcards Map<String, ExprFunction>) : Compiler {


    constructor(ion: IonSystem) :
        this(ion, IonSqlParser(ion), emptyMap())

    constructor(ion: IonSystem,
                userFuncs: @JvmSuppressWildcards Map<String, ExprFunction>):
        this(ion, IonSqlParser(ion), userFuncs)


    /** An [ExprValue] which is the result of an expression evaluated in an [Environment].  */
    private interface ExprThunk {
        fun eval(env: Environment): ExprValue
    }

    private fun exprThunk(thunk: (Environment) -> ExprValue) = object : ExprThunk {
        // TODO make this memoize the result when valid and possible
        override fun eval(env: Environment) = thunk(env)
    }

    /** Specifies the expansion for joins. */
    private enum class JoinExpansion {
        /** Default for non-joined values, CROSS and INNER JOIN. */
        INNER,
        /** Expansion mode for LEFT/RIGHT/FULL JOIN. */
        OUTER
    }

    private data class Alias(val asName: String, val atName: String?)

    private data class FromSource(val alias: Alias,
                                  val expr: ExprThunk,
                                  val joinExpansion: JoinExpansion,
                                  val filter: ExprThunk?)

    /** A condition clause for `CASE`. */
    private data class ConditionThunks(val cond: (Environment) -> Boolean, val resultExpr: ExprThunk)

    /** Compiles the list of [ConditionThunks] as the body of a `CASE`-`WHEN`. */
    private fun List<ConditionThunks>.compile(): ExprThunk = exprThunk { env ->
        find { (condThunk, _) ->
            condThunk(env)
        }?.resultExpr?.eval(env) ?: nullValue
    }

    /**
     * Represents a single `FROM` source production of values.
     *
     * @param values A single production of values from the `FROM` source.
     * @param env The environment scoped to the values of this production.
     */
    private data class FromProductionThunks(val values: List<ExprValue>,
                                            val env: Environment)

    /**
     * Represents an aggregate expression within a `SELECT <list>`.
     *
     * It can be thought of as the components of the following projection:
     *
     * ```
     * SELECT <aggregator>(<inputExpr>) FROM ...
     * ```
     *
     * @param aggregatorFactory The aggregate function factory for this aggregate.
     * @param inputExpr The expression to evaluate for each instance of the data source.
     * @param registerId The ID of the register in the [RegisterBank] allocated for the
     *  [ExprAggregator] intermediate state for execution
     */
    private class Aggregate(val aggregatorFactory: ExprAggregatorFactory,
                            val inputExpr: ExprThunk,
                            val registerId: Int)

    private enum class PathWildcardKind(val isWildcard: Boolean) {
        NONE(isWildcard = false),
        NORMAL(isWildcard = true),
        UNPIVOT(isWildcard = true)
    }

    private val normalPathWildcard = ion.singleValue("(*)").seal()
    private val unpivotPathWildcard = ion.singleValue("(* unpivot)").seal()

    private val missingValue = missingExprValue(ion)
    private val nullValue = nullExprValue(ion)

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

    private fun extractAlias(id: Int, node: IonValue): Alias {
        var asName: String = node.extractAsName(id)
        val atName: String? = when (node[0].text) {
            "at" -> {
                asName = node[2].extractAsName(id)
                node[1].text
            }
            else -> null
        }
        return Alias(asName, atName)
    }

    private fun extractAliases(seq: Sequence<IonValue>): List<Alias> =
        seq.mapIndexed { idx, value -> extractAlias(idx, value) }.toList()

    private val aliasHandler = { cEnv: CompilationEnvironment, ast: IonSexp ->
        when (ast.size) {
            3 -> {
                // TODO we should make the compiler elide this completely
                // NO-OP for evaluation--handled separately by compilation
                val expr = ast[2].compile(cEnv)
                exprThunk { env -> expr.eval(env) }
            }
            else -> err("Bad alias: $ast")
        }
    }

    private val isHandler = { cEnv: CompilationEnvironment, ast: IonSexp ->
        when (ast.size) {
            3 -> {
                val instanceExpr = ast[1].compile(cEnv)
                // TODO consider the type parameters
                val targetType = ExprValueType.fromTypeName(ast[2][1].text)

                exprThunk { env ->
                    val instance = instanceExpr.eval(env)
                    when {
                    // MISSING is NULL
                        instance.type == ExprValueType.MISSING
                            && targetType == ExprValueType.NULL -> true
                        else -> instance.type == targetType
                    }.exprValue()
                }
            }
            else -> err("Arity incorrect for 'is'/'is_not': $ast")
        }

    }

    /** The compilation dispatch table for the AST. */
    private interface CompilationTable {
        /**
         * Returns the compilation routine for a given AST node.
         *
         * @param name The name of the AST node to provide a compilation routine for.
         *
         * @return A function that takes a compiles the [IonSexp] AST node with the given
         *  [CompilationEnvironment].  In general, the [CompilationEnvironment] will be a single global one for the
         *  compiler, but context sensitive compilation (e.g. SQL aggregates) can inject
         *  different compilation behavior by using a custom [CompilationTable] within its environment.
         *  Returns `null` if no handler exists.
         */
        operator fun get(name: String): ((CompilationEnvironment, IonSexp) -> ExprThunk)?
    }

    private fun compilationTableOf(vararg members: Pair<String, (CompilationEnvironment, IonSexp) -> ExprThunk>): CompilationTable {
        val table = mapOf(*members)

        return object : CompilationTable {
            override fun get(name: String) = table[name]
        }
    }

    private fun CompilationTable.delegate(fallback: CompilationTable) = object : CompilationTable {
        override fun get(name: String) = this@delegate[name] ?: fallback[name]
    }

    private fun CompilationTable.blacklist(vararg names: String) = object : CompilationTable {
        override fun get(name: String) = when (name) {
            in names -> null
            else -> this@blacklist[name]
        }
    }

    /**
     * The static environment for the compiler.
     *
     * @param table The syntax table to utilize for compilation.
     * @param regCounter The number of registers for allocated for the compilation unit.
     */
    private data class CompilationEnvironment(val table: CompilationTable, val regCounter: AtomicInteger)

    /** Dispatch table for AST "op-codes" to thunks.  */
    private val syntaxTable = compilationTableOf(
        "missing" to { _, _ ->
            exprThunk { missingValue }
        },
        "lit" to { _, ast ->
            val literal = ast[1].exprValue()
            exprThunk { literal }
        },
        "id" to { _, ast ->
            val name = ast[1].text
            exprThunk { env -> env.current[name] ?: err("No such binding: $name") }
        },
        "@" to { cEnv, ast ->
            val expr = ast[1].compile(cEnv)
            exprThunk { env -> expr.eval(env.flipToLocals()) }
        },
        "call" to { cEnv, ast ->
            ast.compileCall(cEnv, startIndex = 1)
        },
        "call_agg" to { cEnv, ast ->
            ast.compileAggregateCall(cEnv)
        },
        "cast" to { cEnv, ast ->
            if (ast.size != 3) {
                err("cast requires two arguments")
            }

            val sourceExpr = ast[1].compile(cEnv)
            // TODO honor type parameters
            val targetTypeName = ast[2][1].text
            val targetType = ExprValueType.fromTypeName(targetTypeName)
            exprThunk { env -> sourceExpr.eval(env).cast(ion, targetType) }
        },
        "list" to bindOp(minArity = 0, maxArity = Integer.MAX_VALUE) { _, args ->
            SequenceExprValue(
                ion,
                ExprValueType.LIST,
                args.asSequence().mapIndexed { i, arg ->
                    arg.namedValue(i.exprValue())
                }
            )
        },
        "struct" to bindOp(minArity = 0, maxArity = Integer.MAX_VALUE) { _, args ->
            if (args.size % 2 != 0) {
                err("struct requires even number of parameters")
            }

            val seq = (0 until args.size).step(2)
                .asSequence()
                .map {
                    args[it] to args[it + 1]
                }
                .filter { (name, _) ->
                    name.type.isText
                }
                .map { (name, value) ->
                    value.namedValue(name)
                }
            SequenceStruct(ion, isOrdered = true, sequence = seq)
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
        "like" to { cEnv, ast ->
            compileLike(cEnv, ast)
        },
        "not_like" to { cEnv, ast ->
            val likeExprThunk = compileLike(cEnv, ast)
            exprThunk { env -> (!likeExprThunk.eval(env).booleanValue()).exprValue() }
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
        "or" to { cEnv, ast ->
            when (ast.size) {
                3 -> {
                    val left = ast[1].compile(cEnv)
                    val right = ast[2].compile(cEnv)
                    exprThunk { env ->
                        (left.eval(env).booleanValue() || right.eval(env).booleanValue()).exprValue()
                    }
                }
                else -> err("Arity incorrect for 'or': $ast")
            }
        },
        "and" to { cEnv, ast ->
            when (ast.size) {
                3 -> {
                    val left = ast[1].compile(cEnv)
                    val right = ast[2].compile(cEnv)
                    exprThunk { env ->
                        (left.eval(env).booleanValue() && right.eval(env).booleanValue()).exprValue()
                    }
                }
                else -> err("Arity incorrect for 'and': $ast")
            }
        },
        "is" to isHandler,
        "is_not" to { cEnv, ast ->
            val isExpr = isHandler(cEnv, ast)
            exprThunk { env ->
                (!isExpr.eval(env).booleanValue()).exprValue()
            }
        },
        "simple_case" to { cEnv, ast ->
            if (ast.size < 3) {
                err("Arity incorrect for simple case: $ast")
            }

            val targetExpr = ast[1].compile(cEnv)
            val matchExprs = ast.drop(2).map {
                when (it[0].text) {
                    "when" -> when (it.size) {
                        3 -> {
                            val candidateExpr = it[1].compile(cEnv)
                            val resultExpr = it[2].compile(cEnv)

                            ConditionThunks(
                                { env ->
                                    val target = targetExpr.eval(env)
                                    val candidate = candidateExpr.eval(env)
                                    target.exprEquals(candidate)
                                },
                                resultExpr
                            )
                        }
                        else -> err("Arity incorrect for 'when': $it")
                    }
                    "else" -> when (it.size) {
                        2 -> {
                            val elseExpr = it[1].compile(cEnv)
                            ConditionThunks({ true }, elseExpr)
                        }
                        else -> err("Arity incorrect for 'else': $it")
                    }
                    else -> err("Unexpected syntax in simple case: ${it[0]}")
                }
            }

            matchExprs.compile()
        },
        "searched_case" to { cEnv, ast ->
            if (ast.size < 2) {
                err("Arity incorrect for searched case: $ast")
            }

            // go through the cases and else and evaluate them
            val matchExprs = ast.drop(1).map {
                when (it[0].text) {
                    "when" -> when (it.size) {
                        3 -> {
                            val condExpr = it[1].compile(cEnv)
                            val resultExpr = it[2].compile(cEnv)

                            ConditionThunks(
                                { env -> condExpr.eval(env).booleanValue() },
                                resultExpr
                            )
                        }
                        else -> err("Arity incorrect for 'when': $it")
                    }
                    "else" -> when (it.size) {
                        2 -> {
                            val elseExpr = it[1].compile(cEnv)
                            ConditionThunks({ true }, elseExpr)
                        }
                        else -> err("Arity incorrect for 'else': $it")
                    }
                    else -> err("Unexpected syntax in search case: ${it[0]}")
                }
            }

            matchExprs.compile()
        },
        "path" to { cEnv, ast ->
            if (ast.size < 3) {
                err("Path arity to low: $ast")
            }

            var currExpr = ast[1].compile(cEnv)
            var firstWildcardKind = PathWildcardKind.NONE

            // extract all the non-wildcard paths
            var idx = 2
            while (idx < ast.size) {
                val raw = ast[idx]
                firstWildcardKind = raw.determinePathWildcard()
                if (firstWildcardKind.isWildcard) {
                    // need special processing for the rest of the path
                    break
                }

                val targetExpr = currExpr
                val indexExpr = raw.compile(cEnv)
                currExpr = exprThunk { env ->
                    val target = targetExpr.eval(env)
                    val index = indexExpr.eval(env)
                    target[index]
                }
                idx++
            }

            // we are either done or we have wild-card paths and beyond
            val components = ArrayList<(Environment, ExprValue) -> Sequence<ExprValue>>()
            while (idx < ast.size) {
                val raw = ast[idx]
                val wildcardKind = raw.determinePathWildcard()
                components.add(
                    when (wildcardKind) {
                    // treat the entire value as a sequence
                        PathWildcardKind.NORMAL -> { _, exprVal ->
                            exprVal.rangeOver().asSequence()
                        }
                    // treat the entire value as a sequence
                        PathWildcardKind.UNPIVOT -> { _, exprVal ->
                            exprVal.unpivot(ion).asSequence()
                        }
                    // "index" into the value lazily
                        PathWildcardKind.NONE -> {
                            val indexExpr = raw.compile(cEnv);
                            { env, exprVal ->
                                sequenceOf(exprVal[indexExpr.eval(env)])
                            }
                        }
                    }
                )
                idx++
            }

            when (firstWildcardKind) {
                PathWildcardKind.NONE -> currExpr
                else -> {
                    if (firstWildcardKind == PathWildcardKind.UNPIVOT) {
                        val targetExpr = currExpr
                        currExpr = exprThunk { env -> targetExpr.eval(env).unpivot(ion) }
                    }

                    exprThunk { env ->
                        var seq = sequenceOf(currExpr.eval(env))
                        for (component in components) {
                            seq = seq.flatMap { component(env, it) }
                        }

                        SequenceExprValue(ion, seq)
                    }
                }
            }
        },
        "as" to aliasHandler,
        "at" to aliasHandler,
        "unpivot" to { cEnv, ast ->
            if (ast.size != 2) {
                err("UNPIVOT form must have one expression")
            }
            val expr = ast[1].compile(cEnv)
            exprThunk { env -> expr.eval(env).unpivot(ion) }
        },
        "select" to { cEnv, ast ->
            if (ast.size < 3) {
                err("Bad arity on SELECT form $ast: ${ast.size}")
            }

            val projectForm = ast[1]
            if (projectForm !is IonSequence || projectForm.isEmpty) {
                err("SELECT projection node must be non-empty sequence: $projectForm")
            }
            if (projectForm[0].text != "project") {
                err("SELECT projection is not supported ${projectForm[0].text}")
            }
            val selectForm = projectForm[1]
            if (selectForm !is IonSequence || selectForm.isEmpty) {
                err("SELECT projection must be non-empty sequence: $selectForm")
            }

            // the captured list of legacy SQL style aggregates
            val aggregates: MutableList<Aggregate> = mutableListOf()

            val selectFunc: (List<ExprValue>, Environment) -> ExprValue = when (selectForm[0].text) {
                "*" -> {
                    if (selectForm.size != 1) {
                        err("SELECT * must be a singleton list")
                    }
                    // FIXME select * doesn't project ordered tuples
                    // TODO this should work for very specific cases...
                    { joinedValues, _ -> projectAllInto(joinedValues) }
                }
                "list" -> {
                    if (selectForm.size < 2) {
                        err("SELECT ... must have at least one expression")
                    }
                    val selectNames =
                        extractAliases(selectForm.asSequence().drop(1)).map { it.asName.exprValue() }

                    // nested aggregates within aggregates are not allowed
                    val cEnvInner = cEnv.copy(
                        table = cEnv.table.blacklist("call_agg", "call_agg_wildcard")
                    )
                    val aggHandler = { _: CompilationEnvironment, aggAst: IonSexp ->
                        val aggregate = aggAst.compileAggregateWithinSelectList(cEnvInner)
                        aggregates.add(aggregate)
                        exprThunk { env ->
                            // the result of aggregation is stored in the allocated register
                            env.registers[aggregate.registerId].aggregator.compute()
                        }
                    }
                    // intercept aggregates and COUNT(*)
                    val cEnvOuter = cEnv.copy(
                        table = compilationTableOf(
                            "call_agg" to aggHandler,
                            "call_agg_wildcard" to aggHandler
                        ).delegate(cEnv.table)
                    )

                    val selectExprs =
                        selectForm.drop(1).map { it.compile(cEnvOuter) };

                    { _, env -> projectSelectList(env, selectExprs, selectNames) }
                }
                "value" -> {
                    if (selectForm.size != 2) {
                        err("SELECT VALUE must have a single expression")
                    }
                    val expr = selectForm[1].compile(cEnv);

                    { _, env -> expr.eval(env) }
                }
                else -> err("Invalid node in SELECT: $selectForm")
            }

            val sourceThunk = compileQueryWithoutProjection(cEnv, ast)
            when {
                // generate a singleton with the aggregates
                aggregates.isNotEmpty() -> exprThunk { env ->
                    SequenceExprValue(
                        ion,
                        listOf(nullValue).asSequence()
                            .map {
                                // set up aggregate registers
                                val aggregators = aggregates.map {
                                    val aggregator = it.aggregatorFactory.create()
                                    env.registers[it.registerId] = aggregator
                                    aggregator
                                }

                                // map/filter the source table into the inner-expressions
                                // and accumulate into the aggregators
                                sourceThunk(env).forEach { (_, projectEnv) ->
                                    aggregators.forEachIndexed { i, aggregator ->
                                        val inputExpr = aggregates[i].inputExpr
                                        aggregator.next(inputExpr.eval(projectEnv))
                                    }
                                }

                                // generate the final aggregate projection
                                // XXX this only happens for SELECT list so we can use a dummy list
                                selectFunc(emptyList(), env)
                            }
                    )
                }
                // do normal map/filter
                else -> exprThunk { env ->
                    SequenceExprValue(
                        ion,
                        sourceThunk(env).map { (joinedValues, projectEnv) ->
                            selectFunc(joinedValues, projectEnv)
                        }.map {
                            // TODO make this expose the ordinal for ordered sequences
                            // make sure we don't expose the underlying value's name out of a SELECT
                            it.unnamedValue()
                        }
                    )
                }
            }
        },
        "pivot" to { cEnv, ast ->
            if (ast.size < 3) {
                err("Bad arity on PIVOT form $ast: ${ast.size}")
            }

            val memberForm = ast[1]
            if (memberForm !is IonSequence
                || memberForm.size != 3
                || memberForm[0].text != "member") {
                err("PIVOT member node must be of the form (member <name> <value>): $memberForm")
            }
            val nameExpr = memberForm[1].compile(cEnv)
            val valueExpr = memberForm[2].compile(cEnv)
            val sourceThunk = compileQueryWithoutProjection(cEnv, ast)

            exprThunk { env ->
                val seq = sourceThunk(env)
                    .asSequence()
                    .map { (_, env) ->
                        Pair(nameExpr.eval(env), valueExpr.eval(env))
                    }
                    .filter { (name, _) ->
                        name.type.isText
                    }
                    .map { (name, value) ->
                        value.namedValue(name)
                    }
                // TODO support ordered names (when ORDER BY)
                SequenceStruct(ion, isOrdered = false, sequence = seq)
            }
        }
    )

    /**
     * Given an AST node that represents a `LIKE` predicate return an [ExprThunk] that evaluates a `LIKE` predicate.
     *
     * Three cases
     *
     * 1. All arguments are literals, then compile and run the DFA
     * 1. Search pattern and escape pattern are literals, compile the DFA. Running the DFA is deferred to evaluation time.
     * 1. Pattern or escape (or both) are *not* literals, compile and running of DFA deferred to evaluation time.
     *
     *  @param ast ast node representing a `LIKE` predicate
     *
     * @return a thunk that when provided with an environment evaluates the `LIKE` predicate
     */
    private fun compileLike(cEnv: CompilationEnvironment, ast: IonSexp): ExprThunk {
        // AST should be of the form (like V P [E])
        // where V is the value to match on
        //       P is the pattern
        //       E is the escape character which is optional
        checkArity(ast, 2, 3)
        checkArgsAreIonSexps(ast)


        return when {
            ast.tail.forAll(IonValue::isAstLiteral) -> {
                // value, pattern and optional escape character are literals, compile regular expression to DFA and run
                // DFA
                val ionVals = ast.tail.map { it[1] }
                val dfaInput = ionVals[0]
                val dfa = fromLiteralsToDfa(ionVals.tail)
                val matches = dfa.run(dfaInput.stringValue()).exprValue()
                exprThunk { _ -> matches }
            }

            ast.tail.tail.forAll(IonValue::isAstLiteral) -> {
                // pattern and escape are literals, but value to match against is not
                val ionVals = ast.tail.tail.map { it[1] }
                val compiledDfaInput = ast[1].compile(cEnv)
                val dfa = fromLiteralsToDfa(ionVals)

                exprThunk { env ->
                    val value = compiledDfaInput.eval(env).ionValue
                    dfa.run(value.stringValue()).exprValue()
                }
            }
            else -> {
                // pattern and/or escape are not literals, delay compilation of regular expression to DFA till evaluation
                val compiledVal = ast[1].compile(cEnv)
                val compiledPattern = ast[2].compile(cEnv)
                val compiledEscape = if (ast.size == 4) {
                    ast[3].compile(cEnv)
                } else {
                    null
                }

                exprThunk { env ->
                    val value = compiledVal.eval(env).ionValue
                    val pattern = compiledPattern.eval(env).ionValue
                    val escape: IonValue? = compiledEscape?.eval(env)?.ionValue
                    val (patternString: String, escapeChar: Int?, patternSize) = checkPattern(pattern, escape)
                    val dfa: IDFAState = buildDfaFromPattern(patternString, escapeChar, patternSize)
                    dfa.run(value.stringValue()).exprValue()
                }

            }
        }
    }

    /**
     * Given a list of literals (Ion values) that are part of a `LIKE` predicate build and return the DFA
     *
     * @param ionVals list of literals (Ion values) that comprise of the `LIKE`'s pattern and  optionally escape character
     * @return DFA for the pattern and optional escape character
     */
    private fun fromLiteralsToDfa(ionVals: List<IonValue>): IDFAState {
        val pattern = ionVals[0]
        var escape: IonValue? = null
        if (ionVals.size == 2) {
            escape = ionVals[1]

        }
        val (patternString: String, escapeChar: Int?, patternSize) = checkPattern(pattern, escape)
        val dfa: IDFAState = buildDfaFromPattern(patternString, escapeChar, patternSize)
        return dfa
    }

    /**
     * Given the pattern and optional escape character in a `LIKE` SQL predicate as [IonValue]s
     * check their validity based on the SQL92 spec and return a triple that contains in order
     *
     * - the search pattern as a string
     * - the escape character, possibly `null`
     * - the length of the search pattern. The length of the search pattern is either
     *   - the length of the string representing the search pattern when no escape character is used
     *   - the length of the string representing the search pattern without counting uses of the escape character
     *     when an escape character is used
     *
     * A search pattern is valid when
     * 1. pattern is not null
     * 1. pattern contains characters where `_` means any 1 character and `%` means any string of length 0 or more
     * 1. if the escape character is specified then pattern can be deterministically partitioned into character groups where
     *     1. A length 1 character group consists of any character other than the ESCAPE character
     *     1. A length 2 character group consists of the ESCAPE character followed by either `_` or `%` or the ESCAPE character itself
     *
     * @param pattern search pattern
     * @param escape optional escape character provided in the `LIKE` predicate
     *
     * @return a triple that contains in order the search pattern as a [String], optionally the code point for the escape character if one was provided
     * and the size of the search pattern excluding uses of the escape character
     */
    private fun checkPattern(pattern: IonValue, escape: IonValue?): Triple<String, Int?, Int> {
        val patternString = pattern.stringValue()?.let { it } ?: err("Must provide a non-null value for PATTERN in a LIKE predicate: $pattern")
        escape?.let {
            val escapeChar = checkEscapeChar(escape).codePointAt(0)  // escape is a string of length 1
            val validEscapedChars = setOf('_'.toInt(), '%'.toInt(), escapeChar)
            val iter = patternString.codePointSequence().iterator()
            var count = 0

            while (iter.hasNext()) {
                val current = iter.next()
                if (current == escapeChar) {
                    if (!iter.hasNext()) err("Invalid escape sequence : $patternString")
                    else {
                        if (!validEscapedChars.contains(iter.next())) err("Invalid escape sequence : $patternString")
                    }
                }
                count++
            }
            return Triple(patternString, escapeChar, count)
        }
        return Triple(patternString, null, patternString.length)
    }

    /**
     * Given an [IonValue] to be used as the escape character in a `LIKE` predicate check that it is
     * a valid character based on the SQL Spec.
     *
     *
     * A values is a valid escape when
     * 1. it is 1 character long, and,
     * 1. Cannot be null (SQL92 spec marks this cases as *unknown*)
     *
     * @param escape value provided as an escape character for a `LIKE` predicate
     *
     * @return the escape character as a [String] or throws an exception when the input is invalid
     */
    private fun checkEscapeChar(escape: IonValue): String {
        val escapeChar = escape.stringValue()?.let { it } ?: err("Must provide a value when using ESCAPE in a LIKE predicate: $escape")
        when (escapeChar) {
            "" -> err("Cannot use empty character as ESCAPE character in a LIKE predicate: $escape")
            else -> if (escapeChar.trim().length != 1) err("Escape character must have size 1 : $escapeChar")
        }
        return escapeChar
    }

    /**
     * Given an AST as an [IonSexp] check that all arguments (index 1 to n of the s-exp) are also [IonSexp]
     *
     * @param ast AST node
     *
     * @returns true when all elements of ast are [IonSexp], false otherwise
     */
    private fun checkArgsAreIonSexps(ast: IonSexp) =
        ast.tail.filter { x -> x !is IonSexp }.isEmpty()


    /** Provides SELECT * */
    private fun projectAllInto(joinedValues: List<ExprValue>): ExprValue {
        val seq = joinedValues
            .asSequence()
            .mapIndexed { col, joinValue ->
                when (joinValue.type) {
                    ExprValueType.STRUCT -> joinValue
                    else -> {
                        // construct an artificial tuple for SELECT *
                        val name = syntheticColumnName(col).exprValue()
                        listOf(joinValue.namedValue(name))
                    }
                }
            }.flatMap { it.asSequence() }

        return SequenceStruct(ion, isOrdered = false, sequence = seq)
    }

    /** Provides SELECT <list> */
    private fun projectSelectList(env: Environment,
                                  exprs: List<ExprThunk>,
                                  aliases: List<ExprValue>): ExprValue {
        val seq = exprs.asSequence().mapIndexed { col, expr ->
            val name = aliases[col]
            val value = expr.eval(env)
            value.namedValue(name)
        }

        return SequenceStruct(ion, isOrdered = true, sequence = seq)
    }

    /** Flattens JOINs in an AST to a list of [FromSource] for generating the join product. */
    private fun IonValue.compileFromClauseSources(cEnv: CompilationEnvironment): List<FromSource> {
        val sourceAst = this as IonSexp
        fun IonSexp.toFromSource(id: Int, joinExpansion: JoinExpansion, filter: ExprThunk?) =
            FromSource(extractAlias(id, this), this.compile(cEnv), joinExpansion, filter)

        fun joinSources(joinExpansion: JoinExpansion): List<FromSource> {
            val leftSources = sourceAst[1].compileFromClauseSources(cEnv)
            val rightAst = sourceAst[2] as IonSexp
            val filter = when {
                sourceAst.size > 3 -> sourceAst[3].compile(cEnv)
                else -> null
            }
            return leftSources + rightAst.toFromSource(leftSources.size, joinExpansion, filter)
        }

        return when (sourceAst[0].text) {
            "inner_join" -> joinSources(JoinExpansion.INNER)
            "left_join" -> joinSources(JoinExpansion.OUTER)
            // TODO support this--will require reworking the left fold product approach
            "right_join", "outer_join" -> err("RIGHT and FULL JOIN not supported")
            // base case, a singleton with no special expansion
            else -> listOf(sourceAst.toFromSource(0, JoinExpansion.INNER, null))
        }
    }

    /**
     * Compiles the clauses of the SELECT or PIVOT into a thunk that does not generate
     * the final projection.
     */
    private fun compileQueryWithoutProjection(cEnv: CompilationEnvironment, ast: IonSexp): (Environment) -> Sequence<FromProductionThunks> {
        val fromSources = ast[2][1].compileFromClauseSources(cEnv)
        val fromNames = fromSources.map { it.alias }

        var whereExpr: ExprThunk? = null
        var limitExpr: ExprThunk? = null
        for (clause in ast.drop(3)) {
            when (clause[0].text) {
                "where" -> whereExpr = clause[1].compile(cEnv)
                "limit" -> limitExpr = clause[1].compile(cEnv)
                else -> err("Unknown clause in SELECT: $clause")
            }
        }

        return { rootEnv ->
            val fromEnv = rootEnv.flipToGlobalsFirst()

            // compute the join over the data sources
            var seq = fromSources
                .foldLeftProduct({ env: Environment -> env }) { bindEnv, source ->
                    fun correlatedBind(value: ExprValue): Pair<(Environment) -> Environment, ExprValue> {
                        // add the correlated binding environment thunk
                        val alias = source.alias
                        val nextBindEnv = { env: Environment ->
                            val childEnv = bindEnv(env)
                            childEnv.nest(
                                Bindings.over {
                                    when (it) {
                                        alias.asName -> value
                                        alias.atName -> value.name ?: missingValue
                                        else -> null
                                    }
                                },
                                Environment.CurrentMode.GLOBALS_THEN_LOCALS
                            )
                        }
                        return Pair(nextBindEnv, value)
                    }

                    var iter = source.expr.eval(bindEnv(fromEnv))
                        .rangeOver()
                        .asSequence()
                        .map { correlatedBind(it) }
                        .iterator()

                    if (source.filter != null) {
                        // evaluate the ON-clause (before calculating the outer join NULL)
                        // TODO add facet for ExprValue to directly evaluate theta-joins
                        iter = iter
                            .asSequence()
                            .filter { (bindEnv, _) ->
                                // make sure we operate with lexical scoping
                                val filterEnv = bindEnv(rootEnv).flipToLocals()
                                source.filter.eval(filterEnv).booleanValue()
                            }
                            .iterator()
                    }

                    if (!iter.hasNext()) {
                        iter = when (source.joinExpansion) {
                            JoinExpansion.OUTER -> listOf(correlatedBind(nullValue)).iterator()
                            JoinExpansion.INNER -> iter
                        }
                    }

                    iter
                }
                .asSequence()
                .map { joinedValues ->
                    // bind the joined value to the bindings for the filter/project
                    FromProductionThunks(joinedValues, joinedValues.bind(fromEnv, fromNames))
                }

            if (whereExpr != null) {
                seq = seq.filter { (_, env) ->
                    whereExpr!!.eval(env).booleanValue()
                }
            }

            if (limitExpr != null) {
                // TODO determine if this needs to be scoped over projection
                seq = seq.take(limitExpr!!.eval(rootEnv).numberValue().toInt())
            }

            seq
        }
    }

    // TODO hoist built-ins outside of this implementation--requires some decoupling from IonSystem

    /** Dispatch table for built-in functions. */
    private val builtinFunctions: Map<String, ExprFunction> = mapOf(
        "exists" to ExprFunction.over { _, args ->
            when (args.size) {
                1 -> {
                    args[0].asSequence().any().exprValue()
                }
                else -> err("Expected a single argument for exists: ${args.size}")
            }
        }
        // TODO finish implementing "standard" functions
    )

    private val functions = builtinFunctions + userFunctions

    private inner class Accumulator(var current: Number? = 0L,
                                    private val nextFunc: (Number?, ExprValue) -> Number) : ExprAggregator {
        override fun next(value: ExprValue) {
            current = nextFunc(current, value)
        }
        override fun compute() = current!!.exprValue()
    }

    private fun comparisonAccumulator(cmpFunc: (Number, Number) -> Boolean): (Number?, ExprValue) -> Number = { curr, next ->
        val nextNum = next.numberValue()
        when (curr) {
            null -> nextNum
            else -> when {
                cmpFunc(nextNum, curr) -> nextNum
                else -> curr
            }
        }
    }

    // TODO consolidate this definition with LexerConstants and allow users to provide their own aggregates

    /** Dispatch table for built-in aggregate functions. */
    private val builtinAggregates: Map<String, ExprAggregatorFactory> = mapOf(
        "count" to ExprAggregatorFactory.over {
            Accumulator { curr, _ -> curr!! + 1L }
        },
        "sum" to ExprAggregatorFactory.over {
            Accumulator { curr, next -> curr!! + next.numberValue() }
        },
        "min" to ExprAggregatorFactory.over {
            Accumulator(null, comparisonAccumulator { left, right -> left < right })
        },
        "max" to ExprAggregatorFactory.over {
            Accumulator(null, comparisonAccumulator { left, right -> left > right })
        },
        "avg" to ExprAggregatorFactory.over {
            object : ExprAggregator {
                var sum: Number = 0L
                var count = 0L
                override fun next(value: ExprValue) {
                    sum += value.numberValue()
                    count++
                }
                override fun compute() = (sum / count).exprValue()
            }
        }
    )

    private fun List<ExprValue>.bind(parent: Environment, aliases: List<Alias>): Environment {
        val locals = map { it.bindings }

        val bindings = Bindings.over { name ->
            val found = locals.asSequence()
                .mapIndexed { col, _ ->
                    when (name) {
                        // the alias binds to the value itself
                        aliases[col].asName -> this[col]
                        // the alias binds to the name of the value
                        aliases[col].atName -> this[col].name ?: missingValue
                        else -> null
                    }
                }
                .filter { it != null }
                .toList()
            when (found.size) {
                // nothing found at our scope, attempt to look at the attributes in our variables
                // TODO fix dynamic scoping to be in line with SQL++ rules
                0 -> {
                    locals.asSequence()
                        .map { it[name] }
                        .filter { it != null }
                        .firstOrNull()
                }
                // found exactly one thing, success
                1 -> found.head!!
                // multiple things with the same name is a conflict
                else -> err("$name is ambigious: ${found.map { it?.ionValue }}")
            }
        }

        return parent.nest(bindings)
    }

    private fun Boolean.exprValue(): ExprValue = booleanExprValue(this, ion)

    private fun Number.exprValue(): ExprValue = when (this) {
        is Int -> integerExprValue(this, ion)
        is Long -> integerExprValue(this, ion)
        is Double -> floatExprValue(this, ion)
        is BigDecimal -> decimalExprValue(this, ion)
        else -> err("Cannot convert number to expression value: $this")
    }

    private fun String.exprValue(): ExprValue = stringExprValue(this, ion)

    private operator fun ExprValue.get(member: ExprValue): ExprValue = when {
        member.type == ExprValueType.INT -> {
            val index = member.numberValue().toInt()
            ordinalBindings[index] ?: missingValue
        }
        member.type.isText -> {
            val name = member.stringValue()
            // delegate to bindings logic as the scope of lookup by name
            bindings[name] ?: missingValue
        }
        else -> err("Cannot convert index to int/string: $member")
    }

    private val IonValue.text get() = stringValue() ?: err("Expected non-null string: $this")

    private fun IonSexp.compileCall(cEnv: CompilationEnvironment, startIndex: Int): ExprThunk {
        val name = this[startIndex].text
        val func = functions[name] ?: err("No such function: $name")
        val argIndex = startIndex + 1
        return compileFunc(cEnv, argIndex, func)
    }

    /**
     * Compiles an aggregate function call within a `SELECT <list>` context.
     * Note that the given syntax table should be one suitable for the expression
     * **within** the function call.
     */
    private fun IonSexp.compileAggregateWithinSelectList(cEnv: CompilationEnvironment): Aggregate {
        val aggFactory = loadAggregateFactory()
        val inputExpr = when (this[0].text) {
            "call_agg" -> this[3].compile(cEnv)
            // for wild cards we don't need to materialize anything (i.e. count all the things)
            "call_agg_wildcard" -> exprThunk { _ -> nullValue }
            else -> err("Invalid aggregate node: $this")
        }

        return Aggregate(aggFactory, inputExpr, cEnv.regCounter.getAndIncrement())
    }

    private fun IonSexp.compileAggregateCall(cEnv: CompilationEnvironment): ExprThunk {
        val aggFactory = loadAggregateFactory()
        val argThunk = this[3].compile(cEnv)
        return exprThunk { env ->
            val agg = aggFactory.create()
            val arg = argThunk.eval(env)
            arg.forEach { agg.next(it) }
            agg.compute()
        }
    }

    private fun IonSexp.loadAggregateFactory(): ExprAggregatorFactory {
        val expectedSize = when(this[0].text) {
            "call_agg" -> {
                val qualifier = this[2].text
                if (qualifier != "all") {
                    // TODO support DISTINCT aggregates
                    err("DISTINCT aggregate function call not supported")
                }
                4
            }
            "call_agg_wildcard" -> 2
            else -> err("Invalid aggregate node: $this")
        }
        val name = this[1].text

        if (size != expectedSize) {
            err("Aggregate function call node must have arity of $expectedSize")
        }

        val aggFactory = builtinAggregates[name] ?: err("No such aggregate function: $name")

        return aggFactory
    }

    private fun IonSexp.compileArgs(cEnv: CompilationEnvironment, startIndex: Int): List<ExprThunk> =
        (startIndex until size)
            .asSequence()
            .map { this[it] }
            .map { it.compile(cEnv) }
            .toList()

    private fun IonSexp.compileFunc(cEnv: CompilationEnvironment,
                                    argStartIndex: Int,
                                    func: ExprFunction): ExprThunk {
        val argThunks = compileArgs(cEnv, argStartIndex)
        return exprThunk { env ->
            val args = argThunks.map { it.eval(env) }
            func.call(env, args)
        }
    }

    private fun IonValue.compile(cEnv: CompilationEnvironment): ExprThunk {
        if (this !is IonSexp) {
            err("AST node is not s-expression: $this")
        }

        val name = this[0].stringValue() ?:
            err("AST node does not start with non-null string: $this")
        val handler = cEnv.table[name] ?:
            err("No such syntax handler for $name")
        return handler(cEnv, this)
    }

    private fun bindOp(minArity: Int = 2,
                       maxArity: Int = 2,
                       op: (Environment, List<ExprValue>) -> ExprValue): (CompilationEnvironment, IonSexp) -> ExprThunk {
        return { cEnv, ast ->
            checkArity(ast, minArity, maxArity)
            ast.compileFunc(cEnv, 1, ExprFunction.over(op))
        }
    }

    private fun checkArity(ast: IonSexp, minArity: Int, maxArity: Int) {
        val arity = ast.size - 1
        when {
            arity < minArity -> err("Not enough arguments: $ast")
            arity > maxArity -> err("Too many arguments: $ast")
        }
    }

    // TODO support meta-nodes properly for error reporting

    /** Evaluates an unbound syntax tree against a global set of bindings. */
    fun eval(ast: IonSexp, globals: Bindings): ExprValue = compile(ast).eval(globals)

    /** Compiles a syntax tree to an [Expression]. */
    fun compile(ast: IonSexp): Expression {
        val cEnv = CompilationEnvironment(syntaxTable, AtomicInteger(0))
        val expr = ast.filterMetaNodes().seal().compile(cEnv)
        val registerCount = cEnv.regCounter.get()

        return object : Expression {
            override fun eval(globals: Bindings): ExprValue {
                val env = Environment(
                    globals = globals,
                    locals = globals,
                    current = globals,
                    registers = RegisterBank(registerCount)
                )
                try {
                    return expr.eval(env)
                } catch (e: EvaluationException) {
                    throw e
                } catch (e: Exception) {
                    val message = e.message ?: "<NO MESSAGE>"
                    throw EvaluationException("Internal error, $message", e)
                }
            }
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

