package org.partiql.plan.ir.builder

import com.amazon.ionelement.api.IonElement
import org.partiql.plan.ir.Attribute
import org.partiql.plan.ir.Binding
import org.partiql.plan.ir.Case
import org.partiql.plan.ir.Common
import org.partiql.plan.ir.Field
import org.partiql.plan.ir.PartiQLPlan
import org.partiql.plan.ir.PlanNode
import org.partiql.plan.ir.Property
import org.partiql.plan.ir.Rel
import org.partiql.plan.ir.Rex
import org.partiql.plan.ir.SortSpec
import org.partiql.plan.ir.Step
import org.partiql.lang.types.StaticType
import kotlin.Any
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.Set
import kotlin.jvm.JvmStatic

public abstract class PlanFactory {
    public open fun partiQLPlan(version: PartiQLPlan.Version, root: Rex) = PartiQLPlan(version, root)

    public open fun common(
        schema: List<Attribute>,
        properties: Set<Property>,
        metas: Map<String, Any>
    ) = Common(schema, properties, metas)

    public open fun attribute(name: String, type: StaticType) = Attribute(name, type)

    public open fun binding(name: String, `value`: Rex) = Binding(name, value)

    public open fun `field`(name: Rex, `value`: Rex) = Field(name, value)

    public open fun stepKey(`value`: Rex, case: Case?) = Step.Key(value, case)

    public open fun stepWildcard() = Step.Wildcard()

    public open fun stepUnpivot() = Step.Unpivot()

    public open fun sortSpec(
        `value`: Rex,
        dir: SortSpec.Dir,
        nulls: SortSpec.Nulls
    ) = SortSpec(value, dir, nulls)

    public open fun relScan(
        common: Common,
        `value`: Rex,
        alias: String?,
        at: String?,
        `by`: String?
    ) = Rel.Scan(common, value, alias, at, by)

    public open fun relUnpivot(
        common: Common,
        `value`: Rex,
        alias: String?,
        at: String?,
        `by`: String?
    ) = Rel.Unpivot(common, value, alias, at, by)

    public open fun relFilter(
        common: Common,
        input: Rel,
        condition: Rex
    ) = Rel.Filter(common, input, condition)

    public open fun relSort(
        common: Common,
        input: Rel,
        specs: List<SortSpec>
    ) = Rel.Sort(common, input, specs)

    public open fun relBag(
        common: Common,
        lhs: Rel,
        rhs: Rel,
        op: Rel.Bag.Op
    ) = Rel.Bag(common, lhs, rhs, op)

    public open fun relFetch(
        common: Common,
        input: Rel,
        limit: Rex,
        offset: Rex
    ) = Rel.Fetch(common, input, limit, offset)

    public open fun relProject(
        common: Common,
        input: Rel,
        bindings: List<Binding>
    ) = Rel.Project(common, input, bindings)

    public open fun relJoin(
        common: Common,
        lhs: Rel,
        rhs: Rel,
        condition: Rex?,
        type: Rel.Join.Type
    ) = Rel.Join(common, lhs, rhs, condition, type)

    public open fun relAggregate(
        common: Common,
        input: Rel,
        calls: List<Binding>,
        groups: List<Binding>,
        strategy: Rel.Aggregate.Strategy
    ) = Rel.Aggregate(common, input, calls, groups, strategy)

    public open fun rexId(
        name: String,
        case: Case?,
        qualifier: Rex.Id.Qualifier
    ) = Rex.Id(name, case, qualifier)

    public open fun rexPath(root: Rex, steps: List<Step>) = Rex.Path(root, steps)

    public open fun rexUnary(`value`: Rex, op: Rex.Unary.Op) = Rex.Unary(value, op)

    public open fun rexBinary(
        lhs: Rex,
        rhs: Rex,
        op: Rex.Binary.Op
    ) = Rex.Binary(lhs, rhs, op)

    public open fun rexCall(id: String, args: List<Rex>) = Rex.Call(id, args)

    public open fun rexAgg(
        id: String,
        args: List<Rex>,
        modifier: Rex.Agg.Modifier
    ) = Rex.Agg(id, args, modifier)

    public open fun rexLit(`value`: IonElement) = Rex.Lit(value)

    public open fun rexCollection(type: Rex.Collection.Type, values: List<Rex>) = Rex.Collection(
        type,
        values
    )

    public open fun rexTuple(fields: List<Field>) = Rex.Tuple(fields)

    public open fun rexQueryScalarCoerce(query: Rex.Query.Collection) = Rex.Query.Scalar.Coerce(query)

    public open fun rexQueryScalarPivot(
        rel: Rel,
        `value`: Rex,
        at: Rex
    ) = Rex.Query.Scalar.Pivot(rel, value, at)

    public open fun rexQueryCollection(rel: Rel, `constructor`: Rex?) = Rex.Query.Collection(
        rel,
        constructor
    )

    public companion object {
        public val DEFAULT: PlanFactory = object : PlanFactory() {}

        @JvmStatic
        public fun <T : PlanNode> create(block: PlanFactory.() -> T) = PlanFactory.DEFAULT.block()
    }
}
