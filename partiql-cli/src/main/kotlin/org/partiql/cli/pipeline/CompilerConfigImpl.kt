package org.partiql.cli.pipeline

import org.partiql.eval.CompilerConfig
import org.partiql.eval.PartiQLEngine

class CompilerConfigImpl(
    private val mode: PartiQLEngine.Mode,
    private val listener: AppPErrorListener
) : CompilerConfig {
    override fun getErrorListener(): AppPErrorListener = listener

    override fun getMode(): PartiQLEngine.Mode = mode
}
