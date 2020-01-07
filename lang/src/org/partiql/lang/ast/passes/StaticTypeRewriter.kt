/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package org.partiql.lang.ast.passes

import com.amazon.ion.IonSystem
import org.partiql.lang.ast.*
import org.partiql.lang.errors.*
import org.partiql.lang.eval.*
import org.partiql.lang.types.*
import org.partiql.lang.util.*

/**
 * Extra constraints which may be imposed on the type checking.
 */
enum class StaticTypeRewriterConstraints {

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
 * An [AstRewriter] that annotates nodes with their static types and resolves implicit variables
 * explicitly based on the static types.
 *
 * The validations performed may be enhanced by the passing of additional [StaticTypeRewriterConstraints].
 *
 * @param globalEnv The global bindings to the static environment.  This is data catalog purely from a lookup
 *                  perspective.
 * @param constraints Additional constraints on what variable scoping, or other rules should be followed.
 */
class StaticTypeRewriter(private val ion: IonSystem,
                         globalBindings: Bindings<StaticType>,
                         constraints: Set<StaticTypeRewriterConstraints> = setOf()) : AstRewriter {

    /** Used to allow certain binding lookups to occur directly in the global scope. */
    private val globalEnv = wrapBindings(globalBindings, 0)

    private val preventGlobalsExceptInFrom =
        StaticTypeRewriterConstraints.PREVENT_GLOBALS_EXCEPT_IN_FROM in constraints

    private val preventGlobalsInNestedQueries =
        StaticTypeRewriterConstraints.PREVENT_GLOBALS_IN_NESTED_QUERIES in constraints

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
    private inner class Rewriter(private val parentEnv: Bindings<TypeAndDepth>,
                                 private val currentScopeDepth: Int) : AstRewriterBase() {

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

        private fun singleFromSourceRef(sourceName: String, metas: MetaContainer): VariableReference {
            val sourceType = currentEnv[BindingName(sourceName, BindingCase.SENSITIVE)] ?:
                throw IllegalArgumentException("Could not find type for single FROM source variable")

            return VariableReference(
                sourceName,
                CaseSensitivity.SENSITIVE,
                ScopeQualifier.LEXICAL,
                metas + metaContainerOf(StaticTypeMeta(sourceType.type))
            )
        }

        private fun VariableReference.toPathComponent(): PathComponentExpr =
            PathComponentExpr(Literal(ion.newString(id), metas.sourceLocationContainer), case)

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

        private fun addLocal(name: String, type: StaticType, metas: MetaContainer) {
            val existing = localsOnlyEnv[BindingName(name, BindingCase.INSENSITIVE)]
            if (existing != null) {
                errAmbiguousName(name, metas)
            }
            localsMap[name] = type
            // this requires a new instance because of how [Bindings.ofMap] works
            localsOnlyEnv = wrapBindings(Bindings.ofMap(localsMap), currentScopeDepth)
        }

        override fun rewriteCallAgg(node: CallAgg): ExprNode {
            return CallAgg(
                // do not rewrite the funcExpr--as this is a symbolic name in another namespace (AST is over generalized here)
                node.funcExpr,
                node.setQuantifier,
                rewriteExprNode(node.arg),
                rewriteMetas(node))
        }

        override fun rewriteNAry(node: NAry): ExprNode = when (node.op) {
            // do not write the name of the call--this should be a symbolic name in another namespace (AST is over generalized here)
            NAryOp.CALL -> NAry(
                node.op,
                listOf(node.args[0]) + node.args.drop(1).map { rewriteExprNode(it) },
                rewriteMetas(node))
            else -> super.rewriteNAry(node)
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
        private fun findBind(bindingName: BindingName, scopeQualifier: ScopeQualifier): TypeAndScope? {
            // Override the current scope search order if the var is lexically qualified.
            val overridenScopeSearchOrder = when(scopeQualifier) {
                ScopeQualifier.LEXICAL     -> ScopeSearchOrder.LEXICAL
                ScopeQualifier.UNQUALIFIED -> this.scopeOrder
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
         * [StaticTypeRewriter] support what's happening here.
         */
        override fun rewriteVariableReference(node: VariableReference): ExprNode {
            val bindingName = BindingName(node.id, node.case.toBindingCase())

            val found = findBind(bindingName, node.scopeQualifier)

            val singleBinding = singleFromSourceName //Copy to immutable local variable to enable smart-casting

            // If we didn't find a variable with that name...
            if(found == null) {
                // ...and there is a single from-source in the current query, then rewrite it into
                // a path expression, i.e. `SELECT foo FROM bar AS b` becomes `SELECT b.foo FROM bar AS b`
                return when {
                    // If there's a single from source...
                    singleBinding != null -> {
                        makePathIntoFromSource(singleBinding, node)
                    }
                    else -> {
                        // otherwise there is more than one from source so an undefined variable was referenced.
                        errUnboundName(node.id, node.metas)
                    }
                }
            }

            if(found.scope == BindingScope.GLOBAL) {
                when {
                    // If we found a variable in the global scope but a there is a single
                    // from source, we should rewrite to this to path expression anyway and pretend
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
                BindingScope.LOCAL, BindingScope.LEXICAL -> ScopeQualifier.LEXICAL
                BindingScope.GLOBAL -> ScopeQualifier.UNQUALIFIED
            }

            return node.copy(
                scopeQualifier = newScopeQualifier,
                metas = node.metas.add(StaticTypeMeta(found.type)))
        }

        /**
         * Changes the specified variable reference to a path expression with the name of the variable as
         * its first and only element.
         */
        private fun makePathIntoFromSource(fromSourceAlias: String, node: VariableReference): Path {
            return Path(
                singleFromSourceRef(fromSourceAlias, node.metas.sourceLocationContainer),
                listOf(node.toPathComponent()),
                node.metas.sourceLocationContainer)
        }

        override fun rewritePath(node: Path): ExprNode = when (node.root) {
            is VariableReference -> super.rewritePath(node).let {
                it as Path
                when (it.root) {
                    // we started with a variable, that got turned into a path, normalize it
                    // SELECT x.y FROM tbl AS t --> SELECT ("t".x).y FROM tbl AS t --> SELECT "t".x.y FROM tbl AS t
                    is Path -> {
                        val childPath = it.root
                        Path(childPath.root, childPath.components + it.components, it.metas)
                    }

                    // nothing to do--the rewrite didn't change anything
                    else -> it
                }
            }
            else -> super.rewritePath(node)
        }

        // TODO support analyzing up the call chain (n-ary, etc.)

        override fun rewriteFromSourceLet(fromSourceLet: FromSourceLet): FromSourceLet {
            // we need to rewrite the source expression before binding the names to our scope
            val from = super.rewriteFromSourceLet(fromSourceLet)

            fromSourceLet.variables.atName?.let {
                addLocal(it.name, StaticType.ANY, it.metas)
            }

            fromSourceLet.variables.byName?.let {
                addLocal(it.name, StaticType.ANY, it.metas)
            }

            val asSymbolicName = fromSourceLet.variables.asName
                                 ?: error("fromSourceLet.variables.asName is null.  This wouldn't be the case if FromSourceAliasRewriter was executed first.")

            addLocal(asSymbolicName.name, StaticType.ANY, asSymbolicName.metas)


            if (!containsJoin) {
                fromVisited = true
                if (currentScopeDepth == 1) {
                    singleFromSourceName = asSymbolicName.name
                }
            }

            return from
        }

        override fun rewriteFromSourceJoin(fromSource: FromSourceJoin): FromSource {
            // this happens before FromSourceExpr or FromSourceUnpivot gets hit
            val outermostJoin = !containsJoin
            containsJoin = true

            return super.rewriteFromSourceJoin(fromSource)
                .also {
                    if (outermostJoin) {
                        fromVisited = true
                        singleFromSourceName = null
                    }
                }
        }

        override fun rewriteFromSourceValueExpr(expr: ExprNode): ExprNode {
            this.scopeOrder = ScopeSearchOrder.GLOBALS_THEN_LEXICAL
            return rewriteExprNode(expr).also {
                this.scopeOrder = ScopeSearchOrder.LEXICAL
            }
        }

        // TODO support PIVOT
        // TODO support GROUP BY

        override fun rewriteSelect(selectExpr: Select): ExprNode {
            // a SELECT introduces a new scope, we evaluate the each from source
            // which is correlated (and thus has visibility from the previous bindings)
            return createRewriterForNestedScope().innerRewriteSelect(selectExpr)
        }

        override fun rewriteDataManipulation(node: DataManipulation): DataManipulation {
            return createRewriterForNestedScope().innerRewriteDataManipulation(node)
        }

        private fun createRewriterForNestedScope(): Rewriter {
            return Rewriter(currentEnv, currentScopeDepth + 1)
        }

    }

    override fun rewriteExprNode(node: ExprNode): ExprNode =
        Rewriter(wrapBindings(Bindings.empty(), 1), 0)
            .rewriteExprNode(node)

    private fun wrapBindings(bindings: Bindings<StaticType>, depth: Int): Bindings<TypeAndDepth> {
        return Bindings.over { name ->
            bindings[name]?.let { bind ->
                TypeAndDepth(bind, depth)
            }
        }
    }
}