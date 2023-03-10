package org.partiql.sprout.generator.target.kotlin

import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import net.pearx.kasechange.toCamelCase
import org.partiql.sprout.generator.Generator
import org.partiql.sprout.generator.target.kotlin.poems.KotlinBuilderPoem
import org.partiql.sprout.generator.target.kotlin.poems.KotlinJacksonPoem
import org.partiql.sprout.generator.target.kotlin.poems.KotlinListenerPoem
import org.partiql.sprout.generator.target.kotlin.poems.KotlinMetasPoem
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
                "metas" -> KotlinMetasPoem(symbols)
                "jackson" -> KotlinJacksonPoem(symbols)
                else -> error("unknown poem $it, expected: visitor, builder, listener, metas, jackson")
            }
        }

        // --- Generate skeleton
        val spec = KotlinUniverseSpec(
            universe = universe,
            nodes = universe.nodes(symbols),
            base = TypeSpec.classBuilder(symbols.base).addModifiers(KModifier.ABSTRACT),
            types = universe.types(symbols)
        )
        val specs = with(spec) {
            // Apply each poem
            poems.forEach { it.apply(this) }
            // Finalize each spec/builder
            build(options.packageRoot).map { KotlinFileSpec(it) }
        }
        return KotlinResult(specs)
    }

    // --- Internal -----------------------------------

    /**
     * Generate a NodeSpec for each Type in the given Universe
     */
    private fun Universe.nodes(symbols: KotlinSymbols): List<KotlinNodeSpec> = types.mapNotNull { it.generate(symbols) }
        .map {
            it.builder.superclass(symbols.base)
            it
        }

    /**
     * Generate all top-level enums as these are not children of a product definition
     */
    private fun Universe.types(symbols: KotlinSymbols) = types.filterIsInstance<TypeDef.Enum>()
        .map { it.generate(symbols) }
        .toMutableList()

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
    private fun TypeDef.Product.generate(symbols: KotlinSymbols) = KotlinNodeSpec.Product(
        product = this,
        props = props.map { KotlinNodeSpec.Prop(it.name.toCamelCase(), symbols.typeNameOf(it.ref)) },
        clazz = symbols.clazz(ref),
        children = children.mapNotNull { it.generate(symbols) },
        types = props.filterIsInstance<TypeProp.Inline>().mapNotNull {
            when (it.def) {
                is TypeDef.Enum -> it.def.generate(symbols)
                else -> null
            }
        }
    ).apply {
        props.forEach {
            val para = ParameterSpec.builder(it.name, it.type).build()
            val prop = PropertySpec.builder(it.name, it.type).initializer(it.name).build()
            builder.addProperty(prop)
            constructor.addParameter(para)
        }
        when (options.node.modifier) {
            KotlinNodeOptions.Modifier.FINAL -> {}
            KotlinNodeOptions.Modifier.DATA -> if (props.isNotEmpty()) builder.addModifiers(KModifier.DATA)
            KotlinNodeOptions.Modifier.OPEN -> builder.addModifiers(KModifier.OPEN)
        }
        children.forEach { it.builder.superclass(symbols.base) }
    }

    /**
     * Sum node generation
     */
    private fun TypeDef.Sum.generate(symbols: KotlinSymbols) = KotlinNodeSpec.Sum(
        sum = this,
        variants = variants.mapNotNull { it.generate(symbols) },
        clazz = symbols.clazz(ref),
        otherTypes = children.mapNotNull { it.generate(symbols) }
    ).apply {
        variants.forEach { it.builder.superclass(clazz) }
    }

    /**
     * Enum constant generation
     */
    private fun TypeDef.Enum.generate(symbols: KotlinSymbols) = TypeSpec.enumBuilder(symbols.clazz(ref))
        .apply { values.forEach { addEnumConstant(it) } }
        .build()
}
