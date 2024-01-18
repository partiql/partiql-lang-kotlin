/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.spi

/**
 * A [BindingPath] represents an SQL-qualified identifier which is composed of case-sensitive and case-insensitive steps.
 *
 * @property steps
 */
public class BindingPath(public val steps: List<BindingName>) {

    /**
     * SQL-99 CNF — Case Normal Form.
     */
    public val normalized: List<String> = steps.map {it.normalized }

    /**
     * A case-sensitive key.
     */
    public val key: String = steps.joinToString(".") { it.name }

    /**
     * Memoized hashCode for hashing data structures.
     */
    private val hashCode = key.hashCode()

    override fun equals(other: Any?): Boolean = (other is BindingPath && other.key == key)

    override fun hashCode(): Int = hashCode

    override fun toString(): String = key

    public fun startsWith(vararg symbols: String): Boolean {
        if (symbols.size > steps.size) {
            return false
        }
        for (i in symbols.indices) {
            val t = symbols[i]
            if (!steps[i].matches(t)) {
                return false
            }
        }
        return true
    }

    public fun matches(path: List<String>): Boolean {
        if (steps.size != path.size) {
            return false
        }
        for (i in path.indices) {
            val t = path[i]
            val s = steps[i]
            if (!s.matches(t)) {
                return false
            }
        }
        return true
    }
}
