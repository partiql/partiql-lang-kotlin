package org.partiql.lang.eval.visitors.inferencer

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.TestCase
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectQueryOutputType
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.runTest
import org.partiql.types.ListType
import org.partiql.types.StaticType
import org.partiql.types.StructType

class InferencerPathIntoUnionTests {

    @ParameterizedTest
    @MethodSource("parametersForPathingIntoUnion")
    fun pathingIntoUnion(tc: TestCase) = runTest(tc)

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun parametersForPathingIntoUnion() = listOf(
            TestCase(
                name = "path on list",
                originalSql = "a[1]",
                globals = mapOf("a" to ListType(elementType = StaticType.INT)),
                handler = expectQueryOutputType(StaticType.INT)
            ),
            TestCase(
                name = "path on nullable list",
                originalSql = "a[1]",
                globals = mapOf("a" to ListType(elementType = StaticType.INT).asNullable()),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.MISSING
                    )
                )
            ),
            TestCase(
                name = "path on optional list",
                originalSql = "a[1]",
                globals = mapOf("a" to ListType(elementType = StaticType.INT).asOptional()),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.MISSING
                    )
                )
            ),
            TestCase(
                name = "path on nullable, optional list",
                originalSql = "a[1]",
                globals = mapOf("a" to ListType(elementType = StaticType.INT).asNullable().asOptional()),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.MISSING
                    )
                )
            ),
            TestCase(
                name = "path on union of different list types",
                originalSql = "a[1]",
                globals = mapOf(
                    "a" to StaticType.unionOf(
                        ListType(elementType = StaticType.INT),
                        ListType(elementType = StaticType.BOOL),
                        ListType(elementType = StaticType.STRING)
                    )
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.BOOL,
                        StaticType.STRING
                    )
                )
            ),
            TestCase(
                name = "path on list of union type",
                originalSql = "a[1]",
                globals = mapOf(
                    "a" to ListType(
                        elementType = StaticType.unionOf(
                            StaticType.INT,
                            StaticType.BOOL,
                            StaticType.STRING
                        )
                    )
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.BOOL,
                        StaticType.STRING
                    )
                )
            ),
            TestCase(
                name = "path on union of list and ANY",
                originalSql = "a[1]",
                globals = mapOf(
                    "a" to StaticType.unionOf(
                        ListType(elementType = StaticType.INT),
                        StaticType.ANY
                    )
                ),
                handler = expectQueryOutputType(StaticType.ANY)
            ),
            TestCase(
                name = "path on single element union",
                originalSql = "a.id",
                globals = mapOf(
                    "a" to StaticType.unionOf(
                        StructType(mapOf("id" to StaticType.INT))
                    )
                ),
                handler = expectQueryOutputType(StaticType.INT)
            ),
            TestCase(
                name = "path on union of struct and ANY",
                originalSql = "a.id",
                globals = mapOf(
                    "a" to StaticType.unionOf(
                        StaticType.ANY,
                        StructType(mapOf("id" to StaticType.INT))
                    )
                ),
                handler = expectQueryOutputType(StaticType.ANY)
            ),
            TestCase(
                name = "path on nullable struct",
                originalSql = "a.id",
                globals = mapOf(
                    "a" to StructType(
                        mapOf("id" to StaticType.INT)
                    ).asNullable()
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.MISSING
                    )
                )
            ),
            TestCase(
                name = "path on union of structs with same field name",
                originalSql = "a.id",
                globals = mapOf(
                    "a" to StaticType.unionOf(
                        StructType(mapOf("id" to StaticType.INT)),
                        StructType(mapOf("id" to StaticType.STRING))
                    )
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.STRING
                    )
                )
            ),
            TestCase(
                name = "path on union of struct and int",
                originalSql = "a.id",
                globals = mapOf(
                    "a" to StaticType.unionOf(
                        StructType(mapOf("id" to StaticType.INT)),
                        StaticType.INT
                    )
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.MISSING
                    )
                )
            ),
            TestCase(
                name = "path on nullable union of struct and int",
                originalSql = "a.id",
                globals = mapOf(
                    "a" to StaticType.unionOf(
                        StructType(mapOf("id" to StaticType.INT)),
                        StaticType.INT
                    ).asNullable()
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.MISSING
                    )
                )
            ),
            TestCase(
                name = "path on union of struct, int, timestamp",
                originalSql = "a.id",
                globals = mapOf(
                    "a" to StaticType.unionOf(
                        StructType(mapOf("id" to StaticType.INT)),
                        StaticType.INT,
                        StaticType.TIMESTAMP
                    )
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.MISSING
                    )
                )
            ),
            TestCase(
                name = "path on struct with union",
                originalSql = "a.id",
                globals = mapOf(
                    "a" to StructType(
                        mapOf("id" to StaticType.unionOf(StaticType.INT, StaticType.STRING))
                    )
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.STRING
                    )
                )
            ),
            TestCase(
                name = "path on nullable field",
                originalSql = "a.b.id",
                globals = mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf("id" to StaticType.INT)
                            ).asNullable()
                        )
                    )
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.MISSING
                    )
                )
            ),
            TestCase(
                name = "path on optional struct",
                originalSql = "a.id",
                globals = mapOf(
                    "a" to StructType(
                        mapOf("id" to StaticType.INT)
                    ).asOptional()
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.MISSING
                    )
                )
            ),
            TestCase(
                name = "path on nullable, optional struct",
                originalSql = "a.id",
                globals = mapOf(
                    "a" to StructType(
                        mapOf("id" to StaticType.INT)
                    ).asNullable().asOptional()
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.MISSING
                    )
                )
            ),
            TestCase(
                name = "path on nullable struct with multiple path steps",
                originalSql = "a.b.c.id",
                globals = mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf(
                                    "c" to StructType(
                                        mapOf("id" to StaticType.INT)
                                    )
                                )
                            )
                        )
                    ).asNullable()
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.MISSING
                    )
                )
            ),
            TestCase(
                name = "path on nullable struct within path steps",
                originalSql = "a.b.c.id",
                globals = mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf(
                                    "c" to StructType(
                                        mapOf("id" to StaticType.INT)
                                    ).asNullable()
                                )
                            )
                        )
                    )
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.MISSING
                    )
                )
            ),
            TestCase(
                name = "path on union type for every path step",
                originalSql = "a.b.c.id",
                globals = mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf(
                                    "c" to StructType(
                                        mapOf("id" to StaticType.INT)
                                    ).asNullable()
                                )
                            ).asNullable()
                        )
                    ).asNullable()
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.MISSING
                    )
                )
            ),
            TestCase(
                name = "path on struct, terminal is ANY",
                originalSql = "a.b.c.id",
                globals = mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf(
                                    "c" to StructType(
                                        mapOf("id" to StaticType.ANY)
                                    )
                                )
                            )
                        )
                    )
                ),
                handler = expectQueryOutputType(StaticType.ANY)
            ),
            TestCase(
                name = "path on struct, terminal is nullable",
                originalSql = "a.b.c.id",
                globals = mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf(
                                    "c" to StructType(
                                        mapOf("id" to StaticType.INT.asNullable())
                                    )
                                )
                            )
                        )
                    )
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.NULL
                    )
                )
            ),
            TestCase(
                name = "path on struct, terminal is empty struct",
                originalSql = "a.b.c.id",
                globals = mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf(
                                    "c" to StructType(
                                        mapOf("id" to StructType(emptyMap()))
                                    )
                                )
                            )
                        )
                    )
                ),
                handler = expectQueryOutputType(StructType(emptyMap()))
            ),
            TestCase(
                name = "path on struct, terminal is a struct",
                originalSql = "a.b.c",
                globals = mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf(
                                    "c" to StructType(
                                        mapOf("id" to StaticType.INT)
                                    )
                                )
                            )
                        )
                    )
                ),
                handler = expectQueryOutputType(StructType(mapOf("id" to StaticType.INT)))
            ),
            TestCase(
                name = "path on struct, terminal is a nullable struct",
                originalSql = "a.b.c",
                globals = mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf(
                                    "c" to StructType(
                                        mapOf("id" to StaticType.INT)
                                    ).asNullable()
                                )
                            )
                        )
                    )
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StructType(
                            mapOf(
                                "id" to StaticType.INT
                            )
                        ),
                        StaticType.NULL
                    )
                )
            ),
            TestCase(
                name = "path on struct, terminal is an optional struct",
                originalSql = "a.b.c",
                globals = mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf(
                                    "c" to StructType(
                                        mapOf("id" to StaticType.INT)
                                    ).asOptional()
                                )
                            )
                        )
                    )
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StructType(
                            mapOf(
                                "id" to StaticType.INT
                            )
                        ),
                        StaticType.MISSING
                    )
                )
            ),
            TestCase(
                name = "path on struct, terminal is a nullable, optional struct",
                originalSql = "a.b.c",
                globals = mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf(
                                    "c" to StructType(
                                        mapOf("id" to StaticType.INT)
                                    ).asNullable().asOptional()
                                )
                            )
                        )
                    )
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StructType(
                            mapOf(
                                "id" to StaticType.INT
                            )
                        ),
                        StaticType.NULL, StaticType.MISSING
                    )
                )
            ),
            TestCase(
                name = "path on nullable struct, terminal is a struct",
                originalSql = "a.b.c",
                globals = mapOf(
                    "a" to StructType(
                        mapOf(
                            "b" to StructType(
                                mapOf(
                                    "c" to StructType(
                                        mapOf("id" to StaticType.INT)
                                    )
                                )
                            )
                        )
                    ).asNullable()
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StructType(
                            mapOf(
                                "id" to StaticType.INT
                            )
                        ),
                        StaticType.MISSING
                    )
                )
            ),
            TestCase(
                name = "path on nullable struct and list",
                originalSql = "a.b[1]",
                globals = mapOf(
                    "a" to StructType(
                        mapOf("b" to ListType(elementType = StaticType.INT))
                    ).asNullable()
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.MISSING
                    )
                )
            ),
            TestCase(
                name = "path on nullable struct and nullable list",
                originalSql = "a.b[1]",
                globals = mapOf(
                    "a" to StructType(
                        mapOf("b" to ListType(elementType = StaticType.INT).asNullable())
                    ).asNullable()
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.MISSING
                    )
                )
            ),
            TestCase(
                name = "path on nullable list and struct",
                originalSql = "a[1].id",
                globals = mapOf(
                    "a" to ListType(
                        elementType = StructType(mapOf("id" to StaticType.INT))
                    ).asNullable()
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.MISSING
                    )
                )
            ),
            TestCase(
                name = "path on nullable list and nullable struct",
                originalSql = "a[1].id",
                globals = mapOf(
                    "a" to ListType(
                        elementType = StructType(
                            mapOf("id" to StaticType.INT)
                        ).asNullable()
                    ).asNullable()
                ),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.INT,
                        StaticType.MISSING
                    )
                )
            )
        )
    }
}
