package org.partiql.testscript

import com.amazon.ion.system.IonSystemBuilder
import org.partiql.testscript.compiler.Compiler
import org.partiql.testscript.extensions.listRecursive
import org.partiql.testscript.extensions.ptsFileFilter
import org.partiql.testscript.parser.NamedInputStream
import org.partiql.testscript.parser.Parser
import java.io.File
import java.io.FileInputStream

private val ion = IonSystemBuilder.standard().build()
private val ptsParser = Parser(ion)
private val ptsCompiler = Compiler(ion)

fun main() {
    val inputs = File("integration-test2/test-scripts")
        .listRecursive(ptsFileFilter)
        .map { file -> NamedInputStream(file.absolutePath, FileInputStream(file)) }

    val ast = ptsParser.parse(inputs)
    ptsCompiler.compile(ast)
}
