/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.ast.passes

import junitparams.*
import org.junit.*

class GroupByPathExpressionRewriterTest  : RewriterTestBase() {


    // We need [GroupByItemAliasRewriter] below because it adds metas which are essential to the operation of
    // [GroupByPathExpressionRewriter].
    @Test
    @Parameters
    fun transformTest(tc: RewriterTestCase) =
        runTest(tc, listOf(GroupByItemAliasRewriter(), GroupByPathExpressionRewriter()))

    /**
     * All the from sources here are manually aliased since the [GroupByPathExpressionRewriter] requires this
     * and we are not running the rewrite that synthesizes the implicit aliases.
     */
    fun parametersForTransformTest() =
        listOf(
            // SELECT list expressions

            // replaces foo.bar with "bar"
            RewriterTestCase(
                """SELECT foo.bar FROM a AS a GROUP BY foo.bar""",
                """SELECT "bar"   FROM a AS a GROUP BY foo.bar as bar"""),

            // same as above, but with varying case identifier case for `foo`
            RewriterTestCase(
                """SELECT fOo.bar FROM foo AS foo GROUP BY FoO.bar""",
                """SELECT "bar"   FROM foo AS foo GROUP BY FoO.bar as bar"""),

            // same as above, but with varying case identifier case for `bar`
            RewriterTestCase(
                """SELECT foo.bAr FROM a AS a GROUP BY foo.BaR""",
                """SELECT "BaR"   FROM a AS a GROUP BY foo.BaR as BaR"""),

            // replaces foo.bar, foo.bar with "bar", "bar"
            RewriterTestCase(
                """SELECT foo.bar, foo.bar  FROM a AS a GROUP BY foo.bar""",
                """SELECT    "bar",   "bar" FROM a AS a GROUP BY foo.bar as bar"""),

            // replaces foo.bar as bat with "bar" as bat
            RewriterTestCase(
                """SELECT foo.bar AS bat FROM a AS a GROUP BY foo.bar""",
                """SELECT "bar"   AS bat FROM a AS a GROUP BY foo.bar as bar"""),

            // replaces foo.bar, foo.bat with "bar", "bat"
            RewriterTestCase(
                """SELECT foo.bar, foo.bat FROM a AS a GROUP BY foo.bar, foo.bat""",
                """SELECT "bar", "bat"     FROM a AS a GROUP BY foo.bar as bar, foo.bat as bat"""),

            // does not replace anything in an aggregate function call
            RewriterTestCase(
                """SELECT SUM(foo.bar) FROM a AS a GROUP BY foo.bar""",
                """SELECT SUM(foo.bar) FROM a AS a GROUP BY foo.bar as bar"""),

            // does not replace anything in an aggregate function call, with GROUP BY path expression
            RewriterTestCase(
                """SELECT foo.bar, SUM(foo.bar) FROM a AS a GROUP BY foo.bar""",
                """SELECT "bar", SUM(foo.bar) FROM a AS a GROUP BY foo.bar as bar"""),

            // Replaces sub-expressions (#1)
            RewriterTestCase(
                """SELECT foo.bar || 'some_string' FROM foo AS foo GROUP BY foo.bar""",
                """SELECT "bar" || 'some_string'   FROM foo AS foo GROUP BY foo.bar AS bar"""),

            // Replaces sub-expressions (#2)
            RewriterTestCase(
                """SELECT (1 + 2 * 3 / (4 + foo.bar)) FROM foo AS foo GROUP BY foo.bar""",
                """SELECT (1 + 2 * 3 / (4 + "bar"))   FROM foo AS foo GROUP BY foo.bar AS bar"""),

            // HAVING

            // replaces foo.bar with "bar"
            RewriterTestCase(
                """SELECT * FROM a AS a GROUP BY foo.bar        HAVING foo.bar  =  1""",
                """SELECT * FROM a AS a GROUP BY foo.bar as bar HAVING    "bar" = 1"""),

            // replaces foo.bar, foo.bat with "bar", "bat"
            RewriterTestCase(
                """SELECT * FROM a AS a GROUP BY foo.bar,        foo.bat        HAVING foo.bar = 1 AND foo.bat  = 2""",
                """SELECT * FROM a AS a GROUP BY foo.bar as bar, foo.bat as bat HAVING "bar" = 1   AND    "bat" = 2"""),

            // does not replace anything in an aggregate function call
            RewriterTestCase(
                """SELECT * FROM a AS a GROUP BY foo.bar        HAVING SUM(foo.bar) > 1""",
                """SELECT * FROM a AS a GROUP BY foo.bar as bar HAVING SUM(foo.bar) > 1"""),

            // Replaces sub-expressions (#1)
            RewriterTestCase(
                """SELECT * FROM foo AS foo GROUP BY foo.bar        HAVING foo.bar + 1""",
                """SELECT * FROM foo AS foo GROUP BY foo.bar AS bar HAVING "bar" + 1"""),

            // Replaces sub-expressions (#2)
            RewriterTestCase(
                """SELECT * FROM foo AS foo GROUP BY foo.bar        HAVING (1 + 2 * 3 / (4 + foo.bar )) = 1""",
                """SELECT * FROM foo AS foo GROUP BY foo.bar AS bar HAVING (1 + 2 * 3 / (4 +    "bar")) = 1"""),

            // Group by expressions which have explicitly specified aliases should not be rewritten
            RewriterTestCase(
                """SELECT foo.bar AS bat FROM a AS a GROUP BY foo.bar as bar""",
                """SELECT foo.bar AS bat FROM a AS a GROUP BY foo.bar as bar"""),

            // Sub-queries

            // The rewrite can happen anywhere within a sub-query
            RewriterTestCase(
                """SELECT (SELECT a.b FROM a.b AS x WHERE a.b = 1 GROUP BY a.b HAVING a.b = 1) FROM a AS a GROUP BY a.b""",
                """SELECT (SELECT "b" FROM "b" AS x WHERE "b" = 1 GROUP BY "b" AS b HAVING "b" = 1) FROM a AS a GROUP BY a.b AS b"""),

            // Path expressions in sub-queries whose root expression is a reference to a variable introduced in the
            // from clause of the *parent* should be rewritten
           RewriterTestCase(
                """SELECT a.b, (SELECT a.b, c.d FROM c AS c) FROM a AS a GROUP BY a.b""",
                """SELECT "b", (SELECT "b", c.d FROM c AS c) FROM a AS a GROUP BY a.b AS b"""),

            // Multiple nested sub-queries are rewritten correctly.
            RewriterTestCase(
                """
                    SELECT
                        a.b,
                        c.d,
                        e.f,
                        (SELECT
                            a.b,
                            c.d,
                            e.f,
                            (SELECT
                                a.b,
                                c.d,
                                e.f
                             FROM e AS e
                             GROUP BY e.f)
                         FROM c AS c
                         GROUP BY c.d)
                    FROM a AS a
                    GROUP BY a.b
                """,
                """
                    SELECT
                        "b",
                        c.d,            -- c and e are defined in a deeper nested scope, so no rewrite
                        e.f,
                        (SELECT
                            "b",
                            "d",
                            e.f,        -- e is still defined in a deeper nested scope, so no rewrite
                            (SELECT
                                "b",
                                "d",
                                "f"
                             FROM e AS e
                             GROUP BY e.f as f)
                         FROM c AS c
                         GROUP BY c.d as d)
                    FROM a AS a
                    GROUP BY a.b as b
                """),
            RewriterTestCase(
                """
                    SELECT
                        (SELECT
                            f.bar
                        FROM far AS f)
                    FROM foo AS f
                    GROUP BY f.bar
                """,
                """
                    SELECT
                        (SELECT
                            -- f.bar does not get replaced because `f` is defined in this scope
                            -- and has a different meaning than `f` from the parent scopes.
                            f.bar
                        FROM far AS f)
                    FROM foo AS f
                    GROUP BY f.bar AS bar
                """),
            RewriterTestCase(
                """
                    SELECT
                        (SELECT
                            f.bar
                        FROM far AS f
                        GROUP BY f.bar)
                    FROM foo AS f
                    GROUP BY f.bar
                """,
                """
                    SELECT
                        (SELECT
                            -- the second f.bar does get replaced because the `f.bar` is used in a GROUP BY
                            -- and in this case `f` shadows the outer `f`.
                            "bar"
                        FROM far AS f
                        GROUP BY f.bar AS bar)
                    FROM foo AS f
                    GROUP BY f.bar AS bar
                """),
            RewriterTestCase(
                """
                    SELECT
                        (SELECT
                            f.bar,
                            (SELECT
                                f.bar
                            FROM far AS f)
                        FROM doesnt AS matter)
                    FROM foo AS f
                    GROUP BY f.bar
                """,
                """
                    SELECT
                        (SELECT
                            "bar", --the first f.bar gets replaced because `f` is defined in a parent scope
                            (SELECT
                                -- the second f.bar does not get replaced because `f` is defined in this scope
                                -- and has a different meaning than `f` from the parent scopes.
                                f.bar
                            FROM far AS f)
                        FROM doesnt AS matter)
                    FROM foo AS f
                    GROUP BY f.bar AS bar
                """),
            RewriterTestCase(
                """
                    SELECT
                        (SELECT f.bar
                        FROM f.bar AS f)
                    FROM f.bar AS f
                    GROUP BY f.bar
                """,
                """
                    SELECT
                        (SELECT f.bar    -- this f.bar is not rewritten because `f` is shadowed in the inner SELECT
                        FROM "bar" AS f) -- this f.bar is rewritten because `f` refers to `bar` introduced in `GROUP BY` below
                    FROM f.bar AS f
                    GROUP BY f.bar AS bar
                """),
            // This is similar to the previous but another SELECT sub-query is added between the outer and inner queries.
            RewriterTestCase(
                """
                    SELECT
                        (SELECT
                            (SELECT f.bar
                            FROM f.bar AS f)
                        FROM somethingElse AS s)
                    FROM f.bar AS f
                    GROUP BY f.bar
                """,
                """
                    SELECT
                        (SELECT
                            (SELECT f.bar    -- this f.bar is not rewritten because `f` is shadowed in the inner SELECT
                            FROM "bar" AS f) -- this f.bar is rewritten because `f` refers to `bar` introduced in `GROUP BY` below
                        FROM somethingElse AS s)
                    FROM f.bar AS f
                    GROUP BY f.bar AS bar
                """),
            RewriterTestCase(
                """
                    SELECT
                        (SELECT
                            (SELECT f.bar, s.lar
                            FROM f.bar AS f, s.lar AS s)
                        FROM somethingElse AS s
                        GROUP BY s.lar)
                    FROM f.bar AS f
                    GROUP BY f.bar
                """,
                """
                     SELECT
                        (SELECT
                            (SELECT f.bar, s.lar         -- neither s.bar or s.lar is rewritten because `f` and `s` are shadowing
                            FROM "bar" AS f, "lar" AS s) -- but f.bar and s.lar are rewritten because `f` and `s` refer to variables defined in higher scopes.
                        FROM somethingElse AS s
                        GROUP BY s.lar as lar)
                    FROM f.bar AS f
                    GROUP BY f.bar as bar
                """),
            RewriterTestCase(
                """
                    SELECT
                        f.bar,
                        (PIVOT f.bar AT b.o
                        FROM f.bar AS f)
                    FROM foo AS f
                    GROUP BY f.bar
                """,
                """
                    SELECT
                        "bar",
                        (PIVOT f.bar AT b.o -- this `f.bar` is not rewritten in this case because `f` is defined in the FROM clause
                        FROM "bar" AS f --this `f.bar` is rewritten
                        )
                    FROM foo AS f
                    GROUP BY f.bar AS bar
                """)
        )
}