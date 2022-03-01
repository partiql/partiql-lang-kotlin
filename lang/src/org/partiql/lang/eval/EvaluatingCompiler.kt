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

package org.partiql.lang.eval

import com.amazon.ion.IonString
import com.amazon.ion.IonValue
import com.amazon.ion.Timestamp
import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.toIonValue
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.sourceLocation
import org.partiql.lang.ast.toPartiQlMetaContainer
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.domains.staticType
import org.partiql.lang.domains.toBindingCase
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.errors.UNBOUND_QUOTED_IDENTIFIER_HINT
import org.partiql.lang.eval.builtins.storedprocedure.StoredProcedure
import org.partiql.lang.eval.like.PatternPart
import org.partiql.lang.eval.like.executePattern
import org.partiql.lang.eval.like.parsePattern
import org.partiql.lang.eval.time.Time
import org.partiql.lang.eval.visitors.PartiqlPhysicalSanityValidator
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
import org.partiql.lang.util.checkThreadInterrupted
import org.partiql.lang.util.codePointSequence
import org.partiql.lang.util.div
import org.partiql.lang.util.isZero
import org.partiql.lang.util.minus
import org.partiql.lang.util.plus
import org.partiql.lang.util.rem
import org.partiql.lang.util.stringValue
import org.partiql.lang.util.times
import org.partiql.lang.util.timestampValue
import org.partiql.lang.util.totalMinutes
import org.partiql.lang.util.unaryMinus
import java.math.BigDecimal
import java.util.*

/**
 * A basic compiler that converts an instance of [PartiqlPhysical] to an [Expression].
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
    private val thunkFactory = compileOptions.typingMode.createThunkFactory(compileOptions, valueFactory)

    init {
        if(procedures.isNotEmpty()) {
            TODO("Support system stored procedures")
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

    /**
     * Compiles a [PartiqlPhysical.Statement] tree to an [Expression].
     *
     * Checks [Thread.interrupted] before every expression and sub-expression is compiled
     * and throws [InterruptedException] if [Thread.interrupted] it has been set in the
     * hope that long-running compilations may be aborted by the caller.
     */
    fun compile(plan: PartiqlPhysical.Statement): Expression {
        PartiqlPhysicalSanityValidator().validate(plan, compileOptions)

        val thunk = compileAstStatement(plan)

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
     * Compiles the specified [PartiqlPhysical.Statement] into a [ThunkEnv].
     *
     * This function will [InterruptedException] if [Thread.interrupted] has been set.
     */
    private fun compileAstStatement(ast: PartiqlPhysical.Statement): ThunkEnv {
        checkThreadInterrupted()
        return when (ast) {
            is PartiqlPhysical.Statement.Query -> compileAstExpr(ast.expr)
        }
    }

    private fun compileAstExpr(expr: PartiqlPhysical.Expr): ThunkEnv {
        val metas = expr.metas


        // DL TODO: why are we passing metas in to each of these functions?
        return when (expr) {
            is PartiqlPhysical.Expr.Lit -> compileLit(expr, metas)
            is PartiqlPhysical.Expr.Missing -> compileMissing(metas)
            is PartiqlPhysical.Expr.DynamicId -> compileDynamicId(expr, metas)
            is PartiqlPhysical.Expr.LocalId -> compileLocalId(expr, metas)
            is PartiqlPhysical.Expr.GlobalId -> compileGlobalId(expr)
            is PartiqlPhysical.Expr.SimpleCase -> compileSimpleCase(expr, metas)
            is PartiqlPhysical.Expr.SearchedCase -> compileSearchedCase(expr, metas)
            is PartiqlPhysical.Expr.Path -> compilePath(expr, metas)
            is PartiqlPhysical.Expr.Struct -> compileStruct(expr, metas)
            is PartiqlPhysical.Expr.CallAgg -> compileCallAgg(expr, metas)
            is PartiqlPhysical.Expr.Parameter -> compileParameter(expr, metas)
            is PartiqlPhysical.Expr.Date -> compileDate(expr, metas)
            is PartiqlPhysical.Expr.LitTime -> compileLitTime(expr, metas)

            // arithmetic operations
            is PartiqlPhysical.Expr.Plus -> compilePlus(expr, metas)
            is PartiqlPhysical.Expr.Times -> compileTimes(expr, metas)
            is PartiqlPhysical.Expr.Minus -> compileMinus(expr, metas)
            is PartiqlPhysical.Expr.Divide -> compileDivide(expr, metas)
            is PartiqlPhysical.Expr.Modulo -> compileModulo(expr, metas)

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

            // set operators
            is PartiqlPhysical.Expr.Intersect,
            is PartiqlPhysical.Expr.Union,
            is PartiqlPhysical.Expr.Except -> {
                err(
                    "${expr.javaClass.canonicalName} is not yet supported",
                    ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET,
                    errorContextFrom(metas).also {
                        it[Property.FEATURE_NAME] = expr.javaClass.canonicalName
                    },
                    internal = false
                )
            }
            is PartiqlPhysical.Expr.MapValues -> TODO("mapValues expression")
        }
    }


    private fun compileAstExprs(args: List<PartiqlPhysical.Expr>) = args.map { compileAstExpr(it) }

    private fun compileNullIf(expr: PartiqlPhysical.Expr.NullIf, metas: MetaContainer): ThunkEnv {
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

    private fun compileCoalesce(expr: PartiqlPhysical.Expr.Coalesce, metas: MetaContainer): ThunkEnv {
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
                "The expression's static type was supposed to be INT but instead it was ${value.type}" +
                    "This may indicate the presence of a bug in the type inferencer."
            )
        }
    }

    /**
     *  For operators which could return integer type, check integer overflow in case of [TypingMode.PERMISSIVE].
     */
    private fun resolveIntConstraint(computeThunk: ThunkEnv, metas: MetaContainer): ThunkEnv =
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
                            is IntType -> {
                                val validator = integerValueValidator(biggestIntegerType.rangeConstraint.validRange)

                                thunkFactory.thunkEnv(metas) { env ->
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

    private fun compilePlus(expr: PartiqlPhysical.Expr.Plus, metas: MetaContainer): ThunkEnv {
        if (expr.operands.size < 2) {
            error("Internal Error: PartiqlPhysical.Expr.Plus must have at least 2 arguments")
        }

        val argThunks = compileAstExprs(expr.operands)

        val computeThunk = thunkFactory.thunkFold(metas, argThunks) { lValue, rValue ->
            (lValue.numberValue() + rValue.numberValue()).exprValue()
        }

        return resolveIntConstraint(computeThunk, metas)
    }

    private fun compileMinus(expr: PartiqlPhysical.Expr.Minus, metas: MetaContainer): ThunkEnv {
        if (expr.operands.size < 2) {
            error("Internal Error: PartiqlPhysical.Expr.Minus must have at least 2 arguments")
        }

        val argThunks = compileAstExprs(expr.operands)

        val computeThunk = thunkFactory.thunkFold(metas, argThunks) { lValue, rValue ->
            (lValue.numberValue() - rValue.numberValue()).exprValue()
        }

        return resolveIntConstraint(computeThunk, metas)
    }

    private fun compilePos(expr: PartiqlPhysical.Expr.Pos, metas: MetaContainer): ThunkEnv {
        val exprThunk = compileAstExpr(expr.expr)

        val computeThunk = thunkFactory.thunkEnvOperands(metas, exprThunk) { _, value ->
            // Invoking .numberValue() here makes this essentially just a type check
            value.numberValue()
            // Original value is returned unmodified.
            value
        }

        return resolveIntConstraint(computeThunk, metas)
    }

    private fun compileNeg(expr: PartiqlPhysical.Expr.Neg, metas: MetaContainer): ThunkEnv {
        val exprThunk = compileAstExpr(expr.expr)

        val computeThunk = thunkFactory.thunkEnvOperands(metas, exprThunk) { _, value ->
            (-value.numberValue()).exprValue()
        }

        return resolveIntConstraint(computeThunk, metas)
    }

    private fun compileTimes(expr: PartiqlPhysical.Expr.Times, metas: MetaContainer): ThunkEnv {
        val argThunks = compileAstExprs(expr.operands)

        val computeThunk = thunkFactory.thunkFold(metas, argThunks) { lValue, rValue ->
            (lValue.numberValue() * rValue.numberValue()).exprValue()
        }

        return resolveIntConstraint(computeThunk, metas)
    }

    private fun compileDivide(expr: PartiqlPhysical.Expr.Divide, metas: MetaContainer): ThunkEnv {
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

        return resolveIntConstraint(computeThunk, metas)
    }

    private fun compileModulo(expr: PartiqlPhysical.Expr.Modulo, metas: MetaContainer): ThunkEnv {
        val argThunks = compileAstExprs(expr.operands)

        val computeThunk = thunkFactory.thunkFold(metas, argThunks) { lValue, rValue ->
            val denominator = rValue.numberValue()
            if (denominator.isZero()) {
                err("% by zero", ErrorCode.EVALUATOR_MODULO_BY_ZERO, null, false)
            }

            (lValue.numberValue() % denominator).exprValue()
        }

        return resolveIntConstraint(computeThunk, metas)
    }

    private fun compileEq(expr: PartiqlPhysical.Expr.Eq, metas: MetaContainer): ThunkEnv {
        val argThunks = compileAstExprs(expr.operands)

        return thunkFactory.thunkAndMap(metas, argThunks) { lValue, rValue ->
            (lValue.exprEquals(rValue))
        }
    }

    private fun compileNe(expr: PartiqlPhysical.Expr.Ne, metas: MetaContainer): ThunkEnv {
        val argThunks = compileAstExprs(expr.operands)

        return thunkFactory.thunkFold(metas, argThunks) { lValue, rValue ->
            ((!lValue.exprEquals(rValue)).exprValue())
        }
    }

    private fun compileLt(expr: PartiqlPhysical.Expr.Lt, metas: MetaContainer): ThunkEnv {
        val argThunks = compileAstExprs(expr.operands)

        return thunkFactory.thunkAndMap(metas, argThunks) { lValue, rValue -> lValue < rValue }
    }

    private fun compileLte(expr: PartiqlPhysical.Expr.Lte, metas: MetaContainer): ThunkEnv {
        val argThunks = compileAstExprs(expr.operands)

        return thunkFactory.thunkAndMap(metas, argThunks) { lValue, rValue -> lValue <= rValue }
    }

    private fun compileGt(expr: PartiqlPhysical.Expr.Gt, metas: MetaContainer): ThunkEnv {
        val argThunks = compileAstExprs(expr.operands)

        return thunkFactory.thunkAndMap(metas, argThunks) { lValue, rValue -> lValue > rValue }
    }

    private fun compileGte(expr: PartiqlPhysical.Expr.Gte, metas: MetaContainer): ThunkEnv {
        val argThunks = compileAstExprs(expr.operands)

        return thunkFactory.thunkAndMap(metas, argThunks) { lValue, rValue -> lValue >= rValue }
    }

    private fun compileBetween(expr: PartiqlPhysical.Expr.Between, metas: MetaContainer): ThunkEnv {
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
    private fun compileIn(expr: PartiqlPhysical.Expr.InCollection, metas: MetaContainer): ThunkEnv {
        val args = expr.operands
        val leftThunk = compileAstExpr(args[0])
        val rightOp = args[1]

        fun isOptimizedCase(values: List<PartiqlPhysical.Expr>): Boolean = values.all { it is PartiqlPhysical.Expr.Lit && !it.value.isNull }

        fun optimizedCase(values: List<PartiqlPhysical.Expr>): ThunkEnv {
            // Put all the literals in the sequence into a pre-computed map to be checked later by the thunk.
            // If the left-hand value is one of these we can short-circuit with a result of TRUE.
            // This is the fastest possible case and allows for hundreds of literal values (or more) in the
            // sequence without a huge performance penalty.
            // NOTE: we cannot use a [HashSet<>] here because [ExprValue] does not implement [Object.hashCode] or
            // [Object.equals].
            val precomputedLiteralsMap = values
                .filterIsInstance<PartiqlPhysical.Expr.Lit>()
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

    private fun compileNot(expr: PartiqlPhysical.Expr.Not, metas: MetaContainer): ThunkEnv {
        val argThunk = compileAstExpr(expr.expr)

        return thunkFactory.thunkEnvOperands(metas, argThunk) { _, value ->
            (!value.booleanValue()).exprValue()
        }
    }

    private fun compileAnd(expr: PartiqlPhysical.Expr.And, metas: MetaContainer): ThunkEnv {
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

    private fun compileOr(expr: PartiqlPhysical.Expr.Or, metas: MetaContainer): ThunkEnv {
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

    private fun compileConcat(expr: PartiqlPhysical.Expr.Concat, metas: MetaContainer): ThunkEnv {
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

    private fun compileCall(expr: PartiqlPhysical.Expr.Call, metas: MetaContainer): ThunkEnv {
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

            throw EvaluationException(
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
                        numArgs = args.size,
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

        return resolveIntConstraint(computeThunk, metas)
    }

    private fun compileLit(expr: PartiqlPhysical.Expr.Lit, metas: MetaContainer): ThunkEnv {
        val value = valueFactory.newFromIonValue(expr.value.toIonValue(valueFactory.ion))

        return thunkFactory.thunkEnv(metas) { value }
    }

    private fun compileMissing(metas: MetaContainer): ThunkEnv =
        thunkFactory.thunkEnv(metas) { valueFactory.missingValue }


    private fun compileDynamicId(expr: PartiqlPhysical.Expr.DynamicId, metas: MetaContainer): ThunkEnv {
        val bindingName = BindingName(expr.name.text, expr.case.toBindingCase())
        val evalVariableReference = when (compileOptions.undefinedVariable) {
            UndefinedVariableBehavior.ERROR ->
                thunkFactory.thunkEnv(metas) { env ->
                    when (val value = env.current[bindingName]) {
                    null -> {
                            val (errorCode, hint) = when (expr.case) {
                                is PartiqlPhysical.CaseSensitivity.CaseSensitive ->
                                    Pair(
                                        ErrorCode.EVALUATOR_QUOTED_BINDING_DOES_NOT_EXIST,
                                        " $UNBOUND_QUOTED_IDENTIFIER_HINT"
                                    )
                                is PartiqlPhysical.CaseSensitivity.CaseInsensitive ->
                                    Pair(ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST, "")
                            }
                            throw EvaluationException(
                                "No such binding: ${bindingName.name}.$hint",
                                errorCode,
                                errorContextFrom(metas).also {
                                    it[Property.BINDING_NAME] = bindingName.name
                                },
                                internal = false
                            )
                        }
                        else -> value
                    }
                }
            UndefinedVariableBehavior.MISSING ->
                thunkFactory.thunkEnv(metas) { env ->
                    env.current[bindingName] ?: valueFactory.missingValue
                }
        }

        return evalVariableReference
    }

    @Suppress("UNUSED_PARAMETER")
    private fun compileLocalId(expr: PartiqlPhysical.Expr.LocalId, metas: MetaContainer): ThunkEnv {
        TODO()
    }


    private fun compileGlobalId(expr: PartiqlPhysical.Expr.GlobalId): (Environment) -> ExprValue {
        TODO()
    }

    private fun compileParameter(expr: PartiqlPhysical.Expr.Parameter, metas: MetaContainer): ThunkEnv {
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

    private fun compileIs(expr: PartiqlPhysical.Expr.IsType, metas: MetaContainer): ThunkEnv {
        val expThunk = compileAstExpr(expr.value)
        val typedOpParameter = expr.type.toTypedOpParameter(customTypedOpParameters)
        if (typedOpParameter.staticType is AnyType) {
            return thunkFactory.thunkEnv(metas) { valueFactory.newBoolean(true) }
        }
        if (compileOptions.typedOpBehavior == TypedOpBehavior.HONOR_PARAMETERS && expr.type is PartiqlPhysical.Type.FloatType && expr.type.precision != null) {
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

    private fun compileCastHelper(value: PartiqlPhysical.Expr, asType: PartiqlPhysical.Type, metas: MetaContainer): ThunkEnv {
        val expThunk = compileAstExpr(value)
        val typedOpParameter = asType.toTypedOpParameter(customTypedOpParameters)
        if (typedOpParameter.staticType is AnyType) {
            return expThunk
        }
        if (compileOptions.typedOpBehavior == TypedOpBehavior.HONOR_PARAMETERS && asType is PartiqlPhysical.Type.FloatType && asType.precision != null) {
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

    private fun compileCast(expr: PartiqlPhysical.Expr.Cast, metas: MetaContainer): ThunkEnv =
        thunkFactory.thunkEnv(metas, compileCastHelper(expr.value, expr.asType, metas))

    private fun compileCanCast(expr: PartiqlPhysical.Expr.CanCast, metas: MetaContainer): ThunkEnv {
        val typedOpParameter = expr.asType.toTypedOpParameter(customTypedOpParameters)
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

    private fun compileCanLosslessCast(expr: PartiqlPhysical.Expr.CanLosslessCast, metas: MetaContainer): ThunkEnv {
        val typedOpParameter = expr.asType.toTypedOpParameter(customTypedOpParameters)
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

    private fun compileSimpleCase(expr: PartiqlPhysical.Expr.SimpleCase, metas: MetaContainer): ThunkEnv {
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

    private fun compileSearchedCase(expr: PartiqlPhysical.Expr.SearchedCase, metas: MetaContainer): ThunkEnv {
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

    private fun compileStruct(expr: PartiqlPhysical.Expr.Struct, metas: MetaContainer): ThunkEnv {
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

    private fun compileSeq(seqType: ExprValueType, itemExprs: List<PartiqlPhysical.Expr>, metas: MetaContainer): ThunkEnv {
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

    @Suppress("UNUSED_PARAMETER")
    private fun compileCallAgg(expr: PartiqlPhysical.Expr.CallAgg, metas: MetaContainer): ThunkEnv = TODO("call_agg")

    private fun compilePath(expr: PartiqlPhysical.Expr.Path, metas: MetaContainer): ThunkEnv {
        val rootThunk = compileAstExpr(expr.root)
        val remainingComponents = LinkedList<PartiqlPhysical.PathStep>()

        expr.steps.forEach { remainingComponents.addLast(it) }

        val componentThunk = compilePathComponents(remainingComponents, metas)

        return thunkFactory.thunkEnv(metas) { env ->
            val rootValue = rootThunk(env)
            componentThunk(env, rootValue)
        }
    }

    private fun compilePathComponents(
        remainingComponents: LinkedList<PartiqlPhysical.PathStep>,
        pathMetas: MetaContainer
    ): ThunkEnvValue<ExprValue> {

        val componentThunks = ArrayList<ThunkEnvValue<ExprValue>>()

        while (!remainingComponents.isEmpty()) {
            val pathComponent = remainingComponents.removeFirst()
            val componentMetas = pathComponent.metas
            componentThunks.add(
                when (pathComponent) {
                    is PartiqlPhysical.PathStep.PathExpr -> {
                        val indexExpr = pathComponent.index
                        val caseSensitivity = pathComponent.case
                        when {
                            //If indexExpr is a literal string, there is no need to evaluate it--just compile a
                            //thunk that directly returns a bound value
                            indexExpr is PartiqlPhysical.Expr.Lit && indexExpr.value.toIonValue(valueFactory.ion) is IonString -> {
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
                    is PartiqlPhysical.PathStep.PathUnpivot -> {
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
    private fun compileLike(expr: PartiqlPhysical.Expr.Like, metas: MetaContainer): ThunkEnv {
        val valueExpr = expr.value
        val patternExpr = expr.pattern
        val escapeExpr = expr.escape

        val patternLocationMeta = patternExpr.metas.toPartiQlMetaContainer().sourceLocation
        val escapeLocationMeta = escapeExpr?.metas?.toPartiQlMetaContainer()?.sourceLocation

        // Note that the return value is a nullable and deferred.
        // This is so that null short-circuits can be supported.
        fun getPatternParts(pattern: ExprValue, escape: ExprValue?): (() -> List<PatternPart>)? {
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
                    val patternParts = when {
                        patternString.isEmpty() -> emptyList()
                        else -> parsePattern(patternString, escapeChar)
                    }
                    return { patternParts }
                }
            }
        }

        fun runPatternParts(value: ExprValue, patternParts: (() -> List<PatternPart>)?): ExprValue {
            return when {
                patternParts == null || value.type.isUnknown -> valueFactory.nullValue
                !value.type.isText -> err(
                    "LIKE expression must be given non-null strings as input",
                    ErrorCode.EVALUATOR_LIKE_INVALID_INPUTS,
                    errorContextFrom(metas).also {
                        it[Property.LIKE_VALUE] = value.ionValue.toString()
                    },
                    internal = false
                )
                else -> valueFactory.newBoolean(executePattern(patternParts(), value.stringValue()))
            }
        }

        val valueThunk = compileAstExpr(valueExpr)

        // If the pattern and escape expressions are literals then we can compile the pattern now and
        // re-use it with every execution. Otherwise, we must re-compile the pattern every time.
        return when {
            patternExpr is PartiqlPhysical.Expr.Lit && (escapeExpr == null || escapeExpr is PartiqlPhysical.Expr.Lit) -> {
                val patternParts = getPatternParts(
                    valueFactory.newFromIonValue(patternExpr.value.toIonValue(valueFactory.ion)),
                    (escapeExpr as? PartiqlPhysical.Expr.Lit)?.value?.toIonValue(valueFactory.ion)
                        ?.let { valueFactory.newFromIonValue(it) })

                // If valueExpr is also a literal then we can evaluate this at compile time and return a constant.
                if (valueExpr is PartiqlPhysical.Expr.Lit) {
                    val resultValue = runPatternParts(
                        valueFactory.newFromIonValue(valueExpr.value.toIonValue(valueFactory.ion)),
                        patternParts
                    )
                    return thunkFactory.thunkEnv(metas) { resultValue }
                } else {
                    thunkFactory.thunkEnvOperands(metas, valueThunk) { _, value ->
                        runPatternParts(value, patternParts)
                    }
                }
            }
            else -> {
                val patternThunk = compileAstExpr(patternExpr)
                when (escapeExpr) {
                    null -> {
                        // thunk that re-compiles the DFA every evaluation without a custom escape sequence
                        thunkFactory.thunkEnvOperands(metas, valueThunk, patternThunk) { _, value, pattern ->
                            val pps = getPatternParts(pattern, null)
                            runPatternParts(value, pps)
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
                            val pps = getPatternParts(pattern, escape)
                            runPatternParts(value, pps)
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

    private fun compileDate(expr: PartiqlPhysical.Expr.Date, metas: MetaContainer): ThunkEnv =
        thunkFactory.thunkEnv(metas) {
            valueFactory.newDate(
                expr.year.value.toInt(),
                expr.month.value.toInt(),
                expr.day.value.toInt()
            )
        }

    private fun compileLitTime(expr: PartiqlPhysical.Expr.LitTime, metas: MetaContainer): ThunkEnv =
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
}

internal val MetaContainer.sourceLocationMeta get() = this[SourceLocationMeta.TAG] as? SourceLocationMeta

internal fun StaticType.getTypes() = when (val flattened = this.flatten()) {
    is AnyOfType -> flattened.types
    else -> listOf(this)
}
