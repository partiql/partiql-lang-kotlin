package org.partiql.sprout.generator.target.kotlin.poems

import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.MUTABLE_MAP
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import org.partiql.sprout.generator.target.kotlin.KotlinPoem
import org.partiql.sprout.generator.target.kotlin.KotlinSymbols
import org.partiql.sprout.generator.target.kotlin.spec.KotlinUniverseSpec

class KotlinMetasPoem(symbols: KotlinSymbols) : KotlinPoem(symbols) {

    override val id = "metas"

    // Consider a TypeAlias
    private val stringMapOfAny = MUTABLE_MAP.parameterizedBy(STRING, ANY)

    private val metas = PropertySpec.Companion.builder("metadata", stringMapOfAny).initializer("mutableMapOf()").build()

    override fun apply(universe: KotlinUniverseSpec) {
        universe.base.addProperty(metas.toBuilder().build())
        super.apply(universe)
    }
}
