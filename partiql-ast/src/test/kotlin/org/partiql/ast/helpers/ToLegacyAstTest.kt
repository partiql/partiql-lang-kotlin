@file:OptIn(PartiQLValueExperimental::class)

package org.partiql.ast.helpers

import com.amazon.ion.Decimal
import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionDecimal
import com.amazon.ionelement.api.ionFloat
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionNull
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionSymbol
import com.amazon.ionelement.api.loadSingleElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.ast.Ast
import org.partiql.ast.AstNode
import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.GroupBy
import org.partiql.ast.Identifier
import org.partiql.ast.SetQuantifier
import org.partiql.ast.Sort
import org.partiql.ast.builder.AstBuilder
import org.partiql.ast.builder.AstFactory
import org.partiql.ast.builder.ast
import org.partiql.lang.domains.PartiqlAst
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.blobValue
import org.partiql.value.boolValue
import org.partiql.value.clobValue
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
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertFails

/**
 * Tests for translation of the org.partiql.ast.AstNode trees to org.partiql.lang.domains.PartiqlAst.AstNode trees.
 *
 * The `null` expression value is used extensively because we are testing single node structural translations.
 * We don't want convoluted tests with deep trees. More complex tests are covered in end-to-end translation.
 *
 * Similarly, PartiqlAst.Identifier and Identifier nodes are avoided when their meaning doesn't matter.
 * Scan of a string "table" is semantically different than the identifier with name 'table', but structurally
 * it doesn't matter for testing where an arbitrary expression is used. Keep the tree shallow.
 */
class ToLegacyAstTest {

    @ParameterizedTest
    @MethodSource("literals")
    @Execution(ExecutionMode.CONCURRENT)
    fun testLiterals(case: Case) = case.assert()

    @ParameterizedTest
    @MethodSource("ion")
    @Execution(ExecutionMode.CONCURRENT)
    fun testIon(case: Case) = case.assert()

    @ParameterizedTest
    @MethodSource("identifiers")
    @Execution(ExecutionMode.CONCURRENT)
    fun testVars(case: Case) = case.assert()

    @ParameterizedTest
    @MethodSource("calls")
    @Execution(ExecutionMode.CONCURRENT)
    fun testCalls(case: Case) = case.assert()

    @ParameterizedTest
    @MethodSource("operators")
    @Execution(ExecutionMode.CONCURRENT)
    fun testOperators(case: Case) = case.assert()

    @ParameterizedTest
    @MethodSource("paths")
    @Execution(ExecutionMode.CONCURRENT)
    fun testPaths(case: Case) = case.assert()

    @ParameterizedTest
    @MethodSource("collections")
    @Execution(ExecutionMode.CONCURRENT)
    fun testCollections(case: Case) = case.assert()

    @ParameterizedTest
    @MethodSource("types")
    @Execution(ExecutionMode.CONCURRENT)
    fun testTypes(case: Case) = case.assert()

    @ParameterizedTest
    @MethodSource("specialForms")
    @Execution(ExecutionMode.CONCURRENT)
    fun testSpecialForms(case: Case) = case.assert()

    @ParameterizedTest
    @MethodSource("sfw")
    @Execution(ExecutionMode.CONCURRENT)
    fun testSfw(case: Case) = case.assert()

    companion object {

        private fun expect(expected: String, block: AstBuilder.() -> AstNode): Case {
            val i = ast(AstFactory.DEFAULT, block)
            val e = PartiqlAst.transform(loadSingleElement(expected))
            return Case.Translate(i, e)
        }

        private fun fail(message: String, block: AstBuilder.() -> AstNode): Case {
            val i = ast(AstFactory.DEFAULT, block)
            return Case.Fail(i, message)
        }

        private val NULL = Ast.exprLit(nullValue())

        // Shortcut to construct a "legacy-compatible" simple identifier
        private fun id(name: String) = Ast.identifierSymbol(name, Identifier.CaseSensitivity.INSENSITIVE)

        @JvmStatic
        fun literals() = listOf(
            expect("(lit null)") {
                exprLit(nullValue())
            },
            expect("(missing)") {
                exprLit(missingValue())
            },
            expect("(lit true)") {
                exprLit(boolValue(true))
            },
            expect("(lit 1)") {
                exprLit(int8Value(1))
            },
            expect("(lit 2)") {
                exprLit(int16Value(2))
            },
            expect("(lit 3)") {
                exprLit(int32Value(3))
            },
            expect("(lit 4)") {
                exprLit(int64Value(4))
            },
            expect("(lit 5)") {
                exprLit(intValue(BigInteger.valueOf(5)))
            },
            expect("(lit 1.1e0)") {
                exprLit(float32Value(1.1f))
            },
            expect("(lit 1.2e0)") {
                exprLit(float64Value(1.2))
            },
            expect("(lit 1.3)") {
                exprLit(decimalValue(BigDecimal.valueOf(1.3)))
            },
            expect("""(lit "hello")""") {
                exprLit(stringValue("hello"))
            },
            expect("""(lit 'hello')""") {
                exprLit(symbolValue("hello"))
            },
            expect("""(lit {{ '''Hello'''    '''World''' }})""") {
                exprLit(clobValue("HelloWorld".toByteArray()))
            },
            expect("""(lit {{ VG8gaW5maW5pdHkuLi4gYW5kIGJleW9uZCE= }})""") {
                exprLit(blobValue("To infinity... and beyond!".toByteArray()))
            },
            // TODO detailed tests just for _DateTime_ types
        )

        @JvmStatic
        fun ion() = listOf(
            expect("(lit null)") {
                exprIon(ionNull())
            },
            expect("(lit true)") {
                exprIon(ionBool(true))
            },
            expect("(lit 1)") {
                exprIon(ionInt(1))
            },
            expect("(lit 1.2e0)") {
                exprIon(ionFloat(1.2))
            },
            expect("(lit 1.3)") {
                exprIon(ionDecimal(Decimal.valueOf(1.3)))
            },
            expect("""(lit "hello")""") {
                exprIon(ionString("hello"))
            },
            expect("""(lit 'hello')""") {
                exprIon(ionSymbol("hello"))
            },
            // TODO detailed tests just for _DateTime_ types
        )

        @JvmStatic
        fun identifiers() = listOf(
            expect("(id 'a' (case_sensitive) (unqualified))") {
                exprVar {
                    identifier = identifierSymbol("a", Identifier.CaseSensitivity.SENSITIVE)
                    scope = Expr.Var.Scope.DEFAULT
                }
            },
            expect("(id 'a' (case_insensitive) (unqualified))") {
                exprVar {
                    identifier = identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE)
                    scope = Expr.Var.Scope.DEFAULT
                }
            },
            expect("(id 'a' (case_sensitive) (locals_first))") {
                exprVar {
                    identifier = identifierSymbol("a", Identifier.CaseSensitivity.SENSITIVE)
                    scope = Expr.Var.Scope.LOCAL
                }
            },
            expect("(id 'a' (case_insensitive) (locals_first))") {
                exprVar {
                    identifier = identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE)
                    scope = Expr.Var.Scope.LOCAL
                }
            },
            expect("(identifier 'a' (case_insensitive))") {
                identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE)
            },
            //
            fail("Cannot translate qualified identifiers in variable references") {
                exprVar {
                    identifier = identifierQualified {
                        root = identifierSymbol("a", Identifier.CaseSensitivity.INSENSITIVE)
                        steps += identifierSymbol("b", Identifier.CaseSensitivity.INSENSITIVE)
                    }
                    scope = Expr.Var.Scope.LOCAL
                }
            }
        )

        @JvmStatic
        fun calls() = listOf(
            expect("(call 'a' (lit null))") {
                exprCall {
                    function = id("a")
                    args += NULL
                }
            },
            expect("(call_agg (all) 'a' (lit null))") {
                exprAgg {
                    function = id("a")
                    args += NULL
                }
            },
            expect("(call_agg (all) 'a' (lit null))") {
                exprAgg {
                    setq = SetQuantifier.ALL
                    function = id("a")
                    args += NULL
                }
            },
            expect("(call_agg (distinct) 'a' (lit null))") {
                exprAgg {
                    setq = SetQuantifier.DISTINCT
                    function = id("a")
                    args += NULL
                }
            },
            fail("Cannot translate `call_agg` with more than one argument") {
                exprAgg {
                    function = id("a")
                    args += listOf(NULL, NULL)
                }
            },
        )

        @JvmStatic
        fun operators() = listOf(
            expect("(not (lit null))") {
                exprUnary {
                    op = Expr.Unary.Op.NOT
                    expr = NULL
                }
            },
            expect("(pos (lit null))") {
                exprUnary {
                    op = Expr.Unary.Op.POS
                    expr = NULL
                }
            },
            expect("(neg (lit null))") {
                exprUnary {
                    op = Expr.Unary.Op.NEG
                    expr = NULL
                }
            },
            // we don't really need to test _all_ binary operators
            expect("(plus (lit null) (lit null))") {
                exprBinary {
                    op = Expr.Binary.Op.PLUS
                    lhs = NULL
                    rhs = NULL
                }
            },
        )

        @JvmStatic
        fun paths() = listOf(
            expect("(path (lit null) (path_expr (lit null) (case_sensitive)))") {
                exprPath {
                    root = NULL
                    steps += exprPathStepIndex(NULL)
                }
            },
            expect("(path (lit null) (path_wildcard))") {
                exprPath {
                    root = NULL
                    steps += exprPathStepWildcard()
                }
            },
            expect("(path (lit null) (path_unpivot))") {
                exprPath {
                    root = NULL
                    steps += exprPathStepUnpivot()
                }
            },
        )

        @JvmStatic
        fun collections() = listOf(
            expect("(bag (lit null))") {
                exprCollection(Expr.Collection.Type.BAG) {
                    values += NULL
                }
            },
            expect("(list (lit null))") {
                exprCollection(Expr.Collection.Type.ARRAY) {
                    values += NULL
                }
            },
            expect("(list (lit null))") {
                exprCollection(Expr.Collection.Type.VALUES) {
                    values += NULL
                }
            },
            expect("(list (lit null))") {
                exprCollection(Expr.Collection.Type.LIST) {
                    values += NULL
                }
            },
            expect("(sexp (lit null))") {
                exprCollection(Expr.Collection.Type.SEXP) {
                    values += NULL
                }
            },
            expect("(struct (expr_pair (lit null) (lit null)))") {
                exprStruct {
                    fields += exprStructField {
                        name = NULL
                        value = NULL
                    }
                }
            },
        )

        @JvmStatic
        fun types() = listOf(
            // SQL
            expect("(null_type)") { typeNullType() },
            expect("(boolean_type)") { typeBool() },
            expect("(smallint_type)") { typeSmallint() },
            expect("(integer_type)") { typeInt() },
            expect("(real_type)") { typeReal() },
            expect("(float_type null)") { typeFloat32() },
            expect("(double_precision_type)") { typeFloat64() },
            expect("(decimal_type null null)") { typeDecimal() },
            expect("(decimal_type 2 null)") { typeDecimal(2) },
            expect("(decimal_type 2 1)") { typeDecimal(2, 1) },
            expect("(numeric_type null null)") { typeNumeric() },
            expect("(numeric_type 2 null)") { typeNumeric(2) },
            expect("(numeric_type 2 1)") { typeNumeric(2, 1) },
            expect("(timestamp_type)") { typeTimestamp() },
            expect("(character_type null)") { typeChar() },
            expect("(character_type 1)") { typeChar(1) },
            expect("(character_varying_type null)") { typeVarchar() },
            expect("(character_varying_type 1)") { typeVarchar(1) },
            expect("(blob_type)") { typeBlob() },
            expect("(clob_type)") { typeClob() },
            expect("(date_type)") { typeDate() },
            expect("(time_type null)") { typeTime() },
            expect("(time_type 1)") { typeTime(1) },
            expect("(time_with_time_zone_type null)") { typeTimeWithTz() },
            expect("(time_with_time_zone_type 1)") { typeTimeWithTz(1) },
            // PartiQL
            expect("(missing_type)") { typeMissing() },
            expect("(string_type)") { typeString() },
            expect("(symbol_type)") { typeSymbol() },
            expect("(struct_type)") { typeStruct() },
            expect("(tuple_type)") { typeTuple() },
            expect("(list_type)") { typeList() },
            expect("(sexp_type)") { typeSexp() },
            expect("(bag_type)") { typeBag() },
            expect("(any_type)") { typeAny() },
            // Other (??)
            expect("(integer4_type)") { typeInt4() },
            expect("(integer8_type)") { typeInt8() },
            expect("(custom_type dog)") { typeCustom("dog") }
            // LEGACY AST does not have TIMESTAMP or INTERVAL
            // LEGACY AST does not have parameterized blob/clob
        )

        @JvmStatic
        fun specialForms() = listOf(
            expect("(like (lit 'a') (lit 'b') null)") {
                exprLike {
                    value = exprLit(symbolValue("a"))
                    pattern = exprLit(symbolValue("b"))
                }
            },
            expect("(like (lit 'a') (lit 'b') (lit 'c'))") {
                exprLike {
                    value = exprLit(symbolValue("a"))
                    pattern = exprLit(symbolValue("b"))
                    escape = exprLit(symbolValue("c"))
                }
            },
            expect("(not (like (lit 'a') (lit 'b') (lit 'c')))") {
                exprLike {
                    value = exprLit(symbolValue("a"))
                    pattern = exprLit(symbolValue("b"))
                    escape = exprLit(symbolValue("c"))
                    not = true
                }
            },
            expect("(between (lit 'a') (lit 'b') (lit 'c'))") {
                exprBetween {
                    value = exprLit(symbolValue("a"))
                    from = exprLit(symbolValue("b"))
                    to = exprLit(symbolValue("c"))
                }
            },
            expect("(not (between (lit 'a') (lit 'b') (lit 'c')))") {
                exprBetween {
                    value = exprLit(symbolValue("a"))
                    from = exprLit(symbolValue("b"))
                    to = exprLit(symbolValue("c"))
                    not = true
                }
            },
            expect("(in_collection (lit 'a') (lit 'b'))") {
                exprInCollection {
                    lhs = exprLit(symbolValue("a"))
                    rhs = exprLit(symbolValue("b"))
                }
            },
            expect("(not (in_collection (lit 'a') (lit 'b')))") {
                exprInCollection {
                    lhs = exprLit(symbolValue("a"))
                    rhs = exprLit(symbolValue("b"))
                    not = true
                }
            },
            expect("(is_type (lit 'a') (any_type))") {
                exprIsType {
                    value = exprLit(symbolValue(("a")))
                    type = typeAny()
                }
            },
            expect("(not (is_type (lit 'a') (any_type)))") {
                exprIsType {
                    value = exprLit(symbolValue(("a")))
                    type = typeAny()
                    not = true
                }
            },
            // TODO case
            // TODO coalesce
            // TODO nullif
            // TODO substring
            // TODO position
            expect("""(call 'trim' (lit "xyz"))""") {
                exprTrim {
                    value = exprLit(stringValue("xyz"))
                }
            },
            expect("""(call 'trim' (lit "xyz"))""") {
                exprTrim {
                    value = exprLit(stringValue("xyz"))
                }
            },
            expect("""(call 'trim' (lit "xyz"))""") {
                exprTrim {
                    value = exprLit(stringValue("xyz"))
                }
            },
            // TODO overlay
            // TODO extract
            // TODO cast
            // TODO can_cast
            // TODO can_lossless_cast
            // TODO date_add
            // TODO date_diff
        )

        @JvmStatic
        fun sfw() = listOf(
            // PROJECT Variants
            expect("(project_star)") {
                selectStar()
            },
            expect(
                """
                (project_list
                    (project_all (id 'a' (case_sensitive) (unqualified)))
                    (project_expr (lit 1) 'x')
                )
             """
            ) {
                selectProject {
                    items += selectProjectItemAll {
                        expr =
                            exprVar(identifierSymbol("a", Identifier.CaseSensitivity.SENSITIVE), Expr.Var.Scope.DEFAULT)
                    }
                    items += selectProjectItemExpression {
                        expr = exprLit(int32Value(1))
                        asAlias = "x"
                    }
                }
            },
            expect("(project_pivot (lit 1) (lit 2))") {
                selectPivot {
                    value = exprLit(int32Value(1))
                    key = exprLit(int32Value(2))
                }
            },
            expect("(project_value (lit null))") {
                selectValue {
                    constructor = NULL
                }
            },
            // FROM_SOURCE Variants
            expect("(scan (lit null) null null null)") {
                fromValue {
                    expr = NULL
                    type = From.Value.Type.SCAN
                }
            },
            expect("(scan (lit null) 'a' 'b' 'c')") {
                fromValue {
                    expr = NULL
                    type = From.Value.Type.SCAN
                    asAlias = "a"
                    atAlias = "b"
                    byAlias = "c"
                }
            },
            expect("(unpivot (lit null) null null null)") {
                fromValue {
                    expr = NULL
                    type = From.Value.Type.UNPIVOT
                }
            },
            expect("(unpivot (lit null) 'a' 'b' 'c')") {
                fromValue {
                    expr = NULL
                    type = From.Value.Type.UNPIVOT
                    asAlias = "a"
                    atAlias = "b"
                    byAlias = "c"
                }
            },
            expect(
                """
                (join (inner)
                    (scan (lit "lhs") null null null)
                    (scan (lit "rhs") null null null)
                    null
                )
            """
            ) {
                fromJoin {
                    type = From.Join.Type.INNER
                    lhs = fromValue {
                        expr = exprLit(stringValue("lhs"))
                        type = From.Value.Type.SCAN
                    }
                    rhs = fromValue {
                        expr = exprLit(stringValue("rhs"))
                        type = From.Value.Type.SCAN
                    }
                }
            },
            expect(
                """
                (join (inner)
                    (scan (lit "lhs") null null null)
                    (scan (lit "rhs") null null null)
                    (lit true)
                )
            """
            ) {
                fromJoin {
                    // DEFAULT
                    // type = From.Join.Type.INNER
                    lhs = fromValue {
                        expr = exprLit(stringValue("lhs"))
                        type = From.Value.Type.SCAN
                    }
                    rhs = fromValue {
                        expr = exprLit(stringValue("rhs"))
                        type = From.Value.Type.SCAN
                    }
                    condition = exprLit(boolValue(true))
                }
            },
            expect("(let (let_binding (lit null) 'x'))") {
                let {
                    bindings += letBinding {
                        expr = NULL
                        asAlias = "x"
                    }
                }
            },
            expect(
                """
               (group_by (group_full)
                    (group_key_list
                        (group_key (lit "a") null)
                        (group_key (lit "b") 'x')
                    )
                    null
               )
            """
            ) {
                groupBy {
                    strategy = GroupBy.Strategy.FULL
                    keys += groupByKey(exprLit(stringValue("a")), null)
                    keys += groupByKey(exprLit(stringValue("b")), "x")
                }
            },
            expect(
                """
               (group_by (group_partial)
                    (group_key_list
                        (group_key (lit "a") null)
                        (group_key (lit "b") 'x')
                    )
                    'as'
               )
            """
            ) {
                groupBy {
                    strategy = GroupBy.Strategy.PARTIAL
                    keys += groupByKey(exprLit(stringValue("a")), null)
                    keys += groupByKey(exprLit(stringValue("b")), "x")
                    asAlias = "as"
                }
            },
            expect(
                """
               (order_by
                    (sort_spec (lit "a") null null)
                    (sort_spec (lit "b") (asc) (nulls_first))
                    (sort_spec (lit "c") (asc) (nulls_last))
                    (sort_spec (lit "d") (desc) (nulls_first))
                    (sort_spec (lit "e") (desc) (nulls_last))
               )
            """
            ) {
                orderBy {
                    sorts += sort(exprLit(stringValue("a")))
                    sorts += sort(exprLit(stringValue("b")), Sort.Dir.ASC, Sort.Nulls.FIRST)
                    sorts += sort(exprLit(stringValue("c")), Sort.Dir.ASC, Sort.Nulls.LAST)
                    sorts += sort(exprLit(stringValue("d")), Sort.Dir.DESC, Sort.Nulls.FIRST)
                    sorts += sort(exprLit(stringValue("e")), Sort.Dir.DESC, Sort.Nulls.LAST)
                }
            },
        )
    }

    sealed class Case {

        abstract fun assert()

        class Translate(
            private val input: AstNode,
            private val expected: PartiqlAst.PartiqlAstNode,
            private val metas: Map<String, MetaContainer> = emptyMap(),
        ) : Case() {

            override fun assert() {
                val actual = input.toLegacyAst(metas)
                val aIon = actual.toIonElement()
                val eIon = expected.toIonElement()
                assertEquals(eIon, aIon)
            }
        }

        class Fail(
            private val input: AstNode,
            private val message: String,
            private val metas: Map<String, MetaContainer> = emptyMap(),
        ) : Case() {

            override fun assert() {
                assertFails(message) {
                    input.toLegacyAst(metas)
                }
            }
        }
    }
}
