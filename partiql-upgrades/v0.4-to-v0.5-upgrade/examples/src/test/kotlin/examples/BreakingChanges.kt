package examples

import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionelement.api.ionInt
import org.partiql.lang.ast.DateTimeType
import org.partiql.lang.ast.emptyMetaContainer
import org.partiql.lang.domains.PartiqlAst
import org.partiql.lang.eval.Environment
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import org.partiql.lang.eval.NullPropagatingExprFunction
import org.partiql.lang.syntax.SqlParser
import org.partiql.lang.types.StaticType
import org.partiql.lang.types.UnTypedFunctionSignature
import org.partiql.lang.types.VarargFormalParameter
import kotlin.test.Test
import kotlin.test.assertEquals

class BreakingChanges {
    @Test
    fun `api change - modeling of DateTimeType in AST`() {
        // In v0.4.0 and before, date value constructor required the outer `DateTimeType` sealed class
        DateTimeType.Date(year = 2022, month = 1, day = 1, metas = emptyMetaContainer)

        // Similarly for the time value constructor
        DateTimeType.Time(
            hour = 12,
            minute = 34,
            second = 56,
            nano = 78,
            precision = 2,
            with_time_zone = false,
            metas = emptyMetaContainer
        )
    }

    @Test
    fun `api change - refactor of ExprFunction interface`() {
        val exprValueFactory = ExprValueFactory.standard(IonSystemBuilder.standard().build())

        // `ExprFunction`s defined in v0.4.0 and before implemented the `NullPropagatingExprFunction`
        // abstract class, which implements the `ArityCheckingTrait` and `ExprFunction` interfaces
        // Below is an example `ExprFunction` definition that requires one argument and makes another
        // argument optional. The implementation will need to override `eval` to specify the behavior.
        class SomeExprFunction() : NullPropagatingExprFunction(
            name = "some_expr_function",
            arity = 1..2,
            exprValueFactory
        ) {
            override fun eval(env: Environment, args: List<ExprValue>): ExprValue {
                TODO("Implementation details with and without the optional argument")
            }
        }
    }

    @Test
    fun `api change - UntypedFunctionSignature removed`() {
        // v0.4.0 and before had a convenience data class to create an untyped `FunctionSignature` that used a variadic
        // number of [StaticType.ANY] parameters and returned [StaticType.ANY]
        val funName = "untyped_fun"
        // initialization of the untyped `FunctionSignature`
        val signature = UntypedFunctionSignature(name = funName)
        assertEquals(funName, signature.name)
        assertEquals(listOf(VarargFormalParameter(StaticType.ANY)), signature.formalParameters)
        assertEquals(StaticType.ANY, signature.returnType)
    }

    @Test
    fun `api change - modeling of NULLIF and COALESCE as AST nodes`() {
        val ion = IonSystemBuilder.standard().build()
        val parser = SqlParser(ion)

        // In v0.4.0 and before, NULLIF was modeled as an `ExprFunction` call
        val nullIfQuery = "NULLIF(1, 2)"
        val nullIfParsedAst = parser.parseAstStatement(nullIfQuery)
        val nullIfExpectedAst = PartiqlAst.build {
            query(
                call(
                    funcName = "nullif",
                    args = listOf(lit(ionInt(1)), lit(ionInt(2))),
                )
            )
        }
        assertEquals(nullIfExpectedAst, nullIfParsedAst)

        // In v0.4.0 and before, COALESCE was modeled as an `ExprFunction` call
        val coalesceQuery = "COALESCE(1, 2)"
        val coalesceParsedAst = parser.parseAstStatement(coalesceQuery)
        val coalesceExpectedAst = PartiqlAst.build {
            query(
                call(
                    funcName = "coalesce",
                    args = listOf(lit(ionInt(1)), lit(ionInt(2))),
                )
            )
        }
        assertEquals(coalesceExpectedAst, coalesceParsedAst)
    }
}
