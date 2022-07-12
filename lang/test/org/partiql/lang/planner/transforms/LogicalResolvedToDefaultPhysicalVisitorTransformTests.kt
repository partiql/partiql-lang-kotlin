package org.partiql.lang.planner.transforms

import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionSymbol
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.domains.PartiqlLogicalResolved
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.planner.DML_COMMAND_FIELD_ACTION
import org.partiql.lang.planner.DML_COMMAND_FIELD_ROWS
import org.partiql.lang.planner.DML_COMMAND_FIELD_TARGET_UNIQUE_ID
import org.partiql.lang.util.ArgumentsProviderBase
import kotlin.test.fail

class LogicalResolvedToDefaultPhysicalVisitorTransformTests {
    data class BexprTestCase(val input: PartiqlLogicalResolved.Bexpr, val expected: PartiqlPhysical.Bexpr)

    @ParameterizedTest
    @ArgumentsSource(ArgumentsForToPhysicalTests::class)
    fun `relational operators`(tc: BexprTestCase) {
        assertEquals(tc.expected, LogicalResolvedToDefaultPhysicalVisitorTransform().transformBexpr(tc.input))
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
        val actual = LogicalResolvedToDefaultPhysicalVisitorTransform().transformStatement(tc.input)
        if (actual != tc.expected) {
            fail("Expected and actual values must match!\nExpected: ${tc.expected}\nActual  : $actual")
        }
    }

    class ArgumentsForToDMLTests : ArgumentsProviderBase() {
        override fun getParameters() = listOf(
            DmlTestCase(
                // INSERT INTO foo VALUE 1
                PartiqlLogicalResolved.build {
                    dml(
                        dmlTarget(globalId("foo")),
                        dmlInsert(),
                        bag(lit(ionInt(1)))
                    )
                },
                PartiqlPhysical.build {
                    query(
                        struct(
                            structField(DML_COMMAND_FIELD_ACTION, "insert"),
                            structField(DML_COMMAND_FIELD_TARGET_UNIQUE_ID, lit(ionSymbol("foo"))),
                            structField(DML_COMMAND_FIELD_ROWS, bag(lit(ionInt(1))))
                        ),
                        isDml = true
                    )
                }
            ),
            DmlTestCase(
                // INSERT INTO foo SELECT x.* FROM 1 AS x
                PartiqlLogicalResolved.build {
                    dml(
                        dmlTarget(globalId("foo")),
                        dmlInsert(),
                        bindingsToValues(
                            struct(structFields(localId(0))),
                            scan(lit(ionInt(1)), varDecl(0))
                        )
                    )
                },
                PartiqlPhysical.build {
                    query(
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
                        ),
                        isDml = true
                    )
                }
            ),
            DmlTestCase(
                // DELETE FROM y AS y
                PartiqlLogicalResolved.build {
                    dml(
                        dmlTarget(globalId("foo")),
                        dmlDelete(),
                        bindingsToValues(
                            localId(0),
                            scan(globalId("y"), varDecl(0))
                        )
                    )
                },
                PartiqlPhysical.build {
                    query(
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
                        ),
                        isDml = true
                    )
                }
            ),
            DmlTestCase(
                // DELETE FROM y AS y WHERE 1=1
                PartiqlLogicalResolved.build {
                    dml(
                        dmlTarget(globalId("y")),
                        dmlDelete(),
                        bindingsToValues(
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
                    query(
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
                        ),
                        isDml = true
                    )
                }
            ),
        )
    }
}
