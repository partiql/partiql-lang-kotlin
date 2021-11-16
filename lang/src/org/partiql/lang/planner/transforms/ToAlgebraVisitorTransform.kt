
package org.partiql.lang.planner.transforms

import com.amazon.ionelement.api.MetaContainer
import org.partiql.lang.ast.passes.SemanticException
import org.partiql.lang.domains.PartiqlAlgebraUnindexed
import org.partiql.lang.domains.PartiqlAlgebra
import org.partiql.lang.domains.PartiqlAlgebraUnindexedToPartiqlAlgebraVisitorTransform
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.addSourceLocation
import org.partiql.lang.util.propertyValueMapOf
import org.partiql.pig.runtime.LongPrimitive

fun PartiqlAlgebraUnindexed.Statement.toAlgebra(tableExists: (BindingName) -> Boolean) =
    // For now, we don't support resolution of global variables here.
    ToAlgebraVisitorTransform(tableExists, Scope(emptyList(), parent = null)).transformStatement(this)

private data class Scope(val varDecls: List<PartiqlAlgebraUnindexed.VarDecl>, val parent: Scope?)

/** Converts a [PartiqlAlgebraUnindexed.CaseSensitivity] to a [BindingCase]. */
private fun PartiqlAlgebraUnindexed.CaseSensitivity.toBindingCase(): BindingCase = when(this) {
    is PartiqlAlgebraUnindexed.CaseSensitivity.CaseInsensitive -> BindingCase.INSENSITIVE
    is PartiqlAlgebraUnindexed.CaseSensitivity.CaseSensitive -> BindingCase.SENSITIVE
}


private class ToAlgebraVisitorTransform(
    private val tableExists: (BindingName) -> Boolean,
    private val currentScope: Scope = Scope(emptyList(), parent = null)
) : PartiqlAlgebraUnindexedToPartiqlAlgebraVisitorTransform() {
    var lookupUnqualifiedInGlobalsFirst = false

    private fun nest(nextScope: Scope) = ToAlgebraVisitorTransform(tableExists, nextScope)

    private fun PartiqlAlgebraUnindexed.Expr.Id.asGlobal(): PartiqlAlgebra.Expr.Global =
        PartiqlAlgebra.build {
            global_(name, transformCaseSensitivity(case), metas)
        }

    private fun PartiqlAlgebraUnindexed.Expr.Id.asIndexedId(index: LongPrimitive): PartiqlAlgebra.Expr.Id =
        PartiqlAlgebra.build {
            id_(name, index)
        }

    override fun transformBindingsExprScan_expr(node: PartiqlAlgebraUnindexed.BindingsExpr.Scan): PartiqlAlgebra.Expr {
        val oldLookupUnqualifiedInGlobalsFirst = this.lookupUnqualifiedInGlobalsFirst
        this.lookupUnqualifiedInGlobalsFirst = true
        return super.transformBindingsExprScan_expr(node).also {
            this.lookupUnqualifiedInGlobalsFirst = oldLookupUnqualifiedInGlobalsFirst
        }
    }

    override fun transformExprMapValues(node: PartiqlAlgebraUnindexed.Expr.MapValues): PartiqlAlgebra.Expr {
        val oldLookupUnqualifiedInGlobalsFirst = this.lookupUnqualifiedInGlobalsFirst
        this.lookupUnqualifiedInGlobalsFirst = false
        return super.transformExprMapValues(node).also {
            this.lookupUnqualifiedInGlobalsFirst = oldLookupUnqualifiedInGlobalsFirst
        }
    }

    override fun transformExprId(node: PartiqlAlgebraUnindexed.Expr.Id): PartiqlAlgebra.Expr {
        val bindingName = BindingName(node.name.text, node.case.toBindingCase())

        if(lookupUnqualifiedInGlobalsFirst) {
            when (node.qualifier) {
                is PartiqlAlgebraUnindexed.ScopeQualifier.LocalsFirst -> { /* do nothing */ }
                is PartiqlAlgebraUnindexed.ScopeQualifier.Unqualified -> {
                    // check for existence of table first, return to indicate global variable lookup if the table exists.
                    if (tableExists(bindingName)) {
                        return node.asGlobal()
                    }
                }
            }
        }

        tailrec fun findBindings(scope: Scope): PartiqlAlgebra.Expr {
            val found = scope.varDecls.filter {
                bindingName.isEquivalentTo(it.name.text)
            }
            return when (found.size) {
                // If we didn't find the binding...
                0 -> when (scope.parent) {
                    // when there are no more parent scopes to search
                    null -> {
                        // check if the table exists
                        if (!tableExists(bindingName)) {
                            errUnboundName(bindingName.name, node.name.metas)
                        } else {
                            node.asGlobal()
                        }
                    }
                    else -> findBindings(scope.parent)
                }
                1 -> node.asIndexedId(found.first().index)
                else -> errAmbiguousName(node.name.text, node.name.metas)
            }
        }

        return findBindings(currentScope)
    }

    override fun transformExprMapValues_exp(node: PartiqlAlgebraUnindexed.Expr.MapValues): PartiqlAlgebra.Expr {
        val bindings = getNestedScope(node.query, currentScope)
        return nest(bindings).transformExpr(node.exp)
    }

    // TODO: this is a duplicate of the one in StaticTypeVisitorTransform
    private fun errAmbiguousName(name: String, metas: MetaContainer): Nothing = throw SemanticException(
        "A variable named '$name' was already defined in this scope",
        ErrorCode.SEMANTIC_AMBIGUOUS_BINDING,
        propertyValueMapOf(
            Property.BINDING_NAME to name
        ).addSourceLocation(metas)
    )

    private fun errUnboundName(name: String, metas: MetaContainer): Nothing = throw SemanticException(
        "No such variable named '$name'",
        ErrorCode.SEMANTIC_UNBOUND_BINDING,
        propertyValueMapOf(
            Property.BINDING_NAME to name
        ).addSourceLocation(metas)
    )

    override fun transformBindingsExprFilter_predicate(node: PartiqlAlgebraUnindexed.BindingsExpr.Filter): PartiqlAlgebra.Expr {
        val bindings = getNestedScope(node.term, currentScope)
        val nested = ToAlgebraVisitorTransform(tableExists, bindings)
        return nested.transformExpr(node.predicate)
    }

    override fun transformBindingsExprCrossJoin_right(node: PartiqlAlgebraUnindexed.BindingsExpr.CrossJoin): PartiqlAlgebra.BindingsTerm {
        val leftBindings = getNestedScope(node.left, currentScope)
        val nested = ToAlgebraVisitorTransform(tableExists, leftBindings)
        return nested.transformBindingsTerm(node.right)
    }

    private fun getNestedScope(bindingsTerm: PartiqlAlgebraUnindexed.BindingsTerm, parent: Scope?): Scope =
        when(bindingsTerm.exp) {
            is PartiqlAlgebraUnindexed.BindingsExpr.Scan ->
                Scope(
                    listOfNotNull(bindingsTerm.exp.asDecl, bindingsTerm.exp.atDecl, bindingsTerm.exp.byDecl),
                    parent
                )
            is PartiqlAlgebraUnindexed.BindingsExpr.CrossJoin -> {
                val left = getNestedScope(bindingsTerm.exp.left, parent)
                getNestedScope(bindingsTerm.exp.right, left)
            }
            is PartiqlAlgebraUnindexed.BindingsExpr.Filter -> {
                getNestedScope(bindingsTerm.exp.term, parent)
            }
        }
}
