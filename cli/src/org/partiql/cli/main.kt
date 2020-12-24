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

package org.partiql.cli

import com.amazon.ion.system.*
import org.partiql.lang.eval.*
import org.partiql.lang.syntax.*
import org.partiql.cli.functions.*
import joptsimple.*
import org.partiql.lang.*
import java.io.*
import kotlin.system.exitProcess

// TODO how can a user pass the catalog here?
private val ion = IonSystemBuilder.standard().build()
private val valueFactory = ExprValueFactory.standard(ion)

private val parser = SqlParser(ion)
private val compilerPipeline = CompilerPipeline.build(ion) {
    addFunction(ReadFile(valueFactory))
    addFunction(WriteFile(valueFactory))
}

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

enum class OutputFormat {
    ION_TEXT, ION_BINARY, PARTIQL, PARTIQL_PRETTY
}

// opt parser options

private val helpOpt = optParser.acceptsAll(listOf("help", "h"), "prints this help")
    .forHelp()

private val queryOpt = optParser.acceptsAll(listOf("query", "q"), "PartiQL query, triggers non interactive mode")
    .withRequiredArg()
    .ofType(String::class.java)

private val environmentOpt = optParser.acceptsAll(listOf("environment", "e"), "initial global environment (optional)")
    .withRequiredArg()
    .ofType(File::class.java)

private val inputFileOpt = optParser.acceptsAll(listOf("input", "i"), "input file, requires the query option (default: stdin)")
    .availableIf(queryOpt)
    .withRequiredArg()
    .ofType(File::class.java)

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

/**
 * Runs PartiQL CLI.
 *
 * Has two modes:
 * * Interactive (default): Starts a REPL
 * * Non-interactive: takes in an PartiQL query as a command line input
 *
 * Options:
 * * -e --environment: takes an environment file to load as the initial global environment
 * * Non interactive only:
 *      * -q --query: PartiQL query
 *      * -i --input: input file, default STDIN
 *      * -o --output: output file, default STDOUT
 *      * -of --output-format: ION_TEXT, ION_BINARY, PARTIQL (default), PARTIQL_PRETTY
 */
fun main(args: Array<String>) = try {
    optParser.formatHelpWith(formatter)

    val optionSet = optParser.parse(*args)
    if (optionSet.has(helpOpt)) {
        optParser.printHelpOn(System.out)
        System.exit(0) // print help and bail
    }

    if(optionSet.nonOptionArguments().isNotEmpty()) {
        throw IllegalArgumentException("Non option arguments are not allowed!")
    }

    // common options
    val environment = when {
        optionSet.has(environmentOpt) -> {
            val configSource = optionSet.valueOf(environmentOpt).readText(charset("UTF-8"))
            val config = compilerPipeline.compile(configSource).eval(EvaluationSession.standard())
            config.bindings
        }
        else                          -> Bindings.empty()
    }

    if (optionSet.has(queryOpt)) {
        runCli(environment, optionSet)
    }
    else {
        runRepl(environment)
    }
}
catch (e: OptionException) {
    System.err.println("${e.message}\n")
    optParser.printHelpOn(System.err)
    exitProcess(1)

}
catch (e: Exception) {
    e.printStackTrace(System.err)
    exitProcess(1)
}

private fun runRepl(environment: Bindings<ExprValue>) {
    Repl(valueFactory, System.`in`, System.out, parser, compilerPipeline, environment).run()
}

private fun runCli(environment: Bindings<ExprValue>, optionSet: OptionSet) {
    val input = if (optionSet.has(inputFileOpt)) {
        FileInputStream(optionSet.valueOf(inputFileOpt))
    }
    else {
        UnclosableInputStream(System.`in`)
    }

    val output = if (optionSet.has(outputFileOpt)) {
        FileOutputStream(optionSet.valueOf(outputFileOpt))
    }
    else {
        UnclosableOutputStream(System.out)
    }

    val format = optionSet.valueOf(outputFormatOpt)

    val query = optionSet.valueOf(queryOpt)

    input.use {
        output.use {
            Cli(valueFactory, input, output, format, compilerPipeline, environment, query).run()
        }
    }
}
