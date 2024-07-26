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

import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ion.system.IonTextWriterBuilder
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.ionString
import org.partiql.cli.pico.PartiQLCommand
import org.partiql.cli.shell.info
import org.partiql.lang.eval.EvaluationSession
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Statement
import org.partiql.plan.debug.PlanPrinter
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.catalog.Session
import org.partiql.types.PType
import picocli.CommandLine
import java.io.PrintStream
import java.nio.file.Paths
import kotlin.system.exitProcess

/**
 * Runs the PartiQL CLI.
 */
fun main(args: Array<String>) {
    val ion = IonSystemBuilder.standard().build()
    val command = CommandLine(PartiQLCommand(ion))
    val exitCode = command.execute(*args)
    exitProcess(exitCode)
}

/**
 * Highly visible place to modify shell behavior for debugging
 *
 * Consider giving this access to the print stream in Shell.
 * It would have been too hacky without a slight refactor, so now let's just assume System.out for debugging
 */
object Debug {

    private const val USER_ID = "DEBUG_USER_ID"

    private val root = Paths.get(System.getProperty("user.home")).resolve(".partiql/local")

    private val parser = PartiQLParser.default()
    private val planner = PartiQLPlanner.standard()

    // !!
    // IMPLEMENT DEBUG BEHAVIOR HERE
    // !!
    @Suppress("UNUSED_PARAMETER")
    @Throws(Exception::class)
    fun action(input: String, session: EvaluationSession): String {
        val out = PrintStream(System.out)

        // Parse
        val statement = parser.parse(input).root
        out.info("-- AST ----------")
        AstPrinter.append(out, statement)

        // Plan
        val sess = Session.builder()
            .identity("debug")
            .catalog("default")
            .build()
        val result = planner.plan(statement, sess).plan
        out.info("-- Plan ----------")
        PlanPrinter.append(out, result.statement)

        when (val plan = result.statement) {
            is Statement.Query -> {
                out.info("-- Schema ----------")
                val outputSchema = java.lang.StringBuilder()
                val ionWriter = IonTextWriterBuilder.minimal().withPrettyPrinting().build(outputSchema)
                plan.root.type.toIon().writeTo(ionWriter)
                out.info(outputSchema.toString())
            }
        }

        return "OK"
    }

    private fun PType.toIon(): IonElement {
        return ionString(this.toString())
    }
}
