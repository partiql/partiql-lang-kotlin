/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 *
 */

package org.partiql.planner.typer

import org.partiql.errors.Problem
import org.partiql.errors.ProblemCallback
import org.partiql.errors.UNKNOWN_PROBLEM_LOCATION
import org.partiql.plan.Agg
import org.partiql.plan.Fn
import org.partiql.plan.Identifier
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.Statement
import org.partiql.plan.aggResolved
import org.partiql.plan.fnResolved
import org.partiql.plan.identifierSymbol
import org.partiql.plan.rel
import org.partiql.plan.relBinding
import org.partiql.plan.relOpAggregateCall
import org.partiql.plan.relOpErr
import org.partiql.plan.relOpFilter
import org.partiql.plan.relOpJoin
import org.partiql.plan.relOpLimit
import org.partiql.plan.relOpOffset
import org.partiql.plan.relOpProject
import org.partiql.plan.relOpScan
import org.partiql.plan.relOpSort
import org.partiql.plan.relOpUnpivot
import org.partiql.plan.relType
import org.partiql.plan.rex
import org.partiql.plan.rexOpCall
import org.partiql.plan.rexOpCaseBranch
import org.partiql.plan.rexOpCollection
import org.partiql.plan.rexOpErr
import org.partiql.plan.rexOpGlobal
import org.partiql.plan.rexOpLit
import org.partiql.plan.rexOpPath
import org.partiql.plan.rexOpPathStepSymbol
import org.partiql.plan.rexOpSelect
import org.partiql.plan.rexOpStruct
import org.partiql.plan.rexOpStructField
import org.partiql.plan.rexOpTupleUnion
import org.partiql.plan.rexOpVarResolved
import org.partiql.plan.statementQuery
import org.partiql.plan.util.PlanRewriter
import org.partiql.planner.Env
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
import org.partiql.types.BoolType
import org.partiql.types.CollectionType
import org.partiql.types.IntType
import org.partiql.types.ListType
import org.partiql.types.MissingType
import org.partiql.types.NullType
import org.partiql.types.SexpType
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import org.partiql.types.function.FunctionSignature
import org.partiql.value.BoolValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.TextValue
import org.partiql.value.boolValue

/**
 * Rewrites an untyped algebraic translation of the query to be both typed and have resolved variables.
 *
 * @property env
 * @property onProblem
 */
@OptIn(PartiQLValueExperimental::class)
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
        return statementQuery(root)
    }

    /**
     * Types the relational operators of a query expression.
     *
     * @property outer represents the outer TypeEnv of a query expression — only used by scan variable resolution.
     */
    private inner class RelTyper(private val outer: TypeEnv) : PlanRewriter<Rel.Type?>() {

        override fun visitRel(node: Rel, ctx: Rel.Type?) = visitRelOp(node.op, node.type) as Rel

        /**
         * The output schema of a `rel.op.scan` is the single value binding.
         */
        override fun visitRelOpScan(node: Rel.Op.Scan, ctx: Rel.Type?): Rel {
            // descend, with GLOBAL resolution strategy
            val rex = node.rex.type(outer.global())
            // compute rel type
            val valueT = getElementTypeForFromSource(rex.type)
            val type = ctx!!.copyWithSchema(listOf(valueT))
            // rewrite
            val op = relOpScan(rex)
            return rel(type, op)
        }

        override fun visitRelOpErr(node: Rel.Op.Err, ctx: Rel.Type?): Rel {
            val type = ctx ?: relType(emptyList(), emptySet())
            return rel(type, node)
        }

        /**
         * The output schema of a `rel.op.scan_index` is the value binding and index binding.
         */
        override fun visitRelOpScanIndexed(node: Rel.Op.ScanIndexed, ctx: Rel.Type?): Rel {
            // descend, with GLOBAL resolution strategy
            val rex = node.rex.type(outer.global())
            // compute rel type
            val valueT = getElementTypeForFromSource(rex.type)
            val indexT = StaticType.INT
            val type = ctx!!.copyWithSchema(listOf(valueT, indexT))
            // rewrite
            val op = relOpScan(rex)
            return rel(type, op)
        }

        /**
         * TODO handle NULL|STRUCT type
         */
        override fun visitRelOpUnpivot(node: Rel.Op.Unpivot, ctx: Rel.Type?): Rel {
            // descend, with GLOBAL resolution strategy
            val rex = node.rex.type(outer.global())

            // only UNPIVOT a struct
            if (rex.type !is StructType) {
                handleUnexpectedType(rex.type, expected = setOf(StaticType.STRUCT))
                return rel(ctx!!, relOpErr("UNPIVOT on non-STRUCT type ${rex.type}"))
            }

            // compute element type
            val t = rex.type as StructType
            val e = if (t.contentClosed) {
                StaticType.unionOf(t.fields.map { it.value }.toSet()).flatten()
            } else {
                StaticType.ANY
            }

            // compute rel type
            val kType = StaticType.STRING
            val vType = e
            val type = ctx!!.copyWithSchema(listOf(kType, vType))

            // rewrite
            val op = relOpUnpivot(rex)
            return rel(type, op)
        }

        override fun visitRelOpDistinct(node: Rel.Op.Distinct, ctx: Rel.Type?): Rel {
            TODO("Type RelOp Distinct")
        }

        override fun visitRelOpFilter(node: Rel.Op.Filter, ctx: Rel.Type?): Rel {
            // compute input schema
            val input = visitRel(node.input, ctx)
            // type sub-nodes
            val typeEnv = TypeEnv(input.type.schema, ResolutionStrategy.LOCAL)
            val predicate = node.predicate.type(typeEnv)
            // compute output schema
            val type = input.type
            // rewrite
            val op = relOpFilter(input, predicate)
            return rel(type, op)
        }

        override fun visitRelOpSort(node: Rel.Op.Sort, ctx: Rel.Type?): Rel {
            // compute input schema
            val input = visitRel(node.input, ctx)
            // type sub-nodes
            val typeEnv = TypeEnv(input.type.schema, ResolutionStrategy.LOCAL)
            val specs = node.specs.map {
                val rex = it.rex.type(typeEnv)
                it.copy(rex)
            }
            // output schema of a sort is the same as the input
            val type = input.type.copy(props = setOf(Rel.Prop.ORDERED))
            // rewrite
            val op = relOpSort(input, specs)
            return rel(type, op)
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

        override fun visitRelOpLimit(node: Rel.Op.Limit, ctx: Rel.Type?): Rel {
            // compute input schema
            val input = visitRel(node.input, ctx)
            // type limit expression using outer scope with global resolution
            val typeEnv = outer.global()
            val limit = node.limit.type(typeEnv)
            // check types
            assertAsInt(limit.type)
            // compute output schema
            val type = input.type
            // rewrite
            val op = relOpLimit(input, limit)
            return rel(type, op)
        }

        override fun visitRelOpOffset(node: Rel.Op.Offset, ctx: Rel.Type?): Rel {
            // compute input schema
            val input = visitRel(node.input, ctx)
            // type offset expression using outer scope with global resolution
            val typeEnv = outer.global()
            val offset = node.offset.type(typeEnv)
            // check types
            assertAsInt(offset.type)
            // compute output schema
            val type = input.type
            // rewrite
            val op = relOpOffset(input, offset)
            return rel(type, op)
        }

        override fun visitRelOpProject(node: Rel.Op.Project, ctx: Rel.Type?): Rel {
            // compute input schema
            val input = visitRel(node.input, ctx)
            // type sub-nodes
            val typeEnv = TypeEnv(input.type.schema, ResolutionStrategy.LOCAL)
            val projections = node.projections.map {
                it.type(typeEnv)
            }
            // compute output schema
            val schema = projections.map { it.type }
            val type = ctx!!.copyWithSchema(schema)
            // rewrite
            val op = relOpProject(input, projections)
            return rel(type, op)
        }

        override fun visitRelOpJoin(node: Rel.Op.Join, ctx: Rel.Type?): Rel {
            // Rewrite LHS and RHS
            val lhs = visitRel(node.lhs, ctx)
            val rhs = visitRel(node.rhs, ctx)

            // Calculate output schema given JOIN type
            val l = lhs.type.schema
            val r = rhs.type.schema
            val schema = when (node.type) {
                Rel.Op.Join.Type.INNER -> l + r
                Rel.Op.Join.Type.LEFT -> l + r.pad()
                Rel.Op.Join.Type.RIGHT -> l.pad() + r
                Rel.Op.Join.Type.FULL -> l.pad() + r.pad()
            }
            val type = relType(schema, ctx!!.props)

            // Type the condition on the output schema
            val condition = node.rex.type(TypeEnv(type.schema, ResolutionStrategy.LOCAL))

            val op = relOpJoin(lhs, rhs, condition, node.type)
            return rel(type, op)
        }

        /**
         * Initial implementation of `EXCLUDE` schema inference. Until an RFC is finalized for `EXCLUDE`
         * (https://github.com/partiql/partiql-spec/issues/39),
         *
         * This behavior is considered experimental and subject to change.
         *
         * This implementation includes
         *  - Excluding tuple bindings (e.g. t.a.b.c)
         *  - Excluding tuple wildcards (e.g. t.a.*.b)
         *  - Excluding collection indexes (e.g. t.a[0].b -- behavior subject to change; see below discussion)
         *  - Excluding collection wildcards (e.g. t.a[*].b)
         *
         * There are still discussion points regarding the following edge cases:
         *  - EXCLUDE on a tuple attribute that doesn't exist -- give an error/warning?
         *      - currently no error
         *  - EXCLUDE on a tuple attribute that has duplicates -- give an error/warning? exclude one? exclude both?
         *      - currently excludes both w/ no error
         *  - EXCLUDE on a collection index as the last step -- mark element type as optional?
         *      - currently element type as-is
         *  - EXCLUDE on a collection index w/ remaining path steps -- mark last step's type as optional?
         *      - currently marks last step's type as optional
         *  - EXCLUDE on a binding tuple variable (e.g. SELECT ... EXCLUDE t FROM t) -- error?
         *      - currently a parser error
         *  - EXCLUDE on a union type -- give an error/warning? no-op? exclude on each type in union?
         *      - currently exclude on each union type
         *  - If SELECT list includes an attribute that is excluded, we could consider giving an error in PlanTyper or
         * some other semantic pass
         *      - currently does not give an error
         */
        override fun visitRelOpExclude(node: Rel.Op.Exclude, ctx: Rel.Type?): Rel {
            // compute input schema
            val input = visitRel(node.input, ctx)

            // apply exclusions to the input schema
            val init = input.type.schema.map { it.copy() }
            val schema = node.items.fold((init)) { bindings, item -> excludeBindings(bindings, item) }

            // rewrite
            val type = ctx!!.copy(schema)
            return rel(type, node)
        }

        override fun visitRelOpAggregate(node: Rel.Op.Aggregate, ctx: Rel.Type?): Rel {
            // compute input schema
            val input = visitRel(node.input, ctx)

            // type the calls and groups
            val typer = RexTyper(locals = TypeEnv(input.type.schema, ResolutionStrategy.LOCAL))

            // typing of aggregate calls is slightly more complicated because they are not expressions.
            val calls = node.calls.mapIndexed { i, call ->
                when (val agg = call.agg) {
                    is Agg.Resolved -> call to ctx!!.schema[i].type
                    is Agg.Unresolved -> typer.resolveAgg(agg, call.args)
                }
            }
            val groups = node.groups.map { typer.visitRex(it, null) }

            // Compute schema using order (calls...groups...)
            val schema = mutableListOf<StaticType>()
            schema += calls.map { it.second }
            schema += groups.map { it.type }

            // rewrite with typed calls and groups
            val type = ctx!!.copyWithSchema(schema)
            val op = node.copy(
                calls = calls.map { it.first },
                groups = groups,
            )
            return rel(type, op)
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
    @OptIn(PartiQLValueExperimental::class)
    private inner class RexTyper(private val locals: TypeEnv) : PlanRewriter<StaticType?>() {

        override fun visitRex(node: Rex, ctx: StaticType?): Rex = visitRexOp(node.op, node.type) as Rex

        override fun visitRexOpLit(node: Rex.Op.Lit, ctx: StaticType?): Rex {
            // type comes from RexConverter
            return rex(ctx!!, node)
        }

        override fun visitRexOpVarResolved(node: Rex.Op.Var.Resolved, ctx: StaticType?): Rex {
            assert(node.ref < locals.schema.size) { "Invalid resolved variable (var ${node.ref}) for $locals" }
            val type = locals.schema[node.ref].type
            return rex(type, node)
        }

        override fun visitRexOpVarUnresolved(node: Rex.Op.Var.Unresolved, ctx: StaticType?): Rex {
            val path = node.identifier.toBindingPath()
            val resolvedVar = env.resolve(path, locals, node.scope)

            if (resolvedVar == null) {
                handleUndefinedVariable(path.steps.last())
                return rex(StaticType.ANY, rexOpErr("Undefined variable ${node.identifier}"))
            }
            val type = resolvedVar.type
            val op = when (resolvedVar) {
                is ResolvedVar.Global -> rexOpGlobal(resolvedVar.ordinal)
                is ResolvedVar.Local -> resolvedLocalPath(resolvedVar)
            }
            return rex(type, op)
        }

        override fun visitRexOpGlobal(node: Rex.Op.Global, ctx: StaticType?): Rex {
            val global = env.globals[node.ref]
            val type = global.type
            return rex(type, node)
        }

        /**
         * Match path as far as possible (rewriting the steps), then infer based on resolved root and rewritten steps.
         */
        override fun visitRexOpPath(node: Rex.Op.Path, ctx: StaticType?): Rex {
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
                else -> visitRex(node.root, node.root.type) to node.steps
            }

            // short-circuit if whole path was matched
            if (steps.isEmpty()) {
                return root
            }

            // 2. TODO rewrite and type the steps containing expressions
            // val typedSteps = steps.map {
            //     if (it is Rex.Op.Path.Step.Index) {
            //         val key = visitRex(it.key, null)
            //         rexOpPathStepIndex(key)
            //     } else it
            // }

            // 3. Walk the steps to determine the path type.
            val type = steps.fold(root.type) { curr, step -> inferPathStep(curr, step) }

            // 4. Invalid path reference; always MISSING
            if (type == StaticType.MISSING) {
                handleAlwaysMissing()
                return rexErr("Unknown identifier $node")
            }

            // 5. Non-missing, root is resolved
            return rex(type, rexOpPath(root, steps))
        }

        /**
         * Resolve and type scalar function calls.
         *
         * @param node
         * @param ctx
         * @return
         */
        override fun visitRexOpCall(node: Rex.Op.Call, ctx: StaticType?): Rex {
            // Already resolved; unreachable but handle gracefully.
            if (node.fn is Fn.Resolved) return rex(ctx!!, node)

            // Type the arguments
            val fn = node.fn as Fn.Unresolved
            val isEq = fn.isEq()
            var missingArg = false
            val args = node.args.map {
                val arg = visitRex(it, null)
                if (arg.type == MissingType) missingArg = true
                arg
            }

            // 7.1 All functions return MISSING when one of their inputs is MISSING (except `=`)
            if (missingArg && !isEq) {
                handleAlwaysMissing()
                return rex(StaticType.MISSING, rexOpCall(fn, args))
            }

            // Try to match the arguments to functions defined in the catalog
            return when (val match = env.resolveFn(fn, args)) {
                is FnMatch.Ok -> {

                    // Found a match!
                    val newFn = fnResolved(match.signature)
                    val newArgs = rewriteFnArgs(match.mapping, args)
                    val returns = newFn.signature.returns

                    // Determine the nullability of the return type
                    var isNull = false // True iff NULL CALL and has a NULL arg
                    var isNullable = false // True iff NULL CALL and has a NULLABLE arg; or is a NULLABLE operator
                    if (newFn.signature.isNullCall) {
                        for (arg in newArgs) {
                            if (arg.type is NullType) {
                                isNull = true
                                break
                            }
                            if (arg.type.isNullable()) {
                                isNullable = true
                                break
                            }
                        }
                    }
                    isNullable = isNullable || newFn.signature.isNullable

                    // Return type with calculated nullability
                    var type = when {
                        isNull -> StaticType.NULL
                        isNullable -> returns.toStaticType()
                        else -> returns.toNonNullStaticType()
                    }

                    // Some operators can return MISSING during runtime
                    if (match.isMissable && !isEq) {
                        type = StaticType.unionOf(type, StaticType.MISSING)
                    }

                    // Finally, rewrite this node
                    val op = rexOpCall(newFn, newArgs)
                    rex(type, op)
                }
                is FnMatch.Error -> {
                    handleUnknownFunction(match)
                    rexErr("Unknown scalar function $fn")
                }
            }
        }

        override fun visitRexOpCase(node: Rex.Op.Case, ctx: StaticType?): Rex {
            // Type branches and prune branches known to never execute
            val newBranches = node.branches
                .map { visitRexOpCaseBranch(it, it.rex.type) }
                .filterNot { isLiteralBool(it.condition, false) }

            newBranches.forEach { branch ->
                if (canBeBoolean(branch.condition.type).not()) {
                    onProblem.invoke(
                        Problem(
                            UNKNOWN_PROBLEM_LOCATION,
                            PlanningProblemDetails.IncompatibleTypesForOp(branch.condition.type.allTypes, "CASE_WHEN")
                        )
                    )
                }
            }
            val default = visitRex(node.default, node.default.type)

            // Calculate final expression (short-circuit to first branch if the condition is always TRUE).
            val resultTypes = newBranches.map { it.rex }.map { it.type } + listOf(default.type)
            return when (newBranches.size) {
                0 -> default
                else -> when (isLiteralBool(newBranches[0].condition, true)) {
                    true -> newBranches[0].rex
                    false -> rex(type = StaticType.unionOf(resultTypes.toSet()).flatten(), node.copy(branches = newBranches, default = default))
                }
            }
        }

        private fun canBeBoolean(type: StaticType): Boolean {
            return type.flatten().allTypes.any {
                it is BoolType
            }
        }

        @OptIn(PartiQLValueExperimental::class)
        private fun isLiteralBool(rex: Rex, bool: Boolean): Boolean {
            val op = rex.op as? Rex.Op.Lit ?: return false
            val value = op.value as? BoolValue ?: return false
            return value.value == bool
        }

        /**
         * We need special handling for:
         * ```
         * CASE
         *   WHEN a IS STRUCT THEN a
         *   ELSE { 'a': a }
         * END
         * ```
         * When we type the above, if we know that `a` can be many different types (one of them being a struct),
         * then when we see the top-level `a IS STRUCT`, then we can assume that the `a` on the RHS is definitely a
         * struct. We handle this by using [foldCaseBranch].
         */
        override fun visitRexOpCaseBranch(node: Rex.Op.Case.Branch, ctx: StaticType?): Rex.Op.Case.Branch {
            val visitedCondition = visitRex(node.condition, node.condition.type)
            val visitedReturn = visitRex(node.rex, node.rex.type)
            return foldCaseBranch(visitedCondition, visitedReturn)
        }

        /**
         * This takes in a [Rex.Op.Case.Branch.condition] and [Rex.Op.Case.Branch.rex]. If the [condition] is a type check,
         * AKA `<var> IS STRUCT`, then this function will return a new [Rex.Op.Case.Branch] whose [Rex.Op.Case.Branch.rex]
         * assumes that the type will always be a struct. The [Rex.Op.Case.Branch.condition] will be replaced by a
         * boolean literal if it is KNOWN whether a branch will always/never execute. This can be used to prune the
         * branch in subsequent passes.
         *
         * TODO: Currently, this only folds type checking for STRUCTs. We need to add support for all other types.
         *
         * TODO: I added a check for [Rex.Op.Var.Resolved] as it seemed odd to replace a general expression like:
         *  `WHEN { 'a': { 'b': 1} }.a IS STRUCT THEN { 'a': { 'b': 1} }.a.b`. We can discuss this later, but I'm
         *  currently limiting the scope of this intentionally.
         */
        private fun foldCaseBranch(condition: Rex, result: Rex): Rex.Op.Case.Branch {
            val call = condition.op as? Rex.Op.Call ?: return rexOpCaseBranch(condition, result)
            val fn = call.fn as? Fn.Resolved ?: return rexOpCaseBranch(condition, result)
            if (fn.signature.name.equals("is_struct", ignoreCase = true).not()) { return rexOpCaseBranch(condition, result) }
            val ref = call.args.getOrNull(0) ?: error("IS STRUCT requires an argument.")
            val simplifiedCondition = when {
                ref.type.allTypes.all { it is StructType } -> rex(StaticType.BOOL, rexOpLit(boolValue(true)))
                ref.type.allTypes.none { it is StructType } -> rex(StaticType.BOOL, rexOpLit(boolValue(false)))
                else -> condition
            }

            // Replace the result's type
            val type = AnyOfType(ref.type.allTypes.filterIsInstance<StructType>().toSet())
            val replacementVal = ref.copy(type = type)
            val rex = when (ref.op is Rex.Op.Var.Resolved) {
                true -> RexReplacer.replace(result, ref, replacementVal)
                false -> result
            }
            return rexOpCaseBranch(simplifiedCondition, rex)
        }

        override fun visitRexOpCollection(node: Rex.Op.Collection, ctx: StaticType?): Rex {
            if (ctx!! !is CollectionType) {
                handleUnexpectedType(ctx, setOf(StaticType.LIST, StaticType.BAG, StaticType.SEXP))
                return rex(StaticType.NULL_OR_MISSING, rexOpErr("Expected collection type"))
            }
            val values = node.values.map { visitRex(it, it.type) }
            val t = values.toUnionType()
            val type = when (ctx as CollectionType) {
                is BagType -> BagType(t)
                is ListType -> ListType(t)
                is SexpType -> SexpType(t)
            }
            return rex(type, rexOpCollection(values))
        }

        @OptIn(PartiQLValueExperimental::class)
        override fun visitRexOpStruct(node: Rex.Op.Struct, ctx: StaticType?): Rex {
            val fields = node.fields.map {
                val k = visitRex(it.k, it.k.type)
                val v = visitRex(it.v, it.v.type)
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
                            val name = (key.value as TextValue<*>).string!!
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
            return rex(type, rexOpStruct(fields))
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

        override fun visitRexOpSelect(node: Rex.Op.Select, ctx: StaticType?): Rex {
            val rel = node.rel.type(locals)
            val typeEnv = TypeEnv(rel.type.schema, ResolutionStrategy.LOCAL)
            var constructor = node.constructor.type(typeEnv)
            var constructorType = constructor.type
            // add the ordered property to the constructor
            if (constructorType is StructType) {
                // TODO: We shouldn't need to copy the ordered constraint.
                constructorType = constructorType.copy(
                    constraints = constructorType.constraints + setOf(TupleConstraint.Ordered)
                )
                constructor = rex(constructorType, constructor.op)
            }
            val type = when (rel.isOrdered()) {
                true -> ListType(constructor.type)
                else -> BagType(constructor.type)
            }
            return rex(type, rexOpSelect(constructor, rel))
        }

        override fun visitRexOpTupleUnion(node: Rex.Op.TupleUnion, ctx: StaticType?): Rex {
            val args = node.args.map { visitRex(it, ctx) }
            val type = when (args.size) {
                0 -> StructType(
                    fields = emptyMap(),
                    contentClosed = true,
                    constraints = setOf(
                        TupleConstraint.Open(false),
                        TupleConstraint.UniqueAttrs(true),
                        TupleConstraint.Ordered
                    )
                )
                else -> {
                    val argTypes = args.map { it.type }
                    val potentialTypes = buildArgumentPermutations(argTypes).map { argumentList ->
                        calculateTupleUnionOutputType(argumentList)
                    }
                    StaticType.unionOf(potentialTypes.toSet()).flatten()
                }
            }
            val op = rexOpTupleUnion(args)
            return rex(type, op)
        }

        override fun visitRexOpErr(node: Rex.Op.Err, ctx: StaticType?): PlanNode {
            val type = ctx ?: StaticType.ANY
            return rex(type, node)
        }

        // Helpers

        /**
         * Given a list of [args], this calculates the output type of `TUPLEUNION(args)`. NOTE: This does NOT handle union
         * types intentionally. This function expects that all arguments be flattened, and, if need be, that you invoke
         * this function multiple times based on the permutations of arguments.
         *
         * The signature of TUPLEUNION is: (LIST<STRUCT>) -> STRUCT.
         *
         * If any of the arguments are NULL (or potentially NULL), we return NULL.
         * If any of the arguments are non-struct, we return MISSING.
         *
         * Now, assuming all the other arguments are STRUCT, then we compute the output based on a number of factors:
         * - closed content
         * - ordering
         * - unique attributes
         *
         * If all arguments are closed content, then the output is closed content.
         * If all arguments are ordered, then the output is ordered.
         * If all arguments contain unique attributes AND all arguments are closed AND no fields clash, the output has
         *  unique attributes.
         */
        private fun calculateTupleUnionOutputType(args: List<StaticType>): StaticType {
            val structFields = mutableListOf<StructType.Field>()
            var structAmount = 0
            var structIsClosed = true
            var structIsOrdered = true
            var uniqueAttrs = true
            val possibleOutputTypes = mutableListOf<StaticType>()
            args.forEach { arg ->
                when (arg) {
                    is StructType -> {
                        structAmount += 1
                        structFields.addAll(arg.fields)
                        structIsClosed = structIsClosed && arg.constraints.contains(TupleConstraint.Open(false))
                        structIsOrdered = structIsOrdered && arg.constraints.contains(TupleConstraint.Ordered)
                        uniqueAttrs = uniqueAttrs && arg.constraints.contains(TupleConstraint.UniqueAttrs(true))
                    }
                    is AnyOfType -> {
                        onProblem.invoke(
                            Problem(
                                UNKNOWN_PROBLEM_LOCATION,
                                PlanningProblemDetails.CompileError("TupleUnion wasn't normalized to exclude union types.")
                            )
                        )
                        possibleOutputTypes.add(StaticType.MISSING)
                    }
                    is NullType -> { return StaticType.NULL }
                    else -> { return StaticType.MISSING }
                }
            }
            uniqueAttrs = when {
                structIsClosed.not() && structAmount > 1 -> false
                else -> uniqueAttrs
            }
            uniqueAttrs = uniqueAttrs && (structFields.size == structFields.distinctBy { it.key }.size)
            val orderedConstraint = when (structIsOrdered) {
                true -> TupleConstraint.Ordered
                false -> null
            }
            val constraints = setOfNotNull(
                TupleConstraint.Open(!structIsClosed),
                TupleConstraint.UniqueAttrs(uniqueAttrs),
                orderedConstraint
            )
            return StructType(
                fields = structFields.map { it },
                contentClosed = structIsClosed,
                constraints = constraints
            )
        }

        /**
         * We are essentially making permutations of arguments that maintain the same initial ordering. For example,
         * consider the following args:
         * ```
         * [ 0 = UNION(INT, STRING), 1 = (DECIMAL, TIMESTAMP) ]
         * ```
         * This function will return:
         * ```
         * [
         *   [ 0 = INT, 1 = DECIMAL ],
         *   [ 0 = INT, 1 = TIMESTAMP ],
         *   [ 0 = STRING, 1 = DECIMAL ],
         *   [ 0 = STRING, 1 = TIMESTAMP ]
         * ]
         * ```
         *
         * Essentially, this becomes useful specifically in the case of TUPLEUNION, since we can make sure that
         * the ordering of argument's attributes remains the same. For example:
         * ```
         * TUPLEUNION( UNION(STRUCT(a, b), STRUCT(c)), UNION(STRUCT(d, e), STRUCT(f)) )
         * ```
         *
         * Then, the output of the tupleunion will have the output types of all of the below:
         * ```
         * TUPLEUNION(STRUCT(a,b), STRUCT(d,e)) --> STRUCT(a, b, d, e)
         * TUPLEUNION(STRUCT(a,b), STRUCT(f)) --> STRUCT(a, b, f)
         * TUPLEUNION(STRUCT(c), STRUCT(d,e)) --> STRUCT(c, d, e)
         * TUPLEUNION(STRUCT(c), STRUCT(f)) --> STRUCT(c, f)
         * ```
         */
        private fun buildArgumentPermutations(args: List<StaticType>): Sequence<List<StaticType>> {
            val flattenedArgs = args.map { it.flatten().allTypes }
            return buildArgumentPermutations(flattenedArgs, accumulator = emptyList())
        }

        private fun buildArgumentPermutations(args: List<List<StaticType>>, accumulator: List<StaticType>): Sequence<List<StaticType>> {
            if (args.isEmpty()) { return sequenceOf(accumulator) }
            val first = args.first()
            val rest = when (args.size) {
                1 -> emptyList()
                else -> args.subList(1, args.size)
            }
            return sequence {
                first.forEach { argSubType ->
                    yieldAll(buildArgumentPermutations(rest, accumulator + listOf(argSubType)))
                }
            }
        }

        // Helpers

        private fun inferPathStep(type: StaticType, step: Rex.Op.Path.Step): StaticType = when (type) {
            is AnyType -> StaticType.ANY
            is StructType -> inferPathStep(type, step)
            is ListType, is SexpType -> inferPathStep(type as CollectionType, step)
            is AnyOfType -> {
                when (type.types.size) {
                    0 -> throw IllegalStateException("Cannot path on an empty StaticType union")
                    else -> {
                        val prevTypes = type.allTypes
                        if (prevTypes.any { it is AnyType }) {
                            StaticType.ANY
                        } else {
                            val staticTypes = prevTypes.map { inferPathStep(it, step) }
                            AnyOfType(staticTypes.toSet()).flatten()
                        }
                    }
                }
            }
            else -> StaticType.MISSING
        }

        private fun inferPathStep(struct: StructType, step: Rex.Op.Path.Step): StaticType = when (step) {
            is Rex.Op.Path.Step.Index -> {
                if (step.key.type !is StringType) {
                    error("Expected string but found: ${step.key.type}")
                }
                if (step.key.op is Rex.Op.Lit) {
                    val lit = (step.key.op as Rex.Op.Lit).value
                    if (lit is TextValue<*> && !lit.isNull) {
                        val id = identifierSymbol(lit.string!!, Identifier.CaseSensitivity.SENSITIVE)
                        inferStructLookup(struct, id)
                    } else {
                        error("Expected text literal, but got $lit")
                    }
                } else {
                    // cannot infer type of non-literal path step because we don't know its value
                    // we might improve upon this with some constant folding prior to typing
                    StaticType.ANY
                }
            }
            is Rex.Op.Path.Step.Symbol -> inferStructLookup(struct, step.identifier)
            is Rex.Op.Path.Step.Unpivot -> error("Unpivot not supported")
            is Rex.Op.Path.Step.Wildcard -> error("Wildcard not supported")
        }

        private fun inferPathStep(collection: CollectionType, step: Rex.Op.Path.Step): StaticType {
            if (step !is Rex.Op.Path.Step.Index) {
                error("Path step on a collection must be an expression")
            }
            if (step.key.type !is IntType) {
                error("Collections must be indexed with integers, found ${step.key.type}")
            }
            return collection.elementType
        }

        private fun inferStructLookup(struct: StructType, key: Identifier.Symbol): StaticType {
            val binding = key.toBindingName()
            val type = when (struct.constraints.contains(TupleConstraint.Ordered)) {
                true -> struct.fields.firstOrNull { entry -> binding.isEquivalentTo(entry.key) }?.value
                false -> struct.fields.mapNotNull { entry ->
                    entry.value.takeIf { binding.isEquivalentTo(entry.key) }
                }.let { valueTypes ->
                    StaticType.unionOf(valueTypes.toSet()).flatten().takeIf { valueTypes.isNotEmpty() }
                }
            }
            return type ?: when (struct.contentClosed) {
                true -> StaticType.MISSING
                false -> StaticType.ANY
            }
        }

        /**
         * Resolution and typing of aggregation function calls.
         *
         * I've chosen to place this in RexTyper because all arguments will be typed using the same locals.
         * There's no need to create new RexTyper instances for each argument. There is no reason to limit aggregations
         * to a single argument (covar, corr, pct, etc.) but in practice we typically only have single <value expression>.
         *
         * This method is _very_ similar to scalar function resolution, so it is temping to DRY these two out; but the
         * separation is cleaner as the typing of NULLS is subtly different.
         *
         * SQL-99 6.16 General Rules on <set function specification>
         *     Let TX be the single-column table that is the result of applying the <value expression>
         *     to each row of T and eliminating null values <--- all NULL values are eliminated as inputs
         */
        public fun resolveAgg(agg: Agg.Unresolved, arguments: List<Rex>): Pair<Rel.Op.Aggregate.Call, StaticType> {
            var missingArg = false
            val args = arguments.map {
                val arg = visitRex(it, null)
                if (arg.type.isMissable()) missingArg = true
                arg
            }

            //
            if (missingArg) {
                handleAlwaysMissing()
                return relOpAggregateCall(agg, listOf(rexErr("MISSING"))) to MissingType
            }

            // Try to match the arguments to functions defined in the catalog
            return when (val match = env.resolveAgg(agg, args)) {
                is FnMatch.Ok -> {
                    // Found a match!
                    val newAgg = aggResolved(match.signature)
                    val newArgs = rewriteFnArgs(match.mapping, args)
                    val returns = newAgg.signature.returns

                    // Return type with calculated nullability
                    var type = when {
                        newAgg.signature.isNullable -> returns.toStaticType()
                        else -> returns.toNonNullStaticType()
                    }

                    // Some operators can return MISSING during runtime
                    if (match.isMissable) {
                        type = StaticType.unionOf(type, StaticType.MISSING).flatten()
                    }

                    // Finally, rewrite this node
                    relOpAggregateCall(newAgg, newArgs) to type
                }
                is FnMatch.Error -> {
                    handleUnknownFunction(match)
                    return relOpAggregateCall(agg, listOf(rexErr("MISSING"))) to MissingType
                }
            }
        }
    }

    // HELPERS

    private fun Rel.type(typeEnv: TypeEnv): Rel = RelTyper(typeEnv).visitRel(this, null)

    private fun Rex.type(typeEnv: TypeEnv) = RexTyper(typeEnv).visitRex(this, this.type)

    private fun rexErr(message: String) = rex(StaticType.MISSING, rexOpErr(message))

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
     *
     * TODO this does not handle constant expressions in `[]`, only literals
     */
    @OptIn(PartiQLValueExperimental::class)
    private fun rexPathToBindingPath(rootOp: Rex.Op.Var.Unresolved, steps: List<Rex.Op.Path.Step>): BindingPath {
        if (rootOp.identifier !is Identifier.Symbol) {
            throw IllegalArgumentException("Expected identifier symbol")
        }
        val bindingRoot = (rootOp.identifier as Identifier.Symbol).toBindingName()
        val bindingSteps = mutableListOf(bindingRoot)
        for (step in steps) {
            when (step) {
                is Rex.Op.Path.Step.Index -> {
                    if (step.key.op is Rex.Op.Lit) {
                        val v = (step.key.op as Rex.Op.Lit).value
                        if (v is TextValue<*>) {
                            // add to prefix
                            bindingSteps.add(BindingName(v.string!!, BindingCase.SENSITIVE))
                        } else {
                            // short-circuit
                            break
                        }
                    } else {
                        break
                    }
                }
                is Rex.Op.Path.Step.Symbol -> bindingSteps.add(step.identifier.toBindingName())
                else -> break // short-circuit
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
    private fun rewriteFnArgs(mapping: List<FunctionSignature.Scalar?>, args: List<Rex>): List<Rex> {
        if (mapping.size != args.size) {
            error("Fatal, malformed function mapping") // should be unreachable given how a mapping is generated.
        }
        val newArgs = mutableListOf<Rex>()
        for (i in mapping.indices) {
            var a = args[i]
            val m = mapping[i]
            if (m != null) {
                // rewrite
                val type = m.returns.toNonNullStaticType()
                val cast = rexOpCall(fnResolved(m), listOf(a))
                a = rex(type, cast)
            }
            newArgs.add(a)
        }
        return newArgs
    }

    private fun assertAsInt(type: StaticType) {
        if (type.flatten().allTypes.any { variant -> variant is IntType }.not()) {
            handleUnexpectedType(type, setOf(StaticType.INT))
        }
    }

    /**
     * Constructs a Rex.Op.Path from a resolved local
     */
    private fun resolvedLocalPath(local: ResolvedVar.Local): Rex.Op {
        val root = rex(local.rootType, rexOpVarResolved(local.ordinal))
        val steps = local.tail.map {
            val case = when (it.bindingCase) {
                BindingCase.SENSITIVE -> Identifier.CaseSensitivity.SENSITIVE
                BindingCase.INSENSITIVE -> Identifier.CaseSensitivity.INSENSITIVE
            }
            rexOpPathStepSymbol(identifierSymbol(it.name, case))
        }
        return when (steps.isEmpty()) {
            true -> root.op
            false -> rexOpPath(root, steps)
        }
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

    private fun handleUnknownFunction(match: FnMatch.Error<*>) {
        onProblem(
            Problem(
                sourceLocation = UNKNOWN_PROBLEM_LOCATION,
                details = PlanningProblemDetails.UnknownFunction(
                    match.identifier.normalize(),
                    match.args.map { a -> a.type },
                )
            )
        )
    }

    private fun handleAlwaysMissing() {
        onProblem(
            Problem(
                sourceLocation = UNKNOWN_PROBLEM_LOCATION,
                details = PlanningProblemDetails.ExpressionAlwaysReturnsNullOrMissing
            )
        )
    }

    private fun handleUnresolvedExcludeRoot(root: String) {
        onProblem(
            Problem(
                sourceLocation = UNKNOWN_PROBLEM_LOCATION,
                details = PlanningProblemDetails.UnresolvedExcludeExprRoot(root)
            )
        )
    }

    // HELPERS

    private fun Identifier.normalize(): String = when (this) {
        is Identifier.Qualified -> (listOf(root.normalize()) + steps.map { it.normalize() }).joinToString(".")
        is Identifier.Symbol -> when (caseSensitivity) {
            Identifier.CaseSensitivity.SENSITIVE -> symbol
            Identifier.CaseSensitivity.INSENSITIVE -> symbol.lowercase()
        }
    }

    /**
     * The equals function is the only function which NEVER returns MISSING. This function is called when typing
     * and resolving the equals function.
     */
    private fun Fn.Unresolved.isEq(): Boolean {
        return (identifier is Identifier.Symbol && (identifier as Identifier.Symbol).symbol == "eq")
    }

    /**
     * This will make all binding values nullables. If the value is a struct, each field will be nullable.
     *
     * Note, this does not handle union types or nullable struct types.
     */
    private fun List<Rel.Binding>.pad() = map {
        val type = when (val t = it.type) {
            is StructType -> t.withNullableFields()
            else -> t.asNullable()
        }
        relBinding(it.name, type)
    }

    private fun StructType.withNullableFields(): StructType {
        return copy(fields.map { it.copy(value = it.value.asNullable()) })
    }

    private fun excludeBindings(input: List<Rel.Binding>, item: Rel.Op.Exclude.Item): List<Rel.Binding> {
        var matchedRoot = false
        val output = input.map {
            if (item.root.isEquivalentTo(it.name)) {
                matchedRoot = true
                // recompute the StaticType of this binding after apply the exclusions
                val type = it.type.exclude(item.steps, false)
                it.copy(type = type)
            } else {
                it
            }
        }
        if (!matchedRoot) handleUnresolvedExcludeRoot(item.root.symbol)
        return output
    }

    private fun Identifier.Symbol.isEquivalentTo(other: String): Boolean = when (caseSensitivity) {
        Identifier.CaseSensitivity.SENSITIVE -> symbol.equals(other)
        Identifier.CaseSensitivity.INSENSITIVE -> symbol.equals(other, ignoreCase = true)
    }
}
