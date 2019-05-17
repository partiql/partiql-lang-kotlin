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

package org.partiql.testframework.testdriver.ast

import org.partiql.testframework.testdriver.error.*

/**
 * Visitor base class, suitable for implementing AST passes which inspect the AST but do not modify it, such as
 * semantic validation or script execution.
 *
 * Each AST node type implements an accept method which dispatches execution to the correct method below.
 *
 * Nodes that have no children have a single method prefixed with the word `visit`, while nodes that do have children
 * have two methods.  One of the methods is prefixed with `beforeVisit` which is invoked `before` the node's children
 * are visited an another method prefixed with `afterVisit` which is invoked `after` the node's children are visited.
 *
 * A trivial example of an AST validator that ensures each [TestCommand] node has a name would be:
 *
 * ```
 *     class ValidatorVisitor : AstVisitor {
 *         override fun visitTestCommand(cmd: TestCommand) {
 *             if(cmd.name == null || cmd.name.size == 0) {
 *                 throw InvalidASTException("test command missing name")
 *             }
 *         }
 *     }
 *```
 *
 * Invoking this would be simple:
 *
 * ```
 *     val theTestScript: TestScript = /* parse test script */
 *     val visitor = ValidatorVisitor()
 *     theTestScript.accept(visitor)
 * ```
 *
 */
abstract class AstVisitor(protected val resultStream: ResultStream) {
    open fun beforeVisitTestScript(script: TestScript) { }
    open fun afterVisitTestScript(script: TestScript) { }
    open fun beforeVisitFor(cmd: For) { }
    open fun afterVisitFor(cmd: For) { }
    open fun beforeVisitScriptCommandList(cmd: ScriptCommandList) {
        if(cmd.variableSetLocation != null) {
            resultStream.pushContext("In expanded template using variable set located at ${cmd.variableSetLocation}")
        }
    }
    open fun afterVisitScriptCommandList(cmd: ScriptCommandList) {
        if (cmd.variableSetLocation != null) {
            resultStream.popContext()
        }
    }
    open fun beforeVisitInclude(cmd: Include) { resultStream.pushContext("In script included from ${cmd.location}") }

    open fun afterVisitInclude(cmd: Include) { resultStream.popContext() }
    open fun visitTestCommand(cmd: TestCommand) {  }
    open fun visitBenchmark(cmd: BenchmarkCommand) {  }
    open fun visitSetDefaultEnvironment(cmd: SetDefaultEnvironment) { }
    open fun visitSetDefaultCompileOptions(cmd: SetDefaultCompileOptions) { }
    open fun visitSetDefaultSession(cmd: SetDefaultSession) { }
    open fun visitEnvironmentSpecFiles(envSpec: EnvironmentSpecFiles) { }
    open fun visitEnvironmentSpecStruct(envSpec: EnvironmentSpecStruct) { }
    open fun visitEnvironmentSpecVariableRef(envSpec: EnvironmentSpecVariableRef) { }
    open fun visitErrorExpectation(errorExpectation: ErrorExpectation) { }
    open fun visitSuccessExpectation(successExpectation: SuccessExpectation) { }
    open fun visitCountExpectation(successExpectation: CountExpectation) { }
    open fun visitVariableRefExpectation(variableRefExpectation: VariableRefExpectation) { }
}

