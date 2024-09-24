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
import org.partiql.ast.AstNode
import org.partiql.ast.DatetimeField
import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.GroupBy
import org.partiql.ast.Identifier
import org.partiql.ast.SetOp
import org.partiql.ast.SetQuantifier
import org.partiql.ast.Sort
import org.partiql.ast.builder.AstBuilder
import org.partiql.ast.builder.ast
import org.partiql.ast.exprLit
import org.partiql.ast.exprVariant
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.boolValue
import org.partiql.value.dateValue
import org.partiql.value.datetime.DateTimeValue
import org.partiql.value.datetime.TimeZone
import org.partiql.value.decimalValue
import org.partiql.value.float32Value
import org.partiql.value.float64Value
import org.partiql.value.int16Value
import org.partiql.value.int32Value
import org.partiql.value.int64Value
import org.partiql.value.int8Value
import org.partiql.value.intValue
import org.partiql.value.missingValue
import org.partiql.value.nullValue
import org.partiql.value.stringValue
import org.partiql.value.symbolValue
import org.partiql.value.timeValue
import org.partiql.value.timestampValue
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertFails

/**
 * This tests the Ast to test via the base SqlDialect.
 *
 * It does NOT test formatted output.
 */
@OptIn(PartiQLValueExperimental::class)
class SqlDialectTest {

    // Identifiers & Paths

    @ParameterizedTest(name = "identifiers #{index}")
    @MethodSource("identifiers")
    @Execution(ExecutionMode.CONCURRENT)
    fun testIdentifiers(case: Case) = case.assert()

    @ParameterizedTest(name = "paths #{index}")
    @MethodSource("paths")
    @Execution(ExecutionMode.CONCURRENT)
    fun testPaths(case: Case) = case.assert()

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

    companion object {

        private val NULL = exprLit(nullValue())

        @JvmStatic
        fun types() = listOf(
            // SQL
            expect("NULL") { typeNullType() },
            expect("BOOL") { typeBool() },
            expect("SMALLINT") { typeSmallint() },
            expect("INT") { typeInt() },
            expect("REAL") { typeReal() },
            expect("FLOAT32") { typeFloat32() },
            expect("DOUBLE PRECISION") { typeFloat64() },
            expect("DECIMAL") { typeDecimal() },
            expect("DECIMAL(2)") { typeDecimal(2) },
            expect("DECIMAL(2,1)") { typeDecimal(2, 1) },
            expect("NUMERIC") { typeNumeric() },
            expect("NUMERIC(2)") { typeNumeric(2) },
            expect("NUMERIC(2,1)") { typeNumeric(2, 1) },
            expect("TIMESTAMP") { typeTimestamp() },
            expect("CHAR") { typeChar() },
            expect("CHAR(1)") { typeChar(1) },
            expect("VARCHAR") { typeVarchar() },
            expect("VARCHAR(1)") { typeVarchar(1) },
            expect("BLOB") { typeBlob() },
            expect("CLOB") { typeClob() },
            expect("DATE") { typeDate() },
            expect("TIME") { typeTime() },
            expect("TIME(1)") { typeTime(1) },
            expect("TIME WITH TIMEZONE") { typeTimeWithTz() },
            expect("TIME WITH TIMEZONE (1)") { typeTimeWithTz(1) },
            // TODO TIMESTAMP
            // TODO INTERVAL
            // PartiQL
            expect("MISSING") { typeMissing() },
            expect("STRING") { typeString() },
            expect("SYMBOL") { typeSymbol() },
            expect("STRUCT") { typeStruct() },
            expect("TUPLE") { typeTuple() },
            expect("LIST") { typeList() },
            expect("SEXP") { typeSexp() },
            expect("BAG") { typeBag() },
            expect("ANY") { typeAny() },
            // Other (??)
            expect("INT4") { typeInt4() },
            expect("INT8") { typeInt8() },
            //
            fail("PartiQLDialect does not support custom types") { typeCustom("foo") },
        )

        @JvmStatic
        fun exprOperators() = listOf(
            expect("NOT (NULL)") {
                exprNot {
                    value = NULL
                }
            },
            expect("+(NULL)") {
                exprOperator {
                    symbol = "+"
                    rhs = NULL
                }
            },
            expect("-(NULL)") {
                exprOperator {
                    symbol = "-"
                    rhs = NULL
                }
            },
            expect("NOT (NOT (NULL))") {
                exprNot {
                    value = exprNot {
                        value = NULL
                    }
                }
            },
            expect("+(+(NULL))") {
                exprOperator {
                    symbol = "+"
                    rhs = exprOperator {
                        symbol = "+"
                        rhs = NULL
                    }
                }
            },
            expect("-(-(NULL))") {
                exprOperator {
                    symbol = "-"
                    rhs = exprOperator {
                        symbol = "-"
                        rhs = NULL
                    }
                }
            },
            expect("+(-(+(NULL)))") {
                exprOperator {
                    symbol = "+"
                    rhs = exprOperator {
                        symbol = "-"
                        rhs = exprOperator {
                            symbol = "+"
                            rhs = NULL
                        }
                    }
                }
            },
            expect("NULL + NULL") {
                exprOperator {
                    symbol = "+"
                    lhs = NULL
                    rhs = NULL
                }
            },
        )

        @JvmStatic
        fun identifiers() = listOf(
            expect("x") {
                id("x")
            },
            expect("X") {
                id("X")
            },
            expect("\"x\"") {
                id("x", Identifier.CaseSensitivity.SENSITIVE)
            },
            expect("x.y.z") {
                identifierQualified {
                    root = id("x")
                    steps += id("y")
                    steps += id("z")
                }
            },
            expect("x.\"y\".z") {
                identifierQualified {
                    root = id("x")
                    steps += id("y", Identifier.CaseSensitivity.SENSITIVE)
                    steps += id("z")
                }
            },
            expect("\"x\".\"y\".\"z\"") {
                identifierQualified {
                    root = id("x", Identifier.CaseSensitivity.SENSITIVE)
                    steps += id("y", Identifier.CaseSensitivity.SENSITIVE)
                    steps += id("z", Identifier.CaseSensitivity.SENSITIVE)
                }
            },
        )

        @JvmStatic
        fun paths() = listOf(
            expect("x.y.z") {
                path {
                    root = id("x")
                    steps += pathStepSymbol(id("y"))
                    steps += pathStepSymbol(id("z"))
                }
            },
            expect("x.y[0]") {
                path {
                    root = id("x")
                    steps += pathStepSymbol(id("y"))
                    steps += pathStepIndex(0)
                }
            },
            expect("x[0].y") {
                path {
                    root = id("x")
                    steps += pathStepIndex(0)
                    steps += pathStepSymbol(id("y"))
                }
            },
            expect("\"x\".\"y\".\"z\"") {
                path {
                    root = id("x", Identifier.CaseSensitivity.SENSITIVE)
                    steps += pathStepSymbol(id("y", Identifier.CaseSensitivity.SENSITIVE))
                    steps += pathStepSymbol(id("z", Identifier.CaseSensitivity.SENSITIVE))
                }
            },
        )

        // Expressions

        @JvmStatic
        fun exprLitCases() = listOf(
            expect("NULL") {
                exprLit(nullValue())
            },
            expect("MISSING") {
                exprLit(missingValue())
            },
            expect("true") {
                exprLit(boolValue(true))
            },
            expect("1") {
                exprLit(int8Value(1))
            },
            expect("2") {
                exprLit(int16Value(2))
            },
            expect("3") {
                exprLit(int32Value(3))
            },
            expect("4") {
                exprLit(int64Value(4))
            },
            expect("5") {
                exprLit(intValue(BigInteger.valueOf(5)))
            },
            // TODO fix PartiQL Text writer for floats
            // expect("1.1e0") {
            expect("1.1") {
                exprLit(float32Value(1.1f))
            },
            // TODO fix PartiQL Text writer for floats
            // expect("1.2e0") {
            expect("1.2") {
                exprLit(float64Value(1.2))
            },
            expect("1.3") {
                exprLit(decimalValue(BigDecimal.valueOf(1.3)))
            },
            expect("""'hello'""") {
                exprLit(stringValue("hello"))
            },
            expect("""hello""") {
                exprLit(symbolValue("hello"))
            },
            expect("DATE '0001-02-03'") {
                exprLit(dateValue(DateTimeValue.date(1, 2, 3)))
            },
            expect("TIME '01:02:03.456-00:30'") {
                exprLit(timeValue(DateTimeValue.time(1, 2, BigDecimal.valueOf(3.456), TimeZone.UtcOffset.of(-30))))
            },
            expect("TIMESTAMP '0001-02-03 04:05:06.78-00:30'") {
                exprLit(timestampValue(DateTimeValue.timestamp(1, 2, 3, 4, 5, BigDecimal.valueOf(6.78), TimeZone.UtcOffset.of(-30))))
            },

            // expect("""{{ '''Hello'''    '''World''' }}""") {
            //     exprLit(clobValue("HelloWorld".toByteArray()))
            // },
            // expect("""{{ VG8gaW5maW5pdHkuLi4gYW5kIGJleW9uZCE= }}""") {
            //     exprLit(blobValue("To infinity... and beyond!".toByteArray()))
            // },
        )

        @JvmStatic
        fun exprIonCases() = listOf(
            expect("`null`") {
                exprIon(ionNull())
            },
            expect("`true`") {
                exprIon(ionBool(true))
            },
            expect("`1`") {
                exprIon(ionInt(1))
            },
            expect("`1.2e0`") {
                exprIon(ionFloat(1.2))
            },
            expect("`1.3`") {
                exprIon(ionDecimal(Decimal.valueOf(1.3)))
            },
            expect("""`"hello"`""") {
                exprIon(ionString("hello"))
            },
            expect("""`hello`""") {
                exprIon(ionSymbol("hello"))
            },
            expect("`a::b::null`") {
                exprIon(ionNull().withAnnotations("a", "b"))
            },
            expect("`a::b::true`") {
                exprIon(ionBool(true).withAnnotations("a", "b"))
            },
            expect("`a::b::1`") {
                exprIon(ionInt(1).withAnnotations("a", "b"))
            },
            expect("`a::b::1.2e0`") {
                exprIon(ionFloat(1.2).withAnnotations("a", "b"))
            },
            expect("`a::b::1.3`") {
                exprIon(ionDecimal(Decimal.valueOf(1.3)).withAnnotations("a", "b"))
            },
            expect("""`a::b::"hello"`""") {
                exprIon(ionString("hello").withAnnotations("a", "b"))
            },
            expect("""`a::b::hello`""") {
                exprIon(ionSymbol("hello").withAnnotations("a", "b"))
            },
        )

        private fun exprIon(value: IonElement): Expr = exprVariant(value.toString(), "ion")

        @JvmStatic
        fun exprVarCases() = listOf(
            // DEFAULT
            expect("x") {
                val id = id("x")
                exprVar(id, Expr.Var.Scope.DEFAULT)
            },
            expect("\"x\"") {
                val id = id("x", Identifier.CaseSensitivity.SENSITIVE)
                exprVar(id, Expr.Var.Scope.DEFAULT)
            },
            expect("x.y.z") {
                val id = identifierQualified {
                    root = id("x")
                    steps += id("y")
                    steps += id("z")
                }
                exprVar(id, Expr.Var.Scope.DEFAULT)
            },
            expect("x.\"y\".z") {
                val id = identifierQualified {
                    root = id("x")
                    steps += id("y", Identifier.CaseSensitivity.SENSITIVE)
                    steps += id("z")
                }
                exprVar(id, Expr.Var.Scope.DEFAULT)
            },
            expect("\"x\".\"y\".\"z\"") {
                val id = identifierQualified {
                    root = id("x", Identifier.CaseSensitivity.SENSITIVE)
                    steps += id("y", Identifier.CaseSensitivity.SENSITIVE)
                    steps += id("z", Identifier.CaseSensitivity.SENSITIVE)
                }
                exprVar(id, Expr.Var.Scope.DEFAULT)
            },
            // LOCAL
            expect("@x") {
                val id = id("x")
                exprVar(id, Expr.Var.Scope.LOCAL)
            },
            expect("@\"x\"") {
                val id = id("x", Identifier.CaseSensitivity.SENSITIVE)
                exprVar(id, Expr.Var.Scope.LOCAL)
            },
            expect("@x.y.z") {
                val id = identifierQualified {
                    root = id("x")
                    steps += id("y")
                    steps += id("z")
                }
                exprVar(id, Expr.Var.Scope.LOCAL)
            },
            expect("@x.\"y\".z") {
                val id = identifierQualified {
                    root = id("x")
                    steps += id("y", Identifier.CaseSensitivity.SENSITIVE)
                    steps += id("z")
                }
                exprVar(id, Expr.Var.Scope.LOCAL)
            },
            expect("@\"x\".\"y\".\"z\"") {
                val id = identifierQualified {
                    root = id("x", Identifier.CaseSensitivity.SENSITIVE)
                    steps += id("y", Identifier.CaseSensitivity.SENSITIVE)
                    steps += id("z", Identifier.CaseSensitivity.SENSITIVE)
                }
                exprVar(id, Expr.Var.Scope.LOCAL)
            },
        )

        @JvmStatic
        fun exprPathCases() = listOf(
            expect("x.y.*") {
                exprPath {
                    root = exprVar {
                        identifier = id("x")
                        scope = Expr.Var.Scope.DEFAULT
                    }
                    steps += exprPathStepSymbol(id("y"))
                    steps += exprPathStepUnpivot()
                }
            },
            expect("x.y[*]") {
                exprPath {
                    root = exprVar {
                        identifier = id("x")
                        scope = Expr.Var.Scope.DEFAULT
                    }
                    steps += exprPathStepSymbol(id("y"))
                    steps += exprPathStepWildcard()
                }
            },
            expect("x[1 + a]") {
                exprPath {
                    root = exprVar {
                        identifier = id("x")
                        scope = Expr.Var.Scope.DEFAULT
                    }
                    steps += exprPathStepIndex(
                        exprOperator {
                            symbol = "+"
                            lhs = exprLit(int32Value(1))
                            rhs = exprVar {
                                identifier = id("a")
                                scope = Expr.Var.Scope.DEFAULT
                            }
                        }
                    )
                }
            },
            expect("x['y']") {
                exprPath {
                    root = exprVar {
                        identifier = id("x")
                        scope = Expr.Var.Scope.DEFAULT
                    }
                    steps += exprPathStepIndex(exprLit(stringValue("y")))
                }
            },
        )

        @JvmStatic
        fun exprCallCases() = listOf(
            expect("foo(1)") {
                exprCall {
                    function = id("foo")
                    args += exprLit(int32Value(1))
                }
            },
            expect("foo(1, 2)") {
                exprCall {
                    function = id("foo")
                    args += exprLit(int32Value(1))
                    args += exprLit(int32Value(2))
                }
            },
            expect("foo.bar(1)") {
                exprCall {
                    function = identifierQualified {
                        root = id("foo")
                        steps += id("bar")
                    }
                    args += exprLit(int32Value(1))
                }
            },
            expect("foo.bar(1, 2)") {
                exprCall {
                    function = identifierQualified {
                        root = id("foo")
                        steps += id("bar")
                    }
                    args += exprLit(int32Value(1))
                    args += exprLit(int32Value(2))
                }
            },
        )

        @JvmStatic
        fun exprAggCases() = listOf(
            expect("FOO(x)") {
                exprCall {
                    function = id("FOO")
                    args += exprVar(id("x"), Expr.Var.Scope.DEFAULT)
                }
            },
            expect("FOO(ALL x)") {
                exprCall {
                    function = id("FOO")
                    setq = SetQuantifier.ALL
                    args += exprVar(id("x"), Expr.Var.Scope.DEFAULT)
                }
            },
            expect("FOO(DISTINCT x)") {
                exprCall {
                    function = id("FOO")
                    setq = SetQuantifier.DISTINCT
                    args += exprVar(id("x"), Expr.Var.Scope.DEFAULT)
                }
            },
            expect("FOO(x, y)") {
                exprCall {
                    function = id("FOO")
                    args += exprVar(id("x"), Expr.Var.Scope.DEFAULT)
                    args += exprVar(id("y"), Expr.Var.Scope.DEFAULT)
                }
            },
            expect("FOO(ALL x, y)") {
                exprCall {
                    function = id("FOO")
                    setq = SetQuantifier.ALL
                    args += exprVar(id("x"), Expr.Var.Scope.DEFAULT)
                    args += exprVar(id("y"), Expr.Var.Scope.DEFAULT)
                }
            },
            expect("FOO(DISTINCT x, y)") {
                exprCall {
                    function = id("FOO")
                    setq = SetQuantifier.DISTINCT
                    args += exprVar(id("x"), Expr.Var.Scope.DEFAULT)
                    args += exprVar(id("y"), Expr.Var.Scope.DEFAULT)
                }
            },
            expect("COUNT(*)") {
                exprCall {
                    function = id("COUNT") // AST representation for COUNT w/ no args maps to COUNT(*)
                }
            }
        )

        @JvmStatic
        fun exprCollectionCases() = listOf(
            expect("<<>>") {
                exprCollection {
                    type = Expr.Collection.Type.BAG
                }
            },
            expect("<<1, 2, 3>>") {
                exprCollection {
                    type = Expr.Collection.Type.BAG
                    values += exprLit(int32Value(1))
                    values += exprLit(int32Value(2))
                    values += exprLit(int32Value(3))
                }
            },
            expect("[]") {
                exprCollection {
                    type = Expr.Collection.Type.ARRAY
                }
            },
            expect("[1, 2, 3]") {
                exprCollection {
                    type = Expr.Collection.Type.ARRAY
                    values += exprLit(int32Value(1))
                    values += exprLit(int32Value(2))
                    values += exprLit(int32Value(3))
                }
            },
            expect("VALUES ()") {
                exprCollection {
                    type = Expr.Collection.Type.VALUES
                }
            },
            expect("VALUES (1, 2, 3)") {
                exprCollection {
                    type = Expr.Collection.Type.VALUES
                    values += exprLit(int32Value(1))
                    values += exprLit(int32Value(2))
                    values += exprLit(int32Value(3))
                }
            },
            expect("()") {
                exprCollection {
                    type = Expr.Collection.Type.LIST
                }
            },
            expect("(1, 2, 3)") {
                exprCollection {
                    type = Expr.Collection.Type.LIST
                    values += exprLit(int32Value(1))
                    values += exprLit(int32Value(2))
                    values += exprLit(int32Value(3))
                }
            },
            expect("SEXP ()") {
                exprCollection {
                    type = Expr.Collection.Type.SEXP
                }
            },
            expect("SEXP (1, 2, 3)") {
                exprCollection {
                    type = Expr.Collection.Type.SEXP
                    values += exprLit(int32Value(1))
                    values += exprLit(int32Value(2))
                    values += exprLit(int32Value(3))
                }
            },
        )

        @JvmStatic
        fun exprStructCases() = listOf(
            expect("{}") {
                exprStruct()
            },
            expect("{a: 1}") {
                exprStruct {
                    fields += exprStructField {
                        name = exprLit(symbolValue("a"))
                        value = exprLit(int32Value(1))
                    }
                }
            },
            expect("{a: 1, b: false}") {
                exprStruct {
                    fields += exprStructField {
                        name = exprLit(symbolValue("a"))
                        value = exprLit(int32Value(1))
                    }
                    fields += exprStructField {
                        name = exprLit(symbolValue("b"))
                        value = exprLit(boolValue(false))
                    }
                }
            },
        )

        @JvmStatic
        fun exprSpecialFormCases() = listOf(
            expect("x LIKE y") {
                exprLike {
                    value = v("x")
                    pattern = v("y")
                }
            },
            expect("x NOT LIKE y") {
                exprLike {
                    value = v("x")
                    pattern = v("y")
                    not = true
                }
            },
            expect("x LIKE y ESCAPE z") {
                exprLike {
                    value = v("x")
                    pattern = v("y")
                    escape = v("z")
                }
            },
            expect("x BETWEEN y AND z") {
                exprBetween {
                    value = v("x")
                    from = v("y")
                    to = v("z")
                }
            },
            expect("x NOT BETWEEN y AND z") {
                exprBetween {
                    value = v("x")
                    from = v("y")
                    to = v("z")
                    not = true
                }
            },
            expect("x IN y") {
                exprInCollection {
                    lhs = v("x")
                    rhs = v("y")
                }
            },
            expect("x NOT IN y") {
                exprInCollection {
                    lhs = v("x")
                    rhs = v("y")
                    not = true
                }
            },
            expect("x IS BOOL") {
                exprIsType {
                    value = v("x")
                    type = typeBool()
                }
            },
            expect("x IS NOT BOOL") {
                exprIsType {
                    value = v("x")
                    type = typeBool()
                    not = true
                }
            },
            expect("NULLIF(x, y)") {
                exprNullIf {
                    value = v("x")
                    nullifier = v("y")
                }
            },
            expect("COALESCE(x, y, z)") {
                exprCoalesce {
                    args += v("x")
                    args += v("y")
                    args += v("z")
                }
            },
            expect("SUBSTRING(x)") {
                exprSubstring {
                    value = v("x")
                }
            },
            expect("SUBSTRING(x FROM i)") {
                exprSubstring {
                    value = v("x")
                    start = v("i")
                }
            },
            expect("SUBSTRING(x FROM i FOR n)") {
                exprSubstring {
                    value = v("x")
                    start = v("i")
                    length = v("n")
                }
            },
            expect("SUBSTRING(x FOR n)") {
                exprSubstring {
                    value = v("x")
                    length = v("n")
                }
            },
            expect("POSITION(x IN y)") {
                exprPosition {
                    lhs = v("x")
                    rhs = v("y")
                }
            },
            expect("TRIM(x)") {
                exprTrim {
                    value = v("x")
                }
            },
            expect("TRIM(BOTH x)") {
                exprTrim {
                    value = v("x")
                    spec = Expr.Trim.Spec.BOTH
                }
            },
            expect("TRIM(LEADING y FROM x)") {
                exprTrim {
                    value = v("x")
                    spec = Expr.Trim.Spec.LEADING
                    chars = v("y")
                }
            },
            expect("TRIM(y FROM x)") {
                exprTrim {
                    value = v("x")
                    chars = v("y")
                }
            },
            expect("OVERLAY(x PLACING y FROM z)") {
                exprOverlay {
                    value = v("x")
                    overlay = v("y")
                    start = v("z")
                }
            },
            expect("OVERLAY(x PLACING y FROM z FOR n)") {
                exprOverlay {
                    value = v("x")
                    overlay = v("y")
                    start = v("z")
                    length = v("n")
                }
            },
            expect("EXTRACT(MINUTE FROM x)") {
                exprExtract {
                    field = DatetimeField.MINUTE
                    source = v("x")
                }
            },
            expect("CAST(x AS INT)") {
                exprCast {
                    value = v("x")
                    asType = typeInt()
                }
            },
            expect("DATE_ADD(MINUTE, x, y)") {
                exprDateAdd {
                    field = DatetimeField.MINUTE
                    lhs = v("x")
                    rhs = v("y")
                }
            },
            expect("DATE_DIFF(MINUTE, x, y)") {
                exprDateDiff {
                    field = DatetimeField.MINUTE
                    lhs = v("x")
                    rhs = v("y")
                }
            },
            expect("x OUTER UNION y") {
                exprQuerySet {
                    body = queryBodySetOp {
                        type = setOp {
                            type = SetOp.Type.UNION
                            setq = null
                        }
                        isOuter = true
                        lhs = v("x")
                        rhs = v("y")
                    }
                }
            },
            expect("x OUTER UNION ALL y") {
                exprQuerySet {
                    body = queryBodySetOp {
                        type = setOp {
                            type = SetOp.Type.UNION
                            setq = SetQuantifier.ALL
                        }
                        isOuter = true
                        lhs = v("x")
                        rhs = v("y")
                    }
                }
            },
            expect("x OUTER UNION y") {
                exprQuerySet {
                    body = queryBodySetOp {
                        type = setOp {
                            type = SetOp.Type.UNION
                            setq = null
                        }
                        isOuter = true
                        lhs = v("x")
                        rhs = v("y")
                    }
                }
            },
            expect("x OUTER UNION ALL y") {
                exprQuerySet {
                    body = queryBodySetOp {
                        type = setOp {
                            type = SetOp.Type.UNION
                            setq = SetQuantifier.ALL
                        }
                        isOuter = true
                        lhs = v("x")
                        rhs = v("y")
                    }
                }
            },
            expect("(x UNION y) UNION z") {
                exprQuerySet {
                    body = queryBodySetOp {
                        type = setOp {
                            type = SetOp.Type.UNION
                            setq = null
                        }
                        isOuter = false
                        lhs = exprQuerySet {
                            body = queryBodySetOp {
                                type = setOp {
                                    type = SetOp.Type.UNION
                                    setq = null
                                }
                                isOuter = false
                                lhs = v("x")
                                rhs = v("y")
                            }
                        }
                        rhs = v("z")
                    }
                }
            },
            expect("x UNION (y UNION z)") {
                exprQuerySet {
                    body = queryBodySetOp {
                        type = setOp {
                            type = SetOp.Type.UNION
                            setq = null
                        }
                        isOuter = false
                        lhs = v("x")
                        rhs = exprQuerySet {
                            body = queryBodySetOp {
                                type = setOp {
                                    type = SetOp.Type.UNION
                                    setq = null
                                }
                                isOuter = false
                                lhs = v("y")
                                rhs = v("z")
                            }
                        }
                    }
                }
            },
            expect("(x EXCEPT y) EXCEPT z") {
                exprQuerySet {
                    body = queryBodySetOp {
                        type = setOp {
                            type = SetOp.Type.EXCEPT
                            setq = null
                        }
                        isOuter = false
                        lhs = exprQuerySet {
                            body = queryBodySetOp {
                                type = setOp {
                                    type = SetOp.Type.EXCEPT
                                    setq = null
                                }
                                isOuter = false
                                lhs = v("x")
                                rhs = v("y")
                            }
                            rhs = v("z")
                        }
                    }
                }
            },
            expect("x EXCEPT (y EXCEPT z)") {
                exprQuerySet {
                    body = queryBodySetOp {
                        type = setOp {
                            type = SetOp.Type.EXCEPT
                            setq = null
                        }
                        isOuter = false
                        lhs = v("x")
                        rhs = exprQuerySet {
                            body = queryBodySetOp {
                                type = setOp {
                                    type = SetOp.Type.EXCEPT
                                    setq = null
                                }
                                isOuter = false
                                lhs = v("y")
                                rhs = v("z")
                            }
                        }
                    }
                }
            },
            expect("(x INTERSECT y) INTERSECT z") {
                exprQuerySet {
                    body = queryBodySetOp {
                        type = setOp {
                            type = SetOp.Type.INTERSECT
                            setq = null
                        }
                        isOuter = false
                        lhs = exprQuerySet {
                            body = queryBodySetOp {
                                type = setOp {
                                    type = SetOp.Type.INTERSECT
                                    setq = null
                                }
                                isOuter = false
                                lhs = v("x")
                                rhs = v("y")
                            }
                        }
                        rhs = v("z")
                    }
                }
            },
            expect("x INTERSECT (y INTERSECT z)") {
                exprQuerySet {
                    body = queryBodySetOp {
                        type = setOp {
                            type = SetOp.Type.INTERSECT
                            setq = null
                        }
                        isOuter = false
                        lhs = v("x")
                        rhs = exprQuerySet {
                            body = queryBodySetOp {
                                type = setOp {
                                    type = SetOp.Type.INTERSECT
                                    setq = null
                                }
                                isOuter = false
                                lhs = v("y")
                                rhs = v("z")
                            }
                        }
                    }
                }
            },
        )

        @JvmStatic
        fun exprCaseCases() = listOf(
            expect("CASE WHEN a THEN x WHEN b THEN y END") {
                exprCase {
                    branches += exprCaseBranch(v("a"), v("x"))
                    branches += exprCaseBranch(v("b"), v("y"))
                }
            },
            expect("CASE z WHEN a THEN x WHEN b THEN y END") {
                exprCase {
                    expr = v("z")
                    branches += exprCaseBranch(v("a"), v("x"))
                    branches += exprCaseBranch(v("b"), v("y"))
                }
            },
            expect("CASE z WHEN a THEN x ELSE y END") {
                exprCase {
                    expr = v("z")
                    branches += exprCaseBranch(v("a"), v("x"))
                    default = v("y")
                }
            },
        )

        @JvmStatic
        fun selectClauseCases() = listOf(
            expect("SELECT a FROM T") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = selectProject {
                            items += selectProjectItemExpression(v("a"))
                        }
                        from = table("T")
                    }
                }
            },
            expect("SELECT a AS x FROM T") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = selectProject {
                            items += selectProjectItemExpression(v("a"), id("x"))
                        }
                        from = table("T")
                    }
                }
            },
            expect("SELECT a AS x, b AS y FROM T") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = selectProject {
                            items += selectProjectItemExpression(v("a"), id("x"))
                            items += selectProjectItemExpression(v("b"), id("y"))
                        }
                        from = table("T")
                    }
                }
            },
            expect("SELECT ALL a FROM T") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = selectProject {
                            setq = SetQuantifier.ALL
                            items += selectProjectItemExpression(v("a"))
                        }
                        from = table("T")
                    }
                }
            },
            expect("SELECT DISTINCT a FROM T") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = selectProject {
                            setq = SetQuantifier.DISTINCT
                            items += selectProjectItemExpression(v("a"))
                        }
                        from = table("T")
                    }
                }
            },
            expect("SELECT a.* FROM T") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = selectProject {
                            items += selectProjectItemAll(v("a"))
                        }
                        from = table("T")
                    }
                }
            },
            expect("SELECT * FROM T") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = selectStar()
                        from = table("T")
                    }
                }
            },
            expect("SELECT DISTINCT * FROM T") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = selectStar(SetQuantifier.DISTINCT)
                        from = table("T")
                    }
                }
            },
            expect("SELECT ALL * FROM T") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = selectStar(SetQuantifier.ALL)
                        from = table("T")
                    }
                }
            },
            expect("SELECT VALUE a FROM T") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = selectValue {
                            constructor = v("a")
                        }
                        from = table("T")
                    }
                }
            },
            expect("SELECT ALL VALUE a FROM T") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = selectValue {
                            setq = SetQuantifier.ALL
                            constructor = v("a")
                        }
                        from = table("T")
                    }
                }
            },
            expect("SELECT DISTINCT VALUE a FROM T") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = selectValue {
                            setq = SetQuantifier.DISTINCT
                            constructor = v("a")
                        }
                        from = table("T")
                    }
                }
            },
            expect("PIVOT a AT b FROM T") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = selectPivot(v("a"), v("b"))
                        from = table("T")
                    }
                }
            },
        )

        @JvmStatic
        fun excludeClauseCases() = listOf(
            expect("SELECT a EXCLUDE t.a FROM T") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = fromValue {
                            expr = v("T")
                            type = From.Value.Type.SCAN
                        }
                        exclude = exclude {
                            items += excludeItem {
                                root = v("t")
                                steps += insensitiveExcludeStructField("a")
                            }
                        }
                    }
                }
            },
            expect("SELECT a EXCLUDE a.b, c.d, e.f, g.h FROM T") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = fromValue {
                            expr = v("T")
                            type = From.Value.Type.SCAN
                        }
                        exclude = exclude {
                            items += excludeItem {
                                root = v("a")
                                steps += insensitiveExcludeStructField("b")
                            }
                            items += excludeItem {
                                root = v("c")
                                steps += insensitiveExcludeStructField("d")
                            }
                            items += excludeItem {
                                root = v("e")
                                steps += insensitiveExcludeStructField("f")
                            }
                            items += excludeItem {
                                root = v("g")
                                steps += insensitiveExcludeStructField("h")
                            }
                        }
                    }
                }
            },
            expect("SELECT a EXCLUDE t.a.\"b\".*[*].c, \"s\"[0].d.\"e\"[*].f.* FROM T") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = fromValue {
                            expr = v("T")
                            type = From.Value.Type.SCAN
                        }
                        exclude = exclude {
                            items += excludeItem {
                                root = v("t")
                                steps += mutableListOf(
                                    insensitiveExcludeStructField("a"),
                                    sensitiveExcludeStructField("b"),
                                    excludeStepStructWildcard(),
                                    excludeStepCollWildcard(),
                                    insensitiveExcludeStructField("c"),
                                )
                            }
                            items += excludeItem {
                                root = exprVar(id("s", Identifier.CaseSensitivity.SENSITIVE), Expr.Var.Scope.DEFAULT)
                                steps += mutableListOf(
                                    excludeStepCollIndex(0),
                                    insensitiveExcludeStructField("d"),
                                    sensitiveExcludeStructField("e"),
                                    excludeStepCollWildcard(),
                                    insensitiveExcludeStructField("f"),
                                    excludeStepStructWildcard(),
                                )
                            }
                        }
                    }
                }
            },
        )

        private fun AstBuilder.insensitiveExcludeStructField(str: String) = excludeStepStructField {
            symbol = id(str, Identifier.CaseSensitivity.INSENSITIVE)
        }

        private fun AstBuilder.sensitiveExcludeStructField(str: String) = excludeStepStructField {
            symbol = id(str, Identifier.CaseSensitivity.SENSITIVE)
        }

        @JvmStatic
        fun fromClauseCases() = listOf(
            expect("SELECT a FROM T") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = fromValue {
                            expr = v("T")
                            type = From.Value.Type.SCAN
                        }
                    }
                }
            },
            expect("SELECT a FROM T AS x") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = fromValue {
                            expr = v("T")
                            type = From.Value.Type.SCAN
                            asAlias = id("x")
                        }
                    }
                }
            },
            expect("SELECT a FROM T AS x AT y") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = fromValue {
                            expr = v("T")
                            type = From.Value.Type.SCAN
                            asAlias = id("x")
                            atAlias = id("y")
                        }
                    }
                }
            },
            expect("SELECT a FROM T AS x AT y BY z") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = fromValue {
                            expr = v("T")
                            type = From.Value.Type.SCAN
                            asAlias = id("x")
                            atAlias = id("y")
                            byAlias = id("z")
                        }
                    }
                }
            },
            expect("SELECT a FROM UNPIVOT T") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = fromValue {
                            expr = v("T")
                            type = From.Value.Type.UNPIVOT
                        }
                    }
                }
            },
            expect("SELECT a FROM UNPIVOT T AS x") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = fromValue {
                            expr = v("T")
                            type = From.Value.Type.UNPIVOT
                            asAlias = id("x")
                        }
                    }
                }
            },
            expect("SELECT a FROM UNPIVOT T AS x AT y") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = fromValue {
                            expr = v("T")
                            type = From.Value.Type.UNPIVOT
                            asAlias = id("x")
                            atAlias = id("y")
                        }
                    }
                }
            },
            expect("SELECT a FROM UNPIVOT T AS x AT y BY z") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = fromValue {
                            expr = v("T")
                            type = From.Value.Type.UNPIVOT
                            asAlias = id("x")
                            atAlias = id("y")
                            byAlias = id("z")
                        }
                    }
                }
            },
        )

        @JvmStatic
        fun joinClauseCases() = listOf(
            expect("SELECT a FROM T JOIN S") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = fromJoin {
                            lhs = table("T")
                            rhs = table("S")
                        }
                    }
                }
            },
            expect("SELECT a FROM T INNER JOIN S") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = fromJoin {
                            type = From.Join.Type.INNER
                            lhs = table("T")
                            rhs = table("S")
                        }
                    }
                }
            },
            // expect("SELECT a FROM T, S") {
            //     exprSFW {
            //         select = select("a")
            //         from = fromJoin {
            //             type = From.Join.Type.FULL
            //             lhs = table("T")
            //             rhs = table("S")
            //         }
            //     }
            // },
            // expect("SELECT a FROM T CROSS JOIN S") {
            //     exprSFW {
            //         select = select("a")
            //         from = fromJoin {
            //             type = From.Join.Type.FULL
            //             lhs = table("T")
            //             rhs = table("S")
            //         }
            //     }
            // },
            expect("SELECT a FROM T JOIN S ON NULL") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = fromJoin {
                            lhs = table("T")
                            rhs = table("S")
                            condition = NULL
                        }
                    }
                }
            },
            expect("SELECT a FROM T INNER JOIN S ON NULL") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = fromJoin {
                            type = From.Join.Type.INNER
                            lhs = table("T")
                            rhs = table("S")
                            condition = NULL
                        }
                    }
                }
            },
        )

        // These are simple clauses
        @JvmStatic
        private fun otherClausesCases() = listOf(
            expect("SELECT a FROM T LET x AS i") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                        let = let(mutableListOf()) {
                            bindings += letBinding(v("x"), id("i"))
                        }
                    }
                }
            },
            expect("SELECT a FROM T LET x AS i, y AS j") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                        let = let(mutableListOf()) {
                            bindings += letBinding(v("x"), id("i"))
                            bindings += letBinding(v("y"), id("j"))
                        }
                    }
                }
            },
            expect("SELECT a FROM T WHERE x") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                        where = v("x")
                    }
                }
            },
            expect("SELECT a FROM T LIMIT 1") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                    }
                    limit = exprLit(int32Value(1))
                }
            },
            expect("SELECT a FROM T OFFSET 2") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                    }
                    offset = exprLit(int32Value(2))
                }
            },
            expect("SELECT a FROM T LIMIT 1 OFFSET 2") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                    }
                    limit = exprLit(int32Value(1))
                    offset = exprLit(int32Value(2))
                }
            },
            expect("SELECT a FROM T GROUP BY x HAVING y") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                        groupBy = groupBy {
                            strategy = GroupBy.Strategy.FULL
                            keys += groupByKey(v("x"))
                        }
                        having = v("y")
                    }
                }
            },
        )

        @JvmStatic
        private fun groupByClauseCases() = listOf(
            expect("SELECT a FROM T GROUP BY x") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                        groupBy = groupBy {
                            strategy = GroupBy.Strategy.FULL
                            keys += groupByKey(v("x"))
                        }
                    }
                }
            },
            expect("SELECT a FROM T GROUP BY x AS i") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                        groupBy = groupBy {
                            strategy = GroupBy.Strategy.FULL
                            keys += groupByKey(v("x"), id("i"))
                        }
                    }
                }
            },
            expect("SELECT a FROM T GROUP BY x, y") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                        groupBy = groupBy {
                            strategy = GroupBy.Strategy.FULL
                            keys += groupByKey(v("x"))
                            keys += groupByKey(v("y"))
                        }
                    }
                }
            },
            expect("SELECT a FROM T GROUP BY x AS i, y AS j") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                        groupBy = groupBy {
                            strategy = GroupBy.Strategy.FULL
                            keys += groupByKey(v("x"), id("i"))
                            keys += groupByKey(v("y"), id("j"))
                        }
                    }
                }
            },
            expect("SELECT a FROM T GROUP BY x GROUP AS g") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                        groupBy = groupBy {
                            strategy = GroupBy.Strategy.FULL
                            keys += groupByKey(v("x"))
                            asAlias = id("g")
                        }
                    }
                }
            },
            expect("SELECT a FROM T GROUP BY x AS i GROUP AS g") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                        groupBy = groupBy {
                            strategy = GroupBy.Strategy.FULL
                            keys += groupByKey(v("x"), id("i"))
                            asAlias = id("g")
                        }
                    }
                }
            },
            expect("SELECT a FROM T GROUP BY x, y GROUP AS g") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                        groupBy = groupBy {
                            strategy = GroupBy.Strategy.FULL
                            keys += groupByKey(v("x"))
                            keys += groupByKey(v("y"))
                            asAlias = id("g")
                        }
                    }
                }
            },
            expect("SELECT a FROM T GROUP BY x AS i, y AS j GROUP AS g") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                        groupBy = groupBy {
                            strategy = GroupBy.Strategy.FULL
                            keys += groupByKey(v("x"), id("i"))
                            keys += groupByKey(v("y"), id("j"))
                            asAlias = id("g")
                        }
                    }
                }
            },
            expect("SELECT a FROM T GROUP PARTIAL BY x") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                        groupBy = groupBy {
                            strategy = GroupBy.Strategy.PARTIAL
                            keys += groupByKey(v("x"))
                        }
                    }
                }
            },
        )

        @JvmStatic
        private fun orderByClauseCases() = listOf(
            expect("SELECT a FROM T ORDER BY x") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                    }
                    orderBy = orderBy {
                        sorts += sort(v("x"), null, null)
                    }
                }
            },
            expect("SELECT a FROM T ORDER BY x ASC") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                    }
                    orderBy = orderBy {
                        sorts += sort(v("x"), Sort.Dir.ASC, null)
                    }
                }
            },
            expect("SELECT a FROM T ORDER BY x DESC") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                    }
                    orderBy = orderBy {
                        sorts += sort(v("x"), Sort.Dir.DESC, null)
                    }
                }
            },
            expect("SELECT a FROM T ORDER BY x NULLS FIRST") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                    }
                    orderBy = orderBy {
                        sorts += sort(v("x"), null, Sort.Nulls.FIRST)
                    }
                }
            },
            expect("SELECT a FROM T ORDER BY x NULLS LAST") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                    }
                    orderBy = orderBy {
                        sorts += sort(v("x"), null, Sort.Nulls.LAST)
                    }
                }
            },
            expect("SELECT a FROM T ORDER BY x ASC NULLS FIRST") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                    }
                    orderBy = orderBy {
                        sorts += sort(v("x"), Sort.Dir.ASC, Sort.Nulls.FIRST)
                    }
                }
            },
            expect("SELECT a FROM T ORDER BY x ASC NULLS LAST") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                    }
                    orderBy = orderBy {
                        sorts += sort(v("x"), Sort.Dir.ASC, Sort.Nulls.LAST)
                    }
                }
            },
            expect("SELECT a FROM T ORDER BY x DESC NULLS FIRST") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                    }
                    orderBy = orderBy {
                        sorts += sort(v("x"), Sort.Dir.DESC, Sort.Nulls.FIRST)
                    }
                }
            },
            expect("SELECT a FROM T ORDER BY x DESC NULLS LAST") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                    }
                    orderBy = orderBy {
                        sorts += sort(v("x"), Sort.Dir.DESC, Sort.Nulls.LAST)
                    }
                }
            },
            expect("SELECT a FROM T ORDER BY x, y") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                    }
                    orderBy = orderBy {
                        sorts += sort(v("x"), null, null)
                        sorts += sort(v("y"), null, null)
                    }
                }
            },
            expect("SELECT a FROM T ORDER BY x ASC, y DESC") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                    }
                    orderBy = orderBy {
                        sorts += sort(v("x"), Sort.Dir.ASC, null)
                        sorts += sort(v("y"), Sort.Dir.DESC, null)
                    }
                }
            },
            expect("SELECT a FROM T ORDER BY x NULLS FIRST, y NULLS LAST") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                    }
                    orderBy = orderBy {
                        sorts += sort(v("x"), null, Sort.Nulls.FIRST)
                        sorts += sort(v("y"), null, Sort.Nulls.LAST)
                    }
                }
            },
            expect("SELECT a FROM T ORDER BY x ASC NULLS FIRST, y DESC NULLS LAST") {
                exprQuerySet {
                    body = queryBodySFW {
                        select = select("a")
                        from = table("T")
                    }
                    orderBy = orderBy {
                        sorts += sort(v("x"), Sort.Dir.ASC, Sort.Nulls.FIRST)
                        sorts += sort(v("y"), Sort.Dir.DESC, Sort.Nulls.LAST)
                    }
                }
            },
        )

        @JvmStatic
        fun unionClauseCases() = listOf(
            expect("(SELECT a FROM T) UNION (SELECT b FROM S)") {
                exprQuerySet {
                    body = queryBodySetOp {
                        type = setOp {
                            type = SetOp.Type.UNION
                            setq = null
                        }
                        isOuter = false
                        lhs = exprQuerySet {
                            body = queryBodySFW {
                                select = select("a")
                                from = table("T")
                            }
                        }
                        rhs = exprQuerySet {
                            body = queryBodySFW {
                                select = select("b")
                                from = table("S")
                            }
                        }
                    }
                }
            },
            expect("(SELECT a FROM T) UNION ALL (SELECT b FROM S)") {
                exprQuerySet {
                    body = queryBodySetOp {
                        type = setOp(SetOp.Type.UNION, SetQuantifier.ALL)
                        isOuter = false
                        lhs = exprQuerySet {
                            body = queryBodySFW {
                                select = select("a")
                                from = table("T")
                            }
                        }
                        rhs = exprQuerySet {
                            body = queryBodySFW {
                                select = select("b")
                                from = table("S")
                            }
                        }
                    }
                }
            },
            expect("(SELECT a FROM T) UNION DISTINCT (SELECT b FROM S)") {
                exprQuerySet {
                    body = queryBodySetOp {
                        type = setOp(SetOp.Type.UNION, SetQuantifier.DISTINCT)
                        isOuter = false
                        lhs = exprQuerySet {
                            body = queryBodySFW {
                                select = select("a")
                                from = table("T")
                            }
                        }
                        rhs = exprQuerySet {
                            body = queryBodySFW {
                                select = select("b")
                                from = table("S")
                            }
                        }
                    }
                }
            },
            expect("(SELECT a FROM T) UNION (SELECT b FROM S) LIMIT 1") {
                exprQuerySet {
                    body = queryBodySetOp {
                        type = setOp(SetOp.Type.UNION, null)
                        isOuter = false
                        lhs = exprQuerySet {
                            body = queryBodySFW {
                                select = select("a")
                                from = table("T")
                            }
                        }
                        rhs = exprQuerySet {
                            body = queryBodySFW {
                                select = select("b")
                                from = table("S")
                            }
                        }
                    }
                    limit = exprLit(int32Value(1)) // LIMIT associated with SQL set op
                }
            },
            expect("(SELECT a FROM T) UNION (SELECT b FROM S LIMIT 1)") {
                exprQuerySet {
                    body = queryBodySetOp {
                        type = setOp(SetOp.Type.UNION, null)
                        isOuter = false
                        lhs = exprQuerySet {
                            body = queryBodySFW {
                                select = select("a")
                                from = table("T")
                            }
                        }
                        rhs = exprQuerySet {
                            body = queryBodySFW {
                                select = select("b")
                                from = table("S")
                                limit = exprLit(int32Value(1)) // LIMIT associated with rhs SFW query
                            }
                        }
                    }
                }
            },
            expect("(SELECT a FROM T) UNION (SELECT b FROM S) ORDER BY x") {
                exprQuerySet {
                    body = queryBodySetOp {
                        type = setOp(SetOp.Type.UNION, null)
                        isOuter = false
                        lhs = exprQuerySet {
                            body = queryBodySFW {
                                select = select("a")
                                from = table("T")
                            }
                        }
                        rhs = exprQuerySet {
                            body = queryBodySFW {
                                select = select("b")
                                from = table("S")
                            }
                        }
                    }
                    orderBy = orderBy {
                        sorts += sort(v("x"), null, null) // ORDER BY associated with SQL set op
                    }
                }
            },
            expect("(SELECT a FROM T) UNION (SELECT b FROM S ORDER BY x)") {
                exprQuerySet {
                    body = queryBodySetOp {
                        type = setOp(SetOp.Type.UNION, null)
                        isOuter = false
                        lhs = exprQuerySet {
                            body = queryBodySFW {
                                select = select("a")
                                from = table("T")
                            }
                        }
                        rhs = exprQuerySet {
                            body = queryBodySFW {
                                select = select("b")
                                from = table("S")
                                orderBy = orderBy {
                                    sorts += sort(v("x"), null, null) // ORDER BY associated with SFW
                                }
                            }
                        }
                    }
                }
            },
            expect("(SELECT a FROM T) UNION ((SELECT b FROM S) UNION (SELECT c FROM R))") {
                exprQuerySet {
                    body = queryBodySetOp {
                        type = setOp(SetOp.Type.UNION, null)
                        isOuter = false
                        lhs = exprQuerySet {
                            body = queryBodySFW {
                                select = select("a")
                                from = table("T")
                            }
                        }
                        rhs = exprQuerySet {
                            body = queryBodySetOp {
                                type = setOp(SetOp.Type.UNION, null)
                                isOuter = false
                                lhs = exprQuerySet {
                                    body = queryBodySFW {
                                        select = select("b")
                                        from = table("S")
                                    }
                                }
                                rhs = exprQuerySet {
                                    body = queryBodySFW {
                                        select = select("c")
                                        from = table("R")
                                    }
                                }
                            }
                        }
                    }
                }
            },
            expect("((SELECT a FROM T) UNION (SELECT b FROM S)) UNION (SELECT c FROM R)") {
                exprQuerySet {
                    body = queryBodySetOp {
                        type = setOp(SetOp.Type.UNION, null)
                        isOuter = false
                        lhs = exprQuerySet {
                            body = queryBodySetOp {
                                type = setOp(SetOp.Type.UNION, null)
                                isOuter = false
                                lhs = exprQuerySet {
                                    body = queryBodySFW {
                                        select = select("a")
                                        from = table("T")
                                    }
                                }
                                rhs = exprQuerySet {
                                    body = queryBodySFW {
                                        select = select("b")
                                        from = table("S")
                                    }
                                }
                            }
                        }
                        rhs = exprQuerySet {
                            body = queryBodySFW {
                                select = select("c")
                                from = table("R")
                            }
                        }
                    }
                }
            },
        )

        // These are simple clauses
        @JvmStatic
        private fun subqueryCases() = listOf(
            expect("1 = (SELECT a FROM T)") {
                exprOperator {
                    symbol = "="
                    lhs = exprLit(int32Value(1))
                    rhs = exprQuerySet {
                        body = queryBodySFW {
                            select = select("a")
                            from = table("T")
                        }
                    }
                }
            },
            expect("(1, 2) = (SELECT a FROM T)") {
                exprOperator {
                    symbol = "="
                    lhs = exprCollection {
                        type = Expr.Collection.Type.LIST
                        values += exprLit(int32Value(1))
                        values += exprLit(int32Value(2))
                    }
                    rhs = exprQuerySet {
                        body = queryBodySFW {
                            select = select("a")
                            from = table("T")
                        }
                    }
                }
            },
        )

        private fun expect(expected: String, block: AstBuilder.() -> AstNode): Case {
            val i = ast(block)
            return Case.Success(i, expected)
        }

        private fun fail(message: String, block: AstBuilder.() -> AstNode): Case {
            val i = ast(block)
            return Case.Fail(i, message)
        }

        // DSL shorthand

        private fun AstBuilder.v(symbol: String) = this.exprVar {
            identifier = id(symbol)
            scope = Expr.Var.Scope.DEFAULT
        }

        private fun AstBuilder.id(
            symbol: String,
            case: Identifier.CaseSensitivity = Identifier.CaseSensitivity.INSENSITIVE,
        ) = this.identifierSymbol(symbol, case)

        private fun AstBuilder.select(vararg s: String) = selectProject {
            s.forEach {
                items += selectProjectItemExpression(v(it))
            }
        }

        private fun AstBuilder.table(symbol: String) = fromValue {
            expr = v(symbol)
            type = From.Value.Type.SCAN
        }
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
