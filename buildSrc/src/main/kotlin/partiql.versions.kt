/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

// Replace with `versionCatalogs` once stabilized
// https://docs.gradle.org/current/userguide/platforms.html

object Versions {
    //---Language
    const val kotlin = "1.5.31"
    const val kotlinTarget = "1.4"
    const val javaTarget = "1.8"
    //---Dependencies
    const val antlr = "4.10.1"
    const val awsSdk = "1.12.344"
    const val csv = "1.8"
    const val guava = "31.1-jre"
    const val ionBuilder = "1.0.0"
    const val ionElement = "1.0.0"
    const val ionJava = "1.9.0"
    const val ionSchema = "1.4.0"
    const val jansi = "2.4.0"
    const val jline = "3.21.0"
    const val jmh = "0.5.3"
    const val joda = "2.12.1"
    const val picoCli = "4.7.0"
    const val ktlint = "10.2.1"
    const val pig = "0.6.1"
    //---Testing
    const val assertj = "3.11.0"
    const val jacoco = "0.8.8"
    const val junit5 = "5.7.0"
    const val junit4 = "4.12"
    const val junit4Params = "1.1.1"
    const val mockito = "4.5.0"
    const val mockk = "1.11.0"
}

object Deps {
    //---Language
    val kotlin = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"
    val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}"
    //---Dependencies
    val antlr = "org.antlr:antlr4:${Versions.antlr}"
    val antlrRuntime = "org.antlr:antlr4-runtime:${Versions.antlr}"
    val awsSdkBom = "com.amazonaws:aws-java-sdk-bom:${Versions.awsSdk}"
    val awsSdkDynamodb = "com.amazonaws:aws-java-sdk-dynamodb:${Versions.awsSdk}"
    val awsSdkS3 = "com.amazonaws:aws-java-sdk-s3:${Versions.awsSdk}"
    val csv = "org.apache.commons:commons-csv:${Versions.csv}"
    val guava = "com.google.guava:guava:${Versions.guava}"
    val ionJava = "com.amazon.ion:ion-java:${Versions.ionJava}"
    val ionElement = "com.amazon.ion:ion-element:${Versions.ionElement}"
    val ionBuilder = "com.amazon.ion:ion-kotlin-builder:${Versions.ionBuilder}"
    val ionSchema = "com.amazon.ion:ion-schema-kotlin:${Versions.ionSchema}"
    val jansi = "org.fusesource.jansi:jansi:${Versions.jansi}"
    val jline = "org.jline:jline:${Versions.jline}"
    val joda = "joda-time:joda-time:${Versions.joda}"
    const val picoCli = "info.picocli:picocli:${Versions.picoCli}"
    val pig = "org.partiql:partiql-ir-generator:${Versions.pig}"
    val pigRuntime = "org.partiql:partiql-ir-generator-runtime:${Versions.pig}"
    //---Testing
    val assertj = "org.assertj:assertj-core:${Versions.assertj}"
    val junit4 = "junit:junit:${Versions.junit4}"
    val junit4Params = "pl.pragmatists:JUnitParams:${Versions.junit4Params}"
    val junitParams = "org.junit.jupiter:junit-jupiter-params:${Versions.junit5}"
    val junitVintage = "org.junit.vintage:junit-vintage-engine:${Versions.junit5}"
    val kotlinTest = "org.jetbrains.kotlin:kotlin-test:${Versions.kotlin}"
    val kotlinTestJunit = "org.jetbrains.kotlin:kotlin-test-junit5:${Versions.kotlin}"
    val mockito = "org.mockito:mockito-junit-jupiter:${Versions.mockito}"
    val mockk = "io.mockk:mockk:${Versions.mockk}"
}

object Plugins {
    //---PartiQL
    val conventions = "partiql.conventions"
    val pig = "org.partiql.pig.pig-gradle-plugin"
    val publish = "org.partiql.gradle.plugin.publish"
    //---3P
    val antlr = "org.gradle.antlr"
    val application = "org.gradle.application"
    val detekt = "io.gitlab.arturbosch.detekt"
    val dokka = "org.jetbrains.dokka"
    val jmh = "me.champeau.gradle.jmh"
    val ktlint = "org.jlleitschuh.gradle.ktlint"
    val library = "org.gradle.java-library"
}
