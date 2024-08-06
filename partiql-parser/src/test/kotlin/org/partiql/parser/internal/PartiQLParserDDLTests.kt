package org.partiql.parser.internal

import org.junit.jupiter.api.Test
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
import org.partiql.ast.PartitionBy
import org.partiql.ast.Type
import org.partiql.ast.binder
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
import org.partiql.ast.tableProperty
import org.partiql.parser.PartiQLParserException
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.int32Value
import org.partiql.value.stringValue
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
                    null,
                    binder("foo", true),
                    null,
                    null,
                    emptyList()
                )
            ),
            // Support Case Sensitive identifier as table name
            // Subsequent process may need to change
            // See: https://www.db-fiddle.com/f/9A8mknSNYuRGLfkqkLeiHD/0 for reference.
            SuccessTestCase(
                "CREATE TABLE with unqualified case sensitive name",
                "CREATE TABLE \"foo\"",
                ddlOpCreateTable(
                    null,
                    binder("foo", false),
                    null,
                    null,
                    emptyList()
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
                        )
                    ),
                    binder("foo", true),
                    null,
                    null,
                    emptyList()
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
                        )
                    ),
                    binder("foo", true),
                    null,
                    null,
                    emptyList()
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
                    null,
                    binder("tbl", true),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                binder("a", true),
                                Type.Int2(),
                                listOf(constraint(null, constraintDefinitionNotNull())),
                                false,
                                null
                            )
                        ),
                        emptyList()
                    ),
                    null,
                    emptyList()
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
                    null,
                    binder("tbl", true),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                binder("a", true),
                                Type.Int2(),
                                listOf(constraint(null, constraintDefinitionUnique(null, false))),
                                false,
                                null
                            )
                        ),
                        emptyList()
                    ),
                    null,
                    emptyList()
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
                    null,
                    binder("tbl", true),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                binder("a", true),
                                Type.Int2(),
                                listOf(constraint(null, constraintDefinitionUnique(null, true))),
                                false,
                                null
                            )
                        ),
                        emptyList()
                    ),
                    null,
                    emptyList()
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
                    null,
                    binder("tbl", true),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                binder("a", true),
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
                                false,
                                null
                            )
                        ),
                        emptyList()
                    ),
                    null,
                    emptyList()
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
                    null,
                    binder("tbl", true),
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
                    null,
                    emptyList()
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
                    null,
                    binder("tbl", true),
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
                    null,
                    emptyList()
                )
            ),

            // Support for Table Check constraint has been nuked
//            SuccessTestCase(
//                "CREATE TABLE with Table CHECK Constraint",
//                """
//                    CREATE TABLE tbl (
//                        CHECK (a > 0)
//                    )
//                """.trimIndent(),
//                ddlOpCreateTable(
//                    null,
//                    binder("tbl", true),
//                    tableDefinition(
//                        emptyList(),
//                        listOf(
//                            constraintCheck(
//                                exprBinary(
//                                    Expr.Binary.Op.GT,
//                                    exprVar(identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE), Expr.Var.Scope.DEFAULT),
//                                    exprLit(int32Value(0))
//                                )
//                            )
//                        )
//                    ),
//                    null,
//                    emptyList()
//                )
//            ),

            SuccessTestCase(
                "CREATE TABLE with CASE SENSITIVE Identifier as column name",
                """
                    CREATE TABLE tbl (
                        "a" INT2
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    null,
                    binder("tbl", true),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                binder("a", false),
                                Type.Int2(),
                                emptyList(),
                                false,
                                null
                            )
                        ),
                        emptyList()
                    ),
                    null,
                    emptyList()
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
                    null,
                    binder("tbl", true),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                binder("a", true),
                                Type.Struct(
                                    listOf(
                                        Type.Struct.Field(
                                            binder("b", true),
                                            Type.Int2(),
                                            emptyList(),
                                            false,
                                            null
                                        ),
                                        Type.Struct.Field(
                                            binder("c", true),
                                            Type.Int2(),
                                            listOf(Constraint(null, Constraint.Definition.NotNull())),
                                            false,
                                            null
                                        )
                                    )
                                ),
                                emptyList(),
                                false,
                                null
                            )
                        ),
                        emptyList()
                    ),
                    null,
                    emptyList()
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
                    null,
                    binder("tbl", true),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                binder("a", true),
                                Type.Struct(
                                    listOf(
                                        Type.Struct.Field(
                                            binder("b", true),
                                            Type.Struct(
                                                listOf(
                                                    Type.Struct.Field(
                                                        binder("c", true),
                                                        Type.Int2(),
                                                        emptyList(),
                                                        false,
                                                        null
                                                    ),
                                                )
                                            ),
                                            emptyList(),
                                            false,
                                            null
                                        ),
                                        Type.Struct.Field(
                                            binder("d", true),
                                            Type.Array(Type.Int2()),
                                            emptyList(),
                                            false,
                                            null
                                        )
                                    )
                                ),
                                emptyList(),
                                false,
                                null
                            )
                        ),
                        emptyList()
                    ),
                    null,
                    emptyList()
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
                    null,
                    binder("tbl", true),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                binder("a", true),
                                Type.Struct(
                                    emptyList()
                                ),
                                emptyList(),
                                false,
                                null
                            )
                        ),
                        emptyList()
                    ),
                    null,
                    emptyList()
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
                    null,
                    binder("tbl", true),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                binder("a", true),
                                Type.Array(Type.Int2()),
                                emptyList(),
                                false,
                                null
                            )
                        ),
                        emptyList()
                    ),
                    null,
                    emptyList()
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
                    null,
                    binder("tbl", true),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                binder("a", true),
                                Type.Array(
                                    Type.Struct(
                                        listOf(
                                            Type.Struct.Field(
                                                binder("b", true),
                                                Type.Int2(),
                                                emptyList(),
                                                false,
                                                null
                                            ),
                                        )
                                    ),
                                ),
                                emptyList(),
                                false,
                                null
                            )
                        ),
                        emptyList()
                    ),
                    null,
                    emptyList()
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
                    null,
                    binder("tbl", true),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                binder("a", true),
                                Type.Array(
                                    Type.Struct(
                                        listOf(
                                            Type.Struct.Field(
                                                binder("b", true),
                                                Type.Int2(),
                                                emptyList(),
                                                false,
                                                null
                                            ),
                                        )
                                    ),
                                ),
                                emptyList(),
                                false,
                                null
                            )
                        ),
                        emptyList()
                    ),
                    null,
                    emptyList()
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with ARRAY without element type",
                """
                    CREATE TABLE tbl (
                        a ARRAY
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    null,
                    binder("tbl", true),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                binder("a", true),
                                Type.Array(null),
                                emptyList(),
                                false,
                                null
                            )
                        ),
                        emptyList()
                    ),
                    null,
                    emptyList()
                )
            ),

            //
            // Optional keyword
            //
            SuccessTestCase(
                "CREATE TABLE with top level attribute optional",
                """
                    CREATE TABLE tbl (
                        a OPTIONAL INT2
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    null,
                    binder("tbl", true),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                binder("a", true),
                                Type.Int2(),
                                emptyList(),
                                true,
                                null
                            )
                        ),
                        emptyList()
                    ),
                    null,
                    emptyList()
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with struct optional field",
                """
                    CREATE TABLE tbl (
                        a STRUCT<b OPTIONAL: INT2>
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    null,
                    binder("tbl", true),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                binder("a", true),
                                Type.Struct(
                                    listOf(
                                        Type.Struct.Field(
                                            binder("b", true),
                                            Type.Int2(),
                                            emptyList(),
                                            true,
                                            null
                                        )
                                    )
                                ),
                                emptyList(),
                                false,
                                null
                            )
                        ),
                        emptyList()
                    ),
                    null,
                    emptyList()
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with optional struct which has optional field",
                """
                    CREATE TABLE tbl (
                        a OPTIONAL STRUCT<b OPTIONAL: INT2>
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    null,
                    binder("tbl", true),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                binder("a", true),
                                Type.Struct(
                                    listOf(
                                        Type.Struct.Field(
                                            binder("b", true),
                                            Type.Int2(),
                                            emptyList(),
                                            true,
                                            null
                                        )
                                    )
                                ),
                                emptyList(),
                                true,
                                null
                            )
                        ),
                        emptyList()
                    ),
                    null,
                    emptyList()
                )
            ),

            // COMMENT keyword
            SuccessTestCase(
                "CREATE TABLE with comment on top level attribute ",
                """
                    CREATE TABLE tbl (
                        a INT2 COMMENT 'attribute a'
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    null,
                    binder("tbl", true),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                binder("a", true),
                                Type.Int2(),
                                emptyList(),
                                false,
                                "attribute a"
                            )
                        ),
                        emptyList()
                    ),
                    null,
                    emptyList()
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with comment on struct field",
                """
                    CREATE TABLE tbl (
                        a STRUCT<b : INT2 COMMENT 'comment on struct field'>
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    null,
                    binder("tbl", true),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                binder("a", true),
                                Type.Struct(
                                    listOf(
                                        Type.Struct.Field(
                                            binder("b", true),
                                            Type.Int2(),
                                            emptyList(),
                                            false,
                                            "comment on struct field"
                                        )
                                    )
                                ),
                                emptyList(),
                                false,
                                null
                            )
                        ),
                        emptyList()
                    ),
                    null,
                    emptyList()
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with comment on struct which has comment on field",
                """
                    CREATE TABLE tbl (
                        a STRUCT<b : INT2 COMMENT 'comment on inner level'> COMMENT 'comment on top level'
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    null,
                    binder("tbl", true),
                    tableDefinition(
                        listOf(
                            tableDefinitionAttribute(
                                binder("a", true),
                                Type.Struct(
                                    listOf(
                                        Type.Struct.Field(
                                            binder("b", true),
                                            Type.Int2(),
                                            emptyList(),
                                            false,
                                            "comment on inner level"
                                        )
                                    )
                                ),
                                emptyList(),
                                false,
                                "comment on top level"
                            )
                        ),
                        emptyList()
                    ),
                    null,
                    emptyList()
                )
            ),

            // Partition BY
            SuccessTestCase(
                "CREATE TABLE with Partition by single attribute",
                """
                    CREATE TABLE tbl
                        PARTITION BY (a)
                """.trimIndent(),
                ddlOpCreateTable(
                    null,
                    binder("tbl", true),
                    null,
                    PartitionBy.AttrList(
                        listOf(
                            identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
                        )
                    ),
                    emptyList()
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with Partition by multiple attribute",
                """
                    CREATE TABLE tbl
                        PARTITION BY (a, b)
                """.trimIndent(),
                ddlOpCreateTable(
                    null,
                    binder("tbl", true),
                    null,
                    PartitionBy.AttrList(
                        listOf(
                            identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),
                            identifierSymbol("b", Identifier.CaseSensitivity.INSENSITIVE),
                        )
                    ),
                    emptyList()
                )
            ),

            // Table Properties
            SuccessTestCase(
                "CREATE TABLE with TBLPROPERTIES single property",
                """
                    CREATE TABLE tbl
                    TBLPROPERTIES ('k1' = 'v1')
                """.trimIndent(),
                ddlOpCreateTable(
                    null,
                    binder("tbl", true),
                    null,
                    null,
                    listOf(tableProperty("k1", stringValue("v1")))
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with TBLPROPERTIES preserve case sensitivity",
                """
                    CREATE TABLE tbl
                    TBLPROPERTIES ('K1k' = 'V1v')
                """.trimIndent(),
                ddlOpCreateTable(
                    null,
                    binder("tbl", true),
                    null,
                    null,
                    listOf(tableProperty("K1k", stringValue("V1v")))
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with TBLPROPERTIES multiple properties",
                """
                    CREATE TABLE tbl
                    TBLPROPERTIES ('k1' = 'v1', 'k2' = 'v2')
                """.trimIndent(),
                ddlOpCreateTable(
                    null,
                    binder("tbl", true),
                    null,
                    null,
                    listOf(
                        tableProperty("k1", stringValue("v1")),
                        tableProperty("k2", stringValue("v2"))
                    )
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with TBLPROPERTIES and PARTITION BY",
                """
                    CREATE TABLE tbl
                    PARTITION BY (a)
                    TBLPROPERTIES ('k1' = 'v1')
                """.trimIndent(),
                ddlOpCreateTable(
                    null,
                    binder("tbl", true),
                    null,
                    PartitionBy.AttrList(listOf(identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE),),),
                    listOf(tableProperty("k1", stringValue("v1")),)
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
                "NULL not allowed as type in type declaration",
                """
                    CREATE TABLE TBL(
                        a NULL
                    )
                """.trimIndent()
            ),
            ErrorTestCase(
                "MISSING not allowed as type in type declaration",
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
            ErrorTestCase(
                "Multiple Partition by not allowed",
                """
                    CREATE TABLE TBL
                        PARTITION BY (a)
                        PARTITION BY (b)
                """.trimIndent()
            ),
            ErrorTestCase(
                "Empty Partition by not allowed",
                """
                    CREATE TABLE TBL
                        PARTITION BY ()
                """.trimIndent()
            ),

            ErrorTestCase(
                "Multiple TBLPROPERTIES not allowed",
                """
                    CREATE TABLE TBL
                        TBLPROPERTIES('k1' = 'v1')
                        TBLPROPERTIES('k2' = 'v2')
                """.trimIndent()
            ),

            ErrorTestCase(
                "Empty TBLPROPERTIES not allowed",
                """
                    CREATE TABLE TBL
                        TBLPROPERTIES()
                """.trimIndent()
            ),

            ErrorTestCase(
                "TBLPROPERTIES only allowed String value",
                """
                    CREATE TABLE TBL
                        TBLPROPERTIES('k1' = 1)
                """.trimIndent()
            ),

            ErrorTestCase(
                "TBLPROPERTIES only allowed String key",
                """
                    CREATE TABLE TBL
                        TBLPROPERTIES(1 = '1')
                """.trimIndent()
            ),

            ErrorTestCase(
                "OPTIONAL needs to follow attribute name",
                """
                    CREATE TABLE TBL (
                        a INT2 OPTIONAL
                    )
                """.trimIndent()
            ),

            ErrorTestCase(
                "Multiple Optional not allowed",
                """
                    CREATE TABLE TBL (
                        a OPTIONAL OPTIONAL INT2
                    )
                """.trimIndent()
            ),

            ErrorTestCase(
                "Multiple COMMENT not allowed",
                """
                    CREATE TABLE TBL (
                        a INT2 COMMENT 'comment1' COMMENT 'comment2'
                    )
                """.trimIndent()
            ),

            ErrorTestCase(
                "Inline comment needs to appear last",
                """
                    CREATE TABLE TBL (
                        a INT2 COMMENT 'comment1' NOT NULL
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

    @Test
    fun sanity() {
        val query = """
        CREATE TABLE andes.my_provider.my_table_v1 (
            attr1 STRUCT<attr2: STRUCT<attr3: INT2 CHECK(attr3 >=0)>>
        )
        """.trimIndent()

        val ast = parser.parse(query).root

        println(ast)
    }
}
