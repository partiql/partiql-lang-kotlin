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

package org.partiql.testframework.testdriver.rewriters

import com.amazon.ion.*
import org.partiql.testframework.testdriver.*
import org.partiql.testframework.testdriver.ast.*
import org.partiql.testframework.testdriver.error.*
import org.partiql.testframework.testdriver.parser.*
import org.partiql.lang.util.*

class ForUnrollingRewriter(private val ions: IonSystem, resultStream: ResultStream) : AstRewriter(resultStream) {
    override fun rewriteFor(node: For): ScriptCommand =
        try {
            ScriptCommandList(
                node.location,
                node.template?.let {
                    node.variableSets.map {
                        resultStream.pushTemplateExpansionContext(it.location)
                        InterpolatingRewriter(it.struct, ions, resultStream, it.location).rewriteScriptCommandList(node.template)
                            .also {
                                resultStream.popContext()
                            }
                    }
                } ?: ArrayList()
            )
        } catch(ex: UndefinedVariableInterpolationException) {
            resultStream.error(node.location, "Undefined variable reference '${ex.variableName}' in Ion value or string literal inside for::{} command template.")
            node
        }

    class InterpolatingRewriter(private val variables: IonStruct,
                                private val ions: IonSystem,
                                resultStream: ResultStream,
                                private val variableSetLocation: ScriptLocation) : AstRewriter(resultStream) {

        override fun rewriteIonValue(ionValue: IonValue) = ionValue.interpolate(this.variables, this.ions)
        override fun rewriteString(s: String) = s.interpolate(this.variables, this.ions)

        override fun rewriteScriptCommandList(node: ScriptCommandList): ScriptCommandList =
            ScriptCommandList(node.location, node.commands.mapNotNull { rewriteScriptCommand(it) }, variableSetLocation)


        override fun rewriteEnvironmentSpecVariableRef(node: EnvironmentSpecVariableRef): EnvironmentSpec {
            val variableName = node.name.substring(1)
            val ionValue = variables[variableName] ?: throw UndefinedVariableInterpolationException(variableName)

            val reader = ions.newReader(ionValue)
            reader.next()
            val parser = ScriptParser(reader, node.location.inputName, ions, resultStream)

            return parser.parseEnvironmentSpec(node.location)
        }

        override fun rewriteIonText(ionText: IonText): IonText =
            when(ionText) {
                is IonSymbol ->{
                    if(ionText.stringValue().startsWith('$')) {
                        val variableName = ionText.stringValue().substring(1)
                        val variableValue = (variables[variableName] ?: throw UndefinedVariableInterpolationException(variableName))

                        ions.newString(variableValue.stringValue())
                    }

                    else ionText
                }
                is IonString -> {
                    ions.newString(
                        ionText.stringValue().interpolate(
                            variables,
                            ions
                        ))
                }
                else -> ionText
            }

        override fun rewriteVariableRefExpectation(node: VariableRefExpectation): Expectation {
            val ionValue = variables[node.variableName.substring(1)]
                           ?: throw UndefinedVariableInterpolationException(node.variableName)

            val reader = ions.newReader(ionValue)
            reader.next()
            val parser = ScriptParser(reader,
                                      node.location.inputName,
                                      ions,
                                      resultStream)

            return when (node.type) {
                VariableRefExpectation.Type.EXPECTED       -> parser.parseExpectation(node.location)
                VariableRefExpectation.Type.EXPECTED_COUNT -> parser.parseCountExpectation(node.location)
            }
        }
    }
}