package org.partiql.eval.internal

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.eval.Environment
import org.partiql.eval.ExprRelation
import org.partiql.eval.Mode
import org.partiql.eval.Row
import org.partiql.eval.WindowFunction
import org.partiql.eval.WindowPartition
import org.partiql.eval.internal.operator.rel.Collation
import org.partiql.eval.internal.operator.rel.RelOpWindow
import org.partiql.eval.internal.operator.rex.ExprVar
import org.partiql.eval.internal.window.RowNumberFunction
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals

/**
 * This test file tests window functions/clause.
 */
class WindowTests {

    @ParameterizedTest
    @MethodSource("successTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun successTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("failureTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun failureTests(tc: FailureTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("aggregateTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun aggregateTests(tc: SuccessTestCase) = tc.run()

    /**
     * This is used just for debugging purposes.
     */
    @Test
    fun developmentTest() {
        val failingIndex = 3
        successTestCases()[failingIndex - 1].run()
    }

    /**
     * Asserts the (orderingGroupStart, orderingGroupEnd) passed to window functions by [RelOpWindow], for
     * `PARTITION BY department ORDER BY age`. Peers (same department and age) share one ordering group.
     */
    @Test
    fun orderingGroupBounds() {
        val employees = listOf(
            employee("Sales", "Ann", 25), employee("Sales", "Bob", 25), // peers
            employee("Sales", "Cat", 30),
            employee("Sales", "Dan", 35), employee("Sales", "Eve", 35), // peers
            employee("Sales", "Fay", 40), employee("Sales", "Gus", 40), employee("Sales", "Hal", 40), // peers
            employee("Marketing", "Ivy", 28), // single-row partition
            employee("Research", "Jay", 33), employee("Research", "Kim", 33), // every row ties
        )
        assertEquals(
            listOf(
                "Ann" to 0L..1L, "Bob" to 0L..1L,
                "Cat" to 2L..2L,
                "Dan" to 3L..4L, "Eve" to 3L..4L,
                "Fay" to 5L..7L, "Gus" to 5L..7L, "Hal" to 5L..7L,
                "Ivy" to 0L..0L,
                "Jay" to 0L..1L, "Kim" to 0L..1L,
            ),
            orderingGroupBounds(employees, listOf(Collation(AGE, desc = false, last = false)))
        )
        // Without ORDER BY, the whole partition is a single peer group.
        assertEquals(
            listOf(
                "Ann" to 0L..7L, "Bob" to 0L..7L, "Cat" to 0L..7L, "Dan" to 0L..7L,
                "Eve" to 0L..7L, "Fay" to 0L..7L, "Gus" to 0L..7L, "Hal" to 0L..7L,
                "Ivy" to 0L..0L,
                "Jay" to 0L..1L, "Kim" to 0L..1L,
            ),
            orderingGroupBounds(employees, emptyList())
        )
    }

    /**
     * @return each employee's name paired with the ordering group bounds [RelOpWindow] passed for that row.
     */
    private fun orderingGroupBounds(employees: List<Row>, sortBy: List<Collation>): List<Pair<String, LongRange>> {
        val recorder = object : WindowFunction {
            override fun reset(partition: WindowPartition) = Unit
            override fun eval(env: Environment, orderingGroupStart: Long, orderingGroupEnd: Long): Datum =
                Datum.array(listOf(Datum.bigint(orderingGroupStart), Datum.bigint(orderingGroupEnd)))
        }
        val window = RelOpWindow(relationOf(employees), listOf(recorder), listOf(DEPARTMENT), sortBy)
        window.open(Environment())
        val result = mutableListOf<Pair<String, LongRange>>()
        while (window.hasNext()) {
            // Output row is the input (department, name, age) followed by the recorder's result.
            val row = window.next().values
            val bounds = row[3].toList()
            // row[1] is the employee's name; bounds[0] is orderingGroupStart and bounds[1] is orderingGroupEnd.
            result.add(row[1].string to bounds[0].long..bounds[1].long)
        }
        window.close()
        return result
    }

    /**
     * To find the end of the Marketing partition, [RelOpWindow] reads Ann (Sales) ahead. Closing it before Ann is
     * emitted must not leak Ann into the start of the next run.
     */
    @Test
    fun reopenAfterPartialConsumption() {
        val window = RelOpWindow(relationOf(marketingAndSales), listOf(RowNumberFunction()), listOf(DEPARTMENT), emptyList())
        window.open(Environment())
        window.next() // Ivy; Ann has been read ahead
        window.close()

        window.open(Environment())
        val result = mutableListOf<Pair<String, Long>>()
        while (window.hasNext()) {
            val row = window.next().values
            result.add(row[1].string to row[3].long)
        }
        window.close()
        assertEquals(listOf("Ivy" to 1L, "Max" to 2L, "Ann" to 1L), result)
    }

    /**
     * Closing [RelOpWindow] must release the buffered partition, including the window functions' references to it.
     */
    @Test
    fun closeReleasesPartition() {
        var lastPartition: WindowPartition? = null
        val recorder = object : WindowFunction {
            override fun reset(partition: WindowPartition) { lastPartition = partition }
            override fun eval(env: Environment, orderingGroupStart: Long, orderingGroupEnd: Long): Datum = Datum.bigint(0)
        }
        val window = RelOpWindow(relationOf(marketingAndSales), listOf(recorder), listOf(DEPARTMENT), emptyList())
        window.open(Environment())
        window.next() // Ivy
        assertEquals(2L, lastPartition!!.size()) // Marketing: Ivy, Max
        window.close()
        assertEquals(0L, lastPartition!!.size())
    }

    /**
     * Rows (department, name, age), already grouped by department as [RelOpWindow] expects.
     */
    private val marketingAndSales = listOf(
        employee("Marketing", "Ivy", 28),
        employee("Marketing", "Max", 32),
        employee("Sales", "Ann", 25),
    )

    private fun employee(department: String, name: String, age: Int): Row =
        Row.of(Datum.string(department), Datum.string(name), Datum.integer(age))

    /**
     * A re-openable relation over [rows].
     */
    private fun relationOf(rows: List<Row>): ExprRelation = object : ExprRelation {
        private lateinit var iterator: Iterator<Row>
        override fun open(env: Environment) { iterator = rows.iterator() }
        override fun hasNext(): Boolean = iterator.hasNext()
        override fun next(): Row = iterator.next()
        override fun close() = Unit
    }

    companion object {

        private val DEPARTMENT = ExprVar(0, 0)
        private val AGE = ExprVar(0, 2)

        private class Employee(
            val id: Int,
            val name: String,
            val department: String,
            val age: Int,
            val partner: String?
        ) {
            fun toDatum(): Datum {
                val fields = listOfNotNull(
                    Field.of("id", Datum.integer(id)),
                    Field.of("name", Datum.string(name)),
                    Field.of("department", Datum.string(department)),
                    Field.of("age", Datum.integer(age)),
                    partner?.let { Field.of("partner", Datum.string(it)) }
                )
                return Datum.struct(fields)
            }
        }

        private class StockPrice(
            val year: Int,
            val month: Int,
            val day: Int,
            val ticker: String,
            val price: Double,
        ) {
            fun toDatum(): Datum {
                val fields = listOfNotNull(
                    Field.of("_date", Datum.date(LocalDate.of(year, month, day))),
                    Field.of("ticker", Datum.string(ticker)),
                    Field.of("price", Datum.doublePrecision(price)),
                )
                return Datum.struct(fields)
            }
        }

        private val stock_prices = listOf(
            StockPrice(2025, 9, 30, "AMZN", 113.00),
            StockPrice(2025, 10, 4, "AMZN", 121.09),
            StockPrice(2025, 9, 30, "GOOG", 96.15),
            StockPrice(2025, 10, 3, "GOOG", 99.30),
            StockPrice(2025, 10, 4, "GOOG", 101.04)
        )

        private val employees = listOf(
            Employee(0, "Jacob", "Marketing", 40, "Alexa"),
            Employee(1, "Marcus", "Sales", 28, "Gary"),
            Employee(2, "Shelly", "Research", 35, "Michael"),
            Employee(3, "Alexa", "Research", 30, null),
            Employee(4, "Raghavan", "Sales", 28, "Yi"),
            Employee(5, "Yi", "Research", 25, "Raghavan"),
            Employee(6, "Megan", "Marketing", 32, null),
            Employee(7, "Amanda", "Research", 32, null),
            Employee(8, "Samantha", "Sales", 29, "Mason"),
            Employee(9, "Mason", "Research", 30, "Samantha")
        )

        /**
         * Readings (id, grp, v) where v may be null. Group 'b' contains only nulls.
         */
        private val readings = listOf(
            Triple(1, "a", 10),
            Triple(2, "a", null),
            Triple(3, "a", null),
            Triple(4, "a", 40),
            Triple(5, "a", 50),
            Triple(6, "a", null),
            Triple(7, "b", null),
            Triple(8, "b", null),
        )

        /**
         * Rows (g, k, x). Partition 'a' has a tie on k = 2.
         */
        private val nums = listOf(
            Triple("a", 1, 2),
            Triple("a", 2, 4),
            Triple("a", 2, 6),
            Triple("a", 3, 8),
            Triple("b", 1, 10),
        )

        private fun intOrNull(v: Int?): Datum = v?.let { Datum.integer(it) } ?: Datum.nullValue(PType.integer())

        private val globals = listOf(
            Global(
                name = "employee",
                value = Datum.bag(employees.map { it.toDatum() })
            ),
            Global(
                name = "readings",
                value = Datum.bag(
                    readings.map { (id, grp, v) ->
                        Datum.struct(
                            Field.of("id", Datum.integer(id)),
                            Field.of("grp", Datum.string(grp)),
                            Field.of("v", intOrNull(v)),
                        )
                    }
                )
            ),
            Global(
                name = "stock_prices",
                value = Datum.bag(stock_prices.map { it.toDatum() })
            ),
            Global(
                name = "nums",
                value = Datum.bag(
                    nums.map { (g, k, x) ->
                        Datum.struct(Field.of("g", Datum.string(g)), Field.of("k", Datum.integer(k)), Field.of("x", Datum.integer(x)))
                    }
                )
            ),
            Global(
                name = "sparse",
                value = Datum.bagVararg(
                    // `v` is MISSING for id 2 and NULL for id 3
                    Datum.struct(Field.of("id", Datum.integer(1)), Field.of("v", Datum.integer(1))),
                    Datum.struct(Field.of("id", Datum.integer(2))),
                    Datum.struct(Field.of("id", Datum.integer(3)), Field.of("v", Datum.nullValue())),
                    Datum.struct(Field.of("id", Datum.integer(4)), Field.of("v", Datum.integer(4))),
                )
            ),
        )

        private const val FALLBACK: String = "UNKNOWN"

        @JvmStatic
        fun successTestCases() = listOf(
            SuccessTestCase(
                name = "Simplest window with all functions",
                mode = Mode.STRICT(),
                globals = globals,
                input = """
                    SELECT
                        t.id AS _id,
                        t.name AS _name,
                        RANK() OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _rank,
                        DENSE_RANK() OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _dense_rank,
                        ROW_NUMBER() OVER (PARTITION BY t.department ORDER BY t.age, t.name) as _row_number,
                        LAG(t.name, 1, '$FALLBACK') OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _lag,
                        LEAD(t.name, 1, '$FALLBACK') OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _lead
                    FROM employee AS t;
                """.trimIndent(),
                expected = Datum.bagVararg(
                    rowOf(6, 1, 1, 1, null, 0),
                    rowOf(0, 2, 2, 2, 6, null),
                    rowOf(5, 1, 1, 1, null, 3),
                    rowOf(3, 2, 2, 2, 5, 9),
                    rowOf(9, 3, 3, 3, 3, 7),
                    rowOf(7, 4, 4, 4, 9, 2),
                    rowOf(2, 5, 5, 5, 7, null),
                    rowOf(1, 1, 1, 1, null, 4),
                    rowOf(4, 2, 2, 2, 1, 8),
                    rowOf(8, 3, 3, 3, 4, null),
                ),
            ),

            SuccessTestCase(
                name = "Simplest window with all functions and referencing window",
                mode = Mode.STRICT(),
                globals = globals,
                input = """
                    SELECT
                        t.id AS _id,
                        t.name AS _name,
                        RANK() OVER _w AS _rank,
                        DENSE_RANK() OVER _w AS _dense_rank,
                        ROW_NUMBER() OVER _w as _row_number,
                        LAG(t.name, 1, '$FALLBACK') OVER _w AS _lag,
                        LEAD(t.name, 1, '$FALLBACK') OVER _w AS _lead
                    FROM employee AS t
                    WINDOW _w AS (PARTITION BY t.department ORDER BY t.age, t.name);
                """.trimIndent(),
                expected = Datum.bagVararg(
                    rowOf(6, 1, 1, 1, null, 0),
                    rowOf(0, 2, 2, 2, 6, null),
                    rowOf(5, 1, 1, 1, null, 3),
                    rowOf(3, 2, 2, 2, 5, 9),
                    rowOf(9, 3, 3, 3, 3, 7),
                    rowOf(7, 4, 4, 4, 9, 2),
                    rowOf(2, 5, 5, 5, 7, null),
                    rowOf(1, 1, 1, 1, null, 4),
                    rowOf(4, 2, 2, 2, 1, 8),
                    rowOf(8, 3, 3, 3, 4, null),
                ),
            ),
            SuccessTestCase(
                name = """
                    Simplest window with all functions and referencing window.
                    This has indeterministic behavior when the sort has multiple of the same value.
                    There is no secondary, unique sort key.
                    This may break based on the implementation of the window itself. For now, this proves that the
                    rank and dense rank act appropriately.
                """.trimIndent(),
                mode = Mode.STRICT(),
                globals = globals,
                input = """
                    SELECT
                        t.id AS _id,
                        t.name AS _name,
                        RANK() OVER _w AS _rank,
                        DENSE_RANK() OVER _w AS _dense_rank,
                        ROW_NUMBER() OVER _w as _row_number,
                        LAG(t.name, 1, '$FALLBACK') OVER _w AS _lag,
                        LEAD(t.name, 1, '$FALLBACK') OVER _w AS _lead
                    FROM employee AS t
                    WINDOW _w AS (PARTITION BY t.department ORDER BY t.age);
                """.trimIndent(),
                expected = Datum.bagVararg(
                    rowOf(6, 1, 1, 1, null, 0),
                    rowOf(0, 2, 2, 2, 6, null),
                    rowOf(5, 1, 1, 1, null, 3),
                    rowOf(3, 2, 2, 2, 5, 9),
                    rowOf(9, 2, 2, 3, 3, 7),
                    rowOf(7, 4, 3, 4, 9, 2),
                    rowOf(2, 5, 4, 5, 7, null),
                    rowOf(1, 1, 1, 1, null, 4),
                    rowOf(4, 1, 1, 2, 1, 8),
                    rowOf(8, 3, 2, 3, 4, null),
                ),
            ),
            SuccessTestCase(
                name = "Window highlighting the referencing of two differently sorted windows",
                mode = Mode.STRICT(),
                globals = globals,
                input = """
                    SELECT
                        t.id AS _id,
                        t.name AS _name,
                        RANK() OVER _w1 AS _rank_1,
                        RANK() OVER _w2 AS _rank_2,
                        DENSE_RANK() OVER _w1 AS _dense_rank_1,
                        DENSE_RANK() OVER _w2 AS _dense_rank_2,
                        ROW_NUMBER() OVER _w1 as _row_number_1,
                        ROW_NUMBER() OVER _w2 as _row_number_2,
                        LAG(t.name, 1, 'UNKNOWN') OVER _w1 AS _lag_1,
                        LAG(t.name, 1, 'UNKNOWN') OVER _w2 AS _lag_2,
                        LEAD(t.name, 1, 'UNKNOWN') OVER _w1 AS _lead_1,
                        LEAD(t.name, 1, 'UNKNOWN') OVER _w2 AS _lead_2
                    FROM employee AS t
                    WINDOW
                        _w1 AS (PARTITION BY t.department ORDER BY t.age, t.name),
                        _w2 AS (PARTITION BY t.department ORDER BY t.age DESC, t.name DESC)
                    ;
                """.trimIndent(),
                expected = Datum.bagVararg(
                    rowOfDoubleRef(6, 1, 2, 1, 2, 1, 2, null, 0, 0, null),
                    rowOfDoubleRef(0, 2, 1, 2, 1, 2, 1, 6, null, null, 6),
                    rowOfDoubleRef(5, 1, 5, 1, 5, 1, 5, null, 3, 3, null),
                    rowOfDoubleRef(3, 2, 4, 2, 4, 2, 4, 5, 9, 9, 5),
                    rowOfDoubleRef(9, 3, 3, 3, 3, 3, 3, 3, 7, 7, 3),
                    rowOfDoubleRef(7, 4, 2, 4, 2, 4, 2, 9, 2, 2, 9),
                    rowOfDoubleRef(2, 5, 1, 5, 1, 5, 1, 7, null, null, 7),
                    rowOfDoubleRef(1, 1, 3, 1, 3, 1, 3, null, 4, 4, null),
                    rowOfDoubleRef(4, 2, 2, 2, 2, 2, 2, 1, 8, 8, 1),
                    rowOfDoubleRef(8, 3, 1, 3, 1, 3, 1, 4, null, null, 4),
                ),
            ),
            SuccessTestCase(
                name = "Lead/Lag with more than 1",
                mode = Mode.STRICT(),
                globals = globals,
                input = """
                    SELECT
                        t.id AS _id,
                        t.name AS _name,
                        RANK() OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _rank,
                        DENSE_RANK() OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _dense_rank,
                        ROW_NUMBER() OVER (PARTITION BY t.department ORDER BY t.age, t.name) as _row_number,
                        LAG(t.name, 3, '$FALLBACK') OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _lag,
                        LEAD(t.name, 3, '$FALLBACK') OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _lead
                    FROM employee AS t;
                """.trimIndent(),
                expected = Datum.bagVararg(
                    rowOf(6, 1, 1, 1, null, null),
                    rowOf(0, 2, 2, 2, null, null),
                    rowOf(5, 1, 1, 1, null, 7),
                    rowOf(3, 2, 2, 2, null, 2),
                    rowOf(9, 3, 3, 3, null, null),
                    rowOf(7, 4, 4, 4, 5, null),
                    rowOf(2, 5, 5, 5, 3, null),
                    rowOf(1, 1, 1, 1, null, null),
                    rowOf(4, 2, 2, 2, null, null),
                    rowOf(8, 3, 3, 3, null, null),
                ),
            ),
            SuccessTestCase(
                name = "Lag and lead referencing sometimes missing attr (partner)",
                mode = Mode.PERMISSIVE(),
                globals = globals,
                input = """
                    SELECT
                        t.id AS _id,
                        t.name AS _name,
                        RANK() OVER _w AS _rank,
                        DENSE_RANK() OVER _w AS _dense_rank,
                        ROW_NUMBER() OVER _w as _row_number,
                        LAG(t.partner, 1, '$FALLBACK') OVER _w AS _lag,
                        LEAD(t.partner, 1, '$FALLBACK') OVER _w AS _lead
                    FROM employee AS t
                    WINDOW _w AS (PARTITION BY t.department ORDER BY t.age, t.name);
                """.trimIndent(),
                expected = Datum.bagVararg(
                    rowOfPartner(6, 1, 1, 1, FALLBACK, "Alexa"),
                    rowOfPartner(0, 2, 2, 2, null, FALLBACK),
                    rowOfPartner(5, 1, 1, 1, FALLBACK, null),
                    rowOfPartner(3, 2, 2, 2, "Raghavan", "Samantha"),
                    rowOfPartner(9, 3, 3, 3, null, null),
                    rowOfPartner(7, 4, 4, 4, "Samantha", "Michael"),
                    rowOfPartner(2, 5, 5, 5, null, FALLBACK),
                    rowOfPartner(1, 1, 1, 1, FALLBACK, "Yi"),
                    rowOfPartner(4, 2, 2, 2, "Gary", "Mason"),
                    rowOfPartner(8, 3, 3, 3, "Yi", FALLBACK),
                ),
            ),
            SuccessTestCase(
                name = "With group by",
                mode = Mode.PERMISSIVE(),
                globals = globals,
                input = """
                    SELECT
                        _month AS current_month,
                        ticker AS ticker,
                        AVG(price) AS current_month_average,
                        LAG(AVG(price)) OVER (PARTITION BY ticker ORDER BY _month) AS previous_month_avg
                    FROM stock_prices AS sp
                    GROUP BY
                        EXTRACT(MONTH FROM sp._date) AS _month,
                        sp.ticker AS ticker
                    GROUP AS g;
                """.trimIndent(),
                expected = Datum.bagVararg(
                    rowOfStockPrice(9, "AMZN", 113.00, null),
                    rowOfStockPrice(10, "AMZN", 121.09, 113.00),
                    rowOfStockPrice(9, "GOOG", 96.15, null),
                    rowOfStockPrice(10, "GOOG", 100.17, 96.15),
                ),
            ),
            SuccessTestCase(
                name = "With subquery",
                mode = Mode.PERMISSIVE(),
                globals = globals,
                input = """
                    SELECT
                        LAG(sp.price) OVER (PARTITION BY sp.ticker ORDER BY sp._date) AS previous_price,
                        (
                            SELECT
                                LAG(sp.price) OVER (PARTITION BY sp.ticker ORDER BY sp._date) AS inner_lag
                            FROM <<1>>
                       ) AS nested
                    FROM stock_prices AS sp
                """.trimIndent(),
                expected = Datum.bagVararg(
                    rowOfStockPrice(null),
                    rowOfStockPrice(113.0),
                    rowOfStockPrice(null),
                    rowOfStockPrice(96.15),
                    rowOfStockPrice(99.30),
                ),
            ),
            SuccessTestCase(
                name = "With order by",
                mode = Mode.PERMISSIVE(),
                globals = globals,
                input = """
                    SELECT sp._date AS _date,
                        sp.ticker AS ticker,
                        sp.price AS current_price,
                        LAG(sp.price) OVER (PARTITION BY sp.ticker ORDER BY sp._date) AS previous_price
                    FROM stock_prices AS sp
                    ORDER BY sp._date DESC;
                """.trimIndent(),
                expected = Datum.array(
                    listOf(
                        rowOfStockPrice(2025, 10, 4, "AMZN", 121.09, 113.0),
                        rowOfStockPrice(2025, 10, 4, "GOOG", 101.04, 99.3),
                        rowOfStockPrice(2025, 10, 3, "GOOG", 99.3, 96.15),
                        rowOfStockPrice(2025, 9, 30, "AMZN", 113.0, null),
                        rowOfStockPrice(2025, 9, 30, "GOOG", 96.15, null),
                    )
                ),
            ),
            SuccessTestCase(
                name = "LAG/LEAD IGNORE NULLS with offset 1 and 2",
                mode = Mode.STRICT(),
                globals = globals,
                input = """
                    SELECT
                        r.id AS id,
                        LAG(r.v, 1, -1) IGNORE NULLS OVER _w AS lag1,
                        LAG(r.v, 2, -1) IGNORE NULLS OVER _w AS lag2,
                        LEAD(r.v, 1, -1) IGNORE NULLS OVER _w AS lead1,
                        LEAD(r.v, 2, -1) IGNORE NULLS OVER _w AS lead2
                    FROM readings AS r
                    WINDOW _w AS (PARTITION BY r.grp ORDER BY r.id);
                """.trimIndent(),
                expected = Datum.bagVararg(
                    rowOfNav(1, -1, -1, 40, 50),
                    rowOfNav(2, 10, -1, 40, 50),
                    rowOfNav(3, 10, -1, 40, 50),
                    rowOfNav(4, 10, -1, 50, -1),
                    rowOfNav(5, 40, 10, -1, -1),
                    rowOfNav(6, 50, 40, -1, -1),
                    rowOfNav(7, -1, -1, -1, -1),
                    rowOfNav(8, -1, -1, -1, -1),
                ),
            ),
            SuccessTestCase(
                name = "LAG/LEAD IGNORE NULLS skip MISSING as well as NULL",
                mode = Mode.PERMISSIVE(),
                input = """
                    SELECT
                        r.id AS id,
                        LAG(r.v, 1, -1) IGNORE NULLS OVER _w AS lag1,
                        LAG(r.v, 2, -1) IGNORE NULLS OVER _w AS lag2,
                        LEAD(r.v, 1, -1) IGNORE NULLS OVER _w AS lead1,
                        LEAD(r.v, 2, -1) IGNORE NULLS OVER _w AS lead2
                    FROM << {'id': 1, 'v': 10}, {'id': 2, 'v': 20}, {'id': 3}, {'id': 4, 'v': NULL}, {'id': 5, 'v': 50} >> AS r
                    WINDOW _w AS (ORDER BY r.id);
                """.trimIndent(),
                expected = Datum.bagVararg(
                    rowOfNav(1, -1, -1, 20, 50),
                    rowOfNav(2, 10, -1, 50, -1),
                    rowOfNav(3, 20, 10, 50, -1),
                    rowOfNav(4, 20, 10, 50, -1),
                    rowOfNav(5, 20, 10, -1, -1),
                ),
            ),
            SuccessTestCase(
                name = "LAG/LEAD IGNORE NULLS with offset 0 and offset beyond qualifying rows",
                mode = Mode.STRICT(),
                globals = globals,
                input = """
                    SELECT
                        r.id AS id,
                        LAG(r.v, 0, -1) IGNORE NULLS OVER _w AS lag1,
                        LAG(r.v, 4, -1) IGNORE NULLS OVER _w AS lag2,
                        LEAD(r.v, 0, -1) IGNORE NULLS OVER _w AS lead1,
                        LEAD(r.v, 3, -1) IGNORE NULLS OVER _w AS lead2
                    FROM readings AS r
                    WINDOW _w AS (PARTITION BY r.grp ORDER BY r.id);
                """.trimIndent(),
                expected = Datum.bagVararg(
                    rowOfNav(1, 10, -1, 10, -1),
                    rowOfNav(2, null, -1, null, -1),
                    rowOfNav(3, null, -1, null, -1),
                    rowOfNav(4, 40, -1, 40, -1),
                    rowOfNav(5, 50, -1, 50, -1),
                    rowOfNav(6, null, -1, null, -1),
                    rowOfNav(7, null, -1, null, -1),
                    rowOfNav(8, null, -1, null, -1),
                ),
            ),
            SuccessTestCase(
                name = "LAG/LEAD RESPECT NULLS (explicit and default) do not skip nulls",
                mode = Mode.STRICT(),
                globals = globals,
                input = """
                    SELECT
                        r.id AS id,
                        LAG(r.v, 1, -1) RESPECT NULLS OVER _w AS lag1,
                        LAG(r.v, 1, -1) OVER _w AS lag2,
                        LEAD(r.v, 1, -1) RESPECT NULLS OVER _w AS lead1,
                        LEAD(r.v, 1, -1) OVER _w AS lead2
                    FROM readings AS r
                    WINDOW _w AS (PARTITION BY r.grp ORDER BY r.id);
                """.trimIndent(),
                expected = Datum.bagVararg(
                    rowOfNav(1, -1, -1, null, null),
                    rowOfNav(2, 10, 10, null, null),
                    rowOfNav(3, null, null, 40, 40),
                    rowOfNav(4, null, null, 50, 50),
                    rowOfNav(5, 40, 40, null, null),
                    rowOfNav(6, 50, 50, -1, -1),
                    rowOfNav(7, -1, -1, null, null),
                    rowOfNav(8, null, null, -1, -1),
                ),
            ),
        )

        private fun rowOfNav(id: Int, lag1: Int?, lag2: Int?, lead1: Int?, lead2: Int?): Datum {
            return Datum.struct(
                Field.of("id", Datum.integer(id)),
                Field.of("lag1", intOrNull(lag1)),
                Field.of("lag2", intOrNull(lag2)),
                Field.of("lead1", intOrNull(lead1)),
                Field.of("lead2", intOrNull(lead2)),
            )
        }

        /**
         * @param id The employee's id
         * @param rank The employee's rank within their department
         * @param denseRank The employee's dense rank within their department
         * @param rowNumber The employee's row number within their department
         * @param lag The index of the employee's name from the previous row within their department
         * @param lead The index of the employee's name from the next row within their department
         */
        private fun rowOf(id: Int, rank: Long, denseRank: Long, rowNumber: Long, lag: Int?, lead: Int?): Datum {
            return Datum.struct(
                Field.of("_id", Datum.integer(id)),
                Field.of("_name", Datum.string(employees[id].name)),
                Field.of("_rank", Datum.bigint(rank)),
                Field.of("_dense_rank", Datum.bigint(denseRank)),
                Field.of("_row_number", Datum.bigint(rowNumber)),
                Field.of("_lag", Datum.string(lag?.let { employees[it].name } ?: FALLBACK)),
                Field.of("_lead", Datum.string(lead?.let { employees[it].name } ?: FALLBACK))
            )
        }

        /**
         * @param id The employee's id
         * @param rank The employee's rank within their department
         * @param denseRank1 The employee's dense rank within their department
         * @param rowNumber The employee's row number within their department
         * @param lag The index of the employee's name from the previous row within their department
         * @param lead The index of the employee's name from the next row within their department
         */
        private fun rowOfDoubleRef(
            id: Int,
            rank1: Long,
            rank2: Long,
            denseRank1: Long,
            denseRank2: Long,
            rowNumber1: Long,
            rowNumber2: Long,
            lag1: Int?,
            lag2: Int?,
            lead1: Int?,
            lead2: Int?,
        ): Datum {
            return Datum.struct(
                Field.of("_id", Datum.integer(id)),
                Field.of("_name", Datum.string(employees[id].name)),
                Field.of("_rank_1", Datum.bigint(rank1)),
                Field.of("_rank_2", Datum.bigint(rank2)),
                Field.of("_dense_rank_1", Datum.bigint(denseRank1)),
                Field.of("_dense_rank_2", Datum.bigint(denseRank2)),
                Field.of("_row_number_1", Datum.bigint(rowNumber1)),
                Field.of("_row_number_2", Datum.bigint(rowNumber2)),
                Field.of("_lag_1", Datum.string(lag1?.let { employees[it].name } ?: FALLBACK)),
                Field.of("_lag_2", Datum.string(lag2?.let { employees[it].name } ?: FALLBACK)),
                Field.of("_lead_1", Datum.string(lead1?.let { employees[it].name } ?: FALLBACK)),
                Field.of("_lead_2", Datum.string(lead2?.let { employees[it].name } ?: FALLBACK))
            )
        }

        /**
         * @param id The employee's id
         * @param rank The employee's rank within their department
         * @param denseRank The employee's dense rank within their department
         * @param rowNumber The employee's row number within their department
         * @param lag The index of the employee's partner from the previous row within their department
         * @param lead The index of the employee's partner from the next row within their department
         */
        private fun rowOfPartner(id: Int, rank: Long, denseRank: Long, rowNumber: Long, lag: String?, lead: String?): Datum {
            val fields = listOfNotNull(
                Field.of("_id", Datum.integer(id)),
                Field.of("_name", Datum.string(employees[id].name)),
                Field.of("_rank", Datum.bigint(rank)),
                Field.of("_dense_rank", Datum.bigint(denseRank)),
                Field.of("_row_number", Datum.bigint(rowNumber)),
                lag?.let { Field.of("_lag", Datum.string(it)) },
                lead?.let { Field.of("_lead", Datum.string(it)) },
            )
            return Datum.struct(fields)
        }

        private fun rowOfStockPrice(month: Int, ticker: String, avg: Double, prevAvg: Double?): Datum {
            val fields = mutableListOf(
                Field.of("current_month", Datum.integer(month)),
                Field.of("ticker", Datum.string(ticker)),
                Field.of("current_month_average", Datum.doublePrecision(avg)),
            )
            val last = when (prevAvg) {
                null -> Field.of("previous_month_avg", Datum.nullValue(PType.doublePrecision()))
                else -> Field.of("previous_month_avg", Datum.doublePrecision(prevAvg))
            }
            fields.add(last)
            return Datum.struct(fields)
        }

        private fun rowOfStockPrice(prevPrice: Double?): Datum {
            val fields = mutableListOf<Field>()
            val first = when (prevPrice) {
                null -> Field.of("previous_price", Datum.nullValue(PType.doublePrecision()))
                else -> Field.of("previous_price", Datum.doublePrecision(prevPrice))
            }
            fields.add(first)
            fields.add(Field.of("nested", Datum.nullValue(PType.doublePrecision())),)
            return Datum.struct(fields)
        }

        private fun rowOfStockPrice(
            year: Int,
            month: Int,
            day: Int,
            ticker: String,
            currentPrice: Double,
            prevPrice: Double?
        ): Datum {
            val fields = mutableListOf(
                Field.of("_date", Datum.date(LocalDate.of(year, month, day))),
                Field.of("ticker", Datum.string(ticker)),
                Field.of("current_price", Datum.doublePrecision(currentPrice)),
            )
            val last = when (prevPrice) {
                null -> Field.of("previous_price", Datum.nullValue(PType.doublePrecision()))
                else -> Field.of("previous_price", Datum.doublePrecision(prevPrice))
            }
            fields.add(last)
            return Datum.struct(fields)
        }

        /**
         * Creates a struct from (name, value) pairs. Long -> BIGINT, Int -> INT, BigDecimal -> DECIMAL, String -> STRING,
         * null -> NULL.
         */
        private fun row(vararg fields: Pair<String, Any?>): Datum = Datum.struct(
            fields.map { (k, v) ->
                val value = when (v) {
                    null -> Datum.nullValue()
                    is Long -> Datum.bigint(v)
                    is Int -> Datum.integer(v)
                    is BigDecimal -> Datum.decimal(v)
                    is String -> Datum.string(v)
                    is Datum -> v
                    else -> error("Unsupported value $v")
                }
                Field.of(k, value)
            }
        )

        private fun dec(v: Long): BigDecimal = BigDecimal.valueOf(v)

        @JvmStatic
        fun aggregateTestCases() = listOf(Mode.STRICT(), Mode.PERMISSIVE()).flatMap { mode ->
            listOf(
                SuccessTestCase(
                    name = "Running aggregates with ORDER BY; peers share the value ($mode)",
                    mode = mode,
                    globals = globals,
                    input = """
                        SELECT
                            t.id AS id,
                            SUM(t.age) OVER (PARTITION BY t.department ORDER BY t.age) AS s,
                            COUNT(*) OVER (PARTITION BY t.department ORDER BY t.age) AS c,
                            MIN(t.age) OVER (PARTITION BY t.department ORDER BY t.age) AS mn,
                            MAX(t.age) OVER (PARTITION BY t.department ORDER BY t.age) AS mx
                        FROM employee AS t
                    """.trimIndent(),
                    expected = Datum.bagVararg(
                        // Research: 25(5), 30(3), 30(9), 32(7), 35(2)
                        row("id" to 5, "s" to 25L, "c" to 1L, "mn" to 25, "mx" to 25),
                        row("id" to 3, "s" to 85L, "c" to 3L, "mn" to 25, "mx" to 30),
                        row("id" to 9, "s" to 85L, "c" to 3L, "mn" to 25, "mx" to 30),
                        row("id" to 7, "s" to 117L, "c" to 4L, "mn" to 25, "mx" to 32),
                        row("id" to 2, "s" to 152L, "c" to 5L, "mn" to 25, "mx" to 35),
                        // Sales: 28(1), 28(4), 29(8)
                        row("id" to 1, "s" to 56L, "c" to 2L, "mn" to 28, "mx" to 28),
                        row("id" to 4, "s" to 56L, "c" to 2L, "mn" to 28, "mx" to 28),
                        row("id" to 8, "s" to 85L, "c" to 3L, "mn" to 28, "mx" to 29),
                        // Marketing: 32(6), 40(0)
                        row("id" to 6, "s" to 32L, "c" to 1L, "mn" to 32, "mx" to 32),
                        row("id" to 0, "s" to 72L, "c" to 2L, "mn" to 32, "mx" to 40),
                    ),
                ),
                SuccessTestCase(
                    name = "Running aggregates with ORDER BY DESC ($mode)",
                    mode = mode,
                    globals = globals,
                    input = """
                        SELECT
                            t.id AS id,
                            SUM(t.age) OVER (PARTITION BY t.department ORDER BY t.age DESC) AS s,
                            MIN(t.age) OVER (PARTITION BY t.department ORDER BY t.age DESC) AS mn
                        FROM employee AS t
                    """.trimIndent(),
                    expected = Datum.bagVararg(
                        row("id" to 2, "s" to 35L, "mn" to 35),
                        row("id" to 7, "s" to 67L, "mn" to 32),
                        row("id" to 3, "s" to 127L, "mn" to 30),
                        row("id" to 9, "s" to 127L, "mn" to 30),
                        row("id" to 5, "s" to 152L, "mn" to 25),
                        row("id" to 8, "s" to 29L, "mn" to 29),
                        row("id" to 1, "s" to 85L, "mn" to 28),
                        row("id" to 4, "s" to 85L, "mn" to 28),
                        row("id" to 0, "s" to 40L, "mn" to 40),
                        row("id" to 6, "s" to 72L, "mn" to 32),
                    ),
                ),
                SuccessTestCase(
                    name = "Aggregates without ORDER BY are over the whole partition ($mode)",
                    mode = mode,
                    globals = globals,
                    input = """
                        SELECT
                            t.id AS id,
                            SUM(t.age) OVER (PARTITION BY t.department) AS s,
                            COUNT(t.age) OVER (PARTITION BY t.department) AS c,
                            MAX(t.name) OVER (PARTITION BY t.department) AS mx,
                            COUNT(*) OVER () AS total
                        FROM employee AS t
                    """.trimIndent(),
                    expected = Datum.bagVararg(
                        row("id" to 2, "s" to 152L, "c" to 5L, "mx" to "Yi", "total" to 10L),
                        row("id" to 3, "s" to 152L, "c" to 5L, "mx" to "Yi", "total" to 10L),
                        row("id" to 5, "s" to 152L, "c" to 5L, "mx" to "Yi", "total" to 10L),
                        row("id" to 7, "s" to 152L, "c" to 5L, "mx" to "Yi", "total" to 10L),
                        row("id" to 9, "s" to 152L, "c" to 5L, "mx" to "Yi", "total" to 10L),
                        row("id" to 1, "s" to 85L, "c" to 3L, "mx" to "Samantha", "total" to 10L),
                        row("id" to 4, "s" to 85L, "c" to 3L, "mx" to "Samantha", "total" to 10L),
                        row("id" to 8, "s" to 85L, "c" to 3L, "mx" to "Samantha", "total" to 10L),
                        row("id" to 0, "s" to 72L, "c" to 2L, "mx" to "Megan", "total" to 10L),
                        row("id" to 6, "s" to 72L, "c" to 2L, "mx" to "Megan", "total" to 10L),
                    ),
                ),
                SuccessTestCase(
                    name = "AVG/SUM/MIN/MAX/COUNT with ties and multiple partitions ($mode)",
                    mode = mode,
                    globals = globals,
                    input = """
                        SELECT
                            t.g AS g,
                            t.x AS x,
                            SUM(t.x) OVER w AS s,
                            AVG(t.x) OVER w AS a,
                            MIN(t.x) OVER w AS mn,
                            MAX(t.x) OVER w AS mx,
                            COUNT(t.x) OVER w AS c
                        FROM nums AS t
                        WINDOW w AS (PARTITION BY t.g ORDER BY t.k)
                    """.trimIndent(),
                    expected = Datum.bagVararg(
                        row("g" to "a", "x" to 2, "s" to 2L, "a" to dec(2), "mn" to 2, "mx" to 2, "c" to 1L),
                        row("g" to "a", "x" to 4, "s" to 12L, "a" to dec(4), "mn" to 2, "mx" to 6, "c" to 3L),
                        row("g" to "a", "x" to 6, "s" to 12L, "a" to dec(4), "mn" to 2, "mx" to 6, "c" to 3L),
                        row("g" to "a", "x" to 8, "s" to 20L, "a" to dec(5), "mn" to 2, "mx" to 8, "c" to 4L),
                        row("g" to "b", "x" to 10, "s" to 10L, "a" to dec(10), "mn" to 10, "mx" to 10, "c" to 1L),
                    ),
                ),
                SuccessTestCase(
                    name = "Aggregates skip NULL arguments; all-NULL partition ($mode)",
                    mode = mode,
                    globals = globals,
                    input = """
                        SELECT
                            r.id AS id,
                            SUM(r.v) OVER w AS s,
                            COUNT(r.v) OVER w AS cv,
                            COUNT(*) OVER w AS cs,
                            MIN(r.v) OVER w AS mn,
                            MAX(r.v) OVER w AS mx
                        FROM readings AS r
                        WINDOW w AS (PARTITION BY r.grp ORDER BY r.id)
                    """.trimIndent(),
                    expected = Datum.bagVararg(
                        // grp a: 10, null, null, 40, 50, null
                        row("id" to 1, "s" to 10L, "cv" to 1L, "cs" to 1L, "mn" to 10, "mx" to 10),
                        row("id" to 2, "s" to 10L, "cv" to 1L, "cs" to 2L, "mn" to 10, "mx" to 10),
                        row("id" to 3, "s" to 10L, "cv" to 1L, "cs" to 3L, "mn" to 10, "mx" to 10),
                        row("id" to 4, "s" to 50L, "cv" to 2L, "cs" to 4L, "mn" to 10, "mx" to 40),
                        row("id" to 5, "s" to 100L, "cv" to 3L, "cs" to 5L, "mn" to 10, "mx" to 50),
                        row("id" to 6, "s" to 100L, "cv" to 3L, "cs" to 6L, "mn" to 10, "mx" to 50),
                        // grp b: null, null
                        row("id" to 7, "s" to null, "cv" to 0L, "cs" to 1L, "mn" to null, "mx" to null),
                        row("id" to 8, "s" to null, "cv" to 0L, "cs" to 2L, "mn" to null, "mx" to null),
                    ),
                ),
                SuccessTestCase(
                    name = "DISTINCT aggregates over the whole partition ($mode)",
                    mode = mode,
                    globals = globals,
                    input = """
                        SELECT
                            t.id AS id,
                            COUNT(DISTINCT t.age) OVER (PARTITION BY t.department) AS cd,
                            SUM(DISTINCT t.age) OVER (PARTITION BY t.department) AS sd,
                            SUM(ALL t.age) OVER (PARTITION BY t.department) AS sa
                        FROM employee AS t
                    """.trimIndent(),
                    expected = Datum.bagVararg(
                        row("id" to 2, "cd" to 4L, "sd" to 122L, "sa" to 152L),
                        row("id" to 3, "cd" to 4L, "sd" to 122L, "sa" to 152L),
                        row("id" to 5, "cd" to 4L, "sd" to 122L, "sa" to 152L),
                        row("id" to 7, "cd" to 4L, "sd" to 122L, "sa" to 152L),
                        row("id" to 9, "cd" to 4L, "sd" to 122L, "sa" to 152L),
                        row("id" to 1, "cd" to 2L, "sd" to 57L, "sa" to 85L),
                        row("id" to 4, "cd" to 2L, "sd" to 57L, "sa" to 85L),
                        row("id" to 8, "cd" to 2L, "sd" to 57L, "sa" to 85L),
                        row("id" to 0, "cd" to 2L, "sd" to 72L, "sa" to 72L),
                        row("id" to 6, "cd" to 2L, "sd" to 72L, "sa" to 72L),
                    ),
                ),
                SuccessTestCase(
                    name = "Aggregates mixed with ranking and navigation functions ($mode)",
                    mode = mode,
                    globals = globals,
                    input = """
                        SELECT
                            t.id AS id,
                            ROW_NUMBER() OVER w AS rn,
                            RANK() OVER w AS rk,
                            COUNT(*) OVER w AS c,
                            LAG(t.age, 1, 0) OVER w AS prev,
                            SUM(t.age) OVER w AS s,
                            SUM(t.age) OVER (PARTITION BY t.department) AS total
                        FROM employee AS t
                        WINDOW w AS (PARTITION BY t.department ORDER BY t.age)
                    """.trimIndent(),
                    expected = Datum.bagVararg(
                        row("id" to 5, "rn" to 1L, "rk" to 1L, "c" to 1L, "prev" to 0, "s" to 25L, "total" to 152L),
                        row("id" to 3, "rn" to 2L, "rk" to 2L, "c" to 3L, "prev" to 25, "s" to 85L, "total" to 152L),
                        row("id" to 9, "rn" to 3L, "rk" to 2L, "c" to 3L, "prev" to 30, "s" to 85L, "total" to 152L),
                        row("id" to 7, "rn" to 4L, "rk" to 4L, "c" to 4L, "prev" to 30, "s" to 117L, "total" to 152L),
                        row("id" to 2, "rn" to 5L, "rk" to 5L, "c" to 5L, "prev" to 32, "s" to 152L, "total" to 152L),
                        row("id" to 1, "rn" to 1L, "rk" to 1L, "c" to 2L, "prev" to 0, "s" to 56L, "total" to 85L),
                        row("id" to 4, "rn" to 2L, "rk" to 1L, "c" to 2L, "prev" to 28, "s" to 56L, "total" to 85L),
                        row("id" to 8, "rn" to 3L, "rk" to 3L, "c" to 3L, "prev" to 28, "s" to 85L, "total" to 85L),
                        row("id" to 6, "rn" to 1L, "rk" to 1L, "c" to 1L, "prev" to 0, "s" to 32L, "total" to 72L),
                        row("id" to 0, "rn" to 2L, "rk" to 2L, "c" to 2L, "prev" to 32, "s" to 72L, "total" to 72L),
                    ),
                ),
                SuccessTestCase(
                    name = "Window aggregate over group aggregates ($mode)",
                    mode = mode,
                    globals = globals,
                    input = """
                        SELECT
                            g AS g,
                            SUM(t.x) AS s,
                            SUM(SUM(t.x)) OVER (ORDER BY g) AS running,
                            COUNT(*) OVER () AS groups,
                            MAX(COUNT(*)) OVER () AS max_count
                        FROM nums AS t
                        GROUP BY t.g AS g
                    """.trimIndent(),
                    expected = Datum.bagVararg(
                        row("g" to "a", "s" to 20L, "running" to 20L, "groups" to 2L, "max_count" to 4L),
                        row("g" to "b", "s" to 10L, "running" to 30L, "groups" to 2L, "max_count" to 4L),
                    ),
                ),
            )
        } + listOf(
            SuccessTestCase(
                name = "Aggregates skip MISSING arguments",
                mode = Mode.PERMISSIVE(),
                globals = globals,
                input = """
                    SELECT
                        t.id AS id,
                        SUM(t.v) OVER (ORDER BY t.id) AS s,
                        COUNT(t.v) OVER (ORDER BY t.id) AS c,
                        COUNT(*) OVER (ORDER BY t.id) AS cs
                    FROM sparse AS t
                """.trimIndent(),
                expected = Datum.bagVararg(
                    row("id" to 1, "s" to 1L, "c" to 1L, "cs" to 1L),
                    row("id" to 2, "s" to 1L, "c" to 1L, "cs" to 2L),
                    row("id" to 3, "s" to 1L, "c" to 1L, "cs" to 3L),
                    row("id" to 4, "s" to 5L, "c" to 2L, "cs" to 4L),
                ),
            ),
        )

        @JvmStatic
        fun failureTestCases() = listOf(
            // SUM of a non-numeric argument fails at evaluation, as it does for the group aggregate (asserted below too).
            FailureTestCase(
                name = "SUM window over STRING (STRICT)",
                mode = Mode.STRICT(),
                globals = globals,
                input = "SELECT SUM(t.name) OVER (PARTITION BY t.department) AS s FROM employee AS t",
            ),
            FailureTestCase(
                name = "SUM window over STRING (PERMISSIVE)",
                mode = Mode.PERMISSIVE(),
                globals = globals,
                input = "SELECT SUM(t.name) OVER (PARTITION BY t.department) AS s FROM employee AS t",
            ),
            FailureTestCase(
                name = "SUM group aggregate over STRING (STRICT)",
                mode = Mode.STRICT(),
                globals = globals,
                input = "SELECT SUM(t.name) AS s FROM employee AS t GROUP BY t.department",
            ),
            FailureTestCase(
                name = "SUM group aggregate over STRING (PERMISSIVE)",
                mode = Mode.PERMISSIVE(),
                globals = globals,
                input = "SELECT SUM(t.name) AS s FROM employee AS t GROUP BY t.department",
            ),
            FailureTestCase(
                name = "Lag and lead referencing sometimes missing attr (partner)",
                mode = Mode.STRICT(),
                globals = globals,
                input = """
                    SELECT
                        t.id AS _id,
                        t.name AS _name,
                        RANK() OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _rank,
                        DENSE_RANK() OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _dense_rank,
                        ROW_NUMBER() OVER (PARTITION BY t.department ORDER BY t.age, t.name) as _row_number,
                        LAG(t.partner, 1, '$FALLBACK') OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _lag,
                        LEAD(t.partner, 1, '$FALLBACK') OVER (PARTITION BY t.department ORDER BY t.age, t.name) AS _lead
                    FROM employee AS t;
                """.trimIndent(),
            ),
        )
    }
}
