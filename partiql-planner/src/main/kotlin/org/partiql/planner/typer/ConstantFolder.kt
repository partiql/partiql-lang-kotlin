/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
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

package org.partiql.planner.typer

import org.partiql.plan.Fn
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.Rex
import org.partiql.plan.Statement
import org.partiql.plan.rex
import org.partiql.plan.rexOpLit
import org.partiql.plan.rexOpPath
import org.partiql.plan.rexOpPathStepIndex
import org.partiql.plan.rexOpStruct
import org.partiql.plan.rexOpStructField
import org.partiql.plan.statementQuery
import org.partiql.plan.util.PlanRewriter
import org.partiql.types.AnyOfType
import org.partiql.types.AnyType
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.value.BoolValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue
import org.partiql.value.TextValue
import org.partiql.value.boolValue
import org.partiql.value.stringValue

/**
 * Constant folds [Rex]'s.
 *
 * It's usefulness during typing can be seen in the following example:
 * ```
 * SELECT VALUE
 *   TUPLEUNION(
 *     CASE WHEN t IS STRUCT THEN t ELSE { '_1': t }
 *   )
 * FROM << { 'a': 1 } >> AS t
 * ```
 *
 * If the [PlanTyper] were to type the TUPLEUNION, it would see the CASE statement which has two possible output types:
 *   1. STRUCT( a: INT )
 *   2. STRUCT( _1: STRUCT( a: INT ) )
 *
 * Therefore, the TUPLEUNION would have these two potential outputs. Now, what happens when you have multiple arguments
 * that are all union types? It gets tricky, so we can fold things to make it easier to type. After constant folding,
 * the expression will look like:
 * ```
 * SELECT VALUE { 'a': t.a }
 * FROM << { 'a': 1} >> AS t
 * ```
 */
internal object ConstantFolder {

    /**
     * Constant folds an input [PartiQLPlan.statement].
     */
    internal fun fold(statement: Statement): Statement {
        if (statement !is Statement.Query) {
            throw IllegalArgumentException("ConstantFolder only supports Query statements")
        }
        val root = ConstantFolderImpl.visitRex(statement.root, statement.root.type)
        return statementQuery(root)
    }

    internal fun fold(rex: Rex): Rex {
        return ConstantFolderImpl.visitRex(rex, rex.type)
    }

    /**
     * When visiting a [Rex.Op], please be sure to pass the associated [Rex.type] to the visitor.
     */
    private object ConstantFolderImpl : PlanRewriter<StaticType>() {

        private fun fold(rex: Rex): Rex = visitRex(rex, rex.type)

        private fun default(op: Rex.Op, type: StaticType): Rex = rex(type, op)

        override fun visitRex(node: Rex, ctx: StaticType): Rex {
            return visitRexOp(node.op, node.type)
        }

        override fun visitRexOp(node: Rex.Op, ctx: StaticType): Rex {
            return when (val folded = super.visitRexOp(node, ctx)) {
                is Rex -> folded
                is Rex.Op -> rex(ctx, folded)
                else -> error("Expected to find Rex, but instead found $folded. We were visiting $node.")
            }
        }

        @OptIn(PartiQLValueExperimental::class)
        override fun visitRexOpCase(node: Rex.Op.Case, ctx: StaticType): Rex {
            val newBranches = node.branches.mapNotNull { branch ->
                val conditionFolded = fold(branch.condition)
                val conditionFoldedOp = conditionFolded.op as? Rex.Op.Lit ?: return@mapNotNull branch
                val conditionBooleanLiteral = conditionFoldedOp.value as? BoolValue ?: return@mapNotNull branch
                when (conditionBooleanLiteral.value) {
                    true -> branch.copy(conditionFolded, fold(branch.rex))
                    false -> null
                    else -> branch
                }
            }
            val firstBranch = newBranches.firstOrNull() ?: error("CASE_WHEN has NO branches.")
            return when (isLiteralTrue(firstBranch.condition)) {
                true -> firstBranch.rex
                false -> default(node.copy(newBranches), ctx)
            }
        }

        @OptIn(PartiQLValueExperimental::class)
        private fun isLiteralTrue(rex: Rex): Boolean {
            val op = rex.op as? Rex.Op.Lit ?: return false
            val value = op.value as? BoolValue ?: return false
            return value.value ?: false
        }

        @OptIn(PartiQLValueExperimental::class)
        private fun getLiteral(rex: Rex): PartiQLValue? {
            val op = rex.op as? Rex.Op.Lit ?: return null
            return op.value
        }

        @OptIn(PartiQLValueExperimental::class)
        override fun visitRexOpTupleUnion(node: Rex.Op.TupleUnion, ctx: StaticType): Rex {
            // Gather Struct Fields
            val args = node.args.map { fold(it) }
            val fields = args.flatMap { arg ->
                val argType = arg.type.flatten() as? StructType ?: return default(node.copy(args = args), ctx)
                if (argType.contentClosed.not()) { return default(node.copy(args = args), ctx) }
                argType.fields.map { it to arg }
            }.map { field ->
                val fieldName = rex(StaticType.STRING, rexOpLit(stringValue(field.first.key)))
                rexOpStructField(
                    k = fieldName,
                    v = rex(
                        field.first.value,
                        rexOpPath(field.second, steps = listOf(rexOpPathStepIndex(fieldName)))
                    )
                )
            }
            return fold(rex(ctx, rexOpStruct(fields)))
        }

        @OptIn(PartiQLValueExperimental::class)
        override fun visitRexOpPath(node: Rex.Op.Path, ctx: StaticType): Rex {
            val struct = node.root.op as? Rex.Op.Struct ?: return default(node, ctx)
            val step = node.steps.getOrNull(0) ?: return default(node, ctx)
            val stepIndex = step as? Rex.Op.Path.Step.Index ?: return default(node, ctx)
            val stepName = stepIndex.key.op as? Rex.Op.Lit ?: return default(node, ctx)
            val stepNameString = stepName.value as? TextValue<*> ?: return default(node, ctx)
            val matches = struct.fields.filter { field ->
                val fieldName = field.k.op as? Rex.Op.Lit ?: return default(node, ctx)
                val fieldNameString = fieldName.value as? StringValue ?: return default(node, ctx)
                val fieldNameStringValue = fieldNameString.value ?: return default(node, ctx)
                fieldNameStringValue == stepNameString.string
            }
            return when (matches.size) {
                1 -> matches[0].v
                else -> default(node, ctx)
            }
        }

        /**
         * We expect all variants of [visitRexOpCall] to visit their own arguments.
         * TODO: Function signature case sensitivity
         */
        override fun visitRexOpCall(node: Rex.Op.Call, ctx: StaticType): Rex {
            val fn = node.fn as? Fn.Resolved ?: return default(node, ctx)
            return when {
                fn.signature.name.equals("is_struct", ignoreCase = true) -> visitRexOpCallIsStruct(node, ctx)
                fn.signature.name.equals("eq", ignoreCase = true) -> visitRexOpCallEq(node, ctx)
                fn.signature.name.equals("not", ignoreCase = true) -> visitRexOpCallNot(node, ctx)
                else -> rex(ctx, node)
            }
        }

        /**
         * This relies on the fact that [Rex.equals] works and [PartiQLValue.equals] works.
         */
        @OptIn(PartiQLValueExperimental::class)
        private fun visitRexOpCallEq(folded: Rex.Op.Call, ctx: StaticType): Rex {
            val lhs = folded.args.getOrNull(0) ?: error("EQ should have a LHS argument.")
            val rhs = folded.args.getOrNull(1) ?: error("EQ should have a RHS argument.")
            val lhsFolded = fold(lhs)
            val rhsFolded = fold(rhs)
            // Same expressions
            if (lhsFolded == rhsFolded) {
                return rex(StaticType.BOOL, rexOpLit(boolValue(true)))
            }
            val lhsLiteral = getLiteral(lhsFolded) ?: return default(folded, ctx)
            val rhsLiteral = getLiteral(rhsFolded) ?: return default(folded, ctx)
            return rex(StaticType.BOOL, rexOpLit(boolValue(lhsLiteral == rhsLiteral)))
        }

        /**
         * This relies on the fact that [Rex.equals] works and [PartiQLValue.equals] works.
         */
        @OptIn(PartiQLValueExperimental::class)
        private fun visitRexOpCallNot(folded: Rex.Op.Call, ctx: StaticType): Rex {
            val lhs = folded.args.getOrNull(0) ?: error("NOT should have a LHS argument.")
            val lhsFolded = fold(lhs)
            val lhsLiteral = getLiteral(lhsFolded) ?: return default(folded, ctx)
            val booleanValue = lhsLiteral as? BoolValue ?: return default(folded, ctx)
            val boolean = booleanValue.value ?: return default(folded, ctx)
            return rex(StaticType.BOOL, rexOpLit(boolValue(boolean.not())))
        }

        @OptIn(PartiQLValueExperimental::class)
        private fun visitRexOpCallIsStruct(folded: Rex.Op.Call, ctx: StaticType): Rex {
            val isStructLhs = folded.args.getOrNull(0) ?: error("IS STRUCT should have a LHS argument.")
            return when (val resultType = isStructLhs.type.flatten()) {
                is StructType -> rex(StaticType.BOOL, rexOpLit(boolValue(true)))
                is AnyType -> default(folded, ctx)
                is AnyOfType -> {
                    when {
                        resultType.allTypes.any { it is AnyType } -> default(folded, ctx)
                        resultType.allTypes.any { it is AnyOfType } -> error("Flattened union types shouldn't contain unions")
                        resultType.allTypes.all { it is StructType } -> rex(StaticType.BOOL, rexOpLit(boolValue(true)))
                        resultType.allTypes.any { it is StructType }.not() -> rex(StaticType.BOOL, rexOpLit(boolValue(false)))
                        else -> default(folded, ctx)
                    }
                }
                else -> default(folded, ctx)
            }
        }
    }
}
