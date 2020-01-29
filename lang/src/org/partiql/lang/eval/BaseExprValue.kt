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

import org.partiql.lang.util.downcast

/**
 * Base implementation of [ExprValue] that provides a bare minimum implementation of
 * a value.
 */
abstract class BaseExprValue : ExprValue {

    override val scalar: Scalar
        get() = Scalar.EMPTY
    override val bindings: Bindings<ExprValue>
        get() = Bindings.empty()
    override val ordinalBindings: OrdinalBindings
        get() = OrdinalBindings.EMPTY

    override fun iterator(): Iterator<ExprValue> = emptyList<ExprValue>().iterator()

    final override fun <T : Any?> asFacet(type: Class<T>?): T? =
        downcast(type) ?: provideFacet(type)

    /**
     * Provides a fall-back for providing facets if a sub-class doesn't inherit the facet interface
     * or class.
     *
     * This implementation returns `null`.
     */
    open fun <T> provideFacet(type: Class<T>?): T? = null

    override fun toString(): String = stringify()
}

