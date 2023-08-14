/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.coverage.api

import org.partiql.lang.eval.EvaluationSession

/**
 * Each [PartiQLTestProvider] shall hold one or more [PartiQLTestCase]'s. Each [PartiQLTestCase]'s [session] will be
 * used to create PartiQL's evaluator. This may hold other variables for the purpose of testing assertions (among other
 * things).
 *
 * See the below Kotlin example:
 * ```
 * public class SimpleTestCase(val x: Int, val expected: Boolean) : PartiQLTestCase {
 *     override val session: EvaluationSession = EvaluationSession.build {
 *         globals(
 *             Bindings.ofMap(
 *                 mapOf("x" to this.x)
 *             )
 *         )
 *     }
 * }
 * ```
 */
public interface PartiQLTestCase {
    /**
     * Holds information such as globals and parameters to provide inputs to a [PartiQLTestCase].
     */
    public val session: EvaluationSession
}
