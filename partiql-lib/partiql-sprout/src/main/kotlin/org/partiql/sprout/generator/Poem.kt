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

package org.partiql.sprout.generator

import org.partiql.sprout.generator.spec.DomainSpec
import org.partiql.sprout.generator.spec.NodeSpec
import org.partiql.sprout.generator.spec.UniverseSpec

/**
 * A Poem for the Poet
 */
abstract class Poem(val symbols: Symbols) {

    abstract val id: String

    open fun apply(universe: UniverseSpec) {
        universe.nodes.forEach { apply(it) }
        universe.domains.forEach { domain ->
            val members = symbols.membersOf(domain.domain)
            val nodes = universe.nodes.filter { it.def in members }
            apply(domain, nodes)
        }
    }

    open fun apply(node: NodeSpec) {
        when (node) {
            is NodeSpec.Product -> apply(node)
            is NodeSpec.Sum -> apply(node)
        }
    }

    open fun apply(node: NodeSpec.Product) { }

    open fun apply(node: NodeSpec.Sum) {
        node.variants.forEach { apply(it) }
    }

    open fun apply(domain: DomainSpec, members: List<NodeSpec>) {}
}
