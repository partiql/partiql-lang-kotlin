package org.partiql.lang.eval.physical

import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.Thunk
import org.partiql.lang.eval.ThunkValue
import org.partiql.lang.eval.physical.operators.DEFAULT_OPERATOR_COMPILERS
import org.partiql.lang.eval.physical.operators.ThunkConverter

// DL TOOD: relocate this--doesn't really belong here anymore.
const val DEFAULT_IMPL_NAME = "default"

/** A specialization of [Thunk] that we use for evaluation of value expressions physical plans. */
typealias ExprThunkEnv = Thunk<EvaluatorState>

/** A specialization of [ThunkValue] that we use for evaluation of physical plans. */
internal typealias ExprThunkValue<T> = ThunkValue<EvaluatorState, T>

internal class PhysicalBexprToThunkConverter(
    private val exprConverter: PhysicalExprToThunkConverter,
    private val valueFactory: ExprValueFactory,
) : PartiqlPhysical.Bexpr.Converter<RelationThunkEnv> {

    // DL TODO: make constructor argument.
    private val operators = DEFAULT_OPERATOR_COMPILERS

    private val compiler = object : ThunkConverter {
        override fun compile(bexpr: PartiqlPhysical.Bexpr): RelationThunkEnv =
            this@PhysicalBexprToThunkConverter.convert(bexpr)

        override fun compile(expr: PartiqlPhysical.Expr): ExprThunkEnv =
            this@PhysicalBexprToThunkConverter.exprConverter.convert(expr)
    }

    private fun <T : PartiqlPhysical.Bexpr> compileOperator(
        implementationName: String,
        operatorClass: Class<T>,
        node: T
    ) =
        operators.getOperator(implementationName, operatorClass)
            .compile(node, compiler, valueFactory)

    override fun convertProject(node: PartiqlPhysical.Bexpr.Project): RelationThunkEnv =
        compileOperator(node.i.name.text, PartiqlPhysical.Bexpr.Project::class.java, node)

    override fun convertScan(node: PartiqlPhysical.Bexpr.Scan): RelationThunkEnv =
        compileOperator(node.i.name.text, PartiqlPhysical.Bexpr.Scan::class.java, node)

    override fun convertFilter(node: PartiqlPhysical.Bexpr.Filter): RelationThunkEnv =
        compileOperator(node.i.name.text, PartiqlPhysical.Bexpr.Filter::class.java, node)

    override fun convertJoin(node: PartiqlPhysical.Bexpr.Join): RelationThunkEnv =
        compileOperator(node.i.name.text, PartiqlPhysical.Bexpr.Join::class.java, node)

    override fun convertOffset(node: PartiqlPhysical.Bexpr.Offset): RelationThunkEnv =
        compileOperator(node.i.name.text, PartiqlPhysical.Bexpr.Offset::class.java, node)

    override fun convertLimit(node: PartiqlPhysical.Bexpr.Limit): RelationThunkEnv =
        compileOperator(node.i.name.text, PartiqlPhysical.Bexpr.Limit::class.java, node)

    override fun convertLet(node: PartiqlPhysical.Bexpr.Let): RelationThunkEnv =
        compileOperator(node.i.name.text, PartiqlPhysical.Bexpr.Let::class.java, node)
}
