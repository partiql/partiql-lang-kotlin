plugins {
    id(Plugins.conventions)
    id(Plugins.library)
}

dependencies {
    implementation(project(":partiql-ast"))
    implementation(project(":partiql-plan"))
    implementation(project(":partiql-spi"))
    implementation(project(":partiql-types"))
    // temporary
    implementation(project(":partiql-lang"))
}

kotlin {
    explicitApi = null
}

ktlint {
    ignoreFailures.set(true)
}
