/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ion.*
import com.amazon.ionsql.errors.*
import com.amazon.ionsql.eval.EvaluatingCompiler.UnknownsPropagationPolicy.*
import com.amazon.ionsql.eval.ExprValueType.*
import com.amazon.ionsql.eval.binding.*
import com.amazon.ionsql.eval.builtins.*
import com.amazon.ionsql.syntax.*
import com.amazon.ionsql.util.*
import java.math.*
import java.util.*
import java.util.Collections.*
import java.util.concurrent.atomic.*

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
                         private val userFunctions: @JvmSuppressWildcards Map<String, ExprFunction>,
                         private val compileOptions: CompileOptions = CompileOptions.standard()) : Compiler {

    constructor(ion: IonSystem) :
        this(ion, IonSqlParser(ion), emptyMap(), CompileOptions.standard())

    constructor(ion: IonSystem, compileOptions: CompileOptions) :
        this(ion, IonSqlParser(ion), emptyMap(), compileOptions)

    constructor(ion: IonSystem, parser: Parser, userFunctions: @JvmSuppressWildcards Map<String, ExprFunction>) :
        this(ion, parser, userFunctions, CompileOptions.standard())

    constructor(ion: IonSystem, userFuncs: @JvmSuppressWildcards Map<String, ExprFunction>) :
        this(ion, IonSqlParser(ion), userFuncs, CompileOptions.standard())

    /** An [ExprValue] which is the result of an expression evaluated in an [Environment].  */
    private interface ExprThunk {
        fun eval(env: Environment): ExprValue
    }
    /** The value of [eval] doesn't depend on the environment (i.e. is available at compile time) */
    private class ConstantExprThunk(val value: ExprValue) : ExprThunk {
        override fun eval(env: Environment) = value;
    }

    private abstract class RuntimeExprThunk(private val metadata: NodeMetadata? = null) : ExprThunk {
        override fun eval(env: Environment): ExprValue {
            try {
                return innerEval(env)
            } catch (e: EvaluationException) {
                when {
                    e.errorContext == null ->
                        throw EvaluationException(
                            message = e.message,
                            errorCode = e.errorCode,
                            errorContext = metadata?.toErrorContext(),
                            cause = e,
                            internal = e.internal)
                    else -> {
                        metadata?.fillErrorContext(e.errorContext)

                        throw e
                    }
                }
            } catch (e: Exception) {
                val message = e.message ?: "<NO MESSAGE>"
                throw EvaluationException("Internal error, $message",
                                          errorContext = metadata?.toErrorContext(),
                                          cause = e,
                                          internal = true)
            }
        }

        protected abstract fun innerEval(env: Environment): ExprValue
    }

    private fun exprThunk(metadata: NodeMetadata?, thunk: (Environment) -> ExprValue) = object : RuntimeExprThunk(metadata) {
        // TODO make this memoize the result when valid and possible
        override fun innerEval(env: Environment) = thunk(env)
    }

    /** Specifies the expansion for joins. */
    private enum class JoinExpansion {
        /** Default for non-joined values, CROSS and INNER JOIN. */
        INNER,
        /** Expansion mode for LEFT/RIGHT/FULL JOIN. */
        OUTER
    }


    private data class FromSource(val alias: Alias,
                                  val expr: ExprThunk,
                                  val joinExpansion: JoinExpansion,
                                  val filter: ExprThunk?)

    /** A condition clause for `CASE`. */
    private data class ConditionThunks(val cond: (Environment) -> Boolean, val resultExpr: ExprThunk)

    /** Compiles the list of [ConditionThunks] as the body of a `CASE`-`WHEN`. */
    private fun List<ConditionThunks>.compile(metadata: NodeMetadata?): ExprThunk = exprThunk(metadata) { env ->
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
    private val intOneValue = integerExprValue(1, ion)

    private fun IonValue.determinePathWildcard(): PathWildcardKind = when (this) {
        normalPathWildcard -> PathWildcardKind.NORMAL
        unpivotPathWildcard -> PathWildcardKind.UNPIVOT
        else -> PathWildcardKind.NONE
    }

    /**
     * Flattens a tree of an associative operation to a list of terms.
     *
     * Example:
     *
     *     (or (or (or a b) c) (or d (and (and e f) (or g h))))   # Original
     *     (or a b c d (and (and e f) (or g h))))                 # One call to this method
     *     (or a b c d (and e f (or g h)))                        # Recursive visitation
     *
     */
    private fun IonValue.flattenAssociativeOp(): Sequence<IonValue> {
        val opName = this[0].stringValue()

        return this.asSequence().drop(1).flatMap { child ->
            when {
                child[0].stringValue() == opName -> child.flattenAssociativeOp()
                else -> sequenceOf(child)
            }
        }
    }

    private fun IonValue.extractAsName(id: Int, cEnv: CompilationEnvironment) = when (this[0].text(cEnv)) {
        "as", "id" -> this[1].text(cEnv)
        "path" -> {
            var name = syntheticColumnName(id)
            val lastExpr = this[lastIndex]
            when (lastExpr[0].text(cEnv)) {
                "case_sensitive", "case_insensitive" -> {
                    val valueNode = lastExpr[1]
                    val literal = valueNode[1]
                    if (literal.isNonNullText) {
                        name = literal.text(cEnv)
                    }
                }
                "lit" -> {
                    val literal = lastExpr[1]
                    if (literal.isNonNullText) {
                        name = literal.text(cEnv)
                    }
                }
            }
            name
        }
        else -> syntheticColumnName(id)
    }

    private fun extractAlias(id: Int, node: IonValue, cEnv: CompilationEnvironment): Alias {
        var asName: String = node.extractAsName(id, cEnv)
        val atName: String? = when (node[0].text(cEnv)) {
            "at" -> {
                asName = node[2].extractAsName(id, cEnv)
                node[1].text(cEnv)
            }
            else -> null
        }
        return Alias(asName, atName)
    }

    private fun extractAliases(seq: Sequence<IonValue>, cEnv: CompilationEnvironment): List<Alias> =
        seq.mapIndexed { idx, value -> extractAlias(idx, value, cEnv) }.toList()

    private val aliasHandler = { cEnv: CompilationEnvironment, ast: IonSexp ->
        when (ast.size) {
            3 -> {
                // TODO we should make the compiler elide this completely
                // NO-OP for evaluation--handled separately by compilation
                val expr = ast[2].compile(cEnv)
                exprThunk(cEnv.metadataLookup[ast[2]]) { env -> expr.eval(env) }
            }
            else -> err("Bad alias: $ast", cEnv.metadataLookup[ast]?.toErrorContext(), internal = false)
        }
    }

    private val isHandler = { cEnv: CompilationEnvironment, ast: IonSexp ->
        when (ast.size) {
            3 -> {
                val instanceExpr = ast[1].compile(cEnv)
                // TODO consider the type parameters
                val targetType = ExprValueType.fromTypeName(ast[2][1].text(cEnv))

                exprThunk(cEnv.metadataLookup[ast[1]]) { env ->
                    val instance = instanceExpr.eval(env)
                    when {
                    // MISSING is NULL
                        instance.type == MISSING && targetType == NULL -> true
                        else                                           -> instance.type == targetType
                    }.exprValue()
                }
            }
            else -> err("Arity incorrect for 'is'/'is_not': $ast",
                        cEnv.metadataLookup[ast]?.toErrorContext(),
                        internal = false)
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
     * @param metadataLookup the metadataLookup table from AST nodes to metadata, used to give more detailed error information
     */
    private data class CompilationEnvironment(val table: CompilationTable, val regCounter: AtomicInteger, val metadataLookup: NodeMetadataLookup)

    /** Dispatch table for AST "op-codes" to thunks.  */
    private val syntaxTable = compilationTableOf(
        "missing" to { _, _ ->
            ConstantExprThunk(missingValue)
        },
        "lit" to { cEnv, ast ->
            val ionValue = ast[1]
            val literal = when(ionValue) {
                is IonInt -> {
                    // FIXME move this check to the parser
                    if(ionValue.integerSize == IntegerSize.BIG_INTEGER) {
                        errIntOverflow(cEnv.metadataLookup[ast]?.toErrorContext())
                    }

                    ionValue.exprValue()
                }
                else -> ionValue.exprValue()
            }

            ConstantExprThunk(literal)
        },
        "id" to { cEnv, ast ->
            checkArity(ast, 1, 2, cEnv.metadataLookup[ast])
            val name = ast[1].text(cEnv)
            val bindingCase = when {
                ast.size > 2 -> BindingCase.fromIonValue(ast[2])
                else         -> BindingCase.SENSITIVE
            }

            when (compileOptions.undefinedVariable) {
                UndefinedVariableBehavior.ERROR   -> {
                    val bindingName = BindingName(name, bindingCase)
                    exprThunk(cEnv.metadataLookup[ast]) { env ->
                        env.current[bindingName] ?: throw EvaluationException(
                            "No such binding: $name",
                            ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
                            cEnv.metadataLookup[ast]?.fillErrorContext(
                                PropertyValueMap().also {
                                    it[Property.BINDING_NAME] = name
                                }
                            ),
                            internal = false
                        )
                    }
                }
                UndefinedVariableBehavior.MISSING -> {
                    exprThunk(cEnv.metadataLookup[ast]) { env ->
                        env.current[BindingName(name, bindingCase)] ?: this.missingValue
                    }
                }
            }
        },
        "@" to { cEnv, ast ->
            val expr = ast[1].compile(cEnv)
            exprThunk(cEnv.metadataLookup[ast]) { env -> expr.eval(env.flipToLocals()) }
        },
        "call" to { cEnv, ast ->
            ast.compileCall(cEnv, startIndex = 1)
        },
        "call_agg" to { cEnv, ast ->
            ast.compileAggregateCall(cEnv)
        },
        "cast" to { cEnv, ast ->
            if (ast.size != 3) {
                err("cast requires two arguments", cEnv.metadataLookup[ast]?.toErrorContext(), internal = false)
            }

        val sourceExpr = ast[1].compile(cEnv)
        // TODO honor type parameters
        val targetTypeName = ast[2][1].text(cEnv)
        val targetType = ExprValueType.fromTypeName(targetTypeName)
        exprThunk(cEnv.metadataLookup[ast]) { env ->
            sourceExpr.eval(env).cast(ion, targetType, cEnv.metadataLookup[ast])
        }
    }, "list" to bindOp(minArity = 0, maxArity = Integer.MAX_VALUE, policy = NONE) { _, args ->
        SequenceExprValue(ion, LIST, args.asSequence().mapIndexed { i, arg ->
            arg.namedValue(i.exprValue())
        })
    }, "struct" to bindOp(minArity = 0, maxArity = Integer.MAX_VALUE, policy = NONE) { _, args ->
        if (args.size % 2 != 0) {
            errNoContext("struct requires even number of parameters", internal = false)
        }

        val seq = (0 until args.size).step(2).asSequence().map {
            args[it] to args[it + 1]
        }.filter { (name, _) ->
            name.type.isText
        }.map { (name, value) ->
            value.namedValue(name)
        }
        SequenceStruct(ion, isOrdered = true, sequence = seq)
    }, "bag" to bindOp(minArity = 0, maxArity = Integer.MAX_VALUE, policy = NONE) { _, args ->
        SequenceExprValue(ion, args.asSequence().map {
            // make sure we don't expose any underlying value name/ordinal
            it.unnamedValue()
        })
    }, "||" to bindOp { _, args ->
        (args[0].stringValue() + args[1].stringValue()).exprValue()
    }, "+" to bindOp(minArity = 1, maxArity = 2) { _, args ->
        when (args.size) {
            1    -> {
                // force interpretation as a number, and do nothing
                args[0].numberValue()
                args[0]
            }
            else -> (args[0].numberValue() + args[1].numberValue()).exprValue()
        }
    }, "-" to bindOp(minArity = 1, maxArity = 2) { _, args ->
        when (args.size) {
            1    -> {
                -args[0].numberValue()
            }
            else -> args[0].numberValue() - args[1].numberValue()
        }.exprValue()
    }, "*" to bindOp { _, args ->
        (args[0].numberValue() * args[1].numberValue()).exprValue()
    }, "/" to bindOp { _, args ->
        val denominator = args[1].numberValue();
        if (denominator.isZero()) errNoContext("/ by zero", internal = false)
        try {
            (args[0].numberValue() / denominator).exprValue()
        }
        catch (e: ArithmeticException) {
            throw EvaluationException(cause = e, internal = true)
        }
    }, "%" to bindOp { _, args ->
        (args[0].numberValue() % args[1].numberValue()).exprValue()
    }, "<" to bindOp { _, args ->
        (args[0] < args[1]).exprValue()
    }, "<=" to bindOp { _, args ->
        (args[0] <= args[1]).exprValue()
    }, ">" to bindOp { _, args ->
        (args[0] > args[1]).exprValue()
    }, ">=" to bindOp { _, args ->
        (args[0] >= args[1]).exprValue()
    }, "=" to bindOp { _, args ->
        args[0].exprEquals(args[1]).exprValue()
    }, "<>" to bindOp { _, args ->
        (!args[0].exprEquals(args[1])).exprValue()
    }, "between" to bindOp(minArity = 3, maxArity = 3) { _, args ->
        (args[0] >= args[1] && args[0] <= args[2]).exprValue()
    }, "not_between" to bindOp(minArity = 3, maxArity = 3) { _, args ->
        (!(args[0] >= args[1] && args[0] <= args[2])).exprValue()
    }, "like" to { cEnv, ast ->
        compileLike(cEnv, ast)
    }, "not_like" to { cEnv, ast ->
        val likeExprThunk = compileLike(cEnv, ast)
        exprThunk(cEnv.metadataLookup[ast]) { env -> (!likeExprThunk.eval(env).booleanValue()).exprValue() }
    }, "in" to bindOp { _, args ->
        val needle = args[0]
        args[1].asSequence().any { needle.exprEquals(it) }.exprValue()
    }, "not_in" to bindOp { _, args ->
        val needle = args[0]
        (!args[1].asSequence().any { needle.exprEquals(it) }).exprValue()
    }, "not" to bindOp(minArity = 1, maxArity = 1) { _, args ->
        (!args[0].booleanValue()).exprValue()
    }, "or" to { cEnv, ast ->
        when (ast.size) {
            3    -> {
                val terms: List<ExprThunk> = ast.flattenAssociativeOp().map { it.compile(cEnv) }.toList()
                exprThunk(cEnv.metadataLookup[ast]) { env ->

                    var (hasTrue, hasUnknown) = shortCircuitingFold(terms, env){
                        operand -> (operand.type == BOOL) && operand.booleanValue()
                    }


                    when {
                        hasTrue -> booleanExprValue(true, ion)
                        hasUnknown -> nullValue
                        else -> booleanExprValue(false, ion)
                    }
                }
            }
            else -> err("Arity incorrect for 'or': $ast", cEnv.metadataLookup[ast]?.toErrorContext(), internal = false)
        }
    }, "and" to { cEnv, ast ->
        when (ast.size) {
            3    -> {
                val terms: List<ExprThunk> = ast.flattenAssociativeOp().map { it.compile(cEnv) }.toList()
                exprThunk(cEnv.metadataLookup[ast]) { env ->

                    var (hasFalse, hasUnknown) = shortCircuitingFold(terms, env){
                        operand -> (operand.type == BOOL) && !operand.booleanValue()
                    }

                    when {
                        hasFalse   -> booleanExprValue(false, ion)
                        hasUnknown -> nullValue
                        else       -> booleanExprValue(true, ion)
                    }
                }
            }
            else -> err("Arity incorrect for 'and': $ast", cEnv.metadataLookup[ast]?.toErrorContext(), internal = false)
        }
    }, "is" to isHandler, "is_not" to { cEnv, ast ->
        val isExpr = isHandler(cEnv, ast)
        exprThunk(cEnv.metadataLookup[ast]) { env ->
            (!isExpr.eval(env).booleanValue()).exprValue()
        }
    }, "simple_case" to { cEnv, ast ->
        if (ast.size < 3) {
            err("Arity incorrect for simple case: $ast", cEnv.metadataLookup[ast]?.toErrorContext(), internal = false)
        }

            val targetExpr = ast[1].compile(cEnv)
            val matchExprs = ast.drop(2).map {
                when (it[0].text(cEnv)) {
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
                        else -> err("Arity incorrect for 'when': $it", cEnv.metadataLookup[it]?.toErrorContext(), internal = false)
                    }
                    "else" -> when (it.size) {
                        2 -> {
                            val elseExpr = it[1].compile(cEnv)
                            ConditionThunks({ true }, elseExpr)
                        }
                        else -> err("Arity incorrect for 'else': $it", cEnv.metadataLookup[it]?.toErrorContext(), internal = false)
                    }
                    else -> err("Unexpected syntax in simple case: ${it[0]}", cEnv.metadataLookup[it[0]]?.toErrorContext(), internal = false)
                }
            }

            matchExprs.compile(cEnv.metadataLookup[ast])
        },
        "searched_case" to { cEnv, ast ->
            if (ast.size < 2) {
                err("Arity incorrect for searched case: $ast", cEnv.metadataLookup[ast]?.toErrorContext(), internal = false)
            }

            // go through the cases and else and evaluate them
            val matchExprs = ast.drop(1).map {
                when (it[0].text(cEnv)) {
                    "when" -> when (it.size) {
                        3 -> {
                            val condExpr = it[1].compile(cEnv)
                            val resultExpr = it[2].compile(cEnv)

                            ConditionThunks(
                                { env -> condExpr.eval(env).booleanValue() },
                                resultExpr
                            )
                        }
                        else -> err("Arity incorrect for 'when': $it", cEnv.metadataLookup[it]?.toErrorContext(), internal = false)
                    }
                    "else" -> when (it.size) {
                        2 -> {
                            val elseExpr = it[1].compile(cEnv)
                            ConditionThunks({ true }, elseExpr)
                        }
                        else -> err("Arity incorrect for 'else': $it", cEnv.metadataLookup[it]?.toErrorContext(), internal = false)
                    }
                    else -> err("Unexpected syntax in search case: ${it[0]}", cEnv.metadataLookup[it[0]]?.toErrorContext(), internal = false)
                }
            }

            matchExprs.compile(cEnv.metadataLookup[ast])
        },
        "path" to { cEnv, ast ->
            if (ast.size < 3) {
                err("Path arity too low: $ast", cEnv.metadataLookup[ast]?.toErrorContext(), internal = false)
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
                val (indexExpr, bindingCase) = compileIndexExpr(raw, cEnv)

                currExpr = exprThunk(cEnv.metadataLookup[raw]) { env ->
                    val target = targetExpr.eval(env)
                    val index = indexExpr.eval(env)
                    target.get(index, bindingCase, cEnv.metadataLookup[raw])
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
                            val (indexExpr, bindingCase) = compileIndexExpr(raw, cEnv);
                            { env, exprVal -> sequenceOf(exprVal.get(indexExpr.eval(env), bindingCase, cEnv.metadataLookup[raw])) }
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
                        currExpr = exprThunk(cEnv.metadataLookup[ast]) { env -> targetExpr.eval(env).unpivot(ion) }
                    }

                    exprThunk(cEnv.metadataLookup[ast]) { env ->
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
                err("UNPIVOT form must have one expression", cEnv.metadataLookup[ast]?.toErrorContext(), internal = false)
            }
            val expr = ast[1].compile(cEnv)
            exprThunk(cEnv.metadataLookup[ast]) { env -> expr.eval(env).unpivot(ion) }
        },
        "select" to { cEnv, ast ->
            if (ast.size < 3) {
                err("Bad arity on SELECT form $ast: ${ast.size}", cEnv.metadataLookup[ast]?.toErrorContext(), internal = false)
            }

            val projectForm = ast[1]
            if (projectForm !is IonSequence || projectForm.isEmpty) {
                err("SELECT projection node must be non-empty sequence: $projectForm", cEnv.metadataLookup[ast]?.toErrorContext(), internal = false)
            }
            if (projectForm[0].text(cEnv) != "project") {
                err("SELECT projection is not supported ${projectForm[0].text(cEnv)}", cEnv.metadataLookup[ast]?.toErrorContext(), internal = false)
            }
            val selectForm = projectForm[1]
            if (selectForm !is IonSequence || selectForm.isEmpty) {
                err("SELECT projection must be non-empty sequence: $selectForm", cEnv.metadataLookup[ast]?.toErrorContext(), internal = false)
            }

            // the captured list of legacy SQL style aggregates
            val aggregates: MutableList<Aggregate> = mutableListOf()

            val selectFunc: (List<ExprValue>, Environment) -> ExprValue = when (selectForm[0].text(cEnv)) {
                "*" -> {
                    if (selectForm.size != 1) {
                        err("SELECT * must be a singleton list", cEnv.metadataLookup[ast]?.toErrorContext(), internal = false)
                    }
                    // FIXME select * doesn't project ordered tuples
                    // TODO this should work for very specific cases...
                    { joinedValues, _ -> projectAllInto(joinedValues) }
                }
                "list" -> {
                    if (selectForm.size < 2) {
                        err("SELECT ... must have at least one expression", cEnv.metadataLookup[ast]?.toErrorContext(), internal = false)
                    }
                    val selectNames =
                        extractAliases(selectForm.asSequence().drop(1), cEnv).map { it.asName.exprValue() }

                    // nested aggregates within aggregates are not allowed
                    val cEnvInner = cEnv.copy(
                        table = cEnv.table.blacklist("call_agg", "call_agg_wildcard")
                    )
                    val aggHandler = { _: CompilationEnvironment, aggAst: IonSexp ->
                        val aggregate = aggAst.compileAggregateWithinSelectList(cEnvInner)
                        aggregates.add(aggregate)
                        exprThunk(cEnv.metadataLookup[ast]) { env ->
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
                        err("SELECT VALUE must have a single expression", cEnv.metadataLookup[ast]?.toErrorContext(), internal = false)
                    }
                    val expr = selectForm[1].compile(cEnv);

                    { _, env -> expr.eval(env) }
                }
                else -> err("Invalid node in SELECT: $selectForm", cEnv.metadataLookup[ast]?.toErrorContext(), internal = false)
            }

            val sourceThunk = compileQueryWithoutProjection(cEnv, ast)
            when {
                // generate a singleton with the aggregates
                aggregates.isNotEmpty() -> exprThunk(cEnv.metadataLookup[ast]) { env ->
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
                else -> exprThunk(cEnv.metadataLookup[ast]) { env ->
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
                err("Bad arity on PIVOT form $ast: ${ast.size}", cEnv.metadataLookup[ast]?.toErrorContext(), internal = false)
            }

            val memberForm = ast[1]
            if (memberForm !is IonSequence
                || memberForm.size != 3
                || memberForm[0].text(cEnv) != "member") {
                err("PIVOT member node must be of the form (member <name> <value>): $memberForm", cEnv.metadataLookup[memberForm]?.toErrorContext(), internal = false)
            }
            val nameExpr = memberForm[1].compile(cEnv)
            val valueExpr = memberForm[2].compile(cEnv)
            val sourceThunk = compileQueryWithoutProjection(cEnv, ast)

            exprThunk(cEnv.metadataLookup[ast]) { env ->
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


    private fun compileIndexExpr(ast: IonValue, cEnv: CompilationEnvironment) : Pair<ExprThunk, BindingCase> {
        var useAst = ast
        //If modified by a (case_sensitive...) or (case_insensitive...) node, store the case sensitivity and
        //extract the child node, otherwise, the default is BindingCase.SENSITIVE.
        val bindingCase = when {
            ast[0].stringValue()!!.startsWith("case_") -> {
                useAst = ast[1]
                BindingCase.fromIonValue(ast[0])
            }
            else -> BindingCase.SENSITIVE
        }
        return Pair(
            when(ast[0].stringValue()) {
                "lit" -> exprThunk(cEnv.metadataLookup[useAst]) { _ -> useAst[1].exprValue() }
                else -> useAst.compile(cEnv)
            },
            bindingCase)
    }

    /**
     * Helper used to evaluate a list of arguments to a boolean expression. The evaluation short circuits
     * when true (for or) false (for and) appears as an operand.
     *
     * Evaluate each [term] under [env] and apply [update] function on each iteration to update as to whether
     * we have seen a value that should shortcircuit.
     */
    private fun shortCircuitingFold(terms: List<ExprThunk>, env: Environment, update: (ExprValue) -> Boolean): Pair<Boolean, Boolean> {
        var seenShortcircuitValue = false
        var hasUnknown = false
        for (operandExpression in terms) {
            if (seenShortcircuitValue) break
            val operand: ExprValue = operandExpression.eval(env)
            seenShortcircuitValue = seenShortcircuitValue || update(operand)
            hasUnknown = hasUnknown || operand.isUnknown()
        }
        return Pair(seenShortcircuitValue, hasUnknown)
    }

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
        checkArity(ast, 2, 3, cEnv.metadataLookup[ast])
        checkArgsAreIonSexps(ast)

        // Note that the return value is a nullable and deferred.
        // This is so that null short-circuits can be supported.
        // The effective type is Either<Null, Either<Error, IDFA>>
        fun getDfa(pattern: ExprValue, escape: ExprValue?) : (() -> IDFAState)? {
            val dfaArgs = listOf(pattern, escape).filterNotNull()
            when {
                dfaArgs.any { it.type.isNull } -> return null
                dfaArgs.any { ! it.type.isText } -> return { err("LIKE expression must be given non-null strings as input",
                        ErrorCode.EVALUATOR_LIKE_INVALID_INPUTS,
                        cEnv.metadataLookup[ast]?.toErrorContext(PropertyValueMap().also {
                            it[Property.LIKE_PATTERN] = pattern.ionValue.toString()
                            if (escape != null) it[Property.LIKE_ESCAPE] = escape.ionValue.toString()
                        }),
                        internal = false)}
                else -> {
                    val (patternString: String, escapeChar: Int?, patternSize) = checkPattern(pattern.ionValue, escape?.ionValue, cEnv)
                    val dfa = buildDfaFromPattern(patternString, escapeChar, patternSize)
                    return { dfa }
                }
            }
        }

        /** See getDfa for more info on the DFA's odd type. */
        fun runDfa(value : ExprValue, dfa : (() -> IDFAState)?) : ExprValue {
            return when {
                dfa == null || value.type.isNull -> nullValue
                ! value.type.isText -> err("LIKE expression must be given non-null strings as input",
                        ErrorCode.EVALUATOR_LIKE_INVALID_INPUTS,
                        cEnv.metadataLookup[ast]?.toErrorContext(PropertyValueMap().also {
                            it[Property.LIKE_VALUE] = value.ionValue.toString()
                        }),
                        internal = false)
                else -> dfa().run(value.stringValue()).exprValue()
            }
        }

        // pattern and/or escape are not literals, delay compilation of regular expression to DFA till evaluation
        val cArgument = ast[1].compile(cEnv)
        val cPattern = ast[2].compile(cEnv)
        val cEscape = ast.getOrNull(3)?.compile(cEnv)


        if (cPattern is ConstantExprThunk && cEscape is ConstantExprThunk?) {
            val dfa = getDfa(cPattern.value, cEscape?.value)

            if (cArgument is ConstantExprThunk) {
                return ConstantExprThunk(runDfa(cArgument.value, dfa))
            } else {
                return exprThunk(cEnv.metadataLookup[ast]) { env -> runDfa(cArgument.eval(env), dfa) }
            }
        } else {
            return exprThunk(cEnv.metadataLookup[ast]) { env ->
                runDfa(cArgument.eval(env), getDfa(cPattern.eval(env), cEscape?.eval(env)))
            }
        }
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
    private fun checkPattern(pattern: IonValue, escape: IonValue?, cEnv: CompilationEnvironment): Triple<String, Int?, Int> {
        val patternString = pattern.stringValue()?.let { it } ?: err("Must provide a non-null value for PATTERN in a LIKE predicate: $pattern",
                                                                     cEnv.metadataLookup[pattern]?.toErrorContext(),
                                                                     internal = false)
        escape?.let {
            val escapeChar = checkEscapeChar(escape, cEnv).codePointAt(0)  // escape is a string of length 1
            val validEscapedChars = setOf('_'.toInt(), '%'.toInt(), escapeChar)
            val iter = patternString.codePointSequence().iterator()
            var count = 0

            while (iter.hasNext()) {
                val current = iter.next()
                if (current == escapeChar) {
                    if (!iter.hasNext()) err("Invalid escape sequence : $patternString", cEnv.metadataLookup[pattern]?.toErrorContext(), internal = false)
                    else {
                        if (!validEscapedChars.contains(iter.next())) err("Invalid escape sequence : $patternString", cEnv.metadataLookup[pattern]?.toErrorContext(), internal = false)
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
    private fun checkEscapeChar(escape: IonValue, cEnv: CompilationEnvironment): String {
        val escapeChar = escape.stringValue()?.let { it } ?: err("Must provide a value when using ESCAPE in a LIKE predicate: $escape",
                                                                 cEnv.metadataLookup[escape]?.toErrorContext(),
                                                                 internal = false)
        when (escapeChar) {
            "" -> err("Cannot use empty character as ESCAPE character in a LIKE predicate: $escape", cEnv.metadataLookup[escape]?.toErrorContext(), internal = false)
            else -> if (escapeChar.trim().length != 1) err("Escape character must have size 1 : $escapeChar", cEnv.metadataLookup[escape]?.toErrorContext(), internal = false)
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
            FromSource(extractAlias(id, this, cEnv), this.compile(cEnv), joinExpansion, filter)

        fun joinSources(joinExpansion: JoinExpansion): List<FromSource> {
            val leftSources = sourceAst[1].compileFromClauseSources(cEnv)
            val rightAst = sourceAst[2] as IonSexp
            val filter = when {
                sourceAst.size > 3 -> sourceAst[3].compile(cEnv)
                else -> null
            }
            return leftSources + rightAst.toFromSource(leftSources.size, joinExpansion, filter)
        }

        return when (sourceAst[0].text(cEnv)) {
            "inner_join" -> joinSources(JoinExpansion.INNER)
            "left_join" -> joinSources(JoinExpansion.OUTER)
            // TODO support this--will require reworking the left fold product approach
            "right_join", "outer_join" -> err("RIGHT and FULL JOIN not supported", cEnv.metadataLookup[sourceAst[0]]?.toErrorContext(), internal = false)
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
        val localsBinder = fromSources.map { it.alias }.localsBinder(missingValue)

        var whereExpr: ExprThunk? = null
        var limitExpr: ExprThunk? = null
        for (clause in ast.drop(3)) {
            when (clause[0].text(cEnv)) {
                "where" -> whereExpr = clause[1].compile(cEnv)
                "limit" -> limitExpr = clause[1].compile(cEnv)
                else -> err("Unknown clause in SELECT: $clause", cEnv.metadataLookup[clause]?.toErrorContext(), internal = false)
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
                                Bindings.over { bindingName ->
                                    when {
                                        bindingName.isEquivalentTo(alias.asName) ->
                                            value
                                        bindingName.isEquivalentTo(alias.atName) ->
                                            value.name ?: missingValue
                                        else ->
                                            null
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
                    FromProductionThunks(joinedValues, fromEnv.nest(localsBinder.bindLocals(joinedValues)))
                }

            if (whereExpr != null) {
                seq = seq.filter { (_, env) ->
                    val whereClauseResult: ExprValue = whereExpr!!.eval(env)
                    when (whereClauseResult.isUnknown()) {
                        true  -> false // Unknowns behave as `false` in WHERE context (see Issue IONSQL-162)
                        false  -> whereClauseResult.booleanValue()
                    }
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
    private val builtinFunctions: Map<String, ExprFunction> = BuiltinFunctionFactory(ion).createFunctionMap()

    private val functions = builtinFunctions + userFunctions

    val functionSignatures: List<FunctionSignature> by lazy {
        functions.map { (name, _) -> UntypedFunctionSignature(name) }
    }

    private inner class Accumulator(var current: Number? = 0L,
                                         protected val nextFunc: (Number?, ExprValue) -> Number) : ExprAggregator {

        override fun next(value: ExprValue) {

            current = when (value.isUnknown()) {
                false -> nextFunc(current, value)
                true  -> current
            }
        }

        override fun compute() = current?.let { it.exprValue() } ?: nullValue
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
    private val builtinAggregates: Map<String, ExprAggregatorFactory> = mapOf("count" to ExprAggregatorFactory.over {
        Accumulator(0L, { curr, _ -> curr!! + 1L })
    }, "sum" to ExprAggregatorFactory.over {
        Accumulator(null) { curr, next -> curr?.let { it + next.numberValue() } ?: next.numberValue() }
    }, "min" to ExprAggregatorFactory.over {
        Accumulator(null, comparisonAccumulator { left, right -> left < right })
    }, "max" to ExprAggregatorFactory.over {
        Accumulator(null, comparisonAccumulator { left, right -> left > right })
    }, "avg" to ExprAggregatorFactory.over {
        object : ExprAggregator {
            var sum: Number? = null
            var count = 0L
            override fun next(value: ExprValue) {
                when (value.type) {
                    NULL, MISSING -> {
                    }
                    else          -> {
                        sum = sum?.let { it + value.numberValue() } ?: value.numberValue()
                        count++
                    }
                }
            }
            override fun compute() = sum?.let { (it / bigDecimalOf(count)).exprValue() } ?: nullValue
        }
    })

    private fun Boolean.exprValue(): ExprValue = booleanExprValue(this, ion)

    private fun Number.exprValue(): ExprValue = when (this) {
        is Int -> integerExprValue(this, ion)
        is Long -> integerExprValue(this, ion)
        is BigInteger -> integerExprValue(this, ion)
        is Double -> floatExprValue(this, ion)
        is BigDecimal -> decimalExprValue(this, ion)
        else -> errNoContext("Cannot convert number to expression value: $this", internal = true)
    }

    private fun String.exprValue(): ExprValue = stringExprValue(this, ion)

    private fun ExprValue.get(member: ExprValue, case: BindingCase, metadata: NodeMetadata?): ExprValue = when {
        member.type == INT -> {
            val index = member.numberValue().toInt()
            ordinalBindings[index] ?: missingValue
        }
        member.type.isText -> {
            val name = member.stringValue()
            // delegate to bindings logic as the scope of lookup by name
            bindings[BindingName(name, case)] ?: missingValue
        }
        else -> err("Cannot convert index to int/string: $member", metadata?.toErrorContext(), internal = true)
    }

    private fun IonValue.text(cEnv: CompilationEnvironment) = stringValue() ?: err("Expected non-null string: $this", cEnv.metadataLookup[this]?.toErrorContext(), internal = false)

    private fun IonSexp.compileCall(cEnv: CompilationEnvironment, startIndex: Int): ExprThunk {
        val funcValue = this[startIndex]
        val name = funcValue.text(cEnv)
        val func = functions[name] ?: err("No such function: $name", cEnv.metadataLookup[funcValue]?.toErrorContext(), internal = false)
        val argIndex = startIndex + 1
        return compileFunc(cEnv, argIndex, func)
    }

    /**
     * Compiles an aggregate function call within a `SELECT <list>` context.
     * Note that the given syntax table should be one suitable for the expression
     * **within** the function call.
     */
    private fun IonSexp.compileAggregateWithinSelectList(cEnv: CompilationEnvironment): Aggregate {
        val aggFactory = loadAggregateFactory(cEnv)
        val inputExpr = when (this[0].text(cEnv)) {
            "call_agg"          -> this[3].compile(cEnv)
        // for wild cards we don't need to materialize anything (i.e. count all the things)
        // Do *not* use missing on null values as they will break the behaviour of count(*)
            "call_agg_wildcard" -> exprThunk(cEnv.metadataLookup[this]) { _ -> intOneValue }
            else                -> err("Invalid aggregate node: $this",
                                       cEnv.metadataLookup[this[0]]?.toErrorContext(),
                                       internal = false)
        }

        return Aggregate(aggFactory, inputExpr, cEnv.regCounter.getAndIncrement())
    }

    private fun IonSexp.compileAggregateCall(cEnv: CompilationEnvironment): ExprThunk {
        val aggFactory = loadAggregateFactory(cEnv)
        val argThunk = this[3].compile(cEnv)
        return exprThunk(cEnv.metadataLookup[this]) { env ->
            val agg = aggFactory.create()
            val arg = argThunk.eval(env)
            arg.forEach { agg.next(it) }
            agg.compute()
        }
    }

    private fun IonSexp.loadAggregateFactory(cEnv: CompilationEnvironment): ExprAggregatorFactory {
        val expectedSize = when(this[0].text(cEnv)) {
            "call_agg" -> {
                val qualifier = this[2].text(cEnv)
                if (qualifier != "all") {
                    // TODO support DISTINCT aggregates
                    err("DISTINCT aggregate function call not supported", cEnv.metadataLookup[this[2]]?.toErrorContext(), internal = false)
                }
                4
            }
            "call_agg_wildcard" -> 2
            else -> err("Invalid aggregate node: $this", cEnv.metadataLookup[this]?.toErrorContext(), internal = false)
        }
        val name = this[1].text(cEnv)

        if (size != expectedSize) {
            err("Aggregate function call node must have arity of $expectedSize", cEnv.metadataLookup[this[1]]?.toErrorContext(), internal = false)
        }

        val aggFactory = builtinAggregates[name] ?: err("No such aggregate function: $name", cEnv.metadataLookup[this[1]]?.toErrorContext(), internal = false)

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
                                    func: ExprFunction,
                                    policy: UnknownsPropagationPolicy = UnknownsPropagationPolicy.NONE): ExprThunk {

        val argThunks = compileArgs(cEnv, argStartIndex)

        when (policy) {
            UnknownsPropagationPolicy.NONE -> return exprThunk(cEnv.metadataLookup[this]) { env ->
                val args = argThunks.map { it.eval(env) }
                func.call(env, args)
                }
            UnknownsPropagationPolicy.PROPAGATE -> return exprThunk(cEnv.metadataLookup[this]) { env ->
                val args = argThunks.map { it.eval(env) }
                if (args.any { it.isUnknown() }) nullValue
                else func.call(env, args)
            }
        }
    }

    private fun IonValue.compile(cEnv: CompilationEnvironment): ExprThunk = when(this) {
        is IonSexp -> {
            // FIXME
            // Errors bellow should all be `internal = false` as the AST is an input for the Evaluator. However for our
            // current use cases it makes more sense to mark some as `internal = true`. This needs to be handled better
            // by propagating the proper error codes where the client will have more information to decide what should
            // it consider internal and external

            val name = this[0].stringValue() ?:
                       err("AST node does not start with non-null string: $this", cEnv.metadataLookup[this[0]]?.toErrorContext(), internal = true)
            val handler = cEnv.table[name] ?:
                          err("No such syntax handler for $name", cEnv.metadataLookup[this[0]]?.toErrorContext(), internal = false)
            handler(cEnv, this)
        }
        else       -> err("AST node is not s-expression: $this",
                          cEnv.metadataLookup[this]?.toErrorContext(),
                          internal = true)
    }

    /**
     * Captures the propagation policy for IonSQL expressions when passed an unknown value
     *
     * - `NONE` means no policy, unknowns remain untouched, e.g., constructors for lists
     * - `PROPAGATE` for operators and expressions follow the spec on what the evaluation should propagate
     */
    public enum class UnknownsPropagationPolicy {
        NONE, PROPAGATE
    }

    private fun bindOp(minArity: Int = 2,
                       maxArity: Int = 2,
                       policy: UnknownsPropagationPolicy = PROPAGATE,
                       op: (Environment, List<ExprValue>) -> ExprValue): (CompilationEnvironment, IonSexp) -> ExprThunk {
        return { cEnv, ast ->
            checkArity(ast, minArity, maxArity, cEnv.metadataLookup[ast])
            ast.compileFunc(cEnv, 1, ExprFunction.over(op), policy)
        }
    }

    private fun checkArity(ast: IonSexp, minArity: Int, maxArity: Int, metadata: NodeMetadata?) {
        val arity = ast.size - 1
        when {
            arity < minArity -> err("Not enough arguments: $ast", metadata?.toErrorContext(), internal = false)
            arity > maxArity -> err("Too many arguments: $ast", metadata?.toErrorContext(), internal = false)
        }
    }

    // TODO support meta-nodes properly for error reporting

    /** Evaluates an unbound syntax tree against a global set of bindings. */
    fun eval(ast: IonSexp, session: EvaluationSession): ExprValue = compile(ast).eval(session)

    /** Compiles a syntax tree to an [Expression]. */
    fun compile(ast: IonSexp): Expression {

        val (astWithoutMetaNodes, metadataLookup) = NodeMetadataLookup.extractMetaNode(ast)
        val cEnv = CompilationEnvironment(syntaxTable, AtomicInteger(0), metadataLookup)
        val expr = astWithoutMetaNodes.seal().compile(cEnv)

        val registerCount = cEnv.regCounter.get()

        return object : Expression {
            override fun eval(session: EvaluationSession): ExprValue {

                val env = Environment(
                    session = session,
                    locals = session.globals,
                    current = session.globals,
                    registers = RegisterBank(registerCount)
                )

                return expr.eval(env)
            }
        }
    }

    /**
     * Compiles the given source expression into a bound [Expression].
     */
    override fun compile(source: String): Expression {
        val ast = parser.parse(source)

        return object : Expression {
            override fun eval(session: EvaluationSession): ExprValue = eval(ast, session)
        }
    }
}
