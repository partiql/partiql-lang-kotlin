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

package org.partiql.gradle.plugin.publish

import com.github.jengelman.gradle.plugins.shadow.ShadowPlugin
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByName
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import java.io.File

/**
 * Gradle plugin to consolidates the following publishing logic
 * - Maven Publishing
 * - Signing
 * - SourcesJar
 * - Dokka + JavadocJar
 * - Kotlin Explicit API Mode
 */
abstract class PublishPlugin : Plugin<Project> {

    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply(JavaPlugin::class.java)
        pluginManager.apply(MavenPublishPlugin::class.java)
        pluginManager.apply(SigningPlugin::class.java)
        pluginManager.apply(DokkaPlugin::class.java)
        pluginManager.apply(ShadowPlugin::class.java)
        extensions.getByType(KotlinJvmProjectExtension::class.java).explicitApi = ExplicitApiMode.Strict
        val ext = extensions.create("publish", PublishExtension::class.java)
        target.afterEvaluate { publish(ext) }
    }

    private fun Project.publish(ext: PublishExtension) {
        val releaseVersion = !version.toString().endsWith("-SNAPSHOT")

        // Run dokka unless the environment explicitly specifies false
        val runDokka = (System.getenv()["DOKKA"] != "false") || releaseVersion

        // Include "sources" and "javadoc" in the JAR
        extensions.getByType(JavaPluginExtension::class.java).run {
            withSourcesJar()
            withJavadocJar()
        }

        tasks.getByName<DokkaTask>("dokkaHtml") {
            // Only generate javadoc for a release as this consumes a lot of build time
            // 2022 M1 Pro
            //             `./gradlew clean build --no-build-cache` BUILD SUCCESSFUL in 8m 22s
            // `DOKKA=false ./gradlew clean build --no-build-cache` BUILD SUCCESSFUL in 5m 14s
            onlyIf { runDokka }
            outputDirectory.set(File("${buildDir}/javadoc"))
        }

        // Add dokkaHtml output to the javadocJar
        tasks.getByName<Jar>("javadocJar") {
            onlyIf { runDokka }
            dependsOn(JavaPlugin.CLASSES_TASK_NAME)
            archiveClassifier.set("javadoc")
            from(tasks.named("dokkaHtml"))
        }

        tasks.getByName<ShadowJar>("shadowJar") {
            // Use the default name for published shadow jar
            archiveClassifier.set("")
        }

        tasks.getByName<Jar>("jar") {
            // Rename jar for `project` dependencies; not published to Maven
            archiveClassifier.set("original")
        }

        // Setup Maven Central Publishing
        afterEvaluate {
            val publishing = extensions.getByType(PublishingExtension::class.java).apply {
                publications {
                    create<MavenPublication>("maven") {
                        // Publish the shadow jar; create dependencies separately since `ShadowExtension.component`
                        // does not include non-shadowed in POM dependencies
                        artifact(tasks["shadowJar"])
                        artifact(tasks["sourcesJar"])
                        artifact(tasks["javadocJar"])
                        artifactId = ext.artifactId
                        pom {
                            packaging = "jar"
                            name.set(ext.name)
                            description.set(ext.description)
                            url.set(ext.url)
                            scm {
                                connection.set("scm:git@github.com:partiql/partiql-lang-kotlin.git")
                                developerConnection.set("scm:git@github.com:partiql/partiql-lang-kotlin.git")
                                url.set("git@github.com:partiql/partiql-lang-kotlin.git")
                            }
                            licenses {
                                license {
                                    name.set("The Apache License, Version 2.0")
                                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                                }
                            }
                            developers {
                                developer {
                                    name.set("PartiQL Team")
                                    email.set("partiql-dev@amazon.com")
                                    organization.set("PartiQL")
                                    organizationUrl.set("https://github.com/partiql")
                                }
                            }
                            // Publish the dependencies
                            withXml {
                                val dependenciesNode = asNode().appendNode("dependencies")
                                val apiDeps = project.configurations["api"].allDependencies
                                    .filter { it.name !in ext.excludedDependencies }
                                val implDeps = project.configurations["implementation"].allDependencies
                                    .filter { it !in apiDeps && it.name !in ext.excludedDependencies }
                                // Add Gradle 'api' dependencies; mapped to Maven 'compile'
                                apiDeps.forEach { dependency ->
                                    val dependencyNode = dependenciesNode.appendNode("dependency")
                                    dependencyNode.appendNode("groupId", dependency.group)
                                    dependencyNode.appendNode("artifactId", dependency.name)
                                    dependencyNode.appendNode("version", dependency.version)
                                    dependencyNode.appendNode("scope", "compile")
                                }
                                // Add Gradle 'implementation' dependencies; mapped to Maven 'runtime'
                                implDeps.forEach { dependency ->
                                    val dependencyNode = dependenciesNode.appendNode("dependency")
                                    dependencyNode.appendNode("groupId", dependency.group)
                                    dependencyNode.appendNode("artifactId", dependency.name)
                                    dependencyNode.appendNode("version", dependency.version)
                                    dependencyNode.appendNode("scope", "runtime")
                                }
                            }
                        }
                    }
                }
            }

            // Sign only if publishing to Maven Central
            extensions.getByType(SigningExtension::class.java).run {
                setRequired {
                    releaseVersion && gradle.taskGraph.allTasks.any { it is PublishToMavenRepository }
                }
                sign(publishing.publications["maven"])
            }
        }
    }
}

abstract class PublishExtension {
    var artifactId: String = ""
    var name: String = ""
    var description: String = ""
    var url: String = "https://github.com/partiql/partiql-lang-kotlin"
    var excludedDependencies: Set<String> = setOf()
    override fun toString(): String {
        return "PublishExtension(artifactId='$artifactId', name='$name', description='$description', url='$url')"
    }
}
