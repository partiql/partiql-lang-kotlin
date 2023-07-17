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
import org.partiql.ast.Statement
import org.partiql.cli.pico.PartiQLCommand
import org.partiql.lang.eval.EvaluationSession
import org.partiql.parser.PartiQLParserBuilder
import org.partiql.plan.PartiQLVersion
import org.partiql.plan.ion.PartiQLPlanIonWriter
import org.partiql.planner.impl.PartiQLPlannerDefault
import picocli.CommandLine
import java.io.PrintStream
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

    private val parser = PartiQLParserBuilder.standard().build()
    private val planner = PartiQLPlannerDefault()
    private val writer = PartiQLPlanIonWriter.get(PartiQLVersion.VERSION_0_1)

    // !!
    // IMPLEMENT DEBUG BEHAVIOR HERE
    // !!
    @Suppress("UNUSED_PARAMETER")
    @Throws(Exception::class)
    fun action(input: String, session: EvaluationSession): String {
        val out = PrintStream(System.out)
        val ast = parser.parse(input).root
        if (ast !is Statement) {
            error("Expect AST Statement, found $ast")
        }
        val plan = planner.plan(ast).plan
        val ion = writer.toIonDebug(plan)
        // Pretty print Ion
        val sb = StringBuilder()
        val formatter = IonTextWriterBuilder.pretty().build(sb)
        ion.writeTo(formatter)
        out.println(sb)
        return "OK"
    }
}
