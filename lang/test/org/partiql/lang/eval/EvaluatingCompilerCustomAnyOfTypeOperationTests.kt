package org.partiql.lang.eval

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.anyOfType
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.esAnyType
import org.partiql.lang.types.BagType
import org.partiql.lang.types.ListType
import org.partiql.lang.types.NumberConstraint
import org.partiql.lang.types.SexpType
import org.partiql.lang.types.StaticType
import org.partiql.lang.types.StringType
import org.partiql.lang.types.StringType.*
import org.partiql.lang.types.StructType
import org.partiql.lang.types.buildTypeFunction
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.honorTypedOpParameters
import org.partiql.lang.util.legacyTypingMode
import org.partiql.lang.util.permissiveTypingMode

/**
 * This test class covers use of [StaticType] with custom [AnyOfType] types such as `ES_ANY`.
 *
 * Note that in this case class the definition of custom type `ES_ANY` has many different definitions.  See the
 * [ArgumentsProviderBase] implementations for details.
 *
 * This test class generates and tests a couple thousand test cases conforming to the following patterns:
 *
 * - `CAST(<value> AS ES_ANY)`
 * - `CAN_CAST(<value> AS ES_ANY)`
 * - `CAN_LOSSLESS_CAST(<value> AS ES_ANY)`
 * - `<value> IS ES_ANY`
 */
class EvaluatingCompilerCustomAnyOfTypeOperationTests: CastTestBase() {
    companion object {
        private val customTypeFunctions = mapOf("ES_ANY" to esAnyType)

        // Cases that pass the input to the output directly (for IS testing)
        private val esAnyCastIdentityCases = listOf(
            listOf(
                // null/missing
                case("NULL", "null", CastQuality.LOSSLESS) {
                    assertEquals(ExprValueType.NULL, exprValue.type)
                },
                case("MISSING", "null", FixSemantics(CastQuality.LOSSY)) {
                    assertEquals(ExprValueType.MISSING, exprValue.type)
                },
                // bool
                case("TRUE", "true", CastQuality.LOSSLESS),
                case("FALSE", "false", CastQuality.LOSSLESS),
                // int
                case("99", "99", CastQuality.LOSSLESS),
                case("100", "100", CastQuality.LOSSLESS),
                // float
                case("`10e0`", "10e0", CastQuality.LOSSLESS),
                // string
                case("'hey now'", "\"hey now\"", CastQuality.LOSSLESS)
            ).types(listOf("ES_ANY"))
        ).flatten()

        // Cases that convert or fail
        private val esAnyCastConvertOrFailCases = listOf(
            listOf(
                // float -> int (overflow float [100])
                case("`123e0`", "123", CastQuality.LOSSLESS),
                // decimal -> float
                //     This round-trips as 55.1 -> 55.1e0 -> 55.100000000000001421085471520200371742
                case("55.1", "55.1e0", CastQuality.LOSSY),
                // timestamp -> string
                case("`2016-02-27T12:34:56Z`", "\"2016-02-27T12:34:56Z\"", CastQuality.LOSSLESS),
                // symbol -> string
                case("`'moo cow'`", "\"moo cow\"", CastQuality.LOSSLESS),
                // clob
                case("`{{\"moo\"}}`", ErrorCode.EVALUATOR_CAST_FAILED),
                // blob
                case("`{{Ymxhcmc=}}`", ErrorCode.EVALUATOR_CAST_FAILED),
                // list
                case("[1, `1312.1e0`, 9999.0]", "[1, 1312, 9999]", CastQuality.LOSSY),
                case("[1, `{{\"woof\"}}`, 9999.0]", ErrorCode.EVALUATOR_CAST_FAILED),
                // sexp
                //   This round trips from <sexp(<sym>, <sym>, <list(<timestamp>)>)> to <sexp(<str>, <str>, <list(<str>)>)>
                case("`(a b [2099-01-21T12:34:56Z])`",
                    "[\"a\", \"b\", [\"2099-01-21T12:34:56Z\"]]",
                    FixSemantics(CastQuality.LOSSLESS)),
                case("`(a b [2099-01-21T12:34:56Z, {{Ymxhcmc=}}])`", ErrorCode.EVALUATOR_CAST_FAILED),
                // bag
                case("<<99, 20000, MISSING>>", "[99, 20000, null]", FixSemantics(CastQuality.LOSSY)) {
                    assertEquals(ExprValueType.LIST, exprValue.type)
                    assertEquals(ExprValueType.MISSING, exprValue.ordinalBindings[2]?.type)
                },
                case("<<99, 20000, MISSING, `{{}}`>>", ErrorCode.EVALUATOR_CAST_FAILED),
                // struct
                case("`{a: 1000, b: 1312000.1e0, c: 9999000.0, d: null}`",
                    "{a: 1000, b: 1312000, c: 9999000, d: null}",
                    CastQuality.LOSSY),
                case("`{a: 1000, b: 1312000.1e0, c: 9999000.0, d: null, e:[{f:({{}})}]}`",
                    ErrorCode.EVALUATOR_CAST_FAILED)
            ).types(listOf("ES_ANY"))
        ).flatten()

        private val esAnyCastCases = esAnyCastIdentityCases + esAnyCastConvertOrFailCases

        // TODO consider refactoring into CastTestBase (with parameter)
        fun List<CastCase>.toConfiguredCases(): List<ConfiguredCastCase> = (flatMap { case ->
            castBehaviors.map { (castBehaviorName, castBehaviorConfig) ->
                ConfiguredCastCase(case, "$castBehaviorName, LEGACY_TYPING_MODE") {
                    castBehaviorConfig(this)
                    legacyTypingMode()
                }
            }
        } + toPermissive().flatMap { case ->
            castBehaviors.map { (castBehaviorName, castBehaviorConfig) ->
                ConfiguredCastCase(case, "$castBehaviorName, PERMISSIVE_TYPING_MODE") {
                    castBehaviorConfig(this)
                    permissiveTypingMode()
                }
            }
        }).map {
            it.copy(
                configurePipeline = {
                    customTypeFunctions(customTypeFunctions)
                }
            )
        }

        fun List<CastCase>.toConfiguredHonorParamMode(): List<ConfiguredCastCase> = (map { case ->
            ConfiguredCastCase(case, "HONOR_PARAMS, LEGACY_TYPING_MODE") {
                honorTypedOpParameters()
                legacyTypingMode()
            }
        } + toPermissive().map { case ->
            ConfiguredCastCase(case, "HONOR_PARAMS, PERMISSIVE_TYPING_MODE") {
                honorTypedOpParameters()
                permissiveTypingMode()
            }
        }).map {
            it.copy(
                configurePipeline = {
                    customTypeFunctions(customTypeFunctions)
                }
            )
        }

        // TODO: these aren't bad for IS anymore.
        private val badEsAnyTypeDefinitionsForCastAndCanCast: List<StaticType> =
            listOf(
                // collection with constraint -- implicitly self-recursive element type limitation
                anyOfType(
                    ListType(StaticType.INT)
                ),
                anyOfType(
                    SexpType(StaticType.STRING)
                ),
                anyOfType(
                    BagType(StaticType.FLOAT)
                ),
                // struct with constraint -- implicitly self-recursive element type limitation
                anyOfType(
                    StructType(fields = mapOf("hello" to StaticType.INT))
                ),
                anyOfType(
                    StructType(contentClosed = true)
                )
            )
    }

    @ParameterizedTest
    @ArgumentsSource(EsAnyCastConfiguredCases::class)
    fun esAnyCast(configuredCastCase: CastTestBase.ConfiguredCastCase) = configuredCastCase.assertCase()
    class EsAnyCastConfiguredCases : ArgumentsProviderBase() {
        override fun getParameters() = esAnyCastCases.toConfiguredCases()
    }

    @ParameterizedTest
    @ArgumentsSource(EsAnyCanCastConfiguredCases::class)
    fun esAnyCanCast(configuredCastCase: CastTestBase.ConfiguredCastCase) = configuredCastCase.assertCase()
    class EsAnyCanCastConfiguredCases : ArgumentsProviderBase() {
        override fun getParameters() = esAnyCastCases.map { case ->
            case.toCanCast()
        }.toConfiguredCases()
    }

    @ParameterizedTest
    @ArgumentsSource(EsAnyCanLossLessCastConfiguredCases::class)
    fun esAnyCanLosslessCast(configuredCastCase: CastTestBase.ConfiguredCastCase) = configuredCastCase.assertCase()
    class EsAnyCanLossLessCastConfiguredCases : ArgumentsProviderBase() {
        override fun getParameters() = esAnyCastCases.map { case ->
            case.toCanLosslessCast()
        }.toConfiguredCases()
    }

    @ParameterizedTest
    @ArgumentsSource(EsAnyIsConfiguredCastCases::class)
    fun esAnyIs(configuredCastCase: CastTestBase.ConfiguredCastCase) = configuredCastCase.assertCase()
    class EsAnyIsConfiguredCastCases : ArgumentsProviderBase() {
        override fun getParameters(): List<ConfiguredCastCase> {
            val esAnyIsBaseCases = (esAnyCastIdentityCases.map { case ->
                case.copy(
                    funcName = "IS",
                    expected = "true",
                    expectedErrorCode = null,
                    additionalAssertBlock = { }
                )
            } + esAnyCastConvertOrFailCases.map { case ->
                case.copy(
                    funcName = "IS",
                    expected = "false",
                    expectedErrorCode = null,
                    additionalAssertBlock = { }
                )
            })

            return esAnyIsBaseCases.toConfiguredCases() +
                // Take the bad union of type cases and rewrite them for `IS`.
                // Note that for `IS` they are not actually bad.
                esAnyIsBaseCases
                    .toConfiguredHonorParamMode()
                    .flatMap { configuredCase ->
                        badEsAnyTypeDefinitionsForCastAndCanCast.map { badType ->
                            configuredCase.copy(
                                castCase = configuredCase.castCase.copy(
                                    expected = "false",
                                    expectedErrorCode = null,
                                    additionalAssertBlock = { }
                                ),
                                configurePipeline = {
                                    customTypeFunctions(
                                        mapOf("ES_ANY" to buildTypeFunction(badType))
                                    )
                                },
                                description = "${configuredCase.description} $badType"
                            )
                        }
                    }
        }
    }
    @ParameterizedTest
    @ArgumentsSource(EsAnyBadCustomTypeConfiguredCastCases::class)
    fun esAnyBadCustomType(configuredCastCase: CastTestBase.ConfiguredCastCase) = configuredCastCase.assertCase()
    class EsAnyBadCustomTypeConfiguredCastCases : ArgumentsProviderBase() {
        override fun getParameters() =
            esAnyCastCases.toConfiguredCases().flatMap { case ->
                val badEsAnyTypeDefinitionsForCastOnly = listOf(
                    // duplicate types
                    anyOfType(
                        StaticType.STRING,
                        StringType(StringLengthConstraint.Constrained(NumberConstraint.UpTo(500)))
                    ),
                    anyOfType(
                        StaticType.INT,
                        StaticType.INT4,
                        StaticType.INT8
                    ),
                    // nested ANY
                    anyOfType(
                        StaticType.ANY
                    ),
                    // nested AnyOf -- flattening is a pre-condition
                    anyOfType(
                        anyOfType(StaticType.STRING)
                    )
                )

                (badEsAnyTypeDefinitionsForCastAndCanCast + badEsAnyTypeDefinitionsForCastOnly).flatMap { badType ->
                    case.copy(
                        castCase = case.castCase.copy(
                            expected = null,
                            expectedErrorCode = ErrorCode.SEMANTIC_UNION_TYPE_INVALID,
                            additionalAssertBlock = { }
                        ),
                        configurePipeline = {
                            customTypeFunctions(
                                mapOf("ES_ANY" to buildTypeFunction(badType))
                            )
                        },
                        description = "${case.description} $badType"
                    ).let {
                        listOf(
                            it,
                            it.copy(
                                castCase = it.castCase.copy(
                                    funcName = "CAN_CAST"
                                )
                            ),
                            it.copy(
                                castCase = it.castCase.copy(
                                    funcName = "CAN_LOSSLESS_CAST"
                                )
                            )
                        )
                    }
                }
            }
    }
}