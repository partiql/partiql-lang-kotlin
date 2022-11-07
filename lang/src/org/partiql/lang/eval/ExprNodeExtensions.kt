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

@file:Suppress("DEPRECATION") // We don't need warnings about ExprNode deprecation.

package org.partiql.lang.eval

import com.amazon.ion.IonString
import com.amazon.ionelement.api.MetaContainer
import org.partiql.lang.ast.ExprNode
import org.partiql.lang.ast.Literal
import org.partiql.lang.ast.Path
import org.partiql.lang.ast.PathComponentExpr
import org.partiql.lang.ast.Typed
import org.partiql.lang.ast.TypedOp
import org.partiql.lang.ast.VariableReference

/**
 * Determines an appropriate column name for the given [ExprNode].
 *
 * - If [this] is a [VariableReference], returns the name of the variable.
 * - If [this] is a [Path], invokes [Path.extractColumnAlias] to determine the alias.
 * - If [this] is a cast expression, invokes [Typed.extractColumnAlias] to determine the alias.
 *
 * Otherwise, returns the column index prefixed with `_`.
 */
internal fun ExprNode.extractColumnAlias(idx: Int): String =
    when (this) {
        is VariableReference -> this.id
        is Path -> this.extractColumnAlias(idx)
        is Typed -> this.extractColumnAlias(idx)
        else -> syntheticColumnName(idx)
    }

/**
 * Extracts a name for [Typed] CAST expressions and generates a synthetic column name for CAN_CAST and IS
 * expressions.
 */
private fun Typed.extractColumnAlias(idx: Int): String {
    return when (this.op) {
        TypedOp.CAST -> this.expr.extractColumnAlias(idx)
        TypedOp.CAN_CAST, TypedOp.CAN_LOSSLESS_CAST, TypedOp.IS -> syntheticColumnName(idx)
    }
}

/**
 * Returns the name of the last component if it is a string literal, otherwise returns the
 * column index prefixed with `_`.
 */
private fun Path.extractColumnAlias(idx: Int): String {
    val (_, components, _: MetaContainer) = this
    return when (val nameOrigin = components.last()) {
        is PathComponentExpr -> {
            val maybeLiteral = nameOrigin.expr
            when {
                maybeLiteral is Literal && maybeLiteral.ionValue is IonString -> maybeLiteral.ionValue.stringValue()
                else -> syntheticColumnName(idx)
            }
        }
        else -> syntheticColumnName(idx)
    }
}
