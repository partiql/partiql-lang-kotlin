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

package org.partiql.sprout.generator.poems

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.asTypeName
import net.pearx.kasechange.toPascalCase
import org.partiql.sprout.generator.Poem
import org.partiql.sprout.generator.Symbols
import org.partiql.sprout.generator.spec.NodeSpec
import org.partiql.sprout.generator.spec.PackageSpec
import org.partiql.sprout.generator.spec.UniverseSpec
import org.partiql.sprout.generator.types.Annotations

/**
 * Poem which creates a DSL for instantiation
 */
class BuilderPoem(symbols: Symbols) : Poem(symbols) {

    override val id: String = "builder"

    private val builderPackageName = "${symbols.rootPackage}.builder"

    private val factoryName = "${symbols.rootId}Factory"
    private val factoryClass = ClassName(builderPackageName, factoryName)
    private val factory = TypeSpec.classBuilder(factoryClass).addModifiers(KModifier.ABSTRACT)
        .addType(
            TypeSpec.companionObjectBuilder()
                .addProperty(
                    PropertySpec.builder("DEFAULT", factoryClass)
                        .initializer("object : %T() {}", factoryClass)
                        .build()
                )
                .build()
        )

    private val containerClass = ClassName(builderPackageName, symbols.rootId)
    private val container = TypeSpec.classBuilder(containerClass)
        .addKdoc("The Builder is inside this private final class for DSL aesthetics")
        .primaryConstructor(FunSpec.constructorBuilder().addModifiers(KModifier.PRIVATE).build())

    private val builderName = "${symbols.rootId}Builder"
    private val builderClass = containerClass.nestedClass("Builder")
    private val builder = TypeSpec.classBuilder(builderClass)
        .addAnnotation(Annotations.suppress("ClassName"))
        .addProperty(
            PropertySpec.builder("factory", factoryClass)
                .addModifiers(KModifier.PRIVATE)
                .initializer("factory")
                .build()
        )
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter(ParameterSpec.builder("factory", factoryClass).build())
                .build()
        )

    override fun apply(universe: UniverseSpec) {
        super.apply(universe)
        universe.packages.add(
            PackageSpec(
                name = builderPackageName,
                files = mutableListOf(
                    FileSpec.builder(builderPackageName, factoryName)
                        .addType(factory.build())
                        .build(),
                    FileSpec.builder(builderPackageName, builderName)
                        .addType(
                            container
                                .addType(builder.build())
                                .addType(builderCompanion())
                                .build()
                        )
                        .build(),
                )
            )
        )
    }

    override fun apply(node: NodeSpec.Product) {
        // Simple `create` functions
        factory.addFunction(
            FunSpec.builder(symbols.camel(node.product.ref))
                .addModifiers(KModifier.OPEN)
                .apply {
                    node.props.forEach {
                        addParameter(it.name, it.type)
                    }
                    addStatement("return %T(${node.props.joinToString { it.name }})", node.clazz)
                }
                .build()
        )
        // DSL Receiver and Function
        val dsl = node.dslConstructs()
        builder.addType(dsl.first)
        builder.addFunction(dsl.second)
        super.apply(node)
    }

    // --- Internal -------------------

    /**
     * Creates the static entry-points for the DSL and Factory
     */
    private fun builderCompanion() = TypeSpec.companionObjectBuilder().apply {
        val t = TypeVariableName("T", symbols.base)
        addFunction(
            FunSpec.builder("build")
                .addAnnotation(Annotations.jvmStatic)
                .addTypeVariable(t)
                .addParameter(
                    ParameterSpec.builder("factory", factoryClass)
                        .defaultValue("%T.DEFAULT", factoryClass)
                        .build()
                )
                .addParameter(
                    ParameterSpec.builder(
                        "block",
                        LambdaTypeName.get(
                            receiver = builderClass,
                            returnType = t,
                        )
                    ).build()
                )
                .addStatement("return %T(factory).block()", builderClass)
                .build()
        )
        addFunction(
            FunSpec.builder("create")
                .addAnnotation(Annotations.jvmStatic)
                .addTypeVariable(t)
                .addParameter(
                    ParameterSpec.builder(
                        "block",
                        LambdaTypeName.get(
                            receiver = factoryClass,
                            returnType = t,
                        )
                    ).build()
                )
                .addStatement("return %T.DEFAULT.block()", factoryClass)
                .build()
        )
    }.build()

    private fun NodeSpec.Product.dslConstructs(): Pair<TypeSpec, FunSpec> {
        val receiverName = symbols.camel(product.ref)
        val receiverType = builderClass.nestedClass("_${receiverName.toPascalCase()}")
        val receiverConstructor = FunSpec.constructorBuilder()
        val receiver = TypeSpec.classBuilder(receiverType)
        val dslFunction = FunSpec.builder(receiverName).returns(clazz)
        product.props.forEachIndexed { i, it ->
            // we want a mutable and nullable type for builder
            val type = symbols.typeNameOf(it.ref, mutable = true).copy(true)
            val name = props[i].name
            val para = ParameterSpec.builder(name, type).defaultValue("null").build()
            val prop = PropertySpec.builder(name, type).initializer(name).mutable().build()
            receiver.addProperty(prop)
            receiverConstructor.addParameter(para)
            dslFunction.addParameter(para)
        }
        // block last
        dslFunction.addParameter(
            ParameterSpec.builder(
                "block",
                LambdaTypeName.get(
                    receiver = receiverType,
                    returnType = Unit::class.asTypeName()
                )
            )
                .defaultValue("{}")
                .build()
        )
        val r = receiver.primaryConstructor(receiverConstructor.build()).build()
        val f = dslFunction
            .addStatement("val b = %T(${props.joinToString { it.name }})", receiverType)
            .addStatement("b.block()")
            .addStatement("return factory.$receiverName(${props.joinToString { "${it.name} = b.${it.name}!!" }})")
            .build()
        return Pair(r, f)
    }
}
