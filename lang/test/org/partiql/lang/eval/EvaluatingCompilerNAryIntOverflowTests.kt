package org.partiql.lang.eval

import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.ast.toAstStatement
import org.partiql.lang.ast.toExprNode
import org.partiql.lang.eval.visitors.StaticTypeInferenceVisitorTransform
import org.partiql.lang.eval.visitors.StaticTypeVisitorTransform
import org.partiql.lang.eval.visitors.basicVisitorTransforms
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.types.IntType
import org.partiql.lang.types.StaticType
import org.partiql.lang.util.ArgumentsProviderBase

/**
 * This class tests the runtime behavior of integer operations when we have [StaticType] information
 * available.
 *
 * The [StaticTypeInferenceVisitorTransform] decorates each node with [StaticTypeMeta] containing [IntType], which
 * specifies the expected byte length of [NAry] arithmetic operations `+`, `-`, `*`, `/`, and `%`.
 * When this information is present, [EvaluatingCompiler] also inserts evaluation-time assertions that
 * the result of these operations fall within the range of values that can be represented by the given
 * integer size.
 *
 * This test class currently only tests [EvaluatingCompiler] in [TypingMod.PERMISSIVE].
 *
 * TODO:  extend this test class to include other [ErrorMode](s).
 */
class EvaluatingCompilerNAryIntOverflowTests : EvaluatorTestBase() {

    data class Variable(val name: String, val type: StaticType, val value: ExprValue)

    class Env(val globals: List<Variable>) {

        val typeBindings get() =
            object : Bindings<StaticType> {
                override fun get(bindingName: BindingName): StaticType? =
                    globals.firstOrNull { bindingName.isEquivalentTo(it.name) }?.type
                    // this is a unit test so we don't care, but we don't handle the case
                    // of multiple ambiguous matches here.
            }

        val valueBindings get() =
            object : Bindings<ExprValue> {
                override fun get(bindingName: BindingName): ExprValue? =
                    globals.firstOrNull { bindingName.isEquivalentTo(it.name) }?.value
                    // this is a unit test so we don't care, but we don't handle the case
                    // of multiple ambiguous matches here.
            }
    }

    private val sqlParser = SqlParser(ion)

    // TODO: test with [TypingMode.LEGACY] too.
    private val compOptions = CompileOptions.build {
        typingMode(TypingMode.PERMISSIVE)
        visitorTransformMode(VisitorTransformMode.NONE)
    }

    private val compiler = EvaluatingCompiler(
        valueFactory = valueFactory,
        functions = mapOf(),
        customTypeFunctions = mapOf(),
        compileOptions = compOptions,
        procedures = mapOf()
    )

    data class TestCase(
        val sqlUnderTest: String,
        val expectedPermissiveModeResult: String
        // TODO: expectedLegacyModeErrorCode, expectedErrorLine and expectedErrorColumn
    )

    private fun createVariablesForInt(
        prefix: String,
        type: StaticType,
        minValue: Long,
        maxValue: Long
    ) =
        listOf(
            Variable("${prefix}_1", type, valueFactory.newInt(1)),
            Variable("${prefix}_2", type, valueFactory.newInt(2)),
            Variable("${prefix}_neg1", type, valueFactory.newInt(-1)),
            Variable("${prefix}_neg2", type, valueFactory.newInt(-2)),
            Variable("${prefix}_max", type, valueFactory.newInt(maxValue)),
            Variable("${prefix}_min", type, valueFactory.newInt(minValue)),
            Variable("${prefix}_maxMinus1", type, valueFactory.newInt(maxValue - 1)),
            Variable("${prefix}_minPlus1", type, valueFactory.newInt(minValue + 1))
        )

    // TODO: need to include union types types other than strings...?
    private val defaultEnv = Env(
        listOf(
            createVariablesForInt(
                prefix = "int2",
                type = IntType(IntType.IntRangeConstraint.SHORT),
                minValue = Short.MIN_VALUE.toLong(),
                maxValue = Short.MAX_VALUE.toLong()),
            createVariablesForInt(
                prefix = "int4",
                type = IntType(IntType.IntRangeConstraint.INT4),
                minValue = Int.MIN_VALUE.toLong(),
                maxValue = Int.MAX_VALUE.toLong()
            ),
            createVariablesForInt(
                prefix = "int8",
                type = IntType(IntType.IntRangeConstraint.LONG),
                minValue = Long.MIN_VALUE,
                maxValue = Long.MAX_VALUE
            ),
            createVariablesForInt(
                prefix = "int",
                type = IntType(IntType.IntRangeConstraint.UNCONSTRAINED),
                minValue = Long.MIN_VALUE,
                maxValue = Long.MAX_VALUE
            ),
            // union type with multiple integer sizes--only the largest of them is considered.
            createVariablesForInt(
                prefix = "int2_4",
                type = StaticType.unionOf(
                    IntType(IntType.IntRangeConstraint.SHORT),
                    IntType(IntType.IntRangeConstraint.INT4)),
                minValue = Int.MIN_VALUE.toLong(),
                maxValue = Int.MAX_VALUE.toLong()
            ),
            createVariablesForInt(
                prefix = "int2_u",
                type = StaticType.unionOf(
                    IntType(IntType.IntRangeConstraint.INT4),
                    IntType(IntType.IntRangeConstraint.UNCONSTRAINED)),
                minValue = Long.MIN_VALUE,
                maxValue = Long.MAX_VALUE
            ),
            createVariablesForInt(
                prefix = "int4_8",
                type = StaticType.unionOf(StaticType.INT4, StaticType.INT8),
                minValue = Long.MIN_VALUE,
                maxValue = Long.MAX_VALUE
            ),
            createVariablesForInt(
                prefix = "int4_u",
                type = StaticType.unionOf(StaticType.INT4, StaticType.INT),
                minValue = Long.MIN_VALUE,
                maxValue = Long.MAX_VALUE
            ),
            listOf(
                // This variable has the type of `any_of(int2, string) and has a value that is a string
                Variable(
                    name = "int2_or_string_string",
                    type = StaticType.unionOf(StaticType.INT2, StaticType.STRING),
                    value = valueFactory.newString("foo")),
                // This variable has the type of `any_of(int2, string) and has a value that is a integer
                Variable(
                    name = "int2_or_string_int",
                    type = StaticType.unionOf(StaticType.INT2, StaticType.STRING),
                    value = valueFactory.newInt(1))
            )
        ).flatten())

    @ParameterizedTest
    @ArgumentsSource(IntOverflowTestCases::class)
    fun intOverflowTests(tc: TestCase) {
        // TODO:  today we only test permissive error mode mode, but we also need to consider this behavior in legacy
        // error mode as well.

        val ast = sqlParser.parseExprNode(tc.sqlUnderTest).toAstStatement()

        val transformedAst = basicVisitorTransforms().transformStatement(ast).let {
            StaticTypeVisitorTransform(
                ion = ion,
                globalBindings = defaultEnv.typeBindings,
                constraints = emptySet()).transformStatement(it)
        }.let { exprNode ->
            // [StaticTypeInferenceVisitorTransform] currently requires that [StaticTypeVisitorTransform] is run first.
            StaticTypeInferenceVisitorTransform(
                globalBindings = defaultEnv.typeBindings,
                customFunctionSignatures = emptyList(),
                customTypeFunctions = mapOf()).transformStatement(exprNode).toExprNode(ion)
        }

        val expression = compiler.compile(transformedAst)
        val session = EvaluationSession.build {
            globals(defaultEnv.valueBindings)
        }
        val result = expression.eval(session)

        val expectedResult = assertDoesNotThrow("The expected result should evaluate correctly") {
            eval(tc.expectedPermissiveModeResult, session = session)
        }

        assertExprEquals(expectedResult, result, "The expected and actual results must match")
    }
    class IntOverflowTestCases : ArgumentsProviderBase() {
        override fun getParameters(): List<Any> {
            /*
             * For each of the integer types (int2, int4, int8, and unconstrained ints) we generate test cases for:
             *
             * expression                   purpose
             * ------------------------------------------------------------------------------------------
             * max + 1 = missing            (integer overflow)
             * min - 1 = missing            (integer underflow)
             * max_minus_one + 1 = max      (does not overflow)
             * min_plus_one - 1 = min       (does not underflow)
             * 1 + 2 = 3                    (operands with known values do not result in missing or null)
             * -1 + -2 = -3                 (operands with known values do not result in missing or null)
             * 1 - 2 = -1                   (operands with known values do not result in missing or null)
             * -1 - -2 = 3                  (operands with known values do not result in missing or null)
             *
             * where:
             *    max is the maximum value that can be represented for the given bit-length, e.g. 32767 for INT2
             *    min is the minimum value that can be represented for the given bit-length, e.g. -32768 for INT2
             */
            fun createTestCases(prefix: String) =
                listOf(
                    // Note: / and % cannot overflow because the result cannot exceed
                    // the bit length of the operands.

                    // Plus and minus
                    TestCase("${prefix}_max + ${prefix}_1", "MISSING"),
                    TestCase("${prefix}_min - ${prefix}_1", "MISSING"),
                    TestCase("${prefix}_maxMinus1 + ${prefix}_1", "${prefix}_max"),
                    TestCase("${prefix}_minPlus1 - ${prefix}_1", "${prefix}_min"),
                    TestCase("${prefix}_1 + ${prefix}_2", "3"),
                    TestCase("${prefix}_neg1 + ${prefix}_neg2", "-3"),
                    TestCase("${prefix}_1 - ${prefix}_2", "-1"),
                    TestCase("${prefix}_neg1 - ${prefix}_neg2", "1"),

                    // Times 1, -1
                    TestCase("${prefix}_max * ${prefix}_2", "MISSING"),
                    TestCase("${prefix}_min * ${prefix}_neg2", "MISSING"),
                    TestCase("${prefix}_min * ${prefix}_2", "MISSING"),
                    TestCase("${prefix}_max * ${prefix}_neg2", "MISSING"),

                    // Times 2, -2
                    TestCase("${prefix}_max * ${prefix}_1", "${prefix}_max"),
                    TestCase("${prefix}_min * ${prefix}_neg1", "MISSING"),
                    TestCase("${prefix}_min * ${prefix}_1", "${prefix}_min"),
                    TestCase("${prefix}_max * ${prefix}_neg1", "${prefix}_minPlus1"),

                    // Unary negation
                    TestCase("-${prefix}_max", "${prefix}_minPlus1"),
                    // https://github.com/partiql/partiql-lang-kotlin/issues/513
                    //TestCase("-${prefix}_min", "MISSING"),
                    TestCase("-${prefix}_1", "${prefix}_neg1"),
                    TestCase("-${prefix}_neg1", "${prefix}_1")
                )

            return listOf(
                createTestCases("int2"),
                createTestCases("int4"),
                createTestCases("int8"),
                createTestCases("int2_4"),
                createTestCases("int2_u"),
                createTestCases("int4_8"),
                createTestCases("int4_u"),

                // Test cases for when one of the operands is a union that doesn't include a numeric type.
                listOf(
                    TestCase("int2_max + int2_or_string_string", "MISSING"),
                    TestCase("int2_max - int2_or_string_string", "MISSING"),
                    TestCase("int2_max * int2_or_string_string", "MISSING"),

                    TestCase("int2_max + int2_or_string_int", "MISSING"),
                    TestCase("int2_max - int2_or_string_int", "int2_maxMinus1"),
                    TestCase("int2_max * int2_or_string_int", "int2_max")
                )
            ).flatten()
        }
    }

}

