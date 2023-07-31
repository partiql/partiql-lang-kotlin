package org.partiql.transpiler.targets

import org.partiql.ast.Ast
import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.Identifier
import org.partiql.ast.builder.ExprSfwBuilder
import org.partiql.ast.builder.ast
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.visitor.PlanBaseVisitor
import org.partiql.transpiler.TranspilerTarget

/**
 * This class transforms a relational expression tree to a PartiQL [Expr.SFW].
 *
 * !!!
 * This is naive and simple.
 * This only targets the basic SFW; so we assume the Plan of the form: SCAN -> FILTER -> PROJECT.
 * !!!
 *
 * This will require non-trivial rework to handle arbitrary plans.
 * See https://github.com/apache/calcite/blob/main/core/src/main/java/org/apache/calcite/rel/rel2sql/RelToSqlConverter.java
 */
public class RelToSql(
    private val ruleset: TranspilerTarget.BaseRuleset,
) {

    /**
     * This MUST return a mutable SFW builder because we'll need to inspect and/or replace the SELECT.
     */
    public fun apply(rel: Rel): ExprSfwBuilder {
        // descend
        val sfw = Visitor.visitRel(rel, ruleset)
        return sfw
    }

    private object Visitor : PlanBaseVisitor<ExprSfwBuilder, TranspilerTarget.BaseRuleset>() {

        override fun defaultReturn(node: PlanNode, ctx: TranspilerTarget.BaseRuleset) =
            TODO("Cannot translate relation expression $node")

        /**
         * Logical Scan -> FROM Clause
         */
        override fun visitRelScan(node: Rel.Scan, ctx: TranspilerTarget.BaseRuleset): ExprSfwBuilder {
            val sfw = ExprSfwBuilder()
            sfw.from = ast {
                fromValue {
                    expr = ctx.visitRex(node.value, Unit)
                    type = From.Value.Type.SCAN
                    asAlias = node.alias?.let { id(it) }
                    atAlias = node.at?.let { id(it) }
                    byAlias = node.by?.let { id(it) }
                }
            }
            return sfw
        }

        /**
         * Unpivot Scan -> FROM Clause
         */
        override fun visitRelUnpivot(node: Rel.Unpivot, ctx: TranspilerTarget.BaseRuleset): ExprSfwBuilder {
            val sfw = ExprSfwBuilder()
            sfw.from = ast {
                fromValue {
                    expr = ctx.visitRex(node.value, Unit)
                    type = From.Value.Type.UNPIVOT
                    asAlias = node.alias?.let { id(it) }
                    atAlias = node.at?.let { id(it) }
                    byAlias = node.by?.let { id(it) }
                }
            }
            return sfw
        }

        /**
         * Filter -> WHERE or HAVING Clause
         *
         * This is where we make the naive assumption that this is a simple SELECT-FROM-WHERE.
         */
        override fun visitRelFilter(node: Rel.Filter, ctx: TranspilerTarget.BaseRuleset): ExprSfwBuilder {
            val sfw = visitRel(node.input, ctx)
            sfw.where = ctx.visitRex(node.condition, Unit)
            return sfw
        }

        /**
         * Project -> SELECT Clause
         */
        override fun visitRelProject(node: Rel.Project, ctx: TranspilerTarget.BaseRuleset): ExprSfwBuilder {
            val sfw = visitRel(node.input, ctx)
            sfw.select = ast {
                // SQL SELECT-PROJECT
                selectProject {
                    for (b in node.bindings) {
                        items += selectProjectItemExpression {
                            expr = ctx.visitRex(b.value, Unit)
                            asAlias = identifierSymbol(b.name, Identifier.CaseSensitivity.SENSITIVE)
                        }
                    }
                }
            }
            return sfw
        }

        // Helpers

        private fun id(symbol: String): Identifier.Symbol = Ast.identifierSymbol(symbol, Identifier.CaseSensitivity.SENSITIVE)
    }
}

/**
 * Syntax sugar for Kotlin
 */
public fun Rel.toSFW(ruleset: TranspilerTarget.BaseRuleset) = RelToSql(ruleset).apply(this)
