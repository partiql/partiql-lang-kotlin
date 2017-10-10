/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ionsql.Base
import com.amazon.ionsql.eval.binding.Alias
import com.amazon.ionsql.eval.binding.localsBinder
import com.amazon.ionsql.util.*
import org.junit.Test
import kotlin.test.assertFails

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

    @Test
    fun locals() {
        // Test the bindings of the new localsBinder against the old localsBinder
        val aliases = listOf(Alias("s", null), Alias("t", "z"))
        val exprValues = ion.iterate("{a:1, b:1}{a:2, c:2}").asSequence().map { it.exprValue() }.toList()

        val locals = aliases.localsBinder(missingExprValue(ion)).bindLocals(exprValues)
        val oldLocals = oldLocalsBinder(exprValues, aliases, missingExprValue(ion))

        listOf("s", "t", "z", "a", "b", "c", "d")
            .forEach { key ->
                assertEquals(oldLocals[key]?.ionValue, locals[key]?.ionValue)
            }
    }

    @Test
    fun locals_collision() {
        // Multiple aliases
        val aliases = listOf(Alias("s", null), Alias("t", "s"))
        val exprValues = listOf(integerExprValue(1, ion), integerExprValue(2, ion))

        val locals = aliases.localsBinder(missingExprValue(ion)).bindLocals(exprValues)
        val oldLocals = oldLocalsBinder(exprValues, aliases, missingExprValue(ion))

        assertFails("binding 's' should be a collision and throw") { locals["s"] }
        assertFails("binding 's' should be a collision and throw") { oldLocals["s"] }
        assertEquals(oldLocals["t"], locals["t"])
    }
}


private fun oldLocalsBinder(locals: List<ExprValue>, aliases: List<Alias>, missingValue: ExprValue): Bindings {
    val localBindings = locals.map { it.bindings }

    return Bindings.over { name ->
        val found = localBindings.asSequence()
                .mapIndexed { col, _ ->
                    when (name) {
                    // the alias binds to the value itself
                        aliases[col].asName -> locals[col]
                    // the alias binds to the name of the value
                        aliases[col].atName -> locals[col].name ?: missingValue
                        else -> null
                    }
                }
                .filter { it != null }
                .toList()
        when (found.size) {
        // nothing found at our scope, attempt to look at the attributes in our variables
        // TODO fix dynamic scoping to be in line with SQL++ rules
            0 -> {
                localBindings.asSequence()
                        .map { it[name] }
                        .filter { it != null }
                        .firstOrNull()
            }
        // found exactly one thing, success
            1 -> found.head!!
        // multiple things with the same name is a conflict
            else -> errNoContext("$name is ambigious: ${found.map { it?.ionValue }}")
        }
    }
}
