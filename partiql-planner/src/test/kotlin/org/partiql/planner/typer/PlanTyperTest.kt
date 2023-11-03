package org.partiql.planner.typer

import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import org.junit.jupiter.api.Test
import org.partiql.errors.Problem
import org.partiql.errors.ProblemCallback
import org.partiql.errors.ProblemHandler
import org.partiql.errors.ProblemSeverity
import org.partiql.plan.Identifier
import org.partiql.plan.Rex
import org.partiql.plan.identifierSymbol
import org.partiql.plan.rex
import org.partiql.plan.rexOpGlobal
import org.partiql.plan.rexOpLit
import org.partiql.plan.rexOpPath
import org.partiql.plan.rexOpPathStepSymbol
import org.partiql.plan.rexOpStruct
import org.partiql.plan.rexOpStructField
import org.partiql.plan.rexOpVarUnresolved
import org.partiql.plan.statementQuery
import org.partiql.planner.Env
import org.partiql.planner.PartiQLHeader
import org.partiql.planner.PartiQLPlanner
import org.partiql.plugins.local.LocalPlugin
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.int32Value
import org.partiql.value.stringValue
import java.util.Random
import kotlin.io.path.pathString
import kotlin.io.path.toPath
import kotlin.test.assertEquals

class PlanTyperTest {

    companion object {
        private val root = this::class.java.getResource("/catalogs/default")!!.toURI().toPath().pathString

        private val catalogConfig = mapOf(
            "pql" to ionStructOf(
                field("connector_name", ionString("local")),
                field("root", ionString("$root/pql")),
            )
        )

        private val ORDERED_DUPLICATES_STRUCT = StructType(
            fields = listOf(
                StructType.Field("definition", StaticType.STRING),
                StructType.Field("definition", StaticType.FLOAT),
                StructType.Field("DEFINITION", StaticType.DECIMAL),
            ),
            contentClosed = true,
            constraints = setOf(
                TupleConstraint.Open(false),
                TupleConstraint.Ordered
            )
        )

        private val DUPLICATES_STRUCT = StructType(
            fields = listOf(
                StructType.Field("definition", StaticType.STRING),
                StructType.Field("definition", StaticType.FLOAT),
                StructType.Field("DEFINITION", StaticType.DECIMAL),
            ),
            contentClosed = true,
            constraints = setOf(
                TupleConstraint.Open(false)
            )
        )

        private val CLOSED_UNION_DUPLICATES_STRUCT = StaticType.unionOf(
            StructType(
                fields = listOf(
                    StructType.Field("definition", StaticType.STRING),
                    StructType.Field("definition", StaticType.FLOAT),
                    StructType.Field("DEFINITION", StaticType.DECIMAL),
                ),
                contentClosed = true,
                constraints = setOf(
                    TupleConstraint.Open(false)
                )
            ),
            StructType(
                fields = listOf(
                    StructType.Field("definition", StaticType.INT2),
                    StructType.Field("definition", StaticType.INT4),
                    StructType.Field("DEFINITION", StaticType.INT8),
                ),
                contentClosed = true,
                constraints = setOf(
                    TupleConstraint.Open(false),
                    TupleConstraint.Ordered
                )
            ),
        )

        private val OPEN_DUPLICATES_STRUCT = StructType(
            fields = listOf(
                StructType.Field("definition", StaticType.STRING),
                StructType.Field("definition", StaticType.FLOAT),
                StructType.Field("DEFINITION", StaticType.DECIMAL),
            ),
            contentClosed = false
        )

        private fun getTyper(): PlanTyperWrapper {
            val collector = ProblemCollector()
            val env = Env(
                listOf(PartiQLHeader),
                listOf(LocalPlugin()),
                PartiQLPlanner.Session(
                    queryId = Random().nextInt().toString(),
                    userId = "test-user",
                    currentCatalog = "pql",
                    currentDirectory = listOf("main"),
                    catalogConfig = catalogConfig
                )
            )
            return PlanTyperWrapper(PlanTyper(env, collector), collector)
        }
    }

    private class PlanTyperWrapper(
        internal val typer: PlanTyper,
        internal val collector: ProblemCollector
    )

    /**
     * This is a test to show that we convert:
     * ```
     * { 'FiRsT_KeY': { 'sEcoNd_KEY': 5 } }.first_key."sEcoNd_KEY"
     * ```
     * to
     * ```
     * { 'FiRsT_KeY': { 'sEcoNd_KEY': 5 } }."FiRsT_KeY"."sEcoNd_KEY"
     * ```
     *
     * It also checks that we type it all correctly as well.
     */
    @Test
    @OptIn(PartiQLValueExperimental::class)
    fun testReplacingStructs() {
        val wrapper = getTyper()
        val typer = wrapper.typer
        val input = statementQuery(
            root = rex(
                type = StaticType.ANY,
                op = rexOpPath(
                    root = rex(
                        StaticType.ANY,
                        rexOpStruct(
                            fields = listOf(
                                rexOpStructField(
                                    k = rex(StaticType.STRING, rexOpLit(stringValue("FiRsT_KeY"))),
                                    v = rex(
                                        StaticType.ANY,
                                        rexOpStruct(
                                            fields = listOf(
                                                rexOpStructField(
                                                    k = rex(StaticType.STRING, rexOpLit(stringValue("sEcoNd_KEY"))),
                                                    v = rex(StaticType.INT4, rexOpLit(int32Value(5)))
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    steps = listOf(
                        rexOpPathStepSymbol(identifierSymbol("first_key", Identifier.CaseSensitivity.INSENSITIVE)),
                        rexOpPathStepSymbol(identifierSymbol("sEcoNd_KEY", Identifier.CaseSensitivity.SENSITIVE)),
                    )
                )
            )
        )
        val firstKeyStruct = StructType(
            fields = mapOf(
                "sEcoNd_KEY" to StaticType.INT4
            ),
            contentClosed = true,
            constraints = setOf(
                TupleConstraint.UniqueAttrs(true),
                TupleConstraint.Open(false)
            )
        )
        val topLevelStruct = StructType(
            fields = mapOf(
                "FiRsT_KeY" to firstKeyStruct
            ),
            contentClosed = true,
            constraints = setOf(
                TupleConstraint.UniqueAttrs(true),
                TupleConstraint.Open(false)
            )
        )
        val expected = statementQuery(
            root = rex(
                type = StaticType.INT4,
                op = rexOpPath(
                    root = rex(
                        type = topLevelStruct,
                        rexOpStruct(
                            fields = listOf(
                                rexOpStructField(
                                    k = rex(StaticType.STRING, rexOpLit(stringValue("FiRsT_KeY"))),
                                    v = rex(
                                        type = firstKeyStruct,
                                        rexOpStruct(
                                            fields = listOf(
                                                rexOpStructField(
                                                    k = rex(StaticType.STRING, rexOpLit(stringValue("sEcoNd_KEY"))),
                                                    v = rex(StaticType.INT4, rexOpLit(int32Value(5)))
                                                )
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    ),
                    steps = listOf(
                        rexOpPathStepSymbol(identifierSymbol("FiRsT_KeY", Identifier.CaseSensitivity.SENSITIVE)),
                        rexOpPathStepSymbol(identifierSymbol("sEcoNd_KEY", Identifier.CaseSensitivity.SENSITIVE)),
                    )
                )
            )
        )
        val actual = typer.resolve(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testOrderedDuplicates() {
        val wrapper = getTyper()
        val typer = wrapper.typer
        val input = statementQuery(
            root = rex(
                type = StaticType.ANY,
                op = rexOpPath(
                    root = rex(
                        StaticType.ANY,
                        rexOpVarUnresolved(
                            identifierSymbol("closed_ordered_duplicates_struct", Identifier.CaseSensitivity.SENSITIVE),
                            Rex.Op.Var.Scope.DEFAULT
                        )
                    ),
                    steps = listOf(
                        rexOpPathStepSymbol(identifierSymbol("DEFINITION", Identifier.CaseSensitivity.INSENSITIVE)),
                    )
                )
            )
        )
        val expected = statementQuery(
            root = rex(
                type = StaticType.STRING,
                op = rexOpPath(
                    root = rex(
                        ORDERED_DUPLICATES_STRUCT,
                        rexOpGlobal(0)
                    ),
                    steps = listOf(
                        rexOpPathStepSymbol(identifierSymbol("definition", Identifier.CaseSensitivity.SENSITIVE)),
                    )
                )
            )
        )
        val actual = typer.resolve(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testOrderedDuplicatesWithSensitivity() {
        val wrapper = getTyper()
        val typer = wrapper.typer
        val input = statementQuery(
            root = rex(
                type = StaticType.ANY,
                op = rexOpPath(
                    root = rex(
                        StaticType.ANY,
                        rexOpVarUnresolved(
                            identifierSymbol("closed_ordered_duplicates_struct", Identifier.CaseSensitivity.SENSITIVE),
                            Rex.Op.Var.Scope.DEFAULT
                        )
                    ),
                    steps = listOf(
                        rexOpPathStepSymbol(identifierSymbol("DEFINITION", Identifier.CaseSensitivity.SENSITIVE)),
                    )
                )
            )
        )
        val expected = statementQuery(
            root = rex(
                type = StaticType.DECIMAL,
                op = rexOpPath(
                    root = rex(
                        ORDERED_DUPLICATES_STRUCT,
                        rexOpGlobal(0)
                    ),
                    steps = listOf(
                        rexOpPathStepSymbol(identifierSymbol("DEFINITION", Identifier.CaseSensitivity.SENSITIVE)),
                    )
                )
            )
        )
        val actual = typer.resolve(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testUnorderedDuplicates() {
        val wrapper = getTyper()
        val typer = wrapper.typer
        val input = statementQuery(
            root = rex(
                type = StaticType.ANY,
                op = rexOpPath(
                    root = rex(
                        StaticType.ANY,
                        rexOpVarUnresolved(
                            identifierSymbol("closed_duplicates_struct", Identifier.CaseSensitivity.SENSITIVE),
                            Rex.Op.Var.Scope.DEFAULT
                        )
                    ),
                    steps = listOf(
                        rexOpPathStepSymbol(identifierSymbol("DEFINITION", Identifier.CaseSensitivity.INSENSITIVE)),
                    )
                )
            )
        )
        val expected = statementQuery(
            root = rex(
                type = StaticType.unionOf(StaticType.STRING, StaticType.FLOAT, StaticType.DECIMAL),
                op = rexOpPath(
                    root = rex(
                        DUPLICATES_STRUCT,
                        rexOpGlobal(0)
                    ),
                    steps = listOf(
                        rexOpPathStepSymbol(identifierSymbol("DEFINITION", Identifier.CaseSensitivity.INSENSITIVE)),
                    )
                )
            )
        )
        val actual = typer.resolve(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testUnorderedDuplicatesWithSensitivity() {
        val wrapper = getTyper()
        val typer = wrapper.typer
        val input = statementQuery(
            root = rex(
                type = StaticType.ANY,
                op = rexOpPath(
                    root = rex(
                        StaticType.ANY,
                        rexOpVarUnresolved(
                            identifierSymbol("closed_duplicates_struct", Identifier.CaseSensitivity.SENSITIVE),
                            Rex.Op.Var.Scope.DEFAULT
                        )
                    ),
                    steps = listOf(
                        rexOpPathStepSymbol(identifierSymbol("DEFINITION", Identifier.CaseSensitivity.SENSITIVE)),
                    )
                )
            )
        )
        val expected = statementQuery(
            root = rex(
                type = StaticType.DECIMAL,
                op = rexOpPath(
                    root = rex(
                        DUPLICATES_STRUCT,
                        rexOpGlobal(0)
                    ),
                    steps = listOf(
                        rexOpPathStepSymbol(identifierSymbol("DEFINITION", Identifier.CaseSensitivity.SENSITIVE)),
                    )
                )
            )
        )
        val actual = typer.resolve(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testUnorderedDuplicatesWithSensitivityAndDuplicateResults() {
        val wrapper = getTyper()
        val typer = wrapper.typer
        val input = statementQuery(
            root = rex(
                type = StaticType.ANY,
                op = rexOpPath(
                    root = rex(
                        StaticType.ANY,
                        rexOpVarUnresolved(
                            identifierSymbol("closed_duplicates_struct", Identifier.CaseSensitivity.SENSITIVE),
                            Rex.Op.Var.Scope.DEFAULT
                        )
                    ),
                    steps = listOf(
                        rexOpPathStepSymbol(identifierSymbol("definition", Identifier.CaseSensitivity.SENSITIVE)),
                    )
                )
            )
        )
        val expected = statementQuery(
            root = rex(
                type = StaticType.unionOf(StaticType.STRING, StaticType.FLOAT),
                op = rexOpPath(
                    root = rex(
                        DUPLICATES_STRUCT,
                        rexOpGlobal(0)
                    ),
                    steps = listOf(
                        rexOpPathStepSymbol(identifierSymbol("definition", Identifier.CaseSensitivity.SENSITIVE)),
                    )
                )
            )
        )
        val actual = typer.resolve(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testOpenDuplicates() {
        val wrapper = getTyper()
        val typer = wrapper.typer
        val input = statementQuery(
            root = rex(
                type = StaticType.ANY,
                op = rexOpPath(
                    root = rex(
                        StaticType.ANY,
                        rexOpVarUnresolved(
                            identifierSymbol("open_duplicates_struct", Identifier.CaseSensitivity.SENSITIVE),
                            Rex.Op.Var.Scope.DEFAULT
                        )
                    ),
                    steps = listOf(
                        rexOpPathStepSymbol(identifierSymbol("definition", Identifier.CaseSensitivity.SENSITIVE)),
                    )
                )
            )
        )
        val expected = statementQuery(
            root = rex(
                type = StaticType.ANY,
                op = rexOpPath(
                    root = rex(
                        OPEN_DUPLICATES_STRUCT,
                        rexOpGlobal(0)
                    ),
                    steps = listOf(
                        rexOpPathStepSymbol(identifierSymbol("definition", Identifier.CaseSensitivity.SENSITIVE)),
                    )
                )
            )
        )
        val actual = typer.resolve(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testUnionClosedDuplicates() {
        val wrapper = getTyper()
        val typer = wrapper.typer
        val input = statementQuery(
            root = rex(
                type = StaticType.ANY,
                op = rexOpPath(
                    root = rex(
                        StaticType.ANY,
                        rexOpVarUnresolved(
                            identifierSymbol("closed_union_duplicates_struct", Identifier.CaseSensitivity.SENSITIVE),
                            Rex.Op.Var.Scope.DEFAULT
                        )
                    ),
                    steps = listOf(
                        rexOpPathStepSymbol(identifierSymbol("definition", Identifier.CaseSensitivity.INSENSITIVE)),
                    )
                )
            )
        )
        val expected = statementQuery(
            root = rex(
                type = StaticType.unionOf(StaticType.STRING, StaticType.FLOAT, StaticType.DECIMAL, StaticType.INT2),
                op = rexOpPath(
                    root = rex(
                        CLOSED_UNION_DUPLICATES_STRUCT,
                        rexOpGlobal(0)
                    ),
                    steps = listOf(
                        rexOpPathStepSymbol(identifierSymbol("definition", Identifier.CaseSensitivity.INSENSITIVE)),
                    )
                )
            )
        )
        val actual = typer.resolve(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testUnionClosedDuplicatesWithSensitivity() {
        val wrapper = getTyper()
        val typer = wrapper.typer
        val input = statementQuery(
            root = rex(
                type = StaticType.ANY,
                op = rexOpPath(
                    root = rex(
                        StaticType.ANY,
                        rexOpVarUnresolved(
                            identifierSymbol("closed_union_duplicates_struct", Identifier.CaseSensitivity.SENSITIVE),
                            Rex.Op.Var.Scope.DEFAULT
                        )
                    ),
                    steps = listOf(
                        rexOpPathStepSymbol(identifierSymbol("definition", Identifier.CaseSensitivity.SENSITIVE)),
                    )
                )
            )
        )
        val expected = statementQuery(
            root = rex(
                type = StaticType.unionOf(StaticType.STRING, StaticType.FLOAT, StaticType.INT2),
                op = rexOpPath(
                    root = rex(
                        CLOSED_UNION_DUPLICATES_STRUCT,
                        rexOpGlobal(0)
                    ),
                    steps = listOf(
                        rexOpPathStepSymbol(identifierSymbol("definition", Identifier.CaseSensitivity.SENSITIVE)),
                    )
                )
            )
        )
        val actual = typer.resolve(input)
        assertEquals(expected, actual)
    }

    /**
     * A [ProblemHandler] that collects all the encountered [Problem]s without throwing.
     *
     * This is intended to be used when wanting to collect multiple problems that may be encountered (e.g. a static type
     * inference pass that can result in multiple errors and/or warnings). This handler does not collect other exceptions
     * that may be thrown.
     */
    internal class ProblemCollector : ProblemCallback {
        private val problemList = mutableListOf<Problem>()

        val problems: List<Problem>
            get() = problemList

        val hasErrors: Boolean
            get() = problemList.any { it.details.severity == ProblemSeverity.ERROR }

        val hasWarnings: Boolean
            get() = problemList.any { it.details.severity == ProblemSeverity.WARNING }

        override fun invoke(problem: Problem) {
            problemList.add(problem)
        }
    }
}
