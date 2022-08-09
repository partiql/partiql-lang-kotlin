package org.partiql.lang.mappers

import com.amazon.ionelement.api.ionBool
import com.amazon.ionelement.api.ionInt
import com.amazon.ionelement.api.loadAllElements
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.partiql.ionschema.model.IonSchemaModel
import org.partiql.ionschema.model.toIsl
import org.partiql.ionschema.parser.parseSchema
import org.partiql.lang.ots.plugins.standard.types.CompileTimeFloatType
import org.partiql.lang.ots.plugins.standard.types.DecimalType
import org.partiql.lang.types.AnyOfType
import org.partiql.lang.types.AnyType
import org.partiql.lang.types.BagType
import org.partiql.lang.types.BlobType
import org.partiql.lang.types.BoolType
import org.partiql.lang.types.CharType
import org.partiql.lang.types.ClobType
import org.partiql.lang.types.Int4Type
import org.partiql.lang.types.Int8Type
import org.partiql.lang.types.IntType
import org.partiql.lang.types.ListType
import org.partiql.lang.types.SexpType
import org.partiql.lang.types.StaticScalarType
import org.partiql.lang.types.StaticType
import org.partiql.lang.types.StringType
import org.partiql.lang.types.StructType
import org.partiql.lang.types.SymbolType
import org.partiql.lang.types.TimestampType
import org.partiql.lang.types.VarcharType
import org.partiql.pig.runtime.toIonElement

internal fun buildTypeDef(name: String? = null, vararg constraints: IonSchemaModel.Constraint) =
    IonSchemaModel.build { typeDefinition(name, constraintList(constraints.toList())) }

internal fun buildTypeConstraint(name: String, nullable: Boolean = false) =
    IonSchemaModel.build { typeConstraint(namedType(name, ionBool(nullable))) }

private const val islHeader = "schema_header::{imports: [{ id: \"partiql.isl\" }]}"
private const val islFooter = "schema_footer::{}"
private const val typeName = "foo"
private const val unavailableType = "unavailable_type"

internal class MapperE2ETestCase(
    val sourceIsl: String,
    val expectedStaticType: StaticType,
    val expectedIsl: String = sourceIsl
) {
    override fun toString(): String = "${sourceIsl.trimIndent()} -> ${expectedIsl.trimIndent()}"
}

internal class E2EMapperTests {

    @ParameterizedTest
    @MethodSource("parametersForE2ETests")
    fun tests(tc: MapperE2ETestCase) {
        val sourceIsl = tc.sourceIsl
        val staticType = tc.expectedStaticType
        val expectedIsl = tc.expectedIsl

        verifyAssertions(sourceIsl, staticType)
        verifyAssertions(staticType, expectedIsl)
    }

    private fun verifyAssertions(sourceIsl: String, expectedType: StaticType) {
        // Create ISL domain model from raw isl
        val schema = parseSchema(loadAllElements(sourceIsl).toList())

        // Convert to StaticType
        val actualType = StaticTypeMapper(schema).toStaticType(typeName)

        // Create expected type with metas - if the test already provides metas, use them
        val expectedTypeWithMetas = if (expectedType.metas.containsKey(ISL_META_KEY)) {
            expectedType
        } else {
            expectedType.withMetas(
                mapOf(
                    ISL_META_KEY to schema.statements
                        .filterIsInstance<IonSchemaModel.SchemaStatement.TypeStatement>()
                        .map { it.typeDef }
                )
            )
        }

        // Assert StaticType is as expected
        // Throwing AssertionError in order to print a more readable, multi-line message
        // instead of a single-line message that [assertEquals] displays
        if (expectedTypeWithMetas != actualType) {
            throw AssertionError(
                """
                StaticType must match the expected.
                Expected: $expectedTypeWithMetas
                Actual: $actualType
                """.trimIndent()
            )
        }
    }

    private fun verifyAssertions(staticType: StaticType, expectedIsl: String) {
        // Create ISL domain model from input ISL
        val expectedSchema = parseSchema(loadAllElements(islHeader + expectedIsl + islFooter).toList())

        // Map StaticType to ISL domain model
        val actualSchema = IonSchemaMapper(staticType).toIonSchema(typeName)

        // Ensure domain model is as expected. This assertion checks for semantic equivalence.
        // Throwing AssertionError in order to print a more readable, multi-line message
        // instead of a single-line message that [assertEquals] displays
        if (expectedSchema != actualSchema) {
            throw AssertionError(
                """
                Parsed object model must match the expected.
                Expected ISL: ${expectedSchema.toIsl()}
                Actual ISL: ${actualSchema.toIsl()}
                
                Expected schema: $expectedSchema
                Actual schema: $actualSchema
                """.trimIndent()
            )
        }
    }

    companion object {
        @JvmStatic
        fun parametersForE2ETests() = basicSingleTypeTests() +
            basicAnyOfTests() +
            listTests() +
            sexpTests() +
            bagTests() +
            structTests() +
            stringTests() +
            intTests() +
            decimalTests() +
            bagWithCustomElementTests() +
            structWithCustomFieldTests()
    }

    @Test
    fun `field of MissingType should be excluded from ISL`() {
        verifyAssertions(
            staticType = StructType(
                mapOf(
                    "a" to StaticType.MISSING
                )
            ),
            expectedIsl = "type::{ name: $typeName, type: struct, fields: {} }"
        )
    }

    @Test
    fun `field of AnyType should return field as optional and nullable in ISL`() {
        verifyAssertions(
            staticType = StructType(
                mapOf(
                    "a" to StaticType.ANY
                )
            ),
            expectedIsl = "type::{ name: $typeName, type: struct, fields: { a: nullable::any } }"
        )
    }

    @Test
    fun `verify ISL can be created without StaticType metas too`() {
        verifyAssertions(
            staticType = ListType(StaticType.STRING),
            expectedIsl = "type::{ name: $typeName, type: list, element: string }"
        )
    }

    @Test
    fun `type to be mapped does not exist in schema`() {
        val isl = "type::{ name: $typeName, type: string }"
        val schema = parseSchema(loadAllElements(isl).toList())
        assertTypeNotFoundException(schema, unavailableType)
    }

    @Test
    fun `referenced top level type does not exist in schema`() {
        val isl = "type::{ name: $typeName, type: $unavailableType }"
        val schema = parseSchema(loadAllElements(isl).toList())
        assertTypeNotFoundException(schema, typeName)
    }

    private fun assertTypeNotFoundException(schema: IonSchemaModel.Schema, typeName: String) {
        val exception = assertThrows(TypeNotFoundException::class.java) {
            StaticTypeMapper(schema).toStaticType(typeName)
        }
        assertEquals("Type not found : $unavailableType", exception.message)
    }
}

internal fun basicSingleTypeTests() = listOf(
    // null
    MapperE2ETestCase(
        "type::{ name: $typeName, type: \$null }",
        StaticType.NULL,
        "type::{ name: $typeName, type: nullable::\$null }"
    ),
    // implicit any
    // https://github.com/partiql/partiql-lang-kotlin/issues/514
    MapperE2ETestCase(
        "type::{ name: $typeName }",
        StaticType.ANY,
        "type::{ name: $typeName, type: nullable::any }"
    ),
    // explicit any
    // FIXME: Going from Ion any (which doesn't include null) to PartiQL any (which does) and back to Ion
    //   adds nullability to a type even-though it originally wasn't nullable.
    //   https://github.com/partiql/partiql-lang-kotlin/issues/514
    MapperE2ETestCase(
        "type::{ name: $typeName, type: any}",
        StaticType.ANY,
        "type::{ name: $typeName, type: nullable::any }"
    ),
    // nullable any
    MapperE2ETestCase(
        "type::{ name: $typeName, type: nullable::any }",
        StaticType.ANY
    ),
    // bool
    MapperE2ETestCase(
        "type::{ name: $typeName, type: bool }",
        StaticType.BOOL
    ),
    // nullable bool
    MapperE2ETestCase(
        "type::{ name: $typeName, type: nullable::bool }",
        AnyOfType(setOf(StaticType.NULL, StaticType.BOOL))
    ),
    // int
    MapperE2ETestCase(
        "type::{ name: $typeName, type: int }",
        StaticType.INT
    ),
    // nullable int
    MapperE2ETestCase(
        "type::{ name: $typeName, type: nullable::int }",
        AnyOfType(setOf(StaticType.NULL, StaticType.INT))
    ),
    // float
    MapperE2ETestCase(
        "type::{ name: $typeName, type: float }",
        StaticType.FLOAT
    ),
    // nullable float
    MapperE2ETestCase(
        "type::{ name: $typeName, type: nullable::float }",
        AnyOfType(setOf(StaticType.NULL, StaticType.FLOAT))
    ),
    // decimal
    MapperE2ETestCase(
        "type::{ name: $typeName, type: decimal }",
        StaticType.DECIMAL
    ),
    // nullable decimal
    MapperE2ETestCase(
        "type::{ name: $typeName, type: nullable::decimal }",
        AnyOfType(setOf(StaticType.NULL, StaticType.DECIMAL))
    ),
    // timestamp
    MapperE2ETestCase(
        "type::{ name: $typeName, type: timestamp }",
        StaticType.TIMESTAMP
    ),
    // nullable timestamp
    MapperE2ETestCase(
        "type::{ name: $typeName, type: nullable::timestamp }",
        AnyOfType(setOf(StaticType.NULL, StaticType.TIMESTAMP))
    ),
    // symbol
    MapperE2ETestCase(
        "type::{ name: $typeName, type: symbol }",
        StaticType.SYMBOL
    ),
    // nullable symbol
    MapperE2ETestCase(
        "type::{ name: $typeName, type: nullable::symbol }",
        AnyOfType(setOf(StaticType.NULL, StaticType.SYMBOL))
    ),
    // string
    MapperE2ETestCase(
        "type::{ name: $typeName, type: string }",
        StaticType.STRING
    ),
    // nullable string
    MapperE2ETestCase(
        "type::{ name: $typeName, type: nullable::string }",
        AnyOfType(setOf(StaticType.NULL, StaticType.STRING))
    ),
    // Same ISL as above though StaticType was created from "type::{ name: $typeName, type: nullable::{ type: string }}"
    MapperE2ETestCase(
        "type::{ name: $typeName, type: nullable::{ type: string }}",
        StaticType.unionOf(
            StringType(
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(null, buildTypeConstraint("string"))
                    )
                )
            ),
            StaticType.NULL,
            metas = mapOf(
                ISL_META_KEY to listOf(
                    buildTypeDef(
                        typeName,
                        IonSchemaModel.build {
                            typeConstraint(
                                inlineType(buildTypeDef(null, buildTypeConstraint("string")), ionBool(true))
                            )
                        }
                    )
                )
            )
        ),
        "type::{ name: $typeName, type: nullable::string }"
    ),
    // symbol type with codepoint_length constraint
    MapperE2ETestCase(
        "type::{ name: $typeName, type: symbol, codepoint_length: 5 }",
        SymbolType(
            metas = mapOf(
                ISL_META_KEY to listOf(
                    buildTypeDef(typeName, buildTypeConstraint("symbol"), IonSchemaModel.build { codepointLength(equalsNumber(ionInt(5))) })
                )
            )
        )
    ),
    // clob
    MapperE2ETestCase(
        "type::{ name: $typeName, type: clob }",
        StaticType.CLOB
    ),
    // nullable clob
    MapperE2ETestCase(
        "type::{ name: $typeName, type: nullable::clob }",
        AnyOfType(setOf(StaticType.NULL, StaticType.CLOB))
    ),
    // blob
    MapperE2ETestCase(
        "type::{ name: $typeName, type: blob }",
        StaticType.BLOB
    ),
    // nullable blob
    MapperE2ETestCase(
        "type::{ name: $typeName, type: nullable::blob }",
        AnyOfType(setOf(StaticType.NULL, StaticType.BLOB))
    ),
    // list
    MapperE2ETestCase(
        "type::{ name: $typeName, type: list }",
        StaticType.LIST
    ),
    // nullable list
    MapperE2ETestCase(
        "type::{ name: $typeName, type: nullable::list }",
        AnyOfType(setOf(StaticType.NULL, StaticType.LIST))
    ),
    // sexp
    MapperE2ETestCase(
        "type::{ name: $typeName, type: sexp }",
        StaticType.SEXP
    ),
    // nullable sexp
    MapperE2ETestCase(
        "type::{ name: $typeName, type: nullable::sexp }",
        AnyOfType(setOf(StaticType.NULL, StaticType.SEXP))
    ),
    // struct
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct }",
        StaticType.STRUCT
    ),
    // nullable struct
    MapperE2ETestCase(
        "type::{ name: $typeName, type: nullable::struct }",
        AnyOfType(setOf(StaticType.NULL, StaticType.STRUCT))
    ),
    // bag
    MapperE2ETestCase(
        "type::{ name: $typeName, type: bag }",
        StaticType.BAG
    ),
    // nullable bag
    MapperE2ETestCase(
        "type::{ name: $typeName, type: nullable::bag }",
        AnyOfType(setOf(StaticType.NULL, StaticType.BAG))
    ),
    // missing
    MapperE2ETestCase(
        "type::{ name: $typeName, type: missing }",
        StaticType.MISSING
    ),
    // nullable missing
    MapperE2ETestCase(
        "type::{ name: $typeName, type: nullable::missing }",
        AnyOfType(setOf(StaticType.NULL, StaticType.MISSING))
    ),
    // union type - null type
    MapperE2ETestCase(
        "type::{ name: $typeName, type: nullable::\$null }",
        StaticType.NULL.withMetas(mapOf(ISL_META_KEY to listOf(buildTypeDef(typeName, buildTypeConstraint("\$null", true)))))
    ),
    // StaticType with metas
    MapperE2ETestCase(
        "type::{ name: $typeName, type: string }",
        StringType(metas = mapOf(ISL_META_KEY to listOf(buildTypeDef(typeName, buildTypeConstraint("string")))))
    ),
    // inline type constraint
    MapperE2ETestCase(
        "type::{ name: $typeName, type: { type: string } }",
        StaticType.STRING,
        "type::{ name: $typeName, type: string }"
    ),
    // nullable, inline type constraint
    MapperE2ETestCase(
        "type::{ name: $typeName, type: { type: nullable::string } }",
        AnyOfType(setOf(StaticType.NULL, StaticType.STRING)),
        "type::{ name: $typeName, type: nullable::string }"
    ),
    // nullable inline type constraint
    MapperE2ETestCase(
        "type::{ name: $typeName, type: nullable::{ type: string }}",
        StaticType.unionOf(
            StringType(metas = mapOf(ISL_META_KEY to listOf(buildTypeDef(null, buildTypeConstraint("string"))))),
            StaticType.NULL
        ),
        "type::{ name: $typeName, type: nullable::string }"
    ),
    // duplicate type names
    MapperE2ETestCase(
        """
            type::{ name: $typeName, type: string }
            type::{ name: $typeName, type: list, element: string }
        """,
        ListType(
            StaticType.STRING,
            metas = mapOf(
                ISL_META_KEY to listOf(
                    buildTypeDef(typeName, buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("string", ionBool(false))) })
                )
            )
        ),
        "type::{ name: $typeName, type: list, element: string }"
    ),
    // recursive type
    MapperE2ETestCase(
        """
            type::{ name: $typeName, any_of: [string, {type: list, element: $typeName}] }
        """,
        StaticType.unionOf(
            StaticType.STRING,
            ListType(
                StaticType.ANY,
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            typeName,
                            IonSchemaModel.build {
                                anyOf(
                                    namedType("string", ionBool(false)),
                                    inlineType(
                                        buildTypeDef(
                                            null,
                                            buildTypeConstraint("list"),
                                            IonSchemaModel.build { element(namedType(typeName, ionBool(false))) }
                                        ),
                                        ionBool(false)
                                    )
                                )
                            }
                        ),
                        buildTypeDef(null, buildTypeConstraint("list"), IonSchemaModel.build { element(namedType(typeName, ionBool(false))) })
                    )
                )
            )
        )
    )
)

internal fun basicAnyOfTests() = listOf(
    // named and inline types
    MapperE2ETestCase(
        "type::{ name: $typeName, any_of: [int, string, {type: list, element: string}] }",
        AnyOfType(
            setOf(
                StaticType.INT,
                StaticType.STRING,
                ListType(
                    StaticType.STRING,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("list"),
                                IonSchemaModel.build { element(namedType("string", ionBool(false))) }
                            )
                        )
                    )
                )
            )
        )
    ),
    // named and inline type with nested any_of constraint
    MapperE2ETestCase(
        "type::{ name: $typeName, any_of: [int, string, {type: list, element: { any_of: [int, string] }}] }",
        AnyOfType(
            setOf(
                StaticType.INT,
                StaticType.STRING,
                ListType(
                    AnyOfType(
                        setOf(StaticType.INT, StaticType.STRING),
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef(
                                    null,
                                    IonSchemaModel.build { anyOf(namedType("int", ionBool(false)), namedType("string", ionBool(false))) }
                                )
                            )
                        )
                    ),
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("list"),
                                IonSchemaModel.build {
                                    element(
                                        inlineType(
                                            buildTypeDef(
                                                null,
                                                anyOf(namedType("int", ionBool(false)), namedType("string", ionBool(false)))
                                            ),
                                            ionBool(false)
                                        )
                                    )
                                }
                            )
                        )
                    )
                )
            )
        )
    ),
    // nullable, named and inline type
    MapperE2ETestCase(
        "type::{ name: $typeName, any_of: [nullable::int, nullable::{type: list, element: string}] }",
        AnyOfType(
            setOf(
                StaticType.unionOf(StaticType.INT, StaticType.NULL),
                StaticType.unionOf(
                    ListType(
                        StaticType.STRING,
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef(
                                    null,
                                    buildTypeConstraint("list"),
                                    IonSchemaModel.build { element(namedType("string", ionBool(false))) }
                                )
                            )
                        )
                    ),
                    StaticType.NULL,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("list"),
                                IonSchemaModel.build { element(namedType("string", ionBool(false))) }
                            )
                        )
                    )
                )
            )
        )
    ),
    // named types (zero nullable types)
    MapperE2ETestCase(
        "type::{ name: $typeName, any_of: [int, string, float] }",
        AnyOfType(
            setOf(
                StaticType.INT,
                StaticType.STRING,
                StaticType.FLOAT
            )
        )
    ),
    // some nullable types
    // This tests that if a single type within a union type is nullable, then all types are essentially nullable
    // The StaticType below creates ISL that accepts the following as valid ion values: null, null.int, null.string
    // This is based on the understanding that, for all practical purposes, null.string is equivalent to null, hence null.string is an acceptable value
    MapperE2ETestCase(
        "type::{ name: $typeName, any_of: [nullable::int, string] }",
        AnyOfType(
            setOf(
                StaticType.unionOf(StaticType.NULL, StaticType.INT),
                StaticType.STRING
            )
        ),
        "type::{ name: $typeName, any_of: [nullable::int, nullable::string] }"
    ),
    // all nullable types
    MapperE2ETestCase(
        "type::{ name: $typeName, any_of: [nullable::int, nullable::string] }",
        AnyOfType(
            setOf(
                StaticType.unionOf(StaticType.NULL, StaticType.INT),
                StaticType.unionOf(StaticType.NULL, StaticType.STRING)
            )
        )
    ),
    // mix of nullable, named and inline types
    MapperE2ETestCase(
        "type::{ name: $typeName, any_of:[nullable::int, string, { type: float }, { type: list, element: string } ] }",
        StaticType.unionOf(
            StaticType.unionOf(StaticType.NULL, StaticType.INT),
            StaticType.STRING,
            StaticScalarType(
                CompileTimeFloatType,
                metas = mapOf(ISL_META_KEY to listOf(buildTypeDef(null, buildTypeConstraint("float"))))
            ),
            ListType(
                StaticType.STRING,
                metas = mapOf(
                    ISL_META_KEY to
                        listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("list"),
                                IonSchemaModel.build { element(namedType("string", ionBool(false))) }
                            )
                        )
                )
            )
        ),
        "type::{ name: $typeName, any_of:[nullable::int, nullable::string, nullable::float, nullable::{type: list, element: string}] }"
    ),
    // contains MissingType
    MapperE2ETestCase(
        "type::{ name: $typeName, any_of: [int, missing] }",
        AnyOfType(setOf(StaticType.INT, StaticType.MISSING))
    ),
    // contains Null and Missing Type
    MapperE2ETestCase(
        "type::{ name: $typeName, any_of: [\$null, missing] }",
        AnyOfType(setOf(StaticType.NULL, StaticType.MISSING)),
        "type::{ name: $typeName, type: nullable::missing }"
    )
)

internal fun listTests() = listOf(
    // element as named type
    MapperE2ETestCase(
        "type::{ name: $typeName, type: list, element: string }",
        ListType(StaticType.STRING)
    ),
    // element as nullable, named type
    MapperE2ETestCase(
        "type::{ name: $typeName, type: list, element: nullable::string }",
        ListType(StaticType.unionOf(StaticType.NULL, StaticType.STRING))
    ),
    // element as inline core type
    MapperE2ETestCase(
        "type::{ name: $typeName, type: list, element: {type: string} }",
        ListType(StringType(metas = mapOf(ISL_META_KEY to listOf(buildTypeDef(null, buildTypeConstraint("string")))))),
        "type::{ name: $typeName, type: list, element: string }"
    ),
    // element as nullable, inline core type
    MapperE2ETestCase(
        "type::{ name: $typeName, type: list, element: nullable::{type: string}}",
        ListType(
            StaticType.unionOf(
                StringType(
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("string")
                            )
                        )
                    )
                ),
                StaticType.NULL,
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            buildTypeConstraint("string")
                        )
                    )
                )
            )
        ),
        "type::{ name: $typeName, type: list, element: nullable::string }"
    ),
    // Same as above, just another way of expressing input in ISL
    MapperE2ETestCase(
        "type::{ name: $typeName, type: list, element: { type: nullable::int } }",
        ListType(
            StaticType.unionOf(
                StaticType.NULL,
                StaticType.INT,
                metas = mapOf(ISL_META_KEY to listOf(buildTypeDef(null, buildTypeConstraint("int", true))))
            )
        ),
        "type::{ name: $typeName, type: list, element: nullable::int }"
    ),
    // element that has a constraint
    MapperE2ETestCase(
        "type::{ name: $typeName, type: list, element: {type: string, codepoint_length: 5} }",
        ListType(
            CharType(
                5,
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            buildTypeConstraint("string"),
                            IonSchemaModel.build { codepointLength(equalsNumber(ionInt(5))) }
                        )
                    )
                )
            )
        )
    ),
    // element as nullable, inline type with constraints
    MapperE2ETestCase(
        "type::{ name: $typeName, type: list, element: {type: nullable::{type: string, codepoint_length:5}}}",
        ListType(
            StaticType.unionOf(
                CharType(
                    5,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("string"),
                                IonSchemaModel.build { codepointLength(equalsNumber(ionInt(5))) }
                            )
                        )
                    )
                ),
                StaticType.NULL,
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            IonSchemaModel.build {
                                typeConstraint(
                                    inlineType(
                                        typeDefinition(
                                            null,
                                            constraintList(
                                                typeConstraint(namedType("string", ionBool(false))),
                                                codepointLength(equalsNumber(ionInt(5)))
                                            )
                                        ),
                                        ionBool(true)
                                    )
                                )
                            }
                        )
                    )
                )
            )
        ),
        "type::{ name: $typeName, type: list, element: nullable::{type: string, codepoint_length:5}}"
    ),
    // invalid values in string constraint
    MapperE2ETestCase(
        "type::{ name: $typeName, type: list, element: {type: string, codepoint_length: range::[1, 2048]} }",
        ListType(
            StringType(
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            buildTypeConstraint("string"),
                            IonSchemaModel.build { codepointLength(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(2048))))) }
                        )
                    )
                )
            )
        )
    ),
    // invalid values in decimal constraints
    MapperE2ETestCase(
        "type::{ name: $typeName, type: list, element: {type: decimal,  precision: range::[1, 47], scale: range::[1,37]}}",
        ListType(
            StaticScalarType(
                DecimalType.createType(emptyList()),
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            buildTypeConstraint("decimal"),
                            IonSchemaModel.build { precision(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(47))))) },
                            IonSchemaModel.build { scale(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(37))))) }
                        )
                    )
                )
            )
        )
    ),
    MapperE2ETestCase(
        "type::{ name: $typeName, type: list, element: {type: int, valid_values: range::[${Int.MIN_VALUE}, ${Int.MAX_VALUE}]} }",
        ListType(
            Int4Type(
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            buildTypeConstraint("int"),
                            IonSchemaModel.build {
                                validValues(
                                    rangeOfValidValues(
                                        numRange(
                                            numberRange(
                                                inclusive(ionInt(Int.MIN_VALUE.toLong())),
                                                inclusive(ionInt(Int.MAX_VALUE.toLong()))
                                            )
                                        )
                                    )
                                )
                            }
                        )
                    )
                )
            )
        )
    ),
    MapperE2ETestCase(
        "type::{ name: $typeName, type: list, element: {type: nullable::{type: int, valid_values: range::[${Int.MIN_VALUE}, ${Int.MAX_VALUE}]}}}",
        ListType(
            StaticType.unionOf(
                Int4Type(
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("int"),
                                IonSchemaModel.build {
                                    validValues(
                                        rangeOfValidValues(
                                            numRange(
                                                numberRange(
                                                    inclusive(ionInt(Int.MIN_VALUE.toLong())),
                                                    inclusive(ionInt(Int.MAX_VALUE.toLong()))
                                                )
                                            )
                                        )
                                    )
                                }
                            )
                        )
                    )
                ),
                StaticType.NULL,
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            IonSchemaModel.build {
                                typeConstraint(
                                    inlineType(
                                        typeDefinition(
                                            null,
                                            constraintList(
                                                typeConstraint(namedType("int", ionBool(false))),
                                                IonSchemaModel.build {
                                                    validValues(
                                                        rangeOfValidValues(
                                                            numRange(
                                                                numberRange(
                                                                    inclusive(ionInt(Int.MIN_VALUE.toLong())),
                                                                    inclusive(ionInt(Int.MAX_VALUE.toLong()))
                                                                )
                                                            )
                                                        )
                                                    )
                                                }
                                            )
                                        ),
                                        ionBool(true)
                                    )
                                )
                            }
                        )
                    )
                )
            )
        ),
        "type::{ name: $typeName, type: list, element: nullable::{type: int, valid_values: range::[${Int.MIN_VALUE}, ${Int.MAX_VALUE}]}}"
    ),
    // element as named top-level type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: string }
            type::{ name: $typeName, type: list, element: bar }
        """,
        ListType(
            StringType(
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef("bar", buildTypeConstraint("string"))
                    )
                )
            )
        )
    ),
    // element as inline top-level type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: string }
            type::{ name: $typeName, type: list, element: { type: bar } }
        """,
        ListType(
            StringType(
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef("bar", buildTypeConstraint("string")),
                        buildTypeDef(null, buildTypeConstraint("bar"))
                    )
                )
            )
        ),
        """
            type::{ name: bar, type: string }
            type::{ name: $typeName, type: list, element: bar }
        """
    ),
    // element as nullable, named top-level type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: string }
            type::{ name: $typeName, type: list, element: nullable::bar }
        """,
        ListType(
            StaticType.unionOf(
                StaticType.NULL,
                StringType(
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef("bar", buildTypeConstraint("string"))
                        )
                    )
                ),
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef("bar", buildTypeConstraint("string"))
                    )
                )
            )
        )
    ),
    // Same as above, with more constraints on the custom type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: string, codepoint_length: 5, utf8_byte_length: 5 }
            type::{ name: $typeName, type: list, element: nullable::bar }
        """,
        ListType(
            StaticType.unionOf(
                StaticType.NULL,
                CharType(
                    5,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                "bar",
                                buildTypeConstraint("string"),
                                IonSchemaModel.build { codepointLength(equalsNumber(ionInt(5))) },
                                IonSchemaModel.build { utf8ByteLength(equalsNumber(ionInt(5))) }
                            )
                        )
                    )
                ),
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            "bar",
                            buildTypeConstraint("string"),
                            IonSchemaModel.build { codepointLength(equalsNumber(ionInt(5))) },
                            IonSchemaModel.build { utf8ByteLength(equalsNumber(ionInt(5))) }
                        )
                    )
                )
            )
        )
    ),
    // element as collection type
    MapperE2ETestCase(
        "type::{ name: $typeName, type: list, element: { type: list, element: int } }",
        ListType(
            ListType(
                StaticType.INT,
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            buildTypeConstraint("list"),
                            IonSchemaModel.build { element(namedType("int", ionBool(false))) }
                        )
                    )
                )
            )
        )
    ),
    // element as collection type with nullable element
    MapperE2ETestCase(
        "type::{ name: $typeName, type: list, element: { type: list, element: nullable::string } }",
        ListType(
            ListType(
                StaticType.unionOf(StaticType.STRING, StaticType.NULL),
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            buildTypeConstraint("list"),
                            IonSchemaModel.build { element(namedType("string", ionBool(true))) }
                        )
                    )
                )
            )
        )
    ),
    // list with other constraints
    MapperE2ETestCase(
        "type::{ name: $typeName, type: list, element: int, contains: [1, 5] }",
        ListType(
            StaticType.INT,
            metas = mapOf(
                ISL_META_KEY to listOf(
                    buildTypeDef(
                        typeName,
                        buildTypeConstraint("list"),
                        IonSchemaModel.build { element(namedType("int", ionBool(false))) },
                        IonSchemaModel.build { contains(listOf(ionInt(1), ionInt(5))) }
                    )
                )
            )
        )
    ),
    // element as 'any'
    MapperE2ETestCase(
        "type::{ name: $typeName, type: list, element: any }",
        ListType(StaticType.ANY),
        "type::{ name: $typeName, type: list }"
    ),
    // element as any_of
    MapperE2ETestCase(
        "type::{ name: $typeName, type: list, element: { any_of: [int, string] } }",
        ListType(
            AnyOfType(
                setOf(StaticType.INT, StaticType.STRING),
                metas = mapOf(
                    ISL_META_KEY to
                        listOf(
                            buildTypeDef(
                                null,
                                IonSchemaModel.build {
                                    anyOf(
                                        namedType("int", ionBool(false)),
                                        namedType("string", ionBool(false))
                                    )
                                }
                            )
                        )
                )
            )
        )
    ),
    // element as any_of with custom type
    MapperE2ETestCase(
        """
                type::{ name: bar, type: int }
                type::{ name: $typeName, type: list, element: { any_of: [bar, string] } }
            """,
        ListType(
            AnyOfType(
                setOf(
                    IntType(metas = mapOf(ISL_META_KEY to listOf(buildTypeDef("bar", buildTypeConstraint("int"))))),
                    StaticType.STRING
                ),
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef("bar", buildTypeConstraint("int")),
                        buildTypeDef(
                            null,
                            IonSchemaModel.build {
                                anyOf(
                                    namedType("bar", ionBool(false)),
                                    namedType("string", ionBool(false))
                                )
                            }
                        )
                    )
                )
            )
        )
    )
)

internal fun sexpTests() = listOf(
    // element as named type
    MapperE2ETestCase(
        "type::{ name: $typeName, type: sexp, element: string }",
        SexpType(StaticType.STRING)
    ),
    // element as nullable, named type
    MapperE2ETestCase(
        "type::{ name: $typeName, type: sexp, element: nullable::string }",
        SexpType(StaticType.unionOf(StaticType.NULL, StaticType.STRING))
    ),
    // element as inline core type
    MapperE2ETestCase(
        "type::{ name: $typeName, type: sexp, element: {type: string} }",
        SexpType(StringType(metas = mapOf(ISL_META_KEY to listOf(buildTypeDef(null, buildTypeConstraint("string")))))),
        "type::{ name: $typeName, type: sexp, element: string }"
    ),
    // element as nullable, inline core type
    MapperE2ETestCase(
        "type::{ name: $typeName, type: sexp, element: nullable::{type: string}}",
        SexpType(
            StaticType.unionOf(
                StringType(
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("string")
                            )
                        )
                    )
                ),
                StaticType.NULL,
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            buildTypeConstraint("string")
                        )
                    )
                )
            )
        ),
        "type::{ name: $typeName, type: sexp, element: nullable::string }"
    ),
    // Same as above, just another way of expressing input in ISL
    MapperE2ETestCase(
        "type::{ name: $typeName, type: sexp, element: { type: nullable::int } }",
        SexpType(
            StaticType.unionOf(
                StaticType.NULL,
                StaticType.INT,
                metas = mapOf(ISL_META_KEY to listOf(buildTypeDef(null, buildTypeConstraint("int", true))))
            )
        ),
        "type::{ name: $typeName, type: sexp, element: nullable::int }"
    ),
    // element that has a constraint
    MapperE2ETestCase(
        "type::{ name: $typeName, type: sexp, element: {type: string, codepoint_length: 5} }",
        SexpType(
            CharType(
                5,
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            buildTypeConstraint("string"),
                            IonSchemaModel.build { codepointLength(equalsNumber(ionInt(5))) }
                        )
                    )
                )
            )
        )
    ),
    // element as nullable, inline type with constraints
    MapperE2ETestCase(
        "type::{ name: $typeName, type: sexp, element: {type: nullable::{type: string, codepoint_length:5}}}",
        SexpType(
            StaticType.unionOf(
                CharType(
                    5,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("string"),
                                IonSchemaModel.build { codepointLength(equalsNumber(ionInt(5))) }
                            )
                        )
                    )
                ),
                StaticType.NULL,
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            IonSchemaModel.build {
                                typeConstraint(
                                    inlineType(
                                        typeDefinition(
                                            null,
                                            constraintList(
                                                typeConstraint(namedType("string", ionBool(false))),
                                                codepointLength(equalsNumber(ionInt(5)))
                                            )
                                        ),
                                        ionBool(true)
                                    )
                                )
                            }
                        )
                    )
                )
            )
        ),
        "type::{ name: $typeName, type: sexp, element: nullable::{type: string, codepoint_length:5}}"
    ),
    // invalid values in string constraint
    MapperE2ETestCase(
        "type::{ name: $typeName, type: sexp, element: {type: string, codepoint_length: range::[1, 2048]} }",
        SexpType(
            StringType(
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            buildTypeConstraint("string"),
                            IonSchemaModel.build { codepointLength(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(2048))))) }
                        )
                    )
                )
            )
        )
    ),
    // invalid values in decimal constraints
    MapperE2ETestCase(
        "type::{ name: $typeName, type: sexp, element: {type: decimal,  precision: range::[1, 47], scale: range::[1,37]}}",
        SexpType(
            StaticScalarType(
                DecimalType.createType(emptyList()),
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            buildTypeConstraint("decimal"),
                            IonSchemaModel.build { precision(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(47))))) },
                            IonSchemaModel.build { scale(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(37))))) }
                        )
                    )
                )
            )
        )
    ),
    MapperE2ETestCase(
        "type::{ name: $typeName, type: sexp, element: {type: int, valid_values: range::[${Int.MIN_VALUE}, ${Int.MAX_VALUE}]} }",
        SexpType(
            Int4Type(
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            buildTypeConstraint("int"),
                            IonSchemaModel.build {
                                validValues(
                                    rangeOfValidValues(
                                        numRange(
                                            numberRange(
                                                inclusive(ionInt(Int.MIN_VALUE.toLong())),
                                                inclusive(ionInt(Int.MAX_VALUE.toLong()))
                                            )
                                        )
                                    )
                                )
                            }
                        )
                    )
                )
            )
        )
    ),
    MapperE2ETestCase(
        "type::{ name: $typeName, type: sexp, element: {type: nullable::{type: int, valid_values: range::[${Int.MIN_VALUE}, ${Int.MAX_VALUE}]}}}",
        SexpType(
            StaticType.unionOf(
                Int4Type(
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("int"),
                                IonSchemaModel.build {
                                    validValues(
                                        rangeOfValidValues(
                                            numRange(
                                                numberRange(
                                                    inclusive(ionInt(Int.MIN_VALUE.toLong())),
                                                    inclusive(ionInt(Int.MAX_VALUE.toLong()))
                                                )
                                            )
                                        )
                                    )
                                }
                            )
                        )
                    )
                ),
                StaticType.NULL,
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            IonSchemaModel.build {
                                typeConstraint(
                                    inlineType(
                                        typeDefinition(
                                            null,
                                            constraintList(
                                                typeConstraint(namedType("int", ionBool(false))),
                                                IonSchemaModel.build {
                                                    validValues(
                                                        rangeOfValidValues(
                                                            numRange(
                                                                numberRange(
                                                                    inclusive(ionInt(Int.MIN_VALUE.toLong())),
                                                                    inclusive(ionInt(Int.MAX_VALUE.toLong()))
                                                                )
                                                            )
                                                        )
                                                    )
                                                }
                                            )
                                        ),
                                        ionBool(true)
                                    )
                                )
                            }
                        )
                    )
                )
            )
        ),
        "type::{ name: $typeName, type: sexp, element: nullable::{type: int, valid_values: range::[${Int.MIN_VALUE}, ${Int.MAX_VALUE}]}}"
    ),
    // element as named top-level type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: string }
            type::{ name: $typeName, type: sexp, element: bar }
        """,
        SexpType(
            StringType(
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef("bar", buildTypeConstraint("string"))
                    )
                )
            )
        )
    ),
    // element as inline top-level type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: string }
            type::{ name: $typeName, type: sexp, element: { type: bar } }
        """,
        SexpType(
            StringType(
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef("bar", buildTypeConstraint("string")),
                        buildTypeDef(null, buildTypeConstraint("bar"))
                    )
                )
            )
        ),
        """
            type::{ name: bar, type: string }
            type::{ name: $typeName, type: sexp, element: bar }
        """
    ),
    // element as nullable, named top-level type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: string }
            type::{ name: $typeName, type: sexp, element: nullable::bar }
        """,
        SexpType(
            StaticType.unionOf(
                StaticType.NULL,
                StringType(
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef("bar", buildTypeConstraint("string"))
                        )
                    )
                ),
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef("bar", buildTypeConstraint("string"))
                    )
                )
            )
        )
    ),
    // Same as above, with more constraints on the custom type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: string, codepoint_length: 5, utf8_byte_length: 5 }
            type::{ name: $typeName, type: sexp, element: nullable::bar }
        """,
        SexpType(
            StaticType.unionOf(
                StaticType.NULL,
                CharType(
                    5,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                "bar",
                                buildTypeConstraint("string"),
                                IonSchemaModel.build { codepointLength(equalsNumber(ionInt(5))) },
                                IonSchemaModel.build { utf8ByteLength(equalsNumber(ionInt(5))) }
                            )
                        )
                    )
                ),
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            "bar",
                            buildTypeConstraint("string"),
                            IonSchemaModel.build { codepointLength(equalsNumber(ionInt(5))) },
                            IonSchemaModel.build { utf8ByteLength(equalsNumber(ionInt(5))) }
                        )
                    )
                )
            )
        )
    ),
    // element as collection type
    MapperE2ETestCase(
        "type::{ name: $typeName, type: sexp, element: { type: list, element: int } }",
        SexpType(
            ListType(
                StaticType.INT,
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            buildTypeConstraint("list"),
                            IonSchemaModel.build { element(namedType("int", ionBool(false))) }
                        )
                    )
                )
            )
        )
    ),
    // element as collection type with nullable element
    MapperE2ETestCase(
        "type::{ name: $typeName, type: sexp, element: { type: list, element: nullable::string } }",
        SexpType(
            ListType(
                StaticType.unionOf(StaticType.STRING, StaticType.NULL),
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            buildTypeConstraint("list"),
                            IonSchemaModel.build { element(namedType("string", ionBool(true))) }
                        )
                    )
                )
            )
        )
    ),
    // sexp with other constraints
    MapperE2ETestCase(
        "type::{ name: $typeName, type: sexp, element: int, contains: [1, 5] }",
        SexpType(
            StaticType.INT,
            metas = mapOf(
                ISL_META_KEY to listOf(
                    buildTypeDef(
                        typeName,
                        buildTypeConstraint("sexp"),
                        IonSchemaModel.build { element(namedType("int", ionBool(false))) },
                        IonSchemaModel.build { contains(listOf(ionInt(1), ionInt(5))) }
                    )
                )
            )
        )
    ),
    // element as 'any'
    MapperE2ETestCase(
        "type::{ name: $typeName, type: sexp, element: any }",
        SexpType(StaticType.ANY),
        "type::{ name: $typeName, type: sexp }"
    ),
    // element as any_of
    MapperE2ETestCase(
        "type::{ name: $typeName, type: sexp, element: { any_of: [int, string] } }",
        SexpType(
            AnyOfType(
                setOf(StaticType.INT, StaticType.STRING),
                metas = mapOf(
                    ISL_META_KEY to
                        listOf(
                            buildTypeDef(
                                null,
                                IonSchemaModel.build {
                                    anyOf(
                                        namedType("int", ionBool(false)),
                                        namedType("string", ionBool(false))
                                    )
                                }
                            )
                        )
                )
            )
        )
    ),
    // element as any_of with custom type
    MapperE2ETestCase(
        """
                type::{ name: bar, type: int }
                type::{ name: $typeName, type: sexp, element: { any_of: [bar, string] } }
            """,
        SexpType(
            AnyOfType(
                setOf(
                    IntType(metas = mapOf(ISL_META_KEY to listOf(buildTypeDef("bar", buildTypeConstraint("int"))))),
                    StaticType.STRING
                ),
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef("bar", buildTypeConstraint("int")),
                        buildTypeDef(
                            null,
                            IonSchemaModel.build {
                                anyOf(
                                    namedType("bar", ionBool(false)),
                                    namedType("string", ionBool(false))
                                )
                            }
                        )
                    )
                )
            )
        )
    )
)

internal fun bagTests() = listOf(
    // element as named type
    MapperE2ETestCase(
        "type::{ name: $typeName, type: bag, element: string }",
        BagType(StaticType.STRING)
    ),
    // element as nullable, named type
    MapperE2ETestCase(
        "type::{ name: $typeName, type: bag, element: nullable::string }",
        BagType(StaticType.unionOf(StaticType.NULL, StaticType.STRING))
    ),
    // element as inline core type
    MapperE2ETestCase(
        "type::{ name: $typeName, type: bag, element: {type: string} }",
        BagType(StringType(metas = mapOf(ISL_META_KEY to listOf(buildTypeDef(null, buildTypeConstraint("string")))))),
        "type::{ name: $typeName, type: bag, element: string }"
    ),
    // element as nullable, inline core type
    MapperE2ETestCase(
        "type::{ name: $typeName, type: bag, element: nullable::{type: string}}",
        BagType(
            StaticType.unionOf(
                StringType(
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("string")
                            )
                        )
                    )
                ),
                StaticType.NULL,
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            buildTypeConstraint("string")
                        )
                    )
                )
            )
        ),
        "type::{ name: $typeName, type: bag, element: nullable::string }"
    ),
    // Same as above, just another way of expressing input in ISL
    MapperE2ETestCase(
        "type::{ name: $typeName, type: bag, element: { type: nullable::int } }",
        BagType(
            StaticType.unionOf(
                StaticType.NULL,
                StaticType.INT,
                metas = mapOf(ISL_META_KEY to listOf(buildTypeDef(null, buildTypeConstraint("int", true))))
            )
        ),
        "type::{ name: $typeName, type: bag, element: nullable::int }"
    ),
    // element that has a constraint
    MapperE2ETestCase(
        "type::{ name: $typeName, type: bag, element: {type: string, codepoint_length: 5} }",
        BagType(
            CharType(
                5,
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            buildTypeConstraint("string"),
                            IonSchemaModel.build { codepointLength(equalsNumber(ionInt(5))) }
                        )
                    )
                )
            )
        )
    ),
    // element as nullable, inline type with constraints
    MapperE2ETestCase(
        "type::{ name: $typeName, type: bag, element: {type: nullable::{type: string, codepoint_length:5}}}",
        BagType(
            StaticType.unionOf(
                CharType(
                    5,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("string"),
                                IonSchemaModel.build { codepointLength(equalsNumber(ionInt(5))) }
                            )
                        )
                    )
                ),
                StaticType.NULL,
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            IonSchemaModel.build {
                                typeConstraint(
                                    inlineType(
                                        typeDefinition(
                                            null,
                                            constraintList(
                                                typeConstraint(namedType("string", ionBool(false))),
                                                codepointLength(equalsNumber(ionInt(5)))
                                            )
                                        ),
                                        ionBool(true)
                                    )
                                )
                            }
                        )
                    )
                )
            )
        ),
        "type::{ name: $typeName, type: bag, element: nullable::{type: string, codepoint_length:5}}"
    ),
    // invalid values in string constraint
    MapperE2ETestCase(
        "type::{ name: $typeName, type: bag, element: {type: string, codepoint_length: range::[1, 2048]} }",
        BagType(
            StringType(
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            buildTypeConstraint("string"),
                            IonSchemaModel.build { codepointLength(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(2048))))) }
                        )
                    )
                )
            )
        )
    ),
    // invalid values in decimal constraints
    MapperE2ETestCase(
        "type::{ name: $typeName, type: bag, element: {type: decimal,  precision: range::[1, 47], scale: range::[1,37]}}",
        BagType(
            StaticScalarType(
                DecimalType.createType(emptyList()),
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            buildTypeConstraint("decimal"),
                            IonSchemaModel.build { precision(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(47))))) },
                            IonSchemaModel.build { scale(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(37))))) }
                        )
                    )
                )
            )
        )
    ),
    MapperE2ETestCase(
        "type::{ name: $typeName, type: bag, element: {type: int, valid_values: range::[${Int.MIN_VALUE}, ${Int.MAX_VALUE}]} }",
        BagType(
            Int4Type(
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            buildTypeConstraint("int"),
                            IonSchemaModel.build {
                                validValues(
                                    rangeOfValidValues(
                                        numRange(
                                            numberRange(
                                                inclusive(ionInt(Int.MIN_VALUE.toLong())),
                                                inclusive(ionInt(Int.MAX_VALUE.toLong()))
                                            )
                                        )
                                    )
                                )
                            }
                        )
                    )
                )
            )
        )
    ),
    MapperE2ETestCase(
        "type::{ name: $typeName, type: bag, element: {type: nullable::{type: int, valid_values: range::[${Int.MIN_VALUE}, ${Int.MAX_VALUE}]}}}",
        BagType(
            StaticType.unionOf(
                Int4Type(
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("int"),
                                IonSchemaModel.build {
                                    validValues(
                                        rangeOfValidValues(
                                            numRange(
                                                numberRange(
                                                    inclusive(ionInt(Int.MIN_VALUE.toLong())),
                                                    inclusive(ionInt(Int.MAX_VALUE.toLong()))
                                                )
                                            )
                                        )
                                    )
                                }
                            )
                        )
                    )
                ),
                StaticType.NULL,
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            IonSchemaModel.build {
                                typeConstraint(
                                    inlineType(
                                        typeDefinition(
                                            null,
                                            constraintList(
                                                typeConstraint(namedType("int", ionBool(false))),
                                                IonSchemaModel.build {
                                                    validValues(
                                                        rangeOfValidValues(
                                                            numRange(
                                                                numberRange(
                                                                    inclusive(ionInt(Int.MIN_VALUE.toLong())),
                                                                    inclusive(ionInt(Int.MAX_VALUE.toLong()))
                                                                )
                                                            )
                                                        )
                                                    )
                                                }
                                            )
                                        ),
                                        ionBool(true)
                                    )
                                )
                            }
                        )
                    )
                )
            )
        ),
        "type::{ name: $typeName, type: bag, element: nullable::{type: int, valid_values: range::[${Int.MIN_VALUE}, ${Int.MAX_VALUE}]}}"
    ),
    // element as collection type
    MapperE2ETestCase(
        "type::{ name: $typeName, type: bag, element: { type: list, element: int } }",
        BagType(
            ListType(
                StaticType.INT,
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            buildTypeConstraint("list"),
                            IonSchemaModel.build { element(namedType("int", ionBool(false))) }
                        )
                    )
                )
            )
        )
    ),
    // element as collection type with nullable element
    MapperE2ETestCase(
        "type::{ name: $typeName, type: bag, element: { type: list, element: nullable::string } }",
        BagType(
            ListType(
                StaticType.unionOf(StaticType.STRING, StaticType.NULL),
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            buildTypeConstraint("list"),
                            IonSchemaModel.build { element(namedType("string", ionBool(true))) }
                        )
                    )
                )
            )
        )
    ),
    // list with other constraints
    MapperE2ETestCase(
        "type::{ name: $typeName, type: bag, element: int, contains: [1, 5] }",
        BagType(
            StaticType.INT,
            metas = mapOf(
                ISL_META_KEY to listOf(
                    buildTypeDef(
                        typeName,
                        buildTypeConstraint("bag"),
                        IonSchemaModel.build { element(namedType("int", ionBool(false))) },
                        IonSchemaModel.build { contains(listOf(ionInt(1), ionInt(5))) }
                    )
                )
            )
        )
    ),
    // element as 'any'
    MapperE2ETestCase(
        "type::{ name: $typeName, type: bag, element: any }",
        BagType(StaticType.ANY),
        "type::{ name: $typeName, type: bag }"
    ),
    // element as any_of
    MapperE2ETestCase(
        "type::{ name: $typeName, type: bag, element: { any_of: [int, string] } }",
        BagType(
            AnyOfType(
                setOf(StaticType.INT, StaticType.STRING),
                metas = mapOf(
                    ISL_META_KEY to
                        listOf(
                            buildTypeDef(
                                null,
                                IonSchemaModel.build {
                                    anyOf(
                                        namedType("int", ionBool(false)),
                                        namedType("string", ionBool(false))
                                    )
                                }
                            )
                        )
                )
            )
        )
    ),
    // element as any_of with custom type
    MapperE2ETestCase(
        """
                type::{ name: bar, type: int }
                type::{ name: $typeName, type: bag, element: { any_of: [bar, string] } }
            """,
        BagType(
            AnyOfType(
                setOf(
                    IntType(metas = mapOf(ISL_META_KEY to listOf(buildTypeDef("bar", buildTypeConstraint("int"))))),
                    StaticType.STRING
                ),
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef("bar", buildTypeConstraint("int")),
                        buildTypeDef(
                            null,
                            IonSchemaModel.build {
                                anyOf(
                                    namedType("bar", ionBool(false)),
                                    namedType("string", ionBool(false))
                                )
                            }
                        )
                    )
                )
            )
        )
    ),
    // element as struct
    MapperE2ETestCase(
        """
            type::{ name: $typeName, type: bag, element: {
                type: struct, fields:{
                    a: nullable::int,
                    b: { any_of: [int, {type: list, element: string}], occurs: required },
                    c: { any_of: [int, string] }
                }
            }}
        """,
        BagType(
            StructType(
                mapOf(
                    "a" to StaticType.unionOf(StaticType.INT, StaticType.NULL, StaticType.MISSING),
                    "b" to StaticType.unionOf(
                        StaticType.INT,
                        ListType(
                            StaticType.STRING,
                            metas = mapOf(
                                ISL_META_KEY to listOf(
                                    buildTypeDef(
                                        null,
                                        buildTypeConstraint("list"),
                                        IonSchemaModel.build { element(namedType("string", ionBool(false))) }
                                    )
                                )
                            )
                        ),
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef(
                                    null,
                                    IonSchemaModel.build {
                                        anyOf(
                                            namedType("int", ionBool(false)),
                                            inlineType(
                                                buildTypeDef(
                                                    null,
                                                    buildTypeConstraint("list"),
                                                    IonSchemaModel.build { element(namedType("string", ionBool(false))) }
                                                ),
                                                ionBool(false)
                                            )
                                        )
                                    },
                                    IonSchemaModel.build { occurs(occursRequired()) }
                                )
                            )
                        )
                    ),
                    "c" to StaticType.unionOf(
                        StaticType.INT, StaticType.STRING, StaticType.MISSING,
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef(
                                    null,
                                    IonSchemaModel.build {
                                        anyOf(
                                            namedType("int", ionBool(false)),
                                            namedType("string", ionBool(false))
                                        )
                                    }
                                )
                            )
                        )
                    )
                ),
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            buildTypeConstraint("struct"),
                            IonSchemaModel.build {
                                fields(
                                    field("a", namedType("int", ionBool(true))),
                                    field(
                                        "b",
                                        inlineType(
                                            buildTypeDef(
                                                null,
                                                IonSchemaModel.build {
                                                    anyOf(
                                                        namedType("int", ionBool(false)),
                                                        inlineType(
                                                            buildTypeDef(
                                                                null,
                                                                buildTypeConstraint("list"),
                                                                IonSchemaModel.build { element(namedType("string", ionBool(false))) }
                                                            ),
                                                            ionBool(false)
                                                        )
                                                    )
                                                },
                                                IonSchemaModel.build { occurs(occursRequired()) }
                                            ),
                                            ionBool(false)
                                        )
                                    ),
                                    field(
                                        "c",
                                        inlineType(
                                            buildTypeDef(
                                                null,
                                                IonSchemaModel.build {
                                                    anyOf(
                                                        namedType("int", ionBool(false)),
                                                        namedType("string", ionBool(false))
                                                    )
                                                }
                                            ),
                                            ionBool(false)
                                        )
                                    )
                                )
                            }
                        )
                    )
                )
            )
        )
    )
)

internal fun structTests() = listOf(
    // single field, named type
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct, fields: { a: int } }",
        StructType(mapOf("a" to StaticType.unionOf(StaticType.INT, StaticType.MISSING)))
    ),
    // single field, inline type
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct, fields: { a: {type: int} } }",
        StructType(
            mapOf(
                "a" to StaticType.unionOf(
                    IntType(
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef(null, buildTypeConstraint("int"))
                            )
                        )
                    ),
                    StaticType.MISSING,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(null, buildTypeConstraint("int"))
                        )
                    )
                )
            )
        ),
        "type::{ name: $typeName, type: struct, fields: { a: int } }"
    ),
    // single field, type with constraint
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct, fields: { a: {type: string, codepoint_length: range::[0, 5]} } }",
        StructType(
            mapOf(
                "a" to StaticType.unionOf(
                    VarcharType(
                        5,
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef(
                                    null,
                                    buildTypeConstraint("string"),
                                    IonSchemaModel.build { codepointLength(equalsRange(numberRange(inclusive(ionInt(0)), inclusive(ionInt(5))))) }
                                )
                            )
                        )
                    ),
                    StaticType.MISSING,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("string"),
                                IonSchemaModel.build { codepointLength(equalsRange(numberRange(inclusive(ionInt(0)), inclusive(ionInt(5))))) }
                            )
                        )
                    )
                )
            )
        )
    ),
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct, fields: { a: {type: int, valid_values: range::[${Int.MIN_VALUE}, ${Int.MAX_VALUE}]} } }",
        StructType(
            mapOf(
                "a" to StaticType.unionOf(
                    Int4Type(
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef(
                                    null,
                                    buildTypeConstraint("int"),
                                    IonSchemaModel.build { validValues(rangeOfValidValues(numRange(numberRange(inclusive(ionInt(-2147483648)), inclusive(ionInt(2147483647)))))) }
                                )
                            )
                        )
                    ),
                    StaticType.MISSING,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("int"),
                                IonSchemaModel.build { validValues(rangeOfValidValues(numRange(numberRange(inclusive(ionInt(-2147483648)), inclusive(ionInt(2147483647)))))) }
                            )
                        )
                    )
                )
            )
        )
    ),
    // single field, required, type with constraint
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct, fields: { a: {type: string, codepoint_length: range::[0, 5], occurs: required} } }",
        StructType(
            mapOf(
                "a" to VarcharType(
                    5,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("string"),
                                IonSchemaModel.build { codepointLength(equalsRange(numberRange(inclusive(ionInt(0)), inclusive(ionInt(5))))) },
                                IonSchemaModel.build { occurs(occursRequired()) }
                            )
                        )
                    )
                )
            )
        )
    ),
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct, fields: { a: {type: int, valid_values: range::[${Int.MIN_VALUE}, ${Int.MAX_VALUE}], occurs: required} } }",
        StructType(
            mapOf(
                "a" to Int4Type(
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("int"),
                                IonSchemaModel.build { validValues(rangeOfValidValues(numRange(numberRange(inclusive(ionInt(-2147483648)), inclusive(ionInt(2147483647)))))) },
                                IonSchemaModel.build { occurs(occursRequired()) }
                            )
                        )
                    )
                )
            )
        )
    ),
    // single field, nullable type with constraint
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct, fields: { a: nullable::{type: string, codepoint_length: range::[0, 5]} } }",
        StructType(
            mapOf(
                "a" to StaticType.unionOf(
                    VarcharType(
                        5,
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef(
                                    null,
                                    buildTypeConstraint("string"),
                                    IonSchemaModel.build { codepointLength(equalsRange(numberRange(inclusive(ionInt(0)), inclusive(ionInt(5))))) }
                                )
                            )
                        )
                    ),
                    StaticType.MISSING,
                    StaticType.NULL,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("string"),
                                IonSchemaModel.build { codepointLength(equalsRange(numberRange(inclusive(ionInt(0)), inclusive(ionInt(5))))) }
                            )
                        )
                    )
                )
            )
        )
    ),
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct, fields: { a: nullable::{type: string, codepoint_length: range::[1, 2048]}}}",
        StructType(
            mapOf(
                "a" to StaticType.unionOf(
                    StringType(
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef(
                                    null,
                                    buildTypeConstraint("string"),
                                    IonSchemaModel.build { codepointLength(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(2048))))) }
                                )
                            )
                        )
                    ),
                    StaticType.NULL,
                    StaticType.MISSING,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("string"),
                                IonSchemaModel.build { codepointLength(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(2048))))) }
                            )
                        )
                    )
                )
            )
        )
    ),
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct, fields: { a: nullable::{type: decimal, precision: range::[1, 47], scale: range::[1,37]}}}",
        StructType(
            mapOf(
                "a" to StaticType.unionOf(
                    StaticScalarType(
                        DecimalType.createType(emptyList()),
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef(
                                    null,
                                    buildTypeConstraint("decimal"),
                                    IonSchemaModel.build { precision(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(47))))) },
                                    IonSchemaModel.build { scale(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(37))))) }
                                )
                            )
                        )
                    ),
                    StaticType.NULL,
                    StaticType.MISSING,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("decimal"),
                                IonSchemaModel.build { precision(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(47))))) },
                                IonSchemaModel.build { scale(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(37))))) }
                            )
                        )
                    )
                )
            )
        )
    ),
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct, fields: { a: nullable::{type: int, valid_values: range::[${Int.MIN_VALUE}, ${Int.MAX_VALUE}]} } }",
        StructType(
            mapOf(
                "a" to StaticType.unionOf(
                    Int4Type(
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef(
                                    null,
                                    buildTypeConstraint("int"),
                                    IonSchemaModel.build { validValues(rangeOfValidValues(numRange(numberRange(inclusive(ionInt(-2147483648)), inclusive(ionInt(2147483647)))))) }
                                )
                            )
                        )
                    ),
                    StaticType.MISSING,
                    StaticType.NULL,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("int"),
                                IonSchemaModel.build { validValues(rangeOfValidValues(numRange(numberRange(inclusive(ionInt(-2147483648)), inclusive(ionInt(2147483647)))))) }
                            )
                        )
                    )
                )
            )
        )
    ),
    // single field, named type nullable
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct, fields: { a: nullable::int  } }",
        StructType(mapOf("a" to StaticType.unionOf(StaticType.INT, StaticType.NULL, StaticType.MISSING)))
    ),
    // single field, inline type nullable
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct, fields: { a: {type: nullable::int}  } }",
        StructType(
            mapOf(
                "a" to StaticType.unionOf(
                    StaticType.INT,
                    StaticType.NULL,
                    StaticType.MISSING,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(null, buildTypeConstraint("int", true))
                        )
                    )
                )
            )
        ),
        "type::{ name: $typeName, type: struct, fields: { a: nullable::int } }"
    ),
    // single field, nullable inline single type, required
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct, fields: { a: { type: nullable::int, occurs: required } } }",
        StructType(
            mapOf(
                "a" to StaticType.unionOf(
                    StaticType.INT,
                    StaticType.NULL,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("int", true),
                                IonSchemaModel.build { occurs(occursRequired()) }
                            )
                        )
                    )
                )
            )
        ),
        "type::{ name: $typeName, type: struct, fields: { a: nullable::{ type: nullable::int, occurs: required } } }"
    ),
    // single field, inline type, optional
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct, fields: { a: { type: list, element: int } } }",
        StructType(
            mapOf(
                "a" to StaticType.unionOf(
                    ListType(
                        StaticType.INT,
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef(
                                    null,
                                    buildTypeConstraint("list"),
                                    IonSchemaModel.build { element(namedType("int", ionBool(false))) }
                                )
                            )
                        )
                    ),
                    StaticType.MISSING,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("list"),
                                IonSchemaModel.build { element(namedType("int", ionBool(false))) }
                            )
                        )
                    )
                )
            )
        )
    ),
    // single field, inline type with nullable values, optional
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct, fields: { a: { type: list, element: nullable::int } } }",
        StructType(
            mapOf(
                "a" to StaticType.unionOf(
                    ListType(
                        StaticType.unionOf(StaticType.NULL, StaticType.INT),
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef(null, buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("int", ionBool(true))) })
                            )
                        )
                    ),
                    StaticType.MISSING,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(null, buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("int", ionBool(true))) })
                        )
                    )
                )
            )
        )
    ),
    // single field, inline single type, required
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct, fields: { a: { type: int, occurs: required } } }",
        StructType(
            mapOf(
                "a" to IntType(
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(null, buildTypeConstraint("int"), IonSchemaModel.build { occurs(occursRequired()) })
                        )
                    )
                )
            )
        )
    ),
    // single field, inline collection type, required
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct, fields: { a: { type: list, element: int, occurs: required } } }",
        StructType(
            mapOf(
                "a" to ListType(
                    StaticType.INT,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("list"),
                                IonSchemaModel.build { element(namedType("int", ionBool(false))) },
                                IonSchemaModel.build { occurs(occursRequired()) }
                            )
                        )
                    )
                )
            )
        )
    ),
    // single field, multiple types allowed
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct, fields: { a : { any_of: [int, string] } } }",
        StructType(
            mapOf(
                "a" to AnyOfType(
                    setOf(
                        StaticType.INT,
                        StaticType.STRING,
                        StaticType.MISSING
                    ),
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                IonSchemaModel.build { anyOf(namedType("int", ionBool(false)), namedType("string", ionBool(false))) }
                            )
                        )
                    )
                )
            )
        )
    ),
    // same as above, with required field
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct, fields: { a : { any_of: [int, string], occurs: required } } }",
        StructType(
            mapOf(
                "a" to AnyOfType(
                    setOf(
                        StaticType.INT,
                        StaticType.STRING
                    ),
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                IonSchemaModel.build { anyOf(namedType("int", ionBool(false)), namedType("string", ionBool(false))) },
                                IonSchemaModel.build { occurs(occursRequired()) }
                            )
                        )
                    )
                )
            )
        )
    ),
    // same as above, with nullable type
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct, fields: { a : { any_of: [int, nullable::string] } } }",
        StructType(
            mapOf(
                "a" to AnyOfType(
                    setOf(
                        StaticType.INT,
                        StaticType.MISSING,
                        StaticType.unionOf(StaticType.NULL, StaticType.STRING)
                    ),
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                IonSchemaModel.build { anyOf(namedType("int", ionBool(false)), namedType("string", ionBool(true))) }
                            )
                        )
                    )
                )
            )
        ),
        "type::{ name: $typeName, type: struct, fields: { a : { any_of: [nullable::int, nullable::string] } } }"
    ),
    // same as above, with required field
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct, fields: { a : { any_of: [int, nullable::string], occurs: required } } }",
        StructType(
            mapOf(
                "a" to AnyOfType(
                    setOf(StaticType.INT, StaticType.unionOf(StaticType.NULL, StaticType.STRING)),
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                IonSchemaModel.build { anyOf(namedType("int", ionBool(false)), namedType("string", ionBool(true))) },
                                IonSchemaModel.build { occurs(occursRequired()) }
                            )
                        )
                    )
                )
            )
        ),
        "type::{ name: $typeName, type: struct, fields: { a : { any_of: [nullable::int, nullable::string], occurs: required } } }"
    ),
    // multiple fields
    MapperE2ETestCase(
        """
            type::{ name: $typeName, type: struct, fields: {
                a : int,
                b : nullable::int
            }}
        """,
        StructType(
            mapOf(
                "a" to StaticType.unionOf(StaticType.INT, StaticType.MISSING),
                "b" to StaticType.unionOf(StaticType.INT, StaticType.NULL, StaticType.MISSING)
            )
        )
    ),
    // union of named and inline types
    MapperE2ETestCase(
        """
            type::{ name: $typeName, type: struct, fields: {
                a : { any_of: [int, {type:list, element:string}] }
            }}
        """,
        StructType(
            mapOf(
                "a" to StaticType.unionOf(
                    StaticType.INT,
                    ListType(
                        StaticType.STRING,
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef(null, buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("string", ionBool(false))) })
                            )
                        )
                    ),
                    StaticType.MISSING,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                IonSchemaModel.build {
                                    anyOf(
                                        namedType("int", ionBool(false)),
                                        inlineType(
                                            buildTypeDef(null, buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("string", ionBool(false))) }),
                                            ionBool(false)
                                        )
                                    )
                                }
                            )
                        )
                    )
                )
            )
        )
    ),
    // inline type, required, with other constraints
    MapperE2ETestCase(
        """
            type::{ name: $typeName, type: struct, fields: {
                a : { type:list, element: string, container_length:5, occurs:required }
            }}
        """,
        StructType(
            mapOf(
                "a" to ListType(
                    StaticType.STRING,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("list"),
                                IonSchemaModel.build { element(namedType("string", ionBool(false))) },
                                IonSchemaModel.build { containerLength(equalsNumber(ionInt(5))) },
                                IonSchemaModel.build { occurs(occursRequired()) }
                            )
                        )
                    )
                )
            )
        )
    ),
    // inline type, optional, with other constraints
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct, fields: { a: { type: list, container_length: 5 } } }",
        StructType(
            mapOf(
                "a" to StaticType.unionOf(
                    StaticType.MISSING,
                    ListType(
                        StaticType.ANY,
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef(null, buildTypeConstraint("list"), IonSchemaModel.build { containerLength(equalsNumber(ionInt(5))) })
                            )
                        )
                    ),
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(null, buildTypeConstraint("list"), IonSchemaModel.build { containerLength(equalsNumber(ionInt(5))) })
                        )
                    )
                )
            )
        )
    ),
    // field is "any" type and should return union of AnyType and MissingType
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct, fields: { a: any } }",
        StructType(
            mapOf(
                "a" to StaticType.unionOf(StaticType.ANY, StaticType.MISSING)
            )
        ),
        "type::{ name: $typeName, type: struct, fields: { a: nullable::any } }",
    ),
    // struct without fields
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct, fields: {} }",
        StaticType.STRUCT,
        "type::{ name: $typeName, type: struct }"
    ),
    // closed content
    MapperE2ETestCase(
        "type::{ name: $typeName, type: struct, content: closed, fields: { a: int } }",
        StructType(mapOf("a" to StaticType.unionOf(StaticType.INT, StaticType.MISSING)), true)
    )
)

internal fun bagWithCustomElementTests() = listOf(
    // top-level bag and any type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: any }
            type::{ name: $typeName, type: bag, element: bar }
        """,
        BagType(
            AnyType(metas = mapOf(ISL_META_KEY to listOf(buildTypeDef("bar", buildTypeConstraint("any")))))
        ),
        """
            type::{ name: bar, type: any }
            type::{ name: $typeName, type: bag, element: nullable::bar }
        """
    ),
    // top-level bag and bool type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: bool }
            type::{ name: $typeName, type: bag, element: bar }
        """,
        BagType(
            BoolType(
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef("bar", buildTypeConstraint("bool"))
                    )
                )
            )
        )
    ),
    // top-level bag and int type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: int }
            type::{ name: $typeName, type: bag, element: bar }
        """,
        BagType(
            IntType(
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef("bar", buildTypeConstraint("int"))
                    )
                )
            )
        )
    ),
    // top-level bag and float type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: float }
            type::{ name: $typeName, type: bag, element: bar }
        """,
        BagType(
            StaticScalarType(
                CompileTimeFloatType,
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef("bar", buildTypeConstraint("float"))
                    )
                )
            )
        )
    ),
    // top-level bag and decimal type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: decimal }
            type::{ name: $typeName, type: bag, element: bar }
        """,
        BagType(
            StaticScalarType(
                DecimalType.createType(emptyList()),
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef("bar", buildTypeConstraint("decimal"))
                    )
                )
            )
        )
    ),
    // top-level bag and timestamp type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: timestamp }
            type::{ name: $typeName, type: bag, element: bar }
        """,
        BagType(
            TimestampType(
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef("bar", buildTypeConstraint("timestamp"))
                    )
                )
            )
        )
    ),
    // top-level bag and symbol type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: symbol }
            type::{ name: $typeName, type: bag, element: bar }
        """,
        BagType(
            SymbolType(
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef("bar", buildTypeConstraint("symbol"))
                    )
                )
            )
        )
    ),
    // top-level bag and string type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: string }
            type::{ name: $typeName, type: bag, element: bar }
        """,
        BagType(
            StringType(
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef("bar", buildTypeConstraint("string"))
                    )
                )
            )
        )
    ),
    // top-level bag and clob type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: clob }
            type::{ name: $typeName, type: bag, element: bar }
        """,
        BagType(
            ClobType(
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef("bar", buildTypeConstraint("clob"))
                    )
                )
            )
        )
    ),
    // top-level bag and blob type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: blob }
            type::{ name: $typeName, type: bag, element: bar }
        """,
        BagType(
            BlobType(
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef("bar", buildTypeConstraint("blob"))
                    )
                )
            )
        )
    ),
    // top-level bag and list type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: list, element: int }
            type::{ name: $typeName, type: bag, element: bar }
        """,
        BagType(
            ListType(
                StaticType.INT,
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef("bar", buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("int", ionBool(false))) })
                    )
                )
            )
        )
    ),
    // top-level bag and sexp type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: sexp, element: int }
            type::{ name: $typeName, type: bag, element: bar }
        """,
        BagType(
            SexpType(
                StaticType.INT,
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef("bar", buildTypeConstraint("sexp"), IonSchemaModel.build { element(namedType("int", ionBool(false))) })
                    )
                )
            )
        )
    ),
    // top-level bag and struct type with named field
    MapperE2ETestCase(
        """
            type::{ name: bar, type: struct, fields: { a: int } }
            type::{ name: $typeName, type: bag, element: bar }
        """,
        BagType(
            StructType(
                mapOf("a" to StaticType.unionOf(StaticType.INT, StaticType.MISSING)),
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            "bar",
                            buildTypeConstraint("struct"),
                            IonSchemaModel.build {
                                fields(field("a", namedType("int", ionBool(false))))
                            }
                        )
                    )
                )
            )
        )
    ),
    // top-level bag and struct type with inline field
    MapperE2ETestCase(
        """
            type::{ name: bar, type: struct, fields: { a: {type: list, element: string} } }
            type::{ name: $typeName, type: bag, element: bar }
        """,
        BagType(
            StructType(
                mapOf(
                    "a" to StaticType.unionOf(
                        StaticType.MISSING,
                        ListType(
                            StaticType.STRING,
                            metas = mapOf(
                                ISL_META_KEY to listOf(
                                    buildTypeDef(null, buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("string", ionBool(false))) })
                                )
                            )
                        ),
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef(null, buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("string", ionBool(false))) })
                            )
                        )
                    )
                ),
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            "bar", buildTypeConstraint("struct"),
                            IonSchemaModel.build {
                                fields(
                                    field(
                                        "a",
                                        inlineType(
                                            buildTypeDef(
                                                null,
                                                buildTypeConstraint("list"), element(namedType("string", ionBool(false)))
                                            ),
                                            ionBool(false)
                                        )
                                    )
                                )
                            }
                        )
                    )
                )
            )
        )
    ),
    // element as named top-level type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: string }
            type::{ name: $typeName, type: bag, element: bar }
        """,
        BagType(
            StringType(
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef("bar", buildTypeConstraint("string"))
                    )
                )
            )
        )
    ),
    // element as inline top-level type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: string }
            type::{ name: $typeName, type: bag, element: { type: bar } }
        """,
        BagType(
            StringType(
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef("bar", buildTypeConstraint("string")),
                        buildTypeDef(null, buildTypeConstraint("bar"))
                    )
                )
            )
        ),
        """
            type::{ name: bar, type: string }
            type::{ name: $typeName, type: bag, element: bar }
        """
    ),
    // element as nullable, named top-level type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: string }
            type::{ name: $typeName, type: bag, element: nullable::bar }
        """,
        BagType(
            StaticType.unionOf(
                StaticType.NULL,
                StringType(
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef("bar", buildTypeConstraint("string"))
                        )
                    )
                ),
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef("bar", buildTypeConstraint("string"))
                    )
                )
            )
        )
    ),
    // Same as above, with more constraints on the custom type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: string, codepoint_length: 5, utf8_byte_length: 5 }
            type::{ name: $typeName, type: bag, element: nullable::bar }
        """,
        BagType(
            StaticType.unionOf(
                StaticType.NULL,
                CharType(
                    5,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                "bar",
                                buildTypeConstraint("string"),
                                IonSchemaModel.build { codepointLength(equalsNumber(ionInt(5))) },
                                IonSchemaModel.build { utf8ByteLength(equalsNumber(ionInt(5))) }
                            )
                        )
                    )
                ),
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            "bar",
                            buildTypeConstraint("string"),
                            IonSchemaModel.build { codepointLength(equalsNumber(ionInt(5))) },
                            IonSchemaModel.build { utf8ByteLength(equalsNumber(ionInt(5))) }
                        )
                    )
                )
            )
        )
    )
)

internal fun structWithCustomFieldTests() = listOf(
    // custom type is scalar type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: int }
            type::{ name: $typeName, type: struct, fields: { a : bar } }
        """,
        StructType(
            mapOf(
                "a" to StaticType.unionOf(
                    IntType(metas = mapOf(ISL_META_KEY to listOf(buildTypeDef("bar", buildTypeConstraint("int"))))),
                    StaticType.MISSING,
                    metas = mapOf(ISL_META_KEY to listOf(buildTypeDef("bar", buildTypeConstraint("int"))))
                )
            )
        )
    ),
    // Result of a CAST to a custom type, optional
    // Custom type is scalar type with additional constraints
    MapperE2ETestCase(
        """
            type::{ name: bar, type: string, utf8_byte_length: range::[1,2048]}
            type::{ name: $typeName, type: struct, fields: { a : bar }}
        """,
        StructType(
            mapOf(
                "a" to asOptional(
                    StringType(
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef(
                                    "bar",
                                    buildTypeConstraint("string"),
                                    IonSchemaModel.build {
                                        utf8ByteLength(
                                            equalsRange(
                                                numberRange(inclusive(ionInt(1)), inclusive(ionInt(2048)))
                                            )
                                        )
                                    }
                                )
                            )
                        )
                    )
                ).withMetas(
                    mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                "bar",
                                buildTypeConstraint("string"),
                                IonSchemaModel.build {
                                    utf8ByteLength(
                                        equalsRange(
                                            numberRange(inclusive(ionInt(1)), inclusive(ionInt(2048)))
                                        )
                                    )
                                }
                            )
                        )
                    )
                )
            )
        )
    ),
    // required field, custom type is scalar type without additional constraints
    MapperE2ETestCase(
        """
            type::{ name: bar, type: int }
            type::{ name: $typeName, type: struct, fields: { a : { type: bar, occurs: required } } }
        """,
        StructType(
            mapOf(
                "a" to IntType(
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef("bar", buildTypeConstraint("int")),
                            buildTypeDef(null, buildTypeConstraint("bar"), IonSchemaModel.build { occurs(occursRequired()) })
                        )
                    )
                )
            )
        )
    ),
    // Result of a CAST to a custom type
    // Same as above, but custom type is a scalar type with additional constraints
    MapperE2ETestCase(
        """
            type::{ name: bar, type: string, utf8_byte_length: range::[1,2048]}
            type::{ name: $typeName, type: struct, fields: { a : { type: bar, occurs: required } } }
        """,
        StructType(
            mapOf(
                "a" to StringType(
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                "bar",
                                buildTypeConstraint("string"),
                                IonSchemaModel.build {
                                    utf8ByteLength(
                                        equalsRange(
                                            numberRange(inclusive(ionInt(1)), inclusive(ionInt(2048)))
                                        )
                                    )
                                }
                            ),
                            buildTypeDef(null, buildTypeConstraint("bar"), IonSchemaModel.build { occurs(occursRequired()) })
                        )
                    )
                )
            )
        )
    ),
    // Result of a CAST to a custom type, nullable
    MapperE2ETestCase(
        """
            type::{ name: bar, type: string, utf8_byte_length: range::[1,2048]}
            type::{ name: $typeName, type: struct, fields: { a : {type: nullable::bar, occurs: required} } }
        """,
        StructType(
            mapOf(
                "a" to asNullable(
                    StringType(
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef(
                                    "bar",
                                    buildTypeConstraint("string"),
                                    IonSchemaModel.build {
                                        utf8ByteLength(
                                            equalsRange(
                                                numberRange(inclusive(ionInt(1)), inclusive(ionInt(2048)))
                                            )
                                        )
                                    }
                                )
                            )
                        )
                    )
                ).withMetas(
                    mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                "bar",
                                buildTypeConstraint("string"),
                                IonSchemaModel.build {
                                    utf8ByteLength(
                                        equalsRange(
                                            numberRange(inclusive(ionInt(1)), inclusive(ionInt(2048)))
                                        )
                                    )
                                }
                            ),
                            buildTypeDef(null, buildTypeConstraint("bar", true), IonSchemaModel.build { occurs(occursRequired()) })
                        )
                    )
                )
            )
        ),
        """
            type::{ name: bar, type: string, utf8_byte_length: range::[1,2048]}
            type::{ name: $typeName, type: struct, fields: { a : nullable::{type: nullable::bar, occurs: required} } }
        """
    ),
    // Result of a CAST to a custom type, nullable and optional
    MapperE2ETestCase(
        """
            type::{ name: bar, type: string, utf8_byte_length: range::[1,2048]}
            type::{ name: $typeName, type: struct, fields: { a : nullable::bar }}
        """,
        StructType(
            mapOf(
                "a" to asOptional(
                    asNullable(
                        StringType(
                            metas = mapOf(
                                ISL_META_KEY to listOf(
                                    buildTypeDef(
                                        "bar",
                                        buildTypeConstraint("string"),
                                        IonSchemaModel.build {
                                            utf8ByteLength(
                                                equalsRange(
                                                    numberRange(inclusive(ionInt(1)), inclusive(ionInt(2048)))
                                                )
                                            )
                                        }
                                    )
                                )
                            )
                        )
                    )
                ).withMetas(
                    mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                "bar",
                                buildTypeConstraint("string"),
                                IonSchemaModel.build {
                                    utf8ByteLength(
                                        equalsRange(
                                            numberRange(inclusive(ionInt(1)), inclusive(ionInt(2048)))
                                        )
                                    )
                                }
                            )
                        )
                    )
                )
            )
        )
    ),
    // custom type is collection type
    MapperE2ETestCase(
        """
            type::{ name: cat, type: list, element: int }
            type::{ name: $typeName, type: struct, fields: { b: cat } }
        """,
        StructType(
            mapOf(
                "b" to StaticType.unionOf(
                    ListType(
                        StaticType.INT,
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef("cat", buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("int", ionBool(false))) })
                            )
                        )
                    ),
                    StaticType.MISSING,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef("cat", buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("int", ionBool(false))) })
                        )
                    )
                )
            )
        )
    ),
    // nullable field, custom type is scalar type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: int }
            type::{ name: $typeName, type: struct, fields: { c: nullable::bar } }
        """,
        StructType(
            mapOf(
                "c" to StaticType.unionOf(
                    IntType(metas = mapOf(ISL_META_KEY to listOf(buildTypeDef("bar", buildTypeConstraint("int"))))),
                    StaticType.NULL,
                    StaticType.MISSING,
                    metas = mapOf(ISL_META_KEY to listOf(buildTypeDef("bar", buildTypeConstraint("int"))))
                )
            )
        )
    ),
    // nullable field, custom type is collection type
    MapperE2ETestCase(
        """
            type::{ name: cat, type: list, element: int }
            type::{ name: $typeName, type: struct, fields: { d: nullable::cat } }
        """,
        StructType(
            mapOf(
                "d" to StaticType.unionOf(
                    ListType(
                        StaticType.INT,
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef("cat", buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("int", ionBool(false))) })
                            )
                        )
                    ),
                    StaticType.NULL,
                    StaticType.MISSING,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef("cat", buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("int", ionBool(false))) })
                        )
                    )
                )
            )
        )
    ),
    // required field, custom type is collection type
    MapperE2ETestCase(
        """
            type::{ name: bar, type: list, element: int }
            type::{ name: $typeName, type: struct, fields: { a : { type: bar, occurs: required } } }
        """,
        StructType(
            mapOf(
                "a" to ListType(
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef("bar", buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("int", ionBool(false))) }),
                            buildTypeDef(null, buildTypeConstraint("bar"), IonSchemaModel.build { occurs(occursRequired()) })
                        )
                    )
                )
            )
        )
    ),
    MapperE2ETestCase(
        """
            type::{ name: bar, type: string }
            type::{ name: $typeName, type: struct, fields: {
                g: { type: list, element: bar }
            }}
        """,
        StructType(
            mapOf(
                "g" to StaticType.unionOf(
                    StaticType.MISSING,
                    ListType(
                        StringType(
                            metas = mapOf(
                                ISL_META_KEY to listOf(
                                    buildTypeDef("bar", buildTypeConstraint("string"))
                                )
                            )
                        ),
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef("bar", buildTypeConstraint("string")),
                                buildTypeDef(
                                    null,
                                    buildTypeConstraint("list"),
                                    IonSchemaModel.build { element(namedType("bar", ionBool(false))) }
                                )
                            )
                        )
                    ),
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef("bar", buildTypeConstraint("string")),
                            buildTypeDef(
                                null,
                                buildTypeConstraint("list"),
                                IonSchemaModel.build { element(namedType("bar", ionBool(false))) }
                            )
                        )
                    )
                )
            )
        )
    ),
    MapperE2ETestCase(
        """
            type::{ name: bar, type: string }
            type::{ name: $typeName, type: struct, fields: {
                h: { type: list, element: bar, occurs: required }
            }}
        """,
        StructType(
            mapOf(
                "h" to ListType(
                    StringType(
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef("bar", buildTypeConstraint("string"))
                            )
                        )
                    ),
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef("bar", buildTypeConstraint("string")),
                            buildTypeDef(
                                null,
                                buildTypeConstraint("list"),
                                IonSchemaModel.build { element(namedType("bar", ionBool(false))) },
                                IonSchemaModel.build { occurs(occursRequired()) }
                            )
                        )
                    )
                )
            )
        )
    ),
    // any_of types with custom types
    MapperE2ETestCase(
        """
            type::{ name: bar, type: int }
            type::{ name: cat, type: list, element: int }
            type::{ name: $typeName, type: struct, fields: { h: { any_of: [bar,cat] } } }
        """,
        StructType(
            mapOf(
                "h" to StaticType.unionOf(
                    IntType(
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef("bar", buildTypeConstraint("int"))
                            )
                        )
                    ),
                    ListType(
                        StaticType.INT,
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef("cat", buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("int", ionBool(false))) })
                            )
                        )
                    ),
                    StaticType.MISSING,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef("bar", buildTypeConstraint("int")),
                            buildTypeDef("cat", buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("int", ionBool(false))) }),
                            buildTypeDef(
                                null,
                                IonSchemaModel.build {
                                    anyOf(
                                        namedType("bar", ionBool(false)),
                                        namedType("cat", ionBool(false))
                                    )
                                }
                            )
                        )
                    )
                )
            )
        )
    ),
    // Same as above, with required field
    MapperE2ETestCase(
        """
            type::{ name: bar, type: int }
            type::{ name: cat, type: list, element: int }
            type::{ name: $typeName, type: struct, fields: { g: { any_of:[bar,cat], occurs: required } } }
        """,
        StructType(
            mapOf(
                "g" to StaticType.unionOf(
                    IntType(
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef("bar", buildTypeConstraint("int"))
                            )
                        )
                    ),
                    ListType(
                        StaticType.INT,
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef("cat", buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("int", ionBool(false))) })
                            )
                        )
                    ),
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef("bar", buildTypeConstraint("int")),
                            buildTypeDef("cat", buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("int", ionBool(false))) }),
                            buildTypeDef(
                                null,
                                IonSchemaModel.build {
                                    anyOf(
                                        namedType("bar", ionBool(false)),
                                        namedType("cat", ionBool(false))
                                    )
                                },
                                IonSchemaModel.build { occurs(occursRequired()) }
                            )
                        )
                    )
                )
            )
        )
    ),
    // Field with custom type and additional constraints
    MapperE2ETestCase(
        """
            type::{ name: bar, type: int }
            type::{ name: $typeName, type: struct, fields: { a : { type: bar, annotations: ['my_int'] } } }
        """,
        StructType(
            mapOf(
                "a" to StaticType.unionOf(
                    IntType(
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef("bar", buildTypeConstraint("int")),
                                buildTypeDef(null, buildTypeConstraint("bar"), IonSchemaModel.build { annotations(ionBool(false).toIonElement(), annotationList(annotation("my_int"))) })
                            )
                        )
                    ),
                    StaticType.MISSING,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef("bar", buildTypeConstraint("int")),
                            buildTypeDef(null, buildTypeConstraint("bar"), IonSchemaModel.build { annotations(ionBool(false).toIonElement(), annotationList(annotation("my_int"))) })
                        )
                    )
                )
            )
        )
    ),
    // Same as above, with required field
    MapperE2ETestCase(
        """
            type::{ name: bar, type: int }
            type::{ name: $typeName, type: struct, fields: { a : { type: bar, annotations: ['my_int'], occurs: required } } }
        """,
        StructType(
            mapOf(
                "a" to IntType(
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef("bar", buildTypeConstraint("int")),
                            buildTypeDef(
                                null,
                                buildTypeConstraint("bar"),
                                IonSchemaModel.build { annotations(ionBool(false).toIonElement(), annotationList(annotation("my_int"))) },
                                IonSchemaModel.build { occurs(occursRequired()) }
                            )
                        )
                    )
                )
            )
        )
    ),
    // any_of types with mix of core and custom types
    MapperE2ETestCase(
        """
            type::{ name: bar, type: int }
            type::{ name: $typeName, type: struct, fields: { a : { any_of: [string, bar] } } }
        """,
        StructType(
            mapOf(
                "a" to StaticType.unionOf(
                    StaticType.STRING,
                    IntType(metas = mapOf(ISL_META_KEY to listOf(buildTypeDef("bar", buildTypeConstraint("int"))))),
                    StaticType.MISSING,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef("bar", buildTypeConstraint("int")),
                            buildTypeDef(
                                null,
                                IonSchemaModel.build {
                                    anyOf(
                                        namedType("string", ionBool(false)),
                                        namedType("bar", ionBool(false))
                                    )
                                }
                            )
                        )
                    )
                )
            )
        )
    ),
    // Same as above, with required field
    MapperE2ETestCase(
        """
            type::{ name: bar, type: int }
            type::{ name: $typeName, type: struct, fields: { a : { any_of: [string, bar], occurs: required } } }
        """,
        StructType(
            mapOf(
                "a" to StaticType.unionOf(
                    StaticType.STRING,
                    IntType(metas = mapOf(ISL_META_KEY to listOf(buildTypeDef("bar", buildTypeConstraint("int"))))),
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef("bar", buildTypeConstraint("int")),
                            buildTypeDef(
                                null,
                                IonSchemaModel.build {
                                    anyOf(
                                        namedType("string", ionBool(false)),
                                        namedType("bar", ionBool(false))
                                    )
                                },
                                IonSchemaModel.build { occurs(occursRequired()) }
                            )
                        )
                    )
                )
            )
        )
    ),
    // nullable, optional field, custom type with constraints
    MapperE2ETestCase(
        """
            type::{ name: bar, type: string, codepoint_length: 5, utf8_byte_length: 5 }
            type::{ name: $typeName, type: struct, fields: { a: nullable::bar } }
        """,
        StructType(
            mapOf(
                "a" to StaticType.unionOf(
                    CharType(
                        5,
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef(
                                    "bar", buildTypeConstraint("string"),
                                    IonSchemaModel.build { codepointLength(equalsNumber(ionInt(5))) },
                                    IonSchemaModel.build { utf8ByteLength(equalsNumber(ionInt(5))) }
                                )
                            )
                        )
                    ),
                    StaticType.NULL,
                    StaticType.MISSING,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                "bar", buildTypeConstraint("string"),
                                IonSchemaModel.build { codepointLength(equalsNumber(ionInt(5))) },
                                IonSchemaModel.build { utf8ByteLength(equalsNumber(ionInt(5))) }
                            )
                        )
                    )
                )
            )
        )
    ),
    // required field, custom type with constraints
    MapperE2ETestCase(
        """
            type::{ name: bar, type: string, codepoint_length: 5, utf8_byte_length: 5 }
            type::{ name: $typeName, type: struct, fields: { a: { type: bar, occurs: required } } }
        """,
        StructType(
            mapOf(
                "a" to CharType(
                    5,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                "bar", buildTypeConstraint("string"),
                                IonSchemaModel.build { codepointLength(equalsNumber(ionInt(5))) },
                                IonSchemaModel.build { utf8ByteLength(equalsNumber(ionInt(5))) }
                            ),
                            buildTypeDef(null, buildTypeConstraint("bar"), IonSchemaModel.build { occurs(occursRequired()) })
                        )
                    )
                )
            )
        ),
        """
            type::{ name: bar, type: string, codepoint_length: 5, utf8_byte_length: 5 }
            type::{ name: $typeName, type: struct, fields: { a: { type: bar, codepoint_length: 5, occurs: required } } }
        """
    ),
    // nullable, required field, custom type with constraints
    MapperE2ETestCase(
        """
            type::{ name: bar, type: string, codepoint_length: 5, utf8_byte_length: 5 }
            type::{ name: $typeName, type: struct, fields: { a: { type: nullable::bar, occurs: required } } }
        """,
        StructType(
            mapOf(
                "a" to StaticType.unionOf(
                    CharType(
                        5,
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef(
                                    "bar", buildTypeConstraint("string"),
                                    IonSchemaModel.build { codepointLength(equalsNumber(ionInt(5))) },
                                    IonSchemaModel.build { utf8ByteLength(equalsNumber(ionInt(5))) }
                                )
                            )
                        )
                    ),
                    StaticType.NULL,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                "bar", buildTypeConstraint("string"),
                                IonSchemaModel.build { codepointLength(equalsNumber(ionInt(5))) },
                                IonSchemaModel.build { utf8ByteLength(equalsNumber(ionInt(5))) }
                            ),
                            buildTypeDef(null, buildTypeConstraint("bar", true), IonSchemaModel.build { occurs(occursRequired()) })
                        )
                    )
                )
            )
        ),
        """
            type::{ name: bar, type: string, codepoint_length: 5, utf8_byte_length: 5 }
            type::{ name: $typeName, type: struct, fields: { a: nullable::{ type: nullable::bar, occurs: required } } }
        """
    ),
    // recursive type
    MapperE2ETestCase(
        """
            type::{ name: bar, any_of: [string, {type: list, element: bar}] }
            type::{ name: $typeName, type: struct, fields: { a: {type: bar, occurs: required} } }
        """,
        StructType(
            mapOf(
                "a" to StaticType.unionOf(
                    StaticType.STRING,
                    ListType(
                        StaticType.ANY,
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef(
                                    "bar",
                                    IonSchemaModel.build {
                                        anyOf(
                                            namedType("string", ionBool(false)),
                                            inlineType(
                                                buildTypeDef(null, buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("bar", ionBool(false))) }), ionBool(false)
                                            )
                                        )
                                    }
                                ),
                                buildTypeDef(null, buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("bar", ionBool(false))) })
                            )
                        )
                    ),
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                "bar",
                                IonSchemaModel.build {
                                    anyOf(
                                        namedType("string", ionBool(false)),
                                        inlineType(
                                            buildTypeDef(null, buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("bar", ionBool(false))) }), ionBool(false)
                                        )
                                    )
                                }
                            ),
                            buildTypeDef(null, buildTypeConstraint("bar"), IonSchemaModel.build { occurs(occursRequired()) })
                        )
                    )
                )
            )
        ),
        """
            type::{ name: bar, any_of: [string, {type: list, element: bar}] }
            type::{ name: $typeName, type: struct, fields: { a: {any_of:[string, {type: list, element: bar}], occurs: required} } }
        """
    ),
    // same as above, but optional
    MapperE2ETestCase(
        """
            type::{ name: bar, any_of: [string, {type: list, element: bar}] }
            type::{ name: $typeName, type: struct, fields: { a: bar } }
        """,
        StructType(
            mapOf(
                "a" to StaticType.unionOf(
                    StaticType.STRING,
                    ListType(
                        StaticType.ANY,
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef(
                                    "bar",
                                    IonSchemaModel.build {
                                        anyOf(
                                            namedType("string", ionBool(false)),
                                            inlineType(
                                                buildTypeDef(null, buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("bar", ionBool(false))) }), ionBool(false)
                                            )
                                        )
                                    }
                                ),
                                buildTypeDef(null, buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("bar", ionBool(false))) })
                            )
                        )
                    ),
                    StaticType.MISSING,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                "bar",
                                IonSchemaModel.build {
                                    anyOf(
                                        namedType("string", ionBool(false)),
                                        inlineType(
                                            buildTypeDef(null, buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("bar", ionBool(false))) }), ionBool(false)
                                        )
                                    )
                                }
                            )
                        )
                    )
                )
            )
        )
    ),
    // same as above, but nullable too
    MapperE2ETestCase(
        """
            type::{ name: bar, any_of: [string, {type: list, element: bar}] }
            type::{ name: $typeName, type: struct, fields: { a: nullable::bar } }
        """,
        StructType(
            mapOf(
                "a" to StaticType.unionOf(
                    StaticType.STRING,
                    ListType(
                        StaticType.ANY,
                        metas = mapOf(
                            ISL_META_KEY to listOf(
                                buildTypeDef(
                                    "bar",
                                    IonSchemaModel.build {
                                        anyOf(
                                            namedType("string", ionBool(false)),
                                            inlineType(
                                                buildTypeDef(null, buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("bar", ionBool(false))) }), ionBool(false)
                                            )
                                        )
                                    }
                                ),
                                buildTypeDef(null, buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("bar", ionBool(false))) })
                            )
                        )
                    ),
                    StaticType.NULL,
                    StaticType.MISSING,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                "bar",
                                IonSchemaModel.build {
                                    anyOf(
                                        namedType("string", ionBool(false)),
                                        inlineType(
                                            buildTypeDef(null, buildTypeConstraint("list"), IonSchemaModel.build { element(namedType("bar", ionBool(false))) }), ionBool(false)
                                        )
                                    )
                                }
                            )
                        )
                    )
                )
            )
        )
    )
)

internal fun stringTests() = listOf(
    MapperE2ETestCase(
        "type::{ name: $typeName, type: string }",
        StaticType.STRING
    ),
    MapperE2ETestCase(
        "type::{ name: $typeName, type: string, codepoint_length: 5 }",
        CharType(5)
    ),
    MapperE2ETestCase(
        "type::{ name: $typeName, type: nullable::{type: string, codepoint_length: 5} }",
        StaticType.unionOf(
            CharType(
                5,
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            buildTypeConstraint("string"),
                            IonSchemaModel.build { codepointLength(equalsNumber(ionInt(5))) }
                        )
                    )
                )
            ),
            StaticType.NULL
        )
    ),
    MapperE2ETestCase(
        "type::{ name: $typeName, type: string, codepoint_length: range::[0, 5] }",
        VarcharType(5)
    ),
    // nullable string with constraints
    MapperE2ETestCase(
        "type::{ name: $typeName, type: nullable::{type: string, codepoint_length: range::[0,5]} }",
        AnyOfType(
            setOf(
                StaticType.NULL,
                VarcharType(
                    5,
                    metas = mapOf(
                        ISL_META_KEY to listOf(
                            buildTypeDef(
                                null,
                                buildTypeConstraint("string"),
                                IonSchemaModel.build { codepointLength(equalsRange(numberRange(inclusive(ionInt(0)), inclusive(ionInt(5))))) }
                            )
                        )
                    )
                )
            )
        )
    ),
    MapperE2ETestCase(
        "type::{ name: $typeName, type: string, codepoint_length: range::[exclusive::-1, 5] }",
        VarcharType(5),
        "type::{ name: $typeName, type: string, codepoint_length: range::[0, 5] }"
    ),
    MapperE2ETestCase(
        "type::{ name: $typeName, type: string, codepoint_length: range::[0, exclusive::5] }",
        VarcharType(4),
        "type::{ name: $typeName, type: string, codepoint_length: range::[0, 4] }"
    ),
    MapperE2ETestCase(
        "type::{ name: $typeName, type: string, codepoint_length: range::[min, 5] }",
        VarcharType(5),
        "type::{ name: $typeName, type: string, codepoint_length: range::[0, 5] }"
    ),
    MapperE2ETestCase(
        "type::{ name: $typeName, type: string, codepoint_length: range::[min, max] }",
        StaticType.STRING,
        "type::{ name: $typeName, type: string }"
    ),
    MapperE2ETestCase(
        "type::{ name: $typeName, type: string, codepoint_length: range::[1, 2048] }",
        StringType(
            metas = mapOf(
                ISL_META_KEY to listOf(
                    buildTypeDef(
                        typeName,
                        buildTypeConstraint("string"),
                        IonSchemaModel.build { codepointLength(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(2048))))) }
                    )
                )
            )
        )
    )
)

internal fun intTests() = listOf(
    MapperE2ETestCase(
        "type::{ name: $typeName, type: int }",
        StaticType.INT
    ),
    MapperE2ETestCase(
        "type::{ name: $typeName, type: int, valid_values: range::[${Short.MIN_VALUE}, ${Short.MAX_VALUE}] }",
        StaticType.INT2
    ),
    MapperE2ETestCase(
        "type::{ name: $typeName, type: int, valid_values: range::[${Int.MIN_VALUE}, ${Int.MAX_VALUE}] }",
        StaticType.INT4
    ),
    MapperE2ETestCase(
        "type::{ name: $typeName, type: int, valid_values: range::[${Long.MIN_VALUE}, ${Long.MAX_VALUE}] }",
        StaticType.INT8
    ),
    // nullable int with constraints
    MapperE2ETestCase(
        "type::{ name: $typeName, type: nullable::{type: int, valid_values: range::[${Long.MIN_VALUE},${Long.MAX_VALUE}]} }",
        StaticType.unionOf(
            Int8Type(
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            buildTypeConstraint("int"),
                            IonSchemaModel.build {
                                validValues(
                                    rangeOfValidValues(
                                        numRange(
                                            numberRange(
                                                inclusive(ionInt(Long.MIN_VALUE)),
                                                inclusive(ionInt(Long.MAX_VALUE))
                                            )
                                        )
                                    )
                                )
                            }
                        )
                    )
                )
            ),
            StaticType.NULL
        )
    )
)

internal fun decimalTests() = listOf(
    MapperE2ETestCase(
        "type::{ name: $typeName, type: decimal }",
        StaticScalarType(DecimalType.createType(emptyList()))
    ),
    MapperE2ETestCase(
        "type::{ name: $typeName, type: decimal, precision: range::[1, 10] }",
        StaticScalarType(DecimalType.createType(listOf(10))),
        "type::{ name: $typeName, type: decimal, precision: range::[1, 10], scale: 0 }"
    ),
    // precision of exactly 1
    MapperE2ETestCase(
        "type::{ name: $typeName, type: decimal, precision: 1 }",
        StaticScalarType(DecimalType.createType(listOf(1))),
        "type::{ name: $typeName, type: decimal, precision: 1, scale: 0 }"
    ),
    // precision range min to 10
    MapperE2ETestCase(
        "type::{ name: $typeName, type: decimal, precision: range::[min, 10] }",
        StaticScalarType(DecimalType.createType(listOf(10))),
        "type::{ name: $typeName, type: decimal, precision: range::[1, 10], scale: 0 }"
    ),
    // precision range 1 to 10 and non-zero scale
    MapperE2ETestCase(
        "type::{ name: $typeName, type: decimal, precision: range::[1, 10], scale: 5 }",
        StaticScalarType(DecimalType.createType(listOf(10, 5)))
    ),
    // precision range 1 to max
    MapperE2ETestCase(
        "type::{ name: $typeName, type: decimal, precision: range::[1, max], scale: 5 }",
        StaticScalarType(DecimalType.createType(emptyList())),
        "type::{ name: $typeName, type: decimal }"
    ),
    // precision range 0 (exclusive) to 10 (inclusive)
    MapperE2ETestCase(
        "type::{ name: $typeName, type: decimal, precision: range::[exclusive::0, 10] }",
        StaticScalarType(DecimalType.createType(listOf(10))),
        "type::{ name: $typeName, type: decimal, precision: range::[1, 10], scale: 0 }"
    ),
    // precision range 0 (exclusive) to 10 (exclusive)
    MapperE2ETestCase(
        "type::{ name: $typeName, type: decimal, precision: range::[exclusive::0, exclusive::10] }",
        StaticScalarType(DecimalType.createType(listOf(9))),
        "type::{ name: $typeName, type: decimal, precision: range::[1, 9], scale: 0 }"
    ),
    MapperE2ETestCase(
        "type::{ name: $typeName, type: nullable::{type: decimal, precision: range::[1, 10], scale: 5} }",
        StaticType.unionOf(
            StaticScalarType(
                DecimalType.createType(listOf(10, 5)),
                metas = mapOf(
                    ISL_META_KEY to listOf(
                        buildTypeDef(
                            null,
                            buildTypeConstraint("decimal"),
                            IonSchemaModel.build { precision(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(10))))) },
                            IonSchemaModel.build { scale(equalsNumber(ionInt(5))) }
                        )
                    )
                )
            ),
            StaticType.NULL
        )
    ),
    MapperE2ETestCase(
        "type::{ name: $typeName, type: decimal, precision: range::[1,47], scale: range::[1,37] }",
        StaticScalarType(
            DecimalType.createType(emptyList()),
            metas = mapOf(
                ISL_META_KEY to listOf(
                    buildTypeDef(
                        typeName,
                        buildTypeConstraint("decimal"),
                        IonSchemaModel.build { precision(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(47))))) },
                        IonSchemaModel.build { scale(equalsRange(numberRange(inclusive(ionInt(1)), inclusive(ionInt(37))))) }
                    )
                )
            )
        )
    )
)

/* TODO: The following use cases are not supported yet */
/*
internal fun unsupportedTests() = listOf(
    // https://github.com/partiql/partiql-lang-kotlin/issues/503
    // Field has any_of constraint (with or without custom types) and additional constraints.
    MapperE2ETestCase(
        """
            type::{ name: $typeName, type: struct, fields: {
                a: { any_of: [string, symbol], valid_values: ["a", 'b'] }
            }}
        """,
        StructType(mapOf(
            "a" to StaticType.unionOf(
                StaticType.STRING,
                StaticType.SYMBOL,
                StaticType.MISSING,
                metas = mapOf(ISL_META_KEY to listOf(
                    buildTypeDef(null,
                        IonSchemaModel.build { anyOf(
                            namedType("string", ionBool(false)),
                            namedType("symbol", ionBool(false))
                        )},
                        IonSchemaModel.build { validValues(
                            oneOfValidValues(ionString("a").asAnyElement(), ionSymbol("b").asAnyElement())
                        ) }
                    )
                ))
            )
        ))
    ),
    // https://github.com/partiql/partiql-lang-kotlin/issues/503
    // same as above, with required field
    MapperE2ETestCase(
        """
            type::{ name: $typeName, type: struct, fields: {
                a: { any_of: [string, symbol], valid_values: ["a", 'b'], occurs: required }
            }}
        """,
        StructType(mapOf(
            "a" to StaticType.unionOf(
                StaticType.STRING,
                StaticType.SYMBOL,
                metas = mapOf(ISL_META_KEY to listOf(
                    buildTypeDef(null,
                        IonSchemaModel.build { anyOf(
                            namedType("string", ionBool(false)),
                            namedType("symbol", ionBool(false))
                        )},
                        IonSchemaModel.build { validValues(
                            oneOfValidValues(ionString("a").asAnyElement(), ionSymbol("b").asAnyElement())
                        )},
                        IonSchemaModel.build { occurs(occursRequired()) }
                    )
                ))
            )
        ))
    ),
    // https://github.com/partiql/partiql-lang-kotlin/issues/504
    // Field with nullable, custom type and additional constraints
    MapperE2ETestCase(
        """
            type::{ name: bar, type: int }
            type::{ name: $typeName, type: struct, fields: { a : nullable::{ type: nullable::bar, annotations: ['my_int'] } } }
        """,
        StructType(mapOf(
            "a" to StaticType.unionOf(
                IntType(metas = mapOf(ISL_META_KEY to listOf(
                    buildTypeDef("bar", buildTypeConstraint("int"))
                ))),
                StaticType.NULL,
                StaticType.MISSING,
                metas = mapOf(ISL_META_KEY to listOf(
                    buildTypeDef("bar", buildTypeConstraint("int")),
                    buildTypeDef(null, buildTypeConstraint("bar", true), IonSchemaModel.build { annotations(ionBool(false).toIonElement(), annotationList(annotation("my_int"))) })
                ))
            )
        ))
    ),
    // https://github.com/partiql/partiql-lang-kotlin/issues/504
    // Same as above, with required field
    MapperE2ETestCase(
        """
            type::{ name: bar, type: int }
            type::{ name: $typeName, type: struct, fields: { a : { type: nullable::bar, annotations: ['my_int'], occurs: required } } }
        """,
        StructType(mapOf(
            "a" to StaticType.unionOf(
                IntType(metas = mapOf(ISL_META_KEY to listOf(
                    buildTypeDef("bar", buildTypeConstraint("int"))
                ))),
                StaticType.NULL,
                metas = mapOf(ISL_META_KEY to listOf(
                    buildTypeDef("bar", buildTypeConstraint("int")),
                    buildTypeDef(
                        null,
                        buildTypeConstraint("bar", true),
                        IonSchemaModel.build { annotations(ionBool(false).toIonElement(), annotationList(annotation("my_int"))) },
                        IonSchemaModel.build { occurs(occursRequired()) }
                    )
                ))
            )
        ))
    )
)
*/
