package org.partiql.sprout.generator.target.kotlin.poems

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
import org.partiql.sprout.generator.target.kotlin.KotlinPoem
import org.partiql.sprout.generator.target.kotlin.KotlinSymbols
import org.partiql.sprout.generator.target.kotlin.spec.KotlinNodeSpec
import org.partiql.sprout.generator.target.kotlin.spec.KotlinPackageSpec
import org.partiql.sprout.generator.target.kotlin.spec.KotlinUniverseSpec
import org.partiql.sprout.generator.target.kotlin.types.Parameters
import org.partiql.sprout.model.TypeDef
import org.partiql.sprout.model.TypeRef

/**
 * Poem which makes nodes traversable via `children` and a `<Universe>Visitor`
 */
class KotlinVisitorPoem(symbols: KotlinSymbols) : KotlinPoem(symbols) {

    override val id = "visitor"

    private val children = PropertySpec.Companion.builder("children", LIST.parameterizedBy(symbols.base)).build()

    private val visitorPackageName = "${symbols.rootPackage}.visitor"

    // Interface visitor
    private val visitorName = "${symbols.rootId}Visitor"
    private val visitorClass = ClassName(visitorPackageName, visitorName).parameterizedBy(Parameters.R, Parameters.C)

    // Abstract visitor with default walking
    private val baseVisitorName = "${symbols.rootId}BaseVisitor"

    private val accept = FunSpec.builder("accept")
        .addTypeVariable(Parameters.R)
        .addTypeVariable(Parameters.C)
        .addParameter("visitor", visitorClass)
        .addParameter("ctx", Parameters.C)
        .returns(Parameters.R)
        .build()

    private val baseVisit = FunSpec.builder("visit")
        .addParameter("node", symbols.base)
        .addParameter("ctx", Parameters.C)
        .returns(Parameters.R)
        .build()

    /**
     * Defines the open `children` property and the abstract`accept` method on the base node
     */
    override fun apply(universe: KotlinUniverseSpec) {
        universe.base.addProperty(
            children.toBuilder()
                .addModifiers(KModifier.ABSTRACT)
                .build()
        )
        universe.base.addFunction(
            accept.toBuilder()
                .addModifiers(KModifier.ABSTRACT)
                .build()
        )
        universe.packages.add(
            KotlinPackageSpec(
                name = visitorPackageName,
                files = universe.visitors().toMutableList(),
            )
        )
        super.apply(universe)
    }

    /**
     * Overrides `children` and `accept` for this product node
     */
    override fun apply(node: KotlinNodeSpec.Product) {
        val kids = node.kids()
        if (kids != null) {
            node.impl.addProperty(
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
        } else {
            node.impl.addProperty(
                children.toBuilder()
                    .addModifiers(KModifier.OVERRIDE)
                    .initializer("emptyList()")
                    .build()
            )
        }
        node.impl.addFunction(
            accept.toBuilder()
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("return visitor.%L(this, ctx)", node.product.ref.visitMethodName())
                .build()
        )
        super.apply(node)
    }

    /**
     * Overrides `accept` for this sum node
     */
    override fun apply(node: KotlinNodeSpec.Sum) {
        node.builder.addFunction(
            accept.toBuilder()
                .addModifiers(KModifier.OVERRIDE)
                .apply {
                    beginControlFlow("return when (this)")
                    node.sum.variants.forEach {
                        addStatement("is %T -> visitor.%L(this, ctx)", symbols.clazz(it.ref), it.ref.visitMethodName())
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
    private fun KotlinNodeSpec.Product.kids(): CodeBlock? {
        var n = product.props.size
        val isNode: (ref: TypeRef) -> Boolean = { (it is TypeRef.Path) && (symbols.def(it) !is TypeDef.Enum) }
        val block = CodeBlock.builder()
            .addStatement("val kids = mutableListOf<%T?>()", symbols.base)
            .apply {
                product.props.forEachIndexed { i, prop ->
                    val kid = prop.ref
                    val name = props[i].name
                    val action: String? = when {
                        isNode(kid) -> "add"
                        (kid is TypeRef.List && isNode(kid.type)) -> "addAll"
                        (kid is TypeRef.Set && isNode(kid.type)) -> "addAll"
                        else -> {
                            n -= 1
                            null
                        }
                    }
                    if (action != null) {
                        when (kid.nullable) {
                            true -> addStatement("$name?.let { kids.$action(it) }")
                            false -> addStatement("kids.$action($name)")
                        }
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
    private fun KotlinUniverseSpec.visitors(): List<FileSpec> = listOf(visitor(), baseVisitor())

    /**
     * Generates the visitor interface for this universe
     */
    private fun KotlinUniverseSpec.visitor(): FileSpec {
        val visitor = TypeSpec.interfaceBuilder(visitorName)
            .addTypeVariable(Parameters.R)
            .addTypeVariable(Parameters.C)
            .apply {
                addFunction(baseVisit.toBuilder().addModifiers(KModifier.ABSTRACT).build())
                forEachNode {
                    val visit = it.visit().addModifiers(KModifier.ABSTRACT).build()
                    addFunction(visit)
                }
            }
            .build()
        return FileSpec.builder(visitorPackageName, visitorName).addType(visitor).build()
    }

    /**
     * Generates the base visitor for this universe
     */
    private fun KotlinUniverseSpec.baseVisitor(): FileSpec {
        val defaultVisit = FunSpec.builder("defaultVisit")
            .addModifiers(KModifier.OPEN)
            .addParameter(ParameterSpec("node", symbols.base))
            .addParameter(ParameterSpec("ctx", Parameters.C))
            .returns(Parameters.R)
            .beginControlFlow("for (child in node.children)")
            .addStatement("child.accept(this, ctx)")
            .endControlFlow()
            .addStatement("return defaultReturn(node, ctx)")
            .build()
        val defaultReturn = FunSpec.builder("defaultReturn")
            .addModifiers(KModifier.ABSTRACT)
            .addParameter(ParameterSpec("node", symbols.base))
            .addParameter(ParameterSpec("ctx", Parameters.C))
            .returns(Parameters.R)
            .build()
        val visitor = TypeSpec.classBuilder(baseVisitorName)
            .addSuperinterface(visitorClass)
            .addModifiers(KModifier.ABSTRACT)
            .addTypeVariable(Parameters.R)
            .addTypeVariable(Parameters.C)
            .apply {
                addFunction(
                    baseVisit.toBuilder()
                        .addModifiers(KModifier.OVERRIDE)
                        .addCode("return node.accept(this, ctx)")
                        .build()
                )
                forEachNode {
                    addFunction(it.defaultVisit())
                }
            }
            .addFunction(defaultVisit)
            .addFunction(defaultReturn)
            .build()
        return FileSpec.builder(visitorPackageName, baseVisitorName).addType(visitor).build()
    }

    /**
     * Visit interface method
     */
    private fun KotlinNodeSpec.visit() = FunSpec.builder(def.ref.visitMethodName())
        .addParameter(ParameterSpec("node", clazz))
        .addParameter(ParameterSpec("ctx", Parameters.C))
        .returns(Parameters.R)

    /**
     * Visit default method
     */
    private fun KotlinNodeSpec.defaultVisit() = visit()
        .addModifiers(KModifier.OVERRIDE)
        .apply {
            when (this@defaultVisit) {
                is KotlinNodeSpec.Product -> addStatement("return defaultVisit(node, ctx)")
                is KotlinNodeSpec.Sum -> {
                    beginControlFlow("return when (node)")
                    sum.variants.forEach {
                        addStatement("is %T -> %L(node, ctx)", symbols.clazz(it.ref), it.ref.visitMethodName())
                    }
                    endControlFlow()
                }
            }
        }.build()

    /**
     * Returns the visit method name of the given TypeRef.Path
     */
    private fun TypeRef.Path.visitMethodName() = "visit${symbols.pascal(this)}"
}
