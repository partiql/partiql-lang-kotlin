package org.partiql.lang.eval

import junitparams.Parameters
import org.junit.Ignore
import org.junit.Test
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestCase

class JoinWithOnConditionTest : EvaluatorTestBase() {

    val sessionNoNulls = mapOf(
        "t1" to """
               [ 
               {id: 1, val:"a"},
               {id: 2, val:"b"},
               {id: 3, val:"c"},
               ]
            """,
        "t2" to """
               [ 
               {id: 1, val: 10},
               {id: 2, val: 20},
               {id: 3, val:30},
               ]
            """
    ).toSession()

    val sessionNullIdRow = mapOf(
        "t1" to """
               [ 
               {id: 1, val:"a"},
               {id: 2, val:"b"},
               {id: 3, val:"c"},
               ]
            """,
        "t2" to """
               [ 
               {id: 1,    val: 10},
               {id: null, val: 20},
               {id: 3,    val:30},
               ]
            """
    ).toSession()

    val sessionNullTable: EvaluationSession = mapOf(
        "t1" to """
               [ 
               {id: 1, val:"a"},
               {id: 2, val:"b"},
               {id: 3, val:"c"},
               ]
            """,
        "t2" to """ null """
    ).toSession()

    val sessionNullTableRow: EvaluationSession = mapOf(
        "t1" to """
               [ 
               {id: 1, val:"a"},
               {id: 2, val:"b"},
               {id: 3, val:"c"},
               ]
            """,
        "t2" to """[ null ]"""
    ).toSession()

    val sqlUnderTest = """ 
                            SELECT t1.id  AS id, 
                                   t1.val AS val1,
                                   t2.val AS val2
                            FROM t1 AS t1 JOIN t2 as t2 ON t1.id = t2.id
                        """

    private val session = mapOf(
        "A" to "[ { 'n': 1 }, { 'n': 3 } ]",
        "B" to "[ { 'n': 1 }, { 'n': 2 }, { 'n': 3 } ]",
        "C" to "[ { 'n': 2 }, { 'n': 3 } ]"
    ).toSession()

    @Test
    @Parameters
    fun joinWithOnConditionTest(pair: Pair<EvaluatorTestCase, EvaluationSession>): Unit =
        runEvaluatorTestCase(pair.first, pair.second)

    fun parametersForJoinWithOnConditionTest(): List<Pair<EvaluatorTestCase, EvaluationSession>> {

        return listOf(
            Pair(
                EvaluatorTestCase(
                    groupName = "JOIN ON with no nulls",
                    query = sqlUnderTest,
                    expectedResult = """ 
                        << 
                            {'id':1, 'val1':'a', 'val2':10},
                            {'id':2, 'val1':'b', 'val2':20},
                            {'id':3, 'val1':'c', 'val2':30}
                        >>
                    """
                ),
                sessionNoNulls
            ),
            Pair(
                EvaluatorTestCase(
                    groupName = "JOIN ON with no nulls",
                    query = sqlUnderTest,
                    expectedResult = """
                        << 
                            {'id':1, 'val1':'a', 'val2':10},
                            {'id':3, 'val1':'c', 'val2':30}
                        >>
                    """
                ),
                sessionNullIdRow
            ),
            Pair(
                EvaluatorTestCase(
                    groupName = "JOIN ON with no nulls",
                    query = sqlUnderTest,
                    expectedResult = " <<>> "
                ),
                sessionNullTable
            ),
            Pair(
                EvaluatorTestCase(
                    groupName = "JOIN ON with no nulls",
                    query = sqlUnderTest,
                    expectedResult = " <<>> "
                ),
                sessionNullTableRow
            )
        )
    }

    @Test
    fun naturalOrderJoinNonAssociativeTest() {
        val testCase =
            EvaluatorTestCase(
                query = "SELECT * FROM A LEFT JOIN B ON A.n=B.n INNER JOIN C ON B.n=C.n",
                expectedResult = """<< { 'n': 3, 'n': 3, 'n': 3 } >>"""
            )
        runEvaluatorTestCase(testCase, session)
    }

    // TODO: This test is not passing. Needs to be fixed. See https://github.com/partiql/partiql-lang-kotlin/issues/766
    @Ignore
    @Test
    fun specifiedOrderJoinNonAssociativeTest() {
        val testCase =
            EvaluatorTestCase(
                query = "SELECT * FROM A LEFT JOIN (B INNER JOIN C ON B.n=C.n) ON A.n=B.n",
                expectedResult = """<< { 'n': 1, '_2': NULL }, { 'n': 3, 'n': 3, 'n': 3 } >>""".trimMargin()
            )
        runEvaluatorTestCase(testCase, session)
    }
}
