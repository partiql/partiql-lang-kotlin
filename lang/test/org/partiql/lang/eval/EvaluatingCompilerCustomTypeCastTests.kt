package org.partiql.lang.eval

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.CUSTOM_TEST_TYPES_MAP
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.util.ArgumentsProviderBase
import org.partiql.lang.util.honorTypedOpParameters
import org.partiql.lang.util.legacyTypingMode
import org.partiql.lang.util.permissiveTypingMode


class EvaluatingCompilerCustomTypeCastTests : CastTestBase() {
    companion object {

        // TODO refactor this to leverage more of the CAST base test corpus
        // custom type cases
        private val customTypeCases = listOf(
            listOf(
                case("TRUE AND FALSE", "false", CastQuality.LOSSY),
                case("`true`", "true", CastQuality.LOSSLESS),
                case("5", "true", CastQuality.LOSSY),
                case("`0e0`", "false", CastQuality.LOSSLESS),
                case("1.1", "true", CastQuality.LOSSY),
                case("`2007-10-10T`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("'hello'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("'TrUe'", "true", CastQuality.LOSSY),
                case("""`"FALSE"`""", "false", CastQuality.LOSSY),
                case("""`'true'`""", "true", CastQuality.LOSSY),
                case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                case("""`{{"goodbye"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                case("""`{{"false"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                case("""`{{"true"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`{{Z29vZGJ5ZQ==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // goodbye
                case("`{{ZmFsc2U=}}`", ErrorCode.EVALUATOR_INVALID_CAST), // false
                case("`{{dHJ1ZQ==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // true
                case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`[true]`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`[false]`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`[true, false]`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`(true)`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`(false)`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`{a:true}`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("{'b':true}", ErrorCode.EVALUATOR_INVALID_CAST),
                case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST),
                case("<<true>>", ErrorCode.EVALUATOR_INVALID_CAST),
                case("<<false>>", ErrorCode.EVALUATOR_INVALID_CAST)
            ).types(listOf("ES_boolean", "SPARK_boolean", "RS_boolean")),
            listOf(
                case("'99'", "99e0", CastQuality.LOSSLESS),
                case("99", "99e0", CastQuality.LOSSLESS),
                case("1e-2", "1e-2", CastQuality.LOSSLESS),
                case("`1d1`", "10e0", CastQuality.LOSSLESS),
                case("'0e0'", "0e0", CastQuality.LOSSLESS),
                case("'2e-1'", "2e-1", CastQuality.LOSSLESS),
                case("[1,2,3]", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`(1 2)`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("{ 'a': 1 }", ErrorCode.EVALUATOR_INVALID_CAST),
                case("<<'a'>>", ErrorCode.EVALUATOR_INVALID_CAST),
                case("'[1,2,3]'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("'(1 2)'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("'{a: 2}'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("'<< 1 >>'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("[1,2,3]", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`(1 1)`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("{'a': 2}", ErrorCode.EVALUATOR_INVALID_CAST),
                case("<<'a'>>", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`{{+AB/}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`{{ \"clob\"}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("'100'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("'true'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("true", "1e0", CastQuality.LOSSY),
                case("false", "0e0", CastQuality.LOSSY)
            ).types(listOf("ES_FLOAT")),
            listOf(
                case("'100'", "100", CastQuality.LOSSLESS),
                case("3", "3", CastQuality.LOSSLESS),
                case("3e2", "300", CastQuality.LOSSLESS),
                case("`2d2`", "200", CastQuality.LOSSLESS),
                case("'a'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("[2]", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`(1 1)`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("{ 'a': 1 }", ErrorCode.EVALUATOR_INVALID_CAST),
                case("<<'a'>>", ErrorCode.EVALUATOR_INVALID_CAST),
                case("'[1,2,3]'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("'(1 2)'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("'{a: 2}'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("'<< 1 >>'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("[1,2,3]", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`(1 1)`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("{'a': 2}", ErrorCode.EVALUATOR_INVALID_CAST),
                case("<<'a'>>", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`{{+AB/}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`{{ \"clob\"}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("true", "1", CastQuality.LOSSLESS),
                case("false", "0", CastQuality.LOSSLESS)
            ).types(listOf("ES_INTEGER", "SPARK_SHORT", "SPARK_INTEGER", "SPARK_LONG", "RS_INTEGER", "RS_BIGINT")),
            listOf(
                case("42", "\"42\"", CastQuality.LOSSLESS),
                case("0e0", "\"0\"", CastQuality.LOSSLESS),
                case("2e-1", "\"0.2\"", CastQuality.LOSSLESS),
                case("`2d2`", "\"2E+2\"", CastQuality.LOSSLESS),
                case("'12345678901111'", "\"1234567890\"", CastQuality.LOSSY),
                case("'1234567'", "\"1234567\"", CastQuality.LOSSLESS),
                case("[1, 2, 3]", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`(1 1)`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("{ 'a': 2 }", ErrorCode.EVALUATOR_INVALID_CAST),
                case("<<'a'>>", ErrorCode.EVALUATOR_INVALID_CAST),
                case("[1,2,3]", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`(1 1)`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("{'a': 2}", ErrorCode.EVALUATOR_INVALID_CAST),
                case("<<'a'>>", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`{{+AB/}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`{{ \"clob\"}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("true", "\"true\"", CastQuality.LOSSLESS),
                case("false", "\"false\"", CastQuality.LOSSLESS)
            ).types(listOf("RS_VARCHAR_MAX")),
            // Cast to custom parameterized type RS_VARCHAR parameterized by byte length.
            listOf(
                case("'a💩💩💩💩💩j'", "\"a💩\"", CastQuality.LOSSY),
                case("'話家身圧'", "\"話家\"", CastQuality.LOSSY)
            ).types(listOf("RS_VARCHAR(7)", "RS_VARCHAR(8)")),
            listOf(
                case("'a話家l💩k💩'", "\"a話家l💩k\"", CastQuality.LOSSY),
            ).types(listOf("RS_VARCHAR(16)", "RS_VARCHAR(13)")),
            // Cast to custom parameterized type TOY_VARCHAR parameterized by codepoint length.
            listOf(
                case("'a💩💩💩💩💩j'", "\"a💩💩💩💩\"", CastQuality.LOSSY),
                case("'a話家l💩k💩'", "\"a話家l💩\"", CastQuality.LOSSY),
                case("'話家身圧'", "\"話家身圧\"", CastQuality.LOSSY)
            ).types(listOf("TOY_VARCHAR(5)")),
            listOf(
                case("'a💩💩💩💩💩j'", "\"a💩💩\"", CastQuality.LOSSY),
                case("'a話家l💩k💩'", "\"a話家\"", CastQuality.LOSSY),
                case("'話家身圧'", "\"話家身\"", CastQuality.LOSSY)
            ).types(listOf("TOY_VARCHAR(3)")),
            // Cast to custom parameterized type TOY_DECIMAL
            listOf(
                case("1.623", "1.623", CastQuality.LOSSLESS),
            ).types(listOf("TOY_DECIMAL", "TOY_DECIMAL(4,3)")),
            listOf(
                case("1.623", "2.", CastQuality.LOSSY),
            ).types(listOf("TOY_DECIMAL(1)")),
            listOf(
                case("1.627", "1.63", CastQuality.LOSSY),
            ).types(listOf("TOY_DECIMAL(3,2)")),
            listOf(
                case("0", "0e0", CastQuality.LOSSLESS),
                case("5", "5e0", CastQuality.LOSSLESS),
                case("0e0", "0e0", CastQuality.LOSSLESS),
                case("1e-1", "1e-1", CastQuality.LOSSLESS),
                case("'1e-1'", "1e-1", CastQuality.LOSSLESS),
                case("`1d1`", "10e0", CastQuality.LOSSLESS),
                case("'9'", "9e0", CastQuality.LOSSLESS),
                case("'100'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("'a'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("[1,2]", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`(1 1)`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("{ 'a': 2 } ", ErrorCode.EVALUATOR_INVALID_CAST),
                case("<<'a'>>", ErrorCode.EVALUATOR_INVALID_CAST),
                case("'[1,2,3]'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("'(1 2)'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("'{a: 2}'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("'<< 1 >>'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("[1,2,3]", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`(1 1)`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("{'a': 2}", ErrorCode.EVALUATOR_INVALID_CAST),
                case("<<'a'>>", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`{{+AB/}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`{{ \"clob\"}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("true", "1e0", CastQuality.LOSSLESS),
                case("false", "0e0", CastQuality.LOSSLESS)
            ).types(listOf("RS_REAL", "RS_FLOAT4", "SPARK_FLOAT")),
            listOf(
                case("0", "0e0", CastQuality.LOSSLESS),
                case("3", "3e0", CastQuality.LOSSLESS),
                case("'3'", "3e0", CastQuality.LOSSLESS),
                case("3e-1", "3e-1", CastQuality.LOSSLESS),
                case("'3e-1'", "3e-1", CastQuality.LOSSLESS),
                case("`3d-1`", "3e-1", CastQuality.LOSSLESS),
                case("'100'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("'a'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("'[1,2,3]'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("'(1 2)'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("'{a: 2}'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("'<< 1 >>'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("[1,2,3]", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`(1 1)`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("{'a': 2}", ErrorCode.EVALUATOR_INVALID_CAST),
                case("<<'a'>>", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`{{+AB/}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`{{ \"clob\"}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("true", "1e0", CastQuality.LOSSLESS),
                case("false", "0e0", CastQuality.LOSSLESS)
            ).types(listOf("RS_DOUBLE_PRECISION", "RS_FLOAT", "RS_FLOAT8", "SPARK_DOUBLE"))
        ).flatten()

        private val customTypeCastConfiguredTestCases = (customTypeCases.map { case ->
            ConfiguredCastCase(case, "HONOR_PARAM_CAST, LEGACY_TYPING_MODE") {
                honorTypedOpParameters()
                legacyTypingMode()
            }
        } + customTypeCases.toPermissive().map { case ->
            ConfiguredCastCase(case, "HONOR_PARAM_CAST, PERMISSIVE_TYPING_MODE") {
                honorTypedOpParameters()
                permissiveTypingMode()
            }
        }).map {
            it.copy(
                configurePipeline = {
                    customTypeFunctions(CUSTOM_TEST_TYPES_MAP)
                }
            )
        }
    }

    class ConfiguredCastArguments : ArgumentsProviderBase() {
        override fun getParameters() = customTypeCastConfiguredTestCases
    }


    @ParameterizedTest
    @ArgumentsSource(ConfiguredCastArguments::class)
    fun configuredCast(configuredCastCase: CastTestBase.ConfiguredCastCase) = configuredCastCase.assertCase()
}