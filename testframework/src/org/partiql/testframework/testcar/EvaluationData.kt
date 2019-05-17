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
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.EvaluationSession
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.UndefinedVariableBehavior
import org.partiql.lang.util.stringValue

/**
 * Encapsulates all necessary data to run an evaluation for the PartiQL implementation
 */
class EvaluationData(
    val valueFactory: ExprValueFactory,
    environmentStruct: IonStruct,
    sessionStruct: IonStruct,
    compileOptionsStruct: IonStruct
) {
    val compileOptions by lazy { createCompileOptions(compileOptionsStruct) }
    val evaluationSession by lazy { createSession(sessionStruct, environmentStruct) }

    private fun err(message: String, cause: Throwable? = null): Nothing = throw IllegalArgumentException(message, cause)

    private fun createCompileOptions(compileOptionsStruct: IonStruct): CompileOptions = CompileOptions.build {
        undefinedVariable(UndefinedVariableBehavior.ERROR)

        compileOptionsStruct.forEach {
            if (it.isNullValue) err("Compile option '${it.fieldName}` cannot be null")


            when (it.fieldName) {
                "undefinedVariable" -> {
                    val uvb = try {
                        UndefinedVariableBehavior.valueOf(it.stringValue()!!)
                    }
                    catch (e: IllegalArgumentException) {
                        err("Value '${it.stringValue()}' is not a valid value for 'undefinedVariable'", e)
                    }

                    undefinedVariable(uvb)
                }
                else                -> err("Invalid compile option: '${it.fieldName}'")
            }
        }
    }

    private fun createSession(sessionStruct: IonStruct, environmentStruct: IonStruct): EvaluationSession = EvaluationSession.build {
        val environmentExprValue = valueFactory.newFromIonValue(environmentStruct)
        globals(environmentExprValue.bindings)

        sessionStruct.forEach {
            when (it.fieldName) {
                "now" -> {
                    val ionTs = it as? IonTimestamp ?: err("Invalid data type for 'now' session property--must be timestamp.")
                    now(ionTs.timestampValue())
                }
                else  -> err("Invalid session property '${it.fieldName}'")
            }
        }
    }
}