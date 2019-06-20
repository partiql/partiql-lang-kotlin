package org.partiql.testscript.parser

import java.nio.charset.Charset

fun createInput(vararg ionDocuments: String): List<NamedInputStream> =
        ionDocuments.mapIndexed { index, doc ->
            NamedInputStream(
                    "input[$index]",
                    doc.replace("#", "$").byteInputStream(Charset.forName("UTF-8")))
        }