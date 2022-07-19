package org.partiql.lang.planner.transforms

import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionSymbol
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.ast.UNKNOWN_SOURCE_LOCATION
import org.partiql.lang.domains.PartiqlLogicalResolved
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.Problem
import org.partiql.lang.errors.ProblemCollector
import org.partiql.lang.planner.DML_COMMAND_FIELD_ACTION
import org.partiql.lang.planner.DML_COMMAND_FIELD_ROWS
import org.partiql.lang.planner.DML_COMMAND_FIELD_TARGET_UNIQUE_ID
import org.partiql.lang.planner.PlanningProblemDetails
import org.partiql.lang.util.ArgumentsProviderBase
import kotlin.test.fail

class LogicalResolvedToDefaultPhysicalVisitorTransformTests {
    data class BexprTestCase(val input: PartiqlLogicalResolved.Bexpr, val expected: PartiqlPhysical.Bexpr)

    @ParameterizedTest
    @ArgumentsSource(ArgumentsForToPhysicalTests::class)
    fun `relational operators`(tc: BexprTestCase) {
        val problemHandler = ProblemCollector()
        assertEquals(tc.expected, LogicalResolvedToDefaultPhysicalVisitorTransform(problemHandler).transformBexpr(tc.input))
        assertEquals(0, problemHandler.problems.size)
    }

    class ArgumentsForToPhysicalTests : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            BexprTestCase(
                PartiqlLogicalResolved.build {
                    scan(
                        expr = globalId("foo"),
                        asDecl = varDecl(0),
                        atDecl = varDecl(1),
                        byDecl = varDecl(2)
                    )
                },
                PartiqlPhysical.build {
                    scan(
                        i = DEFAULT_IMPL,
                        expr = globalId("foo"),
                        asDecl = varDecl(0),
                        atDecl = varDecl(1),
                        byDecl = varDecl(2)
                    )
                }
            ),
            BexprTestCase(
                PartiqlLogicalResolved.build {
                    filter(
                        predicate = lit(ionBool(true)),
                        source = scan(
                            expr = globalId("foo"),
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
                            expr = globalId("foo"),
                            asDecl = varDecl(0),
                            atDecl = varDecl(1),
                            byDecl = varDecl(2)
                        )
                    )
                }
            )
        )
    }

    data class DmlTestCase(val input: PartiqlLogicalResolved.Statement, val expected: PartiqlPhysical.Statement)

    @ParameterizedTest
    @ArgumentsSource(ArgumentsForToDMLTests::class)
    fun `DML to query`(tc: DmlTestCase) {
        val problemHandler = ProblemCollector()
        val actual = LogicalResolvedToDefaultPhysicalVisitorTransform(problemHandler).transformStatement(tc.input)
        if (actual != tc.expected) {
            fail("Expected and actual values must match!\nExpected: ${tc.expected}\nActual  : $actual")
        }

        assertEquals(0, problemHandler.problems.size, "did not expect any errors or warnings")
    }

    class ArgumentsForToDMLTests : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            DmlTestCase(
                // INSERT INTO foo VALUE 1
                PartiqlLogicalResolved.build {
                    dml(
                        target = globalId("foo"),
                        operation = dmlInsert(),
                        rows = bag(lit(ionInt(1)))
                    )
                },
                PartiqlPhysical.build {
                    dmlQuery(
                        struct(
                            structField(DML_COMMAND_FIELD_ACTION, "insert"),
                            structField(DML_COMMAND_FIELD_TARGET_UNIQUE_ID, lit(ionSymbol("foo"))),
                            structField(DML_COMMAND_FIELD_ROWS, bag(lit(ionInt(1))))
                        )
                    )
                }
            ),
            DmlTestCase(
                // INSERT INTO foo SELECT x.* FROM 1 AS x
                PartiqlLogicalResolved.build {
                    dml(
                        target = globalId("foo"),
                        operation = dmlInsert(),
                        rows = bindingsToValues(
                            struct(structFields(localId(0))),
                            scan(lit(ionInt(1)), varDecl(0))
                        )
                    )
                },
                PartiqlPhysical.build {
                    dmlQuery(
                        struct(
                            structField(DML_COMMAND_FIELD_ACTION, "insert"),
                            structField(DML_COMMAND_FIELD_TARGET_UNIQUE_ID, lit(ionSymbol("foo"))),
                            structField(
                                DML_COMMAND_FIELD_ROWS,
                                bindingsToValues(
                                    struct(structFields(localId(0))),
                                    scan(
                                        i = DEFAULT_IMPL,
                                        expr = lit(ionInt(1)),
                                        asDecl = varDecl(0)
                                    )
                                )
                            )
                        )
                    )
                }
            ),
            DmlTestCase(
                // DELETE FROM y AS y
                PartiqlLogicalResolved.build {
                    dml(
                        target = globalId("foo"),
                        operation = dmlDelete(),
                        rows = bindingsToValues(
                            localId(0),
                            scan(globalId("y"), varDecl(0))
                        )
                    )
                },
                PartiqlPhysical.build {
                    dmlQuery(
                        struct(
                            structField(DML_COMMAND_FIELD_ACTION, "delete"),
                            structField(DML_COMMAND_FIELD_TARGET_UNIQUE_ID, lit(ionSymbol("foo"))),
                            structField(
                                DML_COMMAND_FIELD_ROWS,
                                bindingsToValues(
                                    localId(0),
                                    scan(
                                        i = DEFAULT_IMPL,
                                        expr = globalId("y"),
                                        asDecl = varDecl(0)
                                    )
                                )
                            )
                        )
                    )
                }
            ),
            DmlTestCase(
                // DELETE FROM y AS y WHERE 1=1
                PartiqlLogicalResolved.build {
                    dml(
                        target = globalId("y"),
                        operation = dmlDelete(),
                        rows = bindingsToValues(
                            localId(0),
                            // this logical plan is same as previous but includes this filter
                            filter(
                                eq(lit(ionInt(1)), lit(ionInt(1))),
                                scan(globalId("y"), varDecl(0))
                            )
                        )
                    )
                },
                PartiqlPhysical.build {
                    dmlQuery(
                        struct(
                            structField(DML_COMMAND_FIELD_ACTION, "delete"),
                            structField(DML_COMMAND_FIELD_TARGET_UNIQUE_ID, lit(ionSymbol("y"))),
                            structField(
                                DML_COMMAND_FIELD_ROWS,
                                bindingsToValues(
                                    localId(0),
                                    // this logical plan is same as previous but includes this filter
                                    filter(
                                        i = DEFAULT_IMPL,
                                        eq(lit(ionInt(1)), lit(ionInt(1))),
                                        scan(
                                            i = DEFAULT_IMPL,
                                            expr = globalId("y"),
                                            asDecl = varDecl(0)
                                        )
                                    )
                                )
                            )
                        )
                    )
                }
            ),
        )
    }

    data class UnimplementedFeatureTestCase(val input: PartiqlLogicalResolved.Statement, val expectedProblem: Problem)
    @ParameterizedTest
    @ArgumentsSource(ArgumentsForUnimplementedFeatureTests::class)
    fun `unimplemented features are blocked`(tc: UnimplementedFeatureTestCase) {
        val problemHandler = ProblemCollector()
        LogicalResolvedToDefaultPhysicalVisitorTransform(problemHandler).transformStatement(tc.input)
        Assertions.assertFalse(problemHandler.hasWarnings, "didn't expect any warnings")
        Assertions.assertTrue(problemHandler.hasErrors, "at least one error was expected")

        assertEquals(tc.expectedProblem, problemHandler.problems.first())
    }

    class ArgumentsForUnimplementedFeatureTests : ArgumentsProviderBase() {
        private fun createSimpleDmlStatement(
            target: PartiqlLogicalResolved.Expr
        ) = PartiqlLogicalResolved.build {
            dml(
                target = target,
                operation = dmlDelete(),
                rows = bindingsToValues(localId(0), scan(lit(ionSymbol("doesntmattereither")), varDecl(0)))
            )
        }

        override fun getParameters() = listOf(
            UnimplementedFeatureTestCase(
                createSimpleDmlStatement(
                    target = PartiqlLogicalResolved.build {
                        // path expressions such as this are not currently supported, but it is likely that
                        // we will one day need to support dot notation in our from source table names.  this is
                        // supported by the parser but not within the scope of current efforts.
                        path(globalId("doesntmatter"), pathExpr(lit(ionSymbol("foo")), caseInsensitive()))
                    }
                ),
                Problem(
                    UNKNOWN_SOURCE_LOCATION,
                    PlanningProblemDetails.InvalidDmlTarget
                )
            ),
            UnimplementedFeatureTestCase(
                createSimpleDmlStatement(
                    target = PartiqlLogicalResolved.build {
                        // the parser doesn't actually allow expressions other than identifiers (which are
                        // later resolved to (global_id ...) nodes to serve as pass targets, but we include this test
                        // case in case something really funky is going on, like a user specified planner pass that
                        // specifies an invalid dml target.
                        lit(ionInt(42))
                    }
                ),
                Problem(
                    UNKNOWN_SOURCE_LOCATION,
                    PlanningProblemDetails.InvalidDmlTarget
                )
            )
        )
    }
}
