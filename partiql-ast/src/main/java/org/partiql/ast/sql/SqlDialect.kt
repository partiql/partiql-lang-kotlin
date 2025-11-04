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

package org.partiql.ast.sql

import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.AstNode
import org.partiql.ast.AstVisitor
import org.partiql.ast.DataType
import org.partiql.ast.Exclude
import org.partiql.ast.ExcludePath
import org.partiql.ast.ExcludeStep
import org.partiql.ast.From
import org.partiql.ast.FromExpr
import org.partiql.ast.FromJoin
import org.partiql.ast.FromType
import org.partiql.ast.GroupBy
import org.partiql.ast.GroupByStrategy
import org.partiql.ast.Identifier
import org.partiql.ast.Identifier.Simple
import org.partiql.ast.Identifier.regular
import org.partiql.ast.IntervalQualifier
import org.partiql.ast.JoinType
import org.partiql.ast.Let
import org.partiql.ast.Literal
import org.partiql.ast.Nulls
import org.partiql.ast.Order
import org.partiql.ast.OrderBy
import org.partiql.ast.Query
import org.partiql.ast.QueryBody
import org.partiql.ast.SelectItem
import org.partiql.ast.SelectList
import org.partiql.ast.SelectPivot
import org.partiql.ast.SelectStar
import org.partiql.ast.SelectValue
import org.partiql.ast.SetOp
import org.partiql.ast.SetOpType
import org.partiql.ast.SetQuantifier
import org.partiql.ast.Sort
import org.partiql.ast.WindowClause
import org.partiql.ast.WindowFunctionType
import org.partiql.ast.WindowPartition
import org.partiql.ast.WindowSpecification
import org.partiql.ast.With
import org.partiql.ast.WithListElement
import org.partiql.ast.ddl.CreateTable
import org.partiql.ast.dml.Delete
import org.partiql.ast.dml.Insert
import org.partiql.ast.dml.Replace
import org.partiql.ast.dml.Update
import org.partiql.ast.dml.Upsert
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprAnd
import org.partiql.ast.expr.ExprArray
import org.partiql.ast.expr.ExprBag
import org.partiql.ast.expr.ExprBetween
import org.partiql.ast.expr.ExprBoolTest
import org.partiql.ast.expr.ExprCall
import org.partiql.ast.expr.ExprCase
import org.partiql.ast.expr.ExprCast
import org.partiql.ast.expr.ExprCoalesce
import org.partiql.ast.expr.ExprExtract
import org.partiql.ast.expr.ExprInCollection
import org.partiql.ast.expr.ExprIsType
import org.partiql.ast.expr.ExprLike
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprMissingPredicate
import org.partiql.ast.expr.ExprNot
import org.partiql.ast.expr.ExprNullIf
import org.partiql.ast.expr.ExprNullPredicate
import org.partiql.ast.expr.ExprOperator
import org.partiql.ast.expr.ExprOr
import org.partiql.ast.expr.ExprOverlaps
import org.partiql.ast.expr.ExprOverlay
import org.partiql.ast.expr.ExprParameter
import org.partiql.ast.expr.ExprPath
import org.partiql.ast.expr.ExprPosition
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprRowValue
import org.partiql.ast.expr.ExprSessionAttribute
import org.partiql.ast.expr.ExprStruct
import org.partiql.ast.expr.ExprSubstring
import org.partiql.ast.expr.ExprTrim
import org.partiql.ast.expr.ExprValues
import org.partiql.ast.expr.ExprVarRef
import org.partiql.ast.expr.ExprVariant
import org.partiql.ast.expr.ExprWindowFunction
import org.partiql.ast.expr.PathStep
import org.partiql.ast.expr.TruthValue

/**
 * SqlDialect represents the base behavior for transforming an [AstNode] tree into a [SqlBlock] tree.
 */
public abstract class SqlDialect : AstVisitor<SqlBlock, SqlBlock>() {

    public companion object {

        /**
         * Standard [AstNode] conversion with default parameters.
         */
        @JvmStatic
        public val STANDARD: SqlDialect = object : SqlDialect() {}
    }

    /**
     * Default entry-point, can also be us.
     */
    public fun transform(node: AstNode): SqlBlock {
        val head = SqlBlock.none()
        node.accept(this, head)
        return head
    }

    override fun defaultReturn(node: AstNode, tail: SqlBlock): SqlBlock =
        throw UnsupportedOperationException("Cannot print $node")

    // STATEMENTS

    override fun visitQuery(node: Query, tail: SqlBlock): SqlBlock = visitExpr(node.expr, tail)

    // IDENTIFIERS & PATHS

    /**
     * Default behavior is to wrap all SFW queries with parentheses.
     *
     * @param node
     * @param tail
     */
    public open fun visitExprWrapped(node: Expr, tail: SqlBlock, operator: Boolean = false): SqlBlock = when {
        node is ExprQuerySet -> {
            var t = tail
            t = t concat "("
            t = visit(node, t)
            t = t concat ")"
            t
        }
        node is ExprOperator && operator -> {
            var t = tail
            t = t concat "("
            t = visit(node, t)
            t = t concat ")"
            t
        }
        else -> visitExpr(node, tail)
    }

    override fun visitIdentifierSimple(node: Simple, tail: SqlBlock): SqlBlock = tail concat node.sql()

    override fun visitIdentifier(node: Identifier, tail: SqlBlock): SqlBlock {
        val path = when (node.hasQualifier()) {
            true -> {
                val qualifier = node.qualifier.fold("") { acc, part -> acc + "${part.sql()}." }
                qualifier + node.identifier.sql()
            }
            false -> node.identifier.sql()
        }
        return tail concat path
    }

    override fun visitExclude(node: Exclude, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat " EXCLUDE "
        t = t concat list(start = null, end = null) { node.excludePaths }
        return t
    }

    override fun visitExcludePath(node: ExcludePath, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprVarRef(node.root, t)
        t = t concat list(delimiter = null, start = null, end = null) { node.excludeSteps }
        return t
    }

    override fun visitExcludeStepCollIndex(node: ExcludeStep.CollIndex, tail: SqlBlock): SqlBlock {
        return tail concat "[${node.index}]"
    }

    override fun visitExcludeStepStructWildcard(node: ExcludeStep.StructWildcard, tail: SqlBlock): SqlBlock {
        return tail concat ".*"
    }

    override fun visitExcludeStepStructField(node: ExcludeStep.StructField, tail: SqlBlock): SqlBlock {
        var t = tail concat "."
        t = visitIdentifierSimple(node.symbol, t)
        return t
    }

    override fun visitExcludeStepCollWildcard(node: ExcludeStep.CollWildcard, tail: SqlBlock): SqlBlock {
        return tail concat "[*]"
    }

    // Window Function Methods
    override fun visitExprWindowFunction(node: ExprWindowFunction, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitWindowFunctionType(node.functionType, t)
        t = t concat " OVER "
        t = visitWindowSpecification(node.windowSpecification, t)
        return t
    }

    override fun visitWindowFunctionType(node: WindowFunctionType, tail: SqlBlock): SqlBlock {
        return node.accept(this, tail)
    }

    override fun visitWindowFunctionTypeRank(node: WindowFunctionType.Rank, tail: SqlBlock): SqlBlock {
        return tail concat "RANK()"
    }

    override fun visitWindowFunctionTypeDenseRank(node: WindowFunctionType.DenseRank, tail: SqlBlock): SqlBlock {
        return tail concat "DENSE_RANK()"
    }

    override fun visitWindowFunctionTypePercentRank(node: WindowFunctionType.PercentRank, tail: SqlBlock): SqlBlock {
        return tail concat "PERCENT_RANK()"
    }

    override fun visitWindowFunctionTypeCumeDist(node: WindowFunctionType.CumeDist, tail: SqlBlock): SqlBlock {
        return tail concat "CUME_DIST()"
    }

    override fun visitWindowFunctionTypeRowNumber(node: WindowFunctionType.RowNumber, tail: SqlBlock): SqlBlock {
        return tail concat "ROW_NUMBER()"
    }

    override fun visitWindowFunctionTypeLead(node: WindowFunctionType.Lead, tail: SqlBlock): SqlBlock {
        var t = tail concat "LEAD("
        t = visitExpr(node.extent, t)
        node.offset?.let {
            t = t concat ", $it"
        }

        node.defaultValue.let { defaultValue ->
            t = t concat ", "
            t = visitExpr(defaultValue, t)
        }

        t = t concat ")"
        node.nullTreatment?.let { nullTreatment ->
            t = t concat " ${nullTreatment.name()}"
        }

        return t
    }

    override fun visitWindowFunctionTypeLag(node: WindowFunctionType.Lag, tail: SqlBlock): SqlBlock {
        var t = tail concat "LAG("
        t = visitExpr(node.extent, t)
        node.offset?.let {
            t = t concat ", $it"
        }

        node.defaultValue.let { defaultValue ->
            t = t concat ", "
            t = visitExpr(defaultValue, t)
        }

        t = t concat ")"
        node.nullTreatment?.let { nullTreatment ->
            t = t concat " ${nullTreatment.name()}"
        }
        return t
    }

    override fun visitWindowPartition(node: WindowPartition, tail: SqlBlock): SqlBlock {
        return visitExprWrapped(node.columnReference, tail)
    }

    override fun visitWindowSpecification(node: WindowSpecification, tail: SqlBlock): SqlBlock {
        var t = tail
        if (node.existingName != null) {
            t = visitIdentifierSimple(node.existingName!!, t)
            if (!node.partitionClause.isNullOrEmpty() || node.orderClause != null) {
                t = t concat " "
            }
        } else {
            t = t concat "("
            // PARTITION BY clause
            if (!node.partitionClause.isNullOrEmpty()) {
                t = t concat "PARTITION BY "
                t = t concat list(start = null, end = null) { node.partitionClause!! }
                t = t concat " "
            }

            // ORDER BY clause
            node.orderClause?.let { orderClause ->
                t = visitOrderBy(orderClause, t)
            }
            t = t concat ")"
        }

        return t
    }

    override fun visitWindowClause(node: WindowClause, tail: SqlBlock): SqlBlock {
        var t = tail concat "WINDOW "
        t = t concat list(start = null, end = null) { node.definitions }
        return t
    }

    override fun visitWindowDefinition(node: WindowClause.Definition, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitIdentifierSimple(node.name, t)
        t = t concat " AS "
        t = visitWindowSpecification(node.specification, t)
        return t
    }

    // TYPES
    override fun visitDataType(node: DataType, tail: SqlBlock): SqlBlock {
        return when (node.code()) {
            // <character string type>
            //   no params
            DataType.STRING, DataType.SYMBOL -> tail concat node.name()
            //   with params
            DataType.CHARACTER, DataType.CHAR, DataType.VARCHAR, DataType.CLOB -> tail concat type(node.name(), node.length)
            DataType.CHARACTER_VARYING -> tail concat type("CHARACTER VARYING", node.length)
            DataType.CHAR_VARYING -> tail concat type("CHAR VARYING", node.length)
            DataType.CHARACTER_LARGE_OBJECT -> tail concat type("CHARACTER LARGE OBJECT", node.length)
            DataType.CHAR_LARGE_OBJECT -> tail concat type("CHAR LARGE OBJECT", node.length)
            // <binary large object string type>
            DataType.BLOB -> tail concat type(node.name(), node.length)
            DataType.BINARY_LARGE_OBJECT -> tail concat type("BINARY LARGE OBJECT", node.length)
            // <bit string type>
            DataType.BIT -> tail concat type(node.name(), node.length)
            DataType.BIT_VARYING -> tail concat type("BIT VARYING", node.length)
            // <numeric type> - <exact numeric type>
            //   no params
            DataType.BIGINT, DataType.INT8, DataType.INTEGER8, DataType.INT4, DataType.INTEGER4, DataType.INTEGER,
            DataType.INT, DataType.INT2, DataType.INTEGER2, DataType.SMALLINT, DataType.TINYINT -> tail concat node.name()
            //   with params
            DataType.NUMERIC, DataType.DECIMAL, DataType.DEC -> tail concat type(node.name(), node.precision, node.scale)
            // <numeric type> - <approximate numeric type>
            DataType.FLOAT, DataType.REAL -> tail concat node.name()
            DataType.DOUBLE_PRECISION -> tail concat "DOUBLE PRECISION"
            // <boolean type>
            DataType.BOOLEAN, DataType.BOOL -> tail concat node.name()
            // <datetime type>
            //   no params
            DataType.DATE -> tail concat node.name()
            //   with params
            DataType.TIME, DataType.TIMESTAMP -> tail concat type(node.name(), node.precision, gap = true)
            DataType.TIME_WITH_TIME_ZONE -> tail concat type("TIME", node.precision, gap = true) concat(" WITH TIME ZONE")
            DataType.TIMESTAMP_WITH_TIME_ZONE -> tail concat type("TIMESTAMP", node.precision, gap = true) concat(" WITH TIME ZONE")
            // <interval type>
            DataType.INTERVAL -> visit(node.intervalQualifier, tail concat "INTERVAL ")
            // <container type>
            DataType.STRUCT, DataType.TUPLE -> tail concat node.name()
            // <collection type>
            DataType.LIST, DataType.BAG, DataType.SEXP -> tail concat node.name()
            // <user defined type>
            DataType.USER_DEFINED -> visitIdentifier(node.name, tail)
            else -> defaultReturn(node, tail)
        }
    }

    // Expressions

    override fun visitExprLit(node: ExprLit, tail: SqlBlock): SqlBlock {
        val lit = node.lit
        var t = tail
        when (lit.code()) {
            Literal.NULL -> t = t concat "NULL"
            Literal.MISSING -> t = t concat "MISSING"
            Literal.BOOL -> t = t concat lit.booleanValue().toString()
            Literal.APPROX_NUM, Literal.EXACT_NUM, Literal.INT_NUM -> t = t concat lit.numberValue()
            Literal.STRING -> t = t concat "'${lit.stringValue().replace("'", "''")}'"
            Literal.TYPED_STRING -> {
                val dataType = lit.dataType()
                when (dataType.code()) {
                    DataType.INTERVAL -> {
                        t = t concat "INTERVAL '${lit.stringValue()}' "
                        t = visit(dataType.intervalQualifier, t)
                    }
                    else -> {
                        t = visitDataType(lit.dataType(), t)
                        t = t concat String.format(" '%s'", lit.stringValue())
                    }
                }
            }
            else -> error("Unsupported literal type $lit")
        }
        return t
    }

    override fun visitIntervalQualifierSingle(node: IntervalQualifier.Single, tail: SqlBlock): SqlBlock {
        var singleField = node.field.name()
        if (node.precision != null) {
            singleField += if (node.fractionalPrecision != null) {
                " (${node.precision}, ${node.fractionalPrecision})"
            } else {
                " (${node.precision})"
            }
        }
        return tail concat singleField
    }

    override fun visitIntervalQualifierRange(node: IntervalQualifier.Range, tail: SqlBlock): SqlBlock {
        val startField = node.startField
        val endField = node.endField
        var datetimeField = startField.name()
        if (node.startFieldPrecision != null) {
            datetimeField += " (${node.startFieldPrecision})"
        }
        datetimeField += " TO ${endField.name()}"
        if (node.endFieldFractionalPrecision != null) {
            datetimeField += " (${node.endFieldFractionalPrecision})"
        }
        return tail concat datetimeField
    }

    override fun visitExprVariant(node: ExprVariant, tail: SqlBlock): SqlBlock {
        if (node.encoding != "ion") {
            error("Unsupported encoding ${node.encoding}")
        }
        val value = node.value
        return tail concat "`$value`"
    }

    override fun visitExprOperator(node: ExprOperator, tail: SqlBlock): SqlBlock {
        val lhs = node.lhs
        return if (lhs != null) {
            var t = tail
            t = visitExprWrapped(lhs, t, operator = true)
            t = t concat " ${node.symbol} "
            t = visitExprWrapped(node.rhs, t, operator = true)
            t
        } else {
            var t = tail
            t = t concat node.symbol
            t = visitExprWrapped(node.rhs, t, operator = true)
            return t
        }
    }

    override fun visitExprAnd(node: ExprAnd, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.lhs, t)
        t = t concat " AND "
        t = visitExprWrapped(node.rhs, t)
        return t
    }

    override fun visitExprOr(node: ExprOr, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.lhs, t)
        t = t concat " OR "
        t = visitExprWrapped(node.rhs, t)
        return t
    }

    override fun visitExprNot(node: ExprNot, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "NOT ("
        t = visitExprWrapped(node.value, t)
        t = t concat ")"
        return t
    }

    override fun visitExprVarRef(node: ExprVarRef, tail: SqlBlock): SqlBlock {
        var t = tail
        // Prepend @
        if (node.isQualified) {
            t = t concat "@"
        }
        t = visitIdentifier(node.identifier, t)
        return t
    }

    override fun visitExprSessionAttribute(node: ExprSessionAttribute, tail: SqlBlock): SqlBlock =
        tail concat node.sessionAttribute.name()

    override fun visitExprPath(node: ExprPath, tail: SqlBlock): SqlBlock {
        var t = visitExprWrapped(node.root, tail)
        t = t concat list(delimiter = null, start = null, end = null) { node.steps }
        return t
    }

    override fun visitPathStepField(node: PathStep.Field, tail: SqlBlock): SqlBlock =
        tail concat ".${node.field.sql()}"

    override fun visitPathStepElement(node: PathStep.Element, tail: SqlBlock): SqlBlock {
        var t = tail
        val key = node.element
        // use [ ] syntax
        t = t concat "["
        t = visitExprWrapped(key, t)
        t = t concat "]"
        return t
    }

    override fun visitPathStepAllElements(node: PathStep.AllElements, tail: SqlBlock): SqlBlock = tail concat "[*]"

    override fun visitPathStepAllFields(node: PathStep.AllFields, tail: SqlBlock): SqlBlock = tail concat ".*"

    override fun visitExprCall(node: ExprCall, tail: SqlBlock): SqlBlock {
        var t = tail
        val f = node.function
        // Special case -- COUNT() maps to COUNT(*)
        if (!f.hasQualifier() && f.identifier.text.uppercase() == "COUNT" && node.args.isEmpty()) {
            return t concat "COUNT(*)"
        }
        // Special case -- DATE_ADD('<datetime_field>', <lhs>, <rhs>) -> DATE_ADD(<datetime_field>, <lhs>, <rhs>)
        // Special case -- DATE_DIFF('<datetime_field>', <lhs>, <rhs>) -> DATE_DIFF(<datetime_field>, <lhs>, <rhs>)
        if (!f.hasQualifier() &&
            (f.identifier.text.uppercase() == "DATE_ADD" || f.identifier.text.uppercase() == "DATE_DIFF") &&
            node.args.size == 3
        ) {
            val dtField = (node.args[0] as ExprLit).lit.stringValue()
            // Represent as an `ExprVarRef` to mimic a literal symbol.
            // TODO consider some other representation for unquoted strings
            val newArgs = listOf(exprVarRef(regular(dtField), isQualified = false)) + node.args.drop(1)
            t = visitIdentifier(f, t)
            t = t concat list { newArgs }
            return t
        }
        val setq = node.setq
        val start = if (setq != null) "(${setq.name()} " else "("
        t = visitIdentifier(f, t)
        t = t concat list(start) { node.args }
        return t
    }

    override fun visitExprParameter(node: ExprParameter, tail: SqlBlock): SqlBlock = tail concat "?"

    override fun visitExprValues(node: ExprValues, tail: SqlBlock): SqlBlock =
        tail concat list(start = "VALUES ", end = "") { node.rows }

    override fun visitExprRowValue(node: ExprRowValue, tail: SqlBlock): SqlBlock = tail concat list { node.values }

    override fun visitExprBag(node: ExprBag, tail: SqlBlock): SqlBlock {
        return tail concat list("<<", ">>") { node.values }
    }

    override fun visitExprArray(node: ExprArray, tail: SqlBlock): SqlBlock {
        return tail concat list("[", "]") { node.values }
    }

    override fun visitExprStruct(node: ExprStruct, tail: SqlBlock): SqlBlock =
        tail concat list("{", "}") { node.fields }

    override fun visitExprStructField(node: ExprStruct.Field, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.name, t)
        t = t concat ": "
        t = visitExprWrapped(node.value, t)
        return t
    }

    override fun visitExprLike(node: ExprLike, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.value, t)
        t = t concat if (node.isNot) " NOT LIKE " else " LIKE "
        t = visitExprWrapped(node.pattern, t)
        val escape = node.escape
        if (escape != null) {
            t = t concat " ESCAPE "
            t = visitExprWrapped(escape, t)
        }
        return t
    }

    override fun visitExprBetween(node: ExprBetween, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.value, t)
        t = t concat if (node.isNot) " NOT BETWEEN " else " BETWEEN "
        t = visitExprWrapped(node.from, t)
        t = t concat " AND "
        t = visitExprWrapped(node.to, t)
        return t
    }

    override fun visitExprInCollection(node: ExprInCollection, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.lhs, t)
        t = t concat if (node.isNot) " NOT IN " else " IN "
        t = visitExprWrapped(node.rhs, t)
        return t
    }

    override fun visitExprNullPredicate(node: ExprNullPredicate, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.value, t)
        t = t concat if (node.isNot) " IS NOT NULL" else " IS NULL"
        return t
    }

    override fun visitExprMissingPredicate(node: ExprMissingPredicate, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.value, t)
        t = t concat if (node.isNot) " IS NOT MISSING" else " IS MISSING"
        return t
    }

    override fun visitExprBoolTest(node: ExprBoolTest, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.value, t)
        t = t concat if (node.isNot) " IS NOT " else " IS "
        t = t concat when (node.truthValue.code()) {
            TruthValue.TRUE -> "TRUE"
            TruthValue.FALSE -> "FALSE"
            TruthValue.UNKNOWN -> "UNKNOWN"
            else -> throw UnsupportedOperationException("Cannot print $node")
        }
        return t
    }

    override fun visitExprIsType(node: ExprIsType, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.value, t)
        t = t concat if (node.isNot) " IS NOT " else " IS "
        t = visitDataType(node.type, t)
        return t
    }

    override fun visitExprCase(node: ExprCase, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "CASE"
        t = when (val expr = node.expr) {
            null -> t
            else -> visitExprWrapped(expr, t concat " ")
        }
        // WHEN(s)
        t = node.branches.fold(t) { acc, branch -> visitExprCaseBranch(branch, acc) }
        // ELSE
        t = when (val defaultExpr = node.defaultExpr) {
            null -> t
            else -> {
                t = t concat " ELSE "
                visitExprWrapped(defaultExpr, t)
            }
        }
        t = t concat " END"
        return t
    }

    override fun visitExprCaseBranch(node: ExprCase.Branch, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat " WHEN "
        t = visitExprWrapped(node.condition, t)
        t = t concat " THEN "
        t = visitExprWrapped(node.expr, t)
        return t
    }

    override fun visitExprCoalesce(node: ExprCoalesce, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "COALESCE"
        t = t concat list { node.args }
        return t
    }

    override fun visitExprNullIf(node: ExprNullIf, tail: SqlBlock): SqlBlock {
        val args = listOf(node.v1, node.v2)
        var t = tail
        t = t concat "NULLIF"
        t = t concat list { args }
        return t
    }

    override fun visitExprSubstring(node: ExprSubstring, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "SUBSTRING("
        t = visitExprWrapped(node.value, t)
        val start = node.start
        if (start != null) {
            t = t concat " FROM "
            t = visitExprWrapped(start, t)
        }
        val length = node.length
        if (length != null) {
            t = t concat " FOR "
            t = visitExprWrapped(length, t)
        }
        t = t concat ")"
        return t
    }

    override fun visitExprPosition(node: ExprPosition, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "POSITION("
        t = visitExprWrapped(node.lhs, t)
        t = t concat " IN "
        t = visitExprWrapped(node.rhs, t)
        t = t concat ")"
        return t
    }

    override fun visitExprTrim(node: ExprTrim, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "TRIM("
        // [LEADING|TRAILING|BOTH]
        val trimSpec = node.trimSpec
        val chars = node.chars
        when {
            trimSpec != null && chars != null -> {
                t = t concat trimSpec.name()
                t = t concat " "
                t = visitExprWrapped(chars, t)
                t = t concat " FROM "
            }
            trimSpec != null -> {
                t = t concat trimSpec.name()
                t = t concat " FROM "
            }
            chars != null -> {
                t = visitExprWrapped(chars, t)
                t = t concat " FROM "
            }
        }
        t = visitExprWrapped(node.value, t)
        t = t concat ")"
        return t
    }

    override fun visitExprOverlay(node: ExprOverlay, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "OVERLAY("
        t = visitExprWrapped(node.value, t)
        t = t concat " PLACING "
        t = visitExprWrapped(node.placing, t)
        t = t concat " FROM "
        t = visitExprWrapped(node.from, t)
        val forLength = node.forLength
        if (forLength != null) {
            t = t concat " FOR "
            t = visitExprWrapped(forLength, t)
        }
        t = t concat ")"
        return t
    }

    override fun visitExprExtract(node: ExprExtract, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "EXTRACT("
        t = t concat node.field.name()
        t = t concat " FROM "
        t = visitExprWrapped(node.source, t)
        t = t concat ")"
        return t
    }

    override fun visitExprCast(node: ExprCast, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "CAST("
        t = visitExprWrapped(node.value, t)
        t = t concat " AS "
        t = visitDataType(node.asType, t)
        t = t concat ")"
        return t
    }

    override fun visitExprOverlaps(node: ExprOverlaps, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.lhs, t)
        t = t concat " OVERLAPS "
        t = visitExprWrapped(node.rhs, t)
        return t
    }

    override fun visitExprQuerySet(node: ExprQuerySet, tail: SqlBlock): SqlBlock {
        var t = tail
        t = if (node.with != null) visitWith(node.with!!, t) else t
        // visit body (SFW or other SQL set op)
        t = visit(node.body, t)
        // ORDER BY
        val orderBy = node.orderBy
        t = if (orderBy != null) visitOrderBy(orderBy, t concat " ") else t
        // LIMIT
        val limit = node.limit
        t = if (limit != null) visitExprWrapped(limit, t concat " LIMIT ") else t
        // OFFSET
        val offset = node.offset
        t = if (offset != null) visitExprWrapped(offset, t concat " OFFSET ") else t
        return t
    }

    override fun visitWith(node: With, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "WITH "
        t = if (node.isRecursive) {
            t concat "RECURSIVE "
        } else {
            t
        }
        t = t concat list("", " ") { node.elements }
        return t
    }

    override fun visitWithListElement(node: WithListElement, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visit(node.queryName, t)
        t = node.columnList?.let { columns -> t concat list(" (", ")") { columns } } ?: t
        t = t concat " AS "
        t = visitExprWrapped(node.asQuery, t)
        return t
    }

    // SELECT-FROM-WHERE

    override fun visitQueryBodySFW(node: QueryBody.SFW, tail: SqlBlock): SqlBlock {
        var t = tail
        // SELECT
        t = visit(node.select, t)
        // EXCLUDE
        t = node.exclude?.let { visit(it, t) } ?: t
        // FROM
        t = visitFrom(node.from, t concat " FROM ")
        // LET
        val let = node.let
        t = if (let != null) visitLet(let, t concat " ") else t
        // WHERE
        val where = node.where
        t = if (where != null) visitExprWrapped(where, t concat " WHERE ") else t
        // GROUP BY
        val groupBy = node.groupBy
        t = if (groupBy != null) visitGroupBy(groupBy, t concat " ") else t
        // HAVING
        val having = node.having
        t = if (having != null) visitExprWrapped(having, t concat " HAVING ") else t
        // WINDOW
        val window = node.window
        t = if (window != null) visitWindowClause(window, t concat " ") else t
        return t
    }

    override fun visitQueryBodySetOp(node: QueryBody.SetOp, tail: SqlBlock): SqlBlock {
        val op = mutableListOf<String>()
        when (node.isOuter) {
            true -> op.add("OUTER")
            else -> {}
        }
        when (node.type.setOpType.code()) {
            SetOpType.UNION -> op.add("UNION")
            SetOpType.INTERSECT -> op.add("INTERSECT")
            SetOpType.EXCEPT -> op.add("EXCEPT")
            else -> defaultReturn(node, tail)
        }
        when (node.type.setq?.code()) {
            SetQuantifier.ALL -> op.add("ALL")
            SetQuantifier.DISTINCT -> op.add("DISTINCT")
            null -> {}
            else -> defaultReturn(node, tail)
        }
        var t = tail
        t = visitExprWrapped(node.lhs, t)
        t = t concat " ${op.joinToString(" ")} "
        t = visitExprWrapped(node.rhs, t)
        return t
    }

    // SELECT

    override fun visitSelectStar(node: SelectStar, tail: SqlBlock): SqlBlock {
        val select = when (node.setq?.code()) {
            SetQuantifier.ALL -> "SELECT ALL *"
            SetQuantifier.DISTINCT -> "SELECT DISTINCT *"
            null -> "SELECT *"
            else -> throw UnsupportedOperationException("Cannot print $node")
        }
        return tail concat select
    }

    override fun visitSelectList(node: SelectList, tail: SqlBlock): SqlBlock {
        val select = when (node.setq?.code()) {
            SetQuantifier.ALL -> "SELECT ALL "
            SetQuantifier.DISTINCT -> "SELECT DISTINCT "
            null -> "SELECT "
            else -> throw UnsupportedOperationException("Cannot print $node")
        }
        return tail concat list(select, "") { node.items }
    }

    override fun visitSelectItemStar(node: SelectItem.Star, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        t = t concat ".*"
        return t
    }

    override fun visitSelectItemExpr(node: SelectItem.Expr, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        val asAlias = node.asAlias
        t = if (asAlias != null) t concat " AS ${asAlias.sql()}" else t
        return t
    }

    override fun visitSelectPivot(node: SelectPivot, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat "PIVOT "
        t = visitExprWrapped(node.key, t)
        t = t concat " AT "
        t = visitExprWrapped(node.value, t)
        return t
    }

    override fun visitSelectValue(node: SelectValue, tail: SqlBlock): SqlBlock {
        val select = when (node.setq?.code()) {
            SetQuantifier.ALL -> "SELECT ALL VALUE "
            SetQuantifier.DISTINCT -> "SELECT DISTINCT VALUE "
            null -> "SELECT VALUE "
            else -> throw UnsupportedOperationException("Cannot print $node")
        }
        var t = tail
        t = t concat select
        t = visitExprWrapped(node.constructor, t)
        return t
    }

    // FROM
    override fun visitFrom(node: From, tail: SqlBlock): SqlBlock {
        return tail concat list("", "") { node.tableRefs }
    }

    override fun visitFromExpr(node: FromExpr, tail: SqlBlock): SqlBlock {
        var t = tail
        t = when (node.fromType.code()) {
            FromType.SCAN -> t
            FromType.UNPIVOT -> t concat "UNPIVOT "
            else -> defaultReturn(node, tail)
        }
        t = visitExprWrapped(node.expr, t)
        val asAlias = node.asAlias
        t = if (asAlias != null) t concat " AS ${asAlias.sql()}" else t
        val atAlias = node.atAlias
        t = if (atAlias != null) t concat " AT ${atAlias.sql()}" else t
        return t
    }

    override fun visitFromJoin(node: FromJoin, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitFromTableRef(node.lhs, t)
        t = t concat when (node.joinType?.code()) {
            JoinType.INNER -> " INNER JOIN "
            JoinType.LEFT -> " LEFT JOIN "
            JoinType.LEFT_OUTER -> " LEFT OUTER JOIN "
            JoinType.RIGHT -> " RIGHT JOIN "
            JoinType.RIGHT_OUTER -> " RIGHT OUTER JOIN "
            JoinType.FULL -> " FULL JOIN "
            JoinType.FULL_OUTER -> " FULL OUTER JOIN "
            JoinType.CROSS -> " CROSS JOIN "
            JoinType.LEFT_CROSS -> " LEFT CROSS JOIN "
            null -> " JOIN "
            else -> throw UnsupportedOperationException("Cannot print $node")
        }
        t = visitFromTableRef(node.rhs, t)
        t = if (node.condition != null) visit(node.condition, t concat " ON ") else t
        return t
    }

    // LET

    override fun visitLet(node: Let, tail: SqlBlock): SqlBlock = tail concat list("LET ", "") { node.bindings }

    override fun visitLetBinding(node: Let.Binding, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        t = t concat " AS ${node.asAlias.sql()}"
        return t
    }

    // GROUP BY

    override fun visitGroupBy(node: GroupBy, tail: SqlBlock): SqlBlock {
        var t = tail
        t = t concat when (node.strategy.code()) {
            GroupByStrategy.FULL -> "GROUP BY "
            GroupByStrategy.PARTIAL -> "GROUP PARTIAL BY "
            else -> throw UnsupportedOperationException("Cannot print $node")
        }
        t = t concat list("", "") { node.keys }
        val asAlias = node.asAlias
        t = if (asAlias != null) t concat " GROUP AS ${asAlias.sql()}" else t
        return t
    }

    override fun visitGroupByKey(node: GroupBy.Key, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        val asAlias = node.asAlias
        t = if (asAlias != null) t concat " AS ${asAlias.sql()}" else t
        return t
    }

    // SET OPERATORS

    override fun visitSetOp(node: SetOp, tail: SqlBlock): SqlBlock {
        val op = when (val setq = node.setq) {
            null -> node.setOpType.name()
            else -> "${node.setOpType.name()} ${setq.name()}"
        }
        return tail concat op
    }

    // ORDER BY

    override fun visitOrderBy(node: OrderBy, tail: SqlBlock): SqlBlock =
        tail concat list("ORDER BY ", "") { node.sorts }

    override fun visitSort(node: Sort, tail: SqlBlock): SqlBlock {
        var t = tail
        t = visitExprWrapped(node.expr, t)
        t = when (node.order?.code()) {
            Order.ASC -> t concat " ASC"
            Order.DESC -> t concat " DESC"
            null -> t
            else -> throw UnsupportedOperationException("Cannot print $node")
        }
        t = when (node.nulls?.code()) {
            Nulls.FIRST -> t concat " NULLS FIRST"
            Nulls.LAST -> t concat " NULLS LAST"
            null -> t
            else -> throw UnsupportedOperationException("Cannot print $node")
        }
        return t
    }

    // TODO: DDL
    override fun visitCreateTable(node: CreateTable, ctx: SqlBlock): SqlBlock {
        throw UnsupportedOperationException("CREATE TABLE has not been supported yet in SqlDialect")
    }

    override fun visitInsert(node: Insert?, ctx: SqlBlock?): SqlBlock {
        throw UnsupportedOperationException("INSERT has not been supported yet in SqlDialect")
    }

    override fun visitDelete(node: Delete?, ctx: SqlBlock?): SqlBlock {
        throw UnsupportedOperationException("DELETE has not been supported yet in SqlDialect")
    }

    override fun visitUpsert(node: Upsert?, ctx: SqlBlock?): SqlBlock {
        throw UnsupportedOperationException("UPSERT has not been supported yet in SqlDialect")
    }

    override fun visitReplace(node: Replace?, ctx: SqlBlock?): SqlBlock {
        throw UnsupportedOperationException("REPLACE has not been supported yet in SqlDialect")
    }

    override fun visitUpdate(node: Update?, ctx: SqlBlock?): SqlBlock {
        throw UnsupportedOperationException("UPDATE has not been supported yet in SqlDialect")
    }

    // --- Block Constructor Helpers

    private infix fun SqlBlock.concat(rhs: String): SqlBlock {
        next = SqlBlock.Text(rhs)
        return next!!
    }

    private infix fun SqlBlock.concat(rhs: SqlBlock): SqlBlock {
        next = rhs
        return next!!
    }

    private fun type(symbol: String, vararg args: Int?, gap: Boolean = false): SqlBlock {
        val p = args.filterNotNull()
        val t = when {
            p.isEmpty() -> symbol
            else -> {
                val a = p.joinToString(",")
                when (gap) {
                    true -> "$symbol ($a)"
                    else -> "$symbol($a)"
                }
            }
        }
        // types are modeled as text; as we don't way to reflow
        return SqlBlock.Text(t)
    }

    private fun list(
        start: String? = "(",
        end: String? = ")",
        delimiter: String? = ", ",
        children: () -> List<AstNode>,
    ): SqlBlock {
        val kids = children()
        val h = SqlBlock.none()
        var t = h
        kids.forEachIndexed { i, child ->
            t = child.accept(this, t)
            t = if (delimiter != null && (i + 1) < kids.size) t concat delimiter else t
        }
        return SqlBlock.Nest(
            prefix = start,
            postfix = end,
            child = h,
        )
    }

    private fun Simple.sql() = when (isRegular) {
        true -> text // verbatim ..
        false -> "\"$text\""
    }
}
