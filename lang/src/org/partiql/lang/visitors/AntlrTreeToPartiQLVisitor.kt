/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at:
 *
 *      http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */

package org.partiql.lang.visitors

import com.amazon.ion.IonSystem
import com.amazon.ionelement.api.toIonElement
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.generated.PartiQLBaseVisitor
import org.partiql.lang.generated.PartiQLParser
import org.partiql.lang.util.bigDecimalOf
import org.partiql.lang.util.getPrecisionFromTimeString
import java.math.BigInteger
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AntlrTreeToPartiQLVisitor(val ion: IonSystem) : PartiQLBaseVisitor<PartiqlAst.PartiqlAstNode>() {

    override fun visitSelectFromWhere(ctx: PartiQLParser.SelectFromWhereContext): PartiqlAst.PartiqlAstNode {
        val projection = visit(ctx.selectClause()) as PartiqlAst.Projection
        val strategy = getSetQuantifierStrategy(ctx.selectClause())
        val from = visit(ctx.fromClause()) as PartiqlAst.FromSource
        val select = PartiqlAst.BUILDER().select(project = projection, from = from, setq = strategy)
        return PartiqlAst.BUILDER().query(select)
    }

    override fun visitSelectAll(ctx: PartiQLParser.SelectAllContext): PartiqlAst.PartiqlAstNode = PartiqlAst.BUILDER().projectStar()

    override fun visitSelectItems(ctx: PartiQLParser.SelectItemsContext): PartiqlAst.PartiqlAstNode = visit(ctx.projectionItems())

    override fun visitProjectionItems(ctx: PartiQLParser.ProjectionItemsContext): PartiqlAst.PartiqlAstNode {
        val projections = ctx.projectionItem().map { projection -> visit(projection) as PartiqlAst.ProjectItem }
        return PartiqlAst.BUILDER().projectList(projections)
    }

    override fun visitProjectionItem(ctx: PartiQLParser.ProjectionItemContext): PartiqlAst.PartiqlAstNode {
        val expr = visit(ctx.exprQuery()) as PartiqlAst.Expr
        return PartiqlAst.BUILDER().projectExpr(expr)
    }

    // TODO: Probably redo
    override fun visitSymbolIdentifierQuoted(ctx: PartiQLParser.SymbolIdentifierQuotedContext): PartiqlAst.PartiqlAstNode =
        PartiqlAst.BUILDER().id(ctx.IDENTIFIER_QUOTED().text.toPartiQLIdentifier(), PartiqlAst.CaseSensitivity.CaseSensitive(), PartiqlAst.ScopeQualifier.LocalsFirst())

    // TODO: Probably redo
    override fun visitSymbolIdentifierUnquoted(ctx: PartiQLParser.SymbolIdentifierUnquotedContext): PartiqlAst.PartiqlAstNode =
        PartiqlAst.BUILDER().id(ctx.IDENTIFIER().text, PartiqlAst.CaseSensitivity.CaseSensitive(), PartiqlAst.ScopeQualifier.LocalsFirst())

    override fun visitVarRefExprIdentQuoted(ctx: PartiQLParser.VarRefExprIdentQuotedContext): PartiqlAst.PartiqlAstNode =
        PartiqlAst.BUILDER().id(ctx.toRawString(), PartiqlAst.CaseSensitivity.CaseSensitive(), PartiqlAst.ScopeQualifier.Unqualified())

    override fun visitVarRefExprIdentAtQuoted(ctx: PartiQLParser.VarRefExprIdentAtQuotedContext): PartiqlAst.PartiqlAstNode =
        PartiqlAst.BUILDER().id(ctx.toRawString(), PartiqlAst.CaseSensitivity.CaseSensitive(), PartiqlAst.ScopeQualifier.LocalsFirst())

    override fun visitVarRefExprIdentAtUnquoted(ctx: PartiQLParser.VarRefExprIdentAtUnquotedContext): PartiqlAst.PartiqlAstNode =
        PartiqlAst.BUILDER().id(ctx.toRawString(), PartiqlAst.CaseSensitivity.CaseInsensitive(), PartiqlAst.ScopeQualifier.LocalsFirst())

    override fun visitVarRefExprIdentUnquoted(ctx: PartiQLParser.VarRefExprIdentUnquotedContext): PartiqlAst.PartiqlAstNode =
        PartiqlAst.BUILDER().id(ctx.toRawString(), PartiqlAst.CaseSensitivity.CaseInsensitive(), PartiqlAst.ScopeQualifier.Unqualified())

    override fun visitExprTermVarRefExpr(ctx: PartiQLParser.ExprTermVarRefExprContext): PartiqlAst.PartiqlAstNode = visit(ctx.varRefExpr())

    // TODO
    override fun visitFromClause(ctx: PartiQLParser.FromClauseContext): PartiqlAst.PartiqlAstNode {
        val tableRef = visit(ctx.tableReference(0)) as PartiqlAst.Expr // TODO: Get ALL
        return PartiqlAst.FromSource.Scan(tableRef, asAlias = null, byAlias = null, atAlias = null)
    }

    override fun visitExprTermBag(ctx: PartiQLParser.ExprTermBagContext): PartiqlAst.PartiqlAstNode {
        val exprList = ctx.exprQuery().map { exprQuery -> visit(exprQuery) as PartiqlAst.Expr }
        return PartiqlAst.Expr.Bag(exprList)
    }

    /**
     *
     * LITERALS
     *
     */

    override fun visitLiteralNull(ctx: PartiQLParser.LiteralNullContext): PartiqlAst.PartiqlAstNode = PartiqlAst.Expr.Lit(ion.newNull().toIonElement())
    override fun visitLiteralMissing(ctx: PartiQLParser.LiteralMissingContext): PartiqlAst.PartiqlAstNode = PartiqlAst.BUILDER().missing()
    override fun visitLiteralTrue(ctx: PartiQLParser.LiteralTrueContext): PartiqlAst.PartiqlAstNode = PartiqlAst.Expr.Lit(ion.newBool(true).toIonElement())
    override fun visitLiteralFalse(ctx: PartiQLParser.LiteralFalseContext): PartiqlAst.PartiqlAstNode = PartiqlAst.Expr.Lit(ion.newBool(false).toIonElement())
    override fun visitLiteralIon(ctx: PartiQLParser.LiteralIonContext): PartiqlAst.PartiqlAstNode = PartiqlAst.Expr.Lit(ion.singleValue(ctx.ION_CLOSURE().text.toIonString()).toIonElement())
    override fun visitLiteralString(ctx: PartiQLParser.LiteralStringContext): PartiqlAst.PartiqlAstNode = PartiqlAst.Expr.Lit(ion.newString(ctx.LITERAL_STRING().text.toPartiQLString()).toIonElement())
    override fun visitLiteralInteger(ctx: PartiQLParser.LiteralIntegerContext): PartiqlAst.PartiqlAstNode = PartiqlAst.Expr.Lit(ion.newInt(BigInteger(ctx.LITERAL_INTEGER().text, 10)).toIonElement())
    override fun visitLiteralDate(ctx: PartiQLParser.LiteralDateContext): PartiqlAst.PartiqlAstNode {
        val dateString = ctx.LITERAL_STRING().text.toPartiQLString()
        val (year, month, day) = dateString.split("-")
        return PartiqlAst.BUILDER().date(year.toLong(), month.toLong(), day.toLong())
    }
    override fun visitLiteralTime(ctx: PartiQLParser.LiteralTimeContext): PartiqlAst.PartiqlAstNode {
        val timeString = ctx.LITERAL_STRING().text.toPartiQLString()
        // TODO: Get precision if specified
        val precision = getPrecisionFromTimeString(timeString).toLong()
        val time = LocalTime.parse(timeString, DateTimeFormatter.ISO_TIME)
        return PartiqlAst.BUILDER().litTime(
            PartiqlAst.BUILDER().timeValue(
                time.hour.toLong(), time.minute.toLong(), time.second.toLong(), time.nano.toLong(),
                precision, false, null
            )
        )
    }
    // TODO: Catch exception for exponent too large
    override fun visitLiteralDecimal(ctx: PartiQLParser.LiteralDecimalContext): PartiqlAst.PartiqlAstNode = PartiqlAst.Expr.Lit(ion.newDecimal(bigDecimalOf(ctx.LITERAL_DECIMAL().text)).toIonElement())

    /**
     *
     * HELPER METHODS
     *
     */

    private fun PartiQLParser.VarRefExprIdentAtUnquotedContext.toRawString() = this.IDENTIFIER_AT_UNQUOTED().text.removePrefix("@")
    private fun PartiQLParser.VarRefExprIdentAtQuotedContext.toRawString() = this.IDENTIFIER_AT_QUOTED().text.removePrefix("@").trim('"')
    private fun PartiQLParser.VarRefExprIdentQuotedContext.toRawString() = this.IDENTIFIER_QUOTED().text.toPartiQLIdentifier()
    private fun PartiQLParser.VarRefExprIdentUnquotedContext.toRawString() = this.IDENTIFIER().text

    private fun String.toPartiQLString(): String = this.trim('\'').replace("''", "'")
    private fun String.toPartiQLIdentifier(): String = this.trim('"').replace("\"\"", "\"")
    private fun String.toIonString(): String = this.trim('`')

    private fun getStrategy(strategy: PartiQLParser.SetQuantifierStrategyContext?): PartiqlAst.SetQuantifier? {
        return when {
            strategy == null -> null
            strategy.text.toUpperCase() == "DISTINCT" -> PartiqlAst.SetQuantifier.Distinct()
            else -> PartiqlAst.SetQuantifier.All()
        }
    }

    private fun getSetQuantifierStrategy(ctx: PartiQLParser.SelectClauseContext): PartiqlAst.SetQuantifier? {
        return when (ctx) {
            is PartiQLParser.SelectAllContext -> getStrategy(ctx.setQuantifierStrategy())
            is PartiQLParser.SelectItemsContext -> getStrategy(ctx.setQuantifierStrategy())
            is PartiQLParser.SelectValueContext -> getStrategy(ctx.setQuantifierStrategy())
            is PartiQLParser.SelectPivotContext -> null
            else -> null
        }
    }
}
