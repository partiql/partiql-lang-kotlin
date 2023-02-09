package org.partiql.ir.rel.builder

import org.partiql.ir.rel.Binding
import org.partiql.ir.rel.Common
import org.partiql.ir.rel.Property
import org.partiql.ir.rel.Rel
import org.partiql.lang.domains.PartiqlAst.Expr

public abstract class PlanFactory {
    public open fun common(
        schema: Map<String, Rel.Join.Type>,
        properties: Set<Property>,
        metas: Map<String, Any>
    ) = Common(schema, properties, metas)

    public open fun relScan(
        common: Common,
        `value`: Expr,
        `as`: String?,
        at: String?,
        `by`: String?
    ) = Rel.Scan(common, value, `as`, at, by)

    public open fun relCross(
        common: Common,
        lhs: Rel,
        rhs: Rel
    ) = Rel.Cross(common, lhs, rhs)

    public open fun relFilter(
        common: Common,
        input: Rel,
        condition: Expr
    ) = Rel.Filter(common, input, condition)

    public open fun relSort(
        common: Common,
        expr: Expr,
        dir: Rel.Sort.Dir,
        nulls: Rel.Sort.Nulls
    ) = Rel.Sort(common, expr, dir, nulls)

    public open fun relBag(
        common: Common,
        lhs: Rel,
        rhs: Rel,
        op: Rel.Bag.Op
    ) = Rel.Bag(common, lhs, rhs, op)

    public open fun relFetch(
        common: Common,
        input: Rel,
        limit: Long,
        offset: Long
    ) = Rel.Fetch(common, input, limit, offset)

    public open fun relProject(
        common: Common,
        input: Rel,
        exprs: List<Binding>
    ) = Rel.Project(common, input, exprs)

    public open fun relJoin(
        common: Common,
        lhs: Rel,
        rhs: Rel,
        condition: Expr?,
        type: Rel.Join.Type
    ) = Rel.Join(common, lhs, rhs, condition, type)

    public open fun relAggregate(
        common: Common,
        input: Rel,
        calls: List<Binding>,
        groups: List<Expr>
    ) = Rel.Aggregate(common, input, calls, groups)

    public open fun binding(name: String, `value`: Expr) = Binding(name, value)

    public companion object {
        public val DEFAULT: PlanFactory = object : PlanFactory() {}
    }
}
