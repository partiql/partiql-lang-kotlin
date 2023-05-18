package org.partiql.lang.syntax

import org.junit.jupiter.api.Test

class PartiQLParserDDLTests : PartiQLParserTestBase() {

    @Test
    fun createTable() = assertExpression(
        "CREATE TABLE foo",
        "(ddl (create_table (table_name_prefix) foo null))"
    )

    @Test
    fun createTableWithOnePrefix() = assertExpression(
        "CREATE TABLE mySchema.foo",
        "(ddl (create_table (table_name_prefix (identifier mySchema (case_insensitive))) foo null))"
    )

    @Test
    fun createTableWithCaseSensitivePrefix() = assertExpression(
        "CREATE TABLE \"mySchema\".foo",
        "(ddl (create_table (table_name_prefix (identifier mySchema (case_sensitive))) foo null))"
    )

    @Test
    fun createTableWithMultiplePrefix() = assertExpression(
        "CREATE TABLE myCatalog.\"mySchema\".foo",
        "(ddl (create_table (table_name_prefix (identifier myCatalog (case_insensitive)) (identifier mySchema (case_sensitive))) foo null))"
    )

    @Test
    fun createTableWithColumn() = assertExpression(
        "CREATE TABLE foo (boo string)",
        """
            (ddl (create_table (table_name_prefix) foo  (table_def
                (column_declaration boo (string_type) null))))
        """.trimIndent()
    )

    @Test
    fun createTableWithQuotedIdentifier() = assertExpression(
        "CREATE TABLE \"user\" (\"lastname\" string)",
        """
            (ddl (create_table (table_name_prefix) user (table_def
                (column_declaration lastname (string_type) null))))
        """.trimIndent()
    )

    @Test
    fun createTableWithConstraints() = assertExpression(
        """
            CREATE TABLE Customer (
               name string CONSTRAINT name_is_present NOT NULL, 
               age int,
               city string NULL,
               state string NULL
            )
        """.trimIndent(),
        """
            (ddl
                (create_table
                    (table_name_prefix)
                    Customer (table_def
                        (column_declaration name (string_type)
                            null 
                            (column_constraint name_is_present (column_notnull)))
                        (column_declaration age (integer_type) null)
                        (column_declaration city (string_type)
                            null 
                            (column_constraint null (column_null)))
                        (column_declaration state (string_type)
                            null 
                            (column_constraint null (column_null))))))
        """.trimIndent()
    )

    @Test
    fun createTableWithCheckConstraint() = assertExpression(
        """
            CREATE TABLE Customer (
               name string CONSTRAINT name_is_present NOT NULL, 
               age int CHECK (age >= 18),
               city string NULL,
               state string NULL
            )
        """.trimIndent(),
        """
            (ddl
                (create_table
                    (table_name_prefix)
                    Customer (table_def
                        (column_declaration name (string_type)
                            null
                            (column_constraint name_is_present (column_notnull)))
                        (column_declaration age (integer_type)
                            null
                            (column_constraint
                                null
                                (column_check
                                    (gte
                                        (id age (case_insensitive) (unqualified))
                                        (lit 18)
                                    )
                                )
                            )
                        )
                        (column_declaration city (string_type)
                            null
                            (column_constraint null (column_null)))
                        (column_declaration state (string_type)
                            null
                            (column_constraint null (column_null))))))
        """.trimIndent()
    )

    @Test
    fun createTableWithMultipleConstraints() = assertExpression(
        """
            CREATE TABLE Customer (
               name string CONSTRAINT name_is_present NOT NULL, 
               age int CHECK (age >= 18) CHECK (age <=100) NOT NULL,
               city string NULL,
               state string NULL
            )
        """.trimIndent(),
        """
            (ddl
                (create_table
                    (table_name_prefix)
                    Customer (table_def
                        (column_declaration name (string_type)
                            null
                            (column_constraint name_is_present (column_notnull)))
                        (column_declaration age (integer_type)
                            null
                            (column_constraint
                                null
                                (column_check
                                    (gte
                                        (id age (case_insensitive) (unqualified))
                                        (lit 18)
                                    )
                                )
                            )
                            (column_constraint
                                null
                                (column_check
                                    (lte
                                        (id age (case_insensitive) (unqualified))
                                        (lit 100)
                                    )
                                )
                            )
                            (column_constraint
                                null
                                (column_notnull)
                            )
                        )
                        (column_declaration city (string_type)
                            null
                            (column_constraint null (column_null)))
                        (column_declaration state (string_type)
                            null
                            (column_constraint null (column_null))))))
        """.trimIndent()
    )

    @Test
    fun createTableWithDefaultClauseLiteral() = assertExpression(
        """
            CREATE TABLE Customer (
               name string CONSTRAINT name_is_present NOT NULL, 
               age int DEFAULT 18 NOT NULL,
               city string NULL,
               state string NULL
            )
        """.trimIndent(),
        """
            (ddl
                (create_table
                    (table_name_prefix)
                    Customer (table_def
                        (column_declaration name (string_type)
                            null
                            (column_constraint name_is_present (column_notnull)))
                        (column_declaration age (integer_type)
                            (lit 18)
                            (column_constraint
                                null
                                (column_notnull)
                            )
                        )
                        (column_declaration city (string_type)
                            null
                            (column_constraint null (column_null)))
                        (column_declaration state (string_type)
                            null
                            (column_constraint null (column_null))))))
        """.trimIndent()
    )

    @Test
    fun createTableWithDefaultClauseSession() = assertExpression(
        """
            CREATE TABLE Customer (
               name string CONSTRAINT name_is_present NOT NULL, 
               age int DEFAULT 18,
               city string NULL,
               state string NULL,
               issuer string DEFAULT CURRENT_USER
            )
        """.trimIndent(),
        """
            (ddl
                (create_table
                    (table_name_prefix)
                    Customer (table_def
                        (column_declaration name (string_type)
                            null
                            (column_constraint name_is_present (column_notnull)))
                        (column_declaration age (integer_type)
                            (lit 18)
                        )
                        (column_declaration city (string_type)
                            null
                            (column_constraint null (column_null)))
                        (column_declaration state (string_type)
                            null
                            (column_constraint null (column_null)))
                        (column_declaration issuer (string_type)
                            (session_attribute CURRENT_USER)))))
        """.trimIndent()
    )

    @Test
    fun dropTable() = assertExpression(
        "DROP TABLE foo",
        "(ddl (drop_table (table_name_prefix) (identifier foo (case_insensitive))))"
    )

    @Test
    fun dropTableWithQuotedIdentifier() = assertExpression(
        "DROP TABLE \"user\"",
        "(ddl (drop_table (table_name_prefix) (identifier user (case_sensitive))))"
    )

    @Test
    fun dropTableWithOnePrefix() = assertExpression(
        "DROP TABLE mySchema.foo",
        "(ddl (drop_table (table_name_prefix (identifier mySchema (case_insensitive))) (identifier foo (case_insensitive))))"
    )

    @Test
    fun dropTableWithCaseSensitivePrefix() = assertExpression(
        "DROP TABLE \"mySchema\".foo",
        "(ddl (drop_table (table_name_prefix (identifier mySchema (case_sensitive))) (identifier foo (case_insensitive))))"
    )

    @Test
    fun dropTableWithMultiplePrefix() = assertExpression(
        "DROP TABLE myCatalog.\"mySchema\".\"Foo\"",
        "(ddl (drop_table (table_name_prefix (identifier myCatalog (case_insensitive)) (identifier mySchema (case_sensitive))) (identifier Foo (case_sensitive))))"
    )

    @Test
    fun createIndex() = assertExpression(
        "CREATE INDEX ON foo (x, y.z)",
        """
        (ddl
          (create_index
            (identifier foo (case_insensitive))
            (id x (case_insensitive) (unqualified))
            (path (id y (case_insensitive) (unqualified)) (path_expr (lit "z") (case_insensitive)))))
        """
    )

    @Test
    fun createIndexWithQuotedIdentifiers() = assertExpression(
        "CREATE INDEX ON \"user\" (\"group\")",
        """
        (ddl
          (create_index
            (identifier user (case_sensitive))
            (id group (case_sensitive) (unqualified))))
        """
    )

    @Test
    fun dropIndex() = assertExpression(
        "DROP INDEX bar ON foo",
        "(ddl (drop_index (table (identifier foo (case_insensitive))) (keys (identifier bar (case_insensitive)))))"
    )

    @Test
    fun dropIndexWithQuotedIdentifiers() = assertExpression(
        "DROP INDEX \"bar\" ON \"foo\"",
        "(ddl (drop_index (table (identifier foo (case_sensitive))) (keys (identifier bar (case_sensitive)))))"
    )
}
