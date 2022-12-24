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
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.partiql.sprout.generator.Poem
import org.partiql.sprout.generator.Symbols
import org.partiql.sprout.generator.spec.NodeSpec
import org.partiql.sprout.generator.spec.PackageSpec
import org.partiql.sprout.generator.spec.UniverseSpec
import org.partiql.sprout.generator.types.Parameters
import org.partiql.sprout.model.TypeDef
import org.partiql.sprout.model.TypeRef

/**
 * Poem which makes nodes traversable via `children` and a `<Universe>Visitor`
 */
class VisitorPoem(symbols: Symbols) : Poem(symbols) {

    override val id = "visitor"

    private val children = PropertySpec.builder("children", LIST.parameterizedBy(symbols.base)).build()

    private val visitorPackageName = "${symbols.rootPackage}.visitor"
    private val baseVisitorName = "${symbols.rootId}Visitor"
    private val baseVisitorClass = ClassName(visitorPackageName, baseVisitorName)

    private val accept = FunSpec.builder("accept")
        .addTypeVariable(Parameters.R)
        .addTypeVariable(Parameters.C)
        .addParameter("visitor", baseVisitorClass.parameterizedBy(Parameters.R, Parameters.C))
        .returns(Parameters.`R?`)
        .build()

    /**
     * Defines the open `children` property and the abstract`accept` method on the base node
     */
    override fun apply(universe: UniverseSpec) {
        universe.base.addProperty(
            children.toBuilder()
                .addModifiers(KModifier.OPEN)
                .initializer("emptyList()")
                .build()
        )
        universe.base.addFunction(
            accept.toBuilder()
                .addModifiers(KModifier.ABSTRACT)
                .addParameter(ParameterSpec.builder("ctx", Parameters.`C?`).defaultValue("null").build())
                .build()
        )
        universe.packages.add(
            PackageSpec(
                name = visitorPackageName,
                files = universe.visitors().toMutableList(),
            )
        )
        super.apply(universe)
    }

    /**
     * Overrides `children` and `accept` for this product node
     */
    override fun apply(node: NodeSpec.Product) {
        val kids = node.kids()
        if (kids != null) {
            node.builder.addProperty(
                children.toBuilder()
                    .addModifiers(KModifier.OVERRIDE)
                    .delegate(
                        CodeBlock.builder()
                            .beginControlFlow("lazy")
                            .add(kids)
                            .endControlFlow()
                            .build()
                    )
                    .build()
            )
        }
        node.builder.addFunction(
            accept.toBuilder()
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(ParameterSpec.builder("ctx", Parameters.`C?`).build())
                .addStatement("return visitor.visit(this, ctx)")
                .build()
        )
    }

    /**
     * Overrides `accept` for this sum node
     */
    override fun apply(node: NodeSpec.Sum) {
        node.builder.addFunction(
            accept.toBuilder()
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(ParameterSpec.builder("ctx", Parameters.`C?`).build())
                .apply {
                    beginControlFlow("return when (this)")
                    node.sum.variants.forEach {
                        addStatement("is %T -> visitor.visit(this, ctx)", symbols.clazz(it.ref))
                    }
                    endControlFlow()
                }
                .build()
        )
        super.apply(node)
    }

    // --- Internal ----------------------------------------------------

    /**
     * Returns a CodeBlock which represents a list of all nodes
     */
    private fun NodeSpec.Product.kids(): CodeBlock? {
        var n = product.props.size
        val isNode: (ref: TypeRef) -> Boolean = { (it is TypeRef.Path) && (symbols.def(it) !is TypeDef.Enum) }
        val block = CodeBlock.builder()
            .addStatement("val kids = mutableListOf<%T?>()", symbols.base)
            .apply {
                product.props.forEachIndexed { i, prop ->
                    val kid = prop.ref
                    val name = props[i].name
                    when {
                        isNode(kid) -> addStatement("kids.add($name)")
                        (kid is TypeRef.List && isNode(kid.type)) -> addStatement("kids.addAll($name)")
                        (kid is TypeRef.Set && isNode(kid.type)) -> addStatement("kids.addAll($name)")
                        else -> n -= 1
                    }
                }
            }
            .addStatement("kids.filterNotNull()")
            .build()
        return if (n != 0) block else null
    }

    /**
     * Generate all visitors for this universe
     */
    private fun UniverseSpec.visitors(): List<FileSpec> = listOf(
        visitor(),
        // visitorFold(), VisitorFold is a less useful version of Visitor
    )

    /**
     * Generates the base visitor for this universe
     */
    private fun UniverseSpec.visitor(): FileSpec {
        val defaultVisit = FunSpec.builder("defaultVisit")
            .addModifiers(KModifier.OPEN)
            .addParameter(ParameterSpec("node", symbols.base))
            .addParameter(ParameterSpec("ctx", Parameters.`C?`))
            .returns(Parameters.`R?`)
            .beginControlFlow("for (child in node.children)")
            .addStatement("child.accept(this, ctx)")
            .endControlFlow()
            .addStatement("return null")
            .build()
        val visitor = TypeSpec.classBuilder(baseVisitorName)
            .addModifiers(KModifier.ABSTRACT)
            .addTypeVariable(Parameters.R)
            .addTypeVariable(Parameters.C)
            .apply {
                forEachNode {
                    addFunction(it.visit())
                }
            }
            .addFunction(defaultVisit)
            .build()
        return FileSpec.builder(visitorPackageName, baseVisitorName).addType(visitor).build()
    }

    private fun visitorFold(): FileSpec {
        val foldVisitorName = "${baseVisitorName}Fold"
        val foldVisitorClass = ClassName(visitorPackageName, foldVisitorName)
        val defaultVisit = FunSpec.builder("defaultVisit")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(ParameterSpec("node", symbols.base))
            .addParameter(ParameterSpec("ctx", Parameters.`T?`))
            .returns(Parameters.`T?`)
            .addStatement("return node.children.foldRight(node.accept(this, ctx)) { child, acc -> child.accept(this, acc) }")
            .build()
        val visitor = TypeSpec.classBuilder(foldVisitorClass)
            .addTypeVariable(Parameters.T)
            .superclass(baseVisitorClass.parameterizedBy(listOf(Parameters.T, Parameters.T)))
            .addFunction(defaultVisit)
            .build()
        return FileSpec.builder(visitorPackageName, foldVisitorName).addType(visitor).build()
    }

    private fun NodeSpec.visit(): FunSpec = when (this) {
        is NodeSpec.Product -> this.visit()
        is NodeSpec.Sum -> this.visit()
    }

    private fun NodeSpec.Product.visit() = FunSpec.builder("visit")
        .addModifiers(KModifier.OPEN)
        .addParameter(ParameterSpec("node", clazz))
        .addParameter(ParameterSpec("ctx", Parameters.`C?`))
        .returns(Parameters.`R?`)
        .addStatement("return defaultVisit(node, ctx)")
        .build()

    private fun NodeSpec.Sum.visit() = FunSpec.builder("visit")
        .addModifiers(KModifier.OPEN)
        .addParameter(ParameterSpec("node", clazz))
        .addParameter(ParameterSpec("ctx", Parameters.`C?`))
        .returns(Parameters.`R?`)
        .apply {
            beginControlFlow("return when (node)")
            sum.variants.forEach {
                addStatement("is %T -> visit(node, ctx)", symbols.clazz(it.ref))
            }
            endControlFlow()
        }
        .build()
}
