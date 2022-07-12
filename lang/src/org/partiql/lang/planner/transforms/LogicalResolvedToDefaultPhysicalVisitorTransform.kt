package org.partiql.lang.planner.transforms

import com.amazon.ionelement.api.ionSymbol
import org.partiql.lang.domains.PartiqlLogicalResolved
import org.partiql.lang.domains.PartiqlLogicalResolvedToPartiqlPhysicalVisitorTransform
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.planner.DML_COMMAND_FIELD_ACTION
import org.partiql.lang.planner.DML_COMMAND_FIELD_ROWS
import org.partiql.lang.planner.DML_COMMAND_FIELD_TARGET_UNIQUE_ID
import org.partiql.lang.planner.DmlAction

/**
 * Transforms an instance of [PartiqlLogicalResolved.Statement] to [PartiqlPhysical.Statement],
 * specifying `(impl default)` for each relational operator.
 */
internal fun PartiqlLogicalResolved.Plan.toDefaultPhysicalPlan() =
    LogicalResolvedToDefaultPhysicalVisitorTransform().transformPlan(this)

internal const val DEFAULT_IMPL_NAME = "default"
internal val DEFAULT_IMPL = PartiqlPhysical.build { impl(DEFAULT_IMPL_NAME) }

internal fun PartiqlPhysical.Builder.structField(name: String, value: String) =
    structField(lit(ionSymbol(name)), lit(ionSymbol(value)))

internal fun PartiqlPhysical.Builder.structField(name: String, value: PartiqlPhysical.Expr) =
    structField(lit(ionSymbol(name)), value)

internal class LogicalResolvedToDefaultPhysicalVisitorTransform : PartiqlLogicalResolvedToPartiqlPhysicalVisitorTransform() {

    /** Copies [PartiqlLogicalResolved.Bexpr.Scan] to [PartiqlPhysical.Bexpr.Scan], adding the default impl. */
    override fun transformBexprScan(node: PartiqlLogicalResolved.Bexpr.Scan): PartiqlPhysical.Bexpr {
        val thiz = this
        return PartiqlPhysical.build {
            scan(
                i = DEFAULT_IMPL,
                expr = thiz.transformExpr(node.expr),
                asDecl = thiz.transformVarDecl(node.asDecl),
                atDecl = node.atDecl?.let { thiz.transformVarDecl(it) },
                byDecl = node.byDecl?.let { thiz.transformVarDecl(it) },
                metas = node.metas
            )
        }
    }

    /** Copies [PartiqlLogicalResolved.Bexpr.Filter] to [PartiqlPhysical.Bexpr.Filter], adding the default impl. */
    override fun transformBexprFilter(node: PartiqlLogicalResolved.Bexpr.Filter): PartiqlPhysical.Bexpr {
        val thiz = this
        return PartiqlPhysical.build {
            filter(
                i = DEFAULT_IMPL,
                predicate = thiz.transformExpr(node.predicate),
                source = thiz.transformBexpr(node.source),
                metas = node.metas
            )
        }
    }

    override fun transformBexprJoin(node: PartiqlLogicalResolved.Bexpr.Join): PartiqlPhysical.Bexpr {
        val thiz = this
        return PartiqlPhysical.build {
            join(
                i = DEFAULT_IMPL,
                joinType = thiz.transformJoinType(node.joinType),
                left = thiz.transformBexpr(node.left),
                right = thiz.transformBexpr(node.right),
                predicate = node.predicate?.let { thiz.transformExpr(it) },
                metas = node.metas
            )
        }
    }

    override fun transformBexprOffset(node: PartiqlLogicalResolved.Bexpr.Offset): PartiqlPhysical.Bexpr {
        val thiz = this
        return PartiqlPhysical.build {
            offset(
                i = DEFAULT_IMPL,
                rowCount = thiz.transformExpr(node.rowCount),
                source = thiz.transformBexpr(node.source),
                metas = node.metas
            )
        }
    }

    override fun transformBexprLimit(node: PartiqlLogicalResolved.Bexpr.Limit): PartiqlPhysical.Bexpr {
        val thiz = this
        return PartiqlPhysical.build {
            limit(
                i = DEFAULT_IMPL,
                rowCount = thiz.transformExpr(node.rowCount),
                source = thiz.transformBexpr(node.source),
                metas = node.metas
            )
        }
    }

    override fun transformBexprLet(node: PartiqlLogicalResolved.Bexpr.Let): PartiqlPhysical.Bexpr {
        val thiz = this
        return PartiqlPhysical.build {
            let(
                i = DEFAULT_IMPL,
                source = thiz.transformBexpr(node.source),
                bindings = node.bindings.map { transformLetBinding(it) },
                metas = node.metas
            )
        }
    }

    private fun PartiqlLogicalResolved.DmlTarget.asActionTarget(): PartiqlPhysical.Expr.Lit =
        when (val target = this.target) {
            is PartiqlLogicalResolved.Expr.GlobalId ->
                PartiqlPhysical.build {
                    lit(ionSymbol(target.uniqueId.text))
                }
            else ->
                TODO("User-friendly error for invalid data manipulation target: $target")
        }

    override fun transformStatementDml(node: PartiqlLogicalResolved.Statement.Dml): PartiqlPhysical.Statement {
        val action = when (node.operation) {
            is PartiqlLogicalResolved.DmlOperation.DmlInsert -> DmlAction.INSERT
            is PartiqlLogicalResolved.DmlOperation.DmlDelete -> DmlAction.DELETE
        }.name.toLowerCase()

        return PartiqlPhysical.build {
            query(
                expr = struct(
                    structField(DML_COMMAND_FIELD_ACTION, action),
                    structField(DML_COMMAND_FIELD_TARGET_UNIQUE_ID, node.target.asActionTarget()),
                    structField(DML_COMMAND_FIELD_ROWS, transformExpr(node.rows)),
                    metas = node.metas
                ),
                isDml = true,
                metas = node.metas
            )
        }
    }

    override fun transformStatementQuery(node: PartiqlLogicalResolved.Statement.Query): PartiqlPhysical.Statement =
        PartiqlPhysical.build {
            query(
                transformExpr(node.expr),
                isDml = false, // DML is handled in transformStatementDml
                metas = node.metas
            )
        }
}
