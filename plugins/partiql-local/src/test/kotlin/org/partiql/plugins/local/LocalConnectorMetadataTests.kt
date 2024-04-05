package org.partiql.plugins.local

import org.junit.jupiter.api.Test
import org.partiql.shape.PShape
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorPath
import org.partiql.types.BagType
import org.partiql.types.IntType
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import java.nio.file.Paths
import kotlin.test.assertEquals

class LocalConnectorMetadataTests {

    private val catalogUrl =
        LocalConnectorMetadataTests::class.java.classLoader.getResource("catalogs/local") ?: error("Couldn't be found")

    private val metadata = LocalConnector.Metadata(Paths.get(catalogUrl.path))

    @Test
    fun getTable() {
        // Prepare
        val requested = BindingPath(
            listOf(
                BindingName("data", BindingCase.INSENSITIVE),
                BindingName("records", BindingCase.INSENSITIVE),
            )
        )
        val expected = BagType(
            StructType(
                fields = mapOf(
                    "id" to StaticType.INT,
                    "path" to StaticType.STRING
                ),
                contentClosed = true,
                constraints = setOf(TupleConstraint.Ordered, TupleConstraint.Open(false))
            )
        )

        // Act
        val handle = metadata.getObject(requested)!!
        val descriptor = handle.entity.getType()

        // Assert
        assert(requested.matches(handle.path))
        assert(PShape.fromStaticType(expected) == descriptor) {
            buildString {
                appendLine("Expected: $expected")
                appendLine("Actual: $descriptor")
            }
        }
    }

    @Test
    fun getStruct() {
        // Prepare
        val requested = BindingPath(
            listOf(
                BindingName("data", BindingCase.INSENSITIVE),
                BindingName("struct", BindingCase.INSENSITIVE),
                BindingName("nested", BindingCase.INSENSITIVE),
            )
        )
        val expectedPath = ConnectorPath.of("data", "struct")
        val expected =
            StructType(
                contentClosed = true,
                fields = mapOf(
                    "id" to IntType(),
                    "nested" to StructType(
                        contentClosed = true,
                        fields = mapOf(
                            "nested_id" to IntType()
                        ),
                        constraints = setOf(
                            TupleConstraint.Open(false),
                            TupleConstraint.UniqueAttrs(true),
                            TupleConstraint.Ordered
                        )
                    )
                ),
                constraints = setOf(
                    TupleConstraint.Open(false),
                    TupleConstraint.UniqueAttrs(true),
                    TupleConstraint.Ordered
                )
            )

        // Act
        val handle = metadata.getObject(requested)!!
        val descriptor = handle.entity.getType()

        // Assert
        assertEquals(expectedPath, handle.path)
        assert(PShape.fromStaticType(expected) == descriptor) {
            buildString {
                appendLine("Expected: $expected")
                appendLine("Actual: $descriptor")
            }
        }
    }

    @Test
    fun failToFindObject() {
        // Prepare
        val requested = BindingPath(
            listOf(
                BindingName("data", BindingCase.INSENSITIVE),
                BindingName("unknown", BindingCase.INSENSITIVE),
            )
        )

        // Act
        val handle = metadata.getObject(requested)
        assertEquals(null, handle)
    }

    @Test
    fun failToFindSchema() {
        // Prepare
        val requested = BindingPath(
            listOf(
                BindingName("unknown", BindingCase.INSENSITIVE),
                BindingName("records", BindingCase.INSENSITIVE),
            )
        )

        // Act
        val handle = metadata.getObject(requested)
        assertEquals(null, handle)
    }

    @Test
    fun failToFindCaseSensitiveObject() {
        // Prepare
        val requested = BindingPath(
            listOf(
                BindingName("data", BindingCase.INSENSITIVE),
                BindingName("RECORDS", BindingCase.SENSITIVE),
            )
        )

        // Act
        val handle = metadata.getObject(requested)
        assertEquals(null, handle)
    }
}
