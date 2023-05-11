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

package org.partiql.parser

import com.amazon.ion.Decimal
import com.amazon.ionelement.api.IntElement
import com.amazon.ionelement.api.IntElementSize
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.IonElementException
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionDecimal
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionNull
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionSymbol
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
import org.partiql.ast.Case
import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.GraphMatch
import org.partiql.ast.GroupBy
import org.partiql.ast.Identifier
import org.partiql.ast.Let
import org.partiql.ast.OnConflict
import org.partiql.ast.OrderBy
import org.partiql.ast.Over
import org.partiql.ast.Path
import org.partiql.ast.Returning
import org.partiql.ast.Select
import org.partiql.ast.SetQuantifier
import org.partiql.ast.Statement
import org.partiql.ast.TableDefinition
import org.partiql.ast.Type
import org.partiql.ast.builder.AstFactory
import org.partiql.parser.antlr.PartiQLBaseVisitor
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import java.math.RoundingMode
import java.nio.channels.ClosedByInterruptException
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetTime
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
            e: RecognitionException?
        ) {
            throw PartiQLLexerException(
                token = offendingSymbol?.toString() ?: "",
                message = msg,
                cause = e,
                location = SourceLocation(line, charPositionInLine + 1, msg.length),
            )
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
            e: RecognitionException?
        ) {
            if (offendingSymbol is Token) {
                throw PartiQLParserException(
                    rule = e?.ctx?.toString(rules) ?: "UNKNOWN",
                    message = msg,
                    cause = e,
                    location = SourceLocation(line, charPositionInLine + 1, msg.length),
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
     * Translate an ANTLR ParseTree to a PartiQL AST.
     */
    private class Visitor(
        private val locations: SourceLocations.Mutable,
        private val parameters: Map<Int, Int> = mapOf(),
    ) : PartiQLBaseVisitor<AstNode>() {

        // Currently we directly invoke constructors as it's simpler and equivalent to the default factory.
        private val factory = AstFactory.DEFAULT

        private val id: () -> Int = run {
            var i = 1
            { i++ }
        }

        companion object {

            private val rules = GeneratedParser.ruleNames.asList()

            /**
             * Expose an (internal) friendly entry point into the traversal; mostly for keeping mutable state contained.
             */
            fun translate(
                source: String,
                tokens: CountingTokenStream,
                tree: GeneratedParser.RootContext
            ): PartiQLParser.Result {
                val locations = SourceLocations.Mutable()
                val visitor = Visitor(locations, tokens.parameterIndexes)
                val root = visitor.visitAs<AstNode>(tree)
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
                context: MutableMap<String, Any> = mutableMapOf(),
            ) = PartiQLParserException(
                ctx.toString(rules),
                message,
                cause,
                SourceLocation(
                    line = ctx.start.line,
                    offset = ctx.start.charPositionInLine + 1,
                    length = ctx.stop.stopIndex - ctx.start.startIndex,
                ),
                context,
            )

            fun error(
                token: Token,
                message: String,
                cause: Throwable? = null,
                context: MutableMap<String, Any> = mutableMapOf(),
            ) = PartiQLLexerException(
                GeneratedParser.VOCABULARY.getSymbolicName(token.type),
                message,
                cause,
                SourceLocation(
                    line = token.line,
                    offset = token.charPositionInLine + 1,
                    length = token.stopIndex - token.startIndex,
                ),
                context,
            )

            internal enum class DateTimePart {
                YEAR, MONTH, DAY, HOUR, MINUTE, SECOND, TIMEZONE_HOUR, TIMEZONE_MINUTE;
            }

            internal val DATE_PATTERN_REGEX = Regex("\\d\\d\\d\\d-\\d\\d-\\d\\d")

            internal val GENERIC_TIME_REGEX = Regex("\\d\\d:\\d\\d:\\d\\d(\\.\\d*)?([+|-]\\d\\d:\\d\\d)?")
        }

        /**
         * Each visit attaches source locations from the given parse tree node; inline because gotta go fast.
         */
        private inline fun <T : AstNode> translate(ctx: ParserRuleContext, translate: (factory: AstFactory) -> T): T {
            val node = translate(factory)
            if (ctx.start != null) {
                locations[node.id] = SourceLocation(
                    line = ctx.start.line,
                    offset = ctx.start.charPositionInLine + 1,
                    length = (ctx.stop?.stopIndex ?: ctx.start.stopIndex) - ctx.start.startIndex + 1,
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
                            ExplainParameters.valueOf(option.param.text.toUpperCase())
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
                    Statement.Explain(
                        id = id(),
                        target = Statement.Explain.Target.Domain(
                            id = id(),
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
                GeneratedParser.IDENTIFIER_QUOTED -> Identifier(
                    id(),
                    ctx.IDENTIFIER_QUOTED().getStringValue(),
                    Identifier.CaseSensitivity.SENSITIVE,
                )
                GeneratedParser.IDENTIFIER -> Identifier(
                    id(),
                    ctx.IDENTIFIER_QUOTED().getStringValue(),
                    Identifier.CaseSensitivity.INSENSITIVE,
                )
                else -> throw PartiQLParserException("Invalid symbol reference.")
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
            Statement.DDL.DropTable(id(), table)
        }

        override fun visitDropIndex(ctx: GeneratedParser.DropIndexContext) = translate(ctx) {
            val table = visitSymbolPrimitive(ctx.table.symbolPrimitive())
            val target = visitSymbolPrimitive(ctx.target)
            Statement.DDL.DropIndex(id(), table, target)
        }

        override fun visitCreateTable(ctx: GeneratedParser.CreateTableContext) = translate(ctx) {
            val (name, case) = symbolCased(ctx.tableName().symbolPrimitive())
            val identifier = Identifier(id(), name, case)
            val definition = ctx.tableDef()?.let { visitTableDef(it) }
            Statement.DDL.CreateTable(id(), identifier, definition)
        }

        override fun visitCreateIndex(ctx: GeneratedParser.CreateIndexContext) = translate(ctx) {
            val name = visitOrNull<Identifier>(ctx.symbolPrimitive())
            val table = visitSymbolPrimitive(ctx.tableName().symbolPrimitive())
            val fields = ctx.pathSimple().map { path -> visitPathSimple(path) }
            Statement.DDL.CreateIndex(id(), name, table, fields)
        }

        override fun visitTableDef(ctx: GeneratedParser.TableDefContext) = translate(ctx) {
            // Column Definitions are the only thing we currently allow as table definition parts
            val columns = ctx.tableDefPart().filterIsInstance<GeneratedParser.ColumnDeclarationContext>().map {
                visitColumnDeclaration(it)
            }
            TableDefinition(id(), columns)
        }

        override fun visitColumnDeclaration(ctx: GeneratedParser.ColumnDeclarationContext) = translate(ctx) {
            val name = symbol(ctx.columnName().symbolPrimitive())
            val type = visit(ctx.type()) as Type
            val constraints = ctx.columnConstraint().map {
                visitColumnConstraint(it)
            }
            TableDefinition.Column(id(), name, type, constraints)
        }

        override fun visitColumnConstraint(ctx: GeneratedParser.ColumnConstraintContext) = translate(ctx) {
            val identifier = ctx.columnConstraintName()?.let { symbol(it.symbolPrimitive()) }
            val body = visit(ctx.columnConstraintDef()) as TableDefinition.Column.Constraint.Body
            TableDefinition.Column.Constraint(id(), identifier, body)
        }

        override fun visitColConstrNotNull(ctx: GeneratedParser.ColConstrNotNullContext) = translate(ctx) {
            TableDefinition.Column.Constraint.Body.NotNull(id())
        }

        override fun visitColConstrNull(ctx: GeneratedParser.ColConstrNullContext) = translate(ctx) {
            TableDefinition.Column.Constraint.Body.Nullable(id())
        }

        /**
         *
         * EXECUTE
         *
         */

        override fun visitQueryExec(ctx: GeneratedParser.QueryExecContext) = visitExecCommand(ctx.execCommand())

        override fun visitExecCommand(ctx: GeneratedParser.ExecCommandContext) = translate(ctx) {
            val procedure = stringOrErr(ctx.name)
            val args = visitOrEmpty<Expr>(ctx.args)
            Statement.Exec(id(), procedure, args)
        }

        /**
         *
         * DATA MANIPULATION LANGUAGE (DML)
         *
         */

        /**
         * The PartiQL grammars allows for multiple DML commands in one UPDATE statement.
         * This function unwraps DML commands to the more limited DML.Batch.Op commands.
         */
        override fun visitDmlBaseWrapper(ctx: GeneratedParser.DmlBaseWrapperContext) = translate(ctx) {
            val table = when {
                ctx.updateClause() != null -> ctx.updateClause().tableBaseReference()
                ctx.fromClause() != null -> ctx.fromClause().tableReference()
                else -> throw error(ctx, "Expected UPDATE <table> or FROM <table>")
            }
            val from = visitAs<From>(table)
            val ops = ctx.dmlBaseCommand().map {
                val op = visitDmlBaseCommand(it)
                when (op) {
                    is Statement.DML.Update -> Statement.DML.Batch.Op.Set(op.id, op.assignments)
                    is Statement.DML.Remove -> Statement.DML.Batch.Op.Remove(op.id, op.target)
                    is Statement.DML.Delete -> Statement.DML.Batch.Op.Delete(op.id)
                    is Statement.DML.Insert -> Statement.DML.Batch.Op.Insert(op.id, op.target, op.value, op.onConflict)
                    is Statement.DML.InsertValue -> Statement.DML.Batch.Op.InsertValue(
                        op.id,
                        op.target,
                        op.value,
                        op.index,
                        op.onConflict
                    )
                    else -> throw error(ctx, "Invalid DML operator in batch update")
                }
            }
            val where = ctx.whereClause()?.let { visitExpr(it.expr()) }
            val returning = ctx.returningClause()?.let { visitReturningClause(it) }
            Statement.DML.Batch(id(), from, ops, where, returning)
        }

        override fun visitDmlDelete(ctx: GeneratedParser.DmlDeleteContext) = visitDeleteCommand(ctx.deleteCommand())

        override fun visitDmlInsertReturning(ctx: GeneratedParser.DmlInsertReturningContext) =
            super.visit(ctx.insertCommandReturning()) as Statement.DML.InsertValue

        override fun visitDmlBase(ctx: GeneratedParser.DmlBaseContext) =
            super.visitDmlBaseCommand(ctx.dmlBaseCommand()) as Statement.DML

        override fun visitDmlBaseCommand(ctx: GeneratedParser.DmlBaseCommandContext) =
            super.visitDmlBaseCommand(ctx) as Statement.DML

        override fun visitRemoveCommand(ctx: GeneratedParser.RemoveCommandContext) = translate(ctx) {
            val target = visitPathSimple(ctx.pathSimple())
            Statement.DML.Remove(id(), target)
        }

        override fun visitDeleteCommand(ctx: GeneratedParser.DeleteCommandContext) = translate(ctx) {
            val from = visitAs<From>(ctx.fromClauseSimple())
            val where = ctx.whereClause()?.let { visitExpr(it.arg) }
            val returning = ctx.returningClause()?.let { visitReturningClause(it) }
            Statement.DML.Delete(id(), from, where, returning)
        }

        override fun visitInsertLegacy(ctx: GeneratedParser.InsertLegacyContext) = translate(ctx) {
            val target = Statement.DML.Target(id(), visitPathSimple(ctx.pathSimple()))
            val value = visitExpr(ctx.value)
            val index = ctx.pos?.let { visitExpr(it) }
            val onConflict = ctx.onConflictClause()?.let { visitOnConflictClause(it) }
            Statement.DML.InsertValue(id(), target, value, index, onConflict, null)
        }

        override fun visitInsert(ctx: GeneratedParser.InsertContext) = translate(ctx) {
            val asIdent = when (ctx.asIdent()) {
                null -> visitSymbolPrimitive(ctx.symbolPrimitive())
                else -> visitAsIdent(ctx.asIdent())
            }
            val target = visitPathSimple(ctx.)
            val value = visitExpr(ctx.value)
            val onConflict = ctx.onConflictClause()?.let { visitOnConflictClause(it) }
            Statement.DML.Insert(id(), target, value, onConflict)
        }

        /**
         * TODO move from experimental; pending: https://github.com/partiql/partiql-docs/issues/27
         * Based on the RFC, if alias exists the table must be hidden behind the alias, see:
         * https://github.com/partiql/partiql-docs/blob/main/RFCs/0011-partiql-insert.md#41-insert-parameters
         */
        override fun visitReplaceCommand(ctx: GeneratedParser.ReplaceCommandContext) = translate(ctx) {
            val target = when (ctx.asIdent()) {
                null -> visitSymbolPrimitive(ctx.symbolPrimitive())
                else -> visitAsIdent(ctx.asIdent())
            }
            val value = visitExpr(ctx.value)
            Statement.DML.Replace(id(), target, value)
        }

        /**
         * Based on https://github.com/partiql/partiql-docs/blob/main/RFCs/0011-partiql-insert.md
         * Based on the RFC, if alias exists the table must be hidden behind the alias, see:
         */
        override fun visitUpsertCommand(ctx: GeneratedParser.UpsertCommandContext) = translate(ctx) {
            val target = when (ctx.asIdent()) {
                null -> visitSymbolPrimitive(ctx.symbolPrimitive())
                else -> visitAsIdent(ctx.asIdent())
            }
            val value = visitExpr(ctx.value)
            Statement.DML.Upsert(id(), target, value)
        }

        /**
         * Based on https://github.com/partiql/partiql-docs/blob/main/RFCs/0011-partiql-insert.md
         */
        override fun visitInsertCommandReturning(ctx: GeneratedParser.InsertCommandReturningContext) = translate(ctx) {
            val target = Statement.DML.Target(id(), visitPathSimple(ctx.pathSimple()))
            val value = visitExpr(ctx.value)
            val index = visitOrNull<Expr>(ctx.pos)
            val onConflict = ctx.onConflictClause()?.let { visitOnConflictClause(it) }
            val returning = ctx.returningClause()?.let { visitReturningClause(it) }
            Statement.DML.InsertValue(id(), target, value, index, onConflict, returning)
        }

        override fun visitReturningClause(ctx: GeneratedParser.ReturningClauseContext) = translate(ctx) {
            val columns = visitOrEmpty<Returning.Column>(ctx.returningColumn())
            Returning(id(), columns)
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
                null -> Returning.Column.Value.Expression(id(), visitExpr(ctx.expr()))
                else -> Returning.Column.Value.Wildcard(id())
            }
            Returning.Column(id(), status, age, value)
        }

        private fun visitOnConflictClause(ctx: GeneratedParser.OnConflictClauseContext) = ctx.accept(this) as OnConflict

        override fun visitOnConflict(ctx: GeneratedParser.OnConflictContext) = translate(ctx) {
            val target = ctx.conflictTarget()?.let { visitConflictTarget(it) }
            val action = visitConflictAction(ctx.conflictAction())
            OnConflict(id(), target, action)
        }

        override fun visitOnConflictLegacy(ctx: GeneratedParser.OnConflictLegacyContext) = translate(ctx) {
            val target = OnConflict.Target.Condition(id(), visitExpr(ctx.expr()))
            val action = OnConflict.Action.DoNothing(id())
            OnConflict(id(), target, action)
        }

        override fun visitConflictTarget(ctx: GeneratedParser.ConflictTargetContext) = translate(ctx) {
            if (ctx.constraintName() != null) {
                OnConflict.Target.Constraint(id(), symbol(ctx.constraintName().symbolPrimitive()))
            } else {
                val symbols = ctx.symbolPrimitive().map { symbol(it) }
                OnConflict.Target.Symbols(id(), symbols)
            }
        }

        override fun visitConflictAction(ctx: GeneratedParser.ConflictActionContext) = when {
            ctx.NOTHING() != null -> translate(ctx) { OnConflict.Action.DoNothing(id()) }
            ctx.REPLACE() != null -> visitDoReplace(ctx.doReplace())
            ctx.UPDATE() != null -> visitDoUpdate(ctx.doUpdate())
            else -> throw error(ctx, "ON CONFLICT only supports `DO REPLACE` and `DO NOTHING` actions at the moment.")
        }

        override fun visitDoReplace(ctx: GeneratedParser.DoReplaceContext) = translate(ctx) {
            val value = when {
                ctx.EXCLUDED() != null -> OnConflict.Value.EXCLUDED
                else -> throw error(ctx, "DO REPLACE doesn't support values other than `EXCLUDED` yet.")
            }
            OnConflict.Action.DoReplace(id(), value)
        }

        override fun visitDoUpdate(ctx: GeneratedParser.DoUpdateContext) = translate(ctx) {
            val value = when {
                ctx.EXCLUDED() != null -> OnConflict.Value.EXCLUDED
                else -> throw error(ctx, "DO UPDATE doesn't support values other than `EXCLUDED` yet.")
            }
            OnConflict.Action.DoUpdate(id(), value)
        }

        override fun visitPathSimple(ctx: GeneratedParser.PathSimpleContext) = translate(ctx) {
            val root = visitSymbolPrimitive(ctx.symbolPrimitive())
            val steps = visitOrEmpty<Path.Step>(ctx.pathSimpleSteps())
            Path(id(), root, steps)
        }

        override fun visitPathSimpleLiteral(ctx: GeneratedParser.PathSimpleLiteralContext) = translate(ctx) {
            val value = visit(ctx.LITERAL_INTEGER()) as Expr
            Path.Step.Literal(id(), value)
        }

        override fun visitPathSimpleSymbol(ctx: GeneratedParser.PathSimpleSymbolContext) = translate(ctx) {
            val identifier = visitSymbolPrimitive(ctx.symbolPrimitive())
            Path.Step.Symbol(id(), identifier)
        }

        override fun visitPathSimpleDotSymbol(ctx: GeneratedParser.PathSimpleDotSymbolContext) = translate(ctx) {
            val identifier = visitSymbolPrimitive(ctx.symbolPrimitive())
            Path.Step.Symbol(id(), identifier)
        }

        /**
         * Current grammar models a SET with no UPDATE target as valid DML command.
         * We put a blank target, because we'll have to unpack this.
         */
        override fun visitSetCommand(ctx: GeneratedParser.SetCommandContext) = translate(ctx) {
            val target = Statement.DML.Target(id(), Expr.Missing(id()))
            val assignments = visitOrEmpty<Statement.DML.Update.Assignment>(ctx.setAssignment())
            Statement.DML.Update(id(), target, assignments)
        }

        override fun visitSetAssignment(ctx: GeneratedParser.SetAssignmentContext) = translate(ctx) {
            val target = visitPathSimple(ctx.pathSimple())
            val value = visitExpr(ctx.expr())
            Statement.DML.Update.Assignment(id(), target, value)
        }

        /**
         *
         * DATA QUERY LANGUAGE (DQL)
         *
         */

        override fun visitDql(ctx: GeneratedParser.DqlContext) = translate(ctx) {
            val expr = visitAs<Expr>(ctx.expr())
            Statement.Query(id(), expr)
        }

        override fun visitQueryBase(ctx: GeneratedParser.QueryBaseContext): AstNode = visit(ctx.exprSelect())

        override fun visitSfwQuery(ctx: GeneratedParser.SfwQueryContext) = translate(ctx) {
            val select = visit(ctx.select) as Select
            val from = visitFromClause(ctx.from)
            val let = visitOrNull<Let>(ctx.let)
            val where = visitOrNull<Expr>(ctx.where)
            val groupBy = ctx.group?.let { visitGroupClause(it) }
            val having = visitOrNull<Expr>(ctx.having?.arg)
            val orderBy = ctx.order?.let { visitOrderByClause(it) }
            val limit = visitOrNull<Expr>(ctx.limit?.arg)
            val offset = visitOrNull<Expr>(ctx.offset?.arg)
            Expr.SFW(id(), select, from, let, where, groupBy, having, orderBy, limit, offset)
        }

        /**
         *
         * SELECT & PROJECTIONS
         *
         */

        override fun visitSelectAll(ctx: GeneratedParser.SelectAllContext) = translate(ctx) {
            val quantifier = convertSetQuantifier(ctx.setQuantifierStrategy())
            Select.Star(id(), quantifier)
        }

        override fun visitSelectItems(ctx: GeneratedParser.SelectItemsContext) = translate(ctx) {
            val quantifier = convertSetQuantifier(ctx.setQuantifierStrategy())
            val items = visitOrEmpty<Select.Project.Item>(ctx.projectionItems().projectionItem())
            Select.Project(id(), quantifier, items)
        }

        override fun visitSelectPivot(ctx: GeneratedParser.SelectPivotContext) = translate(ctx) {
            val value = visitExpr(ctx.at)
            val key = visitExpr(ctx.pivot)
            Select.Pivot(id(), value, key)
        }

        override fun visitSelectValue(ctx: GeneratedParser.SelectValueContext) = translate(ctx) {
            val quantifier = convertSetQuantifier(ctx.setQuantifierStrategy())
            val constructor = visitExpr(ctx.expr())
            Select.Value(id(), quantifier, constructor)
        }

        override fun visitProjectionItem(ctx: GeneratedParser.ProjectionItemContext) = translate(ctx) {
            val expr = visitExpr(ctx.expr())
            val alias = ctx.symbolPrimitive()?.let { symbol(it) }
            if (expr is Expr.Path) {
                convertPathToProjectionItem(ctx, expr, alias)
            } else {
                Select.Project.Item.Var(id(), expr, alias)
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
            Let(id(), bindings)
        }

        override fun visitLetBinding(ctx: GeneratedParser.LetBindingContext) = translate(ctx) {
            val expr = visitAs<Expr>(ctx.expr())
            val alias = symbol(ctx.symbolPrimitive())
            Let.Binding(id(), expr, alias)
        }

        /**
         *
         * ORDER BY CLAUSE
         *
         */

        override fun visitOrderByClause(ctx: GeneratedParser.OrderByClauseContext) = translate(ctx) {
            val sorts = visitOrEmpty<OrderBy.Sort>(ctx.orderSortSpec())
            OrderBy(id(), sorts)
        }

        override fun visitOrderSortSpec(ctx: GeneratedParser.OrderSortSpecContext) = translate(ctx) {
            val expr = visitAs<Expr>(ctx.expr())
            val dir = when {
                ctx.dir == null -> {
                    // inserting default ASC value
                    OrderBy.Sort.Dir.ASC
                }
                ctx.dir.type == GeneratedParser.ASC -> OrderBy.Sort.Dir.ASC
                ctx.dir.type == GeneratedParser.DESC -> OrderBy.Sort.Dir.DESC
                else -> throw error(ctx.dir, "Invalid ORDER BY direction; expected ASC or DESC")
            }
            val nulls = when {
                ctx.nulls == null -> {
                    // inserting default null sort
                    if (dir == OrderBy.Sort.Dir.DESC) {
                        OrderBy.Sort.Nulls.FIRST
                    } else {
                        OrderBy.Sort.Nulls.LAST
                    }
                }
                ctx.nulls.type == GeneratedParser.FIRST -> OrderBy.Sort.Nulls.FIRST
                ctx.nulls.type == GeneratedParser.LAST -> OrderBy.Sort.Nulls.LAST
                else -> throw error(ctx.nulls, "Invalid ORDER null ordering; expected FIRST or LAST")
            }
            OrderBy.Sort(id(), expr, dir, nulls)
        }

        /**
         *
         * GROUP BY CLAUSE
         *
         */

        override fun visitGroupClause(ctx: GeneratedParser.GroupClauseContext) = translate(ctx) {
            val strategy = if (ctx.PARTIAL() != null) GroupBy.Strategy.PARTIAL else GroupBy.Strategy.FULL
            val keys = visitOrEmpty<GroupBy.Key>(ctx.groupKey())
            val alias = ctx.groupAlias()?.let { symbol(ctx.groupAlias().symbolPrimitive()) }
            GroupBy(id(), strategy, keys, alias)
        }

        /**
         * Returns a GROUP BY key
         * TODO: Support ordinal case. Also, the conditional defining the exception is odd. 1 + 1 is allowed, but 2 is not.
         *  This is to match the functionality of SqlParser, but this should likely be adjusted.
         */
        override fun visitGroupKey(ctx: GeneratedParser.GroupKeyContext) = translate(ctx) {
            val expr = visitAs<Expr>(ctx.key)
            val alias = ctx.symbolPrimitive()?.let { symbol(it) }
            GroupBy.Key(id(), expr, alias)
        }

        /**
         *
         * BAG OPERATIONS
         *
         */

        override fun visitIntersect(ctx: GeneratedParser.IntersectContext) = translate(ctx) {
            val quantifier = if (ctx.ALL() != null) SetQuantifier.ALL else SetQuantifier.DISTINCT
            val outer = ctx.OUTER() != null
            val lhs = visitAs<Expr>(ctx.lhs)
            val rhs = visitAs<Expr>(ctx.rhs)
            Expr.Set(id(), Expr.Set.Op.INTERSECT, quantifier, outer, lhs, rhs)
        }

        override fun visitExcept(ctx: GeneratedParser.ExceptContext) = translate(ctx) {
            val quantifier = if (ctx.ALL() != null) SetQuantifier.ALL else SetQuantifier.DISTINCT
            val outer = ctx.OUTER() != null
            val lhs = visitAs<Expr>(ctx.lhs)
            val rhs = visitAs<Expr>(ctx.rhs)
            Expr.Set(id(), Expr.Set.Op.EXCEPT, quantifier, outer, lhs, rhs)
        }

        override fun visitUnion(ctx: GeneratedParser.UnionContext) = translate(ctx) {
            val quantifier = if (ctx.ALL() != null) SetQuantifier.ALL else SetQuantifier.DISTINCT
            val outer = ctx.OUTER() != null
            val lhs = visitAs<Expr>(ctx.lhs)
            val rhs = visitAs<Expr>(ctx.rhs)
            Expr.Set(id(), Expr.Set.Op.UNION, quantifier, outer, lhs, rhs)
        }

        /**
         *
         * GRAPH PATTERN MANIPULATION LANGUAGE (GPML)
         *
         */

        override fun visitGpmlPattern(ctx: GeneratedParser.GpmlPatternContext) = translate(ctx) {
            val pattern = visitMatchPattern(ctx.matchPattern())
            val selector = visitOrNull<GraphMatch.Selector>(ctx.matchSelector())
            GraphMatch(id(), listOf(pattern), selector)
        }

        override fun visitGpmlPatternList(ctx: GeneratedParser.GpmlPatternListContext) = translate(ctx) {
            val patterns = ctx.matchPattern().map { pattern -> visitMatchPattern(pattern) }
            val selector = visitOrNull<GraphMatch.Selector>(ctx.matchSelector())
            GraphMatch(id(), patterns, selector)
        }

        override fun visitMatchPattern(ctx: GeneratedParser.MatchPatternContext) = translate(ctx) {
            val parts = visitOrEmpty<GraphMatch.Pattern.Part>(ctx.graphPart())
            val restrictor = ctx.restrictor?.let {
                when (ctx.restrictor.text.toLowerCase()) {
                    "trail" -> GraphMatch.Restrictor.TRAIL
                    "acyclic" -> GraphMatch.Restrictor.ACYCLIC
                    "simple" -> GraphMatch.Restrictor.SIMPLE
                    else -> throw error(ctx.restrictor, "Unrecognized pattern restrictor")
                }
            }
            val variable = visitOrNull<Expr.Identifier>(ctx.variable)?.name
            GraphMatch.Pattern(id(), restrictor, null, variable, null, parts)
        }

        override fun visitPatternPathVariable(ctx: GeneratedParser.PatternPathVariableContext) =
            visitSymbolPrimitive(ctx.symbolPrimitive())

        override fun visitSelectorBasic(ctx: GeneratedParser.SelectorBasicContext) = translate(ctx) {
            when (ctx.mod.type) {
                GeneratedParser.ANY -> GraphMatch.Selector.AnyShortest(id())
                GeneratedParser.ALL -> GraphMatch.Selector.AllShortest(id())
                else -> throw error(ctx, "Unsupported match selector.")
            }
        }

        override fun visitSelectorAny(ctx: GeneratedParser.SelectorAnyContext) = translate(ctx) {
            when (ctx.k) {
                null -> GraphMatch.Selector.Any(id())
                else -> GraphMatch.Selector.AnyK(id(), ctx.k.text.toLong())
            }
        }

        override fun visitSelectorShortest(ctx: GeneratedParser.SelectorShortestContext) = translate(ctx) {
            val k = ctx.k.text.toLong()
            when (ctx.GROUP()) {
                null -> GraphMatch.Selector.ShortestK(id(), k)
                else -> GraphMatch.Selector.ShortestKGroup(id(), k)
            }
        }

        override fun visitPatternPartLabel(ctx: GeneratedParser.PatternPartLabelContext) =
            visitSymbolPrimitive(ctx.symbolPrimitive())

        override fun visitPattern(ctx: GeneratedParser.PatternContext) = translate(ctx) {
            val restrictor = visitRestrictor(ctx.restrictor)
            val variable = visitOrNull<Expr.Identifier>(ctx.variable)?.name
            val prefilter = ctx.where?.let { visitExpr(it.expr()) }
            val quantifier = ctx.quantifier?.let { visitPatternQuantifier(it) }
            val parts = visitOrEmpty<GraphMatch.Pattern.Part>(ctx.graphPart())
            GraphMatch.Pattern(id(), restrictor, prefilter, variable, quantifier, parts)
        }

        override fun visitEdgeAbbreviated(ctx: GeneratedParser.EdgeAbbreviatedContext) = translate(ctx) {
            val direction = visitEdge(ctx.edgeAbbrev())
            val quantifier = visitOrNull<GraphMatch.Quantifier>(ctx.quantifier)
            GraphMatch.Pattern.Part.Edge(id(), direction, quantifier, null, null, emptyList())
        }

        override fun visitEdgeWithSpec(ctx: GeneratedParser.EdgeWithSpecContext) = translate(ctx) {
            val quantifier = visitOrNull<GraphMatch.Quantifier>(ctx.quantifier)
            val edge = visitOrNull<GraphMatch.Pattern.Part.Edge>(ctx.edgeWSpec())
            edge!!.copy(quantifier = quantifier)
        }

        override fun visitEdgeSpec(ctx: GeneratedParser.EdgeSpecContext) = translate(ctx) {
            val placeholderDirection = GraphMatch.Direction.RIGHT
            val variable = visitOrNull<Expr.Identifier>(ctx.symbolPrimitive())?.name
            val prefilter = ctx.whereClause()?.let { visitExpr(it.expr()) }
            val label = visitOrNull<Expr.Identifier>(ctx.patternPartLabel())?.name
            GraphMatch.Pattern.Part.Edge(id(), placeholderDirection, null, prefilter, variable, listOfNotNull(label))
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
                return translate(ctx) { GraphMatch.Pattern.Part.Pattern(id(), part) }
            }
            return part as GraphMatch.Pattern.Part
        }

        override fun visitPatternQuantifier(ctx: GeneratedParser.PatternQuantifierContext) = translate(ctx) {
            when {
                ctx.quant == null -> GraphMatch.Quantifier(id(), ctx.lower.text.toLong(), ctx.upper?.text?.toLong())
                ctx.quant.type == GeneratedParser.PLUS -> GraphMatch.Quantifier(id(), 1L, null)
                ctx.quant.type == GeneratedParser.ASTERISK -> GraphMatch.Quantifier(id(), 0L, null)
                else -> throw error(ctx, "Unsupported quantifier")
            }
        }

        override fun visitNode(ctx: GeneratedParser.NodeContext) = translate(ctx) {
            val variable = visitOrNull<Expr.Identifier>(ctx.symbolPrimitive())?.name
            val prefilter = ctx.whereClause()?.let { visitExpr(it.expr()) }
            val label = visitOrNull<Expr.Identifier>(ctx.patternPartLabel())?.name
            GraphMatch.Pattern.Part.Node(id(), prefilter, variable, listOfNotNull(label))
        }

        private fun visitRestrictor(ctx: GeneratedParser.PatternRestrictorContext?): GraphMatch.Restrictor? {
            if (ctx == null) return null
            return when (ctx.restrictor.text.toLowerCase()) {
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
            val asAlias = ctx.asIdent()?.let { symbol(it.symbolPrimitive()) }
            val atAlias = ctx.atIdent()?.let { symbol(it.symbolPrimitive()) }
            val byAlias = ctx.byIdent()?.let { symbol(it.symbolPrimitive()) }
            From.Collection(id(), expr, false, asAlias, atAlias, byAlias)
        }

        override fun visitTableBaseRefMatch(ctx: GeneratedParser.TableBaseRefMatchContext) = translate(ctx) {
            val expr = visitAs<Expr>(ctx.source)
            val asAlias = ctx.asIdent()?.let { symbol(it.symbolPrimitive()) }
            val atAlias = ctx.atIdent()?.let { symbol(it.symbolPrimitive()) }
            val byAlias = ctx.byIdent()?.let { symbol(it.symbolPrimitive()) }
            From.Collection(id(), expr, false, asAlias, atAlias, byAlias)
        }

        override fun visitFromClauseSimpleExplicit(ctx: GeneratedParser.FromClauseSimpleExplicitContext) =
            translate(ctx) {
                val expr = visitPathSimple(ctx.pathSimple()) as Expr
                val asAlias = ctx.asIdent()?.let { symbol(it.symbolPrimitive()) }
                val atAlias = ctx.atIdent()?.let { symbol(it.symbolPrimitive()) }
                val byAlias = ctx.byIdent()?.let { symbol(it.symbolPrimitive()) }
                From.Collection(id(), expr, false, asAlias, atAlias, byAlias)
            }

        override fun visitTableUnpivot(ctx: GeneratedParser.TableUnpivotContext) = translate(ctx) {
            val expr = visitAs<Expr>(ctx.expr())
            val asAlias = ctx.asIdent()?.let { symbol(it.symbolPrimitive()) }
            val atAlias = ctx.atIdent()?.let { symbol(it.symbolPrimitive()) }
            val byAlias = ctx.byIdent()?.let { symbol(it.symbolPrimitive()) }
            From.Collection(id(), expr, true, asAlias, atAlias, byAlias)
        }

        override fun visitTableCrossJoin(ctx: GeneratedParser.TableCrossJoinContext) = translate(ctx) {
            val type = convertJoinType(ctx.joinType())
            val lhs = visitAs<From>(ctx.lhs)
            val rhs = visitAs<From>(ctx.rhs)
            From.Join(id(), type, null, lhs, rhs)
        }

        private fun convertJoinType(ctx: GeneratedParser.JoinTypeContext?): From.Join.Type {
            if (ctx == null) return From.Join.Type.INNER
            return when (ctx.mod.type) {
                GeneratedParser.LEFT -> From.Join.Type.LEFT
                GeneratedParser.RIGHT -> From.Join.Type.RIGHT
                GeneratedParser.INNER -> From.Join.Type.INNER
                GeneratedParser.FULL -> From.Join.Type.FULL
                GeneratedParser.OUTER -> From.Join.Type.FULL
                else -> From.Join.Type.INNER
            }
        }

        override fun visitTableQualifiedJoin(ctx: GeneratedParser.TableQualifiedJoinContext) = translate(ctx) {
            val type = convertJoinType(ctx.joinType())
            val condition = ctx.joinSpec()?.let { visitExpr(it.expr()) }
            val lhs = visitAs<From>(ctx.lhs)
            val rhs = visitAs<From>(ctx.rhs)
            From.Join(id(), type, condition, lhs, rhs)
        }

        override fun visitTableBaseRefSymbol(ctx: GeneratedParser.TableBaseRefSymbolContext) = translate(ctx) {
            val expr = visitAs<Expr>(ctx.source)
            val asAlias = symbol(ctx.symbolPrimitive())
            From.Collection(id(), expr, false, asAlias, null, null)
        }

        override fun visitFromClauseSimpleImplicit(ctx: GeneratedParser.FromClauseSimpleImplicitContext) =
            translate(ctx) {
                val expr = visitPathSimple(ctx.pathSimple()) as Expr
                val asAlias = symbol(ctx.symbolPrimitive())
                From.Collection(id(), expr, false, asAlias, null, null)
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
            Expr.Unary(id(), Expr.Unary.Op.NOT, expr)
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
            Expr.Unary(id(), convertUnaryOp(ctx.sign), expr)
        }

        private fun convertBinaryExpr(lhs: ParserRuleContext, rhs: ParserRuleContext, op: Expr.Binary.Op): Expr {
            val l = visit(lhs) as Expr
            val r = visit(rhs) as Expr
            return Expr.Binary(id(), op, l, r)
        }

        private fun convertBinaryOp(token: Token) = when (token.type) {
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

        override fun visitPredicateIn(ctx: GeneratedParser.PredicateInContext) = translate(ctx) {
            val lhs = visitAs<Expr>(ctx.lhs)
            val rhs = visitAs<Expr>(ctx.rhs).let {
                // Wrap rhs in an array unless it's a query or already a collection
                if (it is Expr.SFW || it is Expr.Collection) {
                    it
                } else {
                    Expr.Collection(id(), Expr.Collection.Type.ARRAY, listOf(it))
                }
            }
            predicate(ctx.NOT()) { Expr.InCollection(id(), lhs, rhs) }
        }

        override fun visitPredicateIs(ctx: GeneratedParser.PredicateIsContext) = translate(ctx) {
            val value = visitAs<Expr>(ctx.lhs)
            val type = visitAs<Type>(ctx.type())
            predicate(ctx.NOT()) { Expr.IsType(id(), value, type) }
        }

        override fun visitPredicateBetween(ctx: GeneratedParser.PredicateBetweenContext) = translate(ctx) {
            val value = visitAs<Expr>(ctx.lhs)
            val lower = visitAs<Expr>(ctx.lower)
            val upper = visitAs<Expr>(ctx.upper)
            predicate(ctx.NOT()) { Expr.Between(id(), value, lower, upper) }
        }

        override fun visitPredicateLike(ctx: GeneratedParser.PredicateLikeContext) = translate(ctx) {
            val value = visitAs<Expr>(ctx.lhs)
            val pattern = visitAs<Expr>(ctx.rhs)
            val escape = visitOrNull<Expr>(ctx.escape)
            predicate(ctx.NOT()) { Expr.Like(id(), value, pattern, escape) }
        }

        private inline fun predicate(not: TerminalNode?, predicate: () -> Expr): Expr {
            val p = predicate()
            return if (not == null) p else Expr.Unary(id(), Expr.Unary.Op.NOT, p)
        }

        /**
         *
         * PRIMARY EXPRESSIONS
         *
         */

        override fun visitExprTermWrappedQuery(ctx: GeneratedParser.ExprTermWrappedQueryContext) =
            visit(ctx.expr())

        override fun visitVarRefExpr(ctx: GeneratedParser.VarRefExprContext) = translate(ctx) {
            val case = when (ctx.ident.type) {
                GeneratedParser.IDENTIFIER -> Case.INSENSITIVE
                else -> Case.SENSITIVE
            }
            val scope = when (ctx.qualifier) {
                null -> Expr.Identifier.Scope.UNQUALIFIED
                else -> Expr.Identifier.Scope.LOCALS_FIRST
            }
            Expr.Identifier(id(), ctx.ident.getStringValue(), case, scope)
        }

        override fun visitParameter(ctx: GeneratedParser.ParameterContext) = translate(ctx) {
            val index = parameters[ctx.QUESTION_MARK().symbol.tokenIndex]
                ?: throw PartiQLParserException("Unable to find index of parameter.")
            Expr.Parameter(id(), index)
        }

        override fun visitSequenceConstructor(ctx: GeneratedParser.SequenceConstructorContext) = translate(ctx) {
            val expressions = visitOrEmpty<Expr>(ctx.expr())
            val type = when (ctx.datatype.type) {
                GeneratedParser.LIST -> Expr.Collection.Type.LIST
                GeneratedParser.SEXP -> Expr.Collection.Type.SEXP
                else -> throw error(ctx.datatype, "Invalid sequence type")
            }
            Expr.Collection(id(), type, expressions)
        }

        override fun visitExprPrimaryPath(ctx: GeneratedParser.ExprPrimaryPathContext) = translate(ctx) {
            val base = visitAs<Expr>(ctx.exprPrimary())
            val steps = ctx.pathStep().map { visit(it) as Expr.Path.Step }
            Expr.Path(id(), base, steps)
        }

        override fun visitPathStepIndexExpr(ctx: GeneratedParser.PathStepIndexExprContext) = translate(ctx) {
            val key = visitAs<Expr>(ctx.key)
            val case = Case.SENSITIVE
            Expr.Path.Step.Index(id(), key, case)
        }

        override fun visitPathStepDotExpr(ctx: GeneratedParser.PathStepDotExprContext) = translate(ctx) {
            val (symbol, case) = symbolCased(ctx.symbolPrimitive())
            Expr.Path.Step.Index(id(), Expr.Lit(id(), ionString(symbol)), case)
        }

        override fun visitPathStepIndexAll(ctx: GeneratedParser.PathStepIndexAllContext) = translate(ctx) {
            Expr.Path.Step.Wildcard(id())
        }

        override fun visitPathStepDotAll(ctx: GeneratedParser.PathStepDotAllContext) = translate(ctx) {
            Expr.Path.Step.Unpivot(id())
        }

        override fun visitValues(ctx: GeneratedParser.ValuesContext) = translate(ctx) {
            val rows = visitOrEmpty<Expr.Collection>(ctx.valueRow())
            Expr.Collection(id(), Expr.Collection.Type.BAG, rows)
        }

        override fun visitValueRow(ctx: GeneratedParser.ValueRowContext) = translate(ctx) {
            val expressions = visitOrEmpty<Expr>(ctx.expr())
            Expr.Collection(id(), Expr.Collection.Type.LIST, expressions)
        }

        override fun visitValueList(ctx: GeneratedParser.ValueListContext) = translate(ctx) {
            val expressions = visitOrEmpty<Expr>(ctx.expr())
            Expr.Collection(id(), Expr.Collection.Type.LIST, expressions)
        }

        override fun visitExprGraphMatchMany(ctx: GeneratedParser.ExprGraphMatchManyContext) = translate(ctx) {
            val graph = visit(ctx.exprPrimary()) as Expr
            val pattern = visitGpmlPatternList(ctx.gpmlPatternList())
            Expr.Match(id(), graph, pattern)
        }

        override fun visitExprGraphMatchOne(ctx: GeneratedParser.ExprGraphMatchOneContext) = translate(ctx) {
            val graph = visit(ctx.exprPrimary()) as Expr
            val pattern = visitGpmlPattern(ctx.gpmlPattern())
            Expr.Match(id(), graph, pattern)
        }

        /**
         *
         * FUNCTIONS
         *
         */

        override fun visitNullIf(ctx: GeneratedParser.NullIfContext) = translate(ctx) {
            val expr0 = visitExpr(ctx.expr(0))
            val expr1 = visitExpr(ctx.expr(1))
            Expr.NullIf(id(), expr0, expr1)
        }

        override fun visitCoalesce(ctx: GeneratedParser.CoalesceContext) = translate(ctx) {
            val expressions = visitOrEmpty<Expr>(ctx.expr())
            Expr.Coalesce(id(), expressions)
        }

        override fun visitCaseExpr(ctx: GeneratedParser.CaseExprContext) = translate(ctx) {
            val expr = ctx.case_?.let { visitExpr(it) }
            val branches = ctx.whens.indices.map { i ->
                // consider adding locations
                val w = visitExpr(ctx.whens[i])
                val t = visitExpr(ctx.thens[i])
                Expr.Switch.Branch(id(), w, t)
            }
            val default = ctx.else_?.let { visitExpr(it) }
            Expr.Switch(id(), expr, branches, default)
        }

        override fun visitCast(ctx: GeneratedParser.CastContext) = translate(ctx) {
            val expr = visitExpr(ctx.expr())
            val type = visitAs<Type>(ctx.type())
            Expr.Cast(id(), expr, type)
        }

        override fun visitCanCast(ctx: GeneratedParser.CanCastContext) = translate(ctx) {
            val expr = visitExpr(ctx.expr())
            val type = visitAs<Type>(ctx.type())
            Expr.CanCast(id(), expr, type)
        }

        override fun visitCanLosslessCast(ctx: GeneratedParser.CanLosslessCastContext) = translate(ctx) {
            val expr = visitExpr(ctx.expr())
            val type = visitAs<Type>(ctx.type())
            Expr.CanLosslessCast(id(), expr, type)
        }

        override fun visitFunctionCallIdent(ctx: GeneratedParser.FunctionCallIdentContext) = translate(ctx) {
            val function = ctx.name.getString().toLowerCase()
            val args = visitOrEmpty<Expr>(ctx.expr())
            Expr.Call(id(), function, args)
        }

        override fun visitFunctionCallReserved(ctx: GeneratedParser.FunctionCallReservedContext) = translate(ctx) {
            val function = ctx.name.text.toLowerCase()
            val args = visitOrEmpty<Expr>(ctx.expr())
            Expr.Call(id(), function, args)
        }

        override fun visitDateFunction(ctx: GeneratedParser.DateFunctionContext) = translate(ctx) {
            try {
                DateTimePart.valueOf(ctx.dt.text.toUpperCase())
            } catch (ex: IllegalArgumentException) {
                throw error(ctx.dt, "Expected one of: ${DateTimePart.values().joinToString()}", ex)
            }
            val function = ctx.func.text.toLowerCase()
            val date = Expr.Lit(id(), ionSymbol(ctx.dt.text))
            val args = visitOrEmpty<Expr>(ctx.expr())
            Expr.Call(id(), function, listOf(date) + args)
        }

        override fun visitSubstring(ctx: GeneratedParser.SubstringContext) = translate(ctx) {
            val function = ctx.SUBSTRING().text.toLowerCase()
            val args = visitOrEmpty<Expr>(ctx.expr())
            Expr.Call(id(), function, args)
        }

        override fun visitPosition(ctx: GeneratedParser.PositionContext) = translate(ctx) {
            val function = ctx.POSITION().text.toLowerCase()
            val args = visitOrEmpty<Expr>(ctx.expr())
            Expr.Call(id(), function, args)
        }

        override fun visitOverlay(ctx: GeneratedParser.OverlayContext) = translate(ctx) {
            val function = ctx.OVERLAY().text.toLowerCase()
            val args = visitOrEmpty<Expr>(ctx.expr())
            Expr.Call(id(), function, args)
        }

        override fun visitCountAll(ctx: GeneratedParser.CountAllContext) = translate(ctx) {
            val function = ctx.func.text.toLowerCase()
            val args = listOf(Expr.Lit(id(), ionInt(1)))
            Expr.Agg(id(), function, args, SetQuantifier.ALL)
        }

        override fun visitExtract(ctx: GeneratedParser.ExtractContext) = translate(ctx) {
            try {
                DateTimePart.valueOf(ctx.IDENTIFIER().text.toUpperCase())
            } catch (ex: IllegalArgumentException) {
                throw error(ctx.IDENTIFIER().symbol, "Expected one of: ${DateTimePart.values().joinToString()}", ex)
            }
            val function = ctx.EXTRACT().text.toLowerCase()
            val date = Expr.Lit(id(), ionSymbol(ctx.IDENTIFIER().text))
            val time = visitAs<Expr>(ctx.rhs)
            Expr.Call(id(), function, listOf(date, time))
        }

        override fun visitTrimFunction(ctx: GeneratedParser.TrimFunctionContext) = translate(ctx) {
            val mod = when (ctx.mod) {
                null -> {
                    // insert TRIM BOTH
                    "both"
                }
                else -> {
                    val m = ctx.mod.text.toLowerCase()
                    if (m !in setOf("both", "leading", "trailing")) throw error(
                        ctx.mod,
                        "Invalid trim function modifier"
                    )
                    m
                }
            }
            val (function, args) = when (ctx.sub) {
                null -> "trim_whitespace_$mod" to listOf(visitExpr(ctx.target))
                else -> "trim_chars_$mod" to listOf(visitExpr(ctx.sub), visitExpr(ctx.target))
            }
            Expr.Call(id(), function, args)
        }

        override fun visitAggregateBase(ctx: GeneratedParser.AggregateBaseContext) = translate(ctx) {
            val function = ctx.func.text.toLowerCase()
            val args = listOf(visitExpr(ctx.expr()))
            val quantifier = convertSetQuantifier(ctx.setQuantifierStrategy())
            Expr.Agg(id(), function, args, quantifier)
        }

        /**
         * Window Functions
         */

        override fun visitLagLeadFunction(ctx: GeneratedParser.LagLeadFunctionContext) = translate(ctx) {
            val function = ctx.func.text.toLowerCase()
            val args = visitOrEmpty<Expr>(ctx.expr())
            val over = visitOver(ctx.over())
            if (over.sorts.isEmpty()) {
                // LAG and LEAD will require a Window ORDER BY
                throw error(ctx, "Window ORDER BY is required")
            }
            Expr.Window(id(), function, over, args)
        }

        override fun visitOver(ctx: GeneratedParser.OverContext) = translate(ctx) {
            val partitions = visitOrEmpty<Expr>(ctx.windowPartitionList().expr())
            val sorts = visitOrEmpty<OrderBy.Sort>(ctx.windowSortSpecList().orderSortSpec())
            Over(id(), partitions, sorts)
        }

        /**
         *
         * LITERALS
         *
         */

        override fun visitBag(ctx: GeneratedParser.BagContext) = translate(ctx) {
            val expressions = visitOrEmpty<Expr>(ctx.expr())
            Expr.Collection(id(), Expr.Collection.Type.BAG, expressions)
        }

        override fun visitLiteralDecimal(ctx: GeneratedParser.LiteralDecimalContext) = translate(ctx) {
            val decimal = try {
                val v = ctx.LITERAL_DECIMAL().text.trim()
                val d = BigDecimal(v, MathContext(38, RoundingMode.HALF_EVEN))
                ionDecimal(Decimal.valueOf(d))
            } catch (e: NumberFormatException) {
                throw error(ctx, "Invalid decimal literal", e)
            }
            Expr.Lit(id(), decimal)
        }

        override fun visitArray(ctx: GeneratedParser.ArrayContext) = translate(ctx) {
            val expressions = visitOrEmpty<Expr>(ctx.expr())
            Expr.Collection(id(), Expr.Collection.Type.ARRAY, expressions)
        }

        override fun visitLiteralNull(ctx: GeneratedParser.LiteralNullContext) = translate(ctx) {
            Expr.Lit(id(), ionNull())
        }

        override fun visitLiteralMissing(ctx: GeneratedParser.LiteralMissingContext) = translate(ctx) {
            Expr.Missing(id())
        }

        override fun visitLiteralTrue(ctx: GeneratedParser.LiteralTrueContext) = translate(ctx) {
            Expr.Lit(id(), ionBool(true))
        }

        override fun visitLiteralFalse(ctx: GeneratedParser.LiteralFalseContext) = translate(ctx) {
            Expr.Lit(id(), ionBool(false))
        }

        override fun visitLiteralIon(ctx: GeneratedParser.LiteralIonContext) = translate(ctx) {
            val value = try {
                loadSingleElement(ctx.ION_CLOSURE().getStringValue())
            } catch (e: IonElementException) {
                throw error(ctx, "Unable to parse Ion value.", e)
            }
            Expr.Lit(id(), value)
        }

        override fun visitLiteralString(ctx: GeneratedParser.LiteralStringContext) = translate(ctx) {
            val value = ionString(ctx.LITERAL_STRING().getStringValue())
            Expr.Lit(id(), value)
        }

        override fun visitLiteralInteger(ctx: GeneratedParser.LiteralIntegerContext) = translate(ctx) {
            val value = parseToIntElement(ctx.LITERAL_INTEGER().text)
            Expr.Lit(id(), value)
        }

        override fun visitLiteralDate(ctx: GeneratedParser.LiteralDateContext) = translate(ctx) {
            val pattern = ctx.LITERAL_STRING().symbol
            val dateString = ctx.LITERAL_STRING().getStringValue()
            if (DATE_PATTERN_REGEX.matches(dateString).not()) {
                throw error(pattern, "Expected DATE string to be of the format yyyy-MM-dd")
            }
            try {
                LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
                val (year, month, day) = dateString.split("-")
                Expr.Date(id(), year.toLong(), month.toLong(), day.toLong())
            } catch (e: DateTimeParseException) {
                throw error(pattern, e.localizedMessage, e)
            } catch (e: IndexOutOfBoundsException) {
                throw error(pattern, e.localizedMessage, e)
            }
        }

        override fun visitLiteralTime(ctx: GeneratedParser.LiteralTimeContext) = translate(ctx) {
            val (timeString, precision) = getTimeStringAndPrecision(ctx.LITERAL_STRING(), ctx.LITERAL_INTEGER())
            when (ctx.WITH()) {
                null -> convertLocalTime(timeString, false, precision, ctx.LITERAL_STRING())
                else -> convertOffsetTime(timeString, precision, ctx.LITERAL_STRING())
            }
        }

        override fun visitTuple(ctx: GeneratedParser.TupleContext) = translate(ctx) {
            val fields = ctx.pair().map {
                val k = visitExpr(it.lhs)
                val v = visitExpr(it.rhs)
                Expr.Tuple.Field(id(), k, v)
            }
            Expr.Tuple(id(), fields)
        }

        /**
         *
         * TYPES
         *
         */

        override fun visitTypeAtomic(ctx: GeneratedParser.TypeAtomicContext) = translate(ctx) {
            val type = when (ctx.datatype.type) {
                GeneratedParser.NULL -> "null"
                GeneratedParser.MISSING -> "missing"
                GeneratedParser.BOOL,
                GeneratedParser.BOOLEAN -> "bool"
                GeneratedParser.SMALLINT,
                GeneratedParser.INT2,
                GeneratedParser.INTEGER2 -> "int16"
                GeneratedParser.INT4,
                GeneratedParser.INTEGER4 -> "int32"
                GeneratedParser.BIGINT,
                GeneratedParser.INT8,
                GeneratedParser.INTEGER8 -> "int64"
                GeneratedParser.INT,
                GeneratedParser.INTEGER -> "int"
                GeneratedParser.FLOAT -> "float32"
                GeneratedParser.DOUBLE -> "float64"
                GeneratedParser.REAL,
                GeneratedParser.DECIMAL -> "decimal"
                GeneratedParser.TIMESTAMP -> "timestamp"
                GeneratedParser.CHAR,
                GeneratedParser.CHARACTER -> "character"
                GeneratedParser.NUMERIC -> "numeric"
                GeneratedParser.SYMBOL,
                GeneratedParser.STRING -> "string"
                GeneratedParser.BLOB,
                GeneratedParser.CLOB -> "blob"
                GeneratedParser.DATE -> "date"
                GeneratedParser.STRUCT,
                GeneratedParser.TUPLE -> "tuple"
                GeneratedParser.LIST -> "list"
                GeneratedParser.BAG -> "bag"
                GeneratedParser.SEXP -> "sexp"
                GeneratedParser.ANY -> "any"
                else -> throw error(ctx, "Unknown atomic type.")
            }
            Type(id(), type, emptyList())
        }

        override fun visitTypeVarChar(ctx: GeneratedParser.TypeVarCharContext) = translate(ctx) {
            val args = if (ctx.arg0 != null) {
                val arg0 = parseToIntElement(ctx.arg0.text)
                assertIntegerElement(ctx.arg0, arg0)
                listOf(arg0)
            } else {
                emptyList()
            }
            Type(id(), "varchar", args)
        }

        override fun visitTypeArgSingle(ctx: GeneratedParser.TypeArgSingleContext) = translate(ctx) {
            val arg0 = if (ctx.arg0 != null) parseToIntElement(ctx.arg0.text) else null
            assertIntegerElement(ctx.arg0, arg0)
            val type = when (ctx.datatype.type) {
                GeneratedParser.FLOAT -> "float32"
                GeneratedParser.CHAR, GeneratedParser.CHARACTER -> "character"
                GeneratedParser.VARCHAR -> "varchar"
                else -> throw error(ctx.datatype, "Invalid datatype")
            }
            Type(id(), type, listOfNotNull(arg0))
        }

        override fun visitTypeArgDouble(ctx: GeneratedParser.TypeArgDoubleContext) = translate(ctx) {
            val arg0 = if (ctx.arg0 != null) parseToIntElement(ctx.arg0.text) else null
            val arg1 = if (ctx.arg1 != null) parseToIntElement(ctx.arg1.text) else null
            assertIntegerElement(ctx.arg0, arg0)
            assertIntegerElement(ctx.arg1, arg1)
            val type = when (ctx.datatype.type) {
                GeneratedParser.DECIMAL,
                GeneratedParser.DEC -> "decimal"
                GeneratedParser.NUMERIC -> "numeric"
                else -> throw error(ctx.datatype, "Invalid datatype")
            }
            Type(id(), type, listOfNotNull(arg0, arg1))
        }

        override fun visitTypeTimeZone(ctx: GeneratedParser.TypeTimeZoneContext) = translate(ctx) {
            val precision = ctx.precision?.let {
                val p = ctx.precision.text.toInteger().toLong()
                if (p < 0 || 9 < p) throw error(ctx.precision, "Unsupported time precision")
                ionInt(p)
            }
            Type(id(), "time", listOfNotNull(precision))
        }

        override fun visitTypeCustom(ctx: GeneratedParser.TypeCustomContext) = translate(ctx) {
            val symbol = symbol(ctx.symbolPrimitive())
            Type(id(), symbol, emptyList())
        }

        private inline fun <reified T : AstNode> visitOrEmpty(ctx: List<ParserRuleContext>?): List<T> = when {
            ctx.isNullOrEmpty() -> emptyList()
            else -> ctx.map { visit(it) as T }
        }

        private inline fun <reified T : AstNode> visitNullableItems(ctx: List<ParserRuleContext>?): List<T?> = when {
            ctx.isNullOrEmpty() -> emptyList()
            else -> ctx.map { visitOrNull<T>(it) }
        }

        private inline fun <reified T : AstNode> visitOrNull(ctx: ParserRuleContext?): T? = ctx?.let { visit(it) as T }

        private inline fun <reified T : AstNode> visitAs(ctx: ParserRuleContext): T = visit(ctx) as T

        /**
         * Visiting a symbol to get a string, skip the wrapping, unwrapping, and location tracking.
         */
        private fun symbol(ctx: GeneratedParser.SymbolPrimitiveContext) = when (ctx.ident.type) {
            GeneratedParser.IDENTIFIER_QUOTED -> ctx.IDENTIFIER_QUOTED().getStringValue()
            GeneratedParser.IDENTIFIER -> ctx.IDENTIFIER().getStringValue()
            else -> throw PartiQLParserException("Invalid symbol reference.")
        }

        /**
         * Visiting a symbol to get a string with case
         */
        private fun symbolCased(ctx: GeneratedParser.SymbolPrimitiveContext): Pair<String, Case> =
            when (ctx.ident.type) {
                GeneratedParser.IDENTIFIER_QUOTED -> ctx.IDENTIFIER_QUOTED().getStringValue() to Case.SENSITIVE
                GeneratedParser.IDENTIFIER -> ctx.IDENTIFIER().getStringValue() to Case.INSENSITIVE
                else -> throw PartiQLParserException("Invalid symbol reference.")
            }

        /**
         * Visit a string expression and pull the value out
         */
        private fun stringOrErr(ctx: GeneratedParser.ExprContext): String =
            when (val expr = visitExpr(ctx)) {
                is Expr.Identifier -> expr.name
                is Expr.Lit -> expr.value.asAnyElement().stringValueOrNull ?: throw error(
                    ctx,
                    "Unable to pass the string value"
                )
                else -> throw error(ctx, "Unable to get value")
            }

        /**
         * Convert to Set Quantifier enum
         */
        private fun convertSetQuantifier(ctx: GeneratedParser.SetQuantifierStrategyContext?) = when {
            ctx == null -> {
                // DEFAULT SET QUANTIFIERS:
                //  - SELECT [ALL|DISTINCT]
                //  - UNION|INTERSECT|EXCEPT [ALL|DISTINCT]
                //  - AGGREGATE([ALL|DISTINCT] ...)
                SetQuantifier.ALL
            }
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
            integerNode: TerminalNode?
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
                    val p = integerNode.text.toInteger().toInt()
                    if (p < 0 || 9 < p) throw error(integerNode.symbol, "Precision out of bounds")
                    p
                }
            }
            return timeString to precision
        }

        /**
         * Parses a [timeString] using [OffsetTime] and converts to a [Expr.Time]. Fall back to [convertLocalTime].
         */
        private fun convertOffsetTime(timeString: String, precision: Int, stringNode: TerminalNode): Expr.Time = try {
            val time: OffsetTime = OffsetTime.parse(timeString)
            Expr.Time(
                id = id(),
                hour = time.hour,
                minute = time.minute,
                second = time.second,
                nano = time.nano,
                precision = precision,
                withTz = true,
                tzOffsetMinutes = time.offset.totalSeconds / 60,
            )
        } catch (e: DateTimeParseException) {
            convertLocalTime(timeString, true, precision, stringNode)
        }

        /**
         * Parses a [timeString] using [LocalTime] and converts to a [Expr.LitTime]
         */
        private fun convertLocalTime(
            timeString: String,
            withTimeZone: Boolean,
            precision: Int,
            stringNode: TerminalNode,
        ): Expr.Time {
            val time: LocalTime
            val formatter = when (withTimeZone) {
                false -> DateTimeFormatter.ISO_TIME
                else -> DateTimeFormatter.ISO_LOCAL_TIME
            }
            try {
                time = LocalTime.parse(timeString, formatter)
            } catch (e: DateTimeParseException) {
                throw error(stringNode.symbol, "Unable to parse time", e)
            }
            return Expr.Time(
                id = id(),
                hour = time.hour,
                minute = time.minute,
                second = time.second,
                nano = time.nano,
                precision = precision,
                withTz = withTimeZone,
                tzOffsetMinutes = null,
            )
        }

        private fun getPrecisionFromTimeString(timeString: String): Int {
            val matcher = GENERIC_TIME_REGEX.toPattern().matcher(timeString)
            if (!matcher.find()) {
                throw PartiQLParserException("Time string does not match the format 'HH:MM:SS[.ddd....][+|-HH:MM]'")
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
        protected fun convertPathToProjectionItem(ctx: ParserRuleContext, path: Expr.Path, alias: String?) =
            translate(ctx) {
                val steps = mutableListOf<Expr.Path.Step>()
                var containsIndex = false
                path.steps.forEachIndexed { index, step ->
                    // Only last step can have a '.*'
                    if (step is Expr.Path.Step.Unpivot && index != path.steps.lastIndex) {
                        throw PartiQLParserException("Projection item cannot unpivot unless at end.")
                    }
                    // No step can have an indexed wildcard: '[*]'
                    if (step is Expr.Path.Step.Wildcard) {
                        throw PartiQLParserException("Projection item cannot index using wildcard.")
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
                    throw PartiQLParserException("Projection item use wildcard with any indexing.")
                }
                when {
                    path.steps.last() is Expr.Path.Step.Unpivot && steps.isEmpty() -> {
                        Select.Project.Item.All(path.id, path.root)
                    }
                    path.steps.last() is Expr.Path.Step.Unpivot -> {
                        Select.Project.Item.All(path.id, Expr.Path(id(), path.root, steps))
                    }
                    else -> {
                        Select.Project.Item.Var(id(), path, alias)
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

        private fun GeneratedParser.SymbolPrimitiveContext.getString(): String {
            return when {
                this.IDENTIFIER_QUOTED() != null -> this.IDENTIFIER_QUOTED().getStringValue()
                this.IDENTIFIER() != null -> this.IDENTIFIER().text
                else -> throw PartiQLParserException("Unable to get symbol's text.")
            }
        }

        private fun parseToIntElement(text: String): IntElement =
            try {
                ionInt(text.toLong())
            } catch (e: NumberFormatException) {
                ionInt(text.toBigInteger())
            }

        private fun String.toInteger() = BigInteger(this, 10)

        private fun assertIntegerElement(token: Token?, value: IonElement?) {
            if (value == null || token == null)
                return
            if (value !is IntElement)
                throw error(token, "Expected an integer value.")
            if (value.integerSize == IntElementSize.BIG_INTEGER || value.longValue > Int.MAX_VALUE || value.longValue < Int.MIN_VALUE)
                throw error(token, "Type parameter exceeded maximum value")
        }

        private enum class ExplainParameters {
            TYPE,
            FORMAT;

            fun getCompliantString(target: String?, input: Token): String = when (target) {
                null -> input.text!!
                else -> throw error(input, "Cannot set EXPLAIN parameter ${this.name} multiple times.")
            }
        }
    }
}
