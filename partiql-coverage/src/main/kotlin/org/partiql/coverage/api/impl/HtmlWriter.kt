package org.partiql.coverage.api.impl

import com.googlecode.jgenhtml.JGenHtml
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString

/**
 * Writes the LCOV Report in HTML form to a directory. Internally uses JGenHtml to accomplish this.
 */
internal object HtmlWriter {

    fun write(reportPath: String, htmlOutputDir: String, title: String) {
        val sourcePrefix = Path(reportPath).parent.resolve("source").absolutePathString()
        val args = arrayOf(
            "--title", title,
            "--legend",
            "--prefix", sourcePrefix,
            "--output-directory", htmlOutputDir,
            "--branch-coverage",
            "--show-details",
            "--quiet",
            reportPath
        )
        JGenHtml.main(args)
    }
}
