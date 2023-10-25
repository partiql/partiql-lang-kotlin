package org.partiql.lang.eval.visitors.inferencer

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.TestCase
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectQueryOutputType
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.runTest
import org.partiql.types.StaticType
import org.partiql.types.StructType

class InferencerStructTests {
    @ParameterizedTest
    @MethodSource("parametersForStructTests")
    fun structTests(tc: TestCase) = runTest(tc)

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun parametersForStructTests() = listOf(
            TestCase(
                "struct -- no fields",
                "{ }",
                handler = expectQueryOutputType(
                    StructType(
                        emptyMap(),
                        contentClosed = true
                    )
                )
            ),
            TestCase(
                "struct -- text literal keys",
                "{ 'a': 1, 'b': true, `c`: 'foo' }",
                handler = expectQueryOutputType(
                    StructType(
                        mapOf(
                            "a" to StaticType.INT,
                            "b" to StaticType.BOOL,
                            "c" to StaticType.STRING
                        ),
                        contentClosed = true
                    )
                )
            ),
            // TODO: non-text keys should be an error condition, see: https://github.com/partiql/partiql-lang-kotlin/issues/496
            TestCase(
                "struct -- string and int literal keys",
                "{ 'a': 1, 2: 2 }",
                handler = expectQueryOutputType(
                    StructType(
                        mapOf("a" to StaticType.INT),
                        contentClosed = true
                    )
                )
            ),
            // TODO: non-text keys should be an error condition, see: https://github.com/partiql/partiql-lang-kotlin/issues/496
            TestCase(
                "struct -- symbol and int literal keys",
                "{ `a`: 1, 2: 2 }",
                handler = expectQueryOutputType(
                    StructType(
                        mapOf("a" to StaticType.INT),
                        contentClosed = true
                    )
                )
            ),
            // TODO: non-text keys should be an error condition, see: https://github.com/partiql/partiql-lang-kotlin/issues/496
            TestCase(
                "struct -- null literal key",
                "{ null: 1 }",
                handler = expectQueryOutputType(
                    StructType(
                        emptyMap(),
                        contentClosed = true
                    )
                )
            ),
            // TODO: non-text keys should be an error condition, see: https://github.com/partiql/partiql-lang-kotlin/issues/496
            TestCase(
                "struct -- missing key",
                "{ missing: 1 }",
                handler = expectQueryOutputType(
                    StructType(
                        emptyMap(),
                        contentClosed = true
                    )
                )
            ),
            // TODO: non-text keys should be an error condition, see: https://github.com/partiql/partiql-lang-kotlin/issues/496
            TestCase(
                "struct -- multiple non-text keys",
                "{ 1: 1, null: 2, missing: 3, true: 4, {}: 5 }",
                handler = expectQueryOutputType(
                    StructType(
                        emptyMap(),
                        contentClosed = true
                    )
                )
            ),
            TestCase(
                "struct -- string, non-literal key",
                "{ foo: 1 }",
                mapOf("foo" to StaticType.STRING),
                handler = expectQueryOutputType(
                    StructType(
                        emptyMap(),
                        contentClosed = false
                    )
                )
            ),
            TestCase(
                "struct -- symbol, non-literal key",
                "{ foo: 1 }",
                mapOf("foo" to StaticType.SYMBOL),
                handler = expectQueryOutputType(
                    StructType(
                        emptyMap(),
                        contentClosed = false
                    )
                )
            ),
            TestCase(
                "struct -- symbol, non-literal key and text literal key",
                "{ foo: 1, 'b': 123 }",
                mapOf("foo" to StaticType.SYMBOL),
                handler = expectQueryOutputType(
                    StructType(
                        mapOf("b" to StaticType.INT),
                        contentClosed = false
                    )
                )
            ),
            // TODO: non-text keys should be an error condition, see: https://github.com/partiql/partiql-lang-kotlin/issues/496
            TestCase(
                "struct -- non-text, non-literal key",
                "{ foo: 1 }",
                mapOf("foo" to StaticType.BOOL),
                handler = expectQueryOutputType(
                    StructType(
                        emptyMap(),
                        contentClosed = true
                    )
                )
            ),
            // TODO: non-text keys should be an error condition, see: https://github.com/partiql/partiql-lang-kotlin/issues/496
            TestCase(
                "struct -- non-text, non-literal key and text literal key",
                "{ foo: 1, 'b': 123 }",
                mapOf("foo" to StaticType.BOOL),
                handler = expectQueryOutputType(
                    StructType(
                        mapOf("b" to StaticType.INT),
                        contentClosed = true
                    )
                )
            ),
            TestCase(
                "struct -- nullable string, non-literal key",
                "{ foo: 1 }",
                mapOf("foo" to StaticType.STRING.asNullable()),
                handler = expectQueryOutputType(
                    StructType(
                        emptyMap(),
                        contentClosed = false
                    )
                )
            ),
            // TODO: non-text keys should be an error condition, see: https://github.com/partiql/partiql-lang-kotlin/issues/496
            TestCase(
                "struct -- nullable non-text, non-literal key",
                "{ foo: 1 }",
                mapOf("foo" to StaticType.BOOL.asNullable()),
                handler = expectQueryOutputType(
                    StructType(
                        emptyMap(),
                        contentClosed = true
                    )
                )
            ),
            TestCase(
                "struct -- union of text types non-literal key",
                "{ foo: 1 }",
                mapOf("foo" to StaticType.unionOf(StaticType.STRING, StaticType.SYMBOL)),
                handler = expectQueryOutputType(
                    StructType(
                        emptyMap(),
                        contentClosed = false
                    )
                )
            ),
            // TODO: non-text keys should be an error condition, see: https://github.com/partiql/partiql-lang-kotlin/issues/496
            TestCase(
                "struct -- union of non-text types non-literal key",
                "{ foo: 1 }",
                mapOf("foo" to StaticType.unionOf(StaticType.BOOL, StaticType.INT)),
                handler = expectQueryOutputType(
                    StructType(
                        emptyMap(),
                        contentClosed = true
                    )
                )
            ),
            TestCase(
                "struct -- ANY type non-literal key",
                "{ foo: 1 }",
                mapOf("foo" to StaticType.ANY),
                handler = expectQueryOutputType(
                    StructType(
                        emptyMap(),
                        contentClosed = false
                    )
                )
            ),
            // TODO: non-text keys should be an error condition, see: https://github.com/partiql/partiql-lang-kotlin/issues/496
            TestCase(
                "nested struct -- literal, non-text inner key",
                "{ 'a': 1, 'nestedStruct': { 2: 2, 'validKey': 42 } }",
                handler = expectQueryOutputType(
                    StructType(
                        mapOf(
                            "a" to StaticType.INT,
                            "nestedStruct" to StructType(mapOf("validKey" to StaticType.INT), contentClosed = true)
                        ),
                        contentClosed = true
                    )
                )
            ),
            TestCase(
                "nested struct -- non-literal, text inner key",
                "{ 'a': 1, 'nestedStruct': { nonLiteralTextKey: 2, 'validKey': 42 } }",
                mapOf("nonLiteralTextKey" to StaticType.STRING),
                handler = expectQueryOutputType(
                    StructType(
                        mapOf(
                            "a" to StaticType.INT,
                            "nestedStruct" to StructType(mapOf("validKey" to StaticType.INT), contentClosed = false)
                        ),
                        contentClosed = true
                    )
                )
            ),
            TestCase(
                "nested struct -- non-literal, text outer key",
                "{ 'a': 1, nonLiteralTextKey: { 'b': 2, 'validKey': 42 } }",
                mapOf("nonLiteralTextKey" to StaticType.STRING),
                handler = expectQueryOutputType(
                    StructType(
                        mapOf("a" to StaticType.INT),
                        contentClosed = false
                    )
                )
            ),
            // TODO: non-text keys should be an error condition, see: https://github.com/partiql/partiql-lang-kotlin/issues/496
            TestCase(
                "nested struct -- non-literal, non-text outer key",
                "{ 'a': 1, nonLiteralNonTextKey: { 'b': 2, 'validKey': 42 } }",
                mapOf("nonLiteralNonTextKey" to StaticType.BOOL),
                handler = expectQueryOutputType(
                    StructType(
                        mapOf("a" to StaticType.INT),
                        contentClosed = true
                    )
                )
            )
        )
    }
}
