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

package org.partiql.lang.eval.physical

import com.amazon.ion.IonString
import com.amazon.ion.IonValue
import com.amazon.ion.Timestamp
import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.emptyMetaContainer
import com.amazon.ionelement.api.toIonValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.withIndex
import org.partiql.errors.ErrorCode
import org.partiql.errors.Property
import org.partiql.errors.PropertyValueMap
import org.partiql.lang.ast.IsOrderedMeta
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.sourceLocation
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.domains.staticType
import org.partiql.lang.domains.toBindingCase
import org.partiql.lang.eval.AnyOfCastTable
import org.partiql.lang.eval.ArityMismatchException
import org.partiql.lang.eval.BaseExprValue
import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.CastFunc
import org.partiql.lang.eval.DEFAULT_COMPARATOR
import org.partiql.lang.eval.ErrorDetails
import org.partiql.lang.eval.EvaluationException
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueBagOp
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.Expression
import org.partiql.lang.eval.ExpressionAsync
import org.partiql.lang.eval.FunctionNotFoundException
import org.partiql.lang.eval.Named
import org.partiql.lang.eval.PartiQLResult
import org.partiql.lang.eval.ProjectionIterationBehavior
import org.partiql.lang.eval.StructOrdering
import org.partiql.lang.eval.ThunkValueAsync
import org.partiql.lang.eval.TypedOpBehavior
import org.partiql.lang.eval.TypingMode
import org.partiql.lang.eval.booleanValue
import org.partiql.lang.eval.builtins.storedprocedure.StoredProcedure
import org.partiql.lang.eval.call
import org.partiql.lang.eval.cast
import org.partiql.lang.eval.compareTo
import org.partiql.lang.eval.createErrorSignaler
import org.partiql.lang.eval.createThunkFactoryAsync
import org.partiql.lang.eval.distinct
import org.partiql.lang.eval.err
import org.partiql.lang.eval.errorContextFrom
import org.partiql.lang.eval.errorIf
import org.partiql.lang.eval.exprEquals
import org.partiql.lang.eval.fillErrorContext
import org.partiql.lang.eval.impl.FunctionManager
import org.partiql.lang.eval.isNotUnknown
import org.partiql.lang.eval.isUnknown
import org.partiql.lang.eval.like.parsePattern
import org.partiql.lang.eval.longValue
import org.partiql.lang.eval.namedValue
import org.partiql.lang.eval.numberValue
import org.partiql.lang.eval.rangeOver
import org.partiql.lang.eval.relation.RelationType
import org.partiql.lang.eval.sourceLocationMeta
import org.partiql.lang.eval.stringValue
import org.partiql.lang.eval.syntheticColumnName
import org.partiql.lang.eval.time.Time
import org.partiql.lang.eval.timestampValue
import org.partiql.lang.eval.unnamedValue
import org.partiql.lang.planner.EvaluatorOptions
import org.partiql.lang.types.StaticTypeUtils.getRuntimeType
import org.partiql.lang.types.StaticTypeUtils.isInstance
import org.partiql.lang.types.StaticTypeUtils.staticTypeFromExprValue
import org.partiql.lang.types.TypedOpParameter
import org.partiql.lang.types.UnknownArguments
import org.partiql.lang.types.toTypedOpParameter
import org.partiql.lang.util.checkThreadInterrupted
import org.partiql.lang.util.codePointSequence
import org.partiql.lang.util.div
import org.partiql.lang.util.exprValue
import org.partiql.lang.util.isZero
import org.partiql.lang.util.minus
import org.partiql.lang.util.plus
import org.partiql.lang.util.rem
import org.partiql.lang.util.stringValue
import org.partiql.lang.util.times
import org.partiql.lang.util.toIntExact
import org.partiql.lang.util.totalMinutes
import org.partiql.lang.util.unaryMinus
import org.partiql.types.AnyOfType
import org.partiql.types.AnyType
import org.partiql.types.IntType
import org.partiql.types.SingleType
import org.partiql.types.StaticType
import org.partiql.types.UnsupportedTypeCheckException
import java.util.LinkedList
import java.util.TreeSet
import java.util.regex.Pattern

/**
 * A basic "compiler" that converts an instance of [PartiqlPhysical.Expr] to an [Expression].
 *
 * This is a modified copy of the legacy `EvaluatingCompiler` class, which is now legacy.
 * The primary differences between this class an `EvaluatingCompiler` are:
 *
 * - All references to `PartiqlPhysical` are replaced with `PartiqlPhysical`.
 * - `EvaluatingCompiler` compiles "monolithic" SFW queries--this class compiles relational
 * operators (in concert with [PhysicalBexprToThunkConverter]).
 *
 * This implementation produces a "compiled" form consisting of context-threaded
 * code in the form of a tree of [PhysicalPlanThunkAsync]s.  An overview of this technique can be found
 * [here][1].
 *
 * **Note:** *threaded* in this context is used in how the code gets *threaded* together for
 * interpretation and **not** the concurrency primitive. That is to say this code is NOT thread
 * safe.
 *
 * [1]: https://www.complang.tuwien.ac.at/anton/lvas/sem06w/fest.pdf
 */
internal class PhysicalPlanCompilerAsyncImpl(
    private val functions: List<ExprFunction>,
    private val customTypedOpParameters: Map<String, TypedOpParameter>,
    private val procedures: Map<String, StoredProcedure>,
    private val evaluatorOptions: EvaluatorOptions = EvaluatorOptions.standard(),
    private val bexperConverter: PhysicalBexprToThunkConverterAsync,
) : PhysicalPlanCompilerAsync {
    @Deprecated("Use constructor with List<ExprFunction> instead", level = DeprecationLevel.WARNING)
    constructor(
        functions: Map<String, ExprFunction>,
        customTypedOpParameters: Map<String, TypedOpParameter>,
        procedures: Map<String, StoredProcedure>,
        evaluatorOptions: EvaluatorOptions = EvaluatorOptions.standard(),
        bexperConverter: PhysicalBexprToThunkConverterAsync
    ) : this(
        functions = functions.values.toList(),
        customTypedOpParameters = customTypedOpParameters,
        procedures = procedures,
        evaluatorOptions = evaluatorOptions,
        bexperConverter = bexperConverter
    )

    // TODO: remove this once we migrate from `IonValue` to `IonElement`.
    private val ion = IonSystemBuilder.standard().build()

    private val errorSignaler = evaluatorOptions.typingMode.createErrorSignaler()
    private val thunkFactory = evaluatorOptions.typingMode.createThunkFactoryAsync<EvaluatorState>(evaluatorOptions.thunkOptions)

    private val functionManager = FunctionManager(functions)

    private fun Boolean.exprValue(): ExprValue = ExprValue.newBoolean(this)
    private fun String.exprValue(): ExprValue = ExprValue.newString(this)

    /**
     * Compiles a [PartiqlPhysical.Statement] tree to an [ExpressionAsync].
     *
     * Checks [Thread.interrupted] before every expression and sub-expression is compiled
     * and throws [InterruptedException] if [Thread.interrupted] it has been set in the
     * hope that long-running compilations may be aborted by the caller.
     */
    suspend fun compile(plan: PartiqlPhysical.Plan): ExpressionAsync {
        val thunk = compileAstStatement(plan.stmt)

        return object : ExpressionAsync {
            override suspend fun eval(session: EvaluationSession): PartiQLResult {
                val env = EvaluatorState(
                    session = session,
                    registers = Array(plan.locals.size) { ExprValue.missingValue }
                )
                val value = thunk(env)
                return PartiQLResult.Value(value = value)
            }
        }
    }

    /**
     * Compiles a [PartiqlPhysical.Expr] tree to an [ExpressionAsync].
     *
     * Checks [Thread.interrupted] before every expression and sub-expression is compiled
     * and throws [InterruptedException] if [Thread.interrupted] it has been set in the
     * hope that long-running compilations may be aborted by the caller.
     */
    internal suspend fun compile(expr: PartiqlPhysical.Expr, localsSize: Int): ExpressionAsync {
        val thunk = compileAstExpr(expr)

        return object : ExpressionAsync {
            override suspend fun eval(session: EvaluationSession): PartiQLResult {
                val env = EvaluatorState(
                    session = session,
                    registers = Array(localsSize) { ExprValue.missingValue }
                )
                val value = thunk(env)
                return PartiQLResult.Value(value = value)
            }
        }
    }

    override suspend fun convert(expr: PartiqlPhysical.Expr): PhysicalPlanThunkAsync = this.compileAstExpr(expr)

    /**
     * Compiles the specified [PartiqlPhysical.Statement] into a [PhysicalPlanThunkAsync].
     *
     * This function will [InterruptedException] if [Thread.interrupted] has been set.
     */
    private suspend fun compileAstStatement(ast: PartiqlPhysical.Statement): PhysicalPlanThunkAsync {
        return when (ast) {
            is PartiqlPhysical.Statement.Query -> compileAstExpr(ast.expr)
            is PartiqlPhysical.Statement.Exec -> compileExec(ast)
            is PartiqlPhysical.Statement.Dml,
            is PartiqlPhysical.Statement.Explain -> {
                val value = ExprValue.newBoolean(true)
                thunkFactory.thunkEnvAsync(emptyMetaContainer()) { value }
            }
        }
    }

    private suspend fun compileAstExpr(expr: PartiqlPhysical.Expr): PhysicalPlanThunkAsync {
        checkThreadInterrupted()
        val metas = expr.metas

        return when (expr) {
            is PartiqlPhysical.Expr.Lit -> compileLit(expr, metas)
            is PartiqlPhysical.Expr.Missing -> compileMissing(metas)
            is PartiqlPhysical.Expr.LocalId -> compileLocalId(expr, metas)
            is PartiqlPhysical.Expr.GlobalId -> compileGlobalId(expr)
            is PartiqlPhysical.Expr.SimpleCase -> compileSimpleCase(expr, metas)
            is PartiqlPhysical.Expr.SearchedCase -> compileSearchedCase(expr, metas)
            is PartiqlPhysical.Expr.Path -> compilePath(expr, metas)
            is PartiqlPhysical.Expr.Struct -> compileStruct(expr)
            is PartiqlPhysical.Expr.Parameter -> compileParameter(expr, metas)
            is PartiqlPhysical.Expr.Date -> compileDate(expr, metas)
            is PartiqlPhysical.Expr.LitTime -> compileLitTime(expr, metas)

            // arithmetic operations
            is PartiqlPhysical.Expr.Plus -> compilePlus(expr, metas)
            is PartiqlPhysical.Expr.Times -> compileTimes(expr, metas)
            is PartiqlPhysical.Expr.Minus -> compileMinus(expr, metas)
            is PartiqlPhysical.Expr.Divide -> compileDivide(expr, metas)
            is PartiqlPhysical.Expr.Modulo -> compileModulo(expr, metas)
            is PartiqlPhysical.Expr.BitwiseAnd -> compileBitwiseAnd(expr, metas)

            // comparison operators
            is PartiqlPhysical.Expr.And -> compileAnd(expr, metas)
            is PartiqlPhysical.Expr.Between -> compileBetween(expr, metas)
            is PartiqlPhysical.Expr.Eq -> compileEq(expr, metas)
            is PartiqlPhysical.Expr.Gt -> compileGt(expr, metas)
            is PartiqlPhysical.Expr.Gte -> compileGte(expr, metas)
            is PartiqlPhysical.Expr.Lt -> compileLt(expr, metas)
            is PartiqlPhysical.Expr.Lte -> compileLte(expr, metas)
            is PartiqlPhysical.Expr.Like -> compileLike(expr, metas)
            is PartiqlPhysical.Expr.InCollection -> compileIn(expr, metas)

            // logical operators
            is PartiqlPhysical.Expr.Ne -> compileNe(expr, metas)
            is PartiqlPhysical.Expr.Or -> compileOr(expr, metas)

            // unary
            is PartiqlPhysical.Expr.Not -> compileNot(expr, metas)
            is PartiqlPhysical.Expr.Pos -> compilePos(expr, metas)
            is PartiqlPhysical.Expr.Neg -> compileNeg(expr, metas)

            // other operators
            is PartiqlPhysical.Expr.Concat -> compileConcat(expr, metas)
            is PartiqlPhysical.Expr.Call -> compileCall(expr, metas)
            is PartiqlPhysical.Expr.NullIf -> compileNullIf(expr, metas)
            is PartiqlPhysical.Expr.Coalesce -> compileCoalesce(expr, metas)

            // "typed" operators (RHS is a data type and not an expression)
            is PartiqlPhysical.Expr.Cast -> compileCast(expr, metas)
            is PartiqlPhysical.Expr.IsType -> compileIs(expr, metas)
            is PartiqlPhysical.Expr.CanCast -> compileCanCast(expr, metas)
            is PartiqlPhysical.Expr.CanLosslessCast -> compileCanLosslessCast(expr, metas)

            // sequence constructors
            is PartiqlPhysical.Expr.List -> compileSeq(ExprValueType.LIST, expr.values, metas)
            is PartiqlPhysical.Expr.Sexp -> compileSeq(ExprValueType.SEXP, expr.values, metas)
            is PartiqlPhysical.Expr.Bag -> compileSeq(ExprValueType.BAG, expr.values, metas)

            // bag operators
            is PartiqlPhysical.Expr.BagOp -> compileBagOp(expr, metas)
            is PartiqlPhysical.Expr.BindingsToValues -> compileBindingsToValues(expr)
            is PartiqlPhysical.Expr.Pivot -> compilePivot(expr, metas)
            is PartiqlPhysical.Expr.GraphMatch -> TODO("Physical compilation of GraphMatch expression")
            is PartiqlPhysical.Expr.Timestamp -> TODO()
        }
    }

    private suspend fun compileBindingsToValues(expr: PartiqlPhysical.Expr.BindingsToValues): PhysicalPlanThunkAsync {
        val mapThunk = compileAstExpr(expr.exp)
        val bexprThunk: RelationThunkEnvAsync = bexperConverter.convert(expr.query)

        val relationType = when (expr.metas.containsKey(IsOrderedMeta.tag)) {
            true -> RelationType.LIST
            false -> RelationType.BAG
        }

        return thunkFactory.thunkEnvAsync(expr.metas) { env ->
            // we create a snapshot for currentRegister to use during the evaluation
            // this is to avoid issue when iterator planner result
            val currentRegister = env.registers.clone()
            val elements: Flow<ExprValue> = flow {
                env.load(currentRegister)
                val relItr = bexprThunk(env)
                while (relItr.nextRow()) {
                    emit(mapThunk(env))
                }
            }
            when (relationType) {
                RelationType.LIST -> ExprValue.newList(elements.toList())
                RelationType.BAG -> ExprValue.newBag(elements.toList())
            }
        }
    }

    private suspend fun compileAstExprs(args: List<PartiqlPhysical.Expr>) = args.map { compileAstExpr(it) }

    private suspend fun compileNullIf(expr: PartiqlPhysical.Expr.NullIf, metas: MetaContainer): PhysicalPlanThunkAsync {
        val expr1Thunk = compileAstExpr(expr.expr1)
        val expr2Thunk = compileAstExpr(expr.expr2)

        // Note: NULLIF does not propagate the unknown values and .exprEquals  provides the correct semantics.
        return thunkFactory.thunkEnvAsync(metas) { env ->
            val expr1Value = expr1Thunk(env)
            val expr2Value = expr2Thunk(env)
            when {
                expr1Value.exprEquals(expr2Value) -> ExprValue.nullValue
                else -> expr1Value
            }
        }
    }

    private suspend fun compileCoalesce(expr: PartiqlPhysical.Expr.Coalesce, metas: MetaContainer): PhysicalPlanThunkAsync {
        val argThunks = compileAstExprs(expr.args)

        return thunkFactory.thunkEnvAsync(metas) { env ->
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
                    evaluatorOptions.typingMode == TypingMode.PERMISSIVE && !nullFound -> ExprValue.missingValue
                    else -> ExprValue.nullValue
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
                "The expression's static type was supposed to be INT but instead it was ${value.type}" +
                    "This may indicate the presence of a bug in the type inferencer."
            )
        }
    }

    /**
     *  For operators which could return integer type, check integer overflow in case of [TypingMode.PERMISSIVE].
     */
    private suspend fun checkIntegerOverflow(computeThunk: PhysicalPlanThunkAsync, metas: MetaContainer): PhysicalPlanThunkAsync =
        when (val staticTypes = metas.staticType?.type?.getTypes()) {
            // No staticType, can't validate integer size.
            null -> computeThunk
            else -> {
                when (evaluatorOptions.typingMode) {
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
                        val biggestIntegerType = staticTypes.filterIsInstance<IntType>().maxByOrNull {
                            it.rangeConstraint.numBytes
                        }
                        when (biggestIntegerType) {
                            is IntType -> {
                                val validator = integerValueValidator(biggestIntegerType.rangeConstraint.validRange)

                                thunkFactory.thunkEnvAsync(metas) { env ->
                                    val naryResult = computeThunk(env)
                                    errorSignaler.errorIf(
                                        !validator(naryResult),
                                        ErrorCode.EVALUATOR_INTEGER_OVERFLOW,
                                        { ErrorDetails(metas, "Integer overflow", errorContextFrom(metas)) },
                                        { naryResult }
                                    )
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

    private suspend fun compilePlus(expr: PartiqlPhysical.Expr.Plus, metas: MetaContainer): PhysicalPlanThunkAsync {
        if (expr.operands.size < 2) {
            error("Internal Error: PartiqlPhysical.Expr.Plus must have at least 2 arguments")
        }

        val argThunks = compileAstExprs(expr.operands)

        val computeThunk = thunkFactory.thunkFold(metas, argThunks) { lValue, rValue ->
            (lValue.numberValue() + rValue.numberValue()).exprValue()
        }

        return checkIntegerOverflow(computeThunk, metas)
    }

    private suspend fun compileMinus(expr: PartiqlPhysical.Expr.Minus, metas: MetaContainer): PhysicalPlanThunkAsync {
        if (expr.operands.size < 2) {
            error("Internal Error: PartiqlPhysical.Expr.Minus must have at least 2 arguments")
        }

        val argThunks = compileAstExprs(expr.operands)

        val computeThunk = thunkFactory.thunkFold(metas, argThunks) { lValue, rValue ->
            (lValue.numberValue() - rValue.numberValue()).exprValue()
        }

        return checkIntegerOverflow(computeThunk, metas)
    }

    private suspend fun compilePos(expr: PartiqlPhysical.Expr.Pos, metas: MetaContainer): PhysicalPlanThunkAsync {
        val exprThunk = compileAstExpr(expr.expr)

        val computeThunk = thunkFactory.thunkEnvOperands(metas, exprThunk) { _, value ->
            // Invoking .numberValue() here makes this essentially just a type check
            value.numberValue()
            // Original value is returned unmodified.
            value
        }

        return checkIntegerOverflow(computeThunk, metas)
    }

    private suspend fun compileNeg(expr: PartiqlPhysical.Expr.Neg, metas: MetaContainer): PhysicalPlanThunkAsync {
        val exprThunk = compileAstExpr(expr.expr)

        val computeThunk = thunkFactory.thunkEnvOperands(metas, exprThunk) { _, value ->
            (-value.numberValue()).exprValue()
        }

        return checkIntegerOverflow(computeThunk, metas)
    }

    private suspend fun compileTimes(expr: PartiqlPhysical.Expr.Times, metas: MetaContainer): PhysicalPlanThunkAsync {
        val argThunks = compileAstExprs(expr.operands)

        val computeThunk = thunkFactory.thunkFold(metas, argThunks) { lValue, rValue ->
            (lValue.numberValue() * rValue.numberValue()).exprValue()
        }

        return checkIntegerOverflow(computeThunk, metas)
    }

    private suspend fun compileDivide(expr: PartiqlPhysical.Expr.Divide, metas: MetaContainer): PhysicalPlanThunkAsync {
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
                        internal = true
                    )
                }
            }
        }

        return checkIntegerOverflow(computeThunk, metas)
    }

    private suspend fun compileModulo(expr: PartiqlPhysical.Expr.Modulo, metas: MetaContainer): PhysicalPlanThunkAsync {
        val argThunks = compileAstExprs(expr.operands)

        val computeThunk = thunkFactory.thunkFold(metas, argThunks) { lValue, rValue ->
            val denominator = rValue.numberValue()
            if (denominator.isZero()) {
                err("% by zero", ErrorCode.EVALUATOR_MODULO_BY_ZERO, errorContextFrom(metas), internal = false)
            }

            (lValue.numberValue() % denominator).exprValue()
        }

        return checkIntegerOverflow(computeThunk, metas)
    }

    private suspend fun compileBitwiseAnd(expr: PartiqlPhysical.Expr.BitwiseAnd, metas: MetaContainer): PhysicalPlanThunkAsync {
        val argThunks = compileAstExprs(expr.operands)

        return thunkFactory.thunkFold(metas, argThunks) { lValue, rValue ->
            (lValue.longValue() and rValue.longValue()).exprValue()
        }
    }

    private suspend fun compileEq(expr: PartiqlPhysical.Expr.Eq, metas: MetaContainer): PhysicalPlanThunkAsync {
        val argThunks = compileAstExprs(expr.operands)

        return thunkFactory.thunkAndMap(metas, argThunks) { lValue, rValue ->
            (lValue.exprEquals(rValue))
        }
    }

    private suspend fun compileNe(expr: PartiqlPhysical.Expr.Ne, metas: MetaContainer): PhysicalPlanThunkAsync {
        val argThunks = compileAstExprs(expr.operands)

        return thunkFactory.thunkFold(metas, argThunks) { lValue, rValue ->
            ((!lValue.exprEquals(rValue)).exprValue())
        }
    }

    private suspend fun compileLt(expr: PartiqlPhysical.Expr.Lt, metas: MetaContainer): PhysicalPlanThunkAsync {
        val argThunks = compileAstExprs(expr.operands)

        return thunkFactory.thunkAndMap(metas, argThunks) { lValue, rValue -> lValue < rValue }
    }

    private suspend fun compileLte(expr: PartiqlPhysical.Expr.Lte, metas: MetaContainer): PhysicalPlanThunkAsync {
        val argThunks = compileAstExprs(expr.operands)

        return thunkFactory.thunkAndMap(metas, argThunks) { lValue, rValue -> lValue <= rValue }
    }

    private suspend fun compileGt(expr: PartiqlPhysical.Expr.Gt, metas: MetaContainer): PhysicalPlanThunkAsync {
        val argThunks = compileAstExprs(expr.operands)

        return thunkFactory.thunkAndMap(metas, argThunks) { lValue, rValue -> lValue > rValue }
    }

    private suspend fun compileGte(expr: PartiqlPhysical.Expr.Gte, metas: MetaContainer): PhysicalPlanThunkAsync {
        val argThunks = compileAstExprs(expr.operands)

        return thunkFactory.thunkAndMap(metas, argThunks) { lValue, rValue -> lValue >= rValue }
    }

    private suspend fun compileBetween(expr: PartiqlPhysical.Expr.Between, metas: MetaContainer): PhysicalPlanThunkAsync {
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
    private suspend fun compileIn(expr: PartiqlPhysical.Expr.InCollection, metas: MetaContainer): PhysicalPlanThunkAsync {
        val args = expr.operands
        val leftThunk = compileAstExpr(args[0])
        val rightOp = args[1]

        fun isOptimizedCase(values: List<PartiqlPhysical.Expr>): Boolean = values.all { it is PartiqlPhysical.Expr.Lit && !it.value.isNull }

        suspend fun optimizedCase(values: List<PartiqlPhysical.Expr>): PhysicalPlanThunkAsync {
            // Put all the literals in the sequence into a pre-computed map to be checked later by the thunk.
            // If the left-hand value is one of these we can short-circuit with a result of TRUE.
            // This is the fastest possible case and allows for hundreds of literal values (or more) in the
            // sequence without a huge performance penalty.
            // NOTE: we cannot use a [HashSet<>] here because [ExprValue] does not implement [Object.hashCode] or
            // [Object.equals].
            val precomputedLiteralsMap = values
                .filterIsInstance<PartiqlPhysical.Expr.Lit>()
                .mapTo(TreeSet<ExprValue>(DEFAULT_COMPARATOR)) {
                    ExprValue.of(
                        it.value.toIonValue(ion)
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
            rightOp is PartiqlPhysical.Expr.List && isOptimizedCase(rightOp.values) -> optimizedCase(rightOp.values)
            rightOp is PartiqlPhysical.Expr.Bag && isOptimizedCase(rightOp.values) -> optimizedCase(rightOp.values)
            rightOp is PartiqlPhysical.Expr.Sexp && isOptimizedCase(rightOp.values) -> optimizedCase(rightOp.values)
            // The unoptimized case...
            else -> {
                val rightThunk = compileAstExpr(rightOp)

                // Legacy mode:
                //      Returns FALSE when the right side of IN is not a sequence
                //      Returns NULL if the right side is MISSING or any value on the right side is MISSING
                // Permissive mode:
                //      Returns MISSING when the right side of IN is not a sequence
                //      Returns MISSING if the right side is MISSING or any value on the right side is MISSING
                val (propagateMissingAs, propagateNotASeqAs) = when (evaluatorOptions.typingMode) {
                    TypingMode.LEGACY -> ExprValue.nullValue to ExprValue.newBoolean(false)
                    TypingMode.PERMISSIVE -> ExprValue.missingValue to ExprValue.missingValue
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
                                        return@thunkEnvOperands ExprValue.newBoolean(true)
                                    }
                                }
                            }
                            // If we make it here then there was no match. Propagate MISSING, NULL or return false.
                            // Note that if both MISSING and NULL was encountered, MISSING takes precedence.
                            when {
                                missingSeen -> propagateMissingAs
                                nullSeen -> ExprValue.nullValue
                                else -> ExprValue.newBoolean(false)
                            }
                        }
                    }
                }
            }
        }
    }

    private suspend fun compileNot(expr: PartiqlPhysical.Expr.Not, metas: MetaContainer): PhysicalPlanThunkAsync {
        val argThunk = compileAstExpr(expr.expr)

        return thunkFactory.thunkEnvOperands(metas, argThunk) { _, value ->
            (!value.booleanValue()).exprValue()
        }
    }

    private suspend fun compileAnd(expr: PartiqlPhysical.Expr.And, metas: MetaContainer): PhysicalPlanThunkAsync {
        val argThunks = compileAstExprs(expr.operands)

        // can't use the null propagation supplied by [ThunkFactory.thunkEnv] here because AND short-circuits on
        // false values and *NOT* on NULL or MISSING
        return when (evaluatorOptions.typingMode) {
            TypingMode.LEGACY -> thunkFactory.thunkEnvAsync(metas) thunk@{ env ->
                var hasUnknowns = false
                argThunks.forEach { currThunk ->
                    val currValue = currThunk(env)
                    when {
                        currValue.isUnknown() -> hasUnknowns = true
                        // Short circuit only if we encounter a known false value.
                        !currValue.booleanValue() -> return@thunk ExprValue.newBoolean(false)
                    }
                }

                when (hasUnknowns) {
                    true -> ExprValue.nullValue
                    false -> ExprValue.newBoolean(true)
                }
            }
            TypingMode.PERMISSIVE -> thunkFactory.thunkEnvAsync(metas) thunk@{ env ->
                var hasNull = false
                var hasMissing = false
                argThunks.forEach { currThunk ->
                    val currValue = currThunk(env)
                    when (currValue.type) {
                        // Short circuit only if we encounter a known false value.
                        ExprValueType.BOOL -> if (!currValue.booleanValue()) return@thunk ExprValue.newBoolean(false)
                        ExprValueType.NULL -> hasNull = true
                        // type mismatch, return missing
                        else -> hasMissing = true
                    }
                }

                when {
                    hasMissing -> ExprValue.missingValue
                    hasNull -> ExprValue.nullValue
                    else -> ExprValue.newBoolean(true)
                }
            }
        }
    }

    private suspend fun compileOr(expr: PartiqlPhysical.Expr.Or, metas: MetaContainer): PhysicalPlanThunkAsync {
        val argThunks = compileAstExprs(expr.operands)

        // can't use the null propagation supplied by [ThunkFactory.thunkEnv] here because OR short-circuits on
        // true values and *NOT* on NULL or MISSING
        return when (evaluatorOptions.typingMode) {
            TypingMode.LEGACY ->
                thunkFactory.thunkEnvAsync(metas) thunk@{ env ->
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
                            currValue.booleanValue() -> return@thunk ExprValue.newBoolean(true)
                        }
                    }

                    when (hasUnknowns) {
                        true -> ExprValue.nullValue
                        false -> ExprValue.newBoolean(false)
                    }
                }
            TypingMode.PERMISSIVE -> thunkFactory.thunkEnvAsync(metas) thunk@{ env ->
                var hasNull = false
                var hasMissing = false
                argThunks.forEach { currThunk ->
                    val currValue = currThunk(env)
                    when (currValue.type) {
                        // Short circuit only if we encounter a known true value.
                        ExprValueType.BOOL -> if (currValue.booleanValue()) return@thunk ExprValue.newBoolean(true)
                        ExprValueType.NULL -> hasNull = true
                        else -> hasMissing = true // type mismatch, return missing.
                    }
                }

                when {
                    hasMissing -> ExprValue.missingValue
                    hasNull -> ExprValue.nullValue
                    else -> ExprValue.newBoolean(false)
                }
            }
        }
    }

    private suspend fun compileConcat(expr: PartiqlPhysical.Expr.Concat, metas: MetaContainer): PhysicalPlanThunkAsync {
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

    private suspend fun compileCall(expr: PartiqlPhysical.Expr.Call, metas: MetaContainer): PhysicalPlanThunkAsync {
        val funcArgThunks = compileAstExprs(expr.args)
        val arity = funcArgThunks.size
        val name = expr.funcName.text
        return thunkFactory.thunkEnvAsync(metas) { env ->
            val args = funcArgThunks.map { thunk -> thunk(env) }
            val argTypes = args.map { staticTypeFromExprValue(it) }
            try {
                val func = functionManager.get(name = name, arity = arity, args = argTypes)
                val computeThunk = when (func.signature.unknownArguments) {
                    UnknownArguments.PROPAGATE -> thunkFactory.thunkEnvOperands(metas, funcArgThunks) { env, _ ->
                        func.call(env.session, args)
                    }
                    UnknownArguments.PASS_THRU -> thunkFactory.thunkEnvAsync(metas) { env ->
                        func.call(env.session, args)
                    }
                }
                checkIntegerOverflow(computeThunk, metas)(env)
            } catch (e: FunctionNotFoundException) {
                err(
                    "No such function: $name",
                    ErrorCode.EVALUATOR_NO_SUCH_FUNCTION,
                    errorContextFrom(metas).also {
                        it[Property.FUNCTION_NAME] = name
                    },
                    internal = false
                )
            } catch (e: ArityMismatchException) {
                val (minArity, maxArity) = e.arity
                val errorContext = errorContextFrom(metas).also {
                    it[Property.FUNCTION_NAME] = name
                    it[Property.EXPECTED_ARITY_MIN] = minArity
                    it[Property.EXPECTED_ARITY_MAX] = maxArity
                    it[Property.ACTUAL_ARITY] = arity
                }
                err(
                    "No function found with matching arity: $name",
                    ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
                    errorContext,
                    internal = false
                )
            }
        }
    }

    private suspend fun compileLit(expr: PartiqlPhysical.Expr.Lit, metas: MetaContainer): PhysicalPlanThunkAsync {
        val value = ExprValue.of(expr.value.toIonValue(ion))

        return thunkFactory.thunkEnvAsync(metas) { value }
    }

    private suspend fun compileMissing(metas: MetaContainer): PhysicalPlanThunkAsync =
        thunkFactory.thunkEnvAsync(metas) { ExprValue.missingValue }

    private suspend fun compileGlobalId(expr: PartiqlPhysical.Expr.GlobalId): PhysicalPlanThunkAsync {
        // TODO: we really should consider using something other than `Bindings<ExprValue>` for global variables
        // with the physical plan evaluator because `Bindings<ExprValue>.get()` accepts a `BindingName` instance
        // which contains the `case` property which is always set to `SENSITIVE` and is therefore redundant.
        val bindingName = BindingName(expr.uniqueId.text, BindingCase.SENSITIVE)
        return thunkFactory.thunkEnvAsync(expr.metas) { env ->
            env.session.globals[bindingName] ?: throwUndefinedVariableException(bindingName, expr.metas)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private suspend fun compileLocalId(expr: PartiqlPhysical.Expr.LocalId, metas: MetaContainer): PhysicalPlanThunkAsync {
        val localIndex = expr.index.value.toIntExact()
        return thunkFactory.thunkEnvAsync(metas) { env ->
            env.registers[localIndex]
        }
    }

    private fun compileParameter(expr: PartiqlPhysical.Expr.Parameter, metas: MetaContainer): PhysicalPlanThunkAsync {
        val ordinal = expr.index.value.toInt()
        val index = ordinal - 1

        return { env ->
            val params = env.session.parameters
            if (params.size <= index) {
                throw EvaluationException(
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
        return when (evaluatorOptions.typedOpBehavior) {
            TypedOpBehavior.HONOR_PARAMETERS -> { expValue: ExprValue ->
                staticType.allTypes.any {
                    val matchesStaticType = try {
                        isInstance(expValue, it)
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

    private suspend fun compileIs(expr: PartiqlPhysical.Expr.IsType, metas: MetaContainer): PhysicalPlanThunkAsync {
        val expThunk = compileAstExpr(expr.value)
        val typedOpParameter = expr.type.toTypedOpParameter(customTypedOpParameters)
        if (typedOpParameter.staticType is AnyType) {
            return thunkFactory.thunkEnvAsync(metas) { ExprValue.newBoolean(true) }
        }
        if (evaluatorOptions.typedOpBehavior == TypedOpBehavior.HONOR_PARAMETERS && expr.type is PartiqlPhysical.Type.FloatType && (expr.type as PartiqlPhysical.Type.FloatType).precision != null) {
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

        return thunkFactory.thunkEnvAsync(metas) { env ->
            val expValue = expThunk(env)
            typeMatchFunc(expValue).exprValue()
        }
    }

    private suspend fun compileCastHelper(value: PartiqlPhysical.Expr, asType: PartiqlPhysical.Type, metas: MetaContainer): PhysicalPlanThunkAsync {
        val expThunk = compileAstExpr(value)
        val typedOpParameter = asType.toTypedOpParameter(customTypedOpParameters)
        if (typedOpParameter.staticType is AnyType) {
            return expThunk
        }
        if (evaluatorOptions.typedOpBehavior == TypedOpBehavior.HONOR_PARAMETERS && asType is PartiqlPhysical.Type.FloatType && asType.precision != null) {
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

                throw EvaluationException(
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
                    evaluatorOptions.typedOpBehavior,
                    locationMeta,
                    evaluatorOptions.defaultTimezoneOffset
                )
                typeOpValidate(value, castOutput, getRuntimeType(singleType).toString(), locationMeta)
                castOutput
            }
        }

        fun compileSingleTypeCast(singleType: SingleType): PhysicalPlanThunkAsync {
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

        fun compileCast(type: StaticType): PhysicalPlanThunkAsync = when (type) {
            is SingleType -> compileSingleTypeCast(type)
            is AnyOfType -> {
                val locationMeta = metas.sourceLocationMeta
                val castTable = AnyOfCastTable(type, metas, ::singleTypeCastFunc);

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

    private suspend fun compileCast(expr: PartiqlPhysical.Expr.Cast, metas: MetaContainer): PhysicalPlanThunkAsync =
        thunkFactory.thunkEnvAsync(metas, compileCastHelper(expr.value, expr.asType, metas))

    private suspend fun compileCanCast(expr: PartiqlPhysical.Expr.CanCast, metas: MetaContainer): PhysicalPlanThunkAsync {
        val typedOpParameter = expr.asType.toTypedOpParameter(customTypedOpParameters)
        if (typedOpParameter.staticType is AnyType) {
            return thunkFactory.thunkEnvAsync(metas) { ExprValue.newBoolean(true) }
        }

        val expThunk = compileAstExpr(expr.value)

        // TODO consider making this more efficient by not directly delegating to CAST
        // TODO consider also making the operand not double evaluated (e.g. having expThunk memoize)
        val castThunkEnv = compileCastHelper(expr.value, expr.asType, expr.metas)
        return thunkFactory.thunkEnvAsync(metas) { env ->
            val sourceValue = expThunk(env)
            try {
                when {
                    // NULL/MISSING can cast to anything as themselves
                    sourceValue.isUnknown() -> ExprValue.newBoolean(true)
                    else -> {
                        val castedValue = castThunkEnv(env)
                        when {
                            // NULL/MISSING from cast is a permissive way to signal failure
                            castedValue.isUnknown() -> ExprValue.newBoolean(false)
                            else -> ExprValue.newBoolean(true)
                        }
                    }
                }
            } catch (e: EvaluationException) {
                if (e.internal) {
                    throw e
                }
                ExprValue.newBoolean(false)
            }
        }
    }

    private suspend fun compileCanLosslessCast(expr: PartiqlPhysical.Expr.CanLosslessCast, metas: MetaContainer): PhysicalPlanThunkAsync {
        val typedOpParameter = expr.asType.toTypedOpParameter(customTypedOpParameters)
        if (typedOpParameter.staticType is AnyType) {
            return thunkFactory.thunkEnvAsync(metas) { ExprValue.newBoolean(true) }
        }

        val expThunk = compileAstExpr(expr.value)

        // TODO consider making this more efficient by not directly delegating to CAST
        val castThunkEnv = compileCastHelper(expr.value, expr.asType, expr.metas)
        return thunkFactory.thunkEnvAsync(metas) { env ->
            val sourceValue = expThunk(env)
            val sourceType = staticTypeFromExprValue(sourceValue)

            suspend fun roundTrip(): ExprValue {
                val castedValue = castThunkEnv(env)

                val locationMeta = metas.sourceLocationMeta
                fun castFunc(singleType: SingleType) =
                    { value: ExprValue ->
                        value.cast(
                            singleType,
                            evaluatorOptions.typedOpBehavior,
                            locationMeta,
                            evaluatorOptions.defaultTimezoneOffset
                        )
                    }

                val roundTripped = when (sourceType) {
                    is SingleType -> castFunc(sourceType)(castedValue)
                    is AnyOfType -> {
                        val castTable = AnyOfCastTable(sourceType, metas, ::castFunc)
                        castTable.cast(sourceValue)
                    }
                    // Should not be possible
                    is AnyType -> throw IllegalStateException("ANY type is not configured correctly in compiler")
                }

                val lossless = sourceValue.exprEquals(roundTripped)
                return ExprValue.newBoolean(lossless)
            }

            try {
                when (sourceValue.type) {
                    // NULL can cast to anything as itself
                    ExprValueType.NULL -> ExprValue.newBoolean(true)

                    // Short-circuit timestamp -> date roundtrip if precision isn't [Timestamp.Precision.DAY] or
                    //   [Timestamp.Precision.MONTH] or [Timestamp.Precision.YEAR]
                    ExprValueType.TIMESTAMP -> when (typedOpParameter.staticType) {
                        StaticType.DATE -> when (sourceValue.timestampValue().precision) {
                            Timestamp.Precision.DAY, Timestamp.Precision.MONTH, Timestamp.Precision.YEAR -> roundTrip()
                            else -> ExprValue.newBoolean(false)
                        }
                        StaticType.TIME -> ExprValue.newBoolean(false)
                        else -> roundTrip()
                    }

                    // For all other cases, attempt a round-trip of the value through the source and dest types
                    else -> roundTrip()
                }
            } catch (e: EvaluationException) {
                if (e.internal) {
                    throw e
                }
                ExprValue.newBoolean(false)
            }
        }
    }

    private suspend fun compileSimpleCase(expr: PartiqlPhysical.Expr.SimpleCase, metas: MetaContainer): PhysicalPlanThunkAsync {
        val valueThunk = compileAstExpr(expr.expr)
        val branchThunks = expr.cases.pairs.map { Pair(compileAstExpr(it.first), compileAstExpr(it.second)) }
        val elseThunk = when (val default = expr.default) {
            null -> thunkFactory.thunkEnvAsync(metas) { ExprValue.nullValue }
            else -> compileAstExpr(default)
        }

        return thunkFactory.thunkEnvAsync(metas) thunk@{ env ->
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

    private suspend fun compileSearchedCase(expr: PartiqlPhysical.Expr.SearchedCase, metas: MetaContainer): PhysicalPlanThunkAsync {
        val branchThunks = expr.cases.pairs.map { compileAstExpr(it.first) to compileAstExpr(it.second) }
        val elseThunk = when (val default = expr.default) {
            null -> thunkFactory.thunkEnvAsync(metas) { ExprValue.nullValue }
            else -> compileAstExpr(default)
        }

        return when (evaluatorOptions.typingMode) {
            TypingMode.LEGACY -> thunkFactory.thunkEnvAsync(metas) thunk@{ env ->
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
            TypingMode.PERMISSIVE -> thunkFactory.thunkEnvAsync(metas) thunk@{ env ->
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

    private suspend fun compileStruct(expr: PartiqlPhysical.Expr.Struct): PhysicalPlanThunkAsync {
        val structParts = compileStructParts(expr.parts)

        val ordering = if (expr.parts.none { it is PartiqlPhysical.StructPart.StructFields })
            StructOrdering.ORDERED
        else
            StructOrdering.UNORDERED

        return thunkFactory.thunkEnvAsync(expr.metas) { env ->
            val columns = mutableListOf<ExprValue>()
            for (element in structParts) {
                when (element) {
                    is CompiledStructPartAsync.Field -> {
                        val fieldName = element.nameThunk(env)
                        when (evaluatorOptions.typingMode) {
                            TypingMode.LEGACY ->
                                if (!fieldName.type.isText) {
                                    err(
                                        "Found struct field key to be of type ${fieldName.type}",
                                        ErrorCode.EVALUATOR_NON_TEXT_STRUCT_FIELD_KEY,
                                        errorContextFrom(expr.metas.sourceLocationMeta).also { pvm ->
                                            pvm[Property.ACTUAL_TYPE] = fieldName.type.toString()
                                        },
                                        internal = false
                                    )
                                }
                            TypingMode.PERMISSIVE ->
                                if (!fieldName.type.isText) {
                                    continue
                                }
                        }
                        val fieldValue = element.valueThunk(env)
                        columns.add(fieldValue.namedValue(fieldName))
                    }
                    is CompiledStructPartAsync.StructMerge -> {
                        for (projThunk in element.thunks) {
                            val value = projThunk(env)
                            if (value.type == ExprValueType.MISSING) continue

                            val children = value.asSequence()
                            if (!children.any() || value.type.isSequence) {
                                val name = syntheticColumnName(columns.size).exprValue()
                                columns.add(value.namedValue(name))
                            } else {
                                val valuesToProject =
                                    when (evaluatorOptions.projectionIteration) {
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

    private suspend fun compileStructParts(projectItems: List<PartiqlPhysical.StructPart>): List<CompiledStructPartAsync> =
        projectItems.map {
            when (it) {
                is PartiqlPhysical.StructPart.StructField -> {
                    val fieldThunk = compileAstExpr(it.fieldName)
                    val valueThunk = compileAstExpr(it.value)
                    CompiledStructPartAsync.Field(fieldThunk, valueThunk)
                }
                is PartiqlPhysical.StructPart.StructFields -> {
                    CompiledStructPartAsync.StructMerge(listOf(compileAstExpr(it.partExpr)))
                }
            }
        }

    private suspend fun compileSeq(seqType: ExprValueType, itemExprs: List<PartiqlPhysical.Expr>, metas: MetaContainer): PhysicalPlanThunkAsync {
        require(seqType.isSequence) { "seqType must be a sequence!" }

        val itemThunks = compileAstExprs(itemExprs)

        val makeItemThunkSequence = when (seqType) {
            ExprValueType.BAG -> { env: EvaluatorState ->
                itemThunks.asFlow().map { itemThunk ->
                    // call to unnamedValue() makes sure we don't expose any underlying value name/ordinal
                    itemThunk(env).unnamedValue()
                }
            }
            else -> { env: EvaluatorState ->
                itemThunks.asFlow().withIndex().map { indexedVal ->
                    indexedVal.value(env).namedValue(indexedVal.index.exprValue())
                }
            }
        }

        return thunkFactory.thunkEnvAsync(metas) { env ->
            when (seqType) {
                ExprValueType.BAG -> ExprValue.newBag(makeItemThunkSequence(env).toList())
                ExprValueType.LIST -> ExprValue.newList(makeItemThunkSequence(env).toList())
                ExprValueType.SEXP -> ExprValue.newSexp(makeItemThunkSequence(env).toList())
                else -> error("sequence type required")
            }
        }
    }

    private suspend fun compilePath(expr: PartiqlPhysical.Expr.Path, metas: MetaContainer): PhysicalPlanThunkAsync {
        val rootThunk = compileAstExpr(expr.root)
        val remainingComponents = LinkedList<PartiqlPhysical.PathStep>()

        expr.steps.forEach { remainingComponents.addLast(it) }

        val componentThunk = compilePathComponents(remainingComponents, metas)

        return thunkFactory.thunkEnvAsync(metas) { env ->
            val rootValue = rootThunk(env)
            componentThunk(env, rootValue)
        }
    }

    private suspend fun compilePathComponents(
        remainingComponents: LinkedList<PartiqlPhysical.PathStep>,
        pathMetas: MetaContainer
    ): PhysicalPlanThunkValueAsync<ExprValue> {

        val componentThunks = ArrayList<ThunkValueAsync<EvaluatorState, ExprValue>>()

        while (!remainingComponents.isEmpty()) {
            val pathComponent = remainingComponents.removeFirst()
            val componentMetas = pathComponent.metas
            componentThunks.add(
                when (pathComponent) {
                    is PartiqlPhysical.PathStep.PathExpr -> {
                        val indexExpr = pathComponent.index
                        val caseSensitivity = pathComponent.case
                        when {
                            // If indexExpr is a literal string, there is no need to evaluate it--just compile a
                            // thunk that directly returns a bound value
                            indexExpr is PartiqlPhysical.Expr.Lit && indexExpr.value.toIonValue(ion) is IonString -> {
                                val lookupName = BindingName(
                                    indexExpr.value.toIonValue(ion).stringValue()!!,
                                    caseSensitivity.toBindingCase()
                                )
                                thunkFactory.thunkEnvValue(componentMetas) { _, componentValue ->
                                    componentValue.bindings[lookupName] ?: ExprValue.missingValue
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
                                            when (evaluatorOptions.typingMode) {
                                                TypingMode.LEGACY -> err(
                                                    "Cannot convert index to int/string: $indexValue",
                                                    ErrorCode.EVALUATOR_INVALID_CONVERSION,
                                                    errorContextFrom(componentMetas),
                                                    internal = false
                                                )
                                                TypingMode.PERMISSIVE -> ExprValue.missingValue
                                            }
                                        }
                                    } ?: ExprValue.missingValue
                                }
                            }
                        }
                    }
                    is PartiqlPhysical.PathStep.PathUnpivot -> {
                        when {
                            !remainingComponents.isEmpty() -> {
                                val tempThunk = compilePathComponents(remainingComponents, pathMetas)
                                thunkFactory.thunkEnvValue(componentMetas) { env, componentValue ->
                                    val mapped = componentValue.unpivot()
                                        .flatMap { tempThunk(env, it).rangeOver() }
                                        .asSequence()
                                    ExprValue.newBag(mapped)
                                }
                            }
                            else ->
                                thunkFactory.thunkEnvValue(componentMetas) { _, componentValue ->
                                    ExprValue.newBag(componentValue.unpivot().asSequence())
                                }
                        }
                    }
                    // this is for `path[*].component`
                    is PartiqlPhysical.PathStep.PathWildcard -> {
                        when {
                            !remainingComponents.isEmpty() -> {
                                val hasMoreWildCards =
                                    remainingComponents.filterIsInstance<PartiqlPhysical.PathStep.PathWildcard>().any()
                                val tempThunk = compilePathComponents(remainingComponents, pathMetas)

                                when {
                                    !hasMoreWildCards -> thunkFactory.thunkEnvValue(componentMetas) { env, componentValue ->
                                        val mapped = componentValue
                                            .rangeOver()
                                            .map { tempThunk(env, it) }
                                            .asSequence()

                                        ExprValue.newBag(mapped)
                                    }
                                    else -> thunkFactory.thunkEnvValue(componentMetas) { env, componentValue ->
                                        val mapped = componentValue
                                            .rangeOver()
                                            .flatMap {
                                                val tempValue = tempThunk(env, it)
                                                tempValue
                                            }
                                            .asSequence()

                                        ExprValue.newBag(mapped)
                                    }
                                }
                            }
                            else -> {
                                thunkFactory.thunkEnvValue(componentMetas) { _, componentValue ->
                                    val mapped = componentValue.rangeOver().asSequence()
                                    ExprValue.newBag(mapped)
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
    private suspend fun compileLike(expr: PartiqlPhysical.Expr.Like, metas: MetaContainer): PhysicalPlanThunkAsync {
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
                            it[Property.LIKE_PATTERN] = pattern.toString()
                            if (escape != null) it[Property.LIKE_ESCAPE] = escape.toString()
                        },
                        internal = false
                    )
                }
                else -> {
                    val (patternString: String, escapeChar: Int?) =
                        checkPattern(pattern.stringValue(), patternLocationMeta, escape?.stringValue(), escapeLocationMeta)
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
                likePattern == null || value.type.isUnknown -> ExprValue.nullValue
                !value.type.isText -> err(
                    "LIKE expression must be given non-null strings as input",
                    ErrorCode.EVALUATOR_LIKE_INVALID_INPUTS,
                    errorContextFrom(metas).also {
                        it[Property.LIKE_VALUE] = value.toString()
                    },
                    internal = false
                )
                else -> ExprValue.newBoolean(likePattern().matcher(value.stringValue()).matches())
            }
        }

        val valueThunk = compileAstExpr(valueExpr)

        // If the pattern and escape expressions are literals then we can compile the pattern now and
        // re-use it with every execution. Otherwise, we must re-compile the pattern every time.
        return when {
            patternExpr is PartiqlPhysical.Expr.Lit && (escapeExpr == null || escapeExpr is PartiqlPhysical.Expr.Lit) -> {
                val patternParts = getRegexPattern(
                    ExprValue.of(patternExpr.value.toIonValue(ion)),
                    (escapeExpr as? PartiqlPhysical.Expr.Lit)?.value?.toIonValue(ion)
                        ?.let { ExprValue.of(it) }
                )

                // If valueExpr is also a literal then we can evaluate this at compile time and return a constant.
                if (valueExpr is PartiqlPhysical.Expr.Lit) {
                    val resultValue = matchRegexPattern(
                        ExprValue.of(valueExpr.value.toIonValue(ion)),
                        patternParts
                    )
                    return thunkFactory.thunkEnvAsync(metas) { resultValue }
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
        pattern: String,
        patternLocationMeta: SourceLocationMeta?,
        escape: String?,
        escapeLocationMeta: SourceLocationMeta?
    ): Pair<String, Int?> {

        escape?.let {
            val escapeCharString = checkEscapeChar(escape, escapeLocationMeta)
            val escapeCharCodePoint = escapeCharString.codePointAt(0) // escape is a string of length 1
            val validEscapedChars = setOf('_'.code, '%'.code, escapeCharCodePoint)
            val iter = pattern.codePointSequence().iterator()

            while (iter.hasNext()) {
                val current = iter.next()
                if (current == escapeCharCodePoint && (!iter.hasNext() || !validEscapedChars.contains(iter.next()))) {
                    err(
                        "Invalid escape sequence : $pattern",
                        ErrorCode.EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE,
                        errorContextFrom(patternLocationMeta).apply {
                            set(Property.LIKE_PATTERN, pattern)
                            set(Property.LIKE_ESCAPE, escapeCharString)
                        },
                        internal = false
                    )
                }
            }
            return Pair(pattern, escapeCharCodePoint)
        }
        return Pair(pattern, null)
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
    private fun checkEscapeChar(escape: String, locationMeta: SourceLocationMeta?): String {
        when (escape) {
            "" -> {
                err(
                    "Cannot use empty character as ESCAPE character in a LIKE predicate: $escape",
                    ErrorCode.EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE,
                    errorContextFrom(locationMeta),
                    internal = false
                )
            }
            else -> {
                if (escape.trim().length != 1) {
                    err(
                        "Escape character must have size 1 : $escape",
                        ErrorCode.EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE,
                        errorContextFrom(locationMeta),
                        internal = false
                    )
                }
            }
        }
        return escape
    }

    private suspend fun compileExec(node: PartiqlPhysical.Statement.Exec): PhysicalPlanThunkAsync {
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

            throw EvaluationException(
                message,
                ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_PROCEDURE_CALL,
                errorContext,
                internal = false
            )
        }

        // Compile the procedure's arguments
        val argThunks = compileAstExprs(args)

        return thunkFactory.thunkEnvAsync(metas) { env ->
            val procedureArgValues = argThunks.map { it(env) }
            procedure.call(env.session, procedureArgValues)
        }
    }

    private suspend fun compileDate(expr: PartiqlPhysical.Expr.Date, metas: MetaContainer): PhysicalPlanThunkAsync =
        thunkFactory.thunkEnvAsync(metas) {
            ExprValue.newDate(
                expr.year.value.toInt(),
                expr.month.value.toInt(),
                expr.day.value.toInt()
            )
        }

    private suspend fun compileLitTime(expr: PartiqlPhysical.Expr.LitTime, metas: MetaContainer): PhysicalPlanThunkAsync =
        thunkFactory.thunkEnvAsync(metas) {
            // Add the default time zone if the type "TIME WITH TIME ZONE" does not have an explicitly specified time zone.
            ExprValue.newTime(
                Time.of(
                    expr.value.hour.value.toInt(),
                    expr.value.minute.value.toInt(),
                    expr.value.second.value.toInt(),
                    expr.value.nano.value.toInt(),
                    expr.value.precision.value.toInt(),
                    if (expr.value.withTimeZone.value && expr.value.tzMinutes == null) evaluatorOptions.defaultTimezoneOffset.totalMinutes else expr.value.tzMinutes?.value?.toInt()
                )
            )
        }

    private suspend fun compileBagOp(node: PartiqlPhysical.Expr.BagOp, metas: MetaContainer): PhysicalPlanThunkAsync {
        val lhs = compileAstExpr(node.operands[0])
        val rhs = compileAstExpr(node.operands[1])
        val op = ExprValueBagOp.create(node.op, metas)
        return thunkFactory.thunkEnvAsync(metas) { env ->
            val l = lhs(env)
            val r = rhs(env)
            val result = when (node.quantifier) {
                is PartiqlPhysical.SetQuantifier.All -> op.eval(l, r)
                is PartiqlPhysical.SetQuantifier.Distinct -> op.eval(l, r).distinct()
            }
            ExprValue.newBag(result)
        }
    }

    private suspend fun compilePivot(expr: PartiqlPhysical.Expr.Pivot, metas: MetaContainer): PhysicalPlanThunkAsync {
        val inputBExpr: RelationThunkEnvAsync = bexperConverter.convert(expr.input)
        // The names are intentionally flipped for clarity; consider fixing this in the AST
        val valueExpr = compileAstExpr(expr.key)
        val keyExpr = compileAstExpr(expr.value)
        return thunkFactory.thunkEnvAsync(metas) { env ->
            val attributes: Flow<ExprValue> = flow {
                val relation = inputBExpr(env)
                while (relation.nextRow()) {
                    val key = keyExpr.invoke(env)
                    if (key.type.isText) {
                        val value = valueExpr.invoke(env)
                        emit(value.namedValue(key))
                    }
                }
            }
            ExprValue.newStruct(attributes.toList(), StructOrdering.UNORDERED)
        }
    }

    /** A special wrapper for `UNPIVOT` values as a BAG. */
    private class UnpivotedExprValue(private val values: Iterable<ExprValue>) : BaseExprValue() {
        override val type = ExprValueType.BAG
        override fun iterator() = values.iterator()
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
                this.namedValue(ExprValue.newString(syntheticColumnName(0)))
            )
        )
    }

    private fun createStructExprValue(seq: Sequence<ExprValue>, ordering: StructOrdering) =
        ExprValue.newStruct(
            when (evaluatorOptions.projectionIteration) {
                ProjectionIterationBehavior.FILTER_MISSING -> seq.filter { it.type != ExprValueType.MISSING }
                ProjectionIterationBehavior.UNFILTERED -> seq
            },
            ordering
        )
}

/**
 * Represents an element in a select list that is to be projected into the final result.
 * i.e. an expression, or a (project_all) node.
 */
private sealed class CompiledStructPartAsync {

    /**
     * Represents a single compiled expression to be projected into the final result.
     * Given `SELECT a + b as value FROM foo`:
     * - `name` is "value"
     * - `thunk` is compiled expression, i.e. `a + b`
     */
    class Field(val nameThunk: PhysicalPlanThunkAsync, val valueThunk: PhysicalPlanThunkAsync) : CompiledStructPartAsync()

    /**
     * Represents a wildcard ((path_project_all) node) expression to be projected into the final result.
     * This covers two cases.  For `SELECT foo.* FROM foo`, `exprThunks` contains a single compiled expression
     * `foo`.
     *
     * For `SELECT * FROM foo, bar, bat`, `exprThunks` would contain a compiled expression for each of `foo`, `bar` and
     * `bat`.
     */
    class StructMerge(val thunks: List<PhysicalPlanThunkAsync>) : CompiledStructPartAsync()
}
