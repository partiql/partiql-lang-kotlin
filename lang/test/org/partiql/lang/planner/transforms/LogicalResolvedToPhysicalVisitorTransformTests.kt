package org.partiql.lang.planner.transforms

import com.amazon.ionelement.api.ionBool
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.domains.PartiqlLogicalResolved
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.util.ArgumentsProviderBase

class LogicalResolvedToPhysicalVisitorTransformTests {
    data class TestCase(val input: PartiqlLogicalResolved.Bexpr, val expected: PartiqlPhysical.Bexpr)

    @ParameterizedTest
    @ArgumentsSource(ArgumentsForToPhysicalTests::class)
    fun `to physical`(tc: TestCase) {
        assertEquals(tc.expected, LogicalResolvedToPhysicalVisitorTransform().transformBexpr(tc.input))
    }

    class ArgumentsForToPhysicalTests : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            TestCase(
                PartiqlLogicalResolved.build {
                    scan(
                        expr = globalId("foo", "foo"),
                        asDecl = varDecl(0),
                        atDecl = varDecl(1),
                        byDecl = varDecl(2)
                    )
                },
                PartiqlPhysical.build {
                    scan(
                        i = DEFAULT_IMPL,
                        expr = globalId("foo", "foo"),
                        asDecl = varDecl(0),
                        atDecl = varDecl(1),
                        byDecl = varDecl(2)
                    )
                }
            ),
            TestCase(
                PartiqlLogicalResolved.build {
                    filter(
                        predicate = lit(ionBool(true)),
                        source = scan(
                            expr = globalId("foo", "foo"),
                            asDecl = varDecl(0),
                            atDecl = varDecl(1),
                            byDecl = varDecl(2)
                        )
                    )
                },
                PartiqlPhysical.build {
                    filter(
                        i = DEFAULT_IMPL,
                        predicate = lit(ionBool(true)),
                        source = scan(
                            i = DEFAULT_IMPL,
                            expr = globalId("foo", "foo"),
                            asDecl = varDecl(0),
                            atDecl = varDecl(1),
                            byDecl = varDecl(2)
                        )
                    )
                }
            )
        )
    }
}
