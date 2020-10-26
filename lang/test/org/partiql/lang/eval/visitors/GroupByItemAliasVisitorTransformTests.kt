package org.partiql.lang.eval.visitors

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.util.ArgumentsProviderBase

class GroupByItemAliasVisitorTransformTests : VisitorTransformTestBase() {

    class ArgsProvider : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            TransformTestCase(
                """
                SELECT *
                FROM a 
                GROUP BY a
                """,
                """
                SELECT * 
                FROM a 
                GROUP BY a AS a
                """
            ),
            TransformTestCase(
                """
                SELECT *
                FROM a, B 
                GROUP BY a, B
                """,
                """
                SELECT * 
                FROM a, B 
                GROUP BY a AS a, B AS B
                """
            ),
            TransformTestCase(
                """
                SELECT *
                FROM a, B, c
                GROUP BY a, B, c
                """,
                """
                SELECT * 
                FROM a, B, c
                GROUP BY a AS a, B AS B, c AS c
                """
            ),
            TransformTestCase(
                """
                SELECT *
                FROM a
                GROUP BY a.x
                """,
                """
                SELECT * 
                FROM a
                GROUP BY a.x AS x
                """
            ),
            TransformTestCase(
                """
                SELECT *
                FROM a, B
                GROUP BY a.x, B.Y
                """,
                """
                SELECT * 
                FROM a, B
                GROUP BY a.x AS x, B.Y AS Y
                """
            ),
            TransformTestCase(
                """
                SELECT *
                FROM a, B, c
                GROUP BY a.x, B.Y, c.z
                """,
                """
                SELECT * 
                FROM a, B, c
                GROUP BY a.x AS x, B.Y AS Y, c.z AS z
                """
            ),
            TransformTestCase(
                """
                SELECT *
                FROM a, B, c
                GROUP BY a.i.x, B.j.Y, c.k.z
                """,
                """
                SELECT * 
                FROM a, B, c
                GROUP BY a.i.x AS x, B.j.Y AS Y, c.k.z AS z
                """
            ),
            // Other expressions
            TransformTestCase(
                """
                SELECT *
                FROM a
                GROUP BY 1+2
                """,
                """
                SELECT * 
                FROM a
                GROUP BY 1+2 AS _1
                """
            ),
            TransformTestCase(
                """
                SELECT *
                FROM a
                GROUP BY 1+2, 3+4
                """,
                """
                SELECT * 
                FROM a
                GROUP BY 1+2 AS _1, 3+4 AS _2
                """
            ),
            TransformTestCase(
                """
                SELECT *
                FROM a 
                GROUP BY a.b AS foo, a.c, 2+3
                """,
                """
                SELECT * 
                FROM a 
                GROUP BY a.b AS foo, a.c AS c, 2+3 as _3
                """
            ),
            TransformTestCase(
                """
                SELECT *
                FROM a, b
                GROUP BY a + b, a + 1 AS foo, b.y + 2
                """,
                """
                SELECT * 
                FROM a, b
                GROUP BY a + b AS _1, a + 1 AS foo, b.y + 2 AS _3
                """
            ),
            TransformTestCase(
                """
                SELECT *
                FROM a, B
                GROUP BY a.x AS foo, B.Y AS bar
                """,
                """
                SELECT *
                FROM a, B
                GROUP BY a.x AS foo, B.Y AS bar
                """
            ),
            TransformTestCase(
                """
                SELECT *
                FROM (SELECT B.y FROM B GROUP BY B.y) AS a
                GROUP BY a
                """,
                """
                SELECT *
                FROM (SELECT B.y FROM B GROUP BY B.y AS y) AS a
                GROUP BY a AS a
                """
            ),
            TransformTestCase(
                """
                SELECT (SELECT B.y FROM B GROUP BY B.y)
                FROM a
                GROUP BY a AS a
                """,
                """
                SELECT (SELECT B.y FROM B GROUP BY B.y AS y)
                FROM a
                GROUP BY a AS a
                """
            ),
            TransformTestCase(
                """
                SELECT (SELECT * FROM B GROUP BY 1+2)
                FROM a
                GROUP BY 3+4
                """,
                """
                SELECT (SELECT * FROM B GROUP BY 1+2 AS _1)
                FROM a
                GROUP BY 3+4 AS _1
                """
            ),
            TransformTestCase(
                """
                SELECT (SELECT * FROM B GROUP BY B.y, 1+2)
                FROM a
                GROUP BY 3+4
                """,
                """
                SELECT (SELECT * FROM B GROUP BY B.y AS y, 1+2 AS _2)
                FROM a
                GROUP BY 3+4 AS _1
                """
            )
        )
    }

    @ParameterizedTest
    @ArgumentsSource(ArgsProvider::class)
    fun test(tc: TransformTestCase) = runTestForIdempotentTransform(tc, GroupByItemAliasVisitorTransform())
}
