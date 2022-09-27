package org.partiql.lang.eval

import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.lang.ION
import org.partiql.lang.ast.StaticTypeMeta
import org.partiql.lang.domains.metaContainerOf
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.types.StaticScalarType
import org.partiql.lang.types.StaticType
import ots.legacy.types.CharType
import kotlin.test.assertEquals

/**
 * This class is currently limited to testing behavior of both [ThunkFactory] implementations with regard to
 * [ThunkReturnTypeAssertions.ENABLED] but could be expanded to test other aspects as well.  Right now we assume
 * (reasonably) that this class is getting tested as part of all the other evaluation tests.
 */
class ThunkFactoryTests {

    companion object {
        private val compileOptions = ThunkOptions.build {
            evaluationTimeTypeChecks(ThunkReturnTypeAssertions.ENABLED)
        }

        private val valueFactory = ExprValueFactory.standard(ION)

        private val STRING_SHORT = valueFactory.newString("Hello, I'm a string")
        private val STRING_LONG = valueFactory.newString("Hello, I'm a loooooooooooooooooooooooooooooooooooong string!")
        private val INT_42 = valueFactory.newInt(42)
        private val INT_42000 = valueFactory.newInt(42000)
        private val IRRELEVANT = valueFactory.newString("doesn't matter")
        private val IRRELEVANT_METAS = metaContainerOf(StaticTypeMeta(StaticType.STRING))
        private val EXPECT_BOOL_METAS = metaContainerOf(StaticTypeMeta(StaticType.BOOL))

        internal data class TestCase(
            val expectedType: StaticType,
            val thunkReturnValue: ExprValue,
            val expectError: Boolean,
            internal val thunkFactory: ThunkFactory<Environment>
        ) {
            val metas = metaContainerOf(StaticTypeMeta(expectedType))
            val fakeThunk = thunkFactory.thunkEnv(IRRELEVANT_METAS) { IRRELEVANT }
        }

        private fun createTestCases(
            expectedType: StaticType,
            thunkReturnValue: ExprValue,
            expectError: Boolean
        ) = listOf(
            TestCase(expectedType, thunkReturnValue, expectError, LegacyThunkFactory(compileOptions, valueFactory)),
            TestCase(expectedType, thunkReturnValue, expectError, PermissiveThunkFactory(compileOptions, valueFactory))
        )

        @JvmStatic
        @Suppress("UNUSED")
        fun parameters(): List<Any> = listOf(
            createTestCases(StaticType.INT, STRING_SHORT, true),
            createTestCases(StaticType.INT, STRING_LONG, true),
            createTestCases(StaticType.INT2, INT_42000, true),
            createTestCases(StaticType.INT4, INT_42000, false),
            createTestCases(StaticType.INT, INT_42, false),
            createTestCases(StaticType.INT, INT_42000, false),

            createTestCases(StaticType.STRING, INT_42000, true),
            createTestCases(
                StaticScalarType(CharType, listOf(20)),
                STRING_LONG,
                true
            )
        ).flatten()
    }

    @ParameterizedTest
    @MethodSource("parameters")
    internal fun thunkEnvUnexpectedReturnType(tc: TestCase) =
        assertInvoke(tc.expectError) {
            tc.thunkFactory.thunkEnv(tc.metas) {
                tc.thunkReturnValue
            }.invoke(Environment.standard())
        }

    @ParameterizedTest
    @MethodSource("parameters")
    internal fun thunkEnvValueUnexpectedReturnType(tc: TestCase) =
        assertInvoke(tc.expectError) {
            tc.thunkFactory.thunkEnvValue(tc.metas) { _, _ ->
                tc.thunkReturnValue
            }.invoke(Environment.standard(), valueFactory.newString("doesn't matter"))
        }

    @ParameterizedTest
    @MethodSource("parameters")
    internal fun thunkEnvValueListUnexpectedReturnType(tc: TestCase) {
        assertInvoke(tc.expectError) {
            tc.thunkFactory.thunkEnvValueList(tc.metas) { _, _ ->
                tc.thunkReturnValue
            }.invoke(Environment.standard(), listOf(IRRELEVANT))
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    internal fun thunkFoldUnexpectedReturnType(tc: TestCase) {
        assertInvoke(tc.expectError) {
            tc.thunkFactory.thunkFold(tc.metas, listOf(tc.fakeThunk, tc.fakeThunk)) { _, _ ->
                tc.thunkReturnValue
            }.invoke(Environment.standard())
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    internal fun thunkAndMapUnexpectedReturnType(tc: TestCase) {
        // NOTE:  we ignore tc.expectError and tc.metas here and since the thunk we get from thunkAndMap
        // should always return a bool
        assertInvoke(false) {
            tc.thunkFactory.thunkAndMap(EXPECT_BOOL_METAS, listOf(tc.fakeThunk, tc.fakeThunk)) { _, _ ->
                true
            }.invoke(Environment.standard())
        }
    }

    @ParameterizedTest
    @MethodSource("parameters")
    internal fun thunkEnvOperands1(tc: TestCase) =
        assertInvoke(tc.expectError) {
            tc.thunkFactory.thunkEnvOperands(tc.metas, tc.fakeThunk) { _, _ ->
                tc.thunkReturnValue
            }.invoke(Environment.standard())
        }

    @ParameterizedTest
    @MethodSource("parameters")
    internal fun thunkEnvOperands2(tc: TestCase) =
        assertInvoke(tc.expectError) {
            tc.thunkFactory.thunkEnvOperands(tc.metas, tc.fakeThunk, tc.fakeThunk) { _, _, _ ->
                tc.thunkReturnValue
            }.invoke(Environment.standard())
        }

    @ParameterizedTest
    @MethodSource("parameters")
    internal fun thunkEnvOperands3(tc: TestCase) =
        assertInvoke(tc.expectError) {
            tc.thunkFactory.thunkEnvOperands(tc.metas, tc.fakeThunk, tc.fakeThunk, tc.fakeThunk) { _, _, _, _ ->
                tc.thunkReturnValue
            }.invoke(Environment.standard())
        }

    @ParameterizedTest
    @MethodSource("parameters")
    internal fun thunkEnvOperandsList(tc: TestCase) =
        assertInvoke(tc.expectError) {
            tc.thunkFactory.thunkEnvOperands(tc.metas, listOf(tc.fakeThunk, tc.fakeThunk)) { _, _ ->
                tc.thunkReturnValue
            }.invoke(Environment.standard())
        }

    private fun assertInvoke(expectException: Boolean, block: () -> Unit) {
        if (expectException) {
            val ex = assertThrows<EvaluationException> {
                block()
            }
            assertEquals(
                ErrorCode.EVALUATOR_VALUE_NOT_INSTANCE_OF_EXPECTED_TYPE, ex.errorCode,
                "Message was: ${ex.message}"
            )
        } else {
            block()
        }
    }
}
