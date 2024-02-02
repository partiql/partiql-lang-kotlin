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

import org.partiql.spi.connector.ConnectorPath

/**
 * A [BindingPath] represents an SQL-qualified identifier which is composed of case-sensitive and case-insensitive steps.
 *
 * @property steps
 */
public class BindingPath(public val steps: List<BindingName>) {

    /**
     * SQL-99 CNF — Case Normal Form.
     */
    public val normalized: List<String> = steps.map {
        when (it.case) {
            BindingCase.SENSITIVE -> it.name
            BindingCase.INSENSITIVE -> it.name.uppercase()
        }
    }

    /**
     * SQL-99 CNF as string.
     */
    public val key: String = normalized.joinToString(".")

    /**
     * Memoized hashCode for hashing data structures.
     */
    private val hashCode = key.hashCode()

    override fun equals(other: Any?): Boolean = (other is BindingPath && other.key == key)

    override fun hashCode(): Int = hashCode

    override fun toString(): String = key

    public fun matches(path: ConnectorPath): Boolean {
        if (path.steps.size != steps.size) {
            return false
        }
        for (i in path.steps.indices) {
            val t = path.steps[i]
            val s = steps[i]
            if (!s.matches(t)) {
                return false
            }
        }
        return true
    }
}
