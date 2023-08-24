package org.partiql.transpiler.sql

import org.partiql.ast.Ast
import org.partiql.ast.Expr
import org.partiql.ast.Identifier
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.visitor.PlanBaseVisitor
import org.partiql.transpiler.TranspilerProblem
import org.partiql.transpiler.sql.SqlTransform.Companion.translate
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue

/**
 * Local scope.
 */
public typealias TypeEnv = List<Rel.Binding>

/**
 * RexToSql translates a [Rex] tree in the given local scope.
 */
public open class RexToSql(
    private val transform: SqlTransform,
    private val locals: TypeEnv,
) : PlanBaseVisitor<Expr, StaticType>() {

    /**
     * Convert a [Rex] to an [Expr].
     */
    public fun apply(rex: Rex): Expr = rex.accept(this, StaticType.ANY)

    /**
     * Default behavior is considered unsupported.
     */
    override fun defaultReturn(node: PlanNode, ctx: StaticType): Expr =
        throw UnsupportedOperationException("Cannot translate rex $node")

    override fun defaultVisit(node: PlanNode, ctx: StaticType) = defaultReturn(node, ctx)

    /**
     * Pass along the Rex [StaticType]
     */
    override fun visitRex(node: Rex, ctx: StaticType) = super.visitRexOp(node.op, ctx)

    @OptIn(PartiQLValueExperimental::class)
    override fun visitRexOpLit(node: Rex.Op.Lit, ctx: StaticType): Expr {
        return Ast.exprLit(node.value)
    }

    override fun visitRexOpVarResolved(node: Rex.Op.Var.Resolved, ctx: StaticType): Expr {
        val binding = locals.getOrNull(node.ref)
        if (binding == null) {
            error("Malformed plan, resolved local (\$var ${node.ref}) not in ${locals.dump()}")
        }
        val identifier = id(binding.name)
        val scope = Expr.Var.Scope.DEFAULT
        return Ast.exprVar(identifier, scope)
    }

    override fun visitRexOpVarUnresolved(node: Rex.Op.Var.Unresolved, ctx: StaticType): Expr {
        transform.handleProblem(
            TranspilerProblem(
                level = TranspilerProblem.Level.ERROR, message = "Unresolved variable $node",
            )
        )
        val identifier = Ast.translate(node.identifier)
        val scope = when (node.scope) {
            Rex.Op.Var.Scope.DEFAULT -> Expr.Var.Scope.DEFAULT
            Rex.Op.Var.Scope.LOCAL -> Expr.Var.Scope.LOCAL
        }
        return Ast.exprVar(identifier, scope)
    }

    override fun visitRexOpGlobal(node: Rex.Op.Global, ctx: StaticType): Expr {
        val global = transform.getGlobal(node.ref)
        if (global == null) {
            error("Malformed plan, resolved global (\$global ${node.ref}) does not exist")
        }
        val identifier = global
        val scope = Expr.Var.Scope.DEFAULT
        return Ast.exprVar(identifier, scope)
    }

    override fun visitRexOpPath(node: Rex.Op.Path, ctx: StaticType): Expr {
        val root = visitRex(node.root, StaticType.ANY)
        val steps = node.steps.map { visitRexOpPathStep(it) }
        return Ast.exprPath(root, steps)
    }

    private fun visitRexOpPathStep(node: Rex.Op.Path.Step): Expr.Path.Step = when (node) {
        is Rex.Op.Path.Step.Index -> visitRexOpPathStepIndex(node)
        is Rex.Op.Path.Step.Unpivot -> Ast.exprPathStepUnpivot()
        is Rex.Op.Path.Step.Wildcard -> Ast.exprPathStepWildcard()
    }

    private fun visitRexOpPathStepIndex(node: Rex.Op.Path.Step.Index): Expr.Path.Step {
        val k = visitRex(node.key, StaticType.ANY)
        return Ast.exprPathStepIndex(k)
    }

    override fun visitRexOpSelect(node: Rex.Op.Select, ctx: StaticType): Expr {
        // val typeEnv = node.rel.type.schema
        val relToSql = RelToSql(transform)
        // val rexToSql = RexToSql(transform, typeEnv)
        val sfw = relToSql.apply(node.rel)
        assert(sfw.select != null) { "SELECT from RelToSql should never be null" }
        if (node.constructor.isDefault(node.rel.type.schema)) {
            // SELECT
            return sfw.build()
        } else {
            // SELECT VALUE
            // TODO rewrite the constructor replacing variable references with the projected expressions
            throw UnsupportedOperationException("SELECT VALUE is not supported")
        }
    }

    override fun visitRexOpStruct(node: Rex.Op.Struct, ctx: StaticType): Expr {
        val fields = node.fields.map {
            Ast.exprStructField(
                name = visitRex(it.k, StaticType.ANY),
                value = visitRex(it.v, StaticType.ANY),
            )
        }
        return Ast.exprStruct(fields)
    }

    private fun id(symbol: String): Identifier.Symbol = Ast.identifierSymbol(
        symbol = symbol,
        caseSensitivity = Identifier.CaseSensitivity.INSENSITIVE,
    )

    private fun TypeEnv.dump(): String {
        val pairs = this.joinToString { "${it.name}: ${it.type}" }
        return "< $pairs >"
    }

    /**
     * Returns true iff this [Rex] is the default constructor for [schema] derived from an SQL SELECT.
     *
     * See [RelConverter.defaultConstructor] to see how the default constructor is created.
     */
    @OptIn(PartiQLValueExperimental::class)
    private fun Rex.isDefault(schema: List<Rel.Binding>): Boolean {
        val op = this.op
        if (op !is Rex.Op.Struct) {
            return false
        }
        if (op.fields.size != schema.size) {
            // not everything is projected out
            return false
        }
        schema.forEachIndexed { i, binding ->
            val f = op.fields[i]
            val kOp = f.k.op
            val vOp = f.v.op
            // check types
            if (kOp !is Rex.Op.Lit || kOp.value !is StringValue) return false
            if (vOp !is Rex.Op.Var.Resolved) return false
            // check values
            if ((kOp.value as StringValue).value != binding.name) return false
            if (vOp.ref != i) return false
        }
        return true
    }
}
