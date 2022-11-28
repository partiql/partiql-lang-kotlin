package org.partiql.lang.syntax

import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionFloat
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.CUSTOM_TEST_TYPES
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.CastTestBase.Companion.ion
import org.partiql.lang.util.ArgumentsProviderBase

class PartiQLParserCastTests : PartiQLParserTestBase() {

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
                ast = PartiqlAst.build { cast(lit(ionBool(true)), customType("es_boolean")) }
            ),
            Case(
                source = "CAST(1 as es_integer)",
                ast = PartiqlAst.build { cast(lit(ionInt(1)), customType("es_integer")) }
            ),
            Case(
                source = "CAST(`1.2e0` as ES_FLOAT)",
                ast = PartiqlAst.build { cast(lit(ionFloat(1.2)), customType("es_float")) }
            ),
            Case(
                source = "CAST('xyz' as ES_TEXT)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("es_text")) }
            ),
            Case(
                source = "CAST('xyz' as RS_VARCHAR_MAX)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("rs_varchar_max")) }
            ),
            Case(
                source = "CAST('xyz' as RS_REAL)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("rs_real")) }
            ),
            Case(
                source = "CAST('xyz' as RS_FLOAT4)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("rs_real")) }
            ),
            Case(
                source = "CAST('xyz' as RS_DOUBLE_PRECISION)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("rs_double_precision")) }
            ),
            Case(
                source = "CAST('xyz' as RS_FLOAT)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("rs_double_precision")) }
            ),
            Case(
                source = "CAST('xyz' as rs_float8)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("rs_double_precision")) }
            ),
            Case(
                source = "CAST('xyz' as SPARK_FLOAT)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("spark_float")) }
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
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("spark_short")) }
            ),
            Case(
                source = "CAST('xyz' as SPARK_INTEGER)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("spark_integer")) }
            ),
            Case(
                source = "CAST('xyz' as SPARK_LONG)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("spark_long")) }
            ),
            Case(
                source = "CAST('xyz' as SPARK_DOUBLE)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("spark_double")) }
            ),
            Case(
                source = "CAST('xyz' as SPARK_BOOLEAN)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("spark_boolean")) }
            ),
            Case(
                source = "CAST('xyz' as RS_integer)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("rs_integer")) }
            ),
            Case(
                source = "CAST('xyz' as RS_BIGINT)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("rs_bigint")) }
            ),
            Case(
                source = "CAST('xyz' as RS_BOOLEAN)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("rs_boolean")) }
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

        val parser = PartiQLParserBuilder().ionSystem(ion).customTypes(CUSTOM_TEST_TYPES).build()

        fun assertCase() {
            // Convert the query to ast
            val parsedPartiqlAst = parser.parseAstStatement(source) as PartiqlAst.Statement.Query
            assertEquals("Expected PartiqlAst and actual PartiqlAst must match", expectedAst, parsedPartiqlAst)
        }
    }
}
