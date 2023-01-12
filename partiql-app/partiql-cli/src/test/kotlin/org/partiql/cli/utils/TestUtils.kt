package org.partiql.cli.utils

import java.io.File
import java.net.URL

internal object TestUtils {

    internal object ResourceFileNames {
        const val TEST_BAG = "test-bag.ion"
        const val WRAPPED_VALUES = "wrapped-values.ion"
    }

    internal fun getResourceFile(name: String): File {
        val resource: URL = javaClass.classLoader.getResource(name)!!
        return File(resource.toURI())
    }
}
