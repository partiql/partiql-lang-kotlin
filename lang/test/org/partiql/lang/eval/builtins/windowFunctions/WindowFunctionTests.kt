package org.partiql.lang.eval.builtins.windowFunctions

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.eval.EvaluatorTestBase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestAdapter
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestCase
import org.partiql.lang.eval.evaluatortestframework.MultipleTestAdapter
import org.partiql.lang.eval.evaluatortestframework.PartiQLCompilerPipelineFactory
import org.partiql.lang.eval.evaluatortestframework.PipelineEvaluatorTestAdapter
import org.partiql.lang.eval.evaluatortestframework.PlannerPipelineFactory
import org.partiql.lang.util.ArgumentsProviderBase

// TODO: Remove from experimental once https://github.com/partiql/partiql-docs/issues/31 is resolved and a RFC is approved
class WindowFunctionTests : EvaluatorTestBase() {
    // The new AST node is not supported by ExprNode
    override val testHarness: EvaluatorTestAdapter = MultipleTestAdapter(
        listOf(
            PipelineEvaluatorTestAdapter(PlannerPipelineFactory()),
            PipelineEvaluatorTestAdapter(PartiQLCompilerPipelineFactory())
        )
    )
    private val session = mapOf(
        "stock_price" to """[
            { date: 2022-09-30, ticker: AMZN, price: 113.00},
            { date: 2022-10-03, ticker: AMZN, price: 115.88},
            { date: 2022-10-04, ticker: AMZN, price: 121.09},
            { date: 2022-09-30, ticker: GOOG, price: 96.15},
            { date: 2022-10-03, ticker: GOOG, price: 99.30},
            { date: 2022-10-04, ticker: GOOG, price: 101.04}
        ]""",
    ).toSession()

    @ParameterizedTest
    @ArgumentsSource(LagFunctionTestsProvider::class)
    fun lagFunctionTests(tc: EvaluatorTestCase) = runEvaluatorTestCase(
        tc = tc,
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
                    { 'current_month': 9, 'ticker': 'AMZN', 'current_month_average': 113.00, 'previous_month_avg': NULL},
                    { 'current_month': 10, 'ticker': 'AMZN', 'current_month_average': 118.485, 'previous_month_avg': 113.00},
                    { 'current_month': 9, 'ticker': 'GOOG', 'current_month_average': 96.15, 'previous_month_avg': NULL},
                    { 'current_month': 10, 'ticker': 'GOOG', 'current_month_average': 100.17, 'previous_month_avg': 96.15}
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
        )
    }

    @ParameterizedTest
    @ArgumentsSource(LeadFunctionTestsProvider::class)
    fun leadFunctionTests(tc: EvaluatorTestCase) = runEvaluatorTestCase(
        tc = tc,
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
                    { 'current_month': 9, 'ticker': 'AMZN', 'current_month_average': 113.00, 'next_month_avg': 118.485},
                    { 'current_month': 10, 'ticker': 'AMZN', 'current_month_average': 118.485, 'next_month_avg': NULL},
                    { 'current_month': 9, 'ticker': 'GOOG', 'current_month_average': 96.15, 'next_month_avg': 100.17},
                    { 'current_month': 10, 'ticker': 'GOOG', 'current_month_average': 100.17, 'next_month_avg': NULL}
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
        )
    }

    @ParameterizedTest
    @ArgumentsSource(MultipleFunctionTestsProvider::class)
    fun multipleFunctionTests(tc: EvaluatorTestCase) = runEvaluatorTestCase(
        tc = tc,
        session = session
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
                >>""",
                excludeLegacySerializerAssertions = true
            )
        )
    }
}
