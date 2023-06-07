package org.partiql.ast.helpers

import com.amazon.ion.Decimal
import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionDecimal
import com.amazon.ionelement.api.ionInt
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
import kotlin.test.Ignore
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
    fun testIonLiterals(case: Case) = case.assert()

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

    @Ignore
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

        // Shortcut to construct a "legacy-compatible" simple identifier
        private fun id(name: String) = Ast.identifierSymbol(name, Identifier.CaseSensitivity.INSENSITIVE)

        @JvmStatic
        fun literals() = listOf(
            expect("(lit null)") {
                exprNullValue()
            },
            expect("(missing)") {
                exprMissingValue()
            },
            expect("(lit true)") {
                exprLiteral(ionBool(true))
            },
            expect("""(lit "hello")""") {
                exprLiteral(ionString("hello"))
            },
            expect("(lit 1)") {
                exprLiteral(ionInt(1L))
            },
            expect("(lit 1.2)") {
                exprLiteral(ionDecimal(Decimal.valueOf(1.2)))
            },
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
                    args += exprNullValue()
                }
            },
            expect("(call_agg (all) 'a' (lit null))") {
                exprAgg {
                    function = id("a")
                    args += exprNullValue()
                }
            },
            expect("(call_agg (all) 'a' (lit null))") {
                exprAgg {
                    setq = SetQuantifier.ALL
                    function = id("a")
                    args += exprNullValue()
                }
            },
            expect("(call_agg (distinct) 'a' (lit null))") {
                exprAgg {
                    setq = SetQuantifier.DISTINCT
                    function = id("a")
                    args += exprNullValue()
                }
            },
            fail("Cannot translate `call_agg` with more than one argument") {
                exprAgg {
                    function = id("a")
                    args += listOf(exprNullValue(), exprNullValue())
                }
            },
        )

        @JvmStatic
        fun operators() = listOf(
            expect("(not (lit null))") {
                exprUnary {
                    op = Expr.Unary.Op.NOT
                    expr = exprNullValue()
                }
            },
            expect("(pos (lit null))") {
                exprUnary {
                    op = Expr.Unary.Op.POS
                    expr = exprNullValue()
                }
            },
            expect("(neg (lit null))") {
                exprUnary {
                    op = Expr.Unary.Op.NEG
                    expr = exprNullValue()
                }
            },
            // we don't really need to test _all_ binary operators
            expect("(plus (lit null) (lit null))") {
                exprBinary {
                    op = Expr.Binary.Op.PLUS
                    lhs = exprNullValue()
                    rhs = exprNullValue()
                }
            },
        )

        @JvmStatic
        fun paths() = listOf(
            expect("(path (lit null) (path_expr (lit null) (case_sensitive)))") {
                exprPath {
                    root = exprNullValue()
                    steps += exprPathStepIndex(exprNullValue())
                }
            },
            expect("(path (lit null) (path_wildcard))") {
                exprPath {
                    root = exprNullValue()
                    steps += exprPathStepWildcard()
                }
            },
            expect("(path (lit null) (path_unpivot))") {
                exprPath {
                    root = exprNullValue()
                    steps += exprPathStepUnpivot()
                }
            },
        )

        @JvmStatic
        fun collections() = listOf(
            expect("(bag (lit null))") {
                exprCollection(Expr.Collection.Type.BAG) {
                    values += exprNullValue()
                }
            },
            expect("(list (lit null))") {
                exprCollection(Expr.Collection.Type.ARRAY) {
                    values += exprNullValue()
                }
            },
            expect("(list (lit null))") {
                exprCollection(Expr.Collection.Type.VALUES) {
                    values += exprNullValue()
                }
            },
            expect("(list (lit null))") {
                exprCollection(Expr.Collection.Type.LIST) {
                    values += exprNullValue()
                }
            },
            expect("(sexp (lit null))") {
                exprCollection(Expr.Collection.Type.SEXP) {
                    values += exprNullValue()
                }
            },
            expect("(struct (expr_pair (lit null) (lit null)))") {
                exprStruct {
                    fields += exprStructField {
                        name = exprNullValue()
                        value = exprNullValue()
                    }
                }
            },
        )

        @JvmStatic
        fun types() = listOf(
            expect("(null_type)") { TODO() },
            expect("(boolean_type)") { TODO() },
            expect("(smallint_type)") { TODO() },
            expect("(integer4_type)") { TODO() },
            expect("(integer8_type)") { TODO() },
            expect("(integer_type)") { TODO() },
            expect("(float_type precision::(? int))") { TODO() },
            expect("(real_type)") { TODO() },
            expect("(double_precision_type)") { TODO() },
            expect("(decimal_type precision::(? int) scale::(? int))") { TODO() },
            expect("(numeric_type precision::(? int) scale::(? int))") { TODO() },
            expect("(timestamp_type)") { TODO() },
            expect("(character_type length::(? int))") { TODO() },
            expect("(character_varying_type length::(? int))") { TODO() },
            expect("(missing_type)") { TODO() },
            expect("(string_type)") { TODO() },
            expect("(symbol_type)") { TODO() },
            expect("(blob_type)") { TODO() },
            expect("(clob_type)") { TODO() },
            expect("(date_type)") { TODO() },
            expect("(time_type precision::(? int))") { TODO() },
            expect("(time_with_time_zone_type precision::(? int))") { TODO() },
            expect("(struct_type)") { TODO() },
            expect("(tuple_type)") { TODO() },
            expect("(list_type)") { TODO() },
            expect("(sexp_type)") { TODO() },
            expect("(bag_type)") { TODO() },
            expect("(any_type)") { TODO() },
            expect("(custom_type name::symbol)") { TODO() },
        )

        @JvmStatic
        fun specialForms() = listOf(
            expect("(like (lit 'a') (lit 'b') null)") {
                exprLike {
                    value = exprLiteral(ionSymbol("a"))
                    pattern = exprLiteral(ionSymbol("b"))
                }
            },
            expect("(like (lit 'a') (lit 'b') (lit 'c'))") {
                exprLike {
                    value = exprLiteral(ionSymbol("a"))
                    pattern = exprLiteral(ionSymbol("b"))
                    escape = exprLiteral(ionSymbol("c"))
                }
            },
            expect("(not (like (lit 'a') (lit 'b') (lit 'c')))") {
                exprLike {
                    value = exprLiteral(ionSymbol("a"))
                    pattern = exprLiteral(ionSymbol("b"))
                    escape = exprLiteral(ionSymbol("c"))
                    not = true
                }
            },
            expect("(between (lit 'a') (lit 'b') (lit 'c'))") {
                exprBetween {
                    value = exprLiteral(ionSymbol("a"))
                    from = exprLiteral(ionSymbol("b"))
                    to = exprLiteral(ionSymbol("c"))
                }
            },
            expect("(not (between (lit 'a') (lit 'b') (lit 'c')))") {
                exprBetween {
                    value = exprLiteral(ionSymbol("a"))
                    from = exprLiteral(ionSymbol("b"))
                    to = exprLiteral(ionSymbol("c"))
                    not = true
                }
            },
            expect("(in_collection (lit 'a') (lit 'b'))") {
                exprInCollection {
                    lhs = exprLiteral(ionSymbol("a"))
                    rhs = exprLiteral(ionSymbol("b"))
                }
            },
            expect("(not (in_collection (lit 'a') (lit 'b')))") {
                exprInCollection {
                    lhs = exprLiteral(ionSymbol("a"))
                    rhs = exprLiteral(ionSymbol("b"))
                    not = true
                }
            },
            expect("(is_type (lit 'a') (any_type))") {
                exprIsType {
                    value = exprLiteral(ionSymbol(("a")))
                    type = type("any")
                }
            },
            expect("(not (is_type (lit 'a') (any_type)))") {
                exprIsType {
                    value = exprLiteral(ionSymbol(("a")))
                    type = type("any")
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
                    value = exprLiteral(ionString("xyz"))
                }
            },
            expect("""(call 'trim' (lit "xyz"))""") {
                exprTrim {
                    value = exprLiteral(ionString("xyz"))
                }
            },
            expect("""(call 'trim' (lit "xyz"))""") {
                exprTrim {
                    value = exprLiteral(ionString("xyz"))
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
                        expr = exprVar(identifierSymbol("a", Identifier.CaseSensitivity.SENSITIVE), Expr.Var.Scope.DEFAULT)
                    }
                    items += selectProjectItemExpression {
                        expr = exprLiteral(ionInt(1))
                        asAlias = "x"
                    }
                }
            },
            expect("(project_pivot (lit 1) (lit 2))") {
                selectPivot {
                    value = exprLiteral(ionInt(1))
                    key = exprLiteral(ionInt(2))
                }
            },
            expect("(project_value (lit null))") {
                selectValue {
                    constructor = exprNullValue()
                }
            },
            // FROM_SOURCE Variants
            expect("(scan (lit null) null null null)") {
                fromValue {
                    expr = exprNullValue()
                    type = From.Value.Type.SCAN
                }
            },
            expect("(scan (lit null) 'a' 'b' 'c')") {
                fromValue {
                    expr = exprNullValue()
                    type = From.Value.Type.SCAN
                    asAlias = "a"
                    atAlias = "b"
                    byAlias = "c"
                }
            },
            expect("(unpivot (lit null) null null null)") {
                fromValue {
                    expr = exprNullValue()
                    type = From.Value.Type.UNPIVOT
                }
            },
            expect("(unpivot (lit null) 'a' 'b' 'c')") {
                fromValue {
                    expr = exprNullValue()
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
                        expr = exprLiteral(ionString("lhs"))
                        type = From.Value.Type.SCAN
                    }
                    rhs = fromValue {
                        expr = exprLiteral(ionString("rhs"))
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
                        expr = exprLiteral(ionString("lhs"))
                        type = From.Value.Type.SCAN
                    }
                    rhs = fromValue {
                        expr = exprLiteral(ionString("rhs"))
                        type = From.Value.Type.SCAN
                    }
                    condition = exprLiteral(ionBool(true))
                }
            },
            expect("(let (let_binding (lit null) 'x'))") {
                let {
                    bindings += letBinding {
                        expr = exprNullValue()
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
                    keys += groupByKey(exprLiteral(ionString("a")), null)
                    keys += groupByKey(exprLiteral(ionString("b")), "x")
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
                    keys += groupByKey(exprLiteral(ionString("a")), null)
                    keys += groupByKey(exprLiteral(ionString("b")), "x")
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
                    sorts += sort(exprLiteral(ionString("a")))
                    sorts += sort(exprLiteral(ionString("b")), Sort.Dir.ASC, Sort.Nulls.FIRST)
                    sorts += sort(exprLiteral(ionString("c")), Sort.Dir.ASC, Sort.Nulls.LAST)
                    sorts += sort(exprLiteral(ionString("d")), Sort.Dir.DESC, Sort.Nulls.FIRST)
                    sorts += sort(exprLiteral(ionString("e")), Sort.Dir.DESC, Sort.Nulls.LAST)
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
