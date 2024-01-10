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
 * Encapsulates the data necessary to perform a binding lookup.
 */
public data class BindingName(
    public val name: String,
    public val case: BindingCase,
) {

    /**
     * Compares [name] to [otherName] using the rules specified by [case].
     */
    public fun isEquivalentTo(otherName: String?): Boolean =
        otherName != null && name.isBindingNameEquivalent(otherName, case)

    /**
     * Compares [name] to [target] using the rules specified by [case].
     */
    public fun matches(target: String): Boolean = when (case) {
        BindingCase.SENSITIVE -> target == name
        BindingCase.INSENSITIVE -> target.equals(name, ignoreCase = true)
    }

    /**
     * Compares this string to [other] using the rules specified by [case].
     */
    private fun String.isBindingNameEquivalent(other: String, case: BindingCase): Boolean =
        when (case) {
            BindingCase.SENSITIVE -> this.equals(other)
            BindingCase.INSENSITIVE -> this.equals(other, ignoreCase = true)
        }
}
