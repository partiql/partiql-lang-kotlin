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

    public fun print(block: SqlBlock): String

    public companion object {

        /**
         * Default SQL layout.
         */
        @JvmStatic
        public val STANDARD: SqlLayout = object : SqlLayout {

            override fun print(block: SqlBlock): String {
                val sb = StringBuilder()
                var curr: SqlBlock? = block
                while (curr != null) {
                    when (curr) {
                        is SqlBlock.None -> {}
                        is SqlBlock.Line -> sb.appendLine()
                        is SqlBlock.Text -> sb.append(curr.text)
                        is SqlBlock.Nest -> {
                            if (curr.prefix != null) sb.append(curr.prefix)
                            sb.append(print(curr.child))
                            if (curr.postfix != null) sb.append(curr.postfix)
                        }
                    }
                    curr = curr.next
                }
                return sb.toString()
            }
        }

        /**
         * Write SQL statement on one line.
         */
        @JvmStatic
        public val ONELINE: SqlLayout = object : SqlLayout {

            override fun print(block: SqlBlock): String {
                val sb = StringBuilder()
                var curr: SqlBlock? = block
                while (curr != null) {
                    when (curr) {
                        is SqlBlock.None -> {}
                        is SqlBlock.Line -> {} // ignore
                        is SqlBlock.Text -> sb.append(curr.text)
                        is SqlBlock.Nest -> {
                            if (curr.prefix != null) sb.append(curr.prefix)
                            sb.append(print(curr.child))
                            if (curr.postfix != null) sb.append(curr.postfix)
                        }
                    }
                    curr = curr.next
                }
                return sb.toString()
            }
        }
    }
}
