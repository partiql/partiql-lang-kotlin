package org.partiql.test.coverage.utils

import org.partiql.coverage.api.PartiQLTestCase
import org.partiql.lang.eval.EvaluationSession

/**
 * [PartiQLTestCase] with nothing to assert. Uses the default [EvaluationSession].
 */
class PartiQLTestCaseDefault : PartiQLTestCase {
    override val session: EvaluationSession = EvaluationSession.standard()
}
