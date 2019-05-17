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
@file:Suppress("INACCESSIBLE_TYPE")

package org.partiql.testframework.testdriver

import org.partiql.testframework.*
import joptsimple.*
import java.io.*
import kotlin.system.*

const val HELP_DESCRIPTION = """PartiQL Test Suite
Runs test scripts and benchmarks to validate an PartiQL implementation.
"""

private val formatter = object : BuiltinHelpFormatter(80, 2) {
    override fun format(options: MutableMap<String, out OptionDescriptor>?): String {
        return "$HELP_DESCRIPTION \n\n${super.format(options)}"
    }
}

fun main(args: Array<String>) = try {
    val optParser = OptionParser()
    optParser.formatHelpWith(formatter)

    optParser.nonOptions("paths containing test scripts. If it's a directory it will run all test scripts recursively")
        .ofType(File::class.java)

    val helpOpt = optParser.accepts("help", "prints this help")
        .forHelp()

    val outputOpt = optParser.accepts("output", "output file, defaults to STDOUT")
        .withOptionalArg()
        .ofType(PrintStream::class.java)
        .defaultsTo(System.out)

    val optionSet = optParser.parse(*args)
    if (optionSet.has(helpOpt)) {
        optParser.printHelpOn(System.out)
        exitProcess(0)
    }

    val output = optionSet.valueOf(outputOpt)

    @Suppress("UNCHECKED_CAST") // optParser would have failed if this is not a List<File>
    val scriptPaths = optionSet.nonOptionArguments() as List<File>

    Cli(output, System.err).run(scriptPaths)
}
catch (e: FatalException) {
    System.err.println("fatal: " + e.message)
    System.err.println("details: " + e.details ?: "No details were provided")
    System.exit(-1)
}
catch (e: TestSuiteException) {
    System.err.println(e.message)
    System.exit(-1)
}

