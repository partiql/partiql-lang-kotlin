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
import java.util.Stack

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
    private val conditionCounts = mutableMapOf<String, Int>()
    private val branchCounts = mutableMapOf<String, Int>()
    private val conditions: MutableMap<String, CoverageStructure.BranchCondition> = mutableMapOf()
    private val branches: MutableMap<String, CoverageStructure.Branch> = mutableMapOf()
    private val contextStack: Stack<Context> = Stack()

    private enum class Context {
        IN_BRANCH,
        NOT_IN_BRANCH
    }

    override fun compile(originalAst: PartiqlAst.Statement): Expression {
        contextStack.push(Context.NOT_IN_BRANCH)
        val expression = super.compile(originalAst)
        return object : Expression {
            override val coverageStructure: CoverageStructure = CoverageStructure(
                branches = branches.toMap(),
                branchConditions = conditions.toMap()
            )

            @Suppress("DEPRECATION")
            override fun eval(session: EvaluationSession): ExprValue {
                return expression.eval(session)
            }

            override fun evaluate(session: EvaluationSession): PartiQLResult {
                val result = (expression.evaluate(session) as PartiQLResult.Value).value
                // TODO: This is a hack to materialize the ExprValue. Could be useful to use JMH's Blackhole.
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

    // TODO: Figure out how we can determine whether a function call should be a boolean.
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

    override fun compileAnd(expr: PartiqlAst.Expr.And, metas: MetaContainer): ThunkEnv = compileCondition(CoverageStructure.BranchCondition.Type.AND, metas) {
        super.compileAnd(expr, metas)
    }

    override fun compileOr(expr: PartiqlAst.Expr.Or, metas: MetaContainer): ThunkEnv = compileCondition(CoverageStructure.BranchCondition.Type.OR, metas) {
        super.compileOr(expr, metas)
    }

    override fun compileNot(expr: PartiqlAst.Expr.Not, metas: MetaContainer): ThunkEnv = compileCondition(CoverageStructure.BranchCondition.Type.NOT, metas) {
        super.compileNot(expr, metas)
    }

    override fun compileGt(expr: PartiqlAst.Expr.Gt, metas: MetaContainer): ThunkEnv = compileCondition(CoverageStructure.BranchCondition.Type.GT, metas) {
        super.compileGt(expr, metas)
    }

    override fun compileGte(expr: PartiqlAst.Expr.Gte, metas: MetaContainer): ThunkEnv = compileCondition(CoverageStructure.BranchCondition.Type.GTE, metas) {
        super.compileGte(expr, metas)
    }

    override fun compileBetween(expr: PartiqlAst.Expr.Between, metas: MetaContainer): ThunkEnv = compileCondition(CoverageStructure.BranchCondition.Type.BETWEEN, metas) {
        super.compileBetween(expr, metas)
    }

    override fun compileEq(expr: PartiqlAst.Expr.Eq, metas: MetaContainer): ThunkEnv = compileCondition(CoverageStructure.BranchCondition.Type.EQ, metas) {
        super.compileEq(expr, metas)
    }

    override fun compileIs(expr: PartiqlAst.Expr.IsType, metas: MetaContainer): ThunkEnv = compileCondition(CoverageStructure.BranchCondition.Type.IS, metas) {
        super.compileIs(expr, metas)
    }

    override fun compileIn(expr: PartiqlAst.Expr.InCollection, metas: MetaContainer): ThunkEnv = compileCondition(CoverageStructure.BranchCondition.Type.IN, metas) {
        super.compileIn(expr, metas)
    }

    override fun compileCanCast(expr: PartiqlAst.Expr.CanCast, metas: MetaContainer): ThunkEnv = compileCondition(CoverageStructure.BranchCondition.Type.CAN_CAST, metas) {
        super.compileCanCast(expr, metas)
    }

    override fun compileCanLosslessCast(expr: PartiqlAst.Expr.CanLosslessCast, metas: MetaContainer): ThunkEnv = compileCondition(CoverageStructure.BranchCondition.Type.CAN_LOSSLESS_CAST, metas) {
        super.compileCanLosslessCast(expr, metas)
    }

    override fun compileLt(expr: PartiqlAst.Expr.Lt, metas: MetaContainer): ThunkEnv = compileCondition(CoverageStructure.BranchCondition.Type.LT, metas) {
        super.compileLt(expr, metas)
    }

    override fun compileLte(expr: PartiqlAst.Expr.Lte, metas: MetaContainer): ThunkEnv = compileCondition(CoverageStructure.BranchCondition.Type.LTE, metas) {
        super.compileLte(expr, metas)
    }

    override fun compileNe(expr: PartiqlAst.Expr.Ne, metas: MetaContainer): ThunkEnv = compileCondition(CoverageStructure.BranchCondition.Type.NEQ, metas) {
        super.compileNe(expr, metas)
    }
    
    //
    //
    // BRANCHES
    //
    //

    override fun compileWhere(node: PartiqlAst.Expr): ThunkEnv = compileBranch(CoverageStructure.Branch.Type.WHERE, node.metas) {
        super.compileWhere(node)
    }

    override fun compileHaving(node: PartiqlAst.Expr) = compileBranch(CoverageStructure.Branch.Type.HAVING, node.metas) {
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
                compileBranchWithoutCheck(CoverageStructure.Branch.Type.CASE_WHEN, it.first.metas) { compileAstExpr(it.first) },
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
            compileBranchWithoutCheck(CoverageStructure.Branch.Type.CASE_WHEN, it.first.metas) { compileAstExpr(it.first) } to compileAstExpr(it.second)
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

    override fun compileSelect(selectExpr: PartiqlAst.Expr.Select, metas: MetaContainer): ThunkEnv {
        this.contextStack.push(Context.NOT_IN_BRANCH)
        return super.compileSelect(selectExpr, metas).also { this.contextStack.pop() }
    }

    private fun compileBranch(operand: CoverageStructure.Branch.Type, metas: MetaContainer = emptyMetaContainer(), compilation: () -> ThunkEnv) : ThunkEnv {
        val branchThunkEnv = compileBranchWithoutCheck(operand, metas, compilation)

        // Get Boolean Decision/Outcome
        val thunk = branchThunkEnv.thunk
        return { env ->
            val resultExprValue = thunk.invoke(env)

            when (resultExprValue.type) {
                ExprValueType.BOOL -> when (resultExprValue.booleanValue()) {
                    true -> branchCounts[branchThunkEnv.truthId] = branchCounts[branchThunkEnv.truthId]!! + 1
                    false -> branchCounts[branchThunkEnv.falseId] = branchCounts[branchThunkEnv.falseId]!! + 1
                }
                ExprValueType.NULL -> branchCounts[branchThunkEnv.falseId] = branchCounts[branchThunkEnv.falseId]!! + 1
                ExprValueType.MISSING -> branchCounts[branchThunkEnv.falseId] = branchCounts[branchThunkEnv.falseId]!! + 1
                else -> { /* Do nothing */ }
            }
            resultExprValue
        }
    }

    private fun compileBranchWithoutCheck(operand: CoverageStructure.Branch.Type, metas: MetaContainer = emptyMetaContainer(), compilation: () -> ThunkEnv): BranchThunkEnv {
        this.contextStack.push(Context.IN_BRANCH)
        val truthId = "B${++branchCount}"
        val falseId = "B${++branchCount}"
        branchCounts[truthId] = 0
        branchCounts[falseId] = 0

        // Add Location Information
        val lineNumber = metas.sourceLocationMeta?.lineNum ?: -1L
        branches[truthId] = CoverageStructure.Branch(truthId, operand, outcome = CoverageStructure.Branch.Outcome.TRUE, lineNumber)
        branches[falseId] = CoverageStructure.Branch(falseId, operand, outcome = CoverageStructure.Branch.Outcome.FALSE, lineNumber)

        return BranchThunkEnv(
            truthId,
            falseId,
            compilation.invoke()
        ).also {
            this.contextStack.pop()
        }
    }
    
    private class BranchThunkEnv(
        val truthId: String,
        val falseId: String,
        val thunk: ThunkEnv
    )

    private fun compileCondition(operand: CoverageStructure.BranchCondition.Type, metas: MetaContainer = emptyMetaContainer(), compilation: () -> ThunkEnv): ThunkEnv {
        // Make sure the condition is in a BRANCH
        if (this.contextStack.peek() == Context.NOT_IN_BRANCH) { return compilation.invoke() }

        val truthId = "C${++conditionCount}"
        val falseId = "C${++conditionCount}"
        val nullId = "C${++conditionCount}"
        val missingId = "C${++conditionCount}"
        conditionCounts[truthId] = 0
        conditionCounts[falseId] = 0
        conditionCounts[nullId] = 0

        // Add Location Information
        val lineNumber = metas.sourceLocationMeta?.lineNum ?: -1L
        conditions[truthId] = CoverageStructure.BranchCondition(truthId, operand, outcome = CoverageStructure.BranchCondition.Outcome.TRUE, lineNumber)
        conditions[falseId] = CoverageStructure.BranchCondition(falseId, operand, outcome = CoverageStructure.BranchCondition.Outcome.FALSE, lineNumber)
        conditions[nullId] = CoverageStructure.BranchCondition(nullId, operand, outcome = CoverageStructure.BranchCondition.Outcome.NULL, lineNumber)

        // Handle Permissive Mode
        if (compileOptions.typingMode == TypingMode.PERMISSIVE) {
            conditionCounts[missingId] = 0
            conditions[missingId] = CoverageStructure.BranchCondition(
                missingId,
                operand,
                outcome = CoverageStructure.BranchCondition.Outcome.MISSING,
                lineNumber
            )
        }

        // Get Boolean Decision/Outcome
        val thunk = compilation.invoke()
        return { env ->
            val resultExprValue = thunk.invoke(env)
            when (resultExprValue.type) {
                ExprValueType.BOOL -> when (resultExprValue.booleanValue()) {
                    true -> conditionCounts[truthId] = conditionCounts[truthId]!! + 1
                    false -> conditionCounts[falseId] = conditionCounts[falseId]!! + 1
                }
                ExprValueType.NULL -> conditionCounts[nullId] = conditionCounts[nullId]!! + 1
                // We should never receive MISSING unless in permissive mode
                ExprValueType.MISSING -> conditionCounts[missingId] = (conditionCounts[missingId] ?: 0) + 1
                else -> { /* Do nothing */ }
            }
            resultExprValue
        }
    }
}
