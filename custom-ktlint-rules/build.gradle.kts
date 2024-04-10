plugins {
    id(Plugins.conventions)
    `java-library`
}

dependencies {
    implementation(Deps.ktlint)

    testImplementation(Deps.assertj)
    testImplementation(Deps.ktlintTest)
    testImplementation(Deps.junitParams)
}

repositories {
    mavenCentral()
}
