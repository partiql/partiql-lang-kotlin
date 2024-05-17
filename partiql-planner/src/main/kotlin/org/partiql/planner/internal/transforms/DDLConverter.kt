package org.partiql.planner.internal.transforms

import org.partiql.ast.AstNode
import org.partiql.ast.Constraint
import org.partiql.ast.DdlOp
import org.partiql.ast.PartitionBy
import org.partiql.ast.Statement
import org.partiql.ast.TableDefinition
import org.partiql.ast.TableProperty
import org.partiql.ast.Type
import org.partiql.ast.sql.sql
import org.partiql.ast.visitor.AstBaseVisitor
import org.partiql.planner.internal.Env
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.PlanNode
import org.partiql.planner.internal.ir.Statement.DDL
import org.partiql.planner.internal.ir.constraint
import org.partiql.planner.internal.ir.constraintDefinitionCheck
import org.partiql.planner.internal.ir.constraintDefinitionNotNull
import org.partiql.planner.internal.ir.constraintDefinitionNullable
import org.partiql.planner.internal.ir.constraintDefinitionUnique
import org.partiql.planner.internal.ir.ddlOpCreateTable
import org.partiql.planner.internal.ir.partitionByAttrList
import org.partiql.planner.internal.ir.statementDDL
import org.partiql.planner.internal.ir.tableProperty
import org.partiql.planner.internal.ir.typeAtomicBool
import org.partiql.planner.internal.ir.typeAtomicChar
import org.partiql.planner.internal.ir.typeAtomicDate
import org.partiql.planner.internal.ir.typeAtomicDecimal
import org.partiql.planner.internal.ir.typeAtomicFloat64
import org.partiql.planner.internal.ir.typeAtomicInt
import org.partiql.planner.internal.ir.typeAtomicInt2
import org.partiql.planner.internal.ir.typeAtomicInt4
import org.partiql.planner.internal.ir.typeAtomicInt8
import org.partiql.planner.internal.ir.typeAtomicTime
import org.partiql.planner.internal.ir.typeAtomicTimeWithTz
import org.partiql.planner.internal.ir.typeAtomicTimestamp
import org.partiql.planner.internal.ir.typeAtomicTimestampWithTz
import org.partiql.planner.internal.ir.typeAtomicVarchar
import org.partiql.planner.internal.ir.typeCollection
import org.partiql.planner.internal.ir.typeRecord
import org.partiql.planner.internal.ir.typeRecordField
import org.partiql.planner.internal.transforms.AstToPlan.convert
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental

internal object DDLConverter {
    internal fun apply(statement: Statement.DDL, env: Env): DDL = ToDdl.visitStatementDDL(statement, Ctx(env, null))

    private data class Ctx(
        val env: Env,
        val currentFieldName: Identifier.Symbol?
    )

    private object ToDdl : AstBaseVisitor<PlanNode, Ctx>() {
        override fun defaultReturn(node: AstNode, ctx: Ctx) =
            throw IllegalArgumentException("Unsupported operation $node")

        override fun visitStatementDDL(node: Statement.DDL, ctx: Ctx) =
            when (val op = node.op) {
                is DdlOp.CreateTable -> {
                    statementDDL(StaticType.ANY, visitDdlOpCreateTable(op, ctx))
                }
                else -> throw IllegalArgumentException("Unsupported DDL operation $op")
            }

        override fun visitDdlOpCreateTable(node: DdlOp.CreateTable, ctx: Ctx): org.partiql.planner.internal.ir.DdlOp.CreateTable {
            val name = convert(node.name)
            val def = node.definition ?: throw IllegalArgumentException("CREATE TABLE with no table definition is not supported")
            val constraints = def.constraints.map { visitConstraint(it, ctx) }
            // Constraints are stored at the struct level at start, we will arrange constraints later
            val record = typeRecord(def.attributes.map { visitTableDefinitionAttribute(it, ctx) }, constraints)

            val partitionBy = node.partitionBy?.let { visitPartitionBy(it, ctx) }
            val tblProperties = node.tableProperties.map { visitTableProperty(it, ctx) }
            return ddlOpCreateTable(name, typeCollection(record, false, emptyList()), partitionBy, tblProperties)
        }

        override fun visitTableDefinitionAttribute(node: TableDefinition.Attribute, ctx: Ctx): org.partiql.planner.internal.ir.Type.Record.Field {
            val attrName = convert(node.name)
            val type = visitType(node.type, ctx)
            val constraints = node.constraints.map { visitConstraint(it, ctx) }
            return typeRecordField(attrName, type, constraints, node.isOptional, node.comment)
        }

        override fun visitType(node: Type, ctx: Ctx): org.partiql.planner.internal.ir.Type =
            when (node) {
                // Any Type
                is Type.Any -> TODO("Type Any not supported in DDL Yet")

                // Null / Missing Type
                is Type.Missing -> TODO("Type MISSING not supported in DDL Yet")
                is Type.NullType -> TODO("Type NULL not supported in DDL Yet")

                // Complex
                is Type.Array -> typeCollection(node.type?.let { visitType(it, ctx) }, true, emptyList())
                is Type.List -> typeCollection(null, true, emptyList())
                is Type.Sexp -> typeCollection(null, true, emptyList())
                is Type.Bag -> typeCollection(null, false, emptyList())

                // Struct
                is Type.Struct -> typeRecord(
                    node.fields.map { visitTypeStructField(it, ctx) },
                    emptyList()
                )
                is Type.Tuple -> typeRecord(emptyList(), emptyList())

                // Boolean Type
                is Type.Bool -> typeAtomicBool()

                // Numeric Type
                is Type.Tinyint -> TODO("Type TINYINT not supported in DDL Yet")
                is Type.Int2, is Type.Smallint -> typeAtomicInt2()
                is Type.Int4 -> typeAtomicInt4()
                is Type.Int8, is Type.Bigint -> typeAtomicInt8()
                is Type.Int -> typeAtomicInt()
                is Type.Decimal -> typeAtomicDecimal(node.precision, node.scale)
                is Type.Numeric -> typeAtomicDecimal(node.precision, node.scale)
                is Type.Float32 -> TODO("Type Float32 not supported in DDL Yet")
                is Type.Float64, is Type.Real -> typeAtomicFloat64()

                // Text
                is Type.Symbol -> TODO("Type SYMBOL not supported in DDL Yet")
                is Type.Char -> typeAtomicChar(node.length)
                is Type.String -> typeAtomicVarchar(node.length)
                is Type.Varchar -> typeAtomicVarchar(node.length)

                // Bit
                is Type.Bit -> TODO("Type BIT not supported in DDL Yet")
                is Type.BitVarying -> TODO("Type BITVARYING not supported in DDL Yet")
                is Type.ByteString -> TODO("Type BYTESTRING not supported in DDL Yet")

                // Date Time
                is Type.Date -> typeAtomicDate()
                is Type.Time -> typeAtomicTime(node.precision)
                is Type.TimeWithTz -> typeAtomicTimeWithTz(node.precision)
                is Type.Timestamp -> typeAtomicTimestamp(node.precision)
                is Type.TimestampWithTz -> typeAtomicTimestampWithTz(node.precision)
                is Type.Interval -> TODO("Type INTERVAL not supported in DDL Yet")

                // Lob
                is Type.Blob -> TODO("Type BLOB not supported in DDL Yet")
                is Type.Clob -> TODO("Type CLOB not supported in DDL Yet")

                // Custom
                is Type.Custom -> TODO("Custom Type not supported in DDL Yet")
            }

        override fun visitTypeStructField(node: Type.Struct.Field, ctx: Ctx) =
            typeRecordField(
                convert(node.name),
                visitType(node.type, ctx),
                node.constraints.map { visitConstraint(it, ctx) },
                node.isOptional,
                node.comment,
            )

        override fun visitConstraint(node: Constraint, ctx: Ctx) = when (val def = node.definition) {
            is Constraint.Definition.Check -> constraint(node.name, visitConstraintDefinitionCheck(def, ctx))
            is Constraint.Definition.NotNull -> constraint(node.name, constraintDefinitionNotNull())
            is Constraint.Definition.Nullable -> constraint(node.name, constraintDefinitionNullable())
            is Constraint.Definition.Unique -> constraint(node.name, visitConstraintDefinitionUnique(def, ctx))
        }

        override fun visitConstraintDefinitionCheck(node: Constraint.Definition.Check, ctx: Ctx) =
            constraintDefinitionCheck(
                RexConverter.apply(node.expr, ctx.env),
                node.expr.sql()
            )

        override fun visitConstraintDefinitionUnique(node: Constraint.Definition.Unique, ctx: Ctx) =
            constraintDefinitionUnique(
                node.attributes?.map { convert(it) } ?: emptyList(), node.isPrimaryKey
            )

        override fun visitPartitionBy(node: PartitionBy, ctx: Ctx) = node.accept(this, ctx) as org.partiql.planner.internal.ir.PartitionBy

        override fun visitPartitionByAttrList(node: PartitionBy.AttrList, ctx: Ctx) =
            partitionByAttrList(node.list.map { convert(it) })

        @OptIn(PartiQLValueExperimental::class)
        override fun visitTableProperty(node: TableProperty, ctx: Ctx) =
            tableProperty(node.name, node.value)
    }
}
