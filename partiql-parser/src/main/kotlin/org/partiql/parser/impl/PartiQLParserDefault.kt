/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
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

package org.partiql.parser.impl

import com.amazon.ionelement.api.IntElement
import com.amazon.ionelement.api.IntElementSize
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.IonElementException
import com.amazon.ionelement.api.loadSingleElement
import org.antlr.v4.runtime.BailErrorStrategy
import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.TokenSource
import org.antlr.v4.runtime.TokenStream
import org.antlr.v4.runtime.atn.PredictionMode
import org.antlr.v4.runtime.misc.ParseCancellationException
import org.antlr.v4.runtime.tree.TerminalNode
import org.partiql.ast.AstNode
import org.partiql.ast.DatetimeField
import org.partiql.ast.Exclude
import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.GraphMatch
import org.partiql.ast.GroupBy
import org.partiql.ast.Identifier
import org.partiql.ast.Let
import org.partiql.ast.OnConflict
import org.partiql.ast.Path
import org.partiql.ast.Returning
import org.partiql.ast.Select
import org.partiql.ast.SetOp
import org.partiql.ast.SetQuantifier
import org.partiql.ast.Sort
import org.partiql.ast.Statement
import org.partiql.ast.TableDefinition
import org.partiql.ast.Type
import org.partiql.ast.exclude
import org.partiql.ast.excludeExcludeExpr
import org.partiql.ast.excludeStepExcludeCollectionIndex
import org.partiql.ast.excludeStepExcludeCollectionWildcard
import org.partiql.ast.excludeStepExcludeTupleAttr
import org.partiql.ast.excludeStepExcludeTupleWildcard
import org.partiql.ast.exprAgg
import org.partiql.ast.exprBagOp
import org.partiql.ast.exprBetween
import org.partiql.ast.exprBinary
import org.partiql.ast.exprCall
import org.partiql.ast.exprCanCast
import org.partiql.ast.exprCanLosslessCast
import org.partiql.ast.exprCase
import org.partiql.ast.exprCaseBranch
import org.partiql.ast.exprCast
import org.partiql.ast.exprCoalesce
import org.partiql.ast.exprCollection
import org.partiql.ast.exprDateAdd
import org.partiql.ast.exprDateDiff
import org.partiql.ast.exprExtract
import org.partiql.ast.exprInCollection
import org.partiql.ast.exprIon
import org.partiql.ast.exprIsType
import org.partiql.ast.exprLike
import org.partiql.ast.exprLit
import org.partiql.ast.exprMatch
import org.partiql.ast.exprNullIf
import org.partiql.ast.exprOverlay
import org.partiql.ast.exprParameter
import org.partiql.ast.exprPath
import org.partiql.ast.exprPathStepIndex
import org.partiql.ast.exprPathStepSymbol
import org.partiql.ast.exprPathStepUnpivot
import org.partiql.ast.exprPathStepWildcard
import org.partiql.ast.exprPosition
import org.partiql.ast.exprSFW
import org.partiql.ast.exprSessionAttribute
import org.partiql.ast.exprStruct
import org.partiql.ast.exprStructField
import org.partiql.ast.exprSubstring
import org.partiql.ast.exprTrim
import org.partiql.ast.exprUnary
import org.partiql.ast.exprVar
import org.partiql.ast.exprWindow
import org.partiql.ast.exprWindowOver
import org.partiql.ast.fromJoin
import org.partiql.ast.fromValue
import org.partiql.ast.graphMatch
import org.partiql.ast.graphMatchLabelConj
import org.partiql.ast.graphMatchLabelDisj
import org.partiql.ast.graphMatchLabelName
import org.partiql.ast.graphMatchLabelNegation
import org.partiql.ast.graphMatchLabelWildcard
import org.partiql.ast.graphMatchPattern
import org.partiql.ast.graphMatchPatternPartEdge
import org.partiql.ast.graphMatchPatternPartNode
import org.partiql.ast.graphMatchPatternPartPattern
import org.partiql.ast.graphMatchQuantifier
import org.partiql.ast.graphMatchSelectorAllShortest
import org.partiql.ast.graphMatchSelectorAny
import org.partiql.ast.graphMatchSelectorAnyK
import org.partiql.ast.graphMatchSelectorAnyShortest
import org.partiql.ast.graphMatchSelectorShortestK
import org.partiql.ast.graphMatchSelectorShortestKGroup
import org.partiql.ast.groupBy
import org.partiql.ast.groupByKey
import org.partiql.ast.identifierSymbol
import org.partiql.ast.let
import org.partiql.ast.letBinding
import org.partiql.ast.onConflict
import org.partiql.ast.onConflictActionDoNothing
import org.partiql.ast.onConflictActionDoReplace
import org.partiql.ast.onConflictActionDoUpdate
import org.partiql.ast.onConflictTargetConstraint
import org.partiql.ast.onConflictTargetSymbols
import org.partiql.ast.orderBy
import org.partiql.ast.path
import org.partiql.ast.pathStepIndex
import org.partiql.ast.pathStepSymbol
import org.partiql.ast.returning
import org.partiql.ast.returningColumn
import org.partiql.ast.returningColumnValueExpression
import org.partiql.ast.returningColumnValueWildcard
import org.partiql.ast.selectPivot
import org.partiql.ast.selectProject
import org.partiql.ast.selectProjectItemAll
import org.partiql.ast.selectProjectItemExpression
import org.partiql.ast.selectStar
import org.partiql.ast.selectValue
import org.partiql.ast.setOp
import org.partiql.ast.sort
import org.partiql.ast.statementDDLCreateIndex
import org.partiql.ast.statementDDLCreateTable
import org.partiql.ast.statementDDLDropIndex
import org.partiql.ast.statementDDLDropTable
import org.partiql.ast.statementDMLBatchLegacy
import org.partiql.ast.statementDMLBatchLegacyOpDelete
import org.partiql.ast.statementDMLBatchLegacyOpInsert
import org.partiql.ast.statementDMLBatchLegacyOpInsertLegacy
import org.partiql.ast.statementDMLBatchLegacyOpRemove
import org.partiql.ast.statementDMLBatchLegacyOpSet
import org.partiql.ast.statementDMLDelete
import org.partiql.ast.statementDMLDeleteTarget
import org.partiql.ast.statementDMLInsert
import org.partiql.ast.statementDMLInsertLegacy
import org.partiql.ast.statementDMLRemove
import org.partiql.ast.statementDMLReplace
import org.partiql.ast.statementDMLUpdate
import org.partiql.ast.statementDMLUpdateAssignment
import org.partiql.ast.statementDMLUpsert
import org.partiql.ast.statementExec
import org.partiql.ast.statementExplain
import org.partiql.ast.statementExplainTargetDomain
import org.partiql.ast.statementQuery
import org.partiql.ast.tableDefinition
import org.partiql.ast.tableDefinitionColumn
import org.partiql.ast.tableDefinitionColumnConstraint
import org.partiql.ast.tableDefinitionColumnConstraintBodyNotNull
import org.partiql.ast.tableDefinitionColumnConstraintBodyNullable
import org.partiql.ast.typeAny
import org.partiql.ast.typeBag
import org.partiql.ast.typeBlob
import org.partiql.ast.typeBool
import org.partiql.ast.typeChar
import org.partiql.ast.typeClob
import org.partiql.ast.typeCustom
import org.partiql.ast.typeDate
import org.partiql.ast.typeDecimal
import org.partiql.ast.typeFloat32
import org.partiql.ast.typeFloat64
import org.partiql.ast.typeInt
import org.partiql.ast.typeInt2
import org.partiql.ast.typeInt4
import org.partiql.ast.typeInt8
import org.partiql.ast.typeList
import org.partiql.ast.typeMissing
import org.partiql.ast.typeNullType
import org.partiql.ast.typeNumeric
import org.partiql.ast.typeReal
import org.partiql.ast.typeSexp
import org.partiql.ast.typeString
import org.partiql.ast.typeStruct
import org.partiql.ast.typeSymbol
import org.partiql.ast.typeTime
import org.partiql.ast.typeTimeWithTz
import org.partiql.ast.typeTimestamp
import org.partiql.ast.typeTuple
import org.partiql.ast.typeVarchar
import org.partiql.parser.PartiQLLexerException
import org.partiql.parser.PartiQLParser
import org.partiql.parser.PartiQLParserException
import org.partiql.parser.PartiQLSyntaxException
import org.partiql.parser.SourceLocation
import org.partiql.parser.SourceLocations
import org.partiql.parser.antlr.PartiQLBaseVisitor
import org.partiql.parser.impl.util.DateTimeUtils
import org.partiql.value.NumericValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue
import org.partiql.value.boolValue
import org.partiql.value.dateValue
import org.partiql.value.datetime.DateTimeException
import org.partiql.value.datetime.DateTimeValue
import org.partiql.value.decimalValue
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.intValue
import org.partiql.value.missingValue
import org.partiql.value.nullValue
import org.partiql.value.stringValue
import org.partiql.value.timeValue
import org.partiql.value.timestampValue
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import java.nio.channels.ClosedByInterruptException
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import org.partiql.parser.antlr.PartiQLParser as GeneratedParser
import org.partiql.parser.antlr.PartiQLTokens as GeneratedLexer

/**
 * ANTLR Based Implementation of a PartiQLParser
 *
 * SLL Prediction Mode
 * -------------------
 * The [PredictionMode.SLL] mode uses the [BailErrorStrategy]. The [GeneratedParser], upon seeing a syntax error,
 * will throw a [ParseCancellationException] due to the [GeneratedParser.getErrorHandler]
 * being a [BailErrorStrategy]. The purpose of this is to throw syntax errors as quickly as possible once encountered.
 * As noted by the [PredictionMode.SLL] documentation, to guarantee results, it is useful to follow up a failed parse
 * by parsing with [PredictionMode.LL]. See the JavaDocs for [PredictionMode.SLL] and [BailErrorStrategy] for more.
 *
 * LL Prediction Mode
 * ------------------
 * The [PredictionMode.LL] mode is capable of parsing all valid inputs for a grammar,
 * but is slower than [PredictionMode.SLL]. Upon seeing a syntax error, this parser throws a [PartiQLParserException].
 */
internal class PartiQLParserDefault : PartiQLParser {

    @Throws(PartiQLSyntaxException::class, InterruptedException::class)
    override fun parse(source: String): PartiQLParser.Result {
        try {
            return PartiQLParserDefault.parse(source)
        } catch (throwable: Throwable) {
            throw PartiQLSyntaxException.wrap(throwable)
        }
    }

    companion object {

        /**
         * To reduce latency costs, the [PartiQLParserDefault] attempts to use [PredictionMode.SLL] and falls back to
         * [PredictionMode.LL] if a [ParseCancellationException] is thrown by the [BailErrorStrategy].
         */
        private fun parse(source: String): PartiQLParser.Result = try {
            parse(source, PredictionMode.SLL)
        } catch (ex: ParseCancellationException) {
            parse(source, PredictionMode.LL)
        }

        /**
         * Parses an input string [source] using the given prediction mode.
         */
        private fun parse(source: String, mode: PredictionMode): PartiQLParser.Result {
            val tokens = createTokenStream(source)
            val parser = InterruptibleParser(tokens)
            parser.reset()
            parser.removeErrorListeners()
            parser.interpreter.predictionMode = mode
            when (mode) {
                PredictionMode.SLL -> parser.errorHandler = BailErrorStrategy()
                PredictionMode.LL -> parser.addErrorListener(ParseErrorListener())
                else -> throw IllegalArgumentException("Unsupported parser mode: $mode")
            }
            val tree = parser.root()
            return Visitor.translate(source, tokens, tree)
        }

        private fun createTokenStream(source: String): CountingTokenStream {
            val queryStream = source.byteInputStream(StandardCharsets.UTF_8)
            val inputStream = try {
                CharStreams.fromStream(queryStream)
            } catch (ex: ClosedByInterruptException) {
                throw InterruptedException()
            }
            val handler = TokenizeErrorListener()
            val lexer = GeneratedLexer(inputStream)
            lexer.removeErrorListeners()
            lexer.addErrorListener(handler)
            return CountingTokenStream(lexer)
        }
    }

    /**
     * Catches Lexical errors (unidentified tokens) and throws a [PartiQLParserException]
     */
    private class TokenizeErrorListener : BaseErrorListener() {
        @Throws(PartiQLParserException::class)
        override fun syntaxError(
            recognizer: Recognizer<*, *>?,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            msg: String,
            e: RecognitionException?,
        ) {
            if (offendingSymbol is Token) {
                val token = offendingSymbol.text
                val tokenType = GeneratedParser.VOCABULARY.getSymbolicName(offendingSymbol.type)
                throw PartiQLLexerException(
                    token = token,
                    tokenType = tokenType,
                    message = msg,
                    cause = e,
                    location = SourceLocation(
                        line = line,
                        offset = charPositionInLine + 1,
                        length = token.length,
                        lengthLegacy = token.length,
                    ),
                )
            } else {
                throw IllegalArgumentException("Offending symbol is not a Token.")
            }
        }
    }

    /**
     * Catches Parser errors (malformed syntax) and throws a [PartiQLParserException]
     */
    private class ParseErrorListener : BaseErrorListener() {

        private val rules = GeneratedParser.ruleNames.asList()

        @Throws(PartiQLParserException::class)
        override fun syntaxError(
            recognizer: Recognizer<*, *>?,
            offendingSymbol: Any,
            line: Int,
            charPositionInLine: Int,
            msg: String,
            e: RecognitionException?,
        ) {
            if (offendingSymbol is Token) {
                val rule = e?.ctx?.toString(rules) ?: "UNKNOWN"
                val token = offendingSymbol.text
                val tokenType = GeneratedParser.VOCABULARY.getSymbolicName(offendingSymbol.type)
                throw PartiQLParserException(
                    rule = rule,
                    token = token,
                    tokenType = tokenType,
                    message = msg,
                    cause = e,
                    location = SourceLocation(
                        line = line,
                        offset = charPositionInLine + 1,
                        length = msg.length,
                        lengthLegacy = offendingSymbol.text.length,
                    ),
                )
            } else {
                throw IllegalArgumentException("Offending symbol is not a Token.")
            }
        }
    }

    /**
     * A wrapped [GeneratedParser] to allow thread interruption during parse.
     */
    internal class InterruptibleParser(input: TokenStream) : GeneratedParser(input) {
        override fun enterRule(localctx: ParserRuleContext?, state: Int, ruleIndex: Int) {
            if (Thread.interrupted()) {
                throw InterruptedException()
            }
            super.enterRule(localctx, state, ruleIndex)
        }
    }

    /**
     * This token stream creates [parameterIndexes], which is a map, where the keys represent the
     * indexes of all [GeneratedLexer.QUESTION_MARK]'s and the values represent their relative index amongst all other
     * [GeneratedLexer.QUESTION_MARK]'s.
     */
    internal open class CountingTokenStream(tokenSource: TokenSource) : CommonTokenStream(tokenSource) {
        // TODO: Research use-case of parameters and implementation -- see https://github.com/partiql/partiql-docs/issues/23
        val parameterIndexes = mutableMapOf<Int, Int>()
        private var parametersFound = 0
        override fun LT(k: Int): Token? {
            val token = super.LT(k)
            token?.let {
                if (it.type == GeneratedLexer.QUESTION_MARK && parameterIndexes.containsKey(token.tokenIndex).not()) {
                    parameterIndexes[token.tokenIndex] = ++parametersFound
                }
            }
            return token
        }
    }

    /**
     * Translate an ANTLR ParseTree to a PartiQL
     */
    @OptIn(PartiQLValueExperimental::class)
    private class Visitor(
        private val locations: SourceLocations.Mutable,
        private val parameters: Map<Int, Int> = mapOf(),
    ) : PartiQLBaseVisitor<AstNode>() {

        companion object {

            private val rules = GeneratedParser.ruleNames.asList()

            /**
             * Expose an (internal) friendly entry point into the traversal; mostly for keeping mutable state contained.
             */
            fun translate(
                source: String,
                tokens: CountingTokenStream,
                tree: GeneratedParser.RootContext,
            ): PartiQLParser.Result {
                val locations = SourceLocations.Mutable()
                val visitor = Visitor(locations, tokens.parameterIndexes)
                val root = visitor.visitAs<AstNode>(tree) as Statement
                return PartiQLParser.Result(
                    source = source,
                    root = root,
                    locations = locations.toMap(),
                )
            }

            fun error(
                ctx: ParserRuleContext,
                message: String,
                cause: Throwable? = null,
            ) = PartiQLParserException(
                rule = ctx.toStringTree(rules),
                token = ctx.start.text,
                tokenType = GeneratedParser.VOCABULARY.getSymbolicName(ctx.start.type),
                message = message,
                cause = cause,
                location = SourceLocation(
                    line = ctx.start.line,
                    offset = ctx.start.charPositionInLine + 1,
                    length = ctx.stop.stopIndex - ctx.start.startIndex,
                    lengthLegacy = ctx.start.text.length,
                ),
            )

            fun error(
                token: Token,
                message: String,
                cause: Throwable? = null,
            ) = PartiQLLexerException(
                token = token.text,
                tokenType = GeneratedParser.VOCABULARY.getSymbolicName(token.type),
                message = message,
                cause = cause,
                location = SourceLocation(
                    line = token.line,
                    offset = token.charPositionInLine + 1,
                    length = token.stopIndex - token.startIndex,
                    lengthLegacy = token.text.length,
                ),
            )

            internal val DATE_PATTERN_REGEX = Regex("\\d\\d\\d\\d-\\d\\d-\\d\\d")

            internal val GENERIC_TIME_REGEX = Regex("\\d\\d:\\d\\d:\\d\\d(\\.\\d*)?([+|-]\\d\\d:\\d\\d)?")
        }

        /**
         * Each visit attaches source locations from the given parse tree node; constructs nodes via the factory.
         */
        private inline fun <T : AstNode> translate(ctx: ParserRuleContext, block: () -> T): T {
            val node = block()
            if (ctx.start != null) {
                locations[node.tag] = SourceLocation(
                    line = ctx.start.line,
                    offset = ctx.start.charPositionInLine + 1,
                    length = (ctx.stop?.stopIndex ?: ctx.start.stopIndex) - ctx.start.startIndex + 1,
                    lengthLegacy = ctx.start.text.length, // LEGACY LENGTH
                )
            }
            return node
        }

        /**
         *
         * TOP LEVEL
         *
         */

        override fun visitQueryDql(ctx: GeneratedParser.QueryDqlContext): AstNode = visitDql(ctx.dql())

        override fun visitQueryDml(ctx: GeneratedParser.QueryDmlContext): AstNode = visit(ctx.dml())

        override fun visitRoot(ctx: GeneratedParser.RootContext) = translate(ctx) {
            when (ctx.EXPLAIN()) {
                null -> visit(ctx.statement()) as Statement
                else -> {
                    var type: String? = null
                    var format: String? = null
                    ctx.explainOption().forEach { option ->
                        val parameter = try {
                            ExplainParameters.valueOf(option.param.text.uppercase())
                        } catch (ex: java.lang.IllegalArgumentException) {
                            throw error(option.param, "Unknown EXPLAIN parameter.", ex)
                        }
                        when (parameter) {
                            ExplainParameters.TYPE -> {
                                type = parameter.getCompliantString(type, option.value)
                            }
                            ExplainParameters.FORMAT -> {
                                format = parameter.getCompliantString(format, option.value)
                            }
                        }
                    }
                    statementExplain(
                        target = statementExplainTargetDomain(
                            statement = visit(ctx.statement()) as Statement,
                            type = type,
                            format = format,
                        ),
                    )
                }
            }
        }

        /**
         *
         * COMMON USAGES
         *
         */

        override fun visitAsIdent(ctx: GeneratedParser.AsIdentContext) = visitSymbolPrimitive(ctx.symbolPrimitive())

        override fun visitAtIdent(ctx: GeneratedParser.AtIdentContext) = visitSymbolPrimitive(ctx.symbolPrimitive())

        override fun visitByIdent(ctx: GeneratedParser.ByIdentContext) = visitSymbolPrimitive(ctx.symbolPrimitive())

        override fun visitSymbolPrimitive(ctx: GeneratedParser.SymbolPrimitiveContext) = translate(ctx) {
            when (ctx.ident.type) {
                GeneratedParser.IDENTIFIER_QUOTED -> identifierSymbol(
                    ctx.IDENTIFIER_QUOTED().getStringValue(),
                    Identifier.CaseSensitivity.SENSITIVE,
                )
                GeneratedParser.IDENTIFIER -> identifierSymbol(
                    ctx.IDENTIFIER().getStringValue(),
                    Identifier.CaseSensitivity.INSENSITIVE,
                )
                else -> throw error(ctx, "Invalid symbol reference.")
            }
        }

        /**
         *
         * DATA DEFINITION LANGUAGE (DDL)
         *
         */

        override fun visitQueryDdl(ctx: GeneratedParser.QueryDdlContext): AstNode = visitDdl(ctx.ddl())

        override fun visitDropTable(ctx: GeneratedParser.DropTableContext) = translate(ctx) {
            val table = visitSymbolPrimitive(ctx.tableName().symbolPrimitive())
            statementDDLDropTable(table)
        }

        override fun visitDropIndex(ctx: GeneratedParser.DropIndexContext) = translate(ctx) {
            val table = visitSymbolPrimitive(ctx.on)
            val index = visitSymbolPrimitive(ctx.target)
            statementDDLDropIndex(index, table)
        }

        override fun visitCreateTable(ctx: GeneratedParser.CreateTableContext) = translate(ctx) {
            val table = visitSymbolPrimitive(ctx.tableName().symbolPrimitive())
            val definition = ctx.tableDef()?.let { visitTableDef(it) }
            statementDDLCreateTable(table, definition)
        }

        override fun visitCreateIndex(ctx: GeneratedParser.CreateIndexContext) = translate(ctx) {
            // TODO add index name to ANTLR grammar
            val name: Identifier? = null
            val table = visitSymbolPrimitive(ctx.symbolPrimitive())
            val fields = ctx.pathSimple().map { path -> visitPathSimple(path) }
            statementDDLCreateIndex(name, table, fields)
        }

        override fun visitTableDef(ctx: GeneratedParser.TableDefContext) = translate(ctx) {
            // Column Definitions are the only thing we currently allow as table definition parts
            val columns = ctx.tableDefPart().filterIsInstance<GeneratedParser.ColumnDeclarationContext>().map {
                visitColumnDeclaration(it)
            }
            tableDefinition(columns)
        }

        override fun visitColumnDeclaration(ctx: GeneratedParser.ColumnDeclarationContext) = translate(ctx) {
            val name = symbolToString(ctx.columnName().symbolPrimitive())
            val type = visit(ctx.type()) as Type
            val constraints = ctx.columnConstraint().map {
                visitColumnConstraint(it)
            }
            tableDefinitionColumn(name, type, constraints)
        }

        override fun visitColumnConstraint(ctx: GeneratedParser.ColumnConstraintContext) = translate(ctx) {
            val identifier = ctx.columnConstraintName()?.let { symbolToString(it.symbolPrimitive()) }
            val body = visit(ctx.columnConstraintDef()) as TableDefinition.Column.Constraint.Body
            tableDefinitionColumnConstraint(identifier, body)
        }

        override fun visitColConstrNotNull(ctx: GeneratedParser.ColConstrNotNullContext) = translate(ctx) {
            tableDefinitionColumnConstraintBodyNotNull()
        }

        override fun visitColConstrNull(ctx: GeneratedParser.ColConstrNullContext) = translate(ctx) {
            tableDefinitionColumnConstraintBodyNullable()
        }

        /**
         *
         * EXECUTE
         *
         */

        override fun visitQueryExec(ctx: GeneratedParser.QueryExecContext) = visitExecCommand(ctx.execCommand())

        /**
         * TODO EXEC accepts an `expr` as the procedure name so we have to unpack the string.
         *  - https://github.com/partiql/partiql-lang-kotlin/issues/707
         */
        override fun visitExecCommand(ctx: GeneratedParser.ExecCommandContext) = translate(ctx) {
            val expr = visitExpr(ctx.name)
            if (expr !is Expr.Var || expr.identifier !is Identifier.Symbol) {
                throw error(ctx, "EXEC procedure must be a symbol identifier")
            }
            val procedure = (expr.identifier as Identifier.Symbol).symbol
            val args = visitOrEmpty<Expr>(ctx.args)
            statementExec(procedure, args)
        }

        /**
         *
         * DATA MANIPULATION LANGUAGE (DML)
         *
         */

        /**
         * The PartiQL grammars allows for multiple DML commands in one UPDATE statement.
         * This function unwraps DML commands to the more limited DML.BatchLegacy.Op commands.
         */
        override fun visitDmlBaseWrapper(ctx: GeneratedParser.DmlBaseWrapperContext) = translate(ctx) {
            val table = when {
                ctx.updateClause() != null -> ctx.updateClause().tableBaseReference()
                ctx.fromClause() != null -> ctx.fromClause().tableReference()
                else -> throw error(ctx, "Expected UPDATE <table> or FROM <table>")
            }
            val from = visitOrNull<From>(table)
            var returning: Returning? = null
            val ops = ctx.dmlBaseCommand().map {
                val op = visitDmlBaseCommand(it)
                when (op) {
                    is Statement.DML.Update -> statementDMLBatchLegacyOpSet(op.assignments)
                    is Statement.DML.Remove -> statementDMLBatchLegacyOpRemove(op.target)
                    is Statement.DML.Delete -> statementDMLBatchLegacyOpDelete()
                    is Statement.DML.Insert -> statementDMLBatchLegacyOpInsert(
                        op.target, op.values, op.asAlias, op.onConflict
                    )
                    is Statement.DML.InsertLegacy -> statementDMLBatchLegacyOpInsertLegacy(
                        op.target, op.value, op.index, op.conflictCondition
                    )
                    is Statement.DML.BatchLegacy -> {
                        // UNPACK InsertLegacy with returning
                        assert(op.ops.size == 1) { "wrapped batch op can only have one item" }
                        returning = op.returning
                        op.ops[0]
                    }
                    else -> throw error(ctx, "Invalid DML operator in BatchLegacy update")
                }
            }
            val where = ctx.whereClause()?.let { visitExpr(it.expr()) }
            // outer returning
            if (ctx.returningClause() != null) {
                returning = visitReturningClause(ctx.returningClause()!!)
            }
            statementDMLBatchLegacy(ops, from, where, returning)
        }

        override fun visitDmlDelete(ctx: GeneratedParser.DmlDeleteContext) = visitDeleteCommand(ctx.deleteCommand())

        override fun visitDmlInsertReturning(ctx: GeneratedParser.DmlInsertReturningContext): Statement.DML =
            super.visit(ctx.insertCommandReturning()) as Statement.DML

        override fun visitDmlBase(ctx: GeneratedParser.DmlBaseContext) =
            super.visitDmlBaseCommand(ctx.dmlBaseCommand()) as Statement.DML

        override fun visitDmlBaseCommand(ctx: GeneratedParser.DmlBaseCommandContext) =
            super.visitDmlBaseCommand(ctx) as Statement.DML

        override fun visitRemoveCommand(ctx: GeneratedParser.RemoveCommandContext) = translate(ctx) {
            val target = visitPathSimple(ctx.pathSimple())
            statementDMLRemove(target)
        }

        override fun visitDeleteCommand(ctx: GeneratedParser.DeleteCommandContext) = translate(ctx) {
            val from = visitAs<Statement.DML.Delete.Target>(ctx.fromClauseSimple())
            val where = ctx.whereClause()?.let { visitExpr(it.arg) }
            val returning = ctx.returningClause()?.let { visitReturningClause(it) }
            statementDMLDelete(from, where, returning)
        }

        /**
         * Legacy INSERT with RETURNING clause is not represented in the AST as this grammar ..
         * .. only exists for backwards compatibility. The RETURNING clause is ignored.
         *
         * TODO remove insertCommandReturning grammar rule
         *  - https://github.com/partiql/partiql-lang-kotlin/issues/698
         *  - https://github.com/partiql/partiql-lang-kotlin/issues/708
         */
        override fun visitInsertCommandReturning(ctx: GeneratedParser.InsertCommandReturningContext) = translate(ctx) {
            val target = visitPathSimple(ctx.pathSimple())
            val value = visitExpr(ctx.value)
            val index = visitOrNull<Expr>(ctx.pos)
            val conflictCondition = ctx.onConflictLegacy()?.let { visitOnConflictLegacy(it) }
            if (ctx.returningClause() != null) {
                val returning = visitReturningClause(ctx.returningClause()!!)
                val insert = statementDMLBatchLegacyOpInsertLegacy(target, value, index, conflictCondition)
                statementDMLBatchLegacy(listOf(insert), null, null, returning)
            } else {
                statementDMLInsertLegacy(target, value, index, conflictCondition)
            }
        }

        override fun visitInsertStatementLegacy(ctx: GeneratedParser.InsertStatementLegacyContext) = translate(ctx) {
            val target = visitPathSimple(ctx.pathSimple())
            val value = visitExpr(ctx.value)
            val index = visitOrNull<Expr>(ctx.pos)
            val conflictCondition = ctx.onConflictLegacy()?.let { visitOnConflictLegacy(it) }
            statementDMLInsertLegacy(target, value, index, conflictCondition)
        }

        override fun visitInsertStatement(ctx: GeneratedParser.InsertStatementContext) = translate(ctx) {
            val target = visitSymbolPrimitive(ctx.symbolPrimitive())
            val values = visitExpr(ctx.value)
            val asAlias = visitOrNull<Identifier.Symbol>(ctx.asIdent())
            val onConflict = ctx.onConflict()?.let { visitOnConflictClause(it) }
            statementDMLInsert(target, values, asAlias, onConflict)
        }

        override fun visitReplaceCommand(ctx: GeneratedParser.ReplaceCommandContext) = translate(ctx) {
            val target = visitSymbolPrimitive(ctx.symbolPrimitive())
            val values = visitExpr(ctx.value)
            val asAlias = visitOrNull<Identifier.Symbol>(ctx.asIdent())
            statementDMLReplace(target, values, asAlias)
        }

        override fun visitUpsertCommand(ctx: GeneratedParser.UpsertCommandContext) = translate(ctx) {
            val target = visitSymbolPrimitive(ctx.symbolPrimitive())
            val values = visitExpr(ctx.value)
            val asAlias = visitOrNull<Identifier.Symbol>(ctx.asIdent())
            statementDMLUpsert(target, values, asAlias)
        }

        override fun visitReturningClause(ctx: GeneratedParser.ReturningClauseContext) = translate(ctx) {
            val columns = visitOrEmpty<Returning.Column>(ctx.returningColumn())
            returning(columns)
        }

        override fun visitReturningColumn(ctx: GeneratedParser.ReturningColumnContext) = translate(ctx) {
            val status = when (ctx.status.type) {
                GeneratedParser.MODIFIED -> Returning.Column.Status.MODIFIED
                GeneratedParser.ALL -> Returning.Column.Status.ALL
                else -> throw error(ctx.status, "Expected MODIFIED or ALL")
            }
            val age = when (ctx.age.type) {
                GeneratedParser.OLD -> Returning.Column.Age.OLD
                GeneratedParser.NEW -> Returning.Column.Age.NEW
                else -> throw error(ctx.status, "Expected OLD or NEW")
            }
            val value = when (ctx.ASTERISK()) {
                null -> returningColumnValueExpression(visitExpr(ctx.expr()))
                else -> returningColumnValueWildcard()
            }
            returningColumn(status, age, value)
        }

        private fun visitOnConflictClause(ctx: GeneratedParser.OnConflictContext) = ctx.accept(this) as OnConflict

        override fun visitOnConflict(ctx: GeneratedParser.OnConflictContext) = translate(ctx) {
            val target = ctx.conflictTarget()?.let { visitConflictTarget(it) }
            val action = visitConflictAction(ctx.conflictAction())
            onConflict(target, action)
        }

        /**
         * TODO Remove this when we remove INSERT LEGACY as no other conflict actions are allowed in PartiQL.g4.
         */
        override fun visitOnConflictLegacy(ctx: GeneratedParser.OnConflictLegacyContext) = translate(ctx) {
            visitExpr(ctx.expr())
        }

        override fun visitConflictTarget(ctx: GeneratedParser.ConflictTargetContext) = translate(ctx) {
            if (ctx.constraintName() != null) {
                onConflictTargetConstraint(visitSymbolPrimitive(ctx.constraintName().symbolPrimitive()))
            } else {
                val symbols = ctx.symbolPrimitive().map { visitSymbolPrimitive(it) }
                onConflictTargetSymbols(symbols)
            }
        }

        override fun visitConflictAction(ctx: GeneratedParser.ConflictActionContext) = when {
            ctx.NOTHING() != null -> translate(ctx) { onConflictActionDoNothing() }
            ctx.REPLACE() != null -> visitDoReplace(ctx.doReplace())
            ctx.UPDATE() != null -> visitDoUpdate(ctx.doUpdate())
            else -> throw error(ctx, "ON CONFLICT only supports `DO REPLACE` and `DO NOTHING` actions at the moment.")
        }

        override fun visitDoReplace(ctx: GeneratedParser.DoReplaceContext) = translate(ctx) {
            val condition = ctx.condition?.let { visitExpr(it) }
            onConflictActionDoReplace(condition)
        }

        override fun visitDoUpdate(ctx: GeneratedParser.DoUpdateContext) = translate(ctx) {
            val condition = ctx.condition?.let { visitExpr(it) }
            onConflictActionDoUpdate(condition)
        }

        override fun visitPathSimple(ctx: GeneratedParser.PathSimpleContext) = translate(ctx) {
            val root = visitSymbolPrimitive(ctx.symbolPrimitive())
            val steps = visitOrEmpty<Path.Step>(ctx.pathSimpleSteps())
            path(root, steps)
        }

        override fun visitPathSimpleLiteral(ctx: GeneratedParser.PathSimpleLiteralContext) = translate(ctx) {
            val v = visit(ctx.literal())
            if (v !is Expr.Lit) {
                throw error(ctx, "Expected a path element literal")
            }
            when (val i = v.value) {
                is NumericValue<*> -> pathStepIndex(i.int!!)
                is StringValue -> pathStepSymbol(
                    identifierSymbol(
                        i.value!!, Identifier.CaseSensitivity.SENSITIVE
                    )
                )
                else -> throw error(ctx, "Expected an integer or string literal, found literal ${i.type}")
            }
        }

        override fun visitPathSimpleSymbol(ctx: GeneratedParser.PathSimpleSymbolContext) = translate(ctx) {
            val identifier = visitSymbolPrimitive(ctx.symbolPrimitive())
            pathStepSymbol(identifier)
        }

        override fun visitPathSimpleDotSymbol(ctx: GeneratedParser.PathSimpleDotSymbolContext) = translate(ctx) {
            val identifier = visitSymbolPrimitive(ctx.symbolPrimitive())
            pathStepSymbol(identifier)
        }

        /**
         * TODO current PartiQL.g4 grammar models a SET with no UPDATE target as valid DML command.
         */
        override fun visitSetCommand(ctx: GeneratedParser.SetCommandContext) = translate(ctx) {
            // We put a blank target, because we'll have to unpack this.
            val target = path(
                root = identifierSymbol("_blank", Identifier.CaseSensitivity.INSENSITIVE),
                steps = emptyList(),
            )
            val assignments = visitOrEmpty<Statement.DML.Update.Assignment>(ctx.setAssignment())
            statementDMLUpdate(target, assignments)
        }

        override fun visitSetAssignment(ctx: GeneratedParser.SetAssignmentContext) = translate(ctx) {
            val target = visitPathSimple(ctx.pathSimple())
            val value = visitExpr(ctx.expr())
            statementDMLUpdateAssignment(target, value)
        }

        /**
         *
         * DATA QUERY LANGUAGE (DQL)
         *
         */

        override fun visitDql(ctx: GeneratedParser.DqlContext) = translate(ctx) {
            val expr = visitAs<Expr>(ctx.expr())
            statementQuery(expr)
        }

        override fun visitQueryBase(ctx: GeneratedParser.QueryBaseContext): AstNode = visit(ctx.exprSelect())

        override fun visitSfwQuery(ctx: GeneratedParser.SfwQueryContext) = translate(ctx) {
            val select = visit(ctx.select) as Select
            val from = visitFromClause(ctx.from)
            val exclude = visitOrNull<Exclude>(ctx.exclude)
            val let = visitOrNull<Let>(ctx.let)
            val where = visitOrNull<Expr>(ctx.where)
            val groupBy = ctx.group?.let { visitGroupClause(it) }
            val having = visitOrNull<Expr>(ctx.having?.arg)
            // TODO Add SQL UNION, INTERSECT, EXCEPT to PartiQL.g4
            val setOp: Expr.SFW.SetOp? = null
            val orderBy = ctx.order?.let { visitOrderByClause(it) }
            val limit = visitOrNull<Expr>(ctx.limit?.arg)
            val offset = visitOrNull<Expr>(ctx.offset?.arg)
            exprSFW(select, exclude, from, let, where, groupBy, having, setOp, orderBy, limit, offset)
        }

        /**
         *
         * SELECT & PROJECTIONS
         *
         */

        override fun visitSelectAll(ctx: GeneratedParser.SelectAllContext) = translate(ctx) {
            val quantifier = convertSetQuantifier(ctx.setQuantifierStrategy())
            selectStar(quantifier)
        }

        override fun visitSelectItems(ctx: GeneratedParser.SelectItemsContext) = translate(ctx) {
            val items = visitOrEmpty<Select.Project.Item>(ctx.projectionItems().projectionItem())
            val setq = convertSetQuantifier(ctx.setQuantifierStrategy())
            selectProject(items, setq)
        }

        override fun visitSelectPivot(ctx: GeneratedParser.SelectPivotContext) = translate(ctx) {
            val key = visitExpr(ctx.pivot)
            val value = visitExpr(ctx.at)
            selectPivot(key, value)
        }

        override fun visitSelectValue(ctx: GeneratedParser.SelectValueContext) = translate(ctx) {
            val constructor = visitExpr(ctx.expr())
            val setq = convertSetQuantifier(ctx.setQuantifierStrategy())
            selectValue(constructor, setq)
        }

        override fun visitProjectionItem(ctx: GeneratedParser.ProjectionItemContext) = translate(ctx) {
            val expr = visitExpr(ctx.expr())
            val alias = ctx.symbolPrimitive()?.let { visitSymbolPrimitive(it) }
            if (expr is Expr.Path) {
                convertPathToProjectionItem(ctx, expr, alias)
            } else {
                selectProjectItemExpression(expr, alias)
            }
        }

        /**
         *
         * SIMPLE CLAUSES
         *
         */

        override fun visitLimitClause(ctx: GeneratedParser.LimitClauseContext): Expr = visitAs<Expr>(ctx.arg)

        override fun visitExpr(ctx: GeneratedParser.ExprContext): Expr {
            if (Thread.interrupted()) {
                throw InterruptedException()
            }
            return visitAs<Expr>(ctx.exprBagOp())
        }

        override fun visitOffsetByClause(ctx: GeneratedParser.OffsetByClauseContext) = visitAs<Expr>(ctx.arg)

        override fun visitWhereClause(ctx: GeneratedParser.WhereClauseContext) = visitExpr(ctx.arg)

        override fun visitWhereClauseSelect(ctx: GeneratedParser.WhereClauseSelectContext) = visitAs<Expr>(ctx.arg)

        override fun visitHavingClause(ctx: GeneratedParser.HavingClauseContext) = visitAs<Expr>(ctx.arg)

        /**
         *
         * LET CLAUSE
         *
         */

        override fun visitLetClause(ctx: GeneratedParser.LetClauseContext) = translate(ctx) {
            val bindings = visitOrEmpty<Let.Binding>(ctx.letBinding())
            let(bindings)
        }

        override fun visitLetBinding(ctx: GeneratedParser.LetBindingContext) = translate(ctx) {
            val expr = visitAs<Expr>(ctx.expr())
            val alias = visitSymbolPrimitive(ctx.symbolPrimitive())
            letBinding(expr, alias)
        }

        /**
         *
         * ORDER BY CLAUSE
         *
         */

        override fun visitOrderByClause(ctx: GeneratedParser.OrderByClauseContext) = translate(ctx) {
            val sorts = visitOrEmpty<Sort>(ctx.orderSortSpec())
            orderBy(sorts)
        }

        override fun visitOrderSortSpec(ctx: GeneratedParser.OrderSortSpecContext) = translate(ctx) {
            val expr = visitAs<Expr>(ctx.expr())
            val dir = when {
                ctx.dir == null -> null
                ctx.dir.type == GeneratedParser.ASC -> Sort.Dir.ASC
                ctx.dir.type == GeneratedParser.DESC -> Sort.Dir.DESC
                else -> throw error(ctx.dir, "Invalid ORDER BY direction; expected ASC or DESC")
            }
            val nulls = when {
                ctx.nulls == null -> null
                ctx.nulls.type == GeneratedParser.FIRST -> Sort.Nulls.FIRST
                ctx.nulls.type == GeneratedParser.LAST -> Sort.Nulls.LAST
                else -> throw error(ctx.nulls, "Invalid ORDER BY null ordering; expected FIRST or LAST")
            }
            sort(expr, dir, nulls)
        }

        /**
         *
         * GROUP BY CLAUSE
         *
         */

        override fun visitGroupClause(ctx: GeneratedParser.GroupClauseContext) = translate(ctx) {
            val strategy = if (ctx.PARTIAL() != null) GroupBy.Strategy.PARTIAL else GroupBy.Strategy.FULL
            val keys = visitOrEmpty<GroupBy.Key>(ctx.groupKey())
            val alias = ctx.groupAlias()?.symbolPrimitive()?.let { visitSymbolPrimitive(it) }
            groupBy(strategy, keys, alias)
        }

        override fun visitGroupKey(ctx: GeneratedParser.GroupKeyContext) = translate(ctx) {
            val expr = visitAs<Expr>(ctx.key)
            val alias = ctx.symbolPrimitive()?.let { visitSymbolPrimitive(it) }
            groupByKey(expr, alias)
        }

        /**
         * EXCLUDE CLAUSE
         */
        override fun visitExcludeClause(ctx: GeneratedParser.ExcludeClauseContext) = translate(ctx) {
            val excludeExprs = ctx.excludeExpr().map { expr ->
                visitExcludeExpr(expr)
            }
            exclude(excludeExprs)
        }

        override fun visitExcludeExpr(ctx: GeneratedParser.ExcludeExprContext) = translate(ctx) {
            val root = visitSymbolPrimitive(ctx.symbolPrimitive())
            val steps = visitOrEmpty<Exclude.Step>(ctx.excludeExprSteps())
            excludeExcludeExpr(root, steps)
        }

        override fun visitExcludeExprTupleAttr(ctx: GeneratedParser.ExcludeExprTupleAttrContext) = translate(ctx) {
            val identifier = visitSymbolPrimitive(ctx.symbolPrimitive())
            excludeStepExcludeTupleAttr(identifier)
        }

        override fun visitExcludeExprCollectionIndex(ctx: GeneratedParser.ExcludeExprCollectionIndexContext) =
            translate(ctx) {
                val index = ctx.index.text.toInt()
                excludeStepExcludeCollectionIndex(index)
            }

        override fun visitExcludeExprCollectionAttr(ctx: GeneratedParser.ExcludeExprCollectionAttrContext) =
            translate(ctx) {
                val attr = ctx.attr.getStringValue()
                val identifier = identifierSymbol(
                    attr,
                    Identifier.CaseSensitivity.SENSITIVE,
                )
                excludeStepExcludeTupleAttr(identifier)
            }

        override fun visitExcludeExprCollectionWildcard(ctx: org.partiql.parser.antlr.PartiQLParser.ExcludeExprCollectionWildcardContext) =
            translate(ctx) {
                excludeStepExcludeCollectionWildcard()
            }

        override fun visitExcludeExprTupleWildcard(ctx: org.partiql.parser.antlr.PartiQLParser.ExcludeExprTupleWildcardContext) =
            translate(ctx) {
                excludeStepExcludeTupleWildcard()
            }

        /**
         *
         * BAG OPERATIONS
         *
         */

        override fun visitIntersect(ctx: GeneratedParser.IntersectContext) = translate(ctx) {
            val setq = when {
                ctx.ALL() != null -> SetQuantifier.ALL
                ctx.DISTINCT() != null -> SetQuantifier.DISTINCT
                else -> null
            }
            val op = setOp(SetOp.Type.INTERSECT, setq)
            val lhs = visitAs<Expr>(ctx.lhs)
            val rhs = visitAs<Expr>(ctx.rhs)
            val outer = ctx.OUTER() != null
            exprBagOp(op, lhs, rhs, outer)
        }

        override fun visitExcept(ctx: GeneratedParser.ExceptContext) = translate(ctx) {
            val setq = when {
                ctx.ALL() != null -> SetQuantifier.ALL
                ctx.DISTINCT() != null -> SetQuantifier.DISTINCT
                else -> null
            }
            val op = setOp(SetOp.Type.EXCEPT, setq)
            val lhs = visitAs<Expr>(ctx.lhs)
            val rhs = visitAs<Expr>(ctx.rhs)
            val outer = ctx.OUTER() != null
            exprBagOp(op, lhs, rhs, outer)
        }

        override fun visitUnion(ctx: GeneratedParser.UnionContext) = translate(ctx) {
            val setq = when {
                ctx.ALL() != null -> SetQuantifier.ALL
                ctx.DISTINCT() != null -> SetQuantifier.DISTINCT
                else -> null
            }
            val op = setOp(SetOp.Type.UNION, setq)
            val lhs = visitAs<Expr>(ctx.lhs)
            val rhs = visitAs<Expr>(ctx.rhs)
            val outer = ctx.OUTER() != null
            exprBagOp(op, lhs, rhs, outer)
        }

        /**
         *
         * GRAPH PATTERN MANIPULATION LANGUAGE (GPML)
         *
         */

        override fun visitGpmlPattern(ctx: GeneratedParser.GpmlPatternContext) = translate(ctx) {
            val pattern = visitMatchPattern(ctx.matchPattern())
            val selector = visitOrNull<GraphMatch.Selector>(ctx.matchSelector())
            graphMatch(listOf(pattern), selector)
        }

        override fun visitGpmlPatternList(ctx: GeneratedParser.GpmlPatternListContext) = translate(ctx) {
            val patterns = ctx.matchPattern().map { pattern -> visitMatchPattern(pattern) }
            val selector = visitOrNull<GraphMatch.Selector>(ctx.matchSelector())
            graphMatch(patterns, selector)
        }

        override fun visitMatchPattern(ctx: GeneratedParser.MatchPatternContext) = translate(ctx) {
            val parts = visitOrEmpty<GraphMatch.Pattern.Part>(ctx.graphPart())
            val restrictor = ctx.restrictor?.let {
                when (ctx.restrictor.text.lowercase()) {
                    "trail" -> GraphMatch.Restrictor.TRAIL
                    "acyclic" -> GraphMatch.Restrictor.ACYCLIC
                    "simple" -> GraphMatch.Restrictor.SIMPLE
                    else -> throw error(ctx.restrictor, "Unrecognized pattern restrictor")
                }
            }
            val variable = visitOrNull<Identifier.Symbol>(ctx.variable)?.symbol
            graphMatchPattern(restrictor, null, variable, null, parts)
        }

        override fun visitPatternPathVariable(ctx: GeneratedParser.PatternPathVariableContext) =
            visitSymbolPrimitive(ctx.symbolPrimitive())

        override fun visitSelectorBasic(ctx: GeneratedParser.SelectorBasicContext) = translate(ctx) {
            when (ctx.mod.type) {
                GeneratedParser.ANY -> graphMatchSelectorAnyShortest()
                GeneratedParser.ALL -> graphMatchSelectorAllShortest()
                else -> throw error(ctx, "Unsupported match selector.")
            }
        }

        override fun visitSelectorAny(ctx: GeneratedParser.SelectorAnyContext) = translate(ctx) {
            when (ctx.k) {
                null -> graphMatchSelectorAny()
                else -> graphMatchSelectorAnyK(ctx.k.text.toLong())
            }
        }

        override fun visitSelectorShortest(ctx: GeneratedParser.SelectorShortestContext) = translate(ctx) {
            val k = ctx.k.text.toLong()
            when (ctx.GROUP()) {
                null -> graphMatchSelectorShortestK(k)
                else -> graphMatchSelectorShortestKGroup(k)
            }
        }

        override fun visitLabelSpecOr(ctx: GeneratedParser.LabelSpecOrContext) = translate(ctx) {
            val lhs = visit(ctx.labelSpec()) as GraphMatch.Label
            val rhs = visit(ctx.labelTerm()) as GraphMatch.Label
            graphMatchLabelDisj(lhs, rhs)
        }

        override fun visitLabelTermAnd(ctx: GeneratedParser.LabelTermAndContext) = translate(ctx) {
            val lhs = visit(ctx.labelTerm()) as GraphMatch.Label
            val rhs = visit(ctx.labelFactor()) as GraphMatch.Label
            graphMatchLabelConj(lhs, rhs)
        }

        override fun visitLabelFactorNot(ctx: GeneratedParser.LabelFactorNotContext) = translate(ctx) {
            val arg = visit(ctx.labelPrimary()) as GraphMatch.Label
            graphMatchLabelNegation(arg)
        }

        override fun visitLabelPrimaryName(ctx: GeneratedParser.LabelPrimaryNameContext) = translate(ctx) {
            val x = visitSymbolPrimitive(ctx.symbolPrimitive())
            graphMatchLabelName(x.symbol)
        }

        override fun visitLabelPrimaryWild(ctx: GeneratedParser.LabelPrimaryWildContext) = translate(ctx) {
            graphMatchLabelWildcard()
        }

        override fun visitLabelPrimaryParen(ctx: GeneratedParser.LabelPrimaryParenContext) =
            visit(ctx.labelSpec()) as GraphMatch.Label

        override fun visitPattern(ctx: GeneratedParser.PatternContext) = translate(ctx) {
            val restrictor = visitRestrictor(ctx.restrictor)
            val variable = visitOrNull<Identifier.Symbol>(ctx.variable)?.symbol
            val prefilter = ctx.where?.let { visitExpr(it.expr()) }
            val quantifier = ctx.quantifier?.let { visitPatternQuantifier(it) }
            val parts = visitOrEmpty<GraphMatch.Pattern.Part>(ctx.graphPart())
            graphMatchPattern(restrictor, prefilter, variable, quantifier, parts)
        }

        override fun visitEdgeAbbreviated(ctx: GeneratedParser.EdgeAbbreviatedContext) = translate(ctx) {
            val direction = visitEdge(ctx.edgeAbbrev())
            val quantifier = visitOrNull<GraphMatch.Quantifier>(ctx.quantifier)
            graphMatchPatternPartEdge(direction, quantifier, null, null, null)
        }

        override fun visitEdgeWithSpec(ctx: GeneratedParser.EdgeWithSpecContext) = translate(ctx) {
            val quantifier = visitOrNull<GraphMatch.Quantifier>(ctx.quantifier)
            val edge = visitOrNull<GraphMatch.Pattern.Part.Edge>(ctx.edgeWSpec())
            edge!!.copy(quantifier = quantifier)
        }

        override fun visitEdgeSpec(ctx: GeneratedParser.EdgeSpecContext) = translate(ctx) {
            val placeholderDirection = GraphMatch.Direction.RIGHT
            val variable = visitOrNull<Identifier.Symbol>(ctx.symbolPrimitive())?.symbol
            val prefilter = ctx.whereClause()?.let { visitExpr(it.expr()) }
            val label = visitOrNull<GraphMatch.Label>(ctx.labelSpec())
            graphMatchPatternPartEdge(placeholderDirection, null, prefilter, variable, label)
        }

        override fun visitEdgeSpecLeft(ctx: GeneratedParser.EdgeSpecLeftContext): AstNode {
            val edge = visitEdgeSpec(ctx.edgeSpec())
            return edge.copy(direction = GraphMatch.Direction.LEFT)
        }

        override fun visitEdgeSpecRight(ctx: GeneratedParser.EdgeSpecRightContext): AstNode {
            val edge = visitEdgeSpec(ctx.edgeSpec())
            return edge.copy(direction = GraphMatch.Direction.RIGHT)
        }

        override fun visitEdgeSpecBidirectional(ctx: GeneratedParser.EdgeSpecBidirectionalContext): AstNode {
            val edge = visitEdgeSpec(ctx.edgeSpec())
            return edge.copy(direction = GraphMatch.Direction.LEFT_OR_RIGHT)
        }

        override fun visitEdgeSpecUndirectedBidirectional(ctx: GeneratedParser.EdgeSpecUndirectedBidirectionalContext): AstNode {
            val edge = visitEdgeSpec(ctx.edgeSpec())
            return edge.copy(direction = GraphMatch.Direction.LEFT_UNDIRECTED_OR_RIGHT)
        }

        override fun visitEdgeSpecUndirected(ctx: GeneratedParser.EdgeSpecUndirectedContext): AstNode {
            val edge = visitEdgeSpec(ctx.edgeSpec())
            return edge.copy(direction = GraphMatch.Direction.UNDIRECTED)
        }

        override fun visitEdgeSpecUndirectedLeft(ctx: GeneratedParser.EdgeSpecUndirectedLeftContext): AstNode {
            val edge = visitEdgeSpec(ctx.edgeSpec())
            return edge.copy(direction = GraphMatch.Direction.LEFT_OR_UNDIRECTED)
        }

        override fun visitEdgeSpecUndirectedRight(ctx: GeneratedParser.EdgeSpecUndirectedRightContext): AstNode {
            val edge = visitEdgeSpec(ctx.edgeSpec())
            return edge.copy(direction = GraphMatch.Direction.UNDIRECTED_OR_RIGHT)
        }

        private fun visitEdge(ctx: GeneratedParser.EdgeAbbrevContext): GraphMatch.Direction = when {
            ctx.TILDE() != null && ctx.ANGLE_RIGHT() != null -> GraphMatch.Direction.UNDIRECTED_OR_RIGHT
            ctx.TILDE() != null && ctx.ANGLE_LEFT() != null -> GraphMatch.Direction.LEFT_OR_UNDIRECTED
            ctx.TILDE() != null -> GraphMatch.Direction.UNDIRECTED
            ctx.MINUS() != null && ctx.ANGLE_LEFT() != null && ctx.ANGLE_RIGHT() != null -> GraphMatch.Direction.LEFT_OR_RIGHT
            ctx.MINUS() != null && ctx.ANGLE_LEFT() != null -> GraphMatch.Direction.LEFT
            ctx.MINUS() != null && ctx.ANGLE_RIGHT() != null -> GraphMatch.Direction.RIGHT
            ctx.MINUS() != null -> GraphMatch.Direction.LEFT_UNDIRECTED_OR_RIGHT
            else -> throw error(ctx, "Unsupported edge type")
        }

        override fun visitGraphPart(ctx: GeneratedParser.GraphPartContext): GraphMatch.Pattern.Part {
            val part = super.visitGraphPart(ctx)
            if (part is GraphMatch.Pattern) {
                return translate(ctx) { graphMatchPatternPartPattern(part) }
            }
            return part as GraphMatch.Pattern.Part
        }

        override fun visitPatternQuantifier(ctx: GeneratedParser.PatternQuantifierContext) = translate(ctx) {
            when {
                ctx.quant == null -> graphMatchQuantifier(ctx.lower.text.toLong(), ctx.upper?.text?.toLong())
                ctx.quant.type == GeneratedParser.PLUS -> graphMatchQuantifier(1L, null)
                ctx.quant.type == GeneratedParser.ASTERISK -> graphMatchQuantifier(0L, null)
                else -> throw error(ctx, "Unsupported quantifier")
            }
        }

        override fun visitNode(ctx: GeneratedParser.NodeContext) = translate(ctx) {
            val variable = visitOrNull<Identifier.Symbol>(ctx.symbolPrimitive())?.symbol
            val prefilter = ctx.whereClause()?.let { visitExpr(it.expr()) }
            val label = visitOrNull<GraphMatch.Label>(ctx.labelSpec())
            graphMatchPatternPartNode(prefilter, variable, label)
        }

        private fun visitRestrictor(ctx: GeneratedParser.PatternRestrictorContext?): GraphMatch.Restrictor? {
            if (ctx == null) return null
            return when (ctx.restrictor.text.lowercase()) {
                "trail" -> GraphMatch.Restrictor.TRAIL
                "acyclic" -> GraphMatch.Restrictor.ACYCLIC
                "simple" -> GraphMatch.Restrictor.SIMPLE
                else -> throw error(ctx, "Unrecognized pattern restrictor")
            }
        }

        /**
         *
         * TABLE REFERENCES & JOINS & FROM CLAUSE
         *
         */

        override fun visitFromClause(ctx: GeneratedParser.FromClauseContext) = visitAs<From>(ctx.tableReference())

        override fun visitTableBaseRefClauses(ctx: GeneratedParser.TableBaseRefClausesContext) = translate(ctx) {
            val expr = visitAs<Expr>(ctx.source)
            val asAlias = ctx.asIdent()?.let { visitSymbolPrimitive(it.symbolPrimitive()) }
            val atAlias = ctx.atIdent()?.let { visitSymbolPrimitive(it.symbolPrimitive()) }
            val byAlias = ctx.byIdent()?.let { visitSymbolPrimitive(it.symbolPrimitive()) }
            fromValue(expr, From.Value.Type.SCAN, asAlias, atAlias, byAlias)
        }

        override fun visitTableBaseRefMatch(ctx: GeneratedParser.TableBaseRefMatchContext) = translate(ctx) {
            val expr = visitAs<Expr>(ctx.source)
            val asAlias = ctx.asIdent()?.let { visitSymbolPrimitive(it.symbolPrimitive()) }
            val atAlias = ctx.atIdent()?.let { visitSymbolPrimitive(it.symbolPrimitive()) }
            val byAlias = ctx.byIdent()?.let { visitSymbolPrimitive(it.symbolPrimitive()) }
            fromValue(expr, From.Value.Type.SCAN, asAlias, atAlias, byAlias)
        }

        /**
         * TODO Remove as/at/by aliases from DELETE command grammar in PartiQL.g4
         */
        override fun visitFromClauseSimpleExplicit(ctx: GeneratedParser.FromClauseSimpleExplicitContext) =
            translate(ctx) {
                val path = visitPathSimple(ctx.pathSimple())
                val asAlias = ctx.asIdent()?.let { visitAsIdent(it) }
                val atAlias = ctx.atIdent()?.let { visitAtIdent(it) }
                val byAlias = ctx.byIdent()?.let { visitByIdent(it) }
                statementDMLDeleteTarget(path, asAlias, atAlias, byAlias)
            }

        /**
         * TODO Remove fromClauseSimple rule from DELETE command grammar in PartiQL.g4
         */
        override fun visitFromClauseSimpleImplicit(ctx: GeneratedParser.FromClauseSimpleImplicitContext) =
            translate(ctx) {
                val path = visitPathSimple(ctx.pathSimple())
                val asAlias = visitSymbolPrimitive(ctx.symbolPrimitive())
                statementDMLDeleteTarget(path, asAlias, null, null)
            }

        override fun visitTableUnpivot(ctx: GeneratedParser.TableUnpivotContext) = translate(ctx) {
            val expr = visitAs<Expr>(ctx.expr())
            val asAlias = ctx.asIdent()?.let { visitSymbolPrimitive(it.symbolPrimitive()) }
            val atAlias = ctx.atIdent()?.let { visitSymbolPrimitive(it.symbolPrimitive()) }
            val byAlias = ctx.byIdent()?.let { visitSymbolPrimitive(it.symbolPrimitive()) }
            fromValue(expr, From.Value.Type.UNPIVOT, asAlias, atAlias, byAlias)
        }

        override fun visitTableCrossJoin(ctx: GeneratedParser.TableCrossJoinContext) = translate(ctx) {
            val lhs = visitAs<From>(ctx.lhs)
            val rhs = visitAs<From>(ctx.rhs)
            val type = convertJoinType(ctx.joinType())
            fromJoin(lhs, rhs, type, null)
        }

        private fun convertJoinType(ctx: GeneratedParser.JoinTypeContext?): From.Join.Type? {
            if (ctx == null) return null
            return when (ctx.mod.type) {
                GeneratedParser.INNER -> From.Join.Type.INNER
                GeneratedParser.LEFT -> when (ctx.OUTER()) {
                    null -> From.Join.Type.LEFT
                    else -> From.Join.Type.LEFT_OUTER
                }
                GeneratedParser.RIGHT -> when (ctx.OUTER()) {
                    null -> From.Join.Type.RIGHT
                    else -> From.Join.Type.RIGHT_OUTER
                }
                GeneratedParser.FULL -> when (ctx.OUTER()) {
                    null -> From.Join.Type.FULL
                    else -> From.Join.Type.FULL_OUTER
                }
                GeneratedParser.OUTER -> {
                    // TODO https://github.com/partiql/partiql-spec/issues/41
                    // TODO https://github.com/partiql/partiql-lang-kotlin/issues/1013
                    From.Join.Type.FULL_OUTER
                }
                else -> null
            }
        }

        override fun visitTableQualifiedJoin(ctx: GeneratedParser.TableQualifiedJoinContext) = translate(ctx) {
            val lhs = visitAs<From>(ctx.lhs)
            val rhs = visitAs<From>(ctx.rhs)
            val type = convertJoinType(ctx.joinType())
            val condition = ctx.joinSpec()?.let { visitExpr(it.expr()) }
            fromJoin(lhs, rhs, type, condition)
        }

        override fun visitTableBaseRefSymbol(ctx: GeneratedParser.TableBaseRefSymbolContext) = translate(ctx) {
            val expr = visitAs<Expr>(ctx.source)
            val asAlias = visitSymbolPrimitive(ctx.symbolPrimitive())
            fromValue(expr, From.Value.Type.SCAN, asAlias, null, null)
        }

        override fun visitTableWrapped(ctx: GeneratedParser.TableWrappedContext): AstNode = visit(ctx.tableReference())

        override fun visitJoinSpec(ctx: GeneratedParser.JoinSpecContext) = visitExpr(ctx.expr())

        override fun visitJoinRhsTableJoined(ctx: GeneratedParser.JoinRhsTableJoinedContext) =
            visitAs<From>(ctx.tableReference())

        /**
         * SIMPLE EXPRESSIONS
         */

        override fun visitOr(ctx: GeneratedParser.OrContext) = translate(ctx) {
            convertBinaryExpr(ctx.lhs, ctx.rhs, Expr.Binary.Op.OR)
        }

        override fun visitAnd(ctx: GeneratedParser.AndContext) = translate(ctx) {
            convertBinaryExpr(ctx.lhs, ctx.rhs, Expr.Binary.Op.AND)
        }

        override fun visitNot(ctx: GeneratedParser.NotContext) = translate(ctx) {
            val expr = visit(ctx.exprNot()) as Expr
            exprUnary(Expr.Unary.Op.NOT, expr)
        }

        override fun visitMathOp00(ctx: GeneratedParser.MathOp00Context) = translate(ctx) {
            if (ctx.parent != null) return@translate visit(ctx.parent)
            convertBinaryExpr(ctx.lhs, ctx.rhs, convertBinaryOp(ctx.op))
        }

        override fun visitMathOp01(ctx: GeneratedParser.MathOp01Context) = translate(ctx) {
            if (ctx.parent != null) return@translate visit(ctx.parent)
            convertBinaryExpr(ctx.lhs, ctx.rhs, convertBinaryOp(ctx.op))
        }

        override fun visitMathOp02(ctx: GeneratedParser.MathOp02Context) = translate(ctx) {
            if (ctx.parent != null) return@translate visit(ctx.parent)
            convertBinaryExpr(ctx.lhs, ctx.rhs, convertBinaryOp(ctx.op))
        }

        override fun visitValueExpr(ctx: GeneratedParser.ValueExprContext) = translate(ctx) {
            if (ctx.parent != null) return@translate visit(ctx.parent)
            val expr = visit(ctx.rhs) as Expr
            exprUnary(convertUnaryOp(ctx.sign), expr)
        }

        private fun convertBinaryExpr(lhs: ParserRuleContext, rhs: ParserRuleContext, op: Expr.Binary.Op): Expr {
            val l = visit(lhs) as Expr
            val r = visit(rhs) as Expr
            return exprBinary(op, l, r)
        }

        private fun convertBinaryOp(token: Token) = when (token.type) {
            GeneratedParser.AMPERSAND -> Expr.Binary.Op.BITWISE_AND
            GeneratedParser.AND -> Expr.Binary.Op.AND
            GeneratedParser.OR -> Expr.Binary.Op.OR
            GeneratedParser.ASTERISK -> Expr.Binary.Op.TIMES
            GeneratedParser.SLASH_FORWARD -> Expr.Binary.Op.DIVIDE
            GeneratedParser.PLUS -> Expr.Binary.Op.PLUS
            GeneratedParser.MINUS -> Expr.Binary.Op.MINUS
            GeneratedParser.PERCENT -> Expr.Binary.Op.MODULO
            GeneratedParser.CONCAT -> Expr.Binary.Op.CONCAT
            GeneratedParser.ANGLE_LEFT -> Expr.Binary.Op.LT
            GeneratedParser.LT_EQ -> Expr.Binary.Op.LTE
            GeneratedParser.ANGLE_RIGHT -> Expr.Binary.Op.GT
            GeneratedParser.GT_EQ -> Expr.Binary.Op.GTE
            GeneratedParser.NEQ -> Expr.Binary.Op.NE
            GeneratedParser.EQ -> Expr.Binary.Op.EQ
            else -> throw error(token, "Invalid binary operator")
        }

        private fun convertUnaryOp(token: Token) = when (token.type) {
            GeneratedParser.PLUS -> Expr.Unary.Op.POS
            GeneratedParser.MINUS -> Expr.Unary.Op.NEG
            GeneratedParser.NOT -> Expr.Unary.Op.NOT
            else -> throw error(token, "Invalid unary operator")
        }

        /**
         *
         * PREDICATES
         *
         */

        override fun visitPredicateComparison(ctx: GeneratedParser.PredicateComparisonContext) = translate(ctx) {
            val op = convertBinaryOp(ctx.op)
            convertBinaryExpr(ctx.lhs, ctx.rhs, op)
        }

        /**
         * TODO Fix the IN collection grammar, also label alternative forms
         *  - https://github.com/partiql/partiql-lang-kotlin/issues/1115
         *   - https://github.com/partiql/partiql-lang-kotlin/issues/1113
         */
        override fun visitPredicateIn(ctx: GeneratedParser.PredicateInContext) = translate(ctx) {
            val lhs = visitAs<Expr>(ctx.lhs)
            val rhs = visitAs<Expr>(ctx.rhs ?: ctx.expr()).let {
                // Wrap rhs in an array unless it's a query or already a collection
                if (it is Expr.SFW || it is Expr.Collection || ctx.PAREN_LEFT() == null) {
                    it
                } else {
                    // IN ( expr )
                    exprCollection(Expr.Collection.Type.LIST, listOf(it))
                }
            }
            val not = ctx.NOT() != null
            exprInCollection(lhs, rhs, not)
        }

        override fun visitPredicateIs(ctx: GeneratedParser.PredicateIsContext) = translate(ctx) {
            val value = visitAs<Expr>(ctx.lhs)
            val type = visitAs<Type>(ctx.type())
            val not = ctx.NOT() != null
            exprIsType(value, type, not)
        }

        override fun visitPredicateBetween(ctx: GeneratedParser.PredicateBetweenContext) = translate(ctx) {
            val value = visitAs<Expr>(ctx.lhs)
            val lower = visitAs<Expr>(ctx.lower)
            val upper = visitAs<Expr>(ctx.upper)
            val not = ctx.NOT() != null
            exprBetween(value, lower, upper, not)
        }

        override fun visitPredicateLike(ctx: GeneratedParser.PredicateLikeContext) = translate(ctx) {
            val value = visitAs<Expr>(ctx.lhs)
            val pattern = visitAs<Expr>(ctx.rhs)
            val escape = visitOrNull<Expr>(ctx.escape)
            val not = ctx.NOT() != null
            exprLike(value, pattern, escape, not)
        }

        /**
         *
         * PRIMARY EXPRESSIONS
         *
         */

        override fun visitExprTermWrappedQuery(ctx: GeneratedParser.ExprTermWrappedQueryContext): AstNode =
            visit(ctx.expr())

        override fun visitVariableIdentifier(ctx: GeneratedParser.VariableIdentifierContext) = translate(ctx) {
            val symbol = ctx.ident.getStringValue()
            val case = when (ctx.ident.type) {
                GeneratedParser.IDENTIFIER -> Identifier.CaseSensitivity.INSENSITIVE
                else -> Identifier.CaseSensitivity.SENSITIVE
            }
            val scope = when (ctx.qualifier) {
                null -> Expr.Var.Scope.DEFAULT
                else -> Expr.Var.Scope.LOCAL
            }
            exprVar(identifierSymbol(symbol, case), scope)
        }

        override fun visitVariableKeyword(ctx: GeneratedParser.VariableKeywordContext) = translate(ctx) {
            val symbol = ctx.key.text
            val case = Identifier.CaseSensitivity.INSENSITIVE
            val scope = when (ctx.qualifier) {
                null -> Expr.Var.Scope.DEFAULT
                else -> Expr.Var.Scope.LOCAL
            }
            exprVar(identifierSymbol(symbol, case), scope)
        }

        override fun visitParameter(ctx: GeneratedParser.ParameterContext) = translate(ctx) {
            val index = parameters[ctx.QUESTION_MARK().symbol.tokenIndex] ?: throw error(
                ctx, "Unable to find index of parameter."
            )
            exprParameter(index)
        }

        override fun visitSequenceConstructor(ctx: GeneratedParser.SequenceConstructorContext) = translate(ctx) {
            val expressions = visitOrEmpty<Expr>(ctx.expr())
            val type = when (ctx.datatype.type) {
                GeneratedParser.LIST -> Expr.Collection.Type.LIST
                GeneratedParser.SEXP -> Expr.Collection.Type.SEXP
                else -> throw error(ctx.datatype, "Invalid sequence type")
            }
            exprCollection(type, expressions)
        }

        override fun visitExprPrimaryPath(ctx: GeneratedParser.ExprPrimaryPathContext) = translate(ctx) {
            val base = visitAs<Expr>(ctx.exprPrimary())
            val steps = ctx.pathStep().map { visit(it) as Expr.Path.Step }
            exprPath(base, steps)
        }

        override fun visitPathStepIndexExpr(ctx: GeneratedParser.PathStepIndexExprContext) = translate(ctx) {
            val key = visitAs<Expr>(ctx.key)
            exprPathStepIndex(key)
        }

        override fun visitPathStepDotExpr(ctx: GeneratedParser.PathStepDotExprContext) = translate(ctx) {
            val symbol = visitSymbolPrimitive(ctx.symbolPrimitive())
            exprPathStepSymbol(symbol)
        }

        override fun visitPathStepIndexAll(ctx: GeneratedParser.PathStepIndexAllContext) = translate(ctx) {
            exprPathStepWildcard()
        }

        override fun visitPathStepDotAll(ctx: GeneratedParser.PathStepDotAllContext) = translate(ctx) {
            exprPathStepUnpivot()
        }

        override fun visitValues(ctx: GeneratedParser.ValuesContext) = translate(ctx) {
            val rows = visitOrEmpty<Expr.Collection>(ctx.valueRow())
            exprCollection(Expr.Collection.Type.BAG, rows)
        }

        override fun visitValueRow(ctx: GeneratedParser.ValueRowContext) = translate(ctx) {
            val expressions = visitOrEmpty<Expr>(ctx.expr())
            exprCollection(Expr.Collection.Type.LIST, expressions)
        }

        override fun visitValueList(ctx: GeneratedParser.ValueListContext) = translate(ctx) {
            val expressions = visitOrEmpty<Expr>(ctx.expr())
            exprCollection(Expr.Collection.Type.LIST, expressions)
        }

        override fun visitExprGraphMatchMany(ctx: GeneratedParser.ExprGraphMatchManyContext) = translate(ctx) {
            val graph = visit(ctx.exprPrimary()) as Expr
            val pattern = visitGpmlPatternList(ctx.gpmlPatternList())
            exprMatch(graph, pattern)
        }

        override fun visitExprGraphMatchOne(ctx: GeneratedParser.ExprGraphMatchOneContext) = translate(ctx) {
            val graph = visit(ctx.exprPrimary()) as Expr
            val pattern = visitGpmlPattern(ctx.gpmlPattern())
            exprMatch(graph, pattern)
        }

        override fun visitExprTermCurrentUser(ctx: GeneratedParser.ExprTermCurrentUserContext) = translate(ctx) {
            exprSessionAttribute(Expr.SessionAttribute.Attribute.CURRENT_USER)
        }

        override fun visitExprTermCurrentDate(ctx: org.partiql.parser.antlr.PartiQLParser.ExprTermCurrentDateContext) =
            translate(ctx) {
                exprSessionAttribute(Expr.SessionAttribute.Attribute.CURRENT_DATE)
            }

        /**
         *
         * FUNCTIONS
         *
         */

        override fun visitNullIf(ctx: GeneratedParser.NullIfContext) = translate(ctx) {
            val value = visitExpr(ctx.expr(0))
            val nullifier = visitExpr(ctx.expr(1))
            exprNullIf(value, nullifier)
        }

        override fun visitCoalesce(ctx: GeneratedParser.CoalesceContext) = translate(ctx) {
            val expressions = visitOrEmpty<Expr>(ctx.expr())
            exprCoalesce(expressions)
        }

        override fun visitCaseExpr(ctx: GeneratedParser.CaseExprContext) = translate(ctx) {
            val expr = ctx.case_?.let { visitExpr(it) }
            val branches = ctx.whens.indices.map { i ->
                // consider adding locations
                val w = visitExpr(ctx.whens[i])
                val t = visitExpr(ctx.thens[i])
                exprCaseBranch(w, t)
            }
            val default = ctx.else_?.let { visitExpr(it) }
            exprCase(expr, branches, default)
        }

        override fun visitCast(ctx: GeneratedParser.CastContext) = translate(ctx) {
            val expr = visitExpr(ctx.expr())
            val type = visitAs<Type>(ctx.type())
            exprCast(expr, type)
        }

        override fun visitCanCast(ctx: GeneratedParser.CanCastContext) = translate(ctx) {
            val expr = visitExpr(ctx.expr())
            val type = visitAs<Type>(ctx.type())
            exprCanCast(expr, type)
        }

        override fun visitCanLosslessCast(ctx: GeneratedParser.CanLosslessCastContext) = translate(ctx) {
            val expr = visitExpr(ctx.expr())
            val type = visitAs<Type>(ctx.type())
            exprCanLosslessCast(expr, type)
        }

        override fun visitFunctionCallIdent(ctx: GeneratedParser.FunctionCallIdentContext) = translate(ctx) {
            val function = visitSymbolPrimitive(ctx.name)
            val args = visitOrEmpty<Expr>(ctx.expr())
            exprCall(function, args)
        }

        override fun visitFunctionCallReserved(ctx: GeneratedParser.FunctionCallReservedContext) = translate(ctx) {
            val function = ctx.name.text.toIdentifier()
            val args = visitOrEmpty<Expr>(ctx.expr())
            exprCall(function, args)
        }

        /**
         *
         * FUNCTIONS WITH SPECIAL FORMS
         *
         */

        override fun visitDateFunction(ctx: GeneratedParser.DateFunctionContext) = translate(ctx) {
            val field = try {
                DatetimeField.valueOf(ctx.dt.text.uppercase())
            } catch (ex: IllegalArgumentException) {
                throw error(ctx.dt, "Expected one of: ${DatetimeField.values().joinToString()}", ex)
            }
            val lhs = visitExpr(ctx.expr(0))
            val rhs = visitExpr(ctx.expr(1))
            when {
                ctx.DATE_ADD() != null -> exprDateAdd(field, lhs, rhs)
                ctx.DATE_DIFF() != null -> exprDateDiff(field, lhs, rhs)
                else -> throw error(ctx, "Expected DATE_ADD or DATE_DIFF")
            }
        }

        /**
         * TODO Add labels to each alternative, https://github.com/partiql/partiql-lang-kotlin/issues/1113
         */
        override fun visitSubstring(ctx: GeneratedParser.SubstringContext) = translate(ctx) {
            if (ctx.FROM() == null) {
                // normal form
                val function = "SUBSTRING".toIdentifier()
                val args = visitOrEmpty<Expr>(ctx.expr())
                exprCall(function, args)
            } else {
                // special form
                val value = visitExpr(ctx.expr(0))
                val start = visitOrNull<Expr>(ctx.expr(1))
                val length = visitOrNull<Expr>(ctx.expr(2))
                exprSubstring(value, start, length)
            }
        }

        /**
         * TODO Add labels to each alternative, https://github.com/partiql/partiql-lang-kotlin/issues/1113
         */
        override fun visitPosition(ctx: GeneratedParser.PositionContext) = translate(ctx) {
            if (ctx.IN() == null) {
                // normal form
                val function = "POSITION".toIdentifier()
                val args = visitOrEmpty<Expr>(ctx.expr())
                exprCall(function, args)
            } else {
                // special form
                val lhs = visitExpr(ctx.expr(0))
                val rhs = visitExpr(ctx.expr(1))
                exprPosition(lhs, rhs)
            }
        }

        /**
         * TODO Add labels to each alternative, https://github.com/partiql/partiql-lang-kotlin/issues/1113
         */
        override fun visitOverlay(ctx: GeneratedParser.OverlayContext) = translate(ctx) {
            if (ctx.PLACING() == null) {
                // normal form
                val function = "OVERLAY".toIdentifier()
                val args = visitOrEmpty<Expr>(ctx.expr())
                exprCall(function, args)
            } else {
                // special form
                val value = visitExpr(ctx.expr(0))
                val overlay = visitExpr(ctx.expr(1))
                val start = visitExpr(ctx.expr(2))
                val length = visitOrNull<Expr>(ctx.expr(3))
                exprOverlay(value, overlay, start, length)
            }
        }

        /**
         * COUNT(*)
         */
        override fun visitCountAll(ctx: GeneratedParser.CountAllContext) = translate(ctx) {
            val function = "COUNT_STAR".toIdentifier()
            exprAgg(function, emptyList(), SetQuantifier.ALL)
        }

        override fun visitExtract(ctx: GeneratedParser.ExtractContext) = translate(ctx) {
            val field = try {
                DatetimeField.valueOf(ctx.IDENTIFIER().text.uppercase())
            } catch (ex: IllegalArgumentException) {
                throw error(ctx.IDENTIFIER().symbol, "Expected one of: ${DatetimeField.values().joinToString()}", ex)
            }
            val source = visitExpr(ctx.expr())
            exprExtract(field, source)
        }

        override fun visitTrimFunction(ctx: GeneratedParser.TrimFunctionContext) = translate(ctx) {
            val spec = ctx.mod?.let {
                try {
                    Expr.Trim.Spec.valueOf(it.text.uppercase())
                } catch (ex: IllegalArgumentException) {
                    throw error(it, "Expected on of: ${Expr.Trim.Spec.values().joinToString()}", ex)
                }
            }
            val (chars, value) = when (ctx.expr().size) {
                1 -> null to visitExpr(ctx.expr(0))
                2 -> visitExpr(ctx.expr(0)) to visitExpr(ctx.expr(1))
                else -> throw error(ctx, "Expected one or two TRIM expression arguments")
            }
            exprTrim(value, chars, spec)
        }

        override fun visitAggregateBase(ctx: GeneratedParser.AggregateBaseContext) = translate(ctx) {
            val function = ctx.func.text.toIdentifier()
            val args = listOf(visitExpr(ctx.expr()))
            val setq = convertSetQuantifier(ctx.setQuantifierStrategy())
            exprAgg(function, args, setq)
        }

        /**
         * Window Functions
         */

        override fun visitLagLeadFunction(ctx: GeneratedParser.LagLeadFunctionContext) = translate(ctx) {
            val function = when {
                ctx.LAG() != null -> Expr.Window.Function.LAG
                ctx.LEAD() != null -> Expr.Window.Function.LEAD
                else -> throw error(ctx, "Expected LAG or LEAD")
            }
            val expression = visitExpr(ctx.expr(0))
            val offset = visitOrNull<Expr>(ctx.expr(1))
            val default = visitOrNull<Expr>(ctx.expr(2))
            val over = visitOver(ctx.over())
            if (over.sorts == null) {
                throw error(ctx.over(), "$function requires Window ORDER BY")
            }
            exprWindow(function, expression, offset, default, over)
        }

        override fun visitOver(ctx: GeneratedParser.OverContext) = translate(ctx) {
            val partitions = ctx.windowPartitionList()?.let { visitOrEmpty<Expr>(it.expr()) }
            val sorts = ctx.windowSortSpecList()?.let { visitOrEmpty<Sort>(it.orderSortSpec()) }
            exprWindowOver(partitions, sorts)
        }

        /**
         *
         * LITERALS
         *
         */

        override fun visitBag(ctx: GeneratedParser.BagContext) = translate(ctx) {
            val expressions = visitOrEmpty<Expr>(ctx.expr())
            exprCollection(Expr.Collection.Type.BAG, expressions)
        }

        override fun visitLiteralDecimal(ctx: GeneratedParser.LiteralDecimalContext) = translate(ctx) {
            val decimal = try {
                val v = ctx.LITERAL_DECIMAL().text.trim()
                BigDecimal(v, MathContext(38, RoundingMode.HALF_EVEN))
            } catch (e: NumberFormatException) {
                throw error(ctx, "Invalid decimal literal", e)
            }
            exprLit(decimalValue(decimal))
        }

        override fun visitArray(ctx: GeneratedParser.ArrayContext) = translate(ctx) {
            val expressions = visitOrEmpty<Expr>(ctx.expr())
            exprCollection(Expr.Collection.Type.ARRAY, expressions)
        }

        override fun visitLiteralNull(ctx: GeneratedParser.LiteralNullContext) = translate(ctx) {
            exprLit(nullValue())
        }

        override fun visitLiteralMissing(ctx: GeneratedParser.LiteralMissingContext) = translate(ctx) {
            exprLit(missingValue())
        }

        override fun visitLiteralTrue(ctx: GeneratedParser.LiteralTrueContext) = translate(ctx) {
            exprLit(boolValue(true))
        }

        override fun visitLiteralFalse(ctx: GeneratedParser.LiteralFalseContext) = translate(ctx) {
            exprLit(boolValue(false))
        }

        override fun visitLiteralIon(ctx: GeneratedParser.LiteralIonContext) = translate(ctx) {
            val value = try {
                loadSingleElement(ctx.ION_CLOSURE().getStringValue())
            } catch (e: IonElementException) {
                throw error(ctx, "Unable to parse Ion value.", e)
            }
            exprIon(value)
        }

        override fun visitLiteralString(ctx: GeneratedParser.LiteralStringContext) = translate(ctx) {
            val value = ctx.LITERAL_STRING().getStringValue()
            exprLit(stringValue(value))
        }

        override fun visitLiteralInteger(ctx: GeneratedParser.LiteralIntegerContext) = translate(ctx) {
            val n = ctx.LITERAL_INTEGER().text

            // 1st, try parse as int
            try {
                val v = n.toInt(10)
                return@translate exprLit(int32Value(v))
            } catch (ex: NumberFormatException) {
                // ignore
            }

            // 2nd, try parse as long
            try {
                val v = n.toLong(10)
                return@translate exprLit(int64Value(v))
            } catch (ex: NumberFormatException) {
                // ignore
            }

            // 3rd, try parse as BigInteger
            try {
                val v = BigInteger(n)
                return@translate exprLit(intValue(v))
            } catch (ex: NumberFormatException) {
                throw ex
            }
        }

        override fun visitLiteralDate(ctx: GeneratedParser.LiteralDateContext) = translate(ctx) {
            val pattern = ctx.LITERAL_STRING().symbol
            val dateString = ctx.LITERAL_STRING().getStringValue()
            if (DATE_PATTERN_REGEX.matches(dateString).not()) {
                throw error(pattern, "Expected DATE string to be of the format yyyy-MM-dd")
            }
            val value = try {
                LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
            } catch (e: DateTimeParseException) {
                throw error(pattern, e.localizedMessage, e)
            } catch (e: IndexOutOfBoundsException) {
                throw error(pattern, e.localizedMessage, e)
            }
            val date = DateTimeValue.date(value.year, value.monthValue, value.dayOfMonth)
            exprLit(dateValue(date))
        }

        override fun visitLiteralTime(ctx: GeneratedParser.LiteralTimeContext) = translate(ctx) {
            val (timeString, precision) = getTimeStringAndPrecision(ctx.LITERAL_STRING(), ctx.LITERAL_INTEGER())
            val time = try {
                DateTimeUtils.parseTimeLiteral(timeString)
            } catch (e: DateTimeException) {
                throw error(ctx, "Invalid Date Time Literal", e)
            }
            val value = time.toPrecision(precision)
            exprLit(timeValue(value))
        }

        override fun visitLiteralTimestamp(ctx: GeneratedParser.LiteralTimestampContext) = translate(ctx) {
            val (timeString, precision) = getTimeStringAndPrecision(ctx.LITERAL_STRING(), ctx.LITERAL_INTEGER())
            val timestamp = try {
                DateTimeUtils.parseTimestamp(timeString)
            } catch (e: DateTimeException) {
                throw error(ctx, "Invalid Date Time Literal", e)
            }
            val value = timestamp.toPrecision(precision)
            exprLit(timestampValue(value))
        }

        override fun visitTuple(ctx: GeneratedParser.TupleContext) = translate(ctx) {
            val fields = ctx.pair().map {
                val k = visitExpr(it.lhs)
                val v = visitExpr(it.rhs)
                exprStructField(k, v)
            }
            exprStruct(fields)
        }

        /**
         *
         * TYPES
         *
         */

        override fun visitTypeAtomic(ctx: GeneratedParser.TypeAtomicContext) = translate(ctx) {
            when (ctx.datatype.type) {
                GeneratedParser.NULL -> typeNullType()
                GeneratedParser.BOOL, GeneratedParser.BOOLEAN -> typeBool()
                GeneratedParser.SMALLINT, GeneratedParser.INT2, GeneratedParser.INTEGER2 -> typeInt2()
                GeneratedParser.INT4, GeneratedParser.INTEGER4 -> typeInt4()
                GeneratedParser.BIGINT, GeneratedParser.INT8, GeneratedParser.INTEGER8 -> typeInt8()
                GeneratedParser.INT, GeneratedParser.INTEGER -> typeInt()
                GeneratedParser.FLOAT -> typeFloat32()
                GeneratedParser.DOUBLE -> typeFloat64()
                GeneratedParser.REAL -> typeReal()
                GeneratedParser.TIMESTAMP -> typeTimestamp(null)
                GeneratedParser.CHAR, GeneratedParser.CHARACTER -> typeChar(null)
                GeneratedParser.MISSING -> typeMissing()
                GeneratedParser.STRING -> typeString(null)
                GeneratedParser.SYMBOL -> typeSymbol()
                // TODO https://github.com/partiql/partiql-lang-kotlin/issues/1125
                GeneratedParser.BLOB -> typeBlob(null)
                GeneratedParser.CLOB -> typeClob(null)
                GeneratedParser.DATE -> typeDate()
                GeneratedParser.STRUCT -> typeStruct()
                GeneratedParser.TUPLE -> typeTuple()
                GeneratedParser.LIST -> typeList()
                GeneratedParser.SEXP -> typeSexp()
                GeneratedParser.BAG -> typeBag()
                GeneratedParser.ANY -> typeAny()
                else -> throw error(ctx, "Unknown atomic type.")
            }
        }

        override fun visitTypeVarChar(ctx: GeneratedParser.TypeVarCharContext) = translate(ctx) {
            val n = ctx.arg0?.text?.toInt()
            typeVarchar(n)
        }

        override fun visitTypeArgSingle(ctx: GeneratedParser.TypeArgSingleContext) = translate(ctx) {
            val n = ctx.arg0?.text?.toInt()
            when (ctx.datatype.type) {
                GeneratedParser.FLOAT -> when (n) {
                    32 -> typeFloat32()
                    64 -> typeFloat64()
                    else -> throw error(ctx.datatype, "Invalid FLOAT precision. Expected 32 or 64")
                }
                GeneratedParser.CHAR, GeneratedParser.CHARACTER -> typeChar(n)
                GeneratedParser.VARCHAR -> typeVarchar(n)
                else -> throw error(ctx.datatype, "Invalid datatype")
            }
        }

        override fun visitTypeArgDouble(ctx: GeneratedParser.TypeArgDoubleContext) = translate(ctx) {
            val arg0 = ctx.arg0?.text?.toInt()
            val arg1 = ctx.arg1?.text?.toInt()
            when (ctx.datatype.type) {
                GeneratedParser.DECIMAL, GeneratedParser.DEC -> typeDecimal(arg0, arg1)
                GeneratedParser.NUMERIC -> typeNumeric(arg0, arg1)
                else -> throw error(ctx.datatype, "Invalid datatype")
            }
        }

        override fun visitTypeTimeZone(ctx: GeneratedParser.TypeTimeZoneContext) = translate(ctx) {
            val precision = ctx.precision?.let {
                val p = ctx.precision.text.toInt()
                if (p < 0 || 9 < p) throw error(ctx.precision, "Unsupported time precision")
                p
            }
            when (ctx.ZONE()) {
                null -> typeTime(precision)
                else -> typeTimeWithTz(precision)
            }
        }

        override fun visitTypeCustom(ctx: GeneratedParser.TypeCustomContext) = translate(ctx) {
            typeCustom(ctx.text.uppercase())
        }

        private inline fun <reified T : AstNode> visitOrEmpty(ctx: List<ParserRuleContext>?): List<T> = when {
            ctx.isNullOrEmpty() -> emptyList()
            else -> ctx.map { visit(it) as T }
        }

        private inline fun <reified T : AstNode> visitOrNull(ctx: ParserRuleContext?): T? =
            ctx?.let { it.accept(this) as T }

        private inline fun <reified T : AstNode> visitAs(ctx: ParserRuleContext): T = visit(ctx) as T

        /**
         * Visiting a symbol to get a string, skip the wrapping, unwrapping, and location tracking.
         */
        private fun symbolToString(ctx: GeneratedParser.SymbolPrimitiveContext) = when (ctx.ident.type) {
            GeneratedParser.IDENTIFIER_QUOTED -> ctx.IDENTIFIER_QUOTED().getStringValue()
            GeneratedParser.IDENTIFIER -> ctx.IDENTIFIER().getStringValue()
            else -> throw error(ctx, "Invalid symbol reference.")
        }

        /**
         * Convert [ALL|DISTINCT] to SetQuantifier Enum
         */
        private fun convertSetQuantifier(ctx: GeneratedParser.SetQuantifierStrategyContext?): SetQuantifier? = when {
            ctx == null -> null
            ctx.ALL() != null -> SetQuantifier.ALL
            ctx.DISTINCT() != null -> SetQuantifier.DISTINCT
            else -> throw error(ctx, "Expected set quantifier ALL or DISTINCT")
        }

        /**
         * With the <string> and <int> nodes of a literal time expression, returns the parsed string and precision.
         * TIME (<int>)? (WITH TIME ZONE)? <string>
         */
        private fun getTimeStringAndPrecision(
            stringNode: TerminalNode,
            integerNode: TerminalNode?,
        ): Pair<String, Int> {
            val timeString = stringNode.getStringValue()
            val precision = when (integerNode) {
                null -> {
                    try {
                        getPrecisionFromTimeString(timeString)
                    } catch (e: Exception) {
                        throw error(stringNode.symbol, "Unable to parse precision.", e)
                    }
                }
                else -> {
                    val p = integerNode.text.toBigInteger().toInt()
                    if (p < 0 || 9 < p) throw error(integerNode.symbol, "Precision out of bounds")
                    p
                }
            }
            return timeString to precision
        }

        private fun getPrecisionFromTimeString(timeString: String): Int {
            val matcher = GENERIC_TIME_REGEX.toPattern().matcher(timeString)
            if (!matcher.find()) {
                throw IllegalArgumentException("Time string does not match the format 'HH:MM:SS[.ddd....][+|-HH:MM]'")
            }
            val fraction = matcher.group(1)?.removePrefix(".")
            return fraction?.length ?: 0
        }

        /**
         * Converts a Path expression into a Projection Item (either ALL or EXPR). Note: A Projection Item only allows a
         * subset of a typical Path expressions. See the following examples.
         *
         * Examples of valid projections are:
         *
         * ```partiql
         *      SELECT * FROM foo
         *      SELECT foo.* FROM foo
         *      SELECT f.* FROM foo as f
         *      SELECT foo.bar.* FROM foo
         *      SELECT f.bar.* FROM foo as f
         * ```
         * Also validates that the expression is valid for select list context. It does this by making
         * sure that expressions looking like the following do not appear:
         *
         * ```partiql
         *      SELECT foo[*] FROM foo
         *      SELECT f.*.bar FROM foo as f
         *      SELECT foo[1].* FROM foo
         *      SELECT foo.*.bar FROM foo
         * ```
         */
        protected fun convertPathToProjectionItem(ctx: ParserRuleContext, path: Expr.Path, alias: Identifier.Symbol?) =
            translate(ctx) {
                val steps = mutableListOf<Expr.Path.Step>()
                var containsIndex = false
                path.steps.forEachIndexed { index, step ->
                    // Only last step can have a '.*'
                    if (step is Expr.Path.Step.Unpivot && index != path.steps.lastIndex) {
                        throw error(ctx, "Projection item cannot unpivot unless at end.")
                    }
                    // No step can have an indexed wildcard: '[*]'
                    if (step is Expr.Path.Step.Wildcard) {
                        throw error(ctx, "Projection item cannot index using wildcard.")
                    }
                    // TODO If the last step is '.*', no indexing is allowed
                    // if (step.metas.containsKey(IsPathIndexMeta.TAG)) {
                    //     containsIndex = true
                    // }
                    if (step !is Expr.Path.Step.Unpivot) {
                        steps.add(step)
                    }
                }
                if (path.steps.last() is Expr.Path.Step.Unpivot && containsIndex) {
                    throw error(ctx, "Projection item use wildcard with any indexing.")
                }
                when {
                    path.steps.last() is Expr.Path.Step.Unpivot && steps.isEmpty() -> {
                        selectProjectItemAll(path.root)
                    }
                    path.steps.last() is Expr.Path.Step.Unpivot -> {
                        selectProjectItemAll(exprPath(path.root, steps))
                    }
                    else -> {
                        selectProjectItemExpression(path, alias)
                    }
                }
            }

        private fun TerminalNode.getStringValue(): String = this.symbol.getStringValue()

        private fun Token.getStringValue(): String = when (this.type) {
            GeneratedParser.IDENTIFIER -> this.text
            GeneratedParser.IDENTIFIER_QUOTED -> this.text.removePrefix("\"").removeSuffix("\"").replace("\"\"", "\"")
            GeneratedParser.LITERAL_STRING -> this.text.removePrefix("'").removeSuffix("'").replace("''", "'")
            GeneratedParser.ION_CLOSURE -> this.text.removePrefix("`").removeSuffix("`")
            else -> throw error(this, "Unsupported token for grabbing string value.")
        }

        private fun String.toIdentifier(): Identifier.Symbol = identifierSymbol(
            symbol = this,
            caseSensitivity = Identifier.CaseSensitivity.INSENSITIVE,
        )

        private fun String.toBigInteger() = BigInteger(this, 10)

        private fun assertIntegerElement(token: Token?, value: IonElement?) {
            if (value == null || token == null) return
            if (value !is IntElement) throw error(token, "Expected an integer value.")
            if (value.integerSize == IntElementSize.BIG_INTEGER || value.longValue > Int.MAX_VALUE || value.longValue < Int.MIN_VALUE) throw error(
                token, "Type parameter exceeded maximum value"
            )
        }

        private enum class ExplainParameters {
            TYPE, FORMAT;

            fun getCompliantString(target: String?, input: Token): String = when (target) {
                null -> input.text!!
                else -> throw error(input, "Cannot set EXPLAIN parameter ${this.name} multiple times.")
            }
        }
    }
}
