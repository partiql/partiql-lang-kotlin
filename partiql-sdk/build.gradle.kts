import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id(Plugins.conventions)
    id(Plugins.library)
    id(Plugins.smithy).version("1.1.0")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

smithy {}

dependencies {

    // for smithy-build.json
    smithyBuild("software.amazon.smithy.kotlin:smithy-kotlin-codegen:0.33.1")
    smithyBuild("software.amazon.smithy.kotlin:smithy-aws-kotlin-codegen:0.33.1")
    implementation("software.amazon.smithy:smithy-aws-traits:1.52.1")

    // for generated-src
    implementation(kotlin("stdlib"))
    implementation("aws.smithy.kotlin:aws-json-protocols:1.3.1")
    implementation("aws.smithy.kotlin:aws-protocol-core:1.3.1")
    implementation("aws.smithy.kotlin:http:1.3.1")
    implementation("aws.smithy.kotlin:http-auth:1.3.1")
    implementation("aws.smithy.kotlin:http-client-engine-default:1.3.1")
    implementation("aws.smithy.kotlin:identity-api:1.3.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")
    implementation("aws.smithy.kotlin:serde:1.3.1")
    implementation("aws.smithy.kotlin:serde-json:1.3.1")
    implementation("aws.smithy.kotlin:telemetry-defaults:1.3.1")
    api("aws.smithy.kotlin:http-client:1.3.1")
    api("aws.smithy.kotlin:runtime-core:1.3.1")
    api("aws.smithy.kotlin:smithy-client:1.3.1")
    api("aws.smithy.kotlin:telemetry-api:1.3.1")
}

val generatedSrc: Directory = project.layout.buildDirectory.dir("generated-src").get()

val copyGeneratedSources = tasks.register("copyGenerateSources") {
    dependsOn(tasks.smithyBuild)
    outputs.dir(generatedSrc)
    outputs.upToDateWhen { false }
    doLast {
        val src = smithy.outputDirectory.dir("source").get()
        copy {
            from(src.dir("kotlin-codegen/src/main/kotlin"))
            into(generatedSrc)
        }
    }
}

kotlin.sourceSets {
    all {
        languageSettings.optIn("kotlin.RequiresOptIn")
        languageSettings.optIn("aws.smithy.kotlin.runtime.InternalApi")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(copyGeneratedSources)
}

tasks.clean.configure {
    delete(project.layout.projectDirectory.dir("generated-src"))
}
