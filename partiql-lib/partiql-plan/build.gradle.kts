plugins {
    id(Plugins.conventions)
    id(Plugins.library)
}

dependencies {
    // only for the org.partiql.domains package
    implementation(project(":lang"))
    implementation(project(":partiql-spi"))
    // pretty printing
    implementation(Deps.kotlinReflect)
}
