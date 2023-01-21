package org.partiql.lang.eval.builtins.windowFunctions

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestCase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget
import org.partiql.lang.util.ArgumentsProviderBase

class WindowFunctionTests : EvaluatorTestBase() {
    private val session = mapOf(
        "stock_price" to """[
            { date: 2022-09-30, ticker: "AMZN", price: 113.00},
            { date: 2022-10-03, ticker: "AMZN", price: 115.88},
            { date: 2022-10-04, ticker: "AMZN", price: 121.09},
            { date: 2022-09-30, ticker: "GOOG", price: 96.15},
            { date: 2022-10-03, ticker: "GOOG", price: 99.30},
            { date: 2022-10-04, ticker: "GOOG", price: 101.04}
        ]""",
    ).toSession()

    @ParameterizedTest
    @ArgumentsSource(LagFunctionTestsProvider::class)
    fun lagFunctionTests(tc: EvaluatorTestCase) = runEvaluatorTestCase(
        tc = tc.copy(targetPipeline = EvaluatorTestTarget.PARTIQL_PIPELINE),
        session = session
    )
    class LagFunctionTestsProvider : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            // Lag Function with PARTITION BY AND ORDER BY
            EvaluatorTestCase(
                query = """
                    SELECT sp."date" as "date",
                        sp.ticker as ticker,
                        sp.price as current_price,
                        lag(sp.price) OVER (PARTITION BY sp.ticker ORDER BY sp."date") as previous_price
                    FROM stock_price as sp
                """,
                expectedResult = """<<
                    { 'date': `2022-09-30`, 'ticker': 'AMZN', 'current_price': 113.00, 'previous_price': NULL},
                    { 'date': `2022-10-03`, 'ticker': 'AMZN', 'current_price': 115.88, 'previous_price': 113.00},
                    { 'date': `2022-10-04`, 'ticker': 'AMZN', 'current_price': 121.09, 'previous_price': 115.88},
                    { 'date': `2022-09-30`, 'ticker': 'GOOG', 'current_price': 96.15, 'previous_price': NULL},
                    { 'date': `2022-10-03`, 'ticker': 'GOOG', 'current_price': 99.30, 'previous_price': 96.15},
                    { 'date': `2022-10-04`, 'ticker': 'GOOG', 'current_price': 101.04, 'previous_price': 99.30}
                >>"""
            ),

            // Binding to Missing
            EvaluatorTestCase(
                query = """
                    SELECT sp."date" as "date",
                        sp.ticker as ticker,
                        sp.price as current_price,
                        lag(sp.a, 1, 'OUT OF PARTITION') OVER (PARTITION BY sp.ticker ORDER BY sp."date") as previous_a
                    FROM stock_price as sp
                """,
                expectedResult = """<<
                    { 'date': `2022-09-30`, 'ticker': 'AMZN', 'current_price': 113.00, 'previous_a': 'OUT OF PARTITION'},
                    { 'date': `2022-10-03`, 'ticker': 'AMZN', 'current_price': 115.88},
                    { 'date': `2022-10-04`, 'ticker': 'AMZN', 'current_price': 121.09},
                    { 'date': `2022-09-30`, 'ticker': 'GOOG', 'current_price': 96.15, 'previous_a': 'OUT OF PARTITION'},
                    { 'date': `2022-10-03`, 'ticker': 'GOOG', 'current_price': 99.30},
                    { 'date': `2022-10-04`, 'ticker': 'GOOG', 'current_price': 101.04}
                >>"""
            ),

            // Use Lag Function result as an exprValue
            EvaluatorTestCase(
                query = """
                    SELECT sp."date" as "date",
                        sp.ticker as ticker,
                        sp.price as current_price,
                        lag(sp.price) OVER (PARTITION BY sp.ticker ORDER BY sp."date") + 100 as previous_price
                    FROM stock_price as sp
                """,
                expectedResult = """<<
                    { 'date': `2022-09-30`, 'ticker': 'AMZN', 'current_price': 113.00, 'previous_price': NULL},
                    { 'date': `2022-10-03`, 'ticker': 'AMZN', 'current_price': 115.88, 'previous_price': 213.00},
                    { 'date': `2022-10-04`, 'ticker': 'AMZN', 'current_price': 121.09, 'previous_price': 215.88},
                    { 'date': `2022-09-30`, 'ticker': 'GOOG', 'current_price': 96.15, 'previous_price': NULL},
                    { 'date': `2022-10-03`, 'ticker': 'GOOG', 'current_price': 99.30, 'previous_price': 196.15},
                    { 'date': `2022-10-04`, 'ticker': 'GOOG', 'current_price': 101.04, 'previous_price': 199.30}
                >>"""
            ),

            // LAG with ORDER BY; Window Ordering does not affect the result ordering
            EvaluatorTestCase(
                query = """
                    SELECT sp."date" as "date",
                        sp.ticker as ticker,
                        sp.price as current_price,
                        lag(sp.price) OVER (PARTITION BY sp.ticker ORDER BY sp."date") as previous_price
                    FROM stock_price as sp
                    ORDER BY sp."date" DESC
                """,
                expectedResult = """[
                    { 'date': `2022-10-04`, 'ticker': 'AMZN', 'current_price': 121.09, 'previous_price': 115.88},
                    { 'date': `2022-10-04`, 'ticker': 'GOOG', 'current_price': 101.04, 'previous_price': 99.30},
                    { 'date': `2022-10-03`, 'ticker': 'AMZN', 'current_price': 115.88, 'previous_price': 113.00},
                    { 'date': `2022-10-03`, 'ticker': 'GOOG', 'current_price': 99.30, 'previous_price': 96.15},
                    { 'date': `2022-09-30`, 'ticker': 'AMZN', 'current_price': 113.00, 'previous_price': NULL},
                    { 'date': `2022-09-30`, 'ticker': 'GOOG', 'current_price': 96.15, 'previous_price': NULL}
                ]"""
            ),

            // LAG Function with GROUP BY
            EvaluatorTestCase(
                query = """
                    SELECT
                        month as current_month,
                        ticker as ticker,
                        avg(price) as current_month_average,
                        lag(avg(price)) OVER (PARTITION BY ticker ORDER BY month) as previous_month_avg
                    FROM stock_price as sp
                    GROUP BY EXTRACT(MONTH FROM sp."date") as month, sp.ticker as ticker GROUP AS g
                """,
                expectedResult = """<<
                    { 'current_month': 9., 'ticker': 'AMZN', 'current_month_average': 113.00, 'previous_month_avg': NULL},
                    { 'current_month': 10., 'ticker':'AMZN', 'current_month_average': 118.485, 'previous_month_avg': 113.00},
                    { 'current_month': 9., 'ticker': 'GOOG', 'current_month_average': 96.15, 'previous_month_avg': NULL},
                    { 'current_month': 10., 'ticker': 'GOOG', 'current_month_average': 100.17, 'previous_month_avg': 96.15}
                >>"""
            ),

            // Lag function in sub-query
            EvaluatorTestCase(
                query = """
                    SELECT
                       (SELECT
                            lag(sp.price) OVER (PARTITION BY sp.ticker ORDER BY sp."date") as previous_price
                            FROM <<1>>
                       )
                    FROM stock_price as sp
                """,
                expectedResult = """<<
                    {'_1': <<{'previous_price': NULL}>>},
                    {'_1': <<{'previous_price': NULL}>>},
                    {'_1': <<{'previous_price': NULL}>>},
                    {'_1': <<{'previous_price': NULL}>>},
                    {'_1': <<{'previous_price': NULL}>>},
                    {'_1': <<{'previous_price': NULL}>>}
                >>"""
            ),

            EvaluatorTestCase(
                query = """
                    SELECT
                       lag(sp.price) OVER (PARTITION BY sp.ticker ORDER BY sp."date") as previous_price,
                       (SELECT
                            lag(sp.price) OVER (PARTITION BY sp.ticker ORDER BY sp."date") as Inner_lag
                       FROM <<1>>)
                    FROM stock_price as sp
                """,
                expectedResult = """<<
                    {'previous_price': NULL, '_2': <<{'Inner_lag': NULL}>>},
                    {'previous_price': 113.00, '_2': <<{'Inner_lag': NULL}>>},
                    {'previous_price': 115.88, '_2': <<{'Inner_lag': NULL}>>},
                    {'previous_price': NULL, '_2': <<{'Inner_lag': NULL}>>},
                    {'previous_price': 96.15, '_2': <<{'Inner_lag': NULL}>>},
                    {'previous_price': 99.30, '_2': <<{'Inner_lag': NULL}>>}
                >>"""
            ),

            EvaluatorTestCase(
                query = """
                    SELECT a.res FROM (
                        SELECT LAG(sp.price) OVER(PARTITION BY sp.ticker ORDER BY sp."date") AS res
                        FROM stock_price AS sp
                    ) as a
                """,
                expectedResult = """<<
                    {'res': NULL},
                    {'res': 113.00},
                    {'res': 115.88},
                    {'res': NULL},
                    {'res': 96.15},
                    {'res': 99.30}
                >>"""
            ),

            EvaluatorTestCase(
                query = """
                    SELECT Lag(a.res, 1, 0) OVER (ORDER BY a.res NULLS FIRST) as prev_res_or_zero FROM (
                        SELECT LAG(sp.price) OVER(PARTITION BY sp.ticker ORDER BY sp."date") AS res
                        FROM stock_price AS sp
                    ) as a
                """,
                expectedResult = """<<
                    {'prev_res_or_zero': 0},
                    {'prev_res_or_zero': NULL},
                    {'prev_res_or_zero': NULL},
                    {'prev_res_or_zero': 96.15},
                    {'prev_res_or_zero': 99.30},
                    {'prev_res_or_zero': 113.00}
                >>"""
            ),
        )
    }

    @ParameterizedTest
    @ArgumentsSource(LeadFunctionTestsProvider::class)
    fun leadFunctionTests(tc: EvaluatorTestCase) = runEvaluatorTestCase(
        tc = tc.copy(targetPipeline = EvaluatorTestTarget.PARTIQL_PIPELINE),
        session = session
    )

    class LeadFunctionTestsProvider : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            EvaluatorTestCase(
                query = """
                    SELECT sp."date" as "date",
                        sp.ticker as ticker,
                        sp.price as current_price,
                        lead(sp.price) OVER (PARTITION BY sp.ticker ORDER BY sp."date") as next_price
                    FROM stock_price as sp
                """,
                expectedResult = """<<
                    { 'date': `2022-09-30`, 'ticker': 'AMZN', 'current_price': 113.00, 'next_price': 115.88},
                    { 'date': `2022-10-03`, 'ticker': 'AMZN', 'current_price': 115.88, 'next_price': 121.09},
                    { 'date': `2022-10-04`, 'ticker': 'AMZN', 'current_price': 121.09, 'next_price': NULL},
                    { 'date': `2022-09-30`, 'ticker': 'GOOG', 'current_price': 96.15, 'next_price': 99.30},
                    { 'date': `2022-10-03`, 'ticker': 'GOOG', 'current_price': 99.30, 'next_price': 101.04},
                    { 'date': `2022-10-04`, 'ticker': 'GOOG', 'current_price': 101.04, 'next_price': NULL}
                >>"""
            ),

            EvaluatorTestCase(
                query = """
                    SELECT sp."date" as "date",
                        sp.ticker as ticker,
                        sp.price as current_price,
                        lead(sp.a,1,'OUT OF PARTITION') OVER (PARTITION BY sp.ticker ORDER BY sp."date") as next_price
                    FROM stock_price as sp
                """,
                expectedResult = """<<
                    { 'date': `2022-09-30`, 'ticker': 'AMZN', 'current_price': 113.00},
                    { 'date': `2022-10-03`, 'ticker': 'AMZN', 'current_price': 115.88},
                    { 'date': `2022-10-04`, 'ticker': 'AMZN', 'current_price': 121.09, 'next_price': 'OUT OF PARTITION'},
                    { 'date': `2022-09-30`, 'ticker': 'GOOG', 'current_price': 96.15},
                    { 'date': `2022-10-03`, 'ticker': 'GOOG', 'current_price': 99.30},
                    { 'date': `2022-10-04`, 'ticker': 'GOOG', 'current_price': 101.04, 'next_price': 'OUT OF PARTITION'}
                >>"""
            ),

            EvaluatorTestCase(
                query = """
                    SELECT sp."date" as "date",
                        sp.ticker as ticker,
                        sp.price as current_price,
                        lead(sp.price) OVER (PARTITION BY sp.ticker ORDER BY sp."date") + 100 as next_price
                    FROM stock_price as sp
                """,
                expectedResult = """<<
                    { 'date': `2022-09-30`, 'ticker': 'AMZN', 'current_price': 113.00, 'next_price': 215.88},
                    { 'date': `2022-10-03`, 'ticker': 'AMZN', 'current_price': 115.88, 'next_price': 221.09},
                    { 'date': `2022-10-04`, 'ticker': 'AMZN', 'current_price': 121.09, 'next_price': NULL},
                    { 'date': `2022-09-30`, 'ticker': 'GOOG', 'current_price': 96.15, 'next_price': 199.30},
                    { 'date': `2022-10-03`, 'ticker': 'GOOG', 'current_price': 99.30, 'next_price': 201.04},
                    { 'date': `2022-10-04`, 'ticker': 'GOOG', 'current_price': 101.04, 'next_price': NULL}
                >>"""
            ),

            EvaluatorTestCase(
                query = """
                    SELECT sp."date" as "date",
                        sp.ticker as ticker,
                        sp.price as current_price,
                        lead(sp.price) OVER (PARTITION BY sp.ticker ORDER BY sp."date") as next_price
                    FROM stock_price as sp
                    ORDER BY sp."date" DESC
                """,
                expectedResult = """[
                    { 'date': `2022-10-04`, 'ticker': 'AMZN', 'current_price': 121.09, 'next_price': NULL},
                    { 'date': `2022-10-04`, 'ticker': 'GOOG', 'current_price': 101.04, 'next_price': NULL},
                    { 'date': `2022-10-03`, 'ticker': 'AMZN', 'current_price': 115.88, 'next_price': 121.09},
                    { 'date': `2022-10-03`, 'ticker': 'GOOG', 'current_price': 99.30, 'next_price': 101.04},
                    { 'date': `2022-09-30`, 'ticker': 'AMZN', 'current_price': 113.00, 'next_price': 115.88},
                    { 'date': `2022-09-30`, 'ticker': 'GOOG', 'current_price': 96.15, 'next_price': 99.30}
                ]"""
            ),

            EvaluatorTestCase(
                query = """
                    SELECT
                        month as current_month,
                        ticker as ticker,
                        avg(price) as current_month_average,
                        lead(avg(price)) OVER (PARTITION BY ticker ORDER BY month) as next_month_avg
                    FROM stock_price as sp
                    GROUP BY EXTRACT(MONTH FROM sp."date") as month, sp.ticker as ticker GROUP AS g
                """,
                expectedResult = """<<
                    { 'current_month': 9., 'ticker': 'AMZN', 'current_month_average': 113.00, 'next_month_avg': 118.485},
                    { 'current_month': 10., 'ticker': 'AMZN', 'current_month_average': 118.485, 'next_month_avg': NULL},
                    { 'current_month': 9., 'ticker': 'GOOG', 'current_month_average': 96.15, 'next_month_avg': 100.17},
                    { 'current_month': 10., 'ticker': 'GOOG', 'current_month_average': 100.17, 'next_month_avg': NULL}
                >>"""
            ),

            EvaluatorTestCase(
                query = """
                    SELECT
                       (SELECT
                            lead(sp.price) OVER (PARTITION BY sp.ticker ORDER BY sp."date") as next_price
                            FROM <<1>>
                       )
                    FROM stock_price as sp
                """,
                expectedResult = """<<
                    {'_1': <<{'next_price': NULL}>>},
                    {'_1': <<{'next_price': NULL}>>},
                    {'_1': <<{'next_price': NULL}>>},
                    {'_1': <<{'next_price': NULL}>>},
                    {'_1': <<{'next_price': NULL}>>},
                    {'_1': <<{'next_price': NULL}>>}
                >>"""
            ),

            EvaluatorTestCase(
                query = """
                    SELECT
                       lead(sp.price) OVER (PARTITION BY sp.ticker ORDER BY sp."date") as next_price,
                       (SELECT
                            lead(sp.price) OVER (PARTITION BY sp.ticker ORDER BY sp."date") as Inner_lead
                       FROM <<1>>)
                    FROM stock_price as sp
                """,
                expectedResult = """<<
                    {'next_price': 115.88, '_2': <<{'Inner_lead': NULL}>>},
                    {'next_price': 121.09, '_2': <<{'Inner_lead': NULL}>>},
                    {'next_price': NULL, '_2': <<{'Inner_lead': NULL}>>},
                    {'next_price': 99.30, '_2': <<{'Inner_lead': NULL}>>},
                    {'next_price': 101.04, '_2': <<{'Inner_lead': NULL}>>},
                    {'next_price': NULL, '_2': <<{'Inner_lead': NULL}>>}
                >>"""
            ),

            EvaluatorTestCase(
                query = """
                    SELECT a.res FROM (
                        SELECT LEAD(sp.price) OVER(PARTITION BY sp.ticker ORDER BY sp."date") AS res
                        FROM stock_price AS sp
                    ) as a
                """,
                expectedResult = """<<
                    {'res': 99.30},
                    {'res': 101.04},
                    {'res': NULL},
                    {'res': 115.88},
                    {'res': 121.09},
                    {'res': NULL}
                >>"""
            ),

            EvaluatorTestCase(
                query = """
                    SELECT LEAD(a.res, 1, 0) OVER (ORDER BY a.res NULLS FIRST) as next_res_or_zero FROM (
                        SELECT LEAD(sp.price) OVER(PARTITION BY sp.ticker ORDER BY sp."date") AS res
                        FROM stock_price AS sp
                    ) as a
                """,
                expectedResult = """<<
                    {'next_res_or_zero': NULL},
                    {'next_res_or_zero': 99.30},
                    {'next_res_or_zero': 101.04},
                    {'next_res_or_zero': 115.88},
                    {'next_res_or_zero': 121.09},
                    {'next_res_or_zero': 0}
                >>"""
            ),
        )
    }

    @ParameterizedTest
    @ArgumentsSource(MultipleFunctionTestsProvider::class)
    fun multipleFunctionTests(tc: EvaluatorTestCase) = runEvaluatorTestCase(
        tc = tc.copy(targetPipeline = EvaluatorTestTarget.PARTIQL_PIPELINE),
        session = session,
    )
    class MultipleFunctionTestsProvider : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            EvaluatorTestCase(
                query = """
                    SELECT sp."date" as "date",
                        sp.ticker as ticker,
                        sp.price as current_price,
                        lag(sp.price) OVER (PARTITION BY sp.ticker ORDER BY sp."date") as previous_price,
                        lead(sp.price) OVER (PARTITION BY sp.ticker ORDER BY sp."date") as next_price
                    FROM stock_price as sp
                """,
                expectedResult = """<<
                    { 'date': `2022-09-30`, 'ticker': 'AMZN', 'current_price': 113.00, 'previous_price': NULL, 'next_price': 115.88},
                    { 'date': `2022-10-03`, 'ticker': 'AMZN', 'current_price': 115.88, 'previous_price': 113.00, 'next_price': 121.09},
                    { 'date': `2022-10-04`, 'ticker': 'AMZN', 'current_price': 121.09, 'previous_price': 115.88, 'next_price': NULL},
                    { 'date': `2022-09-30`, 'ticker': 'GOOG', 'current_price': 96.15, 'previous_price': NULL, 'next_price': 99.30},
                    { 'date': `2022-10-03`, 'ticker': 'GOOG', 'current_price': 99.30, 'previous_price': 96.15, 'next_price': 101.04},
                    { 'date': `2022-10-04`, 'ticker': 'GOOG', 'current_price': 101.04, 'previous_price': 99.30, 'next_price': NULL}
                >>"""
            ),
            EvaluatorTestCase(
                query = """
                    SELECT 
                        LEAD(a.next_res, 1, 0) OVER (ORDER BY a."date", a.ticker) as next_res_or_zero,
                        LAG(a.prev_res, 1, 0) OVER (ORDER BY a."date", a.ticker) as prev_res_or_zero
                     FROM (
                        SELECT 
                            LEAD(sp.price) OVER(PARTITION BY sp.ticker ORDER BY sp."date") AS next_res,
                            LAG(sp.price) OVER(PARTITION BY sp.ticker ORDER BY sp."date") AS prev_res,
                            sp."date" as "date",
                            sp.ticker as ticker
                        FROM stock_price AS sp
                    ) as a
                """,
                expectedResult = """<<
                    {'next_res_or_zero': 99.30, 'prev_res_or_zero' : 0},
                    {'next_res_or_zero': 121.09, 'prev_res_or_zero' : NULL},
                    {'next_res_or_zero': 101.04, 'prev_res_or_zero' : NULL},
                    {'next_res_or_zero': NULL, 'prev_res_or_zero' : 113.00},
                    {'next_res_or_zero': NULL, 'prev_res_or_zero' : 96.15},
                    {'next_res_or_zero': 0, 'prev_res_or_zero' : 115.88}
                >>"""
            ),
        )
    }
}
