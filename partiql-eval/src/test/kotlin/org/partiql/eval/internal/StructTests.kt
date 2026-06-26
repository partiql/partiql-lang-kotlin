package org.partiql.eval.internal

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Field

class StructTests {

    @ParameterizedTest
    @MethodSource("unpivotTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun unpivotTests(tc: SuccessTestCase) = tc.run()

    companion object {

        private val catalogGlobals = listOf(
            Global(
                name = "users",
                value = Datum.bag(
                    listOf(
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Alice")),
                                Field.of(
                                    "settings",
                                    Datum.struct(
                                        listOf(
                                            Field.of("theme", Datum.string("dark")),
                                            Field.of("lang", Datum.string("en")),
                                        )
                                    )
                                ),
                            )
                        ),
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Bob")),
                                Field.of(
                                    "settings",
                                    Datum.struct(
                                        listOf(
                                            Field.of("theme", Datum.string("light")),
                                            Field.of("lang", Datum.string("fr")),
                                        )
                                    )
                                ),
                            )
                        ),
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Charlie")),
                                Field.of("settings", Datum.nullValue(PType.struct())),
                            )
                        ),
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Peter")),
                                Field.of("settings", Datum.nullValue()), // Untyped null
                            )
                        ),
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Dana")),
                            )
                        ),
                    )
                ),
            ),
        )

        @JvmStatic
        fun unpivotTestCases() = listOf(
            // SELECT * from UNPIVOT — exercises the isUnpivot path in NormalizeSelect
            SuccessTestCase(
                name = "SELECT * FROM UNPIVOT struct with AS and AT",
                input = "SELECT * FROM UNPIVOT {'a': 1, 'b': 2} AS v AT k;",
                expected = Datum.bag(
                    listOf(
                        Datum.struct(
                            listOf(
                                Field.of("v", Datum.integer(1)),
                                Field.of("k", Datum.string("a")),
                            )
                        ),
                        Datum.struct(
                            listOf(
                                Field.of("v", Datum.integer(2)),
                                Field.of("k", Datum.string("b")),
                            )
                        ),
                    )
                ),
            ),
            SuccessTestCase(
                name = "SELECT * FROM UNPIVOT struct with only AT",
                input = "SELECT * FROM UNPIVOT {'a': 1, 'b': 2} AT k;",
                expected = Datum.bag(
                    listOf(
                        Datum.struct(
                            listOf(
                                Field.of("_0", Datum.integer(1)),
                                Field.of("k", Datum.string("a")),
                            )
                        ),
                        Datum.struct(
                            listOf(
                                Field.of("_0", Datum.integer(2)),
                                Field.of("k", Datum.string("b")),
                            )
                        ),
                    )
                ),
            ),
            SuccessTestCase(
                name = "SELECT * FROM UNPIVOT struct with only AS",
                input = "SELECT * FROM UNPIVOT {'a': 1, 'b': 2} AS v;",
                expected = Datum.bag(
                    listOf(
                        Datum.struct(
                            listOf(
                                Field.of("v", Datum.integer(1)),
                                Field.of("_0", Datum.string("a")),
                            )
                        ),
                        Datum.struct(
                            listOf(
                                Field.of("v", Datum.integer(2)),
                                Field.of("_0", Datum.string("b")),
                            )
                        ),
                    )
                ),
            ),
            SuccessTestCase(
                name = "SELECT * FROM UNPIVOT struct without AS or AT",
                input = "SELECT * FROM UNPIVOT {'a': 1, 'b': 2};",
                expected = Datum.bag(
                    listOf(
                        Datum.struct(
                            listOf(
                                Field.of("_0", Datum.integer(1)),
                                Field.of("_1", Datum.string("a")),
                            )
                        ),
                        Datum.struct(
                            listOf(
                                Field.of("_0", Datum.integer(2)),
                                Field.of("_1", Datum.string("b")),
                            )
                        ),
                    )
                ),
            ),
            SuccessTestCase(
                name = "SELECT * FROM UNPIVOT empty struct",
                input = "SELECT * FROM UNPIVOT {} AS v AT k;",
                expected = Datum.bag(emptyList()),
            ),
            /* TODO: https://github.com/partiql/partiql-lang-kotlin/issues/1930
            SuccessTestCase(
                name = "UNPIVOT struct column with WHERE filter (iterates all rows including null)",
                globals = catalogGlobals,
                input = "SELECT u.name, k AS setting_name, v AS setting_value FROM users AS u, UNPIVOT u.settings AS v AT k WHERE u.name = 'Alice';",
                expected = Datum.bag(
                    listOf(
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Alice")),
                                Field.of("setting_name", Datum.string("theme")),
                                Field.of("setting_value", Datum.string("dark")),
                            )
                        ),
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Alice")),
                                Field.of("setting_name", Datum.string("lang")),
                                Field.of("setting_value", Datum.string("en")),
                            )
                        ),
                    )
                ),
            ),*/
            SuccessTestCase(
                name = "SELECT * FROM join with UNPIVOT struct column (pre-filtered to Alice)",
                globals = catalogGlobals,
                input = "SELECT * FROM (SELECT * FROM users WHERE users.name = 'Alice') AS u, UNPIVOT u.settings AS v AT k;",
                expected = Datum.bag(
                    listOf(
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Alice")),
                                Field.of(
                                    "settings",
                                    Datum.struct(
                                        listOf(
                                            Field.of("theme", Datum.string("dark")),
                                            Field.of("lang", Datum.string("en")),
                                        )
                                    )
                                ),
                                Field.of("v", Datum.string("dark")),
                                Field.of("k", Datum.string("theme")),
                            )
                        ),
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Alice")),
                                Field.of(
                                    "settings",
                                    Datum.struct(
                                        listOf(
                                            Field.of("theme", Datum.string("dark")),
                                            Field.of("lang", Datum.string("en")),
                                        )
                                    )
                                ),
                                Field.of("v", Datum.string("en")),
                                Field.of("k", Datum.string("lang")),
                            )
                        ),
                    )
                ),
            ),
            SuccessTestCase(
                name = "UNPIVOT struct column pre-filtered to Alice (returns key-value pairs)",
                globals = catalogGlobals,
                input = "SELECT u.name, k AS setting_name, v AS setting_value FROM (SELECT * FROM users WHERE users.name = 'Alice') AS u, UNPIVOT u.settings AS v AT k;",
                expected = Datum.bag(
                    listOf(
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Alice")),
                                Field.of("setting_name", Datum.string("theme")),
                                Field.of("setting_value", Datum.string("dark")),
                            )
                        ),
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Alice")),
                                Field.of("setting_name", Datum.string("lang")),
                                Field.of("setting_value", Datum.string("en")),
                            )
                        ),
                    )
                ),
            ),
            /* TODO: https://github.com/partiql/partiql-lang-kotlin/issues/1930
            SuccessTestCase(
                name = "UNPIVOT struct column pre-filtered to Charlie (typed null wraps as _1)",
                globals = catalogGlobals,
                input = "SELECT u.name, k AS setting_name, v AS setting_value FROM (SELECT * FROM users WHERE users.name = 'Charlie') AS u, UNPIVOT u.settings AS v AT k;",
                expected = Datum.bag(
                    listOf(
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Charlie")),
                                Field.of("setting_name", Datum.string("_1")),
                                Field.of("setting_value", Datum.nullValue()),
                            )
                        ),
                    )
                ),
            ),
            */
            SuccessTestCase(
                name = "UNPIVOT struct column pre-filtered to Peter (untyped null wraps as _1)",
                globals = catalogGlobals,
                input = "SELECT u.name, k AS setting_name, v AS setting_value FROM (SELECT * FROM users WHERE users.name = 'Peter') AS u, UNPIVOT u.settings AS v AT k;",
                expected = Datum.bag(
                    listOf(
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Peter")),
                                Field.of("setting_name", Datum.string("_1")),
                                Field.of("setting_value", Datum.nullValue()),
                            )
                        ),
                    )
                ),
            ),
            SuccessTestCase(
                name = "UNPIVOT struct column pre-filtered to Dana (missing field returns empty)",
                globals = catalogGlobals,
                input = "SELECT u.name, k AS setting_name, v AS setting_value FROM (SELECT * FROM users WHERE users.name = 'Dana') AS u, UNPIVOT u.settings AS v AT k;",
                expected = Datum.bag(emptyList()),
            ),
        )
    }
}
