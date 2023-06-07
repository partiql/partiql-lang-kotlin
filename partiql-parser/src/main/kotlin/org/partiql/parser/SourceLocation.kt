/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.parser

/**
 * SourceLocation represents the span of a given grammar rule; which corresponds to an AST subtree.
 *
 * TODO Fix Source Location Tests https://github.com/partiql/partiql-lang-kotlin/issues/1114
 * Unfortunately several mistakes were made that are hard to undo altogether. The legacy parser incorrectly
 * used the first token length rather than rule span for source location length. Then we have asserted on these
 * incorrect SourceLocations in many unit tests unrelated to SourceLocations.
 *
 * @property line
 * @property offset
 * @property length
 * @property lengthLegacy
 */
public data class SourceLocation(
    val line: Int,
    val offset: Int,
    val length: Int,
    val lengthLegacy: Int = 0,
) {

    companion object {

        /**
         * This is a flag for backwards compatibility when converting to the legacy AST.
         */
        val UNKNOWN = SourceLocation(-1, -1, -1)
    }
}
