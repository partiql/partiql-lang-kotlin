package org.partiql.eval.internal.operator.ddl

import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import org.partiql.eval.internal.operator.Operator
import org.partiql.plan.Constraint
import org.partiql.plan.Identifier
import org.partiql.plan.PartitionExpr
import org.partiql.plan.TableProperty
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.Connector
import org.partiql.spi.connector.ConnectorPath
import org.partiql.spi.connector.ConnectorSession
import org.partiql.types.BagType
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue
import org.partiql.value.int32Value

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
        val (prefix, tableName) = when(name) {
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

        val session =  object : ConnectorSession {
            override fun getQueryId(): String = "q"

            override fun getUserId(): String = "u"
        }

        // if prefix is 0, how do we choose the default one ????
        val catalog = when(prefix.size) {
            0 -> catalogs.values.first()
            1 -> catalogs.values.first()
            else -> catalogs[prefix.first()] ?: error("no such connector")
        }

        val checkExpression = constraint
            .filter { it.body is Constraint.Body.Check }
            .map {
                val body = it.body as Constraint.Body.Check
                body.unlowered
            }

        val unique =  constraint
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
            int32Value(-1)
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
