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
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.PartiQLException
import org.partiql.lang.eval.PartiQLStatementAsync
import org.partiql.lang.planner.PartiQLPlanner
import org.partiql.lang.planner.PartiQLPlannerBuilder
import org.partiql.lang.syntax.Parser
import org.partiql.lang.syntax.PartiQLParserBuilder

/**
 * TODO ALAN update usage
 * [PartiQLCompilerPipelineAsync] is the top-level class for embedded usage of PartiQL.
 *
 * Example usage:
 * ```
 * val pipeline = PartiQLCompilerPipeline.standard()
 * val session = // session bindings
 * val statement = pipeline.compile("-- some PartiQL query!")
 * val result = statement.eval(session)
 * when (result) {
 *   is PartiQLResult.Value -> handle(result)  // Query Result
 *   is PartiQLResult.Insert -> handle(result) // DML `Insert`
 *   is PartiQLResult.Delete -> handle(result) // DML `Delete`
 *   ...
 * }
 * ```
 */
@ExperimentalPartiQLCompilerPipeline
class PartiQLCompilerPipelineAsync(
    private val parser: Parser,
    private val planner: PartiQLPlanner,
    private val compiler: PartiQLCompilerAsync
) {

    companion object {

        /**
         * Returns a [PartiQLCompilerPipelineAsync] with default parser, planner, and compiler configurations.
         */
        @JvmStatic
        fun standard() = PartiQLCompilerPipelineAsync(
            parser = PartiQLParserBuilder.standard().build(),
            planner = PartiQLPlannerBuilder.standard().build(),
            compiler = PartiQLCompilerAsyncBuilder.standard().build()
        )

        /**
         * Builder utility for pipeline creation.
         *
         * Example usage:
         * ```
         * val pipeline = PartiQLCompilerPipeline.build {
         *    planner.options(plannerOptions)
         *           .globalVariableResolver(globalVariableResolver)
         *    compiler.ionSystem(ION)
         *            .options(evaluatorOptions)
         *            .customTypes(myCustomTypes)
         *            .customFunctions(myCustomFunctions)
         * }
         * ```
         */
        fun build(block: Builder.() -> Unit): PartiQLCompilerPipelineAsync {
            val builder = Builder()
            block.invoke(builder)
            return PartiQLCompilerPipelineAsync(
                parser = builder.parser.build(),
                planner = builder.planner.build(),
                compiler = builder.compiler.build(),
            )
        }
    }

    /**
     * Compiles a PartiQL query into an executable [PartiQLStatementAsync].
     */
    suspend fun compile(statement: String): PartiQLStatementAsync {
        val ast = parser.parseAstStatement(statement)
        return compile(ast)
    }

    /**
     * Compiles a [PartiqlAst.Statement] representation of a query into an executable [PartiQLStatementAsync].
     */
    suspend fun compile(statement: PartiqlAst.Statement): PartiQLStatementAsync {
        val result = planner.plan(statement)
        if (result is PartiQLPlanner.Result.Error) {
            throw PartiQLException(result.problems.toString())
        }
        val plan = (result as PartiQLPlanner.Result.Success).plan
        return compile(plan, result.details)
    }

    /**
     * Compiles a [PartiqlPhysical.Plan] representation of a query into an executable [PartiQLStatementAsync].
     */
    @JvmOverloads
    suspend fun compile(statement: PartiqlPhysical.Plan, details: PartiQLPlanner.PlanningDetails = PartiQLPlanner.PlanningDetails()): PartiQLStatementAsync {
        return compiler.compile(statement, details)
    }

    class Builder internal constructor() {
        var parser = PartiQLParserBuilder.standard()
        var planner = PartiQLPlannerBuilder.standard()
        var compiler = PartiQLCompilerAsyncBuilder.standard()
    }
}
