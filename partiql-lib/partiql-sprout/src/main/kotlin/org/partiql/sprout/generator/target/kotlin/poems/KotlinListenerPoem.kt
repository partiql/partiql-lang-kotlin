package org.partiql.sprout.generator.target.kotlin.poems

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.TypeSpec
import org.partiql.sprout.generator.target.kotlin.KotlinPoem
import org.partiql.sprout.generator.target.kotlin.KotlinSymbols
import org.partiql.sprout.generator.target.kotlin.spec.KotlinNodeSpec
import org.partiql.sprout.generator.target.kotlin.spec.KotlinPackageSpec
import org.partiql.sprout.generator.target.kotlin.spec.KotlinUniverseSpec

class KotlinListenerPoem(symbols: KotlinSymbols) : KotlinPoem(symbols) {

    override val id = "listener"

    private val listenerPackageName = "${symbols.rootPackage}.listener"
    private val baseListenerName = "${symbols.rootId}Listener"
    private val baseListenerClass = ClassName(listenerPackageName, baseListenerName)

    private val enter = FunSpec.builder("enter").addParameter("listener", baseListenerClass).build()
    private val exit = FunSpec.builder("exit").addParameter("listener", baseListenerClass).build()

    /**
     * Defines the open `children` property and the abstract`accept` method on the base node
     */
    override fun apply(universe: KotlinUniverseSpec) {
        universe.base.addFunction(enter.toBuilder().addModifiers(KModifier.ABSTRACT).build())
        universe.base.addFunction(exit.toBuilder().addModifiers(KModifier.ABSTRACT).build())
        universe.packages.add(
            KotlinPackageSpec(
                name = listenerPackageName,
                files = mutableListOf(universe.listener(), walker()),
            )
        )
        super.apply(universe)
    }

    override fun apply(node: KotlinNodeSpec) {
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

    private fun KotlinUniverseSpec.listener(): FileSpec {
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

    private fun KotlinNodeSpec.simpleName() = clazz.simpleNames.joinToString("")
}
