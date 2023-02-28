package org.partiql.catalog

import org.partiql.spi.types.SchemaType

class Catalog {
    var root = Name(value="root")
    fun qualifyName(name: Name, resolver: NameResolver): Name {
        TODO()
    }

    fun addNames(names: List<Name>) {
        root.addChildren(names)
    }
}

class Name(value: String) {
    val value = value
    val objects = mutableListOf<DbObject>()
    var children = mutableListOf<Name>()

    fun addChild(child: Name) {
        children.add(child)
    }

    fun addChildren(names: List<Name>) {
        children.addAll(names)
    }
}

sealed class DbObject {
    data class Table(val name: String, val schema: List<SchemaType>) : DbObject()
}

class BasicNameResolver : NameResolver {
    override fun resolve(name: Name): Name {
        TODO("Not yet implemented")
    }
}

interface NameResolver {
    fun resolve(name: Name): Name
}
