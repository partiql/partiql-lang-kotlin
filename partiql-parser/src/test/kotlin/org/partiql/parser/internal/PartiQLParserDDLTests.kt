package org.partiql.parser.internal

import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.ast.Constraint
import org.partiql.ast.DdlOp
import org.partiql.ast.Expr
import org.partiql.ast.Identifier
import org.partiql.ast.Type
import org.partiql.ast.constraint
import org.partiql.ast.constraintDefinitionCheck
import org.partiql.ast.constraintDefinitionNotNull
import org.partiql.ast.constraintDefinitionUnique
import org.partiql.ast.ddlOpCreateTable
import org.partiql.ast.ddlOpDropTable
import org.partiql.ast.exprBinary
import org.partiql.ast.exprLit
import org.partiql.ast.exprVar
import org.partiql.ast.identifierQualified
import org.partiql.ast.identifierSymbol
import org.partiql.ast.statementDDL
import org.partiql.ast.tableDefinition
import org.partiql.ast.tableDefinitionAttribute
import org.partiql.parser.PartiQLParserException
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.int32Value
import java.util.stream.Stream
import kotlin.test.assertEquals

class PartiQLParserDDLTests {

    private val parser = PartiQLParserDefault()

    data class SuccessTestCase(
        val description: String? = null,
        val query: String,
        val expectedOp: DdlOp
    )

    data class ErrorTestCase(
        val description: String? = null,
        val query: String,
    )

    @ArgumentsSource(SuccessTestProvider::class)
    @ParameterizedTest
    fun successTests(tc: SuccessTestCase) = assertExpression(tc.query, tc.expectedOp)

    @ArgumentsSource(ErrorTestProvider::class)
    @ParameterizedTest
    fun errorTests(tc: ErrorTestCase) = assertIssue(tc.query)

    class SuccessTestProvider : ArgumentsProvider {
        @OptIn(PartiQLValueExperimental::class)
        val createTableTests = listOf(
            //
            // Qualified Identifier as Table Name
            //

            SuccessTestCase(
                "CREATE TABLE with unqualified case insensitive name",
                "CREATE TABLE foo",
                ddlOpCreateTable(
                    identifierSymbol("foo", Identifier.CaseSensitivity.INSENSITIVE),
                    null,
                )
            ),
            // Support Case Sensitive identifier as table name
            // Subsequent process may need to change
            // See: https://www.db-fiddle.com/f/9A8mknSNYuRGLfkqkLeiHD/0 for reference.
            SuccessTestCase(
                "CREATE TABLE with unqualified case sensitive name",
                "CREATE TABLE \"foo\"",
                ddlOpCreateTable(
                    identifierSymbol("foo", Identifier.CaseSensitivity.SENSITIVE),
                    null
                )
            ),
            SuccessTestCase(
                "CREATE TABLE with qualified case insensitive name",
                "CREATE TABLE myCatalog.mySchema.foo",
                ddlOpCreateTable(
                    identifierQualified(
                        identifierSymbol("myCatalog", Identifier.CaseSensitivity.INSENSITIVE),
                        listOf(
                            identifierSymbol("mySchema", Identifier.CaseSensitivity.INSENSITIVE),
                            identifierSymbol("foo", Identifier.CaseSensitivity.INSENSITIVE),
                        )
                    ),
                    null
                )
            ),
            SuccessTestCase(
                "CREATE TABLE with qualified name with mixed case sensitivity",
                "CREATE TABLE myCatalog.\"mySchema\".foo",
                ddlOpCreateTable(
                    identifierQualified(
                        identifierSymbol("myCatalog", Identifier.CaseSensitivity.INSENSITIVE),
                        listOf(
                            identifierSymbol("mySchema", Identifier.CaseSensitivity.SENSITIVE),
                            identifierSymbol("foo", Identifier.CaseSensitivity.INSENSITIVE),
                        )
                    ),
                    null
                )
            ),

            //
            // Column Constraints
            //
            SuccessTestCase(
                "CREATE TABLE with Column NOT NULL Constraint",
                """
                    CREATE TABLE tbl (
                        a INT2 NOT NULL
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
                                Type.Int2(),
                                listOf(constraint(null, constraintDefinitionNotNull())),
                            )
                        ),
                        emptyList()
                    )
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with Column Unique Constraint",
                """
                    CREATE TABLE tbl (
                        a INT2 UNIQUE
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
                                Type.Int2(),
                                listOf(constraint(null, constraintDefinitionUnique(null, false))),
                            )
                        ),
                        emptyList()
                    ),
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with Column Primary Key Constraint",
                """
                    CREATE TABLE tbl (
                        a INT2 PRIMARY KEY
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
                                Type.Int2(),
                                listOf(constraint(null, constraintDefinitionUnique(null, true))),
                            )
                        ),
                        emptyList()
                    ),
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with Column CHECK Constraint",
                """
                    CREATE TABLE tbl (
                        a INT2 CHECK (a > 0)
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
                                Type.Int2(),
                                listOf(
                                    constraint(
                                        null,
                                        constraintDefinitionCheck(
                                            exprBinary(
                                                Expr.Binary.Op.GT,
                                                exprVar(identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE), Expr.Var.Scope.DEFAULT),
                                                exprLit(int32Value(0))
                                            )
                                        )
                                    )
                                ),
                            )
                        ),
                        emptyList()
                    ),
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with Table Unique Constraint",
                """
                    CREATE TABLE tbl (
                        UNIQUE (a, b)
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
                    tableDefinition(
                        emptyList(),
                        listOf(
                            constraint(
                                null,
                                constraintDefinitionUnique(
                                    listOf(
                                        identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
                                        identifierSymbol("b", Identifier.CaseSensitivity.INSENSITIVE),
                                    ),
                                    false
                                )
                            )
                        )
                    ),
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with Table Primary Key Constraint",
                """
                    CREATE TABLE tbl (
                        PRIMARY KEY (a, b)
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
                    tableDefinition(
                        emptyList(),
                        listOf(
                            constraint(
                                null,
                                constraintDefinitionUnique(
                                    listOf(
                                        identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
                                        identifierSymbol("b", Identifier.CaseSensitivity.INSENSITIVE),
                                    ),
                                    true
                                )
                            )
                        )
                    ),
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with Table CHECK Constraint",
                """
                    CREATE TABLE tbl (
                        CHECK (a > 0)
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
                    tableDefinition(
                        emptyList(),
                        listOf(
                            constraint(
                                null,
                                constraintDefinitionCheck(
                                    exprBinary(
                                        Expr.Binary.Op.GT,
                                        exprVar(identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE), Expr.Var.Scope.DEFAULT),
                                        exprLit(int32Value(0))
                                    )
                                )
                            )
                        )
                    ),
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with CASE SENSITIVE Identifier as column name",
                """
                    CREATE TABLE tbl (
                        "a" INT2
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                identifierSymbol("a", Identifier.CaseSensitivity.SENSITIVE),
                                Type.Int2(),
                                emptyList(),
                            )
                        ),
                        emptyList()
                    ),
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with STRUCT",
                """
                    CREATE TABLE tbl (
                        a STRUCT<
                           b: INT2,
                           c: INT2 NOT NULL
                        >
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
                                Type.Struct(
                                    listOf(
                                        Type.Struct.Field(
                                            identifierSymbol("b", Identifier.CaseSensitivity.INSENSITIVE),
                                            Type.Int2(),
                                            emptyList()
                                        ),
                                        Type.Struct.Field(
                                            identifierSymbol("c", Identifier.CaseSensitivity.INSENSITIVE),
                                            Type.Int2(),
                                            listOf(Constraint(null, Constraint.Definition.NotNull()))
                                        )
                                    )
                                ),
                                emptyList(),
                            )
                        ),
                        emptyList()
                    ),
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with STRUCT of complex",
                """
                    CREATE TABLE tbl (
                        a STRUCT<
                           b: STRUCT <c: INT2>,
                           d: ARRAY<INT2>
                        >
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
                                Type.Struct(
                                    listOf(
                                        Type.Struct.Field(
                                            identifierSymbol("b", Identifier.CaseSensitivity.INSENSITIVE),
                                            Type.Struct(
                                                listOf(
                                                    Type.Struct.Field(
                                                        identifierSymbol("c", Identifier.CaseSensitivity.INSENSITIVE),
                                                        Type.Int2(),
                                                        emptyList()
                                                    ),
                                                )
                                            ),
                                            emptyList()
                                        ),
                                        Type.Struct.Field(
                                            identifierSymbol("d", Identifier.CaseSensitivity.INSENSITIVE),
                                            Type.Array(Type.Int2()),
                                            emptyList()
                                        )
                                    )
                                ),
                                emptyList(),
                            )
                        ),
                        emptyList()
                    ),
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with empty",
                """
                    CREATE TABLE tbl (
                        a STRUCT
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
                                Type.Struct(
                                    emptyList()
                                ),
                                emptyList(),
                            )
                        ),
                        emptyList()
                    ),
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with ARRAY",
                """
                    CREATE TABLE tbl (
                        a ARRAY<INT2>
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
                                Type.Array(Type.Int2()),
                                emptyList(),
                            )
                        ),
                        emptyList()
                    ),
                )
            ),
            SuccessTestCase(
                "CREATE TABLE with ARRAY of Struct",
                """
                    CREATE TABLE tbl (
                        a ARRAY< STRUCT< b:INT2 > >
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
                                Type.Array(
                                    Type.Struct(
                                        listOf(
                                            Type.Struct.Field(
                                                identifierSymbol("b", Identifier.CaseSensitivity.INSENSITIVE),
                                                Type.Int2(),
                                                emptyList()
                                            ),
                                        )
                                    ),
                                ),
                                emptyList(),
                            )
                        ),
                        emptyList()
                    ),
                )
            ),
            SuccessTestCase(
                "CREATE TABLE no space between angle right",
                """
                    CREATE TABLE tbl(
                        a LIST<STRUCT<b:INT2>>
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
                                Type.Array(
                                    Type.Struct(
                                        listOf(
                                            Type.Struct.Field(
                                                identifierSymbol("b", Identifier.CaseSensitivity.INSENSITIVE),
                                                Type.Int2(),
                                                emptyList()
                                            ),
                                        )
                                    ),
                                ),
                                emptyList(),
                            )
                        ),
                        emptyList()
                    ),
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with LIST without element type",
                """
                    CREATE TABLE tbl (
                        a ARRAY
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
                                Type.Array(null),
                                emptyList(),
                            )
                        ),
                        emptyList()
                    ),
                )
            ),
        )

        val dropTableTests = listOf(
            SuccessTestCase(
                "DROP TABLE with unqualified case insensitive name",
                "DROP TABLE foo",
                ddlOpDropTable(
                    identifierSymbol("foo", Identifier.CaseSensitivity.INSENSITIVE),
                )
            ),
            SuccessTestCase(
                "DROP TABLE with unqualified case sensitive name",
                "DROP TABLE \"foo\"",
                ddlOpDropTable(
                    identifierSymbol("foo", Identifier.CaseSensitivity.SENSITIVE),
                )
            ),
            SuccessTestCase(
                "DROP TABLE with qualified case insensitive name",
                "DROP TABLE myCatalog.mySchema.foo",
                ddlOpDropTable(
                    identifierQualified(
                        identifierSymbol("myCatalog", Identifier.CaseSensitivity.INSENSITIVE),
                        listOf(
                            identifierSymbol("mySchema", Identifier.CaseSensitivity.INSENSITIVE),
                            identifierSymbol("foo", Identifier.CaseSensitivity.INSENSITIVE),
                        )
                    ),
                )
            ),
            SuccessTestCase(
                "DROP TABLE with qualified name with mixed case sensitivity",
                "DROP TABLE myCatalog.\"mySchema\".foo",
                ddlOpDropTable(
                    identifierQualified(
                        identifierSymbol("myCatalog", Identifier.CaseSensitivity.INSENSITIVE),
                        listOf(
                            identifierSymbol("mySchema", Identifier.CaseSensitivity.SENSITIVE),
                            identifierSymbol("foo", Identifier.CaseSensitivity.INSENSITIVE),
                        )
                    ),
                )
            ),
        )

        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> =
            (createTableTests + dropTableTests).map { Arguments.of(it) }.stream()
    }

    class ErrorTestProvider : ArgumentsProvider {

        val errorTestCases = listOf(
            ErrorTestCase(
                "Create Table Illegal Check Expression",
                """
                    CREATE TABLE TBL(
                        CHECK (SELECT a FROM foo)
                    )
                """.trimIndent()
            ),
            ErrorTestCase(
                "NULL not allowed as type in type decalration",
                """
                    CREATE TABLE TBL(
                        a NULL
                    )
                """.trimIndent()
            ),
            ErrorTestCase(
                "MISSING not allowed as type in type decalration",
                """
                    CREATE TABLE TBL(
                        a MISSING
                    )
                """.trimIndent()
            ),
            ErrorTestCase(
                "STRUCT<> NOT Supported",
                """
                    CREATE TABLE TBL(
                        a STRUCT<>
                    )
                """.trimIndent()
            ),
            ErrorTestCase(
                "LIST<> NOT Supported",
                """
                    CREATE TABLE TBL(
                        a LIST<>
                    )
                """.trimIndent()
            ),

            // TODO: Move this to another place as part of parser test porting process
            ErrorTestCase(
                "Struct Field declaration not allowed for is Operator",
                "a IS STRUCT<b : INT2>"
            ),
            ErrorTestCase(
                "Struct Field declaration not allowed for CAST Operator",
                "CAST(a AS STRUCT<b : INT2>)"
            ),
            ErrorTestCase(
                "ELEMENT declaration for LIST Type not allowed for is Operator",
                "a IS LIST<INT2>"
            ),
            ErrorTestCase(
                "Struct Field declaration not allowed for CAST Operator",
                "CAST(a AS LIST<INT2>)"
            ),
        )
        override fun provideArguments(p0: ExtensionContext?): Stream<out Arguments> =
            errorTestCases.map { Arguments.of(it) }.stream()
    }

    private fun assertExpression(input: String, expected: DdlOp) {
        val result = parser.parse(input)
        val actual = result.root
        assertEquals(statementDDL(expected), actual)
    }

    // For now, just assert throw
    private fun assertIssue(input: String) {
        assertThrows<PartiQLParserException> {
            parser.parse(input)
        }
    }
}
