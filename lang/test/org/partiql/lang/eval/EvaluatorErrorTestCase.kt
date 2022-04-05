package org.partiql.lang.eval

import org.partiql.lang.CompilerPipeline
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.PropertyValueMap

/**
 * Defines a error test case for query evaluation.
 */
data class EvaluatorErrorTestCase(
    /** The "group" of the tests--this only appears in the IDE's test runner and can be used to identify where in the
     * source code the test is defined.
     */
    val groupName: String?,

    /**
     * The query to be evaluated.
     */
    val query: String,

    /**
     * The [ErrorCode] the query is to throw.
     */
    val errorCode: ErrorCode,

    /**
     * The error context the query throws is to match this mapping.
     */
    val expectErrorContext: PropertyValueMap,

    /**
     * Expected result in the permissive mode. Default value is null.
     */
    val expectedPermissiveModeResult: String? = null,

    /**
     * Builder block for building [CompileOptions].
     */
    val compileOptionsBuilderBlock: CompileOptions.Builder.() -> Unit = { },

    /**
     * Allows each test to configure its pipeline.
     */
    val compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { }
) {

    constructor(
        query: String,
        errorCode: ErrorCode,
        expectErrorContext: PropertyValueMap,
        expectedPermissiveModeResult: String? = null,
        compileOptionsBuilderBlock: CompileOptions.Builder.() -> Unit = { },
        compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { }
    ) : this(
        null,
        query,
        errorCode,
        expectErrorContext,
        expectedPermissiveModeResult,
        compileOptionsBuilderBlock,
        compilerPipelineBuilderBlock
    )

    /** This will show up in the IDE's test runner. */
    override fun toString(): String {
        val groupNameString = if (groupName == null) "" else "$groupName"
        return "$groupNameString $query : $errorCode : $expectErrorContext"
    }
}
