package org.partiql.lang.eval

import org.partiql.lang.CompilerPipeline
import org.partiql.lang.errors.ErrorCategory
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.eval.ExprValueType.BAG
import org.partiql.lang.eval.ExprValueType.BLOB
import org.partiql.lang.eval.ExprValueType.BOOL
import org.partiql.lang.eval.ExprValueType.CLOB
import org.partiql.lang.eval.ExprValueType.DECIMAL
import org.partiql.lang.eval.ExprValueType.FLOAT
import org.partiql.lang.eval.ExprValueType.INT
import org.partiql.lang.eval.ExprValueType.LIST
import org.partiql.lang.eval.ExprValueType.MISSING
import org.partiql.lang.eval.ExprValueType.NULL
import org.partiql.lang.eval.ExprValueType.SEXP
import org.partiql.lang.eval.ExprValueType.STRUCT
import org.partiql.lang.eval.ExprValueType.TIMESTAMP
import org.partiql.lang.syntax.ParserException
import org.partiql.lang.util.*
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

abstract class CastTestBase : EvaluatorTestBase() {

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
    data class CastCase(val funcName: String,
                        val source: String,
                        val type: String,
                        val expected: String?,
                        val expectedErrorCode: ErrorCode?,
                        val quality: CastQualityStatus?,
                        val additionalAssertBlock: AssertExprValue.() -> Unit = { }) {
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
     * @param configurePipeline Additional configuration for the compiler pipeline.
     * @param compileOptionBlock The optional lambda with a receiver to a [CompileOptions.Builder] to
     *  configure it.
     */
    data class ConfiguredCastCase(val castCase: CastCase,
                                  val description: String = "",
                                  val configurePipeline: CompilerPipeline.Builder.() -> Unit = {},
                                  val compileOptionBlock: CompileOptions.Builder.() -> Unit = {}) {
        private val additionalDescription = when (description) {
            "" -> ""
            else -> " - $description"
        }
        override fun toString() = "$castCase$additionalDescription"

        fun assertCase() {
            when (castCase.expected) {
                null -> {
                    try {
                        voidEval(
                            castCase.expression,
                            compileOptions = CompileOptions.build(compileOptionBlock),
                            compilerPipelineBuilderBlock = configurePipeline
                        )
                        fail("Expected evaluation error")
                    } catch (e: EvaluationException) {
                        if (castCase.expectedErrorCode == null) {
                            fail("CastCase $castCase did not have an expected value or expected error code.")
                        }
                        assertEquals(castCase.expectedErrorCode, e.errorCode)
                    }
                }
                else -> assertEval(
                    castCase.expression,
                    castCase.expected,
                    compileOptions = CompileOptions.build(compileOptionBlock),
                    compilerPipelineBuilderBlock = configurePipeline,
                    block = castCase.additionalAssertBlock
                )
            }
        }

        // Separate tests for Date and Time as [assertEval] validates serialization and
        // date and time literals are not supported by V0 AST serializer.
        internal fun assertDateTimeCase() {
            when (castCase.expected) {
                null -> {
                    try {
                        voidEval(castCase.expression)
                        fail("Expected evaluation error")
                    } catch (e: EvaluationException) {
                        if (castCase.expectedErrorCode == null) {
                            fail("CastCase $castCase did not have an expected value or expected error code.")
                        }
                        assertEquals(castCase.expectedErrorCode, e.errorCode)
                    } catch (p: ParserException) {
                        if (castCase.expectedErrorCode == null) {
                            fail("CastCase $castCase did not have an expected value or expected error code.")
                        }
                        assertEquals(castCase.expectedErrorCode, p.errorCode)
                    }
                }
                else -> assertEquals(castCase.expected, eval(castCase.expression, compileOptions = CompileOptions.build(compileOptionBlock)).toString())
            }
        }
    }

    companion object : EvaluatorTestBase() {

        /** Partial application of the source expression and the expected Ion value without type. Assumes [Implemented] logic*/
        fun case(source: String,
                 expected: String?,
                 quality: CastQuality,
                 additionalAssertBlock: AssertExprValue.() -> Unit = { }): (String) -> CastCase = {
            CastCase("CAST", source, it, expected, null, Implemented(quality), additionalAssertBlock)
        }

        /** Partial application of the source expression and the expected Ion value without type. */
        fun case(source: String,
                 expected: String?,
                 qualityStatus: CastQualityStatus,
                 additionalAssertBlock: AssertExprValue.() -> Unit = { }): (String) -> CastCase = {
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

        val allTypeNames = ExprValueType.values().flatMap { it.sqlTextNames }

        val commonTestCases =
            listOf(
                listOf(
                    case("NULL", "null", CastQuality.LOSSLESS) {
                        assertEquals(ExprValueType.NULL, exprValue.type)
                    }
                ).types(allTypeNames - "MISSING"),
                listOf(
                    case("NULL", "null", CastQuality.LOSSLESS) {
                        assertEquals(ExprValueType.MISSING, exprValue.type)
                    }
                ).types(listOf("MISSING")),
                listOf(
                    case("MISSING", "null", CastQuality.LOSSLESS) {
                        assertEquals(ExprValueType.MISSING, exprValue.type)
                    }
                ).types(allTypeNames - "NULL"),
                listOf(
                    case("MISSING", "null", CastQuality.LOSSLESS) {
                        assertEquals(ExprValueType.NULL, exprValue.type)
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
                ).types(ExprValueType.BOOL.sqlTextNames),
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
                ).types(ExprValueType.INT.sqlTextNames),
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
                ).types(ExprValueType.FLOAT.sqlTextNames),
                listOf(
                    // booleans
                    case("TRUE AND FALSE", "0d0", CastQuality.LOSSLESS),
                    case("`true`", "1d0", CastQuality.LOSSLESS),
                    // numbers
                    case("5", "5d0", CastQuality.LOSSLESS),
                    case("5 ", "5d0", CastQuality.LOSSLESS),
                    case("`0e0`", "0.", CastQuality.LOSSLESS),  // TODO formalize this behavior
                    case("`1e0`", "1.", CastQuality.LOSSLESS),  // TODO formalize this behavior
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
                ).types(ExprValueType.DECIMAL.sqlTextNames),
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
                    case("`2007-10-10T`", "\$partiql_date::2007-10-10", NotImplemented(CastQuality.LOSSLESS)),
                    case("`2007-02-23T12:14Z`", "\$partiql_date::2007-02-23", CastQuality.LOSSY),
                    case("`2007-02-23T12:14:33.079Z`", "\$partiql_date::2007-02-23", CastQuality.LOSSY),
                    case("`2007-02-23T12:14:33.079-08:00`", "\$partiql_date::2007-02-23", CastQuality.LOSSY),
                    case("`2007-02T`", "\$partiql_date::2007-02-01", NotImplemented(CastQuality.LOSSLESS)),
                    case("`2007T`", "\$partiql_date::2007-01-01", NotImplemented(CastQuality.LOSSLESS)),
                    // text
                    case("'hello'", ErrorCode.EVALUATOR_CAST_FAILED),
                    case("'2016-03-01T01:12:12Z'", ErrorCode.EVALUATOR_CAST_FAILED),
                    case("""`"2001-01-01"`""", "\$partiql_date::2001-01-01", CastQuality.LOSSLESS),
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
                ).types(ExprValueType.DATE.sqlTextNames),
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
                    case("`2007-10-10T`", "\$partiql_time::{hour:0,minute:0,second:0.,timezone_hour:null.int,timezone_minute:null.int}", CastQuality.LOSSY),
                    case("`2007-02-23T12:14Z`", "\$partiql_time::{hour:12,minute:14,second:0.,timezone_hour:null.int,timezone_minute:null.int}", CastQuality.LOSSY),
                    case("`2007-02-23T12:14:33.079Z`", "\$partiql_time::{hour:12,minute:14,second:33.079,timezone_hour:null.int,timezone_minute:null.int}", CastQuality.LOSSY),
                    case("`2007-02-23T12:14:33.079-08:00`", "\$partiql_time::{hour:12,minute:14,second:33.079,timezone_hour:null.int,timezone_minute:null.int}", CastQuality.LOSSY),
                    case("`2007-02T`", "\$partiql_time::{hour:0,minute:0,second:0.,timezone_hour:null.int,timezone_minute:null.int}", CastQuality.LOSSY),
                    case("`2007T`", "\$partiql_time::{hour:0,minute:0,second:0.,timezone_hour:null.int,timezone_minute:null.int}", CastQuality.LOSSY),
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
                ).types(ExprValueType.TIME.sqlTextNames),
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
                ).types(ExprValueType.TIMESTAMP.sqlTextNames),
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
                    case("`{{MA==}}`", ErrorCode.EVALUATOR_INVALID_CAST),  // 0
                    case("`{{MS4w}}`", ErrorCode.EVALUATOR_INVALID_CAST),  // 1.0
                    case("`{{MmUxMA==}}`", ErrorCode.EVALUATOR_INVALID_CAST),  // 2e10
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
                ).types(ExprValueType.SYMBOL.sqlTextNames),
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
                    case("`{{MA==}}`", ErrorCode.EVALUATOR_INVALID_CAST),  // 0
                    case("`{{MS4w}}`", ErrorCode.EVALUATOR_INVALID_CAST),  // 1.0
                    case("`{{MmUxMA==}}`", ErrorCode.EVALUATOR_INVALID_CAST),  // 2e10
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
                ).types(ExprValueType.CLOB.sqlTextNames),
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
                    case("`{{MA==}}`", """{{MA==}}""", CastQuality.LOSSLESS),     // 0
                    case("`{{MS4w}}`", """{{MS4w}}""", CastQuality.LOSSLESS),     // 1.0
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
                ).types(ExprValueType.BLOB.sqlTextNames),
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
                    case("<<>>", "[]", CastQuality.LOSSLESS),      // TODO bag verification
                    case("<<`14d0`>>", "[14d0]", CastQuality.LOSSLESS),  // TODO bag verification
                    case("<<`20d0`>>", "[20d0]", CastQuality.LOSSLESS)   // TODO bag verification
                ).types(ExprValueType.LIST.sqlTextNames),
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
                ).types(ExprValueType.SEXP.sqlTextNames),
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
                ).types(ExprValueType.STRUCT.sqlTextNames),
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
                    case("`[]`", "[]", CastQuality.LOSSLESS),          // TODO bag verification
                    case("['hello']", "[\"hello\"]", CastQuality.LOSSLESS), // TODO bag verification
                    case("`[-2d0, 0d0]`", "[-2d0, 0d0]", CastQuality.LOSSLESS), // TODO bag verification
                    // sexp
                    case("`()`", "[]", CastQuality.LOSSLESS),          // TODO bag verification
                    case("`(1d0)`", "[1d0]", CastQuality.LOSSLESS),       // TODO bag verification
                    case("`(0d0)`", "[0d0]", CastQuality.LOSSLESS),       // TODO bag verification
                    // struct
                    case("`{}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("{}", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("`{a:12d0}`", ErrorCode.EVALUATOR_INVALID_CAST),
                    case("{'b':`-4d0`}", ErrorCode.EVALUATOR_INVALID_CAST),
                    // bag
                    case("<<>>", "[]", CastQuality.LOSSLESS),          // TODO bag verification
                    case("<<`14d0`>>", "[14d0]", CastQuality.LOSSLESS),      // TODO bag verification
                    case("<<`20d0`>>", "[20d0]", CastQuality.LOSSLESS)       // TODO bag verification
                ).types(ExprValueType.BAG.sqlTextNames)
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
                case("`{{MA==}}`", ErrorCode.EVALUATOR_INVALID_CAST),  // 0
                case("`{{MS4w}}`", ErrorCode.EVALUATOR_INVALID_CAST),  // 1.0
                case("`{{MmUxMA==}}`", ErrorCode.EVALUATOR_INVALID_CAST),  // 2e10
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
            ).types(ExprValueType.INT.sqlTextNames),
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
            ).types(ExprValueType.DECIMAL.sqlTextNames.map {"${it}(3)"}),
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
            ).types(ExprValueType.DECIMAL.sqlTextNames.map {"${it}(5, 2)"}),
            // DECIMAL(4,4) ; LEGACY mode does not respect DECIMAL's precison or scale; precision = scale is valid here
            listOf(
                case("0.1", "1d-1", CastQuality.LOSSLESS),
                case("0.1234", "0.1234", CastQuality.LOSSLESS),
                case("0.12345", "0.12345", CastQuality.LOSSLESS)
            ).types(ExprValueType.DECIMAL.sqlTextNames.map { "${it}(4,4)" }),
            // DECIMAL(2, 4) ; LEGACY mode does not respect DECIMAL's precison or scale; precision < scale is valid in legacy mode
            listOf(
                case("1", "1d0", CastQuality.LOSSLESS)
            ).types(ExprValueType.DECIMAL.sqlTextNames.map { "${it}(2,4)" }),
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
                case("`{{MA==}}`", ErrorCode.EVALUATOR_INVALID_CAST),  // 0
                case("`{{MS4w}}`", ErrorCode.EVALUATOR_INVALID_CAST),  // 1.0
                case("`{{MmUxMA==}}`", ErrorCode.EVALUATOR_INVALID_CAST),  // 2e10
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
            ).types(ExprValueType.INT.sqlTextNames),
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
            ).types(ExprValueType.DECIMAL.sqlTextNames.map {"${it}(3)"}),
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
            ).types(ExprValueType.DECIMAL.sqlTextNames.map {"${it}(5, 2)"}),
            // DECIMAL(4,4) precision = scale is valid in honor_params
            listOf(
                case("0.1", "1.000d-1", CastQuality.LOSSLESS),
                case("0.1234", "0.1234", CastQuality.LOSSLESS),
                case("0.12345", "0.1235", CastQuality.LOSSY)
            ).types(ExprValueType.DECIMAL.sqlTextNames.map { "${it}(4,4)" }),
            // DECIMAL(2, 4) is a compilation failure in this mode
            listOf(
                case("1", ErrorCode.SEMANTIC_INVALID_DECIMAL_ARGUMENTS)
            ).types(ExprValueType.DECIMAL.sqlTextNames.map { "${it}(2,4)" }),
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

        private val defaultTimezoneOffset = ZoneOffset.UTC

        private fun createZoneOffset(hours: Int = 0, minutes: Int = 0) = ZoneOffset.ofHoursMinutes(hours, minutes)

        private val commonDateTimeTests = listOf(
            listOf(
                case("DATE '2007-10-10'", "2007-10-10", CastQuality.LOSSLESS)
            ).types(ExprValueType.DATE.sqlTextNames),
            listOf(
                case("DATE '2007-10-10'", "`'2007-10-10'`", CastQuality.LOSSLESS)
            ).types(ExprValueType.SYMBOL.sqlTextNames),
            listOf(
                case("DATE '2007-10-10'", "'2007-10-10'", CastQuality.LOSSLESS)
            ).types(ExprValueType.STRING.sqlTextNames),
            listOf(
                // CAST(<TIME> AS <variants of TIME type>)
                case("TIME '23:12:12.1267'", "TIME", "23:12:12.1267", CastQuality.LOSSLESS),
                case("TIME '23:12:12.1267-05:30'", "TIME WITH TIME ZONE", "23:12:12.1267${defaultTimezoneOffset.getOffsetHHmm()}", CastQuality.LOSSLESS),
                case("TIME '23:12:12.1267+05:30'", "TIME (3)", "23:12:12.127", CastQuality.LOSSY),
                case("TIME '23:12:12.1267-05:30'", "TIME (3) WITH TIME ZONE", "23:12:12.127${defaultTimezoneOffset.getOffsetHHmm()}", CastQuality.LOSSY),
                case("TIME (3) '23:12:12.1267'", "TIME","23:12:12.127", CastQuality.LOSSLESS),
                case("TIME (3) '23:12:12.1267-05:30'", "TIME","23:12:12.127", CastQuality.LOSSLESS),
                case("TIME (3) '23:12:12.1267+05:30'", "TIME WITH TIME ZONE","23:12:12.127${defaultTimezoneOffset.getOffsetHHmm()}", CastQuality.LOSSLESS),
                case("TIME (3) '23:12:12.1267-05:30'", "TIME (9)","23:12:12.127000000", CastQuality.LOSSLESS),
                case("TIME WITH TIME ZONE '23:12:12.1267'", "TIME", "23:12:12.1267", CastQuality.LOSSLESS),
                case("TIME WITH TIME ZONE '23:12:12.1267-05:30'", "TIME WITH TIME ZONE", "23:12:12.1267-05:30", CastQuality.LOSSLESS),
                case("TIME WITH TIME ZONE '23:12:12.1267+05:30'", "TIME (3) WITH TIME ZONE","23:12:12.127+05:30", CastQuality.LOSSY),
                case("TIME WITH TIME ZONE '23:12:12.1267-05:30'", "TIME", "23:12:12.1267", CastQuality.LOSSY),
                case("TIME (3) WITH TIME ZONE '23:12:12.1267'", "TIME", "23:12:12.127", CastQuality.LOSSLESS),
                case("TIME (3) WITH TIME ZONE '23:12:12.1267-05:30'", "TIME WITH TIME ZONE", "23:12:12.127-05:30", CastQuality.LOSSLESS),
                case("TIME (3) WITH TIME ZONE '23:12:12.1267+05:30'", "TIME (5)", "23:12:12.12700", CastQuality.LOSSY),
                case("TIME (3) WITH TIME ZONE '23:12:12.1267-05:30'", "TIME (5) WITH TIME ZONE","23:12:12.12700-05:30", CastQuality.LOSSLESS),
                // CAST(<TIMESTAMP> AS <variants of TIME type>)
                case("`2007-02-23T12:14:33.079Z`", "TIME", "12:14:33.079", CastQuality.LOSSY),
                case("`2007-02-23T12:14:33.079-08:00`", "TIME", "12:14:33.079", CastQuality.LOSSY),
                case("`2007-02-23T12:14:33.079Z`", "TIME WITH TIME ZONE", "12:14:33.079+00:00", CastQuality.LOSSY),
                case("`2007-02-23T12:14:33.079-08:00`", "TIME (1)", "12:14:33.1", CastQuality.LOSSY),
                case("`2007-02-23T12:14:33.079-08:00`", "TIME (2) WITH TIME ZONE", "12:14:33.08-08:00", CastQuality.LOSSY),
                // CAST(<text> AS <variants of TIME type>)
                case("'23:12:12.1267'", "TIME", "23:12:12.1267", CastQuality.LOSSLESS),
                case("'23:12:12.1267'", "TIME (2)", "23:12:12.13", CastQuality.LOSSY),
                case("'23:12:12.1267'", "TIME WITH TIME ZONE", "23:12:12.1267${defaultTimezoneOffset.getOffsetHHmm()}", CastQuality.LOSSY),
                case("'23:12:12.1267'", "TIME (2) WITH TIME ZONE", "23:12:12.13${defaultTimezoneOffset.getOffsetHHmm()}", CastQuality.LOSSY),
                case("""`"23:12:12.1267"`""", "TIME", "23:12:12.1267", CastQuality.LOSSLESS),
                case("""`"23:12:12.1267"`""", "TIME (2)", "23:12:12.13", CastQuality.LOSSY),
                case("""`"23:12:12.1267"`""", "TIME WITH TIME ZONE", "23:12:12.1267${defaultTimezoneOffset.getOffsetHHmm()}", CastQuality.LOSSY),
                case("""`'23:12:12.1267'`""", "TIME (2) WITH TIME ZONE", "23:12:12.13${defaultTimezoneOffset.getOffsetHHmm()}", CastQuality.LOSSY),
                case("""`'23:12:12.1267'`""", "TIME", "23:12:12.1267", CastQuality.LOSSLESS),
                case("""`'23:12:12.1267'`""", "TIME (2)", "23:12:12.13", CastQuality.LOSSY),
                case("""`'23:12:12.1267'`""", "TIME WITH TIME ZONE", "23:12:12.1267${defaultTimezoneOffset.getOffsetHHmm()}", CastQuality.LOSSY),
                case("""`'23:12:12.1267'`""", "TIME (2) WITH TIME ZONE", "23:12:12.13${defaultTimezoneOffset.getOffsetHHmm()}", CastQuality.LOSSY),
            ),
            // Error cases for TIME
            listOf(
                case("TIME '23:12:12.1267'", "TIME (-1)", ErrorCode.PARSE_INVALID_PRECISION_FOR_TIME),
                case("TIME '23:12:12.1267'", "TIME (1, 2)", ErrorCode.PARSE_CAST_ARITY),
                case("TIME '23:12:12.1267'", "TIME (1, 2) WITH TIME ZONE", ErrorCode.PARSE_CAST_ARITY),
                case("TIME '23:12:12.1267-05:30'", "TIME (10) WITH TIME ZONE", ErrorCode.PARSE_INVALID_PRECISION_FOR_TIME),
                case("TIME '23:12:12.1267+05:30'", "TIME (10)", ErrorCode.PARSE_INVALID_PRECISION_FOR_TIME),
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
                case("TIME '23:12:12.1267'", "`'23:12:12.1267'`", CastQuality.LOSSLESS),
                case("TIME '23:12:12.1267-05:30'", "`'23:12:12.1267'`", CastQuality.LOSSLESS),
                case("TIME '23:12:12.1267+05:30'", "`'23:12:12.1267'`", CastQuality.LOSSLESS),
                case("TIME (3) '23:12:12.1267'", "`'23:12:12.127'`", CastQuality.LOSSLESS),
                case("TIME (3) '23:12:12.1267-05:30'", "`'23:12:12.127'`", CastQuality.LOSSLESS),
                case("TIME (3) '23:12:12.1267+05:30'", "`'23:12:12.127'`", CastQuality.LOSSLESS),
                case("TIME WITH TIME ZONE '23:12:12.1267'", "`'23:12:12.1267${defaultTimezoneOffset.getOffsetHHmm()}'`", CastQuality.LOSSLESS),
                case("TIME WITH TIME ZONE '23:12:12.1267-05:30'", "`'23:12:12.1267-05:30'`", CastQuality.LOSSLESS),
                case("TIME WITH TIME ZONE '23:12:12.1267+05:30'", "`'23:12:12.1267+05:30'`", CastQuality.LOSSLESS),
                case("TIME (3) WITH TIME ZONE '23:12:12.1267'", "`'23:12:12.127${defaultTimezoneOffset.getOffsetHHmm()}'`", CastQuality.LOSSLESS),
                case("TIME (3) WITH TIME ZONE '23:12:12.1267-05:30'", "`'23:12:12.127-05:30'`", CastQuality.LOSSLESS),
                case("TIME (3) WITH TIME ZONE '23:12:12.1267+05:30'", "`'23:12:12.127+05:30'`", CastQuality.LOSSLESS)
            ).types(ExprValueType.SYMBOL.sqlTextNames),
            // CAST <TIME> AS STRING
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
            ).types(ExprValueType.STRING.sqlTextNames)
        ).flatten() +
            listOf(MISSING, NULL, BOOL, INT, FLOAT, DECIMAL, TIMESTAMP, CLOB, BLOB, LIST, SEXP, STRUCT, BAG)
                .map { listOf(case("DATE '2007-10-10'", ErrorCode.EVALUATOR_INVALID_CAST)).types(it.sqlTextNames)
            }.flatten()

        private val typingModes: Map<String, (CompileOptions.Builder) -> Unit> = mapOf(
            "LEGACY_TYPING_MODE" to { cob -> cob.legacyTypingMode() },
            "PERMISSIVE_TYPING_MODE" to { cob -> cob.permissiveTypingMode() }
        )

        val castBehaviors: Map<String, (CompileOptions.Builder) -> Unit> = mapOf(
            "LEGACY_CAST" to { cob -> cob.legacyCastBehavior() } ,
            "HONOR_PARAM_CAST" to { cob -> cob.honorTypedOpParameters() }
        )

        private val legacyCastTestCases = (commonTestCases + deviatingLegacyTestCases)
        private val honorParamCastTestCases = (commonTestCases + deviatingParamsTestCases)

        fun List<CastCase>.toPermissive(): List<CastCase> = map { case ->
            when {
                // Note that we do not convert cases where the error is semantic (i.e. static)
                case.expectedErrorCode != null && case.expectedErrorCode.category != ErrorCategory.SEMANTIC -> {
                    // rewrite error code cases to `MISSING` for permissive mode
                    case.copy(expected = "null", expectedErrorCode = null) {
                        assertEquals(ExprValueType.MISSING, exprValue.type)
                    }
                }
                else -> case
            }
        }

        private val castPermissiveConfiguredTestCases = (legacyCastTestCases.toPermissive().map { case ->
            ConfiguredCastCase(case, "LEGACY_CAST, PERMISSIVE_TYPING_MODE") {
                legacyCastBehavior()
                permissiveTypingMode()
            }
        } + honorParamCastTestCases.toPermissive().map { case ->
            ConfiguredCastCase(case, "HONOR_PARAM_CAST, PERMISSIVE_TYPING_MODE") {
                honorTypedOpParameters()
                permissiveTypingMode()
            }
        })

        private val castLegacyConfiguredTestCases = (legacyCastTestCases.map { case ->
            ConfiguredCastCase(case, "LEGACY_CAST, LEGACY_ERROR_MODE") {
                legacyCastBehavior()
                legacyTypingMode()
            }
        } + honorParamCastTestCases.map { case ->
            ConfiguredCastCase(case, "HONOR_PARAM_CAST, LEGACY_ERROR_MODE") {
                honorTypedOpParameters()
                legacyTypingMode()
            }
        })

        private val castDefaultTimezoneOffsetConfiguration =
            // Configuring default timezone offset through CompileOptions
            listOf(
                // CAST(<TIME> AS <TIME WITH TIME ZONE>)
                Pair(case("TIME '23:12:12.1267'", "TIME WITH TIME ZONE", "23:12:12.1267+00:00", CastQuality.LOSSLESS), createZoneOffset()),
                Pair(case("TIME '23:12:12.1267'", "TIME WITH TIME ZONE", "23:12:12.1267+11:00", CastQuality.LOSSLESS), createZoneOffset(11)),
                Pair(case("TIME '23:12:12.1267-05:30'", "TIME WITH TIME ZONE", "23:12:12.1267+01:00", CastQuality.LOSSLESS), createZoneOffset(1)),
                Pair(case("TIME '23:12:12.1267-05:30'", "TIME (2) WITH TIME ZONE", "23:12:12.13-05:30", CastQuality.LOSSY), createZoneOffset(-5, -30)),
                // CAST(<TIMESTAMP> AS <TIME WITH TIME ZONE>)
                Pair(case("`2007-02-23T12:14:33.079Z`", "TIME WITH TIME ZONE", "12:14:33.079+00:00", CastQuality.LOSSY), createZoneOffset()),
                Pair(case("`2007-02-23T12:14:33.079Z`", "TIME WITH TIME ZONE", "12:14:33.079+00:00", CastQuality.LOSSY), createZoneOffset(11)),
                Pair(case("`2007-02-23T12:14:33.079-05:30`", "TIME WITH TIME ZONE", "12:14:33.079-05:30", CastQuality.LOSSY), createZoneOffset(1)),
                Pair(case("`2007-02-23T12:14:33.079Z`", "TIME (2) WITH TIME ZONE", "12:14:33.08+00:00", CastQuality.LOSSY), createZoneOffset(-5, -30)),
                // CAST(<text> AS <TIME WITH TIME ZONE>)
                Pair(case("'23:12:12.1267'", "TIME WITH TIME ZONE", "23:12:12.1267+00:00", CastQuality.LOSSY), createZoneOffset()),
                Pair(case("'23:12:12.1267'", "TIME WITH TIME ZONE", "23:12:12.1267+11:00", CastQuality.LOSSY), createZoneOffset(11)),
                Pair(case("'23:12:12.1267'", "TIME (2) WITH TIME ZONE", "23:12:12.13-05:30", CastQuality.LOSSY), createZoneOffset(-5, -30)),
                Pair(case("""`'23:12:12.1267'`""", "TIME WITH TIME ZONE", "23:12:12.1267+00:00", CastQuality.LOSSY), createZoneOffset()),
                Pair(case("""`'23:12:12.1267'`""", "TIME WITH TIME ZONE", "23:12:12.1267+11:00", CastQuality.LOSSY), createZoneOffset(11)),
                Pair(case("""`'23:12:12.1267'`""", "TIME (2) WITH TIME ZONE", "23:12:12.13-05:30", CastQuality.LOSSY), createZoneOffset(-5, -30)),
                Pair(case("""`"23:12:12.1267"`""", "TIME WITH TIME ZONE", "23:12:12.1267+00:00", CastQuality.LOSSY), createZoneOffset()),
                Pair(case("""`"23:12:12.1267"`""", "TIME WITH TIME ZONE", "23:12:12.1267+11:00", CastQuality.LOSSY), createZoneOffset(11)),
                Pair(case("""`"23:12:12.1267"`""", "TIME (2) WITH TIME ZONE", "23:12:12.13-05:30", CastQuality.LOSSY), createZoneOffset(-5, -30))
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
                    expected = identityValue.ionValue.cloneAndRemoveAnnotations().toString(),
                    expectedErrorCode = null
                ) {
                    assertEquals(identityValue.type, exprValue.type)
                    assertEquals(0, DEFAULT_COMPARATOR.compare(identityValue, exprValue))
                }

                case.copy(
                    castCase = newCastCase
                )
            }.distinctBy {case ->
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

        private val canCastConfiguredTestCases = (legacyCastTestCases.flatMap { case ->
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
        })

        private val canLosslessCastConfiguredTestCases = (legacyCastTestCases.flatMap { case ->
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
        })

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