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

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import org.partiql.sprout.model.TypeDef

/**
 * Wraps a [TypeDef] with KotlinPoet builders
 *
 * @property def            TypeDef
 * @property clazz          Type ClassName
 * @property builder        Type builder
 * @property constructor    Implementation constructor
 * @property companion      A place for static methods on nodes
 * @property nodes          Node types defined within this node
 */
sealed class KotlinNodeSpec(
    val def: TypeDef,
    val clazz: ClassName,
    val builder: TypeSpec.Builder,
    val constructor: FunSpec.Builder,
    val companion: TypeSpec.Builder,
    val ext: MutableList<TypeSpec> = mutableListOf(),
) {

    /**
     * Nodes defined within this node, but don't inherit from this class
     */
    abstract val nodes: List<KotlinNodeSpec>

    /**
     * All types defined within this node
     */
    abstract val children: List<KotlinNodeSpec>

    /**
     * Returns the built Pair<Base, Impl>
     */
    open fun build(): TypeSpec = with(builder) {
        primaryConstructor(constructor.build())
        ext.forEach { addType(it) }
        children.forEach { addType(it.build()) }
        if (companion.propertySpecs.isNotEmpty() || companion.funSpecs.isNotEmpty()) {
            addType(companion.build())
        }
        build()
    }

    /**
     * Wraps a [TypeDef.Product] with codegen builders
     */
    class Product(
        val product: TypeDef.Product,
        val props: List<Prop>,
        override val nodes: List<KotlinNodeSpec>,
        clazz: ClassName,
        ext: MutableList<TypeSpec> = mutableListOf(),
    ) : KotlinNodeSpec(
        def = product,
        clazz = clazz,
        builder = TypeSpec.classBuilder(clazz),
        constructor = FunSpec.constructorBuilder(),
        companion = TypeSpec.companionObjectBuilder(),
        ext = ext,
    ) {
        override val children: List<KotlinNodeSpec> = nodes
    }

    /**
     * Wraps a [TypeDef.Sum] with a codegen builders
     */
    class Sum(
        val sum: TypeDef.Sum,
        val variants: List<KotlinNodeSpec>,
        override val nodes: List<KotlinNodeSpec>,
        clazz: ClassName,
        ext: MutableList<TypeSpec> = mutableListOf(),
    ) : KotlinNodeSpec(
        def = sum,
        clazz = clazz,
        builder = TypeSpec.classBuilder(clazz).addModifiers(KModifier.SEALED),
        constructor = FunSpec.constructorBuilder(),
        companion = TypeSpec.companionObjectBuilder(),
        ext = ext,
    ) {
        override val children: List<KotlinNodeSpec> = variants + nodes
    }

    /**
     * Derived from a [TypeProp], but replaced the ref with a ClassName
     */
    class Prop(
        val name: String,
        val type: TypeName
    )
}
