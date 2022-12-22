/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
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

package org.partiql.pico

import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.PartiQLResult
import org.partiql.lang.eval.ProjectionIterationBehavior
import org.partiql.lang.eval.TypedOpBehavior
import org.partiql.lang.eval.TypingMode
import org.partiql.lang.eval.UndefinedVariableBehavior
import org.partiql.pipeline.AbstractPipeline
import picocli.CommandLine
import java.io.File

internal class PipelineOptions {

    @CommandLine.Option(names = ["--pipeline"], description = ["The type of pipeline to use"], paramLabel = "TYPE")
    var pipelineType: AbstractPipeline.PipelineType = AbstractPipeline.PipelineType.STANDARD

    @CommandLine.Option(names = ["-e", "--environment"], description = ["File containing the global environment"], paramLabel = "FILE")
    var environmentFile: File? = null

    @CommandLine.Option(names = ["--typing-mode"], description = ["Specifies the typing mode"], paramLabel = "MODE")
    var typingMode: TypingMode = TypingMode.LEGACY

    @CommandLine.Option(names = ["-t", "--typed-op-behavior"], description = ["Indicates how CAST should behave"], paramLabel = "BEHAVIOR")
    var typedOpBehavior: TypedOpBehavior = TypedOpBehavior.HONOR_PARAMETERS

    @CommandLine.Option(names = ["-r", "--projection-iter-behavior"], description = ["Controls the behavior of ExprValue.iterator in the projection result"], paramLabel = "BEHAVIOR")
    var projIterBehavior: ProjectionIterationBehavior = ProjectionIterationBehavior.FILTER_MISSING

    @CommandLine.Option(names = ["-v", "--undefined-variable-behavior"], description = ["Defines the behavior when a non-existent variable is referenced"], paramLabel = "BEHAVIOR")
    var undefinedVarBehavior: UndefinedVariableBehavior = UndefinedVariableBehavior.ERROR

    private val pipelineOptions = AbstractPipeline.createPipelineOptions(
        pipelineType,
        typedOpBehavior,
        projIterBehavior,
        undefinedVarBehavior,
        typingMode
    )

    internal val pipeline = AbstractPipeline.create(pipelineOptions)

    internal val globalEnvironment = when (environmentFile) {
        null -> Bindings.empty()
        else -> getEnvironment(environmentFile!!, pipeline)
    }

    private fun getEnvironment(environmentFile: File, pipeline: AbstractPipeline): Bindings<ExprValue> {
        val configSource = environmentFile.readText(charset("UTF-8"))
        val config = pipeline.compile(configSource, EvaluationSession.standard()) as PartiQLResult.Value
        return config.value.bindings
    }
}