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

package org.partiql.testframework.testdriver

import com.amazon.ion.*
import org.partiql.testframework.contracts.*
import org.partiql.testframework.testdriver.ast.*
import org.partiql.testframework.testdriver.ast.EnvironmentSpec
import org.partiql.testframework.testdriver.ast.EnvironmentSpecFiles
import org.partiql.testframework.testdriver.ast.EnvironmentSpecStruct

import java.io.*

fun AstNode.toCarCommand(): CarCommand {
    val rootDir = File(this.location.inputName).parent
    return when (this) {
        is SetDefaultEnvironment    -> SetEnvironmentCommand(this.envSpec.toContract(rootDir))
        is SetDefaultCompileOptions -> SetCompileOptionsCommand(this.options as? IonStruct ?: throw IllegalStateException("TODO:  handle variable reference that doesn't exist within a for template"))
        is SetDefaultSession        -> SetSessionCommand(this.session as? IonStruct ?: throw IllegalStateException("TODO:  handle variable reference that doesn't exist within a for template"))
        is TestCommand              -> ExecuteTestCommand(this.name!!, // should have failed validation if it's null
                                                          this.sql.stringValue(),
                                                          this.environmentSpec?.toContract(rootDir),
                                                          this.alternateCompileOptions as? IonStruct,
                                                          this.alternateSession as? IonStruct)
        is BenchmarkCommand         -> ExecuteBenchmarkCommand(this.name!!, // should have failed validation if it's null
                                                               this.sql.stringValue(),
                                                               this.environmentSpec?.toContract(rootDir),
                                                               this.alternateCompileOptions as? IonStruct,
                                                               this.alternateSession as? IonStruct)
        else                        -> throw IllegalArgumentException("I don't know know how to convert ${this.javaClass.canonicalName} to a car contract")
    }
}

fun EnvironmentSpec.toContract(rootDir: String): org.partiql.testframework.contracts.EnvironmentSpec = when (this) {
    is EnvironmentSpecFiles       -> org.partiql.testframework.contracts.EnvironmentSpecFiles(this.toAbsolutePaths(
        rootDir))
    is EnvironmentSpecStruct      -> org.partiql.testframework.contracts.EnvironmentSpecStruct(this.struct)
    is EnvironmentSpecVariableRef -> throw IllegalStateException("Can't ${this.javaClass.canonicalName} to a contract.")
}
