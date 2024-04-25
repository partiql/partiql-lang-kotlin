// package org.partiql.parser.internal
//
// import org.junit.jupiter.api.Assertions.assertEquals
// import org.junit.jupiter.api.assertThrows
// import org.junit.jupiter.api.extension.ExtensionContext
// import org.junit.jupiter.params.ParameterizedTest
// import org.junit.jupiter.params.provider.Arguments
// import org.junit.jupiter.params.provider.ArgumentsProvider
// import org.junit.jupiter.params.provider.ArgumentsSource
// import org.partiql.ast.DdlOp
// import org.partiql.ast.Expr
// import org.partiql.ast.Identifier
// import org.partiql.ast.Type
// import org.partiql.ast.constraint
// import org.partiql.ast.constraintBodyCheck
// import org.partiql.ast.constraintBodyNotNull
// import org.partiql.ast.constraintBodyUnique
// import org.partiql.ast.ddlOpCreateTable
// import org.partiql.ast.ddlOpDropTable
// import org.partiql.ast.exprBinary
// import org.partiql.ast.exprLit
// import org.partiql.ast.exprVar
// import org.partiql.ast.identifierQualified
// import org.partiql.ast.identifierSymbol
// import org.partiql.ast.partitionExprColomnList
// import org.partiql.ast.statementDDL
// import org.partiql.ast.tableDefinition
// import org.partiql.ast.tableDefinitionColumn
// import org.partiql.ast.tableProperty
// import org.partiql.parser.PartiQLParserException
// import org.partiql.value.PartiQLValueExperimental
// import org.partiql.value.boolValue
// import org.partiql.value.int32Value
// import org.partiql.value.stringValue
// import java.util.stream.Stream
//
// class PartiQLParserDDLTests {
//
//    private val parser = PartiQLParserDefault()
//
//    data class SuccessTestCase(
//        val description: String? = null,
//        val query: String,
//        val expectedOp: DdlOp
//    )
//
//    data class ErrorTestCase(
//        val description: String? = null,
//        val query: String,
//    )
//
//    @ArgumentsSource(SuccessTestProvider::class)
//    @ParameterizedTest
//    fun successTests(tc: SuccessTestCase) = assertExpression(tc.query, tc.expectedOp)
//
//    @ArgumentsSource(ErrorTestProvider::class)
//    @ParameterizedTest
//    fun errorTests(tc: ErrorTestCase) = assertIssue(tc.query)
//
//    class SuccessTestProvider : ArgumentsProvider {
//        @OptIn(PartiQLValueExperimental::class)
//        val createTableTests = listOf(
//            //
//            // Qualified Identifier as Table Name
//            //
//
//            SuccessTestCase(
//                "CREATE TABLE with unqualified case insensitive name",
//                "CREATE TABLE foo",
//                ddlOpCreateTable(
//                    identifierSymbol("foo", Identifier.CaseSensitivity.INSENSITIVE),
//                    null,
//                    null,
//                    emptyList()
//                )
//            ),
//            // Support Case Sensitive identifier as table name
//            // Subsequent process may need to change
//            // See: https://www.db-fiddle.com/f/9A8mknSNYuRGLfkqkLeiHD/0 for reference.
//            SuccessTestCase(
//                "CREATE TABLE with unqualified case sensitive name",
//                "CREATE TABLE \"foo\"",
//                ddlOpCreateTable(
//                    identifierSymbol("foo", Identifier.CaseSensitivity.SENSITIVE),
//                    null,
//                    null,
//                    emptyList()
//                )
//            ),
//            SuccessTestCase(
//                "CREATE TABLE with qualified case insensitive name",
//                "CREATE TABLE myCatalog.mySchema.foo",
//                ddlOpCreateTable(
//                    identifierQualified(
//                        identifierSymbol("myCatalog", Identifier.CaseSensitivity.INSENSITIVE),
//                        listOf(
//                            identifierSymbol("mySchema", Identifier.CaseSensitivity.INSENSITIVE),
//                            identifierSymbol("foo", Identifier.CaseSensitivity.INSENSITIVE),
//                        )
//                    ),
//                    null,
//                    null,
//                    emptyList()
//                )
//            ),
//            SuccessTestCase(
//                "CREATE TABLE with qualified name with mixed case sensitivity",
//                "CREATE TABLE myCatalog.\"mySchema\".foo",
//                ddlOpCreateTable(
//                    identifierQualified(
//                        identifierSymbol("myCatalog", Identifier.CaseSensitivity.INSENSITIVE),
//                        listOf(
//                            identifierSymbol("mySchema", Identifier.CaseSensitivity.SENSITIVE),
//                            identifierSymbol("foo", Identifier.CaseSensitivity.INSENSITIVE),
//                        )
//                    ),
//                    null,
//                    null,
//                    emptyList()
//                )
//            ),
//
//            //
//            // Column Constraints
//            //
//            SuccessTestCase(
//                "CREATE TABLE with Column NOT NULL Constraint",
//                """
//                    CREATE TABLE tbl (
//                        a INT2 NOT NULL
//                    )
//                """.trimIndent(),
//                ddlOpCreateTable(
//                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
//                    tableDefinition(
//                        listOf(
//                            tableDefinitionColumn(
//                                identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
//                                Type.Int2(),
//                                false,
//                                listOf(constraint(null, constraintBodyNotNull())),
//                                null
//                            )
//                        ),
//                        emptyList()
//                    ),
//                    null,
//                    emptyList()
//                )
//            ),
//
//            SuccessTestCase(
//                "CREATE TABLE with Column Unique Constraint",
//                """
//                    CREATE TABLE tbl (
//                        a INT2 UNIQUE
//                    )
//                """.trimIndent(),
//                ddlOpCreateTable(
//                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
//                    tableDefinition(
//                        listOf(
//                            tableDefinitionColumn(
//                                identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
//                                Type.Int2(),
//                                false,
//                                listOf(constraint(null, constraintBodyUnique(null, false))),
//                                null
//                            )
//                        ),
//                        emptyList()
//                    ),
//                    null,
//                    emptyList()
//                )
//            ),
//
//            SuccessTestCase(
//                "CREATE TABLE with Column Primary Key Constraint",
//                """
//                    CREATE TABLE tbl (
//                        a INT2 PRIMARY KEY
//                    )
//                """.trimIndent(),
//                ddlOpCreateTable(
//                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
//                    tableDefinition(
//                        listOf(
//                            tableDefinitionColumn(
//                                identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
//                                Type.Int2(),
//                                false,
//                                listOf(constraint(null, constraintBodyUnique(null, true))),
//                                null
//                            )
//                        ),
//                        emptyList()
//                    ),
//                    null,
//                    emptyList()
//                )
//            ),
//
//            SuccessTestCase(
//                "CREATE TABLE with Column CHECK Constraint",
//                """
//                    CREATE TABLE tbl (
//                        a INT2 CHECK (a > 0)
//                    )
//                """.trimIndent(),
//                ddlOpCreateTable(
//                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
//                    tableDefinition(
//                        listOf(
//                            tableDefinitionColumn(
//                                identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
//                                Type.Int2(),
//                                false,
//                                listOf(
//                                    constraint(
//                                        null,
//                                        constraintBodyCheck(
//                                            exprBinary(
//                                                Expr.Binary.Op.GT,
//                                                exprVar(identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE), Expr.Var.Scope.DEFAULT),
//                                                exprLit(int32Value(0))
//                                            )
//                                        )
//                                    )
//                                ),
//                                null
//                            )
//                        ),
//                        emptyList()
//                    ),
//                    null,
//                    emptyList()
//                )
//            ),
//
//            SuccessTestCase(
//                "CREATE TABLE with Table Unique Constraint",
//                """
//                    CREATE TABLE tbl (
//                        UNIQUE (a, b)
//                    )
//                """.trimIndent(),
//                ddlOpCreateTable(
//                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
//                    tableDefinition(
//                        emptyList(),
//                        listOf(
//                            constraint(
//                                null,
//                                constraintBodyUnique(
//                                    listOf(
//                                        identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
//                                        identifierSymbol("b", Identifier.CaseSensitivity.INSENSITIVE),
//                                    ),
//                                    false
//                                )
//                            )
//                        )
//                    ),
//                    null,
//                    emptyList()
//                )
//            ),
//
//            SuccessTestCase(
//                "CREATE TABLE with Table Primary Key Constraint",
//                """
//                    CREATE TABLE tbl (
//                        PRIMARY KEY (a, b)
//                    )
//                """.trimIndent(),
//                ddlOpCreateTable(
//                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
//                    tableDefinition(
//                        emptyList(),
//                        listOf(
//                            constraint(
//                                null,
//                                constraintBodyUnique(
//                                    listOf(
//                                        identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
//                                        identifierSymbol("b", Identifier.CaseSensitivity.INSENSITIVE),
//                                    ),
//                                    true
//                                )
//                            )
//                        )
//                    ),
//                    null,
//                    emptyList()
//                )
//            ),
//
//            SuccessTestCase(
//                "CREATE TABLE with Table CHECK Constraint",
//                """
//                    CREATE TABLE tbl (
//                        CHECK (a > 0)
//                    )
//                """.trimIndent(),
//                ddlOpCreateTable(
//                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
//                    tableDefinition(
//                        emptyList(),
//                        listOf(
//                            constraint(
//                                null,
//                                constraintBodyCheck(
//                                    exprBinary(
//                                        Expr.Binary.Op.GT,
//                                        exprVar(identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE), Expr.Var.Scope.DEFAULT),
//                                        exprLit(int32Value(0))
//                                    )
//                                )
//                            )
//                        )
//                    ),
//                    null,
//                    emptyList()
//                )
//            ),
//
//            SuccessTestCase(
//                "CREATE TABLE with Table Partition BY",
//                """
//                    CREATE TABLE tbl
//                    PARTITION BY (a, b)
//                """.trimIndent(),
//                ddlOpCreateTable(
//                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
//                    null,
//                    partitionExprColomnList(
//                        listOf(
//                            identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
//                            identifierSymbol("b", Identifier.CaseSensitivity.INSENSITIVE),
//                        )
//                    ),
//                    emptyList()
//                )
//            ),
//
//            SuccessTestCase(
//                "CREATE TABLE with Table TBLPROPERTIES",
//                """
//                    CREATE TABLE tbl
//                    TBLPROPERTIES ('myPropertyKey1' = 'myPropertyValue1', 'myPropertyKey2' = false)
//                """.trimIndent(),
//                ddlOpCreateTable(
//                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
//                    null,
//                    null,
//                    listOf(
//                        tableProperty("mypropertykey1", stringValue("myPropertyValue1")),
//                        tableProperty("mypropertykey2", boolValue(false))
//                    )
//                )
//            ),
//
//            SuccessTestCase(
//                "CREATE TABLE with CASE SENSITIVE Identifier as column name",
//                """
//                    CREATE TABLE tbl (
//                        "a" INT2
//                    )
//                """.trimIndent(),
//                ddlOpCreateTable(
//                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
//                    tableDefinition(
//                        listOf(
//                            tableDefinitionColumn(
//                                identifierSymbol("a", Identifier.CaseSensitivity.SENSITIVE),
//                                Type.Int2(),
//                                false,
//                                emptyList(),
//                                null
//                            )
//                        ),
//                        emptyList()
//                    ),
//                    null,
//                    emptyList()
//                )
//            ),
//            SuccessTestCase(
//                "CREATE TABLE with Optional Field",
//                """
//                    CREATE TABLE tbl (
//                        a OPTIONAL INT2
//                    )
//                """.trimIndent(),
//                ddlOpCreateTable(
//                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
//                    tableDefinition(
//                        listOf(
//                            tableDefinitionColumn(
//                                identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
//                                Type.Int2(),
//                                true,
//                                emptyList(),
//                                null
//                            )
//                        ),
//                        emptyList()
//                    ),
//                    null,
//                    emptyList()
//                )
//            ),
//            SuccessTestCase(
//                "CREATE TABLE with Struct Type",
//                """
//                    CREATE TABLE tbl (
//                        a STRUCT<b: INT2>
//                    )
//                """.trimIndent(),
//                ddlOpCreateTable(
//                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
//                    tableDefinition(
//                        listOf(
//                            tableDefinitionColumn(
//                                identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
//                                Type.Struct(
//                                    listOf(
//                                        Type.Struct.Field(
//                                            identifierSymbol("b", Identifier.CaseSensitivity.INSENSITIVE),
//                                            Type.Int2(),
//                                            false,
//                                            null,
//                                        )
//                                    )
//                                ),
//                                false,
//                                emptyList(),
//                                null
//                            )
//                        ),
//                        emptyList()
//                    ),
//                    null,
//                    emptyList()
//                )
//            ),
//            SuccessTestCase(
//                "CREATE TABLE with Struct Type with Optional Field",
//                """
//                    CREATE TABLE tbl (
//                        a STRUCT<b OPTIONAL : INT2>
//                    )
//                """.trimIndent(),
//                ddlOpCreateTable(
//                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
//                    tableDefinition(
//                        listOf(
//                            tableDefinitionColumn(
//                                identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
//                                Type.Struct(
//                                    listOf(
//                                        Type.Struct.Field(
//                                            identifierSymbol("b", Identifier.CaseSensitivity.INSENSITIVE),
//                                            Type.Int2(),
//                                            true,
//                                            null
//                                        )
//                                    )
//                                ),
//                                false,
//                                emptyList(),
//                                null
//                            )
//                        ),
//                        emptyList()
//                    ),
//                    null,
//                    emptyList()
//                )
//            ),
//            SuccessTestCase(
//                "CREATE TABLE with Struct Type with comments",
//                """
//                    CREATE TABLE tbl (
//                        a STRUCT<b : INT2 COMMENT 'inner comments'> COMMENT 'outer comments'
//                    )
//                """.trimIndent(),
//                ddlOpCreateTable(
//                    identifierSymbol("tbl", Identifier.CaseSensitivity.INSENSITIVE),
//                    tableDefinition(
//                        listOf(
//                            tableDefinitionColumn(
//                                identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
//                                Type.Struct(
//                                    listOf(
//                                        Type.Struct.Field(
//                                            identifierSymbol("b", Identifier.CaseSensitivity.INSENSITIVE),
//                                            Type.Int2(),
//                                            false,
//                                            "inner comments"
//                                        )
//                                    )
//                                ),
//                                false,
//                                emptyList(),
//                                "outer comments"
//                            )
//                        ),
//                        emptyList()
//                    ),
//                    null,
//                    emptyList()
//                )
//            ),
//        )
//
//        val dropTableTests = listOf(
//            SuccessTestCase(
//                "DROP TABLE with unqualified case insensitive name",
//                "DROP TABLE foo",
//                ddlOpDropTable(
//                    identifierSymbol("foo", Identifier.CaseSensitivity.INSENSITIVE),
//                )
//            ),
//            SuccessTestCase(
//                "DROP TABLE with unqualified case sensitive name",
//                "DROP TABLE \"foo\"",
//                ddlOpDropTable(
//                    identifierSymbol("foo", Identifier.CaseSensitivity.SENSITIVE),
//                )
//            ),
//            SuccessTestCase(
//                "DROP TABLE with qualified case insensitive name",
//                "DROP TABLE myCatalog.mySchema.foo",
//                ddlOpDropTable(
//                    identifierQualified(
//                        identifierSymbol("myCatalog", Identifier.CaseSensitivity.INSENSITIVE),
//                        listOf(
//                            identifierSymbol("mySchema", Identifier.CaseSensitivity.INSENSITIVE),
//                            identifierSymbol("foo", Identifier.CaseSensitivity.INSENSITIVE),
//                        )
//                    ),
//                )
//            ),
//            SuccessTestCase(
//                "DROP TABLE with qualified name with mixed case sensitivity",
//                "DROP TABLE myCatalog.\"mySchema\".foo",
//                ddlOpDropTable(
//                    identifierQualified(
//                        identifierSymbol("myCatalog", Identifier.CaseSensitivity.INSENSITIVE),
//                        listOf(
//                            identifierSymbol("mySchema", Identifier.CaseSensitivity.SENSITIVE),
//                            identifierSymbol("foo", Identifier.CaseSensitivity.INSENSITIVE),
//                        )
//                    ),
//                )
//            ),
//        )
//
//        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> =
//            (createTableTests + dropTableTests).map { Arguments.of(it) }.stream()
//    }
//
//    class ErrorTestProvider : ArgumentsProvider {
//
//        val errorTestCases = listOf(
//            ErrorTestCase(
//                "Create Table multiple TBL properties",
//                """
//                    CREATE TABLE TBL
//                    TBLPROPERTIES ('key1' = 'value1')
//                    TBLPROPERTIES ('key2' = 'value2')
//                """.trimIndent()
//            ),
//            ErrorTestCase(
//                "Create Table multiple PARTITION BY",
//                """
//                    CREATE TABLE TBL
//                    PARTITION BY (a)
//                    PARTITION BY (b)
//                """.trimIndent()
//            ),
//            ErrorTestCase(
//                "Create Table Illegal Check Expression",
//                """
//                    CREATE TABLE TBL(
//                        CHECK (SELECT a FROM foo)
//                    )
//                """.trimIndent()
//            )
//        )
//        override fun provideArguments(p0: ExtensionContext?): Stream<out Arguments> =
//            errorTestCases.map { Arguments.of(it) }.stream()
//    }
//
//    private fun assertExpression(input: String, expected: DdlOp) {
//        val result = parser.parse(input)
//        val actual = result.root
//        assertEquals(statementDDL(expected), actual)
//    }
//
//    // For now, just assert throw
//    private fun assertIssue(input: String) {
//        assertThrows<PartiQLParserException> {
//            parser.parse(input)
//        }
//    }
// }
