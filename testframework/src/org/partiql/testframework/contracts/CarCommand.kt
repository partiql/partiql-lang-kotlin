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

package org.partiql.testframework.contracts

import com.amazon.ion.*

sealed class CarCommand
class SetEnvironmentCommand(val envSpec: EnvironmentSpec) : CarCommand()
class SetSessionCommand(val struct: IonStruct) : CarCommand()
class SetCompileOptionsCommand(val options: IonStruct) : CarCommand()

sealed class ExecuteCommand : CarCommand() {
    abstract val name: String
    abstract val sql: String
    abstract val envSpec: EnvironmentSpec?
    abstract val compileOpt: IonStruct?
    abstract val session: IonStruct?
}

class ExecuteTestCommand(override val name: String,
                         override val sql: String,
                         override val envSpec: EnvironmentSpec?,
                         override val compileOpt: IonStruct?,
                         override val session: IonStruct?) : ExecuteCommand()

class ExecuteBenchmarkCommand(override val name: String,
                              override val sql: String,
                              override val envSpec: EnvironmentSpec?,
                              override val compileOpt: IonStruct?,
                              override val session: IonStruct?) : ExecuteCommand()

sealed class EnvironmentSpec
class EnvironmentSpecFiles(val paths: List<String>) : EnvironmentSpec()
class EnvironmentSpecStruct(val struct: IonStruct) : EnvironmentSpec()
