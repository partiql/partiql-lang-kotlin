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

package org.partiql.lang.eval;

import org.junit.Ignore
import org.junit.Test

class EvaluatingCompilerParenJoins : EvaluatorTestBase() {
    private val session = mapOf("A" to "[ { id : 1 } ]",
        "B" to "[ { id : 1 } ]",
        "C" to "[ { id : 1 } ]",
        "D" to "[ { id : 1 } ]").toSession()

    private val session2 = mapOf("t1" to """[
      {
        name:"bb",
        n:11
      }
    ]""",
    "t2" to """[
      {
        name:"bb",
        n:12
      },
      {
        name:"cc",
        n:22
      },
      {
        name:"ee",
        n:42
      }
    ]""",
            "t3" to """[
      {
        name:"bb",
        n:13
      },
      {
        name:"cc",
        n:23
      },
      {
        name:"dd",
        n:33
      }
    ]""").toSession()

    // Initial tests that no errors come up. Will eventually change tests to check ASTs
    @Test
    fun singleJoinNoParensTest() {
        eval(
            "SELECT 1 FROM A INNER JOIN B ON A.id = B.id",
            CompileOptions.standard(),
            session
        )
    }

    @Test
    fun singleJoinMultiParensTest() {
        eval(
            "SELECT 1 FROM (((A INNER JOIN B ON A.id = B.id)))",
            CompileOptions.standard(),
            session
        )
    }

    @Test
    fun singleJoinNoParensAliasTest() {
        eval(
            "SELECT 1 FROM A av INNER JOIN B bv ON av.id = bv.id",
            CompileOptions.standard(),
            session
        )
    }
    
    @Test
    fun singleJoinParensTest() {
        eval(
            "SELECT 1 FROM (A av INNER JOIN B bv ON av.id = bv.id)",
            CompileOptions.standard(),
            session
        )
    }

    @Test
    fun naturalOrderJoinNoParensTest() {
        eval(
            "SELECT 1 FROM A av INNER JOIN B bv ON av.id = bv.id INNER JOIN C cv ON bv.id = cv.id",
            CompileOptions.standard(),
            session
        )
    }

    @Test
    fun naturalOrder4TableJoinNoParensTest() {
        eval(
            "SELECT 1 FROM A INNER JOIN B ON A.id = B.id INNER JOIN C ON A.id = C.id INNER JOIN D ON A.id=D.id",
            CompileOptions.standard(),
            session
        )
    }

    @Test
    fun naturalOrderJoinParensTest() {
        eval(
            "SELECT 1 FROM (A av INNER JOIN B bv ON av.id = bv.id) INNER JOIN C cv ON bv.id = cv.id",
            CompileOptions.standard(),
            session
        )
    }

    @Test
    fun specifiedOrderJoinParensTest() {
        eval(
            "SELECT 1 FROM A INNER JOIN (B INNER JOIN C ON B.id = C.id) ON A.id = B.id",
            CompileOptions.standard(),
            session
        )
    }

    @Test
    fun specifiedOrderJoinParens4Test() {
        eval(
            "SELECT 1 FROM A INNER JOIN (B INNER JOIN (C INNER JOIN D ON C.id = D.id) ON B.id = C.id) ON A.id = B.id",
            CompileOptions.standard(),
            session
        )
    }

    @Test
    fun specifiedOrderJoinMultiParensTest() {
        eval(
            "SELECT 1 FROM A INNER JOIN (B INNER JOIN (C INNER JOIN D ON C.id = D.id) ON B.id = C.id) ON A.id = B.id",
            CompileOptions.standard(),
            session
        )
    }

    @Test
    fun testBasicParens() {
        eval(
            "SELECT (1 + (2 + (3 + 4))) FROM A",
            CompileOptions.standard(),
            session
        )
    }

    @Test
    fun testBasicFromSourceList() {
        eval(
            "SELECT 1 FROM A, B, C, D",
            CompileOptions.standard(),
            session
        )
    }

    @Test
    fun testBasicFromSourceWithAliasesList() {
        eval(
            "SELECT 1 FROM A AS av, B AS bv, C AS cv, D AS dv",
            CompileOptions.standard(),
            session
        )
    }

    @Test
    fun testBasicFromSourceWithShortAliasesList() {
        eval(
            "SELECT 1 FROM A av, B bv, C cv, D dv",
            CompileOptions.standard(),
            session
        )
    }

    @Test
    fun parenClauseOnly() {
        eval(
                "SELECT 1 + 2 + 3 + 4 FROM A",
                CompileOptions.standard(),
                session
        )
    }

    @Test
    fun parenLeftJoin() {
        eval(
                "SELECT * FROM (SELECT * FROM t2) as s2 LEFT JOIN (SELECT * FROM t3) s3 ON s2.name = s3.name",
                CompileOptions.standard(),
                session2
        )
    }
}
