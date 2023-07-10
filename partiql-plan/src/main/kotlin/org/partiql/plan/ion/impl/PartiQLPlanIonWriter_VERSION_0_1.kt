@file:Suppress("ClassName")

package org.partiql.plan.ion.impl

import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.SexpElement
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionSexpOf
import com.amazon.ionelement.api.ionSymbol
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rex
import org.partiql.plan.ion.IllegalPlanException
import org.partiql.plan.ion.PartiQLPlanIonWriter
import org.partiql.plan.visitor.PlanBaseVisitor
import org.partiql.types.AnyOfType
import org.partiql.types.AnyType
import org.partiql.types.BagType
import org.partiql.types.BlobType
import org.partiql.types.BoolType
import org.partiql.types.ClobType
import org.partiql.types.DateType
import org.partiql.types.DecimalType
import org.partiql.types.FloatType
import org.partiql.types.GraphType
import org.partiql.types.IntType
import org.partiql.types.ListType
import org.partiql.types.MissingType
import org.partiql.types.NullType
import org.partiql.types.SexpType
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.SymbolType
import org.partiql.types.TimeType
import org.partiql.types.TimestampType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.io.PartiQLValueIonWriter

/**
 * PartiQLPlanIonWriter for PartiQLVersion.VERSION_0_1
 */
internal object PartiQLPlanIonWriter_VERSION_0_1 : PartiQLPlanIonWriter {

    override fun toIon(plan: PartiQLPlan): IonElement = plan.accept(ToIon, ToIon.nil)

    /**
     * Internal entry point for testing.
     */
    @JvmStatic
    internal fun toIon(node: PlanNode, type: SexpElement? = null): IonElement = node.accept(ToIon, type ?: ionSexpOf())

    /**
     * Adds a `.toIon(): IonElement` function to all plan nodes.
     */
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private object ToIon : PlanBaseVisitor<IonElement, SexpElement>() {

        // To be used
        val nil: SexpElement = ionSexpOf()

        override fun defaultReturn(node: PlanNode, type: SexpElement): IonElement {
            error("ToIon not implemented for node $node")
        }

        // Rex

        override fun visitRex(node: Rex, type: SexpElement): IonElement {
            val t = node.type.toRef()
            return visitRexOp(node.op, t)
        }

        @OptIn(PartiQLValueExperimental::class)
        override fun visitRexOpLit(node: Rex.Op.Lit, type: SexpElement): IonElement {
            val tag = ionSymbol("lit")
            val lit = PartiQLValueIonWriter.toIon(node.value)
            return ionSexpOf(tag, type, lit)
        }

        override fun visitRexOpVarResolved(node: Rex.Op.Var.Resolved, type: SexpElement): IonElement {
            val tag = ionSymbol("var")
            val ref = ionInt(node.ref.toLong())
            return ionSexpOf(tag, type, ref)
        }

        override fun visitRexOpVarUnresolved(node: Rex.Op.Var.Unresolved, type: SexpElement): IonElement {
            // TODO, collect problems for better reporting
            throw IllegalPlanException("Plan has unresolved variables")
        }

        override fun visitRexOpGlobal(node: Rex.Op.Global, type: SexpElement): IonElement {
            val tag = ionSymbol("global")
            val ref = ionInt(node.ref.toLong())
            return ionSexpOf(tag, type, ref)
        }

        override fun visitRexOpPath(node: Rex.Op.Path, type: SexpElement): IonElement {
            val tag = ionSymbol("path")
            val root = visitRex(node.root, nil)
            val steps = node.steps.map { visitRexOpPathStep(it, nil) }
            return ionSexpOf(tag, type, root, ionSexpOf(steps))
        }

        override fun visitRexOpPathStepIndex(node: Rex.Op.Path.Step.Index, type: SexpElement): IonElement {
            val tag = ionSymbol("step")
            val rex = visitRex(node.key, nil)
            return ionSexpOf(tag, rex)
        }

        override fun visitRexOpPathStepWildcard(node: Rex.Op.Path.Step.Wildcard, type: SexpElement): IonElement {
            val tag = ionSymbol("step")
            val wildcard = ionSymbol("wildcard")
            return ionSexpOf(tag, wildcard)
        }

        override fun visitRexOpPathStepUnpivot(node: Rex.Op.Path.Step.Unpivot, type: SexpElement): IonElement {
            val tag = ionSymbol("step")
            val wildcard = ionSymbol("unpivot")
            return ionSexpOf(tag, wildcard)
        }

        // Rel

        // TYPES

        // !! NEED A STATIC TYPE TO PARTIQL HEADER MAP !!
        // !! HIGHLY SIMPLIFIED FOR BOOTSTRAPPING PURPOSES !!

        private fun StaticType.toRef(): SexpElement {
            val tag = ionSymbol("\$type")
            val ordinal = when (this) {
                is AnyOfType -> 0
                is AnyType -> 0
                is NullType -> 1
                MissingType -> 1
                // Boolean types
                is BoolType -> 2
                // Numeric types
                is IntType -> 3
                is DecimalType -> 4
                is FloatType -> 5
                // Character strings
                is StringType -> 6
                is SymbolType -> 7
                // Byte strings
                is BlobType -> 8
                is ClobType -> 9
                is DateType -> TODO()
                // Collections
                is BagType -> 10
                is ListType -> 11
                is SexpType -> 12
                // Additional types
                is GraphType -> 13
                is StructType -> 14
                // Date/Time types
                is TimeType -> 15
                is TimestampType -> 16
            }
            return ionSexpOf(tag, ionInt(ordinal.toLong()))
        }
    }
}
