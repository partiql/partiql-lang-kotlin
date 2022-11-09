/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

@file:Suppress("DEPRECATION") // We don't need warnings about ExprNode deprecation.

package org.partiql.lang.eval

import com.amazon.ion.IntegerSize
import com.amazon.ion.IonInt
import com.amazon.ion.IonString
import com.amazon.ion.IonValue
import com.amazon.ion.Timestamp
import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.toIonValue
import org.partiql.lang.ast.AggregateCallSiteListMeta
import org.partiql.lang.ast.AggregateRegisterIdMeta
import org.partiql.lang.ast.IonElementMetaContainer
import org.partiql.lang.ast.IsCountStarMeta
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.UniqueNameMeta
import org.partiql.lang.ast.find
import org.partiql.lang.ast.sourceLocation
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.domains.staticType
import org.partiql.lang.domains.toBindingCase
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.errors.UNBOUND_QUOTED_IDENTIFIER_HINT
import org.partiql.lang.eval.binding.Alias
import org.partiql.lang.eval.binding.localsBinder
import org.partiql.lang.eval.builtins.storedprocedure.StoredProcedure
import org.partiql.lang.eval.like.parsePattern
import org.partiql.lang.eval.time.Time
import org.partiql.lang.eval.visitors.PartiqlAstSanityValidator
import org.partiql.lang.syntax.PartiQLParserBuilder
import org.partiql.lang.types.AnyOfType
import org.partiql.lang.types.AnyType
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.IntType
import org.partiql.lang.types.SingleType
import org.partiql.lang.types.StaticType
import org.partiql.lang.types.TypedOpParameter
import org.partiql.lang.types.UnknownArguments
import org.partiql.lang.types.UnsupportedTypeCheckException
import org.partiql.lang.types.toTypedOpParameter
import org.partiql.lang.util.bigDecimalOf
import org.partiql.lang.util.checkThreadInterrupted
import org.partiql.lang.util.codePointSequence
import org.partiql.lang.util.div
import org.partiql.lang.util.drop
import org.partiql.lang.util.foldLeftProduct
import org.partiql.lang.util.interruptibleFold
import org.partiql.lang.util.isZero
import org.partiql.lang.util.minus
import org.partiql.lang.util.plus
import org.partiql.lang.util.rem
import org.partiql.lang.util.stringValue
import org.partiql.lang.util.take
import org.partiql.lang.util.times
import org.partiql.lang.util.timestampValue
import org.partiql.lang.util.totalMinutes
import org.partiql.lang.util.unaryMinus
import org.partiql.pig.runtime.SymbolPrimitive
import java.math.BigDecimal
import java.util.LinkedList
import java.util.Stack
import java.util.TreeSet
import java.util.regex.Pattern

/**
 * A thunk with no parameters other than the current environment.
 *
 * See https://en.wikipedia.org/wiki/Thunk
 *
 * This name was chosen because it is a thunk that accepts an instance of `Environment`.
 */
private typealias ThunkEnv = Thunk<Environment>

/**
 * A thunk taking a single [T] argument and the current environment.
 *
 * See https://en.wikipedia.org/wiki/Thunk
 *
 * This name was chosen because it is a thunk that accepts an instance of `Environment` and an [ExprValue] as
 * its arguments.
 */
private typealias ThunkEnvValue<T> = ThunkValue<Environment, T>

/**
 * A basic compiler that converts an instance of [PartiqlAst] to an [Expression].
 *
 * This implementation produces a "compiled" form consisting of context-threaded
 * code in the form of a tree of [ThunkEnv]s.  An overview of this technique can be found
 * [here][1].
 *
 * **Note:** *threaded* in this context is used in how the code gets *threaded* together for
 * interpretation and **not** the concurrency primitive. That is to say this code is NOT thread
 * safe.
 *
 * [1]: https://www.complang.tuwien.ac.at/anton/lvas/sem06w/fest.pdf
 *
 * Note that this is not implemented in the pattern of a typical visitor pattern.  The visitor pattern isn't a good
 * match for all scenarios.  It's great for simple needs such as the types of checks performed or simple transformations
 * such as partial evaluation and transforming variable references to De Bruijn indices, however a compiler needs
 * much finer grain of control over exactly how and when each node is walked, visited, and transformed.
 *
 * @param functions A map of functions keyed by function name that will be available during compilation.
 * @param compileOptions Various options that effect how the source code is compiled.
 */
internal class EvaluatingCompiler(
    private val valueFactory: ExprValueFactory,
    private val functions: Map<String, ExprFunction>,
    private val customTypedOpParameters: Map<String, TypedOpParameter>,
    private val procedures: Map<String, StoredProcedure>,
    private val compileOptions: CompileOptions = CompileOptions.standard()
) {
    private val errorSignaler = compileOptions.typingMode.createErrorSignaler(valueFactory)
    private val thunkFactory = compileOptions.typingMode.createThunkFactory<Environment>(compileOptions.thunkOptions, valueFactory)

    private val compilationContextStack = Stack<CompilationContext>()

    private val currentCompilationContext: CompilationContext
        get() = compilationContextStack.peek() ?: errNoContext(
            "compilationContextStack was empty.", ErrorCode.EVALUATOR_UNEXPECTED_VALUE, internal = true
        )

    // Note: please don't make this inline -- it messes up [EvaluationException] stack traces and
    // isn't a huge benefit because this is only used at SQL-compile time anyway.
    private fun <R> nestCompilationContext(
        expressionContext: ExpressionContext,
        fromSourceNames: Set<String>,
        block: () -> R
    ): R {
        compilationContextStack.push(
            when {
                compilationContextStack.empty() -> CompilationContext(expressionContext, fromSourceNames)
                else -> compilationContextStack.peek().createNested(
                    expressionContext,
                    fromSourceNames
                )
            }
        )

        try {
            return block()
        } finally {
            compilationContextStack.pop()
        }
    }

    private fun Number.exprValue(): ExprValue = when (this) {
        is Int -> valueFactory.newInt(this)
        is Long -> valueFactory.newInt(this)
        is Double -> valueFactory.newFloat(this)
        is BigDecimal -> valueFactory.newDecimal(this)
        else -> errNoContext(
            "Cannot convert number to expression value: $this",
            errorCode = ErrorCode.EVALUATOR_INVALID_CONVERSION,
            internal = true
        )
    }

    private fun Boolean.exprValue(): ExprValue = valueFactory.newBoolean(this)
    private fun String.exprValue(): ExprValue = valueFactory.newString(this)

    /** Represents an instance of a compiled `GROUP BY` expression and alias. */
    private class CompiledGroupByItem(val alias: ExprValue, val uniqueId: String?, val thunk: ThunkEnv)

    /**
     * Represents a memoized binding [BindingName] and an [ExprValue] of the same name.
     * Used during evaluation og `GROUP BY`.
     */
    private data class FromSourceBindingNamePair(val bindingName: BindingName, val nameExprValue: ExprValue)

    /** Represents an instance of a compiled `ORDER BY` expression, orderingSpec and nulls type. */
    private class CompiledOrderByItem(val comparator: NaturalExprValueComparators, val thunk: ThunkEnv)

    /**
     * Base class for [ExprAggregator] instances which accumulate values and perform a final computation.
     */
    private inner class Accumulator(
        var current: ExprValue?,
        val nextFunc: (ExprValue?, ExprValue) -> ExprValue,
        val valueFilter: (ExprValue) -> Boolean = { _ -> true }
    ) : ExprAggregator {

        override fun next(value: ExprValue) {
            // skip the accumulation function if the value is unknown or if the value is filtered out
            if (value.isNotUnknown() && valueFilter.invoke(value)) {
                current = nextFunc(current, value)
            }
        }

        override fun compute() = current ?: valueFactory.nullValue
    }

    private fun comparisonAccumulator(comparator: NaturalExprValueComparators): (ExprValue?, ExprValue) -> ExprValue =
        { left, right ->
            when {
                left == null || comparator.compare(left, right) > 0 -> right
                else -> left
            }
        }

    /** Dispatch table for built-in aggregate functions. */
    private val builtinAggregates: Map<Pair<String, PartiqlAst.SetQuantifier>, ExprAggregatorFactory> =
        run {
            fun checkIsNumberType(funcName: String, value: ExprValue) {
                if (!value.type.isNumber) {
                    errNoContext(
                        message = "Aggregate function $funcName expects arguments of NUMBER type but the following value was provided: ${value.ionValue}, with type of ${value.type}",
                        errorCode = ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_AGG_FUNCTION,
                        internal = false
                    )
                }
            }

            val countAccFunc: (ExprValue?, ExprValue) -> ExprValue = { accumulated, _ -> (accumulated!!.longValue() + 1L).exprValue() }
            val sumAccFunc: (ExprValue?, ExprValue) -> ExprValue = { accumulated, nextItem ->
                checkIsNumberType("SUM", nextItem)
                accumulated?.let { (it.numberValue() + nextItem.numberValue()).exprValue() } ?: nextItem
            }
            val minAccFunc = comparisonAccumulator(NaturalExprValueComparators.NULLS_LAST_ASC)
            val maxAccFunc = comparisonAccumulator(NaturalExprValueComparators.NULLS_LAST_DESC)
            val avgAggregateGenerator = { filter: (ExprValue) -> Boolean ->
                object : ExprAggregator {
                    var sum: Number? = null
                    var count = 0L

                    override fun next(value: ExprValue) {
                        if (value.isNotUnknown() && filter.invoke(value)) {
                            checkIsNumberType("AVG", value)
                            sum = sum?.let { it + value.numberValue() } ?: value.numberValue()
                            count++
                        }
                    }

                    override fun compute() =
                        sum?.let { (it / bigDecimalOf(count)).exprValue() }
                            ?: this@EvaluatingCompiler.valueFactory.nullValue
                }
            }
            val allFilter: (ExprValue) -> Boolean = { _ -> true }
            // each distinct ExprAggregator must get its own createUniqueExprValueFilter()
            mapOf(
                Pair("count", PartiqlAst.SetQuantifier.All()) to ExprAggregatorFactory.over {
                    Accumulator((0L).exprValue(), countAccFunc, allFilter)
                },

                Pair("count", PartiqlAst.SetQuantifier.Distinct()) to ExprAggregatorFactory.over {
                    Accumulator((0L).exprValue(), countAccFunc, createUniqueExprValueFilter())
                },

                Pair("sum", PartiqlAst.SetQuantifier.All()) to ExprAggregatorFactory.over {
                    Accumulator(null, sumAccFunc, allFilter)
                },

                Pair("sum", PartiqlAst.SetQuantifier.Distinct()) to ExprAggregatorFactory.over {
                    Accumulator(null, sumAccFunc, createUniqueExprValueFilter())
                },

                Pair("avg", PartiqlAst.SetQuantifier.All()) to ExprAggregatorFactory.over {
                    avgAggregateGenerator(allFilter)
                },

                Pair("avg", PartiqlAst.SetQuantifier.Distinct()) to ExprAggregatorFactory.over {
                    avgAggregateGenerator(createUniqueExprValueFilter())
                },

                Pair("max", PartiqlAst.SetQuantifier.All()) to ExprAggregatorFactory.over {
                    Accumulator(null, maxAccFunc, allFilter)
                },

                Pair("max", PartiqlAst.SetQuantifier.Distinct()) to ExprAggregatorFactory.over {
                    Accumulator(null, maxAccFunc, createUniqueExprValueFilter())
                },

                Pair("min", PartiqlAst.SetQuantifier.All()) to ExprAggregatorFactory.over {
                    Accumulator(null, minAccFunc, allFilter)
                },

                Pair("min", PartiqlAst.SetQuantifier.Distinct()) to ExprAggregatorFactory.over {
                    Accumulator(null, minAccFunc, createUniqueExprValueFilter())
                }
            )
        }

    /**
     * Compiles a [PartiqlAst.Statement] tree to an [Expression].
     *
     * Checks [Thread.interrupted] before every expression and sub-expression is compiled
     * and throws [InterruptedException] if [Thread.interrupted] it has been set in the
     * hope that long-running compilations may be aborted by the caller.
     */
    fun compile(originalAst: PartiqlAst.Statement): Expression {
        val visitorTransform = compileOptions.visitorTransformMode.createVisitorTransform()
        val transformedAst = visitorTransform.transformStatement(originalAst)
        val partiqlAstSanityValidator = PartiqlAstSanityValidator()

        partiqlAstSanityValidator.validate(transformedAst, compileOptions)

        val thunk = nestCompilationContext(ExpressionContext.NORMAL, emptySet()) {
            compileAstStatement(transformedAst)
        }

        return object : Expression {
            override fun eval(session: EvaluationSession): ExprValue {

                val env = Environment(
                    session = session,
                    locals = session.globals,
                    current = session.globals
                )

                return thunk(env)
            }
        }
    }

    /**
     * Compiles the given source expression into a bound [Expression].
     */
    @Deprecated("Please use CompilerPipeline instead")
    fun compile(source: String): Expression {
        val parser = PartiQLParserBuilder().ionSystem(valueFactory.ion).build()
        val ast = parser.parseAstStatement(source)
        return compile(ast)
    }

    /**
     * Evaluates an instance of [PartiqlAst.Statement] against a global set of bindings.
     */
    fun eval(ast: PartiqlAst.Statement, session: EvaluationSession): ExprValue = compile(ast).eval(session)

    /**
     * Compiles the specified [PartiqlAst.Statement] into a [ThunkEnv].
     *
     * This function will [InterruptedException] if [Thread.interrupted] has been set.
     */
    private fun compileAstStatement(ast: PartiqlAst.Statement): ThunkEnv {
        checkThreadInterrupted()
        return when (ast) {
            is PartiqlAst.Statement.Query -> compileAstExpr(ast.expr)
            is PartiqlAst.Statement.Ddl -> compileDdl(ast)
            is PartiqlAst.Statement.Dml -> compileDml(ast)
            is PartiqlAst.Statement.Exec -> compileExec(ast)
        }
    }

    private fun compileAstExpr(expr: PartiqlAst.Expr): ThunkEnv {
        val metas = expr.metas

        return when (expr) {
            is PartiqlAst.Expr.Lit -> compileLit(expr, metas)
            is PartiqlAst.Expr.Missing -> compileMissing(metas)
            is PartiqlAst.Expr.Id -> compileId(expr, metas)
            is PartiqlAst.Expr.SimpleCase -> compileSimpleCase(expr, metas)
            is PartiqlAst.Expr.SearchedCase -> compileSearchedCase(expr, metas)
            is PartiqlAst.Expr.Path -> compilePath(expr, metas)
            is PartiqlAst.Expr.Struct -> compileStruct(expr, metas)
            is PartiqlAst.Expr.Select -> compileSelect(expr, metas)
            is PartiqlAst.Expr.CallAgg -> compileCallAgg(expr, metas)
            is PartiqlAst.Expr.Parameter -> compileParameter(expr, metas)
            is PartiqlAst.Expr.Date -> compileDate(expr, metas)
            is PartiqlAst.Expr.LitTime -> compileLitTime(expr, metas)

            // arithmetic operations
            is PartiqlAst.Expr.Plus -> compilePlus(expr, metas)
            is PartiqlAst.Expr.Times -> compileTimes(expr, metas)
            is PartiqlAst.Expr.Minus -> compileMinus(expr, metas)
            is PartiqlAst.Expr.Divide -> compileDivide(expr, metas)
            is PartiqlAst.Expr.Modulo -> compileModulo(expr, metas)

            // comparison operators
            is PartiqlAst.Expr.And -> compileAnd(expr, metas)
            is PartiqlAst.Expr.Between -> compileBetween(expr, metas)
            is PartiqlAst.Expr.Eq -> compileEq(expr, metas)
            is PartiqlAst.Expr.Gt -> compileGt(expr, metas)
            is PartiqlAst.Expr.Gte -> compileGte(expr, metas)
            is PartiqlAst.Expr.Lt -> compileLt(expr, metas)
            is PartiqlAst.Expr.Lte -> compileLte(expr, metas)
            is PartiqlAst.Expr.Like -> compileLike(expr, metas)
            is PartiqlAst.Expr.InCollection -> compileIn(expr, metas)

            // logical operators
            is PartiqlAst.Expr.Ne -> compileNe(expr, metas)
            is PartiqlAst.Expr.Or -> compileOr(expr, metas)

            // unary
            is PartiqlAst.Expr.Not -> compileNot(expr, metas)
            is PartiqlAst.Expr.Pos -> compilePos(expr, metas)
            is PartiqlAst.Expr.Neg -> compileNeg(expr, metas)

            // other operators
            is PartiqlAst.Expr.Concat -> compileConcat(expr, metas)
            is PartiqlAst.Expr.Call -> compileCall(expr, metas)
            is PartiqlAst.Expr.NullIf -> compileNullIf(expr, metas)
            is PartiqlAst.Expr.Coalesce -> compileCoalesce(expr, metas)

            // "typed" operators (RHS is a data type and not an expression)
            is PartiqlAst.Expr.Cast -> compileCast(expr, metas)
            is PartiqlAst.Expr.IsType -> compileIs(expr, metas)
            is PartiqlAst.Expr.CanCast -> compileCanCast(expr, metas)
            is PartiqlAst.Expr.CanLosslessCast -> compileCanLosslessCast(expr, metas)

            // sequence constructors
            is PartiqlAst.Expr.List -> compileSeq(ExprValueType.LIST, expr.values, metas)
            is PartiqlAst.Expr.Sexp -> compileSeq(ExprValueType.SEXP, expr.values, metas)
            is PartiqlAst.Expr.Bag -> compileSeq(ExprValueType.BAG, expr.values, metas)

            // bag operators
            is PartiqlAst.Expr.BagOp -> compileBagOp(expr, metas)

            is PartiqlAst.Expr.GraphMatch -> TODO("Compilation of GraphMatch expression")
        }
    }

    private fun compileAstExprs(args: List<PartiqlAst.Expr>) = args.map { compileAstExpr(it) }

    private fun compileNullIf(expr: PartiqlAst.Expr.NullIf, metas: MetaContainer): ThunkEnv {
        val expr1Thunk = compileAstExpr(expr.expr1)
        val expr2Thunk = compileAstExpr(expr.expr2)

        // Note: NULLIF does not propagate the unknown values and .exprEquals  provides the correct semantics.
        return thunkFactory.thunkEnv(metas) { env ->
            val expr1Value = expr1Thunk(env)
            val expr2Value = expr2Thunk(env)
            when {
                expr1Value.exprEquals(expr2Value) -> valueFactory.nullValue
                else -> expr1Value
            }
        }
    }

    private fun compileCoalesce(expr: PartiqlAst.Expr.Coalesce, metas: MetaContainer): ThunkEnv {
        val argThunks = compileAstExprs(expr.args)

        return thunkFactory.thunkEnv(metas) { env ->
            var nullFound = false
            var knownValue: ExprValue? = null
            for (thunk in argThunks) {
                val argValue = thunk(env)
                if (argValue.isNotUnknown()) {
                    knownValue = argValue
                    // No need to execute remaining thunks to save computation as first non-unknown value is found
                    break
                }
                if (argValue.type == ExprValueType.NULL) {
                    nullFound = true
                }
            }
            when (knownValue) {
                null -> when {
                    compileOptions.typingMode == TypingMode.PERMISSIVE && !nullFound -> valueFactory.missingValue
                    else -> valueFactory.nullValue
                }
                else -> knownValue
            }
        }
    }

    /**
     * Returns a function that accepts an [ExprValue] as an argument and returns true it is `NULL`, `MISSING`, or
     * within the range specified by [range].
     */
    private fun integerValueValidator(
        range: LongRange
    ): (ExprValue) -> Boolean = { value ->
        when (value.type) {
            ExprValueType.NULL, ExprValueType.MISSING -> true
            ExprValueType.INT -> {
                val longValue: Long = value.scalar.numberValue()?.toLong()
                    ?: error(
                        "ExprValue.numberValue() must not be `NULL` when its type is INT." +
                            "This indicates that the ExprValue instance has a bug."
                    )

                // PRO-TIP:  make sure to use the `Long` primitive type here with `.contains` otherwise
                // Kotlin will use the version of `.contains` that treats [range] as a collection, and it will
                // be very slow!
                range.contains(longValue)
            }
            else -> error(
                "IntegerValueValidator can only accept ExprValue with type INT, NULL, and MISSING."
            )
        }
    }

    /**
     *  For operators which could return integer type, check integer overflow in case of [TypingMode.PERMISSIVE].
     */
    private fun checkIntegerOverflow(computeThunk: ThunkEnv, metas: MetaContainer): ThunkEnv =
        when (val staticTypes = metas.staticType?.type?.getTypes()) {
            // No staticType, can't validate integer size.
            null -> computeThunk
            else -> {
                when (compileOptions.typingMode) {
                    TypingMode.LEGACY -> {
                        // integer size constraints have not been tested under [TypingMode.LEGACY] because the
                        // [StaticTypeInferenceVisitorTransform] doesn't support being used with legacy mode yet.
                        // throw an exception in case we encounter this untested scenario. This might work fine, but I
                        // wouldn't bet on it.
                        val hasConstrainedInteger = staticTypes.any {
                            it is IntType && it.rangeConstraint != IntType.IntRangeConstraint.UNCONSTRAINED
                        }
                        if (hasConstrainedInteger) {
                            TODO("Legacy mode doesn't support integer size constraints yet.")
                        } else {
                            computeThunk
                        }
                    }

                    TypingMode.PERMISSIVE -> {
                        val biggestIntegerType = staticTypes.filterIsInstance<IntType>().maxBy {
                            it.rangeConstraint.numBytes
                        }
                        when (biggestIntegerType) {
                            // static type contains one or more IntType
                            is IntType -> {
                                val validator = integerValueValidator(biggestIntegerType.rangeConstraint.validRange)
                                thunkFactory.thunkEnv(metas) { env ->
                                    val naryResult = computeThunk(env)
                                    // validation shall only happen when the result is INT/MISSING/NULL
                                    // this is important as StaticType may contain a mixture of multiple types
                                    when (val type = naryResult.type) {
                                        ExprValueType.INT, ExprValueType.MISSING, ExprValueType.NULL -> errorSignaler.errorIf(
                                            !validator(naryResult),
                                            ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
                                            { ErrorDetails(metas, "Integer overflow", errorContextFrom(metas)) },
                                            { naryResult }
                                        )

                                        else -> {
                                            if (staticTypes.all { it is IntType }) {
                                                error(
                                                    "The expression's static type was supposed to be INT but instead it was $type" +
                                                        "This may indicate the presence of a bug in the type inferencer."
                                                )
                                            } else {
                                                naryResult
                                            }
                                        }
                                    }
                                }
                            }
                            // If there is no IntType StaticType, can't validate the integer size either.
                            null -> computeThunk
                            else -> computeThunk
                        }
                    }
                }
            }
        }

    private fun compilePlus(expr: PartiqlAst.Expr.Plus, metas: MetaContainer): ThunkEnv {
        if (expr.operands.size < 2) {
            error("Internal Error: PartiqlAst.Expr.Plus must have at least 2 arguments")
        }

        val argThunks = compileAstExprs(expr.operands)

        val computeThunk = thunkFactory.thunkFold(metas, argThunks) { lValue, rValue ->
            (lValue.numberValue() + rValue.numberValue()).exprValue()
        }

        return checkIntegerOverflow(computeThunk, metas)
    }

    private fun compileMinus(expr: PartiqlAst.Expr.Minus, metas: MetaContainer): ThunkEnv {
        if (expr.operands.size < 2) {
            error("Internal Error: PartiqlAst.Expr.Minus must have at least 2 arguments")
        }

        val argThunks = compileAstExprs(expr.operands)

        val computeThunk = thunkFactory.thunkFold(metas, argThunks) { lValue, rValue ->
            (lValue.numberValue() - rValue.numberValue()).exprValue()
        }

        return checkIntegerOverflow(computeThunk, metas)
    }

    private fun compilePos(expr: PartiqlAst.Expr.Pos, metas: MetaContainer): ThunkEnv {
        val exprThunk = compileAstExpr(expr.expr)

        val computeThunk = thunkFactory.thunkEnvOperands(metas, exprThunk) { _, value ->
            // Invoking .numberValue() here makes this essentially just a type check
            value.numberValue()
            // Original value is returned unmodified.
            value
        }

        return checkIntegerOverflow(computeThunk, metas)
    }

    private fun compileNeg(expr: PartiqlAst.Expr.Neg, metas: MetaContainer): ThunkEnv {
        val exprThunk = compileAstExpr(expr.expr)

        val computeThunk = thunkFactory.thunkEnvOperands(metas, exprThunk) { _, value ->
            (-value.numberValue()).exprValue()
        }

        return checkIntegerOverflow(computeThunk, metas)
    }

    private fun compileTimes(expr: PartiqlAst.Expr.Times, metas: MetaContainer): ThunkEnv {
        val argThunks = compileAstExprs(expr.operands)

        val computeThunk = thunkFactory.thunkFold(metas, argThunks) { lValue, rValue ->
            (lValue.numberValue() * rValue.numberValue()).exprValue()
        }

        return checkIntegerOverflow(computeThunk, metas)
    }

    private fun compileDivide(expr: PartiqlAst.Expr.Divide, metas: MetaContainer): ThunkEnv {
        val argThunks = compileAstExprs(expr.operands)

        val computeThunk = thunkFactory.thunkFold(metas, argThunks) { lValue, rValue ->
            val denominator = rValue.numberValue()

            errorSignaler.errorIf(
                denominator.isZero(),
                ErrorCode.EVALUATOR_DIVIDE_BY_ZERO,
                { ErrorDetails(metas, "/ by zero") }
            ) {
                try {
                    (lValue.numberValue() / denominator).exprValue()
                } catch (e: ArithmeticException) {
                    // Setting the internal flag as true as it is not clear what
                    // ArithmeticException may be thrown by the above
                    throw EvaluationException(
                        cause = e,
                        errorCode = ErrorCode.EVALUATOR_ARITHMETIC_EXCEPTION,
                        errorContext = errorContextFrom(metas),
                        internal = true
                    )
                }
            }
        }

        return checkIntegerOverflow(computeThunk, metas)
    }

    private fun compileModulo(expr: PartiqlAst.Expr.Modulo, metas: MetaContainer): ThunkEnv {
        val argThunks = compileAstExprs(expr.operands)

        val computeThunk = thunkFactory.thunkFold(metas, argThunks) { lValue, rValue ->
            val denominator = rValue.numberValue()
            if (denominator.isZero()) {
                err("% by zero", ErrorCode.EVALUATOR_MODULO_BY_ZERO, errorContextFrom(metas), false)
            }

            (lValue.numberValue() % denominator).exprValue()
        }

        return checkIntegerOverflow(computeThunk, metas)
    }

    private fun compileEq(expr: PartiqlAst.Expr.Eq, metas: MetaContainer): ThunkEnv {
        val argThunks = compileAstExprs(expr.operands)

        return thunkFactory.thunkAndMap(metas, argThunks) { lValue, rValue ->
            (lValue.exprEquals(rValue))
        }
    }

    private fun compileNe(expr: PartiqlAst.Expr.Ne, metas: MetaContainer): ThunkEnv {
        val argThunks = compileAstExprs(expr.operands)

        return thunkFactory.thunkFold(metas, argThunks) { lValue, rValue ->
            ((!lValue.exprEquals(rValue)).exprValue())
        }
    }

    private fun compileLt(expr: PartiqlAst.Expr.Lt, metas: MetaContainer): ThunkEnv {
        val argThunks = compileAstExprs(expr.operands)

        return thunkFactory.thunkAndMap(metas, argThunks) { lValue, rValue -> lValue < rValue }
    }

    private fun compileLte(expr: PartiqlAst.Expr.Lte, metas: MetaContainer): ThunkEnv {
        val argThunks = compileAstExprs(expr.operands)

        return thunkFactory.thunkAndMap(metas, argThunks) { lValue, rValue -> lValue <= rValue }
    }

    private fun compileGt(expr: PartiqlAst.Expr.Gt, metas: MetaContainer): ThunkEnv {
        val argThunks = compileAstExprs(expr.operands)

        return thunkFactory.thunkAndMap(metas, argThunks) { lValue, rValue -> lValue > rValue }
    }

    private fun compileGte(expr: PartiqlAst.Expr.Gte, metas: MetaContainer): ThunkEnv {
        val argThunks = compileAstExprs(expr.operands)

        return thunkFactory.thunkAndMap(metas, argThunks) { lValue, rValue -> lValue >= rValue }
    }

    private fun compileBetween(expr: PartiqlAst.Expr.Between, metas: MetaContainer): ThunkEnv {
        val valueThunk = compileAstExpr(expr.value)
        val fromThunk = compileAstExpr(expr.from)
        val toThunk = compileAstExpr(expr.to)

        return thunkFactory.thunkEnvOperands(metas, valueThunk, fromThunk, toThunk) { _, v, f, t ->
            (v >= f && v <= t).exprValue()
        }
    }

    /**
     * `IN` can *almost* be thought of has being syntactic sugar for the `OR` operator.
     *
     * `a IN (b, c, d)` is equivalent to `a = b OR a = c OR a = d`.  On deep inspection, there
     * are important implications to this regarding propagation of unknown values.  Specifically, the
     * presence of any unknown in `b`, `c`, or `d` will result in unknown propagation iif `a` does not
     * equal `b`, `c`, or `d`. i.e.:
     *
     *     - `1 in (null, 2, 3)` -> `null`
     *     - `2 in (null, 2, 3)` -> `true`
     *     - `2 in (1, 2, 3)` -> `true`
     *     - `0 in (1, 2, 4)` -> `false`
     *
     * `IN` is varies from the `OR` operator in that this behavior holds true when other types of expressions are
     * used on the right side of `IN` such as sub-queries and variables whose value is that of a list or bag.
     */
    private fun compileIn(expr: PartiqlAst.Expr.InCollection, metas: MetaContainer): ThunkEnv {
        val args = expr.operands
        val leftThunk = compileAstExpr(args[0])
        val rightOp = args[1]

        fun isOptimizedCase(values: List<PartiqlAst.Expr>): Boolean =
            values.all { it is PartiqlAst.Expr.Lit && !it.value.isNull }

        fun optimizedCase(values: List<PartiqlAst.Expr>): ThunkEnv {
            // Put all the literals in the sequence into a pre-computed map to be checked later by the thunk.
            // If the left-hand value is one of these we can short-circuit with a result of TRUE.
            // This is the fastest possible case and allows for hundreds of literal values (or more) in the
            // sequence without a huge performance penalty.
            // NOTE: we cannot use a [HashSet<>] here because [ExprValue] does not implement [Object.hashCode] or
            // [Object.equals].
            val precomputedLiteralsMap = values
                .filterIsInstance<PartiqlAst.Expr.Lit>()
                .mapTo(TreeSet<ExprValue>(DEFAULT_COMPARATOR)) {
                    valueFactory.newFromIonValue(
                        it.value.toIonValue(
                            valueFactory.ion
                        )
                    )
                }

            // the compiled thunk simply checks if the left side is contained on the right side.
            // thunkEnvOperands takes care of unknown propagation for the left side; for the right,
            // this unknown propagation does not apply since we've eliminated the possibility of unknowns above.
            return thunkFactory.thunkEnvOperands(metas, leftThunk) { _, leftValue ->
                precomputedLiteralsMap.contains(leftValue).exprValue()
            }
        }

        return when {
            // We can significantly optimize this if rightArg is a sequence constructor which is composed of entirely
            // of non-null literal values.
            rightOp is PartiqlAst.Expr.List && isOptimizedCase(rightOp.values) -> optimizedCase(rightOp.values)
            rightOp is PartiqlAst.Expr.Bag && isOptimizedCase(rightOp.values) -> optimizedCase(rightOp.values)
            rightOp is PartiqlAst.Expr.Sexp && isOptimizedCase(rightOp.values) -> optimizedCase(rightOp.values)
            // The unoptimized case...
            else -> {
                val rightThunk = compileAstExpr(rightOp)

                // Legacy mode:
                //      Returns FALSE when the right side of IN is not a sequence
                //      Returns NULL if the right side is MISSING or any value on the right side is MISSING
                // Permissive mode:
                //      Returns MISSING when the right side of IN is not a sequence
                //      Returns MISSING if the right side is MISSING or any value on the right side is MISSING
                val (propagateMissingAs, propagateNotASeqAs) = with(valueFactory) {
                    when (compileOptions.typingMode) {
                        TypingMode.LEGACY -> nullValue to newBoolean(false)
                        TypingMode.PERMISSIVE -> missingValue to missingValue
                    }
                }

                // Note that standard unknown propagation applies to the left and right operands. Both [TypingMode]s
                // are handled by [ThunkFactory.thunkEnvOperands] and that additional rules for unknown propagation are
                // implemented within the thunk for the values within the sequence on the right side of IN.
                thunkFactory.thunkEnvOperands(metas, leftThunk, rightThunk) { _, leftValue, rightValue ->
                    var nullSeen = false
                    var missingSeen = false

                    when {
                        rightValue.type == ExprValueType.MISSING -> propagateMissingAs
                        !rightValue.type.isSequence -> propagateNotASeqAs
                        else -> {
                            rightValue.forEach {
                                when (it.type) {
                                    ExprValueType.NULL -> nullSeen = true
                                    ExprValueType.MISSING -> missingSeen = true
                                    // short-circuit to TRUE on the first matching value
                                    else -> if (it.exprEquals(leftValue)) {
                                        return@thunkEnvOperands valueFactory.newBoolean(true)
                                    }
                                }
                            }
                            // If we make it here then there was no match. Propagate MISSING, NULL or return false.
                            // Note that if both MISSING and NULL was encountered, MISSING takes precedence.
                            when {
                                missingSeen -> propagateMissingAs
                                nullSeen -> valueFactory.nullValue
                                else -> valueFactory.newBoolean(false)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun compileNot(expr: PartiqlAst.Expr.Not, metas: MetaContainer): ThunkEnv {
        val argThunk = compileAstExpr(expr.expr)

        return thunkFactory.thunkEnvOperands(metas, argThunk) { _, value ->
            (!value.booleanValue()).exprValue()
        }
    }

    private fun compileAnd(expr: PartiqlAst.Expr.And, metas: MetaContainer): ThunkEnv {
        val argThunks = compileAstExprs(expr.operands)

        // can't use the null propagation supplied by [ThunkFactory.thunkEnv] here because AND short-circuits on
        // false values and *NOT* on NULL or MISSING
        return when (compileOptions.typingMode) {
            TypingMode.LEGACY -> thunkFactory.thunkEnv(metas) thunk@{ env ->
                var hasUnknowns = false
                argThunks.forEach { currThunk ->
                    val currValue = currThunk(env)
                    when {
                        currValue.isUnknown() -> hasUnknowns = true
                        // Short circuit only if we encounter a known false value.
                        !currValue.booleanValue() -> return@thunk valueFactory.newBoolean(false)
                    }
                }

                when (hasUnknowns) {
                    true -> valueFactory.nullValue
                    false -> valueFactory.newBoolean(true)
                }
            }
            TypingMode.PERMISSIVE -> thunkFactory.thunkEnv(metas) thunk@{ env ->
                var hasNull = false
                var hasMissing = false
                argThunks.forEach { currThunk ->
                    val currValue = currThunk(env)
                    when (currValue.type) {
                        // Short circuit only if we encounter a known false value.
                        ExprValueType.BOOL -> if (!currValue.booleanValue()) return@thunk valueFactory.newBoolean(false)
                        ExprValueType.NULL -> hasNull = true
                        // type mismatch, return missing
                        else -> hasMissing = true
                    }
                }

                when {
                    hasMissing -> valueFactory.missingValue
                    hasNull -> valueFactory.nullValue
                    else -> valueFactory.newBoolean(true)
                }
            }
        }
    }

    private fun compileOr(expr: PartiqlAst.Expr.Or, metas: MetaContainer): ThunkEnv {
        val argThunks = compileAstExprs(expr.operands)

        // can't use the null propagation supplied by [ThunkFactory.thunkEnv] here because OR short-circuits on
        // true values and *NOT* on NULL or MISSING
        return when (compileOptions.typingMode) {
            TypingMode.LEGACY ->
                thunkFactory.thunkEnv(metas) thunk@{ env ->
                    var hasUnknowns = false
                    argThunks.forEach { currThunk ->
                        val currValue = currThunk(env)
                        // How null-propagation works for OR is rather weird according to the SQL-92 spec.
                        // Nulls are propagated like other expressions only when none of the terms are TRUE.
                        // If any one of them is TRUE, then the entire expression evaluates to TRUE, i.e.:
                        //     NULL OR TRUE -> TRUE
                        //     NULL OR FALSE -> NULL
                        // (strange but true)
                        when {
                            currValue.isUnknown() -> hasUnknowns = true
                            currValue.booleanValue() -> return@thunk valueFactory.newBoolean(true)
                        }
                    }

                    when (hasUnknowns) {
                        true -> valueFactory.nullValue
                        false -> valueFactory.newBoolean(false)
                    }
                }
            TypingMode.PERMISSIVE -> thunkFactory.thunkEnv(metas) thunk@{ env ->
                var hasNull = false
                var hasMissing = false
                argThunks.forEach { currThunk ->
                    val currValue = currThunk(env)
                    when (currValue.type) {
                        // Short circuit only if we encounter a known true value.
                        ExprValueType.BOOL -> if (currValue.booleanValue()) return@thunk valueFactory.newBoolean(true)
                        ExprValueType.NULL -> hasNull = true
                        else -> hasMissing = true // type mismatch, return missing.
                    }
                }

                when {
                    hasMissing -> valueFactory.missingValue
                    hasNull -> valueFactory.nullValue
                    else -> valueFactory.newBoolean(false)
                }
            }
        }
    }

    private fun compileConcat(expr: PartiqlAst.Expr.Concat, metas: MetaContainer): ThunkEnv {
        val argThunks = compileAstExprs(expr.operands)

        return thunkFactory.thunkFold(metas, argThunks) { lValue, rValue ->
            val lType = lValue.type
            val rType = rValue.type

            if (lType.isText && rType.isText) {
                // null/missing propagation is handled before getting here
                (lValue.stringValue() + rValue.stringValue()).exprValue()
            } else {
                err(
                    "Wrong argument type for ||",
                    ErrorCode.EVALUATOR_CONCAT_FAILED_DUE_TO_INCOMPATIBLE_TYPE,
                    errorContextFrom(metas).also {
                        it[Property.ACTUAL_ARGUMENT_TYPES] = listOf(lType, rType).toString()
                    },
                    internal = false
                )
            }
        }
    }

    private fun compileCall(expr: PartiqlAst.Expr.Call, metas: MetaContainer): ThunkEnv {
        val funcArgThunks = compileAstExprs(expr.args)
        val func = functions[expr.funcName.text] ?: err(
            "No such function: ${expr.funcName.text}",
            ErrorCode.EVALUATOR_NO_SUCH_FUNCTION,
            errorContextFrom(metas).also {
                it[Property.FUNCTION_NAME] = expr.funcName.text
            },
            internal = false
        )

        // Check arity
        if (funcArgThunks.size !in func.signature.arity) {
            val errorContext = errorContextFrom(metas).also {
                it[Property.FUNCTION_NAME] = func.signature.name
                it[Property.EXPECTED_ARITY_MIN] = func.signature.arity.first
                it[Property.EXPECTED_ARITY_MAX] = func.signature.arity.last
                it[Property.ACTUAL_ARITY] = funcArgThunks.size
            }

            val message = when {
                func.signature.arity.first == 1 && func.signature.arity.last == 1 ->
                    "${func.signature.name} takes a single argument, received: ${funcArgThunks.size}"
                func.signature.arity.first == func.signature.arity.last ->
                    "${func.signature.name} takes exactly ${func.signature.arity.first} arguments, received: ${funcArgThunks.size}"
                else ->
                    "${func.signature.name} takes between ${func.signature.arity.first} and " +
                        "${func.signature.arity.last} arguments, received: ${funcArgThunks.size}"
            }

            err(
                message,
                ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
                errorContext,
                internal = false
            )
        }

        fun checkArgumentTypes(signature: FunctionSignature, args: List<ExprValue>): Arguments {
            fun checkArgumentType(formalStaticType: StaticType, actualArg: ExprValue, position: Int) {
                val formalExprValueTypeDomain = formalStaticType.typeDomain

                val actualExprValueType = actualArg.type
                val actualStaticType = StaticType.fromExprValue(actualArg)

                if (!actualStaticType.isSubTypeOf(formalStaticType)) {
                    errInvalidArgumentType(
                        signature = signature,
                        position = position,
                        expectedTypes = formalExprValueTypeDomain.toList(),
                        actualType = actualExprValueType
                    )
                }
            }

            val required = args.take(signature.requiredParameters.size)
            val rest = args.drop(signature.requiredParameters.size)

            signature.requiredParameters.zip(required).forEachIndexed { idx, (expected, actual) ->
                checkArgumentType(expected, actual, idx + 1)
            }

            return if (signature.optionalParameter != null && rest.isNotEmpty()) {
                val opt = rest.last()
                checkArgumentType(signature.optionalParameter, opt, required.size + 1)
                RequiredWithOptional(required, opt)
            } else if (signature.variadicParameter != null) {
                rest.forEachIndexed { idx, arg ->
                    checkArgumentType(signature.variadicParameter.type, arg, required.size + 1 + idx)
                }
                RequiredWithVariadic(required, rest)
            } else {
                RequiredArgs(required)
            }
        }

        val computeThunk = when (func.signature.unknownArguments) {
            UnknownArguments.PROPAGATE -> thunkFactory.thunkEnvOperands(metas, funcArgThunks) { env, values ->
                val checkedArgs = checkArgumentTypes(func.signature, values)
                func.call(env.session, checkedArgs)
            }
            UnknownArguments.PASS_THRU -> thunkFactory.thunkEnv(metas) { env ->
                val funcArgValues = funcArgThunks.map { it(env) }
                val checkedArgs = checkArgumentTypes(func.signature, funcArgValues)
                func.call(env.session, checkedArgs)
            }
        }

        return checkIntegerOverflow(computeThunk, metas)
    }

    private fun compileLit(expr: PartiqlAst.Expr.Lit, metas: MetaContainer): ThunkEnv {
        val value = valueFactory.newFromIonValue(expr.value.toIonValue(valueFactory.ion))

        return thunkFactory.thunkEnv(metas) { value }
    }

    private fun compileMissing(metas: MetaContainer): ThunkEnv =
        thunkFactory.thunkEnv(metas) { valueFactory.missingValue }

    private fun compileId(expr: PartiqlAst.Expr.Id, metas: MetaContainer): ThunkEnv {
        val uniqueNameMeta = metas[UniqueNameMeta.TAG] as? UniqueNameMeta
        val fromSourceNames = currentCompilationContext.fromSourceNames

        return when (uniqueNameMeta) {
            null -> {
                val bindingName = BindingName(expr.name.text, expr.case.toBindingCase())
                val evalVariableReference = when (compileOptions.undefinedVariable) {
                    UndefinedVariableBehavior.ERROR ->
                        thunkFactory.thunkEnv(metas) { env ->
                            when (val value = env.current[bindingName]) {
                                null -> {
                                    if (fromSourceNames.any { bindingName.isEquivalentTo(it) }) {
                                        err(
                                            "Variable not in GROUP BY or aggregation function: ${bindingName.name}",
                                            ErrorCode.EVALUATOR_VARIABLE_NOT_INCLUDED_IN_GROUP_BY,
                                            errorContextFrom(metas).also {
                                                it[Property.BINDING_NAME] = bindingName.name
                                            },
                                            internal = false
                                        )
                                    } else {
                                        val (errorCode, hint) = when (expr.case) {
                                            is PartiqlAst.CaseSensitivity.CaseSensitive ->
                                                Pair(
                                                    ErrorCode.EVALUATOR_QUOTED_BINDING_DOES_NOT_EXIST,
                                                    " $UNBOUND_QUOTED_IDENTIFIER_HINT"
                                                )
                                            is PartiqlAst.CaseSensitivity.CaseInsensitive ->
                                                Pair(ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST, "")
                                        }
                                        err(
                                            "No such binding: ${bindingName.name}.$hint",
                                            errorCode,
                                            errorContextFrom(metas).also {
                                                it[Property.BINDING_NAME] = bindingName.name
                                            },
                                            internal = false
                                        )
                                    }
                                }
                                else -> value
                            }
                        }
                    UndefinedVariableBehavior.MISSING ->
                        thunkFactory.thunkEnv(metas) { env ->
                            env.current[bindingName] ?: valueFactory.missingValue
                        }
                }

                when (expr.qualifier) {
                    is PartiqlAst.ScopeQualifier.Unqualified -> evalVariableReference
                    is PartiqlAst.ScopeQualifier.LocalsFirst -> thunkFactory.thunkEnv(metas) { env ->
                        evalVariableReference(env.flipToLocals())
                    }
                }
            }
            else -> {
                val bindingName = BindingName(uniqueNameMeta.uniqueName, BindingCase.SENSITIVE)

                thunkFactory.thunkEnv(metas) { env ->
                    // Unique identifiers are generated by the compiler and should always resolve.  If they
                    // don't for some reason we have a bug.
                    env.current[bindingName] ?: err(
                        "Uniquely named binding \"${bindingName.name}\" does not exist for some reason",
                        ErrorCode.INTERNAL_ERROR,
                        errorContextFrom(metas),
                        internal = true
                    )
                }
            }
        }
    }

    private fun compileParameter(expr: PartiqlAst.Expr.Parameter, metas: MetaContainer): ThunkEnv {
        val ordinal = expr.index.value.toInt()
        val index = ordinal - 1

        return { env ->
            val params = env.session.parameters
            if (params.size <= index) {
                err(
                    "Unbound parameter for ordinal: $ordinal",
                    ErrorCode.EVALUATOR_UNBOUND_PARAMETER,
                    errorContextFrom(metas).also {
                        it[Property.EXPECTED_PARAMETER_ORDINAL] = ordinal
                        it[Property.BOUND_PARAMETER_COUNT] = params.size
                    },
                    internal = false
                )
            }

            params[index]
        }
    }

    /**
     * Returns a lambda that implements the `IS` operator type check according to the current
     * [TypedOpBehavior].
     */
    private fun makeIsCheck(
        staticType: SingleType,
        typedOpParameter: TypedOpParameter,
        metas: MetaContainer
    ): (ExprValue) -> Boolean {
        val exprValueType = staticType.runtimeType

        // The "simple" type match function only looks at the [ExprValueType] of the [ExprValue]
        // and invokes the custom [validationThunk] if one exists.
        val simpleTypeMatchFunc = { expValue: ExprValue ->
            val isTypeMatch = when (exprValueType) {
                // MISSING IS NULL and NULL IS MISSING
                ExprValueType.NULL -> expValue.type.isUnknown
                else -> expValue.type == exprValueType
            }
            (isTypeMatch && typedOpParameter.validationThunk?.let { it(expValue) } != false)
        }

        return when (compileOptions.typedOpBehavior) {
            TypedOpBehavior.LEGACY -> simpleTypeMatchFunc
            TypedOpBehavior.HONOR_PARAMETERS -> { expValue: ExprValue ->
                staticType.allTypes.any {
                    val matchesStaticType = try {
                        it.isInstance(expValue)
                    } catch (e: UnsupportedTypeCheckException) {
                        err(
                            e.message!!,
                            ErrorCode.UNIMPLEMENTED_FEATURE,
                            errorContextFrom(metas),
                            internal = true
                        )
                    }

                    when {
                        !matchesStaticType -> false
                        else -> when (val validator = typedOpParameter.validationThunk) {
                            null -> true
                            else -> validator(expValue)
                        }
                    }
                }
            }
        }
    }

    private fun compileIs(expr: PartiqlAst.Expr.IsType, metas: MetaContainer): ThunkEnv {
        val expThunk = compileAstExpr(expr.value)
        val typedOpParameter = expr.type.toTypedOpParameter()
        if (typedOpParameter.staticType is AnyType) {
            return thunkFactory.thunkEnv(metas) { valueFactory.newBoolean(true) }
        }
        if (compileOptions.typedOpBehavior == TypedOpBehavior.HONOR_PARAMETERS && expr.type is PartiqlAst.Type.FloatType && expr.type.precision != null) {
            err(
                "FLOAT precision parameter is unsupported",
                ErrorCode.SEMANTIC_FLOAT_PRECISION_UNSUPPORTED,
                errorContextFrom(expr.type.metas),
                internal = false
            )
        }

        val typeMatchFunc = when (val staticType = typedOpParameter.staticType) {
            is SingleType -> makeIsCheck(staticType, typedOpParameter, metas)
            is AnyOfType -> staticType.types.map { childType ->
                when (childType) {
                    is SingleType -> makeIsCheck(childType, typedOpParameter, metas)
                    else -> err(
                        "Union type cannot have ANY or nested AnyOf type for IS",
                        ErrorCode.SEMANTIC_UNION_TYPE_INVALID,
                        errorContextFrom(metas),
                        internal = true
                    )
                }
            }.let { typeMatchFuncs ->
                { expValue: ExprValue -> typeMatchFuncs.any { func -> func(expValue) } }
            }
            is AnyType -> throw IllegalStateException("Unexpected ANY type in IS compilation")
        }

        return thunkFactory.thunkEnv(metas) { env ->
            val expValue = expThunk(env)
            typeMatchFunc(expValue).exprValue()
        }
    }

    private fun compileCastHelper(value: PartiqlAst.Expr, asType: PartiqlAst.Type, metas: MetaContainer): ThunkEnv {
        val expThunk = compileAstExpr(value)
        val typedOpParameter = asType.toTypedOpParameter()
        if (typedOpParameter.staticType is AnyType) {
            return expThunk
        }
        if (compileOptions.typedOpBehavior == TypedOpBehavior.HONOR_PARAMETERS && asType is PartiqlAst.Type.FloatType && asType.precision != null) {
            err(
                "FLOAT precision parameter is unsupported",
                ErrorCode.SEMANTIC_FLOAT_PRECISION_UNSUPPORTED,
                errorContextFrom(asType.metas),
                internal = false
            )
        }

        fun typeOpValidate(
            value: ExprValue,
            castOutput: ExprValue,
            typeName: String,
            locationMeta: SourceLocationMeta?
        ) {
            if (typedOpParameter.validationThunk?.let { it(castOutput) } == false) {
                val errorContext = PropertyValueMap().also {
                    it[Property.CAST_FROM] = value.type.toString()
                    it[Property.CAST_TO] = typeName
                }

                locationMeta?.let { fillErrorContext(errorContext, it) }

                err(
                    "Validation failure for $asType",
                    ErrorCode.EVALUATOR_CAST_FAILED,
                    errorContext,
                    internal = false
                )
            }
        }

        fun singleTypeCastFunc(singleType: SingleType): CastFunc {
            val locationMeta = metas.sourceLocationMeta
            return { value ->
                val castOutput = value.cast(
                    singleType,
                    valueFactory,
                    compileOptions.typedOpBehavior,
                    locationMeta,
                    compileOptions.defaultTimezoneOffset
                )
                typeOpValidate(value, castOutput, singleType.runtimeType.toString(), locationMeta)
                castOutput
            }
        }

        fun compileSingleTypeCast(singleType: SingleType): ThunkEnv {
            val castFunc = singleTypeCastFunc(singleType)
            // We do not use thunkFactory here because we want to explicitly avoid
            // the optional evaluation-time type check for CAN_CAST below.
            // Can cast needs  that returns false if an
            // exception is thrown during a normal cast operation.
            return { env ->
                val valueToCast = expThunk(env)
                castFunc(valueToCast)
            }
        }

        fun compileCast(type: StaticType): ThunkEnv = when (type) {
            is SingleType -> compileSingleTypeCast(type)
            is AnyOfType -> {
                val locationMeta = metas.sourceLocationMeta
                val castTable = AnyOfCastTable(type, metas, valueFactory, ::singleTypeCastFunc);

                // We do not use thunkFactory here because we want to explicitly avoid
                // the optional evaluation-time type check for CAN_CAST below.
                // note that this would interfere with the error handling for can_cast that returns false if an
                // exception is thrown during a normal cast operation.
                { env ->
                    val sourceValue = expThunk(env)
                    castTable.cast(sourceValue).also {
                        // TODO put the right type name here
                        typeOpValidate(sourceValue, it, "<UNION TYPE>", locationMeta)
                    }
                }
            }
            is AnyType -> throw IllegalStateException("Unreachable code")
        }

        return compileCast(typedOpParameter.staticType)
    }

    private fun compileCast(expr: PartiqlAst.Expr.Cast, metas: MetaContainer): ThunkEnv =
        thunkFactory.thunkEnv(metas, compileCastHelper(expr.value, expr.asType, metas))

    private fun compileCanCast(expr: PartiqlAst.Expr.CanCast, metas: MetaContainer): ThunkEnv {
        val typedOpParameter = expr.asType.toTypedOpParameter()
        if (typedOpParameter.staticType is AnyType) {
            return thunkFactory.thunkEnv(metas) { valueFactory.newBoolean(true) }
        }

        val expThunk = compileAstExpr(expr.value)

        // TODO consider making this more efficient by not directly delegating to CAST
        // TODO consider also making the operand not double evaluated (e.g. having expThunk memoize)
        val castThunkEnv = compileCastHelper(expr.value, expr.asType, expr.metas)
        return thunkFactory.thunkEnv(metas) { env ->
            val sourceValue = expThunk(env)
            try {
                when {
                    // NULL/MISSING can cast to anything as themselves
                    sourceValue.isUnknown() -> valueFactory.newBoolean(true)
                    else -> {
                        val castedValue = castThunkEnv(env)
                        when {
                            // NULL/MISSING from cast is a permissive way to signal failure
                            castedValue.isUnknown() -> valueFactory.newBoolean(false)
                            else -> valueFactory.newBoolean(true)
                        }
                    }
                }
            } catch (e: EvaluationException) {
                if (e.internal) {
                    throw e
                }
                valueFactory.newBoolean(false)
            }
        }
    }

    private fun compileCanLosslessCast(expr: PartiqlAst.Expr.CanLosslessCast, metas: MetaContainer): ThunkEnv {
        val typedOpParameter = expr.asType.toTypedOpParameter()
        if (typedOpParameter.staticType is AnyType) {
            return thunkFactory.thunkEnv(metas) { valueFactory.newBoolean(true) }
        }

        val expThunk = compileAstExpr(expr.value)

        // TODO consider making this more efficient by not directly delegating to CAST
        val castThunkEnv = compileCastHelper(expr.value, expr.asType, expr.metas)
        return thunkFactory.thunkEnv(metas) { env ->
            val sourceValue = expThunk(env)
            val sourceType = StaticType.fromExprValue(sourceValue)

            fun roundTrip(): ExprValue {
                val castedValue = castThunkEnv(env)

                val locationMeta = metas.sourceLocationMeta
                fun castFunc(singleType: SingleType) =
                    { value: ExprValue ->
                        value.cast(
                            singleType,
                            valueFactory,
                            compileOptions.typedOpBehavior,
                            locationMeta,
                            compileOptions.defaultTimezoneOffset
                        )
                    }

                val roundTripped = when (sourceType) {
                    is SingleType -> castFunc(sourceType)(castedValue)
                    is AnyOfType -> {
                        val castTable = AnyOfCastTable(sourceType, metas, valueFactory, ::castFunc)
                        castTable.cast(sourceValue)
                    }
                    // Should not be possible
                    is AnyType -> throw IllegalStateException("ANY type is not configured correctly in compiler")
                }

                val lossless = sourceValue.exprEquals(roundTripped)
                return valueFactory.newBoolean(lossless)
            }

            try {
                when (sourceValue.type) {
                    // NULL can cast to anything as itself
                    ExprValueType.NULL -> valueFactory.newBoolean(true)

                    // Short-circuit timestamp -> date roundtrip if precision isn't [Timestamp.Precision.DAY] or
                    //   [Timestamp.Precision.MONTH] or [Timestamp.Precision.YEAR]
                    ExprValueType.TIMESTAMP -> when (typedOpParameter.staticType) {
                        StaticType.DATE -> when (sourceValue.ionValue.timestampValue().precision) {
                            Timestamp.Precision.DAY, Timestamp.Precision.MONTH, Timestamp.Precision.YEAR -> roundTrip()
                            else -> valueFactory.newBoolean(false)
                        }
                        StaticType.TIME -> valueFactory.newBoolean(false)
                        else -> roundTrip()
                    }

                    // For all other cases, attempt a round-trip of the value through the source and dest types
                    else -> roundTrip()
                }
            } catch (e: EvaluationException) {
                if (e.internal) {
                    throw e
                }
                valueFactory.newBoolean(false)
            }
        }
    }

    private fun compileSimpleCase(expr: PartiqlAst.Expr.SimpleCase, metas: MetaContainer): ThunkEnv {
        val valueThunk = compileAstExpr(expr.expr)
        val branchThunks = expr.cases.pairs.map { Pair(compileAstExpr(it.first), compileAstExpr(it.second)) }
        val elseThunk = when (expr.default) {
            null -> thunkFactory.thunkEnv(metas) { valueFactory.nullValue }
            else -> compileAstExpr(expr.default)
        }

        return thunkFactory.thunkEnv(metas) thunk@{ env ->
            val caseValue = valueThunk(env)
            // if the case value is unknown then we can short-circuit to the elseThunk directly
            when {
                caseValue.isUnknown() -> elseThunk(env)
                else -> {
                    branchThunks.forEach { bt ->
                        val branchValue = bt.first(env)
                        // Just skip any branch values that are unknown, which we consider the same as false here.
                        when {
                            branchValue.isUnknown() -> { /* intentionally blank */
                            }
                            else -> {
                                if (caseValue.exprEquals(branchValue)) {
                                    return@thunk bt.second(env)
                                }
                            }
                        }
                    }
                }
            }
            elseThunk(env)
        }
    }

    private fun compileSearchedCase(expr: PartiqlAst.Expr.SearchedCase, metas: MetaContainer): ThunkEnv {
        val branchThunks = expr.cases.pairs.map { compileAstExpr(it.first) to compileAstExpr(it.second) }
        val elseThunk = when (expr.default) {
            null -> thunkFactory.thunkEnv(metas) { valueFactory.nullValue }
            else -> compileAstExpr(expr.default)
        }

        return when (compileOptions.typingMode) {
            TypingMode.LEGACY -> thunkFactory.thunkEnv(metas) thunk@{ env ->
                branchThunks.forEach { bt ->
                    val conditionValue = bt.first(env)
                    // Any unknown value is considered the same as false.
                    // Note that .booleanValue() here will throw an EvaluationException if
                    // the data type is not boolean.
                    // TODO:  .booleanValue does not have access to metas, so the EvaluationException is reported to be
                    // at the line & column of the CASE statement, not the predicate, unfortunately.
                    if (conditionValue.isNotUnknown() && conditionValue.booleanValue()) {
                        return@thunk bt.second(env)
                    }
                }
                elseThunk(env)
            }
            // Permissive mode propagates data type mismatches as MISSING, which is
            // equivalent to false for searched CASE predicates.  To simplify this,
            // all we really need to do is consider any non-boolean result from the
            // predicate to be false.
            TypingMode.PERMISSIVE -> thunkFactory.thunkEnv(metas) thunk@{ env ->
                branchThunks.forEach { bt ->
                    val conditionValue = bt.first(env)
                    if (conditionValue.type == ExprValueType.BOOL && conditionValue.booleanValue()) {
                        return@thunk bt.second(env)
                    }
                }
                elseThunk(env)
            }
        }
    }

    private fun compileStruct(expr: PartiqlAst.Expr.Struct, metas: MetaContainer): ThunkEnv {
        class StructFieldThunks(val nameThunk: ThunkEnv, val valueThunk: ThunkEnv)

        val fieldThunks = expr.fields.map {
            StructFieldThunks(compileAstExpr(it.first), compileAstExpr(it.second))
        }

        return when (compileOptions.typingMode) {
            TypingMode.LEGACY -> thunkFactory.thunkEnv(metas) { env ->
                val seq = fieldThunks.map {
                    val nameValue = it.nameThunk(env)
                    if (!nameValue.type.isText) {
                        // Evaluation time error where variable reference might be evaluated to non-text struct field.
                        err(
                            "Found struct field key to be of type ${nameValue.type}",
                            ErrorCode.EVALUATOR_NON_TEXT_STRUCT_FIELD_KEY,
                            errorContextFrom(metas.sourceLocationMeta).also { pvm ->
                                pvm[Property.ACTUAL_TYPE] = nameValue.type.toString()
                            },
                            internal = false
                        )
                    }
                    it.valueThunk(env).namedValue(nameValue)
                }.asSequence()
                createStructExprValue(seq, StructOrdering.ORDERED)
            }
            TypingMode.PERMISSIVE -> thunkFactory.thunkEnv(metas) { env ->
                val seq = fieldThunks.map { it.valueThunk(env).namedValue(it.nameThunk(env)) }.asSequence()
                    .filter {
                        // fields with non-text keys are filtered out of the struct
                        val keyType = it.name?.type
                        keyType != null && keyType.isText
                    }
                createStructExprValue(seq, StructOrdering.ORDERED)
            }
        }
    }

    private fun compileSeq(seqType: ExprValueType, itemExprs: List<PartiqlAst.Expr>, metas: MetaContainer): ThunkEnv {
        require(seqType.isSequence) { "seqType must be a sequence!" }

        val itemThunks = compileAstExprs(itemExprs)

        val makeItemThunkSequence = when (seqType) {
            ExprValueType.BAG -> { env: Environment ->
                itemThunks.asSequence().map { itemThunk ->
                    // call to unnamedValue() makes sure we don't expose any underlying value name/ordinal
                    itemThunk(env).unnamedValue()
                }
            }
            else -> { env: Environment ->
                itemThunks.asSequence().mapIndexed { i, itemThunk -> itemThunk(env).namedValue(i.exprValue()) }
            }
        }

        return thunkFactory.thunkEnv(metas) { env ->
            // todo:  use valueFactory.newSequence() instead.
            SequenceExprValue(
                valueFactory.ion,
                seqType,
                makeItemThunkSequence(env)
            )
        }
    }

    private fun compileBagOp(node: PartiqlAst.Expr.BagOp, metas: MetaContainer): ThunkEnv {
        val lhs = compileAstExpr(node.operands[0])
        val rhs = compileAstExpr(node.operands[1])
        val op = ExprValueBagOp.create(node.op, metas)
        return thunkFactory.thunkEnv(metas) { env ->
            val l = lhs(env)
            val r = rhs(env)
            val result = when (node.quantifier) {
                is PartiqlAst.SetQuantifier.All -> op.eval(l, r)
                is PartiqlAst.SetQuantifier.Distinct -> op.eval(l, r).distinct()
            }
            valueFactory.newBag(result)
        }
    }

    private fun evalLimit(limitThunk: ThunkEnv, env: Environment, limitLocationMeta: SourceLocationMeta?): Long {
        val limitExprValue = limitThunk(env)

        if (limitExprValue.type != ExprValueType.INT) {
            err(
                "LIMIT value was not an integer",
                ErrorCode.EVALUATOR_NON_INT_LIMIT_VALUE,
                errorContextFrom(limitLocationMeta).also {
                    it[Property.ACTUAL_TYPE] = limitExprValue.type.toString()
                },
                internal = false
            )
        }

        // `Number.toLong()` (used below) does *not* cause an overflow exception if the underlying [Number]
        // implementation (i.e. Decimal or BigInteger) exceeds the range that can be represented by Longs.
        // This can cause very confusing behavior if the user specifies a LIMIT value that exceeds
        // Long.MAX_VALUE, because no results will be returned from their query.  That no overflow exception
        // is thrown is not a problem as long as PartiQL's restriction of integer values to +/- 2^63 remains.
        // We throw an exception here if the value exceeds the supported range (say if we change that
        // restriction or if a custom [ExprValue] is provided which exceeds that value).
        val limitIonValue = limitExprValue.ionValue as IonInt
        if (limitIonValue.integerSize == IntegerSize.BIG_INTEGER) {
            err(
                "IntegerSize.BIG_INTEGER not supported for LIMIT values",
                ErrorCode.INTERNAL_ERROR,
                errorContextFrom(limitLocationMeta),
                internal = true
            )
        }

        val limitValue = limitExprValue.numberValue().toLong()

        if (limitValue < 0) {
            err(
                "negative LIMIT",
                ErrorCode.EVALUATOR_NEGATIVE_LIMIT,
                errorContextFrom(limitLocationMeta),
                internal = false
            )
        }

        // we can't use the Kotlin's Sequence<T>.take(n) for this since it accepts only an integer.
        // this references [Sequence<T>.take(count: Long): Sequence<T>] defined in [org.partiql.util].
        return limitValue
    }

    private fun evalOffset(offsetThunk: ThunkEnv, env: Environment, offsetLocationMeta: SourceLocationMeta?): Long {
        val offsetExprValue = offsetThunk(env)

        if (offsetExprValue.type != ExprValueType.INT) {
            err(
                "OFFSET value was not an integer",
                ErrorCode.EVALUATOR_NON_INT_OFFSET_VALUE,
                errorContextFrom(offsetLocationMeta).also {
                    it[Property.ACTUAL_TYPE] = offsetExprValue.type.toString()
                },
                internal = false
            )
        }

        // `Number.toLong()` (used below) does *not* cause an overflow exception if the underlying [Number]
        // implementation (i.e. Decimal or BigInteger) exceeds the range that can be represented by Longs.
        // This can cause very confusing behavior if the user specifies a OFFSET value that exceeds
        // Long.MAX_VALUE, because no results will be returned from their query.  That no overflow exception
        // is thrown is not a problem as long as PartiQL's restriction of integer values to +/- 2^63 remains.
        // We throw an exception here if the value exceeds the supported range (say if we change that
        // restriction or if a custom [ExprValue] is provided which exceeds that value).
        val offsetIonValue = offsetExprValue.ionValue as IonInt
        if (offsetIonValue.integerSize == IntegerSize.BIG_INTEGER) {
            err(
                "IntegerSize.BIG_INTEGER not supported for OFFSET values",
                ErrorCode.INTERNAL_ERROR,
                errorContextFrom(offsetLocationMeta),
                internal = true
            )
        }

        val offsetValue = offsetExprValue.numberValue().toLong()

        if (offsetValue < 0) {
            err(
                "negative OFFSET",
                ErrorCode.EVALUATOR_NEGATIVE_OFFSET,
                errorContextFrom(offsetLocationMeta),
                internal = false
            )
        }

        return offsetValue
    }

    private fun compileSelect(selectExpr: PartiqlAst.Expr.Select, metas: MetaContainer): ThunkEnv {

        // Get all the FROM source aliases and LET bindings for binding error checks
        val fold = object : PartiqlAst.VisitorFold<Set<String>>() {
            /** Store all the visited FROM source aliases in the accumulator */
            override fun visitFromSourceScan(node: PartiqlAst.FromSource.Scan, accumulator: Set<String>): Set<String> {
                val aliases = listOfNotNull(node.asAlias?.text, node.atAlias?.text, node.byAlias?.text)
                return accumulator + aliases.toSet()
            }

            override fun visitLetBinding(node: PartiqlAst.LetBinding, accumulator: Set<String>): Set<String> {
                val aliases = listOfNotNull(node.name.text)
                return accumulator + aliases
            }

            /** Prevents visitor from recursing into nested select statements */
            override fun walkExprSelect(node: PartiqlAst.Expr.Select, accumulator: Set<String>): Set<String> {
                return accumulator
            }
        }

        val allFromSourceAliases = fold.walkFromSource(selectExpr.from, emptySet())
            .union(selectExpr.fromLet?.let { fold.walkLet(selectExpr.fromLet, emptySet()) } ?: emptySet())

        return nestCompilationContext(ExpressionContext.NORMAL, emptySet()) {
            val fromSourceThunks = compileFromSources(selectExpr.from)
            val letSourceThunks = selectExpr.fromLet?.let { compileLetSources(it) }
            val sourceThunks = compileQueryWithoutProjection(selectExpr, fromSourceThunks, letSourceThunks)

            val orderByThunk = selectExpr.order?.let { compileOrderByExpression(selectExpr.order.sortSpecs) }
            val orderByLocationMeta = selectExpr.order?.metas?.sourceLocation

            val offsetThunk = selectExpr.offset?.let { compileAstExpr(it) }
            val offsetLocationMeta = selectExpr.offset?.metas?.sourceLocation

            val limitThunk = selectExpr.limit?.let { compileAstExpr(it) }
            val limitLocationMeta = selectExpr.limit?.metas?.sourceLocation

            fun <T> rowsWithOffsetAndLimit(rows: Sequence<T>, env: Environment): Sequence<T> {
                val rowsWithOffset = when (offsetThunk) {
                    null -> rows
                    else -> rows.drop(evalOffset(offsetThunk, env, offsetLocationMeta))
                }
                return when (limitThunk) {
                    null -> rowsWithOffset
                    else -> rowsWithOffset.take(evalLimit(limitThunk, env, limitLocationMeta))
                }
            }

            // Returns a thunk that invokes [sourceThunks], and invokes [projectionThunk] to perform the projection.
            fun getQueryThunk(selectProjectionThunk: ThunkEnvValue<List<ExprValue>>): ThunkEnv {
                val groupByItems = selectExpr.group?.keyList?.keys ?: listOf()
                val groupAsName = selectExpr.group?.groupAsAlias

                val aggregateListMeta = metas[AggregateCallSiteListMeta.TAG] as AggregateCallSiteListMeta?
                val hasAggregateCallSites = aggregateListMeta?.aggregateCallSites?.any() ?: false

                val queryThunk = when {
                    groupByItems.isEmpty() && !hasAggregateCallSites ->
                        // Grouping is not needed -- simply project the results from the FROM clause directly.
                        thunkFactory.thunkEnv(metas) { env ->

                            val sourcedRows = sourceThunks(env)

                            val orderedRows = when (orderByThunk) {
                                null -> sourcedRows
                                else -> evalOrderBy(sourcedRows, orderByThunk, orderByLocationMeta)
                            }

                            val projectedRows = orderedRows.map { (joinedValues, projectEnv) ->
                                selectProjectionThunk(projectEnv, joinedValues)
                            }

                            val quantifiedRows = when (selectExpr.setq ?: PartiqlAst.SetQuantifier.All()) {
                                // wrap the ExprValue to use ExprValue.equals as the equality
                                is PartiqlAst.SetQuantifier.Distinct -> projectedRows.filter(createUniqueExprValueFilter())
                                is PartiqlAst.SetQuantifier.All -> projectedRows
                            }.let { rowsWithOffsetAndLimit(it, env) }

                            // if order by is specified, return list otherwise bag
                            when (orderByThunk) {
                                null -> valueFactory.newBag(
                                    quantifiedRows.map {
                                        // TODO make this expose the ordinal for ordered sequences
                                        // make sure we don't expose the underlying value's name out of a SELECT
                                        it.unnamedValue()
                                    }
                                )
                                else -> valueFactory.newList(quantifiedRows.map { it.unnamedValue() })
                            }
                        }
                    else -> {
                        // Grouping is needed

                        class CompiledAggregate(val factory: ExprAggregatorFactory, val argThunk: ThunkEnv)

                        // These aggregate call sites are collected in [AggregateSupportVisitorTransform].
                        val compiledAggregates = aggregateListMeta?.aggregateCallSites?.map { it ->
                            val funcName = it.funcName.text
                            CompiledAggregate(
                                factory = getAggregatorFactory(
                                    funcName,
                                    it.setq,
                                    it.metas
                                ),
                                argThunk = compileAstExpr(it.arg)
                            )
                        }

                        // This closure will be invoked to create and initialize a [RegisterBank] for new [Group]s.
                        val createRegisterBank: () -> RegisterBank = when (aggregateListMeta) {
                            // If there are no aggregates, create an empty register bank
                            null -> { -> // -> here forces this block to be a lambda
                                RegisterBank(0)
                            }
                            else -> { ->
                                RegisterBank(aggregateListMeta.aggregateCallSites.size).apply {
                                    // set up aggregate registers
                                    compiledAggregates?.forEachIndexed { index, ca ->
                                        set(index, ca.factory.create())
                                    }
                                }
                            }
                        }

                        when {
                            groupByItems.isEmpty() -> { // There are aggregates but no group by items
                                // Create a closure that groups all the rows in the FROM source into a single group.
                                thunkFactory.thunkEnv(metas) { env ->
                                    // Evaluate the FROM clause
                                    val orderedRows = when (orderByThunk) {
                                        null -> sourceThunks(env)
                                        else -> evalOrderBy(sourceThunks(env), orderByThunk, orderByLocationMeta)
                                    }

                                    val fromProductions: Sequence<FromProduction> =
                                        rowsWithOffsetAndLimit(orderedRows, env)
                                    val registers = createRegisterBank()

                                    // note: the group key can be anything here because we only ever have a single
                                    // group when aggregates are used without GROUP BY expression
                                    val syntheticGroup = Group(valueFactory.nullValue, registers)

                                    // iterate over the values from the FROM clause and populate our
                                    // aggregate register values.
                                    fromProductions.forEach { fromProduction ->
                                        compiledAggregates?.forEachIndexed { index, ca ->
                                            registers[index].aggregator.next(ca.argThunk(fromProduction.env))
                                        }
                                    }

                                    // generate the final group projection
                                    val groupResult = selectProjectionThunk(
                                        env.copy(currentGroup = syntheticGroup),
                                        listOf(syntheticGroup.key)
                                    )

                                    // if order by is specified, return list otherwise bag
                                    when (orderByThunk) {
                                        null -> valueFactory.newBag(listOf(groupResult).asSequence())
                                        else -> valueFactory.newList(listOf(groupResult).asSequence())
                                    }
                                }
                            }
                            else -> {
                                // There are GROUP BY expressions and possibly aggregates. (The most complex scenario.)

                                val compiledGroupByItems = compileGroupByExpressions(groupByItems)

                                val groupKeyThunk = compileGroupKeyThunk(compiledGroupByItems, metas)

                                // Memoize a [BindingName] and an [ExprValue] containing the name of each FromSource
                                // otherwise we would be re-creating them for every row.
                                val fromSourceBindingNames = fromSourceThunks.map {
                                    FromSourceBindingNamePair(
                                        BindingName(it.alias.asName, BindingCase.SENSITIVE),
                                        it.alias.asName.exprValue()
                                    )
                                }

                                val havingThunk = selectExpr.having?.let { compileAstExpr(it) }

                                val filterHavingAndProject: (Environment, Group) -> ExprValue? =
                                    createFilterHavingAndProjectClosure(havingThunk, selectProjectionThunk)

                                val getGroupEnv: (Environment, Group) -> Environment =
                                    createGetGroupEnvClosure(groupAsName)

                                thunkFactory.thunkEnv(metas) { env ->
                                    // Execute the FROM clause
                                    val fromProductions: Sequence<FromProduction> = sourceThunks(env)

                                    // For each "row" in the output of the FROM clause
                                    fromProductions.forEach { fromProduction ->

                                        // Determine the group key for this value
                                        val groupKey = groupKeyThunk(fromProduction.env)

                                        // look up existing group using group key (this is slow)
                                        // We can't do that yet because ExprValue does not implement .hashCode()
                                        // and .equals()
                                        val group: Group = env.groups.getOrPut(groupKey) {
                                            // An existing group was not found so create a new one
                                            Group(groupKey, createRegisterBank())
                                        }

                                        compiledAggregates!!.forEachIndexed { index, ca ->
                                            group.registers[index].aggregator.next(ca.argThunk(fromProduction.env))
                                        }

                                        groupAsName.run {
                                            val seq = fromSourceBindingNames.asSequence().map { pair ->
                                                (
                                                    fromProduction.env.current[pair.bindingName] ?: errNoContext(
                                                        "Could not resolve from source binding name during group as variable mapping",
                                                        errorCode = ErrorCode.INTERNAL_ERROR,
                                                        internal = true
                                                    )
                                                    ).namedValue(pair.nameExprValue)
                                            }.asSequence()

                                            group.groupValues.add(createStructExprValue(seq, StructOrdering.UNORDERED))
                                        }
                                    }

                                    val groupByEnvValuePairs = env.groups.mapNotNull { g -> getGroupEnv(env, g.value) to g.value }.asSequence()
                                    val orderedGroupEnvPairs = when (orderByThunk) {
                                        null -> groupByEnvValuePairs
                                        else -> evalOrderBy(groupByEnvValuePairs, orderByThunk, orderByLocationMeta)
                                    }

                                    // generate the final group by projection
                                    val projectedRows = orderedGroupEnvPairs.mapNotNull { (groupByEnv, groupValue) ->
                                        filterHavingAndProject(groupByEnv, groupValue)
                                    }.asSequence().let { rowsWithOffsetAndLimit(it, env) }

                                    // if order by is specified, return list otherwise bag
                                    when (orderByThunk) {
                                        null -> valueFactory.newBag(projectedRows)
                                        else -> valueFactory.newList(projectedRows)
                                    }
                                }
                            }
                        }
                    }
                } // do normal map/filter

                return thunkFactory.thunkEnv(metas) { env ->
                    queryThunk(env.nestQuery())
                }
            } // end of getQueryThunk(...)

            when (val project = selectExpr.project) {
                is PartiqlAst.Projection.ProjectValue -> {
                    nestCompilationContext(ExpressionContext.NORMAL, allFromSourceAliases) {
                        val valueThunk = compileAstExpr(project.value)
                        getQueryThunk(
                            thunkFactory.thunkEnvValueList(project.metas) { env, _ ->
                                valueThunk(
                                    env
                                )
                            }
                        )
                    }
                }
                is PartiqlAst.Projection.ProjectPivot -> {
                    val asExpr = project.value
                    val atExpr = project.key
                    nestCompilationContext(ExpressionContext.NORMAL, allFromSourceAliases) {
                        val asThunk = compileAstExpr(asExpr)
                        val atThunk = compileAstExpr(atExpr)
                        thunkFactory.thunkEnv(metas) { env ->
                            val sourceValue = rowsWithOffsetAndLimit(sourceThunks(env).asSequence(), env)
                            val seq = sourceValue
                                .map { (_, env) -> Pair(asThunk(env), atThunk(env)) }
                                .filter { (name, _) -> name.type.isText }
                                .map { (name, value) -> value.namedValue(name) }
                            createStructExprValue(seq, StructOrdering.UNORDERED)
                        }
                    }
                }
                is PartiqlAst.Projection.ProjectList -> {
                    val items = project.projectItems
                    nestCompilationContext(ExpressionContext.SELECT_LIST, allFromSourceAliases) {
                        val projectionThunk: ThunkEnvValue<List<ExprValue>> =
                            when {
                                items.filterIsInstance<PartiqlAst.Projection.ProjectStar>().any() -> {
                                    errNoContext(
                                        "Encountered a PartiqlAst.Projection.ProjectStar--did SelectStarVisitorTransform execute?",
                                        errorCode = ErrorCode.INTERNAL_ERROR,
                                        internal = true
                                    )
                                }
                                else -> {
                                    val projectionElements =
                                        compileSelectListToProjectionElements(project)

                                    val ordering = if (items.none { it is PartiqlAst.ProjectItem.ProjectAll })
                                        StructOrdering.ORDERED
                                    else
                                        StructOrdering.UNORDERED

                                    thunkFactory.thunkEnvValueList(project.metas) { env, _ ->
                                        val columns = mutableListOf<ExprValue>()
                                        for (element in projectionElements) {
                                            when (element) {
                                                is SingleProjectionElement -> {
                                                    val eval = element.thunk(env)
                                                    columns.add(eval.namedValue(element.name))
                                                }
                                                is MultipleProjectionElement -> {
                                                    for (projThunk in element.thunks) {
                                                        val value = projThunk(env)
                                                        if (value.type == ExprValueType.MISSING) continue

                                                        val children = value.asSequence()
                                                        if (!children.any() || value.type.isSequence) {
                                                            val name = syntheticColumnName(columns.size).exprValue()
                                                            columns.add(value.namedValue(name))
                                                        } else {
                                                            val valuesToProject =
                                                                when (compileOptions.projectionIteration) {
                                                                    ProjectionIterationBehavior.FILTER_MISSING -> {
                                                                        value.filter { it.type != ExprValueType.MISSING }
                                                                    }
                                                                    ProjectionIterationBehavior.UNFILTERED -> value
                                                                }
                                                            for (childValue in valuesToProject) {
                                                                val namedFacet = childValue.asFacet(Named::class.java)
                                                                val name = namedFacet?.name
                                                                    ?: syntheticColumnName(columns.size).exprValue()
                                                                columns.add(childValue.namedValue(name))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        createStructExprValue(columns.asSequence(), ordering)
                                    }
                                }
                            }
                        getQueryThunk(projectionThunk)
                    } // nestCompilationContext(ExpressionContext.SELECT_LIST)
                } // is SelectProjectionList
                is PartiqlAst.Projection.ProjectStar -> error("Internal Error: PartiqlAst.Projection.ProjectStar can only be wrapped in PartiqlAst.Projection.ProjectList")
            }
        }
    }

    private fun compileGroupByExpressions(groupByItems: List<PartiqlAst.GroupKey>): List<CompiledGroupByItem> =
        groupByItems.map {
            val alias = it.asAlias
                ?: errNoContext(
                    "GroupByItem.asName was not specified",
                    errorCode = ErrorCode.INTERNAL_ERROR,
                    internal = true
                )
            val uniqueName =
                (alias.metas.find(UniqueNameMeta.TAG) as UniqueNameMeta?)?.uniqueName

            CompiledGroupByItem(alias.text.exprValue(), uniqueName, compileAstExpr(it.expr))
        }

    /**
     * Create a thunk that uses the compiled GROUP BY expressions to create the group key.
     */
    private fun compileGroupKeyThunk(compiledGroupByItems: List<CompiledGroupByItem>, selectMetas: MetaContainer) =
        thunkFactory.thunkEnv(selectMetas) { env ->
            val uniqueNames = HashMap<String, ExprValue>(compiledGroupByItems.size, 1f)
            val keyValues = compiledGroupByItems.map { cgbi ->
                val value = cgbi.thunk(env).namedValue(cgbi.alias)
                if (cgbi.uniqueId != null) {
                    uniqueNames[cgbi.uniqueId] = value
                }
                value
            }
            GroupKeyExprValue(valueFactory.ion, keyValues.asSequence(), uniqueNames)
        }

    private fun compileOrderByExpression(sortSpecs: List<PartiqlAst.SortSpec>): List<CompiledOrderByItem> =
        sortSpecs.map {
            val comparator = when (it.orderingSpec ?: PartiqlAst.OrderingSpec.Asc()) {
                is PartiqlAst.OrderingSpec.Asc ->
                    when (it.nullsSpec) {
                        is PartiqlAst.NullsSpec.NullsFirst -> NaturalExprValueComparators.NULLS_FIRST_ASC
                        is PartiqlAst.NullsSpec.NullsLast -> NaturalExprValueComparators.NULLS_LAST_ASC
                        else -> NaturalExprValueComparators.NULLS_LAST_ASC
                    }

                is PartiqlAst.OrderingSpec.Desc ->
                    when (it.nullsSpec) {
                        is PartiqlAst.NullsSpec.NullsFirst -> NaturalExprValueComparators.NULLS_FIRST_DESC
                        is PartiqlAst.NullsSpec.NullsLast -> NaturalExprValueComparators.NULLS_LAST_DESC
                        else -> NaturalExprValueComparators.NULLS_FIRST_DESC
                    }
            }

            CompiledOrderByItem(comparator, compileAstExpr(it.expr))
        }

    private fun <T> evalOrderBy(
        rows: Sequence<T>,
        orderByItems: List<CompiledOrderByItem>,
        offsetLocationMeta: SourceLocationMeta?
    ): Sequence<T> {
        val initialComparator: Comparator<T>? = null
        val resultComparator = orderByItems.interruptibleFold(initialComparator) { intermediateComparator, orderByItem ->
            if (intermediateComparator == null) {
                return@interruptibleFold compareBy<T, ExprValue>(orderByItem.comparator) { row ->
                    val env = resolveEnvironment(row, offsetLocationMeta)
                    orderByItem.thunk(env)
                }
            }

            return@interruptibleFold intermediateComparator.thenBy(orderByItem.comparator) { row ->
                val env = resolveEnvironment(row, offsetLocationMeta)
                orderByItem.thunk(env)
            }
        } ?: errNoContext(
            "Order BY comparator cannot be null",
            ErrorCode.EVALUATOR_ORDER_BY_NULL_COMPARATOR,
            internal = true
        )

        return rows.sortedWith(resultComparator)
    }

    private fun <T> resolveEnvironment(envWrapper: T, offsetLocationMeta: SourceLocationMeta?): Environment {
        return when (envWrapper) {
            is FromProduction -> envWrapper.env
            is Pair<*, *> -> {
                if (envWrapper.first is Environment) {
                    envWrapper.first as Environment
                } else if (envWrapper.second is Environment) {
                    envWrapper.second as Environment
                } else {
                    err(
                        "Environment cannot be resolved from pair",
                        ErrorCode.EVALUATOR_ENVIRONMENT_CANNOT_BE_RESOLVED,
                        errorContextFrom(offsetLocationMeta),
                        internal = true
                    )
                }
            }
            else -> err(
                "Environment cannot be resolved",
                ErrorCode.EVALUATOR_ENVIRONMENT_CANNOT_BE_RESOLVED,
                errorContextFrom(offsetLocationMeta),
                internal = true
            )
        }
    }

    /**
     * Returns a closure which creates an [Environment] for the specified [Group].
     * If a GROUP AS name was specified, also nests that [Environment] in another that
     * has a binding for the GROUP AS name.
     */
    private fun createGetGroupEnvClosure(groupAsName: SymbolPrimitive?): (Environment, Group) -> Environment =
        when {
            groupAsName != null -> { groupByEnv, currentGroup ->
                val groupAsBindings = Bindings.buildLazyBindings<ExprValue> {
                    addBinding(groupAsName.text) {
                        valueFactory.newBag(currentGroup.groupValues.asSequence())
                    }
                }

                groupByEnv.nest(currentGroup.key.bindings, newGroup = currentGroup)
                    .nest(groupAsBindings)
            }
            else -> { groupByEnv, currentGroup ->
                groupByEnv.nest(currentGroup.key.bindings, newGroup = currentGroup)
            }
        }

    /**
     * Returns a closure which performs the final projection and returns the
     * result.  If a HAVING clause was included, a different closure is returned
     * that evaluates the HAVING clause and performs filtering.
     */
    private fun createFilterHavingAndProjectClosure(
        havingThunk: ThunkEnv?,
        selectProjectionThunk: ThunkEnvValue<List<ExprValue>>
    ): (Environment, Group) -> ExprValue? =
        when {
            havingThunk != null -> { groupByEnv, currentGroup ->
                // Create a closure that executes the HAVING clause and returns null if the
                // HAVING criteria is not met
                val havingClauseResult = havingThunk(groupByEnv)
                if (havingClauseResult.isNotUnknown() && havingClauseResult.booleanValue()) {
                    selectProjectionThunk(groupByEnv, listOf(currentGroup.key))
                } else {
                    null
                }
            }
            else -> { groupByEnv, currentGroup ->
                // Create a closure that simply performs the final projection and
                // returns the result.
                selectProjectionThunk(groupByEnv, listOf(currentGroup.key))
            }
        }

    private fun compileCallAgg(expr: PartiqlAst.Expr.CallAgg, metas: MetaContainer): ThunkEnv {
        if (metas.containsKey(IsCountStarMeta.TAG) && currentCompilationContext.expressionContext != ExpressionContext.SELECT_LIST) {
            err(
                "COUNT(*) is not allowed in this context",
                ErrorCode.EVALUATOR_COUNT_START_NOT_ALLOWED,
                errorContextFrom(metas),
                internal = false
            )
        }

        val aggFactory = getAggregatorFactory(expr.funcName.text.toLowerCase(), expr.setq, metas)

        val argThunk = nestCompilationContext(ExpressionContext.AGG_ARG, emptySet()) {
            compileAstExpr(expr.arg)
        }

        return when (currentCompilationContext.expressionContext) {
            ExpressionContext.AGG_ARG -> {
                err(
                    "The arguments of an aggregate function cannot contain aggregate functions",
                    ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_AGG_FUNCTION,
                    errorContextFrom(metas),
                    internal = false
                )
            }
            ExpressionContext.NORMAL ->
                thunkFactory.thunkEnv(metas) { env ->
                    val aggregator = aggFactory.create()
                    val argValue = argThunk(env)
                    argValue.forEach {
                        aggregator.next(it)
                    }
                    aggregator.compute()
                }
            ExpressionContext.SELECT_LIST -> {
                val registerIdMeta = metas[AggregateRegisterIdMeta.TAG] as AggregateRegisterIdMeta
                val registerId = registerIdMeta.registerId
                thunkFactory.thunkEnv(metas) { env ->
                    // Note: env.currentGroup must be set by caller.
                    val registers = env.currentGroup?.registers
                        ?: err(
                            "No current group or current group has no registers",
                            ErrorCode.INTERNAL_ERROR,
                            errorContextFrom(metas),
                            internal = true
                        )

                    registers[registerId].aggregator.compute()
                }
            }
        }
    }

    private fun getAggregatorFactory(
        funcName: String,
        setQuantifier: PartiqlAst.SetQuantifier,
        metas: MetaContainer
    ): ExprAggregatorFactory {
        val key = funcName.toLowerCase() to setQuantifier

        return builtinAggregates[key] ?: err(
            "No such function: $funcName",
            ErrorCode.EVALUATOR_NO_SUCH_FUNCTION,
            errorContextFrom(metas).also { it[Property.FUNCTION_NAME] = funcName },
            internal = false
        )
    }

    private fun compileFromSources(
        fromSource: PartiqlAst.FromSource,
        sources: MutableList<CompiledFromSource> = ArrayList(),
        joinExpansion: JoinExpansion = JoinExpansion.INNER,
        conditionThunk: ThunkEnv? = null
    ): List<CompiledFromSource> {
        val metas = fromSource.metas

        fun addAliases(
            thunk: ThunkEnv,
            asName: String?,
            atName: String?,
            byName: String?,
            metas: IonElementMetaContainer
        ) {
            sources.add(
                CompiledFromSource(
                    alias = Alias(
                        asName = asName ?: err(
                            "PartiqlAst.FromSource.Scan.variables.asName was null", ErrorCode.INTERNAL_ERROR,
                            errorContextFrom(metas), internal = true
                        ),
                        atName = atName,
                        byName = byName
                    ),
                    thunk = thunk,
                    joinExpansion = joinExpansion,
                    filter = conditionThunk
                )
            )
        }

        when (fromSource) {
            is PartiqlAst.FromSource.Scan -> {
                val thunk = compileAstExpr(fromSource.expr)
                addAliases(
                    thunk,
                    fromSource.asAlias?.text,
                    fromSource.atAlias?.text,
                    fromSource.byAlias?.text,
                    fromSource.metas
                )
            }
            is PartiqlAst.FromSource.Unpivot -> {
                val exprThunk = compileAstExpr(fromSource.expr)
                val thunk = thunkFactory.thunkEnv(metas) { env -> exprThunk(env).unpivot() }
                addAliases(
                    thunk,
                    fromSource.asAlias?.text,
                    fromSource.atAlias?.text,
                    fromSource.byAlias?.text,
                    fromSource.metas
                )
            }
            is PartiqlAst.FromSource.Join -> {
                val joinOp = fromSource.type
                val left = fromSource.left
                val right = fromSource.right
                val condition = fromSource.predicate

                val leftSources = compileFromSources(left)
                sources.addAll(leftSources)

                val joinExpansionInner = when (joinOp) {
                    is PartiqlAst.JoinType.Inner -> JoinExpansion.INNER
                    is PartiqlAst.JoinType.Left -> JoinExpansion.OUTER
                    is PartiqlAst.JoinType.Right,
                    is PartiqlAst.JoinType.Full ->
                        err(
                            "RIGHT and FULL JOIN not supported",
                            ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET,
                            errorContextFrom(metas).also {
                                it[Property.FEATURE_NAME] = "RIGHT and FULL JOIN"
                            },
                            internal = false
                        )
                }
                val conditionThunkInner = condition?.let { compileAstExpr(it) }
                    ?: compileAstExpr(PartiqlAst.build { lit(ionBool(true)) })

                // The right side of a FromSourceJoin can never be another FromSourceJoin -- the parser will currently
                // never construct an AST in that fashion.

                // TODO:  do we need to modify the AST to include this enforce this as a constraint?

                // How to modify the ast so it properly constrains the right side
                // of FromSourceJoin?

                // This means that the call to compileFromSources below can only ever yield one
                // additional FromClauseSource, which must contain the JoinExpansion and join
                // condition of this current node, so here we pass those down to the next level
                // of recursion.
                compileFromSources(right, sources, joinExpansionInner, conditionThunkInner)
            }
        }

        return sources
    }

    private fun compileLetSources(letSource: PartiqlAst.Let): List<CompiledLetSource> =
        letSource.letBindings.map {
            CompiledLetSource(name = it.name.text, thunk = compileAstExpr(it.expr))
        }

    /**
     * Compiles the clauses of the SELECT or PIVOT into a thunk that does not generate
     * the final projection.
     */
    private fun compileQueryWithoutProjection(
        ast: PartiqlAst.Expr.Select,
        compiledSources: List<CompiledFromSource>,
        compiledLetSources: List<CompiledLetSource>?
    ): (Environment) -> Sequence<FromProduction> {

        val localsBinder = compiledSources.map { it.alias }.localsBinder(valueFactory.missingValue)
        val whereThunk = ast.where?.let { compileAstExpr(it) }

        return { rootEnv ->
            val fromEnv = rootEnv.flipToGlobalsFirst()
            // compute the join over the data sources
            var seq = compiledSources
                .foldLeftProduct({ env: Environment -> env }) { bindEnv: (Environment) -> Environment, source: CompiledFromSource ->
                    fun correlatedBind(value: ExprValue): Pair<(Environment) -> Environment, ExprValue> {
                        // add the correlated binding environment thunk
                        val alias = source.alias
                        val nextBindEnv = { env: Environment ->
                            val childEnv = bindEnv(env)
                            childEnv.nest(
                                Bindings.buildLazyBindings {
                                    addBinding(alias.asName) { value }
                                    if (alias.atName != null)
                                        addBinding(alias.atName) {
                                            value.name ?: valueFactory.missingValue
                                        }
                                    if (alias.byName != null)
                                        addBinding(alias.byName) {
                                            value.address ?: valueFactory.missingValue
                                        }
                                },
                                Environment.CurrentMode.GLOBALS_THEN_LOCALS
                            )
                        }
                        return Pair(nextBindEnv, value)
                    }

                    var iter = source.thunk(bindEnv(fromEnv))
                        .rangeOver()
                        .asSequence()
                        .map { correlatedBind(it) }
                        .iterator()

                    val filter = source.filter
                    if (filter != null) {
                        // evaluate the ON-clause (before calculating the outer join NULL)
                        // TODO add facet for ExprValue to directly evaluate theta-joins
                        iter = iter
                            .asSequence()
                            .filter { (bindEnv: (Environment) -> Environment, _) ->
                                // make sure we operate with lexical scoping
                                val filterEnv = bindEnv(rootEnv).flipToLocals()
                                val filterResult = filter(filterEnv)
                                if (filterResult.isUnknown()) {
                                    false
                                } else {
                                    filterResult.booleanValue()
                                }
                            }
                            .iterator()
                    }

                    if (!iter.hasNext()) {
                        iter = when (source.joinExpansion) {
                            JoinExpansion.OUTER -> listOf(correlatedBind(valueFactory.nullValue)).iterator()
                            JoinExpansion.INNER -> iter
                        }
                    }

                    iter
                }
                .asSequence()
                .map { joinedValues ->
                    // bind the joined value to the bindings for the filter/project
                    FromProduction(joinedValues, fromEnv.nest(localsBinder.bindLocals(joinedValues)))
                }
            // Nest LET bindings in the FROM environment
            if (compiledLetSources != null) {
                seq = seq.map { fromProduction ->
                    val parentEnv = fromProduction.env

                    val letEnv: Environment = compiledLetSources.fold(parentEnv) { accEnvironment, curLetSource ->
                        val letValue = curLetSource.thunk(accEnvironment)
                        val binding = Bindings.over { bindingName ->
                            when {
                                bindingName.isEquivalentTo(curLetSource.name) -> letValue
                                else -> null
                            }
                        }
                        accEnvironment.nest(newLocals = binding)
                    }
                    fromProduction.copy(env = letEnv)
                }
            }
            if (whereThunk != null) {
                seq = seq.filter { (_, env) ->
                    val whereClauseResult = whereThunk(env)
                    when (whereClauseResult.isUnknown()) {
                        true -> false
                        false -> whereClauseResult.booleanValue()
                    }
                }
            }
            seq
        }
    }

    private fun compileSelectListToProjectionElements(
        selectList: PartiqlAst.Projection.ProjectList
    ): List<ProjectionElement> =
        selectList.projectItems.mapIndexed { idx, it ->
            when (it) {
                is PartiqlAst.ProjectItem.ProjectExpr -> {
                    val alias = it.asAlias?.text ?: it.expr.extractColumnAlias(idx)
                    val thunk = compileAstExpr(it.expr)
                    SingleProjectionElement(valueFactory.newString(alias), thunk)
                }
                is PartiqlAst.ProjectItem.ProjectAll -> {
                    MultipleProjectionElement(listOf(compileAstExpr(it.expr)))
                }
            }
        }

    private fun compilePath(expr: PartiqlAst.Expr.Path, metas: MetaContainer): ThunkEnv {
        val rootThunk = compileAstExpr(expr.root)
        val remainingComponents = LinkedList<PartiqlAst.PathStep>()

        expr.steps.forEach { remainingComponents.addLast(it) }

        val componentThunk = compilePathComponents(remainingComponents, metas)

        return thunkFactory.thunkEnv(metas) { env ->
            val rootValue = rootThunk(env)
            componentThunk(env, rootValue)
        }
    }

    private fun compilePathComponents(
        remainingComponents: LinkedList<PartiqlAst.PathStep>,
        pathMetas: MetaContainer
    ): ThunkEnvValue<ExprValue> {

        val componentThunks = ArrayList<ThunkEnvValue<ExprValue>>()

        while (!remainingComponents.isEmpty()) {
            val pathComponent = remainingComponents.removeFirst()
            val componentMetas = pathComponent.metas
            componentThunks.add(
                when (pathComponent) {
                    is PartiqlAst.PathStep.PathExpr -> {
                        val indexExpr = pathComponent.index
                        val caseSensitivity = pathComponent.case
                        when {
                            // If indexExpr is a literal string, there is no need to evaluate it--just compile a
                            // thunk that directly returns a bound value
                            indexExpr is PartiqlAst.Expr.Lit && indexExpr.value.toIonValue(valueFactory.ion) is IonString -> {
                                val lookupName = BindingName(
                                    indexExpr.value.toIonValue(valueFactory.ion).stringValue()!!,
                                    caseSensitivity.toBindingCase()
                                )
                                thunkFactory.thunkEnvValue(componentMetas) { _, componentValue ->
                                    componentValue.bindings[lookupName] ?: valueFactory.missingValue
                                }
                            }
                            else -> {
                                val indexThunk = compileAstExpr(indexExpr)
                                thunkFactory.thunkEnvValue(componentMetas) { env, componentValue ->
                                    val indexValue = indexThunk(env)
                                    when {
                                        indexValue.type == ExprValueType.INT -> {
                                            componentValue.ordinalBindings[indexValue.numberValue().toInt()]
                                        }
                                        indexValue.type.isText -> {
                                            val lookupName =
                                                BindingName(indexValue.stringValue(), caseSensitivity.toBindingCase())
                                            componentValue.bindings[lookupName]
                                        }
                                        else -> {
                                            when (compileOptions.typingMode) {
                                                TypingMode.LEGACY -> err(
                                                    "Cannot convert index to int/string: $indexValue",
                                                    ErrorCode.EVALUATOR_INVALID_CONVERSION,
                                                    errorContextFrom(componentMetas),
                                                    internal = false
                                                )
                                                TypingMode.PERMISSIVE -> valueFactory.missingValue
                                            }
                                        }
                                    } ?: valueFactory.missingValue
                                }
                            }
                        }
                    }
                    is PartiqlAst.PathStep.PathUnpivot -> {
                        when {
                            !remainingComponents.isEmpty() -> {
                                val tempThunk = compilePathComponents(remainingComponents, pathMetas)
                                thunkFactory.thunkEnvValue(componentMetas) { env, componentValue ->
                                    val mapped = componentValue.unpivot()
                                        .flatMap { tempThunk(env, it).rangeOver() }
                                        .asSequence()
                                    valueFactory.newBag(mapped)
                                }
                            }
                            else ->
                                thunkFactory.thunkEnvValue(componentMetas) { _, componentValue ->
                                    valueFactory.newBag(componentValue.unpivot().asSequence())
                                }
                        }
                    }
                    // this is for `path[*].component`
                    is PartiqlAst.PathStep.PathWildcard -> {
                        when {
                            !remainingComponents.isEmpty() -> {
                                val hasMoreWildCards =
                                    remainingComponents.filterIsInstance<PartiqlAst.PathStep.PathWildcard>().any()
                                val tempThunk = compilePathComponents(remainingComponents, pathMetas)

                                when {
                                    !hasMoreWildCards -> thunkFactory.thunkEnvValue(componentMetas) { env, componentValue ->
                                        val mapped = componentValue
                                            .rangeOver()
                                            .map { tempThunk(env, it) }
                                            .asSequence()

                                        valueFactory.newBag(mapped)
                                    }
                                    else -> thunkFactory.thunkEnvValue(componentMetas) { env, componentValue ->
                                        val mapped = componentValue
                                            .rangeOver()
                                            .flatMap {
                                                val tempValue = tempThunk(env, it)
                                                tempValue
                                            }
                                            .asSequence()

                                        valueFactory.newBag(mapped)
                                    }
                                }
                            }
                            else -> {
                                thunkFactory.thunkEnvValue(componentMetas) { _, componentValue ->
                                    val mapped = componentValue.rangeOver().asSequence()
                                    valueFactory.newBag(mapped)
                                }
                            }
                        }
                    }
                }
            )
        }
        return when (componentThunks.size) {
            1 -> componentThunks.first()
            else -> thunkFactory.thunkEnvValue(pathMetas) { env, rootValue ->
                componentThunks.fold(rootValue) { componentValue, componentThunk ->
                    componentThunk(env, componentValue)
                }
            }
        }
    }

    /**
     * Given an AST node that represents a `LIKE` predicate, return an ExprThunk that evaluates a `LIKE` predicate.
     *
     * Three cases
     *
     * 1. All arguments are literals, then compile and run the pattern
     * 1. Search pattern and escape pattern are literals, compile the pattern. Running the pattern deferred to evaluation time.
     * 1. Pattern or escape (or both) are *not* literals, compile and running of pattern deferred to evaluation time.
     *
     * ```
     * <valueExpr> LIKE <patternExpr> [ESCAPE <escapeExpr>]
     * ```
     *
     * @return a thunk that when provided with an environment evaluates the `LIKE` predicate
     */
    private fun compileLike(expr: PartiqlAst.Expr.Like, metas: MetaContainer): ThunkEnv {
        val valueExpr = expr.value
        val patternExpr = expr.pattern
        val escapeExpr = expr.escape

        val patternLocationMeta = patternExpr.metas.sourceLocation
        val escapeLocationMeta = escapeExpr?.metas?.sourceLocation

        // This is so that null short-circuits can be supported.
        fun getRegexPattern(pattern: ExprValue, escape: ExprValue?): (() -> Pattern)? {
            val patternArgs = listOfNotNull(pattern, escape)
            when {
                patternArgs.any { it.type.isUnknown } -> return null
                patternArgs.any { !it.type.isText } -> return {
                    err(
                        "LIKE expression must be given non-null strings as input",
                        ErrorCode.EVALUATOR_LIKE_INVALID_INPUTS,
                        errorContextFrom(metas).also {
                            it[Property.LIKE_PATTERN] = pattern.ionValue.toString()
                            if (escape != null) it[Property.LIKE_ESCAPE] = escape.ionValue.toString()
                        },
                        internal = false
                    )
                }
                else -> {
                    val (patternString: String, escapeChar: Int?) =
                        checkPattern(pattern.ionValue, patternLocationMeta, escape?.ionValue, escapeLocationMeta)
                    val likeRegexPattern = when {
                        patternString.isEmpty() -> Pattern.compile("")
                        else -> parsePattern(patternString, escapeChar)
                    }
                    return { likeRegexPattern }
                }
            }
        }

        fun matchRegexPattern(value: ExprValue, likePattern: (() -> Pattern)?): ExprValue {
            return when {
                likePattern == null || value.type.isUnknown -> valueFactory.nullValue
                !value.type.isText -> err(
                    "LIKE expression must be given non-null strings as input",
                    ErrorCode.EVALUATOR_LIKE_INVALID_INPUTS,
                    errorContextFrom(metas).also {
                        it[Property.LIKE_VALUE] = value.ionValue.toString()
                    },
                    internal = false
                )
                else -> valueFactory.newBoolean(likePattern().matcher(value.stringValue()).matches())
            }
        }

        val valueThunk = compileAstExpr(valueExpr)

        // If the pattern and escape expressions are literals then we can compile the pattern now and
        // re-use it with every execution. Otherwise, we must re-compile the pattern every time.
        return when {
            patternExpr is PartiqlAst.Expr.Lit && (escapeExpr == null || escapeExpr is PartiqlAst.Expr.Lit) -> {
                val patternParts = getRegexPattern(
                    valueFactory.newFromIonValue(patternExpr.value.toIonValue(valueFactory.ion)),
                    (escapeExpr as? PartiqlAst.Expr.Lit)?.value?.toIonValue(valueFactory.ion)
                        ?.let { valueFactory.newFromIonValue(it) }
                )

                // If valueExpr is also a literal then we can evaluate this at compile time and return a constant.
                if (valueExpr is PartiqlAst.Expr.Lit) {
                    val resultValue = matchRegexPattern(
                        valueFactory.newFromIonValue(valueExpr.value.toIonValue(valueFactory.ion)),
                        patternParts
                    )
                    return thunkFactory.thunkEnv(metas) { resultValue }
                } else {
                    thunkFactory.thunkEnvOperands(metas, valueThunk) { _, value ->
                        matchRegexPattern(value, patternParts)
                    }
                }
            }
            else -> {
                val patternThunk = compileAstExpr(patternExpr)
                when (escapeExpr) {
                    null -> {
                        // thunk that re-compiles the DFA every evaluation without a custom escape sequence
                        thunkFactory.thunkEnvOperands(metas, valueThunk, patternThunk) { _, value, pattern ->
                            val pps = getRegexPattern(pattern, null)
                            matchRegexPattern(value, pps)
                        }
                    }
                    else -> {
                        // thunk that re-compiles the pattern every evaluation but *with* a custom escape sequence
                        val escapeThunk = compileAstExpr(escapeExpr)
                        thunkFactory.thunkEnvOperands(
                            metas,
                            valueThunk,
                            patternThunk,
                            escapeThunk
                        ) { _, value, pattern, escape ->
                            val pps = getRegexPattern(pattern, escape)
                            matchRegexPattern(value, pps)
                        }
                    }
                }
            }
        }
    }

    /**
     * Given the pattern and optional escape character in a `LIKE` predicate as [IonValue]s
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
    private fun checkPattern(
        pattern: IonValue,
        patternLocationMeta: SourceLocationMeta?,
        escape: IonValue?,
        escapeLocationMeta: SourceLocationMeta?
    ): Pair<String, Int?> {

        val patternString = pattern.stringValue()
            ?: err(
                "Must provide a non-null value for PATTERN in a LIKE predicate: $pattern",
                ErrorCode.EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE,
                errorContextFrom(patternLocationMeta),
                internal = false
            )

        escape?.let {
            val escapeCharString = checkEscapeChar(escape, escapeLocationMeta)
            val escapeCharCodePoint = escapeCharString.codePointAt(0) // escape is a string of length 1
            val validEscapedChars = setOf('_'.toInt(), '%'.toInt(), escapeCharCodePoint)
            val iter = patternString.codePointSequence().iterator()

            while (iter.hasNext()) {
                val current = iter.next()
                if (current == escapeCharCodePoint && (!iter.hasNext() || !validEscapedChars.contains(iter.next()))) {
                    err(
                        "Invalid escape sequence : $patternString",
                        ErrorCode.EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE,
                        errorContextFrom(patternLocationMeta).apply {
                            set(Property.LIKE_PATTERN, patternString)
                            set(Property.LIKE_ESCAPE, escapeCharString)
                        },
                        internal = false
                    )
                }
            }
            return Pair(patternString, escapeCharCodePoint)
        }
        return Pair(patternString, null)
    }

    /**
     * Given an [IonValue] to be used as the escape character in a `LIKE` predicate check that it is
     * a valid character based on the SQL Spec.
     *
     *
     * A value is a valid escape when
     * 1. it is 1 character long, and,
     * 1. Cannot be null (SQL92 spec marks this cases as *unknown*)
     *
     * @param escape value provided as an escape character for a `LIKE` predicate
     *
     * @return the escape character as a [String] or throws an exception when the input is invalid
     */
    private fun checkEscapeChar(escape: IonValue, locationMeta: SourceLocationMeta?): String {
        val escapeChar = escape.stringValue() ?: err(
            "Must provide a value when using ESCAPE in a LIKE predicate: $escape",
            ErrorCode.EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE,
            errorContextFrom(locationMeta),
            internal = false
        )
        when (escapeChar) {
            "" -> {
                err(
                    "Cannot use empty character as ESCAPE character in a LIKE predicate: $escape",
                    ErrorCode.EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE,
                    errorContextFrom(locationMeta),
                    internal = false
                )
            }
            else -> {
                if (escapeChar.trim().length != 1) {
                    err(
                        "Escape character must have size 1 : $escapeChar",
                        ErrorCode.EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE,
                        errorContextFrom(locationMeta),
                        internal = false
                    )
                }
            }
        }
        return escapeChar
    }

    private fun compileDdl(node: PartiqlAst.Statement.Ddl): ThunkEnv =
        { _ ->
            err(
                "DDL operations are not supported yet",
                ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET,
                errorContextFrom(node.metas).also {
                    it[Property.FEATURE_NAME] = "DDL Operations"
                },
                internal = false
            )
        }

    private fun compileDml(node: PartiqlAst.Statement.Dml): ThunkEnv =
        { _ ->
            err(
                "DML operations are not supported yet",
                ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET,
                errorContextFrom(node.metas).also {
                    it[Property.FEATURE_NAME] = "DML Operations"
                },
                internal = false
            )
        }

    private fun compileExec(node: PartiqlAst.Statement.Exec): ThunkEnv {
        val metas = node.metas
        val procedureName = node.procedureName.text
        val procedure = procedures[procedureName] ?: err(
            "No such stored procedure: $procedureName",
            ErrorCode.EVALUATOR_NO_SUCH_PROCEDURE,
            errorContextFrom(metas).also {
                it[Property.PROCEDURE_NAME] = procedureName
            },
            internal = false
        )

        val args = node.args
        // Check arity
        if (args.size !in procedure.signature.arity) {
            val errorContext = errorContextFrom(metas).also {
                it[Property.EXPECTED_ARITY_MIN] = procedure.signature.arity.first
                it[Property.EXPECTED_ARITY_MAX] = procedure.signature.arity.last
            }

            val message = when {
                procedure.signature.arity.first == 1 && procedure.signature.arity.last == 1 ->
                    "${procedure.signature.name} takes a single argument, received: ${args.size}"
                procedure.signature.arity.first == procedure.signature.arity.last ->
                    "${procedure.signature.name} takes exactly ${procedure.signature.arity.first} arguments, received: ${args.size}"
                else ->
                    "${procedure.signature.name} takes between ${procedure.signature.arity.first} and " +
                        "${procedure.signature.arity.last} arguments, received: ${args.size}"
            }

            err(
                message,
                ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_PROCEDURE_CALL,
                errorContext,
                internal = false
            )
        }

        // Compile the procedure's arguments
        val argThunks = compileAstExprs(args)

        return thunkFactory.thunkEnv(metas) { env ->
            val procedureArgValues = argThunks.map { it(env) }
            procedure.call(env.session, procedureArgValues)
        }
    }

    private fun compileDate(expr: PartiqlAst.Expr.Date, metas: MetaContainer): ThunkEnv =
        thunkFactory.thunkEnv(metas) {
            valueFactory.newDate(
                expr.year.value.toInt(),
                expr.month.value.toInt(),
                expr.day.value.toInt()
            )
        }

    private fun compileLitTime(expr: PartiqlAst.Expr.LitTime, metas: MetaContainer): ThunkEnv =
        thunkFactory.thunkEnv(metas) {
            // Add the default time zone if the type "TIME WITH TIME ZONE" does not have an explicitly specified time zone.
            valueFactory.newTime(
                Time.of(
                    expr.value.hour.value.toInt(),
                    expr.value.minute.value.toInt(),
                    expr.value.second.value.toInt(),
                    expr.value.nano.value.toInt(),
                    expr.value.precision.value.toInt(),
                    if (expr.value.withTimeZone.value && expr.value.tzMinutes == null) compileOptions.defaultTimezoneOffset.totalMinutes else expr.value.tzMinutes?.value?.toInt()
                )
            )
        }

    /** A special wrapper for `UNPIVOT` values as a BAG. */
    private class UnpivotedExprValue(private val values: Iterable<ExprValue>) : BaseExprValue() {
        override val type = ExprValueType.BAG
        override fun iterator() = values.iterator()

        // XXX this value is only ever produced in a FROM iteration, thus none of these should ever be called
        override val ionValue
            get() = throw UnsupportedOperationException("Synthetic value cannot provide ion value")
    }

    /** Unpivots a `struct`, and synthesizes a synthetic singleton `struct` for other [ExprValue]. */
    internal fun ExprValue.unpivot(): ExprValue = when {
        // special case for our special UNPIVOT value to avoid double wrapping
        this is UnpivotedExprValue -> this
        // Wrap into a pseudo-BAG
        type == ExprValueType.STRUCT || type == ExprValueType.MISSING -> UnpivotedExprValue(this)
        // for non-struct, this wraps any value into a BAG with a synthetic name
        else -> UnpivotedExprValue(
            listOf(
                this.namedValue(valueFactory.newString(syntheticColumnName(0)))
            )
        )
    }

    private fun createStructExprValue(seq: Sequence<ExprValue>, ordering: StructOrdering) =
        valueFactory.newStruct(
            when (compileOptions.projectionIteration) {
                ProjectionIterationBehavior.FILTER_MISSING -> seq.filter { it.type != ExprValueType.MISSING }
                ProjectionIterationBehavior.UNFILTERED -> seq
            },
            ordering
        )

    /** Helper to convert [PartiqlAst.Type] in AST to a [TypedOpParameter]. */
    private fun PartiqlAst.Type.toTypedOpParameter(): TypedOpParameter {
        // hack: to avoid duplicating the function `PartiqlAst.Type.toTypedOpParameter`, we have to convert this
        // PartiqlAst.Type to PartiqlPhysical.Type. The easiest way to do that without using a visitor transform
        // (which is overkill and comes with some downsides for something this simple), is to transform to and from
        // s-expressions again.  This will work without difficulty as long as PartiqlAst.Type remains unchanged in all
        // permuted domains between PartiqlAst and PartiqlPhysical.

        // This is really just a temporary measure, however, which must exist for as long as the type inferencer works only
        // on PartiqlAst.  When it has been migrated to use PartiqlPhysical instead, there should no longer be a reason
        // to keep this function around.
        val sexp = this.toIonElement()
        val physicalType = PartiqlPhysical.transform(sexp) as PartiqlPhysical.Type
        return physicalType.toTypedOpParameter(customTypedOpParameters)
    }
}

/**
 * Contains data about a compiled from source, including its [Alias], [thunk],
 * type of [JoinExpansion] ([JoinExpansion.INNER] for single tables or `CROSS JOIN`S.) and [filter] criteria.
 */
private data class CompiledFromSource(
    val alias: Alias,
    val thunk: ThunkEnv,
    val joinExpansion: JoinExpansion,
    val filter: ThunkEnv?
)

/**
 * Represents a single `FROM` source production of values.
 *
 * @param values A single production of values from the `FROM` source.
 * @param env The environment scoped to the values of this production.
 */
private data class FromProduction(
    val values: List<ExprValue>,
    val env: Environment
)

/** Specifies the expansion for joins. */
private enum class JoinExpansion {
    /** Default for non-joined values, CROSS and INNER JOIN. */
    INNER,

    /** Expansion mode for LEFT/RIGHT/FULL JOIN. */
    OUTER
}

private data class CompiledLetSource(
    val name: String,
    val thunk: ThunkEnv
)

private enum class ExpressionContext {
    /**
     * Indicates that the compiler is compiling a normal expression (i.e. not one of the other
     * contexts).
     */
    NORMAL,

    /**
     * Indicates that the compiler is compiling an expression in a select list.
     */
    SELECT_LIST,

    /**
     * Indicates that the compiler is compiling an expression that is the argument to an aggregate function.
     */
    AGG_ARG
}

/**
 * Tracks state used by the compiler while compiling.
 *
 * @param expressionContext Indicates what part of the grammar is currently being compiled.
 * @param fromSourceNames Set of all FROM source aliases for binding error checks.
 */
private class CompilationContext(val expressionContext: ExpressionContext, val fromSourceNames: Set<String>) {
    fun createNested(expressionContext: ExpressionContext, fromSourceNames: Set<String>) =
        CompilationContext(expressionContext, fromSourceNames)
}

/**
 * Represents an element in a select list that is to be projected into the final result.
 * i.e. an expression, or a (project_all) node.
 */
private sealed class ProjectionElement

/**
 * Represents a single compiled expression to be projected into the final result.
 * Given `SELECT a + b as value FROM foo`:
 * - `name` is "value"
 * - `thunk` is compiled expression, i.e. `a + b`
 */
private class SingleProjectionElement(val name: ExprValue, val thunk: ThunkEnv) : ProjectionElement()

/**
 * Represents a wildcard ((path_project_all) node) expression to be projected into the final result.
 * This covers two cases.  For `SELECT foo.* FROM foo`, `exprThunks` contains a single compiled expression
 * `foo`.
 *
 * For `SELECT * FROM foo, bar, bat`, `exprThunks` would contain a compiled expression for each of `foo`, `bar` and
 * `bat`.
 */
private class MultipleProjectionElement(val thunks: List<ThunkEnv>) : ProjectionElement()

internal val MetaContainer.sourceLocationMeta get() = this[SourceLocationMeta.TAG] as? SourceLocationMeta

private fun StaticType.getTypes() = when (val flattened = this.flatten()) {
    is AnyOfType -> flattened.types
    else -> listOf(this)
}
