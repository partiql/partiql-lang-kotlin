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

import org.partiql.sprout.generator.target.kotlin.KotlinCommand
import picocli.CommandLine
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val command = CommandLine(Sprout())
    exitProcess(command.execute(*args))
}

@CommandLine.Command(
    name = "sprout",
    mixinStandardHelpOptions = true,
    subcommands = [Generate::class],
)
class Sprout : Runnable {

    override fun run() {}
}

@CommandLine.Command(
    name = "generate",
    mixinStandardHelpOptions = true,
    subcommands = [
        KotlinCommand::class,
    ],
)
class Generate : Runnable {

    override fun run() {}
}
