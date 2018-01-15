/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates.  All rights reserved.
 */

package com.amazon.ionsql.eval

import com.amazon.ionsql.IonSqlException
import junitparams.Parameters
import junitparams.naming.TestCaseName
import org.junit.Test
import java.util.*
import com.amazon.ionsql.eval.NaturalExprValueComparators.*

class NaturalExprValueComparatorsTest : EvaluatorBase() {
    // the lists below represent the expected ordering of values
    // grouped by lists of equivalent values.

    private val basicExprs = listOf(
        listOf(
            // reminder, annotations don't affect order
            "null",
            "missing",
            "`a::null`",
            "`null.int`",
            "`null.struct`"
        ),
        listOf(
            "false",
            "`b::false`"
        ),
        listOf(
            "`c::true`",
            "true"
        ),
        listOf(
            // make sure there are at least two nan
            "`nan`",
            "`nan`"
        ),
        listOf(
            // make sure there are at least two -inf
            "`-inf`",
            "`-inf`"
        ),
        listOf(
            "-1e1000",
            "`-1.000000000000d1000`"
        ),
        listOf(
            "-5e-1",
            "`-0.50000000000000000000000000`",
            "`-0.5e0`"
        ),
        listOf(
            "-0.0",
            "`-0.0000000000`",
            "`0e0`",
            "`0d10000`",
            "0"
        ),
        listOf(
            "5e9",
            "5000000000",
            "`0x12a05f200`",
            "`5.0e9`"
        ),
        listOf(
            // make sure there are at least two +inf
            "`+inf`",
            "`+inf`"
        ),
        listOf(
            "`2017T`",
            "`2017-01T`",
            "`2017-01-01T`",
            "`2017-01-01T00:00-00:00`",
            "`2017-01-01T01:00+01:00`"
        ),
        listOf(
            "`2017-01-01T01:00Z`"
        ),
        listOf(
            "''",
            "`foobar::\"\"`"
        ),
        listOf(
            "`'A'`",
            "'A'",
            "`foobar::\"A\"`"
        ),
        listOf(
            "'AA'"
        ),
        listOf(
            "`a`"
        ),
        listOf(
            "'azzzzzzz'"
        ),
        listOf(
            "'z'"
        ),
        // TODO add a UTF-16 order breaker here to verify we're doing the right thing
        listOf(
            """`"\U0001F4A9"`""",
            """`'\uD83D\uDCA9'`"""
        ),
        listOf(
            "`{{}}`",
            "`{{\"\"}}`"
        ),
        listOf(
            "`{{\"A\"}}`",
            "`{{QQ==}}`"
        ),
        listOf(
            """`{{"aaaaaaaaaaaaa\xFF"}}`""",
            """`{{YWFhYWFhYWFhYWFhYf8=}}`"""
        ),
        listOf(
            "[]",
            "`z::x::y::[]`"
        ),
        listOf(
            "[false, {}]"
        ),
        listOf(
            "[true]"
        ),
        listOf(
            "[true, 100]"
        ),
        listOf(
            "`a::b::c::()`"
        ),
        listOf(
            "`a::b::c::(1e0)`",
            "`(1)`",
            "`(1.0000000000000)`"
        ),
        listOf(
            "`(2012T nan)`"
        ),
        listOf(
            "`(2012T 1 2 3)`"
        ),
        listOf(
            "{}",
            "`m::n::o::{}`"
        ),
        listOf(
            "{'a': true, 'b': 1000, 'c': false}",
            "{'b': `1e3`, 'a': true, 'c': false}"
        ),
        listOf(
            "{'b': 1000, 'c': false}",
            "{'c': false, 'b': 1.00000000e3}"
        ),
        listOf(
            "{'c': false}"
        ),
        listOf(
            "{'d': 1, 'f': 2}"
        ),
        listOf(
            "{'d': 2, 'e': 3, 'f': 4}"
        ),
        listOf(
            "{'d': 3, 'e': 2}"
        ),
        listOf(
            "<<>>"
        ),
        listOf(
            "<<1, true, true>>"
        ),
        listOf(
            "<<true, 1, 1.0, `1e0`, true>>"
        ),
        listOf(
            "<<1>>"
        ),
        listOf(
            "<< <<>>, <<>> >>"
        )
    )

    private fun <T> List<List<T>>.flatten() = this.flatMap { it }
    private fun List<List<String>>.eval() = map {
        it.map {
            try {
                eval(it, compileOptions = CompileOptions.standard())
            } catch (e: Exception) {
                throw IonSqlException("Could not evaluate $it", cause = e)
            }
        }
    }

    private val iterations = 1000

    data class CompareCase(val id: Int,
                           val description: String,
                           val comparator: Comparator<ExprValue>,
                           val unordered: List<ExprValue>,
                           val expected: List<List<ExprValue>>) {
        override fun toString() = "$description.$id"
    }

    fun parametersForShuffleAndSort(): List<CompareCase> {
        // TODO consider replacing linear congruential generator with something else (e.g. xorshift)
        // RNG for fuzz testing the sort orders, the seed is arbitrary but static for determinism
        val rng = Random(0x59CF3400BEF36A67)
        var id = 0

        fun <T> List<List<T>>.flatShuffle(): List<T> =
            flatten().map { it }.apply { Collections.shuffle(this, rng) }

        fun <T> List<List<T>>.moveHeadToTail(): List<List<T>> =
            drop(1).plusElement(this[0])

        fun shuffleCase(description: String,
                        comparator: Comparator<ExprValue>,
                        expectedSource: List<List<String>>): CompareCase {
            val expected = expectedSource.eval()
            val unordered = expected.flatShuffle()

            return CompareCase(
                id++,
                description,
                comparator,
                unordered,
                expected
            )
        }

        return (1..iterations).flatMap {
            listOf(
                shuffleCase("BASIC VALUES (NULLS FIRST)", NULLS_FIRST, basicExprs),
                shuffleCase("BASIC VALUES (NULLS LAST)", NULLS_LAST, basicExprs.moveHeadToTail())
            )
        }
    }

    @Test
    @Parameters
    @TestCaseName("{0}")
    fun shuffleAndSort(case: CompareCase) {
        val ordered = case.unordered.toMutableList().apply {
            Collections.sort(this, case.comparator)
        }
        val orderedIter = ordered.iterator()
        case.expected.map { it.toMutableList() }.forEach { equivs ->
            while (equivs.isNotEmpty()) {
                assertTrue("Not enough elements", orderedIter.hasNext())
                val actualNext = orderedIter.next()
                // use reference equality
                assertTrue(
                    "Could not find $actualNext in $equivs",
                    equivs.removeIf { it === actualNext }
                )
            }
        }
        assertFalse("Too many elements", orderedIter.hasNext())
    }
}
