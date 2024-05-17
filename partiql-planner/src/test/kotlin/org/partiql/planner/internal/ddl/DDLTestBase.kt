package org.partiql.planner.internal.ddl

import com.amazon.ionelement.api.field
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import org.partiql.plan.Statement
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.PartiQLPlannerBuilder
import org.partiql.planner.internal.Env
import org.partiql.planner.internal.ir.Constraint
import org.partiql.planner.internal.ir.Identifier
import org.partiql.planner.internal.ir.Ref
import org.partiql.planner.internal.ir.Rex
import org.partiql.planner.internal.ir.Type
import org.partiql.planner.internal.ir.constraint
import org.partiql.planner.internal.ir.constraintDefinitionCheck
import org.partiql.planner.internal.ir.constraintDefinitionNotNull
import org.partiql.planner.internal.ir.constraintDefinitionUnique
import org.partiql.planner.internal.ir.ddlOpCreateTable
import org.partiql.planner.internal.ir.identifierSymbol
import org.partiql.planner.internal.ir.partitionByAttrList
import org.partiql.planner.internal.ir.rex
import org.partiql.planner.internal.ir.rexOpCallStatic
import org.partiql.planner.internal.ir.rexOpCallUnresolved
import org.partiql.planner.internal.ir.rexOpLit
import org.partiql.planner.internal.ir.rexOpVarLocal
import org.partiql.planner.internal.ir.rexOpVarUnresolved
import org.partiql.planner.internal.ir.statementDDL
import org.partiql.planner.internal.ir.tableProperty
import org.partiql.planner.internal.ir.typeAtomicInt4
import org.partiql.planner.internal.ir.typeAtomicVarchar
import org.partiql.planner.internal.ir.typeCollection
import org.partiql.planner.internal.ir.typeRecord
import org.partiql.planner.internal.ir.typeRecordField
import org.partiql.planner.internal.typer.PlanTyper
import org.partiql.plugins.memory.MemoryCatalog
import org.partiql.plugins.memory.MemoryConnector
import org.partiql.spi.connector.ConnectorSession
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnParameter
import org.partiql.spi.fn.FnSignature
import org.partiql.types.BagType
import org.partiql.types.CollectionConstraint
import org.partiql.types.ListType
import org.partiql.types.NumberConstraint
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.int32Value
import org.partiql.value.stringValue
import java.util.Random
import org.partiql.planner.internal.ir.DdlOp as InternalDDLOp
import org.partiql.planner.internal.ir.Statement.DDL as InternalDDLNode

internal class DDLTestBase {
    data class TestCase(
        val description: String,
        val untyped: InternalDDLOp,
        val typed: InternalDDLOp?, // if this is not null, then constraint validation succeed.
        val resolved: InternalDDLNode?, // if this is not null, then constraint resolution succeed
        val publicPlan: Statement.DDL? // if this is not null, then feature gate succeed.
    ) {
        companion object {
            @OptIn(PartiQLValueExperimental::class)

            // For building success test case for entire planning process.
            fun success(
                description: String,
                op: InternalDDLOp.CreateTable,
                normalizedShape: Type.Collection,
                staticType: StaticType,
                partitionByAttrs: List<String> = emptyList(),
                tblProperties: Map<String, String> = emptyMap()
            ): TestCase {
                val typedOp = ddlOpCreateTable(
                    id(tableName), normalizedShape,
                    if (partitionByAttrs.isEmpty()) null else partitionByAttrList(partitionByAttrs.map { id(it) }),
                    tblProperties.map { tableProperty(it.key, stringValue(it.value)) }
                )
                val resolved = statementDDL(
                    staticType, typedOp
                )
                val publicPlan = org.partiql.plan.statementDDL(
                    org.partiql.plan.ddlOpCreateTable(
                        id(tableName).toPublicPlan(), staticType,
                        if (partitionByAttrs.isEmpty()) null else org.partiql.plan.partitionByAttrList(partitionByAttrs.map { id(it).toPublicPlan() }),
                        tblProperties.map { org.partiql.plan.tableProperty(it.key, stringValue(it.value)) }
                    )
                )
                return TestCase(
                    description = description,
                    untyped = op,
                    typed = typedOp,
                    resolved = resolved,
                    publicPlan = publicPlan
                )
            }

            // for building test cases that failed constraint validation
            fun failedValidation(description: String, op: InternalDDLOp.CreateTable) = TestCase(
                description = description,
                untyped = op,
                typed = null,
                resolved = null,
                publicPlan = null,
            )

            // for building test cases that failed constraint resolution
            @OptIn(PartiQLValueExperimental::class)
            fun failedResolution(
                description: String,
                op: InternalDDLOp.CreateTable,
                normalizedShape: Type.Collection,
                partitionByAttrs: List<String> = emptyList(),
                tblProperties: Map<String, String> = emptyMap()
            ) =
                TestCase(
                    description = description,
                    untyped = op,
                    typed = ddlOpCreateTable(
                        id(tableName), normalizedShape,
                        if (partitionByAttrs.isEmpty()) null else partitionByAttrList(partitionByAttrs.map { id(it) }),
                        tblProperties.map { tableProperty(it.key, stringValue(it.value)) }
                    ),
                    resolved = null,
                    publicPlan = null,
                )

            @OptIn(PartiQLValueExperimental::class)
            fun failedConversion(
                description: String,
                op: InternalDDLOp.CreateTable,
                normalizedShape: Type.Collection,
                staticType: StaticType,
                partitionByAttrs: List<String> = emptyList(),
                tblProperties: Map<String, String> = emptyMap()
            ): TestCase {
                val typedOp = ddlOpCreateTable(
                    id(tableName), normalizedShape,
                    if (partitionByAttrs.isEmpty()) null else partitionByAttrList(partitionByAttrs.map { id(it) }),
                    tblProperties.map { tableProperty(it.key, stringValue(it.value)) }
                )
                val resolved = statementDDL(
                    staticType, typedOp
                )
                return TestCase(
                    description = description,
                    untyped = op,
                    typed = typedOp,
                    resolved = resolved,
                    publicPlan = null
                )
            }
        }
    }

    companion object {

        val session = object : ConnectorSession {
            override fun getQueryId(): String = "Q"
            override fun getUserId(): String = "U"
        }

        val tableName = "tbl"

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

        val planner = PartiQLPlannerBuilder().build()

        // Convenient
        val FIELD_A_INT4 = Pair(
            Type.Record.Field(
                id("a"), typeAtomicInt4(),
                emptyList(),
                false, null,
            ),
            StructType.Field("A", StaticType.unionOf(StaticType.INT4, StaticType.NULL))
        )

        val FIELD_B_INT4 = Pair(
            Type.Record.Field(
                id("b"), typeAtomicInt4(),
                emptyList(),
                false, null,
            ),
            StructType.Field("B", StaticType.unionOf(StaticType.INT4, StaticType.NULL))
        )

        val FIELD_C_VARCHAR10 = Pair(
            Type.Record.Field(
                id("c"), typeAtomicVarchar(10),
                emptyList(),
                false, null,
            ),
            StructType.Field("C", StaticType.unionOf(StringType(StringType.StringLengthConstraint.Constrained(NumberConstraint.UpTo(10))), StaticType.NULL))
        )

        val COMMENT_META = mapOf("comment" to "this is a comment")

        @OptIn(PartiQLValueExperimental::class)
        val CONSTRA_A_LT_ZERO = Pair(
            checkConstraintUnresolved(
                null,
                rex(StaticType.ANY, rexOpVarUnresolved(id("a"), Rex.Op.Var.Scope.LOCAL)),
                rex(StaticType.INT4, rexOpLit(int32Value(0))),
                "a < 0"
            ),
            checkConstraintResolved(
                "\$_${tableName}_0", rex(StaticType.INT4, rexOpVarLocal(0, 0)), rex(StaticType.INT4, rexOpLit(int32Value(0))), "a < 0"
            )
        )

        @OptIn(PartiQLValueExperimental::class)
        val CONSTRA_B_LT_ZERO = Pair(
            checkConstraintUnresolved(
                null,
                rex(StaticType.ANY, rexOpVarUnresolved(id("b"), Rex.Op.Var.Scope.LOCAL)),
                rex(StaticType.INT4, rexOpLit(int32Value(0))),
                "b < 0"
            ),
            checkConstraintResolved(
                "\$_tbl_0", rex(StaticType.INT4, rexOpVarLocal(0, 0)), rex(StaticType.INT4, rexOpLit(int32Value(0))), "b < 0"
            )
        )

        @JvmStatic
        fun testCases() = listOf(
            TestCase.success(
                "CREATE TABLE tbl (a INT4)",
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(FIELD_A_INT4.first),
                    null,
                    emptyList()
                ),
                tableInternal(FIELD_A_INT4.first),
                table(FIELD_A_INT4.second),
            ),

            TestCase.success(
                "CREATE TABLE tbl (a INT4 PRIMARY KEY)",
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(FIELD_A_INT4.first.withConstraints(listOf(inlinePK(null)))),
                    null,
                    emptyList()
                ),
                tableInternal(
                    FIELD_A_INT4.first,
                    collectionConstraint = listOf(tuplePk("\$_tbl_0", listOf("a")))
                ),
                table(StructType.Field("A", StaticType.INT4), tableConstraint = setOf(CollectionConstraint.PrimaryKey(setOf("A")))),
            ),

            TestCase.success(
                "CREATE TABLE tbl (a OPTIONAL INT4)",
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(FIELD_A_INT4.first.asOptional()),
                    null,
                    emptyList()
                ),
                tableInternal(FIELD_A_INT4.first.asOptional()),
                table(StructType.Field("A", StaticType.INT4.asNullable().asOptional())),
            ),

            TestCase.failedResolution(
                """CREATE TABLE tbl (a OPTIONAL INT4 PRIMARY KEY) 
                    PRIMARY KEY cannot be optional 
                """.trimMargin(),
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(FIELD_A_INT4.first.withConstraints(listOf(inlinePK(null))).asOptional()),
                    null,
                    emptyList()
                ),
                tableInternal(
                    FIELD_A_INT4.first.asOptional(),
                    collectionConstraint = listOf(tuplePk("\$_tbl_0", listOf("a")))
                ),
            ),

            TestCase.success(
                "CREATE TABLE tbl(a INT4 NOT NULL)",
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(FIELD_A_INT4.first.withConstraints(listOf(nonNullConstraint(null)),)),
                    null,
                    emptyList()
                ),
                tableInternal(FIELD_A_INT4.first.withConstraints(listOf(nonNullConstraint("\$_${tableName}_0")),)),
                table(StructType.Field("A", StaticType.INT4))
            ),

            TestCase.success(
                "CREATE TABLE tbl(a OPTIONAL INT4 NOT NULL)",
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(FIELD_A_INT4.first.withConstraints(listOf(nonNullConstraint(null))).asOptional()),
                    null,
                    emptyList()
                ),
                tableInternal(FIELD_A_INT4.first.withConstraints(listOf(nonNullConstraint("\$_${tableName}_0"))).asOptional()),
                table(StructType.Field("A", StaticType.INT4.asOptional()))
            ),

            TestCase.success(
                "CREATE TABLE tbl (a INT4 COMMENT 'this is a comment')",
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(FIELD_A_INT4.first.withComment("this is a comment")),
                    null,
                    emptyList()
                ),
                tableInternal(FIELD_A_INT4.first.withComment("this is a comment")),
                table(FIELD_A_INT4.second.withMeta(COMMENT_META)),
            ),

            TestCase.failedConversion(
                """
                    CREATE TABLE tbl(
                       a INT4 CONSTRAINT a_not_null NOT NULL
                    )
                    The constraint name is not exposed to public plan
                """.trimIndent(),
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(FIELD_A_INT4.first.withConstraints(listOf(nonNullConstraint("a_not_null")),)),
                    null,
                    emptyList()
                ),
                tableInternal(FIELD_A_INT4.first.withConstraints(listOf(nonNullConstraint("a_not_null")),)),
                table(StructType.Field("A", StaticType.INT4))
            ),

            // Attribute Level check:
            TestCase.success(
                """
                    CREATE TABLE tbl(
                        a INT4 CHECK(a < 0)
                    )
                    Note that the CHECK Constraint is set to attribute level, 
                    but will be normalized to struct level.
                """.trimIndent(),
                ddlOpCreateTable(
                    id("tbl"),
                    tableInternal(
                        FIELD_A_INT4.first.withConstraints(listOf(CONSTRA_A_LT_ZERO.first))
                    ),
                    null,
                    emptyList()
                ),
                tableInternal(
                    FIELD_A_INT4.first,
                    structConstraints = listOf(
                        CONSTRA_A_LT_ZERO.second
                    ),
                ),
                table(
                    FIELD_A_INT4.second,
                    structMeta = mapOf("check_constraints" to ionStructOf(field("\$_${tableName}_0", ionString("a < 0"))),)
                )
            ),

            // Struct Level check:
            TestCase.success(
                """
                    CREATE TABLE tbl(
                        a INT4, 
                        CHECK(a < 0)
                    )
                    Note that the CHECK Constraint is set to tuple level
                """.trimIndent(),
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(
                        FIELD_A_INT4.first,
                        structConstraints = listOf(CONSTRA_A_LT_ZERO.first)
                    ),
                    null,
                    emptyList()
                ),
                tableInternal(
                    FIELD_A_INT4.first,
                    structConstraints = listOf(
                        CONSTRA_A_LT_ZERO.second
                    ),
                ),
                table(
                    FIELD_A_INT4.second,
                    structMeta = mapOf("check_constraints" to ionStructOf(field("\$_${tableName}_0", ionString("a < 0"))),)
                )
            ),

            TestCase.success(
                """
                    CREATE TABLE tbl (
                        a INT4,
                        b INT4,
                        CHECK(a < b) 
                    )
                    Note that the CHECK Constraint refers to multiple attribute in declared.
                """.trimIndent(),
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(
                        FIELD_A_INT4.first,
                        FIELD_B_INT4.first,
                        structConstraints = listOf(
                            checkConstraintUnresolved(
                                null,
                                rex(StaticType.ANY, rexOpVarUnresolved(id("a"), Rex.Op.Var.Scope.LOCAL)),
                                rex(StaticType.ANY, rexOpVarUnresolved(id("b"), Rex.Op.Var.Scope.LOCAL)),
                                "a < b"
                            )
                        )
                    ),
                    null,
                    emptyList()
                ),
                tableInternal(
                    FIELD_A_INT4.first,
                    FIELD_B_INT4.first,
                    structConstraints = listOf(
                        checkConstraintResolved(
                            "\$_${tableName}_0",
                            rex(StaticType.INT4, rexOpVarLocal(0, 0)),
                            rex(StaticType.INT4, rexOpVarLocal(0, 1)),
                            "a < b"
                        ),
                    )
                ),
                table(
                    FIELD_A_INT4.second,
                    FIELD_B_INT4.second,
                    structMeta = mapOf("check_constraints" to ionStructOf(field("\$_${tableName}_0", ionString("a < b"))),)
                )
            ),

            TestCase.success(
                """
                    CREATE TABLE tbl (
                        a INT4,
                        b INT4,
                        PRIMARY KEY(a, b)
                    )
                    Primary key as tuple level constraint
                """.trimIndent(),
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(
                        FIELD_A_INT4.first,
                        FIELD_B_INT4.first,
                        structConstraints = listOf(tuplePk(null, listOf("a", "b")))
                    ),
                    null,
                    emptyList()
                ),
                tableInternal(
                    FIELD_A_INT4.first,
                    FIELD_B_INT4.first,
                    collectionConstraint = listOf(tuplePk("\$_tbl_0", listOf("a", "b")))
                ),
                table(
                    StructType.Field("A", StaticType.INT4),
                    StructType.Field("B", StaticType.INT4),
                    tableConstraint = setOf(CollectionConstraint.PrimaryKey(setOf("A", "B")))
                )
            ),

            TestCase.failedValidation(
                """
                    CREATE TABLE tbl (
                        a INT4,
                        b INT4,
                        PRIMARY KEY(a, a)
                    )
                    Primary key contains duplicated attributes
                """.trimIndent(),
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(
                        FIELD_A_INT4.first,
                        FIELD_B_INT4.first,
                        structConstraints = listOf(tuplePk("\$_tbl_0", listOf("a", "a")))
                    ),
                    null,
                    emptyList()
                ),
            ),

            TestCase.failedValidation(
                """
                    CREATE TABLE tbl (
                        a INT4,
                        b INT4,
                        PRIMARY KEY(c)
                    )
                    Primary key contains non-existing attributes
                """.trimIndent(),
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(
                        FIELD_A_INT4.first,
                        FIELD_B_INT4.first,
                        structConstraints = listOf(tuplePk(null, listOf("c")))
                    ),
                    null,
                    emptyList()
                ),
            ),

            TestCase.failedValidation(
                """
                    CREATE TABLE tbl (
                        a INT4 CHECK(b < 0), 
                    )
                    Note that the check constraint refers to an attribute that is not the attribute being declared
                """.trimIndent(),
                ddlOpCreateTable(
                    id("tbl"),
                    tableInternal(
                        FIELD_A_INT4.first.withConstraints(listOf(CONSTRA_B_LT_ZERO.first))
                    ),
                    null,
                    emptyList()
                ),
            ),

            TestCase.failedValidation(
                """
                    CREATE TABLE tbl (
                        a INT4 CHECK(b < 0), 
                        b INT4,
                    )
                    Note that the check constraint refers to an attribute that is not the attribute being declared
                    Even though "b" is declared in "tbl", attribute level check constraint can only refer to 
                    the attribute being declared.
                """.trimIndent(),
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(
                        FIELD_A_INT4.first.withConstraints(listOf(CONSTRA_B_LT_ZERO.first)),
                        FIELD_B_INT4.first
                    ),
                    null,
                    emptyList()
                ),
            ),
            TestCase.failedResolution(
                """
                    CREATE TABLE tbl (
                        a INT4,
                        a INT4
                    )
                    Duplicated Binding at the same level
                """.trimIndent(),
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(FIELD_A_INT4.first, FIELD_A_INT4.first),
                    null,
                    emptyList()
                ),
                tableInternal(FIELD_A_INT4.first, FIELD_A_INT4.first),
            ),

            // NESTED:
            TestCase.success(
                """
                    CREATE TABLE tbl (
                        nested STRUCT <a : INT4>
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(
                        typeRecordField(
                            id("nested"),
                            typeRecord(
                                listOf(FIELD_A_INT4.first),
                                emptyList()
                            ),
                            emptyList(),
                            false,
                            null,
                        )
                    ),
                    null,
                    emptyList()
                ),
                tableInternal(
                    typeRecordField(
                        id("nested"),
                        typeRecord(
                            listOf(FIELD_A_INT4.first),
                            emptyList()
                        ),
                        emptyList(),
                        false,
                        null,
                    )
                ),
                table(
                    StructType.Field(
                        "NESTED",
                        StaticType.unionOf(
                            struct(FIELD_A_INT4.second), StaticType.NULL
                        )
                    )
                )
            ),

            TestCase.success(
                """
                    CREATE TABLE tbl (
                        nested STRUCT <a : INT4 NOT NULL>
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(
                        typeRecordField(
                            id("nested"),
                            typeRecord(
                                listOf(FIELD_A_INT4.first.withConstraints(listOf(nonNullConstraint(null)))),
                                emptyList()
                            ),
                            emptyList(),
                            false,
                            null,
                        )
                    ),
                    null,
                    emptyList()
                ),
                tableInternal(
                    typeRecordField(
                        id("nested"),
                        typeRecord(
                            listOf(FIELD_A_INT4.first.withConstraints(listOf(nonNullConstraint("\$_tbl_0")))),
                            emptyList()
                        ),
                        emptyList(),
                        false,
                        null,
                    )
                ),
                table(
                    StructType.Field(
                        "NESTED",
                        StaticType.unionOf(
                            struct(StructType.Field("A", StaticType.INT4)), StaticType.NULL
                        )
                    )
                )
            ),

            TestCase.success(
                """
                    CREATE TABLE tbl (
                        nested STRUCT <a OPTIONAL : INT4>
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(
                        typeRecordField(
                            id("nested"),
                            typeRecord(
                                listOf(FIELD_A_INT4.first.asOptional()),
                                emptyList()
                            ),
                            emptyList(),
                            false,
                            null,
                        )
                    ),
                    null,
                    emptyList()
                ),
                tableInternal(
                    typeRecordField(
                        id("nested"),
                        typeRecord(
                            listOf(FIELD_A_INT4.first.asOptional()),
                            emptyList()
                        ),
                        emptyList(),
                        false,
                        null,
                    )
                ),
                table(
                    StructType.Field(
                        "NESTED",
                        StaticType.unionOf(
                            struct(StructType.Field("A", StaticType.INT4.asNullable().asOptional())), StaticType.NULL
                        )
                    )
                )
            ),

            TestCase.success(
                """
                    CREATE TABLE tbl (
                        nested STRUCT <a : INT4 COMMENT 'this is a comment'>
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(
                        typeRecordField(
                            id("nested"),
                            typeRecord(
                                listOf(FIELD_A_INT4.first.withComment("this is a comment")),
                                emptyList()
                            ),
                            emptyList(),
                            false,
                            null,
                        )
                    ),
                    null,
                    emptyList()
                ),
                tableInternal(
                    typeRecordField(
                        id("nested"),
                        typeRecord(
                            listOf(FIELD_A_INT4.first.withComment("this is a comment")),
                            emptyList()
                        ),
                        emptyList(),
                        false,
                        null,
                    )
                ),
                table(
                    StructType.Field(
                        "NESTED",
                        struct(FIELD_A_INT4.second.withMeta(COMMENT_META)).asNullable()
                    )
                )
            ),

            TestCase.failedValidation(
                """
                    CREATE TABLE tbl (
                        nested STRUCT <a : INT4> PRIMARY KEY
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(
                        typeRecordField(
                            id("nested"),
                            typeRecord(
                                listOf(FIELD_A_INT4.first),
                                emptyList()
                            ),
                            listOf(inlinePK(null)),
                            false,
                            null,
                        )
                    ),
                    null,
                    emptyList()
                ),
            ),

            TestCase.success(
                """
                    CREATE TABLE tbl (
                        nested STRUCT <a : INT4>,
                        a INT4
                    )
                    We allow this as the two "a"s are in different scope
                """.trimIndent(),
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(
                        typeRecordField(
                            id("nested"),
                            typeRecord(
                                listOf(FIELD_A_INT4.first),
                                emptyList()
                            ),
                            emptyList(),
                            false,
                            null,
                        ),
                        FIELD_A_INT4.first
                    ),
                    null,
                    emptyList()
                ),
                tableInternal(
                    typeRecordField(
                        id("nested"),
                        typeRecord(
                            listOf(FIELD_A_INT4.first),
                            emptyList()
                        ),
                        emptyList(),
                        false,
                        null,
                    ),
                    FIELD_A_INT4.first
                ),
                table(
                    StructType.Field(
                        "NESTED",
                        StaticType.unionOf(
                            struct(FIELD_A_INT4.second), StaticType.NULL
                        )
                    ),
                    FIELD_A_INT4.second
                )
            ),
            TestCase.failedResolution(
                """
                    CREATE TABLE tbl (
                        nested STRUCT <
                                   a : INT4, 
                                   a : INT4,
                               >
                    )
                    Duplicated binding in nested scope
                """.trimIndent(),
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(
                        typeRecordField(
                            id("nested"),
                            typeRecord(
                                listOf(FIELD_A_INT4.first, FIELD_A_INT4.first),
                                emptyList()
                            ),
                            emptyList(),
                            false,
                            null,
                        )
                    ),
                    null,
                    emptyList()
                ),
                tableInternal(
                    typeRecordField(
                        id("nested"),
                        typeRecord(
                            listOf(FIELD_A_INT4.first, FIELD_A_INT4.first),
                            emptyList()
                        ),
                        emptyList(),
                        false,
                        null,
                    )
                ),
            ),

            // List
            TestCase.success(
                """
                    CREATE TABLE tbl (
                        a LIST<INT4> 
                    )
                """.trimIndent(),
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(
                        Type.Record.Field(
                            id("a"), typeCollection(typeAtomicInt4(), true, emptyList()),
                            emptyList(),
                            false, null,
                        ),
                        structConstraints = emptyList()
                    ),
                    null,
                    emptyList()
                ),
                tableInternal(
                    Type.Record.Field(
                        id("a"), typeCollection(typeAtomicInt4(), true, emptyList()),
                        emptyList(),
                        false, null,
                    ),
                    structConstraints = emptyList()
                ),
                table(
                    StructType.Field("A", ListType(StaticType.INT4.asNullable()).asNullable()),
                    structMeta = emptyMap()
                )
            ),

            TestCase.failedValidation(
                """
                    CREATE TABLE tbl (
                        a LIST<INT4> PRIMARY KEY
                    )
                    This should fail as currently we do not allow setting attribute whose type is collection as primary key
                """.trimIndent(),
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(
                        Type.Record.Field(
                            id("a"), typeCollection(typeAtomicInt4(), true, emptyList()),
                            listOf(inlinePK(null)),
                            false, null,
                        ),
                        structConstraints = emptyList()
                    ),
                    null,
                    emptyList()
                ),
            ),

            TestCase.failedConversion(
                """
                    CREATE TABLE tbl (
                        a LIST<LIST<INT4>> 
                    )
                    COLLECTION OF COLLECTION, this should failed the conversion. 
                """.trimIndent(),
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(
                        Type.Record.Field(
                            id("a"), typeCollection(typeCollection(typeAtomicInt4(), true, emptyList()), true, emptyList()),
                            emptyList(),
                            false, null,
                        ),
                        structConstraints = emptyList()
                    ),
                    null,
                    emptyList()
                ),
                tableInternal(
                    Type.Record.Field(
                        id("a"), typeCollection(typeCollection(typeAtomicInt4(), true, emptyList()), true, emptyList()),
                        emptyList(),
                        false, null,
                    ),
                    structConstraints = emptyList()
                ),
                table(
                    StructType.Field("A", ListType(ListType(StaticType.INT4.asNullable()).asNullable()).asNullable()),
                    structMeta = emptyMap()
                )
            ),

            // BAG Notice that the following does not have syntax support
            TestCase.failedConversion(
                """
                    CREATE TABLE tbl (
                       tbl2 BAG(STRUCT(a : INT4))
                    )
                    This syntax is for demonstration only, 
                    Purpose is to explore how we model create a bag(struct(bag(struct)))
                """.trimIndent(),
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(
                        typeRecordField(
                            id("tbl2"),
                            typeCollection(
                                typeRecord(
                                    listOf(FIELD_A_INT4.first),
                                    emptyList()
                                ),
                                false,
                                emptyList()
                            ),
                            emptyList(),
                            false,
                            null,
                        )
                    ),
                    null,
                    emptyList()
                ),
                tableInternal(
                    typeRecordField(
                        id("tbl2"),
                        typeCollection(
                            typeRecord(
                                listOf(FIELD_A_INT4.first),
                                emptyList()
                            ),
                            false,
                            emptyList()
                        ),
                        emptyList(),
                        false,
                        null,
                    )
                ),
                table(
                    StructType.Field(
                        "TBL2",
                        BagType(
                            struct(FIELD_A_INT4.second).asNullable()
                        ).asNullable()
                    ),
                )
            ),

            TestCase.failedConversion(
                """
                    CREATE TABLE tbl (
                       tbl2 BAG(STRUCT(a : INT4 PRIMARY KEY))
                    )
                    This syntax is for demonstration only, 
                    Purpose is to explore how we model create a bag(struct(bag(struct)))
                    The interesting part is: could we attempts to set up a primary key or other collection constraint associated with the inner bag? 
                    How do we model this?
                """.trimIndent(),
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(
                        typeRecordField(
                            id("tbl2"),
                            typeCollection(
                                typeRecord(
                                    listOf(FIELD_A_INT4.first.withConstraints(listOf(inlinePK(null)))),
                                    emptyList()
                                ),
                                false,
                                emptyList()
                            ),
                            emptyList(),
                            false,
                            null,
                        )
                    ),
                    null,
                    emptyList()
                ),
                tableInternal(
                    typeRecordField(
                        id("tbl2"),
                        typeCollection(
                            typeRecord(
                                listOf(FIELD_A_INT4.first),
                                emptyList()
                            ),
                            false,
                            listOf(tuplePk("\$_tbl_0", listOf("a")))
                        ),
                        emptyList(),
                        false,
                        null,
                    )
                ),
                table(
                    StructType.Field(
                        "TBL2",
                        BagType(
                            // notice hre the struct itself is not nullable.
                            struct(StructType.Field("A", StaticType.INT4)),
                            emptyMap(),
                            setOf(CollectionConstraint.PrimaryKey(setOf("A")))
                        ).asNullable()
                    ),
                )
            ),

            // Partition Key
            TestCase.success(
                "CREATE TABLE tbl (a INT4) PARTITION BY (a)",
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(FIELD_A_INT4.first),
                    partitionByAttrList(listOf(id("a"))),
                    emptyList()
                ),
                tableInternal(FIELD_A_INT4.first),
                table(FIELD_A_INT4.second),
                listOf("a")
            ),

            TestCase.failedValidation(
                "CREATE TABLE tbl (a INT4) PARTITION BY (b)",
                ddlOpCreateTable(
                    id(tableName),
                    tableInternal(FIELD_A_INT4.first),
                    partitionByAttrList(listOf(id("b"))),
                    emptyList()
                ),
            ),

        )

        private fun struct(vararg fields: StructType.Field, structMeta: Map<String, Any> = emptyMap()) =
            StructType(
                fields = fields.toList(),
                contentClosed = true,
                primaryKeyFields = emptyList(),
                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true)),
                metas = structMeta
            )

        private fun table(vararg fields: StructType.Field, structMeta: Map<String, Any> = emptyMap(), tableConstraint: Set<CollectionConstraint> = emptySet()) = BagType(
            struct(*fields, structMeta = structMeta),
            metas = emptyMap(),
            tableConstraint
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

        private fun checkConstraintUnresolved(name: String?, lhs: Rex, rhs: Rex, sql: String) = constraint(
            name,
            constraintDefinitionCheck(
                rex(
                    StaticType.ANY,
                    rexOpCallUnresolved(
                        id("lt"),
                        listOf(lhs, rhs),
                    )
                ),
                sql,
            )
        )

        private fun checkConstraintResolved(name: String?, lhs: Rex, rhs: Rex, sql: String) = constraint(
            name,
            constraintDefinitionCheck(
                rex(
                    StaticType.BOOL,
                    lessThanRexOpResolved(lhs, rhs)
                ),
                sql,
            )
        )

        @OptIn(FnExperimental::class, PartiQLValueExperimental::class)
        private fun lessThanRexOpResolved(lhs: Rex, rhs: Rex) = rexOpCallStatic(
            Ref.Fn(
                "test",
                listOf("LT"),
                FnSignature(
                    "lt",
                    PartiQLValueType.BOOL,
                    listOf(
                        FnParameter("lhs", PartiQLValueType.INT32),
                        FnParameter("rhs", PartiQLValueType.INT32)
                    ),
                    isDeterministic = true,
                    isNullable = false,
                    isNullCall = true,
                    isMissable = true,
                    isMissingCall = true
                )
            ),
            listOf(lhs, rhs)
        )

        private fun Identifier.Symbol.toPublicPlan() =
            when (this.caseSensitivity) {
                Identifier.CaseSensitivity.SENSITIVE -> org.partiql.plan.identifierSymbol(this.symbol, org.partiql.plan.Identifier.CaseSensitivity.SENSITIVE)
                Identifier.CaseSensitivity.INSENSITIVE -> org.partiql.plan.identifierSymbol(this.symbol, org.partiql.plan.Identifier.CaseSensitivity.INSENSITIVE)
            }

        private fun Type.Record.Field.withConstraints(constraints: List<Constraint>) =
            this.copy(name, type, constraints, isOptional, comment)

        private fun Type.Record.Field.asOptional() = this.copy(name, type, constraints, true, comment)

        private fun Type.Record.Field.withComment(comment: String) = this.copy(name, type, constraints, isOptional, comment)

        private fun StructType.Field.withMeta(meta: Map<String, Any>) = StructType.Field(this.key, this.value, meta)

        private fun inlinePK(name: String?) = constraint(name, constraintDefinitionUnique(emptyList(), true))

        private fun tuplePk(name: String?, attrs: List<String>) = constraint(name, constraintDefinitionUnique(attrs.map { id(it) }, true))
    }
}
