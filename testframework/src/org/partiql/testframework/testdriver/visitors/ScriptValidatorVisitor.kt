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

package org.partiql.testframework.testdriver.visitors

import org.partiql.testframework.testdriver.ast.*
import org.partiql.testframework.testdriver.error.*
import java.io.*

class ScriptValidatorVisitor(resultStream: ResultStream) : AstVisitor(resultStream) {

    /**
     * Implements a rule preventing the same test name from being used in the same script twice.  Does not consider
     * included scripts when checking for duplicate names. However, this is a visitor so this will execute once
     * for each TestScript that appears in the AST.
     */
    override fun beforeVisitTestScript(script: TestScript) {
        fun <T : ExecuteSqlCommand> validateUniqueName(commands: List<T>, typeName: String) {
            val nameCollector = mutableMapOf<String, T>()

            commands.filter { it.name != null } // null names will be dealt with in rewrite(cmd: Test)
                .forEach {
                    val saneName = it.name!!.toLowerCase()
                    if(nameCollector.containsKey(saneName)) {
                        val originalTest = nameCollector[saneName]
                        resultStream.error(originalTest!!.location, "$typeName name '${it.name}' is used more than once in this script (case-insensitive)\nused again here: ${it.location}")
                    }
                    else {
                        nameCollector [saneName] = it
                    }
                }
        }

        validateUniqueName(script.commands.filterIsInstance<BenchmarkCommand>(), "BenchmarkCommand")
        validateUniqueName(script.commands.filterIsInstance<TestCommand>(), "Test")
    }

    private fun validateExecuteSqlCommand(cmd: ExecuteSqlCommand, typeName: String) {
        resultStream.errorIf(cmd.name.isNullOrEmpty()) { Pair(cmd.location, "$typeName command is missing 'name' field") }
        resultStream.errorIf(cmd.sql.stringValue().isNullOrEmpty()) { Pair(cmd.location, "$typeName command is missing 'sql' field") }
        resultStream.errorIf(cmd.expectation == null) { Pair(cmd.location, "$typeName command is missing 'expected' field") }
    }

    override fun visitTestCommand(cmd: TestCommand) = validateExecuteSqlCommand(cmd, "test")
    override fun visitBenchmark(cmd: BenchmarkCommand) {
        validateExecuteSqlCommand(cmd, "benchmark")
        resultStream.errorIf(cmd.expectation is ErrorExpectation) { Pair(cmd.location, "benchmark command does not support 'error::' expectations") }
    }

    override fun visitSetDefaultEnvironment(cmd: SetDefaultEnvironment) =
        when(cmd.envSpec) {
            is EnvironmentSpecFiles  -> {
                resultStream.errorIf(cmd.envSpec.paths.isEmpty()) {
                    Pair(cmd.location, "Must specify a list of files or a struct to become the environment.")
                }

                cmd.envSpec.toAbsolutePaths(File(cmd.location.inputName).parent).filter { !File(it).canRead() }
                    .forEach { resultStream.error(cmd.location, "file '$it' does not exist or cannot be read") }
            }
            is EnvironmentSpecStruct -> { /* no validation rules exist for EnvironmentSpecStruct */ }
            is EnvironmentSpecVariableRef -> {
                throw IllegalStateException("Unresolved variable: `${cmd.envSpec.name}`")
            }
        }

    override fun visitSetDefaultCompileOptions(cmd: SetDefaultCompileOptions) {
        //TODO:  ask car to validate the compile options?
    }

    override fun visitSetDefaultSession(cmd: SetDefaultSession) {
        //TODO:  ask car to validate the session?
    }

    override fun beforeVisitFor(cmd: For) {
        when {
            cmd.variableSets.isEmpty()                              -> {
                resultStream.error(cmd.location, "for command has no variable sets")
            }
            cmd.template == null || cmd.template.commands.isEmpty() -> {
                resultStream.error(cmd.location, "for command has no template")
                return
            }
        }

        if(cmd.variableSets.any { it.struct.isEmpty }) {
            resultStream.error(cmd.location, "for command has at least one empty variableSet")
        }

        //Some additional refactoring is required to support this, since the contents of the
        //included scripts are parsed before the validator begins.  So for now we'll just block it here.
        //Even if we did this we should probably *not* allow variables to propagate into the AST branches of the
        //included scripts, but it could be useful to run the same tests over different files which were specified
        //in a variableSet.
        //The refactoring required would be to load and validate included scripts in a pass (inheriting from
        // AstRewriter) that executes *after* this one instead of having the parser load it.

        val badCommandVisitor = object : AstVisitor(resultStream) {
            override fun beforeVisitInclude(cmd: Include) {
                resultStream.error(
                    cmd.location,
                    "for command template may not contain an include command"
                                  )
                super.beforeVisitInclude(cmd)
            }
        }
        cmd.template!!.accept(badCommandVisitor)
    }
}
