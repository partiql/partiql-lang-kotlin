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
import org.partiql.types.StaticType

/**
 * Utility class for creating a MemoryCatalog.
 */
public class MemoryCatalogBuilder {

    private var name: String? = null
    private var items: MutableList<Pair<BindingPath, MemoryObject>> = mutableListOf()

    public fun name(name: String): MemoryCatalogBuilder = this.apply { this.name = name }

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
        items.add(path to obj)
    }

    public fun build(): MemoryCatalog {
        val catalog = MemoryCatalog(name ?: error("MemoryCatalog must have a name"))
        for (item in items) { catalog.insert(item.first, item.second) }
        return catalog
    }
}
