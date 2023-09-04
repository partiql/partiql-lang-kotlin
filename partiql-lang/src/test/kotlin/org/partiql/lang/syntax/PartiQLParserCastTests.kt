package org.partiql.lang.syntax

import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionFloat
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.CUSTOM_TEST_TYPES
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.util.ArgumentsProviderBase

class PartiQLParserCastTests : PartiQLParserTestBase() {

    override val targets: Array<ParserTarget> = arrayOf(ParserTarget.DEFAULT, ParserTarget.EXPERIMENTAL)

    @ParameterizedTest
    @ArgumentsSource(ConfiguredCastArguments::class)
    fun configuredCast(configuredCastCase: ConfiguredCastParseTest) = configuredCastCase.assertCase()

    class ConfiguredCastArguments : ArgumentsProviderBase() {
        override fun getParameters() = cases.flatMap {
            listOf(
                it.toCastTest(),
                it.toCanCastTest(),
                it.toCanLosslessCastTest()
            )
        }
    }

    companion object {

        private val cases = listOf(
            Case(
                source = "CAST(true as es_boolean)",
                ast = PartiqlAst.build { cast(lit(ionBool(true)), customType(defnid("es_boolean", regular()))) }
            ),
            Case(
                source = "CAST(1 as es_integer)",
                ast = PartiqlAst.build { cast(lit(ionInt(1)), customType(defnid("es_integer", regular()))) }
            ),
            Case(
                source = "CAST(`1.2e0` as ES_FLOAT)",
                ast = PartiqlAst.build { cast(lit(ionFloat(1.2)), customType(defnid("ES_FLOAT", regular()))) }
            ),
            Case(
                source = "CAST('xyz' as ES_TEXT)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType(defnid("ES_TEXT", regular()))) }
            ),
            Case(
                source = "CAST('xyz' as RS_VARCHAR_MAX)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType(defnid("RS_VARCHAR_MAX", regular()))) }
            ),
            Case(
                source = "CAST('xyz' as RS_REAL)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType(defnid("RS_REAL", regular()))) }
            ),
            Case(
                source = "CAST('xyz' as RS_FLOAT4)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType(defnid("rs_real", regular()))) }
            ),
            Case(
                source = "CAST('xyz' as RS_DOUBLE_PRECISION)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType(defnid("RS_DOUBLE_PRECISION", regular()))) }
            ),
            Case(
                source = "CAST('xyz' as RS_FLOAT)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType(defnid("rs_double_precision", regular()))) }
            ),
            Case(
                source = "CAST('xyz' as rs_float8)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType(defnid("rs_double_precision", regular()))) }
            ),
            Case(
                source = "CAST('xyz' as SPARK_FLOAT)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType(defnid("SPARK_FLOAT", regular()))) }
            ),
            Case(
                source = "CAST('xyz' as int4)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), integer4Type()) }
            ),
            Case(
                source = "CAST('xyz' as smallint)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), smallintType()) }
            ),
            Case(
                source = "CAST('xyz' as integer2)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), smallintType()) }
            ),
            Case(
                source = "CAST('xyz' as int2)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), smallintType()) }
            ),
            Case(
                source = "CAST('xyz' as integer4)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), integer4Type()) }
            ),
            Case(
                source = "CAST('xyz' as int8)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), integer8Type()) }
            ),
            Case(
                source = "CAST('xyz' as integer8)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), integer8Type()) }
            ),
            Case(
                source = "CAST('xyz' as bigint)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), integer8Type()) }
            ),
            Case(
                source = "CAST('xyz' as SPARK_SHORT)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType(defnid("SPARK_SHORT", regular()))) }
            ),
            Case(
                source = "CAST('xyz' as SPARK_INTEGER)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType(defnid("SPARK_INTEGER", regular()))) }
            ),
            Case(
                source = "CAST('xyz' as SPARK_LONG)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType(defnid("SPARK_LONG", regular()))) }
            ),
            Case(
                source = "CAST('xyz' as SPARK_DOUBLE)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType(defnid("SPARK_DOUBLE", regular()))) }
            ),
            Case(
                source = "CAST('xyz' as SPARK_BOOLEAN)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType(defnid("SPARK_BOOLEAN", regular()))) }
            ),
            Case(
                source = "CAST('xyz' as RS_integer)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType(defnid("RS_integer", regular()))) }
            ),
            Case(
                source = "CAST('xyz' as RS_BIGINT)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType(defnid("RS_BIGINT", regular()))) }
            ),
            Case(
                source = "CAST('xyz' as RS_BOOLEAN)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType(defnid("RS_BOOLEAN", regular()))) }
            )
        )
    }

    data class Case(val source: String, val ast: PartiqlAst.Expr.Cast) {
        fun toCastTest() =
            ConfiguredCastParseTest(
                source,
                PartiqlAst.build { query(cast(ast.value, ast.asType, ast.metas)) }
            )

        fun toCanCastTest() =
            ConfiguredCastParseTest(
                source.replaceFirst("CAST", "CAN_CAST"),
                PartiqlAst.build { query(canCast(ast.value, ast.asType, ast.metas)) }
            )

        fun toCanLosslessCastTest() =
            ConfiguredCastParseTest(
                source.replaceFirst("CAST", "CAN_LOSSLESS_CAST"),
                PartiqlAst.build { query(canLosslessCast(ast.value, ast.asType, ast.metas)) }
            )
    }

    data class ConfiguredCastParseTest(val source: String, val expectedAst: PartiqlAst.PartiqlAstNode) {

        val parser = PartiQLParserBuilder().customTypes(CUSTOM_TEST_TYPES).build()

        fun assertCase() {
            // Convert the query to ast
            val parsedPartiqlAst = parser.parseAstStatement(source) as PartiqlAst.Statement.Query
            assertEquals("Expected PartiqlAst and actual PartiqlAst must match", expectedAst, parsedPartiqlAst)
        }
    }
}
