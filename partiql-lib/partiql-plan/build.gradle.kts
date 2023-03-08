plugins {
    id(Plugins.conventions)
    id(Plugins.library)
}

dependencies {
    // only for the org.partiql.domains package
    implementation(project(":partiql-lang"))
    implementation(project(":partiql-types"))
    // pretty printing
    implementation(Deps.kotlinReflect)
}
