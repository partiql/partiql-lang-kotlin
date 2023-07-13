package org.partiql.coverage.api.impl

import com.googlecode.jgenhtml.JGenHtml
import java.nio.file.Path

/**
 * Writes the LCOV Report in HTML form to a directory. Internally uses JGenHtml to accomplish this.
 */
internal object HtmlWriter {

    public fun write(reportPath: String, htmlOutputDir: String) {
        val prefixToRemove = Path.of("build", "partiql", "coverage", "source")
        val args = arrayOf(
            "--title", "PartiQL Code Coverage Report",
            "--legend",
            "--prefix", prefixToRemove.toAbsolutePath().toString(),
            "--output-directory", htmlOutputDir,
            "--branch-coverage",
            "--show-details",
            "--quiet",
            reportPath
        )
        JGenHtml.main(args)
    }
}
