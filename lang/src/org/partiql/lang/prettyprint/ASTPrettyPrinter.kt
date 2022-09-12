package org.partiql.lang.prettyprint

import com.amazon.ion.system.IonSystemBuilder
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.syntax.PartiQLParserBuilder
import org.partiql.pig.runtime.SymbolPrimitive

/**
 * This class is used to pretty print PIG AST.
 */
class ASTPrettyPrinter {
    /**
     * For the given SQL query outputs the corresponding string formatted PartiQL AST representation, e.g:
     * Given:
     *  "SELECT * FROM 1 WHERE a = b GROUP BY c HAVING d = '123' LIMIT 3 OFFSET 4"
     * Outputs:
     """
     Select
     project: *
     from: Scan
     Lit 1
     where: =
     Id a (case_insensitive) (unqualified)
     Id b (case_insensitive) (unqualified)
     group: Group
     strategy: GroupFull
     keyList: GroupKeyList
     key1: GroupKey
     expr: Id c (case_insensitive) (unqualified)
     having: =
     Id d (case_insensitive) (unqualified)
     Lit "123"
     limit: Lit 3
     offset: Lit 4
     """
     * @param query An SQL query as string.
     * @return formatted string corresponding to the input AST.
     */
    fun prettyPrintAST(query: String): String {
        val ion = IonSystemBuilder.standard().build()
        val ast = PartiQLParserBuilder().ionSystem(ion).build().parseAstStatement(query)

        return prettyPrintAST(ast)
    }

    /**
     * For the given PartiQL AST Statement, outputs a string formatted query representing the given AST, e.g:
     * @param [PartiqlAst.Statement] An SQL query as string.
     * @return formatted string corresponding to the input AST.
     */
    fun prettyPrintAST(ast: PartiqlAst.Statement): String {
        val recursionTree = when (ast) {
            is PartiqlAst.Statement.Query -> toRecursionTree(ast.expr)
            is PartiqlAst.Statement.Dml -> toRecursionTree(ast)
            is PartiqlAst.Statement.Ddl -> toRecursionTree(ast)
            is PartiqlAst.Statement.Exec -> toRecursionTree(ast)
        }

        return recursionTree.convertToString()
    }

    // ********
    // * EXEC *
    // ********
    private fun toRecursionTree(node: PartiqlAst.Statement.Exec): RecursionTree =
        RecursionTree(
            astType = "Exec",
            children = listOf(
                toRecursionTree(node.procedureName, "procedureName")
            ) + node.args.mapIndexed { index, expr -> toRecursionTree(expr, "arg${index + 1}") }
        )

    // *******
    // * DDL *
    // *******
    private fun toRecursionTree(node: PartiqlAst.Statement.Ddl): RecursionTree =
        RecursionTree(
            astType = "Ddl",
            children = listOf(
                toRecursionTree(node.op, "op")
            )
        )

    private fun toRecursionTree(node: PartiqlAst.DdlOp, attrOfParent: String? = null): RecursionTree =
        when (node) {
            is PartiqlAst.DdlOp.CreateIndex -> RecursionTree(
                astType = "CreateIndex",
                attrOfParent = attrOfParent,
                children = listOf(
                    toRecursionTree(node.indexName, "indexName")
                ) + node.fields.mapIndexed { index, expr -> toRecursionTree(expr, "field${index + 1}") }
            )
            is PartiqlAst.DdlOp.CreateTable -> RecursionTree(
                astType = "CreateTable",
                attrOfParent = attrOfParent,
                children = listOf(
                    toRecursionTree(node.tableName, "tableName")
                )
            )
            is PartiqlAst.DdlOp.DropIndex -> RecursionTree(
                astType = "DropIndex",
                attrOfParent = attrOfParent,
                children = listOf(
                    toRecursionTree(node.table, "table"),
                    toRecursionTree(node.keys, "keys")
                )
            )
            is PartiqlAst.DdlOp.DropTable -> RecursionTree(
                astType = "DropTable",
                attrOfParent = attrOfParent,
                children = listOf(
                    toRecursionTree(node.tableName, "tableName")
                )
            )
        }

    private fun toRecursionTree(node: PartiqlAst.Identifier, attrOfParent: String? = null): RecursionTree =
        RecursionTree(
            astType = "Identifier",
            value = node.name.text + " " + node.case.toString(),
            attrOfParent = attrOfParent
        )

    // *******
    // * DML *
    // *******
    private fun toRecursionTree(node: PartiqlAst.Statement.Dml): RecursionTree =
        RecursionTree(
            astType = "Dml",
            children = listOf(
                toRecursionTree(node.operations, "operations")
            ).let {
                if (node.from == null) it else (it.plusElement(toRecursionTree(node.from, "from")))
            }.let {
                if (node.where == null) it else (it.plusElement(toRecursionTree(node.where, "where")))
            }.let {
                if (node.returning == null) it else (it.plusElement(toRecursionTree(node.returning, "returning")))
            }
        )

    private fun toRecursionTree(node: PartiqlAst.DmlOpList, attrOfParent: String? = null): RecursionTree =
        RecursionTree(
            astType = "DmlOpList",
            attrOfParent = attrOfParent,
            children = node.ops.mapIndexed { index, dmlOp -> toRecursionTree(dmlOp, "op${index + 1}") }
        )

    private fun toRecursionTree(node: PartiqlAst.DmlOp, attrOfParent: String? = null): RecursionTree =
        when (node) {
            is PartiqlAst.DmlOp.Delete -> RecursionTree(
                astType = "Delete",
                attrOfParent = attrOfParent
            )
            is PartiqlAst.DmlOp.Insert -> RecursionTree(
                astType = "Insert",
                attrOfParent = attrOfParent,
                children = listOf(
                    toRecursionTree(node.target, "target"),
                    toRecursionTree(node.values, "values")
                )
            )
            is PartiqlAst.DmlOp.InsertValue -> RecursionTree(
                astType = "InsertValue",
                attrOfParent = attrOfParent,
                children = listOf(
                    toRecursionTree(node.target, "target"),
                    toRecursionTree(node.value, "value")
                ).let {
                    if (node.index == null) it else (it.plusElement(toRecursionTree(node.index, "index")))
                }.let {
                    if (node.onConflict == null) it else (it.plusElement(toRecursionTree(node.onConflict, "onConflict")))
                }
            )
            is PartiqlAst.DmlOp.Remove -> RecursionTree(
                astType = "Remove",
                attrOfParent = attrOfParent,
                children = listOf(
                    toRecursionTree(node.target, "target")
                )
            )
            is PartiqlAst.DmlOp.Set -> RecursionTree(
                astType = "Set",
                attrOfParent = attrOfParent,
                children = listOf(
                    toRecursionTree(node.assignment, "assignment")
                )
            )
        }

    private fun toRecursionTree(node: PartiqlAst.Assignment, attrOfParent: String? = null): RecursionTree =
        RecursionTree(
            astType = "Assignment",
            attrOfParent = attrOfParent,
            children = listOf(
                toRecursionTree(node.target, "target"),
                toRecursionTree(node.value, "value")
            )
        )

    private fun toRecursionTree(node: PartiqlAst.OnConflict, attrOfParent: String? = null): RecursionTree =
        RecursionTree(
            astType = "OnConflict",
            attrOfParent = attrOfParent,
            children = listOf(
                toRecursionTree(node.expr, "expr"),
                toRecursionTree(node.conflictAction, "conflictAction")
            )
        )

    private fun toRecursionTree(node: PartiqlAst.ConflictAction, attrOfParent: String? = null): RecursionTree =
        when (node) {
            is PartiqlAst.ConflictAction.DoNothing -> RecursionTree(
                astType = "DoNothing",
                attrOfParent = attrOfParent
            )
        }

    private fun toRecursionTree(node: PartiqlAst.ReturningExpr, attrOfParent: String? = null): RecursionTree =
        RecursionTree(
            astType = "ReturningExpr",
            attrOfParent = attrOfParent,
            children = node.elems.mapIndexed { index, returningElem -> toRecursionTree(returningElem, "elem${index + 1}") }
        )

    private fun toRecursionTree(node: PartiqlAst.ReturningElem, attrOfParent: String? = null): RecursionTree =
        RecursionTree(
            astType = "ReturningElem",
            attrOfParent = attrOfParent,
            children = listOf(
                toRecursionTree(node.mapping, "mapping"),
                toRecursionTree(node.column, "column")
            )
        )

    private fun toRecursionTree(node: PartiqlAst.ReturningMapping, attrOfParent: String? = null): RecursionTree =
        when (node) {
            is PartiqlAst.ReturningMapping.AllNew -> RecursionTree(
                astType = "AllNew",
                attrOfParent = attrOfParent
            )
            is PartiqlAst.ReturningMapping.AllOld -> RecursionTree(
                astType = "AllOld",
                attrOfParent = attrOfParent
            )
            is PartiqlAst.ReturningMapping.ModifiedNew -> RecursionTree(
                astType = "ModifiedNew",
                attrOfParent = attrOfParent
            )
            is PartiqlAst.ReturningMapping.ModifiedOld -> RecursionTree(
                astType = "ModifiedOld",
                attrOfParent = attrOfParent
            )
        }

    private fun toRecursionTree(node: PartiqlAst.ColumnComponent, attrOfParent: String? = null): RecursionTree =
        when (node) {
            is PartiqlAst.ColumnComponent.ReturningColumn -> RecursionTree(
                astType = "ReturningColumn",
                attrOfParent = attrOfParent
            )
            is PartiqlAst.ColumnComponent.ReturningWildcard -> RecursionTree(
                astType = "ReturningWildcard",
                attrOfParent = attrOfParent
            )
        }

    // *********
    // * Query *
    // *********
    private fun toRecursionTree(node: PartiqlAst.Expr, attrOfParent: String? = null): RecursionTree =
        when (node) {
            is PartiqlAst.Expr.Id -> RecursionTree(
                astType = "Id",
                value = node.name.text + " " + node.case.toString() + " " + node.qualifier.toString(),
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
                    ", 'precision': " + node.value.precision.value.toString() +
                    ", 'timeZone': " + node.value.withTimeZone.toString() +
                    ", 'tzminute': " + node.value.tzMinutes.toString(),
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
                astType = "And",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.operands)
            )
            is PartiqlAst.Expr.Or -> RecursionTree(
                astType = "Or",
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
                astType = "In",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.operands)
            )
            is PartiqlAst.Expr.BagOp -> RecursionTree(
                astType = node.op.javaClass.simpleName.capitalize(),
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
                    if (node.escape == null) it else (it + listOf(toRecursionTree(node.escape, "escape")))
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
                    toRecursionTree(node.expr, "expr"),
                    toRecursionTree(node.cases, "cases")
                ).let {
                    if (node.default == null) it else (it.plusElement(toRecursionTree(node.default, "default")))
                }
            )
            is PartiqlAst.Expr.SearchedCase -> RecursionTree(
                astType = "SearchedCase",
                attrOfParent = attrOfParent,
                children = listOf(
                    toRecursionTree(node.cases, "cases")
                ).let {
                    if (node.default == null) it else (it.plusElement(toRecursionTree(node.default, "default")))
                }
            )
            is PartiqlAst.Expr.Struct -> RecursionTree(
                astType = "Struct",
                attrOfParent = attrOfParent,
                children = node.fields.mapIndexed { index, exprPair -> toRecursionTree(exprPair, "field${index + 1}") }
            )
            is PartiqlAst.Expr.Bag -> RecursionTree(
                astType = "Bag",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.values)
            )
            is PartiqlAst.Expr.List -> RecursionTree(
                astType = "List",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.values)
            )
            is PartiqlAst.Expr.Sexp -> RecursionTree(
                astType = "Sexp",
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.values)
            )
            is PartiqlAst.Expr.Path -> RecursionTree(
                astType = "Path",
                attrOfParent = attrOfParent,
                children = listOf(
                    toRecursionTree(node.root, "root")
                ) + node.steps.mapIndexed { index, pathStep -> toRecursionTree(pathStep, "step${index + 1}") }
            )
            is PartiqlAst.Expr.Call -> RecursionTree(
                astType = "Call",
                value = node.funcName.text,
                attrOfParent = attrOfParent,
                children = toRecursionTreeList(node.args, "arg")
            )
            is PartiqlAst.Expr.CallAgg -> RecursionTree(
                astType = "CallAgg",
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
                    if (node.fromLet == null) it else (it.plusElement(toRecursionTree(node.fromLet, "let")))
                }.let {
                    if (node.where == null) it else (it.plusElement(toRecursionTree(node.where, "where")))
                }.let {
                    if (node.group == null) it else (it.plusElement(toRecursionTree(node.group, "group")))
                }.let {
                    if (node.having == null) it else (it.plusElement(toRecursionTree(node.having, "having")))
                }.let {
                    if (node.order == null) it else (it.plusElement(toRecursionTree(node.order, "order")))
                }.let {
                    if (node.limit == null) it else (it.plusElement(toRecursionTree(node.limit, "limit")))
                }.let {
                    if (node.offset == null) it else (it.plusElement(toRecursionTree(node.offset, "offset")))
                }
            )
        }

    private fun toRecursionTreeList(nodes: List<PartiqlAst.Expr>, attrOfParent: String? = null): List<RecursionTree> =
        nodes.map { toRecursionTree(it, attrOfParent) }

    private fun toRecursionTree(node: PartiqlAst.ExprPair, attrOfParent: String? = null): RecursionTree =
        RecursionTree(
            astType = "Pair",
            attrOfParent = attrOfParent,
            children = listOf(
                toRecursionTree(node.first, "first"),
                toRecursionTree(node.second, "second")
            )
        )

    private fun toRecursionTree(node: PartiqlAst.ExprPairList, attrOfParent: String? = null): RecursionTree =
        RecursionTree(
            astType = "ExprPairList",
            attrOfParent = attrOfParent,
            children = node.pairs.mapIndexed { index, exprPair -> toRecursionTree(exprPair, "pair${index + 1}") }
        )

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
                children = node.projectItems.mapIndexed { index, projectItem -> toRecursionTree(projectItem, "projectItem${index + 1}") }
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
                    if (node.asAlias == null) it else { it.plusElement(toRecursionTree(node.asAlias, "as")) }
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
                    if (node.predicate == null) it else { it.plusElement(toRecursionTree(node.predicate, "on")) }
                }
            )
            is PartiqlAst.FromSource.Scan -> RecursionTree(
                astType = "Scan",
                attrOfParent = attrOfParent,
                children = listOf(toRecursionTree(node.expr)).let {
                    if (node.asAlias == null) it else { it.plusElement(toRecursionTree(node.asAlias, attrOfParent = "as")) }
                }.let {
                    if (node.atAlias == null) it else { it.plusElement(toRecursionTree(node.atAlias, attrOfParent = "at")) }
                }.let {
                    if (node.byAlias == null) it else { it.plusElement(toRecursionTree(node.byAlias, attrOfParent = "by")) }
                }
            )
            is PartiqlAst.FromSource.Unpivot -> RecursionTree(
                astType = "Unpivot",
                attrOfParent = attrOfParent,
                children = listOf(toRecursionTree(node.expr)).let {
                    if (node.asAlias == null) it else { it.plusElement(toRecursionTree(node.asAlias, attrOfParent = "as")) }
                }.let {
                    if (node.atAlias == null) it else { it.plusElement(toRecursionTree(node.atAlias, attrOfParent = "at")) }
                }.let {
                    if (node.byAlias == null) it else { it.plusElement(toRecursionTree(node.byAlias, attrOfParent = "by")) }
                }
            )
            else -> TODO("Unsupported FROM AST node")
        }

    private fun toRecursionTree(node: PartiqlAst.Let, attrOfParent: String? = null): RecursionTree =
        RecursionTree(
            astType = "Let",
            attrOfParent = attrOfParent,
            children = node.letBindings.mapIndexed { index, letBinding -> toRecursionTree(letBinding, "letBinding${index + 1}") }
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
                    children = node.keyList.keys.mapIndexed { index, groupKey ->
                        RecursionTree(
                            astType = "GroupKey",
                            attrOfParent = "key${index + 1}",
                            children = listOf(
                                toRecursionTree(groupKey.expr, "expr")
                            ).let {
                                if (groupKey.asAlias == null) it else { it.plusElement(toRecursionTree(groupKey.asAlias, "as")) }
                            }
                        )
                    }
                )
            ).let {
                if (node.groupAsAlias == null) it else { it.plusElement(toRecursionTree(node.groupAsAlias, attrOfParent = "groupAs")) }
            }
        )

    private fun toRecursionTree(node: PartiqlAst.OrderBy, attrOfParent: String? = null): RecursionTree =
        RecursionTree(
            astType = "Order",
            attrOfParent = attrOfParent,
            children = node.sortSpecs.mapIndexed { index, sortSpec ->
                RecursionTree(
                    astType = "SortSpec",
                    attrOfParent = "sortSpec${index + 1}",
                    children = listOf(
                        toRecursionTree(sortSpec.expr, "expr"),
                        RecursionTree(
                            astType = sortSpec.orderingSpec.toString(),
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
