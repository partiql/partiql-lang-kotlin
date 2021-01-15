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

import com.amazon.ion.*
import org.partiql.lang.ast.*


/**
 * Determines an appropriate column name for the given [ExprNode].
 * If [this] is a [VariableReference], returns the name of the variable.
 * If [this] is a [Path], invokes [Path.extractColumnAlias] to determine the alias.
 * Otherwise, returns the column index prefixed with `_`.
 */
fun ExprNode.extractColumnAlias(idx: Int): String =
    when (this) {
    is VariableReference -> {
        val (name, _, _, _: MetaContainer) = this
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
    val (_, components, _: MetaContainer) = this
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

