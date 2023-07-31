package org.partiql.sprout.generator.target.kotlin.poems

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.SET
import com.squareup.kotlinpoet.TypeSpec
import net.pearx.kasechange.toCamelCase
import org.partiql.sprout.generator.target.kotlin.KotlinPoem
import org.partiql.sprout.generator.target.kotlin.KotlinSymbols
import org.partiql.sprout.generator.target.kotlin.spec.KotlinNodeSpec
import org.partiql.sprout.generator.target.kotlin.spec.KotlinPackageSpec
import org.partiql.sprout.generator.target.kotlin.spec.KotlinUniverseSpec
import org.partiql.sprout.generator.target.kotlin.types.Parameters
import org.partiql.sprout.model.TypeDef
import org.partiql.sprout.model.TypeRef

/**
 * This is a poem to generate utilities for working with graphical IRs.
 *
 * This was created a place to put a tree rewriter without creating a dependency between visitors and factories.
 * This is also where things like pretty printing might go.
 *
 * Again, KotlinPoet is verbose. Templating would be simpler. Handwritten preferred.
 */
class KotlinUtilsPoem(symbols: KotlinSymbols) : KotlinPoem(symbols) {

    override val id: String = "util"

    // @file:Suppress("UNUSED_PARAMETER")
    private val suppressUnused = AnnotationSpec.builder(Suppress::class)
        .useSiteTarget(AnnotationSpec.UseSiteTarget.FILE)
        .addMember("%S", "UNUSED_PARAMETER")
        .addMember("%S", "UNUSED_VARIABLE")
        .build()

    // Not taking a dep on builder or visitor poems, as this is temporary
    private val factoryClass = ClassName("${symbols.rootPackage}.builder", "${symbols.rootId}Factory")
    private val visitorBaseClass = ClassName("${symbols.rootPackage}.visitor", "${symbols.rootId}BaseVisitor")
        .parameterizedBy(symbols.base, Parameters.C)

    private val rewriterPackageName = "${symbols.rootPackage}.util"
    private val rewriterName = "${symbols.rootId}Rewriter"

    private val factory = symbols.rootId.toCamelCase()

    /**
     * Defines the open `children` property and the abstract`accept` method on the base node
     */
    override fun apply(universe: KotlinUniverseSpec) {
        val rewriter = TypeSpec.classBuilder(rewriterName)
            .superclass(visitorBaseClass)
            .addModifiers(KModifier.ABSTRACT)
            .addTypeVariable(Parameters.C)
            .apply {
                // open val foo: FooFactory = FooFactory.DEFAULT
                addProperty(
                    PropertySpec.builder(factory, factoryClass)
                        .addModifiers(KModifier.OPEN)
                        .initializer("%T.DEFAULT", factoryClass)
                        .build()
                )
                // override fun defaultReturn(node: PlanNode, ctx: C) = node
                addFunction(
                    FunSpec.builder("defaultReturn")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("node", symbols.base)
                        .addParameter("ctx", Parameters.C)
                        .returns(symbols.base)
                        .addStatement("return node")
                        .build()
                )
                addFunction(_visitCollection("_visitList", LIST, "ArrayList<T>", nullable = false))
                addFunction(_visitCollection("_visitListNull", LIST, "ArrayList<T?>", nullable = true))
                addFunction(_visitCollection("_visitSet", SET, "HashSet<T>", nullable = false))
                addFunction(_visitCollection("_visitSetNull", SET, "HashSet<T?>", nullable = true))
                // overrides for each product node
                universe.forEachNode {
                    if (it is KotlinNodeSpec.Product) {
                        val rewrite = it.rewriter()
                        addFunction(rewrite)
                    }
                }
            }
            .build()
        val rewriterFile = FileSpec.builder(rewriterPackageName, rewriterName)
            .addAnnotation(suppressUnused)
            .addType(rewriter)
            .build()
        universe.packages.add(
            KotlinPackageSpec(
                name = "util",
                files = mutableListOf(rewriterFile),
            )
        )
        super.apply(universe)
    }

    // Non-Node         -> node.n
    // Node N           -> visitN(node.n, ctx) as N
    // Node N?          -> node.n?.let { visitN(it, ctx) as N }
    // Collection<N>    -> rewrite(node, ctx, ::method)
    // Collection<N>?   -> node.n?.let { rewrite(it, ctx, ::method) }
    // Collection<N?>?  -> node.n?.let { rewrite(it, ctx, ::method) }
    // Collection<N?>   -> rewrite(node, ctx, ::method)

    private fun KotlinNodeSpec.Product.rewriter(): FunSpec {
        val visit = product.ref.visitMethodName()
        val constructor = symbols.camel(product.ref)
        return FunSpec.builder(visit)
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(ParameterSpec("node", clazz))
            .addParameter(ParameterSpec("ctx", Parameters.C))
            .returns(symbols.base)
            .apply {
                var n = product.props.size
                val names = product.props.mapIndexed { i, prop ->
                    val ref = prop.ref
                    val name = props[i].name
                    val type = props[i].type
                    val child = "node.$name"
                    // This will not handle collections of collections.
                    when {
                        isNode(ref) -> {
                            // Scalar
                            val method = (ref as TypeRef.Path).visitMethodName()
                            when (ref.nullable) {
                                true -> addStatement("val $name = $child?.let { $method(it, ctx) as %T }", type)
                                false -> addStatement("val $name = $method($child, ctx) as %T", type)
                            }
                        }
                        (ref is TypeRef.List && isNode(ref.type)) -> {
                            // Collections
                            val method = (ref.type as TypeRef.Path).visitMethodName()
                            val helper = when (ref.type.nullable) {
                                true -> "_visitListNull"
                                else -> "_visitList"
                            }
                            when (ref.nullable) {
                                true -> addStatement("val $name = $child?.let { $helper(it, ctx, ::$method) }")
                                false -> addStatement("val $name = $helper($child, ctx, ::$method)")
                            }
                        }
                        (ref is TypeRef.Set && isNode(ref.type)) -> {
                            // Collections
                            val method = (ref.type as TypeRef.Path).visitMethodName()
                            val helper = when (ref.type.nullable) {
                                true -> "_visitSetNull"
                                else -> "_visitSet"
                            }
                            when (ref.nullable) {
                                true -> addStatement("val $name = $child?.let { $helper(it, ctx, ::$method) }")
                                false -> addStatement("val $name = $helper($child, ctx, ::$method)")
                            }
                        }
                        else -> {
                            // non-node
                            addStatement("val $name = node.$name")
                            n -= 1
                        }
                    }
                    name
                }
                if (n == 0) {
                    addStatement("return node")
                    return@apply
                }
                val condition = names.joinToString(" || ") { "$it !== node.$it" }
                beginControlFlow("return if ($condition)")
                addStatement("$factory.$constructor(${names.joinToString(", ")})")
                nextControlFlow("else")
                addStatement("node")
                endControlFlow()
            }
            .build()
    }

    private fun isNode(ref: TypeRef): Boolean = (ref is TypeRef.Path) && (symbols.def(ref) !is TypeDef.Enum)

    private fun TypeRef.Path.visitMethodName() = "visit${symbols.pascal(this)}"

    // private inline fun <reified T> _$name(nodes: $parameter<T>, ctx: C, method: (node: T, ctx: C) -> PlanNode): $collection<T>
    private fun _visitCollection(
        name: String,
        parameter: ClassName,
        constructor: String,
        nullable: Boolean,
    ) = FunSpec.builder(name)
        .addModifiers(KModifier.PRIVATE, KModifier.INLINE)
        .addTypeVariable(Parameters.T.copy(reified = true))
        .addParameter("nodes", parameter.parameterizedBy(Parameters.T.copy(nullable)))
        .addParameter("ctx", Parameters.C)
        .addParameter(
            "method",
            LambdaTypeName.get(
                parameters = listOf(
                    ParameterSpec("node", Parameters.T),
                    ParameterSpec("ctx", Parameters.C),
                ),
                returnType = symbols.base
            )
        )
        .returns(parameter.parameterizedBy(Parameters.T.copy(nullable)))
        .addCode(
            CodeBlock.builder()
                .addStatement("if (nodes.isEmpty()) return nodes")
                .addStatement("var diff = false")
                .addStatement("val transformed = $constructor(nodes.size)")
                .beginControlFlow("nodes.forEach")
                .apply {
                    if (nullable) {
                        addStatement("val n = if (it == null) null else method(it, ctx) as T")
                    } else {
                        addStatement("val n = method(it, ctx) as T")
                    }
                }
                .addStatement("if (it !== n) diff = true")
                .addStatement("transformed.add(n)")
                .endControlFlow()
                .addStatement("return if (diff) transformed else nodes")
                .build()
        )
        .build()
}
