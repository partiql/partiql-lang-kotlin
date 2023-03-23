package org.partiql.lang.eval.visitors

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.util.ArgumentsProviderBase

class SubqueryCoercionVisitorTransformTests : VisitorTransformTestBase() {

    @ParameterizedTest
    @ArgumentsSource(ArgsProvider::class)
    fun test(tc: TransformTestCase) = runTest(tc, listOf(SubqueryCoercionVisitorTransform()))

    class ArgsProvider : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            TransformTestCase(
                name = "Bare SELECT is not coerced",
                original = """ SELECT max(n) FROM [1,2,3] AS n """,
                expected = """ SELECT max(n) FROM [1,2,3] AS n """
            ),
            TransformTestCase(
                name = "Coerce subquery in an arithmetic rhs",
                original = """ 5 + (SELECT max(n) FROM <<1,2,3>> AS n) """,
                expected = """ 5 + coll_to_scalar(SELECT max(n) FROM <<1,2,3>> AS n) """
            ),
            TransformTestCase(
                name = "Coerce subquery in an arithmetic lhs",
                original = """ (SELECT n FROM <<1, 2, 3>> AS n) * 4 """,
                expected = """ coll_to_scalar(SELECT n FROM <<1, 2, 3>> AS n) * 4 """
            ),
            TransformTestCase(
                name = "Coerce in scalar comparison lhs",
                original = """ x < (SELECT avg(n) FROM <<1,2,3>> AS n) """,
                expected = """ x < coll_to_scalar(SELECT avg(n) FROM <<1,2,3>> AS n)"""
            ),
            TransformTestCase(
                name = "Coerce in scalar comparison rhs",
                original = """ (SELECT n FROM [1,2,3] AS n) = 2 """,
                expected = """ coll_to_scalar(SELECT n FROM [1,2,3] AS n) = 2 """
            ),
            TransformTestCase(
                name = "Coerce on both sides of a scalar comparison",
                original = """ (SELECT n FROM [1,2,3] AS n) <> (SELECT avg(n) FROM <<1,2,3>> AS n) """,
                expected = """ coll_to_scalar(SELECT n FROM [1,2,3] AS n) <> coll_to_scalar(SELECT avg(n) FROM <<1,2,3>> AS n) """
            ),
            TransformTestCase(
                name = "Do not coerce in lhs of array comparison",
                original = """ (SELECT n FROM [1,2,3] AS n) = [3, 2, 1] """,
                expected = """ (SELECT n FROM [1,2,3] AS n) = [3, 2, 1] """
            ),
            TransformTestCase(
                name = "Do not coerce in rhs of array comparison",
                original = """ (1, 2, 3) < (SELECT n FROM [1,2,3] AS n) """,
                expected = """ (1, 2, 3) < (SELECT n FROM [1,2,3] AS n)"""
            ),
            TransformTestCase(
                name = "Coerce lhs of IN, but not rhs",
                original = """ (SELECT max(n) FROM <<1,2,3>> AS n) IN (SELECT n FROM [1,2,3] AS n) """,
                expected = """ coll_to_scalar(SELECT max(n) FROM <<1,2,3>> AS n) IN (SELECT n FROM [1,2,3] AS n) """
            ),
            TransformTestCase(
                name = "Coerce subquery that is the WHERE expression",
                original = """
                    SELECT x.b
                    FROM << {'a': 1, 'b': 10}, {'a': 3, 'b': 30} >>  AS x
                    WHERE (SELECT max(n) = x.a FROM [1,2,3] AS n) 
                """.trimIndent(),
                expected = """
                    SELECT x.b
                    FROM << {'a': 1, 'b': 10}, {'a': 3, 'b': 30} >>  AS x
                    WHERE coll_to_scalar(SELECT max(n) = x.a FROM [1,2,3] AS n) 
                """.trimIndent()
            ),
            TransformTestCase(
                name = "Coerce SELECT when the only item in another SELECT",
                original = """ SELECT (SELECT max(n) FROM [1,2,3] AS n) AS m FROM 0 """,
                expected = """ SELECT coll_to_scalar(SELECT max(n) FROM [1,2,3] AS n) AS m FROM 0 """
            ),
            TransformTestCase(
                name = "Coerce SELECT when among items in another SELECT",
                original = """
                     SELECT x.b, 
                            (SELECT y.c
                             FROM << {'a': 1, 'c': 100}, {'a': 3, 'c': 300} >> AS y
                             WHERE y.a = x.a ) AS c2
                     FROM << {'a': 1, 'b': 10}, {'a': 3, 'b': 30} >>  AS x
                """.trimIndent(),
                expected = """
                    SELECT x.b, 
                           coll_to_scalar(
                            SELECT y.c
                            FROM << {'a': 1, 'c': 100}, {'a': 3, 'c': 300} >> AS y
                            WHERE y.a = x.a ) AS c2
                    FROM << {'a': 1, 'b': 10}, {'a': 3, 'b': 30} >>  AS x
                """.trimIndent()
            ),
            TransformTestCase(
                name = "Do not coerce data sources in FROM",
                original = """ 
                    SELECT n2.n1, m2.m
                    FROM (SELECT n1 FROM <<1,2,3>> AS n1) AS n2,
                         (SELECT min(m1) AS m FROM [4,5] AS m1) AS m2
                    WHERE 2 * n2.n1 = m2.m
                """.trimIndent(),
                expected = """
                     SELECT n2.n1, m2.m
                     FROM (SELECT n1 FROM <<1,2,3>> AS n1) AS n2,
                          (SELECT min(m1) AS m FROM [4,5] AS m1) AS m2
                     WHERE 2 * n2.n1 = m2.m
                """.trimIndent()
            ),
        )
    }
}
