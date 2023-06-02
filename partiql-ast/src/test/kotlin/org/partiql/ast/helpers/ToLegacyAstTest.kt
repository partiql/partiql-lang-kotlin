package org.partiql.ast.helpers

import com.amazon.ion.Decimal
import com.amazon.ionelement.api.MetaContainer
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionDecimal
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.loadSingleElement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.ast.AstNode
import org.partiql.ast.builder.AstBuilder
import org.partiql.ast.builder.AstFactory
import org.partiql.ast.builder.ast
import org.partiql.lang.domains.PartiqlAst

class ToLegacyAstTest {

    class Case(
        private val input: AstNode,
        private val expected: PartiqlAst.PartiqlAstNode,
        private val metas: Map<String, MetaContainer> = emptyMap(),
    ) {

        fun assert() {
            val actual = input.toLegacyAst(metas)
            val aIon = actual.toIonElement()
            val eIon = expected.toIonElement()
            assertEquals(eIon, aIon)
        }
    }

    /**
     * Sanity check, as literals are wrapped ion values
     */
    @ParameterizedTest
    @MethodSource("ionLiterals")
    fun testIonLiterals(case: Case) = case.assert()

    companion object {

        private fun expect(expected: String, block: AstBuilder.() -> AstNode): Case {
            val i = ast(AstFactory.DEFAULT, block)
            val e = PartiqlAst.transform(loadSingleElement(expected))
            return Case(i, e)
        }

        @JvmStatic
        fun ionLiterals() = listOf(
            expect("(lit null)") {
                exprNullValue()
            },
            expect("(lit \$missing::null)") {
                exprMissingValue()
            },
            expect("(lit true)") {
                exprLiteral(ionBool(true))
            },
            expect("(lit \"hello\")") {
                exprLiteral(ionString("hello"))
            },
            expect("(lit 1)") {
                exprLiteral(ionInt(1L))
            },
            expect("(lit 1.2)") {
                exprLiteral(ionDecimal(Decimal.valueOf(1.2)))
            },
        )
    }
}
