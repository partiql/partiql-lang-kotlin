package org.partiql.testscript.parser

import java.nio.charset.Charset

/**
 * Creates the input and replaces `#` by `$`. We use `#` in the test fixtures because escaping `$` in a kotlin
 * multiline string is messy, e.g. `"""${"$"}"""` results in `"$"` 
 */
fun createInput(vararg ionDocuments: String): List<NamedInputStream> =
        ionDocuments.mapIndexed { index, doc ->
            NamedInputStream(
                    "input[$index]",
                    doc.replace("#", "$").byteInputStream(Charset.forName("UTF-8")))
        }