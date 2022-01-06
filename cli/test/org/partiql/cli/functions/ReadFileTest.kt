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

import com.amazon.ion.system.IonSystemBuilder
import junit.framework.Assert.assertEquals
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.Environment
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValueFactory
import java.io.File

class ReadFileTest {
    private val ion = IonSystemBuilder.standard().build()
    private val valueFactory = ExprValueFactory.standard(ion)
    private val function = ReadFile(valueFactory)
    private val env = Environment(locals = Bindings.empty(),
                                  session = EvaluationSession.standard())

    private fun String.exprValue() = valueFactory.newFromIonValue(ion.singleValue(this))
    private fun writeFile(path: String, content: String) = File(dirPath(path)).writeText(content)

    companion object {
        fun dirPath(fname: String = "") = "tst-resources/$fname"

        @BeforeClass
        @JvmStatic
        fun setUp() {
            File(dirPath()).mkdir()
        }

        @AfterClass
        @JvmStatic
        fun tearDown() {
            File(dirPath()).deleteRecursively()
        }
    }

    @Test
    fun readIonAsDefault() {
        writeFile("data.ion", "1 2")

        val args = listOf("\"${dirPath("data.ion")}\"").map { it.exprValue() }
        val actual = function.call(env, args).ionValue
        val expected = "[1, 2]"

        assertEquals(ion.singleValue(expected), actual)
    }

    @Test
    fun readIon() {
        writeFile("data.ion", "1 2")

        val args = listOf("\"${dirPath("data.ion")}\"", "{type:\"ion\"}").map { it.exprValue() }
        val actual = function.call(env, args).ionValue
        val expected = "[1, 2]"

        assertEquals(ion.singleValue(expected), actual)
    }

    @Test
    fun readCsv() {
        writeFile("data.csv", "1,2")

        val args = listOf("\"${dirPath("data.csv")}\"", "{type:\"csv\"}").map { it.exprValue() }

        val actual = function.call(env, args).ionValue
        val expected = "[{_1:\"1\",_2:\"2\"}]"

        assertEquals(ion.singleValue(expected), actual)
    }

    @Test
    fun readCsvWithIonSymbolAsInput() {
        writeFile("data_with_ion_symbol_as_input.csv", "1,2")

        val args = listOf("\"${dirPath("data_with_ion_symbol_as_input.csv")}\"", "{type:csv}").map { it.exprValue() }

        val actual = function.call(env, args).ionValue
        val expected = "[{_1:\"1\",_2:\"2\"}]"

        assertEquals(ion.singleValue(expected), actual)
    }

    @Test
    fun readCsvWithDoubleQuotesEscape() {
        writeFile("data_with_double_quotes_escape.csv", "\"1,2\",2")

        val args = listOf("\"${dirPath("data_with_double_quotes_escape.csv")}\"", "{type:\"csv\"}").map { it.exprValue() }

        val actual = function.call(env, args).ionValue
        val expected = "[{_1:\"1,2\",_2:\"2\"}]"

        assertEquals(ion.singleValue(expected), actual)
    }

    @Test
    fun readCsvWithEmptyLines() {
        writeFile("data_with_double_quotes_escape.csv", "1,2\n\n3\n\n")

        val args = listOf("\"${dirPath("data_with_double_quotes_escape.csv")}\"", "{type:\"csv\"}").map { it.exprValue() }

        val actual = function.call(env, args).ionValue
        val expected = "[{_1:\"1\",_2:\"2\"},{_1:\"3\"}]"

        assertEquals(ion.singleValue(expected), actual)
    }

    @Test
    fun readCsvWithHeaderLine() {
        writeFile("data_with_header_line.csv", "col1,col2\n1,2")

        val args = listOf("\"${dirPath("data_with_header_line.csv")}\"", "{type:\"csv\", header:true}").map { it.exprValue() }

        val actual = function.call(env, args).ionValue
        val expected = "[{col1:\"1\",col2:\"2\"}]"

        assertEquals(ion.singleValue(expected), actual)
    }

    @Test
    fun readTsv() {
        writeFile("data.tsv", "1\t2")

        val args = listOf("\"${dirPath("data.tsv")}\"", "{type:\"tsv\"}").map { it.exprValue() }

        val actual = function.call(env, args).ionValue
        val expected = "[{_1:\"1\",_2:\"2\"}]"

        assertEquals(ion.singleValue(expected), actual)
    }

    @Test
    fun readTsvWithHeaderLine() {
        writeFile("data_with_header_line.tsv", "col1\tcol2\n1\t2")

        val args = listOf("\"${dirPath("data_with_header_line.tsv")}\"", "{type:\"tsv\", header:true}").map { it.exprValue() }

        val actual = function.call(env, args).ionValue
        val expected = "[{col1:\"1\",col2:\"2\"}]"

        assertEquals(ion.singleValue(expected), actual)
    }

    @Test
    fun readExcelCsvFile() {
        writeFile("simple_excel.csv", "title,category,price\nharry potter,book,7.99")

        val args = listOf("\"${dirPath("simple_excel.csv")}\"", "{type:\"excel_csv\", header:true}").map { it.exprValue() }

        val actual = function.call(env, args).ionValue
        val expected = "[{title:\"harry potter\",category:\"book\",price:\"7.99\"}]"

        assertEquals(ion.singleValue(expected), actual)
    }

    @Test
    fun readPostgreSQLCsvFile() {
        writeFile("simple_postgresql.csv", "id,name,balance\n1,Bob,10000.00")

        val args = listOf("\"${dirPath("simple_postgresql.csv")}\"", "{type:\"postgresql_csv\", header:true}").map { it.exprValue() }

        val actual = function.call(env, args).ionValue
        val expected = "[{id:\"1\",name:\"Bob\",balance:\"10000.00\"}]"

        assertEquals(ion.singleValue(expected), actual)
    }

    @Test
    fun readPostgreSQLTextFile() {
        writeFile("simple_postgresql.txt", "id\tname\tbalance\n1\tBob\t10000.00")

        val args = listOf("\"${dirPath("simple_postgresql.txt")}\"", "{type:\"postgresql_text\", header:true}").map { it.exprValue() }

        val actual = function.call(env, args).ionValue
        val expected = "[{id:\"1\",name:\"Bob\",balance:\"10000.00\"}]"

        assertEquals(ion.singleValue(expected), actual)
    }

    @Test
    fun readMySQLCsvFile() {
        writeFile("simple_mysql.csv", "id\tname\tbalance\n1\tBob\t10000.00")

        val args = listOf("\"${dirPath("simple_mysql.csv")}\"", "{type:\"mysql_csv\", header:true}").map { it.exprValue() }

        val actual = function.call(env, args).ionValue
        val expected = "[{id:\"1\",name:\"Bob\",balance:\"10000.00\"}]"

        assertEquals(ion.singleValue(expected), actual)
    }

    @Test
    fun readCustomizedCsvFile1() { // delimiter
        writeFile("customized.csv", "id name balance\n1 Bob 10000.00")

        val args = listOf("\"${dirPath("customized.csv")}\"", "{type:\"customized\", header:true, delimiter:' '}").map { it.exprValue() }

        val actual = function.call(env, args).ionValue
        val expected = "[{id:\"1\",name:\"Bob\",balance:\"10000.00\"}]"

        assertEquals(ion.singleValue(expected), actual)
    }

    @Test
    fun readCustomizedCsvFile2() { // ignore_empty_line
        writeFile("customized.csv", "id,name,balance\n\n1,Bob,10000.00")

        val args = listOf("\"${dirPath("customized.csv")}\"", "{type:\"customized\", header:true, ignore_empty_line: false}").map { it.exprValue() }

        val actual = function.call(env, args).ionValue
        val expected = "[{id:\"\"},{id:\"1\",name:\"Bob\",balance:\"10000.00\"}]"

        assertEquals(ion.singleValue(expected), actual)
    }

    @Test
    fun readCustomizedCsvFile3() { // trim and ignore_surrounding_space
        writeFile("customized.csv", "id,name,balance\n 1 , Bob , 10000.00 ")

        val args = listOf("\"${dirPath("customized.csv")}\"", "{type:\"customized\", header:true, ignore_surrounding_space:false, trim:false}").map { it.exprValue() }

        val actual = function.call(env, args).ionValue
        val expected = "[{id:\" 1 \",name:\" Bob \",balance:\" 10000.00 \"}]"

        assertEquals(ion.singleValue(expected), actual)
    }

    @Test
    fun readCustomizedCsvFile4() { // line_breaker
        writeFile("customized.csv", "id,name,balance\r\n1,Bob,10000.00")

        val args = listOf("\"${dirPath("customized.csv")}\"", "{type:\"customized\", header:true, line_breaker:'\\\r\\\n'}").map { it.exprValue() }

        val actual = function.call(env, args).ionValue
        val expected = "[{id:\"1\",name:\"Bob\",balance:\"10000.00\"}]"

        assertEquals(ion.singleValue(expected), actual)
    }

    @Test
    fun readCustomizedCsvFile5() { // escape
        writeFile("customized.csv", "id,name,balance\n\"/\"1\",Bob,10000.00")

        val args = listOf("\"${dirPath("customized.csv")}\"", "{type:\"customized\", header:true, escape:'/'}").map { it.exprValue() }

        val actual = function.call(env, args).ionValue
        val expected = "[{id:\"\\\"1\",name:\"Bob\",balance:\"10000.00\"}]"

        assertEquals(ion.singleValue(expected), actual)
    }

    @Test
    fun readCustomizedCsvFile6() { // quote
        writeFile("customized.csv", "id,name,balance\n'1,',Bob,10000.00")

        val args = listOf("\"${dirPath("customized.csv")}\"", "{type:\"customized\", header:true, quote:\"'\"}").map { it.exprValue() }

        val actual = function.call(env, args).ionValue
        val expected = "[{id:\"1,\",name:\"Bob\",balance:\"10000.00\"}]"

        assertEquals(ion.singleValue(expected), actual)
    }
}
