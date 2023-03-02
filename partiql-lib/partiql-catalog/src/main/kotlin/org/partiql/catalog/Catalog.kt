package org.partiql.catalog

import org.partiql.lang.types.SchemaType

class Catalog {
    var root = Name(id="root")
    var objects = mutableSetOf<Pair<String, Name>>()

    fun qualifyName(id: String) : Name {
        TODO()
    }

    fun addObject(objectId: String, name: Name) {
        objects.add(Pair(objectId, name))
    }
}

class Name(id: String, children: List<Name> = emptyList()) {
    val id = id
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
