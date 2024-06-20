package org.partiql.spi.connector.sql

import com.amazon.ion.system.IonTextWriterBuilder
import com.amazon.ionelement.api.IonElement
import com.amazon.ionelement.api.SymbolElement
import com.amazon.ionelement.api.ionListOf
import com.amazon.ionelement.api.ionSexpOf
import com.amazon.ionelement.api.ionString
import com.amazon.ionelement.api.ionStructOf
import com.amazon.ionelement.api.ionSymbol
import org.junit.jupiter.api.Test
import org.partiql.spi.fn.FnExperimental
import org.partiql.spi.fn.FnSignature
import org.partiql.types.PType
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.outputStream

@OptIn(FnExperimental::class)
class SqlBuiltinsDump {

    private val HOME = System.getProperty("user.home")
    private val DEST = Paths.get(HOME, "Desktop", "builtins")

    @Test
    fun getBuiltins() {
        Files.deleteIfExists(DEST)
        Files.createDirectories(DEST)
        val map = SqlBuiltins.builtins.groupBy(
            keySelector = { it.signature.name },
            valueTransform = { it.signature },
        )
        for ((name, signatures) in map) {
            val file = Files.createFile(DEST.resolve("$name.ion"))
            val fields = mutableListOf<Pair<String, IonElement>>()
            val writer = IonTextWriterBuilder
                .pretty()
                .build(file.outputStream())
            fields.add("name" to ionSymbol(name))
            fields.add("type" to ionSymbol("fn"))
            fields.add("description" to ionString("\nPlaceholder for $name description\n"))
            fields.add("properties" to ionListOf())
            fields.add("impls" to ionListOf(signatures.map { it.toIon() }))
            val definition = ionStructOf(*fields.toTypedArray())
            definition.writeTo(writer)
        }
    }

    // private fun header(fn: FnSignature) = buildString {
    //     appendLine("/*")
    //     append(fn.sql())
    //     appendLine("*/")
    // }

    private fun FnSignature.toIon(): IonElement {
        val elements = mutableListOf<IonElement>()
        elements.add(ionSymbol("fn"))
        for (p in parameters) {
            val t = p.type.toIon()
            elements.add(t.withAnnotations(p.name))
        }
        elements.add(ionSymbol("->"))
        elements.add(returns.toIon())
        return ionSexpOf(elements)
    }

    private fun PType.toIon(): SymbolElement {
        val text = when (kind) {
            PType.Kind.INT_ARBITRARY -> "numeric"
            PType.Kind.DECIMAL_ARBITRARY -> "decimal"
            PType.Kind.DOUBLE_PRECISION -> "double"
            PType.Kind.ROW -> "struct"
            else -> kind.name.lowercase()
        }
        return ionSymbol(text)
    }
}
