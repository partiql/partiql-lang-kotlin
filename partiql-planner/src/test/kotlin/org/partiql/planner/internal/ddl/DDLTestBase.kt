package org.partiql.planner.internal.ddl

import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import org.partiql.plan.Statement
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.Env
import org.partiql.planner.internal.ir.Constraint
import org.partiql.planner.internal.ir.DdlOp
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.Type
import org.partiql.planner.internal.ir.constraint
import org.partiql.planner.internal.ir.constraintDefinitionCheck
import org.partiql.planner.internal.ir.constraintDefinitionNotNull
import org.partiql.planner.internal.ir.ddlOpCreateTable
import org.partiql.planner.internal.ir.identifierSymbol
import org.partiql.planner.internal.ir.partitionByAttrList
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpCallUnresolved
import org.partiql.planner.internal.ir.rexOpLit
import org.partiql.planner.internal.ir.rexOpVarUnresolved
import org.partiql.planner.internal.ir.statementDDL
import org.partiql.planner.internal.ir.tableProperty
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
import org.partiql.value.stringValue
import java.util.*
import org.partiql.planner.internal.ir.Statement.DDL as InternalDDLNode

internal abstract class DDLTestBase {

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

    data class TestCase(
        val untyped: InternalDDLNode,
        val normalizedShape: Type.Collection?,
        val staticType: StaticType?,
        val typed: InternalDDLNode?,
        val publicPlan: Statement.DDL?
    ) {
        companion object {
            // Convenient
            // STRUCT(a : INT2 NOT NULL)



            // STRUCT(a :INT2, b:IN2, CHECK(a > b))
            val STRUCT_A_B_A_GT_B = Pair(

            )



            @OptIn(PartiQLValueExperimental::class)
            fun build(
                op: DdlOp.CreateTable,
                tableName: String,
                shape: Type.Collection,
                expected: StaticType,
                partitionByAttrs: List<String> = emptyList(),
                tblProperties: Map<String, String> = emptyMap()
            ) = TestCase(
                untyped = statementDDL(StaticType.ANY, op),
                normalizedShape = shape,
                staticType = expected,
                typed = statementDDL(
                    expected,
                    ddlOpCreateTable(
                        id(tableName), shape,
                        partitionByAttrList(partitionByAttrs.map { id(it) }),
                        tblProperties.map { tableProperty(it.key, stringValue(it.value)) }
                    )
                ),
                publicPlan = org.partiql.plan.statementDDL(
                    org.partiql.plan.ddlOpCreateTable(
                        id(tableName).toPublicPlan(), expected,
                        org.partiql.plan.partitionByAttrList(partitionByAttrs.map { id(it).toPublicPlan() }),
                        tblProperties.map { org.partiql.plan.tableProperty(it.key, stringValue(it.value)) }
                    )
                )
            )

            fun failedNormalize(op: DdlOp.CreateTable) = TestCase(
                untyped = statementDDL(StaticType.ANY, op),
                normalizedShape = null,
                staticType = null,
                typed = null,
                publicPlan = null,
            )

            fun failedResolve(op: DdlOp.CreateTable, tableName: String, shape: Type.Collection, ) = TestCase(
                untyped = statementDDL(StaticType.ANY, op),
                normalizedShape = shape,
                staticType = null,
                typed = null,
                publicPlan = null
            )

            @OptIn(PartiQLValueExperimental::class)
            fun failedConversion(op: DdlOp.CreateTable,
                                 tableName: String,
                                 shape: Type.Collection,
                                 expected: StaticType,
                                 partitionByAttrs: List<String> = emptyList(),
                                 tblProperties: Map<String, String> = emptyMap()
            ) = TestCase(
                untyped = statementDDL(StaticType.ANY, op),
                normalizedShape = shape,
                staticType = expected,
                typed = statementDDL(
                    expected,
                    ddlOpCreateTable(
                        id(tableName), shape,
                        partitionByAttrList(partitionByAttrs.map { id(it) }),
                        tblProperties.map { tableProperty(it.key, stringValue(it.value)) }
                    )
                ),
                publicPlan = null
            )
        }
    }

    companion object {
        @OptIn(PartiQLValueExperimental::class)
        @JvmStatic
        fun success() = listOf(
            TestCase.build(
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
                "tbl",
                tableInternal(
                    Type.Record.Field(
                        id("a"), typeAtomicInt2(), emptyList(),
                        false, null,
                    ),
                ),
                table(
                    StructType.Field("A", StaticType.unionOf(StaticType.INT2, StaticType.NULL))
                ),
            ),
            TestCase.build(
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
                "tbl",
                tableInternal(
                    Type.Record.Field(
                        id("a"), typeAtomicInt2(),
                        listOf(nonNullConstraint(null)),
                        false, null,
                    ),
                ),
                table(
                    StructType.Field("A", StaticType.INT2)
                )
            ),
            // Attribute Level check:
            TestCase.build(
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
                "tbl",
                tableInternal(
                    Type.Record.Field(
                        id("a"), typeAtomicInt2(),
                        listOf(),
                        false, null
                    ),
                    structConstraints = listOf(checkConstraint(null, "a", rexOpLit(int32Value(0)), "a > 0")),
                ),
                table(
                    StructType.Field("A", StaticType.unionOf(StaticType.INT2, StaticType.NULL)),
                    structMeta = mapOf("check_constraints" to ionStructOf(field("\$_tbl_0", ionString("a > 0"))),)
                )
            ),
            // Struct Level check:
            TestCase.build(
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
            TestCase.build(
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

        private fun id(id: String) = identifierSymbol(id, INSENSITIVE)

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

        private fun Identifier.Symbol.toPublicPlan() =
            when (this.caseSensitivity) {
                SENSITIVE -> org.partiql.plan.identifierSymbol(this.symbol, org.partiql.plan.Identifier.CaseSensitivity.SENSITIVE)
                INSENSITIVE -> org.partiql.plan.identifierSymbol(this.symbol, org.partiql.plan.Identifier.CaseSensitivity.INSENSITIVE)
            }
    }
}
