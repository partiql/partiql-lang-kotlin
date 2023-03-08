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

import com.amazon.ion.IonSystem
import org.partiql.annotations.ExperimentalPartiQLCompilerPipeline
import org.partiql.annotations.ExperimentalWindowFunctions
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ThunkReturnTypeAssertions
import org.partiql.lang.eval.builtins.DynamicLookupExprFunction
import org.partiql.lang.eval.builtins.createBuiltinFunctions
import org.partiql.lang.eval.builtins.storedprocedure.StoredProcedure
import org.partiql.lang.eval.physical.operators.AggregateOperatorFactoryDefault
import org.partiql.lang.eval.physical.operators.FilterRelationalOperatorFactoryDefault
import org.partiql.lang.eval.physical.operators.JoinRelationalOperatorFactoryDefault
import org.partiql.lang.eval.physical.operators.LetRelationalOperatorFactoryDefault
import org.partiql.lang.eval.physical.operators.LimitRelationalOperatorFactoryDefault
import org.partiql.lang.eval.physical.operators.OffsetRelationalOperatorFactoryDefault
import org.partiql.lang.eval.physical.operators.RelationalOperatorFactory
import org.partiql.lang.eval.physical.operators.ScanRelationalOperatorFactoryDefault
import org.partiql.lang.eval.physical.operators.SortOperatorFactoryDefault
import org.partiql.lang.eval.physical.operators.UnpivotOperatorFactoryDefault
import org.partiql.lang.eval.physical.operators.WindowRelationalOperatorFactoryDefault
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
 *                                      .customFunctions(myCustomFunctionList)
 *                                      .build()
 * ```
 */

@ExperimentalPartiQLCompilerPipeline
class PartiQLCompilerBuilder private constructor() {

    private var options: EvaluatorOptions = EvaluatorOptions.standard()
    private var customTypes: List<CustomType> = emptyList()
    private var customFunctions: List<ExprFunction> = emptyList()
    private var customProcedures: List<StoredProcedure> = emptyList()
    private var customOperatorFactories: List<RelationalOperatorFactory> = emptyList()

    companion object {

        /**
         * A collection of all the default relational operator implementations provided by PartiQL.
         *
         * By default, the query planner will select these as the implementations for all relational operators, but
         * alternate implementations may be provided and chosen by physical plan passes.
         *
         * @see [org.partiql.lang.planner.PlannerPipeline.Builder.addPhysicalPlanPass]
         * @see [org.partiql.lang.planner.PlannerPipeline.Builder.addRelationalOperatorFactory]
         */

        private val DEFAULT_RELATIONAL_OPERATOR_FACTORIES = listOf(
            AggregateOperatorFactoryDefault,
            SortOperatorFactoryDefault,
            UnpivotOperatorFactoryDefault,
            FilterRelationalOperatorFactoryDefault,
            ScanRelationalOperatorFactoryDefault,
            JoinRelationalOperatorFactoryDefault,
            OffsetRelationalOperatorFactoryDefault,
            LimitRelationalOperatorFactoryDefault,
            LetRelationalOperatorFactoryDefault,
            // Notice here we will not propagate the optin requirement to the user
            @OptIn(ExperimentalWindowFunctions::class)
            WindowRelationalOperatorFactoryDefault,
        )

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

    @Deprecated("An IonSystem is no longer needed to construct a PartiQLCompiler. Do not use this setter.")
    @Suppress("UNUSED_PARAMETER")
    fun ionSystem(ion: IonSystem): PartiQLCompilerBuilder = this.apply {
    }

    fun options(options: EvaluatorOptions) = this.apply {
        this.options = options
    }

    fun customFunctions(customFunctions: List<ExprFunction>) = this.apply {
        this.customFunctions = customFunctions
    }

    fun customTypes(customTypes: List<CustomType>) = this.apply {
        this.customTypes = customTypes
    }

    fun customProcedures(customProcedures: List<StoredProcedure>) = this.apply {
        this.customProcedures = customProcedures
    }

    fun customOperatorFactories(customOperatorFactories: List<RelationalOperatorFactory>) = this.apply {
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
