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

import com.amazon.ion.*
import org.partiql.testframework.testdriver.parser.*
import java.nio.file.*

sealed class AstNode {
    abstract val location: ScriptLocation
    abstract fun accept(visitor: AstVisitor)
}

sealed class ScriptCommand : AstNode()

sealed class EnvironmentSpec : AstNode()

class EnvironmentSpecFiles(override val location: ScriptLocation, val paths: List<String>): EnvironmentSpec() {
    fun toAbsolutePaths(rootDir: String): List<String> =
        paths.map { Paths.get(rootDir, it).toAbsolutePath().toString() }.toList()

    override fun accept(visitor: AstVisitor) {
        visitor.visitEnvironmentSpecFiles(this)
    }
}

class EnvironmentSpecVariableRef(override val location: ScriptLocation, val name: String): EnvironmentSpec() {
    override fun accept(visitor: AstVisitor) {
        visitor.visitEnvironmentSpecVariableRef(this)
    }
}

class EnvironmentSpecStruct(override val location: ScriptLocation, val struct: IonStruct): EnvironmentSpec() {
    override fun accept(visitor: AstVisitor) {
        visitor.visitEnvironmentSpecStruct(this)
    }
}

sealed class Expectation : AstNode()

class VariableRefExpectation(override val location: ScriptLocation, val variableName: String, val type: Type) : Expectation() {
    enum class Type { EXPECTED, EXPECTED_COUNT }

    override fun accept(visitor: AstVisitor) {
        visitor.visitVariableRefExpectation(this)
    }
}

class SuccessExpectation(override val location: ScriptLocation, val expectedResult : IonValue) : Expectation() {
    override fun accept(visitor: AstVisitor) {
        visitor.visitSuccessExpectation(this)
    }
}

class CountExpectation(override val location: ScriptLocation, val expectedResult : Int) : Expectation() {
    override fun accept(visitor: AstVisitor) {
        visitor.visitCountExpectation(this)
    }
}

class ErrorExpectation(override val location: ScriptLocation, val expectedErrorCode: IonText?, val expectedProperties : IonStruct?) : Expectation() {
    override fun accept(visitor: AstVisitor) {
        visitor.visitErrorExpectation(this)
    }
}

sealed class ExecuteSqlCommand : ScriptCommand() {
    abstract val name: String?
    abstract val sql: IonText
    abstract val expectation: Expectation?
}

//A better name for this would have been just "Test" (to be consistent with the other `ScriptCommand` inheritors)
// however brazil thinks this class is a unit test if it has a "Test" suffix. :(
class TestCommand(
    override val location: ScriptLocation = ScriptLocation.NotSet,
    override val name: String?,
    val description: String? = null,
    override val sql: IonText,
    override val expectation: Expectation?,
    val environmentSpec: EnvironmentSpec? = null,
    val alternateCompileOptions : IonValue? = null,
    val alternateSession : IonValue? = null) : ExecuteSqlCommand() {

    override fun accept(visitor: AstVisitor) {
        visitor.visitTestCommand(this)
    }
}

class BenchmarkCommand(
    override val location: ScriptLocation = ScriptLocation.NotSet,
    override val name: String?,
    val description: String? = null,
    override val sql: IonText,
    override val expectation: Expectation?,
    val environmentSpec: EnvironmentSpec? = null,
    val alternateCompileOptions : IonValue? = null,
    val alternateSession : IonValue? = null) : ExecuteSqlCommand() {

    override fun accept(visitor: AstVisitor) = visitor.visitBenchmark(this)
}

class ScriptCommandList(
    override val location: ScriptLocation,
    val commands: List<ScriptCommand>,
    val variableSetLocation: ScriptLocation? = null) : ScriptCommand() {

    override fun accept(visitor: AstVisitor) {
        visitor.beforeVisitScriptCommandList(this)
        for (c in commands) {
            c.accept(visitor)
        }

        visitor.afterVisitScriptCommandList(this)
    }
}

class VariableSet(val location: ScriptLocation, val struct: IonStruct)

class For(
    override val location: ScriptLocation,
    val variableSets: List<VariableSet>,
    val template: ScriptCommandList?) : ScriptCommand() {

    override fun accept(visitor: AstVisitor) {
        visitor.beforeVisitFor(this)
        template?.accept(visitor)
        visitor.afterVisitFor(this)
    }
}

class Include(override val location: ScriptLocation, val script: TestScript) : ScriptCommand() {
    override fun accept(visitor: AstVisitor) {
        visitor.beforeVisitInclude(this)
        script.accept(visitor)
        visitor.afterVisitInclude(this)
    }
}

class SetDefaultCompileOptions(override val location: ScriptLocation, val options: IonValue) : ScriptCommand() {
    override fun accept(visitor: AstVisitor) {
        visitor.visitSetDefaultCompileOptions(this)
    }
}

class SetDefaultEnvironment(override val location: ScriptLocation, val envSpec: EnvironmentSpec) : ScriptCommand() {

    override fun accept(visitor: AstVisitor) {
        visitor.visitSetDefaultEnvironment(this)
    }
}


class SetDefaultSession(override val location: ScriptLocation, val session: IonStruct) : ScriptCommand() {
    override fun accept(visitor: AstVisitor) {
        visitor.visitSetDefaultSession(this)
    }
}

data class TestScript(val scriptPath: String, val commands: List<ScriptCommand>) : AstNode() {
    override val location: ScriptLocation
        get() = ScriptLocation.NotSet

    override fun accept(visitor: AstVisitor) {
        visitor.beforeVisitTestScript(this)
        for(c in commands) {
            c.accept(visitor)
        }
        visitor.afterVisitTestScript(this)
    }
}
