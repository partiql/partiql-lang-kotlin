package org.partiql.plan.passes

import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.infer.PlannerContext
import org.partiql.lang.infer.QualifiedObjectName
import org.partiql.lang.infer.Session
import org.partiql.lang.types.BagType
import org.partiql.lang.types.StaticType
import org.partiql.lang.types.StructType
import org.partiql.plan.ir.Case
import org.partiql.plan.ir.Rel
import org.partiql.plan.ir.Rex
import org.partiql.plan.utils.PlanUtils
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.sources.TableSchema

/**
 * TODO: Add description
 */
object RexTyperBase : RexTyper<RexTyperBase.Context>() {

    /**
     * Returns the inferred static type
     */
    public fun type(node: Rex, ctx: Context): StaticType {
        return visitRex(node, ctx)
    }

    /**
     * Context object
     */
    public class Context(
        internal val input: Rel?,
        internal val session: ConnectorSession,
        internal val plannerContext: PlannerContext,
        internal val scopingOrder: ScopingOrder
    ) {
        internal val inputSchema = input?.let {
            PlanUtils.getSchema(it)
        } ?: emptyList()
        internal val metadata = plannerContext.metadata
    }

    /**
     * Scoping
     */
    public enum class ScopingOrder {
        GLOBALS_THEN_LEXICAL,
        LEXICAL_THEN_GLOBALS
    }

    override fun visitRexId(node: Rex.Id, ctx: Context): StaticType {
        val bindingName = rexIdToBindingName(node)
        val scopingOrder = when (node.qualifier) {
            Rex.Id.Qualifier.LOCALS_FIRST -> ScopingOrder.LEXICAL_THEN_GLOBALS
            Rex.Id.Qualifier.UNQUALIFIED -> ctx.scopingOrder
        }
        return when (scopingOrder) {
            ScopingOrder.GLOBALS_THEN_LEXICAL -> findGlobalBind(bindingName, ctx)
                ?: findLocalBind(bindingName, ctx)
                ?: StaticType.ANY
            ScopingOrder.LEXICAL_THEN_GLOBALS -> findLocalBind(bindingName, ctx)
                ?: findGlobalBind(bindingName, ctx)
                ?: StaticType.ANY
        }
    }

    //
    //
    // HELPER METHODS
    //
    //

    // TODO: Make Rex.Id.Case mandatory
    private fun rexIdToBindingName(node: Rex.Id): BindingName = BindingName(
        node.name,
        when (node.case) {
            Case.SENSITIVE -> BindingCase.SENSITIVE
            Case.INSENSITIVE -> BindingCase.INSENSITIVE
            else -> error("This shouldn't have happened.")
        }
    )

    // TODO: Don't hard-code
    private fun findGlobalBind(name: BindingName, ctx: Context): StaticType? {
        val session = connectorSessionToSession(ctx.session)
        val qualifiedName = QualifiedObjectName(
            BindingName("localdb", BindingCase.SENSITIVE),
            BindingName("house", BindingCase.SENSITIVE),
            name
        )
        ctx.metadata.getTableHandle(session, qualifiedName)?.let { handle ->
            ctx.metadata.getTableSchema(session, handle).toStaticType().let { return it }
        }
        return null
    }

    private fun findLocalBind(name: BindingName, ctx: Context): StaticType? {
        return ctx.inputSchema.firstOrNull {
            name.isEquivalentTo(it.name)
        }?.type
    }

    /**
     * TODO: This is due to a cyclic dependency. Plan relies on Lang for StaticType. Session should be part of Lang.
     *  StaticType should be part of SPI. Then the graph would be: SPI -> Plan -> Lang (Exec).
     *  Remove hard-code when the project structuring works out. Or maybe, temporarily move Session to Plan.
     */
    private fun connectorSessionToSession(session: ConnectorSession): Session {
        return Session(
            queryId = session.getQueryId(),
            catalog = "localdb",
            schema = "house"
        )
    }

    private fun TableSchema.toStaticType(): StaticType {
        return BagType(
            StructType(
                fields = this.attributes.associate {
                    it.name to it.type
                },
                contentClosed = true
            )
        )
    }
}
