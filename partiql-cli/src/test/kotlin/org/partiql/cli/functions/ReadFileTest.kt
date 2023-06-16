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

package org.partiql.cli.functions

import com.amazon.ion.IonType
import com.amazon.ion.IonValue
import com.amazon.ion.system.IonSystemBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.BAG_ANNOTATION
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.MISSING_ANNOTATION
import org.partiql.lang.eval.toIonValue
import org.partiql.lang.util.asSequence
import java.util.stream.Stream

class ReadFileTest {
    private val ion = IonSystemBuilder.standard().build()
    private val function = ReadFile(ion)
    private val function2 = ReadFile2(ion)
    private val session = EvaluationSession.standard()

    private fun String.exprValue() = ExprValue.of(ion.singleValue(this))

    companion object {
        private fun getResourcePath(name: String): String {
            val url = ReadFileTest::class.java.classLoader.getResource("read_file_tests/$name")
            return url!!.path
        }
    }

    private fun IonValue.removeAnnotations() {
        when (this.type) {
            // Remove $missing annotation from NULL for assertions
            IonType.NULL -> this.removeTypeAnnotation(MISSING_ANNOTATION)
            IonType.DATAGRAM,
            IonType.SEXP,
            IonType.STRUCT,
            IonType.LIST -> {
                // Remove $bag annotation from LIST for assertions
                if (this.type == IonType.LIST) {
                    this.removeTypeAnnotation(BAG_ANNOTATION)
                }
                // Recursively remove annotations
                this.asSequence().forEach {
                    it.removeAnnotations()
                }
            }
            else -> { /* ok to do nothing. */ }
        }
    }

    private fun IonValue.cloneAndRemoveAnnotations() = this.clone().apply {
        removeAnnotations()
        makeReadOnly()
    }

    private fun assertValues(expectedIon: String, value: ExprValue) {
        val expectedValues = ion.singleValue(expectedIon)

        assertEquals(expectedValues, value.toIonValue(ion).cloneAndRemoveAnnotations())
    }

    @Test
    fun testError() {
        val path = getResourcePath("data.ion")
        val args = listOf("\"$path\"").map { it.exprValue() }
        assertThrows<IllegalStateException> {
            function2.callWithRequired(session, args + listOf("{type:\"ion\"}".exprValue()))
        }
    }

    @ParameterizedTest
    @ArgumentsSource(SuccessTestProvider::class)
    fun test(tc: SuccessTestProvider.TestCase) {
        val path = getResourcePath(tc.filename)
        val args = listOf("\"$path\"").map { it.exprValue() }
        val actual = when (tc.additionalOptions) {
            null -> function.callWithRequired(session, args)
            else -> function2.callWithRequired(session, args + listOf(tc.additionalOptions.exprValue()))
        }
        assertValues(tc.expected, actual)
    }

    class SuccessTestProvider : ArgumentsProvider {
        data class TestCase(
            val filename: String,
            val expected: String,
            val additionalOptions: String? = null
        )

        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> {
            return tests.map { Arguments.of(it) }.stream()
        }

        private val tests = listOf(
            TestCase(
                filename = "data_list.ion",
                expected = "[1, 2]"
            ),
            TestCase(
                filename = "data_list.ion",
                expected = "[1, 2]",
                additionalOptions = "{type:\"ion\"}"
            ),
            TestCase(
                filename = "data.csv",
                expected = "[{_1:\"1\",_2:\"2\"}]",
                additionalOptions = "{type:\"csv\"}"
            ),
            TestCase(
                filename = "data_with_ion_symbol_as_input.csv",
                expected = "[{_1:\"1\",_2:\"2\"}]",
                additionalOptions = "{type:csv}"
            ),
            TestCase(
                filename = "data_with_double_quotes_escape.csv",
                expected = "[{_1:\"1,2\",_2:\"2\"}]",
                additionalOptions = "{type:\"csv\"}"
            ),
            TestCase(
                filename = "csv_with_empty_lines.csv",
                expected = "[{_1:\"1\",_2:\"2\"},{_1:\"3\"}]",
                additionalOptions = "{type:\"csv\"}"
            ),
            TestCase(
                filename = "data_with_header_line.csv",
                expected = "[{col1:\"1\",col2:\"2\"}]",
                additionalOptions = "{type:\"csv\", header:true}"
            ),
            TestCase(
                filename = "data.tsv",
                expected = "[{_1:\"1\",_2:\"2\"}]",
                additionalOptions = "{type:\"tsv\"}"
            ),
            TestCase(
                filename = "data_with_header_line.tsv",
                expected = "[{col1:\"1\",col2:\"2\"}]",
                additionalOptions = "{type:\"tsv\", header:true}"
            ),
            TestCase(
                filename = "simple_excel.csv",
                expected = "[{title:\"harry potter\",category:\"book\",price:\"7.99\"}]",
                additionalOptions = "{type:\"excel_csv\", header:true}"
            ),
            TestCase(
                filename = "simple_postgresql.csv",
                expected = "[{id:\"1\",name:\"B\\\"ob\",balance:\"10000.00\"}]",
                additionalOptions = "{type:\"postgresql_csv\", header:true}"
            ),
            TestCase(
                filename = "simple_postgresql.txt",
                expected = "[{id:\"1\",name:\"Bob\",balance:\"10000.00\"}]",
                additionalOptions = "{type:\"postgresql_text\", header:true}"
            ),
            TestCase(
                filename = "simple_mysql.csv",
                expected = "[{id:\"1\",name:\"B\\\"ob\",balance:\"10000.00\"}]",
                additionalOptions = "{type:\"mysql_csv\", header:true}"
            ),
            TestCase(
                filename = "customized.csv",
                expected = "[{id:\"1\",name:\"Bob\",balance:\"10000.00\"}]",
                additionalOptions = "{type:\"customized\", header:true, delimiter:' '}"
            ),
            TestCase(
                filename = "customized_ignore_empty.csv",
                expected = "[{id:\"\"},{id:\"1\",name:\"Bob\",balance:\"10000.00\"}]",
                additionalOptions = "{type:\"customized\", header:true, ignore_empty_line: false}"
            ),
            TestCase(
                filename = "customized_ignore_surrounding.csv",
                expected = "[{id:\" 1 \",name:\" Bob \",balance:\" 10000.00 \"}]",
                additionalOptions = "{type:\"customized\", header:true, ignore_surrounding_space:false, trim:false}"
            ),
            TestCase(
                filename = "customized_line_breaker.csv",
                expected = "[{id:\"1\",name:\"Bob\",balance:\"10000.00\"}]",
                additionalOptions = "{type:\"customized\", header:true, line_breaker:'\\\r\\\n'}"
            ),
            TestCase(
                filename = "customized_escape.csv",
                expected = "[{id:\"\\\"1\",name:\"Bob\",balance:\"10000.00\"}]",
                additionalOptions = "{type:\"customized\", header:true, escape:'/'}"
            ),
            TestCase(
                filename = "customized_quote.csv",
                expected = "[{id:\"1,\",name:\"Bob\",balance:\"10000.00\"}]",
                additionalOptions = "{type:\"customized\", header:true, quote:\"'\"}"
            )
        )
    }
}
