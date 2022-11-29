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

import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.domains.PartiqlLogical
import org.partiql.lang.domains.PartiqlLogicalResolved
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.PartiQLException
import org.partiql.lang.eval.BindingCase
import org.partiql.lang.eval.BindingName
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.ExprFunction
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.Expression
import org.partiql.lang.eval.PartiQLResult
import org.partiql.lang.eval.PartiQLStatement
import org.partiql.lang.eval.builtins.storedprocedure.StoredProcedure
import org.partiql.lang.eval.physical.PhysicalBexprToThunkConverter
import org.partiql.lang.eval.physical.PhysicalPlanCompiler
import org.partiql.lang.eval.physical.PhysicalPlanCompilerImpl
import org.partiql.lang.eval.physical.PhysicalPlanThunk
import org.partiql.lang.eval.physical.operators.RelationalOperatorFactory
import org.partiql.lang.eval.physical.operators.RelationalOperatorFactoryKey
import org.partiql.lang.planner.DML_COMMAND_FIELD_ACTION
import org.partiql.lang.planner.DML_COMMAND_FIELD_ROWS
import org.partiql.lang.planner.DML_COMMAND_FIELD_TARGET_UNIQUE_ID
import org.partiql.lang.planner.DmlAction
import org.partiql.lang.planner.EvaluatorOptions
import org.partiql.lang.planner.PartiQLPlanner
import org.partiql.lang.types.TypedOpParameter

internal class PartiQLCompilerDefault(
    private val valueFactory: ExprValueFactory,
    private val evaluatorOptions: EvaluatorOptions,
    private val customTypedOpParameters: Map<String, TypedOpParameter>,
    private val functions: Map<String, ExprFunction>,
    private val procedures: Map<String, StoredProcedure>,
    private val operatorFactories: Map<RelationalOperatorFactoryKey, RelationalOperatorFactory>
) : PartiQLCompiler {

    private lateinit var exprConverter: PhysicalPlanCompilerImpl
    private val bexprConverter = PhysicalBexprToThunkConverter(
        valueFactory = this.valueFactory,
        exprConverter = object : PhysicalPlanCompiler {
            override fun convert(expr: PartiqlPhysical.Expr): PhysicalPlanThunk = exprConverter.convert(expr)
        },
        relationalOperatorFactory = operatorFactories
    )

    init {
        exprConverter = PhysicalPlanCompilerImpl(
            valueFactory = valueFactory,
            functions = functions,
            customTypedOpParameters = customTypedOpParameters,
            procedures = procedures,
            evaluatorOptions = evaluatorOptions,
            bexperConverter = bexprConverter
        )
    }

    override fun compile(statement: PartiqlPhysical.Plan): PartiQLStatement {
        val expression = exprConverter.compile(statement)
        return when (statement.stmt) {
            is PartiqlPhysical.Statement.DmlQuery -> PartiQLStatement { expression.eval(it).toDML() }
            is PartiqlPhysical.Statement.Exec,
            is PartiqlPhysical.Statement.Query -> PartiQLStatement { expression.eval(it).toValue() }
            is PartiqlPhysical.Statement.Explain -> throw PartiQLException("Unable to compile EXPLAIN without details.")
        }
    }

    override fun compile(statement: PartiqlPhysical.Plan, details: PartiQLPlanner.PlanningDetails): PartiQLStatement {
        return when (statement.stmt) {
            is PartiqlPhysical.Statement.DmlQuery,
            is PartiqlPhysical.Statement.Exec,
            is PartiqlPhysical.Statement.Query -> compile(statement)
            is PartiqlPhysical.Statement.Explain -> PartiQLStatement { compileExplain(statement.stmt, details) }
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
        return when (statement.target) {
            is PartiqlPhysical.ExplainTarget.Domain -> compileExplainDomain(statement.target, details)
        }
    }

    private fun compileExplainDomain(statement: PartiqlPhysical.ExplainTarget.Domain, details: PartiQLPlanner.PlanningDetails): PartiQLResult.Explain.Domain {
        val format = statement.format?.text
        val type = statement.type?.text?.toUpperCase() ?: ExplainDomains.AST.name
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

    /**
     * The physical expr converter is EvaluatingCompiler with s/Ast/Physical and `bindingsToValues -> Thunk`
     *   so it returns the [Expression] rather than a [PartiQLStatement]. This method parses a DML Command from the result.
     *
     * {
     *     'action': <action>,
     *     'target_unique_id': <unique_id>
     *     'rows': <rows>
     * }
     *
     * Later refactors will rework the Compiler to use [PartiQLStatement], but this is an acceptable workaround for now.
     */
    private fun ExprValue.toDML(): PartiQLResult {
        val action = bindings string DML_COMMAND_FIELD_ACTION
        val target = bindings string DML_COMMAND_FIELD_TARGET_UNIQUE_ID
        val rows = bindings seq DML_COMMAND_FIELD_ROWS
        return when (DmlAction.safeValueOf(action)) {
            DmlAction.INSERT -> PartiQLResult.Insert(target, rows)
            DmlAction.DELETE -> PartiQLResult.Delete(target, rows)
            DmlAction.REPLACE -> PartiQLResult.Replace(target, rows)
            null -> error("Unknown DML Action `$action`")
        }
    }

    private fun ExprValue.toValue(): PartiQLResult = PartiQLResult.Value(this)

    private infix fun Bindings<ExprValue>.string(field: String): String {
        return this[BindingName(field, BindingCase.SENSITIVE)]?.scalar?.stringValue() ?: missing(field)
    }

    private infix fun Bindings<ExprValue>.seq(field: String): Iterable<ExprValue> {
        val v = this[BindingName(field, BindingCase.SENSITIVE)] ?: missing(field)
        if (!v.type.isSequence) {
            error("DML command struct '$DML_COMMAND_FIELD_ROWS' field must be a bag or list")
        }
        return v.asIterable()
    }

    private fun missing(field: String): Nothing =
        error("Field `$field` missing from DML command struct or has incorrect Ion type")
}
