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

import picocli.CommandLine

@CommandLine.Command(
    name = "partiql",
    mixinStandardHelpOptions = true,
    versionProvider = PartiQLVersionProvider::class
)
internal class CommandPartiQL : Runnable {

    @CommandLine.Spec
    lateinit var spec: CommandLine.Model.CommandSpec

    override fun run() {
        println("Welcome to the PartiQL CLI!")
        spec.commandLine().usage(System.out)
    }
}
