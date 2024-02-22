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

package org.partiql.eval.plugin

import com.amazon.ionelement.api.toIonElement
import org.partiql.eval.ION
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.toIonValue
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorHandle
import org.partiql.spi.connector.ConnectorPath
import org.partiql.types.BagType
import org.partiql.types.ListType
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.value.BagValue
import org.partiql.value.ListValue
import org.partiql.value.PartiQLValue
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.StructValue
import org.partiql.value.check
import org.partiql.value.io.PartiQLValueIonReaderBuilder

public class MemoryCatalog(public val name: String, public val bindings: Bindings<ExprValue>) {

    private val root: Tree.Dir = Tree.Dir(name)

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
        var currDirs = listOf(root)
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
            0 -> {
                val obj = findFromBindings(path) ?: return null
                // add in global
                insert(path, obj)
                ConnectorHandle.Obj(
                    ConnectorPath(path.steps.map { it.name }),
                    obj
                )
            }
            1 -> currItems.first().obj
            else -> error("Ambiguous binding $path, found multiple matching bindings")
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun findFromBindings(path: BindingPath): MemoryObject? {
        val n = path.steps.last()
        val bn = org.partiql.lang.eval.BindingName(
            name = n.name,
            bindingCase = when (n.case) {
                BindingCase.SENSITIVE -> org.partiql.lang.eval.BindingCase.SENSITIVE
                BindingCase.INSENSITIVE -> org.partiql.lang.eval.BindingCase.INSENSITIVE
            }
        )
        val exprValue = bindings[bn] ?: return null
        val partiqlValue = exprValue.toPartiQLValue()

        // HERE: We need a way to get the full static type
        return MemoryObject(partiqlValue.toStaticType(), partiqlValue)
    }

    @OptIn(PartiQLValueExperimental::class)
    internal fun PartiQLValue.toStaticType(): StaticType = when (val type = this.type) {
        PartiQLValueType.INT8,
        PartiQLValueType.INT16,
        PartiQLValueType.INT32,
        PartiQLValueType.INT64,
        PartiQLValueType.INT,
        PartiQLValueType.BOOL,
        PartiQLValueType.DECIMAL,
        PartiQLValueType.DECIMAL_ARBITRARY,
        PartiQLValueType.FLOAT32,
        PartiQLValueType.FLOAT64,
        PartiQLValueType.CHAR,
        PartiQLValueType.STRING,
        PartiQLValueType.SYMBOL,
        PartiQLValueType.BINARY,
        PartiQLValueType.BYTE,
        PartiQLValueType.BLOB,
        PartiQLValueType.CLOB,
        PartiQLValueType.DATE,
        PartiQLValueType.TIME,
        PartiQLValueType.TIMESTAMP,
        PartiQLValueType.INTERVAL,
        PartiQLValueType.NULL,
        PartiQLValueType.MISSING,
        PartiQLValueType.ANY -> type.toStaticType()
        PartiQLValueType.BAG -> {
            val iter = this.check<BagValue<PartiQLValue>>().iterator()
            val element = mutableListOf<StaticType>()
            iter.forEachRemaining {
                element.add(it.toStaticType())
            }
            BagType(StaticType.unionOf(element.toSet()))
        }
        PartiQLValueType.LIST -> {
            val iter = this.check<ListValue<PartiQLValue>>().iterator()
            val element = mutableListOf<StaticType>()
            iter.forEachRemaining {
                element.add(it.toStaticType())
            }
            ListType(StaticType.unionOf(element.toSet()))
        }
        PartiQLValueType.SEXP -> {
            val iter = this.check<ListValue<PartiQLValue>>().iterator()
            val element = mutableListOf<StaticType>()
            iter.forEachRemaining {
                element.add(it.toStaticType())
            }
            ListType(StaticType.unionOf(element.toSet()))
        }
        PartiQLValueType.STRUCT -> {
            val iter = this.check<StructValue<PartiQLValue>>().entries
            val fields = mutableListOf<StructType.Field>()
            iter.forEach {
                fields.add(
                    StructType.Field(
                        it.first,
                        it.second.toStaticType()
                    )
                )
            }
            StructType(fields)
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    internal fun PartiQLValueType.toStaticType(): StaticType = when (this) {
        PartiQLValueType.NULL -> StaticType.NULL
        PartiQLValueType.MISSING -> StaticType.MISSING
        else -> toNonNullStaticType().asNullable()
    }

    @OptIn(PartiQLValueExperimental::class)
    internal fun PartiQLValueType.toNonNullStaticType(): StaticType = when (this) {
        PartiQLValueType.ANY -> StaticType.ANY
        PartiQLValueType.BOOL -> StaticType.BOOL
        PartiQLValueType.INT8 -> StaticType.INT2
        PartiQLValueType.INT16 -> StaticType.INT2
        PartiQLValueType.INT32 -> StaticType.INT4
        PartiQLValueType.INT64 -> StaticType.INT8
        PartiQLValueType.INT -> StaticType.INT
        PartiQLValueType.DECIMAL_ARBITRARY -> StaticType.DECIMAL
        PartiQLValueType.DECIMAL -> StaticType.DECIMAL
        PartiQLValueType.FLOAT32 -> StaticType.FLOAT
        PartiQLValueType.FLOAT64 -> StaticType.FLOAT
        PartiQLValueType.CHAR -> StaticType.CHAR
        PartiQLValueType.STRING -> StaticType.STRING
        PartiQLValueType.SYMBOL -> StaticType.SYMBOL
        PartiQLValueType.BINARY -> TODO()
        PartiQLValueType.BYTE -> TODO()
        PartiQLValueType.BLOB -> StaticType.BLOB
        PartiQLValueType.CLOB -> StaticType.CLOB
        PartiQLValueType.DATE -> StaticType.DATE
        PartiQLValueType.TIME -> StaticType.TIME
        PartiQLValueType.TIMESTAMP -> StaticType.TIMESTAMP
        PartiQLValueType.INTERVAL -> TODO()
        PartiQLValueType.BAG -> StaticType.BAG
        PartiQLValueType.LIST -> StaticType.LIST
        PartiQLValueType.SEXP -> StaticType.SEXP
        PartiQLValueType.STRUCT -> StaticType.STRUCT
        PartiQLValueType.NULL -> StaticType.NULL
        PartiQLValueType.MISSING -> StaticType.MISSING
    }

    @OptIn(PartiQLValueExperimental::class)
    fun ExprValue.toPartiQLValue() =
        PartiQLValueIonReaderBuilder
            .standard()
            .build(this.toIonValue(ION).toIonElement())
            .read()

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
