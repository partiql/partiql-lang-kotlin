package org.partiql.parser.internal

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.ast.Ast.columnConstraintCheck
import org.partiql.ast.Ast.columnConstraintNullable
import org.partiql.ast.Ast.columnConstraintUnique
import org.partiql.ast.Ast.columnDefinition
import org.partiql.ast.Ast.createTable
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.Ast.keyValue
import org.partiql.ast.Ast.partitionBy
import org.partiql.ast.Ast.tableConstraintUnique
import org.partiql.ast.AstNode
import org.partiql.ast.DataType
import org.partiql.ast.Identifier
import org.partiql.ast.Identifier.Simple.delimited
import org.partiql.ast.Identifier.Simple.regular
import org.partiql.ast.Literal.intNum
import java.util.stream.Stream
import kotlin.test.assertEquals

class PartiQLParserDDLTests {

    private val parser = PartiQLParserDefault()

    data class SuccessTestCase(
        val description: String? = null,
        val query: String,
        val node: AstNode
    )

    // DDL not yet supported in v1 AST
    @ArgumentsSource(TestProvider::class)
    @ParameterizedTest
    fun runTestCases(tc: SuccessTestCase) = assertExpression(tc.query, tc.node)

    class TestProvider : ArgumentsProvider {
        private val createTableTestsExisting = listOf(
            SuccessTestCase(
                "CREATE TABLE with unqualified case insensitive name",
                "CREATE TABLE foo",
                createTable(
                    Identifier.regular("foo"),
                    emptyList(),
                    emptyList(),
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
                createTable(
                    Identifier.delimited("foo"),
                    emptyList(),
                    emptyList(),
                    null,
                    emptyList()
                )
            ),
            SuccessTestCase(
                "CREATE TABLE with qualified case insensitive name",
                "CREATE TABLE myCatalog.mySchema.foo",
                createTable(
                    Identifier.of(
                        regular("myCatalog"),
                        regular("mySchema"),
                        regular("foo")
                    ),
                    emptyList(),
                    emptyList(),
                    null,
                    emptyList()
                )
            ),
            SuccessTestCase(
                "CREATE TABLE with qualified name with mixed case sensitivity",
                "CREATE TABLE myCatalog.\"mySchema\".foo",
                createTable(
                    Identifier.of(
                        regular("myCatalog"),
                        delimited("mySchema"),
                        regular("foo")
                    ),
                    emptyList(),
                    emptyList(),
                    null,
                    emptyList()
                )
            ),
        )

        private val createTableTestsAddition = listOf(
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
                createTable(
                    Identifier.regular("tbl"),
                    listOf(
                        columnDefinition(
                            regular("a"),
                            DataType.INT2(),
                            false,
                            listOf(
                                columnConstraintNullable(null, false)
                            ),
                            null
                        )
                    ),
                    listOf(),
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
                createTable(
                    Identifier.regular("tbl"),
                    listOf(
                        columnDefinition(
                            regular("a"),
                            DataType.INT2(),
                            false,
                            listOf(
                                columnConstraintUnique(null, false)
                            ),
                            null
                        )
                    ),
                    listOf(),
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
                createTable(
                    Identifier.regular("tbl"),
                    listOf(
                        columnDefinition(
                            regular("a"),
                            DataType.INT2(),
                            false,
                            listOf(
                                columnConstraintUnique(null, true)
                            ),
                            null
                        )
                    ),
                    listOf(),
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
                createTable(
                    Identifier.regular("tbl"),
                    listOf(
                        columnDefinition(
                            regular("a"),
                            DataType.INT2(),
                            false,
                            listOf(
                                columnConstraintCheck(
                                    null,
                                    exprOperator(
                                        ">",
                                        exprVarRef(
                                            Identifier.regular("a"),
                                            isQualified = false
                                        ),
                                        exprLit(
                                            intNum(0)
                                        )
                                    )
                                )
                            ),
                            null
                        )
                    ),
                    listOf(),
                    null,
                    emptyList()
                )
            ),

            // Table Constraint
            SuccessTestCase(
                "CREATE TABLE with Table Unique Constraint",
                """
                    CREATE TABLE tbl (
                        UNIQUE (a, b)
                    )
                """.trimIndent(),
                createTable(
                    Identifier.regular("tbl"),
                    listOf(),
                    listOf(
                        tableConstraintUnique(
                            null,
                            listOf(
                                regular("a"),
                                regular("b")
                            ),
                            false,
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
                createTable(
                    Identifier.regular("tbl"),
                    listOf(),
                    listOf(
                        tableConstraintUnique(
                            null,
                            listOf(
                                regular("a"),
                                regular("b")
                            ),
                            true,
                        )
                    ),
                    null,
                    emptyList()
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with CASE SENSITIVE Identifier as column name",
                """
                    CREATE TABLE tbl (
                        "a" INT2
                    )
                """.trimIndent(),
                createTable(
                    Identifier.regular("tbl"),
                    listOf(
                        columnDefinition(
                            delimited("a"),
                            DataType.INT2(),
                            false,
                            listOf(),
                            null
                        )
                    ),
                    listOf(),
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
                createTable(
                    Identifier.regular("tbl"),
                    listOf(
                        columnDefinition(
                            regular("a"),
                            DataType.STRUCT(
                                listOf(
                                    DataType.StructField(
                                        regular("b"),
                                        DataType.INT2(),
                                        false,
                                        emptyList(),
                                        null
                                    ),
                                    DataType.StructField(
                                        regular("c"),
                                        DataType.INT2(),
                                        false,
                                        listOf(
                                            columnConstraintNullable(null, false)
                                        ),
                                        null
                                    ),
                                )
                            ),
                            false,
                            emptyList(),
                            null
                        )
                    ),
                    listOf(),
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
                createTable(
                    Identifier.regular("tbl"),
                    listOf(
                        columnDefinition(
                            regular("a"),
                            DataType.STRUCT(
                                listOf(
                                    DataType.StructField(
                                        regular("b"),
                                        DataType.STRUCT(
                                            listOf(
                                                DataType.StructField(
                                                    regular("c"),
                                                    DataType.INT2(),
                                                    false,
                                                    emptyList(),
                                                    null
                                                )
                                            )
                                        ),
                                        false,
                                        emptyList(),
                                        null
                                    ),
                                    DataType.StructField(
                                        regular("d"),
                                        DataType.ARRAY(
                                            DataType.INT2()
                                        ),
                                        false,
                                        emptyList(),
                                        null
                                    ),
                                )
                            ),
                            false,
                            emptyList(),
                            null
                        )
                    ),
                    listOf(),
                    null,
                    emptyList()
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with non-parameterized struct",
                """
                    CREATE TABLE tbl (
                        a STRUCT
                    )
                """.trimIndent(),
                createTable(
                    Identifier.regular("tbl"),
                    listOf(
                        columnDefinition(
                            regular("a"),
                            DataType.STRUCT(),
                            false,
                            emptyList(),
                            null
                        )
                    ),
                    listOf(),
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
                createTable(
                    Identifier.regular("tbl"),
                    listOf(
                        columnDefinition(
                            regular("a"),
                            DataType.ARRAY(DataType.INT2()),
                            false,
                            emptyList(),
                            null
                        )
                    ),
                    listOf(),
                    null,
                    emptyList()
                )
            ),
            SuccessTestCase(
                "CREATE TABLE with ARRAY of Struct",
                """
                    CREATE TABLE tbl (
                        a ARRAY<STRUCT< b:INT2 >>
                    )
                """.trimIndent(),
                createTable(
                    Identifier.regular("tbl"),
                    listOf(
                        columnDefinition(
                            regular("a"),
                            DataType.ARRAY(
                                DataType.STRUCT(
                                    listOf(
                                        DataType.StructField(
                                            regular("b"),
                                            DataType.INT2(),
                                            false,
                                            emptyList(),
                                            null
                                        ),
                                    )
                                ),
                            ),
                            false,
                            emptyList(),
                            null
                        )
                    ),
                    listOf(),
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
                createTable(
                    Identifier.regular("tbl"),
                    listOf(
                        columnDefinition(
                            regular("a"),
                            DataType.ARRAY(
                                DataType.STRUCT(
                                    listOf(
                                        DataType.StructField(
                                            regular("b"),
                                            DataType.INT2(),
                                            false,
                                            emptyList(),
                                            null
                                        ),
                                    )
                                ),
                            ),
                            false,
                            emptyList(),
                            null
                        )
                    ),
                    listOf(),
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
                createTable(
                    Identifier.regular("tbl"),
                    listOf(
                        columnDefinition(
                            regular("a"),
                            DataType.ARRAY(),
                            false,
                            emptyList(),
                            null
                        )
                    ),
                    listOf(),
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
                createTable(
                    Identifier.regular("tbl"),
                    listOf(
                        columnDefinition(
                            regular("a"),
                            DataType.INT2(),
                            true,
                            emptyList(),
                            null
                        )
                    ),
                    listOf(),
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
                createTable(
                    Identifier.regular("tbl"),
                    listOf(
                        columnDefinition(
                            regular("a"),
                            DataType.STRUCT(
                                listOf(
                                    DataType.StructField(
                                        regular("b"),
                                        DataType.INT2(),
                                        true,
                                        emptyList(),
                                        null
                                    ),
                                )
                            ),
                            false,
                            emptyList(),
                            null
                        )
                    ),
                    listOf(),
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
                createTable(
                    Identifier.regular("tbl"),
                    listOf(
                        columnDefinition(
                            regular("a"),
                            DataType.STRUCT(
                                listOf(
                                    DataType.StructField(
                                        regular("b"),
                                        DataType.INT2(),
                                        true,
                                        emptyList(),
                                        null
                                    ),
                                )
                            ),
                            true,
                            emptyList(),
                            null
                        )
                    ),
                    listOf(),
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
                createTable(
                    Identifier.regular("tbl"),
                    listOf(
                        columnDefinition(
                            regular("a"),
                            DataType.INT2(),
                            false,
                            emptyList(),
                            "attribute a"
                        )
                    ),
                    listOf(),
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
                createTable(
                    Identifier.regular("tbl"),
                    listOf(
                        columnDefinition(
                            regular("a"),
                            DataType.STRUCT(
                                listOf(
                                    DataType.StructField(
                                        regular("b"),
                                        DataType.INT2(),
                                        false,
                                        emptyList(),
                                        "comment on struct field"
                                    ),
                                )
                            ),
                            false,
                            emptyList(),
                            null
                        )
                    ),
                    listOf(),
                    null,
                    emptyList()
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with comment on struct and has comment on field",
                """
                    CREATE TABLE tbl (
                        a STRUCT<b : INT2 COMMENT 'comment on inner level'> COMMENT 'comment on top level'
                    )
                """.trimIndent(),
                createTable(
                    Identifier.regular("tbl"),
                    listOf(
                        columnDefinition(
                            regular("a"),
                            DataType.STRUCT(
                                listOf(
                                    DataType.StructField(
                                        regular("b"),
                                        DataType.INT2(),
                                        false,
                                        emptyList(),
                                        "comment on inner level"
                                    ),
                                )
                            ),
                            false,
                            emptyList(),
                            "comment on top level"
                        )
                    ),
                    listOf(),
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
                createTable(
                    Identifier.regular("tbl"),
                    listOf(),
                    listOf(),
                    partitionBy(
                        listOf(
                            regular("a"),
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
                createTable(
                    Identifier.regular("tbl"),
                    listOf(),
                    listOf(),
                    partitionBy(
                        listOf(
                            regular("a"),
                            regular("b"),
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
                createTable(
                    Identifier.regular("tbl"),
                    listOf(),
                    listOf(),
                    null,
                    listOf(
                        keyValue("k1", "v1")
                    )
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with TBLPROPERTIES preserve case sensitivity",
                """
                    CREATE TABLE tbl
                    TBLPROPERTIES ('K1k' = 'V1v')
                """.trimIndent(),
                createTable(
                    Identifier.regular("tbl"),
                    listOf(),
                    listOf(),
                    null,
                    listOf(
                        keyValue("K1k", "V1v")
                    )
                )
            ),

            SuccessTestCase(
                "CREATE TABLE with TBLPROPERTIES multiple properties",
                """
                    CREATE TABLE tbl
                    TBLPROPERTIES ('k1' = 'v1', 'k2' = 'v2')
                """.trimIndent(),
                createTable(
                    Identifier.regular("tbl"),
                    listOf(),
                    listOf(),
                    null,
                    listOf(
                        keyValue("k1", "v1"),
                        keyValue("k2", "v2")
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
                createTable(
                    Identifier.regular("tbl"),
                    listOf(),
                    listOf(),
                    partitionBy(listOf(regular("a"))),
                    listOf(
                        keyValue("k1", "v1"),
                    )
                )
            ),
        )

//        val dropTableTests = listOf(
//            SuccessTestCase(
//                "DROP TABLE with unqualified case-insensitive name",
//                "DROP TABLE foo",
//                statementDDLDropTable(
//                    identifierSymbol("foo", Identifier.CaseSensitivity.INSENSITIVE),
//                )
//            ),
//            SuccessTestCase(
//                "DROP TABLE with unqualified case-sensitive name",
//                "DROP TABLE \"foo\"",
//                statementDDLDropTable(
//                    identifierSymbol("foo", Identifier.CaseSensitivity.SENSITIVE),
//                )
//            ),
//            SuccessTestCase(
//                "DROP TABLE with qualified case-insensitive name",
//                "DROP TABLE myCatalog.mySchema.foo",
//                statementDDLDropTable(
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
//                statementDDLDropTable(
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

        private val allTest = createTableTestsExisting + createTableTestsAddition

        override fun provideArguments(context: ExtensionContext?): Stream<out Arguments> =
            allTest.map { Arguments.of(it) }.stream()
    }

    private fun assertExpression(input: String, expected: AstNode) {
        val result = parser.parse(input)
        assertEquals(1, result.statements.size)
        val actual = result.statements[0]
        assertEquals(expected, actual)
    }
}
