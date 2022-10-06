package org.partiql.lang.ots.interfaces.type

import OTS.ITF.org.partiql.ots.type.NonParametricType
import OTS.ITF.org.partiql.ots.type.ScalarType
import OTS.ITF.org.partiql.ots.type.TypeParameters
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.lang.eval.CompileOptions
import org.partiql.lang.eval.ExprValueType
import org.partiql.lang.eval.TypedOpBehavior
import org.partiql.lang.eval.visitors.PartiqlAstSanityValidator
import org.partiql.lang.syntax.PartiQLParser
import org.partiql.lang.util.TypeRegistry

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
            override val id: String = "my_type"
            override val names: List<String> = listOf("my_type")
            override val runTimeType: ExprValueType
                get() = error("Not yet implemented")
        }
        val myPlugin = object : DummyPlugin() {
            override val scalarTypes: List<ScalarType>
                get() = listOf(myType)
        }

        // Parsing should always succeed
        val ast0 = parser.parseAstStatement("CAST(1 AS my_type)")
        val ast1 = parser.parseAstStatement("CAST(1 AS my_type(2))")

        // Valid type parameter
        sanityValidator.validate(ast0, compileOptions, TypeRegistry(myPlugin.scalarTypes))
        // Invalid type parameter
        assertThrows<RuntimeException> {
            sanityValidator.validate(ast1, compileOptions, TypeRegistry(myPlugin.scalarTypes))
        }
    }

    @Test
    fun typeParametersArity() {
        val myType = object : DummyScalarType() {
            override val id: String = "my_type"
            override val names: List<String> = listOf("my_type")

            override fun validateParameters(typeParameters: TypeParameters) {
                if (typeParameters.size > 1) {
                    error("${id.toUpperCase()} type requires only 1 type parameter")
                }
            }
        }
        val myPlugin = object : DummyPlugin() {
            override val scalarTypes: List<ScalarType>
                get() = listOf(myType)
        }

        // Parsing should always succeed
        val ast0 = parser.parseAstStatement("CAST(1 AS my_type)")
        val ast1 = parser.parseAstStatement("CAST(1 AS my_type(2))")
        val ast2 = parser.parseAstStatement("CAST(1 AS my_type(1, 1))")

        // The following should succeed since they have valid arity
        sanityValidator.validate(ast0, compileOptions, TypeRegistry(myPlugin.scalarTypes))
        sanityValidator.validate(ast1, compileOptions, TypeRegistry(myPlugin.scalarTypes))

        // The following should fail due to invalid arity
        assertThrows<RuntimeException> {
            sanityValidator.validate(ast2, compileOptions, TypeRegistry(myPlugin.scalarTypes))
        }
    }

    @Test
    fun typeParametersValue() {
        val myType = object : DummyScalarType() {
            override val id: String = "my_type"
            override val names: List<String> = listOf("my_type")
            val defaultParameter = 5

            override fun validateParameters(typeParameters: TypeParameters) {
                if (typeParameters.size > 1) {
                    error("${id.toUpperCase()} type requires only 1 type parameter")
                }

                val typeParameter = typeParameters.getOrNull(0) ?: defaultParameter

                if (typeParameter <= 0 || typeParameter >= 10) {
                    error("the parameter of type ${id.toUpperCase()} should be larger than 0 & smaller than 10")
                }
            }
        }
        val myPlugin = object : DummyPlugin() {
            override val scalarTypes: List<ScalarType>
                get() = listOf(myType)
        }

        // Parsing should always succeed
        val ast0 = parser.parseAstStatement("CAST(1 AS my_type(2))")
        val ast1 = parser.parseAstStatement("CAST(1 AS my_type(11))")

        // The following should succeed since it has valid value
        sanityValidator.validate(ast0, compileOptions, TypeRegistry(myPlugin.scalarTypes))

        // The following should fail due to invalid value
        assertThrows<RuntimeException> {
            sanityValidator.validate(ast1, compileOptions, TypeRegistry(myPlugin.scalarTypes))
        }
    }
}
