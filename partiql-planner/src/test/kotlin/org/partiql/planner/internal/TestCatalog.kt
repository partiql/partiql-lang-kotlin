package org.partiql.planner.internal

import org.partiql.planner.catalog.Catalog
import org.partiql.planner.catalog.Identifier
import org.partiql.planner.catalog.Name
import org.partiql.planner.catalog.Namespace
import org.partiql.planner.catalog.Routine
import org.partiql.planner.catalog.Session
import org.partiql.planner.catalog.Table
import org.partiql.types.PType

public class TestCatalog private constructor(
    private val name: String,
    private val root: Tree,
) : Catalog {

    override fun getName(): String = name

    override fun getTable(session: Session, name: Name): Table? {
        return null
    }

    override fun getTableHandle(session: Session, identifier: Identifier): Table.Handle? {
        return null
        // if (identifier.hasQualifier()) {
        //     error("Catalog does not support qualified table names")
        // }
        // var match: Table? = null
        // val id = identifier.getIdentifier()
        // for (table in tree.values) {
        //     if (id.matches(table.getName())) {
        //         if (match == null) {
        //             match = table
        //         } else {
        //             error("Ambiguous table name: $name")
        //         }
        //     }
        // }
        // return match
    }

    // TODO
    override fun listTables(session: Session, namespace: Namespace): Collection<Name> {
        return emptyList()
    }

    // TODO
    override fun listNamespaces(session: Session, namespace: Namespace): Collection<Namespace> {
        return emptyList()
    }

    // TODO
    override fun getRoutines(session: Session, name: Name): Collection<Routine> {
        return emptyList()
    }

    private class Tree(
        @JvmField var table: Table?,
        @JvmField val children: MutableMap<String, Tree>,
    ) {
        fun contains(name: String) = children.contains(name)
        fun get(name: String): Tree? = children[name]
        fun getOrPut(name: String): Tree = children.getOrPut(name) { Tree(null, mutableMapOf()) }
    }

    companion object {

        @JvmStatic
        fun empty(name: String): TestCatalog = TestCatalog(name, Tree(null, mutableMapOf()))

        @JvmStatic
        fun builder(): Builder = Builder()
    }

    /**
     * Perhaps this will be a
     */
    public class Builder {

        private var name: String? = null
        private val root = Tree(null, mutableMapOf())

        fun name(name: String): Builder {
            this.name = name
            return this
        }

        fun createTable(name: String, schema: PType): Builder {
            return createTable(Name.of(name), schema)
        }

        fun createTable(name: Name, schema: PType): Builder {
            var curr = root
            for (part in name) {
                // upsert namespaces
                curr = curr.getOrPut(part)
            }
            curr.table = Table.of(name.getName(), schema)
            return this
        }

        fun build(): Catalog {
            if (name == null) {
                error("Catalog must have a name")
            }
            return TestCatalog(name!!, root)
        }
    }
}
