package org.partiql.testscript.evaluator

import com.amazon.ion.IonValue
import com.amazon.ion.system.IonSystemBuilder
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import java.util.function.Supplier
import java.util.stream.Stream

internal class DefaultPtsEqualityTest {
    private val ion = IonSystemBuilder.standard().build()
    private val equality = PtsEquality.getDefault()

    private fun String.toIon(): IonValue = ion.singleValue(this)

    private fun assertTemplate(left: String, right: String, assertFunction: (Boolean, Supplier<String>) -> Unit) {
        val messageSupplier = Supplier {
            """|
               |Expected: $left
               |  Actual: $right
            """.trimMargin()
        }
        
        assertFunction(equality.isEqual(left.toIon(), right.toIon()), messageSupplier) 
    }

    private fun assertPtsEqual(left: String, right: String) = assertTemplate(left, right, ::assertTrue)

    private fun assertPtsNotEqual(left: String, right: String) = assertTemplate(left, right, ::assertFalse)


    @ParameterizedTest
    @MethodSource("sameValueEqualsTestCases")
    fun sameValueEquals(value: String) = assertPtsEqual(value, value)

    @ParameterizedTest
    @MethodSource("equivalentValuesTestCases")
    fun equivalentValues(left: String, right: String) = assertPtsEqual(left, right)

    @ParameterizedTest
    @MethodSource("nonEquivalentValuesTestCases")
    fun nonEquivalentValues(left: String, right: String) = assertPtsNotEqual(left, right)
    
    companion object {
        @JvmStatic 
        fun equivalentValuesTestCases(): Stream<Array<String>> = Stream.of(
                arrayOf("(bag 1 2 3)", "(bag 3 2 1)"),
                arrayOf("1.0", "1.00"),
                arrayOf("1e0", "1e00"),
                arrayOf("{foo: 1, bar: 2}", "{bar: 2, foo: 1}"),
                arrayOf("2010T", "2010-01T")
        )

        @JvmStatic
        fun nonEquivalentValuesTestCases(): Stream<Array<String>> = Stream.of(
                arrayOf("(1 2 3)", "(3 2 1)"),
                arrayOf("[1, 2, 3]", "[3, 2, 1]"),
                arrayOf("[1, 2, 3]", "(1 2 3)"),
                arrayOf("{}", "[]"),
                arrayOf("{}", "()"),
                arrayOf("[]", "()"),
                arrayOf("1", "1.0"),
                arrayOf("1", "1e0"),
                arrayOf("1.0", "1e0"),
                arrayOf("missing::null", "null")
        )

        @JvmStatic
        fun sameValueEqualsTestCases(): Stream<String> = Stream.of(
                "missing::null",
                "null",
                "true",
                "false",
                "1",
                "1e0",
                "1.0",
                "2019T",
                "aSymbol",
                "\"a string\"",
                "{{ \"This is a CLOB of text.\" }}", 
                "{{ dHdvIHBhZGRpbmcgY2hhcmFjdGVycw== }}", 
                "[]",
                "[1, 2, 3]",
                "[1, 2, [10,11,[12,[]]]]",
                "()",
                "(1 2 3)",
                "(1 2 (3))",
                "(1 2 (3 4 5 (6)) () () (()))",
                "(bag)",
                "(bag 1 2 3)",
                "(bag 1 2 (3))",
                "(bag 1 2 (3 4 5 (6)) () () (()))",
                "{}",
                "{foo: 1, bar: 2}",
                "{foo: 1, bar: {}}",
                "{foo: [1,2, (a {bar: baz})], bar: {}}"
        )
    }
}