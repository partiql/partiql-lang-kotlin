package org.partiql.catalog

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
    val objects = mutableListOf<CatalogObject>()
    var children = mutableListOf<Name>()

    fun addChild(child: Name) {
        children.add(child)
    }

    fun addChildren(names: List<Name>) {
        children.addAll(names)
    }
}

sealed class CatalogObject {
    data class Table(val name: String) : CatalogObject()
}

class BasicNameResolver : NameResolver {
    override fun resolve(name: Name): Name {
        TODO("Not yet implemented")
    }
}

interface NameResolver {
    fun resolve(name: Name): Name
}
