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

package org.partiql.testframework.testcar

import com.amazon.ion.*
import org.partiql.lang.eval.*
import org.partiql.testframework.contracts.*
import org.partiql.testframework.testcar.benchmark.*
import java.io.*

/**
 * The real car implementation, which actually executes the commands of the car against the reference implementation of PartiQL.
 */
class ReferenceSqlCar(private val ion: IonSystem) : Car {
    private var valueFactory = ExprValueFactory.standard(ion)
    private var defaultEnvironment = ion.newEmptyStruct()!!
    private var defaultCompileOptions = ion.newEmptyStruct()!!
    private var defaultSession = ion.newEmptyStruct()!!

    private val testExecutor = TestExecutor(valueFactory)
    private val benchmarkExecutor = BenchmarkExecutor(ion)

    /** A simple exception that when thrown will cause the car to reply to the command with Error(e.message!!) */
    internal class CarErrorException(message: String, override val cause: Throwable? = null) : Exception(message, cause)

    override fun executeCmd(cmd: CarCommand): CarResponse {
        return try {
            when (cmd) {
                is SetEnvironmentCommand    -> setEnvironment(cmd)
                is SetSessionCommand        -> setSession(cmd)
                is SetCompileOptionsCommand -> setCompileOptions(cmd)
                is ExecuteCommand           -> runExecuteCommand(cmd)
            }
        }
        catch (ex: CarErrorException) {
            Error(ex.message!!)
        }
    }

    private fun setEnvironment(cmd: SetEnvironmentCommand): CarResponse = try {
        val envSpec = cmd.envSpec
        defaultEnvironment = chooseEnvironment(envSpec)

        Ok()
    }
    catch (e: CarErrorException) {
        Error(e.message!!)
    }

    private fun setSession(cmd: SetSessionCommand): CarResponse {
        defaultSession = cmd.struct

        return Ok()
    }

    private fun setCompileOptions(cmd: SetCompileOptionsCommand): CarResponse {
        defaultCompileOptions = cmd.options

        return Ok()
    }

    private fun runExecuteCommand(cmd: ExecuteCommand): CarResponse = try {
        val environment = chooseEnvironment(cmd.envSpec)
        val evalSession = cmd.session ?: defaultSession
        val compileOptions = cmd.compileOpt ?: defaultCompileOptions

        when (cmd) {
            is ExecuteTestCommand      -> testExecutor.execute(cmd.sql, environment, evalSession, compileOptions)
            is ExecuteBenchmarkCommand -> {
                // Running this once as if it were an ExecuteTestCommand allows to see if there's a problem with it
                // because if an exception happens inside the VM that is forked by JMH it is difficult to debug.
                // Perhaps we can remove this as part of https://github.com/partiql/partiql-lang-kotlin/issues/34?
                val response = testExecutor.execute(cmd.sql, environment, evalSession, compileOptions)
                when (response) {
                    is Error -> response
                    else -> benchmarkExecutor.execute(cmd.name, cmd.sql, environment, evalSession, compileOptions)
                }
            }
        }
    }
    catch (e: CarErrorException) {
        Error(e.message!!)
    }

    private fun chooseEnvironment(envSpec: EnvironmentSpec?): IonStruct = when (envSpec) {
        null                     -> defaultEnvironment
        is EnvironmentSpecStruct -> envSpec.struct
        is EnvironmentSpecFiles  -> mergeStructs(envSpec.paths.map { loadIonStruct(it) })
    }

    private fun loadIonStruct(path: String): IonStruct = try {
        FileInputStream(path).use { inputStream ->
            ion.newReader(inputStream).use { ionReader ->
                ionReader.next()
                ion.newValue(ionReader) as? IonStruct ?: throw CarErrorException("$path did not contain an ion struct")
            }
        }
    }
    catch (e: FileNotFoundException) {
        throw CarErrorException("could not open $path", e)
    }

    /**
     * Performs a set union over two structs.  Throws if the union of the two structs is not empty.
     */
    fun mergeStructs(structs: List<IonStruct>): IonStruct {
        if (structs.size == 1) return structs.first()

        val resultStruct = ion.newEmptyStruct()
        structs.flatMap { it.asIterable() }.forEach {
            if (resultStruct.containsKey(it.fieldName)) {
                throw CarErrorException("The field name '${it.fieldName}' was used more than once in the set of structs to be merged.")
            }
            resultStruct.put(it.fieldName, it.clone())
        }
        return resultStruct
    }
}
