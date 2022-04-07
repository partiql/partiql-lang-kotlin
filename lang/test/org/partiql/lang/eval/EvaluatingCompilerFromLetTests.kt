package org.partiql.lang.eval

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.propertyValueMapOf
import org.partiql.lang.util.to

class EvaluatingCompilerFromLetTests : EvaluatorTestBase() {

    private val session = mapOf(
        "A" to "[ { id : 1 } ]",
        "B" to "[ { id : 100 }, { id : 200 } ]",
        "C" to """[ { name: 'foo', region: 'NA' },
                    { name: 'foobar', region: 'EU' },
                    { name: 'foobarbaz', region: 'NA' } ]"""
    ).toSession()

    class ArgsProviderValid : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // LET used in WHERE
            EvaluatorTestCase(
                "SELECT * FROM A LET 1 AS X WHERE X = 1",
                """<< {'id': 1} >>"""
            ),
            // LET used in SELECT
            EvaluatorTestCase(
                "SELECT X FROM A LET 1 AS X",
                """<< {'X': 1} >>"""
            ),
            // LET used in GROUP BY
            EvaluatorTestCase(
                "SELECT * FROM C LET region AS X GROUP BY X",
                """<< {'X': `EU`}, {'X': `NA`} >>"""
            ),
            // LET used in projection after GROUP BY
            EvaluatorTestCase(
                "SELECT foo FROM B LET 100 AS foo GROUP BY B.id, foo",
                """<< {'foo': 100}, {'foo': 100} >>"""
            ),
            // LET used in HAVING after GROUP BY
            EvaluatorTestCase(
                "SELECT B.id FROM B LET 100 AS foo GROUP BY B.id, foo HAVING B.id > foo",
                """<< {'id': 200} >>"""
            ),
            // LET shadowed binding
            EvaluatorTestCase(
                "SELECT X FROM A LET 1 AS X, 2 AS X",
                """<< {'X': 2} >>"""
            ),
            // LET shadowing FROM binding
            EvaluatorTestCase(
                "SELECT * FROM A LET 100 AS A",
                """<< {'_1': 100} >>"""
            ),
            // LET using other variables
            EvaluatorTestCase(
                "SELECT X, Y FROM A LET 1 AS X, X + 1 AS Y",
                """<< {'X': 1, 'Y': 2} >>"""
            ),
            // LET recursive binding
            EvaluatorTestCase(
                "SELECT X FROM A LET 1 AS X, X AS X",
                """<< {'X': 1} >>"""
            ),
            // LET calling function
            EvaluatorTestCase(
                "SELECT X FROM A LET upper('foo') AS X",
                """<< {'X': 'FOO'} >>"""
            ),
            // LET calling function on each row
            EvaluatorTestCase(
                "SELECT nameLength FROM C LET char_length(C.name) AS nameLength",
                """<< {'nameLength': 3}, {'nameLength': 6}, {'nameLength': 9} >>"""
            ),
            // LET calling function with GROUP BY and aggregation
            EvaluatorTestCase(
                "SELECT C.region, MAX(nameLength) AS maxLen FROM C LET char_length(C.name) AS nameLength GROUP BY C.region",
                """<< {'region': `EU`, 'maxLen': 6}, {'region': `NA`, 'maxLen': 9} >>"""
            ),
            // LET outer query has correct value
            EvaluatorTestCase(
                "SELECT X FROM (SELECT VALUE X FROM A LET 1 AS X) LET 2 AS X",
                """<< {'X': 2} >>"""
            )
        )
    }

    @ParameterizedTest
    @ArgumentsSource(ArgsProviderValid::class)
    fun validTests(tc: EvaluatorTestCase) = runEvaluatorTestCase(
        tc.copy(excludeLegacySerializerAssertions = true),
        session
    )

    class ArgsProviderError : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // LET unbound variable
            EvaluatorErrorTestCase(
                query = "SELECT X FROM A LET Y AS X",
                expectedErrorCode = ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
                expectedErrorContext = propertyValueMapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 21L,
                    Property.BINDING_NAME to "Y"
                ),
                expectedPermissiveModeResult = "<<{}>>"
            ),
            // LET binding definition dependent on later binding
            EvaluatorErrorTestCase(
                query = "SELECT X FROM A LET 1 AS X, Y AS Z, 3 AS Y",
                expectedErrorCode = ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
                expectedErrorContext = propertyValueMapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 29L,
                    Property.BINDING_NAME to "Y"
                ),
                expectedPermissiveModeResult = "<<{'X': 1}>>"
            ),
            // LET inner query binding not available in outer query
            EvaluatorErrorTestCase(
                groupName = "SELECT X FROM A LET Y AS X",
                query = "SELECT X FROM (SELECT VALUE X FROM A LET 1 AS X)",
                expectedErrorCode = ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
                expectedErrorContext = propertyValueMapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 8L,
                    Property.BINDING_NAME to "X"
                ),
                expectedPermissiveModeResult = "<<{}>>"
            ),
            // LET binding in subquery not in outer LET query
            EvaluatorErrorTestCase(
                query = "SELECT Z FROM A LET (SELECT 1 FROM A LET 1 AS X) AS Y, X AS Z",
                expectedErrorCode = ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
                expectedErrorContext = propertyValueMapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 56L,
                    Property.BINDING_NAME to "X"
                ),
                expectedPermissiveModeResult = "<<{}>>"
            ),
            // LET binding referenced in HAVING not in GROUP BY
            EvaluatorErrorTestCase(
                query = "SELECT B.id FROM B LET 100 AS foo GROUP BY B.id HAVING B.id > foo",
                expectedErrorCode = ErrorCode.EVALUATOR_VARIABLE_NOT_INCLUDED_IN_GROUP_BY,
                expectedErrorContext = propertyValueMapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 63L,
                    Property.BINDING_NAME to "foo"
                )
            ),
            // LET binding referenced in projection not in GROUP BY
            EvaluatorErrorTestCase(
                query = "SELECT foo FROM B LET 100 AS foo GROUP BY B.id",
                expectedErrorCode = ErrorCode.EVALUATOR_VARIABLE_NOT_INCLUDED_IN_GROUP_BY,
                expectedErrorContext = propertyValueMapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 8L,
                    Property.BINDING_NAME to "foo"
                )
            )
        )
    }

    @ParameterizedTest
    @ArgumentsSource(ArgsProviderError::class)
    fun errorTests(tc: EvaluatorErrorTestCase) = runEvaluatorErrorTestCase(
        tc.copy(excludeLegacySerializerAssertions = true),
        session
    )
}
