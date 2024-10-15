package org.partiql.cli.pipeline

import org.partiql.parser.ParserConfig

class ParserConfigImpl(
    private val listener: AppErrorListener
) : ParserConfig {
    override fun getErrorListener(): AppErrorListener = listener
}
