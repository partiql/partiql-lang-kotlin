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
@file:JvmName("Main")

@file:Suppress("DEPRECATION")

package org.partiql.cli

import com.amazon.ion.system.IonSystemBuilder
import joptsimple.BuiltinHelpFormatter
import joptsimple.OptionDescriptor
import joptsimple.OptionException
import joptsimple.OptionParser
import joptsimple.OptionSet
import org.partiql.cli.functions.ReadFile
import org.partiql.cli.functions.WriteFile
import org.partiql.extensions.cli.functions.QueryDDB
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.PartiQLResult
import org.partiql.lang.eval.ProjectionIterationBehavior
import org.partiql.lang.eval.TypedOpBehavior
import org.partiql.lang.eval.TypingMode
import org.partiql.lang.eval.UndefinedVariableBehavior
import org.partiql.lang.syntax.PartiQLParserBuilder
import org.partiql.pipeline.AbstractPipeline
import org.partiql.shell.Shell
import org.partiql.shell.Shell.ShellConfiguration
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import kotlin.system.exitProcess

// TODO how can a user pass the catalog here?
private val ion = IonSystemBuilder.standard().build()
private val valueFactory = ExprValueFactory.standard(ion)

private val optParser = OptionParser()

private val formatter = object : BuiltinHelpFormatter(120, 2) {
    override fun format(options: MutableMap<String, out OptionDescriptor>?): String {
        return """PartiQL CLI
              |Command line interface for executing PartiQL queries. Can be run in an interactive (REPL) mode or non-interactive.
              |
              |Examples:
              |To run in REPL mode simply execute the executable without any arguments:
              |     partiql
              |
              |In non-interactive mode we use Ion as the format for input data which is bound to a global variable 
              |named "input_data", in the example below /logs/log.ion is bound to "input_data":
              |     partiql --query="SELECT * FROM input_data" --input=/logs/log.ion
              |
              |The cli can output using PartiQL syntax or Ion using the --output-format option, e.g. to output binary ion:
              |     partiql --query="SELECT * FROM input_data" --output-format=ION_BINARY --input=/logs/log.ion
              |
              |To pipe input data in via stdin:
              |     cat /logs/log.ion | partiql --query="SELECT * FROM input_data" --format=ION_BINARY > output.10n
              |
              |${super.format(options)}
        """.trimMargin()
    }
}

enum class InputFormat {
    PARTIQL, ION
}

enum class OutputFormat {
    ION_TEXT, ION_BINARY, PARTIQL, PARTIQL_PRETTY
}

enum class Pipeline {
    STANDARD, EXPERIMENTAL
}

// opt parser options

private val helpOpt = optParser.acceptsAll(listOf("help", "h"), "prints this help")
    .forHelp()

private val queryOpt = optParser.acceptsAll(listOf("query", "q"), "PartiQL query, triggers non interactive mode")
    .withRequiredArg()
    .ofType(String::class.java)

private val permissiveModeOpt = optParser.acceptsAll(listOf("permissive", "p"), "runs the query in permissive mode")

private val typedOpBehaviorOpt = optParser.acceptsAll(listOf("typed-op-behavior", "t"), "indicates how CAST should behave")
    .withRequiredArg()
    .ofType(TypedOpBehavior::class.java)
    .describedAs("(${TypedOpBehavior.values().joinToString("|")})")
    .defaultsTo(TypedOpBehavior.HONOR_PARAMETERS)

private val projectionIterationBehaviorOpt = optParser.acceptsAll(listOf("projection-iter-behavior", "r"), "Controls the behavior of ExprValue.iterator in the projection result")
    .withRequiredArg()
    .ofType(ProjectionIterationBehavior::class.java)
    .describedAs("(${ProjectionIterationBehavior.values().joinToString("|")})")
    .defaultsTo(ProjectionIterationBehavior.FILTER_MISSING)

private val undefinedVariableBehaviorOpt = optParser.acceptsAll(listOf("undefined-variable-behavior", "v"), "Defines the behavior when a non-existent variable is referenced")
    .withRequiredArg()
    .ofType(UndefinedVariableBehavior::class.java)
    .describedAs("(${UndefinedVariableBehavior.values().joinToString("|")})")
    .defaultsTo(UndefinedVariableBehavior.ERROR)

private val environmentOpt = optParser.acceptsAll(listOf("environment", "e"), "initial global environment (optional)")
    .withRequiredArg()
    .ofType(File::class.java)

private val inputFileOpt = optParser.acceptsAll(listOf("input", "i"), "input file, requires the query option (default: stdin)")
    .availableIf(queryOpt)
    .withRequiredArg()
    .ofType(File::class.java)

private val inputFormatOpt = optParser.acceptsAll(listOf("input-format", "if"), "input format, requires the query option")
    .availableIf(queryOpt)
    .withRequiredArg()
    .ofType(InputFormat::class.java)
    .describedAs("(${InputFormat.values().joinToString("|")})")
    .defaultsTo(InputFormat.ION)

private val wrapIonOpt = optParser.acceptsAll(listOf("wrap-ion", "w"), "wraps Ion input file values in a bag, requires the input format to be ION, requires the query option")
    .availableIf(queryOpt)

private val monochromeOpt = optParser.acceptsAll(listOf("monochrome", "m"), "removes syntax highlighting for the REPL")

private val outputFileOpt = optParser.acceptsAll(listOf("output", "o"), "output file, requires the query option (default: stdout)")
    .availableIf(queryOpt)
    .withRequiredArg()
    .ofType(File::class.java)

private val outputFormatOpt = optParser.acceptsAll(listOf("output-format", "of"), "output format, requires the query option")
    .availableIf(queryOpt)
    .withRequiredArg()
    .ofType(OutputFormat::class.java)
    .describedAs("(${OutputFormat.values().joinToString("|")})")
    .defaultsTo(OutputFormat.PARTIQL)

private val pipelineOpt = optParser.acceptsAll(listOf("pipeline"), "pipeline implementation")
    .withRequiredArg()
    .ofType(Pipeline::class.java)
    .describedAs("(${Pipeline.values().joinToString("|")})")
    .defaultsTo(Pipeline.STANDARD)

/**
 * Runs PartiQL CLI.
 *
 * Has two modes:
 * * Interactive (default): Starts a REPL
 * * Non-interactive: takes in an PartiQL query as a command line input
 *
 * Options:
 * * -e --environment: takes an environment file to load as the initial global environment
 * * -p --permissive: run the query in permissive typing mode (returns MISSING rather than error for data type
 * * -t --typed-op-behavior: indicates how CAST should behave: (default: HONOR_PARAMETERS) [LEGACY, HONOR_PARAMETERS]
 * * -r --projection-iter-behavior: Controls the behavior of ExprValue.iterator in the projection result: (default: FILTER_MISSING) [FILTER_MISSING, UNFILTERED]
 * * -v --undefined-variable-behavior: Defines the behavior when a non-existent variable is referenced: (default: ERROR) [ERROR, MISSING]
 * mismatches)
 * * Interactive only:
 *      * -m --monochrome: removes syntax highlighting for the REPL
 * * Non interactive only:
 *      * -q --query: PartiQL query
 *      * -i --input: input file
 *      * -if --input-format: (default: ION) [ION, PARTIQL]
 *      * -w --wrap-ion: wraps Ion input file values in a bag, requires the input format to be ION, requires the query option
 *      * -o --output: output file (default: STDOUT)
 *      * -of --output-format: (default: PARTIQL) [ION_TEXT, ION_BINARY, PARTIQL, PARTIQL_PRETTY]
 */
fun main(args: Array<String>) = try {
    optParser.formatHelpWith(formatter)

    val optionSet = optParser.parse(*args)
    if (optionSet.has(helpOpt)) {
        optParser.printHelpOn(System.out)
        exitProcess(0) // print help and bail
    }

    if (optionSet.nonOptionArguments().isNotEmpty()) {
        throw IllegalArgumentException("Non option arguments are not allowed!")
    }

    // Create Pipeline and Environment
    val options = createPipelineOptions(optionSet)
    val pipeline = AbstractPipeline.create(options)
    val environment = when (optionSet.has(environmentOpt)) {
        true -> getEnvironment(optionSet.valueOf(environmentOpt), pipeline)
        else -> Bindings.empty()
    }

    when (optionSet.has(queryOpt)) {
        true -> runCli(environment, optionSet, pipeline)
        false -> runShell(environment, optionSet, pipeline)
    }
} catch (e: OptionException) {
    System.err.println("${e.message}\n")
    optParser.printHelpOn(System.err)
    exitProcess(1)
} catch (e: Exception) {
    e.printStackTrace(System.err)
    exitProcess(1)
}

private fun createPipelineOptions(optionSet: OptionSet): AbstractPipeline.PipelineOptions {
    val ion = IonSystemBuilder.standard().build()
    val pipeline = optionSet.valueOf(pipelineOpt)
    val typedOpBehavior = optionSet.valueOf(typedOpBehaviorOpt)
    val projectionIteration = optionSet.valueOf(projectionIterationBehaviorOpt)
    val undefinedVariable = optionSet.valueOf(undefinedVariableBehaviorOpt)
    val permissiveMode = when (optionSet.has(permissiveModeOpt)) {
        true -> TypingMode.PERMISSIVE
        false -> TypingMode.LEGACY
    }

    val functions: List<(ExprValueFactory) -> ExprFunction> = listOf(
        { valueFactory -> ReadFile(valueFactory) },
        { valueFactory -> WriteFile(valueFactory) },
        { valueFactory -> QueryDDB(valueFactory) }
    )

    val parser = PartiQLParserBuilder().ionSystem(ion).build()
    return AbstractPipeline.PipelineOptions(
        pipeline,
        ion,
        parser,
        typedOpBehavior,
        projectionIteration,
        undefinedVariable,
        permissiveMode,
        functions = functions
    )
}

private fun getEnvironment(environmentFile: File, pipeline: AbstractPipeline): Bindings<ExprValue> {
    val configSource = environmentFile.readText(charset("UTF-8"))
    val config = pipeline.compile(configSource, EvaluationSession.standard()) as PartiQLResult.Value
    return config.value.bindings
}

private fun runShell(environment: Bindings<ExprValue>, optionSet: OptionSet, pipeline: AbstractPipeline) {
    val config = ShellConfiguration(isMonochrome = optionSet.has(monochromeOpt))
    Shell(valueFactory, System.out, pipeline, environment, config).start()
}

private fun runCli(environment: Bindings<ExprValue>, optionSet: OptionSet, pipeline: AbstractPipeline) {
    val input = when (optionSet.has(inputFileOpt)) {
        true -> FileInputStream(optionSet.valueOf(inputFileOpt))
        false -> EmptyInputStream()
    }
    val output = when (optionSet.has(outputFileOpt)) {
        true -> FileOutputStream(optionSet.valueOf(outputFileOpt))
        false -> UnclosableOutputStream(System.out)
    }

    val inputFormat = optionSet.valueOf(inputFormatOpt)
    val outputFormat = optionSet.valueOf(outputFormatOpt)
    val wrapIon = optionSet.has(wrapIonOpt)
    val query = optionSet.valueOf(queryOpt)

    input.use {
        output.use {
            Cli(valueFactory, input, inputFormat, output, outputFormat, pipeline, environment, query, wrapIon).run()
        }
    }
}
