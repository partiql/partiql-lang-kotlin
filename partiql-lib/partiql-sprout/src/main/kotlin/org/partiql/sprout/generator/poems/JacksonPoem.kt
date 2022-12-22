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
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.NOTHING
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.TypeVariableName
import org.partiql.sprout.generator.Poem
import org.partiql.sprout.generator.Symbols
import org.partiql.sprout.generator.spec.NodeSpec
import org.partiql.sprout.generator.spec.PackageSpec
import org.partiql.sprout.generator.spec.UniverseSpec
import org.partiql.sprout.generator.types.JacksonTypes
import org.partiql.sprout.generator.types.Parameters

/**
 * Poem for Jackson Databind
 */
class JacksonPoem(symbols: Symbols) : Poem(symbols) {

    override val id: String = "jackson"

    private val databindPackageName = "${symbols.rootPackage}.databind"

    // Consider making a separate Jackson+Builder poem; this is not an ideal solution
    private val factoryName = ClassName("${symbols.rootPackage}.builder", "${symbols.rootId}Factory")

    private val moduleName = "${symbols.rootId}Module"
    private val moduleClass = ClassName(databindPackageName, moduleName)
    private val module = TypeSpec.classBuilder(moduleClass)
        .superclass(JacksonTypes.Databind.simpleModule)
        .addProperty(
            PropertySpec.builder("factory", factoryName)
                .addModifiers(KModifier.PRIVATE)
                .initializer("factory")
                .build()
        )
        .primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter(
                    ParameterSpec.builder("factory", factoryName)
                        .defaultValue("%T.DEFAULT", factoryName)
                        .build()
                )
                .build()
        )

    private val inheritedProps = mutableSetOf<String>()

    private val mappingInterfaceName = moduleClass.nestedClass("Mapping")
    private val mappingInterface = TypeSpec.interfaceBuilder(mappingInterfaceName)
        .addModifiers(KModifier.PRIVATE, KModifier.FUN)
        .addTypeVariable(TypeVariableName("T", listOf(symbols.base), KModifier.OUT))
        .addFunction(
            FunSpec.builder("invoke")
                .returns(Parameters.T)
                .addModifiers(KModifier.OPERATOR, KModifier.ABSTRACT)
                .addParameter("node", JacksonTypes.Databind.jsonNode)
                .build()
        )
        .build()
    private val initBlock = CodeBlock.builder()

    override fun apply(universe: UniverseSpec) {
        // Jackson will serialize inherited properties; collect and ignore these before descending
        universe.base.propertySpecs.forEach { inheritedProps.add(it.name) }
        universe.addBaseDeserializer()
        super.apply(universe)
        module.addHelpers()
        module.addInitializerBlock(initBlock.build())
        universe.packages.add(
            PackageSpec(
                name = databindPackageName,
                files = mutableListOf(
                    FileSpec.builder(databindPackageName, moduleName)
                        .addType(module.build())
                        .build()
                )
            )
        )
    }

    override fun apply(node: NodeSpec.Product) {

        // --- Serialization

        // Ignore all properties not in the type definition, with the caveat that the Jackson poem must be last
        val allProps = inheritedProps + node.builder.propertySpecs.map { it.name }.toSet()
        val definedProps = node.props.map { it.name }.toSet()
        val extraneous = allProps - definedProps
        if (extraneous.isNotEmpty()) {
            node.builder.addAnnotation(JacksonTypes.Annotation.ignore(extraneous))
        }
        // Preserve the definition's path without a custom serializer
        val cairn = "_id"
        node.builder.addProperty(
            PropertySpec.builder(cairn, STRING)
                .addModifiers(KModifier.PRIVATE)
                .addAnnotation(JacksonTypes.Annotation.property(cairn))
                .initializer("%S", node.product.ref.id)
                .build()
        )
        node.builder.addAnnotation(JacksonTypes.Annotation.order(cairn))

        // --- Deserialization
        node.addDeserializer {
            val method = symbols.camel(node.product.ref)
            addStatement("factory.$method(")
            node.props.forEachIndexed { i, prop ->
                val name = prop.name
                val ref = node.product.props[i].ref
                addStatement("$name = ${symbols.valueMapping(ref, "it[\"$name\"]")},")
            }
            addStatement(")")
        }
    }

    override fun apply(node: NodeSpec.Sum) {
        node.addDeserializer {
            beginControlFlow("when (val id = it.id())")
            node.variants.forEach {
                addStatement("%S -> _${symbols.camel(it.def.ref)}(it)", it.def.ref.id)
            }
            addStatement("else -> err(id)")
            endControlFlow()
        }
        super.apply(node)
    }

    private fun NodeSpec.addDeserializer(mapping: CodeBlock.Builder.() -> Unit) {
        val method = symbols.camel(def.ref)
        // Add node mapping function
        module.addProperty(
            PropertySpec.builder("_$method", mappingInterfaceName.parameterizedBy(clazz))
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    CodeBlock.builder()
                        .beginControlFlow("Mapping")
                        .apply(mapping)
                        .endControlFlow()
                        .build()
                )
                .build()
        )
        // Register deserializer
        initBlock.addStatement("addDeserializer(%T::class.java, map(_$method))", clazz)
    }

    private fun TypeSpec.Builder.addHelpers() {
        addFunction(
            FunSpec.builder("id")
                .addModifiers(KModifier.PRIVATE)
                .receiver(JacksonTypes.Databind.jsonNode)
                .returns(STRING)
                .addStatement("return get(%S).asText()", "_id")
                .build()
        )
        addFunction(
            FunSpec.builder("err")
                .addModifiers(KModifier.INLINE, KModifier.PRIVATE)
                .addParameter("id", STRING)
                .returns(NOTHING)
                .addStatement("return error(%P)", "no deserializer registered for _id `\$id`")
                .build()
        )

        val deserializer = JacksonTypes.Databind.jsonDeserializer.parameterizedBy(Parameters.T)
        addFunction(
            FunSpec.builder("map")
                .addModifiers(KModifier.PRIVATE)
                .addTypeVariable(TypeVariableName("T", listOf(symbols.base)))
                .addParameter(ParameterSpec("mapping", mappingInterfaceName.parameterizedBy(Parameters.T)))
                .returns(deserializer)
                .addStatement(
                    "return %L",
                    TypeSpec.anonymousClassBuilder()
                        .superclass(deserializer)
                        .addFunction(
                            FunSpec.builder("deserialize")
                                .addModifiers(KModifier.OVERRIDE)
                                .addParameter("p", JacksonTypes.Core.jsonParser)
                                .addParameter("ctxt", JacksonTypes.Databind.deserializationContext)
                                .returns(Parameters.T)
                                .addStatement("return mapping(ctxt.readTree(p)!!)")
                                .build()
                        )
                        .build()
                )
                .build()
        )
        addType(mappingInterface)
    }

    /**
     * Map every type definition (except enums) to its class
     */
    private fun UniverseSpec.addBaseDeserializer() {
        module.addProperty(
            PropertySpec.builder("_base", mappingInterfaceName.parameterizedBy(symbols.base))
                .addModifiers(KModifier.PRIVATE)
                .initializer(
                    CodeBlock.builder()
                        .beginControlFlow("Mapping")
                        .beginControlFlow("when (val id = it.id())")
                        .apply {
                            forEachNode {
                                addStatement("%S -> _${symbols.camel(it.def.ref)}(it)", it.def.ref.id)
                            }
                        }
                        .addStatement("else -> err(id)")
                        .endControlFlow()
                        .endControlFlow()
                        .build()
                )
                .build()
        )
        initBlock.addStatement("addDeserializer(%T::class.java, map(_base))", symbols.base)
    }
}
