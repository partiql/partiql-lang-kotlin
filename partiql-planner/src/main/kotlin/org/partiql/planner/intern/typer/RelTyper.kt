package org.partiql.planner.intern.typer

import org.partiql.planner.intern.SqlTypes
import org.partiql.planner.intern.validate.SqlScope
import org.partiql.planner.intern.validate.SqlTyper
import org.partiql.planner.intern.validate.SqlValidator
import org.partiql.planner.intern.validate.Strategy
import org.partiql.planner.internal.ProblemGenerator
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.util.PlanRewriter
import org.partiql.planner.internal.typer.isNumeric
import org.partiql.types.StaticType
import org.partiql.types.StaticType.Companion.MISSING
import org.partiql.types.StructType
import kotlin.math.max

/**
 * Types the relational operators of a query expression.
 *
 * @property validator  Parent validator which has the system and session.
 * @property types      SQL type factory.
 * @property scope      Parent scope bindings.
 */
internal class RelTyper(
    private val validator: SqlValidator,
    private val types: SqlTypes<StaticType>,
    private val scope: SqlScope,
) : PlanRewriter<Rel.Type>() {

    private fun validate(rex: Rex, bindings: SqlScope, strategy: Strategy = Strategy.LOCAL): Rex {
        return validator(bindings, strategy).validate(rex)
    }

    private fun validator(bindings: SqlScope, strategy: Strategy = Strategy.LOCAL): RexTyper {
        return RexTyper(validator, types, bindings, strategy)
    }

    /**
     * TODO remove Rel.Type once we use SFWD modeling for IR.
     */
    override fun visitRel(node: Rel, ctx: Rel.Type) = visitRelOp(node.op, node.type) as Rel

    /**
     * signature: scan(value) -> (T)
     */
    override fun visitRelOpScan(node: Rel.Op.Scan, ctx: Rel.Type): Rel {
        // descend, with GLOBAL resolution strategy
        val value = validate(node.rex, scope, Strategy.GLOBAL)
        // compute rel type
        val valueT = SqlTyper.getScanType(value.type)
        val type = ctx.copyWithSchema(valueT)
        // rewrite
        return Rel(type, Rel.Op.Scan(value))
    }

    /**
     * signature: scan_indexed(value) -> (T, bigint)
     */
    override fun visitRelOpScanIndexed(node: Rel.Op.ScanIndexed, ctx: Rel.Type): Rel {
        // descend, with GLOBAL resolution strategy
        val value = validate(node.rex, scope, Strategy.GLOBAL)
        // compute rel type
        val valueT = SqlTyper.getScanType(value.type)
        val indexT = types.bigint()
        val type = ctx.copyWithSchema(valueT, indexT)
        // rewrite
        return Rel(type, Rel.Op.ScanIndexed(value))
    }

    /**
     * TODO handle NULL|STRUCT type?
     * TODO change order.
     *
     * signature: unpivot(value) -> (varchar, T)
     */
    override fun visitRelOpUnpivot(node: Rel.Op.Unpivot, ctx: Rel.Type): Rel {
        // descend, with GLOBAL resolution strategy
        val value = validate(node.rex, scope, Strategy.GLOBAL)
        // compute rel type
        val keyT = types.varchar()
        val valueT = SqlTyper.getUnpivotType(value.type)
        // rewrite
        val type = ctx.copyWithSchema(keyT, valueT)
        val op = Rel.Op.Unpivot(value)
        return Rel(type, op)
    }

    /**
     * signature: distinct(rel) -> rel
     */
    override fun visitRelOpDistinct(node: Rel.Op.Distinct, ctx: Rel.Type): Rel {
        // validate input
        val input = visitRel(node.input, ctx)
        // rewrite
        return Rel(input.type, Rel.Op.Distinct(input))
    }

    /**
     * signature: distinct(rel) -> rel
     */
    override fun visitRelOpFilter(node: Rel.Op.Filter, ctx: Rel.Type): Rel {
        // validate input
        val input = visitRel(node.input, ctx)
        // validate children
        val bindings = scope.concat(input.type)
        val predicate = validate(node.predicate, bindings)
        // rewrite
        return Rel(input.type, Rel.Op.Filter(input, predicate))
    }

    override fun visitRelOpSort(node: Rel.Op.Sort, ctx: Rel.Type): Rel {
        // validate input
        val input = visitRel(node.input, ctx)
        // validate children
        val bindings = scope.concat(input.type)
        val validator = validator(bindings)
        val specs = node.specs.map {
            val rex = validator.validate(it.rex)
            it.copy(rex = rex)
        }
        // output schema of a sort is the same as the input
        val type = input.type.copy(props = setOf(Rel.Prop.ORDERED))
        // rewrite
        return Rel(type, Rel.Op.Sort(input, specs))
    }

    override fun visitRelOpSetExcept(node: Rel.Op.Set.Except, ctx: Rel.Type): Rel {
        val lhs = visitRel(node.lhs, node.lhs.type)
        val rhs = visitRel(node.rhs, node.rhs.type)
        // Check for Compatibility
        if (!setOpSchemaSizesMatch(lhs, rhs)) {
            return createRelErrForSetOpMismatchSizes()
        }
        if (!node.isOuter && !setOpSchemaTypesMatch(lhs, rhs)) {
            return createRelErrForSetOpMismatchTypes()
        }
        // Compute Schema
        val type = Rel.Type(lhs.type.schema, props = emptySet())
        return Rel(type, node.copy(lhs = lhs, rhs = rhs))
    }

    override fun visitRelOpSetIntersect(node: Rel.Op.Set.Intersect, ctx: Rel.Type): Rel {
        val lhs = visitRel(node.lhs, node.lhs.type)
        val rhs = visitRel(node.rhs, node.rhs.type)
        // Check for Compatibility
        if (!setOpSchemaSizesMatch(lhs, rhs)) {
            return createRelErrForSetOpMismatchSizes()
        }
        if (!node.isOuter && !setOpSchemaTypesMatch(lhs, rhs)) {
            return createRelErrForSetOpMismatchTypes()
        }
        // Compute Schema
        val type = Rel.Type(lhs.type.schema, props = emptySet())
        return Rel(type, node.copy(lhs = lhs, rhs = rhs))
    }

    override fun visitRelOpSetUnion(node: Rel.Op.Set.Union, ctx: Rel.Type): Rel {
        val lhs = visitRel(node.lhs, node.lhs.type)
        val rhs = visitRel(node.rhs, node.rhs.type)
        // Check for Compatibility
        if (!setOpSchemaSizesMatch(lhs, rhs)) {
            return createRelErrForSetOpMismatchSizes()
        }
        if (!node.isOuter && !setOpSchemaTypesMatch(lhs, rhs)) {
            return createRelErrForSetOpMismatchTypes()
        }
        // Compute Schema
        val size = max(lhs.type.schema.size, rhs.type.schema.size)
        val schema = List(size) {
            val lhsBinding = lhs.type.schema.getOrNull(it) ?: Rel.Binding("_$it", MISSING)
            val rhsBinding = rhs.type.schema.getOrNull(it) ?: Rel.Binding("_$it", MISSING)
            val bindingName = when (lhsBinding.name == rhsBinding.name) {
                true -> lhsBinding.name
                false -> "_$it"
            }
            Rel.Binding(bindingName, types.dynamic(lhsBinding.type, rhsBinding.type))
        }
        val type = Rel.Type(schema, props = emptySet())
        return Rel(type, node.copy(lhs = lhs, rhs = rhs))
    }

    /**
     * @return whether each type of the [lhs] is equal to its counterpart on the [rhs]
     * @param lhs should be typed already
     * @param rhs should be typed already
     */
    private fun setOpSchemaTypesMatch(lhs: Rel, rhs: Rel): Boolean {
        // TODO: [RFC-0007](https://github.com/partiql/partiql-lang/blob/main/RFCs/0007-rfc-bag-operators.md)
        //  states that the types must be "comparable". The below code ONLY makes sure that types need to be
        //  the same. In the future, we need to add support for checking comparable types.
        for (i in 0..lhs.type.schema.lastIndex) {
            val lhsBindingType = lhs.type.schema[i].type
            val rhsBindingType = rhs.type.schema[i].type
            if (lhsBindingType != rhsBindingType) {
                return false
            }
        }
        return true
    }

    /**
     * @return whether the [lhs] and [rhs] schemas are of equal size
     * @param lhs should be typed already
     * @param rhs should be typed already
     */
    private fun setOpSchemaSizesMatch(lhs: Rel, rhs: Rel): Boolean {
        return lhs.type.schema.size == rhs.type.schema.size
    }

    private fun createRelErrForSetOpMismatchSizes(): Rel {
        return Rel(Rel.Type(emptyList(), emptySet()), Rel.Op.Err("LHS and RHS of SET OP do not have the same number of bindings."))
    }

    private fun createRelErrForSetOpMismatchTypes(): Rel {
        return Rel(Rel.Type(emptyList(), emptySet()), Rel.Op.Err("LHS and RHS of SET OP do not have the same type."))
    }

    override fun visitRelOpLimit(node: Rel.Op.Limit, ctx: Rel.Type): Rel {
        // validate input
        val input = visitRel(node.input, ctx)
        // validate children
        val limit = validate(node.limit, scope)
        // TODO error reporting?
        if (limit.type.isNumeric().not()) {
            val err = ProblemGenerator.missingRex(
                causes = listOf(limit.op),
                problem = ProblemGenerator.unexpectedType(limit.type, setOf(StaticType.INT))
            )
            return Rel(input.type, Rel.Op.Limit(input, err))
        }
        // rewrite
        return Rel(input.type, Rel.Op.Limit(input, limit))
    }

    override fun visitRelOpOffset(node: Rel.Op.Offset, ctx: Rel.Type): Rel {
        // validate input
        val input = visitRel(node.input, ctx)
        // validate children
        val offset = validate(node.offset, scope)
        if (offset.type.isNumeric().not()) {
            val err = ProblemGenerator.missingRex(
                causes = listOf(offset.op),
                problem = ProblemGenerator.unexpectedType(offset.type, setOf(StaticType.INT))
            )
            return Rel(input.type, Rel.Op.Limit(input, err))
        }
        // rewrite
        return Rel(input.type, Rel.Op.Limit(input, offset))
    }

    override fun visitRelOpProject(node: Rel.Op.Project, ctx: Rel.Type): Rel {
        // validate input
        val input = visitRel(node.input, ctx)
        // validate children
        val bindings = scope.concat(node.input.type)
        val validator = validator(bindings)
        val projections = node.projections.map { validator.validate(it) }
        // compute output schema
        val type = ctx.copyWithSchema(projections.map { it.type })
        // rewrite
        return Rel(type, Rel.Op.Project(input, projections))
    }

    /**
     * TODO need to model JOIN and LATERAL JOIN differently in the IR.
     */
    override fun visitRelOpJoin(node: Rel.Op.Join, ctx: Rel.Type): Rel {
        // Rewrite LHS and RHS
        val lhs = visitRel(node.lhs, ctx)
        val rhs = RelTyper(validator, types, scope.concat(lhs.type)).visitRel(node.rhs, node.rhs.type)
        // Calculate output schema given the JOIN type
        val l = lhs.type.schema
        val r = rhs.type.schema
        val schema = when (node.type) {
            Rel.Op.Join.Type.INNER -> l + r
            Rel.Op.Join.Type.LEFT -> l + r.pad()
            Rel.Op.Join.Type.RIGHT -> l.pad() + r
            Rel.Op.Join.Type.FULL -> l.pad() + r.pad()
        }
        // validate the condition on the output schema
        val type = Rel.Type(schema, ctx.props)
        val bindings = scope.concat(type)
        val condition = validate(node.rex, bindings)
        // rewrite
        return Rel(type, Rel.Op.Join(lhs, rhs, condition, node.type))
    }

    override fun visitRelOpExclude(node: Rel.Op.Exclude, ctx: Rel.Type): Rel {
        TODO()
    }

    override fun visitRelOpAggregate(node: Rel.Op.Aggregate, ctx: Rel.Type): Rel {
        TODO()
    }

    override fun visitRelOpErr(node: Rel.Op.Err, ctx: Rel.Type): Rel = Rel(ctx, node)

    // -- Helpers

    private fun Rel.Type.copyWithSchema(vararg types: StaticType): Rel.Type = copyWithSchema(types.toList())

    private fun Rel.Type.copyWithSchema(types: List<StaticType>): Rel.Type {
        assert(types.size == schema.size) { "Illegal copy, types size does not matching bindings list size" }
        return this.copy(schema = schema.mapIndexed { i, binding -> binding.copy(type = types[i]) })
    }

    /**
     * This will make all binding values nullables. If the value is a struct, each field will be nullable.
     *
     * Note, this does not handle union types or nullable struct types.
     */
    private fun List<Rel.Binding>.pad() = map {
        val type = when (val t = it.type) {
            is StructType -> t.withNullableFields()
            else -> t.asNullable()
        }
        Rel.Binding(it.name, type)
    }

    private fun StructType.withNullableFields(): StructType {
        return copy(fields.map { it.copy(value = it.value.asNullable()) })
    }
}
