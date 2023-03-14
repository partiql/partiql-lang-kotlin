plugins {
    id(Plugins.conventions)
    id(Plugins.library)
    id(Plugins.publish)
}

dependencies {
    api(project(":partiql-types"))
    implementation(Deps.ionElement)
    implementation(Deps.kotlinReflect)
}

publish {
    artifactId = "partiql-plan"
    name = "PartiQL Plan"
    description = "PartiQL's logical plan."
}
