package org.partiql.testscript.parser

import java.io.File
import java.nio.charset.Charset

val inputBasePath = "${File(".").absolutePath.removeSuffix("/.")}/src/test/resources"

/**
 * Creates the input and replaces `#` by `$`. We use `#` in the test fixtures because escaping `$` in a kotlin
 * multiline string is messy, e.g. `"""${"$"}"""` results in `"$"`
 */
fun createInput(vararg ionDocuments: String): List<NamedInputStream> =
    ionDocuments.mapIndexed { index, doc ->
        NamedInputStream(
            "$inputBasePath/input[$index].sqlts",
            doc.replace("#", "$").byteInputStream(Charset.forName("UTF-8"))
        )
    }
