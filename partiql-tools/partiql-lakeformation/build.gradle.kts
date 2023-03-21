plugins {
    id(Plugins.conventions)
    id(Plugins.publish)
}

dependencies {
    implementation(project(":partiql-lang"))
    implementation(project(":partiql-plan"))
    implementation(project(":partiql-types"))
    implementation(Deps.ionElement)
}

publish {
    artifactId = "partiql-lakeformation"
    name = "PartiQL Lake Formation"
    description = "A tool to help generate Lake Formation Data Filter from SQL compatible Query."
}