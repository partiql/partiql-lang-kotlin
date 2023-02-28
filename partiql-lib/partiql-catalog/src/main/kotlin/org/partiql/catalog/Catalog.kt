package org.partiql.catalog

import org.partiql.lang.types.SchemaType

class Catalog {
    var root = Name(id="root")
    fun qualifyName(name: Name, resolver: NameResolver) : Name {
        TODO()
    }

    fun addNames(names: List<Name>) {
        root.addChildren(names)
    }
}

class Name(id: String) {
    val id = id
    val objects = mutableListOf<DbObject>()
    var children = mutableListOf<Name>()

    fun addChild(child: Name) {
        children.add(child)
    }

    fun addChildren(names: List<Name>) {
        children.addAll(names)
    }
}

interface DbObject {
    fun id(): String
}

class Table(val name: String, val schema: List<SchemaType>) : DbObject {
    override fun id() : String {
        return name
    }
}

class BasicNameResolver : NameResolver {
    override fun resolve(name: Name): Name {
        TODO("Not yet implemented")
    }
}

interface NameResolver {
    fun resolve(name: Name): Name
}
