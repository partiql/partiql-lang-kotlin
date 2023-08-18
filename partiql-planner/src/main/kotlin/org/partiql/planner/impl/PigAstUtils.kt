/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.planner.impl

import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.TextElement
import com.amazon.ionelement.api.emptyMetaContainer
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlLogical
import org.partiql.spi.BindingCase

/**
 * Determines an appropriate column name for the given [PartiqlAst.Expr].
 *
 * If [this] is a [PartiqlAst.Expr.Id], returns the name of the variable.
 *
 * If [this] is a [PartiqlAst.Expr.Path], invokes [PartiqlAst.Expr.Path.extractColumnAlias] to determine the alias.
 *
 * If [this] is a [PartiqlAst.Expr.Cast], the column alias is the same as would be given to the [PartiqlAst.Expr.Cast.value] to be `CAST`.
 *
 * Otherwise, returns the column index prefixed with `_`.
 */
internal fun PartiqlAst.Expr.extractColumnAlias(idx: Int): String =
    when (this) {
        is PartiqlAst.Expr.Id -> this.name.text
        is PartiqlAst.Expr.Path -> {
            this.extractColumnAlias(idx)
        }
        is PartiqlAst.Expr.Cast -> {
            this.value.extractColumnAlias(idx)
        }
        else -> syntheticColumnName(idx)
    }

/**
 * Returns the name of the last component if it is a string literal, otherwise returns the
 * column index prefixed with `_`.
 */
internal fun PartiqlAst.Expr.Path.extractColumnAlias(idx: Int): String {
    return when (val nameOrigin = this.steps.last()) {
        is PartiqlAst.PathStep.PathExpr -> {
            val maybeLiteral = nameOrigin.index
            when {
                maybeLiteral is PartiqlAst.Expr.Lit && maybeLiteral.value is TextElement -> maybeLiteral.value.textValue
                else -> syntheticColumnName(idx)
            }
        }
        else -> syntheticColumnName(idx)
    }
}

/**
 * Converts a [CaseSensitivity] to a [BindingCase].
 */
internal fun PartiqlAst.CaseSensitivity.toBindingCase(): BindingCase = when (this) {
    is PartiqlAst.CaseSensitivity.CaseInsensitive -> BindingCase.INSENSITIVE
    is PartiqlAst.CaseSensitivity.CaseSensitive -> BindingCase.SENSITIVE
}

/**
 * Returns a [MetaContainer] with *only* the source location of the receiver [MetaContainer], if present.
 *
 * Avoids creating a new [MetaContainer] if its not needed.
 */
internal fun PartiqlAst.PartiqlAstNode.extractSourceLocation(): MetaContainer {
    return when (this.metas.size) {
        0 -> emptyMetaContainer()
        1 -> when {
            this.metas.containsKey(SourceLocationMeta.TAG) -> this.metas
            else -> emptyMetaContainer()
        }
        else -> {
            this.metas[SourceLocationMeta.TAG]?.let { com.amazon.ionelement.api.metaContainerOf(SourceLocationMeta.TAG to it) }
                ?: emptyMetaContainer()
        }
    }
}
/**
 * Converts a [PartiqlLogical.CaseSensitivity] to a [BindingCase].
 */
internal fun PartiqlLogical.CaseSensitivity.toBindingCase(): BindingCase = when (this) {
    is PartiqlLogical.CaseSensitivity.CaseInsensitive -> BindingCase.INSENSITIVE
    is PartiqlLogical.CaseSensitivity.CaseSensitive -> BindingCase.SENSITIVE
}
