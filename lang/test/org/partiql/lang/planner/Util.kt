package org.partiql.lang.planner

import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionSymbol
import com.amazon.ionelement.api.toIonValue
import org.junit.Assert
import org.partiql.lang.ION
import org.partiql.lang.ast.SourceLocationMeta
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemDetails
import org.partiql.lang.util.SexpAstPrettyPrinter

/**
 * Creates a fake implementation of [GlobalVariableResolver] with the specified [globalVariableNames].
 *
 * The fake unique identifier of bound variables is computed to be `fake_uid_for_${globalVariableName}`.
 */
fun createFakeGlobalsResolver(vararg globalVariableNames: Pair<String, String>) =
    GlobalVariableResolver { bindingName ->
        val matches = globalVariableNames.filter { bindingName.isEquivalentTo(it.first) }
        when (matches.size) {
            0 -> GlobalResolutionResult.Undefined
            else -> GlobalResolutionResult.GlobalVariable(matches.first().second)
        }
    }

fun problem(line: Int, charOffset: Int, detail: ProblemDetails): Problem =
    Problem(SourceLocationMeta(line.toLong(), charOffset.toLong()), detail)

fun PartiqlPhysical.Builder.litTrue() = lit(ionBool(true))
fun PartiqlPhysical.Builder.litInt(value: Int) = lit(ionInt(value.toLong()))
fun PartiqlPhysical.Builder.litSymbol(value: String) = lit(ionSymbol(value))
fun PartiqlPhysical.Builder.litString(value: String) = lit(ionString(value))

fun assertSexpEquals(
    expectedValue: IonElement,
    actualValue: IonElement,
    message: String = ""
) {
    if (!expectedValue.equals(actualValue)) {
        Assert.fail(
            "Expected and actual values do not match: $message\n" +
                    "Expected:\n${SexpAstPrettyPrinter.format(expectedValue.asAnyElement().toIonValue(ION))}\n" +
                    "Actual:\n${SexpAstPrettyPrinter.format(actualValue.asAnyElement().toIonValue(ION))}"
        )
    }
}