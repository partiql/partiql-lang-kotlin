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

import org.partiql.cli.Cli
import org.partiql.cli.EmptyInputStream
import org.partiql.cli.UnclosableOutputStream
import org.partiql.lang.eval.ExprValueFactory
import picocli.CommandLine
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@CommandLine.Command(
    name = "query",
    mixinStandardHelpOptions = true,
    versionProvider = PartiQLVersionProvider::class,
    description = ["Runs a single query"]
)
internal class CommandQuery(private val valueFactory: ExprValueFactory) : Runnable {

    @CommandLine.Mixin
    internal lateinit var options: PipelineOptions

    @CommandLine.Option(names = ["-i", "--in"], description = ["The path to the input file"], paramLabel = "FILE")
    var inputFile: File? = null

    @CommandLine.Option(names = ["-f", "--in-format"], description = ["The input file format"], paramLabel = "FORMAT")
    var inputFormat: InputFormat = InputFormat.ION

    @CommandLine.Option(names = ["-o", "--out"], description = ["The path to the output file"], paramLabel = "FILE")
    var outputFile: File? = null

    @CommandLine.Option(names = ["-u", "--out-format"], description = ["The output file format"], paramLabel = "FORMAT")
    var outputFormat: OutputFormat = OutputFormat.PARTIQL

    @CommandLine.Option(names = ["-w", "--wrap-ion"], description = ["Indicates that the input Ion file contains a sequence of Ion values rather than a single Ion collection"])
    var wrapIon: Boolean = false

    @CommandLine.Parameters(description = ["The PartiQL query to run"], paramLabel = "QUERY")
    lateinit var query: String

    override fun run() {
        val pipeline = options.pipeline
        val input = when (inputFile) {
            null -> EmptyInputStream()
            else -> FileInputStream(inputFile!!)
        }
        val output = when (outputFile) {
            null -> UnclosableOutputStream(System.out)
            else -> FileOutputStream(outputFile!!)
        }

        input.use {
            output.use {
                Cli(valueFactory, input, inputFormat, output, outputFormat, pipeline, options.globalEnvironment, query, wrapIon).run()
            }
        }
    }

    enum class InputFormat {
        ION,
        PARTIQL
    }

    enum class OutputFormat {
        ION_TEXT,
        ION_BINARY,
        PARTIQL,
        PARTIQL_PRETTY
    }
}
