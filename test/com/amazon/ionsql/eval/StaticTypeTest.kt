/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ionsql.eval.StaticType.Companion.unionOf
import org.junit.Assert.*
import org.junit.Test

class StaticTypeTest {
    private fun assertTypes(static: StaticType, vararg exprValueTypes: ExprValueType) {
        val expectedSet = exprValueTypes.toSet()

        expectedSet.forEach {
            assertTrue("$static does not types $it, should type any of [$expectedSet]", static.isOfType(it))
        }

        assertEquals("$static types any of [$static.typeDomain], while should type any of: [$expectedSet]", expectedSet, static.typeDomain)

        ExprValueType.values()
            .filter { !expectedSet.contains(it) }
            .forEach {
                assertFalse("$static types $it, while should only type any of [$expectedSet]", static.isOfType(it))
            }
    }

    @Test
    fun missingOnlyTypesMissing() = assertTypes(StaticType.MISSING, ExprValueType.MISSING)

    @Test
    fun boolOnlyTypesBool() = assertTypes(StaticType.BOOL, ExprValueType.BOOL)

    @Test
    fun nullOnlyTypesNull() = assertTypes(StaticType.NULL, ExprValueType.NULL)

    @Test
    fun intOnlyTypesInt() = assertTypes(StaticType.INT, ExprValueType.INT)

    @Test
    fun floatOnlyTypesFloat() = assertTypes(StaticType.FLOAT, ExprValueType.FLOAT)

    @Test
    fun decimalOnlyTypesDecimal() = assertTypes(StaticType.DECIMAL, ExprValueType.DECIMAL)

    @Test
    fun timestampOnlyTypesTimestamp() = assertTypes(StaticType.TIMESTAMP, ExprValueType.TIMESTAMP)

    @Test
    fun symbolOnlyTypesSymbol() = assertTypes(StaticType.SYMBOL, ExprValueType.SYMBOL)

    @Test
    fun stringOnlyTypesString() = assertTypes(StaticType.STRING, ExprValueType.STRING)

    @Test
    fun clobOnlyTypesClob() = assertTypes(StaticType.CLOB, ExprValueType.CLOB)

    @Test
    fun blobOnlyTypesBlob() = assertTypes(StaticType.BLOB, ExprValueType.BLOB)

    @Test
    fun listOnlyTypesList() = assertTypes(StaticType.LIST, ExprValueType.LIST)

    @Test
    fun sexpOnlyTypesSexp() = assertTypes(StaticType.SEXP, ExprValueType.SEXP)

    @Test
    fun structOnlyTypesStruct() = assertTypes(StaticType.STRUCT, ExprValueType.STRUCT)

    @Test
    fun bagOnlyTypesBag() = assertTypes(StaticType.BAG, ExprValueType.BAG)

    @Test
    fun anyTypesAllSingle() = assertTypes(StaticType.ANY, *ExprValueType.values())

    @Test
    fun numericTypesNumericTypes() =
        assertTypes(StaticType.NUMERIC, ExprValueType.INT, ExprValueType.FLOAT, ExprValueType.DECIMAL)

    @Test
    fun unionTypesContainedSingles() =
        assertTypes(unionOf("union", StaticType.MISSING), ExprValueType.MISSING)

    @Test
    fun unionTypesContainedUnions() {
        val s1 = unionOf("union1", StaticType.MISSING, StaticType.NULL)
        val s2 = unionOf("union2", StaticType.DECIMAL)

        val union = unionOf("union3", s1, s2)

        assertTypes(union, ExprValueType.DECIMAL, ExprValueType.MISSING, ExprValueType.NULL)
    }
}