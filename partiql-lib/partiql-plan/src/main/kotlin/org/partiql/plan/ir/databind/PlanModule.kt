package org.partiql.plan.ir.databind

import com.amazon.ionelement.api.ionString
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.module.SimpleModule
import org.partiql.plan.ir.Binding
import org.partiql.plan.ir.Common
import org.partiql.plan.ir.PlanNode
import org.partiql.plan.ir.Rel
import org.partiql.plan.ir.Rex
import org.partiql.plan.ir.SortSpec
import org.partiql.plan.ir.StructPart
import org.partiql.plan.ir.builder.PlanFactory
import org.partiql.plan.ir.databind.PlanModule.Mapping

public class PlanModule(
    private val factory: PlanFactory = PlanFactory.DEFAULT
) : SimpleModule() {
    private val _base: Mapping<PlanNode> = Mapping {
        when (val id = it.id()) {
            "common" -> _common(it)
            "rel" -> _rel(it)
            "rel.scan" -> _relScan(it)
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
            "rex.subquery" -> _rexSubquery(it)
            "rex.subquery.tuple" -> _rexSubqueryTuple(it)
            "rex.subquery.scalar" -> _rexSubqueryScalar(it)
            "rex.subquery.collection" -> _rexSubqueryCollection(it)
            "struct_part" -> _structPart(it)
            "struct_part.fields" -> _structPartFields(it)
            "struct_part.field" -> _structPartField(it)
            "sort_spec" -> _sortSpec(it)
            "binding" -> _binding(it)
            else -> err(id)
        }
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

    private val _rel: Mapping<Rel> = Mapping {
        when (val id = it.id()) {
            "rel.scan" -> _relScan(it)
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
            "rex.subquery" -> _rexSubquery(it)
            else -> err(id)
        }
    }

    private val _rexId: Mapping<Rex.Id> = Mapping {
        factory.rexId(
            name = it["name"].asText(),
        )
    }

    private val _rexPath: Mapping<Rex.Path> = Mapping {
        factory.rexPath(
            root = _rex(it["root"]),
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
        // TODO ION HACK
        factory.rexLit(
            value = ionString(it["value"].asText()),
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
            fields = it["fields"].map { n -> _structPart(n) },
        )
    }

    private val _rexSubquery: Mapping<Rex.Subquery> = Mapping {
        when (val id = it.id()) {
            "rex.subquery.tuple" -> _rexSubqueryTuple(it)
            "rex.subquery.scalar" -> _rexSubqueryScalar(it)
            "rex.subquery.collection" -> _rexSubqueryCollection(it)
            else -> err(id)
        }
    }

    private val _rexSubqueryTuple: Mapping<Rex.Subquery.Tuple> = Mapping {
        factory.rexSubqueryTuple(
            rel = _rel(it["rel"]),
        )
    }

    private val _rexSubqueryScalar: Mapping<Rex.Subquery.Scalar> = Mapping {
        factory.rexSubqueryScalar(
            rel = _rel(it["rel"]),
        )
    }

    private val _rexSubqueryCollection: Mapping<Rex.Subquery.Collection> = Mapping {
        factory.rexSubqueryCollection(
            rel = _rel(it["rel"]),
        )
    }

    private val _structPart: Mapping<StructPart> = Mapping {
        when (val id = it.id()) {
            "struct_part.fields" -> _structPartFields(it)
            "struct_part.field" -> _structPartField(it)
            else -> err(id)
        }
    }

    private val _structPartFields: Mapping<StructPart.Fields> = Mapping {
        factory.structPartFields(
            rex = _rex(it["rex"]),
        )
    }

    private val _structPartField: Mapping<StructPart.Field> = Mapping {
        factory.structPartField(
            name = _rex(it["name"]),
            rex = _rex(it["rex"]),
        )
    }

    private val _sortSpec: Mapping<SortSpec> = Mapping {
        factory.sortSpec(
            rex = _rex(it["rex"]),
            dir = org.partiql.plan.ir.SortSpec.Dir.valueOf(it["dir"].asText().toUpperCase()),
            nulls = org.partiql.plan.ir.SortSpec.Nulls.valueOf(it["nulls"].asText().toUpperCase()),
        )
    }

    private val _binding: Mapping<Binding> = Mapping {
        factory.binding(
            name = it["name"].asText(),
            rex = _rex(it["rex"]),
        )
    }

    init {
        addDeserializer(PlanNode::class.java, map(_base))
        addDeserializer(Common::class.java, map(_common))
        addDeserializer(Rel::class.java, map(_rel))
        addDeserializer(Rel.Scan::class.java, map(_relScan))
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
        addDeserializer(Rex.Subquery::class.java, map(_rexSubquery))
        addDeserializer(Rex.Subquery.Tuple::class.java, map(_rexSubqueryTuple))
        addDeserializer(Rex.Subquery.Scalar::class.java, map(_rexSubqueryScalar))
        addDeserializer(Rex.Subquery.Collection::class.java, map(_rexSubqueryCollection))
        addDeserializer(StructPart::class.java, map(_structPart))
        addDeserializer(StructPart.Fields::class.java, map(_structPartFields))
        addDeserializer(StructPart.Field::class.java, map(_structPartField))
        addDeserializer(SortSpec::class.java, map(_sortSpec))
        addDeserializer(Binding::class.java, map(_binding))
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
