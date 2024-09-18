plugins {
    id(Plugins.conventions)
    id(Plugins.library)
    id(Plugins.shadow)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.partiql:partiql-lang-kotlin:0.14.8")
}

tasks.shadowJar {
    // configurations = listOf(project.configurations.shadow.get())
    relocate("org.partiql", "org.partiql_v0_14_8")
}

// Workaround for https://github.com/johnrengelman/shadow/issues/651
// components.withType(AdhocComponentWithVariants::class.java).forEach { c ->
//    c.withVariantsFromConfiguration(project.configurations.shadowRuntimeElements.get()) {
//        skip()
//    }
// }
