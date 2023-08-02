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
import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import org.partiql.annotations.ExperimentalPartiQLSchemaInferencer
import org.partiql.cli.pico.PartiQLCommand
import org.partiql.cli.shell.error
import org.partiql.cli.shell.info
import org.partiql.cli.shell.warn
import org.partiql.lang.errors.ProblemCollector
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.planner.transforms.PartiQLSchemaInferencer
import org.partiql.lang.planner.transforms.PlannerSession
import org.partiql.plan.debug.PlanPrinter
import org.partiql.plugins.mockdb.LocalPlugin
import org.partiql.transpiler.Transpiler
import org.partiql.transpiler.TranspilerProblem
import org.partiql.transpiler.targets.PartiQLTarget
import picocli.CommandLine
import java.io.PrintStream
import java.time.Instant
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
@OptIn(ExperimentalPartiQLSchemaInferencer::class)
object Debug {

    private val PLUGINS = listOf(LocalPlugin())

    private const val USER_ID = "DEBUG_USER_ID"
    private val CATALOG_MAP = mapOf(
        "local" to ionStructOf(
            field("connector_name", ionString("localdb")),
        )
    )

    private fun ctx(queryId: String, catalog: String, path: List<String> = emptyList()): PartiQLSchemaInferencer.Context {
        val session = PlannerSession(
            queryId,
            USER_ID,
            catalog,
            path,
            CATALOG_MAP,
            Instant.now(),
        )
        val collector = ProblemCollector()
        return PartiQLSchemaInferencer.Context(session, PLUGINS, collector)
    }

    @Suppress("UNUSED_PARAMETER")
    @Throws(Exception::class)
    fun action(input: String, session: EvaluationSession): String {
        // IMPLEMENT DEBUG BEHAVIOR HERE
        val target = PartiQLTarget
        val context = ctx("test-query", "local", listOf("babel"))
        val transpiler = Transpiler(target, context)
        val result = transpiler.transpile(input)
        val out = PrintStream(System.out)

        val hadProblems = result.problems.isNotEmpty()
        if (hadProblems) {
            out.println()
            for (p in result.problems) {
                val message = p.toString()
                when (p.level) {
                    TranspilerProblem.Level.INFO -> out.info(message)
                    TranspilerProblem.Level.WARNING -> out.warn(message)
                    TranspilerProblem.Level.ERROR -> out.error(message)
                }
            }
            out.println()
            out.info("-- Plan Dump ----------")
            out.println()
            // Inspect plan
            PlanPrinter.append(out, result.plan)
        }
        // Dump SQL
        out.println()
        out.info("-- Generated [${target.target}] SQL ----------")
        out.println()
        println(result.sql)
        out.println()
        return if (result.problems.isEmpty()) "OK" else "ERROR"
    }
}
