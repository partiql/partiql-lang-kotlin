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

package org.partiql.lang.eval

import com.amazon.ion.IonType
import com.amazon.ion.IonValue
import org.partiql.annotations.PartiQLExperimental
import org.partiql.lang.CUSTOM_TEST_TYPES
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.ION
import org.partiql.lang.SqlException
import org.partiql.lang.TestBase
import org.partiql.lang.errors.ErrorCode
import org.partiql.lang.errors.PropertyValueMap
import org.partiql.lang.eval.evaluatortestframework.CompilerPipelineFactory
import org.partiql.lang.eval.evaluatortestframework.EvaluatorErrorTestCase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestAdapter
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestCase
import org.partiql.lang.eval.evaluatortestframework.EvaluatorTestTarget
import org.partiql.lang.eval.evaluatortestframework.ExpectedResultFormat
import org.partiql.lang.eval.evaluatortestframework.MultipleTestAdapter
import org.partiql.lang.eval.evaluatortestframework.PartiQLCompilerPipelineFactory
import org.partiql.lang.eval.evaluatortestframework.PipelineEvaluatorTestAdapter
import org.partiql.lang.eval.evaluatortestframework.VisitorTransformBaseTestAdapter
import org.partiql.lang.util.asSequence
import org.partiql.lang.util.newFromIonText

/**
 * [EvaluatorTestBase] contains testing infrastructure needed by all test classes that need to evaluate a query.
 */
abstract class EvaluatorTestBase : TestBase() {
    @OptIn(PartiQLExperimental::class)
    private val testHarness: EvaluatorTestAdapter = MultipleTestAdapter(
        listOf(
            PipelineEvaluatorTestAdapter(CompilerPipelineFactory()),
            PipelineEvaluatorTestAdapter(PartiQLCompilerPipelineFactory()),
            VisitorTransformBaseTestAdapter()
        )
    )

    protected fun Map<String, String>.toSession() = EvaluationSession.build {
        globals(Bindings.ofMap(this@toSession.mapValues { newFromIonText(ION, it.value) }))
    }

    /**
     * Constructor style override of [runEvaluatorTestCase].  Constructs an [EvaluatorTestCase]
     * and runs it.  This is intended to be used by non-parameterized tests.
     *
     * The parameters of this function correspond to properties of [EvaluatorTestCase].
     *
     * @see [EvaluatorTestCase]
     */
    protected fun runEvaluatorTestCase(
        query: String,
        session: EvaluationSession = EvaluationSession.standard(),
        expectedResult: String,
        expectedPermissiveModeResult: String = expectedResult,
        expectedResultFormat: ExpectedResultFormat = ExpectedResultFormat.ION_WITHOUT_BAG_AND_MISSING_ANNOTATIONS,
        includePermissiveModeTest: Boolean = true,
        target: EvaluatorTestTarget = EvaluatorTestTarget.ALL_PIPELINES,
        compileOptionsBuilderBlock: CompileOptions.Builder.() -> Unit = { },
        compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { },
        extraResultAssertions: (ExprValue) -> Unit = { }
    ) {
        val tc = EvaluatorTestCase(
            query = query,
            expectedResult = expectedResult,
            expectedPermissiveModeResult = expectedPermissiveModeResult,
            expectedResultFormat = expectedResultFormat,
            implicitPermissiveModeTest = includePermissiveModeTest,
            target = target,
            compileOptionsBuilderBlock = compileOptionsBuilderBlock,
            compilerPipelineBuilderBlock = compilerPipelineBuilderBlock,
            extraResultAssertions = extraResultAssertions
        )
        testHarness.runEvaluatorTestCase(tc, session)
    }

    /**
     * Runs an [EvaluatorTestCase].  This is intended to be used by parameterized tests.
     *
     * @see [EvaluatorTestCase].
     */
    protected fun runEvaluatorTestCase(
        tc: EvaluatorTestCase,
        session: EvaluationSession = EvaluationSession.standard()
    ) =
        testHarness.runEvaluatorTestCase(tc, session)

    /** @see [AstEvaluatorTestAdapter.runEvaluatorErrorTestCase]. */
    protected fun runEvaluatorErrorTestCase(
        query: String,
        expectedErrorCode: ErrorCode,
        expectedErrorContext: PropertyValueMap? = null,
        expectedPermissiveModeResult: String? = null,
        expectedInternalFlag: Boolean? = null,
        compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { },
        compileOptionsBuilderBlock: CompileOptions.Builder.() -> Unit = { },
        addtionalExceptionAssertBlock: (SqlException) -> Unit = { },
        implicitPermissiveModeTest: Boolean = true,
        target: EvaluatorTestTarget = EvaluatorTestTarget.ALL_PIPELINES,
        session: EvaluationSession = EvaluationSession.standard()
    ) {
        val tc = EvaluatorErrorTestCase(
            query = query,
            expectedErrorCode = expectedErrorCode,
            expectedErrorContext = expectedErrorContext,
            expectedInternalFlag = expectedInternalFlag,
            expectedPermissiveModeResult = expectedPermissiveModeResult,
            implicitPermissiveModeTest = implicitPermissiveModeTest,
            additionalExceptionAssertBlock = addtionalExceptionAssertBlock,
            targetPipeline = target,
            compileOptionsBuilderBlock = compileOptionsBuilderBlock,
            compilerPipelineBuilderBlock = compilerPipelineBuilderBlock,
        )

        testHarness.runEvaluatorErrorTestCase(tc, session)
    }

    /** @see [AstEvaluatorTestAdapter.runEvaluatorTestCase] */
    fun runEvaluatorErrorTestCase(tc: EvaluatorErrorTestCase, session: EvaluationSession) =
        testHarness.runEvaluatorErrorTestCase(tc, session)

    /**
     * Uses the AST compiler to evaluate a PartiQL query using the AST evaluator.
     *
     * In general, this function this should be avoided.  It is currently only used to calculate
     * some expected values in [CastTestBase] and [NaturalExprValueComparatorsTest].  TODO: refactor these locations
     * to avoid calling this function.
     *
     * @param source query source to be evaluated
     * @param session [EvaluationSession] used for evaluation
     * @param compilerPipelineBuilderBlock any additional configuration to the pipeline after the options are set.
     */
    fun eval(
        source: String,
        compileOptions: CompileOptions = CompileOptions.standard(),
        session: EvaluationSession = EvaluationSession.standard(),
        compilerPipelineBuilderBlock: CompilerPipeline.Builder.() -> Unit = { }
    ): ExprValue {
        val pipeline = CompilerPipeline.builder(ION).apply {
            customDataTypes(CUSTOM_TEST_TYPES)
            compileOptions(compileOptions)
            compilerPipelineBuilderBlock()
        }

        return pipeline.build().compile(source).eval(session)
    }
}

internal fun IonValue.removeBagAndMissingAnnotations() {
    when (this.type) {
        // Remove $missing annotation from NULL for assertions
        IonType.NULL -> this.removeTypeAnnotation(MISSING_ANNOTATION)
        // Recurse into all container types.
        IonType.DATAGRAM, IonType.SEXP, IonType.STRUCT, IonType.LIST -> {
            // Remove $bag annotation from LIST for assertions
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
 * Clones and removes $bag and $missing annotations from the clone and any child values.
 *
 * There are many tests which were created before these annotations were present and thus do not include them
 * in their expected values.  This function provides an alternative to having to go and update all of them.
 * This is tech debt of the unhappy variety:  all of those test cases should really be updated and this function
 * should be deleted.
 *
 * NOTE: this function does not remove $date annotations ever!  There are tests that depend on this too.
 * $date however, was added AFTER this function was created, and so no test cases needed to remove that
 * annotation.
 */
internal fun IonValue.cloneAndRemoveBagAndMissingAnnotations() = this.clone().apply {
    removeBagAndMissingAnnotations()
    makeReadOnly()
}
