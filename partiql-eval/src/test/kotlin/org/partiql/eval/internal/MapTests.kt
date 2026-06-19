package org.partiql.eval.internal

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.eval.Mode
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import org.partiql.spi.value.Entry
import org.partiql.spi.value.Field
import java.math.BigDecimal
import java.time.LocalDate

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
    @MethodSource("sizeTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun sizeTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("existsTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun existsTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("containsKeyTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun containsKeyTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("mapGetTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun mapGetTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("mapKeysTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun mapKeysTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("mapValuesTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun mapValuesTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("mapEntriesTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun mapEntriesTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("cardinalityTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun cardinalityTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("isMapTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun isMapTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("catalogTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun catalogTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("castTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun castTests(tc: SuccessTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("castFailureTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun castFailureTests(tc: FailureTestCase) = tc.run()

    @ParameterizedTest
    @MethodSource("unpivotTestCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun unpivotTests(tc: SuccessTestCase) = tc.run()

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
            SuccessTestCase(
                name = "MAP constructor strips MISSING values",
                input = "MAP { 'a': 1, 'b': MISSING, 'c': 3 };",
                expected = Datum.map(
                    PType.string(),
                    PType.integer(),
                    listOf(
                        Entry.of(Datum.string("a"), Datum.integer(1)),
                        Entry.of(Datum.string("c"), Datum.integer(3)),
                    )
                ),
            ),
            SuccessTestCase(
                name = "MAP constructor with all MISSING values produces empty map",
                input = "MAP { 'a': MISSING, 'b': MISSING };",
                expected = Datum.map(
                    PType.string(),
                    PType.dynamic(),
                    emptyList()
                ),
            ),
            SuccessTestCase(
                name = "MAP constructor with compatible heterogeneous keys coerces to common type",
                input = "MAP { 2: 'two', 1.0: 'yes' };",
                expected = Datum.map(
                    PType.decimal(38, 19),
                    PType.string(),
                    listOf(
                        Entry.of(Datum.decimal(BigDecimal(2), 38, 19), Datum.string("two")),
                        Entry.of(Datum.decimal(BigDecimal("1.0"), 38, 19), Datum.string("yes")),
                    )
                ),
            ),
            SuccessTestCase(
                name = "MAP constructor with compatible heterogeneous values coerces to common type",
                input = "MAP { 'a': 1, 'b': 2.0 };",
                expected = Datum.map(
                    PType.string(),
                    PType.decimal(38, 19),
                    listOf(
                        Entry.of(Datum.string("a"), Datum.decimal(BigDecimal.ONE, 38, 19)),
                        Entry.of(Datum.string("b"), Datum.decimal(BigDecimal("2.0"), 38, 19)),
                    )
                ),
            ),
            SuccessTestCase(
                name = "MAP constructor with incompatible heterogeneous values are resolved to dynamic value type",
                input = "MAP { 'a': 1, 'b': 'c' };",
                expected = Datum.map(
                    PType.string(),
                    PType.dynamic(),
                    listOf(
                        Entry.of(Datum.string("a"), Datum.integer(1)),
                        Entry.of(Datum.string("b"), Datum.string("c")),
                    )
                ),
            ),
            SuccessTestCase(
                name = "MAP with DATE keys",
                input = "MAP { DATE '2024-01-15': 'holiday', DATE '2024-07-04': 'independence' };",
                expected = Datum.map(
                    PType.date(),
                    PType.string(),
                    listOf(
                        Entry.of(Datum.date(LocalDate.of(2024, 1, 15)), Datum.string("holiday")),
                        Entry.of(Datum.date(LocalDate.of(2024, 7, 4)), Datum.string("independence")),
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
            SuccessTestCase(
                name = "MAP integer key access with bracket notation",
                input = "MAP { 1: 'one', 2: 'two' }[1];",
                expected = Datum.string("one"),
            ),
            SuccessTestCase(
                name = "MAP DATE key access with bracket notation",
                input = "MAP { DATE '2024-01-15': 'holiday', DATE '2024-07-04': 'independence' }[DATE '2024-07-04'];",
                expected = Datum.string("independence"),
            ),
            // Key cast tests: lookup key type differs from MAP key type, implicit cast applied
            SuccessTestCase(
                name = "MAP integer key accessed with decimal (implicit cast to INTEGER)",
                input = "MAP { 1: 'one', 2: 'two' }[1.0];",
                expected = Datum.string("one"),
            ),
            SuccessTestCase(
                name = "MAP integer key accessed with bigint (implicit cast to INTEGER)",
                input = "MAP { 1: 'one', 2: 'two' }[CAST(2 AS BIGINT)];",
                expected = Datum.string("two"),
            ),
            SuccessTestCase(
                name = "MAP decimal key accessed with integer (implicit cast to DECIMAL)",
                input = "MAP { 1.0: 'one', 2.0: 'two' }[1];",
                expected = Datum.string("one"),
            ),
            // Static cast: heterogeneous keys coerced to DECIMAL at plan time, lookup with INTEGER
            SuccessTestCase(
                name = "MAP with heterogeneous keys accessed with integer (plan-time coercion to DECIMAL)",
                input = "MAP { 2: 'two', 1.0: 'yes' }[2];",
                expected = Datum.string("two"),
            ),
            SuccessTestCase(
                name = "MAP with heterogeneous keys accessed with decimal (plan-time coercion to DECIMAL)",
                input = "MAP { 2: 'two', 1.0: 'yes' }[1.0];",
                expected = Datum.string("yes"),
            ),
            // Dynamic type: MAP type resolved at runtime via CASE expression
            SuccessTestCase(
                name = "Dynamic MAP from CASE accessed with integer key (runtime cast)",
                input = "(CASE WHEN 1=1 THEN MAP { 1: 'one', 2: 'two' } ELSE MAP { 3: 'three' } END)[1];",
                expected = Datum.string("one"),
            ),
            SuccessTestCase(
                name = "Dynamic MAP from CASE accessed with decimal key (runtime cast)",
                input = "(CASE WHEN 1=1 THEN MAP { 1: 'one', 2: 'two' } ELSE MAP { 3: 'three' } END)[1.0];",
                expected = Datum.string("one"),
            ),
            SuccessTestCase(
                name = "MAP access with explicit cast to different precision",
                input = "MAP { 2: 'two', 1.0: 'yes' }[CAST(1.0 AS DECIMAL(10,1))];",
                expected = Datum.string("yes"),
            ),
        )

        @JvmStatic
        fun accessFailureTestCases() = listOf(
            FailureTestCase(
                name = "MAP nonexistent key fails (strict)",
                mode = Mode.STRICT(),
                input = "MAP { 'a': 1 }['z'];",
            ),
            FailureTestCase(
                name = "MAP nonexistent integer key fails (strict)",
                mode = Mode.STRICT(),
                input = "MAP { 1: 'one', 2: 'two' }[99];",
            ),
        )

        @JvmStatic
        fun sizeTestCases() = listOf(
            SuccessTestCase(
                name = "size of map with entries",
                input = "size(MAP { 'a': 1, 'b': 2, 'c': 3 });",
                expected = Datum.integer(3),
            ),
            SuccessTestCase(
                name = "size of empty map",
                input = "size(MAP { });",
                expected = Datum.integer(0),
            ),
            SuccessTestCase(
                name = "size of map with integer keys",
                input = "size(MAP { 1: 'a', 2: 'b' });",
                expected = Datum.integer(2),
            ),
        )

        @JvmStatic
        fun existsTestCases() = listOf(
            SuccessTestCase(
                name = "exists on non-empty map returns true",
                input = "exists(MAP { 'a': 1 });",
                expected = Datum.bool(true),
            ),
            SuccessTestCase(
                name = "exists on empty map returns false",
                input = "exists(MAP { });",
                expected = Datum.bool(false),
            ),
            SuccessTestCase(
                name = "exists on non-empty map with integer keys returns true",
                input = "exists(MAP { 1: 'a', 2: 'b' });",
                expected = Datum.bool(true),
            ),
        )

        @JvmStatic
        fun containsKeyTestCases() = listOf(
            SuccessTestCase(
                name = "contains_key returns true when key exists",
                input = "contains_key(MAP { 'a': 1, 'b': 2 }, 'a');",
                expected = Datum.bool(true),
            ),
            SuccessTestCase(
                name = "contains_key returns false when key absent",
                input = "contains_key(MAP { 'a': 1, 'b': 2 }, 'z');",
                expected = Datum.bool(false),
            ),
            SuccessTestCase(
                name = "contains_key with integer key",
                input = "contains_key(MAP { 1: 'one', 2: 'two' }, 1);",
                expected = Datum.bool(true),
            ),
            SuccessTestCase(
                name = "contains_key with null key returns null",
                input = "contains_key(MAP { 'a': 1 }, NULL);",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "contains_key with missing key returns missing",
                mode = Mode.PERMISSIVE(),
                input = "contains_key(MAP { 'a': 1 }, MISSING);",
                expected = Datum.missing(),
            ),
            SuccessTestCase(
                name = "contains_key with decimal key matches same precision",
                input = "contains_key(MAP { 1.0: 'a', 2.0: 'b' }, 1.0);",
                expected = Datum.bool(true),
            ),
            SuccessTestCase(
                name = "contains_key with integer key matches decimal map key (cross-type cast)",
                input = "contains_key(MAP { 1.0: 'a', 2.0: 'b' }, 1);",
                expected = Datum.bool(true),
            ),
            SuccessTestCase(
                name = "contains_key with explicit cast to different precision",
                input = "contains_key(MAP { 1.0: 'a', 2.0: 'b' }, CAST(1.0 AS DECIMAL(10,1)));",
                expected = Datum.bool(true),
            ),
        )

        @JvmStatic
        fun mapGetTestCases() = listOf(
            SuccessTestCase(
                name = "map_get returns value for existing key",
                input = "map_get(MAP { 'a': 1, 'b': 2 }, 'b');",
                expected = Datum.integer(2),
            ),
            SuccessTestCase(
                name = "map_get with integer key",
                input = "map_get(MAP { 10: 'ten', 20: 'twenty' }, 10);",
                expected = Datum.string("ten"),
            ),
            SuccessTestCase(
                name = "map_get with null key returns null",
                input = "map_get(MAP { 'a': 1 }, NULL);",
                expected = Datum.nullValue(),
            ),
            SuccessTestCase(
                name = "map_get with nonexistent key returns missing",
                mode = Mode.PERMISSIVE(),
                input = "map_get(MAP { 'a': 1, 'b': 2 }, 'z');",
                expected = Datum.missing(),
            ),
            SuccessTestCase(
                name = "map_get with missing key returns missing",
                mode = Mode.PERMISSIVE(),
                input = "map_get(MAP { 'a': 1 }, MISSING);",
                expected = Datum.missing(),
            ),
            SuccessTestCase(
                name = "map_get with decimal key matches same precision",
                input = "map_get(MAP { 1.0: 'one', 2.0: 'two' }, 1.0);",
                expected = Datum.string("one"),
            ),
            SuccessTestCase(
                name = "map_get with integer key matches decimal map key (cross-type cast)",
                input = "map_get(MAP { 1.0: 'one', 2.0: 'two' }, 1);",
                expected = Datum.string("one"),
            ),
            SuccessTestCase(
                name = "map_get with explicit cast to different precision",
                input = "map_get(MAP { 1.0: 'one', 2.0: 'two' }, CAST(1.0 AS DECIMAL(10,1)));",
                expected = Datum.string("one"),
            ),
        )

        @JvmStatic
        fun mapKeysTestCases() = listOf(
            SuccessTestCase(
                name = "map_keys returns bag of all keys",
                input = "map_keys(MAP { 'x': 1, 'y': 2 });",
                expected = Datum.bag(listOf(Datum.string("x"), Datum.string("y"))),
            ),
            SuccessTestCase(
                name = "map_keys on empty map returns empty bag",
                input = "map_keys(MAP { });",
                expected = Datum.bag(emptyList()),
            ),
            SuccessTestCase(
                name = "map_keys with integer keys",
                input = "map_keys(MAP { 1: 'a', 2: 'b' });",
                expected = Datum.bag(listOf(Datum.integer(1), Datum.integer(2))),
            ),
        )

        @JvmStatic
        fun mapValuesTestCases() = listOf(
            SuccessTestCase(
                name = "map_values returns bag of all values",
                input = "map_values(MAP { 'x': 1, 'y': 2 });",
                expected = Datum.bag(listOf(Datum.integer(1), Datum.integer(2))),
            ),
            SuccessTestCase(
                name = "map_values on empty map returns empty bag",
                input = "map_values(MAP { });",
                expected = Datum.bag(emptyList()),
            ),
            SuccessTestCase(
                name = "map_values with integer keys",
                input = "map_values(MAP { 1: 'a', 2: 'b' });",
                expected = Datum.bag(listOf(Datum.string("a"), Datum.string("b"))),
            ),
        )

        @JvmStatic
        fun mapEntriesTestCases() = listOf(
            SuccessTestCase(
                name = "map_entries returns bag of key-value structs",
                input = "map_entries(MAP { 'a': 1, 'b': 2 });",
                expected = Datum.bag(
                    listOf(
                        Datum.struct(
                            listOf(
                                org.partiql.spi.value.Field.of("k", Datum.string("a")),
                                org.partiql.spi.value.Field.of("v", Datum.integer(1)),
                            )
                        ),
                        Datum.struct(
                            listOf(
                                org.partiql.spi.value.Field.of("k", Datum.string("b")),
                                org.partiql.spi.value.Field.of("v", Datum.integer(2)),
                            )
                        ),
                    )
                ),
            ),
            SuccessTestCase(
                name = "map_entries on empty map returns empty bag",
                input = "map_entries(MAP { });",
                expected = Datum.bag(emptyList()),
            ),
            SuccessTestCase(
                name = "map_entries with integer keys",
                input = "map_entries(MAP { 1: 'a', 2: 'b' });",
                expected = Datum.bag(
                    listOf(
                        Datum.struct(
                            listOf(
                                org.partiql.spi.value.Field.of("k", Datum.integer(1)),
                                org.partiql.spi.value.Field.of("v", Datum.string("a")),
                            )
                        ),
                        Datum.struct(
                            listOf(
                                org.partiql.spi.value.Field.of("k", Datum.integer(2)),
                                org.partiql.spi.value.Field.of("v", Datum.string("b")),
                            )
                        ),
                    )
                ),
            ),
        )

        @JvmStatic
        fun cardinalityTestCases() = listOf(
            SuccessTestCase(
                name = "cardinality of map with entries",
                input = "cardinality(MAP { 'a': 1, 'b': 2, 'c': 3 });",
                expected = Datum.integer(3),
            ),
            SuccessTestCase(
                name = "cardinality of empty map",
                input = "cardinality(MAP { });",
                expected = Datum.integer(0),
            ),
            SuccessTestCase(
                name = "cardinality of map with integer keys",
                input = "cardinality(MAP { 1: 'a', 2: 'b', 3: 'c' });",
                expected = Datum.integer(3),
            ),
        )

        @JvmStatic
        fun isMapTestCases() = listOf(
            SuccessTestCase(
                name = "is_map returns true for a map",
                input = "MAP { 'a': 1 } IS MAP<STRING, INTEGER>;",
                expected = Datum.bool(true),
            ),
            SuccessTestCase(
                name = "is_map returns false for a struct",
                input = "{'a': 1} IS MAP<STRING, INTEGER>;",
                expected = Datum.bool(false),
            ),
            SuccessTestCase(
                name = "is_map returns false for an integer",
                input = "42 IS MAP<STRING, INTEGER>;",
                expected = Datum.bool(false),
            ),
            SuccessTestCase(
                name = "is_map returns true for map with integer keys",
                input = "MAP { 1: 'one', 2: 'two' } IS MAP<INTEGER, STRING>;",
                expected = Datum.bool(true),
            ),
            SuccessTestCase(
                name = "is_map returns false for mismatched value type (DOUBLE PRECISION)",
                input = "MAP { 'a': 1 } IS MAP<STRING, DOUBLE PRECISION>;",
                expected = Datum.bool(false),
            ),
            SuccessTestCase(
                name = "is_map returns false for mismatched value type (BIGINT)",
                input = "MAP { 'a': 1 } IS MAP<STRING, BIGINT>;",
                expected = Datum.bool(false),
            ),
            SuccessTestCase(
                name = "is_map returns false for mismatched key type (INT)",
                input = "MAP { 'a': 1 } IS MAP<INT, STRING>;",
                expected = Datum.bool(false),
            ),
        )

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
                                    Datum.map(
                                        PType.string(),
                                        PType.string(),
                                        listOf(
                                            Entry.of(Datum.string("theme"), Datum.string("dark")),
                                            Entry.of(Datum.string("lang"), Datum.string("en")),
                                        )
                                    )
                                ),
                                Field.of(
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
                                Field.of("name", Datum.string("Bob")),
                                Field.of(
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
                                Field.of(
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
                                Field.of("name", Datum.string("Charlie")),
                                Field.of(
                                    "settings",
                                    Datum.nullValue(PType.map(PType.string(), PType.string()))
                                ),
                                Field.of(
                                    "scores",
                                    Datum.missing()
                                ),
                            )
                        ),
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Peter")),
                                Field.of(
                                    "settings",
                                    Datum.nullValue()
                                ),
                                Field.of(
                                    "scores",
                                    Datum.missing()
                                ),
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
        fun catalogTestCases() = listOf(
            // --- String key MAP access ---
            SuccessTestCase(
                name = "String key MAP: basic access",
                globals = catalogGlobals,
                input = "SELECT u.name, u.settings['theme'] AS theme FROM users AS u WHERE u.name = 'Alice';",
                expected = Datum.bag(
                    listOf(
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Alice")),
                                Field.of("theme", Datum.string("dark")),
                            )
                        ),
                    )
                ),
            ),
            SuccessTestCase(
                name = "String key MAP: access on null map returns NULL",
                mode = Mode.PERMISSIVE(),
                globals = catalogGlobals,
                input = "SELECT u.name, u.settings['theme'] AS theme FROM users AS u WHERE u.name = 'Charlie';",
                // Charlie has null settings — null propagation returns NULL for theme
                expected = Datum.bag(
                    listOf(
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Charlie")),
                                Field.of("theme", Datum.nullValue()),
                            )
                        ),
                    )
                ),
            ),
            SuccessTestCase(
                name = "String key MAP: access on missing map field excluded from struct",
                mode = Mode.PERMISSIVE(),
                globals = catalogGlobals,
                input = "SELECT u.name, u.settings['theme'] AS theme FROM users AS u WHERE u.name = 'Dana';",
                // Dana has no settings field — MISSING propagates, theme excluded from struct
                expected = Datum.bag(
                    listOf(
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Dana")),
                            )
                        ),
                    )
                ),
            ),
            // --- Integer key MAP access ---
            SuccessTestCase(
                name = "Integer key MAP: basic access",
                globals = catalogGlobals,
                input = "SELECT u.name, u.scores[100] AS score FROM users AS u WHERE u.name = 'Alice';",
                expected = Datum.bag(
                    listOf(
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Alice")),
                                Field.of("score", Datum.integer(5)),
                            )
                        ),
                    )
                ),
            ),
            SuccessTestCase(
                name = "Integer key MAP: nonexistent key returns empty struct",
                mode = Mode.PERMISSIVE(),
                globals = catalogGlobals,
                input = "SELECT u.name, u.scores[999] AS score FROM users AS u WHERE u.name = 'Alice';",
                // key 999 doesn't exist — MISSING propagates, score excluded
                expected = Datum.bag(
                    listOf(
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Alice")),
                            )
                        ),
                    )
                ),
            ),
            SuccessTestCase(
                name = "Integer key MAP: access on missing datum excluded from struct",
                mode = Mode.PERMISSIVE(),
                globals = catalogGlobals,
                input = "SELECT u.name, u.scores[100] AS score FROM users AS u WHERE u.name = 'Charlie';",
                // Charlie has scores field present but value is MISSING — score excluded
                expected = Datum.bag(
                    listOf(
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Charlie")),
                            )
                        ),
                    )
                ),
            ),
            // --- WHERE filter using MAP values ---
            SuccessTestCase(
                name = "WHERE filter on string key map value",
                globals = catalogGlobals,
                input = "SELECT u.name FROM users AS u WHERE u.settings['lang'] = 'fr';",
                expected = Datum.bag(
                    listOf(
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Bob")),
                            )
                        ),
                    )
                ),
            ),
            // --- All rows with mixed null/missing ---
            SuccessTestCase(
                name = "All rows: string key MAP access shows null/missing behavior",
                mode = Mode.PERMISSIVE(),
                globals = catalogGlobals,
                input = "SELECT u.name, u.settings['theme'] AS theme FROM users AS u;",
                expected = Datum.bag(
                    listOf(
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Alice")),
                                Field.of("theme", Datum.string("dark")),
                            )
                        ),
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Bob")),
                                Field.of("theme", Datum.string("light")),
                            )
                        ),
                        // typed null map → NULL value with map's value type
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Charlie")),
                                Field.of("theme", Datum.nullValue(PType.string())),
                            )
                        ),
                        // untyped null → NULL value (UNKNOWN type)
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Peter")),
                                Field.of("theme", Datum.nullValue()),
                            )
                        ),
                        // missing field → excluded from struct
                        Datum.struct(
                            listOf(
                                Field.of("name", Datum.string("Dana")),
                            )
                        ),
                    )
                ),
            ),
            // --- Null map vs missing datum on same row (Charlie) ---
            SuccessTestCase(
                name = "Charlie: null map (settings) vs missing datum (scores)",
                mode = Mode.PERMISSIVE(),
                globals = catalogGlobals,
                input = "SELECT u.settings['theme'] AS theme, u.scores[100] AS score FROM users AS u WHERE u.name = 'Charlie';",
                // settings is null MAP → theme = NULL (null propagation, included)
                // scores is MISSING datum → score excluded from struct
                expected = Datum.bag(
                    listOf(
                        Datum.struct(
                            listOf(
                                Field.of("theme", Datum.nullValue()),
                            )
                        ),
                    )
                ),
            ),
            // --- Access on completely nonexistent field ---
            SuccessTestCase(
                name = "Access nonexistent field then key returns empty struct (permissive)",
                mode = Mode.PERMISSIVE(),
                globals = catalogGlobals,
                input = "SELECT u.nonexistent[100] AS v FROM users AS u;",
                expected = Datum.bag(
                    listOf(
                        Datum.struct(emptyList()),
                        Datum.struct(emptyList()),
                        Datum.struct(emptyList()),
                        Datum.struct(emptyList()),
                        Datum.struct(emptyList()),
                    )
                ),
            ),
        )

        @JvmStatic
        fun unpivotTestCases() = listOf(
            SuccessTestCase(
                name = "UNPIVOT literal map with string keys",
                input = "SELECT k, v FROM UNPIVOT MAP { 'a': 1, 'b': 2 } AS v AT k;",
                expected = Datum.bag(
                    listOf(
                        Datum.struct(
                            listOf(
                                Field.of("k", Datum.string("a")),
                                Field.of("v", Datum.integer(1)),
                            )
                        ),
                        Datum.struct(
                            listOf(
                                Field.of("k", Datum.string("b")),
                                Field.of("v", Datum.integer(2)),
                            )
                        ),
                    )
                ),
            ),
            SuccessTestCase(
                name = "UNPIVOT empty literal map",
                input = "SELECT k, v FROM UNPIVOT MAP { } AS v AT k;",
                expected = Datum.bag(emptyList()),
            ),
            SuccessTestCase(
                name = "UNPIVOT literal map with integer keys",
                input = "SELECT k, v FROM UNPIVOT MAP { 1: 'one', 2: 'two' } AS v AT k;",
                expected = Datum.bag(
                    listOf(
                        Datum.struct(
                            listOf(
                                Field.of("k", Datum.integer(1)),
                                Field.of("v", Datum.string("one")),
                            )
                        ),
                        Datum.struct(
                            listOf(
                                Field.of("k", Datum.integer(2)),
                                Field.of("v", Datum.string("two")),
                            )
                        ),
                    )
                ),
            ),
            SuccessTestCase(
                name = "UNPIVOT literal map with decimal keys",
                input = "SELECT k, v FROM UNPIVOT MAP { 1.0: 'a', 2.5: 'b' } AS v AT k;",
                expected = Datum.bag(
                    listOf(
                        Datum.struct(
                            listOf(
                                Field.of("k", Datum.decimal(BigDecimal("1.0"), 38, 19)),
                                Field.of("v", Datum.string("a")),
                            )
                        ),
                        Datum.struct(
                            listOf(
                                Field.of("k", Datum.decimal(BigDecimal("2.5"), 38, 19)),
                                Field.of("v", Datum.string("b")),
                            )
                        ),
                    )
                ),
            ),
            /* TODO: https://github.com/partiql/partiql-lang-kotlin/issues/1930
            SuccessTestCase(
                name = "UNPIVOT map column with WHERE filter (iterates all rows including null)",
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
                name = "UNPIVOT map column pre-filtered to Alice (returns key-value pairs)",
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
                name = "UNPIVOT map column pre-filtered to Charlie (typed null wraps as _1)",
                globals = catalogGlobals,
                input = "SELECT u.name, k AS setting_name, v AS setting_value FROM (SELECT * FROM users WHERE users.name = 'Charlie') AS u, UNPIVOT u.settings AS v AT k;",
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
            ),*/
            SuccessTestCase(
                name = "UNPIVOT map column pre-filtered to Peter (untyped null wraps as _1)",
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
                name = "UNPIVOT map column pre-filtered to Dana (missing field returns empty)",
                globals = catalogGlobals,
                input = "SELECT u.name, k AS setting_name, v AS setting_value FROM (SELECT * FROM users WHERE users.name = 'Dana') AS u, UNPIVOT u.settings AS v AT k;",
                expected = Datum.bag(emptyList()),
            ),
        )

        @JvmStatic
        fun castTestCases() = listOf(
            SuccessTestCase(
                name = "CAST struct to MAP<STRING, INTEGER>",
                input = "CAST({'a': 1, 'b': 2} AS MAP<STRING, INTEGER>);",
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
                name = "CAST empty struct to MAP",
                input = "CAST({} AS MAP<STRING, INTEGER>);",
                expected = Datum.map(
                    PType.string(),
                    PType.integer(),
                    emptyList()
                ),
            ),
            SuccessTestCase(
                name = "CAST struct with duplicate keys to MAP (last-write-wins)",
                input = "CAST({'a': 1, 'a': 2} AS MAP<STRING, INTEGER>);",
                expected = Datum.map(
                    PType.string(),
                    PType.integer(),
                    listOf(
                        Entry.of(Datum.string("a"), Datum.integer(2)),
                    )
                ),
            ),
            SuccessTestCase(
                name = "CAST MAP<INTEGER, STRING> to MAP<STRING, STRING>",
                input = "CAST(MAP {1: 'one', 2: 'two'} AS MAP<STRING, STRING>);",
                expected = Datum.map(
                    PType.string(),
                    PType.string(),
                    listOf(
                        Entry.of(Datum.string("1"), Datum.string("one")),
                        Entry.of(Datum.string("2"), Datum.string("two")),
                    )
                ),
            ),
            SuccessTestCase(
                name = "CAST MAP<STRING, INTEGER> to MAP<STRING, BIGINT>",
                input = "CAST(MAP {'x': 10, 'y': 20} AS MAP<STRING, BIGINT>);",
                expected = Datum.map(
                    PType.string(),
                    PType.bigint(),
                    listOf(
                        Entry.of(Datum.string("x"), Datum.bigint(10)),
                        Entry.of(Datum.string("y"), Datum.bigint(20)),
                    )
                ),
            ),
            SuccessTestCase(
                name = "CAST struct to MAP with non-string key type fails (permissive), return missing",
                input = "CAST({'a': 1, 'b': 2} AS MAP<INTEGER, INTEGER>);",
                expected = Datum.missing()
            ),
        )

        @JvmStatic
        fun castFailureTestCases() = listOf(
            FailureTestCase(
                name = "CAST struct to MAP with non-string key type fails (strict)",
                input = "CAST({'a': 1, 'b': 2} AS MAP<INTEGER, INTEGER>);",
            ),
        )
    }
}
