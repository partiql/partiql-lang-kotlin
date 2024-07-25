package org.partiql.planner.internal.astPasses

import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.ast.Binder
import org.partiql.ast.Expr
import org.partiql.ast.From
import org.partiql.ast.Identifier
import org.partiql.ast.Statement
import org.partiql.ast.binder
import org.partiql.ast.builder.ast
import org.partiql.ast.identifierSymbol
import org.partiql.planner.internal.CaseNormalization
import java.util.BitSet
import java.util.stream.Collectors.toList
import java.util.stream.IntStream
import java.util.stream.Stream
import kotlin.test.assertEquals

internal typealias Setting = Pair<Boolean, NormalizeIdentifierTest.Normalization?>

internal class NormalizeIdentifierTest {

    data class TestCase(
        val original: Statement,
        val upperCase: Statement,
        val lowerCase: Statement,
        val exactCase: Statement,
        val current: Statement
    )

    enum class Normalization {
        UPPER,
        LOWER,
        EXACT,
        NO_OP // original ast
    }

    @ArgumentsSource(Provider::class)
    @ParameterizedTest
    fun tests(tc: TestCase) = runTestCase(tc)

    val passUpper = NormalizeIdentifier(CaseNormalization.UPPERCASE)

    val passLower = NormalizeIdentifier(CaseNormalization.LOWERCASE)

    val passExact = NormalizeIdentifier(CaseNormalization.EXACTCASE)

    val passCurrent = NormalizeIdentifier(null)

    fun runTestCase(tc: TestCase) {
        val upper = passUpper.apply(tc.original)
        val lower = passLower.apply(tc.original)
        val exact = passExact.apply(tc.original)
        val current = passCurrent.apply(tc.original)

        assertEquals(expected = tc.upperCase, actual = upper, "Normalized to upper case test failed")
        assertEquals(expected = tc.lowerCase, actual = lower, "Normalized to lower case test failed")
        assertEquals(expected = tc.exactCase, actual = exact, "Normalized to exact case test failed")
        assertEquals(expected = tc.current, actual = current, "current behavior failed")
    }

    class Provider() : ArgumentsProvider {

        fun permuteBinary(length: Int) =
            IntStream.range(0, Math.pow(2.0, length.toDouble()).toInt())
                .mapToObj { i -> longArrayOf(i.toLong()) }
                .map { i -> BitSet.valueOf(i) }
                .map { i ->
                    val booleanList = mutableListOf<Boolean>()
                    repeat(length) {
                        booleanList.add(false)
                    }
                    i.stream().forEach { i -> booleanList[i] = true }
                    booleanList.toList()
                }
                .collect(toList())

        // SELECT bAr.fOo as fOo FROM tBl as bAr
        val varRefBAR: (Setting) -> Identifier.Symbol = { setting ->
            val isCaseSensitive = setting.first
            val normalization = setting.second
            val original = "bAr"
            when (normalization) {
                Normalization.UPPER -> identifierSymbol(
                    symbol = if (isCaseSensitive) original else original.uppercase(),
                    caseSensitivity = Identifier.CaseSensitivity.SENSITIVE
                )
                Normalization.LOWER -> identifierSymbol(
                    symbol = if (isCaseSensitive) original else original.lowercase(),
                    caseSensitivity = Identifier.CaseSensitivity.SENSITIVE
                )
                Normalization.EXACT -> identifierSymbol(
                    symbol = original,
                    caseSensitivity = Identifier.CaseSensitivity.SENSITIVE
                )
                Normalization.NO_OP -> identifierSymbol(
                    symbol = original,
                    caseSensitivity = if (isCaseSensitive) Identifier.CaseSensitivity.SENSITIVE else Identifier.CaseSensitivity.INSENSITIVE
                )
                null -> identifierSymbol(
                    symbol = original,
                    caseSensitivity = if (isCaseSensitive) Identifier.CaseSensitivity.SENSITIVE else Identifier.CaseSensitivity.INSENSITIVE
                )
            }
        }

        val varRefFOO: (Setting) -> Identifier.Symbol = { setting ->
            val isCaseSensitive = setting.first
            val normalization = setting.second
            val original = "fOo"
            when (normalization) {
                Normalization.UPPER -> identifierSymbol(
                    symbol = if (isCaseSensitive) original else original.uppercase(),
                    caseSensitivity = Identifier.CaseSensitivity.SENSITIVE
                )
                Normalization.LOWER -> identifierSymbol(
                    symbol = if (isCaseSensitive) original else original.lowercase(),
                    caseSensitivity = Identifier.CaseSensitivity.SENSITIVE
                )
                Normalization.EXACT -> identifierSymbol(
                    symbol = original,
                    caseSensitivity = Identifier.CaseSensitivity.SENSITIVE
                )
                Normalization.NO_OP -> identifierSymbol(
                    symbol = original,
                    caseSensitivity = if (isCaseSensitive) Identifier.CaseSensitivity.SENSITIVE else Identifier.CaseSensitivity.INSENSITIVE
                )
                null -> identifierSymbol(
                    symbol = original,
                    caseSensitivity = if (isCaseSensitive) Identifier.CaseSensitivity.SENSITIVE else Identifier.CaseSensitivity.INSENSITIVE
                )
            }
        }

        val varDeclFOO: (Setting) -> Binder = { setting ->
            val isCaseSensitive = setting.first
            val normalization = setting.second
            val original = "fOo"
            when (normalization) {
                Normalization.UPPER -> binder(
                    symbol = if (isCaseSensitive) original else original.uppercase(),
                    isRegular = false
                )
                Normalization.LOWER -> binder(
                    symbol = if (isCaseSensitive) original else original.lowercase(),
                    isRegular = false
                )
                Normalization.EXACT -> binder(
                    symbol = original,
                    isRegular = false
                )
                Normalization.NO_OP -> binder(
                    symbol = original,
                    isRegular = if (isCaseSensitive) false else true
                )
                null -> binder(
                    symbol = original,
                    false
                )
            }
        }

        val varRefTBL: (Setting) -> Identifier.Symbol = { setting ->
            val isCaseSensitive = setting.first
            val normalization = setting.second
            val original = "tBl"
            when (normalization) {
                Normalization.UPPER -> identifierSymbol(
                    symbol = if (isCaseSensitive) original else original.uppercase(),
                    caseSensitivity = Identifier.CaseSensitivity.SENSITIVE
                )
                Normalization.LOWER -> identifierSymbol(
                    symbol = if (isCaseSensitive) original else original.lowercase(),
                    caseSensitivity = Identifier.CaseSensitivity.SENSITIVE
                )
                Normalization.EXACT -> identifierSymbol(
                    symbol = original,
                    caseSensitivity = Identifier.CaseSensitivity.SENSITIVE
                )
                Normalization.NO_OP -> identifierSymbol(
                    symbol = original,
                    caseSensitivity = if (isCaseSensitive) Identifier.CaseSensitivity.SENSITIVE else Identifier.CaseSensitivity.INSENSITIVE
                )
                null -> identifierSymbol(
                    symbol = original,
                    caseSensitivity = if (isCaseSensitive) Identifier.CaseSensitivity.SENSITIVE else Identifier.CaseSensitivity.INSENSITIVE
                )
            }
        }

        val varDeclBAR: (Setting) -> Binder = { setting ->
            val isCaseSensitive = setting.first
            val normalization = setting.second
            val original = "bAr"
            when (normalization) {
                Normalization.UPPER -> binder(
                    symbol = if (isCaseSensitive) original else original.uppercase(),
                    isRegular = false
                )
                Normalization.LOWER -> binder(
                    symbol = if (isCaseSensitive) original else original.lowercase(),
                    isRegular = false
                )
                Normalization.EXACT -> binder(
                    symbol = original,
                    isRegular = false
                )
                Normalization.NO_OP -> binder(
                    symbol = original,
                    isRegular = if (isCaseSensitive) false else true
                )
                null -> binder(
                    symbol = original,
                    false
                )
            }
        }

        val ast: (Setting, Setting, Setting, Setting, Setting) -> Statement = { var_ref_bar, var_ref_foo, var_decl_foo, var_ref_tbl, var_decl_bar ->
            ast {
                statementQuery {
                    expr = exprSFW {
                        select = selectProject {
                            mutableListOf(
                                selectProjectItemExpression {
                                    expr = exprPath {
                                        root = exprVar {
                                            identifier = varRefBAR(var_ref_bar)
                                            scope = Expr.Var.Scope.DEFAULT
                                        }
                                        steps = mutableListOf(
                                            exprPathStepSymbol {
                                                symbol = varRefFOO(var_ref_foo)
                                            }
                                        )
                                    }
                                    asAlias = varDeclFOO(var_decl_foo)
                                }
                            )
                        }

                        from = fromValue {
                            expr = exprVar {
                                identifier = varRefTBL(var_ref_tbl)
                                scope = Expr.Var.Scope.DEFAULT
                            }
                            type = From.Value.Type.SCAN
                            asAlias = varDeclBAR(var_decl_bar)
                        }
                    }
                }
            }
        }

        private val tests = permuteBinary(5).map { booleanArray ->
            TestCase(
                original = ast(booleanArray[0] to Normalization.NO_OP, booleanArray[1] to Normalization.NO_OP, booleanArray[2] to Normalization.NO_OP, booleanArray[3] to Normalization.NO_OP, booleanArray[4] to Normalization.NO_OP),
                upperCase = ast(booleanArray[0] to Normalization.UPPER, booleanArray[1] to Normalization.UPPER, booleanArray[2] to Normalization.UPPER, booleanArray[3] to Normalization.UPPER, booleanArray[4] to Normalization.UPPER),
                lowerCase = ast(booleanArray[0] to Normalization.LOWER, booleanArray[1] to Normalization.LOWER, booleanArray[2] to Normalization.LOWER, booleanArray[3] to Normalization.LOWER, booleanArray[4] to Normalization.LOWER),
                exactCase = ast(booleanArray[0] to Normalization.EXACT, booleanArray[1] to Normalization.EXACT, booleanArray[2] to Normalization.EXACT, booleanArray[3] to Normalization.EXACT, booleanArray[4] to Normalization.EXACT),
                current = ast(booleanArray[0] to null, booleanArray[1] to null, booleanArray[2] to null, booleanArray[3] to null, booleanArray[4] to null),
            )
        }

        override fun provideArguments(context: ExtensionContext): Stream<out org.junit.jupiter.params.provider.Arguments> =
            tests.map { org.junit.jupiter.params.provider.Arguments.of(it) }.stream()
    }
}
