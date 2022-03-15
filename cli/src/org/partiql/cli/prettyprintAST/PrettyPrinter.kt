package org.partiql.cli.prettyprintAST

import com.amazon.ion.system.IonSystemBuilder
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.syntax.SqlParser
import org.partiql.pig.runtime.SymbolPrimitive

/**
 * This class is used to pretty print PIG AST.
 */
class PrettyPrinter {
    fun prettyPrintAST(query: String): String {
        val ion = IonSystemBuilder.standard().build()
        val ast = SqlParser(ion).parseAstStatement(query)

        return prettyPrintAST(ast)
    }

    /**
     * PIG AST is first transformed into a recursive tree structure, RecursionTree, then it is pretty printed.
     */
    fun prettyPrintAST(ast: PartiqlAst.Statement): String {
        val recursionTree = when (ast) {
            is PartiqlAst.Statement.Query -> {
                toRecursionTree(ast.expr)
            }
            is PartiqlAst.Statement.Dml -> TODO()
            is PartiqlAst.Statement.Ddl -> TODO()
            is PartiqlAst.Statement.Exec -> TODO()
        }

        return recursionTree.convertToString()
    }

    private fun toRecursionTree(node: PartiqlAst.Expr, attrOfParent: String? = null): RecursionTree =
        when (node) {
            is PartiqlAst.Expr.Id -> RecursionTree(
                astType = "Id",
                value = node.name.text,
                attrOfParent = attrOfParent
            )
            is PartiqlAst.Expr.Missing -> RecursionTree(
                astType = "missing",
                attrOfParent = attrOfParent
            )
            is PartiqlAst.Expr.Lit -> RecursionTree(
                astType = "Lit",
                value = node.value.toString(),
                attrOfParent = attrOfParent
            )
            is PartiqlAst.Expr.Parameter -> RecursionTree(
                astType = "Parameter",
                value = node.index.value.toString(),
                attrOfParent = attrOfParent
            )
            is PartiqlAst.Expr.Date -> RecursionTree(
                astType = "Date",
                value = node.year.value.toString() + "-" + node.month.value.toString() + "-" + node.day.value.toString(),
                attrOfParent = attrOfParent
            )
            is PartiqlAst.Expr.LitTime -> RecursionTree(
                astType = "LitTime",
                value = node.value.hour.value.toString() +
                    ":" + node.value.minute.value.toString() +
                    ":" + node.value.second.toString() +
                    "." + node.value.nano.toString() +
                    " 'precision': " + node.value.precision.value.toString() +
                    " 'timeZone': " + node.value.withTimeZone.toString(),
                attrOfParent = attrOfParent,
            )
            is PartiqlAst.Expr.Not -> RecursionTree(
                astType = "Not",
                attrOfParent = attrOfParent,
                children = listOf(toRecursionTree(node.expr))
            )
            is PartiqlAst.Expr.Pos -> RecursionTree(
                astType = "+",
                attrOfParent = attrOfParent,
                children = listOf(toRecursionTree(node.expr))
            )
            is PartiqlAst.Expr.Neg -> RecursionTree(
                astType = "-",
                attrOfParent = attrOfParent,
                children = listOf(toRecursionTree(node.expr))
            )
            is PartiqlAst.Expr.Plus -> RecursionTree(
                astType = "+",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.operands)
            )
            is PartiqlAst.Expr.Minus -> RecursionTree(
                astType = "-",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.operands)
            )
            is PartiqlAst.Expr.Times -> RecursionTree(
                astType = "*",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.operands)
            )
            is PartiqlAst.Expr.Divide -> RecursionTree(
                astType = "/",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.operands)
            )
            is PartiqlAst.Expr.Modulo -> RecursionTree(
                astType = "%",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.operands)
            )
            is PartiqlAst.Expr.Concat -> RecursionTree(
                astType = "||",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.operands)
            )
            is PartiqlAst.Expr.And -> RecursionTree(
                astType = "and",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.operands)
            )
            is PartiqlAst.Expr.Or -> RecursionTree(
                astType = "or",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.operands)
            )
            is PartiqlAst.Expr.Eq -> RecursionTree(
                astType = "=",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.operands)
            )
            is PartiqlAst.Expr.Ne -> RecursionTree(
                astType = "!=",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.operands)
            )
            is PartiqlAst.Expr.Gt -> RecursionTree(
                astType = ">",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.operands)
            )
            is PartiqlAst.Expr.Gte -> RecursionTree(
                astType = ">=",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.operands)
            )
            is PartiqlAst.Expr.Lt -> RecursionTree(
                astType = "<",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.operands)
            )
            is PartiqlAst.Expr.Lte -> RecursionTree(
                astType = "<=",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.operands)
            )
            is PartiqlAst.Expr.InCollection -> RecursionTree(
                astType = "in",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.operands)
            )
            is PartiqlAst.Expr.Union -> RecursionTree(
                astType = "Union",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.operands)
            )
            is PartiqlAst.Expr.Except -> RecursionTree(
                astType = "Except",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.operands)
            )
            is PartiqlAst.Expr.Intersect -> RecursionTree(
                astType = "Intersect",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.operands)
            )
            is PartiqlAst.Expr.Like -> RecursionTree(
                astType = "Like",
                attrOfParent = attrOfParent,
                children = listOf(
                    toRecursionTree(node.value, "value"),
                    toRecursionTree(node.pattern, "pattern")
                ).let {
                    if (node.escape == null) it else (it + listOf(toRecursionTree(node.escape!!, "escape")))
                }
            )
            is PartiqlAst.Expr.Between -> RecursionTree(
                astType = "Between",
                attrOfParent = attrOfParent,
                children = listOf(
                    toRecursionTree(node.value, "value"),
                    toRecursionTree(node.from, "from"),
                    toRecursionTree(node.to, "to")
                )
            )
            is PartiqlAst.Expr.SimpleCase -> RecursionTree(
                astType = "SimpleCase",
                attrOfParent = attrOfParent,
                children = listOf(
                    toRecursionTree(node.expr, "expr")
                ) + toRecursionTreeList(node.cases, "case").let {
                    if (node.default == null) it else (it.plusElement(toRecursionTree(node.default!!, "default")))
                }
            )
            is PartiqlAst.Expr.SearchedCase -> RecursionTree(
                astType = "SearchedCase",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.cases, "case").let {
                    if (node.default == null) it else (it.plusElement(toRecursionTree(node.default!!, "default")))
                }
            )
            is PartiqlAst.Expr.Struct -> RecursionTree(
                astType = "{}",
                attrOfParent = attrOfParent,
                children = node.fields.map { toRecursionTree(it, "field") }
            )
            is PartiqlAst.Expr.Bag -> RecursionTree(
                astType = "<<>>",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.values, "value")
            )
            is PartiqlAst.Expr.List -> RecursionTree(
                astType = "[]",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.values, "value")
            )
            is PartiqlAst.Expr.Sexp -> RecursionTree(
                astType = "()",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.values, "value")
            )
            is PartiqlAst.Expr.Path -> RecursionTree(
                astType = "Path",
                attrOfParent = attrOfParent,
                children = listOf(toRecursionTree(node.root, "root")) + node.steps.map { toRecursionTree(it, "step") }
            )
            is PartiqlAst.Expr.Call -> RecursionTree(
                astType = "Call",
                value = node.funcName.text,
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.args, "arg")
            )
            is PartiqlAst.Expr.CallAgg -> RecursionTree(
                astType = "CallAgg: ",
                value = node.funcName.text,
                attrOfParent = attrOfParent,
                children = listOf(toRecursionTree(node.arg, "arg"))
            )
            is PartiqlAst.Expr.IsType -> RecursionTree(
                astType = "Is",
                attrOfParent = attrOfParent,
                children = listOf(
                    toRecursionTree(node.value, "value"),
                    RecursionTree(
                        astType = node.type.toString(),
                        attrOfParent = "type"
                    )
                )
            )
            is PartiqlAst.Expr.Cast -> RecursionTree(
                astType = "Cast",
                attrOfParent = attrOfParent,
                children = listOf(
                    toRecursionTree(node.value, "value"),
                    RecursionTree(
                        astType = node.asType.toString(),
                        attrOfParent = "asType"
                    )
                )
            )
            is PartiqlAst.Expr.CanCast -> RecursionTree(
                astType = "CanCast",
                attrOfParent = attrOfParent,
                children = listOf(
                    toRecursionTree(node.value, "value"),
                    RecursionTree(
                        astType = node.asType.toString(),
                        attrOfParent = "asType"
                    )
                )
            )
            is PartiqlAst.Expr.CanLosslessCast -> RecursionTree(
                astType = "CanLosslessCast",
                attrOfParent = attrOfParent,
                children = listOf(
                    toRecursionTree(node.value, "value"),
                    RecursionTree(
                        astType = node.asType.toString(),
                        attrOfParent = "asType"
                    )
                )
            )
            is PartiqlAst.Expr.NullIf -> RecursionTree(
                astType = "NullIf",
                attrOfParent = attrOfParent,
                children = listOf(
                    toRecursionTree(node.expr1, "expr1"),
                    toRecursionTree(node.expr2, "expr2")
                )
            )
            is PartiqlAst.Expr.Coalesce -> RecursionTree(
                astType = "Coalesce",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.args, "arg")
            )
            is PartiqlAst.Expr.Select -> RecursionTree(
                astType = "Select",
                attrOfParent = attrOfParent,
                children = listOf(
                    toRecursionTree(node.project, "project"),
                    toRecursionTree(node.from, "from")
                ).let {
                    if (node.fromLet == null) it else (it.plusElement(toRecursionTree(node.fromLet!!, "let")))
                }.let {
                    if (node.where == null) it else (it.plusElement(toRecursionTree(node.where!!, "where")))
                }.let {
                    if (node.group == null) it else (it.plusElement(toRecursionTree(node.group!!, "group")))
                }.let {
                    if (node.having == null) it else (it.plusElement(toRecursionTree(node.having!!, "having")))
                }.let {
                    if (node.order == null) it else (it.plusElement(toRecursionTree(node.order!!, "order")))
                }.let {
                    if (node.limit == null) it else (it.plusElement(toRecursionTree(node.limit!!, "limit")))
                }.let {
                    if (node.offset == null) it else (it.plusElement(toRecursionTree(node.offset!!, "offset")))
                }
            )
        }

    private fun toRecursionTreeList(nodes: List<PartiqlAst.Expr>, attrOfParent: String? = null): List<RecursionTree> =
        nodes.map { toRecursionTree(it, attrOfParent) }

    private fun toRecursionTree(node: PartiqlAst.ExprPair, attrOfParent: String? = null): RecursionTree =
        RecursionTree(
            astType = "pair",
            attrOfParent = attrOfParent,
            children = listOf(
                toRecursionTree(node.first, "first"),
                toRecursionTree(node.second, "second")
            )
        )

    private fun toRecursionTreeList(node: PartiqlAst.ExprPairList, attrOfParent: String? = null): List<RecursionTree> =
        node.pairs.map { toRecursionTree(it, attrOfParent) }

    private fun toRecursionTree(node: PartiqlAst.PathStep, attrOfParent: String? = null): RecursionTree =
        when (node) {
            is PartiqlAst.PathStep.PathExpr -> toRecursionTree(node.index, attrOfParent)
            is PartiqlAst.PathStep.PathWildcard -> RecursionTree(
                astType = "[*]",
                attrOfParent = attrOfParent
            )
            is PartiqlAst.PathStep.PathUnpivot -> RecursionTree(
                astType = "*",
                attrOfParent = attrOfParent
            )
        }

    private fun toRecursionTree(node: PartiqlAst.Projection, attrOfParent: String? = null): RecursionTree =
        when (node) {
            is PartiqlAst.Projection.ProjectStar -> RecursionTree(
                astType = "*",
                attrOfParent = attrOfParent
            )
            is PartiqlAst.Projection.ProjectValue -> RecursionTree(
                astType = "ProjectValue",
                attrOfParent = attrOfParent,
                children = listOf(toRecursionTree(node.value, "value"))
            )
            is PartiqlAst.Projection.ProjectList -> RecursionTree(
                astType = "ProjectList",
                attrOfParent = attrOfParent,
                children = node.projectItems.map { toRecursionTree(it, "projectItem") }
            )
            is PartiqlAst.Projection.ProjectPivot -> RecursionTree(
                astType = "ProjectPivot",
                attrOfParent = attrOfParent,
                children = listOf(
                    toRecursionTree(node.value, "value"),
                    toRecursionTree(node.key, "key")
                )
            )
        }

    private fun toRecursionTree(node: PartiqlAst.ProjectItem, attrOfParent: String? = null): RecursionTree =
        when (node) {
            is PartiqlAst.ProjectItem.ProjectAll -> RecursionTree(
                astType = "ProjectAll",
                attrOfParent = attrOfParent,
                children = listOf(toRecursionTree(node.expr, "expr"))
            )
            is PartiqlAst.ProjectItem.ProjectExpr -> RecursionTree(
                astType = "ProjectExpr",
                attrOfParent = attrOfParent,
                children = listOf(toRecursionTree(node.expr, "expr")).let {
                    if (node.asAlias == null) it else { it.plusElement(toRecursionTree(node.asAlias!!, "as")) }
                }
            )
        }

    private fun toRecursionTree(node: PartiqlAst.FromSource, attrOfParent: String? = null): RecursionTree =
        when (node) {
            is PartiqlAst.FromSource.Join -> RecursionTree(
                astType = node.type.toString(),
                attrOfParent = attrOfParent,
                children = listOf(
                    toRecursionTree(node.left, "left"),
                    toRecursionTree(node.right, "right")
                ).let {
                    if (node.predicate == null) it else { it.plusElement(toRecursionTree(node.predicate!!, "on")) }
                }
            )
            is PartiqlAst.FromSource.Scan -> RecursionTree(
                astType = "Scan",
                attrOfParent = attrOfParent,
                children = listOf(toRecursionTree(node.expr)).let {
                    if (node.asAlias == null) it else { it.plusElement(toRecursionTree(node.asAlias!!, attrOfParent = "as")) }
                }.let {
                    if (node.atAlias == null) it else { it.plusElement(toRecursionTree(node.atAlias!!, attrOfParent = "at")) }
                }.let {
                    if (node.byAlias == null) it else { it.plusElement(toRecursionTree(node.byAlias!!, attrOfParent = "by")) }
                }
            )
            is PartiqlAst.FromSource.Unpivot -> RecursionTree(
                astType = "Unpivot",
                attrOfParent = attrOfParent,
                children = listOf(toRecursionTree(node.expr)).let {
                    if (node.asAlias == null) it else { it.plusElement(toRecursionTree(node.asAlias!!, attrOfParent = "as")) }
                }.let {
                    if (node.atAlias == null) it else { it.plusElement(toRecursionTree(node.atAlias!!, attrOfParent = "at")) }
                }.let {
                    if (node.byAlias == null) it else { it.plusElement(toRecursionTree(node.byAlias!!, attrOfParent = "by")) }
                }
            )
        }

    private fun toRecursionTree(node: PartiqlAst.Let, attrOfParent: String? = null): RecursionTree =
        RecursionTree(
            astType = "Let",
            attrOfParent = attrOfParent,
            children = node.letBindings.map { toRecursionTree(it) }
        )

    private fun toRecursionTree(node: PartiqlAst.LetBinding, attrOfParent: String? = null): RecursionTree =
        RecursionTree(
            astType = "LetBinding",
            attrOfParent = attrOfParent,
            children = listOf(
                toRecursionTree(node.expr, "expr"),
                toRecursionTree(node.name, "name")
            )
        )

    private fun toRecursionTree(node: PartiqlAst.GroupBy, attrOfParent: String? = null): RecursionTree =
        RecursionTree(
            astType = "Group",
            attrOfParent = attrOfParent,
            children = listOf(
                RecursionTree(
                    astType = when (node.strategy) {
                        is PartiqlAst.GroupingStrategy.GroupFull -> "GroupFull"
                        is PartiqlAst.GroupingStrategy.GroupPartial -> "GroupPartial"
                    },
                    attrOfParent = "strategy"
                ),
                RecursionTree(
                    astType = "GroupKeyList",
                    attrOfParent = "keyList",
                    children = node.keyList.keys.map { groupKey ->
                        RecursionTree(
                            astType = "GroupKey",
                            attrOfParent = "key",
                            children = listOf(toRecursionTree(groupKey.expr, "expr")).let {
                                if (groupKey.asAlias == null) it else { it.plusElement(toRecursionTree(groupKey.asAlias!!, "as")) }
                            }
                        )
                    }
                )
            ).let {
                if (node.groupAsAlias == null) it else { it.plusElement(toRecursionTree(node.groupAsAlias!!, attrOfParent = "groupAs")) }
            }
        )

    private fun toRecursionTree(node: PartiqlAst.OrderBy, attrOfParent: String? = null): RecursionTree =
        RecursionTree(
            astType = "Order",
            attrOfParent = attrOfParent,
            children = node.sortSpecs.map {
                RecursionTree(
                    astType = "SortSpec",
                    attrOfParent = "sortSpec",
                    children = listOf(
                        toRecursionTree(it.expr, "expr"),
                        RecursionTree(
                            astType = it.orderingSpec.toString(),
                            attrOfParent = "orderingSpec"
                        )
                    )
                )
            }
        )

    private fun toRecursionTree(symbol: SymbolPrimitive, attrOfParent: String? = null): RecursionTree =
        RecursionTree(
            astType = "Symbol",
            value = symbol.text,
            attrOfParent = attrOfParent
        )
}
