package org.partiql.lang.eval

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
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
        ]""",
        "employees" to """[
             { name: 'Joey', age: 25, manager: { name: 'John', address: { city: 'Seattle' } } },
             { name: 'Chandler', age: 27, manager: { name: 'Rocky', address: { city: 'Seattle' } } },
             { name: 'Ross', age: 22, manager: { 'name': 'Alex', address: { city: 'Chicago' } } }
        ]"""
    ).toSession()

    class ArgsProviderValid : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> = listOf(
            // should order by col1 asc
            EvaluatorTestCase(
                "SELECT col1 FROM simple_1 ORDER BY col1",
                "<<{'col1': 1}, {'col1': 1}, {'col1': 1}, {'col1': 3}, {'col1': 5}>>"
            ),
            // should order by col1 desc
            EvaluatorTestCase(
                "SELECT col1 FROM simple_1 ORDER BY col1 DESC",
                "<<{'col1': 5}, {'col1': 3}, {'col1': 1}, {'col1': 1}, {'col1': 1}>>"
            ),
            // should order by col1 and then col2 asc
            EvaluatorTestCase(
                "SELECT * FROM simple_1 ORDER BY col1, col2",
                "<<{'col1': 1, 'col2': 5}, {'col1': 1, 'col2': 7}, {'col1': 1, 'col2': 10}, {'col1': 5, 'col2': 7}, {'col1': 3, 'col2': 12}>>"
            ),
            // should order by price desc and productId asc
            EvaluatorTestCase(
                "SELECT * FROM products ORDER BY price DESC, productId ASC LIMIT 1",
                "<<{ 'productId': 3, 'supplierId': 10, 'categoryId': 21, 'price': 15.0, 'numInStock': 100 }>>"
            ),
            // should order by supplierId_nulls nulls last
            EvaluatorTestCase(
                "SELECT productId FROM products_sparse ORDER BY supplierId_nulls NULLS LAST, productId",
                "<<{'productId': 1}, {'productId': 2}, {'productId': 3}, {'productId': 6}, {'productId': 7}, {'productId': 4}, {'productId': 5}, {'productId': 8}, {'productId': 9}, {'productId': 10}>>"
            ),
            // should order by supplierId_nulls nulls first
            EvaluatorTestCase(
                "SELECT productId FROM products_sparse ORDER BY supplierId_nulls NULLS FIRST, productId",
                "<<{'productId': 4}, {'productId': 5}, {'productId': 8}, {'productId': 9}, {'productId': 10}, {'productId': 1}, {'productId': 2}, {'productId': 3}, {'productId': 6}, {'productId': 7}>>"
            )
        )
    }

    @ParameterizedTest
    @ArgumentsSource(ArgsProviderValid::class)
    fun validTests(tc: EvaluatorTestCase) = runTestCase(tc, session)
}
