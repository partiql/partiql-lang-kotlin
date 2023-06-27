package org.partiql.lang.eval.impl

import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.emptyMetaContainer
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.EvaluatingCompiler
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.Expression
import org.partiql.lang.eval.Statistics
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
    private val decisions = mutableMapOf<Int, MutableSet<Boolean>>()

    // The line number for a decision. Key = decision id. Value = line number.
    private val locations = mutableMapOf<Int, Int>()

    override fun compile(originalAst: PartiqlAst.Statement): Expression {
        val expression = super.compile(originalAst)
        return object : Expression {
            override fun eval(session: EvaluationSession): ExprValue {
                val value = expression.eval(session)
                // TODO: This is a hack to materialize the ExprValue
                val str = ConfigurableExprValueFormatter.standard.format(value)
                value.statistics = Statistics(uniqueId * 2, decisions.toMap(), locations.toMap())
                return value
            }
        }
    }

    // TODO: Figure out how we can determine whether an ID should be a boolean.
    override fun compileId(expr: PartiqlAst.Expr.Id, metas: MetaContainer): ThunkEnv {
        return super.compileId(expr, metas)
    }

    // TODO: Figure out how we can determine whether an ID should be a boolean.
    override fun compileCall(expr: PartiqlAst.Expr.Call, metas: MetaContainer): ThunkEnv {
        return super.compileCall(expr, metas)
    }

    // NOTE: This should NOT be overridden.
    override fun compileAnd(expr: PartiqlAst.Expr.And, metas: MetaContainer): ThunkEnv = super.compileAnd(expr, metas)

    // NOTE: This should NOT be overridden.
    override fun compileOr(expr: PartiqlAst.Expr.Or, metas: MetaContainer): ThunkEnv = super.compileOr(expr, metas)

    // NOTE: This should NOT be overridden.
    override fun compileLit(expr: PartiqlAst.Expr.Lit, metas: MetaContainer): ThunkEnv = super.compileLit(expr, metas)

    // NOTE: This should NOT be overridden.
    override fun compileNot(expr: PartiqlAst.Expr.Not, metas: MetaContainer): ThunkEnv = super.compileNot(expr, metas)

    // NOTE: This should NOT be overridden.
    override fun compileAstExpr(expr: PartiqlAst.Expr): ThunkEnv = super.compileAstExpr(expr)

    override fun compileGt(expr: PartiqlAst.Expr.Gt, metas: MetaContainer): ThunkEnv = compileDecision(metas) {
        super.compileGt(expr, metas)
    }

    override fun compileGte(expr: PartiqlAst.Expr.Gte, metas: MetaContainer): ThunkEnv = compileDecision(metas) {
        super.compileGte(expr, metas)
    }

    override fun compileBetween(expr: PartiqlAst.Expr.Between, metas: MetaContainer): ThunkEnv = compileDecision(metas) {
        super.compileBetween(expr, metas)
    }

    override fun compileEq(expr: PartiqlAst.Expr.Eq, metas: MetaContainer): ThunkEnv = compileDecision(metas) {
        super.compileEq(expr, metas)
    }

    override fun compileIs(expr: PartiqlAst.Expr.IsType, metas: MetaContainer): ThunkEnv = compileDecision(metas) {
        super.compileIs(expr, metas)
    }

    override fun compileIn(expr: PartiqlAst.Expr.InCollection, metas: MetaContainer): ThunkEnv = compileDecision(metas) {
        super.compileIn(expr, metas)
    }

    override fun compileCanCast(expr: PartiqlAst.Expr.CanCast, metas: MetaContainer): ThunkEnv = compileDecision(metas) {
        super.compileCanCast(expr, metas)
    }

    override fun compileCanLosslessCast(expr: PartiqlAst.Expr.CanLosslessCast, metas: MetaContainer): ThunkEnv = compileDecision(metas) {
        super.compileCanLosslessCast(expr, metas)
    }

    override fun compileLt(expr: PartiqlAst.Expr.Lt, metas: MetaContainer): ThunkEnv = compileDecision(metas) {
        super.compileLt(expr, metas)
    }

    override fun compileLte(expr: PartiqlAst.Expr.Lte, metas: MetaContainer): ThunkEnv = compileDecision(metas) {
        super.compileLte(expr, metas)
    }

    override fun compileNe(expr: PartiqlAst.Expr.Ne, metas: MetaContainer): ThunkEnv = compileDecision(metas) {
        super.compileNe(expr, metas)
    }

    private fun compileDecision(metas: MetaContainer = emptyMetaContainer(), compilation: () -> ThunkEnv): ThunkEnv {
        val nodeId = ++uniqueId

        // Add Location Information
        metas.sourceLocationMeta?.let { location ->
            locations[nodeId] = location.lineNum.toInt()
        }

        // Get Boolean Decision/Outcome
        val thunk = compilation.invoke()
        return { env ->
            val resultExprValue = thunk.invoke(env)
            if (resultExprValue.type == ExprValueType.BOOL) {
                val result = resultExprValue.booleanValue()
                decisions.putIfAbsent(nodeId, mutableSetOf(result))?.add(result)
            }
            resultExprValue
        }
    }
}
