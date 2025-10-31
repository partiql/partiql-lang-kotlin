package org.partiql.ast.sql

import com.amazon.ion.Decimal
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionDecimal
import com.amazon.ionelement.api.ionFloat
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionNull
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionSymbol
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.ast.Ast
import org.partiql.ast.Ast.exclude
import org.partiql.ast.Ast.excludePath
import org.partiql.ast.Ast.excludeStepCollIndex
import org.partiql.ast.Ast.excludeStepCollWildcard
import org.partiql.ast.Ast.excludeStepStructField
import org.partiql.ast.Ast.excludeStepStructWildcard
import org.partiql.ast.Ast.exprArray
import org.partiql.ast.Ast.exprBag
import org.partiql.ast.Ast.exprBetween
import org.partiql.ast.Ast.exprBoolTest
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCase
import org.partiql.ast.Ast.exprCaseBranch
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprCoalesce
import org.partiql.ast.Ast.exprExtract
import org.partiql.ast.Ast.exprInCollection
import org.partiql.ast.Ast.exprIsType
import org.partiql.ast.Ast.exprLike
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprMissingPredicate
import org.partiql.ast.Ast.exprNot
import org.partiql.ast.Ast.exprNullIf
import org.partiql.ast.Ast.exprNullPredicate
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.Ast.exprOverlaps
import org.partiql.ast.Ast.exprOverlay
import org.partiql.ast.Ast.exprPath
import org.partiql.ast.Ast.exprPathStepAllElements
import org.partiql.ast.Ast.exprPathStepAllFields
import org.partiql.ast.Ast.exprPathStepElement
import org.partiql.ast.Ast.exprPathStepField
import org.partiql.ast.Ast.exprPosition
import org.partiql.ast.Ast.exprQuerySet
import org.partiql.ast.Ast.exprRowValue
import org.partiql.ast.Ast.exprStruct
import org.partiql.ast.Ast.exprStructField
import org.partiql.ast.Ast.exprSubstring
import org.partiql.ast.Ast.exprTrim
import org.partiql.ast.Ast.exprValues
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.Ast.exprVariant
import org.partiql.ast.Ast.exprWindowFunction
import org.partiql.ast.Ast.from
import org.partiql.ast.Ast.fromExpr
import org.partiql.ast.Ast.fromJoin
import org.partiql.ast.Ast.groupBy
import org.partiql.ast.Ast.groupByKey
import org.partiql.ast.Ast.letBinding
import org.partiql.ast.Ast.orderBy
import org.partiql.ast.Ast.queryBodySFW
import org.partiql.ast.Ast.queryBodySetOp
import org.partiql.ast.Ast.selectItemExpr
import org.partiql.ast.Ast.selectItemStar
import org.partiql.ast.Ast.selectList
import org.partiql.ast.Ast.selectPivot
import org.partiql.ast.Ast.selectStar
import org.partiql.ast.Ast.selectValue
import org.partiql.ast.Ast.setOp
import org.partiql.ast.Ast.sort
import org.partiql.ast.Ast.windowClause
import org.partiql.ast.Ast.windowClauseDefinition
import org.partiql.ast.Ast.windowPartition
import org.partiql.ast.Ast.windowSpecification
import org.partiql.ast.AstNode
import org.partiql.ast.DataType
import org.partiql.ast.DatetimeField
import org.partiql.ast.Exclude
import org.partiql.ast.From
import org.partiql.ast.FromType
import org.partiql.ast.GroupBy
import org.partiql.ast.GroupByStrategy
import org.partiql.ast.Identifier
import org.partiql.ast.Identifier.Simple.delimited
import org.partiql.ast.Identifier.Simple.regular
import org.partiql.ast.IntervalQualifier
import org.partiql.ast.JoinType
import org.partiql.ast.Let
import org.partiql.ast.Literal.approxNum
import org.partiql.ast.Literal.bool
import org.partiql.ast.Literal.exactNum
import org.partiql.ast.Literal.intNum
import org.partiql.ast.Literal.missing
import org.partiql.ast.Literal.nul
import org.partiql.ast.Literal.string
import org.partiql.ast.Literal.typedString
import org.partiql.ast.Nulls
import org.partiql.ast.Order
import org.partiql.ast.OrderBy
import org.partiql.ast.QueryBody
import org.partiql.ast.Select
import org.partiql.ast.SetOpType
import org.partiql.ast.SetQuantifier
import org.partiql.ast.WindowClause
import org.partiql.ast.WindowFunctionType
import org.partiql.ast.With
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.TrimSpec
import org.partiql.ast.expr.TruthValue
import java.math.BigDecimal
import kotlin.test.assertFails

/**
 * This tests the Ast to test via the base SqlDialect.
 *
 * It does NOT test formatted output.
 */
class SqlDialectTest {

    // Identifiers & Paths

    @ParameterizedTest(name = "identifiers #{index}")
    @MethodSource("identifiers")
    @Execution(ExecutionMode.CONCURRENT)
    fun testIdentifiers(case: Case) = case.assert()

    // Types

    @ParameterizedTest(name = "types #{index}")
    @MethodSource("types")
    @Execution(ExecutionMode.CONCURRENT)
    fun testTypes(case: Case) = case.assert()

    // Expressions

    @ParameterizedTest(name = "expr.lit #{index}")
    @MethodSource("exprLitCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testExprLit(case: Case) = case.assert()

    @ParameterizedTest(name = "expr.ion #{index}")
    @MethodSource("exprIonCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testExprIon(case: Case) = case.assert()

    @ParameterizedTest(name = "expr.var #{index}")
    @MethodSource("exprVarCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testExprVar(case: Case) = case.assert()

    @ParameterizedTest(name = "expr.path #{index}")
    @MethodSource("exprPathCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testExprPath(case: Case) = case.assert()

    @ParameterizedTest()
    @MethodSource("exprOperators")
    @Execution(ExecutionMode.CONCURRENT)
    fun testExprOperators(case: Case) = case.assert()

    @ParameterizedTest(name = "expr.call #{index}")
    @MethodSource("exprCallCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testExprCall(case: Case) = case.assert()

    @ParameterizedTest(name = "expr.agg #{index}")
    @MethodSource("exprAggCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testExprAgg(case: Case) = case.assert()

    @ParameterizedTest(name = "expr.collection #{index}")
    @MethodSource("exprCollectionCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testExprCollection(case: Case) = case.assert()

    @ParameterizedTest(name = "expr.struct #{index}")
    @MethodSource("exprStructCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testExprStruct(case: Case) = case.assert()

    @ParameterizedTest(name = "special form #{index}")
    @MethodSource("exprSpecialFormCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testExprSpecialForm(case: Case) = case.assert()

    @ParameterizedTest(name = "expr.case #{index}")
    @MethodSource("exprCaseCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testExprCase(case: Case) = case.assert()

    // SELECT-FROM-WHERE

    @ParameterizedTest(name = "SELECT Clause #{index}")
    @MethodSource("selectClauseCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testSelectClause(case: Case) = case.assert()

    @ParameterizedTest(name = "EXCLUDE Clause #{index}")
    @MethodSource("excludeClauseCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testExcludeClause(case: Case) = case.assert()

    @ParameterizedTest(name = "FROM Clause #{index}")
    @MethodSource("fromClauseCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testFromClause(case: Case) = case.assert()

    @ParameterizedTest(name = "JOIN Clause #{index}")
    @MethodSource("joinClauseCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testJoinClause(case: Case) = case.assert()

    @ParameterizedTest(name = "GROUP BY Clause #{index}")
    @MethodSource("groupByClauseCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testGroupByClause(case: Case) = case.assert()

    @ParameterizedTest(name = "UNION Clause #{index}")
    @MethodSource("unionClauseCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testUnionClause(case: Case) = case.assert()

    @ParameterizedTest(name = "ORDER BY Clause #{index}")
    @MethodSource("orderByClauseCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testOrderByClause(case: Case) = case.assert()

    @ParameterizedTest(name = "other clauses #{index}")
    @MethodSource("otherClausesCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testOtherClauses(case: Case) = case.assert()

    @ParameterizedTest(name = "subqueries #{index}")
    @MethodSource("subqueryCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testSubqueries(case: Case) = case.assert()

    @ParameterizedTest(name = "intervalSingle #{index}")
    @MethodSource("intervalSingleCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testIntervalSingle(case: Case) = case.assert()

    @ParameterizedTest(name = "intervalRange #{index}")
    @MethodSource("intervalRangeCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testIntervalRange(case: Case) = case.assert()

    @ParameterizedTest(name = "intervalCast #{index}")
    @MethodSource("intervalCastCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testIntervalCast(case: Case) = case.assert()

    @ParameterizedTest(name = "intervalArith #{index}")
    @MethodSource("intervalArithCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testIntervalArithCast(case: Case) = case.assert()

    @ParameterizedTest(name = "windowFunction #{index}")
    @MethodSource("windowFunctionCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testWindowFunction(case: Case) = case.assert()

    @ParameterizedTest(name = "windowClause #{index}")
    @MethodSource("windowClauseCases")
    @Execution(ExecutionMode.CONCURRENT)
    fun testWindowClause(case: Case) = case.assert()

    companion object {

        private val NULL = exprLit(nul())

        @JvmStatic
        fun types() = listOf(
            // SQL
            expect("BOOL", DataType.BOOL()),
            expect("TINYINT", DataType.TINYINT()),
            expect("SMALLINT", DataType.SMALLINT()),
            expect("INT", DataType.INT()),
            expect("REAL", DataType.REAL()),
            expect("FLOAT", DataType.FLOAT()),
            expect("DOUBLE PRECISION", DataType.DOUBLE_PRECISION()),
            expect("DECIMAL", DataType.DECIMAL()),
            expect("DECIMAL(2)", DataType.DECIMAL(2)),
            expect("DECIMAL(2,1)", DataType.DECIMAL(2, 1)),
            expect("NUMERIC", DataType.NUMERIC()),
            expect("NUMERIC(2)", DataType.NUMERIC(2)),
            expect("NUMERIC(2,1)", DataType.NUMERIC(2, 1)),
            expect("TIMESTAMP", DataType.TIMESTAMP()),
            expect("CHAR", DataType.CHAR()),
            expect("CHAR(1)", DataType.CHAR(1)),
            expect("VARCHAR", DataType.VARCHAR()),
            expect("VARCHAR(1)", DataType.VARCHAR(1)),
            expect("BLOB", DataType.BLOB()),
            expect("CLOB", DataType.CLOB()),
            expect("DATE", DataType.DATE()),
            expect("TIME", DataType.TIME()),
            expect("TIME (1)", DataType.TIME(1)),
            expect("TIME WITH TIME ZONE", DataType.TIME_WITH_TIME_ZONE()),
            expect("TIME (1) WITH TIME ZONE", DataType.TIME_WITH_TIME_ZONE(1)),
            // TODO TIMESTAMP
            // TODO INTERVAL
            // TODO other types in `DataType`
            // PartiQL
            expect("STRING", DataType.STRING()),
            expect("SYMBOL", DataType.SYMBOL()),
            expect("STRUCT", DataType.STRUCT()),
            expect("TUPLE", DataType.TUPLE()),
            expect("LIST", DataType.LIST()),
            expect("SEXP", DataType.SEXP()),
            expect("BAG", DataType.BAG()),
            // Other (??)
            expect("INT4", DataType.INT4()),
            expect("INT8", DataType.INT8()),
            //
            expect(
                "FOO.BAR",
                DataType.USER_DEFINED(
                    Identifier.of(
                        regular("FOO"),
                        regular("BAR")
                    )
                )
            ),
        )

        @JvmStatic
        fun exprOperators() = listOf(
            expect(
                "NOT (NULL)",
                exprNot(value = NULL)
            ),
            expect(
                "+NULL",
                exprOperator(symbol = "+", lhs = null, rhs = NULL)
            ),
            expect(
                "-NULL",
                exprOperator(symbol = "-", lhs = null, rhs = NULL)
            ),
            expect(
                "NOT (NOT (NULL))",
                exprNot(value = exprNot(value = NULL))
            ),
            expect(
                "+(+NULL)",
                exprOperator(symbol = "+", lhs = null, rhs = exprOperator(symbol = "+", lhs = null, rhs = NULL))
            ),
            expect(
                "-(-NULL)",
                exprOperator(
                    symbol = "-",
                    lhs = null,
                    rhs = exprOperator(symbol = "-", lhs = null, rhs = NULL)
                )
            ),
            expect(
                "+(-(+NULL))",
                exprOperator(
                    symbol = "+",
                    lhs = null,
                    rhs = exprOperator(
                        symbol = "-",
                        lhs = null,
                        rhs = exprOperator(
                            symbol = "+",
                            lhs = null,
                            rhs = NULL
                        )
                    )
                )
            ),
            expect(
                "NULL + NULL",
                exprOperator(
                    symbol = "+",
                    lhs = NULL,
                    rhs = NULL
                )
            ),
        )

        @JvmStatic
        fun identifiers() = listOf(
            expect(
                "x", regular("x")
            ),
            expect(
                "X", regular("X")
            ),
            expect(
                "\"x\"", delimited("x")
            ),
            expect(
                "x.y.z",
                Identifier.of(
                    regular("x"),
                    regular("y"),
                    regular("z")
                )
            ),
            expect(
                "x.\"y\".z",
                Identifier.of(
                    regular("x"),
                    delimited("y"),
                    regular("z")
                )
            ),
            expect(
                "\"x\".\"y\".\"z\"",
                Identifier.of(
                    delimited("x"),
                    delimited("y"),
                    delimited("z")
                )
            ),
        )

        // Expressions

        @JvmStatic
        fun exprLitCases() = listOf(
            expect(
                "NULL", exprLit(nul())
            ),
            expect(
                "MISSING", exprLit(missing())
            ),
            expect(
                "true", exprLit(bool(true))
            ),
            expect(
                "1", exprLit(intNum(1))
            ),
            expect(
                "2", exprLit(intNum(2))
            ),
            expect(
                "3", exprLit(intNum(3))
            ),
            expect(
                "4", exprLit(intNum(4))
            ),
            expect(
                "5.", exprLit(exactNum("5."))
            ),
            expect("1.1e0", exprLit(approxNum("1.1e0"))),
            expect("1.2E0", exprLit(approxNum("1.2E0"))),
            expect("1.2345E-5", exprLit(approxNum("1.2345E-5"))),
            expect(
                "1.3", exprLit(exactNum(BigDecimal.valueOf(1.3)))
            ),
            expect(
                "'hello'", exprLit(string("hello"))
            ),
            expect(
                "'''hello'''", exprLit(string("'hello'"))
            ),
            expect(
                "'O''Riely'", exprLit(string("O'Riely"))
            ),
            expect(
                """' one''two''three''"four" '""", exprLit(string(""" one'two'three'"four" """))
            ),
            expect(
                "'one\\two'", exprLit(string("one\\two"))
            ),
            expect(
                """hello""", regular("hello")
            ),
            expect(
                "DATE '0001-02-03'", exprLit(typedString(DataType.DATE(), "0001-02-03"))
            ),
            expect(
                "TIME '01:02:03.456-00:30'", exprLit(typedString(DataType.TIME(), "01:02:03.456-00:30"))
            ),
            expect(
                "TIMESTAMP '0001-02-03 04:05:06.78-00:30'", exprLit(typedString(DataType.TIMESTAMP(), "0001-02-03 04:05:06.78-00:30"))
            ),

            // expect("""{{ '''Hello'''    '''World''' }}""") {
            //     exprLit(clobValue("HelloWorld".toByteArray()))
            // },
            // expect("""{{ VG8gaW5maW5pdHkuLi4gYW5kIGJleW9uZCE= }}""") {
            //     exprLit(blobValue("To infinity... and beyond!".toByteArray()))
            // },
        )

        @JvmStatic
        fun exprIonCases() = listOf(
            expect(
                "`null`", exprIon(ionNull())
            ),
            expect(
                "`true`", exprIon(ionBool(true))
            ),
            expect(
                "`1`", exprIon(ionInt(1))
            ),
            expect(
                "`1.2e0`", exprIon(ionFloat(1.2))
            ),
            expect(
                "`1.3`", exprIon(ionDecimal(Decimal.valueOf(1.3)))
            ),
            expect(
                """`"hello"`""", exprIon(ionString("hello"))
            ),
            expect(
                """`hello`""", exprIon(ionSymbol("hello"))
            ),
            expect(
                "`a::b::null`", exprIon(ionNull().withAnnotations("a", "b"))
            ),
            expect(
                "`a::b::true`", exprIon(ionBool(true).withAnnotations("a", "b"))
            ),
            expect(
                "`a::b::1`", exprIon(ionInt(1).withAnnotations("a", "b"))
            ),
            expect(
                "`a::b::1.2e0`", exprIon(ionFloat(1.2).withAnnotations("a", "b"))
            ),
            expect(
                "`a::b::1.3`", exprIon(ionDecimal(Decimal.valueOf(1.3)).withAnnotations("a", "b"))
            ),
            expect(
                """`a::b::"hello"`""", exprIon(ionString("hello").withAnnotations("a", "b"))
            ),
            expect(
                """`a::b::hello`""", exprIon(ionSymbol("hello").withAnnotations("a", "b"))
            ),
        )

        private fun exprIon(value: IonElement): Expr = exprVariant(value.toString(), "ion")

        @JvmStatic
        fun exprVarCases() = listOf(
            // DEFAULT
            expect(
                "x",
                exprVarRef(
                    Identifier.regular("x"),
                    isQualified = false
                )
            ),
            expect(
                "\"x\"",
                exprVarRef(
                    Identifier.delimited("x"),
                    isQualified = false
                )
            ),
            expect(
                "x.y.z",
                exprVarRef(
                    Identifier.of(
                        regular("x"),
                        regular("y"),
                        regular("z")
                    ),
                    isQualified = false
                )
            ),
            expect(
                "x.\"y\".z",
                exprVarRef(
                    Identifier.of(
                        regular("x"),
                        delimited("y"),
                        regular("z")
                    ),
                    isQualified = false
                )
            ),
            expect(
                "\"x\".\"y\".\"z\"",
                exprVarRef(
                    Identifier.of(
                        delimited("x"),
                        delimited("y"),
                        delimited("z")
                    ),
                    isQualified = false
                )
            ),
            // LOCAL
            expect(
                "@x",
                exprVarRef(
                    Identifier.regular("x"),
                    isQualified = true
                )
            ),
            expect(
                "@\"x\"",
                exprVarRef(
                    Identifier.delimited("x"),
                    isQualified = true
                )
            ),
            expect(
                "@x.y.z",
                exprVarRef(
                    Identifier.of(
                        regular("x"),
                        regular("y"),
                        regular("z")
                    ),
                    isQualified = true
                )
            ),
            expect(
                "@x.\"y\".z",
                exprVarRef(
                    Identifier.of(
                        regular("x"),
                        delimited("y"),
                        regular("z")
                    ),
                    isQualified = true
                )
            ),
            expect(
                "@\"x\".\"y\".\"z\"",
                exprVarRef(
                    Identifier.of(
                        delimited("x"),
                        delimited("y"),
                        delimited("z")
                    ),
                    isQualified = true
                )
            ),
        )

        @JvmStatic
        fun exprPathCases() = listOf(
            expect(
                "x.y.*",
                exprPath(
                    root = exprVarRef(
                        identifier = Identifier.of(regular("x")),
                        isQualified = false
                    ),
                    steps = listOf(
                        exprPathStepField(
                            value = regular("y")
                        ),
                        exprPathStepAllFields()
                    )
                )
            ),
            expect(
                "x.y[*]",
                exprPath(
                    root = exprVarRef(
                        identifier = Identifier.of(regular("x")),
                        isQualified = false
                    ),
                    steps = listOf(
                        exprPathStepField(
                            value = regular("y")
                        ),
                        exprPathStepAllElements()
                    )
                )
            ),
            expect(
                "x[1 + a]",
                exprPath(
                    root = exprVarRef(
                        identifier = Identifier.of(regular("x")),
                        isQualified = false
                    ),
                    steps = listOf(
                        exprPathStepElement(
                            element = exprOperator(
                                symbol = "+",
                                lhs = exprLit(intNum(1)),
                                rhs = exprVarRef(
                                    identifier = Identifier.of(regular("a")),
                                    isQualified = false
                                )
                            ),
                        ),

                    )
                )
            ),
            expect(
                "x['y']",
                exprPath(
                    root = exprVarRef(
                        identifier = Identifier.of(regular("x")),
                        isQualified = false
                    ),
                    steps = listOf(
                        exprPathStepElement(exprLit(string("y")))
                    )
                )
            ),
        )

        @JvmStatic
        fun exprCallCases() = listOf(
            expect(
                "foo(1)",
                exprCall(
                    function = Identifier.of(regular("foo")),
                    args = listOf(exprLit(intNum(1))),
                    setq = null
                )
            ),
            expect(
                "foo(1, 2)",
                exprCall(
                    function = Identifier.of(regular("foo")),
                    args = listOf(
                        exprLit(intNum(1)),
                        exprLit(intNum(2)),
                    ),
                    setq = null
                )
            ),
            expect(
                "foo.bar(1)",
                exprCall(
                    function = Identifier.of(
                        regular("foo"),
                        regular("bar")
                    ),
                    args = listOf(exprLit(intNum(1))),
                    setq = null
                )
            ),
            expect(
                "foo.bar(1, 2)",
                exprCall(
                    function = Identifier.of(
                        regular("foo"),
                        regular("bar")
                    ),
                    args = listOf(
                        exprLit(intNum(1)),
                        exprLit(intNum(2))
                    ),
                    setq = null
                )
            ),
        )

        @JvmStatic
        fun exprAggCases() = listOf(
            expect(
                "FOO(x)",
                exprCall(
                    function = Identifier.of(regular("FOO")),
                    args = listOf(exprVarRef(Identifier.of(regular("x")), isQualified = false)),
                    setq = null
                )
            ),
            expect(
                "FOO(ALL x)",
                exprCall(
                    function = Identifier.of(regular("FOO")),
                    args = listOf(exprVarRef(Identifier.of(regular("x")), isQualified = false)),
                    setq = SetQuantifier.ALL()
                )
            ),
            expect(
                "FOO(DISTINCT x)",
                exprCall(
                    function = Identifier.of(regular("FOO")),
                    args = listOf(exprVarRef(Identifier.of(regular("x")), isQualified = false)),
                    setq = SetQuantifier.DISTINCT(),
                )
            ),
            expect(
                "FOO(x, y)",
                exprCall(
                    function = Identifier.of(regular("FOO")),
                    args = listOf(
                        exprVarRef(Identifier.of(regular("x")), isQualified = false),
                        exprVarRef(Identifier.of(regular("y")), isQualified = false)
                    ),
                    setq = null
                )
            ),
            expect(
                "FOO(ALL x, y)",
                exprCall(
                    function = Identifier.of(regular("FOO")),
                    args = listOf(
                        exprVarRef(Identifier.of(regular("x")), isQualified = false),
                        exprVarRef(Identifier.of(regular("y")), isQualified = false)
                    ),
                    setq = SetQuantifier.ALL()
                )
            ),
            expect(
                "FOO(DISTINCT x, y)",
                exprCall(
                    function = Identifier.of(regular("FOO")),
                    args = listOf(
                        exprVarRef(Identifier.of(regular("x")), isQualified = false),
                        exprVarRef(Identifier.of(regular("y")), isQualified = false)
                    ),
                    setq = SetQuantifier.DISTINCT(),
                )
            ),
            expect(
                "COUNT(*)",
                exprCall(
                    function = Identifier.of(regular("COUNT")), // AST representation for COUNT w/ no args maps to COUNT(*)
                    args = emptyList(),
                    setq = null
                )
            )
        )

        @JvmStatic
        fun exprCollectionCases() = listOf(
            expect(
                "<<>>",
                exprBag(values = emptyList())
            ),
            expect(
                "<<1, 2, 3>>",
                exprBag(
                    values = listOf(
                        exprLit(intNum(1)),
                        exprLit(intNum(2)),
                        exprLit(intNum(3))
                    )
                )
            ),
            expect(
                "[]",
                exprArray(values = emptyList())
            ),
            expect(
                "[1, 2, 3]",
                exprArray(
                    values = listOf(
                        exprLit(intNum(1)),
                        exprLit(intNum(2)),
                        exprLit(intNum(3))
                    )
                )
            ),
            expect(
                "VALUES ()",
                exprValues(
                    rows = listOf(
                        exprRowValue(
                            values = emptyList()
                        )
                    )
                )
            ),
            expect(
                "VALUES (1, 2, 3)",
                exprValues(
                    rows = listOf(
                        exprRowValue(
                            values = listOf(
                                exprLit(intNum(1)),
                                exprLit(intNum(2)),
                                exprLit(intNum(3))
                            )
                        )
                    )
                )
            ),
            expect(
                "()",
                exprRowValue(
                    values = emptyList()
                )
            ),
            expect(
                "(1, 2, 3)",
                exprRowValue(
                    values = listOf(
                        exprLit(intNum(1)),
                        exprLit(intNum(2)),
                        exprLit(intNum(3))
                    )
                )
            ),
            // SEXP not in v1
//            expect(
//                "SEXP ()",
//                exprCollection {
//                    type = Expr.Collection.Type.SEXP
//                }
//            ),
//            expect(
//                "SEXP (1, 2, 3)",
//                    exprCollection {
//                    type = Expr.Collection.Type.SEXP
//                    values += exprLit(integer(1))
//                    values += exprLit(integer(2))
//                    values += exprLit(integer(3))
//                }
//            ),
        )

        @JvmStatic
        fun exprStructCases() = listOf(
            expect("{}", exprStruct(listOf())),
            expect(
                "{a: 1}",
                exprStruct(
                    fields = listOf(
                        exprStructField(
                            name = v("a"),
                            value = exprLit(intNum(1))
                        )
                    )
                )
            ),
            expect(
                "{a: 1, b: false}",
                exprStruct(
                    fields = listOf(
                        exprStructField(
                            name = v("a"),
                            value = exprLit(intNum(1))
                        ),
                        exprStructField(
                            name = v("b"),
                            value = exprLit(bool(false))
                        )
                    )
                )
            ),
        )

        @JvmStatic
        fun exprSpecialFormCases() = listOf(
            expect(
                "x LIKE y",
                exprLike(
                    value = v("x"),
                    pattern = v("y"),
                    escape = null,
                    not = false
                )
            ),
            expect(
                "x NOT LIKE y",
                exprLike(
                    value = v("x"),
                    pattern = v("y"),
                    escape = null,
                    not = true
                )
            ),
            expect(
                "x LIKE y ESCAPE z",
                exprLike(
                    value = v("x"),
                    pattern = v("y"),
                    escape = v("z"),
                    not = false
                )
            ),
            expect(
                "x BETWEEN y AND z",
                exprBetween(
                    value = v("x"),
                    from = v("y"),
                    to = v("z"),
                    not = false
                )
            ),
            expect(
                "x NOT BETWEEN y AND z",
                exprBetween(
                    value = v("x"),
                    from = v("y"),
                    to = v("z"),
                    not = true
                )
            ),
            expect(
                "x IN y",
                exprInCollection(
                    lhs = v("x"),
                    rhs = v("y"),
                    not = false
                )
            ),
            expect(
                "x NOT IN y",
                exprInCollection(
                    lhs = v("x"),
                    rhs = v("y"),
                    not = true
                )
            ),
            // IS [NOT] TRUE
            expect(
                "x IS TRUE",
                exprBoolTest(
                    value = v("x"),
                    not = false,
                    truthValue = TruthValue.TRUE()
                )
            ),
            expect(
                "x IS NOT TRUE",
                exprBoolTest(
                    value = v("x"),
                    not = true,
                    truthValue = TruthValue.TRUE()
                )
            ),
            // IS [NOT] FALSE
            expect(
                "x IS FALSE",
                exprBoolTest(
                    value = v("x"),
                    not = false,
                    truthValue = TruthValue.FALSE()
                )
            ),
            expect(
                "x IS NOT FALSE",
                exprBoolTest(
                    value = v("x"),
                    not = true,
                    truthValue = TruthValue.FALSE()
                )
            ),
            // IS [NOT] UNKNOWN
            expect(
                "x IS UNKNOWN",
                exprBoolTest(
                    value = v("x"),
                    not = false,
                    truthValue = TruthValue.UNKNOWN()
                )
            ),
            expect(
                "x IS NOT UNKNOWN",
                exprBoolTest(
                    value = v("x"),
                    not = true,
                    truthValue = TruthValue.UNKNOWN()
                )
            ),
            // IS [NOT] NULL
            expect(
                "x IS NULL",
                exprNullPredicate(
                    value = v("x"),
                    not = false,
                )
            ),
            expect(
                "x IS NOT NULL",
                exprNullPredicate(
                    value = v("x"),
                    not = true,
                )
            ),
            // IS [NOT] MISSING
            expect(
                "x IS MISSING",
                exprMissingPredicate(
                    value = v("x"),
                    not = false,
                )
            ),
            expect(
                "x IS NOT MISSING",
                exprMissingPredicate(
                    value = v("x"),
                    not = true,
                )
            ),
            expect(
                "x IS BOOL",
                exprIsType(
                    value = v("x"),
                    type = DataType.BOOL(),
                    not = false
                )
            ),
            expect(
                "x IS NOT BOOL",
                exprIsType(
                    value = v("x"),
                    type = DataType.BOOL(),
                    not = true
                )
            ),
            expect(
                "NULLIF(x, y)",
                exprNullIf(
                    v1 = v("x"),
                    v2 = v("y")
                )
            ),
            expect(
                "COALESCE(x, y, z)",
                exprCoalesce(
                    args = listOf(v("x"), v("y"), v("z"))
                )
            ),
            expect(
                "SUBSTRING(x)",
                exprSubstring(
                    value = v("x"),
                    start = null,
                    length = null
                )
            ),
            expect(
                "SUBSTRING(x FROM i)",
                exprSubstring(
                    value = v("x"),
                    start = v("i"),
                    length = null
                )
            ),
            expect(
                "SUBSTRING(x FROM i FOR n)",
                exprSubstring(
                    value = v("x"),
                    start = v("i"),
                    length = v("n")
                )
            ),
            expect(
                "SUBSTRING(x FOR n)",
                exprSubstring(
                    value = v("x"),
                    start = null,
                    length = v("n")
                )
            ),
            expect(
                "POSITION(x IN y)",
                exprPosition(
                    lhs = v("x"),
                    rhs = v("y")
                )
            ),
            expect(
                "TRIM(x)",
                exprTrim(
                    value = v("x"),
                    chars = null,
                    trimSpec = null
                )
            ),
            expect(
                "TRIM(BOTH FROM x)",
                exprTrim(
                    value = v("x"),
                    chars = null,
                    trimSpec = TrimSpec.BOTH()
                )
            ),
            expect(
                "TRIM(LEADING y FROM x)",
                exprTrim(
                    value = v("x"),
                    chars = v("y"),
                    trimSpec = TrimSpec.LEADING()
                )
            ),
            expect(
                "TRIM(y FROM x)",
                exprTrim(
                    value = v("x"),
                    chars = v("y"),
                    trimSpec = null
                )
            ),
            expect(
                "OVERLAY(x PLACING y FROM z)",
                exprOverlay(
                    value = v("x"),
                    placing = v("y"),
                    from = v("z"),
                    forLength = null
                )
            ),
            expect(
                "OVERLAY(x PLACING y FROM z FOR n)",
                exprOverlay(
                    value = v("x"),
                    placing = v("y"),
                    from = v("z"),
                    forLength = v("n")
                )
            ),
            expect(
                "EXTRACT(MINUTE FROM x)",
                exprExtract(
                    field = DatetimeField.MINUTE(),
                    source = v("x")
                )
            ),
            expect(
                "(x, y) OVERLAPS (a, b)",
                exprOverlaps(
                    lhs = exprRowValue(listOf(v("x"), v("y"))),
                    rhs = exprRowValue(listOf(v("a"), v("b")))
                )
            ),
            expect(
                "[x, y] OVERLAPS [a, b]",
                exprOverlaps(
                    lhs = exprArray(listOf(v("x"), v("y"))),
                    rhs = exprArray(listOf(v("a"), v("b")))
                )
            ),
            expect(
                "<<x, y>> OVERLAPS <<a, b>>",
                exprOverlaps(
                    lhs = exprBag(listOf(v("x"), v("y"))),
                    rhs = exprBag(listOf(v("a"), v("b")))
                )
            ),
            expect(
                "CAST(x AS INT)",
                exprCast(
                    value = v("x"),
                    asType = DataType.INT()
                )
            ),
            expect(
                "DATE_ADD(MINUTE, x, y)",
                exprCall(
                    function = Identifier.of(regular("DATE_ADD")),
                    args = listOf(
                        exprLit(string("MINUTE")),
                        v("x"),
                        v("y")
                    ),
                    setq = null
                )
            ),
            expect(
                "DATE_DIFF(MINUTE, x, y)",
                exprCall(
                    function = Identifier.of(regular("DATE_DIFF")),
                    args = listOf(
                        exprLit(string("MINUTE")),
                        v("x"),
                        v("y")
                    ),
                    setq = null
                )
            ),
            expect(
                "x OUTER UNION y",
                qSet(
                    body = queryBodySetOp(
                        type = setOp(
                            setOpType = SetOpType.UNION(),
                            setq = null
                        ),
                        isOuter = true,
                        lhs = v("x"),
                        rhs = v("y")
                    ),
                )
            ),
            expect(
                "x OUTER UNION ALL y",
                qSet(
                    body = queryBodySetOp(
                        type = setOp(
                            setOpType = SetOpType.UNION(),
                            setq = SetQuantifier.ALL()
                        ),
                        isOuter = true,
                        lhs = v("x"),
                        rhs = v("y")
                    ),
                )
            ),
            expect(
                "x OUTER UNION y",
                qSet(
                    body = queryBodySetOp(
                        type = setOp(
                            setOpType = SetOpType.UNION(),
                            setq = null
                        ),
                        isOuter = true,
                        lhs = v("x"),
                        rhs = v("y")
                    ),
                )
            ),
            expect(
                "x OUTER UNION ALL y",
                qSet(
                    body = queryBodySetOp(
                        type = setOp(
                            setOpType = SetOpType.UNION(),
                            setq = SetQuantifier.ALL()
                        ),
                        isOuter = true,
                        lhs = v("x"),
                        rhs = v("y")
                    ),
                )
            ),
            expect(
                "(x UNION y) UNION z",
                qSet(
                    body = queryBodySetOp(
                        type = setOp(
                            setOpType = SetOpType.UNION(),
                            setq = null
                        ),
                        isOuter = false,
                        lhs = qSet(
                            body = queryBodySetOp(
                                type = setOp(
                                    setOpType = SetOpType.UNION(),
                                    setq = null
                                ),
                                isOuter = false,
                                lhs = v("x"),
                                rhs = v("y")
                            ),
                        ),
                        rhs = v("z"),
                    ),
                )
            ),
            expect(
                "x UNION (y UNION z)",
                qSet(
                    body = queryBodySetOp(
                        type = setOp(
                            setOpType = SetOpType.UNION(),
                            setq = null
                        ),
                        isOuter = false,
                        lhs = v("x"),
                        rhs = qSet(
                            body = queryBodySetOp(
                                type = setOp(
                                    setOpType = SetOpType.UNION(),
                                    setq = null
                                ),
                                isOuter = false,
                                lhs = v("y"),
                                rhs = v("z"),
                            ),
                        )
                    ),
                )
            ),
            expect(
                "(x EXCEPT y) EXCEPT z",
                qSet(
                    body = queryBodySetOp(
                        type = setOp(
                            setOpType = SetOpType.EXCEPT(),
                            setq = null
                        ),
                        isOuter = false,
                        lhs = qSet(
                            body = queryBodySetOp(
                                type = setOp(
                                    setOpType = SetOpType.EXCEPT(),
                                    setq = null
                                ),
                                isOuter = false,
                                lhs = v("x"),
                                rhs = v("y")
                            ),
                        ),
                        rhs = v("z")
                    ),
                )
            ),
            expect(
                "x EXCEPT (y EXCEPT z)",
                qSet(
                    body = queryBodySetOp(
                        type = setOp(
                            setOpType = SetOpType.EXCEPT(),
                            setq = null
                        ),
                        isOuter = false,
                        lhs = v("x"),
                        rhs = qSet(
                            body = queryBodySetOp(
                                type = setOp(
                                    setOpType = SetOpType.EXCEPT(),
                                    setq = null
                                ),
                                isOuter = false,
                                lhs = v("y"),
                                rhs = v("z")
                            ),
                        )
                    ),
                )
            ),
            expect(
                "(x INTERSECT y) INTERSECT z",
                qSet(
                    body = queryBodySetOp(
                        type = setOp(
                            setOpType = SetOpType.INTERSECT(),
                            setq = null
                        ),
                        isOuter = false,
                        lhs = qSet(
                            body = queryBodySetOp(
                                type = setOp(
                                    setOpType = SetOpType.INTERSECT(),
                                    setq = null
                                ),
                                isOuter = false,
                                lhs = v("x"),
                                rhs = v("y")
                            ),
                        ),
                        rhs = v("z")
                    ),
                )
            ),
            expect(
                "x INTERSECT (y INTERSECT z)",
                qSet(
                    body = queryBodySetOp(
                        type = setOp(
                            setOpType = SetOpType.INTERSECT(),
                            setq = null
                        ),
                        isOuter = false,
                        lhs = v("x"),
                        rhs = qSet(
                            body = queryBodySetOp(
                                type = setOp(
                                    setOpType = SetOpType.INTERSECT(),
                                    setq = null
                                ),
                                isOuter = false,
                                lhs = v("y"),
                                rhs = v("z")
                            ),
                        )
                    ),
                )
            ),
        )

        @JvmStatic
        fun exprCaseCases() = listOf(
            expect(
                "CASE WHEN a THEN x WHEN b THEN y END",
                exprCase(
                    expr = null,
                    branches = listOf(
                        exprCaseBranch(v("a"), v("x")),
                        exprCaseBranch(v("b"), v("y"))
                    ),
                    defaultExpr = null
                )
            ),
            expect(
                "CASE z WHEN a THEN x WHEN b THEN y END",
                exprCase(
                    expr = v("z"),
                    branches = listOf(
                        exprCaseBranch(v("a"), v("x")),
                        exprCaseBranch(v("b"), v("y"))
                    ),
                    defaultExpr = null
                )
            ),
            expect(
                "CASE z WHEN a THEN x ELSE y END",
                exprCase(
                    expr = v("z"),
                    branches = listOf(
                        exprCaseBranch(v("a"), v("x"))
                    ),
                    defaultExpr = v("y")
                )
            ),
        )

        @JvmStatic
        fun selectClauseCases() = listOf(
            expect(
                "SELECT a FROM T",
                qSet(
                    body = sfw(
                        select = selectList(
                            items = listOf(
                                selectItemExpr(v("a"), asAlias = null)
                            ),
                            setq = null
                        ),
                        from = table("T"),
                    ),
                )
            ),
            expect(
                "SELECT a AS x FROM T",
                qSet(
                    body = sfw(
                        select = selectList(
                            items = listOf(selectItemExpr(v("a"), regular("x"))),
                            setq = null
                        ),
                        from = table("T"),
                    ),
                )
            ),
            expect(
                "SELECT a AS x, b AS y FROM T",
                qSet(
                    body = sfw(
                        select = selectList(
                            items = listOf(
                                selectItemExpr(v("a"), regular("x")),
                                selectItemExpr(v("b"), regular("y"))
                            ),
                            setq = null
                        ),
                        from = table("T")
                    ),
                )
            ),
            expect(
                "SELECT ALL a FROM T",
                qSet(
                    body = sfw(
                        select = selectList(
                            items = listOf(selectItemExpr(v("a"), asAlias = null)),
                            setq = SetQuantifier.ALL(),
                        ),
                        from = table("T")
                    )
                )
            ),
            expect(
                "SELECT DISTINCT a FROM T",
                qSet(
                    body = sfw(
                        select = selectList(
                            setq = SetQuantifier.DISTINCT(),
                            items = listOf(
                                selectItemExpr(v("a"), asAlias = null)
                            )
                        ),
                        from = table("T")
                    )
                )
            ),
            expect(
                "SELECT a.* FROM T",
                qSet(
                    body = sfw(
                        select = selectList(
                            items = listOf(selectItemStar(v("a"))),
                            setq = null
                        ),
                        from = table("T")
                    )
                )
            ),
            expect(
                "SELECT * FROM T",
                qSet(
                    body = sfw(
                        select = selectStar(setq = null),
                        from = table("T")
                    )
                )
            ),
            expect(
                "SELECT DISTINCT * FROM T",
                qSet(
                    body = sfw(
                        select = selectStar(SetQuantifier.DISTINCT()),
                        from = table("T")
                    )
                )
            ),
            expect(
                "SELECT ALL * FROM T",
                qSet(
                    body = sfw(
                        select = selectStar(SetQuantifier.ALL()),
                        from = table("T")
                    )
                )
            ),
            expect(
                "SELECT VALUE a FROM T",
                qSet(
                    body = sfw(
                        select = selectValue(
                            constructor = v("a"),
                            setq = null
                        ),
                        from = table("T")
                    )
                )
            ),
            expect(
                "SELECT ALL VALUE a FROM T",
                qSet(
                    body = sfw(
                        select = selectValue(
                            setq = SetQuantifier.ALL(),
                            constructor = v("a")
                        ),
                        from = table("T")
                    )
                )
            ),
            expect(
                "SELECT DISTINCT VALUE a FROM T",
                qSet(
                    body = sfw(
                        select = selectValue(
                            setq = SetQuantifier.DISTINCT(),
                            constructor = v("a")
                        ),
                        from = table("T")
                    )
                )
            ),
            expect(
                "PIVOT a AT b FROM T",
                qSet(
                    body = sfw(
                        select = selectPivot(v("a"), v("b")),
                        from = table("T")
                    )
                )
            ),
        )

        @JvmStatic
        fun excludeClauseCases() = listOf(
            expect(
                "SELECT a EXCLUDE t.a FROM T",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = from(
                            listOf(
                                fromExpr(
                                    expr = v("T"),
                                    fromType = FromType.SCAN(),
                                    asAlias = null,
                                    atAlias = null
                                )
                            )
                        ),
                        exclude = exclude(
                            excludePaths = listOf(
                                excludePath(
                                    varRef = v("t"),
                                    excludeSteps = listOf(insensitiveExcludeStructField("a"))
                                )
                            )
                        )
                    )
                )
            ),
            expect(
                "SELECT a EXCLUDE a.b, c.d, e.f, g.h FROM T",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = from(
                            tableRefs = listOf(
                                fromExpr(
                                    expr = v("T"),
                                    fromType = FromType.SCAN(),
                                    asAlias = null,
                                    atAlias = null
                                )
                            )
                        ),
                        exclude = exclude(
                            excludePaths = listOf(
                                excludePath(
                                    varRef = v("a"),
                                    excludeSteps = listOf(insensitiveExcludeStructField("b"))
                                ),
                                excludePath(
                                    varRef = v("c"),
                                    excludeSteps = listOf(insensitiveExcludeStructField("d"))
                                ),
                                excludePath(
                                    varRef = v("e"),
                                    excludeSteps = listOf(insensitiveExcludeStructField("f"))
                                ),
                                excludePath(
                                    varRef = v("g"),
                                    excludeSteps = listOf(insensitiveExcludeStructField("h"))
                                )
                            )
                        )
                    )
                )
            ),
            expect(
                "SELECT a EXCLUDE t.a.\"b\".*[*].c, \"s\"[0].d.\"e\"[*].f.* FROM T",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = from(
                            tableRefs = listOf(
                                fromExpr(
                                    expr = v("T"),
                                    fromType = FromType.SCAN(),
                                    asAlias = null,
                                    atAlias = null
                                )
                            )
                        ),
                        exclude = exclude(
                            excludePaths = listOf(
                                excludePath(
                                    varRef = v("t"),
                                    excludeSteps = listOf(
                                        insensitiveExcludeStructField("a"),
                                        sensitiveExcludeStructField("b"),
                                        excludeStepStructWildcard(),
                                        excludeStepCollWildcard(),
                                        insensitiveExcludeStructField("c"),
                                    )
                                ),
                                excludePath(
                                    varRef = exprVarRef(Identifier.of(delimited("s")), isQualified = false),
                                    excludeSteps = listOf(
                                        excludeStepCollIndex(0),
                                        insensitiveExcludeStructField("d"),
                                        sensitiveExcludeStructField("e"),
                                        excludeStepCollWildcard(),
                                        insensitiveExcludeStructField("f"),
                                        excludeStepStructWildcard(),
                                    )
                                )
                            )
                        )
                    )
                )
            ),
        )

        private fun insensitiveExcludeStructField(str: String) = excludeStepStructField(
            symbol = regular(str)
        )

        private fun sensitiveExcludeStructField(str: String) = excludeStepStructField(
            symbol = delimited(str)
        )

        @JvmStatic
        fun fromClauseCases() = listOf(
            expect(
                "SELECT a FROM T",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = from(
                            tableRefs = listOf(
                                fromExpr(
                                    expr = v("T"),
                                    fromType = FromType.SCAN(),
                                    asAlias = null,
                                    atAlias = null
                                )
                            )
                        )
                    )
                )
            ),
            expect(
                "SELECT a FROM T AS x",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = from(
                            tableRefs = listOf(
                                fromExpr(
                                    expr = v("T"),
                                    fromType = FromType.SCAN(),
                                    asAlias = regular("x"),
                                    atAlias = null
                                )
                            )
                        )
                    )
                )
            ),
            expect(
                "SELECT a FROM T AS x AT y",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = from(
                            tableRefs = listOf(
                                fromExpr(
                                    expr = v("T"),
                                    fromType = FromType.SCAN(),
                                    asAlias = regular("x"),
                                    atAlias = regular("y")
                                )
                            )
                        )
                    )
                )
            ),
            expect(
                "SELECT a FROM UNPIVOT T",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = from(
                            tableRefs = listOf(
                                fromExpr(
                                    expr = v("T"),
                                    fromType = FromType.UNPIVOT(),
                                    asAlias = null,
                                    atAlias = null
                                )
                            )
                        )
                    )
                )
            ),
            expect(
                "SELECT a FROM UNPIVOT T AS x",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = from(
                            tableRefs = listOf(
                                fromExpr(
                                    expr = v("T"),
                                    fromType = FromType.UNPIVOT(),
                                    asAlias = regular("x"),
                                    atAlias = null
                                )
                            )
                        )
                    )
                )
            ),
            expect(
                "SELECT a FROM UNPIVOT T AS x AT y",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = from(
                            tableRefs = listOf(
                                fromExpr(
                                    expr = v("T"),
                                    fromType = FromType.UNPIVOT(),
                                    asAlias = regular("x"),
                                    atAlias = regular("y")
                                )
                            )
                        )
                    )
                )
            ),
        )

        @JvmStatic
        fun joinClauseCases() = listOf(
            expect(
                "SELECT a FROM T JOIN S",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = from(
                            tableRefs = listOf(
                                fromJoin(
                                    lhs = scan("T"),
                                    rhs = scan("S"),
                                    joinType = null,
                                    condition = null
                                )
                            )
                        )
                    )
                )
            ),
            expect(
                "SELECT a FROM T INNER JOIN S",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = from(
                            tableRefs = listOf(
                                fromJoin(
                                    joinType = JoinType.INNER(),
                                    lhs = scan("T"),
                                    rhs = scan("S"),
                                    condition = null
                                )
                            )
                        )
                    )
                )
            ),
            expect(
                "SELECT a FROM T, S",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = from(
                            tableRefs = listOf(
                                fromExpr(
                                    expr = v("T"),
                                    fromType = FromType.SCAN(),
                                    asAlias = null,
                                    atAlias = null
                                ),
                                fromExpr(
                                    expr = v("S"),
                                    fromType = FromType.SCAN(),
                                    asAlias = null,
                                    atAlias = null
                                )
                            )
                        )
                    )
                )
            ),
            expect(
                "SELECT a FROM T CROSS JOIN S",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = from(
                            tableRefs = listOf(
                                fromJoin(
                                    joinType = JoinType.CROSS(),
                                    lhs = scan("T"),
                                    rhs = scan("S"),
                                    condition = null
                                )
                            )
                        )
                    )

                )
            ),
            expect(
                "SELECT a FROM T JOIN S ON NULL",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = from(
                            tableRefs = listOf(
                                fromJoin(
                                    lhs = scan("T"),
                                    rhs = scan("S"),
                                    joinType = null,
                                    condition = NULL
                                )
                            )
                        )
                    )
                )
            ),
            expect(
                "SELECT a FROM T INNER JOIN S ON NULL",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = from(
                            tableRefs = listOf(
                                fromJoin(
                                    joinType = JoinType.INNER(),
                                    lhs = scan("T"),
                                    rhs = scan("S"),
                                    condition = NULL
                                )
                            )
                        )
                    )
                )
            ),
        )

        // These are simple clauses
        @JvmStatic
        private fun otherClausesCases() = listOf(
            expect(
                "SELECT a FROM T LET x AS i",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T"),
                        let = Ast.let(
                            bindings = listOf(letBinding(v("x"), regular("i")))
                        )
                    )
                )
            ),
            expect(
                "SELECT a FROM T LET x AS i, y AS j",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T"),
                        let = Ast.let(
                            bindings = listOf(
                                letBinding(v("x"), regular("i")),
                                letBinding(v("y"), regular("j"))
                            )
                        )
                    )
                )
            ),
            expect(
                "SELECT a FROM T WHERE x",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T"),
                        where = v("x")
                    )
                )
            ),
            expect(
                "SELECT a FROM T LIMIT 1",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T")
                    ),
                    limit = exprLit(intNum(1))
                )
            ),
            expect(
                "SELECT a FROM T OFFSET 2",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T")
                    ),
                    offset = exprLit(intNum(2))
                )
            ),
            expect(
                "SELECT a FROM T LIMIT 1 OFFSET 2",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T")
                    ),
                    limit = exprLit(intNum(1)),
                    offset = exprLit(intNum(2))
                )
            ),
            expect(
                "SELECT a FROM T GROUP BY x HAVING y",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T"),
                        groupBy = groupBy(
                            strategy = GroupByStrategy.FULL(),
                            keys = listOf(groupByKey(v("x"), asAlias = null)),
                            asAlias = null
                        ),
                        having = v("y")
                    )
                )
            ),
            expect(
                "WITH a AS (SELECT * FROM t) SELECT * FROM a",
                qSet(
                    with = Ast.with(
                        listOf(
                            Ast.withListElement(
                                queryName = regular("a"),
                                asQuery = qSet(
                                    body = sfw(
                                        select = selectStar(),
                                        from = table("t")
                                    )
                                ),
                                columnList = null
                            )
                        ),
                        isRecursive = false
                    ),
                    body = sfw(
                        select = selectStar(),
                        from = table("a"),
                    ),
                )
            ),
            expect(
                "WITH a (b, c, d) AS (SELECT * FROM t) SELECT * FROM a",
                qSet(
                    with = Ast.with(
                        listOf(
                            Ast.withListElement(
                                queryName = regular("a"),
                                asQuery = qSet(
                                    body = sfw(
                                        select = selectStar(),
                                        from = table("t")
                                    )
                                ),
                                columnList = listOf(
                                    regular("b"),
                                    regular("c"),
                                    regular("d")
                                )
                            )
                        ),
                        isRecursive = false
                    ),
                    body = sfw(
                        select = selectStar(),
                        from = table("a"),
                    ),
                )
            ),
        )

        @JvmStatic
        private fun groupByClauseCases() = listOf(
            expect(
                "SELECT a FROM T GROUP BY x",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T"),
                        groupBy = groupBy(
                            strategy = GroupByStrategy.FULL(),
                            keys = listOf(groupByKey(v("x"), asAlias = null)),
                            asAlias = null
                        )
                    )
                )
            ),
            expect(
                "SELECT a FROM T GROUP BY x AS i",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T"),
                        groupBy = groupBy(
                            strategy = GroupByStrategy.FULL(),
                            keys = listOf(groupByKey(v("x"), regular("i"))),
                            asAlias = null
                        )
                    )
                )
            ),
            expect(
                "SELECT a FROM T GROUP BY x, y",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T"),
                        groupBy = groupBy(
                            strategy = GroupByStrategy.FULL(),
                            keys = listOf(
                                groupByKey(v("x"), asAlias = null),
                                groupByKey(v("y"), asAlias = null)
                            ),
                            asAlias = null
                        )
                    )
                )
            ),
            expect(
                "SELECT a FROM T GROUP BY x AS i, y AS j",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T"),
                        groupBy = groupBy(
                            strategy = GroupByStrategy.FULL(),
                            keys = listOf(
                                groupByKey(v("x"), regular("i")),
                                groupByKey(v("y"), regular("j"))
                            ),
                            asAlias = null
                        )
                    )
                )
            ),
            expect(
                "SELECT a FROM T GROUP BY x GROUP AS g",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T"),
                        groupBy = groupBy(
                            strategy = GroupByStrategy.FULL(),
                            keys = listOf(groupByKey(v("x"), asAlias = null)),
                            asAlias = regular("g")
                        )
                    )
                )
            ),
            expect(
                "SELECT a FROM T GROUP BY x AS i GROUP AS g",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T"),
                        groupBy = groupBy(
                            strategy = GroupByStrategy.FULL(),
                            keys = listOf(groupByKey(v("x"), regular("i"))),
                            asAlias = regular("g")
                        )
                    )
                )
            ),
            expect(
                "SELECT a FROM T GROUP BY x, y GROUP AS g",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T"),
                        groupBy = groupBy(
                            strategy = GroupByStrategy.FULL(),
                            keys = listOf(
                                groupByKey(v("x"), asAlias = null),
                                groupByKey(v("y"), asAlias = null)
                            ),
                            asAlias = regular("g")
                        )
                    )
                )
            ),
            expect(
                "SELECT a FROM T GROUP BY x AS i, y AS j GROUP AS g",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T"),
                        groupBy = groupBy(
                            strategy = GroupByStrategy.FULL(),
                            keys = listOf(
                                groupByKey(v("x"), regular("i")),
                                groupByKey(v("y"), regular("j"))
                            ),
                            asAlias = regular("g")
                        )
                    )
                )
            ),
            expect(
                "SELECT a FROM T GROUP PARTIAL BY x",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T"),
                        groupBy = groupBy(
                            strategy = GroupByStrategy.PARTIAL(),
                            keys = listOf(groupByKey(v("x"), asAlias = null)),
                            asAlias = null
                        )
                    )
                )
            ),
        )

        @JvmStatic
        private fun orderByClauseCases() = listOf(
            expect(
                "SELECT a FROM T ORDER BY x",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T")
                    ),
                    orderBy = orderBy(
                        sorts = listOf(sort(v("x"), order = null, nulls = null))
                    )
                )
            ),
            expect(
                "SELECT a FROM T ORDER BY x ASC",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T")
                    ),
                    orderBy = orderBy(
                        sorts = listOf(sort(v("x"), Order.ASC(), nulls = null))
                    )
                )
            ),
            expect(
                "SELECT a FROM T ORDER BY x DESC",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T")
                    ),
                    orderBy = orderBy(
                        sorts = listOf(sort(v("x"), Order.DESC(), nulls = null))
                    )
                )
            ),
            expect(
                "SELECT a FROM T ORDER BY x NULLS FIRST",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T")
                    ),
                    orderBy = orderBy(
                        sorts = listOf(sort(v("x"), order = null, nulls = Nulls.FIRST()))
                    )
                )
            ),
            expect(
                "SELECT a FROM T ORDER BY x NULLS LAST",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T")
                    ),
                    orderBy = orderBy(
                        sorts = listOf(sort(v("x"), order = null, nulls = Nulls.LAST()))
                    )
                )
            ),
            expect(
                "SELECT a FROM T ORDER BY x ASC NULLS FIRST",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T")
                    ),
                    orderBy = orderBy(
                        sorts = listOf(sort(v("x"), Order.ASC(), Nulls.FIRST()))
                    )
                )
            ),
            expect(
                "SELECT a FROM T ORDER BY x ASC NULLS LAST",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T")
                    ),
                    orderBy = orderBy(
                        sorts = listOf(sort(v("x"), Order.ASC(), Nulls.LAST()))
                    )
                )
            ),
            expect(
                "SELECT a FROM T ORDER BY x DESC NULLS FIRST",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T")
                    ),
                    orderBy = orderBy(
                        sorts = listOf(sort(v("x"), Order.DESC(), Nulls.FIRST()))
                    )
                )
            ),
            expect(
                "SELECT a FROM T ORDER BY x DESC NULLS LAST",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T")
                    ),
                    orderBy = orderBy(
                        sorts = listOf(sort(v("x"), Order.DESC(), Nulls.LAST()))
                    )
                )
            ),
            expect(
                "SELECT a FROM T ORDER BY x, y",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T")
                    ),
                    orderBy = orderBy(
                        sorts = listOf(
                            sort(v("x"), order = null, nulls = null),
                            sort(v("y"), order = null, nulls = null)
                        )
                    )
                )
            ),
            expect(
                "SELECT a FROM T ORDER BY x ASC, y DESC",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T")
                    ),
                    orderBy = orderBy(
                        sorts = listOf(
                            sort(v("x"), Order.ASC(), nulls = null),
                            sort(v("y"), Order.DESC(), nulls = null)
                        )
                    )
                )
            ),
            expect(
                "SELECT a FROM T ORDER BY x NULLS FIRST, y NULLS LAST",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T")
                    ),
                    orderBy = orderBy(
                        sorts = listOf(
                            sort(v("x"), order = null, Nulls.FIRST()),
                            sort(v("y"), order = null, Nulls.LAST())
                        )
                    )
                )
            ),
            expect(
                "SELECT a FROM T ORDER BY x ASC NULLS FIRST, y DESC NULLS LAST",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T")
                    ),
                    orderBy = orderBy(
                        sorts = listOf(
                            sort(v("x"), Order.ASC(), Nulls.FIRST()),
                            sort(v("y"), Order.DESC(), Nulls.LAST())
                        )
                    )
                )
            ),
        )

        @JvmStatic
        fun unionClauseCases() = listOf(
            expect(
                "(SELECT a FROM T) UNION (SELECT b FROM S)",
                qSet(
                    body = queryBodySetOp(
                        type = setOp(
                            setOpType = SetOpType.UNION(),
                            setq = null
                        ),
                        isOuter = false,
                        lhs = qSet(
                            body = sfw(
                                select = select("a"),
                                from = table("T")
                            )
                        ),
                        rhs = qSet(
                            body = sfw(
                                select = select("b"),
                                from = table("S")
                            )
                        )
                    )
                )
            ),
            expect(
                "(SELECT a FROM T) UNION ALL (SELECT b FROM S)",
                qSet(
                    body = queryBodySetOp(
                        type = setOp(SetOpType.UNION(), SetQuantifier.ALL()),
                        isOuter = false,
                        lhs = qSet(
                            body = sfw(
                                select = select("a"),
                                from = table("T")
                            )
                        ),
                        rhs = qSet(
                            body = sfw(
                                select = select("b"),
                                from = table("S")
                            )
                        )
                    )
                )
            ),
            expect(
                "(SELECT a FROM T) UNION DISTINCT (SELECT b FROM S)",
                qSet(
                    body = queryBodySetOp(
                        type = setOp(SetOpType.UNION(), SetQuantifier.DISTINCT()),
                        isOuter = false,
                        lhs = qSet(
                            body = sfw(
                                select = select("a"),
                                from = table("T")
                            )
                        ),
                        rhs = qSet(
                            body = sfw(
                                select = select("b"),
                                from = table("S")
                            )
                        )
                    )
                )
            ),
            expect(
                "(SELECT a FROM T) UNION (SELECT b FROM S) LIMIT 1",
                qSet(
                    body = queryBodySetOp(
                        type = setOp(SetOpType.UNION(), setq = null),
                        isOuter = false,
                        lhs = qSet(
                            body = sfw(
                                select = select("a"),
                                from = table("T")
                            )
                        ),
                        rhs = qSet(
                            body = sfw(
                                select = select("b"),
                                from = table("S")
                            )
                        )
                    ),
                    limit = exprLit(intNum(1)) // LIMIT associated with SQL set op
                )
            ),
            expect(
                "(SELECT a FROM T) UNION (SELECT b FROM S LIMIT 1)",
                qSet(
                    body = queryBodySetOp(
                        type = setOp(SetOpType.UNION(), null),
                        isOuter = false,
                        lhs = qSet(
                            body = sfw(
                                select = select("a"),
                                from = table("T")
                            )
                        ),
                        rhs = qSet(
                            body = sfw(
                                select = select("b"),
                                from = table("S"),
                            ),
                            limit = exprLit(intNum(1)) // LIMIT associated with rhs SFW query
                        )
                    )
                )
            ),
            expect(
                "(SELECT a FROM T) UNION (SELECT b FROM S) ORDER BY x",
                qSet(
                    body = queryBodySetOp(
                        type = setOp(SetOpType.UNION(), null),
                        isOuter = false,
                        lhs = qSet(
                            body = sfw(
                                select = select("a"),
                                from = table("T")
                            )
                        ),
                        rhs = qSet(
                            body = sfw(
                                select = select("b"),
                                from = table("S")
                            )
                        )
                    ),
                    orderBy = orderBy(
                        sorts = listOf(sort(v("x"), order = null, nulls = null)) // ORDER BY associated with SQL set op
                    )
                )
            ),
            expect(
                "(SELECT a FROM T) UNION (SELECT b FROM S ORDER BY x)",
                qSet(
                    body = queryBodySetOp(
                        type = setOp(SetOpType.UNION(), null),
                        isOuter = false,
                        lhs = qSet(
                            body = sfw(
                                select = select("a"),
                                from = table("T")
                            )
                        ),
                        rhs = qSet(
                            body = sfw(
                                select = select("b"),
                                from = table("S"),
                            ),
                            orderBy = orderBy(
                                sorts = listOf(sort(v("x"), order = null, nulls = null)) // ORDER BY associated with SFW
                            )
                        ),
                    )
                )
            ),
            expect(
                "(SELECT a FROM T) UNION ((SELECT b FROM S) UNION (SELECT c FROM R))",
                qSet(
                    body = queryBodySetOp(
                        type = setOp(SetOpType.UNION(), null),
                        isOuter = false,
                        lhs = qSet(
                            body = sfw(
                                select = select("a"),
                                from = table("T")
                            )
                        ),
                        rhs = qSet(
                            body = queryBodySetOp(
                                type = setOp(SetOpType.UNION(), null),
                                isOuter = false,
                                lhs = qSet(
                                    body = sfw(
                                        select = select("b"),
                                        from = table("S")
                                    )
                                ),
                                rhs = qSet(
                                    body = sfw(
                                        select = select("c"),
                                        from = table("R")
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            expect(
                "((SELECT a FROM T) UNION (SELECT b FROM S)) UNION (SELECT c FROM R)",
                qSet(
                    body = queryBodySetOp(
                        type = setOp(SetOpType.UNION(), null),
                        isOuter = false,
                        lhs = qSet(
                            body = queryBodySetOp(
                                type = setOp(SetOpType.UNION(), null),
                                isOuter = false,
                                lhs = qSet(
                                    body = sfw(
                                        select = select("a"),
                                        from = table("T")
                                    )
                                ),
                                rhs = qSet(
                                    body = sfw(
                                        select = select("b"),
                                        from = table("S")
                                    )
                                )
                            )
                        ),
                        rhs = qSet(
                            body = sfw(
                                select = select("c"),
                                from = table("R")
                            )
                        )
                    )
                )
            ),
        )

        // These are simple clauses
        @JvmStatic
        private fun subqueryCases() = listOf(
            expect(
                "1 = (SELECT a FROM T)",
                exprOperator(
                    symbol = "=",
                    lhs = exprLit(intNum(1)),
                    rhs = qSet(
                        body = sfw(
                            select = select("a"),
                            from = table("T")
                        )
                    )
                )
            ),
            expect(
                "(1, 2) = (SELECT a FROM T)",
                exprOperator(
                    symbol = "=",
                    lhs = exprRowValue(
                        values = listOf(
                            exprLit(intNum(1)),
                            exprLit(intNum(2))
                        )
                    ),
                    rhs = qSet(
                        body = sfw(
                            select = select("a"),
                            from = table("T")
                        )
                    )
                )
            ),
        )

        // INTERVAL <interval string> <single datetime field>
        @JvmStatic
        private fun intervalSingleCases() = listOf(
            expect(
                "INTERVAL '10' YEAR",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.YEAR(),
                        null,
                        null
                    ),
                    value = "10"
                )
            ),
            expect(
                "INTERVAL '-10' YEAR",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.YEAR(),
                        null,
                        null
                    ),
                    value = "-10"
                )
            ),
            expect(
                "INTERVAL '10' YEAR (2)",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.YEAR(),
                        2,
                        null
                    ),
                    value = "10"
                )
            ),
            expect(
                "INTERVAL '-10' YEAR (2)",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.YEAR(),
                        2,
                        null
                    ),
                    value = "-10"
                )
            ),
            expect(
                "INTERVAL '10' MONTH",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.MONTH(),
                        null,
                        null
                    ),
                    value = "10"
                )
            ),
            expect(
                "INTERVAL '-10' MONTH",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.MONTH(),
                        null,
                        null
                    ),
                    value = "-10"
                )
            ),
            expect(
                "INTERVAL '10' MONTH (2)",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.MONTH(),
                        2,
                        null
                    ),
                    value = "10"
                )
            ),
            expect(
                "INTERVAL '-10' MONTH (2)",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.MONTH(),
                        2,
                        null
                    ),
                    value = "-10"
                )
            ),
            expect(
                "INTERVAL '10' DAY",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.DAY(),
                        null,
                        null
                    ),
                    value = "10"
                )
            ),
            expect(
                "INTERVAL '-10' DAY",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.DAY(),
                        null,
                        null
                    ),
                    value = "-10"
                )
            ),
            expect(
                "INTERVAL '10' DAY (2)",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.DAY(),
                        2,
                        null
                    ),
                    value = "10"
                )
            ),
            expect(
                "INTERVAL '-10' DAY (2)",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.DAY(),
                        2,
                        null
                    ),
                    value = "-10"
                )
            ),
            expect(
                "INTERVAL '10' HOUR",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.HOUR(),
                        null,
                        null
                    ),
                    value = "10"
                )
            ),
            expect(
                "INTERVAL '-10' HOUR",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.HOUR(),
                        null,
                        null
                    ),
                    value = "-10"
                )
            ),
            expect(
                "INTERVAL '10' HOUR (2)",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.HOUR(),
                        2,
                        null
                    ),
                    value = "10"
                )
            ),
            expect(
                "INTERVAL '-10' HOUR (2)",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.HOUR(),
                        2,
                        null
                    ),
                    value = "-10"
                )
            ),
            expect(
                "INTERVAL '10' MINUTE",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.MINUTE(),
                        null,
                        null
                    ),
                    value = "10"
                )
            ),
            expect(
                "INTERVAL '-10' MINUTE",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.MINUTE(),
                        null,
                        null
                    ),
                    value = "-10"
                )
            ),
            expect(
                "INTERVAL '10' MINUTE (2)",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.MINUTE(),
                        2,
                        null
                    ),
                    value = "10"
                )
            ),
            expect(
                "INTERVAL '-10' MINUTE (2)",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.MINUTE(),
                        2,
                        null
                    ),
                    value = "-10"
                )
            ),
            expect(
                "INTERVAL '10' SECOND",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.SECOND(),
                        null,
                        null
                    ),
                    value = "10"
                )
            ),
            expect(
                "INTERVAL '-10' SECOND",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.SECOND(),
                        null,
                        null
                    ),
                    value = "-10"
                )
            ),
            expect(
                "INTERVAL '10' SECOND (2)",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.SECOND(),
                        2,
                        null
                    ),
                    value = "10"
                )
            ),
            expect(
                "INTERVAL '-10' SECOND (2)",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.SECOND(),
                        2,
                        null
                    ),
                    value = "-10"
                )
            ),
            expect(
                "INTERVAL '10.234' SECOND (2, 3)",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.SECOND(),
                        2,
                        3
                    ),
                    value = "10.234"
                )
            ),
            expect(
                "INTERVAL '-10.234' SECOND (2, 3)",
                interval(
                    qualifier = IntervalQualifier.Single(
                        DatetimeField.SECOND(),
                        2,
                        3
                    ),
                    value = "-10.234"
                )
            )
        )

        // CAST to INTERVAL
        @JvmStatic
        private fun intervalCastCases() = listOf(
            expect(
                "CAST(i AS INTERVAL YEAR)",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Single(
                            DatetimeField.YEAR(),
                            null,
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL YEAR (2))",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Single(
                            DatetimeField.YEAR(),
                            2,
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL MONTH)",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Single(
                            DatetimeField.MONTH(),
                            null,
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL MONTH (2))",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Single(
                            DatetimeField.MONTH(),
                            2,
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL DAY)",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Single(
                            DatetimeField.DAY(),
                            null,
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL DAY (2))",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Single(
                            DatetimeField.DAY(),
                            2,
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL HOUR)",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Single(
                            DatetimeField.HOUR(),
                            null,
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL HOUR (2))",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Single(
                            DatetimeField.HOUR(),
                            2,
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL MINUTE)",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Single(
                            DatetimeField.MINUTE(),
                            null,
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL MINUTE (2))",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Single(
                            DatetimeField.MINUTE(),
                            2,
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL SECOND)",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Single(
                            DatetimeField.SECOND(),
                            null,
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL SECOND (2))",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Single(
                            DatetimeField.SECOND(),
                            2,
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL SECOND (2, 3))",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Single(
                            DatetimeField.SECOND(),
                            2,
                            3
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL YEAR TO MONTH)",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Range(
                            DatetimeField.YEAR(),
                            null,
                            DatetimeField.MONTH(),
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL YEAR (2) TO MONTH)",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Range(
                            DatetimeField.YEAR(),
                            2,
                            DatetimeField.MONTH(),
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL DAY TO HOUR)",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Range(
                            DatetimeField.DAY(),
                            null,
                            DatetimeField.HOUR(),
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL DAY (2) TO HOUR)",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Range(
                            DatetimeField.DAY(),
                            2,
                            DatetimeField.HOUR(),
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL DAY TO MINUTE)",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Range(
                            DatetimeField.DAY(),
                            null,
                            DatetimeField.MINUTE(),
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL DAY (2) TO MINUTE)",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Range(
                            DatetimeField.DAY(),
                            2,
                            DatetimeField.MINUTE(),
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL DAY TO SECOND)",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Range(
                            DatetimeField.DAY(),
                            null,
                            DatetimeField.SECOND(),
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL DAY (2) TO SECOND)",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Range(
                            DatetimeField.DAY(),
                            2,
                            DatetimeField.SECOND(),
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL DAY TO SECOND (3))",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Range(
                            DatetimeField.DAY(),
                            null,
                            DatetimeField.SECOND(),
                            3
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL DAY (2) TO SECOND (3))",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Range(
                            DatetimeField.DAY(),
                            2,
                            DatetimeField.SECOND(),
                            3
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL HOUR TO MINUTE)",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Range(
                            DatetimeField.HOUR(),
                            null,
                            DatetimeField.MINUTE(),
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL HOUR (2) TO MINUTE)",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Range(
                            DatetimeField.HOUR(),
                            2,
                            DatetimeField.MINUTE(),
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL HOUR TO SECOND)",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Range(
                            DatetimeField.HOUR(),
                            null,
                            DatetimeField.SECOND(),
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL HOUR (2) TO SECOND)",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Range(
                            DatetimeField.HOUR(),
                            2,
                            DatetimeField.SECOND(),
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL HOUR TO SECOND (3))",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Range(
                            DatetimeField.HOUR(),
                            null,
                            DatetimeField.SECOND(),
                            3
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL HOUR (2) TO SECOND (3))",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Range(
                            DatetimeField.HOUR(),
                            2,
                            DatetimeField.SECOND(),
                            3
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL MINUTE TO SECOND)",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Range(
                            DatetimeField.MINUTE(),
                            null,
                            DatetimeField.SECOND(),
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL MINUTE (2) TO SECOND)",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Range(
                            DatetimeField.MINUTE(),
                            2,
                            DatetimeField.SECOND(),
                            null
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL MINUTE TO SECOND (3))",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Range(
                            DatetimeField.MINUTE(),
                            null,
                            DatetimeField.SECOND(),
                            3
                        )
                    ),
                )
            ),
            expect(
                "CAST(i AS INTERVAL MINUTE (2) TO SECOND (3))",
                exprCast(
                    value = v("i"),
                    asType = DataType.INTERVAL(
                        IntervalQualifier.Range(
                            DatetimeField.MINUTE(),
                            2,
                            DatetimeField.SECOND(),
                            3
                        )
                    ),
                )
            )
        )

        // INTERVAL arithmetic
        @JvmStatic
        private fun intervalArithCases() = listOf(
            expect(
                "v + INTERVAL '10' YEAR",
                exprOperator(
                    symbol = "+",
                    lhs = v("v"),
                    rhs = interval(
                        qualifier = IntervalQualifier.Single(
                            DatetimeField.YEAR(),
                            null,
                            null
                        ),
                        value = "10"
                    ),
                )
            ),
            expect(
                "INTERVAL '10' YEAR + v",
                exprOperator(
                    symbol = "+",
                    lhs = interval(
                        qualifier = IntervalQualifier.Single(
                            DatetimeField.YEAR(),
                            null,
                            null
                        ),
                        value = "10"
                    ),
                    rhs = v("v"),
                )
            ),
            expect(
                "v - INTERVAL '10' YEAR",
                exprOperator(
                    symbol = "-",
                    lhs = v("v"),
                    rhs = interval(
                        qualifier = IntervalQualifier.Single(
                            DatetimeField.YEAR(),
                            null,
                            null
                        ),
                        value = "10"
                    ),
                )
            ),
            expect(
                "INTERVAL '10' YEAR - v",
                exprOperator(
                    symbol = "-",
                    lhs = interval(
                        qualifier = IntervalQualifier.Single(
                            DatetimeField.YEAR(),
                            null,
                            null
                        ),
                        value = "10"
                    ),
                    rhs = v("v"),
                )
            ),
            expect(
                "v * INTERVAL '10' YEAR",
                exprOperator(
                    symbol = "*",
                    lhs = v("v"),
                    rhs = interval(
                        qualifier = IntervalQualifier.Single(
                            DatetimeField.YEAR(),
                            null,
                            null
                        ),
                        value = "10"
                    ),
                )
            ),
            expect(
                "INTERVAL '10' YEAR * v",
                exprOperator(
                    symbol = "*",
                    lhs = interval(
                        qualifier = IntervalQualifier.Single(
                            DatetimeField.YEAR(),
                            null,
                            null
                        ),
                        value = "10"
                    ),
                    rhs = v("v"),
                )
            ),
            expect(
                "v / INTERVAL '10' YEAR",
                exprOperator(
                    symbol = "/",
                    lhs = v("v"),
                    rhs = interval(
                        qualifier = IntervalQualifier.Single(
                            DatetimeField.YEAR(),
                            null,
                            null
                        ),
                        value = "10"
                    ),
                )
            ),
            expect(
                "INTERVAL '10' YEAR / v",
                exprOperator(
                    symbol = "/",
                    lhs = interval(
                        qualifier = IntervalQualifier.Single(
                            DatetimeField.YEAR(),
                            null,
                            null
                        ),
                        value = "10"
                    ),
                    rhs = v("v"),
                )
            ),
        )

        @JvmStatic
        private fun intervalRangeCases() = listOf(
            // <start field> TO <end field>
            expect(
                "INTERVAL '10-3' YEAR TO MONTH",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.YEAR(),
                        null,
                        DatetimeField.MONTH(),
                        null
                    ),
                    value = "10-3"
                )
            ),
            expect(
                "INTERVAL '-10-3' YEAR TO MONTH",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.YEAR(),
                        null,
                        DatetimeField.MONTH(),
                        null
                    ),
                    value = "-10-3"
                )
            ),
            expect(
                "INTERVAL '10-3' YEAR (2) TO MONTH",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.YEAR(),
                        2,
                        DatetimeField.MONTH(),
                        null
                    ),
                    value = "10-3"
                )
            ),
            expect(
                "INTERVAL '-10-3' YEAR (2) TO MONTH",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.YEAR(),
                        2,
                        DatetimeField.MONTH(),
                        null
                    ),
                    value = "-10-3"
                )
            ),
            expect(
                "INTERVAL '10 3' DAY TO HOUR",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.DAY(),
                        null,
                        DatetimeField.HOUR(),
                        null
                    ),
                    value = "10 3"
                )
            ),
            expect(
                "INTERVAL '-10 3' DAY TO HOUR",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.DAY(),
                        null,
                        DatetimeField.HOUR(),
                        null
                    ),
                    value = "-10 3"
                )
            ),
            expect(
                "INTERVAL '10 3' DAY (2) TO HOUR",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.DAY(),
                        2,
                        DatetimeField.HOUR(),
                        null
                    ),
                    value = "10 3"
                )
            ),
            expect(
                "INTERVAL '-10 3' DAY (2) TO HOUR",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.DAY(),
                        2,
                        DatetimeField.HOUR(),
                        null
                    ),
                    value = "-10 3"
                )
            ),
            expect(
                "INTERVAL '10 3:4' DAY TO MINUTE",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.DAY(),
                        null,
                        DatetimeField.MINUTE(),
                        null
                    ),
                    value = "10 3:4"
                )
            ),
            expect(
                "INTERVAL '-10 3:4' DAY TO MINUTE",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.DAY(),
                        null,
                        DatetimeField.MINUTE(),
                        null
                    ),
                    value = "-10 3:4"
                )
            ),
            expect(
                "INTERVAL '10 3:4' DAY (2) TO MINUTE",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.DAY(),
                        2,
                        DatetimeField.MINUTE(),
                        null
                    ),
                    value = "10 3:4"
                )
            ),
            expect(
                "INTERVAL '-10 3:4' DAY (2) TO MINUTE",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.DAY(),
                        2,
                        DatetimeField.MINUTE(),
                        null
                    ),
                    value = "-10 3:4"
                )
            ),
            expect(
                "INTERVAL '10 3:4:5' DAY TO SECOND",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.DAY(),
                        null,
                        DatetimeField.SECOND(),
                        null
                    ),
                    value = "10 3:4:5"
                )
            ),
            expect(
                "INTERVAL '-10 3:4:5' DAY TO SECOND",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.DAY(),
                        null,
                        DatetimeField.SECOND(),
                        null
                    ),
                    value = "-10 3:4:5"
                )
            ),
            expect(
                "INTERVAL '10 3:4:5' DAY (2) TO SECOND",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.DAY(),
                        2,
                        DatetimeField.SECOND(),
                        null
                    ),
                    value = "10 3:4:5"
                )
            ),
            expect(
                "INTERVAL '-10 3:4:5' DAY (2) TO SECOND",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.DAY(),
                        2,
                        DatetimeField.SECOND(),
                        null
                    ),
                    value = "-10 3:4:5"
                )
            ),
            expect(
                "INTERVAL '10 3:4:5.678' DAY TO SECOND (3)",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.DAY(),
                        null,
                        DatetimeField.SECOND(),
                        3
                    ),
                    value = "10 3:4:5.678"
                )
            ),
            expect(
                "INTERVAL '-10 3:4:5.678' DAY TO SECOND (3)",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.DAY(),
                        null,
                        DatetimeField.SECOND(),
                        3
                    ),
                    value = "-10 3:4:5.678"
                )
            ),
            expect(
                "INTERVAL '10 3:4:5.678' DAY (2) TO SECOND (3)",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.DAY(),
                        2,
                        DatetimeField.SECOND(),
                        3
                    ),
                    value = "10 3:4:5.678"
                )
            ),
            expect(
                "INTERVAL '-10 3:4:5.678' DAY (2) TO SECOND (3)",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.DAY(),
                        2,
                        DatetimeField.SECOND(),
                        3
                    ),
                    value = "-10 3:4:5.678"
                )
            ),
            expect(
                "INTERVAL '3:4' HOUR TO MINUTE",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.HOUR(),
                        null,
                        DatetimeField.MINUTE(),
                        null
                    ),
                    value = "3:4"
                )
            ),
            expect(
                "INTERVAL '-3:4' HOUR TO MINUTE",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.HOUR(),
                        null,
                        DatetimeField.MINUTE(),
                        null
                    ),
                    value = "-3:4"
                )
            ),
            expect(
                "INTERVAL '3:4' HOUR (2) TO MINUTE",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.HOUR(),
                        2,
                        DatetimeField.MINUTE(),
                        null
                    ),
                    value = "3:4"
                )
            ),
            expect(
                "INTERVAL '-3:4' HOUR (2) TO MINUTE",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.HOUR(),
                        2,
                        DatetimeField.MINUTE(),
                        null
                    ),
                    value = "-3:4"
                )
            ),
            expect(
                "INTERVAL '2:3:4' HOUR TO SECOND",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.HOUR(),
                        null,
                        DatetimeField.SECOND(),
                        null
                    ),
                    value = "2:3:4"
                )
            ),
            expect(
                "INTERVAL '-2:3:4' HOUR TO SECOND",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.HOUR(),
                        null,
                        DatetimeField.SECOND(),
                        null
                    ),
                    value = "-2:3:4"
                )
            ),
            expect(
                "INTERVAL '2:3:4' HOUR (2) TO SECOND",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.HOUR(),
                        2,
                        DatetimeField.SECOND(),
                        null
                    ),
                    value = "2:3:4"
                )
            ),
            expect(
                "INTERVAL '-2:3:4' HOUR (2) TO SECOND",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.HOUR(),
                        2,
                        DatetimeField.SECOND(),
                        null
                    ),
                    value = "-2:3:4"
                )
            ),
            expect(
                "INTERVAL '2:3:4.567' HOUR TO SECOND (3)",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.HOUR(),
                        null,
                        DatetimeField.SECOND(),
                        3
                    ),
                    value = "2:3:4.567"
                )
            ),
            expect(
                "INTERVAL '-2:3:4.567' HOUR TO SECOND (3)",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.HOUR(),
                        null,
                        DatetimeField.SECOND(),
                        3
                    ),
                    value = "-2:3:4.567"
                )
            ),
            expect(
                "INTERVAL '2:3:4.567' HOUR (2) TO SECOND (3)",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.HOUR(),
                        2,
                        DatetimeField.SECOND(),
                        3
                    ),
                    value = "2:3:4.567"
                )
            ),
            expect(
                "INTERVAL '-2:3:4.567' HOUR (2) TO SECOND (3)",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.HOUR(),
                        2,
                        DatetimeField.SECOND(),
                        3
                    ),
                    value = "-2:3:4.567"
                )
            ),
            expect(
                "INTERVAL '3:4' MINUTE TO SECOND",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.MINUTE(),
                        null,
                        DatetimeField.SECOND(),
                        null
                    ),
                    value = "3:4"
                )
            ),
            expect(
                "INTERVAL '-3:4' MINUTE TO SECOND",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.MINUTE(),
                        null,
                        DatetimeField.SECOND(),
                        null
                    ),
                    value = "-3:4"
                )
            ),
            expect(
                "INTERVAL '3:4' MINUTE (2) TO SECOND",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.MINUTE(),
                        2,
                        DatetimeField.SECOND(),
                        null
                    ),
                    value = "3:4"
                )
            ),
            expect(
                "INTERVAL '-3:4' MINUTE (2) TO SECOND",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.MINUTE(),
                        2,
                        DatetimeField.SECOND(),
                        null
                    ),
                    value = "-3:4"
                )
            ),
            expect(
                "INTERVAL '3:4.567' MINUTE TO SECOND (3)",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.MINUTE(),
                        null,
                        DatetimeField.SECOND(),
                        3
                    ),
                    value = "3:4.567"
                )
            ),
            expect(
                "INTERVAL '-3:4.567' MINUTE TO SECOND (3)",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.MINUTE(),
                        null,
                        DatetimeField.SECOND(),
                        3
                    ),
                    value = "-3:4.567"
                )
            ),
            expect(
                "INTERVAL '3:4.567' MINUTE (2) TO SECOND (3)",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.MINUTE(),
                        2,
                        DatetimeField.SECOND(),
                        3
                    ),
                    value = "3:4.567"
                )
            ),
            expect(
                "INTERVAL '-3:4.567' MINUTE (2) TO SECOND (3)",
                interval(
                    qualifier = IntervalQualifier.Range(
                        DatetimeField.MINUTE(),
                        2,
                        DatetimeField.SECOND(),
                        3
                    ),
                    value = "-3:4.567"
                )
            )
        )

        @JvmStatic
        private fun windowFunctionCases() = listOf(
            // Window functions in SELECT statements
            expect(
                "SELECT ROW_NUMBER() OVER (ORDER BY id ASC NULLS LAST) FROM employees",
                qSet(
                    body = sfw(
                        select = selectList(
                            items = listOf(
                                selectItemExpr(
                                    exprWindowFunction(
                                        type = WindowFunctionType.RowNumber(),
                                        spec = windowSpecification(
                                            null,
                                            emptyList(),
                                            orderBy(listOf(sort(v("id"), Order.ASC(), Nulls.LAST())))
                                        )
                                    ),
                                    asAlias = null
                                )
                            ),
                            setq = null
                        ),
                        from = table("employees")
                    )
                )
            ),
            expect(
                "SELECT name, RANK() OVER (PARTITION BY department ORDER BY salary DESC NULLS LAST) AS rank FROM employees",
                qSet(
                    body = sfw(
                        select = selectList(
                            items = listOf(
                                selectItemExpr(v("name"), asAlias = null),
                                selectItemExpr(
                                    exprWindowFunction(
                                        type = WindowFunctionType.Rank(),
                                        spec = windowSpecification(
                                            null,
                                            listOf(windowPartition(Identifier.of(regular("department")))),
                                            orderBy(listOf(sort(v("salary"), Order.DESC(), Nulls.LAST())))
                                        )
                                    ),
                                    regular("rank")
                                )
                            ),
                            setq = null
                        ),
                        from = table("employees")
                    )
                )
            ),
            expect(
                "SELECT LAG(price, 1, NULL) OVER (ORDER BY date ASC NULLS LAST), LEAD(price, 1, NULL) OVER (ORDER BY date ASC NULLS LAST) FROM stocks",
                qSet(
                    body = sfw(
                        select = selectList(
                            items = listOf(
                                selectItemExpr(
                                    exprWindowFunction(
                                        type = WindowFunctionType.Lag(
                                            v("price"),
                                            1L,
                                            exprLit(nul()),
                                            null
                                        ),
                                        spec = windowSpecification(
                                            null,
                                            emptyList(),
                                            orderBy(listOf(sort(v("date"), Order.ASC(), Nulls.LAST())))
                                        )
                                    ),
                                    asAlias = null
                                ),
                                selectItemExpr(
                                    exprWindowFunction(
                                        type = WindowFunctionType.Lead(
                                            v("price"),
                                            1L,
                                            exprLit(nul()),
                                            null
                                        ),
                                        spec = windowSpecification(
                                            null,
                                            emptyList(),
                                            orderBy(listOf(sort(v("date"), Order.ASC(), Nulls.LAST())))
                                        )
                                    ),
                                    asAlias = null
                                )
                            ),
                            setq = null
                        ),
                        from = table("stocks")
                    )
                )
            ),
            // Basic window functions with OVER clause
            expect(
                "RANK() OVER (PARTITION BY department ORDER BY age ASC NULLS LAST)",
                exprWindowFunction(
                    type = WindowFunctionType.Rank(),
                    spec = windowSpecification(
                        null,
                        listOf(windowPartition(Identifier.of(regular("department")))),
                        orderBy(listOf(sort(v("age"), Order.ASC(), Nulls.LAST())))
                    )
                )
            ),
            expect(
                "DENSE_RANK() OVER (PARTITION BY department ORDER BY age ASC NULLS LAST, name ASC NULLS LAST)",
                exprWindowFunction(
                    type = WindowFunctionType.DenseRank(),
                    spec = windowSpecification(
                        null,
                        listOf(windowPartition(Identifier.of(regular("department")))),
                        orderBy(
                            listOf(
                                sort(v("age"), Order.ASC(), Nulls.LAST()),
                                sort(v("name"), Order.ASC(), Nulls.LAST())
                            )
                        )
                    )
                )
            ),
            expect(
                "ROW_NUMBER() OVER (ORDER BY id ASC NULLS LAST)",
                exprWindowFunction(
                    type = WindowFunctionType.RowNumber(),
                    spec = windowSpecification(
                        null,
                        emptyList(),
                        orderBy(listOf(sort(v("id"), Order.ASC(), Nulls.LAST())))
                    )
                )
            ),
            expect(
                "LAG(name, 1, 'UNKNOWN') OVER (PARTITION BY department ORDER BY age ASC NULLS LAST)",
                exprWindowFunction(
                    type = WindowFunctionType.Lag(
                        v("name"),
                        1L,
                        exprLit(string("UNKNOWN")),
                        null
                    ),
                    spec = windowSpecification(
                        null,
                        listOf(windowPartition(Identifier.of(regular("department")))),
                        orderBy(listOf(sort(v("age"), Order.ASC(), Nulls.LAST())))
                    )
                )
            ),
            expect(
                "LEAD(name, 1, 'UNKNOWN') OVER (PARTITION BY department ORDER BY age DESC NULLS LAST)",
                exprWindowFunction(
                    type = WindowFunctionType.Lead(
                        v("name"),
                        1L,
                        exprLit(string("UNKNOWN")),
                        null
                    ),
                    spec = windowSpecification(
                        null,
                        listOf(windowPartition(Identifier.of(regular("department")))),
                        orderBy(listOf(sort(v("age"), Order.DESC(), Nulls.LAST())))
                    )
                )
            ),
            expect(
                "LAG(partner, 3, 'FALLBACK') OVER (ORDER BY age ASC NULLS LAST, name ASC NULLS LAST)",
                exprWindowFunction(
                    type = WindowFunctionType.Lag(
                        v("partner"),
                        3L,
                        exprLit(string("FALLBACK")),
                        null
                    ),
                    spec = windowSpecification(
                        null,
                        emptyList(),
                        orderBy(
                            listOf(
                                sort(v("age"), Order.ASC(), Nulls.LAST()),
                                sort(v("name"), Order.ASC(), Nulls.LAST())
                            )
                        )
                    )
                )
            ),
            // Window functions with named window reference
            expect(
                "RANK() OVER (w)",
                exprWindowFunction(
                    type = WindowFunctionType.Rank(),
                    spec = windowSpecification(
                        regular("w"),
                        emptyList(),
                        null
                    )
                )
            ),
            expect(
                "DENSE_RANK() OVER (w)",
                exprWindowFunction(
                    type = WindowFunctionType.DenseRank(),
                    spec = windowSpecification(
                        regular("w"),
                        emptyList(),
                        null
                    )
                )
            ),
            expect(
                "ROW_NUMBER() OVER (w)",
                exprWindowFunction(
                    type = WindowFunctionType.RowNumber(),
                    spec = windowSpecification(
                        regular("w"),
                        emptyList(),
                        null
                    )
                )
            ),
            expect(
                "LAG(name, 1, 'UNKNOWN') OVER (w)",
                exprWindowFunction(
                    type = WindowFunctionType.Lag(
                        v("name"),
                        1L,
                        exprLit(string("UNKNOWN")),
                        null
                    ),
                    spec = windowSpecification(
                        regular("w"),
                        emptyList(),
                        null
                    )
                )
            ),
            expect(
                "LEAD(name, 1, 'UNKNOWN') OVER (w)",
                exprWindowFunction(
                    type = WindowFunctionType.Lead(
                        v("name"),
                        1L,
                        exprLit(string("UNKNOWN")),
                        null
                    ),
                    spec = windowSpecification(
                        regular("w"),
                        emptyList(),
                        null
                    )
                )
            ),
            // Window functions without partition
            expect(
                "RANK() OVER (ORDER BY price ASC NULLS LAST)",
                exprWindowFunction(
                    type = WindowFunctionType.Rank(),
                    spec = windowSpecification(
                        null,
                        emptyList(),
                        orderBy(listOf(sort(v("price"), Order.ASC(), Nulls.LAST())))
                    )
                )
            ),
            // Window functions with multiple partitions
            expect(
                "ROW_NUMBER() OVER (PARTITION BY ticker, month ORDER BY date ASC NULLS LAST)",
                exprWindowFunction(
                    type = WindowFunctionType.RowNumber(),
                    spec = windowSpecification(
                        null,
                        listOf(
                            windowPartition(Identifier.of(regular("ticker"))),
                            windowPartition(Identifier.of(regular("month")))
                        ),
                        orderBy(listOf(sort(v("date"), Order.ASC(), Nulls.LAST())))
                    )
                )
            )
        )

        @JvmStatic
        private fun windowClauseCases() = listOf(
            expect(
                "SELECT a FROM T WINDOW w AS (ORDER BY x ASC NULLS LAST)",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T"),
                        window = windowClause(
                            listOf(
                                windowClauseDefinition(
                                    regular("w"),
                                    windowSpecification(
                                        null,
                                        emptyList(),
                                        orderBy(listOf(sort(v("x"), Order.ASC(), Nulls.LAST())))
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            expect(
                "SELECT a FROM T WINDOW w AS (PARTITION BY dept ORDER BY age ASC NULLS LAST), v AS (ORDER BY name ASC NULLS LAST)",
                qSet(
                    body = sfw(
                        select = select("a"),
                        from = table("T"),
                        window = windowClause(
                            listOf(
                                windowClauseDefinition(
                                    regular("w"),
                                    windowSpecification(
                                        null,
                                        listOf(windowPartition(Identifier.of(regular("dept")))),
                                        orderBy(listOf(sort(v("age"), Order.ASC(), Nulls.LAST())))
                                    )
                                ),
                                windowClauseDefinition(
                                    regular("v"),
                                    windowSpecification(
                                        null,
                                        emptyList(),
                                        orderBy(listOf(sort(v("name"), Order.ASC(), Nulls.LAST())))
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            expect(
                "SELECT a, RANK() OVER w FROM T WINDOW w AS (PARTITION BY dept, region ORDER BY salary DESC NULLS FIRST)",
                qSet(
                    body = sfw(
                        select = selectList(
                            items = listOf(
                                selectItemExpr(v("a"), asAlias = null),
                                selectItemExpr(
                                    exprWindowFunction(
                                        type = WindowFunctionType.Rank(),
                                        spec = windowSpecification(
                                            regular("w"),
                                            emptyList(),
                                            null
                                        )
                                    ),
                                    asAlias = null
                                )
                            ),
                            setq = null
                        ),
                        from = table("T"),
                        window = windowClause(
                            listOf(
                                windowClauseDefinition(
                                    regular("w"),
                                    windowSpecification(
                                        null,
                                        listOf(
                                            windowPartition(Identifier.of(regular("dept"))),
                                            windowPartition(Identifier.of(regular("region")))
                                        ),
                                        orderBy(listOf(sort(v("salary"), Order.DESC(), Nulls.FIRST())))
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            expect(
                "SELECT RANK() OVER w1, ROW_NUMBER() OVER (ORDER BY id ASC NULLS LAST) FROM T WINDOW w1 AS (PARTITION BY dept ORDER BY salary DESC NULLS FIRST)",
                qSet(
                    body = sfw(
                        select = selectList(
                            items = listOf(
                                selectItemExpr(
                                    exprWindowFunction(
                                        type = WindowFunctionType.Rank(),
                                        spec = windowSpecification(
                                            regular("w1"),
                                            emptyList(),
                                            null
                                        )
                                    ),
                                    asAlias = null
                                ),
                                selectItemExpr(
                                    exprWindowFunction(
                                        type = WindowFunctionType.RowNumber(),
                                        spec = windowSpecification(
                                            null,
                                            emptyList(),
                                            orderBy(listOf(sort(v("id"), Order.ASC(), Nulls.LAST())))
                                        )
                                    ),
                                    asAlias = null
                                )
                            ),
                            setq = null
                        ),
                        from = table("T"),
                        window = windowClause(
                            listOf(
                                windowClauseDefinition(
                                    regular("w1"),
                                    windowSpecification(
                                        null,
                                        listOf(windowPartition(Identifier.of(regular("dept")))),
                                        orderBy(listOf(sort(v("salary"), Order.DESC(), Nulls.FIRST())))
                                    )
                                )
                            )
                        )
                    )
                )
            ),
            expect(
                "SELECT RANK() OVER w1, DENSE_RANK() OVER w2 FROM T WINDOW w1 AS (PARTITION BY dept ORDER BY salary DESC NULLS FIRST), w2 AS (ORDER BY age ASC NULLS LAST)",
                qSet(
                    body = sfw(
                        select = selectList(
                            items = listOf(
                                selectItemExpr(
                                    exprWindowFunction(
                                        type = WindowFunctionType.Rank(),
                                        spec = windowSpecification(
                                            regular("w1"),
                                            emptyList(),
                                            null
                                        )
                                    ),
                                    asAlias = null
                                ),
                                selectItemExpr(
                                    exprWindowFunction(
                                        type = WindowFunctionType.DenseRank(),
                                        spec = windowSpecification(
                                            regular("w2"),
                                            emptyList(),
                                            null
                                        )
                                    ),
                                    asAlias = null
                                )
                            ),
                            setq = null
                        ),
                        from = table("T"),
                        window = windowClause(
                            listOf(
                                windowClauseDefinition(
                                    regular("w1"),
                                    windowSpecification(
                                        null,
                                        listOf(windowPartition(Identifier.of(regular("dept")))),
                                        orderBy(listOf(sort(v("salary"), Order.DESC(), Nulls.FIRST())))
                                    )
                                ),
                                windowClauseDefinition(
                                    regular("w2"),
                                    windowSpecification(
                                        null,
                                        emptyList(),
                                        orderBy(listOf(sort(v("age"), Order.ASC(), Nulls.LAST())))
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        private fun interval(qualifier: IntervalQualifier, value: String) =
            exprLit(
                typedString(DataType.INTERVAL(qualifier), value)
            )

        private fun expect(
            expected: String,
            node: AstNode
        ): Case {
            return Case.Success(node, expected)
        }

        // DSL shorthand

        private fun v(symbol: String) = exprVarRef(
            identifier = Identifier.of(regular(symbol)),
            isQualified = false
        )

        private fun select(vararg s: String) = selectList(
            items = s.map {
                selectItemExpr(v(it), asAlias = null)
            },
            setq = null
        )

        private fun qSet(body: QueryBody, orderBy: OrderBy? = null, limit: Expr? = null, offset: Expr? = null, with: With? = null) = exprQuerySet(
            body = body,
            orderBy = orderBy,
            limit = limit,
            offset = offset,
            with = with
        )

        private fun sfw(select: Select, from: From, exclude: Exclude? = null, let: Let? = null, where: Expr? = null, groupBy: GroupBy? = null, having: Expr? = null, window: WindowClause? = null) = queryBodySFW(
            select = select,
            exclude = exclude,
            from = from,
            let = let,
            where = where,
            groupBy = groupBy,
            having = having,
            window = window
        )

        private fun table(symbol: String) = from(
            tableRefs = listOf(
                fromExpr(
                    expr = v(symbol),
                    fromType = FromType.SCAN(),
                    asAlias = null,
                    atAlias = null
                )
            )
        )

        private fun scan(symbol: String) = fromExpr(
            expr = v(symbol),
            fromType = FromType.SCAN(),
            asAlias = null,
            atAlias = null
        )
    }

    sealed class Case {

        abstract fun assert()

        class Success(
            private val input: AstNode,
            private val expected: String,
        ) : Case() {

            override fun assert() {
                val actual = input.sql(SqlLayout.ONELINE)
                Assertions.assertEquals(expected, actual)
            }
        }

        class Fail(
            private val input: AstNode,
            private val message: String,
        ) : Case() {

            override fun assert() {
                assertFails(message) {
                    input.sql(SqlLayout.ONELINE)
                }
            }
        }
    }
}
