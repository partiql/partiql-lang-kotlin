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

package org.partiql.lang.ast

enum class AstVersion(val number: Int) {
    /**
     * The "legacy" AST in the form of `(meta <exp> { line: <line>, column: <column> })`.
     */
    V0(0),

    /**
     * The first iteration of the "new" AST in the form of:
     * ```
     * (term
     *   (exp <exp>
     *   (meta ($source_location { line_num: <line_num>, char_offset: })))
     * ```
     */
    V1(1);

    companion object {
        /** The range of currently supported AST versions in human readable format. */
        val versionsAsString = AstVersion.values().map { it.number }.joinToString(", ")
    }
}