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

import org.partiql.annotations.ExperimentalPartiQLCompilerPipeline
import org.partiql.annotations.ExperimentalWindowFunctions
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ThunkReturnTypeAssertions
import org.partiql.lang.eval.TypingMode
import org.partiql.lang.eval.builtins.DynamicLookupExprFunction
import org.partiql.lang.eval.builtins.SCALAR_BUILTINS_DEFAULT
import org.partiql.lang.eval.builtins.definitionalBuiltins
import org.partiql.lang.eval.builtins.storedprocedure.StoredProcedure
import org.partiql.lang.eval.physical.operators.AggregateOperatorFactoryDefaultAsync
import org.partiql.lang.eval.physical.operators.FilterRelationalOperatorFactoryDefaultAsync
import org.partiql.lang.eval.physical.operators.JoinRelationalOperatorFactoryDefaultAsync
import org.partiql.lang.eval.physical.operators.LetRelationalOperatorFactoryDefaultAsync
import org.partiql.lang.eval.physical.operators.LimitRelationalOperatorFactoryDefaultAsync
import org.partiql.lang.eval.physical.operators.OffsetRelationalOperatorFactoryDefaultAsync
import org.partiql.lang.eval.physical.operators.RelationalOperatorFactory
import org.partiql.lang.eval.physical.operators.ScanRelationalOperatorFactoryDefaultAsync
import org.partiql.lang.eval.physical.operators.SortOperatorFactoryDefaultAsync
import org.partiql.lang.eval.physical.operators.UnpivotOperatorFactoryDefaultAsync
import org.partiql.lang.eval.physical.operators.WindowRelationalOperatorFactoryDefaultAsync
import org.partiql.lang.planner.EvaluatorOptions
import org.partiql.lang.types.CustomType

/**
 * Builder class to instantiate a [PartiQLCompilerAsync].
 *
 * Example usages:
 *
 * ```
 * // Default
 * val compiler = PartiQLCompilerAsyncBuilder.standard().build()
 *
 * // Fluent builder
 * val compiler = PartiQLCompilerAsyncBuilder.standard()
 *                                      .customFunctions(myCustomFunctionList)
 *                                      .build()
 * ```
 */

@ExperimentalPartiQLCompilerPipeline
class PartiQLCompilerAsyncBuilder private constructor() {

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
            AggregateOperatorFactoryDefaultAsync,
            SortOperatorFactoryDefaultAsync,
            UnpivotOperatorFactoryDefaultAsync,
            FilterRelationalOperatorFactoryDefaultAsync,
            ScanRelationalOperatorFactoryDefaultAsync,
            JoinRelationalOperatorFactoryDefaultAsync,
            OffsetRelationalOperatorFactoryDefaultAsync,
            LimitRelationalOperatorFactoryDefaultAsync,
            LetRelationalOperatorFactoryDefaultAsync,
            // Notice here we will not propagate the optin requirement to the user
            @OptIn(ExperimentalWindowFunctions::class)
            WindowRelationalOperatorFactoryDefaultAsync,
        )

        @JvmStatic
        fun standard() = PartiQLCompilerAsyncBuilder()
    }

    fun build(): PartiQLCompilerAsync {
        if (options.thunkOptions.thunkReturnTypeAssertions == ThunkReturnTypeAssertions.ENABLED) {
            TODO("ThunkReturnTypeAssertions.ENABLED requires a static type pass")
        }
        return PartiQLCompilerAsyncDefault(
            evaluatorOptions = options,
            customTypedOpParameters = customTypes.associateBy(
                keySelector = { it.name },
                valueTransform = { it.typedOpParameter }
            ),
            functions = allFunctions(options.typingMode),
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

    private fun allFunctions(typingMode: TypingMode): List<ExprFunction> {
        val definitionalBuiltins = definitionalBuiltins(typingMode)
        val builtins = SCALAR_BUILTINS_DEFAULT
        return definitionalBuiltins + builtins + customFunctions + DynamicLookupExprFunction()
    }

    private fun allOperatorFactories() = (DEFAULT_RELATIONAL_OPERATOR_FACTORIES + customOperatorFactories).apply {
        groupBy { it.key }.entries.firstOrNull { it.value.size > 1 }?.let {
            error(
                "More than one BindingsOperatorFactory for ${it.key.operator} named '${it.value}' was specified."
            )
        }
    }.associateBy { it.key }
}
