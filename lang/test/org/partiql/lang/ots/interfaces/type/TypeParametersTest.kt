package org.partiql.lang.ots.interfaces.type

import OTS.ITF.org.partiql.ots.TypeParameters
import OTS.ITF.org.partiql.ots.type.NonParametricType
import OTS.ITF.org.partiql.ots.type.ScalarType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.TypedOpBehavior
import org.partiql.lang.eval.visitors.PartiqlAstSanityValidator
import org.partiql.lang.syntax.PartiQLParser

class TypeParametersTest {
    private val parser = PartiQLParser(ion)
    private val sanityValidator = PartiqlAstSanityValidator()
    private val compileOptions = CompileOptions
        .builder()
        .typedOpBehavior(TypedOpBehavior.HONOR_PARAMETERS)
        .build()

    @Test
    fun nonParametricType() {
        val myType = object : NonParametricType() {
            override val typeName: String = "my_type"
            override val aliases: List<String> = listOf("my_type")
            override val runTimeType: ExprValueType
                get() = error("Not yet implemented")
        }
        val myPlugin = object : DummyPlugin() {
            override val scalarTypes: List<ScalarType>
                get() = listOf(myType)

            override fun findScalarType(typeAlias: String): ScalarType? {
                scalarTypes.forEach {
                    if (typeAlias in it.aliases) {
                        return it
                    }
                }

                return null
            }
        }

        // Parsing should always succeed
        val ast0 = parser.parseAstStatement("CAST(1 AS my_type)")
        val ast1 = parser.parseAstStatement("CAST(1 AS my_type(2))")

        // Valid type parameter
        sanityValidator.validate(ast0, compileOptions, myPlugin)
        // Invalid type parameter
        assertThrows<RuntimeException> {
            sanityValidator.validate(ast1, compileOptions, myPlugin)
        }
    }

    @Test
    fun typeParametersArity() {
        val myType = object : DummyScalarType() {
            override val typeName: String = "my_type"
            override val aliases: List<String> = listOf("my_type")

            override fun validateParameters(typeParameters: TypeParameters) {
                if (typeParameters.size > 1) {
                    error("${typeName.toUpperCase()} type requires only 1 type parameter")
                }
            }
        }
        val myPlugin = object : DummyPlugin() {
            override val scalarTypes: List<ScalarType>
                get() = listOf(myType)

            override fun findScalarType(typeAlias: String): ScalarType? {
                scalarTypes.forEach {
                    if (typeAlias in it.aliases) {
                        return it
                    }
                }

                return null
            }
        }

        // Parsing should always succeed
        val ast0 = parser.parseAstStatement("CAST(1 AS my_type)")
        val ast1 = parser.parseAstStatement("CAST(1 AS my_type(2))")
        val ast2 = parser.parseAstStatement("CAST(1 AS my_type(1, 1))")

        // The following should succeed since they have valid arity
        sanityValidator.validate(ast0, compileOptions, myPlugin)
        sanityValidator.validate(ast1, compileOptions, myPlugin)

        // The following should fail due to invalid arity
        assertThrows<RuntimeException> {
            sanityValidator.validate(ast2, compileOptions, myPlugin)
        }
    }

    @Test
    fun typeParametersValue() {
        val myType = object : DummyScalarType() {
            override val typeName: String = "my_type"
            override val aliases: List<String> = listOf("my_type")
            val defaultParameter = 5

            override fun validateParameters(typeParameters: TypeParameters) {
                if (typeParameters.size > 1) {
                    error("${typeName.toUpperCase()} type requires only 1 type parameter")
                }

                val typeParameter = typeParameters.getOrNull(0) ?: defaultParameter

                if (typeParameter <= 0 || typeParameter >= 10) {
                    error("the parameter of type ${typeName.toUpperCase()} should be larger than 0 & smaller than 10")
                }
            }
        }
        val myPlugin = object : DummyPlugin() {
            override val scalarTypes: List<ScalarType>
                get() = listOf(myType)

            override fun findScalarType(typeAlias: String): ScalarType? {
                scalarTypes.forEach {
                    if (typeAlias in it.aliases) {
                        return it
                    }
                }

                return null
            }
        }

        // Parsing should always succeed
        val ast0 = parser.parseAstStatement("CAST(1 AS my_type(2))")
        val ast1 = parser.parseAstStatement("CAST(1 AS my_type(11))")

        // The following should succeed since it has valid value
        sanityValidator.validate(ast0, compileOptions, myPlugin)

        // The following should fail due to invalid value
        assertThrows<RuntimeException> {
            sanityValidator.validate(ast1, compileOptions, myPlugin)
        }
    }
}
