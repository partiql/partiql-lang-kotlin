package org.partiql.sprout.generator.target.kotlin.poems

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import org.partiql.sprout.generator.target.kotlin.KotlinPoem
import org.partiql.sprout.generator.target.kotlin.KotlinSymbols
import org.partiql.sprout.generator.target.kotlin.spec.KotlinNodeSpec
import org.partiql.sprout.generator.target.kotlin.spec.KotlinUniverseSpec

/**
 * Poem which creates a DSL for instantiation
 */
class KotlinFactoryPoem(symbols: KotlinSymbols) : KotlinPoem(symbols) {

    override val id: String = "factory"

    // file@JvmName("...")
    private val jvmName = AnnotationSpec.builder(ClassName("kotlin.jvm", "JvmName"))

    override fun apply(universe: KotlinUniverseSpec) {
        super.apply(universe)
        val factory = FileSpec.builder(symbols.rootPackage, symbols.rootId)
            .addAnnotation(jvmName.addMember("%S", symbols.rootId).build())
            .apply {
                universe.forEachNode {
                    // add all product creation functions
                    if (it is KotlinNodeSpec.Product) addFunction(it.factoryMethod())
                }
            }
            .build()
        universe.files.add(factory)
    }

    private fun KotlinNodeSpec.Product.factoryMethod() = FunSpec.builder(symbols.camel(product.ref))
        .returns(clazz)
        .apply {
            val args = props.map {
                addParameter(it.name, it.type)
                it.name
            }
            addStatement("return %T(${args.joinToString()})", clazz)
        }
        .build()
}
