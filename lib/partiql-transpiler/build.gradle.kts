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
    // Test
    testImplementation(testFixtures(project(":partiql-planner")))
}

kotlin {
    explicitApi = null
}

ktlint {
    ignoreFailures.set(true)
}

tasks.processTestResources {
    from("${project(":partiql-planner").buildDir}/resources/testFixtures")
}
