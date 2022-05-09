package org.partiql.lang.eval

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestCase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget
import org.partiql.lang.util.ArgumentsProviderBase

class EvaluatingCompilerOrderByTests : EvaluatorTestBase() {
    private val session = mapOf(
        "simple_1" to "[{col1: 1, col2: 10}, {col1: 1, col2: 5}, {col1: 1, col2: 7}, {col1: 5, col2: 7}, {col1: 3, col2: 12}]",
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
        ]"""
    ).toSession()

    class ArgsProviderValid : ArgumentsProviderBase() {
        private val differentDataTypes = """
            [
                { 'data_value': {} },
                { 'data_value': 5 },
                { 'data_value': `2017-01-01T00:00-00:00` },
                { 'data_value': [] },
                { 'data_value': TIME '12:12:12.1' },
                { 'data_value': 'a' },
                { 'data_value': null },
                { 'data_value': false },
                { 'data_value': `{{YWFhYWFhYWFhYWFhYf8=}}` },
                { 'data_value': DATE '2021-08-22' },
                { 'data_value': <<>> },
                { 'data_value': `{{"aaaaaaaaaaaaa\xFF"}}` }
            ]
        """.trimIndent()

        override fun getParameters(): List<Any> = listOf(

            // SIMPLE CASES

            // should order by col1 asc
            EvaluatorTestCase(
                "SELECT col1 FROM simple_1 ORDER BY col1",
                "[{'col1': 1}, {'col1': 1}, {'col1': 1}, {'col1': 3}, {'col1': 5}]"
            ),
            // should order by col1 desc
            EvaluatorTestCase(
                "SELECT col1 FROM simple_1 ORDER BY col1 DESC",
                "[{'col1': 5}, {'col1': 3}, {'col1': 1}, {'col1': 1}, {'col1': 1}]"
            ),
            // should order by col1 and then col2 asc
            EvaluatorTestCase(
                "SELECT * FROM simple_1 ORDER BY col1, col2",
                "[{'col1': 1, 'col2': 5}, {'col1': 1, 'col2': 7}, {'col1': 1, 'col2': 10}, {'col1': 3, 'col2': 12}, {'col1': 5, 'col2': 7}]"
            ),
            // should order by price desc and productId asc
            EvaluatorTestCase(
                "SELECT productId, price FROM products ORDER BY price DESC, productId ASC",
                "[{'productId': 3, 'price': 15.0}, {'productId': 5, 'price': 15.0}, {'productId': 2, 'price': 10.0}, {'productId': 1, 'price': 5.0}, {'productId': 4, 'price': 5.0}]"
            ),
            // should order by supplierId_nulls nulls last
            EvaluatorTestCase(
                "SELECT productId, supplierId_nulls FROM products_sparse ORDER BY supplierId_nulls NULLS LAST, productId",
                "[{'productId': 1, 'supplierId_nulls': 10}, {'productId': 2, 'supplierId_nulls': 10}, {'productId': 3, 'supplierId_nulls': 10}, {'productId': 6, 'supplierId_nulls': 11}, {'productId': 7, 'supplierId_nulls': 11}, {'productId': 4, 'supplierId_nulls': NULL}, {'productId': 5, 'supplierId_nulls': NULL}, {'productId': 8, 'supplierId_nulls': NULL}, {'productId': 9, 'supplierId_nulls': NULL}, {'productId': 10, 'supplierId_nulls': NULL}]"
            ),
            // should order by supplierId_nulls nulls first
            EvaluatorTestCase(
                "SELECT productId, supplierId_nulls FROM products_sparse ORDER BY supplierId_nulls NULLS FIRST, productId",
                "[{'productId': 4, 'supplierId_nulls': NULL}, {'productId': 5, 'supplierId_nulls': NULL}, {'productId': 8, 'supplierId_nulls': NULL}, {'productId': 9, 'supplierId_nulls': NULL}, {'productId': 10, 'supplierId_nulls': NULL}, {'productId': 1, 'supplierId_nulls': 10}, {'productId': 2, 'supplierId_nulls': 10}, {'productId': 3, 'supplierId_nulls': 10}, {'productId': 6, 'supplierId_nulls': 11}, {'productId': 7, 'supplierId_nulls': 11}]"
            ),
            // should order by nulls last as default for supplierId_nulls asc
            EvaluatorTestCase(
                "SELECT productId, supplierId_nulls FROM products_sparse ORDER BY supplierId_nulls ASC, productId",
                "[{'productId': 1, 'supplierId_nulls': 10}, {'productId': 2, 'supplierId_nulls': 10}, {'productId': 3, 'supplierId_nulls': 10}, {'productId': 6, 'supplierId_nulls': 11}, {'productId': 7, 'supplierId_nulls': 11}, {'productId': 4, 'supplierId_nulls': NULL}, {'productId': 5, 'supplierId_nulls': NULL}, {'productId': 8, 'supplierId_nulls': NULL}, {'productId': 9, 'supplierId_nulls': NULL}, {'productId': 10, 'supplierId_nulls': NULL}]"
            ),
            // should order by nulls first as default for supplierId_nulls desc
            EvaluatorTestCase(
                "SELECT productId, supplierId_nulls FROM products_sparse ORDER BY supplierId_nulls DESC, productId",
                "[{'productId': 4, 'supplierId_nulls': NULL}, {'productId': 5, 'supplierId_nulls': NULL}, {'productId': 8, 'supplierId_nulls': NULL}, {'productId': 9, 'supplierId_nulls': NULL}, {'productId': 10, 'supplierId_nulls': NULL}, {'productId': 6, 'supplierId_nulls': 11}, {'productId': 7, 'supplierId_nulls': 11}, {'productId': 1, 'supplierId_nulls': 10}, {'productId': 2, 'supplierId_nulls': 10}, {'productId': 3, 'supplierId_nulls': 10}]"
            ),
            // should group and order by asc sellerId
            EvaluatorTestCase(
                "SELECT sellerId FROM orders GROUP BY sellerId ORDER BY sellerId ASC",
                "[{'sellerId': 1}, {'sellerId': 2}]"
            ),
            // should group and order by desc sellerId
            EvaluatorTestCase(
                "SELECT sellerId FROM orders GROUP BY sellerId ORDER BY sellerId DESC",
                "[{'sellerId': 2}, {'sellerId': 1}]"
            ),
            // should group and order by DESC (NULLS FIRST as default)
            EvaluatorTestCase(
                "SELECT supplierId_nulls FROM products_sparse GROUP BY supplierId_nulls ORDER BY supplierId_nulls DESC",
                " [{'supplierId_nulls': NULL}, {'supplierId_nulls': 11}, {'supplierId_nulls': 10}]"
            ),
            // should group and order by ASC (NULLS LAST as default)
            EvaluatorTestCase(
                "SELECT supplierId_nulls FROM products_sparse GROUP BY supplierId_nulls ORDER BY supplierId_nulls ASC",
                "[{'supplierId_nulls': 10}, {'supplierId_nulls': 11}, {'supplierId_nulls': NULL}]"
            ),
            // should group and place nulls first (asc as default)
            EvaluatorTestCase(
                "SELECT supplierId_nulls FROM products_sparse GROUP BY supplierId_nulls ORDER BY supplierId_nulls NULLS FIRST",
                "[{'supplierId_nulls': NULL}, {'supplierId_nulls': 10}, {'supplierId_nulls': 11}]"
            ),
            // should group and place nulls last (asc as default)
            EvaluatorTestCase(
                "SELECT supplierId_nulls FROM products_sparse GROUP BY supplierId_nulls ORDER BY supplierId_nulls NULLS LAST",
                "[{'supplierId_nulls': 10}, {'supplierId_nulls': 11}, {'supplierId_nulls': NULL}]"
            ),
            // should group and order by asc and place nulls first
            EvaluatorTestCase(
                "SELECT supplierId_nulls FROM products_sparse GROUP BY supplierId_nulls ORDER BY supplierId_nulls ASC NULLS FIRST",
                "[{'supplierId_nulls': NULL}, {'supplierId_nulls': 10}, {'supplierId_nulls': 11}]"
            ),

            // DIFFERENT DATA TYPES
            // should order different data types by following order bool, numbers, date, time, timestamp, text, LOB Types, lists, struct, bag
            // handling nulls/missing can be change by ordering spec(if nulls spec is not specified, NULLS FIRST is default for asc, NULLS LAST default for desc) or nulls spec

            // should order data types by the specifications (NULLS LAST default for asc)
            EvaluatorTestCase(
                "SELECT * FROM $differentDataTypes ORDER BY data_value",
                """[{'data_value': false}, {'data_value': 5}, {'data_value': DATE '2021-08-22'}, {'data_value': TIME '12:12:12.1'}, {'data_value': `2017-01-01T00:00-00:00`}, {'data_value': 'a'}, {'data_value': `{{YWFhYWFhYWFhYWFhYf8=}}`}, {'data_value': `{{"aaaaaaaaaaaaa\xff"}}`}, {'data_value': []}, {'data_value': {}}, {'data_value': <<>>}, {'data_value': NULL}]"""
            ),
            // should order data types by the specifications (NULLS FIRST default for desc)
            EvaluatorTestCase(
                "SELECT * FROM $differentDataTypes ORDER BY data_value DESC",
                """[{'data_value': NULL}, {'data_value': <<>>}, {'data_value': {}}, {'data_value': []}, {'data_value': `{{YWFhYWFhYWFhYWFhYf8=}}`}, {'data_value': `{{"aaaaaaaaaaaaa\xff"}}`}, {'data_value': 'a'}, {'data_value': `2017-01-01T00:00-00:00`}, {'data_value': TIME '12:12:12.1'}, {'data_value': DATE '2021-08-22'}, {'data_value': 5}, {'data_value': false}]"""
            ),
            // should order data types by the specifications (nulls should be first due to nulls spec)
            EvaluatorTestCase(
                "SELECT * FROM $differentDataTypes ORDER BY data_value NULLS FIRST",
                """[{'data_value': NULL}, {'data_value': false}, {'data_value': 5}, {'data_value': DATE '2021-08-22'}, {'data_value': TIME '12:12:12.1'}, {'data_value': `2017-01-01T00:00-00:00`}, {'data_value': 'a'}, {'data_value': `{{YWFhYWFhYWFhYWFhYf8=}}`}, {'data_value': `{{"aaaaaaaaaaaaa\xff"}}`}, {'data_value': []}, {'data_value': {}}, {'data_value': <<>>}]"""
            ),
            // should order data types by the specifications (nulls should be last due to nulls spec)
            EvaluatorTestCase(
                "SELECT * FROM $differentDataTypes ORDER BY data_value NULLS LAST",
                """[{'data_value': false}, {'data_value': 5}, {'data_value': DATE '2021-08-22'}, {'data_value': TIME '12:12:12.1'}, {'data_value': `2017-01-01T00:00-00:00`}, {'data_value': 'a'}, {'data_value': `{{YWFhYWFhYWFhYWFhYf8=}}`}, {'data_value': `{{"aaaaaaaaaaaaa\xff"}}`}, {'data_value': []}, {'data_value': {}}, {'data_value': <<>>}, {'data_value': NULL}]"""
            ),

            // EDGE CASES

            // false before true (ASC)
            EvaluatorTestCase(
                "SELECT * FROM [{ 'a': false }, { 'a': true }, { 'a': true }, { 'a': false }] ORDER BY a",
                "[{'a': false}, {'a': false}, {'a': true}, {'a': true}]"
            ),
            // true before false (DESC)
            EvaluatorTestCase(
                "SELECT * FROM [{ 'a': false }, { 'a': true }, { 'a': true }, { 'a': false }] ORDER BY a DESC",
                "[{'a': true}, {'a': true}, {'a': false}, {'a': false}]"
            ),
            // nan before -inf, then numeric values then +inf (ASC)
            EvaluatorTestCase(
                "SELECT * FROM [{ 'a': 5 }, { 'a': -5e-1 }, { 'a': `-inf` }, { 'a': `nan` }, { 'a': 7 }, { 'a': `+inf` }, { 'a': 9 }] ORDER BY a",
                "[{'a': `nan`}, {'a': `-inf`}, {'a': -0.5}, {'a': 5}, {'a': 7}, {'a': 9}, {'a': `+inf`}]"
            ),
            // +inf before numeric values then -inf then nan (DESC)
            EvaluatorTestCase(
                "SELECT * FROM [{ 'a': 5 }, { 'a': -5e-1 }, { 'a': `-inf` }, { 'a': `nan` }, { 'a': 7 }, { 'a': `+inf` }, { 'a': 9 }] ORDER BY a DESC",
                "[{'a': `+inf`}, {'a': 9}, {'a': 7}, {'a': 5}, {'a': -0.5}, {'a': `-inf`}, {'a': `nan`}]"
            ),
            // text types compared by lexicographical ordering of Unicode scalar (ASC)
            EvaluatorTestCase(
                """SELECT * FROM [{ 'a': `'\uD83D\uDCA9'`}, { 'a': 'Z'}, { 'a': '9' }, { 'a': 'A'}, { 'a': `"\U0001F4A9"`}, { 'a': 'a'}, { 'a': 'z'}, { 'a': '0' }] ORDER BY a""",
                """[{'a': '0'}, {'a': '9'}, {'a': 'A'}, {'a': 'Z'}, {'a': 'a'}, {'a': 'z'}, {'a': `"\U0001F4A9"`}, {'a': `'\uD83D\uDCA9'`}]"""
            ),
            // text types compared by lexicographical ordering of Unicode scalar (DESC)
            EvaluatorTestCase(
                """SELECT * FROM [{ 'a': `'\uD83D\uDCA9'`}, { 'a': 'Z'}, { 'a': '9' }, { 'a': 'A'}, { 'a': `"\U0001F4A9"`}, { 'a': 'a'}, { 'a': 'z'}, { 'a': '0' }] ORDER BY a DESC""",
                """[{'a': `'\uD83D\uDCA9'`}, {'a': `"\U0001F4A9"`}, {'a': 'z'}, {'a': 'a'}, {'a': 'Z'}, {'a': 'A'}, {'a': '9'}, {'a': '0'}]"""
            ),
            // LOB types follow their lexicographical ordering by octet (ASC)
            EvaluatorTestCase(
                """SELECT * FROM [{'a': `{{"Z"}}`}, {'a': `{{"a"}}`}, {'a': `{{"A"}}`}, {'a': `{{"z"}}`}] ORDER BY a""",
                """[{'a': `{{"A"}}`}, {'a': `{{"Z"}}`}, {'a': `{{"a"}}`}, {'a': `{{"z"}}`}]"""
            ),
            // LOB types should ordered (DESC)
            EvaluatorTestCase(
                """SELECT * FROM [{'a': `{{"Z"}}`}, {'a': `{{"a"}}`}, {'a': `{{"A"}}`}, {'a': `{{"z"}}`}] ORDER BY a DESC""",
                """[{'a': `{{"z"}}`}, {'a': `{{"a"}}`}, {'a': `{{"Z"}}`}, {'a': `{{"A"}}`}]"""
            ),
            // shorter array comes first (ASC)
            EvaluatorTestCase(
                "SELECT * FROM [ {'a': [1, 2, 3, 4]}, {'a': [1, 2]}, {'a': [1, 2, 3]}, {'a': []}] ORDER BY a",
                "[{'a': []}, {'a': [1, 2]}, {'a': [1, 2, 3]}, {'a': [1, 2, 3, 4]}]"
            ),
            // longer array comes first (DESC)
            EvaluatorTestCase(
                "SELECT * FROM [ {'a': [1, 2, 3, 4]}, {'a': [1, 2]}, {'a': [1, 2, 3]}, {'a': []}] ORDER BY a DESC",
                "[{'a': [1, 2, 3, 4]}, {'a': [1, 2, 3]}, {'a': [1, 2]}, {'a': []}]"
            ),
            // lists compared lexicographically based on comparison of elements (ASC)
            EvaluatorTestCase(
                "SELECT * FROM [ {'a': ['b', 'a']}, {'a': ['a', 'b']}, {'a': ['b', 'c']}, {'a': ['a', 'c']}] ORDER BY a",
                "[{'a': ['a', 'b']}, {'a': ['a', 'c']}, {'a': ['b', 'a']}, {'a': ['b', 'c']}]"
            ),
            // lists compared lexicographically based on comparison of elements (DESC)
            EvaluatorTestCase(
                "SELECT * FROM [ {'a': ['b', 'a']}, {'a': ['a', 'b']}, {'a': ['b', 'c']}, {'a': ['a', 'c']}] ORDER BY a DESC",
                "[{'a': ['b', 'c']}, {'a': ['b', 'a']}, {'a': ['a', 'c']}, {'a': ['a', 'b']}]"
            ),
            // lists items should be ordered by data types (ASC) (nulls last as default for asc)
            EvaluatorTestCase(
                """SELECT * FROM [{'a': ['a']}, {'a': [1]}, {'a': [true]}, {'a': [null]}, {'a': [{}]}, {'a': [<<>>]}, {'a': [`{{}}`]}, {'a': [[]]} ] ORDER BY a""",
                "[{'a': [true]}, {'a': [1]}, {'a': ['a']}, {'a': [`{{}}`]}, {'a': [[]]}, {'a': [{}]}, {'a': [<<>>]}, {'a': [NULL]}]"
            ),
            // lists items should be ordered by data types (DESC) (nulls first as default for desc)
            EvaluatorTestCase(
                """SELECT * FROM [{'a': ['a']}, {'a': [1]}, {'a': [true]}, {'a': [null]}, {'a': [{}]}, {'a': [<<>>]}, {'a': [`{{}}`]}, {'a': [[]]} ] ORDER BY a DESC""",
                "[{'a': [NULL]}, {'a': [<<>>]}, {'a': [{}]}, {'a': [[]]}, {'a': [`{{}}`]}, {'a': ['a']}, {'a': [1]}, {'a': [true]}]"
            ),
            // structs compared lexicographically first by key then by value (ASC)
            EvaluatorTestCase(
                "SELECT * FROM [{'a': {'b': 'a'}}, {'a': {'a': 'b'}}, {'a': {'b': 'c'}}, {'a': {'a': 'c'}}] ORDER BY a",
                "[{'a': {'a': 'b'}}, {'a': {'a': 'c'}}, {'a': {'b': 'a'}}, {'a': {'b': 'c'}}]"
            ),
            // structs compared lexicographically first by key then by value (DESC)
            EvaluatorTestCase(
                "SELECT * FROM [{'a': {'b': 'a'}}, {'a': {'a': 'b'}}, {'a': {'b': 'c'}}, {'a': {'a': 'c'}}] ORDER BY a DESC",
                "[{'a': {'b': 'c'}}, {'a': {'b': 'a'}}, {'a': {'a': 'c'}}, {'a': {'a': 'b'}}]"
            ),
            // structs should be ordered by data types (ASC) (nulls last as default for asc)
            EvaluatorTestCase(
                "SELECT * FROM [{'a': {'a': 5}}, {'a': {'a': 'b'}}, {'a': {'a': true}}, {'a': {'a': []}}, {'a': {'a': {}}}, {'a': {'a': <<>>}}, {'a': {'a': `{{}}`}}, {'a': {'a': null}}] ORDER BY a",
                "[{'a': {'a': true}}, {'a': {'a': 5}}, {'a': {'a': 'b'}}, {'a': {'a': `{{}}`}}, {'a': {'a': []}}, {'a': {'a': {}}}, {'a': {'a': <<>>}}, {'a': {'a': NULL}}]"
            ),
            // structs should be ordered by data types (DESC) (nulls first as default for desc)
            EvaluatorTestCase(
                "SELECT * FROM [{'a': {'a': 5}}, {'a': {'a': 'b'}}, {'a': {'a': true}}, {'a': {'a': []}}, {'a': {'a': {}}}, {'a': {'a': <<>>}}, {'a': {'a': `{{}}`}}, {'a': {'a': null}}] ORDER BY a DESC",
                "[{'a': {'a': NULL}}, {'a': {'a': <<>>}}, {'a': {'a': {}}}, {'a': {'a': []}}, {'a': {'a': `{{}}`}}, {'a': {'a': 'b'}}, {'a': {'a': 5}}, {'a': {'a': true}}]"
            ),
            // bags compared as sorted lists (ASC)
            EvaluatorTestCase(
                "SELECT * FROM [{'a': <<5>>}, {'a': <<1>>}, {'a': <<10>>}] ORDER BY a",
                "[{'a': <<1>>}, {'a': <<5>>}, {'a': <<10>>}]"
            ),
            // bags compared as sorted lists (DESC)
            EvaluatorTestCase(
                "SELECT * FROM [{'a': <<5>>}, {'a': <<1>>}, {'a': <<10>>}] ORDER BY a DESC",
                "[{'a': <<10>>}, {'a': <<5>>}, {'a': <<1>>}]"
            ),
        )
    }

    @ParameterizedTest
    @ArgumentsSource(ArgsProviderValid::class)
    fun validTests(tc: EvaluatorTestCase) = runEvaluatorTestCase(
        tc = tc.copy(
            excludeLegacySerializerAssertions = true,
            targetPipeline = EvaluatorTestTarget.COMPILER_PIPELINE, // planner & phys. alg. have no support for ORDER BY (yet)
        ),
        session = session
    )
}
