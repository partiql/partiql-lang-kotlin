/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
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

package org.partiql.cli

import org.partiql.cli.format.pretty
import org.partiql.parser.PartiQLParser
import org.partiql.spi.errors.PRuntimeException
import picocli.CommandLine
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

/**
 * The `partiql fmt` subcommand: pretty-prints PartiQL statements.
 */
@CommandLine.Command(
    name = "fmt",
    mixinStandardHelpOptions = true,
    description = ["Format (pretty-print) PartiQL statements and print to stdout."]
)
internal class FmtCommand : Runnable {

    @CommandLine.Option(
        names = ["-w", "--width"],
        description = ["Target line width for formatting."],
        defaultValue = "80",
        showDefaultValue = CommandLine.Help.Visibility.ALWAYS,
    )
    var width: Int = 80

    @CommandLine.Option(
        names = ["-i", "--input"],
        description = ["Input file containing PartiQL statement(s)."],
    )
    var input: File? = null

    @CommandLine.Parameters(
        index = "0",
        arity = "0..1",
        description = ["An optional PartiQL statement string."],
        paramLabel = "'statement'",
    )
    var statement: String? = null

    override fun run() {
        if (statement != null && input != null) {
            error("Cannot specify both an input file and a statement string.")
        }
        val sql = try {
            statement ?: readInput()
        } catch (e: FileNotFoundException) {
            error("Input file does not exist: ${input?.path}")
        } catch (e: IOException) {
            error("Failed to read input: ${e.message}")
        }
        try {
            val parser = PartiQLParser.builder().build()
            val result = parser.parse(sql)
            val formatted = result.statements.joinToString(";\n\n") {
                it.pretty(width = width)
            }
            println(formatted)
        } catch (e: PRuntimeException) {
            error("Failed to parse input: ${e.error}")
        } catch (e: Exception) {
            error("Unexpected error during formatting: ${e.message}")
        }
    }

    private fun readInput(): String {
        val file = input
        return if (file != null) {
            file.readText()
        } else {
            System.`in`.bufferedReader().readText()
        }
    }
}
