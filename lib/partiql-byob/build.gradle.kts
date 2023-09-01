plugins {
    id(Plugins.conventions)
    id(Plugins.application)
}

dependencies {
    //
    implementation(project(":lib:partiql-transpiler"))
    implementation(project(":partiql-spi"))
    // REPL
    implementation(Deps.guava)
    implementation(Deps.picoCli)
    implementation(Deps.jansi)
    implementation(Deps.jline)
    implementation(Deps.joda)
    implementation(project(":partiql-parser")) // highlighter
    implementation(testFixtures(project(":partiql-planner")))
}

kotlin {
    explicitApi = null
}

ktlint {
    ignoreFailures.set(true)
}

tasks.processResources {
    from("${project(":partiql-planner").buildDir}/resources/testFixtures")
}

application {
    applicationName = "byob"
    mainClass.set("org.partiql.transpiler.cli.Main")
}

tasks.register<GradleBuild>("install") {
    tasks = listOf("assembleDist", "distZip", "installDist")
}
