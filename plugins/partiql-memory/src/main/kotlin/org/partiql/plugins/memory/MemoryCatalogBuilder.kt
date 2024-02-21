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

import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorPath
import org.partiql.spi.connector.sql.info.InfoSchema
import org.partiql.spi.fn.Fn
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnIndex
import org.partiql.types.StaticType

/**
 * Utility class for creating a MemoryCatalog.
 */
public class MemoryCatalogBuilder {

    private var _name: String? = null
    private var _info: InfoSchema? = null
    private var _items: MutableList<Pair<BindingPath, MemoryObject>> = mutableListOf()

    public fun name(name: String): MemoryCatalogBuilder = this.apply { this._name = name }

    public fun info(info: InfoSchema): MemoryCatalogBuilder = this.apply { this._info = info }

    /**
     * This is a simple `dot` delimited utility for adding type definitions.
     *
     * At some point, this will support adding values as well as paths.
     *
     * @param name
     * @param type
     */
    public fun define(name: String, type: StaticType): MemoryCatalogBuilder = this.apply {
        val path = BindingPath(name.split(".").map { BindingName(it, BindingCase.SENSITIVE) })
        val obj = MemoryObject(type)
        _items.add(path to obj)
    }

    @OptIn(FnExperimental::class)
    public fun build(): MemoryCatalog {
        val name = _name ?: error("MemoryCatalog must have a name")
        val info = _info ?: InfoSchema(object : FnIndex {
            override fun get(path: List<String>): List<Fn> = emptyList()
            override fun get(path: ConnectorPath, specific: String): Fn? = null
        })
        val catalog = MemoryCatalog(name, info)
        for (item in _items) { catalog.insert(item.first, item.second) }
        return catalog
    }
}
