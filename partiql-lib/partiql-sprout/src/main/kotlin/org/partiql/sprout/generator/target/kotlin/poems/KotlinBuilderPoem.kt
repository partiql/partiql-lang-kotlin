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
import net.pearx.kasechange.toCamelCase
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

    // Abstract factory which can be used by DSL blocks
    private val factoryName = "${symbols.rootId}Factory"
    private val factoryClass = ClassName(builderPackageName, factoryName)
    private val factory = TypeSpec.classBuilder(factoryClass).addModifiers(KModifier.ABSTRACT)
    private val factoryParamDefault = ParameterSpec.builder("factory", factoryClass)
        .defaultValue("%T.DEFAULT", factoryClass)
        .build()

    // Java style builders, used by the DSL
    private val buildersName = "${symbols.rootId}Builders"
    private val buildersFile = FileSpec.builder(builderPackageName, buildersName)

    // Top-Level DSL holder, so that was close on the factory
    private val dslName = "${symbols.rootId}Builder"
    private val dslClass = ClassName(builderPackageName, dslName)
    private val dslSpec = TypeSpec.classBuilder(dslClass)
        .addProperty(
            PropertySpec.builder("factory", factoryClass)
                .addModifiers(KModifier.PRIVATE)
                .initializer("factory")
                .build()
        )
        .primaryConstructor(FunSpec.constructorBuilder().addParameter(factoryParamDefault).build())

    // T : FooNode
    private val boundedT = TypeVariableName("T", symbols.base)

    // Static top-level entry point for DSL
    private val dslFunc = FunSpec.builder(symbols.rootId.toCamelCase())
        .addTypeVariable(boundedT)
        .addParameter(factoryParamDefault)
        .addParameter(
            ParameterSpec.builder(
                "block",
                LambdaTypeName.get(
                    receiver = dslClass,
                    returnType = boundedT,
                )
            ).build()
        )
        .addStatement("return %T(factory).block()", dslClass)
        .build()

    // Static companion object entry point for factory, similar to PIG "build"
    private val factoryFunc = FunSpec.builder("create")
        .addAnnotation(Annotations.jvmStatic)
        .addTypeVariable(boundedT)
        .addParameter(
            ParameterSpec.builder(
                "block",
                LambdaTypeName.get(
                    receiver = factoryClass,
                    returnType = boundedT,
                )
            ).build()
        )
        .addStatement("return %T.DEFAULT.block()", factoryClass)
        .build()

    override fun apply(universe: KotlinUniverseSpec) {
        super.apply(universe)
        universe.packages.add(
            KotlinPackageSpec(
                name = builderPackageName,
                files = mutableListOf(
                    // Factory
                    FileSpec.builder(builderPackageName, factoryName)
                        .addType(factory.addType(factoryCompanion()).build())
                        .build(),
                    // Java Builders
                    buildersFile.build(),
                    // DSL
                    FileSpec.builder(builderPackageName, dslName)
                        .addFunction(dslFunc)
                        .addType(dslSpec.build())
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
                    node.props.forEach { addParameter(it.name, it.type) }
                    addStatement("return %T(${node.props.joinToString { it.name }})", node.clazz)
                }
                .build()
        )
        // DSL Receiver and Function
        val (builder, func) = node.builderToFunc()
        buildersFile.addType(builder)
        dslSpec.addFunction(func)
        super.apply(node)
    }

    // --- Internal -------------------

    /**
     * Returns a Pair of the Java builder and the Kotlin builder receiver function
     * This could be split for clarity, but it could be repetitive.
     */
    private fun KotlinNodeSpec.Product.builderToFunc(): Pair<TypeSpec, FunSpec> {
        // Java Builder, empty constructor
        val builderName = symbols.camel(product.ref)
        val builderType = ClassName(builderPackageName, "${builderName.toPascalCase()}Builder")
        val builder = TypeSpec.classBuilder(builderType)

        // DSL Function
        val funcDsl = FunSpec.builder(builderName).returns(clazz)
        funcDsl.addStatement("val builder = %T()", builderType)

        // Java builder `build(factory: Factory = DEFAULT): T`
        val funcBuild = FunSpec.builder("build").addParameter(factoryParamDefault).returns(clazz)
        val args = mutableListOf<String>()

        companion.addFunction(
            FunSpec.builder("builder")
                .addAnnotation(Annotations.jvmStatic)
                .returns(builderType)
                .addStatement("return %T()", builderType)
                .build()
        )

        // Add all props to Java builder, DSL function, and Factory call
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
            // t: T = default
            val para = ParameterSpec.builder(name, type).build()
            funcDsl.addParameter(para.toBuilder().defaultValue(default).build())
            // public var t: T
            val prop = PropertySpec.builder(name, type).initializer(default).mutable().build()
            builder.addProperty(prop)

            // Fluent builder method, only setters for now, can add collection manipulation later
            builder.addFunction(
                FunSpec.builder(name)
                    .returns(builderType)
                    .addParameter(para)
                    .beginControlFlow("return this.apply")
                    .addStatement("this.%N = %N", para, para)
                    .endControlFlow()
                    .build()
            )

            // Add parameter to `build(factory: Factory =)` me
            val assertion = if (!it.ref.nullable && default == "null") "!!" else ""
            args += "$name = $name$assertion"
        }

        // Add block as last parameter
        funcDsl.addParameter(
            ParameterSpec.builder(
                "block",
                LambdaTypeName.get(
                    receiver = builderType,
                    returnType = Unit::class.asTypeName()
                )
            )
                .defaultValue("{}")
                .build()
        )

        // End of factory.foo call
        funcBuild.addStatement("return factory.$builderName(${args.joinToString()})")

        // Finalize Java builder
        builder.addFunction(
            FunSpec.builder("build")
                .returns(clazz)
                .addStatement("return build(%T.DEFAULT)", factoryClass)
                .build()
        )
        builder.addFunction(funcBuild.build())

        // Finalize DSL function
        funcDsl.addStatement("builder.block()")
        funcDsl.addStatement("return builder.build(factory)")

        return Pair(builder.build(), funcDsl.build())
    }

    private fun factoryCompanion() = TypeSpec.companionObjectBuilder()
        .addProperty(
            PropertySpec.builder("DEFAULT", factoryClass)
                .initializer("object : %T() {}", factoryClass)
                .build()
        )
        .addFunction(factoryFunc)
        .build()
}
