package org.partiql.lang.eval

import org.partiql.lang.CompilerPipeline
import org.partiql.lang.SqlException
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.PropertyValueMap

/**
 * Defines a error test case for query evaluation.
 */
data class EvaluatorErrorTestCase(
    /** The "group" of the tests--this only appears in the IDE's test runner and can be used to identify where in the
     * source code the test is defined.
     */
    val groupName: String? = null,

    /**
     * The query to be evaluated.
     */
    val query: String,

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
    val expectedInternalFlag: Boolean? = false,

    /**
     * Expected result in the permissive mode. Default value is null.
     */
    val expectedPermissiveModeResult: String? = null,

    /**
     * Set to true to avoid testing the legacy AST serializers which are deprecated
     * and not being updated to include new AST nodes.
     */
    val excludeLegacySerializerAssertions: Boolean = false,

    /**
     * Builder block for building [CompileOptions].
     */
    val compileOptionsBuilderBlock: CompileOptions.Builder.() -> Unit = { },

    /**
     * Allows each test to configure its pipeline.
     */
    val compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { },

    /**
     * This will be executed to perform additional exceptions on the resulting exception.
     */
    val additionalExceptionAssertBlock: (SqlException) -> Unit = { }
) {

    /** This will show up in the IDE's test runner. */
    override fun toString(): String {
        val groupNameString = if (groupName == null) "" else "$groupName"
        return "$groupNameString $query : $expectedErrorCode : $expectedErrorContext"
    }
}
