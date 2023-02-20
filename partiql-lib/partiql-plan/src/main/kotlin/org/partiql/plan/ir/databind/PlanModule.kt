package org.partiql.plan.ir.databind

import com.amazon.ionelement.api.loadSingleElement
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.module.SimpleModule
import org.partiql.plan.ir.Binding
import org.partiql.plan.ir.Common
import org.partiql.plan.ir.Plan
import org.partiql.plan.ir.PlanNode
import org.partiql.plan.ir.Rel
import org.partiql.plan.ir.Rex
import org.partiql.plan.ir.SortSpec
import org.partiql.plan.ir.Step
import org.partiql.plan.ir.builder.PlanFactory
import org.partiql.plan.ir.databind.PlanModule.Mapping

public class PlanModule(
    private val factory: PlanFactory = PlanFactory.DEFAULT
) : SimpleModule() {
    private val _base: Mapping<PlanNode> = Mapping {
        when (val id = it.id()) {
            "plan" -> _plan(it)
            "common" -> _common(it)
            "binding" -> _binding(it)
            "step" -> _step(it)
            "step.rex" -> _stepRex(it)
            "step.wildcard" -> _stepWildcard(it)
            "step.unpivot" -> _stepUnpivot(it)
            "sort_spec" -> _sortSpec(it)
            "rel" -> _rel(it)
            "rel.scan" -> _relScan(it)
            "rel.unpivot" -> _relUnpivot(it)
            "rel.filter" -> _relFilter(it)
            "rel.sort" -> _relSort(it)
            "rel.bag" -> _relBag(it)
            "rel.fetch" -> _relFetch(it)
            "rel.project" -> _relProject(it)
            "rel.join" -> _relJoin(it)
            "rel.aggregate" -> _relAggregate(it)
            "rex" -> _rex(it)
            "rex.id" -> _rexId(it)
            "rex.path" -> _rexPath(it)
            "rex.unary" -> _rexUnary(it)
            "rex.binary" -> _rexBinary(it)
            "rex.call" -> _rexCall(it)
            "rex.agg" -> _rexAgg(it)
            "rex.lit" -> _rexLit(it)
            "rex.collection" -> _rexCollection(it)
            "rex.struct" -> _rexStruct(it)
            "rex.query" -> _rexQuery(it)
            "rex.query.scalar" -> _rexQueryScalar(it)
            "rex.query.scalar.coerce" -> _rexQueryScalarCoerce(it)
            "rex.query.scalar.pivot" -> _rexQueryScalarPivot(it)
            "rex.query.collection" -> _rexQueryCollection(it)
            else -> err(id)
        }
    }

    private val _plan: Mapping<Plan> = Mapping {
        factory.plan(
            version = org.partiql.plan.ir.Plan.Version.valueOf(it["version"].asText().toUpperCase()),
            root = _rex(it["root"]),
        )
    }

    private val _common: Mapping<Common> = Mapping {
        factory.common(
            schema = it["schema"].fields().asSequence().associate { e ->
                e.key to
                    org.partiql.plan.ir.Rel.Join.Type.valueOf(e.value.asText().toUpperCase())
            },
            properties = it["properties"].map { n ->
                org.partiql.plan.ir.Property.valueOf(n.asText().toUpperCase())
            }.toSet(),
            metas = it["metas"].fields().asSequence().associate { e -> e.key to e.value.asText() },
        )
    }

    private val _binding: Mapping<Binding> = Mapping {
        factory.binding(
            name = _rex(it["name"]),
            rex = _rex(it["rex"]),
        )
    }

    private val _step: Mapping<Step> = Mapping {
        when (val id = it.id()) {
            "step.rex" -> _stepRex(it)
            "step.wildcard" -> _stepWildcard(it)
            "step.unpivot" -> _stepUnpivot(it)
            else -> err(id)
        }
    }

    private val _stepRex: Mapping<Step.Rex> = Mapping {
        factory.stepRex(
            index = _rex(it["index"]),
            case = org.partiql.plan.ir.Case.valueOf(it["case"].asText().toUpperCase()),
        )
    }

    private val _stepWildcard: Mapping<Step.Wildcard> = Mapping {
        factory.stepWildcard()
    }

    private val _stepUnpivot: Mapping<Step.Unpivot> = Mapping {
        factory.stepUnpivot()
    }

    private val _sortSpec: Mapping<SortSpec> = Mapping {
        factory.sortSpec(
            rex = _rex(it["rex"]),
            dir = org.partiql.plan.ir.SortSpec.Dir.valueOf(it["dir"].asText().toUpperCase()),
            nulls = org.partiql.plan.ir.SortSpec.Nulls.valueOf(it["nulls"].asText().toUpperCase()),
        )
    }

    private val _rel: Mapping<Rel> = Mapping {
        when (val id = it.id()) {
            "rel.scan" -> _relScan(it)
            "rel.unpivot" -> _relUnpivot(it)
            "rel.filter" -> _relFilter(it)
            "rel.sort" -> _relSort(it)
            "rel.bag" -> _relBag(it)
            "rel.fetch" -> _relFetch(it)
            "rel.project" -> _relProject(it)
            "rel.join" -> _relJoin(it)
            "rel.aggregate" -> _relAggregate(it)
            else -> err(id)
        }
    }

    private val _relScan: Mapping<Rel.Scan> = Mapping {
        factory.relScan(
            common = _common(it["common"]),
            rex = _rex(it["rex"]),
            alias = it["alias"].asText(),
            at = it["at"].asText(),
            by = it["by"].asText(),
        )
    }

    private val _relUnpivot: Mapping<Rel.Unpivot> = Mapping {
        factory.relUnpivot(
            common = _common(it["common"]),
            rex = _rex(it["rex"]),
            alias = it["alias"].asText(),
            at = it["at"].asText(),
            by = it["by"].asText(),
        )
    }

    private val _relFilter: Mapping<Rel.Filter> = Mapping {
        factory.relFilter(
            common = _common(it["common"]),
            input = _rel(it["input"]),
            condition = _rex(it["condition"]),
        )
    }

    private val _relSort: Mapping<Rel.Sort> = Mapping {
        factory.relSort(
            common = _common(it["common"]),
            input = _rel(it["input"]),
            specs = it["specs"].map { n -> _sortSpec(n) },
        )
    }

    private val _relBag: Mapping<Rel.Bag> = Mapping {
        factory.relBag(
            common = _common(it["common"]),
            lhs = _rel(it["lhs"]),
            rhs = _rel(it["rhs"]),
            op = org.partiql.plan.ir.Rel.Bag.Op.valueOf(it["op"].asText().toUpperCase()),
        )
    }

    private val _relFetch: Mapping<Rel.Fetch> = Mapping {
        factory.relFetch(
            common = _common(it["common"]),
            input = _rel(it["input"]),
            limit = _rex(it["limit"]),
            offset = _rex(it["offset"]),
        )
    }

    private val _relProject: Mapping<Rel.Project> = Mapping {
        factory.relProject(
            common = _common(it["common"]),
            input = _rel(it["input"]),
            bindings = it["bindings"].map { n -> _binding(n) },
        )
    }

    private val _relJoin: Mapping<Rel.Join> = Mapping {
        factory.relJoin(
            common = _common(it["common"]),
            lhs = _rel(it["lhs"]),
            rhs = _rel(it["rhs"]),
            condition = _rex(it["condition"]),
            type = org.partiql.plan.ir.Rel.Join.Type.valueOf(it["type"].asText().toUpperCase()),
        )
    }

    private val _relAggregate: Mapping<Rel.Aggregate> = Mapping {
        factory.relAggregate(
            common = _common(it["common"]),
            input = _rel(it["input"]),
            calls = it["calls"].map { n -> _binding(n) },
            groups = it["groups"].map { n -> _binding(n) },
            strategy =
            org.partiql.plan.ir.Rel.Aggregate.Strategy.valueOf(it["strategy"].asText().toUpperCase()),
        )
    }

    private val _rex: Mapping<Rex> = Mapping {
        when (val id = it.id()) {
            "rex.id" -> _rexId(it)
            "rex.path" -> _rexPath(it)
            "rex.unary" -> _rexUnary(it)
            "rex.binary" -> _rexBinary(it)
            "rex.call" -> _rexCall(it)
            "rex.agg" -> _rexAgg(it)
            "rex.lit" -> _rexLit(it)
            "rex.collection" -> _rexCollection(it)
            "rex.struct" -> _rexStruct(it)
            "rex.query" -> _rexQuery(it)
            else -> err(id)
        }
    }

    private val _rexId: Mapping<Rex.Id> = Mapping {
        factory.rexId(
            name = it["name"].asText(),
            case = org.partiql.plan.ir.Case.valueOf(it["case"].asText().toUpperCase()),
            qualifier = org.partiql.plan.ir.Rex.Id.Qualifier.valueOf(it["qualifier"].asText().toUpperCase()),
        )
    }

    private val _rexPath: Mapping<Rex.Path> = Mapping {
        factory.rexPath(
            root = _rex(it["root"]),
            steps = it["steps"].map { n -> _step(n) },
        )
    }

    private val _rexUnary: Mapping<Rex.Unary> = Mapping {
        factory.rexUnary(
            rex = _rex(it["rex"]),
            op = org.partiql.plan.ir.Rex.Unary.Op.valueOf(it["op"].asText().toUpperCase()),
        )
    }

    private val _rexBinary: Mapping<Rex.Binary> = Mapping {
        factory.rexBinary(
            lhs = _rex(it["lhs"]),
            rhs = _rex(it["rhs"]),
            op = org.partiql.plan.ir.Rex.Binary.Op.valueOf(it["op"].asText().toUpperCase()),
        )
    }

    private val _rexCall: Mapping<Rex.Call> = Mapping {
        factory.rexCall(
            id = it["id"].asText(),
            args = it["args"].map { n -> _rex(n) },
        )
    }

    private val _rexAgg: Mapping<Rex.Agg> = Mapping {
        factory.rexAgg(
            id = it["id"].asText(),
            args = it["args"].map { n -> _rex(n) },
            modifier = org.partiql.plan.ir.Rex.Agg.Modifier.valueOf(it["modifier"].asText().toUpperCase()),
        )
    }

    private val _rexLit: Mapping<Rex.Lit> = Mapping {
        factory.rexLit(
            value = loadSingleElement(it["value"].asText()),
        )
    }

    private val _rexCollection: Mapping<Rex.Collection> = Mapping {
        factory.rexCollection(
            type = org.partiql.plan.ir.Rex.Collection.Type.valueOf(it["type"].asText().toUpperCase()),
            values = it["values"].map { n -> _rex(n) },
        )
    }

    private val _rexStruct: Mapping<Rex.Struct> = Mapping {
        factory.rexStruct(
            fields = it["fields"].map { n -> _binding(n) },
        )
    }

    private val _rexQuery: Mapping<Rex.Query> = Mapping {
        when (val id = it.id()) {
            "rex.query.scalar" -> _rexQueryScalar(it)
            "rex.query.collection" -> _rexQueryCollection(it)
            else -> err(id)
        }
    }

    private val _rexQueryScalar: Mapping<Rex.Query.Scalar> = Mapping {
        when (val id = it.id()) {
            "rex.query.scalar.coerce" -> _rexQueryScalarCoerce(it)
            "rex.query.scalar.pivot" -> _rexQueryScalarPivot(it)
            else -> err(id)
        }
    }

    private val _rexQueryScalarCoerce: Mapping<Rex.Query.Scalar.Coerce> = Mapping {
        factory.rexQueryScalarCoerce(
            query = _rexQueryCollection(it["query"]),
        )
    }

    private val _rexQueryScalarPivot: Mapping<Rex.Query.Scalar.Pivot> = Mapping {
        factory.rexQueryScalarPivot(
            rel = _rel(it["rel"]),
            rex = _rex(it["rex"]),
            at = _rex(it["at"]),
        )
    }

    private val _rexQueryCollection: Mapping<Rex.Query.Collection> = Mapping {
        factory.rexQueryCollection(
            rel = _rel(it["rel"]),
            constructor = _rex(it["constructor"]),
        )
    }

    init {
        addDeserializer(PlanNode::class.java, map(_base))
        addDeserializer(Plan::class.java, map(_plan))
        addDeserializer(Common::class.java, map(_common))
        addDeserializer(Binding::class.java, map(_binding))
        addDeserializer(Step::class.java, map(_step))
        addDeserializer(Step.Rex::class.java, map(_stepRex))
        addDeserializer(Step.Wildcard::class.java, map(_stepWildcard))
        addDeserializer(Step.Unpivot::class.java, map(_stepUnpivot))
        addDeserializer(SortSpec::class.java, map(_sortSpec))
        addDeserializer(Rel::class.java, map(_rel))
        addDeserializer(Rel.Scan::class.java, map(_relScan))
        addDeserializer(Rel.Unpivot::class.java, map(_relUnpivot))
        addDeserializer(Rel.Filter::class.java, map(_relFilter))
        addDeserializer(Rel.Sort::class.java, map(_relSort))
        addDeserializer(Rel.Bag::class.java, map(_relBag))
        addDeserializer(Rel.Fetch::class.java, map(_relFetch))
        addDeserializer(Rel.Project::class.java, map(_relProject))
        addDeserializer(Rel.Join::class.java, map(_relJoin))
        addDeserializer(Rel.Aggregate::class.java, map(_relAggregate))
        addDeserializer(Rex::class.java, map(_rex))
        addDeserializer(Rex.Id::class.java, map(_rexId))
        addDeserializer(Rex.Path::class.java, map(_rexPath))
        addDeserializer(Rex.Unary::class.java, map(_rexUnary))
        addDeserializer(Rex.Binary::class.java, map(_rexBinary))
        addDeserializer(Rex.Call::class.java, map(_rexCall))
        addDeserializer(Rex.Agg::class.java, map(_rexAgg))
        addDeserializer(Rex.Lit::class.java, map(_rexLit))
        addDeserializer(Rex.Collection::class.java, map(_rexCollection))
        addDeserializer(Rex.Struct::class.java, map(_rexStruct))
        addDeserializer(Rex.Query::class.java, map(_rexQuery))
        addDeserializer(Rex.Query.Scalar::class.java, map(_rexQueryScalar))
        addDeserializer(Rex.Query.Scalar.Coerce::class.java, map(_rexQueryScalarCoerce))
        addDeserializer(Rex.Query.Scalar.Pivot::class.java, map(_rexQueryScalarPivot))
        addDeserializer(Rex.Query.Collection::class.java, map(_rexQueryCollection))
    }

    private fun JsonNode.id(): String = get("_id").asText()

    private inline fun err(id: String): Nothing =
        error("""no deserializer registered for _id `$id`""")

    private fun <T : PlanNode> map(mapping: Mapping<T>): JsonDeserializer<T> = object :
        JsonDeserializer<T>() {
        public override fun deserialize(p: JsonParser, ctxt: DeserializationContext): T =
            mapping(ctxt.readTree(p)!!)
    }

    private fun interface Mapping<out T : PlanNode> {
        public operator fun invoke(node: JsonNode): T
    }
}
