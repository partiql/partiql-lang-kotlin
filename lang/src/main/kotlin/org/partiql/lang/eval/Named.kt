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

/**
 * Facet for a value to indicate that it either has a name within some context
 * or an ordinal position.
 *
 * An implementation should not provide this facet if it does not provide a meaningful name.
 */
interface Named {
    /**
     * The name of this value, generally a `string` for values that have a field name in
     * a `struct` or an `int` for values that have some ordinal in a collection.
     */
    val name: ExprValue
}
