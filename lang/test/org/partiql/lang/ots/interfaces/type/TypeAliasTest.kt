package org.partiql.lang.ots.interfaces.type

import OTS.ITF.org.partiql.ots.TypeParameters
import OTS.ITF.org.partiql.ots.type.ScalarType
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.compiler.PartiQLCompilerBuilder
import org.partiql.lang.eval.visitors.PartiqlAstSanityValidator
import org.partiql.lang.planner.PlannerPipeline
import org.partiql.lang.syntax.PartiQLParser

class TypeAliasTest {
    private val parser = PartiQLParser(ion)
    private val sanityValidator = PartiqlAstSanityValidator()

    // Pass cases
    @Test
    fun `refer to registered type aliases`() {
        val myType = object : DummyScalarType() {
            override val aliases: List<String> = listOf("my_type", "another_alias")
            override fun validateParameters(typeParameters: TypeParameters) {}
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
        val ast1 = parser.parseAstStatement("CAST(1 AS another_alias)")

        // The following should not throw error since they are valid type aliases
        sanityValidator.validate(ast0, plugin = myPlugin)
        sanityValidator.validate(ast1, plugin = myPlugin)
    }

    // Error cases
    @Test
    fun `refer to unregistered type alias`() {
        val myType = object : DummyScalarType() {
            override val aliases: List<String> = listOf("my_type")
            override fun validateParameters(typeParameters: TypeParameters) {}
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
        val ast = parser.parseAstStatement("CAST(1 AS wrong_alias)")

        // The following should throw error due to no such scalar type
        assertThrows<RuntimeException> {
            sanityValidator.validate(ast, plugin = myPlugin)
        }
    }

    @Test
    fun `multiple types should not share the same type alias`() {
        val myType0 = object : DummyScalarType() {
            override val aliases: List<String> = listOf("my_type")
        }
        val myType1 = object : DummyScalarType() {
            override val aliases: List<String> = listOf("my_type")
        }
        val myPlugin = object : DummyPlugin() {
            override val scalarTypes: List<ScalarType>
                get() = listOf(myType0, myType1)

            override fun findScalarType(typeAlias: String): ScalarType? {
                scalarTypes.forEach {
                    if (typeAlias in it.aliases) {
                        return it
                    }
                }

                return null
            }
        }

        assertThrows<RuntimeException> {
            CompilerPipeline.builder(ion).plugin(myPlugin).build()
        }
        assertThrows<RuntimeException> {
            PlannerPipeline.builder(ion).plugin(myPlugin).build()
        }
        assertThrows<RuntimeException> {
            PartiQLCompilerBuilder.standard().plugin(myPlugin).build()
        }
    }

    @Test
    fun `type alias should not have space`() {
        val myType = object : DummyScalarType() {
            override val aliases: List<String> = listOf("my type")
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

        assertThrows<RuntimeException> {
            CompilerPipeline.builder(ion).plugin(myPlugin).build()
        }
        assertThrows<RuntimeException> {
            PlannerPipeline.builder(ion).plugin(myPlugin).build()
        }
        assertThrows<RuntimeException> {
            PartiQLCompilerBuilder.standard().plugin(myPlugin).build()
        }
    }

    @Test
    fun `multiple types should not share the same type name`() {
        val myType0 = object : DummyScalarType() {
            override val typeName = "my_type"
        }
        val myType1 = object : DummyScalarType() {
            override val typeName = "my_type"
        }
        val myPlugin = object : DummyPlugin() {
            override val scalarTypes: List<ScalarType>
                get() = listOf(myType0, myType1)

            override fun findScalarType(typeAlias: String): ScalarType? {
                scalarTypes.forEach {
                    if (typeAlias in it.aliases) {
                        return it
                    }
                }

                return null
            }
        }

        assertThrows<RuntimeException> {
            CompilerPipeline.builder(ion).plugin(myPlugin).build()
        }
        assertThrows<RuntimeException> {
            PlannerPipeline.builder(ion).plugin(myPlugin).build()
        }
        assertThrows<RuntimeException> {
            PartiQLCompilerBuilder.standard().plugin(myPlugin).build()
        }
    }
}
