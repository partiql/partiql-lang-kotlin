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
import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.domains.PartiqlLogicalResolved
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.PartiQLException
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.PartiQLResult
import org.partiql.lang.eval.PartiQLStatementAsync
import org.partiql.lang.eval.builtins.storedprocedure.StoredProcedure
import org.partiql.lang.eval.physical.PhysicalBexprToThunkConverterAsync
import org.partiql.lang.eval.physical.PhysicalPlanCompilerAsync
import org.partiql.lang.eval.physical.PhysicalPlanCompilerAsyncImpl
import org.partiql.lang.eval.physical.PhysicalPlanThunkAsync
import org.partiql.lang.eval.physical.operators.RelationalOperatorFactory
import org.partiql.lang.eval.physical.operators.RelationalOperatorFactoryKey
import org.partiql.lang.planner.EvaluatorOptions
import org.partiql.lang.planner.PartiQLPlanner
import org.partiql.lang.types.TypedOpParameter

@ExperimentalPartiQLCompilerPipeline
internal class PartiQLCompilerAsyncDefault(
    evaluatorOptions: EvaluatorOptions,
    customTypedOpParameters: Map<String, TypedOpParameter>,
    functions: List<ExprFunction>,
    procedures: Map<String, StoredProcedure>,
    operatorFactories: Map<RelationalOperatorFactoryKey, RelationalOperatorFactory>
) : PartiQLCompilerAsync {

    private lateinit var exprConverter: PhysicalPlanCompilerAsyncImpl
    private val bexprConverter = PhysicalBexprToThunkConverterAsync(
        exprConverter = object : PhysicalPlanCompilerAsync {
            override suspend fun convert(expr: PartiqlPhysical.Expr): PhysicalPlanThunkAsync = exprConverter.convert(expr)
        },
        relationalOperatorFactory = operatorFactories
    )

    init {
        exprConverter = PhysicalPlanCompilerAsyncImpl(
            functions = functions,
            customTypedOpParameters = customTypedOpParameters,
            procedures = procedures,
            evaluatorOptions = evaluatorOptions,
            bexperConverter = bexprConverter
        )
    }

    override suspend fun compile(statement: PartiqlPhysical.Plan): PartiQLStatementAsync {
        return when (val stmt = statement.stmt) {
            is PartiqlPhysical.Statement.DmlDelete,
            is PartiqlPhysical.Statement.DmlInsert,
            is PartiqlPhysical.Statement.DmlUpdate -> TODO("DML compilation not supported.")
            is PartiqlPhysical.Statement.Exec,
            is PartiqlPhysical.Statement.Query -> {
                val expression = exprConverter.compile(statement)
                PartiQLStatementAsync { expression.eval(it) }
            }
            is PartiqlPhysical.Statement.Explain -> throw PartiQLException("Unable to compile EXPLAIN without details.")
        }
    }

    override suspend fun compile(statement: PartiqlPhysical.Plan, details: PartiQLPlanner.PlanningDetails): PartiQLStatementAsync {
        return when (val stmt = statement.stmt) {
            is PartiqlPhysical.Statement.DmlDelete,
            is PartiqlPhysical.Statement.DmlInsert,
            is PartiqlPhysical.Statement.DmlUpdate -> TODO("DML compilation not supported.")
            is PartiqlPhysical.Statement.Exec,
            is PartiqlPhysical.Statement.Query -> compile(statement)
            is PartiqlPhysical.Statement.Explain -> PartiQLStatementAsync { compileExplain(stmt, details) }
        }
    }

    // --- INTERNAL -------------------

    private enum class ExplainDomains {
        AST,
        AST_NORMALIZED,
        LOGICAL,
        LOGICAL_RESOLVED,
        PHYSICAL,
        PHYSICAL_TRANSFORMED
    }

    private fun compileExplain(statement: PartiqlPhysical.Statement.Explain, details: PartiQLPlanner.PlanningDetails): PartiQLResult.Explain.Domain {
        return when (val target = statement.target) {
            is PartiqlPhysical.ExplainTarget.Domain -> compileExplainDomain(target, details)
        }
    }

    private fun compileExplainDomain(statement: PartiqlPhysical.ExplainTarget.Domain, details: PartiQLPlanner.PlanningDetails): PartiQLResult.Explain.Domain {
        val format = statement.format?.text
        val type = statement.type?.text?.uppercase() ?: ExplainDomains.AST.name
        val domain = try {
            ExplainDomains.valueOf(type)
        } catch (ex: IllegalArgumentException) {
            throw PartiQLException("Illegal argument: $type")
        }
        return when (domain) {
            ExplainDomains.AST -> {
                val explain = details.ast!! as PartiqlAst.Statement.Explain
                val target = explain.target as PartiqlAst.ExplainTarget.Domain
                PartiQLResult.Explain.Domain(target.statement, format)
            }
            ExplainDomains.AST_NORMALIZED -> {
                val explain = details.astNormalized!! as PartiqlAst.Statement.Explain
                val target = explain.target as PartiqlAst.ExplainTarget.Domain
                PartiQLResult.Explain.Domain(target.statement, format)
            }
            ExplainDomains.LOGICAL -> {
                val explain = details.logical!!.stmt as PartiqlLogical.Statement.Explain
                val target = explain.target as PartiqlLogical.ExplainTarget.Domain
                val plan = details.logical.copy(stmt = target.statement)
                PartiQLResult.Explain.Domain(plan, format)
            }
            ExplainDomains.LOGICAL_RESOLVED -> {
                val explain = details.logicalResolved!!.stmt as PartiqlLogicalResolved.Statement.Explain
                val target = explain.target as PartiqlLogicalResolved.ExplainTarget.Domain
                val plan = details.logicalResolved.copy(stmt = target.statement)
                PartiQLResult.Explain.Domain(plan, format)
            }
            ExplainDomains.PHYSICAL -> {
                val explain = details.physical!!.stmt as PartiqlPhysical.Statement.Explain
                val target = explain.target as PartiqlPhysical.ExplainTarget.Domain
                val plan = details.physical.copy(stmt = target.statement)
                PartiQLResult.Explain.Domain(plan, format)
            }
            ExplainDomains.PHYSICAL_TRANSFORMED -> {
                val explain = details.physicalTransformed!!.stmt as PartiqlPhysical.Statement.Explain
                val target = explain.target as PartiqlPhysical.ExplainTarget.Domain
                val plan = details.physicalTransformed.copy(stmt = target.statement)
                PartiQLResult.Explain.Domain(plan, format)
            }
        }
    }
}
