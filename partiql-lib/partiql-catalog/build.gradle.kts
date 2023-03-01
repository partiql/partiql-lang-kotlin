plugins {
    id(Plugins.conventions)
    id(Plugins.library)
}

dependencies {
    // only for the org.partiql.domains package
    // TODO: Remove dependency on lang. Move the AST and visitors to another package.
    implementation(project(":lang"))
    // pretty printing
    implementation(Deps.kotlinReflect)
}
