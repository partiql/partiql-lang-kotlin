package org.partiql.planner.internal.typer

import org.junit.jupiter.api.Test
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.Env
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.identifierSymbol
import org.partiql.planner.internal.ir.refObj
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpLit
import org.partiql.planner.internal.ir.rexOpPathKey
import org.partiql.planner.internal.ir.rexOpPathSymbol
import org.partiql.planner.internal.ir.rexOpStruct
import org.partiql.planner.internal.ir.rexOpStructField
import org.partiql.planner.internal.ir.rexOpVarGlobal
import org.partiql.planner.internal.ir.rexOpVarUnresolved
import org.partiql.planner.internal.ir.statementQuery
import org.partiql.planner.util.ProblemCollector
import org.partiql.plugins.local.LocalConnector
import org.partiql.types.StaticType
import org.partiql.types.StaticType.Companion.ANY
import org.partiql.types.StaticType.Companion.DECIMAL
import org.partiql.types.StaticType.Companion.FLOAT
import org.partiql.types.StaticType.Companion.INT2
import org.partiql.types.StaticType.Companion.INT4
import org.partiql.types.StaticType.Companion.STRING
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.int32Value
import org.partiql.value.stringValue
import java.util.Random
import kotlin.io.path.toPath
import kotlin.test.assertEquals

class PlanTyperTest {

    companion object {

        private val root = this::class.java.getResource("/catalogs/default/pql")!!.toURI().toPath()

        @OptIn(PartiQLValueExperimental::class)
        private val LITERAL_STRUCT_1 = rex(
            ANY,
            rexOpStruct(
                fields = listOf(
                    rexOpStructField(
                        k = rex(STRING, rexOpLit(stringValue("FiRsT_KeY"))),
                        v = rex(
                            ANY,
                            rexOpStruct(
                                fields = listOf(
                                    rexOpStructField(
                                        k = rex(STRING, rexOpLit(stringValue("sEcoNd_KEY"))),
                                        v = rex(INT4, rexOpLit(int32Value(5)))
                                    )
                                )
                            )
                        )
                    )
                )
            )
        )

        private val LITERAL_STRUCT_1_FIRST_KEY_TYPE = StructType(
            fields = mapOf(
                "sEcoNd_KEY" to INT4
            ),
            contentClosed = true,
            constraints = setOf(
                TupleConstraint.UniqueAttrs(true),
                TupleConstraint.Open(false)
            )
        )

        @OptIn(PartiQLValueExperimental::class)
        private val LITERAL_STRUCT_1_TYPED: Rex
            get() {
                val topLevelStruct = StructType(
                    fields = mapOf(
                        "FiRsT_KeY" to LITERAL_STRUCT_1_FIRST_KEY_TYPE
                    ),
                    contentClosed = true,
                    constraints = setOf(
                        TupleConstraint.UniqueAttrs(true),
                        TupleConstraint.Open(false)
                    )
                )
                return rex(
                    type = topLevelStruct,
                    rexOpStruct(
                        fields = listOf(
                            rexOpStructField(
                                k = rex(STRING, rexOpLit(stringValue("FiRsT_KeY"))),
                                v = rex(
                                    type = LITERAL_STRUCT_1_FIRST_KEY_TYPE,
                                    rexOpStruct(
                                        fields = listOf(
                                            rexOpStructField(
                                                k = rex(STRING, rexOpLit(stringValue("sEcoNd_KEY"))),
                                                v = rex(INT4, rexOpLit(int32Value(5)))
                                            )
                                        )
                                    )
                                )
                            )
                        )
                    )
                )
            }

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
            ProblemCollector()
            val env = Env(
                PartiQLPlanner.Session(
                    queryId = Random().nextInt().toString(),
                    userId = "test-user",
                    currentCatalog = "pql",
                    currentDirectory = listOf("main"),
                    catalogs = mapOf(
                        "pql" to LocalConnector.Metadata(root)
                    ),
                )
            )
            return PlanTyperWrapper(PlanTyper(env))
        }
    }

    private class PlanTyperWrapper(
        internal val typer: PlanTyper,
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
    fun testReplacingStructs() {
        val wrapper = getTyper()
        val typer = wrapper.typer
        val input = statementQuery(LITERAL_STRUCT_1.pathSymbol("first_key").pathKey("sEcoNd_KEY"))
        val expected = statementQuery(
            LITERAL_STRUCT_1_TYPED.pathKey("FiRsT_KeY", LITERAL_STRUCT_1_FIRST_KEY_TYPE).pathKey("sEcoNd_KEY", INT4)
        )

        val actual = typer.resolve(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testOrderedDuplicates() {
        val wrapper = getTyper()
        val typer = wrapper.typer
        val input = statementQuery(
            unresolvedSensitiveVar("closed_ordered_duplicates_struct").pathSymbol("DEFINITION")
        )
        val expected = statementQuery(
            global(
                type = ORDERED_DUPLICATES_STRUCT,
                path = listOf("main", "closed_ordered_duplicates_struct"),
            ).pathKey("definition", STRING)
        )

        val actual = typer.resolve(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testOrderedDuplicatesWithSensitivity() {
        val wrapper = getTyper()
        val typer = wrapper.typer
        val input = statementQuery(unresolvedSensitiveVar("closed_ordered_duplicates_struct").pathKey("DEFINITION"))
        val expected = statementQuery(
            global(
                type = ORDERED_DUPLICATES_STRUCT,
                path = listOf("main", "closed_ordered_duplicates_struct"),
            ).pathKey("DEFINITION", DECIMAL)
        )

        val actual = typer.resolve(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testUnorderedDuplicates() {
        val wrapper = getTyper()
        val typer = wrapper.typer
        val input = statementQuery(unresolvedSensitiveVar("closed_duplicates_struct").pathSymbol("DEFINITION"))
        val expected = statementQuery(
            global(
                type = DUPLICATES_STRUCT,
                path = listOf("main", "closed_duplicates_struct"),
            ).pathSymbol(
                "DEFINITION",
                StaticType.unionOf(STRING, FLOAT, DECIMAL)
            )
        )

        val actual = typer.resolve(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testUnorderedDuplicatesWithSensitivity() {
        val wrapper = getTyper()
        val typer = wrapper.typer
        val input = statementQuery(unresolvedSensitiveVar("closed_duplicates_struct").pathKey("DEFINITION"))
        val expected = statementQuery(
            global(
                type = DUPLICATES_STRUCT,
                path = listOf("main", "closed_duplicates_struct"),
            ).pathKey("DEFINITION", DECIMAL)
        )

        val actual = typer.resolve(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testUnorderedDuplicatesWithSensitivityAndDuplicateResults() {
        val wrapper = getTyper()
        val typer = wrapper.typer
        val input = statementQuery(unresolvedSensitiveVar("closed_duplicates_struct").pathKey("definition"))
        val expected = statementQuery(
            global(
                type = DUPLICATES_STRUCT,
                path = listOf("main", "closed_duplicates_struct"),
            ).pathKey(
                "definition",
                StaticType.unionOf(StaticType.STRING, StaticType.FLOAT)
            )
        )

        val actual = typer.resolve(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testOpenDuplicates() {
        val wrapper = getTyper()
        val typer = wrapper.typer
        val input = statementQuery(unresolvedSensitiveVar("open_duplicates_struct").pathKey("definition"))
        val expected = statementQuery(
            global(
                type = OPEN_DUPLICATES_STRUCT,
                path = listOf("main", "open_duplicates_struct"),
            ).pathKey("definition")
        )

        val actual = typer.resolve(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testUnionClosedDuplicates() {
        val wrapper = getTyper()
        val typer = wrapper.typer
        val input = statementQuery(unresolvedSensitiveVar("closed_union_duplicates_struct").pathSymbol("definition"))
        val expected = statementQuery(
            global(
                type = CLOSED_UNION_DUPLICATES_STRUCT,
                path = listOf("main", "closed_union_duplicates_struct"),
            ).pathSymbol(
                "definition",
                StaticType.unionOf(STRING, FLOAT, DECIMAL, INT2)
            )
        )

        val actual = typer.resolve(input)
        assertEquals(expected, actual)
    }

    @Test
    fun testUnionClosedDuplicatesWithSensitivity() {
        val wrapper = getTyper()
        val typer = wrapper.typer
        val input = statementQuery(unresolvedSensitiveVar("closed_union_duplicates_struct").pathKey("definition"))
        val expected = statementQuery(
            global(
                type = CLOSED_UNION_DUPLICATES_STRUCT,
                path = listOf("main", "closed_union_duplicates_struct"),
            ).pathKey(
                "definition",
                StaticType.unionOf(STRING, FLOAT, INT2)
            )
        )

        val actual = typer.resolve(input)
        assertEquals(expected, actual)
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun rexString(str: String) = rex(STRING, rexOpLit(stringValue(str)))

    private fun Rex.pathKey(key: String, type: StaticType = ANY): Rex = Rex(type, rexOpPathKey(this, rexString(key)))

    private fun Rex.pathSymbol(key: String, type: StaticType = ANY): Rex = Rex(type, rexOpPathSymbol(this, key))

    private fun unresolvedSensitiveVar(name: String, type: StaticType = ANY): Rex {
        return rex(
            type,
            rexOpVarUnresolved(
                identifierSymbol(name, Identifier.CaseSensitivity.SENSITIVE),
                Rex.Op.Var.Scope.DEFAULT
            )
        )
    }

    private fun global(type: StaticType, path: List<String>): Rex {
        return rex(
            type,
            rexOpVarGlobal(refObj(catalog = "pql", path = path, type))
        )
    }
}
