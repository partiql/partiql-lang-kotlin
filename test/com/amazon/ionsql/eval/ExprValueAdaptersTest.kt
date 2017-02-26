/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ionsql.Base
import com.amazon.ionsql.util.exprValue
import org.junit.Test

class ExprValueAdaptersTest : Base() {
    @Test
    fun asNamed() {
        val value = literal("5").exprValue()
        val named = value.asNamed()
        assertSame(value, named.name)
    }

    @Test
    fun unnamedValue() {
        val value = literal("{a:5}").exprValue().bindings["a"]!!
        assertNotNull(value.asFacet(Named::class.java))
        assertNull(value.unnamedValue().asFacet(Named::class.java))
    }
}
