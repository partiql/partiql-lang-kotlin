package org.partiql.plugins.memory

import com.amazon.ionelement.api.emptyIonStruct
import org.junit.jupiter.api.Test
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorObjectPath
import org.partiql.spi.connector.ConnectorSession
import org.partiql.types.BagType
import org.partiql.types.StaticType
import org.partiql.types.StructType

class InMemoryPluginTest {

    private val session = object : ConnectorSession {
        override fun getQueryId(): String = "mock_query_id"
        override fun getUserId(): String = "mock_user"
    }

    companion object {

        val plugin = MemoryPlugin(
            MemoryCatalog.Provider().also {
                it["test"] = MemoryCatalog.of(
                    "a" to StaticType.INT2,
                    "struct" to StructType(
                        fields = listOf(StructType.Field("a", StaticType.INT2))
                    ),
                    "schema.tbl" to BagType(
                        StructType(
                            fields = listOf(StructType.Field("a", StaticType.INT2))
                        )
                    )
                )
            }
        )
    }

    private fun connector(catalog: String) = plugin.factory.create(catalog, emptyIonStruct()) as MemoryConnector

    @Test
    fun getValue() {
        val requested = BindingPath(
            listOf(
                BindingName("a", BindingCase.INSENSITIVE)
            )
        )
        val expected = StaticType.INT2

        val connector = connector("test")

        val metadata = connector.Metadata()

        val handle = metadata.getObjectHandle(session, requested)

        val descriptor = metadata.getObjectType(session, handle!!)

        assert(requested.isEquivalentTo(handle.absolutePath))
        assert(expected == descriptor)
    }

    @Test
    fun getCaseSensitiveValueShouldFail() {
        val requested = BindingPath(
            listOf(
                BindingName("A", BindingCase.SENSITIVE)
            )
        )

        val connector = connector("test")

        val metadata = connector.Metadata()

        val handle = metadata.getObjectHandle(session, requested)

        assert(null == handle)
    }

    @Test
    fun accessStruct() {
        val requested = BindingPath(
            listOf(
                BindingName("struct", BindingCase.INSENSITIVE),
                BindingName("a", BindingCase.INSENSITIVE)
            )
        )

        val connector = connector("test")

        val metadata = connector.Metadata()

        val handle = metadata.getObjectHandle(session, requested)

        val descriptor = metadata.getObjectType(session, handle!!)

        val expectConnectorPath = ConnectorObjectPath(listOf("struct"))

        val expectedObjectType = StructType(fields = listOf(StructType.Field("a", StaticType.INT2)))

        assert(expectConnectorPath == handle.absolutePath)
        assert(expectedObjectType == descriptor)
    }

    @Test
    fun pathNavigationSuccess() {
        val requested = BindingPath(
            listOf(
                BindingName("schema", BindingCase.INSENSITIVE),
                BindingName("tbl", BindingCase.INSENSITIVE)
            )
        )

        val connector = connector("test")

        val metadata = connector.Metadata()

        val handle = metadata.getObjectHandle(session, requested)

        val descriptor = metadata.getObjectType(session, handle!!)

        val expectedObjectType = BagType(StructType(fields = listOf(StructType.Field("a", StaticType.INT2))))

        assert(requested.isEquivalentTo(handle.absolutePath))
        assert(expectedObjectType == descriptor)
    }

    @Test
    fun pathNavigationSuccess2() {
        val requested = BindingPath(
            listOf(
                BindingName("schema", BindingCase.INSENSITIVE),
                BindingName("tbl", BindingCase.INSENSITIVE),
                BindingName("a", BindingCase.INSENSITIVE)
            )
        )

        val connector = connector("test")

        val metadata = connector.Metadata()

        val handle = metadata.getObjectHandle(session, requested)

        val descriptor = metadata.getObjectType(session, handle!!)

        val expectedObjectType = BagType(StructType(fields = listOf(StructType.Field("a", StaticType.INT2))))

        val expectConnectorPath = ConnectorObjectPath(listOf("schema", "tbl"))

        assert(expectConnectorPath == handle.absolutePath)
        assert(expectedObjectType == descriptor)
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
