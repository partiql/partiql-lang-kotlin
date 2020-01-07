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

package org.partiql.lang.util

import com.amazon.ion.*
import org.partiql.lang.errors.*
import org.partiql.lang.eval.*

internal fun errAmbiguousBinding(bindingName: String, matchingNames: List<String>): Nothing {
    err("Multiple matches were found for the specified identifier",
        ErrorCode.EVALUATOR_AMBIGUOUS_BINDING,
        propertyValueMapOf(Property.BINDING_NAME to bindingName,
                           Property.BINDING_NAME_MATCHES to matchingNames.joinToString(", ")),
        internal = false)
}

/**
 * Compares this string to [other] using the rules specified by [case].
 */
fun String.isBindingNameEquivalent(other: String, case: BindingCase): Boolean =
    when(case) {
        BindingCase.SENSITIVE   -> this.equals(other)
        BindingCase.INSENSITIVE -> this.caseInsensitiveEquivalent(other)
    }

/**
 * One case insensitive equality check to rule them all.
 */
fun String.caseInsensitiveEquivalent(name: String) =
    equals(name, ignoreCase = true)

/**
 * Containing static methods intended to be invoked by Java clients, this class provides a standard way to compare
 * identifiers and includes some helper methods for looking up values from [IonStruct] and [Map] instances according to
 * the specified [BindingCase].
 */
abstract class BindingHelper private constructor() {
    companion object {

        /**
         * Use this method to determine if the given identifiers match according to PartiQL rules
         * for identifier equality.
         */
        @JvmStatic
        fun bindingNameEquals(id1: String, id2: String, case: BindingCase): Boolean =
            when(case) {
                BindingCase.SENSITIVE   -> id1.equals(id2)
                BindingCase.INSENSITIVE -> id1.caseInsensitiveEquivalent(id2)
            }

        /**
         * Should be used by clients when a case insensitive lookup results in matching more than one identifier.
         */
        @JvmStatic
        fun throwAmbiguousBindingEvaluationException(bindingName: String, matchingNames: List<String>): Nothing = errAmbiguousBinding(bindingName, matchingNames)
    }
}