/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ionsql.Base
import com.amazon.ionsql.util.*
import org.junit.Test

class BindingsTest : Base() {
    fun bind(text: String): Bindings = literal(text).exprValue().bindings

    fun over(text: String,
             bindingsTransform: Bindings.() -> Bindings,
             block: AssertExprValue.() -> Unit) =
        AssertExprValue(
            literal(text).exprValue(),
            bindingsTransform
        ).run(block)

    @Test
    fun blacklist() = over("{a:1, b:2}", { blacklist("a") }) {
        assertBinding("b") { ion.newInt(2) == ionValue }
        assertNoBinding("a")
    }

    @Test
    fun delegate() = over("{a:1, b:2}", { this.delegate(bind("{b:3, c:4}")) }) {
        assertBinding("b") { ion.newInt(2) == ionValue }
        assertBinding("c") { ion.newInt(4) == ionValue }
    }
}
