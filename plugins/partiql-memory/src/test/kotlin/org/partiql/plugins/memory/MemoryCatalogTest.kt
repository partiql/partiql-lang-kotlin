package org.partiql.plugins.memory

import org.junit.jupiter.api.Test
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.types.BagType
import org.partiql.types.StaticType
import org.partiql.types.StructType

class MemoryCatalogTest {

    companion object {

        private val catalog = MemoryCatalog.builder()
            .name("test")
            .define("a", StaticType.INT2)
            .define(
                "struct",
                StructType(
                    fields = listOf(StructType.Field("a", StaticType.INT2))
                )
            )
            .define(
                "schema.tbl",
                BagType(
                    StructType(
                        fields = listOf(StructType.Field("a", StaticType.INT2))
                    )
                )
            )
            .build()
    }

    @Test
    fun getValue() {
        val requested = BindingPath(
            listOf(
                BindingName("a", BindingCase.INSENSITIVE)
            )
        )
        val expected = StaticType.INT2
        val handle = catalog.find(requested)
        val descriptor = handle!!.entity.getType()
        assert(requested.matches(handle.path))
        assert(expected == descriptor)
    }

    @Test
    fun getCaseSensitiveValueShouldFail() {
        val requested = BindingPath(
            listOf(
                BindingName("A", BindingCase.SENSITIVE)
            )
        )
        val handle = catalog.find(requested)
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
        val handle = catalog.find(requested)
        val descriptor = handle!!.entity.getType()
        val expectConnectorPath = listOf("struct")
        val expectedObjectType = StructType(fields = listOf(StructType.Field("a", StaticType.INT2)))

        assert(expectConnectorPath == handle.path)
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
        val handle = catalog.find(requested)
        val descriptor = handle!!.entity.getType()
        val expectedObjectType = BagType(StructType(fields = listOf(StructType.Field("a", StaticType.INT2))))

        assert(requested.matches(handle.path))
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
        val handle = catalog.find(requested)
        val descriptor = handle!!.entity.getType()
        val expectedObjectType = BagType(StructType(fields = listOf(StructType.Field("a", StaticType.INT2))))
        val expectConnectorPath = listOf("schema", "tbl")

        assert(expectConnectorPath == handle.path)
        assert(expectedObjectType == descriptor)
    }
}
