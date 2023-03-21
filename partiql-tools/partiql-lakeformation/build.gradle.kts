plugins {
    id(Plugins.conventions)
}

dependencies {
    implementation(project(":partiql-lang"))
    implementation(project(":partiql-plan"))
    implementation(project(":partiql-types"))
    implementation(Deps.ionElement)
}
