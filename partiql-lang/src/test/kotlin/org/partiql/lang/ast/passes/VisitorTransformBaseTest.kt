package org.partiql.lang.ast.passes

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.EvaluatorTestCasesAsStatementTestCases
import org.partiql.lang.eval.visitors.VisitorTransformBase
import org.partiql.lang.util.testdsl.StatementTestCase

/**
 * This makes sure that the VisitorTransformBase returns an exact copy as the original Statement
 */
class VisitorTransformBaseTest {
    private val defaultTransformer = object : VisitorTransformBase() {}
    @ParameterizedTest
    @ArgumentsSource(EvaluatorTestCasesAsStatementTestCases::class)
    fun identityTransformTest(tc: StatementTestCase) {
        val transformed = defaultTransformer.transformStatement(tc.expr)
        tc.assertEquals(transformed)
    }
}
