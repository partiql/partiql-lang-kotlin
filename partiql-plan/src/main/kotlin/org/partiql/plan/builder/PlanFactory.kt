package org.partiql.plan.builder

import com.amazon.ionelement.api.IonElement
import org.partiql.plan.Arg
import org.partiql.plan.Binding
import org.partiql.plan.Branch
import org.partiql.plan.Case
import org.partiql.plan.Common
import org.partiql.plan.Field
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.Property
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.SortSpec
import org.partiql.plan.Step
import org.partiql.types.StaticType
import kotlin.Any
import kotlin.String
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.Set
import kotlin.jvm.JvmStatic

public abstract class PlanFactory {
    public open fun partiQLPlan(version: PartiQLPlan.Version, root: Rex) = PartiQLPlan(version, root)

    public open fun common(
        schema: Map<String, Arg.Type>,
        properties: Set<Property>,
        metas: Map<String, Any>
    ) = Common(schema, properties, metas)

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

    public open fun argValue(name: String?, `value`: Rex) = Arg.Value(name, value)

    public open fun argType(name: String?, type: StaticType) = Arg.Type(name, type)

    public open fun branch(condition: Rex, `value`: Rex) = Branch(condition, value)

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
        qualifier: Rex.Id.Qualifier,
        type: StaticType?
    ) = Rex.Id(name, case, qualifier, type)

    public open fun rexPath(
        root: Rex,
        steps: List<Step>,
        type: StaticType?
    ) = Rex.Path(root, steps, type)

    public open fun rexLit(`value`: IonElement, type: StaticType?) = Rex.Lit(value, type)

    public open fun rexUnary(
        `value`: Rex,
        op: Rex.Unary.Op,
        type: StaticType?
    ) = Rex.Unary(value, op, type)

    public open fun rexBinary(
        lhs: Rex,
        rhs: Rex,
        op: Rex.Binary.Op,
        type: StaticType?
    ) = Rex.Binary(lhs, rhs, op, type)

    public open fun rexCall(
        id: String,
        args: List<Arg>,
        type: StaticType?
    ) = Rex.Call(id, args, type)

    public open fun rexSwitch(
        match: Rex?,
        branches: List<Branch>,
        default: Rex?
    ) = Rex.Switch(match, branches, default)

    public open fun rexAgg(
        id: String,
        args: List<Rex>,
        modifier: Rex.Agg.Modifier,
        type: StaticType?
    ) = Rex.Agg(id, args, modifier, type)

    public open fun rexCollectionArray(values: List<Rex>, type: StaticType?) =
        Rex.Collection.Array(values, type)

    public open fun rexCollectionBag(values: List<Rex>, type: StaticType?) =
        Rex.Collection.Bag(values, type)

    public open fun rexTuple(fields: List<Field>, type: StaticType?) = Rex.Tuple(fields, type)

    public open fun rexQueryScalarSubquery(query: Rex.Query.Collection, type: StaticType?) =
        Rex.Query.Scalar.Subquery(query, type)

    public open fun rexQueryScalarPivot(
        rel: Rel,
        `value`: Rex,
        at: Rex,
        type: StaticType?
    ) = Rex.Query.Scalar.Pivot(rel, value, at, type)

    public open fun rexQueryCollection(rel: Rel, `constructor`: Rex?) = Rex.Query.Collection(
        rel,
        constructor
    )

    public companion object {
        public val DEFAULT: PlanFactory = object : PlanFactory() {}

        @JvmStatic
        public fun <T : PlanNode> create(block: PlanFactory.() -> T) = DEFAULT.block()
    }
}
