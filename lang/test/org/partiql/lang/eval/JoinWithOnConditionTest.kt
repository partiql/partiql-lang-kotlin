package org.partiql.lang.eval

import junitparams.Parameters
import org.junit.Test


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
    @Test
    @Parameters
    fun joinWithOnConditionTest(pair: Pair<EvaluatorTestCase, EvaluationSession>): Unit =
            runTestCase(pair.first, pair.second)

    fun parametersForJoinWithOnConditionTest(): List<Pair<EvaluatorTestCase, EvaluationSession>> {

        return listOf(
                Pair(EvaluatorTestCase(
                        "JOIN ON with no nulls",
                        sqlUnderTest,
                        """
                            << 
                            {'id':1, 'val1':'a', 'val2':10},
                            {'id':2, 'val1':'b', 'val2':20},
                            {'id':3, 'val1':'c', 'val2':30}
                            >>
                        """), sessionNoNulls),
                Pair(EvaluatorTestCase(
                        "JOIN ON with no nulls",
                        sqlUnderTest,
                        """
                            << 
                            {'id':1, 'val1':'a', 'val2':10},
                            {'id':3, 'val1':'c', 'val2':30}
                            >>
                        """), sessionNullIdRow),
                Pair(EvaluatorTestCase(
                        "JOIN ON with no nulls",
                        sqlUnderTest,
                        """
                            <<>>
                        """), sessionNullTable),
                Pair(EvaluatorTestCase(
                        "JOIN ON with no nulls",
                        sqlUnderTest,
                        """
                            <<>>
                        """), sessionNullTableRow)
        )
    }
}