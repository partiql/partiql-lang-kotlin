package org.partiql.cli

import com.amazon.ion.IonSystem
import com.amazon.ion.system.IonSystemBuilder
import org.junit.Assert
import org.partiql.lang.CompilerPipeline
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.ExprValue
import org.partiql.lang.eval.ExprValueFactory
import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * Initializes a CLI and runs the passed-in query
 */
fun makeCliAndGetResult(
    query: String,
    input: String? = null,
    bindings: Bindings<ExprValue> = Bindings.empty(),
    outputFormat: OutputFormat = OutputFormat.ION_TEXT,
    output: OutputStream = ByteArrayOutputStream(),
    ion: IonSystem = IonSystemBuilder.standard().build(),
    compilerPipeline: CompilerPipeline = CompilerPipeline.standard(ion),
    valueFactory: ExprValueFactory = ExprValueFactory.standard(ion)
): String {
    val cli = Cli(
        valueFactory,
        input?.byteInputStream(Charsets.UTF_8) ?: EmptyInputStream(),
        InputFormat.ION,
        output,
        outputFormat,
        compilerPipeline,
        bindings,
        query
    )
    cli.run()
    return output.toString()
}

/**
 * An assertion helper
 */
fun assertAsIon(expected: String, actual: String) {
    val ion = IonSystemBuilder.standard().build()
    assertAsIon(ion, expected, actual)
}

/**
 * An assertion helper
 */
fun assertAsIon(ion: IonSystem, expected: String, actual: String) =
    Assert.assertEquals(ion.loader.load(expected), ion.loader.load(actual))
