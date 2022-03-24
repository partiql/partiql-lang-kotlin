package org.partiql.lang.prettyprint

import com.amazon.ion.system.IonSystemBuilder
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.syntax.SqlParser
import org.partiql.pig.runtime.toIonElement
import java.lang.StringBuilder
import java.time.LocalDate
import java.time.LocalTime
import kotlin.IllegalStateException
import kotlin.math.abs


/**
 * This class is used to pretty print a query, which first transforms a query into a parsed tree,
 * and then transform it back to a pretty string.
 *
 * The idea is to use a StringBuilder and write a pretty-printed query to it according to the like
 * of the parsed tree
 */
class QueryPrettyPrinter {
    fun prettyPrintQuery(query: String): String {
        val ion = IonSystemBuilder.standard().build()
        val ast = SqlParser(ion).parseAstStatement(query)

        return astToPrettyQuery(ast)
    }

    fun astToPrettyQuery(ast: PartiqlAst.Statement): String {
        val sb = StringBuilder()
        writeAstNode(ast, sb)
        if (sb.lastOrNull() == '\n'){
            sb.removeLast(1)
        }

        return sb.toString()
    }

    private fun writeAstNode(node: PartiqlAst.Statement, sb: StringBuilder) {
        when(node){
            is PartiqlAst.Statement.Query -> writeAstNode(node.expr, sb, 0)
            is PartiqlAst.Statement.Ddl -> TODO()
            is PartiqlAst.Statement.Dml -> TODO()
            is PartiqlAst.Statement.Exec -> TODO()
        }
    }

    /**
     * @param node is the PIG AST node
     * @param sb is the StringBuilder where we write the pretty query according to the like of the parsed tree
     * @param level is an integer which marks how deep in the nested query we are. It increments Only when we step
     * into a Case or Select clause.
     */
    private fun writeAstNode(node: PartiqlAst.Expr, sb: StringBuilder, level: Int) {
        when (node){
            is PartiqlAst.Expr.Missing -> writeAstNode(node, sb)
            is PartiqlAst.Expr.Lit -> writeAstNode(node, sb)
            is PartiqlAst.Expr.LitTime -> writeAstNode(node, sb)
            is PartiqlAst.Expr.Date -> writeAstNode(node, sb)
            is PartiqlAst.Expr.Id -> writeAstNode(node, sb)
            is PartiqlAst.Expr.Bag -> writeAstNode(node, sb, level)
            is PartiqlAst.Expr.Sexp -> writeAstNode(node, sb, level)
            is PartiqlAst.Expr.Struct -> writeAstNode(node, sb, level)
            is PartiqlAst.Expr.List -> writeAstNode(node, sb, level)
            is PartiqlAst.Expr.Parameter -> writeAstNode(node, sb)
            is PartiqlAst.Expr.Call -> writeAstNode(node, sb, level)
            is PartiqlAst.Expr.CallAgg -> writeAstNode(node, sb, level)

            is PartiqlAst.Expr.Path -> writeAstNode(node, sb, level)

            is PartiqlAst.Expr.SimpleCase -> writeAstNode(node, sb, level)
            is PartiqlAst.Expr.SearchedCase -> writeAstNode(node, sb, level)
            is PartiqlAst.Expr.Select -> writeAstNode(node, sb, level)

            is PartiqlAst.Expr.Pos -> writeAstNode(node, sb, level)
            is PartiqlAst.Expr.Neg -> writeAstNode(node, sb, level)
            is PartiqlAst.Expr.Not -> writeAstNode(node, sb, level)
            is PartiqlAst.Expr.Between -> writeAstNode(node, sb, level)
            is PartiqlAst.Expr.Like -> writeAstNode(node, sb, level)
            is PartiqlAst.Expr.IsType -> writeAstNode(node, sb, level)
            is PartiqlAst.Expr.Cast -> writeAstNode(node, sb, level)
            is PartiqlAst.Expr.CanCast -> writeAstNode(node, sb, level)
            is PartiqlAst.Expr.CanLosslessCast -> writeAstNode(node, sb, level)
            is PartiqlAst.Expr.Coalesce -> writeAstNode(node, sb, level)
            is PartiqlAst.Expr.NullIf -> writeAstNode(node, sb, level)

            is PartiqlAst.Expr.Concat -> writeNAryOperator("||", node.operands, sb, level)
            is PartiqlAst.Expr.Plus -> writeNAryOperator("+", node.operands, sb, level)
            is PartiqlAst.Expr.Minus -> writeNAryOperator("-", node.operands, sb, level)
            is PartiqlAst.Expr.Times -> writeNAryOperator("*", node.operands, sb, level)
            is PartiqlAst.Expr.Divide -> writeNAryOperator("/", node.operands, sb, level)
            is PartiqlAst.Expr.Modulo -> writeNAryOperator("%", node.operands, sb, level)
            is PartiqlAst.Expr.Eq -> writeNAryOperator("=", node.operands, sb, level)
            is PartiqlAst.Expr.Ne -> writeNAryOperator("!=", node.operands, sb, level)
            is PartiqlAst.Expr.Gt -> writeNAryOperator(">", node.operands, sb, level)
            is PartiqlAst.Expr.Gte -> writeNAryOperator(">=", node.operands, sb, level)
            is PartiqlAst.Expr.Lt -> writeNAryOperator("<", node.operands, sb, level)
            is PartiqlAst.Expr.Lte -> writeNAryOperator("<=", node.operands, sb, level)
            is PartiqlAst.Expr.And -> writeNAryOperator("AND", node.operands, sb, level)
            is PartiqlAst.Expr.Or -> writeNAryOperator("OR", node.operands, sb, level)
            is PartiqlAst.Expr.InCollection -> writeNAryOperator("IN", node.operands, sb, level)
            is PartiqlAst.Expr.Union -> when (node.setq) {
                is PartiqlAst.SetQuantifier.Distinct -> writeNAryOperator("UNION", node.operands, sb, level)
                is PartiqlAst.SetQuantifier.All -> writeNAryOperator("UNION ALL", node.operands, sb, level)
            }
            is PartiqlAst.Expr.Intersect -> when (node.setq) {
                is PartiqlAst.SetQuantifier.Distinct -> writeNAryOperator("INTERSECT", node.operands, sb, level)
                is PartiqlAst.SetQuantifier.All -> writeNAryOperator("INTERSECT ALL", node.operands, sb, level)
            }
            is PartiqlAst.Expr.Except -> when (node.setq) {
                is PartiqlAst.SetQuantifier.Distinct -> writeNAryOperator("EXCEPT", node.operands, sb, level)
                is PartiqlAst.SetQuantifier.All -> writeNAryOperator("EXCEPT ALL", node.operands, sb, level)
            }
        }
    }

    /**
     * If the node indicates a sub-query, we surround it with parenthesis and start a new line for it.
     */
    private fun writeAstNodeCheckSubQuery(node: PartiqlAst.Expr, sb: StringBuilder, level: Int){
        when (isCaseOrSelect(node)){
            true -> {
                val indent = getIndent(level + 1)
                sb.append("(\n$indent")
                writeAstNode(node, sb, level + 1)
                sb.append(')')
            }
            false -> writeAstNode(node, sb, level)
        }
    }

    private fun writeAstNode(node: PartiqlAst.Expr.Missing, sb: StringBuilder) {
        sb.append("MISSING")
    }

    private fun writeAstNode(node: PartiqlAst.Expr.Lit, sb: StringBuilder) {
        // Not sure if there is a better way to transform IonElement into a PartiQL value as string
        val value = when (node.value.type){
            com.amazon.ionelement.api.ElementType.NULL -> "NULL"
            com.amazon.ionelement.api.ElementType.BOOL -> node.value.booleanValue.toString().toUpperCase()
            com.amazon.ionelement.api.ElementType.INT -> node.value.longValue.toString()
            com.amazon.ionelement.api.ElementType.DECIMAL -> node.value.decimalValue.toString()
            com.amazon.ionelement.api.ElementType.FLOAT -> node.value.doubleValue.toString()
            com.amazon.ionelement.api.ElementType.STRING -> "'${node.value.stringValue}'"
            else -> "`${node.value.toIonElement()}`"
        }

        sb.append(value)
    }

    private fun writeAstNode(node: PartiqlAst.Expr.Date, sb: StringBuilder) {
        val date = LocalDate.of(node.year.value.toInt(), node.month.value.toInt(), node.day.value.toInt())
        sb.append("DATE '$date'")
    }

    private fun writeAstNode(node: PartiqlAst.Expr.LitTime, sb: StringBuilder) {
        val localTime = LocalTime.of(
            node.value.hour.value.toInt(),
            node.value.minute.value.toInt(),
            node.value.second.value.toInt(),
            node.value.nano.value.toInt()
        )
        val precision = node.value.precision
        val withTimeZone = node.value.withTimeZone
        val tzTime = node.value.tzMinutes?.let {
            val prefix = when {
                (it.value >= 0) -> "+"
                else -> "-"
            }
            val timeValue = abs(it.value.toInt())
            val tzLocalTime = LocalTime.of(timeValue / 60, timeValue % 60)
            "$prefix$tzLocalTime"
        } ?: ""

        when (withTimeZone.value) {
            true -> sb.append("TIME ($precision) WITH TIME ZONE '$localTime$tzTime'")
            false -> sb.append("TIME ($precision) '$localTime'")
        }
    }

    private fun writeAstNode(node: PartiqlAst.Expr.Bag, sb: StringBuilder, level: Int) {
        sb.append("<< ")
        node.values.forEach {
            // The next line may need to be changed, as it does not produce a pretty query
            // where CASE or SELECT clause is wrapped inside a bag
            writeAstNodeCheckSubQuery(it, sb, level)
            sb.append(", ")
        }
        if (node.values.isNotEmpty()) {
            sb.removeLast(2)
        }
        sb.append(" >>")
    }

    private fun writeAstNode(node: PartiqlAst.Expr.Sexp, sb: StringBuilder, level: Int) {
        sb.append("sexp(")
        node.values.forEach {
            // The next line may need to be changed, as it does not produce a pretty query
            // where CASE or SELECT clause is wrapped inside a sexp
            writeAstNodeCheckSubQuery(it, sb, level)
            sb.append(", ")
        }
        if (node.values.isNotEmpty()){
            sb.removeLast(2)
        }
        sb.append(")")
    }

    private fun writeAstNode(node: PartiqlAst.Expr.List, sb: StringBuilder, level: Int) {
        sb.append("[ ")
        node.values.forEach {
            // The next line may need to be changed, as it does not produce a pretty query
            // where CASE or SELECT clause is wrapped inside a list
            writeAstNodeCheckSubQuery(it, sb, level)
            sb.append(", ")
        }
        if (node.values.isNotEmpty()){
            sb.removeLast(2)
        }
        sb.append(" ]")
    }

    private fun writeAstNode(node: PartiqlAst.Expr.Struct, sb: StringBuilder, level: Int) {
        sb.append("{ ")
        node.fields.forEach {
            // The next line may need to be changed, as it does not produce a pretty query
            // where CASE or SELECT clause is wrapped inside a struct
            writeAstNodeCheckSubQuery(it.first, sb, level)
            sb.append(": ")
            writeAstNodeCheckSubQuery(it.second, sb, level)
            sb.append(", ")
        }
        if (node.fields.isNotEmpty()){
            sb.removeLast(2)
        }
        sb.append(" }")
    }

    private fun writeAstNode(node: PartiqlAst.Expr.Parameter, sb: StringBuilder) {
        sb.append("?")
    }

    private fun writeAstNode(node: PartiqlAst.Expr.Id, sb: StringBuilder) {
        when (node.case){
            is PartiqlAst.CaseSensitivity.CaseSensitive -> sb.append("\"${node.name.text}\"")
            is PartiqlAst.CaseSensitivity.CaseInsensitive -> sb.append(node.name.text)
        }
    }

    private fun writeAstNode(node: PartiqlAst.Expr.Call, sb: StringBuilder, level: Int) {
        sb.append("${node.funcName.text}(")
        node.args.forEach { arg ->
            // The next line may need to be changed, as it does not produce a pretty query
            // where CASE or SELECT clause is wrapped inside a function call
            writeAstNodeCheckSubQuery(arg, sb, level)
            sb.append(", ")
        }
        if (node.args.isNotEmpty()){
            sb.removeLast(2)
        }
        sb.append(')')
    }

    private fun writeAstNode(node: PartiqlAst.Expr.CallAgg, sb: StringBuilder, level: Int) {
        sb.append("${node.funcName.text}(")
        if (node.setq is PartiqlAst.SetQuantifier.Distinct) {
            sb.append("DISTINCT ")
        }
        // The next line may need to be changed, as it does not produce a pretty query
        // where CASE or SELECT clause is wrapped inside a function call
        writeAstNodeCheckSubQuery(node.arg, sb, level + 1)
        sb.append(')')
    }

    private fun writeAstNode(node: PartiqlAst.Expr.Path, sb: StringBuilder, level: Int) {
        when {
            isOperator(node.root) || node.root is PartiqlAst.Expr.Path -> {
                sb.append('(')
                writeAstNode(node.root, sb, level)
                sb.append(')')
            }
            else -> writeAstNode(node.root, sb, level) // Assume a path root is not a SELECT or CASE clause, i.e. people don't write (SELECT a FROM b).c
        }
        node.steps.forEach {
            when (it) {
                is PartiqlAst.PathStep.PathExpr -> when (it.case) {
                    is PartiqlAst.CaseSensitivity.CaseSensitive -> {
                        // This means the value of the path component is surrounded by square brackets '[' and ']'
                        // or double-quotes i.e. either a[b] or a."b"
                        // Here we just transform it to be surrounded by square brackets
                        sb.append('[')
                        writeAstNode(it.index, sb, level) // Assume a path component is not a SELECT or CASE clause, i.e. people don't write a[SELECT b FROM c]
                        sb.append(']')
                    }
                    // Case for a.b
                    is PartiqlAst.CaseSensitivity.CaseInsensitive -> when (it.index) {
                        is PartiqlAst.Expr.Lit -> {
                            val value = it.index.value.stringValue // It must be a string according to behavior of Lexer
                            sb.append(".$value")
                        }
                        else -> IllegalArgumentException("PathExpr's attribute 'index' must be PartiqlAst.Expr.Lit when 'caseSensitive' is CaseInsensitive")
                    }
                }
                is PartiqlAst.PathStep.PathUnpivot -> sb.append(".[*]")
                is PartiqlAst.PathStep.PathWildcard -> sb.append(".*")
            }
        }
    }

    private fun writeAstNode(node: PartiqlAst.Expr.SimpleCase, sb: StringBuilder, level: Int) {
        val indent = getIndent(level)
        sb.append("CASE ")
        // The next lines may need to be changed, as it does not produce a pretty query
        // where CASE or SELECT clause is wrapped inside CASE clause
        writeAstNodeCheckSubQuery(node.expr, sb, level)
        writeCaseWhenClauses(node.cases.pairs, sb, level + 1)
        writeCaseElseClause(node.default, sb, level + 1)
        sb.append("\n${indent}END")
    }

    private fun writeAstNode(node: PartiqlAst.Expr.SearchedCase, sb: StringBuilder, level: Int) {
        val indent = getIndent(level)
        sb.append("CASE")
        writeCaseWhenClauses(node.cases.pairs, sb, level + 1)
        writeCaseElseClause(node.default, sb, level + 1)
        sb.append("\n${indent}END")
    }

    private fun writeCaseWhenClauses(pairs: List<PartiqlAst.ExprPair>, sb: StringBuilder, level: Int) {
        val indent = getIndent(level)
        pairs.forEach { pair ->
            sb.append("\n${indent}WHEN ")
            writeAstNodeCheckSubQuery(pair.first, sb, level)
            sb.append(" THEN ")
            writeAstNodeCheckSubQuery(pair.second, sb, level)
        }
    }

    private fun writeCaseElseClause(default: PartiqlAst.Expr?, sb: StringBuilder, level: Int) {
        if (default != null) {
            val indent = getIndent(level)
            sb.append("\n${indent}ELSE ")
            writeAstNodeCheckSubQuery(default, sb, level)
        }
    }

    private fun writeAstNode(node: PartiqlAst.Expr.Select, sb: StringBuilder, level: Int) {
        val indent = getIndent(level)

        // SELECT clause
        when (node.project) {
            is PartiqlAst.Projection.ProjectPivot -> sb.append("PIVOT ")
            else -> when (node.setq) {
                is PartiqlAst.SetQuantifier.Distinct -> sb.append("SELECT DISTINCT ")
                else -> sb.append("SELECT ")
            }
        }
        writeProjection(node.project, sb, level)

        // FROM clause
        sb.append("\n${indent}FROM ")
        writeFromSource(node.from, sb, level)

        // LET clause
        node.fromLet?.let {
            sb.append(" LET ")
            writeFromLet(it, sb, level)
        }

        // WHERE clause
        node.where?.let {
            sb.append("\n${indent}WHERE ")
            writeAstNodeCheckSubQuery(it, sb, level + 1)
        }

        // GROUP clause
        node.group?.let {
            sb.append("\n${indent}GROUP ")
            writeGroupBy(it, sb, level)
        }

        // HAVING clause
        node.having?.let {
            sb.append("\n${indent}HAVING ")
            writeAstNodeCheckSubQuery(it, sb, level)
        }

        // ORDER BY clause
        node.order?.let { orderBy ->
            sb.append("\n${indent}ORDER BY ")
            orderBy.sortSpecs.forEach { sortSpec ->
                writeSortSpec(sortSpec, sb, level)
                sb.append(", ")
            }
            sb.removeLast(2)
        }

        // LIMIT clause
        node.limit?.let {
            sb.append("\n${indent}LIMIT ")
            writeAstNodeCheckSubQuery(it, sb, level + 1)
        }

        // OFFSET clause
        node.offset?.let {
            sb.append("\n${indent}OFFSET ")
            writeAstNodeCheckSubQuery(it, sb, level + 1)
        }
    }

    private fun writeSortSpec (sortSpec: PartiqlAst.SortSpec, sb: StringBuilder, level: Int) {
        writeAstNodeCheckSubQuery(sortSpec.expr, sb, level + 1)
        when (sortSpec.orderingSpec) {
            is PartiqlAst.OrderingSpec.Asc -> sb.append(" ASC")
            is PartiqlAst.OrderingSpec.Desc -> sb.append(" DESC")
        }
    }

    private fun writeGroupBy (group: PartiqlAst.GroupBy, sb: StringBuilder, level: Int) {
        when (group.strategy) {
            is PartiqlAst.GroupingStrategy.GroupFull -> sb.append("BY ")
            is PartiqlAst.GroupingStrategy.GroupPartial -> sb.append("PARTIAL BY ")
        }
        group.keyList.keys.forEach {
            writeGroupKey(it, sb, level)
            sb.append(", ")
        }
        sb.removeLast(2)
        group.groupAsAlias?.let { sb.append(" GROUP AS ${it.text}") }
    }

    private fun writeGroupKey (key: PartiqlAst.GroupKey, sb: StringBuilder, level: Int) {
        writeAstNodeCheckSubQuery(key.expr, sb, level)
        key.asAlias?.let { sb.append(" AS ${it.text}") }
    }

    private fun writeFromLet(fromLet: PartiqlAst.Let, sb: StringBuilder, level: Int) {
        fromLet.letBindings.forEach {
            writeLetBinding(it, sb, level)
            sb.append(", ")
        }
        sb.removeLast(2)
    }

    private fun writeLetBinding(letBinding: PartiqlAst.LetBinding, sb: StringBuilder, level: Int) {
        writeAstNodeCheckSubQuery(letBinding.expr, sb, level)
        sb.append(" AS ${letBinding.name.text}")
    }

    private fun writeFromSource(from: PartiqlAst.FromSource, sb: StringBuilder, level: Int) {
        val indent = getIndent(level)
        when (from) {
            is PartiqlAst.FromSource.Scan -> {
                writeAstNodeCheckSubQuery(from.expr, sb, level)
                from.asAlias?.let { sb.append(" AS ${it.text}") }
                from.atAlias?.let { sb.append(" AT ${it.text}") }
                from.byAlias?.let { sb.append(" BY ${it.text}") }
            }
            is PartiqlAst.FromSource.Join -> when {
                (from.type is PartiqlAst.JoinType.Inner && from.predicate != null) -> {
                    // This means we can use comma to separate JOIN left-hand side and right-hand side
                    writeFromSource(from.left, sb, level)
                    sb.append(", ")
                    writeFromSource(from.right, sb, level)
                }
                else -> {
                    val join = when (from.type) {
                        is PartiqlAst.JoinType.Inner -> "JOIN"
                        is PartiqlAst.JoinType.Left -> "LEFT CROSS JOIN"
                        is PartiqlAst.JoinType.Right -> "RIGHT CROSS JOIN"
                        is PartiqlAst.JoinType.Full -> "FULL CROSS JOIN"
                    }
                    writeFromSource(from.left, sb, level)
                    sb.append("\n${indent}\t$join ")
                    writeFromSource(from.right, sb, level)
                    from.predicate?.let {
                        sb.append(" ON ")
                        writeAstNodeCheckSubQuery(it, sb, level)
                    }
                }
            }
            is PartiqlAst.FromSource.Unpivot -> {
                sb.append("UNPIVOT ")
                writeAstNodeCheckSubQuery(from.expr, sb, level)
                from.asAlias?.let { sb.append(" AS ${it.text}") }
                from.atAlias?.let { sb.append(" AT ${it.text}") }
                from.byAlias?.let { sb.append(" BY ${it.text}") }
            }
        }
    }

    private fun writeProjection(project: PartiqlAst.Projection, sb: StringBuilder, level: Int) {
        when (project) {
            is PartiqlAst.Projection.ProjectStar -> sb.append('*')
            is PartiqlAst.Projection.ProjectValue -> {
                sb.append("VALUE ")
                writeAstNode(project.value, sb, level)
            }
            is PartiqlAst.Projection.ProjectList -> {
                val projectItems = project.projectItems
                projectItems.forEach { item ->
                    writeProjectItem(item, sb, level)
                    sb.append(", ")
                }
                sb.removeLast(2)
            }
            is PartiqlAst.Projection.ProjectPivot -> {
                writeAstNodeCheckSubQuery(project.key, sb, level)
                sb.append(" AT ")
                writeAstNodeCheckSubQuery(project.value, sb, level)
            }
        }
    }

    private fun writeProjectItem(item: PartiqlAst.ProjectItem, sb: StringBuilder, level: Int) {
        when (item) {
            is PartiqlAst.ProjectItem.ProjectAll -> {
                writeAstNodeCheckSubQuery(item.expr, sb, level)
                sb.append(".*")
            }
            is PartiqlAst.ProjectItem.ProjectExpr -> {
                writeAstNodeCheckSubQuery(item.expr, sb, level)
                item.asAlias?.let {
                    sb.append(" AS ")
                    sb.append(it.text)
                }
            }
        }
    }

    // The logic here can be improved, so we can remove unnecessary parenthesis in different scenarios.
    // i.e. currently, it transforms '1 + 2 + 3' as '(1 + 2) + 3', however, the parenthesis can be removed.
    private fun writeAstNodeCheckOp(node: PartiqlAst.Expr, sb: StringBuilder, level: Int) {
        when (isOperator(node)){
            true -> {
                sb.append('(')
                writeAstNode(node, sb, level)
                sb.append(')')
            }
            // The next line may need to be changed, as it does not produce a pretty query
            // where CASE or SELECT clause is wrapped inside operators
            false -> writeAstNodeCheckSubQuery(node, sb, level)
        }
    }

    private fun writeAstNode(node: PartiqlAst.Expr.Pos, sb: StringBuilder, level: Int) {
        sb.append('+')
        writeAstNodeCheckOp(node.expr, sb, level)
    }

    private fun writeAstNode(node: PartiqlAst.Expr.Neg, sb: StringBuilder, level: Int) {
        sb.append('-')
        writeAstNodeCheckOp(node.expr, sb, level)
    }

    private fun writeAstNode(node: PartiqlAst.Expr.Not, sb: StringBuilder, level: Int) {
        sb.append("NOT ")
        writeAstNodeCheckOp(node.expr, sb, level)
    }

    private fun writeAstNode(node: PartiqlAst.Expr.Between, sb: StringBuilder, level: Int) {
        writeAstNodeCheckOp(node.value, sb, level)
        sb.append(" BETWEEN ")
        writeAstNodeCheckOp(node.from, sb, level)
        sb.append(" AND ")
        writeAstNodeCheckOp(node.to, sb, level)
    }

    private fun writeAstNode(node: PartiqlAst.Expr.Like, sb: StringBuilder, level: Int) {
        writeAstNodeCheckOp(node.value, sb, level)
        sb.append(" LIKE ")
        writeAstNodeCheckOp(node.pattern, sb, level)
        node.escape?.let {
            sb.append(" ESCAPE ")
            writeAstNodeCheckOp(node.escape, sb, level)
        }
    }

    private fun writeAstNode(node: PartiqlAst.Expr.IsType, sb: StringBuilder, level: Int) {
        writeAstNodeCheckOp(node.value, sb, level)
        sb.append(" IS ")
        writeType(node.type, sb)
    }

    private fun writeType(node: PartiqlAst.Type, sb: StringBuilder) {
        when (node) {
            is PartiqlAst.Type.NullType -> sb.append("NULL")
            is PartiqlAst.Type.AnyType -> sb.append("ANY")
            is PartiqlAst.Type.BagType -> sb.append("BAG")
            is PartiqlAst.Type.BlobType -> sb.append("BLOB")
            is PartiqlAst.Type.BooleanType -> sb.append("BOOLEAN")
            is PartiqlAst.Type.CharacterType -> sb.append("CHAR")
            is PartiqlAst.Type.CharacterVaryingType -> sb.append("VARCHAR")
            is PartiqlAst.Type.ClobType -> sb.append("CLOB")
            is PartiqlAst.Type.DateType -> sb.append("DATE")
            is PartiqlAst.Type.DecimalType -> sb.append("DECIMAL")
            is PartiqlAst.Type.DoublePrecisionType -> sb.append("DOUBLE_PRECISION")
            is PartiqlAst.Type.EsAny -> sb.append("ES_ANY")
            is PartiqlAst.Type.EsBoolean -> sb.append("ES_BOOLEAN")
            is PartiqlAst.Type.EsFloat -> sb.append("ES_FLOAT")
            is PartiqlAst.Type.EsInteger -> sb.append("ES_INTEGER")
            is PartiqlAst.Type.EsText -> sb.append("ES_TEXT")
            is PartiqlAst.Type.FloatType -> sb.append("FLOAT")
            is PartiqlAst.Type.Integer4Type -> sb.append("INT4")
            is PartiqlAst.Type.Integer8Type -> sb.append("INT8")
            is PartiqlAst.Type.IntegerType -> sb.append("INT")
            is PartiqlAst.Type.ListType -> sb.append("LIST")
            is PartiqlAst.Type.MissingType -> sb.append("MISSING")
            is PartiqlAst.Type.NumericType -> sb.append("NUMERIC")
            is PartiqlAst.Type.RealType -> sb.append("REAL")
            is PartiqlAst.Type.RsBigint -> sb.append("RS_BIGINT")
            is PartiqlAst.Type.RsBoolean -> sb.append("RS_BOOLEAN")
            is PartiqlAst.Type.RsDoublePrecision -> sb.append("RS_DOUBLE_PRECISION")
            is PartiqlAst.Type.RsInteger -> sb.append("RS_INTEGER")
            is PartiqlAst.Type.RsReal -> sb.append("RS_REAL")
            is PartiqlAst.Type.RsVarcharMax -> sb.append("RS_VARCHAR_MAX")
            is PartiqlAst.Type.SexpType -> sb.append("SEXP")
            is PartiqlAst.Type.SmallintType -> sb.append("SMALLINT")
            is PartiqlAst.Type.SparkBoolean -> sb.append("SPARK_BOOLEAN")
            is PartiqlAst.Type.SparkDouble -> sb.append("SPARK_DOUBLE")
            is PartiqlAst.Type.SparkFloat -> sb.append("SPARK_FLOAT")
            is PartiqlAst.Type.SparkInteger -> sb.append("SPARK_INTEGER")
            is PartiqlAst.Type.SparkLong -> sb.append("SPARK_LONG")
            is PartiqlAst.Type.SparkShort -> sb.append("SPARK_SHORT")
            is PartiqlAst.Type.StringType -> sb.append("STRING")
            is PartiqlAst.Type.StructType -> sb.append("STRUCT")
            is PartiqlAst.Type.SymbolType -> sb.append("SYMBOL")
            is PartiqlAst.Type.TimeType -> sb.append("TIME")
            is PartiqlAst.Type.TimeWithTimeZoneType -> sb.append("TIME WITH TIME ZONE")
            is PartiqlAst.Type.TimestampType -> sb.append("TIMESTAMP")
            is PartiqlAst.Type.TupleType -> sb.append("TUPLE")
            is PartiqlAst.Type.CustomType -> error("CustomType is not supported yet. ")
        }
    }

    private fun writeAstNode(node: PartiqlAst.Expr.Cast, sb: StringBuilder, level: Int) {
        sb.append("CAST (")
        writeAstNodeCheckOp(node.value, sb, level)
        sb.append(" AS ")
        writeType(node.asType, sb)
        sb.append(')')
    }

    private fun writeAstNode(node: PartiqlAst.Expr.CanCast, sb: StringBuilder, level: Int) {
        sb.append("CAN_CAST (")
        writeAstNodeCheckOp(node.value, sb, level)
        sb.append(" AS ")
        writeType(node.asType, sb)
        sb.append(')')
    }

    private fun writeAstNode(node: PartiqlAst.Expr.CanLosslessCast, sb: StringBuilder, level: Int) {
        sb.append("CAN_LOSSLESS_CAST (")
        writeAstNodeCheckOp(node.value, sb, level)
        sb.append(" AS ")
        writeType(node.asType, sb)
        sb.append(')')
    }

    private fun writeAstNode(node: PartiqlAst.Expr.Coalesce, sb: StringBuilder, level: Int) {
        sb.append("COALESCE(")
        node.args.forEach { arg ->
            writeAstNodeCheckSubQuery(arg, sb, level)
            sb.append(", ")
        }
        if (node.args.isNotEmpty()){
            sb.removeLast(2)
        }
        sb.append(')')
    }

    private fun writeAstNode(node: PartiqlAst.Expr.NullIf, sb: StringBuilder, level: Int) {
        sb.append("NULLIF(")
        writeAstNodeCheckSubQuery(node.expr1, sb, level)
        sb.append(", ")
        writeAstNodeCheckSubQuery(node.expr2, sb, level)
        sb.append(')')
    }

    private fun writeNAryOperator(operatorName: String, operands: List<PartiqlAst.Expr>, sb: StringBuilder, level: Int){
        if (operands.size < 2) IllegalStateException("Internal Error: NAry operator $operatorName must have at least 2 operands")
        operands.forEach {
            writeAstNodeCheckOp(it, sb, level)
            sb.append(" $operatorName ")
        }
        sb.removeLast(operatorName.length + 2)
    }

    private fun isCaseOrSelect(node: PartiqlAst.Expr) : Boolean =
        when (node) {
            is PartiqlAst.Expr.SimpleCase, is PartiqlAst.Expr.SearchedCase, is PartiqlAst.Expr.Select -> true
            else -> false
        }

    private fun isOperator(node: PartiqlAst.Expr) : Boolean =
        when (node) {
            is PartiqlAst.Expr.And, is PartiqlAst.Expr.Between, is PartiqlAst.Expr.CanCast,
            is PartiqlAst.Expr.CanLosslessCast, is PartiqlAst.Expr.Cast, is PartiqlAst.Expr.Concat,
            is PartiqlAst.Expr.Divide, is PartiqlAst.Expr.Eq, is PartiqlAst.Expr.Except,
            is PartiqlAst.Expr.Gt, is PartiqlAst.Expr.Gte, is PartiqlAst.Expr.InCollection,
            is PartiqlAst.Expr.Intersect, is PartiqlAst.Expr.IsType, is PartiqlAst.Expr.Like,
            is PartiqlAst.Expr.Lt, is PartiqlAst.Expr.Lte, is PartiqlAst.Expr.Minus,
            is PartiqlAst.Expr.Modulo, is PartiqlAst.Expr.Ne, is PartiqlAst.Expr.Neg,
            is PartiqlAst.Expr.Not, is PartiqlAst.Expr.Or, is PartiqlAst.Expr.Plus,
            is PartiqlAst.Expr.Pos, is PartiqlAst.Expr.Times, is PartiqlAst.Expr.Union -> true
            else -> false
        }

    private fun getIndent(level: Int) = "\t".repeat(level)

    private fun StringBuilder.removeLast(n: Int) : StringBuilder {
        for (i in 1..n){
            deleteCharAt(length - 1)
        }
        return this
    }
}