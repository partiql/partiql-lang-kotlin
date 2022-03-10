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
     * PIG AST is first transformed into a recursive tree structure, FeatureTree, then it is pretty printed.
     */
    fun prettyPrintAST(ast: PartiqlAst.Statement): String {
        val featureTree = when (ast) {
            is PartiqlAst.Statement.Query -> {
                toFeatureTree(ast.expr)
            }
            is PartiqlAst.Statement.Dml -> TODO()
            is PartiqlAst.Statement.Ddl -> TODO()
            is PartiqlAst.Statement.Exec -> TODO()
        }

        return featureTree.convertToString()
    }

    private fun toFeatureTree(node: PartiqlAst.Expr, attrOfParent: String? = null): FeatureTree {
        return when (node) {
            is PartiqlAst.Expr.Id -> FeatureTree(
                astType = "Id",
                value = node.name.text,
                attrOfParent = attrOfParent
            )
            is PartiqlAst.Expr.Missing -> FeatureTree(
                astType = "missing",
                attrOfParent = attrOfParent
            )
            is PartiqlAst.Expr.Lit -> FeatureTree(
                astType = "Lit",
                value = node.value.toString(),
                attrOfParent = attrOfParent
            )
            is PartiqlAst.Expr.Parameter -> FeatureTree(
                astType = "Parameter",
                value = node.index.value.toString(),
                attrOfParent = attrOfParent
            )
            is PartiqlAst.Expr.Date -> FeatureTree(
                astType = "Date",
                value = node.year.value.toString() + "-" + node.month.value.toString() + "-" + node.day.value.toString(),
                attrOfParent = attrOfParent
            )
            is PartiqlAst.Expr.LitTime -> FeatureTree(
                astType = "LitTime",
                value = node.value.hour.value.toString() +
                        ":" + node.value.minute.value.toString() +
                        ":" + node.value.second.toString() +
                        "." + node.value.nano.toString() +
                        " 'precision': " + node.value.precision.value.toString() +
                        " 'timeZone': " + node.value.withTimeZone.toString(),
                attrOfParent = attrOfParent,
            )
            is PartiqlAst.Expr.Not -> FeatureTree(
                astType = "Not",
                attrOfParent = attrOfParent,
                children = listOf(toFeatureTree(node.expr))
            )
            is PartiqlAst.Expr.Pos -> FeatureTree(
                astType = "+",
                attrOfParent = attrOfParent,
                children = listOf(toFeatureTree(node.expr))
            )
            is PartiqlAst.Expr.Neg -> FeatureTree(
                astType = "-",
                attrOfParent = attrOfParent,
                children = listOf(toFeatureTree(node.expr))
            )
            is PartiqlAst.Expr.Plus -> FeatureTree(
                astType = "+",
                attrOfParent = attrOfParent,
                children = toFeatureTreeList(node.operands)
            )
            is PartiqlAst.Expr.Minus -> FeatureTree(
                astType = "-",
                attrOfParent = attrOfParent,
                children = toFeatureTreeList(node.operands)
            )
            is PartiqlAst.Expr.Times -> FeatureTree(
                astType = "*",
                attrOfParent = attrOfParent,
                children = toFeatureTreeList(node.operands)
            )
            is PartiqlAst.Expr.Divide -> FeatureTree(
                astType = "/",
                attrOfParent = attrOfParent,
                children = toFeatureTreeList(node.operands)
            )
            is PartiqlAst.Expr.Modulo -> FeatureTree(
                astType = "%",
                attrOfParent = attrOfParent,
                children = toFeatureTreeList(node.operands)
            )
            is PartiqlAst.Expr.Concat -> FeatureTree(
                astType = "||",
                attrOfParent = attrOfParent,
                children = toFeatureTreeList(node.operands)
            )
            is PartiqlAst.Expr.And -> FeatureTree(
                astType = "and",
                attrOfParent = attrOfParent,
                children = toFeatureTreeList(node.operands)
            )
            is PartiqlAst.Expr.Or -> FeatureTree(
                astType = "or",
                attrOfParent = attrOfParent,
                children = toFeatureTreeList(node.operands)
            )
            is PartiqlAst.Expr.Eq -> FeatureTree(
                astType = "=",
                attrOfParent = attrOfParent,
                children = toFeatureTreeList(node.operands)
            )
            is PartiqlAst.Expr.Ne -> FeatureTree(
                astType = "!=",
                attrOfParent = attrOfParent,
                children = toFeatureTreeList(node.operands)
            )
            is PartiqlAst.Expr.Gt -> FeatureTree(
                astType = ">",
                attrOfParent = attrOfParent,
                children = toFeatureTreeList(node.operands)
            )
            is PartiqlAst.Expr.Gte -> FeatureTree(
                astType = ">=",
                attrOfParent = attrOfParent,
                children = toFeatureTreeList(node.operands)
            )
            is PartiqlAst.Expr.Lt -> FeatureTree(
                astType = "<",
                attrOfParent = attrOfParent,
                children = toFeatureTreeList(node.operands)
            )
            is PartiqlAst.Expr.Lte -> FeatureTree(
                astType = "<=",
                attrOfParent = attrOfParent,
                children = toFeatureTreeList(node.operands)
            )
            is PartiqlAst.Expr.InCollection -> FeatureTree(
                astType = "in",
                attrOfParent = attrOfParent,
                children = toFeatureTreeList(node.operands)
            )
            is PartiqlAst.Expr.Union -> FeatureTree(
                astType = "Union",
                attrOfParent = attrOfParent,
                children = toFeatureTreeList(node.operands)
            )
            is PartiqlAst.Expr.Except -> FeatureTree(
                astType = "Except",
                attrOfParent = attrOfParent,
                children = toFeatureTreeList(node.operands)
            )
            is PartiqlAst.Expr.Intersect -> FeatureTree(
                astType = "Intersect",
                attrOfParent = attrOfParent,
                children = toFeatureTreeList(node.operands)
            )
            is PartiqlAst.Expr.Like -> FeatureTree(
                astType = "Like",
                attrOfParent = attrOfParent,
                children = listOf(
                    toFeatureTree(node.value, "value"),
                    toFeatureTree(node.pattern, "pattern")
                ).let {
                    if (node.escape == null) it else (it + listOf(toFeatureTree(node.escape!!, "escape")))
                }
            )
            is PartiqlAst.Expr.Between -> FeatureTree(
                astType = "Between",
                attrOfParent = attrOfParent,
                children = listOf(
                    toFeatureTree(node.value, "value"),
                    toFeatureTree(node.from, "from"),
                    toFeatureTree(node.to, "to")
                )
            )
            is PartiqlAst.Expr.SimpleCase -> FeatureTree(
                astType = "SimpleCase",
                attrOfParent = attrOfParent,
                children = listOf(
                    toFeatureTree(node.expr, "expr")
                ) + toFeatureTreeList(node.cases, "case").let {
                    if (node.default == null) it else (it.plusElement(toFeatureTree(node.default!!, "default")))
                }
            )
            is PartiqlAst.Expr.SearchedCase -> FeatureTree(
                astType = "SearchedCase",
                attrOfParent = attrOfParent,
                children = toFeatureTreeList(node.cases, "case").let {
                    if (node.default == null) it else (it.plusElement(toFeatureTree(node.default!!, "default")))
                }
            )
            is PartiqlAst.Expr.Struct -> FeatureTree(
                astType = "{}",
                attrOfParent = attrOfParent,
                children = node.fields.map { toFeatureTree(it, "field") }
            )
            is PartiqlAst.Expr.Bag -> FeatureTree(
                astType = "<<>>",
                attrOfParent = attrOfParent,
                children = toFeatureTreeList(node.values, "value")
            )
            is PartiqlAst.Expr.List -> FeatureTree(
                astType = "[]",
                attrOfParent = attrOfParent,
                children = toFeatureTreeList(node.values, "value")
            )
            is PartiqlAst.Expr.Sexp -> FeatureTree(
                astType = "()",
                attrOfParent = attrOfParent,
                children = toFeatureTreeList(node.values, "value")
            )
            is PartiqlAst.Expr.Path -> FeatureTree(
                astType = "Path",
                attrOfParent = attrOfParent,
                children = listOf(toFeatureTree(node.root, "root")) + node.steps.map { toFeatureTree(it, "step") }
            )
            is PartiqlAst.Expr.Call -> FeatureTree(
                astType = "Call",
                value = node.funcName.text,
                attrOfParent = attrOfParent,
                children = toFeatureTreeList(node.args, "arg")
            )
            is PartiqlAst.Expr.CallAgg -> FeatureTree(
                astType = "CallAgg: ",
                value = node.funcName.text,
                attrOfParent = attrOfParent,
                children = listOf(toFeatureTree(node.arg, "arg"))
            )
            is PartiqlAst.Expr.IsType -> FeatureTree(
                astType = "Is",
                attrOfParent = attrOfParent,
                children = listOf(
                    toFeatureTree(node.value, "value"),
                    FeatureTree(
                        astType = node.type.toString(),
                        attrOfParent = "type"
                    )
                )
            )
            is PartiqlAst.Expr.Cast -> FeatureTree(
                astType = "Cast",
                attrOfParent = attrOfParent,
                children = listOf(
                    toFeatureTree(node.value, "value"),
                    FeatureTree(
                        astType = node.asType.toString(),
                        attrOfParent = "asType"
                    )
                )
            )
            is PartiqlAst.Expr.CanCast -> FeatureTree(
                astType = "CanCast",
                attrOfParent = attrOfParent,
                children = listOf(
                    toFeatureTree(node.value, "value"),
                    FeatureTree(
                        astType = node.asType.toString(),
                        attrOfParent = "asType"
                    )
                )
            )
            is PartiqlAst.Expr.CanLosslessCast -> FeatureTree(
                astType = "CanLosslessCast",
                attrOfParent = attrOfParent,
                children = listOf(
                    toFeatureTree(node.value, "value"),
                    FeatureTree(
                        astType = node.asType.toString(),
                        attrOfParent = "asType"
                    )
                )
            )
            is PartiqlAst.Expr.NullIf -> FeatureTree(
                astType = "NullIf",
                attrOfParent = attrOfParent,
                children = listOf(
                    toFeatureTree(node.expr1, "expr1"),
                    toFeatureTree(node.expr2, "expr2")
                )
            )
            is PartiqlAst.Expr.Coalesce -> FeatureTree(
                astType = "Coalesce",
                attrOfParent = attrOfParent,
                children = toFeatureTreeList(node.args, "arg")
            )
            is PartiqlAst.Expr.Select -> FeatureTree(
                astType = "Select",
                attrOfParent = attrOfParent,
                children = listOf(
                    toFeatureTree(node.project, "project"),
                    toFeatureTree(node.from, "from")
                ).let {
                    if (node.fromLet == null) it else (it.plusElement(toFeatureTree(node.fromLet!!, "let")))
                }.let {
                    if (node.where == null) it else (it.plusElement(toFeatureTree(node.where!!, "where")))
                }.let {
                    if (node.group == null) it else (it.plusElement(toFeatureTree(node.group!!, "group")))
                }.let {
                    if (node.having == null) it else (it.plusElement(toFeatureTree(node.having!!, "having")))
                }.let {
                    if (node.order == null) it else (it.plusElement(toFeatureTree(node.order!!, "order")))
                }.let {
                    if (node.limit == null) it else (it.plusElement(toFeatureTree(node.limit!!, "limit")))
                }.let {
                    if (node.offset == null) it else (it.plusElement(toFeatureTree(node.offset!!, "offset")))
                }
            )
        }
    }

    private fun toFeatureTreeList(nodes: List<PartiqlAst.Expr>, attrOfParent: String? = null): List<FeatureTree> =
        nodes.map { toFeatureTree(it, attrOfParent) }

    private fun toFeatureTree(node: PartiqlAst.ExprPair, attrOfParent: String? = null): FeatureTree =
        FeatureTree(
            astType = "pair",
            attrOfParent = attrOfParent,
            children = listOf(
                toFeatureTree(node.first, "first"),
                toFeatureTree(node.second, "second")
            )
        )

    private fun toFeatureTreeList(node: PartiqlAst.ExprPairList, attrOfParent: String? = null): List<FeatureTree> =
        node.pairs.map { toFeatureTree(it, attrOfParent) }

    private fun toFeatureTree(node: PartiqlAst.PathStep, attrOfParent: String? = null): FeatureTree =
        when (node) {
            is PartiqlAst.PathStep.PathExpr -> toFeatureTree(node.index, attrOfParent)
            is PartiqlAst.PathStep.PathWildcard -> FeatureTree(
                astType = "[*]",
                attrOfParent = attrOfParent
            )
            is PartiqlAst.PathStep.PathUnpivot -> FeatureTree(
                astType = "*",
                attrOfParent = attrOfParent
            )
        }

    private fun toFeatureTree(node: PartiqlAst.Projection, attrOfParent: String? = null): FeatureTree =
        when (node) {
            is PartiqlAst.Projection.ProjectStar -> FeatureTree(
                astType = "*",
                attrOfParent = attrOfParent
            )
            is PartiqlAst.Projection.ProjectValue -> FeatureTree(
                astType = "ProjectValue",
                attrOfParent = attrOfParent,
                children = listOf(toFeatureTree(node.value, "value"))
            )
            is PartiqlAst.Projection.ProjectList -> FeatureTree(
                astType = "ProjectList",
                attrOfParent = attrOfParent,
                children = node.projectItems.map { toFeatureTree(it, "projectItem") }
            )
            is PartiqlAst.Projection.ProjectPivot -> FeatureTree(
                astType = "ProjectPivot",
                attrOfParent = attrOfParent,
                children = listOf(
                    toFeatureTree(node.value, "value"),
                    toFeatureTree(node.key, "key")
                )
            )
        }

    private fun toFeatureTree(node: PartiqlAst.ProjectItem, attrOfParent: String? = null): FeatureTree =
        when (node) {
            is PartiqlAst.ProjectItem.ProjectAll -> FeatureTree(
                astType = "ProjectAll",
                attrOfParent = attrOfParent,
                children = listOf(toFeatureTree(node.expr, "expr"))
            )
            is PartiqlAst.ProjectItem.ProjectExpr -> FeatureTree(
                astType = "ProjectExpr",
                attrOfParent = attrOfParent,
                children = listOf(toFeatureTree(node.expr, "expr")).let {
                    if (node.asAlias == null) it else { it.plusElement(toFeatureTree(node.asAlias!!,"as")) }
                }
            )
        }

    private fun toFeatureTree(node: PartiqlAst.FromSource, attrOfParent: String? = null): FeatureTree = when (node) {
        is PartiqlAst.FromSource.Join -> FeatureTree(
            astType = node.type.toString(),
            attrOfParent = attrOfParent,
            children = listOf(
                toFeatureTree(node.left, "left"),
                toFeatureTree(node.right, "right")
            ).let {
                if (node.predicate == null) it else { it.plusElement(toFeatureTree(node.predicate!!, "on")) }
            }
        )
        is PartiqlAst.FromSource.Scan -> FeatureTree(
            astType = "Scan",
            attrOfParent = attrOfParent,
            children = listOf(toFeatureTree(node.expr)).let {
                if (node.asAlias == null) it else { it.plusElement(toFeatureTree(node.asAlias!!, attrOfParent = "as")) }
            }.let {
                if (node.atAlias == null) it else { it.plusElement(toFeatureTree(node.atAlias!!, attrOfParent = "at")) }
            }.let {
                if (node.byAlias == null) it else { it.plusElement(toFeatureTree(node.byAlias!!, attrOfParent = "by")) }
            }
        )
        is PartiqlAst.FromSource.Unpivot -> FeatureTree(
            astType = "Unpivot",
            attrOfParent = attrOfParent,
            children = listOf(toFeatureTree(node.expr)).let {
                if (node.asAlias == null) it else { it.plusElement(toFeatureTree(node.asAlias!!, attrOfParent = "as")) }
            }.let {
                if (node.atAlias == null) it else { it.plusElement(toFeatureTree(node.atAlias!!, attrOfParent = "at")) }
            }.let {
                if (node.byAlias == null) it else { it.plusElement(toFeatureTree(node.byAlias!!, attrOfParent = "by")) }
            }
        )
    }

    private fun toFeatureTree(node: PartiqlAst.Let, attrOfParent: String? = null): FeatureTree = FeatureTree(
        astType = "Let",
        attrOfParent = attrOfParent,
        children = node.letBindings.map { toFeatureTree(it) }
    )

    private fun toFeatureTree(node: PartiqlAst.LetBinding, attrOfParent: String? = null): FeatureTree = FeatureTree(
        astType = "LetBinding",
        attrOfParent = attrOfParent,
        children = listOf(
            toFeatureTree(node.expr, "expr"),
            toFeatureTree(node.name, "name")
        )
    )

    private fun toFeatureTree(node: PartiqlAst.GroupBy, attrOfParent: String? = null): FeatureTree = FeatureTree(
        astType = "Group",
        attrOfParent = attrOfParent,
        children = listOf(
            FeatureTree(
                astType = when(node.strategy){
                    is PartiqlAst.GroupingStrategy.GroupFull -> "GroupFull"
                    is PartiqlAst.GroupingStrategy.GroupPartial -> "GroupPartial"
                },
                attrOfParent = "strategy"
            ),
            FeatureTree(
                astType = "GroupKeyList",
                attrOfParent = "keyList",
                children = node.keyList.keys.map { groupKey ->
                    FeatureTree(
                        astType = "GroupKey",
                        attrOfParent = "key",
                        children = listOf(toFeatureTree(groupKey.expr, "expr")).let {
                            if (groupKey.asAlias == null) it else { it.plusElement(toFeatureTree(groupKey.asAlias!!, "as")) }
                        }
                    )
                }
            )
        ).let {
            if (node.groupAsAlias == null) it else { it.plusElement(toFeatureTree(node.groupAsAlias!!, attrOfParent = "groupAs")) }
        }
    )

    private fun toFeatureTree(node: PartiqlAst.OrderBy, attrOfParent: String? = null): FeatureTree = FeatureTree(
        astType = "Order",
        attrOfParent = attrOfParent,
        children = node.sortSpecs.map {
            FeatureTree(
                astType = "SortSpec",
                attrOfParent = "sortSpec",
                children = listOf(
                    toFeatureTree(it.expr, "expr"),
                    FeatureTree(
                        astType = it.orderingSpec.toString(),
                        attrOfParent = "orderingSpec"
                    )
                )
            )
        }
    )

    private fun toFeatureTree(symbol: SymbolPrimitive, attrOfParent: String? = null): FeatureTree = FeatureTree(
        astType = "Symbol",
        value = symbol.text,
        attrOfParent = attrOfParent
    )
}