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

package org.partiql.lang.syntax

import com.amazon.ion.*
import org.partiql.lang.ast.*
import org.partiql.lang.domains.PartiqlAst

/**
 * Parses a list of [Token] into an [IonSexp] based AST.
 *
 * Implementations must be thread-safe.
 */
interface Parser {
    fun parseExprNode(source: String): ExprNode
    fun parseAstStatement(source: String): PartiqlAst.Statement

    @Deprecated("Please use parseExprNode() instead--the return value can be deserialized to backward-compatible IonSexp.")
    fun parse(source: String): IonSexp
}
