package org.partiql.planner.internal.typer

import com.amazon.ionelement.api.ionListOf
import com.amazon.ionelement.api.ionString
import org.partiql.planner.internal.ir.Constraint
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.PlanNode
import org.partiql.planner.internal.ir.Type
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

        fun normalize(collectionType: Type.Collection) =
            Normalizer.visitTypeCollection(collectionType, Unit).first

        // Unique normalizer
        // Lift primary key declaration at attribute level to a collection constraint.
        // We nuked the constraint name and table level constraint support
        private object Normalizer : PlanBaseVisitor<Pair<PlanNode, List<Constraint>>, Unit>() {
            override fun defaultReturn(node: PlanNode, ctx: Unit): Pair<PlanNode, List<Constraint>> = throw IllegalArgumentException("Unsupported feature during shape normalization")

            override fun visitType(node: Type, ctx: Unit): Pair<Type, List<Constraint>> =
                when (node) {
                    is Type.Atomic -> node to emptyList()
                    is Type.Collection -> visitTypeCollection(node, ctx)
                    is Type.Record -> visitTypeRecord(node, ctx)
                }

            override fun visitTypeCollection(node: Type.Collection, ctx: Unit): Pair<Type.Collection, List<Constraint>> {
                if (node.type == null) {
                    return typeCollection(null, node.isOrdered, node.constraints) to emptyList()
                } else {
                    val (elementType, constraints) = visitType(node.type, ctx)
                    return typeCollection(elementType, node.isOrdered, constraints) to emptyList()
                }
            }

            override fun visitTypeRecord(node: Type.Record, ctx: Unit): Pair<Type.Record, List<Constraint>> {
                val (collectionConstraints, structConstraints) = node.constraints.partition { it.isCollectionConstraint() }
                val carriedCollectionConstraint = mutableListOf<Constraint>()
                val fields = node.fields.map { f ->
                    val (field, carried) = visitTypeRecordField(f, ctx)
                    // arrange carried partition
                    carriedCollectionConstraint += carried.filter { it.isCollectionConstraint() }
                    field
                }

                return typeRecord(fields, structConstraints) to collectionConstraints + carriedCollectionConstraint
            }

            override fun visitTypeRecordField(node: Type.Record.Field, ctx: Unit): Pair<Type.Record.Field, List<Constraint>> {
                val (carried, attrConstrs) = node.constraints.partition { it.isCollectionConstraint() }
                val (type, carriedCollectionConstrs) = visitType(node.type, ctx)
                return typeRecordField(node.name, type, attrConstrs, node.isOptional, node.comment) to carriedCollectionConstrs + carried
            }

            private fun Constraint.isCollectionConstraint() =
                when (this) {
                    is Constraint.Unique -> true
                    is Constraint.Check -> false
                    is Constraint.NotNull -> false
                    is Constraint.Nullable -> false
                }
        }
    }

    internal object ConstraintResolver {
        fun resolveTable(type: Type.Collection): BagType {
            val type = Visitor.visitTypeCollection(type, Ctx(emptyList())).removeNull() as BagType
            return type.copy(type.elementType.removeNull(), type.metas, type.constraints)
        }

        fun resolveField(field: Type.Record.Field): StructType.Field {
            val type = Visitor.visitTypeRecordField(field, Ctx(emptyList()))
            return StructType.Field(field.name.symbol, type, field.comment?.let { mapOf("comment" to it) } ?: emptyMap())
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
                val pkConstrs = node.constraints.filterIsInstance<Constraint.Unique>()
                    .filter { it.isPrimaryKey }
                val pkConstr = when (pkConstrs.size) {
                    0 -> null
                    1 -> pkConstrs.first()
                    else -> throw IllegalArgumentException("Only one primary key constraint is allowed")
                }
                val pkAttr = pkConstr?.let {
                    it.attributes
                } ?: emptyList()
                // if associated with PK
                // the underlying type must be a non null struct
                val resolvedElementType = visitType(elementType, Ctx(pkAttr)).let {
                    if (pkAttr.isNotEmpty()) {
                        it.removeNull()
                    } else it
                }
                val collectionConstraint = node.constraints.mapNotNull { contr ->
                    if (contr is Constraint.Unique) {
                        val uniqueReference = contr.attributes.map { it.symbol }.toSet()
                        if (contr.isPrimaryKey) CollectionConstraint.PrimaryKey(uniqueReference)
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
                    if (constr is Constraint.Check) {
                        ionString(constr.sql)
                    } else null
                }.let { if (it.isNotEmpty()) { mapOf("check_constraints" to ionListOf(it)) } else emptyMap() }
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
                    (node.constraints.any { it is Constraint.NotNull }) || isPK

                val checkMeta = node.constraints.mapNotNull {
                    if (it is Constraint.Check) {
                        ionString(it.sql)
                    } else null
                }.let { if (it.isEmpty()) emptyMap() else mapOf("check_constraints" to ionListOf(it)) }

                val nonNullType = visitType(node.type, ctx).removeNull().let { it.withMetas(it.metas + checkMeta) }

                val type = if (notNullable) nonNullType else nonNullType.asNullable()

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
