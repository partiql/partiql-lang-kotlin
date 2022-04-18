package org.partiql.lang.eval.evaluatortestframework

import org.partiql.lang.CompilerPipeline
import org.partiql.lang.SqlException
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.eval.CompileOptions

/**
 * Defines a error test case for query evaluation.
 */
data class EvaluatorErrorTestCase(
    /** The "group" of the tests--this only appears in the IDE's test runner and can be used to identify where in the
     * source code the test is defined.
     */
    override val groupName: String? = null,

    /**
     * The query to be evaluated.
     */
    override val query: String,

    /**
     * The [ErrorCode] the query is to throw.
     */
    val expectedErrorCode: ErrorCode,

    /**
     * The error context the query throws is to match this mapping.
     */
    val expectedErrorContext: PropertyValueMap? = null,

    /**
     * The expected value of [org.partiql.lang.SqlException.internal].
     */
    val expectedInternalFlag: Boolean? = null,

    /**
     * Expected result in the permissive mode. Default value is null.
     *
     * Since the expression with the error isn't always the top-most in the test case's query, value returned by the
     * query may not be `MISSING`, but rather it might be a container with `MISSING` somewhere in it.  Thus, we cannot
     * always assume the result will be `MISSING`.
     */
    override val expectedPermissiveModeResult: String? = null,

    /**
     * Set to true to avoid testing the legacy AST serializers which are deprecated
     * and not being updated to include new AST nodes.
     */
    override val excludeLegacySerializerAssertions: Boolean = false,

    /**
     * Include permissive mode test.
     */
    override val implicitPermissiveModeTest: Boolean = true,

    /**
     * Builder block for building [CompileOptions].
     */
    override val compileOptionsBuilderBlock: CompileOptions.Builder.() -> Unit = { },

    /**
     * Allows each test to configure its pipeline.
     */
    override val compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { },

    /**
     * This will be executed to perform additional exceptions on the resulting exception.
     */
    val additionalExceptionAssertBlock: (SqlException) -> Unit = { }
) : EvaluatorTestDefinition {

    /** This will show up in the IDE's test runner. */
    override fun toString(): String {
        val groupNameString = if (groupName == null) "" else "$groupName"
        return "$groupNameString $query : $expectedErrorCode : $expectedErrorContext"
    }
}
