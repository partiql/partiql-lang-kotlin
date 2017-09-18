/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import org.junit.Assert.*
import org.junit.Test

class FormalParameterTest {
    @Test
    fun paramToString() = assertEquals("Bool", SingleFormalParameter(StaticType.BOOL).toString())

    @Test
    fun varargToString() = assertEquals("Bool...", VarargFormalParameter(StaticType.BOOL).toString())
}