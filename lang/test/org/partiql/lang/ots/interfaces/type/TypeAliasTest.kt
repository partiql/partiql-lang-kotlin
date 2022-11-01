package org.partiql.lang.ots.interfaces.type

import OTS.ITF.org.partiql.ots.type.ScalarType
import OTS.ITF.org.partiql.ots.type.TypeParameters
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.compiler.PartiQLCompilerBuilder
import org.partiql.lang.eval.visitors.PartiqlAstSanityValidator
import org.partiql.lang.planner.PlannerPipeline
import org.partiql.lang.syntax.PartiQLParser
import org.partiql.lang.util.TypeRegistry

class TypeAliasTest {
    private val parser = PartiQLParser(ion)
    private val sanityValidator = PartiqlAstSanityValidator()

    // Pass cases
    @Test
    fun `refer to registered type aliases`() {
        val myType = object : DummyScalarType() {
            override val names: List<String> = listOf("my_type", "another_alias")
            override fun validateParameters(typeParameters: TypeParameters) {}
        }
        val myPlugin = object : DummyPlugin() {
            override val scalarTypes: List<ScalarType>
                get() = listOf(myType)
        }

        // Parsing should always succeed
        val ast0 = parser.parseAstStatement("CAST(1 AS my_type)")
        val ast1 = parser.parseAstStatement("CAST(1 AS another_alias)")

        // The following should not throw error since they are valid type aliases
        sanityValidator.validate(ast0, typeRegistry = TypeRegistry(myPlugin.scalarTypes))
        sanityValidator.validate(ast1, typeRegistry = TypeRegistry(myPlugin.scalarTypes))
    }

    // Error cases
    @Test
    fun `refer to unregistered type alias`() {
        val myType = object : DummyScalarType() {
            override val names: List<String> = listOf("my_type")
            override fun validateParameters(typeParameters: TypeParameters) {}
        }
        val myPlugin = object : DummyPlugin() {
            override val scalarTypes: List<ScalarType>
                get() = listOf(myType)
        }

        // Parsing should always succeed
        val ast = parser.parseAstStatement("CAST(1 AS wrong_alias)")

        // The following should throw error due to no such scalar type
        assertThrows<RuntimeException> {
            sanityValidator.validate(ast, typeRegistry = TypeRegistry(myPlugin.scalarTypes))
        }
    }

    @Test
    fun `multiple types should not share the same type alias`() {
        val myType0 = object : DummyScalarType() {
            override val names: List<String> = listOf("my_type")
        }
        val myType1 = object : DummyScalarType() {
            override val names: List<String> = listOf("my_type")
        }
        val myPlugin = object : DummyPlugin() {
            override val scalarTypes: List<ScalarType>
                get() = listOf(myType0, myType1)
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
            override val names: List<String> = listOf("my type")
        }
        val myPlugin = object : DummyPlugin() {
            override val scalarTypes: List<ScalarType>
                get() = listOf(myType)
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
            override val id = "my_type"
        }
        val myType1 = object : DummyScalarType() {
            override val id = "my_type"
        }
        val myPlugin = object : DummyPlugin() {
            override val scalarTypes: List<ScalarType>
                get() = listOf(myType0, myType1)
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
