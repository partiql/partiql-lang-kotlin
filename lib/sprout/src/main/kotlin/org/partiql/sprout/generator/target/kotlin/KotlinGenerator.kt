package org.partiql.sprout.generator.target.kotlin

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asTypeName
import net.pearx.kasechange.toCamelCase
import org.partiql.sprout.generator.Generator
import org.partiql.sprout.generator.target.kotlin.poems.KotlinBuilderPoem
import org.partiql.sprout.generator.target.kotlin.poems.KotlinJacksonPoem
import org.partiql.sprout.generator.target.kotlin.poems.KotlinListenerPoem
import org.partiql.sprout.generator.target.kotlin.poems.KotlinUtilsPoem
import org.partiql.sprout.generator.target.kotlin.poems.KotlinVisitorPoem
import org.partiql.sprout.generator.target.kotlin.spec.KotlinFileSpec
import org.partiql.sprout.generator.target.kotlin.spec.KotlinNodeSpec
import org.partiql.sprout.generator.target.kotlin.spec.KotlinUniverseSpec
import org.partiql.sprout.model.TypeDef
import org.partiql.sprout.model.TypeProp
import org.partiql.sprout.model.Universe

/**
 * Generates and applies
 */
class KotlinGenerator(private val options: KotlinOptions) : Generator<KotlinResult> {

    override fun generate(universe: Universe): KotlinResult {

        // --- Initialize an empty symbol table(?)
        val symbols = KotlinSymbols.init(universe, options)

        // Still not sure if poems should be registered with an injector
        // This is sufficient for now
        val poems = options.poems.map {
            when (it) {
                "visitor" -> KotlinVisitorPoem(symbols)
                "builder" -> KotlinBuilderPoem(symbols)
                "listener" -> KotlinListenerPoem(symbols)
                "jackson" -> KotlinJacksonPoem(symbols)
                "util" -> KotlinUtilsPoem(symbols)
                else -> error("unknown poem $it, expected: visitor, builder, listener, jackson, util")
            }
        }

        // --- Generate skeleton
        val spec = KotlinUniverseSpec(
            universe = universe,
            nodes = universe.nodes(symbols),
            base = TypeSpec.interfaceBuilder(symbols.base),
            types = universe.types(symbols)
        )
        val specs = with(spec) {
            // Add identifiers
            base.addProperty(PropertySpec.builder("_id", String::class).addModifiers(KModifier.ABSTRACT).build())
            // Apply each poem
            poems.forEach { it.apply(this) }
            // Finalize each spec/builder
            build(options.packageRoot).map {
                if (options.optIns.isNotEmpty()) {
                    val f = it.toBuilder()
                    val optin = AnnotationSpec.builder(ClassName("kotlin", "OptIn"))
                    options.optIns.forEach { o ->
                        val i = o.lastIndexOf(".")
                        optin.addMember("%T::class", ClassName(o.substring(0, i), o.substring(i + 1)))
                    }
                    f.addAnnotation(optin.build())
                    KotlinFileSpec(f.build())
                } else {
                    KotlinFileSpec(it)
                }
            }
        }
        return KotlinResult(specs)
    }

    // --- Internal -----------------------------------

    /**
     * Generate a NodeSpec for each Type in the given Universe
     */
    private fun Universe.nodes(symbols: KotlinSymbols): List<KotlinNodeSpec> =
        types.mapNotNull { it.generate(symbols) }.map {
            it.builder.addSuperinterface(symbols.base)
            it
        }

    /**
     * Generate all top-level enums as these are not children of a product definition
     */
    private fun Universe.types(symbols: KotlinSymbols) =
        types.filterIsInstance<TypeDef.Enum>().map { it.generate(symbols) }.toMutableList()

    /**
     * Entry point for node generation.
     */
    private fun TypeDef.generate(symbols: KotlinSymbols): KotlinNodeSpec? = when (this) {
        is TypeDef.Product -> this.generate(symbols)
        is TypeDef.Sum -> this.generate(symbols)
        is TypeDef.Enum -> null // enums are constants, not nodes
    }

    /**
     * Product Node Generation
     */
    private fun TypeDef.Product.generate(symbols: KotlinSymbols): KotlinNodeSpec {
        val clazz = symbols.clazz(ref)
        val clazzImpl = ClassName(
            packageName = clazz.packageName + ".impl",
            simpleNames = listOf(symbols.pascal(ref) + "Impl"),
        )
        return KotlinNodeSpec.Product(
            product = this,
            props = props.map { KotlinNodeSpec.Prop(it.name.toCamelCase(), symbols.typeNameOf(it.ref)) },
            implClazz = clazzImpl,
            impl = TypeSpec.classBuilder(clazzImpl),
            nodes = children.mapNotNull { it.generate(symbols) },
            clazz = symbols.clazz(ref),
            ext = (props.enumProps(symbols) + types.enums(symbols)).toMutableList(),
        ).apply {
            // Add id to impl
            impl.addProperty(symbols.idProp)
            constructor.addParameter(symbols.idPara)
            props.forEach {
                val para = ParameterSpec.builder(it.name, it.type).build()
                val prop = PropertySpec.builder(it.name, it.type).build()
                builder.addProperty(prop.toBuilder().addModifiers(KModifier.ABSTRACT).build())
                impl.addProperty(prop.toBuilder().addModifiers(KModifier.OVERRIDE).initializer(it.name).build())
                constructor.addParameter(para)
            }
            // impls are open
            impl.superclass(clazz)
            nodes.forEach { it.builder.addSuperinterface(symbols.base) }
            this.addDataClassMethods()
        }
    }

    /**
     * Sum node generation
     */
    private fun TypeDef.Sum.generate(symbols: KotlinSymbols) = KotlinNodeSpec.Sum(
        sum = this,
        variants = variants.mapNotNull { it.generate(symbols) },
        nodes = types.mapNotNull { it.generate(symbols) },
        clazz = symbols.clazz(ref),
        ext = types.enums(symbols).toMutableList(),
    ).apply {
        variants.forEach { it.builder.addSuperinterface(clazz) }
        nodes.forEach { it.builder.addSuperinterface(symbols.base) }
    }

    /**
     * Enum constant generation
     */
    private fun TypeDef.Enum.generate(symbols: KotlinSymbols) =
        TypeSpec.enumBuilder(symbols.clazz(ref)).apply { values.forEach { addEnumConstant(it) } }.build()

    private fun List<TypeProp>.enumProps(symbols: KotlinSymbols) = filterIsInstance<TypeProp.Inline>().mapNotNull {
        when (it.def) {
            is TypeDef.Enum -> it.def.generate(symbols)
            else -> null
        }
    }

    private fun List<TypeDef>.enums(symbols: KotlinSymbols) = filterIsInstance<TypeDef.Enum>().map {
        it.generate(symbols)
    }

    // TODO generate hashCode, equals, componentN so we can have OPEN internal implementations
    private fun KotlinNodeSpec.Product.addDataClassMethods() {
        impl.addModifiers(KModifier.INTERNAL, KModifier.OPEN)
        addEqualsMethod()
        addHashCodeMethod()
        val args = listOf("_id") + props.map { it.name }
        val copy = FunSpec.builder("copy").addModifiers(KModifier.ABSTRACT).returns(clazz)
        val copyImpl = FunSpec.builder("copy")
            .addModifiers(KModifier.OVERRIDE)
            .returns(clazz)
            .addStatement("return %T(${args.joinToString()})", implClazz)
        props.forEach {
            val para = ParameterSpec.builder(it.name, it.type).build()
            copy.addParameter(para.toBuilder().defaultValue("this.${it.name}").build())
            copyImpl.addParameter(para)
        }
        builder.addFunction(copy.build())
        impl.addFunction(copyImpl.build())
    }

    /**
     * Adds `equals` method to the core abstract class
     */
    private fun KotlinNodeSpec.Product.addEqualsMethod() {
        val equalsFunctionBodyBuilder = CodeBlock.builder().let { body ->
            body.addStatement("if (this === other) return true")
            body.addStatement("if (other !is %T) return false", this.clazz)
            this.props.forEach { prop ->
                body.addStatement("if (%N != other.%N) return false", prop.name, prop.name)
            }
            body.addStatement("return true")
        }
        builder.addFunction(
            FunSpec.builder("equals").addModifiers(KModifier.OVERRIDE).returns(Boolean::class)
                .addParameter(ParameterSpec.builder("other", Any::class.asTypeName().copy(nullable = true)).build())
                .addCode(equalsFunctionBodyBuilder.build())
                .build()
        )
    }

    /**
     * Adds `hashCode` method to the core abstract class
     */
    private fun KotlinNodeSpec.Product.addHashCodeMethod() {
        val hashcodeBodyBuilder = CodeBlock.builder().let { body ->
            when (this.props.size) {
                0 -> body.addStatement("return 0")
                1 -> body.addStatement("return %N.hashCode()", this.props.first().name)
                else -> {
                    body.addStatement("var result = %N.hashCode()", this.props.first().name)
                    this.props.subList(1, this.props.size).forEach { prop ->
                        body.addStatement("result = 31 * result + %N.hashCode()", prop.name)
                    }
                    body.addStatement("return result")
                }
            }
            body
        }
        builder.addFunction(
            FunSpec.builder("hashCode")
                .addModifiers(KModifier.OVERRIDE)
                .returns(Int::class)
                .addCode(hashcodeBodyBuilder.build())
                .build()
        )
    }
}
