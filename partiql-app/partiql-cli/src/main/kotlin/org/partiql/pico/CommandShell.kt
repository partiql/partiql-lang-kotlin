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

import org.partiql.lang.eval.ExprValueFactory
import org.partiql.shell.Shell
import picocli.CommandLine

@CommandLine.Command(
    name = "shell",
    mixinStandardHelpOptions = true,
    versionProvider = PartiQLVersionProvider::class,
    description = ["Launches an interactive shell to run multiple queries"]
)
internal class CommandShell(private val valueFactory: ExprValueFactory) : Runnable {

    @CommandLine.Mixin
    internal lateinit var options: PipelineOptions

    @CommandLine.Option(names = ["-m", "--monochrome"], description = ["Specifies that syntax highlighting should not be used"])
    var isMonochrome: Boolean = false

    override fun run() {
        val config = Shell.ShellConfiguration(isMonochrome = isMonochrome)
        Shell(valueFactory, System.out, options.pipeline, options.globalEnvironment, config).start()
    }
}
