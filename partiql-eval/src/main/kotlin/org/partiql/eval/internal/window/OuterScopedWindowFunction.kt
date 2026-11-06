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

package org.partiql.eval.internal.window

import org.partiql.eval.Environment
import org.partiql.eval.WindowFunction

/**
 * A [WindowFunction] that evaluates expressions against rows of its partition other than the current row (e.g. LAG, or
 * an aggregate over the window frame).
 *
 * The environment passed to [WindowFunction.eval] already has the current row pushed onto it, so pushing another row of
 * the partition onto it would shift every outer variable reference by one scope. Such functions must instead push the
 * other row onto the window operator's outer environment, which is provided via [setOuterEnvironment].
 *
 * Outer references in a window function, e.g. `SUM(t.x + o.k) OVER ()` in a subquery correlated on `o`, are a PartiQL
 * extension: SQL:2023 Subclause 9.23, Syntax Rule 3 does not allow a window function to contain an outer reference.
 */
internal interface OuterScopedWindowFunction : WindowFunction {

    /**
     * Sets the environment of the window operator, i.e. the environment without any row of the partition pushed. This is
     * called each time the window operator is opened (e.g. once per outer row of a correlated subquery).
     */
    fun setOuterEnvironment(env: Environment)
}
