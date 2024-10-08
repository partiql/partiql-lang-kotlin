package org.partiql.cli.pipeline

import org.partiql.eval.CompilerConfig
import org.partiql.eval.PartiQLEngine

class CompilerConfigImpl(
    private val mode: PartiQLEngine.Mode,
    private val listener: AppErrorListener
) : CompilerConfig {
    override fun getErrorListener(): AppErrorListener = listener

    override fun getMode(): PartiQLEngine.Mode = mode
}
