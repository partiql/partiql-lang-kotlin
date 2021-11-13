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

import com.amazon.ion.IntegerSize
import com.amazon.ion.IonInt
import com.amazon.ion.IonSexp
import com.amazon.ion.IonString
import com.amazon.ion.IonValue
import com.amazon.ion.Timestamp
import org.partiql.lang.ast.AggregateCallSiteListMeta
import org.partiql.lang.ast.AggregateRegisterIdMeta
import org.partiql.lang.ast.AstDeserializerBuilder
import org.partiql.lang.ast.AstVersion
import org.partiql.lang.ast.CallAgg
import org.partiql.lang.ast.CaseSensitivity
import org.partiql.lang.ast.Coalesce
import org.partiql.lang.ast.CreateIndex
import org.partiql.lang.ast.CreateTable
import org.partiql.lang.ast.DataManipulation
import org.partiql.lang.ast.DateLiteral
import org.partiql.lang.ast.DropIndex
import org.partiql.lang.ast.DropTable
import org.partiql.lang.ast.Exec
import org.partiql.lang.ast.ExprNode
import org.partiql.lang.ast.FromSource
import org.partiql.lang.ast.FromSourceExpr
import org.partiql.lang.ast.FromSourceJoin
import org.partiql.lang.ast.FromSourceLet
import org.partiql.lang.ast.FromSourceUnpivot
import org.partiql.lang.ast.GroupBy
import org.partiql.lang.ast.GroupByItem
import org.partiql.lang.ast.GroupingStrategy
import org.partiql.lang.ast.IsCountStarMeta
import org.partiql.lang.ast.JoinOp
import org.partiql.lang.ast.LetSource
import org.partiql.lang.ast.Literal
import org.partiql.lang.ast.LiteralMissing
import org.partiql.lang.ast.MetaContainer
import org.partiql.lang.ast.NAry
import org.partiql.lang.ast.NAryOp
import org.partiql.lang.ast.NullIf
import org.partiql.lang.ast.Parameter
import org.partiql.lang.ast.Path
import org.partiql.lang.ast.PathComponent
import org.partiql.lang.ast.PathComponentExpr
import org.partiql.lang.ast.PathComponentUnpivot
import org.partiql.lang.ast.PathComponentWildcard
import org.partiql.lang.ast.ScopeQualifier
import org.partiql.lang.ast.SearchedCase
import org.partiql.lang.ast.Select
import org.partiql.lang.ast.SelectListItemExpr
import org.partiql.lang.ast.SelectListItemProjectAll
import org.partiql.lang.ast.SelectListItemStar
import org.partiql.lang.ast.SelectProjectionList
import org.partiql.lang.ast.SelectProjectionPivot
import org.partiql.lang.ast.SelectProjectionValue
import org.partiql.lang.ast.Seq
import org.partiql.lang.ast.SeqType
import org.partiql.lang.ast.SetQuantifier
import org.partiql.lang.ast.SimpleCase
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.Struct
import org.partiql.lang.ast.SymbolicName
import org.partiql.lang.ast.TimeLiteral
import org.partiql.lang.ast.Typed
import org.partiql.lang.ast.TypedOp
import org.partiql.lang.ast.UniqueNameMeta
import org.partiql.lang.ast.VariableReference
import org.partiql.lang.ast.staticType
import org.partiql.lang.ast.toAstExpr
import org.partiql.lang.ast.toAstStatement
import org.partiql.lang.ast.toAstType
import org.partiql.lang.ast.toExprNode
import org.partiql.lang.ast.toExprNodeSetQuantifier
import org.partiql.lang.ast.toPartiQlMetaContainer
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.errors.UNBOUND_QUOTED_IDENTIFIER_HINT
import org.partiql.lang.eval.binding.Alias
import org.partiql.lang.eval.binding.localsBinder
import org.partiql.lang.eval.builtins.storedprocedure.StoredProcedure
import org.partiql.lang.eval.like.PatternPart
import org.partiql.lang.eval.like.executePattern
import org.partiql.lang.eval.like.parsePattern
import org.partiql.lang.eval.time.Time
import org.partiql.lang.eval.visitors.PartiqlAstSanityValidator
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.types.AnyOfType
import org.partiql.lang.types.AnyType
import org.partiql.lang.types.CustomTypeFunction
import org.partiql.lang.types.FloatType
import org.partiql.lang.types.FunctionSignature
import org.partiql.lang.types.IntType
import org.partiql.lang.types.SingleType
import org.partiql.lang.types.StaticType
import org.partiql.lang.types.UnknownArguments
import org.partiql.lang.types.UnsupportedTypeCheckException
import org.partiql.lang.types.toStaticType
import org.partiql.lang.util.bigDecimalOf
import org.partiql.lang.util.case
import org.partiql.lang.util.checkThreadInterrupted
import org.partiql.lang.util.codePointSequence
import org.partiql.lang.util.compareTo
import org.partiql.lang.util.div
import org.partiql.lang.util.drop
import org.partiql.lang.util.foldLeftProduct
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
import java.math.BigDecimal
import java.util.LinkedList
import java.util.Stack
import java.util.TreeSet

/**
 * A basic compiler that converts an instance of [ExprNode] to an [Expression].
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
 * @param valueFactory An [ExprValueFactory] to generate [ExprValue]s.
 * @param functions A map of functions keyed by function name that will be available during compilation.
 * @param customTypeFunctions A map of custom type functions keyed by custom type name that will be available during compilation. Lookup is case-insensitive.
 * @param procedures A map of stored procedures keyed by procedure name that will be available during compilation.
 * @param compileOptions Various options that effect how the source code is compiled.
 */
internal class EvaluatingCompiler(
    private val valueFactory: ExprValueFactory,
    private val functions: Map<String, ExprFunction>,
    customTypeFunctions: Map<String, CustomTypeFunction>,
    private val procedures: Map<String, StoredProcedure>,
    private val compileOptions: CompileOptions = CompileOptions.standard()
) {
    private val errorSignaler = compileOptions.typingMode.createErrorSignaler(valueFactory)
    private val thunkFactory = compileOptions.typingMode.createThunkFactory(compileOptions, valueFactory)
    private val customTypeFunctionsCaseInsensitiveMap = customTypeFunctions.mapKeys { (k, _) -> k.toLowerCase() }

    private val compilationContextStack = Stack<CompilationContext>()

    private val currentCompilationContext: CompilationContext
        get() = compilationContextStack.peek() ?: throw EvaluationException(
            "compilationContextStack was empty.", ErrorCode.EVALUATOR_UNEXPECTED_VALUE, internal = true)

    //Note: please don't make this inline -- it messes up [EvaluationException] stack traces and
    //isn't a huge benefit because this is only used at SQL-compile time anyway.
    private fun <R> nestCompilationContext(expressionContext: ExpressionContext,
                                           fromSourceNames: Set<String>, block: () -> R): R {
        compilationContextStack.push(
            when {
                compilationContextStack.empty() -> CompilationContext(expressionContext, fromSourceNames)
                else -> compilationContextStack.peek().createNested(expressionContext,
                    fromSourceNames)
            })

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
        else -> errNoContext("Cannot convert number to expression value: $this", errorCode = ErrorCode.EVALUATOR_INVALID_CONVERSION, internal = true)
    }

    private fun Boolean.exprValue(): ExprValue = valueFactory.newBoolean(this)
    private fun String.exprValue(): ExprValue = valueFactory.newString(this)

    /** Represents an instance of a compiled `GROUP BY` expression and alias. */
    private class CompiledGroupByItem(val alias: ExprValue, val uniqueId: String?, val thunk: ThunkEnv)

    /**
     * Represents a memozied binding [BindingName] and an [ExprValue] of the same name.
     * Used during evaluation og `GROUP BY`.
     */
    private data class FromSourceBindingNamePair(val bindingName: BindingName, val nameExprValue: ExprValue)

    /**
     * Base class for [ExprAggregator] instances which accumulate values and perform a final computation.
     */
    private inner class Accumulator(
        var current: Number? = 0L,
        val nextFunc: (Number?, ExprValue) -> Number,
        val valueFilter: (ExprValue) -> Boolean = { _ -> true }
    ) : ExprAggregator {

        override fun next(value: ExprValue) {
            // skip the accumulation function if the value is unknown or if the value is filtered out
            if (value.isNotUnknown() && valueFilter.invoke(value)) {
                current = nextFunc(current, value)
            }
        }

        override fun compute() = current?.exprValue() ?: valueFactory.nullValue
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

    /** Dispatch table for built-in aggregate functions. */
    private val builtinAggregates: Map<Pair<String, SetQuantifier>, ExprAggregatorFactory> = {
        val countAccFunc: (Number?, ExprValue) -> Number = { curr, _ -> curr!! + 1L }
        val sumAccFunc: (Number?, ExprValue) -> Number = { curr, next ->
            curr?.let { it + next.numberValue() } ?: next.numberValue()
        }
        val minAccFunc = comparisonAccumulator { left, right -> left < right }
        val maxAccFunc = comparisonAccumulator { left, right -> left > right }

        val avgAggregateGenerator = { filter: (ExprValue) -> Boolean ->
            object : ExprAggregator {
                var sum: Number? = null
                var count = 0L

                override fun next(value: ExprValue) {
                    if (value.isNotUnknown() && filter.invoke(value)) {
                        sum = sum?.let { it + value.numberValue() } ?: value.numberValue()
                        count++
                    }
                }

                override fun compute() = sum?.let { (it / bigDecimalOf(count)).exprValue() } ?: valueFactory.nullValue
            }
        }

        val allFilter: (ExprValue) -> Boolean = { _ -> true }

        // each distinct ExprAggregator must get its own createUniqueExprValueFilter()

        mapOf(
            Pair("count", SetQuantifier.ALL) to ExprAggregatorFactory.over {
                Accumulator(0L, countAccFunc, allFilter)
            },

            Pair("count", SetQuantifier.DISTINCT) to ExprAggregatorFactory.over {
                Accumulator(0L, countAccFunc, createUniqueExprValueFilter())
            },

            Pair("sum", SetQuantifier.ALL) to ExprAggregatorFactory.over {
                Accumulator(null, sumAccFunc, allFilter)
            },

            Pair("sum", SetQuantifier.DISTINCT) to ExprAggregatorFactory.over {
                Accumulator(null, sumAccFunc, createUniqueExprValueFilter())
            },

            Pair("avg", SetQuantifier.ALL) to ExprAggregatorFactory.over {
                avgAggregateGenerator(allFilter)
            },

            Pair("avg", SetQuantifier.DISTINCT) to ExprAggregatorFactory.over {
                avgAggregateGenerator(createUniqueExprValueFilter())
            },

            Pair("max", SetQuantifier.ALL) to ExprAggregatorFactory.over {
                Accumulator(null, maxAccFunc, allFilter)
            },

            Pair("max", SetQuantifier.DISTINCT) to ExprAggregatorFactory.over {
                Accumulator(null, maxAccFunc, createUniqueExprValueFilter())
            },

            Pair("min", SetQuantifier.ALL) to ExprAggregatorFactory.over {
                Accumulator(null, minAccFunc, allFilter)
            },

            Pair("min", SetQuantifier.DISTINCT) to ExprAggregatorFactory.over {
                Accumulator(null, minAccFunc, createUniqueExprValueFilter())
            }
        )
    }()

    /**
     * Compiles an [ExprNode] tree to an [Expression].
     *
     * Checks [Thread.interrupted] before every expression and sub-expression is compiled
     * and throws [InterruptedException] if [Thread.interrupted] it has been set in the
     * hope that long running compilations may be aborted by the caller.
     */
    fun compile(originalAst: ExprNode): Expression {
        val visitorTransform = compileOptions.visitorTransformMode.createVisitorTransform()
        val transformedAst = visitorTransform.transformStatement(originalAst.toAstStatement()).toExprNode(valueFactory.ion)
        val partiqlAstSanityValidator = PartiqlAstSanityValidator()

        partiqlAstSanityValidator.validate(transformedAst.toAstStatement(), compileOptions)

        val thunk = nestCompilationContext(ExpressionContext.NORMAL, emptySet()) {
            compileExprNode(transformedAst)
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
        val parser = SqlParser(valueFactory.ion)
        // TODO: replace `parseExprNode` with `ParseStatement` once evaluator deprecates `ExprNode`
        val ast = parser.parseExprNode(source)
        return compile(ast)
    }

    /**
     * Evaluates a V0 s-exp based AST against a global set of bindings.
     */
    @Deprecated("Please use CompilerPipeline.compile(ExprNode).eval(EvaluationSession) instead.")
    fun eval(ast: IonSexp, session: EvaluationSession): ExprValue {
        val exprNode = AstDeserializerBuilder(valueFactory.ion).build().deserialize(ast, AstVersion.V0)
        return compile(exprNode).eval(session)
    }

    /**
     * Evaluates an instance of [ExprNode] against a global set of bindings.
     */
    fun eval(ast: ExprNode, session: EvaluationSession): ExprValue = compile(ast).eval(session)

    /**
     * Compiles the specified [ExprNode] into a [ThunkEnv].
     *
     * This function will [InterruptedException] if [Thread.interrupted] has been set.
     */
    private fun compileExprNode(expr: ExprNode): ThunkEnv {
        checkThreadInterrupted()
        return when (expr) {
            is Literal -> compileLiteral(expr)
            is LiteralMissing -> compileLiteralMissing(expr)
            is VariableReference -> compileVariableReference(expr)
            is NAry -> compileNAry(expr)
            is Typed -> compileTyped(expr)
            is SimpleCase -> compileSimpleCase(expr)
            is SearchedCase -> compileSearchedCase(expr)
            is Path -> compilePath(expr)
            is Struct -> compileStruct(expr)
            is Seq -> compileSeq(expr)
            is Select -> compileSelect(expr)
            is CallAgg -> compileCallAgg(expr)
            is Parameter -> compileParameter(expr)
            is NullIf -> compileNullIf(expr)
            is Coalesce -> compileCoalesce(expr)
            is DataManipulation -> err(
                "DML operations are not supported yet",
                ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET,
                errorContextFrom(expr.metas).also {
                    it[Property.FEATURE_NAME] = "DataManipulation.${expr.dmlOperations.ops.first().name}"
                }, internal = false
            )
            is CreateTable,
            is CreateIndex,
            is DropIndex,
            is DropTable -> compileDdl(expr)
            is Exec -> compileExec(expr)
            is DateLiteral -> compileDateLiteral(expr)
            is TimeLiteral -> compileTimeLiteral(expr)
        }
    }

    private fun compileNullIf(expr: NullIf): ThunkEnv {
        val (expr1, expr2, metas) = expr
        val expr1Thunk = compileExprNode(expr1)
        val expr2Thunk = compileExprNode(expr2)
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

    private fun compileCoalesce(expr: Coalesce): ThunkEnv {
        val (args, metas) = expr
        val argThunks = args.map { compileExprNode(it) }
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

    private fun compileNAry(expr: NAry): ThunkEnv {

        val (op, args, metas: MetaContainer) = expr

        fun argThunks() = args.map { compileExprNode(it) }

        val computeThunk = when (op) {
            NAryOp.ADD -> compileNAryAdd(argThunks(), metas)
            NAryOp.SUB -> compileNArySub(argThunks(), metas)
            NAryOp.MUL -> compileNAryMul(argThunks(), metas)
            NAryOp.DIV -> compileNAryDiv(argThunks(), metas)
            NAryOp.MOD -> compileNAryMod(argThunks(), metas)
            NAryOp.EQ -> compileNAryEq(argThunks(), metas)
            NAryOp.NE -> compileNAryNe(argThunks(), metas)
            NAryOp.LT -> compileNaryLt(argThunks(), metas)
            NAryOp.LTE -> compileNAryLte(argThunks(), metas)
            NAryOp.GT -> compileNAryGt(argThunks(), metas)
            NAryOp.GTE -> compileNAryGte(argThunks(), metas)
            NAryOp.BETWEEN -> compileNAryBetween(argThunks(), metas)
            NAryOp.LIKE -> compileNAryLike(args, argThunks(), metas)
            NAryOp.IN -> compileNAryIn(args, metas)
            NAryOp.NOT -> compileNAryNot(argThunks(), metas)
            NAryOp.AND -> compileNAryAnd(argThunks(), metas)
            NAryOp.OR -> compileNAryOr(argThunks(), metas)
            NAryOp.STRING_CONCAT -> compileNAryStringConcat(argThunks(), metas)
            NAryOp.CALL -> compileNAryCall(args, metas)

            NAryOp.INTERSECT,
            NAryOp.INTERSECT_ALL,
            NAryOp.EXCEPT,
            NAryOp.EXCEPT_ALL,
            NAryOp.UNION,
            NAryOp.UNION_ALL -> {
                err("NAryOp.$op is not yet supported",
                    ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET,
                    errorContextFrom(metas).also {
                        it[Property.FEATURE_NAME] = "NAryOp.$op"
                    }, internal = false)
            }
        }

        return when (val staticTypes = expr.metas.staticType?.type?.getTypes()) {
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
                                        { naryResult })
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
    }

    /**
     * Returns a function accepts an [ExprValue] as an argument and returns true it is is `NULL`, `MISSING`, or
     * within the range specified by [range].
     */
    private fun integerValueValidator(
        range: LongRange
    ): (ExprValue) -> Boolean = { value ->
        when (value.type) {
            ExprValueType.NULL, ExprValueType.MISSING -> true
            ExprValueType.INT -> {
                val longValue: Long = value.scalar.numberValue()?.toLong()
                    ?: error("ExprValue.numberValue() must not be `NULL` when its type is INT." +
                        "This indicates that the ExprValue instance has a bug.")

                // PRO-TIP:  make sure to use the `Long` primitive type here with `.contains` otherwise
                // Kotlin will use the version of `.contains` that treats [range] as a collection and it will
                // be very slow!
                range.contains(longValue)
            }
            else -> error(
                "The expression's static type was supposed to be INT but instead it was ${value.type}" +
                    "This may indicate the presence of a bug in the type inferencer.")
        }
    }

    private fun compileNAryAdd(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer): ThunkEnv {
        return when (argThunks.size) {
            //Unary +
            1 -> {
                val firstThunk = argThunks[0]
                thunkFactory.thunkEnvOperands(metas, firstThunk) { _, value ->
                    //Invoking .numberValue() here makes this essentially just a type check
                    value.numberValue()
                    //Original value is returned unmodified.
                    value
                }
            }
            //N-ary +
            else -> thunkFactory.thunkFold(metas, argThunks) { lValue, rValue ->
                (lValue.numberValue() + rValue.numberValue()).exprValue()
            }
        }
    }

    private fun compileNArySub(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer): ThunkEnv {
        return when (argThunks.size) {
            //Unary -
            1 -> {
                val firstThunk = argThunks[0]
                thunkFactory.thunkEnvOperands(metas, firstThunk) { _, value ->
                    (-value.numberValue()).exprValue()
                }
            }

            //N-ary -
            else -> thunkFactory.thunkFold(metas, argThunks) { lValue, rValue ->
                (lValue.numberValue() - rValue.numberValue()).exprValue()
            }
        }
    }

    private fun compileNAryMul(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer): ThunkEnv {
        return thunkFactory.thunkFold(metas, argThunks) { lValue, rValue ->
            (lValue.numberValue() * rValue.numberValue()).exprValue()
        }
    }

    private fun compileNAryDiv(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer): ThunkEnv {
        return thunkFactory.thunkFold(metas, argThunks) { lValue, rValue ->
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
                    throw EvaluationException(cause = e, errorCode = ErrorCode.EVALUATOR_ARITHMETIC_EXCEPTION, internal = true)
                }
            }
        }
    }

    private fun compileNAryMod(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer): ThunkEnv {
        return thunkFactory.thunkFold(metas, argThunks) { lValue, rValue ->
            val denominator = rValue.numberValue()
            if (denominator.isZero()) {
                err("% by zero", ErrorCode.EVALUATOR_MODULO_BY_ZERO, null, false)
            }

            (lValue.numberValue() % denominator).exprValue()
        }
    }

    private fun compileNAryEq(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer
    ): ThunkEnv = thunkFactory.thunkAndMap(metas, argThunks) { lValue, rValue ->
        (lValue.exprEquals(rValue))
    }

    private fun compileNAryNe(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer
    ): ThunkEnv = thunkFactory.thunkFold(metas, argThunks) { lValue, rValue ->
        ((!lValue.exprEquals(rValue)).exprValue())
    }

    private fun compileNaryLt(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer
    ): ThunkEnv =
        thunkFactory.thunkAndMap(metas, argThunks) { lValue, rValue -> lValue < rValue }

    private fun compileNAryLte(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer
    ): ThunkEnv =
        thunkFactory.thunkAndMap(metas, argThunks) { lValue, rValue -> lValue <= rValue }

    private fun compileNAryGt(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer
    ): ThunkEnv =
        thunkFactory.thunkAndMap(metas, argThunks) { lValue, rValue -> lValue > rValue }

    private fun compileNAryGte(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer
    ): ThunkEnv =
        thunkFactory.thunkAndMap(metas, argThunks) { lValue, rValue -> lValue >= rValue }

    private fun compileNAryBetween(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer
    ): ThunkEnv {
        val valueThunk = argThunks[0]
        val fromThunk = argThunks[1]
        val toThunk = argThunks[2]

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
    private fun compileNAryIn(args: List<ExprNode>, metas: MetaContainer): ThunkEnv {
        val leftThunk = compileExprNode(args[0])
        val rightOp = args[1]
        return when {
            // We can significantly optimize this if rightArg is a sequence constructor which is comprised of entirely
            // of non-null literal values.
            rightOp is Seq && rightOp.values.all { it is Literal && !it.ionValue.isNullValue } -> {
                // Put all the literals in the sequence into a pre-computed map to be checked later by the thunk.
                // If the left-hand value is one of these we can short-circuit with a result of TRUE.
                // This is the fastest possible case and allows for hundreds of literal values (or more) in the
                // sequence without a huge performance penalty.
                // NOTE: we cannot use a [HashSet<>] here because [ExprValue] does not implement [Object.hashCode] or
                // [Object.equals].
                val precomputedLiteralsMap = rightOp.values
                    .filterIsInstance<Literal>()
                    .mapTo(TreeSet<ExprValue>(DEFAULT_COMPARATOR)) { valueFactory.newFromIonValue(it.ionValue) };

                // the compiled thunk simply checks if the left side is contained in the right side.
                // thunkEnvOperands takes care of unknown propagation for the left side; for the right,
                // this unknown propagation does not apply since we've eliminated the possibility of unknowns above.
                thunkFactory.thunkEnvOperands(metas, leftThunk) { _, leftValue ->
                    precomputedLiteralsMap.contains(leftValue).exprValue()
                }
            }

            // The unoptimized case...
            else -> {
                val rightThunk = compileExprNode(rightOp)

                // Legacy mode:
                //      Returns FALSE when the right side of IN is not a sequence
                //      Returns NULL if the right side is MISSING or any value in the right side is MISSING
                // Permissive mode:
                //      Returns MISSING when the right side of IN is not a sequence
                //      Returns MISSING if the right side is MISSING or any value in the right side is MISSING
                val (propagateMissingAs, propagateNotASeqAs) = with(valueFactory) {
                    when (compileOptions.typingMode) {
                        TypingMode.LEGACY -> nullValue to newBoolean(false)
                        TypingMode.PERMISSIVE -> missingValue to missingValue
                    }
                }

                // Note that standard unknown propagation applies to the left and right operands (both [TypingMode]s
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

    private fun compileNAryNot(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer): ThunkEnv {
        return thunkFactory.thunkEnvOperands(metas, argThunks.first()) { _, value ->
            (!value.booleanValue()).exprValue()
        }
    }

    private fun compileNAryAnd(argThunks: List<ThunkEnv>, metas: MetaContainer): ThunkEnv =
    // can't use the null propagation supplied by [ThunkFactory.thunkEnv] here because AND short-circuits on
        // false values and *NOT* on NULL or MISSING
        when (compileOptions.typingMode) {
            TypingMode.LEGACY -> thunkFactory.thunkEnv(metas) thunk@{ env ->
                var hasUnknowns = false
                argThunks.forEach { currThunk ->
                    val currValue = currThunk(env)
                    when {
                        currValue.isUnknown() -> hasUnknowns = true
                        //Short circuit only if we encounter a known false value.
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
                        //Short circuit only if we encounter a known false value.
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

    private fun compileNAryOr(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer
    ): ThunkEnv =
    // can't use the null propagation supplied by [ThunkFactory.thunkEnv] here because OR short-circuits on
        // true values and *NOT* on NULL or MISSING
        when (compileOptions.typingMode) {
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
                        //Short circuit only if we encounter a known true value.
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

    private fun compileNAryStringConcat(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer): ThunkEnv {

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
                    internal = false)
            }
        }
    }

    private fun compileNAryCall(args: List<ExprNode>, metas: MetaContainer): ThunkEnv {

        // At this time, the first argument which evaluates to a reference to the
        // function to be invoked may only be a VariableReference because we are
        // looking up the function at compile-time without scoping rules.

        val funcExpr = args.first() as? VariableReference
            ?: err(
                "First argument of call must be a VariableReference",
                ErrorCode.INTERNAL_ERROR,
                errorContextFrom(metas),
                internal = true)

        val func = functions[funcExpr.id] ?: err(
            "No such function: ${funcExpr.id}",
            ErrorCode.EVALUATOR_NO_SUCH_FUNCTION,
            errorContextFrom(metas).also {
                it[Property.FUNCTION_NAME] = funcExpr.id
            },
            internal = false)

        val funcArgs = args.drop(1)

        // Check arity
        if (funcArgs.size !in func.signature.arity) {
            val errorContext = errorContextFrom(metas).also {
                it[Property.FUNCTION_NAME] = func.signature.name
                it[Property.EXPECTED_ARITY_MIN] = func.signature.arity.first
                it[Property.EXPECTED_ARITY_MAX] = func.signature.arity.last
                it[Property.ACTUAL_ARITY] = funcArgs.size
            }

            val message = when {
                func.signature.arity.first == 1 && func.signature.arity.last == 1 ->
                    "${func.signature.name} takes a single argument, received: ${funcArgs.size}"
                func.signature.arity.first == func.signature.arity.last ->
                    "${func.signature.name} takes exactly ${func.signature.arity.first} arguments, received: ${funcArgs.size}"
                else ->
                    "${func.signature.name} takes between ${func.signature.arity.first} and " +
                        "${func.signature.arity.last} arguments, received: ${funcArgs.size}"
            }

            throw EvaluationException(message,
                ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_FUNC_CALL,
                errorContext,
                internal = false)
        }

        // Compile the arguments
        val argThunks = funcArgs.map { compileExprNode(it) }

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

            signature.requiredParameters.zip(required).forEachIndexed() { idx, (expected, actual) ->
                checkArgumentType(expected, actual, idx + 1)
            }

            return if (signature.optionalParameter != null && !rest.isEmpty()) {
                val opt = rest.last()
                checkArgumentType(signature.optionalParameter, opt, required.size + 1)
                RequiredWithOptional(required, opt)
            } else if (signature.variadicParameter != null) {
                rest.forEachIndexed() { idx, arg ->
                    checkArgumentType(signature.variadicParameter.type, arg, required.size + 1 + idx)
                }
                RequiredWithVariadic(required, rest)
            } else {
                RequiredArgs(required)
            }
        }

        return when (func.signature.unknownArguments) {
            UnknownArguments.PROPAGATE -> thunkFactory.thunkEnvOperands(metas, argThunks) { env, values ->
                val checkedArgs = checkArgumentTypes(func.signature, values)
                func.call(env, checkedArgs)
            }
            UnknownArguments.PASS_THRU -> thunkFactory.thunkEnv(metas) { env ->
                val funcArgValues = argThunks.map { it(env) }
                val checkedArgs = checkArgumentTypes(func.signature, funcArgValues)
                func.call(env, checkedArgs)
            }
        }
    }

    private fun compileLiteral(expr: Literal): ThunkEnv {
        val (ionValue, metas: MetaContainer) = expr
        val value = valueFactory.newFromIonValue(ionValue)
        return thunkFactory.thunkEnv(metas) { value }
    }

    private fun compileLiteralMissing(expr: LiteralMissing): ThunkEnv {
        val (metas) = expr
        return thunkFactory.thunkEnv(metas) { _ -> valueFactory.missingValue }
    }

    private fun compileVariableReference(expr: VariableReference): ThunkEnv {
        val (id, case, lookupStrategy, metas: MetaContainer) = expr
        val uniqueNameMeta = expr.metas.find(UniqueNameMeta.TAG) as? UniqueNameMeta

        val fromSourceNames = currentCompilationContext.fromSourceNames

        return when (uniqueNameMeta) {
            null -> {
                val bindingName = BindingName(id, case.toBindingCase())
                val evalVariableReference = when (compileOptions.undefinedVariable) {
                    UndefinedVariableBehavior.ERROR ->
                        thunkFactory.thunkEnv(metas) { env ->
                            when (val value = env.current[bindingName]) {
                                null -> {
                                    if (fromSourceNames.any { bindingName.isEquivalentTo(it) }) {
                                        throw EvaluationException(
                                            "Variable not in GROUP BY or aggregation function: ${bindingName.name}",
                                            ErrorCode.EVALUATOR_VARIABLE_NOT_INCLUDED_IN_GROUP_BY,
                                            errorContextFrom(metas).also { it[Property.BINDING_NAME] = bindingName.name },
                                            internal = false)
                                    } else {
                                        val (errorCode, hint) = when (expr.case) {
                                            CaseSensitivity.SENSITIVE ->
                                                Pair(ErrorCode.EVALUATOR_QUOTED_BINDING_DOES_NOT_EXIST,
                                                    " $UNBOUND_QUOTED_IDENTIFIER_HINT")
                                            CaseSensitivity.INSENSITIVE ->
                                                Pair(ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST, "")
                                        }
                                        throw EvaluationException(
                                            "No such binding: ${bindingName.name}.$hint",
                                            errorCode,
                                            errorContextFrom(metas).also { it[Property.BINDING_NAME] = bindingName.name },
                                            internal = false)
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

                when (lookupStrategy) {
                    ScopeQualifier.UNQUALIFIED -> evalVariableReference
                    ScopeQualifier.LEXICAL -> thunkFactory.thunkEnv(metas) { env ->
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
                        internal = true)
                }
            }
        }
    }

    private fun compileParameter(expr: Parameter): ThunkEnv {
        val (ordinal, metas: MetaContainer) = expr
        val index = ordinal - 1

        return { env ->
            val params = env.session.parameters
            if (params.size <= index) {
                throw EvaluationException(
                    "Unbound parameter for ordinal: ${ordinal}",
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

    private fun compileTyped(expr: Typed): ThunkEnv {
        val (op, exp, dataType, metas: MetaContainer) = expr
        val expThunk = compileExprNode(exp)
        val astType = dataType.toAstType()
        val staticType = astType.toStaticType(customTypeFunctionsCaseInsensitiveMap)

        /**
         * Validates the [ExprValue] based on the `astType`. When the `astType` is not a [PartiqlAst.Type.CustomType] it always returns [null].
         */
        val validateExprValue = when (astType) {
            is PartiqlAst.Type.CustomType ->
                // Case-insensitive lookup
                customTypeFunctionsCaseInsensitiveMap[astType.name.text.toLowerCase()]?.validateExprValue
            else -> null
        }

        when (staticType) {
            is SingleType -> {}
            is AnyType -> {
                // return trivial results for operations against ANY
                return when (op) {
                    TypedOp.CAST -> expThunk
                    TypedOp.CAN_CAST, TypedOp.CAN_LOSSLESS_CAST, TypedOp.IS ->
                        thunkFactory.thunkEnv(metas) { valueFactory.newBoolean(true) }
                }
            }
        }

        when (compileOptions.typedOpBehavior) {
            TypedOpBehavior.LEGACY -> {
                // no validation needed since type parameters are not honored in this mode anyway
            }
            TypedOpBehavior.HONOR_PARAMETERS -> {
                when(staticType) {
                    is FloatType ->
                        // check if FLOAT has been given an argument--throw exception since we do not honor it.
                        if (expr.type.args.any()) {
                            err(
                                "FLOAT precision parameter is unsupported",
                                ErrorCode.SEMANTIC_FLOAT_PRECISION_UNSUPPORTED,
                                errorContextFrom(expr.type.metas),
                                internal = false)
                        }
                    else -> {
                        // no validation needed since we honor all other relevant type parameters.
                    }
                }
            }
        }

        when (staticType) {
            is AnyType -> {
                // return trivial results for operations against ANY
                return when (op) {
                    TypedOp.CAST -> expThunk
                    TypedOp.CAN_CAST, TypedOp.CAN_LOSSLESS_CAST, TypedOp.IS ->
                        thunkFactory.thunkEnv(metas) { valueFactory.newBoolean(true) }
                }
            }
            is AnyOfType,
            is SingleType -> {
            }
        }

        fun typeOpValidate(value: ExprValue,
                           castOutput: ExprValue,
                           typeName: String,
                           locationMeta: SourceLocationMeta?) {
            if (validateExprValue?.let { it(castOutput) } == false) {
                val errorContext = PropertyValueMap().also {
                    it[Property.CAST_FROM] = value.type.toString()
                    it[Property.CAST_TO] = typeName
                }

                locationMeta?.let { fillErrorContext(errorContext, it) }

                throw EvaluationException(
                    "Validation failure for ${dataType.sqlDataType}",
                    ErrorCode.EVALUATOR_CAST_FAILED,
                    errorContext,
                    internal = false
                )
            }
        }

        fun singleTypeCastFunc(singleType: SingleType): CastFunc {
            val locationMeta = metas.sourceLocationMeta
            return { value ->
                val castOutput = value.cast(singleType, valueFactory, compileOptions.typedOpBehavior, locationMeta, compileOptions.defaultTimezoneOffset)
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
            // Should not be possible
            is AnyType -> throw IllegalStateException("ANY type is not configured correctly in compiler")
        }

        return when (op) {
            TypedOp.IS -> compileTypedIs(metas, expThunk, staticType, validateExprValue)

            // using thunkFactory here includes the optional evaluation-time type check
            TypedOp.CAST -> thunkFactory.thunkEnv(metas, compileCast(staticType))

            TypedOp.CAN_CAST -> {
                // TODO consider making this more efficient by not directly delegating to CAST
                // TODO consider also making the operand not double evaluated (e.g. having expThunk memoize)
                val castThunkEnv = compileCast(staticType)
                thunkFactory.thunkEnv(metas) { env ->
                    val sourceValue = expThunk(env)
                    try {
                        when {
                            // NULL/MISSING can cast to anything as themselves
                            sourceValue.isUnknown() -> valueFactory.newBoolean(true)
                            else -> {
                                val castedValue = castThunkEnv(env)
                                when {
                                    // NULL/MISSING from cast is permissive way to signal failure
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
            TypedOp.CAN_LOSSLESS_CAST -> {
                // TODO consider making this more efficient by not directly delegating to CAST
                val castThunkEnv = compileCast(staticType)
                thunkFactory.thunkEnv(metas) { env ->
                    val sourceValue = expThunk(env)
                    val sourceType = StaticType.fromExprValue(sourceValue)

                    fun roundTrip(): ExprValue {
                        val castedValue = castThunkEnv(env)

                        val locationMeta = metas.sourceLocationMeta
                        fun castFunc(singleType: SingleType) =
                            { value: ExprValue -> value.cast(singleType, valueFactory, compileOptions.typedOpBehavior, locationMeta, compileOptions.defaultTimezoneOffset) }

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
                            ExprValueType.TIMESTAMP -> when(staticType) {
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
        }
    }

    /**
     * Returns a lambda that implements the `IS` operator type check according to the current [TypedOpBehavior].
     * [staticType] is the type to be checked for a given exprValue.
     * [validateExprValue] validates the exprValue.
     */
    private fun makeIsCheck(
        staticType: SingleType,
        validateExprValue: ((ExprValue) -> Boolean)?,
        metas: MetaContainer
    ): (ExprValue) -> Boolean {
        val exprValueType = staticType.runtimeType

        // The "simple" type match function only looks at the [ExprValueType] of the [ExprValue]
        // and invokes the custom [validateExprValue].
        val simpleTypeMatchFunc = { exprValue: ExprValue ->
            val isTypeMatch = when (exprValueType) {
                // MISSING IS NULL and NULL IS MISSING
                ExprValueType.NULL -> exprValue.type.isUnknown
                else -> exprValue.type == exprValueType
            }
            (isTypeMatch && validateExprValue?.let { it(exprValue) } != false)
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
                            internal = true)
                    }

                    when {
                        !matchesStaticType -> false
                        else -> when (validateExprValue) {
                            null -> true
                            else -> validateExprValue(expValue)
                        }
                    }
                }
            }
        }
    }

    private fun compileTypedIs(
        metas: MetaContainer,
        expThunk: ThunkEnv,
        staticType: StaticType,
        validateExprValue: ((ExprValue) -> Boolean)?
    ): ThunkEnv {
        val typeMatchFunc = when (staticType){
            is SingleType -> makeIsCheck(staticType, validateExprValue, metas)
            is AnyOfType -> staticType.types.map { childType ->
                when (childType) {
                    is SingleType -> makeIsCheck(childType, validateExprValue, metas)
                    else -> err(
                        "Union type cannot have ANY or nested AnyOf type for IS",
                        ErrorCode.SEMANTIC_UNION_TYPE_INVALID,
                        errorContextFrom(metas),
                        internal = true)
                }
            }.let { typeMatchFuncs ->
                { expValue: ExprValue -> typeMatchFuncs.any { func -> func(expValue) } }
            }
            // Should never happen because we short circuit early
            is AnyType -> throw IllegalStateException("Unexpected ANY type in IS compilation")
        }

        return thunkFactory.thunkEnv(metas) { env ->
            val expValue = expThunk(env)
            typeMatchFunc(expValue).exprValue()
        }
    }

    private fun compileSimpleCase(expr: SimpleCase): ThunkEnv {
        val (valueExpr, branches, elseExpr, metas: MetaContainer) = expr
        val valueThunk = compileExprNode(valueExpr)

        val elseThunk = when {
            elseExpr != null -> compileExprNode(elseExpr)
            else -> thunkFactory.thunkEnv(metas) { _ -> valueFactory.nullValue }
        }

        val branchThunks = branches.map { Pair(compileExprNode(it.valueExpr), compileExprNode(it.thenExpr)) }
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
                                    val thenValue = bt.second(env)
                                    return@thunk thenValue
                                }
                            }
                        }
                    }
                }
            }
            elseThunk(env)
        }
    }

    private fun compileSearchedCase(expr: SearchedCase): ThunkEnv {
        val (whenClauses, elseExpr, metas: MetaContainer) = expr

        val elseThunk = when {
            elseExpr != null -> compileExprNode(elseExpr)
            else -> thunkFactory.thunkEnv(metas) { _ -> valueFactory.nullValue }
        }

        val branchThunks = whenClauses.map { compileExprNode(it.condition) to compileExprNode(it.thenExpr) }

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

    private fun compileStruct(expr: Struct): ThunkEnv {
        val (fields, metas: MetaContainer) = expr

        class StructFieldThunks(val nameThunk: ThunkEnv, val valueThunk: ThunkEnv)

        val fieldThunks = fields.map {
            val (nameExpr, valueExpr) = it
            StructFieldThunks(compileExprNode(nameExpr), compileExprNode(valueExpr))
        }

        return when (compileOptions.typingMode) {
            TypingMode.LEGACY -> thunkFactory.thunkEnv(metas) { env ->
                val seq = fieldThunks.map {
                    val nameValue = it.nameThunk(env)
                    if (!nameValue.type.isText) {
                        // Evaluation time error where variable reference might be evaluated to non-text struct field.
                        err("Found struct field key to be of type ${nameValue.type}",
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

    private fun compileSeq(expr: Seq): ThunkEnv {
        val (seqType, items, metas: MetaContainer) = expr
        val itemThunks = items.map { compileExprNode(it) }.asSequence()

        val type = when (seqType) {
            SeqType.SEXP -> ExprValueType.SEXP
            SeqType.LIST -> ExprValueType.LIST
            SeqType.BAG -> ExprValueType.BAG
        }

        val makeItemThunkSequence = when (type) {
            ExprValueType.BAG -> { env: Environment ->
                itemThunks.map { itemThunk ->
                    // call to unnamedValue() makes sure we don't expose any underlying value name/ordinal
                    itemThunk(env).unnamedValue()
                }
            }
            else -> { env: Environment ->
                itemThunks.mapIndexed { i, itemThunk -> itemThunk(env).namedValue(i.exprValue()) }
            }
        }

        return thunkFactory.thunkEnv(metas) { env ->
            // todo:  use valueFactory.newSequence() instead.
            SequenceExprValue(
                valueFactory.ion,
                type,
                makeItemThunkSequence(env))
        }
    }

    private fun evalLimit(limitThunk: ThunkEnv, env: Environment, limitLocationMeta: SourceLocationMeta?): Long {
        val limitExprValue = limitThunk(env)

        if (limitExprValue.type != ExprValueType.INT) {
            err("LIMIT value was not an integer",
                ErrorCode.EVALUATOR_NON_INT_LIMIT_VALUE,
                errorContextFrom(limitLocationMeta).also {
                    it[Property.ACTUAL_TYPE] = limitExprValue.type.toString()
                },
                internal = false)
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
            err("IntegerSize.BIG_INTEGER not supported for LIMIT values",
                ErrorCode.INTERNAL_ERROR,
                errorContextFrom(limitLocationMeta),
                internal = true)
        }

        val limitValue = limitExprValue.numberValue().toLong()

        if (limitValue < 0) {
            err("negative LIMIT",
                ErrorCode.EVALUATOR_NEGATIVE_LIMIT,
                errorContextFrom(limitLocationMeta),
                internal = false)
        }

        // we can't use the Kotlin's Sequence<T>.take(n) for this since it accepts only an integer.
        // this references [Sequence<T>.take(count: Long): Sequence<T>] defined in [org.partiql.util].
        return limitValue
    }

    private fun evalOffset(offsetThunk: ThunkEnv, env: Environment, offsetLocationMeta: SourceLocationMeta?): Long {
        val offsetExprValue = offsetThunk(env)

        if (offsetExprValue.type != ExprValueType.INT) {
            err("OFFSET value was not an integer",
                ErrorCode.EVALUATOR_NON_INT_OFFSET_VALUE,
                errorContextFrom(offsetLocationMeta).also {
                    it[Property.ACTUAL_TYPE] = offsetExprValue.type.toString()
                },
                internal = false)
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
            err("IntegerSize.BIG_INTEGER not supported for OFFSET values",
                ErrorCode.INTERNAL_ERROR,
                errorContextFrom(offsetLocationMeta),
                internal = true)
        }

        val offsetValue = offsetExprValue.numberValue().toLong()

        if (offsetValue < 0) {
            err("negative OFFSET",
                ErrorCode.EVALUATOR_NEGATIVE_OFFSET,
                errorContextFrom(offsetLocationMeta),
                internal = false)
        }

        return offsetValue
    }

    private fun compileSelect(selectExpr: Select): ThunkEnv {
        selectExpr.orderBy?.let {
            err("ORDER BY is not supported in evaluator yet",
                ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET,
                errorContextFrom(selectExpr.metas).also { it[Property.FEATURE_NAME] = "ORDER BY" },
                internal = false)
        }

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

        val pigGeneratedAst = selectExpr.toAstExpr() as PartiqlAst.Expr.Select
        val allFromSourceAliases = fold.walkFromSource(pigGeneratedAst.from, emptySet())
            .union(pigGeneratedAst.fromLet?.let { fold.walkLet(pigGeneratedAst.fromLet, emptySet()) } ?: emptySet())

        return nestCompilationContext(ExpressionContext.NORMAL, emptySet()) {
            val (setQuantifier, projection, from, fromLet, _, groupBy, having, _, limit, offset, metas: MetaContainer) = selectExpr

            val fromSourceThunks = compileFromSources(from)
            val letSourceThunks = fromLet?.let { compileLetSources(it) }
            val sourceThunks = compileQueryWithoutProjection(selectExpr, fromSourceThunks, letSourceThunks)

            val offsetThunk = offset?.let { compileExprNode(it) }
            val offsetLocationMeta = offset?.metas?.sourceLocationMeta
            val limitThunk = limit?.let { compileExprNode(it) }
            val limitLocationMeta = limit?.metas?.sourceLocationMeta

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
                val (_, groupByItems, groupAsName) = groupBy ?: GroupBy(GroupingStrategy.FULL, listOf())

                val aggregateListMeta = selectExpr.metas.find(AggregateCallSiteListMeta.TAG) as AggregateCallSiteListMeta?
                val hasAggregateCallSites = aggregateListMeta?.aggregateCallSites?.any() ?: false

                val queryThunk = when {
                    groupByItems.isEmpty() && !hasAggregateCallSites ->
                        // Grouping is not needed -- simply project the results from the FROM clause directly.
                        thunkFactory.thunkEnv(metas) { env ->

                            val projectedRows = sourceThunks(env).map { (joinedValues, projectEnv) ->
                                selectProjectionThunk(projectEnv, joinedValues)
                            }

                            val quantifiedRows = when (setQuantifier) {
                                // wrap the ExprValue to use ExprValue.equals as the equality
                                SetQuantifier.DISTINCT -> projectedRows.filter(createUniqueExprValueFilter())
                                SetQuantifier.ALL -> projectedRows
                            }.let { rowsWithOffsetAndLimit(it, env) }

                            valueFactory.newBag(quantifiedRows.map {
                                // TODO make this expose the ordinal for ordered sequences
                                // make sure we don't expose the underlying value's name out of a SELECT
                                it.unnamedValue()
                            })
                        }
                    else -> {
                        // Grouping is needed

                        class CompiledAggregate(val factory: ExprAggregatorFactory, val argThunk: ThunkEnv)

                        // These aggregate call sites are collected in [AggregateSupportVisitorTransform].
                        val compiledAggregates = aggregateListMeta?.aggregateCallSites?.map { it ->
                            val funcName = it.funcName.text
                            CompiledAggregate(
                                factory = getAggregatorFactory(funcName, it.setq.toExprNodeSetQuantifier(), it.metas.toPartiQlMetaContainer()),
                                argThunk = compileExprNode(it.arg.toExprNode(valueFactory.ion)))
                        }

                        // This closure will be invoked to create and initialize a [RegisterBank] for new [Group]s.
                        val createRegisterBank: () -> RegisterBank = when (aggregateListMeta) {
                            // If there are no aggregates, create an empty register bank
                            null -> { -> // -> here forces this block to be a lambda
                                RegisterBank(0)
                            }
                            else -> { ->
                                RegisterBank(aggregateListMeta.aggregateCallSites.size).apply {
                                    //set up aggregate registers
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
                                    val fromProductions: Sequence<FromProduction> = rowsWithOffsetAndLimit(sourceThunks(env), env)
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
                                        listOf(syntheticGroup.key))


                                    valueFactory.newBag(listOf(groupResult).asSequence())
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
                                        it.alias.asName.exprValue())
                                }

                                val havingThunk = having?.let { compileExprNode(it) }

                                val filterHavingAndProject: (Environment, Group) -> ExprValue? =
                                    createFilterHavingAndProjectClosure(havingThunk, selectProjectionThunk)

                                val getGroupEnv: (Environment, Group) -> Environment = createGetGroupEnvClosure(groupAsName)

                                thunkFactory.thunkEnv(metas) { env ->
                                    // Execute the FROM clause
                                    val fromProductions: Sequence<FromProduction> = sourceThunks(env)

                                    // For each "row" in the output of the FROM clause
                                    fromProductions.forEach { fromProduction ->

                                        //Determine the group key for this value
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
                                                (fromProduction.env.current[pair.bindingName] ?: errNoContext(
                                                    "Could not resolve from source binding name during group as variable mapping",
                                                    errorCode = ErrorCode.INTERNAL_ERROR,
                                                    internal = true)).namedValue(pair.nameExprValue)
                                            }.asSequence()

                                            group.groupValues.add(createStructExprValue(seq, StructOrdering.UNORDERED))
                                        }
                                    }

                                    // generate the final group by projection
                                    val projectedRows = env.groups.mapNotNull { g ->
                                        val groupByEnv = getGroupEnv(env, g.value)
                                        filterHavingAndProject(groupByEnv, g.value)
                                    }.asSequence().let { rowsWithOffsetAndLimit(it, env) }

                                    valueFactory.newBag(projectedRows)
                                }
                            }
                        }
                    }
                } // do normal map/filter

                return thunkFactory.thunkEnv(metas) { env ->
                    queryThunk(env.nestQuery())
                }
            } // end of getQueryThunk(...)

            when (projection) {
                is SelectProjectionValue -> {
                    val (valueExpr) = projection
                    nestCompilationContext(ExpressionContext.NORMAL, allFromSourceAliases) {
                        val valueThunk = compileExprNode(valueExpr)
                        getQueryThunk(thunkFactory.thunkEnvValueList(projection.metas) { env, _ -> valueThunk(env) })
                    }
                }
                is SelectProjectionPivot -> {
                    val (asExpr, atExpr) = projection
                    nestCompilationContext(ExpressionContext.NORMAL, allFromSourceAliases) {
                        val asThunk = compileExprNode(asExpr)
                        val atThunk = compileExprNode(atExpr)
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
                is SelectProjectionList -> {
                    val (items) = projection
                    nestCompilationContext(ExpressionContext.SELECT_LIST, allFromSourceAliases) {
                        val projectionThunk: ThunkEnvValue<List<ExprValue>> =
                            when {
                                items.filterIsInstance<SelectListItemStar>().any() -> {
                                    errNoContext("Encountered a SelectListItemStar--did SelectStarVisitorTransform execute?",
                                        errorCode = ErrorCode.INTERNAL_ERROR,
                                        internal = true)
                                }
                                else -> {
                                    val projectionElements =
                                        compileSelectListToProjectionElements(projection)

                                    val ordering = if (projection.items.none { it is SelectListItemProjectAll })
                                        StructOrdering.ORDERED
                                    else
                                        StructOrdering.UNORDERED

                                    thunkFactory.thunkEnvValueList(projection.metas) { env, _ ->
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
                                                            val valuesToProject = when (compileOptions.projectionIteration) {
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
            }
        }
    }

    private fun compileGroupByExpressions(groupByItems: List<GroupByItem>): List<CompiledGroupByItem> =
        groupByItems.map {
            val alias = it.asName
                ?: errNoContext("GroupByItem.asName was not specified", errorCode = ErrorCode.INTERNAL_ERROR, internal = true)
            val uniqueName = (alias.metas.find(UniqueNameMeta.TAG) as UniqueNameMeta?)?.uniqueName

            CompiledGroupByItem(alias.name.exprValue(), uniqueName, compileExprNode(it.expr))
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

    /**
     * Returns a closure which creates an [Environment] for the specified [Group].
     * If a GROUP AS name was specified, also nests that [Environment] in another that
     * has a binding for the GROUP AS name.
     */
    private fun createGetGroupEnvClosure(groupAsName: SymbolicName?): (Environment, Group) -> Environment =
        when {
            groupAsName != null -> { groupByEnv, currentGroup ->
                val groupAsBindings = Bindings.buildLazyBindings<ExprValue> {
                    addBinding(groupAsName.name) {
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
                //Create a closure that simply performs the final projection and
                // returns the result.
                selectProjectionThunk(groupByEnv, listOf(currentGroup.key))
            }
        }

    private fun compileCallAgg(expr: CallAgg): ThunkEnv {
        val (funcExpr, setQuantifier, argExpr, metas: MetaContainer) = expr

        if (metas.hasMeta(IsCountStarMeta.TAG) && currentCompilationContext.expressionContext != ExpressionContext.SELECT_LIST) {
            err("COUNT(*) is not allowed in this context", ErrorCode.EVALUATOR_COUNT_START_NOT_ALLOWED, errorContextFrom(metas), internal = false)
        }

        val funcVarRef = funcExpr as VariableReference  // PartiqlAstSanityValidator ensures this cast will succeed

        val aggFactory = getAggregatorFactory(funcVarRef.id.toLowerCase(), setQuantifier, metas)

        val argThunk = nestCompilationContext(ExpressionContext.AGG_ARG, emptySet()) {
            compileExprNode(argExpr)
        }

        return when (currentCompilationContext.expressionContext) {
            ExpressionContext.AGG_ARG -> {
                err("The arguments of an aggregate function cannot contain aggregate functions",
                    ErrorCode.EVALUATOR_INVALID_ARGUMENTS_FOR_AGG_FUNCTION,
                    errorContextFrom(metas),
                    internal = false)
            }
            ExpressionContext.NORMAL ->
                thunkFactory.thunkEnv(metas) { env ->
                    val aggregator = aggFactory.create()
                    val argValue = argThunk(env)
                    argValue.forEach { aggregator.next(it) }
                    aggregator.compute()
                }
            ExpressionContext.SELECT_LIST -> {
                val registerIdMeta = metas.find(AggregateRegisterIdMeta.TAG) as AggregateRegisterIdMeta
                val registerId = registerIdMeta.registerId
                thunkFactory.thunkEnv(metas) { env ->
                    // Note: env.currentGroup must be set by caller.
                    val registers = env.currentGroup?.registers
                        ?: err("No current group or current group has no registers",
                            ErrorCode.INTERNAL_ERROR,
                            errorContextFrom(metas),
                            internal = true)

                    registers[registerId].aggregator.compute()
                }
            }
        }
    }

    fun getAggregatorFactory(funcName: String, setQuantifier: SetQuantifier, metas: MetaContainer): ExprAggregatorFactory {
        val key = funcName.toLowerCase() to setQuantifier

        return builtinAggregates[key] ?: err("No such function: $funcName",
            ErrorCode.EVALUATOR_NO_SUCH_FUNCTION,
            errorContextFrom(metas).also { it[Property.FUNCTION_NAME] = funcName },
            internal = false)
    }

    private fun compileFromSources(
        fromSource: FromSource,
        sources: MutableList<CompiledFromSource> = ArrayList(),
        joinExpansion: JoinExpansion = JoinExpansion.INNER,
        conditionThunk: ThunkEnv? = null
    ): List<CompiledFromSource> {

        val metas = fromSource.metas()

        when (fromSource) {
            is FromSourceLet -> case {
                val thunk = when (fromSource) {
                    is FromSourceExpr -> {
                        compileExprNode(fromSource.expr)
                    }
                    is FromSourceUnpivot -> {
                        val valueThunk = compileExprNode(fromSource.expr)
                        thunkFactory.thunkEnv(metas) { env -> valueThunk(env).unpivot() }
                    }
                }
                sources.add(
                    CompiledFromSource(
                        alias = Alias(
                            asName = fromSource.variables.asName?.name
                                ?: err("FromSourceExpr.variables.asName was null",
                                    ErrorCode.INTERNAL_ERROR,
                                    errorContextFrom(fromSource.expr.metas), internal = true),
                            atName = fromSource.variables.atName?.name,
                            byName = fromSource.variables.byName?.name),
                        thunk = thunk,
                        joinExpansion = joinExpansion,
                        filter = conditionThunk))
            }
            is FromSourceJoin -> case {

                val (joinOp, left, right, condition, _: MetaContainer) = fromSource

                val leftSources = compileFromSources(left)
                sources.addAll(leftSources)

                val joinExpansionInner = when (joinOp) {
                    JoinOp.INNER -> JoinExpansion.INNER
                    JoinOp.LEFT -> JoinExpansion.OUTER
                    JoinOp.RIGHT, JoinOp.OUTER -> err("RIGHT and FULL JOIN not supported",
                        ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET,
                        errorContextFrom(metas).also {
                            it[Property.FEATURE_NAME] = "RIGHT and FULL JOIN"
                        },
                        internal = false)
                }
                val conditionThunkInner = compileExprNode(condition)

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

        }.toUnit()

        return sources
    }

    private fun compileLetSources(letSource: LetSource): List<CompiledLetSource> =
        letSource.bindings.map {
            CompiledLetSource(name = it.name.name, thunk = compileExprNode(it.expr))
        }

    /**
     * Compiles the clauses of the SELECT or PIVOT into a thunk that does not generate
     * the final projection.
     */
    private fun compileQueryWithoutProjection(
        ast: Select,
        compiledSources: List<CompiledFromSource>,
        compiledLetSources: List<CompiledLetSource>?
    ): (Environment) -> Sequence<FromProduction> {

        val localsBinder = compiledSources.map { it.alias }.localsBinder(valueFactory.missingValue)
        val whereThunk = ast.where?.let { compileExprNode(it) }

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
                                Environment.CurrentMode.GLOBALS_THEN_LOCALS)
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
        selectList: SelectProjectionList
    ): List<ProjectionElement> =
        selectList.items.mapIndexed { idx, it ->
            when (it) {
                is SelectListItemStar -> {
                    errNoContext("Encountered a SelectListItemStar--did SelectStarVisitorTransform execute?",
                        errorCode = ErrorCode.INTERNAL_ERROR,
                        internal = true)
                }
                is SelectListItemExpr -> {
                    val (itemExpr, asName) = it
                    val alias = asName?.name ?: itemExpr.extractColumnAlias(idx)
                    val thunk = compileExprNode(itemExpr)
                    SingleProjectionElement(valueFactory.newString(alias), thunk)
                }
                is SelectListItemProjectAll -> {
                    MultipleProjectionElement(listOf(compileExprNode(it.expr)))
                }
            }
        }

    private fun compilePath(expr: Path): ThunkEnv {
        val (root, components, metas) = expr
        val rootThunk = compileExprNode(root)
        val remainingComponents = LinkedList<PathComponent>()
        components.forEach { remainingComponents.addLast(it) }

        val componentThunk = compilePathComponents(metas, remainingComponents)

        return thunkFactory.thunkEnv(metas) { env ->
            val rootValue = rootThunk(env)
            componentThunk(env, rootValue)
        }
    }

    private fun compilePathComponents(
        pathMetas: MetaContainer,
        remainingComponents: LinkedList<PathComponent>
    ): ThunkEnvValue<ExprValue> {

        val componentThunks = ArrayList<ThunkEnvValue<ExprValue>>()

        while (!remainingComponents.isEmpty()) {
            val pathComponent = remainingComponents.removeFirst()
            val componentMetas = pathComponent.metas
            componentThunks.add(
                when (pathComponent) {
                    is PathComponentExpr -> {
                        val (indexExpr, caseSensitivity) = pathComponent
                        when {
                            //If indexExpr is a literal string, there is no need to evaluate it--just compile a
                            //thunk that directly returns a bound value
                            indexExpr is Literal && indexExpr.ionValue is IonString -> {
                                val lookupName = BindingName(indexExpr.ionValue.stringValue(), caseSensitivity.toBindingCase())
                                thunkFactory.thunkEnvValue(componentMetas) { _, componentValue ->
                                    componentValue.bindings[lookupName] ?: valueFactory.missingValue
                                }
                            }
                            else -> {
                                val indexThunk = compileExprNode(indexExpr)
                                thunkFactory.thunkEnvValue(componentMetas) { env, componentValue ->
                                    val indexValue = indexThunk(env)
                                    when {
                                        indexValue.type == ExprValueType.INT -> {
                                            componentValue.ordinalBindings[indexValue.numberValue().toInt()]
                                        }
                                        indexValue.type.isText -> {
                                            val lookupName = BindingName(indexValue.stringValue(), caseSensitivity.toBindingCase())
                                            componentValue.bindings[lookupName]
                                        }
                                        else -> {
                                            when (compileOptions.typingMode) {
                                                TypingMode.LEGACY -> err("Cannot convert index to int/string: $indexValue",
                                                    ErrorCode.EVALUATOR_INVALID_CONVERSION,
                                                    errorContextFrom(componentMetas),
                                                    internal = false)
                                                TypingMode.PERMISSIVE -> valueFactory.missingValue
                                            }

                                        }
                                    } ?: valueFactory.missingValue
                                }
                            }
                        }
                    }
                    is PathComponentUnpivot -> {
                        when {
                            !remainingComponents.isEmpty() -> {
                                val tempThunk = compilePathComponents(pathMetas, remainingComponents)
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
                    is PathComponentWildcard -> {
                        when {
                            !remainingComponents.isEmpty() -> {
                                val hasMoreWildCards = remainingComponents.filterIsInstance<PathComponentWildcard>().any()
                                val tempThunk = compilePathComponents(pathMetas, remainingComponents)

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
                })
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
     * Given an AST node that represents a `LIKE` predicate, return an [ExprThunk] that evaluates a `LIKE` predicate.
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
    private fun compileNAryLike(argExprs: List<ExprNode>, argThunks: List<ThunkEnv>, operatorMetas: MetaContainer): ThunkEnv {
        val valueExpr = argExprs[0]
        val patternExpr = argExprs[1]
        val escapeExpr = when {
            argExprs.size > 2 -> argExprs[2]
            else -> null
        }

        val patternLocationMeta = patternExpr.metas.sourceLocationMeta
        val escapeLocationMeta = escapeExpr?.metas?.sourceLocationMeta


        // Note that the return value is a nullable and deferred.
        // This is so that null short-circuits can be supported.
        fun getPatternParts(pattern: ExprValue, escape: ExprValue?): (() -> List<PatternPart>)? {
            val patternArgs = listOfNotNull(pattern, escape)
            when {
                patternArgs.any { it.type.isUnknown } -> return null
                patternArgs.any { !it.type.isText } -> return {
                    err("LIKE expression must be given non-null strings as input",
                        ErrorCode.EVALUATOR_LIKE_INVALID_INPUTS,
                        errorContextFrom(operatorMetas).also {
                            it[Property.LIKE_PATTERN] = pattern.ionValue.toString()
                            if (escape != null) it[Property.LIKE_ESCAPE] = escape.ionValue.toString()
                        },
                        internal = false)
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
                    errorContextFrom(operatorMetas).also {
                        it[Property.LIKE_VALUE] = value.ionValue.toString()
                    },
                    internal = false)
                else -> valueFactory.newBoolean(executePattern(patternParts(), value.stringValue()))
            }
        }

        val valueThunk = argThunks[0]

        // If the pattern and escape expressions are literals then we can can compile the pattern now and
        // re-use it with every execution.  Otherwise we must re-compile the pattern every time.

        return when {
            patternExpr is Literal && (escapeExpr == null || escapeExpr is Literal) -> {
                val patternParts = getPatternParts(
                    valueFactory.newFromIonValue(patternExpr.ionValue),
                    (escapeExpr as? Literal)?.ionValue?.let { valueFactory.newFromIonValue(it) })

                // If valueExpr is also a literal then we can evaluate this at compile time and return a constant.
                if (valueExpr is Literal) {
                    val resultValue = runPatternParts(valueFactory.newFromIonValue(valueExpr.ionValue), patternParts)
                    return thunkFactory.thunkEnv(operatorMetas) { resultValue }
                } else {
                    thunkFactory.thunkEnvOperands(operatorMetas, valueThunk) { _, value ->
                        runPatternParts(value, patternParts)
                    }
                }
            }
            else -> {
                val patternThunk = argThunks[1]
                when (argThunks.size) {
                    2 -> {
                        //thunk that re-compiles the DFA every evaluation without a custom escape sequence
                        thunkFactory.thunkEnvOperands(operatorMetas, valueThunk, patternThunk) { _, value, pattern ->
                            val pps = getPatternParts(pattern, null)
                            runPatternParts(value, pps)
                        }
                    }
                    else -> {
                        //thunk that re-compiles the pattern every evaluation but *with* a custom escape sequence
                        val escapeThunk = argThunks[2]
                        thunkFactory.thunkEnvOperands(operatorMetas, valueThunk, patternThunk, escapeThunk) { _, value, pattern, escape ->
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
            ?: err("Must provide a non-null value for PATTERN in a LIKE predicate: $pattern",
                ErrorCode.EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE,
                errorContextFrom(patternLocationMeta),
                internal = false)

        escape?.let {
            val escapeCharString = checkEscapeChar(escape, escapeLocationMeta)
            val escapeCharCodePoint = escapeCharString.codePointAt(0)  // escape is a string of length 1
            val validEscapedChars = setOf('_'.toInt(), '%'.toInt(), escapeCharCodePoint)
            val iter = patternString.codePointSequence().iterator()

            while (iter.hasNext()) {
                val current = iter.next()
                if (current == escapeCharCodePoint && (!iter.hasNext() || !validEscapedChars.contains(iter.next()))) {
                    err("Invalid escape sequence : $patternString",
                        ErrorCode.EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE,
                        errorContextFrom(patternLocationMeta).apply {
                            set(Property.LIKE_PATTERN, patternString)
                            set(Property.LIKE_ESCAPE, escapeCharString)
                        },
                        internal = false)
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
     * A values is a valid escape when
     * 1. it is 1 character long, and,
     * 1. Cannot be null (SQL92 spec marks this cases as *unknown*)
     *
     * @param escape value provided as an escape character for a `LIKE` predicate
     *
     * @return the escape character as a [String] or throws an exception when the input is invalid
     */
    private fun checkEscapeChar(escape: IonValue, locationMeta: SourceLocationMeta?): String {
        val escapeChar = escape.stringValue()?.let { it }
            ?: err(
                "Must provide a value when using ESCAPE in a LIKE predicate: $escape",
                ErrorCode.EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE,
                errorContextFrom(locationMeta),
                internal = false)
        when (escapeChar) {
            "" -> {
                err(
                    "Cannot use empty character as ESCAPE character in a LIKE predicate: $escape",
                    ErrorCode.EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE,
                    errorContextFrom(locationMeta),
                    internal = false)
            }
            else -> {
                if (escapeChar.trim().length != 1) {
                    err(
                        "Escape character must have size 1 : $escapeChar",
                        ErrorCode.EVALUATOR_LIKE_PATTERN_INVALID_ESCAPE_SEQUENCE,
                        errorContextFrom(locationMeta),
                        internal = false)
                }
            }
        }
        return escapeChar
    }

    private fun compileDdl(node: ExprNode): ThunkEnv {
        return { _ ->
            err(
                "DDL operations are not supported yet",
                ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET,
                errorContextFrom(node.metas).also {
                    it[Property.FEATURE_NAME] = "DDL Operations"
                }, internal = false
            )
        }
    }

    private fun compileExec(node: Exec): ThunkEnv {
        val (procedureName, args, metas: MetaContainer) = node
        val procedure = procedures[procedureName.name] ?: err(
            "No such stored procedure: ${procedureName.name}",
            ErrorCode.EVALUATOR_NO_SUCH_PROCEDURE,
            errorContextFrom(metas).also {
                it[Property.PROCEDURE_NAME] = procedureName.name
            },
            internal = false)

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

            throw EvaluationException(message,
                ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_PROCEDURE_CALL,
                errorContext,
                internal = false)
        }

        // Compile the procedure's arguments
        val argThunks = args.map { compileExprNode(it) }

        return thunkFactory.thunkEnv(metas) { env ->
            val procedureArgValues = argThunks.map { it(env) }
            procedure.call(env.session, procedureArgValues)
        }
    }

    private fun compileDateLiteral(node: DateLiteral): ThunkEnv {
        val (year, month, day, metas) = node
        val value = valueFactory.newDate(year, month, day)
        return thunkFactory.thunkEnv(metas) { value }
    }

    private fun compileTimeLiteral(node: TimeLiteral): ThunkEnv {
        val (hour, minute, second, nano, precision, with_time_zone, tz_minutes, metas) = node
        return thunkFactory.thunkEnv(metas) {
            // Add the default time zone if the type "TIME WITH TIME ZONE" does not have an explicitly specified time zone.
            valueFactory.newTime(
                Time.of(
                    hour,
                    minute,
                    second,
                    nano,
                    precision,
                    if (with_time_zone && tz_minutes == null) compileOptions.defaultTimezoneOffset.totalMinutes else tz_minutes
                )
            )
        }
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

/**
 * Contains data about a compiled from source, including its [Alias], [thunk],
 * type of [JoinExpansion] ([JoinExpansion.INNER] for single tables or `CROSS JOIN`S.) and [filter] criteria.
 */
private data class CompiledFromSource(
    val alias: Alias,
    val thunk: ThunkEnv,
    val joinExpansion: JoinExpansion,
    val filter: ThunkEnv?)

/**
 * Represents a single `FROM` source production of values.
 *
 * @param values A single production of values from the `FROM` source.
 * @param env The environment scoped to the values of this production.
 */
private data class FromProduction(
    val values: List<ExprValue>,
    val env: Environment)

/** Specifies the expansion for joins. */
private enum class JoinExpansion {
    /** Default for non-joined values, CROSS and INNER JOIN. */
    INNER,

    /** Expansion mode for LEFT/RIGHT/FULL JOIN. */
    OUTER
}

private data class CompiledLetSource(
    val name: String,
    val thunk: ThunkEnv)

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


private val MetaContainer.sourceLocationMeta get() = (this.find(SourceLocationMeta.TAG) as? SourceLocationMeta)


private fun StaticType.getTypes() = when (val flattened = this.flatten()) {
    is AnyOfType -> flattened.types
    else -> listOf(this)
}