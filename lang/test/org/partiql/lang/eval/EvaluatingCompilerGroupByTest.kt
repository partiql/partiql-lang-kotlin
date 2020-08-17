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

package org.partiql.lang.eval

import org.partiql.lang.errors.*
import org.partiql.lang.util.*
import junitparams.*
import org.junit.*

class
EvaluatingCompilerGroupByTest : EvaluatorTestBase() {

     private val session = mapOf(
        "simple_1_col_1_group" to "[{col1: 1}, {col1: 1}]",
        "simple_1_col_2_groups" to "[{col1: 1}, {col1: 2}, {col1: 1}, {col1: 2}]",
        "simple_2_col_1_group" to "[{col1: 1, col2: 10}, {col1: 1, col2: 10}]",
        "simple_2_col_2_groups" to "[{col1: 1, col2: 10}, {col1: 11, col2: 110}, {col1: 1, col2: 10}, {col1: 11, col2: 110}]",
        "string_groups" to "[{ col1: 'a'}, { col1: 'a' }]",
        "string_numbers" to "[{ num: '1'}, { num: '2' }]",
        "all_nulls" to "[{ 'col1': null, 'col2': null }]",
        "join_me" to "[{ 'foo': 20 }, { 'foo': 30 }]",
        "different_types_per_row" to "[{ 'a': 1001 }, 1002.0, 'one-thousand and three']",
        "suppliers" to """[
            { supplierId: 10, supplierName: "Umbrella" },
            { supplierId: 11, supplierName: "Initech" }
        ]""",
        "products" to """[
            { productId: 1, supplierId: 10, categoryId: 20, price: 5.0,  numInStock: 1 },
            { productId: 2, supplierId: 10, categoryId: 20, price: 10.0, numInStock: 10 },
            { productId: 3, supplierId: 10, categoryId: 21, price: 15.0, numInStock: 100 },
            { productId: 4, supplierId: 11, categoryId: 21, price: 5.0,  numInStock: 1000 },
            { productId: 5, supplierId: 11, categoryId: 21, price: 15.0, numInStock: 10000 }
        ]""",
        "products_sparse" to """[
            { productId: 1,  categoryId: 20, regionId: 100, supplierId_nulls: 10,   supplierId_missings: 10, supplierId_mixed: 10,   price_nulls: 1.0,  price_missings: 1.0, price_mixed: 1.0  },
            { productId: 2,  categoryId: 20, regionId: 100, supplierId_nulls: 10,   supplierId_missings: 10, supplierId_mixed: 10,   price_nulls: 2.0,  price_missings: 2.0, price_mixed: 2.0  },
            { productId: 3,  categoryId: 20, regionId: 200, supplierId_nulls: 10,   supplierId_missings: 10, supplierId_mixed: 10,   price_nulls: 3.0,  price_missings: 3.0, price_mixed: 3.0  },
            { productId: 5,  categoryId: 21, regionId: 100, supplierId_nulls: null,                                                  price_nulls: null                                         },
            { productId: 4,  categoryId: 20, regionId: 100, supplierId_nulls: null,                          supplierId_mixed: null, price_nulls: null,                      price_mixed: null },
            { productId: 6,  categoryId: 21, regionId: 100, supplierId_nulls: 11,   supplierId_missings: 11, supplierId_mixed: 11,   price_nulls: 4.0,  price_missings: 4.0, price_mixed: 4.0  },
            { productId: 7,  categoryId: 21, regionId: 200, supplierId_nulls: 11,   supplierId_missings: 11, supplierId_mixed: 11,   price_nulls: 5.0,  price_missings: 5.0, price_mixed: 5.0  },
            { productId: 8,  categoryId: 21, regionId: 200, supplierId_nulls: null,                          supplierId_mixed: null, price_nulls: null,                      price_mixed: null },
            { productId: 9,  categoryId: 21, regionId: 200, supplierId_nulls: null,                                                  price_nulls: null,                                        },
            { productId: 10, categoryId: 21, regionId: 200, supplierId_nulls: null,                          supplierId_mixed: null, price_nulls: null,                                        }
        ]""",
        "widgets_a" to """[
            { categoryId: 1, name: "Doodad" },
        ]""",
        "widgets_b" to """[
            { categoryId: 2, name: "Thingy" }
        ]""",
        "customers" to """[
            { customerId: 123, firstName: "John", lastName: "Smith", age: 23},
            { customerId: 456, firstName: "Rob", lastName: "Jones", age: 45},
            { customerId: 789, firstName: "Emma", lastName: "Miller", age: 67}
        ]""",
        "orders" to """[
            { customerId: 123, sellerId: 1, productId: 11111, cost: 1 },
            { customerId: 123, sellerId: 2, productId: 22222, cost: 2 },
            { customerId: 123, sellerId: 1, productId: 33333, cost: 3 },
            { customerId: 456, sellerId: 2, productId: 44444, cost: 4 },
            { customerId: 456, sellerId: 1, productId: 55555, cost: 5 },
            { customerId: 456, sellerId: 2, productId: 66666, cost: 6 },
            { customerId: 789, sellerId: 1, productId: 77777, cost: 7 },
            { customerId: 789, sellerId: 2, productId: 88888, cost: 8 },
            { customerId: 789, sellerId: 1, productId: 99999, cost: 9 },
            { customerId: 100, sellerId: 2, productId: 10000, cost: 10 }
        ]""").toSession()

    companion object {

        private class SqlTemplate(
            val sql: String,
            val compilationOptions: List<CompOptions> = CompOptions.values().toList())

        /**
         * Creates one [EvaluatorTestCase] for each of the specified `expectedResultFor*` arguments and
         * [SqlTemplate.compilationOptions].
         */
        private fun createAggregateTestCasesFromSqlTemplates(
            groupName: String,
            sqlTemplates: List<SqlTemplate>,
            expectedResultForCount: String? = null,
            expectedResultForSum: String? = null,
            expectedResultForMin: String? = null,
            expectedResultForMax: String? = null,
            expectedResultForAvg: String? = null
        ): List<EvaluatorTestCase> {
            val cases = ArrayList<EvaluatorTestCase>()

            sqlTemplates.forEach { sqlTemplate ->
                sqlTemplate.compilationOptions.forEach { compOptions ->
                    fun applySqlTemplate(aggFuncName: String) = sqlTemplate.sql.replace("{{agg}}", aggFuncName)
                    val coGroupName = "$compOptions|$groupName"
                    expectedResultForCount?.let { cases.add(EvaluatorTestCase(coGroupName, applySqlTemplate("COUNT"), it, compOptions)) }
                    expectedResultForSum?.let { cases.add(EvaluatorTestCase(coGroupName, applySqlTemplate("SUM"), it, compOptions)) }
                    expectedResultForMin?.let { cases.add(EvaluatorTestCase(coGroupName, applySqlTemplate("MIN"), it, compOptions)) }
                    expectedResultForMax?.let { cases.add(EvaluatorTestCase(coGroupName, applySqlTemplate("MAX"), it, compOptions)) }
                    expectedResultForAvg?.let { cases.add(EvaluatorTestCase(coGroupName, applySqlTemplate("AVG"), it, compOptions)) }
                }
            }

            if(cases.size == 0) {
                fail("At least one expected result must be specified.")
            }
            return cases
        }

        private fun createAggregateTestCasesFromSqlStrings(
            groupName: String,
            sqlStrings: List<String>,
            expectedResultForCount: String? = null,
            expectedResultForSum: String? = null,
            expectedResultForMin: String? = null,
            expectedResultForMax: String? = null,
            expectedResultForAvg: String? = null
        ): List<EvaluatorTestCase> =
            createAggregateTestCasesFromSqlTemplates(
                groupName,
                sqlStrings.map { SqlTemplate(it) },
                expectedResultForCount,
                expectedResultForSum,
                expectedResultForMin,
                expectedResultForMax,
                expectedResultForAvg)

        private fun createGroupByTestCases(
            query: String,
            expected: String,
            compilationOptions: List<CompOptions> = CompOptions.values().toList()
        ) = compilationOptions.map { co -> EvaluatorTestCase(query, expected, co)}

        private fun createGroupByTestCases(queries: List<String>, expected: String) =
            queries.flatMap { q ->
                CompOptions.values().map { co -> EvaluatorTestCase(q, expected, co) }
            }
    }

    @Test
    @Parameters
    fun groupByTest(tc: EvaluatorTestCase) = runTestCase(tc, session)

    /** Test cases for GROUP BY without aggregates. */
    fun parametersForGroupByTest() =
        // GROUP by variable

        // GROUP BY over empty
        createGroupByTestCases(
            queries = listOf("SELECT * FROM [] GROUP BY doesntMatterWontBeEvaluated",
                             "SELECT VALUE { } FROM [] GROUP BY doesntMatterWontBeEvaluated "),
            expected = "<< >>") +

        createGroupByTestCases(
            query = "SELECT * FROM simple_1_col_1_group GROUP BY col1",
            expected = "<<{'col1': 1 }>>") +

        createGroupByTestCases(
            query = "SELECT * FROM simple_2_col_1_group GROUP BY col1",
            expected = "<<{'col1': 1 }>>") +
        createGroupByTestCases(
            query = "SELECT * FROM simple_2_col_1_group GROUP BY col2",
            expected = "<<{'col2': 10 }>>") +

        createGroupByTestCases(
            queries = listOf("SELECT col1                FROM simple_1_col_1_group GROUP BY col1",
                             "SELECT VALUE { 'col1': 1 } FROM simple_1_col_1_group GROUP BY col1"),
            expected = "<<{'col1': 1 }>>") +
        createGroupByTestCases(
            queries = listOf("SELECT col1                FROM simple_2_col_1_group GROUP BY col1",
                             "SELECT VALUE { 'col1': 1 } FROM simple_2_col_1_group GROUP BY col1"),
            expected = "<<{'col1': 1 }>>") +
        createGroupByTestCases(
            queries = listOf("SELECT col2                   FROM simple_2_col_1_group GROUP BY col2",
                             "SELECT VALUE { 'col2': col2 } FROM simple_2_col_1_group GROUP BY col2"),
            expected = "<<{'col2': 10 }>>") +

        createGroupByTestCases(
            query = "SELECT * FROM simple_1_col_2_groups GROUP BY col1",
            expected = "<<{'col1': 1 }, {'col1': 2 }>>") +
        createGroupByTestCases(
            query = "SELECT * FROM simple_2_col_2_groups GROUP BY col1",
            expected = "<<{'col1': 1 }, {'col1': 11 }>>") +
        createGroupByTestCases(
            query = "SELECT * FROM simple_2_col_2_groups GROUP BY col2",
            expected = "<<{'col2': 10 }, {'col2': 110 }>>") +

        createGroupByTestCases(
            queries = listOf("SELECT col1                    FROM simple_1_col_2_groups GROUP BY col1",
                             "SELECT VALUE { 'col1': col1 } FROM simple_1_col_2_groups GROUP BY col1"),
            expected = "<<{'col1': 1 }, {'col1': 2}>>") +
        createGroupByTestCases(
            queries = listOf("SELECT col1                   FROM simple_2_col_2_groups GROUP BY col1",
                             "SELECT VALUE { 'col1': col1 } FROM simple_2_col_2_groups GROUP BY col1"),
            expected = "<<{'col1': 1 }, {'col1': 11}>>") +
        createGroupByTestCases(
            queries = listOf("SELECT col2                   FROM simple_2_col_2_groups GROUP BY col2",
                             "SELECT VALUE { 'col2': col2 } FROM simple_2_col_2_groups GROUP BY col2"),
            expected = "<<{'col2': 10 }, { 'col2': 110}>>") +

        // GROUP BY other expressions
        createGroupByTestCases(
            queries = listOf("SELECT *                  FROM simple_1_col_1_group GROUP BY col1 + 1",
                             "SELECT _1                 FROM simple_1_col_1_group GROUP BY col1 + 1",
                             "SELECT VALUE { '_1': _1 } FROM simple_1_col_1_group GROUP BY col1 + 1"),
            expected = "<< { '_1': 2 } >>") +
        createGroupByTestCases(
            queries = listOf("SELECT *                  FROM string_groups GROUP BY col1 || 'a'",
                             "SELECT _1                 FROM string_groups GROUP BY col1 || 'a'",
                             "SELECT VALUE { '_1': _1 } FROM string_groups GROUP BY col1 || 'a'"),
            expected = "<< { '_1': 'aa' } >>") +
        createGroupByTestCases(
            queries = listOf("SELECT *                  FROM string_numbers GROUP BY CAST(num AS INT)",
                             "SELECT _1                 FROM string_numbers GROUP BY CAST(num AS INT)",
                             "SELECT VALUE { '_1': _1 } FROM string_numbers GROUP BY CAST(num AS INT)"),
            expected = "<< { '_1': 1 }, { '_1': 2 } >>") +

        createGroupByTestCases(
            queries = listOf("SELECT *                            FROM simple_1_col_1_group GROUP BY col1 + 1 AS someGBE",
                             "SELECT someGBE                      FROM simple_1_col_1_group GROUP BY col1 + 1 AS someGBE",
                             "SELECT VALUE { 'someGBE': someGBE } FROM simple_1_col_1_group GROUP BY col1 + 1 AS someGBE"),
            expected = "<< { 'someGBE': 2 } >>") +
        createGroupByTestCases(
            queries = listOf("SELECT *                            FROM string_groups GROUP BY col1 || 'a' AS someGBE",
                             "SELECT someGBE                      FROM string_groups GROUP BY col1 || 'a' AS someGBE",
                             "SELECT VALUE { 'someGBE': someGBE } FROM string_groups GROUP BY col1 || 'a' AS someGBE"),
            expected = "<< { 'someGBE': 'aa' } >>") +
        createGroupByTestCases(
            queries = listOf("SELECT *                            FROM string_numbers GROUP BY CAST(num AS INT) AS someGBE",
                             "SELECT someGBE                      FROM string_numbers GROUP BY CAST(num AS INT) AS someGBE",
                             "SELECT VALUE { 'someGBE': someGBE } FROM string_numbers GROUP BY CAST(num AS INT) AS someGBE"),
            expected = "<< { 'someGBE': 1 }, { 'someGBE': 2 } >>") +

        // GROUP BY NULL/MISSING cases
        createGroupByTestCases(
            queries = listOf("SELECT *                              FROM simple_1_col_1_group GROUP BY NULL AS someNull",
                             "SELECT someNull                       FROM simple_1_col_1_group GROUP BY NULL AS someNull",
                             "SELECT VALUE { 'someNull': someNull } FROM simple_1_col_1_group GROUP BY NULL AS someNull"),
            expected = "<< { 'someNull': null } >>") +
        createGroupByTestCases(
            queries = listOf("SELECT *                                    FROM simple_1_col_1_group GROUP BY MISSING AS someMissing",
                             "SELECT someMissing                          FROM simple_1_col_1_group GROUP BY MISSING AS someMissing",
                             "SELECT VALUE { 'someMissing': someMissing } FROM simple_1_col_1_group GROUP BY MISSING AS someMissing"),
            // must explicitly specify MISSING here because https://github.com/partiql/partiql-lang-kotlin/issues/36
            expected = "<< { 'someMissing': MISSING } >>") +
        createGroupByTestCases(
            queries = listOf("SELECT *                              FROM simple_1_col_1_group GROUP BY NULL AS groupExp",
                             "SELECT groupExp                       FROM simple_1_col_1_group GROUP BY NULL AS groupExp",
                             "SELECT VALUE { 'groupExp': groupExp } FROM simple_1_col_1_group GROUP BY NULL AS groupExp"),
            expected = "<< { 'groupExp': null } >>") +
        createGroupByTestCases(
            queries = listOf("SELECT *                              FROM simple_1_col_1_group GROUP BY MISSING AS groupExp",
                             "SELECT groupExp                       FROM simple_1_col_1_group GROUP BY MISSING AS groupExp",
                             "SELECT VALUE { 'groupExp': groupExp } FROM simple_1_col_1_group GROUP BY MISSING AS groupExp"),
            expected = "<< { 'groupExp': MISSING } >>") +
        createGroupByTestCases(
            queries = listOf("SELECT *                                              FROM products_sparse p GROUP BY p.supplierId_nulls",
                             "SELECT supplierId_nulls                               FROM products_sparse p GROUP BY p.supplierId_nulls",
                             "SELECT VALUE { 'supplierId_nulls': supplierId_nulls } FROM products_sparse p GROUP BY p.supplierId_nulls"),
            expected = """<<
                            { 'supplierId_nulls': 10   },
                            { 'supplierId_nulls': 11   },
                            { 'supplierId_nulls': null }
                        >>""") +
        createGroupByTestCases(
            queries = listOf("SELECT *                                                       FROM products_sparse p GROUP BY p.supplierId_missings",
                             "SELECT p.supplierId_missings                                   FROM products_sparse p GROUP BY p.supplierId_missings",
                             "SELECT VALUE { 'supplierId_missings' : p.supplierId_missings } FROM products_sparse p GROUP BY p.supplierId_missings"),
            expected = """<<
                            { 'supplierId_missings': 10 },
                            { 'supplierId_missings': 11 },
                            --must explicitly include the missing value here because of https://github.com/partiql/partiql-lang-kotlin/issues/36
                            { 'supplierId_missings': missing }
                        >>"""
        ) +
        createGroupByTestCases(
            queries = listOf("SELECT *                                                 FROM products_sparse p GROUP BY p.supplierId_mixed",
                             "SELECT p.supplierId_mixed                                FROM products_sparse p GROUP BY p.supplierId_mixed",
                             "SELECT VALUE { 'supplierId_mixed' : p.supplierId_mixed } FROM products_sparse p GROUP BY p.supplierId_mixed"),
            expected = """<<
                            { 'supplierId_mixed': 10 },
                            { 'supplierId_mixed': 11 },
                            --must explicitly include the missing value here because of https://github.com/partiql/partiql-lang-kotlin/issues/363 and https://github.com/partiql/partiql-lang-kotlin/issues/35
                            { 'supplierId_mixed': missing }
                        >>""") +
        createGroupByTestCases(
            queries = listOf("SELECT *                                                                    FROM products_sparse p GROUP BY p.regionId, p.supplierId_nulls",
                             "SELECT regionId, supplierId_nulls                                           FROM products_sparse p GROUP BY p.regionId, p.supplierId_nulls",
                             "SELECT VALUE { 'regionId': regionId, 'supplierId_nulls': supplierId_nulls } FROM products_sparse p GROUP BY p.regionId, p.supplierId_nulls"),
            expected = """<<
                            { 'regionId': 100, 'supplierId_nulls': 10   },
                            { 'regionId': 100, 'supplierId_nulls': 11   },
                            { 'regionId': 100, 'supplierId_nulls': null },
                            { 'regionId': 200, 'supplierId_nulls': 10   },
                            { 'regionId': 200, 'supplierId_nulls': 11   },
                            { 'regionId': 200, 'supplierId_nulls': null }
                        >>""") +
        createGroupByTestCases(
            queries = listOf("SELECT *                                                                              FROM products_sparse p GROUP BY p.regionId, p.supplierId_missings",
                             "SELECT p.regionId, p.supplierId_missings                                              FROM products_sparse p GROUP BY p.regionId, p.supplierId_missings",
                             "SELECT VALUE { 'regionId': p.regionId, 'supplierId_missings': p.supplierId_missings } FROM products_sparse p GROUP BY p.regionId, p.supplierId_missings"),
            expected = """<<
                            --must explicitly include the missing values here because of https://github.com/partiql/partiql-lang-kotlin/issues/36
                            { 'regionId': 100, 'supplierId_missings': 10        },
                            { 'regionId': 100, 'supplierId_missings': 11        },
                            { 'regionId': 100, 'supplierId_missings': missing   },
                            { 'regionId': 200, 'supplierId_missings': 10        },
                            { 'regionId': 200, 'supplierId_missings': 11        },
                            { 'regionId': 200, 'supplierId_missings': missing   }
                        >>""") +
        createGroupByTestCases(
            queries = listOf("SELECT *                                                                         FROM products_sparse p GROUP BY p.regionId, p.supplierId_mixed",
                             "SELECT regionId, p.supplierId_mixed                                              FROM products_sparse p GROUP BY p.regionId, p.supplierId_mixed",
                             "SELECT VALUE { 'regionId': p.regionId, 'supplierId_mixed': p.supplierId_mixed }  FROM products_sparse p GROUP BY p.regionId, p.supplierId_mixed"),
            expected = """<<
                            --must explicitly include the missing values here because of https://github.com/partiql/partiql-lang-kotlin/issues/36
                            { 'regionId': 100, 'supplierId_mixed': 10       },
                            { 'regionId': 100, 'supplierId_mixed': 11       },
                            --for this group, the missing value in supplierId_mixed is encountered first
                            --see: https://github.com/partiql/partiql-lang-kotlin/issues/35
                            { 'regionId': 100, 'supplierId_mixed': missing  },


                            { 'regionId': 200, 'supplierId_mixed': 10       },
                            { 'regionId': 200, 'supplierId_mixed': 11       },
                            --for this group, the null value encountered first
                            { 'regionId': 200, 'supplierId_mixed': null     }
                        >>""")

    @Test
    @Parameters
    fun sql92StyleAggregatesTest(tc: EvaluatorTestCase) = runTestCase(tc, session)

    /**
     * Test cases that cover `COUNT`, `SUM`, `MIN`, `MAX`, and `AVG`.
     */
    fun parametersForSql92StyleAggregatesTest() =
        createAggregateTestCasesFromSqlStrings(
            groupName = "literal argument",
            sqlStrings = listOf("SELECT {{agg}}(5) FROM products"),
            expectedResultForCount = "<< { '_1': 5 } >>",
            expectedResultForSum   = "<< { '_1': 25 } >>",
            expectedResultForMin   = "<< { '_1': 5 } >>",
            expectedResultForMax   = "<< { '_1': 5 } >>",
            expectedResultForAvg   = "<< { '_1': 5 } >>"
        ) +
        createAggregateTestCasesFromSqlStrings(
            groupName = "variable argument",
            sqlStrings = listOf("SELECT {{agg}}(numInStock) AS agg FROM products",
                                "SELECT {{agg}}(p.numInStock) AS agg FROM products AS p"),

            expectedResultForCount = "<< { 'agg': 5 } >>",
            expectedResultForSum   = "<< { 'agg': 11111 } >>",
            expectedResultForMin   = "<< { 'agg': 1 } >>",
            expectedResultForMax   = "<< { 'agg': 10000 } >>",
            expectedResultForAvg   = "<< { 'agg': 2222.2 } >>"
        ) +
        createAggregateTestCasesFromSqlStrings(
            groupName = "binary expression argument",
            sqlStrings = listOf("SELECT {{agg}}(  numInStock + 1) AS agg FROM products",
                                "SELECT {{agg}}(p.numInStock + 1) AS agg FROM products as p"),

            expectedResultForCount = "<< { 'agg': 5 } >>",
            expectedResultForSum   = "<< { 'agg': 11116 } >>",
            expectedResultForMin   = "<< { 'agg': 2 } >>",
            expectedResultForMax   = "<< { 'agg': 10001 } >>",
            expectedResultForAvg   = "<< { 'agg': 2223.2 } >>"
        ) +
        createAggregateTestCasesFromSqlStrings(
            groupName = "as part of binary expression",
            sqlStrings = listOf("SELECT {{agg}}( numInStock) + 2 AS agg FROM products",
                                "SELECT {{agg}}(p.numInStock) + 2 AS agg FROM products as p"),

            expectedResultForCount = "<< { 'agg': 7 } >>",
            expectedResultForSum   = "<< { 'agg': 11113 } >>",
            expectedResultForMin   = "<< { 'agg': 3 } >>",
            expectedResultForMax   = "<< { 'agg': 10002 } >>",
            expectedResultForAvg   = "<< { 'agg': 2224.2 } >>"
        ) +
        createAggregateTestCasesFromSqlStrings(
            groupName = "variable or path argument and WHERE clause (1)",
            sqlStrings = listOf("SELECT {{agg}}(numInStock)   AS agg FROM products      WHERE supplierId = 10",
                                "SELECT {{agg}}(p.numInStock) AS agg FROM products AS p WHERE supplierId = 10"),

            expectedResultForCount = "<< { 'agg': 3 } >>",
            expectedResultForSum   = "<< { 'agg': 111 } >>",
            expectedResultForMin   = "<< { 'agg': 1 } >>",
            expectedResultForMax   = "<< { 'agg': 100 } >>",
            expectedResultForAvg   = "<< { 'agg': 37 } >>"
        ) +
        createAggregateTestCasesFromSqlStrings(
            groupName = "variable or path argument and WHERE clause (2)",
            sqlStrings = listOf("SELECT {{agg}}(  numInStock) AS agg FROM products      WHERE supplierId = 11",
                                "SELECT {{agg}}(p.numInStock) AS agg FROM products AS p WHERE supplierId = 11"),

            expectedResultForCount = "<< { 'agg': 2 } >>",
            expectedResultForSum   = "<< { 'agg': 11000 } >>",
            expectedResultForMin   = "<< { 'agg': 1000 } >>",
            expectedResultForMax   = "<< { 'agg': 10000 } >>",
            expectedResultForAvg   = "<< { 'agg': 5500 } >>"
        ) +
        createAggregateTestCasesFromSqlStrings(
            groupName = "variable or path argument and WHERE clause (3)",
            sqlStrings = listOf("SELECT {{agg}}(  numInStock) AS agg FROM products      WHERE categoryId = 20",
                                "SELECT {{agg}}(p.numInStock) AS agg FROM products AS p WHERE p.categoryId = 20"),

            expectedResultForCount = "<< { 'agg': 2 } >>",
            expectedResultForSum   = "<< { 'agg': 11 } >>",
            expectedResultForMin   = "<< { 'agg': 1 } >>",
            expectedResultForMax   = "<< { 'agg': 10 } >>",
            expectedResultForAvg   = "<< { 'agg': 5.5 } >>"
        ) +
        createAggregateTestCasesFromSqlStrings(
            groupName = "variable or path argument and WHERE clause (4)",
            sqlStrings = listOf("SELECT {{agg}}(  numInStock) AS agg FROM products WHERE categoryId = 21",
                                "SELECT {{agg}}(p.numInStock) AS agg FROM products AS p WHERE categoryId = 21"),

            expectedResultForCount = "<< { 'agg': 3 } >>",
            expectedResultForSum   = "<< { 'agg': 11100 } >>",
            expectedResultForMin   = "<< { 'agg': 100 } >>",
            expectedResultForMax   = "<< { 'agg': 10000 } >>",
            expectedResultForAvg   = "<< { 'agg': 3700 } >>"
        ) +
        createAggregateTestCasesFromSqlStrings(
            groupName = "GROUP BY (1 column) (#1)",
            sqlStrings = listOf("SELECT   supplierId, {{agg}}(  numInStock) AS agg FROM products      GROUP BY   supplierId",
                                "SELECT   supplierId, {{agg}}(p.numInStock) AS agg FROM products AS p GROUP BY p.supplierId",
                                "SELECT p.supplierId, {{agg}}(p.numInStock) AS agg FROM products AS p GROUP BY p.supplierId"),

            expectedResultForCount = """<<
                 { 'supplierId': 10, 'agg': 3 },
                 { 'supplierId': 11, 'agg': 2 }
            >>""",
            expectedResultForSum   = """<<
                 { 'supplierId': 10, 'agg': 111 },
                 { 'supplierId': 11, 'agg': 11000 }
            >>""",
            expectedResultForMin   = """<<
                 { 'supplierId': 10, 'agg': 1 },
                 { 'supplierId': 11, 'agg': 1000 }
            >>""",
            expectedResultForMax   ="""<<
                 { 'supplierId': 10, 'agg': 100 },
                 { 'supplierId': 11, 'agg': 10000 }
            >>""",
            expectedResultForAvg   = """<<
                 { 'supplierId': 10, 'agg': 37 },
                 { 'supplierId': 11, 'agg': 5500 }
            >>"""
        ) +
        createAggregateTestCasesFromSqlStrings(
            groupName = "GROUP BY (1 column) (#2)",
            sqlStrings = listOf("SELECT   categoryId, {{agg}}(  numInStock) AS agg FROM products      GROUP BY   categoryId",
                                "SELECT   categoryId, {{agg}}(p.numInStock) AS agg FROM products AS p GROUP BY p.categoryId",
                                "SELECT p.categoryId, {{agg}}(p.numInStock) AS agg FROM products AS p GROUP BY p.categoryId"),

            expectedResultForCount = """<<
                 { 'categoryId': 20, 'agg': 2 },
                 { 'categoryId': 21, 'agg': 3 }
            >>""",
            expectedResultForSum   = """<<
                 { 'categoryId': 20, 'agg': 11 },
                 { 'categoryId': 21, 'agg': 11100 }
            >>""",
            expectedResultForMin   = """<<
                 { 'categoryId': 20, 'agg': 1 },
                 { 'categoryId': 21, 'agg': 100 }
            >>""",
            expectedResultForMax   ="""<<
                 { 'categoryId': 20, 'agg': 10 },
                 { 'categoryId': 21, 'agg': 10000 }
            >>""",
            expectedResultForAvg   = """<<
                 { 'categoryId': 20, 'agg': 5.5 },
                 { 'categoryId': 21, 'agg': 3700 }
            >>"""
        ) +
        createAggregateTestCasesFromSqlStrings(
            groupName = "GROUP BY (1 column) and WHERE (#1)",
            sqlStrings = listOf(
                "SELECT   supplierId, {{agg}}(  numInStock) AS agg FROM products      WHERE price >= 10 GROUP BY   supplierId",
                "SELECT   supplierId, {{agg}}(p.numInStock) AS agg FROM products AS p WHERE price >= 10 GROUP BY p.supplierId",
                "SELECT p.supplierId, {{agg}}(p.numInStock) AS agg FROM products AS p WHERE price >= 10 GROUP BY p.supplierId"),

            expectedResultForCount = """<<
                 { 'supplierId': 10, 'agg': 2 },
                 { 'supplierId': 11, 'agg': 1 }
            >>""",
            expectedResultForSum   = """<<
                 { 'supplierId': 10, 'agg': 110 },
                 { 'supplierId': 11, 'agg': 10000 }
            >>""",
            expectedResultForMin   = """<<
                 { 'supplierId': 10, 'agg': 10 },
                 { 'supplierId': 11, 'agg': 10000 }
            >>""",
            expectedResultForMax   ="""<<
                 { 'supplierId': 10, 'agg': 100 },
                 { 'supplierId': 11, 'agg': 10000 }
            >>""",
            expectedResultForAvg   = """<<
                 { 'supplierId': 10, 'agg': 55 },
                 { 'supplierId': 11, 'agg': 10000 }
            >>"""
        ) +
        createAggregateTestCasesFromSqlStrings(
            groupName = "GROUP BY (1 column) and WHERE (#2)",
            sqlStrings = listOf(
                "SELECT   categoryId, {{agg}}(  numInStock) AS agg FROM products      WHERE price >= 10 GROUP BY   categoryId",
                "SELECT   categoryId, {{agg}}(p.numInStock) AS agg FROM products AS p WHERE price >= 10 GROUP BY p.categoryId",
                "SELECT p.categoryId, {{agg}}(p.numInStock) AS agg FROM products AS p WHERE price >= 10 GROUP BY p.categoryId"),

            expectedResultForCount = """<<
                 { 'categoryId': 20, 'agg': 1 },
                 { 'categoryId': 21, 'agg': 2 }
            >>""",
            expectedResultForSum   = """<<
                 { 'categoryId': 20, 'agg': 10 },
                 { 'categoryId': 21, 'agg': 10100 }
            >>""",
            expectedResultForMin   = """<<
                 { 'categoryId': 20, 'agg': 10 },
                 { 'categoryId': 21, 'agg': 100 }
            >>""",
            expectedResultForMax   ="""<<
                 { 'categoryId': 20, 'agg': 10 },
                 { 'categoryId': 21, 'agg': 10000 }
            >>""",
            expectedResultForAvg   = """<<
                 { 'categoryId': 20, 'agg': 10 },
                 { 'categoryId': 21, 'agg': 5050 }
            >>"""
        ) +
        createAggregateTestCasesFromSqlStrings(
            groupName = "GROUP BY (2 columns)",
            sqlStrings = listOf(
                "SELECT   supplierId,   categoryId, {{agg}}(  numInStock) AS agg FROM products      GROUP BY   supplierId,   categoryId",
                "SELECT   supplierId,   categoryId, {{agg}}(p.numInStock) AS agg FROM products AS p GROUP BY p.supplierId, p.categoryId",
                "SELECT p.supplierId, p.categoryId, {{agg}}(p.numInStock) AS agg FROM products   AS p GROUP BY p.supplierId, p.categoryId"),

            expectedResultForCount = """<<
                 { 'supplierId': 10, 'categoryId': 20, 'agg': 2 },
                 { 'supplierId': 10, 'categoryId': 21, 'agg': 1 },
                 { 'supplierId': 11, 'categoryId': 21, 'agg': 2 }
            >>""",
            expectedResultForSum   = """<<
                 { 'supplierId': 10, 'categoryId': 20, 'agg': 11 },
                 { 'supplierId': 10, 'categoryId': 21, 'agg': 100 },
                 { 'supplierId': 11, 'categoryId': 21, 'agg': 11000 }
            >>""",
            expectedResultForMin   = """<<
                 { 'supplierId': 10, 'categoryId': 20, 'agg': 1 },
                 { 'supplierId': 10, 'categoryId': 21, 'agg': 100 },
                 { 'supplierId': 11, 'categoryId': 21, 'agg': 1000 }
            >>""",
            expectedResultForMax   ="""<<
                 { 'supplierId': 10, 'categoryId': 20, 'agg': 10 },
                 { 'supplierId': 10, 'categoryId': 21, 'agg': 100 },
                 { 'supplierId': 11, 'categoryId': 21, 'agg': 10000 }
            >>""",
            expectedResultForAvg   = """<<
                 { 'supplierId': 10, 'categoryId': 20, 'agg': 5.5 },
                 { 'supplierId': 10, 'categoryId': 21, 'agg': 100 },
                 { 'supplierId': 11, 'categoryId': 21, 'agg': 5500 }
            >>"""
        ) +
        createAggregateTestCasesFromSqlStrings(
            groupName = "GROUP BY (2 columns) with WHERE",
            sqlStrings = listOf(
                "SELECT   supplierId,   categoryId, {{agg}}(  numInStock) AS agg FROM products      WHERE   price < 15 GROUP BY   supplierId,   categoryId",
                "SELECT   supplierId,   categoryId, {{agg}}(p.numInStock) AS agg FROM products AS p WHERE p.price < 15 GROUP BY p.supplierId, p.categoryId",
                "SELECT p.supplierId, p.categoryId, {{agg}}(p.numInStock) AS agg FROM products AS p WHERE p.price < 15 GROUP BY p.supplierId, p.categoryId"),
            expectedResultForCount = """<<
                 { 'supplierId': 10, 'categoryId': 20, 'agg': 2 },
                 { 'supplierId': 11, 'categoryId': 21, 'agg': 1 }
            >>""",
            expectedResultForSum   = """<<
                 { 'supplierId': 10, 'categoryId': 20, 'agg': 11 },
                 { 'supplierId': 11, 'categoryId': 21, 'agg': 1000 }
            >>""",
            expectedResultForMin   = """<<
                 { 'supplierId': 10, 'categoryId': 20, 'agg': 1 },
                 { 'supplierId': 11, 'categoryId': 21, 'agg': 1000 }
            >>""",
            expectedResultForMax   ="""<<
                 { 'supplierId': 10, 'categoryId': 20, 'agg': 10 },
                 { 'supplierId': 11, 'categoryId': 21, 'agg': 1000 }
            >>""",
            expectedResultForAvg   = """<<
                 { 'supplierId': 10, 'categoryId': 20, 'agg': 5.5 },
                 { 'supplierId': 11, 'categoryId': 21, 'agg': 1000 }
            >>"""
        ) +
        createAggregateTestCasesFromSqlTemplates(
            groupName = "null and missing aggregate arguments",
            sqlTemplates = listOf(
                SqlTemplate("SELECT COUNT(1) AS the_count, {{agg}}(  price_nulls)    AS the_agg FROM products_sparse"),
                SqlTemplate("SELECT COUNT(1) AS the_count, {{agg}}(  price_missings) AS the_agg FROM products_sparse AS p", CompOptions.onlyUndefinedVariableBehaviorMissing),
                SqlTemplate("SELECT COUNT(1) AS the_count, {{agg}}(  price_mixed)    AS the_agg FROM products_sparse AS p", CompOptions.onlyUndefinedVariableBehaviorMissing),

                SqlTemplate("SELECT COUNT(1) AS the_count, {{agg}}(p.price_nulls)    AS the_agg FROM products_sparse AS p"),
                SqlTemplate("SELECT COUNT(1) AS the_count, {{agg}}(p.price_missings) AS the_agg FROM products_sparse AS p"),
                SqlTemplate("SELECT COUNT(1) AS the_count, {{agg}}(p.price_mixed)    AS the_agg FROM products_sparse AS p")
            ),
            expectedResultForCount = "<< { 'the_count': 10, 'the_agg': 5 } >>",
            expectedResultForSum   = "<< { 'the_count': 10, 'the_agg': 15 } >>",
            expectedResultForMin   = "<< { 'the_count': 10, 'the_agg': 1 } >>",
            expectedResultForMax   = "<< { 'the_count': 10, 'the_agg': 5 } >>",
            expectedResultForAvg   = "<< { 'the_count': 10, 'the_agg': 3 } >>"
        ) +
        createAggregateTestCasesFromSqlTemplates(
            groupName = "null and missing aggregate arguments with GROUP BY",
            sqlTemplates = listOf(
                // Templates below which reference `price_missings` and `price_mixed` will only work with UndefinedVariableBehavior.MISSING
                SqlTemplate("SELECT  categoryId, COUNT(1) AS the_count, {{agg}}(  price_nulls)    AS the_agg FROM products_sparse AS p GROUP BY categoryId"),

                SqlTemplate("SELECT  categoryId, COUNT(1) AS the_count, {{agg}}(  price_missings) AS the_agg FROM products_sparse AS p GROUP BY categoryId", CompOptions.onlyUndefinedVariableBehaviorMissing),
                SqlTemplate("SELECT  categoryId, COUNT(1) AS the_count, {{agg}}(  price_mixed)    AS the_agg FROM products_sparse AS p GROUP BY categoryId", CompOptions.onlyUndefinedVariableBehaviorMissing),

                SqlTemplate("SELECT  categoryId, COUNT(1) AS the_count, {{agg}}(p.price_nulls)    AS the_agg FROM products_sparse AS p GROUP BY categoryId"),
                SqlTemplate("SELECT  categoryId, COUNT(1) AS the_count, {{agg}}(p.price_missings) AS the_agg FROM products_sparse AS p GROUP BY categoryId", CompOptions.onlyUndefinedVariableBehaviorMissing),
                SqlTemplate("SELECT  categoryId, COUNT(1) AS the_count, {{agg}}(p.price_mixed)    AS the_agg FROM products_sparse AS p GROUP BY categoryId", CompOptions.onlyUndefinedVariableBehaviorMissing),

                SqlTemplate("SELECT p.categoryId, COUNT(1) AS the_count, {{agg}}(  price_nulls)    AS the_agg FROM products_sparse AS p GROUP BY p.categoryId"),
                SqlTemplate("SELECT p.categoryId, COUNT(1) AS the_count, {{agg}}(  price_missings) AS the_agg FROM products_sparse AS p GROUP BY p.categoryId", CompOptions.onlyUndefinedVariableBehaviorMissing),
                SqlTemplate("SELECT p.categoryId, COUNT(1) AS the_count, {{agg}}(  price_mixed)    AS the_agg FROM products_sparse AS p GROUP BY p.categoryId", CompOptions.onlyUndefinedVariableBehaviorMissing),

                SqlTemplate("SELECT p.categoryId, COUNT(1) AS the_count, {{agg}}(p.price_nulls)    AS the_agg FROM products_sparse AS p GROUP BY p.categoryId"),
                SqlTemplate("SELECT p.categoryId, COUNT(1) AS the_count, {{agg}}(p.price_missings) AS the_agg FROM products_sparse AS p GROUP BY p.categoryId", CompOptions.onlyUndefinedVariableBehaviorMissing),
                SqlTemplate("SELECT p.categoryId, COUNT(1) AS the_count, {{agg}}(p.price_mixed)    AS the_agg FROM products_sparse AS p GROUP BY p.categoryId", CompOptions.onlyUndefinedVariableBehaviorMissing)
            ),
            expectedResultForCount = """<<
                { 'categoryId': 20, 'the_count': 4, 'the_agg': 3 },
                { 'categoryId': 21, 'the_count': 6, 'the_agg': 2 }
                >>""",
            expectedResultForSum   = """<<
                { 'categoryId': 20, 'the_count': 4, 'the_agg': 6 },
                { 'categoryId': 21, 'the_count': 6, 'the_agg': 9 }
                >>""",
            expectedResultForMin   = """<<
                { 'categoryId': 20, 'the_count': 4, 'the_agg': 1 },
                { 'categoryId': 21, 'the_count': 6, 'the_agg': 4 }
                >>""",
            expectedResultForMax   = """<<
                { 'categoryId': 20, 'the_count': 4, 'the_agg': 3 },
                { 'categoryId': 21, 'the_count': 6, 'the_agg': 5 }
                >>""",
            expectedResultForAvg   = """<<
                { 'categoryId': 20, 'the_count': 4, 'the_agg': 2 },
                { 'categoryId': 21, 'the_count': 6, 'the_agg': 4.5 }
                >>"""
        )

    @Test
    @Parameters
    fun groupByAggregatesTest(tc: EvaluatorTestCase) = runTestCase(tc, session)

    /**
     * These are test cases involving aggregates and cover behavior under less usual circumstances
     * that are not otherwise covered by [parametersForGroupByAggregatesTest].
     */
    fun parametersForGroupByAggregatesTest() = listOf(
        EvaluatorTestCase(
            "aggregates when used with empty from source",
            """
                SELECT
                    COUNT(doesntMatterWontBeEvaluated),
                    SUM(doesntMatterWontBeEvaluated),
                    MIN(doesntMatterWontBeEvaluated),
                    MAX(doesntMatterWontBeEvaluated),
                    AVG(doesntMatterWontBeEvaluated)
                FROM []
            """,
            // Note: COUNT(...) returning 0 here while the other aggregates return null does seem odd...
            // but this is consistent with at least mysql and postgres (possibly others).
            """<<
              {
                '_1':  0,
                '_2':  null,
                '_3':  null,
                '_4':  null,
                '_5':  null
              }
            >>"""),
        EvaluatorTestCase(
            "Expression with multiple subqueriees containing aggregates",
            "CAST((SELECT COUNT(1) FROM products) AS LIST)[0]._1 / CAST((SELECT COUNT(1) FROM suppliers) AS LIST)[0]._1",
            "2"),
        EvaluatorTestCase(
            "Aggregates with subquery containing another aggregate",
            "SELECT COUNT(1) + CAST((SELECT SUM(numInStock) FROM products) AS LIST)[0]._1 as a_number FROM products",
            "<<{ 'a_number': 11116 }>>"),
        EvaluatorTestCase(
            "GROUP BY with JOIN",
            """
                SELECT supplierName, COUNT(*) as the_count
                FROM suppliers AS s
                    INNER JOIN products AS p ON s.supplierId = p.supplierId
                GROUP BY supplierName
            """,
            """<<
                { 'supplierName': 'Umbrella', 'the_count': 3 },
                { 'supplierName': 'Initech', 'the_count': 2 }
            >>"""),
        EvaluatorTestCase(
            "`COUNT(*)`, should be equivalent to `COUNT(1)",
            "SELECT COUNT(*) AS the_count_1, COUNT(1) AS the_count_2 FROM products",
            "<< { 'the_count_1': 5, 'the_count_2': 5 } >>"),
        EvaluatorTestCase(
            "SELECT VALUE with nested aggregates",
            "SELECT VALUE (SELECT SUM(outerFromSource.col1) AS the_sum FROM <<1>>) FROM simple_1_col_1_group as outerFromSource",
            "<< << { 'the_sum': 1 } >>,  << { 'the_sum': 1 } >> >>")
    )

    @Test
    @Parameters
    fun groupByGroupAsTest(tc: EvaluatorTestCase) = runTestCase(tc, session)

    fun parametersForGroupByGroupAsTest() =
        // GROUP BY with GROUP AS (the same as above but with "GROUP AS g")
        createGroupByTestCases(
                listOf("SELECT *                              FROM simple_1_col_1_group GROUP BY col1 GROUP AS g",
                       "SELECT col1, g                        FROM simple_1_col_1_group GROUP BY col1 GROUP AS g",
                       "SELECT VALUE { 'col1': col1, 'g': g } FROM simple_1_col_1_group GROUP BY col1 GROUP AS g"),
                 """<<
                    {
                        'col1': 1,
                        'g': <<
                            { 'simple_1_col_1_group': { 'col1': 1 } },
                            { 'simple_1_col_1_group': { 'col1': 1 } }
                        >>
                    }
                    >>
                """) +

        createGroupByTestCases(
                listOf("SELECT *                              FROM simple_2_col_1_group GROUP BY col1 GROUP AS g",
                       "SELECT col1, g                        FROM simple_2_col_1_group GROUP BY col1 GROUP AS g",
                       "SELECT VALUE { 'col1': col1, 'g': g } FROM simple_2_col_1_group GROUP BY col1 GROUP AS g"),
                 """<<
                    {
                        'col1': 1,
                        'g': <<
                            { 'simple_2_col_1_group': { 'col1': 1, 'col2': 10 } },
                            { 'simple_2_col_1_group': { 'col1': 1, 'col2': 10 } }
                        >>
                    }
                    >>
                """) +

        createGroupByTestCases(
                listOf("SELECT *                              FROM simple_2_col_1_group GROUP BY col2 GROUP AS g",
                       "SELECT col2, g                        FROM simple_2_col_1_group GROUP BY col2 GROUP AS g",
                       "SELECT VALUE { 'col2': col2, 'g': g } FROM simple_2_col_1_group GROUP BY col2 GROUP AS g"),
                 """<<
                    {
                        'col2': 10,
                        'g': <<
                            { 'simple_2_col_1_group': { 'col1': 1, 'col2': 10 } },
                            { 'simple_2_col_1_group': { 'col1': 1, 'col2': 10 } }
                        >>
                    }
                    >>
                """) +

        createGroupByTestCases(
                listOf("SELECT *                              FROM simple_1_col_2_groups GROUP BY col1 GROUP AS g",
                       "SELECT col1, g                        FROM simple_1_col_2_groups GROUP BY col1 GROUP AS g",
                       "SELECT VALUE { 'col1': col1, 'g': g } FROM simple_1_col_2_groups GROUP BY col1 GROUP AS g"),
                 """<<
                    {
                        'col1': 1,
                        'g': <<
                            { 'simple_1_col_2_groups': { 'col1': 1 } },
                            { 'simple_1_col_2_groups': { 'col1': 1 } }
                        >>
                    },
                    {
                        'col1': 2,
                        'g': <<
                            { 'simple_1_col_2_groups': { 'col1': 2 } },
                            { 'simple_1_col_2_groups': { 'col1': 2 } }
                        >>
                    }
                    >>
                """) +
        createGroupByTestCases(
                listOf("SELECT *                              FROM simple_2_col_2_groups GROUP BY col1 GROUP AS g",
                       "SELECT col1, g                        FROM simple_2_col_2_groups GROUP BY col1 GROUP AS g",
                       "SELECT VALUE { 'col1': col1, 'g': g } FROM simple_2_col_2_groups GROUP BY col1 GROUP AS g"
                       ),
                 """<<
                    {
                        'col1': 1,
                        'g': <<
                            { 'simple_2_col_2_groups': { 'col1': 1, 'col2': 10 } },
                            { 'simple_2_col_2_groups': { 'col1': 1, 'col2': 10 } }
                        >>
                    },
                    {
                        'col1': 11,
                        'g': <<
                            { 'simple_2_col_2_groups': { 'col1': 11, 'col2': 110 } },
                            { 'simple_2_col_2_groups': { 'col1': 11, 'col2': 110 } }
                        >>
                    }
                    >>
                """) +

        createGroupByTestCases(
                listOf("SELECT *                              FROM simple_2_col_2_groups GROUP BY col2 GROUP AS g",
                       "SELECT col2, g                        FROM simple_2_col_2_groups GROUP BY col2 GROUP AS g",
                       "SELECT VALUE { 'col2': col2, 'g': g } FROM simple_2_col_2_groups GROUP BY col2 GROUP AS g"),
                 """<<
                    {
                        'col2': 10,
                        'g': <<
                            { 'simple_2_col_2_groups': { 'col1': 1, 'col2': 10 } },
                            { 'simple_2_col_2_groups': { 'col1': 1, 'col2': 10 } }
                        >>
                    },
                    {
                        'col2': 110,
                        'g': <<
                            { 'simple_2_col_2_groups': { 'col1': 11, 'col2': 110 } },
                            { 'simple_2_col_2_groups': { 'col1': 11, 'col2': 110 } }
                        >>
                    }
                    >>
                """) +

        // GROUP BY with GROUP AS and a JOIN
        createGroupByTestCases(
                listOf("SELECT *                              FROM simple_1_col_1_group, join_me GROUP BY col1 GROUP AS g",
                       "SELECT col1, g                        FROM simple_1_col_1_group, join_me GROUP BY col1 GROUP AS g",
                       "SELECT VALUE { 'col1': col1, 'g': g } FROM simple_1_col_1_group, join_me GROUP BY col1 GROUP AS g"),
                 """<<
                        {
                            'col1': 1,
                            'g':
                                <<
                                    { 'simple_1_col_1_group': { 'col1': 1 }, 'join_me': { 'foo': 20 } },
                                    { 'simple_1_col_1_group': { 'col1': 1 }, 'join_me': { 'foo': 30 } },
                                    { 'simple_1_col_1_group': { 'col1': 1 }, 'join_me': { 'foo': 20 } },
                                    { 'simple_1_col_1_group': { 'col1': 1 }, 'join_me': { 'foo': 30 } }
                                >>
                        }
                    >>
                """) +
        createGroupByTestCases(
                listOf("SELECT *                              FROM simple_1_col_1_group, different_types_per_row GROUP BY col1 GROUP AS g",
                       "SELECT col1, g                        FROM simple_1_col_1_group, different_types_per_row GROUP BY col1 GROUP AS g",
                       "SELECT VALUE { 'col1': col1, 'g': g } FROM simple_1_col_1_group, different_types_per_row GROUP BY col1 GROUP AS g"),
                 """<<
                        {
                            'col1': 1,
                            'g':
                                <<
                                    { 'simple_1_col_1_group': { 'col1': 1 }, 'different_types_per_row': {'a': 1001} },
                                    { 'simple_1_col_1_group': { 'col1': 1 }, 'different_types_per_row': 1002 },
                                    { 'simple_1_col_1_group': { 'col1': 1 }, 'different_types_per_row': 'one-thousand and three' },
                                    { 'simple_1_col_1_group': { 'col1': 1 }, 'different_types_per_row': {'a': 1001} },
                                    { 'simple_1_col_1_group': { 'col1': 1 }, 'different_types_per_row': 1002 },
                                    { 'simple_1_col_1_group': { 'col1': 1 }, 'different_types_per_row': 'one-thousand and three' }
                                >>
                        }
                    >>
                """)


    @Test
    @Parameters
    fun groupByShadowingTest(tc: EvaluatorTestCase) = runTestCase(tc, session)

    fun parametersForGroupByShadowingTest() =
        createGroupByTestCases(
            """
            SELECT
                a.categoryId,
                (
                    SELECT a.name
                    FROM widgets_b AS a --`a` shadows `a` from outer query
                ) AS from_widgets_b
            FROM widgets_a AS a
            GROUP BY a.categoryId
            """,
            "<< { 'categoryId': 1, 'from_widgets_b': <<{ 'name': 'Thingy' }>> }>>"
        ) +
        createGroupByTestCases(
            """
            SELECT
                a.categoryId,
                (
                    SELECT a.name
                    FROM widgets_b AS a --`a` shadows `a` from outer query
                    GROUP BY a.name
                ) AS from_widgets_b
            FROM widgets_a AS a
            GROUP BY a.categoryId
            """,
            "<< { 'categoryId': 1, 'from_widgets_b': <<{ 'name': 'Thingy' }>> }>>"
        )

    @Test
    @Parameters
    fun groupByDuplicateAliasesTest(tc: EvaluatorTestCase) = runTestCase(tc, session)

    fun parametersForGroupByDuplicateAliasesTest() =
        createGroupByTestCases(
            """
            SELECT dup
            FROM suppliers AS s
            GROUP BY s.supplierId AS dup, s.supplierName as dup
            """,
            "<< { 'dup': 10 }, { 'dup': 11 } >>"
        ) +
        createGroupByTestCases(
            """
            SELECT *
            FROM suppliers AS s
            GROUP BY s.supplierId AS dup, s.supplierName as dup
            """,
            """<< { 'dup': 10, 'dup': 'Umbrella' }, { 'dup': 11, 'dup': 'Initech' } >>"""
        )


    @Test
    fun cannotGroupBySelectListItemAliasTest() {
        checkInputThrowingEvaluationException(
            "SELECT foo AS someSelectListAlias FROM <<{ 'a': 1 }>> GROUP BY someSelectListAlias",
            ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
            mapOf(
                 Property.LINE_NUMBER to 1L,
                 Property.COLUMN_NUMBER to 64L,
                 Property.BINDING_NAME to "someSelectListAlias"
            ),
            null)
    }

    @Test
    fun missingGroupByTest() {
        checkInputThrowingEvaluationException(
            "SELECT MAX(@v2), @v2 FROM `[1, 2.0, 3e0, 4, 5d0]` AS v2",
            ErrorCode.EVALUATOR_VARIABLE_NOT_INCLUDED_IN_GROUP_BY,
            mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 19L,
                    Property.BINDING_NAME to "v2"
            )
        )
    }

    @Test
    fun missingGroupByCausedByHavingTest() {
        checkInputThrowingEvaluationException(
            "SELECT * FROM << {'a': 1 } >> AS f GROUP BY f.a HAVING f.id = 1",
            ErrorCode.EVALUATOR_VARIABLE_NOT_INCLUDED_IN_GROUP_BY,
            mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 56L,
                    Property.BINDING_NAME to "f"
            )
        )
    }

    @Test
    fun missingGroupBySelectValueTest() {
        checkInputThrowingEvaluationException(
            "SELECT VALUE f.id FROM << {'a': 'b' } >> AS f GROUP BY f.a",
            ErrorCode.EVALUATOR_VARIABLE_NOT_INCLUDED_IN_GROUP_BY,
            mapOf(
                    Property.LINE_NUMBER to 1L,
                    Property.COLUMN_NUMBER to 14L,
                    Property.BINDING_NAME to "f"
            )
        )
    }

    @Test
    fun missingGroupByCaseInsensitiveTest() {
        checkInputThrowingEvaluationException(
            """
            SELECT O.customerId, MAX(o.cost)
            FROM orders as o
            """,
            session,
            ErrorCode.EVALUATOR_VARIABLE_NOT_INCLUDED_IN_GROUP_BY,
            mapOf(
                    Property.LINE_NUMBER to 2L,
                    Property.COLUMN_NUMBER to 20L,
                    Property.BINDING_NAME to "O"
            )
        )
    }

    @Test
    fun missingGroupByCaseSensitiveTest() {
        checkInputThrowingEvaluationException(
            """
            SELECT "O".customerId, MAX(o.cost)
            FROM orders as o
            """,
            session,
            ErrorCode.EVALUATOR_BINDING_DOES_NOT_EXIST,
            mapOf(
                    Property.LINE_NUMBER to 2L,
                    Property.COLUMN_NUMBER to 20L,
                    Property.BINDING_NAME to "O"
            )
        )
    }

    @Test
    fun missingGroupByJoinTest() {
        checkInputThrowingEvaluationException(
            """
            SELECT MAX(o.cost), c.firstName
            FROM customers AS c
            INNER JOIN orders AS o ON c.customerId = o.customerId
            """,
            session,
            ErrorCode.EVALUATOR_VARIABLE_NOT_INCLUDED_IN_GROUP_BY,
            mapOf(
                    Property.LINE_NUMBER to 2L,
                    Property.COLUMN_NUMBER to 33L,
                    Property.BINDING_NAME to "c"
            )
        )
    }

    @Test
    fun missingGroupByHavingTest() {
        checkInputThrowingEvaluationException(
            """
            SELECT MAX(o.cost), o.sellerId
            FROM orders AS o
            GROUP BY o.customerId
            HAVING COUNT(1) > 1
            """,
            session,
            ErrorCode.EVALUATOR_VARIABLE_NOT_INCLUDED_IN_GROUP_BY,
            mapOf(
                    Property.LINE_NUMBER to 2L,
                    Property.COLUMN_NUMBER to 33L,
                    Property.BINDING_NAME to "o"
            )
        )
    }

    @Test
    fun missingGroupByOuterQueryTest() {
        checkInputThrowingEvaluationException(
            """
            SELECT AVG(o.cost), o.customerId
            FROM orders AS o
            WHERE
	            o.customerId IN(
                    SELECT VALUE o.customerId
                    FROM orders AS o
                    GROUP BY o.customerId
                )
            """,
            session,
            ErrorCode.EVALUATOR_VARIABLE_NOT_INCLUDED_IN_GROUP_BY,
            mapOf(
                    Property.LINE_NUMBER to 2L,
                    Property.COLUMN_NUMBER to 33L,
                    Property.BINDING_NAME to "o"
            )
        )
    }

    @Test
    fun missingGroupByInnerQueryTest() {
        checkInputThrowingEvaluationException(
            """
            SELECT MIN(o.numOrders)
            FROM(
                SELECT o.customerId, COUNT(1) AS numOrders
                FROM orders AS o
            ) as o
            GROUP BY o.customerId
            """,
            session,
            ErrorCode.EVALUATOR_VARIABLE_NOT_INCLUDED_IN_GROUP_BY,
            mapOf(
                    Property.LINE_NUMBER to 4L,
                    Property.COLUMN_NUMBER to 24L,
                    Property.BINDING_NAME to "o"
            )
        )
    }
}