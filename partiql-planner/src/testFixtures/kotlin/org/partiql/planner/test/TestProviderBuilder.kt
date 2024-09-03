package org.partiql.planner.test

import java.nio.file.Path

class TestProviderBuilder {

    private var root: Path? = null
    private var factory: TestBuilderFactory? = null

    fun root(path: Path): TestProviderBuilder = this.apply {
        this.root = path
    }

    fun factory(factory: TestBuilderFactory): TestProviderBuilder = this.apply {
        this.factory = factory
    }

    fun build(): TestProvider {
        return TestProvider(factory!!, root)
    }
}
