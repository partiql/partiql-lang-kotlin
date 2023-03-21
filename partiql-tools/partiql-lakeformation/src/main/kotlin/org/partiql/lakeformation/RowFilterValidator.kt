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

class RowFilterValidator(val parser: Parser) {

    // Lake Formation restriction.
    // See: https://docs.aws.amazon.com/lake-formation/latest/APIReference/API_RowFilter.html
    private val ROW_FILTER_EXPRESSION_MAX_LENGTH = 2047
    private val ROW_FILTER_EXPRESSION_PATTERN: Pattern = Pattern.compile("^[^\\s].*[^\\s;]$")

    fun validate(whereClause: String) {
        validatePredicateSyntax(whereClause)
        validateRowFilterExpression(whereClause)
    }

    private fun validatePredicateSyntax(rowFilterExpression: String) {
        val plan = Planner.plan(query = rowFilterExpression)
        plan.accept(LakeFormationSyntaxValidation, Unit)
    }

    private fun validateRowFilterExpression(rowFilterExpression: String) {
        if (rowFilterExpression.length > ROW_FILTER_EXPRESSION_MAX_LENGTH) {
            error(
                "Row filter expression length must be less than or equal to $ROW_FILTER_EXPRESSION_MAX_LENGTH"
            )
        } else if (!ROW_FILTER_EXPRESSION_PATTERN.matcher(rowFilterExpression).matches()) {
            error(
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
                is Rex.Switch, is Rex.Tuple -> throw LakeFormationQueryUnsupportedException("$node not supported by Lake Formation")
            }
        }

        override fun visitRexBinary(node: Rex.Binary, ctx: Unit) {
            when (node.op) {
                Rex.Binary.Op.PLUS, Rex.Binary.Op.MINUS,
                Rex.Binary.Op.TIMES, Rex.Binary.Op.DIV,
                Rex.Binary.Op.MODULO, Rex.Binary.Op.CONCAT -> error("${node.op.name} not supported")
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
                        else -> throw LakeFormationQuerySemanticException("simple expression left side not column name")
                    }
                    when (node.rhs) {
                        is Rex.Lit -> Unit
                        is Rex.Collection.Array -> throw LakeFormationQuerySemanticException("comparison with list not allowed now")
                        else -> throw LakeFormationQuerySemanticException("simple expression left side not literal")
                    }
                }
            }
        }

        override fun visitRexCall(node: Rex.Call, ctx: Unit) {
            when (node.id) {
                "between" -> {
                    val args = node.args
                    val lhs = args.getOrNull(0) ?: throw LakeFormationQuerySemanticException("no column name provided for operator between")
                    val rhsFrom = args.getOrNull(1) ?: throw LakeFormationQuerySemanticException("between operator does not have from value")
                    val rhsTo = args.getOrNull(1) ?: throw LakeFormationQuerySemanticException("between operator does not have from value")
                    when (getValueFromArg(lhs)) {
                        is Rex.Id -> Unit
                        else -> throw LakeFormationQuerySemanticException("simple expression left side not column name")
                    }
                    when (getValueFromArg(rhsFrom)) {
                        is Rex.Lit -> Unit
                        else -> throw LakeFormationQuerySemanticException("between operation From value not literal")
                    }
                    when (getValueFromArg(rhsTo)) {
                        is Rex.Lit -> Unit
                        else -> throw LakeFormationQuerySemanticException("between operation From value not literal")
                    }
                }

                "like" -> {
                    val args = node.args
                    val lhs = args.getOrNull(0) ?: throw LakeFormationQuerySemanticException("no column name provided for operator like")
                    val rhsPattern = args.getOrNull(1) ?: throw LakeFormationQuerySemanticException("like operator does not have pattern value")
                    val rhsEscape = args.getOrNull(1)
                    when (getValueFromArg(lhs)) {
                        is Rex.Id -> Unit
                        else -> throw LakeFormationQuerySemanticException("simple expression left side not column name")
                    }
                    when (val rhsPatternValue = getValueFromArg(rhsPattern)) {
                        is Rex.Lit -> {
                            if (rhsPatternValue.type !is StringType) {
                                throw LakeFormationQuerySemanticException("pattern not a string")
                            }
                        }

                        else -> throw LakeFormationQuerySemanticException("between operation From value not literal")
                    }
                    if (rhsEscape == null) {
                        Unit
                    } else {
                        if (getValueFromArg(rhsPattern) is Rex.Lit) {
                            if ((getValueFromArg(rhsPattern) as Rex.Lit).type is StringType) {
                                Unit
                            }
                        } else {
                            throw LakeFormationQuerySemanticException("not a lit")
                        }
                    }
                }
                // <column_name> in <column value>
                "in_collection" -> {
                    val args = node.args
                    val lhs = args.getOrNull(0) ?: throw LakeFormationQuerySemanticException("no column name provided for operator in")
                    val rhs = args.getOrNull(1) ?: throw LakeFormationQuerySemanticException("no column value provided for operator in")
                    when (getValueFromArg(lhs)) {
                        is Rex.Id -> Unit
                        else -> throw LakeFormationQuerySemanticException("simple expression left side not column name")
                    }
                    when (val rhsValue = getValueFromArg(rhs)) {
                        is Rex.Collection.Array -> {
                            if (rhsValue.values.any { it !is Rex.Lit }) {
                                throw LakeFormationQuerySemanticException("array contains non-literal value")
                            }
                        }
                        else -> throw LakeFormationQuerySemanticException("in operation rhs not array")
                    }
                }

                else -> {
                    throw LakeFormationQueryUnsupportedException("function ${node.id} is not supported")
                }
            }
        }

        private fun getValueFromArg(arg: Arg) =
            ((arg as? Arg.Value) ?: error("not value")).value
    }
}
