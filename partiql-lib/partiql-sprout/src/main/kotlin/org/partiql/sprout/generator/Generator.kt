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

import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import net.pearx.kasechange.toCamelCase
import org.partiql.sprout.generator.poems.BuilderPoem
import org.partiql.sprout.generator.poems.VisitorPoem
import org.partiql.sprout.generator.spec.DomainSpec
import org.partiql.sprout.generator.spec.FileSpec
import org.partiql.sprout.generator.spec.NodeSpec
import org.partiql.sprout.generator.spec.UniverseSpec
import org.partiql.sprout.model.TypeDef
import org.partiql.sprout.model.TypeProp
import org.partiql.sprout.model.Universe

class Generator(private val options: Options) {

    fun generate(universe: Universe): Result {

        // --- Initialize an empty symbol table(?)
        val symbols = Symbols.init(universe, options)

        // In the future, this list will be dynamic
        val poems = listOf(
            VisitorPoem(symbols),
            BuilderPoem(symbols),
            // ListenerPoem(symbols),
            // JacksonPoem(symbols),
        )

        // --- Generate skeleton
        val spec = UniverseSpec(
            universe = universe,
            nodes = universe.nodes(symbols),
            domains = universe.domains(symbols),
            base = TypeSpec.classBuilder(symbols.base).addModifiers(KModifier.ABSTRACT),
            types = universe.types(symbols)
        )

        return Result(
            specs = with(spec) {
                // Apply each poem
                poems.forEach { it.apply(this) }
                // Finalize each spec/builder
                build(options.packageRoot).map { FileSpec(it) }
            }
        )
    }

    // --- Internal -----------------------------------

    /**
     * Generate a NodeSpec for each Type in the given Universe
     */
    private fun Universe.nodes(symbols: Symbols): List<NodeSpec> = types.mapNotNull { it.generate(symbols) }
        .map {
            it.builder.superclass(symbols.base)
            it
        }

    /**
     * Generate a DomainSpec for each Domain in the given Universe
     */
    private fun Universe.domains(symbols: Symbols): List<DomainSpec> {
        return domains.map { domain ->
            val name = symbols.typeNameOf(domain)
            val constructor = FunSpec.constructorBuilder().addModifiers(KModifier.PRIVATE).build()
            DomainSpec(
                domain = domain,
                name = name,
                builder = TypeSpec.classBuilder(name).primaryConstructor(constructor),
                companion = TypeSpec.companionObjectBuilder(),
            )
        }
    }

    /**
     * Generate all top-level enums as these are not children of a product definition
     */
    private fun Universe.types(symbols: Symbols) = types.filterIsInstance<TypeDef.Enum>()
        .map { it.generate(symbols) }
        .toMutableList()

    /**
     * Entry point for node generation.
     */
    private fun TypeDef.generate(symbols: Symbols): NodeSpec? = when (this) {
        is TypeDef.Product -> this.generate(symbols)
        is TypeDef.Sum -> this.generate(symbols)
        is TypeDef.Enum -> null // enums are constants, not nodes
    }

    /**
     * Product Node Generation
     */
    private fun TypeDef.Product.generate(symbols: Symbols) = NodeSpec.Product(
        product = this,
        props = props.map { NodeSpec.Prop(it.name.toCamelCase(), symbols.typeNameOf(it.ref)) },
        clazz = symbols.clazz(ref),
        types = props.filterIsInstance<TypeProp.Enum>().map { it.def.generate(symbols) }.toMutableList(),
    ).apply {
        props.forEach {
            val para = ParameterSpec.builder(it.name, it.type).build()
            val prop = PropertySpec.builder(it.name, it.type).initializer(it.name).build()
            builder.addProperty(prop)
            constructor.addParameter(para)
        }
        when (options.node.modifier) {
            NodeOptions.Modifier.FINAL -> {}
            NodeOptions.Modifier.DATA -> if (props.isNotEmpty()) builder.addModifiers(KModifier.DATA)
            NodeOptions.Modifier.OPEN -> builder.addModifiers(KModifier.OPEN)
        }
    }

    /**
     * Sum node generation
     */
    private fun TypeDef.Sum.generate(symbols: Symbols) = NodeSpec.Sum(
        sum = this,
        variants = variants.mapNotNull { it.generate(symbols) },
        clazz = symbols.clazz(ref),
    ).apply {
        variants.forEach { it.builder.superclass(clazz) }
    }

    /**
     * Enum constant generation
     */
    private fun TypeDef.Enum.generate(symbols: Symbols) = TypeSpec.enumBuilder(symbols.clazz(ref))
        .apply { values.forEach { addEnumConstant(it) } }
        .build()
}
