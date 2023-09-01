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

package org.partiql.transpiler.cli

import org.partiql.planner.test.plugin.FsPlugin
import picocli.CommandLine
import java.nio.file.Path
import java.util.Properties
import kotlin.system.exitProcess

/**
 * Run a PartiQL Transpiler REPL.
 *
 * Everything here is considered experimental. Catalog
 */
fun main(args: Array<String>) {
    val command = CommandLine(TranspileCommand())
    val exitCode = command.execute(*args)
    exitProcess(exitCode)
}

@CommandLine.Command(
    name = "transpile",
    mixinStandardHelpOptions = true,
    versionProvider = VersionProvider::class,
    descriptionHeading = "%n@|bold,underline,yellow The PartiQL Transpiler Debug REPL|@%n",
    description = ["This REPL is used for debugging the transpiler"],
    showDefaultValues = true
)
internal class TranspileCommand : Runnable {

    @CommandLine.Option(
        names = ["-d"],
        description = ["Database root directory"],
        paramLabel = "DIR",
        required = true,
    )
    lateinit var root: Path

    @CommandLine.Option(
        names = ["--catalog"],
        description = ["Catalog, use `default` .. by default"],
    )
    var catalog: String = "default"

    override fun run() {
        val fs = FsPlugin(root)
        Shell(fs, catalog).start()
    }
}

internal class VersionProvider : CommandLine.IVersionProvider {
    override fun getVersion(): Array<String> {
        val properties = Properties()
        properties.load(this.javaClass.getResourceAsStream("/transpiler.properties"))
        return Array(1) { "PartiQL Transpiler ${properties.getProperty("version")}-${properties.getProperty("commit")}" }
    }
}
