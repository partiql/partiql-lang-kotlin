package org.partiql.planner.transforms

import org.partiql.lang.domains.PartiqlLogicalResolved
import org.partiql.lang.domains.PartiqlLogicalResolvedToPartiqlPhysicalVisitorTransform
import org.partiql.lang.domains.PartiqlPhysical

/**
 * Transforms an instance of [PartiqlLogicalResolved.Statement] to [PartiqlPhysical.Statement],
 * specifying `(impl default)` for each relational operator.
 */
internal fun PartiqlLogicalResolved.Statement.toPhysical() =
    LogicalResolvedToPhysicalVisitorTransform().transformStatement(this)

internal val DEFAULT_IMPL = PartiqlPhysical.build { impl("default") }

internal class LogicalResolvedToPhysicalVisitorTransform : PartiqlLogicalResolvedToPartiqlPhysicalVisitorTransform() {

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
                predicate = thiz.transformExpr(node.predicate),
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
                bindings = node.bindings.map { transformLetBinding(it) }
            )
        }
    }
}
