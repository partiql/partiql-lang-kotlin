package org.partiql.sprout.generator.target.kotlin.poems

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
import org.partiql.sprout.generator.target.kotlin.KotlinPoem
import org.partiql.sprout.generator.target.kotlin.KotlinSymbols
import org.partiql.sprout.generator.target.kotlin.spec.KotlinNodeSpec
import org.partiql.sprout.generator.target.kotlin.spec.KotlinPackageSpec
import org.partiql.sprout.generator.target.kotlin.spec.KotlinUniverseSpec
import org.partiql.sprout.generator.target.kotlin.types.Annotations
import org.partiql.sprout.model.TypeRef

/**
 * Poem which creates a DSL for instantiation
 */
class KotlinBuilderPoem(symbols: KotlinSymbols) : KotlinPoem(symbols) {

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

    override fun apply(universe: KotlinUniverseSpec) {
        super.apply(universe)
        universe.packages.add(
            KotlinPackageSpec(
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

    override fun apply(node: KotlinNodeSpec.Product) {
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

    private fun KotlinNodeSpec.Product.dslConstructs(): Pair<TypeSpec, FunSpec> {
        val receiverName = symbols.camel(product.ref)
        val receiverType = builderClass.nestedClass("_${receiverName.toPascalCase()}")
        val receiverConstructor = FunSpec.constructorBuilder()
        val receiver = TypeSpec.classBuilder(receiverType)
        val dslFunction = FunSpec.builder(receiverName).returns(clazz)
        val args = mutableListOf<String>()
        product.props.forEachIndexed { i, it ->
            var type = symbols.typeNameOf(it.ref, mutable = true)
            val name = props[i].name
            val default = when (it.ref) {
                is TypeRef.List -> "mutableListOf()"
                is TypeRef.Set -> "mutableSetOf()"
                is TypeRef.Map -> "mutableMapOf()"
                else -> {
                    type = type.copy(nullable = true)
                    "null"
                }
            }
            val para = ParameterSpec.builder(name, type).defaultValue(default).build()
            val prop = PropertySpec.builder(name, type).initializer(name).mutable().build()
            receiver.addProperty(prop)
            receiverConstructor.addParameter(para)
            dslFunction.addParameter(para)
            val assertion = if (!it.ref.nullable && default == "null") "!!" else ""
            args += "$name = b.$name$assertion"
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
            .addStatement("return factory.$receiverName(${args.joinToString()})")
            .build()
        return Pair(r, f)
    }
}
