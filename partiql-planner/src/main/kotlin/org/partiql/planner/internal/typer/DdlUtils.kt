package org.partiql.planner.internal.typer

import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import org.partiql.planner.internal.ir.Constraint
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.PlanNode
import org.partiql.planner.internal.ir.Type
import org.partiql.planner.internal.ir.constraint
import org.partiql.planner.internal.ir.typeCollection
import org.partiql.planner.internal.ir.typeRecord
import org.partiql.planner.internal.ir.typeRecordField
import org.partiql.planner.internal.ir.visitor.PlanBaseVisitor
import org.partiql.types.BagType
import org.partiql.types.CollectionConstraint
import org.partiql.types.DecimalType
import org.partiql.types.ListType
import org.partiql.types.NullType
import org.partiql.types.NumberConstraint
import org.partiql.types.StaticType
import org.partiql.types.StaticType.Companion.BOOL
import org.partiql.types.StaticType.Companion.DATE
import org.partiql.types.StaticType.Companion.DECIMAL
import org.partiql.types.StaticType.Companion.FLOAT
import org.partiql.types.StaticType.Companion.INT
import org.partiql.types.StaticType.Companion.INT2
import org.partiql.types.StaticType.Companion.INT4
import org.partiql.types.StaticType.Companion.INT8
import org.partiql.types.StaticType.Companion.STRING
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.TimeType
import org.partiql.types.TimestampType
import org.partiql.types.TupleConstraint
import org.partiql.value.PartiQLTimestampExperimental

internal object DdlUtils {

    // TODO: Should Lvalue in the plan just use a string?
    internal fun match(lvalue: Identifier.Symbol, rvalue: Identifier.Symbol) = when (lvalue.caseSensitivity) {
        Identifier.CaseSensitivity.SENSITIVE ->
            when (rvalue.caseSensitivity) {
                Identifier.CaseSensitivity.SENSITIVE -> lvalue.symbol == rvalue.symbol
                Identifier.CaseSensitivity.INSENSITIVE -> lvalue.symbol.equals(rvalue.symbol, ignoreCase = true)
            }
        Identifier.CaseSensitivity.INSENSITIVE -> TODO("Lvalue in this stage should all be case sensitive")
    }

    internal class ShapeNormalizer {

        fun normalize(collectionType: Type.Collection, prefix: String) =
            Normalizer.visitTypeCollection(collectionType, Ctx(0, prefix)).first

        internal data class Ctx(
            var count: Int,
            val prefix: String
        )

        // The normalizer will only lift constraint up, instead of push constraint down
        // this is cause the nature of a struct or collection constraint might be declared at attribute level,
        // but an attribute level constraint will never be declared at struct/collection level.
        private object Normalizer : PlanBaseVisitor<Pair<PlanNode, List<Constraint>>, Ctx>() {
            override fun defaultReturn(node: PlanNode, ctx: Ctx): Pair<PlanNode, List<Constraint>> = throw IllegalArgumentException("Unsupported feature during shape normalization")

            override fun visitType(node: Type, ctx: Ctx): Pair<Type, List<Constraint>> =
                when (node) {
                    is Type.Atomic -> node to emptyList()
                    is Type.Collection -> visitTypeCollection(node, ctx)
                    is Type.Record -> visitTypeRecord(node, ctx)
                }

            override fun visitTypeCollection(node: Type.Collection, ctx: Ctx): Pair<Type.Collection, List<Constraint>> {
                if (node.type == null) {
                    return typeCollection(null, node.isOrdered, node.constraints) to emptyList()
                } else {
                    val (elementType, constraints) = visitType(node.type, ctx)
                    val named = constraints.map { it.addNameIfNotExists(ctx) }
                    return typeCollection(elementType, node.isOrdered, named) to emptyList()
                }
            }

            override fun visitTypeRecord(node: Type.Record, ctx: Ctx): Pair<Type.Record, List<Constraint>> {
                val structConstraints = mutableListOf<Constraint>()
                val collectionConstraints = mutableListOf<Constraint>()

                // arrange partition on the struct
                node.constraints.partitionTo(structConstraints, collectionConstraints) {
                    it.definition.isStructConstraint()
                }

                val fields = node.fields.map { f ->
                    val (field, carried) = visitTypeRecordField(f, ctx)
                    // arrange carried partition
                    carried.partitionTo(structConstraints, collectionConstraints) {
                        it.definition.isStructConstraint()
                    }
                    field
                }

                val named = structConstraints.map { it.addNameIfNotExists(ctx) }

                return typeRecord(fields, named) to collectionConstraints
            }

            override fun visitTypeRecordField(node: Type.Record.Field, ctx: Ctx): Pair<Type.Record.Field, List<Constraint>> {
                val (carried, attrConstrs) = node.constraints.partition { it.definition.isStructConstraint() || it.definition.isCollectionConstraint() }
                val (type, carriedCollectionConstrs) = visitType(node.type, ctx)
                val named = attrConstrs.map { it.addNameIfNotExists(ctx) }
                return typeRecordField(node.name, type, named, node.isOptional, node.comment) to carriedCollectionConstrs + carried
            }

            private fun Constraint.addNameIfNotExists(ctx: Ctx): Constraint {
                val named =
                    if (this.name == null) constraint("\$_${ctx.prefix}_${ctx.count}", this.definition)
                    else this
                ctx.count += 1
                return named
            }

            private fun <T> List<T>.partitionTo(container1: MutableList<T>, container2: MutableList<T>, predicate: (T) -> Boolean) {
                val (p1, p2) = this.partition(predicate)
                container1.addAll(p1)
                container2.addAll(p2)
            }

            private fun Constraint.Definition.isStructConstraint() =
                when (this) {
                    is Constraint.Definition.Unique -> false
                    is Constraint.Definition.Check -> true
                    is Constraint.Definition.NotNull -> false
                    is Constraint.Definition.Nullable -> false
                }

            private fun Constraint.Definition.isCollectionConstraint() =
                when (this) {
                    is Constraint.Definition.Unique -> true
                    is Constraint.Definition.Check -> false
                    is Constraint.Definition.NotNull -> false
                    is Constraint.Definition.Nullable -> false
                }
        }
    }

    internal object ConstraintResolver {
        fun resolveTable(type: Type.Collection): BagType {
            val type = Visitor.visitTypeCollection(type, Ctx(emptyList())).removeNull() as BagType
            return type.copy(type.elementType.removeNull(), type.metas, type.constraints)
        }

        private fun StaticType.removeNull() =
            this.allTypes.filterNot { it is NullType }.toSet().let { StaticType.unionOf(it).flatten() }

        data class Ctx(
            val primaryKey: List<Identifier.Symbol>
        )

        object Visitor : PlanBaseVisitor<StaticType, Ctx>() {
            override fun defaultReturn(node: PlanNode, ctx: Ctx): StaticType = throw IllegalArgumentException("Unsupported Feature during constraint resolution")

            override fun visitTypeAtomic(node: Type.Atomic, ctx: Ctx): StaticType =
                node.toStaticType()

            override fun visitTypeCollection(node: Type.Collection, ctx: Ctx): StaticType {
                val elementType = node.type ?: return if (node.isOrdered) StaticType.LIST.asNullable() else StaticType.BAG.asNullable()
                // only one pk constraint
                val pkConstrs = node.constraints.filter {
                    val def = it.definition
                    if (def is Constraint.Definition.Unique) {
                        def.isPrimaryKey
                    } else false
                }
                val pkConstr = when (pkConstrs.size) {
                    0 -> null
                    1 -> pkConstrs.first()
                    else -> throw IllegalArgumentException("Only one primary key constraint is allowed")
                }
                val pkAttr = pkConstr?.let { (it.definition as Constraint.Definition.Unique).attributes } ?: emptyList()
                // if associated with PK
                // the underlying type must be a non null struct
                val resolvedElementType = visitType(elementType, Ctx(pkAttr)).let {
                    if (pkAttr.isNotEmpty()) {
                        it.removeNull()
                    } else it
                }
                val collectionConstraint = node.constraints.mapNotNull { contr ->
                    val def = contr.definition
                    if (def is Constraint.Definition.Unique) {
                        val uniqueReference = def.attributes.map { it.symbol }.toSet()
                        if (def.isPrimaryKey) CollectionConstraint.PrimaryKey(uniqueReference)
                        else CollectionConstraint.UniqueKey(uniqueReference)
                    } else null
                }.toSet()
                return if (node.isOrdered) {
                    ListType(resolvedElementType, mapOf(), collectionConstraint).asNullable()
                } else {
                    BagType(resolvedElementType, mapOf(), collectionConstraint).asNullable()
                }
            }

            override fun visitTypeRecord(node: Type.Record, ctx: Ctx): StaticType {
                // TODO: For now struct level constraint are only check
                //  and struct by default is closed and unique
                //  For now we dump check constraint in struct meta
                val constraintMeta = node.constraints.mapNotNull { constr ->
                    if (constr.definition is Constraint.Definition.Check) {
                        field(constr.name!!, ionString(constr.definition.sql))
                    } else null
                }.let { if (it.isNotEmpty()) { mapOf("check_constraints" to ionStructOf(it)) } else emptyMap() }
                val seen = mutableSetOf<String>()
                val resolvedField = node.fields.map {
                    StructType.Field(
                        it.name.symbol,
                        visitTypeRecordField(it, ctx),
                        it.comment?.let { mapOf("comment" to it) } ?: emptyMap()
                    ).also { field ->
                        if (!seen.add(field.key)) throw IllegalArgumentException("Duplicated binding name ${field.key}")
                    }
                }

                return StructType(
                    resolvedField,
                    true,
                    listOf(),
                    setOf(
                        TupleConstraint.Open(false),
                        TupleConstraint.UniqueAttrs(true)
                    ),
                    constraintMeta
                ).asNullable()
            }

            override fun visitTypeRecordField(node: Type.Record.Field, ctx: Ctx): StaticType {
                val isPK = ctx.primaryKey.any { it.isEquivalentTo(node.name) }

                if (node.isOptional && isPK) throw IllegalArgumentException("Primary key attribute cannot be optional")

                val notNullable =
                    (node.constraints.any { it.definition is Constraint.Definition.NotNull }) || isPK
                val type = visitType(node.type, ctx).let { if (notNullable) it.removeNull() else it }

                return if (node.isOptional) type.asOptional() else type
            }

            @OptIn(PartiQLTimestampExperimental::class)
            private fun Type.Atomic.toStaticType() = when (this) {
                is Type.Atomic.Bool -> BOOL
                is Type.Atomic.Int2 -> INT2
                is Type.Atomic.Int4 -> INT4
                is Type.Atomic.Int8 -> INT8
                is Type.Atomic.Int -> INT
                is Type.Atomic.Decimal ->
                    if (this.precision != null)
                        DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(this.precision, this.scale!!))
                    else DECIMAL
                is Type.Atomic.Float64 -> FLOAT

                is Type.Atomic.Char -> StringType(StringType.StringLengthConstraint.Constrained(NumberConstraint.Equals(this.length ?: 1)))
                is Type.Atomic.Varchar ->
                    this.length?.let {
                        StringType(StringType.StringLengthConstraint.Constrained(NumberConstraint.UpTo(it)))
                    } ?: STRING

                is Type.Atomic.Date -> DATE
                is Type.Atomic.Time -> TimeType(precision, false)
                is Type.Atomic.TimeWithTz -> TimeType(precision, true)
                is Type.Atomic.Timestamp -> TimestampType(precision ?: 6, false)
                is Type.Atomic.TimestampWithTz -> TimestampType(precision ?: 6, true)
            }.asNullable()

            private fun Identifier.Symbol.isEquivalentTo(other: Identifier.Symbol): Boolean = when (caseSensitivity) {
                Identifier.CaseSensitivity.SENSITIVE -> when (other.caseSensitivity) {
                    Identifier.CaseSensitivity.SENSITIVE -> symbol.equals(other.symbol)
                    Identifier.CaseSensitivity.INSENSITIVE -> symbol.equals(other.symbol, ignoreCase = true)
                }
                Identifier.CaseSensitivity.INSENSITIVE -> symbol.equals(other.symbol, ignoreCase = true)
            }
        }
    }
}
