package org.partiql.lang.syntax

import com.amazon.ion.IonSystem
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionFloat
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.CUSTOM_TEST_TYPES_MAP
import org.partiql.lang.ION
import org.partiql.lang.ast.ExprNode
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.util.ArgumentsProviderBase

class SqlParserCastTests : SqlParserTestBase() {

    companion object {
        val ion: IonSystem = ION
        val parser = SqlParser(ion, CUSTOM_TEST_TYPES_MAP)

        fun parse(source: String): ExprNode = parser.parseExprNode(source)
        fun parseToAst(source: String): PartiqlAst.Statement = parser.parseAstStatement(source)

        data class CastParseTest(val source: String, val ast: PartiqlAst.Expr.Cast) {
            fun toCastTest() =
                ConfiguredCastParseTest(
                    source,
                    PartiqlAst.build { query( cast(ast.value, ast.asType, ast.metas)) })
            fun toCanCastTest() =
                ConfiguredCastParseTest(
                    source.replaceFirst("CAST", "CAN_CAST"),
                    PartiqlAst.build { query( canCast(ast.value, ast.asType, ast.metas)) })
            fun toCanLosslessCastTest() =
                ConfiguredCastParseTest(
                    source.replaceFirst("CAST", "CAN_LOSSLESS_CAST"),
                    PartiqlAst.build { query( canLosslessCast(ast.value, ast.asType, ast.metas) ) })
        }
        data class ConfiguredCastParseTest(val source: String, val expectedAst: PartiqlAst.PartiqlAstNode) {
            fun assertCase() {
                // Convert the query to ExprNode
                val parsedPartiqlAst = parseToAst(source) as PartiqlAst.Statement.Query
                assertEquals("Expected PartiqlAst and actual PartiqlAst must match", expectedAst, parsedPartiqlAst)
            }
        }

        fun case(source: String, ast: PartiqlAst.Expr.Cast) = SqlParserCastTests.Companion.CastParseTest(source, ast)

        private val cases = listOf(
            case(
                source = "CAST(true as es_boolean)",
                ast = PartiqlAst.build { cast(lit(ionBool(true)), customType("es_boolean")) }
            ),
            case(
                source = "CAST(1 as es_integer)",
                ast = PartiqlAst.build { cast(lit(ionInt(1)), customType("es_integer")) }
            ),
            case(
                source = "CAST(`1.2e0` as ES_FLOAT)",
                ast = PartiqlAst.build { cast(lit(ionFloat(1.2)), customType("es_float")) }
            ),
            case(
                source = "CAST('xyz' as ES_TEXT)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("es_text")) }
            ),
            case(
                source = "CAST('xyz' as RS_VARCHAR_MAX)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("rs_varchar_max")) }
            ),
            case(
                source = "CAST('xyz' as RS_REAL)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("rs_real")) }
            ),
            case(
                source = "CAST('xyz' as RS_FLOAT4)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("rs_float4")) }
            ),
            case(
                source = "CAST('xyz' as RS_DOUBLE_PRECISION)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("rs_double_precision")) }
            ),
            case(
                source = "CAST('xyz' as RS_FLOAT)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("rs_float")) }
            ),
            case(
                source = "CAST('xyz' as rs_float8)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("rs_float8")) }
            ),
            case(
                source = "CAST('xyz' as SPARK_FLOAT)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("spark_float")) }
            ),
            case(
                source = "CAST('xyz' as int4)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), integer4Type()) }
            ),
            case(
                source = "CAST('xyz' as smallint)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), smallintType()) }
            ),
            case(
                source = "CAST('xyz' as integer2)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), smallintType()) }
            ),
            case(
                source = "CAST('xyz' as int2)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), smallintType()) }
            ),
            case(
                source = "CAST('xyz' as integer4)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), integer4Type()) }
            ),
            case(
                source = "CAST('xyz' as int8)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), integer8Type()) }
            ),
            case(
                source = "CAST('xyz' as integer8)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), integer8Type()) }
            ),
            case(
                source = "CAST('xyz' as bigint)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), integer8Type()) }
            ),
            case(
                source = "CAST('xyz' as SPARK_SHORT)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("spark_short")) }
            ),
            case(
                source = "CAST('xyz' as SPARK_INTEGER)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("spark_integer")) }
            ),
            case(
                source = "CAST('xyz' as SPARK_LONG)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("spark_long")) }
            ),
            case(
                source = "CAST('xyz' as SPARK_DOUBLE)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("spark_double")) }
            ),
            case(
                source = "CAST('xyz' as SPARK_BOOLEAN)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("spark_boolean")) }
            ),
            case(
                source = "CAST('xyz' as RS_integer)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("rs_integer")) }
            ),
            case(
                source = "CAST('xyz' as RS_BIGINT)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("rs_bigint")) }
            ),
            case(
                source = "CAST('xyz' as RS_BOOLEAN)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), customType("rs_boolean")) }
            )
        )

        private val configuredCases = cases.flatMap {
            listOf(
                it.toCastTest(), it.toCanCastTest(), it.toCanLosslessCastTest()
            )
        }
    }

    class SqlConfiguredCastArguments : ArgumentsProviderBase() {
        override fun getParameters() = configuredCases
    }

    @ParameterizedTest
    @ArgumentsSource(SqlConfiguredCastArguments::class)
    fun configuredCast(configuredCastCase: ConfiguredCastParseTest) = configuredCastCase.assertCase()
}