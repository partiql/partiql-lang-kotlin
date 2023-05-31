package org.partiql.plugins.mockdb

import org.junit.jupiter.api.Test
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorObjectPath
import org.partiql.spi.connector.ConnectorSession
import org.partiql.types.BagType
import org.partiql.types.IntType
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint
import java.nio.file.Paths
import kotlin.test.assertEquals

class LocalConnectorMetadataTests {

    private val catalogUrl =
        LocalConnectorMetadataTests::class.java.classLoader.getResource("catalogs/fs") ?: error("Couldn't be found")
    private val catalogName = "fs"
    private val session = object : ConnectorSession {
        override fun getQueryId(): String = "mock_query_id"
        override fun getUserId(): String = "mock_user"
    }

    private val metadata = LocalConnectorMetadata(catalogName, Paths.get(catalogUrl.path))

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
                constraints = setOf(TupleConstraint.Ordered)
            )
        )

        // Act
        val handle = metadata.getObjectHandle(session, requested)!!
        val descriptor = metadata.getObjectType(session, handle)

        // Assert
        assert(requested.isEquivalentTo(handle.absolutePath))
        assertEquals(expected, descriptor)
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
        val expectedPath = ConnectorObjectPath(listOf("data", "struct"))
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
                        constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
                    )
                ),
                constraints = setOf(TupleConstraint.Open(false), TupleConstraint.UniqueAttrs(true), TupleConstraint.Ordered)
            )

        // Act
        val handle = metadata.getObjectHandle(session, requested)!!
        val descriptor = metadata.getObjectType(session, handle)

        // Assert
        assert(expectedPath == handle.absolutePath)
        assert(expected == descriptor)
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
        val handle = metadata.getObjectHandle(session, requested)
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
        val handle = metadata.getObjectHandle(session, requested)
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
        val handle = metadata.getObjectHandle(session, requested)
        assertEquals(null, handle)
    }

    private fun BindingPath.isEquivalentTo(other: ConnectorObjectPath): Boolean {
        if (this.steps.size != other.steps.size) {
            return false
        }
        this.steps.forEachIndexed { index, step ->
            if (step.isEquivalentTo(other.steps[index]).not()) {
                return false
            }
        }
        return true
    }
}
