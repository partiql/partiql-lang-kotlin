package org.partiql.lakeformation

import org.partiql.lakeformation.exception.LakeFormationQuerySemanticException
import org.partiql.lakeformation.exception.LakeFormationQueryUnsupportedException
import org.partiql.lang.planner.transforms.plan.Planner
import org.partiql.lang.syntax.Parser
import org.partiql.plan.Arg
import org.partiql.plan.PartiQLPlan
import org.partiql.plan.PlanNode
import org.partiql.plan.Rex
import org.partiql.plan.builder.PartiQlPlanBuilder
import org.partiql.plan.visitor.PlanBaseVisitor
import org.partiql.types.StringType
import java.util.regex.Pattern

/**
 * Validate a row filter expression using Plan
 */
class RowFilterValidator(val parser: Parser) {

    // Lake Formation restriction.
    // See: https://docs.aws.amazon.com/lake-formation/latest/APIReference/API_RowFilter.html
    private val ROW_FILTER_EXPRESSION_MAX_LENGTH = 2047
    private val ROW_FILTER_EXPRESSION_PATTERN: Pattern = Pattern.compile("^[^\\s].*[^\\s;]$")

    fun validate(rowFilterExpression: String) {
        validatePredicateSyntax(rowFilterExpression)
        validateRowFilterExpression(rowFilterExpression)
    }

    private fun validatePredicateSyntax(rowFilterExpression: String) {
        val plan = Planner.plan(query = rowFilterExpression, parser)
        plan.accept(LakeFormationSyntaxValidation, Unit)
    }

    private fun validateRowFilterExpression(rowFilterExpression: String) {
        if (rowFilterExpression.length > ROW_FILTER_EXPRESSION_MAX_LENGTH) {
            throw LakeFormationQueryUnsupportedException(
                "Row filter expression length must be less than or equal to $ROW_FILTER_EXPRESSION_MAX_LENGTH"
            )
        } else if (!ROW_FILTER_EXPRESSION_PATTERN.matcher(rowFilterExpression).matches()) {
            throw LakeFormationQueryUnsupportedException(
                "Row filter expression must match $ROW_FILTER_EXPRESSION_PATTERN"
            )
        }
    }

    private object LakeFormationSyntaxValidation : PlanBaseVisitor<Unit, Unit>() {
        override fun defaultReturn(node: PlanNode, ctx: Unit) = Unit

        override fun visitRex(node: Rex, ctx: Unit) {
            when (node) {
                is Rex.Binary -> visitRexBinary(node, ctx)
                is Rex.Call -> visitRexCall(node, ctx)
                // TODO : CHECK THIS.
                is Rex.Unary -> throw LakeFormationQueryUnsupportedException("Unary operation is not supported by Lake Formation")

                // Unsupported
                is Rex.Agg, is Rex.Collection.Array, is Rex.Collection.Bag,
                is Rex.Id, is Rex.Lit, is Rex.Path, is Rex.Query.Collection,
                is Rex.Query.Scalar.Pivot, is Rex.Query.Scalar.Subquery,
                is Rex.Switch, is Rex.Tuple -> throw LakeFormationQueryUnsupportedException("Operation not supported by Lake Formation, received $node")
            }
        }

        override fun visitRexBinary(node: Rex.Binary, ctx: Unit) {
            when (node.op) {
                Rex.Binary.Op.PLUS, Rex.Binary.Op.MINUS,
                Rex.Binary.Op.TIMES, Rex.Binary.Op.DIV,
                Rex.Binary.Op.MODULO, Rex.Binary.Op.CONCAT -> throw LakeFormationQueryUnsupportedException("Operation not supported by Lake Formation, received $node")
                // treat left part and right part as separate PartiQL Plan to test
                Rex.Binary.Op.AND, Rex.Binary.Op.OR -> {
                    // Left hand side expression + right hand side expression
                    val expressions = listOf(node.lhs, node.rhs)
                    expressions.forEach {
                        // Dummy node, just use V0 as version.
                        val planNode = PartiQlPlanBuilder().root(it).version(PartiQLPlan.Version.PARTIQL_V0).build()
                        visitPartiQLPlan(planNode, ctx)
                    }
                }
                Rex.Binary.Op.EQ, Rex.Binary.Op.NEQ,
                Rex.Binary.Op.GTE, Rex.Binary.Op.GT,
                Rex.Binary.Op.LT, Rex.Binary.Op.LTE -> {
                    when (node.lhs) {
                        is Rex.Id -> Unit
                        else -> throw LakeFormationQuerySemanticException("Lake Formation Row Filter, expect a Identifier at lhs of basic comparator, received ${node.lhs}")
                    }
                    when (node.rhs) {
                        is Rex.Lit -> Unit
                        else -> throw LakeFormationQuerySemanticException("Lake Formation Row Filter, expect a Literal at rhs of basic comparator, received ${node.rhs}")
                    }
                }
            }
        }

        override fun visitRexCall(node: Rex.Call, ctx: Unit) {
            when (node.id) {
                "between" -> {
                    val args = node.args
                    val lhsArg = args.getOrNull(0) ?: throw LakeFormationQuerySemanticException("no column name provided for operator between")
                    val fromArg = args.getOrNull(1) ?: throw LakeFormationQuerySemanticException("between operator does not have from value")
                    val toArg = args.getOrNull(1) ?: throw LakeFormationQuerySemanticException("between operator does not have from value")
                    checkLhsIsId(lhsArg, node.id)
                    when (val from = getValueFromArg(fromArg)) {
                        is Rex.Lit -> Unit
                        else -> throw LakeFormationQuerySemanticException("Lake Formation Row Filter, expect a Literal at From value, received $from")
                    }
                    when (val to = getValueFromArg(toArg)) {
                        is Rex.Lit -> Unit
                        else -> throw LakeFormationQuerySemanticException("Lake Formation Row Filter, expect a Literal at To value, received $to")
                    }
                }

                "like" -> {
                    val args = node.args
                    val lhsArg = args.getOrNull(0) ?: throw LakeFormationQuerySemanticException("no column name provided for operator like")
                    val patternArg = args.getOrNull(1) ?: throw LakeFormationQuerySemanticException("like operator does not have pattern value")
                    val escapeArg = args.getOrNull(1)
                    checkLhsIsId(lhsArg, node.id)
                    when (val pattern = getValueFromArg(patternArg)) {
                        is Rex.Lit -> {
                            if (pattern.type !is StringType) {
                                throw LakeFormationQuerySemanticException("Lake Formation Row Filter, expect a String as pattern, received $pattern")
                            }
                        }
                        else -> throw LakeFormationQuerySemanticException("Lake Formation Row Filter, expect a Literal as pattern, received $pattern")
                    }

                    if (escapeArg != null) {
                        val escape = getValueFromArg(escapeArg)
                        if (escape is Rex.Lit) {
                            if (escape.type !is StringType) {
                                throw LakeFormationQuerySemanticException("Lake Formation Row Filter, expect a string as escape, received $escape")
                            }
                        } else {
                            throw LakeFormationQuerySemanticException("Lake Formation Row Filter, expect a Literal as escape, received $escape")
                        }
                    }
                }

                "in_collection" -> {
                    val args = node.args
                    val lhsArg = args.getOrNull(0) ?: throw LakeFormationQuerySemanticException("no column name provided for operator in")
                    val rhsArg = args.getOrNull(1) ?: throw LakeFormationQuerySemanticException("no column value provided for operator in")
                    checkLhsIsId(lhsArg, node.id)
                    when (val rhs = getValueFromArg(rhsArg)) {
                        is Rex.Collection.Array -> {
                            if (rhs.values.any { it !is Rex.Lit }) {
                                throw LakeFormationQuerySemanticException("Lake Formation Row Filter, in operator rhs should be an array contains all literal value. Received $rhs")
                            }
                        }
                        else -> throw LakeFormationQuerySemanticException("Lake Formation Row Filter, in operator rhs should be an array. Received $rhs")
                    }
                }

                else -> {
                    throw LakeFormationQuerySemanticException("Lake Formation Row Filter, ${node.id} is not supported")
                }
            }
        }

        private fun checkLhsIsId(lhs: Arg, op: String) {
            when (val lhsValue = getValueFromArg(lhs)) {
                is Rex.Id -> Unit
                else -> throw LakeFormationQuerySemanticException("Lake Formation Row Filter, expect a Identifier at lhs of $op comparator, received $lhsValue")
            }
        }

        private fun getValueFromArg(arg: Arg) =
            ((arg as? Arg.Value) ?: error("not value")).value
    }
}
