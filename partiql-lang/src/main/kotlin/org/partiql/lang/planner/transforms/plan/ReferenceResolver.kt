/*
 * Copyright Amazon.com, Inc. or its affiliates.  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at:
 *
 *       http://aws.amazon.com/apache2.0/
 *
 *  or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
 *  language governing permissions and limitations under the License.
 */

package org.partiql.lang.planner.transforms.plan

import org.partiql.plan.Attribute
import org.partiql.spi.BindingCase
import org.partiql.spi.BindingName
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.ConnectorObjectPath
import org.partiql.types.StaticType
import org.partiql.types.StructType
import org.partiql.types.TupleConstraint

internal object ReferenceResolver {

    internal class ResolvedType(
        val type: StaticType,
        val levelsMatched: Int = 1
    )

    /**
     * Logic is as follows:
     * 1. If Current Catalog and Schema are set, create a Path to the object and attempt to grab handle and schema.
     *   a. If not found, just try to find the object in the catalog.
     * 2. If Current Catalog is not set:
     *   a. Loop through all catalogs and try to find the object.
     *
     * TODO: Add global bindings
     * TODO: Replace paths with global variable references if found
     */
    internal fun resolveGlobalBind(path: BindingPath, ctx: PlanTyper.Context): ResolvedType? {
        val currentCatalog = ctx.session.currentCatalog?.let { BindingName(it, BindingCase.SENSITIVE) }
        val currentCatalogPath = BindingPath(ctx.session.currentDirectory.map { BindingName(it, BindingCase.SENSITIVE) })
        val absoluteCatalogPath = BindingPath(currentCatalogPath.steps + path.steps)
        return when (path.steps.size) {
            0 -> null
            1 -> getDescriptor(ctx, currentCatalog, path, absoluteCatalogPath)
            2 -> getDescriptor(ctx, currentCatalog, path, path) ?: getDescriptor(ctx, currentCatalog, path, absoluteCatalogPath)
            else -> {
                val inferredCatalog = path.steps[0]
                val newPath = BindingPath(path.steps.subList(1, path.steps.size))
                getDescriptor(ctx, inferredCatalog, path, newPath)
                    ?: getDescriptor(ctx, currentCatalog, path, path)
                    ?: getDescriptor(ctx, currentCatalog, path, absoluteCatalogPath)
            }
        }
    }

    /**
     * Logic is as follows:
     * 1. Look through [input] to find the root of the [path]. If found, return. Else, go to step 2.
     * 2. Look through [input] and grab all [StructType]s. Then, grab the fields of each Struct corresponding to the
     *  root of [path].
     *  - If the Struct if ordered, grab the first matching field.
     *  - If unordered and if multiple fields found, merge the output type. If no structs contain a matching field, return null.
     */
    internal fun resolveLocalBind(path: BindingPath, input: List<Attribute>): ResolvedType? {
        if (path.steps.isEmpty()) { return null }
        val root: StaticType = input.firstOrNull {
            path.steps[0].isEquivalentTo(it.name)
        }?.type ?: run {
            input.map { it.type }.filterIsInstance<StructType>().mapNotNull { struct ->
                inferStructLookup(struct, path.steps[0])
            }.let { potentialTypes ->
                when (potentialTypes.size) {
                    1 -> potentialTypes.first()
                    else -> null
                }
            }
        } ?: return null
        return ResolvedType(root)
    }

    /**
     * Searches for the [key] in the [struct]. If not found, return null
     */
    internal fun inferStructLookup(
        struct: StructType,
        key: BindingName,
    ): StaticType? = when (struct.constraints.contains(TupleConstraint.Ordered)) {
        true -> struct.fields.firstOrNull { entry ->
            key.isEquivalentTo(entry.key)
        }?.value
        false -> struct.fields.mapNotNull { entry ->
            entry.value.takeIf { key.isEquivalentTo(entry.key) }
        }.let { valueTypes ->
            StaticType.unionOf(valueTypes.toSet()).flatten().takeIf { valueTypes.isNotEmpty() }
        }
    }

    //
    //
    // HELPER METHODS
    //
    //

    private fun getDescriptor(ctx: PlanTyper.Context, catalog: BindingName?, originalPath: BindingPath, catalogPath: BindingPath): ResolvedType? {
        return catalog?.let { cat ->
            ctx.metadata.getObjectHandle(ctx.session, cat, catalogPath)?.let { handle ->
                ctx.metadata.getObjectDescriptor(ctx.session, handle).let {
                    val matched = calculateMatched(originalPath, catalogPath, handle.connectorHandle.absolutePath)
                    ResolvedType(it, levelsMatched = matched)
                }
            }
        }
    }

    /**
     * Logic for determining how many BindingNames were “matched” by the ConnectorMetadata
     * 1. Matched = RelativePath - Not Found
     * 2. Not Found = Input CatalogPath - Output CatalogPath
     * 3. Matched = RelativePath - (Input CatalogPath - Output CatalogPath)
     * 4. Matched = RelativePath + Output CatalogPath - Input CatalogPath
     */
    private fun calculateMatched(originalPath: BindingPath, inputCatalogPath: BindingPath, outputCatalogPath: ConnectorObjectPath): Int {
        return originalPath.steps.size + outputCatalogPath.steps.size - inputCatalogPath.steps.size
    }
}
