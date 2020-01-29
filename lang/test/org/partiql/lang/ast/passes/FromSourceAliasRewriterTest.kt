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

class FromSourceAliasRewriterTest : RewriterTestBase() {

    @Test
    @Parameters
    fun transformTest(tc: RewriterTestCase) = runTestForIdempotentRewriter(tc, FromSourceAliasRewriter())

    fun parametersForTransformTest() = listOf(
        //Aliases extracted from variable reference names
        RewriterTestCase(
            "SELECT * FROM a",
            "SELECT * FROM a AS a"),
        RewriterTestCase(
            "SELECT * FROM a AT z",
            "SELECT * FROM a AS a AT z"),

        RewriterTestCase(
            "SELECT * FROM a, b",
            "SELECT * FROM a AS a, b AS b"),
        RewriterTestCase(
            "SELECT * FROM a, a",
            "SELECT * FROM a AS a, a AS a"),

        RewriterTestCase(
            "SELECT * FROM a AT z, b AT y",
            "SELECT * FROM a AS a AT z, b AS b AT y"),

        RewriterTestCase(
            "SELECT * FROM a, b, c",
            "SELECT * FROM a AS a, b AS b, c AS c"),
        RewriterTestCase(
            "SELECT * FROM a AT z, b AT y, c AT x",
            "SELECT * FROM a AS a AT z, b AS b AT y, c AS c AT x"),

        //Path variants of the above
        RewriterTestCase(
            "SELECT * FROM foo.a",
            "SELECT * FROM foo.a AS a"),

        RewriterTestCase(
            "SELECT * FROM foo.a, bar.b",
            "SELECT * FROM foo.a AS a, bar.b AS b"),

        RewriterTestCase(
            "SELECT * FROM foo.a, bar.a",
            "SELECT * FROM foo.a AS a, bar.a AS a"),

        RewriterTestCase(
            "SELECT * FROM foo.a, foo.bar.a",
            "SELECT * FROM foo.a AS a, foo.bar.a AS a"),

        RewriterTestCase(
            "SELECT * FROM foo.a, bar.b, bat.c",
            "SELECT * FROM foo.a AS a, bar.b AS b, bat.c AS c"),

        RewriterTestCase(
            "SELECT * FROM foo.doo.a",
            "SELECT * FROM foo.doo.a AS a"),

        RewriterTestCase(
            "SELECT * FROM foo.doo.a, bar.doo.b",
            "SELECT * FROM foo.doo.a AS a, bar.doo.b AS b"),

        RewriterTestCase(
            "SELECT * FROM foo.doo.a, bar.doo.b, bat.doo.c",
            "SELECT * FROM foo.doo.a AS a, bar.doo.b AS b, bat.doo.c AS c"),

        //Aliases synthesized by position in reference
        RewriterTestCase(
            "SELECT * FROM <<a>>",
            "SELECT * FROM <<a>> AS _1"),

        RewriterTestCase(
            "SELECT * FROM <<a>>, <<b>>",
            "SELECT * FROM <<a>> AS _1, <<b>> AS _2"),

        RewriterTestCase(
            "SELECT * FROM <<a>>, <<b>>, <<c>>",
            "SELECT * FROM <<a>> AS _1, <<b>> as _2, <<c>> AS _3"),

        RewriterTestCase(
            "SELECT * FROM a, <<b>>, <<c>>",
            "SELECT * FROM a AS a, <<b>> as _2, <<c>> AS _3"),

        RewriterTestCase(
            "SELECT * FROM <<a>>, b, <<c>>",
            "SELECT * FROM <<a>> AS _1, b AS b, <<c>> AS _3"),

        RewriterTestCase(
            "SELECT * FROM <<a>>, <<b>>, c",
            "SELECT * FROM <<a>> AS _1, <<b>> AS _2, c AS c"),


        //Subqueries should be independent
        RewriterTestCase(
            "SELECT * FROM (SELECT * FROM <<c>>, <<d>>), <<a>>, <<b>>",
            "SELECT * FROM (SELECT * FROM <<c>> AS _1, <<d>> AS _2) AS _1, <<a>> AS _2, <<b>> AS _3"),
        RewriterTestCase(
            "SELECT * FROM a, (SELECT a.x, b.y FROM b)",
            "SELECT * FROM a AS a, (SELECT a.x, b.y FROM b AS b) AS _2"),

        //The rewrite should apply to subqueries even if the from source they are contained within has already been
        //aliased.
        RewriterTestCase(
            "SELECT * FROM (SELECT * FROM <<c>>, <<d>>) AS z, <<a>>, <<b>>",
            "SELECT * FROM (SELECT * FROM <<c>> AS _1, <<d>> AS _2) AS z, <<a>> AS _2, <<b>> AS _3"),

        //UNPIVOT variants of the above
        RewriterTestCase(
            "SELECT * FROM UNPIVOT a",
            "SELECT * FROM UNPIVOT a AS a"),
        RewriterTestCase(
            "SELECT * FROM UNPIVOT a AT z",
            "SELECT * FROM UNPIVOT a AS a AT z"),
        RewriterTestCase(
            "SELECT * FROM UNPIVOT <<a>> AT z",
            "SELECT * FROM UNPIVOT <<a>> AS _1 AT z"),
        RewriterTestCase(
            "SELECT * FROM UNPIVOT (SELECT * FROM <<c>>, <<d>>), <<a>>, <<b>>",
            "SELECT * FROM UNPIVOT (SELECT * FROM <<c>> AS _1, <<d>> AS _2) AS _1, <<a>> AS _2, <<b>> AS _3"),
        RewriterTestCase(
            "SELECT * FROM UNPIVOT (SELECT * FROM <<c>>, <<d>>) AS z, <<a>>, <<b>>",
            "SELECT * FROM UNPIVOT (SELECT * FROM <<c>> AS _1, <<d>> AS _2) AS z, <<a>> AS _2, <<b>> AS _3"),

        // DML
        RewriterTestCase(
            "FROM dogs INSERT INTO collies VALUE ?",
            "FROM dogs AS dogs INSERT INTO collies VALUE ?")
    )

}