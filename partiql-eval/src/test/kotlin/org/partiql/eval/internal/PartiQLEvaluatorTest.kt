package org.partiql.eval.internal

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.eval.Mode
import org.partiql.eval.compiler.PartiQLCompiler
import org.partiql.spi.types.PType
import org.partiql.spi.types.PTypeField
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field
import java.math.BigDecimal

/**
 * This holds sanity tests during the development of the [PartiQLCompiler.standard] implementation.
 */
class PartiQLEvaluatorTest {

    @ParameterizedTest
    @MethodSource("sanityTestsCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun sanityTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("typingModeTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun typingModeTests(tc: TypingTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("subqueryTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun subqueryTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("aggregationTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun aggregationTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("joinTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun joinTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("globalsTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun globalsTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("castTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun castTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("intervalAbsTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun intervalAbsTests(tc: SuccessTestCase) = tc.run()

    companion object {

        @JvmStatic
        fun castTestCases() = listOf(
            SuccessTestCase(
                input = """
                    CAST(20 AS DECIMAL(10, 5));
                """.trimIndent(),
                expected = Datum.decimal(BigDecimal.valueOf(2000000, 5))
            ),
            SuccessTestCase(
                input = """
                    CAST(20 AS DECIMAL(10, 3));
                """.trimIndent(),
                expected = Datum.decimal(BigDecimal.valueOf(20000, 3))
            ),
            SuccessTestCase(
                input = """
                    CAST(20 AS DECIMAL(2, 0));
                """.trimIndent(),
                expected = Datum.decimal(BigDecimal.valueOf(20, 0))
            ),
            SuccessTestCase(
                input = """
                    CAST(20 AS DECIMAL(1, 0));
                """.trimIndent(),
                expected = Datum.missing(),
                mode = Mode.PERMISSIVE()
            ),
            SuccessTestCase(
                input = """
                    1 + 2.0
                """.trimIndent(),
                expected = Datum.decimal(BigDecimal.valueOf(30, 1))
            ),
            SuccessTestCase(
                input = "SELECT DISTINCT VALUE t * 100 FROM <<0, 1, 2.0, 3.0>> AS t;",
                expected = Datum.bagVararg(
                    Datum.integer(0),
                    Datum.integer(100),
                    Datum.decimal(BigDecimal.valueOf(2000, 1)),
                    Datum.decimal(BigDecimal.valueOf(3000, 1)),
                )
            ),
            SuccessTestCase(
                input = """
                    CAST(20 AS CHAR(2));
                """.trimIndent(),
                expected = Datum.character(
                    "20".toString(),
                    2
                ),
            ),
        )

        @JvmStatic
        fun globalsTestCases() = listOf(
            SuccessTestCase(
                input = """
                    SELECT VALUE t.a
                    FROM t;
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.bigint(1),
                    Datum.bigint(2)
                ),
                globals = listOf(
                    Global(
                        name = "t",
                        value = """
                            [
                                { "a": 1 },
                                { "a": 2 }
                            ]
                        """
                    )
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT VALUE t1.a
                    FROM t AS t1, t AS t2;
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.bigint(1),
                    Datum.bigint(1),
                    Datum.bigint(2),
                    Datum.bigint(2)
                ),
                globals = listOf(
                    Global(
                        name = "t",
                        value = """
                            [
                                { "a": 1 },
                                { "a": 2 }
                            ]
                        """
                    )
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT o.name AS orderName,
                        (SELECT c.name FROM customers c WHERE c.id=o.custId) AS customerName
                    FROM orders o
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.struct(
                        Field.of(
                            "orderName",
                            Datum.string("foo")
                        )
                    ),
                    Datum.struct(
                        Field.of(
                            "orderName",
                            Datum.string("bar")
                        ),
                        Field.of(
                            "customerName",
                            Datum.string("Helen")
                        )
                    )
                ),
                globals = listOf(
                    Global(
                        name = "customers",
                        value = """
                            [{id:1, name: "Mary"},
                            {id:2, name: "Helen"},
                            {id:1, name: "John"}
                            ]
                        """
                    ),
                    Global(
                        name = "orders",
                        value = """
                            [{custId:1, name: "foo"},
                            {custId:2, name: "bar"}
                            ]
                        """
                    ),
                )
            ),
        )

        @JvmStatic
        fun joinTestCases() = listOf(
            // LEFT OUTER JOIN -- Easy
            SuccessTestCase(
                input = """
                    SELECT VALUE [lhs, rhs]
                    FROM << 0, 1, 2 >> lhs
                    LEFT OUTER JOIN << 0, 2, 3 >> rhs
                    ON lhs = rhs
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.array(
                        listOf(
                            Datum.integer(0),
                            Datum.integer(0)
                        )
                    ),
                    Datum.array(
                        listOf(
                            Datum.integer(1),
                            Datum.nullValue(PType.integer())
                        )
                    ),
                    Datum.array(
                        listOf(
                            Datum.integer(2),
                            Datum.integer(2)
                        )
                    )
                )
            ),
            // LEFT OUTER JOIN -- RHS Empty
            SuccessTestCase(
                input = """
                    SELECT VALUE [lhs, rhs]
                    FROM
                        << 0, 1, 2 >> lhs
                    LEFT OUTER JOIN (
                        SELECT VALUE n
                        FROM << 0, 2, 3 >> AS n
                        WHERE n > 100
                    ) rhs
                    ON lhs = rhs
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.array(listOf(Datum.integer(0), Datum.nullValue(PType.integer()))),
                    Datum.array(listOf(Datum.integer(1), Datum.nullValue(PType.integer()))),
                    Datum.array(listOf(Datum.integer(2), Datum.nullValue(PType.integer()))),
                )
            ),
            // LEFT OUTER JOIN -- LHS Empty
            SuccessTestCase(
                input = """
                    SELECT VALUE [lhs, rhs]
                    FROM <<>> lhs
                    LEFT OUTER JOIN << 0, 2, 3>> rhs
                    ON lhs = rhs
                """.trimIndent(),
                expected = Datum.bag(emptyList())
            ),
            // LEFT OUTER JOIN -- No Matches
            SuccessTestCase(
                input = """
                    SELECT VALUE [lhs, rhs]
                    FROM << 0, 1, 2 >> lhs
                    LEFT OUTER JOIN << 3, 4, 5 >> rhs
                    ON lhs = rhs
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.array(
                        listOf(
                            Datum.integer(0),
                            Datum.nullValue(PType.integer())
                        )
                    ),
                    Datum.array(
                        listOf(
                            Datum.integer(1),
                            Datum.nullValue(PType.integer())
                        )
                    ),
                    Datum.array(
                        listOf(
                            Datum.integer(2),
                            Datum.nullValue(PType.integer())
                        )
                    )
                )
            ),
            // RIGHT OUTER JOIN -- Easy
            SuccessTestCase(
                input = """
                    SELECT VALUE [lhs, rhs]
                    FROM << 0, 1, 2 >> lhs
                    RIGHT OUTER JOIN << 0, 2, 3 >> rhs
                    ON lhs = rhs
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.array(listOf(Datum.integer(0), Datum.integer(0))),
                    Datum.array(listOf(Datum.integer(2), Datum.integer(2))),
                    Datum.array(listOf(Datum.nullValue(PType.integer()), Datum.integer(3))),
                )
            ),
            // RIGHT OUTER JOIN -- RHS Empty
            SuccessTestCase(
                input = """
                    SELECT VALUE [lhs, rhs]
                    FROM << 0, 1, 2 >> lhs
                    RIGHT OUTER JOIN <<>> rhs
                    ON lhs = rhs
                """.trimIndent(),
                expected = Datum.bag(emptyList())
            ),
            // RIGHT OUTER JOIN -- LHS Empty
            SuccessTestCase(
                input = """
                    SELECT VALUE [lhs, rhs]
                    FROM (
                        SELECT VALUE n
                        FROM << 0, 1, 2 >> AS n
                        WHERE n > 100
                    ) lhs RIGHT OUTER JOIN
                        << 0, 2, 3>> rhs
                    ON lhs = rhs
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.array(listOf(Datum.nullValue(PType.integer()), Datum.integer(0))),
                    Datum.array(listOf(Datum.nullValue(PType.integer()), Datum.integer(2))),
                    Datum.array(listOf(Datum.nullValue(PType.integer()), Datum.integer(3))),
                )
            ),
            // RIGHT OUTER JOIN -- No Matches
            SuccessTestCase(
                input = """
                    SELECT VALUE [lhs, rhs]
                    FROM << 0, 1, 2 >> lhs
                    RIGHT OUTER JOIN << 3, 4, 5 >> rhs
                    ON lhs = rhs
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.array(listOf(Datum.nullValue(PType.integer()), Datum.integer(3))),
                    Datum.array(listOf(Datum.nullValue(PType.integer()), Datum.integer(4))),
                    Datum.array(listOf(Datum.nullValue(PType.integer()), Datum.integer(5))),
                )
            ),
            // LEFT OUTER JOIN -- LATERAL
            SuccessTestCase(
                input = """
                    SELECT VALUE rhs
                    FROM << [0, 1, 2], [10, 11, 12], [20, 21, 22] >> AS lhs
                    LEFT OUTER JOIN lhs AS rhs
                    ON lhs[2] = rhs
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.integer(2),
                    Datum.integer(12),
                    Datum.integer(22),
                )
            ),
            // INNER JOIN -- LATERAL
            SuccessTestCase(
                input = """
                    SELECT VALUE rhs
                    FROM << [0, 1, 2], [10, 11, 12], [20, 21, 22] >> AS lhs
                    INNER JOIN lhs AS rhs
                    ON lhs[2] = rhs
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.integer(2),
                    Datum.integer(12),
                    Datum.integer(22),
                )
            ),
        )

        @JvmStatic
        fun subqueryTestCases() = listOf(
            SuccessTestCase(
                input = """
                    SELECT VALUE (
                        SELECT VALUE t1 + t2
                        FROM <<5, 6>> AS t2
                    ) FROM <<0, 10>> AS t1;
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.bagVararg(Datum.integer(5), Datum.integer(6)),
                    Datum.bagVararg(Datum.integer(15), Datum.integer(16))
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT VALUE (
                        SELECT t1 + t2
                        FROM <<5>> AS t2
                    ) FROM <<0, 10>> AS t1;
                """.trimIndent(),
                expected = Datum.bagVararg(Datum.integer(5), Datum.integer(15))
            ),
            SuccessTestCase(
                input = """
                    SELECT (
                        SELECT VALUE t1 + t2
                        FROM <<5>> AS t2
                    ) AS t1_plus_t2
                    FROM <<0, 10>> AS t1;
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("t1_plus_t2", Datum.bagVararg(Datum.integer(5)))),
                    Datum.struct(Field.of("t1_plus_t2", Datum.bagVararg(Datum.integer(15))))
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT
                        (
                            SELECT (t1 + t2) * (
                                SELECT t1 + t3 + t2
                                FROM <<7>> AS t3
                            )
                            FROM <<5>> AS t2
                        ) AS t1_plus_t2
                    FROM <<0, 10>> AS t1;
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.struct(Field.of("t1_plus_t2", Datum.integer(60))),
                    Datum.struct(Field.of("t1_plus_t2", Datum.integer(330)))
                )
            ),
            SuccessTestCase(
                input = """
                    1 + (SELECT t.a FROM << { 'a': 3 } >> AS t)
                """.trimIndent(),
                expected = Datum.integer(4)
            ),
            SuccessTestCase(
                input = """
                    SELECT VALUE element
                    FROM << { 'a': [0, 1, 2] }, { 'a': [3, 4, 5] } >> AS t, t.a AS element
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.integer(0),
                    Datum.integer(1),
                    Datum.integer(2),
                    Datum.integer(3),
                    Datum.integer(4),
                    Datum.integer(5),
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT VALUE element
                    FROM << { 'a': { 'c': [0, 1, 2] } }, { 'a': { 'c': [3, 4, 5] } } >> AS t, t.a AS b, b.c AS element
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.integer(0),
                    Datum.integer(1),
                    Datum.integer(2),
                    Datum.integer(3),
                    Datum.integer(4),
                    Datum.integer(5),
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT VALUE t_a_b + t_a_c
                    FROM << { 'a': { 'b': [100, 200], 'c': [0, 1, 2] } }, { 'a': { 'b': [300, 400], 'c': [3, 4, 5] } } >>
                        AS t, t.a AS t_a, t_a.b AS t_a_b, t_a.c AS t_a_c
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.integer(100),
                    Datum.integer(101),
                    Datum.integer(102),
                    Datum.integer(200),
                    Datum.integer(201),
                    Datum.integer(202),
                    Datum.integer(303),
                    Datum.integer(304),
                    Datum.integer(305),
                    Datum.integer(403),
                    Datum.integer(404),
                    Datum.integer(405),
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT VALUE t_a_b + t_a_c + t_a_c_original
                    FROM << { 'a': { 'b': [100, 200], 'c': [1, 2] } }, { 'a': { 'b': [300, 400], 'c': [3, 4] } } >>
                        AS t, t.a AS t_a, t_a.b AS t_a_b, t_a.c AS t_a_c, t.a.c AS t_a_c_original
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.integer(102),
                    Datum.integer(103),
                    Datum.integer(103),
                    Datum.integer(104),
                    Datum.integer(202),
                    Datum.integer(203),
                    Datum.integer(203),
                    Datum.integer(204),
                    Datum.integer(306),
                    Datum.integer(307),
                    Datum.integer(307),
                    Datum.integer(308),
                    Datum.integer(406),
                    Datum.integer(407),
                    Datum.integer(407),
                    Datum.integer(408),
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT VALUE t_a_b + t_a_c + t_a_c_original
                    FROM << { 'a': { 'b': [100, 200], 'c': [1, 2] } }, { 'a': { 'b': [300, 400], 'c': [3, 4] } } >>
                        AS t, t.a AS t_a, t_a.b AS t_a_b, t_a.c AS t_a_c, (SELECT VALUE d FROM t.a.c AS d) AS t_a_c_original
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.integer(102),
                    Datum.integer(103),
                    Datum.integer(103),
                    Datum.integer(104),
                    Datum.integer(202),
                    Datum.integer(203),
                    Datum.integer(203),
                    Datum.integer(204),
                    Datum.integer(306),
                    Datum.integer(307),
                    Datum.integer(307),
                    Datum.integer(308),
                    Datum.integer(406),
                    Datum.integer(407),
                    Datum.integer(407),
                    Datum.integer(408),
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT VALUE t_a_b + t_a_c + t_a_c_original
                    FROM << { 'a': { 'b': [100, 200], 'c': [1, 2] } }, { 'a': { 'b': [300, 400], 'c': [3, 4] } } >>
                        AS t,
                        t.a AS t_a,
                        t_a.b AS t_a_b,
                        t_a.c AS t_a_c,
                        (SELECT VALUE d + (SELECT b_og FROM t.a.b AS b_og WHERE b_og = 200 OR b_og = 400) FROM t.a.c AS d) AS t_a_c_original
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.integer(302),
                    Datum.integer(303),
                    Datum.integer(303),
                    Datum.integer(304),
                    Datum.integer(402),
                    Datum.integer(403),
                    Datum.integer(403),
                    Datum.integer(404),
                    Datum.integer(706),
                    Datum.integer(707),
                    Datum.integer(707),
                    Datum.integer(708),
                    Datum.integer(806),
                    Datum.integer(807),
                    Datum.integer(807),
                    Datum.integer(808),
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT VALUE
                        t_a_b + t_a_c + t_a_c_original + (
                            SELECT t_a_c_inner
                            FROM t.a.c AS t_a_c_inner
                            WHERE t_a_c_inner = 2 OR t_a_c_inner = 4
                        )
                    FROM << { 'a': { 'b': [100, 200], 'c': [1, 2] } }, { 'a': { 'b': [300, 400], 'c': [3, 4] } } >>
                        AS t,
                        t.a AS t_a,
                        t_a.b AS t_a_b,
                        t_a.c AS t_a_c,
                        (SELECT VALUE d + (SELECT b_og FROM t.a.b AS b_og WHERE b_og = 200 OR b_og = 400) FROM t.a.c AS d) AS t_a_c_original
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.integer(304),
                    Datum.integer(305),
                    Datum.integer(305),
                    Datum.integer(306),
                    Datum.integer(404),
                    Datum.integer(405),
                    Datum.integer(405),
                    Datum.integer(406),
                    Datum.integer(710),
                    Datum.integer(711),
                    Datum.integer(711),
                    Datum.integer(712),
                    Datum.integer(810),
                    Datum.integer(811),
                    Datum.integer(811),
                    Datum.integer(812),
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT VALUE
                        t_a_b + t_a_c + t_a_c_original + (
                            SELECT t_a_c_inner + t_a_c
                            FROM t.a.c AS t_a_c_inner
                            WHERE t_a_c_inner = 2 OR t_a_c_inner = 4
                        )
                    FROM << { 'a': { 'b': [100, 200], 'c': [1, 2] } }, { 'a': { 'b': [300, 400], 'c': [3, 4] } } >>
                        AS t,
                        t.a AS t_a,
                        t_a.b AS t_a_b,
                        t_a.c AS t_a_c,
                        (SELECT VALUE d + (SELECT b_og + t_a_c FROM t.a.b AS b_og WHERE b_og = 200 OR b_og = 400) FROM t.a.c AS d) AS t_a_c_original
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.integer(306),
                    Datum.integer(307),
                    Datum.integer(309),
                    Datum.integer(310),
                    Datum.integer(406),
                    Datum.integer(407),
                    Datum.integer(409),
                    Datum.integer(410),
                    Datum.integer(716),
                    Datum.integer(717),
                    Datum.integer(719),
                    Datum.integer(720),
                    Datum.integer(816),
                    Datum.integer(817),
                    Datum.integer(819),
                    Datum.integer(820),
                )
            )
        )

        @JvmStatic
        fun aggregationTestCases() = kotlin.collections.listOf(
            SuccessTestCase(
                input = """
                    SELECT VALUE { 'sensor': sensor,
                          'readings': (SELECT VALUE v.l.co FROM g AS v)
                    }
                    FROM [{'sensor':1, 'co':0.4}, {'sensor':1, 'co':0.2}, {'sensor':2, 'co':0.3}] AS l
                    GROUP BY l.sensor AS sensor GROUP AS g
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.struct(
                        Field.of("sensor", Datum.integer(1)),
                        Field.of(
                            "readings",
                            Datum.bagVararg(
                                Datum.decimal(0.4.toBigDecimal()),
                                Datum.decimal(0.2.toBigDecimal())
                            )
                        )
                    ),
                    Datum.struct(
                        Field.of("sensor", Datum.integer(2)),
                        Field.of(
                            "readings",
                            Datum.bagVararg(
                                Datum.decimal(0.3.toBigDecimal())
                            )
                        )
                    ),
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT col1, g
                    FROM [{ 'col1':1 }, { 'col1':1 }] simple_1_col_1_group
                    GROUP BY col1 GROUP AS g
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.struct(
                        Field.of("col1", Datum.integer(1)),
                        Field.of(
                            "g",
                            Datum.bagVararg(
                                Datum.struct(
                                    Field.of("simple_1_col_1_group", Datum.struct(Field.of("col1", Datum.integer(1))))
                                ),
                                Datum.struct(
                                    Field.of("simple_1_col_1_group", Datum.struct(Field.of("col1", Datum.integer(1))))
                                ),
                            )
                        )
                    ),
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT p.supplierId_mixed
                    FROM [
                        { 'productId': 5,  'categoryId': 21, 'regionId': 100, 'supplierId_nulls': null, 'price_nulls': null },
                        { 'productId': 4,  'categoryId': 20, 'regionId': 100, 'supplierId_nulls': null, 'supplierId_mixed': null, 'price_nulls': null, 'price_mixed': null }
                    ] AS p
                    GROUP BY p.supplierId_mixed
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.struct(
                        Field.of("supplierId_mixed", Datum.nullValue()),
                    ),
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT *
                    FROM << { 'a': 1, 'b': 2 } >> AS t
                    GROUP BY a, b, a + b GROUP AS g
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.struct(
                        Field.of("a", Datum.integer(1)),
                        Field.of("b", Datum.integer(2)),
                        Field.of("_3", Datum.integer(3)),
                        Field.of(
                            "g",
                            Datum.bagVararg(
                                Datum.struct(
                                    Field.of(
                                        "t",
                                        Datum.struct(
                                            Field.of("a", Datum.integer(1)),
                                            Field.of("b", Datum.integer(2)),
                                        )
                                    )
                                )
                            )
                        ),
                    ),
                )
            ),
        )

        @JvmStatic
        fun sanityTestsCases() = listOf(
            SuccessTestCase(
                input = "SELECT VALUE 1 FROM <<0, 1>>;",
                expected = Datum.bagVararg(Datum.integer(1), Datum.integer(1))
            ),
            SuccessTestCase(
                input = "SELECT VALUE t FROM <<10, 20, 30>> AS t;",
                expected = Datum.bagVararg(Datum.integer(10), Datum.integer(20), Datum.integer(30))
            ),
            SuccessTestCase(
                input = "SELECT VALUE t FROM <<true, false, true, false, false, false>> AS t WHERE t;",
                expected = Datum.bagVararg(Datum.bool(true), Datum.bool(true))
            ),
            SuccessTestCase(
                input = "SELECT t.a, s.b FROM << { 'a': 1 } >> t, << { 'b': 2 } >> s;",
                expected = Datum.bagVararg(Datum.struct(Field.of("a", Datum.integer(1)), Field.of("b", Datum.integer(2))))
            ),
            SuccessTestCase(
                input = "SELECT t.a, s.b FROM << { 'a': 1 } >> t LEFT JOIN << { 'b': 2 } >> s ON false;",
                expected = Datum.bagVararg(Datum.struct(Field.of("a", Datum.integer(1)), Field.of("b", Datum.nullValue()))),
                mode = Mode.STRICT()
            ),
            SuccessTestCase(
                input = "SELECT t.a, s.b FROM << { 'a': 1 } >> t FULL OUTER JOIN << { 'b': 2 } >> s ON false;",
                expected = Datum.bagVararg(
                    Datum.struct(
                        Field.of("a", Datum.integer(1)),
                        Field.of("b", Datum.nullValue())
                    ),
                    Datum.struct(
                        Field.of("a", Datum.nullValue()),
                        Field.of("b", Datum.integer(2))
                    )
                )
            ),
            SuccessTestCase(
                input = """
                    TUPLEUNION(
                        { 'a': 1 },
                        { 'b': TRUE },
                        { 'c': 'hello' }
                    );
                """.trimIndent(),
                expected = Datum.struct(
                    Field.of("a", Datum.integer(1)),
                    Field.of("b", Datum.bool(true)),
                    Field.of("c", Datum.string("hello"))
                )
            ),
            SuccessTestCase(
                input = """
                    CASE
                        WHEN NULL THEN 'isNull'
                        WHEN MISSING THEN 'isMissing'
                        WHEN FALSE THEN 'isFalse'
                        WHEN TRUE THEN 'isTrue'
                    END
                    ;
                """.trimIndent(),
                expected = Datum.string("isTrue")
            ),
            SuccessTestCase(
                input = "SELECT t.a, s.b FROM << { 'a': 1 } >> t FULL OUTER JOIN << { 'b': 2 } >> s ON TRUE;",
                expected = Datum.bagVararg(
                    Datum.struct(
                        Field.of("a", Datum.integer(1)),
                        Field.of("b", Datum.integer(2))
                    )
                )
            ),
            SuccessTestCase(
                input = """
                    TUPLEUNION(
                        { 'a': 1 },
                        NULL,
                        { 'c': 'hello' }
                    );
                """.trimIndent(),
                expected = Datum.nullValue(PType.struct())
            ),
            SuccessTestCase(
                input = """
                    CASE
                        WHEN NULL THEN 'isNull'
                        WHEN MISSING THEN 'isMissing'
                        WHEN FALSE THEN 'isFalse'
                    END
                    ;
                """.trimIndent(),
                expected = Datum.nullValue(PType.string())
            ),
            SuccessTestCase(
                input = """
                    TUPLEUNION(
                        { 'a': 1 },
                        5,
                        { 'c': 'hello' }
                    );
                """.trimIndent(),
                expected = Datum.missing()
            ),
            SuccessTestCase(
                input = """
                    TUPLEUNION(
                        { 'a': 1, 'b': FALSE },
                        { 'b': TRUE },
                        { 'c': 'hello' }
                    );
                """.trimIndent(),
                expected = Datum.struct(
                    Field.of("a", Datum.integer(1)),
                    Field.of("b", Datum.bool(false)),
                    Field.of("b", Datum.bool(true)),
                    Field.of("c", Datum.string("hello"))
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT * FROM
                    <<
                        { 'a': 1, 'b': FALSE }
                    >> AS t,
                    <<
                        { 'b': TRUE }
                    >> AS s
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.struct(
                        Field.of("a", Datum.integer(1)),
                        Field.of("b", Datum.bool(false)),
                        Field.of("b", Datum.bool(true))
                    )
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT VALUE {
                        'a': 1,
                        'b': NULL,
                        t.c : t.d
                    }
                    FROM <<
                        { 'c': 'hello', 'd': 'world' }
                    >> AS t
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.struct(
                        Field.of("a", Datum.integer(1)),
                        Field.of("b", Datum.nullValue()),
                        Field.of("hello", Datum.string("world"))
                    )
                )
            ),
            SuccessTestCase(
                input = "SELECT v, i FROM << 'a', 'b', 'c' >> AS v AT i",
                expected = Datum.bagVararg(
                    Datum.struct(
                        Field.of("v", Datum.string("a"))
                    ),
                    Datum.struct(
                        Field.of("v", Datum.string("b"))
                    ),
                    Datum.struct(
                        Field.of("v", Datum.string("c"))
                    )
                )
            ),
            SuccessTestCase(
                input = "SELECT DISTINCT VALUE t FROM <<true, false, true, false, false, false>> AS t;",
                expected = Datum.bagVararg(Datum.bool(true), Datum.bool(false))
            ),
            SuccessTestCase(
                input = "SELECT DISTINCT VALUE t FROM <<true, false, true, false, false, false>> AS t WHERE t = TRUE;",
                expected = Datum.bagVararg(Datum.bool(true))
            ),
            SuccessTestCase(
                input = "100 + 50;",
                expected = Datum.integer(150)
            ),
            SuccessTestCase(
                input = "SELECT DISTINCT VALUE t * 100 FROM <<0, 1, 2, 3>> AS t;",
                expected = Datum.bagVararg(Datum.integer(0), Datum.integer(100), Datum.integer(200), Datum.integer(300))
            ),
            SuccessTestCase(
                input = """
                    PIVOT x.v AT x.k FROM << 
                        { 'k': 'a', 'v': 'x' },
                        { 'k': 'b', 'v': 'y' },
                        { 'k': 'c', 'v': 'z' }
                    >> AS x
                """.trimIndent(),
                expected = Datum.struct(
                    Field.of("a", Datum.string("x")),
                    Field.of("b", Datum.string("y")),
                    Field.of("c", Datum.string("z"))
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT t
                    EXCLUDE t.a.b
                    FROM <<
                        {'a': {'b': 2}, 'foo': 'bar', 'foo2': 'bar2'}
                    >> AS t
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.struct(
                        Field.of(
                            "t",
                            Datum.struct(
                                Field.of("a", Datum.struct()),
                                Field.of("foo", Datum.string("bar")),
                                Field.of("foo2", Datum.string("bar2"))
                            )
                        )
                    )
                )
            ),
            SuccessTestCase(
                input = """
                    SELECT *
                    EXCLUDE
                        t.a.b.c[*].field_x
                    FROM [{
                        'a': {
                            'b': {
                                'c': [
                                    {                    -- c[0]; field_x to be removed
                                        'field_x': 0,
                                        'field_y': 0
                                    },
                                    {                    -- c[1]; field_x to be removed
                                        'field_x': 1,
                                        'field_y': 1
                                    },
                                    {                    -- c[2]; field_x to be removed
                                        'field_x': 2,
                                        'field_y': 2
                                    }
                                ]
                            }
                        }
                    }] AS t
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.struct(
                        Field.of(
                            "a",
                            Datum.struct(
                                Field.of(
                                    "b",
                                    Datum.struct(
                                        Field.of(
                                            "c",
                                            Datum.array(
                                                listOf(
                                                    Datum.struct(
                                                        Field.of("field_y", Datum.integer(0))
                                                    ),
                                                    Datum.struct(
                                                        Field.of("field_y", Datum.integer(1))
                                                    ),
                                                    Datum.struct(
                                                        Field.of("field_y", Datum.integer(2))
                                                    )
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            SuccessTestCase(
                input = """
                    CASE (1)
                        WHEN NULL THEN 'isNull'
                        WHEN MISSING THEN 'isMissing'
                        WHEN 2 THEN 'isTwo'
                    END
                    ;
                """.trimIndent(),
                expected = Datum.nullValue(PType.string())
            ),
            SuccessTestCase(
                input = """
                    CASE (1)
                        WHEN NULL THEN 'isNull'
                        WHEN MISSING THEN 'isMissing'
                        WHEN 2 THEN 'isTwo'
                        WHEN 1 THEN 'isOne'
                    END
                    ;
                """.trimIndent(),
                expected = Datum.string("isOne")
            ),
            SuccessTestCase(
                input = """
                    `null.bool` IS NULL
                """.trimIndent(),
                expected = Datum.bool(true)
            ),
            // SELECT * without nested coercion
            SuccessTestCase(
                input = """
                    SELECT *
                    FROM (
                        SELECT t.a AS "first", t.b AS "second"
                        FROM << { 'a': 3, 'b': 5 } >> AS t
                    );
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.struct(
                        Field.of("first", Datum.integer(3)),
                        Field.of("second", Datum.integer(5))
                    )
                )
            ),
            // SELECT list without nested coercion
            SuccessTestCase(
                input = """
                    SELECT "first", "second"
                    FROM (
                        SELECT t.a AS "first", t.b AS "second"
                        FROM << { 'a': 3, 'b': 5 } >> AS t
                    );
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.struct(
                        Field.of("first", Datum.integer(3)),
                        Field.of("second", Datum.integer(5))
                    )
                )
            ),
            // SELECT value without nested coercion
            SuccessTestCase(
                input = """
                    SELECT VALUE "first"
                    FROM (
                        SELECT t.a AS "first", t.b AS "second"
                        FROM << { 'a': 3, 'b': 5 } >> AS t
                    );
                """.trimIndent(),
                expected = Datum.bagVararg(
                    Datum.integer(3)
                )
            ),
            // TODO port `IS <boolean value>` tests to conformance tests
            // IS TRUE
            SuccessTestCase(
                input = "TRUE IS TRUE;",
                expected = Datum.bool(true)
            ),
            SuccessTestCase(
                input = "FALSE IS TRUE;",
                expected = Datum.bool(false)
            ),
            SuccessTestCase(
                input = "NULL IS TRUE;",
                expected = Datum.bool(false)
            ),
            SuccessTestCase(
                input = "MISSING IS TRUE;",
                expected = Datum.bool(false)
            ),
            SuccessTestCase(
                input = "'foo' IS TRUE",
                expected = Datum.missing(),
                mode = Mode.PERMISSIVE()
            ),
            // IS NOT TRUE
            SuccessTestCase(
                input = "TRUE IS NOT TRUE;",
                expected = Datum.bool(false)
            ),
            SuccessTestCase(
                input = "FALSE IS NOT TRUE;",
                expected = Datum.bool(true)
            ),
            SuccessTestCase(
                input = "NULL IS NOT TRUE;",
                expected = Datum.bool(true)
            ),
            SuccessTestCase(
                input = "MISSING IS NOT TRUE;",
                expected = Datum.bool(true)
            ),
            SuccessTestCase(
                input = "'foo' IS NOT TRUE",
                expected = Datum.nullValue(),
                mode = Mode.PERMISSIVE()
            ),
            // IS FALSE
            SuccessTestCase(
                input = "TRUE IS FALSE;",
                expected = Datum.bool(false)
            ),
            SuccessTestCase(
                input = "FALSE IS FALSE;",
                expected = Datum.bool(true)
            ),
            SuccessTestCase(
                input = "NULL IS FALSE;",
                expected = Datum.bool(false)
            ),
            SuccessTestCase(
                input = "MISSING IS FALSE;",
                expected = Datum.bool(false)
            ),
            SuccessTestCase(
                input = "'foo' IS FALSE",
                expected = Datum.nullValue(),
                mode = Mode.PERMISSIVE()
            ),
            // IS NOT FALSE
            SuccessTestCase(
                input = "TRUE IS NOT FALSE;",
                expected = Datum.bool(true)
            ),
            SuccessTestCase(
                input = "FALSE IS NOT FALSE;",
                expected = Datum.bool(false)
            ),
            SuccessTestCase(
                input = "NULL IS NOT FALSE;",
                expected = Datum.bool(true)
            ),
            SuccessTestCase(
                input = "MISSING IS NOT FALSE;",
                expected = Datum.bool(true)
            ),
            SuccessTestCase(
                input = "'foo' IS NOT FALSE",
                expected = Datum.nullValue(),
                mode = Mode.PERMISSIVE()
            ),
            // IS UNKNOWN
            SuccessTestCase(
                input = "TRUE IS UNKNOWN;",
                expected = Datum.bool(false)
            ),
            SuccessTestCase(
                input = "FALSE IS UNKNOWN;",
                expected = Datum.bool(false)
            ),
            SuccessTestCase(
                input = "NULL IS UNKNOWN;",
                expected = Datum.bool(true)
            ),
            SuccessTestCase(
                input = "MISSING IS UNKNOWN;",
                expected = Datum.bool(true)
            ),
            SuccessTestCase(
                input = "'foo' IS UNKNOWN",
                expected = Datum.nullValue(),
                mode = Mode.PERMISSIVE()
            ),
            // IS NOT UNKNOWN
            SuccessTestCase(
                input = "TRUE IS NOT UNKNOWN;",
                expected = Datum.bool(true)
            ),
            SuccessTestCase(
                input = "FALSE IS NOT UNKNOWN;",
                expected = Datum.bool(true)
            ),
            SuccessTestCase(
                input = "NULL IS NOT UNKNOWN;",
                expected = Datum.bool(false)
            ),
            SuccessTestCase(
                input = "MISSING IS NOT UNKNOWN;",
                expected = Datum.bool(false)
            ),
            SuccessTestCase(
                input = "'foo' IS NOT UNKNOWN",
                expected = Datum.missing(),
                mode = Mode.PERMISSIVE()
            ),
            SuccessTestCase(
                input = "MISSING IS MISSING;",
                expected = Datum.bool(true)
            ),
            SuccessTestCase(
                input = "MISSING IS MISSING;",
                expected = Datum.bool(true),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                input = "SELECT VALUE t.a IS MISSING FROM << { 'b': 1 }, { 'a': 2 } >> AS t;",
                expected = Datum.bagVararg(Datum.bool(true), Datum.bool(false))
            ),
            // PartiQL Specification Section 7.1.1 -- Equality
            SuccessTestCase(
                input = "5 = 'a';",
                expected = Datum.bool(false),
            ),
            // PartiQL Specification Section 7.1.1 -- Equality
            SuccessTestCase(
                input = "5 = 'a';",
                expected = Datum.bool(false), // TODO: Is this correct?
                mode = Mode.STRICT(),
            ),
            // PartiQL Specification Section 8
            SuccessTestCase(
                input = "NULL IS MISSING;",
                expected = Datum.bool(false),
            ),
            // PartiQL Specification Section 8
            SuccessTestCase(
                input = "NULL IS MISSING;",
                expected = Datum.bool(false),
                mode = Mode.STRICT(),
            ),
            SuccessTestCase(
                input = "SELECT * FROM <<{'a': 10, 'b': 1}, {'a': 1, 'b': 2}>> AS t ORDER BY t.a;",
                expected = Datum.array(
                    listOf(
                        Datum.struct(Field.of("a", Datum.integer(1)), Field.of("b", Datum.integer(2))),
                        Datum.struct(Field.of("a", Datum.integer(10)), Field.of("b", Datum.integer(1)))
                    )
                )
            ),
            SuccessTestCase(
                input = "SELECT * FROM <<{'a': 10, 'b': 1}, {'a': 1, 'b': 2}>> AS t ORDER BY t.a DESC;",
                expected = Datum.array(
                    listOf(
                        Datum.struct(Field.of("a", Datum.integer(10)), Field.of("b", Datum.integer(1))),
                        Datum.struct(Field.of("a", Datum.integer(1)), Field.of("b", Datum.integer(2)))
                    )
                )
            ),
            SuccessTestCase(
                input = "SELECT * FROM <<{'a': NULL, 'b': 1}, {'a': 1, 'b': 2}, {'a': 3, 'b': 4}>> AS t ORDER BY t.a NULLS LAST;",
                expected = Datum.array(
                    listOf(
                        Datum.struct(Field.of("a", Datum.integer(1)), Field.of("b", Datum.integer(2))),
                        Datum.struct(Field.of("a", Datum.integer(3)), Field.of("b", Datum.integer(4))),
                        Datum.struct(Field.of("a", Datum.nullValue()), Field.of("b", Datum.integer(1)))
                    )
                )
            ),
            SuccessTestCase(
                input = "SELECT * FROM <<{'a': NULL, 'b': 1}, {'a': 1, 'b': 2}, {'a': 3, 'b': 4}>> AS t ORDER BY t.a NULLS FIRST;",
                expected = Datum.array(
                    listOf(
                        Datum.struct(Field.of("a", Datum.nullValue()), Field.of("b", Datum.integer(1))),
                        Datum.struct(Field.of("a", Datum.integer(1)), Field.of("b", Datum.integer(2))),
                        Datum.struct(Field.of("a", Datum.integer(3)), Field.of("b", Datum.integer(4)))
                    )
                )
            ),
            SuccessTestCase(
                input = "SELECT * FROM <<{'a': NULL, 'b': 1}, {'a': 1, 'b': 2}, {'a': 3, 'b': 4}>> AS t ORDER BY t.a DESC NULLS LAST;",
                expected = Datum.array(
                    listOf(
                        Datum.struct(Field.of("a", Datum.integer(3)), Field.of("b", Datum.integer(4))),
                        Datum.struct(Field.of("a", Datum.integer(1)), Field.of("b", Datum.integer(2))),
                        Datum.struct(Field.of("a", Datum.nullValue()), Field.of("b", Datum.integer(1)))
                    )
                )
            ),
            SuccessTestCase(
                input = "SELECT * FROM <<{'a': NULL, 'b': 1}, {'a': 1, 'b': 2}, {'a': 3, 'b': 4}>> AS t ORDER BY t.a DESC NULLS FIRST;",
                expected = Datum.array(
                    listOf(
                        Datum.struct(Field.of("a", Datum.nullValue()), Field.of("b", Datum.integer(1))),
                        Datum.struct(Field.of("a", Datum.integer(3)), Field.of("b", Datum.integer(4))),
                        Datum.struct(Field.of("a", Datum.integer(1)), Field.of("b", Datum.integer(2)))
                    )
                )
            ),
            SuccessTestCase( // use multiple sort specs
                input = "SELECT * FROM <<{'a': NULL, 'b': 1}, {'a': 1, 'b': 2}, {'a': 1, 'b': 4}>> AS t ORDER BY t.a DESC NULLS FIRST, t.b DESC;",
                expected = Datum.array(
                    listOf(
                        Datum.struct(Field.of("a", Datum.nullValue()), Field.of("b", Datum.integer(1))),
                        Datum.struct(Field.of("a", Datum.integer(1)), Field.of("b", Datum.integer(4))),
                        Datum.struct(Field.of("a", Datum.integer(1)), Field.of("b", Datum.integer(2)))
                    )
                )
            ),
            // PartiQL Specification Section 7.1 -- Inputs with wrong types Example 28 (1)
            // According to the Specification, in permissive mode, functions/operators return missing when one of
            //  the parameters is missing.
            SuccessTestCase(
                input = "SELECT VALUE 5 + v FROM <<1, MISSING>> AS v;",
                expected = Datum.bagVararg(Datum.integer(6), Datum.missing())
            ),
            // PartiQL Specification Section 7.1 -- Inputs with wrong types Example 28 (1)
            // See https://github.com/partiql/partiql-tests/pull/118 for more information.
            SuccessTestCase(
                input = "SELECT VALUE 5 + v FROM <<1, MISSING>> AS v;",
                expected = Datum.bagVararg(Datum.integer(6), Datum.missing()),
                mode = Mode.STRICT(),
            ),
        )

        @JvmStatic
        fun typingModeTestCases() = listOf(
            TypingTestCase(
                name = "Expected missing value in collection",
                input = "SELECT VALUE t.a FROM << { 'a': 1 }, { 'b': 2 } >> AS t;",
                expectedPermissive = Datum.bagVararg(Datum.integer(1), Datum.missing())
            ),
            TypingTestCase(
                name = "Expected missing value in tuple in collection",
                input = "SELECT t.a AS \"a\" FROM << { 'a': 1 }, { 'b': 2 } >> AS t;",
                expectedPermissive = Datum.bagVararg(
                    Datum.struct(
                        Field.of("a", Datum.integer(1))
                    ),
                    Datum.struct()
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 4.2 -- index negative",
                input = "[1,2,3][-1];",
                expectedPermissive = Datum.missing()
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 4.2 -- out of bounds",
                input = "[1,2,3][3];",
                expectedPermissive = Datum.missing()
            ),
            TypingTestCase(
                name = "PartiQL Spec Section 5.1.1 -- Position variable on bags",
                input = "SELECT v, p FROM << 5 >> AS v AT p;",
                expectedPermissive = Datum.bagVararg(
                    Datum.struct(
                        Field.of("v", Datum.integer(5))
                    )
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 5.1.1 -- Iteration over a scalar value",
                input = "SELECT v FROM 0 AS v;",
                expectedPermissive = Datum.bagVararg(
                    Datum.struct(
                        Field.of("v", Datum.integer(0))
                    )
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 5.1.1 -- Iteration over a scalar value (with at)",
                input = "SELECT v, p FROM 0 AS v AT p;",
                expectedPermissive = Datum.bagVararg(
                    Datum.struct(
                        Field.of("v", Datum.integer(0))
                    )
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 5.1.1 -- Iteration over a tuple value",
                input = "SELECT v.a AS a FROM { 'a': 1 } AS v;",
                expectedPermissive = Datum.bagVararg(
                    Datum.struct(
                        Field.of("a", Datum.integer(1))
                    )
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 5.1.1 -- Iteration over an absent value (missing)",
                input = "SELECT v AS v FROM MISSING AS v;",
                expectedPermissive = Datum.bagVararg(Datum.struct())
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 5.1.1 -- Iteration over an absent value (null)",
                input = "SELECT v AS v FROM NULL AS v;",
                expectedPermissive = Datum.bagVararg(
                    Datum.struct(
                        Field.of("v", Datum.nullValue())
                    )
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 6.1.4 -- when constructing tuples",
                input = "SELECT VALUE {'a':v.a, 'b':v.b} FROM [{'a':1, 'b':1}, {'a':2}] AS v;",
                expectedPermissive = Datum.bagVararg(
                    Datum.struct(
                        Field.of("a", Datum.integer(1)),
                        Field.of("b", Datum.integer(1))
                    ),
                    Datum.struct(
                        Field.of("a", Datum.integer(2))
                    )
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 6.1.4 -- when constructing bags (1)",
                input = "SELECT VALUE v.b FROM [{'a':1, 'b':1}, {'a':2}] AS v;",
                expectedPermissive = Datum.bagVararg(
                    Datum.integer(1),
                    Datum.missing()
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 6.1.4 -- when constructing bags (2)",
                input = "SELECT VALUE <<v.a, v.b>> FROM [{'a':1, 'b':1}, {'a':2}] AS v;",
                expectedPermissive = Datum.bagVararg(
                    Datum.bagVararg(
                        Datum.integer(1),
                        Datum.integer(1)
                    ),
                    Datum.bagVararg(
                        Datum.integer(2),
                        Datum.missing()
                    )
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 6.2 -- Pivoting a Collection into a Variable-Width Tuple",
                input = "PIVOT t.price AT t.\"symbol\" FROM [{'symbol':25, 'price':31.52}, {'symbol':'amzn', 'price':840.05}] AS t;",
                expectedPermissive = Datum.struct(
                    Field.of("amzn", Datum.decimal(BigDecimal.valueOf(840.05)))
                )
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 7.1 -- Inputs with wrong types Example 28 (3)",
                input = "SELECT VALUE NOT v FROM << false, {'a':1} >> AS v;",
                expectedPermissive = Datum.bagVararg(Datum.bool(true), Datum.missing())
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 7.1 -- Inputs with wrong types Example 28 (2)",
                input = "SELECT VALUE 5 > v FROM <<1, 'a'>> AS v;",
                expectedPermissive = Datum.bagVararg(Datum.bool(true), Datum.missing())
            ),
            TypingTestCase(
                name = "PartiQL Specification Section 9.1",
                input = """
                    SELECT
                        o.name AS orderName,
                        (SELECT c.name FROM << { 'name': 'John', 'id': 1 }, { 'name': 'Alan', 'id': 1 } >> c WHERE c.id=o.custId) AS customerName
                        FROM << { 'name': 'apples', 'custId': 1 } >> o
                """.trimIndent(),
                expectedPermissive = Datum.bagVararg(
                    Datum.struct(
                        Field.of("orderName", Datum.string("apples"))
                    )
                )
            )
        )

        @JvmStatic
        fun intervalAbsTestCases() = listOf(
            // Year-Month interval tests
            SuccessTestCase(
                input = "ABS(INTERVAL '1-6' YEAR TO MONTH)",
                expected = Datum.intervalYearMonth(1, 6, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '+1-6' YEAR TO MONTH)",
                expected = Datum.intervalYearMonth(1, 6, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-1-6' YEAR TO MONTH)",
                expected = Datum.intervalYearMonth(1, 6, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '100-6' YEAR(3) TO MONTH)",
                expected = Datum.intervalYearMonth(100, 6, 3)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-100-6' YEAR(3) TO MONTH)",
                expected = Datum.intervalYearMonth(100, 6, 3)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '5' YEAR)",
                expected = Datum.intervalYear(5, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-5' YEAR)",
                expected = Datum.intervalYear(5, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '100' MONTH(3))",
                expected = Datum.intervalMonth(100, 3)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-100' MONTH(3))",
                expected = Datum.intervalMonth(100, 3)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '0-0' YEAR TO MONTH)",
                expected = Datum.intervalYearMonth(0, 0, 2)
            ),
            // Day-Time interval tests  
            SuccessTestCase(
                input = "ABS(INTERVAL '5' DAY)",
                expected = Datum.intervalDay(5, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-5' DAY)",
                expected = Datum.intervalDay(5, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '100' DAY(3))",
                expected = Datum.intervalDay(100, 3)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-100' DAY(3))",
                expected = Datum.intervalDay(100, 3)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '3' HOUR)",
                expected = Datum.intervalHour(3, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-3' HOUR)",
                expected = Datum.intervalHour(3, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '30' MINUTE)",
                expected = Datum.intervalMinute(30, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-30' MINUTE)",
                expected = Datum.intervalMinute(30, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '45.123' SECOND)",
                expected = Datum.intervalSecond(45, 123000000, 2, 6)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-45.123' SECOND)",
                expected = Datum.intervalSecond(45, 123000000, 2, 6)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '123.456789' SECOND(3,6))",
                expected = Datum.intervalSecond(123, 456789000, 3, 6)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-123.456789' SECOND(3,6))",
                expected = Datum.intervalSecond(123, 456789000, 3, 6)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '1 2' DAY TO HOUR)",
                expected = Datum.intervalDayHour(1, 2, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-1 2' DAY TO HOUR)",
                expected = Datum.intervalDayHour(1, 2, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '1 2:30' DAY TO MINUTE)",
                expected = Datum.intervalDayMinute(1, 2, 30, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-1 2:30' DAY TO MINUTE)",
                expected = Datum.intervalDayMinute(1, 2, 30, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '100 10:30' DAY(3) TO MINUTE)",
                expected = Datum.intervalDayMinute(100, 10, 30, 3)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-100 10:30' DAY(3) TO MINUTE)",
                expected = Datum.intervalDayMinute(100, 10, 30, 3)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '1 2:30:45.567' DAY TO SECOND)",
                expected = Datum.intervalDaySecond(1, 2, 30, 45, 567000000, 2, 6)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-1 2:30:45.567' DAY TO SECOND)",
                expected = Datum.intervalDaySecond(1, 2, 30, 45, 567000000, 2, 6)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '100 10:30:45.123456' DAY(3) TO SECOND(6))",
                expected = Datum.intervalDaySecond(100, 10, 30, 45, 123456000, 3, 6)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-100 10:30:45.123456' DAY(3) TO SECOND(6))",
                expected = Datum.intervalDaySecond(100, 10, 30, 45, 123456000, 3, 6)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '2:30' HOUR TO MINUTE)",
                expected = Datum.intervalHourMinute(2, 30, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-2:30' HOUR TO MINUTE)",
                expected = Datum.intervalHourMinute(2, 30, 2)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '2:30:45.789' HOUR TO SECOND)",
                expected = Datum.intervalHourSecond(2, 30, 45, 789000000, 2, 6)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-2:30:45.789' HOUR TO SECOND)",
                expected = Datum.intervalHourSecond(2, 30, 45, 789000000, 2, 6)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '30:45.123' MINUTE TO SECOND)",
                expected = Datum.intervalMinuteSecond(30, 45, 123000000, 2, 6)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '-30:45.123' MINUTE TO SECOND)",
                expected = Datum.intervalMinuteSecond(30, 45, 123000000, 2, 6)
            ),
            SuccessTestCase(
                input = "ABS(INTERVAL '0 0:0:0' DAY TO SECOND)",
                expected = Datum.intervalDaySecond(0, 0, 0, 0, 0, 2, 6)
            )
        )
    }

    @Test
    fun proveThatRowWorksWhenDynamic() {
        val tc =
            SuccessTestCase(
                input = "t.a = 3",
                expected = Datum.bool(true),
                mode = Mode.STRICT(),
                globals = listOf(
                    Global(
                        name = "t",
                        type = PType.dynamic(),
                        value = Datum.row(Field.of("a", Datum.integer(3)))
                    )
                )
            )
        tc.run()
    }

    @Test
    fun proveThatRowWorks() {
        val tc =
            SuccessTestCase(
                input = "t.a = 3",
                expected = Datum.bool(true),
                mode = Mode.STRICT(),
                globals = listOf(
                    Global(
                        name = "t",
                        type = PType.row(PTypeField.of("a", PType.integer())),
                        value = Datum.row(Field.of("a", Datum.integer(3)))
                    ),
                )
            )
        tc.run()
    }

    @Test
    // @Disabled
    fun developmentTest() {
        val tc =
            SuccessTestCase(
                input = """
                    non_existing_column = 1
                """.trimIndent(),
                expected = Datum.nullValue(PType.bool())
            )
        tc.run()
    }

    @Test
    @Disabled("We need to support section 5.1")
    fun testTypingOfPositionVariable() = TypingTestCase(
        name = "PartiQL Spec Section 5.1.1 -- Position variable on bags",
        input = "SELECT v, p FROM << 5 >> AS v AT p;",
        expectedPermissive = Datum.bagVararg(
            Datum.struct(
                Field.of("v", Datum.integer(5))
            )
        )
    ).run()

    @Test
    @Disabled("This is just a placeholder. We should add support for this. Grouping is not yet supported.")
    fun test3() =
        TypingTestCase(
            name = "PartiQL Specification Section 11.1",
            input = """
                    PLACEHOLDER FOR THE EXAMPLE IN THE RELEVANT SECTION. GROUPING NOT YET SUPPORTED.
            """.trimIndent(),
            expectedPermissive = Datum.missing()
        ).run()

    @Test
    @Disabled("The planner fails this, though it should pass for permissive mode.")
    fun test5() =
        TypingTestCase(
            name = "PartiQL Specification Section 5.2.1 -- Mistyping Cases",
            input = "SELECT v, n FROM UNPIVOT 1 AS v AT n;",
            expectedPermissive = Datum.bagVararg(
                Datum.struct(
                    Field.of("v", Datum.integer(1)),
                    Field.of("n", Datum.string("_1"))
                )
            )
        ).run()

    @Test
    @Disabled("We don't yet support arrays.")
    fun test7() =
        TypingTestCase(
            name = "PartiQL Specification Section 6.1.4 -- when constructing arrays",
            input = "SELECT VALUE [v.a, v.b] FROM [{'a':1, 'b':1}, {'a':2}] AS v;",
            expectedPermissive = Datum.bagVararg(
                Datum.array(
                    listOf(
                        Datum.integer(1),
                        Datum.integer(1)
                    )
                ),
                Datum.array(
                    listOf(
                        Datum.integer(2),
                        Datum.missing()
                    )
                )
            )
        ).run()

    @Test
    @Disabled("There is a bug in the planner which makes this always return missing.")
    fun test8() =
        TypingTestCase(
            name = "PartiQL Specification Section 4.2 -- non integer index",
            input = "SELECT VALUE [1,2,3][v] FROM <<1, 1.0>> AS v;",
            expectedPermissive = Datum.bagVararg(Datum.integer(2), Datum.missing())
        ).run()

    @Test
    @Disabled("CASTs aren't supported yet.")
    fun test9() =
        TypingTestCase(
            name = "PartiQL Specification Section 7.1 -- Inputs with wrong types Example 27",
            input = "SELECT VALUE {'a':3*v.a, 'b':3*(CAST (v.b AS INTEGER))} FROM [{'a':1, 'b':'1'}, {'a':2}] v;",
            expectedPermissive = Datum.bagVararg(
                Datum.struct(
                    Field.of("a", Datum.integer(3)),
                    Field.of("b", Datum.integer(3))
                ),
                Datum.struct(
                    Field.of("a", Datum.integer(6))
                )
            )
        ).run()

    @Test
    @Disabled("Arrays aren't supported yet.")
    fun test10() =
        SuccessTestCase(
            input = "SELECT v, i FROM [ 'a', 'b', 'c' ] AS v AT i",
            expected = Datum.bagVararg(
                Datum.struct(
                    Field.of("v", Datum.string("a")),
                    Field.of("i", Datum.bigint(0))
                ),
                Datum.struct(
                    Field.of("v", Datum.string("b")),
                    Field.of("i", Datum.bigint(1))
                ),
                Datum.struct(
                    Field.of("v", Datum.string("c")),
                    Field.of("i", Datum.bigint(2))
                )
            )
        ).run()

    @Test
    @Disabled(
        """
            We currently do not have support for consolidating collections containing MISSING/NULL. The current
            result (value) is correct. However, the types are slightly wrong due to the SUM__ANY_ANY being resolved.
        """
    )
    fun aggregationOnLiteralBagOfStructs() = SuccessTestCase(
        input = """
            SELECT
                gk_0, SUM(t.c) AS t_c_sum
            FROM <<
                { 'b': NULL, 'c': 1 },
                { 'b': MISSING, 'c': 2 },
                { 'b': 1, 'c': 1 },
                { 'b': 1, 'c': 2 },
                { 'b': 2, 'c': NULL },
                { 'b': 2, 'c': 2 },
                { 'b': 3, 'c': MISSING },
                { 'b': 3, 'c': 2 },
                { 'b': 4, 'c': MISSING },
                { 'b': 4, 'c': NULL }
            >> AS t GROUP BY t.b AS gk_0;
        """.trimIndent(),
        expected = Datum.bagVararg(
            Datum.struct(
                Field.of("gk_0", Datum.integer(1)),
                Field.of("t_c_sum", Datum.integer(3))
            ),
            Datum.struct(
                Field.of("gk_0", Datum.integer(2)),
                Field.of("t_c_sum", Datum.integer(2))
            ),
            Datum.struct(
                Field.of("gk_0", Datum.integer(3)),
                Field.of("t_c_sum", Datum.integer(2))
            ),
            Datum.struct(
                Field.of("gk_0", Datum.integer(4)),
                Field.of("t_c_sum", Datum.nullValue(PType.integer()))
            ),
            Datum.struct(
                Field.of("gk_0", Datum.nullValue()),
                Field.of("t_c_sum", Datum.integer(3))
            )
        ),
        mode = Mode.PERMISSIVE()
    ).run()

    // PartiQL Specification Section 8
    @Test
    @Disabled("Currently, .check(<Datum>) is failing for MISSING. This needs to be resolved.")
    fun missingAndTruePermissive() =
        SuccessTestCase(
            input = "MISSING AND TRUE;",
            expected = Datum.nullValue(PType.bool()),
        ).run()

    // PartiQL Specification Section 8
    @Test
    @Disabled("Currently, .check(<Datum>) is failing for MISSING. This needs to be resolved.")
    fun missingAndTrueStrict() = SuccessTestCase(
        input = "MISSING AND TRUE;",
        expected = Datum.nullValue(PType.bool()), // TODO: Is this right?
        mode = Mode.STRICT()
    ).run()

    @Test
    @Disabled("Support for ORDER BY needs to be added for this to pass.")
    // PartiQL Specification says that SQL's SELECT is coerced, but SELECT VALUE is not.
    fun selectValueNoCoercion() =
        SuccessTestCase(
            input = """
                (4, 5) < (SELECT VALUE t.a FROM << { 'a': 3 }, { 'a': 4 } >> AS t ORDER BY t.a)
            """.trimIndent(),
            expected = Datum.bool(false)
        ).run()

    @Test
    @Disabled("This is appropriately coerced, but this test is failing because LT currently doesn't support LISTS.")
    fun rowCoercion() =
        SuccessTestCase(
            input = """
                (4, 5) < (SELECT t.a, t.a FROM << { 'a': 3 } >> AS t)
            """.trimIndent(),
            expected = Datum.bool(false)
        ).run()

    @Test
    @Disabled("This broke in its introduction to the codebase on merge. See 5fb9a1ccbc7e630b0df62aa8b161d319c763c1f6.")
    // TODO: Add to conformance tests
    fun wildCard() =
        SuccessTestCase(
            input = """
             [
               { 'id':'5',
                 'books':[
                   { 'title':'A',
                     'price':5.0,
                     'authors': [{'name': 'John'}, {'name': 'Doe'}]
                   },
                   { 'title':'B',
                     'price':2.0,
                     'authors': [{'name': 'Zoe'}, {'name': 'Bill'}]
                   }
                 ]
               },
               { 'id':'6',
                 'books':[
                   { 'title':'A',
                     'price':5.0,
                     'authors': [{'name': 'John'}, {'name': 'Doe'}]
                   },
                   { 'title':'E',
                     'price':2.0,
                     'authors': [{'name': 'Zoe'}, {'name': 'Bill'}]
                   }
                 ]
               },
               { 'id':7,
                 'books':[]
               }
             ][*].books[*].authors[*].name
            """.trimIndent(),
            expected = Datum.bagVararg(
                Datum.string("John"), Datum.string("Doe"), Datum.string("Zoe"), Datum.string("Bill"),
                Datum.string("John"), Datum.string("Doe"), Datum.string("Zoe"), Datum.string("Bill")
            )
        ).run()

    @Test
    @Disabled("This broke in its introduction to the codebase on merge. See 5fb9a1ccbc7e630b0df62aa8b161d319c763c1f6.")
    // TODO: add to conformance tests
    // Note that the existing pipeline produced identical result when supplying with
    // SELECT VALUE v2.name FROM e as v0, v0.books as v1, unpivot v1.authors as v2;
    // But it produces different result when supplying with e[*].books[*].authors.*
    // <<
    //  <<{ 'name': 'John'},{'name': 'Doe'} >>,
    //  ...
    // >>
    fun unpivot() =
        SuccessTestCase(
            input = """
             [
               { 'id':'5',
                 'books':[
                   { 'title':'A',
                     'price':5.0,
                     'authors': {
                      'first': {'name': 'John'},
                      'second': {'name': 'Doe'}
                     }
                   },
                   { 'title':'B',
                     'price':2.0,
                     'authors': {
                      'first': {'name': 'Zoe'}, 
                      'second': {'name': 'Bill'}
                     }
                   }
                 ]
               },
               { 'id':'6',
                 'books':[
                   { 'title':'A',
                     'price':5.0,
                     'authors': {
                      'first': {'name': 'John'},
                      'second': {'name': 'Doe'}
                     }
                   },
                   { 'title':'E',
                     'price':2.0,
                     'authors': {
                      'first': {'name': 'Zoe'}, 
                      'second': {'name': 'Bill'}
                     }
                   }
                 ]
               },
               { 'id':7,
                 'books':[]
               }
             ][*].books[*].authors.*.name
            """.trimIndent(),
            expected = Datum.bagVararg(
                Datum.string("John"), Datum.string("Doe"), Datum.string("Zoe"), Datum.string("Bill"),
                Datum.string("John"), Datum.string("Doe"), Datum.string("Zoe"), Datum.string("Bill")
            )
        ).run()
}
