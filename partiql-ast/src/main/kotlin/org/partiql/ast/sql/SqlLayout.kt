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

/**
 * [SqlLayout] determines how an [SqlBlock] tree is transformed in SQL text.
 */
public interface SqlLayout {

    public fun format(head: SqlBlock): String

    public companion object {

        /**
         * Default SQL format.
         */
        @JvmStatic
        public val DEFAULT = object : SqlLayout {

            private val indent = Indent(2, Indent.Type.SPACE)

            override fun format(head: SqlBlock): String {
                val ctx = Ctx.empty()
                format(head, ctx)
                return ctx.toString()
            }

            private tailrec fun format(curr: SqlBlock, ctx: Ctx) {
                when (curr) {
                    is SqlBlock.NL -> ctx.out.appendLine()
                    is SqlBlock.Text -> ctx.out.append(curr.text)
                    is SqlBlock.Nest -> {
                        if (curr.prefix != null) ctx.out.append(curr.prefix)
                        @Suppress("NON_TAIL_RECURSIVE_CALL")
                        format(curr.child, ctx.nest())
                        if (curr.postfix != null) ctx.out.append(curr.postfix)
                    }
                }
                return if (curr.next != null) format(curr.next!!, ctx) else Unit
            }
        }

        /**
         * Write SQL statement on one line.
         */
        @JvmStatic
        public val ONELINE = object : SqlLayout {

            override fun format(head: SqlBlock): String {
                val ctx = Ctx.empty()
                format(head, ctx)
                return ctx.toString()
            }

            private tailrec fun format(curr: SqlBlock, ctx: Ctx) {
                when (curr) {
                    is SqlBlock.NL -> {} // skip newlines
                    is SqlBlock.Text -> ctx.out.append(curr.text)
                    is SqlBlock.Nest -> {
                        if (curr.prefix != null) ctx.out.append(curr.prefix)
                        @Suppress("NON_TAIL_RECURSIVE_CALL")
                        format(curr.child, ctx.nest())
                        if (curr.postfix != null) ctx.out.append(curr.postfix)
                    }
                }
                return if (curr.next != null) format(curr.next!!, ctx) else Unit
            }
        }
    }

    private class Ctx private constructor(val out: StringBuilder, val level: Int) {
        fun nest() = Ctx(out, level + 1)

        override fun toString() = out.toString()

        companion object {
            fun empty() = Ctx(StringBuilder(), 0)
        }
    }

    /**
     * [SqlLayout] indent configuration.
     *
     * @property count
     * @property type
     */
    public data class Indent(
        @JvmField public val count: Int,
        @JvmField public val type: Type,
    ) {

        enum class Type(val char: Char) {
            TAB(Char(9)),
            SPACE(Char(32)),
            ;
        }

        override fun toString() = type.char.toString().repeat(count)
    }
}
