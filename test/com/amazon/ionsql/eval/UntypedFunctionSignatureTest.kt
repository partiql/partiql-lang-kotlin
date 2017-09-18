/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import org.junit.*
import org.junit.Assert.assertEquals

class UntypedFunctionSignatureTest {

    private val subject = UntypedFunctionSignature("funName")

    @Test
    fun toStringTest() = assertEquals("funName(Any...): Any",
                                      subject.toString())

    @Test
    fun values() {
        assertEquals("funName", subject.name)
        assertEquals(listOf(VarargFormalParameter(StaticType.ANY)), subject.formalParameters)
        assertEquals(StaticType.ANY, subject.returnType)
    }
}