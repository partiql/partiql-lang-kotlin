package org.partiql.lang.eval.visitors.inferencer

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.TestCase
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.customTypedOpParameters
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.expectQueryOutputType
import org.partiql.lang.eval.visitors.inferencer.InferencerTestUtil.runTest
import org.partiql.lang.util.compareTo
import org.partiql.types.DecimalType
import org.partiql.types.IntType
import org.partiql.types.NumberConstraint
import org.partiql.types.StaticType
import org.partiql.types.StringType

class InferencerTypedExpressionTests {

    @ParameterizedTest
    @MethodSource("parametersForTypedExpressionTests")
    fun typedExpressionTests(tc: TestCase) = runTest(tc)

    companion object {

        @JvmStatic
        @Suppress("unused")
        fun parametersForTypedExpressionTests() = listOf(
            TestCase(
                name = "CAST to a type that doesn't expect any parameters",
                originalSql = "CAST(an_int AS INT)",
                globals = mapOf("an_int" to IntType(IntType.IntRangeConstraint.LONG)),
                handler = expectQueryOutputType(IntType(IntType.IntRangeConstraint.LONG))
            ),
            TestCase(
                name = "CAST to SMALLINT",
                originalSql = "CAST(an_int AS SMALLINT)",
                globals = mapOf("an_int" to StaticType.INT4),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.MISSING,
                        IntType(IntType.IntRangeConstraint.SHORT)
                    )
                )
            ),
            TestCase(
                name = "CAST to VARCHAR",
                originalSql = "CAST(a_string AS VARCHAR)",
                globals = mapOf("a_string" to StaticType.STRING),
                handler = expectQueryOutputType(StringType(StringType.StringLengthConstraint.Unconstrained))
            ),
            TestCase(
                name = "CAST to VARCHAR(x)",
                originalSql = "CAST(a_string AS VARCHAR(10))",
                globals = mapOf(
                    "a_string" to StringType(
                        StringType.StringLengthConstraint.Constrained(
                            NumberConstraint.UpTo(10)
                        )
                    )
                ),
                handler = expectQueryOutputType(
                    StringType(
                        StringType.StringLengthConstraint.Constrained(
                            NumberConstraint.UpTo(10)
                        )
                    )
                )
            ),
            TestCase(
                name = "CAST to CHAR",
                originalSql = "CAST(a_string AS CHAR)",
                globals = mapOf(
                    "a_string" to StringType(
                        StringType.StringLengthConstraint.Constrained(
                            NumberConstraint.Equals(1)
                        )
                    )
                ),
                handler = expectQueryOutputType(
                    StringType(
                        StringType.StringLengthConstraint.Constrained(
                            NumberConstraint.Equals(1)
                        )
                    )
                )
            ),
            TestCase(
                name = "CAST to CHAR(x)",
                originalSql = "CAST(a_string AS CHAR(10))",
                globals = mapOf(
                    "a_string" to StringType(
                        StringType.StringLengthConstraint.Constrained(
                            NumberConstraint.Equals(10)
                        )
                    )
                ),
                handler = expectQueryOutputType(
                    StringType(
                        StringType.StringLengthConstraint.Constrained(
                            NumberConstraint.Equals(10)
                        )
                    )
                )
            ),
            TestCase(
                name = "CAST to DECIMAL",
                originalSql = "CAST(an_int AS DECIMAL)",
                globals = mapOf("an_int" to StaticType.INT4),
                handler = expectQueryOutputType(StaticType.DECIMAL)
            ),
            TestCase(
                name = "CAST to DECIMAL with precision",
                originalSql = "CAST(a_decimal AS DECIMAL(10))",
                globals = mapOf("a_decimal" to StaticType.DECIMAL),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.MISSING,
                        DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(10))
                    )
                )
            ),
            TestCase(
                name = "CAST to DECIMAL with precision and scale",
                originalSql = "CAST(a_decimal AS DECIMAL(10,2))",
                globals = mapOf("a_decimal" to StaticType.DECIMAL),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.MISSING,
                        DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(10, 2))
                    )
                )
            ),
            TestCase(
                name = "CAST to NUMERIC with precision and scale",
                originalSql = "CAST(a_decimal AS NUMERIC(10,2))",
                globals = mapOf("a_decimal" to StaticType.DECIMAL),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        StaticType.MISSING,
                        DecimalType(DecimalType.PrecisionScaleConstraint.Constrained(10, 2))
                    )
                )
            ),
            TestCase(
                name = "IS operator",
                originalSql = "true IS BOOL",
                handler = expectQueryOutputType(StaticType.BOOL)
            ),
            TestCase(
                name = "CAST with a custom type without validation thunk",
                originalSql = "CAST(an_int AS ES_INTEGER)",
                globals = mapOf("an_int" to IntType(IntType.IntRangeConstraint.LONG)),
                handler = expectQueryOutputType(
                    customTypedOpParameters["es_integer"]!!.staticType
                )
            ),
            TestCase(
                name = "CAST with a custom type with validation thunk",
                originalSql = "CAST(an_int AS ES_FLOAT)",
                globals = mapOf("an_int" to IntType(IntType.IntRangeConstraint.LONG)),
                handler = expectQueryOutputType(
                    StaticType.unionOf(
                        customTypedOpParameters["es_float"]!!.staticType,
                        StaticType.MISSING
                    )
                )
            ),
            TestCase(
                name = "can_lossless_cast int as decimal",
                originalSql = "CAN_LOSSLESS_CAST(an_int AS DECIMAL)",
                globals = mapOf("an_int" to StaticType.INT),
                handler = expectQueryOutputType(StaticType.BOOL)
            ),
            TestCase(
                name = "can_lossless_cast int as decimal with precision",
                originalSql = "CAN_LOSSLESS_CAST(an_int AS DECIMAL(5))",
                globals = mapOf("an_int" to StaticType.INT),
                handler = expectQueryOutputType(StaticType.BOOL)
            ),
            TestCase(
                name = "can_lossless_cast int as decimal with precision and scale",
                originalSql = "CAN_LOSSLESS_CAST(an_int AS DECIMAL(5,2))",
                globals = mapOf("an_int" to StaticType.INT),
                handler = expectQueryOutputType(StaticType.BOOL)
            ),
            TestCase(
                name = "can_lossless_cast decimal as int",
                originalSql = "CAN_LOSSLESS_CAST(a_decimal AS INT)",
                globals = mapOf("a_decimal" to StaticType.DECIMAL),
                handler = expectQueryOutputType(StaticType.BOOL)
            ),
            TestCase(
                name = "can_lossless_cast int as custom type",
                originalSql = "CAN_LOSSLESS_CAST(an_int AS ES_integer)",
                globals = mapOf("an_int" to StaticType.INT),
                handler = expectQueryOutputType(StaticType.BOOL)
            ),
            TestCase(
                name = "can_lossless_cast union of types as non-unknown type",
                originalSql = "CAN_LOSSLESS_CAST(int_or_decimal AS INT)",
                globals = mapOf("int_or_decimal" to StaticType.unionOf(StaticType.INT, StaticType.DECIMAL)),
                handler = expectQueryOutputType(StaticType.BOOL)
            ),
            TestCase(
                name = "can_lossless_cast union of types as non-unknown custom type",
                originalSql = "CAN_LOSSLESS_CAST(int_or_decimal AS ES_integer)",
                globals = mapOf("int_or_decimal" to StaticType.unionOf(StaticType.INT, StaticType.DECIMAL)),
                handler = expectQueryOutputType(StaticType.BOOL)
            )
        )
    }
}
