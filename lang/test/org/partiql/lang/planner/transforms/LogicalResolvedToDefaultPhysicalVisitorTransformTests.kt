package org.partiql.lang.planner.transforms

import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.ionSymbol
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ArgumentsSource
import org.partiql.lang.domains.PartiqlLogicalResolved
import org.partiql.lang.domains.PartiqlPhysical
import org.partiql.lang.errors.ProblemCollector
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
            ),
            BexprTestCase(
                PartiqlLogicalResolved.build {
<<<<<<< HEAD
                    unpivot(
                        expr = globalId("foo"),
                        asDecl = varDecl(0),
                        atDecl = varDecl(1),
                        byDecl = varDecl(2)
                    )
                },
                PartiqlPhysical.build {
                    unpivot(
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
                    sort(
                        source = scan(
                            expr = globalId("foo"),
                            asDecl = varDecl(0),
                            atDecl = varDecl(1),
                            byDecl = varDecl(2)
                        ),
                        sortSpecs = listOf(
                            sortSpec(
                                globalId("foo"),
                                asc(),
                                nullsLast()
                            )
                        )
                    )
                },
                PartiqlPhysical.build {
                    sort(
=======
                    window(
                        source = scan(
                            expr = globalId("foo"),
                            asDecl = varDecl(0)
                        ),
                        windowSpecification = over(
                            windowPartitionList(
                                path(
                                    localId(0),
                                    listOf(pathExpr(lit(ionSymbol("a")), PartiqlLogicalResolved.CaseSensitivity.CaseInsensitive()))
                                )
                            ),
                            windowSortSpecList(
                                sortSpec(
                                    path(
                                        localId(0),
                                        listOf(pathExpr(lit(ionSymbol("b")), PartiqlLogicalResolved.CaseSensitivity.CaseInsensitive()))
                                    )
                                )
                            )
                        ),
                        windowExpression = windowExpression(
                            varDecl(1),
                            "lag",
                            path(
                                localId(0),
                                listOf(
                                    pathExpr(lit(ionSymbol("b")), PartiqlLogicalResolved.CaseSensitivity.CaseInsensitive())
                                )
                            )
                        )

                    )
                },
                PartiqlPhysical.build {
                    window(
>>>>>>> a8e77064 (wip)
                        i = DEFAULT_IMPL,
                        source = scan(
                            i = DEFAULT_IMPL,
                            expr = globalId("foo"),
<<<<<<< HEAD
                            asDecl = varDecl(0),
                            atDecl = varDecl(1),
                            byDecl = varDecl(2)
                        ),
                        sortSpecs = listOf(
                            sortSpec(
                                globalId("foo"),
                                asc(),
                                nullsLast()
=======
                            asDecl = varDecl(0)
                        ),
                        windowSpecification = over(
                            windowPartitionList(
                                path(
                                    localId(0),
                                    listOf(pathExpr(lit(ionSymbol("a")), PartiqlPhysical.CaseSensitivity.CaseInsensitive()))
                                )
                            ),
                            windowSortSpecList(
                                sortSpec(
                                    path(
                                        localId(0),
                                        listOf(pathExpr(lit(ionSymbol("b")), PartiqlPhysical.CaseSensitivity.CaseInsensitive()))
                                    )
                                )
                            )
                        ),
                        windowExpression = windowExpression(
                            varDecl(1),
                            "lag",
                            path(
                                localId(0),
                                listOf(
                                    pathExpr(lit(ionSymbol("b")), PartiqlPhysical.CaseSensitivity.CaseInsensitive())
                                )
>>>>>>> a8e77064 (wip)
                            )
                        )
                    )
                }
<<<<<<< HEAD
            )
=======
            ),
>>>>>>> a8e77064 (wip)
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
                        uniqueId = "foo",
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
                        uniqueId = "foo",
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
                // INSERT INTO foo SELECT x.* FROM 1 AS x ON CONFLICT DO REPLACE EXCLUDED
                PartiqlLogicalResolved.build {
                    dml(
                        uniqueId = "foo",
                        operation = dmlReplace(),
                        rows = bindingsToValues(
                            struct(structFields(localId(0))),
                            scan(lit(ionInt(1)), varDecl(0))
                        )
                    )
                },
                PartiqlPhysical.build {
                    dmlQuery(
                        struct(
                            structField(DML_COMMAND_FIELD_ACTION, "replace"),
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
                        uniqueId = "foo",
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
                        uniqueId = "y",
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
}
