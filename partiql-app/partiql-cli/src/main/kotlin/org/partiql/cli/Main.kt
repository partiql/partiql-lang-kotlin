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
import org.partiql.cli.pico.PartiQLCommand
import org.partiql.lang.eval.ExprValueFactory
import picocli.CommandLine
import kotlin.system.exitProcess

/**
 * Runs the PartiQL CLI.
 */
fun main(args: Array<String>) {
    val ion = IonSystemBuilder.standard().build()
    val valueFactory = ExprValueFactory.standard(ion)
    val command = CommandLine(PartiQLCommand(valueFactory))
    val exitCode = command.execute(*args)
    exitProcess(exitCode)
}
