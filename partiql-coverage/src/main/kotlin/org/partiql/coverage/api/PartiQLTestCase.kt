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
    public val session: EvaluationSession
}
