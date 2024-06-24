package org.partiql.planner.catalog
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

/**
 * A reference to a named object in a catalog; case-preserved.
 */
public class Name(
    private val namespace: Namespace,
    private val name: String,
) {

    /**
     * Returns the unqualified name part.
     */
    public fun getName(): String = name

    /**
     * Returns the name's namespace.
     */
    public fun getNamespace(): Namespace = namespace

    /**
     * Returns true if the namespace is non-empty.
     */
    public fun hasNamespace(): Boolean = !namespace.isEmpty()

    /**
     * Compares two names including their namespaces and symbols.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other == null || javaClass != other.javaClass) {
            return false
        }
        other as Name
        return (this.name == other.name) && (this.namespace == other.namespace)
    }

    /**
     * The hashCode() is case-sensitive.
     */
    override fun hashCode(): Int {
        var result = 1
        result = 31 * result + namespace.hashCode()
        result = 31 * result + name.hashCode()
        return result
    }

    /**
     * Return the SQL name representation of this name — all parts delimited.
     */
    override fun toString(): String {
        val parts = mutableListOf<String>()
        parts.addAll(namespace.getLevels())
        parts.add(name)
        return Identifier.of(parts).toString()
    }

    public companion object {

        /**
         * Construct a name from a string.
         */
        @JvmStatic
        public fun of(vararg names: String): Name = of(names.toList())

        /**
         * Construct a name from a collection of strings.
         */
        @JvmStatic
        public fun of(names: Collection<String>): Name {
            assert(names.size > 1) { "Cannot create an empty name" }
            val namespace = Namespace.of(names.drop(1))
            val name = names.last()
            return Name(namespace, name)
        }
    }
}
