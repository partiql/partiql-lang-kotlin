package org.partiql.lang.planner.transforms

import com.amazon.ion.system.IonSystemBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.util.ArgumentsProviderBase

class ToAstVarDeclVisitorTransformTests {

    @ParameterizedTest
    @ArgumentsSource(SelectListAsAliasArguments::class)
    fun selectListAsAliasTests(tc: TransformTestCase) = runTest(tc)

    @ParameterizedTest
    @ArgumentsSource(FromSourceAsAliasArguments::class)
    fun fromSourceAsAliasTests(tc: TransformTestCase) = runTest(tc)

    private val ion = IonSystemBuilder.standard().build()
    private val parser = SqlParser(ion)

    data class TransformTestCase(val originalSql: String, val expectedSql: String)
    
    /**
     * Similar to [runTest], but executes the transform again a second time on the result of the first transform
     * and ensures that the second result is the same as the first.  This ensures that the transform is idempotent.
     */
    private fun runTest(tc: TransformTestCase) {
        val actualAst = assertDoesNotThrow {
            parser.parseAstStatement(tc.originalSql).toAstVarDecl()
        }
        val expectedAst = assertDoesNotThrow {
            parser.parseAstStatement(tc.expectedSql).toAstVarDecl()
        }

        assertEquals(expectedAst, actualAst, "The expected AST must match the transformed AST")
    }
    
    class SelectListAsAliasArguments : ArgumentsProviderBase() {
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

    class FromSourceAsAliasArguments : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            //Aliases extracted from variable reference names
            TransformTestCase(
                "SELECT * FROM a",
                "SELECT * FROM a AS a"
            ),
            TransformTestCase(
                "SELECT * FROM a AT z",
                "SELECT * FROM a AS a AT z"
            ),

            TransformTestCase(
                "SELECT * FROM a, b",
                "SELECT * FROM a AS a, b AS b"
            ),
            TransformTestCase(
                "SELECT * FROM a, a",
                "SELECT * FROM a AS a, a AS a"
            ),

            TransformTestCase(
                "SELECT * FROM a AT z, b AT y",
                "SELECT * FROM a AS a AT z, b AS b AT y"
            ),

            TransformTestCase(
                "SELECT * FROM a, b, c",
                "SELECT * FROM a AS a, b AS b, c AS c"
            ),
            TransformTestCase(
                "SELECT * FROM a AT z, b AT y, c AT x",
                "SELECT * FROM a AS a AT z, b AS b AT y, c AS c AT x"
            ),

            //Path variants of the above
            TransformTestCase(
                "SELECT * FROM foo.a",
                "SELECT * FROM foo.a AS a"
            ),

            TransformTestCase(
                "SELECT * FROM foo.a, bar.b",
                "SELECT * FROM foo.a AS a, bar.b AS b"
            ),

            TransformTestCase(
                "SELECT * FROM foo.a, bar.a",
                "SELECT * FROM foo.a AS a, bar.a AS a"
            ),

            TransformTestCase(
                "SELECT * FROM foo.a, foo.bar.a",
                "SELECT * FROM foo.a AS a, foo.bar.a AS a"
            ),

            TransformTestCase(
                "SELECT * FROM foo.a, bar.b, bat.c",
                "SELECT * FROM foo.a AS a, bar.b AS b, bat.c AS c"
            ),

            TransformTestCase(
                "SELECT * FROM foo.doo.a",
                "SELECT * FROM foo.doo.a AS a"
            ),

            TransformTestCase(
                "SELECT * FROM foo.doo.a, bar.doo.b",
                "SELECT * FROM foo.doo.a AS a, bar.doo.b AS b"
            ),

            TransformTestCase(
                "SELECT * FROM foo.doo.a, bar.doo.b, bat.doo.c",
                "SELECT * FROM foo.doo.a AS a, bar.doo.b AS b, bat.doo.c AS c"
            ),

            //Aliases synthesized by position in reference
            TransformTestCase(
                "SELECT * FROM <<a>>",
                "SELECT * FROM <<a>> AS _1"
            ),

            TransformTestCase(
                "SELECT * FROM <<a>>, <<b>>",
                "SELECT * FROM <<a>> AS _1, <<b>> AS _2"
            ),

            TransformTestCase(
                "SELECT * FROM <<a>>, <<b>>, <<c>>",
                "SELECT * FROM <<a>> AS _1, <<b>> as _2, <<c>> AS _3"
            ),

            TransformTestCase(
                "SELECT * FROM a, <<b>>, <<c>>",
                "SELECT * FROM a AS a, <<b>> as _2, <<c>> AS _3"
            ),

            TransformTestCase(
                "SELECT * FROM <<a>>, b, <<c>>",
                "SELECT * FROM <<a>> AS _1, b AS b, <<c>> AS _3"
            ),

            TransformTestCase(
                "SELECT * FROM <<a>>, <<b>>, c",
                "SELECT * FROM <<a>> AS _1, <<b>> AS _2, c AS c"
            ),


            //Subqueries should be independent
            TransformTestCase(
                "SELECT * FROM (SELECT * FROM <<c>>, <<d>>), <<a>>, <<b>>",
                "SELECT * FROM (SELECT * FROM <<c>> AS _1, <<d>> AS _2) AS _1, <<a>> AS _2, <<b>> AS _3"
            ),
            TransformTestCase(
                "SELECT * FROM a, (SELECT a.x, b.y FROM b)",
                "SELECT * FROM a AS a, (SELECT a.x, b.y FROM b AS b) AS _2"
            ),

            //The transform should apply to subqueries even if the from source they are contained within has already been
            //aliased.
            TransformTestCase(
                "SELECT * FROM (SELECT * FROM <<c>>, <<d>>) AS z, <<a>>, <<b>>",
                "SELECT * FROM (SELECT * FROM <<c>> AS _1, <<d>> AS _2) AS z, <<a>> AS _2, <<b>> AS _3"
            ),

            //UNPIVOT variants of the above
            TransformTestCase(
                "SELECT * FROM UNPIVOT a",
                "SELECT * FROM UNPIVOT a AS a"
            ),
            TransformTestCase(
                "SELECT * FROM UNPIVOT a AT z",
                "SELECT * FROM UNPIVOT a AS a AT z"
            ),
            TransformTestCase(
                "SELECT * FROM UNPIVOT <<a>> AT z",
                "SELECT * FROM UNPIVOT <<a>> AS _1 AT z"
            ),
            TransformTestCase(
                "SELECT * FROM UNPIVOT (SELECT * FROM <<c>>, <<d>>), <<a>>, <<b>>",
                "SELECT * FROM UNPIVOT (SELECT * FROM <<c>> AS _1, <<d>> AS _2) AS _1, <<a>> AS _2, <<b>> AS _3"
            ),
            TransformTestCase(
                "SELECT * FROM UNPIVOT (SELECT * FROM <<c>>, <<d>>) AS z, <<a>>, <<b>>",
                "SELECT * FROM UNPIVOT (SELECT * FROM <<c>> AS _1, <<d>> AS _2) AS z, <<a>> AS _2, <<b>> AS _3"
            ),

            // DML
            TransformTestCase(
                "FROM dogs INSERT INTO collies VALUE ?",
                "FROM dogs AS dogs INSERT INTO collies VALUE ?"
            )
        )
    }

}