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
    fun prettyPrintQuery(query: String): String =
        astToPrettyQuery(
            SqlParser(
                IonSystemBuilder.standard().build()
            ).parseAstStatement(query)
        )

    fun astToPrettyQuery(ast: PartiqlAst.Statement): String {
        val sb = StringBuilder()
        writeAstNode(ast, sb)
        if (sb.lastOrNull() == '\n') {
            sb.removeLast(1)
        }

        return sb.toString()
    }

    private fun writeAstNode(node: PartiqlAst.Statement, sb: StringBuilder) {
        when (node) {
            is PartiqlAst.Statement.Query -> writeAstNode(node.expr, sb, 0)
            is PartiqlAst.Statement.Ddl -> writeAstNode(node, sb)
            is PartiqlAst.Statement.Dml -> writeAstNode(node, sb)
            is PartiqlAst.Statement.Exec -> writeAstNode(node, sb)
        }
    }

    // ********
    // * Exec *
    // ********
    private fun writeAstNode(node: PartiqlAst.Statement.Exec, sb: StringBuilder) {
        sb.append("EXEC ${node.procedureName.text} ")
        node.args.forEach {
            // Print anything as one line inside EXEC clause
            writeAstNodeCheckSubQuery(it, sb, -1)
            sb.append(", ")
        }
        if (node.args.isNotEmpty()) {
            sb.removeLast(2)
        }
    }

    // *******
    // * Ddl *
    // *******
    private fun writeAstNode(node: PartiqlAst.Statement.Ddl, sb: StringBuilder) {
        when (node.op) {
            is PartiqlAst.DdlOp.CreateTable -> sb.append("CREATE TABLE ${node.op.tableName.text}")
            is PartiqlAst.DdlOp.DropTable -> {
                sb.append("DROP TABLE ")
                writeAstNode(node.op.tableName, sb)
            }
            is PartiqlAst.DdlOp.CreateIndex -> {
                sb.append("CREATE INDEX ON ")
                writeAstNode(node.op.indexName, sb)
                sb.append(" (")
                node.op.fields.forEach {
                    // Assume fields in CREATE INDEX clause are not SELECT or CASE
                    writeAstNode(it, sb, 0)
                    sb.append(", ")
                }
                sb.removeLast(2).append(')')
            }
            is PartiqlAst.DdlOp.DropIndex -> {
                sb.append("DROP INDEX ")
                writeAstNode(node.op.keys, sb)
                sb.append(" ON ")
                writeAstNode(node.op.table, sb)
            }
        }
    }

    private fun writeAstNode(node: PartiqlAst.Identifier, sb: StringBuilder) {
        when (node.case) {
            is PartiqlAst.CaseSensitivity.CaseSensitive -> sb.append("\"${node.name.text}\"")
            is PartiqlAst.CaseSensitivity.CaseInsensitive -> sb.append(node.name.text)
        }
    }

    // *******
    // * Dml *
    // *******
    private fun writeAstNode(node: PartiqlAst.Statement.Dml, sb: StringBuilder) {
        if (node.operations.ops.first() is PartiqlAst.DmlOp.Delete) {
            sb.append("DELETE FROM ")
            writeFromSource(node.from!!, sb, 0)
            node.where?.let {
                sb.append("\nWHERE ")
                writeAstNodeCheckSubQuery(it, sb, 0)
            }
            node.returning?.let { writeReturning(it, sb) }
            return
        }

        node.from?.let {
            sb.append("FROM ")
            writeFromSource(it, sb, 0)
        }

        node.where?.let {
            sb.append("\nWHERE ")
            writeAstNodeCheckSubQuery(it, sb, 0)
        }

        var previousIsSet = false // Consecutive SET nodes should be transformed into one SET clause
        node.operations.ops.forEach {
            if (sb.isNotEmpty()) { // If there is no FROM WHERE clause before, we don't need to add a line break
                sb.append('\n')
            }
            previousIsSet = writeDmlOp(it, sb, previousIsSet)
        }

        node.returning?.let { writeReturning(it, sb) }
    }

    private fun writeDmlOp(dmlOp: PartiqlAst.DmlOp, sb: StringBuilder, previousIsSet: Boolean): Boolean {
        when (dmlOp) {
            is PartiqlAst.DmlOp.Insert -> {
                sb.append("INSERT INTO ")
                writeAstNodeCheckSubQuery(dmlOp.target, sb, 0)
                sb.append(" VALUES ")
                val bag = dmlOp.values as PartiqlAst.Expr.Bag
                bag.values.forEach {
                    val list = it as PartiqlAst.Expr.List
                    sb.append('(')
                    list.values.forEach { value ->
                        writeAstNodeCheckSubQuery(value, sb, 0)
                        sb.append(", ")
                    }
                    sb.removeLast(2)
                    sb.append("), ")
                }
                sb.removeLast(2)
            }
            is PartiqlAst.DmlOp.InsertValue -> {
                sb.append("INSERT INTO ")
                writeAstNodeCheckSubQuery(dmlOp.target, sb, 0)
                sb.append(" VALUE ")
                writeAstNodeCheckSubQuery(dmlOp.value, sb, 0)
                dmlOp.index?.let {
                    sb.append(" AT ")
                    writeAstNodeCheckSubQuery(it, sb, 0)
                }
                dmlOp.onConflict?.let {
                    sb.append(" ON CONFLICT WHERE ")
                    writeAstNodeCheckSubQuery(it.expr, sb, 0)
                    when (it.conflictAction) {
                        is PartiqlAst.ConflictAction.DoNothing -> {
                            sb.append(" DO NOTHING")
                        }
                    }
                }
            }
            is PartiqlAst.DmlOp.Remove -> {
                sb.append("REMOVE ")
                writeAstNodeCheckSubQuery(dmlOp.target, sb, 0)
            }
            is PartiqlAst.DmlOp.Set -> {
                when (previousIsSet) {
                    true -> {
                        sb.removeLast(1) // Remove the last line breaker
                        sb.append(", ")
                    }
                    false -> sb.append("SET ")
                }
                writeAstNodeCheckSubQuery(dmlOp.assignment.target, sb, 0)
                sb.append(" = ")
                writeAstNodeCheckSubQuery(dmlOp.assignment.value, sb, 0)
            }
            is PartiqlAst.DmlOp.Delete -> error("DELETE clause has different syntax")
        }

        return dmlOp is PartiqlAst.DmlOp.Set
    }

    private fun writeReturning(returning: PartiqlAst.ReturningExpr, sb: StringBuilder) {
        sb.append("\nRETURNING ")
        returning.elems.forEach {
            when (it.mapping) {
                is PartiqlAst.ReturningMapping.ModifiedNew -> sb.append("MODIFIED NEW ")
                is PartiqlAst.ReturningMapping.ModifiedOld -> sb.append("MODIFIED OLD ")
                is PartiqlAst.ReturningMapping.AllNew -> sb.append("ALL NEW ")
                is PartiqlAst.ReturningMapping.AllOld -> sb.append("ALL OLD ")
            }
            when (it.column) {
                is PartiqlAst.ColumnComponent.ReturningWildcard -> sb.append('*')
                is PartiqlAst.ColumnComponent.ReturningColumn -> writeAstNode(it.column.expr, sb, 0)
            }
            sb.append(", ")
        }
        sb.removeLast(2)
    }

    // *********
    // * Query *
    // *********
    /**
     * @param node is the PIG AST node
     * @param sb is the StringBuilder where we write the pretty query according to the like of the parsed tree
     * @param level is an integer which marks how deep in the nested query we are. It increments Only when we step
     * into a Case or Select clause. -1 represents no formatting, which transforms the sub-query as a line string
     */
    private fun writeAstNode(node: PartiqlAst.Expr, sb: StringBuilder, level: Int) {
        when (node) {
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
            is PartiqlAst.Expr.Path -> writeAstNode(node, sb, level)
            is PartiqlAst.Expr.Call -> writeAstNode(node, sb, level)
            is PartiqlAst.Expr.CallAgg -> writeAstNode(node, sb, level)

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
    private fun writeAstNodeCheckSubQuery(node: PartiqlAst.Expr, sb: StringBuilder, level: Int) {
        when (isCaseOrSelect(node)) {
            true -> {
                val subQueryLevel = getSubQueryLevel(level)
                val separator = when (subQueryLevel == -1) {
                    true -> ""
                    false -> getSeparator(subQueryLevel)
                }
                sb.append("($separator")
                writeAstNode(node, sb, subQueryLevel)
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
        val value = when (node.value.type) {
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
            // Print anything as one line inside a bag
            writeAstNodeCheckSubQuery(it, sb, -1)
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
            // Print anything as one line inside a sexp
            writeAstNodeCheckSubQuery(it, sb, -1)
            sb.append(", ")
        }
        if (node.values.isNotEmpty()) {
            sb.removeLast(2)
        }
        sb.append(")")
    }

    private fun writeAstNode(node: PartiqlAst.Expr.List, sb: StringBuilder, level: Int) {
        sb.append("[ ")
        node.values.forEach {
            // Print anything as one line inside a list
            writeAstNodeCheckSubQuery(it, sb, -1)
            sb.append(", ")
        }
        if (node.values.isNotEmpty()) {
            sb.removeLast(2)
        }
        sb.append(" ]")
    }

    private fun writeAstNode(node: PartiqlAst.Expr.Struct, sb: StringBuilder, level: Int) {
        sb.append("{ ")
        node.fields.forEach {
            // Print anything as one line inside a struct
            writeAstNodeCheckSubQuery(it.first, sb, -1)
            sb.append(": ")
            writeAstNodeCheckSubQuery(it.second, sb, -1)
            sb.append(", ")
        }
        if (node.fields.isNotEmpty()) {
            sb.removeLast(2)
        }
        sb.append(" }")
    }

    private fun writeAstNode(node: PartiqlAst.Expr.Parameter, sb: StringBuilder) {
        sb.append("?")
    }

    private fun writeAstNode(node: PartiqlAst.Expr.Id, sb: StringBuilder) {
        when (node.case) {
            is PartiqlAst.CaseSensitivity.CaseSensitive -> sb.append("\"${node.name.text}\"")
            is PartiqlAst.CaseSensitivity.CaseInsensitive -> sb.append(node.name.text)
        }
    }

    private fun writeAstNode(node: PartiqlAst.Expr.Call, sb: StringBuilder, level: Int) {
        sb.append("${node.funcName.text}(")
        node.args.forEach { arg ->
            // Print anything as one line inside a function call
            writeAstNodeCheckSubQuery(arg, sb, -1)
            sb.append(", ")
        }
        if (node.args.isNotEmpty()) {
            sb.removeLast(2)
        }
        sb.append(')')
    }

    private fun writeAstNode(node: PartiqlAst.Expr.CallAgg, sb: StringBuilder, level: Int) {
        sb.append("${node.funcName.text}(")
        if (node.setq is PartiqlAst.SetQuantifier.Distinct) {
            sb.append("DISTINCT ")
        }
        // Print anything as one line inside aggregate function call
        writeAstNodeCheckSubQuery(node.arg, sb, -1)
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
        val separator = getSeparator(level)
        val sqLevel = getSubQueryLevel(level)
        sb.append("CASE ")
        // Print anything as one line inside a CASE clause
        writeAstNodeCheckSubQuery(node.expr, sb, -1)
        writeCaseWhenClauses(node.cases.pairs, sb, sqLevel)
        writeCaseElseClause(node.default, sb, sqLevel)
        sb.append("${separator}END")
    }

    private fun writeAstNode(node: PartiqlAst.Expr.SearchedCase, sb: StringBuilder, level: Int) {
        val separator = getSeparator(level)
        sb.append("CASE")
        writeCaseWhenClauses(node.cases.pairs, sb, level + 1)
        writeCaseElseClause(node.default, sb, level + 1)
        sb.append("${separator}END")
    }

    private fun writeCaseWhenClauses(pairs: List<PartiqlAst.ExprPair>, sb: StringBuilder, level: Int) {
        val separator = getSeparator(level)
        pairs.forEach { pair ->
            sb.append("${separator}WHEN ")
            writeAstNodeCheckSubQuery(pair.first, sb, -1)
            sb.append(" THEN ")
            writeAstNodeCheckSubQuery(pair.second, sb, -1)
        }
    }

    private fun writeCaseElseClause(default: PartiqlAst.Expr?, sb: StringBuilder, level: Int) {
        if (default != null) {
            val separator = getSeparator(level)
            sb.append("${separator}ELSE ")
            writeAstNodeCheckSubQuery(default, sb, -1)
        }
    }

    private fun writeAstNode(node: PartiqlAst.Expr.Select, sb: StringBuilder, level: Int) {
        val separator = getSeparator(level)

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
        sb.append("${separator}FROM ")
        writeFromSource(node.from, sb, level)

        // LET clause
        node.fromLet?.let {
            val sqLevel = getSubQueryLevel(level)
            val separator = getSeparator(sqLevel)
            sb.append("${separator}LET ")
            writeFromLet(it, sb, level)
        }

        // WHERE clause
        node.where?.let {
            sb.append("${separator}WHERE ")
            writeAstNodeCheckSubQuery(it, sb, level)
        }

        // GROUP clause
        node.group?.let {
            sb.append("${separator}GROUP ")
            writeGroupBy(it, sb, level)
        }

        // HAVING clause
        node.having?.let {
            sb.append("${separator}HAVING ")
            writeAstNodeCheckSubQuery(it, sb, level)
        }

        // ORDER BY clause
        node.order?.let { orderBy ->
            sb.append("${separator}ORDER BY ")
            orderBy.sortSpecs.forEach { sortSpec ->
                writeSortSpec(sortSpec, sb, level)
                sb.append(", ")
            }
            sb.removeLast(2)
        }

        // LIMIT clause
        node.limit?.let {
            sb.append("${separator}LIMIT ")
            writeAstNodeCheckSubQuery(it, sb, level)
        }

        // OFFSET clause
        node.offset?.let {
            sb.append("${separator}OFFSET ")
            writeAstNodeCheckSubQuery(it, sb, level)
        }
    }

    private fun writeSortSpec(sortSpec: PartiqlAst.SortSpec, sb: StringBuilder, level: Int) {
        writeAstNodeCheckSubQuery(sortSpec.expr, sb, level + 1)
        when (sortSpec.orderingSpec) {
            is PartiqlAst.OrderingSpec.Asc -> sb.append(" ASC")
            is PartiqlAst.OrderingSpec.Desc -> sb.append(" DESC")
        }
    }

    private fun writeGroupBy(group: PartiqlAst.GroupBy, sb: StringBuilder, level: Int) {
        when (group.strategy) {
            is PartiqlAst.GroupingStrategy.GroupFull -> sb.append("BY ")
            is PartiqlAst.GroupingStrategy.GroupPartial -> sb.append("PARTIAL BY ")
        }
        group.keyList.keys.forEach {
            writeGroupKey(it, sb, level)
            sb.append(", ")
        }
        sb.removeLast(2)
        val sqLevel = getSubQueryLevel(level)
        val separator = getSeparator(sqLevel)
        group.groupAsAlias?.let { sb.append("${separator}GROUP AS ${it.text}") }
    }

    private fun writeGroupKey(key: PartiqlAst.GroupKey, sb: StringBuilder, level: Int) {
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
        when (from) {
            is PartiqlAst.FromSource.Scan -> {
                writeAstNodeCheckSubQuery(from.expr, sb, level)
                from.asAlias?.let { sb.append(" AS ${it.text}") }
                from.atAlias?.let { sb.append(" AT ${it.text}") }
                from.byAlias?.let { sb.append(" BY ${it.text}") }
            }
            is PartiqlAst.FromSource.Join -> when {
                (from.type is PartiqlAst.JoinType.Inner && from.predicate == null) -> {
                    // This means we can use comma to separate JOIN left-hand side and right-hand side
                    writeFromSource(from.left, sb, level)
                    sb.append(", ")
                    writeFromSource(from.right, sb, level)
                }
                else -> {
                    val sqLevel = getSubQueryLevel(level)
                    val separator = getSeparator(sqLevel)
                    val join = when (from.type) {
                        is PartiqlAst.JoinType.Inner -> "JOIN"
                        is PartiqlAst.JoinType.Left -> "LEFT CROSS JOIN"
                        is PartiqlAst.JoinType.Right -> "RIGHT CROSS JOIN"
                        is PartiqlAst.JoinType.Full -> "FULL CROSS JOIN"
                    }
                    writeFromSource(from.left, sb, level)
                    sb.append("$separator$join ")
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
        when (isOperator(node)) {
            true -> {
                sb.append('(')
                writeAstNode(node, sb, level)
                sb.append(')')
            }
            // Print anything as one line inside an operator
            false -> writeAstNodeCheckSubQuery(node, sb, -1)
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
            // TODO: Support formatting CustomType
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
            // Write anything as one line as COALESCE arguments
            writeAstNodeCheckSubQuery(arg, sb, -1)
            sb.append(", ")
        }
        if (node.args.isNotEmpty()) {
            sb.removeLast(2)
        }
        sb.append(')')
    }

    private fun writeAstNode(node: PartiqlAst.Expr.NullIf, sb: StringBuilder, level: Int) {
        // Write anything as one line as COALESCE arguments
        sb.append("NULLIF(")
        writeAstNodeCheckSubQuery(node.expr1, sb, -1)
        sb.append(", ")
        writeAstNodeCheckSubQuery(node.expr2, sb, -1)
        sb.append(')')
    }

    private fun writeNAryOperator(operatorName: String, operands: List<PartiqlAst.Expr>, sb: StringBuilder, level: Int) {
        if (operands.size < 2) IllegalStateException("Internal Error: NAry operator $operatorName must have at least 2 operands")
        operands.forEach {
            writeAstNodeCheckOp(it, sb, level)
            sb.append(" $operatorName ")
        }
        sb.removeLast(operatorName.length + 2)
    }

    private fun isCaseOrSelect(node: PartiqlAst.Expr): Boolean =
        when (node) {
            is PartiqlAst.Expr.SimpleCase, is PartiqlAst.Expr.SearchedCase, is PartiqlAst.Expr.Select -> true
            else -> false
        }

    private fun isOperator(node: PartiqlAst.Expr): Boolean =
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

    // We need to add a line breaker and indent only for CASE and SELECT clauses.
    // If level is -1, this indicates there is no need for formatting
    private fun getSeparator(level: Int) =
        when (level == -1) {
            true -> " "
            false -> "\n${"\t".repeat(level)}"
        }

    private fun getSubQueryLevel(level: Int) =
        when (level == -1) {
            true -> -1
            false -> level + 1
        }

    private fun StringBuilder.removeLast(n: Int): StringBuilder {
        for (i in 1..n) {
            deleteCharAt(length - 1)
        }
        return this
    }
}
