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
 * @param userFuncs Functions to provide access to in addition to the built-ins.
 */
class EvaluatingCompiler(private val ion: IonSystem,
                         private val parser: Parser,
                         userFuncs: @JvmSuppressWildcards Map<String, ExprFunction>) : Compiler {
    constructor(ion: IonSystem) : this(ion, IonSqlParser(ion), emptyMap())
    constructor(ion: IonSystem,
                userFuncs: @JvmSuppressWildcards Map<String, ExprFunction>)
        : this(ion, IonSqlParser(ion), userFuncs)

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

    private enum class PathWildcardKind(val isWildcard: Boolean) {
        NONE(isWildcard = false),
        NORMAL(isWildcard = true),
        UNPIVOT(isWildcard = true)
    }

    private val normalPathWildcard = ion.singleValue("(*)").seal()
    private val unpivotPathWildcard = ion.singleValue("(* unpivot)").seal()

    private val missingValue = object : ExprValue by ion.newNull().seal().exprValue() {
        override val type = ExprValueType.MISSING
        override fun toString(): String = stringify()
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

    private val aliasHandler = { ast: IonSexp ->
        when (ast.size) {
            3 -> {
                // TODO we should make the compiler elide this completely
                // NO-OP for evaluation--handled separately by compilation
                val expr = ast[2].compile()
                exprThunk { env -> expr.eval(env) }
            }
            else -> err("Bad alias: $ast")
        }
    }

    private val isHandler = { ast: IonSexp ->
        when (ast.size) {
            3 -> {
                val instanceExpr = ast[1].compile()
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

    /** Dispatch table for AST "op-codes" to thunks.  */
    private val syntax: Map<String, (IonSexp) -> ExprThunk> = mapOf(
        "missing" to { _ ->
            exprThunk { missingValue }
        },
        "lit" to { ast ->
            val literal = ast[1].exprValue()
            exprThunk { literal }
        },
        "id" to { ast ->
            val name = ast[1].text
            exprThunk { env -> env.current[name] ?: err("No such binding: $name") }
        },
        "@" to { ast ->
            val expr = ast[1].compile()
            exprThunk { env -> expr.eval(env.flipToLocals()) }
        },
        "call" to { ast ->
            ast.compileCall(startIndex = 1)
        },
        "cast" to { ast ->
            if (ast.size != 3) {
                err("cast requires two arguments")
            }

            val sourceExpr = ast[1].compile()
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
        "like" to { ast ->
            astToLikeExprThunk(ast)
        },
        "not_like" to { ast ->
            val likeExprThunk = astToLikeExprThunk(ast)
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
        "or" to { ast ->
            when (ast.size) {
                3 -> {
                    val left = ast[1].compile()
                    val right = ast[2].compile()
                    exprThunk { env ->
                        (left.eval(env).booleanValue() || right.eval(env).booleanValue()).exprValue()
                    }
                }
                else -> err("Arity incorrect for 'or': $ast")
            }
        },
        "and" to { ast ->
            when (ast.size) {
                3 -> {
                    val left = ast[1].compile()
                    val right = ast[2].compile()
                    exprThunk { env ->
                        (left.eval(env).booleanValue() && right.eval(env).booleanValue()).exprValue()
                    }
                }
                else -> err("Arity incorrect for 'and': $ast")
            }
        },
        "is" to isHandler,
        "is_not" to { ast ->
            val isExpr = isHandler(ast)
            exprThunk { env ->
                (!isExpr.eval(env).booleanValue()).exprValue()
            }
        },
        "simple_case" to { ast ->
            if (ast.size < 3) {
                err("Arity incorrect for simple case: $ast")
            }

            val targetExpr = ast[1].compile()
            val matchExprs = ast.drop(2).map {
                when (it[0].text) {
                    "when" -> when (it.size) {
                        3 -> {
                            val candidateExpr = it[1].compile()
                            val resultExpr = it[2].compile()

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
                            val elseExpr = it[1].compile()
                            ConditionThunks({ true }, elseExpr)
                        }
                        else -> err("Arity incorrect for 'else': $it")
                    }
                    else -> err("Unexpected syntax in simple case: ${it[0]}")
                }
            }

            matchExprs.compile()
        },
        "searched_case" to { ast ->
            if (ast.size < 2) {
                err("Arity incorrect for searched case: $ast")
            }

            // go through the cases and else and evaluate them
            val matchExprs = ast.drop(1).map {
                when (it[0].text) {
                    "when" -> when (it.size) {
                        3 -> {
                            val condExpr = it[1].compile()
                            val resultExpr = it[2].compile()

                            ConditionThunks(
                                { env -> condExpr.eval(env).booleanValue() },
                                resultExpr
                            )
                        }
                        else -> err("Arity incorrect for 'when': $it")
                    }
                    "else" -> when (it.size) {
                        2 -> {
                            val elseExpr = it[1].compile()
                            ConditionThunks({ true }, elseExpr)
                        }
                        else -> err("Arity incorrect for 'else': $it")
                    }
                    else -> err("Unexpected syntax in search case: ${it[0]}")
                }
            }

            matchExprs.compile()
        },
        "path" to { ast ->
            if (ast.size < 3) {
                err("Path arity to low: $ast")
            }

            var currExpr = ast[1].compile()
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
                val indexExpr = raw.compile()
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
                            val indexExpr = raw.compile();
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
        "unpivot" to { ast ->
            if (ast.size != 2) {
                err("UNPIVOT form must have one expression")
            }
            val expr = ast[1].compile()
            exprThunk { env -> expr.eval(env).unpivot(ion) }
        },
        "select" to { ast ->
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
                        extractAliases(selectForm.asSequence().drop(1)).map { it.asName }
                    val selectExprs =
                        selectForm.drop(1).map { it.compile() };

                    { _, env -> projectSelectList(env, selectExprs, selectNames) }
                }
                "value" -> {
                    if (selectForm.size != 2) {
                        err("SELECT VALUE must have a single expression")
                    }
                    val expr = selectForm[1].compile();

                    { _, env -> expr.eval(env) }
                }
                else -> err("Invalid node in SELECT: $selectForm")
            }

            val sourceThunk = compileQueryWithoutProjection(ast)
            exprThunk { env ->
                SequenceExprValue(
                    ion,
                    sourceThunk(env).map { (joinedValues, env) ->
                        selectFunc(joinedValues, env)
                    }.map {
                        // TODO make this expose the ordinal for ordered sequences
                        // make sure we don't expose the underlying value's name out of a SELECT
                        it.unnamedValue()
                    }
                )
            }
        },
        "pivot" to { ast ->
            if (ast.size < 3) {
                err("Bad arity on PIVOT form $ast: ${ast.size}")
            }

            val memberForm = ast[1]
            if (memberForm !is IonSequence
                || memberForm.size != 3
                || memberForm[0].text != "member") {
                err("PIVOT member node must be of the form (member <name> <value>): $memberForm")
            }
            val nameExpr = memberForm[1].compile()
            val valueExpr = memberForm[2].compile()
            val sourceThunk = compileQueryWithoutProjection(ast)

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
    private fun astToLikeExprThunk(ast: IonSexp): ExprThunk {
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
                val compiledDfaInput = ast[1].compile()
                val dfa = fromLiteralsToDfa(ionVals)

                exprThunk { env ->
                    val value = compiledDfaInput.eval(env).ionValue
                    dfa.run(value.stringValue()).exprValue()
                }
            }
            else -> {
                // pattern and/or escape are not literals, delay compilation of regular expression to DFA till evaluation
                val compiledVal = ast[1].compile()
                val compiledPattern = ast[2].compile()
                val compiledEscape = if (ast.size == 4) {
                    ast[3].compile()
                } else {
                    null
                }

                exprThunk { env ->
                    val value = compiledVal.eval(env).ionValue
                    val pattern = compiledPattern.eval(env).ionValue
                    val escape: IonValue? = compiledEscape?.let { it.eval(env).ionValue }
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
                                  aliases: List<String>): ExprValue {
        val seq = exprs.asSequence().mapIndexed { col, expr ->
            val name = aliases[col].exprValue()
            val value = expr.eval(env)
            value.namedValue(name)
        }

        return SequenceStruct(ion, isOrdered = true, sequence = seq)
    }

    /** Flattens JOINs in an AST to a list of [FromSource] for generating the join product. */
    private fun IonValue.compileFromClauseSources(): List<FromSource> {
        val sourceAst = this as IonSexp
        fun IonSexp.toFromSource(id: Int, joinExpansion: JoinExpansion, filter: ExprThunk?) =
            FromSource(extractAlias(id, this), this.compile(), joinExpansion, filter)

        fun joinSources(joinExpansion: JoinExpansion): List<FromSource> {
            val leftSources = sourceAst[1].compileFromClauseSources()
            val rightAst = sourceAst[2] as IonSexp
            val filter = when {
                sourceAst.size > 3 -> sourceAst[3].compile()
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
    private fun compileQueryWithoutProjection(ast: IonSexp): (Environment) -> Sequence<FromProductionThunks> {
        val fromSources = ast[2][1].compileFromClauseSources()
        val fromNames = fromSources.map { it.alias }

        var whereExpr: ExprThunk? = null
        var limitExpr: ExprThunk? = null
        for (clause in ast.drop(3)) {
            when (clause[0].text) {
                "where" -> whereExpr = clause[1].compile()
                "limit" -> limitExpr = clause[1].compile()
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

    /** Dispatch table for built-in functions. */
    private val builtins: Map<String, ExprFunction> = mapOf(
        "exists" to ExprFunction.over { _, args ->
            when (args.size) {
                1 -> {
                    args[0].asSequence().any().exprValue()
                }
                else -> err("Expected a single argument for exists: ${args.size}")
            }
        },
        // TODO make this a proper aggregate
        "count" to ExprFunction.over { _, args ->
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

    private fun List<ExprValue>.bind(parent: Environment, aliases: List<Alias>): Environment {
        val locals = map { it.bindings }

        val bindings = Bindings.over { name ->
            val found = locals.asSequence()
                .mapIndexed { col, value ->
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

    private fun Boolean.exprValue(): ExprValue = ion.newBool(this).seal().exprValue()

    private fun Number.exprValue(): ExprValue = when (this) {
        is Int -> ion.newInt(this)
        is Long -> ion.newInt(this)
        is Double -> ion.newFloat(this)
        is BigDecimal -> ion.newDecimal(this)
        else -> err("Cannot convert number to expression value: $this")
    }.seal().exprValue()

    private fun String.exprValue(): ExprValue = ion.newString(this).seal().exprValue()

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

    private fun IonSexp.compileCall(startIndex: Int): ExprThunk {
        val name = this[startIndex].text
        val func = functions[name] ?: err("No such function: $name")
        val argIndex = startIndex + 1
        return compileFunc(argIndex, func)
    }

    private fun IonSexp.compileArgs(startIndex: Int): List<ExprThunk> =
        (startIndex until size)
            .asSequence()
            .map { this[it] }
            .map { it.compile() }
            .toList()

    private fun IonSexp.compileFunc(argStartIndex: Int,
                                    func: ExprFunction): ExprThunk {
        val argThunks = compileArgs(argStartIndex)
        return exprThunk { env ->
            val args = argThunks.map { it.eval(env) }
            func.call(env, args)
        }
    }

    private fun IonValue.compile(): ExprThunk {
        if (this !is IonSexp) {
            err("AST node is not s-expression: $this")
        }

        val name = this[0].stringValue() ?:
            err("AST node does not start with non-null string: $this")
        val handler = syntax[name] ?:
            err("No such syntax handler for $name")
        return handler(this)
    }

    private fun bindOp(minArity: Int = 2,
                       maxArity: Int = 2,
                       op: (Environment, List<ExprValue>) -> ExprValue): (IonSexp) -> ExprThunk {
        return { ast ->
            checkArity(ast, minArity, maxArity)
            ast.compileFunc(1, ExprFunction.over(op))
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
        val expr = ast.filterMetaNodes().seal().compile()

        return object : Expression {
            override fun eval(globals: Bindings): ExprValue {
                val env = Environment(globals = globals, locals = globals, current = globals)
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

