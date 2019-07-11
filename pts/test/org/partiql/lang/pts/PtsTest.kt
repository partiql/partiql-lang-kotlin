package org.partiql.lang.pts

import org.partiql.testscript.evaluator.Evaluator
import org.partiql.testscript.evaluator.PtsEquality
import org.partiql.testscript.junitRunner.Junit4PtsTest

// TODO move this back into lang when testscript is its own pkg

/**
 * Runs all tests from PTS as Junit tests
 */
class PtsTest : Junit4PtsTest() {

    override fun getEvaluator(): Evaluator = PartiQlPtsEvaluator(PtsEquality.getDefault())

    override fun getPtsFilePaths() = listOf("../testscript/pts/test-scripts")

}