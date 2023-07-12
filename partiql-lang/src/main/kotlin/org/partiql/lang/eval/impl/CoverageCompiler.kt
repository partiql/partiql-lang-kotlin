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
import org.partiql.lang.eval.booleanValue
import org.partiql.lang.eval.builtins.storedprocedure.StoredProcedure
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
    private var uniqueId: Int = 0

    // The boolean outcomes of each branch
    private val decisions = mutableMapOf<String, Int>()

    // The line number for a decision. Key = decision id. Value = line number.
    private val locations = mutableMapOf<String, Int>()

    override fun compile(originalAst: PartiqlAst.Statement): Expression {
        val expression = super.compile(originalAst)
        return object : Expression {
            override val coverageStructure: CoverageStructure = CoverageStructure(uniqueId * 2, locations.toMap())

            @Suppress("DEPRECATED")
            override fun eval(session: EvaluationSession): ExprValue {
                return expression.eval(session)
            }

            override fun evaluate(session: EvaluationSession): PartiQLResult {
                val result = (expression.evaluate(session) as PartiQLResult.Value).value
                // TODO: This is a hack to materialize the ExprValue
                val str = ConfigurableExprValueFormatter.standard.format(result)
                return PartiQLResult.Value(
                    value = result,
                    coverageData = CoverageData(decisions)
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

    override fun compileAnd(expr: PartiqlAst.Expr.And, metas: MetaContainer): ThunkEnv = compileDecision("AND", metas) {
        super.compileAnd(expr, metas)
    }

    override fun compileOr(expr: PartiqlAst.Expr.Or, metas: MetaContainer): ThunkEnv = compileDecision("OR", metas) {
        super.compileOr(expr, metas)
    }

    override fun compileNot(expr: PartiqlAst.Expr.Not, metas: MetaContainer): ThunkEnv = compileDecision("NOT", metas) {
        super.compileNot(expr, metas)
    }

    override fun compileGt(expr: PartiqlAst.Expr.Gt, metas: MetaContainer): ThunkEnv = compileDecision("GT", metas) {
        super.compileGt(expr, metas)
    }

    override fun compileGte(expr: PartiqlAst.Expr.Gte, metas: MetaContainer): ThunkEnv = compileDecision("GTE", metas) {
        super.compileGte(expr, metas)
    }

    override fun compileBetween(expr: PartiqlAst.Expr.Between, metas: MetaContainer): ThunkEnv = compileDecision("BETWEEN", metas) {
        super.compileBetween(expr, metas)
    }

    override fun compileEq(expr: PartiqlAst.Expr.Eq, metas: MetaContainer): ThunkEnv = compileDecision("EQ", metas) {
        super.compileEq(expr, metas)
    }

    override fun compileIs(expr: PartiqlAst.Expr.IsType, metas: MetaContainer): ThunkEnv = compileDecision("IS", metas) {
        super.compileIs(expr, metas)
    }

    override fun compileIn(expr: PartiqlAst.Expr.InCollection, metas: MetaContainer): ThunkEnv = compileDecision("IN", metas) {
        super.compileIn(expr, metas)
    }

    override fun compileCanCast(expr: PartiqlAst.Expr.CanCast, metas: MetaContainer): ThunkEnv = compileDecision("CAN_CAST", metas) {
        super.compileCanCast(expr, metas)
    }

    override fun compileCanLosslessCast(expr: PartiqlAst.Expr.CanLosslessCast, metas: MetaContainer): ThunkEnv = compileDecision("CAN_LOSSLESS_CAST", metas) {
        super.compileCanLosslessCast(expr, metas)
    }

    override fun compileLt(expr: PartiqlAst.Expr.Lt, metas: MetaContainer): ThunkEnv = compileDecision("LT", metas) {
        super.compileLt(expr, metas)
    }

    override fun compileLte(expr: PartiqlAst.Expr.Lte, metas: MetaContainer): ThunkEnv = compileDecision("LTEQ", metas) {
        super.compileLte(expr, metas)
    }

    override fun compileNe(expr: PartiqlAst.Expr.Ne, metas: MetaContainer): ThunkEnv = compileDecision("NEQ", metas) {
        super.compileNe(expr, metas)
    }

    private fun compileDecision(operand: String? = null, metas: MetaContainer = emptyMetaContainer(), compilation: () -> ThunkEnv): ThunkEnv {
        val uniqueDecisionId = ++uniqueId
        val truthId = "$operand (T) ($uniqueDecisionId)"
        val falseId = "$operand (F) ($uniqueDecisionId)"
        decisions[truthId] = 0
        decisions[falseId] = 0

        // Add Location Information
        metas.sourceLocationMeta?.let { location ->
            locations[truthId] = location.lineNum.toInt()
            locations[falseId] = location.lineNum.toInt()
        } ?: run {
            locations[truthId] = -1
            locations[falseId] = -1
        }

        // Get Boolean Decision/Outcome
        val thunk = compilation.invoke()
        return { env ->
            val resultExprValue = thunk.invoke(env)
            if (resultExprValue.type == ExprValueType.BOOL) {
                when (resultExprValue.booleanValue()) {
                    true -> decisions[truthId] = decisions[truthId]!! + 1
                    false -> decisions[falseId] = decisions[falseId]!! + 1
                }
            }
            resultExprValue
        }
    }
}
