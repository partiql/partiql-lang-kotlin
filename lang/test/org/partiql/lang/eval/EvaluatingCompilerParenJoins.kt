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
        "C" to "[ { id : 1 } ]").toSession()

    // Initial tests that no errors come up. Will eventually change tests to check ASTs
    @Test
    fun singleJoinNoParensTest() {
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
    fun naturalOrderJoinParensTest() {
        eval(
            "SELECT 1 FROM (A av INNER JOIN B bv ON av.id = bv.id) INNER JOIN C sv ON bv.id = cv.id",
            CompileOptions.standard(),
            session
        )
    }

    @Test
    @Ignore
    fun specifiedOrderJoinParensTest() {
        eval(
            "SELECT 1 FROM A INNER JOIN (B INNER JOIN C ON B.id = C.id) ON A.id = B.id",
            CompileOptions.standard(),
            session
        )
    }
}
