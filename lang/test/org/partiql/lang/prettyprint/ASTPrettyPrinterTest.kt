package org.partiql.lang.prettyprint

import org.junit.Assert
import org.junit.Test

class ASTPrettyPrinterTest {
    private val prettyPrinter = ASTPrettyPrinter()

    private fun checkPrettyPrintAst(query: String, expected: String) {
        // In triples quotes, a tab consists of 4 whitespaces. We need to transform them into a tab.
        val newExpected = expected.replace("    ", "\t")
        Assert.assertEquals(newExpected, prettyPrinter.prettyPrintAST(query))
    }

    // ********
    // * EXEC *
    // ********
    @Test
    fun exec() {
        checkPrettyPrintAst(
            "EXEC foo 'bar0', `1d0`, 2, [3], SELECT baz FROM bar",
            """
                Exec
                    procedureName: Symbol foo
                    arg1: Lit "bar0"
                    arg2: Lit 1.
                    arg3: Lit 2
                    arg4: List
                        Lit 3
                    arg5: Select
                        project: ProjectList
                            projectItem1: ProjectExpr
                                expr: Id baz (case_insensitive) (unqualified)
                        from: Scan
                            Id bar (case_insensitive) (unqualified)
            """.trimIndent()
        )
    }

    // *******
    // * DDL *
    // *******
    @Test
    fun createIndex() {
        checkPrettyPrintAst(
            "CREATE INDEX ON foo (x, y.z)",
            """
                Ddl
                    op: CreateIndex
                        indexName: Identifier foo (case_insensitive)
                        field1: Id x (case_insensitive) (unqualified)
                        field2: Path
                            root: Id y (case_insensitive) (unqualified)
                            step1: Lit "z"
            """.trimIndent()
        )
    }

    @Test
    fun createTable() {
        checkPrettyPrintAst(
            "CREATE TABLE foo",
            """
                Ddl
                    op: CreateTable
                        tableName: Symbol foo
            """.trimIndent()
        )
    }

    @Test
    fun dropIndex() {
        checkPrettyPrintAst(
            "DROP INDEX bar ON foo",
            """
                Ddl
                    op: DropIndex
                        table: Identifier foo (case_insensitive)
                        keys: Identifier bar (case_insensitive)
            """.trimIndent()
        )
    }

    @Test
    fun dropTable() {
        checkPrettyPrintAst(
            "DROP TABLE foo",
            """
                Ddl
                    op: DropTable
                        tableName: Identifier foo (case_insensitive)
            """.trimIndent()
        )
    }

    // *******
    // * Dml *
    // *******
    @Test
    fun insert() {
        checkPrettyPrintAst(
            "INSERT INTO foo VALUES (1, 2), (3, 4)",
            """
                Dml
                    operations: DmlOpList
                        op1: Insert
                            target: Id foo (case_insensitive) (unqualified)
                            values: Bag
                                List
                                    Lit 1
                                    Lit 2
                                List
                                    Lit 3
                                    Lit 4
            """.trimIndent()
        )
    }

    @Test
    fun insertValue() {
        checkPrettyPrintAst(
            "INSERT INTO foo VALUE 1 AT bar ON CONFLICT WHERE a DO NOTHING",
            """
                Dml
                    operations: DmlOpList
                        op1: InsertValue
                            target: Id foo (case_insensitive) (unqualified)
                            value: Lit 1
                            index: Id bar (case_insensitive) (unqualified)
                            onConflict: OnConflict
                                expr: Id a (case_insensitive) (unqualified)
                                conflictAction: DoNothing
            """.trimIndent()
        )
    }

    @Test
    fun set() {
        checkPrettyPrintAst(
            "UPDATE x SET k.m = 5",
            """
                Dml
                    operations: DmlOpList
                        op1: Set
                            assignment: Assignment
                                target: Path
                                    root: Id k (case_insensitive) (unqualified)
                                    step1: Lit "m"
                                value: Lit 5
                    from: Scan
                        Id x (case_insensitive) (unqualified)
            """.trimIndent()
        )
    }

    @Test
    fun remove() {
        checkPrettyPrintAst(
            "FROM x WHERE a = b REMOVE y",
            """
                Dml
                    operations: DmlOpList
                        op1: Remove
                            target: Id y (case_insensitive) (unqualified)
                    from: Scan
                        Id x (case_insensitive) (unqualified)
                    where: =
                        Id a (case_insensitive) (unqualified)
                        Id b (case_insensitive) (unqualified)
            """.trimIndent()
        )
    }

    @Test
    fun delete() {
        checkPrettyPrintAst(
            "DELETE FROM y",
            """
                Dml
                    operations: DmlOpList
                        op1: Delete
                    from: Scan
                        Id y (case_insensitive) (unqualified)
            """.trimIndent()
        )
    }

    @Test
    fun longDmlQuery() {
        checkPrettyPrintAst(
            "UPDATE x SET k = 5, m = 6 INSERT INTO c VALUE << 1 >> REMOVE a SET l = 3 REMOVE b WHERE a = b RETURNING MODIFIED OLD a",
            """
                Dml
                    operations: DmlOpList
                        op1: Set
                            assignment: Assignment
                                target: Id k (case_insensitive) (unqualified)
                                value: Lit 5
                        op2: Set
                            assignment: Assignment
                                target: Id m (case_insensitive) (unqualified)
                                value: Lit 6
                        op3: InsertValue
                            target: Id c (case_insensitive) (unqualified)
                            value: Bag
                                Lit 1
                        op4: Remove
                            target: Id a (case_insensitive) (unqualified)
                        op5: Set
                            assignment: Assignment
                                target: Id l (case_insensitive) (unqualified)
                                value: Lit 3
                        op6: Remove
                            target: Id b (case_insensitive) (unqualified)
                    from: Scan
                        Id x (case_insensitive) (unqualified)
                    where: =
                        Id a (case_insensitive) (unqualified)
                        Id b (case_insensitive) (unqualified)
                    returning: ReturningExpr
                        elem1: ReturningElem
                            mapping: ModifiedOld
                            column: ReturningColumn
            """.trimIndent()
        )
    }

    // *********
    // * Query *
    // *********
    @Test
    fun id() {
        checkPrettyPrintAst(
            "a",
            """
                Id a (case_insensitive) (unqualified)
            """.trimIndent()
        )
    }

    @Test
    fun missing() {
        checkPrettyPrintAst(
            "MISSING",
            """
                missing
            """.trimIndent()
        )
    }

    @Test
    fun litNumber() {
        checkPrettyPrintAst(
            "1",
            """
                Lit 1
            """.trimIndent()
        )
    }

    @Test
    fun litString() {
        checkPrettyPrintAst(
            "'1'",
            """
                Lit "1"
            """.trimIndent()
        )
    }

    @Test
    fun litTimestamp() {
        checkPrettyPrintAst(
            "`2017-01-10T05:30:55Z`",
            """
                Lit 2017-01-10T05:30:55Z
            """.trimIndent()
        )
    }

    @Test
    fun parameter() {
        checkPrettyPrintAst(
            "?",
            """
                Parameter 1
            """.trimIndent()
        )
    }

    @Test
    fun date() {
        checkPrettyPrintAst(
            "DATE '2022-03-16'",
            """
                Date 2022-3-16
            """.trimIndent()
        )
    }

    @Test
    fun litTimeTime() {
        checkPrettyPrintAst(
            "Time '01:02:03'",
            """
                LitTime 1:2:3.0, 'precision': 0, 'timeZone': false, 'tzminute': null
            """.trimIndent()
        )
    }

    @Test
    fun litTimeTimeWithTimeZone() {
        checkPrettyPrintAst(
            "Time With Time Zone '01:02:03-05:30'",
            """
                LitTime 1:2:3.0, 'precision': 0, 'timeZone': true, 'tzminute': -330
            """.trimIndent()
        )
    }

    @Test
    fun not() {
        checkPrettyPrintAst(
            "NOT TRUE",
            """
                Not
                    Lit true
            """.trimIndent()
        )
    }

    @Test
    fun pos() {
        checkPrettyPrintAst(
            "+function1()",
            """
                +
                    Call function1
            """.trimIndent()
        )
    }

    @Test
    fun neg() {
        checkPrettyPrintAst(
            "-function1()",
            """
                -
                    Call function1
            """.trimIndent()
        )
    }

    @Test
    fun plus() {
        checkPrettyPrintAst(
            "1 + 2",
            """
                +
                    Lit 1
                    Lit 2
            """.trimIndent()
        )
    }

    @Test
    fun minus() {
        checkPrettyPrintAst(
            "3 - 2",
            """
                -
                    Lit 3
                    Lit 2
            """.trimIndent()
        )
    }

    @Test
    fun times() {
        checkPrettyPrintAst(
            "1 * 2",
            """
                *
                    Lit 1
                    Lit 2
            """.trimIndent()
        )
    }

    @Test
    fun divide() {
        checkPrettyPrintAst(
            "4 / 2",
            """
                /
                    Lit 4
                    Lit 2
            """.trimIndent()
        )
    }

    @Test
    fun modulo() {
        checkPrettyPrintAst(
            "3 % 2",
            """
                %
                    Lit 3
                    Lit 2
            """.trimIndent()
        )
    }

    @Test
    fun concat() {
        checkPrettyPrintAst(
            "'1' || '2'",
            """
                ||
                    Lit "1"
                    Lit "2"
            """.trimIndent()
        )
    }

    @Test
    fun and() {
        checkPrettyPrintAst(
            "TRUE AND FALSE",
            """
                And
                    Lit true
                    Lit false
            """.trimIndent()
        )
    }

    @Test
    fun or() {
        checkPrettyPrintAst(
            "TRUE OR FALSE",
            """
                Or
                    Lit true
                    Lit false
            """.trimIndent()
        )
    }

    @Test
    fun eq() {
        checkPrettyPrintAst(
            "1 = 2",
            """
                =
                    Lit 1
                    Lit 2
            """.trimIndent()
        )
    }

    @Test
    fun ne() {
        checkPrettyPrintAst(
            "1 != 2",
            """
                !=
                    Lit 1
                    Lit 2
            """.trimIndent()
        )
    }

    @Test
    fun gt() {
        checkPrettyPrintAst(
            "2 > 1",
            """
                >
                    Lit 2
                    Lit 1
            """.trimIndent()
        )
    }

    @Test
    fun gte() {
        checkPrettyPrintAst(
            "2 >= 1",
            """
                >=
                    Lit 2
                    Lit 1
            """.trimIndent()
        )
    }

    @Test
    fun lt() {
        checkPrettyPrintAst(
            "1 < 2",
            """
                <
                    Lit 1
                    Lit 2
            """.trimIndent()
        )
    }

    @Test
    fun lte() {
        checkPrettyPrintAst(
            "1 <= 2",
            """
                <=
                    Lit 1
                    Lit 2
            """.trimIndent()
        )
    }

    @Test
    fun inCollection() {
        checkPrettyPrintAst(
            "1 IN [1, 2, 3]",
            """
                In
                    Lit 1
                    List
                        Lit 1
                        Lit 2
                        Lit 3
            """.trimIndent()
        )
    }

    @Test
    fun union() {
        checkPrettyPrintAst(
            "a UNION b",
            """
                Union
                    Id a (case_insensitive) (unqualified)
                    Id b (case_insensitive) (unqualified)
            """.trimIndent()
        )
    }

    @Test
    fun except() {
        checkPrettyPrintAst(
            "a EXCEPT b",
            """
                Except
                    Id a (case_insensitive) (unqualified)
                    Id b (case_insensitive) (unqualified)
            """.trimIndent()
        )
    }

    @Test
    fun intersect() {
        checkPrettyPrintAst(
            "a INTERSECT b",
            """
                Intersect
                    Id a (case_insensitive) (unqualified)
                    Id b (case_insensitive) (unqualified)
            """.trimIndent()
        )
    }

    @Test
    fun like() {
        checkPrettyPrintAst(
            "a LIKE b",
            """
                Like
                    value: Id a (case_insensitive) (unqualified)
                    pattern: Id b (case_insensitive) (unqualified)
            """.trimIndent()
        )
    }

    @Test
    fun between() {
        checkPrettyPrintAst(
            "5 BETWEEN 1 AND 10",
            """
                Between
                    value: Lit 5
                    from: Lit 1
                    to: Lit 10
            """.trimIndent()
        )
    }

    @Test
    fun simpleCase() {
        checkPrettyPrintAst(
            "CASE name WHEN 'jack' THEN 1 END",
            """
                SimpleCase
                    expr: Id name (case_insensitive) (unqualified)
                    cases: ExprPairList
                        pair1: Pair
                            first: Lit "jack"
                            second: Lit 1
            """.trimIndent()
        )
    }

    @Test
    fun searchedCase() {
        checkPrettyPrintAst(
            "CASE WHEN name = 'jack' THEN 1 END",
            """
                SearchedCase
                    cases: ExprPairList
                        pair1: Pair
                            first: =
                                Id name (case_insensitive) (unqualified)
                                Lit "jack"
                            second: Lit 1
            """.trimIndent()
        )
    }

    @Test
    fun struct() {
        checkPrettyPrintAst(
            "{ a: 1 }",
            """
                Struct
                    field1: Pair
                        first: Id a (case_insensitive) (unqualified)
                        second: Lit 1
            """.trimIndent()
        )
    }

    @Test
    fun bag() {
        checkPrettyPrintAst(
            "<< 1, 2, 3 >>",
            """
                Bag
                    Lit 1
                    Lit 2
                    Lit 3
            """.trimIndent()
        )
    }

    @Test
    fun list() {
        checkPrettyPrintAst(
            "[ 1, 2, 3 ]",
            """
                List
                    Lit 1
                    Lit 2
                    Lit 3
            """.trimIndent()
        )
    }

    @Test
    fun sexp() {
        checkPrettyPrintAst(
            "sexp(1, 2, 3)",
            """
                Sexp
                    Lit 1
                    Lit 2
                    Lit 3
            """.trimIndent()
        )
    }

    @Test
    fun path() {
        checkPrettyPrintAst(
            "a.b",
            """
                Path
                    root: Id a (case_insensitive) (unqualified)
                    step1: Lit "b"
            """.trimIndent()
        )
    }

    @Test
    fun call() {
        checkPrettyPrintAst(
            "function1(1)",
            """
                Call function1
                    arg: Lit 1
            """.trimIndent()
        )
    }

    @Test
    fun callAgg() {
        checkPrettyPrintAst(
            "SUM(a)",
            """
                CallAgg sum
                    arg: Id a (case_insensitive) (unqualified)
            """.trimIndent()
        )
    }

    @Test
    fun isType() {
        checkPrettyPrintAst(
            "1 IS INT",
            """
                Is
                    value: Lit 1
                    type: (scalar_type int)
            """.trimIndent()
        )
    }

    @Test
    fun cast() {
        checkPrettyPrintAst(
            "CAST (1 AS STRING)",
            """
                Cast
                    value: Lit 1
                    asType: (scalar_type string)
            """.trimIndent()
        )
    }

    @Test
    fun canCast() {
        checkPrettyPrintAst(
            "CAN_CAST (1 AS STRING)",
            """
                CanCast
                    value: Lit 1
                    asType: (scalar_type string)
            """.trimIndent()
        )
    }

    @Test
    fun canLosslessCast() {
        checkPrettyPrintAst(
            "CAN_Lossless_CAST (1 AS STRING)",
            """
                CanLosslessCast
                    value: Lit 1
                    asType: (scalar_type string)
            """.trimIndent()
        )
    }

    @Test
    fun nullIf() {
        checkPrettyPrintAst(
            "NULLIF(1, 2)",
            """
                NullIf
                    expr1: Lit 1
                    expr2: Lit 2
            """.trimIndent()
        )
    }

    @Test
    fun coalesce() {
        checkPrettyPrintAst(
            "COALESCE(1, 2)",
            """
                Coalesce
                    arg: Lit 1
                    arg: Lit 2
            """.trimIndent()
        )
    }

    // Select
    @Test
    fun selectFrom() {
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
    fun selectFromLet() {
        checkPrettyPrintAst(
            "SELECT * FROM 1 LET 1 AS a",
            """
                Select
                    project: *
                    from: Scan
                        Lit 1
                    let: Let
                        letBinding1: LetBinding
                            expr: Lit 1
                            name: Symbol a
            """.trimIndent()
        )
    }

    @Test
    fun selectFromWhere() {
        checkPrettyPrintAst(
            "SELECT * FROM 1 WHERE a = b",
            """
                Select
                    project: *
                    from: Scan
                        Lit 1
                    where: =
                        Id a (case_insensitive) (unqualified)
                        Id b (case_insensitive) (unqualified)
            """.trimIndent()
        )
    }

    @Test
    fun selectFromWhereGroupHaving() {
        checkPrettyPrintAst(
            "SELECT * FROM 1 WHERE a = b GROUP BY c HAVING d = '123'",
            """
                Select
                    project: *
                    from: Scan
                        Lit 1
                    where: =
                        Id a (case_insensitive) (unqualified)
                        Id b (case_insensitive) (unqualified)
                    group: Group
                        strategy: GroupFull
                        keyList: GroupKeyList
                            key1: GroupKey
                                expr: Id c (case_insensitive) (unqualified)
                    having: =
                        Id d (case_insensitive) (unqualified)
                        Lit "123"
            """.trimIndent()
        )
    }

    @Test
    fun selectFromWhereGroupHavingLimitOffset() {
        checkPrettyPrintAst(
            "SELECT * FROM 1 WHERE a = b GROUP BY c HAVING d = '123' LIMIT 3 OFFSET 4",
            """
                Select
                    project: *
                    from: Scan
                        Lit 1
                    where: =
                        Id a (case_insensitive) (unqualified)
                        Id b (case_insensitive) (unqualified)
                    group: Group
                        strategy: GroupFull
                        keyList: GroupKeyList
                            key1: GroupKey
                                expr: Id c (case_insensitive) (unqualified)
                    having: =
                        Id d (case_insensitive) (unqualified)
                        Lit "123"
                    limit: Lit 3
                    offset: Lit 4
            """.trimIndent()
        )
    }

    @Test
    fun selectFromWhereGroupHavingLimitOffsetWithSubQuery() {
        checkPrettyPrintAst(
            "SELECT (SELECT * FROM foo WHERE bar = 1) FROM 1 WHERE a = b GROUP BY c HAVING d = '123' LIMIT 3 OFFSET 4",
            """
                Select
                    project: ProjectList
                        projectItem1: ProjectExpr
                            expr: Select
                                project: *
                                from: Scan
                                    Id foo (case_insensitive) (unqualified)
                                where: =
                                    Id bar (case_insensitive) (unqualified)
                                    Lit 1
                    from: Scan
                        Lit 1
                    where: =
                        Id a (case_insensitive) (unqualified)
                        Id b (case_insensitive) (unqualified)
                    group: Group
                        strategy: GroupFull
                        keyList: GroupKeyList
                            key1: GroupKey
                                expr: Id c (case_insensitive) (unqualified)
                    having: =
                        Id d (case_insensitive) (unqualified)
                        Lit "123"
                    limit: Lit 3
                    offset: Lit 4
            """.trimIndent()
        )
    }
}
