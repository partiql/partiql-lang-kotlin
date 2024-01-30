package org.partiql.planner.internal.typer

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.partiql.planner.internal.FnMatch
import org.partiql.planner.internal.FnResolver
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.StaticType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType

/**
 * As far as testing is concerned, we can stub out all value related things.
 * We may be able to pretty-print with string equals to also simplify things.
 * Only the "types" of expressions matter, we ignore the underlying ops.
 */
@OptIn(PartiQLValueExperimental::class, FnExperimental::class)
class FnResolverTest {

    @Test
    fun sanity() {
        // 1 + 1.0 -> 2.0
        val variants = listOf(
            FnSignature(
                name = "plus",
                returns = PartiQLValueType.FLOAT64,
                parameters = listOf(
                    FnParameter("arg-0", PartiQLValueType.FLOAT64),
                    FnParameter("arg-1", PartiQLValueType.FLOAT64),
                ),
            )
        )
        val args = listOf(StaticType.INT4, StaticType.FLOAT)
        val expectedImplicitCasts = listOf(true, false)
        val case = Case.Success(variants, args, expectedImplicitCasts)
        case.assert()
    }

    @Test
    fun split() {
        val variants = listOf(
            FnSignature(
                name = "split",
                returns = PartiQLValueType.LIST,
                parameters = listOf(
                    FnParameter("value", PartiQLValueType.STRING),
                    FnParameter("delimiter", PartiQLValueType.STRING),
                ),
                isNullable = false,
            )
        )
        val args = listOf(StaticType.STRING, StaticType.STRING)
        val expectedImplicitCasts = listOf(false, false)
        val case = Case.Success(variants, args, expectedImplicitCasts)
        case.assert()
    }

    private sealed class Case {

        abstract fun assert()

        class Success(
            private val variants: List<FnSignature>,
            private val inputs: List<StaticType>,
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
