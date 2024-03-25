/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
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

package org.partiql.ast.sql

import org.partiql.ast.AstNode

/**
 * Pretty-print this [AstNode] as SQL text with the given [SqlLayout]
 */
@JvmOverloads
public fun AstNode.sql(
    layout: SqlLayout = SqlLayout.DEFAULT,
    dialect: SqlDialect = SqlDialect.PARTIQL,
): String = dialect.apply(this).sql(layout)

/**
 * Write this [SqlBlock] tree as SQL text with the given [SqlLayout].
 *
 * @param layout    SQL formatting ruleset
 * @return SQL text
 */
public fun SqlBlock.sql(layout: SqlLayout = SqlLayout.DEFAULT): String = layout.format(this)
