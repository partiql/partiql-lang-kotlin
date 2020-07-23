/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.eval

import com.amazon.ion.IonString
import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.StringElement
import com.amazon.ionelement.api.emptyMetaContainer
import org.partiql.lang.ast.ExprNode
import org.partiql.lang.ast.Literal
import org.partiql.lang.ast.Path
import org.partiql.lang.ast.PathComponentExpr
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.ast.VariableReference
import org.partiql.lang.domains.PartiqlAst
import org.partiql.pig.runtime.SymbolPrimitive
import org.partiql.lang.ast.MetaContainer as LegacyMetaContainer


/**
 * Determines an appropriate column name for the given [ExprNode].
 * If [this] is a [VariableReference], returns the name of the variable.
 * If [this] is a [Path], invokes [Path.extractColumnAlias] to determine the alias.
 * Otherwise, returns the column index prefixed with `_`.
 */
fun ExprNode.extractColumnAlias(idx: Int): String =
    when (this) {
    is VariableReference -> {
        val (name, _, _, _: LegacyMetaContainer) = this
        name
    }
    is Path              -> {
        this.extractColumnAlias(idx)
    }
    else                 -> syntheticColumnName(idx)
}

/**
 * Returns the name of the last component if it is a string literal, otherwise returns the
 * column index prefixed with `_`.
 */
fun Path.extractColumnAlias(idx: Int): String {
    val (_, components, _: LegacyMetaContainer) = this
    val nameOrigin = components.last()
    return when (nameOrigin) {
        is PathComponentExpr -> {
            val maybeLiteral = nameOrigin.expr
            when {
                maybeLiteral is Literal && maybeLiteral.ionValue is IonString -> maybeLiteral.ionValue.stringValue()
                else                                                          -> syntheticColumnName(idx)
            }
        }
        else                 -> syntheticColumnName(idx)
    }
}

// DL TODO:  move the below to a separate file perhaps in a better location(s)?
// Make these internal or private?
fun MetaContainer.sourceLocationOnly(): MetaContainer {
    return when(val srcLoc = this[SourceLocationMeta.TAG] as? SourceLocationMeta) {
        null -> emptyMetaContainer()
        else -> com.amazon.ionelement.api.metaContainerOf(SourceLocationMeta.TAG to srcLoc)
    }
}

/**
 * Determines an appropriate column name for the given [ExprNode].
 * If [this] is a [VariableReference], returns the name of the variable.
 * If [this] is a [Path], invokes [Path.extractColumnAlias] to determine the alias.
 * Otherwise, returns the column index prefixed with `_`.
 */
fun PartiqlAst.Expr.extractColumnAlias(idx: Int): SymbolPrimitive =
    when (this) {
        is PartiqlAst.Expr.Id -> SymbolPrimitive(name.text, name.metas)
        is PartiqlAst.Expr.Path -> this.extractColumnAlias(idx)
        else -> SymbolPrimitive(syntheticColumnName(idx), this.metas.sourceLocationOnly())
    }

/**
 * Returns the name of the last component if it is a string literal, otherwise returns the
 * column index prefixed with `_`.
 */
fun PartiqlAst.Expr.Path.extractColumnAlias(idx: Int): SymbolPrimitive =
    SymbolPrimitive(
        when (val nameOrigin = this.steps.last()) {
            is PartiqlAst.PathStep.PathExpr -> {
                val maybeLiteral = nameOrigin.index
                when {
                    maybeLiteral is PartiqlAst.Expr.Lit && maybeLiteral.value is StringElement -> {
                        maybeLiteral.value.textValue
                    }
                    else -> syntheticColumnName(idx)
                }
            }
            else -> syntheticColumnName(idx)
        },
        this.metas.sourceLocationOnly())


