package org.partiql.cli

import com.amazon.ion.IonSystem
import com.amazon.ion.system.IonSystemBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.partiql.lang.eval.Bindings
import org.partiql.lang.eval.ExprValue
import org.partiql.pipeline.AbstractPipeline
import java.io.ByteArrayOutputStream
import java.io.OutputStream

/**
 * Initializes a CLI and runs the passed-in query
 */
internal fun makeCliAndGetResult(
    query: String,
    input: String? = null,
    inputFormat: InputFormat = InputFormat.ION,
    bindings: Bindings<ExprValue> = Bindings.empty(),
    outputFormat: OutputFormat = OutputFormat.ION_TEXT,
    output: OutputStream = ByteArrayOutputStream(),
    ion: IonSystem = IonSystemBuilder.standard().build(),
    pipeline: AbstractPipeline = AbstractPipeline.standard(),
    wrapIon: Boolean = false
): String {
    val cli = Cli(
        ion,
        input?.byteInputStream(Charsets.UTF_8) ?: EmptyInputStream(),
        inputFormat,
        output,
        outputFormat,
        pipeline,
        bindings,
        query,
        wrapIon
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
fun assertAsIon(ion: IonSystem, expected: String, actual: String) = assertEquals(ion.loader.load(expected), ion.loader.load(actual))

fun String.singleIonExprValue(ion: IonSystem = IonSystemBuilder.standard().build()) = ExprValue.of(ion.singleValue(this))

fun Map<String, String>.asBinding() =
    Bindings.ofMap(this.mapValues { it.value.singleIonExprValue() })
