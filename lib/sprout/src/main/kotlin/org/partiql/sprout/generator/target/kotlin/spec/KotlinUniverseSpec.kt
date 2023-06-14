/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package org.partiql.sprout.generator.target.kotlin.spec

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.TypeSpec
import org.partiql.sprout.model.Universe

/**
 * Wraps a [Universe] with KotlinPoet builders
 *
 * @property universe       Universe definition
 * @property nodes          Node builders
 * @property base           Base node builder
 * @property packages       Additional packages to include in the universe package
 * @property types          Additional non-node types to include in the universe package
 * @property files          Additional files to include in the universe package
 */
class KotlinUniverseSpec(
    val universe: Universe,
    val nodes: List<KotlinNodeSpec>,
    val base: TypeSpec.Builder,
    val packages: MutableList<KotlinPackageSpec> = mutableListOf(),
    val types: MutableList<TypeSpec> = mutableListOf(),
    val files: MutableList<FileSpec> = mutableListOf()
) {

    /**
     * Build the Kotlin files
     *
     * <root>
     *  ├── Types.kt
     *  ├── ...
     *  ├── builder
     *  │   └── <Universe>Builder.kt
     *  ├── listener
     *  │   └── <Universe>Listener.kt
     *  └── visitor
     *      └── <Universe>Visitor.kt
     */
    fun build(root: String): List<FileSpec> {
        val files = mutableListOf<FileSpec>()
        val specs = nodes.map { it.build() }

        // <root>/Types.kt
        files += with(FileSpec.builder(root, "Types")) {
            addType(base.build())
            specs.forEach { addType(it) }
            types.forEach { addType(it) }
            build()
        }

        // <root>/<ext>.kt
        files += this.files

        // <root>/<package>/...
        files += packages.flatMap { it.files }

        // <root>/impl/Types.kt
        files += with(FileSpec.builder("$root.impl", "Types")) {
            forEachNode {
                if (it is KotlinNodeSpec.Product) {
                    addType(it.buildImpl())
                }
            }
            build()
        }

        return files
    }

    fun forEachNode(action: (KotlinNodeSpec) -> Unit) {
        fun List<KotlinNodeSpec>.applyToAll() {
            forEach {
                action(it)
                it.children.applyToAll()
            }
        }
        nodes.applyToAll()
    }
}
