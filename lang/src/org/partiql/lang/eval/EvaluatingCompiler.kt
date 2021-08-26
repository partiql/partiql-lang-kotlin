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

@file:Suppress("DuplicatedCode")

package org.partiql.lang.eval

import com.amazon.ion.*
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.toIonValue
import org.partiql.lang.ast.*
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.errors.*
import org.partiql.lang.eval.binding.*
import org.partiql.lang.eval.builtins.storedprocedure.StoredProcedure
import org.partiql.lang.eval.like.PatternPart
import org.partiql.lang.eval.like.executePattern
import org.partiql.lang.eval.like.parsePattern
import org.partiql.lang.eval.time.Time
import org.partiql.lang.eval.visitors.PartiqlAstSanityValidator
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.util.*
import org.partiql.pig.runtime.SymbolPrimitive
import java.math.*
import java.util.*
import kotlin.collections.*

/*
 TODO:
    - bring back nary optimization
    - rename all the compileNary* functions
    - reduce/eliminate need for .toPartiQlMetaContainer()
    - reduce/eliminate syntax bitterness of literal.value.toIonValue(valueFactory.ion))
    - Migrate ExprValue.cast to use PIG-generated AST.
 */


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
 * @param ion The ion system to use for synthesizing Ion values.
 * @param functions A map of functions keyed by function name that will be available during compilation.
 * @param compileOptions Various options that effect how the source code is compiled.
 */
internal class EvaluatingCompiler(
    private val valueFactory: ExprValueFactory,
    private val functions: Map<String, ExprFunction>,
    private val procedures: Map<String, StoredProcedure>,
    private val compileOptions: CompileOptions = CompileOptions.standard()
) {
    private val thunkFactory = ThunkFactory(compileOptions.thunkOptions)
    private val compilationContextStack = Stack<CompilationContext>()

    private val currentCompilationContext: CompilationContext
        get() = compilationContextStack.peek() ?: throw EvaluationException(
            "compilationContextStack was empty.", internal = true)

    //Note: please don't make this inline -- it messes up [EvaluationException] stack traces and
    //isn't a huge benefit because this is only used at SQL-compile time anyway.
    private fun <R> nestCompilationContext(expressionContext: ExpressionContext,
                                           fromSourceNames: Set<String>, block: () -> R): R {
        compilationContextStack.push(
            when {
                compilationContextStack.empty() -> CompilationContext(expressionContext, fromSourceNames)
                else                            -> compilationContextStack.peek().createNested(expressionContext,
                                                                                               fromSourceNames)
            })

        try {
            return block()
        }
        finally {
            compilationContextStack.pop()
        }
    }

    private fun Number.exprValue(): ExprValue = when (this) {
        is Int        -> valueFactory.newInt(this)
        is Long       -> valueFactory.newInt(this)
        is Double     -> valueFactory.newFloat(this)
        is BigDecimal -> valueFactory.newDecimal(this)
        else          -> errNoContext("Cannot convert number to expression value: $this", internal = true)
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
        val valueFilter: (ExprValue) -> Boolean = { _ -> true}
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
                else                   -> curr
            }
        }
    }

    /** Dispatch table for built-in aggregate functions. */
    private val builtinAggregates: Map<Pair<String, PartiqlAst.SetQuantifier>, ExprAggregatorFactory> =
        // each distinct ExprAggregator must get its own createUniqueExprValueFilter()
        run {
            val countAccFunc: (Number?, ExprValue) -> Number = { curr, _ -> curr!! + 1L }
            val sumAccFunc: (Number?, ExprValue) -> Number =
                { curr, next -> curr?.let { it + next.numberValue() } ?: next.numberValue() }
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

                    override fun compute() =
                        sum?.let { (it / bigDecimalOf(count)).exprValue() }
                            ?: this@EvaluatingCompiler.valueFactory.nullValue
                }
            }
            val allFilter: (ExprValue) -> Boolean = { _ -> true }
            // each distinct ExprAggregator must get its own createUniqueExprValueFilter()
            // each distinct ExprAggregator must get its own createUniqueExprValueFilter()
            mapOf(
                Pair("count", PartiqlAst.SetQuantifier.All()) to ExprAggregatorFactory.over {
                    Accumulator(0L, countAccFunc, allFilter)
                },

                Pair("count", PartiqlAst.SetQuantifier.Distinct()) to ExprAggregatorFactory.over {
                    Accumulator(0L, countAccFunc, createUniqueExprValueFilter())
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
     * Compiles an [ExprNode] tree to an [Expression].
     *
     * Checks [Thread.interrupted] before every expression and sub-expression is compiled
     * and throws [InterruptedException] if [Thread.interrupted] it has been set in the
     * hope that long running compilations may be aborted by the caller.
     */
    fun compile(originalAst: PartiqlAst.Statement): Expression {

        val visitorTransformer = compileOptions.visitorTransformMode.createVisitorTransform()
        val transformedExpr = visitorTransformer.transformStatement(originalAst)

        PartiqlAstSanityValidator.validate(transformedExpr)

        val thunk = when(transformedExpr) {
            is PartiqlAst.Statement.Query -> nestCompilationContext(ExpressionContext.NORMAL, emptySet()) {
                compileExpr(transformedExpr.expr)
            }
            is PartiqlAst.Statement.Exec -> compileExec(transformedExpr)
            else -> error("TODO: can't compile: $originalAst")
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
        val ast = parser.parseExprNode(source).toAstStatement()
        return compile(ast)
    }

    /**
     * Evaluates a V0 s-exp based AST against a global set of bindings.
     */
    @Deprecated("Please use CompilerPipeline.compile(ExprNode).eval(EvaluationSession) instead.")
    fun eval(ast: IonSexp, session: EvaluationSession): ExprValue {
        val exprNode = AstDeserializerBuilder(valueFactory.ion).build().deserialize(ast, AstVersion.V0)
        return compile(exprNode.toAstStatement()).eval(session)
    }


    /**
     * Evaluates an instance of [ExprNode] against a global set of bindings.
     */
    fun eval(ast: ExprNode, session: EvaluationSession): ExprValue = compile(ast.toAstStatement()).eval(session)

    /**
     * Compiles the specified [ExprNode] into a [ThunkEnv].
     *
     * This function will [InterruptedException] if [Thread.interrupted] has been set.
     */
    private fun compileExpr(expr: PartiqlAst.Expr): ThunkEnv {
        checkThreadInterrupted()
        fun compileArgs(args: List<PartiqlAst.Expr>) = args.map { compileExpr(it) }
        val metas = expr.metas.toPartiQlMetaContainer()

        return when (expr) {
            is PartiqlAst.Expr.Lit -> compileLiteral(expr)
            is PartiqlAst.Expr.Missing -> compileLiteralMissing(expr)
            is PartiqlAst.Expr.Id -> compileVariableReference(expr)
            is PartiqlAst.Expr.SimpleCase -> compileSimpleCase(expr)
            is PartiqlAst.Expr.SearchedCase -> compileSearchedCase(expr)
            is PartiqlAst.Expr.Path -> compilePath(expr)
            is PartiqlAst.Expr.Struct -> compileStruct(expr)
            is PartiqlAst.Expr.Select -> compileSelect(expr)
            is PartiqlAst.Expr.CallAgg -> compileCallAgg(expr)
            is PartiqlAst.Expr.Parameter -> compileParameter(expr)
            is PartiqlAst.Expr.Date -> compileDate(expr)
            is PartiqlAst.Expr.LitTime -> compileTime(expr)

            // arithmetic operations
            is PartiqlAst.Expr.Plus -> compileNAryAdd(compileArgs(expr.operands), metas)
            is PartiqlAst.Expr.Times -> compileNaryMul(compileArgs(expr.operands), metas)
            is PartiqlAst.Expr.Minus -> compileNArySub(compileArgs(expr.operands), metas)
            is PartiqlAst.Expr.Divide -> compileNAryDiv(compileArgs(expr.operands), metas)
            is PartiqlAst.Expr.Modulo -> compileNAryMod(compileArgs(expr.operands), metas)

            // comparison operators
            is PartiqlAst.Expr.And -> compileNAryAnd(compileArgs(expr.operands), metas)
            is PartiqlAst.Expr.Between -> compileNAryBetween(expr)
            is PartiqlAst.Expr.Eq -> compileNAryEq(compileArgs(expr.operands), metas)
            is PartiqlAst.Expr.Gt -> compileNAryGt(compileArgs(expr.operands), metas)
            is PartiqlAst.Expr.Gte -> compileNAryGte(compileArgs(expr.operands), metas)
            is PartiqlAst.Expr.Lt -> compileNaryLt(compileArgs(expr.operands), metas)
            is PartiqlAst.Expr.Lte -> compileNAryLte(compileArgs(expr.operands), metas)
            is PartiqlAst.Expr.Like -> compileNAryLike(expr)
            is PartiqlAst.Expr.InCollection -> compileNAryIn(expr.operands, metas)

            // logical operators
            is PartiqlAst.Expr.Ne -> compileNAryNe(compileArgs(expr.operands), metas)
            is PartiqlAst.Expr.Neg -> compileNAryNeg(expr)
            is PartiqlAst.Expr.Or -> compileNAryOr(compileArgs(expr.operands), metas)

            // unary
            is PartiqlAst.Expr.Not -> compileNAryNot(compileArgs(listOf(expr.expr)), metas)
            is PartiqlAst.Expr.Pos -> compileNAryPos(expr)

            // other operators
            is PartiqlAst.Expr.Concat -> compileNAryStringConcat(compileArgs(expr.operands), metas)
            is PartiqlAst.Expr.Call -> compileNAryCall(expr)

            // "typed" operators (RHS is a data type and not an expression)
            is PartiqlAst.Expr.Cast -> compileExprCast(expr)
            is PartiqlAst.Expr.IsType -> compileExprIsType(expr)

            // sequence constructors
            is PartiqlAst.Expr.List -> compileSeq(ExprValueType.LIST, expr.values, expr.metas)
            is PartiqlAst.Expr.Sexp -> compileSeq(ExprValueType.SEXP, expr.values, expr.metas)
            is PartiqlAst.Expr.Bag -> compileSeq(ExprValueType.BAG, expr.values, expr.metas)

            // set operators
            is PartiqlAst.Expr.Intersect,
            is PartiqlAst.Expr.Union,
            is PartiqlAst.Expr.Except -> {
                err("${expr.javaClass.canonicalName} is not yet supported",
                    ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET,
                    errorContextFrom(metas).also {
                        it[Property.FEATURE_NAME] = expr.javaClass.canonicalName
                    }, internal = false)
            }
        }
    }

    private fun compileNAryAdd(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer): ThunkEnv =
        thunkFactory.thunkFold(valueFactory.nullValue, metas, argThunks) { lValue, rValue ->
            (lValue.numberValue() + rValue.numberValue()).exprValue()
        }

    private fun compileNAryPos(
        expr: PartiqlAst.Expr.Pos
    ): ThunkEnv {
        val firstThunk = compileExpr(expr.expr)
        return thunkFactory.thunkEnv(expr.metas.toPartiQlMetaContainer()) { env ->
            val value = firstThunk(env)
            when {
                value.type.isUnknown -> valueFactory.nullValue
                else -> {
                    //Invoking .numberValue() here makes this essentially just a type check
                    value.numberValue()
                    //Original value is returned unmodified.
                    value
                }
            }
        }
    }

    private fun compileNArySub(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer): ThunkEnv {
        return thunkFactory.thunkFold(valueFactory.nullValue, metas, argThunks) { lValue, rValue ->
            (lValue.numberValue() - rValue.numberValue()).exprValue()
        }
    }

    private fun compileNAryNeg(
        expr: PartiqlAst.Expr.Neg
    ): ThunkEnv {
        val operandThunk = compileExpr(expr.expr)
        return thunkFactory.thunkEnv(expr.metas.toPartiQlMetaContainer()) { env ->
            val value = operandThunk(env)
            when {
                value.type.isUnknown -> valueFactory.nullValue
                else -> (-value.numberValue()).exprValue()
            }
        }
    }

    private fun compileNaryMul(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer
    ): ThunkEnv =
        thunkFactory.thunkFold(valueFactory.nullValue, metas, argThunks) { lValue, rValue ->
            (lValue.numberValue() * rValue.numberValue()).exprValue()
        }

    private fun compileNAryDiv(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer
    ): ThunkEnv {
        return thunkFactory.thunkFold(valueFactory.nullValue, metas, argThunks) { lValue, rValue ->
            val denominator = rValue.numberValue()
            if (denominator.isZero()) {
                err("/ by zero", ErrorCode.EVALUATOR_DIVIDE_BY_ZERO, null, false)
            }
            try {
                (lValue.numberValue() / denominator).exprValue()
            }
            catch (e: ArithmeticException) {
                // Setting the internal flag as true as it is not clear what
                // ArithmeticException may be thrown by the above
                throw EvaluationException(cause = e, internal = true)
            }
        }
    }

    private fun compileNAryMod(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer): ThunkEnv {
        return thunkFactory.thunkFold(
            valueFactory.nullValue,
            metas,
            argThunks) { lValue, rValue ->
            val denominator = rValue.numberValue()
            if (denominator.isZero()) {
                err("% by zero", ErrorCode.EVALUATOR_MODULO_BY_ZERO, null, false)
            }

            (lValue.numberValue() % denominator).exprValue()
        }
    }

    private fun compileNAryEq(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer): ThunkEnv {
        return thunkFactory.thunkAndMap(
            valueFactory,
            metas,
            argThunks) { lValue, rValue -> (lValue.exprEquals(rValue)) }
    }

    private fun compileNAryNe(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer): ThunkEnv {
        return thunkFactory.thunkFold(valueFactory.nullValue, metas, argThunks) { lValue, rValue ->
            ((!lValue.exprEquals(
                rValue)).exprValue())
        }
    }

    private fun compileNaryLt(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer): ThunkEnv {
        return thunkFactory.thunkAndMap(
            valueFactory,
            metas,
            argThunks) { lValue, rValue -> lValue < rValue }
    }

    private fun compileNAryLte(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer): ThunkEnv {
        return thunkFactory.thunkAndMap(
            valueFactory,
            metas,
            argThunks) { lValue, rValue -> lValue <= rValue }
    }

    private fun compileNAryGt(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer): ThunkEnv {
        return thunkFactory.thunkAndMap(
            valueFactory,
            metas,
            argThunks) { lValue, rValue -> lValue > rValue }
    }

    private fun compileNAryGte(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer): ThunkEnv {
        return thunkFactory.thunkAndMap(
            valueFactory,
            metas,
            argThunks) { lValue, rValue -> lValue >= rValue }
    }

    // TODO: compile thunks in this function instead of at call site
    private fun compileNAryBetween(
        expr: PartiqlAst.Expr.Between
    ): ThunkEnv {
        val valueThunk = compileExpr(expr.value)
        val fromThunk = compileExpr(expr.from)
        val toThunk = compileExpr(expr.to)
        val metas = expr.metas.toPartiQlMetaContainer()

        return thunkFactory.thunkEnv(metas) { env ->
            val value = valueThunk(env)
            (value >= fromThunk(env) && value <= toThunk(env)).exprValue()
        }
    }

    // TODO: pass PartiQlAst.Expr.In here instead of just the operands.
    private fun compileNAryIn(
        args: List<PartiqlAst.Expr>,
        metas: MetaContainer): ThunkEnv {
        val leftArg = compileExpr(args[0])
        val rightArg = args[1]

        return when {
            // When the right arg is a list of literals we use a Set to speed up the predicate
            rightArg is PartiqlAst.Expr.List && rightArg.values.all { it is PartiqlAst.Expr.Lit } -> {
                val inSet = rightArg.values
                    .map { it as PartiqlAst.Expr.Lit }
                    .mapTo(TreeSet<ExprValue>(DEFAULT_COMPARATOR)) { valueFactory.newFromIonValue(it.value.toIonValue(valueFactory.ion)) }

                thunkFactory.thunkEnv(metas) { env ->
                    val value = leftArg(env)
                    // we can use contains as exprEquals uses the DEFAULT_COMPARATOR
                    inSet.contains(value).exprValue()
                }
            }

            else -> {
                val rightArgThunk = compileExpr(rightArg)

                thunkFactory.thunkEnv(metas) { env ->
                    val value = leftArg(env)
                    val rigthArgExprValue = rightArgThunk(env)
                    rigthArgExprValue.any { it.exprEquals(value) }.exprValue()
                }
            }
        }
    }

    // TODO: compile nodes here instead of at call site
    private fun compileNAryNot(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer): ThunkEnv {
        val arg = argThunks[0]
        return thunkFactory.thunkEnv(metas) { env ->
            val value = arg(env)
            when {
                value.type.isUnknown -> valueFactory.nullValue
                else                 -> (!value.booleanValue()).exprValue()
            }
        }
    }

    private fun compileNAryAnd(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer): ThunkEnv {
        return thunkFactory.thunkEnv(metas) thunk@{ env ->
            var hasUnknowns = false
            argThunks.forEach { currThunk ->
                val currValue = currThunk(env)
                // How null-propagation works for AND is rather weird according to the SQL-92 spec.
                // Nulls are propagated like other expressions only when none of the terms are FALSE.
                // If any one of them is FALSE, then the entire expression evaluates to FALSE, i.e.:
                //      NULL AND FALSE -> FALSE
                //      NULL AND TRUE -> NULL
                // (strange but true)
                when {
                    currValue.isUnknown()     -> hasUnknowns = true
                //Short circuit only if we encounter a known false value.
                    !currValue.booleanValue() -> return@thunk valueFactory.newBoolean(false)
                }
            }

            when (hasUnknowns) {
                true  -> valueFactory.nullValue
                false -> valueFactory.newBoolean(true)
            }
        }
    }

    private fun compileNAryOr(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer): ThunkEnv {
        return thunkFactory.thunkEnv(metas) thunk@{ env ->
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
                    currValue.isUnknown()    -> hasUnknowns = true
                    currValue.booleanValue() -> return@thunk valueFactory.newBoolean(true)
                }
            }

            when (hasUnknowns) {
                true  -> valueFactory.nullValue
                false -> valueFactory.newBoolean(false)
            }
        }
    }

    private fun compileNAryStringConcat(
        argThunks: List<ThunkEnv>,
        metas: MetaContainer): ThunkEnv {

        return thunkFactory.thunkFold(valueFactory.nullValue, metas, argThunks) { lValue, rValue ->
            val lType = lValue.type
            val rType = rValue.type

            if(lType.isText && rType.isText) {
                // null/missing propagation is handled before getting here
                (lValue.stringValue() + rValue.stringValue()).exprValue()
            }
            else {
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

    private fun compileNAryCall(expr: PartiqlAst.Expr.Call): ThunkEnv {
        val metas = expr.metas.toPartiQlMetaContainer()

        val funcArgThunks = expr.args.map { compileExpr(it) }

        val func = functions[expr.funcName.text] ?: err(
            "No such function: ${expr.funcName.text}",
            ErrorCode.EVALUATOR_NO_SUCH_FUNCTION,
            errorContextFrom(metas).also {
                it[Property.FUNCTION_NAME] = expr.funcName.text
            },
            internal = false)

        return thunkFactory.thunkEnv(metas) { env ->
            val funcArgValues = funcArgThunks.map { it(env) }
            func.call(env, funcArgValues)
        }
    }

    private fun compileLiteral(expr: PartiqlAst.Expr.Lit): ThunkEnv {
        val value = valueFactory.newFromIonValue(expr.value.toIonValue(valueFactory.ion))
        return thunkFactory.thunkEnv(expr.metas.toPartiQlMetaContainer()) { value }
    }

    private fun compileLiteralMissing(expr: PartiqlAst.Expr.Missing): ThunkEnv {
        return thunkFactory.thunkEnv(expr.metas.toPartiQlMetaContainer()) { _ -> valueFactory.missingValue }
    }

    private fun compileVariableReference(expr: PartiqlAst.Expr.Id): ThunkEnv {
        //val (id, case, lookupStrategy, metas: MetaContainer) = expr
        val metas = expr.metas.toPartiQlMetaContainer()
        val uniqueNameMeta = expr.metas[UniqueNameMeta.TAG] as? UniqueNameMeta

        val fromSourceNames = currentCompilationContext.fromSourceNames

        return when(uniqueNameMeta) {
            null -> {
                val bindingName = BindingName(expr.name.text, expr.case.toBindingCase())
                val evalVariableReference = when (compileOptions.undefinedVariable) {
                    UndefinedVariableBehavior.ERROR   ->
                        thunkFactory.thunkEnv(metas) { env ->
                            when(val value = env.current[bindingName]) {
                                null -> {
                                    if (fromSourceNames.any { bindingName.isEquivalentTo(it) }) {
                                        throw EvaluationException(
                                                "Variable not in GROUP BY or aggregation function: ${bindingName.name}",
                                                ErrorCode.EVALUATOR_VARIABLE_NOT_INCLUDED_IN_GROUP_BY,
                                                errorContextFrom(metas).also { it[Property.BINDING_NAME] = bindingName.name },
                                                internal = false)
                                    } else {
                                        throw EvaluationException(
                                                "No such binding: ${bindingName.name}",
                                                ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
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
                        errorContextFrom(metas),
                        internal = true)
                }
            }
        }
    }

    private fun compileParameter(expr: PartiqlAst.Expr.Parameter): ThunkEnv {
        val metas = expr.metas.toPartiQlMetaContainer()
        val index = expr.index.value.toInt() - 1

        return { env ->
            val params = env.session.parameters
            if (params.size <= index) {
                throw EvaluationException(
                        "Unbound parameter for index: $index",
                        ErrorCode.EVALUATOR_UNBOUND_PARAMETER,
                        errorContextFrom(metas).also {
                            it[Property.EXPECTED_PARAMETER_ORDINAL] = index
                            it[Property.BOUND_PARAMETER_COUNT] = params.size
                        },
                        internal = false
                )
            }

            params[index]
        }
    }

    private fun compileExprCast(expr: PartiqlAst.Expr.Cast): ThunkEnv {
        val metas = expr.metas.toPartiQlMetaContainer()
        val locationMeta = metas.sourceLocationMeta
        val expThunk = compileExpr(expr)
        // TODO: migrate cast to use PIG-AST types
        val dataType = expr.asType.toExprNodeType()
        return thunkFactory.thunkEnv(metas) { env ->
            val valueToCast = expThunk(env)
            valueToCast.cast(dataType, valueFactory, locationMeta)
        }
    }

    private fun compileExprIsType(expr: PartiqlAst.Expr.IsType): ThunkEnv {
        val metas = expr.metas.toPartiQlMetaContainer()
        val expThunk = compileExpr(expr.value)
        return when (expr.type) {
            is PartiqlAst.Type.NullType -> thunkFactory.thunkEnv(metas) { env ->
                val expValue = expThunk(env)
                (expValue.type == ExprValueType.MISSING || expValue.type == ExprValueType.NULL).exprValue()
            }
            else -> {
                val exprValueType = ExprValueType.fromPartiQlAstType(expr.type)
                thunkFactory.thunkEnv(metas) { env ->
                    val expValue = expThunk(env)
                    (expValue.type == exprValueType).exprValue()
                }
            }
        }
    }

    private fun compileSimpleCase(expr: PartiqlAst.Expr.SimpleCase): ThunkEnv {
        val metas = expr.metas.toPartiQlMetaContainer()
        val valueThunk = compileExpr(expr.expr)

        val elseThunk = when {
            expr.default != null -> compileExpr(expr.default)
            else             -> thunkFactory.thunkEnv(metas) { _ -> valueFactory.nullValue }
        }

        val branchThunks = expr.cases.pairs.map { Pair(compileExpr(it.first), compileExpr(it.second)) }
        return thunkFactory.thunkEnv(metas) thunk@{ env ->
            val caseValue = valueThunk(env)
            branchThunks.forEach { bt ->
                val branchValue = bt.first(env)
                if (caseValue.exprEquals(branchValue)) {
                    return@thunk bt.second(env)
                }
            }
            elseThunk(env)
        }
    }

    private fun compileSearchedCase(expr: PartiqlAst.Expr.SearchedCase): ThunkEnv {
        val metas = expr.metas.toPartiQlMetaContainer()

        val elseThunk = when {
            expr.default != null -> compileExpr(expr.default)
            else             -> thunkFactory.thunkEnv(metas) { _ -> valueFactory.nullValue }
        }

        val branchThunks = expr.cases.pairs.map { Pair(compileExpr(it.first), compileExpr(it.second)) }

        return thunkFactory.thunkEnv(metas) thunk@{ env ->
            branchThunks.forEach { bt ->
                val conditionValue = bt.first(env)
                if (conditionValue.booleanValue()) {
                    return@thunk bt.second(env)
                }
            }
            elseThunk(env)
        }
    }

    private fun compileStruct(expr: PartiqlAst.Expr.Struct): ThunkEnv {
        val metas = expr.metas.toPartiQlMetaContainer()
        class StructFieldThunks(val nameThunk: ThunkEnv, val valueThunk: ThunkEnv)

        val fieldThunks = expr.fields.map {
            StructFieldThunks(compileExpr(it.first), compileExpr(it.second))
        }

        return thunkFactory.thunkEnv(metas) { env ->
            val seq = fieldThunks.map { it.valueThunk(env).namedValue(it.nameThunk(env)) }.asSequence()
            createStructExprValue(seq, StructOrdering.ORDERED)
        }
    }

    private fun compileSeq(seqType: ExprValueType, itemExprs: List<PartiqlAst.Expr>, metas: IonElementMetaContainer): ThunkEnv {
        require(seqType.isSequence) { "seqType mist be a sequence!" }

        val itemThunks = itemExprs.map { compileExpr(it) }

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

        return thunkFactory.thunkEnv(metas.toPartiQlMetaContainer()) { env ->
            // todo:  use valueFactory.newSequence() instead.
            SequenceExprValue(
                valueFactory.ion,
                seqType,
                makeItemThunkSequence(env))
        }
    }

    private fun evalLimit(limitThunk: ThunkEnv, env: Environment, limitLocationMeta: SourceLocationMeta?): Long {
        val limitExprValue = limitThunk(env)

        if(limitExprValue.type != ExprValueType.INT) {
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
        if(limitIonValue.integerSize == IntegerSize.BIG_INTEGER) {
            err("IntegerSize.BIG_INTEGER not supported for LIMIT values",
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


    private fun compileSelect(selectExpr: PartiqlAst.Expr.Select): ThunkEnv {
        selectExpr.order?.let {
            err("ORDER BY is not supported in evaluator yet",
                ErrorCode.EVALUATOR_FEATURE_NOT_SUPPORTED_YET,
                errorContextFrom(selectExpr.metas.toPartiQlMetaContainer()).also { it[Property.FEATURE_NAME] = "ORDER BY" },
                internal = false )
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

        val allFromSourceAliases = fold.walkFromSource(selectExpr.from, emptySet())
            .union(selectExpr.fromLet?.let { fold.walkLet(selectExpr.fromLet, emptySet()) } ?: emptySet())

        return nestCompilationContext(ExpressionContext.NORMAL, emptySet()) {
            //val (setQuantifier, projection, from, fromLet, _, groupBy, having, _, limit, metas: MetaContainer) = selectExpr
            val setQuantifier = selectExpr.setq ?: PartiqlAst.build { all() }
            val projection = selectExpr.project
            val from = selectExpr.from
            val fromLet = selectExpr.fromLet
            val groupBy = selectExpr.group ?: PartiqlAst.build { groupBy(groupFull(), groupKeyList(emptyList())) }
            val having = selectExpr.having
            val limit = selectExpr.limit
            val metas = selectExpr.metas.toPartiQlMetaContainer()

            val fromSourceThunks = compileFromSources(from)
            val letSourceThunks = fromLet?.let { compileLetSources(it) }
            val sourceThunks = compileQueryWithoutProjection(selectExpr, fromSourceThunks, letSourceThunks)

            val limitThunk = limit?.let { compileExpr(limit) }
            val limitLocationMeta = limit?.metas?.toPartiQlMetaContainer()?.sourceLocationMeta

            // Returns a thunk that invokes [sourceThunks], and invokes [projectionThunk] to perform the projection.
            fun getQueryThunk(selectProjectionThunk: ThunkEnvValue<List<ExprValue>>): ThunkEnv {
                //val (_, groupByItems, groupAsName) =
                val groupByItems = groupBy.keyList.keys
                val groupAsName = groupBy.groupAsAlias

                val aggregateListMeta = selectExpr.metas[AggregateCallSiteListMeta.TAG] as AggregateCallSiteListMeta?
                val hasAggregateCallSites = aggregateListMeta?.aggregateCallSites?.any() ?: false

                val queryThunk = when {
                    groupByItems.isEmpty() && !hasAggregateCallSites ->
                        // Grouping is not needed -- simply project the results from the FROM clause directly.
                        thunkFactory.thunkEnv(metas) { env ->

                            val projectedRows = sourceThunks(env).map { (joinedValues, projectEnv) ->
                                selectProjectionThunk(projectEnv, joinedValues)
                            }

                            val quantifiedRows = when(setQuantifier) {
                                // wrap the ExprValue to use ExprValue.equals as the equality
                                is PartiqlAst.SetQuantifier.Distinct -> projectedRows.filter(createUniqueExprValueFilter())
                                is PartiqlAst.SetQuantifier.All -> projectedRows
                            }.let { rows ->
                                when (limitThunk) {
                                    null -> rows
                                    else -> rows.take(evalLimit(limitThunk, env, limitLocationMeta))
                                }
                            }

                            valueFactory.newBag(quantifiedRows.map {
                                // TODO make this expose the ordinal for ordered sequences
                                // make sure we don't expose the underlying value's name out of a SELECT
                                it.unnamedValue()
                            })
                        }
                    else                                             -> {
                        // Grouping is needed

                        class CompiledAggregate(val factory: ExprAggregatorFactory, val argThunk: ThunkEnv)

                        // These aggregate call sites are collected in [AggregateSupportVisitorTransform].
                        val compiledAggregates = aggregateListMeta?.aggregateCallSites?.map { it ->
                            val funcName = it.funcName.text
                            CompiledAggregate(
                                factory = getAggregatorFactory(funcName, it.setq, it.metas.toPartiQlMetaContainer()),
                                argThunk = compileExpr(it.arg))
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
                                    val fromProductions: Sequence<FromProduction> = sourceThunks(env).let { rows ->
                                        when (limitThunk) {
                                            null -> rows
                                            else -> rows.take(evalLimit(limitThunk, env, limitLocationMeta))
                                        }
                                    }
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

                                val havingThunk = having?.let { compileExpr(it) }

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
                                                    internal = true)).namedValue(pair.nameExprValue)
                                            }.asSequence()

                                            group.groupValues.add(createStructExprValue(seq, StructOrdering.UNORDERED))
                                        }
                                    }

                                    // generate the final group by projection
                                    val projectedRows = env.groups.mapNotNull { g ->
                                        val groupByEnv = getGroupEnv(env, g.value)
                                        filterHavingAndProject(groupByEnv, g.value)
                                    }.asSequence().let { rows ->
                                        when (limitThunk) {
                                            null -> rows
                                            else -> rows.take(evalLimit(limitThunk, env, limitLocationMeta))
                                        }
                                    }

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
                is PartiqlAst.Projection.ProjectValue -> {
                    val valueExpr = projection.value
                    nestCompilationContext(ExpressionContext.NORMAL, allFromSourceAliases) {
                        val valueThunk = compileExpr(valueExpr)
                        getQueryThunk(thunkFactory.thunkEnvValue(metas) { env, _ -> valueThunk(env) })
                    }
                }

                is PartiqlAst.Projection.ProjectPivot -> {
                    val atExpr = projection.key
                    val asExpr = projection.value
                    //val (asExpr, atExpr) = projection
                    nestCompilationContext(ExpressionContext.NORMAL, allFromSourceAliases) {
                        val asThunk = compileExpr(asExpr)
                        val atThunk = compileExpr(atExpr)
                        thunkFactory.thunkEnv(metas) { env ->
                            val sourceValue = sourceThunks(env).asSequence().let { rows ->
                                when (limitThunk) {
                                    null -> rows
                                    else -> rows.take(evalLimit(limitThunk, env, limitLocationMeta))
                                }
                            }

                            val seq = sourceValue
                                    .map { (_, env) -> Pair(asThunk(env), atThunk(env)) }
                                    .filter { (name, _) -> name.type.isText }
                                    .map { (name, value) -> value.namedValue(name) }

                            createStructExprValue(seq, StructOrdering.UNORDERED)
                        }
                    }
                }
                is PartiqlAst.Projection.ProjectList -> {
                    nestCompilationContext(ExpressionContext.SELECT_LIST, allFromSourceAliases) {
                        val projectionElements = compileSelectListToProjectionElements(projection)

                        val projectionThunk: ThunkEnvValue<List<ExprValue>> = thunkFactory.thunkEnvValue(metas) { env, _ ->
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
                            createStructExprValue(columns.asSequence(), StructOrdering.ORDERED)
                        }

                        getQueryThunk(projectionThunk)
                    }
                }

                is PartiqlAst.Projection.ProjectStar -> {
                    errNoContext(
                        "Encountered a SelectListItemStar--did SelectStarVisitorTransform execute?",
                        internal = true
                    )
                }
            }
        }
    }

    private fun compileGroupByExpressions(groupByItems: List<PartiqlAst.GroupKey>): List<CompiledGroupByItem> =
        groupByItems.map {
            val alias = it.asAlias ?: errNoContext("GroupByItem.asName was not specified", internal = true)
            val uniqueName = (alias.metas[UniqueNameMeta.TAG] as UniqueNameMeta?)?.uniqueName

            CompiledGroupByItem(alias.text.exprValue(), uniqueName, compileExpr(it.expr))
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
            else                -> { groupByEnv, currentGroup ->
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
                }
                else {
                    null
                }
            }
            else                -> { groupByEnv, currentGroup ->
                //Create a closure that simply performs the final projection and
                // returns the result.
                selectProjectionThunk(groupByEnv, listOf(currentGroup.key))
            }
        }

    private fun compileCallAgg(expr: PartiqlAst.Expr.CallAgg): ThunkEnv {
        val metas = expr.metas.toPartiQlMetaContainer()

        if(metas.hasMeta(IsCountStarMeta.TAG) && currentCompilationContext.expressionContext != ExpressionContext.SELECT_LIST) {
            err("COUNT(*) is not allowed in this context", errorContextFrom(metas), internal = false)
        }

        val aggFactory = getAggregatorFactory(expr.funcName.text.toLowerCase(), expr.setq, metas)

        val argThunk = nestCompilationContext(ExpressionContext.AGG_ARG, emptySet()) {
            compileExpr(expr.arg)
        }

        return when (currentCompilationContext.expressionContext) {
            ExpressionContext.AGG_ARG     -> {
                err("The arguments of an aggregate function cannot contain aggregate functions",
                    errorContextFrom(metas),
                    internal = false)
            }
            ExpressionContext.NORMAL      ->
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
                    val registers = env.currentGroup?.registers ?: err("No current group or current group has no registers",
                                                                        errorContextFrom(metas),
                                                                        internal = true)

                    registers[registerId].aggregator.compute()
                }
            }
        }
    }

     fun getAggregatorFactory(funcName: String, setQuantifier: PartiqlAst.SetQuantifier, metas: MetaContainer): ExprAggregatorFactory {
         val key = funcName.toLowerCase() to setQuantifier

        return  builtinAggregates[key] ?: err("No such function: $funcName",
                                              ErrorCode.EVALUATOR_NO_SUCH_FUNCTION,
                                              errorContextFrom(metas).also { it[Property.FUNCTION_NAME] = funcName },
                                              internal = false)
    }

    private fun compileFromSources(
        fromSource: PartiqlAst.FromSource,
        sources: MutableList<CompiledFromSource> = ArrayList(),
        joinExpansion: JoinExpansion = JoinExpansion.INNER,
        conditionThunk: ThunkEnv? = null
    ): List<CompiledFromSource> {
        val metas = fromSource.metas.toPartiQlMetaContainer()

        fun addAliases(thunk: ThunkEnv, asName: String?, atName: String?, byName: String?, metas: IonElementMetaContainer) {
            sources.add(
                CompiledFromSource(
                    alias = Alias(
                        asName = asName ?:
                            err("FromSourceExpr.variables.asName was null",
                                errorContextFrom(metas.toPartiQlMetaContainer()), internal = true),
                        atName = atName,
                        byName = byName),
                    thunk = thunk,
                    joinExpansion = joinExpansion,
                    filter = conditionThunk))
        }

        when (fromSource) {
            is PartiqlAst.FromSource.Scan -> {
                val thunk = compileExpr(fromSource.expr)
                addAliases(thunk, fromSource.asAlias?.text, fromSource.atAlias?.text, fromSource.byAlias?.text, fromSource.metas)
            }
            is PartiqlAst.FromSource.Unpivot -> {
                val exprThunk = compileExpr(fromSource.expr)
                val thunk = thunkFactory.thunkEnv(metas) { env -> exprThunk(env).unpivot() }
                addAliases(thunk, fromSource.asAlias?.text, fromSource.atAlias?.text, fromSource.byAlias?.text, fromSource.metas)
            }
            is PartiqlAst.FromSource.Join    -> {
                //val (joinOp, left, right, condition, _: MetaContainer) = fromSource
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
                val conditionThunkInner = condition?.let { compileExpr(it) }
                    ?: compileExpr(PartiqlAst.build { lit(ionBool(true)) })

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
                // TODO return here.  What does old EC do with the return value?
            }

        }.let{}

        return sources
    }

    private fun compileLetSources(letSource: PartiqlAst.Let): List<CompiledLetSource> =
        letSource.letBindings.map {
            CompiledLetSource(name = it.name.text, thunk = compileExpr(it.expr))
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
        val whereThunk = ast.where?.let { compileExpr(it) }

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
                                    if(alias.atName != null)
                                        addBinding(alias.atName) {
                                            value.name ?: valueFactory.missingValue
                                        }
                                    if(alias.byName != null)
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
                        true  -> false
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
                    val thunk = compileExpr(it.expr)
                    SingleProjectionElement(valueFactory.newString(alias), thunk)
                }
                is PartiqlAst.ProjectItem.ProjectAll -> {
                    MultipleProjectionElement(listOf(compileExpr(it.expr)))
                }
            }
        }

    private fun compilePath(expr: PartiqlAst.Expr.Path): ThunkEnv {
        val metas = expr.metas.toPartiQlMetaContainer()
        val rootThunk = compileExpr(expr.root)
        val remainingComponents = LinkedList<PartiqlAst.PathStep>()
        expr.steps.forEach { remainingComponents.addLast(it) }

        val componentThunk = compilePathComponents(metas, remainingComponents)

        return thunkFactory.thunkEnv(metas) { env ->
            val rootValue = rootThunk(env)
            componentThunk(env, rootValue)
        }
    }

    private fun compilePathComponents(
        pathMetas: MetaContainer,
        remainingComponents: LinkedList<PartiqlAst.PathStep>
    ): ThunkEnvValue<ExprValue> {

        val componentThunks = ArrayList<ThunkEnvValue<ExprValue>>()

        while (!remainingComponents.isEmpty()) {
            val c = remainingComponents.removeFirst()

            componentThunks.add(
                when (c) {
                    is PartiqlAst.PathStep.PathExpr -> {
                        val indexExpr = c.index
                        val caseSensitivity = c.case
                        val indexExprMetas = indexExpr.metas.toPartiQlMetaContainer()
                        val locationMeta = indexExprMetas.sourceLocationMeta
                        when {
                            //If indexExpr is a literal string, there is no need to evaluate it--just compile a
                            //thunk that directly returns a bound value
                            indexExpr is PartiqlAst.Expr.Lit && indexExpr.value is IonString -> {
                                val lookupName = BindingName(indexExpr.value.stringValue(), caseSensitivity.toBindingCase())
                                thunkFactory.thunkEnvValue(indexExprMetas) { _, componentValue ->
                                    componentValue.bindings[lookupName] ?: valueFactory.missingValue
                                }
                            }
                            else                                                    -> {
                                val indexThunk = compileExpr(indexExpr)
                                thunkFactory.thunkEnvValue(indexExprMetas) { env, componentValue ->
                                    val indexValue = indexThunk(env)
                                    when {
                                        indexValue.type == ExprValueType.INT -> {
                                            componentValue.ordinalBindings[indexValue.numberValue().toInt()]
                                        }
                                        indexValue.type.isText               -> {
                                            val lookupName = BindingName(indexValue.stringValue(), caseSensitivity.toBindingCase())
                                            componentValue.bindings[lookupName]
                                        }
                                        else                                 -> {
                                            err("Cannot convert index to int/string: $indexValue",
                                                errorContextFrom(locationMeta),
                                                internal = false)
                                        }
                                    } ?: valueFactory.missingValue
                                }
                            }
                        }
                    }
                    is PartiqlAst.PathStep.PathUnpivot -> {
                        val pathComponentMetas = c.metas.toPartiQlMetaContainer()
                        when {
                            !remainingComponents.isEmpty() -> {
                                val tempThunk = compilePathComponents(pathMetas, remainingComponents)
                                thunkFactory.thunkEnvValue(pathComponentMetas) { env, componentValue ->
                                    val mapped = componentValue.unpivot()
                                        .flatMap { tempThunk(env, it).rangeOver() }
                                        .asSequence()
                                    valueFactory.newBag(mapped)
                                }
                            }
                            else                           ->
                                thunkFactory.thunkEnvValue(pathComponentMetas) { _, componentValue ->
                                    valueFactory.newBag(componentValue.unpivot().asSequence())
                                }
                        }
                    }
                    // this is for `path[*].component`
                    is PartiqlAst.PathStep.PathWildcard -> {
                        val pathComponentMetas = c.metas.toPartiQlMetaContainer()
                        when {
                            !remainingComponents.isEmpty() -> {
                                val hasMoreWildCards = remainingComponents.filterIsInstance<PathComponentWildcard>().any()
                                val tempThunk = compilePathComponents(pathMetas, remainingComponents)

                                when {
                                    !hasMoreWildCards -> thunkFactory.thunkEnvValue(pathComponentMetas) { env, componentValue ->
                                        val mapped = componentValue
                                            .rangeOver()
                                            .map { tempThunk(env, it) }
                                            .asSequence()

                                        valueFactory.newBag(mapped)
                                    }
                                    else                -> thunkFactory.thunkEnvValue(pathComponentMetas) { env, componentValue ->
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
                            else                           -> {
                                thunkFactory.thunkEnvValue(pathComponentMetas) { _, componentValue ->
                                    val mapped = componentValue.rangeOver().asSequence()
                                    valueFactory.newBag(mapped)
                                }
                            }
                        }
                    }
                })
        }
        return when (componentThunks.size) {
            1    -> componentThunks.first()
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
    private fun compileNAryLike(expr: PartiqlAst.Expr.Like): ThunkEnv {
        val valueExpr = expr.value
        val patternExpr = expr.pattern
        val escapeExpr = expr.escape
        val metas = expr.metas.toPartiQlMetaContainer()

        val patternLocationMeta = patternExpr.metas.toPartiQlMetaContainer().sourceLocationMeta
        val escapeLocationMeta = escapeExpr?.metas?.toPartiQlMetaContainer()?.sourceLocationMeta

        // Note that the return value is a nullable and deferred.
        // This is so that null short-circuits can be supported.
        fun getPatternParts(pattern: ExprValue, escape: ExprValue?): (() -> List<PatternPart>)? {
            val patternArgs = listOfNotNull(pattern, escape)
            when {
                patternArgs.any { it.type.isUnknown } -> return null
                patternArgs.any { !it.type.isText }   -> return {
                    err("LIKE expression must be given non-null strings as input",
                        ErrorCode.EVALUATOR_LIKE_INVALID_INPUTS,
                        errorContextFrom(metas).also {
                            it[Property.LIKE_PATTERN] = pattern.ionValue.toString()
                            if (escape != null) it[Property.LIKE_ESCAPE] = escape.ionValue.toString()
                        },
                        internal = false)
                }
                else                              -> {
                    val (patternString: String, escapeChar: Int?) =
                        checkPattern(pattern.ionValue, patternLocationMeta, escape?.ionValue, escapeLocationMeta)

                    val patternParts = when {
                        patternString.isEmpty() -> emptyList()
                        else -> parsePattern(patternString, escapeChar)
                    }

                    return  { patternParts }
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
                    internal = false)
                else -> valueFactory.newBoolean(executePattern(patternParts(), value.stringValue()))
            }
        }

        val valueThunk = compileExpr(valueExpr)

        // If the pattern and escape expressions are literals then we can can compile the pattern now and
        // re-use it with every execution.  Otherwise we must re-compile the pattern every time.

        return when {
            patternExpr is PartiqlAst.Expr.Lit && (escapeExpr == null || escapeExpr is PartiqlAst.Expr.Lit) -> {
                val patternParts = getPatternParts(
                    valueFactory.newFromIonValue(patternExpr.value.toIonValue(valueFactory.ion)),
                    (escapeExpr as? Literal)?.ionValue?.let { valueFactory.newFromIonValue(it) })

                // If valueExpr is also a literal then we can evaluate this at compile time and return a constant.
                if (valueExpr is PartiqlAst.Expr.Lit) {
                    val resultValue = runPatternParts(valueFactory.newFromIonValue(valueExpr.value.toIonValue(valueFactory.ion)), patternParts)
                    return thunkFactory.thunkEnv(metas) { resultValue }
                }
                else {
                    thunkFactory.thunkEnv(metas) { env ->
                        val value = valueThunk(env)
                        runPatternParts(value, patternParts)
                    }
                }
            }
            else                                                                    -> {
                val patternThunk = compileExpr(patternExpr)
                when {
                    escapeExpr == null -> {
                        //thunk that re-compiles the pattern every evaluation without a custom escape sequence
                        thunkFactory.thunkEnv(metas) { env ->
                            val value = valueThunk(env)
                            val pattern = patternThunk(env)
                            val pps = getPatternParts(pattern, null)
                            runPatternParts(value, pps)
                        }
                    }
                    else -> {
                        //thunk that re-compiles the pattern every evaluation but *with* a custom escape sequence
                        val escapeThunk = compileExpr(escapeExpr)
                        thunkFactory.thunkEnv(metas) { env ->
                            val value = valueThunk(env)
                            val pattern = patternThunk(env)
                            val escape = escapeThunk(env)
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
                             errorContextFrom(locationMeta),
                             internal = false)
        when (escapeChar) {
            ""   -> {
                err(
                    "Cannot use empty character as ESCAPE character in a LIKE predicate: $escape",
                    errorContextFrom(locationMeta),
                    internal = false)
            }
            else -> {
                if (escapeChar.trim().length != 1) {
                    err(
                        "Escape character must have size 1 : $escapeChar",
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

    private fun compileExec(node: PartiqlAst.Statement.Exec): ThunkEnv {
        val metas = node.metas.toPartiQlMetaContainer()
        val procedure = procedures[node.procedureName.text] ?: err(
            "No such stored procedure: ${node.procedureName.text}",
            ErrorCode.EVALUATOR_NO_SUCH_PROCEDURE,
            errorContextFrom(metas).also {
                it[Property.PROCEDURE_NAME] = node.procedureName.text
            },
            internal = false)

        // Check arity
        if (node.args.size !in procedure.signature.arity) {
            val errorContext = errorContextFrom(metas).also {
                it[Property.EXPECTED_ARITY_MIN] = procedure.signature.arity.first
                it[Property.EXPECTED_ARITY_MAX] = procedure.signature.arity.last
            }

            val message = when {
                procedure.signature.arity.first == 1 && procedure.signature.arity.last == 1 ->
                    "${procedure.signature.name} takes a single argument, received: ${node.args.size}"
                procedure.signature.arity.first == procedure.signature.arity.last ->
                    "${procedure.signature.name} takes exactly ${procedure.signature.arity.first} arguments, received: ${node.args.size}"
                else ->
                    "${procedure.signature.name} takes between ${procedure.signature.arity.first} and " +
                        "${procedure.signature.arity.last} arguments, received: ${node.args.size}"
            }

            throw EvaluationException(message,
                ErrorCode.EVALUATOR_INCORRECT_NUMBER_OF_ARGUMENTS_TO_PROCEDURE_CALL,
                errorContext,
                internal = false)
        }

        // Compile the procedure's arguments
        val argThunks = node.args.map { compileExpr(it) }

        return thunkFactory.thunkEnv(metas) { env ->
            val procedureArgValues = argThunks.map { it(env) }
            procedure.call(env.session, procedureArgValues)
        }
    }

    private fun compileDate(node: PartiqlAst.Expr.Date): ThunkEnv {
        val value = valueFactory.newDate(node.year.value.toInt(), node.month.value.toInt(), node.day.value.toInt())
        return thunkFactory.thunkEnv(node.metas.toPartiQlMetaContainer()) { value }
    }

    private fun compileTime(node: PartiqlAst.Expr.LitTime) : ThunkEnv {
        val hour = node.value.hour.value.toInt()
        val minute = node.value.hour.value.toInt()
        val second = node.value.second.value.toInt()
        val nano = node.value.nano.value.toInt()
        val precision = node.value.precision.value.toInt()
        val tzMinutes = node.value.tzMinutes?.value?.toInt()

        val timeValue = valueFactory.newTime(Time.of(hour, minute, second, nano, precision, tzMinutes))

        return thunkFactory.thunkEnv(node.value.metas.toPartiQlMetaContainer()) {
            timeValue
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
        this is UnpivotedExprValue   -> this
        // Wrap into a pseudo-BAG
        type == ExprValueType.STRUCT -> UnpivotedExprValue(this)
        // for non-struct, this wraps any value into a BAG with a synthetic name
        else                         -> UnpivotedExprValue(
            listOf(
                this.namedValue(valueFactory.newString(syntheticColumnName(0)))
            )
        )
    }


    private fun createStructExprValue(seq: Sequence<ExprValue>, ordering: StructOrdering) =
        valueFactory.newStruct(
            when(compileOptions.projectionIteration) {
                ProjectionIterationBehavior.FILTER_MISSING -> seq.filter { it.type != ExprValueType.MISSING }
                ProjectionIterationBehavior.UNFILTERED     -> seq
            },
            ordering
        )
}

/**
 * Contains data about a compiled from source, inculding its [Alias], [thunk],
 * type of [JoinExpansion] ([JoinExpansion.INNER] for single tables or `CROSS JOIN`S.) and [filter] criteria.
 */
internal data class CompiledFromSource(
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
internal data class FromProduction(
    val values: List<ExprValue>,
    val env: Environment)

/** Specifies the expansion for joins. */
internal enum class JoinExpansion {
    /** Default for non-joined values, CROSS and INNER JOIN. */
    INNER,
    /** Expansion mode for LEFT/RIGHT/FULL JOIN. */
    OUTER
}

internal data class CompiledLetSource(
    val name: String,
    val thunk: ThunkEnv)

internal enum class ExpressionContext {
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
internal class CompilationContext(val expressionContext: ExpressionContext, val fromSourceNames: Set<String>)
{
    fun createNested(expressionContext: ExpressionContext, fromSourceNames: Set<String>) =
        CompilationContext(expressionContext, fromSourceNames)
}

/**
 * Represents an element in a select list that is to be projected into the final result.
 * i.e. an expression, or a (project_all) node.
 */
internal sealed class ProjectionElement

/**
 * Represents a single compiled expression to be projected into the final result.
 * Given `SELECT a + b as value FROM foo`:
 * - `name` is "value"
 * - `thunk` is compiled expression, i.e. `a + b`
 */
internal class SingleProjectionElement(val name: ExprValue, val thunk: ThunkEnv) : ProjectionElement()

/**
 * Represents a wildcard ((path_project_all) node) expression to be projected into the final result.
 * This covers two cases.  For `SELECT foo.* FROM foo`, `exprThunks` contains a single compiled expression
 * `foo`.
 *
 * For `SELECT * FROM foo, bar, bat`, `exprThunks` would contain a compiled expression for each of `foo`, `bar` and
 * `bat`.
 */
internal class MultipleProjectionElement(val thunks: List<ThunkEnv>) : ProjectionElement()


internal val MetaContainer.sourceLocationMeta get() = (this.find(SourceLocationMeta.TAG) as? SourceLocationMeta)
