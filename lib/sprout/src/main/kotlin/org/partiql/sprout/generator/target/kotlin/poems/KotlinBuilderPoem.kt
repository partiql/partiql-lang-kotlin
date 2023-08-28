package org.partiql.sprout.generator.target.kotlin.poems

import com.squareup.kotlinpoet.AnnotationSpec
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
import com.squareup.kotlinpoet.buildCodeBlock
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

    private val idProviderType = LambdaTypeName.get(returnType = String::class.asTypeName())
    private val idProvider = PropertySpec.builder("_id", idProviderType).build()

    // Abstract factory which can be used by DSL blocks
    private val factoryName = "${symbols.rootId}Factory"
    private val factoryClass = ClassName(builderPackageName, factoryName)
    private val factory = TypeSpec.interfaceBuilder(factoryClass)
        .addProperty(idProvider)

    private val baseFactoryName = "${symbols.rootId}FactoryImpl"
    private val baseFactoryClass = ClassName(builderPackageName, baseFactoryName)
    private val baseFactory = TypeSpec.classBuilder(baseFactoryClass)
        .addSuperinterface(factoryClass)
        .addModifiers(KModifier.OPEN)
        .addProperty(
            idProvider.toBuilder()
                .addModifiers(KModifier.OVERRIDE)
                .initializer(
                    "{ %P }",
                    buildCodeBlock {
                        // universe-${"%08x".format(Random.nextInt())}
                        add("${symbols.rootId}-\${%S.format(%T.nextInt())}", "%08x", ClassName("kotlin.random", "Random"))
                    }
                )
                .build()
        )

    private val factoryParamDefault = ParameterSpec.builder("factory", factoryClass)
        .defaultValue("%T.DEFAULT", factoryClass)
        .build()

    // Assume there's a <DOMAIN>.kt file in the package root containing the default builder
    private val factoryDefault = ClassName(symbols.rootPackage, symbols.rootId)

    // Java style builders, used by the DSL
    private val buildersName = "${symbols.rootId}Builders"
    private val buildersFile = FileSpec.builder(builderPackageName, buildersName)

    // @file:Suppress("UNUSED_PARAMETER")
    private val suppressUnused = AnnotationSpec.builder(Suppress::class)
        .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
        .addMember("%S", "UNUSED_PARAMETER")
        .build()

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
                    // Factory Interface
                    FileSpec.builder(builderPackageName, factoryName)
                        .addType(factory.addType(factoryCompanion()).build())
                        .build(),
                    // Factory Base
                    FileSpec.builder(builderPackageName, baseFactoryName)
                        .addType(baseFactory.build())
                        .build(),
                    // Java Builders
                    buildersFile.build(),
                    // DSL
                    FileSpec.builder(builderPackageName, dslName)
                        .addAnnotation(suppressUnused)
                        .addFunction(dslFunc)
                        .addType(dslSpec.build())
                        .build(),
                )
            )
        )
    }

    override fun apply(node: KotlinNodeSpec.Product) {
        val function = FunSpec.builder(symbols.camel(node.product.ref))
            .apply {
                node.props.forEach {
                    addParameter(it.name, it.type)
                }
            }
            .returns(node.clazz)
            .build()
        // interface
        factory.addFunction(function.toBuilder().addModifiers(KModifier.ABSTRACT).build())
        // impl
        baseFactory.addFunction(
            function.toBuilder()
                .addModifiers(KModifier.OVERRIDE)
                .returns(node.clazz)
                .apply {
                    val args = listOf("_id()") + node.props.map {
                        // add as function parameter
                        it.name
                    }
                    // Inject identifier `node(id(), props...)`
                    addStatement("return %T(${args.joinToString()})", node.implClazz)
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
        val builderConstructor = FunSpec.constructorBuilder()
        val builder = TypeSpec.classBuilder(builderType)

        // DSL Function
        val funcDsl = FunSpec.builder(builderName).returns(clazz)
        funcDsl.addStatement("val builder = %T(${ props.joinToString { it.name }})", builderType)

        // Java builder `build(factory: Factory = DEFAULT): T`
        val funcBuild = FunSpec.builder("build").addParameter(factoryParamDefault).returns(clazz)
        val args = mutableListOf<String>()

        // Add builder function to node interface
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

            val para = ParameterSpec.builder(name, type).build()

            // t: T = default
            val paraDefault = para.toBuilder().defaultValue(default).build()
            funcDsl.addParameter(paraDefault)
            builderConstructor.addParameter(paraDefault)

            // public var t: T
            val prop = PropertySpec.builder(name, type).initializer(name).mutable().build()
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
        builder.primaryConstructor(builderConstructor.build())

        // Finalize DSL function
        funcDsl.addStatement("builder.block()")
        funcDsl.addStatement("return builder.build(factory)")

        return Pair(builder.build(), funcDsl.build())
    }

    private fun factoryCompanion() = TypeSpec.companionObjectBuilder()
        .addProperty(
            PropertySpec.builder("DEFAULT", factoryClass)
                .initializer("%T()", baseFactoryClass)
                .build()
        )
        .addFunction(factoryFunc)
        .build()
}
