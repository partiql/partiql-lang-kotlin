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

package org.partiql.lang.eval

import com.amazon.ion.*
import org.partiql.lang.*
import org.junit.*

class NodeMetadataLookupTest : TestBase() {
    private fun String.sexp() = ion.singleValue(this) as IonSexp
    private fun String.struct() = ion.singleValue(this) as IonStruct

    private fun NodeMetadataLookup.assertMetadata(expected: NodeMetadata?,
                                                  ast: IonValue) = assertEquals("$ast metadata is not $expected",
                                                                                expected,
                                                                                this[ast])

    /**
     * Collects all values, not only leaves, inside an [IonContainer]
     */
    private fun IonContainer.collectAllValues(): List<IonValue> {
        val acc = mutableListOf<IonValue>()
        acc.add(this)

        this.forEach {
            when (it) {
                is IonContainer -> acc.addAll(it.collectAllValues())
                else            -> acc.add(it)
            }
        }

        return acc.toList()
    }

    @Test
    fun noMetaNode() {
        val original = "(a 1 (b 2))"
        val (ast, lookup) = NodeMetadataLookup.extractMetaNode(original.sexp())

        assertEquals(original, ast.toString())

        ast.collectAllValues().forEach { lookup.assertMetadata(null, it) }
    }

    @Test
    fun singleTopLevelMetaNode() {
        val metaStruct = "{line: 1, column: 2}"
        val metadata = NodeMetadata(metaStruct.struct())

        val original = "(a 1 (b 2))"
        val originalWithMeta = "(meta $original $metaStruct)"

        val (ast, lookup) = NodeMetadataLookup.extractMetaNode(originalWithMeta.sexp())

        assertEquals(original, ast.toString())

        ast.collectAllValues().forEach { lookup.assertMetadata(metadata, it) }
    }


    @Test
    fun singleMidLevelMetaNode() {
        val metaStruct = "{line: 1, column: 2}"
        val metadata = NodeMetadata(metaStruct.struct())

        val (ast, lookup) = NodeMetadataLookup.extractMetaNode("(a 1 (meta (b 2) $metaStruct))".sexp())

        assertEquals("(a 1 (b 2))", ast.toString())

        with(lookup) {
            assertMetadata(null, ast)    // (a 1 (b 2))
            assertMetadata(null, ast[0]) // a
            assertMetadata(null, ast[1]) // 1

            val withMeta = ast[2] as IonSexp // (b 2)
            withMeta.collectAllValues().forEach { assertMetadata(metadata, it) }
        }
    }

    @Test
    fun allDifferentMetaNodes() {
        val astWithMeta = """
            (meta
                (
                    (meta a {line: 121, column: 122})
                    (meta 1 {line: 131, column: 132})
                    (meta
                        (
                            (meta b {line: 221, column: 222})
                            (meta 2 {line: 231, column: 232})
                        )
                        {line: 211, column: 212}
                    )
                )
                {line: 111, column: 112}
            )
        """.sexp()

        val (ast, lookup) = NodeMetadataLookup.extractMetaNode(astWithMeta)


        with(lookup) {
            assertEquals("(a 1 (b 2))", ast.toString())
            assertMetadata(NodeMetadata(111, 112), ast)

            assertEquals("a", ast[0].toString())
            assertMetadata(NodeMetadata(121, 122), ast[0])

            assertEquals("1", ast[1].toString())
            assertMetadata(NodeMetadata(131, 132), ast[1])

            val b = ast[2] as IonSexp

            assertEquals("(b 2)", b.toString())
            assertMetadata(NodeMetadata(211, 212), b)

            assertEquals("b", b[0].toString())
            assertMetadata(NodeMetadata(221, 222), b[0])

            assertEquals("2", b[1].toString())
            assertMetadata(NodeMetadata(231, 232), b[1])
        }
    }

    @Test
    fun differentMetaNodeForEqualAstNode() {
        val astWithMeta = """
            (
                (meta a {line: 11, column: 12})
                (meta a {line: 21, column: 22})
            )
        """.sexp()

        val (ast, lookup) = NodeMetadataLookup.extractMetaNode(astWithMeta)

        with(lookup) {
            assertEquals("(a a)", ast.toString())
            assertMetadata(null, ast)

            assertEquals("a", ast[0].toString())
            assertMetadata(NodeMetadata(11, 12), ast[0])

            assertEquals("a", ast[0].toString())
            assertMetadata(NodeMetadata(21, 22), ast[1])
        }
    }

    @Test
    fun withStruct() {
        val astWithMeta = """
            (
                (meta
                    {
                        a: 1,
                        b: (meta 2 {line: 21, column: 22})
                    }
                    {line: 11, column: 12}
                )
            )
        """.sexp()

        val (ast, lookup) = NodeMetadataLookup.extractMetaNode(astWithMeta)

        with(lookup) {
            assertEquals("({a:1,b:2})", ast.toString())
            assertMetadata(null, ast)

            assertEquals("{a:1,b:2}", ast[0].toString())
            assertMetadata(NodeMetadata(11, 12), ast[0])

            val str = ast[0] as IonStruct
            assertMetadata(NodeMetadata(11, 12), str["a"]) // a:1
            assertMetadata(NodeMetadata(21, 22), str["b"]) // b:2
        }
    }

    @Test
    fun withStructPreservesAnnotations() {
        val astWithMeta = """
            ((meta (lit [f::[a::b::{x:c::1,y:2,z:e::a}]]) {line:1,column:23}))
        """.sexp()

        val (ast, lookup) = NodeMetadataLookup.extractMetaNode(astWithMeta)

        with(lookup) {
            assertEquals("((lit [f::[a::b::{x:c::1,y:2,z:e::a}]]))", ast.toString())
            assertMetadata(null, ast)

            assertEquals("(lit [f::[a::b::{x:c::1,y:2,z:e::a}]])", ast[0].toString())
            assertMetadata(NodeMetadata(1, 23), ast[0])

            val sexp = ast[0] as com.amazon.ion.IonSexp
            val str = ((sexp[1] as com.amazon.ion.IonList)[0] as com.amazon.ion.IonList)[0] as com.amazon.ion.IonStruct
            assertNotNull(str.typeAnnotationSymbols)
        }
    }
}
