package org.partiql.benchmarks.compiler

class CompilerBuilder {
    private var version: Version = Version.EVAL_HEAD

    fun current(): CompilerBuilder = this.apply {
        this.version = Version.EVAL_HEAD
    }

    fun version(version: Version): CompilerBuilder {
        return this.apply {
            this.version = version
        }
    }

    fun build(): Compiler {
        return when (this.version) {
            Version.EVAL_V1_0_0_PERF_1 -> CompilerV1_0_0_Perf_1()
            Version.EVAL_HEAD -> CompilerCurrent()
            Version.LEGACY_V1_0_0_PERF_1 -> CompilerLegacy()
        }
    }

    enum class Version {
        EVAL_V1_0_0_PERF_1,
        EVAL_HEAD,
        LEGACY_V1_0_0_PERF_1,
    }
}
