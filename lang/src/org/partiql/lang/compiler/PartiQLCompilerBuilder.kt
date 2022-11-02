/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.compiler

import com.amazon.ion.system.IonSystemBuilder
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ThunkReturnTypeAssertions
import org.partiql.lang.eval.builtins.DynamicLookupExprFunction
import org.partiql.lang.eval.builtins.createBuiltinFunctions
import org.partiql.lang.eval.builtins.storedprocedure.StoredProcedure
import org.partiql.lang.eval.physical.operators.DEFAULT_RELATIONAL_OPERATOR_FACTORIES
import org.partiql.lang.eval.physical.operators.RelationalOperatorFactory
import org.partiql.lang.planner.EvaluatorOptions
import org.partiql.lang.types.CustomType

/**
 * Builder class to instantiate a [PartiQLCompiler].
 *
 * Example usages:
 *
 * ```
 * // Default
 * val compiler = PartiQLCompilerBuilder.standard().build()
 *
 * // Fluent builder
 * val compiler = PartiQLCompilerBuilder.standard()
 *                                      .ionSystem(myIonSystem)
 *                                      .customFunctions(myCustomFunctionList)
 *                                      .build()
 * ```
 */
class PartiQLCompilerBuilder private constructor() {

    private var options: EvaluatorOptions = EvaluatorOptions.standard()
    private var customTypes: List<CustomType> = emptyList()
    private var customFunctions: List<ExprFunction> = emptyList()
    private var customProcedures: List<StoredProcedure> = emptyList()
    private var customOperatorFactories: List<RelationalOperatorFactory> = emptyList()

    companion object {

        private val DEFAULT_ION = IonSystemBuilder.standard().build()

        @JvmStatic
        fun standard() = PartiQLCompilerBuilder()
    }

    fun build(): PartiQLCompiler {
        if (options.thunkOptions.thunkReturnTypeAssertions == ThunkReturnTypeAssertions.ENABLED) {
            TODO("ThunkReturnTypeAssertions.ENABLED requires a static type pass")
        }
        return PartiQLCompilerDefault(
            evaluatorOptions = options,
            customTypedOpParameters = customTypes.associateBy(
                keySelector = { it.name },
                valueTransform = { it.typedOpParameter }
            ),
            functions = allFunctions(),
            procedures = customProcedures.associateBy(
                keySelector = { it.signature.name },
                valueTransform = { it }
            ),
            operatorFactories = allOperatorFactories()
        )
    }

    fun options(options: EvaluatorOptions) = this.apply {
        this.options = options
    }

    /**
     * TODO This will be replaced by the open type system.
     *  - https://github.com/partiql/partiql-lang-kotlin/milestone/4
     */
    internal fun customFunctions(customFunctions: List<ExprFunction>) = this.apply {
        this.customFunctions = customFunctions
    }

    /**
     * TODO This will be replaced by the open type system.
     *  - https://github.com/partiql/partiql-lang-kotlin/milestone/4
     */
    internal fun customTypes(customTypes: List<CustomType>) = this.apply {
        this.customTypes = customTypes
    }

    /**
     * TODO This will be replaced by the open type system.
     *  - https://github.com/partiql/partiql-lang-kotlin/milestone/4
     */
    internal fun customProcedures(customProcedures: List<StoredProcedure>) = this.apply {
        this.customProcedures = customProcedures
    }

    internal fun customOperatorFactories(customOperatorFactories: List<RelationalOperatorFactory>) = this.apply {
        this.customOperatorFactories = customOperatorFactories
    }

    // --- Internal ----------------------------------

    private fun allFunctions(): Map<String, ExprFunction> {
        val builtins = createBuiltinFunctions()
        val allFunctions = builtins + customFunctions + DynamicLookupExprFunction()
        return allFunctions.associateBy { it.signature.name }
    }

    private fun allOperatorFactories() = (DEFAULT_RELATIONAL_OPERATOR_FACTORIES + customOperatorFactories).apply {
        groupBy { it.key }.entries.firstOrNull { it.value.size > 1 }?.let {
            error(
                "More than one BindingsOperatorFactory for ${it.key.operator} named '${it.value}' was specified."
            )
        }
    }.associateBy { it.key }
}
