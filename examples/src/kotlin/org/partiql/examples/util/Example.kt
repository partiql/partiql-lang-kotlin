package org.partiql.examples.util

import java.io.PrintStream

abstract class Example(val out: PrintStream) {
    abstract fun run();

    final fun print(label: String, data: String) {
        out.println(label)
        out.println("    ${data.replace("\n", "\n    ")}")
    }
}