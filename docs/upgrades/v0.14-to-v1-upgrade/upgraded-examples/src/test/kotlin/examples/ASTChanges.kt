package examples

import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.Ast.exprQuerySet
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.Ast.from
import org.partiql.ast.Ast.fromExpr
import org.partiql.ast.Ast.query
import org.partiql.ast.Ast.queryBodySFW
import org.partiql.ast.Ast.selectItemExpr
import org.partiql.ast.Ast.selectList
import org.partiql.ast.AstNode
import org.partiql.ast.AstVisitor
import org.partiql.ast.DatetimeField
import org.partiql.ast.From
import org.partiql.ast.FromExpr
import org.partiql.ast.FromType
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.Query
import org.partiql.ast.QueryBody
import org.partiql.ast.SelectItem
import org.partiql.ast.SelectList
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprOperator
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprVarRef
import org.partiql.parser.PartiQLParser
import kotlin.test.Test
import kotlin.test.assertEquals

class ASTChanges {
    @Test
    fun `modeling of select-from-where queries using classes`() {
        // v1 modeling is a bit different from the modeling in v0.14.
        // As an example, this is how the query `SELECT a FROM tbl WHERE d = 'foo'`
        // is modeled.
        // SFW queries are wrapped in a new Expr node called `ExprQuerySet`.
        val ast = Query(
            /* expr = */ ExprQuerySet(
                /* body = */ QueryBody.SFW(
                    /* select = */ SelectList(
                        /* items = */ listOf(
                            SelectItem.Expr(
                                /* expr = */ ExprVarRef(
                                    // v1 introduces some convenience methods for identifier construction
                                    /* identifier = */ Identifier.regular("a",),
                                    /* qualified = */ false
                                ),
                                /* asAlias = */ null
                            )
                        ),
                        /* setq = */ null
                    ),
                    /* exclude = */ null,
                    /* from = */ From(
                        /* tableRefs = */ listOf(
                            FromExpr(
                                /* expr = */ ExprVarRef(
                                    /* identifier = */ Identifier.regular("tbl"),
                                    /* qualified = */ false
                                ),
                                /* fromType = */ FromType.SCAN(),
                                /* asAlias = */ null,
                                /* atAlias = */ null
                            ),
                        )
                    ),
                    /* let = */ null,
                    /* where = */ ExprOperator(
                        /* symbol = */ "=",
                        /* lhs = */ ExprVarRef(
                            /* identifier = */ Identifier.regular("d",),
                            /* qualified = */ false
                        ),
                        /* rhs = */ ExprLit(
                            // Literals are also modeled differently than in v0.14, which had eagerly
                            // typed literal values as distinct variants. The v1 representation allows for
                            // lazy typing and validation.
                            /* lit = */ Literal.string("foo")
                        )
                    ),
                    /* groupBy = */ null,
                    /* having = */ null
                ),
                /* orderBy = */ null,
                /* limit = */ null,
                /* offset = */ null
            )
        )
        val parsedQuery = PartiQLParser.standard().parse("SELECT a FROM tbl WHERE d = 'foo'").statements.first()
        assertEquals(ast, parsedQuery)
    }

    @Test
    fun `use factory methods`() {
        // Here we represent the same query as above using factory methods which are a bit less verbose
        // than the default constructors.
        // In v1, we can omit many of the nullable fields, which will be set to `null`.
        val ast = query(
            expr = exprQuerySet(
                body = queryBodySFW(
                    select = selectList(
                        items = listOf(
                            selectItemExpr(
                                expr = exprVarRef(
                                    identifier = Identifier.regular("a",),
                                    isQualified = false
                                ),
                            )
                        ),
                    ),
                    from = from(
                        tableRefs = listOf(
                            fromExpr(
                                expr = exprVarRef(
                                    identifier = Identifier.regular("tbl"),
                                    isQualified = false
                                ),
                                fromType = FromType.SCAN()
                            ),
                        )
                    ),
                    where = exprOperator(
                        symbol = "=",
                        lhs = exprVarRef(
                            identifier = Identifier.regular("d",),
                            isQualified = false
                        ),
                        rhs = exprLit(
                            Literal.string("foo")
                        )
                    )
                )
            )
        )
        val parsedQuery = PartiQLParser.standard().parse("SELECT a FROM tbl WHERE d = 'foo'").statements.first()
        assertEquals(ast, parsedQuery)
    }

    @Test
    fun `casing on sealed classes`() {
        // v1 defines many of the sealed classes and interfaces using `AstEnum`, which share a lot of similarities
        // with typical Java/Kotlin enums. However, a user is forced to add an `else` branch to ensure backwards-
        // compatibility as new features are added.
        val datetimeField: DatetimeField = DatetimeField.DAY()
        val v = when (datetimeField.code()) {
            DatetimeField.YEAR -> null
            DatetimeField.MONTH -> null
            DatetimeField.DAY -> 42
            DatetimeField.HOUR -> null
            DatetimeField.MINUTE -> null
            DatetimeField.SECOND -> null
            DatetimeField.TIMEZONE_HOUR -> null
            DatetimeField.TIMEZONE_MINUTE -> null
            else -> null // Must include an `else` branch in v1
        }
        assertEquals(v, 42)
    }

    @Test
    fun `AST visitor`() {
        // Simple example showing how to define a visitor in v1 to count up how many literal
        // expressions are in a given AST.
        val countExprLitVisitor = object : AstVisitor<Int, Unit>() {
            var counter = 0

            override fun defaultReturn(node: AstNode, ctx: Unit): Int {
                return counter
            }

            override fun visitExprLit(node: ExprLit, ctx: Unit): Int {
                counter++
                return super.visitExprLit(node, ctx)
            }
        }
        val ast = PartiQLParser.standard().parse("1 + 2 + 3").statements.first()
        val numLits = countExprLitVisitor.visit(ast, Unit)
        assertEquals(numLits, 3)
    }
}
