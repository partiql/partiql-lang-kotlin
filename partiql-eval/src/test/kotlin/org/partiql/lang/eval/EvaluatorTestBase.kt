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

import org.partiql.errors.ErrorCode
import org.partiql.errors.PropertyValueMap
import org.partiql.eval.CUSTOM_TEST_TYPES
import org.partiql.eval.TestBase
import org.partiql.eval.framework.EvaluatorTestTarget
import org.partiql.eval.framework.ExpectedResultFormat
import org.partiql.eval.framework.adapter.EvaluatorTestAdapter
import org.partiql.eval.framework.adapter.impl.MultipleTestAdapter
import org.partiql.eval.framework.adapter.impl.PipelineEvaluatorTestAdapter
import org.partiql.eval.framework.adapter.impl.VisitorTransformBaseTestAdapter
import org.partiql.eval.framework.pipeline.factory.impl.CompilerPipelineFactory
import org.partiql.eval.framework.pipeline.factory.impl.EvalEnginePipelineFactory
import org.partiql.eval.framework.pipeline.factory.impl.PartiQLCompilerPipelineFactory
import org.partiql.eval.framework.testcase.impl.EvaluatorErrorTestCase
import org.partiql.eval.framework.testcase.impl.EvaluatorTestCase
import org.partiql.eval.util.newFromIonText
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.SqlException
import org.partiql.lang.graph.ExternalGraphReader
import java.io.File

/**
 * [EvaluatorTestBase] contains testing infrastructure needed by all test classes that need to evaluate a query.
 */
abstract class EvaluatorTestBase : TestBase() {

    private val testHarness: EvaluatorTestAdapter = MultipleTestAdapter(
        listOf(
            PipelineEvaluatorTestAdapter(CompilerPipelineFactory()),
            PipelineEvaluatorTestAdapter(PartiQLCompilerPipelineFactory()),
            PipelineEvaluatorTestAdapter(EvalEnginePipelineFactory()),
            VisitorTransformBaseTestAdapter()
        )
    )

    private val classloader = EvaluatorTestBase::class.java.classLoader
    private fun readResource(resourcePath: String): String {
        val url = classloader.getResource(resourcePath)
            ?: error("Resource path not found: $resourcePath")
        return url.readText()
    }

    protected fun graphOfText(text: String): ExprValue =
        ExprValue.newGraph(ExternalGraphReader.read(text))

    protected fun graphOfResource(resource: String): ExprValue =
        graphOfText(readResource(resource))

    protected fun graphOfFile(file: String): ExprValue =
        graphOfText(File(file).readText())

    /** The basic method that can be used in tests to construct a session with value bindings. */
    protected fun sessionOf(bindMap: Map<String, ExprValue>) = EvaluationSession.build {
        globals(Bindings.ofMap(bindMap))
    }

    /** A convenience, for a session with bindings to both regular values and graphs,
     *  given by their textual Ion. */
    protected fun sessionOf(
        regulars: Map<String, String> = emptyMap(),
        graphs: Map<String, String> = emptyMap()
    ): EvaluationSession {
        val combined =
            regulars.mapValues { newFromIonText(it.value) } +
                graphs.mapValues { graphOfText(it.value) }
        return sessionOf(combined)
    }

    /** A convenience, for a session with bindings only to regular, non-graph, values. */
    protected fun Map<String, String>.toSession() =
        sessionOf(regulars = this@toSession)

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
        expectedResultFormat: ExpectedResultFormat = ExpectedResultFormat.ION,
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
        val pipeline = CompilerPipeline.builder().apply {
            customDataTypes(CUSTOM_TEST_TYPES)
            compileOptions(compileOptions)
            compilerPipelineBuilderBlock()
        }

        return pipeline.build().compile(source).eval(session)
    }
}
