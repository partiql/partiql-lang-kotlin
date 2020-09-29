package org.partiql.lang.eval

import org.junit.Test
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.Property
import org.partiql.lang.util.to

class EvaluatingCompilerFromLetTests : EvaluatorTestBase() {

    private val session = mapOf("A" to "[ { id : 1 } ]",
        "B" to "[ { id : 100 }, { id : 200 } ]",
        "C" to """[ { name: 'foo', region: 'NA' },
                    { name: 'foobar', region: 'EU' },
                    { name: 'foobarbaz', region: 'NA' } ]""").toSession()

    // Valid test cases
    @Test
    fun `LET clause binding in WHERE` () {
        runTestCase(
            EvaluatorTestCase(
                query = "SELECT * FROM A LET 1 AS X WHERE X = 1",
                expectedSql = """<< {'id': 1} >>"""),
                session = session)
    }

    @Test
    fun `LET clause binding in SELECT` () {
        runTestCase(
            EvaluatorTestCase(
                query = "SELECT X FROM A LET 1 AS X",
                expectedSql = """<< {'X': 1} >>"""),
                session = session)
    }

    @Test
    fun `LET clause binding in GROUP BY` () {
        runTestCase(
            EvaluatorTestCase(
                query = "SELECT * FROM C LET region AS X GROUP BY X",
                expectedSql = """<< {'X': `EU`}, {'X': `NA`} >>"""),
                session = session)
    }

    @Test
    fun `LET clause binding in projection after GROUP BY` () {
        runTestCase(
            EvaluatorTestCase(
                query = "SELECT foo FROM B LET 100 AS foo GROUP BY B.id, foo",
                expectedSql = """<< {'foo': 100}, {'foo': 100} >>"""),
                session = session)
    }

    @Test
    fun `LET clause binding in HAVING after GROUP BY` () {
        runTestCase(
            EvaluatorTestCase(
                query = "SELECT B.id FROM B LET 100 AS foo GROUP BY B.id, foo HAVING B.id > foo",
                expectedSql = """<< {'id': 200} >>"""),
                session = session)
    }

    @Test
    fun `LET clause shadowed bindings` () {
        runTestCase(
            EvaluatorTestCase(
                query = "SELECT X FROM A LET 1 AS X, 2 AS X",
                expectedSql = """<< {'X': 2} >>"""),
                session = session)
    }

    @Test
    fun `LET clause binding shadowing FROM binding` () {
        runTestCase(
            EvaluatorTestCase(
                query = "SELECT * FROM A LET 100 AS A",
                expectedSql = """<< {'_1': 100} >>"""),
                session = session)
    }

    @Test
    fun `LET clause using other variables` () {
        runTestCase(
            EvaluatorTestCase(
                query = "SELECT X, Y FROM A LET 1 AS X, X + 1 AS Y",
                expectedSql = """<< {'X': 1, 'Y': 2} >>"""),
                session = session)
    }

    @Test
    fun `LET clause recursive binding` () {
        runTestCase(
            EvaluatorTestCase(
                query = "SELECT X FROM A LET 1 AS X, X AS X",
                expectedSql = """<< {'X': 1} >>"""),
                session = session)
    }

    @Test
    fun `LET clause calling function` () {
        runTestCase(
            EvaluatorTestCase(
                query = "SELECT X FROM A LET upper('foo') AS X",
                expectedSql = """<< {'X': 'FOO'} >>"""),
                session = session)
    }

    @Test
    fun `LET clause calling function on each row` () {
        runTestCase(
            EvaluatorTestCase(
                query = "SELECT nameLength FROM C LET char_length(C.name) AS nameLength",
                expectedSql = """<< {'nameLength': 3}, {'nameLength': 6}, {'nameLength': 9} >>"""),
                session = session)
    }

    @Test
    fun `LET clause calling function with GROUP BY and aggregation` () {
        runTestCase(
            EvaluatorTestCase(
                query = "SELECT C.region, MAX(nameLength) AS maxLen FROM C LET char_length(C.name) AS nameLength GROUP BY C.region",
                expectedSql = """<< {'region': `EU`, 'maxLen': 6}, {'region': `NA`, 'maxLen': 9} >>"""),
                session = session)
    }

    @Test
    fun `LET clause outer query has correct value` () {
        runTestCase(
            EvaluatorTestCase(
                query = "SELECT X FROM (SELECT VALUE X FROM A LET 1 AS X) LET 2 AS X",
                expectedSql = """<< {'X': 2} >>"""),
                session = session)
    }

    // Error test cases
    @Test
    fun `LET clause unbound variable` () {
        checkInputThrowingEvaluationException(
            "SELECT X FROM A LET Y AS X",
            session,
            ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
            mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 21L,
                    Property.BINDING_NAME to "Y"
            )
        )
    }

    @Test
    fun `LET clause binding definition dependent on later binding` () {
        checkInputThrowingEvaluationException(
            "SELECT X FROM A LET 1 AS X, Y AS Z, 3 AS Y",
            session,
            ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
            mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 29L,
                    Property.BINDING_NAME to "Y"
            )
        )
    }

    @Test
    fun `LET clause inner query binding not available in outer query` () {
        checkInputThrowingEvaluationException(
            "SELECT X FROM (SELECT VALUE X FROM A LET 1 AS X)",
            session,
            ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
            mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 8L,
                    Property.BINDING_NAME to "X"
            )
        )
    }

    @Test
    fun `LET clause binding in subquery not in outer LET query` () {
        checkInputThrowingEvaluationException(
            "SELECT Z FROM A LET (SELECT 1 FROM A LET 1 AS X) AS Y, X AS Z",
            session,
            ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
            mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 56L,
                    Property.BINDING_NAME to "X"
            )
        )
    }

    @Test
    fun `LET clause binding referenced in HAVING not in GROUP BY`() {
        checkInputThrowingEvaluationException(
            "SELECT B.id FROM B LET 100 AS foo GROUP BY B.id HAVING B.id > foo",
            session,
            ErrorCode.EVALUATOR_VARIABLE_NOT_INCLUDED_IN_GROUP_BY,
            mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 63L,
                    Property.BINDING_NAME to "foo"
            )
        )
    }

    @Test
    fun `LET clause binding referenced in projection not in GROUP BY`() {
        checkInputThrowingEvaluationException(
            "SELECT foo FROM B LET 100 AS foo GROUP BY B.id",
            session,
            ErrorCode.EVALUATOR_VARIABLE_NOT_INCLUDED_IN_GROUP_BY,
            mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 8L,
                    Property.BINDING_NAME to "foo"
            )
        )
    }
}
