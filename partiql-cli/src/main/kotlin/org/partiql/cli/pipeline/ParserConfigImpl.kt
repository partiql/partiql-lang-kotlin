package org.partiql.cli.pipeline

import org.partiql.parser.ParserConfig

class ParserConfigImpl(
    private val listener: AppPErrorListener
) : ParserConfig {
    override fun getErrorListener(): AppPErrorListener = listener
}
