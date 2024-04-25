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
import org.partiql.planner.internal.ProblemGenerator
import org.partiql.planner.internal.exclude.ExcludeRepr
import org.partiql.planner.internal.ir.Constraint
import org.partiql.planner.internal.ir.DdlOp
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.PlanNode
import org.partiql.planner.internal.ir.Ref.Cast.Safety.UNSAFE
import org.partiql.planner.internal.ir.Rel
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.Statement
import org.partiql.planner.internal.ir.constraint
import org.partiql.planner.internal.ir.constraintBodyCheck
import org.partiql.planner.internal.ir.constraintBodyUnique
import org.partiql.planner.internal.ir.ddlOpCreateTable
import org.partiql.planner.internal.ir.identifierSymbol
import org.partiql.planner.internal.ir.rel
import org.partiql.planner.internal.ir.relBinding
import org.partiql.planner.internal.ir.relOpAggregate
import org.partiql.planner.internal.ir.relOpAggregateCallUnresolved
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
import org.partiql.planner.internal.ir.relType
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpCaseBranch
import org.partiql.planner.internal.ir.rexOpCoalesce
import org.partiql.planner.internal.ir.rexOpCollection
import org.partiql.planner.internal.ir.rexOpLit
import org.partiql.planner.internal.ir.rexOpNullif
import org.partiql.planner.internal.ir.rexOpPathIndex
import org.partiql.planner.internal.ir.rexOpPathKey
import org.partiql.planner.internal.ir.rexOpPathSymbol
import org.partiql.planner.internal.ir.rexOpPivot
import org.partiql.planner.internal.ir.rexOpSelect
import org.partiql.planner.internal.ir.rexOpStruct
import org.partiql.planner.internal.ir.rexOpStructField
import org.partiql.planner.internal.ir.rexOpSubquery
import org.partiql.planner.internal.ir.rexOpTupleUnion
import org.partiql.planner.internal.ir.statementDDL
import org.partiql.planner.internal.ir.statementQuery
import org.partiql.planner.internal.ir.util.PlanRewriter
import org.partiql.planner.internal.utils.PlanUtils
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnSignature
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
import org.partiql.types.StaticType.Companion.ANY
import org.partiql.types.StaticType.Companion.BOOL
import org.partiql.types.StaticType.Companion.MISSING
import org.partiql.types.StaticType.Companion.NULL
import org.partiql.types.StaticType.Companion.STRING
import org.partiql.types.StaticType.Companion.unionOf
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import org.partiql.value.BoolValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.TextValue
import org.partiql.value.boolValue
import org.partiql.value.stringValue

/**
 * Rewrites an untyped algebraic translation of the query to be both typed and have resolved variables.
 *
 * @property env
 */
@OptIn(PartiQLValueExperimental::class)
internal class PlanTyper(private val env: Env) {

    /**
     * Rewrite the statement with inferred types and resolved variables
     */
    fun resolve(statement: Statement): Statement =
        when (statement) {
            is Statement.DDL -> resolveDdl(statement)
            is Statement.Query -> resolveQuery(statement)
        }

    fun resolveDdl(statement: Statement.DDL): Statement {
        val statement = statement.type()
        return statement
    }

    fun resolveQuery(statement: Statement.Query): Statement.Query {
        val root = statement.root.type(emptyList(), emptyList(), Scope.GLOBAL)
        return statementQuery(root)
    }

    /**
     * Types the relational operators of a query expression.
     *
     * @property outer represents the outer variable scopes of a query expression — only used by scan variable resolution.
     * @property strategy
     */
    private inner class RelTyper(
        private val outer: List<TypeEnv>,
        private val strategy: Scope,
    ) : PlanRewriter<Rel.Type?>() {

        override fun visitRel(node: Rel, ctx: Rel.Type?) = visitRelOp(node.op, node.type) as Rel

        /**
         * The output schema of a `rel.op.scan` is the single value binding.
         */
        override fun visitRelOpScan(node: Rel.Op.Scan, ctx: Rel.Type?): Rel {
            // descend, with GLOBAL resolution strategy
            val rex = node.rex.type(emptyList(), outer, Scope.GLOBAL)
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
            val rex = node.rex.type(emptyList(), outer, Scope.GLOBAL)
            // compute rel type
            val valueT = getElementTypeForFromSource(rex.type)
            val indexT = StaticType.INT8
            val type = ctx!!.copyWithSchema(listOf(valueT, indexT))
            // rewrite
            val op = relOpScanIndexed(rex)
            return rel(type, op)
        }

        /**
         * TODO handle NULL|STRUCT type
         */
        override fun visitRelOpUnpivot(node: Rel.Op.Unpivot, ctx: Rel.Type?): Rel {
            val rex = node.rex.type(emptyList(), outer, Scope.GLOBAL)

            val kType = STRING
            val vTypes = rex.type.allTypes.map { type ->
                when (type) {
                    is StructType -> {
                        if ((type.contentClosed || type.constraints.contains(TupleConstraint.Open(false))) && type.fields.isNotEmpty()) {
                            unionOf(type.fields.map { it.value }.toSet()).flatten()
                        } else {
                            ANY
                        }
                    }
                    else -> type
                }
            }
            val vType = unionOf(vTypes.toSet()).flatten()

            // rewrite
            val type = ctx!!.copyWithSchema(listOf(kType, vType))
            val op = relOpUnpivot(rex)
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
            // TODO: Assert expression doesn't contain locals or upvalues.
            val limit = node.limit.type(input.type.schema, outer, Scope.GLOBAL)
            // check types
            if (limit.type.isNumeric().not()) {
                val err = ProblemGenerator.missingRex(
                    causes = listOf(limit.op),
                    problem = ProblemGenerator.unexpectedType(limit.type, setOf(StaticType.INT))
                )
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
            val offset = node.offset.type(input.type.schema, outer, Scope.GLOBAL)
            // check types
            if (offset.type.isNumeric().not()) {
                val err = ProblemGenerator.missingRex(
                    causes = listOf(offset.op),
                    problem = ProblemGenerator.unexpectedType(offset.type, setOf(StaticType.INT))
                )
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
            val stack = outer + listOf(TypeEnv(lhs.type.schema, outer))
            val rhs = RelTyper(stack, Scope.GLOBAL).visitRel(node.rhs, ctx)

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
            val condition = node.rex.type(TypeEnv(type.schema, outer))

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

        // TODO: better error reporting with exclude.
        override fun visitRelOpExclude(node: Rel.Op.Exclude, ctx: Rel.Type?): Rel {
            // compute input schema
            val input = visitRel(node.input, ctx)

            // apply exclusions to the input schema
            val init = input.type.schema.map { it.copy() }
            val schema = node.paths.fold((init)) { bindings, path -> excludeBindings(bindings, path) }

            // rewrite
            val type = ctx!!.copy(schema = schema)

            // resolve exclude path roots
            val newPaths = node.paths.map { path ->
                val resolvedRoot = when (val root = path.root) {
                    is Rex.Op.Var.Unresolved -> {
                        // resolve `root` to local binding
                        val locals = TypeEnv(input.type.schema, outer)
                        val path = root.identifier.toBindingPath()
                        val resolved = locals.resolve(path)
                        if (resolved == null) {
                            ProblemGenerator.missingRex(
                                emptyList(),
                                ProblemGenerator.unresolvedExcludedExprRoot(root.identifier)
                            ).op
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
            val typer = RexTyper(TypeEnv(input.type.schema, outer), Scope.LOCAL)

            // typing of aggregate calls is slightly more complicated because they are not expressions.
            val calls = node.calls.mapIndexed { i, call ->
                when (val agg = call) {
                    is Rel.Op.Aggregate.Call.Resolved -> call to ctx!!.schema[i].type
                    is Rel.Op.Aggregate.Call.Unresolved -> typer.resolveAgg(agg)
                }
            }
            val groups = node.groups.map { typer.visitRex(it, null) }

            // Compute schema using order (calls...groups...)
            val schema = mutableListOf<StaticType>()
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
     * We should consider making the StaticType? parameter non-nullable.
     *
     * @property locals TypeEnv in which this rex tree is evaluated.
     */
    @OptIn(PartiQLValueExperimental::class)
    private inner class RexTyper(
        private val locals: TypeEnv,
        private val strategy: Scope,
    ) : PlanRewriter<StaticType?>() {

        override fun visitRex(node: Rex, ctx: StaticType?): Rex = visitRexOp(node.op, node.type) as Rex

        override fun visitRexOpLit(node: Rex.Op.Lit, ctx: StaticType?): Rex {
            // type comes from RexConverter
            return rex(ctx!!, node)
        }

        override fun visitRexOpVarLocal(node: Rex.Op.Var.Local, ctx: StaticType?): Rex {
            val scope = locals.getScope(node.depth)
            assert(node.ref < scope.schema.size) {
                "Invalid resolved variable (var ${node.ref}, stack frame ${node.depth}) in env: $locals"
            }
            val type = scope.schema.getOrNull(node.ref)?.type ?: error("Can't find locals value.")
            return rex(type, node)
        }

        override fun visitRexOpMissing(node: Rex.Op.Missing, ctx: StaticType?): PlanNode {
            val type = ctx ?: MISSING
            return rex(type, node)
        }

        override fun visitRexOpVarUnresolved(node: Rex.Op.Var.Unresolved, ctx: StaticType?): Rex {
            val path = node.identifier.toBindingPath()
            val scope = when (node.scope) {
                Rex.Op.Var.Scope.DEFAULT -> strategy
                Rex.Op.Var.Scope.LOCAL -> Scope.LOCAL
            }
            val resolvedVar = when (scope) {
                Scope.LOCAL -> locals.resolve(path) ?: env.resolveObj(path)
                Scope.GLOBAL -> env.resolveObj(path) ?: locals.resolve(path)
            }
            if (resolvedVar == null) {
                val id = PlanUtils.externalize(node.identifier)
                val inScopeVariables = locals.schema.map { it.name }.toSet()
                val err = ProblemGenerator.errorRex(
                    causes = emptyList(),
                    problem = ProblemGenerator.undefinedVariable(id, inScopeVariables)
                )
                return err
            }
            return visitRex(resolvedVar, null)
        }

        override fun visitRexOpVarGlobal(node: Rex.Op.Var.Global, ctx: StaticType?): Rex = rex(node.ref.type, node)

        override fun visitRexOpPathIndex(node: Rex.Op.Path.Index, ctx: StaticType?): Rex {
            val root = visitRex(node.root, node.root.type)
            val key = visitRex(node.key, node.key.type)
            val elementTypes = root.type.allTypes.map { type ->
                if (type !is ListType && type !is SexpType) {
                    return@map MISSING
                }
                (type as CollectionType).elementType
            }.toSet()

            // TODO: For now we just log a single error.
            //  Ideally we can log more detailed information such as key not integer, etc.
            if (elementTypes.all { it is MissingType } || key.type !is IntType) {
                return ProblemGenerator.missingRex(
                    rexOpPathIndex(root, key),
                    ProblemGenerator.expressionAlwaysReturnsMissing("Path Navigation always returns MISSING")
                )
            }
            val finalType = unionOf(elementTypes).flatten()
            return rex(finalType.swallowAny(), rexOpPathIndex(root, key))
        }

        override fun visitRexOpPathKey(node: Rex.Op.Path.Key, ctx: StaticType?): Rex {
            val root = visitRex(node.root, node.root.type)
            val key = visitRex(node.key, node.key.type)

            // Check Key Type
            val toAddTypes = key.type.allTypes.mapNotNull { keyType ->
                when (keyType) {
                    is StringType -> null
                    is NullType -> NULL
                    else -> MISSING
                }
            }

            val pathTypes = root.type.allTypes.map { type ->
                val struct = type as? StructType ?: return@map MISSING

                if (key.op is Rex.Op.Lit) {
                    val lit = key.op.value
                    if (lit is TextValue<*> && !lit.isNull) {
                        val id = identifierSymbol(lit.string!!, Identifier.CaseSensitivity.SENSITIVE)
                        inferStructLookup(struct, id).first
                    } else {
                        return@map MISSING
                    }
                } else {
                    // cannot infer type of non-literal path step because we don't know its value
                    // we might improve upon this with some constant folding prior to typing
                    ANY
                }
            }.toSet()

            // TODO: For now, we just log a single error.
            //  Ideally we can add more details such as key is not an text, root is not a struct, etc.
            if (pathTypes.all { it == MISSING } ||
                (toAddTypes.size == key.type.allTypes.size && toAddTypes.all { it is MissingType }) // key value check
            ) return ProblemGenerator.missingRex(
                rexOpPathKey(root, key),
                ProblemGenerator.expressionAlwaysReturnsMissing("Path Navigation failed")
            )
            val finalType = unionOf(pathTypes + toAddTypes).flatten()
            return rex(finalType.swallowAny(), rexOpPathKey(root, key))
        }

        override fun visitRexOpPathSymbol(node: Rex.Op.Path.Symbol, ctx: StaticType?): Rex {
            val root = visitRex(node.root, node.root.type)

            val paths = root.type.allTypes.mapNotNull { type ->
                val struct = type as? StructType ?: return@mapNotNull null
                val (pathType, replacementId) = inferStructLookup(
                    struct, identifierSymbol(node.key, Identifier.CaseSensitivity.INSENSITIVE)
                )
                when (replacementId.caseSensitivity) {
                    Identifier.CaseSensitivity.INSENSITIVE -> rex(pathType, rexOpPathSymbol(root, replacementId.symbol))
                    Identifier.CaseSensitivity.SENSITIVE -> rex(
                        pathType, rexOpPathKey(root, rexString(replacementId.symbol))
                    )
                }
            }

            if (paths.isEmpty()) return ProblemGenerator.missingRex(
                rexOpPathSymbol(root, node.key),
                ProblemGenerator.expressionAlwaysReturnsMissing("Path Navigation failed - Expect Root to be of type Struct but is ${root.type}")
            )
            val type = unionOf(paths.map { it.type }.toSet()).flatten()
            if (type is MissingType) return ProblemGenerator.missingRex(
                rexOpPathSymbol(root, node.key),
                ProblemGenerator.expressionAlwaysReturnsMissing("Path Navigation always returns MISSING")
            )

            // replace step only if all are disambiguated
            val firstPathOp = paths.first().op
            val replacementOp = when (paths.map { it.op }.all { it == firstPathOp }) {
                true -> firstPathOp
                false -> rexOpPathSymbol(root, node.key)
            }
            return rex(type.swallowAny(), replacementOp)
        }

        /**
         * "Swallows" ANY. If ANY is one of the types in the UNION type, we return ANY. If not, we flatten and return
         * the [type].
         */
        private fun StaticType.swallowAny(): StaticType {
            val flattened = this.flatten()
            return when (flattened.allTypes.any { it is AnyType }) {
                true -> ANY
                false -> flattened
            }
        }

        private fun rexString(str: String) = rex(STRING, rexOpLit(stringValue(str)))

        override fun visitRexOpCastUnresolved(node: Rex.Op.Cast.Unresolved, ctx: StaticType?): Rex {
            val arg = visitRex(node.arg, null)
            val cast = env.resolveCast(arg, node.target) ?: return ProblemGenerator.errorRex(
                node.copy(node.target, arg),
                ProblemGenerator.undefinedFunction("CAST(<arg> AS ${node.target})", listOf(arg.type))
            )
            return visitRexOpCastResolved(cast, null)
        }

        override fun visitRexOpCastResolved(node: Rex.Op.Cast.Resolved, ctx: StaticType?): Rex {
            var type = node.cast.target.toNonNullStaticType()
            val nullable = node.arg.type.isNullable()
            if (nullable) {
                type = type.asNullable()
            }
            val missable = node.arg.type.isMissable() || node.cast.safety == UNSAFE
            if (missable) {
                type = unionOf(type, MISSING).flatten()
            }
            return rex(type, node)
        }

        override fun visitRexOpCallUnresolved(node: Rex.Op.Call.Unresolved, ctx: StaticType?): Rex {
            // Type the arguments
            val args = node.args.map { visitRex(it, null) }
            // Attempt to resolve in the environment
            val path = node.identifier.toBindingPath()
            val rex = env.resolveFn(path, args)
            if (rex == null) {
                return ProblemGenerator.errorRex(
                    causes = args.map { it.op },
                    problem = ProblemGenerator.undefinedFunction(node.identifier, args.map { it.type }),
                )
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
        @OptIn(FnExperimental::class)
        override fun visitRexOpCallStatic(node: Rex.Op.Call.Static, ctx: StaticType?): Rex {
            // Apply the coercions as explicit casts
            val args: List<Rex> = node.args.map {
                // Type the coercions
                when (val op = it.op) {
                    is Rex.Op.Cast.Resolved -> visitRexOpCastResolved(op, null)
                    else -> it
                }
            }
            // Infer fn return type
            val type = inferFnType(node.fn.signature, args)
            if (type is MissingType)
                return ProblemGenerator.missingRex(
                    node,
                    ProblemGenerator.expressionAlwaysReturnsMissing("function always returns missing"),
                )
            return rex(type, node)
        }

        /**
         * Typing of a dynamic function call.
         *
         * isMissable TRUE when the argument permutations may not definitively invoke one of the candidates.
         * You can think of [isMissable] as being the same as "not exhaustive". For example, if we have ABS(INT | STRING), then
         * this function call [isMissable] because there isn't an `ABS(STRING)` function signature AKA we haven't exhausted
         * all the arguments. On the other hand, take an "exhaustive" scenario: ABS(INT | DEC). In this case, [isMissable]
         * is false because we have functions for each potential argument AKA we have exhausted the arguments.
         *
         *
         * @param node
         * @param ctx
         * @return
         */
        @OptIn(FnExperimental::class)
        override fun visitRexOpCallDynamic(node: Rex.Op.Call.Dynamic, ctx: StaticType?): Rex {
            var isMissingCall = false
            val types = node.candidates.map { candidate ->
                isMissingCall = isMissingCall || candidate.fn.signature.isMissingCall
                inferFnType(candidate.fn.signature, node.args)
            }.toMutableSet()

            // We had a branch (arg type permutation) without a candidate.
            if (!node.exhaustive) {
                types.add(MISSING)
            }

            return rex(type = unionOf(types).flatten(), op = node)
        }

        override fun visitRexOpCase(node: Rex.Op.Case, ctx: StaticType?): Rex {
            // Rewrite CASE-WHEN branches
            val oldBranches = node.branches.toTypedArray()
            val newBranches = mutableListOf<Rex.Op.Case.Branch>()
            val typer = DynamicTyper()
            for (i in oldBranches.indices) {

                // Type the branch
                var branch = oldBranches[i]
                branch = visitRexOpCaseBranch(branch, branch.rex.type)

                // Check if branch condition is falsey
                if (boolOrNull(branch.condition.op) == false || branch.condition.type == MISSING) {
                    continue // prune
                }

                // Emit typing error if a branch condition is never a boolean (prune)
                if (!canBeBoolean(branch.condition.type)) {
                    // prune, always false
                    continue
                }

                // Accumulate typing information
                typer.accumulate(branch.rex.type)
                newBranches.add(branch)
            }

            // Rewrite ELSE branch
            var newDefault = visitRex(node.default, null)
            if (newBranches.isEmpty()) {
                return newDefault
            }
            typer.accumulate(newDefault.type)

            // Compute the CASE-WHEN type from the accumulator
            val (type, mapping) = typer.mapping()

            if (type is MissingType) {
                return ProblemGenerator.missingRex(
                    causes = newBranches.map { it.rex.op },
                    problem = ProblemGenerator.expressionAlwaysReturnsMissing("CASE-WHEN always returns missing"),
                )
            }

            // Rewrite branches if we have coercions.
            if (mapping != null) {
                val msize = mapping.size
                val bsize = newBranches.size + 1
                assert(msize == bsize) { "Coercion mappings `len $msize` did not match the number of CASE-WHEN branches `len $bsize`" }
                // Rewrite branches
                for (i in newBranches.indices) {
                    val (operand, target) = mapping[i]
                    if (operand == target) continue // skip
                    val branch = newBranches[i]
                    if (branch.rex.type is MissingType) {
                        continue // skip
                    }
                    val cast = env.resolveCast(branch.rex, target)!!
                    val rex = rex(type, cast)
                    newBranches[i] = branch.copy(rex = rex)
                }
                // Rewrite default
                val (operand, target) = mapping.last()
                if (operand != target) {
                    val cast = env.resolveCast(newDefault, target)!!
                    newDefault = rex(type, cast)
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

        // COALESCE(v1, v2,..., vN)
        // ==
        // CASE
        //     WHEN v1 IS NOT NULL THEN v1  -- WHEN branch always a boolean
        //     WHEN v2 IS NOT NULL THEN v2  -- WHEN branch always a boolean
        //     ... -- similarly for v3..vN-1
        //     ELSE vN
        // END
        // --> minimal common supertype of(<type v1>, <type v2>, ..., <type v3>)
        override fun visitRexOpCoalesce(node: Rex.Op.Coalesce, ctx: StaticType?): Rex {
            val args = node.args.map { visitRex(it, it.type) }.toMutableList()
            val typer = DynamicTyper()
            args.forEach { v ->
                typer.accumulate(v.type)
            }
            val (type, mapping) = typer.mapping()
            if (mapping != null) {
                assert(mapping.size == args.size) { "Coercion mappings `len ${mapping.size}` did not match the number of COALESCE arguments `len ${args.size}`" }
                for (i in args.indices) {
                    val (operand, target) = mapping[i]
                    if (operand == target) continue // skip; no coercion needed
                    val cast = env.resolveCast(args[i], target)
                    if (cast != null) {
                        val rex = rex(type, cast)
                        args[i] = rex
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
        override fun visitRexOpNullif(node: Rex.Op.Nullif, ctx: StaticType?): Rex {
            val value = visitRex(node.value, node.value.type)
            val nullifier = visitRex(node.nullifier, node.nullifier.type)
            val typer = DynamicTyper()
            typer.accumulate(NULL)
            typer.accumulate(value.type)
            val (type, _) = typer.mapping()
            val op = rexOpNullif(value, nullifier)
            return rex(type, op)
        }

        /**
         * In this context, Boolean means PartiQLValueType Bool, which can be nullable.
         * Hence, we permit Static Type BOOL, Static Type NULL, Static Type Missing here.
         */
        private fun canBeBoolean(type: StaticType): Boolean {
            return type.flatten().allTypes.any {
                // TODO: This is a quick fix to unblock the typing or case expression.
                //  We need to model the truth value better in typer.
                it is BoolType || it is NullType || it is MissingType
            }
        }

        /**
         * Returns the boolean value of the expression. For now, only handle literals.
         */
        @OptIn(PartiQLValueExperimental::class)
        private fun boolOrNull(op: Rex.Op): Boolean? {
            return if (op is Rex.Op.Lit && op.value is BoolValue) op.value.value else null
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
         * This takes in a branch condition and its result expression.
         *
         *  1. If the condition is a type check T (ie `<var> IS T`), then this function will be typed as T.
         *  2. If a branch condition is known to be false, it will be removed.
         *
         * TODO: Currently, this only folds type checking for STRUCTs. We need to add support for all other types.
         *
         * TODO: I added a check for [Rex.Op.Var.Outer] as it seemed odd to replace a general expression like:
         *  `WHEN { 'a': { 'b': 1} }.a IS STRUCT THEN { 'a': { 'b': 1} }.a.b`. We can discuss this later, but I'm
         *  currently limiting the scope of this intentionally.
         */
        @OptIn(FnExperimental::class)
        private fun foldCaseBranch(condition: Rex, result: Rex): Rex.Op.Case.Branch {
            return when (val call = condition.op) {
                is Rex.Op.Call.Dynamic -> {
                    val rex = call.candidates.map { candidate ->
                        val fn = candidate.fn
                        if (fn.signature.name.equals("is_struct", ignoreCase = true).not()) {
                            return rexOpCaseBranch(condition, result)
                        }
                        val ref = call.args.getOrNull(0) ?: error("IS STRUCT requires an argument.")
                        // Replace the result's type
                        val type = AnyOfType(ref.type.allTypes.filterIsInstance<StructType>().toSet())
                        val replacementVal = ref.copy(type = type)
                        when (ref.op is Rex.Op.Var.Local) {
                            true -> RexReplacer.replace(result, ref, replacementVal)
                            false -> result
                        }
                    }
                    val type = rex.toUnionType().flatten()
                    return rexOpCaseBranch(condition, result.copy(type))
                }
                is Rex.Op.Call.Static -> {
                    val fn = call.fn
                    if (fn.signature.name.equals("is_struct", ignoreCase = true).not()) {
                        return rexOpCaseBranch(condition, result)
                    }
                    val ref = call.args.getOrNull(0) ?: error("IS STRUCT requires an argument.")
                    val simplifiedCondition = when {
                        ref.type.allTypes.all { it is StructType } -> rex(BOOL, rexOpLit(boolValue(true)))
                        ref.type.allTypes.none { it is StructType } -> rex(BOOL, rexOpLit(boolValue(false)))
                        else -> condition
                    }

                    // Replace the result's type
                    val type = AnyOfType(ref.type.allTypes.filterIsInstance<StructType>().toSet()).flatten()
                    val replacementVal = ref.copy(type = type)
                    val rex = when (ref.op is Rex.Op.Var.Local) {
                        true -> RexReplacer.replace(result, ref, replacementVal)
                        false -> result
                    }
                    return rexOpCaseBranch(simplifiedCondition, rex)
                }
                else -> rexOpCaseBranch(condition, result)
            }
        }

        override fun visitRexOpCollection(node: Rex.Op.Collection, ctx: StaticType?): Rex {
            if (ctx!! !is CollectionType) {
                return ProblemGenerator.missingRex(
                    node,
                    ProblemGenerator.unexpectedType(ctx, setOf(StaticType.LIST, StaticType.BAG, StaticType.SEXP))
                )
            }
            val values = node.values.map { visitRex(it, it.type) }
            val t = when (values.size) {
                0 -> ANY
                else -> values.toUnionType()
            }
            val type = when (ctx as CollectionType) {
                is BagType -> BagType(t)
                is ListType -> ListType(t)
                is SexpType -> SexpType(t)
            }
            return rex(type, rexOpCollection(values))
        }

        @OptIn(PartiQLValueExperimental::class)
        override fun visitRexOpStruct(node: Rex.Op.Struct, ctx: StaticType?): Rex {
            val fields = node.fields.mapNotNull {
                val k = visitRex(it.k, it.k.type)
                val v = visitRex(it.v, it.v.type)
                if (k.op is Rex.Op.Err || k.op is Rex.Op.Missing || v.op is Rex.Op.Err || v.op is Rex.Op.Missing) {
                    rexOpStructField(k, v)
                } else if (v.type is MissingType) {
                    null
                } else {
                    rexOpStructField(k, v)
                }
            }
            var structIsClosed = true
            val structTypeFields = mutableListOf<StructType.Field>()
            val structKeysSeent = mutableSetOf<String>()
            for (field in fields) {
                when (field.k.op) {
                    is Rex.Op.Lit -> {
                        // A field is only included in the StructType if its key is a text literal
                        val key = field.k.op
                        if (key.value is TextValue<*>) {
                            val name = key.value.string!!
                            val type = field.v.type
                            structKeysSeent.add(name)
                            if (field.v.type !is MissingType) {
                                structTypeFields.add(StructType.Field(name, type))
                            }
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
            val stack = locals.outer + listOf(locals)
            val rel = node.rel.type(stack)
            val typeEnv = TypeEnv(rel.type.schema, stack)
            val typer = RexTyper(typeEnv, Scope.LOCAL)
            val key = typer.visitRex(node.key, null)
            val value = typer.visitRex(node.value, null)
            val type = StructType(
                contentClosed = false, constraints = setOf(TupleConstraint.Open(true))
            )
            val op = rexOpPivot(key, value, rel)
            return rex(type, op)
        }

        override fun visitRexOpSubquery(node: Rex.Op.Subquery, ctx: StaticType?): Rex {
            val rel = node.rel.type(locals.outer + listOf(locals))
            val newTypeEnv = TypeEnv(schema = rel.type.schema, outer = locals.outer + listOf(locals))
            val constructor = node.constructor.type(newTypeEnv)
            val subquery = rexOpSubquery(constructor, rel, node.coercion)
            return when (node.coercion) {
                Rex.Op.Subquery.Coercion.SCALAR -> visitRexOpSubqueryScalar(subquery, constructor.type)
                Rex.Op.Subquery.Coercion.ROW -> visitRexOpSubqueryRow(subquery, constructor.type)
            }
        }

        /**
         * Calculate output type of a row-value subquery.
         */
        private fun visitRexOpSubqueryRow(subquery: Rex.Op.Subquery, cons: StaticType): Rex {
            if (cons !is StructType) {
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
            val type = StaticType.LIST
            val op = subquery
            return rex(type, op)
        }

        /**
         * Calculate output type of a scalar subquery.
         */
        private fun visitRexOpSubqueryScalar(subquery: Rex.Op.Subquery, cons: StaticType): Rex {
            if (cons !is StructType) {
                error("Subquery with non-SQL SELECT cannot be coerced to a scalar. Found constructor type: $cons")
            }
            val n = cons.fields.size
            if (n != 1) {
                error("SELECT constructor with $n attributes cannot be coerced to a scalar. Found constructor type: $cons")
            }
            // If we made it this far, then we can coerce this subquery to a scalar
            val type = cons.fields.first().value
            val op = subquery
            return rex(type, op)
        }

        override fun visitRexOpSelect(node: Rex.Op.Select, ctx: StaticType?): Rex {
            val rel = node.rel.type(locals.outer + listOf(locals))
            val newTypeEnv = TypeEnv(schema = rel.type.schema, outer = locals.outer + listOf(locals))
            var constructor = node.constructor.type(newTypeEnv)
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
                0 -> {
                    // empty struct
                    StructType(
                        fields = emptyMap(),
                        contentClosed = true,
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered,
                        )
                    )
                }
                else -> {
                    val argTypes = args.map { it.type }
                    val potentialTypes = buildArgumentPermutations(argTypes).map { argumentList ->
                        calculateTupleUnionOutputType(argumentList)
                    }
                    unionOf(potentialTypes.toSet()).flatten()
                }
            }
            val op = rexOpTupleUnion(args)
            return rex(type, op)
        }

        override fun visitRexOpErr(node: Rex.Op.Err, ctx: StaticType?): PlanNode {
            val type = ctx ?: ANY
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
                        error("TupleUnion wasn't normalized to exclude union types.")
                    }
                    is NullType -> {
                        return NULL
                    }
                    else -> {
                        return MISSING
                    }
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
                TupleConstraint.Open(!structIsClosed), TupleConstraint.UniqueAttrs(uniqueAttrs), orderedConstraint
            )
            return StructType(
                fields = structFields.map { it }, contentClosed = structIsClosed, constraints = constraints
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

        private fun buildArgumentPermutations(
            args: List<List<StaticType>>,
            accumulator: List<StaticType>,
        ): Sequence<List<StaticType>> {
            if (args.isEmpty()) {
                return sequenceOf(accumulator)
            }
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

        /**
         * Logic is as follows:
         * 1. If [struct] is closed and ordered:
         *   - If no item is found, return [MissingType]
         *   - Else, grab first matching item and make sensitive.
         * 2. If [struct] is closed
         *   - AND no item is found, return [MissingType]
         *   - AND only one item is present -> grab item and make sensitive.
         *   - AND more than one item is present, keep sensitivity and grab item.
         * 3. If [struct] is open, return [AnyType]
         *
         * @return a [Pair] where the [Pair.first] represents the type of the [step] and the [Pair.second] represents
         * the disambiguated [key].
         */
        private fun inferStructLookup(struct: StructType, key: Identifier.Symbol): Pair<StaticType, Identifier.Symbol> {
            val binding = key.toBindingName()
            val isClosed = struct.constraints.contains(TupleConstraint.Open(false))
            val isOrdered = struct.constraints.contains(TupleConstraint.Ordered)
            val (name, type) = when {
                // 1. Struct is closed and ordered
                isClosed && isOrdered -> {
                    struct.fields.firstOrNull { entry -> binding.matches(entry.key) }?.let {
                        (sensitive(it.key) to it.value)
                    } ?: (key to MISSING)
                }
                // 2. Struct is closed
                isClosed -> {
                    val matches = struct.fields.filter { entry -> binding.matches(entry.key) }
                    when (matches.size) {
                        0 -> (key to MISSING)
                        1 -> matches.first().let { (sensitive(it.key) to it.value) }
                        else -> {
                            val firstKey = matches.first().key
                            val sharedKey = when (matches.all { it.key == firstKey }) {
                                true -> sensitive(firstKey)
                                false -> key
                            }
                            sharedKey to unionOf(matches.map { it.value }.toSet()).flatten()
                        }
                    }
                }
                // 3. Struct is open
                else -> (key to ANY)
            }
            return type to name
        }

        private fun sensitive(str: String): Identifier.Symbol =
            identifierSymbol(str, Identifier.CaseSensitivity.SENSITIVE)

        @OptIn(FnExperimental::class)
        private fun inferFnType(fn: FnSignature, args: List<Rex>): StaticType {

            // Determine role of NULL and MISSING in the return type
            var hadNull = false
            var hadNullable = false
            var hadMissing = false
            var hadMissable = false
            for (arg in args) {
                val t = arg.type
                when {
                    t is MissingType -> hadMissing = true
                    t is NullType -> hadNull = true
                    t.isMissable() -> hadMissable = true
                    t.isNullable() -> hadNullable = true
                }
            }

            // True iff NULL CALL and had a NULL arg;
            val isNull = (fn.isNullCall && hadNull)

            // True iff NULL CALL and had a NULLABLE arg; or is a NULLABLE operator
            val isNullable = (fn.isNullCall && hadNullable) || fn.isNullable

            // True iff MISSING CALL and had a MISSING arg.
            val isMissing = fn.isMissingCall && hadMissing

            // True iff MISSING CALL and had a MISSABLE arg
            val isMissable = (fn.isMissingCall && hadMissable) && fn.isMissable

            // Return type with calculated nullability
            var type: StaticType = when {
                isMissing -> MISSING
                // Edge cases for EQ and boolean connective
                // If function can not return missing or null, can not propagate missing or null
                // AKA, the Function IS MISSING
                // return signature return type
                !fn.isMissable && !fn.isMissingCall && !fn.isNullable && !fn.isNullCall -> fn.returns.toNonNullStaticType()
                isNull || (!fn.isMissable && hadMissing) -> NULL
                isNullable -> fn.returns.toStaticType()
                else -> fn.returns.toNonNullStaticType()
            }

            // Propagate MISSING unless this operator explicitly doesn't return missing (fn.isMissable = false).
            if (isMissable) {
                type = unionOf(type, MISSING)
            }

            return type.flatten()
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
        @OptIn(FnExperimental::class)
        fun resolveAgg(node: Rel.Op.Aggregate.Call.Unresolved): Pair<Rel.Op.Aggregate.Call, StaticType> {
            // Type the arguments
            var isMissable = false
            val args = node.args.map { visitRex(it, null) }
            val argsResolved = relOpAggregateCallUnresolved(node.name, node.setQuantifier, args)

            // Resolve the function
            val call = env.resolveAgg(node.name, node.setQuantifier, args) ?: return argsResolved to ANY
            if (args.any { it.type == MISSING }) return argsResolved to MISSING
            if (args.any { it.type.isMissable() }) isMissable = true

            // Treat MISSING as NULL in aggregations.
            val isNullable = call.agg.signature.isNullable || isMissable
            val returns = call.agg.signature.returns
            val type: StaticType = when {
                isNullable -> returns.toStaticType()
                else -> returns.toNonNullStaticType()
            }
            //
            return call to type
        }
    }

    // HELPERS

    private fun Rel.type(stack: List<TypeEnv>, strategy: Scope = Scope.LOCAL): Rel =
        RelTyper(stack, strategy).visitRel(this, null)

    /**
     * This types the [Rex] given the input record ([input]) and [stack] of [TypeEnv] (representing the outer scopes).
     */
    private fun Rex.type(input: List<Rel.Binding>, stack: List<TypeEnv>, strategy: Scope = Scope.LOCAL) =
        RexTyper(TypeEnv(input, stack), strategy).visitRex(this, this.type)

    /**
     * This types the [Rex] given a [TypeEnv]. We use the [TypeEnv.schema] as the input schema and the [TypeEnv.outer]
     * as the outer scopes/
     */
    private fun Rex.type(typeEnv: TypeEnv, strategy: Scope = Scope.LOCAL) =
        RexTyper(typeEnv, strategy).visitRex(this, this.type)

    private fun Statement.DDL.type() = DdlTyper().visitStatementDDL(this, ANY)

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
        return this.copy(schema = schema.mapIndexed { i, binding -> binding.copy(type = types[i]) })
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

    private fun Rel.isOrdered(): Boolean = type.props.contains(Rel.Prop.ORDERED)

    /**
     * Produce a union type from all the
     */
    private fun List<Rex>.toUnionType(): StaticType = AnyOfType(map { it.type }.toSet()).flatten()

    private fun getElementTypeForFromSource(fromSourceType: StaticType): StaticType = when (fromSourceType) {
        is BagType -> fromSourceType.elementType
        is ListType -> fromSourceType.elementType
        is AnyType -> ANY
        is AnyOfType -> AnyOfType(fromSourceType.types.map { getElementTypeForFromSource(it) }.toSet())
        // All the other types coerce into a bag of themselves (including null/missing/sexp).
        else -> fromSourceType
    }

    // HELPERS

    private fun Identifier.debug(): String = when (this) {
        is Identifier.Qualified -> (listOf(root.debug()) + steps.map { it.debug() }).joinToString(".")
        is Identifier.Symbol -> when (caseSensitivity) {
            Identifier.CaseSensitivity.SENSITIVE -> "\"$symbol\""
            Identifier.CaseSensitivity.INSENSITIVE -> symbol
        }
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

    private fun excludeBindings(input: List<Rel.Binding>, item: Rel.Op.Exclude.Path): List<Rel.Binding> {
        var matchedRoot = false
        val output = input.map {
            when (val root = item.root) {
                is Rex.Op.Var.Unresolved -> {
                    when (val id = root.identifier) {
                        is Identifier.Symbol -> {
                            if (id.isEquivalentTo(it.name)) {
                                matchedRoot = true
                                // recompute the StaticType of this binding after applying the exclusions
                                val type = it.type.exclude(item.steps, lastStepOptional = false)
                                it.copy(type = type)
                            } else {
                                it
                            }
                        }
                        is Identifier.Qualified -> it
                    }
                }
                is Rex.Op.Var.Local, is Rex.Op.Var.Global -> it
                else -> it
            }
        }
        return output
    }

    private fun Identifier.Symbol.isEquivalentTo(other: String): Boolean = when (caseSensitivity) {
        Identifier.CaseSensitivity.SENSITIVE -> symbol.equals(other)
        Identifier.CaseSensitivity.INSENSITIVE -> symbol.equals(other, ignoreCase = true)
    }

    /**
     * Pretty-print a path and its root type.
     *
     * @return
     */
    private fun Rex.Op.Path.debug(): String {
        val steps = mutableListOf<String>()
        var curr: Rex = rex(ANY, this)
        while (true) {
            curr = when (val op = curr.op) {
                is Rex.Op.Path.Index -> {
                    steps.add("${op.key}")
                    op.root
                }
                is Rex.Op.Path.Key -> {
                    val k = op.key.op
                    if (k is Rex.Op.Lit && k.value is TextValue<*>) {
                        steps.add("${k.value.string}")
                    } else {
                        steps.add("${op.key}")
                    }
                    op.root
                }
                is Rex.Op.Path.Symbol -> {
                    steps.add(op.key)
                    op.root
                }
                else -> break
            }
        }
        // curr is root
        return "`${steps.joinToString(".")}` on root $curr"
    }

    // For now: Verify check constraint expression is valid PartiQL and is typed to Boolean
    private inner class DdlTyper() : PlanRewriter<StaticType>() {

        override fun visitStatementDDL(node: Statement.DDL, ctx: StaticType): Statement.DDL {
            val op = when (node.op) {
                is DdlOp.CreateTable -> visitDdlOpCreateTable(node.op, ctx)
            }
            return statementDDL(op)
        }

        override fun visitDdlOpCreateTable(node: DdlOp.CreateTable, ctx: StaticType): DdlOp {
            val name = node.name
            val shape = node.shape
            val constraints = node.constraint.map { visitConstraint(it, shape) }
            return ddlOpCreateTable(node.name, shape, constraints, node.partitionExpr, node.tableProperties)
        }

        override fun visitConstraint(node: Constraint, ctx: StaticType): Constraint {
            return when (node.body) {
                is Constraint.Body.Check -> constraint(node.name, visitConstraintBodyCheck(node.body, ctx))
                is Constraint.Body.Unique -> constraint(node.name, visitConstraintBodyUnique(node.body, ctx))
            }
        }

        // verfiy check constraint. using rex typer to assert on valid expression
        // Constraint handler to lower if needed.
        override fun visitConstraintBodyCheck(node: Constraint.Body.Check, ctx: StaticType): Constraint.Body.Check {
            // construct Type Env
            val shapeStruct = (ctx as BagType).elementType as StructType
            val bindings = shapeStruct.fields.map {
                Rel.Binding(
                    it.key,
                    it.value
                )
            }
            val typeEnv = TypeEnv(bindings, emptyList())
            val resolvedRex = RexTyper(typeEnv, Scope.LOCAL).visitRex(node.lowered, null)
            val resolvedType = StaticType.unionOf(resolvedRex.type.allTypes.filterNot { it is NullType }.toSet()).flatten()

            if (resolvedType !is BoolType) TODO("Check expression not type to boolean")
            return constraintBodyCheck(resolvedRex, node.unlowered)
        }

        override fun visitConstraintBodyUnique(node: Constraint.Body.Unique, ctx: StaticType) =
            constraintBodyUnique(node.columns, node.isPrimaryKey)
    }
}
