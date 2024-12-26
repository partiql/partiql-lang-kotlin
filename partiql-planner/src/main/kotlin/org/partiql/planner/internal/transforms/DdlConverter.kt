
package org.partiql.planner.internal.transforms

import org.partiql.ast.AstNode
import org.partiql.ast.AstVisitor
import org.partiql.ast.DataType
import org.partiql.ast.ddl.AttributeConstraint.Check
import org.partiql.ast.ddl.AttributeConstraint.Null
import org.partiql.ast.ddl.AttributeConstraint.Unique
import org.partiql.ast.ddl.ColumnDefinition
import org.partiql.ast.ddl.CreateTable
import org.partiql.ast.ddl.Ddl
import org.partiql.ast.ddl.KeyValue
import org.partiql.ast.ddl.PartitionBy
import org.partiql.ast.ddl.TableConstraint
import org.partiql.ast.sql.sql
import org.partiql.planner.internal.DdlField
import org.partiql.planner.internal.Env
import org.partiql.planner.internal.ir.PlanNode
import org.partiql.planner.internal.ir.Statement
import org.partiql.planner.internal.ir.statementDDL
import org.partiql.planner.internal.ir.statementDDLAttribute
import org.partiql.planner.internal.ir.statementDDLCommandCreateTable
import org.partiql.planner.internal.ir.statementDDLConstraintCheck
import org.partiql.planner.internal.ir.statementDDLPartitionByAttrList
import org.partiql.planner.internal.ir.statementDDLTableProperty
import org.partiql.planner.internal.transforms.AstToPlan.convert
import org.partiql.planner.internal.transforms.AstToPlan.visitType
import org.partiql.spi.catalog.Identifier
import org.partiql.types.PType
import org.partiql.types.shape.PShape

internal object DdlConverter {
    internal fun apply(statement: Ddl, env: Env): Statement.DDL = ToDdl.visitDdl(statement, env)

    /**
     * Consider this as the first step to lowering create table statement to [PShape]
     *
     * Post this processing:
     * We made sure that:
     * 1. At column level
     *    - No multiple declaration of primary key constraint associated with one attribute declaration
     *       - i.e., `FOO INT2 PRIMARY KEY PRIMARY KEY` will be rejected
     *    - No optional attribute is declared as priamry key at column level
     *       - i.e., FOO OPTIONAL INT2 PRIMARY KEY will be rejected
     *    - Nullability and optionality is deducted from column level constraint
     *    - Comment is attached to the PType via PTrait
     * 2. At table level
     * - No multiple declaration of primary key constraint assciated with table
     *  ```
     *     CREATE TABLE ... (
     *         ...
     *         PRIMARY KEY (foo)
     *         PRIMARY KEY (bar)
     *     )
     * ```
     * will be rejected.
     * - All unique constraints declared at table level will be concatenated to a single list
     */
    private object ToDdl : AstVisitor<PlanNode, Env>() {

        override fun defaultReturn(node: AstNode?, ctx: Env): PlanNode {
            throw IllegalArgumentException("unsupported DDL node: $node")
        }

        override fun visitDdl(node: Ddl, ctx: Env): Statement.DDL {
            return when (node) {
                is CreateTable -> statementDDL(visitCreateTable(node, ctx))
                else -> throw IllegalArgumentException("Unsupported DDL Command: $node")
            }
        }

        override fun visitCreateTable(node: CreateTable, ctx: Env): Statement.DDL.Command.CreateTable {
            val tableName = convert(node.name)
            val attributes = node.columns.map { visitColumnDefinition(it, ctx) }
            // Table Level PK
            val pk = node.constraints.filterIsInstance<TableConstraint.Unique>()
                .filter { it.isPrimaryKey }
                .let {
                    when (it.size) {
                        0 -> emptyList()
                        1 -> it.first().columns.map { convert(it) }
                        else -> throw IllegalArgumentException("multiple PK")
                    }
                }

            val unique = node.constraints.filterIsInstance<TableConstraint.Unique>()
                .filter { !it.isPrimaryKey }
                .fold(emptyList<Identifier>()) { acc, constr ->
                    acc + constr.columns.map { convert(it) }
                }

            val partitionBy = node.partitionBy?.let { visitPartitionBy(it, ctx) }
            val tableProperty = node.tableProperties.map { visitKeyValue(it, ctx) }

            return statementDDLCommandCreateTable(
                tableName,
                attributes,
                emptyList(),
                partitionBy,
                tableProperty,
                pk,
                unique
            )
        }

        // !!! The planning stage ignores the constraint name for now
        override fun visitColumnDefinition(node: ColumnDefinition, ctx: Env): Statement.DDL.Attribute {
            val name = convert(node.name)
            val type = visitType(node.dataType, ctx)

            // Validation and reducing for nullable constraint
            // If there are one or more nullable constraint, last one wins
            // otherwise, nullable by default
            val nullableConstraint = node.constraints
                .filterIsInstance<Null>()
                .reduceOrNull { acc, next -> next }
                ?.isNullable ?: true

            // Validation and reducing for PK constraints
            // Rule: No multiple PK constraint
            val isPk = node.constraints
                .filterIsInstance<Unique>()
                .filter { it.isPrimaryKey }
                .let {
                    if (it.size > 1) {
                        throw IllegalArgumentException("Multiple primary key constraint declarations are not allowed.")
                    } else it
                }.any()

            // validation -- No optional attribute declared as primary key
            if (isPk && node.isOptional) throw IllegalArgumentException("Optional attribute as primary key is not supported.")

            // final nullability decision:
            // if nullableConstraint was concluded to be not null,
            //      then not null
            // if nullableConstraint was concluded to be nullable, and there is a valid PK constraint
            //      then not null
            // if nullableConstraint was concluded to be nullable, and there is no valid PK constraint
            //      then nullable
            val nullable = nullableConstraint && !isPk

            // Uniqueness decision
            // if associated with unique constraint or Primary key constraint, true
            // else false
            val isUnique = node
                .constraints
                .filterIsInstance<Unique>()
                .any() || isPk

            val additionalConstrs = node.constraints
                .filter { it !is Null && it !is Unique }
                .map { it.accept(this, ctx) as Statement.DDL.Constraint }

            return statementDDLAttribute(name, type, nullable, node.isOptional, isPk, isUnique, additionalConstrs, node.comment)
        }

        override fun visitCheck(node: Check, ctx: Env): Statement.DDL.Constraint =
            statementDDLConstraintCheck(
                RexConverter.apply(node.searchCondition, ctx),
                node.searchCondition.sql()
            )

        override fun visitPartitionBy(node: PartitionBy, ctx: Env): Statement.DDL.PartitionBy {
            return statementDDLPartitionByAttrList(node.columns.map { convert(it) })
        }

        override fun visitKeyValue(node: KeyValue, ctx: Env): Statement.DDL.TableProperty {
            return statementDDLTableProperty(node.key, node.value)
        }

        private fun visitType(node: DataType, ctx: Env): PShape {
            // Struct requires special process in DDL
            return if (node.code() == DataType.STRUCT) {
                val fields = node.fields.map { field ->
                    val name = convert(field.name)
                    val type = visitType(field.type, ctx)
                    // No support for nested PK or UNIQUE
                    val hasUnique = field.constraints
                        .filterIsInstance<Unique>()
                        .any()
                    if (hasUnique) {
                        throw IllegalArgumentException("Associating Primary Key Constraint or Unique Constraint on Struct Field is not supported")
                    }

                    val isNullable = field.constraints
                        .filterIsInstance<Null>()
                        .reduceOrNull { acc, next -> next }
                        ?.isNullable ?: true

                    val additionalConsts = field.constraints
                        .filterNot { it is Null }
                        .map { it.accept(this, ctx) as Statement.DDL.Constraint }

                    DdlField(name, type, isNullable, field.isOptional, additionalConsts, false, false, field.comment)
                }
                PShape(PType.row(fields))
            } else {
                PShape(visitType(node).getDelegate())
            }
        }
    }
}
