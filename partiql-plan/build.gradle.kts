plugins {
    id(Plugins.conventions)
    id(Plugins.library)
}

dependencies {
    implementation(project(":partiql-types"))
    implementation(Deps.ionElement)
    implementation(Deps.kotlinReflect)
}
