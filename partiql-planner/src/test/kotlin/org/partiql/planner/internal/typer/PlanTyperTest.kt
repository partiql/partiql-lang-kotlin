package org.partiql.planner.internal.typer

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.partiql.planner.PlannerConfigBuilder
import org.partiql.planner.internal.Env
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.Statement
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
import org.partiql.planner.internal.typer.PlanTyper.Companion.toCType
import org.partiql.planner.util.ErrorCollector
import org.partiql.plugins.local.LocalCatalog
import org.partiql.spi.catalog.Identifier
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table
import org.partiql.types.PType
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.int32Value
import org.partiql.value.stringValue
import kotlin.io.path.toPath

class PlanTyperTest {

    companion object {

        private val root = this::class.java.getResource("/catalogs/default/pql")!!.toURI().toPath()

        private val ANY = PType.dynamic().toCType()
        private val STRING = PType.string().toCType()
        private val INT4 = PType.integer().toCType()
        private val DOUBLE_PRECISION = PType.doublePrecision().toCType()
        private val DECIMAL = PType.decimal().toCType()

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

        private val LITERAL_STRUCT_1_FIRST_KEY_TYPE = PType.row(
            listOf(CompilerType.Field("sEcoNd_KEY", INT4)),
        ).toCType()

        @OptIn(PartiQLValueExperimental::class)
        private val LITERAL_STRUCT_1_TYPED: Rex
            get() {
                val topLevelStruct = PType.row(
                    listOf(CompilerType.Field("FiRsT_KeY", LITERAL_STRUCT_1_FIRST_KEY_TYPE)),
                ).toCType()
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

        private val ORDERED_DUPLICATES_STRUCT = PType.row(
            listOf(
                CompilerType.Field("definition", STRING),
                CompilerType.Field("definition", DOUBLE_PRECISION),
                CompilerType.Field("DEFINITION", DECIMAL),
            ),
        ).toCType()

        private val DUPLICATES_STRUCT = PType.row(
            listOf(
                CompilerType.Field("definition", STRING),
                CompilerType.Field("definition", DOUBLE_PRECISION),
                CompilerType.Field("DEFINITION", DECIMAL),
            ),
        ).toCType()

        private val CLOSED_UNION_DUPLICATES_STRUCT = ANY

        private val OPEN_DUPLICATES_STRUCT = PType.struct().toCType()

        private fun getTyper(): PlanTyperWrapper {
            val config = PlannerConfigBuilder().setErrorListener(ErrorCollector()).build()
            val env = Env(
                Session.builder()
                    .catalog("pql")
                    .namespace("main")
                    .catalogs(
                        LocalCatalog.builder()
                            .name("pql")
                            .root(root)
                            .build()
                    )
                    .build()
            )
            return PlanTyperWrapper(PlanTyper(env, config))
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
    @Disabled("PartiQL doesn't have the concept of ordered structs (yet)")
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
    @Disabled("PartiQL doesn't have the concept of ordered structs (yet)")
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
                PType.dynamic().toCType()
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
                PType.dynamic().toCType()
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
                PType.dynamic().toCType()
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
                PType.dynamic().toCType()
            )
        )

        val actual = typer.resolve(input)
        assertEquals(expected, actual)
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun rexString(str: String) = rex(STRING, rexOpLit(stringValue(str)))

    private fun Rex.pathKey(key: String, type: CompilerType = ANY): Rex = Rex(type, rexOpPathKey(this, rexString(key)))

    private fun Rex.pathSymbol(key: String, type: CompilerType = ANY): Rex = Rex(type, rexOpPathSymbol(this, key))

    private fun unresolvedSensitiveVar(name: String, type: CompilerType = ANY): Rex {
        return rex(
            type,
            rexOpVarUnresolved(
                Identifier.delimited(name),
                Rex.Op.Var.Scope.DEFAULT
            )
        )
    }

    private fun global(type: CompilerType, path: List<String>): Rex {
        val catalog = "pql"
        val name = Name.of(path)
        val table = Table.empty(name, type)
        return rex(type, rexOpVarGlobal(refObj(catalog, name, type, table)))
    }

    private fun assertEquals(expected: Statement, actual: Statement) {
        return assert(expected == actual) {
            buildString {
                appendLine("Expected : $expected")
                appendLine("Actual   : $actual")
            }
        }
    }
}
