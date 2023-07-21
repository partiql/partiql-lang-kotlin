@file:Suppress("ClassName")

package org.partiql.plan.ion.impl

import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionNull
import com.amazon.ionelement.api.ionSexpOf
import com.amazon.ionelement.api.ionSymbol
import org.partiql.plan.Fn
import org.partiql.plan.Global
import org.partiql.plan.Identifier
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.Statement
import org.partiql.plan.Type
import org.partiql.plan.ion.IllegalPlanException
import org.partiql.plan.ion.PartiQLPlanIonWriter
import org.partiql.plan.visitor.PlanBaseVisitor
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.io.PartiQLValueIonWriter

/**
 * PartiQLPlanIonWriter for PartiQLVersion.VERSION_0_1
 *
 * TODOs
 *  - Error collection / reporting
 */
internal object PartiQLPlanIonWriter_VERSION_0_1 : PartiQLPlanIonWriter {

    /**
     * Writes a PartiQLPlan object to an Ion value adhering to the PartiQL Plan 1.0 Ion Representation specification.
     */
    override fun toIon(plan: PartiQLPlan): IonElement {
        val writer = ToIon(Mode.ERR)
        return plan.accept(writer, ionNull())
    }

    /**
     * Writes a PartiQLPlan with debug annotations; does not throw on unresolved.
     */
    override fun toIonDebug(plan: PartiQLPlan): IonElement {
        val writer = ToIon(Mode.DEBUG)
        return plan.accept(writer, ionNull())
    }

    /**
     * Internal entry point for testing.
     */
    @JvmStatic
    internal fun toIonDebug(node: PlanNode, ctx: IonElement = ionNull()): IonElement {
        val writer = ToIon(Mode.DEBUG)
        return node.accept(writer, ctx)
    }

    private enum class Mode {
        ERR, // ERR on unresolved
        DEBUG // "?" on unresolved, consider mode which adds annotations
    }

    /**
     * Adds a `.toIon(): IonElement` function to all plan nodes.
     */
    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    private class ToIon(private val mode: Mode) : PlanBaseVisitor<IonElement, IonElement>() {

        // Empty context; I would use ignore `_` if Kotlin allowed that.
        private val nil: IonElement = ionNull()

        override fun defaultReturn(node: PlanNode, nil: IonElement): IonElement {
            error("ToIon not implemented for node $node")
        }

        // Top-Level

        override fun visitPartiQLPlan(node: PartiQLPlan, nil: IonElement): IonElement {
            val tag = ionSymbol("plan", annotations = listOf("partiql"))
            val version = ionSexpOf(ionInt(0), ionInt(1), annotations = listOf("version"))
            // TODO import
            val globals = ionSexpOf(node.globals.map { visitGlobal(it, nil) })
            val statement = visitStatement(node.statement, nil)
            return ionSexpOf(tag, version, globals, statement)
        }

        // Statements

        override fun visitStatement(node: Statement, nil: IonElement): IonElement {
            val tag = ionSymbol("statement")
            val statement = super.visitStatement(node, nil)
            return ionSexpOf(tag, statement)
        }

        override fun visitStatementQuery(node: Statement.Query, nil: IonElement): IonElement {
            val tag = ionSymbol("query")
            val rex = visitRex(node.root, nil)
            return ionSexpOf(tag, rex)
        }

        // Types

        override fun visitTypeAtomic(node: Type.Atomic, nil: IonElement): IonElement {
            val tag = ionSymbol(node.symbol)
            return ionSexpOf(tag)
        }

        override fun visitTypeRef(node: Type.Ref, nil: IonElement): IonElement {
            var tag = ionSymbol("\$type")
            val ref = ionInt(node.ordinal.toLong())
            if (mode == Mode.DEBUG) tag = tag.copy(listOf(node.annotation))
            return ionSexpOf(tag, ref)
        }

        // Functions

        override fun visitFn(node: Fn, nil: IonElement): IonElement {
            val tag = ionSymbol("fn")
            val id = ionSymbol(node.id)
            val params = ionSexpOf(node.params.map { visitFnParam(it, nil) })
            val returns = ionSexpOf(visitTypeRef(node.returns, nil), annotations = listOf("returns"))
            return ionSexpOf(tag, id, params, returns)
        }

        override fun visitFnParamValue(node: Fn.Param.Value, nil: IonElement): IonElement {
            val tag = ionSymbol("v")
            val type = visitTypeRef(node.type, nil)
            return ionSexpOf(tag, type)
        }

        override fun visitFnParamType(node: Fn.Param.Type, nil: IonElement): IonElement {
            val tag = ionSymbol("t")
            val type = visitTypeRef(node.type, nil)
            return ionSexpOf(tag, type)
        }

        override fun visitFnRefResolved(node: Fn.Ref.Resolved, nil: IonElement): IonElement {
            val tag = ionSymbol("\$fn")
            val ref = ionInt(node.ordinal.toLong())
            return ionSexpOf(tag, ref)
        }

        override fun visitFnRefUnresolved(node: Fn.Ref.Unresolved, nil: IonElement): IonElement {
            return when (mode) {
                Mode.ERR -> throw IllegalPlanException("Plan has unresolved function $node")
                Mode.DEBUG -> {
                    val tag = ionSymbol("\$fn")
                    val id = node.identifier.sql()
                    ionSexpOf(tag, ionSymbol("?", annotations = listOf(id)))
                }
            }
        }

        // Globals

        override fun visitGlobal(node: Global, nil: IonElement): IonElement {
            val type = visitTypeRef(node.type, nil)
            val identifier = visitIdentifierQualified(node.path, nil)
            return ionSexpOf(type, identifier)
        }

        // Rex : ctx -> type.ref

        override fun visitRex(node: Rex, nil: IonElement): IonElement {
            val type = visitTypeRef(node.type, nil)
            return visitRexOp(node.op, type)
        }

        @OptIn(PartiQLValueExperimental::class)
        override fun visitRexOpLit(node: Rex.Op.Lit, type: IonElement): IonElement {
            val tag = ionSymbol("lit")
            val lit = PartiQLValueIonWriter.toIon(node.value)
            return ionSexpOf(tag, type, lit)
        }

        override fun visitRexOpVarResolved(node: Rex.Op.Var.Resolved, type: IonElement): IonElement {
            val tag = ionSymbol("var")
            val ref = ionInt(node.ref.toLong())
            return ionSexpOf(tag, type, ref)
        }

        override fun visitRexOpVarUnresolved(node: Rex.Op.Var.Unresolved, type: IonElement): IonElement {
            return when (mode) {
                Mode.ERR -> throw IllegalPlanException("Plan has unresolved variable $node")
                Mode.DEBUG -> {
                    val tag = ionSymbol("var")
                    val id = node.identifier.sql()
                    ionSexpOf(tag, ionSymbol("?", annotations = listOf(id)))
                }
            }
        }

        override fun visitRexOpGlobal(node: Rex.Op.Global, type: IonElement): IonElement {
            val tag = ionSymbol("global")
            val ref = ionInt(node.ref.toLong())
            return ionSexpOf(tag, type, ref)
        }

        override fun visitRexOpPath(node: Rex.Op.Path, type: IonElement): IonElement {
            val tag = ionSymbol("path")
            val root = visitRex(node.root, nil)
            val steps = node.steps.map { visitRexOpPathStep(it, nil) }
            return ionSexpOf(tag, type, root, ionSexpOf(steps))
        }

        override fun visitRexOpPathStepIndex(node: Rex.Op.Path.Step.Index, nil: IonElement): IonElement {
            val tag = ionSymbol("step")
            val rex = visitRex(node.key, nil)
            return ionSexpOf(tag, rex)
        }

        override fun visitRexOpPathStepWildcard(node: Rex.Op.Path.Step.Wildcard, nil: IonElement): IonElement {
            val tag = ionSymbol("step")
            val wildcard = ionSymbol("wildcard")
            return ionSexpOf(tag, wildcard)
        }

        override fun visitRexOpPathStepUnpivot(node: Rex.Op.Path.Step.Unpivot, nil: IonElement): IonElement {
            val tag = ionSymbol("step")
            val wildcard = ionSymbol("unpivot")
            return ionSexpOf(tag, wildcard)
        }

        // Consider type? Well, is fn a type?
        override fun visitRexOpCall(node: Rex.Op.Call, nil: IonElement): IonElement {
            val tag = ionSymbol("call")
            val fn = visitFnRef(node.fn, nil)
            val args = ionSexpOf(node.args.map { visitRexOpCallArg(it, nil) })
            return ionSexpOf(tag, fn, args)
        }

        override fun visitRexOpCallArgValue(node: Rex.Op.Call.Arg.Value, nil: IonElement): IonElement {
            val tag = ionSymbol("v")
            val rex = visitRex(node.rex, nil)
            return ionSexpOf(tag, rex)
        }

        override fun visitRexOpCallArgType(node: Rex.Op.Call.Arg.Type, nil: IonElement): IonElement {
            val tag = ionSymbol("t")
            val type = visitTypeRef(node.type, nil)
            return ionSexpOf(tag, type)
        }

        override fun visitRexOpCase(node: Rex.Op.Case, type: IonElement): IonElement {
            val tag = ionSymbol("case")
            val rex = visitRex(node.rex, nil)
            val branches = ionSexpOf(node.branches.map { visitRexOpCaseBranch(it, nil) })
            return ionSexpOf(tag, type, rex, branches)
        }

        override fun visitRexOpCaseBranch(node: Rex.Op.Case.Branch, nil: IonElement): IonElement {
            val condition = visitRex(node.condition, nil)
            val rex = visitRex(node.rex, nil)
            return ionSexpOf(condition, rex)
        }

        override fun visitRexOpCollection(node: Rex.Op.Collection, type: IonElement): IonElement {
            val tag = ionSymbol("collection")
            val values = ionSexpOf(node.values.map { visitRex(it, nil) })
            return ionSexpOf(tag, type, values)
        }

        override fun visitRexOpStruct(node: Rex.Op.Struct, type: IonElement): IonElement {
            val tag = ionSymbol("struct")
            val fields = ionSexpOf(node.fields.map { visitRexOpStructField(it, nil) })
            return ionSexpOf(tag, type, fields)
        }

        override fun visitRexOpStructField(node: Rex.Op.Struct.Field, nil: IonElement): IonElement {
            val k = visitRex(node.k, nil)
            val v = visitRex(node.v, nil)
            return ionSexpOf(k, v)
        }

        override fun visitRexOpSelect(node: Rex.Op.Select, type: IonElement): IonElement {
            val tag = ionSymbol("select")
            val constructor = visitRex(node.constructor, nil)
            val rel = visitRel(node.rel, nil)
            return ionSexpOf(tag, type, constructor, rel)
        }

        override fun visitRexOpPivot(node: Rex.Op.Pivot, type: IonElement): IonElement {
            val tag = ionSymbol("pivot")
            val k = visitRex(node.key, nil)
            val v = visitRex(node.value, nil)
            val rel = visitRel(node.rel, nil)
            return ionSexpOf(tag, type, k, v, rel)
        }

        override fun visitRexOpCollToScalar(node: Rex.Op.CollToScalar, type: IonElement): IonElement {
            val tag = ionSymbol("coll_to_scalar")
            val subquery = visitRexOp(node.subquery.select, visitTypeRef(node.subquery.type, nil))
            return ionSexpOf(tag, type, subquery)
        }

        // Rel : ctx -> schema

        override fun visitRel(node: Rel, nil: IonElement): IonElement {
            val schema = ionSexpOf(node.schema.map { visitTypeRef(it.type, nil) })
            return visitRelOp(node.op, schema)
        }

        override fun visitRelOpScan(node: Rel.Op.Scan, schema: IonElement): IonElement {
            val tag = ionSymbol("scan")
            val rex = visitRex(node.rex, nil)
            return ionSexpOf(tag, schema, rex)
        }

        override fun visitRelOpScanIndexed(node: Rel.Op.ScanIndexed, schema: IonElement): IonElement {
            val tag = ionSymbol("scan_indexed")
            val rex = visitRex(node.rex, nil)
            return ionSexpOf(tag, schema, rex)
        }

        override fun visitRelOpUnpivot(node: Rel.Op.Unpivot, schema: IonElement): IonElement {
            val tag = ionSymbol("unpivot")
            val rex = visitRex(node.rex, nil)
            return ionSexpOf(tag, schema, rex)
        }

        override fun visitRelOpProject(node: Rel.Op.Project, schema: IonElement): IonElement {
            val tag = ionSymbol("project")
            val items = ionSexpOf(node.projections.map { visitRex(it, nil) })
            val input = visitRel(node.input, nil)
            return ionSexpOf(tag, schema, items, input)
        }

        override fun visitRelOpFilter(node: Rel.Op.Filter, schema: IonElement): IonElement {
            val tag = ionSymbol("filter")
            val predicate = visitRex(node.predicate, nil)
            val input = visitRel(node.input, nil)
            return ionSexpOf(tag, schema, predicate, input)
        }

        override fun visitRelOpLimit(node: Rel.Op.Limit, schema: IonElement): IonElement {
            val tag = ionSymbol("limit")
            val limit = visitRex(node.limit, nil)
            val input = visitRel(node.input, nil)
            return ionSexpOf(tag, schema, limit, input)
        }

        override fun visitRelOpOffset(node: Rel.Op.Offset, schema: IonElement): IonElement {
            val tag = ionSymbol("offset")
            val offset = visitRex(node.offset, nil)
            val input = visitRel(node.input, nil)
            return ionSexpOf(tag, schema, offset, input)
        }

        override fun visitRelOpUnion(node: Rel.Op.Union, schema: IonElement): IonElement {
            val tag = ionSymbol("union")
            val lhs = visitRel(node.lhs, nil)
            val rhs = visitRel(node.rhs, nil)
            return ionSexpOf(tag, schema, lhs, rhs)
        }

        override fun visitRelOpIntersect(node: Rel.Op.Intersect, schema: IonElement): IonElement {
            val tag = ionSymbol("intersect")
            val lhs = visitRel(node.lhs, nil)
            val rhs = visitRel(node.rhs, nil)
            return ionSexpOf(tag, schema, lhs, rhs)
        }

        override fun visitRelOpExcept(node: Rel.Op.Except, schema: IonElement): IonElement {
            val tag = ionSymbol("except")
            val lhs = visitRel(node.lhs, nil)
            val rhs = visitRel(node.rhs, nil)
            return ionSexpOf(tag, schema, lhs, rhs)
        }

        // Helpers

        private fun Identifier.sql(): String = when (this) {
            is Identifier.Qualified -> steps.fold(root.sql()) { p, i -> "$p.${i.sql()}" }
            is Identifier.Symbol -> this.sql()
        }

        private fun Identifier.Symbol.sql(): String = when (this.caseSensitivity) {
            Identifier.CaseSensitivity.SENSITIVE -> "'$symbol\'"
            Identifier.CaseSensitivity.INSENSITIVE -> symbol
        }
    }
}
