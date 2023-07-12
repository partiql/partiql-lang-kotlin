package org.partiql.coverage.api.impl

import com.googlecode.jgenhtml.JGenHtml

/**
 * Writes the LCOV Report in HTML form to a directory.
 */
internal object HtmlWriter {

    private const val PREFIX_TO_REMOVE: String = "partiql/coverage/source"

    public fun write(reportPath: String, htmlOutputDir: String) {
        val args = arrayOf(
            "--title", "PartiQL Code Coverage Report",
            "--legend",
            "--prefix", PREFIX_TO_REMOVE,
            "--output-directory", htmlOutputDir,
            "--branch-coverage",
            "--show-details",
            reportPath
        )
        JGenHtml.main(args)
    }
}
