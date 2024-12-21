package org.partiql.planner.ddl

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Action
import org.partiql.plan.Plan
import org.partiql.planner.PartiQLPlanner
import org.partiql.planner.internal.TestCatalog
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table
import org.partiql.spi.errors.PErrorException
import org.partiql.types.Field
import org.partiql.types.PType
import org.partiql.types.shape.PShape
import java.math.BigDecimal

class DDLTest {

    val parser = PartiQLParser.builder().build()
    val planner = PartiQLPlanner.standard()
    val session = Session.builder()
        .catalog("default")
        .catalogs(
            TestCatalog.builder()
                .name("default")
                .build()
        )
        .namespace("SCHEMA")
        .build()
    fun plan(ddl: String): Plan {
        val statement = parser.parse(ddl).statements.first()
        return planner.plan(statement, session).plan
    }

    private fun tableAssertionAndReturnFields(
        table: Table,
        tableName: String,
        primaryKey: List<String> = emptyList(),
        unique: List<String> = emptyList(),
        metadata: Map<String, String> = emptyMap(),
    ): List<Field> {
        Assertions.assertEquals(tableName, table.getName().getName())
        val schema = table.getSchema() as? PShape
            ?: throw AssertionError("Expect Schema to be a PShape")
        schema as? PShape ?: throw AssertionError("Expect Schema to be a PShape")
        Assertions.assertEquals(PType.BAG, schema.code()) {
            "Expect Schema to be a Bag Type"
        }
        if (primaryKey.isNotEmpty()) {
            Assertions.assertTrue(primaryKey.containsAll(schema.primaryKey()))
            Assertions.assertTrue(schema.primaryKey().containsAll(primaryKey))
        }
        if (unique.isNotEmpty()) {
            Assertions.assertTrue(unique.containsAll(schema.unique()))
            Assertions.assertTrue(schema.unique().containsAll(unique))
        }
        val struct = schema.typeParameter
        Assertions.assertEquals(PType.ROW, struct.code())
        return struct.fields.toList()
    }

    private fun assertField(
        field: Field,
        fieldName: String,
        code: Int,
        isNullable: Boolean,
        isOptional: Boolean,
        maxValue: Number? = null,
        minValue: Number? = null,
        precision: Int? = null,
        scale: Int? = null,
        length: Int? = null,
        meta: Map<String, String> = emptyMap(),
    ) {
        Assertions.assertEquals(fieldName, field.name)
        val shape = field.type as PShape
        Assertions.assertEquals(code, shape.code())
        Assertions.assertEquals(isOptional, shape.isOptional)
        Assertions.assertEquals(isNullable, shape.isNullable)
        if (maxValue != null) {
            Assertions.assertTrue(BigDecimal(maxValue.toString()).compareTo(BigDecimal(shape.maxValue().toString())) == 0) {
                """
                    Expected maxValue to be $maxValue but was: ${shape.maxValue()}
                """.trimIndent()
            }
        }
        if (minValue != null) {
            Assertions.assertTrue(BigDecimal(minValue.toString()).compareTo(BigDecimal(shape.minValue().toString())) == 0) {
                """
                    Expected minValue to be $minValue but was: ${shape.minValue()}
                """.trimIndent()
            }
        }
        if (precision != null) {
            Assertions.assertEquals(precision, shape.precision)
        }
        if (scale != null) {
            Assertions.assertEquals(scale, shape.scale)
        }
        if (length != null) {
            Assertions.assertEquals(length, shape.length)
        }
        Assertions.assertEquals(meta, shape.meta())
    }

    private fun assertInt2NullableOptional(field: Field, name: String, nullable: Boolean, optional: Boolean) {
        assertField(
            field,
            name,
            PType.SMALLINT,
            nullable, optional,
            Short.MAX_VALUE, Short.MIN_VALUE
        )
    }

    @Test
    fun createTableBasicTest() {
        val ddl = """
            CREATE TABLE foo (
               int2_attr INT2,
               int4_attr INT4,
               int8_attr INT8,
               decimal_attr DECIMAL(10,5),
               float_attr REAL,
               char_attr CHAR(1),
               varchar_attr VARCHAR(255),
               timestamp_attr TIMESTAMP(2),
               date_attr DATE
            );
        """.trimIndent()
        val plan = plan(ddl)
        val createTable = plan.action as Action.CreateTable
        val fields = tableAssertionAndReturnFields(
            createTable.table,
            "foo"
        )
        val int2_attr = fields[0]
        assertField(
            int2_attr,
            "int2_attr",
            PType.SMALLINT,
            true, false,
            Short.MAX_VALUE, Short.MIN_VALUE
        )
        val int4_attr = fields[1]
        assertField(
            int4_attr,
            "int4_attr",
            PType.INTEGER,
            true, false,
            Int.MAX_VALUE, Int.MIN_VALUE
        )
        val int8_attr = fields[2]
        assertField(
            int8_attr,
            "int8_attr",
            PType.BIGINT,
            true, false,
            Long.MAX_VALUE, Long.MIN_VALUE
        )
        val decimal_attr = fields[3]
        assertField(
            decimal_attr,
            "decimal_attr",
            PType.DECIMAL,
            true, false,
            precision = 10, scale = 5,
        )
        val float_attr = fields[4]
        assertField(
            float_attr,
            "float_attr",
            PType.REAL,
            true, false,
        )
        val char_attr = fields[5]
        assertField(
            char_attr,
            "char_attr",
            PType.CHAR,
            true, false,
            length = 1
        )
        val varchar_attr = fields[6]
        assertField(
            varchar_attr,
            "varchar_attr",
            PType.VARCHAR,
            true, false,
            length = 255
        )
        val timestamp_attr = fields[7]
        assertField(
            timestamp_attr,
            "timestamp_attr",
            PType.TIMESTAMP,
            true, false,
            precision = 2
        )
        val date_attr = fields[8]
        assertField(
            date_attr,
            "date_attr",
            PType.DATE,
            true, false,
        )
    }

    @Test
    fun createTableStructTest() {
        val ddl = """
            CREATE TABLE foo (
               struct_attr STRUCT<
                   int2_attr: INT2,
                   int4_attr: INT4,
                   int8_attr: INT8,
                   decimal_attr: DECIMAL(10,5),
                   float_attr: REAL,
                   char_attr: CHAR(1),
                   varchar_attr: VARCHAR(255),
                   timestamp_attr: TIMESTAMP(2),
                   date_attr: DATE
               >
            );
        """.trimIndent()
        val plan = plan(ddl)
        val createTable = plan.action as Action.CreateTable
        val fields = tableAssertionAndReturnFields(
            createTable.table,
            "foo"
        )
        val struct_attr = fields[0]
        assertField(struct_attr, "struct_attr", PType.ROW, true, false )
        val structFields = struct_attr.type.fields.toList()

        val int2_attr = structFields[0]
        assertField(
            int2_attr,
            "int2_attr",
            PType.SMALLINT,
            true, false,
            Short.MAX_VALUE, Short.MIN_VALUE
        )
        val int4_attr = structFields[1]
        assertField(
            int4_attr,
            "int4_attr",
            PType.INTEGER,
            true, false,
            Int.MAX_VALUE, Int.MIN_VALUE
        )
        val int8_attr = structFields[2]
        assertField(
            int8_attr,
            "int8_attr",
            PType.BIGINT,
            true, false,
            Long.MAX_VALUE, Long.MIN_VALUE
        )
        val decimal_attr = structFields[3]
        assertField(
            decimal_attr,
            "decimal_attr",
            PType.DECIMAL,
            true, false,
            precision = 10, scale = 5,
        )
        val float_attr = structFields[4]
        assertField(
            float_attr,
            "float_attr",
            PType.REAL,
            true, false,
        )
        val char_attr = structFields[5]
        assertField(
            char_attr,
            "char_attr",
            PType.CHAR,
            true, false,
            length = 1
        )
        val varchar_attr = structFields[6]
        assertField(
            varchar_attr,
            "varchar_attr",
            PType.VARCHAR,
            true, false,
            length = 255
        )
        val timestamp_attr = structFields[7]
        assertField(
            timestamp_attr,
            "timestamp_attr",
            PType.TIMESTAMP,
            true, false,
            precision = 2
        )
        val date_attr = structFields[8]
        assertField(
            date_attr,
            "date_attr",
            PType.DATE,
            true, false,
        )
    }

    @Test
    fun createTableNotNullTest() {
        val ddl = """
            CREATE TABLE foo (
               attr1 INT2 NOT NULL,
               attr2 INT2 NOT NULL NULL,
               attr3 INT2 NOT NULL NULL NOT NULL,
               attr4 INT2 NULL NOT NULL
            );
        """.trimIndent()

        val plan = plan(ddl)
        val createTable = plan.action as Action.CreateTable
        val fields = tableAssertionAndReturnFields(
            createTable.table,
            "foo"
        )

        val attr1 = fields[0]
        assertInt2NullableOptional(attr1, "attr1", false, false)
        val attr2 = fields[1]
        assertInt2NullableOptional(attr2, "attr2", true, false)
        val attr3 = fields[2]
        assertInt2NullableOptional(attr3, "attr3", false, false)
        val attr4 = fields[3]
        assertInt2NullableOptional(attr4, "attr4", false, false)
    }

    @Test
    fun createTableOptionalTest() {
        val ddl = """
            CREATE TABLE foo (
               attr1 OPTIONAL INT2,
               attr2 OPTIONAL INT2 NOT NULL
            );
        """.trimIndent()
        val plan = plan(ddl)
        val createTable = plan.action as Action.CreateTable
        val fields = tableAssertionAndReturnFields(
            createTable.table,
            "foo"
        )

        val attr1 = fields[0]
        assertInt2NullableOptional(attr1, "attr1", true, true)
        val attr2 = fields[1]
        assertInt2NullableOptional(attr2, "attr2", false, true)
    }

    @Test
    fun createTableCommentTest() {
        val ddl = """
            CREATE TABLE foo (
               attr1 INT2 COMMENT 'attr1'
            );
        """.trimIndent()
        val plan = plan(ddl)
        val createTable = plan.action as Action.CreateTable
        val fields = tableAssertionAndReturnFields(
            createTable.table,
            "foo"
        )

        val attr1 = fields[0]
        assertField(
            attr1,
            "attr1",
            PType.SMALLINT,
            true, false,
            Short.MAX_VALUE, Short.MIN_VALUE,
            meta = mapOf("comment" to "attr1")
        )
    }

    @Test
    fun createTableCheckConstraintLoweredTest() {
        val ddl = """
            CREATE TABLE foo (
               attr1 INT2 CHECK(attr1 >= 0),
               attr2 INT2 CHECK(attr2 >=0 and attr2 <= 10),
               attr3 INT2 CHECK(attr3 >= 0) CHECK (attr3 <= 10),
               -- Leading to a empty value set
               attr4 INT2 CHECK(attr4 >= 1000000) 
            );
        """.trimIndent()
        val plan = plan(ddl)
        val createTable = plan.action as Action.CreateTable
        val fields = tableAssertionAndReturnFields(
            createTable.table,
            "foo"
        )

        val attr1 = fields[0]
        assertField(
            attr1,
            "attr1",
            PType.SMALLINT,
            true, false,
            Short.MAX_VALUE, 0,
        )

        val attr2 = fields[1]
        assertField(
            attr2,
            "attr2",
            PType.SMALLINT,
            true, false,
            10, 0,
        )

        val attr3 = fields[2]
        assertField(
            attr3,
            "attr3",
            PType.SMALLINT,
            true, false,
            10, 0,
        )

        val attr4 = fields[3]
        assertField(
            attr4,
            "attr4",
            PType.SMALLINT,
            true, false,
            Short.MAX_VALUE, 1000000,
        )
    }

    @Test
    fun createTablePrimaryKeyInlineTest() {
        val ddl = """
            CREATE TABLE foo (
               attr1 INT2 PRIMARY KEY
            );
        """.trimIndent()
        val plan = plan(ddl)
        val createTable = plan.action as Action.CreateTable
        val fields = tableAssertionAndReturnFields(
            createTable.table,
            "foo",
            listOf("attr1")
        )

        // Side effect: Nullable is false
        val attr1 = fields[0]
        assertField(
            attr1,
            "attr1",
            PType.SMALLINT,
            false, false,
            Short.MAX_VALUE, Short.MIN_VALUE,
        )
    }

    @Test
    fun createTablePrimaryKeyTableTest() {
        val ddl = """
            CREATE TABLE foo (
               attr1 INT2,
               -- case insensitive
               PRIMARY KEY (ATTR1)
            );
        """.trimIndent()
        val plan = plan(ddl)
        val createTable = plan.action as Action.CreateTable
        val fields = tableAssertionAndReturnFields(
            createTable.table,
            "foo",
            listOf("attr1")
        )

        // Side effect: Nullable is false
        val attr1 = fields[0]
        assertField(
            attr1,
            "attr1",
            PType.SMALLINT,
            false, false,
            Short.MAX_VALUE, Short.MIN_VALUE,
        )
    }

    @Test
    fun createTableUniqueKey() {
        val ddl = """
            CREATE TABLE foo (
               attr1 INT2 NOT NULL,
               attr2 INT2 UNIQUE,
               attr3 INT2 PRIMARY KEY, 
               attr4 INT2,
               -- Duplicated declaration
               UNIQUE (ATTR2),
               UNIQUE (attr1),
               UNIQUE (attr1, attr4)
            );
        """.trimIndent()
        val plan = plan(ddl)
        val createTable = plan.action as Action.CreateTable
        tableAssertionAndReturnFields(
            createTable.table,
            "foo",
            listOf("attr3"),
            // Side effect: attr3 is primary key, therefore it is unique
            listOf("attr1", "attr2", "attr3", "attr4")
        )
    }

    @Test
    fun createTableMetadata() {
        val ddl = """
            CREATE TABLE foo (
               attr1 INT2
            ) 
            TBLPROPERTIES('key' = 'value')
            PARTITION BY (attr1);
        """.trimIndent()
        val plan = plan(ddl)
        val createTable = plan.action as Action.CreateTable
        tableAssertionAndReturnFields(
            createTable.table,
            "foo",
            metadata = mapOf("key" to "value", "partition" to "[attr1]")
        )
    }

    @Test
    fun negative_createTable_primaryKey_1() {
        val ddl = """
            CREATE TABLE foo (
               attr1 INT2 PRIMARY KEY,
               attr2 INT2 PRIMARY KEY,
            );
        """.trimIndent()
        assertThrows<PErrorException> {
            plan(ddl)
        }
    }

    @Test
    fun negative_createTable_primaryKey_2() {
        val ddl = """
            CREATE TABLE foo (
               attr1 INT2 PRIMARY KEY,
               attr2 INT2,
               PRIMARY KEY(attr2)
            );
        """.trimIndent()
        assertThrows<PErrorException> {
            plan(ddl)
        }
    }

    @Test
    fun negative_createTable_primaryKey_3() {
        val ddl = """
            CREATE TABLE foo (
               attr1 INT2,
               attr2 INT2,
               PRIMARY KEY(attr3)
            );
        """.trimIndent()
        assertThrows<PErrorException> {
            plan(ddl)
        }
    }

    @Test
    fun negative_createTable_primaryKey_4() {
        val ddl = """
            CREATE TABLE foo (
               "attr1" INT2,
               attr2 INT2,
               PRIMARY KEY("ATTR1")
            );
        """.trimIndent()
        assertThrows<PErrorException> {
            plan(ddl)
        }
    }

    @Test
    fun negative_createTable_primaryKey_5() {
        val ddl = """
            CREATE TABLE foo (
               attr1 INT2,
               PRIMARY KEY(attr1, attr1)
            );
        """.trimIndent()
        assertThrows<PErrorException> {
            plan(ddl)
        }
    }

    @Test
    fun negative_createTable_primaryKey_6() {
        val ddl = """
            CREATE TABLE foo (
               attr1 OPTIONAL INT2,
               PRIMARY KEY(attr1)
            );
        """.trimIndent()
        assertThrows<PErrorException> {
            plan(ddl)
        }
    }

    @Test
    fun negative_createTable_primaryKey_7() {
        val ddl = """
            CREATE TABLE foo (
               attr1 ARRAY<INT2>,
               PRIMARY KEY(attr1)
            );
        """.trimIndent()
    }
}
