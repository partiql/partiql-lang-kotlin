package org.partiql.planner.internal.typer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.partiql.planner.internal.FnMatch
import org.partiql.planner.internal.FnResolver
import org.partiql.planner.internal.typer.PlanTyper.Companion.toCType
import org.partiql.spi.function.Function
import org.partiql.spi.function.Parameter
import org.partiql.spi.value.Datum
import org.partiql.types.PType

/**
 * As far as testing is concerned, we can stub out all value related things.
 * We may be able to pretty-print with string equals to also simplify things.
 * Only the "types" of expressions matter, we ignore the underlying ops.
 */
class FnResolverTest {

    @Test
    fun sanity() {
        // 1 + 1.0 -> 2.0
        val variants = listOf(
            Function.static(
                name = "plus",
                returns = PType.doublePrecision(),
                parameters = arrayOf(
                    Parameter("arg-0", PType.doublePrecision()),
                    Parameter("arg-1", PType.doublePrecision()),
                ),
                invoke = { Datum.nullValue() }
            )
        )
        val args = listOf(PType.integer().toCType(), PType.doublePrecision().toCType())
        val expectedImplicitCasts = listOf(true, false)
        val case = Case.Success(variants, args, expectedImplicitCasts)
        case.assert()
    }

    @Test
    fun split() {
        val variants = listOf(
            Function.static(
                name = "split",
                returns = PType.array(),
                parameters = arrayOf(
                    Parameter("value", PType.string()),
                    Parameter("delimiter", PType.string()),
                ),
                invoke = { Datum.nullValue() }
            )
        )
        val args = listOf(PType.string().toCType(), PType.string().toCType())
        val expectedImplicitCasts = listOf(false, false)
        val case = Case.Success(variants, args, expectedImplicitCasts)
        case.assert()
    }

    private sealed class Case {

        abstract fun assert()

        class Success(
            private val variants: List<Function>,
            private val inputs: List<CompilerType>,
            private val expectedImplicitCast: List<Boolean>,
        ) : Case() {

            /**
             * Assert we match the function, and the appropriate implicit CASTs were returned.
             *
             * TODO actually look into what the CAST functions are.
             */
            override fun assert() {
                val match = FnResolver.resolve(variants, inputs)
                val diffs = mutableListOf<String>()
                val message = buildString {
                    appendLine("Given arguments did not match any function signature")
                    appendLine("Input: (${inputs.joinToString()}})")
                }
                if (match == null) {
                    fail { message }
                }
                if (match !is FnMatch.Static) {
                    fail { "Dynamic match, expected static match: $message" }
                }

                if (match.mapping.size != expectedImplicitCast.size) {
                    fail { "Mapping size does not match expected mapping size: $message" }
                }

                // compare args
                for (i in match.mapping.indices) {
                    val m = match.mapping[i]
                    val shouldCast = expectedImplicitCast[i]
                    val diff: String? = when {
                        m == null && shouldCast -> "Arg[$i] is missing an implicit CAST"
                        m != null && !shouldCast -> "Arg[$i] had implicit CAST but should not"
                        else -> null
                    }
                    if (diff != null) diffs.add(diff)
                }
                // pretty-print some debug info
                if (diffs.isNotEmpty()) {
                    fail {
                        buildString {
                            appendLine(message)
                            diffs.forEach { appendLine(it) }
                        }
                    }
                }
            }
        }
    }
}
