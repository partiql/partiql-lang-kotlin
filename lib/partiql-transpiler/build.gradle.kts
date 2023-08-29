plugins {
    id(Plugins.conventions)
    id(Plugins.library)
}

dependencies {
    implementation(project(":partiql-ast"))
    implementation(project(":partiql-parser"))
    implementation(project(":partiql-plan"))
    implementation(project(":partiql-planner"))
    implementation(project(":partiql-spi"))
    implementation(project(":partiql-types"))
}

kotlin {
    explicitApi = null
}

ktlint {
    ignoreFailures.set(true)
}
