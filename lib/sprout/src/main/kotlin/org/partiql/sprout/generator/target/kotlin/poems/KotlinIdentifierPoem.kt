package org.partiql.sprout.generator.target.kotlin.poems

import com.squareup.kotlinpoet.asTypeName
import org.partiql.sprout.generator.target.kotlin.KotlinPoem
import org.partiql.sprout.generator.target.kotlin.KotlinSymbols
import org.partiql.sprout.generator.target.kotlin.spec.KotlinNodeSpec
import org.partiql.sprout.model.ScalarType
import org.partiql.sprout.model.TypeDef
import org.partiql.sprout.model.TypeProp
import org.partiql.sprout.model.TypeRef
import org.partiql.sprout.model.Universe

/**
 * This poem adds a required identifier (int) to all nodes for use in lookup tables.
 */
class KotlinIdentifierPoem(symbols: KotlinSymbols) : KotlinPoem(symbols) {

    override val id = "identifier"

    private val identifier = TypeProp.Ref("id", TypeRef.Scalar(ScalarType.INT))

    override fun redefine(universe: Universe): Universe {
        symbols.baseProps.add(
            KotlinNodeSpec.Prop(
                name = "id",
                type = Int::class.asTypeName(),
            )
        )
        return Universe(
            id = universe.id,
            imports = universe.imports,
            types = universe.types.map { redefine(it) }
        )
    }

    override fun redefine(type: TypeDef.Product): TypeDef {
        val props = listOf(identifier) + type.props.map {
            when (it) {
                is TypeProp.Ref -> it
                is TypeProp.Inline -> TypeProp.Inline(
                    name = it.name,
                    def = redefine(it.def)
                )
            }
        }
        return TypeDef.Product(type.ref, props, type.types.map { redefine(it) })
    }
}
