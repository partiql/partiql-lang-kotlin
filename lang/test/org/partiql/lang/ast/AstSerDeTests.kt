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

package org.partiql.lang.ast

import com.amazon.ion.*
import org.partiql.lang.*
import org.partiql.lang.util.*
import junitparams.*
import org.junit.*

/**
 * This class contains tests for (de)serialization of metas.
 * For tests related to basic structure of the serialized [ExprNode]s, see [org.partiql.lang.syntax.SqlParserTest].
 */
class AstSerDeTests : TestBase() {

    private val deserializer = AstDeserializerBuilder(ion)
        .withMetaDeserializer(TestPropertyMeta.deserializer)
        .withMetaDeserializer(TestFlagMeta.deserializer)
        .build()

    fun parametersForSerDeMetas() = listOf(
        Literal(ion.newInt(1), metaContainerOf(TestPropertyMeta(1, 2))),
        Literal(ion.newInt(1), metaContainerOf(TestFlagMeta.instance)),
        Literal(ion.newInt(1), metaContainerOf(TestFlagMeta.instance, TestPropertyMeta(1, 2)))
    )

    @Test
    @Parameters
    fun serDeMetas(testExprNode: ExprNode) {
        //Serialize and then deserialize testExprNode and assert the result matches
        val deserializedExprNode = deserializer.deserialize(AstSerializer.serialize(testExprNode, ion))
        assertEquals(testExprNode, deserializedExprNode)
    }

    /** This test asserts that unknown metas survive serialization and deserialization without issue. */
    @Test
    fun unknowwnMetasTest() {
        val sexpWithUnknownMetas = """
        (ast
            (version 1)
            (root
                (term
                    (exp
                        (lit 1))
                    (meta //Note that these metas are sorted alphabetically as is done during serialization
                        (unknown_flag_meta ())
                        (unknown_meta_with_properties ({foo: 1, bar: 2 } some arbitrary "ion values"))))))
        """

        val originalSexp = ion.singleValue(sexpWithUnknownMetas) as IonSexp
        val ast = deserializer.deserialize(originalSexp)
        val serializedSexp = AstSerializer.serialize(ast, ion)
        assertSexpEquals(originalSexp, serializedSexp)
    }
}

/** This simple meta has no properties and so functions solely as a flag. */
private class TestFlagMeta private constructor() : Meta {
    override val tag = TAG
    companion object
    {
        const val TAG = "demo_flag_meta"
        val instance = TestFlagMeta()
        val deserializer = MemoizedMetaDeserializer(TAG,  instance)
    }
}

/** This meta has some properties and demonstrates how to persist metas. */
private data class TestPropertyMeta(val propertyA: Long, val propertyB: Long) : Meta {
    override val tag = TAG

    override fun serialize(writer: IonWriter) {
        IonWriterContext(writer).apply {
            struct {
                int("propertyA", propertyA)
                int("propertyB", propertyB)
            }
        }
    }

    companion object {
        const val TAG = "demo_meta_with_properties"

        val deserializer = object : MetaDeserializer {
            override val tag = TAG
            override fun deserialize(sexp: IonSexp): Meta {
                val struct = sexp.first().asIonStruct()
                val lineNum = struct.field("propertyA").longValue()
                val charOffset = struct.field("propertyB").longValue()

                return TestPropertyMeta(lineNum, charOffset)
            }
        }
    }
}