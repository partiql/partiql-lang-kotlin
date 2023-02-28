plugins {
    id(Plugins.conventions)
    id(Plugins.library)
}

dependencies {
    // only for the org.partiql.domains package
    implementation(project(":lang"))
    // pretty printing
    implementation(Deps.kotlinReflect)
}