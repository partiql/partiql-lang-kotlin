package org.partiql.lang.eval.impl

import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.emptyMetaContainer
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.CoverageData
import org.partiql.lang.eval.CoverageStructure
import org.partiql.lang.eval.EvaluatingCompiler
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.Expression
import org.partiql.lang.eval.PartiQLResult
import org.partiql.lang.eval.ThunkEnv
import org.partiql.lang.eval.TypingMode
import org.partiql.lang.eval.booleanValue
import org.partiql.lang.eval.builtins.storedprocedure.StoredProcedure
import org.partiql.lang.eval.exprEquals
import org.partiql.lang.eval.isNotUnknown
import org.partiql.lang.eval.isUnknown
import org.partiql.lang.eval.physical.sourceLocationMeta
import org.partiql.lang.types.TypedOpParameter
import org.partiql.lang.util.ConfigurableExprValueFormatter

/**
 * This should only be used for a single query's compilation due to the attachment of unique ids to the nodes.
 */
internal class CoverageCompiler(
    functions: Map<String, ExprFunction>,
    customTypedOpParameters: Map<String, TypedOpParameter>,
    procedures: Map<String, StoredProcedure>,
    compileOptions: CompileOptions = CompileOptions.standard()
) : EvaluatingCompiler(functions, customTypedOpParameters, procedures, compileOptions) {

    // A unique identifier for each branch
    private var conditionCount: Int = 0
    private var branchCount: Int = 0

    // The boolean outcomes of each branch
    private val conditionCounts = mutableMapOf<String, Int>()
    private val branchCounts = mutableMapOf<String, Int>()

    // The line number for a decision. Key = decision id. Value = line number.
    private val conditionLocations = mutableMapOf<String, Int>()
    private val branchLocations = mutableMapOf<String, Int>()

    override fun compile(originalAst: PartiqlAst.Statement): Expression {
        val expression = super.compile(originalAst)
        return object : Expression {
            override val coverageStructure: CoverageStructure = CoverageStructure(
                conditionCount = conditionCount * 2,
                conditionLocations = conditionLocations.toMap(),
                branchCount = branchCount * 2,
                branchLocations = branchLocations.toMap()
            )

            @Suppress("DEPRECATION")
            override fun eval(session: EvaluationSession): ExprValue {
                return expression.eval(session)
            }

            override fun evaluate(session: EvaluationSession): PartiQLResult {
                val result = (expression.evaluate(session) as PartiQLResult.Value).value
                // TODO: This is a hack to materialize the ExprValue
                val str = ConfigurableExprValueFormatter.standard.format(result)
                return PartiQLResult.Value(
                    value = result,
                    coverageData = CoverageData(conditionCounts, branchCount = branchCounts)
                )
            }
        }
    }

    // TODO: Figure out how we can determine whether an ID should be a boolean.
    //  This will likely be addressed when we move towards static resolution in the successor to the EvaluatingCompiler
    override fun compileId(expr: PartiqlAst.Expr.Id, metas: MetaContainer): ThunkEnv {
        return super.compileId(expr, metas)
    }

    // TODO: Figure out how we can determine whether an ID should be a boolean.
    //  This will likely be addressed when we move towards static resolution in the successor to the EvaluatingCompiler
    override fun compileCall(expr: PartiqlAst.Expr.Call, metas: MetaContainer): ThunkEnv {
        return super.compileCall(expr, metas)
    }

    // NOTE: This should NOT be overridden.
    override fun compileLit(expr: PartiqlAst.Expr.Lit, metas: MetaContainer): ThunkEnv = super.compileLit(expr, metas)

    // NOTE: This should NOT be overridden.
    override fun compileAstExpr(expr: PartiqlAst.Expr): ThunkEnv = super.compileAstExpr(expr)

    //
    //
    // CONDITIONS
    //
    //

    override fun compileAnd(expr: PartiqlAst.Expr.And, metas: MetaContainer): ThunkEnv = compileCondition("AND", metas) {
        super.compileAnd(expr, metas)
    }

    override fun compileOr(expr: PartiqlAst.Expr.Or, metas: MetaContainer): ThunkEnv = compileCondition("OR", metas) {
        super.compileOr(expr, metas)
    }

    override fun compileNot(expr: PartiqlAst.Expr.Not, metas: MetaContainer): ThunkEnv = compileCondition("NOT", metas) {
        super.compileNot(expr, metas)
    }

    override fun compileGt(expr: PartiqlAst.Expr.Gt, metas: MetaContainer): ThunkEnv = compileCondition("GT", metas) {
        super.compileGt(expr, metas)
    }

    override fun compileGte(expr: PartiqlAst.Expr.Gte, metas: MetaContainer): ThunkEnv = compileCondition("GTE", metas) {
        super.compileGte(expr, metas)
    }

    override fun compileBetween(expr: PartiqlAst.Expr.Between, metas: MetaContainer): ThunkEnv = compileCondition("BETWEEN", metas) {
        super.compileBetween(expr, metas)
    }

    override fun compileEq(expr: PartiqlAst.Expr.Eq, metas: MetaContainer): ThunkEnv = compileCondition("EQ", metas) {
        super.compileEq(expr, metas)
    }

    override fun compileIs(expr: PartiqlAst.Expr.IsType, metas: MetaContainer): ThunkEnv = compileCondition("IS", metas) {
        super.compileIs(expr, metas)
    }

    override fun compileIn(expr: PartiqlAst.Expr.InCollection, metas: MetaContainer): ThunkEnv = compileCondition("IN", metas) {
        super.compileIn(expr, metas)
    }

    override fun compileCanCast(expr: PartiqlAst.Expr.CanCast, metas: MetaContainer): ThunkEnv = compileCondition("CAN_CAST", metas) {
        super.compileCanCast(expr, metas)
    }

    override fun compileCanLosslessCast(expr: PartiqlAst.Expr.CanLosslessCast, metas: MetaContainer): ThunkEnv = compileCondition("CAN_LOSSLESS_CAST", metas) {
        super.compileCanLosslessCast(expr, metas)
    }

    override fun compileLt(expr: PartiqlAst.Expr.Lt, metas: MetaContainer): ThunkEnv = compileCondition("LT", metas) {
        super.compileLt(expr, metas)
    }

    override fun compileLte(expr: PartiqlAst.Expr.Lte, metas: MetaContainer): ThunkEnv = compileCondition("LTEQ", metas) {
        super.compileLte(expr, metas)
    }

    override fun compileNe(expr: PartiqlAst.Expr.Ne, metas: MetaContainer): ThunkEnv = compileCondition("NEQ", metas) {
        super.compileNe(expr, metas)
    }
    
    //
    //
    // BRANCHES
    //
    //

    override fun compileWhere(node: PartiqlAst.Expr): ThunkEnv = compileBranch("WHERE", node.metas) {
        super.compileWhere(node)
    }

    override fun compileHaving(node: PartiqlAst.Expr) = compileBranch("HAVING", node.metas) {
        super.compileHaving(node)
    }

    /**
     * This one has a larger override as we need to inject more-specific information regarding branching.
     * The majority of the compilation logic should match the [EvaluatingCompiler]
     */
    override fun compileSimpleCase(expr: PartiqlAst.Expr.SimpleCase, metas: MetaContainer): ThunkEnv {
        val valueThunk = compileAstExpr(expr.expr)
        val branchThunks = expr.cases.pairs.map {
            Pair(
                compileBranchWithoutCheck("WHEN", it.first.metas) { compileAstExpr(it.first) },
                compileAstExpr(it.second)
            )
        }
        val elseThunk = when (val default = expr.default) {
            null -> thunkFactory.thunkEnv(metas) { ExprValue.nullValue }
            else -> compileAstExpr(default)
        }

        return thunkFactory.thunkEnv(metas) thunk@{ env ->
            val caseValue = valueThunk(env)
            // if the case value is unknown then we can short-circuit to the elseThunk directly
            when {
                caseValue.isUnknown() -> elseThunk(env)
                else -> {
                    branchThunks.forEach { bt ->
                        val compiledWhen = bt.first
                        val branchValue = compiledWhen.thunk(env)
                        // Just skip any branch values that are unknown, which we consider the same as false here.
                        when {
                            branchValue.isUnknown() -> { /* intentionally blank */
                            }
                            else -> {
                                val result = caseValue.exprEquals(branchValue)
                                when (result) {
                                    true -> branchCounts[compiledWhen.truthId] = branchCounts[compiledWhen.truthId]!! + 1
                                    false -> branchCounts[compiledWhen.falseId] = branchCounts[compiledWhen.falseId]!! + 1
                                }
                                if (result) {
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

    override fun compileSearchedCase(expr: PartiqlAst.Expr.SearchedCase, metas: MetaContainer): ThunkEnv {
        val branchThunks = expr.cases.pairs.map {
            compileBranchWithoutCheck("WHEN", it.first.metas) { compileAstExpr(it.first) } to compileAstExpr(it.second)
        }
        val elseThunk = when (val default = expr.default) {
            null -> thunkFactory.thunkEnv(metas) { ExprValue.nullValue }
            else -> compileAstExpr(default)
        }

        return when (compileOptions.typingMode) {
            TypingMode.LEGACY -> thunkFactory.thunkEnv(metas) thunk@{ env ->
                branchThunks.forEach { bt ->
                    val compiledWhen = bt.first
                    val conditionValue = compiledWhen.thunk(env)
                    // Any unknown value is considered the same as false.
                    // Note that .booleanValue() here will throw an EvaluationException if
                    // the data type is not boolean.
                    // TODO:  .booleanValue does not have access to metas, so the EvaluationException is reported to be
                    // at the line & column of the CASE statement, not the predicate, unfortunately.
                    if (conditionValue.isNotUnknown()) {
                        val result = conditionValue.booleanValue()
                        when (result) {
                            true -> branchCounts[compiledWhen.truthId] = branchCounts[compiledWhen.truthId]!! + 1
                            false -> branchCounts[compiledWhen.falseId] = branchCounts[compiledWhen.falseId]!! + 1
                        }
                        if (result) {
                            return@thunk bt.second(env)
                        }
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
                    val compiledWhen = bt.first
                    val conditionValue = compiledWhen.thunk(env)
                    if (conditionValue.type == ExprValueType.BOOL) {
                        val result = conditionValue.booleanValue()
                        when (result) {
                            true -> branchCounts[compiledWhen.truthId] = branchCounts[compiledWhen.truthId]!! + 1
                            false -> branchCounts[compiledWhen.falseId] = branchCounts[compiledWhen.falseId]!! + 1
                        }
                        if (result) {
                            return@thunk bt.second(env)
                        }
                    }
                }
                elseThunk(env)
            }
        }
    }
    
    private fun compileBranch(operand: String?, metas: MetaContainer = emptyMetaContainer(), compilation: () -> ThunkEnv) : ThunkEnv {
        val branchThunkEnv = compileBranchWithoutCheck(operand, metas, compilation)

        // Get Boolean Decision/Outcome
        val thunk = branchThunkEnv.thunk
        return { env ->
            val resultExprValue = thunk.invoke(env)
            if (resultExprValue.type == ExprValueType.BOOL) {
                when (resultExprValue.booleanValue()) {
                    true -> branchCounts[branchThunkEnv.truthId] = branchCounts[branchThunkEnv.truthId]!! + 1
                    false -> branchCounts[branchThunkEnv.falseId] = branchCounts[branchThunkEnv.falseId]!! + 1
                }
            }
            resultExprValue
        }
    }

    private fun compileBranchWithoutCheck(operand: String?, metas: MetaContainer = emptyMetaContainer(), compilation: () -> ThunkEnv): BranchThunkEnv {
        val uniqueDecisionId = ++branchCount
        val truthId = "B$uniqueDecisionId::$operand::T"
        val falseId = "B$uniqueDecisionId::$operand::F"
        branchCounts[truthId] = 0
        branchCounts[falseId] = 0

        // Add Location Information
        metas.sourceLocationMeta?.let { location ->
            branchLocations[truthId] = location.lineNum.toInt()
            branchLocations[falseId] = location.lineNum.toInt()
        } ?: run {
            branchLocations[truthId] = -1
            branchLocations[falseId] = -1
        }

        return BranchThunkEnv(
            truthId,
            falseId,
            compilation.invoke()
        )
    }
    
    private class BranchThunkEnv(
        val truthId: String,
        val falseId: String,
        val thunk: ThunkEnv
    )

    private fun compileCondition(operand: String? = null, metas: MetaContainer = emptyMetaContainer(), compilation: () -> ThunkEnv): ThunkEnv {
        val uniqueDecisionId = ++conditionCount
        val truthId = "C$uniqueDecisionId::$operand::T"
        val falseId = "C$uniqueDecisionId::$operand::F"
        conditionCounts[truthId] = 0
        conditionCounts[falseId] = 0

        // Add Location Information
        metas.sourceLocationMeta?.let { location ->
            conditionLocations[truthId] = location.lineNum.toInt()
            conditionLocations[falseId] = location.lineNum.toInt()
        } ?: run {
            conditionLocations[truthId] = -1
            conditionLocations[falseId] = -1
        }

        // Get Boolean Decision/Outcome
        val thunk = compilation.invoke()
        return { env ->
            val resultExprValue = thunk.invoke(env)
            if (resultExprValue.type == ExprValueType.BOOL) {
                when (resultExprValue.booleanValue()) {
                    true -> conditionCounts[truthId] = conditionCounts[truthId]!! + 1
                    false -> conditionCounts[falseId] = conditionCounts[falseId]!! + 1
                }
            }
            resultExprValue
        }
    }
}
