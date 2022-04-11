/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

// We don't need warnings about deprecated ExprNode.
@file:Suppress("DEPRECATION")

package org.partiql.lang.eval

import com.amazon.ion.IonType
import com.amazon.ion.IonValue
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.SqlException
import org.partiql.lang.TestBase
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.util.asSequence
import org.partiql.lang.util.newFromIonText

/**
 * This class is being deprecated because it is becoming unmaintainable but can't be removed yet.
 *
 * New tests should use JUnit5's parameterized testing as much as possible.
 *
 * When code re-use among test classes is needed, please prefer making your new functions top-level and accessible
 * from any test class.
 *
 * As we parameterize PartiQL's other tests, we should migrate them away from using this base class as well.
 */
abstract class EvaluatorTestBase : TestBase() {
    private val testHarness = AstEvaluatorTestHarness()

    protected fun Map<String, String>.toSession() = EvaluationSession.build {
        globals(Bindings.ofMap(this@toSession.mapValues { valueFactory.newFromIonText(it.value) }))
    }

    /**
     * Constructor style override of [runEvaluatorTestCase].  Constructs an [EvaluatorTestCase]
     * and runs it.  This is intended to be used by non-parameterized tests.
     *
     * The parameters of this function correspond to properties of [EvaluatorTestCase].
     *
     * DL TODO: reorder these parameters
     *
     * @see [EvaluatorTestCase]
     */
    protected fun runEvaluatorTestCase(
        query: String,
        expectedLegacyModeResult: String,
        session: EvaluationSession = EvaluationSession.standard(),
        excludeLegacySerializerAssertions: Boolean = false,
        compileOptionsBuilderBlock: CompileOptions.Builder.() -> Unit = { },
        compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { },
        expectedResultMode: ExpectedResultMode = ExpectedResultMode.ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS,
        expectedPermissiveModeResult: String = expectedLegacyModeResult,
        includePermissiveModeTest: Boolean = true,
        block: (ExprValue) -> Unit = { }
    ) {
        val tc = EvaluatorTestCase(
            query = query,
            expectedResult = expectedLegacyModeResult,
            expectedPermissiveModeResult = expectedPermissiveModeResult,
            expectedResultMode = expectedResultMode,
            excludeLegacySerializerAssertions = excludeLegacySerializerAssertions,
            compileOptionsBuilderBlock = compileOptionsBuilderBlock,
            compilerPipelineBuilderBlock = compilerPipelineBuilderBlock,
            implicitPermissiveModeTest = includePermissiveModeTest,
            extraResultAssertions = block
        )
        testHarness.runEvaluatorTestCase(tc, session)
    }

    /**
     * Runs an [EvaluatorTestCase].  This is intended to be used by parameterized tests.
     *
     * @see [EvaluatorTestCase].
     */
    protected fun runEvaluatorTestCase(tc: EvaluatorTestCase, session: EvaluationSession) =
        testHarness.runEvaluatorTestCase(tc, session)


    /** @see [AstEvaluatorTestHarness.runEvaluatorErrorTestCase]. */
    protected fun runEvaluatorErrorTestCase(
        query: String,
        expectedErrorCode: ErrorCode,
        expectedErrorContext: PropertyValueMap? = null,
        expectedPermissiveModeResult: String? = null,
        expectedInternalFlag: Boolean? = null,
        excludeLegacySerializerAssertions: Boolean = false,
        compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { },
        compileOptionsBuilderBlock: CompileOptions.Builder.() -> Unit = { },
        addtionalExceptionAssertBlock: (SqlException) -> Unit = { },
        implicitPermissiveModeTest: Boolean = true,
        session: EvaluationSession = EvaluationSession.standard()
    ) {
        val tc = EvaluatorErrorTestCase(
            query = query,
            expectedErrorCode = expectedErrorCode,
            expectedErrorContext = expectedErrorContext,
            expectedInternalFlag = expectedInternalFlag,
            expectedPermissiveModeResult = expectedPermissiveModeResult,
            excludeLegacySerializerAssertions = excludeLegacySerializerAssertions,
            compileOptionsBuilderBlock = compileOptionsBuilderBlock,
            compilerPipelineBuilderBlock = compilerPipelineBuilderBlock,
            implicitPermissiveModeTest = implicitPermissiveModeTest,
            additionalExceptionAssertBlock = addtionalExceptionAssertBlock,
        )

        testHarness.runEvaluatorErrorTestCase(tc, session)
    }

    /** @see [AstEvaluatorTestHarness.runEvaluatorTestCase] */
    fun runEvaluatorErrorTestCase(tc: EvaluatorErrorTestCase, session: EvaluationSession) =
        testHarness.runEvaluatorErrorTestCase(tc, session)

    /** @see [AstEvaluatorTestHarness.eval] */
    fun eval(
        source: String,
        compileOptions: CompileOptions = CompileOptions.standard(),
        session: EvaluationSession = EvaluationSession.standard(),
        compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { }
    ): ExprValue =
        testHarness.eval(source, compileOptions, session, compilerPipelineBuilderBlock)
}

internal fun IonValue.removeBagAndMissingAnnotations() {
    when (this.type) {
        // Remove $partiql_missing annotation from NULL for assertions
        IonType.NULL -> this.removeTypeAnnotation(MISSING_ANNOTATION)
        // Recurse into all container types.
        IonType.DATAGRAM, IonType.SEXP, IonType.STRUCT, IonType.LIST -> {
            // Remove $partiql_bag annotation from LIST for assertions
            if (this.type == IonType.LIST) {
                this.removeTypeAnnotation(BAG_ANNOTATION)
            }
            // Recursively remove annotations
            this.asSequence().forEach {
                it.removeBagAndMissingAnnotations()
            }
        }
        else -> { /* ok to do nothing. */ }
    }
}

/**
 * Clones and removes $partiql_bag and $partiql_missing annotations from the clone and any child values.
 *
 * There are many tests which were created before these annotations were present and thus do not include them
 * in their expected values.  This function provides an alternative to having to go and update all of them.
 * This is tech debt of the unhappy variety:  all of those test cases should really be updated and this function
 * should be deleted.
 *
 * NOTE: this function does not remove $partiql_date annotations ever!  There are tests that depend on this too.
 * $partiql_date however, was added AFTER this function was created, and so no test cases needed to remove that
 * annotation.
 */
internal fun IonValue.cloneAndRemoveBagAndMissingAnnotations() = this.clone().apply {
    removeBagAndMissingAnnotations()
    makeReadOnly()
}
