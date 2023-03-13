package org.partiql.sprout.parser.ion

import com.amazon.ion.IonStruct
import com.amazon.ion.system.IonSystemBuilder
import com.amazon.ionelement.api.ionListOf
import com.amazon.ionelement.api.ionStructOf
import com.amazon.ionelement.api.ionSymbol
import com.amazon.ionelement.api.toIonValue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class IonImportsTest {

    private val ion = IonSystemBuilder.standard().build()

    @Test
    internal fun testMultipleImports() {
        val struct = ionStructOf(
            "kotlin" to ionListOf(
                type("a", "kotlin.a"),
                type("b", "kotlin.b"),
                type("c", "kotlin.c"),
            ),
            "rust" to ionListOf(
                type("a", "rust.a"),
                type("b", "rust.b"),
                type("c", "rust.c"),
            ),
            "go" to ionListOf(
                type("a", "go.a"),
                type("b", "go.b"),
                type("c", "go.c"),
            ),
        ).toIonValue(ion)
        val imports = IonImports.build(struct as IonStruct)
        assert(imports.symbols.size == 3)
        assert(imports.map.contains("kotlin"))
        assert(imports.map.contains("rust"))
        assert(imports.map.contains("go"))
    }

    @Test
    internal fun testDuplicateImport() {
        val struct = ionStructOf(
            "kotlin" to ionListOf(
                type("a", "kotlin.x"),
                type("a", "kotlin.y"),
            )
        ).toIonValue(ion)
        assertThrows<RuntimeException> {
            IonImports.build(struct as IonStruct)
        }
    }

    @Test
    internal fun testMissingImports() {
        val struct = ionStructOf(
            "kotlin" to ionListOf(
                type("a", "kotlin.a"),
                type("b", "kotlin.b"),
                type("c", "kotlin.c"),
            ),
            "rust" to ionListOf(
                type("a", "rust.a"),
                type("b", "rust.b"),
            ),
            "go" to ionListOf(
                type("a", "go.a"),
            ),
        ).toIonValue(ion)
        assertThrows<RuntimeException> {
            IonImports.build(struct as IonStruct)
        }
    }

    private fun type(symbol: String, path: String) = ionSymbol(path).withAnnotations(symbol)
}
