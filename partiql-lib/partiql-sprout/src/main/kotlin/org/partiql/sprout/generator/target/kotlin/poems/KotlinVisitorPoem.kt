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
import org.partiql.sprout.generator.target.kotlin.spec.KotlinUniverseSpec
import org.partiql.sprout.generator.target.kotlin.spec.PackageSpec
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
    override fun apply(universe: KotlinUniverseSpec) {
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
    override fun apply(node: KotlinNodeSpec.Product) {
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
    override fun apply(node: KotlinNodeSpec.Sum) {
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
    private fun KotlinNodeSpec.Product.kids(): CodeBlock? {
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
    private fun KotlinUniverseSpec.visitors(): List<FileSpec> = listOf(
        visitor(),
        // visitorFold(), VisitorFold is a less useful version of Visitor
    )

    /**
     * Generates the base visitor for this universe
     */
    private fun KotlinUniverseSpec.visitor(): FileSpec {
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

    private fun KotlinNodeSpec.visit(): FunSpec = when (this) {
        is KotlinNodeSpec.Product -> this.visit()
        is KotlinNodeSpec.Sum -> this.visit()
    }

    private fun KotlinNodeSpec.Product.visit() = FunSpec.builder("visit")
        .addModifiers(KModifier.OPEN)
        .addParameter(ParameterSpec("node", clazz))
        .addParameter(ParameterSpec("ctx", Parameters.`C?`))
        .returns(Parameters.`R?`)
        .addStatement("return defaultVisit(node, ctx)")
        .build()

    private fun KotlinNodeSpec.Sum.visit() = FunSpec.builder("visit")
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
