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
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import org.partiql.sprout.generator.Poem
import org.partiql.sprout.generator.Symbols
import org.partiql.sprout.generator.spec.NodeSpec
import org.partiql.sprout.generator.spec.PackageSpec
import org.partiql.sprout.generator.spec.UniverseSpec

class ListenerPoem(symbols: Symbols) : Poem(symbols) {

    override val id = "listener"

    private val listenerPackageName = "${symbols.rootPackage}.listener"
    private val baseListenerName = "${symbols.rootId}Listener"
    private val baseListenerClass = ClassName(listenerPackageName, baseListenerName)

    private val enter = FunSpec.builder("enter").addParameter("listener", baseListenerClass).build()
    private val exit = FunSpec.builder("exit").addParameter("listener", baseListenerClass).build()

    /**
     * Defines the open `children` property and the abstract`accept` method on the base node
     */
    override fun apply(universe: UniverseSpec) {
        universe.base.addFunction(enter.toBuilder().addModifiers(KModifier.ABSTRACT).build())
        universe.base.addFunction(exit.toBuilder().addModifiers(KModifier.ABSTRACT).build())
        universe.packages.add(
            PackageSpec(
                name = listenerPackageName,
                files = mutableListOf(universe.listener(), walker()),
            )
        )
        super.apply(universe)
    }

    override fun apply(node: NodeSpec) {
        val name = node.simpleName()
        node.builder.addFunction(
            enter.toBuilder()
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("listener.enter$name(this)")
                .build()
        )
        node.builder.addFunction(
            exit.toBuilder()
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("listener.exit$name(this)")
                .build()
        )
        super.apply(node)
    }

    // --- Internal -----------------------------------

    private fun UniverseSpec.listener(): FileSpec {
        val listener = TypeSpec.classBuilder(baseListenerClass)
            .addModifiers(KModifier.ABSTRACT)
            .apply {
                forEachNode {
                    val name = it.simpleName()
                    addFunction(
                        FunSpec.builder("enter$name")
                            .addModifiers(KModifier.OPEN)
                            .addParameter("node", it.clazz)
                            .build()
                    )
                    addFunction(
                        FunSpec.builder("exit$name")
                            .addModifiers(KModifier.OPEN)
                            .addParameter("node", it.clazz)
                            .build()
                    )
                }
            }
            .addFunction(
                FunSpec.builder("enterEveryNode")
                    .addModifiers(KModifier.OPEN)
                    .addParameter("node", symbols.base)
                    .build()
            )
            .addFunction(
                FunSpec.builder("exitEveryNode")
                    .addModifiers(KModifier.OPEN)
                    .addParameter("node", symbols.base)
                    .build()
            )
            .build()
        return FileSpec.builder(listenerPackageName, baseListenerName)
            .addType(listener)
            .build()
    }

    private fun walker(): FileSpec {
        val walkerName = "${symbols.rootId}Walker"
        val walkerClass = ClassName(listenerPackageName, walkerName)
        val walk = FunSpec.builder("walk")
            .addParameter(ParameterSpec("listener", baseListenerClass))
            .addParameter(ParameterSpec("node", symbols.base))
            .apply {
                addStatement("listener.enterEveryNode(node)")
                addStatement("node.enter(listener)")
                addStatement("node.children.forEach { walk(listener, it) }")
                addStatement("node.exit(listener)")
                addStatement("listener.exitEveryNode(node)")
            }
            .build()
        val walker = TypeSpec.objectBuilder(walkerClass)
            .addFunction(walk)
            .build()
        return FileSpec.builder(listenerPackageName, walkerName)
            .addType(walker)
            .build()
    }

    private fun NodeSpec.simpleName() = clazz.simpleNames.joinToString("")
}
