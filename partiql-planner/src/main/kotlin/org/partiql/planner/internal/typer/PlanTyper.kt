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

package org.partiql.planner.internal.typer

import org.partiql.planner.internal.Env
import org.partiql.planner.internal.PErrors
import org.partiql.planner.internal.exclude.ExcludeRepr
import org.partiql.planner.internal.ir.PlanNode
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.Statement
import org.partiql.planner.internal.ir.rel
import org.partiql.planner.internal.ir.relOpAggregate
import org.partiql.planner.internal.ir.relOpDistinct
import org.partiql.planner.internal.ir.relOpExclude
import org.partiql.planner.internal.ir.relOpExcludePath
import org.partiql.planner.internal.ir.relOpFilter
import org.partiql.planner.internal.ir.relOpJoin
import org.partiql.planner.internal.ir.relOpLimit
import org.partiql.planner.internal.ir.relOpOffset
import org.partiql.planner.internal.ir.relOpProject
import org.partiql.planner.internal.ir.relOpScan
import org.partiql.planner.internal.ir.relOpScanIndexed
import org.partiql.planner.internal.ir.relOpSort
import org.partiql.planner.internal.ir.relOpUnpivot
import org.partiql.planner.internal.ir.relOpWindow
import org.partiql.planner.internal.ir.relType
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpCoalesce
import org.partiql.planner.internal.ir.rexOpCollection
import org.partiql.planner.internal.ir.rexOpErr
import org.partiql.planner.internal.ir.rexOpNullif
import org.partiql.planner.internal.ir.rexOpPathIndex
import org.partiql.planner.internal.ir.rexOpPathKey
import org.partiql.planner.internal.ir.rexOpPivot
import org.partiql.planner.internal.ir.rexOpStruct
import org.partiql.planner.internal.ir.rexOpStructField
import org.partiql.planner.internal.ir.rexOpSubquery
import org.partiql.planner.internal.ir.statementQuery
import org.partiql.planner.internal.ir.util.PlanRewriter
import org.partiql.planner.internal.util.FunctionUtils
import org.partiql.planner.internal.util.TypeUtils.exclude
import org.partiql.spi.Context
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorListener
import org.partiql.spi.types.PType
import org.partiql.spi.types.PTypeField
import org.partiql.spi.value.Datum
import kotlin.math.max

/**
 * Rewrites an untyped algebraic translation of the query to be both typed and have resolved variables.
 *
 * @property env
 */
internal class PlanTyper(private val env: Env, config: Context) {

    private val _listener = config.errorListener

    /**
     * Rewrite the statement with inferred types and resolved variables
     */
    fun resolve(statement: Statement): Statement {
        if (statement !is Statement.Query) {
            throw IllegalArgumentException("PartiQLPlanner only supports Query statements")
        }
        // root TypeEnv has no bindings
        val root = statement.root.type(emptyList(), emptyList(), Strategy.GLOBAL)
        return statementQuery(root)
    }
    internal companion object {
        fun PType.static(): CompilerType = CompilerType(this)

        fun anyOf(types: Collection<PType>): PType? {
            val unique = types.toSet()
            return when (unique.size) {
                0 -> null
                1 -> unique.first()
                else -> PType.dynamic()
            }
        }

        /**
         * This is specifically to collapse literals.
         *
         * TODO: Can this be merged with [anyOf]? Should we even allow this?
         */
        fun anyOfLiterals(types: Collection<CompilerType>): PType? {
            // Grab unique
            var unique: Collection<PType> = types.map { it.getDelegate() }.toSet()
            if (unique.isEmpty()) {
                return null
            } else if (unique.size == 1) {
                return unique.first()
            }

            // Filter out UNKNOWN
            unique = unique.filter { it.code() != PType.UNKNOWN }
            if (unique.isEmpty()) {
                return PType.unknown()
            } else if (unique.size == 1) {
                return unique.first()
            }

            // Collapse Collections
            if (unique.all { it.code() == PType.ARRAY } ||
                unique.all { it.code() == PType.BAG }
            ) {
                return collapseCollection(unique, unique.first().code())
            }
            // Collapse Structs
            if (unique.all { it.code() == PType.ROW }) {
                return collapseRows(unique)
            }
            return PType.dynamic()
        }

        private fun collapseCollection(collections: Iterable<PType>, type: Int): PType {
            val typeParam = anyOfLiterals(collections.map { it.typeParameter.toCType() })!!
            return when (type) {
                PType.ARRAY -> PType.array(typeParam)
                PType.BAG -> PType.array(typeParam)
                else -> error("This shouldn't have happened.")
            }
        }

        private fun collapseRows(rows: Iterable<PType>): PType {
            val firstFields = rows.first().fields
            val fieldNames = firstFields.map { it.name }
            val fieldTypes = firstFields.map { mutableListOf(it.type.toCType()) }
            rows.map { struct ->
                val fields = struct.fields
                if (fields.map { it.name } != fieldNames) {
                    return PType.struct()
                }
                fields.forEachIndexed { index, field -> fieldTypes[index].add(field.type.toCType()) }
            }
            val newFields = fieldTypes.mapIndexed { i, types -> PTypeField.of(fieldNames[i], anyOfLiterals(types)!!) }
            return PType.row(newFields)
        }

        fun anyOf(vararg types: PType): PType? {
            val unique = types.toSet()
            return anyOf(unique)
        }

        fun PType.toCType(): CompilerType = when (this) {
            is CompilerType -> this
            else -> CompilerType(this)
        }

        fun List<PType>.toCType(): List<CompilerType> = this.map { it.toCType() }

        /**
         * Reports the [problem]]
         * @return an error node
         */
        fun errorRexAndReport(listener: PErrorListener, problem: PError): Rex {
            return errorRexAndReport(listener, problem, PType.dynamic())
        }

        /**
         * Reports the [problem]]
         * @return an error node
         */
        fun errorRexAndReport(listener: PErrorListener, problem: PError, type: PType): Rex {
            listener.report(problem)
            return rex(CompilerType(type, isMissingValue = true), rexOpErr())
        }
    }

    /**
     * Types the relational operators of a query expression.
     *
     * @property outer represents the outer variable scopes of a query expression — only used by scan variable resolution.
     * @property strategy
     */
    private inner class RelTyper(
        private val outer: List<Scope>,
        private val strategy: Strategy,
    ) : PlanRewriter<Rel.Type?>() {

        override fun visitRel(node: Rel, ctx: Rel.Type?) = visitRelOp(node.op, node.type) as Rel

        /**
         * The output schema of a `rel.op.scan` is the single value binding.
         */
        override fun visitRelOpScan(node: Rel.Op.Scan, ctx: Rel.Type?): Rel {
            // descend, with GLOBAL resolution strategy
            val rex = node.rex.type(emptyList(), outer, Strategy.GLOBAL)
            // compute rel type
            val valueT = getElementTypeForFromSource(rex.type)
            val type = ctx!!.copyWithSchema(listOf(valueT))
            // rewrite
            val op = relOpScan(rex)
            return rel(type, op)
        }

        /**
         * The output schema of a `rel.op.scan_index` is the value binding and index binding.
         */
        override fun visitRelOpScanIndexed(node: Rel.Op.ScanIndexed, ctx: Rel.Type?): Rel {
            // descend, with GLOBAL resolution strategy
            val rex = node.rex.type(emptyList(), outer, Strategy.GLOBAL)
            // compute rel type
            val valueT = getElementTypeForFromSource(rex.type)
            val indexT = PType.bigint()
            val type = ctx!!.copyWithSchema(listOf(valueT, indexT).toCType())
            // rewrite
            val op = relOpScanIndexed(rex)
            return rel(type, op)
        }

        override fun visitRelOpWindow(node: Rel.Op.Window, ctx: Rel.Type?): Rel {
            val input = visitRel(node.input, ctx)
            val functions = node.functions.map { visitRelOpWindowWindowFunction(it, input.type) }
            val partitions = node.partitions.map { it.type(input.type.schema, outer) }
            val sorts = node.sorts.map {
                val rex = it.rex.type(input.type.schema, outer)
                it.copy(rex = rex)
            }
            val schema = ctx!!.copyWithSchema(input.type.schema.map { it.type } + functions.map { it.returnType!! })
            val window = relOpWindow(input, functions, partitions, sorts)
            return rel(schema, window)
        }

        override fun visitRelOpWindowWindowFunction(node: Rel.Op.Window.WindowFunction, ctx: Rel.Type?): Rel.Op.Window.WindowFunction {
            val args = node.args.map { it.type(ctx!!.schema, outer) } // TODO: Check this
            return node.copy(returnType = PType.bigint().toCType(), args = args) // TODO: Actually get return type.
        }

        /**
         * TODO handle NULL|STRUCT type
         */
        override fun visitRelOpUnpivot(node: Rel.Op.Unpivot, ctx: Rel.Type?): Rel {
            val rex = node.rex.type(emptyList(), outer, Strategy.GLOBAL)
            val op = relOpUnpivot(rex)
            val kType = PType.string()

            // Check Root (Dynamic)
            if (rex.type.code() == PType.DYNAMIC || rex.type.code() == PType.VARIANT) {
                val type = ctx!!.copyWithSchema(listOf(kType, PType.dynamic()).toCType())
                return rel(type, op)
            }

            // Check Root
            val vType = when (rex.type.code()) {
                PType.ROW -> anyOf(rex.type.fields.map { it.type }) ?: PType.dynamic()
                PType.STRUCT -> PType.dynamic()
                else -> rex.type
            }

            // rewrite
            val type = ctx!!.copyWithSchema(listOf(kType, vType).toCType())
            return rel(type, op)
        }

        override fun visitRelOpErr(node: Rel.Op.Err, ctx: Rel.Type?): Rel {
            val type = ctx ?: relType(emptyList(), emptySet())
            return rel(type, node)
        }

        override fun visitRelOpDistinct(node: Rel.Op.Distinct, ctx: Rel.Type?): Rel {
            val input = visitRel(node.input, ctx)
            return rel(input.type, relOpDistinct(input))
        }

        override fun visitRelOpFilter(node: Rel.Op.Filter, ctx: Rel.Type?): Rel {
            // compute input schema
            val input = visitRel(node.input, ctx)
            // type sub-nodes
            val predicate = node.predicate.type(input.type.schema, outer)
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
            val specs = node.specs.map {
                val rex = it.rex.type(input.type.schema, outer)
                it.copy(rex = rex)
            }
            // output schema of a sort is the same as the input
            val type = input.type.copy(props = setOf(Rel.Prop.ORDERED))
            // rewrite
            val op = relOpSort(input, specs)
            return rel(type, op)
        }

        override fun visitRelOpWith(node: Rel.Op.With, ctx: Rel.Type?): Rel {
            val elements = node.elements.map { element ->
                val representation = element.representation.type(emptyList(), outer)
                element.copy(representation = representation)
            }
            val newStack = outer + Scope(listOf(), outer, elements)
            val input = RelTyper(newStack, Strategy.LOCAL).visitRel(node.input, node.input.type)
            val type = input.type
            return Rel(type, node.copy(elements = elements, input = input))
        }

        override fun visitRelOpExcept(node: Rel.Op.Except, ctx: Rel.Type?): Rel {
            val lhs = visitRel(node.lhs, node.lhs.type)
            val rhs = visitRel(node.rhs, node.rhs.type)
            // Check for Compatibility
            if (!node.isOuter && !setOpSchemaSizesMatch(lhs, rhs)) {
                return createRelErrForSetOpMismatchSizes()
            }
            if (!node.isOuter && !setOpSchemaTypesMatch(lhs, rhs)) {
                return createRelErrForSetOpMismatchTypes()
            }
            // Compute Schema
            val type = Rel.Type(lhs.type.schema, props = emptySet())
            return Rel(type, node.copy(lhs = lhs, rhs = rhs))
        }

        override fun visitRelOpIntersect(node: Rel.Op.Intersect, ctx: Rel.Type?): Rel {
            val lhs = visitRel(node.lhs, node.lhs.type)
            val rhs = visitRel(node.rhs, node.rhs.type)
            // Check for Compatibility
            if (!node.isOuter && !setOpSchemaSizesMatch(lhs, rhs)) {
                return createRelErrForSetOpMismatchSizes()
            }
            if (!node.isOuter && !setOpSchemaTypesMatch(lhs, rhs)) {
                return createRelErrForSetOpMismatchTypes()
            }
            // Compute Schema
            val type = Rel.Type(lhs.type.schema, props = emptySet())
            return Rel(type, node.copy(lhs = lhs, rhs = rhs))
        }

        override fun visitRelOpUnion(node: Rel.Op.Union, ctx: Rel.Type?): Rel {
            val lhs = visitRel(node.lhs, node.lhs.type)
            val rhs = visitRel(node.rhs, node.rhs.type)
            // Check for Compatibility
            if (!node.isOuter && !setOpSchemaSizesMatch(lhs, rhs)) {
                return createRelErrForSetOpMismatchSizes()
            }
            if (!node.isOuter && !setOpSchemaTypesMatch(lhs, rhs)) {
                return createRelErrForSetOpMismatchTypes()
            }
            // Compute Schema
            val size = max(lhs.type.schema.size, rhs.type.schema.size)
            val schema = List(size) {
                val lhsBinding = lhs.type.schema.getOrNull(it) ?: Rel.Binding("_$it", CompilerType(PType.dynamic(), isMissingValue = true))
                val rhsBinding = rhs.type.schema.getOrNull(it) ?: Rel.Binding("_$it", CompilerType(PType.dynamic(), isMissingValue = true))
                val bindingName = when (lhsBinding.name == rhsBinding.name) {
                    true -> lhsBinding.name
                    false -> "_$it"
                }
                Rel.Binding(bindingName, CompilerType(anyOf(lhsBinding.type, rhsBinding.type)!!))
            }
            val type = Rel.Type(schema, props = emptySet())
            return Rel(type, node.copy(lhs = lhs, rhs = rhs))
        }

        /**
         * @return whether each type of the [lhs] is equal to its counterpart on the [rhs]
         * @param lhs should be typed already
         * @param rhs should be typed already
         */
        private fun setOpSchemaTypesMatch(lhs: Rel, rhs: Rel): Boolean {
            // TODO: [RFC-0007](https://github.com/partiql/partiql-lang/blob/main/RFCs/0007-rfc-bag-operators.md)
            //  states that the types must be "comparable". The below code ONLY makes sure that types need to be
            //  the same. In the future, we need to add support for checking comparable types.
            for (i in 0..lhs.type.schema.lastIndex) {
                val lhsBindingType = lhs.type.schema[i].type
                val rhsBindingType = rhs.type.schema[i].type
                if (lhsBindingType != rhsBindingType) {
                    return false
                }
            }
            return true
        }

        /**
         * @return whether the [lhs] and [rhs] schemas are of equal size
         * @param lhs should be typed already
         * @param rhs should be typed already
         */
        private fun setOpSchemaSizesMatch(lhs: Rel, rhs: Rel): Boolean {
            return lhs.type.schema.size == rhs.type.schema.size
        }

        private fun createRelErrForSetOpMismatchSizes(): Rel {
            return Rel(Rel.Type(emptyList(), emptySet()), Rel.Op.Err("LHS and RHS of SET OP do not have the same number of bindings."))
        }

        private fun createRelErrForSetOpMismatchTypes(): Rel {
            return Rel(Rel.Type(emptyList(), emptySet()), Rel.Op.Err("LHS and RHS of SET OP do not have the same type."))
        }

        override fun visitRelOpLimit(node: Rel.Op.Limit, ctx: Rel.Type?): Rel {
            // compute input schema
            val input = visitRel(node.input, ctx)
            // type limit expression using outer scope with global resolution
            // TODO: Assert expression doesn't contain locals or upvalues.
            val limit = node.limit.type(input.type.schema, outer, Strategy.GLOBAL)
            // check types
            if (limit.type.isNumeric().not()) {
                val problem = PErrors.typeUnexpected(null, limit.type, listOf(PType.numeric(38, 0)))
                val err = errorRexAndReport(_listener, problem)
                return rel(input.type, relOpLimit(input, err))
            }
            // rewrite
            val type = input.type
            val op = relOpLimit(input, limit)
            return rel(type, op)
        }

        override fun visitRelOpOffset(node: Rel.Op.Offset, ctx: Rel.Type?): Rel {
            // compute input schema
            val input = visitRel(node.input, ctx)
            // type offset expression using outer scope with global resolution
            // TODO: Assert expression doesn't contain locals or upvalues.
            val offset = node.offset.type(input.type.schema, outer, Strategy.GLOBAL)
            // check types
            if (offset.type.isNumeric().not()) {
                val problem = PErrors.typeUnexpected(null, offset.type, listOf(PType.numeric(38, 0)))
                val err = errorRexAndReport(_listener, problem)
                return rel(input.type, relOpLimit(input, err))
            }
            // rewrite
            val type = input.type
            val op = relOpOffset(input, offset)
            return rel(type, op)
        }

        override fun visitRelOpProject(node: Rel.Op.Project, ctx: Rel.Type?): Rel {
            // compute input schema
            val input = visitRel(node.input, ctx)
            // type sub-nodes
            val projections = node.projections.map {
                it.type(input.type.schema, outer)
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
            val stack = when (node.type) {
                Rel.Op.Join.Type.INNER, Rel.Op.Join.Type.LEFT -> outer + listOf(Scope(lhs.type.schema, outer))
                Rel.Op.Join.Type.FULL, Rel.Op.Join.Type.RIGHT -> outer
            }
            val rhs = RelTyper(stack, Strategy.GLOBAL).visitRel(node.rhs, ctx)

            // Calculate output schema given JOIN type
            val schema = lhs.type.schema + rhs.type.schema
            val type = relType(schema, ctx!!.props)

            // Type the condition on the output schema
            val typeEnv = TypeEnv(env, Scope(type.schema, outer))
            val condition = node.rex.type(typeEnv)

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

        // TODO: better error reporting with exclude.
        override fun visitRelOpExclude(node: Rel.Op.Exclude, ctx: Rel.Type?): Rel {
            // compute input schema
            val input = visitRel(node.input, ctx)

            // apply exclusions to the input schema
            val initBindings = input.type.schema.map { it.copy() }
            ExcludeUtils.checkForInvalidExcludePaths(initBindings, node.paths, _listener)
            val schema = node.paths.fold((initBindings)) { bindings, item -> excludeBindings(bindings, item) }

            // rewrite
            val type = ctx!!.copy(schema = schema)

            // resolve exclude path roots
            val newPaths = node.paths.map { path ->
                val resolvedRoot = when (val root = path.root) {
                    is Rex.Op.Var.Unresolved -> {
                        // resolve `root` to local binding
                        val locals = Scope(input.type.schema, outer)
                        val typeEnv = TypeEnv(env, locals)
                        val resolved = typeEnv.resolve(root.identifier)
                        if (resolved == null) {
                            val problem = when (root.identifier.hasQualifier()) {
                                true -> PErrors.alwaysMissing(null)
                                false -> {
                                    val localBindings = locals.schema.map { it.name }
                                    PErrors.varRefNotFound(null, root.identifier, localBindings)
                                }
                            }
                            val rex = errorRexAndReport(_listener, problem)
                            rex.op
                        } else {
                            // root of exclude is always a symbol
                            resolved.op as Rex.Op.Var
                        }
                    }
                    is Rex.Op.Var.Local, is Rex.Op.Var.Global -> root
                    else -> error("Expect exclude path root to be Rex.Op.Var")
                }
                relOpExcludePath(resolvedRoot, path.steps)
            }

            val subsumedPaths = newPaths
                .groupBy(
                    keySelector = { it.root },
                    valueTransform = { it.steps }
                ) // combine exclude paths with the same resolved root before subsumption
                .map { (root, allSteps) ->
                    val nonRedundant = ExcludeRepr.toExcludeRepr(allSteps.flatten()).removeRedundantSteps()
                    relOpExcludePath(root, nonRedundant.toPlanRepr())
                }
            val op = relOpExclude(input, subsumedPaths)
            return rel(type, op)
        }

        override fun visitRelOpAggregate(node: Rel.Op.Aggregate, ctx: Rel.Type?): Rel {
            // TODO: Do we need to report aggregation call always returns MISSING?
            //  Currently aggregation is part of the rel op
            //  The rel op should produce a set of binding tuple, which missing should be allowed.
            // compute input schema
            val input = visitRel(node.input, ctx)

            // type the calls and groups
            val typeEnv = TypeEnv(env, Scope(input.type.schema, outer))
            val typer = RexTyper(typeEnv, Strategy.LOCAL)

            // typing of aggregate calls is slightly more complicated because they are not expressions.
            val calls = node.calls.mapIndexed { i, call ->
                when (call) {
                    is Rel.Op.Aggregate.Call.Resolved -> call to ctx!!.schema[i].type
                    is Rel.Op.Aggregate.Call.Unresolved -> typer.resolveAgg(call)
                }
            }
            val groups = node.groups.map { typer.visitRex(it, null) }

            // Compute schema using order (calls...groups...)
            val schema = mutableListOf<CompilerType>()
            schema += calls.map { it.second }
            schema += groups.map { it.type }

            // rewrite with typed calls and groups
            val type = ctx!!.copyWithSchema(schema)
            val op = relOpAggregate(
                input = input,
                strategy = node.strategy,
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
     * We should consider making the PType? parameter non-nullable.
     *
     * @property typeEnv TypeEnv in which this rex tree is evaluated.
     */
    private inner class RexTyper(
        private val typeEnv: TypeEnv,
        private val strategy: Strategy,
    ) : PlanRewriter<CompilerType?>() {

        override fun visitRex(node: Rex, ctx: CompilerType?): Rex {
            val rex = visitRexOp(node.op, node.type) as Rex
            return rex
        }

        override fun visitRexOpLit(node: Rex.Op.Lit, ctx: CompilerType?): Rex {
            // type comes from RexConverter
            return rex(ctx!!, node)
        }

        override fun visitRexOpVarLocal(node: Rex.Op.Var.Local, ctx: CompilerType?): Rex {
            val scope = typeEnv.locals.getScope(node.depth)
            assert(node.ref < scope.schema.size) {
                "Invalid resolved variable (var ${node.ref}, stack frame ${node.depth}) in env: $typeEnv"
            }
            val type = scope.schema.getOrNull(node.ref)?.type ?: error("Can't find locals value.")
            return rex(type, node)
        }

        override fun visitRexOpVarUnresolved(node: Rex.Op.Var.Unresolved, ctx: CompilerType?): Rex {
            val strategy = when (node.scope) {
                Rex.Op.Var.Scope.DEFAULT -> strategy
                Rex.Op.Var.Scope.LOCAL -> Strategy.LOCAL
            }
            val resolvedVar = typeEnv.resolve(node.identifier, strategy)
            if (resolvedVar == null) {
                val inScopeVariables = typeEnv.locals.schema.map { it.name }.toSet()
                val problem = PErrors.varRefNotFound(null, node.identifier, inScopeVariables.toList())
                return errorRexAndReport(_listener, problem, PType.unknown())
            }
            return visitRex(resolvedVar, null)
        }

        override fun visitRexOpVarGlobal(node: Rex.Op.Var.Global, ctx: CompilerType?): Rex = rex(node.ref.type, node)

        /**
         * TODO: Create a function signature for the Rex.Op.Path.Index to get automatic coercions.
         */
        override fun visitRexOpPathIndex(node: Rex.Op.Path.Index, ctx: CompilerType?): Rex {
            val root = visitRex(node.root, node.root.type)
            val key = visitRex(node.key, node.key.type)

            // Check Key Type (INT or coercible to INT). TODO: Allow coercions to INT
            if (key.type.code() !in setOf(PType.TINYINT, PType.SMALLINT, PType.INTEGER, PType.BIGINT, PType.NUMERIC)) {
                val problem = PErrors.pathIndexNeverSucceeds(null)
                return errorRexAndReport(_listener, problem)
            }

            // Check if Root is DYNAMIC
            if (root.type.code() == PType.DYNAMIC || root.type.code() == PType.VARIANT) {
                return Rex(CompilerType(PType.dynamic()), Rex.Op.Path.Index(root, key))
            }

            // Check Root Type LIST
            if (root.type.code() != PType.ARRAY) {
                return errorRexAndReport(_listener, PErrors.pathIndexNeverSucceeds(null))
            }

            // Check that root is not literal missing
            if (root.isLiteralMissing()) {
                return errorRexAndReport(_listener, PErrors.pathIndexNeverSucceeds(null))
            }

            return rex(root.type.typeParameter, rexOpPathIndex(root, key))
        }

        private fun Rex.isLiteralMissing(): Boolean = this.op is Rex.Op.Lit && this.op.value.isMissing

        override fun visitRexOpPathKey(node: Rex.Op.Path.Key, ctx: CompilerType?): Rex {
            val root = visitRex(node.root, node.root.type)
            val key = visitRex(node.key, node.key.type)

            // Check Key Type (STRING). TODO: Allow coercions to STRING
            if (key.type.code() != PType.STRING) {
                return errorRexAndReport(_listener, PErrors.pathKeyNeverSucceeds(null))
            }

            // Check if Root is DYNAMIC
            if (root.type.code() == PType.DYNAMIC || root.type.code() == PType.VARIANT) {
                return Rex(CompilerType(PType.dynamic()), Rex.Op.Path.Key(root, key))
            }

            // Check Root Type (STRUCT)
            if (root.type.code() != PType.STRUCT && root.type.code() != PType.ROW) {
                return errorRexAndReport(_listener, PErrors.pathKeyNeverSucceeds(null), PType.unknown())
            }

            // Get Literal Key
            val keyOp = key.op
            val keyLiteral = when (keyOp is Rex.Op.Lit && keyOp.value.isTextValue() && !keyOp.value.isNull) {
                true -> keyOp.value.string
                false -> return rex(CompilerType(PType.dynamic()), rexOpPathKey(root, key))
            }

            // Find Type
            val elementType = root.type.getField(keyLiteral, false) ?: run {
                return errorRexAndReport(_listener, PErrors.pathKeyNeverSucceeds(null), PType.unknown())
            }

            return rex(elementType, rexOpPathKey(root, key))
        }

        private fun Datum.isTextValue(): Boolean {
            return this.type.code() in setOf(PType.STRING, PType.CHAR, PType.VARCHAR)
        }

        override fun visitRexOpPathSymbol(node: Rex.Op.Path.Symbol, ctx: CompilerType?): Rex {
            val root = visitRex(node.root, node.root.type)

            // Check if Root is DYNAMIC
            if (root.type.code() == PType.DYNAMIC || root.type.code() == PType.VARIANT) {
                return Rex(CompilerType(PType.dynamic()), Rex.Op.Path.Symbol(root, node.key))
            }

            // Check Root Type (STRUCT)
            if (root.type.code() != PType.STRUCT && root.type.code() != PType.ROW) {
                return errorRexAndReport(_listener, PErrors.pathSymbolNeverSucceeds(null))
            }

            // Check that root is not literal missing
            if (root.isLiteralMissing()) {
                return errorRexAndReport(_listener, PErrors.pathSymbolNeverSucceeds(null))
            }

            // Find Type
            val field = root.type.getSymbol(node.key) ?: run {
                val inScopeVariables = typeEnv.locals.schema.map { it.name }
                val problem = PErrors.varRefNotFound(null, Identifier.regular(node.key), inScopeVariables)
                return errorRexAndReport(_listener, problem)
            }
            return when (field.first.isRegular()) {
                true -> Rex(field.second, Rex.Op.Path.Symbol(root, node.key))
                else -> Rex(field.second, Rex.Op.Path.Key(root, rexString(field.first.getText())))
            }
        }

        /**
         * Assumes that the type is either a struct of row.
         * @return null when the field definitely does not exist; dynamic when the type cannot be determined
         */
        private fun CompilerType.getField(field: String, ignoreCase: Boolean): CompilerType? {
            if (this.code() == PType.STRUCT) {
                return CompilerType(PType.dynamic())
            }
            val fields = this.fields.filter { it.name.equals(field, ignoreCase) }.map { it.type }.toSet()
            return when (fields.size) {
                0 -> return null
                1 -> fields.first()
                else -> CompilerType(PType.dynamic())
            }
        }

        private fun rexString(str: String) = rex(CompilerType(PType.string()), Rex.Op.Lit(Datum.string(str)))

        override fun visitRexOpCastUnresolved(node: Rex.Op.Cast.Unresolved, ctx: CompilerType?): Rex {
            val arg = visitRex(node.arg, null)
            val cast = env.resolveCast(arg, node.target) ?: run {
                val problem = PErrors.castUndefined(null, arg.type, node.target)
                return errorRexAndReport(_listener, problem)
            }
            return visitRexOpCastResolved(cast, null)
        }

        override fun visitRexOpCastResolved(node: Rex.Op.Cast.Resolved, ctx: CompilerType?): Rex {
            return rex(node.cast.target, node)
        }

        override fun visitRexOpCallUnresolved(node: Rex.Op.Call.Unresolved, ctx: CompilerType?): Rex {
            // Type the arguments
            val args = node.args.map { visitRex(it, null) }
            // Attempt to resolve in the environment
            val rex = env.resolveFn(node.identifier, args)
            if (rex == null) {
                val candidates = env.getCandidates(node.identifier, args)
                val argTypes = args.map { it.type }
                val problem = when (candidates.isEmpty()) {
                    true -> PErrors.functionNotFound(null, node.identifier, argTypes)
                    false -> PErrors.functionTypeMismatch(null, node.identifier, argTypes, candidates)
                }
                return errorRexAndReport(_listener, problem)
            }
            // Pass off to Rex.Op.Call.Static or Rex.Op.Call.Dynamic for typing.
            return visitRex(rex, null)
        }

        /**
         * Resolve and type scalar function calls.
         *
         * @param node
         * @param ctx
         * @return
         */
        override fun visitRexOpCallStatic(node: Rex.Op.Call.Static, ctx: CompilerType?): Rex {
            // Apply the coercions as explicit casts
            val args: List<Rex> = node.args.map {
                // Type the coercions
                when (val op = it.op) {
                    is Rex.Op.Cast.Resolved -> visitRexOpCastResolved(op, null)
                    else -> it
                }
            }
            val instance = node.fn
            val returnType: PType = instance.signature.returns

            // Check if any arg is always missing
            val argIsAlwaysMissing = args.any { it.type.isMissingValue }

            if (argIsAlwaysMissing && instance.signature.isMissingCall) {
                _listener.report(PErrors.alwaysMissing(null))
                return rex(CompilerType(returnType), Rex.Op.Call.Static(node.fn, args))
            }

            // Infer fn return type
            return rex(CompilerType(returnType), Rex.Op.Call.Static(node.fn, args))
        }

        /**
         * Typing of a dynamic function call.
         *
         * @param node
         * @param ctx
         * @return
         */
        override fun visitRexOpCallDynamic(node: Rex.Op.Call.Dynamic, ctx: CompilerType?): Rex {
            return Rex(type = CompilerType(PType.dynamic()), op = node)
        }

        override fun visitRexOpCase(node: Rex.Op.Case, ctx: CompilerType?): Rex {
            // Rewrite CASE-WHEN branches
            val oldBranches = node.branches.toTypedArray()
            val newBranches = mutableListOf<Rex.Op.Case.Branch>()
            val typer = DynamicTyper()
            for (i in oldBranches.indices) {

                // Type the branch
                var branch = oldBranches[i]
                branch = visitRexOpCaseBranch(branch, branch.rex.type)

                // Emit typing error if a branch condition is never a boolean (prune)
                if (!canBeBoolean(branch.condition.type)) {
                    // prune, always false
                    // TODO: Error probably
                    continue
                }

                // Accumulate typing information, but skip if literal NULL or MISSING
                typer.accumulate(branch.rex)
                newBranches.add(branch)
            }

            // Rewrite ELSE branch
            var newDefault = visitRex(node.default, null)
            typer.accumulate(newDefault)

            // Compute the CASE-WHEN type from the accumulator
            val (type, mapping) = typer.mapping()

            // Rewrite branches if we have coercions.
            if (mapping != null) {
                val msize = mapping.size
                val bsize = newBranches.size + 1
                assert(msize == bsize) { "Coercion mappings `len $msize` did not match the number of CASE-WHEN branches `len $bsize`" }
                // Rewrite branches
                for (i in newBranches.indices) {
                    when (val function = mapping[i]) {
                        null -> continue
                        else -> newBranches[i] = newBranches[i].copy(rex = replaceCaseBranch(newBranches[i].rex, type, function))
                    }
                }
                // Rewrite default
                val function = mapping.last()
                if (function != null) {
                    newDefault = replaceCaseBranch(newDefault, type, function)
                }
            }

            // TODO constant folding in planner which also means branch pruning
            // This is added for backwards compatibility, we return the first branch if it's true
            if (boolOrNull(newBranches[0].condition.op) == true) {
                return newBranches[0].rex
            }

            val op = Rex.Op.Case(newBranches, newDefault)
            return rex(type, op)
        }

        private fun replaceCaseBranch(originalRex: Rex, outputType: CompilerType, function: DynamicTyper.Mapping): Rex {
            return when (function) {
                is DynamicTyper.Mapping.Coercion -> {
                    val cast = env.resolveCast(originalRex, function.target)!!
                    Rex(outputType, cast)
                }
                is DynamicTyper.Mapping.Replacement -> {
                    function.replacement
                }
            }
        }

        // COALESCE(v1, v2,..., vN)
        // ==
        // CASE
        //     WHEN v1 IS NOT NULL THEN v1  -- WHEN branch always a boolean
        //     WHEN v2 IS NOT NULL THEN v2  -- WHEN branch always a boolean
        //     ... -- similarly for v3..vN-1
        //     ELSE vN
        // END
        // --> minimal common supertype of(<type v1>, <type v2>, ..., <type v3>)
        override fun visitRexOpCoalesce(node: Rex.Op.Coalesce, ctx: CompilerType?): Rex {
            val args = node.args.map { visitRex(it, it.type) }.toMutableList()
            val typer = DynamicTyper()
            args.forEach { v -> typer.accumulate(v) }
            val (type, mapping) = typer.mapping()
            if (mapping != null) {
                assert(mapping.size == args.size) { "Coercion mappings `len ${mapping.size}` did not match the number of COALESCE arguments `len ${args.size}`" }
                for (i in args.indices) {
                    when (val function = mapping[i]) {
                        null -> continue
                        else -> args[i] = replaceCaseBranch(args[i], type, function)
                    }
                }
            }
            val op = rexOpCoalesce(args)
            return rex(type, op)
        }

        // NULLIF(v1, v2)
        // ==
        // CASE
        //     WHEN v1 = v2 THEN NULL -- WHEN branch always a boolean
        //     ELSE v1
        // END
        // --> minimal common supertype of (NULL, <type v1>)
        override fun visitRexOpNullif(node: Rex.Op.Nullif, ctx: CompilerType?): Rex {
            val value = visitRex(node.value, node.value.type)
            val nullifier = visitRex(node.nullifier, node.nullifier.type)
            val typer = DynamicTyper()

            // Accumulate typing information
            typer.accumulate(value)
            val (type, _) = typer.mapping()
            val op = rexOpNullif(value, nullifier)
            return rex(type, op)
        }

        /**
         * In this context, Boolean means PartiQLValueType Bool, which can be nullable.
         * Hence, we permit Static Type BOOL, Static Type NULL, Static Type Missing here.
         */
        private fun canBeBoolean(type: CompilerType): Boolean {
            return type.code() == PType.DYNAMIC || type.code() == PType.VARIANT || type.code() == PType.BOOL
        }

        /**
         * Returns the boolean value of the expression. For now, only handle literals.
         */
        private fun boolOrNull(op: Rex.Op): Boolean? {
            return if (op is Rex.Op.Lit && op.value.type.code() == PType.BOOL) op.value.boolean else null
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
         * struct.
         */
        override fun visitRexOpCaseBranch(node: Rex.Op.Case.Branch, ctx: CompilerType?): Rex.Op.Case.Branch {
            val visitedCondition = visitRex(node.condition, node.condition.type)
            val visitedReturn = visitRex(node.rex, node.rex.type)
            return Rex.Op.Case.Branch(visitedCondition, visitedReturn)
        }

        override fun visitRexOpCollection(node: Rex.Op.Collection, ctx: CompilerType?): Rex {
            if (ctx!!.code() !in setOf(PType.ARRAY, PType.BAG)) {
                val problem = PErrors.typeUnexpected(null, ctx, listOf(PType.array(), PType.bag()))
                return errorRexAndReport(_listener, problem)
            }
            val values = node.values.map { visitRex(it, it.type) }
            val t = when (values.size) {
                0 -> PType.dynamic()
                else -> anyOfLiterals(values.map { it.type })!!
            }
            val type = when (ctx.code()) {
                PType.BAG -> PType.bag(t)
                PType.ARRAY -> PType.array(t)
                else -> error("This is impossible.")
            }
            return rex(CompilerType(type), rexOpCollection(values))
        }

        override fun visitRexOpStruct(node: Rex.Op.Struct, ctx: CompilerType?): Rex {
            val fields = node.fields.map {
                val k = visitRex(it.k, it.k.type)
                val v = visitRex(it.v, it.v.type)
                rexOpStructField(k, v)
            }
            var structIsClosed = true
            val structTypeFields = mutableListOf<CompilerType.PTypeField>()
            for (field in fields) {
                val keyOp = field.k.op
                // TODO: Check key type
                if (keyOp !is Rex.Op.Lit || !keyOp.value.isTextValue()) {
                    structIsClosed = false
                    continue
                }
                structTypeFields.add(CompilerType.PTypeField(keyOp.value.string, field.v.type))
            }
            val type = when (structIsClosed) {
                true -> CompilerType(PType.row(structTypeFields as Collection<PTypeField>))
                false -> CompilerType(PType.struct())
            }
            return rex(type, rexOpStruct(fields))
        }

        override fun visitRexOpPivot(node: Rex.Op.Pivot, ctx: CompilerType?): Rex {
            val stack = typeEnv.locals.outer + listOf(typeEnv.locals)
            val rel = node.rel.type(stack)
            val scope = Scope(rel.type.schema, stack)
            val typeEnv = TypeEnv(env, scope)
            val typer = RexTyper(typeEnv, Strategy.LOCAL)
            val key = typer.visitRex(node.key, null)
            val value = typer.visitRex(node.value, null)
            val op = rexOpPivot(key, value, rel)
            return rex(CompilerType(PType.struct()), op)
        }

        override fun visitRexOpSubquery(node: Rex.Op.Subquery, ctx: CompilerType?): Rex {
            val rel = node.rel.type(typeEnv.locals.outer + listOf(typeEnv.locals))
            val newScope = Scope(schema = rel.type.schema, outer = typeEnv.locals.outer + listOf(typeEnv.locals))
            val typeEnv = TypeEnv(env, newScope)
            val constructor = node.constructor.type(typeEnv)
            val subquery = rexOpSubquery(constructor, rel, node.coercion)
            return when (node.coercion) {
                Rex.Op.Subquery.Coercion.SCALAR -> visitRexOpSubqueryScalar(subquery, constructor.type)
                Rex.Op.Subquery.Coercion.ROW -> visitRexOpSubqueryRow(subquery, constructor.type)
            }
        }

        /**
         * Calculate output type of a row-value subquery.
         */
        private fun visitRexOpSubqueryRow(subquery: Rex.Op.Subquery, cons: CompilerType): Rex {
            if (cons.code() != PType.ROW) {
                error("Subquery with non-SQL SELECT cannot be coerced to a row-value expression. Found constructor type: $cons")
            }
            // Do a simple cardinality check for the moment.
            // TODO we can only check cardinality if we know we are in a a comparison operator.
            // val n = coercion.columns.size
            // val m = cons.fields.size
            // if (n != m) {
            //     return rexErr("Cannot coercion subquery with $m attributes to a row-value-expression with $n attributes")
            // }
            // If we made it this far, then we can coerce this subquery to the desired complex value
            val type = CompilerType(PType.array())
            return rex(type, subquery)
        }

        /**
         * Calculate output type of a scalar subquery.
         */
        private fun visitRexOpSubqueryScalar(subquery: Rex.Op.Subquery, cons: CompilerType): Rex {
            if (cons.code() == PType.DYNAMIC || cons.code() == PType.VARIANT) {
                return Rex(PType.dynamic().toCType(), subquery)
            }
            if (cons.code() != PType.ROW) {
                error("Subquery with non-SQL SELECT cannot be coerced to a scalar. Found constructor type: $cons")
            }
            val n = cons.fields.size
            if (n != 1) {
                env.listener.report(PErrors.degreeViolationScalarSubquery(n))
            }
            // If we made it this far, then we can coerce this subquery to a scalar
            val type = cons.fields.firstOrNull()?.type ?: PType.dynamic().toCType()
            return Rex(type, subquery)
        }

        // TODO: Should we support the ROW type?
        override fun visitRexOpSelect(node: Rex.Op.Select, ctx: CompilerType?): Rex {
            val rel = node.rel.type(typeEnv.locals.outer + listOf(typeEnv.locals))
            val newScope = Scope(schema = rel.type.schema, outer = typeEnv.locals.outer + listOf(typeEnv.locals))
            val typeEnv = TypeEnv(env, newScope)
            val constructor = node.constructor.type(typeEnv)
            val type = when (rel.isOrdered()) {
                true -> PType.array(constructor.type)
                false -> PType.bag(constructor.type)
            }
            return Rex(CompilerType(type), Rex.Op.Select(constructor, rel))
        }

        override fun visitRexOpTupleUnion(node: Rex.Op.TupleUnion, ctx: CompilerType?): Rex {
            val args = node.args.map { visitRex(it, ctx) }
            val result = Rex.Op.TupleUnion(args)

            // Replace Generated Tuple Union if Schema Present
            // This should occur before typing, however, we don't type on the AST or have an appropriate IR
            replaceGeneratedTupleUnion(result)?.let {
                return it
            }

            // Calculate Type
            val type = when (args.size) {
                0 -> CompilerType(PType.row(emptyList()))
                else -> {
                    val argTypes = args.map { it.type }
                    calculateTupleUnionOutputType(argTypes) ?: run {
                        val problem = PErrors.functionTypeMismatch(null, Identifier.regular("TUPLEUNION"), argTypes, emptyList())
                        return errorRexAndReport(_listener, problem)
                    }
                }
            }
            return Rex(type, result)
        }

        /**
         * This is a hack to replace the generated "TUPLEUNION" that is the result of a SELECT *. In my
         * opinion, this should actually occur prior to PlanTyper. That being said, we currently don't type on the AST,
         * and we don't have an appropriate IR to type on.
         *
         * @return null if the [node] is NOT a generated tuple union; return the replacement if the [node] is a tuple union
         * and there is sufficient schema to replace the tuple union
         */
        private fun replaceGeneratedTupleUnion(node: Rex.Op.TupleUnion): Rex? {
            val args = node.args.map { replaceGeneratedTupleUnionArg(it) }
            if (args.any { it == null }) {
                return null
            }
            // Infer Type
            val type = PType.row(args.flatMap { it!!.type.fields })
            val fields = args.flatMap { arg ->
                val op = arg!!.op
                when (op is Rex.Op.Struct) {
                    true -> op.fields
                    false -> {
                        arg.type.fields.map {
                            Rex.Op.Struct.Field(
                                rexString(it.name),
                                Rex(it.type, Rex.Op.Path.Key(arg, rexString(it.name)))
                            )
                        }
                    }
                }
            }
            // Create struct
            return Rex(type.toCType(), Rex.Op.Struct(fields))
        }

        private fun replaceGeneratedTupleUnionArg(node: Rex): Rex? {
            if (node.op is Rex.Op.Struct && node.type.code() == PType.ROW) {
                return node
            }
            val case = node.op as? Rex.Op.Case ?: return null
            if (case.branches.size != 1) {
                return null
            }
            val firstBranch = case.branches.first()
            val firstBranchCondition = case.branches.first().condition.op
            if (firstBranchCondition !is Rex.Op.Call.Static) {
                return null
            }
            if (!firstBranchCondition.fn.signature.name.equals(FunctionUtils.OP_IS_STRUCT, ignoreCase = true)) {
                return null
            }
            val firstBranchResultType = firstBranch.rex.type
            if (firstBranchResultType.code() != PType.ROW) {
                return null
            }
            return Rex(firstBranchResultType, firstBranch.rex.op)
        }

        override fun visitRexOpErr(node: Rex.Op.Err, ctx: CompilerType?): PlanNode {
            val type = ctx ?: CompilerType(PType.dynamic())
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
         * If any of the arguments are not a struct, we return null.
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
        private fun calculateTupleUnionOutputType(args: List<CompilerType>): CompilerType? {
            val fields = mutableListOf<CompilerType.PTypeField>()
            var structIsOpen = false
            var containsDynamic = false
            var containsNonStruct = false
            args.forEach { arg ->
                if (arg.code() == PType.UNKNOWN) {
                    return@forEach
                }
                when (arg.code()) {
                    PType.ROW -> fields.addAll(arg.fields)
                    PType.STRUCT -> structIsOpen = true
                    PType.DYNAMIC, PType.VARIANT -> containsDynamic = true
                    PType.UNKNOWN -> structIsOpen = true
                    else -> containsNonStruct = true
                }
            }
            return when {
                containsNonStruct -> null
                containsDynamic -> CompilerType(PType.dynamic())
                structIsOpen -> CompilerType(PType.struct())
                else -> CompilerType(PType.row(fields as Collection<PTypeField>))
            }
        }

        // Helpers

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

        fun resolveAgg(node: Rel.Op.Aggregate.Call.Unresolved): Pair<Rel.Op.Aggregate.Call, CompilerType> {
            // Type the arguments
            val args = node.args.map { visitRex(it, null) }
            val argsResolved = Rel.Op.Aggregate.Call.Unresolved(node.name, node.setq, args)

            // Resolve the function
            val call = env.resolveAgg(node.name, node.setq, args) ?: return argsResolved to CompilerType(PType.dynamic())
            // TODO pass argument types to compute the return type.
            val returnType = call.agg.signature.signature.returns
            return call to CompilerType(returnType)
        }
    }

    // HELPERS

    private fun Rel.type(stack: List<Scope>, strategy: Strategy = Strategy.LOCAL): Rel =
        RelTyper(stack, strategy).visitRel(this, null)

    /**
     * This types the [Rex] given the input record ([input]) and [stack] of [Scope] (representing the outer scopes).
     */
    private fun Rex.type(input: List<Rel.Binding>, stack: List<Scope>, strategy: Strategy = Strategy.LOCAL) =
        RexTyper(TypeEnv(env, Scope(input, stack)), strategy).visitRex(this, this.type)

    /**
     * This types the [Rex] given a [Scope]. We use the [Scope.schema] as the input schema and the [Scope.outer]
     * as the outer scopes/
     */
    private fun Rex.type(typeEnv: TypeEnv, strategy: Strategy = Strategy.LOCAL) =
        RexTyper(typeEnv, strategy).visitRex(this, this.type)

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
    private fun Rel.Type.copyWithSchema(types: List<CompilerType>): Rel.Type {
        assert(types.size == schema.size) { "Illegal copy, types size does not matching bindings list size" }
        return this.copy(schema = schema.mapIndexed { i, binding -> binding.copy(type = types[i]) })
    }

    private fun Rel.isOrdered(): Boolean = type.props.contains(Rel.Prop.ORDERED)

    private fun getElementTypeForFromSource(fromSourceType: CompilerType): CompilerType = when (fromSourceType.code()) {
        PType.DYNAMIC -> CompilerType(PType.dynamic())
        PType.BAG, PType.ARRAY -> fromSourceType.typeParameter
        // TODO: Should we emit a warning?
        else -> fromSourceType
    }

    private fun excludeBindings(input: List<Rel.Binding>, item: Rel.Op.Exclude.Path): List<Rel.Binding> {
        val output = input.map {
            when (val root = item.root) {
                is Rex.Op.Var.Unresolved -> {
                    when (root.identifier.hasQualifier()) {
                        true -> {
                            if (root.identifier.first().matches(it.name)) {
                                // recompute the StaticType of this binding after apply the exclusions
                                val type = it.type.exclude(item.steps, false)
                                it.copy(type = type)
                            } else {
                                it
                            }
                        }
                        else -> {
                            if (root.identifier.matches(it.name)) {
                                // recompute the PType of this binding after applying the exclusions
                                val type = it.type.exclude(item.steps, lastStepOptional = false)
                                it.copy(type = type)
                            } else {
                                it
                            }
                        }
                    }
                }
                is Rex.Op.Var.Local, is Rex.Op.Var.Global -> it
                else -> it
            }
        }
        return output
    }
}
