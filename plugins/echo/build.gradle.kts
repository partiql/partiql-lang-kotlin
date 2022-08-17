import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.4.32"
    id("com.github.johnrengelman.shadow") version "7.1.2"
    `java-library`
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // COW HACK because `partiql-spi` is not in Maven
    implementation(files("../../partiql-spi/build/libs/partiql-spi-0.7.1-SNAPSHOT.jar"))
    implementation("com.amazon.ion:ion-java:1.9.0")
    implementation("com.amazon.ion:ion-element:0.2.0")
}

tasks {
    named<ShadowJar>("shadowJar") {
        archiveBaseName.set("echo-plugin")
        archiveClassifier.set("")
        archiveVersion.set("")
    }
}

tasks {
    jar {
        enabled = false
    }
    build {
        dependsOn(shadowJar)
    }
}
