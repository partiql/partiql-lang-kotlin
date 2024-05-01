package org.partiql.eval.internal.operator.ddl

import org.partiql.eval.internal.operator.Operator
import org.partiql.plan.Constraint
import org.partiql.plan.Identifier
import org.partiql.plan.PartitionExpr
import org.partiql.plan.TableProperty
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.Connector
import org.partiql.spi.connector.ConnectorSession
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.int32Value
import org.partiql.value.stringValue

internal class DdlCreate(
    val name: Identifier,
    val shape: StaticType,
    val constraint: List<Constraint>,
    val partitionExpr: PartitionExpr?,
    val tableProperties: List<TableProperty>,
    val catalogs: Map<String, Connector>
) : Operator.Ddl {
    @PartiQLValueExperimental
    override fun create(): PartiQLValue {
        val (prefix, tableName) = when (name) {
            is Identifier.Qualified -> {
                val list = mutableListOf<String>()
                list.add(name.root.symbol)
                while (list.size != name.steps.size) {
                    list.add(name.steps[list.size - 1].symbol)
                }
                list.toList() to name.steps.last().symbol
            }
            is Identifier.Symbol -> emptyList<String>() to name.symbol
        }

        val session = object : ConnectorSession {
            override fun getQueryId(): String = "q"

            override fun getUserId(): String = "u"
        }

        // always assumes the first connector?
        val catalog = catalogs.values.first()

        val checkExpression = constraint
            .filter { it.body is Constraint.Body.Check }
            .map {
                val body = it.body as Constraint.Body.Check
                body.unlowered
            }

        val unique = constraint
            .filter { it.body is Constraint.Body.Unique && (it.body as Constraint.Body.Unique).isPrimaryKey != true }
            .flatMap {
                val body = it.body as Constraint.Body.Unique
                body.columns.map { it.symbol }
            }

        val primaryKey = constraint
            .filter { it.body is Constraint.Body.Unique && (it.body as Constraint.Body.Unique).isPrimaryKey != false }
            .flatMap {
                val body = it.body as Constraint.Body.Unique
                body.columns.map { it.symbol }
            }

        return try {
            catalog
                .getMetadata(session)
                .createTable(
                    name.toBindingPath(),
                    shape,
                    checkExpression,
                    unique,
                    primaryKey,
                )
            int32Value(1)
        } catch (e: Exception) {
            stringValue(e.message ?: "unknown message")
        }
    }

    private fun Identifier.toBindingPath() = when (this) {
        is Identifier.Qualified -> this.toBindingPath()
        is Identifier.Symbol -> BindingPath(listOf(this.toBindingName()))
    }

    private fun Identifier.Qualified.toBindingPath() =
        BindingPath(steps = listOf(this.root.toBindingName()) + steps.map { it.toBindingName() })

    private fun Identifier.Symbol.toBindingName() = BindingName(
        name = symbol,
        case = when (caseSensitivity) {
            Identifier.CaseSensitivity.SENSITIVE -> BindingCase.SENSITIVE
            Identifier.CaseSensitivity.INSENSITIVE -> BindingCase.INSENSITIVE
        }
    )
}
