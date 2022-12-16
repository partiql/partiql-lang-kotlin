package org.partiql.lang.eval.evaluatortestframework

import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.ExprValue

/**
 * Defines a test case for query evaluation.
 *
 * TODO: reorder these properties.
 */
data class EvaluatorTestCase(

    /** The "group" of the tests--this only appears in the IDE's test runner and can be used to identify where in the
     * source code the test is defined.
     */
    override val groupName: String? = null,

    /**
     * The query to be evaluated.
     *
     * The query is evaluated once in [TypingMode.LEGACY] and once in [TypingMode.PERMISSIVE].
     */
    override val query: String,

    /**
     * AN expression which will be evaluated to determine the set of expected values that should match the result
     * of [query].
     */
    val expectedResult: String,

    /**
     * The query's expected result when executed in permissive mode.  Defaults to [expectedResult].
     *
     * Some semantics of permissive mode have changed--namely, in permissive mode, MISSING propagates as NULL.
     *
     * Thus, even positive test cases may have a different result.
     */
    override val expectedPermissiveModeResult: String = expectedResult,

    /**
     * How to handle the expected result.
     *
     * @see [ExpectedResultFormat]
     */
    val expectedResultFormat: ExpectedResultFormat = ExpectedResultFormat.STRICT,

    /**
     * When true, after running the test once with compile options unmodified, run the test again in permissive mode.
     *
     * The default is `true` to ensure that permissive mode is tested as thoroughly as legacy mode.  However, some
     * tests explicitly set legacy or permissive mode.  Such tests should set [implicitPermissiveModeTest] to
     * `false`.  Note that, when `false`, [expectedPermissiveModeResult] is ignored.
     */
    override val implicitPermissiveModeTest: Boolean = true,

    /**
     * Determines which pipeline this test should run against; the [CompilerPipeline],
     * [org.partiql.lang.compiler.PartiQLCompilerPipeline] or both.
     */
    override val targetPipeline: EvaluatorTestTarget = EvaluatorTestTarget.ALL_PIPELINES,

    /**
     * Builder block for building [CompileOptions].
     */
    override val compileOptionsBuilderBlock: CompileOptions.Builder.() -> Unit = { },

    /**
     * Allows each test to configure its pipeline.
     */
    override val compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { },

    val extraResultAssertions: (ExprValue) -> Unit = { }
) : EvaluatorTestDefinition {
    constructor(
        query: String,
        expectedResult: String,
        expectedPermissiveModeResult: String = expectedResult,
        expectedResultFormat: ExpectedResultFormat = ExpectedResultFormat.STRICT,
        implicitPermissiveModeTest: Boolean = true,
        target: EvaluatorTestTarget = EvaluatorTestTarget.ALL_PIPELINES,
        compileOptionsBuilderBlock: CompileOptions.Builder.() -> Unit = { },
        compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { },
        extraResultAssertions: (ExprValue) -> Unit = { },
    ) : this(
        groupName = null,
        query = query,
        expectedResult = expectedResult,
        expectedPermissiveModeResult = expectedPermissiveModeResult,
        expectedResultFormat = expectedResultFormat,
        implicitPermissiveModeTest = implicitPermissiveModeTest,
        targetPipeline = target,
        compileOptionsBuilderBlock = compileOptionsBuilderBlock,
        compilerPipelineBuilderBlock = compilerPipelineBuilderBlock,
        extraResultAssertions = extraResultAssertions
    )

    /** This will show up in the IDE's test runner. */
    override fun toString() = when {
        groupName != null -> "$groupName : $query"
        else -> query
    }

    /** A generated and human-readable description of this test case for display in assertion failure messages. */
    fun testDetails(note: String, actualResult: String? = null): String {
        val b = StringBuilder()
        b.appendLine("Note            : $note")
        b.appendLine("Group name      : $groupName")
        b.appendLine("Query           : $query")
        b.appendLine("Target pipeline : $targetPipeline")
        b.appendLine("Expected result : $expectedResult")
        b.appendLine("Actual result   : $actualResult")
        b.appendLine("Result format   : $expectedResultFormat")

        return b.toString()
    }
}
