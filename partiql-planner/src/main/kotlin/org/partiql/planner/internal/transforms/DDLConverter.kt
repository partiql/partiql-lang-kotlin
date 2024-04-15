package org.partiql.planner.internal.transforms

import org.partiql.ast.AstNode
import org.partiql.ast.Constraint
import org.partiql.ast.DdlOp
import org.partiql.ast.PartitionExpr
import org.partiql.ast.Statement
import org.partiql.ast.TableDefinition
import org.partiql.ast.Type
import org.partiql.ast.sql.sql
import org.partiql.ast.visitor.AstBaseVisitor
import org.partiql.planner.internal.Env
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.PlanNode
import org.partiql.planner.internal.ir.Statement.DDL
import org.partiql.planner.internal.ir.constraint
import org.partiql.planner.internal.ir.constraintBodyCheck
import org.partiql.planner.internal.ir.constraintBodyUnique
import org.partiql.planner.internal.ir.ddlOpCreateTable
import org.partiql.planner.internal.ir.partitionExprColumnList
import org.partiql.planner.internal.ir.statementDDL
import org.partiql.planner.internal.ir.tableProperty
import org.partiql.planner.internal.transforms.AstToPlan.convert
import org.partiql.planner.internal.transforms.DDLConverter.ToDdl.FieldDefinition.Companion.pushDownPK
import org.partiql.types.BagType
import org.partiql.types.DecimalType
import org.partiql.types.NumberConstraint
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.TimeType
import org.partiql.types.TimestampType
import org.partiql.value.PartiQLTimestampExperimental
import org.partiql.value.PartiQLValueExperimental
import org.partiql.planner.internal.ir.Constraint as PlanConstraint

internal object DDLConverter {
    internal fun apply(statement: Statement.DDL, env: Env): DDL = statement.accept(ToDdl, Ctx(env, null)) as DDL

    private data class Ctx(
        val env: Env,
        val currentFieldName: Identifier.Symbol?
    )

    private object ToDdl : AstBaseVisitor<PlanNode, Ctx>() {

        override fun defaultReturn(node: AstNode, ctx: Ctx): DDL {
            throw IllegalArgumentException("unsupported ddl operation $node")
        }

        override fun visitStatementDDL(node: Statement.DDL, ctx: Ctx): DDL =
            when (val op = node.op) {
                is DdlOp.CreateIndex -> TODO("DDL Op Create Index not supported in the Plan")
                is DdlOp.CreateTable -> visitDdlOpCreateTable(op, ctx)
                is DdlOp.DropIndex -> TODO("DDL Op Drop Index not supported in the plan")
                is DdlOp.DropTable -> TODO("DDL Op Drop Table not supported in the plan")
            }

        @OptIn(PartiQLValueExperimental::class)
        override fun visitDdlOpCreateTable(node: DdlOp.CreateTable, ctx: Ctx): DDL {
            val name = convert(node.name)
            val definition = node.definition?.columns?.map { colDef ->
                val colName = convert(colDef.name)
                val fieldDef = FieldDefinition.computeFromColumnDef(colDef)
                val columnConstraints = colDef.constraints.mapNotNull { constr ->
                    val name = constr.name
                    when (val body = constr.body) {
                        is Constraint.Body.Check -> constraint(name, visitConstraintBodyCheck(body, Ctx(ctx.env, colName)))
                        is Constraint.Body.Unique -> constraint(name, visitConstraintBodyUnique(body, Ctx(ctx.env, colName)))
                        // lowered.
                        is Constraint.Body.NotNull -> null
                        is Constraint.Body.Nullable -> null
                    }
                }
                fieldDef to columnConstraints
            } ?: emptyList()

            val fieldDefs = definition.map { it.first }
            val columnConstraints = definition.flatMap { it.second }

            val tableConstraints = node.definition?.constraints?.mapNotNull { constr ->
                val name = constr.name
                when (val body = constr.body) {
                    is Constraint.Body.Check -> constraint(name, visitConstraintBodyCheck(body, ctx))
                    is Constraint.Body.Unique -> constraint(name, visitConstraintBodyUnique(body, ctx))
                    // impossible
                    is Constraint.Body.NotNull -> null
                    is Constraint.Body.Nullable -> null
                }
            } ?: emptyList()

            val allConstraints = verifyConstraints(columnConstraints + tableConstraints, fieldDefs.map { it.name })

            val primaryKeys = allConstraints.filter { it.body is PlanConstraint.Body.Unique && it.body.isPrimaryKey }.firstOrNull()?.let {
                it.body as PlanConstraint.Body.Unique
                it.body.columns
            } ?: emptyList()

            val fields = fieldDefs.pushDownPK(primaryKeys)

            val type = fields.map { it.toStructField() }.let { BagType(StructType(it)) }

            // generate constraints names if needed
            val tableName: (Identifier) -> String = {
                when (it) {
                    is Identifier.Qualified -> {
                        val tableName = it.steps.last()
                        when (tableName.caseSensitivity) {
                            Identifier.CaseSensitivity.SENSITIVE -> tableName.symbol
                            Identifier.CaseSensitivity.INSENSITIVE -> tableName.symbol.uppercase()
                        }
                    }
                    is Identifier.Symbol -> {
                        when (it.caseSensitivity) {
                            Identifier.CaseSensitivity.SENSITIVE -> it.symbol
                            Identifier.CaseSensitivity.INSENSITIVE -> it.symbol.uppercase()
                        }
                    }
                }
            }

            val constraints =
                allConstraints
                    .groupBy { it.body.javaClass }
                    .flatMap {
                        it.value.mapIndexed { index, constr ->
                            if (constr.name == null) {
                                constr.copy("\$${tableName(name)}_${it.key.simpleName}_$index", constr.body)
                            } else constr
                        }
                    }

            val partition = node.partitionBy?.let { getPartitionColumn(it) }
            val tableProperties = node.tableProperties.map {
                tableProperty(it.name, it.value)
            }

            return statementDDL(ddlOpCreateTable(name, type, constraints, partition, tableProperties))
        }

        // To Verify:
        // 1. Only one primary key constraint exists
        // 2. Unique Constraint, attribute no duplications
        // 3. Unique Constraint, attribute is declared
        // 4. no duplicated names
        private fun verifyConstraints(constraints: List<PlanConstraint>, fieldNames: List<Identifier.Symbol>): List<PlanConstraint> {
            var nameSet = mutableSetOf<String>()
            var seenPk = false
            constraints.fold(nameSet) { acc, constraint ->
                val constrName = constraint.name
                if (acc.contains(constrName)) TODO("DUPLICATED NAME")
                constrName?.let { acc.add(constrName) }

                if (constraint.body is PlanConstraint.Body.Unique) {
                    if (constraint.body.isPrimaryKey && seenPk) {
                        TODO("Multiple Declaration of Primary key")
                    }
                    var seen = mutableSetOf<String>()
                    constraint.body.columns.fold(seen) { acc, symbol ->
                        if (!fieldNames.find(symbol)) TODO("Attribue $symbol not yet declared")
                        acc.add(symbol.normalize())
                        acc
                    }
                }
                acc
            }
            return constraints
        }

        private fun Identifier.Symbol.normalize() = when (this.caseSensitivity) {
            Identifier.CaseSensitivity.SENSITIVE -> this.symbol
            Identifier.CaseSensitivity.INSENSITIVE -> this.symbol.uppercase()
        }

        private fun List<Identifier.Symbol>.find(symbol: Identifier.Symbol) =
            this.any() { it.normalize() == symbol.normalize() }
        override fun visitConstraintBodyCheck(node: Constraint.Body.Check, ctx: Ctx) =
            constraintBodyCheck(
                RexConverter.apply(node.expr, ctx.env),
                node.expr.sql()
            )
        override fun visitConstraintBodyUnique(node: Constraint.Body.Unique, ctx: Ctx) =
            constraintBodyUnique(
                node.columns?.map { convert(it) }
                    ?: ctx.currentFieldName?.let { listOf(it) }
                    ?: error("no attribute supplied for unique constraint"),
                node.isPrimaryKey
            )

        private data class FieldDefinition(
            val name: Identifier.Symbol,
            val astType: Type,
            val isNullable: Boolean,
            val isNull: Boolean,
            val isOptional: Boolean,
            val comment: String?,
        ) {

            @OptIn(PartiQLTimestampExperimental::class)
            fun toStructField(): StructType.Field {
                val name = when (name.caseSensitivity) {
                    Identifier.CaseSensitivity.SENSITIVE -> this.name.symbol
                    Identifier.CaseSensitivity.INSENSITIVE -> this.name.symbol.uppercase()
                }

                if (isNull && !isNullable) TODO("Error Null and not null the same time.")

                if (this.isNull) return StructType.Field(name, StaticType.NULL)
                val nonNullType = when (astType) {
                    is Type.Any -> StaticType.ANY
                    is Type.Bag -> StaticType.BAG
                    is Type.Bigint -> StaticType.INT
                    is Type.Bit -> TODO("BIT NOT SUPPORT IN STATIC TYPE")
                    is Type.BitVarying -> TODO("BitVarying NOT SUPPORT IN STATIC TYPE")
                    is Type.Blob -> StaticType.BLOB
                    is Type.Bool -> StaticType.BOOL
                    is Type.ByteString -> TODO("ByteString NOT SUPPORT IN STATIC TYPE")
                    is Type.Char -> {
                        when (val l = astType.length) {
                            // CHAR(1)
                            null -> StaticType.CHAR
                            else -> StringType(StringType.StringLengthConstraint.Constrained(NumberConstraint.Equals(l)))
                        }
                    }
                    is Type.Clob -> StaticType.CLOB
                    is Type.Custom -> TODO("CUSTOM Type NOT SUPPORT YET")
                    is Type.Date -> StaticType.DATE
                    is Type.Decimal -> {
                        val p = astType.precision
                        if (p != null) {
                            val s = astType.scale ?: 0
                            DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(p, s))
                        } else {
                            StaticType.DECIMAL
                        }
                    }
                    is Type.Float32 -> TODO("Float32 NOT SUPPORT IN STATIC TYPE")
                    is Type.Float64 -> StaticType.FLOAT
                    is Type.Int -> StaticType.INT
                    is Type.Int2 -> StaticType.INT2
                    is Type.Int4 -> StaticType.INT4
                    is Type.Int8 -> StaticType.INT8
                    is Type.Interval -> TODO("Interval NOT SUPPORT IN STATIC TYPE")
                    // Adding support for element types in parser
                    is Type.List -> StaticType.LIST
                    is Type.Missing -> TODO("Remove this")
                    is Type.NullType -> TODO("Remove this")
                    is Type.Numeric -> {
                        val p = astType.precision
                        if (p != null) {
                            val s = astType.scale ?: 0
                            DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(p, s))
                        } else {
                            StaticType.DECIMAL
                        }
                    }
                    is Type.Real -> StaticType.FLOAT
                    is Type.Sexp -> StaticType.LIST
                    is Type.Smallint -> StaticType.INT2
                    is Type.String -> {
                        when (val l = astType.length) {
                            // CHAR(1)
                            null -> StaticType.STRING
                            else -> StringType(StringType.StringLengthConstraint.Constrained(NumberConstraint.UpTo(l)))
                        }
                    }
                    is Type.Struct -> {
                        astType.fields.map {
                            computeFromFieldDef(it)
                        }.map {
                            it.toStructField()
                        }.let {
                            StructType(it)
                        }
                    }
                    is Type.Symbol -> TODO("Remove this")
                    is Type.Time -> TimeType(astType.precision)
                    is Type.TimeWithTz -> TimeType(astType.precision, true)
                    is Type.Timestamp -> TimestampType(astType.precision)
                    is Type.TimestampWithTz -> TimestampType(astType.precision, true)
                    is Type.Tinyint -> StaticType.INT2
                    is Type.Tuple -> StaticType.STRUCT
                    is Type.Varchar -> {
                        when (val l = astType.length) {
                            // CHAR(1)
                            null -> StaticType.STRING
                            else -> StringType(StringType.StringLengthConstraint.Constrained(NumberConstraint.UpTo(l)))
                        }
                    }
                }
                val type = nonNullType.let {
                    if (isOptional) it.asOptional()
                    else it
                }.let {
                    if (isNullable) it.asNullable()
                    it
                }

                val meta = if (comment != null) {
                    mapOf("comment" to comment)
                } else emptyMap()
                return StructType.Field(name, type, meta)
            }
            companion object {
                fun computeFromColumnDef(columnDef: TableDefinition.Column): FieldDefinition {
                    val name = convert(columnDef.name)
                    val isNullable = columnDef.constraints.none { it.body is Constraint.Body.NotNull }
                    val isNull = columnDef.constraints.any { it.body is Constraint.Body.Nullable }

                    return FieldDefinition(name, columnDef.type, isNullable, isNull, columnDef.isOptional, columnDef.comment)
                }

                fun computeFromFieldDef(fieldDef: Type.Struct.Field): FieldDefinition {
                    val name = convert(fieldDef.name)
                    val isNullable = fieldDef.constraints.none { it.body is Constraint.Body.Nullable }
                    val isNull = fieldDef.constraints.any { it.body is Constraint.Body.Nullable }

                    return FieldDefinition(name, fieldDef.type, isNullable, isNull, fieldDef.isOptional, fieldDef.comment)
                }

                fun List<FieldDefinition>.pushDownPK(attrList: List<Identifier.Symbol>) =
                    this.map { field ->
                        attrList.forEach { attrName ->
                            if (field.name.normalize() == attrName.normalize()) {
                                return@map field.copy(
                                    name = field.name,
                                    astType = field.astType,
                                    isNullable = false,
                                    isNull = field.isNull,
                                    isOptional = field.isOptional,
                                    comment = field.comment
                                )
                            }
                        }
                        field
                    }
            }
        }

        @OptIn(PartiQLValueExperimental::class)
        private fun getPartitionColumn(expr: PartitionExpr) =
            when (expr) {
                is PartitionExpr.ColomnList -> partitionExprColumnList(expr.list.map { convert(it) })
            }
    }
}
