package org.partiql.sprout.generator.target.kotlin

import org.partiql.sprout.generator.target.kotlin.spec.KotlinNodeSpec
import org.partiql.sprout.generator.target.kotlin.spec.KotlinUniverseSpec
import org.partiql.sprout.model.TypeDef
import org.partiql.sprout.model.TypeProp
import org.partiql.sprout.model.Universe

abstract class KotlinPoem(val symbols: KotlinSymbols) {

    abstract val id: String

    // Default do nothing
    open fun redefine(universe: Universe) = universe

    open fun redefine(type: TypeDef) = when (type) {
        is TypeDef.Enum -> redefine(type)
        is TypeDef.Product -> redefine(type)
        is TypeDef.Sum -> redefine(type)
    }

    open fun redefine(type: TypeDef.Enum): TypeDef = type

    open fun redefine(type: TypeDef.Product): TypeDef = TypeDef.Product(
        ref = type.ref,
        props = type.props.map {
            when (it) {
                is TypeProp.Ref -> it
                is TypeProp.Inline -> TypeProp.Inline(
                    name = it.name,
                    def = redefine(it.def)
                )
            }
        },
        types = type.types.map { redefine(it) }
    )

    open fun redefine(type: TypeDef.Sum): TypeDef = TypeDef.Sum(
        ref = type.ref,
        variants = type.variants.map { redefine(it) },
        types = type.types.map { redefine(it) },
    )

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
