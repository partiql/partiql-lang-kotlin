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

package org.partiql.lang.util;

/**
 * In Kotlin, the cases of a `when <is <type>>...` must be exhaustive only when `when` is used as an expression.
 * Wrapping each of the cases in this is an attempt to allow `when` to be easily used as an expression even
 * when an expression isn't actually needed.  Example:
 *
 * ```
 * when(instance) {
 *   is FooNode -> case { ... }
 *   is BarNode -> case { ... }
 * }.toUnit()
 * ```
 *
 * [case] returns the sentinel instance of [WhenAsExpressionHelper].  When used in all cases
 * of a `when` *and* the value is consumed (using the `.toUnit()` function, the compiler will enforce that the
 * cases of the `when` are exhaustive.
 */
inline fun case(block: () -> Unit): WhenAsExpressionHelper {
    block()
    return WhenAsExpressionHelper.Instance
}


/**
 * See [case] for a description of how to use this[]
 */
class WhenAsExpressionHelper private constructor() {
    fun toUnit() {}
    companion object {
        val Instance = WhenAsExpressionHelper()
    }
}