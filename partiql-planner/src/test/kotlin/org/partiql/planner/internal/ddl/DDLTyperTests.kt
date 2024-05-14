package org.partiql.planner.internal.ddl

import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.Env
import org.partiql.planner.internal.ir.Constraint
import org.partiql.planner.internal.ir.DdlOp
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.Statement
import org.partiql.planner.internal.ir.Type
import org.partiql.planner.internal.ir.constraint
import org.partiql.planner.internal.ir.constraintDefinitionCheck
import org.partiql.planner.internal.ir.constraintDefinitionNotNull
import org.partiql.planner.internal.ir.ddlOpCreateTable
import org.partiql.planner.internal.ir.identifierSymbol
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpCallUnresolved
import org.partiql.planner.internal.ir.rexOpLit
import org.partiql.planner.internal.ir.rexOpVarUnresolved
import org.partiql.planner.internal.ir.statementDDL
import org.partiql.planner.internal.ir.typeAtomicInt2
import org.partiql.planner.internal.ir.typeCollection
import org.partiql.planner.internal.ir.typeRecord
import org.partiql.planner.internal.typer.PlanTyper
import org.partiql.plugins.memory.MemoryCatalog
import org.partiql.plugins.memory.MemoryConnector
import org.partiql.spi.connector.ConnectorSession
import org.partiql.types.BagType
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.int32Value
import java.util.Random
import kotlin.test.assertEquals

internal class DDLTyperTests {
    val session = object : ConnectorSession {
        override fun getQueryId(): String = "Q"

        override fun getUserId(): String = "U"
    }

    val env = Env(
        PartiQLPlanner.Session(
            queryId = Random().nextInt().toString(),
            userId = "test-user",
            currentCatalog = "test",
            currentDirectory = listOf(),
            catalogs = mapOf(
                "test" to MemoryConnector(MemoryCatalog.PartiQL().name("test").build()).getMetadata(session)
            )
        )
    )

    val typer = PlanTyper(env)

    data class Success(
        val untyped: Statement.DDL,
        val expected: StaticType
    ) {
        companion object {
            fun build(op: DdlOp.CreateTable, expected: StaticType) = Success(
                statementDDL(StaticType.ANY, op),
                expected
            )
        }
    }

    data class Failure(
        val untyped: Statement.DDL,
    )

    fun runTestCase(tc: Success) {
        val typed = typer.resolveDdl(tc.untyped)
        assertEquals(tc.expected, typed.shape)
    }

//    @ParameterizedTest
//    @MethodSource("failed")
//    fun testFailedTc(tc: Success) = runTestCase(tc)

    companion object {
        @OptIn(PartiQLValueExperimental::class)
        @JvmStatic
        fun success() = listOf(
            Success.build(
                ddlOpCreateTable(
                    id("tbl"),
                    tableInternal(
                        Type.Record.Field(
                            id("a"), typeAtomicInt2(), emptyList(),
                            false, null,
                        ),
                    ),
                    null,
                    emptyList()
                ),
                table(
                    StructType.Field("A", StaticType.unionOf(StaticType.INT2, StaticType.NULL))
                )
            ),
            Success.build(
                ddlOpCreateTable(
                    id("tbl"),
                    tableInternal(
                        Type.Record.Field(
                            id("a"), typeAtomicInt2(),
                            listOf(nonNullConstraint(null)),
                            false, null,
                        ),
                    ),
                    null,
                    emptyList()
                ),
                table(
                    StructType.Field("A", StaticType.INT2)
                )
            ),
            // Attribute Level check:
            Success.build(
                ddlOpCreateTable(
                    id("tbl"),
                    tableInternal(
                        Type.Record.Field(
                            id("a"), typeAtomicInt2(),
                            listOf(checkConstraint(null, "a", rexOpLit(int32Value(0)), "a > 0")),
                            false, null
                        ),
                    ),
                    null,
                    emptyList()
                ),
                table(
                    StructType.Field("A", StaticType.unionOf(StaticType.INT2, StaticType.NULL)),
                    structMeta = mapOf("check_constraints" to ionStructOf(field("\$_tbl_0", ionString("a > 0"))),)
                )
            ),
            // Struct Level check:
            Success.build(
                ddlOpCreateTable(
                    id("tbl"),
                    tableInternal(
                        Type.Record.Field(
                            id("a"), typeAtomicInt2(), emptyList(),
                            false, null
                        ),
                        structConstraints = listOf(checkConstraint(null, "a", rexOpLit(int32Value(0)), "a > 0")),
                    ),
                    null,
                    emptyList()
                ),
                table(
                    StructType.Field("A", StaticType.unionOf(StaticType.INT2, StaticType.NULL)),
                    structMeta = mapOf("check_constraints" to ionStructOf(field("\$_tbl_0", ionString("a > 0"))),)
                )
            ),

            // Struct Level check --- refers to multiple variable
            Success.build(
                ddlOpCreateTable(
                    id("tbl"),
                    tableInternal(
                        Type.Record.Field(
                            id("a"), typeAtomicInt2(), emptyList(),
                            false, null
                        ),
                        Type.Record.Field(
                            id("b"), typeAtomicInt2(), emptyList(),
                            false, null
                        ),
                        structConstraints = listOf(checkConstraint(null, "a", rexOpVarUnresolved(id("b"), Rex.Op.Var.Scope.LOCAL), "a > b")),
                    ),
                    null,
                    emptyList()
                ),
                table(
                    StructType.Field("A", StaticType.unionOf(StaticType.INT2, StaticType.NULL)),
                    StructType.Field("B", StaticType.unionOf(StaticType.INT2, StaticType.NULL)),
                    structMeta = mapOf("check_constraints" to ionStructOf(field("\$_tbl_0", ionString("a > b"))),)
                )
            ),

        )

        private fun table(vararg fields: StructType.Field, structMeta: Map<String, Any> = emptyMap()) = BagType(
            StructType(
                fields = fields.toList(),
                contentClosed = true,
                primaryKeyFields = emptyList(),
                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true)),
                metas = structMeta
            )
        )

        private fun tableInternal(vararg fields: Type.Record.Field, structConstraints: List<Constraint> = emptyList(), collectionConstraint: List<Constraint> = emptyList()) = typeCollection(
            typeRecord(
                fields.toList(),
                structConstraints
            ),
            false,
            collectionConstraint
        )

        private fun id(id: String) = identifierSymbol(id, Identifier.CaseSensitivity.INSENSITIVE)

        private fun nonNullConstraint(name: String?) = constraint(name, constraintDefinitionNotNull())

        @OptIn(PartiQLValueExperimental::class)
        private fun checkConstraint(name: String?, lhs: String, rhs: Rex.Op, sql: String) = constraint(
            name,
            constraintDefinitionCheck(
                rex(
                    StaticType.ANY,
                    rexOpCallUnresolved(
                        id("lt"),
                        listOf(
                            rex(StaticType.ANY, rexOpVarUnresolved(id(lhs), Rex.Op.Var.Scope.LOCAL)),
                            rex(StaticType.ANY, rhs)
                        ),
                    )
                ),
                sql,
            )

        )
    }

    @ParameterizedTest
    @MethodSource("success")
    fun testSuccessTc(tc: Success) = runTestCase(tc)
}
