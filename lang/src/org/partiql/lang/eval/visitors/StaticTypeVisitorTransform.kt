/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package org.partiql.lang.eval.visitors

import com.amazon.ion.IonSystem
import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.toIonElement
import org.partiql.lang.ast.StaticTypeMeta
import org.partiql.lang.ast.metaContainerOf
import org.partiql.lang.ast.passes.SemanticException
import org.partiql.lang.ast.toIonElementMetaContainer
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.addSourceLocation
import org.partiql.lang.domains.extractSourceLocation
import org.partiql.lang.domains.toBindingCase
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.delegate
import org.partiql.lang.types.StaticType
import org.partiql.lang.util.propertyValueMapOf

/**
 * Extra constraints which may be imposed on the type checking.
 */
enum class StaticTypeVisitorTransformConstraints {

    /**
     * With this constraint, any VariableReferences in SFW queries must be
     * defined within the FROM clause.
     *
     * This provides for variable resolution which is akin to what users of a
     * traditional RDBMS would expect.
     */
    PREVENT_GLOBALS_EXCEPT_IN_FROM,

    /**
     * With this constraint, all variables from nested queries must resolve to
     * lexically scoped variables.
     *
     * Constraining a user's access to global binds is useful for simplified
     * query planning in a DB environment where global binds may be costly to
     * fetch or lead to large intermediate results.
     */
    PREVENT_GLOBALS_IN_NESTED_QUERIES;
}

/**
 * A [PartiqlAst.VisitorTransform] that annotates nodes with their static types and resolves implicit variables
 * explicitly based on the static types.
 *
 * The validations performed may be enhanced by the passing of additional [StaticTypeVisitorTransformConstraints].
 *
 * @param globalEnv The global bindings to the static environment.  This is data catalog purely from a lookup
 *                  perspective.
 * @param constraints Additional constraints on what variable scoping, or other rules should be followed.
 */
class StaticTypeVisitorTransform(private val ion: IonSystem,
                                 globalBindings: Bindings<StaticType>,
                                 constraints: Set<StaticTypeVisitorTransformConstraints> = setOf()) : PartiqlAst.VisitorTransform() {

    /** Used to allow certain binding lookups to occur directly in the global scope. */
    private val globalEnv = wrapBindings(globalBindings, 0)

    private val preventGlobalsExceptInFrom =
        StaticTypeVisitorTransformConstraints.PREVENT_GLOBALS_EXCEPT_IN_FROM in constraints

    private val preventGlobalsInNestedQueries =
        StaticTypeVisitorTransformConstraints.PREVENT_GLOBALS_IN_NESTED_QUERIES in constraints

    /** Captures a [StaticType] and the depth at which it is bound. */
    private data class TypeAndDepth(val type: StaticType, val depth: Int)

    /** Indicates the scope at which a particular bind was found. */
    private enum class BindingScope {

        /** Describes a binding to a variable defined within the same statement. */
        LOCAL,

        /**
         * Describes a binding to a variable defined within the overall scope
         * of a statement. With nested statements, a variable binding from an
         * outer scope would be [LEXICAL], not [LOCAL].
         */
        LEXICAL,

        /**
         * A binding to a variable defined in the DB environment, not within
         * the user's statement.
         */
        GLOBAL;
    }

    /** Captures a [StaticType] and what scope it is bound within. */
    private data class TypeAndScope(val type: StaticType, val scope: BindingScope)

    /** Defines the current scope search order--i.e. globals first when in a FROM source, lexical everywhere else. */
    private enum class ScopeSearchOrder {
        LEXICAL,
        GLOBALS_THEN_LEXICAL
    }

    /**
     * @param parentEnv the enclosing bindings
     * @param currentScopeDepth How deeply nested the current scope is.
     * - 0 means we are in the global scope
     * - 1 is the top-most statement with a `FROM` clause (i.e. select-from-where or DML operation),
     * - Values > 1 are for each subsequent level of nested sub-query.
     */
    private inner class VisitorTransform(private val parentEnv: Bindings<TypeAndDepth>,
                                         private val currentScopeDepth: Int) : PartiqlAst.VisitorTransform() {

        /** Specifies the current scope search order--default is LEXICAL. */
        private var scopeOrder = ScopeSearchOrder.LEXICAL

        private val localsMap = mutableMapOf<String, StaticType>()

        // TODO this used to use a wrapper over localsMap, but that API no longer exists, we should figure something
        //      more reasonable out later for this at some point, but this is good enough for now
        private var localsOnlyEnv = wrapBindings(Bindings.ofMap(localsMap), currentScopeDepth)

        // because of the mutability of the above reference, we need to encode the lookup as a thunk
        private val currentEnv = Bindings.over { localsOnlyEnv[it] }.delegate(parentEnv)

        private var containsJoin = false

        /** Set to true after any FROM source has been visited.*/
        private var fromVisited = false

        /**
         * In short, after the FROM sources have been visited, this is set to the name if-and-only-if there is
         * a single from source.  Otherwise, it is null.
         */
        private var singleFromSourceName: String? = null

        private fun singleFromSourceRef(sourceName: String, metas: MetaContainer): PartiqlAst.Expr.Id {
            val sourceType = currentEnv[BindingName(sourceName, BindingCase.SENSITIVE)] ?:
                throw IllegalArgumentException("Could not find type for single FROM source variable")

            return PartiqlAst.build {
                id(sourceName,
                    caseSensitive(),
                    localsFirst(),
                    metas + metaContainerOf(StaticTypeMeta(sourceType.type)).toIonElementMetaContainer())
            }
        }

        private fun PartiqlAst.Expr.Id.toPathExpr(): PartiqlAst.PathStep.PathExpr =
            PartiqlAst.build {
                pathExpr(index = lit(ion.newString(name.text).toIonElement(), this@toPathExpr.extractSourceLocation()), case = case)
            }

        private fun errUnboundName(name: String, metas: MetaContainer): Nothing = throw SemanticException(
            "No such variable named '$name'",
            ErrorCode.SEMANTIC_UNBOUND_BINDING,
            propertyValueMapOf(
                Property.BINDING_NAME to name
            ).addSourceLocation(metas)
        )

        private fun errIllegalGlobalVariableAccess(name: String, metas: MetaContainer): Nothing = throw SemanticException(
            "Global variable access is illegal in this context",
            ErrorCode.SEMANTIC_ILLEGAL_GLOBAL_VARIABLE_ACCESS,
            propertyValueMapOf(
                Property.BINDING_NAME to name
            ).addSourceLocation(metas)
        )

        private fun errAmbiguousName(name: String, metas: MetaContainer): Nothing = throw SemanticException(
            "A variable named '$name' was already defined in this scope",
            ErrorCode.SEMANTIC_AMBIGUOUS_BINDING,
            propertyValueMapOf(
                Property.BINDING_NAME to name
            ).addSourceLocation(metas)
        )

        private fun errUnimplementedFeature(name: String, metas: MetaContainer? = null): Nothing = throw SemanticException(
            "Feature not implemented yet",
            ErrorCode.UNIMPLEMENTED_FEATURE,
            propertyValueMapOf(
                Property.FEATURE_NAME to name
            ).also {
                if (metas != null) {
                    it.addSourceLocation(metas)
                }
            }
        )

        private fun addLocal(name: String, type: StaticType, metas: MetaContainer) {
            val existing = localsOnlyEnv[BindingName(name, BindingCase.INSENSITIVE)]
            if (existing != null) {
                errAmbiguousName(name, metas)
            }
            localsMap[name] = type
            // this requires a new instance because of how [Bindings.ofMap] works
            localsOnlyEnv = wrapBindings(Bindings.ofMap(localsMap), currentScopeDepth)
        }

        override fun transformExprCallAgg(node: PartiqlAst.Expr.CallAgg): PartiqlAst.Expr {
            return PartiqlAst.build {
                callAgg_(
                    // do not transform the funcExpr--as this is a symbolic name in another namespace (AST is over generalized here)
                    node.setq,
                    node.funcName,
                    transformExpr(node.arg),
                    transformMetas(node.metas))
            }
        }

        // TODO: find if this override shouldn't be necessary because PIG's Call doesn't store the function call name as the first arg
        override fun transformExprCall(node: PartiqlAst.Expr.Call): PartiqlAst.Expr {
            // do not write the name of the call--this should be a symbolic name in another namespace (AST is over generalized here)
            return PartiqlAst.build {
                call_(
                    node.funcName,
                    node.args.map { transformExpr(it) },
                    transformMetas(node.metas)
                )
            }
        }

        private fun Bindings<TypeAndDepth>.lookupBinding(bindingName: BindingName): TypeAndScope? =
            when (val match = this[bindingName]) {
                null -> null
                else -> {
                    val (type, depth) = match
                    val scope = when {
                        depth == 0                 -> BindingScope.GLOBAL
                        depth < currentScopeDepth  -> BindingScope.LEXICAL
                        depth == currentScopeDepth -> BindingScope.LOCAL
                        else                       -> error("Unexpected: depth should never be > currentScopeDepth")
                    }
                    TypeAndScope(type, scope)
                }
            }

        /**
         * Encapsulates variable reference lookup, layering the scoping
         * rules from the exclusions given the current state.
         *
         * Returns an instance of [TypeAndScope] if the binding was found, otherwise returns null.
         */
        private fun findBind(bindingName: BindingName, scopeQualifier: PartiqlAst.ScopeQualifier): TypeAndScope? {
            // Override the current scope search order if the var is lexically qualified.
            val overridenScopeSearchOrder = when(scopeQualifier) {
                is PartiqlAst.ScopeQualifier.LocalsFirst -> ScopeSearchOrder.LEXICAL
                is PartiqlAst.ScopeQualifier.Unqualified -> this.scopeOrder
            }
            val scopes: List<Bindings<TypeAndDepth>> = when(overridenScopeSearchOrder) {
                ScopeSearchOrder.GLOBALS_THEN_LEXICAL -> listOf(globalEnv, currentEnv)
                ScopeSearchOrder.LEXICAL -> listOf(currentEnv, globalEnv)
            }

            return scopes
                .asSequence()
                .mapNotNull { it.lookupBinding(bindingName) }
                .firstOrNull()
        }

        /**
         * The actual variable resolution occurs in this method--all other parts of the
         * [StaticTypeVisitorTransform] support what's happening here.
         */
        override fun transformExprId(node: PartiqlAst.Expr.Id): PartiqlAst.Expr {
            val bindingName = BindingName(node.name.text, node.case.toBindingCase())

            val found = findBind(bindingName, node.qualifier)

            val singleBinding = singleFromSourceName // Copy to immutable local variable to enable smart-casting

            // If we didn't find a variable with that name...
            if (found == null) {
                // ...and there is a single from-source in the current query, then transform it into
                // a path expression, i.e. `SELECT foo FROM bar AS b` becomes `SELECT b.foo FROM bar AS b`
                return when {
                    // If there's a single from source...
                    singleBinding != null -> {
                        makePathIntoFromSource(singleBinding, node)
                    }
                    else -> {
                        // otherwise there is more than one from source so an undefined variable was referenced.
                        errUnboundName(node.name.text, node.metas)
                    }
                }
            }

            if (found.scope == BindingScope.GLOBAL) {
                when {
                    // If we found a variable in the global scope but a there is a single
                    // from source, we should transform to this to path expression anyway and pretend
                    // we didn't match the global variable.
                    singleBinding != null                                  -> {
                        return makePathIntoFromSource(singleBinding, node)
                    }
                    preventGlobalsExceptInFrom && fromVisited       -> {
                        errIllegalGlobalVariableAccess(bindingName.name, node.metas)
                    }
                    preventGlobalsInNestedQueries && currentScopeDepth > 1 -> {
                        errIllegalGlobalVariableAccess(bindingName.name, node.metas)
                    }
                }
            }

            val newScopeQualifier = when(found.scope) {
                BindingScope.LOCAL, BindingScope.LEXICAL -> PartiqlAst.build { localsFirst() }
                BindingScope.GLOBAL -> PartiqlAst.build { unqualified() }
            }

            return PartiqlAst.build {
                id_(node.name, node.case, newScopeQualifier,
                    node.metas + metaContainerOf(StaticTypeMeta(found.type)).toIonElementMetaContainer())
            }
        }

        /**
         * Changes the specified variable reference to a path expression with the name of the variable as
         * its first and only element.
         */
        private fun makePathIntoFromSource(fromSourceAlias: String, node: PartiqlAst.Expr.Id): PartiqlAst.Expr.Path {
            return PartiqlAst.build {
                path(
                    singleFromSourceRef(fromSourceAlias, node.extractSourceLocation()),
                    listOf(node.toPathExpr()),
                    node.extractSourceLocation())
            }
        }

        override fun transformExprPath(node: PartiqlAst.Expr.Path): PartiqlAst.Expr =
            when (node.root) {
                is PartiqlAst.Expr.Id -> super.transformExprPath(node).let {
                    it as PartiqlAst.Expr.Path
                    when (it.root) {
                        // we started with a variable, that got turned into a path, normalize it
                        // SELECT x.y FROM tbl AS t --> SELECT ("t".x).y FROM tbl AS t --> SELECT "t".x.y FROM tbl AS t
                        is PartiqlAst.Expr.Path -> {
                            val childPath = it.root
                            PartiqlAst.build {
                                path(childPath.root, childPath.steps + it.steps, it.metas)
                            }
                        }

                        // nothing to do--the transform didn't change anything
                        else -> it
                    }
                }
            else -> super.transformExprPath(node)
        }

        override fun transformFromSourceScan(node: PartiqlAst.FromSource.Scan): PartiqlAst.FromSource {
            // we need to transform the source expression before binding the names to our scope
            val from = super.transformFromSourceScan(node)

            node.atAlias?.let {
                addLocal(it.text, StaticType.ANY, it.metas)
            }

            node.byAlias?.let {
                addLocal(it.text, StaticType.ANY, it.metas)
            }

            val asSymbolicName = node.asAlias
                                 ?: error("fromSourceLet.variables.asName is null.  This wouldn't be the case if " +
                                     "FromSourceAliasVisitorTransform was executed first.")

            addLocal(asSymbolicName.text, StaticType.ANY, asSymbolicName.metas)

            if (!containsJoin) {
                fromVisited = true
                if (currentScopeDepth == 1) {
                    singleFromSourceName = asSymbolicName.text
                }
            }
            return from
        }

        override fun transformFromSourceUnpivot(node: PartiqlAst.FromSource.Unpivot): PartiqlAst.FromSource {
            // we need to transform the source expression before binding the names to our scope
            val from = super.transformFromSourceUnpivot(node)

            node.atAlias?.let {
                addLocal(it.text, StaticType.ANY, it.metas)
            }

            node.byAlias?.let {
                addLocal(it.text, StaticType.ANY, it.metas)
            }

            val asSymbolicName = node.asAlias
                                 ?: error("fromSourceLet.variables.asName is null.  This wouldn't be the case if " +
                                     "FromSourceAliasVisitorTransform was executed first.")

            addLocal(asSymbolicName.text, StaticType.ANY, asSymbolicName.metas)

            if (!containsJoin) {
                fromVisited = true
                if (currentScopeDepth == 1) {
                    singleFromSourceName = asSymbolicName.text
                }
            }
            return from
        }

        override fun transformFromSourceJoin(node: PartiqlAst.FromSource.Join): PartiqlAst.FromSource {
            // this happens before FromSourceScan or FromSourceUnpivot gets hit
            val outermostJoin = !containsJoin
            containsJoin = true

            return super.transformFromSourceJoin(node)
                .also {
                    if (outermostJoin) {
                        fromVisited = true
                        singleFromSourceName = null
                    }
                }
        }

        override fun transformFromSourceScan_expr(node: PartiqlAst.FromSource.Scan): PartiqlAst.Expr {
            this.scopeOrder = ScopeSearchOrder.GLOBALS_THEN_LEXICAL
            return transformExpr(node.expr).also {
                this.scopeOrder = ScopeSearchOrder.LEXICAL
            }
        }

        override fun transformFromSourceUnpivot_expr(node: PartiqlAst.FromSource.Unpivot): PartiqlAst.Expr {
            this.scopeOrder = ScopeSearchOrder.GLOBALS_THEN_LEXICAL
            return transformExpr(node.expr).also {
                this.scopeOrder = ScopeSearchOrder.LEXICAL
            }
        }

        override fun transformGroupBy(node: PartiqlAst.GroupBy): PartiqlAst.GroupBy {
            errUnimplementedFeature("GROUP BY")
        }

        private fun innerTransformExprSelect(node: PartiqlAst.Expr.Select): PartiqlAst.Expr {
            val new_from = transformExprSelect_from(node)
            val new_fromLet = transformExprSelect_fromLet(node)
            val new_where = transformExprSelect_where(node)
            val new_group = transformExprSelect_group(node)
            val new_having = transformExprSelect_having(node)
            val new_setq = transformExprSelect_setq(node)
            val new_project = transformExprSelect_project(node)
            val new_limit = transformExprSelect_limit(node)
            val new_metas = transformExprSelect_metas(node)
            return PartiqlAst.build {
                PartiqlAst.Expr.Select(
                    setq = new_setq,
                    project = new_project,
                    from = new_from,
                    fromLet = new_fromLet,
                    where = new_where,
                    group = new_group,
                    having = new_having,
                    limit = new_limit,
                    metas = new_metas
                )
            }
        }

        private fun innerTransformDataManipulation(node: PartiqlAst.Statement.Dml): PartiqlAst.Statement {
            val from = node.from?.let { transformFromSource(it) }
            val where = node.where?.let { transformStatementDml_where(node) }
            val dmlOperation = transformDmlOp(node.operation)
            val metas = transformMetas(node.metas)

            return PartiqlAst.build {
                dml(dmlOperation, from, where, metas)
            }
        }


        override fun transformExprSelect(node: PartiqlAst.Expr.Select): PartiqlAst.Expr {
            // a SELECT introduces a new scope, we evaluate the each from source
            // which is correlated (and thus has visibility from the previous bindings)
            return createTransformerForNestedScope().innerTransformExprSelect(node)
        }

        override fun transformStatementDml(node: PartiqlAst.Statement.Dml): PartiqlAst.Statement {
            return createTransformerForNestedScope().innerTransformDataManipulation(node)
        }

        private fun createTransformerForNestedScope(): VisitorTransform {
            return VisitorTransform(currentEnv, currentScopeDepth + 1)
        }

        /**
         * This function differs from the the overridden function only in that it does not attempt to resolve
         * [PartiqlAst.DdlOp.CreateIndex.fields], which would be a problem because they contain [PartiqlAst.Expr.Id]s
         * yet the fields/keys are scoped to the table and do not follow traditional lexical scoping rules.  This
         * indicates that [PartiqlAst.DdlOp.CreateIndex.fields] is incorrectly modeled as a [List<ExprNode>].
         */
        override fun transformDdlOpCreateIndex(node: PartiqlAst.DdlOp.CreateIndex): PartiqlAst.DdlOp =
            PartiqlAst.build {
                createIndex(
                    node.indexName,
                    node.fields,
                    transformMetas(node.metas)
                )
            }

        /**
         * This function differs from the the overridden function only in that it does not attempt to resolve
         * [PartiqlAst.DdlOp.DropIndex.table], which would be a problem because index names are scoped to the table
         * and do not follow traditional lexical scoping rules.  This is not something the [StaticTypeVisitorTransform]
         * is currently plumbed to deal with and also indicates that [PartiqlAst.DdlOp.DropIndex.table] is incorrectly
         * modeled as a [PartiqlAst.Expr.Id].
         */
        override fun transformDdlOpDropIndex(node: PartiqlAst.DdlOp.DropIndex): PartiqlAst.DdlOp =
            PartiqlAst.build {
                dropIndex(
                    node.table,
                    node.keys,
                    transformMetas(node.metas))
            }
    }

    // Use transformStatement since there's both SFW queries in addition to DDL and DML
    override fun transformStatement(node: PartiqlAst.Statement): PartiqlAst.Statement =
        VisitorTransform(wrapBindings(Bindings.empty(), 1), 0)
            .transformStatement(node)

    private fun wrapBindings(bindings: Bindings<StaticType>, depth: Int): Bindings<TypeAndDepth> {
        return Bindings.over { name ->
            bindings[name]?.let { bind ->
                TypeAndDepth(bind, depth)
            }
        }
    }
}
