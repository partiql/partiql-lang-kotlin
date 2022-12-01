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
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.lang.eval.BAG_ANNOTATION
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.MISSING_ANNOTATION
import org.partiql.lang.eval.toIonValue
import org.partiql.lang.util.asSequence
import java.io.File

class ReadFileTest {
    private val ion = IonSystemBuilder.standard().build()
    private val valueFactory = ExprValueFactory.standard(ion)
    private val function = ReadFile(valueFactory)
    private val session = EvaluationSession.standard()

    private fun String.exprValue() = valueFactory.newFromIonValue(ion.singleValue(this))
    private fun writeFile(path: String, content: String) = File(dirPath(path)).writeText(content)

    companion object {
        fun dirPath(fname: String = "") = "tst-resources/$fname"

        @BeforeAll
        @JvmStatic
        fun setUp() {
            File(dirPath()).mkdir()
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            File(dirPath()).deleteRecursively()
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
    fun readIonAsDefault() {
        writeFile("data.ion", "[1, 2]")

        val args = listOf("\"${dirPath("data.ion")}\"").map { it.exprValue() }
        val actual = function.callWithRequired(session, args)
        val expected = "[1, 2]"

        assertValues(expected, actual)
    }

    @Test
    fun readIon() {
        writeFile("data.ion", "[1, 2]")

        val args = listOf("\"${dirPath("data.ion")}\"").map { it.exprValue() }
        val additionalOptions = "{type:\"ion\"}".exprValue()
        val actual = function.callWithOptional(session, args, additionalOptions)
        val expected = "[1, 2]"

        assertValues(expected, actual)
    }

    @Test
    fun readBadIon() {
        writeFile("data.ion", "1 2")

        val args = listOf("\"${dirPath("data.ion")}\"").map { it.exprValue() }
        val additionalOptions = "{type:\"ion\"}".exprValue()
        assertThrows<IllegalStateException> {
            function.callWithOptional(session, args, additionalOptions)
        }
    }

    @Test
    fun readCsv() {
        writeFile("data.csv", "1,2")

        val args = listOf("\"${dirPath("data.csv")}\"").map { it.exprValue() }
        val additionalOptions = "{type:\"csv\"}".exprValue()
        val actual = function.callWithOptional(session, args, additionalOptions)
        val expected = "[{_1:\"1\",_2:\"2\"}]"

        assertValues(expected, actual)
    }

    @Test
    fun readCsvWithIonSymbolAsInput() {
        writeFile("data_with_ion_symbol_as_input.csv", "1,2")

        val args = listOf("\"${dirPath("data_with_ion_symbol_as_input.csv")}\"").map { it.exprValue() }
        val additionalOptions = "{type:csv}".exprValue()
        val actual = function.callWithOptional(session, args, additionalOptions)
        val expected = "[{_1:\"1\",_2:\"2\"}]"

        assertValues(expected, actual)
    }

    @Test
    fun readCsvWithDoubleQuotesEscape() {
        writeFile("data_with_double_quotes_escape.csv", "\"1,2\",2")

        val args = listOf("\"${dirPath("data_with_double_quotes_escape.csv")}\"").map { it.exprValue() }
        val additionalOptions = "{type:\"csv\"}".exprValue()
        val actual = function.callWithOptional(session, args, additionalOptions)
        val expected = "[{_1:\"1,2\",_2:\"2\"}]"

        assertValues(expected, actual)
    }

    @Test
    fun readCsvWithEmptyLines() {
        writeFile("data_with_double_quotes_escape.csv", "1,2\n\n3\n\n")

        val args = listOf("\"${dirPath("data_with_double_quotes_escape.csv")}\"").map { it.exprValue() }
        val additionalOptions = "{type:\"csv\"}".exprValue()
        val actual = function.callWithOptional(session, args, additionalOptions)
        val expected = "[{_1:\"1\",_2:\"2\"},{_1:\"3\"}]"

        assertValues(expected, actual)
    }

    @Test
    fun readCsvWithHeaderLine() {
        writeFile("data_with_header_line.csv", "col1,col2\n1,2")

        val args = listOf("\"${dirPath("data_with_header_line.csv")}\"").map { it.exprValue() }
        val additionalOptions = "{type:\"csv\", header:true}".exprValue()
        val actual = function.callWithOptional(session, args, additionalOptions)
        val expected = "[{col1:\"1\",col2:\"2\"}]"

        assertValues(expected, actual)
    }

    @Test
    fun readTsv() {
        writeFile("data.tsv", "1\t2")

        val args = listOf("\"${dirPath("data.tsv")}\"").map { it.exprValue() }
        val additionalOptions = "{type:\"tsv\"}".exprValue()
        val actual = function.callWithOptional(session, args, additionalOptions)
        val expected = "[{_1:\"1\",_2:\"2\"}]"

        assertValues(expected, actual)
    }

    @Test
    fun readTsvWithHeaderLine() {
        writeFile("data_with_header_line.tsv", "col1\tcol2\n1\t2")

        val args = listOf("\"${dirPath("data_with_header_line.tsv")}\"").map { it.exprValue() }
        val additionalOptions = "{type:\"tsv\", header:true}".exprValue()
        val actual = function.callWithOptional(session, args, additionalOptions)
        val expected = "[{col1:\"1\",col2:\"2\"}]"

        assertValues(expected, actual)
    }

    @Test
    fun readExcelCsvFile() {
        writeFile("simple_excel.csv", "title,category,price\nharry potter,book,7.99")

        val args = listOf("\"${dirPath("simple_excel.csv")}\"").map { it.exprValue() }
        val additionalOptions = "{type:\"excel_csv\", header:true}".exprValue()
        val actual = function.callWithOptional(session, args, additionalOptions)
        val expected = "[{title:\"harry potter\",category:\"book\",price:\"7.99\"}]"

        assertValues(expected, actual)
    }

    @Test
    fun readPostgreSQLCsvFile() {
        writeFile("simple_postgresql.csv", "id,name,balance\n1,B\"\"ob,10000.00")

        val args = listOf("\"${dirPath("simple_postgresql.csv")}\"").map { it.exprValue() }
        val additionalOptions = "{type:\"postgresql_csv\", header:true}".exprValue()
        val actual = function.callWithOptional(session, args, additionalOptions)
        val expected = "[{id:\"1\",name:\"B\\\"ob\",balance:\"10000.00\"}]"

        assertValues(expected, actual)
    }

    @Test
    fun readPostgreSQLTextFile() {
        writeFile("simple_postgresql.txt", "id\tname\tbalance\n1\tBob\t10000.00")

        val args = listOf("\"${dirPath("simple_postgresql.txt")}\"").map { it.exprValue() }
        val additionalOptions = "{type:\"postgresql_text\", header:true}".exprValue()
        val actual = function.callWithOptional(session, args, additionalOptions)
        val expected = "[{id:\"1\",name:\"Bob\",balance:\"10000.00\"}]"

        assertValues(expected, actual)
    }

    @Test
    fun readMySQLCsvFile() {
        writeFile("simple_mysql.csv", "id\tname\tbalance\n1\tB\"ob\t10000.00")

        val args = listOf("\"${dirPath("simple_mysql.csv")}\"").map { it.exprValue() }
        val additionalOptions = "{type:\"mysql_csv\", header:true}".exprValue()
        val actual = function.callWithOptional(session, args, additionalOptions)
        val expected = "[{id:\"1\",name:\"B\\\"ob\",balance:\"10000.00\"}]"

        assertValues(expected, actual)
    }

    @Test
    fun readCustomizedCsvFile1() { // delimiter
        writeFile("customized.csv", "id name balance\n1 Bob 10000.00")

        val args = listOf("\"${dirPath("customized.csv")}\"").map { it.exprValue() }
        val additionalOptions = "{type:\"customized\", header:true, delimiter:' '}".exprValue()
        val actual = function.callWithOptional(session, args, additionalOptions)
        val expected = "[{id:\"1\",name:\"Bob\",balance:\"10000.00\"}]"

        assertValues(expected, actual)
    }

    @Test
    fun readCustomizedCsvFile2() { // ignore_empty_line
        writeFile("customized.csv", "id,name,balance\n\n1,Bob,10000.00")

        val args = listOf("\"${dirPath("customized.csv")}\"").map { it.exprValue() }
        val additionalOptions = "{type:\"customized\", header:true, ignore_empty_line: false}".exprValue()
        val actual = function.callWithOptional(session, args, additionalOptions)
        val expected = "[{id:\"\"},{id:\"1\",name:\"Bob\",balance:\"10000.00\"}]"

        assertValues(expected, actual)
    }

    @Test
    fun readCustomizedCsvFile3() { // trim and ignore_surrounding_space
        writeFile("customized.csv", "id,name,balance\n 1 , Bob , 10000.00 ")

        val args = listOf("\"${dirPath("customized.csv")}\"").map { it.exprValue() }
        val additionalOptions = "{type:\"customized\", header:true, ignore_surrounding_space:false, trim:false}".exprValue()
        val actual = function.callWithOptional(session, args, additionalOptions)
        val expected = "[{id:\" 1 \",name:\" Bob \",balance:\" 10000.00 \"}]"

        assertValues(expected, actual)
    }

    @Test
    fun readCustomizedCsvFile4() { // line_breaker
        writeFile("customized.csv", "id,name,balance\r\n1,Bob,10000.00")

        val args = listOf("\"${dirPath("customized.csv")}\"").map { it.exprValue() }
        val additionalOptions = "{type:\"customized\", header:true, line_breaker:'\\\r\\\n'}".exprValue()
        val actual = function.callWithOptional(session, args, additionalOptions)
        val expected = "[{id:\"1\",name:\"Bob\",balance:\"10000.00\"}]"

        assertValues(expected, actual)
    }

    @Test
    fun readCustomizedCsvFile5() { // escape
        writeFile("customized.csv", "id,name,balance\n\"/\"1\",Bob,10000.00")

        val args = listOf("\"${dirPath("customized.csv")}\"").map { it.exprValue() }
        val additionalOptions = "{type:\"customized\", header:true, escape:'/'}".exprValue()
        val actual = function.callWithOptional(session, args, additionalOptions)
        val expected = "[{id:\"\\\"1\",name:\"Bob\",balance:\"10000.00\"}]"

        assertValues(expected, actual)
    }

    @Test
    fun readCustomizedCsvFile6() { // quote
        writeFile("customized.csv", "id,name,balance\n'1,',Bob,10000.00")

        val args = listOf("\"${dirPath("customized.csv")}\"").map { it.exprValue() }
        val additionalOptions = "{type:\"customized\", header:true, quote:\"'\"}".exprValue()
        val actual = function.callWithOptional(session, args, additionalOptions)
        val expected = "[{id:\"1,\",name:\"Bob\",balance:\"10000.00\"}]"

        assertValues(expected, actual)
    }
}
