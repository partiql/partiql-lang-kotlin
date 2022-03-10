package org.partiql.cli.prettyprintAST

import org.junit.Assert
import org.junit.Test


class PrettyPrinterTest {
    private val prettyPrinter = PrettyPrinter()

    private fun checkPrettyPrintAst(query: String, expected: String) {
        // In triples quotes, a tab consists of 4 whitespaces. We need to transform them into a tab.
        val newExpected = expected.replace("    ", "\t")
        Assert.assertEquals(newExpected, prettyPrinter.prettyPrintAST(query))
    }

    @Test
    fun selectFrom(){
        checkPrettyPrintAst(
            "SELECT * FROM 1",
            """
                Select
                    project: *
                    from: Scan
                        Lit 1
            """.trimIndent()
        )
    }

    @Test
    fun selectFromLet(){
        checkPrettyPrintAst(
            "SELECT * FROM 1 LET 1 AS a",
            """
                Select
                    project: *
                    from: Scan
                        Lit 1
                    let: Let
                        LetBinding
                            expr: Lit 1
                            name: Symbol a
            """.trimIndent()
        )
    }

    @Test
    fun selectFromWhere(){
        checkPrettyPrintAst(
            "SELECT * FROM 1 WHERE a = b",
            """
                Select
                    project: *
                    from: Scan
                        Lit 1
                    where: =
                        Id a
                        Id b
            """.trimIndent()
        )
    }

    @Test
    fun selectFromWhereGroupHaving(){
        checkPrettyPrintAst(
            "SELECT * FROM 1 WHERE a = b GROUP BY c HAVING d = '123'",
            """
                Select
                    project: *
                    from: Scan
                        Lit 1
                    where: =
                        Id a
                        Id b
                    group: Group
                        strategy: GroupFull
                        keyList: GroupKeyList
                            key: GroupKey
                                expr: Id c
                    having: =
                        Id d
                        Lit "123"
            """.trimIndent()
        )
    }

    @Test
    fun selectFromWhereGroupHavingLimitOffset(){
        checkPrettyPrintAst(
            "SELECT * FROM 1 WHERE a = b GROUP BY c HAVING d = '123' LIMIT 3 OFFSET 4",
            """
                Select
                    project: *
                    from: Scan
                        Lit 1
                    where: =
                        Id a
                        Id b
                    group: Group
                        strategy: GroupFull
                        keyList: GroupKeyList
                            key: GroupKey
                                expr: Id c
                    having: =
                        Id d
                        Lit "123"
                    limit: Lit 3
                    offset: Lit 4
            """.trimIndent()
        )
    }

    @Test
    fun selectFromWhereGroupHavingLimitOffsetWithSubQuery(){
        checkPrettyPrintAst(
            "SELECT (SELECT * FROM foo WHERE bar = 1) FROM 1 WHERE a = b GROUP BY c HAVING d = '123' LIMIT 3 OFFSET 4",
            """
                Select
                    project: ProjectList
                        projectItem: ProjectExpr
                            expr: Select
                                project: *
                                from: Scan
                                    Id foo
                                where: =
                                    Id bar
                                    Lit 1
                    from: Scan
                        Lit 1
                    where: =
                        Id a
                        Id b
                    group: Group
                        strategy: GroupFull
                        keyList: GroupKeyList
                            key: GroupKey
                                expr: Id c
                    having: =
                        Id d
                        Lit "123"
                    limit: Lit 3
                    offset: Lit 4
            """.trimIndent()
        )
    }
}