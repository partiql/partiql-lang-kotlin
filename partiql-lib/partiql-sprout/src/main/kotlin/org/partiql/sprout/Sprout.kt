/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.sprout

import org.partiql.sprout.generator.Generator
import org.partiql.sprout.generator.NodeOptions
import org.partiql.sprout.generator.Options
import org.partiql.sprout.parser.SproutParser
import picocli.CommandLine
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Callable
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val command = CommandLine(Generate())
    exitProcess(command.execute(*args))
}

@CommandLine.Command(
    name = "generate",
    mixinStandardHelpOptions = true,
    version = ["0.0.1"],
    description = ["Generates Kotlin sources from type universe definitions"]
)
class Generate : Callable<Int> {

    @CommandLine.Parameters(
        index = "0",
        description = ["Type definition file"]
    )
    lateinit var file: File

    @CommandLine.Option(
        names = ["-p", "--package"],
        description = ["Package root"]
    )
    lateinit var packageRoot: String

    @CommandLine.Option(
        names = ["-u", "--universe"],
        description = ["Universe identifier"]
    )
    lateinit var id: String

    @CommandLine.Option(
        names = ["-o", "--out"],
        description = ["Generated source output directory"]
    )
    lateinit var out: Path

    override fun call(): Int {
        val input = BufferedReader(FileInputStream(file).reader()).readText()
        val parser = SproutParser.default()
        val universe = parser.parse(id, input)
        val options = Options(
            packageRoot = packageRoot,
            node = NodeOptions(
                modifier = NodeOptions.Modifier.DATA,
            )
        )
        val generator = Generator(options)
        val result = generator.generate(universe)
        // Write all generated files
        result.write {
            val p = it.packageName.replace(".", "/")
            val dir = out.resolve(p).toAbsolutePath()
            Files.createDirectories(dir)
            val file = Files.newBufferedWriter(dir.resolve("${it.name}.kt"))
            it.writeTo(file)
            file.close()
        }
        return 0
    }
}
