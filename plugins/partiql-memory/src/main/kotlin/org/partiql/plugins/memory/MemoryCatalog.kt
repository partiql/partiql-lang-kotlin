/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.plugins.memory

import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorFnProvider
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.ConnectorPath
import org.partiql.spi.connector.sql.SqlFnProvider
import org.partiql.spi.connector.sql.info.InfoSchema
import org.partiql.spi.fn.FnExperimental

/**
 * A basic catalog implementation used in testing.
 *
 * Note, this treats both delimited (quoted) and simple (unquoted) identifiers as case-sensitive.
 *
 * @property name
 */
public class MemoryCatalog(public val name: String, public val infoSchema: InfoSchema) {

    @OptIn(FnExperimental::class)
    public fun getFunctions(): ConnectorFnProvider = SqlFnProvider(infoSchema.functions)

    private val root: Tree.Dir = Tree.Dir(name)

    /**
     * Inserts the `obj` at the given path, creating any non-existent intermediate directories as necessary.
     *
     * @param path  Catalog absolute path.
     * @param obj   Object to insert.
     */
    public fun insert(path: BindingPath, obj: MemoryObject) {
        val dir = path.steps.dropLast(1)
        val binding = path.steps.last()
        var curr: Tree.Dir = root
        // create any non-existent intermediate directories
        dir.forEach {
            curr = curr.mkdir(it.name)
        }
        // insert entity in current dir
        curr.insert(
            binding.name,
            ConnectorHandle.Obj(
                path = ConnectorPath(path.steps.map { it.name }),
                entity = obj,
            )
        )
    }

    /**
     * Finds a [MemoryObject] in the catalog, returning `null` if it does not exist.
     *
     *  1) If multiple paths are found, return the longest match.
     *  2) If the path is ambiguous, this will throw an error.
     *
     * This follows the scoping rules in section 10 of the PartiQL Specification.
     *
     * @param path
     * @return
     */
    public fun find(path: BindingPath): ConnectorHandle.Obj? {
        var currItems = listOf<Tree.Item>()
        var currDirs = listOf<Tree.Dir>(root)
        for (name in path.steps) {
            val nextItems = mutableListOf<Tree.Item>()
            val nextDirs = mutableListOf<Tree.Dir>()
            currDirs.flatMap { it.find(name) }.forEach {
                when (it) {
                    is Tree.Dir -> nextDirs.add(it)
                    is Tree.Item -> nextItems.add(it)
                }
            }
            currItems = if (nextItems.isEmpty()) currItems else nextItems
            currDirs = if (nextDirs.isEmpty()) break else nextDirs
        }
        return when (currItems.size) {
            0 -> null
            1 -> currItems.first().obj
            else -> error("Ambiguous binding $path, found multiple matching bindings")
        }
    }

    /**
     * Gets a [MemoryObject] in the catalog, returning `null` if it does not exist.
     *
     * @param path
     * @return
     */
    public fun get(path: ConnectorPath): MemoryObject? {
        var curr: Tree.Dir = root
        for (i in path.steps.indices) {
            val next = curr.get(path.steps[i]) ?: break
            when (next) {
                is Tree.Dir -> curr = next
                is Tree.Item -> {
                    if (i == path.steps.size - 1) {
                        return next.obj.entity as? MemoryObject
                    }
                    break
                }
            }
        }
        return null
    }

    public companion object {

        @JvmStatic
        public fun builder(): MemoryCatalogBuilder = MemoryCatalogBuilder()

        @JvmStatic
        public fun SQL(): MemoryCatalogBuilder = MemoryCatalogBuilder().info(InfoSchema.default())

        @JvmStatic
        public fun PartiQL(): MemoryCatalogBuilder = MemoryCatalogBuilder().info(InfoSchema.ext())
    }

    private sealed interface Tree {

        /**
         * The catalog entry's case-sensitive binding name.
         */
        val name: String

        /**
         * Dir is similar to an SQL Schema as well as a Unix directory.
         *
         * @property name
         */
        class Dir(override val name: String) : Tree {

            private val children: MutableMap<String, Tree> = mutableMapOf()

            /**
             * Creates a directory, returning the new directory.
             *
             *   1) If a subdirectory with this name already exists, no action.
             *   2) If an entity with this name already exists, error.
             *
             * @param name
             * @return
             */
            fun mkdir(name: String): Dir {
                var child = children[name]
                if (child is Item) {
                    error("File exists: `$name`")
                }
                if (child == null) {
                    child = Dir(name)
                    children[name] = child
                }
                return child as Dir
            }

            /**
             * Inserts an entity in this directory, return the new entity.
             *
             *  1) If an entity with this name already exists, overwrite.
             *  2) If a subdirectory with this name already exists, error.
             *
             * @param name
             * @param obj
             * @return
             */
            fun insert(name: String, obj: ConnectorHandle.Obj): Item {
                if (children[name] is Dir) {
                    error("Directory exists: `$name`")
                }
                val child = Item(name, obj)
                children[name] = child
                return child
            }

            /**
             * List directory contents.
             *
             * @return
             */
            fun ls(): Collection<Tree> = children.values

            /**
             * Find all directory entries by binding naming.
             *
             * @param name
             * @return
             */
            fun find(name: BindingName): List<Tree> = ls().filter { name.matches(it.name) }

            /**
             * Get all directory entries by name.
             *
             * @param name
             * @return
             */
            fun get(name: String): Tree? = children[name]
        }

        /**
         * Item represents a type-annotated global binding in a catalog.
         *
         * @property name
         * @property obj
         */
        class Item(override val name: String, val obj: ConnectorHandle.Obj) : Tree
    }
}
