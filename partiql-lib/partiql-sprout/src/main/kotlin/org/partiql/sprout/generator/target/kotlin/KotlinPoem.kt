package org.partiql.sprout.generator.target.kotlin

import org.partiql.sprout.generator.target.kotlin.spec.KotlinNodeSpec
import org.partiql.sprout.generator.target.kotlin.spec.KotlinUniverseSpec

abstract class KotlinPoem(val symbols: KotlinSymbols) {

    abstract val id: String

    open fun apply(universe: KotlinUniverseSpec) {
        universe.nodes.forEach { apply(it) }
    }

    open fun apply(node: KotlinNodeSpec) {
        when (node) {
            is KotlinNodeSpec.Product -> apply(node)
            is KotlinNodeSpec.Sum -> apply(node)
        }
    }

    open fun apply(node: KotlinNodeSpec.Product) {
        node.children.forEach { apply(it) }
    }

    open fun apply(node: KotlinNodeSpec.Sum) {
        node.children.forEach { apply(it) }
    }
}
