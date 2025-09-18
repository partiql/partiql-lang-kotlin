package examples

import org.partiql.ast.AstNode
import org.partiql.ast.DatetimeField
import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.Identifier
import org.partiql.ast.Select
import org.partiql.ast.Statement
import org.partiql.ast.exprBinary
import org.partiql.ast.exprLit
import org.partiql.ast.exprSFW
import org.partiql.ast.exprVar
import org.partiql.ast.fromValue
import org.partiql.ast.identifierSymbol
import org.partiql.ast.selectProject
import org.partiql.ast.selectProjectItemExpression
import org.partiql.ast.statementQuery
import org.partiql.ast.util.AstRewriter
import org.partiql.ast.visitor.AstBaseVisitor
import org.partiql.parser.PartiQLParser
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.stringValue
import kotlin.test.Test
import kotlin.test.assertEquals

class ASTChanges {
    // Simple helper for these examples to parse a single, valid query
    fun parseSingleQuery(query: String): AstNode {
        return PartiQLParser.default().parse(query).root
    }

    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun `modeling of select-from-where queries using classes`() {
        // In 0.14.9, SFW queries among other nodes have a different modeling.
        // As an example, this is how the query `SELECT a FROM tbl WHERE d = 'foo'`
        // is modeled.
        val ast = Statement.Query(
            expr = Expr.SFW(
                select = Select.Project(
                    items = listOf(
                        Select.Project.Item.Expression(
                            expr = Expr.Var(
                                identifier = Identifier.Symbol(
                                    symbol = "a",
                                    caseSensitivity = Identifier.CaseSensitivity.INSENSITIVE
                                ),
                                scope = Expr.Var.Scope.DEFAULT
                            ),
                            asAlias = null
                        )
                    ),
                    setq = null
                ),
                exclude = null,
                from = From.Value(
                    expr = Expr.Var(
                        identifier = Identifier.Symbol(
                            symbol = "tbl",
                            caseSensitivity = Identifier.CaseSensitivity.INSENSITIVE
                        ),
                        scope = Expr.Var.Scope.DEFAULT
                    ),
                    type = From.Value.Type.SCAN,
                    asAlias = null,
                    atAlias = null,
                    byAlias = null
                ),
                let = null,
                where = Expr.Binary(
                    op = Expr.Binary.Op.EQ,
                    lhs = Expr.Var(
                        identifier = Identifier.Symbol(
                            symbol = "d",
                            caseSensitivity = Identifier.CaseSensitivity.INSENSITIVE
                        ),
                        scope = Expr.Var.Scope.DEFAULT
                    ),
                    rhs = Expr.Lit(
                        stringValue("foo")
                    )
                ),
                groupBy = null,
                having = null,
                setOp = null,
                orderBy = null,
                limit = null,
                offset = null
            )
        )
        val parsedQuery = parseSingleQuery("SELECT a FROM tbl WHERE d = 'foo'")
        assertEquals(ast, parsedQuery)
    }

    @OptIn(PartiQLValueExperimental::class)
    @Test
    fun `use factory methods`() {
        // Here we represent the same query as above using factory methods which are a bit less verbose
        // than the default constructors.
        val ast = statementQuery(
            expr = exprSFW(
                select = selectProject(
                    items = listOf(
                        selectProjectItemExpression(
                            expr = exprVar(
                                identifier = identifierSymbol(
                                    symbol = "a",
                                    caseSensitivity = Identifier.CaseSensitivity.INSENSITIVE
                                ),
                                scope = Expr.Var.Scope.DEFAULT
                            ),
                            asAlias = null
                        )
                    ),
                    setq = null
                ),
                exclude = null,
                from = fromValue(
                    expr = exprVar(
                        identifier = Identifier.Symbol(
                            symbol = "tbl",
                            caseSensitivity = Identifier.CaseSensitivity.INSENSITIVE
                        ),
                        scope = Expr.Var.Scope.DEFAULT
                    ),
                    type = From.Value.Type.SCAN,
                    asAlias = null,
                    atAlias = null,
                    byAlias = null
                ),
                let = null,
                where = exprBinary(
                    op = Expr.Binary.Op.EQ,
                    lhs = exprVar(
                        identifier = identifierSymbol(
                            symbol = "d",
                            caseSensitivity = Identifier.CaseSensitivity.INSENSITIVE
                        ),
                        scope = Expr.Var.Scope.DEFAULT
                    ),
                    rhs = exprLit(
                        stringValue("foo")
                    )
                ),
                groupBy = null,
                having = null,
                setOp = null,
                orderBy = null,
                limit = null,
                offset = null
            )
        )
        val parsedQuery = parseSingleQuery("SELECT a FROM tbl WHERE d = 'foo'")
        assertEquals(ast, parsedQuery)
    }

    @Test
    fun `casing on sealed classes`() {
        // v0.14.9 makes use of many sealed classes/interfaces like enums.
        // While convenient for not needing an `else` branch, this makes it hard to add
        // new variants in a backwards compatible manner.
        val datetimeField: DatetimeField = DatetimeField.DAY
        val v = when (datetimeField) {
            DatetimeField.YEAR -> null
            DatetimeField.MONTH -> null
            DatetimeField.DAY -> 42
            DatetimeField.HOUR -> null
            DatetimeField.MINUTE -> null
            DatetimeField.SECOND -> null
            DatetimeField.TIMEZONE_HOUR -> null
            DatetimeField.TIMEZONE_MINUTE -> null
        }
        assertEquals(v, 42)
    }

    @Test
    fun `AST visitor`() {
        // Simple example showing how to define a visitor in PLK 0.14 to count up how many literal
        // expressions are in a given AST.
        val countExprLitVisitor = object : AstBaseVisitor<Int, Unit>() {
            var counter = 0

            override fun defaultReturn(node: AstNode, ctx: Unit): Int {
                return counter
            }

            override fun visitExprLit(node: Expr.Lit, ctx: Unit): Int {
                counter++
                return super.visitExprLit(node, ctx)
            }
        }
        val ast = parseSingleQuery("1 + 2 + 3")
        val numLits = countExprLitVisitor.visit(ast, Unit)
        assertEquals(numLits, 3)
    }

    @Test
    fun `AST rewriter`() {
        // A simple rewriter to demonstrate the `AstRewriter` API.
        // This rewriter adds an alias to any variable references in the projection that was missing an explicit
        // alias.
        val projectionAliasRewriter = object : AstRewriter<Unit>() {
            // Infers the alias based on the last referenced identifier in the `Identifier`
            private fun lastPart(id: Identifier): Identifier.Symbol {
                return when (id) {
                    is Identifier.Symbol -> return id
                    is Identifier.Qualified -> if (id.steps.isEmpty()) {
                        id.root
                    } else {
                        id.steps.last()
                    }
                }
            }

            // Apply this rewrite to just project items
            override fun visitSelectProjectItem(node: Select.Project.Item, ctx: Unit): AstNode {
                val newNode = if (node is Select.Project.Item.Expression && node.asAlias == null) {
                    when (val expr = node.expr) {
                        is Expr.Var -> node.copy(asAlias = lastPart(expr.identifier))
                        else -> node
                    }
                } else {
                    node
                }
                return super.visitSelectProjectItem(newNode, ctx)
            }
        }
        val ast = parseSingleQuery("SELECT a, b FROM tbl")
        val astWithExplicitAliases = parseSingleQuery("SELECT a AS a, b AS b FROM tbl")
        // AST with the explicit aliases should be the same as after performing the rewrite
        assertEquals(astWithExplicitAliases, projectionAliasRewriter.visit(ast, Unit))
    }
}
