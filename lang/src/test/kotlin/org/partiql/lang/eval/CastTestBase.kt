package org.partiql.lang.eval

import org.junit.Ignore
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.errors.ErrorBehaviorInPermissiveMode
import org.partiql.lang.errors.ErrorCategory
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.evaluatortestframework.ExpectedResultFormat
import org.partiql.lang.util.getOffsetHHmm
import org.partiql.lang.util.honorTypedOpParameters
import org.partiql.lang.util.legacyCastBehavior
import org.partiql.lang.util.legacyTypingMode
import org.partiql.lang.util.permissiveTypingMode
import java.time.ZoneOffset

/**
 * The 'quality' of a successful cast.
 * Used for testing [CAN_LOSSLESS_CAST]
 */
enum class CastQuality {
    /** The `CAST` successfully converts, but loses data */
    LOSSY,
    /** The `CAST` successfully converts, without losing data */
    LOSSLESS,
}

/**
 * The status of casting w.r.t. the 'quality' of a successful cast.
 * Used for testing [CAN_LOSSLESS_CAST]
 */
sealed class CastQualityStatus

/**
 * The necessary expected cast transformations are implemented and working as expected
 */
data class Implemented(val expectedQuality: CastQuality) : CastQualityStatus()
/**
 * The necessary expected cast transformations are not yet implemented
 */
data class NotImplemented(val expectedQuality: CastQuality) : CastQualityStatus()
/**
 * The necessary expected cast transformations are implemented, but currently do not conform to expected semantics
 */
data class FixSemantics(val expectedQuality: CastQuality) : CastQualityStatus()

private fun ExprValueType.typeAliases(): List<String> = when (this) {
    ExprValueType.MISSING -> listOf("missing")
    ExprValueType.NULL -> listOf("null")
    ExprValueType.BOOL -> listOf("bool", "boolean")
    ExprValueType.INT -> listOf("int", "smallint", "integer2", "int2", "integer", "integer4", "int4", "integer8", "int8", "bigint")
    ExprValueType.FLOAT -> listOf("float", "real", "double precision")
    ExprValueType.DECIMAL -> listOf("dec", "decimal", "numeric")
    ExprValueType.DATE -> listOf("date")
    ExprValueType.TIMESTAMP -> listOf("timestamp")
    ExprValueType.TIME -> listOf("time")
    ExprValueType.SYMBOL -> listOf("symbol")
    ExprValueType.STRING -> listOf("string", "char", "varchar", "character", "character varying")
    ExprValueType.CLOB -> listOf("clob")
    ExprValueType.BLOB -> listOf("blob")
    ExprValueType.LIST -> listOf("list")
    ExprValueType.SEXP -> listOf("sexp")
    ExprValueType.STRUCT -> listOf("struct", "tuple")
    ExprValueType.BAG -> listOf("bag")
}

abstract class CastTestBase : EvaluatorTestBase() {

    fun ConfiguredCastCase.assertCase(
        expectedResultFormat: ExpectedResultFormat = ExpectedResultFormat.ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS
    ) {
        when (castCase.expected) {
            null -> {
                val expectedErrorCode: ErrorCode? = castCase.expectedErrorCode
                if (expectedErrorCode == null) {
                    fail("CastCase $castCase did not have an expected value or expected error code.")
                } else {
                    runEvaluatorErrorTestCase(
                        query = castCase.expression,
                        expectedErrorCode = expectedErrorCode,
                        expectedPermissiveModeResult = when (expectedErrorCode.errorBehaviorInPermissiveMode) {
                            ErrorBehaviorInPermissiveMode.THROW_EXCEPTION -> null
                            ErrorBehaviorInPermissiveMode.RETURN_MISSING -> "MISSING"
                        },
                        expectedInternalFlag = null, // <-- disables internal flag assertion
                        compilerPipelineBuilderBlock = compilerPipelineBuilderBlock,
                        compileOptionsBuilderBlock = compileOptionBlock,
                        implicitPermissiveModeTest = false,
                        addtionalExceptionAssertBlock = { it ->
                            assertEquals(expectedErrorCode, it.errorCode)
                        }
                    )
                }
            }
            else -> runEvaluatorTestCase(
                castCase.expression,
                expectedResult = castCase.expected,
                expectedResultFormat = expectedResultFormat,
                includePermissiveModeTest = false,
                compileOptionsBuilderBlock = compileOptionBlock,
                compilerPipelineBuilderBlock = compilerPipelineBuilderBlock,
                extraResultAssertions = castCase.additionalAssertBlock
            )
        }
    }

    /**
     * Test case for general casting tests.
     *
     * @param funcName The name of the type function (e.g. `CAST`, `CAN_CAST`).
     * @param source The PartiQL expression to cast.
     * @param type The PartiQL type name to cast to.
     * @param expected The expected Ion value of the result, or `null` for [EvaluationException]
     * @param expectedErrorCode The expected error code of any [EvaluationException] or `null` when no exception
     * is to be expected.
     * @param quality The expected [CastQualityStatus] of the cast, or `null` for [EvaluationException]
     * @param additionalAssertBlock The additional block of assertions on the resulting value.  Only valid for
     * non-error cases and defaults to no-op.
     */
    data class CastCase(
        val funcName: String,
        val source: String,
        val type: String,
        val expected: String?,
        val expectedErrorCode: ErrorCode?,
        val quality: CastQualityStatus?,
        val additionalAssertBlock: (ExprValue) -> Unit = { }
    ) {
        val expression = when (funcName.toUpperCase()) {
            "IS" -> "($source) IS $type"
            else -> "$funcName($source AS $type)"
        }
        override fun toString(): String = "$expression -> ${expected ?: expectedErrorCode}"

        fun toCanCast(): CastCase {
            return when (expectedErrorCode?.category) {
                // semantic and parser errors are compilation failures - so they should behave the same
                ErrorCategory.SEMANTIC, ErrorCategory.PARSER -> copy(funcName = "CAN_CAST")
                else -> {
                    val newExpected = when (expected) {
                        null -> "false"
                        else -> "true"
                    }
                    copy(
                        funcName = "CAN_CAST",
                        expected = newExpected,
                        expectedErrorCode = null,
                        additionalAssertBlock = { }
                    )
                }
            }
        }

        fun toCanLosslessCast(): CastCase {
            return when (expectedErrorCode?.category) {
                // semantic errors are compilation failures - so they should behave the same
                ErrorCategory.SEMANTIC, ErrorCategory.PARSER -> copy(funcName = "CAN_CAST")
                else -> {
                    val newExpected = when (quality) {
                        null -> "false"
                        is Implemented -> {
                            when (quality.expectedQuality) {
                                CastQuality.LOSSY -> "false"
                                CastQuality.LOSSLESS -> when (expected) {
                                    null -> "false"
                                    else -> "true"
                                }
                            }
                        }
                        is NotImplemented -> {
                            when (quality.expectedQuality) {
                                CastQuality.LOSSY -> "true"
                                CastQuality.LOSSLESS -> "false"
                            }
                        }
                        is FixSemantics -> {
                            when (quality.expectedQuality) {
                                CastQuality.LOSSLESS -> "false"
                                CastQuality.LOSSY -> when (expected) {
                                    null -> "false"
                                    else -> "true"
                                }
                            }
                        }
                    }

                    copy(
                        funcName = "CAN_LOSSLESS_CAST",
                        expected = newExpected,
                        expectedErrorCode = null,
                        additionalAssertBlock = { }
                    )
                }
            }
        }
    }

    /**
     * A [CastCase] bound to a configuration of compiler options.
     *
     * @param description Additional description for the test beyond the cast expression.
     * @param compilerPipelineBuilderBlock Additional configuration for the compiler pipeline.
     * @param compileOptionBlock The optional lambda with a receiver to a [CompileOptions.Builder] to
     *  configure it.
     */
    data class ConfiguredCastCase(
        val castCase: CastCase,
        val description: String = "",
        val compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = {},
        val compileOptionBlock: CompileOptions.Builder.() -> Unit = {}
    ) {
        private val additionalDescription = when (description) {
            "" -> ""
            else -> " - $description"
        }
        override fun toString() = "$castCase$additionalDescription"
    }

    /**
     * TODO fix tests — this is static final wasteland of util methods; not a test class.
     *
     * This is considered a test class by junit because TestBase uses the junit4 RunWith annotation.
     *
     * Consider replacing junit4 with junit5 because this is out of hand
     * https://resources.jetbrains.com/storage/products/kotlinconf2018/slides/4_Best%20Practices%20for%20Unit%20Testing%20in%20Kotlin.pdf
     */
    @Ignore
    companion object : EvaluatorTestBase() {

        /** Partial application of the source expression and the expected Ion value without type. Assumes [Implemented] logic*/
        fun case(
            source: String,
            expected: String?,
            quality: CastQuality,
            additionalAssertBlock: (ExprValue) -> Unit = { }
        ): (String) -> CastCase = {
            CastCase("CAST", source, it, expected, null, Implemented(quality), additionalAssertBlock)
        }

        /** Partial application of the source expression and the expected Ion value without type. */
        fun case(
            source: String,
            expected: String?,
            qualityStatus: CastQualityStatus,
            additionalAssertBlock: (ExprValue) -> Unit = { }
        ): (String) -> CastCase = {
            CastCase("CAST", source, it, expected, null, qualityStatus, additionalAssertBlock)
        }

        /**
         * Function to create explicit CAST(<source> to <type>) CastCase.
         */
        fun case(source: String, type: String, expected: String, quality: CastQuality) =
            CastCase("CAST", source, type, expected, null, Implemented(quality))

        /**
         * Function to create explicit CAST(<source> to <type>) CastCase throwing error.
         */
        fun case(source: String, type: String, expectedErrorCode: ErrorCode) =
            CastCase("CAST", source, type, null, expectedErrorCode, null)

        /** Partial application of the source expression and the expected error code without type. */
        fun case(source: String, expectedErrorCode: ErrorCode): (String) -> CastCase = {
            CastCase("CAST", source, it, null, expectedErrorCode, null)
        }

        /** For each partial case, apply each of the given types to generate a concrete cast case. */
        fun List<(String) -> CastCase>.types(types: List<String>): List<CastCase> =
            this.flatMap { partial -> types.map { type -> partial(type) } }

        val allTypeNames = ExprValueType.values().flatMap { it.typeAliases() }

        val commonTestCases =
            listOf(
                listOf(
                    case("NULL", "null", CastQuality.LOSSLESS) {
                        assertEquals(ExprValueType.NULL, it.type)
                    }
                ).types(allTypeNames - ExprValueType.MISSING.typeAliases()),
                listOf(
                    case("NULL", "null", CastQuality.LOSSLESS) {
                        assertEquals(ExprValueType.MISSING, it.type)
                    }
                ).types(listOf("MISSING")),
                listOf(
                    case("MISSING", "null", CastQuality.LOSSLESS) {
                        assertEquals(ExprValueType.MISSING, it.type)
                    }
                ).types(allTypeNames - ExprValueType.NULL.typeAliases()),
                listOf(
                    case("MISSING", "null", CastQuality.LOSSLESS) {
                        assertEquals(ExprValueType.NULL, it.type)
                    }
                ).types(listOf("NULL")),
                listOf(
                    // booleans
                    case("TRUE AND FALSE", "false", CastQuality.LOSSLESS),
                    case("`true`", "true", CastQuality.LOSSLESS),
                    // numbers
                    case("5", "true", CastQuality.LOSSY),
                    case("`0e0`", "false", CastQuality.LOSSLESS),
                    case("1.1", "true", CastQuality.LOSSY),
                    // timestamp
                    case("`2007-10-10T`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // text
                    case("'hello'", ErrorCode.EVALUATOR_CAST_FAILED),
                    case("'TrUe'", "true", CastQuality.LOSSY),
                    case("""`"FALSE"`""", "false", CastQuality.LOSSY),
                    case("""`'true'`""", "true", CastQuality.LOSSLESS),
                    // lob
                    case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"goodbye"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"false"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"true"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{{Z29vZGJ5ZQ==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // goodbye
                    case("`{{ZmFsc2U=}}`", ErrorCode.EVALUATOR_INVALID_CAST), // false
                    case("`{{dHJ1ZQ==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // true
                    // list
                    case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`[true]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`[false]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`[true, false]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // sexp
                    case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`(true)`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`(false)`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // struct
                    case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{a:true}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("{'b':true}", ErrorCode.EVALUATOR_INVALID_CAST),
                    // bag
                    case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("<<true>>", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("<<false>>", ErrorCode.EVALUATOR_INVALID_CAST)
                ).types(listOf("bool", "boolean")),
                listOf(
                    // booleans
                    case("TRUE AND FALSE", "0", CastQuality.LOSSLESS),
                    case("`true`", "1", CastQuality.LOSSLESS),
                    // numbers
                    case("5", "5", CastQuality.LOSSLESS),
                    case(" 5 ", "5", CastQuality.LOSSLESS),
                    case("`0e0`", "0", CastQuality.LOSSLESS),
                    case("1.1", "1", CastQuality.LOSSY),
                    case("-20.1", "-20", CastQuality.LOSSY),
                    // timestamp
                    case("`2007-10-10T`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // text
                    case("'hello'", ErrorCode.EVALUATOR_CAST_FAILED),
                    case("'1234A'", ErrorCode.EVALUATOR_CAST_FAILED), // Invalid ION value
                    case("'20'", "20", CastQuality.LOSSLESS),
                    case("'020'", "20", CastQuality.LOSSY),
                    case("'+20'", "20", CastQuality.LOSSY),
                    case("'+020'", "20", CastQuality.LOSSY),
                    case("'-20'", "-20", CastQuality.LOSSLESS),
                    case("'-020'", "-20", CastQuality.LOSSY),
                    case("'0'", "0", CastQuality.LOSSLESS),
                    case("'00'", "0", CastQuality.LOSSY),
                    case("'+0'", "0", CastQuality.LOSSY),
                    case("'+00'", "0", CastQuality.LOSSY),
                    case("'-0'", "0", CastQuality.LOSSY),
                    case("'-00'", "0", CastQuality.LOSSY),
                    case("'0xA'", "10", CastQuality.LOSSY),
                    case("'0XA'", "10", CastQuality.LOSSY),
                    case("'0x0A'", "10", CastQuality.LOSSY),
                    case("'+0xA'", "10", CastQuality.LOSSY),
                    case("'+0x0A'", "10", CastQuality.LOSSY),
                    case("'-0xA'", "-10", CastQuality.LOSSY),
                    case("'-0x0A'", "-10", CastQuality.LOSSY),
                    case("'0b10'", "2", CastQuality.LOSSY),
                    case("'0B10'", "2", CastQuality.LOSSY),
                    case("'0b010'", "2", CastQuality.LOSSY),
                    case("'+0b10'", "2", CastQuality.LOSSY),
                    case("'+0b010'", "2", CastQuality.LOSSY),
                    case("'-0b10'", "-2", CastQuality.LOSSY),
                    case("'-0b010'", "-2", CastQuality.LOSSY),
                    case("""`"1000"`""", "1000", CastQuality.LOSSLESS),
                    case("""`'2e100'`""", ErrorCode.EVALUATOR_CAST_FAILED),
                    case("""`'2d100'`""", ErrorCode.EVALUATOR_CAST_FAILED),
                    case("'00xA'", ErrorCode.EVALUATOR_CAST_FAILED),
                    case("'00b10'", ErrorCode.EVALUATOR_CAST_FAILED),
                    // lob
                    case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"1.0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"2e10"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{{MA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 0
                    case("`{{MS4w}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 1.0
                    case("`{{MmUxMA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 2e10
                    // list
                    case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`[1]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`[-2, 0]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // sexp
                    case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`(1)`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`(0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // struct
                    case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{a:12}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("{'b':-4}", ErrorCode.EVALUATOR_INVALID_CAST),
                    // bag
                    case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("<<14>>", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("<<20>>", ErrorCode.EVALUATOR_INVALID_CAST)
                ).types(ExprValueType.INT.typeAliases()),
                listOf(
                    // booleans
                    case("TRUE AND FALSE", "0e0", CastQuality.LOSSLESS),
                    case("`true`", "1e0", CastQuality.LOSSLESS),
                    // numbers
                    case("5", "5e0", CastQuality.LOSSLESS),
                    case(" 5 ", "5e0", CastQuality.LOSSLESS),
                    case("`0e0`", "0e0", CastQuality.LOSSLESS),
                    case("1.1", "1.1e0", CastQuality.LOSSY),
                    case("-20.1", "-20.1e0", CastQuality.LOSSY),
                    // timestamp
                    case("`2007-10-10T`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // text
                    case("'hello'", ErrorCode.EVALUATOR_CAST_FAILED),
                    case("'-20'", "-20e0", CastQuality.LOSSY),
                    case("""`"1000"`""", "1000e0", CastQuality.LOSSY),
                    case("""`'2e100'`""", "2e100", CastQuality.LOSSY),
                    case("""`'2d100'`""", ErrorCode.EVALUATOR_CAST_FAILED),
                    // lob
                    case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"1.0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"2e10"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{{MA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 0
                    case("`{{MS4w}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 1.0
                    case("`{{MmUxMA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 2e10
                    // list
                    case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`[1e0]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`[-2e0, 0e0]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // sexp
                    case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`(1e0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`(0e0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // struct
                    case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{a:12e0}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("{'b':`-4e0`}", ErrorCode.EVALUATOR_INVALID_CAST),
                    // bag
                    case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("<<`14e0`>>", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("<<`20e0`>>", ErrorCode.EVALUATOR_INVALID_CAST)
                ).types(ExprValueType.FLOAT.typeAliases()),
                listOf(
                    // booleans
                    case("TRUE AND FALSE", "0d0", CastQuality.LOSSLESS),
                    case("`true`", "1d0", CastQuality.LOSSLESS),
                    // numbers
                    case("5", "5d0", CastQuality.LOSSLESS),
                    case("5 ", "5d0", CastQuality.LOSSLESS),
                    case("`0e0`", "0.", CastQuality.LOSSLESS), // TODO formalize this behavior
                    case("`1e0`", "1.", CastQuality.LOSSLESS), // TODO formalize this behavior
                    case("1.1", "1.1d0", CastQuality.LOSSLESS),
                    case("-20.1", "-20.1d0", CastQuality.LOSSLESS),
                    // timestamp
                    case("`2007-10-10T`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // text
                    case("'hello'", ErrorCode.EVALUATOR_CAST_FAILED),
                    case("'-20'", "-20d0", CastQuality.LOSSLESS),
                    case("""`"1000"`""", "1000d0", CastQuality.LOSSLESS),
                    case("""`'2e100'`""", "2d100", CastQuality.LOSSY),
                    case("""`'2d100'`""", ErrorCode.EVALUATOR_CAST_FAILED),
                    // lob
                    case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"1.0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"2e10"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{{MA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 0
                    case("`{{MS4w}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 1.0
                    case("`{{MmUxMA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 2e10
                    // list
                    case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`[1d0]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`[-2d0, 0d0]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // sexp
                    case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`(1d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`(0d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // struct
                    case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{a:12d0}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("{'b':`-4d0`}", ErrorCode.EVALUATOR_INVALID_CAST),
                    // bag
                    case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("<<`14d0`>>", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("<<`20d0`>>", ErrorCode.EVALUATOR_INVALID_CAST)
                ).types(ExprValueType.DECIMAL.typeAliases()),
                listOf(
                    // booleans
                    case("TRUE AND FALSE", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`true`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // numbers
                    case("5", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`0e0`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("1.1", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("-20.1", ErrorCode.EVALUATOR_INVALID_CAST),
                    // timestamp
                    case("`2007-10-10T`", "$DATE_ANNOTATION::2007-10-10", NotImplemented(CastQuality.LOSSLESS)),
                    case("`2007-02-23T12:14Z`", "$DATE_ANNOTATION::2007-02-23", CastQuality.LOSSY),
                    case("`2007-02-23T12:14:33.079Z`", "$DATE_ANNOTATION::2007-02-23", CastQuality.LOSSY),
                    case("`2007-02-23T12:14:33.079-08:00`", "$DATE_ANNOTATION::2007-02-23", CastQuality.LOSSY),
                    case("`2007-02T`", "$DATE_ANNOTATION::2007-02-01", NotImplemented(CastQuality.LOSSLESS)),
                    case("`2007T`", "$DATE_ANNOTATION::2007-01-01", NotImplemented(CastQuality.LOSSLESS)),
                    // text
                    case("'hello'", ErrorCode.EVALUATOR_CAST_FAILED),
                    case("'2016-03-01T01:12:12Z'", ErrorCode.EVALUATOR_CAST_FAILED),
                    case("""`"2001-01-01"`""", "$DATE_ANNOTATION::2001-01-01", CastQuality.LOSSLESS),
                    case("""`"+20212-02-01"`""", ErrorCode.EVALUATOR_CAST_FAILED),
                    case("""`"20212-02-01"`""", ErrorCode.EVALUATOR_CAST_FAILED),
                    case("""`'2000T'`""", ErrorCode.EVALUATOR_CAST_FAILED),
                    case("""`'1999-04T'`""", ErrorCode.EVALUATOR_CAST_FAILED),
                    // lob
                    case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // list
                    case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // sexp
                    case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // struct
                    case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // bag
                    case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST)
                ).types(ExprValueType.DATE.typeAliases()),
                // Find more coverage for the "Cast as Time" tests in `castDateAndTime`.
                listOf(
                    // booleans
                    case("TRUE AND FALSE", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`true`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // numbers
                    case("5", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`0e0`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("1.1", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("-20.1", ErrorCode.EVALUATOR_INVALID_CAST),
                    // timestamp
                    case("`2007-10-10T`", "$TIME_ANNOTATION::{hour:0,minute:0,second:0.,timezone_hour:null.int,timezone_minute:null.int}", CastQuality.LOSSY),
                    case("`2007-02-23T12:14Z`", "$TIME_ANNOTATION::{hour:12,minute:14,second:0.,timezone_hour:null.int,timezone_minute:null.int}", CastQuality.LOSSY),
                    case("`2007-02-23T12:14:33.079Z`", "$TIME_ANNOTATION::{hour:12,minute:14,second:33.079,timezone_hour:null.int,timezone_minute:null.int}", CastQuality.LOSSY),
                    case("`2007-02-23T12:14:33.079-08:00`", "$TIME_ANNOTATION::{hour:12,minute:14,second:33.079,timezone_hour:null.int,timezone_minute:null.int}", CastQuality.LOSSY),
                    case("`2007-02T`", "$TIME_ANNOTATION::{hour:0,minute:0,second:0.,timezone_hour:null.int,timezone_minute:null.int}", CastQuality.LOSSY),
                    case("`2007T`", "$TIME_ANNOTATION::{hour:0,minute:0,second:0.,timezone_hour:null.int,timezone_minute:null.int}", CastQuality.LOSSY),
                    // text
                    case("'hello'", ErrorCode.EVALUATOR_CAST_FAILED),
                    case("'2016-03-01T01:12:12Z'", ErrorCode.EVALUATOR_CAST_FAILED),
                    case("""`"23:2:12.12345"`""", ErrorCode.EVALUATOR_CAST_FAILED),
                    case("""`"+20212-02-01"`""", ErrorCode.EVALUATOR_CAST_FAILED),
                    case("""`"20212-02-01"`""", ErrorCode.EVALUATOR_CAST_FAILED),
                    case("""`'2000T'`""", ErrorCode.EVALUATOR_CAST_FAILED),
                    case("""`'1999-04T'`""", ErrorCode.EVALUATOR_CAST_FAILED),
                    // lob
                    case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // list
                    case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // sexp
                    case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // struct
                    case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // bag
                    case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST)
                ).types(ExprValueType.TIME.typeAliases()),
                listOf(
                    // booleans
                    case("TRUE AND FALSE", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`true`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // numbers
                    case("5", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`0e0`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("1.1", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("-20.1", ErrorCode.EVALUATOR_INVALID_CAST),
                    // timestamp
                    case("`2007-10-10T`", "2007-10-10T", CastQuality.LOSSLESS),
                    // text
                    case("'hello'", ErrorCode.EVALUATOR_CAST_FAILED),
                    case("'2016-03-01T01:12:12Z'", "2016-03-01T01:12:12Z", CastQuality.LOSSLESS),
                    case("""`"2001-01-01"`""", "2001-01-01T", CastQuality.LOSSLESS),
                    case("""`'2000T'`""", "2000T", CastQuality.LOSSLESS),
                    case("""`'1999-04T'`""", "1999-04T", CastQuality.LOSSLESS),
                    // lob
                    case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // list
                    case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // sexp
                    case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // struct
                    case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // bag
                    case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST)
                ).types(ExprValueType.TIMESTAMP.typeAliases()),
                listOf(
                    // booleans
                    case("TRUE AND FALSE", "'false'", CastQuality.LOSSLESS),
                    case("`true`", "'true'", CastQuality.LOSSLESS),
                    // numbers
                    case("5", "'5'", CastQuality.LOSSLESS),
                    case("`0e0`", "'0.0'", CastQuality.LOSSLESS),
                    case("1.1", "'1.1'", CastQuality.LOSSLESS),
                    case("-20.1", "'-20.1'", CastQuality.LOSSLESS),
                    // timestamp
                    case("`2007-10-10T`", "'2007-10-10'", CastQuality.LOSSLESS),
                    // text
                    case("'hello'", "'hello'", CastQuality.LOSSLESS),
                    case("'-20'", "'-20'", CastQuality.LOSSLESS),
                    case("""`"1000"`""", "'1000'", CastQuality.LOSSLESS),
                    case("""`'2e100'`""", "'2e100'", CastQuality.LOSSLESS),
                    case("""`'2d100'`""", "'2d100'", CastQuality.LOSSLESS),
                    // lob
                    case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"1.0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"2e10"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{{MA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 0
                    case("`{{MS4w}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 1.0
                    case("`{{MmUxMA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 2e10
                    // list
                    case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("['hello']", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`[-2d0, 0d0]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // sexp
                    case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`(1d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`(0d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // struct
                    case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{a:12d0}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("{'b':`-4d0`}", ErrorCode.EVALUATOR_INVALID_CAST),
                    // bag
                    case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("<<`14d0`>>", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("<<`20d0`>>", ErrorCode.EVALUATOR_INVALID_CAST)
                ).types(ExprValueType.SYMBOL.typeAliases()),
                listOf(
                    // booleans
                    case("TRUE AND FALSE", "\"false\"", CastQuality.LOSSLESS),
                    case("`true`", "\"true\"", CastQuality.LOSSLESS),
                    // numbers
                    case("5", "\"5\"", CastQuality.LOSSLESS),
                    case("`0e0`", "\"0.0\"", CastQuality.LOSSLESS),
                    case("1.1", "\"1.1\"", CastQuality.LOSSLESS),
                    case("-20.1", "\"-20.1\"", CastQuality.LOSSLESS),
                    // timestamp
                    case("`2007-10-10T`", "\"2007-10-10\"", CastQuality.LOSSLESS),
                    // text
                    case("'hello'", "\"hello\"", CastQuality.LOSSLESS),
                    case("'-20'", "\"-20\"", CastQuality.LOSSLESS),
                    case("""`"1000"`""", "\"1000\"", CastQuality.LOSSLESS),
                    case("""`'2e100'`""", "\"2e100\"", CastQuality.LOSSLESS),
                    case("""`'2d100'`""", "\"2d100\"", CastQuality.LOSSLESS),
                    // lob
                    case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"1.0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"2e10"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{{MA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 0
                    case("`{{MS4w}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 1.0
                    case("`{{MmUxMA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 2e10
                    // list
                    case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("['hello']", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`[-2d0, 0d0]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // sexp
                    case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`(1d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`(0d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // struct
                    case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{a:12d0}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("{'b':`-4d0`}", ErrorCode.EVALUATOR_INVALID_CAST),
                    // bag
                    case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("<<`14d0`>>", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("<<'a', <<'hello'>>>>", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("<<`20d0`>>", ErrorCode.EVALUATOR_INVALID_CAST)
                ).types(listOf("STRING", "VARCHAR", "CHARACTER VARYING")),
                listOf(
                    // booleans
                    case("TRUE AND FALSE", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`true`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // numbers
                    case("5", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`0e0`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("1.1", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("-20.1", ErrorCode.EVALUATOR_INVALID_CAST),
                    // timestamp
                    case("`2007-10-10T`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // text
                    case("'hello'", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("'-20'", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`"1000"`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`'2e100'`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`'2d100'`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    // lob
                    case("""`{{""}}`""", """{{""}}""", CastQuality.LOSSLESS),
                    case("""`{{"0"}}`""", """{{"0"}}""", CastQuality.LOSSLESS),
                    case("""`{{"1.0"}}`""", """{{"1.0"}}""", CastQuality.LOSSLESS),
                    case("""`{{"2e10"}}`""", """{{"2e10"}}""", CastQuality.LOSSLESS),
                    case("`{{}}`", """{{""}}""", CastQuality.LOSSLESS),
                    case("`{{MA==}}`", """{{"0"}}""", CastQuality.LOSSLESS),
                    case("`{{MS4w}}`", """{{"1.0"}}""", CastQuality.LOSSLESS),
                    case("`{{MmUxMA==}}`", """{{"2e10"}}""", CastQuality.LOSSLESS),
                    // list
                    case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("['hello']", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`[-2d0, 0d0]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // sexp
                    case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`(1d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`(0d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // struct
                    case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{a:12d0}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("{'b':`-4d0`}", ErrorCode.EVALUATOR_INVALID_CAST),
                    // bag
                    case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("<<`14d0`>>", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("<<`20d0`>>", ErrorCode.EVALUATOR_INVALID_CAST)
                ).types(ExprValueType.CLOB.typeAliases()),
                listOf(
                    // booleans
                    case("TRUE AND FALSE", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`true`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // numbers
                    case("5", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`0e0`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("1.1", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("-20.1", ErrorCode.EVALUATOR_INVALID_CAST),
                    // timestamp
                    case("`2007-10-10T`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // text
                    case("'hello'", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("'-20'", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`"1000"`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`'2e100'`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`'2d100'`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    // lob
                    case("""`{{""}}`""", """{{}}""", CastQuality.LOSSLESS),
                    case("""`{{"0"}}`""", """{{MA==}}""", CastQuality.LOSSLESS),
                    case("""`{{"1.0"}}`""", """{{MS4w}}""", CastQuality.LOSSLESS),
                    case("""`{{"2e10"}}`""", """{{MmUxMA==}}""", CastQuality.LOSSLESS),
                    case("`{{}}`", """{{}}""", CastQuality.LOSSLESS),
                    case("`{{MA==}}`", """{{MA==}}""", CastQuality.LOSSLESS), // 0
                    case("`{{MS4w}}`", """{{MS4w}}""", CastQuality.LOSSLESS), // 1.0
                    case("`{{MmUxMA==}}`", """{{MmUxMA==}}""", CastQuality.LOSSLESS), // 2e10
                    // list
                    case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("['hello']", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`[-2d0, 0d0]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // sexp
                    case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`(1d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`(0d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // struct
                    case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{a:12d0}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("{'b':`-4d0`}", ErrorCode.EVALUATOR_INVALID_CAST),
                    // bag
                    case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("<<`14d0`>>", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("<<`20d0`>>", ErrorCode.EVALUATOR_INVALID_CAST)
                ).types(ExprValueType.BLOB.typeAliases()),
                listOf(
                    // booleans
                    case("TRUE AND FALSE", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`true`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // numbers
                    case("5", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`0e0`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("1.1", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("-20.1", ErrorCode.EVALUATOR_INVALID_CAST),
                    // timestamp
                    case("`2007-10-10T`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // text
                    case("'hello'", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("'-20'", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`"1000"`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`'2e100'`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`'2d100'`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    // lob
                    case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"1.0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"2e10"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{{MA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 0
                    case("`{{MS4w}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 1.0
                    case("`{{MmUxMA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 2e10
                    // list
                    case("`[]`", "[]", CastQuality.LOSSLESS),
                    case("['hello']", "[\"hello\"]", CastQuality.LOSSLESS),
                    case("`[-2d0, 0d0]`", "[-2d0, 0d0]", CastQuality.LOSSLESS),
                    // sexp
                    case("`()`", "[]", CastQuality.LOSSLESS),
                    case("`(1d0)`", "[1d0]", CastQuality.LOSSLESS),
                    case("`(0d0)`", "[0d0]", CastQuality.LOSSLESS),
                    // struct
                    case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{a:12d0}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("{'b':`-4d0`}", ErrorCode.EVALUATOR_INVALID_CAST),
                    // bag
                    case("<<>>", "[]", CastQuality.LOSSLESS), // TODO bag verification
                    case("<<`14d0`>>", "[14d0]", CastQuality.LOSSLESS), // TODO bag verification
                    case("<<`20d0`>>", "[20d0]", CastQuality.LOSSLESS) // TODO bag verification
                ).types(ExprValueType.LIST.typeAliases()),
                listOf(
                    // booleans
                    case("TRUE AND FALSE", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`true`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // numbers
                    case("5", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`0e0`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("1.1", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("-20.1", ErrorCode.EVALUATOR_INVALID_CAST),
                    // timestamp
                    case("`2007-10-10T`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // text
                    case("'hello'", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("'-20'", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`"1000"`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`'2e100'`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`'2d100'`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    // lob
                    case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"1.0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"2e10"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{{MA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 0
                    case("`{{MS4w}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 1.0
                    case("`{{MmUxMA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 2e10
                    // list
                    case("`[]`", "()", CastQuality.LOSSLESS),
                    case("['hello']", "(\"hello\")", CastQuality.LOSSLESS),
                    case("`[-2d0, 0d0]`", "(-2d0 0d0)", CastQuality.LOSSLESS),
                    // sexp
                    case("`()`", "()", CastQuality.LOSSLESS),
                    case("`(1d0)`", "(1d0)", CastQuality.LOSSLESS),
                    case("`(0d0)`", "(0d0)", CastQuality.LOSSLESS),
                    // struct
                    case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{a:12d0}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("{'b':`-4d0`}", ErrorCode.EVALUATOR_INVALID_CAST),
                    // bag
                    case("<<>>", "()", CastQuality.LOSSLESS),
                    case("<<`14d0`>>", "(14d0)", CastQuality.LOSSLESS),
                    case("<<`20d0`>>", "(20d0)", CastQuality.LOSSLESS)
                ).types(ExprValueType.SEXP.typeAliases()),
                listOf(
                    // booleans
                    case("TRUE AND FALSE", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`true`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // numbers
                    case("5", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`0e0`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("1.1", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("-20.1", ErrorCode.EVALUATOR_INVALID_CAST),
                    // timestamp
                    case("`2007-10-10T`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // text
                    case("'hello'", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("'-20'", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`"1000"`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`'2e100'`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`'2d100'`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    // lob
                    case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"1.0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"2e10"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{{MA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 0
                    case("`{{MS4w}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 1.0
                    case("`{{MmUxMA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 2e10
                    // list
                    case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("['hello']", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`[-2d0, 0d0]`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // sexp
                    case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`(1d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`(0d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // struct
                    case("`{}`", "{}", CastQuality.LOSSLESS),
                    case("{}", "{}", CastQuality.LOSSLESS),
                    case("`{a:12d0}`", "{a:12d0}", CastQuality.LOSSLESS),
                    case("{'b':`-4d0`}", "{b:-4d0}", CastQuality.LOSSLESS),
                    // bag
                    case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("<<`14d0`>>", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("<<`20d0`>>", ErrorCode.EVALUATOR_INVALID_CAST)
                ).types(ExprValueType.STRUCT.typeAliases()),
                listOf(
                    // booleans
                    case("TRUE AND FALSE", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`true`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // numbers
                    case("5", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`0e0`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("1.1", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("-20.1", ErrorCode.EVALUATOR_INVALID_CAST),
                    // timestamp
                    case("`2007-10-10T`", ErrorCode.EVALUATOR_INVALID_CAST),
                    // text
                    case("'hello'", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("'-20'", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`"1000"`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`'2e100'`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`'2d100'`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    // lob
                    case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"1.0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("""`{{"2e10"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{{MA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 0
                    case("`{{MS4w}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 1.0
                    case("`{{MmUxMA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 2e10
                    // list
                    case("`[]`", "[]", CastQuality.LOSSLESS), // TODO bag verification
                    case("['hello']", "[\"hello\"]", CastQuality.LOSSLESS), // TODO bag verification
                    case("`[-2d0, 0d0]`", "[-2d0, 0d0]", CastQuality.LOSSLESS), // TODO bag verification
                    // sexp
                    case("`()`", "[]", CastQuality.LOSSLESS), // TODO bag verification
                    case("`(1d0)`", "[1d0]", CastQuality.LOSSLESS), // TODO bag verification
                    case("`(0d0)`", "[0d0]", CastQuality.LOSSLESS), // TODO bag verification
                    // struct
                    case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{a:12d0}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("{'b':`-4d0`}", ErrorCode.EVALUATOR_INVALID_CAST),
                    // bag
                    case("<<>>", "[]", CastQuality.LOSSLESS), // TODO bag verification
                    case("<<`14d0`>>", "[14d0]", CastQuality.LOSSLESS), // TODO bag verification
                    case("<<`20d0`>>", "[20d0]", CastQuality.LOSSLESS) // TODO bag verification
                ).types(ExprValueType.BAG.typeAliases())
            ).flatten()

        val deviatingLegacyTestCases = listOf(
            listOf(
                // booleans
                case("TRUE AND FALSE", "\"false\"", CastQuality.LOSSLESS),
                case("`true`", "\"true\"", CastQuality.LOSSLESS),
                // numbers
                case("5", "\"5\"", CastQuality.LOSSLESS),
                case("`0e0`", "\"0.0\"", CastQuality.LOSSLESS),
                case("1.1", "\"1.1\"", CastQuality.LOSSLESS),
                case("-20.1", "\"-20.1\"", CastQuality.LOSSLESS),
                // inf, -inf, nan
                case("`+inf`", "\"Infinity\"", CastQuality.LOSSLESS),
                case("`-inf`", "\"-Infinity\"", CastQuality.LOSSLESS),
                case("`nan`", "\"NaN\"", CastQuality.LOSSLESS),
                // timestamp
                case("`2007-10-10T`", "\"2007-10-10\"", CastQuality.LOSSLESS),
                // text
                case("'hello'", "\"hello\"", CastQuality.LOSSLESS),
                case("'-20'", "\"-20\"", CastQuality.LOSSLESS),
                case("""`"1000"`""", "\"1000\"", CastQuality.LOSSLESS),
                case("""`'2e100'`""", "\"2e100\"", CastQuality.LOSSLESS),
                case("""`'2d100'`""", "\"2d100\"", CastQuality.LOSSLESS),
                // lob
                case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                case("""`{{"0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                case("""`{{"1.0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                case("""`{{"2e10"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`{{MA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 0
                case("`{{MS4w}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 1.0
                case("`{{MmUxMA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 2e10
                // list
                case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("['hello']", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`[-2d0, 0d0]`", ErrorCode.EVALUATOR_INVALID_CAST),
                // sexp
                case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`(1d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`(0d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                // struct
                case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`{a:12d0}`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("{'b':`-4d0`}", ErrorCode.EVALUATOR_INVALID_CAST),
                // bag
                case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST),
                case("<<`14d0`>>", ErrorCode.EVALUATOR_INVALID_CAST),
                case("<<'a', <<'hello'>>>>", ErrorCode.EVALUATOR_INVALID_CAST),
                case("<<`20d0`>>", ErrorCode.EVALUATOR_INVALID_CAST)
            ).types(listOf("CHAR", "CHARACTER")),
            // rounding tests
            listOf(
                case("1.9", "1", CastQuality.LOSSY),
                case("-20.9", "-20", CastQuality.LOSSY),
                case("1.5", "1", CastQuality.LOSSY),
                case("2.5", "2", CastQuality.LOSSY)
            ).types(ExprValueType.INT.typeAliases()),
            // SMALLINT tests
            listOf(
                // over range
                case("32768", "32768", CastQuality.LOSSLESS),
                case("-32769", "-32769", CastQuality.LOSSLESS),
                // within range, rounded
                case("1.5", "1", CastQuality.LOSSY),
                // borderline
                case("32767.3", "32767", CastQuality.LOSSY),
                case("32767.5", "32767", CastQuality.LOSSY),
                case("32767.8", "32767", CastQuality.LOSSY),
                case("-32768.3", "-32768", CastQuality.LOSSY),
                case("-32768.5", "-32768", CastQuality.LOSSY),
                case("-32768.9", "-32768", CastQuality.LOSSY),
            ).types(listOf("SMALLINT", "INT2", "INTEGER2")),
            // INT4 tests
            listOf(
                // over range
                case("2147483647", "2147483647", CastQuality.LOSSLESS),
                case("-2147483648", "-2147483648", CastQuality.LOSSLESS),
                // within range, rounded
                case("1.5", "1", CastQuality.LOSSY),
                // borderline
                case("2147483647.3", "2147483647", CastQuality.LOSSY),
                case("2147483647.5", "2147483647", CastQuality.LOSSY),
                case("2147483647.8", "2147483647", CastQuality.LOSSY),
                case("-2147483648.3", "-2147483648", CastQuality.LOSSY),
                case("-2147483648.5", "-2147483648", CastQuality.LOSSY),
                case("-2147483648.9", "-2147483648", CastQuality.LOSSY),
            ).types(listOf("INT4", "INTEGER4")),
            // LONG tests
            listOf(
                case("9223372036854775807", "9223372036854775807", CastQuality.LOSSLESS),
                case("9223372036854775807.3", "9223372036854775807", CastQuality.LOSSY),
                case("9223372036854775807.5", "9223372036854775807", CastQuality.LOSSY),
                case("9223372036854775807.8", "9223372036854775807", CastQuality.LOSSY),
                case("9223372036854775808", ErrorCode.SEMANTIC_LITERAL_INT_OVERFLOW),
                // A very large decimal
                case("1e2147483609", ErrorCode.EVALUATOR_INTEGER_OVERFLOW)
            ).types(listOf("INT", "INTEGER", "INTEGER8", "INT8", "BIGINT")),
            // DECIMAL(3) ; LEGACY mode does not respect DECIMAL's precison or scale
            listOf(
                case("12", "12.", CastQuality.LOSSLESS),
                case("123", "123.", CastQuality.LOSSLESS),
                case("1234", "1234.", CastQuality.LOSSLESS),
                case("123.45", "123.45", CastQuality.LOSSLESS),
                case("'12'", "12.", CastQuality.LOSSLESS),
                case("'123'", "123.", CastQuality.LOSSLESS),
                case("'1234'", "1234.", CastQuality.LOSSLESS),
                case("'123.45'", "123.45", CastQuality.LOSSLESS)
            ).types(ExprValueType.DECIMAL.typeAliases().map { "$it(3)" }),
            // DECIMAL(5,2) ; LEGACY mode does not respect DECIMAL's precison or scale
            listOf(
                case("12", "12.", CastQuality.LOSSLESS),
                case("123", "123.", CastQuality.LOSSLESS),
                case("1234", "1234.", CastQuality.LOSSLESS),
                case("123.45", "123.45", CastQuality.LOSSLESS),
                case("123.459", "123.459", CastQuality.LOSSLESS),
                case("'12'", "12.", CastQuality.LOSSLESS),
                case("'123'", "123.", CastQuality.LOSSLESS),
                case("'1234'", "1234.", CastQuality.LOSSLESS),
                case("'123.45'", "123.45", CastQuality.LOSSLESS),
                case("'123.459'", "123.459", CastQuality.LOSSLESS)
            ).types(ExprValueType.DECIMAL.typeAliases().map { "$it(5, 2)" }),
            // DECIMAL(4,4) ; LEGACY mode does not respect DECIMAL's precison or scale; precision = scale is valid here
            listOf(
                case("0.1", "1d-1", CastQuality.LOSSLESS),
                case("0.1234", "0.1234", CastQuality.LOSSLESS),
                case("0.12345", "0.12345", CastQuality.LOSSLESS)
            ).types(ExprValueType.DECIMAL.typeAliases().map { "$it(4,4)" }),
            // DECIMAL(2, 4) ; LEGACY mode does not respect DECIMAL's precison or scale; precision < scale is valid in legacy mode
            listOf(
                case("1", "1d0", CastQuality.LOSSLESS)
            ).types(ExprValueType.DECIMAL.typeAliases().map { "$it(2,4)" }),
            // VARCHAR(4) legacy mode doesn't care about params
            listOf(
                // from string types
                case("'a'", "\"a\"", CastQuality.LOSSLESS),
                case("'abcde'", "\"abcde\"", CastQuality.LOSSLESS),
                case("'💩💩💩💩💩'", "\"💩💩💩💩💩\"", CastQuality.LOSSLESS), // legacy behavior does not truncate.
                // from non-string types
                case("TRUE AND FALSE", "\"false\"", CastQuality.LOSSLESS),
                case("`true`", "\"true\"", CastQuality.LOSSLESS),
                case("5", "\"5\"", CastQuality.LOSSLESS),
                case("`0e0`", "\"0.0\"", CastQuality.LOSSLESS),
                case("1.1", "\"1.1\"", CastQuality.LOSSLESS),
                case("-20.1", "\"-20.1\"", CastQuality.LOSSLESS)
            ).types(listOf("VARCHAR(4)", "CHARACTER VARYING(4)")),
            listOf(
                // from string types
                case("'a'", "\"a\"", CastQuality.LOSSLESS),
                case("'a    '", "\"a    \"", CastQuality.LOSSLESS),
                case("'a\t\t'", "\"a\t\t\"", CastQuality.LOSSLESS),
                case("'abcde'", "\"abcde\"", CastQuality.LOSSLESS),
                case("'💩💩💩💩💩'", "\"💩💩💩💩💩\"", CastQuality.LOSSLESS), // legacy behavior does not truncate.
                // from non-string types
                case("TRUE AND FALSE", "\"false\"", CastQuality.LOSSLESS),
                case("`true`", "\"true\"", CastQuality.LOSSLESS),
                // numbers
                case("5", "\"5\"", CastQuality.LOSSLESS),
                case("`0e0`", "\"0.0\"", CastQuality.LOSSLESS),
                case("1.1", "\"1.1\"", CastQuality.LOSSLESS),
                case("-20.1", "\"-20.1\"", CastQuality.LOSSLESS)
            ).types(listOf("CHAR(4)", "CHARACTER(4)"))
        ).flatten()

        val deviatingParamsTestCases = listOf(
            // CHAR without params, equaivalent to CHAR(1)
            listOf(
                // booleans
                case("TRUE AND FALSE", "\"f\"", CastQuality.LOSSY),
                case("`true`", "\"t\"", CastQuality.LOSSY),
                // numbers
                case("5", "\"5\"", CastQuality.LOSSLESS),
                case("`0e0`", "\"0\"", CastQuality.LOSSLESS),
                case("1.1", "\"1\"", CastQuality.LOSSY),
                case("-20.1", "\"-\"", CastQuality.LOSSY),
                // inf, -inf, nan
                case("`+inf`", "\"I\"", CastQuality.LOSSY),
                case("`-inf`", "\"-\"", CastQuality.LOSSY),
                case("`nan`", "\"N\"", CastQuality.LOSSY),
                // timestamp
                case("`2007-10-10T`", "\"2\"", CastQuality.LOSSY),
                // text
                case("'hello'", "\"h\"", CastQuality.LOSSY),
                case("'-20'", "\"-\"", CastQuality.LOSSY),
                case("""`"1000"`""", "\"1\"", CastQuality.LOSSY),
                case("""`'2e100'`""", "\"2\"", CastQuality.LOSSY),
                case("""`'2d100'`""", "\"2\"", CastQuality.LOSSY),
                // lob
                case("""`{{""}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                case("""`{{"0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                case("""`{{"1.0"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                case("""`{{"2e10"}}`""", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`{{}}`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`{{MA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 0
                case("`{{MS4w}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 1.0
                case("`{{MmUxMA==}}`", ErrorCode.EVALUATOR_INVALID_CAST), // 2e10
                // list
                case("`[]`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("['hello']", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`[-2d0, 0d0]`", ErrorCode.EVALUATOR_INVALID_CAST),
                // sexp
                case("`()`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`(1d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`(0d0)`", ErrorCode.EVALUATOR_INVALID_CAST),
                // struct
                case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`{a:12d0}`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("{'b':`-4d0`}", ErrorCode.EVALUATOR_INVALID_CAST),
                // bag
                case("<<>>", ErrorCode.EVALUATOR_INVALID_CAST),
                case("<<`14d0`>>", ErrorCode.EVALUATOR_INVALID_CAST),
                case("<<'a', <<'hello'>>>>", ErrorCode.EVALUATOR_INVALID_CAST),
                case("<<`20d0`>>", ErrorCode.EVALUATOR_INVALID_CAST)
            ).types(listOf("CHAR", "CHARACTER")),
            // rounding tests
            listOf(
                case("1.9", "2", CastQuality.LOSSY),
                case("-20.9", "-21", CastQuality.LOSSY),
                case("1.5", "2", CastQuality.LOSSY),
                case("2.5", "2", CastQuality.LOSSY)
            ).types(ExprValueType.INT.typeAliases()),
            // SMALLINT tests
            listOf(
                // over range
                case("32768", ErrorCode.EVALUATOR_INTEGER_OVERFLOW),
                case("-32769", ErrorCode.EVALUATOR_INTEGER_OVERFLOW),
                // within range, rounded
                case("1.5", "2", CastQuality.LOSSY),
                // borderline
                case("32767.3", "32767", CastQuality.LOSSY),
                case("32767.5", ErrorCode.EVALUATOR_INTEGER_OVERFLOW),
                case("32767.8", ErrorCode.EVALUATOR_INTEGER_OVERFLOW),
                case("-32768.3", "-32768", CastQuality.LOSSY),
                case("-32768.5", "-32768", CastQuality.LOSSY),
                case("-32768.9", ErrorCode.EVALUATOR_INTEGER_OVERFLOW)
            ).types(listOf("SMALLINT", "INT2", "INTEGER2")),
            // INT4 tests
            listOf(
                // over range
                case("2147483648", ErrorCode.EVALUATOR_INTEGER_OVERFLOW),
                case("-2147483649", ErrorCode.EVALUATOR_INTEGER_OVERFLOW),
                // within range, rounded
                case("1.5", "2", CastQuality.LOSSY),
                // borderline
                case("2147483647.3", "2147483647", CastQuality.LOSSY),
                case("2147483647.5", ErrorCode.EVALUATOR_INTEGER_OVERFLOW),
                case("2147483647.8", ErrorCode.EVALUATOR_INTEGER_OVERFLOW),
                case("-2147483648.3", "-2147483648", CastQuality.LOSSY),
                case("-2147483648.5", "-2147483648", CastQuality.LOSSY),
                case("-2147483648.9", ErrorCode.EVALUATOR_INTEGER_OVERFLOW)
            ).types(listOf("INT4", "INTEGER4")),
            // LONG tests
            listOf(
                case("9223372036854775807", "9223372036854775807", CastQuality.LOSSLESS),
                case("9223372036854775807.3", "9223372036854775807", CastQuality.LOSSY),
                case("9223372036854775807.5", ErrorCode.EVALUATOR_INTEGER_OVERFLOW),
                case("9223372036854775807.8", ErrorCode.EVALUATOR_INTEGER_OVERFLOW),
                case("9223372036854775808", ErrorCode.SEMANTIC_LITERAL_INT_OVERFLOW),
                // A very large decimal
                case("1e2147483609", ErrorCode.EVALUATOR_INTEGER_OVERFLOW)
            ).types(listOf("INT")),
            // DECIMAL(3)
            listOf(
                case("12", "12.", CastQuality.LOSSLESS),
                case("123", "123.", CastQuality.LOSSLESS),
                case("1234", ErrorCode.EVALUATOR_CAST_FAILED),
                case("123.45", "123.", CastQuality.LOSSY),
                case("'12'", "12.", CastQuality.LOSSLESS),
                case("'123'", "123.", CastQuality.LOSSLESS),
                case("'1234'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("'123.45'", "123.", CastQuality.LOSSY)
            ).types(ExprValueType.DECIMAL.typeAliases().map { "$it(3)" }),
            // DECIMAL(5,2)
            listOf(
                case("12", "12.00", CastQuality.LOSSLESS),
                case("123", "123.00", CastQuality.LOSSLESS),
                case("1234", ErrorCode.EVALUATOR_CAST_FAILED),
                case("123.45", "123.45", CastQuality.LOSSLESS),
                case("123.459", "123.46", CastQuality.LOSSY),
                case("'12'", "12.00", CastQuality.LOSSY),
                case("'123'", "123.00", CastQuality.LOSSY),
                case("'1234'", ErrorCode.EVALUATOR_CAST_FAILED),
                case("'123.45'", "123.45", CastQuality.LOSSLESS),
                case("'123.459'", "123.46", CastQuality.LOSSY)
            ).types(ExprValueType.DECIMAL.typeAliases().map { "$it(5, 2)" }),
            // DECIMAL(4,4) precision = scale is valid in honor_params
            listOf(
                case("0.1", "1.000d-1", CastQuality.LOSSLESS),
                case("0.1234", "0.1234", CastQuality.LOSSLESS),
                case("0.12345", "0.1235", CastQuality.LOSSY)
            ).types(ExprValueType.DECIMAL.typeAliases().map { "$it(4,4)" }),
            // DECIMAL(2, 4) is a compilation failure in this mode
            listOf(
                case("1", ErrorCode.SEMANTIC_INVALID_DECIMAL_ARGUMENTS)
            ).types(ExprValueType.DECIMAL.typeAliases().map { "$it(2,4)" }),
            // VARCHAR(4) should truncate to size <= 4
            listOf(
                // from string types
                case("'a'", "\"a\"", CastQuality.LOSSLESS),
                case("'a    '", "\"a   \"", CastQuality.LOSSY),
                case("'abcde'", "\"abcd\"", CastQuality.LOSSY),
                // 4-byte unicode
                case("'💩💩💩💩💩'", "\"💩💩💩💩\"", CastQuality.LOSSY),
                case("TRUE AND FALSE", "\"fals\"", CastQuality.LOSSY),
                case("`true`", "\"true\"", CastQuality.LOSSLESS),
                case("5", "\"5\"", CastQuality.LOSSLESS),
                case("`0e0`", "\"0.0\"", CastQuality.LOSSLESS),
                case("1.1", "\"1.1\"", CastQuality.LOSSLESS),
                case("-20.1", "\"-20.\"", CastQuality.LOSSY)
            ).types(listOf("VARCHAR(4)", "CHARACTER VARYING(4)")),
            // CHAR(4) should truncate or pad with whitespace to size <= 4
            listOf(
                // from string types
                case("'a'", "\"a\"", CastQuality.LOSSLESS),
                case("'a'", "\"a\"", CastQuality.LOSSLESS),
                case("'abcde'", "\"abcd\"", CastQuality.LOSSY),
                case("'💩💩💩💩💩'", "\"💩💩💩💩\"", CastQuality.LOSSY),
                case("TRUE AND FALSE", "\"fals\"", CastQuality.LOSSY),
                case("`true`", "\"true\"", CastQuality.LOSSLESS),
                // numbers
                case("5", "\"5\"", CastQuality.LOSSLESS),
                case("`0e0`", "\"0.0\"", CastQuality.LOSSLESS),
                case("1.1", "\"1.1\"", CastQuality.LOSSLESS),
                case("-20.1", "\"-20.\"", CastQuality.LOSSY)
            ).types(listOf("CHAR(4)", "CHARACTER(4)"))
        ).flatten()

        // collection of test demostrating how cast([`+inf` | `-inf` | `nan`] as <type>) behave
        val infinityOrNanTestCases = listOf(
            // cast([`+inf` | `-inf` | `nan`] as [INT | DECIMAL]) returns Evaluator_CAST_FAILED error
            listOf(
                case("`+inf`", ErrorCode.EVALUATOR_CAST_FAILED),
                case("`-inf`", ErrorCode.EVALUATOR_CAST_FAILED),
                case("`nan`", ErrorCode.EVALUATOR_CAST_FAILED),
            ).types(ExprValueType.INT.typeAliases() + ExprValueType.DECIMAL.typeAliases()),
            // cast([`+inf` | `-inf` | `nan`] as FLOAT) returns the original value
            listOf(
                case("`+inf`", "+inf", CastQuality.LOSSLESS),
                case("`-inf`", "-inf", CastQuality.LOSSLESS),
                case("`nan`", "nan", CastQuality.LOSSLESS)
            ).types(ExprValueType.FLOAT.typeAliases()),
            // cast([`+inf` | `-inf` | `nan`] as STRING) returns "Infinity", "-Infinity", and "NaN" respectively.
            // for casting behavor with parametered char, character, see [deviatingParamsTestCases] and [deviatingLegacyTestCases]
            listOf(
                case("`+inf`", "\"Infinity\"", CastQuality.LOSSLESS),
                case("`-inf`", "\"-Infinity\"", CastQuality.LOSSLESS),
                case("`nan`", "\"NaN\"", CastQuality.LOSSLESS)
            ).types(listOf("STRING", "VARCHAR", "CHARACTER VARYING")),
            // cast([`+inf` | `-inf` | `nan`] as STRING) returns 'Infinity', '-Infinity', and 'NaN' respectively.
            listOf(
                case("`+inf`", "'Infinity'", CastQuality.LOSSLESS),
                case("`-inf`", "'-Infinity'", CastQuality.LOSSLESS),
                case("`nan`", "'NaN'", CastQuality.LOSSLESS)
            ).types(ExprValueType.SYMBOL.typeAliases()),
            // cast([`+inf` | `-inf` | `nan`] as BOOLEAN) returns true, since none of which has value of 0.
            listOf(
                case("`+inf`", "true", CastQuality.LOSSY),
                case("`-inf`", "true", CastQuality.LOSSY),
                case("`nan`", "true", CastQuality.LOSSY)
            ).types(ExprValueType.BOOL.typeAliases()),
            listOf(
                case("`+inf`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`-inf`", ErrorCode.EVALUATOR_INVALID_CAST),
                case("`nan`", ErrorCode.EVALUATOR_INVALID_CAST)
            ).types(
                listOf(ExprValueType.INT, ExprValueType.DECIMAL, ExprValueType.FLOAT, ExprValueType.STRING, ExprValueType.SYMBOL, ExprValueType.BOOL)
                    .map { it.typeAliases() }
                    .fold(allTypeNames) { allTypes, type -> allTypes - type }
            )
        ).flatten()

        private val defaultTimezoneOffset = ZoneOffset.UTC

        private fun createZoneOffset(hours: Int = 0, minutes: Int = 0) = ZoneOffset.ofHoursMinutes(hours, minutes)

        private val commonDateTimeTests = listOf(
            listOf(
                case("DATE '2007-10-10'", "$DATE_ANNOTATION::2007-10-10", CastQuality.LOSSLESS)
            ).types(ExprValueType.DATE.typeAliases()),
            listOf(
                case("DATE '2007-10-10'", "'2007-10-10'", CastQuality.LOSSLESS)
            ).types(ExprValueType.SYMBOL.typeAliases()),
            listOf(
                case("DATE '2007-10-10'", "\"2007-10-10\"", CastQuality.LOSSLESS)
            ).types(listOf("string", "varchar")),
            listOf(
                // CAST(<TIME> AS <variants of TIME type>)
                case("TIME '23:12:12.1267'", "TIME", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.1267, timezone_hour:null.int, timezone_minute:null.int}", CastQuality.LOSSLESS),
                case("TIME '23:12:12.1267-05:30'", "TIME WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.1267, timezone_hour:0, timezone_minute:0}", CastQuality.LOSSLESS),
                case("TIME '23:12:12.1267+05:30'", "TIME (3)", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.127, timezone_hour:null.int, timezone_minute:null.int}", CastQuality.LOSSY),
                case("TIME '23:12:12.1267-05:30'", "TIME (3) WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.127, timezone_hour:0, timezone_minute:0}", CastQuality.LOSSY),
                case("TIME (3) '23:12:12.1267'", "TIME", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.127, timezone_hour:null.int, timezone_minute:null.int}", CastQuality.LOSSLESS),
                case("TIME (3) '23:12:12.1267-05:30'", "TIME", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.127, timezone_hour:null.int, timezone_minute:null.int}", CastQuality.LOSSLESS),
                case("TIME (3) '23:12:12.1267+05:30'", "TIME WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.127, timezone_hour:0, timezone_minute:0}", CastQuality.LOSSLESS),
                case("TIME (3) '23:12:12.1267-05:30'", "TIME (9)", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.127000000, timezone_hour:null.int, timezone_minute:null.int}", CastQuality.LOSSLESS),
                case("TIME WITH TIME ZONE '23:12:12.1267'", "TIME", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.1267, timezone_hour:null.int, timezone_minute:null.int}", CastQuality.LOSSLESS),
                case("TIME WITH TIME ZONE '23:12:12.1267-05:30'", "TIME WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.1267, timezone_hour:-5, timezone_minute:-30}", CastQuality.LOSSLESS),
                case("TIME WITH TIME ZONE '23:12:12.1267+05:30'", "TIME (3) WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.127, timezone_hour:5, timezone_minute:30}", CastQuality.LOSSY),
                case("TIME WITH TIME ZONE '23:12:12.1267-05:30'", "TIME", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.1267, timezone_hour:null.int, timezone_minute:null.int}", CastQuality.LOSSY),
                case("TIME (3) WITH TIME ZONE '23:12:12.1267'", "TIME", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.127, timezone_hour:null.int, timezone_minute:null.int}", CastQuality.LOSSLESS),
                case("TIME (3) WITH TIME ZONE '23:12:12.1267-05:30'", "TIME WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.127, timezone_hour:-5, timezone_minute:-30}", CastQuality.LOSSLESS),
                case("TIME (3) WITH TIME ZONE '23:12:12.1267+05:30'", "TIME (5)", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.12700, timezone_hour:null.int, timezone_minute:null.int}", CastQuality.LOSSY),
                case("TIME (3) WITH TIME ZONE '23:12:12.1267-05:30'", "TIME (5) WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.12700, timezone_hour:-5, timezone_minute:-30}", CastQuality.LOSSLESS),
                // CAST(<TIMESTAMP> AS <variants of TIME type>)
                case("`2007-02-23T12:14:33.079Z`", "TIME", "$TIME_ANNOTATION::{hour:12, minute:14, second:33.079, timezone_hour:null.int, timezone_minute:null.int}", CastQuality.LOSSY),
                case("`2007-02-23T12:14:33.079-08:00`", "TIME", "$TIME_ANNOTATION::{hour:12, minute:14, second:33.079, timezone_hour:null.int, timezone_minute:null.int}", CastQuality.LOSSY),
                case("`2007-02-23T12:14:33.079Z`", "TIME WITH TIME ZONE", "$TIME_ANNOTATION::{hour:12, minute:14, second:33.079, timezone_hour:0, timezone_minute:0}", CastQuality.LOSSY),
                case("`2007-02-23T12:14:33.079-08:00`", "TIME (1)", "$TIME_ANNOTATION::{hour:12, minute:14, second:33.1, timezone_hour:null.int, timezone_minute:null.int}", CastQuality.LOSSY),
                case("`2007-02-23T12:14:33.079-08:00`", "TIME (2) WITH TIME ZONE", "$TIME_ANNOTATION::{hour:12, minute:14, second:33.08, timezone_hour:-8, timezone_minute:0}", CastQuality.LOSSY),
                // CAST(<text> AS <variants of TIME type>)
                case("'23:12:12.1267'", "TIME", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.1267, timezone_hour:null.int, timezone_minute:null.int}", CastQuality.LOSSLESS),
                case("'23:12:12.1267'", "TIME (2)", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.13, timezone_hour:null.int, timezone_minute:null.int}", CastQuality.LOSSY),
                case("'23:12:12.1267'", "TIME WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.1267, timezone_hour:0, timezone_minute:0}", CastQuality.LOSSY),
                case("'23:12:12.1267'", "TIME (2) WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.13, timezone_hour:0, timezone_minute:0}", CastQuality.LOSSY),
                case("""`"23:12:12.1267"`""", "TIME", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.1267, timezone_hour:null.int, timezone_minute:null.int}", CastQuality.LOSSLESS),
                case("""`"23:12:12.1267"`""", "TIME (2)", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.13, timezone_hour:null.int, timezone_minute:null.int}", CastQuality.LOSSY),
                case("""`"23:12:12.1267"`""", "TIME WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.1267, timezone_hour:0, timezone_minute:0}", CastQuality.LOSSY),
                case("""`'23:12:12.1267'`""", "TIME (2) WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.13, timezone_hour:0, timezone_minute:0}", CastQuality.LOSSY),
                case("""`'23:12:12.1267'`""", "TIME", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.1267, timezone_hour:null.int, timezone_minute:null.int}", CastQuality.LOSSLESS),
                case("""`'23:12:12.1267'`""", "TIME (2)", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.13, timezone_hour:null.int, timezone_minute:null.int}", CastQuality.LOSSY),
                case("""`'23:12:12.1267'`""", "TIME WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.1267, timezone_hour:0, timezone_minute:0}", CastQuality.LOSSY),
                case("""`'23:12:12.1267'`""", "TIME (2) WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.13, timezone_hour:0, timezone_minute:0}", CastQuality.LOSSY),
            ),
            // Error cases for TIME
            listOf(
                // Cannot cast timestamp with undefined timezone to "TIME WITH TIME ZONE"
                case("`2007-02-23T`", "TIME WITH TIME ZONE", ErrorCode.EVALUATOR_CAST_FAILED),
                case("`2007-02-23T12:14:33.079-00:00`", "TIME WITH TIME ZONE", ErrorCode.EVALUATOR_CAST_FAILED),
                // Invalid format for time text.
                case("'23:12:2.1267'", "TIME", ErrorCode.EVALUATOR_CAST_FAILED),
                case("'2:12:2.1267'", "TIME (2)", ErrorCode.EVALUATOR_CAST_FAILED),
                case("'25:12:2.1267'", "TIME WITH TIME ZONE", ErrorCode.EVALUATOR_CAST_FAILED),
                case("'24:60:2.1267'", "TIME (2) WITH TIME ZONE", ErrorCode.EVALUATOR_CAST_FAILED),
                case("""`"12:60:12.1267"`""", "TIME", ErrorCode.EVALUATOR_CAST_FAILED),
                case("""`"23:1:12.1267"`""", "TIME (2)", ErrorCode.EVALUATOR_CAST_FAILED),
                case("""`"30:12:12.1267"`""", "TIME WITH TIME ZONE", ErrorCode.EVALUATOR_CAST_FAILED),
                case("""`'23:12:60.1267'`""", "TIME (2) WITH TIME ZONE", ErrorCode.EVALUATOR_CAST_FAILED),
                case("""`'23:12:1.1267'`""", "TIME", ErrorCode.EVALUATOR_CAST_FAILED),
                case("""`'2:12:12.1267'`""", "TIME (2)", ErrorCode.EVALUATOR_CAST_FAILED),
                case("""`'23:1:12.1267'`""", "TIME WITH TIME ZONE", ErrorCode.EVALUATOR_CAST_FAILED),
                case("""`'-23:41:12.1267'`""", "TIME (2) WITH TIME ZONE", ErrorCode.EVALUATOR_CAST_FAILED)
            ),
            // CAST <TIME> AS SYMBOL
            listOf(
                case("TIME '23:12:12.1267'", "'23:12:12.1267'", CastQuality.LOSSLESS),
                case("TIME '23:12:12.1267-05:30'", "'23:12:12.1267'", CastQuality.LOSSLESS),
                case("TIME '23:12:12.1267+05:30'", "'23:12:12.1267'", CastQuality.LOSSLESS),
                case("TIME (3) '23:12:12.1267'", "'23:12:12.127'", CastQuality.LOSSLESS),
                case("TIME (3) '23:12:12.1267-05:30'", "'23:12:12.127'", CastQuality.LOSSLESS),
                case("TIME (3) '23:12:12.1267+05:30'", "'23:12:12.127'", CastQuality.LOSSLESS),
                case("TIME WITH TIME ZONE '23:12:12.1267'", "'23:12:12.1267${defaultTimezoneOffset.getOffsetHHmm()}'", CastQuality.LOSSLESS),
                case("TIME WITH TIME ZONE '23:12:12.1267-05:30'", "'23:12:12.1267-05:30'", CastQuality.LOSSLESS),
                case("TIME WITH TIME ZONE '23:12:12.1267+05:30'", "'23:12:12.1267+05:30'", CastQuality.LOSSLESS),
                case("TIME (3) WITH TIME ZONE '23:12:12.1267'", "'23:12:12.127${defaultTimezoneOffset.getOffsetHHmm()}'", CastQuality.LOSSLESS),
                case("TIME (3) WITH TIME ZONE '23:12:12.1267-05:30'", "'23:12:12.127-05:30'", CastQuality.LOSSLESS),
                case("TIME (3) WITH TIME ZONE '23:12:12.1267+05:30'", "'23:12:12.127+05:30'", CastQuality.LOSSLESS)
            ).types(ExprValueType.SYMBOL.typeAliases()),
            // CAST <TIME> AS STRING
            listOf(
                case("TIME '23:12:12.1267'", "\"23:12:12.1267\"", CastQuality.LOSSLESS),
                case("TIME '23:12:12.1267-05:30'", "\"23:12:12.1267\"", CastQuality.LOSSLESS),
                case("TIME '23:12:12.1267+05:30'", "\"23:12:12.1267\"", CastQuality.LOSSLESS),
                case("TIME (3) '23:12:12.1267'", "\"23:12:12.127\"", CastQuality.LOSSLESS),
                case("TIME (3) '23:12:12.1267-05:30'", "\"23:12:12.127\"", CastQuality.LOSSLESS),
                case("TIME (3) '23:12:12.1267+05:30'", "\"23:12:12.127\"", CastQuality.LOSSLESS),
                case("TIME WITH TIME ZONE '23:12:12.1267'", "\"23:12:12.1267${defaultTimezoneOffset.getOffsetHHmm()}\"", CastQuality.LOSSLESS),
                case("TIME WITH TIME ZONE '23:12:12.1267-05:30'", "\"23:12:12.1267-05:30\"", CastQuality.LOSSLESS),
                case("TIME WITH TIME ZONE '23:12:12.1267+05:30'", "\"23:12:12.1267+05:30\"", CastQuality.LOSSLESS),
                case("TIME (3) WITH TIME ZONE '23:12:12.1267'", "\"23:12:12.127${defaultTimezoneOffset.getOffsetHHmm()}\"", CastQuality.LOSSLESS),
                case("TIME (3) WITH TIME ZONE '23:12:12.1267-05:30'", "\"23:12:12.127-05:30\"", CastQuality.LOSSLESS),
                case("TIME (3) WITH TIME ZONE '23:12:12.1267+05:30'", "\"23:12:12.127+05:30\"", CastQuality.LOSSLESS)
            ).types(listOf("string", "varchar"))
        ).flatten() +
            listOf(ExprValueType.MISSING, ExprValueType.NULL, ExprValueType.BOOL, ExprValueType.INT, ExprValueType.FLOAT, ExprValueType.DECIMAL, ExprValueType.TIMESTAMP, ExprValueType.CLOB, ExprValueType.BLOB, ExprValueType.LIST, ExprValueType.SEXP, ExprValueType.STRUCT, ExprValueType.BAG)
                .map {
                    listOf(case("DATE '2007-10-10'", ErrorCode.EVALUATOR_INVALID_CAST)).types(it.typeAliases())
                }.flatten()

        private val typingModes: Map<String, (CompileOptions.Builder) -> Unit> = mapOf(
            "LEGACY_TYPING_MODE" to { cob -> cob.legacyTypingMode() },
            "PERMISSIVE_TYPING_MODE" to { cob -> cob.permissiveTypingMode() }
        )

        val castBehaviors: Map<String, (CompileOptions.Builder) -> Unit> = mapOf(
            "LEGACY_CAST" to { cob -> cob.legacyCastBehavior() },
            "HONOR_PARAM_CAST" to { cob -> cob.honorTypedOpParameters() }
        )

        private val legacyCastTestCases = (commonTestCases + deviatingLegacyTestCases + infinityOrNanTestCases)
        private val honorParamCastTestCases = (commonTestCases + deviatingParamsTestCases + infinityOrNanTestCases)

        fun List<CastCase>.toPermissive(): List<CastCase> = map { case ->
            when {
                // Note that we do not convert cases where the error is semantic (i.e. static)
                case.expectedErrorCode != null && case.expectedErrorCode.category != ErrorCategory.SEMANTIC -> {
                    // rewrite error code cases to `MISSING` for permissive mode
                    case.copy(expected = "null", expectedErrorCode = null) {
                        assertEquals(ExprValueType.MISSING, it.type)
                    }
                }
                else -> case
            }
        }

        private val castPermissiveConfiguredTestCases = (
            legacyCastTestCases.toPermissive().map { case ->
                ConfiguredCastCase(case, "LEGACY_CAST, PERMISSIVE_TYPING_MODE") {
                    legacyCastBehavior()
                    permissiveTypingMode()
                }
            } + honorParamCastTestCases.toPermissive().map { case ->
                ConfiguredCastCase(case, "HONOR_PARAM_CAST, PERMISSIVE_TYPING_MODE") {
                    honorTypedOpParameters()
                    permissiveTypingMode()
                }
            }
            )

        private val castLegacyConfiguredTestCases = (
            legacyCastTestCases.map { case ->
                ConfiguredCastCase(case, "LEGACY_CAST, LEGACY_ERROR_MODE") {
                    legacyCastBehavior()
                    legacyTypingMode()
                }
            } + honorParamCastTestCases.map { case ->
                ConfiguredCastCase(case, "HONOR_PARAM_CAST, LEGACY_ERROR_MODE") {
                    honorTypedOpParameters()
                    legacyTypingMode()
                }
            }
            )

        private val castDefaultTimezoneOffsetConfiguration =
            // Configuring default timezone offset through CompileOptions
            listOf(
                // CAST(<TIME> AS <TIME WITH TIME ZONE>)
                Pair(case("TIME '23:12:12.1267'", "TIME WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.1267, timezone_hour:0, timezone_minute:0}", CastQuality.LOSSLESS), createZoneOffset()),
                Pair(case("TIME '23:12:12.1267'", "TIME WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.1267, timezone_hour:11, timezone_minute:0}", CastQuality.LOSSLESS), createZoneOffset(11)),
                Pair(case("TIME '23:12:12.1267-05:30'", "TIME WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.1267, timezone_hour:1, timezone_minute:0}", CastQuality.LOSSLESS), createZoneOffset(1)),
                Pair(case("TIME '23:12:12.1267-05:30'", "TIME (2) WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.13, timezone_hour:-5, timezone_minute:-30}", CastQuality.LOSSY), createZoneOffset(-5, -30)),
                // CAST(<TIMESTAMP> AS <TIME WITH TIME ZONE>)
                Pair(case("`2007-02-23T12:14:33.079Z`", "TIME WITH TIME ZONE", "$TIME_ANNOTATION::{hour:12, minute:14, second:33.079, timezone_hour:0, timezone_minute:0}", CastQuality.LOSSY), createZoneOffset()),
                Pair(case("`2007-02-23T12:14:33.079Z`", "TIME WITH TIME ZONE", "$TIME_ANNOTATION::{hour:12, minute:14, second:33.079, timezone_hour:0, timezone_minute:0}", CastQuality.LOSSY), createZoneOffset(11)),
                Pair(case("`2007-02-23T12:14:33.079-05:30`", "TIME WITH TIME ZONE", "$TIME_ANNOTATION::{hour:12, minute:14, second:33.079, timezone_hour:-5, timezone_minute:-30}", CastQuality.LOSSY), createZoneOffset(1)),
                Pair(case("`2007-02-23T12:14:33.079Z`", "TIME (2) WITH TIME ZONE", "$TIME_ANNOTATION::{hour:12, minute:14, second:33.08, timezone_hour:0, timezone_minute:0}", CastQuality.LOSSY), createZoneOffset(-5, -30)),
                // CAST(<text> AS <TIME WITH TIME ZONE>)
                Pair(case("'23:12:12.1267'", "TIME WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.1267, timezone_hour:0, timezone_minute:0}", CastQuality.LOSSY), createZoneOffset()),
                Pair(case("'23:12:12.1267'", "TIME WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.1267, timezone_hour:11, timezone_minute:0}", CastQuality.LOSSY), createZoneOffset(11)),
                Pair(case("'23:12:12.1267'", "TIME (2) WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.13, timezone_hour:-5, timezone_minute:-30}", CastQuality.LOSSY), createZoneOffset(-5, -30)),
                Pair(case("""`'23:12:12.1267'`""", "TIME WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.1267, timezone_hour:0, timezone_minute:0}", CastQuality.LOSSY), createZoneOffset()),
                Pair(case("""`'23:12:12.1267'`""", "TIME WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.1267, timezone_hour:11, timezone_minute:0}", CastQuality.LOSSY), createZoneOffset(11)),
                Pair(case("""`'23:12:12.1267'`""", "TIME (2) WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.13, timezone_hour:-5, timezone_minute:-30}", CastQuality.LOSSY), createZoneOffset(-5, -30)),
                Pair(case("""`"23:12:12.1267"`""", "TIME WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.1267, timezone_hour:0, timezone_minute:0}", CastQuality.LOSSY), createZoneOffset()),
                Pair(case("""`"23:12:12.1267"`""", "TIME WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.1267, timezone_hour:11, timezone_minute:0}", CastQuality.LOSSY), createZoneOffset(11)),
                Pair(case("""`"23:12:12.1267"`""", "TIME (2) WITH TIME ZONE", "$TIME_ANNOTATION::{hour:23, minute:12, second:12.13, timezone_hour:-5, timezone_minute:-30}", CastQuality.LOSSY), createZoneOffset(-5, -30))
            )

        private val castConfiguredTestCases = castPermissiveConfiguredTestCases + castLegacyConfiguredTestCases

        private val castToAnyConfiguredTestCases =
            castConfiguredTestCases.filter { case ->
                // ignore the static failure cases for ANY casting
                case.castCase.expectedErrorCode?.category != ErrorCategory.SEMANTIC
            }.map { case ->
                // translate all of our CAST cases into the identity cast to ANY
                val castCase = case.castCase
                val identityValue = eval(castCase.source)
                val newCastCase = castCase.copy(
                    type = "ANY",
                    expected = identityValue.ionValue.cloneAndRemoveBagAndMissingAnnotations().toString(),
                    expectedErrorCode = null
                ) {
                    assertEquals(identityValue.type, it.type)
                    assertEquals(0, DEFAULT_COMPARATOR.compare(identityValue, it))
                }

                case.copy(
                    castCase = newCastCase
                )
            }.distinctBy { case ->
                // dedupe by source and compilation option function
                Pair(case.castCase.source, case.compileOptionBlock)
            }

        private val canCastAndIsToAnyConfiguredTestCases = castToAnyConfiguredTestCases.map { case ->
            val newCastCase = when (case.castCase.expectedErrorCode) {
                null -> case.castCase.toCanCast()
                // error cases for ANY cast are still error cases
                else -> case.castCase.copy(
                    funcName = "CAN_CAST"
                )
            }
            case.copy(
                castCase = newCastCase
            )
        }.flatMap { case ->
            // IS/CAN_CAST behave identically for ANY
            listOf(
                case,
                case.copy(
                    castCase = case.castCase.copy(funcName = "IS")
                )
            )
        }

        private val canCastConfiguredTestCases = (
            legacyCastTestCases.flatMap { case ->
                typingModes.map { (typingModeName, typingModeConfig) ->
                    ConfiguredCastCase(case.toCanCast(), "LEGACY_CAST, $typingModeName") {
                        legacyCastBehavior()
                        typingModeConfig(this)
                    }
                }
            } + honorParamCastTestCases.flatMap { case ->
                typingModes.map { (typingModeName, typingModeConfig) ->
                    ConfiguredCastCase(case.toCanCast(), "HONOR_PARAM_CAST, $typingModeName") {
                        honorTypedOpParameters()
                        typingModeConfig(this)
                    }
                }
            }
            )

        private val canLosslessCastConfiguredTestCases = (
            legacyCastTestCases.flatMap { case ->
                typingModes.map { (typingModeName, typingModeConfig) ->
                    ConfiguredCastCase(case.toCanLosslessCast(), "LEGACY_CAST, $typingModeName") {
                        legacyCastBehavior()
                        typingModeConfig(this)
                    }
                }
            } + honorParamCastTestCases.flatMap { case ->
                typingModes.map { (typingModeName, typingModeConfig) ->
                    ConfiguredCastCase(case.toCanLosslessCast(), "HONOR_PARAM_CAST, $typingModeName") {
                        honorTypedOpParameters()
                        typingModeConfig(this)
                    }
                }
            }
            )

        internal val allConfiguredTestCases =
            castConfiguredTestCases +
                castToAnyConfiguredTestCases +
                canCastAndIsToAnyConfiguredTestCases +
                canCastConfiguredTestCases +
                canLosslessCastConfiguredTestCases

        private val configuredDateTimeTestCases = commonDateTimeTests.map { case ->
            ConfiguredCastCase(case, "LEGACY_ERROR_MODE") {
                legacyTypingMode()
            }
        } + commonDateTimeTests.map { case ->
            ConfiguredCastCase(case, "PERMISSIVE_TYPING_MODE") {
                permissiveTypingMode()
            }
        } + castDefaultTimezoneOffsetConfiguration.map { (case, configuredTimezoneOffset) ->
            ConfiguredCastCase(case, "Configuring default timezone offset") {
                defaultTimezoneOffset(configuredTimezoneOffset)
            }
        }

        private val canCastConfiguredDateTimeTestCases = commonDateTimeTests.map { case ->
            ConfiguredCastCase(case.toCanCast(), "LEGACY_ERROR_MODE") {
                legacyTypingMode()
            }
        } + commonDateTimeTests.map { case ->
            ConfiguredCastCase(case.toCanCast(), "PERMISSIVE_TYPING_MODE") {
                permissiveTypingMode()
            }
        } + castDefaultTimezoneOffsetConfiguration.map { (case, configuredTimezoneOffset) ->
            ConfiguredCastCase(case.toCanCast(), "Configuring default timezone offset") {
                defaultTimezoneOffset(configuredTimezoneOffset)
            }
        }

        private val canLosslessCastConfiguredDateTimeTestCases = commonDateTimeTests.map { case ->
            ConfiguredCastCase(case.toCanLosslessCast(), "LEGACY_ERROR_MODE") {
                legacyTypingMode()
            }
        } + commonDateTimeTests.map { case ->
            ConfiguredCastCase(case.toCanLosslessCast(), "PERMISSIVE_TYPING_MODE") {
                permissiveTypingMode()
            }
        } + castDefaultTimezoneOffsetConfiguration.map { (case, configuredTimezoneOffset) ->
            ConfiguredCastCase(case.toCanLosslessCast(), "Configuring default timezone offset") {
                defaultTimezoneOffset(configuredTimezoneOffset)
            }
        }

        internal val allConfiguredDateTimeTestCases =
            configuredDateTimeTestCases +
                canCastConfiguredDateTimeTestCases +
                canLosslessCastConfiguredDateTimeTestCases
    }
}
