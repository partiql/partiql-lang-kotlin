package org.partiql.coverage.api

import org.partiql.lang.eval.EvaluationSession

interface PartiQLTestCase {
    val session: EvaluationSession
}
