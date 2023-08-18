package org.partiql.planner.typer

import org.partiql.errors.Problem
import org.partiql.errors.ProblemCallback
import org.partiql.errors.UNKNOWN_PROBLEM_LOCATION
import org.partiql.plan.Fn
import org.partiql.plan.Identifier
import org.partiql.plan.Plan
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.Statement
import org.partiql.plan.util.PlanRewriter
import org.partiql.planner.Env
import org.partiql.planner.FnMatch
import org.partiql.planner.PlanningProblemDetails
import org.partiql.planner.ResolutionStrategy
import org.partiql.planner.ResolvedVar
import org.partiql.planner.TypeEnv
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.types.AnyOfType
import org.partiql.types.AnyType
import org.partiql.types.BagType
import org.partiql.types.CollectionType
import org.partiql.types.IntType
import org.partiql.types.ListType
import org.partiql.types.SexpType
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import org.partiql.types.function.FunctionSignature
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.TextValue

/**
 * Rewrites an untyped algebraic translation of the query to be both typed and have resolved variables.
 *
 * @property env
 * @property onProblem
 */
internal class PlanTyper(
    private val env: Env,
    private val onProblem: ProblemCallback,
) {

    /**
     * Rewrite the statement with inferred types and resolved variables
     */
    public fun resolve(statement: Statement): Statement {
        if (statement !is Statement.Query) {
            throw IllegalArgumentException("PartiQLPlanner only supports Query statements")
        }
        // root TypeEnv has no bindings
        val typeEnv = TypeEnv(
            schema = emptyList(),
            strategy = ResolutionStrategy.GLOBAL,
        )
        val root = statement.root.type(typeEnv)
        return Plan.statementQuery(root)
    }

    /**
     * Use default factory for rewrites
     */
    private val factory = Plan

    private inline fun <T> rewrite(block: Plan.() -> T): T = block.invoke(factory)

    /**
     * Types the relational operators of a query expression.
     *
     * @property outer represents the outer TypeEnv of a query expression — only used by scan variable resolution.
     */
    private inner class RelTyper(private val outer: TypeEnv) : PlanRewriter<Rel.Type?>() {

        override fun visitRel(node: Rel, ctx: Rel.Type?) = super.visitRelOp(node.op, node.type) as Rel

        /**
         * The output schema of a `rel.op.scan` is the single value binding.
         */
        override fun visitRelOpScan(node: Rel.Op.Scan, ctx: Rel.Type?): Rel = rewrite {
            // descend, with GLOBAL resolution strategy
            val rex = node.rex.type(outer.global())
            // compute rel type
            val valueT = getElementTypeForFromSource(rex.type)
            val type = ctx!!.copyWithSchema(listOf(valueT))
            // rewrite
            val op = relOpScan(rex)
            rel(type, op)
        }

        /**
         * The output schema of a `rel.op.scan_index` is the value binding and index binding.
         */
        override fun visitRelOpScanIndexed(node: Rel.Op.ScanIndexed, ctx: Rel.Type?): Rel = rewrite {
            // descend, with GLOBAL resolution strategy
            val rex = node.rex.type(outer.global())
            // compute rel type
            val valueT = getElementTypeForFromSource(rex.type)
            val indexT = StaticType.INT
            val type = ctx!!.copyWithSchema(listOf(valueT, indexT))
            // rewrite
            val op = relOpScan(rex)
            rel(type, op)
        }

        override fun visitRelOpUnpivot(node: Rel.Op.Unpivot, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Unpivot")
        }

        override fun visitRelOpDistinct(node: Rel.Op.Distinct, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Distinct")
        }

        override fun visitRelOpFilter(node: Rel.Op.Filter, ctx: Rel.Type?): Rel = rewrite {
            // compute input schema
            val input = visitRel(node.input, ctx)
            // type sub-nodes
            val typeEnv = TypeEnv(input.type.schema, ResolutionStrategy.LOCAL)
            val predicate = node.predicate.type(typeEnv)
            // compute output schema
            val type = input.type
            // rewrite
            val op = relOpFilter(input, predicate)
            rel(type, op)
        }

        override fun visitRelOpSort(node: Rel.Op.Sort, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Sort")
        }

        override fun visitRelOpSortSpec(node: Rel.Op.Sort.Spec, ctx: Rel.Type?): Rel {
            TODO("Type RelOp SortSpec")
        }

        override fun visitRelOpUnion(node: Rel.Op.Union, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Union")
        }

        override fun visitRelOpIntersect(node: Rel.Op.Intersect, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Intersect")
        }

        override fun visitRelOpExcept(node: Rel.Op.Except, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Except")
        }

        override fun visitRelOpLimit(node: Rel.Op.Limit, ctx: Rel.Type?) = rewrite {
            // compute input schema
            val input = visitRel(node.input, ctx)
            // type limit expression using outer scope with global resolution
            val typeEnv = outer.global()
            val limit = node.limit.type(typeEnv)
            // compute output schema
            val type = input.type
            // rewrite
            val op = relOpLimit(input, limit)
            rel(type, op)
        }

        override fun visitRelOpOffset(node: Rel.Op.Offset, ctx: Rel.Type?) = rewrite {
            // compute input schema
            val input = visitRel(node.input, ctx)
            // type offset expression using outer scope with global resolution
            val typeEnv = outer.global()
            val offset = node.offset.type(typeEnv)
            // compute output schema
            val type = input.type
            // rewrite
            val op = relOpOffset(input, offset)
            rel(type, op)
        }

        override fun visitRelOpProject(node: Rel.Op.Project, ctx: Rel.Type?): Rel = rewrite {
            // compute input schema
            val input = visitRel(node.input, ctx)
            // type sub-nodes
            val typeEnv = TypeEnv(input.type.schema, ResolutionStrategy.LOCAL)
            val projections = node.projections.map { it.type(typeEnv) }
            // compute output schema
            val schema = projections.map { it.type }
            val type = input.type.copyWithSchema(schema)
            // rewrite
            val op = relOpProject(input, projections)
            rel(type, op)
        }

        override fun visitRelOpJoin(node: Rel.Op.Join, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Join")
        }

        override fun visitRelOpJoinTypeCross(node: Rel.Op.Join.Type.Cross, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Cross")
        }

        override fun visitRelOpJoinTypeEqui(node: Rel.Op.Join.Type.Equi, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Equi")
        }

        override fun visitRelOpJoinTypeTheta(node: Rel.Op.Join.Type.Theta, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Theta")
        }

        override fun visitRelOpAggregate(node: Rel.Op.Aggregate, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Aggregate")
        }

        override fun visitRelOpAggregateAgg(node: Rel.Op.Aggregate.Agg, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Agg")
        }

        override fun visitRelBinding(node: Rel.Binding, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Binding")
        }
    }

    /**
     * Types a PartiQL expression tree. For now, we ignore the pre-existing type. We assume all existing types
     * are simply the `any`, so we keep the new type. Ideally we can programmatically calculate the most specific type.
     *
     * We should consider making the StaticType? parameter non-nullable.
     *
     * @property locals TypeEnv in which this rex tree is evaluated.
     */
    private inner class RexTyper(private val locals: TypeEnv) : PlanRewriter<StaticType?>() {

        override fun visitRex(node: Rex, ctx: StaticType?): Rex = super.visitRexOp(node.op, node.type) as Rex

        override fun visitRexOpLit(node: Rex.Op.Lit, ctx: StaticType?): Rex = rewrite {
            // type comes from RexConverter
            rex(ctx!!, node)
        }

        override fun visitRexOpVarResolved(node: Rex.Op.Var.Resolved, ctx: StaticType?): Rex = rewrite {
            assert(node.ref < locals.schema.size) { "Invalid resolved variable (var ${node.ref}) for $locals" }
            val type = locals.schema[node.ref].type
            rex(type, node)
        }

        override fun visitRexOpVarUnresolved(node: Rex.Op.Var.Unresolved, ctx: StaticType?): Rex = rewrite {
            val path = node.identifier.toBindingPath()
            val resolvedVar = env.resolve(path, locals, node.scope)
            if (resolvedVar == null) {
                handleUndefinedVariable(path.steps.last())
                return rex(StaticType.ANY, node)
            }
            val type = resolvedVar.type
            val op = when (resolvedVar) {
                is ResolvedVar.Global -> rexOpGlobal(resolvedVar.ordinal)
                is ResolvedVar.Local -> rexOpVarResolved(resolvedVar.ordinal)
            }
            rex(type, op)
        }

        override fun visitRexOpGlobal(node: Rex.Op.Global, ctx: StaticType?): Rex = rewrite {
            val global = env.globals[node.ref]
            val type = global.type
            rex(type, node)
        }

        /**
         * Match path as far as possible (rewriting the steps), then infer based on resolved root and rewritten steps.
         */
        override fun visitRexOpPath(node: Rex.Op.Path, ctx: StaticType?): Rex = rewrite {
            // 1. Resolve path prefix
            val (root, steps) = when (val rootOp = node.root.op) {
                is Rex.Op.Var.Unresolved -> {
                    // Rewrite the root
                    val path = rexPathToBindingPath(rootOp, node.steps)
                    val resolvedVar = env.resolve(path, locals, rootOp.scope)
                    if (resolvedVar == null) {
                        handleUndefinedVariable(path.steps.last())
                        return rex(StaticType.ANY, node)
                    }
                    val type = resolvedVar.type
                    val (op, steps) = when (resolvedVar) {
                        is ResolvedVar.Local -> {
                            // Root was a local; replace just the root
                            rexOpVarResolved(resolvedVar.ordinal) to node.steps
                        }
                        is ResolvedVar.Global -> {
                            // Root (and some steps) was a global; replace root and re-calculate remaining steps.
                            val remainingFirstIndex = resolvedVar.depth - 1
                            val remaining = when (remainingFirstIndex > node.steps.lastIndex) {
                                true -> emptyList()
                                false -> node.steps.subList(remainingFirstIndex, node.steps.size)
                            }
                            rexOpGlobal(resolvedVar.ordinal) to remaining
                        }
                    }
                    // rewrite root
                    rex(type, op) to steps
                }
                else -> node.root to node.steps
            }
            // 2. Evaluate remaining path steps
            val type = steps.fold(root.type) { type, step ->
                when (step) {
                    is Rex.Op.Path.Step.Index -> inferPathStepType(type, step)
                    is Rex.Op.Path.Step.Unpivot -> type
                    is Rex.Op.Path.Step.Wildcard -> error("Wildcard path type inference implemented")
                }
            }
            rex(type, rexOpPath(root, steps))
        }

        override fun visitRexOpCall(node: Rex.Op.Call, ctx: StaticType?): Rex = rewrite {
            val fn = node.fn
            val args = node.args.map { visitCallArg(it) }
            // Already resolved; unreachable but handle gracefully.
            if (fn is Fn.Resolved) {
                val type = fn.signature.returns.toStaticType()
                return rex(type, rexOpCall(fn, args))
            }
            when (val match = env.resolveFn(fn as Fn.Unresolved, args)) {
                is FnMatch.Ok -> {
                    val newFn = fnResolved(match.signature)
                    val newArgs = rewriteFnArgs(match.mapping, args)
                    val type = match.signature.returns.toStaticType()
                    val op = rexOpCall(newFn, newArgs)
                    rex(type, op)
                }
                is FnMatch.Error -> {
                    handleUnknownFunction(match)
                    rex(StaticType.ANY, rexOpErr())
                }
            }
        }

        // TODO https://github.com/partiql/partiql-lang-kotlin/issues/1179
        private fun visitCallArg(node: Rex.Op.Call.Arg): Rex.Op.Call.Arg = when (node) {
            is Rex.Op.Call.Arg.Type -> visitRexOpCallArgType(node, null) as Rex.Op.Call.Arg
            is Rex.Op.Call.Arg.Value -> visitRexOpCallArgValue(node, null) as Rex.Op.Call.Arg
        }

        override fun visitRexOpCase(node: Rex.Op.Case, ctx: StaticType?): Rex {
            TODO("Type RexOpCase")
        }

        override fun visitRexOpCaseBranch(node: Rex.Op.Case.Branch, ctx: StaticType?): Rex {
            TODO("Type RexOpCaseBranch")
        }

        override fun visitRexOpCollection(node: Rex.Op.Collection, ctx: StaticType?): Rex = rewrite {
            if (ctx!! !is CollectionType) {
                handleUnexpectedType(ctx, setOf(StaticType.LIST, StaticType.BAG, StaticType.SEXP))
                return rex(ctx, rexOpErr())
            }
            val values = node.values.map { visitRex(it, null) }
            val t = values.toUnionType()
            val type = when (ctx as CollectionType) {
                is BagType -> BagType(t)
                is ListType -> ListType(t)
                is SexpType -> SexpType(t)
            }
            rex(type, rexOpCollection(values))
        }

        @OptIn(PartiQLValueExperimental::class)
        override fun visitRexOpStruct(node: Rex.Op.Struct, ctx: StaticType?): Rex = rewrite {
            val fields = node.fields.map {
                val k = visitRex(it.k, null)
                val v = visitRex(it.v, null)
                rexOpStructField(k, v)
            }
            var structIsClosed = true
            val structTypeFields = mutableListOf<StructType.Field>()
            val structKeysSeent = mutableSetOf<String>()
            for (field in fields) {
                when (field.k.op) {
                    is Rex.Op.Lit -> {
                        // A field is only included in the StructType if its key is a text literal
                        val key = field.k.op as Rex.Op.Lit
                        if (key.value is TextValue<*>) {
                            val name = (key.value as TextValue<*>).string
                            val type = field.v.type
                            structKeysSeent.add(name)
                            structTypeFields.add(StructType.Field(name, type))
                        }
                    }
                    else -> {
                        if (field.k.type.allTypes.any { it.isText() }) {
                            // If the non-literal could be text, StructType will have open content.
                            structIsClosed = false
                        } else {
                            // A field with a non-literal key name is not included in the StructType.
                        }
                    }
                }
            }
            val type = StructType(
                fields = structTypeFields,
                contentClosed = structIsClosed,
                constraints = setOf(
                    TupleConstraint.Open(!structIsClosed),
                    TupleConstraint.UniqueAttrs(structKeysSeent.size == fields.size)
                ),
            )
            rex(type, rexOpStruct(fields))
        }

        override fun visitRexOpPivot(node: Rex.Op.Pivot, ctx: StaticType?): Rex {
            TODO("Type RexOpPivot")
        }

        override fun visitRexOpCollToScalar(node: Rex.Op.CollToScalar, ctx: StaticType?): Rex {
            TODO("Type RexOpCollToScalar")
        }

        override fun visitRexOpCollToScalarSubquery(node: Rex.Op.CollToScalar.Subquery, ctx: StaticType?): Rex {
            TODO("Type RexOpCollToScalarSubquery")
        }

        override fun visitRexOpSelect(node: Rex.Op.Select, ctx: StaticType?): Rex = rewrite {
            val rel = node.rel.type(locals)
            val typeEnv = TypeEnv(rel.type.schema, ResolutionStrategy.LOCAL)
            val constructor = node.constructor.type(typeEnv)
            val type = when (rel.isOrdered()) {
                true -> ListType(constructor.type)
                else -> BagType(constructor.type)
            }
            rex(type, rexOpSelect(constructor, rel))
        }

        override fun visitRexOpTupleUnion(node: Rex.Op.TupleUnion, ctx: StaticType?): Rex = rewrite {
            val args = node.args.map { visitTupleUnionArg(it) }
            val structFields = mutableListOf<StructType.Field>()
            var structIsClosed = true
            for (arg in args) {
                when (arg) {
                    is Rex.Op.TupleUnion.Arg.Spread -> {
                        val t = arg.v.type
                        if (t is StructType) {
                            // arg is definitely a struct
                            structFields.addAll(t.fields)
                            structIsClosed = structIsClosed && t.contentClosed
                        } else if (t.allTypes.filterIsInstance<StructType>().isNotEmpty()) {
                            // arg is possibly a struct, just declare OPEN content
                            structIsClosed = false
                        } else {
                            // arg is definitely NOT a struct
                            val field = StructType.Field(arg.k, arg.v.type)
                            structFields.add(field)
                        }
                    }
                    is Rex.Op.TupleUnion.Arg.Struct -> {
                        val field = StructType.Field(arg.k, arg.v.type)
                        structFields.add(field)
                    }
                }
            }
            val type = StructType(
                fields = structFields,
                contentClosed = structIsClosed,
                constraints = setOf(
                    TupleConstraint.Open(!structIsClosed),
                    TupleConstraint.UniqueAttrs(structFields.size == structFields.map { it.key }.distinct().size),
                    TupleConstraint.Ordered,
                ),
            )
            val op = rexOpTupleUnion(args)
            rex(type, op)
        }

        private fun visitTupleUnionArg(node: Rex.Op.TupleUnion.Arg) = when (node) {
            is Rex.Op.TupleUnion.Arg.Spread -> visitRexOpTupleUnionArgSpread(node, null)
            is Rex.Op.TupleUnion.Arg.Struct -> visitRexOpTupleUnionArgStruct(node, null)
        }

        override fun visitRexOpTupleUnionArgStruct(
            node: Rex.Op.TupleUnion.Arg.Struct,
            ctx: StaticType?,
        ) = super.visitRexOpTupleUnionArgStruct(node, ctx) as Rex.Op.TupleUnion.Arg

        override fun visitRexOpTupleUnionArgSpread(
            node: Rex.Op.TupleUnion.Arg.Spread,
            ctx: StaticType?,
        ) = super.visitRexOpTupleUnionArgSpread(node, ctx) as Rex.Op.TupleUnion.Arg

        // Helpers

        // TODO remove env
        private fun inferPathStepType(type: StaticType, step: Rex.Op.Path.Step.Index): StaticType = when (type) {
            is AnyType -> StaticType.ANY
            is StructType -> inferStructLookupType(type, step).flatten()
            is ListType,
            is SexpType,
            -> {
                val previous = type as CollectionType
                val key = visitRex(step.key, null)
                if (key.type is IntType) {
                    previous.elementType
                } else {
                    StaticType.MISSING
                }
            }
            is AnyOfType -> {
                when (type.types.size) {
                    0 -> throw IllegalStateException("Cannot path on an empty StaticType union")
                    else -> {
                        val prevTypes = type.allTypes
                        if (prevTypes.any { it is AnyType }) {
                            StaticType.ANY
                        } else {
                            val staticTypes = prevTypes.map { inferPathStepType(it, step) }
                            AnyOfType(staticTypes.toSet()).flatten()
                        }
                    }
                }
            }
            else -> StaticType.MISSING
        }

        // TODO remove env
        @OptIn(PartiQLValueExperimental::class)
        private fun inferStructLookupType(struct: StructType, step: Rex.Op.Path.Step.Index): StaticType =
            when (val key = step.key.op) {
                is Rex.Op.Lit -> {
                    if (key.value is TextValue<*>) {
                        val name = (key.value as TextValue<*>).string
                        val case = BindingCase.SENSITIVE
                        val binding = BindingName(name, case)
                        env.inferStructLookup(struct, binding)
                            ?: when (struct.contentClosed) {
                                true -> StaticType.MISSING
                                false -> StaticType.ANY
                            }
                    } else {
                        // Should this branch result in an error?
                        StaticType.MISSING
                    }
                }
                else -> {
                    StaticType.MISSING
                }
            }
    }

    // HELPERS

    private fun Rel.type(typeEnv: TypeEnv): Rel = RelTyper(typeEnv).visitRel(this, null)

    private fun Rex.type(typeEnv: TypeEnv) = RexTyper(typeEnv).visitRex(this, null)

    /**
     * I found decorating the tree with the binding names (for resolution) was easier than associating introduced
     * bindings with a node via an id->list<string> map. ONLY because right now I don't think we have a good way
     * of managing ids when trees are rewritten.
     *
     * We need a good answer for these questions before going for it:
     * - If you copy, should the id should come along for the ride?
     * - If someone writes their own pass and forgets to copy the id, then resolution could break.
     *
     * We may be able to eliminate this issue by keeping everything internal and running the typing pass first.
     * This is simple enough for now.
     */
    private fun Rel.Type.copyWithSchema(types: List<StaticType>): Rel.Type {
        assert(types.size == schema.size) { "Illegal copy, types size does not matching bindings list size" }
        return this.copy(
            schema = schema.mapIndexed { i, binding -> binding.copy(type = types[i]) }
        )
    }

    private fun Identifier.toBindingPath() = when (this) {
        is Identifier.Qualified -> this.toBindingPath()
        is Identifier.Symbol -> BindingPath(listOf(this.toBindingName()))
    }

    private fun Identifier.Qualified.toBindingPath() = BindingPath(
        steps = steps.map { it.toBindingName() }
    )

    private fun Identifier.Symbol.toBindingName() = BindingName(
        name = symbol,
        bindingCase = when (caseSensitivity) {
            Identifier.CaseSensitivity.SENSITIVE -> BindingCase.SENSITIVE
            Identifier.CaseSensitivity.INSENSITIVE -> BindingCase.INSENSITIVE
        }
    )

    private fun Rel.isOrdered(): Boolean = type.props.contains(Rel.Prop.ORDERED)

    /**
     * Produce a union type from all the
     */
    private fun List<Rex>.toUnionType(): StaticType = AnyOfType(map { it.type }.toSet()).flatten()

    /**
     * Helper function which returns the literal string/symbol steps of a path expression as a [BindingPath].
     */
    @OptIn(PartiQLValueExperimental::class)
    private fun rexPathToBindingPath(rootOp: Rex.Op.Var.Unresolved, steps: List<Rex.Op.Path.Step>): BindingPath {
        if (rootOp.identifier !is Identifier.Symbol) {
            throw IllegalArgumentException("Expected identifier symbol")
        }
        val bindingRoot = (rootOp.identifier as Identifier.Symbol).toBindingName()
        val bindingSteps = mutableListOf(bindingRoot)
        for (step in steps) {
            if (step is Rex.Op.Path.Step.Index && step.key.op is Rex.Op.Lit) {
                val v = (step.key.op as Rex.Op.Lit).value
                if (v is TextValue<*>) {
                    // add to prefix
                    bindingSteps.add(BindingName(v.string, BindingCase.SENSITIVE))
                } else {
                    // short-circuit
                    break
                }
            } else {
                // short-circuit
                break
            }
        }
        return BindingPath(bindingSteps)
    }

    private fun getElementTypeForFromSource(fromSourceType: StaticType): StaticType =
        when (fromSourceType) {
            is BagType -> fromSourceType.elementType
            is ListType -> fromSourceType.elementType
            is AnyType -> StaticType.ANY
            is AnyOfType -> AnyOfType(fromSourceType.types.map { getElementTypeForFromSource(it) }.toSet())
            // All the other types coerce into a bag of themselves (including null/missing/sexp).
            else -> fromSourceType
        }

    /**
     * Rewrites function arguments, wrapping in the given function if exists.
     */
    private fun Plan.rewriteFnArgs(
        mapping: List<FunctionSignature?>,
        args: List<Rex.Op.Call.Arg>,
    ): List<Rex.Op.Call.Arg> {
        if (mapping.size != args.size) {
            error("Fatal, malformed function mapping") // should be unreachable given how a mapping is generated.
        }
        val newArgs = mutableListOf<Rex.Op.Call.Arg>()
        for (i in mapping.indices) {
            val a = args[i]
            val m = mapping[i]
            if (m == null || a is Rex.Op.Call.Arg.Type) {
                newArgs.add(a)
            } else {
                val vArg = (a as Rex.Op.Call.Arg.Value).rex
                val tArg = m.returns.toStaticType()
                val cast = rexOpCall(
                    fn = fnResolved(m),
                    args = listOf(rexOpCallArgValue(vArg), rexOpCallArgType(tArg)),
                )
                val rex = rex(tArg, cast)
                rexOpCallArgValue(rex)
            }
        }
        return newArgs
    }

    // ERRORS

    private fun handleUndefinedVariable(name: BindingName) {
        onProblem(
            Problem(
                sourceLocation = UNKNOWN_PROBLEM_LOCATION,
                details = PlanningProblemDetails.UndefinedVariable(name.name, name.bindingCase == BindingCase.SENSITIVE)
            )
        )
    }

    private fun handleUnexpectedType(actual: StaticType, expected: Set<StaticType>) {
        onProblem(
            Problem(
                sourceLocation = UNKNOWN_PROBLEM_LOCATION,
                details = PlanningProblemDetails.UnexpectedType(actual, expected),
            )
        )
    }

    private fun handleUnknownFunction(match: FnMatch.Error) {
        onProblem(
            Problem(
                sourceLocation = UNKNOWN_PROBLEM_LOCATION,
                details = PlanningProblemDetails.UnknownFunction(
                    match.fn.identifier,
                    match.candidates,
                    match.args.map { a ->
                        when (a) {
                            is Rex.Op.Call.Arg.Type -> a.type
                            is Rex.Op.Call.Arg.Value -> a.rex.type
                        }
                    }
                )
            )
        )
    }
}
