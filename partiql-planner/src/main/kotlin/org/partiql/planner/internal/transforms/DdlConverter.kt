package org.partiql.planner.internal.transforms

import org.partiql.ast.AstNode
import org.partiql.ast.Constraint
import org.partiql.ast.DdlOp
import org.partiql.ast.Expr
import org.partiql.ast.Statement
import org.partiql.ast.TableDefinition
import org.partiql.ast.TableProperty
import org.partiql.ast.Type
import org.partiql.ast.visitor.AstBaseVisitor
import org.partiql.planner.internal.Env
import org.partiql.planner.internal.ir.Ddl
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.ddlConstraint
import org.partiql.planner.internal.ir.ddlConstraintBodyCheck
import org.partiql.planner.internal.ir.ddlConstraintBodyUnique
import org.partiql.planner.internal.ir.ddlCreateTable
import org.partiql.planner.internal.ir.ddlShape
import org.partiql.planner.internal.ir.ddlTableProperty
import org.partiql.types.DecimalType
import org.partiql.types.NumberConstraint
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.TimeType
import org.partiql.types.TimestampType
import org.partiql.value.PartiQLTimestampExperimental
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue
import java.sql.Timestamp

internal object DdlConverter {
    internal fun apply(statement: Statement.DDL, env: Env): Ddl = statement.accept(ToDdl, env)

    private object ToDdl : AstBaseVisitor<Ddl, Env>() {
        override fun defaultReturn(node: AstNode, ctx: Env): Ddl {
            throw IllegalArgumentException("unsupported ddl operation $node")
        }

        override fun visitStatementDDL(node: Statement.DDL, ctx: Env): Ddl =
            when (val op = node.op) {
                is DdlOp.CreateIndex -> TODO()
                is DdlOp.CreateTable -> visitDdlOpCreateTable(op, ctx)
                is DdlOp.DropIndex -> TODO()
                is DdlOp.DropTable -> TODO()
            }

        // Step 1: Get all the constraints
        // Step 2: Get all unique/ PK constraints
        // Step 3: validate all unique constraints refers to existing columns
        // Step 4: Validate all PK constraints refers to existing columns
        // Step 5: remove null if exists in fields.
        // TODO: Update this PK Partition by to identifier symbol
        override fun visitDdlOpCreateTable(node: DdlOp.CreateTable, ctx: Env): Ddl {
            val name = AstToPlan.convert(node.name)
            val definitions = node.definition?.columns?.map {
                calculateTypeAndRetrieveColumnConstraints(it, ctx)
            }?.unzip()
            val fields = definitions?.first ?: emptyList()
            val columnConstraints = definitions?.second?.flatten()
            val tableConstraints = node.definition?.constraints?.map {
                when (val body = it.body) {
                    is Constraint.Body.Check -> ddlConstraint(it.name, ddlConstraintBodyCheck(RexConverter.apply(body.expr, ctx)))
                    is Constraint.Body.NotNull -> TODO("Should not be in table constraint")
                    is Constraint.Body.Nullable -> TODO("Should not be in table constraint")
                    is Constraint.Body.Unique -> ddlConstraint(
                        it.name,
                        ddlConstraintBodyUnique(
                            body.columns
                                ?: error("table level unique constraint requires column specification"),
                            body.isPrimaryKey
                        )
                    )
                }
            }

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
                (columnConstraints.orEmpty() + tableConstraints.orEmpty())
                    .groupBy { it.body.javaClass }
                    .flatMap {
                        it.value.mapIndexed { index, constr ->
                            if (constr.name == null) {
                                constr.copy("\$${tableName(name)}_${it.key.simpleName}_$index", constr.body)
                            } else constr
                        }
                    }

            // verify key here?
            // assert single primary key clause
            val pk = constraints
                .filter { it.body is Ddl.Constraint.Body.Unique }
                .filter { (it.body as Ddl.Constraint.Body.Unique).isPrimaryKey }
            if (pk.size > 1) error("multiple primary key clause not allowed")

            // fields name unique? do we want this? is there a way to not have this in paritQL???
            val fieldsName = fields.map { it.name }
                .also { names ->
                    val duplicates = names
                        .groupingBy { it }
                        .eachCount()
                        .filter { it.value > 1 }
                        .keys
                    if (duplicates.size > 0) {
                        error("duplicated fields $duplicates")
                    }
                }

            // unique key column name needs to be in the declaration.....
            constraints.filter {
                it.body is Ddl.Constraint.Body.Unique
            }.let { uniques ->
                uniques.forEach {
                    val body = it.body as Ddl.Constraint.Body.Unique
                    val columns = body.columns
                    if (!fieldsName.containsAll(columns)) error("columns not specified")
                }
            }

            // TODO: Change AST model....
            val partition = node.partitionBy?.let { getPartitionColumn(it) } ?: emptyList()
            val tableProperties = node.tableProperties.map {
                visitTableProperty(it, ctx)
            }

            return ddlCreateTable(name, fields, constraints, partition, tableProperties)
        }

        @OptIn(PartiQLTimestampExperimental::class)
        private fun calculateTypeAndRetrieveColumnConstraints(node: TableDefinition.Column, ctx: Env): Pair<Ddl.Shape, List<Ddl.Constraint>> {
            val name = node.name
            val nonNullType = when (val type = node.type) {
                is Type.Any -> StaticType.ANY
                is Type.Bag -> StaticType.BAG
                is Type.Bigint -> StaticType.INT
                is Type.Bit -> TODO("BIT NOT SUPPORT IN STATIC TYPE")
                is Type.BitVarying -> TODO("BitVarying NOT SUPPORT IN STATIC TYPE")
                is Type.Blob -> StaticType.BLOB
                is Type.Bool -> StaticType.BOOL
                is Type.ByteString -> TODO("ByteString NOT SUPPORT IN STATIC TYPE")
                is Type.Char -> {
                    when (val l = type.length) {
                        // CHAR(1)
                        null -> StaticType.CHAR
                        else -> StringType(StringType.StringLengthConstraint.Constrained(NumberConstraint.Equals(l)))
                    }
                }
                is Type.Clob -> StaticType.CLOB
                is Type.Custom -> TODO("CUSTOM Type NOT SUPPORT YET")
                is Type.Date -> StaticType.DATE
                is Type.Decimal -> {
                    val p = type.precision
                    if (p != null) {
                        val s = type.scale ?: 0
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
                    val p = type.precision
                    if (p != null) {
                        val s = type.scale ?: 0
                        DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(p, s))
                    } else {
                        StaticType.DECIMAL
                    }
                }
                is Type.Real -> StaticType.FLOAT
                is Type.Sexp -> StaticType.LIST
                is Type.Smallint -> StaticType.INT2
                is Type.String -> {
                    when (val l = type.length) {
                        // CHAR(1)
                        null -> StaticType.STRING
                        else -> StringType(StringType.StringLengthConstraint.Constrained(NumberConstraint.UpTo(l)))
                    }
                }
                // TODO: Ignore nested constraint for now
                is Type.Struct -> {
                    val fields = type.fields.map {
                        calculateTypeAndRetrieveColumnConstraints(it, ctx)
                    }.unzip().let {
                        it.first.map {
                            StructType.Field(it.name, it.type)
                        }
                    }
                    StructType(fields)
                }
                is Type.Symbol -> TODO("Remove this")
                is Type.Time -> TimeType(type.precision)
                is Type.TimeWithTz -> TimeType(type.precision, true)
                is Type.Timestamp -> TimestampType(type.precision)
                is Type.TimestampWithTz -> TimestampType(type.precision, true)
                is Type.Tinyint -> StaticType.INT2
                is Type.Tuple -> StaticType.STRUCT
                is Type.Varchar -> {
                    when (val l = type.length) {
                        // CHAR(1)
                        null -> StaticType.STRING
                        else -> StringType(StringType.StringLengthConstraint.Constrained(NumberConstraint.UpTo(l)))
                    }
                }
            }
            var type = nonNullType.asNullable()

            val constraints = node.constraints.mapIndexedNotNull { index, constr ->
                when (val body = constr.body) {
                    is Constraint.Body.Check ->
                        ddlConstraint(constr.name, ddlConstraintBodyCheck(RexConverter.apply(body.expr, ctx)))
                    // not-null constraints do not have names
                    // as we directly modify the typing here.
                    // PostgreSQL seemingly is following a similar trend.
                    // https://www.postgresql.org/docs/16/ddl-alter.html#DDL-ALTER-REMOVING-A-CONSTRAINT
                    is Constraint.Body.NotNull -> {
                        type = StaticType.unionOf(type.allTypes.filter { it == StaticType.NULL }.toSet())
                        null
                    }
                    is Constraint.Body.Nullable -> {
                        type = StaticType.NULL
                        null
                    }
                    // column level unique
                    is Constraint.Body.Unique -> {
                        ddlConstraint(constr.name, ddlConstraintBodyUnique(listOf(name), body.isPrimaryKey))
                    }
                }
            }

            return ddlShape(name, type) to constraints
        }

//        override fun visitConstraintBodyCheck(node: Constraint.Body.Check, ctx: Env) =
//            ddlConstraintBodyCheck(RexConverter.apply(node.expr, ctx)) as Ddl.Constraint.Body

        @OptIn(PartiQLValueExperimental::class)
        override fun visitTableProperty(node: TableProperty, ctx: Env) = ddlTableProperty(node.name, node.value)

        @OptIn(PartiQLValueExperimental::class)
        private fun getPartitionColumn(node: Expr): List<String> {
            val node = node as? Expr.Collection ?: error("unsupported partition by clause")
            return node.values.map {
                val v = (it as Expr.Lit).value
                v as StringValue
                v.value!!
            }
        }
    }
}
