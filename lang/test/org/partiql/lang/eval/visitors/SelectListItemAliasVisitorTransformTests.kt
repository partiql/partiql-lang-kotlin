package org.partiql.lang.eval.visitors

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.util.ArgumentsProviderBase

class SelectListItemAliasVisitorTransformTests : VisitorTransformTestBase() {

    class ArgsProvider : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // singular variable references
            TransformTestCase(
                """
                SELECT 
                    a 
                FROM foo
                """,
                """
                SELECT 
                    a AS a 
                FROM foo
                """
            ),
            TransformTestCase(
                """
                SELECT 
                    a,
                    b
                FROM foo
                """,
                """
                SELECT 
                    a AS a,
                    b AS b 
                FROM foo
                """
            ),
            TransformTestCase(
                """
                SELECT 
                    a,
                    B,
                    c
                FROM foo
                """,
                """
                SELECT 
                    a AS a,
                    B AS B,
                    c AS c 
                FROM foo
                """
            ),
            // path expressions
            TransformTestCase(
                """
                SELECT 
                    x.a
                FROM foo
                """,
                """
                SELECT 
                    x.a AS a 
                FROM foo
                """
            ),
            TransformTestCase(
                """
                SELECT 
                    x.a,
                    x.B
                FROM foo
                """,
                """
                SELECT 
                    x.a AS a,
                    x.B AS B 
                FROM foo
                """
            ),
            TransformTestCase(
                """
                SELECT 
                    x.a,
                    x.B,
                    x.c
                FROM foo
                """,
                """
                SELECT 
                    x.a AS a,
                    x.B AS B,
                    x.c AS c 
                FROM foo
                """
            ),
            TransformTestCase(
                """
                SELECT 
                    x.m.a,
                    x.n.B,
                    x.o.c
                FROM foo
                """,
                """
                SELECT 
                    x.m.a AS a,
                    x.n.B AS B,
                    x.o.c AS c 
                FROM foo
                """
            ),
            // Other expressions resulting in synthetic names.
            TransformTestCase(
                """
                SELECT 
                    99
                FROM foo
                """,
                """
                SELECT 
                    99 AS _1
                FROM foo
                """
            ),
            TransformTestCase(
                """
                SELECT 
                    99,
                    101
                FROM foo
                """,
                """
                SELECT 
                    99 AS _1,
                    101 AS _2
                FROM foo
                """
            ),
            TransformTestCase(
                """
                SELECT 
                    99,
                    bar,
                    bork AS fwee,
                    bat,
                    101
                FROM foo
                """,
                """
                SELECT 
                    99 AS _1,
                    bar AS bar,
                    bork AS fwee,
                    bat AS bat,
                    101 AS _5
                FROM foo
                """
            ),
            TransformTestCase(
                """
                SELECT 
                    x + 100,
                    y + 101
                FROM foo
                """,
                """
                SELECT 
                    x + 100 AS _1,
                    y + 101 AS _2
                FROM foo
                """
            ),
            TransformTestCase(
                """
                SELECT 
                    (SELECT n FROM p),
                    (SELECT q FROM r) AS o
                FROM foo
                """,
                """
                SELECT 
                    (SELECT n AS n FROM p) AS _1,
                    (SELECT q AS q FROM r) AS o
                FROM foo
                """
            ),
            TransformTestCase(
                """
                SELECT 
                    (SELECT 
                        (SELECT n FROM p1) FROM p),
                    (SELECT 
                        (SELECT q FROM r1) FROM r) AS o
                FROM foo
                """,
                """
                SELECT 
                    (SELECT 
                        (SELECT n AS n FROM p1) AS _1 FROM p) AS _1,
                    (SELECT 
                        (SELECT q AS q FROM r1) AS _1 FROM r) AS o
                FROM foo
                """
            )
        )
    }

    @ParameterizedTest
    @ArgumentsSource(ArgsProvider::class)
    fun test(tc: TransformTestCase) = runTestForIdempotentTransform(tc, SelectListItemAliasVisitorTransform())
}
