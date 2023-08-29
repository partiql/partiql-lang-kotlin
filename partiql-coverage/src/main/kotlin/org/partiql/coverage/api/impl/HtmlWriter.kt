/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

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
