
plugins {
    id(Plugins.conventions)
    `java-library`
}

dependencies {
    implementation("com.pinterest.ktlint:ktlint-core:0.47.0")

    testImplementation("com.pinterest.ktlint:ktlint-test:0.47.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.0")
}

tasks.test {
    useJUnitPlatform() // Enable JUnit5
}

repositories {
    mavenCentral()
}
