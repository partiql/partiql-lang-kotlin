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
import com.amazon.ionelement.api.DecimalElement
import com.amazon.ionelement.api.FloatElement
import com.amazon.ionelement.api.IntElement
import com.amazon.ionelement.api.IntElementSize
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionDecimal
import com.amazon.ionelement.api.ionFloat
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionNull
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionSymbol
import com.amazon.ionelement.api.loadSingleElement
import com.ibm.icu.text.CurrencyMetaInfo.CurrencyFilter.all
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
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import org.partiql.ast.AstNode
import org.partiql.ast.Case
import org.partiql.ast.Expr
import org.partiql.ast.SetQuantifier
import org.partiql.ast.Statement
import org.partiql.ast.TableDefinition
import org.partiql.parser.PartiQLParserDefault.Visitor
import org.partiql.parser.antlr.PartiQLBaseVisitor
import org.partiql.parser.antlr.PartiQLParser.ColumnDeclarationContext
import org.partiql.parser.antlr.PartiQLParser.RootContext
import org.partiql.parser.antlr.PartiQLParser.TableDefPartContext
import org.partiql.types.StaticType
import sun.jvm.hotspot.utilities.AddressOps.gte
import sun.jvm.hotspot.utilities.AddressOps.lte
import java.math.BigInteger
import java.nio.channels.ClosedByInterruptException
import java.nio.charset.StandardCharsets
import java.time.LocalTime
import java.time.OffsetTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.management.Query.eq
import kotlin.reflect.KClass
import kotlin.reflect.cast
import org.partiql.parser.antlr.PartiQLParser as GeneratedParser
import org.partiql.parser.antlr.PartiQLTokens as GeneratedLexer

private typealias SourceLocations = MutableMap<Int, PartiQLParser.SourceLocation>

/**
 * ANTLR Based Implementation of PartiQLParser
 * [GeneratedParser] to create an ANTLR [ParseTree] from the input query. Then, it uses the configured [Visitor]
 * to convert the [ParseTree] into a [Statement].
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

    @Throws(PartiQLParserException::class, InterruptedException::class)
    override fun parse(source: String): PartiQLParser.Result {
        try {
            return PartiQLParserDefault.parse(source)
        } catch (throwable: Throwable) {
            throw PartiQLParserException.wrap(throwable)
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
                else -> throw PartiQLParserException("Unsupported parser mode: $mode")
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
                msg, e, mapOf(
                    "line_no" to line.toLong(),
                    "column_no" to charPositionInLine.toLong() + 1,
                    "token_string" to msg,
                )
            )
        }
    }

    /**
     * Catches Parser errors (malformed syntax) and throws a [PartiQLParserException]
     */
    private class ParseErrorListener : BaseErrorListener() {

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
                    msg, e, mapOf(
                        "line_no" to line.toLong(),
                        "column_no" to charPositionInLine.toLong() + 1,
                        "token_description" to offendingSymbol.type.getAntlrDisplayString(),
                        "token_value" to offendingSymbol.asIonElement(),
                    )
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
     * Extends ANTLR's generated [PartiQLBaseVisitor] to visit an ANTLR ParseTree and convert it into a PartiQL AST. This
     * class uses the [AstNode] to represent all nodes within the new AST.
     */
    private class Visitor(
        private val locations: SourceLocations,
        private val parameters: Map<Int, Int> = mapOf(),
    ) : PartiQLBaseVisitor<AstNode>() {

        private val id: () -> Int = run {
            var i = 0
            { i++ }
        }

        companion object {

            /**
             * Expose an (internal) friendly entry point into the traversal; mostly for keeping mutable state contained.
             */
            fun translate(source: String, tokens: CountingTokenStream, tree: RootContext): PartiQLParser.Result {
                val locations = mutableMapOf<Int, PartiQLParser.SourceLocation>()
                val visitor = Visitor(locations, tokens.parameterIndexes)
                val root = visitor.visit(tree)
                return PartiQLParser.Result(
                    source = source,
                    root = root,
                    locations = PartiQLParser.SourceLocations(source, locations),
                )
            }
        }

        /**
         * Each visit attaches source locations from the given parse tree node; inline because gotta go fast.
         */
        inline fun translate(ctx: ParserRuleContext, translate: () -> AstNode): AstNode {
            val node = translate()
            if (ctx.start != null) {
                locations[node.id] = PartiQLParser.SourceLocation(
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
                GeneratedParser.IDENTIFIER_QUOTED -> Expr.Identifier(
                    id(),
                    ctx.IDENTIFIER_QUOTED().getStringValue(),
                    Case.SENSITIVE,
                    Expr.Identifier.Scope.UNQUALIFIED,
                )
                GeneratedParser.IDENTIFIER -> Expr.Identifier(
                    id(),
                    ctx.IDENTIFIER().getStringValue(),
                    Case.INSENSITIVE,
                    Expr.Identifier.Scope.UNQUALIFIED,
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
            val table = visitSymbolPrimitive(ctx.tableName().symbolPrimitive()) as Expr.Identifier
            Statement.DDL.DropTable(id(), table)
        }

        override fun visitDropIndex(ctx: GeneratedParser.DropIndexContext) = translate(ctx) {
            val table = visitSymbolPrimitive(ctx.target) as Expr.Identifier
            val keys = visitSymbolPrimitive(ctx.on) as Expr.Identifier
            Statement.DDL.DropIndex(id(), table, keys)
        }

        override fun visitCreateTable(ctx: GeneratedParser.CreateTableContext) = translate(ctx) {
            val identifier = visitRawSymbol(ctx.tableName().symbolPrimitive())
            val definition = ctx.tableDef()?.let { visitTableDef(it) as TableDefinition }
            Statement.DDL.CreateTable(id(), identifier, definition)
        }

        override fun visitCreateIndex(ctx: GeneratedParser.CreateIndexContext) = translate(ctx) {
            val identifier = visitRawSymbol(ctx.symbolPrimitive())
            val fields = ctx.pathSimple().map { path -> visitPathSimple(path) as Expr }
            Statement.DDL.CreateIndex(id(), identifier, fields)
        }

        override fun visitTableDef(ctx: GeneratedParser.TableDefContext) = translate(ctx) {
            // Column Definitions are the only thing we currently allow as table definition parts
            val columns = ctx.tableDefPart().filterIsInstance<ColumnDeclarationContext>().map {
                visitColumnDeclaration(it) as TableDefinition.Column
            }
            TableDefinition(id(), columns)
        }

        override fun visitColumnDeclaration(ctx: GeneratedParser.ColumnDeclarationContext) = translate(ctx) {
            val name = visitRawSymbol(ctx.columnName().symbolPrimitive())
            // val type = visit(ctx.type(), Type::class)
            val type = StaticType.ANY
            val constraints = ctx.columnConstraint().map { visitColumnConstraint(it) as TableDefinition.Column.Constraint }
            TableDefinition.Column(id(), name, type, constraints)
        }

        override fun visitColumnConstraint(ctx: GeneratedParser.ColumnConstraintContext) = translate(ctx) {
            val identifier = ctx.columnConstraintName()?.let { visitRawSymbol(it.symbolPrimitive()) }
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

        // override fun visitQueryExec(ctx: GeneratedParser.QueryExecContext) = visitExecCommand(ctx.execCommand())
        //
        // override fun visitExecCommand(ctx: GeneratedParser.ExecCommandContext) = build {
        //     val name = visitExpr(ctx.name).getStringValue(ctx.name.getStart())
        //     val args = visitOrEmpty(ctx.args, Expr::class)
        //     exec_(
        //         SymbolPrimitive(name.toLowerCase(), emptyMetaContainer()),
        //         args,
        //         ctx.name.getStart().getSourceMetaContainer()
        //     )
        // }
        //
        // /**
        //  *
        //  * DATA MANIPULATION LANGUAGE (DML)
        //  *
        //  */
        //
        // override fun visitDmlBaseWrapper(ctx: GeneratedParser.DmlBaseWrapperContext) = build {
        //     val sourceContext = when {
        //         ctx.updateClause() != null -> ctx.updateClause()
        //         ctx.fromClause() != null -> ctx.fromClause()
        //         else -> throw PartiQLParserException(
        //             "Unable to deduce from source in DML",
        //             ErrorCode.PARSE_INVALID_QUERY
        //         )
        //     }
        //     val from = visitOrNull(sourceContext, FromSource::class)
        //     val where = visitOrNull(ctx.whereClause(), Expr::class)
        //     val returning = visitOrNull(ctx.returningClause(), ReturningExpr::class)
        //     val operations = ctx.dmlBaseCommand().map { command -> getCommandList(visit(command)) }.flatten()
        //     dml(dmlOpList(operations, operations[0].metas), from, where, returning, metas = operations[0].metas)
        // }
        //
        // override fun visitDmlBase(ctx: GeneratedParser.DmlBaseContext) = build {
        //     val commands = getCommandList(visit(ctx.dmlBaseCommand()))
        //     dml(dmlOpList(commands, commands[0].metas), metas = commands[0].metas)
        // }
        //
        // private fun getCommandList(command: AstNode): List<DmlOp> {
        //     return when (command) {
        //         is DmlOpList -> command.ops
        //         is DmlOp -> listOf(command)
        //         else -> throw PartiQLParserException("Unable to grab DML operation.", ErrorCode.PARSE_INVALID_QUERY)
        //     }
        // }
        //
        // override fun visitRemoveCommand(ctx: GeneratedParser.RemoveCommandContext) = build {
        //     val target = visitPathSimple(ctx.pathSimple())
        //     remove(target, ctx.REMOVE().getSourceMetaContainer())
        // }
        //
        // override fun visitDeleteCommand(ctx: GeneratedParser.DeleteCommandContext) = build {
        //     val from = visit(ctx.fromClauseSimple(), FromSource::class)
        //     val where = visitOrNull(ctx.whereClause(), Expr::class)
        //     val returning = visitOrNull(ctx.returningClause(), ReturningExpr::class)
        //     dml(
        //         dmlOpList(delete(ctx.DELETE().getSourceMetaContainer()), metas = ctx.DELETE().getSourceMetaContainer()),
        //         from,
        //         where,
        //         returning,
        //         ctx.DELETE().getSourceMetaContainer()
        //     )
        // }
        //
        // override fun visitInsertLegacy(ctx: GeneratedParser.InsertLegacyContext) = build {
        //     val metas = ctx.INSERT().getSourceMetaContainer()
        //     val target = visitPathSimple(ctx.pathSimple())
        //     val index = visitOrNull(ctx.pos, Expr::class)
        //     val onConflict = visitOrNull(ctx.onConflictClause(), OnConflict::class)
        //     insertValue(target, visit(ctx.value, Expr::class), index, onConflict, metas)
        // }
        //
        // override fun visitInsert(ctx: GeneratedParser.InsertContext) = build {
        //     val metas = ctx.INSERT().getSourceMetaContainer()
        //     val asIdent = ctx.asIdent()
        //     // Based on the RFC, if alias exists the table must be hidden behind the alias, see:
        //     // https://github.com/partiql/partiql-docs/blob/main/RFCs/0011-partiql-insert.md#41-insert-parameters
        //     val target = if (asIdent != null) visitAsIdent(asIdent) else visitSymbolPrimitive(ctx.symbolPrimitive())
        //     val conflictAction = visitOrNull(ctx.onConflictClause(), ConflictAction::class)
        //     insert(target, visit(ctx.value, Expr::class), conflictAction, metas)
        // }
        //
        // // TODO move from experimental; pending: https://github.com/partiql/partiql-docs/issues/27
        // override fun visitReplaceCommand(ctx: GeneratedParser.ReplaceCommandContext) = build {
        //     val asIdent = ctx.asIdent()
        //     // Based on the RFC, if alias exists the table must be hidden behind the alias, see:
        //     // https://github.com/partiql/partiql-docs/blob/main/RFCs/0011-partiql-insert.md#41-insert-parameters
        //     val target = if (asIdent != null) visitAsIdent(asIdent) else visitSymbolPrimitive(ctx.symbolPrimitive())
        //     insert(
        //         target = target,
        //         values = visit(ctx.value, Expr::class),
        //         conflictAction = doReplace(excluded()),
        //         metas = ctx.REPLACE().getSourceMetaContainer()
        //     )
        // }
        //
        // // Based on https://github.com/partiql/partiql-docs/blob/main/RFCs/0011-partiql-insert.md
        // override fun visitUpsertCommand(ctx: GeneratedParser.UpsertCommandContext) = build {
        //     val asIdent = ctx.asIdent()
        //     // Based on the RFC, if alias exists the table must be hidden behind the alias, see:
        //     // https://github.com/partiql/partiql-docs/blob/main/RFCs/0011-partiql-insert.md#41-insert-parameters
        //     val target = if (asIdent != null) visitAsIdent(asIdent) else visitSymbolPrimitive(ctx.symbolPrimitive())
        //     insert(
        //         target = target,
        //         values = visit(ctx.value, Expr::class),
        //         conflictAction = doUpdate(excluded()),
        //         metas = ctx.UPSERT().getSourceMetaContainer()
        //     )
        // }
        //
        // // Based on https://github.com/partiql/partiql-docs/blob/main/RFCs/0011-partiql-insert.md
        // override fun visitInsertCommandReturning(ctx: GeneratedParser.InsertCommandReturningContext) = build {
        //     val metas = ctx.INSERT().getSourceMetaContainer()
        //     val target = visitPathSimple(ctx.pathSimple())
        //     val index = visitOrNull(ctx.pos, Expr::class)
        //     val onConflictLegacy = visitOrNull(ctx.onConflictClause(), OnConflict::class)
        //     val returning = visitOrNull(ctx.returningClause(), ReturningExpr::class)
        //     dml(
        //         dmlOpList(
        //             insertValue(
        //                 target,
        //                 visit(ctx.value, Expr::class),
        //                 index = index,
        //                 onConflict = onConflictLegacy,
        //                 ctx.INSERT().getSourceMetaContainer()
        //             ),
        //             metas = metas
        //         ),
        //         returning = returning,
        //         metas = metas
        //     )
        // }
        //
        // override fun visitReturningClause(ctx: GeneratedParser.ReturningClauseContext) = build {
        //     val elements = visitOrEmpty(ctx.returningColumn(), ReturningElem::class)
        //     returningExpr(elements, ctx.RETURNING().getSourceMetaContainer())
        // }
        //
        // private fun getReturningMapping(status: Token, age: Token) = build {
        //     when {
        //         status.type == GeneratedParser.MODIFIED && age.type == GeneratedParser.NEW -> modifiedNew()
        //         status.type == GeneratedParser.MODIFIED && age.type == GeneratedParser.OLD -> modifiedOld()
        //         status.type == GeneratedParser.ALL && age.type == GeneratedParser.NEW -> allNew()
        //         status.type == GeneratedParser.ALL && age.type == GeneratedParser.OLD -> allOld()
        //         else -> throw status.err("Unable to get return mapping.", ErrorCode.PARSE_UNEXPECTED_TOKEN)
        //     }
        // }
        //
        // override fun visitReturningColumn(ctx: GeneratedParser.ReturningColumnContext) = build {
        //     val column = when (ctx.ASTERISK()) {
        //         null -> returningColumn(visitExpr(ctx.expr()))
        //         else -> returningWildcard()
        //     }
        //     returningElem(getReturningMapping(ctx.status, ctx.age), column)
        // }
        //
        // override fun visitOnConflict(ctx: GeneratedParser.OnConflictContext) = build {
        //     visit(ctx.conflictAction(), ConflictAction::class)
        // }
        //
        // override fun visitOnConflictLegacy(ctx: GeneratedParser.OnConflictLegacyContext) = build {
        //     onConflict(
        //         expr = visitExpr(ctx.expr()),
        //         conflictAction = doNothing(),
        //         metas = ctx.ON().getSourceMetaContainer()
        //     )
        // }
        //
        // override fun visitConflictAction(ctx: GeneratedParser.ConflictActionContext) = build {
        //     when {
        //         ctx.NOTHING() != null -> doNothing()
        //         ctx.REPLACE() != null -> visitDoReplace(ctx.doReplace())
        //         ctx.UPDATE() != null -> visitDoUpdate(ctx.doUpdate())
        //         else -> TODO("ON CONFLICT only supports `DO REPLACE` and `DO NOTHING` actions at the moment.")
        //     }
        // }
        //
        // override fun visitDoReplace(ctx: GeneratedParser.DoReplaceContext) = build {
        //     val value = when {
        //         ctx.EXCLUDED() != null -> excluded()
        //         else -> TODO("DO REPLACE doesn't support values other than `EXCLUDED` yet.")
        //     }
        //     doReplace(value)
        // }
        //
        // override fun visitDoUpdate(ctx: GeneratedParser.DoUpdateContext) = build {
        //     val value = when {
        //         ctx.EXCLUDED() != null -> excluded()
        //         else -> TODO("DO UPDATE doesn't support values other than `EXCLUDED` yet.")
        //     }
        //     doUpdate(value)
        // }
        //
        // override fun visitPathSimple(ctx: GeneratedParser.PathSimpleContext) = build {
        //     val root = visitSymbolPrimitive(ctx.symbolPrimitive())
        //     if (ctx.pathSimpleSteps().isEmpty()) return@build root
        //     val steps = visitOrEmpty(ctx.pathSimpleSteps(), PathStep::class)
        //     path(root, steps, root.metas)
        // }
        //
        // override fun visitPathSimpleLiteral(ctx: GeneratedParser.PathSimpleLiteralContext) = build {
        //     pathExpr(visit(ctx.literal()) as Expr, caseSensitive())
        // }
        //
        // override fun visitPathSimpleSymbol(ctx: GeneratedParser.PathSimpleSymbolContext) = build {
        //     pathExpr(visitSymbolPrimitive(ctx.symbolPrimitive()), caseSensitive())
        // }
        //
        // override fun visitPathSimpleDotSymbol(ctx: GeneratedParser.PathSimpleDotSymbolContext) =
        //     getSymbolPathExpr(ctx.symbolPrimitive())
        //
        // override fun visitSetCommand(ctx: GeneratedParser.SetCommandContext) = build {
        //     val assignments = visitOrEmpty(ctx.setAssignment(), DmlOp.Set::class)
        //     val newSets = assignments.map { assignment -> assignment.copy(metas = ctx.SET().getSourceMetaContainer()) }
        //     dmlOpList(newSets, ctx.SET().getSourceMetaContainer())
        // }
        //
        // override fun visitSetAssignment(ctx: GeneratedParser.SetAssignmentContext) = build {
        //     set(assignment(visitPathSimple(ctx.pathSimple()), visitExpr(ctx.expr())))
        // }
        //
        // override fun visitUpdateClause(ctx: GeneratedParser.UpdateClauseContext) =
        //     visit(ctx.tableBaseReference(), FromSource::class)
        //
        // /**
        //  *
        //  * DATA QUERY LANGUAGE (DQL)
        //  *
        //  */
        //
        // override fun visitDql(ctx: GeneratedParser.DqlContext) = build {
        //     val query = visit(ctx.expr(), Expr::class)
        //     query(query, query.metas)
        // }
        //
        // override fun visitQueryBase(ctx: GeneratedParser.QueryBaseContext) = visit(ctx.exprSelect(), Expr::class)
        //
        // override fun visitSfwQuery(ctx: GeneratedParser.SfwQueryContext) = build {
        //     val projection = visit(ctx.select, Projection::class)
        //     val strategy = getSetQuantifierStrategy(ctx.select)
        //     val from = visitFromClause(ctx.from)
        //     val order = visitOrNull(ctx.order, OrderBy::class)
        //     val group = visitOrNull(ctx.group, GroupBy::class)
        //     val limit = visitOrNull(ctx.limit, Expr::class)
        //     val offset = visitOrNull(ctx.offset, Expr::class)
        //     val where = visitOrNull(ctx.where, Expr::class)
        //     val having = visitOrNull(ctx.having, Expr::class)
        //     val let = visitOrNull(ctx.let, Let::class)
        //     val metas = ctx.selectClause().getMetas()
        //     select(
        //         project = projection,
        //         from = from,
        //         setq = strategy,
        //         order = order,
        //         group = group,
        //         limit = limit,
        //         offset = offset,
        //         where = where,
        //         having = having,
        //         fromLet = let,
        //         metas = metas
        //     )
        // }
        //
        // /**
        //  *
        //  * SELECT & PROJECTIONS
        //  *
        //  */
        //
        // override fun visitSetQuantifierStrategy(ctx: GeneratedParser.SetQuantifierStrategyContext?): SetQuantifier? =
        //     when {
        //         ctx == null -> null
        //         ctx.DISTINCT() != null -> SetQuantifier.Distinct()
        //         ctx.ALL() != null -> SetQuantifier.All()
        //         else -> null
        //     }
        //
        // override fun visitSelectAll(ctx: GeneratedParser.SelectAllContext) = build {
        //     projectStar(ctx.ASTERISK().getSourceMetaContainer())
        // }
        //
        // override fun visitSelectItems(ctx: GeneratedParser.SelectItemsContext) =
        //     convertProjectionItems(ctx.projectionItems(), ctx.SELECT().getSourceMetaContainer())
        //
        // override fun visitSelectPivot(ctx: GeneratedParser.SelectPivotContext) = build {
        //     projectPivot(visitExpr(ctx.at), visitExpr(ctx.pivot))
        // }
        //
        // override fun visitSelectValue(ctx: GeneratedParser.SelectValueContext) = build {
        //     projectValue(visitExpr(ctx.expr()))
        // }
        //
        // override fun visitProjectionItem(ctx: GeneratedParser.ProjectionItemContext) = build {
        //     val expr = visit(ctx.expr(), Expr::class)
        //     val alias = visitOrNull(ctx.symbolPrimitive(), Expr.Id::class)?.name
        //     if (expr is Expr.Path) convertPathToProjectionItem(expr, alias)
        //     else projectExpr_(expr, asAlias = alias, expr.metas)
        // }
        //
        // /**
        //  *
        //  * SIMPLE CLAUSES
        //  *
        //  */
        //
        // override fun visitLimitClause(ctx: GeneratedParser.LimitClauseContext): Expr = visit(ctx.arg, Expr::class)
        //
        // override fun visitExpr(ctx: GeneratedParser.ExprContext): Expr {
        //     checkThreadInterrupted()
        //     return visit(ctx.exprBagOp(), Expr::class)
        // }
        //
        // override fun visitOffsetByClause(ctx: GeneratedParser.OffsetByClauseContext) = visit(ctx.arg, Expr::class)
        //
        // override fun visitWhereClause(ctx: GeneratedParser.WhereClauseContext) = visitExpr(ctx.arg)
        //
        // override fun visitWhereClauseSelect(ctx: GeneratedParser.WhereClauseSelectContext) = visit(ctx.arg, Expr::class)
        //
        // override fun visitHavingClause(ctx: GeneratedParser.HavingClauseContext) = visit(ctx.arg, Expr::class)
        //
        // /**
        //  *
        //  * LET CLAUSE
        //  *
        //  */
        //
        // override fun visitLetClause(ctx: GeneratedParser.LetClauseContext) = build {
        //     val letBindings = visitOrEmpty(ctx.letBinding(), LetBinding::class)
        //     let(letBindings)
        // }
        //
        // override fun visitLetBinding(ctx: GeneratedParser.LetBindingContext) = build {
        //     val expr = visit(ctx.expr(), Expr::class)
        //     val metas = ctx.symbolPrimitive().getSourceMetaContainer()
        //     letBinding_(expr, convertSymbolPrimitive(ctx.symbolPrimitive())!!, metas)
        // }
        //
        // /**
        //  *
        //  * ORDER BY CLAUSE
        //  *
        //  */
        //
        // override fun visitOrderByClause(ctx: GeneratedParser.OrderByClauseContext) = build {
        //     val sortSpecs = visitOrEmpty(ctx.orderSortSpec(), SortSpec::class)
        //     val metas = ctx.ORDER().getSourceMetaContainer()
        //     orderBy(sortSpecs, metas)
        // }
        //
        // override fun visitOrderSortSpec(ctx: GeneratedParser.OrderSortSpecContext) = build {
        //     val expr = visit(ctx.expr(), Expr::class)
        //     val orderSpec = when {
        //         ctx.dir == null -> null
        //         ctx.dir.type == GeneratedParser.ASC -> asc()
        //         ctx.dir.type == GeneratedParser.DESC -> desc()
        //         else -> throw ctx.dir.err("Invalid query syntax", ErrorCode.PARSE_INVALID_QUERY)
        //     }
        //     val nullSpec = when {
        //         ctx.nulls == null -> null
        //         ctx.nulls.type == GeneratedParser.FIRST -> nullsFirst()
        //         ctx.nulls.type == GeneratedParser.LAST -> nullsLast()
        //         else -> throw ctx.dir.err("Invalid query syntax", ErrorCode.PARSE_INVALID_QUERY)
        //     }
        //     sortSpec(expr, orderingSpec = orderSpec, nullsSpec = nullSpec)
        // }
        //
        // /**
        //  *
        //  * GROUP BY CLAUSE
        //  *
        //  */
        //
        // override fun visitGroupClause(ctx: GeneratedParser.GroupClauseContext) = build {
        //     val strategy = if (ctx.PARTIAL() != null) groupPartial() else groupFull()
        //     val keys = visitOrEmpty(ctx.groupKey(), GroupKey::class)
        //     val keyList = groupKeyList(keys)
        //     val alias = visitOrNull(ctx.groupAlias(), Expr.Id::class).toPigSymbolPrimitive()
        //     groupBy_(strategy, keyList = keyList, groupAsAlias = alias, ctx.GROUP().getSourceMetaContainer())
        // }
        //
        // override fun visitGroupAlias(ctx: GeneratedParser.GroupAliasContext) =
        //     visitSymbolPrimitive(ctx.symbolPrimitive())
        //
        // /**
        //  * Returns a GROUP BY key
        //  * TODO: Support ordinal case. Also, the conditional defining the exception is odd. 1 + 1 is allowed, but 2 is not.
        //  *  This is to match the functionality of SqlParser, but this should likely be adjusted.
        //  */
        // override fun visitGroupKey(ctx: GeneratedParser.GroupKeyContext) = build {
        //     val expr = visit(ctx.key, Expr::class)
        //     val possibleLiteral = when (expr) {
        //         is Expr.Pos -> expr.expr
        //         is Expr.Neg -> expr.expr
        //         else -> expr
        //     }
        //     if (
        //         (possibleLiteral is Expr.Lit && possibleLiteral.value != ionNull()) ||
        //         possibleLiteral is Expr.LitTime || possibleLiteral is Expr.Date
        //     ) {
        //         throw ctx.key.getStart().err(
        //             "Literals (including ordinals) not supported in GROUP BY",
        //             ErrorCode.PARSE_UNSUPPORTED_LITERALS_GROUPBY
        //         )
        //     }
        //     val alias = visitOrNull(ctx.symbolPrimitive(), Expr.Id::class).toPigSymbolPrimitive()
        //     groupKey_(expr, asAlias = alias, expr.metas)
        // }
        //
        // /**
        //  *
        //  * BAG OPERATIONS
        //  *
        //  */
        //
        // override fun visitIntersect(ctx: GeneratedParser.IntersectContext) = build {
        //     val lhs = visit(ctx.lhs, Expr::class)
        //     val rhs = visit(ctx.rhs, Expr::class)
        //     val quantifier = if (ctx.ALL() != null) all() else distinct()
        //     val (intersect, metas) = when (ctx.OUTER()) {
        //         null -> intersect() to ctx.INTERSECT().getSourceMetaContainer()
        //         else -> outerIntersect() to ctx.OUTER().getSourceMetaContainer()
        //     }
        //     bagOp(intersect, quantifier, listOf(lhs, rhs), metas)
        // }
        //
        // override fun visitExcept(ctx: GeneratedParser.ExceptContext) = build {
        //     val lhs = visit(ctx.lhs, Expr::class)
        //     val rhs = visit(ctx.rhs, Expr::class)
        //     val quantifier = if (ctx.ALL() != null) all() else distinct()
        //     val (except, metas) = when (ctx.OUTER()) {
        //         null -> except() to ctx.EXCEPT().getSourceMetaContainer()
        //         else -> outerExcept() to ctx.OUTER().getSourceMetaContainer()
        //     }
        //     bagOp(except, quantifier, listOf(lhs, rhs), metas)
        // }
        //
        // override fun visitUnion(ctx: GeneratedParser.UnionContext) = build {
        //     val lhs = visit(ctx.lhs, Expr::class)
        //     val rhs = visit(ctx.rhs, Expr::class)
        //     val quantifier = if (ctx.ALL() != null) all() else distinct()
        //     val (union, metas) = when (ctx.OUTER()) {
        //         null -> union() to ctx.UNION().getSourceMetaContainer()
        //         else -> outerUnion() to ctx.OUTER().getSourceMetaContainer()
        //     }
        //     bagOp(union, quantifier, listOf(lhs, rhs), metas)
        // }
        //
        // /**
        //  *
        //  * GRAPH PATTERN MANIPULATION LANGUAGE (GPML)
        //  *
        //  */
        //
        // override fun visitGpmlPattern(ctx: GeneratedParser.GpmlPatternContext) = build {
        //     val selector = visitOrNull(ctx.matchSelector(), GraphMatchSelector::class)
        //     val pattern = visitMatchPattern(ctx.matchPattern())
        //     gpmlPattern(selector, listOf(pattern))
        // }
        //
        // override fun visitGpmlPatternList(ctx: GeneratedParser.GpmlPatternListContext) = build {
        //     val selector = visitOrNull(ctx.matchSelector(), GraphMatchSelector::class)
        //     val patterns = ctx.matchPattern().map { pattern -> visitMatchPattern(pattern) }
        //     gpmlPattern(selector, patterns)
        // }
        //
        // override fun visitMatchPattern(ctx: GeneratedParser.MatchPatternContext) = build {
        //     val parts = visitOrEmpty(ctx.graphPart(), GraphMatchPatternPart::class)
        //     val restrictor = visitOrNull(ctx.restrictor, GraphMatchRestrictor::class)
        //     val variable = visitOrNull(ctx.variable, Expr.Id::class)?.name
        //     graphMatchPattern_(parts = parts, restrictor = restrictor, variable = variable)
        // }
        //
        // override fun visitPatternPathVariable(ctx: GeneratedParser.PatternPathVariableContext) =
        //     visitSymbolPrimitive(ctx.symbolPrimitive())
        //
        // override fun visitSelectorBasic(ctx: GeneratedParser.SelectorBasicContext) = build {
        //     val metas = ctx.mod.getSourceMetaContainer()
        //     when (ctx.mod.type) {
        //         GeneratedParser.ANY -> selectorAnyShortest(metas)
        //         GeneratedParser.ALL -> selectorAllShortest(metas)
        //         else -> throw PartiQLParserException("Unsupported match selector.", ErrorCode.PARSE_INVALID_QUERY)
        //     }
        // }
        //
        // override fun visitSelectorAny(ctx: GeneratedParser.SelectorAnyContext) = build {
        //     val metas = ctx.ANY().getSourceMetaContainer()
        //     when (ctx.k) {
        //         null -> selectorAny(metas)
        //         else -> selectorAnyK(ctx.k.text.toLong(), metas)
        //     }
        // }
        //
        // override fun visitSelectorShortest(ctx: GeneratedParser.SelectorShortestContext) = build {
        //     val k = ctx.k.text.toLong()
        //     val metas = ctx.k.getSourceMetaContainer()
        //     when (ctx.GROUP()) {
        //         null -> selectorShortestK(k, metas)
        //         else -> selectorShortestKGroup(k, metas)
        //     }
        // }
        //
        // override fun visitPatternPartLabel(ctx: GeneratedParser.PatternPartLabelContext) =
        //     visitSymbolPrimitive(ctx.symbolPrimitive())
        //
        // override fun visitPattern(ctx: GeneratedParser.PatternContext) = build {
        //     val restrictor = visitOrNull(ctx.restrictor, GraphMatchRestrictor::class)
        //     val variable = visitOrNull(ctx.variable, Expr.Id::class)?.name
        //     val prefilter = visitOrNull(ctx.where, Expr::class)
        //     val quantifier = visitOrNull(ctx.quantifier, GraphMatchQuantifier::class)
        //     val parts = visitOrEmpty(ctx.graphPart(), GraphMatchPatternPart::class)
        //     pattern(
        //         graphMatchPattern_(
        //             parts = parts,
        //             variable = variable,
        //             restrictor = restrictor,
        //             quantifier = quantifier,
        //             prefilter = prefilter
        //         )
        //     )
        // }
        //
        // override fun visitEdgeAbbreviated(ctx: GeneratedParser.EdgeAbbreviatedContext) = build {
        //     val direction = visitEdgeAbbrev(ctx.edgeAbbrev())
        //     val quantifier = visitOrNull(ctx.quantifier, GraphMatchQuantifier::class)
        //     edge(direction = direction, quantifier = quantifier)
        // }
        //
        // override fun visitEdgeWithSpec(ctx: GeneratedParser.EdgeWithSpecContext) = build {
        //     val quantifier = visitOrNull(ctx.quantifier, GraphMatchQuantifier::class)
        //     val edge = visitOrNull(ctx.edgeWSpec(), GraphMatchPatternPart.Edge::class)
        //     edge!!.copy(quantifier = quantifier)
        // }
        //
        // override fun visitEdgeSpec(ctx: GeneratedParser.EdgeSpecContext) = build {
        //     val placeholderDirection = edgeRight()
        //     val variable = visitOrNull(ctx.symbolPrimitive(), Expr.Id::class)?.name
        //     val prefilter = visitOrNull(ctx.whereClause(), Expr::class)
        //     val label = visitOrNull(ctx.patternPartLabel(), Expr.Id::class)?.name
        //     edge_(
        //         direction = placeholderDirection,
        //         variable = variable,
        //         prefilter = prefilter,
        //         label = listOfNotNull(label)
        //     )
        // }
        //
        // override fun visitEdgeSpecLeft(ctx: GeneratedParser.EdgeSpecLeftContext) = build {
        //     val edge = visitEdgeSpec(ctx.edgeSpec())
        //     edge.copy(direction = edgeLeft())
        // }
        //
        // override fun visitEdgeSpecRight(ctx: GeneratedParser.EdgeSpecRightContext) = build {
        //     val edge = visitEdgeSpec(ctx.edgeSpec())
        //     edge.copy(direction = edgeRight())
        // }
        //
        // override fun visitEdgeSpecBidirectional(ctx: GeneratedParser.EdgeSpecBidirectionalContext) = build {
        //     val edge = visitEdgeSpec(ctx.edgeSpec())
        //     edge.copy(direction = edgeLeftOrRight())
        // }
        //
        // override fun visitEdgeSpecUndirectedBidirectional(ctx: GeneratedParser.EdgeSpecUndirectedBidirectionalContext) =
        //     build {
        //         val edge = visitEdgeSpec(ctx.edgeSpec())
        //         edge.copy(direction = edgeLeftOrUndirectedOrRight())
        //     }
        //
        // override fun visitEdgeSpecUndirected(ctx: GeneratedParser.EdgeSpecUndirectedContext) = build {
        //     val edge = visitEdgeSpec(ctx.edgeSpec())
        //     edge.copy(direction = edgeUndirected())
        // }
        //
        // override fun visitEdgeSpecUndirectedLeft(ctx: GeneratedParser.EdgeSpecUndirectedLeftContext) = build {
        //     val edge = visitEdgeSpec(ctx.edgeSpec())
        //     edge.copy(direction = edgeLeftOrUndirected())
        // }
        //
        // override fun visitEdgeSpecUndirectedRight(ctx: GeneratedParser.EdgeSpecUndirectedRightContext) = build {
        //     val edge = visitEdgeSpec(ctx.edgeSpec())
        //     edge.copy(direction = edgeUndirectedOrRight())
        // }
        //
        // override fun visitEdgeAbbrev(ctx: GeneratedParser.EdgeAbbrevContext) = build {
        //     when {
        //         ctx.TILDE() != null && ctx.ANGLE_RIGHT() != null -> edgeUndirectedOrRight()
        //         ctx.TILDE() != null && ctx.ANGLE_LEFT() != null -> edgeLeftOrUndirected()
        //         ctx.TILDE() != null -> edgeUndirected()
        //         ctx.MINUS() != null && ctx.ANGLE_LEFT() != null && ctx.ANGLE_RIGHT() != null -> edgeLeftOrRight()
        //         ctx.MINUS() != null && ctx.ANGLE_LEFT() != null -> edgeLeft()
        //         ctx.MINUS() != null && ctx.ANGLE_RIGHT() != null -> edgeRight()
        //         ctx.MINUS() != null -> edgeLeftOrUndirectedOrRight()
        //         else -> throw PartiQLParserException("Unsupported edge type", ErrorCode.PARSE_INVALID_QUERY)
        //     }
        // }
        //
        // override fun visitPatternQuantifier(ctx: GeneratedParser.PatternQuantifierContext) = build {
        //     when {
        //         ctx.quant == null -> graphMatchQuantifier(ctx.lower.text.toLong(), ctx.upper?.text?.toLong())
        //         ctx.quant.type == GeneratedParser.PLUS -> graphMatchQuantifier(1L)
        //         ctx.quant.type == GeneratedParser.ASTERISK -> graphMatchQuantifier(0L)
        //         else -> throw PartiQLParserException("Unsupported quantifier", ErrorCode.PARSE_INVALID_QUERY)
        //     }
        // }
        //
        // override fun visitNode(ctx: GeneratedParser.NodeContext) = build {
        //     val variable = visitOrNull(ctx.symbolPrimitive(), Expr.Id::class)?.name
        //     val prefilter = visitOrNull(ctx.whereClause(), Expr::class)
        //     val label = visitOrNull(ctx.patternPartLabel(), Expr.Id::class)?.name
        //     node_(variable = variable, prefilter = prefilter, label = listOfNotNull(label))
        // }
        //
        // override fun visitPatternRestrictor(ctx: GeneratedParser.PatternRestrictorContext) = build {
        //     val metas = ctx.restrictor.getSourceMetaContainer()
        //     when (ctx.restrictor.text.toLowerCase()) {
        //         "trail" -> restrictorTrail(metas)
        //         "acyclic" -> restrictorAcyclic(metas)
        //         "simple" -> restrictorSimple(metas)
        //         else -> throw PartiQLParserException("Unrecognized pattern restrictor", ErrorCode.PARSE_INVALID_QUERY)
        //     }
        // }
        //
        // /**
        //  *
        //  * TABLE REFERENCES & JOINS & FROM CLAUSE
        //  *
        //  */
        //
        // override fun visitFromClause(ctx: GeneratedParser.FromClauseContext) =
        //     visit(ctx.tableReference(), FromSource::class)
        //
        // override fun visitTableBaseRefClauses(ctx: GeneratedParser.TableBaseRefClausesContext) = build {
        //     val expr = visit(ctx.source, Expr::class)
        //     val (asAlias, atAlias, byAlias) = visitNullableItems(
        //         listOf(ctx.asIdent(), ctx.atIdent(), ctx.byIdent()),
        //         Expr.Id::class
        //     )
        //     scan_(
        //         expr,
        //         asAlias = asAlias.toPigSymbolPrimitive(),
        //         byAlias = byAlias.toPigSymbolPrimitive(),
        //         atAlias = atAlias.toPigSymbolPrimitive(),
        //         metas = expr.metas
        //     )
        // }
        //
        // override fun visitTableBaseRefMatch(ctx: GeneratedParser.TableBaseRefMatchContext) = build {
        //     val expr = visit(ctx.source, Expr::class)
        //     val (asAlias, atAlias, byAlias) = visitNullableItems(
        //         listOf(ctx.asIdent(), ctx.atIdent(), ctx.byIdent()),
        //         Expr.Id::class
        //     )
        //     scan_(
        //         expr,
        //         asAlias = asAlias.toPigSymbolPrimitive(),
        //         byAlias = byAlias.toPigSymbolPrimitive(),
        //         atAlias = atAlias.toPigSymbolPrimitive(),
        //         metas = expr.metas
        //     )
        // }
        //
        // override fun visitFromClauseSimpleExplicit(ctx: GeneratedParser.FromClauseSimpleExplicitContext) = build {
        //     val expr = visitPathSimple(ctx.pathSimple())
        //     val (asAlias, atAlias, byAlias) = visitNullableItems(
        //         listOf(ctx.asIdent(), ctx.atIdent(), ctx.byIdent()),
        //         Expr.Id::class
        //     )
        //     scan_(
        //         expr,
        //         asAlias = asAlias.toPigSymbolPrimitive(),
        //         byAlias = byAlias.toPigSymbolPrimitive(),
        //         atAlias = atAlias.toPigSymbolPrimitive(),
        //         metas = expr.metas
        //     )
        // }
        //
        // override fun visitTableUnpivot(ctx: GeneratedParser.TableUnpivotContext) = build {
        //     val expr = visitExpr(ctx.expr())
        //     val metas = ctx.UNPIVOT().getSourceMetaContainer()
        //     val (asAlias, atAlias, byAlias) = visitNullableItems(
        //         listOf(ctx.asIdent(), ctx.atIdent(), ctx.byIdent()),
        //         Expr.Id::class
        //     )
        //     unpivot_(
        //         expr,
        //         asAlias = asAlias.toPigSymbolPrimitive(),
        //         atAlias = atAlias.toPigSymbolPrimitive(),
        //         byAlias = byAlias.toPigSymbolPrimitive(),
        //         metas
        //     )
        // }
        //
        // override fun visitTableCrossJoin(ctx: GeneratedParser.TableCrossJoinContext) = build {
        //     val lhs = visit(ctx.lhs, FromSource::class)
        //     val joinType = visitJoinType(ctx.joinType())
        //     val rhs = visit(ctx.rhs, FromSource::class)
        //     val metas = metaContainerOf(IsImplictJoinMeta.instance) + joinType.metas
        //     join(joinType, lhs, rhs, metas = metas)
        // }
        //
        // override fun visitTableQualifiedJoin(ctx: GeneratedParser.TableQualifiedJoinContext) = build {
        //     val lhs = visit(ctx.lhs, FromSource::class)
        //     val joinType = visitJoinType(ctx.joinType())
        //     val rhs = visit(ctx.rhs, FromSource::class)
        //     val condition = visitOrNull(ctx.joinSpec(), Expr::class)
        //     join(joinType, lhs, rhs, condition, metas = joinType.metas)
        // }
        //
        // override fun visitTableBaseRefSymbol(ctx: GeneratedParser.TableBaseRefSymbolContext) = build {
        //     val expr = visit(ctx.source, Expr::class)
        //     val name = visitOrNull(ctx.symbolPrimitive(), Expr.Id::class)
        //     scan_(expr, name.toPigSymbolPrimitive(), metas = expr.metas)
        // }
        //
        // override fun visitFromClauseSimpleImplicit(ctx: GeneratedParser.FromClauseSimpleImplicitContext) = build {
        //     val path = visitPathSimple(ctx.pathSimple())
        //     val name = visitOrNull(ctx.symbolPrimitive(), Expr.Id::class)?.name
        //     scan_(path, name, metas = path.metas)
        // }
        //
        // override fun visitTableWrapped(ctx: GeneratedParser.TableWrappedContext): AstNode = visit(ctx.tableReference())
        //
        // override fun visitJoinSpec(ctx: GeneratedParser.JoinSpecContext) = visitExpr(ctx.expr())
        //
        // override fun visitJoinType(ctx: GeneratedParser.JoinTypeContext?) = build {
        //     if (ctx == null) return@build inner()
        //     val metas = ctx.mod.getSourceMetaContainer()
        //     when (ctx.mod.type) {
        //         GeneratedParser.LEFT -> left(metas)
        //         GeneratedParser.RIGHT -> right(metas)
        //         GeneratedParser.INNER -> inner(metas)
        //         GeneratedParser.FULL -> full(metas)
        //         GeneratedParser.OUTER -> full(metas)
        //         else -> inner(metas)
        //     }
        // }
        //
        // override fun visitJoinRhsTableJoined(ctx: GeneratedParser.JoinRhsTableJoinedContext) =
        //     visit(ctx.tableReference(), FromSource::class)
        //
        // /**
        //  * SIMPLE EXPRESSIONS
        //  */
        //
        // override fun visitOr(ctx: GeneratedParser.OrContext) =
        //     visitBinaryOperation(ctx.lhs, ctx.rhs, ctx.OR().symbol, null)
        //
        // override fun visitAnd(ctx: GeneratedParser.AndContext) = visitBinaryOperation(ctx.lhs, ctx.rhs, ctx.op, null)
        //
        // override fun visitNot(ctx: GeneratedParser.NotContext) = visitUnaryOperation(ctx.rhs, ctx.op, null)
        //
        // override fun visitMathOp00(ctx: GeneratedParser.MathOp00Context): AstNode =
        //     visitBinaryOperation(ctx.lhs, ctx.rhs, ctx.op, ctx.parent)
        //
        // override fun visitMathOp01(ctx: GeneratedParser.MathOp01Context): AstNode =
        //     visitBinaryOperation(ctx.lhs, ctx.rhs, ctx.op, ctx.parent)
        //
        // override fun visitMathOp02(ctx: GeneratedParser.MathOp02Context): AstNode =
        //     visitBinaryOperation(ctx.lhs, ctx.rhs, ctx.op, ctx.parent)
        //
        // override fun visitValueExpr(ctx: GeneratedParser.ValueExprContext) =
        //     visitUnaryOperation(ctx.rhs, ctx.sign, ctx.parent)
        //
        // /**
        //  *
        //  * PREDICATES
        //  *
        //  */
        //
        // override fun visitPredicateComparison(ctx: GeneratedParser.PredicateComparisonContext) =
        //     visitBinaryOperation(ctx.lhs, ctx.rhs, ctx.op)
        //
        // /**
        //  * Note: This predicate can take a wrapped expression on the RHS, and it will wrap it in a LIST. However, if the
        //  * expression is a SELECT or VALUES expression, it will NOT wrap it in a list. This is per SqlParser.
        //  */
        // override fun visitPredicateIn(ctx: GeneratedParser.PredicateInContext) = build {
        //     // Wrap Expression with LIST unless SELECT / VALUES
        //     val rhs = if (ctx.expr() != null) {
        //         val possibleRhs = visitExpr(ctx.expr())
        //         if (possibleRhs is Expr.Select || possibleRhs.metas.containsKey(IsValuesExprMeta.TAG))
        //             possibleRhs
        //         else list(possibleRhs, metas = possibleRhs.metas + metaContainerOf(IsListParenthesizedMeta))
        //     } else {
        //         visit(ctx.rhs, Expr::class)
        //     }
        //     val lhs = visit(ctx.lhs, Expr::class)
        //     val args = listOf(lhs, rhs)
        //     val inCollection = inCollection(args, ctx.IN().getSourceMetaContainer())
        //     if (ctx.NOT() == null) return@build inCollection
        //     not(inCollection, ctx.NOT().getSourceMetaContainer() + metaContainerOf(LegacyLogicalNotMeta.instance))
        // }
        //
        // override fun visitPredicateIs(ctx: GeneratedParser.PredicateIsContext) = build {
        //     val lhs = visit(ctx.lhs, Expr::class)
        //     val rhs = visit(ctx.type(), Type::class)
        //     val isType = isType(lhs, rhs, ctx.IS().getSourceMetaContainer())
        //     if (ctx.NOT() == null) return@build isType
        //     not(isType, ctx.NOT().getSourceMetaContainer() + metaContainerOf(LegacyLogicalNotMeta.instance))
        // }
        //
        // override fun visitPredicateBetween(ctx: GeneratedParser.PredicateBetweenContext) = build {
        //     val args = visitOrEmpty(listOf(ctx.lhs, ctx.lower, ctx.upper), Expr::class)
        //     val between = between(args[0], args[1], args[2], ctx.BETWEEN().getSourceMetaContainer())
        //     if (ctx.NOT() == null) return@build between
        //     not(between, ctx.NOT().getSourceMetaContainer() + metaContainerOf(LegacyLogicalNotMeta.instance))
        // }
        //
        // override fun visitPredicateLike(ctx: GeneratedParser.PredicateLikeContext) = build {
        //     val args = visitOrEmpty(listOf(ctx.lhs, ctx.rhs), Expr::class)
        //     val escape = visitOrNull(ctx.escape, Expr::class)
        //     val like = like(args[0], args[1], escape, ctx.LIKE().getSourceMetaContainer())
        //     if (ctx.NOT() == null) return@build like
        //     not(like, metas = ctx.NOT().getSourceMetaContainer() + metaContainerOf(LegacyLogicalNotMeta.instance))
        // }
        //
        // /**
        //  *
        //  * PRIMARY EXPRESSIONS
        //  *
        //  */
        //
        // override fun visitExprTermWrappedQuery(ctx: GeneratedParser.ExprTermWrappedQueryContext) =
        //     visit(ctx.expr(), Expr::class)
        //
        // override fun visitVarRefExpr(ctx: GeneratedParser.VarRefExprContext): AstNode = build {
        //     val metas = ctx.ident.getSourceMetaContainer()
        //     val qualifier = if (ctx.qualifier == null) unqualified() else localsFirst()
        //     val sensitivity = if (ctx.ident.type == GeneratedParser.IDENTIFIER) caseInsensitive() else caseSensitive()
        //     Debug.id(ctx.ident.getStringValue(), sensitivity, qualifier, metas)
        // }
        //
        // override fun visitParameter(ctx: GeneratedParser.ParameterContext) = build {
        //     val parameterIndex = parameters[ctx.QUESTION_MARK().symbol.tokenIndex]
        //         ?: throw PartiQLParserException("Unable to find index of parameter.", ErrorCode.PARSE_INVALID_QUERY)
        //     parameter(parameterIndex.toLong(), ctx.QUESTION_MARK().getSourceMetaContainer())
        // }
        //
        // override fun visitSequenceConstructor(ctx: GeneratedParser.SequenceConstructorContext) = build {
        //     val expressions = visitOrEmpty(ctx.expr(), Expr::class)
        //     val metas = ctx.datatype.getSourceMetaContainer()
        //     when (ctx.datatype.type) {
        //         GeneratedParser.LIST -> list(expressions, metas)
        //         GeneratedParser.SEXP -> sexp(expressions, metas)
        //         else -> throw PartiQLParserException("Unknown sequence", ErrorCode.PARSE_INVALID_QUERY)
        //     }
        // }
        //
        // override fun visitExprPrimaryPath(ctx: GeneratedParser.ExprPrimaryPathContext) = build {
        //     val base = visit(ctx.exprPrimary()) as Expr
        //     val steps = ctx.pathStep().map { step -> visit(step) as PathStep }
        //     path(base, steps, base.metas)
        // }
        //
        // override fun visitPathStepIndexExpr(ctx: GeneratedParser.PathStepIndexExprContext) = build {
        //     val expr = visit(ctx.key, Expr::class)
        //     val metas = expr.metas + metaContainerOf(IsPathIndexMeta.instance)
        //     pathExpr(expr, CaseSensitivity.CaseSensitive(), metas)
        // }
        //
        // override fun visitPathStepDotExpr(ctx: GeneratedParser.PathStepDotExprContext) = getSymbolPathExpr(ctx.key)
        //
        // override fun visitPathStepIndexAll(ctx: GeneratedParser.PathStepIndexAllContext) = build {
        //     pathWildcard(metas = ctx.ASTERISK().getSourceMetaContainer())
        // }
        //
        // override fun visitPathStepDotAll(ctx: GeneratedParser.PathStepDotAllContext) = build {
        //     pathUnpivot()
        // }
        //
        // override fun visitExprGraphMatchMany(ctx: GeneratedParser.ExprGraphMatchManyContext) = build {
        //     val graph = visit(ctx.exprPrimary()) as Expr
        //     val gpmlPattern = visitGpmlPatternList(ctx.gpmlPatternList())
        //     graphMatch(graph, gpmlPattern, graph.metas)
        // }
        //
        // override fun visitExprGraphMatchOne(ctx: GeneratedParser.ExprGraphMatchOneContext) = build {
        //     val graph = visit(ctx.exprPrimary()) as Expr
        //     val gpmlPattern = visitGpmlPattern(ctx.gpmlPattern())
        //     graphMatch(graph, gpmlPattern, graph.metas)
        // }
        //
        // override fun visitValues(ctx: GeneratedParser.ValuesContext) = build {
        //     val rows = visitOrEmpty(ctx.valueRow(), Expr.List::class)
        //     bag(rows, ctx.VALUES().getSourceMetaContainer() + metaContainerOf(IsValuesExprMeta.instance))
        // }
        //
        // override fun visitValueRow(ctx: GeneratedParser.ValueRowContext) = build {
        //     val expressions = visitOrEmpty(ctx.expr(), Expr::class)
        //     list(
        //         expressions,
        //         metas = ctx.PAREN_LEFT().getSourceMetaContainer() + metaContainerOf(IsListParenthesizedMeta)
        //     )
        // }
        //
        // override fun visitValueList(ctx: GeneratedParser.ValueListContext) = build {
        //     val expressions = visitOrEmpty(ctx.expr(), Expr::class)
        //     list(
        //         expressions,
        //         metas = ctx.PAREN_LEFT().getSourceMetaContainer() + metaContainerOf(IsListParenthesizedMeta)
        //     )
        // }
        //
        // /**
        //  *
        //  * FUNCTIONS
        //  *
        //  */
        //
        // override fun visitNullIf(ctx: GeneratedParser.NullIfContext) = build {
        //     val lhs = visitExpr(ctx.expr(0))
        //     val rhs = visitExpr(ctx.expr(1))
        //     val metas = ctx.NULLIF().getSourceMetaContainer()
        //     nullIf(lhs, rhs, metas)
        // }
        //
        // override fun visitCoalesce(ctx: GeneratedParser.CoalesceContext) = build {
        //     val expressions = visitOrEmpty(ctx.expr(), Expr::class)
        //     val metas = ctx.COALESCE().getSourceMetaContainer()
        //     coalesce(expressions, metas)
        // }
        //
        // override fun visitCaseExpr(ctx: GeneratedParser.CaseExprContext) = build {
        //     val pairs = ctx.whens.indices.map { i ->
        //         exprPair(visitExpr(ctx.whens[i]), visitExpr(ctx.thens[i]))
        //     }
        //     val elseExpr = visitOrNull(ctx.else_, Expr::class)
        //     val caseMeta = ctx.CASE().getSourceMetaContainer()
        //     when (ctx.case_) {
        //         null -> searchedCase(exprPairList(pairs), elseExpr, metas = caseMeta)
        //         else -> simpleCase(visitExpr(ctx.case_), exprPairList(pairs), elseExpr, metas = caseMeta)
        //     }
        // }
        //
        // override fun visitCast(ctx: GeneratedParser.CastContext) = build {
        //     val expr = visitExpr(ctx.expr())
        //     val type = visit(ctx.type(), Type::class)
        //     val metas = ctx.CAST().getSourceMetaContainer()
        //     cast(expr, type, metas)
        // }
        //
        // override fun visitCanCast(ctx: GeneratedParser.CanCastContext) = build {
        //     val expr = visitExpr(ctx.expr())
        //     val type = visit(ctx.type(), Type::class)
        //     val metas = ctx.CAN_CAST().getSourceMetaContainer()
        //     canCast(expr, type, metas)
        // }
        //
        // override fun visitCanLosslessCast(ctx: GeneratedParser.CanLosslessCastContext) = build {
        //     val expr = visitExpr(ctx.expr())
        //     val type = visit(ctx.type(), Type::class)
        //     val metas = ctx.CAN_LOSSLESS_CAST().getSourceMetaContainer()
        //     canLosslessCast(expr, type, metas)
        // }
        //
        // override fun visitFunctionCallIdent(ctx: GeneratedParser.FunctionCallIdentContext) = build {
        //     val name = ctx.name.getString().toLowerCase()
        //     val args = visitOrEmpty(ctx.expr(), Expr::class)
        //     val metas = ctx.name.getSourceMetaContainer()
        //     call(name, args = args, metas = metas)
        // }
        //
        // override fun visitFunctionCallReserved(ctx: GeneratedParser.FunctionCallReservedContext) = build {
        //     val name = ctx.name.text.toLowerCase()
        //     val args = visitOrEmpty(ctx.expr(), Expr::class)
        //     val metas = ctx.name.getSourceMetaContainer()
        //     call(name, args = args, metas = metas)
        // }
        //
        // override fun visitDateFunction(ctx: GeneratedParser.DateFunctionContext) = build {
        //     if (!DATE_TIME_PART_KEYWORDS.contains(ctx.dt.text.toLowerCase())) {
        //         throw ctx.dt.err("Expected one of: $DATE_TIME_PART_KEYWORDS", ErrorCode.PARSE_EXPECTED_DATE_TIME_PART)
        //     }
        //     val datetimePart = lit(ionSymbol(ctx.dt.text))
        //     val secondaryArgs = visitOrEmpty(ctx.expr(), Expr::class)
        //     val args = listOf(datetimePart) + secondaryArgs
        //     val metas = ctx.func.getSourceMetaContainer()
        //     call(ctx.func.text.toLowerCase(), args, metas)
        // }
        //
        // override fun visitSubstring(ctx: GeneratedParser.SubstringContext) = build {
        //     val args = visitOrEmpty(ctx.expr(), Expr::class)
        //     val metas = ctx.SUBSTRING().getSourceMetaContainer()
        //     call(ctx.SUBSTRING().text.toLowerCase(), args, metas)
        // }
        //
        // override fun visitPosition(ctx: GeneratedParser.PositionContext) = build {
        //     val args = visitOrEmpty(ctx.expr(), Expr::class)
        //     val metas = ctx.POSITION().getSourceMetaContainer()
        //     call(ctx.POSITION().text.toLowerCase(), args, metas)
        // }
        //
        // override fun visitOverlay(ctx: GeneratedParser.OverlayContext) = build {
        //     val args = visitOrEmpty(ctx.expr(), Expr::class)
        //     val metas = ctx.OVERLAY().getSourceMetaContainer()
        //     call(ctx.OVERLAY().text.toLowerCase(), args, metas)
        // }
        //
        // override fun visitCountAll(ctx: GeneratedParser.CountAllContext) = build {
        //     callAgg(
        //         all(),
        //         ctx.func.text.toLowerCase(),
        //         lit(ionInt(1)),
        //         ctx.COUNT().getSourceMetaContainer() + metaContainerOf(IsCountStarMeta.instance)
        //     )
        // }
        //
        // override fun visitExtract(ctx: GeneratedParser.ExtractContext) = build {
        //     if (!DATE_TIME_PART_KEYWORDS.contains(ctx.IDENTIFIER().text.toLowerCase())) {
        //         throw ctx.IDENTIFIER()
        //             .err("Expected one of: $DATE_TIME_PART_KEYWORDS", ErrorCode.PARSE_EXPECTED_DATE_TIME_PART)
        //     }
        //     val datetimePart = lit(ionSymbol(ctx.IDENTIFIER().text))
        //     val timeExpr = visit(ctx.rhs, Expr::class)
        //     val args = listOf(datetimePart, timeExpr)
        //     val metas = ctx.EXTRACT().getSourceMetaContainer()
        //     call(ctx.EXTRACT().text.toLowerCase(), args, metas)
        // }
        //
        // /**
        //  * Note: This implementation is odd because the TRIM function contains keywords that are not keywords outside
        //  * of TRIM. Therefore, TRIM(<spec> <substring> FROM <target>) needs to be parsed as below. The <spec> needs to be
        //  * an identifier (according to SqlParser), but if the identifier is NOT a trim specification, and the <substring> is
        //  * null, we need to make the substring equal to the <spec> (and make <spec> null).
        //  */
        // override fun visitTrimFunction(ctx: GeneratedParser.TrimFunctionContext) = build {
        //     val possibleModText = if (ctx.mod != null) ctx.mod.text.toLowerCase() else null
        //     val isTrimSpec = TRIM_SPECIFICATION_KEYWORDS.contains(possibleModText)
        //     val (modifier, substring) = when {
        //         // if <spec> is not null and <substring> is null
        //         // then there are two possible cases trim(( BOTH | LEADING | TRAILING ) FROM <target> )
        //         // or trim(<substring> FROM target), i.e., we treat what is recognized by parser as the modifier as <substring>
        //         ctx.mod != null && ctx.sub == null -> {
        //             if (isTrimSpec) ctx.mod.toSymbol() to null
        //             else null to Debug.id(
        //                 possibleModText!!,
        //                 caseInsensitive(),
        //                 unqualified(),
        //                 ctx.mod.getSourceMetaContainer()
        //             )
        //         }
        //         ctx.mod == null && ctx.sub != null -> {
        //             null to visitExpr(ctx.sub)
        //         }
        //         ctx.mod != null && ctx.sub != null -> {
        //             if (isTrimSpec) ctx.mod.toSymbol() to visitExpr(ctx.sub)
        //             // todo we need to decide if it should be an evaluator error or a parser error
        //             else {
        //                 val errorContext = PropertyValueMap()
        //                 errorContext[Property.TOKEN_STRING] = ctx.mod.text
        //                 throw ctx.mod.err(
        //                     "'${ctx.mod.text}' is an unknown trim specification, valid values: $TRIM_SPECIFICATION_KEYWORDS",
        //                     ErrorCode.PARSE_INVALID_TRIM_SPEC,
        //                     errorContext
        //                 )
        //             }
        //         }
        //         else -> null to null
        //     }
        //
        //     val target = visitExpr(ctx.target)
        //     val args = listOfNotNull(modifier, substring, target)
        //     val metas = ctx.func.getSourceMetaContainer()
        //     call(ctx.func.text.toLowerCase(), args, metas)
        // }
        //
        // override fun visitAggregateBase(ctx: GeneratedParser.AggregateBaseContext) = build {
        //     val strategy = getStrategy(ctx.setQuantifierStrategy(), default = all())
        //     val arg = visitExpr(ctx.expr())
        //     val metas = ctx.func.getSourceMetaContainer()
        //     callAgg(strategy, ctx.func.text.toLowerCase(), arg, metas)
        // }
        //
        // /**
        //  *
        //  * Window Functions
        //  * TODO: Remove from experimental once https://github.com/partiql/partiql-docs/issues/31 is resolved and a RFC is approved
        //  *
        //  */
        //
        // override fun visitLagLeadFunction(ctx: GeneratedParser.LagLeadFunctionContext) = build {
        //     val args = visitOrEmpty(ctx.expr(), Expr::class)
        //     val over = visitOver(ctx.over())
        //     // LAG and LEAD will require a Window ORDER BY
        //     if (over.orderBy == null) {
        //         val errorContext = PropertyValueMap()
        //         errorContext[Property.TOKEN_STRING] = ctx.func.text.toLowerCase()
        //         throw ctx.func.err(
        //             "${ctx.func.text} requires Window ORDER BY",
        //             ErrorCode.PARSE_EXPECTED_WINDOW_ORDER_BY,
        //             errorContext
        //         )
        //     }
        //     val metas = ctx.func.getSourceMetaContainer()
        //     callWindow(ctx.func.text.toLowerCase(), over, args, metas)
        // }
        //
        // override fun visitOver(ctx: GeneratedParser.OverContext) = build {
        //     val windowPartitionList =
        //         if (ctx.windowPartitionList() != null) visitWindowPartitionList(ctx.windowPartitionList()) else null
        //     val windowSortSpecList =
        //         if (ctx.windowSortSpecList() != null) visitWindowSortSpecList(ctx.windowSortSpecList()) else null
        //     val metas = ctx.OVER().getSourceMetaContainer()
        //     over(windowPartitionList, windowSortSpecList, metas)
        // }
        //
        // override fun visitWindowPartitionList(ctx: GeneratedParser.WindowPartitionListContext) = build {
        //     val args = visitOrEmpty(ctx.expr(), Expr::class)
        //     val metas = ctx.PARTITION().getSourceMetaContainer()
        //     windowPartitionList(args, metas)
        // }
        //
        // override fun visitWindowSortSpecList(ctx: GeneratedParser.WindowSortSpecListContext) = build {
        //     val sortSpecList = visitOrEmpty(ctx.orderSortSpec(), SortSpec::class)
        //     val metas = ctx.ORDER().getSourceMetaContainer()
        //     windowSortSpecList(sortSpecList, metas)
        // }
        //
        // /**
        //  *
        //  * LITERALS
        //  *
        //  */
        //
        // override fun visitBag(ctx: GeneratedParser.BagContext) = build {
        //     val exprList = visitOrEmpty(ctx.expr(), Expr::class)
        //     bag(exprList, ctx.ANGLE_DOUBLE_LEFT().getSourceMetaContainer())
        // }
        //
        // override fun visitLiteralDecimal(ctx: GeneratedParser.LiteralDecimalContext) = build {
        //     val decimal = try {
        //         ionDecimal(Decimal.valueOf(bigDecimalOf(ctx.LITERAL_DECIMAL().text)))
        //     } catch (e: NumberFormatException) {
        //         val errorContext = PropertyValueMap()
        //         errorContext[Property.TOKEN_STRING] = ctx.LITERAL_DECIMAL().text
        //         throw ctx.LITERAL_DECIMAL()
        //             .err("Invalid decimal literal", ErrorCode.LEXER_INVALID_LITERAL, errorContext)
        //     }
        //     lit(
        //         decimal,
        //         ctx.LITERAL_DECIMAL().getSourceMetaContainer()
        //     )
        // }
        //
        // override fun visitArray(ctx: GeneratedParser.ArrayContext) = build {
        //     val metas = ctx.BRACKET_LEFT().getSourceMetaContainer()
        //     list(visitOrEmpty(ctx.expr(), Expr::class), metas)
        // }
        //
        // override fun visitLiteralNull(ctx: GeneratedParser.LiteralNullContext) = build {
        //     lit(ionNull(), ctx.NULL().getSourceMetaContainer())
        // }
        //
        // override fun visitLiteralMissing(ctx: GeneratedParser.LiteralMissingContext) = build {
        //     missing(ctx.MISSING().getSourceMetaContainer())
        // }
        //
        // override fun visitLiteralTrue(ctx: GeneratedParser.LiteralTrueContext) = build {
        //     lit(ionBool(true), ctx.TRUE().getSourceMetaContainer())
        // }
        //
        // override fun visitLiteralFalse(ctx: GeneratedParser.LiteralFalseContext) = build {
        //     lit(ionBool(false), ctx.FALSE().getSourceMetaContainer())
        // }
        //
        // override fun visitLiteralIon(ctx: GeneratedParser.LiteralIonContext) = build {
        //     val ionValue = try {
        //         loadSingleElement(ctx.ION_CLOSURE().getStringValue())
        //     } catch (e: IonElementException) {
        //         throw PartiQLParserException("Unable to parse Ion value.", ErrorCode.PARSE_UNEXPECTED_TOKEN, cause = e)
        //     }
        //     lit(
        //         ionValue,
        //         ctx.ION_CLOSURE().getSourceMetaContainer() + metaContainerOf(IsIonLiteralMeta.instance)
        //     )
        // }
        //
        // override fun visitLiteralString(ctx: GeneratedParser.LiteralStringContext) = build {
        //     lit(ionString(ctx.LITERAL_STRING().getStringValue()), ctx.LITERAL_STRING().getSourceMetaContainer())
        // }
        //
        // override fun visitLiteralInteger(ctx: GeneratedParser.LiteralIntegerContext): Expr.Lit = build {
        //     lit(parseToIntElement(ctx.LITERAL_INTEGER().text), ctx.LITERAL_INTEGER().getSourceMetaContainer())
        // }
        //
        // override fun visitLiteralDate(ctx: GeneratedParser.LiteralDateContext) = build {
        //     val dateString = ctx.LITERAL_STRING().getStringValue()
        //     if (DATE_PATTERN_REGEX.matches(dateString).not()) {
        //         throw ctx.LITERAL_STRING()
        //             .err("Expected DATE string to be of the format yyyy-MM-dd", ErrorCode.PARSE_INVALID_DATE_STRING)
        //     }
        //     try {
        //         LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
        //         val (year, month, day) = dateString.split("-")
        //         date(year.toLong(), month.toLong(), day.toLong(), ctx.DATE().getSourceMetaContainer())
        //     } catch (e: DateTimeParseException) {
        //         throw ctx.LITERAL_STRING().err(e.localizedMessage, ErrorCode.PARSE_INVALID_DATE_STRING, cause = e)
        //     } catch (e: IndexOutOfBoundsException) {
        //         throw ctx.LITERAL_STRING().err(e.localizedMessage, ErrorCode.PARSE_INVALID_DATE_STRING, cause = e)
        //     }
        // }
        //
        // override fun visitLiteralTime(ctx: GeneratedParser.LiteralTimeContext) = build {
        //     val (timeString, precision) = getTimeStringAndPrecision(ctx.LITERAL_STRING(), ctx.LITERAL_INTEGER())
        //     when (ctx.WITH()) {
        //         null -> getLocalTime(timeString, false, precision, ctx.LITERAL_STRING(), ctx.TIME(0))
        //         else -> getOffsetTime(timeString, precision, ctx.LITERAL_STRING(), ctx.TIME(0))
        //     }
        // }
        //
        // override fun visitTuple(ctx: GeneratedParser.TupleContext) = build {
        //     val pairs = visitOrEmpty(ctx.pair(), ExprPair::class)
        //     val metas = ctx.BRACE_LEFT().getSourceMetaContainer()
        //     struct(pairs, metas)
        // }
        //
        // override fun visitPair(ctx: GeneratedParser.PairContext) = build {
        //     val lhs = visitExpr(ctx.lhs)
        //     val rhs = visitExpr(ctx.rhs)
        //     exprPair(lhs, rhs)
        // }
        //
        // /**
        //  *
        //  * TYPES
        //  *
        //  */
        //
        // override fun visitTypeAtomic(ctx: GeneratedParser.TypeAtomicContext) = build {
        //     val metas = ctx.datatype.getSourceMetaContainer()
        //     when (ctx.datatype.type) {
        //         GeneratedParser.NULL -> nullType(metas)
        //         GeneratedParser.BOOL -> booleanType(metas)
        //         GeneratedParser.BOOLEAN -> booleanType(metas)
        //         GeneratedParser.SMALLINT -> smallintType(metas)
        //         GeneratedParser.INT2 -> smallintType(metas)
        //         GeneratedParser.INTEGER2 -> smallintType(metas)
        //         GeneratedParser.INT -> integerType(metas)
        //         GeneratedParser.INTEGER -> integerType(metas)
        //         GeneratedParser.INT4 -> integer4Type(metas)
        //         GeneratedParser.INTEGER4 -> integer4Type(metas)
        //         GeneratedParser.INT8 -> integer8Type(metas)
        //         GeneratedParser.INTEGER8 -> integer8Type(metas)
        //         GeneratedParser.BIGINT -> integer8Type(metas)
        //         GeneratedParser.REAL -> realType(metas)
        //         GeneratedParser.DOUBLE -> doublePrecisionType(metas)
        //         GeneratedParser.TIMESTAMP -> timestampType(metas)
        //         GeneratedParser.CHAR -> characterType(metas = metas)
        //         GeneratedParser.CHARACTER -> characterType(metas = metas)
        //         GeneratedParser.MISSING -> missingType(metas)
        //         GeneratedParser.STRING -> stringType(metas)
        //         GeneratedParser.SYMBOL -> symbolType(metas)
        //         GeneratedParser.BLOB -> blobType(metas)
        //         GeneratedParser.CLOB -> clobType(metas)
        //         GeneratedParser.DATE -> dateType(metas)
        //         GeneratedParser.STRUCT -> structType(metas)
        //         GeneratedParser.TUPLE -> tupleType(metas)
        //         GeneratedParser.LIST -> listType(metas)
        //         GeneratedParser.BAG -> bagType(metas)
        //         GeneratedParser.SEXP -> sexpType(metas)
        //         GeneratedParser.ANY -> anyType(metas)
        //         else -> throw PartiQLParserException("Unsupported type.", ErrorCode.PARSE_INVALID_QUERY)
        //     }
        // }
        //
        // override fun visitTypeVarChar(ctx: GeneratedParser.TypeVarCharContext) = build {
        //     val arg0 = if (ctx.arg0 != null) parseToIntElement(ctx.arg0.text) else null
        //     val metas = ctx.CHARACTER().getSourceMetaContainer()
        //     assertIntegerElement(ctx.arg0, arg0)
        //     characterVaryingType(arg0?.longValue, metas)
        // }
        //
        // override fun visitTypeArgSingle(ctx: GeneratedParser.TypeArgSingleContext) = build {
        //     val arg0 = if (ctx.arg0 != null) parseToIntElement(ctx.arg0.text) else null
        //     assertIntegerElement(ctx.arg0, arg0)
        //     val metas = ctx.datatype.getSourceMetaContainer()
        //     when (ctx.datatype.type) {
        //         GeneratedParser.FLOAT -> floatType(arg0?.longValue, metas)
        //         GeneratedParser.CHAR, GeneratedParser.CHARACTER -> characterType(arg0?.longValue, metas)
        //         GeneratedParser.VARCHAR -> characterVaryingType(arg0?.longValue, metas)
        //         else -> throw PartiQLParserException(
        //             "Unknown datatype",
        //             ErrorCode.PARSE_UNEXPECTED_TOKEN,
        //             PropertyValueMap()
        //         )
        //     }
        // }
        //
        // override fun visitTypeArgDouble(ctx: GeneratedParser.TypeArgDoubleContext) = build {
        //     val arg0 = if (ctx.arg0 != null) parseToIntElement(ctx.arg0.text) else null
        //     val arg1 = if (ctx.arg1 != null) parseToIntElement(ctx.arg1.text) else null
        //     assertIntegerElement(ctx.arg0, arg0)
        //     assertIntegerElement(ctx.arg1, arg1)
        //     val metas = ctx.datatype.getSourceMetaContainer()
        //     when (ctx.datatype.type) {
        //         GeneratedParser.DECIMAL, GeneratedParser.DEC -> decimalType(arg0?.longValue, arg1?.longValue, metas)
        //         GeneratedParser.NUMERIC -> numericType(arg0?.longValue, arg1?.longValue, metas)
        //         else -> throw PartiQLParserException(
        //             "Unknown datatype",
        //             ErrorCode.PARSE_UNEXPECTED_TOKEN,
        //             PropertyValueMap()
        //         )
        //     }
        // }
        //
        // override fun visitTypeTimeZone(ctx: GeneratedParser.TypeTimeZoneContext) = build {
        //     val precision = if (ctx.precision != null) ctx.precision.text.toInteger().toLong() else null
        //     if (precision != null && (precision < 0 || precision > MAX_PRECISION_FOR_TIME)) {
        //         throw ctx.precision.err("Unsupported precision", ErrorCode.PARSE_INVALID_PRECISION_FOR_TIME)
        //     }
        //     if (ctx.WITH() == null) return@build timeType(precision)
        //     timeWithTimeZoneType(precision)
        // }
        //
        // override fun visitTypeCustom(ctx: GeneratedParser.TypeCustomContext) = build {
        //     val metas = ctx.symbolPrimitive().getSourceMetaContainer()
        //     val customName: String = when (val name = ctx.symbolPrimitive().getString().toLowerCase()) {
        //         in customKeywords -> name
        //         in customTypeAliases.keys -> customTypeAliases.getOrDefault(name, name)
        //         else -> throw PartiQLParserException("Invalid custom type name: $name", ErrorCode.PARSE_INVALID_QUERY)
        //     }
        //     customType_(SymbolPrimitive(customName, metas), metas)
        // }
        //
        // /**
        //  * NOT OVERRIDDEN
        //  * Explicitly defining the override helps by showing the user (via the IDE) which methods remain to be overridden.
        //  */
        //
        // override fun visitTerminal(node: TerminalNode?): AstNode = super.visitTerminal(node)
        // override fun shouldVisitNextChild(node: RuleNode?, currentResult: AstNode?) =
        //     super.shouldVisitNextChild(node, currentResult)
        //
        // override fun visitErrorNode(node: ErrorNode?): AstNode = super.visitErrorNode(node)
        // override fun visitChildren(node: RuleNode?): AstNode = super.visitChildren(node)
        // override fun visitExprPrimaryBase(ctx: GeneratedParser.ExprPrimaryBaseContext?): AstNode =
        //     super.visitExprPrimaryBase(ctx)
        //
        // override fun visitExprTermBase(ctx: GeneratedParser.ExprTermBaseContext?): AstNode =
        //     super.visitExprTermBase(ctx)
        //
        // override fun visitCollection(ctx: GeneratedParser.CollectionContext?): AstNode = super.visitCollection(ctx)
        // override fun visitPredicateBase(ctx: GeneratedParser.PredicateBaseContext?): AstNode =
        //     super.visitPredicateBase(ctx)
        //
        // override fun visitTableNonJoin(ctx: GeneratedParser.TableNonJoinContext?): AstNode =
        //     super.visitTableNonJoin(ctx)
        //
        // override fun visitTableRefBase(ctx: GeneratedParser.TableRefBaseContext?): AstNode =
        //     super.visitTableRefBase(ctx)
        //
        // override fun visitJoinRhsBase(ctx: GeneratedParser.JoinRhsBaseContext?): AstNode = super.visitJoinRhsBase(ctx)
        // override fun visitConflictTarget(ctx: GeneratedParser.ConflictTargetContext?): AstNode =
        //     super.visitConflictTarget(ctx)
        //
        /**
         *
         * HELPER METHODS
         *
         */

        private fun <T : AstNode> visitOrEmpty(ctx: List<ParserRuleContext>?, clazz: KClass<T>): List<T> = when {
            ctx.isNullOrEmpty() -> emptyList()
            else -> ctx.map { clazz.cast(visit(it)) }
        }

        private fun <T : AstNode> visitNullableItems(ctx: List<ParserRuleContext>?, clazz: KClass<T>): List<T?> = when {
            ctx.isNullOrEmpty() -> emptyList()
            else -> ctx.map { visitOrNull(it, clazz) }
        }

        private fun <T : AstNode> visitOrNull(ctx: ParserRuleContext?, clazz: KClass<T>): T? = when (ctx) {
            null -> null
            else -> clazz.cast(visit(ctx))
        }

        private fun <T : AstNode> visit(ctx: ParserRuleContext, clazz: KClass<T>): T = clazz.cast(visit(ctx))

        /**
         * Visiting a symbol to get a string, skip the wrapping, unwrapping, and location tracking.
         */
        fun visitRawSymbol(ctx: GeneratedParser.SymbolPrimitiveContext) = when (ctx.ident.type) {
            GeneratedParser.IDENTIFIER_QUOTED -> ctx.IDENTIFIER_QUOTED().getStringValue()
            GeneratedParser.IDENTIFIER -> ctx.IDENTIFIER().getStringValue()
            else -> throw PartiQLParserException("Invalid symbol reference.")
        }

        private fun visitBinaryOperation(
            lhs: ParserRuleContext?,
            rhs: ParserRuleContext?,
            op: Token?,
            parent: ParserRuleContext? = null
        ) = build {
            if (parent != null) return@build visit(parent, Expr::class)
            val args = visitOrEmpty(listOf(lhs!!, rhs!!), Expr::class)
            val metas = op.getSourceMetaContainer()
            when (op!!.type) {
                GeneratedParser.AND -> and(args, metas)
                GeneratedParser.OR -> or(args, metas)
                GeneratedParser.ASTERISK -> times(args, metas)
                GeneratedParser.SLASH_FORWARD -> divide(args, metas)
                GeneratedParser.PLUS -> plus(args, metas)
                GeneratedParser.MINUS -> minus(args, metas)
                GeneratedParser.PERCENT -> modulo(args, metas)
                GeneratedParser.CONCAT -> concat(args, metas)
                GeneratedParser.ANGLE_LEFT -> lt(args, metas)
                GeneratedParser.LT_EQ -> lte(args, metas)
                GeneratedParser.ANGLE_RIGHT -> gt(args, metas)
                GeneratedParser.GT_EQ -> gte(args, metas)
                GeneratedParser.NEQ -> ne(args, metas)
                GeneratedParser.EQ -> eq(args, metas)
                else -> throw PartiQLParserException("Unknown binary operator", ErrorCode.PARSE_INVALID_QUERY)
            }
        }

        private fun visitUnaryOperation(operand: ParserRuleContext?, op: Token?, parent: ParserRuleContext? = null) =
            build {
                if (parent != null) return@build visit(parent, Expr::class)
                val arg = visit(operand!!, Expr::class)
                val metas = op.getSourceMetaContainer()
                when (op!!.type) {
                    GeneratedParser.PLUS -> {
                        when {
                            arg !is Expr.Lit -> pos(arg, metas)
                            arg.value is IntElement -> arg
                            arg.value is FloatElement -> arg
                            arg.value is DecimalElement -> arg
                            else -> pos(arg, metas)
                        }
                    }
                    GeneratedParser.MINUS -> {
                        when {
                            arg !is Expr.Lit -> neg(arg, metas)
                            arg.value is IntElement -> {
                                val intValue = when (arg.value.integerSize) {
                                    IntElementSize.LONG -> ionInt(-arg.value.longValue)
                                    IntElementSize.BIG_INTEGER -> when (arg.value.bigIntegerValue) {
                                        Long.MAX_VALUE.toBigInteger() + (1L).toBigInteger() -> ionInt(Long.MIN_VALUE)
                                        else -> ionInt(arg.value.bigIntegerValue * BigInteger.valueOf(-1L))
                                    }
                                }
                                arg.copy(value = intValue.asAnyElement())
                            }
                            arg.value is FloatElement -> arg.copy(value = ionFloat(-(arg.value.doubleValue)).asAnyElement())
                            arg.value is DecimalElement -> arg.copy(value = ionDecimal(-(arg.value.decimalValue)).asAnyElement())
                            else -> neg(arg, metas)
                        }
                    }
                    GeneratedParser.NOT -> not(arg, metas)
                    else -> throw PartiQLParserException("Unknown unary operator", ErrorCode.PARSE_INVALID_QUERY)
                }
            }

        private fun GeneratedParser.SymbolPrimitiveContext.getSourceMetaContainer() = when (this.ident.type) {
            GeneratedParser.IDENTIFIER -> this.IDENTIFIER().getSourceMetaContainer()
            GeneratedParser.IDENTIFIER_QUOTED -> this.IDENTIFIER_QUOTED().getSourceMetaContainer()
            else -> throw PartiQLParserException(
                "Unable to get identifier's source meta-container.",
                ErrorCode.PARSE_INVALID_QUERY
            )
        }

        /**
         * With the <string> and <int> nodes of a literal time expression, returns the parsed string and precision.
         * TIME (<int>)? (WITH TIME ZONE)? <string>
         */
        private fun getTimeStringAndPrecision(
            stringNode: TerminalNode,
            integerNode: TerminalNode?
        ): Pair<String, Long> {
            val timeString = stringNode.getStringValue()
            val precision = when (integerNode) {
                null -> try {
                    getPrecisionFromTimeString(timeString).toLong()
                } catch (e: EvaluationException) {
                    throw stringNode.err(
                        "Unable to parse precision.", ErrorCode.PARSE_INVALID_TIME_STRING,
                        cause = e
                    )
                }
                else -> integerNode.text.toInteger().toLong()
            }
            if (precision < 0 || precision > MAX_PRECISION_FOR_TIME) {
                throw integerNode.err("Precision out of bounds", ErrorCode.PARSE_INVALID_PRECISION_FOR_TIME)
            }
            return timeString to precision
        }

        /**
         * Parses a [timeString] using [OffsetTime] and converts to a [Expr.LitTime]. If unable to parse, parses
         * using [getLocalTime].
         */
        private fun getOffsetTime(
            timeString: String,
            precision: Long,
            stringNode: TerminalNode,
            timeNode: TerminalNode
        ) = build {
            try {
                val time: OffsetTime = OffsetTime.parse(timeString)
                litTime(
                    timeValue(
                        time.hour.toLong(), time.minute.toLong(), time.second.toLong(), time.nano.toLong(),
                        precision, true, (time.offset.totalSeconds / 60).toLong()
                    )
                )
            } catch (e: DateTimeParseException) {
                getLocalTime(timeString, true, precision, stringNode, timeNode)
            }
        }

        /**
         * Parses a [timeString] using [LocalTime] and converts to a [Expr.LitTime]
         */
        private fun getLocalTime(
            timeString: String,
            withTimeZone: Boolean,
            precision: Long,
            stringNode: TerminalNode,
            timeNode: TerminalNode
        ) = build {
            val time: LocalTime
            val formatter = when (withTimeZone) {
                false -> DateTimeFormatter.ISO_TIME
                else -> DateTimeFormatter.ISO_LOCAL_TIME
            }
            try {
                time = LocalTime.parse(timeString, formatter)
            } catch (e: DateTimeParseException) {
                throw stringNode.err("Unable to parse time", ErrorCode.PARSE_INVALID_TIME_STRING, cause = e)
            }
            litTime(
                timeValue(
                    time.hour.toLong(), time.minute.toLong(), time.second.toLong(),
                    time.nano.toLong(), precision, withTimeZone, null,
                    stringNode.getSourceMetaContainer()
                ),
                timeNode.getSourceMetaContainer()
            )
        }

        private fun convertProjectionItems(ctx: GeneratedParser.ProjectionItemsContext, metas: MetaContainer) = build {
            val projections = visitOrEmpty(ctx.projectionItem(), ProjectItem::class)
            projectList(projections, metas)
        }

        private fun GeneratedParser.SelectClauseContext.getMetas(): MetaContainer = when (this) {
            is GeneratedParser.SelectAllContext -> this.SELECT().getSourceMetaContainer()
            is GeneratedParser.SelectItemsContext -> this.SELECT().getSourceMetaContainer()
            is GeneratedParser.SelectValueContext -> this.SELECT().getSourceMetaContainer()
            is GeneratedParser.SelectPivotContext -> this.PIVOT().getSourceMetaContainer()
            else -> throw PartiQLParserException("Unknown meta location.", ErrorCode.PARSE_INVALID_QUERY)
        }

        /**
         * Converts a Path expression into a Projection Item (either ALL or EXPR). Note: A Projection Item only allows a
         * subset of a typical Path expressions. See the following examples.
         *
         * Examples of valid projections are:
         *
         * ```sql
         *      SELECT * FROM foo
         *      SELECT foo.* FROM foo
         *      SELECT f.* FROM foo as f
         *      SELECT foo.bar.* FROM foo
         *      SELECT f.bar.* FROM foo as f
         * ```
         * Also validates that the expression is valid for select list context. It does this by making
         * sure that expressions looking like the following do not appear:
         *
         * ```sql
         *      SELECT foo[*] FROM foo
         *      SELECT f.*.bar FROM foo as f
         *      SELECT foo[1].* FROM foo
         *      SELECT foo.*.bar FROM foo
         * ```
         */
        protected fun convertPathToProjectionItem(path: Expr.Path, alias: SymbolPrimitive?) = build {
            val steps = mutableListOf<PathStep>()
            var containsIndex = false
            path.steps.forEachIndexed { index, step ->

                // Only last step can have a '.*'
                if (step is PathStep.PathUnpivot && index != path.steps.lastIndex) {
                    throw PartiQLParserException(
                        "Projection item cannot unpivot unless at end.",
                        ErrorCode.PARSE_INVALID_QUERY
                    )
                }

                // No step can have an indexed wildcard: '[*]'
                if (step is PathStep.PathWildcard) {
                    throw PartiQLParserException(
                        "Projection item cannot index using wildcard.",
                        ErrorCode.PARSE_INVALID_QUERY
                    )
                }

                // If the last step is '.*', no indexing is allowed
                if (step.metas.containsKey(IsPathIndexMeta.TAG)) {
                    containsIndex = true
                }

                if (step !is PathStep.PathUnpivot) {
                    steps.add(step)
                }
            }

            if (path.steps.last() is PathStep.PathUnpivot && containsIndex) {
                throw PartiQLParserException(
                    "Projection item use wildcard with any indexing.",
                    ErrorCode.PARSE_INVALID_QUERY
                )
            }

            when {
                path.steps.last() is PathStep.PathUnpivot && steps.isEmpty() -> projectAll(path.root, path.metas)
                path.steps.last() is PathStep.PathUnpivot -> projectAll(path(path.root, steps, path.metas), path.metas)
                else -> projectExpr_(path, asAlias = alias, path.metas)
            }
        }

        private fun TerminalNode.getStringValue(): String = this.symbol.getStringValue()

        private fun Token.getStringValue(): String = when (this.type) {
            GeneratedParser.IDENTIFIER -> this.text
            GeneratedParser.IDENTIFIER_QUOTED -> this.text.removePrefix("\"").removeSuffix("\"").replace("\"\"", "\"")
            GeneratedParser.LITERAL_STRING -> this.text.removePrefix("'").removeSuffix("'").replace("''", "'")
            GeneratedParser.ION_CLOSURE -> this.text.removePrefix("`").removeSuffix("`")
            else -> throw this.err("Unsupported token for grabbing string value.", ErrorCode.PARSE_INVALID_QUERY)
        }

        private fun getStrategy(strategy: GeneratedParser.SetQuantifierStrategyContext?, default: SetQuantifier) =
            build {
                when {
                    strategy == null -> default
                    strategy.DISTINCT() != null -> distinct()
                    strategy.ALL() != null -> all()
                    else -> default
                }
            }

        private fun getStrategy(strategy: GeneratedParser.SetQuantifierStrategyContext?): SetQuantifier? {
            return when {
                strategy == null -> null
                strategy.DISTINCT() != null -> build { distinct() }
                else -> null
            }
        }

        private fun getSetQuantifierStrategy(ctx: GeneratedParser.SelectClauseContext): SetQuantifier? {
            return when (ctx) {
                is GeneratedParser.SelectAllContext -> getStrategy(ctx.setQuantifierStrategy())
                is GeneratedParser.SelectItemsContext -> getStrategy(ctx.setQuantifierStrategy())
                is GeneratedParser.SelectValueContext -> getStrategy(ctx.setQuantifierStrategy())
                is GeneratedParser.SelectPivotContext -> null
                else -> null
            }
        }

        private fun GeneratedParser.SymbolPrimitiveContext.getString(): String {
            return when {
                this.IDENTIFIER_QUOTED() != null -> this.IDENTIFIER_QUOTED().getStringValue()
                this.IDENTIFIER() != null -> this.IDENTIFIER().text
                else -> throw PartiQLParserException("Unable to get symbol's text.", ErrorCode.PARSE_INVALID_QUERY)
            }
        }

        private fun getSymbolPathExpr(ctx: GeneratedParser.SymbolPrimitiveContext) = build {
            when {
                ctx.IDENTIFIER_QUOTED() != null -> pathExpr(
                    lit(ionString(ctx.IDENTIFIER_QUOTED().getStringValue())), caseSensitive(),
                    metas = ctx.IDENTIFIER_QUOTED().getSourceMetaContainer()
                )
                ctx.IDENTIFIER() != null -> pathExpr(
                    lit(ionString(ctx.IDENTIFIER().text)), caseInsensitive(),
                    metas = ctx.IDENTIFIER().getSourceMetaContainer()
                )
                else -> throw PartiQLParserException("Unable to get symbol's text.", ErrorCode.PARSE_INVALID_QUERY)
            }
        }

        private fun String.toInteger() = BigInteger(this, 10)

        private fun Token.toSymbol(): Expr.Lit {
            val str = this.text
            val metas = this.getSourceMetaContainer()
            return build {
                lit(ionSymbol(str), metas)
            }
        }

        private fun parseToIntElement(text: String): IntElement =
            try {
                ionInt(text.toLong())
            } catch (e: NumberFormatException) {
                ionInt(text.toBigInteger())
            }

        private fun assertIntegerElement(token: Token?, value: IonElement?) {
            if (value == null)
                return
            if (value !is IntElement)
                throw token.err("Expected an integer value.", ErrorCode.PARSE_MALFORMED_PARSE_TREE)
            if (value.integerSize == IntElementSize.BIG_INTEGER || value.longValue > Int.MAX_VALUE || value.longValue < Int.MIN_VALUE)
                throw token.err(
                    "Type parameter exceeded maximum value",
                    ErrorCode.PARSE_TYPE_PARAMETER_EXCEEDED_MAXIMUM_VALUE
                )
        }

        private enum class ExplainParameters {
            TYPE,
            FORMAT;

            fun getCompliantString(target: String?, input: Token): String = when (target) {
                null -> input.text!!
                else -> throw input.error(
                    "Cannot set EXPLAIN parameter ${this.name} multiple times.",
                    ErrorCode.PARSE_UNEXPECTED_TOKEN
                )
            }
        }

        private fun TerminalNode?.err(
            msg: String,
            code: ErrorCode,
            ctx: PropertyValueMap = PropertyValueMap(),
            cause: Throwable? = null
        ) = this.error(msg, code, ctx, cause)

        private fun Token?.err(
            msg: String,
            code: ErrorCode,
            ctx: PropertyValueMap = PropertyValueMap(),
            cause: Throwable? = null
        ) = this.error(msg, code, ctx, cause)
    }
}

/**
 * Returns the corresponding [IonElement] for a particular ANTLR Token
 */
internal fun Token.asIonElement(): IonElement = when {
    type == GeneratedParser.EOF -> ionSymbol("EOF")
    // ALL_OPERATORS.contains(text.toLowerCase()) -> ionSymbol(text.toLowerCase())
    type == GeneratedParser.ION_CLOSURE -> loadSingleElement(text.trimStart('`').trimEnd('`'))
    type == GeneratedParser.TRUE -> ionBool(true)
    type == GeneratedParser.FALSE -> ionBool(false)
    type == GeneratedParser.NULL -> ionNull()
    type == GeneratedParser.NULLS -> ionSymbol("nulls")
    type == GeneratedParser.MISSING -> ionNull()
    type == GeneratedParser.LITERAL_STRING -> ionString(text.trim('\'').replace("''", "'"))
    type == GeneratedParser.LITERAL_INTEGER -> ionInt(BigInteger(text, 10))
    type == GeneratedParser.LITERAL_DECIMAL -> try {
        ionDecimal(Decimal.valueOf(text))
    } catch (e: NumberFormatException) {
        throw token.error(e.localizedMessage, ErrorCode.PARSE_EXPECTED_NUMBER, cause = e)
    }
    type == GeneratedParser.IDENTIFIER_QUOTED -> ionSymbol(text.trim('\"').replace("\"\"", "\""))
    // MULTI_LEXEME_TOKEN_MAP.containsKey(text.toLowerCase().split("\\s+".toRegex())) -> {
    //     val pair = MULTI_LEXEME_TOKEN_MAP[text.toLowerCase().split("\\s+".toRegex())]!!
    //     ionSymbol(pair.first)
    // }
    // KEYWORDS.contains(text.toLowerCase()) -> ionSymbol(TYPE_ALIASES[text.toLowerCase()] ?: text.toLowerCase())
    else -> ionSymbol(text)
}

internal fun Int.getAntlrDisplayString(): String = GeneratedParser.VOCABULARY.getSymbolicName(this)

internal fun error(
    token: Token,
    message: String,
    cause: Throwable? = null,
    context: MutableMap<String, Any> = mutableMapOf(),
): PartiQLParserException {
    context["line_no"] = token.line.toLong()
    context["column_no"] = token.charPositionInLine.toLong() + 1
    context["token_description"] = token.type.getAntlrDisplayString()
    context["token_value"] = token.asIonElement()
    return PartiQLParserException(message, cause, context)
}
