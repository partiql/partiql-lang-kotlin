package org.partiql.plan.passes.impl

import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.plan.PlannerSession
import org.partiql.plan.impl.PlannerContext
import org.partiql.plan.impl.QualifiedObjectName
import org.partiql.plan.ir.Case
import org.partiql.plan.ir.Rel
import org.partiql.plan.ir.Rex
import org.partiql.plan.passes.RexTyper
import org.partiql.spi.sources.TableSchema
import org.partiql.lang.types.BagType
import org.partiql.lang.types.StaticType
import org.partiql.lang.types.StructType
import org.partiql.plan.PlannerSession2
import org.partiql.plan.impl.PlannerContext2

/**
 * Poses as a mechanism to return the [StaticType] of an arbitrary [Rex].
 */
internal object RexTyperBase2 : RexTyper<RexTyperBase2.Context>() {

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
        internal val session: PlannerSession2,
        internal val plannerContext: PlannerContext2,
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

    override fun visitRexPath(node: Rex.Path, ctx: Context): StaticType {
        if (node.root is Rex.Id) {
            // TODO: Check if global binding

            // TODO: Check if catalog

            // TODO: Check if schema

            // TODO: Visit
        }
        return super.visitRexPath(node, ctx)
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

    // TODO: Add global bindings
    private fun findGlobalBind(name: BindingName, ctx: Context): StaticType? {
        ctx.metadata.getTableHandle(ctx.session, name)?.let { handle ->
            ctx.metadata.getTableSchema(ctx.session, handle).toStaticType().let { return it }
        }
        return null
    }

    private fun findLocalBind(name: BindingName, ctx: Context): StaticType? {
        return ctx.inputSchema.firstOrNull {
            name.isEquivalentTo(it.name)
        }?.type
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
