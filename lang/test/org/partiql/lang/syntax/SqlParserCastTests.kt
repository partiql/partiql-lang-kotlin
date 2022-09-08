package org.partiql.lang.syntax

import com.amazon.ion.IonSystem
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionFloat
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.ION
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.util.ArgumentsProviderBase

class SqlParserCastTests : SqlParserTestBase() {

    companion object {
        val ion: IonSystem = ION

        data class CastParseTest(val source: String, val ast: PartiqlAst.Expr.Cast) {
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
        data class ConfiguredCastParseTest(val source: String, val expectedAst: PartiqlAst.PartiqlAstNode, val targetParsers: Set<ParserTypes> = defaultParserTypes) {
            fun assertCase() {
                // Convert the query to ast
                targetParsers.forEach { parser ->
                    val parsedPartiqlAst = parser.parser.parseAstStatement(source) as PartiqlAst.Statement.Query
                    assertEquals("Expected PartiqlAst and actual PartiqlAst must match", expectedAst, parsedPartiqlAst)
                }
            }
        }

        fun case(source: String, ast: PartiqlAst.Expr.Cast) = SqlParserCastTests.Companion.CastParseTest(source, ast)

        private val cases = listOf(
            case(
                source = "CAST(true as es_boolean)",
                ast = PartiqlAst.build { cast(lit(ionBool(true)), scalarType("es_boolean")) }
            ),
            case(
                source = "CAST(1 as es_integer)",
                ast = PartiqlAst.build { cast(lit(ionInt(1)), scalarType("es_integer")) }
            ),
            case(
                source = "CAST(`1.2e0` as ES_FLOAT)",
                ast = PartiqlAst.build { cast(lit(ionFloat(1.2)), scalarType("es_float")) }
            ),
            case(
                source = "CAST('xyz' as ES_TEXT)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), scalarType("es_text")) }
            ),
            case(
                source = "CAST('xyz' as RS_VARCHAR_MAX)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), scalarType("rs_varchar_max")) }
            ),
            case(
                source = "CAST('xyz' as RS_REAL)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), scalarType("rs_real")) }
            ),
            case(
                source = "CAST('xyz' as RS_FLOAT4)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), scalarType("rs_real")) }
            ),
            case(
                source = "CAST('xyz' as RS_DOUBLE_PRECISION)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), scalarType("rs_double_precision")) }
            ),
            case(
                source = "CAST('xyz' as RS_FLOAT)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), scalarType("rs_double_precision")) }
            ),
            case(
                source = "CAST('xyz' as rs_float8)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), scalarType("rs_double_precision")) }
            ),
            case(
                source = "CAST('xyz' as SPARK_FLOAT)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), scalarType("spark_float")) }
            ),
            case(
                source = "CAST('xyz' as int4)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), scalarType("int4")) }
            ),
            case(
                source = "CAST('xyz' as smallint)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), scalarType("smallint")) }
            ),
            case(
                source = "CAST('xyz' as integer2)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), scalarType("integer2")) }
            ),
            case(
                source = "CAST('xyz' as int2)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), scalarType("int2")) }
            ),
            case(
                source = "CAST('xyz' as integer4)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), scalarType("integer4")) }
            ),
            case(
                source = "CAST('xyz' as int8)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), scalarType("int8")) }
            ),
            case(
                source = "CAST('xyz' as integer8)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), scalarType("integer8")) }
            ),
            case(
                source = "CAST('xyz' as bigint)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), scalarType("bigint")) }
            ),
            case(
                source = "CAST('xyz' as SPARK_SHORT)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), scalarType("spark_short")) }
            ),
            case(
                source = "CAST('xyz' as SPARK_INTEGER)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), scalarType("spark_integer")) }
            ),
            case(
                source = "CAST('xyz' as SPARK_LONG)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), scalarType("spark_long")) }
            ),
            case(
                source = "CAST('xyz' as SPARK_DOUBLE)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), scalarType("spark_double")) }
            ),
            case(
                source = "CAST('xyz' as SPARK_BOOLEAN)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), scalarType("spark_boolean")) }
            ),
            case(
                source = "CAST('xyz' as RS_integer)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), scalarType("rs_integer")) }
            ),
            case(
                source = "CAST('xyz' as RS_BIGINT)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), scalarType("rs_bigint")) }
            ),
            case(
                source = "CAST('xyz' as RS_BOOLEAN)",
                ast = PartiqlAst.build { cast(lit(ionString("xyz")), scalarType("rs_boolean")) }
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
