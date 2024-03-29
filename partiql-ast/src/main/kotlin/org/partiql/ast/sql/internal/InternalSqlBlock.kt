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

package org.partiql.ast.sql.internal

/**
 * Representation of some textual elements as a token (singly-linked) list.
 */
internal sealed class InternalSqlBlock {

    /**
     * Next token (if any) in the list.
     */
    internal var next: InternalSqlBlock? = null

    /**
     * A newline / link break token.
     */
    internal class NL : InternalSqlBlock()

    /**
     * A raw text token. Cannot be broken.
     */
    internal class Text(val text: String) : InternalSqlBlock()

    /**
     * A nest token representing a (possible indented) token sublist.
     *
     * @property prefix     A prefix character such as '{', '(', or '['.
     * @property postfix    A postfix character such as  '}', ')', or ']].
     * @property child
     */
    internal class Nest(
        val prefix: String?,
        val postfix: String?,
        val child: InternalSqlBlock,
    ) : InternalSqlBlock()

    companion object {

        /**
         * Helper function to create root node (empty).
         */
        @JvmStatic
        internal fun root(): InternalSqlBlock = Text("")
    }
}
