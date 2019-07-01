package org.partiql.testscript.evaluator

import com.amazon.ion.IonValue
import com.amazon.ion.system.IonSystemBuilder
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.testscript.extensions.crossProduct
import java.lang.IllegalArgumentException
import java.util.function.Supplier
import java.util.stream.Stream

internal class DefaultPtsEqualityTest {
    private val ion = IonSystemBuilder.standard().build()
    private val equality = PtsEquality.getDefault()

    private fun String.toIon(): IonValue = ion.singleValue(this)

    private fun assertTemplate(left: String, right: String, assertFunction: (Boolean, Supplier<String>) -> Unit) {
        val leftIon = left.toIon()
        val rightIon = right.toIon()

        // do both since equality is commutative  
        assertFunction(equality.areEqual(leftIon, rightIon), Supplier { "$left == $right" })
        assertFunction(equality.areEqual(rightIon, leftIon), Supplier { "$right == $left" })
    }

    private fun assertPtsEqual(left: String, right: String) = assertTemplate(left, right, ::assertTrue)

    private fun assertPtsNotEqual(left: String, right: String) = assertTemplate(left, right, ::assertFalse)

    @ParameterizedTest
    @MethodSource("equivalentValuesTestCases")
    fun equivalentValues(left: String, right: String) = assertPtsEqual(left, right)

    @ParameterizedTest
    @MethodSource("nonEquivalentValuesTestCases")
    fun nonEquivalentValues(left: String, right: String) = assertPtsNotEqual(left, right)

    companion object {
        // values are grouped by equivalent values
        private val values = listOf(
                // unknown
                listOf("missing::null"),
                listOf("null"),

                // boolean
                listOf("true"),
                listOf("false"),

                // int
                listOf("1"),
                listOf("2"),

                // float
                listOf("nan"),
                listOf("-inf", "-1e1000", "-1.000000000000e1000"),
                listOf("1e0", "1e00"),
                listOf("-5e-1", "-0.5e0"),
                listOf("+inf", "1e1000", "1.000000000000e1000"),

                // decimal
                listOf("1.0", "1.00"),
                listOf("-0.0", "-0.0000000000", "0d10000"),
                listOf("-1d1000", "-1.000000000000d1000"),

                // timestamp
                listOf("2019T"),
                listOf(
                        "2017T",
                        "2017-01T",
                        "2017-01-01T",
                        "2017-01-01T00:00-00:00",
                        "2017-01-01T01:00+01:00"
                ),

                // symbol
                listOf("aSymbol"),

                // string
                listOf("\"a string\""),

                // clob
                listOf("{{ \"This is a CLOB of text.\" }}"),

                // blob
                listOf("{{ dHdvIHBhZGRpbmcgY2hhcmFjdGVycw== }}"),

                // list
                listOf("[]"),
                listOf("[[]]"),
                listOf("[[1]]"),
                listOf("[1, 2, 3]"),
                listOf("[1, 2, 4]"),
                listOf("[1, 2, [10,11,[12,[]]]]"),

                // S-exp
                listOf("()"),
                listOf("(1 2 3)"),
                listOf("(1 2 4)"),
                listOf("(3 2 1)"),
                listOf("(1 2 (3))"),
                listOf("(1 2 (3 4 5 (6)) () () (()))"),

                // bag
                listOf("(bag)"),
                listOf("(bag 1 2 3)", "(bag 3 2 1)"),
                listOf("(bag 1 1 2)"),
                listOf("(bag 1 2 2)"),
                listOf("(bag 1 2 (3))"),
                listOf("(bag 1 2 (3 4 5 (6)) () () (()))"),
                listOf("(bag 1 2 (3 4 5 (6)) () () ((null) missing::null))"),

                // struct 
                listOf("{}"),
                listOf("{foo: 1, bar: {}}"),
                listOf("{foo: 1, bar: 2}", "{bar: 2, foo: 1}"),
                listOf("{foo: [1,2, (a {bar: baz})], bar: {}}")
        )

        @JvmStatic
        fun equivalentValuesTestCases(): Stream<Array<String>> = values.flatMap { it.combinations2() }.stream()

        // combine each element from a equivalent group with the elements from the other groups
        @JvmStatic
        fun nonEquivalentValuesTestCases(): Stream<Array<String>> =
                values.foldIndexed(mutableListOf<Array<String>>()) { index, pairs, list ->
                    val nonEquivalent = values.toMutableList().apply { removeAt(index) }.flatten()

                    list.crossProduct(nonEquivalent).mapTo(pairs) { arrayOf(it.first, it.second) }

                    pairs
                }.stream()
    }
}

// all combinations of size 2 including each element with itself
private fun List<String>.combinations2(): List<Array<String>> =
        when (this.size) {
            0 -> throw IllegalArgumentException()
            1 -> listOf(arrayOf(this[0], this[0]))
            else -> this.foldIndexed(mutableListOf()) { index, pairs, el ->
                this.subList(index, this.size).forEach { subEl -> pairs.add(arrayOf(el, subEl)) }

                pairs
            }
        }
