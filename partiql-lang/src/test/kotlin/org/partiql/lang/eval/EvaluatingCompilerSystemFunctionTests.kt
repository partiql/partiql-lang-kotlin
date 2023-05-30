package org.partiql.lang.eval

import org.junit.Test

class EvaluatingCompilerSystemFunctionTests : EvaluatorTestBase() {

    private val user = "partiql"
    private val session = EvaluationSession.build {
        user(user)
    }

    @Test
    fun simple() =
        runEvaluatorTestCase(
            "CURRENT_USER",
            session,
            "\"$user\""
        )

    @Test
    fun simpleConcat() =
        runEvaluatorTestCase(
            "CURRENT_USER || ' rocks!'",
            session,
            "\"$user rocks!\""
        )

    @Test
    fun simpleFilter() =
        runEvaluatorTestCase(
            "SELECT id FROM <<{'id': 0, 'name': 'partiql'}, {'id': 1, 'name': 'ion'}>> WHERE name = CURRENT_USER",
            session,
            "$BAG_ANNOTATION::[{'id': 0}]"
        )
}
