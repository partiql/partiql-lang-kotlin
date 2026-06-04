package org.partiql.eval.internal

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.eval.Mode
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Entry

class MapTests {

    @ParameterizedTest
    @MethodSource("constructionTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun constructionTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("accessTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun accessTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("accessFailureTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun accessFailureTests(tc: FailureTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("catalogTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun catalogTests(tc: SuccessTestCase) = tc.run()

    companion object {

        @JvmStatic
        fun constructionTestCases() = listOf(
            SuccessTestCase(
                name = "Empty MAP constructor",
                input = "MAP {};",
                expected = Datum.map(
                    PType.string(),
                    PType.dynamic(),
                    emptyList()
                ),
            ),
            SuccessTestCase(
                name = "MAP constructor with string keys",
                input = "MAP { 'a': 1, 'b': 2 };",
                expected = Datum.map(
                    PType.string(),
                    PType.integer(),
                    listOf(
                        Entry.of(Datum.string("a"), Datum.integer(1)),
                        Entry.of(Datum.string("b"), Datum.integer(2)),
                    )
                ),
            ),
            SuccessTestCase(
                name = "MAP with integer keys",
                input = "MAP { 1: 'a', 2: 'b', 3: 'c' };",
                expected = Datum.map(
                    PType.integer(),
                    PType.string(),
                    listOf(
                        Entry.of(Datum.integer(1), Datum.string("a")),
                        Entry.of(Datum.integer(2), Datum.string("b")),
                        Entry.of(Datum.integer(3), Datum.string("c")),
                    )
                ),
            ),
            SuccessTestCase(
                name = "MAP with expression as key",
                input = "MAP { 1+1: 'two', 2+1: 'three' };",
                expected = Datum.map(
                    PType.integer(),
                    PType.string(),
                    listOf(
                        Entry.of(Datum.integer(2), Datum.string("two")),
                        Entry.of(Datum.integer(3), Datum.string("three")),
                    )
                ),
            ),
            SuccessTestCase(
                name = "MAP key coercion with numeric types (last-write-wins)",
                input = "MAP { 1: 'first', 1.0: 'second' };",
                expected = Datum.map(
                    PType.decimal(38, 19),
                    PType.string(),
                    listOf(
                        Entry.of(Datum.decimal(java.math.BigDecimal("1.0"), 38, 19), Datum.string("second")),
                    )
                ),
            ),
        )

        @JvmStatic
        fun accessTestCases() = listOf(
            SuccessTestCase(
                name = "MAP key access with bracket notation",
                input = "MAP { 'a': 1, 'b': 2 }['a'];",
                expected = Datum.integer(1),
            ),
            SuccessTestCase(
                name = "MAP nonexistent key returns MISSING (permissive)",
                mode = Mode.PERMISSIVE(),
                input = "MAP { 'a': 1 }['z'];",
                expected = Datum.missing(),
            ),
            SuccessTestCase(
                name = "MAP null key access returns NULL",
                mode = Mode.PERMISSIVE(),
                input = "MAP { 'a': 1 }[NULL];",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "MAP MISSING key access returns MISSING",
                mode = Mode.PERMISSIVE(),
                input = "MAP { 'a': 1 }[MISSING];",
                expected = Datum.missing(),
            ),
        )

        @JvmStatic
        fun accessFailureTestCases() = listOf(
            FailureTestCase(
                name = "MAP nonexistent key fails (strict)",
                mode = Mode.STRICT(),
                input = "MAP { 'a': 1 }['z'];",
            ),
        )

        private val catalogGlobals = listOf(
            Global(
                name = "users",
                value = Datum.bag(
                    listOf(
                        Datum.struct(
                            listOf(
                                org.partiql.spi.value.Field.of("name", Datum.string("Alice")),
                                org.partiql.spi.value.Field.of(
                                    "settings",
                                    Datum.map(
                                        PType.string(),
                                        PType.string(),
                                        listOf(
                                            Entry.of(Datum.string("theme"), Datum.string("dark")),
                                            Entry.of(Datum.string("lang"), Datum.string("en")),
                                        )
                                    )
                                ),
                                org.partiql.spi.value.Field.of(
                                    "scores",
                                    Datum.map(
                                        PType.integer(),
                                        PType.integer(),
                                        listOf(
                                            Entry.of(Datum.integer(100), Datum.integer(5)),
                                            Entry.of(Datum.integer(200), Datum.integer(10)),
                                        )
                                    )
                                ),
                            )
                        ),
                        Datum.struct(
                            listOf(
                                org.partiql.spi.value.Field.of("name", Datum.string("Bob")),
                                org.partiql.spi.value.Field.of(
                                    "settings",
                                    Datum.map(
                                        PType.string(),
                                        PType.string(),
                                        listOf(
                                            Entry.of(Datum.string("theme"), Datum.string("light")),
                                            Entry.of(Datum.string("lang"), Datum.string("fr")),
                                        )
                                    )
                                ),
                                org.partiql.spi.value.Field.of(
                                    "scores",
                                    Datum.map(
                                        PType.integer(),
                                        PType.integer(),
                                        listOf(
                                            Entry.of(Datum.integer(100), Datum.integer(3)),
                                            Entry.of(Datum.integer(300), Datum.integer(7)),
                                        )
                                    )
                                ),
                            )
                        ),
                        Datum.struct(
                            listOf(
                                org.partiql.spi.value.Field.of("name", Datum.string("Charlie")),
                                org.partiql.spi.value.Field.of(
                                    "settings",
                                    Datum.nullValue(PType.map(PType.string(), PType.string()))
                                ),
                                org.partiql.spi.value.Field.of(
                                    "scores",
                                    Datum.missing()
                                ),
                            )
                        ),
                        Datum.struct(
                            listOf(
                                org.partiql.spi.value.Field.of("name", Datum.string("Dana")),
                            )
                        ),
                    )
                ),
            ),
        )

        @JvmStatic
        fun catalogTestCases() = listOf(
            // --- String key MAP access ---
            SuccessTestCase(
                name = "String key MAP: basic access",
                globals = catalogGlobals,
                input = "SELECT u.name, u.settings['theme'] AS theme FROM users AS u WHERE u.name = 'Alice';",
                expected = Datum.bag(listOf(
                    Datum.struct(listOf(
                        org.partiql.spi.value.Field.of("name", Datum.string("Alice")),
                        org.partiql.spi.value.Field.of("theme", Datum.string("dark")),
                    ))
                )),
            ),
            SuccessTestCase(
                name = "String key MAP: access on null map returns NULL",
                mode = Mode.PERMISSIVE(),
                globals = catalogGlobals,
                input = "SELECT u.name, u.settings['theme'] AS theme FROM users AS u WHERE u.name = 'Charlie';",
                // Charlie has null settings — null propagation returns NULL for theme
                expected = Datum.bag(listOf(
                    Datum.struct(listOf(
                        org.partiql.spi.value.Field.of("name", Datum.string("Charlie")),
                        org.partiql.spi.value.Field.of("theme", Datum.nullValue()),
                    ))
                )),
            ),
            SuccessTestCase(
                name = "String key MAP: access on missing map field excluded from struct",
                mode = Mode.PERMISSIVE(),
                globals = catalogGlobals,
                input = "SELECT u.name, u.settings['theme'] AS theme FROM users AS u WHERE u.name = 'Dana';",
                // Dana has no settings field — MISSING propagates, theme excluded from struct
                expected = Datum.bag(listOf(
                    Datum.struct(listOf(
                        org.partiql.spi.value.Field.of("name", Datum.string("Dana")),
                    ))
                )),
            ),
            // --- Integer key MAP access ---
            SuccessTestCase(
                name = "Integer key MAP: basic access",
                globals = catalogGlobals,
                input = "SELECT u.name, u.scores[100] AS score FROM users AS u WHERE u.name = 'Alice';",
                expected = Datum.bag(listOf(
                    Datum.struct(listOf(
                        org.partiql.spi.value.Field.of("name", Datum.string("Alice")),
                        org.partiql.spi.value.Field.of("score", Datum.integer(5)),
                    ))
                )),
            ),
            SuccessTestCase(
                name = "Integer key MAP: nonexistent key returns empty struct",
                mode = Mode.PERMISSIVE(),
                globals = catalogGlobals,
                input = "SELECT u.name, u.scores[999] AS score FROM users AS u WHERE u.name = 'Alice';",
                // key 999 doesn't exist — MISSING propagates, score excluded
                expected = Datum.bag(listOf(
                    Datum.struct(listOf(
                        org.partiql.spi.value.Field.of("name", Datum.string("Alice")),
                    ))
                )),
            ),
            SuccessTestCase(
                name = "Integer key MAP: access on missing datum excluded from struct",
                mode = Mode.PERMISSIVE(),
                globals = catalogGlobals,
                input = "SELECT u.name, u.scores[100] AS score FROM users AS u WHERE u.name = 'Charlie';",
                // Charlie has scores field present but value is MISSING — score excluded
                expected = Datum.bag(listOf(
                    Datum.struct(listOf(
                        org.partiql.spi.value.Field.of("name", Datum.string("Charlie")),
                    ))
                )),
            ),
            // --- WHERE filter using MAP values ---
            SuccessTestCase(
                name = "WHERE filter on string key map value",
                globals = catalogGlobals,
                input = "SELECT u.name FROM users AS u WHERE u.settings['lang'] = 'fr';",
                expected = Datum.bag(listOf(
                    Datum.struct(listOf(
                        org.partiql.spi.value.Field.of("name", Datum.string("Bob")),
                    ))
                )),
            ),
            // --- All rows with mixed null/missing ---
            SuccessTestCase(
                name = "All rows: string key MAP access shows null/missing behavior",
                mode = Mode.PERMISSIVE(),
                globals = catalogGlobals,
                input = "SELECT u.name, u.settings['theme'] AS theme FROM users AS u;",
                expected = Datum.bag(listOf(
                    Datum.struct(listOf(
                        org.partiql.spi.value.Field.of("name", Datum.string("Alice")),
                        org.partiql.spi.value.Field.of("theme", Datum.string("dark")),
                    )),
                    Datum.struct(listOf(
                        org.partiql.spi.value.Field.of("name", Datum.string("Bob")),
                        org.partiql.spi.value.Field.of("theme", Datum.string("light")),
                    )),
                    // null map → NULL value included
                    Datum.struct(listOf(
                        org.partiql.spi.value.Field.of("name", Datum.string("Charlie")),
                        org.partiql.spi.value.Field.of("theme", Datum.nullValue()),
                    )),
                    // missing field → excluded from struct
                    Datum.struct(listOf(
                        org.partiql.spi.value.Field.of("name", Datum.string("Dana")),
                    )),
                )),
            ),
            // --- Null map vs missing datum on same row (Charlie) ---
            SuccessTestCase(
                name = "Charlie: null map (settings) vs missing datum (scores)",
                mode = Mode.PERMISSIVE(),
                globals = catalogGlobals,
                input = "SELECT u.settings['theme'] AS theme, u.scores[100] AS score FROM users AS u WHERE u.name = 'Charlie';",
                // settings is null MAP → theme = NULL (null propagation, included)
                // scores is MISSING datum → score excluded from struct
                expected = Datum.bag(listOf(
                    Datum.struct(listOf(
                        org.partiql.spi.value.Field.of("theme", Datum.nullValue()),
                    ))
                )),
            ),
            // --- Access on completely nonexistent field ---
            SuccessTestCase(
                name = "Access nonexistent field then key returns empty struct (permissive)",
                mode = Mode.PERMISSIVE(),
                globals = catalogGlobals,
                input = "SELECT u.nonexistent[100] AS v FROM users AS u;",
                expected = Datum.bag(listOf(
                    Datum.struct(emptyList()),
                    Datum.struct(emptyList()),
                    Datum.struct(emptyList()),
                    Datum.struct(emptyList()),
                )),
            ),
        )
    }
}
