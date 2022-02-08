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

package org.partiql.lang.eval

import com.amazon.ion.IonSequence

/** A simple mapping of ordinal index to [ExprValue]. */
interface OrdinalBindings {
    companion object {
        // @JvmField
        // Note: for some reason, @JvmField being present causes IntelliJ's test runner to throw
        // an exception and fail to run any tests when you tell it to run tests in a package.  It still works
        // when you tell it to run all tests in a single class or individual tests, though.
        val EMPTY = object : OrdinalBindings {
            override fun get(index: Int): ExprValue? = null
        }

        @JvmStatic
        fun ofList(list: List<ExprValue>) = object : OrdinalBindings {
            override fun get(index: Int): ExprValue? = list.getOrNull(index)
        }

        @JvmStatic
        fun ofIonSequence(seq: IonSequence, valueFactory: ExprValueFactory) =
            object : OrdinalBindings {
                override fun get(index: Int): ExprValue? =
                    when {
                        index < 0 || index >= seq.size -> null
                        else -> {
                            val ordinalValue = seq[index]
                            valueFactory.newFromIonValue(ordinalValue)
                        }
                    }
            }

    }

    /**
     * Looks up an index within this binding.
     *
     * @param index The binding to look up.  The index is zero-based.
     *
     * @return The value mapped to the binding, or `null` if no such binding exists.
     */
    operator fun get(index: Int): ExprValue?
}
