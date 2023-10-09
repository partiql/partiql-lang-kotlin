package org.partiql.transpiler.sql

import org.partiql.ast.Ast
import org.partiql.ast.Expr
import org.partiql.ast.Identifier
import org.partiql.ast.Select
import org.partiql.ast.SetQuantifier
import org.partiql.plan.Fn
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
        val identifier = Ast.translate(node.identifier)
        val debug = identifier.sql()
        transform.handleProblem(
            TranspilerProblem(
                level = TranspilerProblem.Level.ERROR, message = "Unresolved variable $debug",
            )
        )
        val scope = when (node.scope) {
            Rex.Op.Var.Scope.DEFAULT -> Expr.Var.Scope.DEFAULT
            Rex.Op.Var.Scope.LOCAL -> Expr.Var.Scope.LOCAL
        }
        return Ast.exprVar(identifier, scope)
    }

    override fun visitRexOpGlobal(node: Rex.Op.Global, ctx: StaticType): Expr {
        val global = transform.getGlobal(node.ref)
            ?: error("Malformed plan, resolved global (\$global ${node.ref}) does not exist")
        val identifier = global
        val scope = Expr.Var.Scope.DEFAULT
        return Ast.exprVar(identifier, scope)
    }

    override fun visitRexOpPath(node: Rex.Op.Path, ctx: StaticType): Expr {
        val root = visitRex(node.root, StaticType.ANY)
        val steps = node.steps.map { visitRexOpPathStep(it) }
        return Ast.exprPath(root, steps)
    }

    @OptIn(PartiQLValueExperimental::class)
    override fun visitRexOpTupleUnion(node: Rex.Op.TupleUnion, ctx: StaticType): Expr {
        return Ast.create {
            val args = node.args.map { arg -> visitRex(arg, ctx) }
            exprCall(
                identifierSymbol("TUPLEUNION", Identifier.CaseSensitivity.INSENSITIVE),
                args = args
            )
        }
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

    override fun visitRexOpStruct(node: Rex.Op.Struct, ctx: StaticType): Expr {
        val fields = node.fields.map {
            Ast.exprStructField(
                name = visitRex(it.k, StaticType.ANY),
                value = visitRex(it.v, StaticType.ANY),
            )
        }
        return Ast.exprStruct(fields)
    }

    override fun visitRexOpCase(node: Rex.Op.Case, ctx: StaticType): Expr {
        val branches = node.branches.map {
            val condition = visitRex(it.condition, StaticType.ANY)
            val result = visitRex(it.rex, StaticType.ANY)
            condition to result
        }.map {
            Ast.exprCaseBranch(condition = it.first, expr = it.second)
        }
        if (branches.isEmpty()) {
            transform.handleProblem(
                TranspilerProblem(
                    TranspilerProblem.Level.ERROR,
                    "No CASE-WHEN-THEN branches to convert to AST."
                )
            )
        }
        return Ast.exprCase(expr = null, branches = branches, default = null)
    }

    override fun visitRexOpCall(node: Rex.Op.Call, ctx: StaticType): Expr {
        val name = when (val f = node.fn) {
            is Fn.Resolved -> f.signature.name
            is Fn.Unresolved -> {
                val id = Ast.translate(f.identifier)
                assert(id is Identifier.Symbol) { "Functions with qualified identifiers are currently not supported" }
                transform.handleProblem(
                    TranspilerProblem(
                        level = TranspilerProblem.Level.ERROR,
                        message = "Could not resolve function ${id.sql()}"
                    )
                )
                (id as Identifier.Symbol).symbol
            }
        }
        val args = node.args.map { SqlArg(visitRex(it, StaticType.ANY), it.type) }
        return transform.getFunction(name, args)
    }

    override fun visitRexOpSelect(node: Rex.Op.Select, ctx: StaticType): Expr {
        val typeEnv = node.rel.type.schema
        val relToSql = RelToSql(transform)
        val rexToSql = RexToSql(transform, typeEnv)
        val sfw = relToSql.apply(node.rel)
        assert(sfw.select != null) { "SELECT from RelToSql should never be null" }
        val setq = getSetQuantifier(sfw.select!!)
        val select = convertSelectValueToSqlSelect(node.constructor, node.rel, setq)
            ?: convertSelectValue(node.constructor, node.rel, setq)
            ?: Ast.create { selectValue(rexToSql.apply(node.constructor), setq) }
        sfw.select = select
        return sfw.build()
    }

    /**
     * Grabs the [SetQuantifier] of a [Select].
     */
    private fun getSetQuantifier(select: Select): SetQuantifier? = when (select) {
        is Select.Project -> select.setq
        is Select.Value -> select.setq
        is Select.Star -> select.setq
        is Select.Pivot -> null
    }

    /**
     * Attempts to convert a SELECT VALUE to a SELECT LIST. Returns NULL if unable.
     *
     * For SELECT VALUE <v> FROM queries, the <v> gets pushed into the PROJECT, and it gets replaced with a variable
     * reference to the single projection. Therefore, if there was a SELECT * (which gets converted into a SELECT VALUE TUPLEUNION),
     * then the TUPLEUNION will be placed in the [Rel.Op.Project], and the [Rex.Op.Select.constructor] will be a
     * [Rex.Op.Var.Resolved] referencing the single projection. With that in mind, we can reconstruct the AST by looking
     * at the [constructor]. If it's a [Rex.Op.Var.Resolved], and it references a literal [Rex.Op.Struct], we can pull
     * out those struct's attributes and create a SQL-style select.
     *
     * NOTE: [Rex.Op.TupleUnion]'s get constant folded in the [org.partiql.planner.typer.PlanTyper].
     *
     * Example:
     * ```
     * SELECT t.* FROM t
     * -- Gets converted into
     * SELECT VALUE TUPLEUNION({ 'a': <INT>, 'b': <DECIMAL> }) FROM t
     * -- Gets constant folded (in the PlanTyper) into:
     * SELECT VALUE { 'a': <INT>, 'b': <DECIMAL> } FROM t
     * -- Gets converted into:
     * SELECT VALUE $__x
     * -> PROJECT < $__x: TUPLEUNION(...) >
     * -> SCAN t
     * -- Gets converted into:
     * SELECT a, b, c FROM t
     * ```
     *
     * If unable to convert into SQL-style projections (due to open content structs, non-struct arguments, etc), we
     * return null.
     */
    @OptIn(PartiQLValueExperimental::class)
    private fun convertSelectValueToSqlSelect(constructor: Rex, input: Rel, setq: SetQuantifier?): Select? {
        val relProject = input.op as? Rel.Op.Project ?: return null
        val structOp = getConstructorFromProjection(constructor, relProject)?.op as? Rex.Op.Struct ?: return null
        val newRexToSql = RexToSql(transform, relProject.input.type.schema)
        val projections = structOp.fields.map { field ->
            val key = field.k.op
            if (key !is Rex.Op.Lit || key.value !is StringValue) { return null }
            val fieldName = (key.value as StringValue).value ?: return null
            Ast.create {
                selectProjectItemExpression(expr = newRexToSql.apply(field.v), asAlias = id(fieldName))
            }
        }
        return Ast.create {
            selectProject(
                items = projections,
                setq = setq
            )
        }
    }

    /**
     * Since the <v> in SELECT VALUE <v> gets pulled into the [Rel.Op.Project], we attempt to recuperate the [Rex] and
     * convert it to SQL. If we are unable to, return NULL.
     */
    @OptIn(PartiQLValueExperimental::class)
    private fun convertSelectValue(constructor: Rex, input: Rel, setq: SetQuantifier?): Select? {
        val relProject = input.op as? Rel.Op.Project ?: return null
        val projection = getConstructorFromProjection(constructor, relProject) ?: return null
        val rexToSql = RexToSql(transform, relProject.input.type.schema)
        return Ast.create { selectValue(rexToSql.apply(projection), setq) }
    }

    /**
     * Grabs the first projection from [Rel.Op.Project] if the [constructor] is referencing it. If unable to
     * grab, return null.
     */
    private fun getConstructorFromProjection(constructor: Rex, relProject: Rel.Op.Project): Rex? {
        val constructorOp = constructor.op as? Rex.Op.Var.Resolved ?: return null
        if (constructorOp.ref != 0) { return null }
        if (relProject.projections.size != 1) { return null }
        return relProject.projections[0]
    }

    private fun id(symbol: String): Identifier.Symbol = Ast.identifierSymbol(
        symbol = symbol,
        caseSensitivity = Identifier.CaseSensitivity.INSENSITIVE,
    )

    private fun TypeEnv.dump(): String {
        val pairs = this.joinToString { "${it.name}: ${it.type}" }
        return "< $pairs >"
    }

    internal fun Identifier.sql(): String = when (this) {
        is Identifier.Qualified -> (listOf(this.root.sql()) + this.steps.map { it.sql() }).joinToString(".")
        is Identifier.Symbol -> when (this.caseSensitivity) {
            Identifier.CaseSensitivity.SENSITIVE -> "\"$symbol\""
            Identifier.CaseSensitivity.INSENSITIVE -> symbol
        }
    }
}
